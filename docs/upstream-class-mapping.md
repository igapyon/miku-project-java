# Upstream Class Mapping

## 目的

この文書は、Node.js upstream の主要ファイルと Java 側 class / package の対応関係を固定するためのメモである。
repo top から入るときの入口は `README.md` の `Development Docs` 節とする。

移植作業では、Java 側の作りやすさよりも upstream 追随時の見通しを重視する。
そのため、どの Java class がどの upstream file を受け持つかを、先に明示しておく。

## 差分確認テンプレート

upstream 更新追随で 1 file を確認するときは、少なくとも次の形でメモすると追跡しやすい。

```text
upstream file:
  vendor/mikuproject/src/ts/<target>.ts

java classes:
  jp.igapyon.mikuproject.<package>.<ClassA>
  jp.igapyon.mikuproject.<package>.<ClassB>

tests:
  <RelatedTest1>
  <RelatedTest2>

diff summary:
  挙動差分:
  命名差分:
  未移植差分:
  Java 側独自拡張:

follow-up:
  - 確認した test:
  - 確認した fixture 比較:
  - 不足時に補った test / fixture:
  - `docs/remaining-migration-items.md` への反映要否:
```

このテンプレートの目的は、差分確認の粒度を `upstream file -> Java class / test` で固定することである。
実際の差分確認結果は `docs/upstream-followup-log.md` に、同じ `upstream file` 単位で残す。

補足:

- `docs only` 更新では、原則として追加テストを回さない
- コード変更を含む場合、または新しい focused 回帰コマンド自体を関連文書へ追加する場合だけ、対象単位を実行して確認する
- この種の追随運用文書の整備は、`docs-only` のコミットとしてまとめてよい

最短フロー:

1. `docs/remaining-migration-items.md` で現在地と対象範囲を確認する
2. この文書で対象 upstream file に対応する Java class を引く
3. `docs/upstream-test-mapping.md` で対応 test と focused 回帰単位を引く
4. `docs/development.md` で実行する focused test command を確認する
5. 必要なら対象単位だけ test を実行し、`docs/upstream-followup-log.md` と `docs/remaining-migration-items.md` に結果を反映する

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
- workbook JSON:
  - `vendor/mikuproject/src/ts/project-workbook-json.ts`
- project xlsx:
  - `vendor/mikuproject/src/ts/project-xlsx.ts`
- workbook wrapper:
  - `vendor/mikuproject/src/ts/core-api-workbook.ts`
- workbook xlsx wrapper:
  - `vendor/mikuproject/src/ts/core-api-workbook-xlsx.ts`
- SVG report:
  - `vendor/mikuproject/src/ts/wbs-svg.ts`
- Markdown report:
  - `vendor/mikuproject/src/ts/wbs-markdown.ts`
- XLSX report:
  - `vendor/mikuproject/src/ts/wbs-xlsx.ts`

report / workbook / import / AI view の主要導線は、この文書でも sample ベースで辿れる状態にある。
現フェーズでは、新規機能追加ではなく、この対応表と既存実装 / test 対応のズレを小さく保つことを優先する。

## 差分確認サンプル

次は、`wbs-svg.ts` 周辺を確認するときの簡易サンプルである。

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
  WbsSvgTest.exportsDependencyFixtureIntoSvgOutputs
  WbsSvgTest.exportsHierarchyFixtureIntoSvgOutputs
  WbsSvgTest.appliesLabelAndHolidayOptionsToSvgOutputs
  CoreApiPublicTest.exposesWorkingReportApiSurfaceForDependencyFixture
  CoreApiPublicTest.exposesWorkingReportApiSurfaceForHierarchyFixture
  MikuprojectCliTest.appliesSvgOptionArgumentsToSvgExports
  MikuprojectCliTest.exportsReportBundleAndReportDirForFixturesThroughCli

diff summary:
  挙動差分:
    daily / weekly / monthly の出力内容、label mode、holiday 件数表示、dependency path の有無を見る
  命名差分:
    upstream の helper 分割と Java 側の class 分割が追跡可能かを見る
  未移植差分:
    upstream 側で増えた SVG option や月次 archive 挙動が Java 側へ入っているかを見る
  Java 側独自拡張:
    CLI option parse や report bundle / report directory 連携は upstream 本体とは分けて扱う

follow-up:
  - 追加/更新した test:
    WbsSvgTest / CoreApiPublicTest / MikuprojectCliTest のどこで吸収したかを明記する
  - 追加/更新した fixture 比較:
    hierarchy / dependency fixture のどちらで確認したかを明記する
  - `docs/remaining-migration-items.md` への反映要否:
    option 契約や report 精度面の残件に影響する場合だけ反映する
