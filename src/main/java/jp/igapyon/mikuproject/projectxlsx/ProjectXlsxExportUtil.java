/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuproject.projectxlsx;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import jp.igapyon.mikuproject.projectworkbookjson.ProjectWorkbookSchema;

public class ProjectXlsxExportUtil {
    public static final String OPTIONS_SHEET_NAME = "Options";
    public static final String HEADER_FILL = "#D9EAF7";
    public static final String SECTION_FILL = "#BFD7EA";
    public static final String LABEL_FILL = "#EDF5FB";
    public static final String ALT_ROW_FILL = "#F9FBFD";
    public static final String DATE_FILL = "#FFF4E8";
    public static final String PERCENT_FILL = "#FCECF3";
    public static final String REFERENCE_FILL = "#EEF7F4";
    public static final String COUNT_FILL = "#F2F5F8";
    public static final String EDITABLE_FILL = "#FDE7C7";
    public static final String DURATION_FILL = "#FBF6ED";
    public static final String NOTES_FILL = "#FFFBEA";
    public static final String NAME_FILL = "#FAF6FF";
    public static final String WORK_FILL = "#F1F8FD";
    public static final String BOOLEAN_TRUE_LABEL = "○";
    public static final String BOOLEAN_FALSE_LABEL = "ー";
    public static final SheetTheme PROJECT_THEME = new SheetTheme("#BFD7EA", "#D9EAF7", "#EDF5FB");
    public static final SheetTheme TASKS_THEME = new SheetTheme("#D4E0EC", "#E6EDF4", "#F2F6FA");
    public static final SheetTheme RESOURCES_THEME = new SheetTheme("#C8E3D8", "#DDF0E8", "#EFF8F4");
    public static final SheetTheme ASSIGNMENTS_THEME = new SheetTheme("#D7D2EC", "#E7E3F5", "#F2F0FA");
    public static final SheetTheme CALENDARS_THEME = new SheetTheme("#D7E3C4", "#E7F0DA", "#F2F7EA");
    public static final SheetTheme NON_WORKING_DAYS_THEME = new SheetTheme("#E9C7D5", "#F4DDE6", "#FBEEF3");
    private static final Set<String> DATE_TIME_LABELS = setOf("StartDate", "FinishDate", "CurrentDate", "StatusDate");
    private static final Set<String> NUMERIC_SUMMARY_LABELS = setOf("OutlineCodes", "WBSMasks", "ExtendedAttributes",
            "MinutesPerDay", "MinutesPerWeek", "DaysPerMonth");
    private static final Set<String> EDITABLE_PROJECT_LABELS = setOf(ProjectWorkbookSchema.PROJECT_EDITABLE_FIELDS);
    private static final Set<String> TASK_IMPORTABLE_FIELDS = setOf("Name", "Start", "Finish", "Duration",
            "PercentComplete", "PercentWorkComplete", "Milestone", "Summary", "Critical", "Type", "Priority",
            "CalendarUID", "ConstraintType", "ConstraintDate", "Deadline", "Predecessors", "Notes");
    private static final Set<String> RESOURCE_IMPORTABLE_FIELDS = setOf("Name", "Type", "Initials", "Group", "MaxUnits",
            "CalendarUID", "StandardRate", "OvertimeRate", "CostPerUse", "Work", "ActualWork", "RemainingWork", "Cost",
            "ActualCost", "RemainingCost", "PercentWorkComplete", "WorkGroup", "StandardRateFormat",
            "OvertimeRateFormat");
    private static final Set<String> ASSIGNMENT_IMPORTABLE_FIELDS = setOf("Start", "Finish", "StartVariance",
            "FinishVariance", "Delay", "Milestone", "WorkContour", "Units", "Work", "Cost", "ActualWork",
            "RemainingWork", "ActualCost", "RemainingCost", "OvertimeWork", "ActualOvertimeWork",
            "PercentWorkComplete");
    private static final Set<String> CALENDAR_IMPORTABLE_FIELDS = setOf("Name", "IsBaseCalendar", "BaseCalendarUID");
    private static final Set<String> NON_WORKING_DAY_IMPORTABLE_FIELDS = setOf("Name", "Date", "FromDate", "ToDate",
            "DayWorking");
    private static final Set<String> DATE_TIME_COLUMNS = setOf("Start", "Finish", "ConstraintDate", "Deadline");
    private static final Set<String> DATE_ONLY_COLUMNS = setOf("Date", "FromDate", "ToDate");
    private static final Set<String> DURATION_COLUMNS = setOf("Duration", "StartVariance", "FinishVariance", "Delay");
    private static final Set<String> NAME_COLUMNS = setOf("Name", "TaskName", "ResourceName", "CalendarName");
    private static final Set<String> COUNT_COLUMNS = setOf("UID", "ID", "OutlineLevel", "Milestone", "Summary",
            "Critical", "Type", "Priority", "CalendarUID", "ConstraintType", "ResourceUID", "TaskUID", "Units", "Cost",
            "ActualCost", "RemainingCost", "CostPerUse", "MaxUnits", "WorkGroup", "StandardRateFormat",
            "OvertimeRateFormat", "IsBaseCalendar", "BaseCalendarUID", "WeekDays", "Exceptions", "WorkWeeks", "Index",
            "DayWorking");
    private static final Map<String, Set<String>> IMPORTABLE_FIELDS_BY_SHEET = buildImportableFieldsBySheet();
    private static final Map<String, SheetTheme> SHEET_THEMES = buildSheetThemes();

