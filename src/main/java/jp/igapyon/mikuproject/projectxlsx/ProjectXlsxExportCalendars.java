/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuproject.projectxlsx;

import java.util.List;
import java.util.Map;

public class ProjectXlsxExportCalendars {
    private final ProjectXlsxExportEntities entities = new ProjectXlsxExportEntities();

    public XlsxSheetLike buildCalendarsSheet(String[] headers, List<Map<String, Object>> rows) {
        return entities.buildTabularSheet("Calendars", "Calendar List", headers, rows);
    }

    public XlsxSheetLike buildNonWorkingDaysSheet(String[] headers, List<Map<String, Object>> rows) {
        return entities.buildTabularSheet("NonWorkingDays", "NonWorkingDay List", headers, rows);
    }
}
