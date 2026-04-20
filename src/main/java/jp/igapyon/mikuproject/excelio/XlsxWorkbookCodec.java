/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuproject.excelio;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jp.igapyon.mikuproject.projectxlsx.XlsxCellLike;
import jp.igapyon.mikuproject.projectxlsx.XlsxColumnLike;
import jp.igapyon.mikuproject.projectxlsx.XlsxDataValidationLike;
import jp.igapyon.mikuproject.projectxlsx.XlsxRowLike;
import jp.igapyon.mikuproject.projectxlsx.XlsxSheetLike;
import jp.igapyon.mikuproject.projectxlsx.XlsxWorkbookLike;

public class XlsxWorkbookCodec {
    private static final byte[] MAGIC = ExcelIoUtil.encodeUtf8("mikuproject_xlsx_workbook_v1\n");
    private final ExcelIoZip zip = new ExcelIoZip();
    private final ExcelIoPackageXml packageXml = new ExcelIoPackageXml();
    private final ExcelIoWorkbookBuild workbookBuild = new ExcelIoWorkbookBuild();
    private final ExcelIoWorkbookParse workbookParse = new ExcelIoWorkbookParse();
    private final ExcelIoNormalize normalize = new ExcelIoNormalize();

    public byte[] exportWorkbook(XlsxWorkbookLike workbook) {
        return exportWorkbookArchive(workbook);
    }

    public XlsxWorkbookLike importWorkbook(byte[] bytes) {
        if (hasMagic(bytes)) {
            return importLegacyWorkbook(bytes);
        }
        return importWorkbookArchive(bytes);
    }

    private XlsxWorkbookLike importLegacyWorkbook(byte[] bytes) {
        String text = ExcelIoUtil.decodeUtf8(slice(bytes, MAGIC.length, bytes.length - MAGIC.length));
        return new Parser(text).parseWorkbook();
    }

    private boolean hasMagic(byte[] bytes) {
        if (bytes == null || bytes.length < MAGIC.length) {
            return false;
        }
        for (int index = 0; index < MAGIC.length; index++) {
            if (bytes[index] != MAGIC[index]) {
                return false;
            }
        }
        return true;
    }

    public byte[] exportWorkbookArchive(XlsxWorkbookLike workbook) {
        normalize.normalizeWorkbook(workbook);
        return zip.packZip(exportPackageEntries(workbook));
    }

    public XlsxWorkbookLike importWorkbookArchive(byte[] bytes) {
        return normalize.normalizeWorkbook(workbookParse.parseWorkbookEntries(zip.unpackZip(bytes)));
    }

    public List<String> listEntries(byte[] bytes) {
        return zip.listEntries(bytes);
    }

    public Map<String, byte[]> unpackEntries(byte[] bytes) {
        return zip.unpackZip(bytes);
    }

    public List<ExcelIoZip.ZipEntryData> exportPackageEntries(XlsxWorkbookLike workbook) {
        normalize.normalizeWorkbook(workbook);
        return workbookBuild.createWorkbookEntries(workbook, packageXml);
    }

    private void appendWorkbook(StringBuilder builder, XlsxWorkbookLike workbook) {
        builder.append("WB\n");
        builder.append(workbook.sheets.size()).append('\n');
        for (XlsxSheetLike sheet : workbook.sheets) {
            appendSheet(builder, sheet);
        }
    }

    private void appendSheet(StringBuilder builder, XlsxSheetLike sheet) {
        builder.append("SH\n");
        appendString(builder, sheet.name);
        appendColumns(builder, sheet.columns);
        appendStringList(builder, sheet.mergedRanges);
        appendValidations(builder, sheet.dataValidations);
        builder.append(sheet.rows.size()).append('\n');
        for (XlsxRowLike row : sheet.rows) {
            appendRow(builder, row);
        }
    }

    private void appendColumns(StringBuilder builder, List<XlsxColumnLike> columns) {
        int size = columns == null ? 0 : columns.size();
        builder.append(size).append('\n');
        if (columns != null) {
            for (XlsxColumnLike column : columns) {
                builder.append(column.width == null ? "" : column.width).append('\n');
                builder.append(column.hidden == null ? "" : column.hidden).append('\n');
            }
        }
    }

    private void appendStringList(StringBuilder builder, List<String> values) {
        int size = values == null ? 0 : values.size();
        builder.append(size).append('\n');
        if (values != null) {
            for (String value : values) {
                appendString(builder, value);
            }
        }
    }

    private void appendValidations(StringBuilder builder, List<XlsxDataValidationLike> validations) {
        int size = validations == null ? 0 : validations.size();
        builder.append(size).append('\n');
        if (validations != null) {
            for (XlsxDataValidationLike validation : validations) {
                appendString(builder, validation.type);
                appendString(builder, validation.sqref);
                appendString(builder, validation.formula1);
                appendString(builder, validation.allowBlank == null ? null : String.valueOf(validation.allowBlank));
            }
        }
    }

    private void appendRow(StringBuilder builder, XlsxRowLike row) {
        builder.append("RW\n");
        appendString(builder, row.height == null ? null : String.valueOf(row.height));
        builder.append(row.cells.size()).append('\n');
        for (XlsxCellLike cell : row.cells) {
            appendCell(builder, cell);
        }
    }

