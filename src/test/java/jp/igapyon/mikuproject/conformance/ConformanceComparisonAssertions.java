/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuproject.conformance;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Reusable test-side assertions for the v1 comparison modes.
 *
 * <p>These methods intentionally compare only the supplied evidence. A future
 * command runner owns process launch, clean-directory recreation, project-read
 * observation, and destination-creation observation; it must pass those facts
 * to its case-specific assertions rather than making this utility guess them.
 * This keeps P5-B independent from an unimplemented Java CLI.</p>
 */
public final class ConformanceComparisonAssertions {
    private static final List<String> CORE_CAPABILITIES = Collections.unmodifiableList(Arrays.asList(
            "miku-project.capability.apply-change.set-task-percent-complete/v1",
            "miku-project.capability.format.ms-project-xml-subset.read/v1",
            "miku-project.capability.format.ms-project-xml-subset.write/v1",
            "miku-project.capability.inspect.project-overview/v1",
            "miku-project.capability.inspect.task-change-context/v1",
            "miku-project.capability.plan-change.set-task-percent-complete/v1",
            "miku-project.capability.publication.exclusive-directory-commit-marker/v1",
            "miku-project.capability.validate.project/v1",
            "miku-project.capability.verify-artifact/v1"));

    private final ConformanceSchemaRegistry registry;
    private final String expectedCorpusDigest;

    private ConformanceComparisonAssertions(ConformanceSchemaRegistry registry, String expectedCorpusDigest) {
        this.registry = registry;
        this.expectedCorpusDigest = expectedCorpusDigest;
    }

    /** Loads assertions from the verified immutable contract snapshot. */
    public static ConformanceComparisonAssertions load(Path snapshotRoot) throws IOException {
        ConformanceSchemaRegistry registry = ConformanceSchemaRegistry.load(snapshotRoot);
        Map<String, Object> source = object(registry.parseJson(snapshotRoot.resolve("SOURCE.json")));
        Map<String, Object> conformance = source == null ? null : object(source.get("conformance"));
        String corpusDigest = digestValue(conformance == null ? null : conformance.get("corpus_digest"));
        if (corpusDigest == null) {
            throw new IOException("verified contract snapshot lacks conformance.corpus_digest");
        }
        return new ConformanceComparisonAssertions(registry, corpusDigest);
    }

    /** The fixed corpus digest a runtime manifest must declare for this suite. */
    public String expectedCorpusDigest() {
        return expectedCorpusDigest;
    }

    /**
     * Compares runtime-independent JSON. Object key order and formatting are
     * ignored; collections whose semantic contract says order is irrelevant
     * are canonicalized before comparison.
     */
    public Comparison exactJson(Object expected, Object actual) {
        return compareJson(expected, actual, "exact-json");
    }

    /**
     * Compares v1 semantic state while preserving task/root/sibling order and
     * treating dependency/resource/assignment/calendar collections as sets.
     */
    public Comparison semanticState(Object expected, Object actual) {
        return compareJson(expected, actual, "semantic-state");
    }

    /**
     * A caller passes already-extracted runtime-independent values. This avoids
     * a hidden broad rule that might accidentally discard a semantic digest or
     * provenance field during Node/Java comparison.
     */
    public Comparison semanticCrossRuntime(Object expectedRuntimeIndependent, Object actualRuntimeIndependent) {
        return compareJson(expectedRuntimeIndependent, actualRuntimeIndependent, "semantic-cross-runtime");
    }

    /** Compares two result/artifact byte streams from the same invocation shape. */
    public Comparison byteSameRuntime(byte[] first, byte[] second) {
        if (first == null || second == null) {
            return Comparison.failure("byte-same-runtime", "byte-stream-missing", null, null);
        }
        try {
            String firstDigest = ConformanceCanonicalJson.sha256RawBytes(first);
            String secondDigest = ConformanceCanonicalJson.sha256RawBytes(second);
            return Arrays.equals(first, second)
                    ? Comparison.success("byte-same-runtime", firstDigest, secondDigest)
                    : Comparison.failure("byte-same-runtime", "byte-stream-different", firstDigest, secondDigest);
        } catch (ConformanceCanonicalJson.CanonicalJsonException error) {
            return Comparison.failure("byte-same-runtime", "digest-unavailable", null, null);
        }
    }

