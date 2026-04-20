/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuproject.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import java.util.zip.ZipInputStream;

import org.junit.jupiter.api.Test;

import jp.igapyon.mikuproject.coreapi.CoreApiWorkbookXlsx;
import jp.igapyon.mikuproject.msprojectxml.MsProjectSamples;
import jp.igapyon.mikuproject.projectworkbookjson.WorkbookJsonDocument;
import jp.igapyon.mikuproject.projectxlsx.XlsxRowLike;
import jp.igapyon.mikuproject.projectxlsx.XlsxSheetLike;
import jp.igapyon.mikuproject.projectxlsx.XlsxWorkbookLike;

public class MikuprojectCliTest {
    @Test
    public void printsUsageForHelp() throws IOException {
        MikuprojectCli cli = new MikuprojectCli();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();

        int exitCode = cli.run(new String[] { "help" }, stream(out), stream(err));

        assertEquals(0, exitCode);
        assertTrue(text(out).contains("validate-xml"));
        assertEquals("", text(err));
    }

    @Test
    public void validatesXmlAndExportsFormats() throws IOException {
        Path xmlFile = Files.createTempFile("mikuproject-cli", ".xml");
        Files.write(xmlFile, new MsProjectSamples().buildSampleXml().getBytes(StandardCharsets.UTF_8));
        Path monthlyZipFile = Files.createTempFile("mikuproject-monthly", ".zip");
        Path bundleZipFile = Files.createTempFile("mikuproject-bundle", ".zip");
        Path reportDir = Files.createTempDirectory("mikuproject-report-dir");
        try {
            MikuprojectCli cli = new MikuprojectCli();
            ByteArrayOutputStream validateOut = new ByteArrayOutputStream();
            ByteArrayOutputStream validateErr = new ByteArrayOutputStream();
            ByteArrayOutputStream mermaidOut = new ByteArrayOutputStream();
            ByteArrayOutputStream markdownOut = new ByteArrayOutputStream();
            ByteArrayOutputStream dailySvgOut = new ByteArrayOutputStream();
            ByteArrayOutputStream weeklySvgOut = new ByteArrayOutputStream();
            ByteArrayOutputStream monthlyZipOut = new ByteArrayOutputStream();
            ByteArrayOutputStream bundleZipOut = new ByteArrayOutputStream();
            ByteArrayOutputStream reportDirOut = new ByteArrayOutputStream();

            assertEquals(0, cli.run(new String[] { "validate-xml", xmlFile.toString() }, stream(validateOut), stream(validateErr)));
            assertEquals(0, cli.run(new String[] { "export-mermaid", xmlFile.toString() }, stream(mermaidOut), stream(new ByteArrayOutputStream())));
            assertEquals(0, cli.run(new String[] { "export-wbs-markdown", xmlFile.toString() }, stream(markdownOut), stream(new ByteArrayOutputStream())));
            assertEquals(0, cli.run(new String[] { "export-daily-svg", xmlFile.toString() }, stream(dailySvgOut), stream(new ByteArrayOutputStream())));
            assertEquals(0, cli.run(new String[] { "export-weekly-svg", xmlFile.toString() }, stream(weeklySvgOut), stream(new ByteArrayOutputStream())));
            assertEquals(0, cli.run(new String[] { "export-monthly-svg-zip", xmlFile.toString(), monthlyZipFile.toString() }, stream(monthlyZipOut),
                    stream(new ByteArrayOutputStream())));
            assertEquals(0, cli.run(new String[] { "export-report-bundle", xmlFile.toString(), bundleZipFile.toString() }, stream(bundleZipOut),
                    stream(new ByteArrayOutputStream())));
            assertEquals(0, cli.run(new String[] { "export-report-dir", xmlFile.toString(), reportDir.toString() }, stream(reportDirOut),
                    stream(new ByteArrayOutputStream())));

            assertTrue(text(validateOut).contains("issues="));
            assertEquals("", text(validateErr));
            assertTrue(text(mermaidOut).contains("gantt"));
            assertTrue(text(markdownOut).contains("mikuproject開発"));
            assertTrue(text(dailySvgOut).contains("<svg"));
            assertTrue(text(weeklySvgOut).contains("weekly timeline"));
            assertTrue(text(monthlyZipOut).contains("entries"));
            assertTrue(text(bundleZipOut).contains("entries"));
            assertTrue(text(reportDirOut).contains("entries"));
            assertTrue(Files.size(monthlyZipFile) > 0);
            assertTrue(Files.size(bundleZipFile) > 0);
            assertTrue(Files.exists(reportDir.resolve("wbs.md")));
            assertTrue(Files.exists(reportDir.resolve("mermaid.mmd")));
            assertTrue(Files.exists(reportDir.resolve("daily.svg")));
            assertTrue(Files.exists(reportDir.resolve("weekly.svg")));
            assertTrue(Files.exists(reportDir.resolve("wbs.xlsx")));
            assertTrue(Files.exists(reportDir.resolve("monthly-calendar")));
        } finally {
            Files.deleteIfExists(xmlFile);
            Files.deleteIfExists(monthlyZipFile);
            Files.deleteIfExists(bundleZipFile);
            deleteTree(reportDir);
        }
    }

    @Test
    public void validatesXmlBatchAndExportsReportDirBatch() throws IOException {
        Path minimalXmlFile = Files.createTempFile("mikuproject-cli-batch-minimal", ".xml");
        Path dependencyXmlFile = Files.createTempFile("mikuproject-cli-batch-dependency", ".xml");
        Path outputRoot = Files.createTempDirectory("mikuproject-cli-batch-report");
        Files.write(minimalXmlFile, readVendorTestdata("minimal.xml").getBytes(StandardCharsets.UTF_8));
        Files.write(dependencyXmlFile, readVendorTestdata("dependency.xml").getBytes(StandardCharsets.UTF_8));
        try {
            MikuprojectCli cli = new MikuprojectCli();
            ByteArrayOutputStream validateOut = new ByteArrayOutputStream();
            ByteArrayOutputStream batchOut = new ByteArrayOutputStream();

            assertEquals(0, cli.run(new String[] { "validate-xml-batch", minimalXmlFile.toString(), dependencyXmlFile.toString() },
                    stream(validateOut), stream(new ByteArrayOutputStream())));
            assertEquals(0, cli.run(new String[] { "export-report-dir-batch", outputRoot.toString(), minimalXmlFile.toString(), "minimal",
                    dependencyXmlFile.toString(), "dependency", "--", "1", "2", "business", "business", "2026-03-20", "uid" }, stream(batchOut),
                    stream(new ByteArrayOutputStream())));

            assertTrue(text(validateOut).contains("issues="));
            assertTrue(text(validateOut).contains("errors="));
            assertTrue(text(batchOut).contains("minimal"));
            assertTrue(text(batchOut).contains("dependency"));
            assertTrue(Files.exists(outputRoot.resolve("minimal").resolve("wbs.md")));
            assertTrue(Files.exists(outputRoot.resolve("dependency").resolve("daily.svg")));
            assertTrue(Files.readString(outputRoot.resolve("dependency").resolve("daily.svg"), StandardCharsets.UTF_8).contains(">1</text>"));
        } finally {
            Files.deleteIfExists(minimalXmlFile);
            Files.deleteIfExists(dependencyXmlFile);
            deleteTree(outputRoot);
        }
    }

