/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuproject.conformance;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

public class ConformanceComparisonAssertionsTest {
    private static final Path SNAPSHOT = Paths.get("vendor", "miku-project-contract", "v1.0.3");

    @Test
    public void comparesRuntimeIndependentJsonAndPreservesTaskOrder() throws IOException,
            ConformanceCanonicalJson.CanonicalJsonException {
        ConformanceComparisonAssertions assertions = ConformanceComparisonAssertions.load(SNAPSHOT);
        ConformanceSchemaRegistry registry = ConformanceSchemaRegistry.load(SNAPSHOT);
        Map<String, Object> expected = object(ConformanceCanonicalJson.deepCopy(registry.parseJson(
                SNAPSHOT.resolve("testdata/conformance/v1/golden/semantic/dependency.state.json"))));
        Map<String, Object> sameMeaning = object(ConformanceCanonicalJson.deepCopy(expected));
        reverse(list(sameMeaning.get("dependencies")));
        reverse(list(sameMeaning.get("resources")));
        reverse(list(sameMeaning.get("assignments")));
        reverse(list(sameMeaning.get("calendars")));

        assertTrue(assertions.exactJson(expected, sameMeaning).equal);
        assertTrue(assertions.semanticState(expected, sameMeaning).equal);
        assertTrue(assertions.semanticCrossRuntime(expected, sameMeaning).equal);

        Map<String, Object> differentTaskOrder = object(ConformanceCanonicalJson.deepCopy(expected));
        reverse(list(differentTaskOrder.get("tasks")));
        assertFalse(assertions.semanticState(expected, differentTaskOrder).equal);

        assertTrue(assertions.byteSameRuntime(new byte[] { 1, 2, 3 }, new byte[] { 1, 2, 3 }).equal);
        assertFalse(assertions.byteSameRuntime(new byte[] { 1, 2, 3 }, new byte[] { 1, 2, 4 }).equal);
    }

    @Test
    public void comparisonModesUseTheSameDomainCanonicalizationAsSemanticDigest() throws Exception {
        ConformanceComparisonAssertions assertions = ConformanceComparisonAssertions.load(SNAPSHOT);
        Map<String, Object> expected = orderingCollections();
        Map<String, Object> reversed = object(ConformanceCanonicalJson.deepCopy(expected));
        reverse(list(reversed.get("dependencies")));
        reverse(list(reversed.get("resources")));
        reverse(list(reversed.get("assignments")));
        reverse(list(reversed.get("calendars")));

        ConformanceComparisonAssertions.Comparison exact = assertions.exactJson(expected, reversed);
        assertTrue(exact.equal);
        assertTrue(assertions.semanticState(expected, reversed).equal);
        assertTrue(assertions.semanticCrossRuntime(expected, reversed).equal);
        // A whole-object JSON-text comparison would put the optional-field
        // values first; v1 instead uses the contract's UID / tuple keys.
        assertTrue(exact.expectedRepresentation.indexOf("\"predecessor_uid\":\"\uE000\"")
                < exact.expectedRepresentation.indexOf("\"predecessor_uid\":\"😀\""));
        assertTrue(exact.expectedRepresentation.indexOf("\"resource_uid\":\"Zulu\",\"task_uid\":\"😀\",\"uid\":\"1\"")
                < exact.expectedRepresentation.indexOf("\"resource_uid\":\"Alpha\",\"task_uid\":\"\uE000\",\"uid\":\"2\""));
        assertTrue(exact.expectedRepresentation.indexOf("\"name\":\"Zulu\",\"uid\":\"1\"")
                < exact.expectedRepresentation.indexOf("\"name\":\"Alpha\",\"uid\":\"2\""));
    }

