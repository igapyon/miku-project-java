/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuproject.msprojectxml;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import jp.igapyon.mikuproject.model.AssignmentModel;
import jp.igapyon.mikuproject.model.AssignmentBaselineModel;
import jp.igapyon.mikuproject.model.AssignmentExtendedAttributeModel;
import jp.igapyon.mikuproject.model.AssignmentTimephasedDataModel;
import jp.igapyon.mikuproject.model.CalendarExceptionModel;
import jp.igapyon.mikuproject.model.CalendarModel;
import jp.igapyon.mikuproject.model.OutlineCodeMaskModel;
import jp.igapyon.mikuproject.model.OutlineCodeModel;
import jp.igapyon.mikuproject.model.OutlineCodeValueModel;
import jp.igapyon.mikuproject.model.PredecessorModel;
import jp.igapyon.mikuproject.model.ProjectExtendedAttributeModel;
import jp.igapyon.mikuproject.model.ProjectModel;
import jp.igapyon.mikuproject.model.ResourceBaselineModel;
import jp.igapyon.mikuproject.model.ResourceExtendedAttributeModel;
import jp.igapyon.mikuproject.model.ResourceModel;
import jp.igapyon.mikuproject.model.ResourceTimephasedDataModel;
import jp.igapyon.mikuproject.model.TaskBaselineModel;
import jp.igapyon.mikuproject.model.TaskExtendedAttributeModel;
import jp.igapyon.mikuproject.model.TaskModel;
import jp.igapyon.mikuproject.model.TaskTimephasedDataModel;
import jp.igapyon.mikuproject.model.WeekDayModel;
import jp.igapyon.mikuproject.model.WbsMaskModel;
import jp.igapyon.mikuproject.model.WorkWeekModel;
import jp.igapyon.mikuproject.model.WorkingTimeModel;

public class MsProjectCodec {
    private final MsProjectXmlDom xmlDom = new MsProjectXmlDom();

    public ProjectModel importMsProjectXml(String xmlText) {
        Document document = xmlDom.parseXmlDocument(xmlText);
        Element projectElement = document.getDocumentElement();

        ProjectModel model = new ProjectModel();
        model.project.name = xmlDom.textContent(projectElement, "Name");
        model.project.title = xmlDom.textContent(projectElement, "Title");
        model.project.author = xmlDom.textContent(projectElement, "Author");
        model.project.company = xmlDom.textContent(projectElement, "Company");
        model.project.creationDate = xmlDom.textContent(projectElement, "CreationDate");
        model.project.lastSaved = xmlDom.textContent(projectElement, "LastSaved");
        model.project.startDate = xmlDom.textContent(projectElement, "StartDate");
        model.project.finishDate = xmlDom.textContent(projectElement, "FinishDate");
        model.project.currentDate = xmlDom.textContent(projectElement, "CurrentDate");
        model.project.defaultStartTime = xmlDom.textContent(projectElement, "DefaultStartTime");
        model.project.defaultFinishTime = xmlDom.textContent(projectElement, "DefaultFinishTime");
        model.project.statusDate = xmlDom.textContent(projectElement, "StatusDate");
        model.project.currencyCode = xmlDom.textContent(projectElement, "CurrencyCode");
        model.project.currencySymbol = xmlDom.textContent(projectElement, "CurrencySymbol");
        model.project.fyStartDate = xmlDom.textContent(projectElement, "FYStartDate");
        model.project.defaultStandardRate = xmlDom.textContent(projectElement, "DefaultStandardRate");
        model.project.defaultOvertimeRate = xmlDom.textContent(projectElement, "DefaultOvertimeRate");

        String saveVersion = xmlDom.textContent(projectElement, "SaveVersion");
        if (!saveVersion.isEmpty()) {
            model.project.saveVersion = Integer.valueOf(xmlDom.parseNumber(saveVersion, 0));
        }

        String scheduleFromStart = xmlDom.textContent(projectElement, "ScheduleFromStart");
        if (!scheduleFromStart.isEmpty()) {
            model.project.scheduleFromStart = xmlDom.parseBoolean(scheduleFromStart);
        }

        String minutesPerDay = xmlDom.textContent(projectElement, "MinutesPerDay");
        if (!minutesPerDay.isEmpty()) {
            model.project.minutesPerDay = Integer.valueOf(xmlDom.parseNumber(minutesPerDay, 0));
        }

        String minutesPerWeek = xmlDom.textContent(projectElement, "MinutesPerWeek");
        if (!minutesPerWeek.isEmpty()) {
            model.project.minutesPerWeek = Integer.valueOf(xmlDom.parseNumber(minutesPerWeek, 0));
        }

        String daysPerMonth = xmlDom.textContent(projectElement, "DaysPerMonth");
        if (!daysPerMonth.isEmpty()) {
            model.project.daysPerMonth = Integer.valueOf(xmlDom.parseNumber(daysPerMonth, 0));
        }

        String weekStartDay = xmlDom.textContent(projectElement, "WeekStartDay");
        if (!weekStartDay.isEmpty()) {
            model.project.weekStartDay = Integer.valueOf(xmlDom.parseNumber(weekStartDay, 0));
        }

        String workFormat = xmlDom.textContent(projectElement, "WorkFormat");
        if (!workFormat.isEmpty()) {
            model.project.workFormat = Integer.valueOf(xmlDom.parseNumber(workFormat, 0));
        }

        String durationFormat = xmlDom.textContent(projectElement, "DurationFormat");
        if (!durationFormat.isEmpty()) {
            model.project.durationFormat = Integer.valueOf(xmlDom.parseNumber(durationFormat, 0));
        }

        String currencyDigits = xmlDom.textContent(projectElement, "CurrencyDigits");
        if (!currencyDigits.isEmpty()) {
            model.project.currencyDigits = Integer.valueOf(xmlDom.parseNumber(currencyDigits, 0));
        }

        String currencySymbolPosition = xmlDom.textContent(projectElement, "CurrencySymbolPosition");
        if (!currencySymbolPosition.isEmpty()) {
            model.project.currencySymbolPosition = Integer.valueOf(xmlDom.parseNumber(currencySymbolPosition, 0));
        }

        String fiscalYearStart = xmlDom.textContent(projectElement, "FiscalYearStart");
        if (!fiscalYearStart.isEmpty()) {
            model.project.fiscalYearStart = Boolean.valueOf(xmlDom.parseBoolean(fiscalYearStart));
        }

        String criticalSlackLimit = xmlDom.textContent(projectElement, "CriticalSlackLimit");
        if (!criticalSlackLimit.isEmpty()) {
            model.project.criticalSlackLimit = Integer.valueOf(xmlDom.parseNumber(criticalSlackLimit, 0));
        }

        String defaultTaskType = xmlDom.textContent(projectElement, "DefaultTaskType");
        if (!defaultTaskType.isEmpty()) {
            model.project.defaultTaskType = Integer.valueOf(xmlDom.parseNumber(defaultTaskType, 0));
        }

        String defaultFixedCostAccrual = xmlDom.textContent(projectElement, "DefaultFixedCostAccrual");
        if (!defaultFixedCostAccrual.isEmpty()) {
            model.project.defaultFixedCostAccrual = Integer.valueOf(xmlDom.parseNumber(defaultFixedCostAccrual, 0));
        }

        String defaultTaskEVMethod = xmlDom.textContent(projectElement, "DefaultTaskEVMethod");
        if (!defaultTaskEVMethod.isEmpty()) {
            model.project.defaultTaskEVMethod = Integer.valueOf(xmlDom.parseNumber(defaultTaskEVMethod, 0));
        }

        String newTaskStartDate = xmlDom.textContent(projectElement, "NewTaskStartDate");
        if (!newTaskStartDate.isEmpty()) {
            model.project.newTaskStartDate = Integer.valueOf(xmlDom.parseNumber(newTaskStartDate, 0));
        }

        String newTasksAreManual = xmlDom.textContent(projectElement, "NewTasksAreManual");
        if (!newTasksAreManual.isEmpty()) {
            model.project.newTasksAreManual = Boolean.valueOf(xmlDom.parseBoolean(newTasksAreManual));
        }

        String newTasksEffortDriven = xmlDom.textContent(projectElement, "NewTasksEffortDriven");
        if (!newTasksEffortDriven.isEmpty()) {
            model.project.newTasksEffortDriven = Boolean.valueOf(xmlDom.parseBoolean(newTasksEffortDriven));
        }

        String newTasksEstimated = xmlDom.textContent(projectElement, "NewTasksEstimated");
        if (!newTasksEstimated.isEmpty()) {
            model.project.newTasksEstimated = Boolean.valueOf(xmlDom.parseBoolean(newTasksEstimated));
        }

        String actualsInSync = xmlDom.textContent(projectElement, "ActualsInSync");
        if (!actualsInSync.isEmpty()) {
            model.project.actualsInSync = Boolean.valueOf(xmlDom.parseBoolean(actualsInSync));
        }

        String editableActualCosts = xmlDom.textContent(projectElement, "EditableActualCosts");
        if (!editableActualCosts.isEmpty()) {
            model.project.editableActualCosts = Boolean.valueOf(xmlDom.parseBoolean(editableActualCosts));
        }

        String honorConstraints = xmlDom.textContent(projectElement, "HonorConstraints");
        if (!honorConstraints.isEmpty()) {
            model.project.honorConstraints = Boolean.valueOf(xmlDom.parseBoolean(honorConstraints));
        }

        String insertedProjectsLikeSummary = xmlDom.textContent(projectElement, "InsertedProjectsLikeSummary");
        if (!insertedProjectsLikeSummary.isEmpty()) {
            model.project.insertedProjectsLikeSummary = Boolean.valueOf(xmlDom.parseBoolean(insertedProjectsLikeSummary));
        }

        String multipleCriticalPaths = xmlDom.textContent(projectElement, "MultipleCriticalPaths");
        if (!multipleCriticalPaths.isEmpty()) {
            model.project.multipleCriticalPaths = Boolean.valueOf(xmlDom.parseBoolean(multipleCriticalPaths));
        }

        String taskUpdatesResource = xmlDom.textContent(projectElement, "TaskUpdatesResource");
        if (!taskUpdatesResource.isEmpty()) {
            model.project.taskUpdatesResource = Boolean.valueOf(xmlDom.parseBoolean(taskUpdatesResource));
        }

        String updateManuallyScheduledTasksWhenEditingLinks = xmlDom.textContent(projectElement, "UpdateManuallyScheduledTasksWhenEditingLinks");
        if (!updateManuallyScheduledTasksWhenEditingLinks.isEmpty()) {
            model.project.updateManuallyScheduledTasksWhenEditingLinks =
                    Boolean.valueOf(xmlDom.parseBoolean(updateManuallyScheduledTasksWhenEditingLinks));
        }

        model.project.calendarUID = xmlDom.textContent(projectElement, "CalendarUID");
        model.project.outlineCodes.addAll(xmlDom.parseOutlineCodes(projectElement));
        model.project.wbsMasks.addAll(xmlDom.parseWbsMasks(projectElement));
        model.project.extendedAttributes.addAll(xmlDom.parseProjectExtendedAttributes(projectElement));
        importCalendars(projectElement, model);
        importTasks(projectElement, model);
        importResources(projectElement, model);
        importAssignments(projectElement, model);
        return model;
    }

