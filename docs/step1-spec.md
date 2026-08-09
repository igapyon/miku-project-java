# STEP1 Spec

## 現在の位置づけ

この文書は、Java 版 `miku-project` の STEP1 で採用した設計判断と current implementation の基準をまとめる文書である。
冒頭の `STEP1 のスコープ` は初期実装の最小範囲を示すものであり、現在の未実装リストではない。

現在は、XML round-trip を土台に、workbook JSON / XLSX、AI JSON、patch JSON、CLI、SVG / Markdown / Mermaid などの周辺導線も Java 側に実装済みである。
現フェーズでは新規機能追加ではなく、既存実装がこの文書の判断基準と upstream 対応表に沿っているかを確認する。

## 目的

Java 版 `miku-project` の STEP1 では、`MS Project XML` の意味的ラウンドトリップを成立させることを目的とする。

ここでいう意味的ラウンドトリップとは、少なくとも次を満たすことである。

- `MS Project XML` を読める
- 必要な情報を内部モデルへ落とせる
- 内部モデルから `MS Project XML` を再生成できる
- 再生成した `MS Project XML` を Java 版 `miku-project` 自身で再読込できる
- 主要フィールドが壊れず往復できる

目標は XML テキストの完全一致ではなく、意味的に往復できることである。

## 基本方針

Java 版でも、Node.js 版 upstream の思想をそのまま引き継ぐ。

- `MS Project XML` を意味の基軸として扱う
- `ProjectModel` を内部中立表現として扱う
- Java 版独自の思想へ作り替えない
- upstream と思想がずれる設計は、移植性と追随性を損なうため避ける

## STEP1 初期のスコープ

STEP1 初期に扱う最小スコープは次のとおり。

- `MS Project XML -> ProjectModel`
- `ProjectModel -> MS Project XML`

初期段階では、次は後段とした。
現在は主要導線が Java 側に存在するため、以下は追加リストではなく、XML round-trip を土台に後続で広げた領域として扱う。

- `XLSX`
- workbook JSON
- AI JSON
- patch JSON
- CLI
- SVG / Markdown / Mermaid などの派生出力

## `ProjectModel` の位置づけ

`ProjectModel` は、Java 版でも内部中立表現とする。

- 正本は `MS Project XML`
- `ProjectModel` は import / export のあいだをつなぐ内部表現
- `ProjectModel` 自体を外部交換形式の中心にしない

この考え方は Node.js 版 upstream と揃える。

## データ構造方針

STEP1 では、コア部分から Java の POJO で表現する。

- まずはコア部分を POJO で定義する
- 未確定な領域を無理に広げない
- 初期段階では、広い汎用構造へ逃がすことより、Node.js upstream の責務に追随しやすい形を優先する

詳細な型設計は初期移植時点では後続検討事項だったが、少なくとも STEP1 では「まず XML 往復に必要な範囲を POJO として切る」方針を採った。

## STEP1 で優先して扱う対象

STEP1 の `ProjectModel` では、少なくとも次の要素群を優先対象とする。

- `Project`
- `Tasks`
- `Resources`
- `Assignments`
- `Calendars`

ただし、各要素のどのフィールドまで初期移植で扱うかは、別途詳細化した。

## 非目標

STEP1 の時点では、次を目標にしない。

- XML の完全一致再現
- Java 側独自アーキテクチャへの作り替え
- Java 版だけの高度な抽象化
- Node.js 版 upstream から独立した仕様再設計

## 初期移植時点の詳細化項目

- `ProjectModel` の初期移植フィールド範囲
- XML import の責務分割
- XML export の責務分割
- 日付時刻の扱い
- 数値型の扱い

これらの実装後方針は、この文書の後続節、`docs/remaining-migration-items.md`, `docs/upstream-class-mapping.md`, `docs/upstream-test-mapping.md` を正本として扱う。

## `null` / 未設定 / 空文字 / `0` の扱い

STEP1 の current implementation では、値の有無を次のように扱う。

### 基本原則

