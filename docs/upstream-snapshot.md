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

## Verification anchor

The Node CLI source of truth is
`vendor/mikuproject/scripts/mikuproject-cli.mjs`. Shared CLI behavior is mapped
in `docs/upstream-cli-mapping.md`. Report artifact parity uses the vendored
fixtures and the opt-in `MikuprojectNodeParityTest` suite.
