/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuproject.msprojectxml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import jp.igapyon.mikuproject.model.ProjectModel;

public class MsProjectAiViewsTest {
    @Test
    public void exportsProjectOverviewAndDefaultPhaseDetailViewsFromHierarchy() throws IOException {
        MsProjectXml xml = new MsProjectXml();
        ProjectModel model = xml.importFromXml(readVendorTestdata("hierarchy.xml"));

        Map<String, Object> overview = xml.exportProjectOverviewView(model);
        Map<String, Object> phaseDetail = xml.exportPhaseDetailView(model);

        assertEquals("project_overview_view", overview.get("view_type"));
        assertEquals("Hierarchy Project", map(overview.get("project")).get("name"));
        assertEquals(Integer.valueOf(3), map(overview.get("summary")).get("task_count"));
        assertEquals(Integer.valueOf(1), map(overview.get("summary")).get("summary_task_count"));
        assertEquals(1, list(overview.get("phases")).size());
        assertEquals("1", map(list(overview.get("phases")).get(0)).get("uid"));
        assertEquals("Summary", map(list(overview.get("phases")).get(0)).get("name"));
        assertTrue(list(map(overview.get("rules")).get("allow_patch_ops")).contains("add_task"));

        assertEquals("phase_detail_view", phaseDetail.get("view_type"));
        assertEquals("1", map(phaseDetail.get("phase")).get("uid"));
        assertEquals("full", map(phaseDetail.get("scope")).get("mode"));
        assertNull(map(phaseDetail.get("scope")).get("root_uid"));
        assertNull(map(phaseDetail.get("scope")).get("max_depth"));
        assertEquals(Arrays.asList("2", "3"), extractTaskUids(list(phaseDetail.get("tasks"))));
        assertTrue(extractTaskNames(list(phaseDetail.get("tasks"))).contains("Child B"));
        assertTrue(list(map(phaseDetail.get("rules")).get("allow_patch_ops")).contains("link_tasks"));
    }

    @Test
    public void exportsTaskEditViewWithPredecessorsSuccessorsAndAssignments() throws IOException {
        MsProjectXml xml = new MsProjectXml();
        ProjectModel model = xml.importFromXml(readVendorTestdata("dependency.xml"));

        Map<String, Object> taskEdit = xml.exportTaskEditView(model, "2");

        assertEquals("task_edit_view", taskEdit.get("view_type"));
        assertEquals("Dependency Project", map(taskEdit.get("project")).get("name"));
        assertNull(taskEdit.get("phase"));
        assertEquals("2", map(taskEdit.get("target_task")).get("uid"));
        assertEquals("Execute", map(taskEdit.get("target_task")).get("name"));
        assertNull(taskEdit.get("parent_task"));
        assertEquals(1, list(taskEdit.get("predecessors")).size());
        assertEquals("1", map(list(taskEdit.get("predecessors")).get(0)).get("task_uid"));
        assertEquals("Prepare", map(list(taskEdit.get("predecessors")).get(0)).get("name"));
        assertEquals("FS", map(list(taskEdit.get("predecessors")).get(0)).get("type"));
        assertEquals("PT0H0M0S", map(list(taskEdit.get("predecessors")).get(0)).get("lag"));
        assertEquals(Double.valueOf(0), map(list(taskEdit.get("predecessors")).get(0)).get("lag_hours"));
        assertTrue(list(taskEdit.get("successors")).isEmpty());
        assertEquals(1, list(taskEdit.get("assignments")).size());
        assertEquals("1", map(list(taskEdit.get("assignments")).get(0)).get("uid"));
        assertEquals("1", map(list(taskEdit.get("assignments")).get(0)).get("resource_uid"));
        assertEquals("Miku", map(list(taskEdit.get("assignments")).get(0)).get("resource_name"));
        assertTrue(list(map(taskEdit.get("rules")).get("allow_patch_ops")).contains("update_assignment"));
        assertTrue(list(map(taskEdit.get("rules")).get("allowed_edit_fields")).contains("planned_duration_hours"));
    }

