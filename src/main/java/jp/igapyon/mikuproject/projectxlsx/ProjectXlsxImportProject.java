/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuproject.projectxlsx;

import java.util.LinkedHashMap;
import java.util.Map;

import jp.igapyon.mikuproject.projectworkbookjson.ProjectWorkbookSchema;
import jp.igapyon.mikuproject.projectworkbookjson.WorkbookJsonDocument;

public class ProjectXlsxImportProject {
    private final ProjectXlsxImportUtil util = new ProjectXlsxImportUtil();

    public void importProjectSheet(WorkbookJsonDocument document, XlsxSheetLike sheet) {
        for (int index = ProjectWorkbookSchema.DATA_ROW_START_INDEX.intValue(); index < sheet.rows.size(); index++) {
            XlsxRowLike row = sheet.rows.get(index);
            Map<String, Object> item = new LinkedHashMap<String, Object>();
            item.put("Field", util.readCell(row.cells, 0));
            item.put("Value", util.readCell(row.cells, 1));
            document.ensureSheet("Project").add(item);
        }
    }
}
