# 派生出力 / workbook import / calendar ギャップメモ

この文書は、`仕様上の制約` と `現状実装でまだ保持していないもの` を横断して見分けやすくするためのメモである。

## 見方

- `仕様上の制約`: 現時点で意図的に lossless を狙っていない
- `実装漏れ / 未保持`: モデルや他形式には概念があるが、当該入出力ではまだ保持できていない

## 派生出力

| 対象 | 位置づけ | 仕様上の制約 | 実装漏れ / 未保持 |
| --- | --- | --- | --- |
| `workbook JSON / XLSX` | 人が編集するための交換形式 | `限定列のみ` を部分更新する。全列編集や全量復元は狙わない | `Project.OutlineCodes / WBSMasks / ExtendedAttributes` は export 上見えても import 対象外 |
| `WBS XLSX` | 人向け派生出力 | 表示専用。構造忠実な workbook 交換形式ではない | workbook import/export の lossless 性は継承しない |
| `WBS Markdown` | 人向け派生出力 | tree / table / summary を読むための出力であり、完全保持形式ではない | task 全属性や calendar 実体は保持しない |
| `WBS SVG` | 人向け派生出力 | 可視化用。編集・交換形式ではない | dateband / holiday / display-range の詰めが弱い箇所が残る |
| `Mermaid gantt` | 人向け派生出力 | 共有・資料向けの片方向出力。完全依存表現は狙わない | 複雑 dependency は comment 化され、summary は section 名寄りの扱いに留まる |
| `CSV + ParentID` | 軽量交換形式 | `single CSV` を主系統にした最小逆変換。lossless round-trip は未確定 | `Project` 詳細、`Calendars`、`Baseline`、`TimephasedData`、assignment 詳細は完全復元しない |

## workbook import

### 限定列のみが仕様である範囲

現時点で import 対象とするのは次だけである。

- `Project`: `Name / Title / Author / Company / StartDate / FinishDate / CurrentDate / StatusDate / CalendarUID / MinutesPerDay / MinutesPerWeek / DaysPerMonth / ScheduleFromStart`
- `Tasks`: `Name / Start / Finish / Duration / PercentComplete / PercentWorkComplete / Milestone / Summary / Critical / CalendarUID / Predecessors / Notes`
- `Resources`: `Name / Group / MaxUnits / CalendarUID`
- `Assignments`: `Units / Work / PercentWorkComplete`
- `Calendars`: `Name / IsBaseCalendar / BaseCalendarUID`
- `NonWorkingDays`: `Name / Date / FromDate / ToDate / DayWorking`

### 列は見えるが import 対象外のもの

- `Project.OutlineCodes / WBSMasks / ExtendedAttributes`
- `Tasks.ID / OutlineLevel / OutlineNumber / WBS`
- `Calendars.WeekDays / Exceptions / WorkWeeks`

### `Tasks.Predecessors` の制約

- workbook import では `predecessorUid` の `,` 区切り一覧だけを戻す
- `type / linkLag` などの詳細は workbook では保持しない
- したがって `Predecessors` は `依存がある` ことの最小編集列であり、依存リンクの完全交換列ではない

## `Calendars` sheet の制約

`Calendars` sheet には `WeekDays / Exceptions / WorkWeeks` の列見出しがあるが、現時点では実体編集列ではない。

- export ではそれぞれの件数を表示する
- import では `Name / IsBaseCalendar / BaseCalendarUID` だけを反映する
- `Exceptions` の限定編集は `NonWorkingDays` sheet 側で扱う
- したがって `Calendars` sheet は、現時点では `read-only metadata + 最小属性編集` に近い

## `Patch JSON` の first cut 制約

### block 条件つき削除

- `delete_task`
  - 葉 task のみ
  - child task がある task は削除不可
  - assignment がある task は削除不可
  - successor から参照されている task は削除不可
- `delete_resource`
  - assignment が残っている resource は削除不可
- `delete_calendar`
  - project / resource / 他 calendar の `base_calendar_uid` などから参照されている calendar は削除不可

block された場合は warning を返し、実装は削除を続行しない。

### 未対応 key / 未対応 op

- 未対応 `op` は warning を返して無視する
- 対応済み `op` に含まれる未対応 key も warning を返して無視する
- first cut では `hard error` より `warning を返して継続` を優先するが、`uid` 欠落や参照不整合など意味が成立しないものは別途 warning / reject 対象になる

## `CSV + ParentID` の非 lossless 領域

現時点の最小逆変換で、落としてよい前提としているものは次である。

- `Project` 詳細
- `Calendars` 実体
- `Baseline`
- `TimephasedData`
- assignment 詳細
- cost 系の詳細

利用者に明示すべき点:

- `CSV + ParentID` は task 中心の軽量交換形式である
- `Resource` は task 行へ集約した補助表現であり、assignment の正本ではない
- 同名 resource 衝突、多重 assignment、`ResourceID` 正本連携が必要なら、将来の複数 CSV 構成を検討する