- `null` は「値なし」を表す内部表現として使う
- 空文字は `String` 項目では保持しうるが、import の入口では `null` 相当に寄せることがある
- `0` は数値として意味を持つ場合にだけ使い、`値なし` の代用にしない
- XML / workbook などの外部表現で欠落している値は、Java 側では原則 `null` へ寄せる

### XML import

- XML text 取得では、要素がない場合は空文字を返す
- そのうえで、各 field への格納時に `isEmpty()` を見て `null` を避けるか、値ありのときだけ代入する
- boolean は `1` / `true` を `true`、それ以外を `false` として読む初期方針を採る
- 数値は parse 失敗時に既定値へ倒す

意味:

- DOM helper は `null` を返すより、空文字や既定値で呼び出し側を単純化する
- model へ入れる段階で「未設定」と「値あり」を切り分ける

### workbook JSON import

- blank なセルは `null` として扱う
- boolean は `○ / 1 / true` を `true`、`ー / 0 / false` を `false` とし、それ以外は `null` とする
- integer / double は blank や parse 失敗時に `null` とする
- 日付文字列は blank を `null` に寄せ、日付のみの場合は `start` なら `T09:00:00`、`finish` なら `T18:00:00` を補う

意味:

- workbook は編集系入口なので、`未入力` を `null` で保持する
- `0` と blank を混同しない

### workbook / report export

- 値なしは原則 `null` で出力する
- ただし人向け出力では、見た目のために `-` や空文字へ置き換えることがある
- boolean の表示専用出力では `○ / ー` を使う
- count 表示や件数表示では `0` を有効値として出す

意味:

- lossless 寄りの交換形式では `null` を維持する
- 人向け派生出力では可読性のための置換を許容する

### 実装判断の基準

今後の実装でも、次を基準に揃える。

- 内部 model で `値なし` を表したいときは、`0` や空文字ではなく `null` を優先する
- parse helper の戻り値が既定値なのか `null` なのかは、入力段階の責務に合わせて決める
- CLI や report の表示都合で置き換えた値を、そのまま内部正本へ持ち込まない
- `0` が有効値になる項目では、blank と `0` を必ず区別する

## parse / import / validate 系のエラー返却方針

STEP1 の current implementation では、`parse` / `import` / `validate` の返し方は次の 3 層に分ける。

### 1. 入口契約違反は例外

対象:

- 入力の型が違う
- 必須 top-level field がない
- format / version / mode が不正
- XML / JSON / CSV 自体が構文として読めない

扱い:

- `IllegalArgumentException` を投げる
- `IOException` のような lower-level 例外は、必要に応じて `IllegalStateException` などへ包む
- この段階では partial result を返さない

例:

- workbook JSON validate の `format` / `version` / `sheets` 不正
- patch JSON validate の `operations` 不正
- external import の `format` / `mode` / `baseModel` 契約違反
- XML / CSV の構文不正

意味:

- API 呼び出し側の前提が壊れている場合は、warning 集約へ落とさず即失敗させる

### 2. 意味検証は issue / warning を返す

対象:

- model は組み上がっているが、参照整合や値域に問題がある
- 実行継続はできるが、結果品質に懸念がある

扱い:

- `validate` 系は例外ではなく `issue` の配列を返す
- severity は `error` / `warning` のように data として持つ
- `error` issue があっても、`validate` 自体は原則 throw しない

例:

- `validateProjectModel` の UID 重複
- 参照先 calendar / task 不整合
- 日付前後関係の不正
- 推奨値域から外れる数値

意味:

- `validate` は「読めるか」ではなく「妥当か」を返す責務とする

### 3. 初期移植で継続可能な import 差分は result に warnings を積む

対象:

- 文書の骨格は妥当で読み進められる
- ただし未知列、未知 sheet、未対応 op、局所的な値不正などがある

扱い:

- `ImportResult` / `ValidationResult` / `CoreApiImportResult` の `warnings` へ積む
- 継続可能な場合は model や document を返す
- 補正や無視を行った場合は、可能なら `changes` にも反映する

例:

