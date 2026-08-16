/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuproject.conformance;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

public class ContractSnapshotVerifierTest {
    private static final Path SNAPSHOT = Paths.get("vendor", "miku-project-contract", "v1.0.3");

    @Test
    public void acceptsTheTrackedV103Snapshot() throws IOException {
        ContractSnapshotVerifier.verify(SNAPSHOT);
    }

    @Test
    public void rejectsAnUnexpectedMember() throws IOException {
        Path copied = copySnapshot();
        try {
            Files.write(copied.resolve("unexpected.txt"), "unexpected\n".getBytes(StandardCharsets.UTF_8));

            assertThrows(ContractSnapshotVerifier.ContractSnapshotVerificationException.class,
                    () -> ContractSnapshotVerifier.verify(copied));
        } finally {
            deleteTree(copied);
        }
    }

    @Test
    public void rejectsAnUnexpectedEmptyDirectory() throws IOException {
        Path copied = copySnapshot();
        try {
            Files.createDirectories(copied.resolve("unexpected-empty"));

            assertThrows(ContractSnapshotVerifier.ContractSnapshotVerificationException.class,
                    () -> ContractSnapshotVerifier.verify(copied));
        } finally {
            deleteTree(copied);
        }
    }

    @Test
    public void rejectsAMissingMember() throws IOException {
        Path copied = copySnapshot();
        try {
            Files.delete(copied.resolve("docs/miku-project-cli-contract-v1.md"));

            assertThrows(ContractSnapshotVerifier.ContractSnapshotVerificationException.class,
                    () -> ContractSnapshotVerifier.verify(copied));
        } finally {
            deleteTree(copied);
        }
    }

    @Test
    public void rejectsADigestMismatch() throws IOException {
        Path copied = copySnapshot();
        try {
            Files.write(copied.resolve("docs/miku-project-cli-contract-v1.md"), "tampered\n".getBytes(StandardCharsets.UTF_8));

            assertThrows(ContractSnapshotVerifier.ContractSnapshotVerificationException.class,
                    () -> ContractSnapshotVerifier.verify(copied));
        } finally {
            deleteTree(copied);
        }
    }

    @Test
    public void rejectsAFractionalMemberSize() throws IOException {
        Path copied = copySnapshot();
        try {
            Path source = copied.resolve("SOURCE.json");
            String original = new String(Files.readAllBytes(source), StandardCharsets.UTF_8);
            String changed = original.replaceFirst("\\\"size_bytes\\\":[0-9]+", "\\\"size_bytes\\\":1.5");
            Files.write(source, changed.getBytes(StandardCharsets.UTF_8));

            assertThrows(ContractSnapshotVerifier.ContractSnapshotVerificationException.class,
                    () -> ContractSnapshotVerifier.verify(copied));
        } finally {
            deleteTree(copied);
        }
    }

    @Test
    public void rejectsARewrittenSourceManifestBeforeReadingMemberFiles() throws IOException {
        Path copied = copySnapshot();
        try {
            Path source = copied.resolve("SOURCE.json");
            String original = new String(Files.readAllBytes(source), StandardCharsets.UTF_8);
            String changed = original.replaceFirst("9473a0535a934f20334c39dabfe3b860fe01099539e9a7f216e7de28d74178d4",
                    "0000000000000000000000000000000000000000000000000000000000000000");
            Files.write(source, changed.getBytes(StandardCharsets.UTF_8));

            assertThrows(ContractSnapshotVerifier.ContractSnapshotVerificationException.class,
                    () -> ContractSnapshotVerifier.verify(copied));
        } finally {
            deleteTree(copied);
        }
    }

    @Test
    public void rejectsASymlinkedMember() throws IOException {
        Path copied = copySnapshot();
        try {
            Path target = copied.resolve("docs/miku-project-cli-contract-v1.md");
            Path replacement = copied.resolve("replacement.md");
            Files.move(target, replacement);
            Files.createSymbolicLink(target, replacement.getFileName());

            assertThrows(ContractSnapshotVerifier.ContractSnapshotVerificationException.class,
                    () -> ContractSnapshotVerifier.verify(copied));
        } finally {
            deleteTree(copied);
        }
    }

    private static Path copySnapshot() throws IOException {
        Path destination = Files.createTempDirectory("miku-project-contract-snapshot-");
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
