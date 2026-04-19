/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuproject.wbsxlsx;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jp.igapyon.mikuproject.model.AssignmentModel;
import jp.igapyon.mikuproject.model.CalendarModel;
import jp.igapyon.mikuproject.model.PredecessorModel;
import jp.igapyon.mikuproject.model.ProjectModel;
import jp.igapyon.mikuproject.model.ResourceModel;
import jp.igapyon.mikuproject.model.TaskModel;
import jp.igapyon.mikuproject.msprojectxml.MsProjectXml;
import jp.igapyon.mikuproject.projectxlsx.XlsxCellLike;
import jp.igapyon.mikuproject.projectxlsx.XlsxColumnLike;
import jp.igapyon.mikuproject.projectxlsx.XlsxRowLike;
import jp.igapyon.mikuproject.projectxlsx.XlsxSheetLike;
import jp.igapyon.mikuproject.projectxlsx.XlsxWorkbookLike;
import jp.igapyon.mikuproject.wbsdateband.WbsDateband;

public class WbsXlsxExport {
    private static final int FIXED_COLUMN_COUNT = 20;

    private final MsProjectXml msProjectXml;
    private final WbsDateband dateband;
    private final WbsXlsxLayout layout;

    public WbsXlsxExport(MsProjectXml msProjectXml, WbsDateband dateband, WbsXlsxLayout layout) {
        this.msProjectXml = msProjectXml;
        this.dateband = dateband;
        this.layout = layout;
    }

    public XlsxWorkbookLike exportWbsWorkbook(ProjectModel model, WbsExportOptions options) {
        ProjectModel normalized = msProjectXml.normalizeProjectModel(model);
        WbsExportOptions actualOptions = options == null ? new WbsExportOptions() : options;
        Set<String> holidaySet = new LinkedHashSet<String>(dateband.collectWbsHolidayDates(normalized));
        if (actualOptions.holidayDates != null) {
            for (String holiday : actualOptions.holidayDates) {
                if (holiday != null && holiday.length() >= 10) {
                    holidaySet.add(holiday.substring(0, 10));
                }
            }
        }
        Set<Integer> nonWorkingDayTypes = dateband.collectProjectNonWorkingDayTypes(normalized);
        List<String> displayDateBand = dateband.buildDisplayDateBand(normalized.project.startDate, normalized.project.finishDate,
                normalized.project.currentDate, actualOptions.displayDaysBeforeBaseDate, actualOptions.displayDaysAfterBaseDate,
                holidaySet, nonWorkingDayTypes, actualOptions.useBusinessDaysForDisplayRange);

        XlsxWorkbookLike workbook = new XlsxWorkbookLike();
        XlsxSheetLike sheet = new XlsxSheetLike();
        sheet.name = "WBS";
        appendColumns(sheet, displayDateBand.size());
        workbook.sheets.add(sheet);

        appendProjectInfoRows(sheet, normalized, holidaySet);
        appendDateBandRows(sheet, displayDateBand, normalized.project.currentDate, holidaySet, nonWorkingDayTypes);
        appendTaskRows(sheet, normalized, displayDateBand, holidaySet, nonWorkingDayTypes, actualOptions.useBusinessDaysForProgressBand);
        appendLegendRows(sheet);
        appendSummaryRows(sheet, normalized, displayDateBand, holidaySet, nonWorkingDayTypes, actualOptions);
        return workbook;
    }

    private void appendColumns(XlsxSheetLike sheet, int displayDays) {
        double[] fixedWidths = new double[] {
                6.43d, 6.43d, 9.29d, 8.57d, 6.43d, 42d, 12.14d, 12.14d, 9.29d, 28d, 12d, 18d, 12d, 12d, 12d, 15d, 12d, 20d, 18d, 4.5d
        };
        for (int index = 0; index < fixedWidths.length; index++) {
            XlsxColumnLike column = new XlsxColumnLike();
            column.width = Double.valueOf(fixedWidths[index]);
            if (index == 11 || index == 12 || index == 13 || index == 14 || index == 16 || index == 17 || index == 18) {
                column.hidden = Boolean.TRUE;
            }
            sheet.columns.add(column);
        }
        for (int index = 0; index < displayDays; index++) {
            XlsxColumnLike column = new XlsxColumnLike();
            column.width = Double.valueOf(4.5d);
            sheet.columns.add(column);
        }
    }

