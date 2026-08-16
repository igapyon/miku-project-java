# TODO

## 使い分け

- この `TODO.md` は Java 版 CLI 中心の移植計画を扱う
- ここでは「今後の判断」や「完了条件」を優先して残し、個々の確認結果や通過ログの正本は `docs/remaining-migration-items.md` に寄せる
- `vendor/mikuproject/docs/TODO.md` は upstream 機能差分、仕様制約、入出力の未整理事項を扱う

## 最重要

### Gate G5へ向けたv1 Java CLI適合

詳細は[docs/v1-contract-snapshot.md](docs/v1-contract-snapshot.md)と親repositoryの`miku-project` v1 implementation planを参照する。既存`vendor/mikuproject/`はhistorical portのlegacy referenceであり、新v1の仕様正本ではない。

- [x] `ZB-P5-A1` Node `v1.0.3` tag / revisionからallowlistだけを抽出し、tag identity確認後はfull revisionだけを読んで`vendor/miku-project-contract/v1.0.3/`へimmutable snapshotとcanonical `SOURCE.json`を生成するimporterを追加した。完全一致する既存snapshotはmetadata不変no-op、それ以外はhard errorとし、exclusive directory creation・repository外/symlink親拒否・ownership確認済みfailure cleanupで既存treeを上書きしない
- [x] `ZB-P5-A2` Java testでsnapshotのsource identity、corpus digest、member set、exact directory topology、`SOURCE.json` raw SHA-256外側pin、regular/non-symlink、size、SHA-256を検証し、extra / missing / tampered / symlink / fractional size / rewritten manifestを拒否する。Node importer testは正常install、metadata不変の同一rerun、既存tree / empty directory、tag / revision、working tree、確認後tag移動、出力path、symlink親、deterministic manifest、Git symlink、marker / member / manifest failure cleanup、unknown file / empty directory保全を検証する（12 tests）。repository-wide `sh scripts/test-all.sh`をRelease workflowでも実行する
- [x] `ZB-P5-A3` README、upstream snapshot / CLI mapping、development notesでv1 snapshotとlegacy subtreeのauthorityを分離した。P5-Aは2026-08-14に最終レビューを通過し、承認済みである
- [x] `ZB-P5-B1` `suite-index.json` / `contract-cases.json` loaderをtest-side harnessに追加した。public entrypointはP5-A snapshot verifier後のimmutable treeだけを読み、30 workflow caseと31 contract/binding caseを型付きで読み出す。unknown field、case ID重複、path escape、allowlist外参照をfail-closedにした（2026-08-14、focused 8 tests、Node 12 tests + Java 148 tests成功）
- [x] `ZB-P5-B2` result / diagnostic / artifact / runtime manifestの四Schema registryをJava 8 test-sideで実装した。checked-in positive example、代表negative example、JSON Schema layerの18 mutation caseを検証し、未対応語彙と登録外refはfail-closedにする
- [x] `ZB-P5-B3` canonical JSON / SHA-256とcase materializerをtest-sideに追加し、`cross-artifact-binding` 13 caseで`RB-001`〜`RB-006`、`RB-011`、`RB-012`を実入力から検証した。`exact-json` / `semantic-state` / `semantic-cross-runtime` / `byte-same-runtime` / `artifact-topology` / `runtime-integrity`も別assertion boundaryとしてtestし、Node live outputやJava固有goldenをoracleにしない（2026-08-14、Node 12 tests + Java 161 tests、failure/error 0、skip 4）
- [x] `ZB-P5-B4` P5-B harnessを再技術reviewした。前回review後に見つけたwhole-object JSON textによるsemantic collection整列を、Node参照実装どおりdependency tuple / UID / Unicode scalar順の共通domain-aware canonicalizerへ置換し、digestと`exact-json` / `semantic-state` / `semantic-cross-runtime`へ適用した。summary taskと`summary = false`だがchildを持つschema-valid ProjectionのRB-012 negative testを追加した。focused test、`sh scripts/test-all.sh`（Node importer 12 tests、Java 164 tests、failure/error 0、skip 4）、snapshot post-verification（8 tests）、両repoの`git diff --check`を2026-08-15に成功させた。P5-Bは同日に人が承認した
- [ ] `ZB-P5-C1` `validate`を最初のv1 vertical sliceとして実装し、初回・再技術reviewのJava側指摘を修正した。再技術reviewと人の承認を待つ。`CU-USAGE-001`の現行Node/Java source挙動はcorpusどおり一致したが、frozen `v1.0.3` release/snapshotは修正前なのでcross-runtime / Gate G5の適合をここで主張しない
  - legacy `MikuprojectCli`を変更せず、isolated `MikuProjectV1Validate`だけが厳格な`validate --project <path|-> [--result <path|->]`を受ける。v1 runtime manifest launcherは未実装なので、callerがSHA-256を検証済みの`VerifiedRuntime`を渡せない場合はproject inputを読む前に`runtime.manifest-invalid` / exit 3でfail closedにする
  - direct regular XML / stdin、UTF-8（先頭BOMのnormalizationを含む）、MS Project XML subset、semantic validation、canonical semantic-state SHA-256、構造化JSON result / diagnostic、exclusive-create `--result`を実装した。artifact-set directoryはP5-C5 `verify-artifact`まで明示的に未対応である
  - 再reviewでは、`CU-USAGE-001`をservice prefixなしのwhole CLI argvとして直接実行し、option scope / option locationを固定した。percent欠落を`S-I008`、欠損dependency endpointを`S-I008`、既存でないendpointを`S-I014`に分離し、Java整数をNode `Number.isSafeInteger`と同じ`0`〜`2^53-1`に広げた。resource / assignment / calendarのunknown・calendar参照はUID付きdiagnostic pathへ揃え、巨大outline levelは入力member数で上限を設けてallocationをfail-safeにした
  - 固定corpusの`command = cli`、`arguments = ["--unknown-option"]`、`cli.unknown-option`を正本とし、現行Node parserも先頭unknown long optionをoption scope / option location付き`cli.unknown-option`に修正した。Java serviceと現行Node sourceの挙動は一致する。snapshotは改変せず、後続Node corrective releaseと新snapshotでreference identityを更新する
  - immutable v1.0.3 corpusの`CV-VALID-001`、hierarchy valid、invalid、unsupported、hierarchy invalid、`CU-USAGE-001`、runtime failure、malformed XML、result overwrite拒否に加え、初回・再review回帰をfocused 12 testsで確認した。Nodeのargv / XML adapter / R1 integration関連20 testsと全体`npm test`、`sh scripts/test-all.sh`のNode importer 12 tests、Java 176 tests（failures / errors 0、skipped 4）、snapshot post-verification 8 testsが成功した。これはP5-C1の人による承認、またはcorrective release identityを含むcross-runtime適合をまだ意味しない