```

次は、`core-api-report.ts` 周辺を確認するときの簡易サンプルである。

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
  MikuprojectCliTest.exportsReportBundleAndReportDirForFixturesThroughCli
  MikuprojectCliTest.appliesWbsOptionArgumentsToReportBundleAndWbsXlsx

diff summary:
  挙動差分:
    report bundle の entry 構成、`wbs.xlsx` 同梱有無、fixture ごとの主要内容を確認する
  命名差分:
    upstream の report API 名と Java 側 adapter / public wrapper の公開名が追跡可能かを見る
  未移植差分:
    upstream 側で report entry や option が増えた場合に Java 側の `all` export と CLI 導線まで入っているかを見る
  Java 側独自拡張:
    report directory export や CLI diagnostics は `core-api-report.ts` 本体とは分けて扱う

follow-up:
  - 追加/更新した test:
    CoreApiPublicTest を基点に、必要なら MikuprojectCliTest の bundle / report dir 比較まで広げる
  - 追加/更新した fixture 比較:
    dependency / hierarchy fixture のどちらで entry 構成と内容差分を確認したかを明記する
  - `docs/remaining-migration-items.md` への反映要否:
    report 出力の精度面や option 契約の残件に影響する場合だけ反映する
```

次は、`msproject-ai-views.ts` 周辺を確認するときの簡易サンプルである。

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
    `project_overview_view` / `phase_detail_view` / `task_edit_view` / `project_draft_request` の出力内容と参照整合を確認する
  命名差分:
    upstream の view kind 名と Java 側 JSON field 名、CLI command 名が追跡可能かを見る
  未移植差分:
    upstream 側で view field や scoped option が増えた場合に Java 側の unit / CLI 導線へ入っているかを見る
  Java 側独自拡張:
    CLI batch export や diagnostics は `msproject-ai-views.ts` 本体とは分けて扱う

follow-up:
  - 追加/更新した test:
    まず MsProjectAiViewsTest で吸収し、CLI 契約に影響する場合だけ MikuprojectCliTest まで広げる
  - 追加/更新した fixture 比較:
    hierarchy fixture の overview / scoped phase detail で確認したかを明記する
  - `docs/remaining-migration-items.md` への反映要否:
    AI view export/import の残件や command 契約に影響する場合だけ反映する
```

次は、`core-api-import.ts` 周辺を確認するときの簡易サンプルである。

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
    `project_draft_view` / `workbook_json` / `patch_json` / `ms_project_xml` / `xlsx` の import 結果と `mode` 判定を確認する
  命名差分:
    upstream の `kind` / `mode` / `source.format` 名と Java 側 API の field 名が追跡可能かを見る
  未移植差分:
    upstream 側で import source や mode 制約が増えた場合に Java 側の unified import と wrapper 導線へ入っているかを見る
  Java 側独自拡張:
    CLI entrypoint や diagnostics は `core-api-import.ts` 本体とは分けて扱う

follow-up:
  - 追加/更新した test:
    まず CoreApiImportTest で吸収し、CLI 契約に影響する場合だけ MikuprojectCliTest の import 系まで広げる
  - 追加/更新した fixture 比較:
    dependency / hierarchy fixture の replace / merge round-trip で確認したかを明記する
  - `docs/remaining-migration-items.md` への反映要否:
    import mode 制約や external format の残件に影響する場合だけ反映する
```

次は、`project-workbook-json.ts` 周辺を確認するときの簡易サンプルである。

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
    workbook JSON の fixed format、editable field、warning / reject 契約、hierarchy fixture round-trip を確認する
  命名差分:
    upstream の workbook JSON helper 群と Java 側の export / import / validate 分割が追跡可能かを見る
  未移植差分:
    upstream 側で sheet 構成や editable field 契約が増えた場合に Java 側へ入っているかを見る
  Java 側独自拡張:
    Core API wrapper 経由の統一導線は upstream 本体とは分けて扱う

follow-up:
  - 追加/更新した test:
    まず ProjectWorkbookJsonTest で吸収し、wrapper 契約に影響する場合だけ CoreApiWorkbookTest まで広げる
  - 追加/更新した fixture 比較:
    hierarchy fixture の workbook JSON round-trip で確認したかを明記する
  - `docs/remaining-migration-items.md` への反映要否:
    workbook JSON schema や editable field 契約の残件に影響する場合だけ反映する
```

次は、`core-api-workbook.ts` 周辺を確認するときの簡易サンプルである。

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
    workbook JSON / xlsx wrapper、encode/decode、validate / patch、hierarchy fixture round-trip を確認する
  命名差分:
    upstream の workbook wrapper 名と Java 側の workbookJson / xlsx wrapper 分割が追跡可能かを見る
  未移植差分:
    upstream 側で workbook wrapper の mode や公開 API が増えた場合に Java 側へ入っているかを見る
  Java 側独自拡張:
    CLI 導線は upstream 本体の `core-api-workbook.ts` ではなく、Java CLI entrypoint 側の拡張として扱う

follow-up:
  - 追加/更新した test:
    まず CoreApiWorkbookTest で吸収し、必要なら ProjectWorkbookJsonTest / ProjectXlsxTest の fixture 比較まで広げる
  - 追加/更新した fixture 比較:
    hierarchy fixture の workbook JSON / xlsx round-trip で確認したかを明記する
  - `docs/remaining-migration-items.md` への反映要否:
    workbook wrapper 公開面や round-trip 契約の残件に影響する場合だけ反映する
```

