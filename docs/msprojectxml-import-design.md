# MS Project XML Import Design

## 目的

この文書は、Java 版 `mikuproject` の STEP1 における
`MS Project XML -> ProjectModel`
の最小 import 設計を整理した設計メモである。

前提として、Node.js 版 upstream の `msproject-xml.ts` / `msproject-codec.ts` / `msproject-xml-dom.ts` / `msproject-calendar.ts` / `msproject-validate.ts`
の責務分割を尊重する。

## 現在の位置づけ

この文書は STEP1 初期の import 設計メモとして残す。
現在は、ここで整理した `MsProjectXml` / `MsProjectCodec` / `MsProjectXmlDom` / `MsProjectCalendar` / `MsProjectValidate` に加え、`MsProjectValidateHelpers` / `MsProjectSamples` / `MsProjectCsv` / `MsProjectAiViews` / `MsProjectMermaid` まで Java 側へ実装済みである。

したがって、現フェーズではこの文書を新規実装リストとして扱わない。
不足を見つけた場合は、既存実装と upstream 対応表、test 対応表、差分確認ログのどこに反映するかを先に確認する。

現在の正本:

- `docs/miku-straight-conversion-guide.md`
- `docs/step1-spec.md`
- `docs/remaining-migration-items.md`
- `docs/upstream-class-mapping.md`
- `docs/upstream-test-mapping.md`

## 基本方針

- Java 版でも `MS Project XML` import の主導線は `ProjectModel` 生成とする
- Node.js 版 upstream の責務境界をできるだけ保つ
- XML の parse、XML から model への写像、既定値補完、validation を分ける
- Java 1.8 前提でも、責務の境界は upstream と対応づけやすい形を優先する

## Node.js upstream での責務分割

upstream では、おおむね次の分担になっている。

- `msproject-xml`
  - facade
  - codec / xml helper / calendar / validate helper / validate を束ねる
- `msproject-codec`
  - XML と `ProjectModel` の相互変換本体
- `msproject-xml-dom`
  - XML DOM ヘルパ
  - `textContent`, `parseBoolean`, `parseNumber`, `parseWeekDays`, `parseWorkingTimes` など
- `msproject-calendar`
  - 既定 calendar の補完
- `msproject-validate`
  - `ProjectModel` の妥当性検査

Java 版でも、この責務分割を基本的に踏襲する。

## Java 側の初期移植 class 構成

`jp.igapyon.mikuproject.msprojectxml` 配下の基本構成は次のとおり。

- `MsProjectXml`
  - facade
  - import / export / normalize / validate の入口
- `MsProjectCodec`
  - XML と `ProjectModel` の相互変換本体
- `MsProjectXmlDom`
  - XML DOM ヘルパ
- `MsProjectCalendar`
  - 既定 calendar の補完
- `MsProjectValidate`
  - `ProjectModel` の妥当性検査

当初は、必要に応じて後続で次も追加候補としていた。
現在は主要導線の対応 class が存在するため、以下は追加リストではなく、upstream 対応を追うための関連 class 群として扱う。

- `MsProjectValidateHelpers`
- `MsProjectSamples`
- `MsProjectCsv`
- `MsProjectAiViews`
- `MsProjectMermaid`

STEP1 初期では、まず `XML import/export` に必要な最小集合を優先した。
現在の残作業は、これらの class を新規追加することではなく、既存実装が upstream 更新時に追える状態を保つことである。

## import の処理段階

Java 版の `MS Project XML -> ProjectModel` は、少なくとも次の段階に分ける。

1. XML 文字列を XML Document へ parse する
2. XML helper を使って Project / Calendars / Tasks / Resources / Assignments を読む
3. `ProjectModel` POJO を組み立てる
4. 既定 calendar を補完する
5. 必要に応じて normalize する
6. validation を実行できる状態にする

この流れは、upstream の `importMsProjectXml(...)` の考え方を引き継ぐ。

## `MsProjectXml` の役割

