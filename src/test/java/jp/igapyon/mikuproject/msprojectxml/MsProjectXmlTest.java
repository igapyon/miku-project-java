package jp.igapyon.mikuproject.msprojectxml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import jp.igapyon.mikuproject.model.ProjectModel;
import jp.igapyon.mikuproject.model.ValidationIssue;

public class MsProjectXmlTest {
    @Test
    public void importFromXmlReadsProjectCoreFields() {
        MsProjectXml xml = new MsProjectXml();

        ProjectModel model = xml.importFromXml(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                        + "<Project>"
                        + "<Name>Sample Project</Name>"
                        + "<Title>Sample Title</Title>"
                        + "<Author>Sample Author</Author>"
                        + "<Company>Sample Company</Company>"
                        + "<StartDate>2026-04-01T09:00:00</StartDate>"
                        + "<FinishDate>2026-04-30T18:00:00</FinishDate>"
                        + "<DefaultStartTime>09:00:00</DefaultStartTime>"
                        + "<DefaultFinishTime>18:00:00</DefaultFinishTime>"
                        + "<MinutesPerDay>480</MinutesPerDay>"
                        + "<MinutesPerWeek>2400</MinutesPerWeek>"
                        + "<DaysPerMonth>20</DaysPerMonth>"
                        + "<StatusDate>2026-04-15T09:00:00</StatusDate>"
                        + "<WeekStartDay>2</WeekStartDay>"
                        + "<WorkFormat>2</WorkFormat>"
                        + "<DurationFormat>7</DurationFormat>"
                        + "<ScheduleFromStart>1</ScheduleFromStart>"
                        + "</Project>");

        assertNotNull(model);
        assertEquals("Sample Project", model.project.name);
        assertEquals("Sample Title", model.project.title);
        assertEquals("Sample Author", model.project.author);
        assertEquals("Sample Company", model.project.company);
        assertEquals("2026-04-01T09:00:00", model.project.startDate);
        assertEquals("2026-04-30T18:00:00", model.project.finishDate);
        assertEquals("09:00:00", model.project.defaultStartTime);
        assertEquals("18:00:00", model.project.defaultFinishTime);
        assertEquals(Integer.valueOf(480), model.project.minutesPerDay);
        assertEquals(Integer.valueOf(2400), model.project.minutesPerWeek);
        assertEquals(Integer.valueOf(20), model.project.daysPerMonth);
        assertEquals("2026-04-15T09:00:00", model.project.statusDate);
        assertEquals(Integer.valueOf(2), model.project.weekStartDay);
        assertEquals(Integer.valueOf(2), model.project.workFormat);
        assertEquals(Integer.valueOf(7), model.project.durationFormat);
        assertTrue(model.project.scheduleFromStart);
        assertFalse(model.calendars.isEmpty());
    }

