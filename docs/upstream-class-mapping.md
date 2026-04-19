# Upstream Class Mapping

## 目的

この文書は、Node.js upstream の主要ファイルと Java 側 class / package の対応関係を固定するためのメモである。

移植作業では、Java 側の作りやすさよりも upstream 追随時の見通しを重視する。
そのため、どの Java class がどの upstream file を受け持つかを、先に明示しておく。

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
  - Java 側では `project_draft_view` import 層が未実装のため、sample 内容を `ProjectModel` へ直接組み立てる

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
  - `encode/decode` は `excel-io*` 未移植のため現時点では未対応

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
  - Java first cut の最小 JSON parse

### `vendor/mikuproject/src/ts/ai-json-spec.ts`

- Java 側 class:
  - `jp.igapyon.mikuproject.coreapi.CoreApiAiJson`
  - `jp.igapyon.mikuproject.coreapi.CoreApiAiJsonSpec`
- 責務:
  - `mikuproject-ai-json-spec` の安定取得
  - version 抽出
  - Java 側では `vendor/mikuproject/docs/mikuproject-ai-json-spec.md` を参照

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
  - Java 側では `report` を placeholder 付きで公開

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

### `vendor/mikuproject/src/ts/core-api-report.ts`

- Java 側 class:
  - `jp.igapyon.mikuproject.coreapi.CoreApiReport`
- 責務:
  - report entry export
  - Java first cut では `wbs.md`, `mermaid.mmd`, `wbs.xlsx`, `daily.svg`, `weekly.svg`, `monthly-calendar/*` を生成
  - zip bundle 作成

### `vendor/mikuproject/src/ts/core-api-report-adapters.ts`

- Java 側 class:
  - `jp.igapyon.mikuproject.coreapi.CoreApiReportAdapters`
- 責務:
  - report 公開面の adapter
  - first cut では `all / wbsMarkdown / mermaid / svg / wbsXlsx` を実動

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
  - Java first cut では workbook object の round-trip byte codec と OOXML-like zip package import/export を提供

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
  - project info / date band / task row / legend / summary を含む first cut workbook 生成
  - `projectxlsx.XlsxWorkbookLike` を使って report / excel-io と接続する

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

## 現在未着手または保留

以下は upstream には存在するが、Java STEP1 ではまだ直接対応 class を置いていない領域である。

### AI / patch / workbook / CSV / report

## 補足

- Java 側では、upstream 1 file に対して 1 class を基本線とする
- ただし `types.ts` のように upstream 側が型定義集合である場合は、Java 側では複数 POJO class へ分割する
- その場合でも package と class 名から元の upstream file を推測できる状態を保つ
