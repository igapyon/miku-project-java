# Miku Straight Conversion Guide

Document version: `2026-04-22`

## 目的

この文書は、miku シリーズで Node.js / TypeScript 系 upstream を Java へ移すときの `straight conversion migration` 方針をまとめるためのメモである。

各リポジトリでは、この文書を共通原則として置き、実装の正本や current status はそのリポジトリ固有の `README.md`, 進捗管理文書, upstream 対応表, test 対応表を参照する。
この文書は、それらの前提にある「なぜこの移し方をしたのか」を固定することを目的とする。

加えて、この文書は、別の miku シリーズで straight conversion 作業を再現できる程度に、進め方と成果物の粒度を揃えることも目的とする。

## 使い方

この文書は、単なる方針集としてではなく、straight conversion を開始して保守フェーズへ入るまでの手順書として使う。
ただし、この文書は共通原則を固定するためのものであり、個別リポジトリの進捗や current status の正本にはしない。

基本の使い方は次のとおり。

1. `基本姿勢` と `なぜ straight conversion を選ぶか` で判断基準を確認する
2. `当初固定すること` と `開始時チェックリスト` を埋める
3. `作業の再現手順` に沿って棚卸し、対応付け、実装、検証を進める
4. 各段階で `成果物として残すもの` を文書と test に反映する
5. 実装後は `保守フェーズでの扱い` と `docs と追随運用` に従って運用する

生成AIにこの文書を読ませる場合は、次の読み方を前提にする。

1. `当初固定すること` は判断理由を含む固定方針として読む
2. `開始時チェックリスト` は実行前の短い確認として読む
3. `作業の再現手順` は実際の作業順序として読む
4. 後続の個別方針は、実装中に迷ったときの判断基準として読む

この文書には、同じ方針が固定方針、チェックリスト、再現手順に重複して現れることがある。
これは生成AIが途中の節だけを参照しても重要な前提を落としにくくするためであり、単純な重複として削りすぎない。

文中の強さは、次のように読み分ける。

- `固定する`, `固定`, `必ず`, `対象外`, `しない` は、その miku Java 版で先に揃える固定方針として読む
- `基本`, `原則`, `優先する` は、迷ったときの既定方針として読む
- `してよい`, `許容する`, `選んでよい` は、upstream 追随性を壊さない範囲で使える許容パターンとして読む
- `傾向`, `知見`, `例`, `このリポジトリで見えている` は、他の miku シリーズへそのまま強制せず、判断材料として読む
- `このリポジトリで固定した` は、`mikuproject-java` での適用例であり、共通原則そのものとは区別して読む

## 基本姿勢

ここでいう Java 化は、Java 向け再設計を先に行う移植ではない。
最初に目指すのは、Node.js / TypeScript upstream を Java 上で追跡可能な形へ移す `straight conversion` である。

ここでいう `straight conversion` は、単純な機械変換を意味しない。
重視したのは次の点である。

- upstream の file 境界と責務分割を追いやすいこと
- upstream の語彙や命名を Java 側でも辿りやすいこと
- upstream 更新時に、どこを見比べればよいか説明できること
- Java 側だけ別製品のような再設計に寄りすぎないこと

したがって、移植の初期段階では、Java らしい抽象化や再編成の綺麗さよりも、upstream 追随性を優先した。

## なぜ straight conversion を選ぶか

多くの miku シリーズは、upstream 側に明確な file 境界、責務分割、語彙、入出力契約を持つ。
この構造を Java 側で最初から別アーキテクチャへ作り替えると、次の問題が起きやすい。

- upstream のどの file を受けた実装か分かりにくくなる
- upstream 更新時に差分の影響範囲を読み解きにくくなる
- 移植というより再実装になりやすい
- Java 側で見つかった差分が仕様差なのか設計差なのか切り分けにくくなる

そのため、まずは upstream を忠実に辿れる形で Java 版を成立させ、その後に必要なら Java 側の再整理を考える段取りを採った。

## 当初固定すること

straight conversion を始める時点で、少なくとも次は先に決めておく。

- straight conversion 元 upstream が、移植開始に耐える程度に整理済みか確認する
- target Java compatibility は source / binary ともに `1.8` 対応へ固定する
- build tool は `Maven` に固定する
- test framework は `JUnit Jupiter` に固定する
- primary test entrypoint は `mvn test` に固定する
- runtime packaging は single fat jar を固定する
- distribution zip は処理対象に応じて採否を決める
- repo root に `workplace/` を作成し、`workplace/.gitkeep` だけを Git 管理対象にする
- `workplace/.gitkeep` 以外の `workplace/` 配下ファイルは Git 管理対象外にする
- straight conversion の過程では新機能を追加しない
- Java-first の再設計を先に行わない
- upstream の file 境界と責務分割を尊重する
- `upstream file -> Java class 群` の対応を追えるようにする
- Java package は `jp.igapyon.<project>` を基準にし、下位 package は upstream Node 側 file 名と責務から決める
- class 名は Java 規約へ読み替えるが、upstream 語彙を維持する
- upstream の関数名が `camelCase` なら、Java 側メソッド名も原則そのまま引き継ぐ
- GUI は持ち込まず、Java 側の対象は CLI runtime を基本とする
- CLI を持つ upstream では、Java 側 CLI は Node 版 CLI のインタフェースをできるだけ尊重する
- upstream にない Java 側独自拡張は、本体契約と分けて扱う
- upstream の不具合を見つけても、まずは連絡事項へ記録し、そのままコンバージョンを進める

