# mikuproject-java

Java port workspace for `mikuproject`.

## Purpose

This repository is a Java 1.8 based port of `mikuproject`.

The Java port does not try to redesign the upstream project into a Java-first architecture at the initial stage.
The primary goal is to preserve the upstream Node.js structure, naming, and intent closely enough that upstream changes remain traceable.

The Java port targets the CLI-oriented feature set that can run in the Java runtime.
Browser/Web UI related behavior is kept out of scope for this repository.

In other words, the port is intended to be a straight conversion first.
Java-specific redesign should be treated as a later step, after the corresponding upstream behavior has been carried over and remains traceable.

Current STEP1 scope is focused on:

- `MS Project XML -> ProjectModel -> MS Project XML`
- `ProjectModel` based internal normalization
- validation around `Project / Tasks / Resources / Assignments / Calendars`
- upstream-aware automated tests using JUnit Jupiter

Current status should be read in the following 2 buckets:

- implemented: Java-side classes and behavior already exist
- maintenance follow-up: existing implementation needs verification, docs alignment, or upstream diff tracking

Current status is tracked in 2 streams:

- `core`: `MS Project XML`, `ProjectModel`, workbook, patch / AI JSON, CSV / Mermaid, validation
- `CLI / report`: CLI entrypoints, batch commands, runtime packaging, WBS Markdown / SVG / XLSX and report bundle style outputs

Current progress should be read as:

- `core`: roughly `98%` in the current STEP1 target, based on implemented upstream file coverage
- `CLI / report`: major commands and report outputs exist; remaining work is limited to existing-scope verification, TODO/docs cleanup, and upstream diff tracking

This repository keeps `README.md` and `docs/remaining-migration-items.md` aligned on these scope and progress assumptions.

## Upstream Policy

- Keep the Node.js upstream repository under `vendor/mikuproject` using `git subtree`.
- Treat `vendor/mikuproject` as read-only upstream reference unless there is an explicit reason to patch it.
- Keep Java implementation and Java-specific specs outside `vendor/`.
- Keep `workplace/` out of Git tracking as a local working area.

## Porting Policy

- Keep Java package names under `jp.igapyon.mikuproject`.
- Respect upstream file boundaries and responsibility splits as much as practical.
- Prefer names that are easy to map back to upstream `kebab-case` files and `camelCase` methods.
- Do not over-optimize for modern Java style when it harms migration traceability.
- When upstream uses scratch logic, prefer mirroring that logic first.
- When upstream uses external libraries, evaluate corresponding Java libraries case by case.

## Testing Policy

- Use `mvn test` as the primary test entrypoint.
- Keep Java tests aligned with upstream test intent and upstream fixtures where possible.
- Use upstream fixture files under `vendor/mikuproject/testdata` when they are useful as comparison material.

## Development Docs

Use this section as the repo-top entrypoint for upstream tracking and migration maintenance.

Suggested order:

1. `docs/remaining-migration-items.md`
2. `docs/miku-straight-conversion-guide.md`
3. `docs/upstream-class-mapping.md`
4. `docs/upstream-test-mapping.md`
5. `docs/development.md`
6. `docs/upstream-followup-log.md`

Suggested tracking flow:

1. Check current scope and status in `docs/remaining-migration-items.md`
2. Find the matching Java classes in `docs/upstream-class-mapping.md`
3. Find the matching tests and focused regression unit in `docs/upstream-test-mapping.md`
4. Use `docs/development.md` for the focused test command you actually want to run
5. Record the result in `docs/upstream-followup-log.md`

Operational note:

- For `docs/*.md` only changes, do not run extra tests by default
- Run tests only when code changes are included, or when you add a new regression command / verification result to the docs
- Keep current maintenance work focused on existing implementation checks, TODO/docs cleanup, migrated-scope verification, and upstream diff tracking
- A change set like this workflow/status alignment can be grouped as a `docs-only` commit

- `docs/remaining-migration-items.md`
  - Current migration status, remaining items, and recent verification state
- `docs/miku-straight-conversion-guide.md`
  - Why this repository treats the Java port as a straight conversion first
- `docs/upstream-class-mapping.md`
  - `upstream file -> Java class` mapping and diff-check samples
- `docs/upstream-test-mapping.md`
  - `upstream test intent -> Java test` mapping and focused regression units
- `docs/development.md`
  - Daily development notes and focused test commands
- `docs/upstream-followup-log.md`
  - Per-file follow-up records for upstream tracking

## CLI

The Java port now includes a practical CLI entrypoint covering the main Java runtime feature set.

The distributable runtime artifact is a single fat jar produced by `mvn package`.

- `target/mikuproject.jar`

`mvn package` also produces a distribution archive.

- `target/mikuproject-dist.zip`

The expected execution path is:

- `java -jar target/mikuproject.jar ...`

The distribution zip contains:

- `mikuproject.jar`
- `README.md`
- `LICENSE`
- `docs/runtime-java-cli.md`

Supported commands:

