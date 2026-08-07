/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuproject.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import jp.igapyon.mikuproject.coreapi.CoreApiAiJsonUtil;
import jp.igapyon.mikuproject.model.ProjectModel;
import jp.igapyon.mikuproject.msprojectxml.MsProjectSamples;
import jp.igapyon.mikuproject.msprojectxml.MsProjectXml;
import jp.igapyon.mikuproject.projectworkbookjson.ProjectWorkbookJson;
import jp.igapyon.mikuproject.projectworkbookjson.WorkbookJsonDocument;

public class MikuprojectCliTest {
    @Test
    public void printsUsageForHelp() throws IOException {
        MikuprojectCli cli = new MikuprojectCli();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();

        int exitCode = cli.run(new String[] { "help" }, stream(out), stream(err));

        assertEquals(0, exitCode);
        assertTrue(text(out).contains("ai spec"));
        assertTrue(text(out).contains("state from-draft [--in draft.editjson|-]"));
        assertTrue(text(out).contains("report wbs-xlsx [--in workbook.json|-] [--diagnostics text|json] (--out report.xlsx|--out-base64 -)"));
        assertFalse(text(out).contains("xlsxbin"));
        assertEquals("", text(err));
    }

    @Test
    public void printsUsageForHelpAliases() throws IOException {
        MikuprojectCli cli = new MikuprojectCli();
        ByteArrayOutputStream shortHelpOut = new ByteArrayOutputStream();
        ByteArrayOutputStream longHelpOut = new ByteArrayOutputStream();

        assertEquals(0, cli.run(new String[] { "-h" }, stream(shortHelpOut), stream(new ByteArrayOutputStream())));
        assertEquals(0, cli.run(new String[] { "--help" }, stream(longHelpOut), stream(new ByteArrayOutputStream())));

        assertTrue(text(shortHelpOut).contains("report dir --in workbook.json --out report.dir"));
        assertTrue(text(shortHelpOut).contains("ai export bundle [--in workbook.json|-]"));
        assertTrue(text(longHelpOut).contains("ai export phase-detail [--in workbook.json|-]"));
        assertTrue(text(longHelpOut).contains("--root-task-uid rootTaskUid"));
        assertFalse(text(longHelpOut).contains("--root-uid"));
    }

    @Test
    public void printsVersion() throws IOException {
        MikuprojectCli cli = new MikuprojectCli();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();

        int exitCode = cli.run(new String[] { "--version" }, stream(out), stream(err));

        assertEquals(0, exitCode);
        assertEquals("mikuproject-java 0.8.3\n", text(out));
        assertEquals("", text(err));
    }

    @Test
    public void keepsReadmeCliCommandListInSyncWithHelpOutput() throws IOException {
        MikuprojectCli cli = new MikuprojectCli();
        ByteArrayOutputStream helpOut = new ByteArrayOutputStream();

        assertEquals(0, cli.run(new String[] { "help" }, stream(helpOut), stream(new ByteArrayOutputStream())));

        List<String> actualCommands = extractUsageCommands(text(helpOut));
        List<String> readmeCommands = extractReadmeCliCommands(Files.readString(Paths.get("README.md"), StandardCharsets.UTF_8));

        assertEquals(readmeCommands, actualCommands);
    }

