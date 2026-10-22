/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuproject.cli;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import jp.igapyon.mikuproject.coreapi.CoreApiAiJsonUtil;
import jp.igapyon.mikuproject.coreapi.CoreApiWorkbook;
import jp.igapyon.mikuproject.model.ProjectModel;
import jp.igapyon.mikuproject.msprojectxml.MsProjectXml;
import jp.igapyon.mikuproject.projectworkbookjson.ProjectWorkbookJson;
import jp.igapyon.mikuproject.projectworkbookjson.ProjectWorkbookSchema;
import jp.igapyon.mikuproject.projectxlsx.XlsxSheetLike;
import jp.igapyon.mikuproject.projectxlsx.XlsxWorkbookLike;

public class MikuprojectNodeParityTest {
    @TempDir
    Path directory;

    @Test
    public void comparesReportDirectoryBytesWithNodeUpstreamWhenEnabled() throws IOException, InterruptedException {
        assumeNodeParityEnabled();

        Path xmlFile = Files.createTempFile("mikuproject-node-parity", ".xml");
        Files.write(xmlFile, Files.readAllBytes(Paths.get("vendor", "mikuproject", "testdata", "dependency.xml")));
        Path workbookFile = writeWorkbookJson(xmlFile);
        Path javaDir = Files.createTempDirectory("mikuproject-node-parity-java");
        Path nodeDir = Files.createTempDirectory("mikuproject-node-parity-node");

        MikuprojectCli cli = new MikuprojectCli();
        assertEquals(0, cli.run(new String[] { "report", "dir", "--in", workbookFile.toString(), "--out", javaDir.toString() },
                new java.io.PrintStream(new java.io.ByteArrayOutputStream(), true, StandardCharsets.UTF_8.name()),
                new java.io.PrintStream(new java.io.ByteArrayOutputStream(), true, StandardCharsets.UTF_8.name())));

        runNode("src/test/node/export-upstream-report-dir.mjs", xmlFile.toString(), nodeDir.toString());

        List<String> javaNames = listRelativeFiles(javaDir);
        List<String> nodeNames = listRelativeFiles(nodeDir);
        assertEquals(javaNames, nodeNames);
        for (String name : javaNames) {
            byte[] javaBytes = Files.readAllBytes(javaDir.resolve(name));
            byte[] nodeBytes = Files.readAllBytes(nodeDir.resolve(name));
            assertTrue(javaBytes.length > 0, "Java report dir entry is non-empty: " + name);
            assertTrue(nodeBytes.length > 0, "Node report dir entry is non-empty: " + name);
            assertArrayEquals(nodeBytes, javaBytes, name);
        }
    }

    @Test
    public void comparesReportBundleZipBytesWithNodeUpstreamWhenEnabled() throws IOException, InterruptedException {
        assumeNodeParityEnabled();

        Path xmlFile = Files.createTempFile("mikuproject-node-parity-bundle", ".xml");
        Files.write(xmlFile, Files.readAllBytes(Paths.get("vendor", "mikuproject", "testdata", "dependency.xml")));
        Path workbookFile = writeWorkbookJson(xmlFile);
        Path javaZip = Files.createTempFile("mikuproject-node-parity-bundle-java", ".zip");
        Path nodeZip = Files.createTempFile("mikuproject-node-parity-bundle-node", ".zip");

        MikuprojectCli cli = new MikuprojectCli();
        assertEquals(0, cli.run(new String[] { "report", "all", "--in", workbookFile.toString(), "--out", javaZip.toString() },
                new java.io.PrintStream(new java.io.ByteArrayOutputStream(), true, StandardCharsets.UTF_8.name()),
                new java.io.PrintStream(new java.io.ByteArrayOutputStream(), true, StandardCharsets.UTF_8.name())));

        runNode("src/test/node/export-upstream-report-bundle.mjs", xmlFile.toString(), nodeZip.toString());

        byte[] nodeBytes = Files.readAllBytes(nodeZip);
        byte[] javaBytes = Files.readAllBytes(javaZip);
        assertTrue(nodeBytes.length > 0, "Node report bundle ZIP is non-empty");
        assertTrue(javaBytes.length > 0, "Java report bundle ZIP is non-empty");
        List<String> expectedEntries = Arrays.asList("wbs.xlsx", "wbs.md", "mermaid.mmd", "daily.svg", "weekly.svg",
                "monthly-calendar/2026-03.svg");
        assertZipEntries(nodeBytes, expectedEntries, "Node report bundle ZIP");
        assertZipEntries(javaBytes, expectedEntries, "Java report bundle ZIP");
        assertArrayEquals(nodeBytes, javaBytes, "report bundle zip");
    }

    @Test
    public void comparesMonthlySvgZipBytesWithNodeUpstreamWhenEnabled() throws IOException, InterruptedException {
        assumeNodeParityEnabled();

        Path xmlFile = Files.createTempFile("mikuproject-node-parity-monthly", ".xml");
        Files.write(xmlFile, Files.readAllBytes(Paths.get("vendor", "mikuproject", "testdata", "dependency.xml")));
        Path workbookFile = writeWorkbookJson(xmlFile);
        Path javaZip = Files.createTempFile("mikuproject-node-parity-monthly-java", ".zip");
        Path nodeZip = Files.createTempFile("mikuproject-node-parity-monthly-node", ".zip");

        MikuprojectCli cli = new MikuprojectCli();
        assertEquals(0, cli.run(new String[] { "report", "monthly-calendar-svg", "--in", workbookFile.toString(), "--out", javaZip.toString() },
                new java.io.PrintStream(new java.io.ByteArrayOutputStream(), true, StandardCharsets.UTF_8.name()),
                new java.io.PrintStream(new java.io.ByteArrayOutputStream(), true, StandardCharsets.UTF_8.name())));

        runNode("src/test/node/export-upstream-monthly-svg-zip.mjs", xmlFile.toString(), nodeZip.toString());

        byte[] nodeBytes = Files.readAllBytes(nodeZip);
        byte[] javaBytes = Files.readAllBytes(javaZip);
        assertTrue(nodeBytes.length > 0, "Node monthly SVG ZIP is non-empty");
        assertTrue(javaBytes.length > 0, "Java monthly SVG ZIP is non-empty");
        List<String> expectedEntries = Arrays.asList("monthly-calendar/2026-03.svg");
        assertZipEntries(nodeBytes, expectedEntries, "Node monthly SVG ZIP");
        assertZipEntries(javaBytes, expectedEntries, "Java monthly SVG ZIP");
        assertArrayEquals(nodeBytes, javaBytes, "monthly svg zip");
    }

