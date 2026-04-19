/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuproject.projectpatchjson;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jp.igapyon.mikuproject.model.AssignmentModel;
import jp.igapyon.mikuproject.model.PredecessorModel;
import jp.igapyon.mikuproject.model.ProjectInfo;
import jp.igapyon.mikuproject.model.ProjectModel;
import jp.igapyon.mikuproject.model.TaskModel;

public class ProjectPatchJsonTasks {
    private final ProjectPatchJsonUtil util = new ProjectPatchJsonUtil();

    public TaskModel applyAddTaskOperation(PatchOperation operation, List<TaskModel> tasks, ProjectInfo project,
            List<ImportChange> changes, List<PatchWarning> warnings, int operationIndex,
            ProjectPatchJsonEntities entities) {
        String uid = safe(operation.uid).trim();
        if (uid.isEmpty()) {
            warnings.add(warning("add_task の uid がありません: operations[" + operationIndex + "]", null, null, null));
            return null;
        }
        if ("0".equals(uid)) {
            warnings.add(warning("add_task の uid に placeholder 値 0 は使えません: " + uid, "tasks", uid, null));
            return null;
        }
        if (findTask(tasks, uid) != null) {
            warnings.add(warning("add_task の uid が既存 task と重複しています: " + uid, "tasks", uid, null));
            return null;
        }
        String name = safe(operation.name).trim();
        if (name.isEmpty()) {
            warnings.add(warning("add_task.name は空でない文字列が必要です: " + uid, "tasks", uid, null));
            return null;
        }
        entities.warnUnsupportedPatchKeysForScope(operation,
                setOf("op", "uid", "name", "is_summary", "new_parent_uid", "new_index", "is_milestone", "planned_start",
                        "planned_finish", "planned_duration", "planned_duration_hours"),
                "add_task", "tasks", uid, name, warnings);
        if (operation.isSummary == null && operation.sourceKeys.contains("is_summary")) {
            warnings.add(warning("add_task.is_summary は boolean が必要です: " + uid, "tasks", uid, name));
            return null;
        }
        if (operation.isMilestone == null && operation.sourceKeys.contains("is_milestone")) {
            warnings.add(warning("add_task.is_milestone は boolean が必要です: " + uid, "tasks", uid, name));
            return null;
        }
        boolean isSummary = Boolean.TRUE.equals(operation.isSummary);
        if (isSummary && Boolean.TRUE.equals(operation.isMilestone)) {
            warnings.add(warning("add_task では is_summary と is_milestone を同時に true にできません: " + uid, "tasks", uid,
                    name));
            return null;
        }
        String nextParentUid = isBlank(operation.newParentUid) ? null : operation.newParentUid.trim();
        TaskModel nextParent = null;
        if (nextParentUid != null) {
            nextParent = findTask(tasks, nextParentUid);
            if (nextParent == null) {
                warnings.add(warning("add_task.new_parent_uid が既存 task を指していません: " + nextParentUid, "tasks", uid,
                        name));
                return null;
            }
            if (!nextParent.summary) {
                warnings.add(warning("add_task.new_parent_uid は summary task を指す必要があります: " + nextParentUid, "tasks",
                        uid, name));
                return null;
            }
        }
        if (operation.newIndex == null || operation.newIndex.intValue() < 0) {
            warnings.add(warning("add_task.new_index は 0 以上の整数が必要です: " + uid, "tasks", uid, name));
            return null;
        }
        int nextIndex = operation.newIndex.intValue();
        Map<String, String> parentMap = buildTaskParentMap(tasks);
        List<Integer> siblingStartIndices = findSiblingStartIndices(tasks, parentMap, nextParentUid);
        if (nextIndex > siblingStartIndices.size()) {
            warnings.add(warning("add_task.new_index が sibling 範囲外です: " + uid + " -> " + nextIndex, "tasks", uid, name));
            return null;
        }
        boolean isMilestone = Boolean.TRUE.equals(operation.isMilestone);
        TaskModel pseudoTask = new TaskModel();
        pseudoTask.uid = uid;
        pseudoTask.name = name;
        pseudoTask.milestone = isMilestone;
        String normalizedStartCandidate = normalizeOptionalPatchedTaskDate(operation.plannedStart, "start", pseudoTask, project);
        if (operation.plannedStart != null && normalizedStartCandidate == null) {
            warnings.add(warning("add_task.planned_start の日付形式が解釈できません: " + uid, "tasks", uid, name));
            return null;
        }
        String normalizedFinishCandidate = normalizeOptionalPatchedTaskDate(operation.plannedFinish, "finish", pseudoTask, project);
        if (operation.plannedFinish != null && normalizedFinishCandidate == null) {
            warnings.add(warning("add_task.planned_finish の日付形式が解釈できません: " + uid, "tasks", uid, name));
            return null;
        }
        String normalizedStart = normalizedStartCandidate != null ? normalizedStartCandidate : project.startDate;
        String normalizedFinish = normalizedFinishCandidate != null ? normalizedFinishCandidate : normalizedStart;
        if (normalizedStart != null && normalizedFinish != null && normalizedStart.compareTo(normalizedFinish) > 0) {
            warnings.add(warning("add_task.planned_start が planned_finish より後です: " + uid, "tasks", uid, name));
            return null;
        }
        String duration = resolveAddedTaskDuration(operation, uid, name, warnings);
        if (duration == null) {
            duration = "PT0H0M0S";
        }
        if (isMilestone) {
            if (!safe(normalizedFinish).equals(normalizedStart)) {
                warnings.add(warning("add_task.is_milestone=true のため planned_finish は planned_start に揃えます: " + uid,
                        "tasks", uid, name));
                normalizedFinish = normalizedStart;
            }
            if (!util.isZeroDuration(duration)) {
                warnings.add(warning("add_task.is_milestone=true のため planned_duration は 0 に揃えます: " + uid, "tasks", uid,
                        name));
                duration = "PT0H0M0S";
            }
        }
        int insertionIndex = resolveAddInsertionIndex(tasks, siblingStartIndices, nextIndex, nextParentUid);
        TaskModel addedTask = new TaskModel();
        addedTask.uid = uid;
        addedTask.id = "";
        addedTask.name = name;
        addedTask.outlineLevel = Integer.valueOf(nextParent != null ? nextParent.outlineLevel.intValue() + 1 : 1);
        addedTask.outlineNumber = "";
        addedTask.start = normalizedStart;
        addedTask.finish = normalizedFinish;
        addedTask.duration = duration;
        addedTask.milestone = isMilestone;
        addedTask.summary = isSummary;
        addedTask.percentComplete = Integer.valueOf(0);
        tasks.add(insertionIndex, addedTask);
        rebuildTaskHierarchyMetadata(tasks);
        Map<String, Integer> nextPositionMap = buildTaskPositionMap(tasks, buildTaskParentMap(tasks));
        changes.add(change("tasks", uid, name, "name", null, name));
        changes.add(change("tasks", uid, name, "parent_uid", null, nextParentUid != null ? nextParentUid : "(root)"));
        changes.add(change("tasks", uid, name, "position", null,
                nextPositionMap.containsKey(uid) ? nextPositionMap.get(uid) : Integer.valueOf(nextIndex)));
        if (operation.plannedStart != null) {
            changes.add(change("tasks", uid, name, "planned_start", null, normalizedStart));
        }
        if (operation.plannedFinish != null) {
            changes.add(change("tasks", uid, name, "planned_finish", null, normalizedFinish));
        }
        return addedTask;
    }