    @Test
    public void supportsAgentFriendlyGroupedCommandsWithWorkbookJsonState() throws IOException {
        MikuprojectCli cli = new MikuprojectCli();
        Path draftFile = Files.createTempFile("mikuproject-cli-draft", ".editjson");
        Path patchFile = Files.createTempFile("mikuproject-cli-patch", ".editjson");
        Path workbookFile = Files.createTempFile("mikuproject-cli-state", ".json");
        Path nextWorkbookFile = Files.createTempFile("mikuproject-cli-state-next", ".json");
        Path reportZipFile = Files.createTempFile("mikuproject-cli-report", ".zip");
        Path reportDir = Files.createTempDirectory("mikuproject-cli-report-dir");
        Path wbsXlsxFile = Files.createTempFile("mikuproject-cli-wbs", ".xlsx");
        Path structuralXlsxFile = Files.createTempFile("mikuproject-cli-workbook", ".xlsx");
        Path importedWorkbookFile = Files.createTempFile("mikuproject-cli-imported", ".json");
        Path exportedXmlFile = Files.createTempFile("mikuproject-cli-exported", ".xml");
        try {
            Files.write(draftFile, aiJsonText());
            Files.write(patchFile, patchJsonText());

            ByteArrayOutputStream specOut = new ByteArrayOutputStream();
            ByteArrayOutputStream kindOut = new ByteArrayOutputStream();
            ByteArrayOutputStream validateOut = new ByteArrayOutputStream();
            ByteArrayOutputStream summaryOut = new ByteArrayOutputStream();
            ByteArrayOutputStream diffOut = new ByteArrayOutputStream();
            ByteArrayOutputStream overviewOut = new ByteArrayOutputStream();
            ByteArrayOutputStream bundleOut = new ByteArrayOutputStream();
            ByteArrayOutputStream taskEditOut = new ByteArrayOutputStream();
            ByteArrayOutputStream phaseOut = new ByteArrayOutputStream();
            ByteArrayOutputStream markdownOut = new ByteArrayOutputStream();
            ByteArrayOutputStream mermaidOut = new ByteArrayOutputStream();

            assertEquals(0, cli.run(new String[] { "ai", "spec" }, stream(specOut), stream(new ByteArrayOutputStream())));
            assertEquals(0, cli.run(new String[] { "state", "from-draft", "--in", draftFile.toString(), "--out", workbookFile.toString() },
                    stream(new ByteArrayOutputStream()), stream(new ByteArrayOutputStream())));
            assertEquals(0, cli.run(new String[] { "ai", "detect-kind", "--in", patchFile.toString() }, stream(kindOut), stream(new ByteArrayOutputStream())));
            assertEquals(0, cli.run(new String[] { "state", "validate", "--in", workbookFile.toString() }, stream(validateOut),
                    stream(new ByteArrayOutputStream())));
            assertEquals(0, cli.run(new String[] { "state", "apply-patch", "--state", workbookFile.toString(), "--in", patchFile.toString(),
                    "--out", nextWorkbookFile.toString() }, stream(new ByteArrayOutputStream()), stream(new ByteArrayOutputStream())));
            assertEquals(0, cli.run(new String[] { "state", "summarize", "--in", nextWorkbookFile.toString() }, stream(summaryOut),
                    stream(new ByteArrayOutputStream())));
            assertEquals(0, cli.run(new String[] { "state", "diff", "--before", workbookFile.toString(), "--after", nextWorkbookFile.toString() },
                    stream(diffOut), stream(new ByteArrayOutputStream())));
            assertEquals(0, cli.run(new String[] { "ai", "export", "project-overview", "--in", nextWorkbookFile.toString() }, stream(overviewOut),
                    stream(new ByteArrayOutputStream())));
            assertEquals(0, cli.run(new String[] { "ai", "export", "bundle", "--in", nextWorkbookFile.toString() }, stream(bundleOut),
                    stream(new ByteArrayOutputStream())));
            assertEquals(0, cli.run(new String[] { "ai", "export", "task-edit", "--in", nextWorkbookFile.toString(), "--task-uid", "2" },
                    stream(taskEditOut), stream(new ByteArrayOutputStream())));
            assertEquals(0, cli.run(new String[] { "ai", "export", "phase-detail", "--in", nextWorkbookFile.toString() }, stream(phaseOut),
                    stream(new ByteArrayOutputStream())));
            assertEquals(0, cli.run(new String[] { "export", "workbook-json", "--in", nextWorkbookFile.toString(), "--out", importedWorkbookFile.toString() },
                    stream(new ByteArrayOutputStream()), stream(new ByteArrayOutputStream())));
            assertEquals(0, cli.run(new String[] { "export", "xml", "--in", nextWorkbookFile.toString(), "--out", exportedXmlFile.toString() },
                    stream(new ByteArrayOutputStream()), stream(new ByteArrayOutputStream())));
            assertEquals(0, cli.run(new String[] { "export", "xlsx", "--in", nextWorkbookFile.toString(), "--out", structuralXlsxFile.toString() },
                    stream(new ByteArrayOutputStream()), stream(new ByteArrayOutputStream())));
            assertEquals(0, cli.run(new String[] { "validate", "xlsx", "--in", structuralXlsxFile.toString() }, stream(new ByteArrayOutputStream()),
                    stream(new ByteArrayOutputStream())));
            assertEquals(0, cli.run(new String[] { "import", "xlsx", "--in", structuralXlsxFile.toString(), "--out", importedWorkbookFile.toString() },
                    stream(new ByteArrayOutputStream()), stream(new ByteArrayOutputStream())));
            assertEquals(0, cli.run(new String[] { "merge", "xlsx", "--state", workbookFile.toString(), "--in", structuralXlsxFile.toString(),
                    "--out", importedWorkbookFile.toString() }, stream(new ByteArrayOutputStream()), stream(new ByteArrayOutputStream())));
            assertEquals(0, cli.run(new String[] { "report", "all", "--in", nextWorkbookFile.toString(), "--out", reportZipFile.toString() },
                    stream(new ByteArrayOutputStream()), stream(new ByteArrayOutputStream())));
            assertEquals(0, cli.run(new String[] { "report", "dir", "--in", nextWorkbookFile.toString(), "--out", reportDir.toString() },
                    stream(new ByteArrayOutputStream()), stream(new ByteArrayOutputStream())));
            assertEquals(0, cli.run(new String[] { "report", "wbs-xlsx", "--in", nextWorkbookFile.toString(), "--out", wbsXlsxFile.toString() },
                    stream(new ByteArrayOutputStream()), stream(new ByteArrayOutputStream())));
            assertEquals(0, cli.run(new String[] { "report", "wbs-markdown", "--in", nextWorkbookFile.toString() }, stream(markdownOut),
                    stream(new ByteArrayOutputStream())));
            assertEquals(0, cli.run(new String[] { "report", "mermaid", "--in", nextWorkbookFile.toString() }, stream(mermaidOut),
                    stream(new ByteArrayOutputStream())));

            assertTrue(text(specOut).contains("project_draft_view"));
            assertEquals("patch_json\n", text(kindOut));
            assertTrue(text(validateOut).contains("warnings="));
            assertTrue(compactJsonLayout(Files.readString(workbookFile, StandardCharsets.UTF_8)).contains("\"format\":\"mikuproject_workbook_json\""));
            assertTrue(Files.readString(nextWorkbookFile, StandardCharsets.UTF_8).contains("Patched Project"));
            assertTrue(compactJsonLayout(text(summaryOut)).contains("\"kind\":\"state_summary\""));
            assertTrue(compactJsonLayout(text(diffOut)).contains("\"kind\":\"state_diff_summary\""));
            assertTrue(compactJsonLayout(text(overviewOut)).contains("\"view_type\":\"project_overview_view\""));
            assertTrue(compactJsonLayout(text(bundleOut)).contains("\"view_type\":\"ai_projection_bundle\""));
            assertTrue(text(bundleOut).contains("\"project_overview_view\""));
            assertTrue(compactJsonLayout(text(bundleOut)).contains("\"phase_detail_views_full\":["));
            assertTrue(compactJsonLayout(text(bundleOut)).contains("\"task_edit_views_full\":["));
            assertTrue(compactJsonLayout(text(taskEditOut)).contains("\"view_type\":\"task_edit_view\""));
            assertTrue(compactJsonLayout(text(phaseOut)).contains("\"view_type\":\"phase_detail_view\""));
            assertTrue(Files.readString(exportedXmlFile, StandardCharsets.UTF_8).contains("Patched Project"));
            assertTrue(Files.size(reportZipFile) > 0);
            assertTrue(Files.exists(reportDir.resolve("wbs.xlsx")));
            assertTrue(Files.size(wbsXlsxFile) > 0);
            assertTrue(text(markdownOut).contains("Patched Project"));
            assertTrue(text(mermaidOut).contains("gantt"));
        } finally {
            Files.deleteIfExists(draftFile);
            Files.deleteIfExists(patchFile);
            Files.deleteIfExists(workbookFile);
            Files.deleteIfExists(nextWorkbookFile);
            Files.deleteIfExists(reportZipFile);
            deleteTree(reportDir);
            Files.deleteIfExists(wbsXlsxFile);
            Files.deleteIfExists(structuralXlsxFile);
            Files.deleteIfExists(importedWorkbookFile);
            Files.deleteIfExists(exportedXmlFile);
        }
    }

