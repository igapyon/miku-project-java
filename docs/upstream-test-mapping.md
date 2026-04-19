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

- `ProjectPatchJsonTasksTest.importsAddTaskMoveTaskAndDeleteTask`
  - upstream の `add_task` / `move_task` / `delete_task` 基本動作に対応

- `ProjectPatchJsonTasksTest.rejectsBrokenAddTaskAndNormalizesMilestoneAddTask`
  - upstream の invalid add_task / milestone normalize / unsupported key warning に対応

- `ProjectPatchJsonTasksTest.rejectsDeleteTaskWhenReferencesRemain`
  - upstream の summary / assignment / successor 参照付き delete_task 拒否に対応

- `ProjectPatchJsonTasksTest.ignoresNoOpMoveTask`
  - upstream の no-op move_task warning に対応

## Java 側 Workbook JSON test

- `ProjectWorkbookJsonTest.exportsWorkbookJsonWithFixedFormatAndSheets`
  - upstream の fixed format / fixed sheet export に対応

- `ProjectWorkbookJsonTest.importsLimitedEditableFieldsThroughWorkbookJson`
  - upstream の editable field 限定 import に対応

- `ProjectWorkbookJsonTest.rejectsInvalidWorkbookJsonFormat`
  - upstream の invalid format reject に対応

- `ProjectWorkbookJsonTest.reportsWarningsForUnknownSheetAndUnknownColumns`
  - upstream の unknown sheet / unknown column warning に対応

- `ProjectWorkbookJsonTest.rejectsNonArraySheetsAndNonObjectRows`
  - upstream の invalid sheet shape reject に対応

- `ProjectWorkbookJsonTest.keepsNonEditableTaskColumnsUnchangedThroughWorkbookJsonImport`
  - upstream の non-editable task column 保持に対応

## Java 側 Project XLSX test

- `ProjectXlsxTest.convertsProjectModelIntoWorkbookSheets`
  - upstream の workbook sheet 変換の first cut に対応

- `ProjectXlsxTest.importsLimitedEditableFieldsThroughWorkbook`
  - upstream の workbook import editable field 反映に対応

- `ProjectXlsxTest.importsWorkbookAsProjectModel`
  - upstream の workbook からの `ProjectModel` 構築に対応

## Java 側 Core API Workbook test

- `CoreApiWorkbookTest.importsWorkbookJsonWithAndWithoutBaseModel`
  - upstream の workbook json import wrapper に対応

- `CoreApiWorkbookTest.validatesWorkbookJsonAndAppliesPatchJson`
  - upstream の workbook validate / patch wrapper に対応

- `CoreApiWorkbookTest.exposesProjectXlsxThroughUnifiedEntryPoint`
  - upstream の xlsx workbook wrapper に対応

- `CoreApiWorkbookTest.encodesAndDecodesWorkbookThroughUnifiedEntryPoint`
  - Java first cut の workbook byte codec 経由 encode/decode に対応

## Java 側 Core API Import test

- `CoreApiImportTest.parsesFencedAiJsonTextAndDetectsKind`
  - upstream の fenced AI JSON parse と kind 判定に対応

- `CoreApiImportTest.importsProjectDraftViewWithoutUiDependencies`
  - upstream の `project_draft_view` import に対応

- `CoreApiImportTest.importsWorkbookJsonWithAndWithoutBaseModel`
  - upstream の workbook json replace / merge import に対応

- `CoreApiImportTest.importsAiJsonTextAndExposesAiJsonSpec`
  - upstream の `importAiJsonText` と `getAiJsonSpec*` に対応

- `CoreApiImportTest.importsExternalFormatsThroughImportExternal`
  - upstream の `importExternal` による xml / xlsx / workbook_json / patch_json import に対応

- `CoreApiImportTest.appliesPatchJsonThroughUnifiedEntryPoint`
  - upstream の patch JSON unified import に対応

- `CoreApiImportTest.rejectsPatchJsonWhenBaseModelIsMissing`
  - upstream の patch import `baseModel` 必須制約に対応

- `CoreApiImportTest.rejectsUnsupportedFormatAndModeCombinationsInImportExternal`
  - upstream の `importExternal` mode 制約に対応

- `CoreApiImportTest.rejectsMergeImportsWhenBaseModelIsMissing`
  - upstream の merge / patch import `baseModel` 必須制約に対応

## Java 側 Core API Public test

- `CoreApiPublicTest.exposesUnifiedPublicApiSurface`
  - upstream の `core-api-public` / `core-api-registry` / `core-api` の公開面に対応

- `CoreApiPublicTest.exposesWorkingReportApiSurface`
  - Java first cut では `wbsMarkdown / mermaid / svg / all / wbsXlsx` を実動
  - report bundle に `wbs.xlsx` が含まれることを確認

## Java 側 Excel IO test

- `ExcelIoTest.roundTripsWorkbookBytes`
  - Java first cut の workbook byte codec round-trip に対応

- `ExcelIoTest.rejectsInvalidWorkbookBytes`
  - invalid byte payload reject に対応

- `ExcelIoTest.exportsWorkbookAsOoxmlLikeZipPackage`
  - OOXML-like zip package export と entry list / unpack の first cut に対応

- `ExcelIoTest.importsWorkbookFromOoxmlLikeZipPackage`
  - OOXML-like zip package からの workbook import に対応

- `ExcelIoTest.normalizesWorkbookShapeBeforeExportAndValidatesNamesAndRanges`
  - workbook normalize と sheet name / range / color validate に対応

## Java 側 WBS Markdown test

- `WbsMarkdownTest.exportsOneMarkdownDocumentWithTreeFirstAndTableAfterIt`
  - upstream の tree first / table second / summary last 構成に対応

- `WbsMarkdownTest.showsNotesInTheTreeSectionAndSummaryAfterTheTable`
  - upstream の notes 表示と summary option 表示に対応

- `WbsMarkdownTest.escapesMarkdownSensitiveTextInTableCells`
  - upstream の markdown-sensitive text escape に対応

- `WbsMarkdownTest.usesFenceThatDoesNotBreakWhenTreeTextIncludesBackticks`
  - upstream の fence 長さ調整に対応

- `WbsMarkdownTest.keepsDeepHierarchyReadable`
  - upstream の deep hierarchy 可読性確認に対応

## Java 側 WBS SVG test

- `WbsSvgTest.exportsDailyAndWeeklySvg`
  - upstream の daily / weekly preview SVG export に対応

- `WbsSvgTest.rendersDependencyConnectorsInDailyAndWeeklySvg`
  - upstream の dependency connector 描画に対応

- `WbsSvgTest.exportsMonthlyCalendarArchive`
  - upstream の monthly calendar SVG archive export に対応

## Java 側 WBS XLSX test

- `WbsXlsxTest.providesExcelStyleLayoutReferencesForWbsWorksheetTuning`
  - upstream の layout helper 基本操作に対応

- `WbsXlsxTest.canLogWbsLayoutCellReferencesOnDemand`
  - upstream の layout log helper に対応

- `WbsXlsxTest.exportsDedicatedWbsWorkbookAndCanEncodeIt`
  - upstream の WBS workbook export と xlsx encode 接続の first cut に対応
