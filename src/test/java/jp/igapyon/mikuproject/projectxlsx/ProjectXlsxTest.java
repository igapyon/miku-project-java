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
    }

    @Test
    public void importsLimitedEditableFieldsThroughWorkbook() throws IOException {
        ProjectXlsx projectXlsx = new ProjectXlsx();
        ProjectModel model = new MsProjectXml().importFromXml(readVendorTestdata("dependency.xml"));
        XlsxWorkbookLike workbook = projectXlsx.exportProjectWorkbook(model);
        XlsxSheetLike projectSheet = workbook.sheets.get(0);
        XlsxSheetLike tasksSheet = workbook.sheets.get(1);
        XlsxSheetLike resourcesSheet = workbook.sheets.get(2);

        projectSheet.rows.get(3).cells.get(1).value = "XLSX import project";
        tasksSheet.rows.get(4).cells.get(2).value = "XLSX import task";
        tasksSheet.rows.get(4).cells.get(6).value = "2026-03-16 10:00:00";
        tasksSheet.rows.get(4).cells.get(8).value = "PT24H0M0S";
        tasksSheet.rows.get(4).cells.get(11).value = "○";
        tasksSheet.rows.get(4).cells.get(12).value = "○";
        tasksSheet.rows.get(4).cells.get(13).value = "ー";
        tasksSheet.rows.get(4).cells.get(14).value = "2";
        tasksSheet.rows.get(4).cells.get(15).value = "2";
        resourcesSheet.rows.get(3).cells.get(2).value = "Miku Updated";

        ProjectXlsxImport.ImportResult result = projectXlsx.importProjectWorkbookDetailed(workbook, model);

        assertEquals("XLSX import project", result.model.project.name);
        assertEquals("XLSX import task", result.model.tasks.get(1).name);
        assertEquals("2026-03-16T10:00:00", result.model.tasks.get(1).start);
        assertEquals("PT24H0M0S", result.model.tasks.get(1).duration);
        assertTrue(result.model.tasks.get(1).milestone);
        assertTrue(result.model.tasks.get(1).summary);
        assertEquals(Boolean.FALSE, result.model.tasks.get(1).critical);
        assertEquals("2", result.model.tasks.get(1).calendarUID);
        assertEquals("2", result.model.tasks.get(1).predecessors.get(0).predecessorUid);
        assertEquals("Miku Updated", result.model.resources.get(0).name);
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

    private String readVendorTestdata(String fileName) throws IOException {
        byte[] bytes = Files.readAllBytes(Paths.get("vendor", "mikuproject", "testdata", fileName));
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
