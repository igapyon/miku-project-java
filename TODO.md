# TODO

## 使い分け

- この `TODO.md` は Java 版 CLI 中心の移植計画を扱う
- `vendor/mikuproject/docs/TODO.md` は upstream 機能差分、仕様制約、入出力の未整理事項を扱う

## 最重要

### 現在フェーズの運用方針

- 新規機能追加ではなく、既存実装の確認、TODO / docs 整理、移植済み範囲の検証や差分確認に限定して進める
- `docs/remaining-migration-items.md`, `docs/upstream-class-mapping.md`, `docs/upstream-test-mapping.md`, `docs/upstream-followup-log.md` の整合を保つ
- 既存 command / API / fixture 回帰の不足が見つかった場合は、機能追加ではなく既存仕様の不足またはバグとして扱う

### 直近の限定作業

- [x] 残作業を新規機能追加ではなく、既存範囲の確認 / docs 整理 / upstream 差分確認へ絞る
- [x] `README.md`, `docs/remaining-migration-items.md`, `docs/upstream-class-mapping.md`, `docs/upstream-followup-log.md`, `docs/msprojectxml-import-design.md` の現在フェーズ表現を揃える

### 今週

- [x] Java 版の対象を `CLI` 中心に固定し、対象範囲を文書で揃える
  - [x] `README.md` / `docs/remaining-migration-items.md` / `TODO.md` で、Java 版の対象範囲を `CLI` 中心に揃える
  - [x] `対応済み` `保守確認` の 2 区分で、現時点の領域分類を定義する
  - [x] `straight conversion` を原則とし、Java-first 再設計は後段で扱う方針を文書で固定する
- [x] core / CLI / report の 3 系統で、移植計画を作る
  - [x] core 系について、upstream file 単位で `対応済み / 保守確認 / 保留` を棚卸しする
  - [x] CLI 系について、既存 command と upstream command/導線との差分を棚卸しする
  - [x] report 系について、`lossless 交換形式` と `人向け派生出力` の役割を切り分けたうえで保守確認観点を棚卸しする

### 次

- [x] upstream 更新追随時の確認単位を、Java 側でも対応ファイル単位で辿れるようにする
- [x] Node 版 upstream から継承する仕様と、Java 版でまだ保留の仕様を切り分ける

### 後で

- [x] Java CLI entry の役割を最小 entrypoint から全機能 entrypoint へ拡張する段取りを決める

## 近い順

### 1. Core の精度を上げる

- [x] `null` / 未設定 / 空文字 / `0` の扱いを整理する
- [x] parse / import / validate 系のエラー返却方針を決める
- [x] Node 側 options object を Java でどう表現するか決める
- [x] package 依存方向とモジュール境界を整理する

### 2. Java 側の配置を固める

- [x] Java 側は過度な Java 流再編を避け、upstream のファイル境界を尊重して分割する
- [x] upstream 1 ファイルに対して Java 側で複数クラスへ分ける場合も、追跡しやすい命名規則を決める
- [x] Java 側の package 構成案を決める

### 3. CLI / runtime を整える

- [x] text / bytes / stream の I/O API 方針を決める
- [x] 日付時刻の型とタイムゾーン方針を決める
- [x] 数値型の方針を決める
- [x] 文字コードを UTF-8 前提でどこまで明示するか決める

## テスト

- [x] upstream 比較を前提にしたテスト方針を決める
- [x] `src/test/java/` を用意する
- [x] JUnit Jupiter を Maven に追加する
- [x] upstream 対応を意識したテスト class 命名規則を決める
- [x] upstream の主要テストと Java 側テストの対応表を作る
- [x] upstream fixture 比較を、core / workbook / report へ段階拡張する

## ドキュメント

- [x] `README.md` に Java 版の目的を追記する
- [x] upstream 対応方針を `README.md` に追記する
- [x] `README.md` と `docs/remaining-migration-items.md` の進捗前提を定期的に同期する
- [x] Java 版移植の現在地を、core 完了率と CLI/report 完了率で分けて示す

## 完了済みメモ

- [x] `vendor/mikuproject` に Node.js upstream を subtree で保持する
- [x] `workplace/` は Git 管理外のローカル作業スペースとして扱う
- [x] Java package の基底名を `jp.igapyon.mikuproject` にする
- [x] Java のターゲットを `1.8` にする
- [x] Maven ベースで開始する
- [x] Node.js upstream の構成を尊重し、Java 側の責務分割も対応関係を追いやすくする
- [x] Java 版の初期移植の目的を明文化する
- [x] `MS Project XML` と `ProjectModel` の位置づけを Java 版でも定義する
- [x] Java 版 `ProjectModel` の責務と範囲を決める
- [x] 初期移植で扱う要素範囲を決める
- [x] CLI を初期移植に含めるか決める
- [x] `ProjectModel` を `Map` ベースで持つか、POJO で持つか方針を決める
- [x] ソースヘッダーにライセンス表記を追加する
