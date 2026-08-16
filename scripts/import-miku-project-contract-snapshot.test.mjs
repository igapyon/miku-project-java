import assert from "node:assert/strict";
import { createHash } from "node:crypto";
import { execFileSync } from "node:child_process";
import {
  existsSync,
  mkdirSync,
  mkdtempSync,
  readFileSync,
  readdirSync,
  rmSync,
  statSync,
  symlinkSync,
  writeFileSync
} from "node:fs";
import os from "node:os";
import path from "node:path";
import test, { after } from "node:test";

import {
  ContractSnapshotImportError,
  createContractSnapshotImporter
} from "./import-miku-project-contract-snapshot.mjs";

const TEMPORARY_DIRECTORIES = [];

after(() => {
  for (const directory of TEMPORARY_DIRECTORIES.reverse()) {
    rmSync(directory, { recursive: true, force: true });
  }
});

test("imports only the fixed Git tag and leaves an identical snapshot unchanged", () => {
  const source = createSourceRepository();
  const repositoryRoot = createTemporaryDirectory("miku-project-contract-java-root-");
  const importer = createContractSnapshotImporter({ contract: source.contract });

  const first = importer({ sourceRepository: source.directory, repositoryRoot });
  const snapshot = path.join(repositoryRoot, source.contract.defaultOutputDirectory);
  assert.equal(first.snapshot_action, "installed");
  assert.equal(readFileSync(path.join(snapshot, "docs/contract.md"), "utf8"), "tagged contract\n");
  assert.equal(readFileSync(path.join(source.directory, "docs/contract.md"), "utf8"), "working tree change\n");

  const before = snapshotMetadata(snapshot);
  const second = importer({ sourceRepository: source.directory, repositoryRoot });
  assert.equal(second.snapshot_action, "unchanged");
  assert.deepEqual(snapshotMetadata(snapshot), before);
});

test("continues to read the verified revision when the source tag moves afterward", () => {
  const source = createSourceRepository();
  const repositoryRoot = createTemporaryDirectory("miku-project-contract-java-root-");
  let verifiedIdentity;
  const importer = createContractSnapshotImporter({
    contract: source.contract,
    afterSourceIdentity(identity) {
      verifiedIdentity = identity;
      git(identity.sourceRoot, ["tag", "-f", identity.sourceTag, source.alternateRevision]);
    }
  });

  const result = importer({ sourceRepository: source.directory, repositoryRoot });
  const snapshot = path.join(repositoryRoot, source.contract.defaultOutputDirectory);
  assert.equal(verifiedIdentity.sourceRevision, source.contract.sourceRevision);
  assert.equal(result.source.revision, source.contract.sourceRevision);
  assert.equal(readFileSync(path.join(snapshot, "docs/contract.md"), "utf8"), "tagged contract\n");
  const manifest = JSON.parse(readFileSync(path.join(snapshot, "SOURCE.json"), "utf8"));
  assert.equal(manifest.source.revision, source.contract.sourceRevision);
});

test("rejects a differing destination without changing its contents", () => {
  const source = createSourceRepository();
  const repositoryRoot = createTemporaryDirectory("miku-project-contract-java-root-");
  const importer = createContractSnapshotImporter({ contract: source.contract });
  importer({ sourceRepository: source.directory, repositoryRoot });

  const snapshot = path.join(repositoryRoot, source.contract.defaultOutputDirectory);
  writeFileSync(path.join(snapshot, "docs/contract.md"), "changed destination\n");
  const before = snapshotInventory(snapshot);
  assertImportError(
    () => importer({ sourceRepository: source.directory, repositoryRoot }),
    "contract-snapshot.destination-conflict"
  );
  assert.deepEqual(snapshotInventory(snapshot), before);
});

test("rejects an unexpected empty destination directory without changing it", () => {
  const source = createSourceRepository();
  const repositoryRoot = createTemporaryDirectory("miku-project-contract-java-root-");
  const importer = createContractSnapshotImporter({ contract: source.contract });
  importer({ sourceRepository: source.directory, repositoryRoot });

  const snapshot = path.join(repositoryRoot, source.contract.defaultOutputDirectory);
  mkdirSync(path.join(snapshot, "unexpected-empty"));
  const before = snapshotMetadata(snapshot);
  assertImportError(
    () => importer({ sourceRepository: source.directory, repositoryRoot }),
    "contract-snapshot.destination-conflict"
  );
  assert.deepEqual(snapshotMetadata(snapshot), before);
});

