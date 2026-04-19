# Migration Status

## 目的

この文書は、Node.js upstream を基準にした Java 版移植の現在地と、保守フェーズで残る論点を整理するためのメモである。

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
- `project-patch-json-tasks.ts`
- `project-workbook-json-export.ts`
- `project-workbook-json-import.ts`
- `project-workbook-json-validate.ts`
- `project-workbook-json.ts`
- `project-workbook-schema.ts`
- `project-xlsx-export.ts`
- `project-xlsx-import.ts`
- `project-xlsx.ts`
- `core-api-workbook-xlsx.ts`
- `core-api-workbook.ts`
- `ai-json-spec.ts`
- `ai-json-util.ts`
- `core-api-msproject-ai.ts`
- `core-api-msproject.ts`
- `core-api-ai-json-import.ts`
- `core-api-ai-json.ts`
- `core-api-external-binary.ts`
- `core-api-external-document.ts`
- `core-api-external-import.ts`
- `core-api-import.ts`
- `core-api-registry.ts`
- `core-api-public.ts`
- `core-api.ts`
- `core-api-report-adapters.ts`
- `core-api-report-public.ts`
- `core-api-report.ts`
- `excel-io.ts`
- `excel-io-package-xml.ts`
- `excel-io-zip.ts`
- `excel-io-normalize.ts`
- `excel-io-workbook-build.ts`
- `excel-io-workbook-parse.ts`
- `excel-io-worksheet-build.ts`
- `excel-io-worksheet-parse.ts`
- `excel-io-styles-build.ts`
- `excel-io-styles-parse.ts`
- `markdown-escape.ts`
- `wbs-dateband.ts`
- `wbs-markdown.ts`
- `wbs-svg.ts`
- `wbs-xlsx-layout.ts`
- `wbs-xlsx.ts`

### fixture / testdata 対応済み

- `vendor/mikuproject/testdata/minimal.xml`
- `vendor/mikuproject/testdata/hierarchy.xml`
- `vendor/mikuproject/testdata/dependency.xml`

### 進捗の目安

- `MS Project XML STEP1`: およそ `98%` 前後
- upstream 全体移植: およそ `98%` 前後

これは厳密な工数比ではなく、upstream file 群と Java 側の現在実装範囲から見た概算である。
現時点では core / CLI / report 系の Java 側対応はかなり進んでいるが、最終目標は Node.js 版の全機能移植であり、Web UI / browser main 系も今後の移植対象に含む。

## 保守フェーズの論点

現在の自然な続きは、次の領域である。

- `MS Project XML` / workbook / report の fixture 比較追加
- Java CLI entry の batch / option 整理
- upstream 更新追随時の差分吸収準備

これらは現在の移植本体を崩さずに、精度と追随性を上げやすい。

## 保守フェーズで残る項目

### CLI / automation

- Java CLI entry の細かな automation command 整備
  - 現状は validate / validate batch、主要 export、report zip / report directory / report directory batch、WBS xlsx、workbook JSON export / export batch / import / merge、workbook xlsx export / export batch / import / merge、project overview view export / export batch、phase detail view export / export batch、patch JSON、AI JSON spec / kind / draft request / view export / import、external import まで
  - WBS markdown / xlsx / report directory には display range / progress / holiday option を渡せる
  - daily / weekly SVG には label mode、monthly SVG zip には holiday / label option を渡せる
  - report bundle / report directory にも holiday / label を含む SVG option と WBS option をまとめて渡せる
  - 複数入力をまとめる command は validate / report directory / workbook JSON / workbook xlsx / project overview view / phase detail view まで
  - そのほかの出力形式別の細かな option や batch command は未整理

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

## 保留中だが移植対象

現在の Java 版では、Web UI / browser main 系は未着手領域として保留している。
ただし、これは対象外を意味せず、Node.js 版全機能移植の一部として今後追う前提とする。

## 優先順の目安

現時点の自然な優先順は次のとおり。

1. `MS Project XML` / workbook / report の細部寄せとテスト厚み追加
2. Java CLI entry の batch / option 整理
3. Web UI / browser main 系の段階的移植計画を固める
4. upstream 更新追随時の差分吸収

## 補足

- upstream 更新追随時は、この文書のカテゴリ単位ではなく、対応 file 単位で差分を見る
- Java 側で着手した領域は、対応 class と test をセットで増やしていく
- Java CLI には、upstream にはない Java 側独自の複数入力 `*-batch` command を追加している
- 現時点の独自 batch command は `validate-xml-batch`, `export-report-dir-batch`, `export-workbook-json-batch`, `export-xlsx-batch`, `export-project-overview-view-batch`, `export-phase-detail-view-batch`
- これらは移植本体そのものではなく、CLI 運用上の利便性向上を目的にした後付け拡張として扱う
- Java CLI の正式配布成果物は `mvn package` で生成される単一 fat jar とし、想定パスは `target/mikuproject.jar` とする
- 利用側向けの配布パッケージとして `target/mikuproject-dist.zip` を生成し、`mikuproject.jar`, `README.md`, `LICENSE`, `docs/runtime-java-cli.md` を同梱する
