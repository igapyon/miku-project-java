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

import jp.igapyon.mikuproject.model.ProjectModel;
import jp.igapyon.mikuproject.model.TaskModel;
import jp.igapyon.mikuproject.wbsdateband.WbsDateband;

public class WbsSvgCalendar {
    private final WbsDateband dateband;
    private final WbsSvgRender render;
    private final WbsSvgZip zip;

    public WbsSvgCalendar(WbsDateband dateband, WbsSvgRender render, WbsSvgZip zip) {
        this.dateband = dateband;
        this.render = render;
        this.zip = zip;
    }

    public WbsSvg.MonthlyCalendarSvgArchive exportMonthlyWbsCalendarSvgArchive(ProjectModel model) {
        Set<String> holidaySet = new LinkedHashSet<String>(dateband.collectWbsHolidayDates(model));
        Map<String, StringBuilder> monthBuffers = new LinkedHashMap<String, StringBuilder>();
        for (TaskModel task : render.exportableTasks(model)) {
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
                    buffer.append("<text x=\"24\" y=\"").append(60 + render.countLines(buffer.toString()) * 20).append("\">")
                            .append(render.escapeXml(task.name)).append("</text>");
                }
            }
        }
        if (monthBuffers.isEmpty()) {
            monthBuffers.put(render.defaultMonthKey(model), new StringBuilder());
        }
        WbsSvg.MonthlyCalendarSvgArchive archive = new WbsSvg.MonthlyCalendarSvgArchive();
        for (Map.Entry<String, StringBuilder> entry : monthBuffers.entrySet()) {
            String monthKey = entry.getKey();
            String svg = "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"800\" height=\"600\" viewBox=\"0 0 800 600\">"
                    + "<text x=\"24\" y=\"32\" font-size=\"20\">" + render.escapeXml(render.safe(model.project.name, "Project")) + "</text>"
                    + "<text x=\"24\" y=\"56\">" + render.escapeXml(monthKey) + "</text>"
                    + entry.getValue().toString()
                    + "<text x=\"24\" y=\"580\">holidays " + holidaySet.size() + "</text>"
                    + "</svg>";
            archive.entries.add(new WbsSvg.MonthlyCalendarEntry(monthKey + ".svg", svg));
        }
        archive.zipBytes = zip.packMonthlyEntries(archive.entries);
        return archive;
    }
}
