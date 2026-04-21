# Runtime Java CLI

## 目的

この文書は、`mikuproject-java` の Java CLI 実行物を downstream や利用側へ受け渡す際の前提と、CLI の実行方法を整理するための文書である。

## 配布成果物

`mikuproject-java` は、`mvn package` により次の配布成果物を生成する。

- `target/mikuproject.jar`
- `target/mikuproject-dist.zip`

このうち runtime の正式成果物は次である。

- `target/mikuproject.jar`

`target/mikuproject-dist.zip` は、利用側へ受け渡しやすい配布パッケージとして扱う。

## `mikuproject-dist.zip` の内容

現在の `dist.zip` には次を含める。

- `mikuproject.jar`
- `README.md`
- `LICENSE`
- `docs/runtime-java-cli.md`

## 成果物の性質

正式成果物は、`java -jar` で直接起動できる単一 jar を前提とする。

将来 runtime dependency が増えた場合でも、downstream 側の受け取り物がぶれないよう、この成果物は fat jar として維持する。

## downstream 連携

downstream 側は、`mikuproject-java` から次のものを受け取る前提とする。

- `target/mikuproject.jar`
  または
- `target/mikuproject-dist.zip`

downstream 側では、`dist.zip` から `mikuproject.jar` を bundle へコピーするか、直接 `target/mikuproject.jar` を受け取る。

正規実行経路は次のとおり。

- `java -jar <bundled-jar>`

## CLI 利用の前提

CLI は `java -jar mikuproject.jar <command> ...` の形で実行する。

代表的な command は次である。

- `validate-xml <input.xml>`
- `export-mermaid <input.xml>`
- `export-wbs-markdown <input.xml> ...`
- `export-daily-svg <input.xml> ...`
- `export-weekly-svg <input.xml> ...`
- `export-monthly-svg-zip <input.xml> <output.zip> ...`
- `export-report-bundle <input.xml> <output.zip> ...`
- `export-report-dir <input.xml> <output.dir> ...`
- `export-workbook-json <input.xml>`
- `export-xlsx <input.xml> <output.xlsxbin>`
- `import-external <format> <mode> <input> <output.xml> [<base.xml>]`

詳細な command 一覧は `README.md` を参照する。

## 実行時リソース

`export-ai-json-spec` は、ビルド済み classpath / JAR 内に含まれる `mikuproject-ai-json-spec.md` を出力する。
実行時にカレントディレクトリ上の `vendor/mikuproject/docs/mikuproject-ai-json-spec.md` は参照しない。

この Markdown は Maven の `process-resources` で `vendor/mikuproject/docs/mikuproject-ai-json-spec.md` から `target/classes/jp/igapyon/mikuproject/coreapi/mikuproject-ai-json-spec.md` へコピーされる。
そのため、配布済み `mikuproject.jar` は任意のカレントディレクトリから `java -jar mikuproject.jar export-ai-json-spec` を実行できる。

## report SVG の注意

daily / weekly SVG は、モデル内の最も早い task 開始日を timeline の基準日として描画する。
これにより、サンプル月以外の日付を持つ project でも、task bar が SVG viewBox 外へ飛びにくい。

ただし、入力 model の全 task が同一日時かつ zero duration の場合、出力 SVG は有効でも全 task bar が同じ日付位置へ縦に並ぶ。
2泊3日など期間を持つ計画として見せたい場合は、AI JSON / workbook JSON / XML の段階で task の `Start` / `Finish` または `planned_start` / `planned_finish` に実日程を入れる。

`validate-xml` では、placeholder / summary / milestone を除く複数 task が同一 `start` / `finish` かつ zero duration に潰れている場合、この入力品質リスクを warning として報告する。

## 最小 entrypoint から全機能 entrypoint への段取り

Java CLI は、`validate-xml` と主要 export だけを持つ最小 entrypoint から開始したが、現在は workbook / patch / AI JSON / xlsx / batch command まで含む実用的な entrypoint へ広がっている。

今後の整理は、次の 4 段階で進める。

### 1. core 公開面の薄い公開入口として揃える

- CLI は `coreapi` の薄い入口に徹する
- format 判定、option 解釈、入出力 path 処理以外の意味解釈は core へ残す
- command の追加より先に、既存 command がどの core API へ対応するかを保守できる状態に保つ

### 2. 単発 command を全主要入出力へ揃える

- validate / export / import / merge / apply の主要導線を、Java 版対象範囲で一通り揃える
- 現在の CLI はこの段階を概ね満たしている
- 対象は `MS Project XML`, workbook JSON, workbook xlsx, patch JSON, AI JSON, external import, report 出力群とする

### 3. option と batch command を整理する

- 複数 command で重複する option 群は、`WbsMarkdownOptions`, `WbsExportOptions`, `NativeSvgOptions` の単位で揃える
- `*-batch` command は upstream straight conversion そのものではなく、Java 側運用拡張として分けて扱う
- help 表示、`README.md`、test を同じ単位で更新し、entrypoint の見え方を崩さない

### 4. full entrypoint として保守可能に固定する

- 追加済み command 群を `README.md`, `docs/remaining-migration-items.md`, `docs/upstream-test-mapping.md`, CLI test で追跡可能にする
- upstream 追随時は `core API の差分` と `Java CLI 独自拡張` を分けて確認する
- 新しい command を足すより、既存 command の option / diagnostics / test を揃えることを優先する

## 現在位置

Java CLI は、上の 4 段階のうち次の位置にある。

- `1. core 公開面の薄い公開入口として揃える`: 対応済み
- `2. 単発 command を全主要入出力へ揃える`: 対応済み
- `3. option と batch command を整理する`: 主要導線は対応済み、既存 command の表現と追跡性を保守中
- `4. full entrypoint として保守可能に固定する`: 主要導線は対応済み、README / test / 追随文書との同期を保守中

したがって、次の自然な作業は `新 command の追加` より `既存 command 群の整理と保守可能性の固定` である。

## 補足

- この文書でいう正式成果物は、Java CLI の runtime 配布物を指す
- Web UI / browser main 系は、この文書で扱う Java CLI runtime 配布物には含めない
- Java 版の対象は CLI runtime であり、Web UI / browser main 系は移植対象外とする
