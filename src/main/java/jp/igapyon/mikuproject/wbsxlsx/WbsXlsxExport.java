/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuproject.wbsxlsx;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import jp.igapyon.mikuproject.model.ProjectModel;
import jp.igapyon.mikuproject.msprojectxml.MsProjectXml;
import jp.igapyon.mikuproject.projectxlsx.XlsxSheetLike;
import jp.igapyon.mikuproject.projectxlsx.XlsxWorkbookLike;
import jp.igapyon.mikuproject.wbsdateband.WbsDateband;

public class WbsXlsxExport {
    private final MsProjectXml msProjectXml;
    private final WbsDateband dateband;
    private final WbsXlsxLayout layout;
    private final WbsXlsxBase base;
    private final WbsXlsxCells cells;
    private final WbsXlsxSections sections;
    private final WbsXlsxTaskmeta taskmeta;

    public WbsXlsxExport(MsProjectXml msProjectXml, WbsDateband dateband, WbsXlsxLayout layout) {
        this.msProjectXml = msProjectXml;
        this.dateband = dateband;
        this.layout = layout;
        this.base = new WbsXlsxBase(dateband);
        this.cells = new WbsXlsxCells(base.getFixedColumnCount(), dateband, base);
        this.sections = new WbsXlsxSections(dateband, base, cells);
        this.taskmeta = new WbsXlsxTaskmeta();
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
        base.appendColumns(sheet, displayDateBand.size());
        workbook.sheets.add(sheet);

        WbsXlsxTaskmeta.Taskmeta collectedTaskmeta = taskmeta.collectTaskmeta(normalized);
        sections.appendProjectInfoRows(sheet, normalized, holidaySet, layout);
        sections.appendDateBandRows(sheet, displayDateBand, normalized.project.currentDate, holidaySet, nonWorkingDayTypes);
        sections.appendTaskRows(sheet, normalized, displayDateBand, holidaySet, nonWorkingDayTypes, actualOptions.useBusinessDaysForProgressBand,
                collectedTaskmeta);
        sections.appendLegendRows(sheet);
        sections.appendSummaryRows(sheet, normalized, displayDateBand, holidaySet, nonWorkingDayTypes, actualOptions);
        return workbook;
    }

    public static class WbsExportOptions {
        public List<String> holidayDates = new ArrayList<String>();
        public Integer displayDaysBeforeBaseDate;
        public Integer displayDaysAfterBaseDate;
        public Boolean useBusinessDaysForDisplayRange;
        public Boolean useBusinessDaysForProgressBand;
    }
}