    public void applyMoveTaskOperation(PatchOperation operation, List<TaskModel> tasks, List<ImportChange> changes,
            List<PatchWarning> warnings, int operationIndex) {
        String uid = safe(operation.uid).trim();
        if (uid.isEmpty()) {
            warnings.add(warning("move_task の uid がありません: operations[" + operationIndex + "]", null, null, null));
            return;
        }
        int taskIndex = findTaskIndex(tasks, uid);
        if (taskIndex < 0) {
            warnings.add(warning("move_task の uid が既存 task を指していません: " + uid, "tasks", uid, null));
            return;
        }
        TaskModel task = tasks.get(taskIndex);
        Map<String, String> currentParentMap = buildTaskParentMap(tasks);
        Map<String, Integer> currentPositionMap = buildTaskPositionMap(tasks, currentParentMap);
        String currentParentUid = currentParentMap.get(uid);
        String nextParentUid = isBlank(operation.newParentUid) ? null : operation.newParentUid.trim();
        if (operation.newIndex == null || operation.newIndex.intValue() < 0) {
            warnings.add(warning("move_task.new_index は 0 以上の整数が必要です: " + uid, "tasks", uid,
                    defaultLabel(task.name, task.uid)));
            return;
        }
        int nextIndex = operation.newIndex.intValue();
        Range currentRange = collectTaskSubtreeRange(tasks, taskIndex);
        List<TaskModel> subtree = new ArrayList<TaskModel>(tasks.subList(currentRange.start, currentRange.end));
        Set<String> subtreeUidSet = new LinkedHashSet<String>();
        for (TaskModel item : subtree) {
            subtreeUidSet.add(item.uid);
        }
        if (nextParentUid != null && subtreeUidSet.contains(nextParentUid)) {
            warnings.add(warning("move_task では task を自身または配下へ移動できません: " + uid + " -> " + nextParentUid,
                    "tasks", uid, defaultLabel(task.name, task.uid)));
            return;
        }
        TaskModel nextParent = null;
        if (nextParentUid != null) {
            nextParent = findTask(tasks, nextParentUid);
            if (nextParent == null) {
                warnings.add(warning("move_task.new_parent_uid が既存 task を指していません: " + nextParentUid, "tasks", uid,
                        defaultLabel(task.name, task.uid)));
                return;
            }
            if (!nextParent.summary) {
                warnings.add(warning("move_task.new_parent_uid は summary task を指す必要があります: " + nextParentUid, "tasks",
                        uid, defaultLabel(task.name, task.uid)));
                return;
            }
        }
        List<TaskModel> remainingTasks = new ArrayList<TaskModel>(tasks.subList(0, currentRange.start));
        remainingTasks.addAll(tasks.subList(currentRange.end, tasks.size()));
        Map<String, String> remainingParentMap = buildTaskParentMap(remainingTasks);
        List<Integer> siblingStartIndices = findSiblingStartIndices(remainingTasks, remainingParentMap, nextParentUid);
        if (nextIndex > siblingStartIndices.size()) {
            warnings.add(warning("move_task.new_index が sibling 範囲外です: " + uid + " -> " + nextIndex, "tasks", uid,
                    defaultLabel(task.name, task.uid)));
            return;
        }
        int insertionIndex = resolveMoveInsertionIndex(remainingTasks, siblingStartIndices, nextIndex, nextParentUid);
        int nextOutlineLevel = nextParent != null ? nextParent.outlineLevel.intValue() + 1 : 1;
        int levelDelta = nextOutlineLevel - task.outlineLevel.intValue();
        if (isNoOpMove(tasks, remainingTasks, subtree, insertionIndex, levelDelta)) {
            warnings.add(warning("move_task は結果が変わらないため無視します: " + uid + " -> parent="
                    + (nextParentUid != null ? nextParentUid : "(root)") + " index=" + nextIndex, "tasks", uid,
                    defaultLabel(task.name, task.uid)));
            return;
        }
        for (TaskModel item : subtree) {
            item.outlineLevel = Integer.valueOf(item.outlineLevel.intValue() + levelDelta);
        }
        remainingTasks.addAll(insertionIndex, subtree);
        rebuildTaskHierarchyMetadata(remainingTasks);
        tasks.clear();
        tasks.addAll(remainingTasks);
        Map<String, String> nextParentMap = buildTaskParentMap(tasks);
        Map<String, Integer> nextPositionMap = buildTaskPositionMap(tasks, nextParentMap);
        String nextParentText = nextParentUid != null ? nextParentUid : "(root)";
        String currentParentText = currentParentUid != null ? currentParentUid : "(root)";
        Integer nextPosition = nextPositionMap.containsKey(uid) ? nextPositionMap.get(uid) : Integer.valueOf(nextIndex);
        Integer currentPosition = currentPositionMap.containsKey(uid) ? currentPositionMap.get(uid) : Integer.valueOf(0);
        if (!currentParentText.equals(nextParentText)) {
            changes.add(change("tasks", uid, defaultLabel(task.name, task.uid), "parent_uid", currentParentText,
                    nextParentText));
        }
        if (!currentPosition.equals(nextPosition)) {
            changes.add(change("tasks", uid, defaultLabel(task.name, task.uid), "position", currentPosition, nextPosition));
        }
    }

