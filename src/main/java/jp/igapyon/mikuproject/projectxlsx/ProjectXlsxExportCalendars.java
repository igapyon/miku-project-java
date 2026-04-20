/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuproject.projectxlsx;

import java.util.List;
import java.util.Map;

public class ProjectXlsxExportCalendars {
    private final ProjectXlsxExportEntities entities = new ProjectXlsxExportEntities();
    private final ProjectXlsxExportUtil util = new ProjectXlsxExportUtil();

    public XlsxSheetLike buildCalendarsSheet(String[] headers, List<Map<String, Object>> rows) {
        XlsxSheetLike sheet = entities.buildTabularSheet("Calendars", "Calendar List", headers, rows);
        util.addColumns(sheet, new double[] { 10d, 24d, 16d, 16d, 32d, 32d, 32d });
        int startRow = 4;
        int endRow = startRow + Math.max(0, rows == null ? 0 : rows.size()) - 1;
        util.addBooleanDataValidation(sheet, new String[] { util.buildColumnRange(2, startRow, endRow) });
        return sheet;
    }

    public XlsxSheetLike buildNonWorkingDaysSheet(String[] headers, List<Map<String, Object>> rows) {
        XlsxSheetLike sheet = entities.buildTabularSheet("NonWorkingDays", "NonWorkingDay List", headers, rows);
        util.addColumns(sheet, new double[] { 12d, 8d, 24d, 24d, 14d, 14d, 14d, 14d });
        int startRow = 4;
        int endRow = startRow + Math.max(0, rows == null ? 0 : rows.size()) - 1;
        util.addBooleanDataValidation(sheet, new String[] { util.buildColumnRange(7, startRow, endRow) });
        return sheet;
    }
}