- workbook JSON の未知 sheet / 未知列
- patch JSON の未対応 op
- patch import 中の局所的な update 失敗

意味:

- 初期移植では、局所的な不整合を 1 件で全体失敗にしない
- ただし「結果が使える範囲でのみ」継続する

### 実装判断の基準

今後の実装でも、次を基準に揃える。

- 入力契約を満たさない場合は `IllegalArgumentException`
- 意味検証の結果は throw ではなく `issue` / `warning` として返す
- 継続不能な構文不正と、継続可能な意味不整合を混同しない
- CLI 層は core が返した例外と warning をそのまま区別して扱う
- 新しい API を追加するときは、`throw する条件` と `result に積む条件` を先に決める

## Node 側 options object を Java でどう表現するか

STEP1 の current implementation では、Node.js 側の options object は Java では `public field` を持つ素朴な POJO として表現する。

### 基本原則

- builder pattern は初期移植では使わない
- getter / setter 必須の JavaBean に寄せすぎない
- upstream の option 名と意味を追いやすい field 名を優先する
- option がない呼び出しのために、`options なし overload` と `options あり overload` を併置してよい

### 現在の代表形

- report 系は `WbsMarkdownOptions` / `NativeSvgOptions` / `WbsExportOptions`
- import 系の複合入力は `ExternalImportInput` / `ExternalImportSource`

共通する形:

- `public static class` または独立 class で定義する
- field は `public` のまま持つ
- `List` field は生成時に空 list で初期化する
- optional な scalar は `Integer` / `Boolean` / `String` など nullable な wrapper / reference 型で持つ

### 値の持ち方

- `null` は「未指定」を表す
- `Boolean.TRUE` / `Boolean.FALSE` は明示指定を表す
- collection は `null` より空 collection を優先する
- CLI で未指定だった option は `null` または空 collection のまま core へ渡す

意味:

- Node.js の「property がない」状態を、Java では nullable field で近似する
- list 系は `null` チェックより空 collection で扱うほうが初期移植を単純化できる

### 配置の基準

- 特定機能だけで使う option は、その機能 class の近くに置く
- 返り値や入力束ねの小さな構造体も、利用箇所の近くに置く
- 複数 subsystem をまたぐ共通 request object を早い段階で抽象化しすぎない

例:

- `WbsMarkdownOptions` は `WbsMarkdown` 配下
- `NativeSvgOptions` は `WbsSvg` 配下
- `ExternalImportInput` は `CoreApiExternalImport` 配下

### CLI との境界

- CLI は文字列引数を parse して options object を組み立てるだけに留める
- core 側は options object の意味を解釈し、既定値補完や正規化を行う
- CLI 専用都合の parse helper を core の option class へ持ち込まない

### 今後の実装判断の基準

- upstream が object 引数 1 個で受ける箇所は、Java 側でもまず 1 個の options/request POJO で受ける
- 引数が少数でも upstream 追跡性が高いなら、無理に個別引数へ展開しない
- 初期移植では immutable 化や fluent API 化を優先しない
- field 名は upstream 語彙を優先し、Java 側独自の一般名へ置き換えない

## package 依存方向とモジュール境界

STEP1 の current implementation を踏まえると、Java 側の package 依存は次の向きで整理する。

### 基本の層

1. `model`
2. `msprojectxml` / `projectworkbookjson` / `projectpatchjson` / `projectxlsx` / `excelio` / `wbsdateband` / `markdownescape`
3. `wbsmarkdown` / `wbssvg` / `wbsxlsx`
4. `coreapi`
5. `cli`

意味:

- `model` は最下層の内部表現
- import / export / workbook / patch / xlsx codec 群は中核機能
- report 群は `ProjectModel` を読む派生出力
- `coreapi` は各機能 package を束ねる公開面
- `cli` は最上位 entrypoint

### package ごとの役割

- `model`
  - `ProjectModel` と関連 POJO
  - 他 package へ依存しない
- `msprojectxml`
  - XML / CSV / Mermaid / AI view / validation / normalize
  - `model` を中心に扱う
