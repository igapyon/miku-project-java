/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuproject.conformance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import jp.igapyon.mikuproject.coreapi.CoreApiAiJsonUtil;

public class ConformanceSuiteLoaderTest {
    private static final Path SNAPSHOT = Paths.get("vendor", "miku-project-contract", "v1.0.3");
    private static final String CONFORMANCE_DIRECTORY = "testdata/conformance/v1";
    private final CoreApiAiJsonUtil jsonUtil = new CoreApiAiJsonUtil();

    @Test
    public void loadsTheVerifiedV103CorpusDirectlyFromTheSnapshot() throws IOException {
        ConformanceSuiteLoader.ConformanceSuite suite = ConformanceSuiteLoader.load(SNAPSHOT);

        assertEquals(30, suite.workflowCases.size());
        assertEquals(31, suite.contractCases.size());
        assertEquals(33, suite.semanticFixtureIds.size());
        assertEquals("validate", suite.workflowCasesById.get("CV-VALID-001").command);
        assertEquals(0, suite.workflowCasesById.get("CV-VALID-001").expectedExitCode);
        assertEquals("json-schema", suite.contractCasesById.get("SC-DIFF-EMPTY-001").validationLayer);
        assertTrue(suite.workflowCasesById.get("CV-VALID-001").input.startsWith(SNAPSHOT.toAbsolutePath().normalize()));
        assertTrue(suite.artifactSchema.startsWith(SNAPSHOT.toAbsolutePath().normalize()));
        assertNotNull(suite.contractCasesById.get("BC-PLAN-BINDINGS-VALID-001"));
    }

    @Test
    public void rejectsAnUnknownIndexField() throws IOException {
        Path copied = copySnapshot();
        try {
            updateJson(copied.resolve(CONFORMANCE_DIRECTORY).resolve("suite-index.json"), new JsonUpdate() {
                @Override
                public void apply(Map<String, Object> document) {
                    document.put("unexpected", Boolean.TRUE);
                }
            });

            assertLoadFailure(copied, "conformance-suite.unknown-field");
        } finally {
            deleteTree(copied);
        }
    }

    @Test
    public void rejectsAnUnknownWorkflowParameterField() throws IOException {
        Path copied = copySnapshot();
        try {
            updateJson(copied.resolve(CONFORMANCE_DIRECTORY).resolve("suite-index.json"), new JsonUpdate() {
                @Override
                public void apply(Map<String, Object> document) {
                    workflowCase(document, "CV-VALID-001").parameters.put("unexpected", Boolean.TRUE);
                }
            });

            assertLoadFailure(copied, "conformance-suite.unknown-field");
        } finally {
            deleteTree(copied);
        }
    }

    @Test
    public void rejectsDuplicateWorkflowCaseIdentifiers() throws IOException {
        Path copied = copySnapshot();
        try {
            updateJson(copied.resolve(CONFORMANCE_DIRECTORY).resolve("suite-index.json"), new JsonUpdate() {
                @Override
                public void apply(Map<String, Object> document) {
                    workflowCase(document, "CV-INVALID-001").document.put("id", "CV-VALID-001");
                }
            });

            assertLoadFailure(copied, "conformance-suite.duplicate-case-id");
        } finally {
            deleteTree(copied);
        }
    }

    @Test
    public void rejectsDuplicateCaseIdentifiersAcrossTheTwoIndexes() throws IOException {
        Path copied = copySnapshot();
        try {
            updateJson(copied.resolve(CONFORMANCE_DIRECTORY).resolve("contract-cases.json"), new JsonUpdate() {
                @Override
                public void apply(Map<String, Object> document) {
                    contractCase(document, "SC-DIFF-EMPTY-001").put("id", "CV-VALID-001");
                }
            });

            assertLoadFailure(copied, "conformance-suite.duplicate-case-id");
        } finally {
            deleteTree(copied);
        }
    }

    @Test
    public void rejectsAReferenceThatEscapesTheSnapshotRoot() throws IOException {
        Path copied = copySnapshot();
        try {
            updateJson(copied.resolve(CONFORMANCE_DIRECTORY).resolve("suite-index.json"), new JsonUpdate() {
                @Override
                public void apply(Map<String, Object> document) {
                    workflowCase(document, "CV-VALID-001").document.put("input", "../../../../outside.xml");
                }
            });

            assertLoadFailure(copied, "conformance-suite.reference-escape");
        } finally {
            deleteTree(copied);
        }
    }