    @Test
    public void exportsWorkbookJsonBatch() throws IOException {
        Path minimalXmlFile = Files.createTempFile("mikuproject-cli-batch-workbook-minimal", ".xml");
        Path hierarchyXmlFile = Files.createTempFile("mikuproject-cli-batch-workbook-hierarchy", ".xml");
        Path outputRoot = Files.createTempDirectory("mikuproject-cli-batch-workbook");
        Files.write(minimalXmlFile, readVendorTestdata("minimal.xml").getBytes(StandardCharsets.UTF_8));
        Files.write(hierarchyXmlFile, readVendorTestdata("hierarchy.xml").getBytes(StandardCharsets.UTF_8));
        try {
            MikuprojectCli cli = new MikuprojectCli();
            ByteArrayOutputStream batchOut = new ByteArrayOutputStream();

            assertEquals(0, cli.run(
                    new String[] { "export-workbook-json-batch", outputRoot.toString(), minimalXmlFile.toString(), "minimal", hierarchyXmlFile.toString(),
                            "hierarchy" },
                    stream(batchOut), stream(new ByteArrayOutputStream())));

            assertTrue(text(batchOut).contains("minimal.json"));
            assertTrue(text(batchOut).contains("hierarchy.json"));
            assertTrue(Files.readString(outputRoot.resolve("minimal.json"), StandardCharsets.UTF_8).contains("\"format\":\"mikuproject_workbook_json\""));
            assertTrue(Files.readString(outputRoot.resolve("hierarchy.json"), StandardCharsets.UTF_8).contains("\"Tasks\""));
        } finally {
            Files.deleteIfExists(minimalXmlFile);
            Files.deleteIfExists(hierarchyXmlFile);
            deleteTree(outputRoot);
        }
    }

    @Test
    public void exportsXlsxBatch() throws IOException {
        Path minimalXmlFile = Files.createTempFile("mikuproject-cli-batch-xlsx-minimal", ".xml");
        Path hierarchyXmlFile = Files.createTempFile("mikuproject-cli-batch-xlsx-hierarchy", ".xml");
        Path outputRoot = Files.createTempDirectory("mikuproject-cli-batch-xlsx");
        Files.write(minimalXmlFile, readVendorTestdata("minimal.xml").getBytes(StandardCharsets.UTF_8));
        Files.write(hierarchyXmlFile, readVendorTestdata("hierarchy.xml").getBytes(StandardCharsets.UTF_8));
        try {
            MikuprojectCli cli = new MikuprojectCli();
            ByteArrayOutputStream batchOut = new ByteArrayOutputStream();

            assertEquals(0, cli.run(
                    new String[] { "export-xlsx-batch", outputRoot.toString(), minimalXmlFile.toString(), "minimal", hierarchyXmlFile.toString(),
                            "hierarchy" },
                    stream(batchOut), stream(new ByteArrayOutputStream())));

            assertTrue(text(batchOut).contains("minimal.xlsxbin"));
            assertTrue(text(batchOut).contains("hierarchy.xlsxbin"));
            assertTrue(Files.size(outputRoot.resolve("minimal.xlsxbin")) > 0);
            assertTrue(Files.size(outputRoot.resolve("hierarchy.xlsxbin")) > 0);
        } finally {
            Files.deleteIfExists(minimalXmlFile);
            Files.deleteIfExists(hierarchyXmlFile);
            deleteTree(outputRoot);
        }
    }

    @Test
    public void exportsProjectOverviewViewBatch() throws IOException {
        Path minimalXmlFile = Files.createTempFile("mikuproject-cli-batch-overview-minimal", ".xml");
        Path hierarchyXmlFile = Files.createTempFile("mikuproject-cli-batch-overview-hierarchy", ".xml");
        Path outputRoot = Files.createTempDirectory("mikuproject-cli-batch-overview");
        Files.write(minimalXmlFile, readVendorTestdata("minimal.xml").getBytes(StandardCharsets.UTF_8));
        Files.write(hierarchyXmlFile, readVendorTestdata("hierarchy.xml").getBytes(StandardCharsets.UTF_8));
        try {
            MikuprojectCli cli = new MikuprojectCli();
            ByteArrayOutputStream batchOut = new ByteArrayOutputStream();

            assertEquals(0, cli.run(
                    new String[] { "export-project-overview-view-batch", outputRoot.toString(), minimalXmlFile.toString(), "minimal",
                            hierarchyXmlFile.toString(), "hierarchy" },
                    stream(batchOut), stream(new ByteArrayOutputStream())));

            assertTrue(text(batchOut).contains("minimal.json"));
            assertTrue(text(batchOut).contains("hierarchy.json"));
            assertTrue(Files.readString(outputRoot.resolve("minimal.json"), StandardCharsets.UTF_8).contains("\"view_type\":\"project_overview_view\""));
            assertTrue(Files.readString(outputRoot.resolve("hierarchy.json"), StandardCharsets.UTF_8).contains("Hierarchy Project"));
        } finally {
            Files.deleteIfExists(minimalXmlFile);
            Files.deleteIfExists(hierarchyXmlFile);
            deleteTree(outputRoot);
        }
    }

    @Test
    public void exportsPhaseDetailViewBatch() throws IOException {
        Path hierarchyXmlFile = Files.createTempFile("mikuproject-cli-batch-phase-hierarchy", ".xml");
        Path outputRoot = Files.createTempDirectory("mikuproject-cli-batch-phase");
        Files.write(hierarchyXmlFile, readVendorTestdata("hierarchy.xml").getBytes(StandardCharsets.UTF_8));
        try {
            MikuprojectCli cli = new MikuprojectCli();
            ByteArrayOutputStream batchOut = new ByteArrayOutputStream();

            assertEquals(0, cli.run(
                    new String[] { "export-phase-detail-view-batch", outputRoot.toString(), hierarchyXmlFile.toString(), "default", hierarchyXmlFile.toString(),
                            "scoped", "--", "1", "phase", "1", "1" },
                    stream(batchOut), stream(new ByteArrayOutputStream())));

            assertTrue(text(batchOut).contains("default.json"));
            assertTrue(text(batchOut).contains("scoped.json"));
            assertTrue(Files.readString(outputRoot.resolve("default.json"), StandardCharsets.UTF_8).contains("\"view_type\":\"phase_detail_view\""));
            assertTrue(Files.readString(outputRoot.resolve("scoped.json"), StandardCharsets.UTF_8).contains("Child A"));
        } finally {
            Files.deleteIfExists(hierarchyXmlFile);
            deleteTree(outputRoot);
        }
    }

