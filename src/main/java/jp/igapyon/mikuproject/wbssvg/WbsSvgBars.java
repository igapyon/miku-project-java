/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuproject.wbssvg;

public class WbsSvgBars {
    public void appendDailyBar(StringBuilder builder, WbsSvgTimeline.TaskPlacement placement, int y) {
        builder.append("<rect x=\"").append(placement.startX).append("\" y=\"").append(y - 12)
                .append("\" width=\"").append(placement.width).append("\" height=\"14\" fill=\"#5b8def\" rx=\"3\" ry=\"3\"/>");
    }

    public void appendWeeklyBar(StringBuilder builder, WbsSvgTimeline.TaskPlacement placement, int y) {
        builder.append("<rect x=\"").append(placement.startX).append("\" y=\"").append(y - 12)
                .append("\" width=\"").append(placement.width).append("\" height=\"14\" fill=\"#7bb661\" rx=\"3\" ry=\"3\"/>");
    }
}
