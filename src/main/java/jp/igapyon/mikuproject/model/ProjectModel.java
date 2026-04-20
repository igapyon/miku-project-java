/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuproject.model;

import java.util.ArrayList;
import java.util.List;

public class ProjectModel {
    public ProjectInfo project = new ProjectInfo();
    public List<TaskModel> tasks = new ArrayList<TaskModel>();
    public List<ResourceModel> resources = new ArrayList<ResourceModel>();
    public List<AssignmentModel> assignments = new ArrayList<AssignmentModel>();
    public List<CalendarModel> calendars = new ArrayList<CalendarModel>();
}
