/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuproject.wbssvg;

public class WbsSvgLabels {
    public void appendTaskLabel(StringBuilder builder, WbsSvgTimeline.TaskPlacement placement, int y, String escapedLabel) {
        builder.append("<text x=\"").append(placement.labelX).append("\" y=\"").append(y)
                .append("\" text-anchor=\"").append(placement.anchor).append("\">")
                .append(escapedLabel)
                .append("</text>");
    }
}
