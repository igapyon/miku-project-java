/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuproject.wbssvg;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

import jp.igapyon.mikuproject.excelio.ExcelIoZip;
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
        assertTrue(dailySvg.contains("miku-project開発"));
        assertTrue(dailySvg.contains("class=\"grid\""));
        assertTrue(dailySvg.contains(">3/16</text>"));
        assertTrue(weeklySvg.contains("<svg"));
        assertTrue(weeklySvg.contains("weekly overview"));
        assertTrue(weeklySvg.contains("class=\"monthAxis\""));
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
    public void keepsDailySvgTasksInsideViewBoxForLateZeroDurationTasks() {
        MsProjectXml xml = new MsProjectXml();
        ProjectModel model = new ProjectModel();
        model.project.name = "Late Zero Duration Demo";
        model.project.startDate = "2026-04-20T20:36:58";
        model.project.finishDate = "2026-04-20T20:36:58";
        model.tasks.add(task("1", "First", "2026-04-20T20:36:58", "2026-04-20T20:36:58"));
        model.tasks.add(task("2", "Second", "2026-04-20T20:36:58", "2026-04-20T20:36:58"));
        model = xml.normalizeProjectModel(model);

        String dailySvg = xml.exportNativeSvg(model);

        assertTrue(dailySvg.contains("<rect x=\"6\""));
        assertFalse(dailySvg.contains("x=\"1520\""));
    }

    @Test
    public void exportsMonthlyCalendarArchive() {
        MsProjectXml xml = new MsProjectXml();
        ProjectModel model = xml.importFromXml(new MsProjectSamples().buildSampleXml());

        WbsSvg.MonthlyCalendarSvgArchive archive = xml.exportMonthlyWbsCalendarSvgArchive(model);

        assertTrue(archive.entries.size() > 0);
        assertTrue(archive.entries.get(0).fileName.endsWith(".svg"));
        assertTrue(archive.entries.get(0).svg.contains("<svg"));
        assertTrue(archive.entries.get(0).svg.contains("class=\"weekday\""));
        assertTrue(archive.entries.get(0).svg.contains("class=\"cellBorder\""));
        assertTrue(archive.zipBytes.length > 0);
        assertTrue(readUnsignedShortLE(archive.zipBytes, 10) == ExcelIoZip.FIXED_2025_01_01_MOD_TIME);
        assertTrue(readUnsignedShortLE(archive.zipBytes, 12) == ExcelIoZip.FIXED_2025_01_01_MOD_DATE);
    }

    @Test
    public void exportsDependencyFixtureIntoSvgOutputs() throws IOException {
        MsProjectXml xml = new MsProjectXml();
        ProjectModel model = xml.importFromXml(readVendorTestdata("dependency.xml"));

        String dailySvg = xml.exportNativeSvg(model);
        String weeklySvg = xml.exportWeeklyNativeSvg(model);

        assertTrue(dailySvg.contains("Dependency Project"));
        assertTrue(dailySvg.contains("Prepare"));
        assertTrue(dailySvg.contains("Execute"));
        assertTrue(dailySvg.contains("class=\"dependencyPath\""));
        assertTrue(weeklySvg.contains("weekly overview"));
        assertTrue(weeklySvg.contains("Prepare"));
    }

    @Test
    public void exportsHierarchyFixtureIntoSvgOutputs() throws IOException {
        MsProjectXml xml = new MsProjectXml();
        ProjectModel model = xml.importFromXml(readVendorTestdata("hierarchy.xml"));

        String dailySvg = xml.exportNativeSvg(model);
        String weeklySvg = xml.exportWeeklyNativeSvg(model);

        assertTrue(dailySvg.contains("Hierarchy Project"));
        assertTrue(dailySvg.contains("Child A"));
        assertTrue(dailySvg.contains("Child B"));
        assertTrue(weeklySvg.contains("weekly overview"));
        assertTrue(weeklySvg.contains("Child A"));
        assertTrue(weeklySvg.contains("Child B"));
    }

    @Test
    public void appliesLabelAndHolidayOptionsToSvgOutputs() throws IOException {
        MsProjectXml xml = new MsProjectXml();
        ProjectModel model = xml.importFromXml(readVendorTestdata("dependency.xml"));
        WbsSvg.NativeSvgOptions options = new WbsSvg.NativeSvgOptions();
        options.labelMode = "uid";
        options.holidayDates.add("2026-03-20");
        options.holidayDates.add("2026-03-21");

        String dailySvg = xml.exportNativeSvg(model, options);
        String weeklySvg = xml.exportWeeklyNativeSvg(model, options);
        WbsSvg.MonthlyCalendarSvgArchive archive = xml.exportMonthlyWbsCalendarSvgArchive(model, options);

        assertTrue(dailySvg.contains(">1</text>"));
        assertTrue(dailySvg.contains(">2</text>"));
        assertTrue(weeklySvg.contains(">1</text>"));
        assertTrue(weeklySvg.contains(">2</text>"));
        assertTrue(archive.entries.get(0).svg.contains("#fce7ef"));
        assertTrue(archive.entries.get(0).svg.contains(">1</text>"));
    }

    @Test
    public void exportsMonthlyCalendarArchiveForSampleAndFixtureRanges() throws IOException {
        MsProjectXml xml = new MsProjectXml();

        WbsSvg.MonthlyCalendarSvgArchive sampleArchive = xml
                .exportMonthlyWbsCalendarSvgArchive(xml.importFromXml(new MsProjectSamples().buildSampleXml()));
        WbsSvg.MonthlyCalendarSvgArchive dependencyArchive = xml.exportMonthlyWbsCalendarSvgArchive(
                xml.importFromXml(readVendorTestdata("dependency.xml")));
        WbsSvg.MonthlyCalendarSvgArchive hierarchyArchive = xml.exportMonthlyWbsCalendarSvgArchive(
                xml.importFromXml(readVendorTestdata("hierarchy.xml")));

        assertEquals(2, sampleArchive.entries.size());
        assertEquals("2026-03.svg", sampleArchive.entries.get(0).fileName);
        assertEquals("2026-04.svg", sampleArchive.entries.get(1).fileName);
        assertTrue(sampleArchive.entries.get(0).svg.contains("<svg"));
        assertTrue(sampleArchive.entries.get(1).svg.contains("<svg"));

        assertEquals(1, dependencyArchive.entries.size());
        assertEquals("2026-03.svg", dependencyArchive.entries.get(0).fileName);
        assertTrue(dependencyArchive.entries.get(0).svg.contains("Dependency Project"));
        assertTrue(dependencyArchive.entries.get(0).svg.contains("class=\"cellBorder\""));

        assertEquals(1, hierarchyArchive.entries.size());
        assertEquals("2026-03.svg", hierarchyArchive.entries.get(0).fileName);
        assertTrue(hierarchyArchive.entries.get(0).svg.contains("Hierarchy Project"));
        assertTrue(hierarchyArchive.entries.get(0).svg.contains("Child A"));
    }

    private String readVendorTestdata(String fileName) throws IOException {
        byte[] bytes = Files.readAllBytes(Paths.get("vendor", "miku-project", "testdata", fileName));
        return new String(bytes, StandardCharsets.UTF_8);
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

    private int readUnsignedShortLE(byte[] bytes, int offset) {
        return (bytes[offset] & 0xff) | ((bytes[offset + 1] & 0xff) << 8);
    }
}
