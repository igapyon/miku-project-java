/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuproject.conformance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import jp.igapyon.mikuproject.coreapi.CoreApiAiJsonUtil;

public class ConformanceSchemaRegistryTest {
    private static final Path SNAPSHOT = Paths.get("vendor", "miku-project-contract", "v1.0.3");
    private final CoreApiAiJsonUtil jsonUtil = new CoreApiAiJsonUtil();

    @Test
    public void loadsExactlyTheFourPinnedSchemasFromTheVerifiedSnapshot() throws IOException {
        ConformanceSchemaRegistry registry = ConformanceSchemaRegistry.load(SNAPSHOT);

        assertEquals(4, registry.schemaIds().size());
        assertTrue(registry.schemaIds().contains("urn:miku-project:schema:artifacts:v1"));
        assertTrue(registry.schemaIds().contains("urn:miku-project:schema:cli-diagnostic:v1"));
        assertTrue(registry.schemaIds().contains("urn:miku-project:schema:cli-result:v1"));
        assertTrue(registry.schemaIds().contains("urn:miku-project:schema:runtime-manifest:v1"));
    }

    @Test
    public void acceptsAllCheckedInPositiveExamplesForEachRegisteredSchema() throws IOException {
        ConformanceSchemaRegistry registry = ConformanceSchemaRegistry.load(SNAPSHOT);

        for (Path file : jsonFiles("docs/examples/artifacts-v1")) {
            assertValid(file, registry.validateArtifact(registry.parseJson(file)));
        }
        int diagnosticCount = 0;
        for (Path file : jsonFiles("docs/examples/cli-v1")) {
            Object result = registry.parseJson(file);
            assertValid(file, registry.validateResult(result));
            for (Object diagnostic : diagnostics(result)) {
                diagnosticCount++;
                assertValid(file, registry.validateDiagnostic(diagnostic));
            }
        }
        assertTrue(diagnosticCount > 0);
        for (Path file : jsonFiles("docs/examples/runtime-manifest-v1")) {
            assertValid(file, registry.validateRuntimeManifest(registry.parseJson(file)));
        }
    }

    @Test
    public void rejectsRepresentativeNegativeDocumentsForAllFourSchemas() throws IOException {
        ConformanceSchemaRegistry registry = ConformanceSchemaRegistry.load(SNAPSHOT);

        Map<String, Object> artifact = object(registry.parseJson(SNAPSHOT.resolve("docs/examples/artifacts-v1/change-request.example.json")));
        artifact.remove("operations");
        assertInvalid(registry.validateArtifact(artifact));

        Map<String, Object> result = object(registry.parseJson(SNAPSHOT.resolve("docs/examples/cli-v1/plan-change-succeeded.result.json")));
        object(result.get("data")).remove("output_plan");
        assertInvalid(registry.validateResult(result));

        Map<String, Object> diagnosticResult = object(registry.parseJson(SNAPSHOT.resolve("docs/examples/cli-v1/usage-error.result.json")));
        Map<String, Object> diagnostic = object(diagnostics(diagnosticResult).get(0));
        diagnostic.remove("message");
        assertInvalid(registry.validateDiagnostic(diagnostic));

        Map<String, Object> runtime = object(registry.parseJson(SNAPSHOT.resolve("docs/examples/runtime-manifest-v1/java-runtime-manifest.example.json")));
        runtime.remove("runtime");
        assertInvalid(registry.validateRuntimeManifest(runtime));
    }

    @Test
    public void evaluatesEveryCheckedInJsonSchemaMutationCase() throws IOException {
        ConformanceSchemaRegistry registry = ConformanceSchemaRegistry.load(SNAPSHOT);
        ConformanceSuiteLoader.ConformanceSuite suite = ConformanceSuiteLoader.load(SNAPSHOT);
        int executed = 0;
        for (ConformanceSuiteLoader.ContractCase contractCase : suite.contractCases) {
            if (!"json-schema".equals(contractCase.validationLayer)) {
                continue;
            }
            assertEquals(1, contractCase.inputs.size(), contractCase.id);
            assertEquals("result", contractCase.inputs.get(0).role, contractCase.id);
            Object document = deepCopy(registry.parseJson(contractCase.inputs.get(0).path));
            for (ConformanceSuiteLoader.Mutation mutation : contractCase.mutations) {
                applyMutation(document, mutation);
            }
            ConformanceSchemaRegistry.Validation validation = registry.validateResult(document);
            assertEquals(contractCase.expectedValid, validation.valid, contractCase.id);
            executed++;
        }
        assertEquals(18, executed);
    }

