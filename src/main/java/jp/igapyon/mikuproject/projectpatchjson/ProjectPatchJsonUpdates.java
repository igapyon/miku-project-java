/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuproject.projectpatchjson;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;

import jp.igapyon.mikuproject.model.AssignmentModel;
import jp.igapyon.mikuproject.model.CalendarModel;
import jp.igapyon.mikuproject.model.ProjectInfo;
import jp.igapyon.mikuproject.model.ProjectModel;
import jp.igapyon.mikuproject.model.ResourceModel;
import jp.igapyon.mikuproject.model.TaskModel;

public class ProjectPatchJsonUpdates {
    private final ProjectPatchJsonUtil util = new ProjectPatchJsonUtil();

    public void applyUpdateTaskOperation(TaskModel task, Map<String, Object> rawFields, ProjectModel model,
            List<ImportChange> changes, List<PatchWarning> warnings, int operationIndex) {
        Map<String, Object> fields = new LinkedHashMap<String, Object>(rawFields);
        if (fields.isEmpty()) {
            warnings.add(warning("update_task に fields がありません: " + task.uid, "tasks", task.uid, taskLabel(task)));
            return;
        }
        if (fields.containsKey("planned_duration") && fields.containsKey("planned_duration_hours")) {
            warnings.add(warning("planned_duration と planned_duration_hours が同時指定されたため、planned_duration_hours は無視します: "
                    + task.uid, "tasks", task.uid, taskLabel(task)));
            fields.remove("planned_duration_hours");
        }

        java.util.Set<String> handledFields = new LinkedHashSet<String>();
        boolean nextMilestone = task.milestone;

        if (fields.containsKey("is_milestone")) {
            handledFields.add("is_milestone");
            Object rawValue = fields.get("is_milestone");
            if (!(rawValue instanceof Boolean)) {
                warnings.add(warning("update_task.is_milestone は boolean が必要です: " + task.uid, "tasks", task.uid,
                        taskLabel(task)));
            } else if (task.summary && ((Boolean) rawValue).booleanValue()) {
                warnings.add(warning("update_task では summary task を milestone にできません: " + task.uid, "tasks", task.uid,
                        taskLabel(task)));
            } else if (task.milestone != ((Boolean) rawValue).booleanValue()) {
                boolean value = ((Boolean) rawValue).booleanValue();
                changes.add(change("tasks", task.uid, taskLabel(task), "is_milestone", Boolean.valueOf(task.milestone),
                        Boolean.valueOf(value)));
                task.milestone = value;
                nextMilestone = value;
            }
        }

        if (fields.containsKey("name")) {
            handledFields.add("name");
            Object rawValue = fields.get("name");
            if (!(rawValue instanceof String) || ((String) rawValue).trim().length() == 0) {
                warnings.add(warning("update_task.name は空でない文字列が必要です: " + task.uid, "tasks", task.uid,
                        taskLabel(task)));
            } else {
                String value = ((String) rawValue).trim();
                if (!equalsNullable(task.name, value)) {
                    changes.add(change("tasks", task.uid, taskLabel(task), "name", task.name, value));
                    task.name = value;
                }
            }
        }

        if (fields.containsKey("notes")) {
            handledFields.add("notes");
            Object rawValue = fields.get("notes");
            if (!(rawValue instanceof String)) {
                warnings.add(warning("update_task.notes は文字列が必要です: " + task.uid, "tasks", task.uid,
                        taskLabel(task)));
            } else {
                String value = (String) rawValue;
                String currentValue = task.notes == null ? "" : task.notes;
                if (!currentValue.equals(value)) {
                    changes.add(change("tasks", task.uid, taskLabel(task), "notes", task.notes, value));
                    task.notes = value.length() == 0 ? null : value;
                }
            }
        }

        if (fields.containsKey("calendar_uid")) {
            handledFields.add("calendar_uid");
            Object rawValue = fields.get("calendar_uid");
            if (!(rawValue instanceof String)) {
                warnings.add(warning("update_task.calendar_uid は文字列が必要です: " + task.uid, "tasks", task.uid,
                        taskLabel(task)));
            } else {
                String value = ((String) rawValue).trim();
                String nextCalendarUid = value.length() == 0 ? null : value;
                if (nextCalendarUid != null && findCalendar(model, nextCalendarUid) == null) {
                    warnings.add(warning("update_task.calendar_uid が既存 calendar を指していません: " + task.uid + " -> "
                            + nextCalendarUid, "tasks", task.uid, taskLabel(task)));
                } else if (!equalsNullable(task.calendarUID, nextCalendarUid)) {
                    changes.add(change("tasks", task.uid, taskLabel(task), "calendarUID", task.calendarUID,
                            nextCalendarUid == null ? "(cleared)" : nextCalendarUid));
                    task.calendarUID = nextCalendarUid;
                }
            }
        }

        if (fields.containsKey("percent_complete")) {
            handledFields.add("percent_complete");
            Object rawValue = fields.get("percent_complete");
            Integer value = integerPercentage(rawValue);
            if (value == null) {
                warnings.add(warning("update_task.percent_complete は 0 以上 100 以下の整数が必要です: " + task.uid,
                        "tasks", task.uid, taskLabel(task)));
            } else if (!equalsNullable(task.percentComplete, value)) {
                changes.add(change("tasks", task.uid, taskLabel(task), "percentComplete", task.percentComplete,
                        value));
                task.percentComplete = value;
            }
        }

        if (fields.containsKey("percent_work_complete")) {
            handledFields.add("percent_work_complete");
            Object rawValue = fields.get("percent_work_complete");
            Integer value = integerPercentage(rawValue);
            if (value == null) {
                warnings.add(warning("update_task.percent_work_complete は 0 以上 100 以下の整数が必要です: " + task.uid,
                        "tasks", task.uid, taskLabel(task)));
            } else if (!equalsNullable(task.percentWorkComplete, value)) {
                changes.add(change("tasks", task.uid, taskLabel(task), "percentWorkComplete",
                        task.percentWorkComplete, value));
                task.percentWorkComplete = value;
            }
        }

        if (fields.containsKey("critical")) {
            handledFields.add("critical");
            Object rawValue = fields.get("critical");
            if (!(rawValue instanceof Boolean)) {
                warnings.add(warning("update_task.critical は boolean が必要です: " + task.uid, "tasks", task.uid,
                        taskLabel(task)));
            } else {
                Boolean value = (Boolean) rawValue;
                if (!equalsNullable(task.critical, value)) {
                    changes.add(change("tasks", task.uid, taskLabel(task), "critical", task.critical, value));
                    task.critical = value;
                }
            }
        }

        if (fields.containsKey("planned_start")) {
            handledFields.add("planned_start");
            String value = util.normalizePatchedTaskDate(fields.get("planned_start"), "start", task, model.project);
            if (value == null) {
                warnings.add(warning("update_task.planned_start の日付形式が解釈できません: " + task.uid, "tasks", task.uid,
                        taskLabel(task)));
            } else if (!equalsNullable(task.start, value)) {
                changes.add(change("tasks", task.uid, taskLabel(task), "planned_start", task.start, value));
                task.start = value;
            }
        }

        if (fields.containsKey("planned_finish")) {
            handledFields.add("planned_finish");
            String value = util.normalizePatchedTaskDate(fields.get("planned_finish"), "finish", task, model.project);
            if (value == null) {
                warnings.add(warning("update_task.planned_finish の日付形式が解釈できません: " + task.uid, "tasks", task.uid,
                        taskLabel(task)));
            } else if (!equalsNullable(task.finish, value)) {
                changes.add(change("tasks", task.uid, taskLabel(task), "planned_finish", task.finish, value));
                task.finish = value;
            }
        }

        if (fields.containsKey("planned_duration")) {
            handledFields.add("planned_duration");
            Object rawValue = fields.get("planned_duration");
            if (!(rawValue instanceof String) || ((String) rawValue).trim().length() == 0) {
                warnings.add(warning("update_task.planned_duration は空でない文字列が必要です: " + task.uid, "tasks", task.uid,
                        taskLabel(task)));
            } else {
                String value = ((String) rawValue).trim();
                if (!equalsNullable(task.duration, value)) {
                    changes.add(change("tasks", task.uid, taskLabel(task), "planned_duration", task.duration, value));
                    task.duration = value;
                }
            }
        }

        if (fields.containsKey("planned_duration_hours")) {
            handledFields.add("planned_duration_hours");
            Object rawValue = fields.get("planned_duration_hours");
            Double value = finiteNonNegativeNumber(rawValue);
            if (value == null) {
                warnings.add(warning("update_task.planned_duration_hours は 0 以上の数値が必要です: " + task.uid, "tasks",
                        task.uid, taskLabel(task)));
            } else {
                Double before = util.parseDurationHours(task.duration);
                String nextDuration = util.formatDurationHours(value.doubleValue());
                if (!equalsNullable(task.duration, nextDuration)) {
                    changes.add(change("tasks", task.uid, taskLabel(task), "planned_duration_hours", before, value));
                    task.duration = nextDuration;
                }
            }
        }

        if (nextMilestone) {
            if (!equalsNullable(task.finish, task.start)) {
                warnings.add(warning("update_task.is_milestone=true のため planned_finish は planned_start に揃えます: "
                        + task.uid, "tasks", task.uid, taskLabel(task)));
                changes.add(change("tasks", task.uid, taskLabel(task), "planned_finish", task.finish, task.start));
                task.finish = task.start;
            }
            if (!util.isZeroDuration(task.duration)) {
                warnings.add(warning("update_task.is_milestone=true のため planned_duration は 0 に揃えます: " + task.uid,
                        "tasks", task.uid, taskLabel(task)));
                changes.add(change("tasks", task.uid, taskLabel(task), "planned_duration", task.duration, "PT0H0M0S"));
                task.duration = "PT0H0M0S";
            }
        }

        for (String fieldName : fields.keySet()) {
            if (!handledFields.contains(fieldName)) {
                warnings.add(warning("未対応の field は無視します: operations[" + operationIndex + "].fields." + fieldName,
                        "tasks", task.uid, taskLabel(task)));
            }
        }
    }

