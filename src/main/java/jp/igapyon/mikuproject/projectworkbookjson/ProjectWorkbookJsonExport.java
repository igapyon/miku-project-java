/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuproject.projectworkbookjson;

import java.util.LinkedHashMap;
import java.util.Map;

import jp.igapyon.mikuproject.model.AssignmentModel;
import jp.igapyon.mikuproject.model.CalendarExceptionModel;
import jp.igapyon.mikuproject.model.CalendarModel;
import jp.igapyon.mikuproject.model.PredecessorModel;
import jp.igapyon.mikuproject.model.ProjectModel;
import jp.igapyon.mikuproject.model.ResourceModel;
import jp.igapyon.mikuproject.model.TaskModel;

public class ProjectWorkbookJsonExport {
    public WorkbookJsonDocument exportProjectWorkbookJson(ProjectModel model) {
        WorkbookJsonDocument document = new WorkbookJsonDocument();
        for (String sheetName : ProjectWorkbookSchema.SHEET_NAMES) {
            document.ensureSheet(sheetName);
        }
        exportProjectSheet(document, model);
        exportTasksSheet(document, model);
        exportResourcesSheet(document, model);
        exportAssignmentsSheet(document, model);
        exportCalendarsSheet(document, model);
        exportNonWorkingDaysSheet(document, model);
        return document;
    }

    private void exportProjectSheet(WorkbookJsonDocument document, ProjectModel model) {
        for (String field : ProjectWorkbookSchema.PROJECT_FIELD_ORDER) {
            Map<String, Object> row = new LinkedHashMap<String, Object>();
            row.put("Field", field);
            row.put("Value", projectFieldValue(model, field));
            document.ensureSheet("Project").add(row);
        }
    }

    private void exportTasksSheet(WorkbookJsonDocument document, ProjectModel model) {
        for (TaskModel task : model.tasks) {
            Map<String, Object> row = new LinkedHashMap<String, Object>();
            row.put("UID", task.uid);
            row.put("ID", task.id);
            row.put("Name", task.name);
            row.put("OutlineLevel", task.outlineLevel);
            row.put("OutlineNumber", task.outlineNumber);
            row.put("WBS", task.wbs);
            row.put("Start", task.start);
            row.put("Finish", task.finish);
            row.put("Duration", task.duration);
            row.put("PercentComplete", task.percentComplete);
            row.put("PercentWorkComplete", task.percentWorkComplete);
            row.put("Milestone", formatBooleanSymbol(task.milestone));
            row.put("Summary", formatBooleanSymbol(task.summary));
            row.put("Critical", task.critical == null ? null : formatBooleanSymbol(task.critical.booleanValue()));
            row.put("Type", task.type);
            row.put("Priority", task.priority);
            row.put("CalendarUID", task.calendarUID);
            row.put("ConstraintType", task.constraintType);
            row.put("ConstraintDate", task.constraintDate);
            row.put("Deadline", task.deadline);
            row.put("Predecessors", formatPredecessors(task));
            row.put("Notes", task.notes);
            document.ensureSheet("Tasks").add(row);
        }
    }

    private void exportResourcesSheet(WorkbookJsonDocument document, ProjectModel model) {
        for (ResourceModel resource : model.resources) {
            Map<String, Object> row = new LinkedHashMap<String, Object>();
            row.put("UID", resource.uid);
            row.put("ID", resource.id);
            row.put("Name", resource.name);
            row.put("Type", resource.type);
            row.put("Initials", resource.initials);
            row.put("Group", resource.group);
            row.put("MaxUnits", resource.maxUnits);
            row.put("CalendarUID", resource.calendarUID);
            row.put("StandardRate", resource.standardRate);
            row.put("OvertimeRate", resource.overtimeRate);
            row.put("CostPerUse", resource.costPerUse);
            row.put("Work", resource.work);
            row.put("ActualWork", resource.actualWork);
            row.put("RemainingWork", resource.remainingWork);
            row.put("Cost", resource.cost);
            row.put("ActualCost", resource.actualCost);
            row.put("RemainingCost", resource.remainingCost);
            row.put("PercentWorkComplete", resource.percentWorkComplete);
            row.put("WorkGroup", resource.workGroup);
            row.put("StandardRateFormat", resource.standardRateFormat);
            row.put("OvertimeRateFormat", resource.overtimeRateFormat);
            document.ensureSheet("Resources").add(row);
        }
    }