この段階で命名、責務境界、CLI 契約、対象外スコープを曖昧にすると、後から upstream 追随性を戻しにくい。
同様に、Java version, build tool, test framework, packaging を途中で揺らすと、移植そのものより実行環境差の吸収に工数を使いやすくなる。
また、移植の過程で新機能を足し始めると、straight conversion と独自拡張の境界が曖昧になり、どこまでが upstream 対応でどこからが追加仕様か説明しにくくなる。
同様に、upstream の不具合を見つけるたびに移植を止めると、straight conversion の進行が upstream 修正待ちに引きずられやすい。

上のうち、環境前提は miku シリーズの Java 版では次の意味で固定する。

- target Java compatibility
  - source version と binary target version は `1.8` 対応へ固定する
  - コンパイラ実体は必ずしも `JDK 1.8` でなくてよい
  - 利用者がそれ以上の Java runtime を使うことは妨げない
- build tool
  - `Maven` 固定とする
- test framework
  - `JUnit Jupiter` 固定とする
- primary test entrypoint
  - `mvn test` 固定とする
- runtime packaging
  - single fat jar 固定とする
  - distribution zip は処理対象に応じて追加する
- local workspace
  - repo root に `workplace/` を置く
  - `workplace/.gitkeep` は Git 管理対象にする
  - `workplace/.gitkeep` 以外の `workplace/` 配下ファイルは Git 管理対象外にする

重要なのは、後で一般論として最適そうなものへ揺らすことではなく、その straight conversion で使う土台を最初に固定することである。

また、移植前提として upstream 自体が十分に整理されていることも重要である。
もし upstream の file 境界や責務分割がまだ不安定なら、Java 側へ移す前に upstream 側でリファクタリングしてから始めるほうがよい。

この種の CLI / batch / report 中心ツールでは、利用者の実行環境を広く取りやすくするため、target Java compatibility は保守可能な範囲で低めに固定するほうがよい。
そのため、miku シリーズの Java 版では source / binary compatibility を `1.8` 対応へ固定する。

## 開始時チェックリスト

straight conversion を始めるときは、少なくとも次を埋める。

- straight conversion 元 upstream が、少なくとも移植単位で見て十分に整理・リファクタリング済みである
- target Java compatibility を source / binary ともに `1.8` 対応へ固定した
- build tool を `Maven` に固定した
- test framework を `JUnit Jupiter` に固定した
- primary test entrypoint を `mvn test` に固定した
- runtime packaging は single fat jar を固定した
- distribution zip を作るかどうかを処理対象に応じて決めた
- repo root に `workplace/` を作成し、`.gitkeep` だけを Git 管理対象にする方針を確認した
- GUI を持ち込まない範囲を決めた
- CLI を持つ場合、どこまで Node 版インタフェースを尊重するか決めた
- source file 一覧を取得した
- test file / fixture 一覧を取得した
- import / export / report artifact 一覧を取得した
- `upstream file -> Java class` 対応表の置き場を決めた
- `upstream test intent -> Java test` 対応表の置き場を決めた
- follow-up log の置き場を決めた
- focused regression command の置き場を決めた

特に、straight conversion 元 upstream が未整理のままでは、Java 側で「移植」と「upstream 側の構造整理」を同時に背負いやすい。
その状態では `upstream file -> Java class` の対応が不安定になり、straight conversion より再設計寄りの作業になりやすい。
したがって、開始前に、少なくとも移植対象の file 群がすっきり追える状態まで upstream 側で整っていることが望ましい。

## 作業の再現手順

別の miku シリーズで straight conversion を再現するときは、少なくとも次の順序で進める。

### 1. 前提を固定する

最初に固定するもの:

- straight conversion 元 upstream が十分に整理済みであること
- target Java compatibility
  - source / binary ともに `1.8` 対応へ固定
  - コンパイラ実体は `JDK 1.8` に限定しない
- build tool
  - `Maven` 固定
- test framework
  - `JUnit Jupiter` 固定
- primary test entrypoint
  - `mvn test` 固定
- runtime packaging
  - single fat jar 固定
  - distribution zip は処理対象に応じて追加
- local workspace
  - repo root に `workplace/` を作成する
  - `workplace/.gitkeep` だけを Git 管理対象にする
  - それ以外の `workplace/` 配下ファイルは Git 管理対象外にする
- straight conversion の過程では新機能を追加しないこと
- upstream bug を見つけた場合の記録先と扱い
- GUI を持ち込まないこと
- CLI を持つ場合の互換方針
- class 名 / method 名 / package 名の基本規則
  - package の基準名は `jp.igapyon.<project>`
  - 下位 package は upstream Node 側 file 名と責務から決める

この段階の成果物:

- 移植開始に耐える upstream 状態であるという確認
- repo top README の基本方針
- このガイドへの参照
- 開発基盤を固定した build file
- 新機能追加を移植スコープから外すという合意
- upstream bug を連絡事項として残す置き場

### 2. upstream 参照方法を決める

miku シリーズの Java 版では、upstream の参照方法は次の 3 通りのいずれかとする。

1. subtree による upstream 丸抱え
2. Git remote として upstream へ繋がっているが、repo 内に upstream 実体を持たない参照
3. 必要な都度、`workplace/` 以下へ upstream を `git clone` して最新を取得する参照

miku シリーズ全体では、upstream snapshot を repo 内へ保持できる subtree を好みがちである。
一方で、straight conversion 作業では、最新 upstream を汚さず見比べやすい `workplace/` 以下への都度 clone も好みがちである。