次は、`project-xlsx.ts` 周辺を確認するときの簡易サンプルである。

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
    workbook sheet 変換、editable field import、`ProjectModel` 構築、hierarchy fixture round-trip を確認する
  命名差分:
    upstream の project xlsx helper 群と Java 側の export / import 分割が追跡可能かを見る
  未移植差分:
    upstream 側で workbook sheet 構成や import 契約が増えた場合に Java 側へ入っているかを見る
  Java 側独自拡張:
    Core API wrapper や encode/decode 導線は upstream 本体の `project-xlsx.ts` とは分けて扱う

follow-up:
  - 追加/更新した test:
    まず ProjectXlsxTest で吸収し、wrapper 契約に影響する場合だけ CoreApiWorkbookTest まで広げる
  - 追加/更新した fixture 比較:
    hierarchy fixture の workbook export / import round-trip で確認したかを明記する
  - `docs/remaining-migration-items.md` への反映要否:
    workbook sheet 構成や import 契約の残件に影響する場合だけ反映する
```

次は、`core-api-workbook-xlsx.ts` 周辺を確認するときの簡易サンプルである。

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
    xlsx wrapper の export / import、encode / decode、hierarchy fixture round-trip を確認する
  命名差分:
    upstream の workbook xlsx wrapper 名と Java 側の `CoreApiWorkbookXlsx` が追跡可能かを見る
  未移植差分:
    upstream 側で xlsx wrapper の公開 API や mode 契約が増えた場合に Java 側へ入っているかを見る
  Java 側独自拡張:
    CLI entrypoint や report 導線は upstream 本体の `core-api-workbook-xlsx.ts` とは分けて扱う

follow-up:
  - 追加/更新した test:
    まず CoreApiWorkbookTest で吸収し、必要なら ProjectXlsxTest の fixture 比較まで広げる
  - 追加/更新した fixture 比較:
    hierarchy fixture の xlsx export / import round-trip で確認したかを明記する
  - `docs/remaining-migration-items.md` への反映要否:
    xlsx wrapper 公開面や encode / decode 契約の残件に影響する場合だけ反映する
```

## 現在の主対応

### `vendor/mikuproject/src/ts/types.ts`

- Java 側 package:
  - `jp.igapyon.mikuproject.model`
- 主対応 class:
  - `ProjectModel`
  - `ProjectInfo`
  - `TaskModel`
  - `ResourceModel`
  - `AssignmentModel`
  - `CalendarModel`
  - `PredecessorModel`
  - `OutlineCodeModel`
  - `OutlineCodeMaskModel`
  - `OutlineCodeValueModel`
  - `WbsMaskModel`
  - `ProjectExtendedAttributeModel`
  - `TaskExtendedAttributeModel`
  - `ResourceExtendedAttributeModel`
  - `AssignmentExtendedAttributeModel`
  - `TaskBaselineModel`
  - `ResourceBaselineModel`
  - `AssignmentBaselineModel`
  - `TaskTimephasedDataModel`
  - `ResourceTimephasedDataModel`
  - `AssignmentTimephasedDataModel`
  - `WeekDayModel`
  - `WorkingTimeModel`
  - `CalendarExceptionModel`
  - `WorkWeekModel`
  - `ValidationIssue`

### `vendor/mikuproject/src/ts/msproject-xml.ts`

- Java 側 class:
  - `jp.igapyon.mikuproject.msprojectxml.MsProjectXml`
- 責務:
  - import/export の公開入口
  - `ProjectModel` normalize
  - validation 呼び出し
  - default calendar 補完の呼び出し

### `vendor/mikuproject/src/ts/msproject-codec.ts`

- Java 側 class:
  - `jp.igapyon.mikuproject.msprojectxml.MsProjectCodec`
- 責務:
  - `MS Project XML <-> ProjectModel` の codec
  - `Project / Calendars / Tasks / Resources / Assignments` の import/export

### `vendor/mikuproject/src/ts/msproject-xml-dom.ts`

- Java 側 class:
  - `jp.igapyon.mikuproject.msprojectxml.MsProjectXmlDom`
- 責務:
  - DOM parse helper
  - text / boolean / number parse helper
  - `WeekDays / WorkingTimes / OutlineCodes / ExtendedAttributes / Baselines / TimephasedData` の下位 parse helper

### `vendor/mikuproject/src/ts/msproject-calendar.ts`

- Java 側 class:
  - `jp.igapyon.mikuproject.msprojectxml.MsProjectCalendar`
- 責務:
  - default `Standard` calendar 補完
  - project date range に応じた祝日 exception 補完

### `vendor/mikuproject/src/ts/msproject-validate.ts`

- Java 側 class:
  - `jp.igapyon.mikuproject.msprojectxml.MsProjectValidate`
- 責務:
  - `ProjectModel` の validation
  - `Project / Calendars / Tasks / Resources / Assignments` の整合検査

### `vendor/mikuproject/src/ts/msproject-validate-helpers.ts`

- Java 側 class:
  - `jp.igapyon.mikuproject.msprojectxml.MsProjectValidateHelpers`
- 責務:
  - date parse helper
  - placeholder UID / unassigned resource UID helper
  - describe helper
  - task order helper

