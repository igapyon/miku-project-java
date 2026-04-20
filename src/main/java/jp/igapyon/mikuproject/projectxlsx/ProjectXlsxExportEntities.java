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
        return sheet;
    }
}
