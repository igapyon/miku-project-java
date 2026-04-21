/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuproject.projectpatchjson;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import jp.igapyon.mikuproject.model.AssignmentModel;
import jp.igapyon.mikuproject.model.CalendarModel;
import jp.igapyon.mikuproject.model.ProjectModel;
import jp.igapyon.mikuproject.model.ResourceModel;
import jp.igapyon.mikuproject.model.TaskModel;

public class ProjectPatchJsonEntities {
    private final ProjectPatchJsonUtil util = new ProjectPatchJsonUtil();

    public void warnUnsupportedPatchKeysForScope(PatchOperation operation, Set<String> allowedKeys, String opName, String scope,
            String uid, String label, List<PatchWarning> warnings) {
        if (operation.sourceKeys != null && !operation.sourceKeys.isEmpty()) {
            for (String key : operation.sourceKeys) {
                if (!allowedKeys.contains(key)) {
                    warnings.add(warning(opName + " の未対応 key は無視します: " + key, scope, uid, label));
                }
            }
            return;
        }
        java.lang.reflect.Field[] fields = PatchOperation.class.getFields();
        for (java.lang.reflect.Field field : fields) {
            try {
                Object value = field.get(operation);
                if (value != null && !allowedKeys.contains(fieldNameToKey(field.getName()))) {
                    warnings.add(warning(opName + " の未対応 key は無視します: " + fieldNameToKey(field.getName()), scope, uid, label));
                }
            } catch (IllegalAccessException ex) {
                // ignore
            }
        }
    }

    public ResourceModel applyAddResourceOperation(PatchOperation operation, ProjectModel model, List<ImportChange> changes,
            List<PatchWarning> warnings, int operationIndex) {
        String uid = safe(operation.uid).trim();
        if (uid.isEmpty()) {
            warnings.add(warning("add_resource の uid がありません: operations[" + operationIndex + "]", null, null, null));
            return null;
        }
        if (findResource(model, uid) != null) {
            warnings.add(warning("add_resource の uid が既存 resource と重複しています: " + uid, "resources", uid, uid));
            return null;
        }
        String name = safe(operation.name).trim();
        if (name.isEmpty()) {
            warnings.add(warning("add_resource.name は空でない文字列が必要です: " + uid, "resources", uid, uid));
            return null;
        }
        if (!isBlank(operation.calendarUid) && findCalendar(model, operation.calendarUid.trim()) == null) {
            warnings.add(warning("add_resource.calendar_uid が既存 calendar を指していません: " + uid + " -> "
                    + operation.calendarUid.trim(), "resources", uid, name));
            return null;
        }
        if (operation.maxUnits != null && operation.maxUnits.doubleValue() < 0) {
            warnings.add(warning("add_resource.max_units は 0 以上の数値が必要です: " + uid, "resources", uid, name));
            return null;
        }
        if (operation.costPerUse != null && operation.costPerUse.doubleValue() < 0) {
            warnings.add(warning("add_resource.cost_per_use は 0 以上の数値が必要です: " + uid, "resources", uid, name));
            return null;
        }
        if (operation.percentWorkComplete != null
                && (operation.percentWorkComplete.intValue() < 0 || operation.percentWorkComplete.intValue() > 100)) {
            warnings.add(warning("add_resource.percent_work_complete は 0 以上 100 以下の数値が必要です: " + uid, "resources",
                    uid, name));
            return null;
        }
        ResourceModel resource = new ResourceModel();
        resource.uid = uid;
        resource.id = uid;
        resource.name = name;
        resource.initials = blankToNull(safe(operation.initials).trim());
        resource.group = blankToNull(safe(operation.group).trim());
        resource.calendarUID = blankToNull(safe(operation.calendarUid).trim());
        resource.maxUnits = operation.maxUnits;
        resource.standardRate = blankToNull(safe(operation.standardRate).trim());
        resource.overtimeRate = blankToNull(safe(operation.overtimeRate).trim());
        resource.costPerUse = operation.costPerUse;
        resource.percentWorkComplete = operation.percentWorkComplete;
        model.resources.add(resource);
        changes.add(change("resources", uid, name, "name", null, name));
        return resource;
    }