    private void appendCell(StringBuilder builder, XlsxCellLike cell) {
        builder.append("CL\n");
        String type = cell.value == null ? "null"
                : cell.value instanceof Boolean ? "boolean"
                        : cell.value instanceof Number ? "number" : "string";
        appendString(builder, type);
        appendString(builder, cell.value == null ? null : String.valueOf(cell.value));
        appendString(builder, cell.numberFormat);
        appendString(builder, cell.horizontalAlign);
        appendString(builder, cell.verticalAlign);
        appendString(builder, cell.wrapText == null ? null : String.valueOf(cell.wrapText));
        appendString(builder, cell.bold == null ? null : String.valueOf(cell.bold));
        appendString(builder, cell.fontSize == null ? null : String.valueOf(cell.fontSize));
        appendString(builder, cell.fillColor);
        appendString(builder, cell.border);
    }

    private void appendString(StringBuilder builder, String value) {
        String text = value == null ? "" : value;
        builder.append(text.length()).append(':').append(text).append('\n');
    }

    private byte[] slice(byte[] bytes, int offset, int length) {
        byte[] result = new byte[length];
        System.arraycopy(bytes, offset, result, 0, length);
        return result;
    }

    private static class Parser {
        private final String text;
        private int index;

        private Parser(String text) {
            this.text = text;
        }

        private XlsxWorkbookLike parseWorkbook() {
            expect("WB");
            int sheetCount = parseLineInt();
            XlsxWorkbookLike workbook = new XlsxWorkbookLike();
            for (int i = 0; i < sheetCount; i++) {
                workbook.sheets.add(parseSheet());
            }
            return workbook;
        }

        private XlsxSheetLike parseSheet() {
            expect("SH");
            XlsxSheetLike sheet = new XlsxSheetLike();
            sheet.name = parseString();
            int columnCount = parseLineInt();
            for (int i = 0; i < columnCount; i++) {
                XlsxColumnLike column = new XlsxColumnLike();
                String width = parseLine();
                column.width = width.isEmpty() ? null : Double.valueOf(Double.parseDouble(width));
                String hidden = parseLine();
                column.hidden = hidden.isEmpty() ? null : Boolean.valueOf(Boolean.parseBoolean(hidden));
                sheet.columns.add(column);
            }
            int mergedCount = parseLineInt();
            for (int i = 0; i < mergedCount; i++) {
                sheet.mergedRanges.add(parseString());
            }
            int validationCount = parseLineInt();
            for (int i = 0; i < validationCount; i++) {
                XlsxDataValidationLike validation = new XlsxDataValidationLike();
                validation.type = parseString();
                validation.sqref = parseString();
                validation.formula1 = parseString();
                String allowBlank = parseString();
                validation.allowBlank = allowBlank.isEmpty() ? null : Boolean.valueOf(Boolean.parseBoolean(allowBlank));
                sheet.dataValidations.add(validation);
            }
            int rowCount = parseLineInt();
            for (int i = 0; i < rowCount; i++) {
                sheet.rows.add(parseRow());
            }
            return sheet;
        }

        private XlsxRowLike parseRow() {
            expect("RW");
            XlsxRowLike row = new XlsxRowLike();
            String height = parseString();
            row.height = height.isEmpty() ? null : Integer.valueOf(Integer.parseInt(height));
            int cellCount = parseLineInt();
            for (int i = 0; i < cellCount; i++) {
                row.cells.add(parseCell());
            }
            return row;
        }

        private XlsxCellLike parseCell() {
            expect("CL");
            XlsxCellLike cell = new XlsxCellLike();
            String type = parseString();
            String value = parseString();
            if (!"null".equals(type)) {
                if ("boolean".equals(type)) {
                    cell.value = Boolean.valueOf(Boolean.parseBoolean(value));
                } else if ("number".equals(type)) {
                    cell.value = value.contains(".") ? Double.valueOf(Double.parseDouble(value))
                            : Integer.valueOf(Integer.parseInt(value));
                } else {
                    cell.value = value;
                }
            }
            cell.numberFormat = emptyToNull(parseString());
            cell.horizontalAlign = emptyToNull(parseString());
            cell.verticalAlign = emptyToNull(parseString());
            String wrapText = parseString();
            cell.wrapText = wrapText.isEmpty() ? null : Boolean.valueOf(Boolean.parseBoolean(wrapText));
            String bold = parseString();
            cell.bold = bold.isEmpty() ? null : Boolean.valueOf(Boolean.parseBoolean(bold));
            String fontSize = parseString();
            cell.fontSize = fontSize.isEmpty() ? null : Integer.valueOf(Integer.parseInt(fontSize));
            cell.fillColor = emptyToNull(parseString());
            cell.border = emptyToNull(parseString());
            return cell;
        }

        private void expect(String marker) {
            if (!marker.equals(parseLine())) {
                throw new IllegalArgumentException("invalid workbook payload");
            }
        }

        private String parseString() {
            int colon = text.indexOf(':', index);
            if (colon < 0) {
                throw new IllegalArgumentException("invalid workbook payload");
            }
            int length = Integer.parseInt(text.substring(index, colon));
            int start = colon + 1;
            int end = start + length;
            if (end > text.length()) {
                throw new IllegalArgumentException("invalid workbook payload");
            }
            String value = text.substring(start, end);
            index = end;
            if (index < text.length() && text.charAt(index) == '\n') {
                index += 1;
            }
            return value;
        }

        private int parseLineInt() {
            return Integer.parseInt(parseLine());
        }

        private String parseLine() {
            int newline = text.indexOf('\n', index);
            if (newline < 0) {
                String value = text.substring(index);
                index = text.length();
                return value;
            }
            String value = text.substring(index, newline);
            index = newline + 1;
            return value;
        }

        private String emptyToNull(String value) {
            return value.isEmpty() ? null : value;
        }
    }
}
