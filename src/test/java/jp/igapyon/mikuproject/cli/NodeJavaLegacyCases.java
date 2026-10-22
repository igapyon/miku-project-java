/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuproject.cli;

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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import jp.igapyon.mikuproject.coreapi.CoreApiAiJsonUtil;
import jp.igapyon.mikuproject.model.ProjectModel;
import jp.igapyon.mikuproject.msprojectxml.MsProjectXml;
import jp.igapyon.mikuproject.projectworkbookjson.ProjectWorkbookJson;
import jp.igapyon.mikuproject.projectworkbookjson.WorkbookJsonDocument;

/** Test-side, byte-preserving observation helper for legacy Node/Java CLI cases. */
final class NodeJavaLegacyCases {
    private static final Path NODE_CLI = Paths.get("vendor", "mikuproject", "scripts", "miku-project-cli.mjs");
    private static final Path HIERARCHY_XML = Paths.get("vendor", "mikuproject", "testdata", "hierarchy.xml");

    private NodeJavaLegacyCases() {
    }

    static byte[] fixture(String name) throws IOException {
        String resource = "/node-java-137/" + name;
        InputStream input = NodeJavaLegacyCases.class.getResourceAsStream(resource);
        if (input == null) {
            throw new IOException("missing legacy fixture resource: " + resource);
        }
        try {
            return readAll(input);
        } finally {
            input.close();
        }
    }

    static Path writeHierarchyState(Path directory) throws IOException {
        String xmlText = new String(Files.readAllBytes(HIERARCHY_XML), StandardCharsets.UTF_8);
        ProjectModel model = new MsProjectXml().importFromXml(xmlText);
        WorkbookJsonDocument document = new ProjectWorkbookJson().exportProjectWorkbookJson(model);
        Map<String, Object> json = new LinkedHashMap<String, Object>();
        json.put("format", document.format);
        json.put("version", document.version);
        json.put("sheets", document.sheets);
        String text = new CoreApiAiJsonUtil().stringifyJson(json) + "\n";
        Path state = directory.resolve("STATE.json");
        Files.write(state, text.getBytes(StandardCharsets.UTF_8));
        return state;
    }

    static Observation runJava(String[] args, byte[] stdin, Path... artifacts) throws IOException {
        InputStream originalStdin = System.in;
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        int exitCode;
        try (PrintStream out = new PrintStream(stdout, true, StandardCharsets.UTF_8.name());
                PrintStream err = new PrintStream(stderr, true, StandardCharsets.UTF_8.name())) {
            System.setIn(new ByteArrayInputStream(stdin == null ? new byte[0] : stdin));
            exitCode = new MikuprojectCli().run(args, out, err);
        } finally {
            System.setIn(originalStdin);
        }
        return new Observation(exitCode, stdout.toByteArray(), stderr.toByteArray(), captureArtifacts(artifacts));
    }

    static Observation runNode(String[] args, byte[] stdin, Path... artifacts) throws IOException, InterruptedException {
        Path capture = Files.createTempDirectory("miku-project-node-java-137-");
        try {
            Path stdoutPath = capture.resolve("stdout.bin");
            Path stderrPath = capture.resolve("stderr.bin");
            List<String> command = new ArrayList<String>();
            command.add(System.getProperty("miku.project.node.binary", "node"));
            command.add(NODE_CLI.toString());
            for (String arg : args) {
                command.add(arg);
            }
            Process process = new ProcessBuilder(command)
                    .directory(Paths.get(".").toAbsolutePath().toFile())
                    .redirectOutput(stdoutPath.toFile())
                    .redirectError(stderrPath.toFile())
                    .start();
            try {
                process.getOutputStream().write(stdin == null ? new byte[0] : stdin);
                process.getOutputStream().close();
                if (!process.waitFor(10, TimeUnit.SECONDS)) {
                    process.destroyForcibly();
                    process.waitFor(1, TimeUnit.SECONDS);
                    throw new IOException("Node CLI timed out after 10 seconds: " + command);
                }
                return new Observation(process.exitValue(), Files.readAllBytes(stdoutPath), Files.readAllBytes(stderrPath),
                        captureArtifacts(artifacts));
            } catch (InterruptedException ex) {
                process.destroyForcibly();
                Thread.currentThread().interrupt();
                throw ex;
            }
        } finally {
            deleteTree(capture);
        }
    }

    private static Map<String, byte[]> captureArtifacts(Path... paths) throws IOException {
        Map<String, byte[]> result = new LinkedHashMap<String, byte[]>();
        for (Path path : paths) {
            if (!Files.isRegularFile(path)) {
                throw new IOException("expected CLI artifact was not created: " + path);
            }
            result.put(path.getFileName().toString(), Files.readAllBytes(path));
        }
        return result;
    }

    private static byte[] readAll(InputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int read;
        while ((read = input.read(buffer)) >= 0) {
            output.write(buffer, 0, read);
        }
        return output.toByteArray();
    }

    private static void deleteTree(Path root) throws IOException {
        if (!Files.exists(root)) {
            return;
        }
        Stream<Path> paths = Files.walk(root);
        try {
            paths.sorted((left, right) -> right.getNameCount() - left.getNameCount()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException ex) {
                    throw new DeleteFailure(ex);
                }
            });
        } catch (DeleteFailure ex) {
            throw ex.cause;
        } finally {
            paths.close();
        }
    }

    static final class Observation {
        final int exitCode;
        final byte[] stdout;
        final byte[] stderr;
        final Map<String, byte[]> artifacts;

        Observation(int exitCode, byte[] stdout, byte[] stderr, Map<String, byte[]> artifacts) {
            this.exitCode = exitCode;
            this.stdout = stdout.clone();
            this.stderr = stderr.clone();
            this.artifacts = new LinkedHashMap<String, byte[]>(artifacts);
        }
    }

    private static final class DeleteFailure extends RuntimeException {
        private static final long serialVersionUID = 1L;
        final IOException cause;

        DeleteFailure(IOException cause) {
            super(cause);
            this.cause = cause;
        }
    }
}