    @Test
    public void importFromXmlReadsFirstCutEntities() {
        MsProjectXml xml = new MsProjectXml();

        ProjectModel model = xml.importFromXml(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                        + "<Project>"
                        + "<Name>Sample Project</Name>"
                        + "<StartDate>2026-04-01T09:00:00</StartDate>"
                        + "<FinishDate>2026-04-30T18:00:00</FinishDate>"
                        + "<ScheduleFromStart>1</ScheduleFromStart>"
                        + "<Calendars>"
                        + "<Calendar><UID>10</UID><Name>Standard</Name><IsBaseCalendar>1</IsBaseCalendar></Calendar>"
                        + "</Calendars>"
                        + "<Tasks>"
                        + "<Task><UID>1</UID><ID>1</ID><Name>Task A</Name><OutlineLevel>1</OutlineLevel><OutlineNumber>1</OutlineNumber><Type>2</Type><Priority>500</Priority><Start>2026-04-01T09:00:00</Start><Finish>2026-04-02T18:00:00</Finish><Duration>PT16H0M0S</Duration><ActualStart>2026-04-01T09:00:00</ActualStart><ActualFinish>2026-04-02T18:00:00</ActualFinish><Deadline>2026-04-03T18:00:00</Deadline><Milestone>0</Milestone><Summary>0</Summary><Critical>1</Critical><PercentComplete>50</PercentComplete><PercentWorkComplete>60</PercentWorkComplete><ConstraintType>4</ConstraintType><ConstraintDate>2026-04-03T09:00:00</ConstraintDate><PredecessorLink><PredecessorUID>99</PredecessorUID><Type>1</Type><LinkLag>PT0H0M0S</LinkLag></PredecessorLink></Task>"
                        + "</Tasks>"
                        + "<Resources>"
                        + "<Resource><UID>2</UID><ID>2</ID><Name>Res A</Name><Group>Dev</Group><MaxUnits>1</MaxUnits></Resource>"
                        + "</Resources>"
                        + "<Assignments>"
                        + "<Assignment><UID>3</UID><TaskUID>1</TaskUID><ResourceUID>2</ResourceUID><Units>1</Units><Work>PT8H0M0S</Work><PercentWorkComplete>50</PercentWorkComplete></Assignment>"
                        + "</Assignments>"
                        + "</Project>");

        assertEquals(1, model.tasks.size());
        assertEquals(1, model.resources.size());
        assertEquals(1, model.assignments.size());
        assertFalse(model.calendars.isEmpty());
        assertEquals("Task A", model.tasks.get(0).name);
        assertEquals(Integer.valueOf(2), model.tasks.get(0).type);
        assertEquals(Integer.valueOf(500), model.tasks.get(0).priority);
        assertEquals("2026-04-01T09:00:00", model.tasks.get(0).actualStart);
        assertEquals("2026-04-02T18:00:00", model.tasks.get(0).actualFinish);
        assertEquals("2026-04-03T18:00:00", model.tasks.get(0).deadline);
        assertEquals(Boolean.TRUE, model.tasks.get(0).critical);
        assertEquals(Integer.valueOf(50), model.tasks.get(0).percentComplete);
        assertEquals(Integer.valueOf(60), model.tasks.get(0).percentWorkComplete);
        assertEquals(Integer.valueOf(4), model.tasks.get(0).constraintType);
        assertEquals("2026-04-03T09:00:00", model.tasks.get(0).constraintDate);
        assertEquals("99", model.tasks.get(0).predecessors.get(0).predecessorUid);
        assertEquals("Res A", model.resources.get(0).name);
        assertEquals(Double.valueOf(1.0d), model.assignments.get(0).units);
    }

    @Test
    public void importFromXmlReadsCalendarStructures() {
        MsProjectXml xml = new MsProjectXml();

        ProjectModel model = xml.importFromXml(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                        + "<Project>"
                        + "<Name>Sample Project</Name>"
                        + "<StartDate>2026-04-01T09:00:00</StartDate>"
                        + "<FinishDate>2026-04-30T18:00:00</FinishDate>"
                        + "<ScheduleFromStart>1</ScheduleFromStart>"
                        + "<Calendars>"
                        + "<Calendar>"
                        + "<UID>10</UID><Name>Standard</Name><IsBaseCalendar>1</IsBaseCalendar>"
                        + "<WeekDays><WeekDay><DayType>2</DayType><DayWorking>1</DayWorking><WorkingTimes><WorkingTime><FromTime>09:00:00</FromTime><ToTime>12:00:00</ToTime></WorkingTime></WorkingTimes></WeekDay></WeekDays>"
                        + "<Exceptions><Exception><Name>Holiday</Name><FromDate>2026-04-10T00:00:00</FromDate><ToDate>2026-04-10T23:59:59</ToDate><DayWorking>0</DayWorking></Exception></Exceptions>"
                        + "<WorkWeeks><WorkWeek><Name>Golden Week</Name><FromDate>2026-05-01T00:00:00</FromDate><ToDate>2026-05-08T23:59:59</ToDate><WeekDays><WeekDay><DayType>2</DayType><DayWorking>0</DayWorking></WeekDay></WeekDays></WorkWeek></WorkWeeks>"
                        + "</Calendar>"
                        + "</Calendars>"
                        + "</Project>");

        assertEquals(1, model.calendars.size());
        assertEquals(1, model.calendars.get(0).weekDays.size());
        assertEquals(1, model.calendars.get(0).exceptions.size());
        assertEquals(1, model.calendars.get(0).workWeeks.size());
        assertEquals("Holiday", model.calendars.get(0).exceptions.get(0).name);
        assertEquals("Golden Week", model.calendars.get(0).workWeeks.get(0).name);
    }

