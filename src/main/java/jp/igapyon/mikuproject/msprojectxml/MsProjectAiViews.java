/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuproject.msprojectxml;

import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jp.igapyon.mikuproject.model.AssignmentModel;
import jp.igapyon.mikuproject.model.PredecessorModel;
import jp.igapyon.mikuproject.model.ProjectInfo;
import jp.igapyon.mikuproject.model.ProjectModel;
import jp.igapyon.mikuproject.model.ResourceModel;
import jp.igapyon.mikuproject.model.TaskModel;

public class MsProjectAiViews {
    private static final int DEFAULT_PROJECT_MINUTES_PER_DAY = 480;
    private static final int DEFAULT_PROJECT_MINUTES_PER_WEEK = 2400;
    private static final int DEFAULT_PROJECT_DAYS_PER_MONTH = 20;

    private final MsProjectCalendar msProjectCalendar = new MsProjectCalendar();
    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    public Map<String, Object> buildProjectDraftRequest(String name, String plannedStart, String goal, Integer teamCount,
            List<String> mustHavePhases, List<String> mustHaveMilestones) {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("view_type", "project_draft_request");

        Map<String, Object> project = new LinkedHashMap<String, Object>();
        project.put("name", name);
        project.put("planned_start", blankToNull(plannedStart));
        result.put("project", project);

        Map<String, Object> requirements = new LinkedHashMap<String, Object>();
        requirements.put("goal", blankToNull(goal));
        requirements.put("team_count", teamCount);
        requirements.put("must_have_phases", mustHavePhases == null ? new ArrayList<String>() : mustHavePhases);
        requirements.put("must_have_milestones",
                mustHaveMilestones == null ? new ArrayList<String>() : mustHaveMilestones);
        result.put("requirements", requirements);
        return result;
    }

