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

import jp.igapyon.mikuproject.excelio.ExcelIoZip;
import jp.igapyon.mikuproject.excelio.XlsxWorkbookCodec;
import jp.igapyon.mikuproject.model.ProjectModel;
import jp.igapyon.mikuproject.projectxlsx.XlsxWorkbookLike;
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
        assertEquals(7, bundle.entries.size());
        assertEquals(Arrays.asList("wbs.md", "mermaid.mmd", "wbs.xlsx", "daily.svg", "weekly.svg", "monthly-calendar/2026-03.svg",
                "monthly-calendar/2026-04.svg"),
                entryNames(bundle));
        assertTrue(containsEntry(bundle, "wbs.md", "# WBS テーブル"));
        assertTrue(containsEntry(bundle, "mermaid.mmd", "gantt"));
        assertTrue(containsEntry(bundle, "daily.svg", "<svg"));
        assertTrue(containsEntry(bundle, "weekly.svg", "<svg"));
        assertTrue(bundle.zipBytes.length > 0);
        assertEquals(ExcelIoZip.FIXED_2025_01_01_MOD_TIME, readUnsignedShortLE(bundle.zipBytes, 10));
        assertEquals(ExcelIoZip.FIXED_2025_01_01_MOD_DATE, readUnsignedShortLE(bundle.zipBytes, 12));
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

    @Test
    public void exposesWorkingReportApiSurfaceForHierarchyFixture() throws IOException {
        CoreApi api = new CoreApi();
        ProjectModel model = api.msProject.importFromXml(readVendorTestdata("hierarchy.xml"));

        CoreApiReportAdapters.ReportBundle bundle = api.report.all.export(model);
        XlsxWorkbookLike workbook = decodeWorkbookEntry(bundle, "wbs.xlsx");

        assertEquals(Arrays.asList("wbs.md", "mermaid.mmd", "wbs.xlsx", "daily.svg", "weekly.svg", "monthly-calendar/2026-03.svg"),
                entryNames(bundle));
        assertTrue(containsEntry(bundle, "wbs.md", "Hierarchy Project"));
        assertTrue(containsEntry(bundle, "wbs.md", "Child A"));
        assertTrue(containsEntry(bundle, "mermaid.mmd", "title Hierarchy Project"));
        assertTrue(containsEntry(bundle, "daily.svg", "Child A"));
        assertTrue(containsEntry(bundle, "weekly.svg", "weekly overview"));
        assertEquals("WBS", workbook.sheets.get(0).name);
        assertTrue(containsCellText(workbook, "Hierarchy Project"));
        assertTrue(containsCellText(workbook, "Child A"));
        assertTrue(containsCellText(workbook, "Child B"));
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

    private XlsxWorkbookLike decodeWorkbookEntry(CoreApiReportAdapters.ReportBundle bundle, String expectedName) {
        for (CoreApiReport.ReportEntry entry : bundle.entries) {
            if (expectedName.equals(entry.name)) {
                return new XlsxWorkbookCodec().importWorkbook(entry.data);
            }
        }
        throw new IllegalArgumentException("entry が見つかりません: " + expectedName);
    }

    private boolean containsCellText(XlsxWorkbookLike workbook, String value) {
        for (int rowIndex = 0; rowIndex < workbook.sheets.get(0).rows.size(); rowIndex++) {
            for (int cellIndex = 0; cellIndex < workbook.sheets.get(0).rows.get(rowIndex).cells.size(); cellIndex++) {
                Object cellValue = workbook.sheets.get(0).rows.get(rowIndex).cells.get(cellIndex).value;
                if (cellValue != null && String.valueOf(cellValue).contains(value)) {
                    return true;
                }
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

    private int readUnsignedShortLE(byte[] bytes, int offset) {
        return (bytes[offset] & 0xff) | ((bytes[offset + 1] & 0xff) << 8);
    }
}