    private void exportAssignmentsSheet(WorkbookJsonDocument document, ProjectModel model) {
        for (AssignmentModel assignment : model.assignments) {
            Map<String, Object> row = new LinkedHashMap<String, Object>();
            row.put("UID", assignment.uid);
            row.put("TaskUID", assignment.taskUid);
            row.put("TaskName", resolveTaskName(model, assignment.taskUid));
            row.put("ResourceUID", assignment.resourceUid);
            row.put("ResourceName", resolveResourceName(model, assignment.resourceUid));
            row.put("Start", assignment.start);
            row.put("Finish", assignment.finish);
            row.put("StartVariance", assignment.startVariance);
            row.put("FinishVariance", assignment.finishVariance);
            row.put("Delay", assignment.delay);
            row.put("Milestone", assignment.milestone == null ? null : formatBooleanSymbol(assignment.milestone.booleanValue()));
            row.put("WorkContour", assignment.workContour);
            row.put("Units", assignment.units);
            row.put("Work", assignment.work);
            row.put("Cost", assignment.cost);
            row.put("ActualWork", assignment.actualWork);
            row.put("RemainingWork", assignment.remainingWork);
            row.put("ActualCost", assignment.actualCost);
            row.put("RemainingCost", assignment.remainingCost);
            row.put("OvertimeWork", assignment.overtimeWork);
            row.put("ActualOvertimeWork", assignment.actualOvertimeWork);
            row.put("PercentWorkComplete", assignment.percentWorkComplete);
            document.ensureSheet("Assignments").add(row);
        }
    }

    private void exportCalendarsSheet(WorkbookJsonDocument document, ProjectModel model) {
        for (CalendarModel calendar : model.calendars) {
            Map<String, Object> row = new LinkedHashMap<String, Object>();
            row.put("UID", calendar.uid);
            row.put("Name", calendar.name);
            row.put("IsBaseCalendar", formatBooleanSymbol(calendar.isBaseCalendar));
            row.put("BaseCalendarUID", calendar.baseCalendarUID);
            row.put("WeekDays", Integer.valueOf(calendar.weekDays.size()));
            row.put("Exceptions", Integer.valueOf(calendar.exceptions.size()));
            row.put("WorkWeeks", Integer.valueOf(calendar.workWeeks.size()));
            document.ensureSheet("Calendars").add(row);
        }
    }

    private void exportNonWorkingDaysSheet(WorkbookJsonDocument document, ProjectModel model) {
        for (CalendarModel calendar : model.calendars) {
            for (int index = 0; index < calendar.exceptions.size(); index++) {
                CalendarExceptionModel exception = calendar.exceptions.get(index);
                Map<String, Object> row = new LinkedHashMap<String, Object>();
                row.put("CalendarUID", calendar.uid);
                row.put("Index", Integer.valueOf(index));
                row.put("CalendarName", calendar.name);
                row.put("Name", exception.name);
                row.put("Date", exception.fromDate != null && exception.fromDate.equals(exception.toDate) ? exception.fromDate : null);
                row.put("FromDate", exception.fromDate);
                row.put("ToDate", exception.toDate);
                row.put("DayWorking", exception.dayWorking == null ? null : formatBooleanSymbol(exception.dayWorking.booleanValue()));
                document.ensureSheet("NonWorkingDays").add(row);
            }
        }
    }

    private Object projectFieldValue(ProjectModel model, String field) {
        if ("Name".equals(field)) return model.project.name;
        if ("Title".equals(field)) return model.project.title;
        if ("Author".equals(field)) return model.project.author;
        if ("Company".equals(field)) return model.project.company;
        if ("StartDate".equals(field)) return model.project.startDate;
        if ("FinishDate".equals(field)) return model.project.finishDate;
        if ("CurrentDate".equals(field)) return model.project.currentDate;
        if ("StatusDate".equals(field)) return model.project.statusDate;
        if ("CalendarUID".equals(field)) return model.project.calendarUID;
        if ("MinutesPerDay".equals(field)) return model.project.minutesPerDay;
        if ("MinutesPerWeek".equals(field)) return model.project.minutesPerWeek;
        if ("DaysPerMonth".equals(field)) return model.project.daysPerMonth;
        if ("ScheduleFromStart".equals(field)) return formatBooleanSymbol(model.project.scheduleFromStart);
        if ("OutlineCodes".equals(field)) return Integer.valueOf(model.project.outlineCodes.size());
        if ("WBSMasks".equals(field)) return Integer.valueOf(model.project.wbsMasks.size());
        if ("ExtendedAttributes".equals(field)) return Integer.valueOf(model.project.extendedAttributes.size());
        return null;
    }

    private String formatPredecessors(TaskModel task) {
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < task.predecessors.size(); index++) {
            PredecessorModel predecessor = task.predecessors.get(index);
            if (index > 0) {
                builder.append(", ");
            }
            builder.append(predecessor.predecessorUid);
        }
        return builder.length() == 0 ? null : builder.toString();
    }

    private String resolveTaskName(ProjectModel model, String taskUid) {
        for (TaskModel task : model.tasks) {
            if (safe(taskUid).equals(task.uid)) {
                return task.name;
            }
        }
        return null;
    }

    private String resolveResourceName(ProjectModel model, String resourceUid) {
        for (ResourceModel resource : model.resources) {
            if (safe(resourceUid).equals(resource.uid)) {
                return resource.name;
            }
        }
        return null;
    }

    private String formatBooleanSymbol(boolean value) {
        return value ? "○" : "ー";
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
