/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuproject.conformance;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jp.igapyon.mikuproject.coreapi.CoreApiAiJsonUtil;

/**
 * Test-side trust boundary for an immutable miku-project contract snapshot.
 * It validates the snapshot before any future v1 command test reads a fixture
 * or derives behavior from it.
 */
public final class ContractSnapshotVerifier {
    private static final String KIND = "miku_project_contract_snapshot";
    private static final String CONTRACT_VERSION = "1";
    private static final String FIXTURE_SUITE_VERSION = "1";
    private static final String SOURCE_REPOSITORY = "https://github.com/igapyon/miku-project";
    private static final String SOURCE_TAG = "v1.0.3";
    private static final String SOURCE_REVISION = "693b4ecd7d4328d77f3b2eada9c4965a9c9b15f5";
    private static final String CORPUS_DIGEST = "f0b4a821f80f155ba01afc1fdf7c5d2fa6ca5e744bbd0090819e0d591f1c47a3";
    private static final String GATE_RUNTIME_LOCK_DIGEST = "95cd11cc4460348fa066908994430adba5983384c06c75679855120e5c5ea3d5";
    private static final String SOURCE_MANIFEST_DIGEST = "a9eebb6db384680de23a98cb90d887a63cabd8f1ef999f3ebf85bd48628c0807";

    private ContractSnapshotVerifier() {
    }