- `projectworkbookjson`
  - workbook JSON validate / export / import
  - `model` と workbook schema を扱う
- `projectpatchjson`
  - patch JSON validate / import
  - `model` への差分適用を扱う
- `projectxlsx`
  - workbook-like な中間表現と project xlsx import / export
  - `projectworkbookjson` と `excelio` の橋渡しになる
- `excelio`
  - xlsx zip / worksheet / styles の codec
  - `projectxlsx` の workbook-like を入出力する
- `wbsdateband` / `markdownescape`
  - report から使う小さな支援 package
- `wbsmarkdown` / `wbssvg` / `wbsxlsx`
  - `ProjectModel` を読む派生 report 出力
- `coreapi`
  - lower package を束ねる公開 facade
- `cli`
  - 引数 parse、標準入出力、ファイル I/O、終了コード

### 許容する依存

- `model` <- 全 package
- `projectxlsx` <- `excelio`, `wbsxlsx`, `coreapi`
- `wbsdateband` <- `wbsmarkdown`, `wbssvg`, `wbsxlsx`
- `msprojectxml` <- `coreapi`, `cli`, 一部 report facade
- `projectworkbookjson` / `projectpatchjson` <- `coreapi`, `cli`, `projectxlsx`
- report package <- `coreapi`, `cli`
- `coreapi` <- `cli`

### 避ける依存

- `model` から他 package への逆依存
- `coreapi` への下位 package 依存
- `cli` への他 package 依存
- report package から `cli` / `coreapi` への逆依存
- `excelio` から `model` や report への直接依存

### current implementation を踏まえた補足

- `MsProjectXml` は現在、report 出力の convenience facade も兼ねており、`wbsmarkdown` / `wbssvg` へ依存している
- これは現状の公開面としては許容するが、`model` や `excelio` のような下位 package へ同種の依存を広げない
- package 境界の判断では、まず `upstream file` ごとの責務追跡を優先し、層の純化だけを目的に大きく分解し直さない

### 今後の実装判断の基準

- 新しい class は、まず upstream 対応 file の近い package へ置く
- 汎用化したくなっても、複数 package から本当に共有されるまでは移動しない
- facade を足す場合も、下位 package の責務を吸い上げすぎない
- import/export の正本ロジックと CLI 専用都合の処理を同じ package に混ぜない

## Java 側は過度な Java 流再編を避け、upstream のファイル境界を尊重して分割する

STEP1 の current implementation では、Java 側の配置は「Java らしい理想形」よりも「upstream file の責務を追えること」を優先する。

### 基本原則

- upstream 1 file に主要責務 1 つがあるなら、Java 側でもまず 1 class を第一候補にする
- 既存 package へ吸収したくなっても、対応元の upstream file が見えなくなるなら吸収しない
- utility 抽出は、複数箇所から本当に共有されるまで急がない
- facade と実処理を分ける場合も、元の upstream file との対応関係を説明できるようにする

### 避けること

- Java 都合だけで `service`, `domain`, `infra`, `util` のような抽象 package へ全面再編すること
- upstream では 1 file でまとまっている責務を、初期段階から細かい層へばらまくこと
- import / validate / render / io の語彙を、Java 独自の一般名へ置き換えること
- 小さな再利用のために、追跡性を壊す共通基底 class や汎用 helper package を増やすこと

### 例外として許容する分割

- Java の 1 file サイズや可読性の制約で、責務の近い補助 class へ分ける場合
- upstream の data 構造 1 file を、Java では複数 POJO へ分ける場合
- workbook-like や parse result など、Java で小さな構造体が必要な場合

この場合も、分割理由は `Java で必要な最小限` に留める。

## upstream 1 ファイルに対して Java 側で複数クラスへ分ける場合の命名規則

upstream 1 file を Java で分ける場合も、元の file 名を起点にして追跡しやすい命名を維持する。

### 基本規則