    public ProjectModel importProjectDraftView(Object draft) {
        if (!(draft instanceof Map<?, ?>)) {
            throw new IllegalArgumentException("project_draft_view がオブジェクトではありません");
        }
        Map<?, ?> data = (Map<?, ?>) draft;
        if (!"project_draft_view".equals(stringValue(data.get("view_type")))) {
            throw new IllegalArgumentException("view_type が project_draft_view ではありません");
        }
        Map<?, ?> project = mapValue(data.get("project"));
        if (isBlank(stringValue(project.get("name")))) {
            throw new IllegalArgumentException("project.name がありません");
        }

        List<Map<?, ?>> inputTasks = mapListValue(data.get("tasks"));
        List<Map<?, ?>> inputResources = mapListValue(data.get("resources"));
        List<Map<?, ?>> inputAssignments = mapListValue(data.get("assignments"));

        Set<String> seenTaskUids = new LinkedHashSet<String>();
        for (Map<?, ?> task : inputTasks) {
            String uid = stringValue(task.get("uid")).trim();
            if (uid.isEmpty()) {
                throw new IllegalArgumentException("draft task の uid がありません");
            }
            if (seenTaskUids.contains(uid)) {
                throw new IllegalArgumentException("draft task の uid が重複しています: " + uid);
            }
            seenTaskUids.add(uid);
            if (isBlank(stringValue(task.get("name")))) {
                throw new IllegalArgumentException("draft task の name がありません: " + uid);
            }
        }
        for (Map<?, ?> task : inputTasks) {
            String uid = stringValue(task.get("uid")).trim();
            String parentUid = stringValue(task.get("parent_uid")).trim();
            if (!parentUid.isEmpty() && !seenTaskUids.contains(parentUid)) {
                throw new IllegalArgumentException(
                        "draft task の parent_uid が既存 uid を指していません: " + uid + " -> " + parentUid);
            }
        }

        Set<String> seenResourceUids = new LinkedHashSet<String>();
        for (Map<?, ?> resource : inputResources) {
            String uid = stringValue(resource.get("uid")).trim();
            if (uid.isEmpty()) {
                throw new IllegalArgumentException("draft resource の uid がありません");
            }
            if (seenResourceUids.contains(uid)) {
                throw new IllegalArgumentException("draft resource の uid が重複しています: " + uid);
            }
            seenResourceUids.add(uid);
            if (isBlank(stringValue(resource.get("name")))) {
                throw new IllegalArgumentException("draft resource の name がありません: " + uid);
            }
        }
        for (Map<?, ?> assignment : inputAssignments) {
            String uid = stringValue(assignment.get("uid")).trim();
            if (uid.isEmpty()) {
                throw new IllegalArgumentException("draft assignment の uid がありません");
            }
            String taskUid = stringValue(assignment.get("task_uid")).trim();
            String resourceUid = stringValue(assignment.get("resource_uid")).trim();
            if (taskUid.isEmpty() || !seenTaskUids.contains(taskUid)) {
                throw new IllegalArgumentException(
                        "draft assignment の task_uid が既存 uid を指していません: " + uid + " -> " + taskUid);
            }
            if (resourceUid.isEmpty() || !seenResourceUids.contains(resourceUid)) {
                throw new IllegalArgumentException(
                        "draft assignment の resource_uid が既存 uid を指していません: " + uid + " -> " + resourceUid);
            }
        }

        String projectStart = firstNonBlank(stringValue(project.get("planned_start")), stringValue(project.get("planned_finish")),
                toIsoLocalString(LocalDateTime.now()));
        Map<String, String> taskUidMap = new LinkedHashMap<String, String>();
        for (int i = 0; i < inputTasks.size(); i++) {
            taskUidMap.put(stringValue(inputTasks.get(i).get("uid")).trim(), String.valueOf(i + 1));
        }
        Map<String, String> resourceUidMap = new LinkedHashMap<String, String>();
        for (int i = 0; i < inputResources.size(); i++) {
            resourceUidMap.put(stringValue(inputResources.get(i).get("uid")).trim(), String.valueOf(i + 1));
        }

        List<NormalizedDraftTask> normalizedTasks = new ArrayList<NormalizedDraftTask>();
        for (int i = 0; i < inputTasks.size(); i++) {
            Map<?, ?> task = inputTasks.get(i);
            NormalizedDraftTask normalized = new NormalizedDraftTask();
            normalized.uid = taskUidMap.get(stringValue(task.get("uid")).trim());
            normalized.name = stringValue(task.get("name")).trim();
            String parentUid = stringValue(task.get("parent_uid")).trim();
            normalized.parentUid = parentUid.isEmpty() ? null : taskUidMap.get(parentUid);
            Integer position = numberValue(task.get("position"));
            normalized.position = position == null ? i : position.intValue();
            normalized.isSummary = booleanValue(task.get("is_summary"), false);
            normalized.isMilestone = booleanValue(task.get("is_milestone"), false);
            Integer percentComplete = numberValue(task.get("percent_complete"));
            normalized.percentComplete = clamp(percentComplete == null ? 0 : percentComplete.intValue(), 0, 100);
            normalized.plannedDuration = blankToNull(stringValue(task.get("planned_duration")));
            normalized.plannedDurationHours = decimalValue(task.get("planned_duration_hours"));
            normalized.plannedStart = blankToNull(stringValue(task.get("planned_start")));
            normalized.plannedFinish = blankToNull(stringValue(task.get("planned_finish")));
            normalized.predecessorUids.addAll(normalizeDraftPredecessors(task, taskUidMap));
            normalizedTasks.add(normalized);
        }

        Map<String, List<NormalizedDraftTask>> byParent = new LinkedHashMap<String, List<NormalizedDraftTask>>();
        for (NormalizedDraftTask task : normalizedTasks) {
            String key = task.parentUid == null ? "__root__" : task.parentUid;
            List<NormalizedDraftTask> siblings = byParent.get(key);
            if (siblings == null) {
                siblings = new ArrayList<NormalizedDraftTask>();
                byParent.put(key, siblings);
            }
            siblings.add(task);
        }
        for (List<NormalizedDraftTask> siblings : byParent.values()) {
            java.util.Collections.sort(siblings, new java.util.Comparator<NormalizedDraftTask>() {
                @Override
                public int compare(NormalizedDraftTask left, NormalizedDraftTask right) {
                    int byPosition = Integer.compare(left.position, right.position);
                    return byPosition != 0 ? byPosition : left.uid.compareTo(right.uid);
                }
            });
        }

        List<TaskModel> orderedTasks = new ArrayList<TaskModel>();
        walkDraftTasks(byParent, "__root__", new ArrayList<Integer>(), orderedTasks, projectStart);

        List<String> taskFinishes = new ArrayList<String>();
        for (TaskModel task : orderedTasks) {
            if (!isBlank(task.finish)) {
                taskFinishes.add(task.finish);
            }
        }
        java.util.Collections.sort(taskFinishes);

        List<ResourceModel> orderedResources = new ArrayList<ResourceModel>();
        for (int i = 0; i < inputResources.size(); i++) {
            Map<?, ?> resource = inputResources.get(i);
            ResourceModel model = new ResourceModel();
            String originalUid = stringValue(resource.get("uid")).trim();
            model.uid = resourceUidMap.get(originalUid);
            model.id = model.uid;
            model.name = stringValue(resource.get("name")).trim();
            model.initials = blankToNull(stringValue(resource.get("initials")).trim());
            model.group = blankToNull(stringValue(resource.get("group")).trim());
            model.maxUnits = decimalValue(resource.get("max_units"));
            model.calendarUID = blankToNull(stringValue(resource.get("calendar_uid")).trim());
            orderedResources.add(model);
        }

        List<AssignmentModel> orderedAssignments = new ArrayList<AssignmentModel>();
        for (int i = 0; i < inputAssignments.size(); i++) {
            Map<?, ?> assignment = inputAssignments.get(i);
            AssignmentModel model = new AssignmentModel();
            model.uid = String.valueOf(i + 1);
            model.taskUid = taskUidMap.get(stringValue(assignment.get("task_uid")).trim());
            model.resourceUid = resourceUidMap.get(stringValue(assignment.get("resource_uid")).trim());
            model.start = blankToNull(stringValue(assignment.get("start")).trim());
            model.finish = blankToNull(stringValue(assignment.get("finish")).trim());
            model.units = decimalValue(assignment.get("units"));
            model.work = blankToNull(stringValue(assignment.get("work")).trim());
            Integer percentWorkComplete = numberValue(assignment.get("percent_work_complete"));
            model.percentWorkComplete = percentWorkComplete == null ? null
                    : Integer.valueOf(clamp(percentWorkComplete.intValue(), 0, 100));
            orderedAssignments.add(model);
        }

        ProjectModel model = new ProjectModel();
        ProjectInfo projectInfo = new ProjectInfo();
        projectInfo.name = stringValue(project.get("name")).trim();
        projectInfo.title = projectInfo.name;
        projectInfo.startDate = projectStart;
        projectInfo.finishDate = firstNonBlank(stringValue(project.get("planned_finish")),
                taskFinishes.isEmpty() ? projectStart : taskFinishes.get(taskFinishes.size() - 1), projectStart);
        Boolean scheduleFromStart = boolObjectValue(project.get("schedule_from_start"));
        projectInfo.scheduleFromStart = scheduleFromStart == null ? true : scheduleFromStart.booleanValue();
        projectInfo.minutesPerDay = integerOrDefault(numberValue(project.get("minutes_per_day")), DEFAULT_PROJECT_MINUTES_PER_DAY);
        projectInfo.minutesPerWeek = integerOrDefault(numberValue(project.get("minutes_per_week")), DEFAULT_PROJECT_MINUTES_PER_WEEK);
        projectInfo.daysPerMonth = integerOrDefault(numberValue(project.get("days_per_month")), DEFAULT_PROJECT_DAYS_PER_MONTH);
        model.project = projectInfo;
        model.tasks = orderedTasks;
        model.resources = orderedResources;
        model.assignments = orderedAssignments;
        return normalizeProjectModel(msProjectCalendar.ensureDefaultProjectCalendar(model));
    }

