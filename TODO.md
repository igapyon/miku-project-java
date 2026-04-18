# TODO

## 基本方針

- [x] `vendor/mikuproject` に Node.js upstream を subtree で保持する
- [x] `workplace/` は Git 管理外のローカル作業スペースとして扱う
- [x] Java package の基底名を `jp.igapyon.mikuproject` にする
- [x] Java のターゲットを `1.8` にする
- [x] Maven ベースで開始する
- [x] Node.js upstream の構成を尊重し、Java 側の責務分割も対応関係を追いやすくする

## 構成方針

- [ ] Node.js upstream の主要ファイルと Java 側クラス/package の対応表を作る
- [ ] Java 側は過度な Java 流再編を避け、upstream のファイル境界を尊重して分割する
- [ ] upstream 1ファイルに対して Java 側で複数クラスへ分ける場合も、追跡しやすい命名規則を決める
- [ ] upstream 更新追随時の確認単位を、Java 側でも対応ファイル単位で辿れるようにする

## 仕様検討

- [x] Java 版の first cut の目的を明文化する
- [x] `MS Project XML` と `ProjectModel` の位置づけを Java 版でも定義する
- [x] Java 版 `ProjectModel` の責務と範囲を決める
- [x] first cut で扱う要素範囲を決める
- [x] CLI を first cut に含めるか決める
- [ ] Node 版 upstream から継承する仕様と、Java 版で一旦保留する仕様を切り分ける

## Node -> Java 移植論点

- [x] `ProjectModel` を `Map` ベースで持つか、POJO で持つか方針を決める
- [ ] AI JSON / patch JSON のデータ構造をどう表現するか決める
- [ ] `null` / 未設定 / 空文字 / 0 の扱いを整理する
- [ ] parse / import / validate 系のエラー返却方針を決める
- [ ] text / bytes / stream の I/O API 方針を決める
- [ ] XML 処理を DOM / SAX / StAX のどれで進めるか検討する
- [ ] 日付時刻の型とタイムゾーン方針を決める
- [ ] 数値型の方針を決める
- [ ] 文字コードを UTF-8 前提でどこまで明示するか決める
- [x] upstream 比較を前提にしたテスト方針を決める
- [ ] Node 側 options object を Java でどう表現するか決める
- [ ] package 依存方向とモジュール境界を整理する

## リポジトリ整備

- [ ] Java 側の package 構成案を決める
- [ ] 最小の Java エントリポイントを置く
- [x] `src/test/java/` を用意する
- [x] JUnit Jupiter を Maven に追加する
- [ ] upstream 対応を意識したテスト class 命名規則を決める
- [ ] upstream の主要テストと Java 側テストの対応表を作る
- [ ] `README.md` に Java 版の目的を追記する
- [ ] upstream 対応方針を `README.md` に追記する

## upstream 連絡事項

- [ ] 該当なし

## 次の候補

- [ ] `docs/` を作成する
- [x] Java 版 STEP 1 仕様メモを書く
- [x] `MS Project XML -> ProjectModel` の最小 import 設計を検討する
- [x] Java 側 `ProjectModel` POJO の class 分割案を書く
