/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuproject.wbssvg;

import java.util.ArrayList;
import java.util.List;
import jp.igapyon.mikuproject.model.ProjectModel;

public class WbsSvg {
    private final WbsSvgPublic publicApi = new WbsSvgPublic();

    public String exportNativeSvg(ProjectModel model) {
        return exportNativeSvg(model, null);
    }

    public String exportNativeSvg(ProjectModel model, NativeSvgOptions options) {
        return publicApi.exportNativeSvg(model, options);
    }

    public String exportWeeklyNativeSvg(ProjectModel model) {
        return exportWeeklyNativeSvg(model, null);
    }

    public String exportWeeklyNativeSvg(ProjectModel model, NativeSvgOptions options) {
        return publicApi.exportWeeklyNativeSvg(model, options);
    }

    public MonthlyCalendarSvgArchive exportMonthlyWbsCalendarSvgArchive(ProjectModel model) {
        return exportMonthlyWbsCalendarSvgArchive(model, null);
    }

    public MonthlyCalendarSvgArchive exportMonthlyWbsCalendarSvgArchive(ProjectModel model, NativeSvgOptions options) {
        return publicApi.exportMonthlyWbsCalendarSvgArchive(model, options);
    }

    public List<String> collectWbsHolidayDates(ProjectModel model) {
        return publicApi.collectWbsHolidayDates(model);
    }

    public static class NativeSvgOptions {
        public List<String> holidayDates = new ArrayList<String>();
        public Integer displayDaysBeforeBaseDate;
        public Integer displayDaysAfterBaseDate;
        public Boolean useBusinessDaysForDisplayRange;
        public Boolean useBusinessDaysForProgressBand;
        public String labelMode;
    }

    public static class MonthlyCalendarSvgArchive {
        public final List<MonthlyCalendarEntry> entries = new ArrayList<MonthlyCalendarEntry>();
        public byte[] zipBytes;
    }

    public static class MonthlyCalendarEntry {
        public final String fileName;
        public final String svg;

        public MonthlyCalendarEntry(String fileName, String svg) {
            this.fileName = fileName;
            this.svg = svg;
        }
    }
}
