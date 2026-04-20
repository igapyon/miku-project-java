/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuproject.projectxlsx;

public class ProjectXlsxExportUtil {
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
}