    public XlsxRowLike titleRow(String title) {
        return titleRow(title, SECTION_FILL);
    }

    public XlsxRowLike titleRow(String title, String fillColor) {
        XlsxRowLike row = new XlsxRowLike();
        row.height = Integer.valueOf(28);
        XlsxCellLike cell = new XlsxCellLike();
        cell.value = title;
        cell.bold = Boolean.TRUE;
        cell.fontSize = Integer.valueOf(16);
        cell.fillColor = fillColor;
        cell.horizontalAlign = "left";
        row.cells.add(cell);
        XlsxCellLike continuation = new XlsxCellLike();
        continuation.fillColor = fillColor;
        row.cells.add(continuation);
        return row;
    }

    public XlsxRowLike sectionTitleRow(String title) {
        return sectionTitleRow(title, 1, SECTION_FILL);
    }

    public XlsxRowLike sectionTitleRow(String title, int columnCount, String fillColor) {
        XlsxRowLike row = new XlsxRowLike();
        row.height = Integer.valueOf(26);
        XlsxCellLike cell = new XlsxCellLike();
        cell.value = title;
        cell.bold = Boolean.TRUE;
        cell.fontSize = Integer.valueOf(14);
        cell.fillColor = fillColor;
        cell.horizontalAlign = "left";
        row.cells.add(cell);
        for (int index = 1; index < Math.max(1, columnCount); index++) {
            XlsxCellLike continuation = new XlsxCellLike();
            continuation.fillColor = fillColor;
            row.cells.add(continuation);
        }
        return row;
    }

    public XlsxRowLike headerRow(String[] headers) {
        return headerRow(headers, HEADER_FILL);
    }

    public XlsxRowLike headerRow(String[] headers, String fillColor) {
        XlsxRowLike row = new XlsxRowLike();
        row.height = Integer.valueOf(24);
        for (String header : headers) {
            XlsxCellLike cell = cell(header);
            cell.bold = Boolean.TRUE;
            cell.fillColor = fillColor;
            cell.border = "thin";
            cell.horizontalAlign = "center";
            row.cells.add(cell);
        }
        return row;
    }

    public XlsxRowLike keyValueRow(Object field, Object value) {
        return keyValueRow(field, value, LABEL_FILL);
    }

    public XlsxRowLike keyValueRow(Object field, Object value, String labelFill) {
        XlsxRowLike row = new XlsxRowLike();
        XlsxCellLike labelCell = cell(field);
        labelCell.bold = Boolean.TRUE;
        labelCell.fillColor = labelFill;
        labelCell.border = "thin";
        row.cells.add(labelCell);
        row.cells.add(projectValueCell(String.valueOf(field), value));
        return row;
    }

    public XlsxRowLike singleValueRow(String value) {
        XlsxRowLike row = new XlsxRowLike();
        row.cells.add(cell(value));
        return row;
    }

    public XlsxCellLike cell(Object value) {
        XlsxCellLike cell = new XlsxCellLike();
        cell.value = stringifyCellValue(value);
        if (value != null) {
            cell.border = "thin";
        }
        return cell;
    }

