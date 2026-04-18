/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuproject.msprojectxml;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import jp.igapyon.mikuproject.model.AssignmentModel;
import jp.igapyon.mikuproject.model.PredecessorModel;
import jp.igapyon.mikuproject.model.ProjectInfo;
import jp.igapyon.mikuproject.model.ProjectModel;
import jp.igapyon.mikuproject.model.ResourceModel;
import jp.igapyon.mikuproject.model.TaskModel;

public class MsProjectCsv {
    private final MsProjectXml msProjectXml = new MsProjectXml();
    private final MsProjectCalendar msProjectCalendar = new MsProjectCalendar();

    public String exportCsvParentId(ProjectModel model) {
        ProjectModel normalizedModel = msProjectXml.normalizeProjectModel(model);
        List<String> header = Arrays.asList("ID", "ParentID", "WBS", "Name", "Start", "Finish", "PredecessorID", "Resource",
                "PercentComplete", "PercentWorkComplete", "Milestone", "Summary", "Critical", "Type", "Priority", "Work",
                "CalendarUID", "ConstraintType", "ConstraintDate", "Deadline", "Notes");
        Map<String, String> parentUidMap = buildTaskParentUidMap(normalizedModel.tasks);
        Map<String, String> resourceNameByUid = new LinkedHashMap<String, String>();
        for (ResourceModel resource : normalizedModel.resources) {
            if (resource != null && resource.uid != null) {
                resourceNameByUid.put(resource.uid, resource.name);
            }
        }
        Map<String, List<String>> assignmentNamesByTaskUid = new LinkedHashMap<String, List<String>>();
        for (AssignmentModel assignment : normalizedModel.assignments) {
            if (assignment == null) {
                continue;
            }
            String resourceName = resourceNameByUid.get(assignment.resourceUid);
            if (resourceName == null || resourceName.isEmpty()) {
                continue;
            }
            List<String> names = assignmentNamesByTaskUid.get(assignment.taskUid);
            if (names == null) {
                names = new ArrayList<String>();
                assignmentNamesByTaskUid.put(assignment.taskUid, names);
            }
            if (!names.contains(resourceName)) {
                names.add(resourceName);
            }
        }

        StringBuilder builder = new StringBuilder();
        appendCsvRow(builder, header);
        for (TaskModel task : normalizedModel.tasks) {
            List<String> row = new ArrayList<String>();
            row.add(safe(task.uid));
            row.add(safe(parentUidMap.get(task.uid)));
            row.add(task.wbs != null && !task.wbs.isEmpty() ? task.wbs : safe(task.outlineNumber));
            row.add(safe(task.name));
            row.add(safe(task.start));
            row.add(safe(task.finish));
            row.add(joinPredecessors(task.predecessors));
            row.add(joinStrings(assignmentNamesByTaskUid.get(task.uid), "|"));
            row.add(task.percentComplete == null ? "" : String.valueOf(task.percentComplete));
            row.add(task.percentWorkComplete == null ? "" : String.valueOf(task.percentWorkComplete));
            row.add(task.milestone ? "1" : "0");
            row.add(task.summary ? "1" : "0");
            row.add(task.critical == null ? "" : (task.critical.booleanValue() ? "1" : "0"));
            row.add(task.type == null ? "" : String.valueOf(task.type));
            row.add(task.priority == null ? "" : String.valueOf(task.priority));
            row.add(safe(task.work));
            row.add(safe(task.calendarUID));
            row.add(task.constraintType == null ? "" : String.valueOf(task.constraintType));
            row.add(safe(task.constraintDate));
            row.add(safe(task.deadline));
            row.add(safe(task.notes));
            appendCsvRow(builder, row);
        }
        return builder.toString();
    }

