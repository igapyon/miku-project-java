# Contributing

Keep changes traceable to `vendor/miku-project` when they affect shared product
behavior. Document Java-only behavior in `docs/upstream-cli-mapping.md` or the
relevant mapping document.

Run `mvn test` for code changes. Use the focused commands in
`docs/development.md` when maintaining a mapped upstream area, and run the
opt-in Node parity suite when changing report artifacts or shared CLI behavior.

Do not edit `vendor/miku-project` during ordinary Java maintenance. Treat it as
the vendored upstream snapshot and update it only through an explicit subtree
operation.
