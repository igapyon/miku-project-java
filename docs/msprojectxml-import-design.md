# MS Project XML Import Design

## 目的

この文書は、Java 版 `mikuproject` の STEP1 における
`MS Project XML -> ProjectModel`
の最小 import 設計を整理するためのメモである。

前提として、Node.js 版 upstream の `msproject-xml.ts` / `msproject-codec.ts` / `msproject-xml-dom.ts` / `msproject-calendar.ts` / `msproject-validate.ts`
の責務分割を尊重する。

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

## Java 側の first cut class 案

`jp.igapyon.mikuproject.msprojectxml` 配下の first cut 候補は次のとおり。

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

必要に応じて、後続で次も追加候補とする。

- `MsProjectValidateHelpers`
- `MsProjectSamples`
- `MsProjectCsv`
- `MsProjectAiViews`
- `MsProjectMermaid`

ただし STEP1 では、まず `XML import/export` に必要な最小集合を優先する。

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

最低限、次の責務を持たせる候補とする。

- `importFromXml`
- `exportToXml`
- `normalizeProjectModel`
- `validateProjectModel`

メソッド名は、upstream の語彙に寄せる。
ただし class 名は Java 規約に合わせて `MsProjectXml` とする。

## `MsProjectCodec` の役割

`MsProjectCodec` は、XML と `ProjectModel` の相互変換本体とする。

最低限、次の責務を持たせる候補とする。

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

最低限、次のような helper を持つ候補とする。

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

最低限、次の責務を持たせる候補とする。

- `ensureDefaultProjectCalendar`

Node.js 版 upstream と同様に、`ProjectModel` に calendar がない場合の既定補完を扱う。

## `MsProjectValidate` の役割

`MsProjectValidate` は、`ProjectModel` に対する妥当性検査を持つ class とする。

最低限、次の責務を持たせる候補とする。

- `validateProjectModel`

STEP1 の段階では、validation は import 完了後の確認用であり、import と強く結合させない。

## package 案

first cut では、次の package を基本候補とする。

- `jp.igapyon.mikuproject.model`
- `jp.igapyon.mikuproject.msprojectxml`

役割の対応は次のとおり。

- `model`
  - `ProjectModel` とその下位 POJO
- `msprojectxml`
  - XML import / export / helper / calendar / validate

この段階では、Java 側独自都合でさらに細かい package へ分割しすぎない。

## Java 1.8 前提での XML API 方針

STEP1 の first cut では、Node.js 版 upstream の DOM ベースの構造を追いやすくするため、Java 側でも DOM ベースを第一候補とする。

この時点では、次を優先する。

- parse の分かりやすさ
- upstream の `textContent(...)` / `getElementsByTagName(...)` 的な流れとの対応
- 小さな helper で処理を追えること

このため、first cut では SAX や StAX への最適化を先行しない。

## 例外とエラーの位置づけ

STEP1 の import 設計では、次を分けて扱う。

- XML として壊れている
  - parse error
- XML は読めるが値が不正、または不足している
  - validation issue

つまり、import は「読めるものを `ProjectModel` へ落とす」ことを優先し、妥当性の問題は validation で別に返せる形を基本とする。

ただし、XML 自体の parse failure は import 失敗として扱う。

## first cut の未確定事項

次は別途詳細化する。

- Java 側で `normalizeProjectModel` をどこへ置くか
- import 時に validation を自動実行するか
- DOM helper を instance method にするか static method にするか
- date/time / duration の Java 型
- parse error の exception class