    public ProjectModel importCsvParentId(String csvText) {
        List<List<String>> rows = parseCsvRows(csvText == null ? "" : csvText.trim());
        if (rows.isEmpty()) {
            throw new IllegalArgumentException("CSV が空です");
        }

        List<String> header = rows.get(0);
        List<String> requiredColumns = Arrays.asList("ID", "ParentID", "Name");
        for (String requiredColumn : requiredColumns) {
            if (!header.contains(requiredColumn)) {
                throw new IllegalArgumentException("CSV に必須列がありません: " + requiredColumn);
            }
        }

        List<CsvTaskRow> entries = new ArrayList<CsvTaskRow>();
        for (int i = 1; i < rows.size(); i++) {
            List<String> row = rows.get(i);
            CsvTaskRow entry = new CsvTaskRow();
            entry.id = cell(row, header, "ID");
            entry.parentId = cell(row, header, "ParentID");
            entry.wbs = cell(row, header, "WBS");
            entry.name = cell(row, header, "Name");
            entry.start = cell(row, header, "Start");
            entry.finish = cell(row, header, "Finish");
            entry.predecessorId = cell(row, header, "PredecessorID");
            entry.resource = cell(row, header, "Resource");
            entry.percentComplete = parseNumber(defaultString(cell(row, header, "PercentComplete"), "0"), 0);
            entry.percentWorkComplete = optionalNumber(cell(row, header, "PercentWorkComplete"));
            entry.milestone = parseCsvBooleanCell(cell(row, header, "Milestone"), false);
            entry.summary = optionalBoolean(cell(row, header, "Summary"));
            entry.critical = optionalBoolean(cell(row, header, "Critical"));
            entry.type = optionalNumber(cell(row, header, "Type"));
            entry.priority = optionalNumber(cell(row, header, "Priority"));
            entry.work = cell(row, header, "Work");
            entry.calendarUID = cell(row, header, "CalendarUID");
            entry.constraintType = optionalNumber(cell(row, header, "ConstraintType"));
            entry.constraintDate = cell(row, header, "ConstraintDate");
            entry.deadline = cell(row, header, "Deadline");
            entry.notes = cell(row, header, "Notes");
            if (!entry.id.isEmpty()) {
                entries.add(entry);
            }
        }

        Map<String, CsvTaskRow> entryMap = new LinkedHashMap<String, CsvTaskRow>();
        for (CsvTaskRow entry : entries) {
            if (entryMap.containsKey(entry.id)) {
                throw new IllegalArgumentException("CSV の ID が重複しています: " + entry.id);
            }
            if (entry.name.isEmpty()) {
                throw new IllegalArgumentException("CSV の Name が空です: ID=" + entry.id);
            }
            if (!entry.parentId.isEmpty() && entry.parentId.equals(entry.id)) {
                throw new IllegalArgumentException("CSV の ParentID が自身を指しています: ID=" + entry.id);
            }
            entryMap.put(entry.id, entry);
        }
        for (CsvTaskRow entry : entries) {
            if (!entry.parentId.isEmpty() && !entryMap.containsKey(entry.parentId)) {
                throw new IllegalArgumentException(
                        "CSV の ParentID が既存 ID を指していません: ID=" + entry.id + ", ParentID=" + entry.parentId);
            }
        }

        Set<String> visiting = new LinkedHashSet<String>();
        Set<String> visited = new LinkedHashSet<String>();
        for (CsvTaskRow entry : entries) {
            checkCycle(entry, entryMap, visiting, visited);
        }

        List<CsvTaskRow> roots = new ArrayList<CsvTaskRow>();
        for (CsvTaskRow entry : entries) {
            if (entry.parentId.isEmpty()) {
                roots.add(entry);
            } else {
                entryMap.get(entry.parentId).children.add(entry);
            }
        }

        List<TaskModel> tasks = new ArrayList<TaskModel>();
        for (int i = 0; i < roots.size(); i++) {
            walkEntry(roots.get(i), tasks, Arrays.asList(Integer.valueOf(i + 1)));
        }

        Set<String> resourceNames = new LinkedHashSet<String>();
        for (CsvTaskRow entry : entries) {
            resourceNames.addAll(parseCsvMultiValueCell(entry.resource));
        }
        List<ResourceModel> resources = new ArrayList<ResourceModel>();
        Map<String, String> resourceUidByName = new LinkedHashMap<String, String>();
        int resourceIndex = 1;
        for (String resourceName : resourceNames) {
            ResourceModel resource = new ResourceModel();
            resource.uid = String.valueOf(resourceIndex);
            resource.id = String.valueOf(resourceIndex);
            resource.name = resourceName;
            resources.add(resource);
            resourceUidByName.put(resourceName, resource.uid);
            resourceIndex++;
        }

        Map<String, TaskModel> taskByUid = new LinkedHashMap<String, TaskModel>();
        for (TaskModel task : tasks) {
            taskByUid.put(task.uid, task);
        }
        List<AssignmentModel> assignments = new ArrayList<AssignmentModel>();
        int assignmentUid = 1;
        for (CsvTaskRow entry : entries) {
            TaskModel task = taskByUid.get(entry.id);
            if (task == null) {
                continue;
            }
            for (String resourceName : parseCsvMultiValueCell(entry.resource)) {
                AssignmentModel assignment = new AssignmentModel();
                assignment.uid = String.valueOf(assignmentUid++);
                assignment.taskUid = entry.id;
                assignment.resourceUid = defaultString(resourceUidByName.get(resourceName), "");
                assignment.start = task.start;
                assignment.finish = task.finish;
                assignment.percentWorkComplete = task.percentComplete;
                assignments.add(assignment);
            }
        }

        List<String> taskStarts = new ArrayList<String>();
        List<String> taskFinishes = new ArrayList<String>();
        for (TaskModel task : tasks) {
            if (task.start != null && !task.start.isEmpty()) {
                taskStarts.add(task.start);
            }
            if (task.finish != null && !task.finish.isEmpty()) {
                taskFinishes.add(task.finish);
            }
        }
        java.util.Collections.sort(taskStarts);
        java.util.Collections.sort(taskFinishes);

        ProjectModel model = new ProjectModel();
        ProjectInfo project = new ProjectInfo();
        project.name = "CSV Imported Project";
        project.title = "CSV Imported Project";
        project.startDate = taskStarts.isEmpty() ? "" : taskStarts.get(0);
        project.finishDate = taskFinishes.isEmpty() ? "" : taskFinishes.get(taskFinishes.size() - 1);
        project.scheduleFromStart = true;
        model.project = project;
        model.tasks = tasks;
        model.resources = resources;
        model.assignments = assignments;
        model.calendars = new ArrayList<jp.igapyon.mikuproject.model.CalendarModel>();
        return msProjectXml.normalizeProjectModel(msProjectCalendar.ensureDefaultProjectCalendar(model));
    }

