/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuproject.wbssvg;

import java.util.ArrayList;
import java.util.List;

import jp.igapyon.mikuproject.excelio.ExcelIoUtil;
import jp.igapyon.mikuproject.excelio.ExcelIoZip;

public class WbsSvgZip {
    private final ExcelIoZip zip = new ExcelIoZip();

    public byte[] packMonthlyEntries(List<WbsSvg.MonthlyCalendarEntry> entries) {
        List<ExcelIoZip.ZipEntryData> zipEntries = new ArrayList<ExcelIoZip.ZipEntryData>();
        for (WbsSvg.MonthlyCalendarEntry entry : entries) {
            zipEntries.add(new ExcelIoZip.ZipEntryData("monthly-calendar/" + entry.fileName, ExcelIoUtil.encodeUtf8(entry.svg)));
        }
        return zip.packZip(zipEntries, ExcelIoZip.FIXED_2025_01_01_MOD_TIME, ExcelIoZip.FIXED_2025_01_01_MOD_DATE);
    }
}
