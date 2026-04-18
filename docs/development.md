# Development Notes

## 基本方針

`mikuproject-java` は、`vendor/mikuproject` に保持した Node.js 版 upstream を参照しながら、Java 版へ移植していく。

このリポジトリでは、通常の新規 Java プロジェクトのように Java 側の都合だけで構成を最適化することを第一目的にしない。
重要なのは、upstream との対応関係が見え、あとから upstream 更新へ追従しやすいことである。

今回の移植範囲には、Web UI は含めない。
したがって、Java 版では Node.js 版 upstream のうち UI 層そのものを直接移植対象とせず、まずは内部モデル、MS Project XML、関連ロジック、テストを中心に進める。

現時点では、ビルド基盤は Maven を使う。
ただし、Maven 採用も Java 側独自の構造最適化を進めるためではなく、Java 1.8 前提の最小限の開発土台として扱う。

自動テストは JUnit を使う。
Java 1.8 前提でも利用可能な JUnit 系の現行版を第一候補とし、原則として JUnit 5 Jupiter を優先する。

## Java 1.8 前提

ターゲット Java は `1.8` とする。

これは現代 Java としては古い前提であり、言語機能や標準 API の面で制約がある。
したがって、本プロジェクトでは「最新 Java らしい最善構成」を目標にしない。

設計上の優先順位は次のとおり。

- Node.js upstream との対応関係が追いやすいこと
- upstream 更新時に差分比較しやすいこと
- 移植漏れや仕様ずれを見つけやすいこと
- Java 1.8 で無理なく保守できること

## upstream 構造尊重

Java 側の package / class 分割は、Node.js 版 upstream のファイル構造と責務境界を尊重する。

特に次を重視する。

- 主要な責務分割は、可能な限り upstream のファイル境界に寄せる
- Java 側で過度な再編成を行わない
- upstream 1ファイルに対して Java 側で複数クラスへ分ける場合でも、対応元が推測しやすい命名にする
- upstream 更新時に「どの Java クラスを見ればよいか」を追いやすい状態を保つ

## 命名方針

クラス名や補助型名も、Node.js upstream との対応関係を見つけやすいように付ける。

前提として、Java の命名規約は尊重する。
したがって、クラス名は `UpperCamelCase` を使う。

ただし、単に Java らしい一般名へ置き換えるのではなく、upstream のファイル名や責務名から自然に読み替えた名前を優先する。

基本の読み替え規則は、`kebab-case` を Java の `UpperCamelCase` へ変換する形とする。

例:

- upstream の `project-model` 系は、Java 側では `ProjectModel` 系へ寄せる
- upstream の `msproject-xml` 系は、Java 側では `MsProjectXml` 系へ寄せる
- upstream の `excel-io` 系は、Java 側では `ExcelIo` 系へ寄せる
- upstream の `patch-json` 系は、Java 側では `PatchJson` 系へ寄せる
- upstream の `ai-views` 系は、Java 側では `AiViews` 系へ寄せる

重視する点は次のとおり。

- upstream のどのファイルに対応する Java クラスか想像しやすいこと
- 名前を見たときに責務の対応が追えること
- Java の規約を守りながら、upstream との距離を保つこと

避けたいこと:

- upstream 由来が見えなくなるほど一般化した名前
- Java 側独自の抽象名へ早い段階で置き換えること
- 同じ責務なのに upstream と大きく異なる語彙を使うこと

## メソッド名方針

メソッド名は、Node.js upstream の関数名が `camelCase` である場合、原則として無変換でそのまま Java 側へ持ち込む。

これは、クラス名のように Java 規約に合わせた大きな読み替えを必要としないためである。
したがって、Node.js 側の関数名と Java 側メソッド名の対応は、できるだけ 1 対 1 に保つ。

例:

- `importFromXml` は、Java 側でも `importFromXml` を優先する
- `exportToXml` は、Java 側でも `exportToXml` を優先する
- `parseAiJsonText` は、Java 側でも `parseAiJsonText` を優先する
- `importIntoProjectModel` は、Java 側でも `importIntoProjectModel` を優先する

重視する点は次のとおり。

- upstream のどの関数に対応する Java メソッドか想像しやすいこと
- 差分比較のときに語彙対応を追いやすいこと
- Java 側で補助型を導入しても、中心の動詞や責務名は upstream から大きくずらさないこと

Java 側で変わりやすいのは、名前そのものよりも次の点である。

- static method にするか instance method にするか
- 引数を個別指定にするか request class にまとめるか
- 例外や戻り値を Java 向けにどう型付けするか

ただし、これらの差があっても、主メソッド名はできるだけ upstream の語彙を維持する。

避けたいこと:

- `importFromXml` を `loadProject` のような別語彙へ置き換えること
- `exportProjectOverviewView` を `createOverview` のように短縮・一般化すること
- `applyToProjectModel` を `updateState` のような抽象名へ変えること

