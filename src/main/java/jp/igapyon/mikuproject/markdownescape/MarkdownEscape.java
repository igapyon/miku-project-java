/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuproject.markdownescape;

public class MarkdownEscape {
    public String escapeMarkdownLineStart(String text) {
        String value = text == null ? "" : text;
        value = value.replaceFirst("^(\\s*)([#>])", "$1\\\\$2");
        value = value.replaceFirst("^(\\s*)([-+*])(\\s+)", "$1\\\\$2$3");
        value = value.replaceFirst("^(\\s*)(\\d+)\\.(\\s+)", "$1$2\\\\.$3");
        return value;
    }

    public String escapeMarkdownLiteralText(String text) {
        String source = text == null ? "" : text;
        String normalized = source.replace("\r\n", "\n").replace("\r", "\n");
        String[] lines = normalized.split("\n", -1);
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < lines.length; index++) {
            if (index > 0) {
                builder.append('\n');
            }
            builder.append(escapeMarkdownLiteralLine(lines[index]));
        }
        return builder.toString();
    }

    public String escapeMarkdownTableCell(String text) {
        return escapeMarkdownLiteralText(text).replace("|", "\\|").replace("\n", "<br>");
    }

    private String escapeMarkdownLiteralLine(String text) {
        String source = text == null ? "" : text;
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < source.length(); index++) {
            char ch = source.charAt(index);
            boolean atLineStart = index == 0;
            char next = index + 1 < source.length() ? source.charAt(index + 1) : '\0';
            if (ch == '\\') {
                builder.append("\\\\");
                continue;
            }
            if (ch == '&') {
                builder.append("&amp;");
                continue;
            }
            if (ch == '<') {
                builder.append("&lt;");
                continue;
            }
            if (ch == '>') {
                builder.append("&gt;");
                continue;
            }
            if ("`*_{}[]()!|~".indexOf(ch) >= 0) {
                builder.append('\\').append(ch);
                continue;
            }
            if (atLineStart && ch == '#') {
                builder.append('\\').append(ch);
                continue;
            }
            if (atLineStart && "-+*".indexOf(ch) >= 0 && Character.isWhitespace(next)) {
                builder.append('\\').append(ch);
                continue;
            }
            if (atLineStart && Character.isDigit(ch)) {
                int cursor = index;
                while (cursor < source.length() && Character.isDigit(source.charAt(cursor))) {
                    cursor++;
                }
                if (cursor < source.length() && source.charAt(cursor) == '.'
                        && cursor + 1 < source.length() && Character.isWhitespace(source.charAt(cursor + 1))) {
                    builder.append(source.substring(index, cursor));
                    builder.append("\\.");
                    index = cursor;
                    continue;
                }
            }
            builder.append(ch);
        }
        return builder.toString();
    }
}