`MsProjectXml` は、Java 側の facade とする。

現在は、少なくとも次の責務を受ける facade として扱う。

- `importFromXml`
- `exportToXml`
- `normalizeProjectModel`
- `validateProjectModel`

メソッド名は、upstream の語彙に寄せる。
ただし class 名は Java 規約に合わせて `MsProjectXml` とする。

## `MsProjectCodec` の役割

`MsProjectCodec` は、XML と `ProjectModel` の相互変換本体とする。

現在は、少なくとも次の責務を受ける codec 本体として扱う。

- `importMsProjectXml`
- `exportMsProjectXml`

この class では、Node.js 版 upstream の `msproject-codec.ts` と対応づけやすい構造を維持する。

特に import では、次の単位を順に読む形を基本とする。

- `project`
- `calendars`
- `tasks`
- `resources`
- `assignments`

Java 側で private helper を増やすことは許容するが、上位の責務境界は upstream から大きくずらさない。

## `MsProjectXmlDom` の役割

`MsProjectXmlDom` は、XML DOM 周辺の小さな helper を持つ class とする。

現在は、少なくとも次のような helper 群を受ける class として扱う。

- `textContent`
- `parseBoolean`
- `parseNumber`
- `parseWeekDays`
- `parseWorkingTimes`
- `appendTextElement`
- `appendWeekDays`
- `appendWorkingTimes`
- `parseXmlDocument`
- `formatXml`

Java では DOM 実装自体は標準 API を使う想定だが、helper の責務は upstream に寄せる。

## `MsProjectCalendar` の役割

`MsProjectCalendar` は、既定 calendar を補完する責務を持つ class とする。

現在は、少なくとも次の責務を受ける class として扱う。

- `ensureDefaultProjectCalendar`

Node.js 版 upstream と同様に、`ProjectModel` に calendar がない場合の既定補完を扱う。

## `MsProjectValidate` の役割

`MsProjectValidate` は、`ProjectModel` に対する妥当性検査を持つ class とする。

現在は、少なくとも次の責務を受ける class として扱う。

- `validateProjectModel`

STEP1 の段階では、validation は import 完了後の確認用であり、import と強く結合させない。

## package 方針

現在の基本 package は次のとおりである。

- `jp.igapyon.mikuproject.model`
- `jp.igapyon.mikuproject.msprojectxml`

役割の対応は次のとおり。

- `model`
  - `ProjectModel` とその下位 POJO
- `msprojectxml`
  - XML import / export / helper / calendar / validate

この方針は current implementation でも維持しており、Java 側独自都合でさらに細かい package へ分割しすぎない。

## Java 1.8 前提での XML API 方針

STEP1 の初期移植では、Node.js 版 upstream の DOM ベースの構造を追いやすくするため、Java 側でも DOM ベースを第一候補とする。

この時点では、次を優先する。

- parse の分かりやすさ
- upstream の `textContent(...)` / `getElementsByTagName(...)` 的な流れとの対応
- 小さな helper で処理を追えること

このため、初期移植では SAX や StAX への最適化を先行しない。

## 例外とエラーの位置づけ

STEP1 の import 設計では、次を分けて扱う。

- XML として壊れている
  - parse error
- XML は読めるが値が不正、または不足している
  - validation issue

つまり、import は「読めるものを `ProjectModel` へ落とす」ことを優先し、妥当性の問題は validation で別に返せる形を基本とする。

ただし、XML 自体の parse failure は import 失敗として扱う。

## 初期移植時点の未確定事項

次は初期移植時点では別途詳細化する項目として扱っていた。
現在は `docs/step1-spec.md`, `docs/remaining-migration-items.md`, `docs/upstream-class-mapping.md`, `docs/upstream-test-mapping.md` に実装後の方針と確認単位を集約している。

- Java 側で `normalizeProjectModel` をどこへ置くか
- import 時に validation を自動実行するか
- DOM helper を instance method にするか static method にするか
- date/time / duration の Java 型
- parse error の exception class
