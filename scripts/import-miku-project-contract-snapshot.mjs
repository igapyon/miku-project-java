#!/usr/bin/env node

import {
  lstatSync,
  mkdirSync,
  mkdtempSync,
  readFileSync,
  readdirSync,
  realpathSync,
  rmSync,
  rmdirSync,
  unlinkSync,
  writeFileSync
} from "node:fs";
import os from "node:os";
import path from "node:path";
import { createHash, randomBytes } from "node:crypto";
import { execFileSync } from "node:child_process";
import { fileURLToPath } from "node:url";

const REPOSITORY_ROOT = path.resolve(import.meta.dirname, "..");
const PRODUCTION_CONTRACT = Object.freeze({
  contractVersion: "1",
  fixtureSuiteVersion: "1",
  nodeReferenceRelease: "1.0.3",
  sourceRepository: "https://github.com/igapyon/miku-project",
  sourceTag: "v1.0.3",
  sourceRevision: "693b4ecd7d4328d77f3b2eada9c4965a9c9b15f5",
  corpusDigest: "f0b4a821f80f155ba01afc1fdf7c5d2fa6ca5e744bbd0090819e0d591f1c47a3",
  gateRuntimeLockDigest: "95cd11cc4460348fa066908994430adba5983384c06c75679855120e5c5ea3d5",
  defaultOutputDirectory: "vendor/miku-project-contract/v1.0.3",
  contractDocuments: Object.freeze([
    "docs/miku-project-semantic-contract-v1.md",
    "docs/miku-project-semantic-fixture-catalog-v1.md",
    "docs/miku-project-format-and-loss-contract-v1.md",
    "docs/miku-project-change-contract-v1.md",
    "docs/miku-project-cli-contract-v1.md",
    "docs/miku-project-cli-result-contract-v1.md",
    "docs/miku-project-runtime-capability-contract-v1.md",
    "docs/miku-project-runtime-manifest-contract-v1.md",
    "docs/miku-project-conformance-corpus-v1.md",
    "docs/miku-project-human-gate-and-next-action-contract-v1.md"
  ]),
  directoryPrefixes: Object.freeze([
    "docs/schemas/",
    "docs/examples/artifacts-v1/",
    "docs/examples/cli-v1/",
    "docs/examples/runtime-manifest-v1/",
    "testdata/conformance/v1/"
  ])
});

const SYSTEM_GIT_READER = Object.freeze({
  resolveCommit({ sourceRoot, sourceTag }) {
    return execGit(sourceRoot, ["rev-parse", `${sourceTag}^{commit}`]).toString("utf8").trim();
  },
  listTreeEntries({ sourceRoot, sourceRevision, paths }) {
    const bytes = execGit(sourceRoot, [
      "ls-tree", "-r", "-z", sourceRevision, "--", ...paths
    ]);
    return parseGitTreeEntries(bytes);
  },
  readBlob({ sourceRoot, sourceRevision, sourcePath }) {
    return execGit(sourceRoot, ["show", `${sourceRevision}:${sourcePath}`]);
  }
});

export class ContractSnapshotImportError extends Error {
  constructor(code, message) {
    super(message);
    this.name = "ContractSnapshotImportError";
    this.code = code;
  }
}

const DEFAULT_IMPORTER = createContractSnapshotImporter();

if (isMainModule()) {
  try {
    main();
  } catch (error) {
    const code = error instanceof ContractSnapshotImportError ? `${error.code}: ` : "";
    process.stderr.write(`${code}${error?.message ?? String(error)}\n`);
    process.exitCode = 1;
  }
}

/**
 * Creates an importer for a fixed contract identity. The public CLI always
 * uses the production v1.0.3 identity; the factory exists solely so its
 * Git-object and filesystem boundary can be tested with a synthetic Git repo.
 */