    public String exportMsProjectXml(ProjectModel model) {
        StringBuilder builder = new StringBuilder();
        builder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        builder.append("<Project xmlns=\"http://schemas.microsoft.com/project\">\n");
        appendTextElement(builder, 1, "Name", model.project.name);
        appendTextElement(builder, 1, "Title", model.project.title);
        appendTextElement(builder, 1, "Company", model.project.company);
        appendTextElement(builder, 1, "Author", model.project.author);
        appendTextElement(builder, 1, "CreationDate", model.project.creationDate);
        appendTextElement(builder, 1, "LastSaved", model.project.lastSaved);
        appendTextElement(builder, 1, "SaveVersion", model.project.saveVersion);
        appendTextElement(builder, 1, "CurrentDate", model.project.currentDate);
        appendTextElement(builder, 1, "StartDate", model.project.startDate);
        appendTextElement(builder, 1, "FinishDate", model.project.finishDate);
        appendTextElement(builder, 1, "ScheduleFromStart", Boolean.valueOf(model.project.scheduleFromStart));
        appendTextElement(builder, 1, "DefaultStartTime", model.project.defaultStartTime);
        appendTextElement(builder, 1, "DefaultFinishTime", model.project.defaultFinishTime);
        appendTextElement(builder, 1, "MinutesPerDay", model.project.minutesPerDay);
        appendTextElement(builder, 1, "MinutesPerWeek", model.project.minutesPerWeek);
        appendTextElement(builder, 1, "DaysPerMonth", model.project.daysPerMonth);
        appendTextElement(builder, 1, "StatusDate", model.project.statusDate);
        appendTextElement(builder, 1, "WeekStartDay", model.project.weekStartDay);
        appendTextElement(builder, 1, "WorkFormat", model.project.workFormat);
        appendTextElement(builder, 1, "DurationFormat", model.project.durationFormat);
        appendTextElement(builder, 1, "CurrencyCode", model.project.currencyCode);
        appendTextElement(builder, 1, "CurrencyDigits", model.project.currencyDigits);
        appendTextElement(builder, 1, "CurrencySymbol", model.project.currencySymbol);
        appendTextElement(builder, 1, "CurrencySymbolPosition", model.project.currencySymbolPosition);
        appendTextElement(builder, 1, "FYStartDate", model.project.fyStartDate);
        appendTextElement(builder, 1, "FiscalYearStart", model.project.fiscalYearStart);
        appendTextElement(builder, 1, "CriticalSlackLimit", model.project.criticalSlackLimit);
        appendTextElement(builder, 1, "DefaultTaskType", model.project.defaultTaskType);
        appendTextElement(builder, 1, "DefaultFixedCostAccrual", model.project.defaultFixedCostAccrual);
        appendTextElement(builder, 1, "DefaultStandardRate", model.project.defaultStandardRate);
        appendTextElement(builder, 1, "DefaultOvertimeRate", model.project.defaultOvertimeRate);
        appendTextElement(builder, 1, "DefaultTaskEVMethod", model.project.defaultTaskEVMethod);
        appendTextElement(builder, 1, "NewTaskStartDate", model.project.newTaskStartDate);
        appendTextElement(builder, 1, "NewTasksAreManual", model.project.newTasksAreManual);
        appendTextElement(builder, 1, "NewTasksEffortDriven", model.project.newTasksEffortDriven);
        appendTextElement(builder, 1, "NewTasksEstimated", model.project.newTasksEstimated);
        appendTextElement(builder, 1, "ActualsInSync", model.project.actualsInSync);
        appendTextElement(builder, 1, "EditableActualCosts", model.project.editableActualCosts);
        appendTextElement(builder, 1, "HonorConstraints", model.project.honorConstraints);
        appendTextElement(builder, 1, "InsertedProjectsLikeSummary", model.project.insertedProjectsLikeSummary);
        appendTextElement(builder, 1, "MultipleCriticalPaths", model.project.multipleCriticalPaths);
        appendTextElement(builder, 1, "TaskUpdatesResource", model.project.taskUpdatesResource);
        appendTextElement(builder, 1, "UpdateManuallyScheduledTasksWhenEditingLinks",
                model.project.updateManuallyScheduledTasksWhenEditingLinks);
        appendTextElement(builder, 1, "CalendarUID", model.project.calendarUID);
        appendOutlineCodes(builder, model);
        appendWbsMasks(builder, model);
        appendProjectExtendedAttributes(builder, model);
        appendCalendars(builder, model);
        appendTasks(builder, model);
        appendResources(builder, model);
        appendAssignments(builder, model);
        builder.append("</Project>\n");
        return builder.toString();
    }

