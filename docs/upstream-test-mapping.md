# Upstream Test Mapping

## 方針

Java 版のテストは、Node.js upstream のテスト意図と fixture を追えることを重視する。
この文書は、upstream 側テストと Java 側テストの対応関係を固定するためのメモである。
repo top から入るときの入口は `README.md` の `Development Docs` 節とする。

## 追随時メモ

upstream 更新追随で差分を見たときは、まず変更された upstream file に対応する Java class と test をこの文書と `docs/upstream-class-mapping.md` で引く。

部分確認の目安:

- report unit 回帰:
  - `mvn test -Dtest=WbsMarkdownTest,WbsSvgTest,WbsXlsxTest`
- report API / CLI を含むまとまった回帰:
  - `mvn test -Dtest=WbsMarkdownTest,WbsSvgTest,WbsXlsxTest,CoreApiPublicTest,MikuprojectCliTest`
- CLI entrypoint 回帰:
  - `mvn test -Dtest=MikuprojectCliTest`
- import / AI view 回帰:
  - `mvn test -Dtest=CoreApiImportTest,MsProjectAiViewsTest`
- workbook 回帰:
  - `mvn test -Dtest=ProjectWorkbookJsonTest,ProjectXlsxTest,CoreApiWorkbookTest`
- upstream 追随の保守回帰:
  - `mvn test -Dtest=WbsMarkdownTest,WbsSvgTest,WbsXlsxTest,ProjectWorkbookJsonTest,ProjectXlsxTest,CoreApiWorkbookTest,CoreApiPublicTest,CoreApiImportTest,MsProjectAiViewsTest,MikuprojectCliTest`

確認結果は、少なくとも次の 4 区分でメモすると追跡しやすい。

- `挙動差分`
- `命名差分`
- `未移植差分`
- `Java 側独自拡張`

実際の記録は `docs/upstream-followup-log.md` に、`upstream file` 単位で残す。

補足:

- `docs only` 更新では、原則として追加テストを回さない
- コード変更を含む場合、または新しい focused 回帰コマンド自体をこの文書へ追加する場合だけ、対象単位を実行して確認する
- この種の追随運用文書の整備は、`docs-only` のコミットとしてまとめてよい

最短フロー:

1. `docs/remaining-migration-items.md` で現在地と対象範囲を確認する
2. `docs/upstream-class-mapping.md` で対象 file に対応する Java class を引く
3. この文書で対応 test と focused 回帰単位を引く
4. `docs/development.md` で実行する focused test command を確認する
5. 必要なら対象単位だけ test を実行し、`docs/upstream-followup-log.md` と `docs/remaining-migration-items.md` に結果を反映する

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
    - `MsProjectXmlTest.roundTripsUpstreamHierarchyXmlFixture`

- `vendor/mikuproject/testdata/dependency.xml`
  - Java 側:
    - `MsProjectXmlTest.importsUpstreamDependencyXmlFixture`
    - `MsProjectXmlTest.roundTripsUpstreamDependencyXmlFixture`

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
  - patch document validate の主要導線確認

- `ProjectPatchJsonTest.rejectsInvalidPatchJsonOperationsWithoutMainUi`
  - upstream の invalid patch warning を確認

- `ProjectPatchJsonTest.reportsPatchJsonWarningDetailsWithoutMainUi`
  - upstream の link/unlink と resource/calendar warning を確認

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

- `ProjectWorkbookJsonTest.roundTripsHierarchyFixtureThroughWorkbookJson`
  - hierarchy fixture を workbook JSON export / import で往復できることを確認

## Java 側 Project XLSX test

- `ProjectXlsxTest.convertsProjectModelIntoWorkbookSheets`
  - upstream の workbook sheet 変換の主要導線に対応

- `ProjectXlsxTest.importsLimitedEditableFieldsThroughWorkbook`
  - upstream の workbook import editable field 反映に対応

- `ProjectXlsxTest.importsWorkbookAsProjectModel`
  - upstream の workbook からの `ProjectModel` 構築に対応

- `ProjectXlsxTest.roundTripsHierarchyFixtureThroughWorkbook`
  - hierarchy fixture を workbook export / import で往復できることを確認

## Java 側 Core API Workbook test

- `CoreApiWorkbookTest.importsWorkbookJsonWithAndWithoutBaseModel`
  - upstream の workbook json import wrapper に対応

- `CoreApiWorkbookTest.validatesWorkbookJsonAndAppliesPatchJson`
  - upstream の workbook validate / patch wrapper に対応

- `CoreApiWorkbookTest.exposesProjectXlsxThroughUnifiedEntryPoint`
  - upstream の xlsx workbook wrapper に対応

- `CoreApiWorkbookTest.encodesAndDecodesWorkbookThroughUnifiedEntryPoint`
  - Java workbook byte codec 経由 encode/decode に対応