export function createContractSnapshotImporter({
  contract = PRODUCTION_CONTRACT,
  gitReader = SYSTEM_GIT_READER,
  afterSourceIdentity = () => {},
  installFileWriter = writeNewRegularFile
} = {}) {
  validateContractDefinition(contract);
  validateGitReader(gitReader);
  validateTestHook(afterSourceIdentity, "afterSourceIdentity");
  validateTestHook(installFileWriter, "installFileWriter");

  return function importContractSnapshot({
    sourceRepository,
    outputDirectory = contract.defaultOutputDirectory,
    repositoryRoot = REPOSITORY_ROOT
  } = {}) {
    const root = requireCanonicalDirectory(repositoryRoot, "Java repository");
    const sourceRoot = requireDirectory(sourceRepository, "source repository");
    const lexicalDestination = resolveOutputDirectory({ root, outputDirectory });

    const sourceRevision = assertSourceIdentity({ sourceRoot, contract, gitReader });
    afterSourceIdentity(Object.freeze({ sourceRoot, sourceTag: contract.sourceTag, sourceRevision }));
    const sourceEntries = listAllowlistedSourceEntries({ sourceRoot, sourceRevision, contract, gitReader });
    const destination = prepareSafeDestination({ root, lexicalDestination });
    const staging = mkdtempSync(path.join(path.dirname(destination), `.${path.basename(destination)}.staging-`));

    try {
      const members = sourceEntries.map((entry) => materializeMember({
        sourceRoot,
        sourceRevision,
        sourceEntry: entry,
        staging,
        contract,
        gitReader
      }));
      const corpusDigest = computeCorpusDigest({ staging, members });
      if (corpusDigest !== contract.corpusDigest) {
        throw importFailure("contract-snapshot.corpus-mismatch", "The source tag does not match the expected conformance corpus.");
      }

      const manifest = createSourceManifest({ contract, members, corpusDigest });
      const manifestBytes = Buffer.from(`${canonicalJsonText(manifest)}\n`, "utf8");
      const existingState = inspectDestination({ destination, members, manifestBytes });
      if (existingState === "identical") {
        return createResult({ contract, destination, members, corpusDigest, snapshotAction: "unchanged" });
      }
      if (existingState === "conflict") {
        throw destinationConflict();
      }

      if (!createDestinationDirectory(destination)) {
        const racedState = inspectDestination({ destination, members, manifestBytes });
        if (racedState === "identical") {
          return createResult({ contract, destination, members, corpusDigest, snapshotAction: "unchanged" });
        }
        throw destinationConflict();
      }

      installStagedSnapshot({ destination, staging, members, manifestBytes, installFileWriter });
      return createResult({ contract, destination, members, corpusDigest, snapshotAction: "installed" });
    } finally {
      rmSync(staging, { recursive: true, force: true });
    }
  };
}

/**
 * Imports only the exact, allowlisted v1.0.3 Git object. It never reads the
 * source working tree and never changes the legacy vendor/mikuproject subtree.
 */
export function importMikuProjectContractSnapshot(options) {
  return DEFAULT_IMPORTER(options);
}

function main() {
  const options = parseArgs(process.argv.slice(2));
  const result = importMikuProjectContractSnapshot(options);
  process.stdout.write(`${canonicalJsonText(result)}\n`);
}

function parseArgs(argv) {
  const values = new Map();
  for (let index = 0; index < argv.length; index += 2) {
    const option = argv[index];
    const value = argv[index + 1];
    if (!new Set(["--source-repo", "--out-dir"]).has(option) || !isNonEmptyString(value) || values.has(option)) {
      throw new Error("usage: node scripts/import-miku-project-contract-snapshot.mjs --source-repo <miku-project-directory> [--out-dir <repository-relative-directory>]");
    }
    values.set(option, value);
  }
  if (!values.has("--source-repo")) {
    throw new Error("usage: node scripts/import-miku-project-contract-snapshot.mjs --source-repo <miku-project-directory> [--out-dir <repository-relative-directory>]");
  }
  return {
    sourceRepository: values.get("--source-repo"),
    outputDirectory: values.get("--out-dir") ?? PRODUCTION_CONTRACT.defaultOutputDirectory
  };
}

