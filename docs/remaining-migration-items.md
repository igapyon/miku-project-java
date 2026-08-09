# Migration Status

## 目的

この文書は、Node.js upstream を基準にした Java 版移植の現在地と、保守フェーズで残る論点を整理するためのメモである。

ここでの整理は、実装量の厳密な測定ではなく、upstream file 単位での進捗把握を目的とする。

運用上は、この文書を「確認結果と通過ログの正本」とし、`TODO.md` 側は今後の判断や完了条件を短く持つ。

前提として、Java 版は Java-first の再設計を先に行うのではなく、Node.js 版をできるだけ追跡可能な形で移す `straight conversion` を原則とする。
構造改善や Java 向け再整理は、その対応 upstream 機能が Java 側へ移されたあとに扱う。

## 状態区分

この文書では、移植状態を次の 2 区分で扱う。

- `対応済み`: Java 側に主要対応 class または動作が存在する
- `保守確認`: 対応済み範囲について、検証、docs 同期、upstream 差分確認を継続する

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

- `vendor/miku-project/testdata/minimal.xml`
- `vendor/miku-project/testdata/hierarchy.xml`
- `vendor/miku-project/testdata/dependency.xml`
- `vendor/miku-project/testdata/workbook-import-sample.json`

The current vendored Node baseline is `v0.12.0` (`48ec367`), with the paired
CLI test renamed in upstream commit `f437cbf` to
`tests/miku-project-cli.test.js`.

### 進捗の目安

- `core` 完了率の目安: およそ `98%` 前後
- `CLI / report` 完了率の目安: 主要 command / report 出力は対応済みであり、残る作業は既存範囲の確認、TODO / docs 整理、移植済み範囲の検証や差分確認に寄せる

これは厳密な工数比ではなく、upstream file 群と Java 側の現在実装範囲から見た概算である。
`README.md` でも同じ前提で、Java 版の対象範囲と現在地を `core` と `CLI / report` の 2 系統に分けて扱う。

## 保守フェーズの論点

現在の自然な続きは、次の領域である。

- `MS Project XML` / workbook / report の既存 fixture 比較の確認と不足観点の整理
- Java CLI entry の既存 command / option / diagnostics の追跡可能性確認
- upstream 更新追随時の差分吸収準備

これらは新規機能追加ではなく、現在の移植済み範囲を崩さずに精度と追随性を上げるための保守作業として扱う。

## 保守フェーズで残る項目

### CLI / automation

- Java CLI entry の既存 automation command 確認
  - 現状の正規 CLI 契約は Node.js CLI に寄せた `ai`, `state`, `validate`, `export`, `import`, `merge`, `report` の command group と named option を中心に扱う
  - `mikuproject_workbook_json` を Agent Skills との会話境界 state とし、XML は `validate xml` / `export xml` のような明示 command でのみ表に出す
  - `state from-draft`, `state apply-patch`, `state summarize`, `state diff`, `report all`, `report wbs-xlsx` は Java CLI test で workbook JSON state 起点の導線を固定済み
  - WBS markdown / xlsx / report directory には display range / progress / holiday option を渡せる
  - daily / weekly SVG には label mode、monthly SVG zip には holiday / label option を渡せる
  - report bundle / report directory にも holiday / label を含む SVG option と WBS option をまとめて渡せる
  - `ai spec` は実行時に JAR / classpath 内リソースを読み、`vendor/...` 相対パスには依存しない
  - daily / weekly SVG は task の最初の開始日を timeline 基準日にし、固定サンプル日付への依存を避ける
  - 複数入力をまとめる command は Java 側独自の運用拡張として扱い、現時点では追加を優先しない
  - help / `README.md` / CLI test では、主要 command 一覧、help alias、未知 command / 引数不足 / `*-batch` pair 数不整合 / unsupported calendar mode の入口契約を固定済み
  - hierarchy / dependency fixture に対する AI view / report bundle / report directory / scoped phase detail の主要出力も CLI test で固定済み
  - diagnostics については、usage error / command failed / `I/O error` の 3 系統を分け、入口契約違反には `usage error:` prefix を付ける基本契約を CLI test で固定済み
  - 残るのは、Node.js CLI と同水準の `--diagnostics json`、stdin / stdout handling、usage error code をどこまで細かく揃えるかという運用面の詰めである
- Java CLI entry を最小 entrypoint から full entrypoint へ広げる段取りは `docs/runtime-java-cli.md` に 4 段階で整理済み
  - `core 公開面の薄い入口化`
  - `主要単発 command の整備`
  - `option / batch command の整理`
  - `full entrypoint としての保守固定`
  - 現在位置は `3` と `4` の主要導線が対応済みで、README / help / test / 追随文書との同期を保守中とみなす

## 運用精度を強めるべき領域