    public Map<String, Object> exportProjectOverviewView(ProjectModel model) {
        ProjectModel normalized = normalizeProjectModel(model);
        Map<String, String> parentMap = buildTaskParentMap(normalized.tasks);
        List<TaskModel> phaseTasks = collectTopLevelPhases(normalized.tasks);
        Set<String> phaseUidSet = new LinkedHashSet<String>();
        for (TaskModel phase : phaseTasks) {
            phaseUidSet.add(phase.uid);
        }
        List<TaskModel> allMilestones = new ArrayList<TaskModel>();
        for (TaskModel task : normalized.tasks) {
            if (!isPlaceholderUid(task.uid) && task.milestone) {
                allMilestones.add(task);
            }
        }

        Map<String, Map<String, Object>> topLevelDependencyMap = new LinkedHashMap<String, Map<String, Object>>();
        for (TaskModel task : normalized.tasks) {
            String toPhaseUid = resolvePhaseUidForTask(task.uid, parentMap, phaseUidSet);
            if (toPhaseUid == null) {
                continue;
            }
            for (PredecessorModel predecessor : task.predecessors) {
                String fromPhaseUid = resolvePhaseUidForTask(predecessor.predecessorUid, parentMap, phaseUidSet);
                if (fromPhaseUid == null || fromPhaseUid.equals(toPhaseUid)) {
                    continue;
                }
                String type = formatDependencyType(predecessor.type);
                String key = fromPhaseUid + "->" + toPhaseUid + ":" + type;
                if (!topLevelDependencyMap.containsKey(key)) {
                    Map<String, Object> dependency = new LinkedHashMap<String, Object>();
                    dependency.put("from_uid", fromPhaseUid);
                    dependency.put("to_uid", toPhaseUid);
                    dependency.put("type", type);
                    topLevelDependencyMap.put(key, dependency);
                }
            }
        }

        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("view_type", "project_overview_view");
        Map<String, Object> project = new LinkedHashMap<String, Object>();
        project.put("name", normalized.project.name);
        project.put("planned_start", normalized.project.startDate);
        project.put("planned_finish", normalized.project.finishDate);
        putIfNonNull(project, "status_date", normalized.project.statusDate);
        result.put("project", project);

        Map<String, Object> summary = new LinkedHashMap<String, Object>();
        summary.put("task_count", Integer.valueOf(countNonPlaceholderTasks(normalized.tasks)));
        summary.put("summary_task_count", Integer.valueOf(countSummaryTasks(normalized.tasks)));
        summary.put("milestone_count", Integer.valueOf(allMilestones.size()));
        summary.put("max_outline_level", Integer.valueOf(maxOutlineLevel(normalized.tasks)));
        result.put("summary", summary);

        List<Map<String, Object>> phases = new ArrayList<Map<String, Object>>();
        for (TaskModel phase : phaseTasks) {
            Set<String> phaseTaskUids = collectPhaseTaskUids(normalized.tasks, phase.uid);
            List<TaskModel> descendantTasks = new ArrayList<TaskModel>();
            for (TaskModel task : normalized.tasks) {
                if (phaseTaskUids.contains(task.uid)) {
                    descendantTasks.add(task);
                }
            }
            Map<String, Object> phaseJson = new LinkedHashMap<String, Object>();
            phaseJson.put("uid", phase.uid);
            phaseJson.put("name", phase.name);
            phaseJson.put("wbs", firstNonBlank(phase.wbs, phase.outlineNumber));
            phaseJson.put("task_count", Integer.valueOf(descendantTasks.size()));
            phaseJson.put("milestone_count", Integer.valueOf(countMilestones(descendantTasks)));
            phaseJson.put("planned_start", phase.start);
            phaseJson.put("planned_finish", phase.finish);
            phaseJson.put("duration", phase.duration);
            phaseJson.put("duration_hours", parseDurationHours(phase.duration));
            phaseJson.put("percent_complete", phase.percentComplete);
            List<Map<String, Object>> sampleTasks = new ArrayList<Map<String, Object>>();
            for (int i = 0; i < descendantTasks.size() && i < 3; i++) {
                Map<String, Object> sampleTask = new LinkedHashMap<String, Object>();
                sampleTask.put("uid", descendantTasks.get(i).uid);
                sampleTask.put("name", descendantTasks.get(i).name);
                sampleTasks.add(sampleTask);
            }
            phaseJson.put("sample_tasks", sampleTasks);
            phases.add(phaseJson);
        }
        result.put("phases", phases);

        List<Map<String, Object>> milestones = new ArrayList<Map<String, Object>>();
        for (TaskModel task : allMilestones) {
            Map<String, Object> milestone = new LinkedHashMap<String, Object>();
            milestone.put("uid", task.uid);
            milestone.put("name", task.name);
            milestone.put("parent_uid", parentMap.get(task.uid));
            milestone.put("date", firstNonBlank(task.finish, task.start));
            milestones.add(milestone);
        }
        result.put("milestones", milestones);
        result.put("top_level_dependencies", new ArrayList<Map<String, Object>>(topLevelDependencyMap.values()));
        result.put("rules", buildDefaultRules("project_overview_view"));
        return result;
    }

    public Map<String, Object> exportPhaseDetailView(ProjectModel model) {
        return exportPhaseDetailView(model, null, null, null, null);
    }

