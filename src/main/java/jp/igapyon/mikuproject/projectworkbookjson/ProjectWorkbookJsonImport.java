/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuproject.projectworkbookjson;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jp.igapyon.mikuproject.model.CalendarExceptionModel;
import jp.igapyon.mikuproject.model.CalendarModel;
import jp.igapyon.mikuproject.model.PredecessorModel;
import jp.igapyon.mikuproject.model.ProjectModel;
import jp.igapyon.mikuproject.model.ResourceModel;
import jp.igapyon.mikuproject.model.TaskModel;
import jp.igapyon.mikuproject.msprojectxml.MsProjectXml;

public class ProjectWorkbookJsonImport {
    private final ProjectWorkbookJsonValidate validate = new ProjectWorkbookJsonValidate();

    public ImportResult importProjectWorkbookJson(Object documentLike, ProjectModel baseModel) {
        ProjectWorkbookJsonValidate.ValidationResult validation = validate.validateWorkbookJsonDocument(documentLike);
        ProjectModel model = new MsProjectXml().normalizeProjectModel(cloneModel(baseModel));
        List<ImportChange> changes = new ArrayList<ImportChange>();
        importProjectRows(validation.document.ensureSheet("Project"), model, changes);
        importTaskRows(validation.document.ensureSheet("Tasks"), model, changes);
        importResourceRows(validation.document.ensureSheet("Resources"), model, changes);
        importAssignmentRows(validation.document.ensureSheet("Assignments"), model, changes);
        importCalendarRows(validation.document.ensureSheet("Calendars"), model, changes);
        importNonWorkingDayRows(validation.document.ensureSheet("NonWorkingDays"), model, changes);
        ImportResult result = new ImportResult();
        result.model = new MsProjectXml().normalizeProjectModel(model);
        result.changes = changes;
        result.warnings = validation.warnings;
        return result;
    }

    public ImportAsProjectModelResult importProjectWorkbookJsonAsProjectModel(Object documentLike) {
        ProjectWorkbookJsonValidate.ValidationResult validation = validate.validateWorkbookJsonDocument(documentLike);
        ProjectModel model = new MsProjectXml().normalizeProjectModel(new ProjectModel());
        importProjectRows(validation.document.ensureSheet("Project"), model, new ArrayList<ImportChange>());
        importTaskRowsAsProjectModel(validation.document.ensureSheet("Tasks"), model);
        importResourceRowsAsProjectModel(validation.document.ensureSheet("Resources"), model);
        importAssignmentRowsAsProjectModel(validation.document.ensureSheet("Assignments"), model);
        importCalendarRowsAsProjectModel(validation.document.ensureSheet("Calendars"), model);
        importNonWorkingDayRows(validation.document.ensureSheet("NonWorkingDays"), model, new ArrayList<ImportChange>());
        ImportAsProjectModelResult result = new ImportAsProjectModelResult();
        result.model = new MsProjectXml().normalizeProjectModel(model);
        result.warnings = validation.warnings;
        return result;
    }

    public ProjectWorkbookJsonValidate.ValidationResult validateWorkbookJsonDocument(Object documentLike) {
        return validate.validateWorkbookJsonDocument(documentLike);
    }

