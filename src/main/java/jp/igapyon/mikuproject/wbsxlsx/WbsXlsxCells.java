/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuproject.wbsxlsx;

import java.util.Set;

import jp.igapyon.mikuproject.model.TaskModel;
import jp.igapyon.mikuproject.projectxlsx.XlsxCellLike;
import jp.igapyon.mikuproject.projectxlsx.XlsxRowLike;
import jp.igapyon.mikuproject.wbsdateband.WbsDateband;

public class WbsXlsxCells {
    private final int fixedColumnCount;
    private final WbsDateband dateband;
    private final WbsXlsxBase base;

    public WbsXlsxCells(int fixedColumnCount, WbsDateband dateband, WbsXlsxBase base) {
        this.fixedColumnCount = fixedColumnCount;
        this.dateband = dateband;
        this.base = base;
    }

    public XlsxRowLike titleRow(String title) {
        return titleRow(title, 5);
    }

    public XlsxRowLike titleRow(String title, int cellCount) {
        XlsxRowLike row = new XlsxRowLike();
        row.height = Integer.valueOf(24);
        for (int index = 0; index < cellCount; index++) {
            row.cells.add(styledCell("", null, null, null, null, "#E1EDF8", "thin"));
        }
        row.cells.set(0, styledCell(title, "left", null, Boolean.TRUE, Integer.valueOf(14), "#E1EDF8", "thin"));
        return row;
    }

    public XlsxRowLike projectInfoRow(String label, String value, String extraValue) {
        XlsxRowLike row = new XlsxRowLike();
        for (int index = 0; index < fixedColumnCount; index++) {
            row.cells.add(blankCell());
        }
        row.height = Integer.valueOf(22);
        String fill = ("プロジェクト名".equals(label) || "カレンダ".equals(label)) ? "#E8F4F1" : "#FDF1E4";
        row.cells.set(0, styledCell(label, "right", null, Boolean.TRUE, null, fill, "thin"));
        row.cells.set(1, styledCell("", null, null, null, null, fill, "thin"));
        row.cells.set(2, styledCell(value, isNumeric(value) ? "center" : "left", null, Boolean.TRUE, null, fill, "thin"));
        row.cells.set(3, styledCell("", null, null, null, null, fill, "thin"));
        row.cells.set(4, styledCell("", null, null, null, null, fill, "thin"));
        if (extraValue != null) {
            row.cells.set(9, styledCell(extraValue, "left", "center", null, null, null, null));
        }
        return row;
    }

    public XlsxRowLike summaryRow(String label, String value) {
        XlsxRowLike row = new XlsxRowLike();
        for (int index = 0; index < fixedColumnCount; index++) {
            row.cells.add(blankCell());
        }
        row.height = Integer.valueOf(22);
        String fill = isCountSummaryLabel(label) ? "#E8F4F1" : "#FDF1E4";
        row.cells.set(0, styledCell(label, "right", null, Boolean.TRUE, null, fill, "thin"));
        row.cells.set(1, styledCell(value, isNumeric(value) ? "center" : "left", null, Boolean.TRUE, null, fill, "thin"));
        row.cells.set(2, styledCell("", null, null, null, null, fill, "thin"));
        return row;
    }

    public XlsxRowLike singleValueRow(String value, String fillColor) {
        XlsxRowLike row = new XlsxRowLike();
        for (int index = 0; index < fixedColumnCount; index++) {
            row.cells.add(blankCell());
        }
        row.height = Integer.valueOf(24);
        row.cells.set(0, styledCell(value, "center", null, Boolean.TRUE, null, fillColor, "thin"));
        row.cells.set(1, styledCell("", null, null, null, null, fillColor, "thin"));
        row.cells.set(2, styledCell("", null, null, null, null, fillColor, "thin"));
        return row;
    }

    public XlsxRowLike emptyRow() {
        XlsxRowLike row = new XlsxRowLike();
        row.height = Integer.valueOf(28);
        for (int index = 0; index < fixedColumnCount; index++) {
            row.cells.add(blankCell());
        }
        return row;
    }