    @Test
    public void rejectsAnInSnapshotReferenceOutsideTheWorkflowAllowlist() throws IOException {
        Path copied = copySnapshot();
        try {
            updateJson(copied.resolve(CONFORMANCE_DIRECTORY).resolve("suite-index.json"), new JsonUpdate() {
                @Override
                public void apply(Map<String, Object> document) {
                    workflowCase(document, "CV-VALID-001").document.put("input",
                            "../../../docs/miku-project-cli-contract-v1.md");
                }
            });

            assertLoadFailure(copied, "conformance-suite.reference-not-allowed");
        } finally {
            deleteTree(copied);
        }
    }

    @Test
    public void copiedIndexTestsDoNotAlterTheTrackedSnapshot() throws IOException {
        ContractSnapshotVerifier.verify(SNAPSHOT);
        ConformanceSuiteLoader.ConformanceSuite suite = ConformanceSuiteLoader.load(SNAPSHOT);
        assertEquals("CV-VALID-001", suite.workflowCases.get(0).id);
    }

    private void assertLoadFailure(Path copied, String expectedCode) {
        ConformanceSuiteLoader.ConformanceSuiteLoadException error = assertThrows(
                ConformanceSuiteLoader.ConformanceSuiteLoadException.class,
                () -> ConformanceSuiteLoader.loadFromVerifiedSnapshotForTest(copied));
        assertEquals(expectedCode, error.code);
    }

    private WorkflowCaseMap workflowCase(Map<String, Object> document, String id) {
        for (Object raw : array(document.get("cases"))) {
            Map<String, Object> candidate = object(raw);
            if (id.equals(candidate.get("id"))) {
                return new WorkflowCaseMap(candidate, object(candidate.get("parameters")));
            }
        }
        throw new AssertionError("Missing workflow case: " + id);
    }

    private Map<String, Object> contractCase(Map<String, Object> document, String id) {
        for (Object raw : array(document.get("cases"))) {
            Map<String, Object> candidate = object(raw);
            if (id.equals(candidate.get("id"))) {
                return candidate;
            }
        }
        throw new AssertionError("Missing contract case: " + id);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> object(Object value) {
        return (Map<String, Object>) value;
    }

    @SuppressWarnings("unchecked")
    private List<Object> array(Object value) {
        return (List<Object>) value;
    }

    private void updateJson(Path file, JsonUpdate update) throws IOException {
        Map<String, Object> document = object(jsonUtil.parseJsonText(new String(Files.readAllBytes(file), StandardCharsets.UTF_8)));
        update.apply(document);
        Files.write(file, (jsonUtil.stringifyPrettyJson(document) + "\n").getBytes(StandardCharsets.UTF_8));
    }

    private Path copySnapshot() throws IOException {
        Path destination = Files.createTempDirectory("miku-project-conformance-suite-");
        try (Stream<Path> paths = Files.walk(SNAPSHOT)) {
            List<Path> sourcePaths = paths.collect(Collectors.toList());
            for (Path source : sourcePaths) {
                Path relative = SNAPSHOT.relativize(source);
                Path target = destination.resolve(relative.toString());
                if (Files.isDirectory(source)) {
                    Files.createDirectories(target);
                } else {
                    Files.copy(source, target);
                }
            }
        }
        return destination;
    }

    private void deleteTree(Path root) throws IOException {
        if (root == null || !Files.exists(root)) {
            return;
        }
        try (Stream<Path> paths = Files.walk(root)) {
            List<Path> entries = paths.sorted((left, right) -> right.compareTo(left)).collect(Collectors.toList());
            for (Path entry : entries) {
                Files.deleteIfExists(entry);
            }
        }
    }

    private interface JsonUpdate {
        void apply(Map<String, Object> document);
    }

    private static final class WorkflowCaseMap {
        private final Map<String, Object> document;
        private final Map<String, Object> parameters;

        private WorkflowCaseMap(Map<String, Object> document, Map<String, Object> parameters) {
            this.document = document;
            this.parameters = parameters;
        }
    }
}
