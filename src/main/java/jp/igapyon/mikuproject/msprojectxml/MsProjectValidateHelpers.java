/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuproject.msprojectxml;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.List;

import jp.igapyon.mikuproject.model.AssignmentModel;
import jp.igapyon.mikuproject.model.CalendarModel;
import jp.igapyon.mikuproject.model.ProjectModel;
import jp.igapyon.mikuproject.model.ResourceModel;
import jp.igapyon.mikuproject.model.TaskModel;

public class MsProjectValidateHelpers {
    public Long parseDateValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        String trimmed = value.trim();
        try {
            return Long.valueOf(Instant.parse(trimmed).toEpochMilli());
        } catch (DateTimeParseException ex) {
            // continue
        }
        try {
            return Long.valueOf(OffsetDateTime.parse(trimmed).toInstant().toEpochMilli());
        } catch (DateTimeParseException ex) {
            // continue
        }
        try {
            return Long.valueOf(LocalDateTime.parse(trimmed).toInstant(ZoneOffset.UTC).toEpochMilli());
        } catch (DateTimeParseException ex) {
            // continue
        }
        try {
            return Long.valueOf(LocalDate.parse(trimmed).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli());
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    public boolean isPlaceholderUid(String value) {
        return "0".equals(value == null ? "" : value.trim());
    }

    public boolean isUnassignedResourceUid(String value) {
        return "-65535".equals(value == null ? "" : value.trim());
    }

    public String describeTask(TaskModel task) {
        if (task == null) {
            return "UID=(なし)";
        }
        return "UID=" + blankSafe(task.uid) + (isBlank(task.name) ? "" : " (" + task.name + ")");
    }

    public TaskOrderIssue detectTaskOrderIssue(List<TaskModel> tasks) {
        TaskModel previousComparableTask = null;
        for (TaskModel task : tasks) {
            if (task == null || isPlaceholderUid(task.uid) || !isComparableOutlineNumber(task.outlineNumber)) {
                continue;
            }
            if (previousComparableTask != null
                    && compareOutlineNumbers(previousComparableTask.outlineNumber, task.outlineNumber) >= 0) {
                TaskOrderIssue issue = new TaskOrderIssue();
                issue.previous = previousComparableTask;
                issue.current = task;
                return issue;
            }
            previousComparableTask = task;
        }
        return null;
    }

    public String describeResource(ResourceModel resource) {
        if (resource == null) {
            return "UID=(なし)";
        }
        return "UID=" + blankSafe(resource.uid) + (isBlank(resource.name) ? "" : " (" + resource.name + ")");
    }

    public String describeCalendar(CalendarModel calendar) {
        if (calendar == null) {
            return "UID=(なし)";
        }
        return "UID=" + blankSafe(calendar.uid) + (isBlank(calendar.name) ? "" : " (" + calendar.name + ")");
    }

    public String describeAssignment(AssignmentModel assignment) {
        if (assignment == null) {
            return "UID=(なし)";
        }
        return "UID=" + blankSafe(assignment.uid);
    }

    public String describeTaskRef(ProjectModel model, String taskUid) {
        if (taskUid == null || taskUid.trim().isEmpty()) {
            return "TaskUID=(なし)";
        }
        if (model != null && model.tasks != null) {
            for (TaskModel task : model.tasks) {
                if (task != null && taskUid.equals(task.uid)) {
                    return "TaskUID=" + taskUid + (isBlank(task.name) ? "" : " (" + task.name + ")");
                }
            }
        }
        return "TaskUID=" + taskUid;
    }

    public String describeResourceRef(ProjectModel model, String resourceUid) {
        if (resourceUid == null || resourceUid.trim().isEmpty()) {
            return "ResourceUID=(なし)";
        }
        if (model != null && model.resources != null) {
            for (ResourceModel resource : model.resources) {
                if (resource != null && resourceUid.equals(resource.uid)) {
                    return "ResourceUID=" + resourceUid + (isBlank(resource.name) ? "" : " (" + resource.name + ")");
                }
            }
        }
        return "ResourceUID=" + resourceUid;
    }

    private boolean isComparableOutlineNumber(String value) {
        if (value == null || value.trim().isEmpty()) {
            return false;
        }
        String[] parts = value.split("\\.");
        for (String part : parts) {
            if (part.isEmpty()) {
                return false;
            }
            for (int index = 0; index < part.length(); index += 1) {
                if (!Character.isDigit(part.charAt(index))) {
                    return false;
                }
            }
        }
        return true;
    }

    private int compareOutlineNumbers(String left, String right) {
        if (left == null || left.trim().isEmpty() || right == null || right.trim().isEmpty()) {
            return 0;
        }
        String[] leftParts = left.split("\\.");
        String[] rightParts = right.split("\\.");
        int maxLength = Math.max(leftParts.length, rightParts.length);
        for (int index = 0; index < maxLength; index += 1) {
            if (index >= leftParts.length) {
                return -1;
            }
            if (index >= rightParts.length) {
                return 1;
            }
            int leftPart = Integer.parseInt(leftParts[index]);
            int rightPart = Integer.parseInt(rightParts[index]);
            if (leftPart != rightPart) {
                return leftPart - rightPart;
            }
        }
        return 0;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String blankSafe(String value) {
        return isBlank(value) ? "(なし)" : value;
    }

    public static class TaskOrderIssue {
        public TaskModel previous;
        public TaskModel current;
    }
}
