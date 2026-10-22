/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuproject.projectxlsx;

import java.util.LinkedHashMap;
import java.util.Map;

import jp.igapyon.mikuproject.projectworkbookjson.ProjectWorkbookSchema;
import jp.igapyon.mikuproject.projectworkbookjson.WorkbookJsonDocument;

public class ProjectXlsxImportEntities {
    private final ProjectXlsxImportUtil util = new ProjectXlsxImportUtil();

    public void importTabularSheet(WorkbookJsonDocument document, XlsxSheetLike sheet, String[] headers) {
        Map<String, Integer> headerColumns = headerColumns(sheet, headers);
        for (int index = ProjectWorkbookSchema.DATA_ROW_START_INDEX.intValue(); index < sheet.rows.size(); index++) {
            XlsxRowLike row = sheet.rows.get(index);
            Map<String, Object> item = new LinkedHashMap<String, Object>();
            for (String header : headers) {
                Integer columnIndex = headerColumns.get(header);
                if (columnIndex != null) {
                    item.put(header, util.readCell(row.cells, columnIndex.intValue()));
                }
            }
            document.ensureSheet(sheet.name).add(item);
        }
    }

    private Map<String, Integer> headerColumns(XlsxSheetLike sheet, String[] headers) {
        Map<String, Integer> columns = new LinkedHashMap<String, Integer>();
        int headerRowIndex = ProjectWorkbookSchema.HEADER_ROW_INDEX.intValue();
        if (headerRowIndex < sheet.rows.size()) {
            XlsxRowLike headerRow = sheet.rows.get(headerRowIndex);
            for (String header : headers) {
                for (int columnIndex = 0; columnIndex < headerRow.cells.size(); columnIndex++) {
                    if (header.equals(headerRow.cells.get(columnIndex).value)) {
                        columns.put(header, Integer.valueOf(columnIndex));
                        break;
                    }
                }
            }
        }
        if (columns.isEmpty()) {
            for (int columnIndex = 0; columnIndex < headers.length; columnIndex++) {
                columns.put(headers[columnIndex], Integer.valueOf(columnIndex));
            }
        }
        return columns;
    }
}
