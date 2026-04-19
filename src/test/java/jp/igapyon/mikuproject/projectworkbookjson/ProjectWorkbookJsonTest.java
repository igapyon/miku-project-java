/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuproject.projectworkbookjson;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import jp.igapyon.mikuproject.model.ProjectModel;
import jp.igapyon.mikuproject.model.TaskModel;
import jp.igapyon.mikuproject.msprojectxml.MsProjectSamples;
import jp.igapyon.mikuproject.msprojectxml.MsProjectXml;

public class ProjectWorkbookJsonTest {
    @Test
    public void exportsWorkbookJsonWithFixedFormatAndSheets() {
        ProjectWorkbookJson workbookJson = new ProjectWorkbookJson();
        ProjectModel model = new MsProjectSamples().buildSampleProjectModel();

        WorkbookJsonDocument document = workbookJson.exportProjectWorkbookJson(model);

        assertEquals("mikuproject_workbook_json", document.format);
        assertEquals(Integer.valueOf(1), document.version);
        assertEquals("[Project, Tasks, Resources, Assignments, Calendars, NonWorkingDays]", document.sheets.keySet().toString());
        assertEquals("Name", document.sheets.get("Project").get(0).get("Field"));
        assertEquals("mikuproject開発", document.sheets.get("Project").get(0).get("Value"));
        assertEquals("基盤整備", document.sheets.get("Tasks").get(0).get("Name"));
        assertTrue(document.sheets.get("Tasks").get(0).get("UID") != null);
        assertEquals("Mikuku", document.sheets.get("Resources").get(0).get("Name"));
        assertEquals("Mikuku", document.sheets.get("Assignments").get(0).get("ResourceName"));
    }

    @Test
    public void importsLimitedEditableFieldsThroughWorkbookJson() throws IOException {
        ProjectWorkbookJson workbookJson = new ProjectWorkbookJson();
        ProjectModel baseModel = new MsProjectXml().importFromXml(readVendorTestdata("dependency.xml"));
        WorkbookJsonDocument document = workbookJson.exportProjectWorkbookJson(baseModel);
        Map<String, Object> projectRow = findProjectRow(document.sheets.get("Project"), "Name");
        Map<String, Object> taskRow = findRowByUid(document.sheets.get("Tasks"), "2");
        Map<String, Object> resourceRow = findRowByUid(document.sheets.get("Resources"), "1");

        projectRow.put("Value", "JSON import project");
        taskRow.put("Name", "JSON import task");
        taskRow.put("Start", "2026-03-16 10:00:00");
        taskRow.put("Duration", "PT24H0M0S");
        taskRow.put("Milestone", "○");
        taskRow.put("Summary", "○");
        taskRow.put("Critical", "ー");
        taskRow.put("CalendarUID", "2");
        taskRow.put("Predecessors", "2");
        taskRow.put("OutlineNumber", "999");
        resourceRow.put("Name", "Miku Updated");
        resourceRow.put("Group", "Dev");
        resourceRow.put("MaxUnits", Integer.valueOf(1));
        resourceRow.put("CalendarUID", "1");

        ProjectWorkbookJsonImport.ImportResult result = workbookJson.importProjectWorkbookJson(toDocumentLike(document), baseModel);

        assertEquals("JSON import project", result.model.project.name);
        assertEquals("JSON import task", result.model.tasks.get(1).name);
        assertEquals("2026-03-16T10:00:00", result.model.tasks.get(1).start);
        assertEquals("PT24H0M0S", result.model.tasks.get(1).duration);
        assertTrue(result.model.tasks.get(1).milestone);
        assertTrue(result.model.tasks.get(1).summary);
        assertEquals(Boolean.FALSE, result.model.tasks.get(1).critical);
        assertEquals("2", result.model.tasks.get(1).calendarUID);
        assertEquals(1, result.model.tasks.get(1).predecessors.size());
        assertEquals("2", result.model.tasks.get(1).predecessors.get(0).predecessorUid);
        assertEquals("Miku Updated", result.model.resources.get(0).name);
        assertEquals("Dev", result.model.resources.get(0).group);
        assertEquals(Double.valueOf(1), result.model.resources.get(0).maxUnits);
        assertEquals("1", result.model.resources.get(0).calendarUID);
        assertEquals(baseModel.tasks.get(1).outlineNumber, result.model.tasks.get(1).outlineNumber);
        assertTrue(hasChangeField(result.changes, "Name"));
    }

    @Test
    public void rejectsInvalidWorkbookJsonFormat() {
        ProjectWorkbookJson workbookJson = new ProjectWorkbookJson();
        Map<String, Object> invalid = new LinkedHashMap<String, Object>();
        invalid.put("format", "other");
        invalid.put("version", Integer.valueOf(1));
        invalid.put("sheets", new LinkedHashMap<String, Object>());

        assertThrows(IllegalArgumentException.class, new org.junit.jupiter.api.function.Executable() {
            @Override
            public void execute() {
                workbookJson.validateWorkbookJsonDocument(invalid);
            }
        });
    }

    @Test
    public void reportsWarningsForUnknownSheetAndUnknownColumns() {
        ProjectWorkbookJson workbookJson = new ProjectWorkbookJson();
        Map<String, Object> document = new LinkedHashMap<String, Object>();
        Map<String, Object> sheets = new LinkedHashMap<String, Object>();
        document.put("format", "mikuproject_workbook_json");
        document.put("version", Integer.valueOf(1));
        document.put("sheets", sheets);
        sheets.put("Project", java.util.Arrays.<Object>asList(mapOf("Field", "Name", "Value", "x", "Extra", "ignored")));
        sheets.put("UnknownSheet", java.util.Arrays.<Object>asList());

        ProjectWorkbookJsonValidate.ValidationResult result = workbookJson.validateWorkbookJsonDocument(document);

        assertEquals("未知の列は無視します: Project[0].Extra", result.warnings.get(0).message);
        assertEquals("未知の sheet は無視します: UnknownSheet", result.warnings.get(1).message);
    }