    public void applyDeleteTaskOperation(PatchOperation operation, ProjectModel model, List<ImportChange> changes,
            List<PatchWarning> warnings, int operationIndex) {
        String uid = safe(operation.uid).trim();
        if (uid.isEmpty()) {
            warnings.add(warning("delete_task の uid がありません: operations[" + operationIndex + "]", null, null, null));
            return;
        }
        int taskIndex = findTaskIndex(model.tasks, uid);
        if (taskIndex < 0) {
            warnings.add(warning("delete_task の uid が既存 task を指していません: " + uid, "tasks", uid, null));
            return;
        }
        TaskModel task = model.tasks.get(taskIndex);
        Range range = collectTaskSubtreeRange(model.tasks, taskIndex);
        if (range.end - range.start > 1) {
            int childCount = range.end - range.start - 1;
            warnings.add(warning("delete_task first cut では summary task や子を持つ task は削除できません: " + uid
                    + " (children=" + childCount + ")", "tasks", uid, defaultLabel(task.name, task.uid)));
            return;
        }
        List<String> assignmentUids = new ArrayList<String>();
        for (AssignmentModel assignment : model.assignments) {
            if (uid.equals(assignment.taskUid)) {
                assignmentUids.add(assignment.uid);
            }
        }
        if (!assignmentUids.isEmpty()) {
            warnings.add(warning("delete_task first cut では assignment がある task は削除できません: " + uid + " (assignments="
                    + join(assignmentUids) + ")", "tasks", uid, defaultLabel(task.name, task.uid)));
            return;
        }
        List<String> successorUids = new ArrayList<String>();
        for (TaskModel item : model.tasks) {
            for (PredecessorModel predecessor : item.predecessors) {
                if (uid.equals(predecessor.predecessorUid)) {
                    successorUids.add(item.uid);
                    break;
                }
            }
        }
        if (!successorUids.isEmpty()) {
            warnings.add(warning("delete_task first cut では後続依存がある task は削除できません: " + uid + " (successors="
                    + join(successorUids) + ")", "tasks", uid, defaultLabel(task.name, task.uid)));
            return;
        }
        Map<String, String> parentMap = buildTaskParentMap(model.tasks);
        Map<String, Integer> positionMap = buildTaskPositionMap(model.tasks, parentMap);
        String parentUid = parentMap.get(uid);
        Integer position = positionMap.containsKey(uid) ? positionMap.get(uid) : Integer.valueOf(0);
        model.tasks.remove(taskIndex);
        rebuildTaskHierarchyMetadata(model.tasks);
        changes.add(change("tasks", uid, defaultLabel(task.name, task.uid), "name", defaultLabel(task.name, task.uid),
                "(deleted)"));
        changes.add(change("tasks", uid, defaultLabel(task.name, task.uid), "parent_uid",
                parentUid != null ? parentUid : "(root)", "(deleted)"));
        changes.add(change("tasks", uid, defaultLabel(task.name, task.uid), "position", position, "(deleted)"));
    }