    public static void verify(Path snapshotRoot) throws IOException {
        if (snapshotRoot == null || Files.isSymbolicLink(snapshotRoot)
                || !Files.isDirectory(snapshotRoot, LinkOption.NOFOLLOW_LINKS)) {
            throw failure("snapshot root must be a regular directory");
        }
        Path root = snapshotRoot.toAbsolutePath().normalize();
        Path sourceFile = root.resolve("SOURCE.json");
        requireRegularFile(sourceFile, "SOURCE.json");
        byte[] sourceBytes = Files.readAllBytes(sourceFile);
        Map<String, Object> document = parseDocument(sourceBytes);
        requireCanonicalJson(sourceBytes, document);
        validateIdentity(document);

        List<Member> members = parseMembers(document);
        requireEquals(SOURCE_MANIFEST_DIGEST, sha256(sourceBytes), "SOURCE.json raw SHA-256");
        Set<String> expectedFiles = new LinkedHashSet<String>();
        expectedFiles.add("SOURCE.json");
        for (Member member : members) {
            if (!expectedFiles.add(member.path)) {
                throw failure("SOURCE.json contains a duplicate member path: " + member.path);
            }
            Path memberPath = resolveMember(root, member.path);
            requireRegularFile(memberPath, member.path);
            if (Files.size(memberPath) != member.sizeBytes) {
                throw failure("snapshot member size mismatch: " + member.path);
            }
            if (!member.digest.equals(sha256(Files.readAllBytes(memberPath)))) {
                throw failure("snapshot member digest mismatch: " + member.path);
            }
        }
        assertExactFilesystem(root, expectedFiles, expectedDirectoryPaths(expectedFiles));
        assertCorpusDigest(document, members);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> parseDocument(byte[] sourceBytes) {
        Object parsed;
        try {
            parsed = new CoreApiAiJsonUtil().parseJsonText(new String(sourceBytes, StandardCharsets.UTF_8));
        } catch (RuntimeException error) {
            throw failure("SOURCE.json is not valid JSON", error);
        }
        if (!(parsed instanceof Map<?, ?>)) {
            throw failure("SOURCE.json must be a JSON object");
        }
        return (Map<String, Object>) parsed;
    }

    private static void requireCanonicalJson(byte[] sourceBytes, Map<String, Object> document) {
        String expected = new CoreApiAiJsonUtil().stringifyJson(document) + "\n";
        String actual = new String(sourceBytes, StandardCharsets.UTF_8);
        if (!expected.equals(actual)) {
            throw failure("SOURCE.json must be canonical JSON with one trailing LF");
        }
    }

    private static void validateIdentity(Map<String, Object> document) {
        requireExactKeys(document, "SOURCE.json", "conformance", "contract_version", "gate", "kind", "members",
                "node_reference", "schema_version", "source", "source_gate_runtime_lock_digest");
        requireEquals(KIND, document.get("kind"), "SOURCE.json.kind");
        requireEquals("1", document.get("schema_version"), "SOURCE.json.schema_version");
        requireEquals(CONTRACT_VERSION, document.get("contract_version"), "SOURCE.json.contract_version");
        requireEquals("G4", document.get("gate"), "SOURCE.json.gate");

        Map<String, Object> source = requireObject(document.get("source"), "SOURCE.json.source");
        requireExactKeys(source, "SOURCE.json.source", "repository", "revision", "tag");
        requireEquals(SOURCE_REPOSITORY, source.get("repository"), "SOURCE.json.source.repository");
        requireEquals(SOURCE_REVISION, source.get("revision"), "SOURCE.json.source.revision");
        requireEquals(SOURCE_TAG, source.get("tag"), "SOURCE.json.source.tag");

        Map<String, Object> conformance = requireObject(document.get("conformance"), "SOURCE.json.conformance");
        requireExactKeys(conformance, "SOURCE.json.conformance", "corpus_digest", "fixture_suite_version");
        requireEquals(FIXTURE_SUITE_VERSION, conformance.get("fixture_suite_version"), "SOURCE.json.conformance.fixture_suite_version");
        requireDigest(conformance.get("corpus_digest"), CORPUS_DIGEST, "SOURCE.json.conformance.corpus_digest");

        Map<String, Object> nodeReference = requireObject(document.get("node_reference"), "SOURCE.json.node_reference");
        requireExactKeys(nodeReference, "SOURCE.json.node_reference", "release_version", "runtime_family", "runtime_role", "runtime_version");
        requireEquals("1.0.3", nodeReference.get("release_version"), "SOURCE.json.node_reference.release_version");
        requireEquals("node", nodeReference.get("runtime_family"), "SOURCE.json.node_reference.runtime_family");
        requireEquals("reference", nodeReference.get("runtime_role"), "SOURCE.json.node_reference.runtime_role");
        requireEquals("1.0.3", nodeReference.get("runtime_version"), "SOURCE.json.node_reference.runtime_version");
        requireDigest(document.get("source_gate_runtime_lock_digest"), GATE_RUNTIME_LOCK_DIGEST,
                "SOURCE.json.source_gate_runtime_lock_digest");
    }

    @SuppressWarnings("unchecked")
    private static List<Member> parseMembers(Map<String, Object> document) {
        Object rawMembers = document.get("members");
        if (!(rawMembers instanceof List<?>)) {
            throw failure("SOURCE.json.members must be an array");
        }
        List<Member> members = new ArrayList<Member>();
        String previousPath = null;
        for (Object rawMember : (List<Object>) rawMembers) {
            Map<String, Object> member = requireObject(rawMember, "SOURCE.json.members[]");
            requireExactKeys(member, "SOURCE.json.members[]", "digest", "path", "size_bytes");
            String memberPath = requireSafePath(member.get("path"), "SOURCE.json.members[].path");
            if (previousPath != null && previousPath.compareTo(memberPath) >= 0) {
                throw failure("SOURCE.json.members must be strictly path-sorted");
            }
            previousPath = memberPath;
            long sizeBytes = requireNonNegativeLong(member.get("size_bytes"), "SOURCE.json.members[].size_bytes");
            String digest = requireDigest(member.get("digest"), null, "SOURCE.json.members[].digest");
            members.add(new Member(memberPath, sizeBytes, digest));
        }
        if (members.isEmpty()) {
            throw failure("SOURCE.json.members must not be empty");
        }
        return members;
    }

    private static void assertExactFilesystem(Path root, Set<String> expectedFiles, Set<String> expectedDirectories)
            throws IOException {
        List<String> actualFiles;
        try (Stream<Path> stream = Files.walk(root)) {
            actualFiles = stream.map(path -> root.relativize(path)).filter(path -> !path.toString().isEmpty())
                    .map(path -> path.toString().replace(path.getFileSystem().getSeparator(), "/"))
                    .sorted().collect(Collectors.toList());
        }
        for (String relative : actualFiles) {
            Path entry = root.resolve(relative);
            if (Files.isSymbolicLink(entry)) {
                throw failure("snapshot must not contain a symbolic link: " + relative);
            }
            if (!Files.isDirectory(entry, LinkOption.NOFOLLOW_LINKS) && !Files.isRegularFile(entry, LinkOption.NOFOLLOW_LINKS)) {
                throw failure("snapshot contains an unsupported filesystem entry: " + relative);
            }
        }
        Set<String> actualRegularFiles = new LinkedHashSet<String>();
        Set<String> actualDirectories = new LinkedHashSet<String>();
        for (String relative : actualFiles) {
            Path entry = root.resolve(relative);
            if (Files.isRegularFile(entry, LinkOption.NOFOLLOW_LINKS)) {
                actualRegularFiles.add(relative);
            } else if (Files.isDirectory(entry, LinkOption.NOFOLLOW_LINKS)) {
                actualDirectories.add(relative);
            }
        }
        if (!expectedFiles.equals(actualRegularFiles)) {
            throw failure("snapshot filesystem members do not match SOURCE.json");
        }
        if (!expectedDirectories.equals(actualDirectories)) {
            throw failure("snapshot directory topology does not match SOURCE.json member paths");
        }
    }

    private static Set<String> expectedDirectoryPaths(Set<String> expectedFiles) {
        Set<String> directories = new LinkedHashSet<String>();
        for (String file : expectedFiles) {
            int separator = file.indexOf('/');
            while (separator >= 0) {
                directories.add(file.substring(0, separator));
                separator = file.indexOf('/', separator + 1);
            }
        }
        return directories;
    }

    private static void assertCorpusDigest(Map<String, Object> document, List<Member> members) {
        List<Member> corpusMembers = new ArrayList<Member>();
        for (Member member : members) {
            if (member.path.startsWith("testdata/conformance/v1/")) {
                corpusMembers.add(member);
            }
        }
        Collections.sort(corpusMembers, (left, right) -> left.path.compareTo(right.path));
        StringBuilder lines = new StringBuilder();
        for (Member member : corpusMembers) {
            lines.append(member.digest).append("  ")
                    .append(member.path.substring("testdata/conformance/v1/".length())).append("\n");
        }
        String actualDigest = sha256(lines.toString().getBytes(StandardCharsets.UTF_8));
        requireDigest(requireObject(document.get("conformance"), "SOURCE.json.conformance").get("corpus_digest"), actualDigest,
                "SOURCE.json.conformance.corpus_digest");
    }

    private static Path resolveMember(Path root, String memberPath) {
        Path resolved = root.resolve(memberPath).normalize();
        if (!resolved.startsWith(root)) {
            throw failure("snapshot member path escapes the root: " + memberPath);
        }
        return resolved;
    }

    private static void requireRegularFile(Path path, String label) {
        if (Files.isSymbolicLink(path) || !Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
            throw failure(label + " must be a regular non-symlink file");
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> requireObject(Object value, String label) {
        if (!(value instanceof Map<?, ?>)) {
            throw failure(label + " must be an object");
        }
        return (Map<String, Object>) value;
    }

    private static void requireExactKeys(Map<String, Object> value, String label, String... expectedKeys) {
        Set<String> expected = new LinkedHashSet<String>();
        Collections.addAll(expected, expectedKeys);
        if (!expected.equals(value.keySet())) {
            throw failure(label + " has an unexpected object shape");
        }
    }

    private static void requireEquals(Object expected, Object actual, String label) {
        if (!expected.equals(actual)) {
            throw failure(label + " does not match the fixed contract snapshot");
        }
    }

    private static String requireSafePath(Object value, String label) {
        if (!(value instanceof String)) {
            throw failure(label + " must be a string");
        }
        String path = (String) value;
        if (path.isEmpty() || path.startsWith("/") || path.indexOf('\\') >= 0 || path.contains("//")) {
            throw failure(label + " is not a safe relative path");
        }
        for (String segment : path.split("/")) {
            if (segment.isEmpty() || ".".equals(segment) || "..".equals(segment)) {
                throw failure(label + " is not a safe relative path");
            }
        }
        return path;
    }

    private static long requireNonNegativeLong(Object value, String label) {
        if (!(value instanceof Integer) && !(value instanceof Long)) {
            throw failure(label + " must be a non-negative integer");
        }
        if (((Number) value).longValue() < 0) {
            throw failure(label + " must be a non-negative integer");
        }
        return ((Number) value).longValue();
    }

    @SuppressWarnings("unchecked")
    private static String requireDigest(Object value, String expectedValue, String label) {
        Map<String, Object> digest = requireObject(value, label);
        requireExactKeys(digest, label, "algorithm", "value");
        requireEquals("sha-256", digest.get("algorithm"), label + ".algorithm");
        if (!(digest.get("value") instanceof String) || !((String) digest.get("value")).matches("[0-9a-f]{64}")) {
            throw failure(label + ".value must be a lowercase SHA-256 digest");
        }
        String actualValue = (String) digest.get("value");
        if (expectedValue != null) {
            requireEquals(expectedValue, actualValue, label + ".value");
        }
        return actualValue;
    }

    private static String sha256(byte[] value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value);
            StringBuilder builder = new StringBuilder();
            for (byte valueByte : digest) {
                builder.append(String.format("%02x", valueByte & 0xff));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException("SHA-256 is unavailable", error);
        }
    }

    private static ContractSnapshotVerificationException failure(String message) {
        return new ContractSnapshotVerificationException(message);
    }

    private static ContractSnapshotVerificationException failure(String message, Throwable cause) {
        return new ContractSnapshotVerificationException(message, cause);
    }

    private static final class Member {
        private final String path;
        private final long sizeBytes;
        private final String digest;

        private Member(String path, long sizeBytes, String digest) {
            this.path = path;
            this.sizeBytes = sizeBytes;
            this.digest = digest;
        }
    }

    public static final class ContractSnapshotVerificationException extends IllegalStateException {
        private static final long serialVersionUID = 1L;

        private ContractSnapshotVerificationException(String message) {
            super(message);
        }

        private ContractSnapshotVerificationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