### 現在フェーズの運用方針

- historical portでは、新規機能追加ではなく、既存実装の確認、TODO / docs 整理、移植済み範囲の検証や差分確認に限定して進める。v1 CLIは上記Gate G5工程に限り新規適合実装を行う
- `docs/remaining-migration-items.md`, `docs/upstream-class-mapping.md`, `docs/upstream-test-mapping.md`, `docs/upstream-followup-log.md` の整合を保つ
- 既存 command / API / fixture 回帰の不足が見つかった場合は、機能追加ではなく既存仕様の不足またはバグとして扱う

### 直近の限定作業

- 再開ポイント:
  - ここまでで `dependency.xml` の report directory / report bundle ZIP / monthly SVG ZIP は Node upstream と byte-level parity 済み
  - ここまでで WBS XLSX / SVG / report ZIP 系の大きな簡略化は潰し、`mvn package` と opt-in Node parity が通っている
  - `ProjectXlsxExport*` の editable cell styling、sheet theme、Project sheet の Settings section / merged range は upstream TypeScript と照合して Java 側へ反映済み
  - `projectpatchjson` の warning / blocker details は `delete_task`, `delete_resource`, `delete_calendar`, `delete_assignment` を upstream と構造比較し、Java 側へ `delete_assignment` を反映済み
  - 次に再開するなら、Agent Skills 側の `project_draft_view` 生成契約確認、または zero duration task warning 方針の整理へ進む