    private void importCalendars(Element projectElement, ProjectModel model) {
        Element calendarsElement = xmlDom.firstChildElement(projectElement, "Calendars");
        if (calendarsElement == null) {
            return;
        }
        NodeList nodes = calendarsElement.getChildNodes();
        for (int index = 0; index < nodes.getLength(); index += 1) {
            Node node = nodes.item(index);
            if (!(node instanceof Element) || !"Calendar".equals(((Element) node).getTagName())) {
                continue;
            }
            Element calendarElement = (Element) node;
            CalendarModel calendar = new CalendarModel();
            calendar.uid = xmlDom.textContent(calendarElement, "UID");
            calendar.name = xmlDom.textContent(calendarElement, "Name");
            calendar.baseCalendarUID = xmlDom.textContent(calendarElement, "BaseCalendarUID");
            String isBaseCalendar = xmlDom.textContent(calendarElement, "IsBaseCalendar");
            if (!isBaseCalendar.isEmpty()) {
                calendar.isBaseCalendar = xmlDom.parseBoolean(isBaseCalendar);
            }
            String isBaselineCalendar = xmlDom.textContent(calendarElement, "IsBaselineCalendar");
            if (!isBaselineCalendar.isEmpty()) {
                calendar.isBaselineCalendar = Boolean.valueOf(xmlDom.parseBoolean(isBaselineCalendar));
            }
            calendar.weekDays.addAll(xmlDom.parseWeekDays(calendarElement));
            calendar.exceptions.addAll(xmlDom.parseCalendarExceptions(calendarElement));
            calendar.workWeeks.addAll(xmlDom.parseWorkWeeks(calendarElement));
            model.calendars.add(calendar);
        }
    }

