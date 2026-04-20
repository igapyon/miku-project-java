/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuproject.model;

import java.util.ArrayList;
import java.util.List;

public class TaskModel {
    public String uid;
    public String id;
    public String name;
    public Integer outlineLevel;
    public String outlineNumber;
    public String wbs;
    public Integer type;
    public String calendarUID;
    public Integer priority;
    public String start;
    public String finish;
    public String duration;
    public String actualStart;
    public String actualFinish;
    public String deadline;
    public String startVariance;
    public String finishVariance;
    public String work;
    public String workVariance;
    public String totalSlack;
    public String freeSlack;
    public Double cost;
    public Double actualCost;
    public Double remainingCost;
    public String remainingWork;
    public String actualWork;
    public boolean milestone;
    public boolean summary;
    public Boolean critical;
    public Integer percentComplete;
    public Integer percentWorkComplete;
    public String notes;
    public Integer constraintType;
    public String constraintDate;
    public List<TaskExtendedAttributeModel> extendedAttributes = new ArrayList<TaskExtendedAttributeModel>();
    public List<TaskBaselineModel> baselines = new ArrayList<TaskBaselineModel>();
    public List<TaskTimephasedDataModel> timephasedData = new ArrayList<TaskTimephasedDataModel>();
    public List<PredecessorModel> predecessors = new ArrayList<PredecessorModel>();
}
