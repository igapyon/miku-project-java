# calendar と workbook 表現の責務ずれメモ

この文書は、`WbsDateband` が読める calendar 情報と、workbook `XLSX / JSON` が実際に運べる情報のずれを整理するためのメモである。

## 現状

### calendar ロジック側

`WbsDateband` は、少なくとも次を読む。

- `Calendar.WeekDays`
- `Calendar.Exceptions`
- `Calendar.BaseCalendarUID`
- `CalendarException.DayWorking`
- `CalendarException.WorkingTimes`

扱い方:

- 週次の非稼働日は `WeekDays` から判定する
- 祝日などの非稼働日例外は `Exceptions` から展開する
- `workingTimes` が入っている exception は、終日非稼働日としては扱わない
- `BaseCalendarUID` をたどって project calendar の週次非稼働日を解決する

### workbook 側

workbook の current implementation は次のとおり。

- `Calendars` sheet
  - export: `WeekDays / Exceptions / WorkWeeks` の件数だけを出す
  - import: `Name / IsBaseCalendar / BaseCalendarUID` だけを反映する
- `NonWorkingDays` sheet
  - export: `Name / Date / FromDate / ToDate / DayWorking`
  - import: 同じ列だけを戻す
  - `workingTimes` は運ばない
- `WeekDays / WorkWeeks`
  - workbook import/export では実体を運ばない

## ずれの正体

責務ずれは次の 2 点に分かれる。

### 1. WBS 表示に必要な情報を workbook が運びきれない

- `WbsDateband` は `WeekDays` を見て週次の非稼働日を決める
- しかし workbook import/export は `WeekDays` 実体を保持しない
- そのため workbook を経由すると、calendar の週次ルールは XML 正本ほど忠実には保てない

### 2. exception の意味を workbook が粗くしか運ばない

- `WbsDateband` は `exception.workingTimes` がある日を、終日非稼働日から除外する
- しかし `NonWorkingDays` sheet は `workingTimes` を保持しない
- そのため workbook を経由した exception は、`dayWorking` と日付帯だけの粗い表現になる

## 現時点の整理方針

### 方針 1. calendar の意味正本は XML に置く

- `MS Project XML` を calendar 実体の意味正本とする
- workbook は calendar の full round-trip 形式ではなく、限定編集と確認用 metadata に留める

### 方針 2. workbook の `Calendars` は read-only metadata 寄りとみなす

- `Name / IsBaseCalendar / BaseCalendarUID` の最小編集は許容する
- `WeekDays / Exceptions / WorkWeeks` は件数確認用 metadata とみなす
- calendar 実体の編集 UI や lossless workbook 編集は次段判断とする

### 方針 3. `NonWorkingDays` は終日例外の最小編集に留める

- `Name / Date / FromDate / ToDate / DayWorking` を workbook で扱う
- `workingTimes` を持つ部分稼働例外や複雑な例外時間帯は、現時点では workbook へ広げない
- したがって `NonWorkingDays` は `full exception editor` ではなく、`終日非稼働日中心の簡易編集` とみなす

## 次段の優先順位

### 優先 1

- `NonWorkingDays` が `workingTimes` を落とすことを docs で明示する
- workbook を経由した calendar が `WbsDateband` の入力としては縮退形になることを docs で明示する

### 優先 2

- `update_calendar / add_calendar` の first cut が `name / is_base_calendar / base_calendar_uid` のみであることを、calendar 実体編集の非対応範囲と合わせて docs へ寄せる

### 優先 3

- 将来、`Calendars` を件数表示から実体編集へ広げるか判断する
- 広げる場合の最初の候補は `WeekDays` ではなく `NonWorkingDays` の detail 拡張とする

## 判断メモ

- いま優先すべきなのは `calendar workbook editor を作ること` ではなく、`どこまでが縮退形かを誤解なく示すこと`
- `WbsDateband` が読む情報量と workbook が運ぶ情報量は意図的に非対称である
- この非対称を埋める場合も、まずは docs と validation / preview から進め、full 編集は後段に回す