使い分けの目安:

- subtree は、移植元 snapshot を repo 内に保持し、いつでも同じ file 群を見返せるようにしたい場合に使う
- Git remote のみの参照は、repo 内に upstream 実体を持ち込まず、差分確認や履歴参照を Git 操作で行いたい場合に使う
- `workplace/` 以下への都度 clone は、最新 upstream を一時的に確認したい場合や、repo 本体へ upstream 実体を残したくない場合に使う

どの方法を選んでも、`upstream file -> Java class` と `upstream test intent -> Java test` の対応を文書で追える状態にする。
また、`workplace/` 以下へ clone した upstream は一時参照であり、`workplace/.gitkeep` 以外は Git 管理対象にしない。

### 3. upstream を棚卸しする

次を一覧化する。

- source file 一覧
- 公開 API / facade 一覧
- CLI command / option 一覧
- test file 一覧
- fixture / testdata 一覧
- import / export / report artifact 一覧

この段階の成果物:

- `upstream file` 一覧
- 主要 fixture 一覧
- Java 側で対象にする範囲と対象外スコープ

### 4. 対応表を先に作る

実装前または実装初期に、少なくとも次を文書で固定する。

- `upstream file -> Java class / package`
- `upstream test intent -> Java test`
- `upstream CLI -> Java CLI`

この段階の成果物:

- class mapping 文書
- test mapping 文書
- 必要なら CLI mapping 文書または README 節

### 5. core から実装する

実装順序の基本は次のとおり。

1. domain model
2. codec / import / export
3. validation
4. report / artifact
5. CLI
6. packaging

ここでは、Java 向けに作り直すより、upstream file ごとの責務を受けることを優先する。

この段階の成果物:

- Java class 群
- 最小 round-trip
- focused unit test

### 6. CLI と公開入口を揃える

core 実装が動き始めたら、CLI と公開入口を整える。

重視する点:

- Node 版 CLI の command / option / argument order を、説明文や usage markdown を同じ文面で利用できる程度まで寄せる
- Java 側独自拡張は upstream straight conversion と分けて扱う
- help / README / test の表現を揃える
- README / docs / CLI help は、command 名や option 名だけでなく、説明文を流用できる程度まで同期する
- AI 向け prompt markdown は source file 参照にせず、API / CLI 取得契約として揃える

この段階の成果物:

- Java CLI entrypoint
- help / usage 契約
- AI 向け prompt markdown の取得契約
- CLI regression test

### 7. 検証を段階的に強める

検証は次の順で強めるのが進めやすい。

1. semantic parity
2. fixture round-trip
3. API / CLI regression
4. deterministic output
5. byte-level parity

byte-level parity は、まず report や archive のような最終成果物から入れると効果が高い。

この段階の成果物:

- focused regression command
- parity test
- determinism test

### 8. 保守運用へ移す

主要導線が揃ったら、作業の中心を新規実装から追随運用へ移す。

重視する点:

- `upstream file` 単位で差分確認する
- 差分確認結果を文書へ残す
- docs-only 更新とコード変更を分けて扱う
- 必要な regression 単位を固定する
- upstream bug の記録と Java 側暫定回避を区別して残す

この段階の成果物:

- remaining items 文書
- follow-up log
- focused regression 一覧

## 最低限の文書テンプレート

再現可能性を高めるには、文書の粒度も揃えたほうがよい。

### 1. class mapping

最低限、次の形を持つ。

```text
upstream file:
  vendor/<project>/src/ts/<target>.ts

java classes:
  <package>.<ClassA>
  <package>.<ClassB>

notes:
  - facade:
  - helper split:
  - Java-side extension:
```

### 2. test mapping

最低限、次の形を持つ。

```text
upstream test / intent:
  <upstream test name or behavior>

java tests:
  <JavaTestClass.testMethod>

fixtures:
  <fixture path>

focused regression:
  <command>
```

### 3. follow-up log

最低限、次の形を持つ。

```text
upstream file:
  vendor/<project>/src/ts/<target>.ts

java classes:
  <package>.<ClassA>
  <package>.<ClassB>

tests:
  <RelatedTest1>
  <RelatedTest2>

diff summary:
  挙動差分:
  命名差分:
  未移植差分:
  Java 側独自拡張:

follow-up:
  - 実施した確認:
  - fixture:
  - 次回の確認観点:
```

### 4. remaining items

最低限、次を持つ。

- 現在地
- `対応済み / 保守確認 / 保留`
- focused regression 一覧
- 直近通過結果
- 次に確認すべき単位

## 移し方の基本

移植単位の基本は、upstream 側の source file 群である。

- `kebab-case` の upstream file 名を、Java の `UpperCamelCase` class 名へ読み替える
- package 名は `jp.igapyon.<project>` を基準 package 名にする
- そのうえで、下位 package 名は upstream Node 側 file 名と責務をもとに決める
- upstream の関数名が `camelCase` なら、Java 側メソッド名もできるだけ維持する
- 1 upstream file を 1 Java class で受けるのを基本にしつつ、Java で見通しを保つために helper class へ分割してよい
- その場合も、分割後の class 名から upstream の責務が推測できるようにする

一般化した例:

- `feature-a.ts` -> `FeatureA`
- `feature-b.ts` -> `FeatureB`, `FeatureBHelper`, `FeatureBValidate`
- `core-api-x.ts` -> `CoreApiX`, `CoreApiXImport`, `CoreApiXPublic`
- `excel-io.ts` -> `jp.igapyon.<project>.excelio.ExcelIo`
- `excel-io-normalize.ts` -> `jp.igapyon.<project>.excelio.ExcelIoNormalize`