    @Test
    public void exportsScopedPhaseDetailAndRejectsInvalidRootUid() throws IOException {
        MsProjectXml xml = new MsProjectXml();
        ProjectModel model = xml.importFromXml(readVendorTestdata("hierarchy.xml"));

        Map<String, Object> phaseDetail = xml.exportPhaseDetailView(model, "1", "scoped", "2", Integer.valueOf(1));

        assertEquals("phase_detail_view", phaseDetail.get("view_type"));
        assertEquals("scoped", map(phaseDetail.get("scope")).get("mode"));
        assertEquals("2", map(phaseDetail.get("scope")).get("root_uid"));
        assertEquals(Integer.valueOf(1), map(phaseDetail.get("scope")).get("max_depth"));
        assertEquals(Arrays.asList("2"), extractTaskUids(list(phaseDetail.get("tasks"))));
        assertEquals("Child A", map(list(phaseDetail.get("tasks")).get(0)).get("name"));
        assertTrue(list(phaseDetail.get("dependency_summary")).isEmpty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                new org.junit.jupiter.api.function.Executable() {
                    @Override
                    public void execute() {
                        xml.exportPhaseDetailView(model, "1", "scoped", "999", null);
                    }
                });
        assertTrue(exception.getMessage().contains("root_uid"));
    }

    @Test
    public void buildsProjectDraftRequestAndImportsPredecessorMappingFromProjectDraftView() {
        MsProjectXml xml = new MsProjectXml();

        Map<String, Object> request = xml.buildProjectDraftRequest("Draft Request Project", "2026-04-01", "Goal text",
                Integer.valueOf(3), Arrays.asList("Plan", "Build"), Arrays.asList("Kickoff"));

        Map<String, Object> draft = new LinkedHashMap<String, Object>();
        draft.put("view_type", "project_draft_view");
        Map<String, Object> project = new LinkedHashMap<String, Object>();
        project.put("name", "Draft Import Project");
        project.put("planned_start", "2026-04-01");
        draft.put("project", project);
        draft.put("tasks", Arrays.asList(task("draft-phase", "Phase", null, Integer.valueOf(0), Boolean.TRUE, null, null,
                "2026-04-01", "2026-04-03", null), task("draft-task-1", "Task A", "draft-phase", Integer.valueOf(0), null, null,
                        null, "2026-04-01", "2026-04-01", null), task("draft-task-2", "Task B", "draft-phase", Integer.valueOf(1),
                                null, null, null, "2026-04-02", "2026-04-03", Arrays.asList("draft-task-1"))));
        draft.put("resources", Arrays.asList(resource("draft-res-1", "Miku")));
        draft.put("assignments", Arrays.asList(assignment("draft-asg-1", "draft-task-2", "draft-res-1", Integer.valueOf(1))));

        ProjectModel model = xml.importProjectDraftView(draft);

        assertEquals("project_draft_request", request.get("view_type"));
        assertEquals("Draft Request Project", map(request.get("project")).get("name"));
        assertEquals("2026-04-01", map(request.get("project")).get("planned_start"));
        assertEquals("Goal text", map(request.get("requirements")).get("goal"));
        assertEquals(Integer.valueOf(3), map(request.get("requirements")).get("team_count"));
        assertEquals(Arrays.asList("Plan", "Build"), list(map(request.get("requirements")).get("must_have_phases")));
        assertEquals(Arrays.asList("Kickoff"), list(map(request.get("requirements")).get("must_have_milestones")));

        assertEquals("Draft Import Project", model.project.name);
        assertEquals(3, model.tasks.size());
        assertTrue(model.tasks.get(0).summary);
        assertEquals("Task A", model.tasks.get(1).name);
        assertEquals("Task B", model.tasks.get(2).name);
        assertEquals(1, model.tasks.get(2).predecessors.size());
        assertEquals("2", model.tasks.get(2).predecessors.get(0).predecessorUid);
        assertEquals("1", model.resources.get(0).uid);
        assertEquals("3", model.assignments.get(0).taskUid);
        assertEquals("1", model.assignments.get(0).resourceUid);
    }

    @Test
    public void importsProjectDraftViewWithoutTaskDatesUsingProjectStartFallback() {
        MsProjectXml xml = new MsProjectXml();

        Map<String, Object> draft = new LinkedHashMap<String, Object>();
        draft.put("view_type", "project_draft_view");
        Map<String, Object> project = new LinkedHashMap<String, Object>();
        project.put("name", "Fallback Draft");
        project.put("planned_start", "2026-04-01");
        draft.put("project", project);
        draft.put("tasks", Arrays.asList(task("draft-task-1", "Task without dates", null, Integer.valueOf(0), null, null, null,
                null, null, null)));

        ProjectModel model = xml.importProjectDraftView(draft);

        assertEquals("2026-04-01", model.project.startDate);
        assertEquals("2026-04-01T18:00:00", model.project.finishDate);
        assertEquals(1, model.tasks.size());
        assertEquals("2026-04-01T09:00:00", model.tasks.get(0).start);
        assertEquals("2026-04-01T18:00:00", model.tasks.get(0).finish);
        assertEquals("PT0H0M0S", model.tasks.get(0).duration);
        assertTrue(model.tasks.get(0).predecessors.isEmpty());
    }