- [x] 残作業を新規機能追加ではなく、既存範囲の確認 / docs 整理 / upstream 差分確認へ絞る
- [x] `README.md`, `docs/remaining-migration-items.md`, `docs/upstream-class-mapping.md`, `docs/upstream-followup-log.md`, `docs/msprojectxml-import-design.md` の現在フェーズ表現を揃える
- [x] Agent Skills 側で `project_draft_view` 生成時に `planned_start` / `planned_finish` を入れる前提になっているか確認する
  - `mikuproject-skills` 紹介記事や upstream sample では `planned_start` / `planned_finish` を含む例があるが、現時点で task ごとの日付を必須とする契約までは固定されていない
  - upstream `msproject-ai-views.ts` と Java `MsProjectAiViews` はどちらも task 日付未指定を許容し、`project.planned_start` または `project.planned_finish` を起点に import する
  - そのため 2泊3日など期間を持つ計画でも Agent Skills 側が task 日付を落とすと、全 task が同一日時 / zero duration に寄り得る。Java 側で自動補完する話とは分けて、warning 方針の論点として残す
- [x] miku-project-java 側で zero duration task が大量に来たときの warning 方針を検討する
  - 方針は import 導線ごとの個別 warning ではなく、共通 `ProjectModel` validation warning として扱う
  - 現時点では `validate-xml` / `validateProjectModel(...)` で、placeholder / summary / milestone を除く複数 task が同一 `start` / `finish` かつ zero duration に潰れている場合に warning を返す
  - workbook JSON / AI JSON / XML import 自体の戻り warning へ混ぜるのは見送り、既存の validation 導線と runtime docs に揃える
- [x] Node 版に比べて Java 版の straight conversion が薄い箇所を解消する
  - 優先度 A: `excelio` / `XlsxWorkbookCodec`
    - [x] `exportWorkbook(...)` が実 Excel OOXML zip ではなく独自 `mikuproject_xlsx_workbook_v1` 形式を返している問題を修正し、主要導線を OOXML ZIP へ切り替える
    - [x] `exportWorkbookArchive(...)` / `importWorkbookArchive(...)` は report / CLI / Core API の主要導線から使う形へ寄せた
    - [x] upstream の `dataValidations` と style descriptor の主要部を Java 側の OOXML build / parse に反映する
    - [x] `formula`, `freezePane` を Java 側 model / OOXML build / parse に反映する
  - 優先度 A: `projectxlsx`
    - [x] `ProjectXlsxExport*` の列幅、boolean data validation、boolean option sheet を upstream 寄りに補強する
    - [x] `ProjectXlsxExport*` の editable cell styling、sheet theme、Project sheet の Settings section / merged range を upstream 寄りに補強する
    - [x] import 側も upstream の workbook validation / editable cell 前提とズレがないか、fixture round-trip と sheet 構造で確認する
  - 優先度 A: `wbsxlsx`
    - [x] WBS workbook の主要導線が独自 workbook bytes を出しており、`wbs.xlsx` というファイル名と実体が合っていない問題を修正する
    - [x] upstream 相当の row height、hidden columns、style、date band cell、progress band、summary / legend へ寄せる
    - [x] `dependency.xml` の Node parity で `wbs.xlsx` の OOXML ZIP / worksheet XML / styles XML / workbook XML が byte-level 一致するところまで寄せる
    - `freeze` / `formula` は Java 側 model / OOXML build / parse へ反映済み。WBS workbook 自体は現時点でこれらを使っていない
  - 優先度 B: `wbssvg`
    - [x] `dependency.xml` の Node parity で `daily.svg` / `weekly.svg` / `monthly-calendar/*.svg` が byte-level 一致するところまで寄せる
    - [x] 週次 SVG は viewport trim / label placement / dependency connector routing の主要部を Java 側へ反映済み
    - [x] daily / weekly / monthly について、見た目の目視だけでなく class / shape / path 構造比較テストを追加する
  - 優先度 B: `projectpatchjson`
    - [x] `first cut` 制限自体は upstream 由来だが、Java 側の warning 詳細や参照 blocker が薄くなりやすい箇所を確認する
    - [x] `delete_task`, `delete_resource`, `delete_calendar`, `delete_assignment` の warning / changes を upstream と構造比較する
  - 優先度 C: docs / follow-up log
    - [x] `docs/upstream-followup-log.md` の「大きな差分は見当たらない」という過去記録を、今回見つかった薄い箇所で更新する
    - [x] `docs/remaining-migration-items.md` の CLI/report 完了率表現を、XLSX / SVG の再点検が終わるまで保守的に戻す
