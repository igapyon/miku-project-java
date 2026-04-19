/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuproject.wbssvg;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import jp.igapyon.mikuproject.model.PredecessorModel;
import jp.igapyon.mikuproject.model.ProjectModel;
import jp.igapyon.mikuproject.model.TaskModel;
import jp.igapyon.mikuproject.msprojectxml.MsProjectSamples;
import jp.igapyon.mikuproject.msprojectxml.MsProjectXml;

public class WbsSvgTest {
    @Test
    public void exportsDailyAndWeeklySvg() {
        MsProjectXml xml = new MsProjectXml();
        ProjectModel model = xml.importFromXml(new MsProjectSamples().buildSampleXml());

        String dailySvg = xml.exportNativeSvg(model);
        String weeklySvg = xml.exportWeeklyNativeSvg(model);

        assertTrue(dailySvg.contains("<svg"));
        assertTrue(dailySvg.contains("mikuproject開発"));
        assertTrue(weeklySvg.contains("<svg"));
        assertTrue(weeklySvg.contains("weekly overview"));
    }

    @Test
    public void rendersDependencyConnectorsInDailyAndWeeklySvg() {
        MsProjectXml xml = new MsProjectXml();
        ProjectModel model = new ProjectModel();
        model.project.name = "Dependency Demo";
        model.project.startDate = "2026-03-16T09:00:00";
        model.project.finishDate = "2026-03-20T18:00:00";
        model.project.currentDate = "2026-03-18T12:00:00";
        TaskModel prep = task("1", "Prep", "2026-03-16T09:00:00", "2026-03-17T18:00:00");
        TaskModel ship = task("2", "Ship", "2026-03-18T09:00:00", "2026-03-19T18:00:00");
        PredecessorModel predecessor = new PredecessorModel();
        predecessor.predecessorUid = "1";
        predecessor.type = Integer.valueOf(1);
        ship.predecessors.add(predecessor);
        model.tasks.add(prep);
        model.tasks.add(ship);
        model = xml.normalizeProjectModel(model);

        String dailySvg = xml.exportNativeSvg(model);
        String weeklySvg = xml.exportWeeklyNativeSvg(model);

        assertTrue(dailySvg.contains("class=\"dependencyPath\""));
        assertTrue(dailySvg.contains("marker-end=\"url(#dependencyArrow)\""));
        assertTrue(dailySvg.contains("data-from-uid=\"1\""));
        assertTrue(dailySvg.contains("data-to-uid=\"2\""));
        assertTrue(weeklySvg.contains("class=\"dependencyPath\""));
    }

    @Test
    public void exportsMonthlyCalendarArchive() {
        MsProjectXml xml = new MsProjectXml();
        ProjectModel model = xml.importFromXml(new MsProjectSamples().buildSampleXml());

        WbsSvg.MonthlyCalendarSvgArchive archive = xml.exportMonthlyWbsCalendarSvgArchive(model);

        assertTrue(archive.entries.size() > 0);
        assertTrue(archive.entries.get(0).fileName.endsWith(".svg"));
        assertTrue(archive.entries.get(0).svg.contains("<svg"));
        assertTrue(archive.zipBytes.length > 0);
    }

    private TaskModel task(String uid, String name, String start, String finish) {
        TaskModel task = new TaskModel();
        task.uid = uid;
        task.id = uid;
        task.name = name;
        task.outlineLevel = Integer.valueOf(1);
        task.outlineNumber = uid;
        task.wbs = uid;
        task.start = start;
        task.finish = finish;
        task.duration = "PT16H0M0S";
        task.percentComplete = Integer.valueOf(0);
        return task;
    }
}
