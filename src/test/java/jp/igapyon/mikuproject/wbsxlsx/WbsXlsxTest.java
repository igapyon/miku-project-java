/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuproject.wbsxlsx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import jp.igapyon.mikuproject.excelio.XlsxWorkbookCodec;
import jp.igapyon.mikuproject.model.ProjectModel;
import jp.igapyon.mikuproject.msprojectxml.MsProjectSamples;
import jp.igapyon.mikuproject.msprojectxml.MsProjectXml;
import jp.igapyon.mikuproject.projectxlsx.XlsxWorkbookLike;

public class WbsXlsxTest {
    @Test
    public void providesExcelStyleLayoutReferencesForWbsWorksheetTuning() {
        WbsXlsxLayout layout = new WbsXlsxLayout();

        assertEquals(0, layout.columnIndex("A"));
        assertEquals(18, layout.columnIndex("S"));
        assertEquals("S", layout.columnName(18));
        assertEquals("C17", layout.reference(17, 2));
        assertEquals("A1:C17", layout.range("A1", "C17"));

        WbsXlsxLayout.ParsedCellReference parsed = layout.parseCellReference("C17");

        assertEquals("C17", parsed.reference);
        assertEquals(17, parsed.rowNumber);
        assertEquals(16, parsed.rowIndex);
        assertEquals("C", parsed.columnName);
        assertEquals(2, parsed.columnIndex);
        assertEquals("C17 (row 17, rowIndex 16, column C, columnIndex 2)", layout.describeCell("C17"));
    }

    @Test
    public void canLogWbsLayoutCellReferencesOnDemand() {
        WbsXlsxLayout layout = new WbsXlsxLayout();
        final List<String> messages = new ArrayList<String>();

        String message = layout.logCell("S12", "week header", new WbsXlsxLayout.Logger() {
            @Override
            public void log(String line) {
                messages.add(line);
            }
        });

        assertEquals("week header: S12 (row 12, rowIndex 11, column S, columnIndex 18)", message);
        assertEquals(1, messages.size());
        assertEquals(message, messages.get(0));
    }

    @Test
    public void exportsDedicatedWbsWorkbookAndCanEncodeIt() {
        WbsXlsx wbsXlsx = new WbsXlsx();
        ProjectModel model = new MsProjectSamples().buildSampleProjectModel();

        XlsxWorkbookLike workbook = wbsXlsx.exportWbsWorkbook(model);
        XlsxWorkbookCodec codec = new XlsxWorkbookCodec();
        byte[] bytes = codec.exportWorkbook(workbook);
        XlsxWorkbookLike imported = codec.importWorkbook(bytes);

        assertEquals(1, workbook.sheets.size());
        assertEquals("WBS", workbook.sheets.get(0).name);
        assertEquals(Boolean.TRUE, workbook.sheets.get(0).columns.get(11).hidden);
        assertTrue(workbook.sheets.get(0).mergedRanges.contains("A1:E1"));
        assertEquals("プロジェクト情報", workbook.sheets.get(0).rows.get(0).cells.get(0).value);
        int headerRowIndex = findRowIndexByCellValue(workbook, "UID");
        int legendRowIndex = findRowIndexByCellValue(workbook, "凡例");
        int summaryRowIndex = findRowIndexByCellValue(workbook, "サマリ");
        assertTrue(headerRowIndex > 0);
        assertEquals("Mon", workbook.sheets.get(0).rows.get(headerRowIndex).cells.get(20).value);
        assertEquals("3/16", workbook.sheets.get(0).rows.get(headerRowIndex - 1).cells.get(20).value);
        assertTrue(legendRowIndex > headerRowIndex);
        assertTrue(summaryRowIndex > legendRowIndex);
        assertTrue(bytes.length > 0);
        assertEquals("WBS", imported.sheets.get(0).name);
        assertEquals(Boolean.TRUE, imported.sheets.get(0).columns.get(11).hidden);
        assertNotNull(imported.sheets.get(0).rows.get(headerRowIndex + 1).cells.get(5).value);
    }