- [x] 同一入力に対する Node 版 / Java 版の全出力 byte-level parity 方針を決め、比較テストへ分解する
  - 方針: straight conversion の検証軸として、CLI / Core API が返す「出力という出力」は原則 Node 版 upstream とバイト一致を目標に置く。ただし現フェーズでは report 系出力を優先して parity を固定し、JSON view / XML / diagnostics / project XLSX など非 report 全出力への拡張は次段の保守対象として明示的に切り分ける
  - 対象 A: report 派生出力の `wbs.md`, `mermaid.mmd`, `daily.svg`, `weekly.svg`, `monthly-calendar/*.svg`, `wbs.xlsx`, report bundle ZIP, report directory
  - 対象 B: workbook / project 交換出力の workbook JSON, project overview view JSON, phase detail view JSON, task edit view JSON, project draft request JSON, exported XML, project XLSX
  - 対象 C: validation / detection / import / merge 系の stdout text / JSON diagnostics / generated XML / generated workbook bytes
  - 前提: `svg`, `md`, `mmd`, JSON, XML のような plain text 生成物は、同じ model / options / key order / 改行規約 / UTF-8 encoding でバイト一致を目標にできる
  - 前提: `.xlsx` / report bundle / monthly SVG ZIP も、entry 順、entry 名、entry bytes、ZIP local header / central directory / CRC32 / 圧縮方式 / timestamp を upstream と揃えればバイト一致を目標にできる
  - upstream 確認: `main-util.ts` の report bundle ZIP と `wbs-svg.ts` / `wbs-svg-zip.ts` の monthly SVG ZIP は DOS timestamp を `2025-01-01 00:00:00` 相当に固定している
  - upstream 確認: `excel-io-zip.ts` の OOXML workbook ZIP は手書き ZIP だが mod time / mod date は `0` を書いており、report/monthly ZIP の `2025-01-01` 固定とは異なる
  - [x] Java 側の `ExcelIoZip.packZip`, `CoreApiReport.packZipEntries`, `WbsSvgZip.packMonthlyEntries` を deterministic stored ZIP writer に寄せ、report / monthly ZIP は `2025-01-01 00:00:00`、OOXML workbook ZIP は mod fields `0` を使う
  - 段階 1: `md` / `mmd` / standalone SVG / JSON view / XML など単体 text 出力を fixture ごとに Node 版出力とバイト比較する
    - 現時点では report 系 text 出力は `dependency.xml` の opt-in Node parity に含まれている。JSON view / XML / diagnostics まで広げるかは、現フェーズでは未着手のまま保留する
  - 段階 2: ZIP を展開した entry-level parity として、report bundle / monthly SVG ZIP / `.xlsx` の entry 名、entry 順、entry bytes を比較する
    - 現時点では report bundle / monthly SVG ZIP / `wbs.xlsx` の report 系 parity を確認済みであり、project XLSX や非 report 出力全体への拡張は今後の保守対象として残す
  - [x] 段階 3 の前提として Java 側に upstream と同じ stored ZIP writer を移植する
  - [x] `.xlsx` の主要導線を独自 `mikuproject_xlsx_workbook_v1` 出力から OOXML ZIP 出力へ切り替える
  - [x] `styles.xml` を固定 2 スタイルから upstream 相当の動的 style book 生成へ寄せる
  - [x] `wbs.xlsx` の worksheet model を upstream 相当の project info / task rows / legend / summary / progress band へ寄せる
  - [x] `.xlsx` の worksheet XML / styles XML / workbook XML の byte-level parity を entry 単位で確認する
    - 現状: `dependency.xml` の opt-in Node parity は report directory 出力一式で一致する
  - [x] `dependency.xml` の opt-in Node parity で report bundle ZIP / monthly SVG ZIP の byte-level parity を確認する
    - `CoreApiReport` の bundle entry 順も upstream と同じ `wbs.xlsx`, `wbs.md`, `mermaid.mmd`, `daily.svg`, `weekly.svg`, `monthly-calendar/*.svg` に揃える
    - Project XLSX 側の editable styling / sheet theme / Settings section などの workbook layout 差分は 2026-04-21 時点で補強済み
  - parity が難しい場合の例外は、差分理由を固定値、entry 順、JSON key order、XML serialization、数値丸め、locale / timezone、CLI diagnostics のどれかへ分類し、正規化比較へ逃げる箇所を TODO ではなく明示的な仕様として記録する
    - 2026-04-21 時点では report 系 parity を現フェーズの完了ラインとし、非 report 出力への拡張は `docs/remaining-migration-items.md` の保守論点として残す
