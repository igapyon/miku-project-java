# ProjectModel POJO Class Plan

## 目的

この文書は、Node.js 版 upstream の `types.ts` を、Java 版の POJO class 群へどう読み替えるかを整理するためのメモである。

前提として、Java 側でも型構造は Node.js 版 upstream に寄せる。
Java 向けに命名規約は調整するが、責務や語彙は upstream から大きくずらさない。

## 基本方針

- `types.ts` の主要型に対応する Java class を用意する
- `kebab-case` のファイル名は Java 側では class 群に分かれるが、語彙は upstream に寄せる
- TypeScript の `type` は Java 側で POJO class として表現する
- 配列型は Java 側で `List<...>` を基本候補とする
- optional 項目は Java 側で nullable field として扱う前提で詳細化する

## package 方針

first cut では、POJO は `jp.igapyon.mikuproject.model` 配下へ置くことを基本候補とする。

必要に応じて、後続で package を分けることはありうるが、初期段階では細分化しすぎない。

## 主要対応表

Node.js upstream の `types.ts` に対する Java 側 class 対応は、まず次を基本とする。

| upstream type | Java class 候補 |
| --- | --- |
| `ProjectModel` | `ProjectModel` |
| `ProjectInfo` | `ProjectInfo` |
| `TaskModel` | `TaskModel` |
| `ResourceModel` | `ResourceModel` |
| `AssignmentModel` | `AssignmentModel` |
| `CalendarModel` | `CalendarModel` |
| `WeekDayModel` | `WeekDayModel` |
| `WorkingTimeModel` | `WorkingTimeModel` |
| `CalendarExceptionModel` | `CalendarExceptionModel` |
| `WorkWeekModel` | `WorkWeekModel` |
| `PredecessorModel` | `PredecessorModel` |
| `OutlineCodeModel` | `OutlineCodeModel` |
| `OutlineCodeMaskModel` | `OutlineCodeMaskModel` |
| `OutlineCodeValueModel` | `OutlineCodeValueModel` |
| `WBSMaskModel` | `WbsMaskModel` |
| `ProjectExtendedAttributeModel` | `ProjectExtendedAttributeModel` |
| `TaskExtendedAttributeModel` | `TaskExtendedAttributeModel` |
| `ResourceExtendedAttributeModel` | `ResourceExtendedAttributeModel` |
| `AssignmentExtendedAttributeModel` | `AssignmentExtendedAttributeModel` |
| `TaskBaselineModel` | `TaskBaselineModel` |
| `ResourceBaselineModel` | `ResourceBaselineModel` |
| `AssignmentBaselineModel` | `AssignmentBaselineModel` |
| `TaskTimephasedDataModel` | `TaskTimephasedDataModel` |
| `ResourceTimephasedDataModel` | `ResourceTimephasedDataModel` |
| `AssignmentTimephasedDataModel` | `AssignmentTimephasedDataModel` |
| `ValidationIssue` | `ValidationIssue` |

`WBSMaskModel` については、Java の一般的な読みでは `WbsMaskModel` が自然なため、これを第一候補とする。
ただし upstream 語彙との対応は明確に保つ。

## `ProjectModel` の構成

`ProjectModel` は、Node.js 版 upstream と同様に、次の 5 要素を持つ class とする。

- `project`
- `tasks`
- `resources`
- `assignments`
- `calendars`

Java 側でも、まず `ProjectModel` を最上位集約 class とする。

## `ProjectInfo` の扱い

Node.js 版 upstream の `project` オブジェクトは、Java 側では `ProjectInfo` class として切り出す。

理由:

- upstream の `ProjectInfo` と対応づけやすい
- `ProjectModel` 直下に多数の field を抱え込ませずに済む
- `msproject-codec` での `project` 部分 import/export と対応しやすい

## `Task / Resource / Assignment / Calendar` の扱い

`TaskModel`, `ResourceModel`, `AssignmentModel`, `CalendarModel` は、それぞれ独立 class とする。

これは upstream の責務分離にそのまま対応するためである。

特に、次の関係を保つ。

- `ProjectModel.tasks` は `List<TaskModel>`
- `ProjectModel.resources` は `List<ResourceModel>`
- `ProjectModel.assignments` は `List<AssignmentModel>`
- `ProjectModel.calendars` は `List<CalendarModel>`

## 下位型の扱い

Node.js 版 upstream で配列としてぶら下がっている下位型も、Java 側では独立 class とする。

例:

- `TaskModel.predecessors` は `List<PredecessorModel>`
- `TaskModel.extendedAttributes` は `List<TaskExtendedAttributeModel>`
- `TaskModel.baselines` は `List<TaskBaselineModel>`
- `TaskModel.timephasedData` は `List<TaskTimephasedDataModel>`
- `CalendarModel.weekDays` は `List<WeekDayModel>`
- `CalendarModel.exceptions` は `List<CalendarExceptionModel>`
- `CalendarModel.workWeeks` は `List<WorkWeekModel>`
- `WeekDayModel.workingTimes` は `List<WorkingTimeModel>`

## `ValidationIssue` の扱い

Node.js 版 upstream の `ValidationIssue` も、Java 側で独立 class とする。

最低限、次を持つ class を候補とする。

- `level`
- `scope`
- `message`

`level` と `scope` は、Java 側では enum 化も候補だが、これは別途詳細化する。

## first cut での class 作成順

POJO class は、まず次の順で作成するのが自然である。

1. `ProjectModel`
2. `ProjectInfo`
3. `TaskModel`
4. `PredecessorModel`
5. `ResourceModel`
6. `AssignmentModel`
7. `CalendarModel`
8. `WeekDayModel`
9. `WorkingTimeModel`
10. `CalendarExceptionModel`
11. `WorkWeekModel`

その後、必要に応じて次を順次追加する。

- `OutlineCodeModel` 系
- `WbsMaskModel`
- `ExtendedAttributeModel` 系
- `BaselineModel` 系
- `TimephasedDataModel` 系
- `ValidationIssue`

## first cut での field 詳細化方針

class を作る順序と、field を全部一気に埋めることは分けて考える。

- class の骨格は upstream に合わせて先に作る
- field は `docs/projectmodel-first-cut.md` の優先順に従って段階的に埋める
- first cut に入っていない field も、後で追加しやすいよう class 自体の対応関係は先に固定する

## Java 側でまだ未確定の点

次は別途詳細化する。

- field を public にするか private + accessor にするか
- builder を使うかどうか
- `List` の具象型をどうするか
- enum 化する項目の範囲
- date/time/duration を `String` のまま持つか、Java 型へ変換するか
- optional number / boolean の扱い