    @Test
    public void comparesStandaloneReportCommandBytesWithNodeWhenEnabled() throws IOException, InterruptedException {
        assumeNodeParityEnabled();

        Path xmlFile = Files.createTempFile("mikuproject-node-parity-report-standalone", ".xml");
        Files.write(xmlFile, Files.readAllBytes(Paths.get("vendor", "mikuproject", "testdata", "dependency.xml")));
        Path workbookFile = writeWorkbookJson(xmlFile);
        Path reportDir = Files.createDirectory(directory.resolve("report-dir"));
        MikuprojectCli cli = new MikuprojectCli();
        assertEquals(0, cli.run(new String[] { "report", "dir", "--in", workbookFile.toString(), "--out", reportDir.toString() },
                new java.io.PrintStream(new java.io.ByteArrayOutputStream(), true, StandardCharsets.UTF_8.name()),
                new java.io.PrintStream(new java.io.ByteArrayOutputStream(), true, StandardCharsets.UTF_8.name())));

        String[][] standaloneReports = {
                { "wbs-xlsx", "wbs.xlsx" },
                { "daily-svg", "daily.svg" },
                { "weekly-svg", "weekly.svg" },
                { "wbs-markdown", "wbs.md" },
                { "mermaid", "mermaid.mmd" }
        };
        for (String[] report : standaloneReports) {
            String subject = report[0];
            String entryName = report[1];
            Path javaOutput = directory.resolve("java-" + subject + ".out");
            Path nodeOutput = directory.resolve("node-" + subject + ".out");
            String[] javaArgs = { "report", subject, "--in", workbookFile.toString(), "--out", javaOutput.toString() };
            String[] nodeArgs = { "report", subject, "--in", workbookFile.toString(), "--out", nodeOutput.toString() };
            NodeJavaLegacyCases.Observation java = NodeJavaLegacyCases.runJava(javaArgs, new byte[0], javaOutput);
            NodeJavaLegacyCases.Observation node = NodeJavaLegacyCases.runNode(nodeArgs, new byte[0], nodeOutput);

            assertEquals(0, java.exitCode, "Java report " + subject + " exit code");
            assertEquals(node.exitCode, java.exitCode, "report " + subject + " exit parity");
            byte[] javaBytes = java.artifacts.get(javaOutput.getFileName().toString());
            byte[] nodeBytes = node.artifacts.get(nodeOutput.getFileName().toString());
            assertTrue(javaBytes.length > 0, "Java report " + subject + " output is non-empty");
            assertTrue(nodeBytes.length > 0, "Node report " + subject + " output is non-empty");
            assertArrayEquals(nodeBytes, javaBytes, "report " + subject + " bytes");
            assertArrayEquals(Files.readAllBytes(reportDir.resolve(entryName)), javaBytes,
                    "report " + subject + " bytes match report dir entry " + entryName);
        }
    }

    @Test
    public void comparesSharedTextCliOutputWithNodeUpstreamWhenEnabled() throws IOException, InterruptedException {
        assumeNodeParityEnabled();

        assertSharedCliResultEqualsNode(new String[] { "state", "summarize", "--in",
                "vendor/mikuproject/testdata/workbook-import-sample.json" });
        assertSharedCliResultEqualsNode(new String[] { "ai", "export", "bundle", "--in",
                "vendor/mikuproject/testdata/workbook-import-sample.json", "--diagnostics", "json" });
        assertSharedCliResultEqualsNode(new String[] { "export", "workbook-json", "--in",
                "vendor/mikuproject/testdata/workbook-import-sample.json", "--diagnostics", "json" });
    }

