/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuproject.coreapi;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import jp.igapyon.mikuproject.excelio.ExcelIoUtil;
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

    public List<ReportEntry> exportAllReportEntries(ProjectModel model) {
        return exportAllReportEntries(model, null, null, null);
    }

    public List<ReportEntry> exportAllReportEntries(ProjectModel model, WbsMarkdownOptions options) {
        return exportAllReportEntries(model, options, null, null);
    }

    public List<ReportEntry> exportAllReportEntries(ProjectModel model, WbsMarkdownOptions markdownOptions,
            WbsExportOptions xlsxOptions, NativeSvgOptions svgOptions) {
        List<ReportEntry> entries = new ArrayList<ReportEntry>();
        entries.add(new ReportEntry("wbs.md", ExcelIoUtil.encodeUtf8(wbsMarkdown.exportWbsMarkdown(model, markdownOptions) + "\n")));
        entries.add(new ReportEntry("mermaid.mmd", ExcelIoUtil.encodeUtf8(msProjectXml.exportMermaidGantt(model) + "\n")));
        entries.add(new ReportEntry("wbs.xlsx", xlsxWorkbookCodec.exportWorkbook(wbsXlsx.exportWbsWorkbook(model, xlsxOptions))));
        entries.add(new ReportEntry("daily.svg", ExcelIoUtil.encodeUtf8(msProjectXml.exportNativeSvg(model, svgOptions) + "\n")));
        entries.add(new ReportEntry("weekly.svg", ExcelIoUtil.encodeUtf8(msProjectXml.exportWeeklyNativeSvg(model, svgOptions) + "\n")));
        MonthlyCalendarSvgArchive monthlyArchive = msProjectXml.exportMonthlyWbsCalendarSvgArchive(model, svgOptions);
        for (MonthlyCalendarEntry entry : monthlyArchive.entries) {
            entries.add(new ReportEntry("monthly-calendar/" + entry.fileName, ExcelIoUtil.encodeUtf8(entry.svg)));
        }
        return entries;
    }

    public byte[] packZipEntries(List<ReportEntry> entries) {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        ZipOutputStream zip = null;
        try {
            zip = new ZipOutputStream(buffer);
            for (ReportEntry entry : entries) {
                zip.putNextEntry(new ZipEntry(entry.name));
                zip.write(entry.data);
                zip.closeEntry();
            }
            zip.finish();
            return buffer.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("report zip の生成に失敗しました", ex);
        } finally {
            if (zip != null) {
                try {
                    zip.close();
                } catch (IOException ex) {
                    // ignore
                }
            }
        }
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
