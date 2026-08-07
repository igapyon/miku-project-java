# Upstream Snapshot

## Compatibility source

- Upstream repository: <https://github.com/igapyon/mikuproject>
- Vendored path: `vendor/mikuproject`
- Upstream package version in the vendored snapshot: `0.8.0`
- Java companion version: `0.8.3`
- Checked: 2026-08-06

`vendor/mikuproject` is a Git subtree and is read-only for ordinary Java
maintenance. The initial subtree import recorded upstream commit
`9257d1991244ac8277cc489b1cabb680d1e1fb8b`; the last target-repository commit
that updated vendored CLI behavior is
`c8df3a24d121f55673275de47ac00240122acef0`.

The exact upstream commit for that later update was not retained in the target
repository history. Record it with the next subtree update so a future Java
maintenance change has a precise upstream revision anchor.

## Verification anchor

The Node CLI source of truth is
`vendor/mikuproject/scripts/mikuproject-cli.mjs`. Shared CLI behavior is mapped
in `docs/upstream-cli-mapping.md`. Report artifact parity uses the vendored
fixtures and the opt-in `MikuprojectNodeParityTest` suite.