    public XlsxCellLike headerCell(String value) {
        return styledCell(value, "center", "center", Boolean.TRUE, null, headerFill(value), "thin");
    }

    public XlsxCellLike dateBandCell(String value, String day, String currentDate, Set<String> holidaySet, Set<Integer> nonWorkingDayTypes) {
        String fill = base.isCurrentDay(day, currentDate) ? "#FFE6A7"
                : holidaySet.contains(day) ? "#FCE4EC"
                        : dateband.isWeeklyNonWorkingDay(day, nonWorkingDayTypes) ? "#EEF3F8" : "#D9EAF7";
        return styledCell(value, "center", "center", Boolean.TRUE, null, fill, "thin");
    }

    public XlsxCellLike weekdayCell(String value, String day, String currentDate, Set<String> holidaySet, Set<Integer> nonWorkingDayTypes) {
        String fill = holidaySet.contains(day) ? "#FCE4EC"
                : dateband.isWeeklyNonWorkingDay(day, nonWorkingDayTypes) ? "#EEF3F8"
                        : base.isCurrentDay(day, currentDate) ? "#FFE6A7" : "#D9EAF7";
        return styledCell(value, "center", "center", Boolean.TRUE, null, fill, "thin");
    }

    public XlsxCellLike kindCell(TaskModel task) {
        String fill = task.summary ? "#EEF7E8" : task.milestone ? "#FFF4E0" : "#EEF2F6";
        return styledCell(task.summary ? "フェーズ" : task.milestone ? "マイル" : "タスク", "center", "center", Boolean.TRUE, null, fill, "thin");
    }

    public XlsxCellLike identifierCell(TaskModel task, String value) {
        if (value == null || value.isEmpty()) {
            return blankCell();
        }
        String fill = task.summary ? "#EEF7E8" : task.milestone ? "#FFF4E0" : "#F7F9FC";
        return styledCell(value, "center", "center", Boolean.valueOf(task.summary || task.milestone), null, fill, "thin");
    }

    public XlsxCellLike nameCell(TaskModel task) {
        String prefix = task.summary ? "> " : task.milestone ? "* " : "- ";
        return styledCell(prefix + base.safe(task.name, "-"), "left", "center", Boolean.valueOf(task.summary), null, taskFill(task), "thin");
    }

    public XlsxCellLike detailCell(String notes, TaskModel task) {
        XlsxCellLike cell = styledCell(notes == null || notes.trim().isEmpty() ? "-" : notes.trim(), "left", "center", null, null,
                notes == null || notes.isEmpty() ? "#F5F7FA" : taskFill(task), "thin");
        if (notes != null && !notes.trim().isEmpty()) {
            cell.wrapText = Boolean.TRUE;
        }
        return cell;
    }

    public XlsxCellLike taskCell(String value, TaskModel task, String align) {
        if (value == null || value.isEmpty()) {
            return blankCell();
        }
        XlsxCellLike cell = styledCell(value, align, "center", Boolean.valueOf(task.summary || task.milestone), null, taskFillForAlign(task, align), "thin");
        cell.wrapText = Boolean.TRUE;
        return cell;
    }

    public XlsxCellLike progressCell(Integer percent, TaskModel task) {
        if (percent == null) {
            XlsxCellLike cell = styledCell("", "center", "center", Boolean.valueOf(task.summary || task.milestone), null,
                    task.summary ? "#EEF7E8" : task.milestone ? "#FFF4E0" : "#FCF8FB", "thin");
            cell.wrapText = Boolean.TRUE;
            return cell;
        }
        int safePercent = percent == null ? 0 : Math.max(0, Math.min(100, percent.intValue()));
        int filled = Math.round(safePercent / 10.0f);
        StringBuilder bar = new StringBuilder();
        for (int index = 0; index < 10; index++) {
            bar.append(index < filled ? '#' : '-');
        }
        String value = String.format(java.util.Locale.ROOT, "%3d%%\n[%s]", Integer.valueOf(safePercent), bar.toString());
        return styledCell(value, "center", "center", Boolean.valueOf(task.summary || task.milestone), null,
                task.summary ? "#EEF7E8" : task.milestone ? "#FFF4E0" : "#FCF8FB", "thin");
    }

