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
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

public class MikuprojectNodeParityTest {
    @Test
    public void comparesReportDirectoryBytesWithNodeUpstreamWhenEnabled() throws IOException, InterruptedException {
        assumeNodeParityEnabled();

        Path xmlFile = Files.createTempFile("mikuproject-node-parity", ".xml");
        Files.write(xmlFile, Files.readAllBytes(Paths.get("vendor", "mikuproject", "testdata", "dependency.xml")));
        Path javaDir = Files.createTempDirectory("mikuproject-node-parity-java");
        Path nodeDir = Files.createTempDirectory("mikuproject-node-parity-node");

        MikuprojectCli cli = new MikuprojectCli();
        assertEquals(0, cli.run(new String[] { "export-report-dir", xmlFile.toString(), javaDir.toString() },
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
        Path javaZip = Files.createTempFile("mikuproject-node-parity-bundle-java", ".zip");
        Path nodeZip = Files.createTempFile("mikuproject-node-parity-bundle-node", ".zip");

        MikuprojectCli cli = new MikuprojectCli();
        assertEquals(0, cli.run(new String[] { "export-report-bundle", xmlFile.toString(), javaZip.toString() },
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
        Path javaZip = Files.createTempFile("mikuproject-node-parity-monthly-java", ".zip");
        Path nodeZip = Files.createTempFile("mikuproject-node-parity-monthly-node", ".zip");

        MikuprojectCli cli = new MikuprojectCli();
        assertEquals(0, cli.run(new String[] { "export-monthly-svg-zip", xmlFile.toString(), javaZip.toString() },
                new java.io.PrintStream(new java.io.ByteArrayOutputStream(), true, StandardCharsets.UTF_8.name()),
                new java.io.PrintStream(new java.io.ByteArrayOutputStream(), true, StandardCharsets.UTF_8.name())));

        runNode("src/test/node/export-upstream-monthly-svg-zip.mjs", xmlFile.toString(), nodeZip.toString());

        assertArrayEquals(Files.readAllBytes(nodeZip), Files.readAllBytes(javaZip), "monthly svg zip");
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
