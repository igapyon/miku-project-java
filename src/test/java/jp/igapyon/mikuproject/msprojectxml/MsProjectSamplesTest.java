/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuproject.msprojectxml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import jp.igapyon.mikuproject.model.ProjectModel;

public class MsProjectSamplesTest {
    @Test
    public void buildSampleProjectModelCreatesExpectedSample() {
        MsProjectSamples samples = new MsProjectSamples();

        ProjectModel model = samples.buildSampleProjectModel();

        assertEquals("mikuproject開発", model.project.name);
        assertEquals("2026-03-23T09:00:00", model.project.currentDate);
        assertEquals("2026-03-23T09:00:00", model.project.statusDate);
        assertEquals(Integer.valueOf(480), model.project.minutesPerDay);
        assertEquals(13, model.tasks.size());
        assertEquals(1, model.resources.size());
        assertEquals(2, model.assignments.size());
        assertEquals("1", model.tasks.get(0).outlineNumber);
        assertTrue(model.tasks.get(0).summary);
        assertTrue(model.tasks.get(1).milestone);
        assertEquals("Mikuku", model.resources.get(0).name);
        assertEquals("draft-120", model.assignments.get(0).taskUid);
        assertEquals("res-1", model.assignments.get(0).resourceUid);
    }

    @Test
    public void buildSampleXmlRoundTripsThroughMsProjectXml() {
        MsProjectSamples samples = new MsProjectSamples();
        MsProjectXml xml = new MsProjectXml();

        String xmlText = samples.buildSampleXml(xml);
        ProjectModel reparsed = xml.importFromXml(xmlText);

        assertNotNull(xmlText);
        assertTrue(xmlText.contains("<Name>mikuproject開発</Name>"));
        assertTrue(xmlText.contains("<CurrentDate>2026-03-23T09:00:00</CurrentDate>"));
        assertEquals("mikuproject開発", reparsed.project.name);
        assertEquals("2026-03-23T09:00:00", reparsed.project.currentDate);
        assertEquals(13, reparsed.tasks.size());
        assertFalse(reparsed.calendars.isEmpty());
        assertTrue(xml.validateProjectModel(reparsed).isEmpty());
    }
}