    /**
     * Checks the committed artifact-set topology against an output plan. The
     * optional descriptor is the successful result's {@code data.artifact_set}
     * object; providing it adds raw member-digest and path checks.
     */
    public FilesystemValidation artifactTopology(Path destination, Object outputPlan, Object artifactSetDescriptor) {
        List<String> violations = new ArrayList<String>();
        Map<String, Object> plan = object(outputPlan);
        if (plan == null || !registry.validateArtifact(plan).valid
                || !"miku_project_output_plan".equals(text(plan.get("kind")))) {
            violations.add("output-plan-schema-invalid");
            return new FilesystemValidation(violations);
        }
        Map<String, Object> output = object(plan.get("output"));
        Map<String, Object> plannedDestination = output == null ? null : object(output.get("destination"));
        String plannedPath = textAt(plannedDestination, "path");
        if (destination == null || plannedPath == null || !samePath(destination, plannedPath)) {
            violations.add("destination-path-mismatch");
        }
        if (destination == null || Files.isSymbolicLink(destination)
                || !Files.isDirectory(destination, LinkOption.NOFOLLOW_LINKS)) {
            violations.add("destination-not-regular-directory");
            return new FilesystemValidation(violations);
        }
        Map<String, Map<String, Object>> plannedMembers = plannedMembers(output == null ? null : list(output.get("members")),
                violations);
        if (plannedMembers.isEmpty()) {
            return new FilesystemValidation(violations);
        }
        Set<String> expectedNames = new LinkedHashSet<String>(plannedMembers.keySet());
        Set<String> actualNames = directRegularMembers(destination, violations);
        if (!expectedNames.equals(actualNames)) {
            violations.add("member-set-mismatch");
        }
        Map<String, Object> projectMember = plannedMembers.get("project.xml");
        Map<String, Object> provenanceMember = plannedMembers.get("provenance.json");
        Map<String, Object> markerMember = plannedMembers.get("COMMITTED");
        Path projectFile = destination.resolve("project.xml");
        Path provenanceFile = destination.resolve("provenance.json");
        Path markerFile = destination.resolve("COMMITTED");
        if (projectMember == null || !regularNonSymlink(projectFile)) {
            violations.add("project-member-invalid");
        }
        if (provenanceMember == null || !regularNonSymlink(provenanceFile)) {
            violations.add("provenance-member-invalid");
        }
        if (markerMember == null || !regularNonSymlink(markerFile)) {
            violations.add("commit-marker-invalid");
        } else {
            Long expectedMarkerSize = longValue(markerMember.get("size"));
            try {
                if (expectedMarkerSize == null || Files.size(markerFile) != expectedMarkerSize.longValue()) {
                    violations.add("commit-marker-size-mismatch");
                }
            } catch (IOException error) {
                violations.add("commit-marker-unreadable");
            }
        }
        String projectDigest = rawDigest(projectFile, violations, "project");
        String provenanceDigest = rawDigest(provenanceFile, violations, "provenance");
        Map<String, Object> preflight = object(plan.get("preflight"));
        if (projectDigest == null || !digestValueEquals(digestAt(preflight, "project_artifact_digest"), projectDigest)) {
            violations.add("project-digest-mismatch");
        }
        if (regularNonSymlink(provenanceFile)) {
            try {
                Object provenance = registry.parseJson(provenanceFile);
                if (!registry.validateArtifact(provenance).valid
                        || !"miku_project_provenance".equals(textAt(object(provenance), "kind"))) {
                    violations.add("provenance-schema-invalid");
                }
            } catch (IOException error) {
                violations.add("provenance-json-invalid");
            }
        }
        validateArtifactSetDescriptor(destination, artifactSetDescriptor, projectDigest, provenanceDigest, violations);
        return new FilesystemValidation(violations);
    }

