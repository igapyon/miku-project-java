/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuproject.model;

import java.util.ArrayList;
import java.util.List;

public class ResourceModel {
    public String uid;
    public String id;
    public String name;
    public Integer type;
    public String initials;
    public String group;
    public Integer workGroup;
    public Double maxUnits;
    public String calendarUID;
    public String standardRate;
    public Integer standardRateFormat;
    public String overtimeRate;
    public Integer overtimeRateFormat;
    public Double costPerUse;
    public String work;
    public String actualWork;
    public String remainingWork;
    public Double cost;
    public Double actualCost;
    public Double remainingCost;
    public Integer percentWorkComplete;
    public List<ResourceExtendedAttributeModel> extendedAttributes = new ArrayList<ResourceExtendedAttributeModel>();
    public List<ResourceBaselineModel> baselines = new ArrayList<ResourceBaselineModel>();
    public List<ResourceTimephasedDataModel> timephasedData = new ArrayList<ResourceTimephasedDataModel>();
}