    @Test
    public void comparesJapaneseSummaryUtf8AndLfBytesWithNodeWhenEnabled() throws IOException, InterruptedException {
        assumeNodeParityEnabled();

        String source = new String(NodeJavaLegacyCases.fixture("BASE.json"), StandardCharsets.UTF_8);
        String japanese = source.replace("\"Value\":\"Project\"", "\"Value\":\"日本語プロジェクト\"");
        Path state = directory.resolve("japanese-state.json");
        Files.write(state, japanese.getBytes(StandardCharsets.UTF_8));

        String[] stdoutArgs = { "state", "summarize", "--in", state.toString(), "--diagnostics", "json" };
        NodeJavaLegacyCases.Observation javaStdout = NodeJavaLegacyCases.runJava(stdoutArgs, new byte[0]);
        NodeJavaLegacyCases.Observation nodeStdout = NodeJavaLegacyCases.runNode(stdoutArgs, new byte[0]);
        assertEquals(nodeStdout.exitCode, javaStdout.exitCode, "JSON summary exit code");
        assertArrayEquals(nodeStdout.stdout, javaStdout.stdout, "JSON summary stdout UTF-8 bytes");
        assertArrayEquals(nodeStdout.stderr, javaStdout.stderr, "JSON summary diagnostics UTF-8 bytes");
        assertUtf8Lf(javaStdout.stdout, "stdout");
        assertUtf8Lf(javaStdout.stderr, "stderr");

        Path javaDirectory = Files.createDirectory(directory.resolve("java"));
        Path nodeDirectory = Files.createDirectory(directory.resolve("node"));
        Path javaOutput = javaDirectory.resolve("summary.json");
        Path nodeOutput = nodeDirectory.resolve("summary.json");
        String[] fileArgs = { "state", "summarize", "--in", state.toString(), "--out", javaOutput.toString() };
        NodeJavaLegacyCases.Observation javaFile = NodeJavaLegacyCases.runJava(fileArgs, new byte[0], javaOutput);
        String[] nodeFileArgs = { "state", "summarize", "--in", state.toString(), "--out", nodeOutput.toString() };
        NodeJavaLegacyCases.Observation nodeFile = NodeJavaLegacyCases.runNode(nodeFileArgs, new byte[0], nodeOutput);
        assertEquals(nodeFile.exitCode, javaFile.exitCode, "file summary exit code");
        assertArrayEquals(nodeFile.artifacts.get("summary.json"), javaFile.artifacts.get("summary.json"), "summary file UTF-8 bytes");
        assertUtf8Lf(javaFile.artifacts.get("summary.json"), "summary file");
    }

    @Test
    public void comparesAiCommandSuccessPathsAndRecordsFrozenOutputMetadataDifferences() throws IOException, InterruptedException {
        assumeNodeParityEnabled();

        Path state = NodeJavaLegacyCases.writeHierarchyState(directory);
        Path patch = directory.resolve("PATCH_OK.json");
        Files.write(patch, NodeJavaLegacyCases.fixture("PATCH_OK.json"));
        Path output = directory.resolve("result.json");
        List<String> byteDifferences = new ArrayList<String>();
        List<String> jsonDifferences = new ArrayList<String>();

        assertLegacySuccessParity(new String[] { "ai", "spec", "--out", output.toString() }, output, false, false, "spec", byteDifferences, jsonDifferences);
        assertLegacySuccessParity(new String[] { "ai", "detect-kind", "--in", patch.toString(), "--out", output.toString(),
                "--diagnostics", "json" }, output, true, false, "detect-kind", byteDifferences, jsonDifferences);
        assertLegacySuccessParity(new String[] { "ai", "export", "project-overview", "--in", state.toString(), "--out",
                output.toString(), "--diagnostics", "json" }, output, false, true, "project-overview", byteDifferences, jsonDifferences);
        assertLegacySuccessParity(new String[] { "ai", "export", "task-edit", "--in", state.toString(), "--select", "auto",
                "--out", output.toString(), "--diagnostics", "json" }, output, false, true, "task-edit-auto", byteDifferences, jsonDifferences);
        assertLegacySuccessParity(new String[] { "ai", "export", "task-edit", "--in", state.toString(), "--select", "uid",
                "--task-uid", "2", "--out", output.toString(), "--diagnostics", "json" }, output, false, true, "task-edit-uid", byteDifferences, jsonDifferences);
        assertLegacySuccessParity(new String[] { "ai", "export", "phase-detail", "--in", state.toString(), "--phase-uid", "1",
                "--mode", "scoped", "--out", output.toString(), "--diagnostics", "json" }, output, false, true, "phase-detail-scoped-uid", byteDifferences, jsonDifferences);
        assertLegacySuccessParity(new String[] { "ai", "export", "phase-detail", "--in", state.toString(), "--select", "auto",
                "--mode", "full", "--out", output.toString(), "--diagnostics", "json" }, output, false, true, "phase-detail-full-auto", byteDifferences, jsonDifferences);
        assertLegacySuccessParity(new String[] { "ai", "export", "bundle", "--in", state.toString(), "--out", output.toString(),
                "--diagnostics", "json" }, output, false, true, "bundle", byteDifferences, jsonDifferences);
        assertLegacySuccessParity(new String[] { "ai", "validate-patch", "--state", state.toString(), "--in", patch.toString(),
                "--out", output.toString(), "--diagnostics", "json" }, output, true, true, "validate-patch", byteDifferences, jsonDifferences);
        assertEquals(Arrays.asList("project-overview", "task-edit-auto", "task-edit-uid", "phase-detail-scoped-uid",
                "phase-detail-full-auto", "bundle", "validate-patch"), byteDifferences, "reviewed artifact byte difference cases");
        assertEquals(Arrays.asList(
                "project-overview:$.phases[0].duration_hours number text Node=24 Java=24.0",
                "task-edit-auto:$.target_task.calendar_uid value Node=null Java=",
                "task-edit-auto:$.target_task.notes additional in Java=",
                "task-edit-auto:$.target_task.planned_duration_hours number text Node=8 Java=8.0",
                "task-edit-uid:$.target_task.calendar_uid value Node=null Java=",
                "task-edit-uid:$.target_task.notes additional in Java=",
                "task-edit-uid:$.target_task.planned_duration_hours number text Node=8 Java=8.0",
                "phase-detail-scoped-uid:$.tasks[0].planned_duration_hours number text Node=8 Java=8.0",
                "phase-detail-scoped-uid:$.tasks[1].planned_duration_hours number text Node=16 Java=16.0",
                "phase-detail-full-auto:$.tasks[0].planned_duration_hours number text Node=8 Java=8.0",
                "phase-detail-full-auto:$.tasks[1].planned_duration_hours number text Node=16 Java=16.0",
                "bundle:$.phase_detail_views_full[0].tasks[0].planned_duration_hours number text Node=8 Java=8.0",
                "bundle:$.phase_detail_views_full[0].tasks[1].planned_duration_hours number text Node=16 Java=16.0",
                "bundle:$.project_overview_view.phases[0].duration_hours number text Node=24 Java=24.0",
                "bundle:$.task_edit_views_full[0].target_task.calendar_uid value Node=null Java=",
                "bundle:$.task_edit_views_full[0].target_task.notes additional in Java=",
                "bundle:$.task_edit_views_full[0].target_task.planned_duration_hours number text Node=8 Java=8.0",
                "bundle:$.task_edit_views_full[1].target_task.calendar_uid value Node=null Java=",
                "bundle:$.task_edit_views_full[1].target_task.planned_duration_hours number text Node=16 Java=16.0",
                "validate-patch:$.context additional in Java=ai validate-patch",
                "validate-patch:$.io.output.mode value Node=stdout Java=file",
                "validate-patch:$.io.output.path additional in Java=<result.json>"), jsonDifferences,
                "reviewed JSON difference paths (numeric lexical, optional field shape, and frozen metadata)");
    }

