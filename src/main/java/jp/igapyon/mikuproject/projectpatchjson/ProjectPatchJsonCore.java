/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuproject.projectpatchjson;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jp.igapyon.mikuproject.model.ProjectModel;
import jp.igapyon.mikuproject.model.TaskModel;
import jp.igapyon.mikuproject.msprojectxml.MsProjectXml;

public class ProjectPatchJsonCore {
    private final ProjectPatchJsonUtil util = new ProjectPatchJsonUtil();
    private final ProjectPatchJsonLinks links = new ProjectPatchJsonLinks();
    private final ProjectPatchJsonEntities entities = new ProjectPatchJsonEntities();
    private final ProjectPatchJsonUpdates updates = new ProjectPatchJsonUpdates();
    private final ProjectPatchJsonTasks tasks = new ProjectPatchJsonTasks();

    public ImportResult importProjectPatchJson(Object documentLike, ProjectModel baseModel) {
        ValidationResult validation = validatePatchDocument(documentLike);
        ProjectModel nextModel = util.cloneProjectModel(baseModel);
        List<ImportChange> changes = new ArrayList<ImportChange>();
        List<PatchWarning> warnings = new ArrayList<PatchWarning>(validation.warnings);
        Map<String, TaskModel> taskByUid = new LinkedHashMap<String, TaskModel>();
        for (TaskModel task : nextModel.tasks) {
            taskByUid.put(task.uid, task);
        }

        for (int index = 0; index < validation.document.operations.size(); index++) {
            PatchOperation operation = validation.document.operations.get(index);
            String op = safe(operation.op).trim();
            if ("update_project".equals(op)) {
                if (operation.fields == null) {
                    warnings.add(warning("update_project.fields がオブジェクトではありません: operations[" + index + "]",
                            "project", "project", nextModel.project.name));
                    continue;
                }
                updates.applyUpdateProjectOperation(nextModel.project, operation.fields, nextModel, changes, warnings, index);
                continue;
            }
            if ("update_assignment".equals(op)) {
                jp.igapyon.mikuproject.model.AssignmentModel assignment = findAssignment(nextModel, safe(operation.uid).trim());
                if (assignment == null) {
                    warnings.add(warning("update_assignment の uid が既存 assignment を指していません: "
                            + safe(operation.uid).trim(), "assignments", safe(operation.uid).trim(), null));
                    continue;
                }
                if (operation.fields == null) {
                    warnings.add(warning("update_assignment.fields がオブジェクトではありません: " + assignment.uid,
                            "assignments", assignment.uid, assignment.uid));
                    continue;
                }
                updates.applyUpdateAssignmentOperation(assignment, operation.fields, nextModel.project, changes, warnings, index);
                continue;
            }
            if ("update_resource".equals(op)) {
                jp.igapyon.mikuproject.model.ResourceModel resource = findResource(nextModel, safe(operation.uid).trim());
                if (resource == null) {
                    warnings.add(warning("update_resource の uid が既存 resource を指していません: "
                            + safe(operation.uid).trim(), "resources", safe(operation.uid).trim(), null));
                    continue;
                }
                if (operation.fields == null) {
                    warnings.add(warning("update_resource.fields がオブジェクトではありません: " + resource.uid, "resources",
                            resource.uid, resource.name));
                    continue;
                }
                updates.applyUpdateResourceOperation(resource, operation.fields, nextModel, changes, warnings, index);
                continue;
            }
            if ("update_calendar".equals(op)) {
                jp.igapyon.mikuproject.model.CalendarModel calendar = findCalendar(nextModel, safe(operation.uid).trim());
                if (calendar == null) {
                    warnings.add(warning("update_calendar の uid が既存 calendar を指していません: "
                            + safe(operation.uid).trim(), "calendars", safe(operation.uid).trim(), null));
                    continue;
                }
                if (operation.fields == null) {
                    warnings.add(warning("update_calendar.fields がオブジェクトではありません: " + calendar.uid, "calendars",
                            calendar.uid, calendar.name));
                    continue;
                }
                updates.applyUpdateCalendarOperation(calendar, operation.fields, nextModel, changes, warnings, index);
                continue;
            }
            if ("add_assignment".equals(op)) {
                entities.applyAddAssignmentOperation(operation, nextModel, changes, warnings, index);
                continue;
            }
            if ("add_task".equals(op)) {
                TaskModel addedTask = tasks.applyAddTaskOperation(operation, nextModel.tasks, nextModel.project, changes, warnings,
                        index, entities);
                if (addedTask != null) {
                    taskByUid.put(addedTask.uid, addedTask);
                }
                continue;
            }
            if ("add_resource".equals(op)) {
                entities.applyAddResourceOperation(operation, nextModel, changes, warnings, index);
                continue;
            }
            if ("add_calendar".equals(op)) {
                entities.applyAddCalendarOperation(operation, nextModel, changes, warnings, index);
                continue;
            }
            if ("delete_resource".equals(op)) {
                entities.applyDeleteResourceOperation(operation, nextModel, changes, warnings, index);
                continue;
            }
            if ("delete_calendar".equals(op)) {
                entities.applyDeleteCalendarOperation(operation, nextModel, changes, warnings, index);
                continue;
            }
            if ("link_tasks".equals(op)) {
                links.applyLinkTasksOperation(operation, taskByUid, changes, warnings, index);
                continue;
            }
            if ("unlink_tasks".equals(op)) {
                links.applyUnlinkTasksOperation(operation, taskByUid, changes, warnings, index);
                continue;
            }
            if ("move_task".equals(op)) {
                tasks.applyMoveTaskOperation(operation, nextModel.tasks, changes, warnings, index);
                taskByUid.clear();
                for (TaskModel task : nextModel.tasks) {
                    taskByUid.put(task.uid, task);
                }
                continue;
            }
            if ("delete_task".equals(op)) {
                tasks.applyDeleteTaskOperation(operation, nextModel, changes, warnings, index);
                taskByUid.clear();
                for (TaskModel task : nextModel.tasks) {
                    taskByUid.put(task.uid, task);
                }
                continue;
            }
            warnings.add(warning(buildUnsupportedOperationWarningMessage(op, index), null, null, null));
        }

        ImportResult result = new ImportResult();
        result.model = new MsProjectXml().normalizeProjectModel(nextModel);
        result.changes = changes;
        result.warnings = warnings;
        return result;
    }

