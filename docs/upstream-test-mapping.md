# Upstream Test Mapping

## 方針

Java 版のテストは、Node.js upstream のテスト意図と fixture を追えることを重視する。
この文書は、upstream 側テストと Java 側テストの対応関係を固定するためのメモである。

## 対応表

### `vendor/mikuproject/tests/mikuproject-msproject-xml-roundtrip.test.js`

- `limits default calendar holiday exceptions to the project date range`
  - Java 側対応:
    - `MsProjectXmlTest.ensureDefaultProjectCalendarBuildsJapaneseHolidayExceptions`
  - 補足:
    - Java 側では `MsProjectCalendar.ensureDefaultProjectCalendar` を直接確認する
    - 祝日 exception が project date range 内に限定されることを見る

- `round-trips the minimal xml sample`
  - Java 側対応:
    - `MsProjectXmlTest.roundTripsUpstreamMinimalXmlFixture`
  - fixture:
    - `vendor/mikuproject/testdata/minimal.xml`

- `round-trips hierarchy through CSV + ParentID export and import`
  - Java 側対応:
    - `MsProjectCsvTest.roundTripsHierarchyThroughCsvParentId`

- `exports project overview and default phase detail views from hierarchy`
  - Java 側対応:
    - `MsProjectAiViewsTest.exportsProjectOverviewAndDefaultPhaseDetailViewsFromHierarchy`

- `exports task_edit_view with predecessors, successors, and assignments`
  - Java 側対応:
    - `MsProjectAiViewsTest.exportsTaskEditViewWithPredecessorsSuccessorsAndAssignments`

- `exports scoped phase detail with root_uid and max_depth`
  - Java 側対応:
    - `MsProjectAiViewsTest.exportsScopedPhaseDetailAndRejectsInvalidRootUid`

- `rejects scoped phase detail when root_uid is outside the phase`
  - Java 側対応:
    - `MsProjectAiViewsTest.exportsScopedPhaseDetailAndRejectsInvalidRootUid`

- `builds project draft request and imports predecessor mapping from project_draft_view`
  - Java 側対応:
    - `MsProjectAiViewsTest.buildsProjectDraftRequestAndImportsPredecessorMappingFromProjectDraftView`

## fixture 対応

- `vendor/mikuproject/testdata/minimal.xml`
  - Java 側:
    - `MsProjectXmlTest.roundTripsUpstreamMinimalXmlFixture`

- `vendor/mikuproject/testdata/hierarchy.xml`
  - Java 側:
    - `MsProjectXmlTest.importsUpstreamHierarchyXmlFixture`

- `vendor/mikuproject/testdata/dependency.xml`
  - Java 側:
    - `MsProjectXmlTest.importsUpstreamDependencyXmlFixture`

## Java 側テスト命名

Java 側テスト名は、次の基準で付ける。

- upstream に既存テスト名がある場合は、その意図を読める英語名を優先する
- fixture 名がある場合は、`Upstream` と fixture 名を含めて対応元を推測しやすくする
- Java 側固有の補助検証は、責務名をそのままメソッド名に出す

例:

- `roundTripsUpstreamMinimalXmlFixture`
- `importsUpstreamHierarchyXmlFixture`
- `ensureDefaultProjectCalendarBuildsJapaneseHolidayExceptions`

## Java 側固有の sample test

- `MsProjectSamplesTest.buildSampleProjectModelCreatesExpectedSample`
  - `msproject-samples.ts` の sample 内容に対応する Java 側確認

- `MsProjectSamplesTest.buildSampleXmlRoundTripsThroughMsProjectXml`
  - `msproject-samples.ts` の sample XML 生成に対応する Java 側確認

## Java 側 CSV test

- `MsProjectCsvTest.roundTripsHierarchyThroughCsvParentId`
  - upstream の CSV + ParentID round-trip test に対応

- `MsProjectCsvTest.rejectsDuplicateIdInCsvImport`
  - upstream の duplicate ID 異常系に対応

- `MsProjectCsvTest.rejectsMissingParentIdInCsvImport`
  - upstream の missing parent 異常系に対応

- `MsProjectCsvTest.rejectsCyclicParentIdInCsvImport`
  - upstream の cyclic parent 異常系に対応

## Java 側 Mermaid test

- `MsProjectMermaidTest.exportsMermaidFromSampleProject`
  - sample project からの Mermaid gantt 出力確認

- `MsProjectMermaidTest.keepsComplexMermaidDependenciesAsComments`
  - upstream の complex dependency comment 出力に対応

- `MsProjectMermaidTest.sanitizesDateLeadingMermaidGanttLabels`
  - upstream の date-leading label sanitize に対応

## Java 側 AI view test

- `MsProjectAiViewsTest.exportsProjectOverviewAndDefaultPhaseDetailViewsFromHierarchy`
  - upstream の overview / default phase detail に対応

- `MsProjectAiViewsTest.exportsTaskEditViewWithPredecessorsSuccessorsAndAssignments`
  - upstream の task_edit_view 出力に対応

- `MsProjectAiViewsTest.exportsScopedPhaseDetailAndRejectsInvalidRootUid`
  - upstream の scoped phase detail と invalid root_uid 異常系に対応

- `MsProjectAiViewsTest.buildsProjectDraftRequestAndImportsPredecessorMappingFromProjectDraftView`
  - upstream の project_draft_request / project_draft_view import に対応

- `MsProjectAiViewsTest.rejectsInvalidProjectDraftViewReferences`
  - upstream の invalid project_draft_view reference 異常系に対応

## Java 側 Patch test

- `ProjectPatchJsonTest.validatesPatchDocuments`
  - patch document validate の first cut 確認

- `ProjectPatchJsonTest.rejectsInvalidPatchJsonOperationsWithoutMainUi`
  - upstream の invalid patch warning を first cut で確認

- `ProjectPatchJsonTest.reportsPatchJsonWarningDetailsWithoutMainUi`
  - upstream の link/unlink と resource/calendar warning を first cut で確認
