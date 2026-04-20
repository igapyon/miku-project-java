/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuproject.wbssvg;

import java.util.Calendar;
import java.util.Date;

import jp.igapyon.mikuproject.model.TaskModel;
import jp.igapyon.mikuproject.wbsdateband.WbsDateband;

public class WbsSvgViewport {
    private final WbsDateband dateband;
    private Date baseDate;

    public WbsSvgViewport(WbsDateband dateband) {
        this.dateband = dateband;
    }

    public void setBaseDate(Date baseDate) {
        this.baseDate = baseDate;
    }

    public int dailyStartX(TaskModel task) {
        java.util.Date date = dateband.parseDateOnly(task.start);
        if (date == null) {
            return 120;
        }
        long diff = date.getTime() - baseDate().getTime();
        int days = (int) Math.max(0, diff / (24L * 60L * 60L * 1000L));
        return 120 + days * 28;
    }

    public int dailyWidth(TaskModel task) {
        int days = Math.max(1, dateband.buildDateBand(task.start, task.finish).size());
        return days * 26;
    }

    public int weeklyStartX(TaskModel task) {
        java.util.Date date = dateband.parseDateOnly(task.start);
        if (date == null) {
            return 120;
        }
        long diff = date.getTime() - baseDate().getTime();
        int weeks = (int) Math.max(0, diff / (7L * 24L * 60L * 60L * 1000L));
        return 120 + weeks * 56;
    }

    public int weeklyWidth(TaskModel task) {
        int days = Math.max(1, dateband.buildDateBand(task.start, task.finish).size());
        return Math.max(40, ((days + 6) / 7) * 52);
    }

    private Date baseDate() {
        if (baseDate != null) {
            return baseDate;
        }
        Calendar base = Calendar.getInstance();
        base.clear();
        base.set(2026, Calendar.MARCH, 1);
        return base.getTime();
    }
}
