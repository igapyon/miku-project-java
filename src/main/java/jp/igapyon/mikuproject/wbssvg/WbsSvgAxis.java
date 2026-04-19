/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuproject.wbssvg;

public class WbsSvgAxis {
    public void appendDailyAxis(StringBuilder builder) {
        builder.append("<line x1=\"120\" y1=\"44\" x2=\"1120\" y2=\"44\" stroke=\"#D0D7DE\"/>");
        builder.append("<text x=\"120\" y=\"40\" font-size=\"12\" fill=\"#666\">daily timeline</text>");
    }

    public void appendWeeklyAxis(StringBuilder builder) {
        builder.append("<line x1=\"120\" y1=\"68\" x2=\"920\" y2=\"68\" stroke=\"#D0D7DE\"/>");
        builder.append("<text x=\"120\" y=\"64\" font-size=\"12\" fill=\"#666\">weekly timeline</text>");
    }
}
