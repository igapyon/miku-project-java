/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuproject.wbssvg;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import jp.igapyon.mikuproject.excelio.ExcelIoUtil;
import jp.igapyon.mikuproject.model.PredecessorModel;
import jp.igapyon.mikuproject.model.ProjectModel;
import jp.igapyon.mikuproject.model.TaskModel;
import jp.igapyon.mikuproject.wbsdateband.WbsDateband;

public class WbsSvg {
    private final WbsDateband dateband = new WbsDateband();

    public String exportNativeSvg(ProjectModel model) {
        return exportNativeSvg(model, null);
    }

    public String exportNativeSvg(ProjectModel model, NativeSvgOptions options) {
        NativeSvgOptions actualOptions = options == null ? new NativeSvgOptions() : options;
        List<TaskModel> tasks = exportableTasks(model);
        StringBuilder builder = new StringBuilder();
        builder.append("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"1200\" height=\"")
                .append(120 + tasks.size() * 28)
                .append("\" viewBox=\"0 0 1200 ")
                .append(120 + tasks.size() * 28)
                .append("\">");
        builder.append("<defs><marker id=\"dependencyArrow\" markerWidth=\"10\" markerHeight=\"10\" refX=\"8\" refY=\"3\" orient=\"auto\"><path d=\"M0,0 L0,6 L9,3 z\" fill=\"#555\"/></marker></defs>");
        builder.append("<text x=\"24\" y=\"32\" font-size=\"20\">").append(escapeXml(safe(model.project.name, "Project"))).append("</text>");
        int y = 60;
        for (TaskModel task : tasks) {
            int startX = dailyStartX(task);
            int width = dailyWidth(task);
            String label = safe(task.name, task.uid);
            String anchor = startX > 700 ? "end" : "start";
            int labelX = "end".equals(anchor) ? startX - 8 : startX + width + 8;
            builder.append("<rect x=\"").append(startX).append("\" y=\"").append(y - 12)
                    .append("\" width=\"").append(width).append("\" height=\"14\" fill=\"#5b8def\" rx=\"3\" ry=\"3\"/>");
            builder.append("<text x=\"").append(labelX).append("\" y=\"").append(y)
                    .append("\" text-anchor=\"").append(anchor).append("\">")
                    .append(escapeXml(label))
                    .append("</text>");
            y += 28;
        }
        appendDependencyPaths(builder, tasks, 60, 28, true);
        builder.append("</svg>");
        return builder.toString();
    }

    public String exportWeeklyNativeSvg(ProjectModel model) {
        return exportWeeklyNativeSvg(model, null);
    }

    public String exportWeeklyNativeSvg(ProjectModel model, NativeSvgOptions options) {
        List<TaskModel> tasks = exportableTasks(model);
        StringBuilder builder = new StringBuilder();
        builder.append("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"1000\" height=\"")
                .append(120 + tasks.size() * 28)
                .append("\" viewBox=\"0 0 1000 ")
                .append(120 + tasks.size() * 28)
                .append("\">");
        builder.append("<defs><marker id=\"dependencyArrow\" markerWidth=\"10\" markerHeight=\"10\" refX=\"8\" refY=\"3\" orient=\"auto\"><path d=\"M0,0 L0,6 L9,3 z\" fill=\"#555\"/></marker></defs>");
        builder.append("<text x=\"24\" y=\"32\" font-size=\"20\">").append(escapeXml(safe(model.project.name, "Project"))).append("</text>");
        builder.append("<text x=\"24\" y=\"56\">weekly overview</text>");
        int y = 80;
        for (TaskModel task : tasks) {
            int startX = weeklyStartX(task);
            int width = weeklyWidth(task);
            builder.append("<rect x=\"").append(startX).append("\" y=\"").append(y - 12)
                    .append("\" width=\"").append(width).append("\" height=\"14\" fill=\"#7bb661\" rx=\"3\" ry=\"3\"/>");
            builder.append("<text x=\"").append(startX + width + 8).append("\" y=\"").append(y)
                    .append("\" text-anchor=\"start\">")
                    .append(escapeXml(safe(task.name, task.uid)))
                    .append("</text>");
            y += 28;
        }
        appendDependencyPaths(builder, tasks, 80, 28, false);
        builder.append("</svg>");
        return builder.toString();
    }

