/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuproject.projectpatchjson;

import java.util.List;
import java.util.Map;

import jp.igapyon.mikuproject.model.AssignmentModel;
import jp.igapyon.mikuproject.model.CalendarModel;
import jp.igapyon.mikuproject.model.ProjectInfo;
import jp.igapyon.mikuproject.model.ProjectModel;
import jp.igapyon.mikuproject.model.ResourceModel;

public class ProjectPatchJsonUpdates {
    private final ProjectPatchJsonUtil util = new ProjectPatchJsonUtil();

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