test("rejects a missing tag and a mismatched revision before it creates an output tree", () => {
  const source = createSourceRepository();
  const missingTagRoot = createTemporaryDirectory("miku-project-contract-java-root-");
  const missingTagImporter = createContractSnapshotImporter({
    contract: { ...source.contract, sourceTag: "v-missing" }
  });
  assertImportError(
    () => missingTagImporter({ sourceRepository: source.directory, repositoryRoot: missingTagRoot }),
    "contract-snapshot.source-tag-unavailable"
  );
  assert.equal(existsSync(path.join(missingTagRoot, "vendor")), false);

  const wrongRevisionRoot = createTemporaryDirectory("miku-project-contract-java-root-");
  const wrongRevisionImporter = createContractSnapshotImporter({
    contract: { ...source.contract, sourceRevision: "0".repeat(40) }
  });
  assertImportError(
    () => wrongRevisionImporter({ sourceRepository: source.directory, repositoryRoot: wrongRevisionRoot }),
    "contract-snapshot.source-revision-mismatch"
  );
  assert.equal(existsSync(path.join(wrongRevisionRoot, "vendor")), false);
});

test("rejects a repository-external destination", () => {
  const source = createSourceRepository();
  const repositoryRoot = createTemporaryDirectory("miku-project-contract-java-root-");
  const importer = createContractSnapshotImporter({ contract: source.contract });

  assertImportError(
    () => importer({
      sourceRepository: source.directory,
      repositoryRoot,
      outputDirectory: "../outside"
    }),
    "contract-snapshot.destination-invalid"
  );
  assert.equal(existsSync(path.join(path.dirname(repositoryRoot), "outside")), false);
});

test("rejects a symlinked destination parent without following it", () => {
  const source = createSourceRepository();
  const repositoryRoot = createTemporaryDirectory("miku-project-contract-java-root-");
  const outside = createTemporaryDirectory("miku-project-contract-outside-");
  symlinkSync(outside, path.join(repositoryRoot, "vendor"), "dir");
  const importer = createContractSnapshotImporter({ contract: source.contract });

  assertImportError(
    () => importer({ sourceRepository: source.directory, repositoryRoot }),
    "contract-snapshot.destination-parent-invalid"
  );
  assert.equal(existsSync(path.join(outside, "miku-project-contract")), false);
});

test("generates byte-identical SOURCE.json for independent installations", () => {
  const source = createSourceRepository();
  const firstRoot = createTemporaryDirectory("miku-project-contract-java-root-");
  const secondRoot = createTemporaryDirectory("miku-project-contract-java-root-");
  const importer = createContractSnapshotImporter({ contract: source.contract });

  importer({ sourceRepository: source.directory, repositoryRoot: firstRoot });
  importer({ sourceRepository: source.directory, repositoryRoot: secondRoot });
  const firstManifest = readFileSync(path.join(firstRoot, source.contract.defaultOutputDirectory, "SOURCE.json"));
  const secondManifest = readFileSync(path.join(secondRoot, source.contract.defaultOutputDirectory, "SOURCE.json"));
  assert.deepEqual(secondManifest, firstManifest);
});

test("rejects a symlink Git tree entry instead of materializing it", () => {
  const source = createSourceRepository({ symlinkedFixture: true });
  const repositoryRoot = createTemporaryDirectory("miku-project-contract-java-root-");
  const importer = createContractSnapshotImporter({ contract: source.contract });

  assertImportError(
    () => importer({ sourceRepository: source.directory, repositoryRoot }),
    "contract-snapshot.allowlist-invalid"
  );
  assert.equal(existsSync(path.join(repositoryRoot, "vendor")), false);
});

