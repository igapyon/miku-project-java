/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuproject.wbsxlsx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import jp.igapyon.mikuproject.excelio.XlsxWorkbookCodec;
import jp.igapyon.mikuproject.model.ProjectModel;
import jp.igapyon.mikuproject.msprojectxml.MsProjectSamples;
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

    private int findRowIndexByCellValue(XlsxWorkbookLike workbook, String value) {
        for (int rowIndex = 0; rowIndex < workbook.sheets.get(0).rows.size(); rowIndex++) {
            Object cellValue = workbook.sheets.get(0).rows.get(rowIndex).cells.get(0).value;
            if (value.equals(cellValue)) {
                return rowIndex;
            }
        }
        return -1;
    }
}