    @Test
    public void comparesStateCommandSuccessPathsAndPreservesInputs() throws IOException, InterruptedException {
        assumeNodeParityEnabled();

        Path draft = directory.resolve("PROJECT_DRAFT.json");
        Files.write(draft, NodeJavaLegacyCases.fixture("PROJECT_DRAFT.json"));
        Path draftState = directory.resolve("draft-state.json");
        Path summary = directory.resolve("summary.json");
        Path patch = directory.resolve("PATCH_OK.json");
        Files.write(patch, NodeJavaLegacyCases.fixture("PATCH_OK.json"));
        Path patchedState = directory.resolve("patched-state.json");
        Path diff = directory.resolve("state-diff.json");
        List<String> byteDifferences = new ArrayList<String>();
        List<String> jsonDifferences = new ArrayList<String>();

        assertLegacySuccessParity(new String[] { "state", "from-draft", "--in", draft.toString(), "--out", draftState.toString() },
                draftState, false, true, "state-from-draft", byteDifferences, jsonDifferences);
        byte[] draftInput = Files.readAllBytes(draft);
        byte[] patchInput = Files.readAllBytes(patch);
        byte[] draftStateInput = Files.readAllBytes(draftState);

        assertLegacySuccessParity(new String[] { "state", "summarize", "--in", draftState.toString(), "--out", summary.toString(),
                "--diagnostics", "json" }, summary, false, true, "state-summarize", byteDifferences, jsonDifferences);
        assertLegacySuccessParity(new String[] { "state", "apply-patch", "--state", draftState.toString(), "--in", patch.toString(),
                "--out", patchedState.toString(), "--diagnostics", "json" }, patchedState, false, true,
                "state-apply-patch", byteDifferences, jsonDifferences);
        byte[] patchedStateInput = Files.readAllBytes(patchedState);

        assertLegacySuccessParity(new String[] { "state", "diff", "--before", draftState.toString(), "--after", patchedState.toString(),
                "--out", diff.toString(), "--diagnostics", "json" }, diff, false, true, "state-diff", byteDifferences,
                jsonDifferences);

        assertArrayEquals(NodeJavaLegacyCases.fixture("PROJECT_DRAFT.json"), Files.readAllBytes(draft), "draft input unchanged");
        assertArrayEquals(NodeJavaLegacyCases.fixture("PATCH_OK.json"), Files.readAllBytes(patch), "patch input unchanged");
        assertArrayEquals(draftStateInput, Files.readAllBytes(draftState), "from-draft output unchanged by consumers");
        assertArrayEquals(patchedStateInput, Files.readAllBytes(patchedState), "apply-patch output unchanged by diff");
        assertEquals(Arrays.asList("state-summarize", "state-apply-patch"), byteDifferences, "reviewed state artifact byte differences");
        assertEquals(Arrays.asList(
                "state-summarize:$.major_milestones[0].date value Node=2026-04-01 Java=2026-04-01T18:00:00",
                "state-summarize:$.project.planned_finish value Node=2026-04-01 Java=2026-04-01T18:00:00",
                "state-summarize:$.project.planned_start value Node=2026-04-01 Java=2026-04-01T09:00:00",
                "state-apply-patch:$.sheets.Calendars[0].BaseCalendarUID value Node=null Java=",
                "state-apply-patch:$.sheets.Project[2].Value value Node=null Java=",
                "state-apply-patch:$.sheets.Project[3].Value value Node=null Java=",
                "state-apply-patch:$.sheets.Project[4].Value value Node=2026-04-01 Java=2026-04-01T09:00:00",
                "state-apply-patch:$.sheets.Project[5].Value value Node=2026-04-01 Java=2026-04-01T18:00:00",
                "state-apply-patch:$.sheets.Project[6].Value value Node=null Java=",
                "state-apply-patch:$.sheets.Project[7].Value value Node=null Java=",
                "state-apply-patch:$.sheets.Tasks[0].Finish value Node=2026-04-01 Java=2026-04-01T18:00:00",
                "state-apply-patch:$.sheets.Tasks[0].Start value Node=2026-04-01 Java=2026-04-01T09:00:00"), jsonDifferences,
                "reviewed state value differences (date-time enrichment and blank/null cells)");
    }