    private void walkEntry(CsvTaskRow entry, List<TaskModel> tasks, List<Integer> outlinePath) {
        String start = entry.start;
        String finish = entry.finish;
        if ((isBlank(start) || isBlank(finish)) && !entry.children.isEmpty()) {
            List<String> childStarts = new ArrayList<String>();
            List<String> childFinishes = new ArrayList<String>();
            for (CsvTaskRow child : entry.children) {
                if (!isBlank(child.start)) {
                    childStarts.add(child.start);
                }
                if (!isBlank(child.finish)) {
                    childFinishes.add(child.finish);
                }
            }
            java.util.Collections.sort(childStarts);
            java.util.Collections.sort(childFinishes);
            if (isBlank(start) && !childStarts.isEmpty()) {
                start = childStarts.get(0);
            }
            if (isBlank(finish) && !childFinishes.isEmpty()) {
                finish = childFinishes.get(childFinishes.size() - 1);
            }
        }
        String outlineNumber = joinIntegers(outlinePath, ".");
        TaskModel task = new TaskModel();
        task.uid = entry.id;
        task.id = entry.id;
        task.name = entry.name;
        task.outlineLevel = Integer.valueOf(outlinePath.size());
        task.outlineNumber = outlineNumber;
        task.wbs = entry.wbs.isEmpty() ? outlineNumber : entry.wbs;
        task.type = entry.type;
        task.priority = entry.priority;
        task.work = entry.work.isEmpty() ? null : entry.work;
        task.calendarUID = entry.calendarUID.isEmpty() ? null : entry.calendarUID;
        task.start = start.isEmpty() ? null : start;
        task.finish = finish.isEmpty() ? null : finish;
        task.duration = "PT0H0M0S";
        task.milestone = entry.milestone || (!isBlank(start) && start.equals(finish));
        task.summary = entry.summary == null ? !entry.children.isEmpty() : entry.summary.booleanValue();
        task.critical = entry.critical;
        task.percentComplete = Integer.valueOf(clamp(entry.percentComplete, 0, 100));
        task.percentWorkComplete = entry.percentWorkComplete == null ? null
                : Integer.valueOf(clamp(entry.percentWorkComplete.intValue(), 0, 100));
        task.constraintType = entry.constraintType;
        task.constraintDate = entry.constraintDate.isEmpty() ? null : entry.constraintDate;
        task.deadline = entry.deadline.isEmpty() ? null : entry.deadline;
        task.notes = entry.notes.isEmpty() ? null : entry.notes;
        for (String predecessorUid : parseCsvMultiValueCell(entry.predecessorId)) {
            PredecessorModel predecessor = new PredecessorModel();
            predecessor.predecessorUid = predecessorUid;
            task.predecessors.add(predecessor);
        }
        tasks.add(task);
        for (int i = 0; i < entry.children.size(); i++) {
            List<Integer> childPath = new ArrayList<Integer>(outlinePath);
            childPath.add(Integer.valueOf(i + 1));
            walkEntry(entry.children.get(i), tasks, childPath);
        }
    }