    @Test
    public void exportsWorkbookJsonAndAiViews() throws IOException {
        Path xmlFile = Files.createTempFile("mikuproject-cli-export-json", ".xml");
        Files.write(xmlFile, new MsProjectSamples().buildSampleXml().getBytes(StandardCharsets.UTF_8));
        try {
            MikuprojectCli cli = new MikuprojectCli();
            ByteArrayOutputStream workbookOut = new ByteArrayOutputStream();
            ByteArrayOutputStream overviewOut = new ByteArrayOutputStream();
            ByteArrayOutputStream phaseOut = new ByteArrayOutputStream();
            ByteArrayOutputStream taskEditOut = new ByteArrayOutputStream();
            ByteArrayOutputStream draftRequestOut = new ByteArrayOutputStream();

            assertEquals(0, cli.run(new String[] { "export-workbook-json", xmlFile.toString() }, stream(workbookOut), stream(new ByteArrayOutputStream())));
            assertEquals(0, cli.run(new String[] { "export-project-overview-view", xmlFile.toString() }, stream(overviewOut),
                    stream(new ByteArrayOutputStream())));
            assertEquals(0, cli.run(new String[] { "export-phase-detail-view", xmlFile.toString() }, stream(phaseOut), stream(new ByteArrayOutputStream())));
            assertEquals(0, cli.run(new String[] { "export-task-edit-view", xmlFile.toString(), "draft-120" }, stream(taskEditOut),
                    stream(new ByteArrayOutputStream())));
            assertEquals(0, cli.run(new String[] { "export-project-draft-request", "Draft Request", "2026-04-01", "Goal", "3", "Plan,Build", "Kickoff" },
                    stream(draftRequestOut), stream(new ByteArrayOutputStream())));

            assertTrue(text(workbookOut).contains("\"format\":\"mikuproject_workbook_json\""));
            assertTrue(text(overviewOut).contains("\"view_type\":\"project_overview_view\""));
            assertTrue(text(phaseOut).contains("\"view_type\":\"phase_detail_view\""));
            assertTrue(text(taskEditOut).contains("\"view_type\":\"task_edit_view\""));
            assertTrue(text(draftRequestOut).contains("\"view_type\":\"project_draft_request\""));
            assertTrue(text(draftRequestOut).contains("\"must_have_phases\":[\"Plan\",\"Build\"]"));
        } finally {
            Files.deleteIfExists(xmlFile);
        }
    }

    @Test
    public void appliesWbsOptionArgumentsToMarkdownAndReportDir() throws IOException {
        Path xmlFile = Files.createTempFile("mikuproject-cli-wbs-options", ".xml");
        Path reportDir = Files.createTempDirectory("mikuproject-cli-wbs-report-dir");
        Files.write(xmlFile, new MsProjectSamples().buildSampleXml().getBytes(StandardCharsets.UTF_8));
        try {
            MikuprojectCli cli = new MikuprojectCli();
            ByteArrayOutputStream markdownOut = new ByteArrayOutputStream();
            ByteArrayOutputStream reportDirOut = new ByteArrayOutputStream();

            assertEquals(0, cli.run(
                    new String[] { "export-wbs-markdown", xmlFile.toString(), "1", "2", "business", "business", "2026-04-29,2026-04-30" },
                    stream(markdownOut), stream(new ByteArrayOutputStream())));
            assertEquals(0, cli.run(
                    new String[] { "export-report-dir", xmlFile.toString(), reportDir.toString(), "1", "2", "business", "business",
                            "2026-04-29,2026-04-30", "uid" },
                    stream(reportDirOut), stream(new ByteArrayOutputStream())));

            assertTrue(text(markdownOut).contains("| 前日数 | 1 |"));
            assertTrue(text(markdownOut).contains("| 後日数 | 2 |"));
            assertTrue(text(markdownOut).contains("| 表示 | 営業日 |"));
            assertTrue(text(markdownOut).contains("| 進捗 | 営業日 |"));
            String reportMarkdown = Files.readString(reportDir.resolve("wbs.md"), StandardCharsets.UTF_8);
            String reportDailySvg = Files.readString(reportDir.resolve("daily.svg"), StandardCharsets.UTF_8);
            assertTrue(text(reportDirOut).contains("entries"));
            assertTrue(reportMarkdown.contains("| 前日数 | 1 |"));
            assertTrue(reportMarkdown.contains("| 後日数 | 2 |"));
            assertTrue(reportMarkdown.contains("| 表示 | 営業日 |"));
            assertTrue(reportMarkdown.contains("| 進捗 | 営業日 |"));
            assertTrue(reportDailySvg.contains(">draft-110</text>"));
        } finally {
            Files.deleteIfExists(xmlFile);
            deleteTree(reportDir);
        }
    }

    @Test
    public void appliesSvgOptionArgumentsToSvgExports() throws IOException {
        Path dependencyXmlFile = Files.createTempFile("mikuproject-cli-svg-options", ".xml");
        Path monthlyZipFile = Files.createTempFile("mikuproject-cli-svg-monthly", ".zip");
        Files.write(dependencyXmlFile, readVendorTestdata("dependency.xml").getBytes(StandardCharsets.UTF_8));
        try {
            MikuprojectCli cli = new MikuprojectCli();
            ByteArrayOutputStream dailyOut = new ByteArrayOutputStream();
            ByteArrayOutputStream weeklyOut = new ByteArrayOutputStream();
            ByteArrayOutputStream monthlyOut = new ByteArrayOutputStream();

            assertEquals(0, cli.run(new String[] { "export-daily-svg", dependencyXmlFile.toString(), "uid" }, stream(dailyOut),
                    stream(new ByteArrayOutputStream())));
            assertEquals(0, cli.run(new String[] { "export-weekly-svg", dependencyXmlFile.toString(), "uid" }, stream(weeklyOut),
                    stream(new ByteArrayOutputStream())));
            assertEquals(0, cli.run(new String[] { "export-monthly-svg-zip", dependencyXmlFile.toString(), monthlyZipFile.toString(),
                    "2026-03-20,2026-03-21", "uid" }, stream(monthlyOut), stream(new ByteArrayOutputStream())));

            assertTrue(text(dailyOut).contains(">1</text>"));
            assertTrue(text(weeklyOut).contains(">2</text>"));
            assertTrue(text(monthlyOut).contains("entries"));
            assertTrue(zipText(monthlyZipFile).contains("holidays 2"));
            assertTrue(zipText(monthlyZipFile).contains(">1</text>"));
        } finally {
            Files.deleteIfExists(dependencyXmlFile);
            Files.deleteIfExists(monthlyZipFile);
        }
    }