    @Test
    public void writesAiProjectionBundleToFile() throws IOException {
        MikuprojectCli cli = new MikuprojectCli();
        Path draftFile = Files.createTempFile("mikuproject-cli-bundle-draft", ".editjson");
        Path workbookFile = Files.createTempFile("mikuproject-cli-bundle-workbook", ".json");
        Path bundleFile = Files.createTempFile("mikuproject-cli-bundle", ".editjson");
        Files.write(draftFile, aiJsonText());
        try {
            assertEquals(0, cli.run(new String[] { "state", "from-draft", "--in", draftFile.toString(), "--out", workbookFile.toString() },
                    stream(new ByteArrayOutputStream()), stream(new ByteArrayOutputStream())));

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ByteArrayOutputStream err = new ByteArrayOutputStream();
            int exitCode = cli.run(new String[] { "ai", "export", "bundle", "--in", workbookFile.toString(), "--out", bundleFile.toString() },
                    stream(out), stream(err));

            String bundleText = Files.readString(bundleFile, StandardCharsets.UTF_8);
            assertEquals(0, exitCode);
            assertEquals("", text(out));
            assertEquals("", text(err));
            assertTrue(compactJsonLayout(bundleText).contains("\"view_type\":\"ai_projection_bundle\""));
            assertTrue(bundleText.contains("\"project_overview_view\""));
            assertTrue(compactJsonLayout(bundleText).contains("\"phase_detail_views_full\":["));
            assertTrue(compactJsonLayout(bundleText).contains("\"task_edit_views_full\":["));
        } finally {
            Files.deleteIfExists(draftFile);
            Files.deleteIfExists(workbookFile);
            Files.deleteIfExists(bundleFile);
        }
    }