- `CoreApiWorkbookTest.roundTripsHierarchyFixtureThroughWorkbookWrappers`
  - hierarchy fixture を workbook JSON / xlsx wrapper 経由で往復できることを確認

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

- `CoreApiImportTest.roundTripsHierarchyFixtureThroughUnifiedImportWrappers`
  - hierarchy fixture を unified import wrapper の workbook_json / xlsx replace import で往復できることを確認

## Java 側 Core API Public test

- `CoreApiPublicTest.exposesUnifiedPublicApiSurface`
  - upstream の `core-api-public` / `core-api-registry` / `core-api` の公開面に対応

- `CoreApiPublicTest.exposesWorkingReportApiSurface`
  - Java 側では `wbsMarkdown / mermaid / svg / all / wbsXlsx` を実動
  - report bundle に `wbs.xlsx` が含まれることを確認
  - report bundle の entry 名と主要出力内容を確認

- `CoreApiPublicTest.exposesWorkingReportApiSurfaceForDependencyFixture`
  - dependency fixture でも report bundle の entry 名と主要出力内容を確認

- `CoreApiPublicTest.exposesWorkingReportApiSurfaceForHierarchyFixture`
  - hierarchy fixture でも report bundle の entry 名、主要出力内容、bundle 内 `wbs.xlsx` の decode 結果を確認

## Java 側 CLI test

- `MikuprojectCliTest.printsUsageForHelp`
  - Java CLI の help 表示に対応

- `MikuprojectCliTest.printsUsageForHelpAliases`
  - Java CLI の `-h` / `--help` alias 表示に対応

- `MikuprojectCliTest.keepsReadmeCliCommandListInSyncWithHelpOutput`
  - Java CLI help と `README.md` の command 一覧同期を固定

- `MikuprojectCliTest.validatesXmlAndExportsFormats`
  - Java CLI から validate / Mermaid / Markdown / SVG export、zip 出力、report directory 出力を呼べることを確認

- `MikuprojectCliTest.validatesXmlBatchAndExportsReportDirBatch`
  - Java CLI から validate batch と report bundle / report directory batch export を呼べることを確認

- `MikuprojectCliTest.exportsWorkbookJsonBatch`
  - Java CLI から workbook JSON batch export を呼べることを確認

- `MikuprojectCliTest.exportsXlsxBatch`
  - Java CLI から xlsx batch export を呼べることを確認

- `MikuprojectCliTest.exportsMermaidBatch`
  - Java CLI から Mermaid batch export を呼べることを確認

- `MikuprojectCliTest.exportsWbsMarkdownBatch`
  - Java CLI から WBS markdown batch export を呼べることを確認

- `MikuprojectCliTest.exportsDailySvgBatch`
  - Java CLI から daily SVG batch export を呼べることを確認

- `MikuprojectCliTest.exportsWeeklySvgBatch`
  - Java CLI から weekly SVG batch export を呼べることを確認

- `MikuprojectCliTest.exportsMonthlySvgZipBatch`
  - Java CLI から monthly SVG zip batch export を呼べることを確認

- `MikuprojectCliTest.exportsWbsXlsxBatch`
  - Java CLI から WBS xlsx batch export を呼べることを確認

- `MikuprojectCliTest.exportsProjectOverviewViewBatch`
  - Java CLI から project overview view batch export を呼べることを確認

- `MikuprojectCliTest.exportsPhaseDetailViewBatch`
  - Java CLI から phase detail view batch export を呼べることを確認

- `MikuprojectCliTest.exportsTaskEditViewBatch`
  - Java CLI から task edit view batch export を呼べることを確認

- `MikuprojectCliTest.appliesWbsOptionArgumentsToMarkdownAndReportDir`
  - Java CLI から WBS markdown / report directory export に display range / progress / holiday / label option を渡せることを確認

- `MikuprojectCliTest.appliesWbsOptionArgumentsToReportBundleAndWbsXlsx`
  - Java CLI から report bundle / WBS xlsx export に display range / progress / holiday / label option を渡せることを確認

- `MikuprojectCliTest.appliesSvgOptionArgumentsToSvgExports`
  - Java CLI から daily / weekly SVG の label mode と monthly SVG zip の holiday / label option を渡せることを確認

- `MikuprojectCliTest.exportsWorkbookJsonAndAiViews`
  - Java CLI から workbook JSON export、AI view export、project draft request export を呼べることを確認

- `MikuprojectCliTest.importsWorkbookJsonAndAppliesPatchJson`
  - Java CLI から workbook JSON validate / validate batch / import / import batch / merge / merge batch と patch JSON validate / validate batch / apply / apply batch を呼べることを確認

- `MikuprojectCliTest.importsAiJsonAndExternalFormats`
  - Java CLI から AI JSON import と workbook / patch / xlsx external import を呼べることを確認

- `MikuprojectCliTest.exportsHierarchyAndDependencyFixturesThroughCli`
  - Java CLI から hierarchy / dependency fixture の AI view / SVG / ms_project_xml external import を呼べることを確認

