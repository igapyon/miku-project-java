# Upstream Follow-up Log

## 目的

この文書は、Node.js upstream 更新追随時の差分確認結果を、`upstream file` 単位で残すための記録置き場である。
repo top から入るときの入口は `README.md` の `Development Docs` 節とする。

`docs/upstream-class-mapping.md` のテンプレートとサンプル、`docs/upstream-test-mapping.md` の関連 test 対応、`docs/development.md` の focused test command を、実際の確認記録へ落とすときに使う。
対応する Java class / test を引き直したいときは、`docs/upstream-class-mapping.md` と `docs/upstream-test-mapping.md` を参照する。

## 記録ルール

- 1 記録は 1 upstream file を単位にする
- `挙動差分 / 命名差分 / 未移植差分 / Java 側独自拡張` の 4 区分で残す
- 必要なら、確認に使った test と fixture を明記する
- 差分がなかった場合も、その時点の確認結果を簡潔に残す

記録前の確認単位:

- report 系:
  - `mvn test -Dtest=WbsMarkdownTest,WbsSvgTest,WbsXlsxTest,CoreApiPublicTest,MikuprojectCliTest`
- import / AI view 系:
  - `mvn test -Dtest=CoreApiImportTest,MsProjectAiViewsTest`
- workbook 系:
  - `mvn test -Dtest=ProjectWorkbookJsonTest,ProjectXlsxTest,CoreApiWorkbookTest`
- upstream 追随の保守回帰:
  - `mvn test -Dtest=WbsMarkdownTest,WbsSvgTest,WbsXlsxTest,ProjectWorkbookJsonTest,ProjectXlsxTest,CoreApiWorkbookTest,CoreApiPublicTest,CoreApiImportTest,MsProjectAiViewsTest,MikuprojectCliTest`

補足:

- `docs only` 更新では、原則として追加テストを回さない
- コード変更を含む場合、または新しい回帰コマンド自体をここへ記録する場合だけ、対象単位を実行して確認する
- この種の追随運用文書の整備は、`docs-only` のコミットとしてまとめてよい

最短フロー:

1. `docs/remaining-migration-items.md` で現在地と対象範囲を確認する
2. `docs/upstream-class-mapping.md` で対象 file の対応 class を引く
3. `docs/upstream-test-mapping.md` で対応 test と focused 回帰単位を引く
4. `docs/development.md` で実行する focused test command を確認する
5. 必要なら対象単位だけ test を実行し、この文書と `docs/remaining-migration-items.md` に結果を反映する

## 現在の sample coverage

- report bundle / public report API:
  - `vendor/mikuproject/src/ts/core-api-report.ts`
- unified import / external import:
  - `vendor/mikuproject/src/ts/core-api-import.ts`
- XML public entry:
  - `vendor/mikuproject/src/ts/msproject-xml.ts`
- XML codec:
  - `vendor/mikuproject/src/ts/msproject-codec.ts`
- model validation:
  - `vendor/mikuproject/src/ts/msproject-validate.ts`
- AI view export / import:
  - `vendor/mikuproject/src/ts/msproject-ai-views.ts`
- Mermaid export:
  - `vendor/mikuproject/src/ts/msproject-mermaid.ts`
- workbook JSON / workbook wrapper:
  - `vendor/mikuproject/src/ts/project-workbook-json.ts`
  - `vendor/mikuproject/src/ts/core-api-workbook.ts`
- workbook xlsx wrapper:
  - `vendor/mikuproject/src/ts/core-api-workbook-xlsx.ts`
- project xlsx:
  - `vendor/mikuproject/src/ts/project-xlsx.ts`
- SVG report:
  - `vendor/mikuproject/src/ts/wbs-svg.ts`
- Markdown report:
  - `vendor/mikuproject/src/ts/wbs-markdown.ts`
- XLSX report:
  - `vendor/mikuproject/src/ts/wbs-xlsx.ts`

report / workbook / import / AI view の主要導線は sample 記録がある状態になっている。
現フェーズでは、新規機能追加ではなく、この sample 記録と既存実装 / test 対応のズレを小さく保つことを優先する。

## 2026-08-09 `scripts/miku-project-cli.mjs` / `docs/miku-project-ai-json-spec.md`

```text
upstream files:
  scripts/miku-project-cli.mjs
  docs/miku-project-ai-json-spec.md
  src/ts/msproject-samples.ts

java classes:
  jp.igapyon.mikuproject.cli.MikuprojectCli
  jp.igapyon.mikuproject.coreapi.CoreApiAiJson
  jp.igapyon.mikuproject.msprojectxml.MsProjectSamples

tests:
  MikuprojectCliTest.printsVersion
  CoreApiImportTest.importsAiJsonTextAndExposesAiJsonSpec
  MsProjectSamplesTest.buildSampleProjectModelCreatesExpectedSample
  ProjectXlsxTest.convertsProjectModelIntoWorkbookSheets
  WbsMarkdownTest.exportsOneMarkdownDocumentWithTreeFirstAndTableAfterIt
  WbsSvgTest.exportsDailyAndWeeklySvg
  MsProjectMermaidTest.exportsMermaidFromSampleProject

diff summary:
  挙動差分:
    public CLI version text, AI JSON spec identifier/text, and the built-in sample project name use `miku-project`.
  命名差分:
    upstream `a3385a5` makes `miku-project` canonical and keeps `mikuproject` as a Node CLI alias. Java keeps `MikuprojectCli`, its package, the vendored path, and exchange-format identifiers such as `mikuproject_workbook_json` as compatibility anchors.
  未移植差分:
    The fixed vendored snapshot still uses the old CLI/spec paths. Updating that subtree is a separate compatibility review; portable content changes are reviewed in the following records, while Node/browser UI and release-only changes remain outside Java scope.
  Java 側独自拡張:
    None.

follow-up:
  - 実施した確認:
    `mvn test -Dtest=MikuprojectCliTest,CoreApiImportTest,MsProjectSamplesTest,ProjectXlsxTest,WbsMarkdownTest,WbsSvgTest,MsProjectMermaidTest`
  - fixture:
    built-in sample project and the vendored AI JSON spec resource
  - 次回の確認観点:
    When the subtree baseline reaches `a3385a5` or later, replace the old vendored CLI/spec paths and run the Node parity suite against that updated snapshot.
  - `docs/remaining-migration-items.md` への反映:
    2026-08-09 naming follow-up recorded in `TODO.md`.
```

## 2026-08-09 `src/ts/excel-io-zip.ts`

