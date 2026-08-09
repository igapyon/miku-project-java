/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuproject.wbsmarkdown;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

import jp.igapyon.mikuproject.model.TaskModel;
import jp.igapyon.mikuproject.msprojectxml.MsProjectXml;
import jp.igapyon.mikuproject.wbsmarkdown.WbsMarkdown.WbsMarkdownOptions;

public class WbsMarkdownTest {
    @Test
    public void exportsOneMarkdownDocumentWithTreeFirstAndTableAfterIt() {
        MsProjectXml xml = new MsProjectXml();
        String markdown = xml.exportWbsMarkdown(xml.importFromXml(new jp.igapyon.mikuproject.msprojectxml.MsProjectSamples().buildSampleXml()));

        assertTrue(markdown.contains("# プロジェクト情報"));
        assertTrue(markdown.contains("# WBS ツリー"));
        assertTrue(markdown.contains("# WBS テーブル"));
        assertTrue(markdown.contains("# サマリ"));
        assertTrue(markdown.indexOf("# WBS ツリー") < markdown.indexOf("# WBS テーブル"));
        assertTrue(markdown.indexOf("# WBS テーブル") < markdown.indexOf("# サマリ"));
        assertTrue(markdown.contains("| プロジェクト名 | miku-project開発 |"));
        assertTrue(markdown.contains("```text"));
        assertTrue(markdown.contains("1 基盤整備 (3/16 - 3/17): 100%"));
        assertTrue(markdown.contains("┗　1.1 着手 (3/16): 100%"));
        assertTrue(markdown.contains("| 1 | フェーズ | 1 | 基盤整備 | 2026-03-16 | 2026-03-17 |"));
    }

    @Test
    public void showsNotesInTheTreeSectionAndSummaryAfterTheTable() {
        MsProjectXml xml = new MsProjectXml();
        jp.igapyon.mikuproject.model.ProjectModel model = xml.importFromXml(new jp.igapyon.mikuproject.msprojectxml.MsProjectSamples().buildSampleXml());
        model.tasks.get(1).notes = "補足A\n補足B";

        WbsMarkdownOptions options = new WbsMarkdownOptions();
        options.displayDaysBeforeBaseDate = Integer.valueOf(1);
        options.displayDaysAfterBaseDate = Integer.valueOf(2);
        options.useBusinessDaysForDisplayRange = Boolean.TRUE;
        options.useBusinessDaysForProgressBand = Boolean.TRUE;
        String markdown = xml.exportWbsMarkdown(model, options);

        assertTrue(markdown.contains("詳細: 補足A"));
        assertTrue(markdown.contains("      補足B"));
        assertTrue(markdown.contains("| 前日数 | 1 |"));
        assertTrue(markdown.contains("| 後日数 | 2 |"));
        assertTrue(markdown.contains("| 表示 | 営業日 |"));
        assertTrue(markdown.contains("| 進捗 | 営業日 |"));
    }

    @Test
    public void escapesMarkdownSensitiveTextInTableCells() {
        MsProjectXml xml = new MsProjectXml();
        jp.igapyon.mikuproject.model.ProjectModel model = xml.importFromXml(new jp.igapyon.mikuproject.msprojectxml.MsProjectSamples().buildSampleXml());
        model.tasks.get(1).notes = "# 見出し風\n- 箇条書き風\n1. 番号付き風\nA | B <tag> & text";

        String markdown = xml.exportWbsMarkdown(model);

        assertTrue(markdown.contains("詳細: # 見出し風"));
        assertTrue(markdown.contains("\\# 見出し風<br>\\- 箇条書き風<br>1\\. 番号付き風<br>A \\\\| B &lt;tag&gt; &amp; text"));
    }

    @Test
    public void usesFenceThatDoesNotBreakWhenTreeTextIncludesBackticks() {
        MsProjectXml xml = new MsProjectXml();
        jp.igapyon.mikuproject.model.ProjectModel model = xml.importFromXml(new jp.igapyon.mikuproject.msprojectxml.MsProjectSamples().buildSampleXml());
        model.tasks.get(1).name = "``` fenced name";
        model.tasks.get(1).notes = "line 1\n``` fenced note";

        String markdown = xml.exportWbsMarkdown(model);

        assertTrue(markdown.contains("````text"));
        assertTrue(markdown.contains("``` fenced name"));
        assertTrue(markdown.contains("``` fenced note"));
    }

