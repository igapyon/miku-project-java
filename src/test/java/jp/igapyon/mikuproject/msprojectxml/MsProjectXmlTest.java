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
                        + "<CreationDate>2026-03-01T09:00:00</CreationDate>"
                        + "<LastSaved>2026-03-31T18:00:00</LastSaved>"
                        + "<SaveVersion>15</SaveVersion>"
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
                        + "<CurrencyCode>JPY</CurrencyCode>"
                        + "<CurrencyDigits>0</CurrencyDigits>"
                        + "<CurrencySymbol>¥</CurrencySymbol>"
                        + "<CurrencySymbolPosition>0</CurrencySymbolPosition>"
                        + "<FYStartDate>2026-01-01T00:00:00</FYStartDate>"
                        + "<FiscalYearStart>1</FiscalYearStart>"
                        + "<CriticalSlackLimit>0</CriticalSlackLimit>"
                        + "<DefaultTaskType>1</DefaultTaskType>"
                        + "<DefaultFixedCostAccrual>2</DefaultFixedCostAccrual>"
                        + "<DefaultStandardRate>1000/h</DefaultStandardRate>"
                        + "<DefaultOvertimeRate>1500/h</DefaultOvertimeRate>"
                        + "<DefaultTaskEVMethod>0</DefaultTaskEVMethod>"
                        + "<NewTaskStartDate>1</NewTaskStartDate>"
                        + "<NewTasksAreManual>0</NewTasksAreManual>"
                        + "<NewTasksEffortDriven>1</NewTasksEffortDriven>"
                        + "<NewTasksEstimated>1</NewTasksEstimated>"
                        + "<ActualsInSync>1</ActualsInSync>"
                        + "<EditableActualCosts>0</EditableActualCosts>"
                        + "<HonorConstraints>1</HonorConstraints>"
                        + "<InsertedProjectsLikeSummary>1</InsertedProjectsLikeSummary>"
                        + "<MultipleCriticalPaths>0</MultipleCriticalPaths>"
                        + "<TaskUpdatesResource>1</TaskUpdatesResource>"
                        + "<UpdateManuallyScheduledTasksWhenEditingLinks>0</UpdateManuallyScheduledTasksWhenEditingLinks>"
                        + "<OutlineCodes><OutlineCode><FieldID>188743731</FieldID><FieldName>OutlineCode1</FieldName><Alias>Phase</Alias><OnlyTableValues>1</OnlyTableValues><Masks><Mask><Level>1</Level><Mask>AA</Mask><Length>2</Length><Sequence>1</Sequence></Mask></Masks><Values><Value><Value>A1</Value><Description>Phase A</Description></Value></Values></OutlineCode></OutlineCodes>"
                        + "<WBSMasks><WBSMask><Level>1</Level><Mask>##</Mask><Length>2</Length><Sequence>1</Sequence></WBSMask></WBSMasks>"
                        + "<ExtendedAttributes><ExtendedAttribute><FieldID>188743734</FieldID><FieldName>Text1</FieldName><Alias>Memo</Alias><CalculationType>0</CalculationType><RestrictValues>0</RestrictValues><AppendNewValues>1</AppendNewValues></ExtendedAttribute></ExtendedAttributes>"
                        + "<ScheduleFromStart>1</ScheduleFromStart>"
                        + "</Project>");

        assertNotNull(model);
        assertEquals("Sample Project", model.project.name);
        assertEquals("Sample Title", model.project.title);
        assertEquals("Sample Author", model.project.author);
        assertEquals("Sample Company", model.project.company);
        assertEquals("2026-03-01T09:00:00", model.project.creationDate);
        assertEquals("2026-03-31T18:00:00", model.project.lastSaved);
        assertEquals(Integer.valueOf(15), model.project.saveVersion);
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
        assertEquals("JPY", model.project.currencyCode);
        assertEquals(Integer.valueOf(0), model.project.currencyDigits);
        assertEquals("¥", model.project.currencySymbol);
        assertEquals(Integer.valueOf(0), model.project.currencySymbolPosition);
        assertEquals("2026-01-01T00:00:00", model.project.fyStartDate);
        assertEquals(Boolean.TRUE, model.project.fiscalYearStart);
        assertEquals(Integer.valueOf(0), model.project.criticalSlackLimit);
        assertEquals(Integer.valueOf(1), model.project.defaultTaskType);
        assertEquals(Integer.valueOf(2), model.project.defaultFixedCostAccrual);
        assertEquals("1000/h", model.project.defaultStandardRate);
        assertEquals("1500/h", model.project.defaultOvertimeRate);
        assertEquals(Integer.valueOf(0), model.project.defaultTaskEVMethod);
        assertEquals(Integer.valueOf(1), model.project.newTaskStartDate);
        assertEquals(Boolean.FALSE, model.project.newTasksAreManual);
        assertEquals(Boolean.TRUE, model.project.newTasksEffortDriven);
        assertEquals(Boolean.TRUE, model.project.newTasksEstimated);
        assertEquals(Boolean.TRUE, model.project.actualsInSync);
        assertEquals(Boolean.FALSE, model.project.editableActualCosts);
        assertEquals(Boolean.TRUE, model.project.honorConstraints);
        assertEquals(Boolean.TRUE, model.project.insertedProjectsLikeSummary);
        assertEquals(Boolean.FALSE, model.project.multipleCriticalPaths);
        assertEquals(Boolean.TRUE, model.project.taskUpdatesResource);
        assertEquals(Boolean.FALSE, model.project.updateManuallyScheduledTasksWhenEditingLinks);
        assertEquals(1, model.project.outlineCodes.size());
        assertEquals("OutlineCode1", model.project.outlineCodes.get(0).fieldName);
        assertEquals(1, model.project.wbsMasks.size());
        assertEquals("##", model.project.wbsMasks.get(0).mask);
        assertEquals(1, model.project.extendedAttributes.size());
        assertEquals("Memo", model.project.extendedAttributes.get(0).alias);
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
                        + "<Task><UID>1</UID><ID>1</ID><Name>Task A</Name><OutlineLevel>1</OutlineLevel><OutlineNumber>1</OutlineNumber><Type>2</Type><Priority>500</Priority><Start>2026-04-01T09:00:00</Start><Finish>2026-04-02T18:00:00</Finish><Duration>PT16H0M0S</Duration><ActualStart>2026-04-01T09:00:00</ActualStart><ActualFinish>2026-04-02T18:00:00</ActualFinish><Deadline>2026-04-03T18:00:00</Deadline><StartVariance>PT1H0M0S</StartVariance><FinishVariance>PT2H0M0S</FinishVariance><Work>PT16H0M0S</Work><WorkVariance>PT1H0M0S</WorkVariance><TotalSlack>PT4H0M0S</TotalSlack><FreeSlack>PT2H0M0S</FreeSlack><Cost>1200</Cost><ActualCost>700</ActualCost><RemainingCost>500</RemainingCost><RemainingWork>PT8H0M0S</RemainingWork><ActualWork>PT8H0M0S</ActualWork><Milestone>0</Milestone><Summary>0</Summary><Critical>1</Critical><PercentComplete>50</PercentComplete><PercentWorkComplete>60</PercentWorkComplete><ConstraintType>4</ConstraintType><ConstraintDate>2026-04-03T09:00:00</ConstraintDate><ExtendedAttributes><ExtendedAttribute><FieldID>188743734</FieldID><Value>Task Memo</Value></ExtendedAttribute></ExtendedAttributes><Baselines><Baseline><Number>0</Number><Start>2026-04-01T09:00:00</Start><Finish>2026-04-02T18:00:00</Finish><Work>PT16H0M0S</Work><Cost>1000</Cost></Baseline></Baselines><TimephasedData><TimephasedData><Type>1</Type><UID>501</UID><Start>2026-04-01T09:00:00</Start><Finish>2026-04-01T18:00:00</Finish><Unit>2</Unit><Value>PT8H0M0S</Value></TimephasedData></TimephasedData><PredecessorLink><PredecessorUID>99</PredecessorUID><Type>1</Type><LinkLag>PT0H0M0S</LinkLag></PredecessorLink></Task>"
                        + "</Tasks>"
                        + "<Resources>"
                        + "<Resource><UID>2</UID><ID>2</ID><Name>Res A</Name><Type>1</Type><Initials>RA</Initials><Group>Dev</Group><WorkGroup>0</WorkGroup><MaxUnits>1</MaxUnits><StandardRate>1000</StandardRate><StandardRateFormat>2</StandardRateFormat><OvertimeRate>1500</OvertimeRate><OvertimeRateFormat>2</OvertimeRateFormat><CostPerUse>10</CostPerUse><Work>PT8H0M0S</Work><ActualWork>PT4H0M0S</ActualWork><RemainingWork>PT4H0M0S</RemainingWork><Cost>1000</Cost><ActualCost>500</ActualCost><RemainingCost>500</RemainingCost><PercentWorkComplete>50</PercentWorkComplete><ExtendedAttributes><ExtendedAttribute><FieldID>188743735</FieldID><Value>Res Memo</Value></ExtendedAttribute></ExtendedAttributes><Baselines><Baseline><Number>0</Number><Start>2026-04-01T09:00:00</Start><Finish>2026-04-02T18:00:00</Finish><Work>PT8H0M0S</Work><Cost>500</Cost></Baseline></Baselines><TimephasedData><TimephasedData><Type>2</Type><UID>601</UID><Start>2026-04-01T09:00:00</Start><Finish>2026-04-01T18:00:00</Finish><Unit>3</Unit><Value>PT4H0M0S</Value></TimephasedData></TimephasedData></Resource>"
                        + "</Resources>"
                        + "<Assignments>"
                        + "<Assignment><UID>3</UID><TaskUID>1</TaskUID><ResourceUID>2</ResourceUID><Start>2026-04-01T09:00:00</Start><Finish>2026-04-02T18:00:00</Finish><StartVariance>PT0H0M0S</StartVariance><FinishVariance>PT0H0M0S</FinishVariance><Delay>PT0H0M0S</Delay><Milestone>0</Milestone><WorkContour>1</WorkContour><Units>1</Units><Work>PT8H0M0S</Work><Cost>1000</Cost><ActualCost>500</ActualCost><RemainingCost>500</RemainingCost><PercentWorkComplete>50</PercentWorkComplete><OvertimeWork>PT1H0M0S</OvertimeWork><ActualOvertimeWork>PT0H30M0S</ActualOvertimeWork><ActualWork>PT4H0M0S</ActualWork><RemainingWork>PT4H0M0S</RemainingWork><ExtendedAttributes><ExtendedAttribute><FieldID>188743736</FieldID><Value>Assign Memo</Value></ExtendedAttribute></ExtendedAttributes><Baselines><Baseline><Number>0</Number><Start>2026-04-01T09:00:00</Start><Finish>2026-04-02T18:00:00</Finish><Work>PT8H0M0S</Work><Cost>500</Cost></Baseline></Baselines><TimephasedData><TimephasedData><Type>3</Type><UID>701</UID><Start>2026-04-01T09:00:00</Start><Finish>2026-04-01T18:00:00</Finish><Unit>4</Unit><Value>PT4H0M0S</Value></TimephasedData></TimephasedData></Assignment>"
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
        assertEquals("PT1H0M0S", model.tasks.get(0).startVariance);
        assertEquals("PT2H0M0S", model.tasks.get(0).finishVariance);
        assertEquals("PT16H0M0S", model.tasks.get(0).work);
        assertEquals("PT1H0M0S", model.tasks.get(0).workVariance);
        assertEquals("PT4H0M0S", model.tasks.get(0).totalSlack);
        assertEquals("PT2H0M0S", model.tasks.get(0).freeSlack);
        assertEquals(Double.valueOf(1200.0d), model.tasks.get(0).cost);
        assertEquals(Double.valueOf(700.0d), model.tasks.get(0).actualCost);
        assertEquals(Double.valueOf(500.0d), model.tasks.get(0).remainingCost);
        assertEquals("PT8H0M0S", model.tasks.get(0).remainingWork);
        assertEquals("PT8H0M0S", model.tasks.get(0).actualWork);
        assertEquals(Boolean.TRUE, model.tasks.get(0).critical);
        assertEquals(Integer.valueOf(50), model.tasks.get(0).percentComplete);
        assertEquals(Integer.valueOf(60), model.tasks.get(0).percentWorkComplete);
        assertEquals(Integer.valueOf(4), model.tasks.get(0).constraintType);
        assertEquals("2026-04-03T09:00:00", model.tasks.get(0).constraintDate);
        assertEquals(1, model.tasks.get(0).extendedAttributes.size());
        assertEquals("Task Memo", model.tasks.get(0).extendedAttributes.get(0).value);
        assertEquals(1, model.tasks.get(0).baselines.size());
        assertEquals(Double.valueOf(1000.0d), model.tasks.get(0).baselines.get(0).cost);
        assertEquals(1, model.tasks.get(0).timephasedData.size());
        assertEquals("501", model.tasks.get(0).timephasedData.get(0).uid);
        assertEquals("99", model.tasks.get(0).predecessors.get(0).predecessorUid);
        assertEquals("Res A", model.resources.get(0).name);
        assertEquals(Integer.valueOf(1), model.resources.get(0).type);
        assertEquals("RA", model.resources.get(0).initials);
        assertEquals(Integer.valueOf(0), model.resources.get(0).workGroup);
        assertEquals("1000", model.resources.get(0).standardRate);
        assertEquals(Integer.valueOf(2), model.resources.get(0).standardRateFormat);
        assertEquals("1500", model.resources.get(0).overtimeRate);
        assertEquals(Integer.valueOf(2), model.resources.get(0).overtimeRateFormat);
        assertEquals(Double.valueOf(10.0d), model.resources.get(0).costPerUse);
        assertEquals("PT8H0M0S", model.resources.get(0).work);
        assertEquals("PT4H0M0S", model.resources.get(0).actualWork);
        assertEquals("PT4H0M0S", model.resources.get(0).remainingWork);
        assertEquals(Double.valueOf(1000.0d), model.resources.get(0).cost);
        assertEquals(Double.valueOf(500.0d), model.resources.get(0).actualCost);
        assertEquals(Double.valueOf(500.0d), model.resources.get(0).remainingCost);
        assertEquals(Integer.valueOf(50), model.resources.get(0).percentWorkComplete);
        assertEquals(1, model.resources.get(0).extendedAttributes.size());
        assertEquals("Res Memo", model.resources.get(0).extendedAttributes.get(0).value);
        assertEquals(1, model.resources.get(0).baselines.size());
        assertEquals(Double.valueOf(500.0d), model.resources.get(0).baselines.get(0).cost);
        assertEquals(1, model.resources.get(0).timephasedData.size());
        assertEquals("601", model.resources.get(0).timephasedData.get(0).uid);
        assertEquals("2026-04-01T09:00:00", model.assignments.get(0).start);
        assertEquals("2026-04-02T18:00:00", model.assignments.get(0).finish);
        assertEquals("PT0H0M0S", model.assignments.get(0).startVariance);
        assertEquals("PT0H0M0S", model.assignments.get(0).finishVariance);
        assertEquals("PT0H0M0S", model.assignments.get(0).delay);
        assertEquals(Boolean.FALSE, model.assignments.get(0).milestone);
        assertEquals(Integer.valueOf(1), model.assignments.get(0).workContour);
        assertEquals(Double.valueOf(1.0d), model.assignments.get(0).units);
        assertEquals(Double.valueOf(1000.0d), model.assignments.get(0).cost);
        assertEquals(Double.valueOf(500.0d), model.assignments.get(0).actualCost);
        assertEquals(Double.valueOf(500.0d), model.assignments.get(0).remainingCost);
        assertEquals("PT1H0M0S", model.assignments.get(0).overtimeWork);
        assertEquals("PT0H30M0S", model.assignments.get(0).actualOvertimeWork);
        assertEquals("PT4H0M0S", model.assignments.get(0).actualWork);
        assertEquals("PT4H0M0S", model.assignments.get(0).remainingWork);
        assertEquals(1, model.assignments.get(0).extendedAttributes.size());
        assertEquals("Assign Memo", model.assignments.get(0).extendedAttributes.get(0).value);
        assertEquals(1, model.assignments.get(0).baselines.size());
        assertEquals(Double.valueOf(500.0d), model.assignments.get(0).baselines.get(0).cost);
        assertEquals(1, model.assignments.get(0).timephasedData.size());
        assertEquals("701", model.assignments.get(0).timephasedData.get(0).uid);
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
        model.project.creationDate = "2026-03-01T09:00:00";
        model.project.lastSaved = "2026-03-31T18:00:00";
        model.project.saveVersion = Integer.valueOf(15);
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
        model.project.currencyCode = "JPY";
        model.project.currencyDigits = Integer.valueOf(0);
        model.project.currencySymbol = "¥";
        model.project.currencySymbolPosition = Integer.valueOf(0);
        model.project.fyStartDate = "2026-01-01T00:00:00";
        model.project.fiscalYearStart = Boolean.TRUE;
        model.project.criticalSlackLimit = Integer.valueOf(0);
        model.project.defaultTaskType = Integer.valueOf(1);
        model.project.defaultFixedCostAccrual = Integer.valueOf(2);
        model.project.defaultStandardRate = "1000/h";
        model.project.defaultOvertimeRate = "1500/h";
        model.project.defaultTaskEVMethod = Integer.valueOf(0);
        model.project.newTaskStartDate = Integer.valueOf(1);
        model.project.newTasksAreManual = Boolean.FALSE;
        model.project.newTasksEffortDriven = Boolean.TRUE;
        model.project.newTasksEstimated = Boolean.TRUE;
        model.project.actualsInSync = Boolean.TRUE;
        model.project.editableActualCosts = Boolean.FALSE;
        model.project.honorConstraints = Boolean.TRUE;
        model.project.insertedProjectsLikeSummary = Boolean.TRUE;
        model.project.multipleCriticalPaths = Boolean.FALSE;
        model.project.taskUpdatesResource = Boolean.TRUE;
        model.project.updateManuallyScheduledTasksWhenEditingLinks = Boolean.FALSE;
        model.project.scheduleFromStart = true;

        jp.igapyon.mikuproject.model.OutlineCodeModel outlineCode = new jp.igapyon.mikuproject.model.OutlineCodeModel();
        outlineCode.fieldID = "188743731";
        outlineCode.fieldName = "OutlineCode1";
        outlineCode.alias = "Phase";
        outlineCode.onlyTableValues = Boolean.TRUE;
        jp.igapyon.mikuproject.model.OutlineCodeMaskModel outlineCodeMask = new jp.igapyon.mikuproject.model.OutlineCodeMaskModel();
        outlineCodeMask.level = Integer.valueOf(1);
        outlineCodeMask.mask = "AA";
        outlineCodeMask.length = Integer.valueOf(2);
        outlineCodeMask.sequence = Integer.valueOf(1);
        outlineCode.masks.add(outlineCodeMask);
        jp.igapyon.mikuproject.model.OutlineCodeValueModel outlineCodeValue = new jp.igapyon.mikuproject.model.OutlineCodeValueModel();
        outlineCodeValue.value = "A1";
        outlineCodeValue.description = "Phase A";
        outlineCode.values.add(outlineCodeValue);
        model.project.outlineCodes.add(outlineCode);

        jp.igapyon.mikuproject.model.WbsMaskModel wbsMask = new jp.igapyon.mikuproject.model.WbsMaskModel();
        wbsMask.level = Integer.valueOf(1);
        wbsMask.mask = "##";
        wbsMask.length = Integer.valueOf(2);
        wbsMask.sequence = Integer.valueOf(1);
        model.project.wbsMasks.add(wbsMask);

        jp.igapyon.mikuproject.model.ProjectExtendedAttributeModel projectExtendedAttribute = new jp.igapyon.mikuproject.model.ProjectExtendedAttributeModel();
        projectExtendedAttribute.fieldID = "188743734";
        projectExtendedAttribute.fieldName = "Text1";
        projectExtendedAttribute.alias = "Memo";
        projectExtendedAttribute.calculationType = Integer.valueOf(0);
        projectExtendedAttribute.restrictValues = Boolean.FALSE;
        projectExtendedAttribute.appendNewValues = Boolean.TRUE;
        model.project.extendedAttributes.add(projectExtendedAttribute);

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
        task.startVariance = "PT1H0M0S";
        task.finishVariance = "PT2H0M0S";
        task.work = "PT16H0M0S";
        task.workVariance = "PT1H0M0S";
        task.totalSlack = "PT4H0M0S";
        task.freeSlack = "PT2H0M0S";
        task.cost = Double.valueOf(1200.0d);
        task.actualCost = Double.valueOf(700.0d);
        task.remainingCost = Double.valueOf(500.0d);
        task.remainingWork = "PT8H0M0S";
        task.actualWork = "PT8H0M0S";
        task.critical = Boolean.TRUE;
        task.percentComplete = Integer.valueOf(50);
        task.percentWorkComplete = Integer.valueOf(60);
        task.constraintType = Integer.valueOf(4);
        task.constraintDate = "2026-04-03T09:00:00";
        jp.igapyon.mikuproject.model.TaskExtendedAttributeModel taskExtendedAttribute = new jp.igapyon.mikuproject.model.TaskExtendedAttributeModel();
        taskExtendedAttribute.fieldID = "188743734";
        taskExtendedAttribute.value = "Task Memo";
        task.extendedAttributes.add(taskExtendedAttribute);
        jp.igapyon.mikuproject.model.TaskBaselineModel taskBaseline = new jp.igapyon.mikuproject.model.TaskBaselineModel();
        taskBaseline.number = Integer.valueOf(0);
        taskBaseline.start = "2026-04-01T09:00:00";
        taskBaseline.finish = "2026-04-02T18:00:00";
        taskBaseline.work = "PT16H0M0S";
        taskBaseline.cost = Double.valueOf(1000.0d);
        task.baselines.add(taskBaseline);
        jp.igapyon.mikuproject.model.TaskTimephasedDataModel taskTimephasedData = new jp.igapyon.mikuproject.model.TaskTimephasedDataModel();
        taskTimephasedData.type = Integer.valueOf(1);
        taskTimephasedData.uid = "501";
        taskTimephasedData.start = "2026-04-01T09:00:00";
        taskTimephasedData.finish = "2026-04-01T18:00:00";
        taskTimephasedData.unit = Integer.valueOf(2);
        taskTimephasedData.value = "PT8H0M0S";
        task.timephasedData.add(taskTimephasedData);
        jp.igapyon.mikuproject.model.PredecessorModel predecessor = new jp.igapyon.mikuproject.model.PredecessorModel();
        predecessor.predecessorUid = "99";
        task.predecessors.add(predecessor);
        model.tasks.add(task);

        jp.igapyon.mikuproject.model.ResourceModel resource = new jp.igapyon.mikuproject.model.ResourceModel();
        resource.uid = "2";
        resource.id = "2";
        resource.name = "Res A";
        resource.type = Integer.valueOf(1);
        resource.initials = "RA";
        resource.group = "Dev";
        resource.workGroup = Integer.valueOf(0);
        resource.maxUnits = Double.valueOf(1.0d);
        resource.standardRate = "1000";
        resource.standardRateFormat = Integer.valueOf(2);
        resource.overtimeRate = "1500";
        resource.overtimeRateFormat = Integer.valueOf(2);
        resource.costPerUse = Double.valueOf(10.0d);
        resource.work = "PT8H0M0S";
        resource.actualWork = "PT4H0M0S";
        resource.remainingWork = "PT4H0M0S";
        resource.cost = Double.valueOf(1000.0d);
        resource.actualCost = Double.valueOf(500.0d);
        resource.remainingCost = Double.valueOf(500.0d);
        resource.percentWorkComplete = Integer.valueOf(50);
        jp.igapyon.mikuproject.model.ResourceExtendedAttributeModel resourceExtendedAttribute = new jp.igapyon.mikuproject.model.ResourceExtendedAttributeModel();
        resourceExtendedAttribute.fieldID = "188743735";
        resourceExtendedAttribute.value = "Res Memo";
        resource.extendedAttributes.add(resourceExtendedAttribute);
        jp.igapyon.mikuproject.model.ResourceBaselineModel resourceBaseline = new jp.igapyon.mikuproject.model.ResourceBaselineModel();
        resourceBaseline.number = Integer.valueOf(0);
        resourceBaseline.start = "2026-04-01T09:00:00";
        resourceBaseline.finish = "2026-04-02T18:00:00";
        resourceBaseline.work = "PT8H0M0S";
        resourceBaseline.cost = Double.valueOf(500.0d);
        resource.baselines.add(resourceBaseline);
        jp.igapyon.mikuproject.model.ResourceTimephasedDataModel resourceTimephasedData = new jp.igapyon.mikuproject.model.ResourceTimephasedDataModel();
        resourceTimephasedData.type = Integer.valueOf(2);
        resourceTimephasedData.uid = "601";
        resourceTimephasedData.start = "2026-04-01T09:00:00";
        resourceTimephasedData.finish = "2026-04-01T18:00:00";
        resourceTimephasedData.unit = Integer.valueOf(3);
        resourceTimephasedData.value = "PT4H0M0S";
        resource.timephasedData.add(resourceTimephasedData);
        model.resources.add(resource);

        jp.igapyon.mikuproject.model.AssignmentModel assignment = new jp.igapyon.mikuproject.model.AssignmentModel();
        assignment.uid = "3";
        assignment.taskUid = "1";
        assignment.resourceUid = "2";
        assignment.start = "2026-04-01T09:00:00";
        assignment.finish = "2026-04-02T18:00:00";
        assignment.startVariance = "PT0H0M0S";
        assignment.finishVariance = "PT0H0M0S";
        assignment.delay = "PT0H0M0S";
        assignment.milestone = Boolean.FALSE;
        assignment.workContour = Integer.valueOf(1);
        assignment.units = Double.valueOf(1.0d);
        assignment.work = "PT8H0M0S";
        assignment.cost = Double.valueOf(1000.0d);
        assignment.actualCost = Double.valueOf(500.0d);
        assignment.remainingCost = Double.valueOf(500.0d);
        assignment.percentWorkComplete = Integer.valueOf(50);
        assignment.overtimeWork = "PT1H0M0S";
        assignment.actualOvertimeWork = "PT0H30M0S";
        assignment.actualWork = "PT4H0M0S";
        assignment.remainingWork = "PT4H0M0S";
        jp.igapyon.mikuproject.model.AssignmentExtendedAttributeModel assignmentExtendedAttribute = new jp.igapyon.mikuproject.model.AssignmentExtendedAttributeModel();
        assignmentExtendedAttribute.fieldID = "188743736";
        assignmentExtendedAttribute.value = "Assign Memo";
        assignment.extendedAttributes.add(assignmentExtendedAttribute);
        jp.igapyon.mikuproject.model.AssignmentBaselineModel assignmentBaseline = new jp.igapyon.mikuproject.model.AssignmentBaselineModel();
        assignmentBaseline.number = Integer.valueOf(0);
        assignmentBaseline.start = "2026-04-01T09:00:00";
        assignmentBaseline.finish = "2026-04-02T18:00:00";
        assignmentBaseline.work = "PT8H0M0S";
        assignmentBaseline.cost = Double.valueOf(500.0d);
        assignment.baselines.add(assignmentBaseline);
        jp.igapyon.mikuproject.model.AssignmentTimephasedDataModel assignmentTimephasedData = new jp.igapyon.mikuproject.model.AssignmentTimephasedDataModel();
        assignmentTimephasedData.type = Integer.valueOf(3);
        assignmentTimephasedData.uid = "701";
        assignmentTimephasedData.start = "2026-04-01T09:00:00";
        assignmentTimephasedData.finish = "2026-04-01T18:00:00";
        assignmentTimephasedData.unit = Integer.valueOf(4);
        assignmentTimephasedData.value = "PT4H0M0S";
        assignment.timephasedData.add(assignmentTimephasedData);
        model.assignments.add(assignment);

        String xmlText = xml.exportToXml(model);

        assertTrue(xmlText.contains("<Calendar>"));
        assertTrue(xmlText.contains("<Task>"));
        assertTrue(xmlText.contains("<Resource>"));
        assertTrue(xmlText.contains("<Assignment>"));
        assertTrue(xmlText.contains("<Title>Sample Title</Title>"));
        assertTrue(xmlText.contains("<CreationDate>2026-03-01T09:00:00</CreationDate>"));
        assertTrue(xmlText.contains("<LastSaved>2026-03-31T18:00:00</LastSaved>"));
        assertTrue(xmlText.contains("<SaveVersion>15</SaveVersion>"));
        assertTrue(xmlText.contains("<MinutesPerDay>480</MinutesPerDay>"));
        assertTrue(xmlText.contains("<WeekStartDay>2</WeekStartDay>"));
        assertTrue(xmlText.contains("<CurrencyCode>JPY</CurrencyCode>"));
        assertTrue(xmlText.contains("<CurrencySymbol>¥</CurrencySymbol>"));
        assertTrue(xmlText.contains("<DefaultStandardRate>1000/h</DefaultStandardRate>"));
        assertTrue(xmlText.contains("<DefaultOvertimeRate>1500/h</DefaultOvertimeRate>"));
        assertTrue(xmlText.contains("<NewTasksEffortDriven>1</NewTasksEffortDriven>"));
        assertTrue(xmlText.contains("<TaskUpdatesResource>1</TaskUpdatesResource>"));
        assertTrue(xmlText.contains("<OutlineCodes>"));
        assertTrue(xmlText.contains("<FieldName>OutlineCode1</FieldName>"));
        assertTrue(xmlText.contains("<WBSMasks>"));
        assertTrue(xmlText.contains("<Mask>##</Mask>"));
        assertTrue(xmlText.contains("<ExtendedAttributes>"));
        assertTrue(xmlText.contains("<Alias>Memo</Alias>"));
        assertTrue(xmlText.contains("<Type>2</Type>"));
        assertTrue(xmlText.contains("<Priority>500</Priority>"));
        assertTrue(xmlText.contains("<ActualStart>2026-04-01T09:00:00</ActualStart>"));
        assertTrue(xmlText.contains("<StartVariance>PT1H0M0S</StartVariance>"));
        assertTrue(xmlText.contains("<FinishVariance>PT2H0M0S</FinishVariance>"));
        assertTrue(xmlText.contains("<Work>PT16H0M0S</Work>"));
        assertTrue(xmlText.contains("<WorkVariance>PT1H0M0S</WorkVariance>"));
        assertTrue(xmlText.contains("<TotalSlack>PT4H0M0S</TotalSlack>"));
        assertTrue(xmlText.contains("<FreeSlack>PT2H0M0S</FreeSlack>"));
        assertTrue(xmlText.contains("<Cost>1200.0</Cost>"));
        assertTrue(xmlText.contains("<ActualCost>700.0</ActualCost>"));
        assertTrue(xmlText.contains("<RemainingCost>500.0</RemainingCost>"));
        assertTrue(xmlText.contains("<RemainingWork>PT8H0M0S</RemainingWork>"));
        assertTrue(xmlText.contains("<ActualWork>PT8H0M0S</ActualWork>"));
        assertTrue(xmlText.contains("<Critical>1</Critical>"));
        assertTrue(xmlText.contains("<PercentWorkComplete>60</PercentWorkComplete>"));
        assertTrue(xmlText.contains("<ConstraintType>4</ConstraintType>"));
        assertTrue(xmlText.contains("<ExtendedAttribute>"));
        assertTrue(xmlText.contains("<Value>Task Memo</Value>"));
        assertTrue(xmlText.contains("<Baselines>"));
        assertTrue(xmlText.contains("<TimephasedData>"));
        assertTrue(xmlText.contains("<UID>501</UID>"));
        assertTrue(xmlText.contains("<Initials>RA</Initials>"));
        assertTrue(xmlText.contains("<StandardRate>1000</StandardRate>"));
        assertTrue(xmlText.contains("<OvertimeRate>1500</OvertimeRate>"));
        assertTrue(xmlText.contains("<CostPerUse>10.0</CostPerUse>"));
        assertTrue(xmlText.contains("<ActualCost>500.0</ActualCost>"));
        assertTrue(xmlText.contains("<StartVariance>PT0H0M0S</StartVariance>"));
        assertTrue(xmlText.contains("<WorkContour>1</WorkContour>"));
        assertTrue(xmlText.contains("<OvertimeWork>PT1H0M0S</OvertimeWork>"));
        assertTrue(xmlText.contains("<ActualOvertimeWork>PT0H30M0S</ActualOvertimeWork>"));
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
        model.project.saveVersion = Integer.valueOf(-1);
        model.project.currencyDigits = Integer.valueOf(-1);
        model.project.currencySymbolPosition = Integer.valueOf(-1);
        model.project.criticalSlackLimit = Integer.valueOf(-1);
        model.project.defaultTaskType = Integer.valueOf(-1);
        model.project.defaultFixedCostAccrual = Integer.valueOf(-1);
        model.project.defaultTaskEVMethod = Integer.valueOf(-1);
        model.project.newTaskStartDate = Integer.valueOf(-1);

        jp.igapyon.mikuproject.model.OutlineCodeModel outlineCode = new jp.igapyon.mikuproject.model.OutlineCodeModel();
        jp.igapyon.mikuproject.model.OutlineCodeMaskModel outlineCodeMask = new jp.igapyon.mikuproject.model.OutlineCodeMaskModel();
        outlineCodeMask.level = Integer.valueOf(0);
        outlineCode.masks.add(outlineCodeMask);
        model.project.outlineCodes.add(outlineCode);

        jp.igapyon.mikuproject.model.WbsMaskModel wbsMask = new jp.igapyon.mikuproject.model.WbsMaskModel();
        wbsMask.level = Integer.valueOf(0);
        model.project.wbsMasks.add(wbsMask);

        jp.igapyon.mikuproject.model.ProjectExtendedAttributeModel projectExtendedAttribute = new jp.igapyon.mikuproject.model.ProjectExtendedAttributeModel();
        projectExtendedAttribute.calculationType = Integer.valueOf(-1);
        model.project.extendedAttributes.add(projectExtendedAttribute);

        List<ValidationIssue> issues = xml.validateProjectModel(model);

        assertTrue(containsMessage(issues, "Project MinutesPerDay は正の値が望ましいです"));
        assertTrue(containsMessage(issues, "Project MinutesPerWeek は正の値が望ましいです"));
        assertTrue(containsMessage(issues, "Project DaysPerMonth は正の値が望ましいです"));
        assertTrue(containsMessage(issues, "Project SaveVersion は 0 以上が望ましいです"));
        assertTrue(containsMessage(issues, "Project WeekStartDay は 1..7 が望ましいです"));
        assertTrue(containsMessage(issues, "Project WorkFormat は 0 以上が望ましいです"));
        assertTrue(containsMessage(issues, "Project DurationFormat は 0 以上が望ましいです"));
        assertTrue(containsMessage(issues, "Project CurrencyDigits は 0 以上が望ましいです"));
        assertTrue(containsMessage(issues, "Project CurrencySymbolPosition は 0 以上が望ましいです"));
        assertTrue(containsMessage(issues, "Project CriticalSlackLimit は 0 以上が望ましいです"));
        assertTrue(containsMessage(issues, "Project DefaultTaskType は 0 以上が望ましいです"));
        assertTrue(containsMessage(issues, "Project DefaultFixedCostAccrual は 0 以上が望ましいです"));
        assertTrue(containsMessage(issues, "Project DefaultTaskEVMethod は 0 以上が望ましいです"));
        assertTrue(containsMessage(issues, "Project NewTaskStartDate は 0 以上が望ましいです"));
        assertTrue(containsMessage(issues, "Project OutlineCode は FieldID または FieldName を持つことが望ましいです"));
        assertTrue(containsMessage(issues, "Project OutlineCode Mask Level は 1 以上が望ましいです"));
        assertTrue(containsMessage(issues, "Project WBSMask Level は 1 以上が望ましいです"));
        assertTrue(containsMessage(issues, "Project ExtendedAttribute は FieldID または FieldName を持つことが望ましいです"));
        assertTrue(containsMessage(issues, "Project ExtendedAttribute CalculationType は 0 以上が望ましいです"));
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
        task.priority = Integer.valueOf(1001);
        task.constraintType = Integer.valueOf(-1);
        task.cost = Double.valueOf(-1.0d);
        task.actualCost = Double.valueOf(-1.0d);
        task.remainingCost = Double.valueOf(-1.0d);
        task.calendarUID = "missing-calendar";
        model.tasks.add(task);

        jp.igapyon.mikuproject.model.ResourceModel resource = new jp.igapyon.mikuproject.model.ResourceModel();
        resource.uid = "2";
        resource.id = "2";
        resource.name = "Res A";
        resource.type = Integer.valueOf(-1);
        resource.workGroup = Integer.valueOf(-1);
        resource.maxUnits = Double.valueOf(-1.0d);
        resource.standardRateFormat = Integer.valueOf(-1);
        resource.overtimeRateFormat = Integer.valueOf(-1);
        resource.costPerUse = Double.valueOf(-1.0d);
        resource.cost = Double.valueOf(-1.0d);
        resource.actualCost = Double.valueOf(-1.0d);
        resource.remainingCost = Double.valueOf(-1.0d);
        resource.percentWorkComplete = Integer.valueOf(150);
        resource.calendarUID = "missing-calendar";
        jp.igapyon.mikuproject.model.ResourceExtendedAttributeModel resourceExtendedAttribute = new jp.igapyon.mikuproject.model.ResourceExtendedAttributeModel();
        resourceExtendedAttribute.value = "missing-field-id";
        resource.extendedAttributes.add(resourceExtendedAttribute);
        jp.igapyon.mikuproject.model.ResourceBaselineModel resourceBaseline = new jp.igapyon.mikuproject.model.ResourceBaselineModel();
        resourceBaseline.number = Integer.valueOf(-1);
        resourceBaseline.cost = Double.valueOf(-1.0d);
        resource.baselines.add(resourceBaseline);
        jp.igapyon.mikuproject.model.ResourceTimephasedDataModel resourceTimephasedData = new jp.igapyon.mikuproject.model.ResourceTimephasedDataModel();
        resourceTimephasedData.type = Integer.valueOf(-1);
        resourceTimephasedData.unit = Integer.valueOf(-1);
        resource.timephasedData.add(resourceTimephasedData);
        model.resources.add(resource);

        jp.igapyon.mikuproject.model.AssignmentModel assignment = new jp.igapyon.mikuproject.model.AssignmentModel();
        assignment.uid = "3";
        assignment.taskUid = "missing-task";
        assignment.resourceUid = "missing-resource";
        assignment.units = Double.valueOf(-1.0d);
        assignment.workContour = Integer.valueOf(-1);
        assignment.cost = Double.valueOf(-1.0d);
        assignment.actualCost = Double.valueOf(-1.0d);
        assignment.remainingCost = Double.valueOf(-1.0d);
        assignment.percentWorkComplete = Integer.valueOf(150);
        jp.igapyon.mikuproject.model.AssignmentExtendedAttributeModel assignmentExtendedAttribute = new jp.igapyon.mikuproject.model.AssignmentExtendedAttributeModel();
        assignment.extendedAttributes.add(assignmentExtendedAttribute);
        jp.igapyon.mikuproject.model.AssignmentBaselineModel assignmentBaseline = new jp.igapyon.mikuproject.model.AssignmentBaselineModel();
        assignmentBaseline.number = Integer.valueOf(-1);
        assignmentBaseline.cost = Double.valueOf(-1.0d);
        assignment.baselines.add(assignmentBaseline);
        jp.igapyon.mikuproject.model.AssignmentTimephasedDataModel assignmentTimephasedData = new jp.igapyon.mikuproject.model.AssignmentTimephasedDataModel();
        assignmentTimephasedData.type = Integer.valueOf(-1);
        assignmentTimephasedData.unit = Integer.valueOf(-1);
        assignment.timephasedData.add(assignmentTimephasedData);
        model.assignments.add(assignment);

        jp.igapyon.mikuproject.model.TaskExtendedAttributeModel taskExtendedAttribute = new jp.igapyon.mikuproject.model.TaskExtendedAttributeModel();
        task.extendedAttributes.add(taskExtendedAttribute);
        jp.igapyon.mikuproject.model.TaskBaselineModel taskBaseline = new jp.igapyon.mikuproject.model.TaskBaselineModel();
        taskBaseline.number = Integer.valueOf(-1);
        taskBaseline.cost = Double.valueOf(-1.0d);
        task.baselines.add(taskBaseline);
        jp.igapyon.mikuproject.model.TaskTimephasedDataModel taskTimephasedData = new jp.igapyon.mikuproject.model.TaskTimephasedDataModel();
        taskTimephasedData.type = Integer.valueOf(-1);
        taskTimephasedData.unit = Integer.valueOf(-1);
        task.timephasedData.add(taskTimephasedData);

        List<ValidationIssue> issues = xml.validateProjectModel(model);

        assertFalse(issues.isEmpty());
        assertTrue(containsMessage(issues, "Project CalendarUID が既存 Calendar を指していません"));
        assertTrue(containsMessage(issues, "Task CalendarUID が既存 Calendar を指していません"));
        assertTrue(containsMessage(issues, "Task PercentComplete が 0..100 の範囲外です"));
        assertTrue(containsMessage(issues, "Task PercentWorkComplete が 0..100 の範囲外です"));
        assertTrue(containsMessage(issues, "Task Type は 0 以上が望ましいです"));
        assertTrue(containsMessage(issues, "Task Priority が 0..1000 の範囲外です"));
        assertTrue(containsMessage(issues, "Task ConstraintType は 0 以上が望ましいです"));
        assertTrue(containsMessage(issues, "Task Cost は 0 以上が望ましいです"));
        assertTrue(containsMessage(issues, "Task ActualCost は 0 以上が望ましいです"));
        assertTrue(containsMessage(issues, "Task RemainingCost は 0 以上が望ましいです"));
        assertTrue(containsMessage(issues, "Task ExtendedAttribute FieldID が空です"));
        assertTrue(containsMessage(issues, "Task Baseline Number は 0 以上が望ましいです"));
        assertTrue(containsMessage(issues, "Task Baseline Cost は 0 以上が望ましいです"));
        assertTrue(containsMessage(issues, "Task TimephasedData Type は 0 以上が望ましいです"));
        assertTrue(containsMessage(issues, "Task TimephasedData Unit は 0 以上が望ましいです"));
        assertTrue(containsMessage(issues, "Resource CalendarUID が既存 Calendar を指していません"));
        assertTrue(containsMessage(issues, "Resource Type は 0 以上が望ましいです"));
        assertTrue(containsMessage(issues, "Resource WorkGroup は 0 以上が望ましいです"));
        assertTrue(containsMessage(issues, "Resource MaxUnits は 0 以上が望ましいです"));
        assertTrue(containsMessage(issues, "Resource StandardRateFormat は 0 以上が望ましいです"));
        assertTrue(containsMessage(issues, "Resource OvertimeRateFormat は 0 以上が望ましいです"));
        assertTrue(containsMessage(issues, "Resource CostPerUse は 0 以上が望ましいです"));
        assertTrue(containsMessage(issues, "Resource Cost は 0 以上が望ましいです"));
        assertTrue(containsMessage(issues, "Resource ActualCost は 0 以上が望ましいです"));
        assertTrue(containsMessage(issues, "Resource RemainingCost は 0 以上が望ましいです"));
        assertTrue(containsMessage(issues, "Resource PercentWorkComplete が 0..100 の範囲外です"));
        assertTrue(containsMessage(issues, "Resource ExtendedAttribute FieldID が空です"));
        assertTrue(containsMessage(issues, "Resource Baseline Number は 0 以上が望ましいです"));
        assertTrue(containsMessage(issues, "Resource Baseline Cost は 0 以上が望ましいです"));
        assertTrue(containsMessage(issues, "Resource TimephasedData Type は 0 以上が望ましいです"));
        assertTrue(containsMessage(issues, "Resource TimephasedData Unit は 0 以上が望ましいです"));
        assertTrue(containsMessage(issues, "Assignment TaskUID が既存 Task を指していません"));
        assertTrue(containsMessage(issues, "Assignment ResourceUID が既存 Resource を指していません"));
        assertTrue(containsMessage(issues, "Assignment Units は 0 以上が望ましいです"));
        assertTrue(containsMessage(issues, "Assignment WorkContour は 0 以上が望ましいです"));
        assertTrue(containsMessage(issues, "Assignment Cost は 0 以上が望ましいです"));
        assertTrue(containsMessage(issues, "Assignment ActualCost は 0 以上が望ましいです"));
        assertTrue(containsMessage(issues, "Assignment RemainingCost は 0 以上が望ましいです"));
        assertTrue(containsMessage(issues, "Assignment PercentWorkComplete が 0..100 の範囲外です"));
        assertTrue(containsMessage(issues, "Assignment ExtendedAttribute FieldID が空です"));
        assertTrue(containsMessage(issues, "Assignment Baseline Number は 0 以上が望ましいです"));
        assertTrue(containsMessage(issues, "Assignment Baseline Cost は 0 以上が望ましいです"));
        assertTrue(containsMessage(issues, "Assignment TimephasedData Type は 0 以上が望ましいです"));
        assertTrue(containsMessage(issues, "Assignment TimephasedData Unit は 0 以上が望ましいです"));
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