    public Map<String, Object> exportPhaseDetailView(ProjectModel model, String requestedPhaseUid, String mode, String rootUid,
            Integer maxDepth) {
        ProjectModel normalized = normalizeProjectModel(model);
        List<TaskModel> phaseTasks = collectTopLevelPhases(normalized.tasks);
        if (phaseTasks.isEmpty()) {
            throw new IllegalArgumentException("phase が見つかりません");
        }
        TaskModel phase = requestedPhaseUid == null ? phaseTasks.get(0) : findTaskByUid(phaseTasks, requestedPhaseUid);
        if (phase == null) {
            throw new IllegalArgumentException("phase が見つかりません: " + requestedPhaseUid);
        }

        Map<String, String> parentMap = buildTaskParentMap(normalized.tasks);
        Map<String, Integer> positionMap = buildTaskPositionMap(normalized.tasks, parentMap);
        Set<String> phaseTaskUids = collectPhaseTaskUids(normalized.tasks, phase.uid);
        List<TaskModel> phaseTasksOnly = new ArrayList<TaskModel>();
        for (TaskModel task : normalized.tasks) {
            if (phaseTaskUids.contains(task.uid)) {
                phaseTasksOnly.add(task);
            }
        }

        String resolvedMode = "scoped".equals(mode) ? "scoped" : "full";
        String resolvedRootUid = "scoped".equals(resolvedMode) && !isBlank(rootUid) ? rootUid.trim() : null;
        Integer resolvedMaxDepth = "scoped".equals(resolvedMode) && maxDepth != null && maxDepth.intValue() >= 0
                ? Integer.valueOf(maxDepth.intValue())
                : null;

        Set<String> scopedTaskUids = phaseTaskUids;
        if (resolvedRootUid != null) {
            TaskModel rootTask = findTaskByUid(phaseTasksOnly, resolvedRootUid);
            if (rootTask == null) {
                throw new IllegalArgumentException("phase 配下に root_uid が見つかりません: " + resolvedRootUid);
            }
            scopedTaskUids = collectTaskSubtreeUids(phaseTasksOnly, resolvedRootUid, resolvedMaxDepth);
        }

        List<TaskModel> descendantTasks = new ArrayList<TaskModel>();
        for (TaskModel task : phaseTasksOnly) {
            if (scopedTaskUids.contains(task.uid)) {
                descendantTasks.add(task);
            }
        }

        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("view_type", "phase_detail_view");
        Map<String, Object> project = new LinkedHashMap<String, Object>();
        project.put("name", normalized.project.name);
        project.put("planned_start", normalized.project.startDate);
        project.put("planned_finish", normalized.project.finishDate);
        result.put("project", project);

        Map<String, Object> phaseJson = new LinkedHashMap<String, Object>();
        phaseJson.put("uid", phase.uid);
        phaseJson.put("name", phase.name);
        phaseJson.put("wbs", firstNonBlank(phase.wbs, phase.outlineNumber));
        phaseJson.put("planned_start", phase.start);
        phaseJson.put("planned_finish", phase.finish);
        phaseJson.put("task_count", Integer.valueOf(descendantTasks.size()));
        phaseJson.put("milestone_count", Integer.valueOf(countMilestones(descendantTasks)));
        phaseJson.put("percent_complete", phase.percentComplete);
        result.put("phase", phaseJson);

        Map<String, Object> scope = new LinkedHashMap<String, Object>();
        scope.put("mode", resolvedMode);
        scope.put("root_uid", resolvedRootUid);
        scope.put("max_depth", resolvedMaxDepth);
        result.put("scope", scope);

        List<Map<String, Object>> tasks = new ArrayList<Map<String, Object>>();
        for (TaskModel task : descendantTasks) {
            Map<String, Object> taskJson = new LinkedHashMap<String, Object>();
            taskJson.put("uid", task.uid);
            taskJson.put("name", task.name);
            taskJson.put("parent_uid", parentMap.get(task.uid));
            taskJson.put("position", positionMap.get(task.uid) == null ? Integer.valueOf(0) : positionMap.get(task.uid));
            taskJson.put("is_summary", Boolean.valueOf(task.summary));
            taskJson.put("is_milestone", Boolean.valueOf(task.milestone));
            taskJson.put("planned_duration", task.duration);
            putIfNonNull(taskJson, "planned_duration_hours", parseDurationHours(task.duration));
            taskJson.put("planned_start", task.start);
            taskJson.put("planned_finish", task.finish);
            taskJson.put("percent_complete", task.percentComplete);
            List<String> predecessorUids = new ArrayList<String>();
            for (PredecessorModel predecessor : task.predecessors) {
                predecessorUids.add(predecessor.predecessorUid);
            }
            taskJson.put("predecessor_uids", predecessorUids);
            tasks.add(taskJson);
        }
        result.put("tasks", tasks);

        List<Map<String, Object>> milestones = new ArrayList<Map<String, Object>>();
        for (TaskModel task : descendantTasks) {
            if (!task.milestone) {
                continue;
            }
            Map<String, Object> milestone = new LinkedHashMap<String, Object>();
            milestone.put("uid", task.uid);
            milestone.put("name", task.name);
            milestone.put("date", firstNonBlank(task.finish, task.start));
            milestones.add(milestone);
        }
        result.put("milestones", milestones);

        List<Map<String, Object>> dependencySummary = new ArrayList<Map<String, Object>>();
        for (TaskModel task : descendantTasks) {
            for (PredecessorModel predecessor : task.predecessors) {
                if (!scopedTaskUids.contains(predecessor.predecessorUid)) {
                    continue;
                }
                Map<String, Object> dependency = new LinkedHashMap<String, Object>();
                dependency.put("from_uid", predecessor.predecessorUid);
                dependency.put("to_uid", task.uid);
                dependency.put("type", formatDependencyType(predecessor.type));
                dependency.put("lag", firstNonBlank(predecessor.linkLag, "PT0H0M0S"));
                Double lagHours = parseDurationHours(firstNonBlank(predecessor.linkLag, "PT0H0M0S"));
                dependency.put("lag_hours", lagHours == null ? Integer.valueOf(0) : lagHours);
                dependencySummary.add(dependency);
            }
        }
        result.put("dependency_summary", dependencySummary);
        result.put("rules", buildDefaultRules("phase_detail_view"));
        return result;
    }

