/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuproject.projectpatchjson;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import jp.igapyon.mikuproject.model.PredecessorModel;
import jp.igapyon.mikuproject.model.TaskModel;

public class ProjectPatchJsonLinks {
    private final ProjectPatchJsonUtil util = new ProjectPatchJsonUtil();

    public void applyLinkTasksOperation(PatchOperation operation, Map<String, TaskModel> taskByUid, List<ImportChange> changes,
            List<PatchWarning> warnings, int operationIndex) {
        TaskRelation relation = resolvePatchTaskRelation(operation, "link_tasks", taskByUid, warnings, operationIndex);
        if (relation == null) {
            return;
        }
        Integer typeCode = util.resolveDependencyType(operation.type, warnings, relation.toTask, operationIndex, "link_tasks",
                false);
        String linkLag = util.resolveDependencyLag(operation, warnings, relation.toTask, operationIndex, "link_tasks", false);
        PredecessorModel existing = findExisting(relation.toTask, relation.fromTask.uid);
        if (existing != null) {
            String beforeText = util.formatPredecessorList(relation.toTask.predecessors);
            Integer nextType = typeCode == null ? existing.type : typeCode;
            String nextLag = linkLag == null ? existing.linkLag : linkLag;
            String relationText = util.formatRequestedDependencyRelation(relation.fromTask.uid, relation.toTask.uid, nextType,
                    nextLag);
            if (equalsNullable(existing.type, nextType) && equalsNullable(existing.linkLag, nextLag)) {
                warnings.add(warning("link_tasks の依存関係は既に存在します: " + relationText, "tasks", relation.toTask.uid,
                        label(relation.toTask)));
                return;
            }
            existing.type = nextType;
            existing.linkLag = nextLag;
            changes.add(change("tasks", relation.toTask.uid, label(relation.toTask), "Predecessors", beforeText,
                    util.formatPredecessorList(relation.toTask.predecessors)));
            return;
        }
        String beforeText = util.formatPredecessorList(relation.toTask.predecessors);
        String relationText = util.formatRequestedDependencyRelation(relation.fromTask.uid, relation.toTask.uid, typeCode, linkLag);
        if (createsDependencyCycle(taskByUid, relation.fromTask.uid, relation.toTask.uid)) {
            warnings.add(warning("link_tasks で循環依存になるため無視します: " + relationText, "tasks", relation.toTask.uid,
                    label(relation.toTask)));
            return;
        }
        PredecessorModel predecessor = new PredecessorModel();
        predecessor.predecessorUid = relation.fromTask.uid;
        predecessor.type = typeCode;
        predecessor.linkLag = linkLag;
        relation.toTask.predecessors.add(predecessor);
        changes.add(change("tasks", relation.toTask.uid, label(relation.toTask), "Predecessors", beforeText,
                util.formatPredecessorList(relation.toTask.predecessors)));
    }

