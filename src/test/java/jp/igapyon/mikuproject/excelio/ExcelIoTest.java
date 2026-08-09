/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuproject.excelio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.Test;

import jp.igapyon.mikuproject.projectxlsx.XlsxCellLike;
import jp.igapyon.mikuproject.projectxlsx.XlsxDataValidationLike;
import jp.igapyon.mikuproject.projectxlsx.XlsxFreezePaneLike;
import jp.igapyon.mikuproject.projectxlsx.XlsxRowLike;
import jp.igapyon.mikuproject.projectxlsx.XlsxSheetLike;
import jp.igapyon.mikuproject.projectxlsx.XlsxWorkbookLike;

public class ExcelIoTest {
    @Test
    public void packsStoredZipDeterministicallyWithZeroTimestampByDefault() {
        ExcelIoZip zip = new ExcelIoZip();
        List<ExcelIoZip.ZipEntryData> entries = new ArrayList<ExcelIoZip.ZipEntryData>();
        entries.add(new ExcelIoZip.ZipEntryData("a.txt", ExcelIoUtil.encodeUtf8("alpha")));
        entries.add(new ExcelIoZip.ZipEntryData("b.txt", ExcelIoUtil.encodeUtf8("beta")));

        byte[] first = zip.packZip(entries);
        byte[] second = zip.packZip(entries);
        Map<String, byte[]> unpacked = zip.unpackZip(first);

        assertArrayEquals(first, second);
        assertEquals(0, readUnsignedShortLE(first, 8));
        assertEquals(ExcelIoZip.ZERO_MOD_TIME, readUnsignedShortLE(first, 10));
        assertEquals(ExcelIoZip.ZERO_MOD_DATE, readUnsignedShortLE(first, 12));
        assertEquals("alpha", ExcelIoUtil.decodeUtf8(unpacked.get("a.txt")));
        assertEquals("beta", ExcelIoUtil.decodeUtf8(unpacked.get("b.txt")));
    }

    @Test
    public void roundTripsWorkbookBytes() {
        XlsxWorkbookCodec codec = new XlsxWorkbookCodec();
        XlsxWorkbookLike workbook = new XlsxWorkbookLike();
        XlsxSheetLike sheet = new XlsxSheetLike();
        sheet.name = "Project";
        XlsxDataValidationLike validation = new XlsxDataValidationLike();
        validation.type = "list";
        validation.sqref = "B17";
        validation.formula1 = "Options!$A$2:$A$3";
        validation.allowBlank = Boolean.TRUE;
        sheet.dataValidations.add(validation);
        XlsxRowLike row = new XlsxRowLike();
        XlsxCellLike cell1 = new XlsxCellLike();
        cell1.value = "Name";
        cell1.bold = Boolean.TRUE;
        XlsxCellLike cell2 = new XlsxCellLike();
        cell2.value = "Miku Project";
        row.cells.add(cell1);
        row.cells.add(cell2);
        sheet.rows.add(row);
        workbook.sheets.add(sheet);

        byte[] bytes = codec.exportWorkbook(workbook);
        XlsxWorkbookLike imported = codec.importWorkbook(bytes);

        assertTrue(bytes.length > 0);
        assertEquals("Project", imported.sheets.get(0).name);
        assertEquals("Name", imported.sheets.get(0).rows.get(0).cells.get(0).value);
        assertEquals("Miku Project", imported.sheets.get(0).rows.get(0).cells.get(1).value);
        assertEquals("B17", imported.sheets.get(0).dataValidations.get(0).sqref);
    }