    public AssignmentModel applyAddAssignmentOperation(PatchOperation operation, ProjectModel model, List<ImportChange> changes,
            List<PatchWarning> warnings, int operationIndex) {
        String uid = safe(operation.uid).trim();
        if (uid.isEmpty()) {
            warnings.add(warning("add_assignment の uid がありません: operations[" + operationIndex + "]", null, null, null));
            return null;
        }
        if (findAssignment(model, uid) != null) {
            warnings.add(warning("add_assignment の uid が既存 assignment と重複しています: " + uid, "assignments", uid, uid));
            return null;
        }
        String taskUid = safe(operation.taskUid).trim();
        if (taskUid.isEmpty() || findTask(model, taskUid) == null) {
            warnings.add(warning("add_assignment.task_uid が既存 task を指していません: " + uid + " -> " + taskUid,
                    "assignments", uid, uid));
            return null;
        }
        String resourceUid = safe(operation.resourceUid).trim();
        if (resourceUid.isEmpty() || findResource(model, resourceUid) == null) {
            warnings.add(warning("add_assignment.resource_uid が既存 resource を指していません: " + uid + " -> " + resourceUid,
                    "assignments", uid, uid));
            return null;
        }
        String normalizedStart = operation.start == null ? null : util.normalizePatchedPlainDateTime(operation.start, "start",
                model.project);
        if (operation.start != null && normalizedStart == null) {
            warnings.add(warning("add_assignment.start の日付形式が解釈できません: " + uid, "assignments", uid, uid));
            return null;
        }
        String normalizedFinish = operation.finish == null ? null
                : util.normalizePatchedPlainDateTime(operation.finish, "finish", model.project);
        if (operation.finish != null && normalizedFinish == null) {
            warnings.add(warning("add_assignment.finish の日付形式が解釈できません: " + uid, "assignments", uid, uid));
            return null;
        }
        if (normalizedStart != null && normalizedFinish != null && normalizedStart.compareTo(normalizedFinish) > 0) {
            warnings.add(warning("add_assignment.start が finish より後です: " + uid, "assignments", uid, uid));
            return null;
        }
        if (operation.units != null && operation.units.doubleValue() < 0) {
            warnings.add(warning("add_assignment.units は 0 以上の数値が必要です: " + uid, "assignments", uid, uid));
            return null;
        }
        if (operation.work != null && safe(operation.work).trim().isEmpty()) {
            warnings.add(warning("add_assignment.work は空でない文字列が必要です: " + uid, "assignments", uid, uid));
            return null;
        }
        if (operation.percentWorkComplete != null
                && (operation.percentWorkComplete.intValue() < 0 || operation.percentWorkComplete.intValue() > 100)) {
            warnings.add(warning("add_assignment.percent_work_complete は 0 以上 100 以下の数値が必要です: " + uid,
                    "assignments", uid, uid));
            return null;
        }
        AssignmentModel assignment = new AssignmentModel();
        assignment.uid = uid;
        assignment.taskUid = taskUid;
        assignment.resourceUid = resourceUid;
        assignment.start = normalizedStart;
        assignment.finish = normalizedFinish;
        assignment.units = operation.units;
        assignment.work = blankToNull(safe(operation.work).trim());
        assignment.percentWorkComplete = operation.percentWorkComplete;
        model.assignments.add(assignment);
        changes.add(change("assignments", uid, uid, "taskUid", null, taskUid));
        return assignment;
    }