    @Test
    public void validatesCommittedArtifactTopologyWithoutACommandImplementation() throws Exception {
        ConformanceSchemaRegistry registry = ConformanceSchemaRegistry.load(SNAPSHOT);
        ConformanceComparisonAssertions assertions = ConformanceComparisonAssertions.load(SNAPSHOT);
        Path destination = Files.createTempDirectory("miku-project-conformance-artifact-");
        try {
            Map<String, Object> planResult = object(registry.parseJson(
                    SNAPSHOT.resolve("docs/examples/cli-v1/plan-change-succeeded.result.json")));
            Map<String, Object> outputPlan = object(ConformanceCanonicalJson.deepCopy(object(planResult.get("data")).get("output_plan")));
            object(object(outputPlan.get("output")).get("destination")).put("path", destination.toString());

            byte[] projectBytes = "<Project>canonical fixture</Project>".getBytes(StandardCharsets.UTF_8);
            Files.write(destination.resolve("project.xml"), projectBytes);
            String projectDigest = ConformanceCanonicalJson.sha256RawBytes(projectBytes);
            putDigest(object(outputPlan.get("preflight")), "project_artifact_digest", projectDigest);

            Map<String, Object> provenance = object(ConformanceCanonicalJson.deepCopy(registry.parseJson(
                    SNAPSHOT.resolve("docs/examples/artifacts-v1/provenance.example.json"))));
            putDigest(object(provenance.get("output")), "artifact_digest", projectDigest);
            byte[] provenanceBytes = (ConformanceCanonicalJson.serialize(provenance) + "\n").getBytes(StandardCharsets.UTF_8);
            Files.write(destination.resolve("provenance.json"), provenanceBytes);
            Files.write(destination.resolve("COMMITTED"), new byte[0]);

            Map<String, Object> descriptor = artifactSetDescriptor(destination, projectDigest,
                    ConformanceCanonicalJson.sha256RawBytes(provenanceBytes));
            ConformanceComparisonAssertions.FilesystemValidation valid = assertions.artifactTopology(destination,
                    outputPlan, descriptor);
            assertTrue(valid.valid, valid.violations.toString());

            Files.write(destination.resolve("unexpected.txt"), new byte[] { 1 });
            ConformanceComparisonAssertions.FilesystemValidation extraMember = assertions.artifactTopology(destination,
                    outputPlan, descriptor);
            assertFalse(extraMember.valid);
            assertTrue(extraMember.violations.contains("member-set-mismatch"));
            Files.delete(destination.resolve("unexpected.txt"));

            Files.write(destination.resolve("COMMITTED"), new byte[] { 1 });
            ConformanceComparisonAssertions.FilesystemValidation marker = assertions.artifactTopology(destination,
                    outputPlan, descriptor);
            assertFalse(marker.valid);
            assertTrue(marker.violations.contains("commit-marker-size-mismatch"));
        } finally {
            deleteTree(destination);
        }
    }

