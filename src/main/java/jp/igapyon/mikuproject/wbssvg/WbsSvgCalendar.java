/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuproject.wbssvg;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import jp.igapyon.mikuproject.model.ProjectModel;
import jp.igapyon.mikuproject.model.TaskModel;
import jp.igapyon.mikuproject.wbsdateband.WbsDateband;

public class WbsSvgCalendar {
    private static final int CELL_WIDTH = 164;
    private static final int CELL_HEIGHT = 126;
    private static final int LEFT_PADDING = 24;
    private static final int TOP_PADDING = 24;
    private static final int HEADER_HEIGHT = 108;
    private static final int WEEKDAY_HEIGHT = 34;
    private static final int RIGHT_PADDING = 24;
    private static final int BOTTOM_PADDING = 24;
    private static final int MAX_ITEMS_PER_DAY = 3;
    private static final int MAX_LABEL_CHARS = 15;
    private final WbsDateband dateband;
    private final WbsSvgRender render;
    private final WbsSvgZip zip;

    public WbsSvgCalendar(WbsDateband dateband, WbsSvgRender render, WbsSvgZip zip) {
        this.dateband = dateband;
        this.render = render;
        this.zip = zip;
    }

    public WbsSvg.MonthlyCalendarSvgArchive exportMonthlyWbsCalendarSvgArchive(ProjectModel model) {
        return exportMonthlyWbsCalendarSvgArchive(model, null);
    }

    public WbsSvg.MonthlyCalendarSvgArchive exportMonthlyWbsCalendarSvgArchive(ProjectModel model, WbsSvg.NativeSvgOptions options) {
        WbsSvg.NativeSvgOptions actualOptions = options == null ? new WbsSvg.NativeSvgOptions() : options;
        Set<String> holidaySet = render.holidaySet(model, actualOptions);
        Set<Integer> nonWorkingDayTypes = dateband.collectProjectNonWorkingDayTypes(model);
        List<String> monthKeys = buildProjectMonthKeys(model);
        if (monthKeys.isEmpty()) {
            monthKeys.add(render.defaultMonthKey(model));
        }
        WbsSvg.MonthlyCalendarSvgArchive archive = new WbsSvg.MonthlyCalendarSvgArchive();
        for (String monthKey : monthKeys) {
            String svg = exportMonthlyWbsCalendarSvg(model, monthKey, holidaySet, nonWorkingDayTypes, actualOptions);
            archive.entries.add(new WbsSvg.MonthlyCalendarEntry(monthKey + ".svg", svg));
        }
        archive.zipBytes = zip.packMonthlyEntries(archive.entries);
        return archive;
    }