    @Test
    public void comparesExchangeCommandSuccessPathsAndRecordsNativeXlsxColumns() throws IOException, InterruptedException {
        assumeNodeParityEnabled();

        Path state = NodeJavaLegacyCases.writeHierarchyState(directory);
        byte[] stateInput = Files.readAllBytes(state);
        Path outputJson = directory.resolve("exchange.json");
        List<String> jsonByteDifferences = new ArrayList<String>();
        List<String> jsonValueDifferences = new ArrayList<String>();

        assertLegacySuccessParity(new String[] { "export", "workbook-json", "--in", state.toString(), "--out", outputJson.toString(),
                "--diagnostics", "json" }, outputJson, false, true, "export-workbook-json", jsonByteDifferences,
                jsonValueDifferences);

        Path xmlOutput = directory.resolve("exchange.xml");
        String[] xmlArgs = { "export", "xml", "--in", state.toString(), "--out", xmlOutput.toString(), "--diagnostics", "json" };
        NodeJavaLegacyCases.Observation javaXml = NodeJavaLegacyCases.runJava(xmlArgs, new byte[0], xmlOutput);
        NodeJavaLegacyCases.Observation nodeXml = NodeJavaLegacyCases.runNode(xmlArgs, new byte[0], xmlOutput);
        assertEquals(0, javaXml.exitCode, "Java export xml exit");
        assertEquals(nodeXml.exitCode, javaXml.exitCode, "export xml exit parity");
        assertArrayEquals(nodeXml.stdout, javaXml.stdout, "export xml stdout");
        byte[] nodeXmlBytes = nodeXml.artifacts.get(xmlOutput.getFileName().toString());
        byte[] javaXmlBytes = javaXml.artifacts.get(xmlOutput.getFileName().toString());
        assertFalse(Arrays.equals(nodeXmlBytes, javaXmlBytes), "XML artifacts should be compared semantically, not by raw bytes");
        Map<String, Object> nodeXmlDiagnostics = jsonObject(new String(nodeXml.stderr, StandardCharsets.UTF_8));
        Map<String, Object> javaXmlDiagnostics = jsonObject(new String(javaXml.stderr, StandardCharsets.UTF_8));
        assertEquals(new String(nodeXmlBytes, StandardCharsets.UTF_8).length(),
                ((Number) nodeXmlDiagnostics.get("output_length")).intValue(), "Node XML diagnostic length");
        assertEquals(new String(javaXmlBytes, StandardCharsets.UTF_8).length(),
                ((Number) javaXmlDiagnostics.get("output_length")).intValue(), "Java XML diagnostic length");
        nodeXmlDiagnostics.remove("output_length");
        javaXmlDiagnostics.remove("output_length");
        assertEquals(nodeXmlDiagnostics, javaXmlDiagnostics, "export xml diagnostics excluding runtime output length");
        assertTrue(nodeXmlBytes.length > 0, "Node XML artifact should exist");
        assertTrue(javaXmlBytes.length > 0, "Java XML artifact should exist");
        Path nodeXmlFile = directory.resolve("node-export.xml");
        Path javaXmlFile = directory.resolve("java-export.xml");
        Files.write(nodeXmlFile, nodeXmlBytes);
        Files.write(javaXmlFile, javaXmlBytes);
        Path nodeXmlState = writeWorkbookJson(nodeXmlFile);
        Path javaXmlState = writeWorkbookJson(javaXmlFile);
        Object nodeXmlDocument = new CoreApiAiJsonUtil().parseJsonText(
                new String(Files.readAllBytes(nodeXmlState), StandardCharsets.UTF_8));
        Object javaXmlDocument = new CoreApiAiJsonUtil().parseJsonText(
                new String(Files.readAllBytes(javaXmlState), StandardCharsets.UTF_8));
        List<String> xmlSemanticDifferences = new ArrayList<String>();
        collectJsonDifferences(nodeXmlDocument, javaXmlDocument, "$", "export-xml-model", xmlSemanticDifferences);

        Path xlsxOutput = directory.resolve("exchange.xlsx");
        String[] xlsxArgs = { "export", "xlsx", "--in", state.toString(), "--out", xlsxOutput.toString(), "--diagnostics", "json" };
        NodeJavaLegacyCases.Observation javaXlsx = NodeJavaLegacyCases.runJava(xlsxArgs, new byte[0], xlsxOutput);
        NodeJavaLegacyCases.Observation nodeXlsx = NodeJavaLegacyCases.runNode(xlsxArgs, new byte[0], xlsxOutput);
        assertEquals(0, javaXlsx.exitCode, "Java export xlsx exit");
        assertEquals(nodeXlsx.exitCode, javaXlsx.exitCode, "export xlsx exit parity");
        assertArrayEquals(nodeXlsx.stdout, javaXlsx.stdout, "export xlsx stdout");
        byte[] nodeXlsxBytes = nodeXlsx.artifacts.get(xlsxOutput.getFileName().toString());
        byte[] javaXlsxBytes = javaXlsx.artifacts.get(xlsxOutput.getFileName().toString());
        Map<String, Object> nodeXlsxDiagnostics = jsonObject(new String(nodeXlsx.stderr, StandardCharsets.UTF_8));
        Map<String, Object> javaXlsxDiagnostics = jsonObject(new String(javaXlsx.stderr, StandardCharsets.UTF_8));
        assertEquals(nodeXlsxBytes.length, ((Number) nodeXlsxDiagnostics.get("byte_length")).intValue(), "Node XLSX byte length");
        assertEquals(javaXlsxBytes.length, ((Number) javaXlsxDiagnostics.get("byte_length")).intValue(), "Java XLSX byte length");
        nodeXlsxDiagnostics.remove("byte_length");
        javaXlsxDiagnostics.remove("byte_length");
        assertEquals(nodeXlsxDiagnostics, javaXlsxDiagnostics, "export xlsx diagnostics excluding binary size");
        assertFalse(Arrays.equals(nodeXlsxBytes, javaXlsxBytes), "Java native XLSX columns intentionally differ from Node");

        Map<String, List<String>> nodeHeaders = xlsxHeaders(nodeXlsxBytes);
        Map<String, List<String>> javaHeaders = xlsxHeaders(javaXlsxBytes);
        assertEquals(new ArrayList<String>(nodeHeaders.keySet()), new ArrayList<String>(javaHeaders.keySet()), "XLSX sheet names/order");
        for (String sheetName : nodeHeaders.keySet()) {
            assertHeadersAreSubsequence(nodeHeaders.get(sheetName), javaHeaders.get(sheetName), sheetName);
        }
        assertEquals(Arrays.asList("Type", "Priority", "ConstraintType", "ConstraintDate", "Deadline"),
                extraHeaders(nodeHeaders.get("Tasks"), javaHeaders.get("Tasks")), "Java-only Tasks headers");
        assertEquals(Arrays.asList("Cost", "ActualCost", "RemainingCost", "PercentWorkComplete", "WorkGroup",
                "StandardRateFormat", "OvertimeRateFormat"),
                extraHeaders(nodeHeaders.get("Resources"), javaHeaders.get("Resources")), "Java-only Resources headers");
        assertEquals(Arrays.asList("StartVariance", "FinishVariance", "Delay", "Milestone", "WorkContour", "Cost",
                "ActualCost", "RemainingCost", "OvertimeWork", "ActualOvertimeWork"),
                extraHeaders(nodeHeaders.get("Assignments"), javaHeaders.get("Assignments")), "Java-only Assignments headers");
        for (String sheetName : Arrays.asList("Project", "Calendars", "NonWorkingDays", "Options")) {
            assertEquals(Collections.emptyList(), extraHeaders(nodeHeaders.get(sheetName), javaHeaders.get(sheetName)),
                    "no Java-only headers in " + sheetName);
        }

        Path nodeXlsxInput = directory.resolve("node-export.xlsx");
        Files.write(nodeXlsxInput, nodeXlsxBytes);
        assertLegacySuccessParity(new String[] { "import", "xlsx", "--in", nodeXlsxInput.toString(), "--out", outputJson.toString(),
                "--diagnostics", "json" }, outputJson, false, true, "import-xlsx-node-export", jsonByteDifferences,
                jsonValueDifferences);

        assertArrayEquals(stateInput, Files.readAllBytes(state), "exchange commands leave workbook input unchanged");
        assertEquals(Arrays.asList("export-workbook-json", "import-xlsx-node-export"), jsonByteDifferences,
                "reviewed common JSON exchange artifact byte differences");
        assertEquals(Arrays.asList(
                "export-workbook-json:$.sheets.Project[1].Value value Node=null Java=",
                "export-workbook-json:$.sheets.Project[2].Value value Node=null Java=",
                "export-workbook-json:$.sheets.Project[3].Value value Node=null Java=",
                "export-workbook-json:$.sheets.Project[4].Value value Node=2026-03-16 09:00:00 Java=2026-03-16T09:00:00",
                "export-workbook-json:$.sheets.Project[5].Value value Node=2026-03-18 18:00:00 Java=2026-03-18T18:00:00",
                "export-workbook-json:$.sheets.Project[6].Value value Node=2026-03-16 09:00:00 Java=2026-03-16T09:00:00",
                "export-workbook-json:$.sheets.Tasks[0].Finish value Node=2026-03-18 18:00:00 Java=2026-03-18T18:00:00",
                "export-workbook-json:$.sheets.Tasks[0].Start value Node=2026-03-16 09:00:00 Java=2026-03-16T09:00:00",
                "export-workbook-json:$.sheets.Tasks[1].Finish value Node=2026-03-16 18:00:00 Java=2026-03-16T18:00:00",
                "export-workbook-json:$.sheets.Tasks[1].Start value Node=2026-03-16 09:00:00 Java=2026-03-16T09:00:00",
                "export-workbook-json:$.sheets.Tasks[2].Finish value Node=2026-03-18 18:00:00 Java=2026-03-18T18:00:00",
                "export-workbook-json:$.sheets.Tasks[2].Start value Node=2026-03-17 09:00:00 Java=2026-03-17T09:00:00",
                "import-xlsx-node-export:$.sheets.Project[4].Value value Node=2026-03-16 09:00:00 Java=2026-03-16T09:00:00",
                "import-xlsx-node-export:$.sheets.Project[5].Value value Node=2026-03-18 18:00:00 Java=2026-03-18T18:00:00",
                "import-xlsx-node-export:$.sheets.Project[6].Value value Node=2026-03-16 09:00:00 Java=2026-03-16T09:00:00",
                "import-xlsx-node-export:$.sheets.Tasks[0].Finish value Node=2026-03-18 18:00:00 Java=2026-03-18T18:00:00",
                "import-xlsx-node-export:$.sheets.Tasks[0].Start value Node=2026-03-16 09:00:00 Java=2026-03-16T09:00:00",
                "import-xlsx-node-export:$.sheets.Tasks[1].Finish value Node=2026-03-16 18:00:00 Java=2026-03-16T18:00:00",
                "import-xlsx-node-export:$.sheets.Tasks[1].Start value Node=2026-03-16 09:00:00 Java=2026-03-16T09:00:00",
                "import-xlsx-node-export:$.sheets.Tasks[2].Finish value Node=2026-03-18 18:00:00 Java=2026-03-18T18:00:00",
                "import-xlsx-node-export:$.sheets.Tasks[2].Start value Node=2026-03-17 09:00:00 Java=2026-03-17T09:00:00"), jsonValueDifferences,
                "reviewed common JSON exchange date and field differences");
        assertEquals(Arrays.asList(), xmlSemanticDifferences, "XML import semantic projection");
    }

