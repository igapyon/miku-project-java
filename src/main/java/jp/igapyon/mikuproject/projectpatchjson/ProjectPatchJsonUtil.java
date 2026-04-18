/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuproject.projectpatchjson;

import java.util.List;

import jp.igapyon.mikuproject.model.PredecessorModel;
import jp.igapyon.mikuproject.model.ProjectInfo;
import jp.igapyon.mikuproject.model.ProjectModel;
import jp.igapyon.mikuproject.model.TaskModel;
import jp.igapyon.mikuproject.msprojectxml.MsProjectXml;

public class ProjectPatchJsonUtil {
    public Integer resolveDependencyType(String rawType, List<PatchWarning> warnings, TaskModel task, int operationIndex,
            String opName, boolean allowEmpty) {
        String trimmed = safe(rawType).trim();
        if (trimmed.isEmpty()) {
            return allowEmpty ? null : Integer.valueOf(1);
        }
        if ("FS".equalsIgnoreCase(trimmed)) {
            return Integer.valueOf(1);
        }
        if ("SS".equalsIgnoreCase(trimmed)) {
            return Integer.valueOf(2);
        }
        if ("FF".equalsIgnoreCase(trimmed)) {
            return Integer.valueOf(3);
        }
        if ("SF".equalsIgnoreCase(trimmed)) {
            return Integer.valueOf(4);
        }
        PatchWarning warning = new PatchWarning();
        warning.message = opName + ".type は FS / SS / FF / SF のいずれかが必要です: operations[" + operationIndex
                + "].type = " + trimmed;
        warning.scope = "tasks";
        warning.uid = task.uid;
        warning.label = defaultLabel(task.name, task.uid);
        warnings.add(warning);
        return null;
    }

    public String resolveDependencyLag(PatchOperation operation, List<PatchWarning> warnings, TaskModel task, int operationIndex,
            String opName, boolean allowEmpty) {
        if (!isBlank(operation.lag) && operation.lagHours != null) {
            PatchWarning warning = new PatchWarning();
            warning.message = opName + ".lag と " + opName + ".lag_hours が同時指定されたため、lag_hours は無視します: "
                    + task.uid;
            warning.scope = "tasks";
            warning.uid = task.uid;
            warning.label = defaultLabel(task.name, task.uid);
            warnings.add(warning);
        }
        if (!isBlank(operation.lag)) {
            String normalizedLag = operation.lag.trim();
            if (!isValidDurationText(normalizedLag)) {
                PatchWarning warning = new PatchWarning();
                warning.message = opName + ".lag は ISO 8601 duration 形式が必要です: " + task.uid;
                warning.scope = "tasks";
                warning.uid = task.uid;
                warning.label = defaultLabel(task.name, task.uid);
                warnings.add(warning);
                return null;
            }
            return normalizedLag;
        }
        if (operation.lagHours != null) {
            return formatDurationHours(operation.lagHours.doubleValue());
        }
        return allowEmpty ? null : null;
    }

    public String formatDependencyType(Integer type) {
        if (type == null || type.intValue() == 1) {
            return "FS";
        }
        if (type.intValue() == 2) {
            return "SS";
        }
        if (type.intValue() == 3) {
            return "FF";
        }
        if (type.intValue() == 4) {
            return "SF";
        }
        return "type=" + type;
    }

    public boolean isZeroDuration(String duration) {
        String text = safe(duration).trim();
        return text.isEmpty() || "PT0H0M0S".equals(text) || "PT0M0S".equals(text) || "PT0S".equals(text);
    }

    public String normalizeDurationText(String duration) {
        String text = safe(duration).trim();
        return text.isEmpty() ? null : text;
    }

    public boolean isValidDurationText(String duration) {
        String text = safe(duration).trim();
        return text.matches("^-?P(?:T(?:(\\d+)H)?(?:(\\d+)M)?(?:(\\d+)S)?)$") && !text.equals("P");
    }