- `MikuprojectCliTest.exportsScopedPhaseDetailAndRejectsInvalidRootUidThroughCli`
  - Java CLI から hierarchy fixture の scoped phase detail と invalid root_uid 異常系を確認

- `MikuprojectCliTest.exportsReportBundleAndReportDirForFixturesThroughCli`
  - Java CLI から dependency / hierarchy fixture の report bundle / report directory の主要 entry と内容を確認

- `MikuprojectCliTest.exportsAiJsonSpecAndDetectsAiJsonKind`
  - Java CLI から AI JSON spec export と kind detect / kind detect batch を呼べることを確認

- `MikuprojectCliTest.exportsAndImportsXlsxWorkbookBytes`
  - Java CLI から workbook xlsx export / import / import batch / merge / merge batch を呼べることを確認

- `MikuprojectCliTest.validatesAndExportsWbsXlsxWorkbookBytes`
  - Java CLI から WBS xlsx export と xlsx validate / validate batch を呼べることを確認

- `MikuprojectCliTest.returnsUsageErrorForMissingArgument`
  - Java CLI の引数不足時エラーに対応

- `MikuprojectCliTest.returnsUsageErrorForUnknownCommand`
  - Java CLI の未知 command 時エラーと usage 表示に対応

- `MikuprojectCliTest.returnsUsageErrorForBatchCommandsWithOddPairs`
  - Java CLI の `*-batch` pair 数不整合エラーに対応

- `MikuprojectCliTest.returnsCommandFailureForUnsupportedCalendarMode`
  - Java CLI の unsupported calendar mode 異常系に対応

- `MikuprojectCliTest.returnsCommandFailureForUnsupportedExternalFormat`
  - Java CLI の unsupported external format 異常系に対応

- `MikuprojectCliTest.returnsIoErrorForMissingInputFile`
  - Java CLI の missing input file 時 `I/O error` 経路に対応

## Java 側 Excel IO test

- `ExcelIoTest.roundTripsWorkbookBytes`
  - Java workbook byte codec round-trip に対応

- `ExcelIoTest.rejectsInvalidWorkbookBytes`
  - invalid byte payload reject に対応

- `ExcelIoTest.exportsWorkbookAsOoxmlLikeZipPackage`
  - OOXML-like zip package export と entry list / unpack の主要導線に対応

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

- `WbsMarkdownTest.exportsHierarchyFixtureIntoReadableMarkdown`
  - hierarchy fixture を Markdown 出力へ落としたときの主要内容を確認

- `WbsMarkdownTest.exportsDependencyFixtureIntoReadableMarkdown`
  - dependency fixture を Markdown 出力へ落としたときの主要内容を確認

- `WbsMarkdownTest.appliesDisplayAndHolidayOptionsToMarkdown`
  - Markdown の display / holiday option を direct API 呼び出しで確認

## Java 側 WBS SVG test

- `WbsSvgTest.exportsDailyAndWeeklySvg`
  - upstream の daily / weekly preview SVG export に対応

- `WbsSvgTest.rendersDependencyConnectorsInDailyAndWeeklySvg`
  - upstream の dependency connector 描画に対応

- `WbsSvgTest.exportsMonthlyCalendarArchive`
  - upstream の monthly calendar SVG archive export に対応

- `WbsSvgTest.exportsDependencyFixtureIntoSvgOutputs`
  - dependency fixture を daily / weekly SVG 出力へ落としたときの主要内容を確認

- `WbsSvgTest.exportsHierarchyFixtureIntoSvgOutputs`
  - hierarchy fixture を daily / weekly SVG 出力へ落としたときの主要内容を確認

- `WbsSvgTest.appliesLabelAndHolidayOptionsToSvgOutputs`
  - SVG の label / holiday option を direct API 呼び出しで確認

## Java 側 WBS XLSX test

- `WbsXlsxTest.providesExcelStyleLayoutReferencesForWbsWorksheetTuning`
  - upstream の layout helper 基本操作に対応

- `WbsXlsxTest.canLogWbsLayoutCellReferencesOnDemand`
  - upstream の layout log helper に対応

- `WbsXlsxTest.exportsDedicatedWbsWorkbookAndCanEncodeIt`
  - upstream の WBS workbook export と xlsx encode 接続の主要導線に対応

- `WbsXlsxTest.exportsHierarchyFixtureIntoDedicatedWorkbook`
  - hierarchy fixture を WBS workbook へ落としたときの主要内容を確認

- `WbsXlsxTest.exportsDependencyFixtureIntoDedicatedWorkbook`
  - dependency fixture を WBS workbook へ落としたときの主要内容を確認

- `WbsXlsxTest.appliesDisplayAndHolidayOptionsToDedicatedWorkbook`
  - WBS workbook の display / holiday option を direct API 呼び出しで確認