    @Test
    public void importsWorkbookJsonAndAppliesPatchJson() throws IOException {
        Path baseXmlFile = Files.createTempFile("mikuproject-cli-base", ".xml");
        Path workbookJsonFile = Files.createTempFile("mikuproject-cli-workbook", ".json");
        Path importedXmlFile = Files.createTempFile("mikuproject-cli-imported", ".xml");
        Path mergedXmlFile = Files.createTempFile("mikuproject-cli-merged", ".xml");
        Path patchJsonFile = Files.createTempFile("mikuproject-cli-patch", ".json");
        Path patchedXmlFile = Files.createTempFile("mikuproject-cli-patched", ".xml");
        Files.write(baseXmlFile, new MsProjectSamples().buildSampleXml().getBytes(StandardCharsets.UTF_8));
        Files.write(workbookJsonFile, workbookJsonText());
        Files.write(patchJsonFile, patchJsonText());
        try {
            MikuprojectCli cli = new MikuprojectCli();
            ByteArrayOutputStream workbookValidateOut = new ByteArrayOutputStream();
            ByteArrayOutputStream importOut = new ByteArrayOutputStream();
            ByteArrayOutputStream mergeOut = new ByteArrayOutputStream();
            ByteArrayOutputStream patchValidateOut = new ByteArrayOutputStream();
            ByteArrayOutputStream patchOut = new ByteArrayOutputStream();

            assertEquals(0, cli.run(new String[] { "validate-workbook-json", workbookJsonFile.toString() }, stream(workbookValidateOut),
                    stream(new ByteArrayOutputStream())));
            assertEquals(0, cli.run(new String[] { "import-workbook-json", workbookJsonFile.toString(), importedXmlFile.toString() }, stream(importOut),
                    stream(new ByteArrayOutputStream())));
            assertEquals(0, cli.run(new String[] { "merge-workbook-json", baseXmlFile.toString(), workbookJsonFile.toString(), mergedXmlFile.toString() },
                    stream(mergeOut), stream(new ByteArrayOutputStream())));
            assertEquals(0, cli.run(new String[] { "validate-patch-json", patchJsonFile.toString() }, stream(patchValidateOut),
                    stream(new ByteArrayOutputStream())));
            assertEquals(0, cli.run(new String[] { "apply-patch-json", baseXmlFile.toString(), patchJsonFile.toString(), patchedXmlFile.toString() },
                    stream(patchOut), stream(new ByteArrayOutputStream())));

            assertTrue(text(workbookValidateOut).contains("warnings="));
            assertTrue(text(importOut).contains("wrote"));
            assertTrue(text(mergeOut).contains("changes="));
            assertTrue(text(patchValidateOut).contains("operations="));
            assertTrue(text(patchOut).contains("changes="));
            assertTrue(Files.readAllBytes(importedXmlFile).length > 0);
            assertTrue(Files.readString(mergedXmlFile, StandardCharsets.UTF_8).contains("Workbook Imported Project"));
            assertTrue(Files.readString(mergedXmlFile, StandardCharsets.UTF_8).contains("<Type>2</Type>"));
            assertTrue(Files.readString(mergedXmlFile, StandardCharsets.UTF_8).contains("<Priority>500</Priority>"));
            assertTrue(Files.readString(mergedXmlFile, StandardCharsets.UTF_8).contains("<ConstraintType>4</ConstraintType>"));
            assertTrue(Files.readString(mergedXmlFile, StandardCharsets.UTF_8).contains("<ConstraintDate>2026-03-17T09:00:00</ConstraintDate>"));
            assertTrue(Files.readString(mergedXmlFile, StandardCharsets.UTF_8).contains("<Deadline>2026-03-18T18:00:00</Deadline>"));
            assertTrue(Files.readString(mergedXmlFile, StandardCharsets.UTF_8).contains("<Type>1</Type>"));
            assertTrue(Files.readString(mergedXmlFile, StandardCharsets.UTF_8).contains("<Initials>MU</Initials>"));
            assertTrue(Files.readString(mergedXmlFile, StandardCharsets.UTF_8).contains("<StandardRate>1200/h</StandardRate>"));
            assertTrue(Files.readString(mergedXmlFile, StandardCharsets.UTF_8).contains("<OvertimeRate>1800/h</OvertimeRate>"));
            assertTrue(Files.readString(mergedXmlFile, StandardCharsets.UTF_8).contains("<CostPerUse>500.0</CostPerUse>"));
            assertTrue(Files.readString(mergedXmlFile, StandardCharsets.UTF_8).contains("<Work>PT8H0M0S</Work>"));
            assertTrue(Files.readString(mergedXmlFile, StandardCharsets.UTF_8).contains("<ActualWork>PT4H0M0S</ActualWork>"));
            assertTrue(Files.readString(mergedXmlFile, StandardCharsets.UTF_8).contains("<RemainingWork>PT4H0M0S</RemainingWork>"));
            assertTrue(Files.readString(mergedXmlFile, StandardCharsets.UTF_8).contains("<Cost>1000.0</Cost>"));
            assertTrue(Files.readString(mergedXmlFile, StandardCharsets.UTF_8).contains("<ActualCost>500.0</ActualCost>"));
            assertTrue(Files.readString(mergedXmlFile, StandardCharsets.UTF_8).contains("<RemainingCost>500.0</RemainingCost>"));
            assertTrue(Files.readString(mergedXmlFile, StandardCharsets.UTF_8).contains("<PercentWorkComplete>50</PercentWorkComplete>"));
            assertTrue(Files.readString(mergedXmlFile, StandardCharsets.UTF_8).contains("<WorkGroup>0</WorkGroup>"));
            assertTrue(Files.readString(mergedXmlFile, StandardCharsets.UTF_8).contains("<StandardRateFormat>2</StandardRateFormat>"));
            assertTrue(Files.readString(mergedXmlFile, StandardCharsets.UTF_8).contains("<OvertimeRateFormat>2</OvertimeRateFormat>"));
            assertTrue(Files.readString(mergedXmlFile, StandardCharsets.UTF_8).contains("<StartVariance>PT0H30M0S</StartVariance>"));
            assertTrue(Files.readString(mergedXmlFile, StandardCharsets.UTF_8).contains("<FinishVariance>PT1H0M0S</FinishVariance>"));
            assertTrue(Files.readString(mergedXmlFile, StandardCharsets.UTF_8).contains("<Delay>PT0H15M0S</Delay>"));
            assertTrue(Files.readString(mergedXmlFile, StandardCharsets.UTF_8).contains("<Milestone>1</Milestone>"));
            assertTrue(Files.readString(mergedXmlFile, StandardCharsets.UTF_8).contains("<WorkContour>1</WorkContour>"));
            assertTrue(Files.readString(mergedXmlFile, StandardCharsets.UTF_8).contains("<ActualWork>PT4H0M0S</ActualWork>"));
            assertTrue(Files.readString(mergedXmlFile, StandardCharsets.UTF_8).contains("<RemainingWork>PT4H0M0S</RemainingWork>"));
            assertTrue(Files.readString(mergedXmlFile, StandardCharsets.UTF_8).contains("<ActualCost>300.0</ActualCost>"));
            assertTrue(Files.readString(mergedXmlFile, StandardCharsets.UTF_8).contains("<RemainingCost>200.0</RemainingCost>"));
            assertTrue(Files.readString(mergedXmlFile, StandardCharsets.UTF_8).contains("<OvertimeWork>PT1H0M0S</OvertimeWork>"));
            assertTrue(Files.readString(mergedXmlFile, StandardCharsets.UTF_8).contains("<ActualOvertimeWork>PT0H30M0S</ActualOvertimeWork>"));
            assertTrue(Files.readString(patchedXmlFile, StandardCharsets.UTF_8).contains("Patched Project"));
        } finally {
            Files.deleteIfExists(baseXmlFile);
            Files.deleteIfExists(workbookJsonFile);
            Files.deleteIfExists(importedXmlFile);
            Files.deleteIfExists(mergedXmlFile);
            Files.deleteIfExists(patchJsonFile);
            Files.deleteIfExists(patchedXmlFile);
        }
    }

