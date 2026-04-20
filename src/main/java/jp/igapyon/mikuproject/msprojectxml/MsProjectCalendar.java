/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuproject.msprojectxml;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import jp.igapyon.mikuproject.model.CalendarExceptionModel;
import jp.igapyon.mikuproject.model.CalendarModel;
import jp.igapyon.mikuproject.model.ProjectInfo;
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
            model.project = new ProjectInfo();
        }
        if (model.calendars.isEmpty()) {
            String uid = allocateDefaultCalendarUid(model);
            CalendarModel calendar = new CalendarModel();
            calendar.uid = uid;
            calendar.name = "Standard";
            calendar.isBaseCalendar = true;
            calendar.isBaselineCalendar = Boolean.TRUE;
            calendar.weekDays.addAll(buildDefaultStandardWeekDays(model.project));
            calendar.exceptions.addAll(buildDefaultJapaneseHolidayExceptions(model.project));
            model.calendars.add(calendar);
            model.project.calendarUID = uid;
        }
        return model;
    }

    private String allocateDefaultCalendarUid(ProjectModel model) {
        Set<String> usedUids = new TreeSet<String>();
        for (CalendarModel calendar : model.calendars) {
            if (calendar != null && calendar.uid != null && !calendar.uid.trim().isEmpty()) {
                usedUids.add(calendar.uid.trim());
            }
        }
        int candidate = 1;
        while (usedUids.contains(String.valueOf(candidate))) {
            candidate += 1;
        }
        return String.valueOf(candidate);
    }

    private List<WeekDayModel> buildDefaultStandardWeekDays(ProjectInfo project) {
        List<WorkingTimeModel> defaultWorkingTimes = buildDefaultWorkingTimes(project);
        List<WeekDayModel> weekDays = new ArrayList<WeekDayModel>();
        for (int index = 1; index <= 7; index += 1) {
            WeekDayModel weekDay = new WeekDayModel();
            weekDay.dayType = Integer.valueOf(index);
            weekDay.dayWorking = index != 1 && index != 7;
            if (weekDay.dayWorking) {
                for (WorkingTimeModel source : defaultWorkingTimes) {
                    WorkingTimeModel workingTime = new WorkingTimeModel();
                    workingTime.fromTime = source.fromTime;
                    workingTime.toTime = source.toTime;
                    weekDay.workingTimes.add(workingTime);
                }
            }
            weekDays.add(weekDay);
        }
        return weekDays;
    }

    private List<WorkingTimeModel> buildDefaultWorkingTimes(ProjectInfo project) {
        String start = trimToDefault(project.defaultStartTime, "09:00:00");
        String finish = trimToDefault(project.defaultFinishTime, "18:00:00");
        List<WorkingTimeModel> workingTimes = new ArrayList<WorkingTimeModel>();
        if (start.compareTo("12:00:00") < 0 && finish.compareTo("13:00:00") > 0) {
            WorkingTimeModel am = new WorkingTimeModel();
            am.fromTime = start;
            am.toTime = "12:00:00";
            workingTimes.add(am);

            WorkingTimeModel pm = new WorkingTimeModel();
            pm.fromTime = "13:00:00";
            pm.toTime = finish;
            workingTimes.add(pm);
            return workingTimes;
        }

        WorkingTimeModel full = new WorkingTimeModel();
        full.fromTime = start;
        full.toTime = finish;
        workingTimes.add(full);
        return workingTimes;
    }

    private List<CalendarExceptionModel> buildDefaultJapaneseHolidayExceptions(ProjectInfo project) {
        LocalDate start = parseDateOnly(project.startDate);
        LocalDate finish = parseDateOnly(project.finishDate);
        if (start == null && finish == null) {
            start = LocalDate.now();
            finish = start;
        } else if (start == null) {
            start = finish;
        } else if (finish == null) {
            finish = start;
        }
        if (start.isAfter(finish)) {
            LocalDate swap = start;
            start = finish;
            finish = swap;
        }

        List<CalendarExceptionModel> exceptions = new ArrayList<CalendarExceptionModel>();
        for (int year = start.getYear(); year <= finish.getYear(); year += 1) {
            Map<String, String> holidays = buildJapaneseHolidayMapForYear(year);
            for (Map.Entry<String, String> entry : holidays.entrySet()) {
                LocalDate date = parseDateOnly(entry.getKey());
                if (date == null || date.isBefore(start) || date.isAfter(finish)) {
                    continue;
                }
                CalendarExceptionModel exception = new CalendarExceptionModel();
                exception.name = entry.getValue();
                exception.fromDate = entry.getKey() + "T00:00:00";
                exception.toDate = entry.getKey() + "T23:59:59";
                exception.dayWorking = Boolean.FALSE;
                exceptions.add(exception);
            }
        }
        return exceptions;
    }

    private Map<String, String> buildJapaneseHolidayMapForYear(int year) {
        Map<String, String> holidays = new LinkedHashMap<String, String>();
        addHoliday(holidays, LocalDate.of(year, 1, 1), "元日");
        addHoliday(holidays, buildNthWeekdayOfMonth(year, 1, DayOfWeek.MONDAY, 2), "成人の日");
        addHoliday(holidays, LocalDate.of(year, 2, 11), "建国記念の日");
        addHoliday(holidays, LocalDate.of(year, 2, 23), "天皇誕生日");
        addHoliday(holidays, LocalDate.of(year, 3, calculateVernalEquinoxDay(year)), "春分の日");
        addHoliday(holidays, LocalDate.of(year, 4, 29), "昭和の日");
        addHoliday(holidays, LocalDate.of(year, 5, 3), "憲法記念日");
        addHoliday(holidays, LocalDate.of(year, 5, 4), "みどりの日");
        addHoliday(holidays, LocalDate.of(year, 5, 5), "こどもの日");
        addHoliday(holidays, buildNthWeekdayOfMonth(year, 7, DayOfWeek.MONDAY, 3), "海の日");
        addHoliday(holidays, LocalDate.of(year, 8, 11), "山の日");
        addHoliday(holidays, buildNthWeekdayOfMonth(year, 9, DayOfWeek.MONDAY, 3), "敬老の日");
        addHoliday(holidays, LocalDate.of(year, 9, calculateAutumnalEquinoxDay(year)), "秋分の日");
        addHoliday(holidays, buildNthWeekdayOfMonth(year, 10, DayOfWeek.MONDAY, 2), "スポーツの日");
        addHoliday(holidays, LocalDate.of(year, 11, 3), "文化の日");
        addHoliday(holidays, LocalDate.of(year, 11, 23), "勤労感謝の日");

        List<String> baseDates = new ArrayList<String>(holidays.keySet());
        java.util.Collections.sort(baseDates);
        for (String dateText : baseDates) {
            LocalDate date = parseDateOnly(dateText);
            if (date == null || date.getDayOfWeek() != DayOfWeek.SUNDAY) {
                continue;
            }
            LocalDate substitute = date.plusDays(1);
            while (holidays.containsKey(formatDateOnly(substitute))) {
                substitute = substitute.plusDays(1);
            }
            holidays.put(formatDateOnly(substitute), "休日");
        }

        List<String> sortedDates = new ArrayList<String>(holidays.keySet());
        java.util.Collections.sort(sortedDates);
        for (int index = 0; index < sortedDates.size() - 1; index += 1) {
            LocalDate current = parseDateOnly(sortedDates.get(index));
            LocalDate next = parseDateOnly(sortedDates.get(index + 1));
            if (current == null || next == null) {
                continue;
            }
            if (current.plusDays(2).equals(next)) {
                LocalDate between = current.plusDays(1);
                String betweenText = formatDateOnly(between);
                if (!holidays.containsKey(betweenText) && between.getDayOfWeek() != DayOfWeek.SUNDAY) {
                    holidays.put(betweenText, "休日");
                }
            }
        }

        List<String> finalDates = new ArrayList<String>(holidays.keySet());
        java.util.Collections.sort(finalDates);
        Map<String, String> sorted = new LinkedHashMap<String, String>();
        for (String dateText : finalDates) {
            sorted.put(dateText, holidays.get(dateText));
        }
        return sorted;
    }

    private void addHoliday(Map<String, String> holidays, LocalDate date, String name) {
        holidays.put(formatDateOnly(date), name);
    }

    private LocalDate buildNthWeekdayOfMonth(int year, int month, DayOfWeek dayOfWeek, int nth) {
        LocalDate first = LocalDate.of(year, month, 1);
        int offset = (dayOfWeek.getValue() - first.getDayOfWeek().getValue() + 7) % 7;
        return first.plusDays(offset + ((nth - 1) * 7));
    }

    private int calculateVernalEquinoxDay(int year) {
        return (int) Math.floor(20.8431 + (0.242194 * (year - 1980)) - Math.floor((year - 1980) / 4.0d));
    }

    private int calculateAutumnalEquinoxDay(int year) {
        return (int) Math.floor(23.2488 + (0.242194 * (year - 1980)) - Math.floor((year - 1980) / 4.0d));
    }

    private LocalDate parseDateOnly(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        String text = value.trim();
        if (text.length() >= 10) {
            text = text.substring(0, 10);
        }
        try {
            return LocalDate.parse(text);
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    private String formatDateOnly(LocalDate value) {
        return value.toString();
    }

    private String trimToDefault(String value, String defaultValue) {
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        return value.trim();
    }
}