    public XlsxCellLike styledCell(Object value, int rowIndex) {
        return styledCell(value, rowIndex, null, null);
    }

    public XlsxCellLike styledCell(Object value, int rowIndex, String fillColor, String horizontalAlign) {
        XlsxCellLike cell = cell(value);
        if (cell.value == null) {
            return cell;
        }
        cell.fillColor = fillColor != null ? fillColor : (rowIndex % 2 == 0 ? ALT_ROW_FILL : null);
        cell.horizontalAlign = horizontalAlign;
        return cell;
    }

    public XlsxCellLike editableCell(XlsxCellLike cell) {
        cell.border = cell.border == null ? "thin" : cell.border;
        cell.fillColor = EDITABLE_FILL;
        return cell;
    }

    public XlsxCellLike projectValueCell(String label, Object value) {
        XlsxCellLike cell;
        if (isDateTimeLabel(label)) {
            cell = cell(formatDateTimeDisplay(value));
            cell.fillColor = DATE_FILL;
        } else if ("Name".equals(label) || "Title".equals(label)) {
            cell = cell(value);
            cell.fillColor = NAME_FILL;
            cell.bold = Boolean.TRUE;
        } else if ("Author".equals(label) || "Company".equals(label)) {
            cell = cell(value);
            cell.fillColor = NAME_FILL;
        } else if ("CalendarUID".equals(label)) {
            cell = cell(value);
            cell.fillColor = REFERENCE_FILL;
            cell.horizontalAlign = "center";
        } else if ("ScheduleFromStart".equals(label)) {
            cell = cell(value);
            cell.fillColor = COUNT_FILL;
            cell.horizontalAlign = "center";
        } else {
            cell = cell(value);
            if (isNumericSummaryLabel(label)) {
                cell.fillColor = COUNT_FILL;
            }
        }
        return isEditableProjectLabel(label) ? editableCell(cell) : cell;
    }

    public XlsxCellLike dataCell(String sheetName, String header, Object value, int rowIndex) {
        XlsxCellLike cell;
        if (isDateTimeColumn(header) || isDateOnlyColumn(header)) {
            cell = styledCell(formatDateTimeDisplay(value), rowIndex, DATE_FILL, "center");
        } else if (isPercentColumn(header)) {
            cell = styledCell(value, rowIndex, PERCENT_FILL, "center");
        } else if (isDurationOrWorkColumn(header)) {
            cell = styledCell(value, rowIndex, isWorkColumn(header) ? WORK_FILL : DURATION_FILL, "center");
        } else if (isReferenceColumn(header)) {
            cell = styledCell(value, rowIndex, REFERENCE_FILL, "center");
        } else if (isNameColumn(header)) {
            cell = styledCell(value, rowIndex, NAME_FILL, null);
        } else if (isCountColumn(header)) {
            cell = styledCell(value, rowIndex, COUNT_FILL, "center");
        } else if ("Notes".equals(header)) {
            cell = styledCell(value, rowIndex, NOTES_FILL, null);
        } else {
            cell = styledCell(value, rowIndex);
        }
        return isImportableField(sheetName, header) ? editableCell(cell) : cell;
    }

    public void addColumns(XlsxSheetLike sheet, double[] widths) {
        sheet.columns.clear();
        for (double width : widths) {
            XlsxColumnLike column = new XlsxColumnLike();
            column.width = Double.valueOf(width);
            sheet.columns.add(column);
        }
    }

    public void addBooleanDataValidation(XlsxSheetLike sheet, String[] ranges) {
        StringBuilder sqref = new StringBuilder();
        if (ranges != null) {
            for (String range : ranges) {
                if (range == null || range.isEmpty()) {
                    continue;
                }
                if (sqref.length() > 0) {
                    sqref.append(' ');
                }
                sqref.append(range);
            }
        }
        if (sqref.length() == 0) {
            return;
        }
        XlsxDataValidationLike validation = new XlsxDataValidationLike();
        validation.type = "list";
        validation.sqref = sqref.toString();
        validation.formula1 = OPTIONS_SHEET_NAME + "!$A$2:$A$3";
        validation.allowBlank = Boolean.TRUE;
        sheet.dataValidations.add(validation);
    }