    @Test
    public void exportToXmlWritesFirstCutEntities() {
        MsProjectXml xml = new MsProjectXml();
        ProjectModel model = new ProjectModel();

        model.project.name = "Sample Project";
        model.project.title = "Sample Title";
        model.project.author = "Sample Author";
        model.project.company = "Sample Company";
        model.project.startDate = "2026-04-01T09:00:00";
        model.project.finishDate = "2026-04-30T18:00:00";
        model.project.defaultStartTime = "09:00:00";
        model.project.defaultFinishTime = "18:00:00";
        model.project.minutesPerDay = Integer.valueOf(480);
        model.project.minutesPerWeek = Integer.valueOf(2400);
        model.project.daysPerMonth = Integer.valueOf(20);
        model.project.statusDate = "2026-04-15T09:00:00";
        model.project.weekStartDay = Integer.valueOf(2);
        model.project.workFormat = Integer.valueOf(2);
        model.project.durationFormat = Integer.valueOf(7);
        model.project.scheduleFromStart = true;

        jp.igapyon.mikuproject.model.CalendarModel calendar = new jp.igapyon.mikuproject.model.CalendarModel();
        calendar.uid = "10";
        calendar.name = "Standard";
        calendar.isBaseCalendar = true;
        model.calendars.add(calendar);

        jp.igapyon.mikuproject.model.TaskModel task = new jp.igapyon.mikuproject.model.TaskModel();
        task.uid = "1";
        task.id = "1";
        task.name = "Task A";
        task.outlineLevel = Integer.valueOf(1);
        task.outlineNumber = "1";
        task.type = Integer.valueOf(2);
        task.priority = Integer.valueOf(500);
        task.start = "2026-04-01T09:00:00";
        task.finish = "2026-04-02T18:00:00";
        task.duration = "PT16H0M0S";
        task.actualStart = "2026-04-01T09:00:00";
        task.actualFinish = "2026-04-02T18:00:00";
        task.deadline = "2026-04-03T18:00:00";
        task.critical = Boolean.TRUE;
        task.percentComplete = Integer.valueOf(50);
        task.percentWorkComplete = Integer.valueOf(60);
        task.constraintType = Integer.valueOf(4);
        task.constraintDate = "2026-04-03T09:00:00";
        jp.igapyon.mikuproject.model.PredecessorModel predecessor = new jp.igapyon.mikuproject.model.PredecessorModel();
        predecessor.predecessorUid = "99";
        task.predecessors.add(predecessor);
        model.tasks.add(task);

        jp.igapyon.mikuproject.model.ResourceModel resource = new jp.igapyon.mikuproject.model.ResourceModel();
        resource.uid = "2";
        resource.id = "2";
        resource.name = "Res A";
        resource.group = "Dev";
        resource.maxUnits = Double.valueOf(1.0d);
        model.resources.add(resource);

        jp.igapyon.mikuproject.model.AssignmentModel assignment = new jp.igapyon.mikuproject.model.AssignmentModel();
        assignment.uid = "3";
        assignment.taskUid = "1";
        assignment.resourceUid = "2";
        assignment.units = Double.valueOf(1.0d);
        assignment.work = "PT8H0M0S";
        assignment.percentWorkComplete = Integer.valueOf(50);
        model.assignments.add(assignment);

        String xmlText = xml.exportToXml(model);

        assertTrue(xmlText.contains("<Calendar>"));
        assertTrue(xmlText.contains("<Task>"));
        assertTrue(xmlText.contains("<Resource>"));
        assertTrue(xmlText.contains("<Assignment>"));
        assertTrue(xmlText.contains("<Title>Sample Title</Title>"));
        assertTrue(xmlText.contains("<MinutesPerDay>480</MinutesPerDay>"));
        assertTrue(xmlText.contains("<WeekStartDay>2</WeekStartDay>"));
        assertTrue(xmlText.contains("<Type>2</Type>"));
        assertTrue(xmlText.contains("<Priority>500</Priority>"));
        assertTrue(xmlText.contains("<ActualStart>2026-04-01T09:00:00</ActualStart>"));
        assertTrue(xmlText.contains("<Critical>1</Critical>"));
        assertTrue(xmlText.contains("<PercentWorkComplete>60</PercentWorkComplete>"));
        assertTrue(xmlText.contains("<ConstraintType>4</ConstraintType>"));
        assertTrue(xmlText.contains("<PredecessorUID>99</PredecessorUID>"));
    }

