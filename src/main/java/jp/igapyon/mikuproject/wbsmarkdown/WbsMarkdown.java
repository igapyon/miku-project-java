/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuproject.wbsmarkdown;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jp.igapyon.mikuproject.markdownescape.MarkdownEscape;
import jp.igapyon.mikuproject.model.AssignmentModel;
import jp.igapyon.mikuproject.model.CalendarModel;
import jp.igapyon.mikuproject.model.PredecessorModel;
import jp.igapyon.mikuproject.model.ProjectModel;
import jp.igapyon.mikuproject.model.ResourceModel;
import jp.igapyon.mikuproject.model.TaskModel;
import jp.igapyon.mikuproject.wbsdateband.WbsDateband;

public class WbsMarkdown {
    private final MarkdownEscape markdownEscape = new MarkdownEscape();
    private final WbsDateband dateband = new WbsDateband();

    public String exportWbsMarkdown(ProjectModel model) {
        return exportWbsMarkdown(model, null);
    }

    public String exportWbsMarkdown(ProjectModel model, WbsMarkdownOptions options) {
        WbsMarkdownOptions actualOptions = options == null ? new WbsMarkdownOptions() : options;
        Set<Integer> nonWorkingDayTypes = dateband.collectProjectNonWorkingDayTypes(model);
        Set<String> holidaySet = new LinkedHashSet<String>(dateband.collectWbsHolidayDates(model));
        for (String holiday : actualOptions.holidayDates) {
            if (holiday != null && holiday.length() >= 10) {
                holidaySet.add(holiday.substring(0, 10));
            }
        }
        List<String> displayDateBand = dateband.buildDisplayDateBand(model.project.startDate, model.project.finishDate,
                model.project.currentDate, actualOptions.displayDaysBeforeBaseDate, actualOptions.displayDaysAfterBaseDate,
                holidaySet, nonWorkingDayTypes, actualOptions.useBusinessDaysForDisplayRange);
        Map<String, String> calendarNameByUid = new LinkedHashMap<String, String>();
        for (CalendarModel calendar : model.calendars) {
            calendarNameByUid.put(calendar.uid, calendar.name);
        }
        Map<String, String> resourceNameByUid = new LinkedHashMap<String, String>();
        for (ResourceModel resource : model.resources) {
            resourceNameByUid.put(resource.uid, resource.name);
        }
        Map<String, String> predecessorNameByUid = new LinkedHashMap<String, String>();
        for (TaskModel task : model.tasks) {
            predecessorNameByUid.put(task.uid, task.name);
        }
        Map<String, List<String>> resourceNamesByTaskUid = new LinkedHashMap<String, List<String>>();
        for (AssignmentModel assignment : model.assignments) {
            String resourceName = resourceNameByUid.get(assignment.resourceUid);
            if (resourceName == null) {
                continue;
            }
            List<String> resourceNames = resourceNamesByTaskUid.get(assignment.taskUid);
            if (resourceNames == null) {
                resourceNames = new ArrayList<String>();
                resourceNamesByTaskUid.put(assignment.taskUid, resourceNames);
            }
            if (!resourceNames.contains(resourceName)) {
                resourceNames.add(resourceName);
            }
        }

        List<String> sections = new ArrayList<String>();
        sections.add("# プロジェクト情報");
        sections.add("");
        sections.addAll(buildKeyValueTable(new String[][] {
                { "プロジェクト名", safe(model.project.name, "-") },
                { "カレンダ", formatCalendarLabel(model.project.calendarUID, calendarNameByUid) },
                { "開始日", formatWbsDate(model.project.startDate) },
                { "終了日", formatWbsDate(model.project.finishDate) },
                { "現在日", formatWbsDate(model.project.currentDate) },
                { "祝日", String.valueOf(holidaySet.size()) }
        }));
        sections.add("");
        sections.add("# WBS ツリー");
        sections.add("");
        sections.addAll(wrapFenceBlock(buildTreeLines(model.tasks), "text"));
        sections.add("");
        sections.add("---");
        sections.add("");
        sections.add("# WBS テーブル");
        sections.add("");
        sections.addAll(buildWbsTable(model.tasks, holidaySet, nonWorkingDayTypes, resourceNamesByTaskUid, predecessorNameByUid,
                actualOptions.useBusinessDaysForProgressBand));
        sections.add("");
        sections.add("---");
        sections.add("");
        sections.add("# サマリ");
        sections.add("");
        sections.addAll(buildKeyValueTable(new String[][] {
                { "表示日", String.valueOf(displayDateBand.size()) },
                { "表示週", String.valueOf(displayDateBand.isEmpty() ? 0 : (int) Math.ceil(displayDateBand.size() / 7.0d)) },
                { "営業日", String.valueOf(dateband.countBusinessDays(displayDateBand, holidaySet, nonWorkingDayTypes)) },
                { "前日数", formatOptionalCount(actualOptions.displayDaysBeforeBaseDate) },
                { "後日数", formatOptionalCount(actualOptions.displayDaysAfterBaseDate) },
                { "表示", Boolean.TRUE.equals(actualOptions.useBusinessDaysForDisplayRange) ? "営業日" : "暦日" },
                { "進捗", Boolean.TRUE.equals(actualOptions.useBusinessDaysForProgressBand) ? "営業日" : "暦日" },
                { "基準日", formatWbsDate(model.project.currentDate) },
                { "タスク", String.valueOf(model.tasks.size()) },
                { "リソース", String.valueOf(model.resources.size()) },
                { "割当", String.valueOf(model.assignments.size()) },
                { "カレンダ", String.valueOf(model.calendars.size()) }
        }));
        sections.add("");
        return joinLines(sections);
    }

