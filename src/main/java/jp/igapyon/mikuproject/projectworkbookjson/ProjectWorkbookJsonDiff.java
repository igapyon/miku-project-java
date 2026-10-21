/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuproject.projectworkbookjson;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Compares two workbook JSON documents without projecting them through ProjectModel. */
public class ProjectWorkbookJsonDiff {
    private static final String[] TASK_HEADERS = { "UID", "ID", "Name", "OutlineLevel", "OutlineNumber", "WBS", "Start",
            "Finish", "Duration", "PercentComplete", "PercentWorkComplete", "Milestone", "Summary", "Critical", "CalendarUID",
            "Predecessors", "Notes" };
    private static final String[] RESOURCE_HEADERS = { "UID", "ID", "Name", "Type", "Initials", "Group", "MaxUnits",
            "CalendarUID", "StandardRate", "OvertimeRate", "CostPerUse", "Work", "ActualWork", "RemainingWork" };
    private static final String[] ASSIGNMENT_HEADERS = { "UID", "TaskUID", "TaskName", "ResourceUID", "ResourceName", "Start",
            "Finish", "Units", "Work", "ActualWork", "RemainingWork", "PercentWorkComplete" };
    private static final String[] CALENDAR_HEADERS = { "UID", "Name", "IsBaseCalendar", "BaseCalendarUID", "WeekDays",
            "Exceptions", "WorkWeeks" };
    private static final String[] NON_WORKING_DAYS_HEADERS = { "CalendarUID", "Index", "CalendarName", "Name", "Date",
            "FromDate", "ToDate", "DayWorking" };

    private final ProjectWorkbookJsonValidate validate = new ProjectWorkbookJsonValidate();

    public Result diffDocuments(Object beforeLike, Object afterLike) {
        ProjectWorkbookJsonValidate.ValidationResult beforeValidation = validate.validateWorkbookJsonDocument(beforeLike);
        ProjectWorkbookJsonValidate.ValidationResult afterValidation = validate.validateWorkbookJsonDocument(afterLike);
        List<WorkbookJsonWarning> warnings = new ArrayList<WorkbookJsonWarning>();
        appendPrefixedWarnings(warnings, "before", beforeValidation.warnings);
        appendPrefixedWarnings(warnings, "after", afterValidation.warnings);
        appendUpstreamColumnWarnings(warnings, beforeValidation.document, "before");
        appendUpstreamColumnWarnings(warnings, afterValidation.document, "after");

        List<Map<String, Object>> changes = new ArrayList<Map<String, Object>>();
        Map<RowKey, IndexedRow> beforeProject = indexRows(beforeValidation.document, "Project", "before", warnings);
        Map<RowKey, IndexedRow> afterProject = indexRows(afterValidation.document, "Project", "after", warnings);
        String projectLabel = projectLabel(beforeProject, afterProject);
        for (String field : ProjectWorkbookSchema.PROJECT_FIELD_ORDER) {
            Object before = fieldValue(beforeProject.get(RowKey.single(field)));
            Object after = fieldValue(afterProject.get(RowKey.single(field)));
            if (!scalarEquals(before, after)) {
                changes.add(change("project", "project", projectLabel, field, before, after, "field-updated", "Project", field));
            }
        }

        String[][] definitions = {
                { "Tasks", "tasks" }, { "Resources", "resources" }, { "Assignments", "assignments" },
                { "Calendars", "calendars" }, { "NonWorkingDays", "calendars" }
        };
        for (String[] definition : definitions) {
            String sheet = definition[0];
            String scope = definition[1];
            Map<RowKey, IndexedRow> oldRows = indexRows(beforeValidation.document, sheet, "before", warnings);
            Map<RowKey, IndexedRow> newRows = indexRows(afterValidation.document, sheet, "after", warnings);
            String[] fields = headers(sheet);

            for (Map.Entry<RowKey, IndexedRow> entry : oldRows.entrySet()) {
                RowKey key = entry.getKey();
                IndexedRow oldItem = entry.getValue();
                if (!newRows.containsKey(key)) {
                    Map<String, Object> item = change(scope, oldItem.uid, label(oldItem.row, oldItem.uid), "$row",
                            snapshot(oldItem.row, fields), null, "row-removed", sheet, key.toOutputKey(sheet));
                    item.put("before_position", Integer.valueOf(oldItem.position));
                    item.put("after_position", null);
                    changes.add(item);
                }
            }
            for (Map.Entry<RowKey, IndexedRow> entry : newRows.entrySet()) {
                RowKey key = entry.getKey();
                IndexedRow newItem = entry.getValue();
                IndexedRow oldItem = oldRows.get(key);
                if (oldItem == null) {
                    Map<String, Object> item = change(scope, newItem.uid, label(newItem.row, newItem.uid), "$row", null,
                            snapshot(newItem.row, fields), "row-added", sheet, key.toOutputKey(sheet));
                    item.put("before_position", null);
                    item.put("after_position", Integer.valueOf(newItem.position));
                    changes.add(item);
                    continue;
                }
                for (String field : fields) {
                    if ("UID".equals(field) || ("NonWorkingDays".equals(sheet)
                            && ("CalendarUID".equals(field) || "Index".equals(field)))) {
                        continue;
                    }
                    Object before = scalar(oldItem.row.get(field), sheet + "." + field);
                    Object after = scalar(newItem.row.get(field), sheet + "." + field);
                    if (!scalarEquals(before, after)) {
                        changes.add(change(scope, oldItem.uid, label(oldItem.row, oldItem.uid), field, before, after,
                                "field-updated", sheet, key.toOutputKey(sheet)));
                    }
                }
            }
            if ("Tasks".equals(sheet)) {
                appendTaskOrderChanges(changes, oldRows, newRows, sheet, scope);
            }
        }

        Result result = new Result();
        result.changes = changes;
        result.warnings = warnings;
        result.comparisonComplete = warnings.isEmpty();
        return result;
    }