    /**
     * Checks the static runtime bundle before a command reads project input.
     * The caller supplies an optional external raw-manifest SHA-256 pin; a null
     * pin means only internal bundle consistency is being checked.
     */
    public FilesystemValidation runtimeIntegrity(Path runtimeDirectory, String expectedManifestRawDigest) {
        List<String> violations = new ArrayList<String>();
        if (runtimeDirectory == null || Files.isSymbolicLink(runtimeDirectory)
                || !Files.isDirectory(runtimeDirectory, LinkOption.NOFOLLOW_LINKS)) {
            violations.add("runtime-directory-invalid");
            return new FilesystemValidation(violations);
        }
        Path manifestFile = runtimeDirectory.resolve("runtime-manifest.json");
        if (!regularNonSymlink(manifestFile)) {
            violations.add("runtime-manifest-missing-or-symlink");
            return new FilesystemValidation(violations);
        }
        byte[] manifestBytes;
        try {
            manifestBytes = Files.readAllBytes(manifestFile);
        } catch (IOException error) {
            violations.add("runtime-manifest-unreadable");
            return new FilesystemValidation(violations);
        }
        if (hasUtf8Bom(manifestBytes)) {
            violations.add("runtime-manifest-has-bom");
        }
        String actualManifestDigest;
        try {
            actualManifestDigest = ConformanceCanonicalJson.sha256RawBytes(manifestBytes);
        } catch (ConformanceCanonicalJson.CanonicalJsonException error) {
            violations.add("runtime-manifest-digest-unavailable");
            return new FilesystemValidation(violations);
        }
        if (expectedManifestRawDigest != null && !expectedManifestRawDigest.equals(actualManifestDigest)) {
            violations.add("runtime-manifest-digest-mismatch");
        }
        Map<String, Object> manifest;
        try {
            manifest = ConformanceStrictJson.parseUtf8Object(manifestBytes);
        } catch (ConformanceStrictJson.StrictJsonException error) {
            if ("duplicate-key".equals(error.code)) {
                violations.add("runtime-manifest-duplicate-key");
            } else if ("invalid-utf8".equals(error.code)) {
                violations.add("runtime-manifest-not-utf8");
            } else {
                violations.add("runtime-manifest-json-invalid");
            }
            return new FilesystemValidation(violations);
        } catch (RuntimeException error) {
            violations.add("runtime-manifest-json-invalid");
            return new FilesystemValidation(violations);
        }
        if (manifest == null || !registry.validateRuntimeManifest(manifest).valid) {
            violations.add("runtime-manifest-schema-invalid");
            return new FilesystemValidation(violations);
        }
        Map<String, Object> artifacts = object(manifest.get("artifacts"));
        Map<String, Object> executable = artifacts == null ? null : object(artifacts.get("executable"));
        Map<String, Object> sources = artifacts == null ? null : object(artifacts.get("sources"));
        validateRuntimeArtifact(runtimeDirectory, executable, "executable", violations);
        validateRuntimeArtifact(runtimeDirectory, sources, "sources", violations);
        validateRuntimeNames(manifest, executable, sources, violations);
        validateCoreCapabilities(manifest, violations);
        validateCorpusBinding(manifest, violations);
        Set<String> expectedNames = new LinkedHashSet<String>();
        expectedNames.add("runtime-manifest.json");
        if (textAt(executable, "path") != null) {
            expectedNames.add(textAt(executable, "path"));
        }
        if (textAt(sources, "path") != null) {
            expectedNames.add(textAt(sources, "path"));
        }
        if (!expectedNames.equals(directRegularMembers(runtimeDirectory, violations))) {
            violations.add("runtime-member-set-mismatch");
        }
        return new FilesystemValidation(violations);
    }

    private Comparison compareJson(Object expected, Object actual, String mode) {
        try {
            Object expectedNormalized = ConformanceCanonicalJson.normalizeSemanticCollections(expected);
            Object actualNormalized = ConformanceCanonicalJson.normalizeSemanticCollections(actual);
            String expectedCanonical = ConformanceCanonicalJson.serialize(expectedNormalized);
            String actualCanonical = ConformanceCanonicalJson.serialize(actualNormalized);
            return expectedCanonical.equals(actualCanonical)
                    ? Comparison.success(mode, expectedCanonical, actualCanonical)
                    : Comparison.failure(mode, "json-value-different", expectedCanonical, actualCanonical);
        } catch (ConformanceCanonicalJson.CanonicalJsonException error) {
            return Comparison.failure(mode, "canonical-json-invalid", null, null);
        }
    }

    private Map<String, Map<String, Object>> plannedMembers(List<Object> members, List<String> violations) {
        LinkedHashMap<String, Map<String, Object>> result = new LinkedHashMap<String, Map<String, Object>>();
        if (members == null) {
            violations.add("output-plan-members-missing");
            return result;
        }
        for (Object rawMember : members) {
            Map<String, Object> member = object(rawMember);
            String path = textAt(member, "path");
            if (member == null || path == null || result.put(path, member) != null) {
                violations.add("output-plan-members-invalid");
            }
        }
        return result;
    }