    private void importTasks(Element projectElement, ProjectModel model) {
        Element tasksElement = xmlDom.firstChildElement(projectElement, "Tasks");
        if (tasksElement == null) {
            return;
        }
        NodeList nodes = tasksElement.getChildNodes();
        for (int index = 0; index < nodes.getLength(); index += 1) {
            Node node = nodes.item(index);
            if (!(node instanceof Element) || !"Task".equals(((Element) node).getTagName())) {
                continue;
            }
            Element taskElement = (Element) node;
            TaskModel task = new TaskModel();
            task.uid = xmlDom.textContent(taskElement, "UID");
            task.id = xmlDom.textContent(taskElement, "ID");
            task.name = xmlDom.textContent(taskElement, "Name");
            task.outlineNumber = xmlDom.textContent(taskElement, "OutlineNumber");
            task.wbs = xmlDom.textContent(taskElement, "WBS");
            task.start = xmlDom.textContent(taskElement, "Start");
            task.finish = xmlDom.textContent(taskElement, "Finish");
            task.duration = xmlDom.textContent(taskElement, "Duration");
            task.actualStart = xmlDom.textContent(taskElement, "ActualStart");
            task.actualFinish = xmlDom.textContent(taskElement, "ActualFinish");
            task.deadline = xmlDom.textContent(taskElement, "Deadline");
            task.startVariance = xmlDom.textContent(taskElement, "StartVariance");
            task.finishVariance = xmlDom.textContent(taskElement, "FinishVariance");
            task.work = xmlDom.textContent(taskElement, "Work");
            task.workVariance = xmlDom.textContent(taskElement, "WorkVariance");
            task.totalSlack = xmlDom.textContent(taskElement, "TotalSlack");
            task.freeSlack = xmlDom.textContent(taskElement, "FreeSlack");
            task.remainingWork = xmlDom.textContent(taskElement, "RemainingWork");
            task.actualWork = xmlDom.textContent(taskElement, "ActualWork");
            task.calendarUID = xmlDom.textContent(taskElement, "CalendarUID");
            task.notes = xmlDom.textContent(taskElement, "Notes");

            String outlineLevel = xmlDom.textContent(taskElement, "OutlineLevel");
            if (!outlineLevel.isEmpty()) {
                task.outlineLevel = Integer.valueOf(xmlDom.parseNumber(outlineLevel, 1));
            }

            String type = xmlDom.textContent(taskElement, "Type");
            if (!type.isEmpty()) {
                task.type = Integer.valueOf(xmlDom.parseNumber(type, 0));
            }

            String priority = xmlDom.textContent(taskElement, "Priority");
            if (!priority.isEmpty()) {
                task.priority = Integer.valueOf(xmlDom.parseNumber(priority, 0));
            }

            String milestone = xmlDom.textContent(taskElement, "Milestone");
            if (!milestone.isEmpty()) {
                task.milestone = xmlDom.parseBoolean(milestone);
            }

            String summary = xmlDom.textContent(taskElement, "Summary");
            if (!summary.isEmpty()) {
                task.summary = xmlDom.parseBoolean(summary);
            }

            String percentComplete = xmlDom.textContent(taskElement, "PercentComplete");
            if (!percentComplete.isEmpty()) {
                task.percentComplete = Integer.valueOf(xmlDom.parseNumber(percentComplete, 0));
            }

            String percentWorkComplete = xmlDom.textContent(taskElement, "PercentWorkComplete");
            if (!percentWorkComplete.isEmpty()) {
                task.percentWorkComplete = Integer.valueOf(xmlDom.parseNumber(percentWorkComplete, 0));
            }

            String critical = xmlDom.textContent(taskElement, "Critical");
            if (!critical.isEmpty()) {
                task.critical = Boolean.valueOf(xmlDom.parseBoolean(critical));
            }

            String cost = xmlDom.textContent(taskElement, "Cost");
            if (!cost.isEmpty()) {
                task.cost = Double.valueOf(xmlDom.parseDouble(cost, 0.0d));
            }

            String actualCost = xmlDom.textContent(taskElement, "ActualCost");
            if (!actualCost.isEmpty()) {
                task.actualCost = Double.valueOf(xmlDom.parseDouble(actualCost, 0.0d));
            }

            String remainingCost = xmlDom.textContent(taskElement, "RemainingCost");
            if (!remainingCost.isEmpty()) {
                task.remainingCost = Double.valueOf(xmlDom.parseDouble(remainingCost, 0.0d));
            }

            String constraintType = xmlDom.textContent(taskElement, "ConstraintType");
            if (!constraintType.isEmpty()) {
                task.constraintType = Integer.valueOf(xmlDom.parseNumber(constraintType, 0));
            }

            task.constraintDate = xmlDom.textContent(taskElement, "ConstraintDate");
            task.extendedAttributes.addAll(xmlDom.parseTaskExtendedAttributes(taskElement));
            task.baselines.addAll(xmlDom.parseTaskBaselines(taskElement));
            task.timephasedData.addAll(xmlDom.parseTaskTimephasedData(taskElement));

            NodeList children = taskElement.getChildNodes();
            for (int childIndex = 0; childIndex < children.getLength(); childIndex += 1) {
                Node child = children.item(childIndex);
                if (!(child instanceof Element) || !"PredecessorLink".equals(((Element) child).getTagName())) {
                    continue;
                }
                Element predecessorElement = (Element) child;
                PredecessorModel predecessor = new PredecessorModel();
                predecessor.predecessorUid = xmlDom.textContent(predecessorElement, "PredecessorUID");
                String predecessorType = xmlDom.textContent(predecessorElement, "Type");
                if (!predecessorType.isEmpty()) {
                    predecessor.type = Integer.valueOf(xmlDom.parseNumber(predecessorType, 0));
                }
                predecessor.linkLag = xmlDom.textContent(predecessorElement, "LinkLag");
                task.predecessors.add(predecessor);
            }

            model.tasks.add(task);
        }
    }

    private void importResources(Element projectElement, ProjectModel model) {
        Element resourcesElement = xmlDom.firstChildElement(projectElement, "Resources");
        if (resourcesElement == null) {
            return;
        }
        NodeList nodes = resourcesElement.getChildNodes();
        for (int index = 0; index < nodes.getLength(); index += 1) {
            Node node = nodes.item(index);
            if (!(node instanceof Element) || !"Resource".equals(((Element) node).getTagName())) {
                continue;
            }
            Element resourceElement = (Element) node;
            ResourceModel resource = new ResourceModel();
            resource.uid = xmlDom.textContent(resourceElement, "UID");
            resource.id = xmlDom.textContent(resourceElement, "ID");
            resource.name = xmlDom.textContent(resourceElement, "Name");
            resource.initials = xmlDom.textContent(resourceElement, "Initials");
            resource.group = xmlDom.textContent(resourceElement, "Group");
            resource.calendarUID = xmlDom.textContent(resourceElement, "CalendarUID");
            resource.standardRate = xmlDom.textContent(resourceElement, "StandardRate");
            resource.overtimeRate = xmlDom.textContent(resourceElement, "OvertimeRate");
            resource.work = xmlDom.textContent(resourceElement, "Work");
            resource.actualWork = xmlDom.textContent(resourceElement, "ActualWork");
            resource.remainingWork = xmlDom.textContent(resourceElement, "RemainingWork");

            String maxUnits = xmlDom.textContent(resourceElement, "MaxUnits");
            if (!maxUnits.isEmpty()) {
                resource.maxUnits = Double.valueOf(xmlDom.parseDouble(maxUnits, 0.0d));
            }

            String type = xmlDom.textContent(resourceElement, "Type");
            if (!type.isEmpty()) {
                resource.type = Integer.valueOf(xmlDom.parseNumber(type, 0));
            }

            String workGroup = xmlDom.textContent(resourceElement, "WorkGroup");
            if (!workGroup.isEmpty()) {
                resource.workGroup = Integer.valueOf(xmlDom.parseNumber(workGroup, 0));
            }

            String standardRateFormat = xmlDom.textContent(resourceElement, "StandardRateFormat");
            if (!standardRateFormat.isEmpty()) {
                resource.standardRateFormat = Integer.valueOf(xmlDom.parseNumber(standardRateFormat, 0));
            }

            String overtimeRateFormat = xmlDom.textContent(resourceElement, "OvertimeRateFormat");
            if (!overtimeRateFormat.isEmpty()) {
                resource.overtimeRateFormat = Integer.valueOf(xmlDom.parseNumber(overtimeRateFormat, 0));
            }

            String costPerUse = xmlDom.textContent(resourceElement, "CostPerUse");
            if (!costPerUse.isEmpty()) {
                resource.costPerUse = Double.valueOf(xmlDom.parseDouble(costPerUse, 0.0d));
            }

            String cost = xmlDom.textContent(resourceElement, "Cost");
            if (!cost.isEmpty()) {
                resource.cost = Double.valueOf(xmlDom.parseDouble(cost, 0.0d));
            }

            String actualCost = xmlDom.textContent(resourceElement, "ActualCost");
            if (!actualCost.isEmpty()) {
                resource.actualCost = Double.valueOf(xmlDom.parseDouble(actualCost, 0.0d));
            }

            String remainingCost = xmlDom.textContent(resourceElement, "RemainingCost");
            if (!remainingCost.isEmpty()) {
                resource.remainingCost = Double.valueOf(xmlDom.parseDouble(remainingCost, 0.0d));
            }

            String percentWorkComplete = xmlDom.textContent(resourceElement, "PercentWorkComplete");
            if (!percentWorkComplete.isEmpty()) {
                resource.percentWorkComplete = Integer.valueOf(xmlDom.parseNumber(percentWorkComplete, 0));
            }

            resource.extendedAttributes.addAll(xmlDom.parseResourceExtendedAttributes(resourceElement));
            resource.baselines.addAll(xmlDom.parseResourceBaselines(resourceElement));
            resource.timephasedData.addAll(xmlDom.parseResourceTimephasedData(resourceElement));

            model.resources.add(resource);
        }
    }