### `vendor/mikuproject/src/ts/msproject-samples.ts`

- Java 側 class:
  - `jp.igapyon.mikuproject.msprojectxml.MsProjectSamples`
- 責務:
  - upstream sample 相当の `ProjectModel` 生成
  - sample XML 生成
  - Java 側では sample 内容を `ProjectModel` へ直接組み立てる

### `vendor/mikuproject/src/ts/msproject-csv.ts`

- Java 側 class:
  - `jp.igapyon.mikuproject.msprojectxml.MsProjectCsv`
- 責務:
  - CSV + ParentID export
  - CSV + ParentID import
  - CSV parse / escape のスクラッチ実装
  - Java 側でも upstream 同様に `ProjectModel` を直接組み立てる

### `vendor/mikuproject/src/ts/msproject-mermaid.ts`

- Java 側 class:
  - `jp.igapyon.mikuproject.msprojectxml.MsProjectMermaid`
- 責務:
  - Mermaid gantt 出力
  - dependency comment / native dependency の出し分け
  - label / task id / duration の正規化

### `vendor/mikuproject/src/ts/msproject-ai-views.ts`

- Java 側 class:
  - `jp.igapyon.mikuproject.msprojectxml.MsProjectAiViews`
- 責務:
  - `project_draft_request` 生成
  - `project_draft_view` import
  - `project_overview_view` 出力
  - `phase_detail_view` 出力
  - `task_edit_view` 出力
  - Java 側の JSON view は `Map / List` ベースで表現する

### `vendor/mikuproject/src/ts/project-patch-json.ts`

- Java 側 class:
  - `jp.igapyon.mikuproject.projectpatchjson.ProjectPatchJson`
- 責務:
  - patch JSON 公開入口

### `vendor/mikuproject/src/ts/project-patch-json-core.ts`

- Java 側 class:
  - `jp.igapyon.mikuproject.projectpatchjson.ProjectPatchJsonCore`
- 責務:
  - patch document validate
  - patch operation dispatch
  - import result 組み立て

### `vendor/mikuproject/src/ts/project-patch-json-util.ts`

- Java 側 class:
  - `jp.igapyon.mikuproject.projectpatchjson.ProjectPatchJsonUtil`
- 責務:
  - dependency type / lag helper
  - date normalize helper
  - duration helper
  - clone helper

### `vendor/mikuproject/src/ts/project-patch-json-links.ts`

- Java 側 class:
  - `jp.igapyon.mikuproject.projectpatchjson.ProjectPatchJsonLinks`
- 責務:
  - `link_tasks`
  - `unlink_tasks`

### `vendor/mikuproject/src/ts/project-patch-json-entities.ts`

- Java 側 class:
  - `jp.igapyon.mikuproject.projectpatchjson.ProjectPatchJsonEntities`
- 責務:
  - `add_assignment`
  - `add_resource`
  - `add_calendar`
  - `delete_resource`
  - `delete_calendar`

### `vendor/mikuproject/src/ts/project-patch-json-updates.ts`

- Java 側 class:
  - `jp.igapyon.mikuproject.projectpatchjson.ProjectPatchJsonUpdates`
- 責務:
  - `update_project`
  - `update_assignment`
  - `update_resource`
  - `update_calendar`

### `vendor/mikuproject/src/ts/project-patch-json-tasks.ts`

- Java 側 class:
  - `jp.igapyon.mikuproject.projectpatchjson.ProjectPatchJsonTasks`
- 責務:
  - `add_task`
  - `move_task`
  - `delete_task`
  - task hierarchy rebuild
  - task parent / position helper

### `vendor/mikuproject/src/ts/project-workbook-schema.ts`

- Java 側 class:
  - `jp.igapyon.mikuproject.projectworkbookjson.ProjectWorkbookSchema`
- 責務:
  - workbook JSON sheet 名
  - project field order
  - sheet header 定義

### `vendor/mikuproject/src/ts/project-workbook-json-validate.ts`

- Java 側 class:
  - `jp.igapyon.mikuproject.projectworkbookjson.ProjectWorkbookJsonValidate`
- 責務:
  - workbook JSON document validate
  - unknown sheet / unknown column warning

### `vendor/mikuproject/src/ts/project-workbook-json-export.ts`

- Java 側 class:
  - `jp.igapyon.mikuproject.projectworkbookjson.ProjectWorkbookJsonExport`
- 責務:
  - `ProjectModel -> workbook JSON` export
  - fixed sheet order / row shape の組み立て

### `vendor/mikuproject/src/ts/project-workbook-json-import.ts`

- Java 側 class:
  - `jp.igapyon.mikuproject.projectworkbookjson.ProjectWorkbookJsonImport`
- 責務:
  - workbook JSON import
  - `baseModel` への merge import
  - workbook JSON からの `ProjectModel` 構築

### `vendor/mikuproject/src/ts/project-workbook-json.ts`

- Java 側 class:
  - `jp.igapyon.mikuproject.projectworkbookjson.ProjectWorkbookJson`
- 責務:
  - workbook JSON 公開入口