    public Map<String, Object> exportTaskEditView(ProjectModel model, String requestedTaskUid) {
        ProjectModel normalized = normalizeProjectModel(model);
        List<TaskModel> tasks = new ArrayList<TaskModel>();
        for (TaskModel task : normalized.tasks) {
            if (!isPlaceholderUid(task.uid)) {
                tasks.add(task);
            }
        }
        if (tasks.isEmpty()) {
            throw new IllegalArgumentException("task が見つかりません");
        }
        TaskModel targetTask = requestedTaskUid == null ? findDefaultTask(tasks) : findTaskByUid(tasks, requestedTaskUid);
        if (targetTask == null) {
            throw new IllegalArgumentException("task が見つかりません: " + requestedTaskUid);
        }

        Map<String, String> parentMap = buildTaskParentMap(normalized.tasks);
        Map<String, Integer> positionMap = buildTaskPositionMap(normalized.tasks, parentMap);
        String parentUid = parentMap.get(targetTask.uid);
        TaskModel parentTask = parentUid == null ? null : findTaskByUid(tasks, parentUid);
        List<Map<String, Object>> siblingTasks = new ArrayList<Map<String, Object>>();
        for (TaskModel task : tasks) {
            String currentParentUid = parentMap.get(task.uid);
            if (equalsNullable(currentParentUid, parentUid) && !task.uid.equals(targetTask.uid)) {
                Map<String, Object> sibling = new LinkedHashMap<String, Object>();
                sibling.put("uid", task.uid);
                sibling.put("name", task.name);
                sibling.put("position", positionMap.get(task.uid) == null ? Integer.valueOf(0) : positionMap.get(task.uid));
                sibling.put("is_summary", Boolean.valueOf(task.summary));
                sibling.put("is_milestone", Boolean.valueOf(task.milestone));
                siblingTasks.add(sibling);
            }
        }

        List<TaskModel> phaseTasks = collectTopLevelPhases(normalized.tasks);
        Set<String> phaseUidSet = new LinkedHashSet<String>();
        for (TaskModel phase : phaseTasks) {
            phaseUidSet.add(phase.uid);
        }
        String phaseUid = resolvePhaseUidForTask(targetTask.uid, parentMap, phaseUidSet);
        TaskModel phaseTask = phaseUid == null ? null : findTaskByUid(tasks, phaseUid);

        Map<String, TaskModel> taskByUid = new LinkedHashMap<String, TaskModel>();
        for (TaskModel task : tasks) {
            taskByUid.put(task.uid, task);
        }
        List<Map<String, Object>> predecessors = new ArrayList<Map<String, Object>>();
        for (PredecessorModel predecessor : targetTask.predecessors) {
            Map<String, Object> item = new LinkedHashMap<String, Object>();
            item.put("task_uid", predecessor.predecessorUid);
            item.put("name", taskByUid.containsKey(predecessor.predecessorUid) ? taskByUid.get(predecessor.predecessorUid).name
                    : predecessor.predecessorUid);
            item.put("type", formatDependencyType(predecessor.type));
            item.put("lag", firstNonBlank(predecessor.linkLag, "PT0H0M0S"));
            Double lagHours = parseDurationHours(firstNonBlank(predecessor.linkLag, "PT0H0M0S"));
            item.put("lag_hours", lagHours == null ? Integer.valueOf(0) : lagHours);
            predecessors.add(item);
        }

        List<Map<String, Object>> successors = new ArrayList<Map<String, Object>>();
        for (TaskModel task : tasks) {
            for (PredecessorModel predecessor : task.predecessors) {
                if (!targetTask.uid.equals(predecessor.predecessorUid)) {
                    continue;
                }
                Map<String, Object> item = new LinkedHashMap<String, Object>();
                item.put("task_uid", task.uid);
                item.put("name", task.name);
                item.put("type", formatDependencyType(predecessor.type));
                item.put("lag", firstNonBlank(predecessor.linkLag, "PT0H0M0S"));
                Double lagHours = parseDurationHours(firstNonBlank(predecessor.linkLag, "PT0H0M0S"));
                item.put("lag_hours", lagHours == null ? Integer.valueOf(0) : lagHours);
                successors.add(item);
            }
        }

        List<Map<String, Object>> assignments = new ArrayList<Map<String, Object>>();
        for (AssignmentModel assignment : normalized.assignments) {
            if (!targetTask.uid.equals(assignment.taskUid)) {
                continue;
            }
            ResourceModel resource = findResourceByUid(normalized.resources, assignment.resourceUid);
            Map<String, Object> item = new LinkedHashMap<String, Object>();
            item.put("uid", assignment.uid);
            item.put("resource_uid", assignment.resourceUid);
            item.put("resource_name", resource == null ? assignment.resourceUid : resource.name);
            item.put("start", assignment.start);
            item.put("finish", assignment.finish);
            item.put("units", assignment.units);
            item.put("work", assignment.work);
            item.put("percent_work_complete", assignment.percentWorkComplete);
            assignments.add(item);
        }

        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("view_type", "task_edit_view");
        Map<String, Object> project = new LinkedHashMap<String, Object>();
        project.put("name", normalized.project.name);
        project.put("planned_start", normalized.project.startDate);
        project.put("planned_finish", normalized.project.finishDate);
        result.put("project", project);
        if (phaseTask == null) {
            result.put("phase", null);
        } else {
            Map<String, Object> phase = new LinkedHashMap<String, Object>();
            phase.put("uid", phaseTask.uid);
            phase.put("name", phaseTask.name);
            result.put("phase", phase);
        }

        Map<String, Object> target = new LinkedHashMap<String, Object>();
        target.put("uid", targetTask.uid);
        target.put("name", targetTask.name);
        target.put("parent_uid", parentUid);
        target.put("position", positionMap.get(targetTask.uid) == null ? Integer.valueOf(0) : positionMap.get(targetTask.uid));
        target.put("is_summary", Boolean.valueOf(targetTask.summary));
        target.put("is_milestone", Boolean.valueOf(targetTask.milestone));
        target.put("planned_duration", targetTask.duration);
        putIfNonNull(target, "planned_duration_hours", parseDurationHours(targetTask.duration));
        target.put("planned_start", targetTask.start);
        target.put("planned_finish", targetTask.finish);
        target.put("percent_complete", targetTask.percentComplete);
        putIfNonNull(target, "notes", targetTask.notes);
        target.put("calendar_uid", targetTask.calendarUID == null ? null : targetTask.calendarUID);
        putIfNonNull(target, "critical", targetTask.critical);
        result.put("target_task", target);

        if (parentTask == null) {
            result.put("parent_task", null);
        } else {
            Map<String, Object> parent = new LinkedHashMap<String, Object>();
            parent.put("uid", parentTask.uid);
            parent.put("name", parentTask.name);
            parent.put("position", positionMap.get(parentTask.uid) == null ? Integer.valueOf(0) : positionMap.get(parentTask.uid));
            result.put("parent_task", parent);
        }
        result.put("sibling_tasks", siblingTasks);
        result.put("predecessors", predecessors);
        result.put("successors", successors);
        result.put("assignments", assignments);
        result.put("rules", buildDefaultRules("task_edit_view"));
        return result;
    }