    private void assertLegacySuccessParity(String[] args, Path artifact, boolean frozenOutputMetadataDifference,
            boolean jsonArtifact, String label, List<String> byteDifferences, List<String> jsonDifferences)
            throws IOException, InterruptedException {
        NodeJavaLegacyCases.Observation java = NodeJavaLegacyCases.runJava(args, new byte[0], artifact);
        NodeJavaLegacyCases.Observation node = NodeJavaLegacyCases.runNode(args, new byte[0], artifact);
        assertEquals(0, java.exitCode, "Java exit: " + String.join(" ", args));
        assertEquals(node.exitCode, java.exitCode, "exit code: " + String.join(" ", args));
        assertArrayEquals(node.stdout, java.stdout, "stdout: " + String.join(" ", args));
        byte[] nodeArtifact = node.artifacts.get(artifact.getFileName().toString());
        byte[] javaArtifact = java.artifacts.get(artifact.getFileName().toString());
        if (!Arrays.equals(nodeArtifact, javaArtifact)) {
            assertTrue(jsonArtifact, "unexpected non-JSON artifact byte difference: " + label);
            Object nodeJson = new CoreApiAiJsonUtil().parseJsonText(new String(nodeArtifact, StandardCharsets.UTF_8));
            Object javaJson = new CoreApiAiJsonUtil().parseJsonText(new String(javaArtifact, StandardCharsets.UTF_8));
            collectJsonDifferences(nodeJson, javaJson, "$", label, jsonDifferences);
            byteDifferences.add(label);
        }
        if (!frozenOutputMetadataDifference) {
            assertArrayEquals(node.stderr, java.stderr, "stderr: " + String.join(" ", args));
            return;
        }
        if (node.stderr.length == 0 || java.stderr.length == 0) {
            assertArrayEquals(node.stderr, java.stderr, "stderr without diagnostics: " + String.join(" ", args));
            return;
        }

        Map<String, Object> javaDiagnostics = jsonObject(new String(java.stderr, StandardCharsets.UTF_8));
        Map<String, Object> nodeDiagnostics = jsonObject(new String(node.stderr, StandardCharsets.UTF_8));
        Map<?, ?> javaIo = (Map<?, ?>) javaDiagnostics.get("io");
        Map<?, ?> nodeIo = (Map<?, ?>) nodeDiagnostics.get("io");
        assertEquals("file", ((Map<?, ?>) javaIo.get("output")).get("mode"));
        assertEquals("stdout", ((Map<?, ?>) nodeIo.get("output")).get("mode"));
        @SuppressWarnings("unchecked")
        Map<String, Object> mutableJavaIo = (Map<String, Object>) javaIo;
        @SuppressWarnings("unchecked")
        Map<String, Object> mutableNodeIo = (Map<String, Object>) nodeIo;
        mutableJavaIo.put("output", mutableNodeIo.get("output"));
        assertEquals(nodeDiagnostics, javaDiagnostics, "only the frozen output metadata differs: " + String.join(" ", args));
    }

