# Upstream Snapshot

## Compatibility source

- Upstream repository: <https://github.com/igapyon/miku-project>
- Vendored path: `vendor/miku-project`
- Upstream package version in the vendored snapshot: `0.12.0`
- Upstream release commit: `48ec367e9be5f2490c8f96d4784d55cb3fbb9f03` (`v0.12.0`)
- Applied Node naming-test commit: `f437cbf3f6a8fc03613cf8ada3038a05751c6837`
- Java companion version: `0.12.0`
- Checked: 2026-08-09

`vendor/miku-project` is a Git subtree and is read-only for ordinary Java
maintenance. The initial subtree import recorded upstream commit
`9257d1991244ac8277cc489b1cabb680d1e1fb8b`; the former `0.8.0` baseline was
`245deaa99d6d2ba970969a9359ce003386da3472`. The current subtree advances to
the `v0.12.0` release and applies the focused rename of
`tests/mikuproject-cli.test.js` to `tests/miku-project-cli.test.js`.

The vendored CLI, AI JSON spec, package metadata, and canonical test path now
use `miku-project`. Java package names and established exchange-format IDs,
such as `mikuproject_workbook_json`, remain compatibility and traceability
identifiers; they are not public artifact or CLI names.

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

The Node CLI source of truth is
`vendor/miku-project/scripts/miku-project-cli.mjs`; its paired test is
`vendor/miku-project/tests/miku-project-cli.test.js`. Shared CLI behavior is
mapped in `docs/upstream-cli-mapping.md`. Report artifact parity uses the
vendored fixtures and the opt-in `MikuprojectNodeParityTest` suite.