### `vendor/mikuproject/src/ts/project-xlsx.ts`

- Java 側 class:
  - `jp.igapyon.mikuproject.projectxlsx.ProjectXlsx`
- 責務:
  - workbook object 公開入口

### `vendor/mikuproject/src/ts/project-xlsx-export.ts`

- Java 側 class:
  - `jp.igapyon.mikuproject.projectxlsx.ProjectXlsxExport`
- 責務:
  - `ProjectModel -> XlsxWorkbookLike` export
  - fixed sheet order の組み立て

### `vendor/mikuproject/src/ts/project-xlsx-export-project.ts`

- Java 側 class:
  - `jp.igapyon.mikuproject.projectxlsx.ProjectXlsxExportProject`
- 責務:
  - Project sheet 構築

### `vendor/mikuproject/src/ts/project-xlsx-export-entities.ts`

- Java 側 class:
  - `jp.igapyon.mikuproject.projectxlsx.ProjectXlsxExportEntities`
- 責務:
  - Tasks / Resources / Assignments の tabular sheet 構築

### `vendor/mikuproject/src/ts/project-xlsx-export-calendars.ts`

- Java 側 class:
  - `jp.igapyon.mikuproject.projectxlsx.ProjectXlsxExportCalendars`
- 責務:
  - Calendars / NonWorkingDays sheet 構築

### `vendor/mikuproject/src/ts/project-xlsx-export-util.ts`

- Java 側 class:
  - `jp.igapyon.mikuproject.projectxlsx.ProjectXlsxExportUtil`
- 責務:
  - title / section / header / key-value / option row helper

### `vendor/mikuproject/src/ts/project-xlsx-import.ts`

- Java 側 class:
  - `jp.igapyon.mikuproject.projectxlsx.ProjectXlsxImport`
- 責務:
  - `XlsxWorkbookLike -> ProjectModel` import
  - detailed import changes
  - workbook JSON bridge

### `vendor/mikuproject/src/ts/project-xlsx-import-project.ts`

- Java 側 class:
  - `jp.igapyon.mikuproject.projectxlsx.ProjectXlsxImportProject`
- 責務:
  - Project sheet import

### `vendor/mikuproject/src/ts/project-xlsx-import-entities.ts`

- Java 側 class:
  - `jp.igapyon.mikuproject.projectxlsx.ProjectXlsxImportEntities`
- 責務:
  - Tasks / Resources / Assignments の tabular sheet import

### `vendor/mikuproject/src/ts/project-xlsx-import-calendars.ts`

- Java 側 class:
  - `jp.igapyon.mikuproject.projectxlsx.ProjectXlsxImportCalendars`
- 責務:
  - Calendars / NonWorkingDays sheet import

### `vendor/mikuproject/src/ts/project-xlsx-import-util.ts`

- Java 側 class:
  - `jp.igapyon.mikuproject.projectxlsx.ProjectXlsxImportUtil`
- 責務:
  - workbook document-like 変換
  - tabular read helper
  - known sheet 補完

### `vendor/mikuproject/src/ts/core-api-workbook-xlsx.ts`

- Java 側 class:
  - `jp.igapyon.mikuproject.coreapi.CoreApiWorkbookXlsx`
- 責務:
  - xlsx workbook API wrapper
  - `ProjectXlsx` への委譲
  - workbook object の encode/decode

### `vendor/mikuproject/src/ts/core-api-workbook.ts`

- Java 側 class:
  - `jp.igapyon.mikuproject.coreapi.CoreApiWorkbook`
- 責務:
  - workbook JSON API wrapper
  - xlsx API wrapper
  - patch JSON API wrapper

### `vendor/mikuproject/src/ts/ai-json-util.ts`

- Java 側 class:
  - `jp.igapyon.mikuproject.coreapi.CoreApiAiJsonUtil`
- 責務:
  - last fenced `json` block 抽出
  - AI JSON kind 判定
  - Java 側の最小 JSON parse

### `vendor/mikuproject/src/ts/ai-json-spec.ts`

- Java 側 class:
  - `jp.igapyon.mikuproject.coreapi.CoreApiAiJson`
  - `jp.igapyon.mikuproject.coreapi.CoreApiAiJsonSpec`
- 責務:
  - `mikuproject-ai-json-spec` の安定取得
  - version 抽出
  - Java 側では実行時に classpath / JAR 内リソースとして内包された `mikuproject-ai-json-spec.md` を参照
  - 元 Markdown は Maven の `process-resources` で `vendor/mikuproject/docs/mikuproject-ai-json-spec.md` から `target/classes/jp/igapyon/mikuproject/coreapi/` へコピーする

### `vendor/mikuproject/src/ts/core-api-msproject-ai.ts`

- Java 側 class:
  - `jp.igapyon.mikuproject.coreapi.CoreApiMsprojectAi`
- 責務:
  - ai view wrapper
  - mermaid wrapper

### `vendor/mikuproject/src/ts/core-api-msproject.ts`

- Java 側 class:
  - `jp.igapyon.mikuproject.coreapi.CoreApiMsproject`