    private void walkDraftTasks(Map<String, List<NormalizedDraftTask>> byParent, String parentKey, List<Integer> outlinePath,
            List<TaskModel> orderedTasks, String projectStart) {
        List<NormalizedDraftTask> siblings = byParent.get(parentKey);
        if (siblings == null) {
            return;
        }
        for (int i = 0; i < siblings.size(); i++) {
            NormalizedDraftTask task = siblings.get(i);
            List<Integer> currentPath = new ArrayList<Integer>(outlinePath);
            currentPath.add(Integer.valueOf(i + 1));
            String outlineNumber = joinOutlinePath(currentPath);
            String start = firstNonBlank(task.plannedStart, task.plannedFinish, projectStart);
            String finish = task.plannedFinish;
            if (isBlank(finish)) {
                if (task.plannedDurationHours != null) {
                    finish = addHoursToDateTime(start, task.plannedDurationHours.doubleValue());
                } else {
                    finish = start;
                }
            }
            boolean dateOnlyTaskRange = !task.isMilestone && isDateOnlyText(start) && isDateOnlyText(finish)
                    && task.plannedDuration == null && task.plannedDurationHours == null;
            if (dateOnlyTaskRange) {
                start = withTimeOnDate(start, "09:00:00");
                finish = withTimeOnDate(finish, "18:00:00");
            }
            boolean hasChildren = byParent.containsKey(task.uid);

            TaskModel model = new TaskModel();
            model.uid = task.uid;
            model.id = task.uid;
            model.name = task.name;
            model.outlineLevel = Integer.valueOf(currentPath.size());
            model.outlineNumber = outlineNumber;
            model.wbs = outlineNumber;
            model.start = start;
            model.finish = finish;
            model.duration = task.plannedDuration != null ? task.plannedDuration
                    : (task.plannedDurationHours != null ? formatHoursDuration(task.plannedDurationHours.doubleValue()) : "PT0H0M0S");
            model.milestone = task.isMilestone;
            model.summary = task.isSummary || hasChildren;
            model.percentComplete = Integer.valueOf(task.percentComplete);
            for (String predecessorUid : task.predecessorUids) {
                PredecessorModel predecessor = new PredecessorModel();
                predecessor.predecessorUid = predecessorUid;
                model.predecessors.add(predecessor);
            }
            orderedTasks.add(model);
            walkDraftTasks(byParent, task.uid, currentPath, orderedTasks, projectStart);
        }
    }

    private List<String> normalizeDraftPredecessors(Map<?, ?> task, Map<String, String> draftUidMap) {
        List<String> result = new ArrayList<String>();
        Object predecessorUids = task.get("predecessor_uids");
        if (predecessorUids instanceof List<?>) {
            for (Object item : (List<?>) predecessorUids) {
                String uid = stringValue(item);
                if (!isBlank(uid)) {
                    result.add(firstNonBlank(draftUidMap.get(uid), uid));
                }
            }
        }
        Object predecessors = task.get("predecessors");
        if (predecessors instanceof List<?>) {
            for (Object item : (List<?>) predecessors) {
                if (item instanceof String) {
                    String uid = stringValue(item);
                    if (!isBlank(uid)) {
                        result.add(firstNonBlank(draftUidMap.get(uid), uid));
                    }
                } else if (item instanceof Map<?, ?>) {
                    String uid = stringValue(((Map<?, ?>) item).get("task_uid"));
                    if (!isBlank(uid)) {
                        result.add(firstNonBlank(draftUidMap.get(uid), uid));
                    }
                }
            }
        }
        return result;
    }

    private Map<String, Object> buildDefaultRules(String scope) {
        Map<String, Object> rules = new LinkedHashMap<String, Object>();
        if ("project_overview_view".equals(scope)) {
            rules.put("allow_patch_ops", Arrays.asList("add_task", "update_task", "move_task"));
            rules.put("forbid_completed_task_changes", Boolean.TRUE);
            rules.put("forbid_summary_task_direct_edit", Boolean.TRUE);
            return rules;
        }
        if ("task_edit_view".equals(scope)) {
            rules.put("allow_patch_ops", Arrays.asList("update_task", "move_task", "link_tasks", "unlink_tasks", "add_task",
                    "delete_task", "update_assignment", "add_assignment", "delete_assignment"));
            rules.put("allowed_edit_fields", Arrays.asList("name", "notes", "calendar_uid", "percent_complete",
                    "percent_work_complete", "critical", "planned_start", "planned_finish", "planned_duration",
                    "planned_duration_hours", "is_milestone"));
            rules.put("forbid_completed_task_changes", Boolean.TRUE);
            rules.put("forbid_summary_task_direct_edit", Boolean.TRUE);
            return rules;
        }
        rules.put("allow_patch_ops", Arrays.asList("add_task", "update_task", "move_task", "link_tasks", "unlink_tasks"));
        rules.put("forbid_completed_task_changes", Boolean.TRUE);
        rules.put("forbid_summary_task_direct_edit", Boolean.TRUE);
        return rules;
    }

    private Map<String, String> buildTaskParentMap(List<TaskModel> tasks) {
        Map<String, String> parentMap = new LinkedHashMap<String, String>();
        Deque<TaskModel> stack = new ArrayDeque<TaskModel>();
        for (TaskModel task : tasks) {
            while (!stack.isEmpty()
                    && compareOutlineLevel(task.outlineLevel, stack.peekLast().outlineLevel) <= 0) {
                stack.removeLast();
            }
            parentMap.put(task.uid, stack.isEmpty() ? null : stack.peekLast().uid);
            if (task.summary) {
                stack.addLast(task);
            }
        }
        return parentMap;
    }

    private Map<String, Integer> buildTaskPositionMap(List<TaskModel> tasks, Map<String, String> parentMap) {
        Map<String, Integer> counters = new LinkedHashMap<String, Integer>();
        Map<String, Integer> positionMap = new LinkedHashMap<String, Integer>();
        for (TaskModel task : tasks) {
            String parentUid = parentMap.get(task.uid);
            String key = parentUid == null ? "__root__" : parentUid;
            Integer position = counters.containsKey(key) ? counters.get(key) : Integer.valueOf(0);
            positionMap.put(task.uid, position);
            counters.put(key, Integer.valueOf(position.intValue() + 1));
        }
        return positionMap;
    }

    private List<TaskModel> collectTopLevelPhases(List<TaskModel> tasks) {
        List<TaskModel> phases = new ArrayList<TaskModel>();
        for (TaskModel task : tasks) {
            if (!isPlaceholderUid(task.uid) && task.summary && task.outlineLevel != null && task.outlineLevel.intValue() == 1) {
                phases.add(task);
            }
        }
        return phases;
    }

    private Set<String> collectPhaseTaskUids(List<TaskModel> tasks, String phaseUid) {
        Set<String> uids = new LinkedHashSet<String>();
        int phaseIndex = indexOfTask(tasks, phaseUid);
        if (phaseIndex < 0) {
            return uids;
        }
        TaskModel phase = tasks.get(phaseIndex);
        for (int index = phaseIndex + 1; index < tasks.size(); index++) {
            TaskModel task = tasks.get(index);
            if (compareOutlineLevel(task.outlineLevel, phase.outlineLevel) <= 0) {
                break;
            }
            if (!isPlaceholderUid(task.uid)) {
                uids.add(task.uid);
            }
        }
        return uids;
    }