- [x] report 生成物全体の品質を再点検する
  - SVG の日付軸 / 月次カレンダーが簡略実装へ退化していたため、同じ report 系の XLSX / ZIP / directory export も信用しすぎない
  - [x] `dependency.xml` の report directory 出力で、SVG / XLSX / MD / MMD の Node 版 byte-level parity を確認する
  - [x] `dependency.xml` の report bundle ZIP / monthly SVG ZIP 出力で Node 版 byte-level parity を確認する
  - SVG は既存 TypeScript 版に寄せ、title 位置、style class、bar / milestone / phase の形状、dependency connector、weekly meta、monthly calendar の見た目を構造比較する
  - `export-report-dir`, `export-report-bundle`, `export-monthly-svg-zip`, `export-wbs-xlsx` を同一 fixture で出力し、entry 名、0 バイト有無、主要シート / 主要セル / SVG 構造を確認する
  - [x] `wbs.xlsx` と standalone `export-wbs-xlsx` の内容が同等か確認する
    - 2026-04-21 時点で `MikuprojectCliTest` に bundle / report dir / standalone `wbs.xlsx` の byte 一致確認を追加済み
  - [x] report bundle zip と report dir の entry 構成が同等か確認する
    - 2026-04-21 時点で `MikuprojectCliTest` に entry 名と各 entry bytes の一致確認を追加済み
  - [x] monthly calendar はプロジェクト期間に含まれる月がすべて出ること、各 SVG が空でないこと、zip 内 path が `monthly-calendar/YYYY-MM.svg` になることを確認する
    - 2026-04-21 時点で `WbsSvgTest` に sample / dependency / hierarchy の月数・file 名・非空確認を追加し、CLI 側では zip path を `monthly-calendar/YYYY-MM.svg` で固定済み
  - [x] dependency / hierarchy / 実利用サンプルの 3 系統で fixture を分け、サンプルだけで通る状態を避ける
    - 2026-04-21 時点で monthly calendar の fixture coverage を sample / dependency / hierarchy に拡張済み
  - [x] upstream TypeScript 版との目視または構造比較が必要な項目を `docs/upstream-followup-log.md` へ記録する
    - 2026-04-21 時点で `core-api-report.ts` と `wbs-svg.ts` の follow-up 記録へ、report 同等性回帰と monthly calendar の 3 系統 coverage を追記済み
