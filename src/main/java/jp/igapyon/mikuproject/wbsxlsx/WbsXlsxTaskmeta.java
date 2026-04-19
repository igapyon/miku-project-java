/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuproject.wbsxlsx;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jp.igapyon.mikuproject.model.AssignmentModel;
import jp.igapyon.mikuproject.model.CalendarModel;
import jp.igapyon.mikuproject.model.ProjectModel;
import jp.igapyon.mikuproject.model.ResourceModel;

public class WbsXlsxTaskmeta {
    public Taskmeta collectTaskmeta(ProjectModel model) {
        Taskmeta taskmeta = new Taskmeta();
        for (CalendarModel calendar : model.calendars) {
            if (calendar != null) {
                taskmeta.calendarNameByUid.put(calendar.uid, calendar.name);
            }
        }
        for (ResourceModel resource : model.resources) {
            if (resource != null) {
                taskmeta.resourceNameByUid.put(resource.uid, resource.name);
            }
        }
        for (AssignmentModel assignment : model.assignments) {
            if (assignment == null || assignment.taskUid == null) {
                continue;
            }
            String name = taskmeta.resourceNameByUid.get(assignment.resourceUid);
            if (name == null) {
                continue;
            }
            List<String> names = taskmeta.resourceNamesByTaskUid.get(assignment.taskUid);
            if (names == null) {
                names = new ArrayList<String>();
                taskmeta.resourceNamesByTaskUid.put(assignment.taskUid, names);
            }
            if (!names.contains(name)) {
                names.add(name);
            }
        }
        return taskmeta;
    }

    public static class Taskmeta {
        public final Map<String, String> calendarNameByUid = new LinkedHashMap<String, String>();
        public final Map<String, String> resourceNameByUid = new LinkedHashMap<String, String>();
        public final Map<String, List<String>> resourceNamesByTaskUid = new LinkedHashMap<String, List<String>>();
    }
}
