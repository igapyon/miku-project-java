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
    public void keepsGroupedCliOutputsDeterministicForSameInput() throws IOException {
        Path draftFile = Files.createTempFile("mikuproject-cli-determinism-draft", ".editjson");
        Path workbookFile = Files.createTempFile("mikuproject-cli-determinism-workbook", ".json");
        Files.write(draftFile, draftJson());
        run(new String[] { "state", "from-draft", "--in", draftFile.toString(), "--out", workbookFile.toString() });

        assertStdoutDeterministic("ai", "spec");
        assertStdoutDeterministic("state", "validate", "--in", workbookFile.toString());
        assertStdoutDeterministic("state", "summarize", "--in", workbookFile.toString());
        assertStdoutDeterministic("report", "wbs-markdown", "--in", workbookFile.toString());
        assertStdoutDeterministic("report", "mermaid", "--in", workbookFile.toString());
        assertStdoutDeterministic("ai", "export", "project-overview", "--in", workbookFile.toString());
        assertStdoutDeterministic("ai", "export", "bundle", "--in", workbookFile.toString());

        assertFileOutputDeterministic(".zip", "report", "all", "--in", workbookFile.toString(), "--out");
        assertFileOutputDeterministic(".xlsx", "export", "xlsx", "--in", workbookFile.toString(), "--out");
        assertFileOutputDeterministic(".xlsx", "report", "wbs-xlsx", "--in", workbookFile.toString(), "--out");
        assertReportDirOutputDeterministic(workbookFile);
    }

    private void assertStdoutDeterministic(String... args) throws IOException {
        byte[] first = runStdout(args);
        byte[] second = runStdout(args);
        assertArrayEquals(first, second, args[0]);
    }

    private void assertFileOutputDeterministic(String suffix, String... commandPrefix) throws IOException {
        Path first = Files.createTempFile("mikuproject-determinism-first", suffix);
        Path second = Files.createTempFile("mikuproject-determinism-second", suffix);

        run(withOutput(commandPrefix, first));
        run(withOutput(commandPrefix, second));

        assertArrayEquals(Files.readAllBytes(first), Files.readAllBytes(second), commandPrefix[0] + " " + commandPrefix[1]);
    }

    private void assertReportDirOutputDeterministic(Path workbookFile) throws IOException {
        Path first = Files.createTempDirectory("mikuproject-determinism-report-first");
        Path second = Files.createTempDirectory("mikuproject-determinism-report-second");

        run(new String[] { "report", "dir", "--in", workbookFile.toString(), "--out", first.toString() });
        run(new String[] { "report", "dir", "--in", workbookFile.toString(), "--out", second.toString() });

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

    private void run(String[] args) throws IOException {
        MikuprojectCli cli = new MikuprojectCli();
        ByteArrayOutputStream err = new ByteArrayOutputStream();

        int exitCode = cli.run(args, stream(new ByteArrayOutputStream()), stream(err));

        assertEquals(0, exitCode, new String(err.toByteArray(), StandardCharsets.UTF_8));
        assertArrayEquals(new byte[0], err.toByteArray(), args[0]);
    }

    private String[] withOutput(String[] commandPrefix, Path outputPath) {
        String[] args = new String[commandPrefix.length + 1];
        System.arraycopy(commandPrefix, 0, args, 0, commandPrefix.length);
        args[args.length - 1] = outputPath.toString();
        return args;
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

    private byte[] draftJson() {
        StringBuilder builder = new StringBuilder();
        builder.append("{\"view_type\":\"project_draft_view\",");
        builder.append("\"project\":{\"name\":\"Determinism Project\",\"planned_start\":\"2026-04-01\"},");
        builder.append("\"tasks\":[");
        builder.append("{\"uid\":\"t1\",\"name\":\"Task 1\",\"planned_start\":\"2026-04-01\",\"planned_finish\":\"2026-04-02\"}");
        builder.append("],");
        builder.append("\"resources\":[],");
        builder.append("\"assignments\":[]");
        builder.append("}");
        return builder.toString().getBytes(StandardCharsets.UTF_8);
    }

    private PrintStream stream(ByteArrayOutputStream output) {
        try {
            return new PrintStream(output, true, StandardCharsets.UTF_8.name());
        } catch (java.io.UnsupportedEncodingException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
