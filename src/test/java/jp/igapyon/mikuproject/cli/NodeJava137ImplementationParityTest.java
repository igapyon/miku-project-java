/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuproject.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.ResourceLock;

import jp.igapyon.mikuproject.coreapi.CoreApiAiJsonUtil;

@ResourceLock("java.lang.System.in")
public class NodeJava137ImplementationParityTest {
    @TempDir
    Path directory;

    private final CoreApiAiJsonUtil json = new CoreApiAiJsonUtil();

    @Test
    public void comparesCurrentNodeAndJavaDepthBoundariesAndSubtreeMembership() throws Exception {
        CurrentNode currentNode = currentNode();
        Path state = writeDeepState();
        byte[] originalState = Files.readAllBytes(state);

        for (String maxDepth : Arrays.asList(null, "0", "100")) {
            Path javaOutput = directory.resolve("phase-java-" + (maxDepth == null ? "omitted" : maxDepth) + ".json");
            Path nodeOutput = directory.resolve("phase-node-" + (maxDepth == null ? "omitted" : maxDepth) + ".json");
            String[] javaArgs = phaseArgs(state, javaOutput, maxDepth);
            String[] nodeArgs = phaseArgs(state, nodeOutput, maxDepth);

            Observation java = runJava(javaArgs);
            Observation node = runCurrentNode(currentNode, nodeArgs);
            assertEquals(0, java.exitCode, "Java maxDepth=" + maxDepth + ": " + java.stderrText());
            assertEquals(0, node.exitCode, "Node maxDepth=" + maxDepth + ": " + node.stderrText());
            assertEquals("", java.stdoutText());
            assertEquals("", node.stdoutText());

            Map<String, Object> javaProjection = object(Files.readAllBytes(javaOutput));
            Map<String, Object> nodeProjection = object(Files.readAllBytes(nodeOutput));
            assertEquals(taskUids(nodeProjection), taskUids(javaProjection), "maxDepth=" + maxDepth);
            assertEquals(expectedUidCount(maxDepth), taskUids(javaProjection).size(), "maxDepth=" + maxDepth);
            assertEquals(maxDepth == null ? null : Integer.valueOf(maxDepth), scopeDepth(javaProjection));
            assertEquals(scopeDepth(nodeProjection), scopeDepth(javaProjection));
        }

        for (String maxDepth : Arrays.asList("101", repeat('9', 200))) {
            Path javaOutput = directory.resolve("invalid-java.json");
            Path nodeOutput = directory.resolve("invalid-node.json");
            Files.write(javaOutput, "java sentinel\n".getBytes(StandardCharsets.UTF_8));
            Files.write(nodeOutput, "node sentinel\n".getBytes(StandardCharsets.UTF_8));
            byte[] javaSentinel = Files.readAllBytes(javaOutput);
            byte[] nodeSentinel = Files.readAllBytes(nodeOutput);

            Observation java = runJava(phaseArgs(state, javaOutput, maxDepth));
            Observation node = runCurrentNode(currentNode, phaseArgs(state, nodeOutput, maxDepth));
            assertEquals(2, java.exitCode, "Java maxDepth=" + maxDepth);
            assertEquals(2, node.exitCode, "Node maxDepth=" + maxDepth);
            assertEquals("", java.stdoutText());
            assertEquals("", node.stdoutText());
            assertEquals("invalid_integer_option", object(java.stderr).get("error_code"));
            assertEquals("invalid_integer_option", object(node.stderr).get("error_code"));
            assertEquals("--max-depth", object(java.stderr).get("option"));
            assertEquals("--max-depth", object(node.stderr).get("error_details") instanceof Map<?, ?>
                    ? ((Map<?, ?>) object(node.stderr).get("error_details")).get("option") : null);
            assertTrue(Arrays.equals(javaSentinel, Files.readAllBytes(javaOutput)));
            assertTrue(Arrays.equals(nodeSentinel, Files.readAllBytes(nodeOutput)));
        }
        assertTrue(Arrays.equals(originalState, Files.readAllBytes(state)), "depth commands must not mutate input state");
    }