test("cleans up a destination after marker, member, or SOURCE write failure", () => {
  for (const failureKind of ["install-marker", "second-member", "source-manifest"]) {
    const source = createSourceRepository();
    const repositoryRoot = createTemporaryDirectory("miku-project-contract-java-root-");
    const snapshot = path.join(repositoryRoot, source.contract.defaultOutputDirectory);
    let memberWrites = 0;
    const importer = createContractSnapshotImporter({
      contract: source.contract,
      installFileWriter(filePath, bytes, context) {
        if (context.kind === "install-marker" && failureKind === "install-marker") {
          throw new Error("injected install-marker failure");
        }
        if (context.kind === "snapshot-member") {
          memberWrites += 1;
          if (failureKind === "second-member" && memberWrites === 2) {
            throw new Error("injected member failure");
          }
        }
        if (context.kind === "source-manifest" && failureKind === "source-manifest") {
          throw new Error("injected SOURCE failure");
        }
        writeTestRegularFile(filePath, bytes);
      }
    });

    assert.throws(() => importer({ sourceRepository: source.directory, repositoryRoot }), /injected/);
    assert.equal(existsSync(snapshot), false, failureKind);
    const retry = createContractSnapshotImporter({ contract: source.contract });
    assert.equal(retry({ sourceRepository: source.directory, repositoryRoot }).snapshot_action, "installed", failureKind);
  }
});

test("preserves an unknown destination member after an interrupted install", () => {
  const source = createSourceRepository();
  const repositoryRoot = createTemporaryDirectory("miku-project-contract-java-root-");
  const snapshot = path.join(repositoryRoot, source.contract.defaultOutputDirectory);
  const importer = createContractSnapshotImporter({
    contract: source.contract,
    installFileWriter(filePath, bytes, context) {
      writeTestRegularFile(filePath, bytes);
      if (context.kind === "snapshot-member") {
        writeFileSync(path.join(snapshot, "third-party.txt"), "do not delete\n", { flag: "wx" });
        throw new Error("injected foreign-member failure");
      }
    }
  });

  assert.throws(() => importer({ sourceRepository: source.directory, repositoryRoot }), /foreign-member/);
  assert.equal(readFileSync(path.join(snapshot, "third-party.txt"), "utf8"), "do not delete\n");
  assert.equal(existsSync(path.join(snapshot, ".miku-project-contract-install-marker")), true);
  const retry = createContractSnapshotImporter({ contract: source.contract });
  assertImportError(
    () => retry({ sourceRepository: source.directory, repositoryRoot }),
    "contract-snapshot.destination-conflict"
  );
});

test("preserves an unknown empty directory after an interrupted install", () => {
  const source = createSourceRepository();
  const repositoryRoot = createTemporaryDirectory("miku-project-contract-java-root-");
  const snapshot = path.join(repositoryRoot, source.contract.defaultOutputDirectory);
  const importer = createContractSnapshotImporter({
    contract: source.contract,
    installFileWriter(filePath, bytes, context) {
      writeTestRegularFile(filePath, bytes);
      if (context.kind === "snapshot-member") {
        mkdirSync(path.join(snapshot, "third-party-empty"));
        throw new Error("injected foreign-directory failure");
      }
    }
  });

  assert.throws(() => importer({ sourceRepository: source.directory, repositoryRoot }), /foreign-directory/);
  assert.equal(statSync(path.join(snapshot, "third-party-empty")).isDirectory(), true);
  assert.equal(existsSync(path.join(snapshot, ".miku-project-contract-install-marker")), true);
  const retry = createContractSnapshotImporter({ contract: source.contract });
  assertImportError(
    () => retry({ sourceRepository: source.directory, repositoryRoot }),
    "contract-snapshot.destination-conflict"
  );
});