この方針により、Java 側の class 分割はあっても、確認単位はあくまで `upstream file -> Java class 群` として保てる。
また、`jp.igapyon.mikuproject.excelio` のように、project 名の下に upstream file 名由来の責務 package を置くことで、Java package から upstream の確認単位を辿りやすくする。

このリポジトリで見えている命名パターン例:

この命名パターン例は、他の miku シリーズへそのまま強制するものではない。
ただし、同じ file 名・責務・語彙を持つ場合は、upstream 追随性を保つための有力な初期値として扱う。

- upstream file stem の `kebab-case` は、package では小文字連結にする
- 例: `wbs-svg.ts` -> `jp.igapyon.<project>.wbssvg`
- 例: `wbs-xlsx.ts` -> `jp.igapyon.<project>.wbsxlsx`
- 例: `project-xlsx.ts` -> `jp.igapyon.<project>.projectxlsx`
- 例: `project-workbook-json.ts` -> `jp.igapyon.<project>.projectworkbookjson`
- 例: `project-patch-json.ts` -> `jp.igapyon.<project>.projectpatchjson`
- 例: `msproject-xml.ts` / `msproject-codec.ts` / `msproject-validate.ts` など近い責務群 -> `jp.igapyon.<project>.msprojectxml`
- 例: `core-api-*.ts` -> `jp.igapyon.<project>.coreapi`

class 名の語彙変換では、upstream 語彙を保ちつつ Java の `UpperCamelCase` に寄せる。
このリポジトリでは、少なくとも次のような読み替えが暗黙に使われている。

- `wbs` -> `Wbs`
- `xlsx` -> `Xlsx`
- `xml` -> `Xml`
- `svg` -> `Svg`
- `json` -> `Json`
- `ai` -> `Ai`
- `io` -> `Io`
- `msproject` -> `MsProject`

1 upstream file が大きい場合、Java 側では責務 suffix を付けて class を分割してよい。
このとき、suffix は Java 側の都合だけで付けるのではなく、upstream file 内の責務を追える語彙にする。

- `Public`: 公開 facade / public wrapper
- `Import`: import 側の処理
- `Export`: export 側の処理
- `Validate`: validate 側の処理
- `Parse`: parse 側の処理
- `Build`: build / serialize 側の処理
- `Util`: 小さな補助処理
- `Helpers`: validate や変換の補助処理
- `Adapters`: core API と個別実装の橋渡し
- `Registry`: 公開 API を束ねる登録 / facade
- `Result`: API result object
- `Warning`: warning object
- `Document`: 外部交換用 document object
- `Operation`: patch / edit operation object
- `Zip`: archive / zip artifact 処理

例:

- `wbs-svg.ts` -> `WbsSvg`, `WbsSvgPublic`, `WbsSvgRender`, `WbsSvgCalendar`, `WbsSvgZip`
- `wbs-xlsx.ts` -> `WbsXlsx`, `WbsXlsxExport`, `WbsXlsxLayout`, `WbsXlsxCells`, `WbsXlsxPublic`
- `project-xlsx.ts` -> `ProjectXlsx`, `ProjectXlsxImport`, `ProjectXlsxExport`, `ProjectXlsxImportProject`, `ProjectXlsxExportCalendars`
- `project-workbook-json.ts` -> `ProjectWorkbookJson`, `ProjectWorkbookJsonImport`, `ProjectWorkbookJsonExport`, `ProjectWorkbookJsonValidate`
- `project-patch-json.ts` -> `ProjectPatchJson`, `ProjectPatchJsonCore`, `ProjectPatchJsonTasks`, `ProjectPatchJsonUpdates`
- `core-api-report.ts` -> `CoreApiReport`, `CoreApiReportAdapters`, `CoreApiReportPublic`
- `core-api-import.ts` -> `CoreApiImport`, `CoreApiExternalImport`, `CoreApiExternalDocument`, `CoreApiExternalBinary`
- `msproject-validate.ts` -> `MsProjectValidate`, `MsProjectValidateHelpers`

外部ライブラリや browser API の代替をスクラッチ実装する場合は、必要最小限の互換 wrapper を `Like` suffix で置いてよい。
これは upstream の依存 API 全体を再現するためではなく、straight conversion に必要な範囲だけを説明可能にするためである。
xlsx のように Apache POI を思いつきやすい領域でも、miku シリーズでは POI すら前提にせず、必要範囲だけをスクラッチ実装し、`Like` wrapper で最小互換を作ることがある。

- `XlsxWorkbookLike`
- `XlsxSheetLike`
- `XlsxRowLike`
- `XlsxCellLike`
- `XlsxColumnLike`
- `XlsxFreezePaneLike`
- `XlsxDataValidationLike`

内部 model は、原則として `model` package に置き、外部表現や upstream file 名由来の package とは分ける。
model class は `ProjectModel`, `TaskModel`, `ResourceModel`, `AssignmentModel`, `CalendarModel` のように `Model` suffix を使う。
一方で、外部交換用 document / warning / operation / result は、その責務を表す suffix を優先し、必ずしも `Model` に寄せない。

注意点:

