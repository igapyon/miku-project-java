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
        for (int index = ProjectWorkbookSchema.DATA_ROW_START_INDEX.intValue(); index < sheet.rows.size(); index++) {
            XlsxRowLike row = sheet.rows.get(index);
            Map<String, Object> item = new LinkedHashMap<String, Object>();
            for (int headerIndex = 0; headerIndex < headers.length; headerIndex++) {
                item.put(headers[headerIndex], util.readCell(row.cells, headerIndex));
            }
            document.ensureSheet(sheet.name).add(item);
        }
    }
}
