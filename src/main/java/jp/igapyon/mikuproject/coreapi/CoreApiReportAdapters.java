/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuproject.coreapi;

import java.util.List;

import jp.igapyon.mikuproject.model.ProjectModel;
import jp.igapyon.mikuproject.msprojectxml.MsProjectXml;
import jp.igapyon.mikuproject.projectxlsx.XlsxWorkbookLike;
import jp.igapyon.mikuproject.wbssvg.WbsSvg.MonthlyCalendarSvgArchive;
import jp.igapyon.mikuproject.wbsmarkdown.WbsMarkdown;
import jp.igapyon.mikuproject.wbsmarkdown.WbsMarkdown.WbsMarkdownOptions;
import jp.igapyon.mikuproject.wbsxlsx.WbsXlsx;

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
            return export(model, null);
        }

        public ReportBundle export(ProjectModel model, WbsMarkdownOptions options) {
            List<CoreApiReport.ReportEntry> entries = coreApiReport.exportAllReportEntries(model, options);
            ReportBundle bundle = new ReportBundle();
            bundle.entries.addAll(entries);
            bundle.zipBytes = coreApiReport.packZipEntries(entries);
            return bundle;
        }
    }

    public class WbsXlsxApi {
        public XlsxWorkbookLike exportWorkbook(ProjectModel model) {
            return wbsXlsx.exportWbsWorkbook(model);
        }

        public byte[] exportBytes(ProjectModel model) {
            return workbookXlsx.encodeWorkbook(wbsXlsx.exportWbsWorkbook(model));
        }
    }

    public class SvgApi {
        public String exportDaily(ProjectModel model) {
            return msProjectXml.exportNativeSvg(model);
        }

        public String exportWeekly(ProjectModel model) {
            return msProjectXml.exportWeeklyNativeSvg(model);
        }

        public MonthlyCalendarSvgArchive exportMonthlyCalendar(ProjectModel model) {
            return msProjectXml.exportMonthlyWbsCalendarSvgArchive(model);
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