    private void checkCycle(CsvTaskRow entry, Map<String, CsvTaskRow> entryMap, Set<String> visiting, Set<String> visited) {
        if (visited.contains(entry.id)) {
            return;
        }
        if (visiting.contains(entry.id)) {
            throw new IllegalArgumentException("CSV の ParentID が循環しています: ID=" + entry.id);
        }
        visiting.add(entry.id);
        if (!entry.parentId.isEmpty()) {
            CsvTaskRow parent = entryMap.get(entry.parentId);
            if (parent != null) {
                checkCycle(parent, entryMap, visiting, visited);
            }
        }
        visiting.remove(entry.id);
        visited.add(entry.id);
    }

    private Map<String, String> buildTaskParentUidMap(List<TaskModel> tasks) {
        Map<String, String> parentMap = new LinkedHashMap<String, String>();
        Deque<TaskModel> stack = new ArrayDeque<TaskModel>();
        for (TaskModel task : tasks) {
            while (!stack.isEmpty() && compareOutlineLevel(task.outlineLevel, stack.peekLast().outlineLevel) <= 0) {
                stack.removeLast();
            }
            if (!stack.isEmpty()) {
                parentMap.put(task.uid, stack.peekLast().uid);
            }
            stack.addLast(task);
        }
        return parentMap;
    }

    private int compareOutlineLevel(Integer left, Integer right) {
        int leftValue = left == null ? 0 : left.intValue();
        int rightValue = right == null ? 0 : right.intValue();
        return Integer.compare(leftValue, rightValue);
    }

    private List<List<String>> parseCsvRows(String csvText) {
        List<List<String>> rows = new ArrayList<List<String>>();
        List<String> row = new ArrayList<String>();
        StringBuilder cell = new StringBuilder();
        boolean inQuotes = false;
        for (int index = 0; index < csvText.length(); index++) {
            char current = csvText.charAt(index);
            char next = index + 1 < csvText.length() ? csvText.charAt(index + 1) : '\0';
            if (current == '"') {
                if (inQuotes && next == '"') {
                    cell.append('"');
                    index++;
                } else {
                    inQuotes = !inQuotes;
                }
                continue;
            }
            if (!inQuotes && current == ',') {
                row.add(cell.toString());
                cell.setLength(0);
                continue;
            }
            if (!inQuotes && (current == '\n' || current == '\r')) {
                if (current == '\r' && next == '\n') {
                    index++;
                }
                row.add(cell.toString());
                rows.add(row);
                row = new ArrayList<String>();
                cell.setLength(0);
                continue;
            }
            cell.append(current);
        }
        if (cell.length() > 0 || !row.isEmpty()) {
            row.add(cell.toString());
            rows.add(row);
        }
        List<List<String>> filteredRows = new ArrayList<List<String>>();
        for (List<String> currentRow : rows) {
            boolean hasNonBlankCell = false;
            for (String currentCell : currentRow) {
                if (!currentCell.trim().isEmpty()) {
                    hasNonBlankCell = true;
                    break;
                }
            }
            if (hasNonBlankCell) {
                filteredRows.add(currentRow);
            }
        }
        return filteredRows;
    }