    private void appendProjectInfoRows(XlsxSheetLike sheet, ProjectModel model, Set<String> holidaySet) {
        sheet.rows.add(titleRow("プロジェクト情報"));
        sheet.mergedRanges.add(layout.range("A1", "E1"));
        sheet.rows.add(projectInfoRow("プロジェクト名", safe(model.project.name, "-"), "出力日時 " + formatTimestamp()));
        sheet.rows.add(projectInfoRow("カレンダ", formatCalendarLabel(model), null));
        sheet.rows.add(projectInfoRow("開始日", formatDate(model.project.startDate), null));
        sheet.rows.add(projectInfoRow("終了日", formatDate(model.project.finishDate), null));
        sheet.rows.add(projectInfoRow("現在日", formatDate(model.project.currentDate), null));
        sheet.rows.add(projectInfoRow("祝日", String.valueOf(holidaySet.size()), null));
    }

    private void appendDateBandRows(XlsxSheetLike sheet, List<String> displayDateBand, String currentDate, Set<String> holidaySet,
            Set<Integer> nonWorkingDayTypes) {
        XlsxRowLike dateRow = new XlsxRowLike();
        XlsxRowLike weekdayRow = new XlsxRowLike();
        for (int index = 0; index < FIXED_COLUMN_COUNT; index++) {
            dateRow.cells.add(blankCell());
            weekdayRow.cells.add(blankCell());
        }
        String[] fixedHeaders = new String[] {
                "UID", "ID", "WBS", "種別", "階層", "名称", "開始", "終了", "期間", "タスク詳細", "進捗", "作業進捗", "マイル", "サマリ", "クリティカル",
                "担当", "カレンダ", "リソース", "先行", ""
        };
        for (String header : fixedHeaders) {
            dateRow.cells.add(blankCell());
        }
        weekdayRow.cells.clear();
        for (String header : fixedHeaders) {
            weekdayRow.cells.add(headerCell(header));
        }
        dateRow.cells.subList(FIXED_COLUMN_COUNT, dateRow.cells.size()).clear();
        for (String day : displayDateBand) {
            dateRow.cells.add(dateBandCell(formatShortDate(day), day, currentDate, holidaySet, nonWorkingDayTypes));
            weekdayRow.cells.add(weekdayCell(formatWeekday(day), day, currentDate, holidaySet, nonWorkingDayTypes));
        }
        sheet.rows.add(dateRow);
        sheet.rows.add(weekdayRow);
    }

