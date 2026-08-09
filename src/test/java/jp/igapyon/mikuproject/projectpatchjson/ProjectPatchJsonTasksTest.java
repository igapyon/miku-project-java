/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuproject.projectpatchjson;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import jp.igapyon.mikuproject.model.ProjectModel;
import jp.igapyon.mikuproject.model.TaskModel;
import jp.igapyon.mikuproject.msprojectxml.MsProjectXml;

public class ProjectPatchJsonTasksTest {
    @Test
    public void importsAddTaskMoveTaskAndDeleteTask() throws IOException {
        ProjectPatchJson patch = new ProjectPatchJson();
        MsProjectXml xml = new MsProjectXml();
        ProjectModel hierarchyModel = xml.importFromXml(readVendorTestdata("hierarchy.xml"));

        ProjectPatchJsonCore.ImportResult added = patch.importProjectPatchJson(mapOf("operations",
                Arrays.asList(mapOf("op", "add_task", "uid", "4", "name", "Child C", "new_parent_uid", "1", "new_index",
                        Integer.valueOf(1), "planned_start", "2026-03-17", "planned_finish", "2026-03-17"))), hierarchyModel);

        assertEquals(5, added.changes.size());
        TaskModel addedTask = findTask(added.model, "4");
        assertNotNull(addedTask);
        assertEquals(Integer.valueOf(2), addedTask.outlineLevel);
        assertEquals("1.2", addedTask.outlineNumber);
        assertEquals("2026-03-17T09:00:00", addedTask.start);
        assertEquals("2026-03-17T18:00:00", addedTask.finish);

        ProjectPatchJsonCore.ImportResult moved = patch.importProjectPatchJson(
                mapOf("operations", Arrays.asList(mapOf("op", "move_task", "uid", "3", "new_parent_uid", null, "new_index",
                        Integer.valueOf(1)))),
                hierarchyModel);

        assertEquals(1, moved.changes.size());
        TaskModel movedTask = findTask(moved.model, "3");
        assertNotNull(movedTask);
        assertEquals(Integer.valueOf(1), movedTask.outlineLevel);
        assertEquals("2", movedTask.outlineNumber);

        ProjectPatchJsonCore.ImportResult deleted = patch.importProjectPatchJson(
                mapOf("operations", Arrays.asList(mapOf("op", "delete_task", "uid", "3"))), hierarchyModel);

        assertEquals(3, deleted.changes.size());
        assertEquals(null, findTask(deleted.model, "3"));
        assertEquals(2, deleted.model.tasks.size());
    }

    @Test
    public void rejectsBrokenAddTaskAndNormalizesMilestoneAddTask() throws IOException {
        ProjectPatchJson patch = new ProjectPatchJson();
        ProjectModel hierarchyModel = new MsProjectXml().importFromXml(readVendorTestdata("hierarchy.xml"));

        ProjectPatchJsonCore.ImportResult invalidDate = patch.importProjectPatchJson(mapOf("operations",
                Arrays.asList(mapOf("op", "add_task", "uid", "4", "name", "Broken Task", "new_parent_uid", "1", "new_index",
                        Integer.valueOf(1), "planned_start", "2026-03-18", "planned_finish", "2026-03-17"))), hierarchyModel);
        ProjectPatchJsonCore.ImportResult invalidSummaryGate = patch.importProjectPatchJson(mapOf("operations",
                Arrays.asList(mapOf("op", "add_task", "uid", "4", "name", "Broken Summary Gate", "is_summary",
                        Boolean.TRUE, "is_milestone", Boolean.TRUE, "new_parent_uid", null, "new_index",
                        Integer.valueOf(1)))), hierarchyModel);
        ProjectPatchJsonCore.ImportResult milestone = patch.importProjectPatchJson(mapOf("operations",
                Arrays.asList(mapOf("op", "add_task", "uid", "4", "name", "Gate", "new_parent_uid", "1", "new_index",
                        Integer.valueOf(1), "is_milestone", Boolean.TRUE, "planned_start", "2026-03-17", "planned_finish",
                        "2026-03-18", "planned_duration_hours", Integer.valueOf(8), "extra_key", "ignored"))), hierarchyModel);

        assertEquals(0, invalidDate.changes.size());
        assertTrue(joinWarnings(invalidDate).contains("add_task.planned_start が planned_finish より後です"));

        assertEquals(0, invalidSummaryGate.changes.size());
        assertTrue(joinWarnings(invalidSummaryGate).contains("add_task では is_summary と is_milestone を同時に true にできません"));

        TaskModel milestoneTask = findTask(milestone.model, "4");
        assertNotNull(milestoneTask);
        assertTrue(milestoneTask.milestone);
        assertEquals("2026-03-17", milestoneTask.finish);
        assertEquals("PT0H0M0S", milestoneTask.duration);
        assertTrue(joinWarnings(milestone).contains("add_task.is_milestone=true のため planned_finish は planned_start に揃えます"));
        assertTrue(joinWarnings(milestone).contains("add_task.is_milestone=true のため planned_duration は 0 に揃えます"));
        assertTrue(joinWarnings(milestone).contains("add_task の未対応 key は無視します: extra_key"));
    }