    @Test
    public void keepsDeepHierarchyReadable() {
        MsProjectXml xml = new MsProjectXml();
        jp.igapyon.mikuproject.model.ProjectModel model = xml.importFromXml(new jp.igapyon.mikuproject.msprojectxml.MsProjectSamples().buildSampleXml());
        TaskModel baseTask = model.tasks.get(2);
        model.tasks.add(createDerivedTask(baseTask, "901", "901", "内部 JSON 形式への写像方針を確認する", 3, "1.2.1", "1.2.1",
                "2026-03-17T09:00:00", "2026-03-18T18:00:00", 40, "説明責務と round-trip 観点を切り分ける"));
        model.tasks.add(createDerivedTask(baseTask, "902", "902", "フィールド差分の洗い出し結果を整理する", 4, "1.2.1.1", "1.2.1.1",
                "2026-03-18T09:00:00", "2026-03-18T18:00:00", 10, "XML と内部モデルの差分を箇条書きで残す"));

        String markdown = xml.exportWbsMarkdown(model);

        assertTrue(markdown.contains("　┗　1.2.1"));
        assertTrue(markdown.contains("　　┗　1.2.1.1"));
        assertTrue(markdown.contains("詳細: 説明責務と round-trip 観点を切り分ける"));
    }

    @Test
    public void exportsHierarchyFixtureIntoReadableMarkdown() throws IOException {
        MsProjectXml xml = new MsProjectXml();
        String markdown = xml.exportWbsMarkdown(xml.importFromXml(readVendorTestdata("hierarchy.xml")));

        assertTrue(markdown.contains("| プロジェクト名 | Hierarchy Project |"));
        assertTrue(markdown.contains("Summary"));
        assertTrue(markdown.contains("Child A"));
        assertTrue(markdown.contains("Child B"));
        assertTrue(markdown.contains("Second child task"));
    }

    @Test
    public void exportsDependencyFixtureIntoReadableMarkdown() throws IOException {
        MsProjectXml xml = new MsProjectXml();
        String markdown = xml.exportWbsMarkdown(xml.importFromXml(readVendorTestdata("dependency.xml")));

        assertTrue(markdown.contains("| プロジェクト名 | Dependency Project |"));
        assertTrue(markdown.contains("Prepare"));
        assertTrue(markdown.contains("Execute"));
        assertTrue(markdown.contains("2026-03-16"));
        assertTrue(markdown.contains("2026-03-19"));
    }

    @Test
    public void appliesDisplayAndHolidayOptionsToMarkdown() {
        MsProjectXml xml = new MsProjectXml();
        jp.igapyon.mikuproject.model.ProjectModel model = xml.importFromXml(new jp.igapyon.mikuproject.msprojectxml.MsProjectSamples().buildSampleXml());
        WbsMarkdownOptions options = new WbsMarkdownOptions();
        options.displayDaysBeforeBaseDate = Integer.valueOf(1);
        options.displayDaysAfterBaseDate = Integer.valueOf(2);
        options.useBusinessDaysForDisplayRange = Boolean.TRUE;
        options.useBusinessDaysForProgressBand = Boolean.TRUE;
        options.holidayDates.add("2026-04-29");
        options.holidayDates.add("2026-04-30");

        String markdown = xml.exportWbsMarkdown(model, options);

        assertTrue(markdown.contains("| 前日数 | 1 |"));
        assertTrue(markdown.contains("| 後日数 | 2 |"));
        assertTrue(markdown.contains("| 表示 | 営業日 |"));
        assertTrue(markdown.contains("| 進捗 | 営業日 |"));
    }

    private String readVendorTestdata(String fileName) throws IOException {
        byte[] bytes = Files.readAllBytes(Paths.get("vendor", "miku-project", "testdata", fileName));
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private TaskModel createDerivedTask(TaskModel baseTask, String uid, String id, String name, int outlineLevel,
            String outlineNumber, String wbs, String start, String finish, int percentComplete, String notes) {
        TaskModel task = new TaskModel();
        task.uid = uid;
        task.id = id;
        task.name = name;
        task.outlineLevel = Integer.valueOf(outlineLevel);
        task.outlineNumber = outlineNumber;
        task.wbs = wbs;
        task.start = start;
        task.finish = finish;
        task.duration = "PT8H0M0S";
        task.percentComplete = Integer.valueOf(percentComplete);
        task.percentWorkComplete = Integer.valueOf(percentComplete);
        task.summary = false;
        task.milestone = false;
        task.critical = Boolean.FALSE;
        task.notes = notes;
        task.calendarUID = baseTask.calendarUID;
        return task;
    }
}