    public String buildColumnRange(int columnIndex, int startRow, int endRow) {
        if (endRow < startRow) {
            return null;
        }
        String column = encodeColumnName(columnIndex);
        return column + startRow + ":" + column + endRow;
    }

    public String buildSingleCellReference(int columnIndex, int rowNumber) {
        if (rowNumber <= 0) {
            return null;
        }
        return encodeColumnName(columnIndex) + rowNumber;
    }

    private String encodeColumnName(int columnIndex) {
        int current = columnIndex + 1;
        StringBuilder builder = new StringBuilder();
        while (current > 0) {
            int remainder = (current - 1) % 26;
            builder.insert(0, (char) ('A' + remainder));
            current = (current - 1) / 26;
        }
        return builder.toString();
    }

    public SheetTheme themeForSheet(String sheetName) {
        SheetTheme theme = SHEET_THEMES.get(sheetName);
        return theme != null ? theme : PROJECT_THEME;
    }

    private Object stringifyCellValue(Object value) {
        if (value instanceof Boolean) {
            return ((Boolean) value).booleanValue() ? BOOLEAN_TRUE_LABEL : BOOLEAN_FALSE_LABEL;
        }
        return value;
    }

    private Object formatDateTimeDisplay(Object value) {
        if (value instanceof String) {
            return ((String) value).replace("T", " ");
        }
        return value;
    }

    private boolean isDateTimeLabel(String label) {
        return DATE_TIME_LABELS.contains(label);
    }

    private boolean isNumericSummaryLabel(String label) {
        return NUMERIC_SUMMARY_LABELS.contains(label);
    }

    private boolean isEditableProjectLabel(String label) {
        return EDITABLE_PROJECT_LABELS.contains(label);
    }

    private boolean isImportableField(String sheetName, String header) {
        Set<String> importableFields = IMPORTABLE_FIELDS_BY_SHEET.get(sheetName);
        return importableFields != null && importableFields.contains(header);
    }

    private boolean isDateTimeColumn(String header) {
        return DATE_TIME_COLUMNS.contains(header);
    }

    private boolean isDateOnlyColumn(String header) {
        return DATE_ONLY_COLUMNS.contains(header);
    }

    private boolean isPercentColumn(String header) {
        return header.indexOf("Percent") >= 0;
    }

    private boolean isDurationOrWorkColumn(String header) {
        return DURATION_COLUMNS.contains(header) || isWorkColumn(header);
    }

    private boolean isWorkColumn(String header) {
        return header.endsWith("Work") || "Work".equals(header);
    }

    private boolean isReferenceColumn(String header) {
        return header.endsWith("UID") || "Predecessors".equals(header);
    }

    private boolean isNameColumn(String header) {
        return NAME_COLUMNS.contains(header);
    }

    private boolean isCountColumn(String header) {
        return COUNT_COLUMNS.contains(header);
    }

    private static Set<String> setOf(String... values) {
        return Collections.unmodifiableSet(new LinkedHashSet<String>(Arrays.asList(values)));
    }

    private static Map<String, Set<String>> buildImportableFieldsBySheet() {
        Map<String, Set<String>> map = new LinkedHashMap<String, Set<String>>();
        map.put("Tasks", TASK_IMPORTABLE_FIELDS);
        map.put("Resources", RESOURCE_IMPORTABLE_FIELDS);
        map.put("Assignments", ASSIGNMENT_IMPORTABLE_FIELDS);
        map.put("Calendars", CALENDAR_IMPORTABLE_FIELDS);
        map.put("NonWorkingDays", NON_WORKING_DAY_IMPORTABLE_FIELDS);
        return Collections.unmodifiableMap(map);
    }

    private static Map<String, SheetTheme> buildSheetThemes() {
        Map<String, SheetTheme> map = new LinkedHashMap<String, SheetTheme>();
        map.put("Tasks", TASKS_THEME);
        map.put("Resources", RESOURCES_THEME);
        map.put("Assignments", ASSIGNMENTS_THEME);
        map.put("Calendars", CALENDARS_THEME);
        map.put("NonWorkingDays", NON_WORKING_DAYS_THEME);
        return Collections.unmodifiableMap(map);
    }

    public static class SheetTheme {
        public final String section;
        public final String header;
        public final String label;

        public SheetTheme(String section, String header, String label) {
            this.section = section;
            this.header = header;
            this.label = label;
        }
    }
}
