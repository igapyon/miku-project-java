/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuproject.coreapi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import jp.igapyon.mikuproject.model.ProjectModel;
import jp.igapyon.mikuproject.projectworkbookjson.WorkbookJsonDocument;

public class CoreApiPublicTest {
    @Test
    public void exposesUnifiedPublicApiSurface() throws IOException {
        CoreApi api = new CoreApi();
        ProjectModel baseModel = api.msProject.importFromXml(readVendorTestdata("dependency.xml"));
        WorkbookJsonDocument document = api.workbookJson.exportProjectWorkbookJson(baseModel);
        findProjectNameRow(document).put("Value", "Core API public workbook import");

        CoreApiImportResult result = api.importAiJsonDocument(toDocumentLike(document), baseModel);

        assertEquals(1, api.version);
        assertNotNull(api.samples);
        assertNotNull(api.projectModel);
        assertNotNull(api.msProject);
        assertNotNull(api.aiViews);
        assertNotNull(api.workbookJson);
        assertNotNull(api.xlsx);
        assertNotNull(api.patchJson);
        assertNotNull(api.report);
        assertEquals("workbook_json", api.detectAiJsonDocumentKind(toDocumentLike(document)));
        assertEquals("workbook_json", result.kind);
        assertEquals("merge", result.mode);
        assertEquals("Core API public workbook import", result.model.project.name);
        assertTrue(api.getAiJsonSpecText().contains("project_draft_view"));
    }

    @Test
    public void exposesWorkingReportApiSurface() {
        CoreApi api = new CoreApi();
        ProjectModel model = new jp.igapyon.mikuproject.msprojectxml.MsProjectSamples().buildSampleProjectModel();

        String markdown = api.report.wbsMarkdown.export(model);
        String mermaid = api.report.mermaid.exportGantt(model);
        CoreApiReportAdapters.ReportBundle bundle = api.report.all.export(model);
        String dailySvg = api.report.svg.exportDaily(model);
        String weeklySvg = api.report.svg.exportWeekly(model);
        jp.igapyon.mikuproject.wbssvg.WbsSvg.MonthlyCalendarSvgArchive monthlyCalendar = api.report.svg.exportMonthlyCalendar(model);
        jp.igapyon.mikuproject.projectxlsx.XlsxWorkbookLike workbook = api.report.wbsXlsx.exportWorkbook(model);
        byte[] workbookBytes = api.report.wbsXlsx.exportBytes(model);

        assertTrue(markdown.contains("# WBS テーブル"));
        assertTrue(mermaid.contains("gantt"));
        assertTrue(dailySvg.contains("<svg"));
        assertTrue(weeklySvg.contains("<svg"));
        assertTrue(monthlyCalendar.entries.size() > 0);
        assertEquals("WBS", workbook.sheets.get(0).name);
        assertTrue(workbookBytes.length > 0);
        assertEquals(6, bundle.entries.size());
        assertEquals(Arrays.asList("wbs.md", "mermaid.mmd", "wbs.xlsx", "daily.svg", "weekly.svg", "monthly-calendar/2026-03.svg"),
                entryNames(bundle));
        assertTrue(containsEntry(bundle, "wbs.md", "# WBS テーブル"));
        assertTrue(containsEntry(bundle, "mermaid.mmd", "gantt"));
        assertTrue(containsEntry(bundle, "daily.svg", "<svg"));
        assertTrue(containsEntry(bundle, "weekly.svg", "<svg"));
        assertTrue(bundle.zipBytes.length > 0);
    }

    @Test
    public void exposesWorkingReportApiSurfaceForDependencyFixture() throws IOException {
        CoreApi api = new CoreApi();
        ProjectModel model = api.msProject.importFromXml(readVendorTestdata("dependency.xml"));

        CoreApiReportAdapters.ReportBundle bundle = api.report.all.export(model);

        assertEquals(Arrays.asList("wbs.md", "mermaid.mmd", "wbs.xlsx", "daily.svg", "weekly.svg", "monthly-calendar/2026-03.svg"),
                entryNames(bundle));
        assertTrue(containsEntry(bundle, "wbs.md", "Dependency Project"));
        assertTrue(containsEntry(bundle, "mermaid.mmd", "Prepare"));
        assertTrue(containsEntry(bundle, "mermaid.mmd", "Execute"));
        assertTrue(containsEntry(bundle, "daily.svg", "Prepare"));
        assertTrue(containsEntry(bundle, "weekly.svg", "weekly overview"));
        assertTrue(bundle.zipBytes.length > 0);
    }

    private List<String> entryNames(CoreApiReportAdapters.ReportBundle bundle) {
        List<String> names = new ArrayList<String>();
        for (CoreApiReport.ReportEntry entry : bundle.entries) {
            names.add(entry.name);
        }
        return names;
    }

    private boolean containsEntry(CoreApiReportAdapters.ReportBundle bundle, String expectedName, String expectedText) {
        for (CoreApiReport.ReportEntry entry : bundle.entries) {
            if (expectedName.equals(entry.name)) {
                return new String(entry.data, StandardCharsets.UTF_8).contains(expectedText);
            }
        }
        return false;
    }

    private Map<String, Object> findProjectNameRow(WorkbookJsonDocument document) {
        for (Map<String, Object> row : document.sheets.get("Project")) {
            if ("Name".equals(row.get("Field"))) {
                return row;
            }
        }
        return null;
    }

    private Map<String, Object> toDocumentLike(WorkbookJsonDocument document) {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        map.put("format", document.format);
        map.put("version", document.version);
        map.put("sheets", document.sheets);
        return map;
    }

    private String readVendorTestdata(String fileName) throws IOException {
        byte[] bytes = Files.readAllBytes(Paths.get("vendor", "mikuproject", "testdata", fileName));
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