    private Set<String> collectTaskSubtreeUids(List<TaskModel> tasks, String rootUid, Integer maxDepth) {
        Set<String> uids = new LinkedHashSet<String>();
        int rootIndex = indexOfTask(tasks, rootUid);
        if (rootIndex < 0) {
            return uids;
        }
        TaskModel rootTask = tasks.get(rootIndex);
        if (!isPlaceholderUid(rootTask.uid)) {
            uids.add(rootTask.uid);
        }
        for (int index = rootIndex + 1; index < tasks.size(); index++) {
            TaskModel task = tasks.get(index);
            if (compareOutlineLevel(task.outlineLevel, rootTask.outlineLevel) <= 0) {
                break;
            }
            int relativeDepth = task.outlineLevel.intValue() - rootTask.outlineLevel.intValue();
            if (maxDepth != null && relativeDepth > maxDepth.intValue()) {
                continue;
            }
            if (!isPlaceholderUid(task.uid)) {
                uids.add(task.uid);
            }
        }
        return uids;
    }

    private String resolvePhaseUidForTask(String taskUid, Map<String, String> parentMap, Set<String> phaseUidSet) {
        String currentUid = taskUid;
        while (currentUid != null) {
            if (phaseUidSet.contains(currentUid)) {
                return currentUid;
            }
            currentUid = parentMap.get(currentUid);
        }
        return null;
    }

    private int compareOutlineLevel(Integer left, Integer right) {
        int leftValue = left == null ? 0 : left.intValue();
        int rightValue = right == null ? 0 : right.intValue();
        return Integer.compare(leftValue, rightValue);
    }

    private TaskModel findTaskByUid(List<TaskModel> tasks, String uid) {
        for (TaskModel task : tasks) {
            if (uid.equals(task.uid)) {
                return task;
            }
        }
        return null;
    }

    private ResourceModel findResourceByUid(List<ResourceModel> resources, String uid) {
        for (ResourceModel resource : resources) {
            if (equalsNullable(resource.uid, uid)) {
                return resource;
            }
        }
        return null;
    }

    private TaskModel findDefaultTask(List<TaskModel> tasks) {
        for (TaskModel task : tasks) {
            if (!task.summary) {
                return task;
            }
        }
        return tasks.get(0);
    }

    private int indexOfTask(List<TaskModel> tasks, String uid) {
        for (int i = 0; i < tasks.size(); i++) {
            if (uid.equals(tasks.get(i).uid)) {
                return i;
            }
        }
        return -1;
    }

    private int countNonPlaceholderTasks(List<TaskModel> tasks) {
        int count = 0;
        for (TaskModel task : tasks) {
            if (!isPlaceholderUid(task.uid)) {
                count++;
            }
        }
        return count;
    }

    private int countSummaryTasks(List<TaskModel> tasks) {
        int count = 0;
        for (TaskModel task : tasks) {
            if (!isPlaceholderUid(task.uid) && task.summary) {
                count++;
            }
        }
        return count;
    }

    private int countMilestones(List<TaskModel> tasks) {
        int count = 0;
        for (TaskModel task : tasks) {
            if (task.milestone) {
                count++;
            }
        }
        return count;
    }

    private int maxOutlineLevel(List<TaskModel> tasks) {
        int max = 0;
        for (TaskModel task : tasks) {
            max = Math.max(max, task.outlineLevel == null ? 0 : task.outlineLevel.intValue());
        }
        return max;
    }

    private String formatDependencyType(Integer type) {
        if (type == null) {
            return "FS";
        }
        if (type.intValue() == 0) {
            return "FF";
        }
        if (type.intValue() == 1) {
            return "FS";
        }
        if (type.intValue() == 2) {
            return "FF";
        }
        if (type.intValue() == 3) {
            return "SF";
        }
        if (type.intValue() == 4) {
            return "SS";
        }
        return String.valueOf(type);
    }

    private Double parseDurationHours(String duration) {
        String text = stringValue(duration).trim();
        if (text.isEmpty()) {
            return null;
        }
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("^(-)?P(?:T(?:(\\d+)H)?(?:(\\d+)M)?(?:(\\d+)S)?)$")
                .matcher(text);
        if (!matcher.matches()) {
            return null;
        }
        int sign = matcher.group(1) == null ? 1 : -1;
        double hours = matcher.group(2) == null ? 0 : Double.parseDouble(matcher.group(2));
        double minutes = matcher.group(3) == null ? 0 : Double.parseDouble(matcher.group(3));
        double seconds = matcher.group(4) == null ? 0 : Double.parseDouble(matcher.group(4));
        return Double.valueOf(sign * (hours + minutes / 60.0d + seconds / 3600.0d));
    }

    private String addHoursToDateTime(String dateTime, double hours) {
        LocalDateTime parsed = parseDateTime(dateTime);
        if (parsed == null) {
            return dateTime;
        }
        long seconds = Math.round(hours * 60d * 60d);
        return toIsoLocalString(parsed.plusSeconds(seconds));
    }

    private boolean isDateOnlyText(String value) {
        return value != null && value.trim().matches("^\\d{4}-\\d{2}-\\d{2}$");
    }

    private String withTimeOnDate(String dateText, String timeText) {
        return dateText + "T" + timeText;
    }

    private LocalDateTime parseDateTime(String value) {
        if (isBlank(value)) {
            return null;
        }
        try {
            if (isDateOnlyText(value)) {
                return LocalDateTime.parse(value.trim() + "T00:00:00", dateTimeFormatter);
            }
            return LocalDateTime.parse(value.trim(), dateTimeFormatter);
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    private String toIsoLocalString(LocalDateTime value) {
        return value.format(dateTimeFormatter);
    }

    private String formatHoursDuration(double hours) {
        if (Math.abs(hours - Math.rint(hours)) < 1e-9d) {
            return "PT" + (long) Math.rint(hours) + "H";
        }
        DecimalFormat format = new DecimalFormat("0.###");
        return "PT" + format.format(hours) + "H";
    }

    private String joinOutlinePath(List<Integer> outlinePath) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < outlinePath.size(); i++) {
            if (i > 0) {
                builder.append('.');
            }
            builder.append(outlinePath.get(i).intValue());
        }
        return builder.toString();
    }

