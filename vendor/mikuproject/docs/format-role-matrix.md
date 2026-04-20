# 交換形式と派生出力の対応表

この表は、各形式が `lossless 交換形式` なのか、`片方向の人向け派生出力` なのかを一目で分かるようにするためのもの。

| 形式 | 主用途 | 区分 | 現時点の扱い |
| --- | --- | --- | --- |
| `MS Project XML` | 正本交換・round-trip | lossless 交換形式 | 最も意味の基軸に近い |
| workbook `XLSX` | 人が表で編集する交換形式 | 準 lossless / 限定列交換 | `限定列のみ` の部分更新を行う |
| workbook `JSON` | 人と AI が編集する交換形式 | 準 lossless / 限定列交換 | workbook `XLSX` と同じ列制約を持つ |
| `Patch JSON` | 既存 project への差分適用 | 差分交換形式 | first cut の op / key 制約つき |
| `project_draft_view` | 新規草案の生成AI入出力 | 草案交換形式 | 新規 project 草案向けで、既存 project の差分適用には使わない |
| `CSV + ParentID` | task 中心の軽量交換 | 非 lossless 交換形式 | 最小逆変換のみ。完全往復は未確定 |
| `WBS XLSX` | 表示・共有 | 片方向の人向け派生出力 | 表示専用 |
| `WBS Markdown` | 表示・共有 | 片方向の人向け派生出力 | tree / table / summary を読むための出力 |
| `WBS SVG` | 表示・共有 | 片方向の人向け派生出力 | 可視化用 |
| `Mermaid gantt` | 設計資料・共有 | 片方向の人向け派生出力 | 複雑 dependency は簡略化される |

## 補足

- `lossless 交換形式` でも、今の workbook import は全列反映ではなく `限定列のみ` を対象とする
- `片方向の人向け派生出力` は、読みやすさや共有を優先するため、内部情報の一部を落としてよい
- `CSV + ParentID` は交換形式だが、現時点では lossless を約束しない
