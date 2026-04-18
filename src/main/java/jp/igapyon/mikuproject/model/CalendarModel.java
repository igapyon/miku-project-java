package jp.igapyon.mikuproject.model;

import java.util.ArrayList;
import java.util.List;

public class CalendarModel {
    public String uid;
    public String name;
    public boolean isBaseCalendar;
    public Boolean isBaselineCalendar;
    public String baseCalendarUID;
    public List<WeekDayModel> weekDays = new ArrayList<WeekDayModel>();
    public List<CalendarExceptionModel> exceptions = new ArrayList<CalendarExceptionModel>();
    public List<WorkWeekModel> workWeeks = new ArrayList<WorkWeekModel>();
}