    private void appendTaskOrderChanges(List<Map<String, Object>> changes, Map<RowKey, IndexedRow> oldRows,
            Map<RowKey, IndexedRow> newRows, String sheet, String scope) {
        List<RowKey> oldOrder = new ArrayList<RowKey>();
        List<RowKey> newOrder = new ArrayList<RowKey>();
        for (RowKey key : oldRows.keySet()) {
            if (newRows.containsKey(key)) oldOrder.add(key);
        }
        for (RowKey key : newRows.keySet()) {
            if (oldRows.containsKey(key)) newOrder.add(key);
        }
        Map<RowKey, Integer> newPositions = new LinkedHashMap<RowKey, Integer>();
        for (int index = 0; index < newOrder.size(); index++) {
            newPositions.put(newOrder.get(index), Integer.valueOf(index));
        }
        for (int index = 0; index < oldOrder.size(); index++) {
            RowKey key = oldOrder.get(index);
            int nextPosition = newPositions.get(key).intValue();
            if (index == nextPosition) continue;
            IndexedRow item = oldRows.get(key);
            changes.add(change(scope, item.uid, label(item.row, item.uid), "$position", Integer.valueOf(index),
                    Integer.valueOf(nextPosition), "order-changed", sheet, key.toOutputKey(sheet)));
        }
    }

