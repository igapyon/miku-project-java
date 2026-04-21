# ProjectModel POJO Class Plan

## 目的

この文書は、Node.js 版 upstream の `types.ts` を、Java 版の POJO class 群へどう読み替えるかを整理した設計メモである。

前提として、Java 側でも型構造は Node.js 版 upstream に寄せる。
Java 向けに命名規約は調整するが、責務や語彙は upstream から大きくずらさない。

## 現在の位置づけ

この文書は STEP1 初期に `types.ts` と Java POJO class 群の対応を決めるための設計メモとして残す。
現在は主要 model class が Java 側に存在するため、この文書の `候補` や `未確定` は未実装 TODO ではなく、当時の判断背景として読む。

現フェーズで model field や class の不足を見つけた場合は、新規機能追加ではなく、既存仕様の不足または upstream 差分として扱い、対応表と test 対応へ反映する。

現在の正本:

- `docs/miku-straight-conversion-guide.md`
- `docs/step1-spec.md`
- `docs/remaining-migration-items.md`
- `docs/upstream-class-mapping.md`
- `docs/upstream-test-mapping.md`

## 基本方針

- `types.ts` の主要型に対応する Java class を用意する
- `kebab-case` のファイル名は Java 側では class 群に分かれるが、語彙は upstream に寄せる
- TypeScript の `type` は Java 側で POJO class として表現する
- 配列型は Java 側で `List<...>` を基本候補とする
- optional 項目は Java 側で nullable field として扱う前提で詳細化する

## package 方針

現在の POJO は `jp.igapyon.mikuproject.model` 配下へ置く方針で運用している。

必要に応じて、後続で package を分けることはありうるが、current implementation でも細分化しすぎない。

## 主要対応表

Node.js upstream の `types.ts` に対する Java 側 class 対応は、まず次を基本とする。

| upstream type | Java class |
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

現在は、少なくとも次を持つ class として扱う。

- `level`
- `scope`
- `message`

`level` と `scope` は enum 化候補もあったが、現在の実装後方針は `docs/step1-spec.md` を正本として扱う。

## 初期移植での class 作成順

POJO class は、STEP1 初期ではまず次の順で作成するのが自然だと整理した。

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

その後、必要に応じて次を順次追加する想定で整理した。

- `OutlineCodeModel` 系
- `WbsMaskModel`
- `ExtendedAttributeModel` 系
- `BaselineModel` 系
- `TimephasedDataModel` 系
- `ValidationIssue`

## 初期移植での field 詳細化方針

class を作る順序と、field を全部一気に埋めることは分けて考える。

- class の骨格は upstream に合わせて先に作る
- field は `docs/projectmodel-first-cut.md` の優先順に従って段階的に埋める
- 初期移植に入っていない field も、後で補いやすいよう class 自体の対応関係は先に固定する

## 初期移植時点で未確定だった点

次は初期移植時点では別途詳細化する項目として扱っていた。
現在の実装後方針は `docs/step1-spec.md`, `docs/remaining-migration-items.md`, `docs/upstream-class-mapping.md`, `docs/upstream-test-mapping.md` を正本として扱う。

- field を public にするか private + accessor にするか
- builder を使うかどうか
- `List` の具象型をどうするか
- enum 化する項目の範囲
- date/time/duration を `String` のまま持つか、Java 型へ変換するか
- optional number / boolean の扱い
