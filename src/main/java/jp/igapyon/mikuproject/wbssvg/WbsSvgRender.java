/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuproject.wbssvg;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jp.igapyon.mikuproject.model.PredecessorModel;
import jp.igapyon.mikuproject.model.ProjectModel;
import jp.igapyon.mikuproject.model.TaskModel;
import jp.igapyon.mikuproject.wbsdateband.WbsDateband;

public class WbsSvgRender {
    private final WbsDateband dateband;
    private final WbsSvgViewport viewport;
    private final WbsSvgTimeline timeline;
    private final WbsSvgScaffold scaffold;
    private final WbsSvgBars bars;
    private final WbsSvgLabels labels;

    public WbsSvgRender(WbsDateband dateband) {
        this.dateband = dateband;
        this.viewport = new WbsSvgViewport(dateband);
        this.timeline = new WbsSvgTimeline(viewport);
        this.scaffold = new WbsSvgScaffold();
        this.bars = new WbsSvgBars();
        this.labels = new WbsSvgLabels();
    }

    public String exportNativeSvg(ProjectModel model, WbsSvg.NativeSvgOptions options) {
        List<TaskModel> tasks = exportableTasks(model);
        StringBuilder builder = new StringBuilder();
        int height = 120 + tasks.size() * 28;
        scaffold.appendSvgOpen(builder, 1200, height);
        scaffold.appendDependencyDefs(builder);
        scaffold.appendProjectTitle(builder, escapeXml(safe(model.project.name, "Project")));
        int y = 60;
        for (TaskModel task : tasks) {
            WbsSvgTimeline.TaskPlacement placement = timeline.dailyPlacement(task);
            String label = safe(task.name, task.uid);
            bars.appendDailyBar(builder, placement, y);
            labels.appendTaskLabel(builder, placement, y, escapeXml(label));
            y += 28;
        }
        appendDependencyPaths(builder, tasks, 60, 28, true);
        scaffold.appendSvgClose(builder);
        return builder.toString();
    }

    public String exportWeeklyNativeSvg(ProjectModel model, WbsSvg.NativeSvgOptions options) {
        List<TaskModel> tasks = exportableTasks(model);
        StringBuilder builder = new StringBuilder();
        int height = 120 + tasks.size() * 28;
        scaffold.appendSvgOpen(builder, 1000, height);
        scaffold.appendDependencyDefs(builder);
        scaffold.appendProjectTitle(builder, escapeXml(safe(model.project.name, "Project")));
        scaffold.appendWeeklySubtitle(builder);
        int y = 80;
        for (TaskModel task : tasks) {
            WbsSvgTimeline.TaskPlacement placement = timeline.weeklyPlacement(task);
            bars.appendWeeklyBar(builder, placement, y);
            labels.appendTaskLabel(builder, placement, y, escapeXml(safe(task.name, task.uid)));
            y += 28;
        }
        appendDependencyPaths(builder, tasks, 80, 28, false);
        scaffold.appendSvgClose(builder);
        return builder.toString();
    }

    public List<TaskModel> exportableTasks(ProjectModel model) {
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

    public void appendDependencyPaths(StringBuilder builder, List<TaskModel> tasks, int originY, int rowHeight, boolean daily) {
        Map<String, Integer> yByUid = new LinkedHashMap<String, Integer>();
        Map<String, Integer> xByUid = new LinkedHashMap<String, Integer>();
        for (int index = 0; index < tasks.size(); index++) {
            TaskModel task = tasks.get(index);
            yByUid.put(task.uid, Integer.valueOf(originY + index * rowHeight));
            xByUid.put(task.uid, Integer.valueOf((daily ? viewport.dailyStartX(task) : viewport.weeklyStartX(task))
                    + (daily ? viewport.dailyWidth(task) : viewport.weeklyWidth(task))));
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
                int toX = (daily ? viewport.dailyStartX(task) : viewport.weeklyStartX(task));
                int toY = yByUid.get(task.uid).intValue() - 6;
                builder.append("<path class=\"dependencyPath\" marker-end=\"url(#dependencyArrow)\" data-from-uid=\"")
                        .append(escapeXml(predecessor.predecessorUid)).append("\" data-to-uid=\"")
                        .append(escapeXml(task.uid)).append("\" d=\"M")
                        .append(fromX).append(",").append(fromY).append(" L").append(toX).append(",").append(toY)
                        .append("\" stroke=\"#555\" fill=\"none\"/>");
            }
        }
    }

    public String defaultMonthKey(ProjectModel model) {
        String value = model != null && model.project != null ? safe(model.project.startDate, "2026-03-01") : "2026-03-01";
        return value.length() >= 7 ? value.substring(0, 7) : "2026-03";
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
}