    @Test
    public void comparesCurrentNodeAndJavaUpdateTaskPublicApplyAndValidation() throws Exception {
        CurrentNode currentNode = currentNode();
        Path state = writeTaskState();
        byte[] originalState = Files.readAllBytes(state);
        Map<String, Object> fields = mapOf(
                "name", "Updated task",
                "notes", "  keep spaces  ",
                "calendar_uid", "1",
                "critical", Boolean.TRUE,
                "planned_start", "2026-04-01T09:30:00",
                "planned_finish", "2026-04-02T12:00:00",
                "planned_duration", "PT2H0M0S");
        Path patch = writeJson("update-task.patch.json", mapOf("operations", Arrays.asList(
                mapOf("op", "update_task", "uid", "2", "fields", fields))));
        byte[] originalPatch = Files.readAllBytes(patch);

        Path javaReportPath = directory.resolve("java-validation.json");
        Path nodeReportPath = directory.resolve("node-validation.json");
        Observation javaValidation = runJava(new String[] { "ai", "validate-patch", "--state", state.toString(), "--in",
                patch.toString(), "--diagnostics", "json", "--out", javaReportPath.toString() });
        Observation nodeValidation = runCurrentNode(currentNode, new String[] { "ai", "validate-patch", "--state",
                state.toString(), "--in", patch.toString(), "--diagnostics", "json", "--out", nodeReportPath.toString() });
        assertEquals(0, javaValidation.exitCode, javaValidation.stderrText());
        assertEquals(0, nodeValidation.exitCode, nodeValidation.stderrText());
        assertEquals("", javaValidation.stdoutText());
        assertEquals("", nodeValidation.stdoutText());
        Map<String, Object> javaReport = object(Files.readAllBytes(javaReportPath));
        Map<String, Object> nodeReport = object(Files.readAllBytes(nodeReportPath));
        assertEquals(Boolean.TRUE, javaReport.get("ok"));
        assertEquals(Boolean.TRUE, nodeReport.get("ok"));
        assertEquals(Integer.valueOf(0), javaReport.get("error_count"));
        assertEquals(Integer.valueOf(0), nodeReport.get("error_count"));
        assertEquals(Integer.valueOf(0), javaReport.get("warning_count"));
        assertEquals(Integer.valueOf(0), nodeReport.get("warning_count"));
        assertEquals(object(nodeReport.get("changes_summary")), object(javaReport.get("changes_summary")));

        Path javaOutput = directory.resolve("java-next.json");
        Path nodeOutput = directory.resolve("node-next.json");
        Observation javaApply = runJava(new String[] { "state", "apply-patch", "--state", state.toString(), "--in",
                patch.toString(), "--out", javaOutput.toString(), "--diagnostics", "json" });
        Observation nodeApply = runCurrentNode(currentNode, new String[] { "state", "apply-patch", "--state", state.toString(),
                "--in", patch.toString(), "--out", nodeOutput.toString(), "--diagnostics", "json" });
        assertEquals(0, javaApply.exitCode, javaApply.stderrText());
        assertEquals(0, nodeApply.exitCode, nodeApply.stderrText());
        assertEquals("", javaApply.stdoutText());
        assertEquals("", nodeApply.stdoutText());
        assertEquals(Integer.valueOf(0), object(javaApply.stderr).get("warning_count"));
        assertEquals(Integer.valueOf(0), object(nodeApply.stderr).get("warning_count"));
        assertEquals(object(object(nodeApply.stderr).get("changes_summary")),
                object(object(javaApply.stderr).get("changes_summary")));

        Map<String, Object> javaTask = taskRow(Files.readAllBytes(javaOutput), "2");
        Map<String, Object> nodeTask = taskRow(Files.readAllBytes(nodeOutput), "2");
        for (String key : Arrays.asList("Name", "Notes", "CalendarUID", "Critical", "Duration")) {
            assertEquals(nodeTask.get(key), javaTask.get(key), "task field " + key);
        }
        assertEquals("Updated task", javaTask.get("Name"));
        assertEquals("  keep spaces  ", javaTask.get("Notes"));
        assertEquals("1", String.valueOf(javaTask.get("CalendarUID")));
        assertEquals("2026-04-01 09:30:00", nodeTask.get("Start"));
        assertEquals("2026-04-01T09:30:00", javaTask.get("Start"));
        assertEquals("2026-04-02 12:00:00", nodeTask.get("Finish"));
        assertEquals("2026-04-02T12:00:00", javaTask.get("Finish"));
        assertEquals("PT2H0M0S", javaTask.get("Duration"));

        assertTrue(Arrays.equals(originalState, Files.readAllBytes(state)), "apply and validate must preserve state input bytes");
        assertTrue(Arrays.equals(originalPatch, Files.readAllBytes(patch)), "apply and validate must preserve patch input bytes");

        Path datePatch = writeJson("space-date.patch.json", mapOf("operations", Arrays.asList(
                mapOf("op", "update_task", "uid", "2", "fields", mapOf("planned_start", "2026-04-03 10:15:00")))));
        Path javaDateReport = directory.resolve("java-space-date-validation.json");
        Path nodeDateReport = directory.resolve("node-space-date-validation.json");
        Observation javaDate = runJava(new String[] { "ai", "validate-patch", "--state", state.toString(), "--in",
                datePatch.toString(), "--diagnostics", "json", "--out", javaDateReport.toString() });
        Observation nodeDate = runCurrentNode(currentNode, new String[] { "ai", "validate-patch", "--state", state.toString(),
                "--in", datePatch.toString(), "--diagnostics", "json", "--out", nodeDateReport.toString() });
        assertEquals(0, javaDate.exitCode);
        assertEquals(0, nodeDate.exitCode);
        assertEquals(Integer.valueOf(1), object(Files.readAllBytes(javaDateReport)).get("warning_count"));
        assertEquals(Integer.valueOf(0), object(Files.readAllBytes(nodeDateReport)).get("warning_count"));
        assertTrue(joinWarnings(object(Files.readAllBytes(javaDateReport))).contains("planned_start"));
        assertFalse(joinWarnings(object(Files.readAllBytes(nodeDateReport))).contains("planned_start"));
        System.out.println("I137-08 expected input-profile difference: Node accepts space datetime; Java task helper reports warning.");
    }

