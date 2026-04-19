/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuproject.wbssvg;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import jp.igapyon.mikuproject.model.ProjectModel;
import jp.igapyon.mikuproject.model.TaskModel;
import jp.igapyon.mikuproject.wbsdateband.WbsDateband;

public class WbsSvg {
    private final WbsDateband dateband = new WbsDateband();
    private final WbsSvgRender render = new WbsSvgRender(dateband);
    private final WbsSvgZip zip = new WbsSvgZip();
    private final WbsSvgCalendar calendar = new WbsSvgCalendar(dateband, render, zip);

    public String exportNativeSvg(ProjectModel model) {
        return exportNativeSvg(model, null);
    }

    public String exportNativeSvg(ProjectModel model, NativeSvgOptions options) {
        NativeSvgOptions actualOptions = options == null ? new NativeSvgOptions() : options;
        return render.exportNativeSvg(model, actualOptions);
    }

    public String exportWeeklyNativeSvg(ProjectModel model) {
        return exportWeeklyNativeSvg(model, null);
    }

    public String exportWeeklyNativeSvg(ProjectModel model, NativeSvgOptions options) {
        NativeSvgOptions actualOptions = options == null ? new NativeSvgOptions() : options;
        return render.exportWeeklyNativeSvg(model, actualOptions);
    }

    public MonthlyCalendarSvgArchive exportMonthlyWbsCalendarSvgArchive(ProjectModel model) {
        return calendar.exportMonthlyWbsCalendarSvgArchive(model);
    }

    public List<String> collectWbsHolidayDates(ProjectModel model) {
        return dateband.collectWbsHolidayDates(model);
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