    public MonthlyCalendarSvgArchive exportMonthlyWbsCalendarSvgArchive(ProjectModel model) {
        Set<String> holidaySet = new LinkedHashSet<String>(dateband.collectWbsHolidayDates(model));
        Map<String, StringBuilder> monthBuffers = new LinkedHashMap<String, StringBuilder>();
        for (TaskModel task : exportableTasks(model)) {
            List<String> band = dateband.buildDateBand(task.start, task.finish);
            for (String day : band) {
                if (day.length() < 7) {
                    continue;
                }
                String monthKey = day.substring(0, 7);
                StringBuilder buffer = monthBuffers.get(monthKey);
                if (buffer == null) {
                    buffer = new StringBuilder();
                    monthBuffers.put(monthKey, buffer);
                }
                if (buffer.indexOf(task.name) < 0) {
                    buffer.append("<text x=\"24\" y=\"").append(60 + countLines(buffer.toString()) * 20).append("\">")
                            .append(escapeXml(task.name)).append("</text>");
                }
            }
        }
        if (monthBuffers.isEmpty()) {
            monthBuffers.put(defaultMonthKey(model), new StringBuilder());
        }
        MonthlyCalendarSvgArchive archive = new MonthlyCalendarSvgArchive();
        for (Map.Entry<String, StringBuilder> entry : monthBuffers.entrySet()) {
            String monthKey = entry.getKey();
            String svg = "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"800\" height=\"600\" viewBox=\"0 0 800 600\">"
                    + "<text x=\"24\" y=\"32\" font-size=\"20\">" + escapeXml(safe(model.project.name, "Project")) + "</text>"
                    + "<text x=\"24\" y=\"56\">" + escapeXml(monthKey) + "</text>"
                    + entry.getValue().toString()
                    + "<text x=\"24\" y=\"580\">holidays " + holidaySet.size() + "</text>"
                    + "</svg>";
            archive.entries.add(new MonthlyCalendarEntry(monthKey + ".svg", svg));
        }
        archive.zipBytes = packMonthlyEntries(archive.entries);
        return archive;
    }

    public List<String> collectWbsHolidayDates(ProjectModel model) {
        return dateband.collectWbsHolidayDates(model);
    }

    private List<TaskModel> exportableTasks(ProjectModel model) {
        List<TaskModel> result = new ArrayList<TaskModel>();
        if (model == null || model.tasks == null) {
            return result;
        }
        for (TaskModel task : model.tasks) {
            if (task != null && !task.summary) {
                result.add(task);
            }
        }
        return result;
    }

