# Migration Status

## 目的

この文書は、Node.js upstream を基準にした Java 版移植の現在地と、保守フェーズで残る論点を整理するためのメモである。

ここでの整理は、実装量の厳密な測定ではなく、upstream file 単位での進捗把握を目的とする。

前提として、Java 版は Java-first の再設計を先に行うのではなく、Node.js 版をできるだけ追跡可能な形で移す `straight conversion` を原則とする。
構造改善や Java 向け再整理は、その対応 upstream 機能が Java 側へ移されたあとに扱う。

## 状態区分

この文書では、移植状態を次の 2 区分で扱う。

- `対応済み`: Java 側に主要対応 class または動作が存在する
- `未着手`: 対象であることは認識しているが、まだ移植計画や着手単位の整理が弱い

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
- `vendor/mikuproject/testdata/workbook-import-sample.json`

### 進捗の目安

- `core` 完了率の目安: およそ `98%` 前後
- `CLI / report` 完了率の目安: 主要 command / report 出力は対応済みだが、細かな option / batch 整理と測り方の固定は未了

これは厳密な工数比ではなく、upstream file 群と Java 側の現在実装範囲から見た概算である。
`README.md` でも同じ前提で、Java 版の対象範囲と現在地を `core` と `CLI / report` の 2 系統に分けて扱う。

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
- Java CLI entry を最小 entrypoint から full entrypoint へ広げる段取りは `docs/runtime-java-cli.md` に 4 段階で整理済み
  - `core 公開面の薄い入口化`
  - `主要単発 command の整備`
  - `option / batch command の整理`
  - `full entrypoint としての保守固定`
  - 現在位置は `3` と `4` の途中とみなす

## 未着手として整理を強めるべき領域

現時点では、次は `未着手` 領域について着手単位の整理を強めたい。

- CLI / report の依存整理
- upstream 更新追随時の確認単位の固定

## upstream 更新追随時の確認単位

upstream 更新追随では、カテゴリ単位ではなく `upstream file -> Java class` の対応単位で確認する。

基準:

- upstream 側の確認単位は 1 file とする
- Java 側の確認単位は、その upstream file に対応づけた class 群と test 群とする
- 対応関係の正本は `docs/upstream-class-mapping.md` とする

確認手順:

1. upstream で変更された file を列挙する
2. `docs/upstream-class-mapping.md` で対応する Java class を引く
3. 差分を `挙動差分 / 命名差分 / 未移植差分 / Java 側独自拡張` に分ける
4. 必要な test と fixture 比較を同じ単位で追加する
5. `docs/remaining-migration-items.md` と `TODO.md` へ、必要なら保留項目を反映する

差分の見方:

- `挙動差分`: upstream と Java 側の意味や結果がずれている
- `命名差分`: 追跡可能性を損なう rename や分割が起きている
- `未移植差分`: upstream 変更が Java 側へまだ入っていない
- `Java 側独自拡張`: batch command や packaging など upstream にはない後付け拡張

運用上の原則:

- まず `straight conversion` を優先し、Java 側独自整理は後回しにする
- 1 回の追随では、できるだけ 1 upstream file の責務境界を崩さない
- Java 側で 1 file が複数 class に分かれている場合も、upstream file 単位でまとめて確認する
- test も対応 file 単位で増やし、差分吸収と検証を分離しない

## 3 系統の段階計画

Java 版は、次の 3 系統で段階的に進める。

### 1. core

役割:

- `ProjectModel`
- `MS Project XML`
- workbook
- patch / AI JSON
- CSV / Mermaid
- validation

段階:

1. 既存 Java 実装と upstream file の対応を固定する
2. `対応済み / 要差分確認 / 未着手` を upstream file 単位で棚卸しする
3. lossless 交換形式と片方向補助出力の差を明文化する
4. core API 契約を、upstream 追跡可能な形で揃える

### 2. CLI

役割:

- core 機能の公開 entrypoint
- batch command
- diagnostics
- runtime packaging

段階:

1. 現行 Java CLI command と upstream 導線との差分を棚卸しする
2. command 群を `core の straight conversion` と `Java 側運用拡張` に分けて整理する
3. CLI 補助 command や automation 導線を整理する

### 3. report

役割:

- `WBS XLSX`
- `WBS Markdown`
- `SVG`
- `Mermaid`
- report bundle

段階:

1. `lossless 交換形式` と `人向け派生出力` の役割を切り分ける
2. 各 report 出力の upstream との差分を、仕様上の制約と実装差分に分けて整理する
3. report API を、upstream と対応づけて揃える

## 継承する仕様と保留する仕様

Java 版では、Node.js 版 upstream の仕様を次の 2 つに分けて扱う。

### 継承する仕様

優先してそのまま持ち込む対象:

- upstream の file 境界と責務分割
- core API の公開面
- import / export / validate の意味
- workbook / report / AI JSON / patch JSON の入出力契約

扱い方:

- まずは upstream の語彙と責務を維持する
- Java らしい再設計は、対応 upstream 機能が移り切ったあとに検討する
- 仕様差分を入れる場合も、どの upstream file の差分か追える形を維持する

### 保留する仕様

現時点で先送りしてよい対象:

- Java 側独自の構造最適化
- UI を先に前提とした抽象化
- upstream 変更と無関係な package 再編
- 追跡可能性を下げる rename

保留の原則:

- `straight conversion` を妨げる変更は先送りする
- 保留は Java 版の対象範囲内で、順番を後ろへ送るだけとみなす
- 保留した項目は `TODO.md` または `docs/remaining-migration-items.md` に明示する

## 優先順の目安

現時点の自然な優先順は次のとおり。

1. `MS Project XML` / workbook / report の細部寄せとテスト厚み追加
2. Java CLI entry の batch / option 整理
3. upstream 更新追随時の差分吸収

## 補足

- upstream 更新追随時は、この文書のカテゴリ単位ではなく、対応 file 単位で差分を見る
- Java 側で着手した領域は、対応 class と test をセットで増やしていく
- Java CLI には、upstream にはない Java 側独自の複数入力 `*-batch` command を追加している
- 現時点の独自 batch command は `validate-xml-batch`, `export-report-dir-batch`, `export-workbook-json-batch`, `export-xlsx-batch`, `export-project-overview-view-batch`, `export-phase-detail-view-batch`
- これらは移植本体そのものではなく、CLI 運用上の利便性向上を目的にした後付け拡張として扱う
- Java CLI の正式配布成果物は `mvn package` で生成される単一 fat jar とし、想定パスは `target/mikuproject.jar` とする
- 利用側向けの配布パッケージとして `target/mikuproject-dist.zip` を生成し、`mikuproject.jar`, `README.md`, `LICENSE`, `docs/runtime-java-cli.md` を同梱する