    public void applyUpdateProjectOperation(ProjectInfo project, Map<String, Object> fields, ProjectModel model, List<ImportChange> changes,
            List<PatchWarning> warnings, int operationIndex) {
        if (fields.isEmpty()) {
            warnings.add(warning("update_project に fields がありません", "project", "project", label(project)));
            return;
        }
        if (fields.containsKey("name")) {
            String name = stringValue(fields.get("name")).trim();
            if (name.isEmpty()) {
                warnings.add(warning("update_project.name は空でない文字列が必要です", "project", "project", label(project)));
            } else if (!name.equals(project.name)) {
                changes.add(change("project", "project", label(project), "name", project.name, name));
                project.name = name;
            }
        }
        String nextStart = project.startDate;
        if (fields.containsKey("start_date")) {
            nextStart = util.normalizePatchedPlainDateTime(fields.get("start_date"), "start", project);
            if (nextStart == null) {
                warnings.add(warning("update_project.start_date の日付形式が解釈できません", "project", "project",
                        label(project)));
            }
        }
        String nextFinish = project.finishDate;
        if (fields.containsKey("finish_date")) {
            nextFinish = util.normalizePatchedPlainDateTime(fields.get("finish_date"), "finish", project);
            if (nextFinish == null) {
                warnings.add(warning("update_project.finish_date の日付形式が解釈できません", "project", "project",
                        label(project)));
            }
        }
        if (nextStart != null && nextFinish != null && nextStart.compareTo(nextFinish) > 0) {
            warnings.add(warning("update_project.start_date が finish_date より後です", "project", "project", label(project)));
        } else {
            if (fields.containsKey("start_date") && nextStart != null && !equalsNullable(project.startDate, nextStart)) {
                changes.add(change("project", "project", label(project), "startDate", project.startDate, nextStart));
                project.startDate = nextStart;
            }
            if (fields.containsKey("finish_date") && nextFinish != null && !equalsNullable(project.finishDate, nextFinish)) {
                changes.add(change("project", "project", label(project), "finishDate", project.finishDate, nextFinish));
                project.finishDate = nextFinish;
            }
        }
        if (fields.containsKey("current_date")) {
            String currentDate = util.normalizePatchedPlainDateTime(fields.get("current_date"), "start", project);
            if (currentDate == null) {
                warnings.add(warning("update_project.current_date の日付形式が解釈できません", "project", "project",
                        label(project)));
            } else if (!equalsNullable(project.currentDate, currentDate)) {
                changes.add(change("project", "project", label(project), "currentDate", project.currentDate, currentDate));
                project.currentDate = currentDate;
            }
        }
        if (fields.containsKey("calendar_uid")) {
            String calendarUid = stringValue(fields.get("calendar_uid")).trim();
            if (!calendarUid.isEmpty() && findCalendar(model, calendarUid) == null) {
                warnings.add(warning("update_project.calendar_uid が既存 calendar を指していません", "project", "project",
                        label(project)));
            } else {
                project.calendarUID = calendarUid.isEmpty() ? null : calendarUid;
            }
        }
        if (fields.containsKey("minutes_per_day")) {
            Double value = number(fields.get("minutes_per_day"));
            if (value == null || value.doubleValue() <= 0) {
                warnings.add(warning("update_project.minutes_per_day は 0 より大きい数値が必要です", "project", "project",
                        label(project)));
            } else {
                project.minutesPerDay = Integer.valueOf(value.intValue());
            }
        }
        if (fields.containsKey("schedule_from_start")) {
            Boolean value = bool(fields.get("schedule_from_start"));
            if (value == null) {
                warnings.add(warning("update_project.schedule_from_start は boolean が必要です", "project", "project",
                        label(project)));
            } else {
                project.scheduleFromStart = value.booleanValue();
            }
        }
    }