    @Test
    public void exportToXmlWritesCalendarStructures() {
        MsProjectXml xml = new MsProjectXml();
        ProjectModel model = new ProjectModel();
        model.project.name = "Sample Project";
        model.project.startDate = "2026-04-01T09:00:00";
        model.project.finishDate = "2026-04-30T18:00:00";
        model.project.scheduleFromStart = true;

        jp.igapyon.mikuproject.model.CalendarModel calendar = new jp.igapyon.mikuproject.model.CalendarModel();
        calendar.uid = "10";
        calendar.name = "Standard";
        calendar.isBaseCalendar = true;

        jp.igapyon.mikuproject.model.WeekDayModel weekDay = new jp.igapyon.mikuproject.model.WeekDayModel();
        weekDay.dayType = Integer.valueOf(2);
        weekDay.dayWorking = true;
        jp.igapyon.mikuproject.model.WorkingTimeModel workingTime = new jp.igapyon.mikuproject.model.WorkingTimeModel();
        workingTime.fromTime = "09:00:00";
        workingTime.toTime = "12:00:00";
        weekDay.workingTimes.add(workingTime);
        calendar.weekDays.add(weekDay);

        jp.igapyon.mikuproject.model.CalendarExceptionModel exception = new jp.igapyon.mikuproject.model.CalendarExceptionModel();
        exception.name = "Holiday";
        exception.fromDate = "2026-04-10T00:00:00";
        exception.toDate = "2026-04-10T23:59:59";
        exception.dayWorking = Boolean.FALSE;
        calendar.exceptions.add(exception);

        jp.igapyon.mikuproject.model.WorkWeekModel workWeek = new jp.igapyon.mikuproject.model.WorkWeekModel();
        workWeek.name = "Golden Week";
        workWeek.fromDate = "2026-05-01T00:00:00";
        workWeek.toDate = "2026-05-08T23:59:59";
        workWeek.weekDays.add(weekDay);
        calendar.workWeeks.add(workWeek);

        model.calendars.add(calendar);

        String xmlText = xml.exportToXml(model);

        assertTrue(xmlText.contains("<WeekDays>"));
        assertTrue(xmlText.contains("<WorkingTimes>"));
        assertTrue(xmlText.contains("<Exceptions>"));
        assertTrue(xmlText.contains("<WorkWeeks>"));
        assertTrue(xmlText.contains("<Name>Golden Week</Name>"));
    }

    @Test
    public void validateProjectModelReturnsWarningsForMissingCoreFields() {
        MsProjectXml xml = new MsProjectXml();

        List<ValidationIssue> issues = xml.validateProjectModel(new ProjectModel());

        assertNotNull(issues);
        assertFalse(issues.isEmpty());
    }

    @Test
    public void validateProjectModelChecksProjectRanges() {
        MsProjectXml xml = new MsProjectXml();
        ProjectModel model = new ProjectModel();

        model.project.name = "Sample Project";
        model.project.startDate = "2026-04-01T09:00:00";
        model.project.finishDate = "2026-04-30T18:00:00";
        model.project.minutesPerDay = Integer.valueOf(0);
        model.project.minutesPerWeek = Integer.valueOf(-1);
        model.project.daysPerMonth = Integer.valueOf(0);
        model.project.weekStartDay = Integer.valueOf(8);
        model.project.workFormat = Integer.valueOf(-1);
        model.project.durationFormat = Integer.valueOf(-1);

        List<ValidationIssue> issues = xml.validateProjectModel(model);

        assertTrue(containsMessage(issues, "Project MinutesPerDay は正の値が望ましいです"));
        assertTrue(containsMessage(issues, "Project MinutesPerWeek は正の値が望ましいです"));
        assertTrue(containsMessage(issues, "Project DaysPerMonth は正の値が望ましいです"));
        assertTrue(containsMessage(issues, "Project WeekStartDay は 1..7 が望ましいです"));
        assertTrue(containsMessage(issues, "Project WorkFormat は 0 以上が望ましいです"));
        assertTrue(containsMessage(issues, "Project DurationFormat は 0 以上が望ましいです"));
    }

