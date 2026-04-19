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

first cut の `dist.zip` には次を含める。

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

## 補足

- この文書でいう正式成果物は、Java CLI の runtime 配布物を指す
- Web UI / browser main 系は Java 版の移植対象外であり、この成果物定義にも含めない