    @Test
    public void importsAiJsonAndExternalFormats() throws IOException {
        Path baseXmlFile = Files.createTempFile("mikuproject-cli-ai-base", ".xml");
        Path aiJsonFile = Files.createTempFile("mikuproject-cli-ai", ".txt");
        Path workbookJsonFile = Files.createTempFile("mikuproject-cli-external-workbook", ".json");
        Path patchJsonFile = Files.createTempFile("mikuproject-cli-external-patch", ".json");
        Path xlsxBytesFile = Files.createTempFile("mikuproject-cli-external-xlsx", ".xlsxbin");
        Path aiOutputXmlFile = Files.createTempFile("mikuproject-cli-ai-out", ".xml");
        Path externalWorkbookXmlFile = Files.createTempFile("mikuproject-cli-ext-workbook", ".xml");
        Path externalPatchXmlFile = Files.createTempFile("mikuproject-cli-ext-patch", ".xml");
        Path externalXlsxXmlFile = Files.createTempFile("mikuproject-cli-ext-xlsx", ".xml");
        Files.write(baseXmlFile, new MsProjectSamples().buildSampleXml().getBytes(StandardCharsets.UTF_8));
        Files.write(aiJsonFile, aiJsonText());
        Files.write(workbookJsonFile, workbookJsonText());
        Files.write(patchJsonFile, patchJsonText());
        try {
            MikuprojectCli cli = new MikuprojectCli();
            ByteArrayOutputStream aiOut = new ByteArrayOutputStream();
            ByteArrayOutputStream workbookOut = new ByteArrayOutputStream();
            ByteArrayOutputStream patchOut = new ByteArrayOutputStream();
            ByteArrayOutputStream xlsxOut = new ByteArrayOutputStream();

            assertEquals(0, cli.run(new String[] { "export-xlsx", baseXmlFile.toString(), xlsxBytesFile.toString() }, stream(new ByteArrayOutputStream()),
                    stream(new ByteArrayOutputStream())));

            assertEquals(0, cli.run(new String[] { "import-ai-json", aiJsonFile.toString(), aiOutputXmlFile.toString() }, stream(aiOut),
                    stream(new ByteArrayOutputStream())));
            assertEquals(0, cli.run(new String[] { "import-external", "workbook_json", "merge", workbookJsonFile.toString(),
                    externalWorkbookXmlFile.toString(), baseXmlFile.toString() }, stream(workbookOut), stream(new ByteArrayOutputStream())));
            assertEquals(0, cli.run(new String[] { "import-external", "patch_json", "patch", patchJsonFile.toString(),
                    externalPatchXmlFile.toString(), baseXmlFile.toString() }, stream(patchOut), stream(new ByteArrayOutputStream())));
            assertEquals(0, cli.run(new String[] { "import-external", "xlsx", "replace", xlsxBytesFile.toString(),
                    externalXlsxXmlFile.toString() }, stream(xlsxOut), stream(new ByteArrayOutputStream())));

            assertTrue(text(aiOut).contains("kind=project_draft_view"));
            assertTrue(text(workbookOut).contains("kind=workbook_json"));
            assertTrue(text(patchOut).contains("kind=patch_json"));
            assertTrue(text(xlsxOut).contains("kind=xlsx"));
            assertTrue(Files.readString(aiOutputXmlFile, StandardCharsets.UTF_8).contains("AI JSON Imported Project"));
            assertTrue(Files.readString(externalWorkbookXmlFile, StandardCharsets.UTF_8).contains("Workbook Imported Project"));
            assertTrue(Files.readString(externalWorkbookXmlFile, StandardCharsets.UTF_8).contains("<Type>2</Type>"));
            assertTrue(Files.readString(externalWorkbookXmlFile, StandardCharsets.UTF_8).contains("<Priority>500</Priority>"));
            assertTrue(Files.readString(externalWorkbookXmlFile, StandardCharsets.UTF_8).contains("<ConstraintType>4</ConstraintType>"));
            assertTrue(Files.readString(externalWorkbookXmlFile, StandardCharsets.UTF_8).contains("<ConstraintDate>2026-03-17T09:00:00</ConstraintDate>"));
            assertTrue(Files.readString(externalWorkbookXmlFile, StandardCharsets.UTF_8).contains("<Deadline>2026-03-18T18:00:00</Deadline>"));
            assertTrue(Files.readString(externalWorkbookXmlFile, StandardCharsets.UTF_8).contains("<Type>1</Type>"));
            assertTrue(Files.readString(externalWorkbookXmlFile, StandardCharsets.UTF_8).contains("<Initials>MU</Initials>"));
            assertTrue(Files.readString(externalWorkbookXmlFile, StandardCharsets.UTF_8).contains("<StandardRate>1200/h</StandardRate>"));
            assertTrue(Files.readString(externalWorkbookXmlFile, StandardCharsets.UTF_8).contains("<Work>PT8H0M0S</Work>"));
            assertTrue(Files.readString(externalWorkbookXmlFile, StandardCharsets.UTF_8).contains("<Cost>1000.0</Cost>"));
            assertTrue(Files.readString(externalWorkbookXmlFile, StandardCharsets.UTF_8).contains("<PercentWorkComplete>50</PercentWorkComplete>"));
            assertTrue(Files.readString(externalWorkbookXmlFile, StandardCharsets.UTF_8).contains("<WorkGroup>0</WorkGroup>"));
            assertTrue(Files.readString(externalWorkbookXmlFile, StandardCharsets.UTF_8).contains("<StandardRateFormat>2</StandardRateFormat>"));
            assertTrue(Files.readString(externalWorkbookXmlFile, StandardCharsets.UTF_8).contains("<OvertimeRateFormat>2</OvertimeRateFormat>"));
            assertTrue(Files.readString(externalWorkbookXmlFile, StandardCharsets.UTF_8).contains("<StartVariance>PT0H30M0S</StartVariance>"));
            assertTrue(Files.readString(externalWorkbookXmlFile, StandardCharsets.UTF_8).contains("<FinishVariance>PT1H0M0S</FinishVariance>"));
            assertTrue(Files.readString(externalWorkbookXmlFile, StandardCharsets.UTF_8).contains("<Delay>PT0H15M0S</Delay>"));
            assertTrue(Files.readString(externalWorkbookXmlFile, StandardCharsets.UTF_8).contains("<Milestone>1</Milestone>"));
            assertTrue(Files.readString(externalWorkbookXmlFile, StandardCharsets.UTF_8).contains("<WorkContour>1</WorkContour>"));
            assertTrue(Files.readString(externalWorkbookXmlFile, StandardCharsets.UTF_8).contains("<ActualCost>300.0</ActualCost>"));
            assertTrue(Files.readString(externalPatchXmlFile, StandardCharsets.UTF_8).contains("Patched Project"));
            assertTrue(Files.readString(externalXlsxXmlFile, StandardCharsets.UTF_8).contains("mikuproject開発"));
        } finally {
            Files.deleteIfExists(baseXmlFile);
            Files.deleteIfExists(aiJsonFile);
            Files.deleteIfExists(workbookJsonFile);
            Files.deleteIfExists(patchJsonFile);
            Files.deleteIfExists(xlsxBytesFile);
            Files.deleteIfExists(aiOutputXmlFile);
            Files.deleteIfExists(externalWorkbookXmlFile);
            Files.deleteIfExists(externalPatchXmlFile);
            Files.deleteIfExists(externalXlsxXmlFile);
        }
    }

    @Test
    public void exportsAiJsonSpecAndDetectsAiJsonKind() throws IOException {
        Path aiJsonFile = Files.createTempFile("mikuproject-cli-ai-kind", ".txt");
        Files.write(aiJsonFile, aiJsonText());
        try {
            MikuprojectCli cli = new MikuprojectCli();
            ByteArrayOutputStream specOut = new ByteArrayOutputStream();
            ByteArrayOutputStream kindOut = new ByteArrayOutputStream();

            assertEquals(0, cli.run(new String[] { "export-ai-json-spec" }, stream(specOut), stream(new ByteArrayOutputStream())));
            assertEquals(0, cli.run(new String[] { "detect-ai-json-kind", aiJsonFile.toString() }, stream(kindOut), stream(new ByteArrayOutputStream())));

            assertTrue(text(specOut).contains("project_draft_view"));
            assertTrue(text(kindOut).contains("kind=project_draft_view"));
        } finally {
            Files.deleteIfExists(aiJsonFile);
        }
    }

