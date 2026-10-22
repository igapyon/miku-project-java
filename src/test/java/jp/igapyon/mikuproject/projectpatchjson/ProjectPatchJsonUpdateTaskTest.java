/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuproject.projectpatchjson;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import jp.igapyon.mikuproject.model.ProjectModel;
import jp.igapyon.mikuproject.model.TaskModel;
import jp.igapyon.mikuproject.msprojectxml.MsProjectXml;

public class ProjectPatchJsonUpdateTaskTest {
    @Test
    public void updatesTaskTextCalendarBooleanDateAndDurationWithoutMutatingInputs() throws IOException {
        ProjectModel base = hierarchyModel();
        TaskModel originalTask = findTask(base, "2");
        originalTask.notes = "old note";
        originalTask.critical = Boolean.FALSE;
        originalTask.calendarUID = null;
        base.project.defaultStartTime = "08:30:00";
        base.project.defaultFinishTime = "17:15:00";
        String calendarUid = base.calendars.get(0).uid;

        Map<String, Object> fields = mapOf(
                "name", "  Revised  ",
                "notes", "  retained spacing  ",
                "calendar_uid", " " + calendarUid + " ",
                "critical", Boolean.TRUE,
                "planned_start", "2026-04-01",
                "planned_finish", "2026-04-02T12:00:00",
                "planned_duration_hours", Double.valueOf(1.5d),
                "percent_complete", Double.valueOf(25.5d));
        Map<String, Object> fieldsBefore = new LinkedHashMap<String, Object>(fields);
        Map<String, Object> operation = mapOf("op", "update_task", "uid", "2", "fields", fields);

        ProjectPatchJsonCore.ImportResult result = apply(operation, base);
        TaskModel updated = findTask(result.model, "2");

        assertEquals("Revised", updated.name);
        assertEquals("  retained spacing  ", updated.notes);
        assertEquals(calendarUid, updated.calendarUID);
        assertEquals(Boolean.TRUE, updated.critical);
        assertEquals("2026-04-01T08:30:00", updated.start);
        assertEquals("2026-04-02T12:00:00", updated.finish);
        assertEquals("PT1H30M0S", updated.duration);
        assertEquals(Arrays.asList("name", "notes", "calendarUID", "critical", "planned_start", "planned_finish",
                "planned_duration_hours"), changeFields(result));
        assertEquals(1, result.warnings.size());
        assertTrue(result.warnings.get(0).message.contains("update_task.percent_complete は 0 以上 100 以下の整数が必要です: 2"));
        assertEquals(fieldsBefore, fields);
        assertEquals("Child A", originalTask.name);
        assertEquals("old note", originalTask.notes);
        assertEquals(Boolean.FALSE, originalTask.critical);
        assertNull(originalTask.calendarUID);
    }

    @Test
    public void clearsNotesAndCalendarWithFieldSpecificChanges() throws IOException {
        ProjectModel base = hierarchyModel();
        TaskModel originalTask = findTask(base, "2");
        originalTask.notes = "remove me";
        originalTask.calendarUID = base.calendars.get(0).uid;

        ProjectPatchJsonCore.ImportResult result = apply(mapOf("op", "update_task", "uid", "2", "fields",
                mapOf("notes", "", "calendar_uid", "  ")), base);

        assertNull(findTask(result.model, "2").notes);
        assertNull(findTask(result.model, "2").calendarUID);
        assertEquals(0, result.warnings.size());
        assertEquals("notes", result.changes.get(0).field);
        assertEquals("", result.changes.get(0).after);
        assertEquals("calendarUID", result.changes.get(1).field);
        assertEquals("(cleared)", result.changes.get(1).after);
        assertEquals("remove me", originalTask.notes);
        assertEquals(base.calendars.get(0).uid, originalTask.calendarUID);
    }

    @Test
    public void appliesMilestoneCouplingAndKeepsSummaryRestriction() throws IOException {
        ProjectModel base = hierarchyModel();
        ProjectPatchJson patch = new ProjectPatchJson();
        Map<String, Object> taskFields = mapOf("is_milestone", Boolean.TRUE, "planned_start", "2026-04-01",
                "planned_duration_hours", Double.valueOf(2d));
        ProjectPatchJsonCore.ImportResult milestone = patch.importProjectPatchJson(
                mapOf("operations", Arrays.asList(mapOf("op", "update_task", "uid", "3", "fields", taskFields))), base);
        TaskModel updated = findTask(milestone.model, "3");

        assertTrue(updated.milestone);
        assertEquals("2026-04-01", updated.start);
        assertEquals("2026-04-01", updated.finish);
        assertEquals("PT0H0M0S", updated.duration);
        assertTrue(joinWarnings(milestone).contains("update_task.is_milestone=true のため planned_finish は planned_start に揃えます: 3"));
        assertTrue(joinWarnings(milestone).contains("update_task.is_milestone=true のため planned_duration は 0 に揃えます: 3"));
        assertEquals("Child B", findTask(base, "3").name);

        ProjectPatchJsonCore.ImportResult summary = patch.importProjectPatchJson(
                mapOf("operations", Arrays.asList(mapOf("op", "update_task", "uid", "1", "fields",
                        mapOf("is_milestone", Boolean.TRUE, "name", "  Revised summary  ")))), base);
        assertFalse(findTask(summary.model, "1").milestone);
        assertEquals("Revised summary", findTask(summary.model, "1").name);
        assertEquals(1, summary.changes.size());
        assertEquals("update_task では summary task を milestone にできません: 1", summary.warnings.get(0).message);
    }