    private void importProjectRows(List<Map<String, Object>> rows, ProjectModel model, List<ImportChange> changes) {
        for (Map<String, Object> row : rows) {
            String field = stringValue(row.get("Field"));
            Object value = row.get("Value");
            if ("Name".equals(field)) updateProjectField(model, changes, field, model.project.name, stringValue(value));
            if ("Title".equals(field)) updateProjectField(model, changes, field, model.project.title, stringValue(value));
            if ("Author".equals(field)) updateProjectField(model, changes, field, model.project.author, stringValue(value));
            if ("Company".equals(field)) updateProjectField(model, changes, field, model.project.company, stringValue(value));
            if ("StartDate".equals(field)) updateProjectField(model, changes, field, model.project.startDate, normalizeDateTime(value, "start"));
            if ("FinishDate".equals(field)) updateProjectField(model, changes, field, model.project.finishDate, normalizeDateTime(value, "finish"));
            if ("CurrentDate".equals(field)) updateProjectField(model, changes, field, model.project.currentDate, normalizeDateTime(value, "start"));
            if ("StatusDate".equals(field)) updateProjectField(model, changes, field, model.project.statusDate, normalizeDateTime(value, "finish"));
            if ("CalendarUID".equals(field)) updateProjectField(model, changes, field, model.project.calendarUID, stringValue(value));
            if ("MinutesPerDay".equals(field)) model.project.minutesPerDay = updateProjectNumberField(changes, "project", "project", defaultLabel(model.project.name, "project"), field,
                    model.project.minutesPerDay, integerValue(value));
            if ("MinutesPerWeek".equals(field)) model.project.minutesPerWeek = updateProjectNumberField(changes, "project", "project", defaultLabel(model.project.name, "project"), field,
                    model.project.minutesPerWeek, integerValue(value));
            if ("DaysPerMonth".equals(field)) model.project.daysPerMonth = updateProjectNumberField(changes, "project", "project", defaultLabel(model.project.name, "project"), field,
                    model.project.daysPerMonth, integerValue(value));
            if ("ScheduleFromStart".equals(field)) {
                Boolean parsed = booleanValue(value);
                if (parsed != null && parsed.booleanValue() != model.project.scheduleFromStart) {
                    changes.add(change("project", "project", defaultLabel(model.project.name, "project"), field,
                            Boolean.valueOf(model.project.scheduleFromStart), parsed));
                    model.project.scheduleFromStart = parsed.booleanValue();
                }
            }
        }
    }

    private void importTaskRows(List<Map<String, Object>> rows, ProjectModel model, List<ImportChange> changes) {
        for (Map<String, Object> row : rows) {
            TaskModel task = findTask(model, stringValue(row.get("UID")));
            if (task == null) {
                continue;
            }
            task.name = applyStringChange(changes, "tasks", task.uid, defaultLabel(task.name, task.uid), "Name", task.name,
                    stringValue(row.get("Name")), task.name);
            task.start = applyTaskDateChange(changes, task, "Start", task.start, row.get("Start"), "start");
            task.finish = applyTaskDateChange(changes, task, "Finish", task.finish, row.get("Finish"), "finish");
            task.duration = applyStringChange(changes, "tasks", task.uid, defaultLabel(task.name, task.uid), "Duration", task.duration,
                    stringValue(row.get("Duration")), task.duration);
            task.percentComplete = applyIntegerChange(changes, "tasks", task.uid, defaultLabel(task.name, task.uid),
                    "PercentComplete", task.percentComplete, integerValue(row.get("PercentComplete")));
            task.percentWorkComplete = applyIntegerChange(changes, "tasks", task.uid, defaultLabel(task.name, task.uid),
                    "PercentWorkComplete", task.percentWorkComplete, integerValue(row.get("PercentWorkComplete")));
            task.milestone = applyBooleanChange(changes, "tasks", task.uid, defaultLabel(task.name, task.uid), "Milestone",
                    task.milestone, booleanValue(row.get("Milestone")), task.milestone);
            task.summary = applyBooleanChange(changes, "tasks", task.uid, defaultLabel(task.name, task.uid), "Summary",
                    task.summary, booleanValue(row.get("Summary")), task.summary);
            task.critical = applyNullableBooleanChange(changes, "tasks", task.uid, defaultLabel(task.name, task.uid), "Critical",
                    task.critical, booleanValue(row.get("Critical")));
            task.type = applyIntegerChange(changes, "tasks", task.uid, defaultLabel(task.name, task.uid), "Type", task.type,
                    integerValue(row.get("Type")));
            task.priority = applyIntegerChange(changes, "tasks", task.uid, defaultLabel(task.name, task.uid), "Priority",
                    task.priority, integerValue(row.get("Priority")));
            task.calendarUID = applyStringChange(changes, "tasks", task.uid, defaultLabel(task.name, task.uid), "CalendarUID",
                    task.calendarUID, stringValue(row.get("CalendarUID")), task.calendarUID);
            task.constraintType = applyIntegerChange(changes, "tasks", task.uid, defaultLabel(task.name, task.uid), "ConstraintType",
                    task.constraintType, integerValue(row.get("ConstraintType")));
            task.constraintDate = applyTaskDateChange(changes, task, "ConstraintDate", task.constraintDate, row.get("ConstraintDate"),
                    "start");
            task.deadline = applyTaskDateChange(changes, task, "Deadline", task.deadline, row.get("Deadline"), "finish");
            task.predecessors = parsePredecessors(row.get("Predecessors"), task.predecessors);
            task.notes = applyStringChange(changes, "tasks", task.uid, defaultLabel(task.name, task.uid), "Notes", task.notes,
                    stringValue(row.get("Notes")), task.notes);
        }
    }

