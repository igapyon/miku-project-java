/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuproject.model;

import java.util.ArrayList;
import java.util.List;

public class WorkWeekModel {
    public String name;
    public String fromDate;
    public String toDate;
    public List<WeekDayModel> weekDays = new ArrayList<WeekDayModel>();
}