function assertSourceIdentity({ sourceRoot, contract, gitReader }) {
  let revision;
  try {
    revision = gitReader.resolveCommit({ sourceRoot, sourceTag: contract.sourceTag });
  } catch {
    throw importFailure("contract-snapshot.source-tag-unavailable", `The required source tag ${contract.sourceTag} is unavailable.`);
  }
  if (revision !== contract.sourceRevision) {
    throw importFailure("contract-snapshot.source-revision-mismatch", `The required source tag ${contract.sourceTag} does not identify the expected revision.`);
  }
  return revision;
}

function listAllowlistedSourceEntries({ sourceRoot, sourceRevision, contract, gitReader }) {
  let entries;
  try {
    entries = gitReader.listTreeEntries({
      sourceRoot,
      sourceRevision,
      paths: [...contract.contractDocuments, ...contract.directoryPrefixes.map((prefix) => prefix.slice(0, -1))]
    });
  } catch {
    throw importFailure("contract-snapshot.source-tree-unavailable", "Unable to list the fixed contract source tree.");
  }
  const sortedEntries = [...entries].sort((left, right) => compareUnicodeScalars(left.path, right.path));
  const paths = sortedEntries.map((entry) => entry.path);
  if (paths.length === 0 || new Set(paths).size !== paths.length || !sortedEntries.every((entry) => isAllowlistedRegularFile(entry, contract))) {
    throw importFailure("contract-snapshot.allowlist-invalid", "The fixed source tree does not match the v1 contract allowlist of regular files.");
  }
  for (const requiredPath of contract.contractDocuments) {
    if (!paths.includes(requiredPath)) {
      throw importFailure("contract-snapshot.required-member-missing", `The required contract document is missing: ${requiredPath}`);
    }
  }
  for (const prefix of contract.directoryPrefixes) {
    if (!paths.some((candidate) => candidate.startsWith(prefix))) {
      throw importFailure("contract-snapshot.required-directory-empty", `The required contract directory is empty: ${prefix}`);
    }
  }
  return sortedEntries;
}

function materializeMember({ sourceRoot, sourceRevision, sourceEntry, staging, contract, gitReader }) {
  const sourcePath = sourceEntry.path;
  const destinationPath = path.join(staging, sourcePath);
  const relative = path.relative(staging, destinationPath);
  if (!isSafeRelativePath(relative) || relative !== sourcePath) {
    throw importFailure("contract-snapshot.member-path-invalid", `The source member path is unsafe: ${sourcePath}`);
  }
  let bytes;
  try {
    bytes = Buffer.from(gitReader.readBlob({ sourceRoot, sourceRevision, sourcePath }));
  } catch {
    throw importFailure("contract-snapshot.member-unavailable", `Unable to read fixed source member: ${sourcePath}`);
  }
  writeNewRegularFile(destinationPath, bytes);
  return Object.freeze({
    path: sourcePath,
    size_bytes: bytes.length,
    digest: digestRecord(sha256(bytes))
  });
}

function computeCorpusDigest({ staging, members }) {
  const lines = members
    .filter((member) => member.path.startsWith("testdata/conformance/v1/"))
    .sort((left, right) => compareUnicodeScalars(left.path, right.path))
    .map((member) => {
      const relativePath = member.path.slice("testdata/conformance/v1/".length);
      const bytes = readFileSync(path.join(staging, member.path));
      return `${sha256(bytes)}  ${relativePath}\n`;
    });
  return sha256(Buffer.from(lines.join(""), "utf8"));
}

function createSourceManifest({ contract, members, corpusDigest }) {
  return {
    conformance: {
      corpus_digest: digestRecord(corpusDigest),
      fixture_suite_version: contract.fixtureSuiteVersion
    },
    contract_version: contract.contractVersion,
    gate: "G4",
    kind: "miku_project_contract_snapshot",
    members: [...members].sort((left, right) => compareUnicodeScalars(left.path, right.path)),
    node_reference: {
      release_version: contract.nodeReferenceRelease,
      runtime_family: "node",
      runtime_role: "reference",
      runtime_version: contract.nodeReferenceRelease
    },
    schema_version: "1",
    source: {
      repository: contract.sourceRepository,
      revision: contract.sourceRevision,
      tag: contract.sourceTag
    },
    source_gate_runtime_lock_digest: digestRecord(contract.gateRuntimeLockDigest)
  };
}

