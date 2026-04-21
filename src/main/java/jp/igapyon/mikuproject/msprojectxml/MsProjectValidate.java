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

        addWarningIfBlank(issues, "project", model.project.name, "Project Name が空です");
        addWarningIfNegativeInteger(issues, "project", model.project.saveVersion, "Project SaveVersion は 0 以上が望ましいです");
        addWarningIfBlank(issues, "project", model.project.startDate, "Project StartDate が空です");
        addWarningIfBlank(issues, "project", model.project.finishDate, "Project FinishDate が空です");
        addWarningIfNonPositiveInteger(issues, "project", model.project.minutesPerDay,
                "Project MinutesPerDay は正の値が望ましいです");
        addWarningIfNonPositiveInteger(issues, "project", model.project.minutesPerWeek,
                "Project MinutesPerWeek は正の値が望ましいです");
        addWarningIfNonPositiveInteger(issues, "project", model.project.daysPerMonth,
                "Project DaysPerMonth は正の値が望ましいです");
        addWarningIfOutOfRangeInteger(issues, "project", model.project.weekStartDay, 1, 7,
                "Project WeekStartDay は 1..7 が望ましいです");
        addWarningIfNegativeInteger(issues, "project", model.project.workFormat, "Project WorkFormat は 0 以上が望ましいです");
        addWarningIfNegativeInteger(issues, "project", model.project.durationFormat,
                "Project DurationFormat は 0 以上が望ましいです");
        addWarningIfNegativeInteger(issues, "project", model.project.currencyDigits,
                "Project CurrencyDigits は 0 以上が望ましいです");
        addWarningIfNegativeInteger(issues, "project", model.project.currencySymbolPosition,
                "Project CurrencySymbolPosition は 0 以上が望ましいです");
        if (!isBlank(model.project.fyStartDate) && helpers.parseDateValue(model.project.fyStartDate) == null) {
            issues.add(issue("warning", "project", "Project FYStartDate の日付形式が解釈できません"));
        }
        addWarningIfNegativeInteger(issues, "project", model.project.criticalSlackLimit,
                "Project CriticalSlackLimit は 0 以上が望ましいです");
        addWarningIfNegativeInteger(issues, "project", model.project.defaultTaskType,
                "Project DefaultTaskType は 0 以上が望ましいです");
        addWarningIfNegativeInteger(issues, "project", model.project.defaultFixedCostAccrual,
                "Project DefaultFixedCostAccrual は 0 以上が望ましいです");
        addWarningIfNegativeInteger(issues, "project", model.project.defaultTaskEVMethod,
                "Project DefaultTaskEVMethod は 0 以上が望ましいです");
        addWarningIfNegativeInteger(issues, "project", model.project.newTaskStartDate,
                "Project NewTaskStartDate は 0 以上が望ましいです");
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
                String taskLabel = helpers.describeTask(task);
                addWarningIfOutOfRangeInteger(issues, "tasks", task.percentComplete, 0, 100,
                        "Task PercentComplete が 0..100 の範囲外です: " + taskLabel);
                addWarningIfOutOfRangeInteger(issues, "tasks", task.percentWorkComplete, 0, 100,
                        "Task PercentWorkComplete が 0..100 の範囲外です: " + taskLabel);
                addWarningIfNegativeInteger(issues, "tasks", task.type, "Task Type は 0 以上が望ましいです: " + taskLabel);
                addWarningIfOutOfRangeInteger(issues, "tasks", task.priority, 0, 1000,
                        "Task Priority が 0..1000 の範囲外です: " + taskLabel);
                addWarningIfNegativeInteger(issues, "tasks", task.constraintType,
                        "Task ConstraintType は 0 以上が望ましいです: " + taskLabel);
                addWarningIfNegativeDouble(issues, "tasks", task.cost, "Task Cost が負値です: " + taskLabel);
                addWarningIfNegativeDouble(issues, "tasks", task.actualCost, "Task ActualCost が負値です: " + taskLabel);
                addWarningIfNegativeDouble(issues, "tasks", task.remainingCost, "Task RemainingCost が負値です: " + taskLabel);
                addWarningIfDateAfter(issues, "tasks", task.start, task.finish, "Task Start が Finish より後です: " + taskLabel);
                addWarningIfDateAfter(issues, "tasks", task.finish, task.deadline, "Task Finish が Deadline より後です: " + taskLabel);
                addWarningIfDateAfter(issues, "tasks", task.actualStart, task.actualFinish,
                        "Task ActualStart が ActualFinish より後です: " + taskLabel);
                for (jp.igapyon.mikuproject.model.TaskExtendedAttributeModel attribute : task.extendedAttributes) {
                    if (isBlank(attribute.fieldID)) {
                        issues.add(issue("warning", "tasks",
                                "Task ExtendedAttribute に FieldID がありません: " + helpers.describeTask(task)));
                    }
                }
                validateTaskBaselines(issues, task, taskLabel);
                validateTaskTimephasedData(issues, task);
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
            ZeroDurationClusterIssue zeroDurationClusterIssue = detectZeroDurationClusterIssue(model.tasks);
            if (zeroDurationClusterIssue != null) {
                issues.add(issue("warning", "tasks",
                        "複数 task が同一の start / finish に寄った zero duration 入力です。工程が横方向に展開されない可能性があります: tasks="
                                + zeroDurationClusterIssue.taskCount + ", start=" + blankSafe(zeroDurationClusterIssue.start)
                                + ", finish=" + blankSafe(zeroDurationClusterIssue.finish)));
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
                String resourceLabel = helpers.describeResource(resource);
                addWarningIfNegativeDouble(issues, "resources", resource.maxUnits,
                        "Resource MaxUnits は 0 以上が望ましいです: " + resourceLabel);
                addWarningIfNegativeInteger(issues, "resources", resource.type,
                        "Resource Type は 0 以上が望ましいです: " + resourceLabel);
                addWarningIfNegativeInteger(issues, "resources", resource.workGroup,
                        "Resource WorkGroup は 0 以上が望ましいです: " + resourceLabel);
                addWarningIfNegativeInteger(issues, "resources", resource.standardRateFormat,
                        "Resource StandardRateFormat は 0 以上が望ましいです: " + resourceLabel);
                addWarningIfNegativeInteger(issues, "resources", resource.overtimeRateFormat,
                        "Resource OvertimeRateFormat は 0 以上が望ましいです: " + resourceLabel);
                addWarningIfNegativeDouble(issues, "resources", resource.costPerUse,
                        "Resource CostPerUse は 0 以上が望ましいです: " + resourceLabel);
                addWarningIfNegativeDouble(issues, "resources", resource.cost, "Resource Cost が負値です: " + resourceLabel);
                addWarningIfNegativeDouble(issues, "resources", resource.actualCost,
                        "Resource ActualCost が負値です: " + resourceLabel);
                addWarningIfNegativeDouble(issues, "resources", resource.remainingCost,
                        "Resource RemainingCost が負値です: " + resourceLabel);
                addWarningIfOutOfRangeInteger(issues, "resources", resource.percentWorkComplete, 0, 100,
                        "Resource PercentWorkComplete が 0..100 の範囲外です: " + resourceLabel);
                for (jp.igapyon.mikuproject.model.ResourceExtendedAttributeModel attribute : resource.extendedAttributes) {
                    if (isBlank(attribute.fieldID)) {
                        issues.add(issue("warning", "resources",
                                "Resource ExtendedAttribute に FieldID がありません: " + helpers.describeResource(resource)));
                    }
                }
                validateResourceBaselines(issues, resource, resourceLabel);
                validateResourceTimephasedData(issues, resource);
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
                String assignmentLabel = helpers.describeAssignment(assignment);
                addWarningIfDateAfter(issues, "assignments", assignment.start, assignment.finish,
                        "Assignment Start が Finish より後です: " + assignmentLabel);

                addWarningIfNegativeDouble(issues, "assignments", assignment.units,
                        "Assignment Units が負値です: " + assignmentLabel);
                addWarningIfNegativeInteger(issues, "assignments", assignment.workContour,
                        "Assignment WorkContour は 0 以上が望ましいです: " + assignmentLabel);
                addWarningIfNegativeDouble(issues, "assignments", assignment.cost,
                        "Assignment Cost が負値です: " + assignmentLabel);
                addWarningIfNegativeDouble(issues, "assignments", assignment.actualCost,
                        "Assignment ActualCost が負値です: " + assignmentLabel);
                addWarningIfNegativeDouble(issues, "assignments", assignment.remainingCost,
                        "Assignment RemainingCost が負値です: " + assignmentLabel);
                addWarningIfOutOfRangeInteger(issues, "assignments", assignment.percentWorkComplete, 0, 100,
                        "Assignment PercentWorkComplete が 0..100 の範囲外です: " + assignmentLabel);
                addWarningIfEmptyPresent(issues, "assignments", assignment.overtimeWork,
                        "Assignment OvertimeWork が空です: " + assignmentLabel);
                addWarningIfEmptyPresent(issues, "assignments", assignment.actualOvertimeWork,
                        "Assignment ActualOvertimeWork が空です: " + assignmentLabel);
                addWarningIfEmptyPresent(issues, "assignments", assignment.startVariance,
                        "Assignment StartVariance が空です: " + assignmentLabel);
                addWarningIfEmptyPresent(issues, "assignments", assignment.finishVariance,
                        "Assignment FinishVariance が空です: " + assignmentLabel);
                for (jp.igapyon.mikuproject.model.AssignmentExtendedAttributeModel attribute : assignment.extendedAttributes) {
                    if (isBlank(attribute.fieldID)) {
                        issues.add(issue("warning", "assignments",
                                "Assignment ExtendedAttribute に FieldID がありません: "
                                        + helpers.describeAssignment(assignment)));
                    }
                }
                validateAssignmentBaselines(issues, assignment, assignmentLabel);
                validateAssignmentTimephasedData(issues, assignment);
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

    private void addWarningIfBlank(List<ValidationIssue> issues, String scope, String value, String message) {
        if (isBlank(value)) {
            issues.add(issue("warning", scope, message));
        }
    }

    private void addWarningIfNegativeInteger(List<ValidationIssue> issues, String scope, Integer value, String message) {
        if (value != null && value.intValue() < 0) {
            issues.add(issue("warning", scope, message));
        }
    }

    private void addWarningIfNonPositiveInteger(List<ValidationIssue> issues, String scope, Integer value, String message) {
        if (value != null && value.intValue() <= 0) {
            issues.add(issue("warning", scope, message));
        }
    }

    private void addWarningIfOutOfRangeInteger(List<ValidationIssue> issues, String scope, Integer value, int min, int max,
            String message) {
        if (value != null && (value.intValue() < min || value.intValue() > max)) {
            issues.add(issue("warning", scope, message));
        }
    }

    private void addWarningIfNegativeDouble(List<ValidationIssue> issues, String scope, Double value, String message) {
        if (value != null && value.doubleValue() < 0.0d) {
            issues.add(issue("warning", scope, message));
        }
    }

    private void addWarningIfDateAfter(List<ValidationIssue> issues, String scope, String left, String right, String message) {
        if (compareDateTime(left, right) > 0) {
            issues.add(issue("warning", scope, message));
        }
    }

    private void addWarningIfEmptyPresent(List<ValidationIssue> issues, String scope, String value, String message) {
        if (value != null && value.isEmpty()) {
            issues.add(issue("warning", scope, message));
        }
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

    private ZeroDurationClusterIssue detectZeroDurationClusterIssue(List<TaskModel> tasks) {
        ZeroDurationClusterIssue issue = new ZeroDurationClusterIssue();
        for (TaskModel task : tasks) {
            if (task == null || helpers.isPlaceholderUid(task.uid) || task.summary || task.milestone) {
                continue;
            }
            if (!isZeroDuration(task.duration) || isBlank(task.start) || isBlank(task.finish)) {
                return null;
            }
            if (issue.taskCount == 0) {
                issue.start = task.start;
                issue.finish = task.finish;
            } else if (!issue.start.equals(task.start) || !issue.finish.equals(task.finish)) {
                return null;
            }
            issue.taskCount++;
        }
        return issue.taskCount >= 2 ? issue : null;
    }

    private boolean isZeroDuration(String duration) {
        if (isBlank(duration)) {
            return false;
        }
        String text = duration.trim();
        return "PT0H0M0S".equals(text) || "PT0S".equals(text) || "PT0M0S".equals(text) || "PT0H".equals(text)
                || "PT0H0M".equals(text);
    }

    private void validateTaskBaselines(List<ValidationIssue> issues, TaskModel task, String taskLabel) {
        for (jp.igapyon.mikuproject.model.TaskBaselineModel baseline : task.baselines) {
            addWarningIfNegativeInteger(issues, "tasks", baseline.number,
                    "Task Baseline Number は 0 以上が望ましいです: " + taskLabel);
            addWarningIfNegativeDouble(issues, "tasks", baseline.cost,
                    "Task Baseline Cost が負値です: " + taskLabel);
            addWarningIfDateAfter(issues, "tasks", baseline.start, baseline.finish,
                    "Task Baseline Start が Finish より後です: " + taskLabel);
        }
    }

    private void validateTaskTimephasedData(List<ValidationIssue> issues, TaskModel task) {
        for (jp.igapyon.mikuproject.model.TaskTimephasedDataModel timephasedData : task.timephasedData) {
            addWarningIfNegativeInteger(issues, "tasks", timephasedData.type,
                    "Task TimephasedData Type は 0 以上が望ましいです: " + helpers.describeTask(task));
            addWarningIfNegativeInteger(issues, "tasks", timephasedData.unit,
                    "Task TimephasedData Unit は 0 以上が望ましいです: " + blankSafe(task.uid));
            addWarningIfDateAfter(issues, "tasks", timephasedData.start, timephasedData.finish,
                    "Task TimephasedData Start が Finish より後です: " + blankSafe(task.uid));
        }
    }

    private void validateResourceBaselines(List<ValidationIssue> issues, ResourceModel resource, String resourceLabel) {
        for (jp.igapyon.mikuproject.model.ResourceBaselineModel baseline : resource.baselines) {
            addWarningIfNegativeInteger(issues, "resources", baseline.number,
                    "Resource Baseline Number は 0 以上が望ましいです: " + resourceLabel);
            addWarningIfNegativeDouble(issues, "resources", baseline.cost,
                    "Resource Baseline Cost が負値です: " + resourceLabel);
            addWarningIfDateAfter(issues, "resources", baseline.start, baseline.finish,
                    "Resource Baseline Start が Finish より後です: " + resourceLabel);
        }
    }

    private void validateResourceTimephasedData(List<ValidationIssue> issues, ResourceModel resource) {
        for (jp.igapyon.mikuproject.model.ResourceTimephasedDataModel timephasedData : resource.timephasedData) {
            addWarningIfNegativeInteger(issues, "resources", timephasedData.type,
                    "Resource TimephasedData Type は 0 以上が望ましいです: " + helpers.describeResource(resource));
            addWarningIfNegativeInteger(issues, "resources", timephasedData.unit,
                    "Resource TimephasedData Unit は 0 以上が望ましいです: " + blankSafe(resource.uid));
            addWarningIfDateAfter(issues, "resources", timephasedData.start, timephasedData.finish,
                    "Resource TimephasedData Start が Finish より後です: " + blankSafe(resource.uid));
        }
    }

    private void validateAssignmentBaselines(List<ValidationIssue> issues, AssignmentModel assignment, String assignmentLabel) {
        for (jp.igapyon.mikuproject.model.AssignmentBaselineModel baseline : assignment.baselines) {
            addWarningIfNegativeInteger(issues, "assignments", baseline.number,
                    "Assignment Baseline Number は 0 以上が望ましいです: " + assignmentLabel);
            addWarningIfNegativeDouble(issues, "assignments", baseline.cost,
                    "Assignment Baseline Cost が負値です: " + assignmentLabel);
            addWarningIfDateAfter(issues, "assignments", baseline.start, baseline.finish,
                    "Assignment Baseline Start が Finish より後です: " + assignmentLabel);
        }
    }

    private void validateAssignmentTimephasedData(List<ValidationIssue> issues, AssignmentModel assignment) {
        for (jp.igapyon.mikuproject.model.AssignmentTimephasedDataModel timephasedData : assignment.timephasedData) {
            addWarningIfNegativeInteger(issues, "assignments", timephasedData.type,
                    "Assignment TimephasedData Type は 0 以上が望ましいです: " + helpers.describeAssignment(assignment));
            addWarningIfNegativeInteger(issues, "assignments", timephasedData.unit,
                    "Assignment TimephasedData Unit は 0 以上が望ましいです: " + blankSafe(assignment.uid));
            addWarningIfDateAfter(issues, "assignments", timephasedData.start, timephasedData.finish,
                    "Assignment TimephasedData Start が Finish より後です: " + blankSafe(assignment.uid));
        }
    }

    private static class TaskOrderIssue {
        private TaskModel previous;
        private TaskModel current;
    }

    private static class ZeroDurationClusterIssue {
        private int taskCount;
        private String start;
        private String finish;
    }
}