    @Test
    public void usesNodeCompatibleBase64ForBinaryCliIo() throws IOException {
        MikuprojectCli cli = new MikuprojectCli();
        Path draftFile = Files.createTempFile("mikuproject-cli-binary-draft", ".editjson");
        Path workbookFile = Files.createTempFile("mikuproject-cli-binary-workbook", ".json");
        Path xlsxFile = Files.createTempFile("mikuproject-cli-binary", ".xlsx");
        Files.write(draftFile, aiJsonText());
        try {
            assertEquals(0, cli.run(new String[] { "state", "from-draft", "--in", draftFile.toString(), "--out", workbookFile.toString() },
                    stream(new ByteArrayOutputStream()), stream(new ByteArrayOutputStream())));
            assertEquals(0, cli.run(new String[] { "export", "xlsx", "--in", workbookFile.toString(), "--out", xlsxFile.toString() },
                    stream(new ByteArrayOutputStream()), stream(new ByteArrayOutputStream())));

            ByteArrayOutputStream exportOut = new ByteArrayOutputStream();
            ByteArrayOutputStream importOut = new ByteArrayOutputStream();
            ByteArrayOutputStream importErr = new ByteArrayOutputStream();
            ByteArrayOutputStream reportOut = new ByteArrayOutputStream();

            assertEquals(0, cli.run(new String[] { "export", "xlsx", "--in", workbookFile.toString(), "--out-base64", "-" },
                    stream(exportOut), stream(new ByteArrayOutputStream())));
            byte[] exportedBytes = Base64.getDecoder().decode(text(exportOut).trim());
            assertEquals("PK", new String(exportedBytes, 0, 2, StandardCharsets.UTF_8));

            InputStream originalIn = System.in;
            try {
                System.setIn(new ByteArrayInputStream((Base64.getEncoder().encodeToString(Files.readAllBytes(xlsxFile)) + "\n")
                        .getBytes(StandardCharsets.UTF_8)));
                assertEquals(0, cli.run(new String[] { "import", "xlsx", "--in-base64", "-", "--diagnostics", "json" },
                        stream(importOut), stream(importErr)));
            } finally {
                System.setIn(originalIn);
            }
            assertTrue(compactJsonLayout(text(importOut)).contains("\"format\":\"mikuproject_workbook_json\""));
            assertTrue(compactJsonLayout(text(importErr)).contains("\"command\":\"import xlsx\""));
            assertTrue(compactJsonLayout(text(importErr)).contains("\"mode\":\"stdin_base64\""));

            assertEquals(0, cli.run(new String[] { "report", "all", "--in", workbookFile.toString(), "--out-base64", "-" },
                    stream(reportOut), stream(new ByteArrayOutputStream())));
            byte[] reportBytes = Base64.getDecoder().decode(text(reportOut).trim());
            assertEquals("PK", new String(reportBytes, 0, 2, StandardCharsets.UTF_8));
        } finally {
            Files.deleteIfExists(draftFile);
            Files.deleteIfExists(workbookFile);
            Files.deleteIfExists(xlsxFile);
        }
    }