    @Test
    public void exportsHierarchyFixtureIntoDedicatedWorkbook() throws IOException {
        WbsXlsx wbsXlsx = new WbsXlsx();
        ProjectModel model = new MsProjectXml().importFromXml(readVendorTestdata("hierarchy.xml"));

        XlsxWorkbookLike workbook = wbsXlsx.exportWbsWorkbook(model);

        assertEquals("WBS", workbook.sheets.get(0).name);
        assertTrue(containsCellText(workbook, "Hierarchy Project"));
        assertTrue(containsCellText(workbook, "Summary"));
        assertTrue(containsCellText(workbook, "Child A"));
        assertTrue(containsCellText(workbook, "Child B"));
    }

    @Test
    public void exportsDependencyFixtureIntoDedicatedWorkbook() throws IOException {
        WbsXlsx wbsXlsx = new WbsXlsx();
        ProjectModel model = new MsProjectXml().importFromXml(readVendorTestdata("dependency.xml"));

        XlsxWorkbookLike workbook = wbsXlsx.exportWbsWorkbook(model);

        assertEquals("WBS", workbook.sheets.get(0).name);
        assertTrue(containsCellText(workbook, "Dependency Project"));
        assertTrue(containsCellText(workbook, "Prepare"));
        assertTrue(containsCellText(workbook, "Execute"));
        assertTrue(containsCellText(workbook, "100%"));
        assertTrue(containsCellText(workbook, "0%"));
    }

    @Test
    public void appliesDisplayAndHolidayOptionsToDedicatedWorkbook() {
        WbsXlsx wbsXlsx = new WbsXlsx();
        ProjectModel model = new MsProjectSamples().buildSampleProjectModel();
        WbsXlsx.WbsExportOptions options = new WbsXlsx.WbsExportOptions();
        options.displayDaysBeforeBaseDate = Integer.valueOf(1);
        options.displayDaysAfterBaseDate = Integer.valueOf(2);
        options.useBusinessDaysForDisplayRange = Boolean.TRUE;
        options.useBusinessDaysForProgressBand = Boolean.TRUE;
        options.holidayDates.add("2026-04-29");
        options.holidayDates.add("2026-04-30");

        XlsxWorkbookLike workbook = wbsXlsx.exportWbsWorkbook(model, options);

        assertEquals("1", valueAtRowLabel(workbook, "前日数"));
        assertEquals("2", valueAtRowLabel(workbook, "後日数"));
        assertEquals("営業日", valueAtRowLabel(workbook, "表示"));
        assertEquals("営業日", valueAtRowLabel(workbook, "進捗"));
        assertEquals("2", valueAtProjectInfoLabel(workbook, "祝日"));
    }

    private int findRowIndexByCellValue(XlsxWorkbookLike workbook, String value) {
        for (int rowIndex = 0; rowIndex < workbook.sheets.get(0).rows.size(); rowIndex++) {
            Object cellValue = workbook.sheets.get(0).rows.get(rowIndex).cells.get(0).value;
            if (value.equals(cellValue)) {
                return rowIndex;
            }
        }
        return -1;
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

    private String valueAtRowLabel(XlsxWorkbookLike workbook, String label) {
        for (int rowIndex = 0; rowIndex < workbook.sheets.get(0).rows.size(); rowIndex++) {
            Object cellValue = workbook.sheets.get(0).rows.get(rowIndex).cells.get(0).value;
            if (label.equals(cellValue)) {
                Object value = workbook.sheets.get(0).rows.get(rowIndex).cells.get(1).value;
                return value == null ? null : String.valueOf(value);
            }
        }
        return null;
    }

    private String valueAtProjectInfoLabel(XlsxWorkbookLike workbook, String label) {
        for (int rowIndex = 0; rowIndex < workbook.sheets.get(0).rows.size(); rowIndex++) {
            Object cellValue = workbook.sheets.get(0).rows.get(rowIndex).cells.get(0).value;
            if (label.equals(cellValue)) {
                Object value = workbook.sheets.get(0).rows.get(rowIndex).cells.get(2).value;
                return value == null ? null : String.valueOf(value);
            }
        }
        return null;
    }

    private String readVendorTestdata(String fileName) throws IOException {
        byte[] bytes = Files.readAllBytes(Paths.get("vendor", "miku-project", "testdata", fileName));
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