    @Test
    public void keepsUnknownOperationWarningAndContinuesToUpdateTask() throws Exception {
        CurrentNode currentNode = currentNode();
        Path state = writeTaskState();
        Path patch = writeJson("unknown-op.patch.json", mapOf("operations", Arrays.asList(
                mapOf("op", "unknown_future_operation"),
                mapOf("op", "update_task", "uid", "2", "fields", mapOf("name", "Continued update")))));
        Path javaOutput = directory.resolve("unknown-op-java.json");
        Path nodeOutput = directory.resolve("unknown-op-node.json");
        Observation java = runJava(new String[] { "state", "apply-patch", "--state", state.toString(), "--in", patch.toString(),
                "--out", javaOutput.toString(), "--diagnostics", "json" });
        Observation node = runCurrentNode(currentNode, new String[] { "state", "apply-patch", "--state", state.toString(),
                "--in", patch.toString(), "--out", nodeOutput.toString(), "--diagnostics", "json" });

        assertEquals(0, java.exitCode);
        assertEquals(0, node.exitCode);
        assertEquals(Integer.valueOf(1), object(java.stderr).get("warning_count"));
        assertEquals(Integer.valueOf(1), object(node.stderr).get("warning_count"));
        assertTrue(joinWarnings(object(java.stderr)).contains("unknown_future_operation"));
        assertTrue(joinWarnings(object(node.stderr)).contains("unknown_future_operation"));
        assertEquals("Continued update", taskRow(Files.readAllBytes(javaOutput), "2").get("Name"));
        assertEquals("Continued update", taskRow(Files.readAllBytes(nodeOutput), "2").get("Name"));
    }