    public ValidationResult validatePatchDocument(Object documentLike) {
        if (!(documentLike instanceof Map<?, ?>)) {
            throw new IllegalArgumentException("Patch JSON がオブジェクトではありません");
        }
        Object operationsValue = ((Map<?, ?>) documentLike).get("operations");
        if (!(operationsValue instanceof List<?>)) {
            throw new IllegalArgumentException("Patch JSON には operations 配列が必要です");
        }
        PatchDocument document = new PatchDocument();
        List<?> operations = (List<?>) operationsValue;
        for (int index = 0; index < operations.size(); index++) {
            Object item = operations.get(index);
            if (!(item instanceof Map<?, ?>)) {
                throw new IllegalArgumentException("operations[" + index + "] がオブジェクトではありません");
            }
            document.operations.add(parseOperation((Map<?, ?>) item));
        }
        ValidationResult result = new ValidationResult();
        result.document = document;
        result.warnings = new ArrayList<PatchWarning>();
        return result;
    }

    private PatchOperation parseOperation(Map<?, ?> map) {
        PatchOperation operation = new PatchOperation();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            operation.sourceKeys.add(String.valueOf(entry.getKey()));
        }
        operation.op = optionalStringValue(map, "op");
        operation.uid = optionalStringValue(map, "uid");
        operation.name = optionalStringValue(map, "name");
        operation.initials = optionalStringValue(map, "initials");
        operation.group = optionalStringValue(map, "group");
        operation.calendarUid = optionalStringValue(map, "calendar_uid");
        operation.maxUnits = number(map.get("max_units"));
        operation.isSummary = bool(map.get("is_summary"));
        operation.newParentUid = optionalStringValue(map, "new_parent_uid");
        operation.newIndex = integer(map.get("new_index"));
        operation.isMilestone = bool(map.get("is_milestone"));
        operation.taskUid = optionalStringValue(map, "task_uid");
        operation.resourceUid = optionalStringValue(map, "resource_uid");
        operation.start = optionalStringValue(map, "start");
        operation.finish = optionalStringValue(map, "finish");
        operation.units = number(map.get("units"));
        operation.work = optionalStringValue(map, "work");
        operation.percentWorkComplete = integer(map.get("percent_work_complete"));
        operation.plannedStart = optionalStringValue(map, "planned_start");
        operation.plannedFinish = optionalStringValue(map, "planned_finish");
        operation.plannedDuration = optionalStringValue(map, "planned_duration");
        operation.plannedDurationHours = number(map.get("planned_duration_hours"));
        operation.fromUid = optionalStringValue(map, "from_uid");
        operation.toUid = optionalStringValue(map, "to_uid");
        operation.type = optionalStringValue(map, "type");
        operation.lag = optionalStringValue(map, "lag");
        operation.lagHours = number(map.get("lag_hours"));
        operation.isBaseCalendar = bool(map.get("is_base_calendar"));
        operation.baseCalendarUid = optionalStringValue(map, "base_calendar_uid");
        operation.standardRate = optionalStringValue(map, "standard_rate");
        operation.overtimeRate = optionalStringValue(map, "overtime_rate");
        operation.costPerUse = number(map.get("cost_per_use"));
        if (map.get("fields") instanceof Map<?, ?>) {
            operation.fields = new LinkedHashMap<String, Object>();
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) map.get("fields")).entrySet()) {
                operation.fields.put(String.valueOf(entry.getKey()), entry.getValue());
            }
        }
        return operation;
    }

    private String buildUnsupportedOperationWarningMessage(String op, int index) {
        String normalizedOp = op == null || op.isEmpty() ? "(empty)" : op;
        return "未対応の op は無視します: operations[" + index + "].op = " + normalizedOp;
    }

    private jp.igapyon.mikuproject.model.AssignmentModel findAssignment(ProjectModel model, String uid) {
        for (jp.igapyon.mikuproject.model.AssignmentModel assignment : model.assignments) {
            if (uid.equals(assignment.uid)) {
                return assignment;
            }
        }
        return null;
    }

    private jp.igapyon.mikuproject.model.ResourceModel findResource(ProjectModel model, String uid) {
        for (jp.igapyon.mikuproject.model.ResourceModel resource : model.resources) {
            if (uid.equals(resource.uid)) {
                return resource;
            }
        }
        return null;
    }

    private jp.igapyon.mikuproject.model.CalendarModel findCalendar(ProjectModel model, String uid) {
        for (jp.igapyon.mikuproject.model.CalendarModel calendar : model.calendars) {
            if (uid.equals(calendar.uid)) {
                return calendar;
            }
        }
        return null;
    }

    private PatchWarning warning(String message, String scope, String uid, String label) {
        PatchWarning warning = new PatchWarning();
        warning.message = message;
        warning.scope = scope;
        warning.uid = uid;
        warning.label = label;
        return warning;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String optionalStringValue(Map<?, ?> map, String key) {
        return map.containsKey(key) ? stringValue(map.get(key)) : null;
    }

    private Integer integer(Object value) {
        if (value instanceof Number) {
            return Integer.valueOf(((Number) value).intValue());
        }
        try {
            return value == null || String.valueOf(value).trim().isEmpty() ? null
                    : Integer.valueOf((int) Math.floor(Double.parseDouble(String.valueOf(value))));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Double number(Object value) {
        if (value instanceof Number) {
            return Double.valueOf(((Number) value).doubleValue());
        }
        try {
            return value == null || String.valueOf(value).trim().isEmpty() ? null
                    : Double.valueOf(Double.parseDouble(String.valueOf(value)));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Boolean bool(Object value) {
        return value instanceof Boolean ? (Boolean) value : null;
    }

    public static class ValidationResult {
        public PatchDocument document;
        public List<PatchWarning> warnings;
    }

    public static class ImportResult {
        public ProjectModel model;
        public List<ImportChange> changes;
        public List<PatchWarning> warnings;
    }
}
