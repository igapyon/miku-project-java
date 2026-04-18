package jp.igapyon.mikuproject.msprojectxml;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import jp.igapyon.mikuproject.model.AssignmentModel;
import jp.igapyon.mikuproject.model.CalendarExceptionModel;
import jp.igapyon.mikuproject.model.CalendarModel;
import jp.igapyon.mikuproject.model.PredecessorModel;
import jp.igapyon.mikuproject.model.ProjectModel;
import jp.igapyon.mikuproject.model.ResourceModel;
import jp.igapyon.mikuproject.model.TaskModel;
import jp.igapyon.mikuproject.model.WeekDayModel;
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
        model.project.startDate = xmlDom.textContent(projectElement, "StartDate");
        model.project.finishDate = xmlDom.textContent(projectElement, "FinishDate");
        model.project.currentDate = xmlDom.textContent(projectElement, "CurrentDate");
        model.project.defaultStartTime = xmlDom.textContent(projectElement, "DefaultStartTime");
        model.project.defaultFinishTime = xmlDom.textContent(projectElement, "DefaultFinishTime");
        model.project.statusDate = xmlDom.textContent(projectElement, "StatusDate");

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

        model.project.calendarUID = xmlDom.textContent(projectElement, "CalendarUID");
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
        appendTextElement(builder, 1, "Author", model.project.author);
        appendTextElement(builder, 1, "Company", model.project.company);
        appendTextElement(builder, 1, "CurrentDate", model.project.currentDate);
        appendTextElement(builder, 1, "StartDate", model.project.startDate);
        appendTextElement(builder, 1, "FinishDate", model.project.finishDate);
        appendTextElement(builder, 1, "DefaultStartTime", model.project.defaultStartTime);
        appendTextElement(builder, 1, "DefaultFinishTime", model.project.defaultFinishTime);
        appendTextElement(builder, 1, "MinutesPerDay", model.project.minutesPerDay);
        appendTextElement(builder, 1, "MinutesPerWeek", model.project.minutesPerWeek);
        appendTextElement(builder, 1, "DaysPerMonth", model.project.daysPerMonth);
        appendTextElement(builder, 1, "StatusDate", model.project.statusDate);
        appendTextElement(builder, 1, "WeekStartDay", model.project.weekStartDay);
        appendTextElement(builder, 1, "WorkFormat", model.project.workFormat);
        appendTextElement(builder, 1, "DurationFormat", model.project.durationFormat);
        builder.append("  <ScheduleFromStart>").append(model.project.scheduleFromStart ? "1" : "0").append("</ScheduleFromStart>\n");
        appendTextElement(builder, 1, "CalendarUID", model.project.calendarUID);
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

            String constraintType = xmlDom.textContent(taskElement, "ConstraintType");
            if (!constraintType.isEmpty()) {
                task.constraintType = Integer.valueOf(xmlDom.parseNumber(constraintType, 0));
            }

            task.constraintDate = xmlDom.textContent(taskElement, "ConstraintDate");

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
            resource.group = xmlDom.textContent(resourceElement, "Group");
            resource.calendarUID = xmlDom.textContent(resourceElement, "CalendarUID");

            String maxUnits = xmlDom.textContent(resourceElement, "MaxUnits");
            if (!maxUnits.isEmpty()) {
                resource.maxUnits = Double.valueOf(xmlDom.parseDouble(maxUnits, 0.0d));
            }

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
            assignment.work = xmlDom.textContent(assignmentElement, "Work");

            String units = xmlDom.textContent(assignmentElement, "Units");
            if (!units.isEmpty()) {
                assignment.units = Double.valueOf(xmlDom.parseDouble(units, 0.0d));
            }

            String percentWorkComplete = xmlDom.textContent(assignmentElement, "PercentWorkComplete");
            if (!percentWorkComplete.isEmpty()) {
                assignment.percentWorkComplete = Integer.valueOf(xmlDom.parseNumber(percentWorkComplete, 0));
            }

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
            appendTextElement(builder, 3, "Priority", task.priority);
            appendTextElement(builder, 3, "CalendarUID", task.calendarUID);
            appendTextElement(builder, 3, "Start", task.start);
            appendTextElement(builder, 3, "Finish", task.finish);
            appendTextElement(builder, 3, "Duration", task.duration);
            appendTextElement(builder, 3, "ActualStart", task.actualStart);
            appendTextElement(builder, 3, "ActualFinish", task.actualFinish);
            appendTextElement(builder, 3, "Deadline", task.deadline);
            appendTextElement(builder, 3, "Milestone", Boolean.valueOf(task.milestone));
            appendTextElement(builder, 3, "Summary", Boolean.valueOf(task.summary));
            appendTextElement(builder, 3, "Critical", task.critical);
            appendTextElement(builder, 3, "PercentComplete", task.percentComplete);
            appendTextElement(builder, 3, "PercentWorkComplete", task.percentWorkComplete);
            appendTextElement(builder, 3, "ConstraintType", task.constraintType);
            appendTextElement(builder, 3, "ConstraintDate", task.constraintDate);
            appendTextElement(builder, 3, "Notes", task.notes);
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
            appendTextElement(builder, 3, "Group", resource.group);
            appendTextElement(builder, 3, "MaxUnits", resource.maxUnits);
            appendTextElement(builder, 3, "CalendarUID", resource.calendarUID);
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
            appendTextElement(builder, 3, "Units", assignment.units);
            appendTextElement(builder, 3, "Work", assignment.work);
            appendTextElement(builder, 3, "PercentWorkComplete", assignment.percentWorkComplete);
            builder.append("    </Assignment>\n");
        }
        builder.append("  </Assignments>\n");
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
