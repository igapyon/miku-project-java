/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuproject.projectworkbookjson;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ProjectWorkbookJsonValidate {
    public ValidationResult validateWorkbookJsonDocument(Object documentLike) {
        if (!(documentLike instanceof Map<?, ?>)) {
            throw new IllegalArgumentException("workbook JSON がオブジェクトではありません");
        }
        Map<?, ?> raw = (Map<?, ?>) documentLike;
        if (!"mikuproject_workbook_json".equals(raw.get("format"))) {
            throw new IllegalArgumentException("format が mikuproject_workbook_json ではありません");
        }
        if (!(raw.get("version") instanceof Number) || ((Number) raw.get("version")).intValue() != 1) {
            throw new IllegalArgumentException("version は 1 である必要があります");
        }
        if (!(raw.get("sheets") instanceof Map<?, ?>)) {
            throw new IllegalArgumentException("sheets がありません");
        }
        WorkbookJsonDocument document = new WorkbookJsonDocument();
        document.sheets.clear();
        List<WorkbookJsonWarning> warnings = new ArrayList<WorkbookJsonWarning>();
        Map<?, ?> rawSheets = (Map<?, ?>) raw.get("sheets");
        for (Map.Entry<?, ?> entry : rawSheets.entrySet()) {
            String sheetName = String.valueOf(entry.getKey());
            if (!isKnownSheet(sheetName)) {
                warnings.add(warning("未知の sheet は無視します: " + sheetName));
                continue;
            }
            if (!(entry.getValue() instanceof List<?>)) {
                throw new IllegalArgumentException("sheets." + sheetName + " は配列である必要があります");
            }
            List<Map<String, Object>> rows = new ArrayList<Map<String, Object>>();
            List<?> rawRows = (List<?>) entry.getValue();
            for (int rowIndex = 0; rowIndex < rawRows.size(); rowIndex++) {
                Object rowValue = rawRows.get(rowIndex);
                if (!(rowValue instanceof Map<?, ?>)) {
                    throw new IllegalArgumentException("sheets." + sheetName + " にオブジェクトではない行があります");
                }
                Map<String, Object> row = new LinkedHashMap<String, Object>();
                for (Map.Entry<?, ?> rowEntry : ((Map<?, ?>) rowValue).entrySet()) {
                    String key = String.valueOf(rowEntry.getKey());
                    if (!isKnownColumn(sheetName, key)) {
                        warnings.add(warning("未知の列は無視します: " + sheetName + "[" + rowIndex + "]." + key));
                    }
                    row.put(key, rowEntry.getValue());
                }
                rows.add(row);
            }
            document.sheets.put(sheetName, rows);
        }
        for (String sheetName : ProjectWorkbookSchema.SHEET_NAMES) {
            document.ensureSheet(sheetName);
        }
        ValidationResult result = new ValidationResult();
        result.document = document;
        result.warnings = warnings;
        return result;
    }

    private boolean isKnownSheet(String sheetName) {
        return Arrays.asList(ProjectWorkbookSchema.SHEET_NAMES).contains(sheetName);
    }

    private boolean isKnownColumn(String sheetName, String key) {
        if ("Project".equals(sheetName)) {
            return "Field".equals(key) || "Value".equals(key);
        }
        if ("Tasks".equals(sheetName)) {
            return Arrays.asList(ProjectWorkbookSchema.TASK_HEADERS).contains(key);
        }
        if ("Resources".equals(sheetName)) {
            return Arrays.asList(ProjectWorkbookSchema.RESOURCE_HEADERS).contains(key);
        }
        if ("Assignments".equals(sheetName)) {
            return Arrays.asList(ProjectWorkbookSchema.ASSIGNMENT_HEADERS).contains(key);
        }
        if ("Calendars".equals(sheetName)) {
            return Arrays.asList(ProjectWorkbookSchema.CALENDAR_HEADERS).contains(key);
        }
        if ("NonWorkingDays".equals(sheetName)) {
            return Arrays.asList(ProjectWorkbookSchema.NON_WORKING_DAYS_HEADERS).contains(key);
        }
        return false;
    }

    private WorkbookJsonWarning warning(String message) {
        WorkbookJsonWarning warning = new WorkbookJsonWarning();
        warning.message = message;
        return warning;
    }

    public static class ValidationResult {
        public WorkbookJsonDocument document;
        public List<WorkbookJsonWarning> warnings;
    }
}
