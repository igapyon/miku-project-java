/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuproject.projectworkbookjson;

public class ProjectWorkbookSchema {
    public static final String[] SHEET_NAMES = { "Project", "Tasks", "Resources", "Assignments", "Calendars",
            "NonWorkingDays" };
    public static final Integer HEADER_ROW_INDEX = Integer.valueOf(2);
    public static final Integer DATA_ROW_START_INDEX = Integer.valueOf(3);
    public static final String[] PROJECT_FIELD_ORDER = { "Name", "Title", "Author", "Company", "StartDate", "FinishDate",
            "CurrentDate", "StatusDate", "CalendarUID", "MinutesPerDay", "MinutesPerWeek", "DaysPerMonth",
            "ScheduleFromStart", "OutlineCodes", "WBSMasks", "ExtendedAttributes" };
    public static final String[] PROJECT_EDITABLE_FIELDS = { "Name", "Title", "Author", "Company", "StartDate",
            "FinishDate", "CurrentDate", "StatusDate", "CalendarUID", "MinutesPerDay", "MinutesPerWeek", "DaysPerMonth",
            "ScheduleFromStart" };
    public static final String[] TASK_HEADERS = { "UID", "ID", "Name", "OutlineLevel", "OutlineNumber", "WBS", "Start",
            "Finish", "Duration", "PercentComplete", "PercentWorkComplete", "Milestone", "Summary", "Critical",
            "CalendarUID", "Predecessors", "Notes" };
    public static final String[] RESOURCE_HEADERS = { "UID", "ID", "Name", "Type", "Initials", "Group", "MaxUnits",
            "CalendarUID", "StandardRate", "OvertimeRate", "CostPerUse", "Work", "ActualWork", "RemainingWork" };
    public static final String[] ASSIGNMENT_HEADERS = { "UID", "TaskUID", "TaskName", "ResourceUID", "ResourceName", "Start",
            "Finish", "Units", "Work", "ActualWork", "RemainingWork", "PercentWorkComplete" };
    public static final String[] CALENDAR_HEADERS = { "UID", "Name", "IsBaseCalendar", "BaseCalendarUID", "WeekDays",
            "Exceptions", "WorkWeeks" };
    public static final String[] NON_WORKING_DAYS_HEADERS = { "CalendarUID", "Index", "CalendarName", "Name", "Date",
            "FromDate", "ToDate", "DayWorking" };

    private ProjectWorkbookSchema() {
    }
}