    private void collectJsonDifferences(Object first, Object second, String path, String label, List<String> differences) {
        if (first instanceof Number && second instanceof Number) {
            java.math.BigDecimal nodeNumber = new java.math.BigDecimal(String.valueOf(first));
            java.math.BigDecimal javaNumber = new java.math.BigDecimal(String.valueOf(second));
            if (nodeNumber.compareTo(javaNumber) != 0) {
                differences.add(label + ":" + path + " value Node=" + first + " Java=" + second);
            } else if (!String.valueOf(first).equals(String.valueOf(second))) {
                differences.add(label + ":" + path + " number text Node=" + first + " Java=" + second);
            }
            return;
        }
        if (first instanceof Map<?, ?> && second instanceof Map<?, ?>) {
            Map<?, ?> left = (Map<?, ?>) first;
            Map<?, ?> right = (Map<?, ?>) second;
            java.util.TreeSet<String> keys = new java.util.TreeSet<String>();
            for (Object key : left.keySet()) keys.add(String.valueOf(key));
            for (Object key : right.keySet()) keys.add(String.valueOf(key));
            for (String key : keys) {
                boolean inNode = left.containsKey(key);
                boolean inJava = right.containsKey(key);
                if (!inNode) {
                    String value = label.equals("validate-patch") && "$.io.output".equals(path) && "path".equals(key)
                            ? "<result.json>" : String.valueOf(right.get(key));
                    differences.add(label + ":" + path + "." + key + " additional in Java=" + value);
                }
                else if (!inJava) differences.add(label + ":" + path + "." + key + " missing in Java; Node=" + left.get(key));
                else collectJsonDifferences(left.get(key), right.get(key), path + "." + key, label, differences);
            }
            return;
        }
        if (first instanceof List<?> && second instanceof List<?>) {
            List<?> left = (List<?>) first;
            List<?> right = (List<?>) second;
            if (left.size() != right.size()) differences.add(label + ":" + path + " list size Node=" + left.size() + " Java=" + right.size());
            for (int index = 0; index < Math.min(left.size(), right.size()); index++) {
                collectJsonDifferences(left.get(index), right.get(index), path + "[" + index + "]", label, differences);
            }
            return;
        }
        if (first == null ? second != null : !first.equals(second)) {
            differences.add(label + ":" + path + " value Node=" + first + " Java=" + second);
        }
    }

    private Map<String, List<String>> xlsxHeaders(byte[] bytes) {
        XlsxWorkbookLike workbook = new CoreApiWorkbook().xlsx.decodeWorkbook(bytes);
        Map<String, List<String>> result = new LinkedHashMap<String, List<String>>();
        for (XlsxSheetLike sheet : workbook.sheets) {
            int headerIndex = "Options".equals(sheet.name) ? 0 : ProjectWorkbookSchema.HEADER_ROW_INDEX.intValue();
            List<String> headers = new ArrayList<String>();
            for (jp.igapyon.mikuproject.projectxlsx.XlsxCellLike cell : sheet.rows.get(headerIndex).cells) {
                headers.add(cell.value == null ? "" : String.valueOf(cell.value));
            }
            result.put(sheet.name, headers);
        }
        return result;
    }