```text
upstream file:
  src/ts/excel-io-zip.ts

java classes:
  jp.igapyon.mikuproject.excelio.ExcelIoZip
  jp.igapyon.mikuproject.excelio.XlsxWorkbookCodec

tests:
  ExcelIoTest.unpacksDeflatedZipEntriesForExternalPackages

diff summary:
  挙動差分:
    upstream `8c5043f` は browser/Node の非同期 ZIP 読み込みを miku-ms-office-core へ共通化した。Java は標準の `ZipInputStream` により stored / DEFLATE の両方を同期的に読めるため、同じ XLSX package input 契約を追加ライブラリなしで満たす。DEFLATE entry の回帰を追加した。
  命名差分:
    Java は `ExcelIoZip` / `XlsxWorkbookCodec` に責務を分け、miku-ms-office-core module 名を公開しない。
  未移植差分:
    browser の asynchronous API と shared module packaging は Java CLI の対象外である。
  Java 側独自拡張:
    deterministic stored ZIP 出力は既存の Java artifact parity 契約として維持する。

follow-up:
  - 実施した確認:
    `mvn test -Dtest=ExcelIoTest`
  - fixture:
    JDK `ZipOutputStream` が生成する DEFLATE entry
  - 次回の確認観点:
    upstream が ZIP64、暗号化、または追加の compression method を入力契約に含めた場合は Java standard library の対応範囲を再確認する。
```

## 2026-08-09 `miku-ms-office-core-java` `v0.6.0`

```text
upstream companion:
  https://github.com/igapyon/miku-ms-office-core-java
  Release v0.6.0

java classes:
  jp.igapyon.mikuproject.excelio.ExcelIoZip
  jp.igapyon.mikumsofficecore.ZipPackage

tests:
  ExcelIoTest.unpacksDeflatedZipEntriesForExternalPackages
  ExcelIoTest.normalizesZipEntryPathsThroughMikuMsOfficeCore
  ProjectXlsxTest
  WbsXlsxTest

diff summary:
  挙動差分:
    `ExcelIoZip.unpackZip` は miku-ms-office-core-java の `ZipPackage.readZipPackage` を使う。DEFLATE input、OPC package path normalize、structured package diagnostics を共通 core へ寄せた。
  命名差分:
    product-side の `ExcelIoZip` public API は維持し、low-level core class を Java application の API surface へ直接露出しない。
  未移植差分:
    core の generic OPC relationship / content type API は、現時点では既存の product-side XLSX parser と重複するため置換していない。
  Java 側独自拡張:
    ZIP write は Node report artifact との byte-level parity を守る既存 `ExcelIoZip` に残す。

follow-up:
  - 実施した確認:
    `mvn test -Dtest=ExcelIoTest,ProjectXlsxTest,WbsXlsxTest,CoreApiWorkbookTest,MikuprojectCliTest`
  - artifact:
    `jp.igapyon:miku-ms-office-core:0.6.0`
    SHA-256 `d25392727d9449e5001b9024b888f0ce09962c9fb977c18613731f37027b0a77`
  - 次回の確認観点:
    product-side XLSX parser が generic OPC relationship / content-type helper を必要とした時点で、重複する helper を core API へ段階的に委譲する。
```

## 2026-08-09 `src/ts/excel-io-util.ts` / `src/ts/excel-io-worksheet-build.ts`

```text
upstream files:
  src/ts/excel-io-util.ts
  src/ts/excel-io-worksheet-build.ts

java classes:
  jp.igapyon.mikuproject.excelio.ExcelIoWorksheetBuild

tests:
  ExcelIoTest.preservesSupplementaryUnicodeAndRemovesInvalidXmlCharacters
  ExcelIoTest.preservesXmlWhitespaceAndRemovesXmlControlCharacters

diff summary:
  挙動差分:
    upstream `7e5d283` の XML 1.0 sanitizer（補助平面 Unicode と TAB/LF/CR を保持し、不正 surrogate / noncharacter / control character を除去）は Java に既に実装済みだった。上流と同じ control character と XML whitespace の回帰観点を追加した。
  命名差分:
    なし。
  未移植差分:
    なし。
  Java 側独自拡張:
    なし。

follow-up:
  - 実施した確認:
    `mvn test -Dtest=ExcelIoTest`
  - fixture:
    `😀 🐇 𠮷野家`、XML whitespace、invalid surrogate / noncharacter / control character
  - 次回の確認観点:
    XML 生成対象が worksheet text 以外へ広がる場合は、その field でも XML 1.0 sanitizer を通す必要があるか確認する。
```

## 2026-08-09 `src/ts/msproject-ai-views.ts` / `src/ts/msproject-calendar.ts`

```text
upstream files:
  src/ts/msproject-ai-views.ts
  src/ts/msproject-calendar.ts

java classes:
  jp.igapyon.mikuproject.msprojectxml.MsProjectAiViews
  jp.igapyon.mikuproject.msprojectxml.MsProjectCalendar

tests:
  MsProjectAiViewsTest.buildsProjectDraftRequestAndImportsPredecessorMappingFromProjectDraftView
  WbsMarkdownTest
  WbsSvgTest

diff summary:
  挙動差分:
    upstream `7e5d283` は runtime locale に依存しない lexical ordering を明示した。Java は task UID を `String.compareTo`、date text を `Collections.sort` で順序付けており、同じ UTF-16 lexical ordering を既に満たす。
  命名差分:
    なし。
  未移植差分:
    なし。
  Java 側独自拡張:
    なし。

follow-up:
  - 実施した確認:
    `mvn test -Dtest=MsProjectAiViewsTest,WbsMarkdownTest,WbsSvgTest`
  - fixture:
    `vendor/mikuproject/testdata/hierarchy.xml` と holiday / date range を使う report fixtures
  - 次回の確認観点:
    upstream が UID / date text 以外に locale-sensitive sort を導入した場合は、Java 側でも明示 comparator を追加する。
```

## 2026-05-01 `vendor/mikuproject/scripts/mikuproject-cli.mjs`

```text
upstream file:
  vendor/mikuproject/scripts/mikuproject-cli.mjs

java classes:
  jp.igapyon.mikuproject.cli.MikuprojectCli

tests:
  MikuprojectCliTest.usesNodeCompatibleBase64ForBinaryCliIo
  MikuprojectCliTest.rejectsBinaryStdoutWithoutBase64Option
  MikuprojectCliTest.keepsReadmeCliCommandListInSyncWithHelpOutput

diff summary:
  挙動差分:
    Node.js CLI の XLSX / ZIP binary artifact 出力契約に合わせ、Java CLI でも `export xlsx` / `report wbs-xlsx` / `report monthly-calendar-svg` / `report all` は `--out <path>` または `--out-base64 -` を要求する。binary stdout は usage error として扱う。
  命名差分:
    Node.js 側の `--in-base64` / `--out-base64` option 名を Java CLI でもそのまま採用した。
  未移植差分:
    現時点で今回の binary I/O 追随範囲に顕在化している未移植差分は記録していない。
  Java 側独自拡張:
    Java CLI の diagnostics JSON は既存の簡易 `io` 構造を維持しつつ、`stdin_base64` / `stdout_base64` を記録する。

follow-up:
  - 実施した確認:
    `mvn test -Dtest=MikuprojectCliTest`
  - fixture:
    `project_draft_view` から生成した workbook JSON / XLSX を使う CLI focused test
  - 次回の確認観点:
    upstream 側で binary 入出力 option、diagnostics io schema、XLSX import mode が変わった場合は再確認する
  - `docs/remaining-migration-items.md` への反映:
    2026-05-01 時点の binary I/O 追随結果と focused test 通過結果を追加
```