    public void applyUnlinkTasksOperation(PatchOperation operation, Map<String, TaskModel> taskByUid, List<ImportChange> changes,
            List<PatchWarning> warnings, int operationIndex) {
        TaskRelation relation = resolvePatchTaskRelation(operation, "unlink_tasks", taskByUid, warnings, operationIndex);
        if (relation == null) {
            return;
        }
        Integer requestedType = util.resolveDependencyType(operation.type, warnings, relation.toTask, operationIndex,
                "unlink_tasks", true);
        String requestedLag = util.resolveDependencyLag(operation, warnings, relation.toTask, operationIndex, "unlink_tasks",
                true);
        String beforeText = util.formatPredecessorList(relation.toTask.predecessors);
        java.util.List<PredecessorModel> matched = new java.util.ArrayList<PredecessorModel>();
        java.util.List<PredecessorModel> filtered = new java.util.ArrayList<PredecessorModel>();
        for (PredecessorModel predecessor : relation.toTask.predecessors) {
            boolean keep = true;
            if (relation.fromTask.uid.equals(predecessor.predecessorUid)) {
                if (requestedType == null || equalsNullable(predecessor.type, requestedType)) {
                    if (requestedLag == null
                            || equalsNullable(util.normalizeDurationText(predecessor.linkLag), util.normalizeDurationText(requestedLag))) {
                        keep = false;
                        matched.add(predecessor);
                    }
                }
            }
            if (keep) {
                filtered.add(predecessor);
            }
        }
        if (filtered.size() == relation.toTask.predecessors.size()) {
            warnings.add(warning("unlink_tasks の対象依存関係が見つかりません: "
                    + util.formatRequestedDependencyRelation(relation.fromTask.uid, relation.toTask.uid, requestedType,
                            requestedLag), "tasks", relation.toTask.uid, label(relation.toTask)));
            return;
        }
        relation.toTask.predecessors = filtered;
        if (matched.size() > 1) {
            warnings.add(warning("unlink_tasks は一致した依存関係 " + matched.size() + " 件をすべて解除しました: "
                    + util.formatRequestedDependencyRelation(relation.fromTask.uid, relation.toTask.uid, requestedType,
                            requestedLag), "tasks", relation.toTask.uid, label(relation.toTask)));
        }
        changes.add(change("tasks", relation.toTask.uid, label(relation.toTask), "Predecessors", beforeText,
                util.formatPredecessorList(relation.toTask.predecessors)));
    }

    private TaskRelation resolvePatchTaskRelation(PatchOperation operation, String opName, Map<String, TaskModel> taskByUid,
            List<PatchWarning> warnings, int operationIndex) {
        String fromUid = safe(operation.fromUid).trim();
        String toUid = safe(operation.toUid).trim();
        if (fromUid.isEmpty() || toUid.isEmpty()) {
            warnings.add(warning(opName + " には from_uid / to_uid が必要です: operations[" + operationIndex + "]", null, null, null));
            return null;
        }
        TaskModel fromTask = taskByUid.get(fromUid);
        if (fromTask == null) {
            warnings.add(warning(opName + ".from_uid が既存 task を指していません: " + fromUid, "tasks", fromUid, null));
            return null;
        }
        TaskModel toTask = taskByUid.get(toUid);
        if (toTask == null) {
            warnings.add(warning(opName + ".to_uid が既存 task を指していません: " + toUid, "tasks", toUid, null));
            return null;
        }
        if (fromUid.equals(toUid)) {
            warnings.add(warning(opName + " では同一 task を依存元 / 依存先にできません: " + fromUid, "tasks", toTask.uid,
                    label(toTask)));
            return null;
        }
        TaskRelation relation = new TaskRelation();
        relation.fromTask = fromTask;
        relation.toTask = toTask;
        return relation;
    }

    private boolean createsDependencyCycle(Map<String, TaskModel> taskByUid, String fromUid, String toUid) {
        Queue<String> queue = new ArrayDeque<String>();
        java.util.Set<String> visited = new java.util.LinkedHashSet<String>();
        queue.add(toUid);
        while (!queue.isEmpty()) {
            String currentUid = queue.remove();
            if (currentUid.equals(fromUid)) {
                return true;
            }
            if (!visited.add(currentUid)) {
                continue;
            }
            for (TaskModel task : taskByUid.values()) {
                for (PredecessorModel predecessor : task.predecessors) {
                    if (currentUid.equals(predecessor.predecessorUid) && !visited.contains(task.uid)) {
                        queue.add(task.uid);
                    }
                }
            }
        }
        return false;
    }

    private PredecessorModel findExisting(TaskModel task, String predecessorUid) {
        for (PredecessorModel predecessor : task.predecessors) {
            if (predecessorUid.equals(predecessor.predecessorUid)) {
                return predecessor;
            }
        }
        return null;
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

    private String label(TaskModel task) {
        return task.name == null || task.name.isEmpty() ? task.uid : task.name;
    }

    private boolean equalsNullable(Object left, Object right) {
        return left == null ? right == null : left.equals(right);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private static class TaskRelation {
        TaskModel fromTask;
        TaskModel toTask;
    }
}
