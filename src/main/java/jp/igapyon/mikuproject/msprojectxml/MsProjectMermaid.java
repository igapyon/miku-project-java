/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuproject.msprojectxml;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jp.igapyon.mikuproject.model.PredecessorModel;
import jp.igapyon.mikuproject.model.ProjectModel;
import jp.igapyon.mikuproject.model.TaskModel;

public class MsProjectMermaid {
    public String exportMermaidGantt(ProjectModel model) {
        String projectName = model != null && model.project != null ? model.project.name : null;
        List<String> lines = new ArrayList<String>();
        lines.add("gantt");
        lines.add("  title " + normalizeMermaidGanttLabel(projectName, "Project", "Project"));
        lines.add("  dateFormat YYYY-MM-DDTHH:mm:ss");
        lines.add("  axisFormat %m/%d");

        List<TaskModel> tasks = model == null || model.tasks == null ? new ArrayList<TaskModel>() : model.tasks;
        Map<String, String> sectionMap = buildTaskSectionMap(tasks, projectName);
        Map<String, String> taskNameMap = new LinkedHashMap<String, String>();
        for (TaskModel task : tasks) {
            if (task != null) {
                taskNameMap.put(task.uid, normalizeMermaidGanttLabel(task.name, "Task " + safeTaskKey(task), "Task"));
            }
        }

        List<TaskModel> exportedTasks = new ArrayList<TaskModel>();
        for (TaskModel task : tasks) {
            if (task != null && !task.summary && !isBlank(task.start) && !isBlank(task.finish)) {
                exportedTasks.add(task);
            }
        }

        String currentSection = "";
        for (TaskModel task : exportedTasks) {
            String section = sectionMap.containsKey(task.uid) ? sectionMap.get(task.uid) : "Tasks";
            if (!section.equals(currentSection)) {
                currentSection = section;
                lines.add("  section " + section);
            }

            List<String> tags = new ArrayList<String>();
            if (Boolean.TRUE.equals(task.critical)) {
                tags.add("crit");
            }
            if (task.milestone) {
                tags.add("milestone");
            } else if (task.percentComplete != null && task.percentComplete.intValue() >= 100) {
                tags.add("done");
            } else if (task.percentComplete != null && task.percentComplete.intValue() > 0) {
                tags.add("active");
            }

            String taskId = "task_" + normalizeMermaidTaskId(firstNonBlank(task.uid, task.id, "x"), "x");
            PredecessorModel singlePredecessor = task.predecessors != null && task.predecessors.size() == 1
                    ? task.predecessors.get(0)
                    : null;
            String nativeDependencyTarget = singlePredecessor == null ? null
                    : "task_" + normalizeMermaidTaskId(singlePredecessor.predecessorUid, "x");
            String nativeDuration = !task.milestone ? toMermaidDuration(task.duration) : null;
            boolean useNativeDependency = singlePredecessor != null && nativeDependencyTarget != null && nativeDuration != null
                    && isZeroDuration(singlePredecessor.linkLag)
                    && (singlePredecessor.type == null || singlePredecessor.type.intValue() == 1);

            List<String> fields = new ArrayList<String>();
            fields.addAll(tags);
            fields.add(taskId);
            if (useNativeDependency) {
                fields.add("after " + nativeDependencyTarget);
                fields.add(nativeDuration);
            } else {
                addIfNotBlank(fields, task.start);
                addIfNotBlank(fields, task.finish);
            }
            lines.add("  " + normalizeMermaidGanttLabel(task.name, "Task " + safeTaskKey(task), "Task") + " :"
                    + join(fields, ", "));

            if (task.predecessors != null) {
                for (PredecessorModel predecessor : task.predecessors) {
                    if (predecessor == null) {
                        continue;
                    }
                    String predecessorTaskId = "task_" + normalizeMermaidTaskId(predecessor.predecessorUid, "x");
                    String predecessorName = taskNameMap.containsKey(predecessor.predecessorUid)
                            ? taskNameMap.get(predecessor.predecessorUid)
                            : "Task " + predecessor.predecessorUid;
                    if (useNativeDependency && predecessorTaskId.equals(nativeDependencyTarget)) {
                        lines.add("  %% dependency(native): " + fallbackTaskName(task, taskId) + " after " + predecessorName
                                + " (" + taskId + " after " + predecessorTaskId + ")");
                        continue;
                    }
                    String details = buildDependencyDetails(predecessor);
                    lines.add("  %% dependency: " + fallbackTaskName(task, taskId) + " after " + predecessorName
                            + (details.isEmpty() ? "" : " (" + details + ")") + " [" + taskId + " after "
                            + predecessorTaskId + "]");
                    if (!isZeroDuration(predecessor.linkLag)) {
                        lines.add("  %% dependency(pseudo): " + fallbackTaskName(task, taskId) + " ~= after "
                                + predecessorName + " + " + formatMermaidLag(predecessor.linkLag));
                    }
                }
                if (task.predecessors.size() > 1) {
                    lines.add("  %% dependency(note): " + fallbackTaskName(task, taskId) + " has multiple predecessors");
                } else if (singlePredecessor != null && !useNativeDependency) {
                    String reasons = buildNonNativeDependencyReasons(task, singlePredecessor, nativeDuration);
                    if (!reasons.isEmpty()) {
                        lines.add("  %% dependency(note): " + fallbackTaskName(task, taskId)
                                + " kept as comment because " + reasons);
                    }
                }
            }
        }

        if (exportedTasks.isEmpty()) {
            lines.add("  section Tasks");
            lines.add("  No tasks :milestone, empty_0, 1970-01-01T00:00:00, 1970-01-01T00:00:00");
        }
        return join(lines, "\n") + "\n";
    }