- 責務:
  - sample XML / sample model wrapper
  - project model normalize / validate wrapper
  - MS Project XML / CSV wrapper
  - ai view / mermaid wrapper

### `vendor/mikuproject/src/ts/core-api-ai-json-import.ts`

- Java 側 class:
  - `jp.igapyon.mikuproject.coreapi.CoreApiAiJsonImport`
  - `jp.igapyon.mikuproject.coreapi.CoreApiImportResult`
- 責務:
  - AI JSON kind 判定
  - `project_draft_view / workbook_json / patch_json` import dispatch
  - mode / baseModel 制約の維持

### `vendor/mikuproject/src/ts/core-api-ai-json.ts`

- Java 側 class:
  - `jp.igapyon.mikuproject.coreapi.CoreApiAiJson`
  - `jp.igapyon.mikuproject.coreapi.CoreApiAiJsonParseResult`
- 責務:
  - AI JSON spec 取得
  - fenced text parse
  - unified AI JSON import wrapper

### `vendor/mikuproject/src/ts/core-api-external-binary.ts`

- Java 側 class:
  - `jp.igapyon.mikuproject.coreapi.CoreApiExternalBinary`
- 責務:
  - `ms_project_xml` replace import
  - `xlsx` replace / merge import

### `vendor/mikuproject/src/ts/core-api-external-document.ts`

- Java 側 class:
  - `jp.igapyon.mikuproject.coreapi.CoreApiExternalDocument`
- 責務:
  - `workbook_json / project_draft_view / patch_json` の document import dispatch

### `vendor/mikuproject/src/ts/core-api-external-import.ts`

- Java 側 class:
  - `jp.igapyon.mikuproject.coreapi.CoreApiExternalImport`
- 責務:
  - external import mode 制約
  - `source.format` ごとの dispatch

### `vendor/mikuproject/src/ts/core-api-import.ts`

- Java 側 class:
  - `jp.igapyon.mikuproject.coreapi.CoreApiImport`
- 責務:
  - AI JSON import と external import の unified entrypoint

### `vendor/mikuproject/src/ts/core-api-registry.ts`

- Java 側 class:
  - `jp.igapyon.mikuproject.coreapi.CoreApiRegistry`
- 責務:
  - import / msproject / workbook API の束ね直し
  - `report` を含む公開 API の束ね直し

### `vendor/mikuproject/src/ts/core-api-public.ts`

- Java 側 class:
  - `jp.igapyon.mikuproject.coreapi.CoreApiPublic`
- 責務:
  - `version = 1` を持つ公開 API 面
  - registry の薄い公開ラッパ

### `vendor/mikuproject/src/ts/core-api.ts`

- Java 側 class:
  - `jp.igapyon.mikuproject.coreapi.CoreApi`
- 責務:
  - Java 側の最終公開入口
  - `CoreApiPublic` の alias

### Java CLI entry

- Java 側 class:
  - `jp.igapyon.mikuproject.cli.MikuprojectCli`
- 責務:
  - Java 版の最小 command-line entrypoint
  - `MS Project XML` validate
  - Mermaid / WBS Markdown / daily SVG / weekly SVG export
  - monthly SVG archive zip / report bundle zip export
  - WBS xlsx export
  - workbook JSON export
  - project overview / phase detail / task edit AI view export
  - project draft request export
  - workbook JSON validate / import / merge
  - workbook xlsx export / import / merge
  - patch JSON validate / apply
  - AI JSON spec export / kind detect
  - AI JSON import
  - external import の最小 command-line 入口

### `vendor/mikuproject/src/ts/core-api-report.ts`

- Java 側 class:
  - `jp.igapyon.mikuproject.coreapi.CoreApiReport`
- 責務:
  - report entry export
  - Java 側では `wbs.md`, `mermaid.mmd`, `wbs.xlsx`, `daily.svg`, `weekly.svg`, `monthly-calendar/*` を生成
  - zip bundle 作成

### `vendor/mikuproject/src/ts/core-api-report-adapters.ts`

- Java 側 class:
  - `jp.igapyon.mikuproject.coreapi.CoreApiReportAdapters`
- 責務:
  - report 公開面の adapter
  - `all / wbsMarkdown / mermaid / svg / wbsXlsx` を実動

### `vendor/mikuproject/src/ts/core-api-report-public.ts`

- Java 側 class:
  - `jp.igapyon.mikuproject.coreapi.CoreApiReportPublic`
- 責務:
  - report 公開面の薄い wrapper

### `vendor/mikuproject/src/ts/excel-io.ts`

- Java 側 class:
  - `jp.igapyon.mikuproject.excelio.XlsxWorkbookCodec`
  - `jp.igapyon.mikuproject.excelio.ExcelIoUtil`
- 責務:
  - workbook byte codec
  - UTF-8 helper
  - workbook archive export
  - package entry list / unpack
  - archive import の公開入口
  - Java 側では workbook object の round-trip byte codec と OOXML-like zip package import/export を提供

### `vendor/mikuproject/src/ts/excel-io-package-xml.ts`

- Java 側 class:
  - `jp.igapyon.mikuproject.excelio.ExcelIoPackageXml`