    private void importResourceRows(List<Map<String, Object>> rows, ProjectModel model, List<ImportChange> changes) {
        for (Map<String, Object> row : rows) {
            ResourceModel resource = findResource(model, stringValue(row.get("UID")));
            if (resource == null) {
                continue;
            }
            resource.name = applyStringChange(changes, "resources", resource.uid, defaultLabel(resource.name, resource.uid), "Name",
                    resource.name, stringValue(row.get("Name")), resource.name);
            resource.type = applyIntegerChange(changes, "resources", resource.uid, defaultLabel(resource.name, resource.uid), "Type",
                    resource.type, integerValue(row.get("Type")));
            resource.initials = applyStringChange(changes, "resources", resource.uid, defaultLabel(resource.name, resource.uid),
                    "Initials", resource.initials, stringValue(row.get("Initials")), resource.initials);
            resource.group = applyStringChange(changes, "resources", resource.uid, defaultLabel(resource.name, resource.uid), "Group",
                    resource.group, stringValue(row.get("Group")), resource.group);
            resource.maxUnits = applyDoubleChange(changes, "resources", resource.uid, defaultLabel(resource.name, resource.uid),
                    "MaxUnits", resource.maxUnits, doubleValue(row.get("MaxUnits")));
            resource.calendarUID = applyStringChange(changes, "resources", resource.uid, defaultLabel(resource.name, resource.uid),
                    "CalendarUID", resource.calendarUID, stringValue(row.get("CalendarUID")), resource.calendarUID);
            resource.standardRate = applyStringChange(changes, "resources", resource.uid, defaultLabel(resource.name, resource.uid),
                    "StandardRate", resource.standardRate, stringValue(row.get("StandardRate")), resource.standardRate);
            resource.overtimeRate = applyStringChange(changes, "resources", resource.uid, defaultLabel(resource.name, resource.uid),
                    "OvertimeRate", resource.overtimeRate, stringValue(row.get("OvertimeRate")), resource.overtimeRate);
            resource.costPerUse = applyDoubleChange(changes, "resources", resource.uid, defaultLabel(resource.name, resource.uid),
                    "CostPerUse", resource.costPerUse, doubleValue(row.get("CostPerUse")));
            resource.work = applyStringChange(changes, "resources", resource.uid, defaultLabel(resource.name, resource.uid), "Work",
                    resource.work, stringValue(row.get("Work")), resource.work);
            resource.actualWork = applyStringChange(changes, "resources", resource.uid, defaultLabel(resource.name, resource.uid),
                    "ActualWork", resource.actualWork, stringValue(row.get("ActualWork")), resource.actualWork);
            resource.remainingWork = applyStringChange(changes, "resources", resource.uid, defaultLabel(resource.name, resource.uid),
                    "RemainingWork", resource.remainingWork, stringValue(row.get("RemainingWork")), resource.remainingWork);
            resource.cost = applyDoubleChange(changes, "resources", resource.uid, defaultLabel(resource.name, resource.uid), "Cost",
                    resource.cost, doubleValue(row.get("Cost")));
            resource.actualCost = applyDoubleChange(changes, "resources", resource.uid, defaultLabel(resource.name, resource.uid),
                    "ActualCost", resource.actualCost, doubleValue(row.get("ActualCost")));
            resource.remainingCost = applyDoubleChange(changes, "resources", resource.uid, defaultLabel(resource.name, resource.uid),
                    "RemainingCost", resource.remainingCost, doubleValue(row.get("RemainingCost")));
            resource.percentWorkComplete = applyIntegerChange(changes, "resources", resource.uid, defaultLabel(resource.name, resource.uid),
                    "PercentWorkComplete", resource.percentWorkComplete, integerValue(row.get("PercentWorkComplete")));
            resource.workGroup = applyIntegerChange(changes, "resources", resource.uid, defaultLabel(resource.name, resource.uid),
                    "WorkGroup", resource.workGroup, integerValue(row.get("WorkGroup")));
            resource.standardRateFormat = applyIntegerChange(changes, "resources", resource.uid, defaultLabel(resource.name, resource.uid),
                    "StandardRateFormat", resource.standardRateFormat, integerValue(row.get("StandardRateFormat")));
            resource.overtimeRateFormat = applyIntegerChange(changes, "resources", resource.uid, defaultLabel(resource.name, resource.uid),
                    "OvertimeRateFormat", resource.overtimeRateFormat, integerValue(row.get("OvertimeRateFormat")));
        }
    }