    private void appendTaskRows(XlsxSheetLike sheet, ProjectModel model, List<String> displayDateBand, Set<String> holidaySet,
            Set<Integer> nonWorkingDayTypes, Boolean useBusinessDaysForProgressBand) {
        Map<String, String> calendarNameByUid = new LinkedHashMap<String, String>();
        for (CalendarModel calendar : model.calendars) {
            if (calendar != null) {
                calendarNameByUid.put(calendar.uid, calendar.name);
            }
        }
        Map<String, String> resourceNameByUid = new LinkedHashMap<String, String>();
        for (ResourceModel resource : model.resources) {
            if (resource != null) {
                resourceNameByUid.put(resource.uid, resource.name);
            }
        }
        Map<String, List<String>> resourceNamesByTaskUid = new LinkedHashMap<String, List<String>>();
        for (AssignmentModel assignment : model.assignments) {
            if (assignment == null || assignment.taskUid == null) {
                continue;
            }
            String name = resourceNameByUid.get(assignment.resourceUid);
            if (name == null) {
                continue;
            }
            List<String> names = resourceNamesByTaskUid.get(assignment.taskUid);
            if (names == null) {
                names = new ArrayList<String>();
                resourceNamesByTaskUid.put(assignment.taskUid, names);
            }
            if (!names.contains(name)) {
                names.add(name);
            }
        }

        for (TaskModel task : model.tasks) {
            XlsxRowLike row = new XlsxRowLike();
            row.cells.add(taskCell(task.uid, task, "center"));
            row.cells.add(taskCell(task.id, task, "center"));
            row.cells.add(taskCell(safe(task.wbs != null ? task.wbs : task.outlineNumber, "-"), task, "center"));
            row.cells.add(kindCell(task));
            row.cells.add(taskCell(task.outlineLevel == null ? "-" : String.valueOf(task.outlineLevel), task, "center"));
            row.cells.add(nameCell(task));
            row.cells.add(taskCell(formatDate(task.start), task, "left"));
            row.cells.add(taskCell(formatDate(task.finish), task, "left"));
            row.cells.add(taskCell(formatDuration(task, holidaySet, nonWorkingDayTypes, useBusinessDaysForProgressBand), task, "center"));
            row.cells.add(detailCell(task.notes, task));
            row.cells.add(progressCell(task.percentComplete, task));
            row.cells.add(progressWorkCell(task.percentWorkComplete, task));
            row.cells.add(flagCell(task.milestone, "Mil", task));
            row.cells.add(flagCell(task.summary, "Sum", task));
            row.cells.add(flagCell(task.critical, "Crit", task));
            List<String> resourceNames = resourceNamesByTaskUid.get(task.uid);
            row.cells.add(taskCell(first(resourceNames), task, "center"));
            row.cells.add(taskCell(formatCalendarLabel(task.calendarUID, calendarNameByUid), task, "center"));
            row.cells.add(taskCell(join(resourceNames, ", "), task, "center"));
            row.cells.add(taskCell(formatPredecessors(task.predecessors), task, "center"));
            row.cells.add(blankCell());
            List<String> taskBand = dateband.buildDateBand(formatDate(task.start), formatDate(task.finish));
            Set<String> taskBandSet = new LinkedHashSet<String>(taskBand);
            for (String day : displayDateBand) {
                row.cells.add(taskBandSet.contains(day) ? progressMarker(task) : blankBandCell(day, holidaySet, nonWorkingDayTypes));
            }
            sheet.rows.add(row);
        }
    }

    private void appendLegendRows(XlsxSheetLike sheet) {
        sheet.rows.add(emptyRow());
        sheet.rows.add(titleRow("凡例"));
        sheet.rows.add(singleValueRow("━:フェーズ", "#EEF7E8"));
        sheet.rows.add(singleValueRow("◆:マイルストーン", "#FFF4E0"));
        sheet.rows.add(singleValueRow("■:進捗済みタスク", "#8EB9EA"));
        sheet.rows.add(singleValueRow("□:予定タスク", "#D9EAF7"));
        sheet.rows.add(singleValueRow("Mil:マイルストーン", "#FFF4E0"));
        sheet.rows.add(singleValueRow("Sum:サマリ", "#F7EAF0"));
        sheet.rows.add(singleValueRow("Crit:クリティカル", "#F3E1E9"));
        sheet.rows.add(singleValueRow("-:未設定", "#F5F7FA"));
    }

    private void appendSummaryRows(XlsxSheetLike sheet, ProjectModel model, List<String> displayDateBand, Set<String> holidaySet,
            Set<Integer> nonWorkingDayTypes, WbsExportOptions options) {
        sheet.rows.add(emptyRow());
        sheet.rows.add(titleRow("サマリ"));
        sheet.rows.add(summaryRow("表示日", String.valueOf(displayDateBand.size())));
        sheet.rows.add(summaryRow("営業日", String.valueOf(dateband.countBusinessDays(displayDateBand, holidaySet, nonWorkingDayTypes))));
        sheet.rows.add(summaryRow("前日数", options.displayDaysBeforeBaseDate == null ? "-" : String.valueOf(options.displayDaysBeforeBaseDate)));
        sheet.rows.add(summaryRow("後日数", options.displayDaysAfterBaseDate == null ? "-" : String.valueOf(options.displayDaysAfterBaseDate)));
        sheet.rows.add(summaryRow("表示", Boolean.TRUE.equals(options.useBusinessDaysForDisplayRange) ? "営業日" : "暦日"));
        sheet.rows.add(summaryRow("進捗", Boolean.TRUE.equals(options.useBusinessDaysForProgressBand) ? "営業日" : "暦日"));
        sheet.rows.add(summaryRow("基準日", formatDate(model.project.currentDate)));
        sheet.rows.add(summaryRow("タスク", String.valueOf(model.tasks.size())));
        sheet.rows.add(summaryRow("リソース", String.valueOf(model.resources.size())));
        sheet.rows.add(summaryRow("割当", String.valueOf(model.assignments.size())));
        sheet.rows.add(summaryRow("カレンダ", String.valueOf(model.calendars.size())));
    }

