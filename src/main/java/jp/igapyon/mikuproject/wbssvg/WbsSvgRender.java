/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuproject.wbssvg;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jp.igapyon.mikuproject.model.PredecessorModel;
import jp.igapyon.mikuproject.model.ProjectModel;
import jp.igapyon.mikuproject.model.TaskModel;
import jp.igapyon.mikuproject.wbsdateband.WbsDateband;

public class WbsSvgRender {
    private static final int DAILY_DAY_WIDTH = 38;
    private static final int WEEKLY_WEEK_WIDTH = 38;
    private final WbsDateband dateband;
    private final WbsSvgViewport viewport;
    private final WbsSvgTimeline timeline;
    private final WbsSvgScaffold scaffold;
    private final WbsSvgBars bars;
    private final WbsSvgLabels labels;
    private final WbsSvgAxis axis;

    public WbsSvgRender(WbsDateband dateband) {
        this.dateband = dateband;
        this.viewport = new WbsSvgViewport(dateband);
        this.timeline = new WbsSvgTimeline(viewport);
        this.scaffold = new WbsSvgScaffold();
        this.bars = new WbsSvgBars();
        this.labels = new WbsSvgLabels();
        this.axis = new WbsSvgAxis();
    }

    public String exportNativeSvg(ProjectModel model, WbsSvg.NativeSvgOptions options) {
        List<TaskModel> tasks = exportableTasks(model);
        WbsSvg.NativeSvgOptions actualOptions = options == null ? new WbsSvg.NativeSvgOptions() : options;
        Set<String> holidaySet = holidaySet(model, actualOptions);
        Set<Integer> nonWorkingDayTypes = dateband.collectProjectNonWorkingDayTypes(model);
        List<String> dateBand = buildDisplayDateBand(model, tasks, actualOptions, holidaySet, nonWorkingDayTypes);
        if (!dateBand.isEmpty()) {
            viewport.setBaseDate(dateband.parseDateOnly(dateBand.get(0)));
        } else {
            viewport.setBaseDate(resolveTimelineBaseDate(model, tasks));
        }
        StringBuilder builder = new StringBuilder();
        int chartOriginX = 0;
        int chartOriginY = 104;
        int chartWidth = Math.max(1, dateBand.size()) * DAILY_DAY_WIDTH;
        int height = 22 + 82 + tasks.size() * 38 + 28;
        int width = Math.max(1, chartOriginX + chartWidth);
        scaffold.appendDailyScaffold(builder, width, height, chartOriginX, chartWidth, 22,
                escapeXml(safe(model.project.name, "Project")));
        axis.appendDailyAxis(builder, dateBand, chartOriginX, chartOriginY, height, DAILY_DAY_WIDTH, holidaySet,
                nonWorkingDayTypes, dateband, model.project.currentDate);
        int y = chartOriginY + 24;
        appendDependencyPaths(builder, tasks, chartOriginY + 24, 38, true);
        for (TaskModel task : tasks) {
            WbsSvgTimeline.TaskPlacement placement = timeline.dailyPlacement(task);
            if ("start".equals(placement.anchor) && placement.labelX > width) {
                placement.anchor = "end";
                placement.labelX = placement.startX - 12;
            }
            String label = resolveLabel(task, actualOptions);
            bars.appendDailyBar(builder, task, placement, y);
            labels.appendTaskLabel(builder, task, placement, y, escapeXml(label));
            y += 38;
        }
        scaffold.appendSvgClose(builder);
        return builder.toString();
    }

    public String exportWeeklyNativeSvg(ProjectModel model, WbsSvg.NativeSvgOptions options) {
        List<TaskModel> tasks = exportableTasks(model);
        WbsSvg.NativeSvgOptions actualOptions = options == null ? new WbsSvg.NativeSvgOptions() : options;
        Set<String> holidaySet = holidaySet(model, actualOptions);
        Set<Integer> nonWorkingDayTypes = dateband.collectProjectNonWorkingDayTypes(model);
        List<String> dateBand = buildDisplayDateBand(model, tasks, actualOptions, holidaySet, nonWorkingDayTypes);
        List<WbsSvgAxis.WeeklyBand> weeklyBand = buildWeeklyBand(dateBand);
        if (!weeklyBand.isEmpty()) {
            viewport.setBaseDate(dateband.parseDateOnly(weeklyBand.get(0).startDay));
        } else {
            viewport.setBaseDate(resolveTimelineBaseDate(model, tasks));
        }
        StringBuilder builder = new StringBuilder();
        int chartOriginX = 120;
        int chartOriginY = 118;
        int chartWidth = Math.max(1, weeklyBand.size()) * WEEKLY_WEEK_WIDTH;
        int height = chartOriginY + 38 + tasks.size() * 38;
        int width = Math.max(1000, chartOriginX + chartWidth + 420);
        scaffold.appendWeeklyScaffold(builder, width, height, chartOriginX, chartWidth, 22,
                escapeXml(safe(model.project.name, "Project")), escapeXml(dateOnly(model.project.startDate)),
                escapeXml(dateOnly(model.project.finishDate)));
        axis.appendWeeklyAxis(builder, weeklyBand, chartOriginX, chartOriginY, height, WEEKLY_WEEK_WIDTH,
                model.project.currentDate);
        int y = chartOriginY + 38;
        for (TaskModel task : tasks) {
            WbsSvgTimeline.TaskPlacement placement = timeline.weeklyPlacement(task);
            bars.appendWeeklyBar(builder, task, placement, y);
            labels.appendTaskLabel(builder, task, placement, y, escapeXml(resolveLabel(task, actualOptions)));
            y += 38;
        }
        appendDependencyPaths(builder, tasks, chartOriginY + 38, 38, false);
        scaffold.appendSvgClose(builder);
        return builder.toString();
    }