- 中心 class は upstream file 名に最も近い名前を持つ
- 補助 class は中心 class 名を prefix にして責務語を後ろへ付ける
- suffix は `*Util`, `*Helpers`, `*Import`, `*Export`, `*Project`, `*Entities`, `*Calendars` のように、役割が読めるものに限る
- `Manager`, `Processor`, `Handler`, `Service` のような一般名は、upstream 対応が弱くなるなら避ける

### 既存実装に沿った例

- `project-xlsx-import.ts` -> `ProjectXlsxImport`, `ProjectXlsxImportProject`, `ProjectXlsxImportEntities`, `ProjectXlsxImportCalendars`, `ProjectXlsxImportUtil`
- `project-xlsx-export.ts` -> `ProjectXlsxExport`, `ProjectXlsxExportProject`, `ProjectXlsxExportEntities`, `ProjectXlsxExportCalendars`, `ProjectXlsxExportUtil`
- `project-patch-json-*.ts` -> `ProjectPatchJsonCore`, `ProjectPatchJsonTasks`, `ProjectPatchJsonLinks`, `ProjectPatchJsonEntities`, `ProjectPatchJsonUpdates`, `ProjectPatchJsonUtil`
- `types.ts` -> `ProjectModel`, `TaskModel` など複数 POJO

### 命名判断の基準

- class 名を見て upstream file 名が逆引きできること
- 同じ prefix の class 群は、同じ upstream file family に属すると分かること
- 分割後も、`docs/upstream-class-mapping.md` に無理なく記載できること

## Java 側の package 構成案

現在の実装を踏まえた package 構成案は次のとおり。

- `jp.igapyon.mikuproject.model`
  - `types.ts` 相当の POJO 群
- `jp.igapyon.mikuproject.msprojectxml`
  - `msproject-xml*`, `msproject-csv`, `msproject-mermaid`, `msproject-ai-views`
- `jp.igapyon.mikuproject.projectworkbookjson`
  - workbook JSON schema / validate / export / import
- `jp.igapyon.mikuproject.projectpatchjson`
  - patch JSON validate / import
- `jp.igapyon.mikuproject.projectxlsx`
  - workbook-like model と project xlsx import / export
- `jp.igapyon.mikuproject.excelio`
  - OOXML-like xlsx byte codec
- `jp.igapyon.mikuproject.wbsdateband`
  - report 共通の日付帯 helper
- `jp.igapyon.mikuproject.markdownescape`
  - markdown 出力補助
- `jp.igapyon.mikuproject.wbsmarkdown`
  - WBS Markdown
- `jp.igapyon.mikuproject.wbssvg`
  - WBS SVG
- `jp.igapyon.mikuproject.wbsxlsx`
  - WBS XLSX
- `jp.igapyon.mikuproject.coreapi`
  - Java 側公開 facade
- `jp.igapyon.mikuproject.cli`
  - command-line entrypoint

### 構成案の原則

- package 名は upstream の責務名を自然に Java へ読み替えたものを使う
- package を増やす基準は「upstream file family が独立しているか」で判断する
- report 系は `wbs*` family ごとに分け、早い段階で 1 package に混ぜない
- `coreapi` と `cli` は公開面であり、正本実装 package とは分ける

### 今後の実装判断の基準

- 新規 package を作る前に、対応する upstream file family が明確か確認する
- 既存 package へ置くか迷ったら、`docs/upstream-class-mapping.md` で最も自然に説明できる位置を優先する
- Java だけの都合で package 横断の大規模再編をしない

## text / bytes / stream の I/O API 方針

STEP1 の current implementation では、I/O は `text` と `bytes` を正本にし、`stream` は主に CLI や zip 実装の内部に留める。

### 基本原則

- XML / Markdown / Mermaid / SVG / JSON spec / AI JSON text は `String` で扱う
- xlsx workbook binary / zip bundle / zip entry data は `byte[]` で扱う
- `InputStream` / `OutputStream` は公開 API の中心にせず、内部実装や Java 標準 API 接続でだけ使う
- file path / file read / file write は `cli` 層へ留める

### 現在の整理