    private XlsxRowLike titleRow(String title) {
        XlsxRowLike row = new XlsxRowLike();
        row.height = Integer.valueOf(24);
        for (int index = 0; index < FIXED_COLUMN_COUNT; index++) {
            row.cells.add(blankCell());
        }
        row.cells.set(0, styledCell(title, "left", "center", Boolean.TRUE, Integer.valueOf(14), "#E1EDF8", "thin"));
        return row;
    }

    private XlsxRowLike projectInfoRow(String label, String value, String extraValue) {
        XlsxRowLike row = new XlsxRowLike();
        for (int index = 0; index < FIXED_COLUMN_COUNT; index++) {
            row.cells.add(blankCell());
        }
        row.cells.set(0, styledCell(label, "right", null, Boolean.TRUE, null, null, "thin"));
        row.cells.set(2, styledCell(value, "left", null, null, null, null, "thin"));
        if (extraValue != null) {
            row.cells.set(9, styledCell(extraValue, "left", null, null, null, null, null));
        }
        return row;
    }

    private XlsxRowLike summaryRow(String label, String value) {
        XlsxRowLike row = new XlsxRowLike();
        for (int index = 0; index < FIXED_COLUMN_COUNT; index++) {
            row.cells.add(blankCell());
        }
        row.cells.set(0, styledCell(label, "right", null, Boolean.TRUE, null, null, "thin"));
        row.cells.set(1, styledCell(value, "center", null, Boolean.TRUE, null, null, "thin"));
        return row;
    }

    private XlsxRowLike singleValueRow(String value, String fillColor) {
        XlsxRowLike row = new XlsxRowLike();
        for (int index = 0; index < FIXED_COLUMN_COUNT; index++) {
            row.cells.add(blankCell());
        }
        row.cells.set(0, styledCell(value, "left", null, Boolean.TRUE, null, fillColor, "thin"));
        return row;
    }

    private XlsxRowLike emptyRow() {
        XlsxRowLike row = new XlsxRowLike();
        row.height = Integer.valueOf(28);
        for (int index = 0; index < FIXED_COLUMN_COUNT; index++) {
            row.cells.add(blankCell());
        }
        return row;
    }

    private XlsxCellLike headerCell(String value) {
        return styledCell(value, "center", "center", Boolean.TRUE, null, "#D9EAF7", "thin");
    }

    private XlsxCellLike dateBandCell(String value, String day, String currentDate, Set<String> holidaySet, Set<Integer> nonWorkingDayTypes) {
        String fill = holidaySet.contains(day) ? "#FFE6A7"
                : dateband.isWeeklyNonWorkingDay(day, nonWorkingDayTypes) ? "#EEF3F8"
                        : isCurrentDay(day, currentDate) ? "#D9EAF7" : "#FFFFFF";
        return styledCell(value, "center", "center", null, null, fill, "thin");
    }

    private XlsxCellLike weekdayCell(String value, String day, String currentDate, Set<String> holidaySet, Set<Integer> nonWorkingDayTypes) {
        String fill = (holidaySet.contains(day) || dateband.isWeeklyNonWorkingDay(day, nonWorkingDayTypes)) ? "#EEF3F8"
                : isCurrentDay(day, currentDate) ? "#D9EAF7" : "#FFFFFF";
        return styledCell(value, "center", "center", Boolean.TRUE, null, fill, "thin");
    }