    private String resolveAddedTaskDuration(PatchOperation operation, String uid, String name, List<PatchWarning> warnings) {
        if (operation.plannedDuration != null && operation.plannedDurationHours != null) {
            warnings.add(warning(
                    "add_task.planned_duration と add_task.planned_duration_hours が同時指定されたため、planned_duration_hours は無視します: "
                            + uid,
                    "tasks", uid, name));
        }
        if (operation.plannedDuration != null) {
            String trimmed = operation.plannedDuration.trim();
            if (trimmed.isEmpty()) {
                warnings.add(warning("add_task.planned_duration は空でない文字列が必要です: " + uid, "tasks", uid, name));
                return null;
            }
            return trimmed;
        }
        if (operation.plannedDurationHours != null) {
            if (Double.isNaN(operation.plannedDurationHours.doubleValue())
                    || Double.isInfinite(operation.plannedDurationHours.doubleValue())
                    || operation.plannedDurationHours.doubleValue() < 0) {
                warnings.add(warning("add_task.planned_duration_hours は 0 以上の数値が必要です: " + uid, "tasks", uid,
                        name));
                return null;
            }
            return util.formatDurationHours(operation.plannedDurationHours.doubleValue());
        }
        return null;
    }

    private String normalizeOptionalPatchedTaskDate(Object rawValue, String kind, TaskModel task, ProjectInfo project) {
        if (rawValue == null) {
            return null;
        }
        return util.normalizePatchedTaskDate(rawValue, kind, task, project);
    }

