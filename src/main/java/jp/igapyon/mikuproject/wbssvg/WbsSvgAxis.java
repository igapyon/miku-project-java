/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuproject.wbssvg;

import java.util.List;
import java.util.Set;

import jp.igapyon.mikuproject.wbsdateband.WbsDateband;

public class WbsSvgAxis {
    public void appendDailyAxis(StringBuilder builder) {
        builder.append("<line x1=\"120\" y1=\"44\" x2=\"1120\" y2=\"44\" stroke=\"#D0D7DE\"/>");
        builder.append("<text x=\"120\" y=\"40\" font-size=\"12\" fill=\"#666\">daily timeline</text>");
    }

    public void appendWeeklyAxis(StringBuilder builder) {
        builder.append("<line x1=\"120\" y1=\"68\" x2=\"920\" y2=\"68\" stroke=\"#D0D7DE\"/>");
        builder.append("<text x=\"120\" y=\"64\" font-size=\"12\" fill=\"#666\">weekly timeline</text>");
    }

    public void appendDailyAxis(StringBuilder builder, List<String> dateBand, int chartOriginX, int chartOriginY,
            int svgHeight, int dayWidth, Set<String> holidaySet, Set<Integer> nonWorkingDayTypes, WbsDateband dateband,
            String today) {
        for (int index = 0; index < dateBand.size(); index++) {
            String day = dateBand.get(index);
            int x = chartOriginX + index * dayWidth;
            boolean holiday = holidaySet != null && holidaySet.contains(day);
            boolean weekend = nonWorkingDayTypes != null && dateband.isWeeklyNonWorkingDay(day, nonWorkingDayTypes);
            String fill = holiday ? "#fce4ec" : (weekend ? "#eef3f8" : "#ffffff");
            builder.append("<rect x=\"").append(x).append("\" y=\"").append(chartOriginY - 52)
                    .append("\" width=\"").append(dayWidth).append("\" height=\"")
                    .append(svgHeight - 58).append("\" fill=\"").append(fill).append("\"/>");
            builder.append("<line class=\"grid\" x1=\"").append(x).append("\" y1=\"").append(chartOriginY - 56)
                    .append("\" x2=\"").append(x).append("\" y2=\"").append(svgHeight - 28).append("\" />");
            builder.append("<text class=\"axis\" x=\"").append(x + dayWidth / 2).append("\" y=\"")
                    .append(chartOriginY - 28).append("\" text-anchor=\"middle\">")
                    .append(formatDailyLabel(day)).append("</text>");
        }
        int endX = chartOriginX + dateBand.size() * dayWidth;
        builder.append("<line class=\"grid\" x1=\"").append(endX).append("\" y1=\"").append(chartOriginY - 56)
                .append("\" x2=\"").append(endX).append("\" y2=\"").append(svgHeight - 28).append("\" />");
        if (today != null && today.length() >= 10) {
            String todayKey = today.substring(0, 10);
            int todayIndex = dateBand.indexOf(todayKey);
            if (todayIndex >= 0) {
                int todayX = chartOriginX + todayIndex * dayWidth + dayWidth / 2;
                builder.append("<line class=\"today\" x1=\"").append(todayX).append("\" y1=\"")
                        .append(chartOriginY - 56).append("\" x2=\"").append(todayX).append("\" y2=\"")
                        .append(svgHeight - 28).append("\" />");
            }
        }
    }

    public void appendWeeklyAxis(StringBuilder builder, List<WeeklyBand> weeklyBand, int chartOriginX, int chartOriginY,
            int svgHeight, int weekWidth, String today) {
        builder.append("<text class=\"axisTitle\" x=\"").append(chartOriginX).append("\" y=\"")
                .append(chartOriginY - 40).append("\" font-size=\"12\" fill=\"#666\">weekly timeline</text>");
        String currentMonth = null;
        int monthStartIndex = 0;
        for (int index = 0; index <= weeklyBand.size(); index++) {
            String month = index < weeklyBand.size() ? weeklyBand.get(index).monthKey : null;
            if (index == 0) {
                currentMonth = month;
                monthStartIndex = 0;
            }
            if (index == weeklyBand.size() || !safeEquals(currentMonth, month)) {
                int x = chartOriginX + monthStartIndex * weekWidth;
                int width = (index - monthStartIndex) * weekWidth;
                builder.append("<text class=\"monthAxis\" x=\"").append(x + width / 2).append("\" y=\"")
                        .append(chartOriginY - 18).append("\" text-anchor=\"middle\">")
                        .append(currentMonth).append("</text>");
                builder.append("<line class=\"monthBoundary\" x1=\"").append(x).append("\" y1=\"")
                        .append(chartOriginY - 12).append("\" x2=\"").append(x).append("\" y2=\"")
                        .append(svgHeight - 20).append("\"/>");
                currentMonth = month;
                monthStartIndex = index;
            }
        }
        int endX = chartOriginX + weeklyBand.size() * weekWidth;
        builder.append("<line class=\"monthBoundary\" x1=\"").append(endX).append("\" y1=\"").append(chartOriginY - 12)
                .append("\" x2=\"").append(endX).append("\" y2=\"").append(svgHeight - 20).append("\"/>");

        for (int index = 0; index < weeklyBand.size(); index++) {
            WeeklyBand week = weeklyBand.get(index);
            int x = chartOriginX + index * weekWidth;
            builder.append("<line class=\"grid\" x1=\"").append(x).append("\" y1=\"").append(chartOriginY - 12)
                    .append("\" x2=\"").append(x).append("\" y2=\"").append(svgHeight - 20).append("\"/>");
            builder.append("<text class=\"weekAxis\" x=\"").append(x + weekWidth / 2).append("\" y=\"")
                    .append(chartOriginY + 4).append("\" text-anchor=\"middle\">")
                    .append(formatDailyLabel(week.startDay)).append("</text>");
        }
        int todayIndex = indexOfWeek(weeklyBand, today);
        if (todayIndex >= 0) {
            int todayX = chartOriginX + todayIndex * weekWidth + weekWidth / 2;
            builder.append("<line class=\"today\" x1=\"").append(todayX).append("\" y1=\"").append(chartOriginY - 12)
                    .append("\" x2=\"").append(todayX).append("\" y2=\"").append(svgHeight - 20)
                    .append("\"/>");
        }
    }

    public int indexOfWeek(List<WeeklyBand> weeklyBand, String value) {
        if (value == null || value.length() < 10) {
            return -1;
        }
        String day = value.substring(0, 10);
        for (int index = 0; index < weeklyBand.size(); index++) {
            WeeklyBand week = weeklyBand.get(index);
            if (day.compareTo(week.startDay) >= 0 && day.compareTo(week.endDay) <= 0) {
                return index;
            }
        }
        return -1;
    }

    private String formatDailyLabel(String day) {
        if (day == null || day.length() < 10) {
            return "";
        }
        return Integer.parseInt(day.substring(5, 7)) + "/" + Integer.parseInt(day.substring(8, 10));
    }

    private boolean safeEquals(String left, String right) {
        return left == null ? right == null : left.equals(right);
    }

    public static class WeeklyBand {
        public String startDay;
        public String endDay;
        public String monthKey;

        public WeeklyBand(String startDay, String endDay, String monthKey) {
            this.startDay = startDay;
            this.endDay = endDay;
            this.monthKey = monthKey;
        }
    }
}