    @Test
    public void comparesPublicWarningsForMissingUidUnknownUidInvalidAndEmptyFields() throws Exception {
        CurrentNode currentNode = currentNode();
        Path state = writeTaskState();
        Path patch = writeJson("invalid-update-task.patch.json", mapOf("operations", Arrays.asList(
                mapOf("op", "update_task", "fields", mapOf("name", "missing uid")),
                mapOf("op", "update_task", "uid", "404", "fields", mapOf("name", "unknown uid")),
                mapOf("op", "update_task", "uid", "2", "fields", Arrays.asList("not", "an", "object")),
                mapOf("op", "update_task", "uid", "2", "fields", mapOf()),
                mapOf("op", "update_task", "uid", "2", "fields", mapOf("name", "After warnings")))));
        Path javaOutput = directory.resolve("invalid-update-java.json");
        Path nodeOutput = directory.resolve("invalid-update-node.json");
        Observation java = runJava(new String[] { "state", "apply-patch", "--state", state.toString(), "--in", patch.toString(),
                "--out", javaOutput.toString(), "--diagnostics", "json" });
        Observation node = runCurrentNode(currentNode, new String[] { "state", "apply-patch", "--state", state.toString(),
                "--in", patch.toString(), "--out", nodeOutput.toString(), "--diagnostics", "json" });

        assertEquals(0, java.exitCode);
        assertEquals(0, node.exitCode);
        Map<String, Object> javaDiagnostics = object(java.stderr);
        Map<String, Object> nodeDiagnostics = object(node.stderr);
        assertEquals(Integer.valueOf(4), javaDiagnostics.get("warning_count"));
        assertEquals(Integer.valueOf(4), nodeDiagnostics.get("warning_count"));
        assertEquals(warningMessages(nodeDiagnostics), warningMessages(javaDiagnostics));
        assertEquals(Integer.valueOf(1), object(object(javaDiagnostics.get("changes_summary"))).get("total_changes"));
        assertEquals("After warnings", taskRow(Files.readAllBytes(javaOutput), "2").get("Name"));
        assertEquals("After warnings", taskRow(Files.readAllBytes(nodeOutput), "2").get("Name"));
    }

    private CurrentNode currentNode() throws Exception {
        String nodeCliValue = System.getProperty("miku.project.current.node.cli");
        String nodeBinaryValue = System.getProperty("miku.project.node.binary");
        if (nodeCliValue == null && nodeBinaryValue == null) {
            assumeTrue(false, "set miku.project.current.node.cli and miku.project.node.binary for current Node parity");
        }
        assertNotNull(nodeCliValue, "miku.project.current.node.cli must be set when current Node parity is enabled");
        assertNotNull(nodeBinaryValue, "miku.project.node.binary must be set when current Node parity is enabled");
        Path nodeCli = Paths.get(nodeCliValue).toAbsolutePath().normalize();
        Path nodeBinary = Paths.get(nodeBinaryValue).toAbsolutePath().normalize();
        assertTrue(Files.isRegularFile(nodeCli), "current Node CLI does not exist: " + nodeCli);
        assertTrue(Files.isRegularFile(nodeBinary) && Files.isExecutable(nodeBinary), "Node binary does not exist or is not executable: " + nodeBinary);
        String digest = sha256(Files.readAllBytes(nodeCli));
        Process version = new ProcessBuilder(nodeBinary.toString(), "--version").redirectErrorStream(true).start();
        byte[] versionText = readProcess(version);
        System.out.println("I137 current Node CLI=" + nodeCli + " sha256=" + digest + " version="
                + new String(versionText, StandardCharsets.UTF_8).trim());
        return new CurrentNode(nodeBinary, nodeCli);
    }

    private Observation runJava(String[] args) throws IOException {
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        try (PrintStream out = new PrintStream(stdout, true, StandardCharsets.UTF_8.name());
                PrintStream err = new PrintStream(stderr, true, StandardCharsets.UTF_8.name())) {
            int exitCode = new MikuprojectCli().run(args, out, err);
            return new Observation(exitCode, stdout.toByteArray(), stderr.toByteArray());
        }
    }