    @Test
    public void reportsInvalidOperationsButContinuesAndResolvesCurrentTaskIndex() throws IOException {
        ProjectModel base = hierarchyModel();
        List<Object> operations = new ArrayList<Object>();
        operations.add(mapOf("op", "update_task", "fields", mapOf("name", "Missing UID")));
        operations.add(mapOf("op", "update_task", "uid", "404", "fields", mapOf("name", "Unknown UID")));
        operations.add(mapOf("op", "update_task", "uid", "2", "fields", Arrays.asList("not", "an", "object")));
        operations.add(mapOf("op", "update_task", "uid", "2", "fields", mapOf()));
        operations.add(mapOf("op", "add_task", "uid", "4", "name", "Added", "new_parent_uid", "1", "new_index",
                Integer.valueOf(2)));
        operations.add(mapOf("op", "update_task", "uid", "4", "fields", mapOf("name", "Updated after add")));
        operations.add(mapOf("op", "update_task", "uid", "3", "fields", mapOf("name", "Moved task")));
        operations.add(mapOf("op", "move_task", "uid", "3", "new_parent_uid", "1", "new_index", Integer.valueOf(2)));
        operations.add(mapOf("op", "update_task", "uid", "3", "fields", mapOf("name", "Updated after move")));

        ProjectPatchJsonCore.ImportResult result = new ProjectPatchJson().importProjectPatchJson(
                mapOf("operations", operations), base);

        assertEquals(4, result.warnings.size());
        assertTrue(joinWarnings(result).contains("update_task の uid がありません: operations[0]"));
        assertTrue(joinWarnings(result).contains("update_task の uid が既存 task を指していません: 404"));
        assertTrue(joinWarnings(result).contains("update_task.fields がオブジェクトではありません: 2"));
        assertTrue(joinWarnings(result).contains("update_task に fields がありません: 2"));
        assertEquals("Updated after add", findTask(result.model, "4").name);
        assertEquals("Updated after move", findTask(result.model, "3").name);
        assertEquals("Child B", findTask(base, "3").name);
    }

    @Test
    public void rejectsWrongTypesAndRecordsJavaDateParserDifference() throws IOException {
        ProjectModel base = hierarchyModel();
        TaskModel task = findTask(base, "2");
        String originalName = task.name;
        String originalStart = task.start;
        ProjectPatchJsonCore.ImportResult result = apply(mapOf("op", "update_task", "uid", "2", "fields",
                mapOf("name", Integer.valueOf(4), "notes", null, "critical", "true", "planned_start",
                        "2026-04-01 09:30:00", "planned_finish", "2026-04-02T12:00:00", "percent_complete",
                        Double.valueOf(50d))), base);

        assertEquals(originalName, findTask(result.model, "2").name);
        assertEquals(originalStart, findTask(result.model, "2").start);
        assertEquals("2026-04-02T12:00:00", findTask(result.model, "2").finish);
        assertEquals(Integer.valueOf(50), findTask(result.model, "2").percentComplete);
        assertEquals(4, result.warnings.size());
        assertTrue(joinWarnings(result).contains("update_task.name は空でない文字列が必要です: 2"));
        assertTrue(joinWarnings(result).contains("update_task.notes は文字列が必要です: 2"));
        assertTrue(joinWarnings(result).contains("update_task.critical は boolean が必要です: 2"));
        assertTrue(joinWarnings(result).contains("update_task.planned_start の日付形式が解釈できません: 2"));
        assertEquals(Arrays.asList("percentComplete", "planned_finish"), changeFields(result));
    }

    private ProjectPatchJsonCore.ImportResult apply(Map<String, Object> operation, ProjectModel base) {
        return new ProjectPatchJson().importProjectPatchJson(mapOf("operations", Arrays.asList(operation)), base);
    }

    private ProjectModel hierarchyModel() throws IOException {
        return new MsProjectXml().importFromXml(readVendorTestdata("hierarchy.xml"));
    }

    private TaskModel findTask(ProjectModel model, String uid) {
        for (TaskModel task : model.tasks) {
            if (uid.equals(task.uid)) return task;
        }
        return null;
    }

    private List<String> changeFields(ProjectPatchJsonCore.ImportResult result) {
        List<String> fields = new ArrayList<String>();
        for (ImportChange change : result.changes) fields.add(change.field);
        return fields;
    }

    private String joinWarnings(ProjectPatchJsonCore.ImportResult result) {
        StringBuilder builder = new StringBuilder();
        for (PatchWarning warning : result.warnings) {
            if (builder.length() > 0) builder.append('\n');
            builder.append(warning.message);
        }
        return builder.toString();
    }

    private String readVendorTestdata(String fileName) throws IOException {
        byte[] bytes = Files.readAllBytes(Paths.get("vendor", "mikuproject", "testdata", fileName));
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private Map<String, Object> mapOf(Object... entries) {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        for (int i = 0; i < entries.length; i += 2) map.put(String.valueOf(entries[i]), entries[i + 1]);
        return map;
    }
}