    @Test
    public void rejectsDeleteTaskWhenReferencesRemain() throws IOException {
        ProjectPatchJson patch = new ProjectPatchJson();
        MsProjectXml xml = new MsProjectXml();
        ProjectModel hierarchyModel = xml.importFromXml(readVendorTestdata("hierarchy.xml"));

        ProjectPatchJsonCore.ImportResult summaryDelete = patch.importProjectPatchJson(
                mapOf("operations", Arrays.asList(mapOf("op", "delete_task", "uid", "1"))), hierarchyModel);
        ProjectPatchJsonCore.ImportResult assignmentDelete = patch.importProjectPatchJson(
                mapOf("operations", Arrays.asList(mapOf("op", "delete_task", "uid", "3"))),
                xml.importFromXml(hierarchyXmlWithAssignment()));
        ProjectPatchJsonCore.ImportResult successorDelete = patch.importProjectPatchJson(
                mapOf("operations", Arrays.asList(mapOf("op", "delete_task", "uid", "2"))),
                xml.importFromXml(hierarchyXmlWithSuccessor()));

        assertTrue(joinWarnings(summaryDelete).contains("delete_task first cut では summary task や子を持つ task は削除できません"));
        assertTrue(joinWarnings(summaryDelete).contains("children=2"));
        assertTrue(joinWarnings(assignmentDelete).contains("delete_task first cut では assignment がある task は削除できません"));
        assertTrue(joinWarnings(assignmentDelete).contains("assignments=1"));
        assertTrue(joinWarnings(successorDelete).contains("delete_task first cut では後続依存がある task は削除できません"));
        assertTrue(joinWarnings(successorDelete).contains("successors=3"));
    }

    @Test
    public void ignoresNoOpMoveTask() throws IOException {
        ProjectPatchJson patch = new ProjectPatchJson();
        ProjectModel hierarchyModel = new MsProjectXml().importFromXml(readVendorTestdata("hierarchy.xml"));

        ProjectPatchJsonCore.ImportResult result = patch.importProjectPatchJson(mapOf("operations",
                Arrays.asList(mapOf("op", "move_task", "uid", "3", "new_parent_uid", "1", "new_index",
                        Integer.valueOf(1)))), hierarchyModel);

        assertEquals(0, result.changes.size());
        assertTrue(joinWarnings(result).contains("move_task は結果が変わらないため無視します"));
        assertTrue(joinWarnings(result).contains("parent=1 index=1"));
        assertFalse(joinWarnings(result).isEmpty());
    }

    private TaskModel findTask(ProjectModel model, String uid) {
        for (TaskModel task : model.tasks) {
            if (uid.equals(task.uid)) {
                return task;
            }
        }
        return null;
    }

    private String hierarchyXmlWithAssignment() throws IOException {
        return readVendorTestdata("hierarchy.xml").replace("<Resources />",
                "<Resources>\n  <Resource>\n    <UID>1</UID>\n    <ID>1</ID>\n    <Name>Owner</Name>\n  </Resource>\n</Resources>")
                .replace("<Assignments />",
                        "<Assignments>\n  <Assignment>\n    <UID>1</UID>\n    <TaskUID>3</TaskUID>\n    <ResourceUID>1</ResourceUID>\n  </Assignment>\n</Assignments>");
    }

    private String hierarchyXmlWithSuccessor() throws IOException {
        return readVendorTestdata("hierarchy.xml").replace("</Notes>\n    </Task>",
                "</Notes>\n      <PredecessorLink>\n        <PredecessorUID>2</PredecessorUID>\n        <Type>1</Type>\n      </PredecessorLink>\n    </Task>");
    }

    private String joinWarnings(ProjectPatchJsonCore.ImportResult result) {
        StringBuilder builder = new StringBuilder();
        for (PatchWarning warning : result.warnings) {
            if (builder.length() > 0) {
                builder.append('\n');
            }
            builder.append(warning.message);
        }
        return builder.toString();
    }

    private String readVendorTestdata(String fileName) throws IOException {
        byte[] bytes = Files.readAllBytes(Paths.get("vendor", "miku-project", "testdata", fileName));
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
