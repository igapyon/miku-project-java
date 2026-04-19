/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuproject.projectworkbookjson;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class WorkbookJsonDocument {
    public String format = "mikuproject_workbook_json";
    public Integer version = Integer.valueOf(1);
    public Map<String, List<Map<String, Object>>> sheets = new LinkedHashMap<String, List<Map<String, Object>>>();

    public List<Map<String, Object>> ensureSheet(String sheetName) {
        List<Map<String, Object>> rows = sheets.get(sheetName);
        if (rows == null) {
            rows = new ArrayList<Map<String, Object>>();
            sheets.put(sheetName, rows);
        }
        return rows;
    }
}
