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

- `project-workbook-json*.ts`
- `project-xlsx*.ts`
- `wbs-svg*.ts`
- `wbs-markdown.ts`

## 補足

- Java 側では、upstream 1 file に対して 1 class を基本線とする
- ただし `types.ts` のように upstream 側が型定義集合である場合は、Java 側では複数 POJO class へ分割する
- その場合でも package と class 名から元の upstream file を推測できる状態を保つ