    @Test
    public void rejectsARegistrySchemaThatUsesAnUnreviewedKeyword() throws IOException {
        Path copied = copySnapshot();
        try {
            Path schema = copied.resolve("docs/schemas/miku-project-runtime-manifest-v1.schema.json");
            Map<String, Object> document = object(jsonUtil.parseJsonText(new String(Files.readAllBytes(schema), StandardCharsets.UTF_8)));
            document.put("unreviewed_keyword", Boolean.TRUE);
            Files.write(schema, (jsonUtil.stringifyPrettyJson(document) + "\n").getBytes(StandardCharsets.UTF_8));

            ConformanceSchemaRegistry.ConformanceSchemaException error = assertThrows(
                    ConformanceSchemaRegistry.ConformanceSchemaException.class,
                    () -> ConformanceSchemaRegistry.loadFromVerifiedSnapshotForTest(copied));
            assertEquals("conformance-schema.unsupported-keyword", error.code);
        } finally {
            deleteTree(copied);
        }
    }

    private void assertValid(Path file, ConformanceSchemaRegistry.Validation validation) {
        assertTrue(validation.valid, () -> file + " should be valid but failed at "
                + (validation.violations.isEmpty() ? "<unknown>" : validation.violations.get(0).keyword));
    }

    private void assertInvalid(ConformanceSchemaRegistry.Validation validation) {
        assertFalse(validation.valid);
        assertFalse(validation.violations.isEmpty());
    }

    private Object deepCopy(Object value) {
        return jsonUtil.parseJsonText(jsonUtil.stringifyJson(value));
    }

    private void applyMutation(Object document, ConformanceSuiteLoader.Mutation mutation) {
        String[] segments = mutation.pointer.substring(1).split("/", -1);
        Object parent = document;
        for (int index = 0; index < segments.length - 1; index++) {
            parent = child(parent, decodePointerSegment(segments[index]));
        }
        String last = decodePointerSegment(segments[segments.length - 1]);
        if (parent instanceof Map<?, ?>) {
            Map<String, Object> object = object(parent);
            if ("remove".equals(mutation.operation)) {
                assertTrue(object.containsKey(last), mutation.pointer);
                object.remove(last);
            } else {
                object.put(last, deepCopy(mutation.value));
            }
            return;
        }
        List<Object> array = array(parent);
        if ("add".equals(mutation.operation) && "-".equals(last)) {
            array.add(deepCopy(mutation.value));
            return;
        }
        int index = Integer.parseInt(last);
        if ("add".equals(mutation.operation)) {
            array.add(index, deepCopy(mutation.value));
        } else if ("replace".equals(mutation.operation)) {
            array.set(index, deepCopy(mutation.value));
        } else {
            array.remove(index);
        }
    }

    private Object child(Object parent, String segment) {
        if (parent instanceof Map<?, ?>) {
            Map<String, Object> object = object(parent);
            assertTrue(object.containsKey(segment), segment);
            return object.get(segment);
        }
        return array(parent).get(Integer.parseInt(segment));
    }

    private String decodePointerSegment(String segment) {
        return segment.replace("~1", "/").replace("~0", "~");
    }

    @SuppressWarnings("unchecked")
    private List<Object> diagnostics(Object result) {
        Object value = object(result).get("diagnostics");
        return value instanceof List<?> ? (List<Object>) value : new ArrayList<Object>();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> object(Object value) {
        return (Map<String, Object>) value;
    }

    @SuppressWarnings("unchecked")
    private List<Object> array(Object value) {
        return (List<Object>) value;
    }

    private List<Path> jsonFiles(String relativeDirectory) throws IOException {
        try (Stream<Path> paths = Files.list(SNAPSHOT.resolve(relativeDirectory))) {
            return paths.filter(path -> path.getFileName().toString().endsWith(".json"))
                    .sorted().collect(Collectors.toList());
        }
    }

    private Path copySnapshot() throws IOException {
        Path destination = Files.createTempDirectory("miku-project-conformance-schema-");
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
}
