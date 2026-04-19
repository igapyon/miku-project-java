# TODO

この文書には、`mikuproject` 本体の未完了作業だけを書く。概要説明や仕様判断は `README.md` と `docs/spec.md` に寄せる。

## 使い方

- `High`: 利用者誤解や仕様不一致を減らすため先に片付ける
- `Medium`: lossless 性や保持範囲を段階的に広げる
- `Low`: 表示改善や次段拡張

## High

- 派生出力 / workbook import / calendar の棚卸し結果を踏まえ、`仕様上の制約` と `実装漏れ` を一覧化したギャップメモを作る
- workbook / CSV / Mermaid / Markdown / SVG / WBS XLSX の各出力について、`lossless 交換形式` と `片方向の人向け派生出力` を明示した対応表を docs に追加する
- workbook `XLSX / JSON import` について、`限定列のみ` が仕様である範囲と、未対応のまま残っている列を分けて明文化する
- workbook `Tasks.Predecessors` について、`UID` 列挙だけを戻す現状と、`type / lag` を保持しない制約を docs に明記する
- workbook `Project` について、`OutlineCodes / WBSMasks / ExtendedAttributes` は export 上見えても import 対象外である点を docs に明記する
- `Calendars` sheet について、`WeekDays / Exceptions / WorkWeeks` の列見出しに対して、現状は件数確認用であり lossless 編集用ではない点を明記する
- `Patch JSON` の first cut 制約について、`delete_task / delete_resource / delete_calendar` が block する条件を docs と diagnostics で一貫して示す
- `Patch JSON` の未対応 key / 未対応 op を warning で無視する現状について、どこまでを許容仕様として固定するか整理する
- `CSV + ParentID` の最小逆変換で落としてよい項目と、lossless でないことを利用者へ明示すべき項目を整理する

## Medium

- `WbsDateband` は `weekDays / exceptions` を読む一方で workbook import/export がそこまで運べていないため、calendar ロジックと workbook 表現の責務ずれを解消する
- `NonWorkingDays` import について、`workingTimes` など exception detail を落としている点を整理し、どこまで戻すか優先順位を決める
- `Calendars` の workbook export について、件数表示ではなく実体編集へ広げるか、このまま read-only metadata に留めるか判断する
- `update_calendar` / `add_calendar` の first cut が `name / is_base_calendar / base_calendar_uid` に留まる点を、calendar 実体編集の非対応範囲とあわせて docs に明記する
- `ExtendedAttributes` について、preview/代表値確認で留める範囲と、lossless 保持・import/export 対応へ進める範囲を分けて優先順位を決める
- `Baseline / TimephasedData` について、validation で存在だけ追う段階に留めるのか、内部保持だけ先に始めるのか、report/export まで含めて扱うのか段階案を作る
- `Assignment` について、`ActualWork / RemainingWork / ActualCost / RemainingCost / OvertimeWork / ActualOvertimeWork` をどの入出力で保持し、どこでは意図的に落とすか整理する
- `Calendar` について、`IsBaselineCalendar` を内部保持だけに留めるのか、validation・preview・交換形式へ出すのかを決める
- workbook import の次段候補として、少なくとも次の列を優先順位つきで整理する
  - 優先候補: `Resources.StandardRate / OvertimeRate / CostPerUse`
  - 優先候補: `Assignments.Start / Finish`
- `XLSX Import` の実地回帰観点を明文化し、主要 editable 列や真偽値列が戻ることを継続確認する

## Low

- `Mermaid` 出力は Markdown / 設計資料向けに残しつつ、見た目を制御しやすい `WBS SVG` 描画を別系統で追加するか検討する
- `Mermaid gantt` について、summary task を section 名としてだけ使う現状で十分か、summary bar 相当の扱いが必要か整理する
- `WBS Markdown` について、派生出力として意図的に落としている情報と、将来拡張したい項目を切り分けて整理する
- `CSV + ParentID` について、`single CSV` を主系統に維持する条件と、`tasks.csv / resources.csv / assignments.csv` へ進む判断条件を仕様として固定する
- `CSV + ParentID` の `Resource` 集約表現について、同名 resource 衝突や多重 assignment をどこまで warning で見せるか整理する
- WBS workbook の見た目改善と、構造忠実 workbook との責務分離を保つ
- WBS について、完了タスクの表示 / 非表示を切り替えるオプションを追加する
- 将来検討: WBS workbook について、表示専用列と Excel 再利用向けの機械利用列（hidden 列）の分離が必要か整理する
- SVG のガントチャートについて、前後関係を dependency connector として表示するか検討する
- `Daily` 表示の日ごとの横幅を、もう少し狭くできるか検討する
- `WBS SVG` について、今の既定である `近接ラベル` 表示だけを残し、左側にテキストを描画する `一覧ラベル` モードは将来的に廃止したい
- WBS Markdown の `プロジェクト情報` / `サマリ` / `WBS ツリー` / `WBS テーブル` をどう出すか sample ベースで固める
- `project summary markdown` のような、WBS 以外の Markdown 出力拡張を検討する
- `phase summary markdown` のような scoped Markdown 出力を追加するか検討する
- `WBS記述書 Markdown` 出力を追加し、task ごとの説明を別 Markdown として保存できるようにする
- `WBS記述書` 用 `Task.ExtendedAttribute` の最小項目として `TaskPurpose / TaskDeliverable / TaskOutOfScope / TaskDoneDefinition / TaskOwner` を扱う
- `WBS記述書 Markdown` では、長文補足を `Task.Notes` から出す
- `WBS記述書 Markdown` の sample 出力を作成し、1 task 1 節構成で読みやすいか確認する

## 補助

- 最優先: サンプルデータを更新し、利用者の好みに合う題材・構造・見た目へ見直す
- `local-data/` 配下のファイルを、参照用・検証用・生成物で整理する
- `local-data/` に置くべきでない生成物や一時ファイルがないか見直す
- 構造変更を再開するときは、区切りごとに `npm run build:full` を回し、`tests/mikuproject-cli.test.js` の実行時間も継続確認する
