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

    public void appendDependencyDefs(StringBuilder builder) {
        builder.append("<defs><marker id=\"dependencyArrow\" markerWidth=\"10\" markerHeight=\"10\" refX=\"8\" refY=\"3\" orient=\"auto\"><path d=\"M0,0 L0,6 L9,3 z\" fill=\"#555\"/></marker></defs>");
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
}