    @Test
    public void rejectsBinaryStdoutWithoutBase64Option() throws IOException {
        MikuprojectCli cli = new MikuprojectCli();
        Path draftFile = Files.createTempFile("mikuproject-cli-binary-reject-draft", ".editjson");
        Path workbookFile = Files.createTempFile("mikuproject-cli-binary-reject-workbook", ".json");
        Files.write(draftFile, aiJsonText());
        try {
            assertEquals(0, cli.run(new String[] { "state", "from-draft", "--in", draftFile.toString(), "--out", workbookFile.toString() },
                    stream(new ByteArrayOutputStream()), stream(new ByteArrayOutputStream())));

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ByteArrayOutputStream err = new ByteArrayOutputStream();
            int exitCode = cli.run(new String[] { "export", "xlsx", "--in", workbookFile.toString(), "--out", "-" },
                    stream(out), stream(err));

            assertEquals(2, exitCode);
            assertEquals("", text(out));
            assertTrue(text(err).contains("binary artifact"));
            assertTrue(text(err).contains("--out-base64 -"));
        } finally {
            Files.deleteIfExists(draftFile);
            Files.deleteIfExists(workbookFile);
        }
    }

    @Test
    public void exportsTaskEditWithNodeCompatibleSelectOptions() throws IOException {
        MikuprojectCli cli = new MikuprojectCli();
        MsProjectXml xml = new MsProjectXml();
        ProjectModel model = xml.importFromXml(readVendorTestdata("hierarchy.xml"));
        Path workbookFile = Files.createTempFile("mikuproject-cli-task-select", ".json");
        Files.write(workbookFile, exportWorkbookJsonText(model).getBytes(StandardCharsets.UTF_8));
        try {
            ByteArrayOutputStream defaultOut = new ByteArrayOutputStream();
            ByteArrayOutputStream firstTaskOut = new ByteArrayOutputStream();
            ByteArrayOutputStream missingUidErr = new ByteArrayOutputStream();

            assertEquals(0, cli.run(new String[] { "ai", "export", "task-edit", "--in", workbookFile.toString() },
                    stream(defaultOut), stream(new ByteArrayOutputStream())));
            assertEquals(0, cli.run(new String[] { "ai", "export", "task-edit", "--in", workbookFile.toString(),
                    "--select", "first-task" }, stream(firstTaskOut), stream(new ByteArrayOutputStream())));
            assertEquals(2, cli.run(new String[] { "ai", "export", "task-edit", "--in", workbookFile.toString(),
                    "--select", "uid" }, stream(new ByteArrayOutputStream()), stream(missingUidErr)));

            assertTrue(compactJsonLayout(text(defaultOut)).contains("\"view_type\":\"task_edit_view\""));
            assertTrue(compactJsonLayout(text(defaultOut)).contains("\"uid\":\"2\""));
            assertTrue(compactJsonLayout(text(firstTaskOut)).contains("\"uid\":\"2\""));
            assertTrue(text(missingUidErr).contains("--task-uid"));
        } finally {
            Files.deleteIfExists(workbookFile);
        }
    }