    @Test
    public void validatesRuntimeManifestAssetsAndCoreCapabilityProfile() throws Exception {
        ConformanceSchemaRegistry registry = ConformanceSchemaRegistry.load(SNAPSHOT);
        ConformanceComparisonAssertions assertions = ConformanceComparisonAssertions.load(SNAPSHOT);
        Path runtime = Files.createTempDirectory("miku-project-conformance-runtime-");
        try {
            Map<String, Object> manifest = object(ConformanceCanonicalJson.deepCopy(registry.parseJson(
                    SNAPSHOT.resolve("docs/examples/runtime-manifest-v1/node-runtime-manifest.example.json"))));
            byte[] executableBytes = "#!/usr/bin/env node\nconsole.log('miku-project');\n".getBytes(StandardCharsets.UTF_8);
            byte[] sourceBytes = "source archive bytes".getBytes(StandardCharsets.UTF_8);
            Map<String, Object> artifacts = object(manifest.get("artifacts"));
            Map<String, Object> executable = object(artifacts.get("executable"));
            Map<String, Object> sources = object(artifacts.get("sources"));
            putDigest(object(object(manifest.get("compatibility")).get("conformance")), "corpus_digest",
                    assertions.expectedCorpusDigest());
            Files.write(runtime.resolve(text(executable.get("path"))), executableBytes);
            Files.write(runtime.resolve(text(sources.get("path"))), sourceBytes);
            putArtifactIntegrity(executable, executableBytes);
            putArtifactIntegrity(sources, sourceBytes);
            String manifestDigest = writeManifest(runtime, manifest);

            ConformanceComparisonAssertions.FilesystemValidation valid = assertions.runtimeIntegrity(runtime,
                    manifestDigest);
            assertTrue(valid.valid, valid.violations.toString());

            Files.write(runtime.resolve(text(executable.get("path"))), "tampered".getBytes(StandardCharsets.UTF_8));
            ConformanceComparisonAssertions.FilesystemValidation digestMismatch = assertions.runtimeIntegrity(runtime,
                    manifestDigest);
            assertFalse(digestMismatch.valid);
            assertTrue(digestMismatch.violations.contains("runtime-executable-size-mismatch")
                    || digestMismatch.violations.contains("runtime-executable-digest-mismatch"));
            Files.write(runtime.resolve(text(executable.get("path"))), executableBytes);

            list(object(object(manifest.get("compatibility")).get("capabilities")).get("provided")).remove(7);
            manifestDigest = writeManifest(runtime, manifest);
            ConformanceComparisonAssertions.FilesystemValidation capabilityMissing = assertions.runtimeIntegrity(runtime,
                    manifestDigest);
            assertFalse(capabilityMissing.valid);
            assertTrue(capabilityMissing.violations.contains("runtime-core-capabilities-missing"));

            putDigest(object(object(manifest.get("compatibility")).get("conformance")), "corpus_digest",
                    "eeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee");
            manifestDigest = writeManifest(runtime, manifest);
            ConformanceComparisonAssertions.FilesystemValidation corpusMismatch = assertions.runtimeIntegrity(runtime,
                    manifestDigest);
            assertFalse(corpusMismatch.valid);
            assertTrue(corpusMismatch.violations.contains("runtime-corpus-digest-mismatch"));
            putDigest(object(object(manifest.get("compatibility")).get("conformance")), "corpus_digest",
                    assertions.expectedCorpusDigest());
            manifestDigest = writeManifest(runtime, manifest);

            Path manifestFile = runtime.resolve("runtime-manifest.json");
            String validManifest = new String(Files.readAllBytes(manifestFile), StandardCharsets.UTF_8);
            String duplicateKeyManifest = validManifest.replaceFirst("\\\"kind\\\":\\\"miku_project_runtime_manifest\\\"",
                    "\\\"kind\\\":\\\"miku_project_runtime_manifest\\\",\\\"kind\\\":\\\"miku_project_runtime_manifest\\\"");
            byte[] duplicateKeyBytes = duplicateKeyManifest.getBytes(StandardCharsets.UTF_8);
            Files.write(manifestFile, duplicateKeyBytes);
            ConformanceComparisonAssertions.FilesystemValidation duplicateKey = assertions.runtimeIntegrity(runtime,
                    ConformanceCanonicalJson.sha256RawBytes(duplicateKeyBytes));
            assertFalse(duplicateKey.valid);
            assertTrue(duplicateKey.violations.contains("runtime-manifest-duplicate-key"));

            byte[] malformedUtf8 = new byte[] { (byte) 0xc3, (byte) 0x28 };
            Files.write(manifestFile, malformedUtf8);
            ConformanceComparisonAssertions.FilesystemValidation invalidUtf8 = assertions.runtimeIntegrity(runtime,
                    ConformanceCanonicalJson.sha256RawBytes(malformedUtf8));
            assertFalse(invalidUtf8.valid);
            assertTrue(invalidUtf8.violations.contains("runtime-manifest-not-utf8"));
        } finally {
            deleteTree(runtime);
        }
    }

