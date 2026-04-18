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
        operation.op = stringValue(map.get("op"));
        operation.uid = stringValue(map.get("uid"));
        operation.name = stringValue(map.get("name"));
        operation.initials = stringValue(map.get("initials"));
        operation.group = stringValue(map.get("group"));
        operation.calendarUid = stringValue(map.get("calendar_uid"));
        operation.maxUnits = number(map.get("max_units"));
        operation.isSummary = bool(map.get("is_summary"));
        operation.newParentUid = map.containsKey("new_parent_uid") ? stringValue(map.get("new_parent_uid")) : null;
        operation.newIndex = integer(map.get("new_index"));
        operation.isMilestone = bool(map.get("is_milestone"));
        operation.taskUid = stringValue(map.get("task_uid"));
        operation.resourceUid = stringValue(map.get("resource_uid"));
        operation.start = stringValue(map.get("start"));
        operation.finish = stringValue(map.get("finish"));
        operation.units = number(map.get("units"));
        operation.work = stringValue(map.get("work"));
        operation.percentWorkComplete = integer(map.get("percent_work_complete"));
        operation.plannedStart = stringValue(map.get("planned_start"));
        operation.plannedFinish = stringValue(map.get("planned_finish"));
        operation.plannedDuration = stringValue(map.get("planned_duration"));
        operation.plannedDurationHours = number(map.get("planned_duration_hours"));
        operation.fromUid = stringValue(map.get("from_uid"));
        operation.toUid = stringValue(map.get("to_uid"));
        operation.type = stringValue(map.get("type"));
        operation.lag = stringValue(map.get("lag"));
        operation.lagHours = number(map.get("lag_hours"));
        operation.isBaseCalendar = bool(map.get("is_base_calendar"));
        operation.baseCalendarUid = stringValue(map.get("base_calendar_uid"));
        operation.standardRate = stringValue(map.get("standard_rate"));
        operation.overtimeRate = stringValue(map.get("overtime_rate"));
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
        if ("link_tasks".equals(normalizedOp) || "unlink_tasks".equals(normalizedOp)) {
            return "未対応の op は無視します: operations[" + index + "].op = " + normalizedOp
                    + "。依存関係は " + normalizedOp + " で扱う方針ですが、現時点では未実装です";
        }
        if ("move_task".equals(normalizedOp)) {
            return "未対応の op は無視します: operations[" + index + "].op = " + normalizedOp
                    + "。親子や順序変更は move_task で扱う方針ですが、現時点では未実装です";
        }
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
