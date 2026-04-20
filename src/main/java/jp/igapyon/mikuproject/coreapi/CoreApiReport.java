/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuproject.coreapi;

import java.util.ArrayList;
import java.util.List;

import jp.igapyon.mikuproject.excelio.ExcelIoUtil;
import jp.igapyon.mikuproject.excelio.ExcelIoZip;
import jp.igapyon.mikuproject.excelio.XlsxWorkbookCodec;
import jp.igapyon.mikuproject.model.ProjectModel;
import jp.igapyon.mikuproject.msprojectxml.MsProjectXml;
import jp.igapyon.mikuproject.wbssvg.WbsSvg.NativeSvgOptions;
import jp.igapyon.mikuproject.wbssvg.WbsSvg.MonthlyCalendarEntry;
import jp.igapyon.mikuproject.wbssvg.WbsSvg.MonthlyCalendarSvgArchive;
import jp.igapyon.mikuproject.wbsmarkdown.WbsMarkdown;
import jp.igapyon.mikuproject.wbsmarkdown.WbsMarkdown.WbsMarkdownOptions;
import jp.igapyon.mikuproject.wbsxlsx.WbsXlsx;
import jp.igapyon.mikuproject.wbsxlsx.WbsXlsx.WbsExportOptions;

public class CoreApiReport {
    private final MsProjectXml msProjectXml = new MsProjectXml();
    private final WbsMarkdown wbsMarkdown = new WbsMarkdown();
    private final WbsXlsx wbsXlsx = new WbsXlsx();
    private final XlsxWorkbookCodec xlsxWorkbookCodec = new XlsxWorkbookCodec();
    private final ExcelIoZip zip = new ExcelIoZip();

    public List<ReportEntry> exportAllReportEntries(ProjectModel model) {
        return exportAllReportEntries(model, null, null, null);
    }

    public List<ReportEntry> exportAllReportEntries(ProjectModel model, WbsMarkdownOptions options) {
        return exportAllReportEntries(model, options, null, null);
    }

    public List<ReportEntry> exportAllReportEntries(ProjectModel model, WbsMarkdownOptions markdownOptions,
            WbsExportOptions xlsxOptions, NativeSvgOptions svgOptions) {
        List<ReportEntry> entries = new ArrayList<ReportEntry>();
        entries.add(new ReportEntry("wbs.xlsx", xlsxWorkbookCodec.exportWorkbook(wbsXlsx.exportWbsWorkbook(model, xlsxOptions))));
        entries.add(new ReportEntry("wbs.md", ExcelIoUtil.encodeUtf8(wbsMarkdown.exportWbsMarkdown(model, markdownOptions) + "\n")));
        entries.add(new ReportEntry("mermaid.mmd", ExcelIoUtil.encodeUtf8(msProjectXml.exportMermaidGantt(model) + "\n")));
        entries.add(new ReportEntry("daily.svg", ExcelIoUtil.encodeUtf8(msProjectXml.exportNativeSvg(model, svgOptions) + "\n")));
        entries.add(new ReportEntry("weekly.svg", ExcelIoUtil.encodeUtf8(msProjectXml.exportWeeklyNativeSvg(model, svgOptions) + "\n")));
        MonthlyCalendarSvgArchive monthlyArchive = msProjectXml.exportMonthlyWbsCalendarSvgArchive(model, svgOptions);
        for (MonthlyCalendarEntry entry : monthlyArchive.entries) {
            entries.add(new ReportEntry("monthly-calendar/" + entry.fileName, ExcelIoUtil.encodeUtf8(entry.svg)));
        }
        return entries;
    }

    public byte[] packZipEntries(List<ReportEntry> entries) {
        List<ExcelIoZip.ZipEntryData> zipEntries = new ArrayList<ExcelIoZip.ZipEntryData>();
        for (ReportEntry entry : entries) {
            zipEntries.add(new ExcelIoZip.ZipEntryData(entry.name, entry.data));
        }
        return zip.packZip(zipEntries, ExcelIoZip.FIXED_2025_01_01_MOD_TIME, ExcelIoZip.FIXED_2025_01_01_MOD_DATE);
    }

    public static class ReportEntry {
        public final String name;
        public final byte[] data;

        public ReportEntry(String name, byte[] data) {
            this.name = name;
            this.data = data;
        }
    }
}
