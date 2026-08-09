/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuproject.projectpatchjson;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
import jp.igapyon.mikuproject.msprojectxml.MsProjectXml;

public class ProjectPatchJsonTest {
    @Test
    public void validatesPatchDocuments() {
        ProjectPatchJson patch = new ProjectPatchJson();
        Map<String, Object> valid = new LinkedHashMap<String, Object>();
        valid.put("operations", Arrays.asList(mapOf("op", "update_project", "fields", mapOf("name", "Validated Patch"))));

        assertEquals(1, patch.validatePatchDocument(valid).document.operations.size());
        assertThrows(IllegalArgumentException.class, new org.junit.jupiter.api.function.Executable() {
            @Override
            public void execute() {
                patch.validatePatchDocument(null);
            }
        });
        assertThrows(IllegalArgumentException.class, new org.junit.jupiter.api.function.Executable() {
            @Override
            public void execute() {
                patch.validatePatchDocument(new LinkedHashMap<String, Object>());
            }
        });
    }

    @Test
    public void ignoresUnknownPatchOperationWithGenericWarning() throws IOException {
        ProjectPatchJson patch = new ProjectPatchJson();
        ProjectModel dependencyModel = new MsProjectXml().importFromXml(readVendorTestdata("dependency.xml"));

        ProjectPatchJsonCore.ImportResult result = patch.importProjectPatchJson(
                mapOf("operations", Arrays.asList(mapOf("op", "unknown_future_op", "uid", "x1"))),
                dependencyModel);

        assertEquals(0, result.changes.size());
        assertTrue(joinWarnings(result).contains("未対応の op は無視します: operations[0].op = unknown_future_op"));
    }

    @Test
    public void rejectsInvalidPatchJsonOperationsWithoutMainUi() throws IOException {
        ProjectPatchJson patch = new ProjectPatchJson();
        ProjectModel dependencyModel = new MsProjectXml().importFromXml(readVendorTestdata("dependency.xml"));

        ProjectPatchJsonCore.ImportResult projectResult = patch.importProjectPatchJson(mapOf("operations",
                Arrays.asList(mapOf("op", "update_project", "fields",
                        mapOf("name", "", "start_date", "2026-03-20", "finish_date", "2026-03-19", "current_date",
                                "bad-date", "calendar_uid", "999", "minutes_per_day", Integer.valueOf(0),
                                "schedule_from_start", "yes")))),
                dependencyModel);

        ProjectPatchJsonCore.ImportResult assignmentResult = patch.importProjectPatchJson(mapOf("operations",
                Arrays.asList(
                        mapOf("op", "update_assignment", "uid", "1", "fields",
                                mapOf("start", "2026-03-20", "finish", "2026-03-19", "units", Integer.valueOf(-1), "work",
                                        "", "percent_work_complete", Integer.valueOf(120))),
                        mapOf("op", "add_assignment", "uid", "3", "task_uid", "999", "resource_uid", "1", "units",
                                Integer.valueOf(-1)))),
                dependencyModel);

        String projectWarnings = joinWarnings(projectResult);
        String assignmentWarnings = joinWarnings(assignmentResult);
        assertEquals(0, projectResult.changes.size());
        assertTrue(projectWarnings.contains("update_project.name は空でない文字列が必要です"));
        assertTrue(projectWarnings.contains("update_project.start_date が finish_date より後です"));
        assertTrue(projectWarnings.contains("update_project.current_date の日付形式が解釈できません"));
        assertTrue(projectWarnings.contains("update_project.calendar_uid が既存 calendar を指していません"));
        assertTrue(projectWarnings.contains("update_project.minutes_per_day は 0 より大きい数値が必要です"));
        assertTrue(projectWarnings.contains("update_project.schedule_from_start は boolean が必要です"));
        assertEquals("Dependency Project", projectResult.model.project.name);

        assertEquals(0, assignmentResult.changes.size());
        assertTrue(assignmentWarnings.contains("update_assignment.start が finish より後です"));
        assertTrue(assignmentWarnings.contains("update_assignment.units は 0 以上の数値が必要です"));
        assertTrue(assignmentWarnings.contains("update_assignment.work は空でない文字列が必要です"));
        assertTrue(assignmentWarnings.contains("update_assignment.percent_work_complete は 0 以上 100 以下の数値が必要です"));
        assertTrue(assignmentWarnings.contains("add_assignment.task_uid が既存 task を指していません"));
    }

