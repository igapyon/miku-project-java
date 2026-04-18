# Remaining Migration Items

## 目的

この文書は、Node.js upstream を基準にした Java 版移植の残作業一覧を整理するためのメモである。

ここでの整理は、実装量の厳密な測定ではなく、upstream file 単位での進捗把握を目的とする。

## 現在地

### 対応済みの主領域

以下は、Java 側に主要対応 class が存在する領域である。

- `types.ts`
- `msproject-xml.ts`
- `msproject-codec.ts`
- `msproject-xml-dom.ts`
- `msproject-calendar.ts`
- `msproject-validate.ts`
- `msproject-validate-helpers.ts`
- `msproject-samples.ts`
- `msproject-csv.ts`
- `msproject-mermaid.ts`
- `msproject-ai-views.ts`
- `project-patch-json.ts`
- `project-patch-json-core.ts`
- `project-patch-json-util.ts`
- `project-patch-json-links.ts`
- `project-patch-json-entities.ts`
- `project-patch-json-updates.ts`

### fixture / testdata 対応済み

- `vendor/mikuproject/testdata/minimal.xml`
- `vendor/mikuproject/testdata/hierarchy.xml`
- `vendor/mikuproject/testdata/dependency.xml`

### 進捗の目安

- `MS Project XML STEP1`: およそ `65%` 前後
- upstream 全体移植: およそ `15%〜20%` 前後

これは厳密な工数比ではなく、upstream file 群と Java 側の現在実装範囲から見た概算である。

## 次候補

STEP1 の流れを保ったまま次に進めやすいのは、次の領域である。

- `project-patch-json-tasks.ts`

これらは `msproject*` 系であり、現在の `MS Project XML` まわりの実装群と比較的近い責務を持つ。

## 未着手一覧

### MS Project 拡張系

### AI JSON / patch 系

- `ai-json-spec.ts`
- `ai-json-util.ts`
- `core-api-ai-json-import.ts`
- `core-api-ai-json.ts`
- `project-patch-json-tasks.ts`

### workbook / XLSX 系

- `project-workbook-json-export.ts`
- `project-workbook-json-import.ts`
- `project-workbook-json-validate.ts`
- `project-workbook-json.ts`
- `project-workbook-schema.ts`
- `project-xlsx-export-calendars.ts`
- `project-xlsx-export-entities.ts`
- `project-xlsx-export-project.ts`
- `project-xlsx-export-util.ts`
- `project-xlsx-export.ts`
- `project-xlsx-import-calendars.ts`
- `project-xlsx-import-entities.ts`
- `project-xlsx-import-project.ts`
- `project-xlsx-import-util.ts`
- `project-xlsx-import.ts`
- `project-xlsx.ts`
- `excel-io-normalize.ts`
- `excel-io-package-xml.ts`
- `excel-io-styles-build.ts`
- `excel-io-styles-parse.ts`
- `excel-io-util.ts`
- `excel-io-workbook-build.ts`
- `excel-io-workbook-parse.ts`
- `excel-io-worksheet-build.ts`
- `excel-io-worksheet-parse.ts`
- `excel-io-zip.ts`
- `excel-io.ts`
- `core-api-workbook-xlsx.ts`
- `core-api-workbook.ts`

### WBS / report 出力系

- `wbs-dateband.ts`
- `wbs-markdown.ts`
- `wbs-svg-axis.ts`
- `wbs-svg-bars.ts`
- `wbs-svg-calendar.ts`
- `wbs-svg-labels.ts`
- `wbs-svg-public.ts`
- `wbs-svg-render.ts`
- `wbs-svg-scaffold.ts`
- `wbs-svg-timeline.ts`
- `wbs-svg-viewport.ts`
- `wbs-svg-zip.ts`
- `wbs-svg.ts`
- `wbs-xlsx-base.ts`
- `wbs-xlsx-cells.ts`
- `wbs-xlsx-export.ts`
- `wbs-xlsx-layout.ts`
- `wbs-xlsx-public.ts`
- `wbs-xlsx-sections.ts`
- `wbs-xlsx-taskmeta.ts`
- `wbs-xlsx.ts`
- `core-api-report-adapters.ts`
- `core-api-report-public.ts`
- `core-api-report.ts`

### 公開 API / 統合層

- `core-api-external-binary.ts`
- `core-api-external-document.ts`
- `core-api-external-import.ts`
- `core-api-import.ts`
- `core-api-msproject-ai.ts`
- `core-api-msproject.ts`
- `core-api-public.ts`
- `core-api-registry.ts`
- `core-api.ts`

### Web UI / browser main 系

- `main-archive-actions.ts`
- `main-button-events.ts`
- `main-downloads.ts`
- `main-events.ts`
- `main-export.ts`
- `main-flow.ts`
- `main-import-actions.ts`
- `main-import.ts`
- `main-input-events.ts`
- `main-io.ts`
- `main-model.ts`
- `main-output-actions.ts`
- `main-preview-actions.ts`
- `main-preview.ts`
- `main-render.ts`
- `main-samples.ts`
- `main-save-state.ts`
- `main-support.ts`
- `main-tab-actions.ts`
- `main-transform.ts`
- `main-ui.ts`
- `main-util.ts`
- `main-xml-actions.ts`
- `main.ts`

## 保留の考え方

現在の Java 版では、Web UI は移植対象外としている。
したがって、`main*.ts` 群は存在していても、現段階では保留でよい。

また、AI view / patch / workbook / XLSX / WBS 出力系は、`MS Project XML STEP1` のあとに進める候補である。

## 優先順の目安

現時点の自然な優先順は次のとおり。

1. `project-patch-json-tasks.ts`
2. `project-workbook-json*.ts`
3. `project-xlsx*.ts`

## 補足

- upstream 更新追随時は、この文書のカテゴリ単位ではなく、対応 file 単位で差分を見る
- Java 側で着手した領域は、対応 class と test をセットで増やしていく