    private XlsxCellLike kindCell(TaskModel task) {
        String fill = task.summary ? "#EEF7E8" : task.milestone ? "#FFF4E0" : "#EEF2F6";
        return styledCell(task.summary ? "フェーズ" : task.milestone ? "マイル" : "タスク", "center", "center", Boolean.TRUE, null, fill, "thin");
    }

    private XlsxCellLike nameCell(TaskModel task) {
        String prefix = task.summary ? "> " : task.milestone ? "  * " : "  - ";
        return styledCell(prefix + safe(task.name, "-"), "left", "center", Boolean.valueOf(task.summary), null, taskFill(task), "thin");
    }

    private XlsxCellLike detailCell(String notes, TaskModel task) {
        return styledCell(notes == null || notes.isEmpty() ? "-" : notes.replace("\n", "\n"), "left", "top", null, null,
                notes == null || notes.isEmpty() ? "#F5F7FA" : taskFill(task), "thin");
    }

    private XlsxCellLike taskCell(String value, TaskModel task, String align) {
        return styledCell(value == null || value.isEmpty() ? "-" : value, align, "center", null, null, taskFill(task), "thin");
    }

    private XlsxCellLike progressCell(Integer percent, TaskModel task) {
        int safePercent = percent == null ? 0 : Math.max(0, Math.min(100, percent.intValue()));
        int filled = safePercent / 10;
        StringBuilder bar = new StringBuilder();
        for (int index = 0; index < 10; index++) {
            bar.append(index < filled ? '#' : '.');
        }
        return styledCell(safePercent + "%\n[" + bar + "]", "center", "center", Boolean.TRUE, null, taskFill(task), "thin");
    }

    private XlsxCellLike progressWorkCell(Integer percent, TaskModel task) {
        if (percent == null) {
            return styledCell("", "center", "center", null, null, taskFill(task), "thin");
        }
        return styledCell(percent + "%", "center", "center", null, null, taskFill(task), "thin");
    }

    private XlsxCellLike flagCell(Boolean enabled, String marker, TaskModel task) {
        return styledCell(Boolean.TRUE.equals(enabled) ? marker : "-", "center", "center", null, null,
                Boolean.TRUE.equals(enabled) ? taskFill(task) : "#F5F7FA", "thin");
    }

    private XlsxCellLike progressMarker(TaskModel task) {
        String value = task.summary ? "━" : task.milestone ? "◆" : "■";
        String fill = task.summary ? "#8EB9EA" : task.milestone ? "#8EB9EA" : "#D9EAF7";
        return styledCell(value, "center", "center", Boolean.TRUE, null, fill, "thin");
    }

    private XlsxCellLike blankBandCell(String day, Set<String> holidaySet, Set<Integer> nonWorkingDayTypes) {
        String fill = holidaySet.contains(day) ? "#FFE6A7"
                : dateband.isWeeklyNonWorkingDay(day, nonWorkingDayTypes) ? "#EEF3F8" : "#FFFFFF";
        return styledCell("", "center", "center", null, null, fill, "thin");
    }

    private XlsxCellLike blankCell() {
        return new XlsxCellLike();
    }

    private XlsxCellLike styledCell(String value, String horizontalAlign, String verticalAlign, Boolean bold, Integer fontSize,
            String fillColor, String border) {
        XlsxCellLike cell = new XlsxCellLike();
        cell.value = value;
        cell.horizontalAlign = horizontalAlign;
        cell.verticalAlign = verticalAlign;
        cell.bold = bold;
        cell.fontSize = fontSize;
        cell.fillColor = fillColor;
        cell.border = border;
        cell.wrapText = value != null && value.indexOf('\n') >= 0 ? Boolean.TRUE : null;
        return cell;
    }

    private String taskFill(TaskModel task) {
        return task.summary ? "#EEF7E8" : task.milestone ? "#FFF4E0" : null;
    }

    private String formatDate(String value) {
        return value == null || value.length() < 10 ? "-" : value.substring(0, 10);
    }

    private String formatShortDate(String value) {
        if (value == null || value.length() < 10) {
            return "";
        }
        return Integer.parseInt(value.substring(5, 7)) + "/" + Integer.parseInt(value.substring(8, 10));
    }