    @Test
    public void validateProjectModelChecksFirstCutEntityReferences() {
        MsProjectXml xml = new MsProjectXml();
        ProjectModel model = new ProjectModel();

        model.project.name = "Sample Project";
        model.project.startDate = "2026-04-01T09:00:00";
        model.project.finishDate = "2026-04-30T18:00:00";
        model.project.calendarUID = "missing-calendar";

        jp.igapyon.mikuproject.model.TaskModel task = new jp.igapyon.mikuproject.model.TaskModel();
        task.uid = "1";
        task.id = "1";
        task.name = "Task A";
        task.start = "2026-04-01T09:00:00";
        task.finish = "2026-04-02T18:00:00";
        task.percentComplete = Integer.valueOf(120);
        task.percentWorkComplete = Integer.valueOf(130);
        task.type = Integer.valueOf(-1);
        task.priority = Integer.valueOf(-1);
        task.constraintType = Integer.valueOf(-1);
        task.calendarUID = "missing-calendar";
        model.tasks.add(task);

        jp.igapyon.mikuproject.model.ResourceModel resource = new jp.igapyon.mikuproject.model.ResourceModel();
        resource.uid = "2";
        resource.id = "2";
        resource.name = "Res A";
        resource.maxUnits = Double.valueOf(-1.0d);
        resource.calendarUID = "missing-calendar";
        model.resources.add(resource);

        jp.igapyon.mikuproject.model.AssignmentModel assignment = new jp.igapyon.mikuproject.model.AssignmentModel();
        assignment.uid = "3";
        assignment.taskUid = "missing-task";
        assignment.resourceUid = "missing-resource";
        assignment.units = Double.valueOf(-1.0d);
        assignment.percentWorkComplete = Integer.valueOf(150);
        model.assignments.add(assignment);

        List<ValidationIssue> issues = xml.validateProjectModel(model);

        assertFalse(issues.isEmpty());
        assertTrue(containsMessage(issues, "Project CalendarUID が既存 Calendar を指していません"));
        assertTrue(containsMessage(issues, "Task CalendarUID が既存 Calendar を指していません"));
        assertTrue(containsMessage(issues, "Task PercentComplete が 0..100 の範囲外です"));
        assertTrue(containsMessage(issues, "Task PercentWorkComplete が 0..100 の範囲外です"));
        assertTrue(containsMessage(issues, "Task Type は 0 以上が望ましいです"));
        assertTrue(containsMessage(issues, "Task Priority は 0 以上が望ましいです"));
        assertTrue(containsMessage(issues, "Task ConstraintType は 0 以上が望ましいです"));
        assertTrue(containsMessage(issues, "Resource CalendarUID が既存 Calendar を指していません"));
        assertTrue(containsMessage(issues, "Resource MaxUnits は 0 以上が望ましいです"));
        assertTrue(containsMessage(issues, "Assignment TaskUID が既存 Task を指していません"));
        assertTrue(containsMessage(issues, "Assignment ResourceUID が既存 Resource を指していません"));
        assertTrue(containsMessage(issues, "Assignment Units は 0 以上が望ましいです"));
        assertTrue(containsMessage(issues, "Assignment PercentWorkComplete が 0..100 の範囲外です"));
    }

    private boolean containsMessage(List<ValidationIssue> issues, String fragment) {
        for (ValidationIssue issue : issues) {
            if (issue.message != null && issue.message.contains(fragment)) {
                return true;
            }
        }
        return false;
    }
}
