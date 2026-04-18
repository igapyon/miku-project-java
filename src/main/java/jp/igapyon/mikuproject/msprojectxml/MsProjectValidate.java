/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuproject.msprojectxml;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jp.igapyon.mikuproject.model.AssignmentModel;
import jp.igapyon.mikuproject.model.CalendarModel;
import jp.igapyon.mikuproject.model.ProjectModel;
import jp.igapyon.mikuproject.model.ResourceModel;
import jp.igapyon.mikuproject.model.TaskModel;
import jp.igapyon.mikuproject.model.ValidationIssue;

public class MsProjectValidate {
    private final MsProjectValidateHelpers helpers = new MsProjectValidateHelpers();

    public List<ValidationIssue> validateProjectModel(ProjectModel model) {
        List<ValidationIssue> issues = new ArrayList<ValidationIssue>();
        Set<String> calendarUids = new HashSet<String>();
        Set<String> taskUids = new HashSet<String>();
        Set<String> taskIds = new HashSet<String>();
        Set<String> resourceUids = new HashSet<String>();
        Set<String> assignmentUids = new HashSet<String>();

        if (model == null || model.project == null) {
            issues.add(issue("error", "project", "Project が初期化されていません"));
            return issues;
        }

        if (isBlank(model.project.name)) {
            issues.add(issue("warning", "project", "Project Name が空です"));
        }
        if (model.project.saveVersion != null && model.project.saveVersion.intValue() < 0) {
            issues.add(issue("warning", "project", "Project SaveVersion は 0 以上が望ましいです"));
        }
        if (isBlank(model.project.startDate)) {
            issues.add(issue("warning", "project", "Project StartDate が空です"));
        }
        if (isBlank(model.project.finishDate)) {
            issues.add(issue("warning", "project", "Project FinishDate が空です"));
        }
        if (model.project.minutesPerDay != null && model.project.minutesPerDay.intValue() <= 0) {
            issues.add(issue("warning", "project", "Project MinutesPerDay は正の値が望ましいです"));
        }
        if (model.project.minutesPerWeek != null && model.project.minutesPerWeek.intValue() <= 0) {
            issues.add(issue("warning", "project", "Project MinutesPerWeek は正の値が望ましいです"));
        }
        if (model.project.daysPerMonth != null && model.project.daysPerMonth.intValue() <= 0) {
            issues.add(issue("warning", "project", "Project DaysPerMonth は正の値が望ましいです"));
        }
        if (model.project.weekStartDay != null
                && (model.project.weekStartDay.intValue() < 1 || model.project.weekStartDay.intValue() > 7)) {
            issues.add(issue("warning", "project", "Project WeekStartDay は 1..7 が望ましいです"));
        }
        if (model.project.workFormat != null && model.project.workFormat.intValue() < 0) {
            issues.add(issue("warning", "project", "Project WorkFormat は 0 以上が望ましいです"));
        }
        if (model.project.durationFormat != null && model.project.durationFormat.intValue() < 0) {
            issues.add(issue("warning", "project", "Project DurationFormat は 0 以上が望ましいです"));
        }
        if (model.project.currencyDigits != null && model.project.currencyDigits.intValue() < 0) {
            issues.add(issue("warning", "project", "Project CurrencyDigits は 0 以上が望ましいです"));
        }
        if (model.project.currencySymbolPosition != null && model.project.currencySymbolPosition.intValue() < 0) {
            issues.add(issue("warning", "project", "Project CurrencySymbolPosition は 0 以上が望ましいです"));
        }
        if (!isBlank(model.project.fyStartDate) && helpers.parseDateValue(model.project.fyStartDate) == null) {
            issues.add(issue("warning", "project", "Project FYStartDate の日付形式が解釈できません"));
        }
        if (model.project.criticalSlackLimit != null && model.project.criticalSlackLimit.intValue() < 0) {
            issues.add(issue("warning", "project", "Project CriticalSlackLimit は 0 以上が望ましいです"));
        }
        if (model.project.defaultTaskType != null && model.project.defaultTaskType.intValue() < 0) {
            issues.add(issue("warning", "project", "Project DefaultTaskType は 0 以上が望ましいです"));
        }
        if (model.project.defaultFixedCostAccrual != null && model.project.defaultFixedCostAccrual.intValue() < 0) {
            issues.add(issue("warning", "project", "Project DefaultFixedCostAccrual は 0 以上が望ましいです"));
        }
        if (model.project.defaultTaskEVMethod != null && model.project.defaultTaskEVMethod.intValue() < 0) {
            issues.add(issue("warning", "project", "Project DefaultTaskEVMethod は 0 以上が望ましいです"));
        }
        if (model.project.newTaskStartDate != null && model.project.newTaskStartDate.intValue() < 0) {
            issues.add(issue("warning", "project", "Project NewTaskStartDate は 0 以上が望ましいです"));
        }
        for (jp.igapyon.mikuproject.model.OutlineCodeModel outlineCode : model.project.outlineCodes) {
            if (isBlank(outlineCode.fieldID) && isBlank(outlineCode.fieldName)) {
                issues.add(issue("warning", "project", "Project OutlineCode は FieldID または FieldName を持つことが望ましいです"));
            }
            for (jp.igapyon.mikuproject.model.OutlineCodeMaskModel mask : outlineCode.masks) {
                if (mask.level != null && mask.level.intValue() < 1) {
                    issues.add(issue("warning", "project", "Project OutlineCode Mask Level は 1 以上が望ましいです"));
                }
            }
        }
        for (jp.igapyon.mikuproject.model.WbsMaskModel wbsMask : model.project.wbsMasks) {
            if (wbsMask.level != null && wbsMask.level.intValue() < 1) {
                issues.add(issue("warning", "project", "Project WBSMask Level は 1 以上が望ましいです"));
            }
        }
        for (jp.igapyon.mikuproject.model.ProjectExtendedAttributeModel attribute : model.project.extendedAttributes) {
            if (isBlank(attribute.fieldID) && isBlank(attribute.fieldName)) {
                issues.add(issue("warning", "project", "Project ExtendedAttribute は FieldID または FieldName を持つことが望ましいです"));
            }
            if (attribute.calculationType != null && attribute.calculationType.intValue() < 0) {
                issues.add(issue("warning", "project", "Project ExtendedAttribute CalculationType は 0 以上が望ましいです"));
            }
        }

        if (model.calendars != null) {
            for (CalendarModel calendar : model.calendars) {
                if (isBlank(calendar.uid)) {
                    issues.add(issue("error", "calendars", "Calendar UID が空です"));
                    continue;
                }
                if (!calendarUids.add(calendar.uid)) {
                    issues.add(issue("error", "calendars", "Calendar UID が重複しています: " + calendar.uid));
                }
                if (isBlank(calendar.name)) {
                    issues.add(issue("warning", "calendars",
                            "Calendar Name が空です: " + helpers.describeCalendar(calendar)));
                }
                if (calendar.isBaselineCalendar != null && calendar.isBaselineCalendar.booleanValue() && !calendar.isBaseCalendar) {
                    issues.add(issue("warning", "calendars",
                            "Calendar IsBaselineCalendar は通常 BaseCalendar と整合していることが望ましいです: "
                                    + helpers.describeCalendar(calendar)));
                }
                for (jp.igapyon.mikuproject.model.WeekDayModel weekDay : calendar.weekDays) {
                    if (weekDay.dayType != null
                            && (weekDay.dayType.intValue() < 1 || weekDay.dayType.intValue() > 7)) {
                        issues.add(issue("warning", "calendars",
                                "Calendar WeekDay DayType が 1..7 の範囲外です: " + helpers.describeCalendar(calendar)));
                    }
                    for (jp.igapyon.mikuproject.model.WorkingTimeModel workingTime : weekDay.workingTimes) {
                        if (isBlank(workingTime.fromTime) || isBlank(workingTime.toTime)) {
                            issues.add(issue("warning", "calendars",
                                    "Calendar WorkingTime の時刻が不足しています: " + helpers.describeCalendar(calendar)));
                        }
                    }
                }
                for (jp.igapyon.mikuproject.model.CalendarExceptionModel exception : calendar.exceptions) {
                    if (compareDateTime(exception.fromDate, exception.toDate) > 0) {
                        issues.add(issue("warning", "calendars",
                                "Calendar Exception FromDate が ToDate より後です: " + helpers.describeCalendar(calendar)));
                    }
                    for (jp.igapyon.mikuproject.model.WorkingTimeModel workingTime : exception.workingTimes) {
                        if (isBlank(workingTime.fromTime) || isBlank(workingTime.toTime)) {
                            issues.add(issue("warning", "calendars",
                                    "Calendar Exception WorkingTime の時刻が不足しています: "
                                            + helpers.describeCalendar(calendar)));
                        }
                    }
                }
                for (jp.igapyon.mikuproject.model.WorkWeekModel workWeek : calendar.workWeeks) {
                    if (compareDateTime(workWeek.fromDate, workWeek.toDate) > 0) {
                        issues.add(issue("warning", "calendars",
                                "Calendar WorkWeek FromDate が ToDate より後です: " + helpers.describeCalendar(calendar)));
                    }
                    for (jp.igapyon.mikuproject.model.WeekDayModel weekDay : workWeek.weekDays) {
                        if (weekDay.dayType != null
                                && (weekDay.dayType.intValue() < 1 || weekDay.dayType.intValue() > 7)) {
                            issues.add(issue("warning", "calendars",
                                    "Calendar WorkWeek DayType が 1..7 の範囲外です: "
                                            + helpers.describeCalendar(calendar)));
                        }
                    }
                }
            }
        }

        if (!isBlank(model.project.calendarUID) && !calendarUids.contains(model.project.calendarUID)) {
            issues.add(issue("error", "project", "Project CalendarUID が既存 Calendar を指していません: "
                    + model.project.calendarUID));
        }
        if (model.calendars != null) {
            for (CalendarModel calendar : model.calendars) {
                if (!isBlank(calendar.baseCalendarUID) && !calendarUids.contains(calendar.baseCalendarUID)) {
                    issues.add(issue("warning", "calendars",
                            "Calendar BaseCalendarUID が既存 Calendar を指していません: "
                                    + helpers.describeCalendar(calendar)));
                }
                if (!isBlank(calendar.baseCalendarUID) && calendar.baseCalendarUID.equals(calendar.uid)) {
                    issues.add(issue("warning", "calendars",
                            "Calendar BaseCalendarUID が自身を指しています: " + helpers.describeCalendar(calendar)));
                }
            }
        }

        if (model.tasks != null) {
            for (TaskModel task : model.tasks) {
                if (isBlank(task.uid)) {
                    issues.add(issue("error", "tasks", "Task UID が空です"));
                } else if (!taskUids.add(task.uid)) {
                    issues.add(issue("error", "tasks", "Task UID が重複しています: " + task.uid));
                }

                if (isBlank(task.id)) {
                    issues.add(issue("error", "tasks", "Task ID が空です: " + blankSafe(task.name)));
                } else if (!taskIds.add(task.id)) {
                    issues.add(issue("error", "tasks", "Task ID が重複しています: " + task.id));
                }

                if (isBlank(task.name) && !helpers.isPlaceholderUid(task.uid)) {
                    issues.add(issue("warning", "tasks", "Task Name が空です: " + helpers.describeTask(task)));
                }
                if (isBlank(task.start)) {
                    issues.add(issue("warning", "tasks", "Task Start が空です: " + helpers.describeTask(task)));
                }
                if (isBlank(task.finish)) {
                    issues.add(issue("warning", "tasks", "Task Finish が空です: " + helpers.describeTask(task)));
                }
                if (task.outlineLevel != null && task.outlineLevel.intValue() < 1 && !helpers.isPlaceholderUid(task.uid)) {
                    issues.add(issue("error", "tasks", "Task OutlineLevel が不正です: " + helpers.describeTask(task)));
                }
                if (!isBlank(task.outlineNumber) && task.outlineLevel != null) {
                    int outlineParts = task.outlineNumber.split("\\.").length;
                    if (outlineParts != task.outlineLevel.intValue() && !helpers.isPlaceholderUid(task.uid)) {
                        issues.add(issue("warning", "tasks",
                                "Task OutlineNumber と OutlineLevel の整合が取れていません: "
                                        + helpers.describeTask(task)));
                    }
                }
                if (task.percentComplete != null && (task.percentComplete.intValue() < 0 || task.percentComplete.intValue() > 100)) {
                    issues.add(issue("warning", "tasks",
                            "Task PercentComplete が 0..100 の範囲外です: " + helpers.describeTask(task)));
                }
                if (task.percentWorkComplete != null
                        && (task.percentWorkComplete.intValue() < 0 || task.percentWorkComplete.intValue() > 100)) {
                    issues.add(issue("warning", "tasks",
                            "Task PercentWorkComplete が 0..100 の範囲外です: " + helpers.describeTask(task)));
                }
                if (task.type != null && task.type.intValue() < 0) {
                    issues.add(issue("warning", "tasks", "Task Type は 0 以上が望ましいです: " + helpers.describeTask(task)));
                }
                if (task.priority != null
                        && (task.priority.intValue() < 0 || task.priority.intValue() > 1000)) {
                    issues.add(issue("warning", "tasks",
                            "Task Priority が 0..1000 の範囲外です: " + helpers.describeTask(task)));
                }
                if (task.constraintType != null && task.constraintType.intValue() < 0) {
                    issues.add(issue("warning", "tasks",
                            "Task ConstraintType は 0 以上が望ましいです: " + helpers.describeTask(task)));
                }
                if (task.cost != null && task.cost.doubleValue() < 0.0d) {
                    issues.add(issue("warning", "tasks", "Task Cost が負値です: " + helpers.describeTask(task)));
                }
                if (task.actualCost != null && task.actualCost.doubleValue() < 0.0d) {
                    issues.add(issue("warning", "tasks", "Task ActualCost が負値です: " + helpers.describeTask(task)));
                }
                if (task.remainingCost != null && task.remainingCost.doubleValue() < 0.0d) {
                    issues.add(issue("warning", "tasks", "Task RemainingCost が負値です: " + helpers.describeTask(task)));
                }
                if (compareDateTime(task.start, task.finish) > 0) {
                    issues.add(issue("warning", "tasks", "Task Start が Finish より後です: " + helpers.describeTask(task)));
                }
                if (compareDateTime(task.finish, task.deadline) > 0) {
                    issues.add(issue("warning", "tasks", "Task Finish が Deadline より後です: " + helpers.describeTask(task)));
                }
                if (compareDateTime(task.actualStart, task.actualFinish) > 0) {
                    issues.add(issue("warning", "tasks",
                            "Task ActualStart が ActualFinish より後です: " + helpers.describeTask(task)));
                }
                for (jp.igapyon.mikuproject.model.TaskExtendedAttributeModel attribute : task.extendedAttributes) {
                    if (isBlank(attribute.fieldID)) {
                        issues.add(issue("warning", "tasks",
                                "Task ExtendedAttribute に FieldID がありません: " + helpers.describeTask(task)));
                    }
                }
                for (jp.igapyon.mikuproject.model.TaskBaselineModel baseline : task.baselines) {
                    if (baseline.number != null && baseline.number.intValue() < 0) {
                        issues.add(issue("warning", "tasks",
                                "Task Baseline Number は 0 以上が望ましいです: " + helpers.describeTask(task)));
                    }
                    if (baseline.cost != null && baseline.cost.doubleValue() < 0.0d) {
                        issues.add(issue("warning", "tasks",
                                "Task Baseline Cost が負値です: " + helpers.describeTask(task)));
                    }
                    if (compareDateTime(baseline.start, baseline.finish) > 0) {
                        issues.add(issue("warning", "tasks",
                                "Task Baseline Start が Finish より後です: " + helpers.describeTask(task)));
                    }
                }
                for (jp.igapyon.mikuproject.model.TaskTimephasedDataModel timephasedData : task.timephasedData) {
                    if (timephasedData.type != null && timephasedData.type.intValue() < 0) {
                        issues.add(issue("warning", "tasks",
                                "Task TimephasedData Type は 0 以上が望ましいです: " + helpers.describeTask(task)));
                    }
                    if (timephasedData.unit != null && timephasedData.unit.intValue() < 0) {
                        issues.add(issue("warning", "tasks", "Task TimephasedData Unit は 0 以上が望ましいです: " + blankSafe(task.uid)));
                    }
                    if (compareDateTime(timephasedData.start, timephasedData.finish) > 0) {
                        issues.add(issue("warning", "tasks",
                                "Task TimephasedData Start が Finish より後です: " + blankSafe(task.uid)));
                    }
                }
                if (!isBlank(task.calendarUID) && !calendarUids.contains(task.calendarUID)) {
                    issues.add(issue("warning", "tasks",
                            "Task CalendarUID が既存 Calendar を指していません: " + helpers.describeTask(task)));
                }
                for (jp.igapyon.mikuproject.model.PredecessorModel predecessor : task.predecessors) {
                    if (isBlank(predecessor.predecessorUid) || !taskUids.contains(predecessor.predecessorUid)) {
                        issues.add(issue("error", "tasks",
                                "PredecessorUID が既存 Task を指していません: " + helpers.describeTask(task)
                                        + ", " + helpers.describeTaskRef(model, predecessor.predecessorUid)));
                    }
                }
            }
            TaskOrderIssue taskOrderIssue = detectTaskOrderIssue(model.tasks);
            if (taskOrderIssue != null) {
                issues.add(issue("warning", "tasks",
                        "Task の並び順が OutlineNumber 順と一致していない可能性があります: "
                                + helpers.describeTask(taskOrderIssue.current) + " (直前: "
                                + helpers.describeTask(taskOrderIssue.previous) + ")"));
            }
        }

        if (model.resources != null) {
            for (ResourceModel resource : model.resources) {
                if (isBlank(resource.uid)) {
                    issues.add(issue("error", "resources", "Resource UID が空です"));
                } else if (!resourceUids.add(resource.uid)) {
                    issues.add(issue("error", "resources", "Resource UID が重複しています: " + resource.uid));
                }

                if (isBlank(resource.id)) {
                    issues.add(issue("warning", "resources", "Resource ID が空です: " + blankSafe(resource.uid)));
                }
                if (isBlank(resource.name) && !helpers.isPlaceholderUid(resource.uid)) {
                    issues.add(issue("warning", "resources",
                            "Resource Name が空です: " + helpers.describeResource(resource)));
                }
                if (resource.maxUnits != null && resource.maxUnits.doubleValue() < 0.0d) {
                    issues.add(issue("warning", "resources",
                            "Resource MaxUnits は 0 以上が望ましいです: " + helpers.describeResource(resource)));
                }
                if (resource.type != null && resource.type.intValue() < 0) {
                    issues.add(issue("warning", "resources",
                            "Resource Type は 0 以上が望ましいです: " + helpers.describeResource(resource)));
                }
                if (resource.workGroup != null && resource.workGroup.intValue() < 0) {
                    issues.add(issue("warning", "resources",
                            "Resource WorkGroup は 0 以上が望ましいです: " + helpers.describeResource(resource)));
                }
                if (resource.standardRateFormat != null && resource.standardRateFormat.intValue() < 0) {
                    issues.add(issue("warning", "resources",
                            "Resource StandardRateFormat は 0 以上が望ましいです: " + helpers.describeResource(resource)));
                }
                if (resource.overtimeRateFormat != null && resource.overtimeRateFormat.intValue() < 0) {
                    issues.add(issue("warning", "resources",
                            "Resource OvertimeRateFormat は 0 以上が望ましいです: " + helpers.describeResource(resource)));
                }
                if (resource.costPerUse != null && resource.costPerUse.doubleValue() < 0.0d) {
                    issues.add(issue("warning", "resources",
                            "Resource CostPerUse は 0 以上が望ましいです: " + helpers.describeResource(resource)));
                }
                if (resource.cost != null && resource.cost.doubleValue() < 0.0d) {
                    issues.add(issue("warning", "resources", "Resource Cost が負値です: " + helpers.describeResource(resource)));
                }
                if (resource.actualCost != null && resource.actualCost.doubleValue() < 0.0d) {
                    issues.add(issue("warning", "resources",
                            "Resource ActualCost が負値です: " + helpers.describeResource(resource)));
                }
                if (resource.remainingCost != null && resource.remainingCost.doubleValue() < 0.0d) {
                    issues.add(issue("warning", "resources",
                            "Resource RemainingCost が負値です: " + helpers.describeResource(resource)));
                }
                if (resource.percentWorkComplete != null
                        && (resource.percentWorkComplete.intValue() < 0 || resource.percentWorkComplete.intValue() > 100)) {
                    issues.add(issue("warning", "resources",
                            "Resource PercentWorkComplete が 0..100 の範囲外です: " + helpers.describeResource(resource)));
                }
                for (jp.igapyon.mikuproject.model.ResourceExtendedAttributeModel attribute : resource.extendedAttributes) {
                    if (isBlank(attribute.fieldID)) {
                        issues.add(issue("warning", "resources",
                                "Resource ExtendedAttribute に FieldID がありません: " + helpers.describeResource(resource)));
                    }
                }
                for (jp.igapyon.mikuproject.model.ResourceBaselineModel baseline : resource.baselines) {
                    if (baseline.number != null && baseline.number.intValue() < 0) {
                        issues.add(issue("warning", "resources",
                                "Resource Baseline Number は 0 以上が望ましいです: " + helpers.describeResource(resource)));
                    }
                    if (baseline.cost != null && baseline.cost.doubleValue() < 0.0d) {
                        issues.add(issue("warning", "resources",
                                "Resource Baseline Cost が負値です: " + helpers.describeResource(resource)));
                    }
                    if (compareDateTime(baseline.start, baseline.finish) > 0) {
                        issues.add(issue("warning", "resources",
                                "Resource Baseline Start が Finish より後です: " + helpers.describeResource(resource)));
                    }
                }
                for (jp.igapyon.mikuproject.model.ResourceTimephasedDataModel timephasedData : resource.timephasedData) {
                    if (timephasedData.type != null && timephasedData.type.intValue() < 0) {
                        issues.add(issue("warning", "resources",
                                "Resource TimephasedData Type は 0 以上が望ましいです: " + helpers.describeResource(resource)));
                    }
                    if (timephasedData.unit != null && timephasedData.unit.intValue() < 0) {
                        issues.add(issue("warning", "resources", "Resource TimephasedData Unit は 0 以上が望ましいです: " + blankSafe(resource.uid)));
                    }
                    if (compareDateTime(timephasedData.start, timephasedData.finish) > 0) {
                        issues.add(issue("warning", "resources",
                                "Resource TimephasedData Start が Finish より後です: " + blankSafe(resource.uid)));
                    }
                }
                if (!isBlank(resource.calendarUID) && !calendarUids.contains(resource.calendarUID)) {
                    issues.add(issue("warning", "resources",
                            "Resource CalendarUID が既存 Calendar を指していません: " + helpers.describeResource(resource)));
                }
            }
        }

        if (model.assignments != null) {
            for (AssignmentModel assignment : model.assignments) {
                if (isBlank(assignment.uid)) {
                    issues.add(issue("warning", "assignments", "Assignment UID が空です"));
                } else if (!assignmentUids.add(assignment.uid)) {
                    issues.add(issue("error", "assignments", "Assignment UID が重複しています: " + assignment.uid));
                }

                if (isBlank(assignment.taskUid) || !taskUids.contains(assignment.taskUid)) {
                    issues.add(issue("error", "assignments",
                            "Assignment TaskUID が既存 Task を指していません: " + helpers.describeAssignment(assignment)
                                    + ", " + helpers.describeTaskRef(model, assignment.taskUid)));
                }

                if ((isBlank(assignment.resourceUid)
                        || !resourceUids.contains(assignment.resourceUid))
                        && !helpers.isUnassignedResourceUid(assignment.resourceUid)) {
                    issues.add(issue("error", "assignments",
                            "Assignment ResourceUID が既存 Resource を指していません: "
                                    + helpers.describeAssignment(assignment) + ", "
                                    + helpers.describeTaskRef(model, assignment.taskUid) + ", "
                                    + helpers.describeResourceRef(model, assignment.resourceUid)));
                }
                if (isBlank(assignment.start)) {
                    issues.add(issue("warning", "assignments",
                            "Assignment Start が空です: " + helpers.describeAssignment(assignment)));
                }
                if (isBlank(assignment.finish)) {
                    issues.add(issue("warning", "assignments",
                            "Assignment Finish が空です: " + helpers.describeAssignment(assignment)));
                }
                if (compareDateTime(assignment.start, assignment.finish) > 0) {
                    issues.add(issue("warning", "assignments",
                            "Assignment Start が Finish より後です: " + helpers.describeAssignment(assignment)));
                }

                if (assignment.units != null && assignment.units.doubleValue() < 0.0d) {
                    issues.add(issue("warning", "assignments",
                            "Assignment Units が負値です: " + helpers.describeAssignment(assignment)));
                }
                if (assignment.workContour != null && assignment.workContour.intValue() < 0) {
                    issues.add(issue("warning", "assignments",
                            "Assignment WorkContour は 0 以上が望ましいです: " + helpers.describeAssignment(assignment)));
                }
                if (assignment.cost != null && assignment.cost.doubleValue() < 0.0d) {
                    issues.add(issue("warning", "assignments",
                            "Assignment Cost が負値です: " + helpers.describeAssignment(assignment)));
                }
                if (assignment.actualCost != null && assignment.actualCost.doubleValue() < 0.0d) {
                    issues.add(issue("warning", "assignments",
                            "Assignment ActualCost が負値です: " + helpers.describeAssignment(assignment)));
                }
                if (assignment.remainingCost != null && assignment.remainingCost.doubleValue() < 0.0d) {
                    issues.add(issue("warning", "assignments",
                            "Assignment RemainingCost が負値です: " + helpers.describeAssignment(assignment)));
                }
                if (assignment.percentWorkComplete != null
                        && (assignment.percentWorkComplete.intValue() < 0 || assignment.percentWorkComplete.intValue() > 100)) {
                    issues.add(issue("warning", "assignments",
                            "Assignment PercentWorkComplete が 0..100 の範囲外です: "
                                    + helpers.describeAssignment(assignment)));
                }
                if (assignment.overtimeWork != null && assignment.overtimeWork.isEmpty()) {
                    issues.add(issue("warning", "assignments",
                            "Assignment OvertimeWork が空です: " + helpers.describeAssignment(assignment)));
                }
                if (assignment.actualOvertimeWork != null && assignment.actualOvertimeWork.isEmpty()) {
                    issues.add(issue("warning", "assignments",
                            "Assignment ActualOvertimeWork が空です: " + helpers.describeAssignment(assignment)));
                }
                if (assignment.startVariance != null && assignment.startVariance.isEmpty()) {
                    issues.add(issue("warning", "assignments",
                            "Assignment StartVariance が空です: " + helpers.describeAssignment(assignment)));
                }
                if (assignment.finishVariance != null && assignment.finishVariance.isEmpty()) {
                    issues.add(issue("warning", "assignments",
                            "Assignment FinishVariance が空です: " + helpers.describeAssignment(assignment)));
                }
                for (jp.igapyon.mikuproject.model.AssignmentExtendedAttributeModel attribute : assignment.extendedAttributes) {
                    if (isBlank(attribute.fieldID)) {
                        issues.add(issue("warning", "assignments",
                                "Assignment ExtendedAttribute に FieldID がありません: "
                                        + helpers.describeAssignment(assignment)));
                    }
                }
                for (jp.igapyon.mikuproject.model.AssignmentBaselineModel baseline : assignment.baselines) {
                    if (baseline.number != null && baseline.number.intValue() < 0) {
                        issues.add(issue("warning", "assignments",
                                "Assignment Baseline Number は 0 以上が望ましいです: "
                                        + helpers.describeAssignment(assignment)));
                    }
                    if (baseline.cost != null && baseline.cost.doubleValue() < 0.0d) {
                        issues.add(issue("warning", "assignments",
                                "Assignment Baseline Cost が負値です: " + helpers.describeAssignment(assignment)));
                    }
                    if (compareDateTime(baseline.start, baseline.finish) > 0) {
                        issues.add(issue("warning", "assignments",
                                "Assignment Baseline Start が Finish より後です: "
                                        + helpers.describeAssignment(assignment)));
                    }
                }
                for (jp.igapyon.mikuproject.model.AssignmentTimephasedDataModel timephasedData : assignment.timephasedData) {
                    if (timephasedData.type != null && timephasedData.type.intValue() < 0) {
                        issues.add(issue("warning", "assignments",
                                "Assignment TimephasedData Type は 0 以上が望ましいです: "
                                        + helpers.describeAssignment(assignment)));
                    }
                    if (timephasedData.unit != null && timephasedData.unit.intValue() < 0) {
                        issues.add(issue("warning", "assignments", "Assignment TimephasedData Unit は 0 以上が望ましいです: " + blankSafe(assignment.uid)));
                    }
                    if (compareDateTime(timephasedData.start, timephasedData.finish) > 0) {
                        issues.add(issue("warning", "assignments",
                                "Assignment TimephasedData Start が Finish より後です: " + blankSafe(assignment.uid)));
                    }
                }
            }
        }

        return issues;
    }

    private ValidationIssue issue(String level, String scope, String message) {
        ValidationIssue issue = new ValidationIssue();
        issue.level = level;
        issue.scope = scope;
        issue.message = message;
        return issue;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String blankSafe(String value) {
        return isBlank(value) ? "(未設定)" : value;
    }

    private int compareDateTime(String left, String right) {
        Long leftValue = helpers.parseDateValue(left);
        Long rightValue = helpers.parseDateValue(right);
        if (leftValue == null || rightValue == null) {
            return 0;
        }
        return leftValue.compareTo(rightValue);
    }

    private TaskOrderIssue detectTaskOrderIssue(List<TaskModel> tasks) {
        MsProjectValidateHelpers.TaskOrderIssue helperIssue = helpers.detectTaskOrderIssue(tasks);
        if (helperIssue == null) {
            return null;
        }
        TaskOrderIssue issue = new TaskOrderIssue();
        issue.previous = helperIssue.previous;
        issue.current = helperIssue.current;
        return issue;
    }

    private static class TaskOrderIssue {
        private TaskModel previous;
        private TaskModel current;
    }
}
