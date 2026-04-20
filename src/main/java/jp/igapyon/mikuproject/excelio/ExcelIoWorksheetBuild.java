/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuproject.excelio;

import jp.igapyon.mikuproject.projectxlsx.XlsxCellLike;
import jp.igapyon.mikuproject.projectxlsx.XlsxColumnLike;
import jp.igapyon.mikuproject.projectxlsx.XlsxDataValidationLike;
import jp.igapyon.mikuproject.projectxlsx.XlsxFreezePaneLike;
import jp.igapyon.mikuproject.projectxlsx.XlsxRowLike;
import jp.igapyon.mikuproject.projectxlsx.XlsxSheetLike;

public class ExcelIoWorksheetBuild {
    public String buildWorksheetXml(XlsxSheetLike sheet, ExcelIoStylesBuild.StyleBook styleBook, ExcelIoStylesBuild stylesBuild) {
        StringBuilder builder = new StringBuilder();
        builder.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n");
        builder.append("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">\n");
        builder.append("  ");
        appendSheetViews(builder, sheet.freezePane);
        builder.append("\n");
        builder.append("  ");
        appendColumns(builder, sheet);
        builder.append("\n");
        builder.append("  <sheetData>");
        for (int rowIndex = 0; rowIndex < sheet.rows.size(); rowIndex++) {
            appendRow(builder, sheet.rows.get(rowIndex), rowIndex, styleBook, stylesBuild);
        }
        builder.append("</sheetData>\n");
        builder.append("  ");
        if (sheet.mergedRanges != null && !sheet.mergedRanges.isEmpty()) {
            builder.append("<mergeCells count=\"").append(sheet.mergedRanges.size()).append("\">");
            for (String range : sheet.mergedRanges) {
                builder.append("<mergeCell ref=\"").append(escapeXml(range)).append("\"/>");
            }
            builder.append("</mergeCells>");
        }
        builder.append("\n");
        builder.append("  ");
        appendDataValidations(builder, sheet);
        builder.append("\n");
        builder.append("</worksheet>");
        return builder.toString();
    }

    private void appendColumns(StringBuilder builder, XlsxSheetLike sheet) {
        if (sheet.columns == null || sheet.columns.isEmpty()) {
            return;
        }
        builder.append("<cols>");
        for (int index = 0; index < sheet.columns.size(); index++) {
            XlsxColumnLike column = sheet.columns.get(index);
            builder.append("<col min=\"").append(index + 1).append("\" max=\"").append(index + 1).append("\"");
            if (column.width != null) {
                builder.append(" width=\"").append(formatDouble(column.width.doubleValue())).append("\" customWidth=\"1\"");
            }
            if (Boolean.TRUE.equals(column.hidden)) {
                builder.append(" hidden=\"1\"");
            }
            builder.append("/>");
        }
        builder.append("</cols>");
    }

    private void appendRow(StringBuilder builder, XlsxRowLike row, int rowIndex, ExcelIoStylesBuild.StyleBook styleBook,
            ExcelIoStylesBuild stylesBuild) {
        StringBuilder cells = new StringBuilder();
        for (int cellIndex = 0; cellIndex < row.cells.size(); cellIndex++) {
            XlsxCellLike cell = row.cells.get(cellIndex);
            int styleIndex = stylesBuild.resolveStyleIndex(cell, styleBook);
            if (cell.value == null && cell.formula == null && styleIndex <= 0) {
                continue;
            }
            cells.append("<c r=\"").append(toColumnName(cellIndex)).append(rowIndex + 1).append("\"");
            if (styleIndex > 0) {
                cells.append(" s=\"").append(styleIndex).append("\"");
            }
            if (cell.value == null && cell.formula == null) {
                cells.append("/>");
            } else {
                appendCellValue(cells, cell);
                cells.append("</c>");
            }
        }
        if (cells.length() == 0) {
            return;
        }
        builder.append("<row r=\"").append(rowIndex + 1).append("\"");
        if (row.height != null) {
            builder.append(" ht=\"").append(row.height.intValue()).append("\" customHeight=\"1\"");
        }
        builder.append(">");
        builder.append(cells);
        builder.append("</row>");
    }

    private void appendSheetViews(StringBuilder builder, XlsxFreezePaneLike freezePane) {
        if (freezePane == null || ((freezePane.rowSplit == null || freezePane.rowSplit.intValue() == 0)
                && (freezePane.colSplit == null || freezePane.colSplit.intValue() == 0))) {
            return;
        }
        Integer rowSplit = freezePane.rowSplit == null || freezePane.rowSplit.intValue() == 0 ? null : freezePane.rowSplit;
        Integer colSplit = freezePane.colSplit == null || freezePane.colSplit.intValue() == 0 ? null : freezePane.colSplit;
        builder.append("<sheetViews><sheetView workbookViewId=\"0\"><pane");
        if (colSplit != null) {
            builder.append(" xSplit=\"").append(colSplit.intValue()).append("\"");
        }
        if (rowSplit != null) {
            builder.append(" ySplit=\"").append(rowSplit.intValue()).append("\"");
        }
        builder.append(" topLeftCell=\"").append(toColumnName(colSplit == null ? 0 : colSplit.intValue()))
                .append((rowSplit == null ? 0 : rowSplit.intValue()) + 1).append("\"");
        builder.append(" activePane=\"").append(resolveActivePane(rowSplit, colSplit)).append("\" state=\"frozen\"/>");
        builder.append("</sheetView></sheetViews>");
    }

    private void appendDataValidations(StringBuilder builder, XlsxSheetLike sheet) {
        if (sheet.dataValidations == null || sheet.dataValidations.isEmpty()) {
            return;
        }
        builder.append("<dataValidations count=\"").append(sheet.dataValidations.size()).append("\">");
        for (XlsxDataValidationLike validation : sheet.dataValidations) {
            builder.append("<dataValidation type=\"").append(escapeXml(validation.type)).append("\" allowBlank=\"")
                    .append(Boolean.TRUE.equals(validation.allowBlank) ? "1" : "0")
                    .append("\" showErrorMessage=\"1\" sqref=\"").append(escapeXml(validation.sqref)).append("\">")
                    .append("<formula1>").append(escapeXml(validation.formula1)).append("</formula1></dataValidation>");
        }
        builder.append("</dataValidations>");
    }

    private void appendCellValue(StringBuilder builder, XlsxCellLike cell) {
        if (cell.formula != null) {
            if (cell.value instanceof String) {
                builder.append(" t=\"str\"");
            } else if (cell.value instanceof Boolean) {
                builder.append(" t=\"b\"");
            }
            builder.append("><f>").append(escapeXml(cell.formula)).append("</f>");
            if (cell.value != null) {
                builder.append("<v>");
                if (cell.value instanceof Boolean) {
                    builder.append(Boolean.TRUE.equals(cell.value) ? "1" : "0");
                } else if (cell.value instanceof Number) {
                    builder.append(formatNumber((Number) cell.value));
                } else {
                    builder.append(escapeXml(String.valueOf(cell.value)));
                }
                builder.append("</v>");
            }
            return;
        }
        if (cell.value instanceof Boolean) {
            builder.append(" t=\"b\"><v>").append(Boolean.TRUE.equals(cell.value) ? "1" : "0").append("</v>");
            return;
        }
        if (cell.value instanceof Number) {
            builder.append("><v>").append(formatNumber((Number) cell.value)).append("</v>");
            return;
        }
        builder.append(" t=\"inlineStr\"><is>").append(buildInlineStringTextXml(String.valueOf(cell.value))).append("</is>");
    }

    private String buildInlineStringTextXml(String value) {
        String sanitized = sanitizeXmlText(value);
        if (sanitized.length() == 0) {
            return "<t xml:space=\"preserve\"></t>";
        }
        boolean preserve = Character.isWhitespace(sanitized.charAt(0))
                || Character.isWhitespace(sanitized.charAt(sanitized.length() - 1))
                || sanitized.indexOf('\n') >= 0;
        return "<t" + (preserve ? " xml:space=\"preserve\"" : "") + ">" + escapeXml(sanitized) + "</t>";
    }

    private String sanitizeXmlText(String value) {
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < value.length(); index++) {
            char ch = value.charAt(index);
            if ((ch >= 0x00 && ch <= 0x08) || ch == 0x0b || ch == 0x0c || (ch >= 0x0e && ch <= 0x1f)) {
                continue;
            }
            builder.append(ch);
        }
        return builder.toString();
    }

    private String formatNumber(Number value) {
        if (value instanceof Float || value instanceof Double) {
            double number = value.doubleValue();
            if (!Double.isFinite(number)) {
                throw new IllegalArgumentException("Cell number must be finite: " + value);
            }
            return formatDouble(number);
        }
        return String.valueOf(value);
    }

    private String toColumnName(int columnIndex) {
        int current = columnIndex;
        StringBuilder builder = new StringBuilder();
        do {
            int remainder = current % 26;
            builder.insert(0, (char) ('A' + remainder));
            current = current / 26 - 1;
        } while (current >= 0);
        return builder.toString();
    }

    private String resolveActivePane(Integer rowSplit, Integer colSplit) {
        if (rowSplit != null && colSplit != null) {
            return "bottomRight";
        }
        if (rowSplit != null) {
            return "bottomLeft";
        }
        return "topRight";
    }

    private String formatDouble(double value) {
        if (value == Math.rint(value)) {
            return String.valueOf((long) value);
        }
        return String.valueOf(value);
    }

    private String escapeXml(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}
