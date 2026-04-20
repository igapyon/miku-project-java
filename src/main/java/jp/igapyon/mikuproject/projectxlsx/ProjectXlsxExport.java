/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuproject.projectxlsx;

import java.util.List;
import java.util.Map;

import jp.igapyon.mikuproject.model.ProjectModel;
import jp.igapyon.mikuproject.projectworkbookjson.ProjectWorkbookJson;
import jp.igapyon.mikuproject.projectworkbookjson.ProjectWorkbookSchema;
import jp.igapyon.mikuproject.projectworkbookjson.WorkbookJsonDocument;

public class ProjectXlsxExport {
    private final ProjectXlsxExportProject exportProject = new ProjectXlsxExportProject();
    private final ProjectXlsxExportEntities exportEntities = new ProjectXlsxExportEntities();
    private final ProjectXlsxExportCalendars exportCalendars = new ProjectXlsxExportCalendars();
    private final ProjectXlsxExportUtil util = new ProjectXlsxExportUtil();

    public XlsxWorkbookLike exportProjectWorkbook(ProjectModel model) {
        WorkbookJsonDocument document = new ProjectWorkbookJson().exportProjectWorkbookJson(model);
        XlsxWorkbookLike workbook = new XlsxWorkbookLike();
        workbook.sheets.add(exportProject.buildProjectSheet(document));
        workbook.sheets.add(exportEntities.buildTabularSheet("Tasks", "Task List", ProjectWorkbookSchema.TASK_HEADERS,
                document.sheets.get("Tasks")));
        workbook.sheets.add(exportEntities.buildTabularSheet("Resources", "Resource List", ProjectWorkbookSchema.RESOURCE_HEADERS,
                document.sheets.get("Resources")));
        workbook.sheets.add(exportEntities.buildTabularSheet("Assignments", "Assignment List", ProjectWorkbookSchema.ASSIGNMENT_HEADERS,
                document.sheets.get("Assignments")));
        workbook.sheets.add(exportCalendars.buildCalendarsSheet(ProjectWorkbookSchema.CALENDAR_HEADERS, document.sheets.get("Calendars")));
        workbook.sheets.add(exportCalendars.buildNonWorkingDaysSheet(
                ProjectWorkbookSchema.NON_WORKING_DAYS_HEADERS, document.sheets.get("NonWorkingDays")));
        workbook.sheets.add(buildOptionsSheet());
        return workbook;
    }

    private XlsxSheetLike buildOptionsSheet() {
        XlsxSheetLike sheet = new XlsxSheetLike();
        sheet.name = ProjectXlsxExportUtil.OPTIONS_SHEET_NAME;
        util.addColumns(sheet, new double[] { 18d, 14d });
        sheet.rows.add(util.headerRow(new String[] { "BooleanChoice", "Meaning" }));
        XlsxRowLike trueRow = new XlsxRowLike();
        trueRow.cells.add(util.cell("○"));
        trueRow.cells.add(util.cell("true"));
        sheet.rows.add(trueRow);
        XlsxRowLike falseRow = new XlsxRowLike();
        falseRow.cells.add(util.cell("ー"));
        falseRow.cells.add(util.cell("false"));
        sheet.rows.add(falseRow);
        return sheet;
    }
}