    private void importAssignmentRows(List<Map<String, Object>> rows, ProjectModel model, List<ImportChange> changes) {
        for (Map<String, Object> row : rows) {
            jp.igapyon.mikuproject.model.AssignmentModel assignment = findAssignment(model, stringValue(row.get("UID")));
            if (assignment == null) {
                continue;
            }
            assignment.start = applyAssignmentDateChange(changes, assignment, "Start", assignment.start, row.get("Start"), "start");
            assignment.finish = applyAssignmentDateChange(changes, assignment, "Finish", assignment.finish, row.get("Finish"), "finish");
            assignment.startVariance = applyStringChange(changes, "assignments", assignment.uid, assignment.uid, "StartVariance",
                    assignment.startVariance, stringValue(row.get("StartVariance")), assignment.startVariance);
            assignment.finishVariance = applyStringChange(changes, "assignments", assignment.uid, assignment.uid, "FinishVariance",
                    assignment.finishVariance, stringValue(row.get("FinishVariance")), assignment.finishVariance);
            assignment.delay = applyStringChange(changes, "assignments", assignment.uid, assignment.uid, "Delay", assignment.delay,
                    stringValue(row.get("Delay")), assignment.delay);
            assignment.milestone = applyNullableBooleanChange(changes, "assignments", assignment.uid, assignment.uid, "Milestone",
                    assignment.milestone, booleanValue(row.get("Milestone")));
            assignment.workContour = applyIntegerChange(changes, "assignments", assignment.uid, assignment.uid, "WorkContour",
                    assignment.workContour, integerValue(row.get("WorkContour")));
            assignment.units = applyDoubleChange(changes, "assignments", assignment.uid, assignment.uid, "Units", assignment.units,
                    doubleValue(row.get("Units")));
            assignment.work = applyStringChange(changes, "assignments", assignment.uid, assignment.uid, "Work", assignment.work,
                    stringValue(row.get("Work")), assignment.work);
            assignment.cost = applyDoubleChange(changes, "assignments", assignment.uid, assignment.uid, "Cost", assignment.cost,
                    doubleValue(row.get("Cost")));
            assignment.actualWork = applyStringChange(changes, "assignments", assignment.uid, assignment.uid, "ActualWork",
                    assignment.actualWork, stringValue(row.get("ActualWork")), assignment.actualWork);
            assignment.remainingWork = applyStringChange(changes, "assignments", assignment.uid, assignment.uid, "RemainingWork",
                    assignment.remainingWork, stringValue(row.get("RemainingWork")), assignment.remainingWork);
            assignment.actualCost = applyDoubleChange(changes, "assignments", assignment.uid, assignment.uid, "ActualCost",
                    assignment.actualCost, doubleValue(row.get("ActualCost")));
            assignment.remainingCost = applyDoubleChange(changes, "assignments", assignment.uid, assignment.uid, "RemainingCost",
                    assignment.remainingCost, doubleValue(row.get("RemainingCost")));
            assignment.overtimeWork = applyStringChange(changes, "assignments", assignment.uid, assignment.uid, "OvertimeWork",
                    assignment.overtimeWork, stringValue(row.get("OvertimeWork")), assignment.overtimeWork);
            assignment.actualOvertimeWork = applyStringChange(changes, "assignments", assignment.uid, assignment.uid,
                    "ActualOvertimeWork", assignment.actualOvertimeWork, stringValue(row.get("ActualOvertimeWork")),
                    assignment.actualOvertimeWork);
            assignment.percentWorkComplete = applyIntegerChange(changes, "assignments", assignment.uid, assignment.uid,
                    "PercentWorkComplete", assignment.percentWorkComplete, integerValue(row.get("PercentWorkComplete")));
        }
    }

