/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuproject.projectxlsx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

import jp.igapyon.mikuproject.model.ProjectModel;
import jp.igapyon.mikuproject.msprojectxml.MsProjectCalendar;
import jp.igapyon.mikuproject.msprojectxml.MsProjectSamples;
import jp.igapyon.mikuproject.msprojectxml.MsProjectXml;

public class ProjectXlsxTest {
    @Test
    public void convertsProjectModelIntoWorkbookSheets() {
        ProjectXlsx projectXlsx = new ProjectXlsx();
        ProjectModel model = new MsProjectSamples().buildSampleProjectModel();

        XlsxWorkbookLike workbook = projectXlsx.exportProjectWorkbook(model);

        assertEquals("Project", workbook.sheets.get(0).name);
        assertEquals("Tasks", workbook.sheets.get(1).name);
        assertEquals("Resources", workbook.sheets.get(2).name);
        assertEquals("Assignments", workbook.sheets.get(3).name);
        assertEquals("Calendars", workbook.sheets.get(4).name);
        assertEquals("NonWorkingDays", workbook.sheets.get(5).name);
        assertEquals("Options", workbook.sheets.get(6).name);
        assertEquals("Project", workbook.sheets.get(0).rows.get(0).cells.get(0).value);
        assertEquals("Field", workbook.sheets.get(0).rows.get(2).cells.get(0).value);
        assertEquals("Name", workbook.sheets.get(0).rows.get(3).cells.get(0).value);
        assertEquals("mikuproject開発", workbook.sheets.get(0).rows.get(3).cells.get(1).value);
        assertEquals("Tasks", workbook.sheets.get(1).name);
        assertEquals("Tasks", workbook.sheets.get(1).rows.get(0).cells.get(0).value);
        assertEquals(Double.valueOf(26d), workbook.sheets.get(0).columns.get(0).width);
        assertEquals(Double.valueOf(42d), workbook.sheets.get(0).columns.get(1).width);
        assertEquals("Settings", workbook.sheets.get(0).rows.get(11).cells.get(0).value);
        assertEquals("A11:B11", workbook.sheets.get(0).mergedRanges.get(0));
        assertEquals("#BFD7EA", workbook.sheets.get(0).rows.get(0).cells.get(0).fillColor);
        assertEquals("#D9EAF7", workbook.sheets.get(0).rows.get(2).cells.get(0).fillColor);
        assertEquals("#FDE7C7", workbook.sheets.get(0).rows.get(3).cells.get(1).fillColor);
        assertEquals("#E6EDF4", workbook.sheets.get(1).rows.get(2).cells.get(0).fillColor);
        assertEquals("#FDE7C7", workbook.sheets.get(1).rows.get(3).cells.get(2).fillColor);
        assertEquals("Options!$A$2:$A$3", workbook.sheets.get(0).dataValidations.get(0).formula1);
        assertTrue(workbook.sheets.get(1).dataValidations.get(0).sqref.contains("L4:L"));
        assertEquals(Double.valueOf(28d), workbook.sheets.get(1).columns.get(2).width);
        assertEquals("BooleanChoice", workbook.sheets.get(6).rows.get(0).cells.get(0).value);
        assertEquals("true", workbook.sheets.get(6).rows.get(1).cells.get(1).value);
    }

