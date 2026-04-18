/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuproject.projectpatchjson;

import java.util.Map;

public class PatchOperation {
    public String op;
    public String uid;
    public String name;
    public String initials;
    public String group;
    public String calendarUid;
    public Double maxUnits;
    public Boolean isSummary;
    public String newParentUid;
    public Integer newIndex;
    public Boolean isMilestone;
    public String taskUid;
    public String resourceUid;
    public String start;
    public String finish;
    public Double units;
    public String work;
    public Integer percentWorkComplete;
    public String plannedStart;
    public String plannedFinish;
    public String plannedDuration;
    public Double plannedDurationHours;
    public String fromUid;
    public String toUid;
    public String type;
    public String lag;
    public Double lagHours;
    public Map<String, Object> fields;
    public Boolean isBaseCalendar;
    public String baseCalendarUid;
    public String standardRate;
    public String overtimeRate;
    public Double costPerUse;
}