    public XlsxCellLike progressWorkCell(Integer percent, TaskModel task) {
        return progressCell(percent, task);
    }

    public XlsxCellLike flagCell(Boolean enabled, String marker, TaskModel task) {
        return styledCell(Boolean.TRUE.equals(enabled) ? marker : "", "center", "center", Boolean.valueOf(Boolean.TRUE.equals(enabled)), null,
                task.summary ? "#EEF7E8" : task.milestone ? "#FFF4E0" : null, "thin");
    }

    public XlsxCellLike progressMarker(TaskModel task, boolean complete, String day, String currentDate) {
        String value = task.summary ? "━" : task.milestone ? "◆" : complete ? "■" : "□";
        String fill = complete ? (base.isCurrentDay(day, currentDate) ? "#6F9FD8" : "#8EB9EA")
                : (base.isCurrentDay(day, currentDate) ? "#C9DFF8" : "#D9EFFF");
        return styledCell(value, "center", "center", null, null, fill, "thin");
    }

    public XlsxCellLike blankBandCell(String day, String currentDate, Set<String> holidaySet, Set<Integer> nonWorkingDayTypes) {
        String fill = base.isCurrentDay(day, currentDate) ? "#FFE6A7"
                : holidaySet.contains(day) ? "#FCE4EC"
                        : dateband.isWeeklyNonWorkingDay(day, nonWorkingDayTypes) ? "#EEF3F8" : null;
        return styledCell("", "center", "center", null, null, fill, "thin");
    }

    public XlsxCellLike blankCell() {
        return new XlsxCellLike();
    }

    public XlsxCellLike dividerCell() {
        return styledCell("", "center", "center", null, null, "#D9E2EA", "thin");
    }

    public XlsxCellLike styledReferenceCell(String value, TaskModel task, String align) {
        boolean placeholder = value == null || value.isEmpty() || "-".equals(value);
        String display = placeholder ? "-" : value;
        String fill = placeholder ? "#F5F7FA" : task.summary ? "#EEF7E8" : task.milestone ? "#FFF4E0" : "#F8FBFB";
        return styledCell(display, placeholder ? "center" : align, "center", Boolean.valueOf(task.summary || task.milestone), null, fill, "thin");
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
        return task.summary ? "#EEF7E8" : task.milestone ? "#FFF4E0" : "#FBFCFE";
    }

    private String taskFillForAlign(TaskModel task, String align) {
        if (task.summary) {
            return "#EEF7E8";
        }
        if (task.milestone) {
            return "#FFF4E0";
        }
        if ("left".equals(align)) {
            return "#FBFCFE";
        }
        if ("center".equals(align)) {
            return "#FCFAF7";
        }
        return null;
    }

    private String headerFill(String label) {
        if ("UID".equals(label) || "ID".equals(label)) {
            return "#E1EDF8";
        }
        if ("WBS".equals(label) || "種別".equals(label) || "階層".equals(label) || "名称".equals(label)) {
            return "#E6F0DF";
        }
        if ("開始".equals(label) || "終了".equals(label) || "期間".equals(label)) {
            return "#FDE7D3";
        }
        if ("進捗".equals(label) || "作業進捗".equals(label) || "マイル".equals(label) || "サマリ".equals(label) || "クリティカル".equals(label)) {
            return "#FBE4EC";
        }
        if ("担当".equals(label) || "カレンダ".equals(label) || "リソース".equals(label) || "先行".equals(label)) {
            return "#E2F1EF";
        }
        return "#D9EAF7";
    }

    private boolean isNumeric(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        for (int index = 0; index < value.length(); index++) {
            if (!Character.isDigit(value.charAt(index))) {
                return false;
            }
        }
        return true;
    }

    private boolean isCountSummaryLabel(String label) {
        return "タスク".equals(label) || "リソース".equals(label) || "割当".equals(label) || "カレンダ".equals(label);
    }
}