    private Map<RowKey, IndexedRow> indexRows(WorkbookJsonDocument document, String sheet, String side,
            List<WorkbookJsonWarning> warnings) {
        Map<RowKey, IndexedRow> indexed = new LinkedHashMap<RowKey, IndexedRow>();
        List<Map<String, Object>> rows = document.sheets.get(sheet);
        String[] fields = headers(sheet);
        String[] identityFields = "Project".equals(sheet) ? new String[] { "Field", "Value" } : fields;
        for (int position = 0; position < rows.size(); position++) {
            Map<String, Object> row = rows.get(position);
            String location = side + "." + sheet + "[" + position + "]";
            if (isBlankRow(row, identityFields)) continue;
            String uid = identity(row.get("Project".equals(sheet) ? "Field"
                    : "NonWorkingDays".equals(sheet) ? "CalendarUID" : "UID"), location);
            RowKey key = "NonWorkingDays".equals(sheet)
                    ? RowKey.pair(uid, identity(row.get("Index"), location + ".Index"))
                    : RowKey.single(uid);
            if (indexed.containsKey(key)) {
                throw new IllegalArgumentException("workbook diff の行識別子が重複しています: " + location + " (" + key + ")");
            }
            if ("Project".equals(sheet) && !Arrays.asList(ProjectWorkbookSchema.PROJECT_FIELD_ORDER).contains(uid)) {
                warnings.add(warning(side + ": 未知の Project field は比較しません: " + uid));
            }
            for (String field : "Project".equals(sheet) ? new String[] { "Value" } : fields) {
                scalar(row.get(field), location + "." + field);
            }
            indexed.put(key, new IndexedRow(row, position, uid));
        }
        return indexed;
    }

    private boolean isBlankRow(Map<String, Object> row, String[] fields) {
        for (String field : fields) {
            Object value = row.get(field);
            if (value != null && !"".equals(value)) return false;
        }
        return true;
    }

    private String identity(Object value, String location) {
        if (!(value instanceof String) && !(value instanceof Number)) {
            throw new IllegalArgumentException("workbook diff の行識別子がありません: " + location);
        }
        String text;
        if (value instanceof Number) {
            ensureFinite((Number) value, location);
            try {
                text = new BigDecimal(value.toString()).stripTrailingZeros().toPlainString();
            } catch (NumberFormatException ex) {
                text = String.valueOf(value);
            }
        } else {
            text = String.valueOf(value).trim();
        }
        if (text.length() == 0) throw new IllegalArgumentException("workbook diff の行識別子がありません: " + location);
        return text;
    }

    private Object scalar(Object value, String location) {
        if (value == null || value instanceof String || value instanceof Boolean) return value;
        if (value instanceof Number) {
            ensureFinite((Number) value, location);
            return value;
        }
        throw new IllegalArgumentException("workbook diff の値は scalar である必要があります: " + location);
    }

    private void ensureFinite(Number value, String location) {
        if ((value instanceof Double && !Double.isFinite(value.doubleValue()))
                || (value instanceof Float && !Float.isFinite(value.floatValue()))) {
            throw new IllegalArgumentException("workbook diff の値は scalar である必要があります: " + location);
        }
    }

    private boolean scalarEquals(Object before, Object after) {
        if (before instanceof Number && after instanceof Number) {
            try {
                return new BigDecimal(before.toString()).compareTo(new BigDecimal(after.toString())) == 0;
            } catch (NumberFormatException ex) {
                return Double.compare(((Number) before).doubleValue(), ((Number) after).doubleValue()) == 0;
            }
        }
        return Objects.equals(before, after);
    }

    private Object fieldValue(IndexedRow row) {
        return scalar(row == null ? null : row.row.get("Value"), "Project.Value");
    }

    private String projectLabel(Map<RowKey, IndexedRow> before, Map<RowKey, IndexedRow> after) {
        Object name = fieldValue(before.get(RowKey.single("Name")));
        if (name == null) name = fieldValue(after.get(RowKey.single("Name")));
        return name == null ? "project" : String.valueOf(name);
    }

    private String label(Map<String, Object> row, String uid) {
        Object value = row.get("Name");
        if (value == null) value = row.get("TaskName");
        if (value == null) value = row.get("CalendarName");
        return value == null ? uid : String.valueOf(value);
    }

    private Map<String, Object> snapshot(Map<String, Object> row, String[] fields) {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        for (String field : fields) result.put(field, scalar(row.get(field), field));
        return result;
    }