    private String formatWeekday(String value) {
        java.util.Date date = dateband.parseDateOnly(value);
        if (date == null) {
            return "";
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        switch (calendar.get(Calendar.DAY_OF_WEEK)) {
            case Calendar.MONDAY:
                return "Mon";
            case Calendar.TUESDAY:
                return "Tue";
            case Calendar.WEDNESDAY:
                return "Wed";
            case Calendar.THURSDAY:
                return "Thu";
            case Calendar.FRIDAY:
                return "Fri";
            case Calendar.SATURDAY:
                return "Sat";
            default:
                return "Sun";
        }
    }

    private boolean isCurrentDay(String day, String currentDate) {
        return day != null && currentDate != null && currentDate.length() >= 10 && day.equals(currentDate.substring(0, 10));
    }

    private String formatDuration(TaskModel task, Set<String> holidaySet, Set<Integer> nonWorkingDayTypes, Boolean useBusinessDaysForProgressBand) {
        if (Boolean.TRUE.equals(useBusinessDaysForProgressBand)) {
            int businessDays = dateband.enumerateBusinessDays(task.start, task.finish, holidaySet, nonWorkingDayTypes).size();
            return businessDays > 0 ? businessDays + "営業日" : "-";
        }
        int days = dateband.buildDateBand(formatDate(task.start), formatDate(task.finish)).size();
        return days > 0 ? days + "日" : "-";
    }

    private String formatPredecessors(List<PredecessorModel> predecessors) {
        if (predecessors == null || predecessors.isEmpty()) {
            return "-";
        }
        List<String> values = new ArrayList<String>();
        for (PredecessorModel predecessor : predecessors) {
            if (predecessor != null && predecessor.predecessorUid != null) {
                values.add(predecessor.predecessorUid);
            }
        }
        return values.isEmpty() ? "-" : join(values, ", ");
    }

    private String first(List<String> values) {
        return values == null || values.isEmpty() ? "-" : values.get(0);
    }

    private String join(List<String> values, String separator) {
        if (values == null || values.isEmpty()) {
            return "-";
        }
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < values.size(); index++) {
            if (index > 0) {
                builder.append(separator);
            }
            builder.append(values.get(index));
        }
        return builder.toString();
    }

    private String formatCalendarLabel(ProjectModel model) {
        Map<String, String> calendarNameByUid = new LinkedHashMap<String, String>();
        for (CalendarModel calendar : model.calendars) {
            if (calendar != null) {
                calendarNameByUid.put(calendar.uid, calendar.name);
            }
        }
        return formatCalendarLabel(model.project.calendarUID, calendarNameByUid);
    }

    private String formatCalendarLabel(String calendarUid, Map<String, String> calendarNameByUid) {
        if (calendarUid == null || calendarUid.isEmpty()) {
            return "-";
        }
        String name = calendarNameByUid.get(calendarUid);
        return name == null || name.isEmpty() ? calendarUid : calendarUid + " " + name;
    }

    private String safe(String value, String fallback) {
        return value == null || value.isEmpty() ? fallback : value;
    }

    private String formatTimestamp() {
        Calendar calendar = Calendar.getInstance();
        StringBuilder builder = new StringBuilder();
        builder.append(calendar.get(Calendar.YEAR)).append('-');
        appendTwoDigits(builder, calendar.get(Calendar.MONTH) + 1);
        builder.append('-');
        appendTwoDigits(builder, calendar.get(Calendar.DAY_OF_MONTH));
        builder.append(' ');
        appendTwoDigits(builder, calendar.get(Calendar.HOUR_OF_DAY));
        builder.append(':');
        appendTwoDigits(builder, calendar.get(Calendar.MINUTE));
        return builder.toString();
    }

    private void appendTwoDigits(StringBuilder builder, int value) {
        if (value < 10) {
            builder.append('0');
        }
        builder.append(value);
    }

    public static class WbsExportOptions {
        public List<String> holidayDates = new ArrayList<String>();
        public Integer displayDaysBeforeBaseDate;
        public Integer displayDaysAfterBaseDate;
        public Boolean useBusinessDaysForDisplayRange;
        public Boolean useBusinessDaysForProgressBand;
    }
}