## ライブラリ方針

Java には多くの有用なライブラリがあるが、移植初期段階では「Java で便利だから置き換える」という判断を優先しない。

基本方針は次のとおり。

- Node.js upstream でスクラッチ実装している箇所は、まず Java 側でもその構造やロジックをできるだけ踏襲する
- Node.js upstream で外部ライブラリを利用している箇所は、対応する Java ライブラリの利用を検討してよい
- ただし、Node.js 側で利用しているライブラリに相当する Java ライブラリが見当たらない、または適合度が低い場合は、Java 側でスクラッチ実装してよい
- Java 側だけで独自に重い外部ライブラリへ置き換える判断は、upstream 対応が見えにくくなるため慎重に行う
- first cut では、実装の洗練より upstream との対応可能性を優先する

理由は次のとおり。

- upstream の仕様意図を読み解きやすい
- 差分比較で挙動のズレを見つけやすい
- upstream 更新追随のときに影響範囲を判断しやすい
- Java 側だけ別実装に寄りすぎると、移植というより再実装になりやすい
- 一方で、適切な Java ライブラリがないのに無理に合わせると、かえって実装と保守が不自然になる

したがって、ライブラリ導入の判断基準は次の順序とする。

1. upstream が外部ライブラリを使っているか
2. upstream がスクラッチ実装しているか
3. Java 側に十分適合するライブラリがあるか
4. Java 1.8 前提で保守可能か
5. upstream 追随性を壊さないか

ライブラリ採用可否は一律に決めず、適合度、実装コスト、保守の面倒さを見ながらケースバイケースで判断する。

## 避けたいこと

次のような変更は、初期段階では避ける。

- Java 流の都合だけで package を大きく組み替えること
- upstream では近接している責務を、Java 側で広く分散させること
- 対応元が分からなくなる命名や抽象化を先行させること
- 早い段階で独自アーキテクチャへ作り替えること

## package 方針

Java package の基底は `jp.igapyon.mikuproject` とする。

`mikuproject-java` のような別名は package へ持ち込まない。
Java 側の package は製品名ベースで統一する。

下位 package も、可能な限り upstream の責務名から自然に読み替えた構造を優先する。
たとえば `jp.igapyon.mikuproject.model` や `jp.igapyon.mikuproject.msprojectxml` のような形を基本候補とする。

ここでも、Java 側だけの都合で大きく再編した package 構成は初期段階では避ける。

## upstream 追随の考え方

このリポジトリでは、実装のしやすさだけでなく upstream 追随時の見通しを重視する。

そのため、開発時は次を意識する。

- Node.js upstream のどのファイルを参照して移植しているかを把握する
- Java 側の class / package が、どの upstream ファイルに対応するか説明できるようにする
- upstream 更新時に、差分の確認単位を対応ファイル単位で辿れるようにする
- 移植時の判断で upstream と意図的にずらした点は、必要に応じて `docs/` へ残す

upstream の bug を移植中に見つけた場合でも、原則として Java 側の移植作業自体は止めない。

その場合は次のように扱う。

- まず `TODO.md` に upstream への連絡事項として記録する
- 必要なら、どの upstream ファイルや責務に関係する bug かが分かるように短く残す
- Java 側では、その bug の存在を意識しつつ移植作業を継続する
- Java 側で暫定回避を入れる場合も、upstream bug に起因することが分かるようにする

## テスト方針

自動テストは、Java 側で単に妥当なテストを書くのではなく、Node.js upstream のテストとの対応関係が追えることを重視する。

特に次を重視する。

- 可能な限り、upstream のテスト対象ファイルに対応する Java 側テスト class を用意する
- テスト名や責務の切り方も、upstream のテスト意図を追いやすい形を優先する
- 同じ入力と同種の期待値で比較できる箇所は、Java 側でもできるだけ対応づける
- upstream に既存テストがある箇所は、Java 側でも「何に対応するテストか」を説明できるようにする

例:

- upstream の `msproject-codec` 系テストに対応する Java 側テストを用意する
- upstream の round-trip テストがある場合は、Java 側でも round-trip テストを優先する
- upstream の fixture や sample XML に対応づけられるなら、それを活用する

避けたいこと:

- Java 側だけ独自の観点へ寄りすぎて、upstream との比較が難しくなること
- upstream に明確な対応元があるのに、テスト構成や命名が大きく乖離すること
- 移植確認より Java 側独自都合の網羅性だけを優先すること

## workspace 方針

`workplace/` は Git 管理外のローカル作業スペースとして扱う。

一時メモ、試験コード、調査用ファイルは、必要に応じてここで扱う。
リポジトリへ残すべき内容は、整理したうえで `docs/` や `src/` へ反映する。