    public String exportMonthlyWbsCalendarSvg(ProjectModel model, String monthKey, Set<String> holidaySet,
            Set<Integer> nonWorkingDayTypes, WbsSvg.NativeSvgOptions options) {
        Date monthStart = dateband.parseDateOnly(monthKey + "-01");
        if (monthStart == null) {
            monthStart = dateband.parseDateOnly(render.defaultMonthKey(model) + "-01");
        }
        Date monthEnd = endOfMonth(monthStart);
        Date gridStart = startOfWeekSunday(monthStart);
        Date gridEnd = endOfWeekSaturday(monthEnd);
        List<String> calendarDays = dateband.buildDateBand(dateband.formatDateOnly(gridStart), dateband.formatDateOnly(gridEnd));
        List<List<String>> weeks = new ArrayList<List<String>>();
        for (int index = 0; index < calendarDays.size(); index += 7) {
            weeks.add(calendarDays.subList(index, Math.min(index + 7, calendarDays.size())));
        }
        Map<String, List<DayItem>> dayItems = buildDayItemMap(model, calendarDays, holidaySet, nonWorkingDayTypes, options);
        int width = LEFT_PADDING + 7 * CELL_WIDTH + RIGHT_PADDING;
        int height = TOP_PADDING + HEADER_HEIGHT + WEEKDAY_HEIGHT + weeks.size() * CELL_HEIGHT + BOTTOM_PADDING;
        String projectName = model != null && model.project != null ? render.safe(model.project.name, "Project") : "Project";
        String[] weekdayLabels = new String[] { "Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat" };
        Calendar monthCalendar = Calendar.getInstance();
        monthCalendar.setTime(monthStart);

        StringBuilder builder = new StringBuilder();
        builder.append("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"").append(width).append("\" height=\"")
                .append(height).append("\" viewBox=\"0 0 ").append(width).append(" ").append(height)
                .append("\" role=\"img\" aria-label=\"").append(render.escapeXml(projectName + " " + monthKey))
                .append("\">");
        builder.append("<style>");
        builder.append("text { font-family: 'Hiragino Sans', 'Yu Gothic', sans-serif; fill: #1d2740; }");
        builder.append(".title { font-size: 28px; font-weight: 700; }");
        builder.append(".meta { font-size: 13px; fill: #5b6370; }");
        builder.append(".weekday { font-size: 13px; font-weight: 700; fill: #5b6370; }");
        builder.append(".dayNumber { font-size: 16px; font-weight: 700; }");
        builder.append(".dayNumberMuted { font-size: 16px; font-weight: 700; fill: #97a2b0; }");
        builder.append(".itemText { font-size: 12px; }");
        builder.append(".moreText { font-size: 11px; fill: #5b6370; }");
        builder.append(".summaryText { font-size: 12px; font-weight: 700; }");
        builder.append(".milestoneText { font-size: 12px; font-weight: 700; fill: #9a284d; }");
        builder.append(".cellBorder { stroke: #d4dbe5; stroke-width: 1; }");
        builder.append("</style>");
        builder.append("<rect x=\"0\" y=\"0\" width=\"").append(width).append("\" height=\"").append(height)
                .append("\" fill=\"#ffffff\"/>");
        builder.append("<text class=\"title\" x=\"").append(LEFT_PADDING).append("\" y=\"").append(TOP_PADDING + 28)
                .append("\">").append(render.escapeXml(projectName)).append("</text>");
        builder.append("<text class=\"meta\" x=\"").append(LEFT_PADDING).append("\" y=\"").append(TOP_PADDING + 56)
                .append("\">").append(render.escapeXml(monthKey)).append("</text>");
        builder.append("<text class=\"meta\" x=\"").append(LEFT_PADDING).append("\" y=\"").append(TOP_PADDING + 78)
                .append("\">project range ").append(render.escapeXml(projectRange(model))).append("</text>");

        int weekdayY = TOP_PADDING + HEADER_HEIGHT;
        for (int dayIndex = 0; dayIndex < weekdayLabels.length; dayIndex++) {
            int x = LEFT_PADDING + dayIndex * CELL_WIDTH;
            builder.append("<text class=\"weekday\" x=\"").append(x + 10).append("\" y=\"").append(weekdayY)
                    .append("\">").append(weekdayLabels[dayIndex]).append("</text>");
        }
        int gridOriginY = TOP_PADDING + HEADER_HEIGHT + WEEKDAY_HEIGHT;
        for (int weekIndex = 0; weekIndex < weeks.size(); weekIndex++) {
            List<String> week = weeks.get(weekIndex);
            for (int dayIndex = 0; dayIndex < week.size(); dayIndex++) {
                String day = week.get(dayIndex);
                Date date = dateband.parseDateOnly(day);
                Calendar dayCalendar = Calendar.getInstance();
                dayCalendar.setTime(date);
                boolean inMonth = dayCalendar.get(Calendar.YEAR) == monthCalendar.get(Calendar.YEAR)
                        && dayCalendar.get(Calendar.MONTH) == monthCalendar.get(Calendar.MONTH);
                boolean holiday = holidaySet.contains(day);
                boolean weekend = dateband.isWeeklyNonWorkingDay(day, nonWorkingDayTypes);
                String background = !inMonth ? "#f8fafc" : (holiday ? "#fce7ef" : (weekend ? "#eef4f8" : "#ffffff"));
                int x = LEFT_PADDING + dayIndex * CELL_WIDTH;
                int y = gridOriginY + weekIndex * CELL_HEIGHT;
                builder.append("<rect x=\"").append(x).append("\" y=\"").append(y).append("\" width=\"")
                        .append(CELL_WIDTH).append("\" height=\"").append(CELL_HEIGHT).append("\" fill=\"")
                        .append(background).append("\" class=\"cellBorder\"/>");
                builder.append("<text class=\"").append(inMonth ? "dayNumber" : "dayNumberMuted").append("\" x=\"")
                        .append(x + 10).append("\" y=\"").append(y + 22).append("\">")
                        .append(dayCalendar.get(Calendar.DAY_OF_MONTH)).append("</text>");
                List<DayItem> items = dayItems.get(day);
                int visible = Math.min(items == null ? 0 : items.size(), MAX_ITEMS_PER_DAY);
                for (int itemIndex = 0; itemIndex < visible; itemIndex++) {
                    DayItem item = items.get(itemIndex);
                    builder.append("<text class=\"").append(item.className).append("\" x=\"").append(x + 10)
                            .append("\" y=\"").append(y + 42 + itemIndex * 24).append("\">")
                            .append(render.escapeXml(item.label)).append("</text>");
                }
                if (items != null && items.size() > MAX_ITEMS_PER_DAY) {
                    builder.append("<text class=\"moreText\" x=\"").append(x + 10).append("\" y=\"")
                            .append(y + CELL_HEIGHT - 10).append("\">+")
                            .append(items.size() - MAX_ITEMS_PER_DAY).append(" more</text>");
                }
            }
        }
        builder.append("</svg>");
        return builder.toString();
    }