function createResult({ contract, destination, members, corpusDigest, snapshotAction }) {
  return Object.freeze({
    kind: "miku_project_contract_snapshot_import",
    schema_version: "1",
    status: "succeeded",
    snapshot_action: snapshotAction,
    destination,
    source: {
      repository: contract.sourceRepository,
      revision: contract.sourceRevision,
      tag: contract.sourceTag
    },
    member_count: members.length,
    corpus_digest: digestRecord(corpusDigest)
  });
}

function resolveOutputDirectory({ root, outputDirectory }) {
  if (!isNonEmptyString(outputDirectory)) {
    throw new TypeError("outputDirectory must be a non-empty string");
  }
  const destination = path.resolve(root, outputDirectory);
  const relative = path.relative(root, destination);
  if (!isSafeRelativePath(relative)) {
    throw importFailure("contract-snapshot.destination-invalid", "The output directory must be a descendant of the Java repository.");
  }
  return destination;
}

function prepareSafeDestination({ root, lexicalDestination }) {
  const destinationParent = path.dirname(lexicalDestination);
  const relativeParent = path.relative(root, destinationParent);
  if (relativeParent && !isSafeRelativePath(relativeParent)) {
    throw importFailure("contract-snapshot.destination-invalid", "The output directory must be a descendant of the Java repository.");
  }
  let current = root;
  for (const segment of relativeParent ? relativeParent.split(path.sep) : []) {
    current = path.join(current, segment);
    ensureNonSymlinkDirectory(current);
  }
  const canonicalParent = realpathSync(current);
  const canonicalRelative = path.relative(root, canonicalParent);
  if (canonicalRelative && !isSafeRelativePath(canonicalRelative)) {
    throw importFailure("contract-snapshot.destination-parent-invalid", "The output directory parent resolves outside the Java repository.");
  }
  return path.join(canonicalParent, path.basename(lexicalDestination));
}

function ensureNonSymlinkDirectory(directory) {
  let entry;
  try {
    entry = lstatSync(directory);
  } catch (error) {
    if (error?.code !== "ENOENT") {
      throw importFailure("contract-snapshot.destination-parent-unavailable", "The output directory parent cannot be inspected.");
    }
    try {
      mkdirSync(directory);
      entry = lstatSync(directory);
    } catch {
      throw importFailure("contract-snapshot.destination-parent-unavailable", "The output directory parent cannot be created safely.");
    }
  }
  if (entry.isSymbolicLink() || !entry.isDirectory()) {
    throw importFailure("contract-snapshot.destination-parent-invalid", "The output directory parent must be a non-symlink directory.");
  }
}

function inspectDestination({ destination, members, manifestBytes, transientFiles = [] }) {
  let entry;
  try {
    entry = lstatSync(destination);
  } catch (error) {
    if (error?.code === "ENOENT") return "missing";
    throw importFailure("contract-snapshot.destination-unavailable", "The output directory cannot be inspected.");
  }
  if (entry.isSymbolicLink() || !entry.isDirectory()) return "conflict";

  let actualTree;
  try {
    actualTree = collectSnapshotTree(destination);
  } catch {
    return "conflict";
  }
  const expectedPaths = [...members.map((member) => member.path), "SOURCE.json", ...transientFiles.map((file) => file.path)]
    .sort(compareUnicodeScalars);
  const actualFiles = actualTree.files;
  const actualPaths = [...actualFiles.keys()].sort(compareUnicodeScalars);
  if (expectedPaths.length !== actualPaths.length || expectedPaths.some((expected, index) => expected !== actualPaths[index])) {
    return "conflict";
  }
  const expectedDirectories = expectedDirectoryPaths(expectedPaths);
  if (!sameSortedPaths(expectedDirectories, actualTree.directories)) return "conflict";
  const sourceManifest = actualFiles.get("SOURCE.json");
  if (!sourceManifest || !sourceManifest.bytes.equals(manifestBytes)) return "conflict";
  for (const member of members) {
    const actual = actualFiles.get(member.path);
    if (!actual || actual.bytes.length !== member.size_bytes || sha256(actual.bytes) !== member.digest.value) {
      return "conflict";
    }
  }
  for (const transientFile of transientFiles) {
    const actual = actualFiles.get(transientFile.path);
    if (!actual || !actual.bytes.equals(transientFile.bytes)) return "conflict";
  }
  return "identical";
}