現時点では、`upstream 更新追随時の確認単位` については基本的な文書化が済んでいる。
次は、保守確認として、運用精度を上げるための整理を強めたい。

- CLI / report の依存整理
- upstream 更新追随時の確認単位の固定
- 新規機能追加ではなく、既存実装と upstream 対応表、test 対応表、実行ログの整合確認

## upstream 更新追随時の確認単位

upstream 更新追随では、カテゴリ単位ではなく `upstream file -> Java class` の対応単位で確認する。
最初に見る入口は、この節の `関連文書` と `最短フロー` である。

関連文書:

- `README.md`
  - repo top から追随運用文書群へ入る入口
- `docs/remaining-migration-items.md`
  - 現在地、残件、直近の通過状態をまとめて見る
- `docs/upstream-class-mapping.md`
  - upstream file と Java class / package の対応を引く
- `docs/upstream-test-mapping.md`
  - upstream test 意図と Java test の対応、focused 回帰単位を引く
- `docs/development.md`
  - 日常的に使う test command と運用メモを引く
- `docs/upstream-followup-log.md`
  - 実際の差分確認結果を `upstream file` 単位で残す

最短フロー:

1. この文書で現在地と対象範囲を確認する
2. `docs/upstream-class-mapping.md` で対象 file の対応 class を引く
3. `docs/upstream-test-mapping.md` で対応 test と focused 回帰単位を引く
4. `docs/development.md` で実行する focused test command を確認する
5. 必要なら対象単位だけ test を実行し、`docs/upstream-followup-log.md` とこの文書へ結果を反映する

基準:

- upstream 側の確認単位は 1 file とする
- Java 側の確認単位は、その upstream file に対応づけた class 群と test 群とする
- 対応関係の正本は `docs/upstream-class-mapping.md` とする
- 実行する test 単位の正本は `docs/upstream-test-mapping.md` とする
- 日常的に使う focused test command の正本は `docs/development.md` とする

現状:

- `docs/upstream-class-mapping.md` には `wbs-svg.ts` / `core-api-report.ts` / `msproject-ai-views.ts` / `core-api-import.ts` / `project-workbook-json.ts` / `core-api-workbook.ts` / `project-xlsx.ts` / `core-api-workbook-xlsx.ts` を題材にした差分確認サンプルを追加済み
- `docs/upstream-class-mapping.md` の先頭にも sample coverage を置き、coverage を先に見てから各サンプルへ降りられるようにした
- `docs/upstream-test-mapping.md` には report / workbook / CLI / core API import / AI view の関連 test と focused test command を整理済み
- `docs/development.md` には report unit 回帰、report API / CLI を含むまとまった回帰、CLI entrypoint 回帰、workbook 回帰、import / AI view 回帰、upstream 追随の保守回帰の実行コマンドを整理済み
- `docs/upstream-followup-log.md` には `core-api-report.ts` / `core-api-import.ts` / `msproject-ai-views.ts` / `wbs-svg.ts` / `wbs-markdown.ts` / `wbs-xlsx.ts` / `project-workbook-json.ts` / `core-api-workbook.ts` / `project-xlsx.ts` / `core-api-workbook-xlsx.ts` を題材にした差分確認記録 sample を追加済み
- report / workbook / import / AI view の主要導線は「対応表」「実行コマンド」「実記録 sample」まで揃った状態にある
- 現フェーズでは、新規機能追加ではなく、これらの文書と既存実装 / test 対応のズレを小さく保つ
- `docs/development.md` / `docs/upstream-test-mapping.md` から `docs/upstream-followup-log.md` へ辿れる導線も追加済み
- `docs/upstream-class-mapping.md` と `docs/upstream-followup-log.md` の間も相互参照できる状態にした
- `docs/upstream-followup-log.md` 自体にも、記録前に回す focused / maintenance 回帰単位を明記した
- `docs only` 更新では原則として追加テストを回さず、コード変更や新しい回帰コマンド追加時だけ対象単位を実行する方針も `docs/development.md` に明記した
- `README.md` からも追随運用文書群へ辿れる入口を追加済み
- `README.md` にも `docs only` 更新時の運用注意を反映済み
- `README.md` の `Suggested tracking flow` と内部文書の `最短フロー` も同じ順序に揃えた
- 追随運用の現在地は、次の 6 文書で入口と役割分担が揃った状態にある
  - `README.md`: repo top からの入口
  - `docs/upstream-class-mapping.md`: file -> class の対応
  - `docs/upstream-test-mapping.md`: file / test 意図 -> Java test / focused 回帰
  - `docs/development.md`: 日常運用の test command
  - `docs/upstream-followup-log.md`: 実記録
  - `docs/remaining-migration-items.md`: 現在地 / 残件 / 直近通過状態
