/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuproject.wbssvg;

import java.util.List;

import jp.igapyon.mikuproject.model.ProjectModel;
import jp.igapyon.mikuproject.wbsdateband.WbsDateband;

public class WbsSvgPublic {
    private final WbsDateband dateband = new WbsDateband();
    private final WbsSvgRender render = new WbsSvgRender(dateband);
    private final WbsSvgZip zip = new WbsSvgZip();
    private final WbsSvgCalendar calendar = new WbsSvgCalendar(dateband, render, zip);

    public String exportNativeSvg(ProjectModel model) {
        return exportNativeSvg(model, null);
    }

    public String exportNativeSvg(ProjectModel model, WbsSvg.NativeSvgOptions options) {
        WbsSvg.NativeSvgOptions actualOptions = options == null ? new WbsSvg.NativeSvgOptions() : options;
        return render.exportNativeSvg(model, actualOptions);
    }

    public String exportWeeklyNativeSvg(ProjectModel model) {
        return exportWeeklyNativeSvg(model, null);
    }

    public String exportWeeklyNativeSvg(ProjectModel model, WbsSvg.NativeSvgOptions options) {
        WbsSvg.NativeSvgOptions actualOptions = options == null ? new WbsSvg.NativeSvgOptions() : options;
        return render.exportWeeklyNativeSvg(model, actualOptions);
    }

    public WbsSvg.MonthlyCalendarSvgArchive exportMonthlyWbsCalendarSvgArchive(ProjectModel model) {
        return exportMonthlyWbsCalendarSvgArchive(model, null);
    }

    public WbsSvg.MonthlyCalendarSvgArchive exportMonthlyWbsCalendarSvgArchive(ProjectModel model, WbsSvg.NativeSvgOptions options) {
        return calendar.exportMonthlyWbsCalendarSvgArchive(model, options);
    }

    public List<String> collectWbsHolidayDates(ProjectModel model) {
        return dateband.collectWbsHolidayDates(model);
    }
}