    private void importAssignments(Element projectElement, ProjectModel model) {
        Element assignmentsElement = xmlDom.firstChildElement(projectElement, "Assignments");
        if (assignmentsElement == null) {
            return;
        }
        NodeList nodes = assignmentsElement.getChildNodes();
        for (int index = 0; index < nodes.getLength(); index += 1) {
            Node node = nodes.item(index);
            if (!(node instanceof Element) || !"Assignment".equals(((Element) node).getTagName())) {
                continue;
            }
            Element assignmentElement = (Element) node;
            AssignmentModel assignment = new AssignmentModel();
            assignment.uid = xmlDom.textContent(assignmentElement, "UID");
            assignment.taskUid = xmlDom.textContent(assignmentElement, "TaskUID");
            assignment.resourceUid = xmlDom.textContent(assignmentElement, "ResourceUID");
            assignment.start = xmlDom.textContent(assignmentElement, "Start");
            assignment.finish = xmlDom.textContent(assignmentElement, "Finish");
            assignment.startVariance = xmlDom.textContent(assignmentElement, "StartVariance");
            assignment.finishVariance = xmlDom.textContent(assignmentElement, "FinishVariance");
            assignment.delay = xmlDom.textContent(assignmentElement, "Delay");
            assignment.work = xmlDom.textContent(assignmentElement, "Work");
            assignment.overtimeWork = xmlDom.textContent(assignmentElement, "OvertimeWork");
            assignment.actualOvertimeWork = xmlDom.textContent(assignmentElement, "ActualOvertimeWork");
            assignment.actualWork = xmlDom.textContent(assignmentElement, "ActualWork");
            assignment.remainingWork = xmlDom.textContent(assignmentElement, "RemainingWork");

            String units = xmlDom.textContent(assignmentElement, "Units");
            if (!units.isEmpty()) {
                assignment.units = Double.valueOf(xmlDom.parseDouble(units, 0.0d));
            }

            String milestone = xmlDom.textContent(assignmentElement, "Milestone");
            if (!milestone.isEmpty()) {
                assignment.milestone = Boolean.valueOf(xmlDom.parseBoolean(milestone));
            }

            String workContour = xmlDom.textContent(assignmentElement, "WorkContour");
            if (!workContour.isEmpty()) {
                assignment.workContour = Integer.valueOf(xmlDom.parseNumber(workContour, 0));
            }

            String cost = xmlDom.textContent(assignmentElement, "Cost");
            if (!cost.isEmpty()) {
                assignment.cost = Double.valueOf(xmlDom.parseDouble(cost, 0.0d));
            }

            String actualCost = xmlDom.textContent(assignmentElement, "ActualCost");
            if (!actualCost.isEmpty()) {
                assignment.actualCost = Double.valueOf(xmlDom.parseDouble(actualCost, 0.0d));
            }

            String remainingCost = xmlDom.textContent(assignmentElement, "RemainingCost");
            if (!remainingCost.isEmpty()) {
                assignment.remainingCost = Double.valueOf(xmlDom.parseDouble(remainingCost, 0.0d));
            }

            String percentWorkComplete = xmlDom.textContent(assignmentElement, "PercentWorkComplete");
            if (!percentWorkComplete.isEmpty()) {
                assignment.percentWorkComplete = Integer.valueOf(xmlDom.parseNumber(percentWorkComplete, 0));
            }

            assignment.extendedAttributes.addAll(xmlDom.parseAssignmentExtendedAttributes(assignmentElement));
            assignment.baselines.addAll(xmlDom.parseAssignmentBaselines(assignmentElement));
            assignment.timephasedData.addAll(xmlDom.parseAssignmentTimephasedData(assignmentElement));

            model.assignments.add(assignment);
        }
    }

    private void appendCalendars(StringBuilder builder, ProjectModel model) {
        builder.append("  <Calendars>\n");
        for (CalendarModel calendar : model.calendars) {
            builder.append("    <Calendar>\n");
            appendTextElement(builder, 3, "UID", calendar.uid);
            appendTextElement(builder, 3, "Name", calendar.name);
            appendTextElement(builder, 3, "IsBaseCalendar", Boolean.valueOf(calendar.isBaseCalendar));
            appendTextElement(builder, 3, "IsBaselineCalendar", calendar.isBaselineCalendar);
            appendTextElement(builder, 3, "BaseCalendarUID", calendar.baseCalendarUID);
            appendWeekDays(builder, 3, calendar.weekDays);
            appendExceptions(builder, 3, calendar.exceptions);
            appendWorkWeeks(builder, 3, calendar.workWeeks);
            builder.append("    </Calendar>\n");
        }
        builder.append("  </Calendars>\n");
    }

