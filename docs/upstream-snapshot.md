# Upstream Snapshot

## v1 contract source

The Gate G4-approved v1 Java CLI source of authority is the immutable
`vendor/miku-project-contract/v1.0.3/` snapshot, not the historical
`vendor/mikuproject` subtree.

- Node reference release: `1.0.3`
- source tag: `v1.0.3`
- source revision: `693b4ecd7d4328d77f3b2eada9c4965a9c9b15f5`
- contract version / fixture suite version: `1` / `1`
- conformance corpus SHA-256: `f0b4a821f80f155ba01afc1fdf7c5d2fa6ca5e744bbd0090819e0d591f1c47a3`

`SOURCE.json` inventories every imported contract document, Schema, example,
fixture, and golden with its raw SHA-256. See [v1 contract snapshot](v1-contract-snapshot.md)
for the importer, verifier, and update rule. The snapshot is immutable; a
future contract change creates a new versioned snapshot directory.

## Compatibility source

- Upstream repository: <https://github.com/igapyon/miku-project>
- Vendored path: `vendor/mikuproject`
- Upstream package version in the vendored snapshot: `1.0.4`
- Vendored upstream commit: `dc6fe12310c38bfdba5ea3283c93500d027eac86`
- Java companion version: `1.0.4`
- Checked: 2026-08-16

`vendor/mikuproject` is a Git subtree and is read-only for ordinary Java
maintenance. The initial subtree import recorded upstream commit
`9257d1991244ac8277cc489b1cabb680d1e1fb8b`; the vendored snapshot used by the
current Java compatibility baseline is
`dc6fe12310c38bfdba5ea3283c93500d027eac86` (tag `v1.0.4`), incorporated by
target-repository merge commit `ab27348`.

The local `mikuproject` remote uses
`git@github.com:igapyon/miku-project.git`; do not rely on the old GitHub
redirect when fetching future subtree updates.

The public naming follow-up is anchored to upstream commit
`a3385a5` (`miku-project 命名体系へ Main Application を移行`). The v1.0.4
subtree contains the renamed CLI and AI JSON specification paths. The Java
resource copy and Node parity runner follow those paths, while
`vendor/mikuproject`, `jp.igapyon.mikuproject`, and established exchange-format
IDs remain compatibility anchors.

The content follow-up is anchored to upstream commits `8c5043f`
(`miku-ms-office-coreでXLSXパッケージ読み込みを共通化する`) and `7e5d283`
(`miku-soft標準適合とXLSX XML sanitizerを更新`). Java's `ZipInputStream`
already reads both stored and DEFLATE ZIP entries, so the Node/browser-only
`miku-ms-office-core` module is not vendored into the Java application. The
Java XML sanitizer already retains valid supplementary Unicode code points and
removes invalid XML characters; the current regression tests also cover valid
XML whitespace and DEFLATE input. `String.compareTo` / `Collections.sort`
provide the same locale-independent lexical ordering that those upstream
changes made explicit for task UIDs and date strings.

Java now uses the release artifact of the Java companion project
[`miku-ms-office-core-java`](https://github.com/igapyon/miku-ms-office-core-java)
for ZIP package reading. The vendored Maven repository artifact is
`jp.igapyon:miku-ms-office-core:0.6.0` (Release `v0.6.0`, SHA-256
`d25392727d9449e5001b9024b888f0ce09962c9fb977c18613731f37027b0a77`).
`ExcelIoZip.unpackZip` delegates to its `ZipPackage.readZipPackage`; existing
Java ZIP writing remains product-side because byte-level report parity fixes
its current header and timestamp contract.

## Legacy verification anchor

The legacy Node CLI source of truth is
`vendor/mikuproject/scripts/miku-project-cli.mjs`. It retains `mikuproject` as
a command alias while `miku-project` is canonical. Shared CLI behavior is
mapped in `docs/upstream-cli-mapping.md`. Report artifact parity uses the
vendored fixtures and the opt-in `MikuprojectNodeParityTest` suite.

This legacy anchor does not define v1 command, result/diagnostic, conformance,
or runtime-manifest behavior.