- これらは Java 側を自由に再設計するための命名規則ではなく、upstream file / 責務 / test との対応を保つための実務上の規則である
- suffix を増やす場合は、`docs/upstream-class-mapping.md` で `upstream file -> Java class 群` として説明できるかを基準にする
- 同じ `ImportChange` のような単純名が複数 package に現れても、責務 package が分かれていて upstream 対応を説明できるなら許容する

このリポジトリから導出した命名以外の観点:

この節は、実装から見えた固定方針、既定方針、許容パターン、具体例をまとめたものである。
個々の文の強さは、`固定する`, `基本`, `許容する`, `例` などの表現に従って読む。

- Maven coordinate は `groupId = jp.igapyon`, `artifactId = <project>` を基本にする
- single fat jar の成果物名は利用者が覚えやすい `<project>.jar` に寄せ、version 番号はファイル名に含めない
- CLI main class は `jp.igapyon.<project>.cli.<Project>Cli` に置く
- CLI は `main(String[] args)` に実処理を詰め込まず、`run(String[] args, PrintStream out, PrintStream err)` のような test 可能な入口へ委譲する
- `System.exit` は原則として CLI main の最後だけに閉じ込め、core API や CLI 実処理からは exit code を返す
- CLI の stdout は正常出力や成果物本文、stderr は diagnostics / usage error / progress に使い、混ぜない
- CLI で binary artifact を扱う場合は output file を基本にし、stdout へ流す場合は command 契約として明示する
- CLI command dispatch は、upstream command 名を読みやすく辿れる程度に単純な分岐で保ち、過度に抽象化しすぎない
- distribution zip を作る場合は、jar, `README.md`, `LICENSE`, runtime CLI docs のような実行利用者向け最小物を入れる
- Java main source は source / binary `1.8` compatibility を守り、`List.of`, `Map.of`, `record`, `var`, `Files.readString` など新しい Java API / 構文を前提にしない
- test source でも可能な限り Java 1.8 前提に寄せるが、最終判断は Maven の compiler 設定と CI / 実行環境で確認する
- Java source には、その repository で採用している copyright / SPDX header を揃えて付ける
- root class は upstream file 名に近い facade として残し、実装が大きい場合は `Public`, `Import`, `Export`, `Validate` などの責務 class へ委譲する
- facade class は upstream 由来の公開 method 名と overload を保ち、Java 側 helper class の存在を利用者へ露出しすぎない
- straight conversion 中でも、外部利用者が触る public API は root facade に寄せて安定させ、helper class は追随作業に応じて揺れてよいものとして扱う
- POJO / result object / warning object は、初期 straight conversion では public field の単純 class を許容する
- report entry や archive entry のような小さな immutable value object は、constructor と public final field の単純 class で表してよい
- options object は nested class と public field で置いてよく、`null` options は入口で既定 options に読み替える
- JSON / report / workbook のように順序が出力へ影響する構造では、`LinkedHashMap` / `LinkedHashSet` を優先し、必要なら明示的に sort する
- text artifact は `UTF-8` を明示して `String` として扱う
- binary artifact は `byte[]` として扱い、file path は CLI / test / runtime 境界へ寄せる
- core API の戻り値は、単一成果物なら `String` / `byte[]` / model を直接返し、warnings / changes / 複数成果物を伴う場合は result object にまとめる
- zip / xlsx / report bundle のような archive artifact は、entry 名、entry 順、timestamp、bytes が揺れないように determinism を固定する
- report bundle や zip の entry 名は、利用者が読める固定名にし、temporary path, version, current date 由来の値を混ぜない
- text entry を archive に入れる場合は、必要なら末尾改行の有無も固定し、byte-level parity の比較対象にする
- zip timestamp は必要なら固定値を使い、byte-level parity の邪魔になる現在時刻を混ぜない
- AI 向け prompt / spec markdown は source tree の path を利用者に参照させず、classpath resource と API / CLI 取得契約へ寄せる
- vendor 由来の markdown を runtime で使う場合は、build 時に classpath resource へコピーし、JAR 内から読める状態にする
- classpath resource は `getResourceAsStream` のような JAR 内でも動く読み方に寄せ、開発 checkout の相対 path へ依存しない
- fixture は upstream の `testdata` を優先して使い、fixture 名が test 名から推測できるようにする
- test 名は upstream test intent を英語で読める形にし、必要なら `Upstream` と fixture 名を含める
- focused regression command は docs に残し、変更対象の upstream file / Java class / Java test から辿れるようにする
- docs-only 更新では追加 test を回さない運用を許容し、コード変更や回帰コマンド更新時だけ対象 test を実行する
- README / docs / CLI help の同期は test 対象に含め、usage 文面と実装のずれを CLI regression で検出できるようにする
- CLI test は stdout / stderr / exit code / output file / archive entry まで確認し、README / usage と実装のずれを早めに検出する
- warning / changes は、処理継続できる差分の記録として result object に積む
- 入口契約違反や構文として読めない入力は、warning へ落とさず例外へ寄せる
- `replace` / `merge` / `patch` のような mode は文字列契約として明示し、base model が必要な mode は入口で検査する
- `import` のような Java keyword と衝突する upstream 語彙は、`imports` や `Import` suffix のような最小変更で回避し、別語彙へ置き換えすぎない
- Java 側の batch command は `*-batch` のように通常 command と見分けやすい名前へ寄せ、core API の責務へ混ぜない
- Java 側独自の batch command や diagnostics は便利でも、core straight conversion の契約とは文書上分ける

メソッド名についても同じ考え方を採る。