    private void importCalendarRows(List<Map<String, Object>> rows, ProjectModel model, List<ImportChange> changes) {
        for (Map<String, Object> row : rows) {
            CalendarModel calendar = findCalendar(model, stringValue(row.get("UID")));
            if (calendar == null) {
                continue;
            }
            calendar.name = applyStringChange(changes, "calendars", calendar.uid, defaultLabel(calendar.name, calendar.uid), "Name",
                    calendar.name, stringValue(row.get("Name")), calendar.name);
            Boolean baseCalendar = booleanValue(row.get("IsBaseCalendar"));
            if (baseCalendar != null && baseCalendar.booleanValue() != calendar.isBaseCalendar) {
                changes.add(change("calendars", calendar.uid, defaultLabel(calendar.name, calendar.uid), "IsBaseCalendar",
                        Boolean.valueOf(calendar.isBaseCalendar), baseCalendar));
                calendar.isBaseCalendar = baseCalendar.booleanValue();
            }
            calendar.baseCalendarUID = applyStringChange(changes, "calendars", calendar.uid, defaultLabel(calendar.name, calendar.uid),
                    "BaseCalendarUID", calendar.baseCalendarUID, stringValue(row.get("BaseCalendarUID")), calendar.baseCalendarUID);
        }
    }

    private void importNonWorkingDayRows(List<Map<String, Object>> rows, ProjectModel model, List<ImportChange> changes) {
        for (Map<String, Object> row : rows) {
            CalendarModel calendar = findCalendar(model, stringValue(row.get("CalendarUID")));
            if (calendar == null) {
                continue;
            }
            Integer index = integerValue(row.get("Index"));
            if (index == null || index.intValue() < 0) {
                continue;
            }
            while (calendar.exceptions.size() <= index.intValue()) {
                calendar.exceptions.add(new CalendarExceptionModel());
            }
            CalendarExceptionModel exception = calendar.exceptions.get(index.intValue());
            if (row.containsKey("Name")) {
                exception.name = stringValue(row.get("Name"));
            }
            if (row.containsKey("Date") && !isBlank(stringValue(row.get("Date")))) {
                exception.fromDate = normalizeDateTime(row.get("Date"), "start");
                exception.toDate = normalizeDateTime(row.get("Date"), "finish");
            }
            if (row.containsKey("FromDate") && !isBlank(stringValue(row.get("FromDate")))) {
                exception.fromDate = normalizeDateTime(row.get("FromDate"), "start");
            }
            if (row.containsKey("ToDate") && !isBlank(stringValue(row.get("ToDate")))) {
                exception.toDate = normalizeDateTime(row.get("ToDate"), "finish");
            }
            if (row.containsKey("DayWorking")) {
                exception.dayWorking = booleanValue(row.get("DayWorking"));
            }
        }
    }

    private void importTaskRowsAsProjectModel(List<Map<String, Object>> rows, ProjectModel model) {
        for (Map<String, Object> row : rows) {
            TaskModel task = new TaskModel();
            task.uid = stringValue(row.get("UID"));
            task.id = stringValue(row.get("ID"));
            task.name = stringValue(row.get("Name"));
            task.outlineLevel = integerValue(row.get("OutlineLevel"));
            task.outlineNumber = stringValue(row.get("OutlineNumber"));
            task.wbs = stringValue(row.get("WBS"));
            task.start = normalizeDateTime(row.get("Start"), "start");
            task.finish = normalizeDateTime(row.get("Finish"), "finish");
            task.duration = stringValue(row.get("Duration"));
            task.percentComplete = integerValue(row.get("PercentComplete"));
            task.percentWorkComplete = integerValue(row.get("PercentWorkComplete"));
            Boolean milestone = booleanValue(row.get("Milestone"));
            task.milestone = milestone != null ? milestone.booleanValue() : false;
            Boolean summary = booleanValue(row.get("Summary"));
            task.summary = summary != null ? summary.booleanValue() : false;
            task.critical = booleanValue(row.get("Critical"));
            task.type = integerValue(row.get("Type"));
            task.priority = integerValue(row.get("Priority"));
            task.calendarUID = stringValue(row.get("CalendarUID"));
            task.constraintType = integerValue(row.get("ConstraintType"));
            task.constraintDate = normalizeDateTime(row.get("ConstraintDate"), "start");
            task.deadline = normalizeDateTime(row.get("Deadline"), "finish");
            task.predecessors = parsePredecessors(row.get("Predecessors"), new ArrayList<PredecessorModel>());
            task.notes = stringValue(row.get("Notes"));
            model.tasks.add(task);
        }
    }

