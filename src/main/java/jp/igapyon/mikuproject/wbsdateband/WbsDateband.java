/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuproject.wbsdateband;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jp.igapyon.mikuproject.model.CalendarExceptionModel;
import jp.igapyon.mikuproject.model.CalendarModel;
import jp.igapyon.mikuproject.model.ProjectModel;
import jp.igapyon.mikuproject.model.WeekDayModel;

public class WbsDateband {
    public Date parseDateOnly(String value) {
        if (value == null || value.length() < 10) {
            return null;
        }
        try {
            int year = Integer.parseInt(value.substring(0, 4));
            int month = Integer.parseInt(value.substring(5, 7));
            int day = Integer.parseInt(value.substring(8, 10));
            Calendar calendar = Calendar.getInstance();
            calendar.clear();
            calendar.set(year, month - 1, day);
            return calendar.getTime();
        } catch (RuntimeException ex) {
            return null;
        }
    }

    public String formatDateOnly(Date value) {
        if (value == null) {
            return "";
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(value);
        StringBuilder builder = new StringBuilder();
        builder.append(calendar.get(Calendar.YEAR)).append('-');
        appendTwoDigits(builder, calendar.get(Calendar.MONTH) + 1);
        builder.append('-');
        appendTwoDigits(builder, calendar.get(Calendar.DAY_OF_MONTH));
        return builder.toString();
    }

    public List<String> buildDateBand(String startDate, String finishDate) {
        Date start = parseDateOnly(startDate);
        Date finish = parseDateOnly(finishDate);
        if (start == null || finish == null || start.after(finish)) {
            return new ArrayList<String>();
        }
        List<String> days = new ArrayList<String>();
        Calendar cursor = Calendar.getInstance();
        cursor.setTime(start);
        while (!cursor.getTime().after(finish)) {
            days.add(formatDateOnly(cursor.getTime()));
            cursor.add(Calendar.DAY_OF_MONTH, 1);
        }
        return days;
    }

    public List<String> expandExceptionDays(CalendarExceptionModel exception) {
        String from = exception == null || exception.fromDate == null ? "" : exception.fromDate.substring(0, Math.min(10, exception.fromDate.length()));
        String to = exception == null || exception.toDate == null ? "" : exception.toDate.substring(0, Math.min(10, exception.toDate.length()));
        if (from.isEmpty()) {
            return new ArrayList<String>();
        }
        if (to.isEmpty() || from.equals(to)) {
            return Collections.singletonList(from);
        }
        List<String> dateBand = buildDateBand(from, to);
        return dateBand.isEmpty() ? Collections.singletonList(from) : dateBand;
    }

    public List<String> collectWbsHolidayDates(ProjectModel model) {
        Set<String> holidaySet = new LinkedHashSet<String>();
        if (model == null || model.calendars == null) {
            return new ArrayList<String>();
        }
        for (CalendarModel calendar : model.calendars) {
            if (calendar == null || calendar.exceptions == null) {
                continue;
            }
            for (CalendarExceptionModel exception : calendar.exceptions) {
                if (exception == null) {
                    continue;
                }
                if (exception.dayWorking != null && exception.dayWorking.booleanValue()) {
                    continue;
                }
                if (exception.workingTimes != null && !exception.workingTimes.isEmpty()) {
                    continue;
                }
                holidaySet.addAll(expandExceptionDays(exception));
            }
        }
        List<String> holidays = new ArrayList<String>(holidaySet);
        Collections.sort(holidays);
        return holidays;
    }

    public Set<Integer> collectProjectNonWorkingDayTypes(ProjectModel model) {
        Map<String, CalendarModel> calendarByUid = new LinkedHashMap<String, CalendarModel>();
        if (model != null && model.calendars != null) {
            for (CalendarModel calendar : model.calendars) {
                if (calendar != null) {
                    calendarByUid.put(calendar.uid, calendar);
                }
            }
        }
        CalendarModel projectCalendar = resolveProjectCalendar(model);
        Set<Integer> result = new LinkedHashSet<Integer>();
        for (int dayType = 1; dayType <= 7; dayType++) {
            Boolean dayWorking = resolveCalendarDayWorking(calendarByUid, projectCalendar, dayType, new LinkedHashSet<String>());
            if (Boolean.FALSE.equals(dayWorking)) {
                result.add(Integer.valueOf(dayType));
                continue;
            }
            if (dayWorking == null && (dayType == 1 || dayType == 7)) {
                result.add(Integer.valueOf(dayType));
            }
        }
        return result;
    }

    public List<String> buildDisplayDateBand(String startDate, String finishDate, String baseDate,
            Integer displayDaysBeforeBaseDate, Integer displayDaysAfterBaseDate, Set<String> holidaySet,
            Set<Integer> nonWorkingDayTypes, Boolean useBusinessDaysForDisplayRange) {
        List<String> fullBand = buildDateBand(startDate, finishDate);
        Integer before = normalizeDisplayDayCount(displayDaysBeforeBaseDate);
        Integer after = normalizeDisplayDayCount(displayDaysAfterBaseDate);
        if (before == null && after == null) {
            return fullBand;
        }
        Date base = parseDateOnly(baseDate);
        Date projectStart = parseDateOnly(startDate);
        Date projectFinish = parseDateOnly(finishDate);
        if (base == null || fullBand.isEmpty() || projectStart == null || projectFinish == null) {
            return fullBand;
        }
        Date from = Boolean.TRUE.equals(useBusinessDaysForDisplayRange)
                ? shiftBusinessDays(base, -(before == null ? 0 : before.intValue()), holidaySet, nonWorkingDayTypes)
                : shiftCalendarDays(base, -(before == null ? 0 : before.intValue()));
        Date to = Boolean.TRUE.equals(useBusinessDaysForDisplayRange)
                ? shiftBusinessDays(base, after == null ? 0 : after.intValue(), holidaySet, nonWorkingDayTypes)
                : shiftCalendarDays(base, after == null ? 0 : after.intValue());
        Date clampedStart = from.before(projectStart) ? projectStart : from;
        Date clampedFinish = to.after(projectFinish) ? projectFinish : to;
        if (clampedStart.after(clampedFinish)) {
            return fullBand;
        }
        return buildDateBand(formatDateOnly(clampedStart), formatDateOnly(clampedFinish));
    }

    public boolean isWeeklyNonWorkingDay(String day, Set<Integer> nonWorkingDayTypes) {
        Date target = parseDateOnly(day);
        if (target == null) {
            return false;
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(target);
        int calendarDay = calendar.get(Calendar.DAY_OF_WEEK);
        int dayType = calendarDay == Calendar.SUNDAY ? 1 : calendarDay;
        return nonWorkingDayTypes.contains(Integer.valueOf(dayType));
    }

    public int countBusinessDays(List<String> dateBand, Set<String> holidaySet, Set<Integer> nonWorkingDayTypes) {
        int count = 0;
        for (String day : dateBand) {
            if (!isWeeklyNonWorkingDay(day, nonWorkingDayTypes) && !holidaySet.contains(day)) {
                count++;
            }
        }
        return count;
    }

    public List<String> enumerateBusinessDays(String startDate, String finishDate, Set<String> holidaySet,
            Set<Integer> nonWorkingDayTypes) {
        List<String> result = new ArrayList<String>();
        for (String day : buildDateBand(startDate, finishDate)) {
            if (!isWeeklyNonWorkingDay(day, nonWorkingDayTypes) && !holidaySet.contains(day)) {
                result.add(day);
            }
        }
        return result;
    }

    private CalendarModel resolveProjectCalendar(ProjectModel model) {
        if (model == null || model.calendars == null || model.calendars.isEmpty()) {
            return null;
        }
        if (model.project != null && model.project.calendarUID != null) {
            for (CalendarModel calendar : model.calendars) {
                if (calendar != null && model.project.calendarUID.equals(calendar.uid)) {
                    return calendar;
                }
            }
        }
        for (CalendarModel calendar : model.calendars) {
            if (calendar != null && calendar.isBaseCalendar) {
                return calendar;
            }
        }
        return model.calendars.get(0);
    }

    private Boolean resolveCalendarDayWorking(Map<String, CalendarModel> calendarByUid, CalendarModel calendar, int dayType,
            Set<String> visiting) {
        if (calendar == null) {
            return null;
        }
        if (calendar.uid != null && visiting.contains(calendar.uid)) {
            return null;
        }
        if (calendar.uid != null) {
            visiting.add(calendar.uid);
        }
        if (calendar.weekDays != null) {
            for (WeekDayModel weekDay : calendar.weekDays) {
                if (weekDay != null && weekDay.dayType != null && weekDay.dayType.intValue() == dayType) {
                    return weekDay.dayWorking;
                }
            }
        }
        if (calendar.baseCalendarUID != null) {
            return resolveCalendarDayWorking(calendarByUid, calendarByUid.get(calendar.baseCalendarUID), dayType, visiting);
        }
        return null;
    }

    private Integer normalizeDisplayDayCount(Integer value) {
        if (value == null) {
            return null;
        }
        return Integer.valueOf(Math.max(0, value.intValue()));
    }

    private Date shiftCalendarDays(Date base, int offset) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(base);
        calendar.add(Calendar.DAY_OF_MONTH, offset);
        return calendar.getTime();
    }

    private Date shiftBusinessDays(Date base, int offset, Set<String> holidaySet, Set<Integer> nonWorkingDayTypes) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(base);
        int direction = offset < 0 ? -1 : 1;
        int remaining = Math.abs(offset);
        while (remaining > 0) {
            calendar.add(Calendar.DAY_OF_MONTH, direction);
            String day = formatDateOnly(calendar.getTime());
            if (isWeeklyNonWorkingDay(day, nonWorkingDayTypes) || holidaySet.contains(day)) {
                continue;
            }
            remaining--;
        }
        return calendar.getTime();
    }

    private void appendTwoDigits(StringBuilder builder, int value) {
        if (value < 10) {
            builder.append('0');
        }
        builder.append(value);
    }
}
