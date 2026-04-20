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
- AI view export / import:
  - `vendor/mikuproject/src/ts/msproject-ai-views.ts`
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

## 記録サンプル

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

diff summary:
  挙動差分:
    現時点で大きな差分は見当たらない。Java 側では report bundle の entry 構成と entry 順、dependency / hierarchy fixture の主要内容、bundle 内 `wbs.xlsx` decode、`dependency.xml` の Node upstream bundle ZIP byte-level parity を確認済みである。
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
    upstream 側で report bundle entry や option 契約が変わった場合は、Core API と CLI の両方を見直す
  - `docs/remaining-migration-items.md` への反映:
    2026-04-20 時点では追加反映なし
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
  MsProjectAiViewsTest.rejectsInvalidProjectDraftViewReferences
  MikuprojectCliTest.exportsWorkbookJsonAndAiViews
  MikuprojectCliTest.exportsScopedPhaseDetailAndRejectsInvalidRootUidThroughCli

diff summary:
  挙動差分:
    現時点で大きな差分は見当たらない。Java 側では `project_overview_view` / `phase_detail_view` / `task_edit_view` / `project_draft_request` と scoped phase detail の正常系 / 異常系を確認済みである。
  命名差分:
    Java 側は `MsProjectAiViews` と Core API / CLI 導線に分かれるが、view kind と command 名の対応は追跡可能である。
  未移植差分:
    現時点で顕在化している未移植差分は記録していない。今後 upstream で view field や scoped option が増えた場合は再確認が必要である。
  Java 側独自拡張:
    CLI batch export や diagnostics は upstream 本体の `msproject-ai-views.ts` ではなく、Java CLI 運用拡張として扱う。

follow-up:
  - 実施した確認:
    `mvn test -Dtest=MsProjectAiViewsTest,MikuprojectCliTest`
  - fixture:
    `vendor/mikuproject/testdata/hierarchy.xml`
  - 次回の確認観点:
    upstream 側で scoped option や view field が変わった場合は、unit test と CLI export の両方を見直す
  - `docs/remaining-migration-items.md` への反映:
    2026-04-20 時点では追加反映なし
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
  WbsSvgTest.exportsDependencyFixtureIntoSvgOutputs
  WbsSvgTest.exportsHierarchyFixtureIntoSvgOutputs
  WbsSvgTest.appliesLabelAndHolidayOptionsToSvgOutputs
  CoreApiPublicTest.exposesWorkingReportApiSurfaceForDependencyFixture
  CoreApiPublicTest.exposesWorkingReportApiSurfaceForHierarchyFixture
  MikuprojectCliTest.appliesSvgOptionArgumentsToSvgExports
  MikuprojectCliTest.exportsReportBundleAndReportDirForFixturesThroughCli

diff summary:
  挙動差分:
    現時点で大きな差分は見当たらない。Java 側では daily / weekly / monthly SVG の出力、dependency connector、hierarchy / dependency fixture の主要内容、label / holiday option を確認済みである。
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
    `vendor/mikuproject/testdata/dependency.xml`
    `vendor/mikuproject/testdata/hierarchy.xml`
  - 次回の確認観点:
    upstream 側で label mode、holiday 表示、monthly archive entry 構成が変わった場合は、unit / core API / CLI の 3 層を見直す
  - `docs/remaining-migration-items.md` への反映:
    2026-04-20 時点では追加反映なし
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
    以前は列幅、boolean data validation、boolean option sheet、formula / freezePane 対応が薄かった。Java 側では `formula` / `freezePane` を workbook-like model と OOXML build / parse に追加し、Project XLSX export へ列幅、boolean data validation、boolean option sheet を反映した。
  命名差分:
    Java 側は export / import に分割しているが、`ProjectXlsx` 公開面との対応は追跡可能である。
  未移植差分:
    editable cell styling、sheet theme、Project sheet の Settings section / merged range はまだ upstream より薄い。今後 upstream で workbook sheet 構成や import 契約が変わった場合は再確認が必要である。
  Java 側独自拡張:
    Core API wrapper や encode/decode 導線は upstream 本体の `project-xlsx.ts` ではなく、Java 側の wrapper 層として扱う。

follow-up:
  - 実施した確認:
    `mvn test -Dtest=ProjectXlsxTest,ExcelIoTest,MikuprojectNodeParityTest`
  - fixture:
    `vendor/mikuproject/testdata/hierarchy.xml`
  - 次回の確認観点:
    upstream 側で workbook sheet 構成、editable field styling、sheet theme、round-trip 契約が変わった場合は、unit と Core API wrapper の両方を見直す
  - `docs/remaining-migration-items.md` への反映:
    2026-04-21 時点で report directory byte-level parity と Project XLSX layout 補強の現在地を反映
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