    private Range collectTaskSubtreeRange(List<TaskModel> tasks, int startIndex) {
        TaskModel rootTask = tasks.get(startIndex);
        int end = startIndex + 1;
        while (end < tasks.size() && tasks.get(end).outlineLevel.intValue() > rootTask.outlineLevel.intValue()) {
            end += 1;
        }
        return new Range(startIndex, end);
    }

    private Map<String, String> buildTaskParentMap(List<TaskModel> tasks) {
        Map<String, String> parentMap = new LinkedHashMap<String, String>();
        List<TaskModel> stack = new ArrayList<TaskModel>();
        for (TaskModel task : tasks) {
            while (!stack.isEmpty() && task.outlineLevel.intValue() <= stack.get(stack.size() - 1).outlineLevel.intValue()) {
                stack.remove(stack.size() - 1);
            }
            parentMap.put(task.uid, stack.isEmpty() ? null : stack.get(stack.size() - 1).uid);
            if (task.summary) {
                stack.add(task);
            }
        }
        return parentMap;
    }

    private Map<String, Integer> buildTaskPositionMap(List<TaskModel> tasks, Map<String, String> parentMap) {
        Map<String, Integer> counters = new LinkedHashMap<String, Integer>();
        Map<String, Integer> positionMap = new LinkedHashMap<String, Integer>();
        for (TaskModel task : tasks) {
            String parentUid = parentMap.get(task.uid);
            String key = parentUid != null ? parentUid : "__root__";
            int position = counters.containsKey(key) ? counters.get(key).intValue() : 0;
            positionMap.put(task.uid, Integer.valueOf(position));
            counters.put(key, Integer.valueOf(position + 1));
        }
        return positionMap;
    }

    private List<Integer> findSiblingStartIndices(List<TaskModel> tasks, Map<String, String> parentMap, String nextParentUid) {
        List<Integer> indices = new ArrayList<Integer>();
        for (int index = 0; index < tasks.size(); index++) {
            String parentUid = parentMap.get(tasks.get(index).uid);
            if (equalsNullable(parentUid, nextParentUid)) {
                indices.add(Integer.valueOf(index));
            }
        }
        return indices;
    }

    private int resolveMoveInsertionIndex(List<TaskModel> remainingTasks, List<Integer> siblingStartIndices, int nextIndex,
            String nextParentUid) {
        if (siblingStartIndices.isEmpty()) {
            if (nextParentUid == null) {
                return 0;
            }
            int parentIndex = findTaskIndex(remainingTasks, nextParentUid);
            return parentIndex >= 0 ? parentIndex + 1 : remainingTasks.size();
        }
        if (nextIndex < siblingStartIndices.size()) {
            return siblingStartIndices.get(nextIndex).intValue();
        }
        if (nextParentUid == null) {
            return remainingTasks.size();
        }
        int parentIndex = findTaskIndex(remainingTasks, nextParentUid);
        if (parentIndex < 0) {
            return remainingTasks.size();
        }
        return collectTaskSubtreeRange(remainingTasks, parentIndex).end;
    }

    private int resolveAddInsertionIndex(List<TaskModel> tasks, List<Integer> siblingStartIndices, int nextIndex,
            String nextParentUid) {
        if (siblingStartIndices.isEmpty()) {
            if (nextParentUid == null) {
                return 0;
            }
            int parentIndex = findTaskIndex(tasks, nextParentUid);
            return parentIndex >= 0 ? parentIndex + 1 : tasks.size();
        }
        if (nextIndex < siblingStartIndices.size()) {
            return siblingStartIndices.get(nextIndex).intValue();
        }
        if (nextParentUid == null) {
            return tasks.size();
        }
        int parentIndex = findTaskIndex(tasks, nextParentUid);
        if (parentIndex < 0) {
            return tasks.size();
        }
        return collectTaskSubtreeRange(tasks, parentIndex).end;
    }