    @Test
    public void importsLimitedEditableFieldsThroughWorkbook() throws IOException {
        ProjectXlsx projectXlsx = new ProjectXlsx();
        ProjectModel model = new MsProjectXml().importFromXml(readVendorTestdata("dependency.xml"));
        XlsxWorkbookLike workbook = projectXlsx.exportProjectWorkbook(model);
        XlsxSheetLike projectSheet = workbook.sheets.get(0);
        XlsxSheetLike tasksSheet = workbook.sheets.get(1);
        XlsxSheetLike resourcesSheet = workbook.sheets.get(2);
        XlsxSheetLike assignmentsSheet = workbook.sheets.get(3);

        projectSheet.rows.get(3).cells.get(1).value = "XLSX import project";
        tasksSheet.rows.get(4).cells.get(2).value = "XLSX import task";
        tasksSheet.rows.get(4).cells.get(6).value = "2026-03-16 10:00:00";
        tasksSheet.rows.get(4).cells.get(8).value = "PT24H0M0S";
        tasksSheet.rows.get(4).cells.get(11).value = "○";
        tasksSheet.rows.get(4).cells.get(12).value = "○";
        tasksSheet.rows.get(4).cells.get(13).value = "ー";
        tasksSheet.rows.get(4).cells.get(14).value = "2";
        tasksSheet.rows.get(4).cells.get(15).value = "500";
        tasksSheet.rows.get(4).cells.get(16).value = "2";
        tasksSheet.rows.get(4).cells.get(17).value = "4";
        tasksSheet.rows.get(4).cells.get(18).value = "2026-03-17 09:00:00";
        tasksSheet.rows.get(4).cells.get(19).value = "2026-03-18 18:00:00";
        tasksSheet.rows.get(4).cells.get(20).value = "2";
        resourcesSheet.rows.get(3).cells.get(2).value = "Miku Updated";
        resourcesSheet.rows.get(3).cells.get(3).value = "1";
        resourcesSheet.rows.get(3).cells.get(4).value = "MU";
        resourcesSheet.rows.get(3).cells.get(8).value = "1200/h";
        resourcesSheet.rows.get(3).cells.get(9).value = "1800/h";
        resourcesSheet.rows.get(3).cells.get(10).value = "500";
        resourcesSheet.rows.get(3).cells.get(11).value = "PT8H0M0S";
        resourcesSheet.rows.get(3).cells.get(12).value = "PT4H0M0S";
        resourcesSheet.rows.get(3).cells.get(13).value = "PT4H0M0S";
        resourcesSheet.rows.get(3).cells.get(14).value = "1000";
        resourcesSheet.rows.get(3).cells.get(15).value = "500";
        resourcesSheet.rows.get(3).cells.get(16).value = "500";
        resourcesSheet.rows.get(3).cells.get(17).value = "50";
        resourcesSheet.rows.get(3).cells.get(18).value = "0";
        resourcesSheet.rows.get(3).cells.get(19).value = "2";
        resourcesSheet.rows.get(3).cells.get(20).value = "2";
        assignmentsSheet.rows.get(3).cells.get(5).value = "2026-03-16 09:00:00";
        assignmentsSheet.rows.get(3).cells.get(6).value = "2026-03-16 18:00:00";
        assignmentsSheet.rows.get(3).cells.get(7).value = "PT0H30M0S";
        assignmentsSheet.rows.get(3).cells.get(8).value = "PT1H0M0S";
        assignmentsSheet.rows.get(3).cells.get(9).value = "PT0H15M0S";
        assignmentsSheet.rows.get(3).cells.get(10).value = "○";
        assignmentsSheet.rows.get(3).cells.get(11).value = "1";
        assignmentsSheet.rows.get(3).cells.get(14).value = "1000";
        assignmentsSheet.rows.get(3).cells.get(15).value = "PT4H0M0S";
        assignmentsSheet.rows.get(3).cells.get(16).value = "PT4H0M0S";
        assignmentsSheet.rows.get(3).cells.get(17).value = "300";
        assignmentsSheet.rows.get(3).cells.get(18).value = "200";
        assignmentsSheet.rows.get(3).cells.get(19).value = "PT1H0M0S";
        assignmentsSheet.rows.get(3).cells.get(20).value = "PT0H30M0S";

        ProjectXlsxImport.ImportResult result = projectXlsx.importProjectWorkbookDetailed(workbook, model);

        assertEquals("XLSX import project", result.model.project.name);
        assertEquals("XLSX import task", result.model.tasks.get(1).name);
        assertEquals("2026-03-16T10:00:00", result.model.tasks.get(1).start);
        assertEquals("PT24H0M0S", result.model.tasks.get(1).duration);
        assertTrue(result.model.tasks.get(1).milestone);
        assertTrue(result.model.tasks.get(1).summary);
        assertEquals(Boolean.FALSE, result.model.tasks.get(1).critical);
        assertEquals(Integer.valueOf(2), result.model.tasks.get(1).type);
        assertEquals(Integer.valueOf(500), result.model.tasks.get(1).priority);
        assertEquals("2", result.model.tasks.get(1).calendarUID);
        assertEquals(Integer.valueOf(4), result.model.tasks.get(1).constraintType);
        assertEquals("2026-03-17T09:00:00", result.model.tasks.get(1).constraintDate);
        assertEquals("2026-03-18T18:00:00", result.model.tasks.get(1).deadline);
        assertEquals("2", result.model.tasks.get(1).predecessors.get(0).predecessorUid);
        assertEquals("Miku Updated", result.model.resources.get(0).name);
        assertEquals(Integer.valueOf(1), result.model.resources.get(0).type);
        assertEquals("MU", result.model.resources.get(0).initials);
        assertEquals("1200/h", result.model.resources.get(0).standardRate);
        assertEquals("1800/h", result.model.resources.get(0).overtimeRate);
        assertEquals(Double.valueOf(500), result.model.resources.get(0).costPerUse);
        assertEquals("PT8H0M0S", result.model.resources.get(0).work);
        assertEquals("PT4H0M0S", result.model.resources.get(0).actualWork);
        assertEquals("PT4H0M0S", result.model.resources.get(0).remainingWork);
        assertEquals(Double.valueOf(1000), result.model.resources.get(0).cost);
        assertEquals(Double.valueOf(500), result.model.resources.get(0).actualCost);
        assertEquals(Double.valueOf(500), result.model.resources.get(0).remainingCost);
        assertEquals(Integer.valueOf(50), result.model.resources.get(0).percentWorkComplete);
        assertEquals(Integer.valueOf(0), result.model.resources.get(0).workGroup);
        assertEquals(Integer.valueOf(2), result.model.resources.get(0).standardRateFormat);
        assertEquals(Integer.valueOf(2), result.model.resources.get(0).overtimeRateFormat);
        assertEquals("2026-03-16T09:00:00", result.model.assignments.get(0).start);
        assertEquals("2026-03-16T18:00:00", result.model.assignments.get(0).finish);
        assertEquals("PT0H30M0S", result.model.assignments.get(0).startVariance);
        assertEquals("PT1H0M0S", result.model.assignments.get(0).finishVariance);
        assertEquals("PT0H15M0S", result.model.assignments.get(0).delay);
        assertEquals(Boolean.TRUE, result.model.assignments.get(0).milestone);
        assertEquals(Integer.valueOf(1), result.model.assignments.get(0).workContour);
        assertEquals(Double.valueOf(1000), result.model.assignments.get(0).cost);
        assertEquals("PT4H0M0S", result.model.assignments.get(0).actualWork);
        assertEquals("PT4H0M0S", result.model.assignments.get(0).remainingWork);
        assertEquals(Double.valueOf(300), result.model.assignments.get(0).actualCost);
        assertEquals(Double.valueOf(200), result.model.assignments.get(0).remainingCost);
        assertEquals("PT1H0M0S", result.model.assignments.get(0).overtimeWork);
        assertEquals("PT0H30M0S", result.model.assignments.get(0).actualOvertimeWork);
        assertTrue(result.changes.size() > 0);
    }

