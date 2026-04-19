/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuproject.wbssvg;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import jp.igapyon.mikuproject.excelio.ExcelIoUtil;

public class WbsSvgZip {
    public byte[] packMonthlyEntries(List<WbsSvg.MonthlyCalendarEntry> entries) {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        ZipOutputStream zip = null;
        try {
            zip = new ZipOutputStream(buffer);
            for (WbsSvg.MonthlyCalendarEntry entry : entries) {
                zip.putNextEntry(new ZipEntry("monthly-calendar/" + entry.fileName));
                zip.write(ExcelIoUtil.encodeUtf8(entry.svg));
                zip.closeEntry();
            }
            zip.finish();
            return buffer.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("monthly calendar zip の生成に失敗しました", ex);
        } finally {
            if (zip != null) {
                try {
                    zip.close();
                } catch (IOException ex) {
                    // ignore
                }
            }
        }
    }
}