    public List<TaskModel> exportableTasks(ProjectModel model) {
        List<TaskModel> result = new ArrayList<TaskModel>();
        if (model == null || model.tasks == null) {
            return result;
        }
        for (TaskModel task : model.tasks) {
            if (task != null) {
                result.add(task);
            }
        }
        return result;
    }

    public void appendDependencyPaths(StringBuilder builder, List<TaskModel> tasks, int originY, int rowHeight, boolean daily) {
        Map<String, Integer> yByUid = new LinkedHashMap<String, Integer>();
        Map<String, Integer> xByUid = new LinkedHashMap<String, Integer>();
        for (int index = 0; index < tasks.size(); index++) {
            TaskModel task = tasks.get(index);
            yByUid.put(task.uid, Integer.valueOf(originY + index * rowHeight));
            int endX = (daily ? viewport.dailyStartX(task) + viewport.dailyWidth(task) - 6
                    : viewport.weeklyStartX(task) + viewport.weeklyWidth(task));
            xByUid.put(task.uid, Integer.valueOf(endX));
        }
        for (TaskModel task : tasks) {
            if (task.predecessors == null) {
                continue;
            }
            for (PredecessorModel predecessor : task.predecessors) {
                if (predecessor == null || !yByUid.containsKey(predecessor.predecessorUid) || !yByUid.containsKey(task.uid)) {
                    continue;
                }
                int fromX = xByUid.get(predecessor.predecessorUid).intValue();
                int fromY = yByUid.get(predecessor.predecessorUid).intValue() - 5;
                int toX = (daily ? viewport.dailyStartX(task) + 2 : viewport.weeklyStartX(task));
                int toY = yByUid.get(task.uid).intValue() - 5;
                if (daily) {
                    int controlRightX = fromX + 18;
                    int midY = fromY + (toY - fromY) / 2;
                    int controlLeftX = fromX - 18;
                    builder.append("<path class=\"dependencyPath\" d=\"M ")
                            .append(fromX).append(" ").append(fromY).append(" C ")
                            .append(controlRightX).append(" ").append(fromY).append(" ")
                            .append(controlRightX).append(" ").append(midY - 10).append(" ")
                            .append(fromX).append(" ").append(midY).append(" C ")
                            .append(controlLeftX).append(" ").append(midY + 10).append(" ")
                            .append(controlLeftX).append(" ").append(toY).append(" ")
                            .append(fromX).append(" ").append(toY).append(" L ")
                            .append(toX).append(" ").append(toY)
                            .append("\" marker-end=\"url(#dependencyArrow)\" data-from-uid=\"")
                            .append(escapeXml(predecessor.predecessorUid)).append("\" data-to-uid=\"")
                            .append(escapeXml(task.uid)).append("\" data-link-type=\"")
                            .append(predecessor.type == null ? "FS" : describeLinkType(predecessor.type.intValue())).append("\"/>");
                    continue;
                }
                builder.append("<path class=\"dependencyPath\" marker-end=\"url(#dependencyArrow)\" data-from-uid=\"")
                        .append(escapeXml(predecessor.predecessorUid)).append("\" data-to-uid=\"")
                        .append(escapeXml(task.uid)).append("\" d=\"M")
                        .append(fromX).append(",").append(fromY).append(" L").append(toX).append(",").append(toY)
                        .append("\"/>");
            }
        }
    }

    private String describeLinkType(int type) {
        if (type == 0) {
            return "FF";
        }
        if (type == 3) {
            return "SF";
        }
        if (type == 4) {
            return "SS";
        }
        return "FS";
    }

    public String defaultMonthKey(ProjectModel model) {
        String value = model != null && model.project != null ? safe(model.project.startDate, "2026-03-01") : "2026-03-01";
        return value.length() >= 7 ? value.substring(0, 7) : "2026-03";
    }