    private Set<String> directRegularMembers(Path directory, List<String> violations) {
        LinkedHashSet<String> names = new LinkedHashSet<String>();
        try (Stream<Path> entries = Files.list(directory)) {
            for (Path entry : entries.collect(Collectors.toList())) {
                String name = entry.getFileName().toString();
                if (Files.isSymbolicLink(entry) || !Files.isRegularFile(entry, LinkOption.NOFOLLOW_LINKS)) {
                    violations.add("member-not-regular-file:" + name);
                } else {
                    names.add(name);
                }
            }
        } catch (IOException error) {
            violations.add("directory-unreadable");
        }
        return names;
    }

    private String rawDigest(Path file, List<String> violations, String role) {
        if (!regularNonSymlink(file)) {
            return null;
        }
        try {
            return ConformanceCanonicalJson.sha256RawBytes(Files.readAllBytes(file));
        } catch (IOException error) {
            violations.add(role + "-member-unreadable");
        } catch (ConformanceCanonicalJson.CanonicalJsonException error) {
            violations.add(role + "-digest-unavailable");
        }
        return null;
    }

    private void validateArtifactSetDescriptor(Path destination, Object rawDescriptor, String projectDigest,
            String provenanceDigest, List<String> violations) {
        if (rawDescriptor == null) {
            return;
        }
        Map<String, Object> descriptor = object(rawDescriptor);
        if (descriptor == null || !"miku_project_artifact_set".equals(text(descriptor.get("kind")))
                || !samePath(destination, text(descriptor.get("path")))
                || !"committed".equals(text(descriptor.get("publication_state")))) {
            violations.add("artifact-set-descriptor-invalid");
            return;
        }
        if (projectDigest == null || !digestValueEquals(descriptor.get("project_artifact_digest"), projectDigest)) {
            violations.add("artifact-set-project-digest-mismatch");
        }
        if (provenanceDigest == null || !digestValueEquals(descriptor.get("provenance_digest"), provenanceDigest)) {
            violations.add("artifact-set-provenance-digest-mismatch");
        }
    }

    private void validateRuntimeArtifact(Path root, Map<String, Object> artifact, String role, List<String> violations) {
        String path = textAt(artifact, "path");
        Long expectedSize = artifact == null ? null : longValue(artifact.get("size_bytes"));
        String expectedDigest = artifact == null ? null : digestValue(artifact.get("digest"));
        if (path == null || expectedSize == null || expectedDigest == null || path.indexOf('/') >= 0 || path.indexOf('\\') >= 0
                || ".".equals(path) || "..".equals(path)) {
            violations.add("runtime-" + role + "-descriptor-invalid");
            return;
        }
        Path normalizedRoot = root.toAbsolutePath().normalize();
        Path file = normalizedRoot.resolve(path).normalize();
        if (!file.getParent().equals(normalizedRoot) || !regularNonSymlink(file)) {
            violations.add("runtime-" + role + "-missing-or-symlink");
            return;
        }
        try {
            if (Files.size(file) != expectedSize.longValue()) {
                violations.add("runtime-" + role + "-size-mismatch");
            }
            if (!expectedDigest.equals(ConformanceCanonicalJson.sha256RawBytes(Files.readAllBytes(file)))) {
                violations.add("runtime-" + role + "-digest-mismatch");
            }
        } catch (IOException error) {
            violations.add("runtime-" + role + "-unreadable");
        } catch (ConformanceCanonicalJson.CanonicalJsonException error) {
            violations.add("runtime-" + role + "-digest-unavailable");
        }
    }

    private void validateRuntimeNames(Map<String, Object> manifest, Map<String, Object> executable,
            Map<String, Object> sources, List<String> violations) {
        Map<String, Object> runtime = object(manifest.get("runtime"));
        String family = textAt(runtime, "family");
        String version = textAt(runtime, "version");
        if (family == null || version == null) {
            violations.add("runtime-identity-missing");
            return;
        }
        String expectedExecutable = "miku-project-" + family + "-" + version + ("node".equals(family) ? ".mjs" : ".jar");
        String expectedSources = "miku-project-" + family + "-" + version + "-sources.tgz";
        if (!expectedExecutable.equals(textAt(executable, "path"))) {
            violations.add("runtime-executable-name-mismatch");
        }
        if (!expectedSources.equals(textAt(sources, "path"))) {
            violations.add("runtime-sources-name-mismatch");
        }
    }