    private boolean isNoOpMove(List<TaskModel> originalTasks, List<TaskModel> remainingTasks, List<TaskModel> subtree,
            int insertionIndex, int levelDelta) {
        String currentSignature = taskSignature(originalTasks, 0);
        List<TaskSignature> candidateTasks = new ArrayList<TaskSignature>();
        for (TaskModel task : remainingTasks) {
            candidateTasks.add(new TaskSignature(task.uid, task.outlineLevel.intValue()));
        }
        List<TaskSignature> subtreeSignature = new ArrayList<TaskSignature>();
        for (TaskModel task : subtree) {
            subtreeSignature.add(new TaskSignature(task.uid, task.outlineLevel.intValue() + levelDelta));
        }
        candidateTasks.addAll(insertionIndex, subtreeSignature);
        StringBuilder builder = new StringBuilder();
        for (TaskSignature signature : candidateTasks) {
            if (builder.length() > 0) {
                builder.append('|');
            }
            builder.append(signature.uid).append('@').append(signature.outlineLevel);
        }
        return currentSignature.equals(builder.toString());
    }

    private String taskSignature(List<TaskModel> tasks, int levelDelta) {
        StringBuilder builder = new StringBuilder();
        for (TaskModel task : tasks) {
            if (builder.length() > 0) {
                builder.append('|');
            }
            builder.append(task.uid).append('@').append(task.outlineLevel.intValue() + levelDelta);
        }
        return builder.toString();
    }

    private void rebuildTaskHierarchyMetadata(List<TaskModel> tasks) {
        List<Integer> counters = new ArrayList<Integer>();
        for (int index = 0; index < tasks.size(); index++) {
            TaskModel task = tasks.get(index);
            int outlineLevel = task.outlineLevel.intValue();
            while (counters.size() < Math.max(1, outlineLevel)) {
                counters.add(Integer.valueOf(0));
            }
            while (counters.size() > Math.max(1, outlineLevel)) {
                counters.remove(counters.size() - 1);
            }
            counters.set(outlineLevel - 1, Integer.valueOf(counters.get(outlineLevel - 1).intValue() + 1));
            StringBuilder outlineNumber = new StringBuilder();
            for (int i = 0; i < outlineLevel; i++) {
                if (i > 0) {
                    outlineNumber.append('.');
                }
                outlineNumber.append(counters.get(i).intValue());
            }
            task.outlineNumber = outlineNumber.toString();
            task.id = String.valueOf(index + 1);
        }
    }

    private TaskModel findTask(List<TaskModel> tasks, String uid) {
        int index = findTaskIndex(tasks, uid);
        return index >= 0 ? tasks.get(index) : null;
    }

    private int findTaskIndex(List<TaskModel> tasks, String uid) {
        for (int index = 0; index < tasks.size(); index++) {
            if (uid.equals(tasks.get(index).uid)) {
                return index;
            }
        }
        return -1;
    }

    private ImportChange change(String scope, String uid, String label, String field, Object before, Object after) {
        ImportChange change = new ImportChange();
        change.scope = scope;
        change.uid = uid;
        change.label = label;
        change.field = field;
        change.before = before;
        change.after = after;
        return change;
    }

    private PatchWarning warning(String message, String scope, String uid, String label) {
        PatchWarning warning = new PatchWarning();
        warning.message = message;
        warning.scope = scope;
        warning.uid = uid;
        warning.label = label;
        return warning;
    }

    private Set<String> setOf(String... values) {
        Set<String> set = new LinkedHashSet<String>();
        for (String value : values) {
            set.add(value);
        }
        return set;
    }

    private String join(List<String> values) {
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < values.size(); index++) {
            if (index > 0) {
                builder.append(", ");
            }
            builder.append(values.get(index));
        }
        return builder.toString();
    }

    private String defaultLabel(String name, String uid) {
        return isBlank(name) ? uid : name;
    }

    private boolean equalsNullable(String left, String right) {
        return left == null ? right == null : left.equals(right);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private static final class Range {
        private final int start;
        private final int end;

        private Range(int start, int end) {
            this.start = start;
            this.end = end;
        }
    }

    private static final class TaskSignature {
        private final String uid;
        private final int outlineLevel;

        private TaskSignature(String uid, int outlineLevel) {
            this.uid = uid;
            this.outlineLevel = outlineLevel;
        }
    }
}