function createSourceRepository({ symlinkedFixture = false } = {}) {
  const directory = createTemporaryDirectory("miku-project-contract-source-");
  mkdirSync(path.join(directory, "docs"), { recursive: true });
  mkdirSync(path.join(directory, "testdata/conformance/v1"), { recursive: true });
  writeFileSync(path.join(directory, "docs/contract.md"), "tagged contract\n");
  writeFileSync(path.join(directory, "testdata/conformance/v1/fixture.json"), "{\"fixture\":true}\n");
  if (symlinkedFixture) {
    symlinkSync("fixture.json", path.join(directory, "testdata/conformance/v1/link.json"));
  }
  git(directory, ["init", "-q"]);
  git(directory, ["add", "."]);
  git(directory, ["-c", "user.name=Contract Test", "-c", "user.email=contract-test@example.invalid", "commit", "-qm", "contract fixture"]);
  git(directory, ["tag", "v-contract"]);
  const revision = git(directory, ["rev-parse", "v-contract^{commit}"]).trim();
  writeFileSync(path.join(directory, "docs/contract.md"), "alternate contract\n");
  writeFileSync(path.join(directory, "testdata/conformance/v1/fixture.json"), "{\"fixture\":\"alternate\"}\n");
  git(directory, ["add", "."]);
  git(directory, ["-c", "user.name=Contract Test", "-c", "user.email=contract-test@example.invalid", "commit", "-qm", "alternate fixture"]);
  const alternateRevision = git(directory, ["rev-parse", "HEAD"]).trim();
  writeFileSync(path.join(directory, "docs/contract.md"), "working tree change\n");

  const fixtureBytes = Buffer.from("{\"fixture\":true}\n", "utf8");
  const corpusDigest = sha256(Buffer.from(`${sha256(fixtureBytes)}  fixture.json\n`, "utf8"));
  return {
    directory,
    alternateRevision,
    contract: {
      contractVersion: "test-1",
      fixtureSuiteVersion: "test-1",
      nodeReferenceRelease: "test-node-1",
      sourceRepository: "https://example.invalid/miku-project-test",
      sourceTag: "v-contract",
      sourceRevision: revision,
      corpusDigest,
      gateRuntimeLockDigest: "1".repeat(64),
      defaultOutputDirectory: "vendor/miku-project-contract/test-1",
      contractDocuments: ["docs/contract.md"],
      directoryPrefixes: ["testdata/conformance/v1/"]
    }
  };
}

function createTemporaryDirectory(prefix) {
  const directory = mkdtempSync(path.join(os.tmpdir(), prefix));
  TEMPORARY_DIRECTORIES.push(directory);
  return directory;
}

function git(directory, args) {
  return execFileSync("git", args, { cwd: directory, encoding: "utf8" });
}

function assertImportError(action, expectedCode) {
  assert.throws(action, (error) => error instanceof ContractSnapshotImportError && error.code === expectedCode);
}

function snapshotInventory(root) {
  const entries = [];
  visit(root, "", entries);
  return entries.sort((left, right) => left.path.localeCompare(right.path));
}

function snapshotMetadata(root) {
  const entries = [];
  visitMetadata(root, ".", entries);
  return entries.sort((left, right) => left.path.localeCompare(right.path));
}

function visitMetadata(entryPath, relative, entries) {
  const stat = statSync(entryPath, { bigint: true });
  const entry = {
    path: relative,
    type: stat.isDirectory() ? "directory" : "file",
    inode: stat.ino.toString(),
    mode: stat.mode.toString(),
    size: stat.size.toString(),
    mtimeNs: stat.mtimeNs.toString(),
    ctimeNs: stat.ctimeNs.toString()
  };
  if (stat.isFile()) {
    entry.digest = sha256(readFileSync(entryPath));
  }
  entries.push(entry);
  if (stat.isDirectory()) {
    for (const child of readdirSync(entryPath, { withFileTypes: true })) {
      const childRelative = relative === "." ? child.name : `${relative}/${child.name}`;
      visitMetadata(path.join(entryPath, child.name), childRelative, entries);
    }
  }
}

function visit(directory, relativeDirectory, entries) {
  for (const entry of readdirSync(directory, { withFileTypes: true })) {
    const relative = relativeDirectory ? `${relativeDirectory}/${entry.name}` : entry.name;
    const entryPath = path.join(directory, entry.name);
    if (entry.isDirectory()) {
      visit(entryPath, relative, entries);
    } else {
      entries.push({ path: relative, digest: sha256(readFileSync(entryPath)) });
    }
  }
}

function writeTestRegularFile(filePath, bytes) {
  mkdirSync(path.dirname(filePath), { recursive: true });
  writeFileSync(filePath, bytes, { flag: "wx", mode: 0o644 });
}

function sha256(bytes) {
  return createHash("sha256").update(bytes).digest("hex");
}