## 2026-08-06 `vendor/mikuproject/scripts/mikuproject-cli.mjs`

```text
upstream file:
  vendor/mikuproject/scripts/mikuproject-cli.mjs

java classes:
  jp.igapyon.mikuproject.cli.MikuprojectCli
  jp.igapyon.mikuproject.msprojectxml.MsProjectAiViews

tests:
  MikuprojectNodeParityTest.comparesSharedTextCliOutputWithNodeUpstreamWhenEnabled
  MikuprojectNodeParityTest.comparesReportDirectoryBytesWithNodeUpstreamWhenEnabled
  MikuprojectNodeParityTest.comparesReportBundleZipBytesWithNodeUpstreamWhenEnabled
  MikuprojectNodeParityTest.comparesMonthlySvgZipBytesWithNodeUpstreamWhenEnabled

diff summary:
  挙動差分:
    `state summarize`、AI projection bundle、`export workbook-json` の stdout / stderr / exit code を Node runtime と byte comparison する。shared diagnostics JSON は Node の version、status、io schema、pretty JSON format に合わせた。
  命名差分:
    Java help は Node-compatible commands と Java extensions を別節に表示する。共有 command の option signature は Node help に合わせる。
  未移植差分:
    本記録の fixture 範囲では未移植差分はない。未日付モデルの SVG fallback と Java extended XLSX layout は、下記 Java 側独自拡張として扱う。
  Java 側独自拡張:
    `state validate` / `state import` / `state merge`、`validate xml` / `validate xlsx`、`merge xlsx`、`report dir`、extended XLSX layout、未日付 SVG fallback を保持する。

follow-up:
  - 実施した確認:
    `MIKUPROJECT_RUN_NODE_PARITY=true mvn -B test`
  - fixture:
    `vendor/mikuproject/testdata/workbook-import-sample.json`、`dependency.xml`
  - 次回の確認観点:
    CLI help、diagnostics schema、workbook JSON projection、report artifact の upstream 変更
```

## 記録サンプル

### 2026-04-21 `vendor/mikuproject/src/ts/msproject-validate.ts`

```text
upstream file:
  vendor/mikuproject/src/ts/msproject-validate.ts

java classes:
  jp.igapyon.mikuproject.msprojectxml.MsProjectValidate
  jp.igapyon.mikuproject.msprojectxml.MsProjectValidateHelpers

tests:
  MsProjectXmlTest.validateProjectModelReturnsWarningsForMissingCoreFields
  MsProjectXmlTest.validateProjectModelChecksProjectRanges
  MsProjectXmlTest.validateProjectModelChecksCalendarStructures
  MsProjectXmlTest.validateProjectModelChecksFirstCutEntityReferences
  MsProjectXmlTest.validateProjectModelChecksTaskOrderIssueAndUnassignedResource
  MsProjectXmlTest.validateProjectModelWarnsWhenTasksCollapseIntoSameZeroDurationRange

diff summary:
  挙動差分:
    Java 側では project / calendars / tasks / resources / assignments の整合 warning / error、task order、unassigned resource UID の扱い、zero duration cluster warning を validation で確認している。現時点で upstream 追随上の大きな差分は記録していない。
  命名差分:
    upstream の validate 本体と helper 群に対して、Java 側は `MsProjectValidate` と `MsProjectValidateHelpers` に分割しているが、validation 本体と describe / parse helper の責務対応は追跡可能である。
  未移植差分:
    現時点で顕在化している未移植差分は記録していない。今後 upstream で validation rule や severity 分類が変わった場合は再確認が必要である。
  Java 側独自拡張:
    Java 側の zero duration cluster warning は runtime docs に沿った補助 validation として扱う。

follow-up:
  - 実施した確認:
    `mvn test -Dtest=MsProjectXmlTest`
  - fixture:
    validation focused tests は手組み model が中心
  - 次回の確認観点:
    upstream 側で validation rule、warning / error の文言、placeholder / unassigned resource の扱いが変わった場合は再確認する
  - `docs/remaining-migration-items.md` への反映:
    2026-04-21 時点で validation の upstream follow-up 実例を追加
```

### 2026-04-21 `vendor/mikuproject/src/ts/msproject-codec.ts`

```text
upstream file:
  vendor/mikuproject/src/ts/msproject-codec.ts

java classes:
  jp.igapyon.mikuproject.msprojectxml.MsProjectCodec
  jp.igapyon.mikuproject.msprojectxml.MsProjectXmlDom

tests:
  MsProjectXmlTest.importFromXmlReadsProjectCoreFields
  MsProjectXmlTest.importFromXmlReadsFirstCutEntities
  MsProjectXmlTest.exportToXmlWritesProjectCoreFields
  MsProjectXmlTest.exportToXmlWritesFirstCutEntities
  MsProjectXmlTest.roundTripsUpstreamMinimalXmlFixture
  MsProjectXmlTest.roundTripsUpstreamHierarchyXmlFixture
  MsProjectXmlTest.roundTripsUpstreamDependencyXmlFixture

diff summary:
  挙動差分:
    現時点で大きな差分は見当たらない。Java 側でも project core fields、Calendars / Tasks / Resources / Assignments の first cut entity、upstream fixture の round-trip を確認済みである。
  命名差分:
    upstream の `msproject-codec.ts` に対して、Java 側は `MsProjectCodec` を主体にし、下位 parse helper を `MsProjectXmlDom` に分けているが、codec 境界との対応は追跡可能である。
  未移植差分:
    現時点で顕在化している未移植差分は記録していない。今後 upstream で XML field coverage や serialize 順が変わった場合は再確認が必要である。
  Java 側独自拡張:
    `MsProjectXml` 公開面から codec を呼ぶ構成や validation / calendar 補完の連携は wrapper 側として扱う。

follow-up:
  - 実施した確認:
    `mvn test -Dtest=MsProjectXmlTest`
  - fixture:
    `vendor/mikuproject/testdata/minimal.xml`
    `vendor/mikuproject/testdata/hierarchy.xml`
    `vendor/mikuproject/testdata/dependency.xml`
  - 次回の確認観点:
    upstream 側で XML field coverage、boolean / number parse、predecessor / baseline / timephasedData の codec 契約が変わった場合は再確認する
  - `docs/remaining-migration-items.md` への反映:
    2026-04-21 時点で XML codec の upstream follow-up 実例を追加
```