    public CalendarModel applyAddCalendarOperation(PatchOperation operation, ProjectModel model, List<ImportChange> changes,
            List<PatchWarning> warnings, int operationIndex) {
        String uid = safe(operation.uid).trim();
        if (uid.isEmpty()) {
            warnings.add(warning("add_calendar の uid がありません: operations[" + operationIndex + "]", null, null, null));
            return null;
        }
        if (findCalendar(model, uid) != null) {
            warnings.add(warning("add_calendar の uid が既存 calendar と重複しています: " + uid, "calendars", uid, uid));
            return null;
        }
        String name = safe(operation.name).trim();
        if (name.isEmpty()) {
            warnings.add(warning("add_calendar.name は空でない文字列が必要です: " + uid, "calendars", uid, uid));
            return null;
        }
        if (operation.isBaseCalendar != null && !(operation.isBaseCalendar instanceof Boolean)) {
            warnings.add(warning("add_calendar.is_base_calendar は boolean が必要です: " + uid, "calendars", uid, name));
            return null;
        }
        if (!isBlank(operation.baseCalendarUid)) {
            if (uid.equals(operation.baseCalendarUid.trim())) {
                warnings.add(warning("add_calendar.base_calendar_uid は自身を指せません: " + uid, "calendars", uid, name));
                return null;
            }
            if (findCalendar(model, operation.baseCalendarUid.trim()) == null) {
                warnings.add(warning("add_calendar.base_calendar_uid が既存 calendar を指していません: " + uid + " -> "
                        + operation.baseCalendarUid.trim(), "calendars", uid, name));
                return null;
            }
        }
        CalendarModel calendar = new CalendarModel();
        calendar.uid = uid;
        calendar.name = name;
        calendar.isBaseCalendar = operation.isBaseCalendar != null ? operation.isBaseCalendar.booleanValue() : false;
        calendar.baseCalendarUID = blankToNull(safe(operation.baseCalendarUid).trim());
        model.calendars.add(calendar);
        changes.add(change("calendars", uid, name, "name", null, name));
        return calendar;
    }

    public void applyDeleteResourceOperation(PatchOperation operation, ProjectModel model, List<ImportChange> changes,
            List<PatchWarning> warnings, int operationIndex) {
        String uid = safe(operation.uid).trim();
        if (uid.isEmpty()) {
            warnings.add(warning("delete_resource の uid がありません: operations[" + operationIndex + "]", null, null, null));
            return;
        }
        ResourceModel resource = findResource(model, uid);
        if (resource == null) {
            warnings.add(warning("delete_resource の uid が既存 resource を指していません: " + uid, "resources", uid, uid));
            return;
        }
        List<String> assignmentUids = new ArrayList<String>();
        for (AssignmentModel assignment : model.assignments) {
            if (uid.equals(assignment.resourceUid)) {
                assignmentUids.add(assignment.uid);
            }
        }
        if (!assignmentUids.isEmpty()) {
            warnings.add(warning("delete_resource first cut では assignment がある resource は削除できません: " + uid
                    + " (assignments=" + join(assignmentUids) + ")", "resources", uid,
                    resource.name == null || resource.name.isEmpty() ? resource.uid : resource.name));
            return;
        }
        model.resources.remove(resource);
        changes.add(change("resources", uid, resource.name == null || resource.name.isEmpty() ? resource.uid : resource.name,
                "name", resource.name == null || resource.name.isEmpty() ? resource.uid : resource.name, "(deleted)"));
    }