    public void applyUpdateAssignmentOperation(AssignmentModel assignment, Map<String, Object> fields, ProjectInfo project,
            List<ImportChange> changes, List<PatchWarning> warnings, int operationIndex) {
        if (fields.containsKey("start")) {
            String start = util.normalizePatchedPlainDateTime(fields.get("start"), "start", project);
            String finish = fields.containsKey("finish") ? util.normalizePatchedPlainDateTime(fields.get("finish"), "finish", project)
                    : assignment.finish;
            if (start != null && finish != null && start.compareTo(finish) > 0) {
                warnings.add(warning("update_assignment.start が finish より後です", "assignments", assignment.uid, assignment.uid));
            } else if (start != null) {
                assignment.start = start;
            }
        }
        if (fields.containsKey("units")) {
            Double units = number(fields.get("units"));
            if (units == null || units.doubleValue() < 0) {
                warnings.add(warning("update_assignment.units は 0 以上の数値が必要です", "assignments", assignment.uid,
                        assignment.uid));
            } else {
                assignment.units = units;
            }
        }
        if (fields.containsKey("work")) {
            String work = stringValue(fields.get("work")).trim();
            if (work.isEmpty()) {
                warnings.add(warning("update_assignment.work は空でない文字列が必要です", "assignments", assignment.uid,
                        assignment.uid));
            } else {
                assignment.work = work;
            }
        }
        if (fields.containsKey("percent_work_complete")) {
            Double value = number(fields.get("percent_work_complete"));
            if (value == null || value.doubleValue() < 0 || value.doubleValue() > 100) {
                warnings.add(warning("update_assignment.percent_work_complete は 0 以上 100 以下の数値が必要です", "assignments",
                        assignment.uid, assignment.uid));
            } else {
                assignment.percentWorkComplete = Integer.valueOf(value.intValue());
            }
        }
    }