- [x] XLSX / OOXML 系の簡略実装を upstream 相当へ戻す
  - [x] `CoreApiReport` の `wbs.xlsx` entry と `export-wbs-xlsx` が `XlsxWorkbookCodec.exportWorkbook(...)` の独自 `mikuproject_xlsx_workbook_v1` 形式を出しており、実 Excel workbook zip ではない問題を修正する
  - [x] `XlsxWorkbookCodec.exportWorkbookArchive(...)` / `importWorkbookArchive(...)` を report / CLI の主要導線から使う
  - [x] `XlsxSheetLike` に upstream の `freezePane`、`XlsxCellLike` に upstream の `formula` を追加する
  - [x] `ExcelIoWorksheetBuild` が `dataValidations` を OOXML へ出していない問題を修正する
  - [x] `ExcelIoWorksheetBuild` / parse が `freezePane` と formula cell を OOXML へ出し入れできるようにする
  - [x] `ExcelIoStylesBuild` が実際の `fillColor` / alignment / number format / wrap / border の組み合わせを十分に反映せず、ほぼ固定 style へ潰している問題を修正する
  - [x] `ProjectXlsxExport*` は列幅、boolean data validation、boolean option sheet、editable cell styling、sheet theme、Project sheet の Settings section / merged range を補強済み
  - [x] テストは byte size / decode だけでなく、zip entry、worksheet XML、styles XML、data validation、freeze pane、主要セル style を確認する
    - 2026-04-21 時点で `ExcelIoTest` が OOXML zip entry / worksheet XML / data validation / freeze pane / formula を確認し、`ProjectXlsxTest` が主要セル style、data validation、sheet theme、Settings section / merged range を確認済み

### メンテナンス記録

