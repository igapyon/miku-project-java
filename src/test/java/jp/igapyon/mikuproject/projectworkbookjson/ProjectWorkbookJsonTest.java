/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuproject.projectworkbookjson;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
import jp.igapyon.mikuproject.model.AssignmentModel;
import jp.igapyon.mikuproject.model.TaskModel;
import jp.igapyon.mikuproject.coreapi.CoreApiAiJsonUtil;
import jp.igapyon.mikuproject.msprojectxml.MsProjectCalendar;
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
        assertEquals("miku-project開発", document.sheets.get("Project").get(0).get("Value"));
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
        Map<String, Object> assignmentRow = findRowByUid(document.sheets.get("Assignments"), "1");

        projectRow.put("Value", "JSON import project");
        taskRow.put("Name", "JSON import task");
        taskRow.put("Start", "2026-03-16 10:00:00");
        taskRow.put("Duration", "PT24H0M0S");
        taskRow.put("Milestone", "○");
        taskRow.put("Summary", "○");
        taskRow.put("Critical", "ー");
        taskRow.put("Type", Integer.valueOf(2));
        taskRow.put("Priority", Integer.valueOf(500));
        taskRow.put("CalendarUID", "2");
        taskRow.put("ConstraintType", Integer.valueOf(4));
        taskRow.put("ConstraintDate", "2026-03-17 09:00:00");
        taskRow.put("Deadline", "2026-03-18 18:00:00");
        taskRow.put("Predecessors", "2");
        taskRow.put("OutlineNumber", "999");
        resourceRow.put("Name", "Miku Updated");
        resourceRow.put("Type", Integer.valueOf(1));
        resourceRow.put("Initials", "MU");
        resourceRow.put("Group", "Dev");
        resourceRow.put("MaxUnits", Integer.valueOf(1));
        resourceRow.put("CalendarUID", "1");
        resourceRow.put("StandardRate", "1200/h");
        resourceRow.put("OvertimeRate", "1800/h");
        resourceRow.put("CostPerUse", Double.valueOf(500));
        resourceRow.put("Work", "PT8H0M0S");
        resourceRow.put("ActualWork", "PT4H0M0S");
        resourceRow.put("RemainingWork", "PT4H0M0S");
        resourceRow.put("Cost", Double.valueOf(1000));
        resourceRow.put("ActualCost", Double.valueOf(500));
        resourceRow.put("RemainingCost", Double.valueOf(500));
        resourceRow.put("PercentWorkComplete", Integer.valueOf(50));
        resourceRow.put("WorkGroup", Integer.valueOf(0));
        resourceRow.put("StandardRateFormat", Integer.valueOf(2));
        resourceRow.put("OvertimeRateFormat", Integer.valueOf(2));
        assignmentRow.put("Start", "2026-03-16 09:00:00");
        assignmentRow.put("Finish", "2026-03-16 18:00:00");
        assignmentRow.put("StartVariance", "PT0H30M0S");
        assignmentRow.put("FinishVariance", "PT1H0M0S");
        assignmentRow.put("Delay", "PT0H15M0S");
        assignmentRow.put("Milestone", "○");
        assignmentRow.put("WorkContour", Integer.valueOf(1));
        assignmentRow.put("Cost", Double.valueOf(1000));
        assignmentRow.put("ActualWork", "PT4H0M0S");
        assignmentRow.put("RemainingWork", "PT4H0M0S");
        assignmentRow.put("ActualCost", Double.valueOf(300));
        assignmentRow.put("RemainingCost", Double.valueOf(200));
        assignmentRow.put("OvertimeWork", "PT1H0M0S");
        assignmentRow.put("ActualOvertimeWork", "PT0H30M0S");

        ProjectWorkbookJsonImport.ImportResult result = workbookJson.importProjectWorkbookJson(toDocumentLike(document), baseModel);
        AssignmentModel importedAssignment = findAssignment(result.model, "1");

        assertEquals("JSON import project", result.model.project.name);
        assertEquals("JSON import task", result.model.tasks.get(1).name);
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
        assertEquals(1, result.model.tasks.get(1).predecessors.size());
        assertEquals("2", result.model.tasks.get(1).predecessors.get(0).predecessorUid);
        assertEquals("Miku Updated", result.model.resources.get(0).name);
        assertEquals(Integer.valueOf(1), result.model.resources.get(0).type);
        assertEquals("MU", result.model.resources.get(0).initials);
        assertEquals("Dev", result.model.resources.get(0).group);
        assertEquals(Double.valueOf(1), result.model.resources.get(0).maxUnits);
        assertEquals("1", result.model.resources.get(0).calendarUID);
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
        assertEquals("2026-03-16T09:00:00", importedAssignment.start);
        assertEquals("2026-03-16T18:00:00", importedAssignment.finish);
        assertEquals("PT0H30M0S", importedAssignment.startVariance);
        assertEquals("PT1H0M0S", importedAssignment.finishVariance);
        assertEquals("PT0H15M0S", importedAssignment.delay);
        assertEquals(Boolean.TRUE, importedAssignment.milestone);
        assertEquals(Integer.valueOf(1), importedAssignment.workContour);
        assertEquals(Double.valueOf(1000), importedAssignment.cost);
        assertEquals("PT4H0M0S", importedAssignment.actualWork);
        assertEquals("PT4H0M0S", importedAssignment.remainingWork);
        assertEquals(Double.valueOf(300), importedAssignment.actualCost);
        assertEquals(Double.valueOf(200), importedAssignment.remainingCost);
        assertEquals("PT1H0M0S", importedAssignment.overtimeWork);
        assertEquals("PT0H30M0S", importedAssignment.actualOvertimeWork);
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

    @Test
    public void importsUpstreamWorkbookJsonFixtureIntoHierarchyBaseModel() throws IOException {
        ProjectWorkbookJson workbookJson = new ProjectWorkbookJson();
        ProjectModel baseModel = new MsProjectXml().importFromXml(readVendorTestdata("hierarchy.xml"));
        Map<String, Object> document = parseJsonDocument(readVendorTestdata("workbook-import-sample.json"));

        ProjectWorkbookJsonImport.ImportResult result = workbookJson.importProjectWorkbookJson(document, baseModel);
        TaskModel importedTask = findTask(result.model, "3");

        assertNotNull(importedTask);
        assertEquals("初期実装 Imported From JSON File", importedTask.name);
        assertEquals(Integer.valueOf(55), importedTask.percentComplete);
        assertEquals("Child A", findTask(result.model, "2").name);
        assertTrue(hasChangeField(result.changes, "Name"));
        assertTrue(hasChangeField(result.changes, "PercentComplete"));
    }

    @Test
    public void importsCalendarBooleanColumnsThroughWorkbookJson() {
        ProjectWorkbookJson workbookJson = new ProjectWorkbookJson();
        ProjectModel baseModel = buildCalendarWorkbookBaseModel();
        WorkbookJsonDocument document = workbookJson.exportProjectWorkbookJson(baseModel);
        String calendarUid = baseModel.calendars.get(0).uid;
        Map<String, Object> calendarRow = findRowByUid(document.sheets.get("Calendars"), calendarUid);
        Map<String, Object> nonWorkingDayRow = findNonWorkingDayRow(document.sheets.get("NonWorkingDays"), calendarUid, "0");

        calendarRow.put("IsBaseCalendar", "ー");
        nonWorkingDayRow.put("DayWorking", "○");

        ProjectWorkbookJsonImport.ImportResult result = workbookJson.importProjectWorkbookJson(toDocumentLike(document), baseModel);

        assertEquals(Boolean.FALSE, Boolean.valueOf(result.model.calendars.get(0).isBaseCalendar));
        assertEquals(Boolean.TRUE, result.model.calendars.get(0).exceptions.get(0).dayWorking);
        assertTrue(hasChangeField(result.changes, "IsBaseCalendar"));
    }

    private ProjectModel buildCalendarWorkbookBaseModel() {
        ProjectModel model = new ProjectModel();
        model.project.startDate = "2026-05-01T09:00:00";
        model.project.finishDate = "2026-05-05T18:00:00";
        model.project.defaultStartTime = "09:00:00";
        model.project.defaultFinishTime = "18:00:00";
        return new MsProjectCalendar().ensureDefaultProjectCalendar(model);
    }

    private TaskModel findTask(ProjectModel model, String uid) {
        for (TaskModel task : model.tasks) {
            if (uid.equals(task.uid)) {
                return task;
            }
        }
        return null;
    }

    private AssignmentModel findAssignment(ProjectModel model, String uid) {
        for (AssignmentModel assignment : model.assignments) {
            if (uid.equals(assignment.uid)) {
                return assignment;
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

    private Map<String, Object> findNonWorkingDayRow(List<Map<String, Object>> rows, String calendarUid, String index) {
        for (Map<String, Object> row : rows) {
            if (calendarUid.equals(String.valueOf(row.get("CalendarUID"))) && index.equals(String.valueOf(row.get("Index")))) {
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

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseJsonDocument(String jsonText) {
        return (Map<String, Object>) new CoreApiAiJsonUtil().parseJsonText(jsonText);
    }
}
