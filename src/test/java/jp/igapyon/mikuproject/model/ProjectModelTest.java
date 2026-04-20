/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuproject.model;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import jp.igapyon.mikuproject.msprojectxml.MsProjectXml;

public class ProjectModelTest {
    @Test
    public void modelListsAreInitialized() {
        ProjectModel model = new ProjectModel();

        assertNotNull(model.project);
        assertNotNull(model.tasks);
        assertNotNull(model.resources);
        assertNotNull(model.assignments);
        assertNotNull(model.calendars);
        assertTrue(model.tasks.isEmpty());
        assertTrue(model.resources.isEmpty());
        assertTrue(model.assignments.isEmpty());
        assertTrue(model.calendars.isEmpty());
    }

    @Test
    public void normalizeProjectModelInitializesNestedLists() {
        MsProjectXml xml = new MsProjectXml();
        ProjectModel model = new ProjectModel();

        model.project.outlineCodes = null;
        model.project.wbsMasks = null;
        model.project.extendedAttributes = null;

        TaskModel task = new TaskModel();
        task.extendedAttributes = null;
        task.baselines = null;
        task.timephasedData = null;
        task.predecessors = null;
        model.tasks.add(task);

        ResourceModel resource = new ResourceModel();
        resource.extendedAttributes = null;
        resource.baselines = null;
        resource.timephasedData = null;
        model.resources.add(resource);

        AssignmentModel assignment = new AssignmentModel();
        assignment.extendedAttributes = null;
        assignment.baselines = null;
        assignment.timephasedData = null;
        model.assignments.add(assignment);

        CalendarModel calendar = new CalendarModel();
        calendar.weekDays = null;
        calendar.exceptions = null;
        calendar.workWeeks = null;
        model.calendars.add(calendar);

        ProjectModel normalized = xml.normalizeProjectModel(model);

        assertNotNull(normalized.project.outlineCodes);
        assertNotNull(normalized.project.wbsMasks);
        assertNotNull(normalized.project.extendedAttributes);
        assertNotNull(normalized.tasks.get(0).extendedAttributes);
        assertNotNull(normalized.tasks.get(0).baselines);
        assertNotNull(normalized.tasks.get(0).timephasedData);
        assertNotNull(normalized.tasks.get(0).predecessors);
        assertNotNull(normalized.resources.get(0).extendedAttributes);
        assertNotNull(normalized.resources.get(0).baselines);
        assertNotNull(normalized.resources.get(0).timephasedData);
        assertNotNull(normalized.assignments.get(0).extendedAttributes);
        assertNotNull(normalized.assignments.get(0).baselines);
        assertNotNull(normalized.assignments.get(0).timephasedData);
        assertNotNull(normalized.calendars.get(0).weekDays);
        assertNotNull(normalized.calendars.get(0).exceptions);
        assertNotNull(normalized.calendars.get(0).workWeeks);
        assertFalse(normalized.tasks.get(0).predecessors == null);
    }
}
