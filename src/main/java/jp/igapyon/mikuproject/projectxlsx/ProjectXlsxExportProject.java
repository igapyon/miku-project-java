/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuproject.projectxlsx;

import java.util.List;
import java.util.Map;

import jp.igapyon.mikuproject.projectworkbookjson.WorkbookJsonDocument;

public class ProjectXlsxExportProject {
    private final ProjectXlsxExportUtil util = new ProjectXlsxExportUtil();

    public XlsxSheetLike buildProjectSheet(WorkbookJsonDocument document) {
        XlsxSheetLike sheet = new XlsxSheetLike();
        sheet.name = "Project";
        util.addColumns(sheet, new double[] { 26d, 42d });
        sheet.rows.add(util.titleRow("Project", ProjectXlsxExportUtil.PROJECT_THEME.section));
        sheet.rows.add(util.titleRow("Basic Info", ProjectXlsxExportUtil.PROJECT_THEME.section));
        sheet.rows.add(util.headerRow(new String[] { "Field", "Value" }, ProjectXlsxExportUtil.PROJECT_THEME.header));
        List<Map<String, Object>> rows = document.sheets.get("Project");
        int scheduleFromStartRowNumber = -1;
        for (int index = 0; index < rows.size(); index++) {
            if (index == 8) {
                sheet.rows.add(util.titleRow("Settings", ProjectXlsxExportUtil.PROJECT_THEME.section));
                sheet.mergedRanges.add("A11:B11");
            }
            Map<String, Object> row = rows.get(index);
            Object field = row.get("Field");
            if ("ScheduleFromStart".equals(String.valueOf(field))) {
                scheduleFromStartRowNumber = sheet.rows.size() + 1;
            }
            sheet.rows.add(util.keyValueRow(field, row.get("Value"), ProjectXlsxExportUtil.PROJECT_THEME.label));
        }
        util.addBooleanDataValidation(sheet, new String[] { util.buildSingleCellReference(1, scheduleFromStartRowNumber) });
        return sheet;
    }
}