    @Test
    public void preservesSupplementaryUnicodeAndRemovesInvalidXmlCharacters() {
        XlsxWorkbookCodec codec = new XlsxWorkbookCodec();
        XlsxWorkbookLike workbook = new XlsxWorkbookLike();
        XlsxSheetLike sheet = new XlsxSheetLike();
        sheet.name = "Project";
        XlsxRowLike row = new XlsxRowLike();
        XlsxCellLike supplementary = new XlsxCellLike();
        supplementary.value = "😀 🐇 𠮷野家";
        XlsxCellLike invalid = new XlsxCellLike();
        invalid.value = "before" + Character.toString((char) 0xd800) + "middle"
                + Character.toString((char) 0xdc00)
                + Character.toString((char) 0xfffe)
                + Character.toString((char) 0xffff) + "after";
        row.cells.add(supplementary);
        row.cells.add(invalid);
        sheet.rows.add(row);
        workbook.sheets.add(sheet);

        XlsxWorkbookLike imported = codec.importWorkbook(codec.exportWorkbook(workbook));

        assertEquals("😀 🐇 𠮷野家", imported.sheets.get(0).rows.get(0).cells.get(0).value);
        assertEquals("beforemiddleafter", imported.sheets.get(0).rows.get(0).cells.get(1).value);
    }

    @Test
    public void preservesXmlWhitespaceAndRemovesXmlControlCharacters() {
        XlsxWorkbookCodec codec = new XlsxWorkbookCodec();
        XlsxWorkbookLike workbook = new XlsxWorkbookLike();
        XlsxSheetLike sheet = new XlsxSheetLike();
        sheet.name = "Project";
        XlsxRowLike row = new XlsxRowLike();
        XlsxCellLike controlCharacters = new XlsxCellLike();
        controlCharacters.value = "ok\u0000bad\u0008text";
        XlsxCellLike xmlWhitespace = new XlsxCellLike();
        xmlWhitespace.value = "line1\nline2\tend";
        row.cells.add(controlCharacters);
        row.cells.add(xmlWhitespace);
        sheet.rows.add(row);
        workbook.sheets.add(sheet);

        XlsxWorkbookLike imported = codec.importWorkbook(codec.exportWorkbook(workbook));

        assertEquals("okbadtext", imported.sheets.get(0).rows.get(0).cells.get(0).value);
        assertEquals("line1\nline2\tend", imported.sheets.get(0).rows.get(0).cells.get(1).value);
    }