### 2026-04-21 `vendor/mikuproject/src/ts/msproject-xml.ts`

```text
upstream file:
  vendor/mikuproject/src/ts/msproject-xml.ts

java classes:
  jp.igapyon.mikuproject.msprojectxml.MsProjectXml
  jp.igapyon.mikuproject.msprojectxml.MsProjectCodec
  jp.igapyon.mikuproject.msprojectxml.MsProjectCalendar
  jp.igapyon.mikuproject.msprojectxml.MsProjectValidate

tests:
  MsProjectXmlTest.roundTripsUpstreamMinimalXmlFixture
  MsProjectXmlTest.importsUpstreamHierarchyXmlFixture
  MsProjectXmlTest.roundTripsUpstreamHierarchyXmlFixture
  MsProjectXmlTest.importsUpstreamDependencyXmlFixture
  MsProjectXmlTest.roundTripsUpstreamDependencyXmlFixture
  MsProjectXmlTest.ensureDefaultProjectCalendarBuildsJapaneseHolidayExceptions

diff summary:
  挙動差分:
    現時点で大きな差分は見当たらない。Java 側でも `MsProjectXml` を入口に minimal / hierarchy / dependency fixture の import / round-trip と、project date range に応じた default calendar 補完を確認済みである。
  命名差分:
    upstream の公開入口 `msproject-xml.ts` に対して、Java 側は `MsProjectXml` を公開面にし、codec / calendar / validate を補助 class に分けているが、責務対応は追跡可能である。
  未移植差分:
    現時点で顕在化している未移植差分は記録していない。今後 upstream で `MsProjectXml` 公開導線や normalize / validate 呼び出し順が変わった場合は再確認が必要である。
  Java 側独自拡張:
    Java 側 validation の補助 warning や CLI / Core API からの利用導線は wrapper 側の運用拡張として扱う。

follow-up:
  - 実施した確認:
    `mvn test -Dtest=MsProjectXmlTest`
  - fixture:
    `vendor/mikuproject/testdata/minimal.xml`
    `vendor/mikuproject/testdata/hierarchy.xml`
    `vendor/mikuproject/testdata/dependency.xml`
  - 次回の確認観点:
    upstream 側で public entry の normalize / ensureDefaultProjectCalendar / validate 契約、または round-trip fixture の期待が変わった場合は再確認する
  - `docs/remaining-migration-items.md` への反映:
    2026-04-21 時点で XML public entry の upstream follow-up 実例を追加
```

### 2026-04-21 `vendor/mikuproject/src/ts/msproject-mermaid.ts`

```text
upstream file:
  vendor/mikuproject/src/ts/msproject-mermaid.ts

java classes:
  jp.igapyon.mikuproject.msprojectxml.MsProjectMermaid

tests:
  MsProjectMermaidTest.exportsMermaidFromSampleProject
  MsProjectMermaidTest.keepsComplexMermaidDependenciesAsComments
  MsProjectMermaidTest.sanitizesDateLeadingMermaidGanttLabels

diff summary:
  挙動差分:
    現時点で大きな差分は見当たらない。Java 側でも gantt title / section の正規化、single predecessor の native dependency 化、複雑依存の comment 化、date-leading label sanitize を確認済みである。
  命名差分:
    Java 側は `MsProjectMermaid` 単独 class で保持しているが、upstream file の責務境界との対応は追跡可能である。
  未移植差分:
    現時点で顕在化している未移植差分は記録していない。今後 upstream で Mermaid tag や dependency comment 形式が変わった場合は再確認が必要である。
  Java 側独自拡張:
    `MsProjectXml.exportMermaidGantt(...)` からの公開導線は Java 側 wrapper として扱う。

follow-up:
  - 実施した確認:
    `mvn test -Dtest=MsProjectMermaidTest`
  - fixture:
    `vendor/mikuproject/testdata/hierarchy.xml`
  - 次回の確認観点:
    upstream 側で native dependency 判定条件、`dependency(note)` / `dependency(pseudo)` comment 文言、label sanitize 規則が変わった場合は再確認する
  - `docs/remaining-migration-items.md` への反映:
    2026-04-21 時点で Mermaid export の upstream follow-up 実例を追加
```

### 2026-04-20 `vendor/mikuproject/src/ts/core-api-report.ts`

```text
upstream file:
  vendor/mikuproject/src/ts/core-api-report.ts

java classes:
  jp.igapyon.mikuproject.coreapi.CoreApiReport
  jp.igapyon.mikuproject.coreapi.CoreApiReportAdapters
  jp.igapyon.mikuproject.coreapi.CoreApiReportPublic

tests:
  CoreApiPublicTest.exposesWorkingReportApiSurface
  CoreApiPublicTest.exposesWorkingReportApiSurfaceForDependencyFixture
  CoreApiPublicTest.exposesWorkingReportApiSurfaceForHierarchyFixture
  MikuprojectNodeParityTest.comparesReportBundleZipBytesWithNodeUpstreamWhenEnabled
  MikuprojectCliTest.exportsReportBundleAndReportDirForFixturesThroughCli
  MikuprojectCliTest.appliesWbsOptionArgumentsToReportBundleAndWbsXlsx
  MikuprojectCliTest.keepsReportBundleAndReportDirOutputsEquivalentToStandaloneWbsXlsx

diff summary:
  挙動差分:
    現時点で大きな差分は見当たらない。Java 側では report bundle の entry 構成と entry 順、dependency / hierarchy fixture の主要内容、bundle 内 `wbs.xlsx` decode、`dependency.xml` の Node upstream bundle ZIP byte-level parity を確認済みである。加えて CLI 保守回帰で、dependency fixture に対する report bundle zip と report dir の entry 名 / entry bytes、および standalone WBS XLSX report と report 同梱 `wbs.xlsx` の byte 一致を固定した。
  命名差分:
    Java 側は `CoreApiReport` / `CoreApiReportAdapters` / `CoreApiReportPublic` に分割しているが、`report` 公開面との対応は追跡可能である。
  未移植差分:
    現時点で顕在化している未移植差分は記録していない。今後 upstream で report entry や option が増えた場合は再確認が必要である。
  Java 側独自拡張:
    `report directory` export と CLI diagnostics は upstream 本体の `core-api-report.ts` ではなく、Java CLI 運用拡張として扱う。

follow-up:
  - 実施した確認:
    `mvn test -Dtest=WbsMarkdownTest,WbsSvgTest,WbsXlsxTest,CoreApiPublicTest,MikuprojectCliTest`
    `MIKUPROJECT_RUN_NODE_PARITY=true mvn test -Dtest=MikuprojectNodeParityTest`
  - fixture:
    `vendor/mikuproject/testdata/dependency.xml`
    `vendor/mikuproject/testdata/hierarchy.xml`
  - 次回の確認観点:
    upstream 側で report bundle entry や option 契約が変わった場合は、Core API と CLI の両方を見直す。特に report dir / bundle / standalone `wbs.xlsx` の同等性が崩れていないかを再確認する
  - `docs/remaining-migration-items.md` への反映:
    2026-04-21 時点で report bundle / report dir / standalone `wbs.xlsx` の同等性回帰を反映
```