- 追随運用文書の初期整備は docs-only 変更として整理済みである
- 現フェーズの docs 更新は、実態と文書のズレを小さく保つための保守整理として扱う
- 2026-04-20 時点では `mvn test -Dtest=WbsMarkdownTest,WbsSvgTest,WbsXlsxTest,CoreApiPublicTest,MikuprojectCliTest` が `58` tests, `0` failures で通っている
- 2026-04-20 時点では `mvn test -Dtest=ProjectWorkbookJsonTest,ProjectXlsxTest,CoreApiWorkbookTest` も `20` tests, `0` failures で通っている
- 2026-04-20 時点では `mvn test -Dtest=CoreApiImportTest,MsProjectAiViewsTest` も `16` tests, `0` failures で通っている
- 2026-04-20 時点では、保守回帰の旧単位として `mvn test -Dtest=WbsMarkdownTest,WbsSvgTest,WbsXlsxTest,CoreApiPublicTest,CoreApiImportTest,MsProjectAiViewsTest,MikuprojectCliTest` が `74` tests, `0` failures で通っている
- 2026-04-21 時点では、Project XLSX layout 補強後に `mvn test -Dtest=ProjectXlsxTest,ExcelIoTest` が `12` tests, `0` failures、`mvn test -Dtest=ProjectWorkbookJsonTest,ProjectXlsxTest,CoreApiWorkbookTest` が `20` tests, `0` failures、`mvn test` が `143` tests, `0` failures, `3` skipped で通っている
- 2026-04-21 時点では、XLSX / OOXML の保守回帰根拠として `ExcelIoTest` が OOXML zip entry / worksheet XML / data validation / freeze pane / formula を確認し、`ProjectXlsxTest` が主要セル style、sheet theme、Settings section / merged range、boolean data validation を確認しているため、TODO 上の XLSX / OOXML 簡略実装戻し節は完了扱いに寄せた
- 2026-04-21 時点では、straight conversion が薄かった report / XLSX / patch 周辺の補強は TODO 上で完了扱いに寄せた。Node parity 方針も、現フェーズでは report 系出力の opt-in parity を完了ラインとし、JSON view / XML / diagnostics / project XLSX を含む「全出力」への拡張は次段の保守論点として切り分ける方針に整理した
- 2026-04-21 時点では、Project Patch JSON の delete 系 warning / changes 補強後に `mvn test -Dtest=ProjectPatchJsonTest,ProjectPatchJsonTasksTest` が `8` tests, `0` failures、`mvn test -Dtest=ProjectPatchJsonTest,ProjectPatchJsonTasksTest,CoreApiWorkbookTest,MikuprojectCliTest` が `49` tests, `0` failures で通っている
- 2026-04-21 時点では、`project_draft_view` import の task 日付 fallback 契約確認として `mvn test -Dtest=MsProjectAiViewsTest` が `6` tests, `0` failures で通っている。upstream / Java とも task ごとの `planned_start` / `planned_finish` は必須ではなく、欠けた場合は project 起点 fallback を使う
- 2026-04-21 時点では、zero duration cluster warning 方針を `validateProjectModel(...)` / `validate xml --in` に固定し、`mvn test -Dtest=MsProjectXmlTest,MikuprojectCliTest` の対象追加分で確認する
- 2026-04-21 時点では、report 出力同等性の保守回帰として `MikuprojectCliTest.keepsReportBundleAndReportDirOutputsEquivalentToStandaloneWbsXlsx` を追加し、`dependency.xml` で report bundle zip と report dir の entry 名 / entry bytes、および standalone `report wbs-xlsx` と report 同梱 `wbs.xlsx` の byte 一致を固定した
- 2026-04-21 時点では、monthly calendar coverage を `WbsSvgTest.exportsMonthlyCalendarArchiveForSampleAndFixtureRanges` で sample / dependency / hierarchy の 3 系統へ広げ、プロジェクト期間に含まれる月数、`YYYY-MM.svg` file 名、各 SVG 非空を固定した。CLI 側の zip path は既存 test で `monthly-calendar/YYYY-MM.svg` を確認済みである
- 2026-04-21 時点では、`vendor/miku-project/src/ts/msproject-mermaid.ts` を題材にした upstream follow-up 実例を追加し、`mvn test -Dtest=MsProjectMermaidTest` が `3` tests, `0` failures で通っている
- 2026-04-21 時点では、`vendor/miku-project/src/ts/msproject-xml.ts` を題材にした upstream follow-up 実例も追加し、`mvn test -Dtest=MsProjectXmlTest` が `18` tests, `0` failures で通っている
- 2026-04-21 時点では、`vendor/miku-project/src/ts/msproject-codec.ts` を題材にした upstream follow-up 実例も追加し、同じく `mvn test -Dtest=MsProjectXmlTest` の fixture / round-trip 確認を根拠に記録した
- 2026-05-01 時点では、`mikuproject/devel` の `245deaa` まで `vendor/miku-project` を追随し、Node.js CLI の binary I/O 更新を Java CLI へ反映した
- 2026-05-01 時点では、Java CLI の `export xlsx` / `report wbs-xlsx` / `report monthly-calendar-svg` / `report all` は binary artifact を `--out <path>` または `--out-base64 -` で出力する契約に寄せた。`--out -` や `--out` 省略による binary stdout は usage error とする
- 2026-05-01 時点では、Java CLI の `import xlsx` は `--in <path>` に加えて `--in-base64 -` を受け、diagnostics JSON の io に `stdin_base64` / `stdout_base64` を記録する
- 2026-05-01 時点では、上記 CLI 追随の focused 回帰として `mvn test -Dtest=MikuprojectCliTest` が `16` tests, `0` failures で通っている
- 2026-04-21 時点では、`vendor/miku-project/src/ts/msproject-validate.ts` を題材にした upstream follow-up 実例も追加し、同じく `mvn test -Dtest=MsProjectXmlTest` の validation focused tests を根拠に記録した
- workbook を含む新しい保守回帰コマンドは `docs/development.md` / `docs/upstream-test-mapping.md` 側の正本に合わせて更新済みであり、次回のコード変更時にその単位で確認する
- 残る論点は、これらの文書を使って upstream 更新 1 回分の実例を積み、過不足を詰めることである