    public String formatPredecessor(PredecessorModel predecessor) {
        StringBuilder builder = new StringBuilder();
        builder.append(predecessor.predecessorUid).append("(").append(formatDependencyType(predecessor.type));
        if (!isZeroDuration(predecessor.linkLag)) {
            builder.append(", lag=").append(safe(predecessor.linkLag).trim());
        }
        builder.append(")");
        return builder.toString();
    }

    public String formatRequestedDependencyRelation(String fromUid, String toUid, Integer type, String linkLag) {
        StringBuilder builder = new StringBuilder();
        builder.append(fromUid).append(" -> ").append(toUid).append(" (").append(formatDependencyType(type));
        if (!isZeroDuration(linkLag)) {
            builder.append(", lag=").append(safe(linkLag).trim());
        }
        builder.append(")");
        return builder.toString();
    }

    public String formatPredecessorList(List<PredecessorModel> predecessors) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < predecessors.size(); i++) {
            if (i > 0) {
                builder.append(", ");
            }
            builder.append(formatPredecessor(predecessors.get(i)));
        }
        return builder.toString();
    }

    public String normalizePatchedTaskDate(Object value, String kind, TaskModel task, ProjectInfo project) {
        if (!(value instanceof String)) {
            return null;
        }
        String trimmed = ((String) value).trim();
        if (trimmed.isEmpty() || !isDateText(trimmed)) {
            return null;
        }
        if (isDateOnlyText(trimmed) && !task.milestone) {
            String timeText = "start".equals(kind) ? defaultString(project.defaultStartTime, "09:00:00")
                    : defaultString(project.defaultFinishTime, "18:00:00");
            return trimmed + "T" + timeText;
        }
        return trimmed;
    }

    public String normalizePatchedPlainDateTime(Object value, String kind, ProjectInfo project) {
        if (!(value instanceof String)) {
            return null;
        }
        String trimmed = ((String) value).trim();
        if (trimmed.isEmpty() || !isDateText(trimmed)) {
            return null;
        }
        if (isDateOnlyText(trimmed)) {
            String timeText = "start".equals(kind) ? defaultString(project.defaultStartTime, "09:00:00")
                    : defaultString(project.defaultFinishTime, "18:00:00");
            return trimmed + "T" + timeText;
        }
        return trimmed;
    }

    public String formatDurationHours(double hours) {
        long totalSeconds = Math.round(hours * 60d * 60d);
        long normalizedSeconds = Math.max(0, totalSeconds);
        long durationHours = normalizedSeconds / 3600;
        long durationMinutes = (normalizedSeconds % 3600) / 60;
        long durationSeconds = normalizedSeconds % 60;
        return "PT" + durationHours + "H" + durationMinutes + "M" + durationSeconds + "S";
    }

    public Double parseDurationHours(String duration) {
        String text = safe(duration).trim();
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("^PT(?:(\\d+(?:\\.\\d+)?)H)?(?:(\\d+(?:\\.\\d+)?)M)?(?:(\\d+(?:\\.\\d+)?)S)?$")
                .matcher(text);
        if (!matcher.matches()) {
            return null;
        }
        double hours = matcher.group(1) == null ? 0 : Double.parseDouble(matcher.group(1));
        double minutes = matcher.group(2) == null ? 0 : Double.parseDouble(matcher.group(2));
        double seconds = matcher.group(3) == null ? 0 : Double.parseDouble(matcher.group(3));
        return Double.valueOf(hours + minutes / 60d + seconds / 3600d);
    }

    public ProjectModel cloneProjectModel(ProjectModel model) {
        MsProjectXml xml = new MsProjectXml();
        return xml.importFromXml(xml.exportToXml(model));
    }

    private boolean isDateOnlyText(String value) {
        return value.matches("^\\d{4}-\\d{2}-\\d{2}$");
    }

    private boolean isDateText(String value) {
        if (isDateOnlyText(value)) {
            return true;
        }
        try {
            java.time.LocalDateTime.parse(value);
            return true;
        } catch (java.time.format.DateTimeParseException ex) {
            return false;
        }
    }

    private String defaultLabel(String name, String uid) {
        return isBlank(name) ? uid : name;
    }

    private String defaultString(String value, String fallback) {
        return isBlank(value) ? fallback : value;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
