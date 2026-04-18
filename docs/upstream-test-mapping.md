# Upstream Test Mapping

## 方針

Java 版のテストは、Node.js upstream のテスト意図と fixture を追えることを重視する。
この文書は、upstream 側テストと Java 側テストの対応関係を固定するためのメモである。

## 対応表

### `vendor/mikuproject/tests/mikuproject-msproject-xml-roundtrip.test.js`

- `limits default calendar holiday exceptions to the project date range`
  - Java 側対応:
    - `MsProjectXmlTest.ensureDefaultProjectCalendarBuildsJapaneseHolidayExceptions`
  - 補足:
    - Java 側では `MsProjectCalendar.ensureDefaultProjectCalendar` を直接確認する
    - 祝日 exception が project date range 内に限定されることを見る

- `round-trips the minimal xml sample`
  - Java 側対応:
    - `MsProjectXmlTest.roundTripsUpstreamMinimalXmlFixture`
  - fixture:
    - `vendor/mikuproject/testdata/minimal.xml`

- `round-trips hierarchy through CSV + ParentID export and import`
  - Java 側対応:
    - 未対応
  - 補足:
    - これは CSV import/export 系であり、現在の Java STEP1 対象外

- `exports project overview and default phase detail views from hierarchy`
  - Java 側対応:
    - 未対応
  - 補足:
    - これは AI view / projection 系であり、現在の Java STEP1 対象外

- `exports task_edit_view with predecessors, successors, and assignments`
  - Java 側対応:
    - 未対応
  - 補足:
    - これは AI view / projection 系であり、現在の Java STEP1 対象外

- `exports scoped phase detail with root_uid and max_depth`
  - Java 側対応:
    - 未対応
  - 補足:
    - これは AI view / projection 系であり、現在の Java STEP1 対象外

- `rejects scoped phase detail when root_uid is outside the phase`
  - Java 側対応:
    - 未対応
  - 補足:
    - これは AI view / projection 系であり、現在の Java STEP1 対象外

- `builds project draft request and imports predecessor mapping from project_draft_view`
  - Java 側対応:
    - 未対応
  - 補足:
    - これは AI draft / import 系であり、現在の Java STEP1 対象外

## fixture 対応

- `vendor/mikuproject/testdata/minimal.xml`
  - Java 側:
    - `MsProjectXmlTest.roundTripsUpstreamMinimalXmlFixture`

- `vendor/mikuproject/testdata/hierarchy.xml`
  - Java 側:
    - `MsProjectXmlTest.importsUpstreamHierarchyXmlFixture`

- `vendor/mikuproject/testdata/dependency.xml`
  - Java 側:
    - `MsProjectXmlTest.importsUpstreamDependencyXmlFixture`

## Java 側テスト命名

Java 側テスト名は、次の基準で付ける。

- upstream に既存テスト名がある場合は、その意図を読める英語名を優先する
- fixture 名がある場合は、`Upstream` と fixture 名を含めて対応元を推測しやすくする
- Java 側固有の補助検証は、責務名をそのままメソッド名に出す

例:

- `roundTripsUpstreamMinimalXmlFixture`
- `importsUpstreamHierarchyXmlFixture`
- `ensureDefaultProjectCalendarBuildsJapaneseHolidayExceptions`