    private Map<String, String> buildTaskSectionMap(List<TaskModel> tasks, String projectName) {
        Map<String, String> sectionMap = new LinkedHashMap<String, String>();
        Deque<TaskModel> summaryStack = new ArrayDeque<TaskModel>();
        for (TaskModel task : tasks) {
            if (task == null) {
                continue;
            }
            while (!summaryStack.isEmpty()
                    && compareOutlineLevel(task.outlineLevel, summaryStack.peekLast().outlineLevel) <= 0) {
                summaryStack.removeLast();
            }
            if (task.summary) {
                summaryStack.addLast(task);
                continue;
            }
            String sectionName = summaryStack.isEmpty()
                    ? normalizeMermaidGanttLabel(projectName, "Tasks", "Section")
                    : normalizeMermaidGanttLabel(summaryStack.peekLast().name, "Summary", "Section");
            sectionMap.put(task.uid, sectionName);
        }
        return sectionMap;
    }

    private int compareOutlineLevel(Integer left, Integer right) {
        int leftValue = left == null ? 0 : left.intValue();
        int rightValue = right == null ? 0 : right.intValue();
        return Integer.compare(leftValue, rightValue);
    }

    private String buildDependencyDetails(PredecessorModel predecessor) {
        List<String> parts = new ArrayList<String>();
        parts.add("type=" + describePredecessorType(predecessor.type));
        if (!isZeroDuration(predecessor.linkLag)) {
            parts.add("lag=" + formatMermaidLag(predecessor.linkLag));
        }
        return join(parts, ", ");
    }

    private String buildNonNativeDependencyReasons(TaskModel task, PredecessorModel predecessor, String nativeDuration) {
        List<String> reasons = new ArrayList<String>();
        if (!isZeroDuration(predecessor.linkLag)) {
            reasons.add("lag=" + formatMermaidLag(predecessor.linkLag));
        }
        if (predecessor.type != null && predecessor.type.intValue() != 1) {
            reasons.add("type=" + describePredecessorType(predecessor.type));
        }
        if (nativeDuration == null && !task.milestone) {
            reasons.add("duration=" + firstNonBlank(task.duration, "(empty)"));
        }
        return join(reasons, ", ");
    }

    private String fallbackTaskName(TaskModel task, String fallback) {
        return firstNonBlank(task.name, fallback);
    }

    private String describePredecessorType(Integer type) {
        if (type == null) {
            return "default";
        }
        if (type.intValue() == 0) {
            return "FF";
        }
        if (type.intValue() == 1) {
            return "FS";
        }
        if (type.intValue() == 2) {
            return "FF";
        }
        if (type.intValue() == 3) {
            return "SF";
        }
        if (type.intValue() == 4) {
            return "SS";
        }
        return "type=" + type;
    }

    private String normalizeMermaidText(String value, String fallback) {
        String text = firstNonBlank(value, fallback);
        text = text.replace(':', ' ').replace('：', ' ').replace('#', ' ').replace(',', ' ').replace('，', ' ');
        text = text.replaceAll("\\s+", " ").trim();
        return text.isEmpty() ? fallback : text;
    }

    private String normalizeMermaidGanttLabel(String value, String fallback, String leadingPrefix) {
        String text = normalizeMermaidText(value, fallback);
        return text.matches("^\\d.*") ? leadingPrefix + " " + text : text;
    }

    private String normalizeMermaidTaskId(String value, String fallback) {
        return firstNonBlank(value, fallback).replaceAll("[^A-Za-z0-9_]", "_");
    }

    private String toMermaidDuration(String duration) {
        String text = safe(duration).trim();
        if (text.isEmpty()) {
            return null;
        }
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("^P(?:T(?:(\\d+)H)?(?:(\\d+)M)?(?:(\\d+)S)?)$")
                .matcher(text);
        if (!matcher.matches()) {
            return null;
        }
        int hours = matcher.group(1) == null ? 0 : Integer.parseInt(matcher.group(1));
        int minutes = matcher.group(2) == null ? 0 : Integer.parseInt(matcher.group(2));
        int seconds = matcher.group(3) == null ? 0 : Integer.parseInt(matcher.group(3));
        List<String> parts = new ArrayList<String>();
        if (hours > 0) {
            parts.add(hours + "h");
        }
        if (minutes > 0) {
            parts.add(minutes + "m");
        }
        if (seconds > 0) {
            parts.add(seconds + "s");
        }
        return parts.isEmpty() ? null : join(parts, " ");
    }

    private String formatMermaidLag(String duration) {
        String shortValue = toMermaidDuration(duration);
        return shortValue != null ? shortValue : safe(duration).trim();
    }

    private boolean isZeroDuration(String duration) {
        String text = safe(duration).trim();
        return text.isEmpty() || "PT0H0M0S".equals(text) || "PT0M0S".equals(text) || "PT0S".equals(text);
    }

    private void addIfNotBlank(List<String> values, String value) {
        if (!isBlank(value)) {
            values.add(value);
        }
    }

    private String join(List<String> values, String delimiter) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                builder.append(delimiter);
            }
            builder.append(values.get(i));
        }
        return builder.toString();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String firstNonBlank(String first, String second) {
        return !isBlank(first) ? first : second;
    }

    private String firstNonBlank(String first, String second, String third) {
        return !isBlank(first) ? first : firstNonBlank(second, third);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String safeTaskKey(TaskModel task) {
        return firstNonBlank(task.uid, task.id, "x");
    }
}