    @Test
    public void rejectsNonArraySheetsAndNonObjectRows() {
        ProjectWorkbookJson workbookJson = new ProjectWorkbookJson();
        Map<String, Object> invalidSheet = new LinkedHashMap<String, Object>();
        invalidSheet.put("format", "mikuproject_workbook_json");
        invalidSheet.put("version", Integer.valueOf(1));
        invalidSheet.put("sheets", mapOf("Project", new LinkedHashMap<String, Object>()));
        Map<String, Object> invalidRow = new LinkedHashMap<String, Object>();
        invalidRow.put("format", "mikuproject_workbook_json");
        invalidRow.put("version", Integer.valueOf(1));
        invalidRow.put("sheets", mapOf("Tasks", java.util.Arrays.<Object>asList(new Object[] { null })));

        assertThrows(IllegalArgumentException.class, new org.junit.jupiter.api.function.Executable() {
            @Override
            public void execute() {
                workbookJson.validateWorkbookJsonDocument(invalidSheet);
            }
        });
        assertThrows(IllegalArgumentException.class, new org.junit.jupiter.api.function.Executable() {
            @Override
            public void execute() {
                workbookJson.validateWorkbookJsonDocument(invalidRow);
            }
        });
    }

    @Test
    public void keepsNonEditableTaskColumnsUnchangedThroughWorkbookJsonImport() throws IOException {
        ProjectWorkbookJson workbookJson = new ProjectWorkbookJson();
        ProjectModel baseModel = new MsProjectXml().importFromXml(readVendorTestdata("dependency.xml"));
        WorkbookJsonDocument document = workbookJson.exportProjectWorkbookJson(baseModel);
        Map<String, Object> taskRow = findRowByUid(document.sheets.get("Tasks"), "2");

        taskRow.put("ID", "999");
        taskRow.put("OutlineLevel", Integer.valueOf(9));
        taskRow.put("OutlineNumber", "999");
        taskRow.put("WBS", "WBS-999");

        ProjectWorkbookJsonImport.ImportResult result = workbookJson.importProjectWorkbookJson(toDocumentLike(document), baseModel);
        TaskModel importedTask = findTask(result.model, "2");
        TaskModel baseTask = findTask(baseModel, "2");

        assertEquals(baseTask.id, importedTask.id);
        assertEquals(baseTask.outlineLevel, importedTask.outlineLevel);
        assertEquals(baseTask.outlineNumber, importedTask.outlineNumber);
        assertEquals(baseTask.wbs, importedTask.wbs);
        assertEquals(0, result.changes.size());
    }

    @Test
    public void roundTripsHierarchyFixtureThroughWorkbookJson() throws IOException {
        ProjectWorkbookJson workbookJson = new ProjectWorkbookJson();
        ProjectModel baseModel = new MsProjectXml().importFromXml(readVendorTestdata("hierarchy.xml"));

        WorkbookJsonDocument document = workbookJson.exportProjectWorkbookJson(baseModel);
        ProjectWorkbookJsonImport.ImportAsProjectModelResult imported =
                workbookJson.importProjectWorkbookJsonAsProjectModel(toDocumentLike(document));

        assertEquals("Hierarchy Project", imported.model.project.name);
        assertEquals(3, imported.model.tasks.size());
        assertEquals("Summary", imported.model.tasks.get(0).name);
        assertEquals("1", imported.model.tasks.get(0).outlineNumber);
        assertEquals("1.1", imported.model.tasks.get(1).outlineNumber);
        assertEquals("1.2", imported.model.tasks.get(2).outlineNumber);
        assertEquals("Second child task", imported.model.tasks.get(2).notes);
    }

    private TaskModel findTask(ProjectModel model, String uid) {
        for (TaskModel task : model.tasks) {
            if (uid.equals(task.uid)) {
                return task;
            }
        }
        return null;
    }

    private boolean hasChangeField(List<ImportChange> changes, String field) {
        for (ImportChange change : changes) {
            if (field.equals(change.field)) {
                return true;
            }
        }
        return false;
    }

    private Map<String, Object> findProjectRow(List<Map<String, Object>> rows, String field) {
        for (Map<String, Object> row : rows) {
            if (field.equals(row.get("Field"))) {
                return row;
            }
        }
        return null;
    }

    private Map<String, Object> findRowByUid(List<Map<String, Object>> rows, String uid) {
        for (Map<String, Object> row : rows) {
            if (uid.equals(row.get("UID"))) {
                return row;
            }
        }
        return null;
    }

    private Map<String, Object> toDocumentLike(WorkbookJsonDocument document) {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        map.put("format", document.format);
        map.put("version", document.version);
        map.put("sheets", document.sheets);
        return map;
    }

    private String readVendorTestdata(String fileName) throws IOException {
        byte[] bytes = Files.readAllBytes(Paths.get("vendor", "mikuproject", "testdata", fileName));
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private Map<String, Object> mapOf(Object... entries) {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        for (int i = 0; i < entries.length; i += 2) {
            map.put(String.valueOf(entries[i]), entries[i + 1]);
        }
        return map;
    }
}