    public void applyUpdateResourceOperation(ResourceModel resource, Map<String, Object> fields, ProjectModel model,
            List<ImportChange> changes, List<PatchWarning> warnings, int operationIndex) {
        if (fields.containsKey("name")) {
            String name = stringValue(fields.get("name")).trim();
            if (name.isEmpty()) {
                warnings.add(warning("update_resource.name は空でない文字列が必要です", "resources", resource.uid, label(resource)));
            } else {
                resource.name = name;
            }
        }
        if (fields.containsKey("calendar_uid")) {
            String calendarUid = stringValue(fields.get("calendar_uid")).trim();
            if (!calendarUid.isEmpty() && findCalendar(model, calendarUid) == null) {
                warnings.add(
                        warning("update_resource.calendar_uid が既存 calendar を指していません", "resources", resource.uid,
                                label(resource)));
            } else {
                resource.calendarUID = calendarUid.isEmpty() ? null : calendarUid;
            }
        }
        if (fields.containsKey("max_units")) {
            Double value = number(fields.get("max_units"));
            if (value == null || value.doubleValue() < 0) {
                warnings.add(warning("update_resource.max_units は 0 以上の数値が必要です", "resources", resource.uid,
                        label(resource)));
            } else {
                resource.maxUnits = value;
            }
        }
        if (fields.containsKey("cost_per_use")) {
            Double value = number(fields.get("cost_per_use"));
            if (value == null || value.doubleValue() < 0) {
                warnings.add(warning("update_resource.cost_per_use は 0 以上の数値が必要です", "resources", resource.uid,
                        label(resource)));
            } else {
                resource.costPerUse = value;
            }
        }
        if (fields.containsKey("percent_work_complete")) {
            Double value = number(fields.get("percent_work_complete"));
            if (value == null || value.doubleValue() < 0 || value.doubleValue() > 100) {
                warnings.add(warning("update_resource.percent_work_complete は 0 以上 100 以下の数値が必要です", "resources",
                        resource.uid, label(resource)));
            } else {
                resource.percentWorkComplete = Integer.valueOf(value.intValue());
            }
        }
    }

