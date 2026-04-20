# XLSX Import の次段候補と回帰観点

この文書は、workbook `XLSX Import` を今後どの列から広げるかと、既存 editable 列の回帰をどの観点で継続確認するかを整理するためのメモである。

## 実装済みの優先候補

- `Assignments.Start / Finish`
- `Resources.StandardRate / OvertimeRate / CostPerUse`
- `Assignments.ActualWork / RemainingWork`
- `Assignments.ActualCost / RemainingCost / OvertimeWork / ActualOvertimeWork`

判断メモ:

- workbook import/export の `Assignments` は、期間・工数・実績系までひと通り反映できる段へ進んだ
- `actuals` 系は XML 正本にある値を workbook から部分更新する範囲として扱う
- ただし `CSV + ParentID` や主要 WBS report では、引き続き中心軸にしない

## 後段候補

- `Calendars.WeekDays / Exceptions / WorkWeeks`
- `ExtendedAttributes / Baseline / TimephasedData`

理由:

- 実績コストや overtime 系は意味が重い
- calendar 実体は workbook で縮退形しか扱っていない
- `ExtendedAttributes / Baseline / TimephasedData` は内部保持を先に固定する段階にある

## 回帰観点

### 1. 主要 editable 列が戻ること

最低限、次を継続確認する。

- `Project`
  - `Name`
  - `StartDate / FinishDate`
  - `MinutesPerDay / MinutesPerWeek / DaysPerMonth`
  - `ScheduleFromStart`
- `Tasks`
  - `Name`
  - `Start / Finish / Duration`
  - `PercentComplete / PercentWorkComplete`
  - `Milestone / Summary / Critical`
  - `Type / Priority`
  - `CalendarUID`
  - `ConstraintType / ConstraintDate / Deadline`
  - `Predecessors`
  - `Notes`
- `Resources`
  - `Name`
  - `Type / Initials`
  - `Group`
  - `MaxUnits`
  - `CalendarUID`
  - `StandardRate / OvertimeRate / CostPerUse`
  - `Work / ActualWork / RemainingWork`
  - `Cost / ActualCost / RemainingCost`
  - `PercentWorkComplete`
  - `WorkGroup / StandardRateFormat / OvertimeRateFormat`
- `Assignments`
  - `Start / Finish / StartVariance / FinishVariance`
  - `Delay / Milestone / WorkContour`
  - `Units`
  - `Work / Cost`
  - `ActualWork / RemainingWork`
  - `ActualCost / RemainingCost`
  - `OvertimeWork / ActualOvertimeWork`
  - `PercentWorkComplete`
- `Calendars`
  - `Name`
  - `IsBaseCalendar`
  - `BaseCalendarUID`
- `NonWorkingDays`
  - `Name / Date / FromDate / ToDate / DayWorking`

### 2. 真偽値列が `○ / ー` を通って戻ること

最低限、次を継続確認する。

- `Tasks.Milestone`
- `Tasks.Summary`
- `Tasks.Critical`
- `Project.ScheduleFromStart`
- `Calendars.IsBaseCalendar`
- `NonWorkingDays.DayWorking`

### 3. 非 editable 列が壊れないこと

最低限、次は `表示のみ` のまま保持されることを確認する。

- `Tasks.ID / OutlineLevel / OutlineNumber / WBS`
- `Project.OutlineCodes / WBSMasks / ExtendedAttributes`
- `Calendars.WeekDays / Exceptions / WorkWeeks`

### 4. fixture 回帰

最低限、次の fixture を継続確認に使う。

- `dependency.xml`
  - predecessor
  - resource / assignment
  - calendar 参照
- `hierarchy.xml`
  - summary / outline
- `workbook-import-sample.json`
  - workbook merge の最小更新

## テストの置き場所

主な確認位置:

- `ProjectXlsxTest`
  - workbook 単体の editable 列回帰
- `CoreApiWorkbookTest`
  - workbook wrapper の回帰
- `MikuprojectCliTest`
  - CLI からの export / import / merge 導線の回帰

## 判断

- 新しい列を増やす前に、既存 editable 列の回帰観点を固定する
- `Assignments` の期間・実績系と `Resources` の rate/cost は、workbook import の既存回帰対象として維持する
- 重い列は `XML では保持、workbook では段階導入` の原則を崩さず、次は calendar 実体と `ExtendedAttributes / Baseline / TimephasedData` を分けて扱う