    @Test
    public void rejectsInvalidProjectDraftViewReferences() {
        MsProjectXml xml = new MsProjectXml();

        Map<String, Object> invalidParentDraft = new LinkedHashMap<String, Object>();
        invalidParentDraft.put("view_type", "project_draft_view");
        Map<String, Object> invalidParentProject = new LinkedHashMap<String, Object>();
        invalidParentProject.put("name", "Broken Draft");
        invalidParentDraft.put("project", invalidParentProject);
        invalidParentDraft.put("tasks", Arrays.asList(task("draft-task-1", "Task", "missing-parent", null, null, null, null, null,
                null, null)));

        IllegalArgumentException parentException = assertThrows(IllegalArgumentException.class,
                new org.junit.jupiter.api.function.Executable() {
                    @Override
                    public void execute() {
                        xml.importProjectDraftView(invalidParentDraft);
                    }
                });
        assertTrue(parentException.getMessage().contains("parent_uid"));

        Map<String, Object> invalidResourceDraft = new LinkedHashMap<String, Object>();
        invalidResourceDraft.put("view_type", "project_draft_view");
        Map<String, Object> invalidResourceProject = new LinkedHashMap<String, Object>();
        invalidResourceProject.put("name", "Broken Draft");
        invalidResourceDraft.put("project", invalidResourceProject);
        invalidResourceDraft.put("tasks",
                Arrays.asList(task("draft-task-1", "Task", null, null, null, null, null, null, null, null)));
        invalidResourceDraft.put("resources", Arrays.asList(resource("draft-res-1", "Miku")));
        invalidResourceDraft.put("assignments",
                Arrays.asList(assignment("draft-asg-1", "draft-task-1", "missing-resource", null)));

        IllegalArgumentException resourceException = assertThrows(IllegalArgumentException.class,
                new org.junit.jupiter.api.function.Executable() {
                    @Override
                    public void execute() {
                        xml.importProjectDraftView(invalidResourceDraft);
                    }
                });
        assertTrue(resourceException.getMessage().contains("resource_uid"));
    }

    private Map<String, Object> task(String uid, String name, String parentUid, Integer position, Boolean isSummary,
            Boolean isMilestone, Integer percentComplete, String plannedStart, String plannedFinish, List<String> predecessors) {
        Map<String, Object> task = new LinkedHashMap<String, Object>();
        task.put("uid", uid);
        task.put("name", name);
        task.put("parent_uid", parentUid);
        task.put("position", position);
        task.put("is_summary", isSummary);
        task.put("is_milestone", isMilestone);
        task.put("percent_complete", percentComplete);
        task.put("planned_start", plannedStart);
        task.put("planned_finish", plannedFinish);
        task.put("predecessors", predecessors);
        return task;
    }

    private Map<String, Object> resource(String uid, String name) {
        Map<String, Object> resource = new LinkedHashMap<String, Object>();
        resource.put("uid", uid);
        resource.put("name", name);
        return resource;
    }

    private Map<String, Object> assignment(String uid, String taskUid, String resourceUid, Integer units) {
        Map<String, Object> assignment = new LinkedHashMap<String, Object>();
        assignment.put("uid", uid);
        assignment.put("task_uid", taskUid);
        assignment.put("resource_uid", resourceUid);
        assignment.put("units", units);
        return assignment;
    }

    private List<String> extractTaskUids(List<Object> tasks) {
        List<String> result = new java.util.ArrayList<String>();
        for (Object task : tasks) {
            result.add(String.valueOf(map(task).get("uid")));
        }
        return result;
    }

    private List<String> extractTaskNames(List<Object> tasks) {
        List<String> result = new java.util.ArrayList<String>();
        for (Object task : tasks) {
            result.add(String.valueOf(map(task).get("name")));
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> map(Object value) {
        return (Map<String, Object>) value;
    }

    @SuppressWarnings("unchecked")
    private List<Object> list(Object value) {
        return (List<Object>) value;
    }

    private String readVendorTestdata(String fileName) throws IOException {
        byte[] bytes = Files.readAllBytes(Paths.get("vendor", "mikuproject", "testdata", fileName));
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