    public void applyDeleteCalendarOperation(PatchOperation operation, ProjectModel model, List<ImportChange> changes,
            List<PatchWarning> warnings, int operationIndex) {
        String uid = safe(operation.uid).trim();
        if (uid.isEmpty()) {
            warnings.add(warning("delete_calendar の uid がありません: operations[" + operationIndex + "]", null, null, null));
            return;
        }
        CalendarModel calendar = findCalendar(model, uid);
        if (calendar == null) {
            warnings.add(warning("delete_calendar の uid が既存 calendar を指していません: " + uid, "calendars", uid, uid));
            return;
        }
        List<String> blockers = new ArrayList<String>();
        if (uid.equals(model.project.calendarUID)) {
            blockers.add("project=1");
        }
        List<String> taskRefs = new ArrayList<String>();
        for (TaskModel task : model.tasks) {
            if (uid.equals(task.calendarUID)) {
                taskRefs.add(task.uid);
            }
        }
        if (!taskRefs.isEmpty()) {
            blockers.add("tasks=" + join(taskRefs));
        }
        List<String> resourceRefs = new ArrayList<String>();
        for (ResourceModel resource : model.resources) {
            if (uid.equals(resource.calendarUID)) {
                resourceRefs.add(resource.uid);
            }
        }
        if (!resourceRefs.isEmpty()) {
            blockers.add("resources=" + join(resourceRefs));
        }
        List<String> baseCalendarRefs = new ArrayList<String>();
        for (CalendarModel item : model.calendars) {
            if (!uid.equals(item.uid) && uid.equals(item.baseCalendarUID)) {
                baseCalendarRefs.add(item.uid);
            }
        }
        if (!baseCalendarRefs.isEmpty()) {
            blockers.add("baseRefs=" + join(baseCalendarRefs));
        }
        if (!blockers.isEmpty()) {
            warnings.add(warning("delete_calendar first cut では参照が残っている calendar は削除できません: " + uid
                    + " (" + joinSemicolon(blockers) + ")", "calendars", uid,
                    calendar.name == null || calendar.name.isEmpty() ? calendar.uid : calendar.name));
            return;
        }
        model.calendars.remove(calendar);
        changes.add(change("calendars", uid, calendar.name == null || calendar.name.isEmpty() ? calendar.uid : calendar.name,
                "name", calendar.name == null || calendar.name.isEmpty() ? calendar.uid : calendar.name, "(deleted)"));
    }

    public void applyDeleteAssignmentOperation(PatchOperation operation, ProjectModel model, List<ImportChange> changes,
            List<PatchWarning> warnings, int operationIndex) {
        String uid = safe(operation.uid).trim();
        if (uid.isEmpty()) {
            warnings.add(warning("delete_assignment の uid がありません: operations[" + operationIndex + "]", null, null, null));
            return;
        }
        AssignmentModel assignment = findAssignment(model, uid);
        if (assignment == null) {
            warnings.add(warning("delete_assignment の uid が既存 assignment を指していません: " + uid, "assignments", uid, uid));
            return;
        }
        model.assignments.remove(assignment);
        changes.add(change("assignments", uid, uid, "taskUid", assignment.taskUid, "(deleted)"));
        changes.add(change("assignments", uid, uid, "resourceUid", assignment.resourceUid, "(deleted)"));
    }

    private jp.igapyon.mikuproject.model.TaskModel findTask(ProjectModel model, String uid) {
        for (jp.igapyon.mikuproject.model.TaskModel task : model.tasks) {
            if (uid.equals(task.uid)) {
                return task;
            }
        }
        return null;
    }

    private ResourceModel findResource(ProjectModel model, String uid) {
        for (ResourceModel resource : model.resources) {
            if (uid.equals(resource.uid)) {
                return resource;
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

    private CalendarModel findCalendar(ProjectModel model, String uid) {
        for (CalendarModel calendar : model.calendars) {
            if (uid.equals(calendar.uid)) {
                return calendar;
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

    private String blankToNull(String value) {
        return isBlank(value) ? null : value;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String safe(String value) {
        return value == null ? "" : value;
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

    private String joinSemicolon(List<String> values) {
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < values.size(); index++) {
            if (index > 0) {
                builder.append("; ");
            }
            builder.append(values.get(index));
        }
        return builder.toString();
    }

    private String fieldNameToKey(String fieldName) {
        return fieldName.replaceAll("([A-Z])", "_$1").toLowerCase();
    }
}