### 2026-04-20 `vendor/mikuproject/src/ts/core-api-import.ts`

```text
upstream file:
  vendor/mikuproject/src/ts/core-api-import.ts

java classes:
  jp.igapyon.mikuproject.coreapi.CoreApiImport
  jp.igapyon.mikuproject.coreapi.CoreApiAiJsonImport
  jp.igapyon.mikuproject.coreapi.CoreApiExternalImport
  jp.igapyon.mikuproject.coreapi.CoreApiExternalDocument

tests:
  CoreApiImportTest.parsesFencedAiJsonTextAndDetectsKind
  CoreApiImportTest.importsProjectDraftViewWithoutUiDependencies
  CoreApiImportTest.importsWorkbookJsonWithAndWithoutBaseModel
  CoreApiImportTest.importsAiJsonTextAndExposesAiJsonSpec
  CoreApiImportTest.importsExternalFormatsThroughImportExternal
  CoreApiImportTest.appliesPatchJsonThroughUnifiedEntryPoint
  CoreApiImportTest.rejectsPatchJsonWhenBaseModelIsMissing
  CoreApiImportTest.rejectsUnsupportedFormatAndModeCombinationsInImportExternal
  CoreApiImportTest.rejectsMergeImportsWhenBaseModelIsMissing
  CoreApiImportTest.roundTripsHierarchyFixtureThroughUnifiedImportWrappers

diff summary:
  挙動差分:
    現時点で大きな差分は見当たらない。Java 側では `project_draft_view` / `workbook_json` / `patch_json` / `ms_project_xml` / `xlsx` の import と `kind` / `mode` を確認済みである。
  命名差分:
    Java 側は AI JSON parse/import と external import を複数 class に分割しているが、`CoreApiImport` を入口にした unified import の対応は追跡可能である。
  未移植差分:
    現時点で顕在化している未移植差分は記録していない。今後 upstream で import source や mode 制約が増えた場合は再確認が必要である。
  Java 側独自拡張:
    CLI entrypoint や diagnostics は upstream 本体の `core-api-import.ts` ではなく、Java CLI 運用拡張として扱う。

follow-up:
  - 実施した確認:
    `mvn test -Dtest=CoreApiImportTest`
  - fixture:
    `vendor/mikuproject/testdata/dependency.xml`
    `vendor/mikuproject/testdata/hierarchy.xml`
  - 次回の確認観点:
    upstream 側で `source.format` や `mode` 制約が変わった場合は、AI JSON import と external import の両方を見直す
  - `docs/remaining-migration-items.md` への反映:
    2026-04-20 時点では追加反映なし
```

### 2026-04-20 `vendor/mikuproject/src/ts/msproject-ai-views.ts`

```text
upstream file:
  vendor/mikuproject/src/ts/msproject-ai-views.ts

java classes:
  jp.igapyon.mikuproject.msprojectxml.MsProjectAiViews
  jp.igapyon.mikuproject.coreapi.CoreApiMsprojectAi
  jp.igapyon.mikuproject.coreapi.CoreApiRegistry

tests:
  MsProjectAiViewsTest.exportsProjectOverviewAndDefaultPhaseDetailViewsFromHierarchy
  MsProjectAiViewsTest.exportsTaskEditViewWithPredecessorsSuccessorsAndAssignments
  MsProjectAiViewsTest.exportsScopedPhaseDetailAndRejectsInvalidRootUid
  MsProjectAiViewsTest.buildsProjectDraftRequestAndImportsPredecessorMappingFromProjectDraftView
  MsProjectAiViewsTest.importsProjectDraftViewWithoutTaskDatesUsingProjectStartFallback
  MsProjectAiViewsTest.rejectsInvalidProjectDraftViewReferences
  MikuprojectCliTest.exportsWorkbookJsonAndAiViews
  MikuprojectCliTest.exportsScopedPhaseDetailAndRejectsInvalidRootUidThroughCli

diff summary:
  挙動差分:
    現時点で大きな差分は見当たらない。Java 側では `project_overview_view` / `phase_detail_view` / `task_edit_view` / `project_draft_request` と scoped phase detail の正常系 / 異常系を確認済みである。あわせて upstream / Java とも `project_draft_view` import では task ごとの `planned_start` / `planned_finish` を必須にしておらず、欠けた場合は `project.planned_start` / `planned_finish` / current time fallback を使うことを確認した。
  命名差分:
    Java 側は `MsProjectAiViews` と Core API / CLI 導線に分かれるが、view kind と command 名の対応は追跡可能である。
  未移植差分:
    現時点で顕在化している未移植差分は記録していない。今後 upstream で view field や scoped option が増えた場合は再確認が必要である。
  Java 側独自拡張:
    CLI batch export や diagnostics は upstream 本体の `msproject-ai-views.ts` ではなく、Java CLI 運用拡張として扱う。

follow-up:
  - 実施した確認:
    `mvn test -Dtest=MsProjectAiViewsTest`
    `mvn test -Dtest=MsProjectAiViewsTest,MikuprojectCliTest`
  - fixture:
    `vendor/mikuproject/testdata/hierarchy.xml`
  - 次回の確認観点:
    upstream 側で Agent Skills 向け `project_draft_view` の生成規約が task 日付必須へ変わる、または zero duration 入力への warning 方針が追加された場合は、unit test と CLI export / runtime docs を見直す
  - `docs/remaining-migration-items.md` への反映:
    2026-04-21 時点で `project_draft_view` の task 日付 fallback 契約確認を TODO / test mapping に反映
```

### 2026-04-20 `vendor/mikuproject/src/ts/wbs-svg.ts`

