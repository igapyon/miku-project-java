# STEP1 Spec

## 目的

Java 版 `mikuproject` の STEP1 では、`MS Project XML` の意味的ラウンドトリップを成立させることを目的とする。

ここでいう意味的ラウンドトリップとは、少なくとも次を満たすことである。

- `MS Project XML` を読める
- 必要な情報を内部モデルへ落とせる
- 内部モデルから `MS Project XML` を再生成できる
- 再生成した `MS Project XML` を Java 版 `mikuproject` 自身で再読込できる
- 主要フィールドが壊れず往復できる

目標は XML テキストの完全一致ではなく、意味的に往復できることである。

## 基本方針

Java 版でも、Node.js 版 upstream の思想をそのまま引き継ぐ。

- `MS Project XML` を意味の基軸として扱う
- `ProjectModel` を内部中立表現として扱う
- Java 版独自の思想へ作り替えない
- upstream と思想がずれる設計は、移植性と追随性を損なうため避ける

## STEP1 のスコープ

STEP1 で扱う最小スコープは次のとおり。

- `MS Project XML -> ProjectModel`
- `ProjectModel -> MS Project XML`

この段階では、次は後段とする。

- `XLSX`
- workbook JSON
- AI JSON
- patch JSON
- CLI
- SVG / Markdown / Mermaid などの派生出力

## `ProjectModel` の位置づけ

`ProjectModel` は、Java 版でも内部中立表現とする。

- 正本は `MS Project XML`
- `ProjectModel` は import / export のあいだをつなぐ内部表現
- `ProjectModel` 自体を外部交換形式の中心にしない

この考え方は Node.js 版 upstream と揃える。

## データ構造方針

STEP1 では、コア部分から Java の POJO で表現する。

- まずはコア部分を POJO で定義する
- 未確定な領域を無理に広げない
- 初期段階では、広い汎用構造へ逃がすことより、Node.js upstream の責務に追随しやすい形を優先する

詳細な型設計は今後の検討事項だが、少なくとも STEP1 では「まず XML 往復に必要な範囲を POJO として切る」方針を採る。

## STEP1 で優先して扱う対象

STEP1 の `ProjectModel` では、少なくとも次の要素群を優先対象とする。

- `Project`
- `Tasks`
- `Resources`
- `Assignments`
- `Calendars`

ただし、各要素のどのフィールドまで first cut で扱うかは、別途詳細化する。

## 非目標

STEP1 の時点では、次を目標にしない。

- XML の完全一致再現
- Java 側独自アーキテクチャへの作り替え
- Java 版だけの高度な抽象化
- Node.js 版 upstream から独立した仕様再設計

## 今後の詳細化項目

- `ProjectModel` の first cut フィールド範囲
- XML import の責務分割
- XML export の責務分割
- 日付時刻の扱い
- 数値型の扱い
- null / 未設定値の扱い
- エラー返却方針