- `MsProjectXml.importFromXml` は `String` を受ける
- `CoreApiAiJson.parseAiJsonText` / `importAiJsonText` は `String` を受ける
- `CoreApiExternalBinary.importXlsx` は `byte[]` を受ける
- `CoreApiWorkbookXlsx.encodeWorkbook` / `decodeWorkbook` は `byte[]` を返す / 受ける
- `CoreApiReport.ReportEntry` は `byte[] data` を持つ
- CLI は `Files.readAllBytes` / `Files.write` と `PrintStream` を使って、text/bytes とファイル・標準出力を橋渡しする

### 境界の引き方

- core package は `Path` や `Files` に依存しない
- text/binary の判定は format ごとに固定する
- text を一度 `byte[]` に変換するのは、境界でのみ行う
- UTF-8 encode/decode helper は `excelio` 側の共通 helper を使ってよい

### stream の扱い

- zip pack/unpack や XML parser 接続では `ByteArrayInputStream` / `ByteArrayOutputStream` を内部利用してよい
- ただし public API は、初期移植では `InputStream` / `OutputStream` overload を増やさない
- stream API が必要になっても、まずは CLI/runtime の要求として追加を検討する

### 今後の実装判断の基準

- upstream が text document を扱う箇所は、Java 側でもまず `String` を正本にする
- runtime artifact や archive のような binary 境界では `byte[]` を使う
- file path や stream を core 正本 API に持ち込まない
- CLI は format ごとの text/bytes 境界変換だけを担当し、意味解釈は core へ残す

## 日付時刻の型とタイムゾーン方針

STEP1 の current implementation では、日付時刻の正本は `String` で保持し、比較・補正・日付帯計算の局所ロジックでだけ `java.time` または `Date/Calendar` を使う。

### 基本原則

- `ProjectModel` の日付時刻 field は `String` のまま保持する
- XML / workbook / patch / AI view との境界でも、まずは文字列表現を維持する
- date-only と date-time を混在して受けうるが、必要に応じて局所的に正規化する
- timezone を含まない local date-time を初期移植の主表現として扱う

### 現在の扱い

- XML codec は `StartDate` / `FinishDate` / `CurrentDate` などを `String` のまま import / export する
- workbook import や patch import では、date-only 入力に対して `start` なら `T09:00:00`、`finish` なら `T18:00:00` を補う
- report 系は日付帯計算や表示で `YYYY-MM-DD` 部分を主に使う
- validate では比較のために `Instant` / `OffsetDateTime` / `LocalDateTime` / `LocalDate` の順で parse を試みる

### timezone の扱い

- offset 付き時刻や `Z` 付き時刻は、validate の比較では解釈してよい
- ただし model 正本へ timezone 正規化を書き戻さない
- timezone を持たない `LocalDateTime` は、比較時には `UTC` 基準の instant へ写像して扱う
- date-only は日付境界比較のために `00:00:00` 相当として扱う

意味:

- 初期移植では timezone-aware な統一型へ全面変換しない
- 文字列表現の保持を優先しつつ、比較や検査で必要な最低限の解釈だけ行う

### 日付帯計算と表示

- WBS dateband や weekly/monthly 表示は `YYYY-MM-DD` 単位で扱う
- 時刻成分は表示範囲計算の正本に使わない
- holiday / non-working day 判定も date-only ベースで扱う

### 今後の実装判断の基準

- upstream が文字列で持つ日付時刻は、Java 側でもまず `String` を維持する
- 比較や補正の helper では `java.time` を使ってよい
- report や dateband では date-only ロジックを優先し、時刻や timezone を持ち込まない
- timezone を含む入力を受けても、初期移植では意味比較に使うだけで、正本の全面正規化は行わない

## 数値型の方針

STEP1 の current implementation では、数値は `Integer` / `Double` / `String` を用途ごとに分け、`null` を保持できる wrapper 型を優先する。

### 基本原則