- upstream の関数名が `camelCase` なら、Java 側メソッド名も原則そのまま引き継ぐ
- class 名のように Java 規約へ大きく読み替えるのは避け、語彙対応を保つ
- static / instance の違い、引数のまとめ方、戻り値型や例外設計は Java 側で調整してよい
- ただし中心となる動詞や責務名は upstream から大きくずらさない

例:

- `importFromXml` -> `importFromXml`
- `exportToXml` -> `exportToXml`
- `parseAiJsonText` -> `parseAiJsonText`
- `importIntoProjectModel` -> `importIntoProjectModel`

## どこを移し、どこを外したか

Java 版は upstream 全体をそのまま持ち込むことを目的にしていない。
対象は、Java runtime で自然に扱える core / CLI / batch / packaging 中心の導線である。

主対象:

- domain model
- codec / import / export
- validation
- CLI / batch entrypoint
- runtime packaging
- Java runtime で自然に扱える report / artifact / exchange format

主に対象外:

- browser / Web UI 前提の導線
- GUI そのものを Java 側へ持ち込むこと
- single-file web app としての配布形態そのもの
- ブラウザ固有の DOM / preview / event handling
- Java 側で自然でない UI 都合の補助実装

つまり、upstream の意味構造は受けるが、GUI は持ち込まず、実行環境は Java CLI / jar 配布へ読み替えている。

## Java 側であえて追加したもの

`straight conversion` であっても、Java 側では運用上必要な wrapper や packaging を追加している。

主な例:

- `CoreApi*` のような公開入口の整理
- Java CLI entrypoint
- AI 向け prompt markdown を取得する API / CLI entrypoint
- single fat jar packaging
- 処理対象に応じた distribution zip packaging
- batch command など、Java 側運用のための補助導線

これらは upstream 本体と同一責務として混ぜず、`Java 側独自拡張` として区別して扱う。

## CLI の扱い

CLI を持つ upstream を Java へ移す場合、Java 側 CLI は Node 版 CLI のインタフェースをできるだけ尊重する。

重視する点は次のとおり。

- command 名や subcommand の切り方は、upstream の説明文や usage markdown を同じ文面で利用できる程度まで寄せる
- option 名や引数順を、可能な限り upstream に寄せる
- text / json / binary の出力契約を崩さない
- diagnostics や usage error の系統を追跡可能に保つ
- AI 向け prompt markdown は、source file 参照ではなく CLI / API の取得契約として提供する

ただし、次は Java 側の運用都合として追加してよい。

- batch command
- fat jar 実行に合わせた最小限の起動方法差
- Java runtime / file API に合わせた補助 diagnostics

この場合も、upstream CLI 本体の契約と Java 側独自拡張は混ぜず、どこからが追加分か説明できる状態を保つ。

知見:

- CLI 契約を最初に曖昧にすると、あとで help / README / test / diagnostics の同期コストが増えやすい
- Java 側独自の batch command は便利だが、upstream straight conversion と混ぜると追随単位が崩れやすい
- `core の契約` と `Java 側運用拡張` を分けて扱うほうが保守しやすい

## 文書へ戻す運用判断

straight conversion は、最初の方針だけでは足りない。
そのため、ストレートコンバージョンの適用を繰り返すたびにこの文書を更新し、途中で個別に固める運用判断を減らす努力をする。

- upstream 差分確認の単位を `upstream file` 基準で固定する
- `upstream file -> Java class -> Java test` の対応を文書で固定する
- docs-only 更新とコード変更を運用上区別する
- Java 側独自拡張を `batch command`, packaging, diagnostics などに分けて管理する
- 主要成果物について、意味比較だけでなく byte-level parity を使う
- 同一入力から同一出力が出る determinism を test で固定する
- 保守フェーズでは新規機能追加より既存実装の確認と upstream 差分追随を優先する

特に byte-level parity は、途中で品質基準として明示的に取り入れる価値が高い。
text / json / xml / zip / xlsx / report artifact について byte 単位まで比較できるようにすると、並び順、改行、entry 名、既定値補完、付随メタデータのような暗黙仕様が見えやすくなる。
これは Java 側の無意識な再設計や整形差分を抑え、upstream との差分を具体的に議論しやすくする。

知見:

- byte-level parity は最初から全面適用しなくてもよいが、report や archive のような最終成果物には早めに入れる価値が高い
- 全出力に同じ厳しさを一気に求めるより、比較価値の高い成果物から段階的に広げるほうが進めやすい

## core API の境界

straight conversion では、core API と CLI / runtime API の境界を早めに固定する。

重視する点は次のとおり。

- core package は file path や `Files` のような file system API に依存しない
- text document は `String` を正本にする
- binary artifact は `byte[]` を正本にする
- core API は標準入出力へ直接書かず、戻り値として model / result object / `String` / `byte[]` を返す
- 単一成果物だけを返す API は直接 `String` や `byte[]` を返し、warnings / changes / 複数 artifact / metadata が必要な API は result object を返す
- AI 向け prompt markdown のような利用者向け文書も、source tree の file path 参照ではなく API / CLI で取得できる成果物として扱う
- `InputStream` / `OutputStream` は主に内部実装や runtime 境界で使う
- file read / write や標準入出力との橋渡しは CLI 層に寄せる
- CLI では通常出力や成果物本文を stdout または output file へ出し、diagnostics / usage error / progress は stderr へ出す
- binary artifact は原則として output file へ書き、stdout へ出す場合は command 契約として明示する
- stdin 入力は text / json のような stream と相性がよいものに限定し、binary や複数 input file が必要な処理では file path 引数を優先する

