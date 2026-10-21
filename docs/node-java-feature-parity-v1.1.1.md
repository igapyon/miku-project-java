# Node / Java 機能差分調査 v1.1.1

調査日: 2026-10-21

## 比較基準

- Node版: miku-project v1.1.1 release tag、commit `9a9f562c03d28f1b0f3c809226dc61d0cd9d5f7e`
- Java版: pom.xml version 1.1.1、local branch `devel-tiga1021xdc` のcommit `56384e85cba1be93718a9b7b782f11c9a36a77e7`
- 同じ製品versionであることは、両runtimeの機能同等性を意味しない。

参照先:

- [Node v1.1.1 release](https://github.com/igapyon/miku-project/releases/tag/v1.1.1)
- [Node v1 capability matrix](https://github.com/igapyon/miku-project/blob/v1.1.1/docs/miku-project-v1-capability-matrix-v20260813.md)
- [Java v1 contract snapshot](v1-contract-snapshot.md)
- [Java migration status](remaining-migration-items.md)

## 差分の分類

| 領域 | Node版 v1.1.1 | Java版 1.1.1 checkout | 判断 |
| --- | --- | --- | --- |
| 既存CLI | ai / state / import / export / report のcommand群 | 主なshared command群を提供 | この範囲に、直ちに移植が必要なcommand欠落は見つからなかった |
| Java CLI extension | Node CLIにない一部のstate / validate / merge / report導線 | state validate / import / merge、validate xml / xlsx、merge xlsx、report dirをJava extensionsとして公開 | Node側へ一律追加せず、Java側からも直ちに削除しない。共通契約外のextensionとして扱う |
| 既存CLI help | command parserはai spec / ai detect-kind / ai validate-patch / state diffの`--out`を受け付けるが、root helpのusageには表示されない | Java helpはこれらの`--out`を表示 | Node側のhelp表示がparserとずれている。Node側でhelpを同期する |
| 新v1 runtime | 九つの共通capabilityをNode referenceとして実装。Gate G4のcandidateは内部reference用で、公開runtime Releaseとは区別 | `MikuProjectV1Validate`によるvalidate serviceまで実装。P5-C1はreviewと人の承認待ちで、Gate G5は未完 | v1は共通契約の予定範囲。Java側を続行するが、既存Gateと承認順を守る |
| v1 contract snapshot | v1.1.1でCLI contractに、help / versionはruntime asset検証を要しない旨が追加された | Javaはimmutableなv1.0.3 snapshotを保持 | v1.0.3を変更せず、次のv1実装前に新しいimmutable snapshotを追加する |
| 配布・Web | Node runtime bundleとbrowser runtimeを提供。Web UIは別repository | fat JAR / sources JAR / distribution ZIPを提供。Web UIは対象外 | runtimeと配布形式の違いであり、機能不足として同期しない |

## 確認した根拠

- Nodeのlegacy CLI root helpとoption registryを比較した。registryは上記四commandで`--out`を許可するが、help文には表示されない。
- Java READMEはJava固有CLI commandを明示的にJava extensionsとして列挙する。
- Node v1 capability matrixは九つの共有capabilityを定義し、2026-08-14時点ではJava runtimeをnot startedとして記録する。
- Java TODOではP5-A / P5-B完了、P5-C1 validate実装済み・reviewと人の承認待ちと記録する。v1 runtime launcherと残りのv1 commandは未完である。
- Node tag v1.0.3とv1.1.1のv1 contract / corpus / schema差分では、CLI contractだけにcontrol operationのasset verificationに関する追加がある。Java snapshotの更新規則はcontract変更時に別snapshotを追加し、既存snapshotを上書きしないとしている。

## 結論と次の作業

「Node版の方が機能が少ない」と一方向には言えない。従来CLIでは主なcommand群は共有され、Javaには便利なextensionがある。一方、新v1 workflowはNodeがreference実装を進め、JavaはGate G5の途中である。ただしNodeのv1 candidateは公開Release runtimeと同一ではない。

現在、Javaの既存機能を追加・削除する根拠は得られていない。Java extensionsは残し、Node側へ機械的に移植しない。v1の差は意図的に捨てられた機能ではなく、既存計画上の未完了作業として扱う。次に進めるには、P5-C1のreview / 承認を完了し、Node v1.1.1の変更を含む新snapshotを追加してからGate G5の残りを実装する。

Node側には、現行Javaの進捗をcapability matrixへ反映し、legacy CLI helpのoption表示をregistryと揃えるIssueを提案する。