    private List<String> buildKeyValueTable(String[][] rows) {
        List<String> lines = new ArrayList<String>();
        lines.add("| 項目 | 値 |");
        lines.add("| --- | --- |");
        for (String[] row : rows) {
            lines.add("| " + escapeMarkdownCell(row[0]) + " | " + escapeMarkdownCell(row[1]) + " |");
        }
        return lines;
    }

    private List<String> buildWbsTable(List<TaskModel> tasks, Set<String> holidaySet, Set<Integer> nonWorkingDayTypes,
            Map<String, List<String>> resourceNamesByTaskUid, Map<String, String> predecessorNameByUid,
            Boolean useBusinessDaysForProgressBand) {
        List<String> lines = new ArrayList<String>();
        lines.add("| WBS | 種別 | 階層 | 名称 | 開始 | 終了 | 期間 | タスク詳細 | 進捗 | 担当 | リソース | 先行 |");
        lines.add("| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |");
        for (TaskModel task : tasks) {
            List<String> resourceNames = resourceNamesByTaskUid.get(task.uid);
            List<String> predecessors = new ArrayList<String>();
            if (task.predecessors != null) {
                for (PredecessorModel predecessor : task.predecessors) {
                    String label = predecessorNameByUid.get(predecessor.predecessorUid);
                    predecessors.add(label == null ? predecessor.predecessorUid : label);
                }
            }
            String[] cells = new String[] {
                    safe(task.wbs != null ? task.wbs : task.outlineNumber, "-"),
                    classifyTaskKind(task),
                    task.outlineLevel == null ? "-" : String.valueOf(task.outlineLevel),
                    safe(task.name, "-"),
                    formatWbsDate(task.start),
                    formatWbsDate(task.finish),
                    formatDurationLabel(task, holidaySet, nonWorkingDayTypes, useBusinessDaysForProgressBand),
                    formatNoteCell(task.notes),
                    formatPercentCell(task.percentComplete),
                    firstResourceName(resourceNames),
                    join(resourceNames, ", ", "-"),
                    predecessors.isEmpty() ? "-" : join(predecessors, ", ", "-")
            };
            lines.add("| " + escapeMarkdownCell(cells[0]) + " | " + escapeMarkdownCell(cells[1]) + " | "
                    + escapeMarkdownCell(cells[2]) + " | " + escapeMarkdownCell(cells[3]) + " | "
                    + escapeMarkdownCell(cells[4]) + " | " + escapeMarkdownCell(cells[5]) + " | "
                    + escapeMarkdownCell(cells[6]) + " | " + escapeMarkdownCell(cells[7]) + " | "
                    + escapeMarkdownCell(cells[8]) + " | " + escapeMarkdownCell(cells[9]) + " | "
                    + escapeMarkdownCell(cells[10]) + " | " + escapeMarkdownCell(cells[11]) + " |");
        }
        return lines;
    }