    public Date resolveTimelineBaseDate(ProjectModel model, List<TaskModel> tasks) {
        Date earliest = null;
        if (tasks != null) {
            for (TaskModel task : tasks) {
                Date start = task == null ? null : dateband.parseDateOnly(task.start);
                if (start != null && (earliest == null || start.before(earliest))) {
                    earliest = start;
                }
            }
        }
        if (earliest != null) {
            return earliest;
        }
        return model != null && model.project != null ? dateband.parseDateOnly(model.project.startDate) : null;
    }

    public String resolveLabel(TaskModel task, WbsSvg.NativeSvgOptions options) {
        if (task == null) {
            return "";
        }
        String labelMode = options == null ? null : options.labelMode;
        if ("uid".equalsIgnoreCase(safe(labelMode, ""))) {
            return safe(task.uid, safe(task.name, "-"));
        }
        if ("wbs".equalsIgnoreCase(safe(labelMode, ""))) {
            return safe(task.wbs, safe(task.outlineNumber, safe(task.name, "-")));
        }
        return safe(task.name, safe(task.uid, "-"));
    }

    public int countLines(String text) {
        int count = 0;
        for (int index = 0; index < text.length(); index++) {
            if (text.startsWith("<text ", index)) {
                count++;
            }
        }
        return count;
    }

    public String safe(String value, String fallback) {
        return value == null || value.isEmpty() ? fallback : value;
    }

    public String escapeXml(String value) {
        return safe(value, "").replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    private String dateOnly(String value) {
        return value != null && value.length() >= 10 ? value.substring(0, 10) : "";
    }

    public Set<String> holidaySet(ProjectModel model, WbsSvg.NativeSvgOptions options) {
        Set<String> holidaySet = new LinkedHashSet<String>(dateband.collectWbsHolidayDates(model));
        if (options != null && options.holidayDates != null) {
            for (String holiday : options.holidayDates) {
                if (holiday != null && holiday.length() >= 10) {
                    holidaySet.add(holiday.substring(0, 10));
                }
            }
        }
        return holidaySet;
    }

    public List<String> buildDisplayDateBand(ProjectModel model, List<TaskModel> tasks, WbsSvg.NativeSvgOptions options,
            Set<String> holidaySet, Set<Integer> nonWorkingDayTypes) {
        String start = model != null && model.project != null ? model.project.startDate : null;
        String finish = model != null && model.project != null ? model.project.finishDate : null;
        if (dateband.parseDateOnly(start) == null || dateband.parseDateOnly(finish) == null) {
            Date earliest = null;
            Date latest = null;
            for (TaskModel task : tasks) {
                Date taskStart = task == null ? null : dateband.parseDateOnly(task.start);
                Date taskFinish = task == null ? null : dateband.parseDateOnly(task.finish);
                if (taskStart != null && (earliest == null || taskStart.before(earliest))) {
                    earliest = taskStart;
                }
                if (taskFinish != null && (latest == null || taskFinish.after(latest))) {
                    latest = taskFinish;
                }
            }
            start = dateband.formatDateOnly(earliest);
            finish = dateband.formatDateOnly(latest);
        }
        List<String> band = dateband.buildDisplayDateBand(start, finish, model != null && model.project != null
                ? model.project.currentDate : null, options.displayDaysBeforeBaseDate, options.displayDaysAfterBaseDate,
                holidaySet, nonWorkingDayTypes, options.useBusinessDaysForDisplayRange);
        if (band.isEmpty()) {
            Date base = resolveTimelineBaseDate(model, tasks);
            if (base != null) {
                String baseDay = dateband.formatDateOnly(base);
                band = dateband.buildDateBand(baseDay, baseDay);
            }
        }
        return band;
    }

    public List<WbsSvgAxis.WeeklyBand> buildWeeklyBand(List<String> dateBand) {
        List<WbsSvgAxis.WeeklyBand> weeks = new ArrayList<WbsSvgAxis.WeeklyBand>();
        if (dateBand == null || dateBand.isEmpty()) {
            return weeks;
        }
        Date start = startOfWeekSunday(dateband.parseDateOnly(dateBand.get(0)));
        Date finish = endOfWeekSaturday(dateband.parseDateOnly(dateBand.get(dateBand.size() - 1)));
        Calendar cursor = Calendar.getInstance();
        cursor.setTime(start);
        while (!cursor.getTime().after(finish)) {
            String startDay = dateband.formatDateOnly(cursor.getTime());
            Calendar end = Calendar.getInstance();
            end.setTime(cursor.getTime());
            end.add(Calendar.DAY_OF_MONTH, 6);
            weeks.add(new WbsSvgAxis.WeeklyBand(startDay, dateband.formatDateOnly(end.getTime()),
                    startDay.substring(0, 7)));
            cursor.add(Calendar.DAY_OF_MONTH, 7);
        }
        return weeks;
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
}
