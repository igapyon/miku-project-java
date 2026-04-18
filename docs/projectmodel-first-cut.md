# ProjectModel First Cut

## 目的

この文書は、Java 版 `mikuproject` の STEP1 において、`ProjectModel` の first cut としてどこから実装するかを整理するためのメモである。

前提として、Java 版でも最終的には Node.js 版 upstream の `ProjectModel` 思想を引き継ぐ。
この文書は対象を削るためのものではなく、実装順序を明確にするためのものである。

## 基本方針

- 最終的な目標は、Node.js 版 upstream と同等の `ProjectModel` を目指す
- ただし Java 版 STEP1 の実装は、`MS Project XML -> ProjectModel -> MS Project XML` の意味的ラウンドトリップを優先する
- そのため、first cut では「まず round-trip の核になるフィールド」から POJO 化する
- 後続で広げる場合も、Node.js 版 upstream の型構造と責務に寄せる

## upstream の基本構造

Node.js 版 upstream の `ProjectModel` は、少なくとも次の 5 要素を持つ。

- `project`
- `tasks`
- `resources`
- `assignments`
- `calendars`

Java 版 STEP1 でも、この構造をそのまま基本骨格とする。

## first cut の優先対象

first cut では、次の順序で実装を優先する。

1. `Project`
2. `Tasks`
3. `Resources`
4. `Assignments`
5. `Calendars`

この順序は、`MS Project XML` の round-trip において中心になる情報を先に固めるためのものである。

## `Project` first cut

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

## `Task` first cut

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

## `Resource` first cut

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

## `Assignment` first cut

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

## `Calendar` first cut

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

## first cut の考え方

ここでいう first cut は、「Java 版 STEP1 で最初に POJO と codec を成立させるための優先対象」である。

したがって、次のように扱う。

- first cut に入っている項目は、最初の Java 実装で優先的に POJO 化する
- first cut に入っていない項目も、Node.js 版 upstream に存在する以上、将来的な移植対象である
- Java 版独自都合で upstream の型構造を縮退させた恒久仕様にはしない

## 次の詳細化項目

- Java 側 POJO の class 分割案
- `msproject-codec` に対応する Java 側 import/export の責務分割
- 各フィールドの Java 型
- `null` / 未設定値の扱い
- date/time と duration の表現方法