    private void importResourceRowsAsProjectModel(List<Map<String, Object>> rows, ProjectModel model) {
        for (Map<String, Object> row : rows) {
            ResourceModel resource = new ResourceModel();
            resource.uid = stringValue(row.get("UID"));
            resource.id = stringValue(row.get("ID"));
            resource.name = stringValue(row.get("Name"));
            resource.type = integerValue(row.get("Type"));
            resource.initials = stringValue(row.get("Initials"));
            resource.group = stringValue(row.get("Group"));
            resource.maxUnits = doubleValue(row.get("MaxUnits"));
            resource.calendarUID = stringValue(row.get("CalendarUID"));
            resource.standardRate = stringValue(row.get("StandardRate"));
            resource.overtimeRate = stringValue(row.get("OvertimeRate"));
            resource.costPerUse = doubleValue(row.get("CostPerUse"));
            resource.work = stringValue(row.get("Work"));
            resource.actualWork = stringValue(row.get("ActualWork"));
            resource.remainingWork = stringValue(row.get("RemainingWork"));
            resource.cost = doubleValue(row.get("Cost"));
            resource.actualCost = doubleValue(row.get("ActualCost"));
            resource.remainingCost = doubleValue(row.get("RemainingCost"));
            resource.percentWorkComplete = integerValue(row.get("PercentWorkComplete"));
            resource.workGroup = integerValue(row.get("WorkGroup"));
            resource.standardRateFormat = integerValue(row.get("StandardRateFormat"));
            resource.overtimeRateFormat = integerValue(row.get("OvertimeRateFormat"));
            model.resources.add(resource);
        }
    }

    private void importAssignmentRowsAsProjectModel(List<Map<String, Object>> rows, ProjectModel model) {
        for (Map<String, Object> row : rows) {
            jp.igapyon.mikuproject.model.AssignmentModel assignment = new jp.igapyon.mikuproject.model.AssignmentModel();
            assignment.uid = stringValue(row.get("UID"));
            assignment.taskUid = stringValue(row.get("TaskUID"));
            assignment.resourceUid = stringValue(row.get("ResourceUID"));
            assignment.start = normalizeDateTime(row.get("Start"), "start");
            assignment.finish = normalizeDateTime(row.get("Finish"), "finish");
            assignment.startVariance = stringValue(row.get("StartVariance"));
            assignment.finishVariance = stringValue(row.get("FinishVariance"));
            assignment.delay = stringValue(row.get("Delay"));
            assignment.milestone = booleanValue(row.get("Milestone"));
            assignment.workContour = integerValue(row.get("WorkContour"));
            assignment.units = doubleValue(row.get("Units"));
            assignment.work = stringValue(row.get("Work"));
            assignment.cost = doubleValue(row.get("Cost"));
            assignment.actualWork = stringValue(row.get("ActualWork"));
            assignment.remainingWork = stringValue(row.get("RemainingWork"));
            assignment.actualCost = doubleValue(row.get("ActualCost"));
            assignment.remainingCost = doubleValue(row.get("RemainingCost"));
            assignment.overtimeWork = stringValue(row.get("OvertimeWork"));
            assignment.actualOvertimeWork = stringValue(row.get("ActualOvertimeWork"));
            assignment.percentWorkComplete = integerValue(row.get("PercentWorkComplete"));
            model.assignments.add(assignment);
        }
    }

    private void importCalendarRowsAsProjectModel(List<Map<String, Object>> rows, ProjectModel model) {
        for (Map<String, Object> row : rows) {
            CalendarModel calendar = new CalendarModel();
            calendar.uid = stringValue(row.get("UID"));
            calendar.name = stringValue(row.get("Name"));
            Boolean isBaseCalendar = booleanValue(row.get("IsBaseCalendar"));
            calendar.isBaseCalendar = isBaseCalendar != null ? isBaseCalendar.booleanValue() : false;
            calendar.baseCalendarUID = stringValue(row.get("BaseCalendarUID"));
            model.calendars.add(calendar);
        }
    }