    @Test
    public void reportsPatchJsonWarningDetailsWithoutMainUi() throws IOException {
        ProjectPatchJson patch = new ProjectPatchJson();
        MsProjectXml xml = new MsProjectXml();
        ProjectModel hierarchyModel = xml.importFromXml(readVendorTestdata("hierarchy.xml"));
        ProjectPatchJsonCore.ImportResult linked = patch.importProjectPatchJson(
                mapOf("operations", Arrays.asList(mapOf("op", "link_tasks", "from_uid", "2", "to_uid", "3", "type", "SS",
                        "lag_hours", Integer.valueOf(4)))),
                hierarchyModel);
        ProjectPatchJsonCore.ImportResult duplicate = patch.importProjectPatchJson(
                mapOf("operations", Arrays.asList(mapOf("op", "link_tasks", "from_uid", "2", "to_uid", "3", "type", "SS",
                        "lag_hours", Integer.valueOf(4)))),
                linked.model);
        ProjectPatchJsonCore.ImportResult missingLag = patch.importProjectPatchJson(
                mapOf("operations", Arrays.asList(mapOf("op", "unlink_tasks", "from_uid", "2", "to_uid", "3", "type", "SS",
                        "lag_hours", Integer.valueOf(8)))),
                linked.model);
        ProjectPatchJsonCore.ImportResult modelResult = patch.importProjectPatchJson(
                mapOf("operations",
                        Arrays.asList(
                                mapOf("op", "update_resource", "uid", "1", "fields",
                                        mapOf("name", "", "calendar_uid", "999", "max_units", Integer.valueOf(-1),
                                                "cost_per_use", Integer.valueOf(-1), "percent_work_complete",
                                                Integer.valueOf(120))),
                                mapOf("op", "update_calendar", "uid", "1", "fields",
                                        mapOf("name", "", "is_base_calendar", "yes", "base_calendar_uid", "1")),
                                mapOf("op", "add_calendar", "uid", "2", "name", "", "is_base_calendar", "yes",
                                        "base_calendar_uid", "999"),
                                mapOf("op", "add_resource", "uid", "2", "name", "", "calendar_uid", "999", "max_units",
                                        Integer.valueOf(-1), "cost_per_use", Integer.valueOf(-1), "percent_work_complete",
                                        Integer.valueOf(120)),
                                mapOf("op", "delete_assignment", "uid", "404"),
                                mapOf("op", "delete_calendar", "uid", "1"),
                                mapOf("op", "delete_resource", "uid", "1"))),
                xml.importFromXml(readVendorTestdata("dependency.xml")));
        ProjectPatchJsonCore.ImportResult deleteAssignment = patch.importProjectPatchJson(
                mapOf("operations", Arrays.asList(mapOf("op", "delete_assignment", "uid", "1"))),
                xml.importFromXml(readVendorTestdata("dependency.xml")));

        assertTrue(joinWarnings(duplicate).contains("link_tasks の依存関係は既に存在します: 2 -> 3 (SS, lag=PT4H0M0S)"));
        assertTrue(joinWarnings(missingLag).contains("unlink_tasks の対象依存関係が見つかりません: 2 -> 3 (SS, lag=PT8H0M0S)"));

        String modelWarnings = joinWarnings(modelResult);
        assertTrue(modelWarnings.contains("update_resource.name は空でない文字列が必要です"));
        assertTrue(modelWarnings.contains("update_resource.calendar_uid が既存 calendar を指していません"));
        assertTrue(modelWarnings.contains("update_resource.max_units は 0 以上の数値が必要です"));
        assertTrue(modelWarnings.contains("update_resource.cost_per_use は 0 以上の数値が必要です"));
        assertTrue(modelWarnings.contains("update_resource.percent_work_complete は 0 以上 100 以下の数値が必要です"));
        assertTrue(modelWarnings.contains("update_calendar.name は空でない文字列が必要です"));
        assertTrue(modelWarnings.contains("update_calendar.is_base_calendar は boolean が必要です"));
        assertTrue(modelWarnings.contains("update_calendar.base_calendar_uid は自身を指せません"));
        assertTrue(modelWarnings.contains("add_calendar.name は空でない文字列が必要です"));
        assertTrue(modelWarnings.contains("add_resource.name は空でない文字列が必要です"));
        assertTrue(modelWarnings.contains("delete_assignment の uid が既存 assignment を指していません: 404"));
        assertTrue(modelWarnings.contains(
                "delete_calendar first cut では参照が残っている calendar は削除できません: 1 (project=1)"));
        assertTrue(modelWarnings.contains(
                "delete_resource first cut では assignment がある resource は削除できません: 1 (assignments=1)"));

        assertEquals(2, deleteAssignment.changes.size());
        assertEquals(0, deleteAssignment.warnings.size());
        assertEquals(0, deleteAssignment.model.assignments.size());
        assertEquals("taskUid", deleteAssignment.changes.get(0).field);
        assertEquals("2", deleteAssignment.changes.get(0).before);
        assertEquals("(deleted)", deleteAssignment.changes.get(0).after);
        assertEquals("resourceUid", deleteAssignment.changes.get(1).field);
        assertEquals("1", deleteAssignment.changes.get(1).before);
        assertEquals("(deleted)", deleteAssignment.changes.get(1).after);
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