- `validate-xml <input.xml>`
- `validate-xml-batch <input.xml>...`
- `export-mermaid <input.xml>`
- `export-mermaid-batch <outputRoot.dir> <input.xml> <name> [<input.xml> <name>]...`
- `export-wbs-markdown <input.xml> [<beforeDays> [<afterDays> [<displayMode> [<progressMode> [<holidayDatesCsv>]]]]]`
- `export-wbs-markdown-batch <outputRoot.dir> <input.xml> <name> [<input.xml> <name>]... [-- <beforeDays> [<afterDays> [<displayMode> [<progressMode> [<holidayDatesCsv>]]]]]`
- `export-daily-svg <input.xml> [<labelMode>]`
- `export-daily-svg-batch <outputRoot.dir> <input.xml> <name> [<input.xml> <name>]... [-- <labelMode>]`
- `export-weekly-svg <input.xml> [<labelMode>]`
- `export-weekly-svg-batch <outputRoot.dir> <input.xml> <name> [<input.xml> <name>]... [-- <labelMode>]`
- `export-monthly-svg-zip <input.xml> <output.zip> [<holidayDatesCsv> [<labelMode>]]`
- `export-monthly-svg-zip-batch <outputRoot.dir> <input.xml> <name> [<input.xml> <name>]... [-- <holidayDatesCsv> [<labelMode>]]`
- `export-report-bundle <input.xml> <output.zip> [<beforeDays> [<afterDays> [<displayMode> [<progressMode> [<holidayDatesCsv> [<labelMode>]]]]]]`
- `export-report-bundle-batch <outputRoot.dir> <input.xml> <name> [<input.xml> <name>]... [-- <beforeDays> [<afterDays> [<displayMode> [<progressMode> [<holidayDatesCsv> [<labelMode>]]]]]]`
- `export-report-dir <input.xml> <output.dir> [<beforeDays> [<afterDays> [<displayMode> [<progressMode> [<holidayDatesCsv> [<labelMode>]]]]]]`
- `export-report-dir-batch <outputRoot.dir> <input.xml> <name> [<input.xml> <name>]... [-- <beforeDays> [<afterDays> [<displayMode> [<progressMode> [<holidayDatesCsv> [<labelMode>]]]]]]`
- `export-wbs-xlsx <input.xml> <output.xlsxbin> [<beforeDays> [<afterDays> [<displayMode> [<progressMode> [<holidayDatesCsv>]]]]]`
- `export-wbs-xlsx-batch <outputRoot.dir> <input.xml> <name> [<input.xml> <name>]... [-- <beforeDays> [<afterDays> [<displayMode> [<progressMode> [<holidayDatesCsv>]]]]]`
- `export-workbook-json <input.xml>`
- `export-workbook-json-batch <outputRoot.dir> <input.xml> <name> [<input.xml> <name>]...`
- `export-project-overview-view <input.xml>`
- `export-project-overview-view-batch <outputRoot.dir> <input.xml> <name> [<input.xml> <name>]...`
- `export-phase-detail-view <input.xml> [<phaseUid> [<mode> [<rootUid> [<maxDepth>]]]]`
- `export-phase-detail-view-batch <outputRoot.dir> <input.xml> <name> [<input.xml> <name>]... [-- <phaseUid> [<mode> [<rootUid> [<maxDepth>]]]]`
- `export-task-edit-view <input.xml> <taskUid>`
- `export-task-edit-view-batch <outputRoot.dir> <taskUid> <input.xml> <name> [<input.xml> <name>]...`
- `export-project-draft-request <name> <plannedStart> [<goal> [<teamCount> [<mustHavePhasesCsv> [<mustHaveMilestonesCsv>]]]]`
- `validate-workbook-json <input.json>`
- `validate-workbook-json-batch <input.json>...`
- `import-workbook-json <input.json> <output.xml>`
- `import-workbook-json-batch <outputRoot.dir> <input.json> <name> [<input.json> <name>]...`
- `merge-workbook-json <base.xml> <input.json> <output.xml>`
- `merge-workbook-json-batch <base.xml> <outputRoot.dir> <input.json> <name> [<input.json> <name>]...`
- `validate-patch-json <input.json>`
- `validate-patch-json-batch <input.json>...`
- `apply-patch-json <base.xml> <patch.json> <output.xml>`
- `apply-patch-json-batch <base.xml> <outputRoot.dir> <patch.json> <name> [<patch.json> <name>]...`
- `export-ai-json-spec`
- `detect-ai-json-kind <input.txt>`
- `detect-ai-json-kind-batch <input.txt>...`
- `import-ai-json <input.txt> <output.xml> [<base.xml>]`
- `import-external <format> <mode> <input> <output.xml> [<base.xml>]`
- `export-xlsx <input.xml> <output.xlsxbin>`
- `export-xlsx-batch <outputRoot.dir> <input.xml> <name> [<input.xml> <name>]...`
- `validate-xlsx <input.xlsxbin>`
- `validate-xlsx-batch <input.xlsxbin>...`
- `import-xlsx <input.xlsxbin> <output.xml>`
- `import-xlsx-batch <outputRoot.dir> <input.xlsxbin> <name> [<input.xlsxbin> <name>]...`
- `merge-xlsx <base.xml> <input.xlsxbin> <output.xml>`
- `merge-xlsx-batch <base.xml> <outputRoot.dir> <input.xlsxbin> <name> [<input.xlsxbin> <name>]...`

Notes:

- `export-ai-json-spec` returns the Markdown spec embedded in the built classpath / JAR. Runtime execution does not read `vendor/mikuproject/docs/mikuproject-ai-json-spec.md` by relative path.
- `export-daily-svg` and report directory daily SVG output place the timeline origin at the earliest task start date in the model, so projects outside the historical sample month still render inside the SVG viewBox. If every task has the same zero-duration date, the output is valid but all bars will naturally stack on the same day.
