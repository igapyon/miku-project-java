/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuproject.projectxlsx;

public class ProjectXlsxExportUtil {
    public static final String OPTIONS_SHEET_NAME = "Options";

    public XlsxRowLike titleRow(String title) {
        XlsxRowLike row = new XlsxRowLike();
        row.height = Integer.valueOf(22);
        XlsxCellLike cell = cell(title);
        cell.bold = Boolean.TRUE;
        cell.fontSize = Integer.valueOf(16);
        row.cells.add(cell);
        row.cells.add(new XlsxCellLike());
        return row;
    }

    public XlsxRowLike sectionTitleRow(String title) {
        XlsxRowLike row = new XlsxRowLike();
        row.height = Integer.valueOf(20);
        XlsxCellLike cell = cell(title);
        cell.bold = Boolean.TRUE;
        cell.fontSize = Integer.valueOf(14);
        row.cells.add(cell);
        return row;
    }

    public XlsxRowLike headerRow(String[] headers) {
        XlsxRowLike row = new XlsxRowLike();
        for (String header : headers) {
            XlsxCellLike cell = cell(header);
            cell.bold = Boolean.TRUE;
            row.cells.add(cell);
        }
        return row;
    }

    public XlsxRowLike keyValueRow(Object field, Object value) {
        XlsxRowLike row = new XlsxRowLike();
        row.cells.add(cell(field));
        row.cells.add(cell(value));
        return row;
    }

    public XlsxRowLike singleValueRow(String value) {
        XlsxRowLike row = new XlsxRowLike();
        row.cells.add(cell(value));
        return row;
    }

    public XlsxCellLike cell(Object value) {
        XlsxCellLike cell = new XlsxCellLike();
        cell.value = value;
        return cell;
    }

    public void addColumns(XlsxSheetLike sheet, double[] widths) {
        for (double width : widths) {
            XlsxColumnLike column = new XlsxColumnLike();
            column.width = Double.valueOf(width);
            sheet.columns.add(column);
        }
    }

    public void addBooleanDataValidation(XlsxSheetLike sheet, String[] ranges) {
        StringBuilder sqref = new StringBuilder();
        if (ranges != null) {
            for (String range : ranges) {
                if (range == null || range.isEmpty()) {
                    continue;
                }
                if (sqref.length() > 0) {
                    sqref.append(' ');
                }
                sqref.append(range);
            }
        }
        if (sqref.length() == 0) {
            return;
        }
        XlsxDataValidationLike validation = new XlsxDataValidationLike();
        validation.type = "list";
        validation.sqref = sqref.toString();
        validation.formula1 = OPTIONS_SHEET_NAME + "!$A$2:$A$3";
        validation.allowBlank = Boolean.TRUE;
        sheet.dataValidations.add(validation);
    }

    public String buildColumnRange(int columnIndex, int startRow, int endRow) {
        if (endRow < startRow) {
            return null;
        }
        String column = encodeColumnName(columnIndex);
        return column + startRow + ":" + column + endRow;
    }

    public String buildSingleCellReference(int columnIndex, int rowNumber) {
        if (rowNumber <= 0) {
            return null;
        }
        return encodeColumnName(columnIndex) + rowNumber;
    }

    private String encodeColumnName(int columnIndex) {
        int current = columnIndex + 1;
        StringBuilder builder = new StringBuilder();
        while (current > 0) {
            int remainder = (current - 1) % 26;
            builder.insert(0, (char) ('A' + remainder));
            current = (current - 1) / 26;
        }
        return builder.toString();
    }
}