    private List<String> buildTreeLines(List<TaskModel> tasks) {
        List<String> lines = new ArrayList<String>();
        for (TaskModel task : tasks) {
            int outlineLevel = task.outlineLevel == null ? 1 : task.outlineLevel.intValue();
            StringBuilder indent = new StringBuilder();
            for (int index = 0; index < Math.max(0, outlineLevel - 2); index++) {
                indent.append('　');
            }
            if (outlineLevel > 1) {
                indent.append("┗　");
            }
            lines.add(indent.toString() + formatTreeInlineText(safe(task.wbs != null ? task.wbs : task.outlineNumber, "-"))
                    + " " + formatTreeInlineText(safe(task.name, "-")) + " ("
                    + formatTreeInlineText(formatTreeDateRange(task.start, task.finish)) + "): "
                    + formatTreeInlineText(formatPercentCell(task.percentComplete)));
            String normalizedNotes = normalizeFenceText(task.notes);
            if (!normalizedNotes.isEmpty()) {
                StringBuilder noteIndent = new StringBuilder();
                for (int index = 0; index < Math.max(0, outlineLevel - 1); index++) {
                    noteIndent.append('　');
                }
                noteIndent.append('　');
                String[] noteLines = normalizedNotes.split("\n");
                lines.add(noteIndent.toString() + "詳細: " + noteLines[0]);
                for (int index = 1; index < noteLines.length; index++) {
                    if (!noteLines[index].isEmpty()) {
                        lines.add(noteIndent.toString() + "      " + noteLines[index]);
                    }
                }
            }
        }
        if (lines.isEmpty()) {
            lines.add("(task なし)");
        }
        return lines;
    }

    private List<String> wrapFenceBlock(List<String> lines, String infoString) {
        String content = joinLines(lines);
        int longestBacktickRun = longestFenceRun(content, '`');
        int fenceLength = Math.max(3, longestBacktickRun + 1);
        String fence = repeat('`', fenceLength);
        List<String> result = new ArrayList<String>();
        result.add(fence + infoString);
        result.addAll(lines);
        result.add(fence);
        return result;
    }

    private int longestFenceRun(String text, char ch) {
        int maxRun = 0;
        int currentRun = 0;
        for (int index = 0; index < text.length(); index++) {
            if (text.charAt(index) == ch) {
                currentRun++;
                maxRun = Math.max(maxRun, currentRun);
            } else {
                currentRun = 0;
            }
        }
        return maxRun;
    }

    private String classifyTaskKind(TaskModel task) {
        if (task.summary) {
            return "フェーズ";
        }
        if (task.milestone) {
            return "マイル";
        }
        return "タスク";
    }

    private String formatDurationLabel(TaskModel task, Set<String> holidaySet, Set<Integer> nonWorkingDayTypes,
            Boolean useBusinessDaysForProgressBand) {
        if (Boolean.TRUE.equals(useBusinessDaysForProgressBand)) {
            int businessDays = dateband.enumerateBusinessDays(task.start, task.finish, holidaySet, nonWorkingDayTypes).size();
            return businessDays > 0 ? businessDays + "営業日" : "-";
        }
        int calendarDays = dateband.buildDateBand(task.start, task.finish).size();
        return calendarDays > 0 ? calendarDays + "日" : "-";
    }

    private String formatWbsDate(String value) {
        return value == null || value.length() < 10 ? "-" : value.substring(0, 10);
    }

    private String formatTreeDate(String value) {
        if (value == null || value.length() < 10) {
            return "-";
        }
        try {
            int month = Integer.parseInt(value.substring(5, 7));
            int day = Integer.parseInt(value.substring(8, 10));
            return month + "/" + day;
        } catch (RuntimeException ex) {
            return value.substring(0, 10);
        }
    }