    private Observation runCurrentNode(CurrentNode currentNode, String[] args) throws IOException, InterruptedException {
        Path stdoutPath = Files.createTempFile(directory, "node-stdout-", ".bin");
        Path stderrPath = Files.createTempFile(directory, "node-stderr-", ".bin");
        List<String> command = new ArrayList<String>();
        command.add(currentNode.binary.toString());
        command.add(currentNode.cli.toString());
        command.addAll(Arrays.asList(args));
        Process process = new ProcessBuilder(command).directory(directory.toFile())
                .redirectOutput(stdoutPath.toFile()).redirectError(stderrPath.toFile()).start();
        try {
            process.getOutputStream().close();
            if (!process.waitFor(30, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                throw new IOException("current Node CLI timed out: " + command);
            }
            return new Observation(process.exitValue(), Files.readAllBytes(stdoutPath), Files.readAllBytes(stderrPath));
        } finally {
            Files.deleteIfExists(stdoutPath);
            Files.deleteIfExists(stderrPath);
        }
    }

    private Path writeDeepState() throws IOException {
        Map<String, Object> state = object(NodeJavaLegacyCases.fixture("BASE.json"));
        Map<String, Object> sheets = object(state.get("sheets"));
        Map<String, Object> phase = taskRowTemplate(sheets, "1");
        phase.put("UID", "1");
        phase.put("ID", Integer.valueOf(1));
        phase.put("Name", "Phase");
        phase.put("OutlineLevel", Integer.valueOf(1));
        phase.put("OutlineNumber", "1");
        phase.put("WBS", "1");
        phase.put("Summary", Boolean.TRUE);
        Map<String, Object> root = taskRowTemplate(sheets, "2");
        root.put("UID", "2");
        root.put("ID", Integer.valueOf(2));
        root.put("Name", "Root");
        root.put("OutlineLevel", Integer.valueOf(2));
        root.put("OutlineNumber", "1.1");
        root.put("WBS", "1.1");
        root.put("Summary", Boolean.FALSE);
        List<Object> tasks = new ArrayList<Object>();
        tasks.add(phase);
        tasks.add(root);
        for (int index = 0; index < 101; index++) {
            Map<String, Object> child = new LinkedHashMap<String, Object>(root);
            child.put("UID", String.valueOf(index + 3));
            child.put("ID", Integer.valueOf(index + 3));
            child.put("Name", "Child " + (index + 1));
            child.put("OutlineLevel", Integer.valueOf(index + 3));
            String outlineNumber = "1.1" + repeat(".1", index + 1);
            child.put("OutlineNumber", outlineNumber);
            child.put("WBS", outlineNumber);
            child.put("Summary", Boolean.FALSE);
            tasks.add(child);
        }
        Map<String, Object> sibling = new LinkedHashMap<String, Object>(root);
        sibling.put("UID", "999");
        sibling.put("ID", Integer.valueOf(999));
        sibling.put("Name", "Sibling");
        sibling.put("OutlineNumber", "1.2");
        sibling.put("WBS", "1.2");
        sibling.put("OutlineLevel", Integer.valueOf(2));
        sibling.put("Summary", Boolean.FALSE);
        tasks.add(sibling);
        sheets.put("Tasks", tasks);
        return writeJson("deep-state.json", state);
    }

    private Path writeTaskState() throws IOException {
        Map<String, Object> state = object(NodeJavaLegacyCases.fixture("BASE.json"));
        Map<String, Object> sheets = object(state.get("sheets"));
        Map<String, Object> task = taskRowTemplate(sheets, "2");
        task.put("Name", "Second");
        task.put("Start", "2026-03-20T09:00:00");
        task.put("Finish", "2026-03-20T17:00:00");
        task.put("Duration", "PT8H0M0S");
        task.put("Critical", Boolean.FALSE);
        task.put("Notes", "Remove me");
        sheets.put("Tasks", Arrays.asList(taskRowTemplate(sheets, "1"), task));
        return writeJson("task-state.json", state);
    }

    private Map<String, Object> taskRowTemplate(Map<String, Object> sheets, String uid) {
        for (Object value : (List<?>) sheets.get("Tasks")) {
            Map<String, Object> task = object(value);
            if (uid.equals(String.valueOf(task.get("UID")))) return new LinkedHashMap<String, Object>(task);
        }
        throw new IllegalArgumentException("missing task " + uid);
    }

    private String[] phaseArgs(Path state, Path output, String maxDepth) {
        List<String> args = new ArrayList<String>(Arrays.asList("ai", "export", "phase-detail", "--in", state.toString(),
                "--phase-uid", "1", "--mode", "scoped", "--root-task-uid", "2"));
        if (maxDepth != null) {
            args.add("--max-depth");
            args.add(maxDepth);
        }
        args.addAll(Arrays.asList("--out", output.toString(), "--diagnostics", "json"));
        return args.toArray(new String[args.size()]);
    }

    private int expectedUidCount(String maxDepth) {
        if (maxDepth == null) return 102;
        return "0".equals(maxDepth) ? 1 : 101;
    }

    private List<String> taskUids(Map<String, Object> projection) {
        List<String> uids = new ArrayList<String>();
        for (Object value : (List<?>) projection.get("tasks")) {
            uids.add(String.valueOf(object(value).get("uid")));
        }
        return uids;
    }

    private Object scopeDepth(Map<String, Object> projection) {
        return object(projection.get("scope")).get("max_depth");
    }

    private Map<String, Object> taskRow(byte[] workbookBytes, String uid) {
        Map<String, Object> workbook = object(workbookBytes);
        Map<String, Object> sheets = object(workbook.get("sheets"));
        for (Object value : (List<?>) sheets.get("Tasks")) {
            Map<String, Object> task = object(value);
            if (uid.equals(String.valueOf(task.get("UID")))) return task;
        }
        throw new AssertionError("missing output task " + uid);
    }

    private String joinWarnings(Map<String, Object> report) {
        StringBuilder builder = new StringBuilder();
        Object warnings = report.get("warnings");
        if (warnings instanceof List<?>) {
            for (Object value : (List<?>) warnings) {
                Map<String, Object> warning = object(value);
                if (builder.length() > 0) builder.append('\n');
                builder.append(warning.get("message"));
            }
        }
        return builder.toString();
    }

    private List<String> warningMessages(Map<String, Object> report) {
        List<String> messages = new ArrayList<String>();
        Object warnings = report.get("warnings");
        if (warnings instanceof List<?>) {
            for (Object value : (List<?>) warnings) messages.add(String.valueOf(object(value).get("message")));
        }
        return messages;
    }

    private Path writeJson(String fileName, Object value) throws IOException {
        Path path = directory.resolve(fileName);
        Files.write(path, (json.stringifyJson(value) + "\n").getBytes(StandardCharsets.UTF_8));
        return path;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> object(Object value) {
        return (Map<String, Object>) value;
    }

    private Map<String, Object> object(byte[] bytes) {
        return object(json.parseJsonText(new String(bytes, StandardCharsets.UTF_8)));
    }

    private Map<String, Object> mapOf(Object... entries) {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        for (int index = 0; index < entries.length; index += 2) map.put(String.valueOf(entries[index]), entries[index + 1]);
        return map;
    }

    private String repeat(char value, int count) {
        char[] result = new char[count];
        Arrays.fill(result, value);
        return new String(result);
    }

    private String repeat(String value, int count) {
        StringBuilder result = new StringBuilder();
        for (int index = 0; index < count; index++) result.append(value);
        return result.toString();
    }

    private String sha256(byte[] bytes) throws NoSuchAlgorithmException {
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes);
        StringBuilder result = new StringBuilder();
        for (byte value : digest) result.append(String.format("%02x", Byte.valueOf(value)));
        return result.toString();
    }

    private byte[] readProcess(Process process) throws IOException, InterruptedException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        while (true) {
            int length = process.getInputStream().read(buffer);
            if (length < 0) break;
            output.write(buffer, 0, length);
        }
        if (!process.waitFor(10, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            throw new IOException("process timed out");
        }
        assertEquals(0, process.exitValue(), "Node --version");
        return output.toByteArray();
    }

    private static final class CurrentNode {
        final Path binary;
        final Path cli;

        CurrentNode(Path binary, Path cli) {
            this.binary = binary;
            this.cli = cli;
        }
    }

    private static final class Observation {
        final int exitCode;
        final byte[] stdout;
        final byte[] stderr;

        Observation(int exitCode, byte[] stdout, byte[] stderr) {
            this.exitCode = exitCode;
            this.stdout = stdout;
            this.stderr = stderr;
        }

        String stdoutText() {
            return new String(stdout, StandardCharsets.UTF_8);
        }

        String stderrText() {
            return new String(stderr, StandardCharsets.UTF_8);
        }
    }
}