    @Test
    public void exportsScopedPhaseDetailWithRootTaskUidOption() throws IOException {
        MikuprojectCli cli = new MikuprojectCli();
        MsProjectXml xml = new MsProjectXml();
        ProjectModel model = xml.importFromXml(readVendorTestdata("hierarchy.xml"));
        Path workbookFile = Files.createTempFile("mikuproject-cli-phase-detail", ".json");
        Path phaseFile = Files.createTempFile("mikuproject-cli-phase-detail", ".editjson");
        Files.write(workbookFile, exportWorkbookJsonText(model).getBytes(StandardCharsets.UTF_8));
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ByteArrayOutputStream err = new ByteArrayOutputStream();

            int exitCode = cli.run(new String[] { "ai", "export", "phase-detail", "--in", workbookFile.toString(),
                    "--phase-uid", "1", "--mode", "scoped", "--root-task-uid", "2", "--max-depth", "1", "--out",
                    phaseFile.toString() }, stream(out), stream(err));

            String phaseText = Files.readString(phaseFile, StandardCharsets.UTF_8);
            assertEquals(0, exitCode);
            assertEquals("", text(out));
            assertEquals("", text(err));
            assertTrue(compactJsonLayout(phaseText).contains("\"view_type\":\"phase_detail_view\""));
            assertTrue(compactJsonLayout(phaseText).contains("\"mode\":\"scoped\""));
            assertTrue(compactJsonLayout(phaseText).contains("\"root_uid\":\"2\""));
            assertTrue(compactJsonLayout(phaseText).contains("\"max_depth\":1"));
        } finally {
            Files.deleteIfExists(workbookFile);
            Files.deleteIfExists(phaseFile);
        }
    }

    @Test
    public void exportsPhaseDetailWithNodeCompatibleSelectOptions() throws IOException {
        MikuprojectCli cli = new MikuprojectCli();
        MsProjectXml xml = new MsProjectXml();
        ProjectModel model = xml.importFromXml(readVendorTestdata("hierarchy.xml"));
        Path workbookFile = Files.createTempFile("mikuproject-cli-phase-select", ".json");
        Files.write(workbookFile, exportWorkbookJsonText(model).getBytes(StandardCharsets.UTF_8));
        try {
            ByteArrayOutputStream firstPhaseOut = new ByteArrayOutputStream();
            ByteArrayOutputStream missingUidErr = new ByteArrayOutputStream();

            assertEquals(0, cli.run(new String[] { "ai", "export", "phase-detail", "--in", workbookFile.toString(),
                    "--select", "first-phase" }, stream(firstPhaseOut), stream(new ByteArrayOutputStream())));
            assertEquals(2, cli.run(new String[] { "ai", "export", "phase-detail", "--in", workbookFile.toString(),
                    "--select", "uid" }, stream(new ByteArrayOutputStream()), stream(missingUidErr)));

            assertTrue(compactJsonLayout(text(firstPhaseOut)).contains("\"view_type\":\"phase_detail_view\""));
            assertTrue(compactJsonLayout(text(firstPhaseOut)).contains("\"mode\":\"scoped\""));
            assertTrue(compactJsonLayout(text(firstPhaseOut)).contains("\"uid\":\"1\""));
            assertTrue(text(missingUidErr).contains("--phase-uid"));
        } finally {
            Files.deleteIfExists(workbookFile);
        }
    }

    @Test
    public void writesAiExportBundleDiagnosticsToStderr() throws IOException {
        MikuprojectCli cli = new MikuprojectCli();
        Path draftFile = Files.createTempFile("mikuproject-cli-bundle-diag-draft", ".editjson");
        Path workbookFile = Files.createTempFile("mikuproject-cli-bundle-diag-workbook", ".json");
        Files.write(draftFile, aiJsonText());
        try {
            assertEquals(0, cli.run(new String[] { "state", "from-draft", "--in", draftFile.toString(), "--out", workbookFile.toString() },
                    stream(new ByteArrayOutputStream()), stream(new ByteArrayOutputStream())));

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ByteArrayOutputStream err = new ByteArrayOutputStream();
            int exitCode = cli.run(new String[] { "ai", "export", "bundle", "--in", workbookFile.toString(),
                    "--diagnostics", "json" }, stream(out), stream(err));

            assertEquals(0, exitCode);
            assertTrue(compactJsonLayout(text(out)).contains("\"view_type\":\"ai_projection_bundle\""));
            assertFalse(text(out).contains("\"diagnostics_version\""));
            assertTrue(compactJsonLayout(text(err)).contains("\"diagnostics_version\":1"));
            assertTrue(compactJsonLayout(text(err)).contains("\"command\":\"ai export bundle\""));
            assertTrue(compactJsonLayout(text(err)).contains("\"output_kind\":\"ai_projection_bundle\""));
            assertTrue(compactJsonLayout(text(err)).contains("\"phase_count\":1"));
            assertTrue(compactJsonLayout(text(err)).contains("\"task_count\":1"));
        } finally {
            Files.deleteIfExists(draftFile);
            Files.deleteIfExists(workbookFile);
        }
    }

    @Test
    public void validatesXmlThroughGroupedCommand() throws IOException {
        MikuprojectCli cli = new MikuprojectCli();
        Path xmlFile = Files.createTempFile("mikuproject-cli-xml", ".xml");
        Files.write(xmlFile, new MsProjectSamples().buildSampleXml().getBytes(StandardCharsets.UTF_8));
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ByteArrayOutputStream err = new ByteArrayOutputStream();

            int exitCode = cli.run(new String[] { "validate", "xml", "--in", xmlFile.toString() }, stream(out), stream(err));

            assertEquals(0, exitCode);
            assertTrue(text(out).contains("issues="));
            assertEquals("", text(err));
        } finally {
            Files.deleteIfExists(xmlFile);
        }
    }

    @Test
    public void rejectsLegacyFlatCommands() throws IOException {
        MikuprojectCli cli = new MikuprojectCli();

        assertUnknownCommand(cli, "validate-xml");
        assertUnknownCommand(cli, "export-ai-json-spec");
        assertUnknownCommand(cli, "export-xlsx");
        assertUnknownCommand(cli, "export-wbs-xlsx");
        assertUnknownCommand(cli, "export-report-bundle");
        assertUnknownCommand(cli, "export-xlsx-batch");
    }

    @Test
    public void returnsUsageErrorForMissingNamedOption() throws IOException {
        MikuprojectCli cli = new MikuprojectCli();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();

        int exitCode = cli.run(new String[] { "state", "apply-patch", "--in", "patch.editjson" }, stream(out), stream(err));

        assertEquals(2, exitCode);
        assertEquals("", text(out));
        assertTrue(text(err).contains("usage error:"));
        assertTrue(text(err).contains("state apply-patch requires --state"));
    }

    @Test
    public void returnsIoErrorForMissingInputFile() throws IOException {
        MikuprojectCli cli = new MikuprojectCli();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();

        int exitCode = cli.run(new String[] { "validate", "xml", "--in", "/tmp/does-not-exist-mikuproject.xml" }, stream(out), stream(err));

        assertEquals(1, exitCode);
        assertEquals("", text(out));
        assertTrue(text(err).contains("I/O error:"));
    }

    private void assertUnknownCommand(MikuprojectCli cli, String command) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();

        int exitCode = cli.run(new String[] { command }, stream(out), stream(err));

        assertEquals(2, exitCode);
        assertEquals("", text(out));
        assertTrue(text(err).contains("usage error:"));
        assertTrue(text(err).contains("unknown command: " + command));
        assertTrue(text(err).contains("ai spec"));
    }

    private PrintStream stream(ByteArrayOutputStream out) {
        return new PrintStream(out, true);
    }

    private String text(ByteArrayOutputStream out) {
        return new String(out.toByteArray(), StandardCharsets.UTF_8);
    }

    private String compactJsonLayout(String value) {
        return value.replaceAll("(?m)^\\s+", "").replace("\r", "").replace("\n", "").replace(": ", ":");
    }

    private List<String> extractUsageCommands(String usageText) {
        List<String> commands = new ArrayList<String>();
        String[] lines = usageText.split("\\R");
        for (String line : lines) {
            if (line.startsWith("  ")) {
                commands.add(line.substring(2));
            }
        }
        return commands;
    }

    private List<String> extractReadmeCliCommands(String readmeText) {
        List<String> commands = new ArrayList<String>();
        boolean inCliList = false;
        String[] lines = readmeText.split("\\R");
        for (String line : lines) {
            if ("Supported commands:".equals(line)) {
                inCliList = true;
                continue;
            }
            if (!inCliList) {
                continue;
            }
            if ("Notes:".equals(line)) {
                break;
            }
            if (line.startsWith("- `")) {
                commands.add(line.substring(3, line.length() - 1));
            }
        }
        return commands;
    }

    private byte[] aiJsonText() {
        StringBuilder builder = new StringBuilder();
        builder.append("{\"view_type\":\"project_draft_view\",");
        builder.append("\"project\":{\"name\":\"AI JSON Imported Project\",\"planned_start\":\"2026-04-01\"},");
        builder.append("\"tasks\":[");
        builder.append("{\"uid\":\"phase-1\",\"name\":\"Phase 1\",\"is_summary\":true,");
        builder.append("\"planned_start\":\"2026-04-01\",\"planned_finish\":\"2026-04-03\"},");
        builder.append("{\"uid\":\"t1\",\"name\":\"Draft Task 1\",\"parent_uid\":\"phase-1\",");
        builder.append("\"planned_start\":\"2026-04-01\",\"planned_finish\":\"2026-04-02\"}");
        builder.append("],");
        builder.append("\"resources\":[],");
        builder.append("\"assignments\":[]");
        builder.append("}");
        return builder.toString().getBytes(StandardCharsets.UTF_8);
    }

    private byte[] patchJsonText() {
        StringBuilder builder = new StringBuilder();
        builder.append("{\"operations\":[");
        builder.append("{\"op\":\"update_project\",\"fields\":{\"name\":\"Patched Project\"}},");
        builder.append("{\"op\":\"add_resource\",\"uid\":\"r-new\",\"name\":\"CLI Resource\"}");
        builder.append("]}");
        return builder.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String exportWorkbookJsonText(ProjectModel model) {
        WorkbookJsonDocument document = new ProjectWorkbookJson().exportProjectWorkbookJson(model);
        Map<String, Object> json = new LinkedHashMap<String, Object>();
        json.put("format", document.format);
        json.put("version", document.version);
        json.put("sheets", document.sheets);
        return new CoreApiAiJsonUtil().stringifyJson(json);
    }

    private String readVendorTestdata(String fileName) throws IOException {
        byte[] bytes = Files.readAllBytes(Paths.get("vendor", "mikuproject", "testdata", fileName));
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private void deleteTree(Path root) throws IOException {
        if (root == null || !Files.exists(root)) {
            return;
        }
        try (java.util.stream.Stream<Path> paths = Files.walk(root)) {
            List<Path> items = new ArrayList<Path>();
            paths.forEach(items::add);
            for (int index = items.size() - 1; index >= 0; index--) {
                Files.deleteIfExists(items.get(index));
            }
        }
    }
}