    private String formatTreeDateRange(String start, String finish) {
        String startLabel = formatTreeDate(start);
        String finishLabel = formatTreeDate(finish);
        return startLabel.equals(finishLabel) ? startLabel : startLabel + " - " + finishLabel;
    }

    private String formatNoteCell(String value) {
        String normalized = normalizeTextBlock(value);
        return normalized.isEmpty() ? "-" : normalized;
    }

    private String formatPercentCell(Integer value) {
        if (value == null) {
            return "-";
        }
        int bounded = Math.max(0, Math.min(100, value.intValue()));
        return bounded + "%";
    }

    private String formatOptionalCount(Integer value) {
        return value == null ? "-" : String.valueOf(Math.max(0, value.intValue()));
    }

    private String formatCalendarLabel(String calendarUid, Map<String, String> calendarNameByUid) {
        if (calendarUid == null || calendarUid.isEmpty()) {
            return "-";
        }
        String calendarName = calendarNameByUid.get(calendarUid);
        return calendarName == null ? calendarUid : calendarUid + " " + calendarName;
    }

    private String firstResourceName(List<String> resourceNames) {
        return resourceNames == null || resourceNames.isEmpty() ? "-" : resourceNames.get(0);
    }

    private String formatTreeInlineText(String value) {
        return normalizeFenceText(value).replace("\n", " / ");
    }

    private String normalizeTextBlock(String value) {
        String source = value == null ? "" : value;
        String normalized = source.replace("\r\n", "\n").replace('\r', '\n').replace('\t', ' ');
        String[] lines = normalized.split("\n", -1);
        List<String> result = new ArrayList<String>();
        for (String line : lines) {
            String trimmedEnd = trimEnd(stripControlCharacters(line));
            if (trimmedEnd.isEmpty() && !result.isEmpty() && result.get(result.size() - 1).isEmpty()) {
                continue;
            }
            result.add(trimmedEnd);
        }
        while (!result.isEmpty() && result.get(result.size() - 1).isEmpty()) {
            result.remove(result.size() - 1);
        }
        return joinLines(result).trim();
    }

    private String normalizeFenceText(String value) {
        return normalizeTextBlock(value);
    }

    private String escapeMarkdownCell(String value) {
        return markdownEscape.escapeMarkdownTableCell(value);
    }

    private String safe(String value, String fallback) {
        return value == null || value.isEmpty() ? fallback : value;
    }

    private String join(List<String> values, String separator, String fallback) {
        if (values == null || values.isEmpty()) {
            return fallback;
        }
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < values.size(); index++) {
            if (index > 0) {
                builder.append(separator);
            }
            builder.append(values.get(index));
        }
        return builder.toString();
    }

    private String joinLines(List<String> lines) {
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < lines.size(); index++) {
            if (index > 0) {
                builder.append('\n');
            }
            builder.append(lines.get(index));
        }
        return builder.toString();
    }

    private String repeat(char ch, int count) {
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < count; index++) {
            builder.append(ch);
        }
        return builder.toString();
    }

    private String trimEnd(String value) {
        int end = value.length();
        while (end > 0 && Character.isWhitespace(value.charAt(end - 1))) {
            end--;
        }
        return value.substring(0, end);
    }

    private String stripControlCharacters(String value) {
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < value.length(); index++) {
            char ch = value.charAt(index);
            if ((ch >= 0 && ch <= 8) || ch == 11 || ch == 12 || (ch >= 14 && ch <= 31) || ch == 127) {
                continue;
            }
            builder.append(ch);
        }
        return builder.toString();
    }

    public static class WbsMarkdownOptions {
        public List<String> holidayDates = new ArrayList<String>();
        public Integer displayDaysBeforeBaseDate;
        public Integer displayDaysAfterBaseDate;
        public Boolean useBusinessDaysForDisplayRange;
        public Boolean useBusinessDaysForProgressBand;
    }
}