    private Map<String, List<DayItem>> buildDayItemMap(ProjectModel model, List<String> calendarDays, Set<String> holidaySet,
            Set<Integer> nonWorkingDayTypes, WbsSvg.NativeSvgOptions options) {
        Map<String, List<DayItem>> result = new LinkedHashMap<String, List<DayItem>>();
        Set<String> calendarDaySet = new LinkedHashSet<String>(calendarDays);
        for (String day : calendarDays) {
            result.put(day, new ArrayList<DayItem>());
        }
        if (model == null || model.tasks == null) {
            return result;
        }
        for (TaskModel task : model.tasks) {
            String startDay = task == null || task.start == null || task.start.length() < 10 ? null : task.start.substring(0, 10);
            String finishDay = task == null || task.finish == null || task.finish.length() < 10 ? null : task.finish.substring(0, 10);
            if (startDay == null || finishDay == null) {
                continue;
            }
            String label = truncateLabel(render.resolveLabel(task, options));
            if (task.milestone) {
                addDayItem(result, calendarDaySet, startDay, label, "milestoneText");
                continue;
            }
            if (task.summary) {
                addDayItem(result, calendarDaySet, startDay, label, "summaryText");
                if (!startDay.equals(finishDay)) {
                    addDayItem(result, calendarDaySet, finishDay, label, "summaryText");
                }
                continue;
            }
            for (String day : dateband.buildDateBand(startDay, finishDay)) {
                if (!calendarDaySet.contains(day)) {
                    continue;
                }
                boolean boundary = day.equals(startDay) || day.equals(finishDay);
                boolean nonWorking = holidaySet.contains(day) || dateband.isWeeklyNonWorkingDay(day, nonWorkingDayTypes);
                if (nonWorking && !boundary) {
                    continue;
                }
                addDayItem(result, calendarDaySet, day, label, "itemText");
            }
        }
        return result;
    }

    private void addDayItem(Map<String, List<DayItem>> result, Set<String> calendarDaySet, String day, String label,
            String className) {
        if (calendarDaySet.contains(day)) {
            result.get(day).add(new DayItem(label, className));
        }
    }

    private List<String> buildProjectMonthKeys(ProjectModel model) {
        List<String> result = new ArrayList<String>();
        Date start = model == null || model.project == null ? null : dateband.parseDateOnly(model.project.startDate);
        Date finish = model == null || model.project == null ? null : dateband.parseDateOnly(model.project.finishDate);
        if (start == null || finish == null || start.after(finish)) {
            return result;
        }
        Calendar cursor = Calendar.getInstance();
        cursor.setTime(start);
        cursor.set(Calendar.DAY_OF_MONTH, 1);
        Calendar limit = Calendar.getInstance();
        limit.setTime(finish);
        limit.set(Calendar.DAY_OF_MONTH, 1);
        while (!cursor.after(limit)) {
            result.add(dateband.formatDateOnly(cursor.getTime()).substring(0, 7));
            cursor.add(Calendar.MONTH, 1);
        }
        return result;
    }

    private Date startOfWeekSunday(Date value) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(value);
        calendar.add(Calendar.DAY_OF_MONTH, -calendar.get(Calendar.DAY_OF_WEEK) + Calendar.SUNDAY);
        return calendar.getTime();
    }

    private Date endOfWeekSaturday(Date value) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(value);
        calendar.add(Calendar.DAY_OF_MONTH, Calendar.SATURDAY - calendar.get(Calendar.DAY_OF_WEEK));
        return calendar.getTime();
    }

    private Date endOfMonth(Date value) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(value);
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH));
        return calendar.getTime();
    }

    private String projectRange(ProjectModel model) {
        String start = model != null && model.project != null && model.project.startDate != null && model.project.startDate.length() >= 10
                ? model.project.startDate.substring(0, 10) : "";
        String finish = model != null && model.project != null && model.project.finishDate != null && model.project.finishDate.length() >= 10
                ? model.project.finishDate.substring(0, 10) : "";
        return start + " - " + finish;
    }

    private String truncateLabel(String value) {
        String text = value == null || value.trim().isEmpty() ? "-" : value.trim();
        if (text.length() <= MAX_LABEL_CHARS) {
            return text;
        }
        return text.substring(0, MAX_LABEL_CHARS - 3) + "...";
    }

    private static class DayItem {
        private final String label;
        private final String className;

        private DayItem(String label, String className) {
            this.label = label;
            this.className = className;
        }
    }
}