- 2026-08-09 | miku-project-java | canonical naming migration (#37)
  - baseline: default branch `devel`, target HEAD `482bf175515daed233b4be3254515677a360e473`, latest Release `v0.8.3.4`, vendored upstream `245deaa99d6d2ba970969a9359ce003386da3472`
  - applied: GitHub repository rename was performed by a human; local `origin` and `mikuproject` remotes now use `igapyon/miku-project-java` and `igapyon/miku-project`
  - applied: upstream naming commit `a3385a5` is followed for the Java public CLI/version text, AI JSON spec ID/text, and built-in sample project name
  - applied: upstream content commits `8c5043f` / `7e5d283` were reviewed. Java's standard ZIP reader already handles DEFLATE, XML sanitizer behavior already matches valid Unicode / invalid XML character handling, and deterministic Java string ordering already matches the upstream ordering change. Added focused regressions for XML whitespace / controls and DEFLATE ZIP input.
  - applied: `miku-ms-office-core-java` `v0.6.0` is vendored as a checksum-fixed Maven repository artifact. `ExcelIoZip.unpackZip` now delegates package reading to `ZipPackage`; product ZIP write stays local to preserve Node report artifact byte parity.
  - applied: Java companion version and public CLI version are aligned to the current Node upstream release `0.12.0`.
  - compatibility: public names move to `miku-project`; `jp.igapyon.mikuproject`, `MikuprojectCli`, `vendor/mikuproject/`, and established exchange-format identifiers such as `mikuproject_workbook_json` remain unchanged
  - maintenance uplift: the approved explicit Java build-JDK item is already satisfied by the Release workflow; checksum and staged-artifact controls remain a separate change because this migration renames public artifacts

- 2026-08-06 | mikuproject-java | Java CLI runtime / repository operation | `workplace/` の除外規則を `workplace/.gitkeep` のみ追跡する規約へ明確化
  - 適用: `.gitignore` を `workplace/*` と `!workplace/.gitkeep` に更新し、README の運用説明も同期
  - 適用: Release workflow は Temurin 21 の明示 build、Maven cache、Maven の `help:evaluate` による version 取得、Java 8 runtime smoke へ更新
  - 検証: `MIKUPROJECT_RUN_NODE_PARITY=true mvn -B test` を実行し、129 tests を成功（skip 0）
  - 次: GitHub Actions 上で release workflow を初回実行する際、Java 8 runtime smoke の実行ログを確認する

- 2026-08-06 | mikuproject-java | miku-soft standard alignment | shared Node CLI contract と Java extensions の分離を明文化
  - 適用: 標準参照、upstream snapshot、CLI mapping、CONTRIBUTING / CONTRIBUTORS / THIRD_PARTY_NOTICES を追加。共有標準のコピー文書は削除
  - 適用: shared JSON output / diagnostics / workbook projection の Node-compatible serialization を追加し、Java-only commands と legacy runtime behavior を対応表へ明記
  - 検証: `MikuprojectNodeParityTest` を stdout / stderr / exit code comparison へ拡張し、report artifact bytes と合わせて通過

### 今週

- [x] Java 版の対象を `CLI` 中心に固定し、対象範囲を文書で揃える
  - [x] `README.md` / `docs/remaining-migration-items.md` / `TODO.md` で、Java 版の対象範囲を `CLI` 中心に揃える
  - [x] `対応済み` `保守確認` の 2 区分で、現時点の領域分類を定義する
  - [x] `straight conversion` を原則とし、Java-first 再設計は後段で扱う方針を文書で固定する
- [x] core / CLI / report の 3 系統で、移植計画を作る
  - [x] core 系について、upstream file 単位で `対応済み / 保守確認 / 保留` を棚卸しする
  - [x] CLI 系について、既存 command と upstream command/導線との差分を棚卸しする
  - [x] report 系について、`lossless 交換形式` と `人向け派生出力` の役割を切り分けたうえで保守確認観点を棚卸しする

### 次

- [x] upstream 更新追随時の確認単位を、Java 側でも対応ファイル単位で辿れるようにする
- [x] Node 版 upstream から継承する仕様と、Java 版でまだ保留の仕様を切り分ける

### 後で

- [x] Java CLI entry の役割を最小 entrypoint から全機能 entrypoint へ拡張する段取りを決める

## 近い順

### 1. Core の精度を上げる

- [x] `null` / 未設定 / 空文字 / `0` の扱いを整理する
- [x] parse / import / validate 系のエラー返却方針を決める
- [x] Node 側 options object を Java でどう表現するか決める
- [x] package 依存方向とモジュール境界を整理する

### 2. Java 側の配置を固める

- [x] Java 側は過度な Java 流再編を避け、upstream のファイル境界を尊重して分割する
- [x] upstream 1 ファイルに対して Java 側で複数クラスへ分ける場合も、追跡しやすい命名規則を決める
- [x] Java 側の package 構成案を決める

### 3. CLI / runtime を整える

- [x] text / bytes / stream の I/O API 方針を決める
- [x] 日付時刻の型とタイムゾーン方針を決める
- [x] 数値型の方針を決める
- [x] 文字コードを UTF-8 前提でどこまで明示するか決める

## テスト

- [x] upstream 比較を前提にしたテスト方針を決める
- [x] `src/test/java/` を用意する
- [x] JUnit Jupiter を Maven に追加する
- [x] upstream 対応を意識したテスト class 命名規則を決める
- [x] upstream の主要テストと Java 側テストの対応表を作る
- [x] upstream fixture 比較を、core / workbook / report へ段階拡張する

## ドキュメント

- [x] `README.md` に Java 版の目的を追記する
- [x] upstream 対応方針を `README.md` に追記する
- [x] `README.md` と `docs/remaining-migration-items.md` の進捗前提を定期的に同期する
- [x] Java 版移植の現在地を、core 完了率と CLI/report 完了率で分けて示す

## 完了済みメモ

- [x] `vendor/mikuproject` に Node.js upstream を subtree で保持する
- [x] `workplace/` は Git 管理外のローカル作業スペースとして扱う
- [x] Java package の基底名を `jp.igapyon.mikuproject` にする
- [x] Java のターゲットを `1.8` にする
- [x] Maven ベースで開始する
- [x] Node.js upstream の構成を尊重し、Java 側の責務分割も対応関係を追いやすくする
- [x] Java 版の初期移植の目的を明文化する
- [x] `MS Project XML` と `ProjectModel` の位置づけを Java 版でも定義する
- [x] Java 版 `ProjectModel` の責務と範囲を決める
- [x] 初期移植で扱う要素範囲を決める
- [x] CLI を初期移植に含めるか決める
- [x] `ProjectModel` を `Map` ベースで持つか、POJO で持つか方針を決める
- [x] ソースヘッダーにライセンス表記を追加する
