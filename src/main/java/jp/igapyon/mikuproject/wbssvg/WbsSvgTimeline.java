/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuproject.wbssvg;

import jp.igapyon.mikuproject.model.TaskModel;

public class WbsSvgTimeline {
    private final WbsSvgViewport viewport;

    public WbsSvgTimeline(WbsSvgViewport viewport) {
        this.viewport = viewport;
    }

    public TaskPlacement dailyPlacement(TaskModel task) {
        TaskPlacement placement = new TaskPlacement();
        placement.startX = viewport.dailyStartX(task);
        placement.width = viewport.dailyWidth(task);
        placement.anchor = placement.startX > 700 ? "end" : "start";
        placement.labelX = "end".equals(placement.anchor) ? placement.startX - 8 : placement.startX + placement.width + 8;
        return placement;
    }

    public TaskPlacement weeklyPlacement(TaskModel task) {
        TaskPlacement placement = new TaskPlacement();
        placement.startX = viewport.weeklyStartX(task);
        placement.width = viewport.weeklyWidth(task);
        placement.anchor = "start";
        placement.labelX = placement.startX + placement.width + 8;
        return placement;
    }

    public static class TaskPlacement {
        public int startX;
        public int width;
        public int labelX;
        public String anchor;
    }
}
