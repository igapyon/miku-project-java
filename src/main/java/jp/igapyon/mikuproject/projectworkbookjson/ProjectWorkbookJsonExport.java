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
            row.put("UID", workbookCellValue(task.uid));
            row.put("ID", workbookCellValue(task.id));
            row.put("Name", workbookCellValue(task.name));
            row.put("OutlineLevel", workbookCellValue(task.outlineLevel));
            row.put("OutlineNumber", workbookCellValue(task.outlineNumber));
            row.put("WBS", workbookCellValue(task.wbs));
            row.put("Start", workbookCellValue(task.start));
            row.put("Finish", workbookCellValue(task.finish));
            row.put("Duration", workbookCellValue(task.duration));
            row.put("PercentComplete", workbookCellValue(task.percentComplete));
            row.put("PercentWorkComplete", workbookCellValue(task.percentWorkComplete));
            row.put("Milestone", formatBooleanSymbol(task.milestone));
            row.put("Summary", formatBooleanSymbol(task.summary));
            row.put("Critical", task.critical == null ? null : formatBooleanSymbol(task.critical.booleanValue()));
            row.put("Type", workbookCellValue(task.type));
            row.put("Priority", workbookCellValue(task.priority));
            row.put("CalendarUID", workbookCellValue(task.calendarUID));
            row.put("ConstraintType", workbookCellValue(task.constraintType));
            row.put("ConstraintDate", workbookCellValue(task.constraintDate));
            row.put("Deadline", workbookCellValue(task.deadline));
            row.put("Predecessors", formatPredecessors(task));
            row.put("Notes", workbookCellValue(task.notes));
            document.ensureSheet("Tasks").add(row);
        }
    }

    private void exportResourcesSheet(WorkbookJsonDocument document, ProjectModel model) {
        if (model.resources.isEmpty()) {
            document.ensureSheet("Resources").add(emptyRow(ProjectWorkbookSchema.RESOURCE_HEADERS));
            return;
        }
        for (ResourceModel resource : model.resources) {
            Map<String, Object> row = new LinkedHashMap<String, Object>();
            row.put("UID", workbookCellValue(resource.uid));
            row.put("ID", workbookCellValue(resource.id));
            row.put("Name", workbookCellValue(resource.name));
            row.put("Type", workbookCellValue(resource.type));
            row.put("Initials", workbookCellValue(resource.initials));
            row.put("Group", workbookCellValue(resource.group));
            row.put("MaxUnits", workbookCellValue(resource.maxUnits));
            row.put("CalendarUID", workbookCellValue(resource.calendarUID));
            row.put("StandardRate", workbookCellValue(resource.standardRate));
            row.put("OvertimeRate", workbookCellValue(resource.overtimeRate));
            row.put("CostPerUse", workbookCellValue(resource.costPerUse));
            row.put("Work", workbookCellValue(resource.work));
            row.put("ActualWork", workbookCellValue(resource.actualWork));
            row.put("RemainingWork", workbookCellValue(resource.remainingWork));
            row.put("Cost", workbookCellValue(resource.cost));
            row.put("ActualCost", workbookCellValue(resource.actualCost));
            row.put("RemainingCost", workbookCellValue(resource.remainingCost));
            row.put("PercentWorkComplete", workbookCellValue(resource.percentWorkComplete));
            row.put("WorkGroup", workbookCellValue(resource.workGroup));
            row.put("StandardRateFormat", workbookCellValue(resource.standardRateFormat));
            row.put("OvertimeRateFormat", workbookCellValue(resource.overtimeRateFormat));
            document.ensureSheet("Resources").add(row);
        }
    }

    private void exportAssignmentsSheet(WorkbookJsonDocument document, ProjectModel model) {
        if (model.assignments.isEmpty()) {
            document.ensureSheet("Assignments").add(emptyRow(ProjectWorkbookSchema.ASSIGNMENT_HEADERS));
            return;
        }
        for (AssignmentModel assignment : model.assignments) {
            Map<String, Object> row = new LinkedHashMap<String, Object>();
            row.put("UID", workbookCellValue(assignment.uid));
            row.put("TaskUID", workbookCellValue(assignment.taskUid));
            row.put("TaskName", workbookCellValue(resolveTaskName(model, assignment.taskUid)));
            row.put("ResourceUID", workbookCellValue(assignment.resourceUid));
            row.put("ResourceName", workbookCellValue(resolveResourceName(model, assignment.resourceUid)));
            row.put("Start", workbookCellValue(assignment.start));
            row.put("Finish", workbookCellValue(assignment.finish));
            row.put("StartVariance", workbookCellValue(assignment.startVariance));
            row.put("FinishVariance", workbookCellValue(assignment.finishVariance));
            row.put("Delay", workbookCellValue(assignment.delay));
            row.put("Milestone", assignment.milestone == null ? null : formatBooleanSymbol(assignment.milestone.booleanValue()));
            row.put("WorkContour", workbookCellValue(assignment.workContour));
            row.put("Units", workbookCellValue(assignment.units));
            row.put("Work", workbookCellValue(assignment.work));
            row.put("Cost", workbookCellValue(assignment.cost));
            row.put("ActualWork", workbookCellValue(assignment.actualWork));
            row.put("RemainingWork", workbookCellValue(assignment.remainingWork));
            row.put("ActualCost", workbookCellValue(assignment.actualCost));
            row.put("RemainingCost", workbookCellValue(assignment.remainingCost));
            row.put("OvertimeWork", workbookCellValue(assignment.overtimeWork));
            row.put("ActualOvertimeWork", workbookCellValue(assignment.actualOvertimeWork));
            row.put("PercentWorkComplete", workbookCellValue(assignment.percentWorkComplete));
            document.ensureSheet("Assignments").add(row);
        }
    }

    private void exportCalendarsSheet(WorkbookJsonDocument document, ProjectModel model) {
        for (CalendarModel calendar : model.calendars) {
            Map<String, Object> row = new LinkedHashMap<String, Object>();
            row.put("UID", workbookCellValue(calendar.uid));
            row.put("Name", workbookCellValue(calendar.name));
            row.put("IsBaseCalendar", formatBooleanSymbol(calendar.isBaseCalendar));
            row.put("BaseCalendarUID", workbookCellValue(calendar.baseCalendarUID));
            row.put("WeekDays", workbookCellValue(Integer.valueOf(calendar.weekDays.size())));
            row.put("Exceptions", workbookCellValue(Integer.valueOf(calendar.exceptions.size())));
            row.put("WorkWeeks", workbookCellValue(Integer.valueOf(calendar.workWeeks.size())));
            document.ensureSheet("Calendars").add(row);
        }
    }

    private void exportNonWorkingDaysSheet(WorkbookJsonDocument document, ProjectModel model) {
        for (CalendarModel calendar : model.calendars) {
            for (int index = 0; index < calendar.exceptions.size(); index++) {
                CalendarExceptionModel exception = calendar.exceptions.get(index);
                Map<String, Object> row = new LinkedHashMap<String, Object>();
                row.put("CalendarUID", workbookCellValue(calendar.uid));
                row.put("Index", workbookCellValue(Integer.valueOf(index)));
                row.put("CalendarName", workbookCellValue(calendar.name));
                row.put("Name", workbookCellValue(exception.name));
                row.put("Date", workbookCellValue(formatExceptionDate(exception)));
                row.put("FromDate", workbookCellValue(formatExceptionBoundaryDate(exception.fromDate)));
                row.put("ToDate", workbookCellValue(formatExceptionBoundaryDate(exception.toDate)));
                row.put("DayWorking", exception.dayWorking == null ? null : formatBooleanSymbol(exception.dayWorking.booleanValue()));
                document.ensureSheet("NonWorkingDays").add(row);
            }
        }
    }

    private Object projectFieldValue(ProjectModel model, String field) {
        if ("Name".equals(field)) return workbookCellValue(model.project.name);
        if ("Title".equals(field)) return workbookCellValue(model.project.title);
        if ("Author".equals(field)) return workbookCellValue(model.project.author);
        if ("Company".equals(field)) return workbookCellValue(model.project.company);
        if ("StartDate".equals(field)) return workbookCellValue(model.project.startDate);
        if ("FinishDate".equals(field)) return workbookCellValue(model.project.finishDate);
        if ("CurrentDate".equals(field)) return workbookCellValue(model.project.currentDate);
        if ("StatusDate".equals(field)) return workbookCellValue(model.project.statusDate);
        if ("CalendarUID".equals(field)) return workbookCellValue(model.project.calendarUID);
        if ("MinutesPerDay".equals(field)) return workbookCellValue(model.project.minutesPerDay);
        if ("MinutesPerWeek".equals(field)) return workbookCellValue(model.project.minutesPerWeek);
        if ("DaysPerMonth".equals(field)) return workbookCellValue(model.project.daysPerMonth);
        if ("ScheduleFromStart".equals(field)) return formatBooleanSymbol(model.project.scheduleFromStart);
        if ("OutlineCodes".equals(field)) return workbookCellValue(Integer.valueOf(model.project.outlineCodes.size()));
        if ("WBSMasks".equals(field)) return workbookCellValue(Integer.valueOf(model.project.wbsMasks.size()));
        if ("ExtendedAttributes".equals(field)) return workbookCellValue(Integer.valueOf(model.project.extendedAttributes.size()));
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

    private Map<String, Object> emptyRow(String[] headers) {
        Map<String, Object> row = new LinkedHashMap<String, Object>();
        for (String header : headers) {
            row.put(header, null);
        }
        return row;
    }

    private Object workbookCellValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Boolean) {
            return formatBooleanSymbol(((Boolean) value).booleanValue());
        }
        return String.valueOf(value);
    }

    private String formatExceptionDate(CalendarExceptionModel exception) {
        String from = formatExceptionBoundaryDate(exception.fromDate);
        String to = formatExceptionBoundaryDate(exception.toDate);
        return from != null && from.equals(to) ? from : null;
    }

    private String formatExceptionBoundaryDate(String value) {
        return value == null ? null : value.length() <= 10 ? value : value.substring(0, 10);
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