- 責務:
  - `[Content_Types].xml` 生成
  - root/workbook relationships XML 生成
  - workbook XML 生成

### `vendor/mikuproject/src/ts/excel-io-zip.ts`

- Java 側 class:
  - `jp.igapyon.mikuproject.excelio.ExcelIoZip`
- 責務:
  - zip pack
  - zip unpack
  - entry list helper

### `vendor/mikuproject/src/ts/excel-io-normalize.ts`

- Java 側 class:
  - `jp.igapyon.mikuproject.excelio.ExcelIoNormalize`
- 責務:
  - workbook shape normalize
  - sheet name / merged range / sqref validate
  - color normalize / denormalize

### `vendor/mikuproject/src/ts/excel-io-workbook-build.ts`

- Java 側 class:
  - `jp.igapyon.mikuproject.excelio.ExcelIoWorkbookBuild`
- 責務:
  - workbook package entry 組み立て
  - workbook / rels / styles / worksheet entry の束ね

### `vendor/mikuproject/src/ts/excel-io-workbook-parse.ts`

- Java 側 class:
  - `jp.igapyon.mikuproject.excelio.ExcelIoWorkbookParse`
- 責務:
  - workbook.xml / workbook rels parse
  - worksheet target 解決
  - archive からの workbook 復元

### `vendor/mikuproject/src/ts/excel-io-worksheet-build.ts`

- Java 側 class:
  - `jp.igapyon.mikuproject.excelio.ExcelIoWorksheetBuild`
- 責務:
  - worksheet XML build
  - columns / rows / cells / mergedRanges の出力

### `vendor/mikuproject/src/ts/excel-io-worksheet-parse.ts`

- Java 側 class:
  - `jp.igapyon.mikuproject.excelio.ExcelIoWorksheetParse`
- 責務:
  - worksheet XML parse
  - columns / rows / cells / mergedRanges の復元

### `vendor/mikuproject/src/ts/excel-io-styles-build.ts`

- Java 側 class:
  - `jp.igapyon.mikuproject.excelio.ExcelIoStylesBuild`
- 責務:
  - style book 生成
  - style index 解決
  - styles.xml build

### `vendor/mikuproject/src/ts/excel-io-styles-parse.ts`

- Java 側 class:
  - `jp.igapyon.mikuproject.excelio.ExcelIoStylesParse`
- 責務:
  - styles.xml parse
  - alignment / wrapText などの最小 style 復元

### `vendor/mikuproject/src/ts/markdown-escape.ts`

- Java 側 class:
  - `jp.igapyon.mikuproject.markdownescape.MarkdownEscape`
- 責務:
  - Markdown literal escape
  - Markdown table cell escape

### `vendor/mikuproject/src/ts/wbs-dateband.ts`

- Java 側 class:
  - `jp.igapyon.mikuproject.wbsdateband.WbsDateband`
- 責務:
  - holiday / non-working day helper
  - date band / display date band helper
  - business day count helper

### `vendor/mikuproject/src/ts/wbs-markdown.ts`

- Java 側 class:
  - `jp.igapyon.mikuproject.wbsmarkdown.WbsMarkdown`
- 責務:
  - WBS markdown export
  - tree / table / summary の 1 文書出力
  - `MsProjectXml.exportWbsMarkdown` からの利用

### `vendor/mikuproject/src/ts/wbs-svg.ts`

- Java 側 class:
  - `jp.igapyon.mikuproject.wbssvg.WbsSvg`
- 責務:
  - daily SVG export
  - weekly SVG export
  - monthly calendar SVG archive export
  - `MsProjectXml` からの利用

### `vendor/mikuproject/src/ts/wbs-svg-render.ts`

- Java 側 class:
  - `jp.igapyon.mikuproject.wbssvg.WbsSvgRender`
- 責務:
  - daily / weekly SVG 描画
  - timeline 基準日の解決
  - exportable task 抽出
  - dependency path 描画
  - `WbsSvgScaffold` / `WbsSvgTimeline` / `WbsSvgViewport` を束ねる render

### `vendor/mikuproject/src/ts/wbs-svg-scaffold.ts`

- Java 側 class:
  - `jp.igapyon.mikuproject.wbssvg.WbsSvgScaffold`
- 責務:
  - SVG ルート、defs、title、weekly subtitle などの scaffold 出力

### `vendor/mikuproject/src/ts/wbs-svg-timeline.ts`

- Java 側 class:
  - `jp.igapyon.mikuproject.wbssvg.WbsSvgTimeline`
- 責務:
  - daily / weekly の task placement 計算

### `vendor/mikuproject/src/ts/wbs-svg-viewport.ts`

- Java 側 class:
  - `jp.igapyon.mikuproject.wbssvg.WbsSvgViewport`
- 責務:
  - daily / weekly の x 位置と幅の計算

### `vendor/mikuproject/src/ts/wbs-svg-bars.ts`

- Java 側 class:
  - `jp.igapyon.mikuproject.wbssvg.WbsSvgBars`
- 責務:
  - daily / weekly の task bar 描画

### `vendor/mikuproject/src/ts/wbs-svg-labels.ts`