確認手順:

1. upstream で変更された file を列挙する
2. `docs/upstream-class-mapping.md` で対応する Java class を引く
3. 差分を `挙動差分 / 命名差分 / 未移植差分 / Java 側独自拡張` に分ける
4. 必要な test と fixture 比較を同じ単位で確認し、既存回帰で足りない場合だけ補う
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
- test も対応 file 単位で確認し、差分吸収と検証を分離しない

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
2. `対応済み / 保守確認 / 保留` を upstream file 単位で棚卸しする
3. lossless 交換形式と片方向補助出力の差を明文化する
4. core API 契約を、upstream 追跡可能な形で揃える

### 2. CLI

役割:

- core 機能の公開 entrypoint
- batch command
- diagnostics
  - usage error / command failure の出し分けは CLI test で固定済み
- runtime packaging

段階:

1. 現行 Java CLI command と upstream 導線との差分を棚卸しする
2. command 群を `core の straight conversion` と `Java 側運用拡張` に分けて整理する
3. 追加済み CLI 補助 command や automation 導線を、README / help / test / 追随文書で保守する

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
   - `WBS Markdown` / `SVG` / `WBS XLSX` / report bundle / report directory について、hierarchy / dependency fixture の主要出力比較を Java test で固定済み
   - option についても、`WBS Markdown` / `SVG` / `WBS XLSX` の direct API test と CLI test の両方で display / progress / holiday / label の反映を固定済み
   - `dependency.xml` の opt-in Node parity では report directory の `wbs.md` / `mermaid.mmd` / `daily.svg` / `weekly.svg` / `monthly-calendar/*.svg` / `wbs.xlsx` が byte-level 一致することを確認済み
   - 同じ opt-in Node parity で report bundle ZIP と monthly SVG ZIP も byte-level 一致することを確認済み
   - 現時点では report 周りの主要 regression は unit / core API / CLI の 3 層でまとまって通る状態にある
   - 直近のまとまった確認では `WbsMarkdownTest`, `WbsSvgTest`, `WbsXlsxTest`, `CoreApiPublicTest`, `MikuprojectCliTest` をまとめて通しており、report の主要導線はこの単位で回帰確認できる
   - 残る論点は、各 report の option 契約と比較粒度をどこまで細かく保守するか、という精度面の詰めである

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

1. `MS Project XML` / workbook / report の既存 fixture 比較と回帰単位の確認
2. Java CLI entry の既存 diagnostics / option 表現 / docs 同期の整理
3. upstream 更新追随時の差分吸収

## 補足

- upstream 更新追随時は、この文書のカテゴリ単位ではなく、対応 file 単位で差分を見る
- Java 側で着手済みの領域は、対応 class と test の対応関係をセットで保守する
- Java CLI は Agent Skills からの利用を優先し、正規契約では command group / named option を使う
- 旧 CLI にあった batch command 群は正規契約から外し、README / help では扱わない
- 現フェーズでは、batch command を増やすことは優先しない
- Java CLI の正式配布成果物は `mvn package` で生成される単一 fat jar とし、想定パスは `target/miku-project.jar` とする
- `mvn package` で追跡・レビュー用の `target/miku-project-sources.jar` も生成する
- 利用側向けの配布パッケージとして `target/miku-project-dist.zip` を生成し、`miku-project.jar`, `miku-project-sources.jar`, `README.md`, `LICENSE`, `docs/runtime-java-cli.md` を同梱する
