/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuproject.projectxlsx;

import java.util.List;
import java.util.Map;

public class ProjectXlsxExportEntities {
    private final ProjectXlsxExportUtil util = new ProjectXlsxExportUtil();

    public XlsxSheetLike buildTabularSheet(String sheetName, String sectionTitle, String[] headers, List<Map<String, Object>> rows) {
        XlsxSheetLike sheet = new XlsxSheetLike();
        sheet.name = sheetName;
        addSheetColumns(sheet, sheetName);
        sheet.rows.add(util.titleRow(sheetName));
        sheet.rows.add(util.sectionTitleRow(sectionTitle));
        sheet.rows.add(util.headerRow(headers));
        for (Map<String, Object> row : rows) {
            XlsxRowLike xlsxRow = new XlsxRowLike();
            for (String header : headers) {
                xlsxRow.cells.add(util.cell(row.get(header)));
            }
            sheet.rows.add(xlsxRow);
        }
        addSheetDataValidations(sheet, sheetName, rows == null ? 0 : rows.size());
        return sheet;
    }

    private void addSheetColumns(XlsxSheetLike sheet, String sheetName) {
        if ("Tasks".equals(sheetName)) {
            util.addColumns(sheet, new double[] {
                    10d, 8d, 28d, 12d, 14d, 12d, 20d, 20d, 14d, 16d, 18d,
                    12d, 12d, 12d, 12d, 12d, 12d, 12d, 20d, 20d, 18d, 34d
            });
        } else if ("Resources".equals(sheetName)) {
            util.addColumns(sheet, new double[] {
                    10d, 8d, 24d, 10d, 12d, 18d, 12d, 12d, 14d, 14d, 12d,
                    14d, 14d, 14d, 12d, 12d, 12d, 18d, 12d, 18d, 18d
            });
        } else if ("Assignments".equals(sheetName)) {
            util.addColumns(sheet, new double[] {
                    10d, 10d, 24d, 12d, 24d, 20d, 20d, 14d, 14d, 14d, 12d,
                    14d, 10d, 14d, 12d, 14d, 14d, 12d, 12d, 14d, 18d, 18d
            });
        }
    }

    private void addSheetDataValidations(XlsxSheetLike sheet, String sheetName, int rowCount) {
        int startRow = 4;
        int endRow = startRow + Math.max(0, rowCount) - 1;
        if ("Tasks".equals(sheetName)) {
            util.addBooleanDataValidation(sheet, new String[] {
                    util.buildColumnRange(11, startRow, endRow),
                    util.buildColumnRange(12, startRow, endRow),
                    util.buildColumnRange(13, startRow, endRow)
            });
        } else if ("Assignments".equals(sheetName)) {
            util.addBooleanDataValidation(sheet, new String[] {
                    util.buildColumnRange(10, startRow, endRow)
            });
        }
    }
}