    private List<String> parseCsvMultiValueCell(String value) {
        String normalized = safe(value).trim();
        if (normalized.isEmpty()) {
            return new ArrayList<String>();
        }
        String[] items = normalized.split("[|;,、]");
        Set<String> uniqueItems = new LinkedHashSet<String>();
        for (String item : items) {
            String trimmed = item.trim();
            if (!trimmed.isEmpty()) {
                uniqueItems.add(trimmed);
            }
        }
        return new ArrayList<String>(uniqueItems);
    }

    private boolean parseCsvBooleanCell(String value, boolean fallback) {
        String normalized = safe(value).trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            return fallback;
        }
        if (Arrays.asList("1", "true", "yes", "y", "on").contains(normalized)) {
            return true;
        }
        if (Arrays.asList("0", "false", "no", "n", "off").contains(normalized)) {
            return false;
        }
        return fallback;
    }

    private Boolean optionalBoolean(String value) {
        String normalized = safe(value).trim();
        if (normalized.isEmpty()) {
            return null;
        }
        return Boolean.valueOf(parseCsvBooleanCell(normalized, false));
    }

    private Integer optionalNumber(String value) {
        String normalized = safe(value).trim();
        if (normalized.isEmpty()) {
            return null;
        }
        return Integer.valueOf(parseNumber(normalized, 0));
    }

    private int parseNumber(String value, int fallback) {
        String normalized = safe(value).trim();
        if (normalized.isEmpty()) {
            return fallback;
        }
        try {
            return Integer.parseInt(normalized);
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private String cell(List<String> row, List<String> header, String name) {
        int index = header.indexOf(name);
        if (index < 0 || index >= row.size()) {
            return "";
        }
        return safe(row.get(index)).trim();
    }

    private void appendCsvRow(StringBuilder builder, List<String> row) {
        for (int i = 0; i < row.size(); i++) {
            if (i > 0) {
                builder.append(',');
            }
            builder.append(escapeCsvCell(row.get(i)));
        }
        builder.append('\n');
    }

    private String escapeCsvCell(String value) {
        String text = safe(value);
        if (text.contains("\"") || text.contains(",") || text.contains("\n")) {
            return "\"" + text.replace("\"", "\"\"") + "\"";
        }
        return text;
    }

    private String joinPredecessors(List<PredecessorModel> predecessors) {
        List<String> values = new ArrayList<String>();
        if (predecessors != null) {
            for (PredecessorModel predecessor : predecessors) {
                if (predecessor != null && predecessor.predecessorUid != null && !predecessor.predecessorUid.isEmpty()) {
                    values.add(predecessor.predecessorUid);
                }
            }
        }
        return joinStrings(values, "|");
    }

    private String joinStrings(List<String> values, String delimiter) {
        if (values == null || values.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                builder.append(delimiter);
            }
            builder.append(safe(values.get(i)));
        }
        return builder.toString();
    }

    private String joinIntegers(List<Integer> values, String delimiter) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                builder.append(delimiter);
            }
            builder.append(values.get(i).intValue());
        }
        return builder.toString();
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String defaultString(String value, String fallback) {
        return value == null || value.isEmpty() ? fallback : value;
    }

    private static class CsvTaskRow {
        String id = "";
        String parentId = "";
        String wbs = "";
        String name = "";
        String start = "";
        String finish = "";
        String predecessorId = "";
        String resource = "";
        int percentComplete;
        Integer percentWorkComplete;
        boolean milestone;
        Boolean summary;
        Boolean critical;
        Integer type;
        Integer priority;
        String work = "";
        String calendarUID = "";
        Integer constraintType;
        String constraintDate = "";
        String deadline = "";
        String notes = "";
        List<CsvTaskRow> children = new ArrayList<CsvTaskRow>();
    }
}
