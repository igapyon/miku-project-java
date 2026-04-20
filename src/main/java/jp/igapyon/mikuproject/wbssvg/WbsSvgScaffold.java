/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuproject.wbssvg;

public class WbsSvgScaffold {
    public void appendSvgOpen(StringBuilder builder, int width, int height) {
        builder.append("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"")
                .append(width)
                .append("\" height=\"")
                .append(height)
                .append("\" viewBox=\"0 0 ")
                .append(width)
                .append(" ")
                .append(height)
                .append("\">");
    }

    public void appendDailyScaffold(StringBuilder builder, int width, int height, int chartOriginX, int chartWidth,
            int topPadding, String escapedProjectName) {
        appendSvgOpen(builder, width, height, escapedProjectName);
        appendBaseStyle(builder, false);
        appendDependencyDefs(builder);
        appendWhiteBackground(builder, width, height);
        appendCenteredTitle(builder, chartOriginX + chartWidth / 2, topPadding + 18, escapedProjectName);
    }

    public void appendWeeklyScaffold(StringBuilder builder, int width, int height, int chartOriginX, int chartWidth,
            int topPadding, String escapedProjectName, String escapedProjectStartDate, String escapedProjectFinishDate) {
        appendSvgOpen(builder, width, height, escapedProjectName + " weekly overview");
        appendBaseStyle(builder, true);
        appendDependencyDefs(builder);
        appendWhiteBackground(builder, width, height);
        int centerX = chartOriginX + chartWidth / 2;
        appendCenteredTitle(builder, centerX, topPadding + 18, escapedProjectName + " weekly overview");
        builder.append("<text class=\"meta\" x=\"").append(centerX).append("\" y=\"").append(topPadding + 40)
                .append("\" text-anchor=\"middle\">project range ")
                .append(escapedProjectStartDate).append(" - ").append(escapedProjectFinishDate).append("</text>");
    }

    public void appendSvgOpen(StringBuilder builder, int width, int height, String escapedAriaLabel) {
        builder.append("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"")
                .append(width)
                .append("\" height=\"")
                .append(height)
                .append("\" viewBox=\"0 0 ")
                .append(width)
                .append(" ")
                .append(height)
                .append("\" role=\"img\" aria-label=\"")
                .append(escapedAriaLabel)
                .append("\">");
    }

    public void appendBaseStyle(StringBuilder builder, boolean weekly) {
        builder.append("<style>");
        builder.append("text { font-family: 'Hiragino Sans', 'Yu Gothic', sans-serif; fill: #1d2740; }");
        builder.append(".title { font-size: 18px; font-weight: 700; }");
        builder.append(".axis { font-size: 12px; fill: #5b6370; }");
        builder.append(".label { font-size: 13px; }");
        builder.append(".phaseLabel { font-size: 13px; font-weight: 700; }");
        builder.append(".grid { stroke: #c9d3e1; stroke-width: 1; }");
        builder.append(".today { stroke: #ff6b5a; stroke-width: 2; }");
        builder.append(".dependencyPath { fill: none; stroke: #9eb6c8; stroke-width: 1.5; stroke-linecap: round; stroke-linejoin: round; opacity: 0.95; }");
        if (weekly) {
            builder.append(".meta { font-size: 12px; fill: #5b6370; }");
            builder.append(".monthAxis { font-size: 13px; font-weight: 700; fill: #475467; }");
            builder.append(".weekAxis { font-size: 10px; fill: #667085; }");
            builder.append(".monthBoundary { stroke: #98a2b3; stroke-width: 1.5; }");
        }
        builder.append("</style>");
    }

    public void appendDependencyDefs(StringBuilder builder) {
        builder.append("<defs><marker id=\"dependencyArrow\" markerWidth=\"7\" markerHeight=\"7\" refX=\"5.5\" refY=\"3.5\" orient=\"auto\" markerUnits=\"strokeWidth\"><path d=\"M0,0 L7,3.5 L0,7 Z\" fill=\"#9eb6c8\" /></marker></defs>");
    }

    public void appendProjectTitle(StringBuilder builder, String escapedProjectName) {
        builder.append("<text x=\"24\" y=\"32\" font-size=\"20\">").append(escapedProjectName).append("</text>");
    }

    public void appendWeeklySubtitle(StringBuilder builder) {
        builder.append("<text x=\"24\" y=\"56\">weekly overview</text>");
    }

    public void appendSvgClose(StringBuilder builder) {
        builder.append("</svg>");
    }

    private void appendWhiteBackground(StringBuilder builder, int width, int height) {
        builder.append("<rect x=\"0\" y=\"0\" width=\"").append(width).append("\" height=\"").append(height)
                .append("\" fill=\"#ffffff\"/>");
    }

    private void appendCenteredTitle(StringBuilder builder, int x, int y, String escapedTitle) {
        builder.append("<text class=\"title\" x=\"").append(x).append("\" y=\"").append(y)
                .append("\" text-anchor=\"middle\">").append(escapedTitle).append("</text>");
    }
}
