/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuproject.wbssvg;

import jp.igapyon.mikuproject.model.TaskModel;

public class WbsSvgLabels {
    public void appendTaskLabel(StringBuilder builder, WbsSvgTimeline.TaskPlacement placement, int y, String escapedLabel) {
        appendTaskLabel(builder, null, placement, y, escapedLabel);
    }

    public void appendTaskLabel(StringBuilder builder, TaskModel task, WbsSvgTimeline.TaskPlacement placement, int y,
            String escapedLabel) {
        builder.append("<text class=\"").append(task != null && task.summary ? "phaseLabel" : "label")
                .append("\" x=\"").append(placement.labelX).append("\" y=\"").append(y)
                .append("\" text-anchor=\"").append(placement.anchor).append("\">")
                .append(escapedLabel)
                .append("</text>");
    }
}