    private boolean isPlaceholderUid(String value) {
        return "0".equals(stringValue(value).trim());
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private String blankToNull(String value) {
        return isBlank(value) ? null : value;
    }

    private boolean equalsNullable(String left, String right) {
        return left == null ? right == null : left.equals(right);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private void putIfNonNull(Map<String, Object> target, String key, Object value) {
        if (value != null) {
            target.put(key, value);
        }
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private Integer numberValue(Object value) {
        if (value instanceof Number) {
            return Integer.valueOf(((Number) value).intValue());
        }
        String text = stringValue(value).trim();
        if (text.isEmpty()) {
            return null;
        }
        try {
            return Integer.valueOf((int) Math.floor(Double.parseDouble(text)));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Double decimalValue(Object value) {
        if (value instanceof Number) {
            return Double.valueOf(((Number) value).doubleValue());
        }
        String text = stringValue(value).trim();
        if (text.isEmpty()) {
            return null;
        }
        try {
            return Double.valueOf(Double.parseDouble(text));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private boolean booleanValue(Object value, boolean fallback) {
        Boolean parsed = boolObjectValue(value);
        return parsed == null ? fallback : parsed.booleanValue();
    }

    private Boolean boolObjectValue(Object value) {
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof Number) {
            return Boolean.valueOf(((Number) value).intValue() != 0);
        }
        String text = stringValue(value).trim().toLowerCase();
        if (text.isEmpty()) {
            return null;
        }
        if ("1".equals(text) || "true".equals(text) || "yes".equals(text) || "y".equals(text) || "on".equals(text)) {
            return Boolean.TRUE;
        }
        if ("0".equals(text) || "false".equals(text) || "no".equals(text) || "n".equals(text) || "off".equals(text)) {
            return Boolean.FALSE;
        }
        return null;
    }

    private int integerOrDefault(Integer value, int fallback) {
        return value == null ? fallback : value.intValue();
    }

    private String firstNonBlank(String first, String second) {
        return !isBlank(first) ? first : second;
    }

    private String firstNonBlank(String first, String second, String third) {
        return !isBlank(first) ? first : firstNonBlank(second, third);
    }

    private ProjectModel normalizeProjectModel(ProjectModel model) {
        if (model == null) {
            return new ProjectModel();
        }
        if (model.project == null) {
            model.project = new ProjectInfo();
        }
        if (model.project.outlineCodes == null) {
            model.project.outlineCodes = new ArrayList<jp.igapyon.mikuproject.model.OutlineCodeModel>();
        }
        if (model.project.wbsMasks == null) {
            model.project.wbsMasks = new ArrayList<jp.igapyon.mikuproject.model.WbsMaskModel>();
        }
        if (model.project.extendedAttributes == null) {
            model.project.extendedAttributes = new ArrayList<jp.igapyon.mikuproject.model.ProjectExtendedAttributeModel>();
        }
        if (model.tasks == null) {
            model.tasks = new ArrayList<TaskModel>();
        }
        if (model.resources == null) {
            model.resources = new ArrayList<ResourceModel>();
        }
        if (model.assignments == null) {
            model.assignments = new ArrayList<AssignmentModel>();
        }
        if (model.calendars == null) {
            model.calendars = new ArrayList<jp.igapyon.mikuproject.model.CalendarModel>();
        }
        for (TaskModel task : model.tasks) {
            if (task == null) {
                continue;
            }
            if (task.extendedAttributes == null) {
                task.extendedAttributes = new ArrayList<jp.igapyon.mikuproject.model.TaskExtendedAttributeModel>();
            }
            if (task.baselines == null) {
                task.baselines = new ArrayList<jp.igapyon.mikuproject.model.TaskBaselineModel>();
            }
            if (task.timephasedData == null) {
                task.timephasedData = new ArrayList<jp.igapyon.mikuproject.model.TaskTimephasedDataModel>();
            }
            if (task.predecessors == null) {
                task.predecessors = new ArrayList<PredecessorModel>();
            }
        }
        for (ResourceModel resource : model.resources) {
            if (resource == null) {
                continue;
            }
            if (resource.extendedAttributes == null) {
                resource.extendedAttributes = new ArrayList<jp.igapyon.mikuproject.model.ResourceExtendedAttributeModel>();
            }
            if (resource.baselines == null) {
                resource.baselines = new ArrayList<jp.igapyon.mikuproject.model.ResourceBaselineModel>();
            }
            if (resource.timephasedData == null) {
                resource.timephasedData = new ArrayList<jp.igapyon.mikuproject.model.ResourceTimephasedDataModel>();
            }
        }
        for (AssignmentModel assignment : model.assignments) {
            if (assignment == null) {
                continue;
            }
            if (assignment.extendedAttributes == null) {
                assignment.extendedAttributes =
                        new ArrayList<jp.igapyon.mikuproject.model.AssignmentExtendedAttributeModel>();
            }
            if (assignment.baselines == null) {
                assignment.baselines = new ArrayList<jp.igapyon.mikuproject.model.AssignmentBaselineModel>();
            }
            if (assignment.timephasedData == null) {
                assignment.timephasedData =
                        new ArrayList<jp.igapyon.mikuproject.model.AssignmentTimephasedDataModel>();
            }
        }
        for (jp.igapyon.mikuproject.model.CalendarModel calendarModel : model.calendars) {
            if (calendarModel == null) {
                continue;
            }
            if (calendarModel.weekDays == null) {
                calendarModel.weekDays = new ArrayList<jp.igapyon.mikuproject.model.WeekDayModel>();
            }
            if (calendarModel.exceptions == null) {
                calendarModel.exceptions = new ArrayList<jp.igapyon.mikuproject.model.CalendarExceptionModel>();
            }
            if (calendarModel.workWeeks == null) {
                calendarModel.workWeeks = new ArrayList<jp.igapyon.mikuproject.model.WorkWeekModel>();
            }
        }
        return model;
    }

    private Map<?, ?> mapValue(Object value) {
        return value instanceof Map<?, ?> ? (Map<?, ?>) value : new LinkedHashMap<Object, Object>();
    }

    private List<Map<?, ?>> mapListValue(Object value) {
        List<Map<?, ?>> result = new ArrayList<Map<?, ?>>();
        if (value instanceof List<?>) {
            for (Object item : (List<?>) value) {
                if (item instanceof Map<?, ?>) {
                    result.add((Map<?, ?>) item);
                }
            }
        }
        return result;
    }

    private static class NormalizedDraftTask {
        String uid;
        String name;
        String parentUid;
        int position;
        boolean isSummary;
        boolean isMilestone;
        int percentComplete;
        String plannedDuration;
        Double plannedDurationHours;
        String plannedStart;
        String plannedFinish;
        List<String> predecessorUids = new ArrayList<String>();
    }
}
