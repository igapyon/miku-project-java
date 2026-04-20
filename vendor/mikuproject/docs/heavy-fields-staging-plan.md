# ExtendedAttributes / Baseline / TimephasedData / 実績系フィールドの段階案

この文書は、内部モデルには存在するが workbook / CSV / report / preview ではまだ全面対応していない重めのフィールド群について、どこまでを現段階で扱い、どこからを次段へ送るかを整理するためのメモである。

## 対象

- `ExtendedAttributes`
- `Baseline`
- `TimephasedData`
- `Assignment` の実績系フィールド
- `Calendar.IsBaselineCalendar`

## 現状

### すでに入っている段

- `MS Project XML -> ProjectModel -> MS Project XML` の round-trip
- `MsProjectValidate` による最小 validation
- 一部 preview / 件数表示

### まだ広げていない段

- workbook `XLSX / JSON` の編集反映
- `CSV + ParentID` の保持
- report / Markdown / SVG / Mermaid への露出
- AI 編集形式での全面対応

## `ExtendedAttributes`

### 現段階で固定する範囲

- `Project / Task / Resource / Assignment` の `ExtendedAttributes` は内部保持する
- XML import/export では保持する
- `Project` については preview / workbook export 上で件数や代表値確認に留めてよい

### 次段へ送る範囲

- workbook import での lossless 編集
- task / resource / assignment ごとの `ExtendedAttributes` の preview 全面対応
- report 出力へ `ExtendedAttributes` を積極露出すること

### 優先順位

1. `Project` の代表値確認
2. task ごとの最小 preview
3. workbook import/export の拡張
4. report / AI 編集系での利用

## `Baseline / TimephasedData`

### 現段階で固定する範囲

- 内部モデルでは保持する
- XML import/export では保持する
- validation では数値範囲や `Start <= Finish` のような最小整合を確認する

### 次段へ送る範囲

- workbook 編集
- CSV 編集
- WBS report への表示
- AI view / Patch JSON からの直接編集

### 段階案

1. 内部保持 + XML round-trip + validation
2. preview / summary で件数や代表値だけ見せる
3. workbook 交換形式へ広げるか判断する
4. 必要なら report / export へ露出する

判断メモ:

- `Baseline / TimephasedData` は重要だが構造が重い
- 先に `内部保持` と `validation` を安定させ、編集形式への展開は別段にするのが自然

## `Assignment` の実績系フィールド

対象:

- `ActualWork`
- `RemainingWork`
- `ActualCost`
- `RemainingCost`
- `OvertimeWork`
- `ActualOvertimeWork`

### 現段階で固定する範囲

- XML import/export では保持する
- validation では負値や空文字などの最小整合を確認する
- workbook export/import では `ActualWork / RemainingWork / ActualCost / RemainingCost / OvertimeWork / ActualOvertimeWork` を部分更新対象として扱ってよい

### 現段階で意図的に落とす範囲

- `CSV + ParentID` では保持しない
- report では主要 WBS 可視化の軸にしない

### 次段候補

優先候補:

- report / preview での実績コスト表示をどこまで広げるか
- AI 編集形式へ `Assignments` 実績系を露出するか

後段候補:

- `CSV + ParentID` での扱い
- `ExtendedAttributes / Baseline / TimephasedData` と合わせた重い列の交換形式整理

## `Calendar.IsBaselineCalendar`

### 現段階で固定する範囲

- 内部保持する
- XML import/export では保持する
- validation で `IsBaselineCalendar` と `IsBaseCalendar` の整合を確認する

### 現段階で広げない範囲

- workbook 編集
- CSV 編集
- report 表示

### 判断

- `IsBaselineCalendar` は当面 `内部保持 + validation` に留める
- 交換形式や preview へ出す必要が具体化したときに次段へ回す

## まとめ

- `ExtendedAttributes` は `代表値確認までは前進、全面編集は後段`
- `Baseline / TimephasedData` は `内部保持 + validation` を先に固定
- `Assignment` 実績系は `XML では保持、workbook / CSV では意図的に絞る`
- `Calendar.IsBaselineCalendar` は `内部保持 + validation` に留める
