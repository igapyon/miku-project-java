package jp.igapyon.mikuproject.msprojectxml;

import java.util.ArrayList;

import jp.igapyon.mikuproject.model.CalendarExceptionModel;
import jp.igapyon.mikuproject.model.CalendarModel;
import jp.igapyon.mikuproject.model.ProjectModel;
import jp.igapyon.mikuproject.model.WeekDayModel;
import jp.igapyon.mikuproject.model.WorkingTimeModel;

public class MsProjectCalendar {
    public ProjectModel ensureDefaultProjectCalendar(ProjectModel model) {
        if (model == null) {
            model = new ProjectModel();
        }
        if (model.calendars == null) {
            model.calendars = new ArrayList<CalendarModel>();
        }
        if (model.project == null) {
            model.project = new jp.igapyon.mikuproject.model.ProjectInfo();
        }
        if (model.calendars.isEmpty()) {
            CalendarModel calendar = new CalendarModel();
            calendar.uid = "1";
            calendar.name = "Standard";
            calendar.isBaseCalendar = true;
            calendar.isBaselineCalendar = Boolean.TRUE;
            calendar.weekDays.addAll(buildDefaultWeekDays());
            calendar.exceptions.addAll(buildDefaultExceptions());
            model.calendars.add(calendar);
            if (model.project.calendarUID == null || model.project.calendarUID.isEmpty()) {
                model.project.calendarUID = calendar.uid;
            }
        }
        return model;
    }

    private java.util.List<WeekDayModel> buildDefaultWeekDays() {
        java.util.List<WeekDayModel> weekDays = new java.util.ArrayList<WeekDayModel>();
        for (int dayType = 1; dayType <= 7; dayType += 1) {
            WeekDayModel weekDay = new WeekDayModel();
            weekDay.dayType = Integer.valueOf(dayType);
            weekDay.dayWorking = dayType != 1 && dayType != 7;
            if (weekDay.dayWorking) {
                WorkingTimeModel am = new WorkingTimeModel();
                am.fromTime = "09:00:00";
                am.toTime = "12:00:00";
                weekDay.workingTimes.add(am);

                WorkingTimeModel pm = new WorkingTimeModel();
                pm.fromTime = "13:00:00";
                pm.toTime = "18:00:00";
                weekDay.workingTimes.add(pm);
            }
            weekDays.add(weekDay);
        }
        return weekDays;
    }

    private java.util.List<CalendarExceptionModel> buildDefaultExceptions() {
        return new java.util.ArrayList<CalendarExceptionModel>();
    }
}
