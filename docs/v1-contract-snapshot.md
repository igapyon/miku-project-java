# v1 Contract Snapshot

## Purpose

The v1 Java CLI does not use the moving `vendor/mikuproject` subtree as its
contract authority. It consumes the immutable snapshot at
`vendor/miku-project-contract/v1.0.3/` instead.

The snapshot is the Java-side input for the Gate G4-approved Node reference
contract. It contains approved contract documents, JSON Schema, examples, and
the shared conformance fixture/golden corpus. It is not a Node runtime and is
not a public Release artifact.

## Fixed identity

- contract version: `1`
- fixture suite version: `1`
- Node reference release: `1.0.3`
- source repository: `https://github.com/igapyon/miku-project`
- source tag: `v1.0.3`
- source revision: `693b4ecd7d4328d77f3b2eada9c4965a9c9b15f5`
- conformance corpus SHA-256: `f0b4a821f80f155ba01afc1fdf7c5d2fa6ca5e744bbd0090819e0d591f1c47a3`
- Gate runtime lock SHA-256: `95cd11cc4460348fa066908994430adba5983384c06c75679855120e5c5ea3d5`
- `SOURCE.json` SHA-256: `a9eebb6db384680de23a98cb90d887a63cabd8f1ef999f3ebf85bd48628c0807`

`SOURCE.json` is canonical JSON and records every imported member with its
relative path, byte size, and SHA-256. Its raw digest is separately pinned in
the Java verifier, so replacing both a member and its inventory is also
rejected. The verifier derives the exact directory topology from member paths
and rejects a missing, extra, symlinked, resized, or digest-mismatched member,
as well as an unexpected empty directory, before a v1 command test uses the
snapshot.

## Initial import

Run the importer from a Java repository whose destination directory is either
absent or already the exact immutable inventory for this release.

```sh
node scripts/import-miku-project-contract-snapshot.mjs \
  --source-repo /absolute/path/to/miku-project
```

The importer resolves the exact `v1.0.3` tag once, checks its full revision,
then reads every tree entry and blob by that full revision; it does not copy the
source repository working tree or follow a later tag movement. It refuses a
source tag/revision mismatch, repository-external destination, and symlinked
destination parent. It reserves a new destination through exclusive directory
creation, so it never replaces an existing snapshot directory. A completely
identical existing snapshot is a successful no-op (`"snapshot_action":"unchanged"`)
with its inode, mode, size, mtime, and ctime unchanged; any other existing tree
is `contract-snapshot.destination-conflict` and remains untouched.

After reserving a new destination, marker creation, member copy, manifest
copy, inventory verification, and marker removal are one cleanup boundary. An
owned partial tree is removed for a safe retry by unlinking only verified known
files and applying non-recursive `rmdir` from the deepest known directory. If
an unknown file or directory appears, the importer preserves it and fails
closed instead of deleting it.

Verify the installed snapshot with:

```sh
mvn test -Dtest=ContractSnapshotVerifierTest
```

Verify importer behavior (12 tests, including directory topology, tag movement,
and injected installation failures) with:

```sh
node --test scripts/import-miku-project-contract-snapshot.test.mjs
```

The repository-wide test entrypoint runs this Node suite before all Java tests,
then runs the snapshot verifier once more after those tests:

```sh
sh scripts/test-all.sh
```

The P5-A immutable-input boundary passed final review on 2026-08-14. P5-B now
uses this snapshot directly: its loader rejects unknown index fields, duplicate
case IDs, snapshot escapes, and allowlist-external references; its Java 8
test-side registry validates the four pinned schemas, checked-in positive
examples, and 18 JSON Schema mutation cases. P5-B3 adds canonical JSON / SHA-256,
case materialization, all 13 cross-artifact binding cases, and separate
semantic/byte/topology/runtime comparison boundaries without calling Node or
creating Java-only golden fixtures. P5-B passed its technical harness review
on 2026-08-14; human approval remains required before P5-C command work, which
has not begun.

## Update rule

A changed contract, schema, fixture, golden, source tag, or corpus digest is a
new snapshot release. Do not edit or replace `v1.0.3` in place. Add a new
versioned directory, generate its own `SOURCE.json`, extend the verifier test,
and review the Node/Java contract change together.

## Legacy boundary

`vendor/mikuproject/` remains the read-only source for the historical Java
port, its existing commands, report parity, and compatibility work. It is not
used as the authority for v1 `validate`, `inspect`, `plan-change`,
`apply-change`, `verify-artifact`, result/diagnostic envelopes, or runtime
manifest behavior.