    private void appendTasks(StringBuilder builder, ProjectModel model) {
        builder.append("  <Tasks>\n");
        for (TaskModel task : model.tasks) {
            builder.append("    <Task>\n");
            appendTextElement(builder, 3, "UID", task.uid);
            appendTextElement(builder, 3, "ID", task.id);
            appendTextElement(builder, 3, "Name", task.name);
            appendTextElement(builder, 3, "OutlineLevel", task.outlineLevel);
            appendTextElement(builder, 3, "OutlineNumber", task.outlineNumber);
            appendTextElement(builder, 3, "WBS", task.wbs);
            appendTextElement(builder, 3, "Type", task.type);
            appendTextElement(builder, 3, "CalendarUID", task.calendarUID);
            appendTextElement(builder, 3, "Priority", task.priority);
            appendTextElement(builder, 3, "Start", task.start);
            appendTextElement(builder, 3, "Finish", task.finish);
            appendTextElement(builder, 3, "Duration", task.duration);
            appendTextElement(builder, 3, "ActualStart", task.actualStart);
            appendTextElement(builder, 3, "ActualFinish", task.actualFinish);
            appendTextElement(builder, 3, "Deadline", task.deadline);
            appendTextElement(builder, 3, "StartVariance", task.startVariance);
            appendTextElement(builder, 3, "FinishVariance", task.finishVariance);
            appendTextElement(builder, 3, "Work", task.work);
            appendTextElement(builder, 3, "WorkVariance", task.workVariance);
            appendTextElement(builder, 3, "TotalSlack", task.totalSlack);
            appendTextElement(builder, 3, "FreeSlack", task.freeSlack);
            appendTextElement(builder, 3, "Cost", task.cost);
            appendTextElement(builder, 3, "ActualCost", task.actualCost);
            appendTextElement(builder, 3, "RemainingCost", task.remainingCost);
            appendTextElement(builder, 3, "RemainingWork", task.remainingWork);
            appendTextElement(builder, 3, "ActualWork", task.actualWork);
            appendTextElement(builder, 3, "Milestone", Boolean.valueOf(task.milestone));
            appendTextElement(builder, 3, "Summary", Boolean.valueOf(task.summary));
            appendTextElement(builder, 3, "Critical", task.critical);
            appendTextElement(builder, 3, "PercentComplete", task.percentComplete);
            appendTextElement(builder, 3, "PercentWorkComplete", task.percentWorkComplete);
            appendTextElement(builder, 3, "Notes", task.notes);
            appendTextElement(builder, 3, "ConstraintType", task.constraintType);
            appendTextElement(builder, 3, "ConstraintDate", task.constraintDate);
            appendTaskExtendedAttributes(builder, task);
            appendTaskBaselines(builder, task);
            appendTaskTimephasedData(builder, task);
            for (PredecessorModel predecessor : task.predecessors) {
                builder.append("      <PredecessorLink>\n");
                appendTextElement(builder, 4, "PredecessorUID", predecessor.predecessorUid);
                appendTextElement(builder, 4, "Type", predecessor.type);
                appendTextElement(builder, 4, "LinkLag", predecessor.linkLag);
                builder.append("      </PredecessorLink>\n");
            }
            builder.append("    </Task>\n");
        }
        builder.append("  </Tasks>\n");
    }

    private void appendResources(StringBuilder builder, ProjectModel model) {
        builder.append("  <Resources>\n");
        for (ResourceModel resource : model.resources) {
            builder.append("    <Resource>\n");
            appendTextElement(builder, 3, "UID", resource.uid);
            appendTextElement(builder, 3, "ID", resource.id);
            appendTextElement(builder, 3, "Name", resource.name);
            appendTextElement(builder, 3, "Type", resource.type);
            appendTextElement(builder, 3, "Initials", resource.initials);
            appendTextElement(builder, 3, "Group", resource.group);
            appendTextElement(builder, 3, "WorkGroup", resource.workGroup);
            appendTextElement(builder, 3, "MaxUnits", resource.maxUnits);
            appendTextElement(builder, 3, "CalendarUID", resource.calendarUID);
            appendTextElement(builder, 3, "StandardRate", resource.standardRate);
            appendTextElement(builder, 3, "StandardRateFormat", resource.standardRateFormat);
            appendTextElement(builder, 3, "OvertimeRate", resource.overtimeRate);
            appendTextElement(builder, 3, "OvertimeRateFormat", resource.overtimeRateFormat);
            appendTextElement(builder, 3, "CostPerUse", resource.costPerUse);
            appendTextElement(builder, 3, "Work", resource.work);
            appendTextElement(builder, 3, "ActualWork", resource.actualWork);
            appendTextElement(builder, 3, "RemainingWork", resource.remainingWork);
            appendTextElement(builder, 3, "Cost", resource.cost);
            appendTextElement(builder, 3, "ActualCost", resource.actualCost);
            appendTextElement(builder, 3, "RemainingCost", resource.remainingCost);
            appendTextElement(builder, 3, "PercentWorkComplete", resource.percentWorkComplete);
            appendResourceExtendedAttributes(builder, resource);
            appendResourceBaselines(builder, resource);
            appendResourceTimephasedData(builder, resource);
            builder.append("    </Resource>\n");
        }
        builder.append("  </Resources>\n");
    }

    private void appendAssignments(StringBuilder builder, ProjectModel model) {
        builder.append("  <Assignments>\n");
        for (AssignmentModel assignment : model.assignments) {
            builder.append("    <Assignment>\n");
            appendTextElement(builder, 3, "UID", assignment.uid);
            appendTextElement(builder, 3, "TaskUID", assignment.taskUid);
            appendTextElement(builder, 3, "ResourceUID", assignment.resourceUid);
            appendTextElement(builder, 3, "Start", assignment.start);
            appendTextElement(builder, 3, "Finish", assignment.finish);
            appendTextElement(builder, 3, "StartVariance", assignment.startVariance);
            appendTextElement(builder, 3, "FinishVariance", assignment.finishVariance);
            appendTextElement(builder, 3, "Delay", assignment.delay);
            appendTextElement(builder, 3, "Milestone", assignment.milestone);
            appendTextElement(builder, 3, "WorkContour", assignment.workContour);
            appendTextElement(builder, 3, "Units", assignment.units);
            appendTextElement(builder, 3, "Work", assignment.work);
            appendTextElement(builder, 3, "Cost", assignment.cost);
            appendTextElement(builder, 3, "ActualCost", assignment.actualCost);
            appendTextElement(builder, 3, "RemainingCost", assignment.remainingCost);
            appendTextElement(builder, 3, "PercentWorkComplete", assignment.percentWorkComplete);
            appendTextElement(builder, 3, "OvertimeWork", assignment.overtimeWork);
            appendTextElement(builder, 3, "ActualOvertimeWork", assignment.actualOvertimeWork);
            appendTextElement(builder, 3, "ActualWork", assignment.actualWork);
            appendTextElement(builder, 3, "RemainingWork", assignment.remainingWork);
            appendAssignmentExtendedAttributes(builder, assignment);
            appendAssignmentBaselines(builder, assignment);
            appendAssignmentTimephasedData(builder, assignment);
            builder.append("    </Assignment>\n");
        }
        builder.append("  </Assignments>\n");
    }

    private void appendOutlineCodes(StringBuilder builder, ProjectModel model) {
        if (model.project.outlineCodes == null || model.project.outlineCodes.isEmpty()) {
            return;
        }
        builder.append("  <OutlineCodes>\n");
        for (OutlineCodeModel outlineCode : model.project.outlineCodes) {
            builder.append("    <OutlineCode>\n");
            appendTextElement(builder, 3, "FieldID", outlineCode.fieldID);
            appendTextElement(builder, 3, "FieldName", outlineCode.fieldName);
            appendTextElement(builder, 3, "Alias", outlineCode.alias);
            appendTextElement(builder, 3, "OnlyTableValues", outlineCode.onlyTableValues);
            appendTextElement(builder, 3, "Enterprise", outlineCode.enterprise);
            appendTextElement(builder, 3, "ResourceSubstitutionEnabled", outlineCode.resourceSubstitutionEnabled);
            appendTextElement(builder, 3, "LeafOnly", outlineCode.leafOnly);
            appendTextElement(builder, 3, "AllLevelsRequired", outlineCode.allLevelsRequired);
            appendOutlineCodeMasks(builder, outlineCode.masks);
            appendOutlineCodeValues(builder, outlineCode.values);
            builder.append("    </OutlineCode>\n");
        }
        builder.append("  </OutlineCodes>\n");
    }