    private void updateProjectField(ProjectModel model, List<ImportChange> changes, String field, String before, String after) {
        if (after == null) {
            return;
        }
        if ("Name".equals(field) && !equalsText(before, after)) {
            model.project.name = after;
            changes.add(change("project", "project", defaultLabel(after, "project"), field, before, after));
        } else if ("Title".equals(field) && !equalsText(before, after)) {
            model.project.title = after;
            changes.add(change("project", "project", defaultLabel(model.project.name, "project"), field, before, after));
        } else if ("Author".equals(field) && !equalsText(before, after)) {
            model.project.author = after;
            changes.add(change("project", "project", defaultLabel(model.project.name, "project"), field, before, after));
        } else if ("Company".equals(field) && !equalsText(before, after)) {
            model.project.company = after;
            changes.add(change("project", "project", defaultLabel(model.project.name, "project"), field, before, after));
        } else if ("StartDate".equals(field) && !equalsText(before, after)) {
            model.project.startDate = after;
            changes.add(change("project", "project", defaultLabel(model.project.name, "project"), field, before, after));
        } else if ("FinishDate".equals(field) && !equalsText(before, after)) {
            model.project.finishDate = after;
            changes.add(change("project", "project", defaultLabel(model.project.name, "project"), field, before, after));
        } else if ("CurrentDate".equals(field) && !equalsText(before, after)) {
            model.project.currentDate = after;
            changes.add(change("project", "project", defaultLabel(model.project.name, "project"), field, before, after));
        } else if ("StatusDate".equals(field) && !equalsText(before, after)) {
            model.project.statusDate = after;
            changes.add(change("project", "project", defaultLabel(model.project.name, "project"), field, before, after));
        } else if ("CalendarUID".equals(field) && !equalsText(before, after)) {
            model.project.calendarUID = after;
            changes.add(change("project", "project", defaultLabel(model.project.name, "project"), field, before, after));
        }
    }

    private Integer updateProjectNumberField(List<ImportChange> changes, String scope, String uid, String label, String field,
            Integer before, Integer after) {
        if (after != null && !after.equals(before)) {
            changes.add(change(scope, uid, label, field, before, after));
            return after;
        }
        return before;
    }

    private String applyTaskDateChange(List<ImportChange> changes, TaskModel task, String field, String before, Object rawValue,
            String kind) {
        String normalized = normalizeDateTime(rawValue, kind);
        if (normalized != null && !equalsText(before, normalized)) {
            changes.add(change("tasks", task.uid, defaultLabel(task.name, task.uid), field, before, normalized));
            return normalized;
        }
        return before;
    }

    private String applyAssignmentDateChange(List<ImportChange> changes, jp.igapyon.mikuproject.model.AssignmentModel assignment,
            String field, String before, Object rawValue, String kind) {
        String normalized = normalizeDateTime(rawValue, kind);
        if (normalized != null && !equalsText(before, normalized)) {
            changes.add(change("assignments", assignment.uid, assignment.uid, field, before, normalized));
            return normalized;
        }
        return before;
    }

    private String applyStringChange(List<ImportChange> changes, String scope, String uid, String label, String field,
            String before, String candidate, String fallback) {
        if (!isBlank(candidate) && !equalsText(before, candidate)) {
            changes.add(change(scope, uid, label, field, before, candidate));
            return candidate;
        }
        return fallback;
    }

    private Integer applyIntegerChange(List<ImportChange> changes, String scope, String uid, String label, String field,
            Integer before, Integer candidate) {
        if (candidate != null && !candidate.equals(before)) {
            changes.add(change(scope, uid, label, field, before, candidate));
            return candidate;
        }
        return before;
    }

    private Double applyDoubleChange(List<ImportChange> changes, String scope, String uid, String label, String field, Double before,
            Double candidate) {
        if (candidate != null && !candidate.equals(before)) {
            changes.add(change(scope, uid, label, field, before, candidate));
            return candidate;
        }
        return before;
    }

