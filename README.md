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
- not yet started: target scope is known, but migration work has not begun

Current status is tracked in 2 streams:

- `core`: `MS Project XML`, `ProjectModel`, workbook, patch / AI JSON, CSV / Mermaid, validation
- `CLI / report`: CLI entrypoints, batch commands, runtime packaging, WBS Markdown / SVG / XLSX and report bundle style outputs

Current progress should be read as:

- `core`: roughly `98%` in the current STEP1 target, based on implemented upstream file coverage
- `CLI / report`: major commands and report outputs exist, but option / batch整理 and progress measurement still need follow-up

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

## CLI

The Java port now includes a minimal CLI entrypoint.

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

- `validate-xml <input.xml>`
- `validate-xml-batch <input.xml>...`
- `export-mermaid <input.xml>`
- `export-wbs-markdown <input.xml> [<beforeDays> [<afterDays> [<displayMode> [<progressMode> [<holidayDatesCsv>]]]]]`
- `export-daily-svg <input.xml> [<labelMode>]`
- `export-weekly-svg <input.xml> [<labelMode>]`
- `export-monthly-svg-zip <input.xml> <output.zip> [<holidayDatesCsv> [<labelMode>]]`
- `export-report-bundle <input.xml> <output.zip> [<beforeDays> [<afterDays> [<displayMode> [<progressMode> [<holidayDatesCsv> [<labelMode>]]]]]]`
- `export-report-dir <input.xml> <output.dir> [<beforeDays> [<afterDays> [<displayMode> [<progressMode> [<holidayDatesCsv> [<labelMode>]]]]]]`
- `export-report-dir-batch <outputRoot.dir> <input.xml> <name> [<input.xml> <name>]... [-- <beforeDays> [<afterDays> [<displayMode> [<progressMode> [<holidayDatesCsv> [<labelMode>]]]]]]`
- `export-wbs-xlsx <input.xml> <output.xlsxbin> [<beforeDays> [<afterDays> [<displayMode> [<progressMode> [<holidayDatesCsv>]]]]]`
- `export-workbook-json <input.xml>`
- `export-workbook-json-batch <outputRoot.dir> <input.xml> <name> [<input.xml> <name>]...`
- `export-project-overview-view <input.xml>`
- `export-project-overview-view-batch <outputRoot.dir> <input.xml> <name> [<input.xml> <name>]...`
- `export-phase-detail-view <input.xml> [<phaseUid> [<mode> [<rootUid> [<maxDepth>]]]]`
- `export-phase-detail-view-batch <outputRoot.dir> <input.xml> <name> [<input.xml> <name>]... [-- <phaseUid> [<mode> [<rootUid> [<maxDepth>]]]]`
- `export-task-edit-view <input.xml> <taskUid>`
- `export-project-draft-request <name> <plannedStart> [<goal> [<teamCount> [<mustHavePhasesCsv> [<mustHaveMilestonesCsv>]]]]`
- `validate-workbook-json <input.json>`
- `import-workbook-json <input.json> <output.xml>`
- `merge-workbook-json <base.xml> <input.json> <output.xml>`
- `validate-patch-json <input.json>`
- `apply-patch-json <base.xml> <patch.json> <output.xml>`
- `export-ai-json-spec`
- `detect-ai-json-kind <input.txt>`
- `import-ai-json <input.txt> <output.xml> [<base.xml>]`
- `import-external <format> <mode> <input> <output.xml> [<base.xml>]`
- `export-xlsx <input.xml> <output.xlsxbin>`
- `export-xlsx-batch <outputRoot.dir> <input.xml> <name> [<input.xml> <name>]...`
- `validate-xlsx <input.xlsxbin>`
- `import-xlsx <input.xlsxbin> <output.xml>`
- `merge-xlsx <base.xml> <input.xlsxbin> <output.xml>`
