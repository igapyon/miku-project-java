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
        XlsxRowLike row = new XlsxRowLike();
        row.height = Integer.valueOf(24);
        for (int index = 0; index < fixedColumnCount; index++) {
            row.cells.add(blankCell());
        }
        row.cells.set(0, styledCell(title, "left", "center", Boolean.TRUE, Integer.valueOf(14), "#E1EDF8", "thin"));
        return row;
    }

    public XlsxRowLike projectInfoRow(String label, String value, String extraValue) {
        XlsxRowLike row = new XlsxRowLike();
        for (int index = 0; index < fixedColumnCount; index++) {
            row.cells.add(blankCell());
        }
        row.cells.set(0, styledCell(label, "right", null, Boolean.TRUE, null, null, "thin"));
        row.cells.set(2, styledCell(value, "left", null, null, null, null, "thin"));
        if (extraValue != null) {
            row.cells.set(9, styledCell(extraValue, "left", null, null, null, null, null));
        }
        return row;
    }

    public XlsxRowLike summaryRow(String label, String value) {
        XlsxRowLike row = new XlsxRowLike();
        for (int index = 0; index < fixedColumnCount; index++) {
            row.cells.add(blankCell());
        }
        row.cells.set(0, styledCell(label, "right", null, Boolean.TRUE, null, null, "thin"));
        row.cells.set(1, styledCell(value, "center", null, Boolean.TRUE, null, null, "thin"));
        return row;
    }

    public XlsxRowLike singleValueRow(String value, String fillColor) {
        XlsxRowLike row = new XlsxRowLike();
        for (int index = 0; index < fixedColumnCount; index++) {
            row.cells.add(blankCell());
        }
        row.cells.set(0, styledCell(value, "left", null, Boolean.TRUE, null, fillColor, "thin"));
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
        return styledCell(value, "center", "center", Boolean.TRUE, null, "#D9EAF7", "thin");
    }

    public XlsxCellLike dateBandCell(String value, String day, String currentDate, Set<String> holidaySet, Set<Integer> nonWorkingDayTypes) {
        String fill = holidaySet.contains(day) ? "#FFE6A7"
                : dateband.isWeeklyNonWorkingDay(day, nonWorkingDayTypes) ? "#EEF3F8"
                        : base.isCurrentDay(day, currentDate) ? "#D9EAF7" : "#FFFFFF";
        return styledCell(value, "center", "center", null, null, fill, "thin");
    }

    public XlsxCellLike weekdayCell(String value, String day, String currentDate, Set<String> holidaySet, Set<Integer> nonWorkingDayTypes) {
        String fill = (holidaySet.contains(day) || dateband.isWeeklyNonWorkingDay(day, nonWorkingDayTypes)) ? "#EEF3F8"
                : base.isCurrentDay(day, currentDate) ? "#D9EAF7" : "#FFFFFF";
        return styledCell(value, "center", "center", Boolean.TRUE, null, fill, "thin");
    }

    public XlsxCellLike kindCell(TaskModel task) {
        String fill = task.summary ? "#EEF7E8" : task.milestone ? "#FFF4E0" : "#EEF2F6";
        return styledCell(task.summary ? "フェーズ" : task.milestone ? "マイル" : "タスク", "center", "center", Boolean.TRUE, null, fill, "thin");
    }

    public XlsxCellLike nameCell(TaskModel task) {
        String prefix = task.summary ? "> " : task.milestone ? "  * " : "  - ";
        return styledCell(prefix + base.safe(task.name, "-"), "left", "center", Boolean.valueOf(task.summary), null, taskFill(task), "thin");
    }

    public XlsxCellLike detailCell(String notes, TaskModel task) {
        return styledCell(notes == null || notes.isEmpty() ? "-" : notes.replace("\n", "\n"), "left", "top", null, null,
                notes == null || notes.isEmpty() ? "#F5F7FA" : taskFill(task), "thin");
    }

    public XlsxCellLike taskCell(String value, TaskModel task, String align) {
        return styledCell(value == null || value.isEmpty() ? "-" : value, align, "center", null, null, taskFill(task), "thin");
    }

    public XlsxCellLike progressCell(Integer percent, TaskModel task) {
        int safePercent = percent == null ? 0 : Math.max(0, Math.min(100, percent.intValue()));
        int filled = safePercent / 10;
        StringBuilder bar = new StringBuilder();
        for (int index = 0; index < 10; index++) {
            bar.append(index < filled ? '#' : '.');
        }
        return styledCell(safePercent + "%\n[" + bar + "]", "center", "center", Boolean.TRUE, null, taskFill(task), "thin");
    }

    public XlsxCellLike progressWorkCell(Integer percent, TaskModel task) {
        if (percent == null) {
            return styledCell("", "center", "center", null, null, taskFill(task), "thin");
        }
        return styledCell(percent + "%", "center", "center", null, null, taskFill(task), "thin");
    }

    public XlsxCellLike flagCell(Boolean enabled, String marker, TaskModel task) {
        return styledCell(Boolean.TRUE.equals(enabled) ? marker : "-", "center", "center", null, null,
                Boolean.TRUE.equals(enabled) ? taskFill(task) : "#F5F7FA", "thin");
    }

    public XlsxCellLike progressMarker(TaskModel task) {
        String value = task.summary ? "━" : task.milestone ? "◆" : "■";
        String fill = task.summary ? "#8EB9EA" : task.milestone ? "#8EB9EA" : "#D9EAF7";
        return styledCell(value, "center", "center", Boolean.TRUE, null, fill, "thin");
    }

    public XlsxCellLike blankBandCell(String day, Set<String> holidaySet, Set<Integer> nonWorkingDayTypes) {
        String fill = holidaySet.contains(day) ? "#FFE6A7"
                : dateband.isWeeklyNonWorkingDay(day, nonWorkingDayTypes) ? "#EEF3F8" : "#FFFFFF";
        return styledCell("", "center", "center", null, null, fill, "thin");
    }

    public XlsxCellLike blankCell() {
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
}
