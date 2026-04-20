/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuproject.model;

import java.util.ArrayList;
import java.util.List;

public class AssignmentModel {
    public String uid;
    public String taskUid;
    public String resourceUid;
    public String start;
    public String finish;
    public String startVariance;
    public String finishVariance;
    public String delay;
    public Boolean milestone;
    public Integer workContour;
    public Double units;
    public String work;
    public Double cost;
    public Double actualCost;
    public Double remainingCost;
    public Integer percentWorkComplete;
    public String overtimeWork;
    public String actualOvertimeWork;
    public String actualWork;
    public String remainingWork;
    public List<AssignmentExtendedAttributeModel> extendedAttributes = new ArrayList<AssignmentExtendedAttributeModel>();
    public List<AssignmentBaselineModel> baselines = new ArrayList<AssignmentBaselineModel>();
    public List<AssignmentTimephasedDataModel> timephasedData = new ArrayList<AssignmentTimephasedDataModel>();
}