    private List<String> extraHeaders(List<String> nodeHeaders, List<String> javaHeaders) {
        List<String> result = new ArrayList<String>();
        for (String header : javaHeaders) {
            if (!nodeHeaders.contains(header)) result.add(header);
        }
        return result;
    }

    private void assertHeadersAreSubsequence(List<String> nodeHeaders, List<String> javaHeaders, String sheetName) {
        int lastIndex = -1;
        for (String header : nodeHeaders) {
            int index = javaHeaders.indexOf(header);
            assertTrue(index > lastIndex, "Node header order must remain intact in Java " + sheetName + ": " + header);
            lastIndex = index;
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> jsonObject(String text) {
        return (Map<String, Object>) new CoreApiAiJsonUtil().parseJsonText(text);
    }

    private void assertUtf8Lf(byte[] bytes, String label) {
        assertTrue(bytes.length > 0, label + " should have data");
        assertTrue(bytes[bytes.length - 1] == (byte) '\n', label + " should end in LF");
        assertTrue(!new String(bytes, StandardCharsets.UTF_8).contains("\r"), label + " should not contain CR");
        assertArrayEquals(bytes, new String(bytes, StandardCharsets.UTF_8).getBytes(StandardCharsets.UTF_8), label + " UTF-8 round trip");
    }

    private void assumeNodeParityEnabled() {
        assumeTrue("true".equalsIgnoreCase(System.getenv("MIKUPROJECT_RUN_NODE_PARITY")),
                "set MIKUPROJECT_RUN_NODE_PARITY=true to run Node upstream parity");
        assumeTrue(Files.isDirectory(Paths.get("vendor", "mikuproject", "node_modules")),
                "run npm --prefix vendor/mikuproject ci before Node upstream parity");
    }

    private void runNode(String script, String input, String output) throws IOException, InterruptedException {
        Process process = new ProcessBuilder(System.getProperty("miku.project.node.binary", "node"), script, input, output)
                .directory(Paths.get(".").toFile())
                .redirectErrorStream(true)
                .start();
        byte[] processOutput = readAll(process.getInputStream());
        int exitCode = process.waitFor();
        assertEquals(0, exitCode, new String(processOutput, StandardCharsets.UTF_8));
    }

    private void assertSharedCliResultEqualsNode(String[] args) throws IOException, InterruptedException {
        java.io.ByteArrayOutputStream javaOut = new java.io.ByteArrayOutputStream();
        java.io.ByteArrayOutputStream javaErr = new java.io.ByteArrayOutputStream();
        int javaExitCode = new MikuprojectCli().run(args,
                new java.io.PrintStream(javaOut, true, StandardCharsets.UTF_8.name()),
                new java.io.PrintStream(javaErr, true, StandardCharsets.UTF_8.name()));

        List<String> command = new ArrayList<String>();
        command.add("node");
        command.add("vendor/mikuproject/scripts/miku-project-cli.mjs");
        Collections.addAll(command, args);
        Process process = new ProcessBuilder(command).directory(Paths.get(".").toFile()).start();
        byte[] nodeOut = readAll(process.getInputStream());
        byte[] nodeErr = readAll(process.getErrorStream());
        int nodeExitCode = process.waitFor();

        assertEquals(nodeExitCode, javaExitCode, "exit code");
        assertArrayEquals(nodeOut, javaOut.toByteArray(), "stdout");
        assertArrayEquals(nodeErr, javaErr.toByteArray(), "stderr");
    }

    private Path writeWorkbookJson(Path xmlFile) throws IOException {
        MsProjectXml msProjectXml = new MsProjectXml();
        ProjectWorkbookJson workbookJson = new ProjectWorkbookJson();
        CoreApiAiJsonUtil jsonUtil = new CoreApiAiJsonUtil();
        ProjectModel model = msProjectXml.importFromXml(Files.readString(xmlFile, StandardCharsets.UTF_8));
        jp.igapyon.mikuproject.projectworkbookjson.WorkbookJsonDocument document = workbookJson.exportProjectWorkbookJson(model);
        Map<String, Object> json = new LinkedHashMap<String, Object>();
        json.put("format", document.format);
        json.put("version", document.version);
        json.put("sheets", document.sheets);
        Path output = Files.createTempFile("mikuproject-node-parity-workbook", ".json");
        Files.write(output, jsonUtil.stringifyJson(json).getBytes(StandardCharsets.UTF_8));
        return output;
    }

    private List<String> listRelativeFiles(Path root) throws IOException {
        List<String> names = new ArrayList<String>();
        Stream<Path> stream = Files.walk(root);
        try {
            stream.filter(Files::isRegularFile).forEach(path -> names.add(root.relativize(path).toString().replace('\\', '/')));
        } finally {
            stream.close();
        }
        Collections.sort(names);
        return names;
    }

    private void assertZipEntries(byte[] zipBytes, List<String> expectedNames, String label) throws IOException {
        List<String> names = new ArrayList<String>();
        ZipInputStream input = new ZipInputStream(new ByteArrayInputStream(zipBytes));
        try {
            ZipEntry entry;
            while ((entry = input.getNextEntry()) != null) {
                byte[] contents = readAll(input);
                assertTrue(contents.length > 0, label + " entry is non-empty: " + entry.getName());
                names.add(entry.getName());
                input.closeEntry();
            }
        } finally {
            input.close();
        }
        assertEquals(expectedNames, names, label + " entry names");
    }

    private byte[] readAll(java.io.InputStream input) throws IOException {
        java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int read;
        while ((read = input.read(buffer)) >= 0) {
            output.write(buffer, 0, read);
        }
        return output.toByteArray();
    }
}