- `値なし` を保持したい model field では primitive ではなく wrapper 型を使う
- 件数、列挙コード、整数 percent、outline level のような整数値は `Integer` を使う
- units、cost、hours のように小数を含みうる量は `Double` を使う
- duration / work / rate / slack のように upstream が文字列表現を前提にする値は `String` を維持する

### `Integer` を使うもの

- ID ではなく、数として解釈する設定値やコード値
- 例:
  - `minutesPerDay`, `minutesPerWeek`, `daysPerMonth`
  - `outlineLevel`, `priority`, `constraintType`
  - `percentComplete`, `percentWorkComplete`
  - `type`, `unit`, `sequence`, `level`

意味:

- XML や workbook では未設定を持ちうるため、`Integer` で `null` を保持する
- percent や code 値は初期移植では整数として扱う

### `Double` を使うもの

- 量や金額で小数を許容する値
- 例:
  - `units`
  - `maxUnits`
  - `cost`, `actualCost`, `remainingCost`
  - `costPerUse`
  - `plannedDurationHours`, `lagHours`

意味:

- import では `Number` も文字列数値も `Double` へ寄せる
- workbook / patch では整数入力でも `Double` として受けてよい

### `String` を維持するもの

- ISO 8601 duration や MS Project 由来の text 形式を持つ値
- 例:
  - `duration`
  - `work`, `actualWork`, `remainingWork`, `overtimeWork`
  - `standardRate`, `overtimeRate`
  - `startVariance`, `finishVariance`, `workVariance`, `totalSlack`, `freeSlack`

意味:

- これらは初期移植では単純な数値へ正規化しない
- 表示・round-trip・upstream 追跡性を優先して文字列で持つ

### parse 時の扱い

- XML helper は `parseNumber` / `parseDouble` で既定値へ倒す箇所がある
- workbook / patch import では blank や parse 失敗時に `null` を返す
- workbook から `Integer` を読むときは、小数入力を `Math.floor` 相当で整数化している箇所がある

### 今後の実装判断の基準

- `null` を意味として保持する field では wrapper 型を優先する
- upstream が text で扱う duration/work/rate は数値型へ急いで変換しない
- 数値項目を新設するときは、`整数コード`、`小数量`、`文字列表現の量` のどれかを先に決める
- CLI の parse で整数に見えても、core model 側の意味が `Double` なら `Double` のまま渡す

## 文字コードを UTF-8 前提でどこまで明示するか

STEP1 の current implementation では、text の入出力は UTF-8 を既定ではなく明示前提で扱う。

### 基本原則

- XML / JSON / Markdown / Mermaid / SVG / spec text は UTF-8 で読む
- text を `byte[]` へ変換するときも UTF-8 を明示する
- xlsx zip entry 内の XML も UTF-8 前提で encode / decode する
- binary artifact 自体は文字コード対象外とし、`byte[]` のまま扱う

### 現在の扱い

- CLI の `readText` / `writeXml` は `StandardCharsets.UTF_8` を明示している
- `CoreApiAiJson` の spec 読み込みも UTF-8 を明示し、実行時には classpath / JAR 内リソースから読む
- `ExcelIoUtil.encodeUtf8` / `decodeUtf8` を text-binary 境界の共通 helper として使っている
- OOXML-like XML builder は XML 宣言でも `encoding="UTF-8"` を出している
- `XlsxWorkbookCodec` の text 部分も UTF-8 で encode / decode する

### 明示する場所

- `Files.readAllBytes` -> `new String(bytes, UTF_8)` の境界
- `String` -> `getBytes(UTF_8)` の境界
- XML 宣言文字列
- text entry を zip へ詰める箇所

### 明示しなくてよい場所

- すでに `String` として組み上がった内部処理
- `byte[]` をそのまま受け渡す binary API
- workbook zip や report zip のような binary container そのもの

### 今後の実装判断の基準

- text file / text entry の read/write では、デフォルト charset に依存しない
- 新しい text-binary 境界では、`StandardCharsets.UTF_8` または `ExcelIoUtil.UTF_8` を必ず明示する
- binary と text が混在する API では、どちらが正本かを先に決め、text 側だけ UTF-8 を明示する
