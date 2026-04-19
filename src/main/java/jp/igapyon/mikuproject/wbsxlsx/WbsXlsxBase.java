/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuproject.wbsxlsx;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jp.igapyon.mikuproject.model.CalendarModel;
import jp.igapyon.mikuproject.model.PredecessorModel;
import jp.igapyon.mikuproject.model.ProjectModel;
import jp.igapyon.mikuproject.model.TaskModel;
import jp.igapyon.mikuproject.projectxlsx.XlsxColumnLike;
import jp.igapyon.mikuproject.projectxlsx.XlsxSheetLike;
import jp.igapyon.mikuproject.wbsdateband.WbsDateband;

public class WbsXlsxBase {
    private static final int FIXED_COLUMN_COUNT = 20;

    private final WbsDateband dateband;

    public WbsXlsxBase(WbsDateband dateband) {
        this.dateband = dateband;
    }

    public int getFixedColumnCount() {
        return FIXED_COLUMN_COUNT;
    }

    public void appendColumns(XlsxSheetLike sheet, int displayDays) {
        double[] fixedWidths = new double[] {
                6.43d, 6.43d, 9.29d, 8.57d, 6.43d, 42d, 12.14d, 12.14d, 9.29d, 28d, 12d, 18d, 12d, 12d, 12d, 15d, 12d, 20d, 18d, 4.5d
        };
        for (int index = 0; index < fixedWidths.length; index++) {
            XlsxColumnLike column = new XlsxColumnLike();
            column.width = Double.valueOf(fixedWidths[index]);
            if (index == 11 || index == 12 || index == 13 || index == 14 || index == 16 || index == 17 || index == 18) {
                column.hidden = Boolean.TRUE;
            }
            sheet.columns.add(column);
        }
        for (int index = 0; index < displayDays; index++) {
            XlsxColumnLike column = new XlsxColumnLike();
            column.width = Double.valueOf(4.5d);
            sheet.columns.add(column);
        }
    }

    public String formatDate(String value) {
        return value == null || value.length() < 10 ? "-" : value.substring(0, 10);
    }

    public String formatShortDate(String value) {
        if (value == null || value.length() < 10) {
            return "";
        }
        return Integer.parseInt(value.substring(5, 7)) + "/" + Integer.parseInt(value.substring(8, 10));
    }

    public String formatWeekday(String value) {
        java.util.Date date = dateband.parseDateOnly(value);
        if (date == null) {
            return "";
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        switch (calendar.get(Calendar.DAY_OF_WEEK)) {
            case Calendar.MONDAY:
                return "Mon";
            case Calendar.TUESDAY:
                return "Tue";
            case Calendar.WEDNESDAY:
                return "Wed";
            case Calendar.THURSDAY:
                return "Thu";
            case Calendar.FRIDAY:
                return "Fri";
            case Calendar.SATURDAY:
                return "Sat";
            default:
                return "Sun";
        }
    }

    public boolean isCurrentDay(String day, String currentDate) {
        return day != null && currentDate != null && currentDate.length() >= 10 && day.equals(currentDate.substring(0, 10));
    }

    public String formatDuration(TaskModel task, Set<String> holidaySet, Set<Integer> nonWorkingDayTypes, Boolean useBusinessDaysForProgressBand) {
        if (Boolean.TRUE.equals(useBusinessDaysForProgressBand)) {
            int businessDays = dateband.enumerateBusinessDays(task.start, task.finish, holidaySet, nonWorkingDayTypes).size();
            return businessDays > 0 ? businessDays + "営業日" : "-";
        }
        int days = dateband.buildDateBand(formatDate(task.start), formatDate(task.finish)).size();
        return days > 0 ? days + "日" : "-";
    }

    public String formatPredecessors(List<PredecessorModel> predecessors) {
        if (predecessors == null || predecessors.isEmpty()) {
            return "-";
        }
        List<String> values = new ArrayList<String>();
        for (PredecessorModel predecessor : predecessors) {
            if (predecessor != null && predecessor.predecessorUid != null) {
                values.add(predecessor.predecessorUid);
            }
        }
        return values.isEmpty() ? "-" : join(values, ", ");
    }

    public String first(List<String> values) {
        return values == null || values.isEmpty() ? "-" : values.get(0);
    }

    public String join(List<String> values, String separator) {
        if (values == null || values.isEmpty()) {
            return "-";
        }
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < values.size(); index++) {
            if (index > 0) {
                builder.append(separator);
            }
            builder.append(values.get(index));
        }
        return builder.toString();
    }

    public String formatCalendarLabel(ProjectModel model) {
        Map<String, String> calendarNameByUid = new LinkedHashMap<String, String>();
        for (CalendarModel calendar : model.calendars) {
            if (calendar != null) {
                calendarNameByUid.put(calendar.uid, calendar.name);
            }
        }
        return formatCalendarLabel(model.project.calendarUID, calendarNameByUid);
    }

    public String formatCalendarLabel(String calendarUid, Map<String, String> calendarNameByUid) {
        if (calendarUid == null || calendarUid.isEmpty()) {
            return "-";
        }
        String name = calendarNameByUid.get(calendarUid);
        return name == null || name.isEmpty() ? calendarUid : calendarUid + " " + name;
    }

    public String safe(String value, String fallback) {
        return value == null || value.isEmpty() ? fallback : value;
    }

    public String formatTimestamp() {
        Calendar calendar = Calendar.getInstance();
        StringBuilder builder = new StringBuilder();
        builder.append(calendar.get(Calendar.YEAR)).append('-');
        appendTwoDigits(builder, calendar.get(Calendar.MONTH) + 1);
        builder.append('-');
        appendTwoDigits(builder, calendar.get(Calendar.DAY_OF_MONTH));
        builder.append(' ');
        appendTwoDigits(builder, calendar.get(Calendar.HOUR_OF_DAY));
        builder.append(':');
        appendTwoDigits(builder, calendar.get(Calendar.MINUTE));
        return builder.toString();
    }

    private void appendTwoDigits(StringBuilder builder, int value) {
        if (value < 10) {
            builder.append('0');
        }
        builder.append(value);
    }
}
