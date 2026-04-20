/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuproject.projectxlsx;

public class ProjectXlsxImportCalendars {
    private final ProjectXlsxImportEntities entities = new ProjectXlsxImportEntities();

    public void importCalendarsSheet(jp.igapyon.mikuproject.projectworkbookjson.WorkbookJsonDocument document, XlsxSheetLike sheet,
            String[] headers) {
        entities.importTabularSheet(document, sheet, headers);
    }

    public void importNonWorkingDaysSheet(jp.igapyon.mikuproject.projectworkbookjson.WorkbookJsonDocument document,
            XlsxSheetLike sheet, String[] headers) {
        entities.importTabularSheet(document, sheet, headers);
    }
}
