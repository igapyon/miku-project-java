# Upstream Snapshot

## Compatibility source

- Upstream repository: <https://github.com/igapyon/miku-project>
- Vendored path: `vendor/mikuproject`
- Upstream package version in the vendored snapshot: `0.8.0`
- Vendored upstream commit: `245deaa99d6d2ba970969a9359ce003386da3472`
- Java companion version: `0.8.4`
- Checked: 2026-08-09

`vendor/mikuproject` is a Git subtree and is read-only for ordinary Java
maintenance. The initial subtree import recorded upstream commit
`9257d1991244ac8277cc489b1cabb680d1e1fb8b`; the vendored snapshot used by the
current Java compatibility baseline is
`245deaa99d6d2ba970969a9359ce003386da3472`, incorporated by target-repository
commit `c8df3a24d121f55673275de47ac00240122acef0`.

The local `mikuproject` remote uses
`git@github.com:igapyon/miku-project.git`; do not rely on the old GitHub
redirect when fetching future subtree updates.

The public naming follow-up is anchored to upstream commit
`a3385a5` (`miku-project 命名体系へ Main Application を移行`). Java applies
its public CLI, AI JSON spec, sample-data, artifact, and repository naming
without advancing the fixed vendored snapshot. `vendor/mikuproject`,
`jp.igapyon.mikuproject`, and established exchange-format IDs remain
compatibility anchors. A later subtree update must switch the vendored CLI and
AI JSON spec paths to their renamed upstream paths as part of that separate
compatibility review.

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

## Verification anchor

The fixed-snapshot Node CLI source of truth is
`vendor/mikuproject/scripts/mikuproject-cli.mjs`. Current upstream renamed it
to `scripts/miku-project-cli.mjs`; the vendored path remains until the subtree
baseline is upgraded. Shared CLI behavior is mapped in
`docs/upstream-cli-mapping.md`. Report artifact parity uses the vendored
fixtures and the opt-in `MikuprojectNodeParityTest` suite.