    public void applyUpdateCalendarOperation(CalendarModel calendar, Map<String, Object> fields, ProjectModel model,
            List<ImportChange> changes, List<PatchWarning> warnings, int operationIndex) {
        if (fields.containsKey("name")) {
            String name = stringValue(fields.get("name")).trim();
            if (name.isEmpty()) {
                warnings.add(warning("update_calendar.name は空でない文字列が必要です", "calendars", calendar.uid, label(calendar)));
            } else {
                calendar.name = name;
            }
        }
        if (fields.containsKey("is_base_calendar")) {
            Boolean value = bool(fields.get("is_base_calendar"));
            if (value == null) {
                warnings.add(warning("update_calendar.is_base_calendar は boolean が必要です", "calendars", calendar.uid,
                        label(calendar)));
            } else {
                calendar.isBaseCalendar = value.booleanValue();
            }
        }
        if (fields.containsKey("base_calendar_uid")) {
            String baseCalendarUid = stringValue(fields.get("base_calendar_uid")).trim();
            if (calendar.uid.equals(baseCalendarUid)) {
                warnings.add(warning("update_calendar.base_calendar_uid は自身を指せません", "calendars", calendar.uid,
                        label(calendar)));
            } else if (!baseCalendarUid.isEmpty() && findCalendar(model, baseCalendarUid) == null) {
                warnings.add(warning("update_calendar.base_calendar_uid が既存 calendar を指していません", "calendars",
                        calendar.uid, label(calendar)));
            } else {
                calendar.baseCalendarUID = baseCalendarUid.isEmpty() ? null : baseCalendarUid;
            }
        }
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

    private String label(ProjectInfo project) {
        return project.name == null || project.name.isEmpty() ? "project" : project.name;
    }

    private String label(ResourceModel resource) {
        return resource.name == null || resource.name.isEmpty() ? resource.uid : resource.name;
    }

    private String label(CalendarModel calendar) {
        return calendar.name == null || calendar.name.isEmpty() ? calendar.uid : calendar.name;
    }

    private String taskLabel(TaskModel task) {
        return task.name == null || task.name.length() == 0 ? task.uid : task.name;
    }

    private Integer integerPercentage(Object value) {
        if (!(value instanceof Number)) {
            return null;
        }
        double number = ((Number) value).doubleValue();
        if (Double.isNaN(number) || Double.isInfinite(number) || number < 0 || number > 100
                || number != Math.floor(number)) {
            return null;
        }
        return Integer.valueOf((int) number);
    }

    private Double finiteNonNegativeNumber(Object value) {
        if (!(value instanceof Number)) {
            return null;
        }
        double number = ((Number) value).doubleValue();
        return Double.isNaN(number) || Double.isInfinite(number) || number < 0 ? null : Double.valueOf(number);
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private Double number(Object value) {
        if (value instanceof Number) {
            return Double.valueOf(((Number) value).doubleValue());
        }
        try {
            return value == null ? null : Double.valueOf(Double.parseDouble(String.valueOf(value)));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Boolean bool(Object value) {
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return null;
    }

    private boolean equalsNullable(Object left, Object right) {
        return left == null ? right == null : left.equals(right);
    }
}