function collectSnapshotTree(directory) {
  const files = new Map();
  const directories = new Set();
  visitDirectory(directory, "", files, directories);
  return { files, directories };
}

function visitDirectory(directory, relativeDirectory, files, directories) {
  const entries = readdirSync(directory, { withFileTypes: true })
    .sort((left, right) => compareUnicodeScalars(left.name, right.name));
  for (const entry of entries) {
    const relative = relativeDirectory ? `${relativeDirectory}/${entry.name}` : entry.name;
    if (!isSafeSnapshotPath(relative)) {
      throw new Error("unsafe snapshot path");
    }
    const entryPath = path.join(directory, entry.name);
    const stat = lstatSync(entryPath);
    if (stat.isSymbolicLink()) {
      throw new Error("snapshot symlink");
    }
    if (stat.isDirectory()) {
      directories.add(relative);
      visitDirectory(entryPath, relative, files, directories);
    } else if (stat.isFile()) {
      files.set(relative, { bytes: readFileSync(entryPath) });
    } else {
      throw new Error("snapshot non-regular member");
    }
  }
}

function createDestinationDirectory(destination) {
  try {
    mkdirSync(destination, { mode: 0o755 });
    return true;
  } catch (error) {
    if (error?.code === "EEXIST") return false;
    throw importFailure("contract-snapshot.destination-unavailable", "The output directory cannot be reserved safely.");
  }
}

function installStagedSnapshot({ destination, staging, members, manifestBytes, installFileWriter }) {
  const markerPath = path.join(destination, ".miku-project-contract-install-marker");
  const markerRelativePath = ".miku-project-contract-install-marker";
  const markerBytes = Buffer.from(randomBytes(32).toString("hex"), "utf8");
  let markerWritten = false;
  let expectedFiles = [];
  try {
    installFileWriter(markerPath, markerBytes, Object.freeze({ kind: "install-marker", relativePath: markerRelativePath }));
    markerWritten = true;
    expectedFiles = [
      ...members.map((member) => ({ path: member.path, bytes: readFileSync(path.join(staging, member.path)) })),
      { path: "SOURCE.json", bytes: manifestBytes }
    ];
    for (const file of expectedFiles) {
      installFileWriter(path.join(destination, file.path), file.bytes, Object.freeze({
        kind: file.path === "SOURCE.json" ? "source-manifest" : "snapshot-member",
        relativePath: file.path
      }));
    }
    if (inspectDestination({
      destination,
      members,
      manifestBytes,
      transientFiles: [{ path: markerRelativePath, bytes: markerBytes }]
    }) !== "identical") {
      throw importFailure("contract-snapshot.install-verification-failed", "The installed contract snapshot did not pass its own inventory verification.");
    }
    unlinkSync(markerPath);
  } catch (error) {
    cleanupFailedInstallation({ destination, markerPath, markerBytes, expectedFiles, markerWritten });
    throw error;
  }
}

function cleanupFailedInstallation({ destination, markerPath, markerBytes, expectedFiles, markerWritten }) {
  if (markerWritten || hasOwnedMarker(markerPath, markerBytes)) {
    cleanupIncompleteInstallation({ destination, markerPath, markerBytes, expectedFiles });
  } else {
    cleanupEmptyDestination(destination);
  }
}

function hasOwnedMarker(markerPath, markerBytes) {
  try {
    const marker = lstatSync(markerPath);
    return marker.isFile() && readFileSync(markerPath).equals(markerBytes);
  } catch {
    return false;
  }
}