この境界を曖昧にすると、core 実装が Java runtime 都合へ引きずられ、upstream の責務との対応が見えにくくなる。

知見:

- `String` と `byte[]` を正本にし、file path や stream を境界へ寄せる整理はうまく機能しやすい
- core が `Path` や `Files` を持ち始めると、CLI 都合の修正が core へ逆流しやすい
- stdout / stderr の使い分けを固定すると、CLI test で正常出力と diagnostics を分けて確認しやすい

## 値の持ち方

内部 model で値の有無をどう表すかも、途中で揺らさないほうがよい。

基本方針:

- `null` は `値なし` を表す内部表現として使う
- 空文字は必要なら保持してよいが、入口では `null` 相当に寄せてもよい
- `0` は有効値である場合にだけ使い、`値なし` の代用にしない
- report や CLI の表示都合で置き換えた値を、そのまま内部正本へ持ち込まない

重要なのは、`blank` / `null` / `0` の混同を避けることである。

知見:

- この方針は概ねうまく機能する
- 特に workbook import や report 表示で、未入力と有効値 `0` を分けやすくなる
- 一方で XML import の入口や人向け出力では局所ルールが必要になり、全面的に単純化できるわけではない
- したがって、`内部正本では混同しない`, `入力境界と表示境界では局所変換を許す` という整理が実務上扱いやすい

## 例外と warning の返し方

`parse` / `import` / `validate` の返し方も、共通原則として固定しておくとぶれにくい。

基本の分け方:

- 入口契約違反
  - 例外で返す
- 意味検証の問題
  - issue / warning として返す
- 継続可能な局所差分
  - result の warnings や changes へ積む

この線引きを先に決めておくと、Java 側 API が「何で throw し、何を warning へ落とすか」を一貫させやすい。
diagnostics の粒度は層ごとに分ける。
CLI では利用者向け message と exit code を返し、core API では例外または result object に集約し、test では期待差分として固定する。
この分離により、人向け表示都合が core の例外設計や内部 model へ逆流しにくくなる。

知見:

- この線引きがないと、同じ種類の不整合が API ごとに throw と warning に割れやすい
- `入口契約違反` と `意味検証` を分けるだけでも、CLI diagnostics がかなり安定しやすい

## options object の扱い

Node.js upstream の options object を Java でどう表すかも、初期段階で方針を決めておく。

基本方針:

- builder pattern を初期移植で必須にしない
- JavaBean へ寄せすぎず、upstream の option 名と意味を追いやすい field 名を優先する
- `options なし overload` と `options あり overload` を併置してよい
- option 名は Java 側独自語彙へ置き換えず、upstream との対応を保つ

知見:

- 初期移植で builder や抽象 request class を入れすぎると、upstream option object との対応が見えにくくなりやすい
- まず素朴な POJO で移し、その後に必要なら整理するほうが追随性を保ちやすい

## ライブラリ方針

Java では便利なライブラリが多いが、straight conversion では「便利だから置き換える」を優先しない。

判断基準:

1. upstream が外部ライブラリを使っているか
2. upstream がスクラッチ実装しているか
3. Java 側に十分適合するライブラリがあるか
4. target Java compatibility で保守可能か
5. upstream 追随性を壊さないか

このため、miku シリーズの Java 版では、無理にライブラリを利用せず、スクラッチで実装することが多い傾向にある。
upstream がスクラッチ実装している箇所は、まず Java 側でもその構造やロジックを踏襲するほうが差分比較しやすい。
たとえば xlsx のように Apache POI を思いつきやすい領域でも、必要な生成・読取・検証の範囲が限定され、byte-level parity や upstream 追随性を優先するなら、POI すら使わずスクラッチ実装を選んでよい。

知見:

- Java に便利な既製ライブラリがあっても、早い段階で置き換えると「移植」より「再実装」に寄りやすい
- 一方で upstream が外部ライブラリを使っている箇所は、対応 Java ライブラリを検討したほうが自然な場合も多い

## docs と追随運用

straight conversion では、実装だけでなく追随運用も共通原則として持つ。

重視する点は次のとおり。

- `upstream file -> Java class -> Java test` の対応を文書で固定する
- 差分確認結果を `upstream file` 単位で残す
- docs-only 更新とコード変更を運用上区別する
- focused regression を用意し、変更単位に応じて実行できるようにする
- upstream bug を見つけた場合の連絡事項の置き場を持つ

特に docs-only 更新では、原則として追加テストを回さず、コード変更や回帰コマンド自体の更新時だけ対象単位を確認する、という運用は有効である。

upstream bug を見つけた場合は、原則として次の順で扱う。

1. upstream への連絡事項として記録する
2. どの upstream file / 責務に関係するか分かるように残す
3. Java 側の straight conversion 自体は止めず、そのまま進める
4. Java 側で暫定回避を入れる場合は、upstream bug に起因することを記録する

知見:

- upstream bug を見つけても、その場で Java 側の設計を変え始めると straight conversion より独自修正へ寄りやすい
- まず記録し、まず移す、という順にしたほうが upstream 追随性を保ちやすい

## 成果物として残すもの

straight conversion を再現可能にするには、コードだけでなく、次の成果物を残す必要がある。

- repo top README
- straight conversion guide
- remaining items / current status 文書
- `upstream file -> Java class` mapping
- `upstream test intent -> Java test` mapping
- follow-up log
- focused regression command 一覧
- CLI help / usage 契約
- AI 向け prompt markdown の API / CLI 取得契約
- fixture / parity / determinism test

