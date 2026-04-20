/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuproject.wbssvg;

import jp.igapyon.mikuproject.model.TaskModel;

public class WbsSvgBars {
    public void appendDailyBar(StringBuilder builder, TaskModel task, WbsSvgTimeline.TaskPlacement placement, int y) {
        appendTaskBar(builder, task, placement, y, 6, 13);
    }

    public void appendWeeklyBar(StringBuilder builder, TaskModel task, WbsSvgTimeline.TaskPlacement placement, int y) {
        appendTaskBar(builder, task, placement, y, 4, 11);
    }

    private void appendTaskBar(StringBuilder builder, TaskModel task, WbsSvgTimeline.TaskPlacement placement, int y,
            int horizontalInset, int milestoneHalfWidth) {
        int percent = task.percentComplete == null ? 0 : Math.max(0, Math.min(100, task.percentComplete.intValue()));
        if (task.milestone) {
            int centerX = placement.startX + Math.max(placement.width / 2, milestoneHalfWidth);
            int centerY = y - 1;
            String fill = percent >= 100 ? "#d9efff" : "#ffffff";
            builder.append("<polygon points=\"")
                    .append(centerX).append(",").append(centerY - milestoneHalfWidth).append(" ")
                    .append(centerX + milestoneHalfWidth).append(",").append(centerY).append(" ")
                    .append(centerX).append(",").append(centerY + milestoneHalfWidth).append(" ")
                    .append(centerX - milestoneHalfWidth).append(",").append(centerY)
                    .append("\" fill=\"").append(fill).append("\" stroke=\"#4f95d6\" stroke-width=\"3\"/>");
            return;
        }
        int barX = placement.startX + horizontalInset;
        int barWidth = Math.max(10, placement.width - horizontalInset * 2);
        if (task.summary) {
            int lineY = y - 1;
            int progressEndX = barX + Math.max(0, Math.min(barWidth, Math.round(barWidth * percent / 100.0f)));
            builder.append("<line x1=\"").append(barX).append("\" y1=\"").append(lineY).append("\" x2=\"")
                    .append(barX + barWidth).append("\" y2=\"").append(lineY)
                    .append("\" stroke=\"#8eb9ea\" stroke-width=\"3\" stroke-linecap=\"round\"/>");
            if (progressEndX > barX) {
                builder.append("<line x1=\"").append(barX).append("\" y1=\"").append(lineY).append("\" x2=\"")
                        .append(progressEndX).append("\" y2=\"").append(lineY)
                        .append("\" stroke=\"#2f79d0\" stroke-width=\"3\" stroke-linecap=\"round\"/>");
            }
            return;
        }
        int progressWidth = Math.max(0, Math.min(barWidth, Math.round(barWidth * percent / 100.0f)));
        builder.append("<rect x=\"").append(barX).append("\" y=\"").append(y - 16)
                .append("\" width=\"").append(barWidth)
                .append("\" height=\"22\" rx=\"4\" fill=\"#d9efff\" stroke=\"#4f95d6\" stroke-width=\"3\"/>");
        if (progressWidth > 0) {
            builder.append("<rect x=\"").append(barX).append("\" y=\"").append(y - 16)
                    .append("\" width=\"").append(progressWidth)
                    .append("\" height=\"22\" rx=\"4\" fill=\"#3f86d8\" stroke=\"none\"/>");
        }
    }
}