    @Test
    public void exportsHierarchyAndDependencyFixturesThroughCli() throws IOException {
        Path hierarchyXmlFile = Files.createTempFile("mikuproject-cli-hierarchy", ".xml");
        Path dependencyXmlFile = Files.createTempFile("mikuproject-cli-dependency", ".xml");
        Path importedXmlFile = Files.createTempFile("mikuproject-cli-imported-xml", ".xml");
        Files.write(hierarchyXmlFile, readVendorTestdata("hierarchy.xml").getBytes(StandardCharsets.UTF_8));
        Files.write(dependencyXmlFile, readVendorTestdata("dependency.xml").getBytes(StandardCharsets.UTF_8));
        try {
            MikuprojectCli cli = new MikuprojectCli();
            ByteArrayOutputStream overviewOut = new ByteArrayOutputStream();
            ByteArrayOutputStream phaseOut = new ByteArrayOutputStream();
            ByteArrayOutputStream taskEditOut = new ByteArrayOutputStream();
            ByteArrayOutputStream dailySvgOut = new ByteArrayOutputStream();
            ByteArrayOutputStream importExternalOut = new ByteArrayOutputStream();

            assertEquals(0, cli.run(new String[] { "export-project-overview-view", hierarchyXmlFile.toString() }, stream(overviewOut),
                    stream(new ByteArrayOutputStream())));
            assertEquals(0, cli.run(new String[] { "export-phase-detail-view", hierarchyXmlFile.toString() }, stream(phaseOut),
                    stream(new ByteArrayOutputStream())));
            assertEquals(0, cli.run(new String[] { "export-task-edit-view", dependencyXmlFile.toString(), "2" }, stream(taskEditOut),
                    stream(new ByteArrayOutputStream())));
            assertEquals(0, cli.run(new String[] { "export-daily-svg", dependencyXmlFile.toString() }, stream(dailySvgOut),
                    stream(new ByteArrayOutputStream())));
            assertEquals(0, cli.run(new String[] { "import-external", "ms_project_xml", "replace", dependencyXmlFile.toString(),
                    importedXmlFile.toString() }, stream(importExternalOut), stream(new ByteArrayOutputStream())));

            assertTrue(text(overviewOut).contains("\"view_type\":\"project_overview_view\""));
            assertTrue(text(overviewOut).contains("Hierarchy Project"));
            assertTrue(text(phaseOut).contains("\"view_type\":\"phase_detail_view\""));
            assertTrue(text(phaseOut).contains("Child A"));
            assertTrue(text(taskEditOut).contains("\"view_type\":\"task_edit_view\""));
            assertTrue(text(taskEditOut).contains("Prepare"));
            assertTrue(text(taskEditOut).contains("Execute"));
            assertTrue(text(dailySvgOut).contains("class=\"dependencyPath\""));
            assertTrue(text(importExternalOut).contains("kind=ms_project_xml"));
            assertTrue(Files.readString(importedXmlFile, StandardCharsets.UTF_8).contains("Dependency Project"));
        } finally {
            Files.deleteIfExists(hierarchyXmlFile);
            Files.deleteIfExists(dependencyXmlFile);
            Files.deleteIfExists(importedXmlFile);
        }
    }

    @Test
    public void exportsAndImportsXlsxWorkbookBytes() throws IOException {
        Path baseXmlFile = Files.createTempFile("mikuproject-cli-xlsx-base", ".xml");
        Path workbookBytesFile = Files.createTempFile("mikuproject-cli-xlsx", ".xlsxbin");
        Path importedXmlFile = Files.createTempFile("mikuproject-cli-xlsx-imported", ".xml");
        Path mergedXmlFile = Files.createTempFile("mikuproject-cli-xlsx-merged", ".xml");
        Files.write(baseXmlFile, new MsProjectSamples().buildSampleXml().getBytes(StandardCharsets.UTF_8));
        try {
            MikuprojectCli cli = new MikuprojectCli();
            ByteArrayOutputStream exportOut = new ByteArrayOutputStream();
            ByteArrayOutputStream importOut = new ByteArrayOutputStream();
            ByteArrayOutputStream mergeOut = new ByteArrayOutputStream();

            assertEquals(0, cli.run(new String[] { "export-xlsx", baseXmlFile.toString(), workbookBytesFile.toString() }, stream(exportOut),
                    stream(new ByteArrayOutputStream())));
            writeExtendedWorkbookValues(workbookBytesFile);
            assertEquals(0, cli.run(new String[] { "import-xlsx", workbookBytesFile.toString(), importedXmlFile.toString() }, stream(importOut),
                    stream(new ByteArrayOutputStream())));
            assertEquals(0, cli.run(new String[] { "merge-xlsx", baseXmlFile.toString(), workbookBytesFile.toString(), mergedXmlFile.toString() },
                    stream(mergeOut), stream(new ByteArrayOutputStream())));

            assertTrue(text(exportOut).contains("bytes"));
            assertTrue(text(importOut).contains("wrote"));
            assertTrue(text(mergeOut).contains("wrote"));
            assertTrue(Files.size(workbookBytesFile) > 0);
            assertTrue(Files.readString(importedXmlFile, StandardCharsets.UTF_8).contains("mikuproject開発"));
            assertTrue(Files.readString(mergedXmlFile, StandardCharsets.UTF_8).contains("mikuproject開発"));
            assertTrue(Files.readString(mergedXmlFile, StandardCharsets.UTF_8).contains("<Type>2</Type>"));
            assertTrue(Files.readString(mergedXmlFile, StandardCharsets.UTF_8).contains("<Priority>500</Priority>"));
            assertTrue(Files.readString(mergedXmlFile, StandardCharsets.UTF_8).contains("<ConstraintType>4</ConstraintType>"));
            assertTrue(Files.readString(mergedXmlFile, StandardCharsets.UTF_8).contains("<ConstraintDate>2026-03-17T09:00:00</ConstraintDate>"));
            assertTrue(Files.readString(mergedXmlFile, StandardCharsets.UTF_8).contains("<Deadline>2026-03-18T18:00:00</Deadline>"));
            assertTrue(Files.readString(mergedXmlFile, StandardCharsets.UTF_8).contains("<Type>1</Type>"));
            assertTrue(Files.readString(mergedXmlFile, StandardCharsets.UTF_8).contains("<Initials>MU</Initials>"));
            assertTrue(Files.readString(mergedXmlFile, StandardCharsets.UTF_8).contains("<StandardRate>1200/h</StandardRate>"));
            assertTrue(Files.readString(mergedXmlFile, StandardCharsets.UTF_8).contains("<OvertimeRate>1800/h</OvertimeRate>"));
            assertTrue(Files.readString(mergedXmlFile, StandardCharsets.UTF_8).contains("<CostPerUse>500.0</CostPerUse>"));
            assertTrue(Files.readString(mergedXmlFile, StandardCharsets.UTF_8).contains("<Work>PT8H0M0S</Work>"));
            assertTrue(Files.readString(mergedXmlFile, StandardCharsets.UTF_8).contains("<ActualWork>PT4H0M0S</ActualWork>"));
            assertTrue(Files.readString(mergedXmlFile, StandardCharsets.UTF_8).contains("<RemainingWork>PT4H0M0S</RemainingWork>"));
            assertTrue(Files.readString(mergedXmlFile, StandardCharsets.UTF_8).contains("<Cost>1000.0</Cost>"));
            assertTrue(Files.readString(mergedXmlFile, StandardCharsets.UTF_8).contains("<ActualCost>500.0</ActualCost>"));
            assertTrue(Files.readString(mergedXmlFile, StandardCharsets.UTF_8).contains("<RemainingCost>500.0</RemainingCost>"));
            assertTrue(Files.readString(mergedXmlFile, StandardCharsets.UTF_8).contains("<PercentWorkComplete>50</PercentWorkComplete>"));
            assertTrue(Files.readString(mergedXmlFile, StandardCharsets.UTF_8).contains("<WorkGroup>0</WorkGroup>"));
            assertTrue(Files.readString(mergedXmlFile, StandardCharsets.UTF_8).contains("<StandardRateFormat>2</StandardRateFormat>"));
            assertTrue(Files.readString(mergedXmlFile, StandardCharsets.UTF_8).contains("<OvertimeRateFormat>2</OvertimeRateFormat>"));
            assertTrue(Files.readString(mergedXmlFile, StandardCharsets.UTF_8).contains("<StartVariance>PT0H30M0S</StartVariance>"));
            assertTrue(Files.readString(mergedXmlFile, StandardCharsets.UTF_8).contains("<FinishVariance>PT1H0M0S</FinishVariance>"));
            assertTrue(Files.readString(mergedXmlFile, StandardCharsets.UTF_8).contains("<Delay>PT0H15M0S</Delay>"));
            assertTrue(Files.readString(mergedXmlFile, StandardCharsets.UTF_8).contains("<Milestone>1</Milestone>"));
            assertTrue(Files.readString(mergedXmlFile, StandardCharsets.UTF_8).contains("<WorkContour>1</WorkContour>"));
            assertTrue(Files.readString(mergedXmlFile, StandardCharsets.UTF_8).contains("<ActualWork>PT4H0M0S</ActualWork>"));
            assertTrue(Files.readString(mergedXmlFile, StandardCharsets.UTF_8).contains("<RemainingWork>PT4H0M0S</RemainingWork>"));
            assertTrue(Files.readString(mergedXmlFile, StandardCharsets.UTF_8).contains("<ActualCost>300.0</ActualCost>"));
            assertTrue(Files.readString(mergedXmlFile, StandardCharsets.UTF_8).contains("<RemainingCost>200.0</RemainingCost>"));
            assertTrue(Files.readString(mergedXmlFile, StandardCharsets.UTF_8).contains("<OvertimeWork>PT1H0M0S</OvertimeWork>"));
            assertTrue(Files.readString(mergedXmlFile, StandardCharsets.UTF_8).contains("<ActualOvertimeWork>PT0H30M0S</ActualOvertimeWork>"));
        } finally {
            Files.deleteIfExists(baseXmlFile);
            Files.deleteIfExists(workbookBytesFile);
            Files.deleteIfExists(importedXmlFile);
            Files.deleteIfExists(mergedXmlFile);
        }
    }