    private Map<String, Object> change(String scope, String uid, String label, String field, Object before, Object after,
            String type, String sheet, String rowKey) {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("scope", scope);
        result.put("uid", uid);
        result.put("label", label);
        result.put("field", field);
        result.put("before", before);
        result.put("after", after);
        result.put("change_type", type);
        result.put("sheet", sheet);
        result.put("row_key", rowKey);
        return result;
    }

    private String[] headers(String sheet) {
        if ("Project".equals(sheet)) return new String[] { "Field", "Value" };
        if ("Tasks".equals(sheet)) return TASK_HEADERS;
        if ("Resources".equals(sheet)) return RESOURCE_HEADERS;
        if ("Assignments".equals(sheet)) return ASSIGNMENT_HEADERS;
        if ("Calendars".equals(sheet)) return CALENDAR_HEADERS;
        return NON_WORKING_DAYS_HEADERS;
    }

    private void appendUpstreamColumnWarnings(List<WorkbookJsonWarning> warnings, WorkbookJsonDocument document, String side) {
        for (Map.Entry<String, List<Map<String, Object>>> sheetEntry : document.sheets.entrySet()) {
            String sheet = sheetEntry.getKey();
            List<String> known = Arrays.asList(headers(sheet));
            List<Map<String, Object>> rows = sheetEntry.getValue();
            for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
                for (String field : rows.get(rowIndex).keySet()) {
                    if (known.contains(field)) continue;
                    String message = side + ": 未知の列は無視します: " + sheet + "[" + rowIndex + "]." + field;
                    if (!hasWarning(warnings, message)) warnings.add(warning(message));
                }
            }
        }
    }

    private boolean hasWarning(List<WorkbookJsonWarning> warnings, String message) {
        for (WorkbookJsonWarning warning : warnings) {
            if (message.equals(warning.message)) return true;
        }
        return false;
    }

    private void appendPrefixedWarnings(List<WorkbookJsonWarning> target, String side, List<WorkbookJsonWarning> source) {
        for (WorkbookJsonWarning item : source) target.add(warning(side + ": " + item.message));
    }

    private WorkbookJsonWarning warning(String message) {
        WorkbookJsonWarning result = new WorkbookJsonWarning();
        result.message = message;
        return result;
    }

    private static String escapeJson(String value) {
        StringBuilder result = new StringBuilder();
        for (int index = 0; index < value.length(); index++) {
            char ch = value.charAt(index);
            if (ch == '"' || ch == '\\') result.append('\\').append(ch);
            else if (ch == '\n') result.append("\\n");
            else if (ch == '\r') result.append("\\r");
            else if (ch == '\t') result.append("\\t");
            else if (ch < 0x20) result.append(String.format("\\u%04x", Integer.valueOf(ch)));
            else result.append(ch);
        }
        return result.toString();
    }

    private static final class IndexedRow {
        private final Map<String, Object> row;
        private final int position;
        private final String uid;

        private IndexedRow(Map<String, Object> row, int position, String uid) {
            this.row = row;
            this.position = position;
            this.uid = uid;
        }
    }

    private static final class RowKey {
        private final String first;
        private final String second;

        private RowKey(String first, String second) { this.first = first; this.second = second; }
        private static RowKey single(String value) { return new RowKey(value, null); }
        private static RowKey pair(String first, String second) { return new RowKey(first, second); }

        private String toOutputKey(String sheet) {
            if (!"NonWorkingDays".equals(sheet)) return first;
            return "[\"" + escapeJson(first) + "\",\"" + escapeJson(second) + "\"]";
        }

        @Override public boolean equals(Object other) {
            if (!(other instanceof RowKey)) return false;
            RowKey that = (RowKey) other;
            return Objects.equals(first, that.first) && Objects.equals(second, that.second);
        }

        @Override public int hashCode() { return Objects.hash(first, second); }
        @Override public String toString() { return second == null ? first : "[" + first + ", " + second + "]"; }
    }

    public static class Result {
        public List<Map<String, Object>> changes;
        public List<WorkbookJsonWarning> warnings;
        public boolean comparisonComplete;
    }
}