    @Test
    public void unpacksDeflatedZipEntriesForExternalPackages() throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream output = new ZipOutputStream(bytes)) {
            output.putNextEntry(new ZipEntry("[Content_Types].xml"));
            output.write(ExcelIoUtil.encodeUtf8("<Types></Types>"));
            output.closeEntry();
        }

        Map<String, byte[]> entries = new ExcelIoZip().unpackZip(bytes.toByteArray());

        assertEquals("<Types></Types>", ExcelIoUtil.decodeUtf8(entries.get("[Content_Types].xml")));
    }

    @Test
    public void normalizesZipEntryPathsThroughMikuMsOfficeCore() throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream output = new ZipOutputStream(bytes)) {
            output.putNextEntry(new ZipEntry("xl/./workbook.xml"));
            output.write(ExcelIoUtil.encodeUtf8("<workbook/>"));
            output.closeEntry();
        }

        Map<String, byte[]> entries = new ExcelIoZip().unpackZip(bytes.toByteArray());

        assertEquals("<workbook/>", ExcelIoUtil.decodeUtf8(entries.get("xl/workbook.xml")));
    }

    @Test
    public void rejectsInvalidWorkbookBytes() {
        XlsxWorkbookCodec codec = new XlsxWorkbookCodec();
        assertThrows(IllegalArgumentException.class, new org.junit.jupiter.api.function.Executable() {
            @Override
            public void execute() {
                codec.importWorkbook(new byte[0]);
            }
        });
    }

    @Test
    public void exportsWorkbookAsOoxmlLikeZipPackage() {
        XlsxWorkbookCodec codec = new XlsxWorkbookCodec();
        XlsxWorkbookLike workbook = new XlsxWorkbookLike();
        XlsxSheetLike sheet = new XlsxSheetLike();
        sheet.name = "Project";
        sheet.mergedRanges.add("A1:B1");
        jp.igapyon.mikuproject.projectxlsx.XlsxColumnLike column = new jp.igapyon.mikuproject.projectxlsx.XlsxColumnLike();
        column.width = Double.valueOf(18d);
        column.hidden = Boolean.TRUE;
        sheet.columns.add(column);
        XlsxRowLike row = new XlsxRowLike();
        XlsxCellLike cell1 = new XlsxCellLike();
        cell1.value = "Project";
        cell1.bold = Boolean.TRUE;
        cell1.fontSize = Integer.valueOf(14);
        XlsxCellLike cell2 = new XlsxCellLike();
        cell2.value = "Miku";
        row.cells.add(cell1);
        row.cells.add(cell2);
        sheet.rows.add(row);
        workbook.sheets.add(sheet);

        byte[] bytes = codec.exportWorkbookArchive(workbook);
        List<String> entries = codec.listEntries(bytes);
        Map<String, byte[]> unpacked = codec.unpackEntries(bytes);
        String workbookXml = ExcelIoUtil.decodeUtf8(unpacked.get("xl/workbook.xml"));
        String worksheetXml = ExcelIoUtil.decodeUtf8(unpacked.get("xl/worksheets/sheet1.xml"));

        assertTrue(bytes.length > 0);
        assertEquals(0, readUnsignedShortLE(bytes, 8));
        assertEquals(ExcelIoZip.ZERO_MOD_TIME, readUnsignedShortLE(bytes, 10));
        assertEquals(ExcelIoZip.ZERO_MOD_DATE, readUnsignedShortLE(bytes, 12));
        assertTrue(entries.contains("[Content_Types].xml"));
        assertTrue(entries.contains("_rels/.rels"));
        assertTrue(entries.contains("xl/workbook.xml"));
        assertTrue(entries.contains("xl/_rels/workbook.xml.rels"));
        assertTrue(entries.contains("xl/styles.xml"));
        assertTrue(entries.contains("xl/worksheets/sheet1.xml"));
        assertTrue(workbookXml.contains("sheet name=\"Project\""));
        assertTrue(worksheetXml.contains("mergeCell ref=\"A1:B1\""));
        assertTrue(worksheetXml.contains("hidden=\"1\""));
        assertTrue(worksheetXml.contains("inlineStr"));
    }

    @Test
    public void importsWorkbookFromOoxmlLikeZipPackage() {
        XlsxWorkbookCodec codec = new XlsxWorkbookCodec();
        XlsxWorkbookLike workbook = new XlsxWorkbookLike();
        XlsxSheetLike sheet = new XlsxSheetLike();
        sheet.name = "Project";
        jp.igapyon.mikuproject.projectxlsx.XlsxColumnLike column = new jp.igapyon.mikuproject.projectxlsx.XlsxColumnLike();
        column.width = Double.valueOf(12d);
        column.hidden = Boolean.TRUE;
        sheet.columns.add(column);
        XlsxRowLike row = new XlsxRowLike();
        row.height = Integer.valueOf(24);
        XlsxCellLike cell1 = new XlsxCellLike();
        cell1.value = "Name";
        cell1.horizontalAlign = "center";
        XlsxCellLike cell2 = new XlsxCellLike();
        cell2.value = Integer.valueOf(7);
        row.cells.add(cell1);
        row.cells.add(cell2);
        sheet.rows.add(row);
        workbook.sheets.add(sheet);

        byte[] bytes = codec.exportWorkbookArchive(workbook);
        XlsxWorkbookLike imported = codec.importWorkbookArchive(bytes);

        assertEquals("Project", imported.sheets.get(0).name);
        assertEquals(Double.valueOf(12d), imported.sheets.get(0).columns.get(0).width);
        assertEquals(Boolean.TRUE, imported.sheets.get(0).columns.get(0).hidden);
        assertEquals(Integer.valueOf(24), imported.sheets.get(0).rows.get(0).height);
        assertEquals("Name", imported.sheets.get(0).rows.get(0).cells.get(0).value);
        assertEquals(Integer.valueOf(7), imported.sheets.get(0).rows.get(0).cells.get(1).value);
    }

    @Test
    public void exportsAndImportsFormulaAndFreezePaneInOoxmlPackage() {
        XlsxWorkbookCodec codec = new XlsxWorkbookCodec();
        XlsxWorkbookLike workbook = new XlsxWorkbookLike();
        XlsxSheetLike sheet = new XlsxSheetLike();
        sheet.name = "Project";
        XlsxFreezePaneLike freezePane = new XlsxFreezePaneLike();
        freezePane.rowSplit = Integer.valueOf(1);
        freezePane.colSplit = Integer.valueOf(1);
        sheet.freezePane = freezePane;
        XlsxRowLike row = new XlsxRowLike();
        XlsxCellLike formulaCell = new XlsxCellLike();
        formulaCell.formula = "SUM(B1:C1)";
        formulaCell.value = Integer.valueOf(7);
        row.cells.add(formulaCell);
        sheet.rows.add(row);
        workbook.sheets.add(sheet);

        byte[] bytes = codec.exportWorkbookArchive(workbook);
        Map<String, byte[]> unpacked = codec.unpackEntries(bytes);
        String worksheetXml = ExcelIoUtil.decodeUtf8(unpacked.get("xl/worksheets/sheet1.xml"));
        XlsxWorkbookLike imported = codec.importWorkbookArchive(bytes);

        assertTrue(worksheetXml.contains("<pane xSplit=\"1\" ySplit=\"1\" topLeftCell=\"B2\" activePane=\"bottomRight\" state=\"frozen\"/>"));
        assertTrue(worksheetXml.contains("<f>SUM(B1:C1)</f><v>7</v>"));
        assertEquals(Integer.valueOf(1), imported.sheets.get(0).freezePane.rowSplit);
        assertEquals(Integer.valueOf(1), imported.sheets.get(0).freezePane.colSplit);
        assertEquals("SUM(B1:C1)", imported.sheets.get(0).rows.get(0).cells.get(0).formula);
        assertEquals(Integer.valueOf(7), imported.sheets.get(0).rows.get(0).cells.get(0).value);
    }

    @Test
    public void normalizesWorkbookShapeBeforeExportAndValidatesNamesAndRanges() {
        ExcelIoNormalize normalize = new ExcelIoNormalize();
        XlsxWorkbookLike workbook = new XlsxWorkbookLike();
        XlsxSheetLike sheet = new XlsxSheetLike();
        sheet.name = "Project";
        jp.igapyon.mikuproject.projectxlsx.XlsxColumnLike column = new jp.igapyon.mikuproject.projectxlsx.XlsxColumnLike();
        column.width = Double.valueOf(12d);
        sheet.columns.add(column);
        XlsxDataValidationLike validation = new XlsxDataValidationLike();
        validation.type = "list";
        validation.sqref = "a1 b2:c3";
        validation.formula1 = "Options!$A$2:$A$3";
        sheet.dataValidations.add(validation);
        sheet.mergedRanges.add("a1:b2");
        XlsxRowLike row = new XlsxRowLike();
        XlsxCellLike cell = new XlsxCellLike();
        cell.value = "Project";
        cell.fillColor = "#d9eaf7";
        row.cells.add(cell);
        sheet.rows.add(row);
        workbook.sheets.add(sheet);

        normalize.normalizeWorkbook(workbook);

        assertEquals("A1:B2", workbook.sheets.get(0).mergedRanges.get(0));
        assertEquals("A1 B2:C3", workbook.sheets.get(0).dataValidations.get(0).sqref);
        assertEquals("FFD9EAF7", workbook.sheets.get(0).rows.get(0).cells.get(0).fillColor);
        assertEquals("#D9EAF7", normalize.denormalizeColor("FFD9EAF7"));
        assertThrows(IllegalArgumentException.class, new org.junit.jupiter.api.function.Executable() {
            @Override
            public void execute() {
                normalize.validateSheetName("bad/name");
            }
        });
    }

    private int readUnsignedShortLE(byte[] bytes, int offset) {
        return (bytes[offset] & 0xff) | ((bytes[offset + 1] & 0xff) << 8);
    }
}