    @Test
    public void validatesAndExportsWbsXlsxWorkbookBytes() throws IOException {
        Path baseXmlFile = Files.createTempFile("mikuproject-cli-wbs-xlsx-base", ".xml");
        Path workbookBytesFile = Files.createTempFile("mikuproject-cli-wbs-xlsx", ".xlsxbin");
        Files.write(baseXmlFile, new MsProjectSamples().buildSampleXml().getBytes(StandardCharsets.UTF_8));
        try {
            MikuprojectCli cli = new MikuprojectCli();
            ByteArrayOutputStream exportOut = new ByteArrayOutputStream();
            ByteArrayOutputStream validateOut = new ByteArrayOutputStream();

            assertEquals(0, cli.run(new String[] { "export-wbs-xlsx", baseXmlFile.toString(), workbookBytesFile.toString() }, stream(exportOut),
                    stream(new ByteArrayOutputStream())));
            assertEquals(0, cli.run(new String[] { "validate-xlsx", workbookBytesFile.toString() }, stream(validateOut), stream(new ByteArrayOutputStream())));

            assertTrue(text(exportOut).contains("bytes"));
            assertTrue(text(validateOut).contains("sheets="));
            assertTrue(Files.size(workbookBytesFile) > 0);
        } finally {
            Files.deleteIfExists(baseXmlFile);
            Files.deleteIfExists(workbookBytesFile);
        }
    }

    @Test
    public void returnsUsageErrorForMissingArgument() throws IOException {
        MikuprojectCli cli = new MikuprojectCli();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();

        int exitCode = cli.run(new String[] { "validate-xml" }, stream(out), stream(err));

        assertEquals(2, exitCode);
        assertTrue(text(err).contains("requires <input.xml>"));
    }

    private PrintStream stream(ByteArrayOutputStream out) {
        return new PrintStream(out, true);
    }

    private String text(ByteArrayOutputStream out) {
        return new String(out.toByteArray(), StandardCharsets.UTF_8);
    }

    private byte[] workbookJsonText() {
        WorkbookJsonDocument document = new WorkbookJsonDocument();
        List<Map<String, Object>> projectRows = document.ensureSheet("Project");
        Map<String, Object> nameRow = new LinkedHashMap<String, Object>();
        nameRow.put("Field", "Name");
        nameRow.put("Value", "Workbook Imported Project");
        projectRows.add(nameRow);
        List<Map<String, Object>> taskRows = document.ensureSheet("Tasks");
        Map<String, Object> taskRow = new LinkedHashMap<String, Object>();
        taskRow.put("UID", "draft-120");
        taskRow.put("ID", "3");
        taskRow.put("Name", "Imported Task");
        taskRow.put("OutlineLevel", Integer.valueOf(1));
        taskRow.put("OutlineNumber", "1");
        taskRow.put("WBS", "1");
        taskRow.put("Start", "2026-03-16");
        taskRow.put("Finish", "2026-03-17");
        taskRow.put("Duration", "P2D");
        taskRow.put("Type", Integer.valueOf(2));
        taskRow.put("Priority", Integer.valueOf(500));
        taskRow.put("ConstraintType", Integer.valueOf(4));
        taskRow.put("ConstraintDate", "2026-03-17 09:00:00");
        taskRow.put("Deadline", "2026-03-18 18:00:00");
        taskRows.add(taskRow);
        List<Map<String, Object>> resourceRows = document.ensureSheet("Resources");
        Map<String, Object> resourceRow = new LinkedHashMap<String, Object>();
        resourceRow.put("UID", "res-1");
        resourceRow.put("ID", "1");
        resourceRow.put("Name", "Mikuku");
        resourceRow.put("Type", Integer.valueOf(1));
        resourceRow.put("Initials", "MU");
        resourceRow.put("StandardRate", "1200/h");
        resourceRow.put("OvertimeRate", "1800/h");
        resourceRow.put("CostPerUse", Double.valueOf(500));
        resourceRow.put("Work", "PT8H0M0S");
        resourceRow.put("ActualWork", "PT4H0M0S");
        resourceRow.put("RemainingWork", "PT4H0M0S");
        resourceRow.put("Cost", Double.valueOf(1000));
        resourceRow.put("ActualCost", Double.valueOf(500));
        resourceRow.put("RemainingCost", Double.valueOf(500));
        resourceRow.put("PercentWorkComplete", Integer.valueOf(50));
        resourceRow.put("WorkGroup", Integer.valueOf(0));
        resourceRow.put("StandardRateFormat", Integer.valueOf(2));
        resourceRow.put("OvertimeRateFormat", Integer.valueOf(2));
        resourceRows.add(resourceRow);
        List<Map<String, Object>> assignmentRows = document.ensureSheet("Assignments");
        Map<String, Object> assignmentRow = new LinkedHashMap<String, Object>();
        assignmentRow.put("UID", "asg-1");
        assignmentRow.put("TaskUID", "draft-120");
        assignmentRow.put("ResourceUID", "res-1");
        assignmentRow.put("Start", "2026-03-16 09:00:00");
        assignmentRow.put("Finish", "2026-03-16 18:00:00");
        assignmentRow.put("StartVariance", "PT0H30M0S");
        assignmentRow.put("FinishVariance", "PT1H0M0S");
        assignmentRow.put("Delay", "PT0H15M0S");
        assignmentRow.put("Milestone", "○");
        assignmentRow.put("WorkContour", Integer.valueOf(1));
        assignmentRow.put("Cost", Double.valueOf(1000));
        assignmentRow.put("ActualWork", "PT4H0M0S");
        assignmentRow.put("RemainingWork", "PT4H0M0S");
        assignmentRow.put("ActualCost", Double.valueOf(300));
        assignmentRow.put("RemainingCost", Double.valueOf(200));
        assignmentRow.put("OvertimeWork", "PT1H0M0S");
        assignmentRow.put("ActualOvertimeWork", "PT0H30M0S");
        assignmentRows.add(assignmentRow);
        return toJson(document).getBytes(StandardCharsets.UTF_8);
    }