    private void appendOutlineCodeMasks(StringBuilder builder, java.util.List<OutlineCodeMaskModel> masks) {
        if (masks == null || masks.isEmpty()) {
            return;
        }
        builder.append("      <Masks>\n");
        for (OutlineCodeMaskModel mask : masks) {
            builder.append("        <Mask>\n");
            appendTextElement(builder, 5, "Level", mask.level);
            appendTextElement(builder, 5, "Mask", mask.mask);
            appendTextElement(builder, 5, "Length", mask.length);
            appendTextElement(builder, 5, "Sequence", mask.sequence);
            builder.append("        </Mask>\n");
        }
        builder.append("      </Masks>\n");
    }

    private void appendOutlineCodeValues(StringBuilder builder, java.util.List<OutlineCodeValueModel> values) {
        if (values == null || values.isEmpty()) {
            return;
        }
        builder.append("      <Values>\n");
        for (OutlineCodeValueModel value : values) {
            builder.append("        <Value>\n");
            appendTextElement(builder, 5, "Value", value.value);
            appendTextElement(builder, 5, "Description", value.description);
            builder.append("        </Value>\n");
        }
        builder.append("      </Values>\n");
    }

    private void appendWbsMasks(StringBuilder builder, ProjectModel model) {
        if (model.project.wbsMasks == null || model.project.wbsMasks.isEmpty()) {
            return;
        }
        builder.append("  <WBSMasks>\n");
        for (WbsMaskModel wbsMask : model.project.wbsMasks) {
            builder.append("    <WBSMask>\n");
            appendTextElement(builder, 3, "Level", wbsMask.level);
            appendTextElement(builder, 3, "Mask", wbsMask.mask);
            appendTextElement(builder, 3, "Length", wbsMask.length);
            appendTextElement(builder, 3, "Sequence", wbsMask.sequence);
            builder.append("    </WBSMask>\n");
        }
        builder.append("  </WBSMasks>\n");
    }

    private void appendProjectExtendedAttributes(StringBuilder builder, ProjectModel model) {
        if (model.project.extendedAttributes == null || model.project.extendedAttributes.isEmpty()) {
            return;
        }
        builder.append("  <ExtendedAttributes>\n");
        for (ProjectExtendedAttributeModel attribute : model.project.extendedAttributes) {
            builder.append("    <ExtendedAttribute>\n");
            appendTextElement(builder, 3, "FieldID", attribute.fieldID);
            appendTextElement(builder, 3, "FieldName", attribute.fieldName);
            appendTextElement(builder, 3, "Alias", attribute.alias);
            appendTextElement(builder, 3, "CalculationType", attribute.calculationType);
            appendTextElement(builder, 3, "RestrictValues", attribute.restrictValues);
            appendTextElement(builder, 3, "AppendNewValues", attribute.appendNewValues);
            builder.append("    </ExtendedAttribute>\n");
        }
        builder.append("  </ExtendedAttributes>\n");
    }

    private void appendTaskExtendedAttributes(StringBuilder builder, TaskModel task) {
        if (task.extendedAttributes == null || task.extendedAttributes.isEmpty()) {
            return;
        }
        for (TaskExtendedAttributeModel attribute : task.extendedAttributes) {
            builder.append("      <ExtendedAttribute>\n");
            appendTextElement(builder, 4, "FieldID", attribute.fieldID);
            appendTextElement(builder, 4, "Value", attribute.value);
            builder.append("      </ExtendedAttribute>\n");
        }
    }

    private void appendResourceExtendedAttributes(StringBuilder builder, ResourceModel resource) {
        if (resource.extendedAttributes == null || resource.extendedAttributes.isEmpty()) {
            return;
        }
        for (ResourceExtendedAttributeModel attribute : resource.extendedAttributes) {
            builder.append("      <ExtendedAttribute>\n");
            appendTextElement(builder, 4, "FieldID", attribute.fieldID);
            appendTextElement(builder, 4, "Value", attribute.value);
            builder.append("      </ExtendedAttribute>\n");
        }
    }

    private void appendAssignmentExtendedAttributes(StringBuilder builder, AssignmentModel assignment) {
        if (assignment.extendedAttributes == null || assignment.extendedAttributes.isEmpty()) {
            return;
        }
        for (AssignmentExtendedAttributeModel attribute : assignment.extendedAttributes) {
            builder.append("      <ExtendedAttribute>\n");
            appendTextElement(builder, 4, "FieldID", attribute.fieldID);
            appendTextElement(builder, 4, "Value", attribute.value);
            builder.append("      </ExtendedAttribute>\n");
        }
    }

    private void appendTaskBaselines(StringBuilder builder, TaskModel task) {
        if (task.baselines == null || task.baselines.isEmpty()) {
            return;
        }
        for (TaskBaselineModel baseline : task.baselines) {
            builder.append("      <Baseline>\n");
            appendTextElement(builder, 4, "Number", baseline.number);
            appendTextElement(builder, 4, "Start", baseline.start);
            appendTextElement(builder, 4, "Finish", baseline.finish);
            appendTextElement(builder, 4, "Work", baseline.work);
            appendTextElement(builder, 4, "Cost", baseline.cost);
            builder.append("      </Baseline>\n");
        }
    }

    private void appendResourceBaselines(StringBuilder builder, ResourceModel resource) {
        if (resource.baselines == null || resource.baselines.isEmpty()) {
            return;
        }
        for (ResourceBaselineModel baseline : resource.baselines) {
            builder.append("      <Baseline>\n");
            appendTextElement(builder, 4, "Number", baseline.number);
            appendTextElement(builder, 4, "Start", baseline.start);
            appendTextElement(builder, 4, "Finish", baseline.finish);
            appendTextElement(builder, 4, "Work", baseline.work);
            appendTextElement(builder, 4, "Cost", baseline.cost);
            builder.append("      </Baseline>\n");
        }
    }

    private void appendAssignmentBaselines(StringBuilder builder, AssignmentModel assignment) {
        if (assignment.baselines == null || assignment.baselines.isEmpty()) {
            return;
        }
        for (AssignmentBaselineModel baseline : assignment.baselines) {
            builder.append("      <Baseline>\n");
            appendTextElement(builder, 4, "Number", baseline.number);
            appendTextElement(builder, 4, "Start", baseline.start);
            appendTextElement(builder, 4, "Finish", baseline.finish);
            appendTextElement(builder, 4, "Work", baseline.work);
            appendTextElement(builder, 4, "Cost", baseline.cost);
            builder.append("      </Baseline>\n");
        }
    }

