/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuproject.wbsxlsx;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import jp.igapyon.mikuproject.model.ProjectModel;
import jp.igapyon.mikuproject.model.TaskModel;
import jp.igapyon.mikuproject.projectxlsx.XlsxRowLike;
import jp.igapyon.mikuproject.projectxlsx.XlsxSheetLike;
import jp.igapyon.mikuproject.wbsdateband.WbsDateband;

public class WbsXlsxSections {
    private final WbsDateband dateband;
    private final WbsXlsxBase base;
    private final WbsXlsxCells cells;

    public WbsXlsxSections(WbsDateband dateband, WbsXlsxBase base, WbsXlsxCells cells) {
        this.dateband = dateband;
        this.base = base;
        this.cells = cells;
    }

    public void appendProjectInfoRows(XlsxSheetLike sheet, ProjectModel model, Set<String> holidaySet, WbsXlsxLayout layout) {
        sheet.rows.add(cells.titleRow("プロジェクト情報"));
        sheet.mergedRanges.add(layout.range("A1", "E1"));
        sheet.rows.add(cells.projectInfoRow("プロジェクト名", base.safe(model.project.name, "-"), "出力日時 " + base.formatTimestamp()));
        sheet.rows.add(cells.projectInfoRow("カレンダ", base.formatCalendarLabel(model), null));
        sheet.rows.add(cells.projectInfoRow("開始日", base.formatDate(model.project.startDate), null));
        sheet.rows.add(cells.projectInfoRow("終了日", base.formatDate(model.project.finishDate), null));
        sheet.rows.add(cells.projectInfoRow("現在日", base.formatDate(model.project.currentDate), null));
        sheet.rows.add(cells.projectInfoRow("祝日", String.valueOf(holidaySet.size()), null));
    }

    public void appendDateBandRows(XlsxSheetLike sheet, List<String> displayDateBand, String currentDate, Set<String> holidaySet,
            Set<Integer> nonWorkingDayTypes) {
        XlsxRowLike dateRow = new XlsxRowLike();
        XlsxRowLike weekdayRow = new XlsxRowLike();
        for (int index = 0; index < base.getFixedColumnCount(); index++) {
            dateRow.cells.add(cells.blankCell());
            weekdayRow.cells.add(cells.blankCell());
        }
        String[] fixedHeaders = new String[] {
                "UID", "ID", "WBS", "種別", "階層", "名称", "開始", "終了", "期間", "タスク詳細", "進捗", "作業進捗", "マイル", "サマリ", "クリティカル",
                "担当", "カレンダ", "リソース", "先行", ""
        };
        for (String header : fixedHeaders) {
            dateRow.cells.add(cells.blankCell());
        }
        weekdayRow.cells.clear();
        for (String header : fixedHeaders) {
            weekdayRow.cells.add(cells.headerCell(header));
        }
        dateRow.cells.subList(base.getFixedColumnCount(), dateRow.cells.size()).clear();
        for (String day : displayDateBand) {
            dateRow.cells.add(cells.dateBandCell(base.formatShortDate(day), day, currentDate, holidaySet, nonWorkingDayTypes));
            weekdayRow.cells.add(cells.weekdayCell(base.formatWeekday(day), day, currentDate, holidaySet, nonWorkingDayTypes));
        }
        sheet.rows.add(dateRow);
        sheet.rows.add(weekdayRow);
    }