    private void writeExtendedWorkbookValues(Path workbookBytesFile) throws IOException {
        CoreApiWorkbookXlsx workbookApi = new CoreApiWorkbookXlsx();
        XlsxWorkbookLike workbook = workbookApi.decodeWorkbook(Files.readAllBytes(workbookBytesFile));
        XlsxRowLike taskRow = findRowByUid(findSheet(workbook, "Tasks"), "draft-110");
        XlsxRowLike resourceRow = findRowByUid(findSheet(workbook, "Resources"), "res-1");
        XlsxRowLike assignmentRow = findRowByUid(findSheet(workbook, "Assignments"), "asg-1");

        taskRow.cells.get(14).value = "2";
        taskRow.cells.get(15).value = "500";
        taskRow.cells.get(17).value = "4";
        taskRow.cells.get(18).value = "2026-03-17 09:00:00";
        taskRow.cells.get(19).value = "2026-03-18 18:00:00";
        resourceRow.cells.get(3).value = "1";
        resourceRow.cells.get(4).value = "MU";
        resourceRow.cells.get(8).value = "1200/h";
        resourceRow.cells.get(9).value = "1800/h";
        resourceRow.cells.get(10).value = "500";
        resourceRow.cells.get(11).value = "PT8H0M0S";
        resourceRow.cells.get(12).value = "PT4H0M0S";
        resourceRow.cells.get(13).value = "PT4H0M0S";
        resourceRow.cells.get(14).value = "1000";
        resourceRow.cells.get(15).value = "500";
        resourceRow.cells.get(16).value = "500";
        resourceRow.cells.get(17).value = "50";
        resourceRow.cells.get(18).value = "0";
        resourceRow.cells.get(19).value = "2";
        resourceRow.cells.get(20).value = "2";
        assignmentRow.cells.get(5).value = "2026-03-16 09:00:00";
        assignmentRow.cells.get(6).value = "2026-03-16 18:00:00";
        assignmentRow.cells.get(7).value = "PT0H30M0S";
        assignmentRow.cells.get(8).value = "PT1H0M0S";
        assignmentRow.cells.get(9).value = "PT0H15M0S";
        assignmentRow.cells.get(10).value = "○";
        assignmentRow.cells.get(11).value = "1";
        assignmentRow.cells.get(14).value = "1000";
        assignmentRow.cells.get(15).value = "PT4H0M0S";
        assignmentRow.cells.get(16).value = "PT4H0M0S";
        assignmentRow.cells.get(17).value = "300";
        assignmentRow.cells.get(18).value = "200";
        assignmentRow.cells.get(19).value = "PT1H0M0S";
        assignmentRow.cells.get(20).value = "PT0H30M0S";
        Files.write(workbookBytesFile, workbookApi.encodeWorkbook(workbook));
    }

    private XlsxSheetLike findSheet(XlsxWorkbookLike workbook, String name) {
        for (XlsxSheetLike sheet : workbook.sheets) {
            if (name.equals(sheet.name)) {
                return sheet;
            }
        }
        return null;
    }

    private XlsxRowLike findRowByUid(XlsxSheetLike sheet, String uid) {
        for (int index = 3; index < sheet.rows.size(); index += 1) {
            XlsxRowLike row = sheet.rows.get(index);
            if (uid.equals(String.valueOf(row.cells.get(0).value))) {
                return row;
            }
        }
        return null;
    }

    private byte[] patchJsonText() {
        StringBuilder builder = new StringBuilder();
        builder.append("{\"operations\":[");
        builder.append("{\"op\":\"update_project\",\"fields\":{\"name\":\"Patched Project\"}},");
        builder.append("{\"op\":\"add_resource\",\"uid\":\"r-new\",\"name\":\"CLI Resource\"}");
        builder.append("]}");
        return builder.toString().getBytes(StandardCharsets.UTF_8);
    }

    private byte[] aiJsonText() {
        StringBuilder builder = new StringBuilder();
        builder.append("{\"view_type\":\"project_draft_view\",");
        builder.append("\"project\":{\"name\":\"AI JSON Imported Project\",\"planned_start\":\"2026-04-01\"},");
        builder.append("\"tasks\":[");
        builder.append("{\"uid\":\"t1\",\"name\":\"Draft Task 1\",\"planned_start\":\"2026-04-01\",\"planned_finish\":\"2026-04-02\"}");
        builder.append("],");
        builder.append("\"resources\":[],");
        builder.append("\"assignments\":[]");
        builder.append("}");
        return builder.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String toJson(WorkbookJsonDocument document) {
        StringBuilder builder = new StringBuilder();
        builder.append("{");
        builder.append("\"format\":\"").append(document.format).append("\",");
        builder.append("\"version\":").append(document.version).append(",");
        builder.append("\"sheets\":{");
        int sheetIndex = 0;
        for (Map.Entry<String, List<Map<String, Object>>> sheetEntry : document.sheets.entrySet()) {
            if (sheetIndex++ > 0) {
                builder.append(",");
            }
            builder.append("\"").append(sheetEntry.getKey()).append("\":[");
            int rowIndex = 0;
            for (Map<String, Object> row : sheetEntry.getValue()) {
                if (rowIndex++ > 0) {
                    builder.append(",");
                }
                builder.append("{");
                int cellIndex = 0;
                for (Map.Entry<String, Object> cell : row.entrySet()) {
                    if (cellIndex++ > 0) {
                        builder.append(",");
                    }
                    builder.append("\"").append(cell.getKey()).append("\":");
                    Object value = cell.getValue();
                    if (value instanceof Number || value instanceof Boolean) {
                        builder.append(String.valueOf(value));
                    } else {
                        builder.append("\"").append(String.valueOf(value)).append("\"");
                    }
                }
                builder.append("}");
            }
            builder.append("]");
        }
        builder.append("}}");
        return builder.toString();
    }

    private String readVendorTestdata(String fileName) throws IOException {
        byte[] bytes = Files.readAllBytes(Paths.get("vendor", "mikuproject", "testdata", fileName));
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private String zipText(Path zipFile) throws IOException {
        StringBuilder builder = new StringBuilder();
        try (ZipInputStream input = new ZipInputStream(Files.newInputStream(zipFile), StandardCharsets.UTF_8)) {
            byte[] buffer = new byte[2048];
            while (input.getNextEntry() != null) {
                int read;
                while ((read = input.read(buffer)) >= 0) {
                    builder.append(new String(buffer, 0, read, StandardCharsets.UTF_8));
                }
            }
        }
        return builder.toString();
    }

    private void deleteTree(Path root) throws IOException {
        if (root == null || !Files.exists(root)) {
            return;
        }
        if (Files.isDirectory(root)) {
            Path[] children;
            try (Stream<Path> stream = Files.list(root)) {
                children = stream.toArray(Path[]::new);
            }
            for (Path child : children) {
                deleteTree(child);
            }
        }
        Files.deleteIfExists(root);
    }
}
