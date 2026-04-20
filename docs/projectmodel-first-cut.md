# ProjectModel Initial Migration

## 目的

この文書は、Java 版 `mikuproject` の STEP1 において、`ProjectModel` の初期移植としてどこから実装するかを整理するためのメモである。

前提として、Java 版でも最終的には Node.js 版 upstream の `ProjectModel` 思想を引き継ぐ。
この文書は対象を削るためのものではなく、実装順序を明確にするためのものである。

## 現在の位置づけ

この文書は STEP1 初期に `ProjectModel` の実装順序を決めるための設計メモとして残す。
現在は、主要 POJO と codec / workbook / patch / xlsx 連携の導線が Java 側に存在するため、この文書を未実装 field の追加リストとしては扱わない。

現フェーズで不足を見つけた場合は、新規機能追加ではなく、既存仕様の不足または upstream 差分として `docs/remaining-migration-items.md`, `docs/upstream-class-mapping.md`, `docs/upstream-test-mapping.md` のどこに反映するかを確認する。

## 基本方針

- 最終的な目標は、Node.js 版 upstream と同等の `ProjectModel` を目指す
- ただし Java 版 STEP1 の実装は、`MS Project XML -> ProjectModel -> MS Project XML` の意味的ラウンドトリップを優先する
- そのため、初期移植では「まず round-trip の核になるフィールド」から POJO 化する
- 後続で広げる場合も、Node.js 版 upstream の型構造と責務に寄せる

## upstream の基本構造

Node.js 版 upstream の `ProjectModel` は、少なくとも次の 5 要素を持つ。

- `project`
- `tasks`
- `resources`
- `assignments`
- `calendars`

Java 版 STEP1 でも、この構造をそのまま基本骨格とする。

## 初期移植の優先対象

初期移植では、次の順序で実装を優先する。

1. `Project`
2. `Tasks`
3. `Resources`
4. `Assignments`
5. `Calendars`

この順序は、`MS Project XML` の round-trip において中心になる情報を先に固めるためのものである。

## `Project` 初期移植

まずは、Node.js 版 upstream の `project` から、round-trip の核になる次の項目を優先する。

- `name`
- `startDate`
- `finishDate`
- `scheduleFromStart`
- `currentDate`
- `calendarUID`

次の項目も、Node.js 版 upstream が `ProjectModel` に持っているため、STEP1 の後半で順次取り込む対象とする。

- `title`
- `author`
- `company`
- `creationDate`
- `lastSaved`
- `saveVersion`
- `defaultStartTime`
- `defaultFinishTime`
- `minutesPerDay`
- `minutesPerWeek`
- `daysPerMonth`
- `statusDate`
- `weekStartDay`
- `workFormat`
- `durationFormat`
- `currencyCode`
- `currencyDigits`
- `currencySymbol`
- `currencySymbolPosition`
- `fyStartDate`
- `fiscalYearStart`
- `criticalSlackLimit`
- `defaultTaskType`
- `defaultFixedCostAccrual`
- `defaultStandardRate`
- `defaultOvertimeRate`
- `defaultTaskEVMethod`
- `newTaskStartDate`
- `newTasksAreManual`
- `newTasksEffortDriven`
- `newTasksEstimated`
- `actualsInSync`
- `editableActualCosts`
- `honorConstraints`
- `insertedProjectsLikeSummary`
- `multipleCriticalPaths`
- `taskUpdatesResource`
- `updateManuallyScheduledTasksWhenEditingLinks`
- `outlineCodes`
- `wbsMasks`
- `extendedAttributes`

## `Task` 初期移植

まずは、Node.js 版 upstream の `TaskModel` から、WBS と依存関係の round-trip に必要な次の項目を優先する。

- `uid`
- `id`
- `name`
- `outlineLevel`
- `outlineNumber`
- `wbs`
- `start`
- `finish`
- `duration`
- `milestone`
- `summary`
- `percentComplete`
- `calendarUID`
- `predecessors`
- `notes`

次の項目は、Node.js 版 upstream に存在し、STEP1 の後半で順次広げる対象とする。

- `type`
- `priority`
- `actualStart`
- `actualFinish`
- `deadline`
- `startVariance`
- `finishVariance`
- `work`
- `workVariance`
- `totalSlack`
- `freeSlack`
- `cost`
- `actualCost`
- `remainingCost`
- `remainingWork`
- `actualWork`
- `critical`
- `percentWorkComplete`
- `constraintType`
- `constraintDate`
- `extendedAttributes`
- `baselines`
- `timephasedData`

`predecessors` では、少なくとも次を upstream と同じ語彙で持つ。

- `predecessorUid`
- `type`
- `linkLag`

## `Resource` 初期移植

まずは、Node.js 版 upstream の `ResourceModel` から、task との関連と基本的な割当解釈に必要な次の項目を優先する。

- `uid`
- `id`
- `name`
- `group`
- `maxUnits`
- `calendarUID`

次の項目は、STEP1 の後半で順次広げる対象とする。

- `type`
- `initials`
- `workGroup`
- `standardRate`
- `standardRateFormat`
- `overtimeRate`
- `overtimeRateFormat`
- `costPerUse`
- `work`
- `actualWork`
- `remainingWork`
- `cost`
- `actualCost`
- `remainingCost`
- `percentWorkComplete`
- `extendedAttributes`
- `baselines`
- `timephasedData`

## `Assignment` 初期移植

まずは、Node.js 版 upstream の `AssignmentModel` から、task と resource の結合に必要な次の項目を優先する。

- `uid`
- `taskUid`
- `resourceUid`
- `units`
- `work`
- `percentWorkComplete`

次の項目は、STEP1 の後半で順次広げる対象とする。

- `start`
- `finish`
- `startVariance`
- `finishVariance`
- `delay`
- `milestone`
- `workContour`
- `cost`
- `actualCost`
- `remainingCost`
- `overtimeWork`
- `actualOvertimeWork`
- `actualWork`
- `remainingWork`
- `extendedAttributes`
- `baselines`
- `timephasedData`

## `Calendar` 初期移植

まずは、Node.js 版 upstream の `CalendarModel` から、project / task / resource 参照の基盤になる次の項目を優先する。

- `uid`
- `name`
- `isBaseCalendar`
- `baseCalendarUID`

次の項目は、STEP1 の後半で順次広げる対象とする。

- `isBaselineCalendar`
- `weekDays`
- `exceptions`
- `workWeeks`

関連する下位要素の語彙も、upstream に寄せる。

- `WeekDayModel`: `dayType`, `dayWorking`, `workingTimes`
- `WorkingTimeModel`: `fromTime`, `toTime`
- `CalendarExceptionModel`: `name`, `fromDate`, `toDate`, `dayWorking`, `workingTimes`
- `WorkWeekModel`: `name`, `fromDate`, `toDate`, `weekDays`

## 初期移植の考え方

ここでいう初期移植は、「Java 版 STEP1 で最初に POJO と codec を成立させるための優先対象」である。

したがって、次のように扱う。

- 初期移植に入っている項目は、最初の Java 実装で優先的に POJO 化する
- 初期移植に入っていない項目も、Node.js 版 upstream に存在する以上、将来的な移植対象である
- Java 版独自都合で upstream の型構造を縮退させた恒久仕様にはしない

## 次の詳細化項目

以下は初期移植時点の詳細化項目である。
現在の実装後方針は `docs/step1-spec.md` と追随運用文書群を正本として扱う。

- Java 側 POJO の class 分割案
- `msproject-codec` に対応する Java 側 import/export の責務分割
- 各フィールドの Java 型
- `null` / 未設定値の扱い
- date/time と duration の表現方法