これらが揃っていると、別の人が途中から入っても、同じ粒度で移植と追随作業を再開しやすい。

## 完了条件

straight conversion の初期段階を完了扱いにするには、少なくとも次を満たすとよい。

- core の主要導線が Java 側で動く
- 最小 round-trip が成立する
- `upstream file -> Java class` 対応表がある
- `upstream test intent -> Java test` 対応表がある
- focused regression command がある
- CLI を持つ場合、主要 command / option / diagnostics の契約が固定されている
- follow-up log で少なくとも数件の `upstream file` 実例が残っている
- 主要成果物について determinism が確認されている
- 比較価値の高い成果物について byte-level parity が確認されている、または未適用理由が文書化されている

重要なのは、コードが動くだけでなく、追随可能性と検証単位が残っていることである。

## 最初の1週間の進め方の例

### Day 1

- target Java compatibility, build tool, test framework, primary test entrypoint, single fat jar packaging を固定する
- distribution zip が必要な処理対象か確認する
- repo root に `workplace/` を作成し、`.gitkeep` だけを Git 管理対象にする
- upstream file / test / fixture / CLI の棚卸しを始める
- README に基本方針を書く

### Day 2

- `upstream file -> Java class` の初版を作る
- `upstream test intent -> Java test` の初版を作る
- core の最小対象を決める

### Day 3

- domain model と最小 codec を実装する
- 最小 round-trip test を通す

### Day 4

- validation と主要 fixture import を足す
- focused regression の最小単位を決める

### Day 5

- CLI entrypoint を作る
- help / usage / diagnostics の契約を test で固定する

### Day 6

- report / artifact のうち比較価値が高いものを実装する
- determinism を test で固定する

### Day 7

- byte-level parity を入れられる成果物から比較を始める
- remaining items と follow-up log を整備する
- docs-only と code-change の運用を分ける

知見:

- docs-only 更新まで毎回 test を要求すると、文書保守の速度が落ちやすい
- 代わりに、focused regression の正本を文書へ固定しておくと、コード変更時に必要な確認単位を迷いにくい

## 品質の上げ方

straight conversion では、Java 側でそれらしい実装を書くことより、upstream 対応の説明可能性と出力同等性を重視する。

確認の柱は次のとおり。

- `docs/upstream-class-mapping.md`
  - `upstream file -> Java class` の対応を固定する
- `docs/upstream-test-mapping.md`
  - `upstream test intent -> Java test` の対応を固定する
- `docs/upstream-followup-log.md`
  - `upstream file` 単位で差分確認結果を残す

さらに、text / json / xml / zip / xlsx / report artifact などの成果物については、意味比較だけでなく byte-level の比較も使う。

この確認は次の効果を持つ。

- 並び順、改行、entry 名、既定値補完などの暗黙仕様が見える
- Java 側の無意識な再設計や整形差分を抑えやすい
- upstream 差分を「どこが違うか」で具体的に議論しやすい
- 同じ入力から同じ出力が出る determinism を固定しやすい

## 保守フェーズでの扱い

straight conversion の初期実装を越えて、主要導線の Java 側実装を一通り持った後は、保守フェーズとして扱う。

ただし、この段階でも基本姿勢は変えない。

- 新規機能追加より、既存実装の確認と upstream 差分追随を優先する
- Java 側独自整理を進める前に、対応表と test と follow-up log の整合を保つ
- upstream 更新時は `upstream file` 単位で確認する

特に、straight conversion の進行中は新機能を混ぜない。
必要な新機能や Java 側独自価値は、straight conversion の主要導線が揃い、追随単位と検証単位が固定された後に別トラックで扱うほうがよい。

このため、この移植作業は「Java 向けに作り直す作業」というより、「Node.js / TypeScript upstream を追跡可能なまま Java へ写像し、その同等性を保守する作業」と表現するのが適切である。

## このリポジトリでの適用例

このリポジトリでは、上の共通原則を `mikuproject` に適用している。

- upstream:
  - `vendor/mikuproject/src/ts/*.ts`
- Java 側の主要責務:
  - `MS Project XML`
  - `ProjectModel`
  - validation
  - workbook JSON / XLSX
  - patch / AI JSON
  - report (`Markdown`, `SVG`, `WBS XLSX`, `Mermaid`)
  - CLI entrypoint
- Java 側独自拡張の代表:
  - `CoreApi*`
  - `MikuprojectCli`
  - single fat jar packaging
  - 処理対象に応じた distribution zip packaging

このように、共通原則を先に固定し、その下に各リポジトリ固有の適用例を置くと、他の miku シリーズへも流用しやすい。

このリポジトリで固定した環境前提:

- target Java compatibility:
  - source / binary ともに `1.8` 対応
  - コンパイラ実体は `JDK 1.8` に限定しない
- build tool:
  - `Maven`
- test framework:
  - `JUnit Jupiter 5.14.1`
- primary test entrypoint:
  - `mvn test`
- runtime packaging:
  - single fat jar 固定 (`target/mikuproject.jar`)
  - distribution zip は処理対象に応じた追加成果物 (`target/mikuproject-dist.zip`)

このリポジトリでの進捗、残件、直近の確認結果は、この適用例ではなく `README.md`, `docs/remaining-migration-items.md`, `docs/upstream-class-mapping.md`, `docs/upstream-test-mapping.md`, `docs/upstream-followup-log.md` を正本として扱う。
