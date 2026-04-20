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
        sheet.rows.add(util.titleRow("Project"));
        sheet.rows.add(util.sectionTitleRow("Basic Info"));
        sheet.rows.add(util.headerRow(new String[] { "Field", "Value" }));
        List<Map<String, Object>> rows = document.sheets.get("Project");
        int scheduleFromStartRowNumber = -1;
        for (Map<String, Object> row : rows) {
            if ("ScheduleFromStart".equals(String.valueOf(row.get("Field")))) {
                scheduleFromStartRowNumber = sheet.rows.size() + 1;
            }
            sheet.rows.add(util.keyValueRow(row.get("Field"), row.get("Value")));
        }
        util.addBooleanDataValidation(sheet, new String[] { util.buildSingleCellReference(1, scheduleFromStartRowNumber) });
        return sheet;
    }
}