    @Test
    public void importsWorkbookAsProjectModel() {
        ProjectXlsx projectXlsx = new ProjectXlsx();
        ProjectModel sample = new MsProjectSamples().buildSampleProjectModel();
        XlsxWorkbookLike workbook = projectXlsx.exportProjectWorkbook(sample);

        ProjectModel imported = projectXlsx.importProjectWorkbookAsProjectModel(workbook);

        assertEquals(sample.project.name, imported.project.name);
        assertEquals(sample.tasks.size(), imported.tasks.size());
        assertEquals(sample.resources.size(), imported.resources.size());
        assertEquals(sample.assignments.size(), imported.assignments.size());
    }

    @Test
    public void roundTripsHierarchyFixtureThroughWorkbook() throws IOException {
        ProjectXlsx projectXlsx = new ProjectXlsx();
        ProjectModel model = new MsProjectXml().importFromXml(readVendorTestdata("hierarchy.xml"));

        XlsxWorkbookLike workbook = projectXlsx.exportProjectWorkbook(model);
        ProjectModel imported = projectXlsx.importProjectWorkbookAsProjectModel(workbook);

        assertEquals("Hierarchy Project", imported.project.name);
        assertEquals(3, imported.tasks.size());
        assertEquals("Summary", imported.tasks.get(0).name);
        assertEquals("1", imported.tasks.get(0).outlineNumber);
        assertEquals("1.1", imported.tasks.get(1).outlineNumber);
        assertEquals("1.2", imported.tasks.get(2).outlineNumber);
    }

