/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuproject.projectxlsx;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jp.igapyon.mikuproject.model.ProjectModel;
import jp.igapyon.mikuproject.projectworkbookjson.ProjectWorkbookJson;
import jp.igapyon.mikuproject.projectworkbookjson.ProjectWorkbookJsonImport;
import jp.igapyon.mikuproject.projectworkbookjson.ProjectWorkbookSchema;
import jp.igapyon.mikuproject.projectworkbookjson.WorkbookJsonDocument;

public class ProjectXlsxImport {
    private final ProjectXlsxImportUtil util = new ProjectXlsxImportUtil();
    private final ProjectXlsxImportProject importProject = new ProjectXlsxImportProject();
    private final ProjectXlsxImportEntities importEntities = new ProjectXlsxImportEntities();
    private final ProjectXlsxImportCalendars importCalendars = new ProjectXlsxImportCalendars();

    public ProjectModel importProjectWorkbook(XlsxWorkbookLike workbook, ProjectModel baseModel) {
        return importProjectWorkbookDetailed(workbook, baseModel).model;
    }

    public ProjectModel importProjectWorkbookAsProjectModel(XlsxWorkbookLike workbook) {
        return new ProjectWorkbookJson().importProjectWorkbookJsonAsProjectModel(util.toDocumentLike(toWorkbookJsonDocument(workbook))).model;
    }

    public ImportResult importProjectWorkbookDetailed(XlsxWorkbookLike workbook, ProjectModel baseModel) {
        ProjectWorkbookJsonImport.ImportResult imported = new ProjectWorkbookJson().importProjectWorkbookJson(
                util.toDocumentLike(toWorkbookJsonDocument(workbook)), baseModel);
        ImportResult result = new ImportResult();
        result.model = imported.model;
        for (jp.igapyon.mikuproject.projectworkbookjson.ImportChange source : imported.changes) {
            ImportChange change = new ImportChange();
            change.scope = source.scope;
            change.uid = source.uid;
            change.label = source.label;
            change.field = source.field;
            change.before = source.before;
            change.after = source.after;
            result.changes.add(change);
        }
        return result;
    }

    private WorkbookJsonDocument toWorkbookJsonDocument(XlsxWorkbookLike workbook) {
        WorkbookJsonDocument document = new WorkbookJsonDocument();
        for (XlsxSheetLike sheet : workbook.sheets) {
            if ("Project".equals(sheet.name)) {
                importProject.importProjectSheet(document, sheet);
            } else if ("Tasks".equals(sheet.name)) {
                importEntities.importTabularSheet(document, sheet, ProjectWorkbookSchema.TASK_HEADERS);
            } else if ("Resources".equals(sheet.name)) {
                importEntities.importTabularSheet(document, sheet, ProjectWorkbookSchema.RESOURCE_HEADERS);
            } else if ("Assignments".equals(sheet.name)) {
                importEntities.importTabularSheet(document, sheet, ProjectWorkbookSchema.ASSIGNMENT_HEADERS);
            } else if ("Calendars".equals(sheet.name)) {
                importCalendars.importCalendarsSheet(document, sheet, ProjectWorkbookSchema.CALENDAR_HEADERS);
            } else if ("NonWorkingDays".equals(sheet.name)) {
                importCalendars.importNonWorkingDaysSheet(document, sheet, ProjectWorkbookSchema.NON_WORKING_DAYS_HEADERS);
            }
        }
        util.ensureAllKnownSheets(document);
        return document;
    }

    public static class ImportResult {
        public ProjectModel model;
        public java.util.List<ImportChange> changes = new java.util.ArrayList<ImportChange>();
    }
}
