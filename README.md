# miku-project-java

Java port workspace for `miku-project`.

## Purpose

This repository is a Java 1.8 based port of `miku-project`.

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

- Keep the Node.js upstream repository under `vendor/miku-project` using `git subtree`.
- Treat `vendor/miku-project` as read-only upstream reference unless there is an explicit reason to patch it.
- Use the checksum-fixed `miku-ms-office-core-java` Release JAR under `vendor/miku-ms-office-core-java/` for product-neutral Office ZIP package reading. Keep document semantics in this repository.
- Keep Java implementation and Java-specific specs outside `vendor/`.
- Keep `workplace/` out of Git tracking as a local working area; only `workplace/.gitkeep` is tracked to retain the directory.

## Porting Policy

- Keep Java package names under `jp.igapyon.mikuproject`.
- Respect upstream file boundaries and responsibility splits as much as practical.
- Prefer names that are easy to map back to upstream `kebab-case` files and `camelCase` methods.
- Do not over-optimize for modern Java style when it harms migration traceability.
- When upstream uses scratch logic, prefer mirroring that logic first.
- When upstream uses external libraries, evaluate corresponding Java libraries case by case.

## Naming and Compatibility

- The public repository, Maven artifact, runtime artifacts, and CLI product name use `miku-project`.
- The vendored subtree also uses the canonical `vendor/miku-project/` path. Java package names, the `MikuprojectCli` main class, and established exchange-format identifiers such as `mikuproject_workbook_json` remain legacy compatibility and traceability identifiers.

## Testing Policy

- Use `mvn test` as the primary test entrypoint.
- Keep Java tests aligned with upstream test intent and upstream fixtures where possible.
- Use upstream fixture files under `vendor/miku-project/testdata` when they are useful as comparison material.

## Development Docs

Use this section as the repo-top entrypoint for upstream tracking and migration maintenance.

Suggested order:

1. `docs/remaining-migration-items.md`
2. `docs/miku-soft-reference.md`
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
- `docs/miku-soft-reference.md`
  - Shared miku-soft references, the selected Java straight-conversion workflow, and reference revision metadata
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

- `target/miku-project.jar`

`mvn package` also produces a sources jar and distribution archive.

- `target/miku-project-sources.jar`
- `target/miku-project-dist.zip`

The expected execution path is:

- `java -jar target/miku-project.jar ...`

The distribution zip contains:

- `miku-project.jar`
- `miku-project-sources.jar`
- `README.md`
- `LICENSE`
- `docs/runtime-java-cli.md`

Supported commands:

### Shared Node-compatible commands

- `--version`
- `ai spec`
- `ai export project-overview [--in workbook.json|-] [--diagnostics text|json] [--out overview.editjson|-]`
- `ai export task-edit [--in workbook.json|-] [--task-uid taskUid] [--select auto|first-task|uid] [--diagnostics text|json] [--out task.editjson|-]`
- `ai export phase-detail [--in workbook.json|-] [--phase-uid phaseUid] [--select auto|first-phase|uid] [--mode scoped|full] [--root-task-uid rootTaskUid] [--max-depth n] [--diagnostics text|json] [--out phase.editjson|-]`
- `ai export bundle [--in workbook.json|-] [--diagnostics text|json] [--out bundle.editjson|-]`
- `ai detect-kind [--in document.json|-] [--diagnostics text|json]`
- `ai validate-patch --state workbook.json [--in patch.json] [--diagnostics text|json]`
- `state from-draft [--in draft.editjson|-] [--out workbook.json|-]`
- `state apply-patch --state workbook.json|- [--in patch.json|-] [--diagnostics text|json] [--out workbook.next.json|-]`
- `state summarize [--in workbook.json|-] [--diagnostics text|json]`
- `state diff --before workbook.before.json --after workbook.after.json [--diagnostics text|json]`
- `export workbook-json [--in workbook.json|-] [--diagnostics text|json] [--out workbook.json|-]`
- `export xml [--in workbook.json|-] [--diagnostics text|json] [--out project.xml|-]`
- `export xlsx [--in workbook.json|-] [--diagnostics text|json] (--out project.xlsx|--out-base64 -)`
- `import xlsx (--in workbook.xlsx|--in-base64 -) [--diagnostics text|json] [--out workbook.json|-]`
- `report wbs-xlsx [--in workbook.json|-] [--diagnostics text|json] (--out report.xlsx|--out-base64 -)`
- `report daily-svg [--in workbook.json|-] [--diagnostics text|json] [--out report.svg|-]`
- `report weekly-svg [--in workbook.json|-] [--diagnostics text|json] [--out report.svg|-]`
- `report monthly-calendar-svg [--in workbook.json|-] [--diagnostics text|json] (--out report.zip|--out-base64 -)`
- `report all [--in workbook.json|-] [--diagnostics text|json] (--out report-bundle.zip|--out-base64 -)`
- `report wbs-markdown [--in workbook.json|-] [--diagnostics text|json] [--out report.md|-]`
- `report mermaid [--in workbook.json|-] [--diagnostics text|json] [--out report.mmd|-]`

### Java extensions

- `state validate --in workbook.json`
- `state import --in workbook.json [--out workbook.normalized.json]`
- `state merge --state workbook.json --in workbook.patch.json [--out workbook.next.json]`
- `validate xml --in project.xml`
- `validate xlsx --in workbook.xlsx`
- `merge xlsx --state workbook.json --in workbook.xlsx [--out workbook.next.json]`
- `report dir --in workbook.json --out report.dir`

Notes:

- `ai spec` returns the public `miku-project-ai-json-spec` Markdown embedded in the built classpath / JAR. Runtime execution does not read the vendored source by relative path.
- `export-daily-svg` and report directory daily SVG output place the timeline origin at the earliest task start date in the model, so projects outside the historical sample month still render inside the SVG viewBox. If every task has the same zero-duration date, the output is valid but all bars will naturally stack on the same day.