```text
upstream file:
  vendor/mikuproject/src/ts/wbs-svg.ts

java classes:
  jp.igapyon.mikuproject.wbssvg.WbsSvg
  jp.igapyon.mikuproject.wbssvg.WbsSvgPublic
  jp.igapyon.mikuproject.wbssvg.WbsSvgRender
  jp.igapyon.mikuproject.wbssvg.WbsSvgCalendar
  jp.igapyon.mikuproject.wbssvg.WbsSvgZip

tests:
  WbsSvgTest.exportsDailyAndWeeklySvg
  WbsSvgTest.rendersDependencyConnectorsInDailyAndWeeklySvg
  WbsSvgTest.exportsMonthlyCalendarArchive
  WbsSvgTest.exportsMonthlyCalendarArchiveForSampleAndFixtureRanges
  WbsSvgTest.exportsDependencyFixtureIntoSvgOutputs
  WbsSvgTest.exportsHierarchyFixtureIntoSvgOutputs
  WbsSvgTest.appliesLabelAndHolidayOptionsToSvgOutputs
  CoreApiPublicTest.exposesWorkingReportApiSurfaceForDependencyFixture
  CoreApiPublicTest.exposesWorkingReportApiSurfaceForHierarchyFixture
  MikuprojectCliTest.appliesSvgOptionArgumentsToSvgExports
  MikuprojectCliTest.exportsReportBundleAndReportDirForFixturesThroughCli

diff summary:
  挙動差分:
    現時点で大きな差分は見当たらない。Java 側では daily / weekly / monthly SVG の出力、dependency connector、hierarchy / dependency fixture の主要内容、label / holiday option を確認済みである。加えて monthly calendar が sample / dependency / hierarchy の 3 系統で project range に含まれる月数を出し、`YYYY-MM.svg` file 名と各 SVG 非空を保つことを固定した。
  命名差分:
    Java 側は render / calendar / zip helper に分割しているが、`WbsSvg` 公開面との対応は追跡可能である。
  未移植差分:
    現時点で顕在化している未移植差分は記録していない。今後 upstream で SVG option や月次 archive 構成が増えた場合は再確認が必要である。
  Java 側独自拡張:
    report bundle / report directory 連携と CLI option parse は upstream 本体の `wbs-svg.ts` ではなく、Java CLI / report 運用拡張として扱う。

follow-up:
  - 実施した確認:
    `mvn test -Dtest=WbsSvgTest,CoreApiPublicTest,MikuprojectCliTest`
  - fixture:
    sample project
    `vendor/mikuproject/testdata/dependency.xml`
    `vendor/mikuproject/testdata/hierarchy.xml`
  - 次回の確認観点:
    upstream 側で label mode、holiday 表示、monthly archive entry 構成が変わった場合は、unit / core API / CLI の 3 層を見直す。特に month span と `YYYY-MM.svg` 命名規則が変わっていないかを再確認する
  - `docs/remaining-migration-items.md` への反映:
    2026-04-21 時点で monthly calendar の 3 系統 coverage と file 名 / 非空確認を反映
```

### 2026-04-20 `vendor/mikuproject/src/ts/wbs-markdown.ts`

```text
upstream file:
  vendor/mikuproject/src/ts/wbs-markdown.ts

java classes:
  jp.igapyon.mikuproject.wbsmarkdown.WbsMarkdown
  jp.igapyon.mikuproject.wbsmarkdown.WbsMarkdownPublic

tests:
  WbsMarkdownTest.exportsOneMarkdownDocumentWithTreeFirstAndTableAfterIt
  WbsMarkdownTest.showsNotesInTheTreeSectionAndSummaryAfterTheTable
  WbsMarkdownTest.escapesMarkdownSensitiveTextInTableCells
  WbsMarkdownTest.usesFenceThatDoesNotBreakWhenTreeTextIncludesBackticks
  WbsMarkdownTest.keepsDeepHierarchyReadable
  WbsMarkdownTest.exportsHierarchyFixtureIntoReadableMarkdown
  WbsMarkdownTest.exportsDependencyFixtureIntoReadableMarkdown
  WbsMarkdownTest.appliesDisplayAndHolidayOptionsToMarkdown
  MikuprojectCliTest.appliesWbsOptionArgumentsToMarkdownAndReportDir
  MikuprojectCliTest.exportsReportBundleAndReportDirForFixturesThroughCli

diff summary:
  挙動差分:
    現時点で大きな差分は見当たらない。Java 側では tree/table 構成、notes 表示、Markdown escape、hierarchy / dependency fixture、display / progress option を確認済みである。
  命名差分:
    Java 側は `WbsMarkdown` と `WbsMarkdownPublic` に分かれるが、Markdown 出力公開面との対応は追跡可能である。
  未移植差分:
    現時点で顕在化している未移植差分は記録していない。今後 upstream で summary 行や option 契約が増えた場合は再確認が必要である。
  Java 側独自拡張:
    report directory / report bundle 連携と CLI option parse は upstream 本体の `wbs-markdown.ts` ではなく、Java CLI / report 運用拡張として扱う。

follow-up:
  - 実施した確認:
    `mvn test -Dtest=WbsMarkdownTest,MikuprojectCliTest`
  - fixture:
    `vendor/mikuproject/testdata/hierarchy.xml`
    `vendor/mikuproject/testdata/dependency.xml`
  - 次回の確認観点:
    upstream 側で summary 行、tree 表示、display / progress option が変わった場合は、unit と CLI の両方を見直す
  - `docs/remaining-migration-items.md` への反映:
    2026-04-20 時点では追加反映なし
```

### 2026-04-20 `vendor/mikuproject/src/ts/wbs-xlsx.ts`

```text
upstream file:
  vendor/mikuproject/src/ts/wbs-xlsx.ts

java classes:
  jp.igapyon.mikuproject.wbsxlsx.WbsXlsx
  jp.igapyon.mikuproject.wbsxlsx.WbsXlsxLayout
  jp.igapyon.mikuproject.wbsxlsx.WbsXlsxPublic

tests:
  WbsXlsxTest.providesExcelStyleLayoutReferencesForWbsWorksheetTuning
  WbsXlsxTest.canLogWbsLayoutCellReferencesOnDemand
  WbsXlsxTest.exportsDedicatedWbsWorkbookAndCanEncodeIt
  WbsXlsxTest.exportsHierarchyFixtureIntoDedicatedWorkbook
  WbsXlsxTest.exportsDependencyFixtureIntoDedicatedWorkbook
  WbsXlsxTest.appliesDisplayAndHolidayOptionsToDedicatedWorkbook
  CoreApiPublicTest.exposesWorkingReportApiSurface
  CoreApiPublicTest.exposesWorkingReportApiSurfaceForHierarchyFixture
  MikuprojectCliTest.appliesWbsOptionArgumentsToReportBundleAndWbsXlsx
  MikuprojectCliTest.exportsReportBundleAndReportDirForFixturesThroughCli

diff summary:
  挙動差分:
    現時点で大きな差分は見当たらない。Java 側では dedicated workbook 出力、layout 参照、hierarchy / dependency fixture、display / holiday option、report bundle 同梱を確認済みである。
  命名差分:
    Java 側は workbook 本体、layout helper、public wrapper に分かれるが、`WBS XLSX` 出力公開面との対応は追跡可能である。
  未移植差分:
    現時点で顕在化している未移植差分は記録していない。今後 upstream で workbook layout や option 契約が増えた場合は再確認が必要である。
  Java 側独自拡張:
    report bundle 連携と CLI option parse は upstream 本体の `wbs-xlsx.ts` ではなく、Java CLI / report 運用拡張として扱う。

follow-up:
  - 実施した確認:
    `mvn test -Dtest=WbsXlsxTest,CoreApiPublicTest,MikuprojectCliTest`
  - fixture:
    `vendor/mikuproject/testdata/hierarchy.xml`
    `vendor/mikuproject/testdata/dependency.xml`
  - 次回の確認観点:
    upstream 側で workbook layout、summary 行、display / holiday option が変わった場合は、unit / core API / CLI の 3 層を見直す
  - `docs/remaining-migration-items.md` への反映:
    2026-04-20 時点では追加反映なし
```