    public void appendTaskRows(XlsxSheetLike sheet, ProjectModel model, List<String> displayDateBand, Set<String> holidaySet,
            Set<Integer> nonWorkingDayTypes, Boolean useBusinessDaysForProgressBand, WbsXlsxTaskmeta.Taskmeta taskmeta) {
        for (TaskModel task : model.tasks) {
            XlsxRowLike row = new XlsxRowLike();
            row.cells.add(cells.taskCell(task.uid, task, "center"));
            row.cells.add(cells.taskCell(task.id, task, "center"));
            row.cells.add(cells.taskCell(base.safe(task.wbs != null ? task.wbs : task.outlineNumber, "-"), task, "center"));
            row.cells.add(cells.kindCell(task));
            row.cells.add(cells.taskCell(task.outlineLevel == null ? "-" : String.valueOf(task.outlineLevel), task, "center"));
            row.cells.add(cells.nameCell(task));
            row.cells.add(cells.taskCell(base.formatDate(task.start), task, "left"));
            row.cells.add(cells.taskCell(base.formatDate(task.finish), task, "left"));
            row.cells.add(cells.taskCell(base.formatDuration(task, holidaySet, nonWorkingDayTypes, useBusinessDaysForProgressBand), task, "center"));
            row.cells.add(cells.detailCell(task.notes, task));
            row.cells.add(cells.progressCell(task.percentComplete, task));
            row.cells.add(cells.progressWorkCell(task.percentWorkComplete, task));
            row.cells.add(cells.flagCell(task.milestone, "Mil", task));
            row.cells.add(cells.flagCell(task.summary, "Sum", task));
            row.cells.add(cells.flagCell(task.critical, "Crit", task));
            List<String> resourceNames = taskmeta.resourceNamesByTaskUid.get(task.uid);
            row.cells.add(cells.taskCell(base.first(resourceNames), task, "center"));
            row.cells.add(cells.taskCell(base.formatCalendarLabel(task.calendarUID, taskmeta.calendarNameByUid), task, "center"));
            row.cells.add(cells.taskCell(base.join(resourceNames, ", "), task, "center"));
            row.cells.add(cells.taskCell(base.formatPredecessors(task.predecessors), task, "center"));
            row.cells.add(cells.blankCell());
            List<String> taskBand = dateband.buildDateBand(base.formatDate(task.start), base.formatDate(task.finish));
            Set<String> taskBandSet = new LinkedHashSet<String>(taskBand);
            for (String day : displayDateBand) {
                row.cells.add(taskBandSet.contains(day) ? cells.progressMarker(task) : cells.blankBandCell(day, holidaySet, nonWorkingDayTypes));
            }
            sheet.rows.add(row);
        }
    }

    public void appendLegendRows(XlsxSheetLike sheet) {
        sheet.rows.add(cells.emptyRow());
        sheet.rows.add(cells.titleRow("凡例"));
        sheet.rows.add(cells.singleValueRow("━:フェーズ", "#EEF7E8"));
        sheet.rows.add(cells.singleValueRow("◆:マイルストーン", "#FFF4E0"));
        sheet.rows.add(cells.singleValueRow("■:進捗済みタスク", "#8EB9EA"));
        sheet.rows.add(cells.singleValueRow("□:予定タスク", "#D9EAF7"));
        sheet.rows.add(cells.singleValueRow("Mil:マイルストーン", "#FFF4E0"));
        sheet.rows.add(cells.singleValueRow("Sum:サマリ", "#F7EAF0"));
        sheet.rows.add(cells.singleValueRow("Crit:クリティカル", "#F3E1E9"));
        sheet.rows.add(cells.singleValueRow("-:未設定", "#F5F7FA"));
    }

    public void appendSummaryRows(XlsxSheetLike sheet, ProjectModel model, List<String> displayDateBand, Set<String> holidaySet,
            Set<Integer> nonWorkingDayTypes, WbsXlsxExport.WbsExportOptions options) {
        sheet.rows.add(cells.emptyRow());
        sheet.rows.add(cells.titleRow("サマリ"));
        sheet.rows.add(cells.summaryRow("表示日", String.valueOf(displayDateBand.size())));
        sheet.rows.add(cells.summaryRow("営業日", String.valueOf(dateband.countBusinessDays(displayDateBand, holidaySet, nonWorkingDayTypes))));
        sheet.rows.add(cells.summaryRow("前日数", options.displayDaysBeforeBaseDate == null ? "-" : String.valueOf(options.displayDaysBeforeBaseDate)));
        sheet.rows.add(cells.summaryRow("後日数", options.displayDaysAfterBaseDate == null ? "-" : String.valueOf(options.displayDaysAfterBaseDate)));
        sheet.rows.add(cells.summaryRow("表示", Boolean.TRUE.equals(options.useBusinessDaysForDisplayRange) ? "営業日" : "暦日"));
        sheet.rows.add(cells.summaryRow("進捗", Boolean.TRUE.equals(options.useBusinessDaysForProgressBand) ? "営業日" : "暦日"));
        sheet.rows.add(cells.summaryRow("基準日", base.formatDate(model.project.currentDate)));
        sheet.rows.add(cells.summaryRow("タスク", String.valueOf(model.tasks.size())));
        sheet.rows.add(cells.summaryRow("リソース", String.valueOf(model.resources.size())));
        sheet.rows.add(cells.summaryRow("割当", String.valueOf(model.assignments.size())));
        sheet.rows.add(cells.summaryRow("カレンダ", String.valueOf(model.calendars.size())));
    }
}