    private void appendTaskTimephasedData(StringBuilder builder, TaskModel task) {
        if (task.timephasedData == null || task.timephasedData.isEmpty()) {
            return;
        }
        for (TaskTimephasedDataModel value : task.timephasedData) {
            builder.append("      <TimephasedData>\n");
            appendTextElement(builder, 4, "Type", value.type);
            appendTextElement(builder, 4, "UID", value.uid);
            appendTextElement(builder, 4, "Start", value.start);
            appendTextElement(builder, 4, "Finish", value.finish);
            appendTextElement(builder, 4, "Unit", value.unit);
            appendTextElement(builder, 4, "Value", value.value);
            builder.append("      </TimephasedData>\n");
        }
    }

    private void appendResourceTimephasedData(StringBuilder builder, ResourceModel resource) {
        if (resource.timephasedData == null || resource.timephasedData.isEmpty()) {
            return;
        }
        for (ResourceTimephasedDataModel value : resource.timephasedData) {
            builder.append("      <TimephasedData>\n");
            appendTextElement(builder, 4, "Type", value.type);
            appendTextElement(builder, 4, "UID", value.uid);
            appendTextElement(builder, 4, "Start", value.start);
            appendTextElement(builder, 4, "Finish", value.finish);
            appendTextElement(builder, 4, "Unit", value.unit);
            appendTextElement(builder, 4, "Value", value.value);
            builder.append("      </TimephasedData>\n");
        }
    }

    private void appendAssignmentTimephasedData(StringBuilder builder, AssignmentModel assignment) {
        if (assignment.timephasedData == null || assignment.timephasedData.isEmpty()) {
            return;
        }
        for (AssignmentTimephasedDataModel value : assignment.timephasedData) {
            builder.append("      <TimephasedData>\n");
            appendTextElement(builder, 4, "Type", value.type);
            appendTextElement(builder, 4, "UID", value.uid);
            appendTextElement(builder, 4, "Start", value.start);
            appendTextElement(builder, 4, "Finish", value.finish);
            appendTextElement(builder, 4, "Unit", value.unit);
            appendTextElement(builder, 4, "Value", value.value);
            builder.append("      </TimephasedData>\n");
        }
    }

    private void appendWeekDays(StringBuilder builder, int indentLevel, java.util.List<WeekDayModel> weekDays) {
        if (weekDays == null || weekDays.isEmpty()) {
            return;
        }
        appendIndent(builder, indentLevel);
        builder.append("<WeekDays>\n");
        for (WeekDayModel weekDay : weekDays) {
            appendIndent(builder, indentLevel + 1);
            builder.append("<WeekDay>\n");
            appendTextElement(builder, indentLevel + 2, "DayType", weekDay.dayType);
            appendTextElement(builder, indentLevel + 2, "DayWorking", Boolean.valueOf(weekDay.dayWorking));
            appendWorkingTimes(builder, indentLevel + 2, weekDay.workingTimes);
            appendIndent(builder, indentLevel + 1);
            builder.append("</WeekDay>\n");
        }
        appendIndent(builder, indentLevel);
        builder.append("</WeekDays>\n");
    }

    private void appendExceptions(StringBuilder builder, int indentLevel, java.util.List<CalendarExceptionModel> exceptions) {
        if (exceptions == null || exceptions.isEmpty()) {
            return;
        }
        appendIndent(builder, indentLevel);
        builder.append("<Exceptions>\n");
        for (CalendarExceptionModel exception : exceptions) {
            appendIndent(builder, indentLevel + 1);
            builder.append("<Exception>\n");
            appendTextElement(builder, indentLevel + 2, "Name", exception.name);
            appendTextElement(builder, indentLevel + 2, "FromDate", exception.fromDate);
            appendTextElement(builder, indentLevel + 2, "ToDate", exception.toDate);
            appendTextElement(builder, indentLevel + 2, "DayWorking", exception.dayWorking);
            appendWorkingTimes(builder, indentLevel + 2, exception.workingTimes);
            appendIndent(builder, indentLevel + 1);
            builder.append("</Exception>\n");
        }
        appendIndent(builder, indentLevel);
        builder.append("</Exceptions>\n");
    }

    private void appendWorkWeeks(StringBuilder builder, int indentLevel, java.util.List<WorkWeekModel> workWeeks) {
        if (workWeeks == null || workWeeks.isEmpty()) {
            return;
        }
        appendIndent(builder, indentLevel);
        builder.append("<WorkWeeks>\n");
        for (WorkWeekModel workWeek : workWeeks) {
            appendIndent(builder, indentLevel + 1);
            builder.append("<WorkWeek>\n");
            appendTextElement(builder, indentLevel + 2, "Name", workWeek.name);
            appendTextElement(builder, indentLevel + 2, "FromDate", workWeek.fromDate);
            appendTextElement(builder, indentLevel + 2, "ToDate", workWeek.toDate);
            appendWeekDays(builder, indentLevel + 2, workWeek.weekDays);
            appendIndent(builder, indentLevel + 1);
            builder.append("</WorkWeek>\n");
        }
        appendIndent(builder, indentLevel);
        builder.append("</WorkWeeks>\n");
    }

    private void appendWorkingTimes(StringBuilder builder, int indentLevel, java.util.List<WorkingTimeModel> workingTimes) {
        if (workingTimes == null || workingTimes.isEmpty()) {
            return;
        }
        appendIndent(builder, indentLevel);
        builder.append("<WorkingTimes>\n");
        for (WorkingTimeModel workingTime : workingTimes) {
            appendIndent(builder, indentLevel + 1);
            builder.append("<WorkingTime>\n");
            appendTextElement(builder, indentLevel + 2, "FromTime", workingTime.fromTime);
            appendTextElement(builder, indentLevel + 2, "ToTime", workingTime.toTime);
            appendIndent(builder, indentLevel + 1);
            builder.append("</WorkingTime>\n");
        }
        appendIndent(builder, indentLevel);
        builder.append("</WorkingTimes>\n");
    }

    private void appendTextElement(StringBuilder builder, int indentLevel, String name, Object value) {
        if (value == null) {
            return;
        }
        String text = String.valueOf(value);
        if (value instanceof Boolean) {
            text = ((Boolean) value).booleanValue() ? "1" : "0";
        }
        if (text.isEmpty()) {
            return;
        }
        for (int index = 0; index < indentLevel; index += 1) {
            builder.append("  ");
        }
        builder.append("<").append(name).append(">");
        builder.append(escapeXml(text));
        builder.append("</").append(name).append(">\n");
    }

    private void appendIndent(StringBuilder builder, int indentLevel) {
        for (int index = 0; index < indentLevel; index += 1) {
            builder.append("  ");
        }
    }

    private String escapeXml(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}
