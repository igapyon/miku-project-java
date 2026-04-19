/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuproject.coreapi;

import java.util.List;

import jp.igapyon.mikuproject.model.ProjectModel;
import jp.igapyon.mikuproject.msprojectxml.MsProjectXml;
import jp.igapyon.mikuproject.projectxlsx.XlsxWorkbookLike;
import jp.igapyon.mikuproject.wbssvg.WbsSvg.NativeSvgOptions;
import jp.igapyon.mikuproject.wbssvg.WbsSvg.MonthlyCalendarSvgArchive;
import jp.igapyon.mikuproject.wbsmarkdown.WbsMarkdown;
import jp.igapyon.mikuproject.wbsmarkdown.WbsMarkdown.WbsMarkdownOptions;
import jp.igapyon.mikuproject.wbsxlsx.WbsXlsx;
import jp.igapyon.mikuproject.wbsxlsx.WbsXlsx.WbsExportOptions;

public class CoreApiReportAdapters {
    private final CoreApiReport coreApiReport = new CoreApiReport();
    private final CoreApiMsproject msproject = new CoreApiMsproject();
    private final CoreApiWorkbookXlsx workbookXlsx = new CoreApiWorkbookXlsx();
    private final WbsMarkdown wbsMarkdown = new WbsMarkdown();
    private final WbsXlsx wbsXlsx = new WbsXlsx();
    private final MsProjectXml msProjectXml = new MsProjectXml();

    public final ReportApi report = new ReportApi();

    public class ReportApi {
        public final AllApi all = new AllApi();
        public final WbsXlsxApi wbsXlsx = new WbsXlsxApi();
        public final SvgApi svg = new SvgApi();
        public final WbsMarkdownApi wbsMarkdown = new WbsMarkdownApi();
        public final MermaidApi mermaid = new MermaidApi();
    }

    public class AllApi {
        public ReportBundle export(ProjectModel model) {
            return export(model, null, null, null);
        }

        public ReportBundle export(ProjectModel model, WbsMarkdownOptions options) {
            return export(model, options, null, null);
        }

        public ReportBundle export(ProjectModel model, WbsMarkdownOptions markdownOptions, WbsExportOptions xlsxOptions,
                NativeSvgOptions svgOptions) {
            List<CoreApiReport.ReportEntry> entries = coreApiReport.exportAllReportEntries(model, markdownOptions, xlsxOptions, svgOptions);
            ReportBundle bundle = new ReportBundle();
            bundle.entries.addAll(entries);
            bundle.zipBytes = coreApiReport.packZipEntries(entries);
            return bundle;
        }
    }

    public class WbsXlsxApi {
        public XlsxWorkbookLike exportWorkbook(ProjectModel model) {
            return exportWorkbook(model, null);
        }

        public XlsxWorkbookLike exportWorkbook(ProjectModel model, WbsExportOptions options) {
            return wbsXlsx.exportWbsWorkbook(model, options);
        }

        public byte[] exportBytes(ProjectModel model) {
            return exportBytes(model, null);
        }

        public byte[] exportBytes(ProjectModel model, WbsExportOptions options) {
            return workbookXlsx.encodeWorkbook(wbsXlsx.exportWbsWorkbook(model, options));
        }
    }

    public class SvgApi {
        public String exportDaily(ProjectModel model) {
            return exportDaily(model, null);
        }

        public String exportDaily(ProjectModel model, NativeSvgOptions options) {
            return msProjectXml.exportNativeSvg(model, options);
        }

        public String exportWeekly(ProjectModel model) {
            return exportWeekly(model, null);
        }

        public String exportWeekly(ProjectModel model, NativeSvgOptions options) {
            return msProjectXml.exportWeeklyNativeSvg(model, options);
        }

        public MonthlyCalendarSvgArchive exportMonthlyCalendar(ProjectModel model) {
            return exportMonthlyCalendar(model, null);
        }

        public MonthlyCalendarSvgArchive exportMonthlyCalendar(ProjectModel model, NativeSvgOptions options) {
            return msProjectXml.exportMonthlyWbsCalendarSvgArchive(model, options);
        }
    }

    public class WbsMarkdownApi {
        public String export(ProjectModel model) {
            return wbsMarkdown.exportWbsMarkdown(model);
        }

        public String export(ProjectModel model, WbsMarkdownOptions options) {
            return wbsMarkdown.exportWbsMarkdown(model, options);
        }
    }

    public class MermaidApi {
        public String exportGantt(ProjectModel model) {
            return msproject.mermaid.exportGantt(model);
        }
    }

    public static class ReportBundle {
        public final List<CoreApiReport.ReportEntry> entries = new java.util.ArrayList<CoreApiReport.ReportEntry>();
        public byte[] zipBytes;
    }
}