    private void appendDependencyPaths(StringBuilder builder, List<TaskModel> tasks, int originY, int rowHeight, boolean daily) {
        Map<String, Integer> yByUid = new LinkedHashMap<String, Integer>();
        Map<String, Integer> xByUid = new LinkedHashMap<String, Integer>();
        for (int index = 0; index < tasks.size(); index++) {
            TaskModel task = tasks.get(index);
            yByUid.put(task.uid, Integer.valueOf(originY + index * rowHeight));
            xByUid.put(task.uid, Integer.valueOf((daily ? dailyStartX(task) : weeklyStartX(task)) + (daily ? dailyWidth(task) : weeklyWidth(task))));
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
                int fromY = yByUid.get(predecessor.predecessorUid).intValue() - 6;
                int toX = (daily ? dailyStartX(task) : weeklyStartX(task));
                int toY = yByUid.get(task.uid).intValue() - 6;
                builder.append("<path class=\"dependencyPath\" marker-end=\"url(#dependencyArrow)\" data-from-uid=\"")
                        .append(escapeXml(predecessor.predecessorUid)).append("\" data-to-uid=\"")
                        .append(escapeXml(task.uid)).append("\" d=\"M")
                        .append(fromX).append(",").append(fromY).append(" L").append(toX).append(",").append(toY)
                        .append("\" stroke=\"#555\" fill=\"none\"/>");
            }
        }
    }

    private int dailyStartX(TaskModel task) {
        Calendar base = Calendar.getInstance();
        base.clear();
        base.set(2026, Calendar.MARCH, 1);
        java.util.Date date = dateband.parseDateOnly(task.start);
        if (date == null) {
            return 120;
        }
        long diff = date.getTime() - base.getTimeInMillis();
        int days = (int) Math.max(0, diff / (24L * 60L * 60L * 1000L));
        return 120 + days * 28;
    }

    private int dailyWidth(TaskModel task) {
        int days = Math.max(1, dateband.buildDateBand(task.start, task.finish).size());
        return days * 26;
    }

    private int weeklyStartX(TaskModel task) {
        Calendar base = Calendar.getInstance();
        base.clear();
        base.set(2026, Calendar.MARCH, 1);
        java.util.Date date = dateband.parseDateOnly(task.start);
        if (date == null) {
            return 120;
        }
        long diff = date.getTime() - base.getTimeInMillis();
        int weeks = (int) Math.max(0, diff / (7L * 24L * 60L * 60L * 1000L));
        return 120 + weeks * 56;
    }

    private int weeklyWidth(TaskModel task) {
        int days = Math.max(1, dateband.buildDateBand(task.start, task.finish).size());
        return Math.max(40, ((days + 6) / 7) * 52);
    }

    private String defaultMonthKey(ProjectModel model) {
        String value = model != null && model.project != null ? safe(model.project.startDate, "2026-03-01") : "2026-03-01";
        return value.length() >= 7 ? value.substring(0, 7) : "2026-03";
    }

    private int countLines(String text) {
        int count = 0;
        for (int index = 0; index < text.length(); index++) {
            if (text.startsWith("<text ", index)) {
                count++;
            }
        }
        return count;
    }

    private byte[] packMonthlyEntries(List<MonthlyCalendarEntry> entries) {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        ZipOutputStream zip = null;
        try {
            zip = new ZipOutputStream(buffer);
            for (MonthlyCalendarEntry entry : entries) {
                zip.putNextEntry(new ZipEntry("monthly-calendar/" + entry.fileName));
                zip.write(ExcelIoUtil.encodeUtf8(entry.svg));
                zip.closeEntry();
            }
            zip.finish();
            return buffer.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("monthly calendar zip の生成に失敗しました", ex);
        } finally {
            if (zip != null) {
                try {
                    zip.close();
                } catch (IOException ex) {
                    // ignore
                }
            }
        }
    }

    private String safe(String value, String fallback) {
        return value == null || value.isEmpty() ? fallback : value;
    }

    private String escapeXml(String value) {
        return safe(value, "").replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    public static class NativeSvgOptions {
        public List<String> holidayDates = new ArrayList<String>();
        public Integer displayDaysBeforeBaseDate;
        public Integer displayDaysAfterBaseDate;
        public Boolean useBusinessDaysForDisplayRange;
        public Boolean useBusinessDaysForProgressBand;
        public String labelMode;
    }

    public static class MonthlyCalendarSvgArchive {
        public final List<MonthlyCalendarEntry> entries = new ArrayList<MonthlyCalendarEntry>();
        public byte[] zipBytes;
    }

    public static class MonthlyCalendarEntry {
        public final String fileName;
        public final String svg;

        public MonthlyCalendarEntry(String fileName, String svg) {
            this.fileName = fileName;
            this.svg = svg;
        }
    }
}