function cleanupIncompleteInstallation({ destination, markerPath, markerBytes, expectedFiles }) {
  try {
    if (!hasOwnedMarker(markerPath, markerBytes)) return;
    const actualTree = collectSnapshotTree(destination);
    const expected = new Map([
      ...expectedFiles.map((file) => [file.path, file.bytes]),
      [".miku-project-contract-install-marker", markerBytes]
    ]);
    if ([...actualTree.files.entries()].some(([filePath, file]) => !expected.get(filePath)?.equals(file.bytes))) {
      return;
    }
    const allowedDirectories = expectedDirectoryPaths([...expected.keys()]);
    if ([...actualTree.directories].some((directoryPath) => !allowedDirectories.has(directoryPath))) return;

    for (const [filePath, file] of actualTree.files.entries()) {
      if (filePath === ".miku-project-contract-install-marker") continue;
      unlinkExpectedRegularFile(path.join(destination, ...filePath.split("/")), file.bytes);
    }
    const deepestFirst = [...actualTree.directories].sort((left, right) => {
      const depthDifference = right.split("/").length - left.split("/").length;
      return depthDifference || compareUnicodeScalars(right, left);
    });
    for (const directoryPath of deepestFirst) {
      rmdirSync(path.join(destination, ...directoryPath.split("/")));
    }
    unlinkExpectedRegularFile(markerPath, markerBytes);
    rmdirSync(destination);
  } catch {
    // Preserve an uncertain destination rather than risk deleting another actor's files.
  }
}

function unlinkExpectedRegularFile(filePath, expectedBytes) {
  const entry = lstatSync(filePath);
  if (entry.isSymbolicLink() || !entry.isFile() || !readFileSync(filePath).equals(expectedBytes)) {
    throw new Error("owned snapshot file changed before cleanup");
  }
  unlinkSync(filePath);
}

function expectedDirectoryPaths(filePaths) {
  const directories = new Set();
  for (const filePath of filePaths) {
    const segments = filePath.split("/");
    for (let length = 1; length < segments.length; length += 1) {
      directories.add(segments.slice(0, length).join("/"));
    }
  }
  return directories;
}

function sameSortedPaths(left, right) {
  const leftPaths = [...left].sort(compareUnicodeScalars);
  const rightPaths = [...right].sort(compareUnicodeScalars);
  return leftPaths.length === rightPaths.length
    && leftPaths.every((candidate, index) => candidate === rightPaths[index]);
}

function cleanupEmptyDestination(destination) {
  try {
    rmdirSync(destination);
  } catch {
    // A non-empty or externally changed destination is preserved fail-closed.
  }
}

function writeNewRegularFile(filePath, bytes) {
  mkdirSync(path.dirname(filePath), { recursive: true });
  writeFileSync(filePath, bytes, { flag: "wx", mode: 0o644 });
}

function isAllowlistedRegularFile(entry, contract) {
  return isAllowlistedPath(entry.path, contract)
    && entry.type === "blob"
    && (entry.mode === "100644" || entry.mode === "100755");
}

function isAllowlistedPath(candidate, contract) {
  return contract.contractDocuments.includes(candidate)
    || contract.directoryPrefixes.some((prefix) => candidate.startsWith(prefix));
}

function isSafeRelativePath(candidate) {
  return isNonEmptyString(candidate)
    && !path.isAbsolute(candidate)
    && candidate !== ".."
    && !candidate.startsWith(`..${path.sep}`)
    && !candidate.split(path.sep).includes("..");
}

function isSafeSnapshotPath(candidate) {
  return isNonEmptyString(candidate)
    && !candidate.startsWith("/")
    && !candidate.split("/").includes("..")
    && !candidate.split("/").includes("");
}

function requireDirectory(directory, name) {
  if (!isNonEmptyString(directory)) {
    throw new TypeError(`${name} must be a non-empty string`);
  }
  const resolved = path.resolve(directory);
  let entry;
  try {
    entry = lstatSync(resolved);
  } catch {
    throw importFailure("contract-snapshot.source-unavailable", `${name} is missing or unreadable.`);
  }
  if (entry.isSymbolicLink() || !entry.isDirectory()) {
    throw importFailure("contract-snapshot.source-invalid", `${name} must be a non-symlink directory.`);
  }
  return resolved;
}