### 2026-04-20 `vendor/mikuproject/src/ts/project-workbook-json.ts`

```text
upstream file:
  vendor/mikuproject/src/ts/project-workbook-json.ts

java classes:
  jp.igapyon.mikuproject.projectworkbookjson.ProjectWorkbookJson
  jp.igapyon.mikuproject.projectworkbookjson.ProjectWorkbookJsonExport
  jp.igapyon.mikuproject.projectworkbookjson.ProjectWorkbookJsonImport
  jp.igapyon.mikuproject.projectworkbookjson.ProjectWorkbookJsonValidate

tests:
  ProjectWorkbookJsonTest.exportsWorkbookJsonWithFixedFormatAndSheets
  ProjectWorkbookJsonTest.importsLimitedEditableFieldsThroughWorkbookJson
  ProjectWorkbookJsonTest.rejectsInvalidWorkbookJsonFormat
  ProjectWorkbookJsonTest.reportsWarningsForUnknownSheetAndUnknownColumns
  ProjectWorkbookJsonTest.rejectsNonArraySheetsAndNonObjectRows
  ProjectWorkbookJsonTest.keepsNonEditableTaskColumnsUnchangedThroughWorkbookJsonImport
  ProjectWorkbookJsonTest.roundTripsHierarchyFixtureThroughWorkbookJson
  CoreApiWorkbookTest.importsWorkbookJsonWithAndWithoutBaseModel
  CoreApiWorkbookTest.validatesWorkbookJsonAndAppliesPatchJson
  CoreApiWorkbookTest.roundTripsHierarchyFixtureThroughWorkbookWrappers

diff summary:
  挙動差分:
    現時点で大きな差分は見当たらない。Java 側では fixed format/sheets、editable field 制約、warning / reject 系、hierarchy fixture round-trip を確認済みである。
  命名差分:
    Java 側は export / import / validate に分割しているが、`ProjectWorkbookJson` 公開面との対応は追跡可能である。
  未移植差分:
    現時点で顕在化している未移植差分は記録していない。今後 upstream で sheet 構成や editable field 契約が変わった場合は再確認が必要である。
  Java 側独自拡張:
    Core API wrapper 経由の統一導線は upstream 本体の `project-workbook-json.ts` ではなく、Java 側の wrapper 層として扱う。

follow-up:
  - 実施した確認:
    `mvn test -Dtest=ProjectWorkbookJsonTest,CoreApiWorkbookTest`
  - fixture:
    `vendor/mikuproject/testdata/hierarchy.xml`
  - 次回の確認観点:
    upstream 側で workbook JSON の schema、editable field、warning 契約が変わった場合は、unit と Core API wrapper の両方を見直す
  - `docs/remaining-migration-items.md` への反映:
    2026-04-20 時点では追加反映なし
```

### 2026-04-20 `vendor/mikuproject/src/ts/core-api-workbook.ts`

```text
upstream file:
  vendor/mikuproject/src/ts/core-api-workbook.ts

java classes:
  jp.igapyon.mikuproject.coreapi.CoreApiWorkbook
  jp.igapyon.mikuproject.coreapi.CoreApiWorkbookJson
  jp.igapyon.mikuproject.coreapi.CoreApiWorkbookXlsx

tests:
  CoreApiWorkbookTest.importsWorkbookJsonWithAndWithoutBaseModel
  CoreApiWorkbookTest.validatesWorkbookJsonAndAppliesPatchJson
  CoreApiWorkbookTest.exposesProjectXlsxThroughUnifiedEntryPoint
  CoreApiWorkbookTest.encodesAndDecodesWorkbookThroughUnifiedEntryPoint
  CoreApiWorkbookTest.roundTripsHierarchyFixtureThroughWorkbookWrappers
  ProjectWorkbookJsonTest.roundTripsHierarchyFixtureThroughWorkbookJson
  ProjectXlsxTest.roundTripsHierarchyFixtureThroughWorkbook

diff summary:
  挙動差分:
    現時点で大きな差分は見当たらない。Java 側では workbook JSON / xlsx wrapper、encode/decode、validate / patch、hierarchy fixture round-trip を確認済みである。
  命名差分:
    Java 側は workbook JSON / xlsx の wrapper に分割しているが、`CoreApiWorkbook` を中心にした公開面との対応は追跡可能である。
  未移植差分:
    現時点で顕在化している未移植差分は記録していない。今後 upstream で workbook wrapper の mode や公開 API が増えた場合は再確認が必要である。
  Java 側独自拡張:
    CLI 導線は upstream 本体の `core-api-workbook.ts` ではなく、Java CLI 側の entrypoint 拡張として扱う。

follow-up:
  - 実施した確認:
    `mvn test -Dtest=ProjectWorkbookJsonTest,ProjectXlsxTest,CoreApiWorkbookTest`
  - fixture:
    `vendor/mikuproject/testdata/hierarchy.xml`
  - 次回の確認観点:
    upstream 側で workbook wrapper の公開面や round-trip 契約が変わった場合は、unit と Core API wrapper の両方を見直す
  - `docs/remaining-migration-items.md` への反映:
    2026-04-20 時点では追加反映なし
```

### 2026-04-21 `vendor/mikuproject/src/ts/project-patch-json-entities.ts`