    @Test
    public void importsCalendarBooleanColumnsThroughWorkbook() {
        ProjectXlsx projectXlsx = new ProjectXlsx();
        ProjectModel model = buildCalendarWorkbookBaseModel();
        XlsxWorkbookLike workbook = projectXlsx.exportProjectWorkbook(model);
        XlsxSheetLike calendarsSheet = findSheet(workbook, "Calendars");
        XlsxSheetLike nonWorkingDaysSheet = findSheet(workbook, "NonWorkingDays");
        XlsxRowLike calendarRow = findRowByCellValue(calendarsSheet, 0, model.calendars.get(0).uid);
        XlsxRowLike nonWorkingDayRow = findNonWorkingDayRow(nonWorkingDaysSheet, model.calendars.get(0).uid, "0");

        calendarRow.cells.get(2).value = "ー";
        nonWorkingDayRow.cells.get(7).value = "○";

        ProjectXlsxImport.ImportResult result = projectXlsx.importProjectWorkbookDetailed(workbook, model);

        assertEquals(Boolean.FALSE, Boolean.valueOf(result.model.calendars.get(0).isBaseCalendar));
        assertEquals(Boolean.TRUE, result.model.calendars.get(0).exceptions.get(0).dayWorking);
        assertTrue(result.changes.size() > 0);
    }

    private ProjectModel buildCalendarWorkbookBaseModel() {
        ProjectModel model = new ProjectModel();
        model.project.startDate = "2026-05-01T09:00:00";
        model.project.finishDate = "2026-05-05T18:00:00";
        model.project.defaultStartTime = "09:00:00";
        model.project.defaultFinishTime = "18:00:00";
        return new MsProjectCalendar().ensureDefaultProjectCalendar(model);
    }

    private XlsxSheetLike findSheet(XlsxWorkbookLike workbook, String name) {
        for (XlsxSheetLike sheet : workbook.sheets) {
            if (name.equals(sheet.name)) {
                return sheet;
            }
        }
        return null;
    }

    private XlsxRowLike findRowByCellValue(XlsxSheetLike sheet, int cellIndex, String value) {
        for (int index = 3; index < sheet.rows.size(); index += 1) {
            XlsxRowLike row = sheet.rows.get(index);
            if (value.equals(String.valueOf(row.cells.get(cellIndex).value))) {
                return row;
            }
        }
        return null;
    }

    private XlsxRowLike findNonWorkingDayRow(XlsxSheetLike sheet, String calendarUid, String rowIndex) {
        for (int index = 3; index < sheet.rows.size(); index += 1) {
            XlsxRowLike row = sheet.rows.get(index);
            if (calendarUid.equals(String.valueOf(row.cells.get(0).value))
                    && rowIndex.equals(String.valueOf(row.cells.get(1).value))) {
                return row;
            }
        }
        return null;
    }

    private String readVendorTestdata(String fileName) throws IOException {
        byte[] bytes = Files.readAllBytes(Paths.get("vendor", "mikuproject", "testdata", fileName));
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