    private static Map<String, Object> artifactSetDescriptor(Path destination, String projectDigest, String provenanceDigest) {
        LinkedHashMap<String, Object> descriptor = new LinkedHashMap<String, Object>();
        descriptor.put("kind", "miku_project_artifact_set");
        descriptor.put("schema_version", "1");
        descriptor.put("path", destination.toString());
        descriptor.put("publication_state", "committed");
        descriptor.put("project_artifact_digest", digest(projectDigest));
        descriptor.put("provenance_digest", digest(provenanceDigest));
        return descriptor;
    }

    private static void putArtifactIntegrity(Map<String, Object> artifact, byte[] bytes)
            throws ConformanceCanonicalJson.CanonicalJsonException {
        artifact.put("size_bytes", Integer.valueOf(bytes.length));
        artifact.put("digest", digest(ConformanceCanonicalJson.sha256RawBytes(bytes)));
    }

    private static void putDigest(Map<String, Object> object, String key, String value) {
        object.put(key, digest(value));
    }

    private static Map<String, Object> digest(String value) {
        LinkedHashMap<String, Object> digest = new LinkedHashMap<String, Object>();
        digest.put("algorithm", "sha-256");
        digest.put("value", value);
        return digest;
    }

    private static String writeManifest(Path runtime, Map<String, Object> manifest)
            throws IOException, ConformanceCanonicalJson.CanonicalJsonException {
        byte[] bytes = (ConformanceCanonicalJson.serialize(manifest) + "\n").getBytes(StandardCharsets.UTF_8);
        Files.write(runtime.resolve("runtime-manifest.json"), bytes);
        return ConformanceCanonicalJson.sha256RawBytes(bytes);
    }

    private static void reverse(List<Object> values) {
        java.util.Collections.reverse(values);
    }

    private static Map<String, Object> orderingCollections() {
        LinkedHashMap<String, Object> state = new LinkedHashMap<String, Object>();
        state.put("tasks", java.util.Arrays.<Object>asList(namedUid("2", "first"), namedUid("1", "second")));
        state.put("dependencies", java.util.Arrays.<Object>asList(
                dependency("😀", "x", "Alpha"), dependency("\uE000", "z", "Zulu")));
        state.put("resources", java.util.Arrays.<Object>asList(namedUid("2", "Alpha"), namedUid("1", "Zulu")));
        state.put("assignments", java.util.Arrays.<Object>asList(assignment("2", "\uE000", "Alpha"),
                assignment("1", "😀", "Zulu")));
        state.put("calendars", java.util.Arrays.<Object>asList(namedUid("2", "Alpha"), namedUid("1", "Zulu")));
        return state;
    }

    private static Map<String, Object> namedUid(String uid, String name) {
        LinkedHashMap<String, Object> item = new LinkedHashMap<String, Object>();
        item.put("uid", uid);
        item.put("name", name);
        return item;
    }

    private static Map<String, Object> dependency(String predecessorUid, String successorUid, String note) {
        LinkedHashMap<String, Object> dependency = new LinkedHashMap<String, Object>();
        dependency.put("predecessor_uid", predecessorUid);
        dependency.put("successor_uid", successorUid);
        dependency.put("type", "FS");
        dependency.put("lag", "PT0H0M0S");
        dependency.put("note", note);
        return dependency;
    }

    private static Map<String, Object> assignment(String uid, String taskUid, String resourceUid) {
        LinkedHashMap<String, Object> assignment = new LinkedHashMap<String, Object>();
        assignment.put("uid", uid);
        assignment.put("task_uid", taskUid);
        assignment.put("resource_uid", resourceUid);
        return assignment;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> object(Object value) {
        return (Map<String, Object>) value;
    }

    @SuppressWarnings("unchecked")
    private static List<Object> list(Object value) {
        return (List<Object>) value;
    }

    private static String text(Object value) {
        return (String) value;
    }

    private static void deleteTree(Path root) throws IOException {
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
