/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuproject.cli;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import jp.igapyon.mikuproject.coreapi.CoreApiAiJsonUtil;
import jp.igapyon.mikuproject.model.ProjectModel;
import jp.igapyon.mikuproject.msprojectxml.MsProjectXml;
import jp.igapyon.mikuproject.projectworkbookjson.ProjectWorkbookJson;

public class MikuprojectNodeParityTest {
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
            assertArrayEquals(Files.readAllBytes(javaDir.resolve(name)), Files.readAllBytes(nodeDir.resolve(name)), name);
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

        assertArrayEquals(Files.readAllBytes(nodeZip), Files.readAllBytes(javaZip), "report bundle zip");
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

        assertArrayEquals(Files.readAllBytes(nodeZip), Files.readAllBytes(javaZip), "monthly svg zip");
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

    private void assumeNodeParityEnabled() {
        assumeTrue("true".equalsIgnoreCase(System.getenv("MIKUPROJECT_RUN_NODE_PARITY")),
                "set MIKUPROJECT_RUN_NODE_PARITY=true to run Node upstream parity");
        assumeTrue(Files.isDirectory(Paths.get("vendor", "mikuproject", "node_modules")),
                "run npm --prefix vendor/mikuproject ci before Node upstream parity");
    }

    private void runNode(String script, String input, String output) throws IOException, InterruptedException {
        Process process = new ProcessBuilder("node", script, input, output)
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
        command.add("vendor/mikuproject/scripts/mikuproject-cli.mjs");
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
