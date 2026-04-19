# TODO

## 使い分け

- この `TODO.md` は Java 版全機能移植の段階計画を扱う
- `vendor/mikuproject/docs/TODO.md` は upstream 機能差分、仕様制約、入出力の未整理事項を扱う

## 最重要

### 今週

- [ ] Node.js 版の全機能を最終的な移植対象とし、STEP ごとの暫定スコープと最終到達点を分けて管理する
- [ ] core / CLI / report / Web UI の 4 系統で、全機能移植の段階計画を作る
- [ ] Web UI / browser main 系を含む移植順序を決める

### 次

- [ ] upstream 更新追随時の確認単位を、Java 側でも対応ファイル単位で辿れるようにする
- [ ] Node 版 upstream から継承する仕様と、Java 版でまだ保留の仕様を切り分ける
- [ ] Web UI / browser main 系の Java 側配置方針を決める

### 後で

- [ ] Java CLI entry の役割を最小 entrypoint から全機能 entrypoint へ拡張する段取りを決める
- [ ] `main*.ts` 群の依存関係を整理し、Java 側でどこから移植開始するか決める
- [ ] browser main 系の first slice を決める

## 近い順

### 1. Core の精度を上げる

- [ ] `null` / 未設定 / 空文字 / `0` の扱いを整理する
- [ ] parse / import / validate 系のエラー返却方針を決める
- [ ] Node 側 options object を Java でどう表現するか決める
- [ ] package 依存方向とモジュール境界を整理する

### 2. Java 側の配置を固める

- [ ] Java 側は過度な Java 流再編を避け、upstream のファイル境界を尊重して分割する
- [ ] upstream 1 ファイルに対して Java 側で複数クラスへ分ける場合も、追跡しやすい命名規則を決める
- [ ] Java 側の package 構成案を決める

### 3. CLI / runtime を整える

- [ ] text / bytes / stream の I/O API 方針を決める
- [ ] 日付時刻の型とタイムゾーン方針を決める
- [ ] 数値型の方針を決める
- [ ] 文字コードを UTF-8 前提でどこまで明示するか決める

### 4. Web UI 移植を始める

- [ ] Web UI の state / import / export / preview の境界を、Java 側でも追跡可能にする

## テスト

- [x] upstream 比較を前提にしたテスト方針を決める
- [x] `src/test/java/` を用意する
- [x] JUnit Jupiter を Maven に追加する
- [x] upstream 対応を意識したテスト class 命名規則を決める
- [x] upstream の主要テストと Java 側テストの対応表を作る
- [ ] upstream fixture 比較を、core / workbook / report / Web UI へ段階拡張する

## ドキュメント

- [x] `README.md` に Java 版の目的を追記する
- [x] upstream 対応方針を `README.md` に追記する
- [ ] `README.md` と `docs/remaining-migration-items.md` の進捗前提を定期的に同期する
- [ ] Java 版全機能移植の現在地を、core 完了率と full port 完了率で分けて示す

## 完了済みメモ

- [x] `vendor/mikuproject` に Node.js upstream を subtree で保持する
- [x] `workplace/` は Git 管理外のローカル作業スペースとして扱う
- [x] Java package の基底名を `jp.igapyon.mikuproject` にする
- [x] Java のターゲットを `1.8` にする
- [x] Maven ベースで開始する
- [x] Node.js upstream の構成を尊重し、Java 側の責務分割も対応関係を追いやすくする
- [x] Java 版の first cut の目的を明文化する
- [x] `MS Project XML` と `ProjectModel` の位置づけを Java 版でも定義する
- [x] Java 版 `ProjectModel` の責務と範囲を決める
- [x] first cut で扱う要素範囲を決める
- [x] CLI を first cut に含めるか決める
- [x] `ProjectModel` を `Map` ベースで持つか、POJO で持つか方針を決める
- [x] ソースヘッダーにライセンス表記を追加する