```text
upstream file:
  vendor/mikuproject/src/ts/project-patch-json-entities.ts
  vendor/mikuproject/src/ts/project-patch-json-tasks.ts

java classes:
  jp.igapyon.mikuproject.projectpatchjson.ProjectPatchJsonEntities
  jp.igapyon.mikuproject.projectpatchjson.ProjectPatchJsonTasks
  jp.igapyon.mikuproject.projectpatchjson.ProjectPatchJsonCore

tests:
  ProjectPatchJsonTest.reportsPatchJsonWarningDetailsWithoutMainUi
  ProjectPatchJsonTasksTest.rejectsDeleteTaskWhenReferencesRemain
  CoreApiWorkbookTest
  MikuprojectCliTest

diff summary:
  挙動差分:
    `delete_task`, `delete_resource`, `delete_calendar` は Java 側でも upstream と同じく first cut 制限の blocker details を warning message に含めていた。`delete_assignment` は upstream 側に存在するが Java 側 dispatch / 実装が欠けていたため、assignment 削除と `taskUid` / `resourceUid` の changes を追加した。
  命名差分:
    Java 側は entity delete 系を `ProjectPatchJsonEntities`、task delete 系を `ProjectPatchJsonTasks` に分けているが、upstream file の責務境界との対応は追跡可能である。
  未移植差分:
    現時点で delete 系 warning / changes の顕在差分は記録していない。今後 upstream で blocker detail の形式や delete operation が増えた場合は再確認が必要である。
  Java 側独自拡張:
    CLI / Core API 経由の patch JSON 実行は Java 側 wrapper 層として扱う。

follow-up:
  - 実施した確認:
    `mvn test -Dtest=ProjectPatchJsonTest,ProjectPatchJsonTasksTest`
    `mvn test -Dtest=ProjectPatchJsonTest,ProjectPatchJsonTasksTest,CoreApiWorkbookTest,MikuprojectCliTest`
  - fixture:
    `vendor/mikuproject/testdata/dependency.xml`
    `vendor/mikuproject/testdata/hierarchy.xml`
  - 次回の確認観点:
    upstream 側で delete 系 operation、warning scope / uid / label、changes の field 名が変わった場合は unit と CLI / Core API wrapper の両方を見直す
  - `docs/remaining-migration-items.md` への反映:
    2026-04-21 時点で Project Patch JSON delete 系 warning / changes 補強の確認結果を反映
```

### 2026-04-20 `vendor/mikuproject/src/ts/project-xlsx.ts`

```text
upstream file:
  vendor/mikuproject/src/ts/project-xlsx.ts

java classes:
  jp.igapyon.mikuproject.projectxlsx.ProjectXlsx
  jp.igapyon.mikuproject.projectxlsx.ProjectXlsxExport
  jp.igapyon.mikuproject.projectxlsx.ProjectXlsxImport

tests:
  ProjectXlsxTest.convertsProjectModelIntoWorkbookSheets
  ProjectXlsxTest.importsLimitedEditableFieldsThroughWorkbook
  ProjectXlsxTest.importsWorkbookAsProjectModel
  ProjectXlsxTest.roundTripsHierarchyFixtureThroughWorkbook
  CoreApiWorkbookTest.exposesProjectXlsxThroughUnifiedEntryPoint
  CoreApiWorkbookTest.encodesAndDecodesWorkbookThroughUnifiedEntryPoint
  CoreApiWorkbookTest.roundTripsHierarchyFixtureThroughWorkbookWrappers

diff summary:
  挙動差分:
    以前は列幅、boolean data validation、boolean option sheet、formula / freezePane 対応、editable cell styling、sheet theme、Project sheet の Settings section / merged range が薄かった。Java 側では `formula` / `freezePane` を workbook-like model と OOXML build / parse に追加し、Project XLSX export へ列幅、boolean data validation、boolean option sheet、editable cell styling、sheet theme、Project sheet の Settings section / merged range を反映した。さらに `ExcelIoTest` / `ProjectXlsxTest` で OOXML zip entry、worksheet XML、data validation、freeze pane、主要セル style を確認し、簡略実装へ戻っていないことを保守回帰で固定した。
  命名差分:
    Java 側は export / import に分割しているが、`ProjectXlsx` 公開面との対応は追跡可能である。
  未移植差分:
    現時点で顕在化している Project XLSX layout の未移植差分は記録していない。今後 upstream で workbook sheet 構成や import 契約が変わった場合は再確認が必要である。
  Java 側独自拡張:
    Core API wrapper や encode/decode 導線は upstream 本体の `project-xlsx.ts` ではなく、Java 側の wrapper 層として扱う。

follow-up:
  - 実施した確認:
    `mvn test -Dtest=ProjectXlsxTest,ExcelIoTest`
    `mvn test -Dtest=ProjectWorkbookJsonTest,ProjectXlsxTest,CoreApiWorkbookTest`
    `mvn test`
  - fixture:
    `vendor/mikuproject/testdata/hierarchy.xml`
  - 次回の確認観点:
    upstream 側で workbook sheet 構成、editable field styling、sheet theme、round-trip 契約が変わった場合は、unit と Core API wrapper の両方を見直す。特に OOXML entry 構成、worksheet XML、style / validation / freezePane 契約が崩れていないかを再確認する
  - `docs/remaining-migration-items.md` への反映:
    2026-04-21 時点で report directory byte-level parity、Project XLSX layout 補強、OOXML 回帰テスト根拠を反映
```

### 2026-04-20 `vendor/mikuproject/src/ts/core-api-workbook-xlsx.ts`

```text
upstream file:
  vendor/mikuproject/src/ts/core-api-workbook-xlsx.ts

java classes:
  jp.igapyon.mikuproject.coreapi.CoreApiWorkbookXlsx

tests:
  CoreApiWorkbookTest.exposesProjectXlsxThroughUnifiedEntryPoint
  CoreApiWorkbookTest.encodesAndDecodesWorkbookThroughUnifiedEntryPoint
  CoreApiWorkbookTest.roundTripsHierarchyFixtureThroughWorkbookWrappers
  ProjectXlsxTest.roundTripsHierarchyFixtureThroughWorkbook

diff summary:
  挙動差分:
    現時点で大きな差分は見当たらない。Java 側では xlsx wrapper の export / import、encode / decode、hierarchy fixture round-trip を確認済みである。
  命名差分:
    Java 側は `CoreApiWorkbookXlsx` として切り出しており、upstream の workbook xlsx wrapper 名との対応は追跡可能である。
  未移植差分:
    現時点で顕在化している未移植差分は記録していない。今後 upstream で xlsx wrapper の公開 API や mode 契約が変わった場合は再確認が必要である。
  Java 側独自拡張:
    CLI entrypoint や report 導線は upstream 本体の `core-api-workbook-xlsx.ts` ではなく、Java 側の CLI / wrapper 運用拡張として扱う。

follow-up:
  - 実施した確認:
    `mvn test -Dtest=ProjectXlsxTest,CoreApiWorkbookTest`
  - fixture:
    `vendor/mikuproject/testdata/hierarchy.xml`
  - 次回の確認観点:
    upstream 側で xlsx wrapper の公開面や encode / decode 契約が変わった場合は、unit と Core API wrapper の両方を見直す
  - `docs/remaining-migration-items.md` への反映:
    2026-04-20 時点では追加反映なし
```