    private boolean applyBooleanChange(List<ImportChange> changes, String scope, String uid, String label, String field,
            boolean before, Boolean candidate, boolean fallback) {
        if (candidate != null && candidate.booleanValue() != before) {
            changes.add(change(scope, uid, label, field, Boolean.valueOf(before), candidate));
            return candidate.booleanValue();
        }
        return fallback;
    }

    private Boolean applyNullableBooleanChange(List<ImportChange> changes, String scope, String uid, String label, String field,
            Boolean before, Boolean candidate) {
        if (candidate != null && (before == null || candidate.booleanValue() != before.booleanValue())) {
            changes.add(change(scope, uid, label, field, before, candidate));
            return candidate;
        }
        return before;
    }

    private List<PredecessorModel> parsePredecessors(Object rawValue, List<PredecessorModel> fallback) {
        String text = stringValue(rawValue);
        if (isBlank(text)) {
            return fallback;
        }
        List<PredecessorModel> predecessors = new ArrayList<PredecessorModel>();
        for (String item : text.split(",")) {
            String trimmed = item.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            String uid = trimmed.replaceAll("\\(.*$", "").trim();
            PredecessorModel predecessor = new PredecessorModel();
            predecessor.predecessorUid = uid;
            predecessors.add(predecessor);
        }
        return predecessors;
    }

    private ProjectModel cloneModel(ProjectModel model) {
        MsProjectXml xml = new MsProjectXml();
        return xml.importFromXml(xml.exportToXml(model));
    }

    private TaskModel findTask(ProjectModel model, String uid) {
        for (TaskModel task : model.tasks) {
            if (safe(uid).equals(task.uid)) {
                return task;
            }
        }
        return null;
    }

    private ResourceModel findResource(ProjectModel model, String uid) {
        for (ResourceModel resource : model.resources) {
            if (safe(uid).equals(resource.uid)) {
                return resource;
            }
        }
        return null;
    }

    private jp.igapyon.mikuproject.model.AssignmentModel findAssignment(ProjectModel model, String uid) {
        for (jp.igapyon.mikuproject.model.AssignmentModel assignment : model.assignments) {
            if (safe(uid).equals(assignment.uid)) {
                return assignment;
            }
        }
        return null;
    }

    private CalendarModel findCalendar(ProjectModel model, String uid) {
        for (CalendarModel calendar : model.calendars) {
            if (safe(uid).equals(calendar.uid)) {
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

    private String normalizeDateTime(Object value, String kind) {
        String text = stringValue(value);
        if (isBlank(text)) {
            return null;
        }
        String normalized = text.trim().replace(' ', 'T');
        if (normalized.matches("^\\d{4}-\\d{2}-\\d{2}$")) {
            return normalized + ("start".equals(kind) ? "T09:00:00" : "T18:00:00");
        }
        return normalized;
    }

    private Boolean booleanValue(Object value) {
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        String raw = stringValue(value);
        if (raw == null) {
            return null;
        }
        String text = raw.trim();
        if ("○".equals(text) || "1".equals(text) || "true".equalsIgnoreCase(text)) {
            return Boolean.TRUE;
        }
        if ("ー".equals(text) || "0".equals(text) || "false".equalsIgnoreCase(text)) {
            return Boolean.FALSE;
        }
        return null;
    }

    private Integer integerValue(Object value) {
        if (value instanceof Number) {
            return Integer.valueOf(((Number) value).intValue());
        }
        try {
            return isBlank(stringValue(value)) ? null : Integer.valueOf((int) Math.floor(Double.parseDouble(stringValue(value))));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Double doubleValue(Object value) {
        if (value instanceof Number) {
            return Double.valueOf(((Number) value).doubleValue());
        }
        try {
            return isBlank(stringValue(value)) ? null : Double.valueOf(Double.parseDouble(stringValue(value)));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String coalesceString(String value, String fallback) {
        return isBlank(value) ? fallback : value;
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private boolean equalsText(String left, String right) {
        return left == null ? right == null : left.equals(right);
    }

    private String defaultLabel(String value, String fallback) {
        return isBlank(value) ? fallback : value;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    public static class ImportResult {
        public ProjectModel model;
        public List<ImportChange> changes;
        public List<WorkbookJsonWarning> warnings;
    }

    public static class ImportAsProjectModelResult {
        public ProjectModel model;
        public List<WorkbookJsonWarning> warnings;
    }
}
