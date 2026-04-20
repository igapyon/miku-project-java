/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuproject.cli;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

public class MikuprojectCliDeterminismTest {
    @Test
    public void keepsCliOutputsDeterministicForSameInput() throws IOException {
        Path xmlFile = Files.createTempFile("mikuproject-cli-determinism", ".xml");
        Files.write(xmlFile, readVendorTestdata("dependency.xml"));
        Path hierarchyXmlFile = Files.createTempFile("mikuproject-cli-determinism-hierarchy", ".xml");
        Files.write(hierarchyXmlFile, readVendorTestdata("hierarchy.xml"));

        assertStdoutDeterministic("validate-xml", xmlFile.toString());
        assertStdoutDeterministic("export-mermaid", xmlFile.toString());
        assertStdoutDeterministic("export-wbs-markdown", xmlFile.toString());
        assertStdoutDeterministic("export-daily-svg", xmlFile.toString());
        assertStdoutDeterministic("export-weekly-svg", xmlFile.toString());
        assertStdoutDeterministic("export-workbook-json", xmlFile.toString());
        assertStdoutDeterministic("export-project-overview-view", xmlFile.toString());
        assertStdoutDeterministic("export-phase-detail-view", hierarchyXmlFile.toString(), "1", "scoped", "2", "1");
        assertStdoutDeterministic("export-task-edit-view", xmlFile.toString(), "2");
        assertStdoutDeterministic("export-project-draft-request", "Draft Request", "2026-04-01", "Goal", "3", "Plan,Build",
                "Kickoff");
        assertStdoutDeterministic("export-ai-json-spec");

        assertFileOutputDeterministic("export-monthly-svg-zip", ".zip", xmlFile.toString());
        assertFileOutputDeterministic("export-report-bundle", ".zip", xmlFile.toString());
        assertFileOutputDeterministic("export-xlsx", ".xlsx", xmlFile.toString());
        assertFileOutputDeterministic("export-wbs-xlsx", ".xlsx", xmlFile.toString());
        assertReportDirOutputDeterministic(xmlFile);
    }

    private void assertStdoutDeterministic(String... args) throws IOException {
        byte[] first = runStdout(args);
        byte[] second = runStdout(args);
        assertArrayEquals(first, second, args[0]);
    }

    private void assertFileOutputDeterministic(String command, String suffix, String inputFile) throws IOException {
        Path first = Files.createTempFile("mikuproject-determinism-first", suffix);
        Path second = Files.createTempFile("mikuproject-determinism-second", suffix);

        runFileCommand(command, inputFile, first);
        runFileCommand(command, inputFile, second);

        assertArrayEquals(Files.readAllBytes(first), Files.readAllBytes(second), command);
    }

    private void assertReportDirOutputDeterministic(Path inputFile) throws IOException {
        Path first = Files.createTempDirectory("mikuproject-determinism-report-first");
        Path second = Files.createTempDirectory("mikuproject-determinism-report-second");

        runFileCommand("export-report-dir", inputFile.toString(), first);
        runFileCommand("export-report-dir", inputFile.toString(), second);

        assertEquals(snapshotDirectory(first), snapshotDirectory(second));
    }

    private byte[] runStdout(String... args) throws IOException {
        MikuprojectCli cli = new MikuprojectCli();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();

        int exitCode = cli.run(args, stream(out), stream(err));

        assertEquals(0, exitCode, new String(err.toByteArray(), StandardCharsets.UTF_8));
        assertArrayEquals(new byte[0], err.toByteArray(), args[0]);
        return out.toByteArray();
    }

    private void runFileCommand(String command, String inputFile, Path outputPath) throws IOException {
        MikuprojectCli cli = new MikuprojectCli();
        ByteArrayOutputStream err = new ByteArrayOutputStream();

        int exitCode = cli.run(new String[] { command, inputFile, outputPath.toString() }, stream(new ByteArrayOutputStream()),
                stream(err));

        assertEquals(0, exitCode, new String(err.toByteArray(), StandardCharsets.UTF_8));
        assertArrayEquals(new byte[0], err.toByteArray(), command);
    }

    private Map<String, String> snapshotDirectory(Path root) throws IOException {
        Map<String, String> snapshot = new LinkedHashMap<String, String>();
        List<Path> files = new ArrayList<Path>();
        Stream<Path> stream = Files.walk(root);
        try {
            stream.filter(Files::isRegularFile).forEach(files::add);
        } finally {
            stream.close();
        }
        Collections.sort(files);
        for (Path file : files) {
            snapshot.put(root.relativize(file).toString().replace('\\', '/'), Base64.getEncoder().encodeToString(Files.readAllBytes(file)));
        }
        return snapshot;
    }

    private byte[] readVendorTestdata(String fileName) throws IOException {
        return Files.readAllBytes(Paths.get("vendor", "mikuproject", "testdata", fileName));
    }

    private PrintStream stream(ByteArrayOutputStream output) {
        try {
            return new PrintStream(output, true, StandardCharsets.UTF_8.name());
        } catch (java.io.UnsupportedEncodingException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