- Java 側 class:
  - `jp.igapyon.mikuproject.wbssvg.WbsSvgLabels`
- 責務:
  - task label 描画

### `vendor/mikuproject/src/ts/wbs-svg-calendar.ts`

- Java 側 class:
  - `jp.igapyon.mikuproject.wbssvg.WbsSvgCalendar`
- 責務:
  - monthly calendar SVG archive のエントリ構築

### `vendor/mikuproject/src/ts/wbs-svg-axis.ts`

- Java 側 class:
  - `jp.igapyon.mikuproject.wbssvg.WbsSvgAxis`
- 責務:
  - daily / weekly SVG の軸補助描画

### `vendor/mikuproject/src/ts/wbs-svg-public.ts`

- Java 側 class:
  - `jp.igapyon.mikuproject.wbssvg.WbsSvgPublic`
- 責務:
  - WBS SVG export の public API
  - `WbsSvgRender` / `WbsSvgCalendar` への facade

### `vendor/mikuproject/src/ts/wbs-svg-zip.ts`

- Java 側 class:
  - `jp.igapyon.mikuproject.wbssvg.WbsSvgZip`
- 責務:
  - monthly calendar SVG archive の zip 化

### `vendor/mikuproject/src/ts/wbs-xlsx-layout.ts`

- Java 側 class:
  - `jp.igapyon.mikuproject.wbsxlsx.WbsXlsxLayout`
- 責務:
  - Excel-style column / cell reference helper
  - range / parse / describe / log helper

### `vendor/mikuproject/src/ts/wbs-xlsx.ts`

- Java 側 class:
  - `jp.igapyon.mikuproject.wbsxlsx.WbsXlsx`
- 責務:
  - WBS workbook export の公開入口
  - `WbsXlsxPublic` / `WbsXlsxExport` への facade

### `vendor/mikuproject/src/ts/wbs-xlsx-public.ts`

- Java 側 class:
  - `jp.igapyon.mikuproject.wbsxlsx.WbsXlsxPublic`
- 責務:
  - WBS workbook export の public API
  - holiday date collect
  - layout helper の公開

### `vendor/mikuproject/src/ts/wbs-xlsx-export.ts`

- Java 側 class:
  - `jp.igapyon.mikuproject.wbsxlsx.WbsXlsxExport`
- 責務:
  - project info / date band / task row / legend / summary を含む workbook 生成
  - `projectxlsx.XlsxWorkbookLike` を使って report / excel-io と接続する

### `vendor/mikuproject/src/ts/wbs-xlsx-base.ts`

- Java 側 class:
  - `jp.igapyon.mikuproject.wbsxlsx.WbsXlsxBase`
- 責務:
  - WBS xlsx export 共通の基礎 helper
  - fixed column 数
  - date / weekday / duration / predecessors / calendar label / timestamp 変換

### `vendor/mikuproject/src/ts/wbs-xlsx-cells.ts`

- Java 側 class:
  - `jp.igapyon.mikuproject.wbsxlsx.WbsXlsxCells`
- 責務:
  - WBS xlsx export 用 cell / row 組み立て
  - title / project info / summary / legend row
  - date band / task / progress marker cell

### `vendor/mikuproject/src/ts/wbs-xlsx-sections.ts`

- Java 側 class:
  - `jp.igapyon.mikuproject.wbsxlsx.WbsXlsxSections`
- 責務:
  - project info / date band / task / legend / summary の section 単位組み立て

### `vendor/mikuproject/src/ts/wbs-xlsx-taskmeta.ts`

- Java 側 class:
  - `jp.igapyon.mikuproject.wbsxlsx.WbsXlsxTaskmeta`
- 責務:
  - task row 生成用の calendar / resource / assignment 補助情報収集

## testdata 対応

### `vendor/mikuproject/testdata/minimal.xml`

- Java 側 test:
  - `MsProjectXmlTest.roundTripsUpstreamMinimalXmlFixture`

### `vendor/mikuproject/testdata/hierarchy.xml`

- Java 側 test:
  - `MsProjectXmlTest.importsUpstreamHierarchyXmlFixture`

### `vendor/mikuproject/testdata/dependency.xml`

- Java 側 test:
  - `MsProjectXmlTest.importsUpstreamDependencyXmlFixture`

## 現在の保留領域

AI / patch / workbook / CSV / report の主要導線は、Java 側にも対応 class と test がある。
そのため、現時点でこの文書が扱う残件は、新しい大分類を実装することではなく、upstream 更新時に既存対応表、test 対応、実記録を崩さず追える状態を保つことである。

直接対応 class の追加を急がない領域は、Java CLI runtime の対象外である Web UI / browser main 系、または Java 側独自の配布 / automation 都合に限る。
これらを扱う場合も、新規機能追加ではなく、`docs/remaining-migration-items.md` に保留理由と確認単位を明記してから判断する。

## 補足

- Java 側では、upstream 1 file に対して 1 class を基本線とする
- ただし `types.ts` のように upstream 側が型定義集合である場合は、Java 側では複数 POJO class へ分割する
- その場合でも package と class 名から元の upstream file を推測できる状態を保つ
