/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuproject.projectxlsx;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jp.igapyon.mikuproject.projectworkbookjson.ProjectWorkbookSchema;
import jp.igapyon.mikuproject.projectworkbookjson.WorkbookJsonDocument;

public class ProjectXlsxImportUtil {
    public Map<String, Object> toDocumentLike(WorkbookJsonDocument document) {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        map.put("format", document.format);
        map.put("version", document.version);
        map.put("sheets", document.sheets);
        return map;
    }

    public Object readCell(List<XlsxCellLike> cells, int index) {
        return index < cells.size() ? cells.get(index).value : null;
    }

    public void ensureAllKnownSheets(WorkbookJsonDocument document) {
        for (String sheetName : ProjectWorkbookSchema.SHEET_NAMES) {
            document.ensureSheet(sheetName);
        }
    }
}