    private void validateCoreCapabilities(Map<String, Object> manifest, List<String> violations) {
        Map<String, Object> compatibility = object(manifest.get("compatibility"));
        Map<String, Object> capabilities = compatibility == null ? null : object(compatibility.get("capabilities"));
        List<Object> provided = capabilities == null ? null : list(capabilities.get("provided"));
        if (provided == null || provided.size() != CORE_CAPABILITIES.size()) {
            violations.add("runtime-core-capabilities-missing");
            return;
        }
        List<String> values = new ArrayList<String>();
        for (Object value : provided) {
            if (!(value instanceof String)) {
                violations.add("runtime-core-capabilities-invalid");
                return;
            }
            values.add((String) value);
        }
        if (!CORE_CAPABILITIES.equals(values)) {
            violations.add("runtime-core-capabilities-missing");
        }
    }

    private void validateCorpusBinding(Map<String, Object> manifest, List<String> violations) {
        Map<String, Object> compatibility = object(manifest.get("compatibility"));
        Map<String, Object> conformance = compatibility == null ? null : object(compatibility.get("conformance"));
        if (!expectedCorpusDigest.equals(digestValue(conformance == null ? null : conformance.get("corpus_digest")))) {
            violations.add("runtime-corpus-digest-mismatch");
        }
    }

    private static boolean regularNonSymlink(Path path) {
        return path != null && !Files.isSymbolicLink(path) && Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS);
    }

    private static boolean hasUtf8Bom(byte[] bytes) {
        return bytes.length >= 3 && (bytes[0] & 0xff) == 0xef && (bytes[1] & 0xff) == 0xbb
                && (bytes[2] & 0xff) == 0xbf;
    }

    private static boolean digestValueEquals(Object digest, String value) {
        return "sha-256".equals(textAt(object(digest), "algorithm")) && value.equals(digestValue(digest));
    }

    private static String digestValue(Object digest) {
        return textAt(object(digest), "value");
    }

    private static Object digestAt(Map<String, Object> object, String key) {
        return object == null ? null : object.get(key);
    }

    private static boolean samePath(Path actual, String expected) {
        if (actual == null || expected == null) {
            return false;
        }
        try {
            return actual.toAbsolutePath().normalize().equals(Paths.get(expected).toAbsolutePath().normalize());
        } catch (RuntimeException error) {
            return false;
        }
    }

    private static String textAt(Map<String, Object> object, String key) {
        return object == null ? null : text(object.get(key));
    }

    private static String text(Object value) {
        return value instanceof String ? (String) value : null;
    }

    private static Long longValue(Object value) {
        if (value instanceof Byte || value instanceof Short || value instanceof Integer || value instanceof Long) {
            return Long.valueOf(((Number) value).longValue());
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> object(Object value) {
        return value instanceof Map<?, ?> ? (Map<String, Object>) value : null;
    }

    @SuppressWarnings("unchecked")
    private static List<Object> list(Object value) {
        return value instanceof List<?> ? (List<Object>) value : null;
    }

    private static Set<String> setOf(String... values) {
        return new LinkedHashSet<String>(Arrays.asList(values));
    }

    public static final class Comparison {
        public final boolean equal;
        public final String mode;
        public final String reason;
        public final String expectedRepresentation;
        public final String actualRepresentation;

        private Comparison(boolean equal, String mode, String reason, String expectedRepresentation,
                String actualRepresentation) {
            this.equal = equal;
            this.mode = mode;
            this.reason = reason;
            this.expectedRepresentation = expectedRepresentation;
            this.actualRepresentation = actualRepresentation;
        }

        static Comparison success(String mode, String expectedRepresentation, String actualRepresentation) {
            return new Comparison(true, mode, null, expectedRepresentation, actualRepresentation);
        }

        static Comparison failure(String mode, String reason, String expectedRepresentation,
                String actualRepresentation) {
            return new Comparison(false, mode, reason, expectedRepresentation, actualRepresentation);
        }
    }

    public static final class FilesystemValidation {
        public final boolean valid;
        public final List<String> violations;

        FilesystemValidation(List<String> violations) {
            this.valid = violations.isEmpty();
            this.violations = Collections.unmodifiableList(new ArrayList<String>(violations));
        }
    }
}