function requireCanonicalDirectory(directory, name) {
  return realpathSync(requireDirectory(directory, name));
}

function parseGitTreeEntries(bytes) {
  return Buffer.from(bytes).toString("utf8").split("\0").filter(Boolean).map((line) => {
    const match = /^(\d+) (\w+) [0-9a-f]+\t([\s\S]+)$/u.exec(line);
    if (!match) throw new Error("invalid git ls-tree entry");
    return Object.freeze({ mode: match[1], type: match[2], path: match[3] });
  });
}

function execGit(sourceRoot, args) {
  return execFileSync("git", args, {
    cwd: sourceRoot,
    encoding: "buffer",
    maxBuffer: 32 * 1024 * 1024,
    stdio: ["ignore", "pipe", "ignore"]
  });
}

function validateContractDefinition(contract) {
  const requiredStrings = [
    "contractVersion", "fixtureSuiteVersion", "nodeReferenceRelease", "sourceRepository",
    "sourceTag", "sourceRevision", "corpusDigest", "gateRuntimeLockDigest", "defaultOutputDirectory"
  ];
  if (!contract || typeof contract !== "object" || !requiredStrings.every((key) => isNonEmptyString(contract[key]))) {
    throw new TypeError("contract must contain the fixed identity fields");
  }
  if (!Array.isArray(contract.contractDocuments) || !Array.isArray(contract.directoryPrefixes)
    || !contract.contractDocuments.every(isNonEmptyString) || !contract.directoryPrefixes.every((prefix) => isNonEmptyString(prefix) && prefix.endsWith("/"))) {
    throw new TypeError("contract must contain document and directory allowlists");
  }
}

function validateGitReader(gitReader) {
  if (!gitReader || typeof gitReader.resolveCommit !== "function" || typeof gitReader.listTreeEntries !== "function" || typeof gitReader.readBlob !== "function") {
    throw new TypeError("gitReader must provide resolveCommit, listTreeEntries, and readBlob");
  }
}

function validateTestHook(value, name) {
  if (typeof value !== "function") {
    throw new TypeError(`${name} must be a function`);
  }
}

function destinationConflict() {
  return importFailure("contract-snapshot.destination-conflict", "The contract snapshot directory already exists but is not the exact expected immutable inventory.");
}

function digestRecord(value) {
  return { algorithm: "sha-256", value };
}

function sha256(bytes) {
  return createHash("sha256").update(bytes).digest("hex");
}

function canonicalJsonText(value) {
  if (value === null || typeof value === "boolean" || typeof value === "number" || typeof value === "string") {
    if (typeof value === "number" && (!Number.isSafeInteger(value) || !Number.isFinite(value))) {
      throw new TypeError("canonical JSON only permits safe integer numbers");
    }
    return JSON.stringify(value);
  }
  if (Array.isArray(value)) {
    return `[${value.map(canonicalJsonText).join(",")}]`;
  }
  if (value && typeof value === "object") {
    return `{${Object.keys(value).sort(compareUnicodeScalars).map((key) => `${canonicalJsonText(key)}:${canonicalJsonText(value[key])}`).join(",")}}`;
  }
  throw new TypeError("value is outside the canonical JSON domain");
}

function compareUnicodeScalars(left, right) {
  const leftPoints = Array.from(left);
  const rightPoints = Array.from(right);
  const length = Math.min(leftPoints.length, rightPoints.length);
  for (let index = 0; index < length; index += 1) {
    const difference = leftPoints[index].codePointAt(0) - rightPoints[index].codePointAt(0);
    if (difference !== 0) return difference;
  }
  return leftPoints.length - rightPoints.length;
}

function isNonEmptyString(value) {
  return typeof value === "string" && value.length > 0;
}

function importFailure(code, message) {
  return new ContractSnapshotImportError(code, message);
}

function isMainModule() {
  return process.argv[1] && path.resolve(process.argv[1]) === fileURLToPath(import.meta.url);
}
