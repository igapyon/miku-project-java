/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuproject.cli;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;

public class NodeJavaLegacyCasesTest {
    @Test
    @ResourceLock("java.lang.System.in")
    public void capturesRepeatableStateSummaryAndArtifactBytesForBothRuntimes() throws IOException, InterruptedException {
        assumeNodeParityEnabled();
        Path directory = Files.createTempDirectory("miku-project-node-java-137-cases-");
        try {
            Path state = NodeJavaLegacyCases.writeHierarchyState(directory);
            Path javaOutput = directory.resolve("java-summary.json");
            Path nodeOutput = directory.resolve("node-summary.json");
            String[] javaArgs = { "state", "summarize", "--in", state.toString(), "--out", javaOutput.toString(), "--diagnostics", "json" };
            String[] nodeArgs = { "state", "summarize", "--in", state.toString(), "--out", nodeOutput.toString(), "--diagnostics", "json" };

            NodeJavaLegacyCases.Observation javaFirst = NodeJavaLegacyCases.runJava(javaArgs, new byte[0], javaOutput);
            NodeJavaLegacyCases.Observation javaSecond = NodeJavaLegacyCases.runJava(javaArgs, new byte[0], javaOutput);
            NodeJavaLegacyCases.Observation nodeFirst = NodeJavaLegacyCases.runNode(nodeArgs, new byte[0], nodeOutput);
            NodeJavaLegacyCases.Observation nodeSecond = NodeJavaLegacyCases.runNode(nodeArgs, new byte[0], nodeOutput);

            assertEquals(0, javaFirst.exitCode);
            assertEquals(0, nodeFirst.exitCode);
            assertSameObservation(javaFirst, javaSecond);
            assertSameObservation(nodeFirst, nodeSecond);
            assertArrayEquals(Files.readAllBytes(javaOutput), javaFirst.artifacts.get("java-summary.json"));
            assertArrayEquals(Files.readAllBytes(nodeOutput), nodeFirst.artifacts.get("node-summary.json"));
        } finally {
            deleteTree(directory);
        }
    }

    private void assumeNodeParityEnabled() {
        assumeTrue("true".equalsIgnoreCase(System.getenv("MIKUPROJECT_RUN_NODE_PARITY")),
                "set MIKUPROJECT_RUN_NODE_PARITY=true to run Node upstream parity");
        assumeTrue(Files.isDirectory(Paths.get("vendor", "mikuproject", "node_modules")),
                "run npm --prefix vendor/mikuproject ci before Node upstream parity");
    }

    private void assertSameObservation(NodeJavaLegacyCases.Observation expected, NodeJavaLegacyCases.Observation actual) {
        assertEquals(expected.exitCode, actual.exitCode, "exit code");
        assertArrayEquals(expected.stdout, actual.stdout, "stdout");
        assertArrayEquals(expected.stderr, actual.stderr, "stderr");
        assertIterableEquals(expected.artifacts.keySet(), actual.artifacts.keySet(), "artifact names");
        for (Map.Entry<String, byte[]> artifact : expected.artifacts.entrySet()) {
            assertArrayEquals(artifact.getValue(), actual.artifacts.get(artifact.getKey()), artifact.getKey());
        }
    }

    private void deleteTree(Path root) throws IOException {
        Stream<Path> paths = Files.walk(root);
        try {
            Path[] ordered = paths.sorted((left, right) -> right.getNameCount() - left.getNameCount()).toArray(Path[]::new);
            for (Path path : ordered) {
                Files.deleteIfExists(path);
            }
        } finally {
            paths.close();
        }
    }
}
