/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuproject.coreapi;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CoreApiAiJsonUtil {
    private static final Pattern JSON_FENCE_PATTERN = Pattern.compile("```json\\s*([\\s\\S]*?)```");

    public String extractLastJsonBlock(String value) {
        String text = value == null ? "" : value;
        Matcher matcher = JSON_FENCE_PATTERN.matcher(text);
        String lastMatch = null;
        while (matcher.find()) {
            lastMatch = matcher.group(1);
        }
        if (lastMatch != null) {
            return lastMatch.trim();
        }
        return text.trim();
    }

    public String detectJsonDocumentKind(Object documentLike) {
        if (!(documentLike instanceof Map<?, ?>)) {
            return null;
        }
        Map<?, ?> candidate = (Map<?, ?>) documentLike;
        Object format = candidate.get("format");
        Object viewType = candidate.get("view_type");
        Object operations = candidate.get("operations");
        if ("mikuproject_workbook_json".equals(format)) {
            return "workbook_json";
        }
        if ("project_draft_view".equals(viewType)) {
            return "project_draft_view";
        }
        if (operations instanceof java.util.List<?>) {
            return "patch_json";
        }
        return null;
    }

    public Object parseJsonText(String jsonText) {
        return new JsonParser(jsonText).parse();
    }

    public String stringifyJson(Object value) {
        StringBuilder builder = new StringBuilder();
        appendJson(builder, value);
        return builder.toString();
    }

    @SuppressWarnings("unchecked")
    private void appendJson(StringBuilder builder, Object value) {
        if (value == null) {
            builder.append("null");
            return;
        }
        if (value instanceof String) {
            appendJsonString(builder, (String) value);
            return;
        }
        if (value instanceof Number || value instanceof Boolean) {
            builder.append(String.valueOf(value));
            return;
        }
        if (value instanceof Map<?, ?>) {
            builder.append("{");
            boolean first = true;
            for (Map.Entry<Object, Object> entry : ((Map<Object, Object>) value).entrySet()) {
                if (!first) {
                    builder.append(",");
                }
                first = false;
                appendJsonString(builder, String.valueOf(entry.getKey()));
                builder.append(":");
                appendJson(builder, entry.getValue());
            }
            builder.append("}");
            return;
        }
        if (value instanceof List<?>) {
            builder.append("[");
            boolean first = true;
            for (Object item : (List<Object>) value) {
                if (!first) {
                    builder.append(",");
                }
                first = false;
                appendJson(builder, item);
            }
            builder.append("]");
            return;
        }
        appendJsonString(builder, String.valueOf(value));
    }

    private void appendJsonString(StringBuilder builder, String value) {
        builder.append('"');
        String text = value == null ? "" : value;
        for (int index = 0; index < text.length(); index++) {
            char ch = text.charAt(index);
            if (ch == '"' || ch == '\\') {
                builder.append('\\').append(ch);
            } else if (ch == '\b') {
                builder.append("\\b");
            } else if (ch == '\f') {
                builder.append("\\f");
            } else if (ch == '\n') {
                builder.append("\\n");
            } else if (ch == '\r') {
                builder.append("\\r");
            } else if (ch == '\t') {
                builder.append("\\t");
            } else if (ch < 0x20) {
                String hex = Integer.toHexString(ch);
                builder.append("\\u");
                for (int pad = hex.length(); pad < 4; pad++) {
                    builder.append('0');
                }
                builder.append(hex);
            } else {
                builder.append(ch);
            }
        }
        builder.append('"');
    }

    private static class JsonParser {
        private final String text;
        private int index;

        JsonParser(String text) {
            this.text = text == null ? "" : text;
        }

        Object parse() {
            skipWhitespace();
            Object value = parseValue();
            skipWhitespace();
            if (index != text.length()) {
                throw error("JSON の末尾に余分な文字があります");
            }
            return value;
        }

        private Object parseValue() {
            skipWhitespace();
            if (index >= text.length()) {
                throw error("JSON が空です");
            }
            char ch = text.charAt(index);
            if (ch == '{') {
                return parseObject();
            }
            if (ch == '[') {
                return parseArray();
            }
            if (ch == '"') {
                return parseString();
            }
            if (ch == 't') {
                expectKeyword("true");
                return Boolean.TRUE;
            }
            if (ch == 'f') {
                expectKeyword("false");
                return Boolean.FALSE;
            }
            if (ch == 'n') {
                expectKeyword("null");
                return null;
            }
            if (ch == '-' || isDigit(ch)) {
                return parseNumber();
            }
            throw error("JSON の値を解釈できません");
        }

        private Map<String, Object> parseObject() {
            LinkedHashMap<String, Object> map = new LinkedHashMap<String, Object>();
            expect('{');
            skipWhitespace();
            if (peek('}')) {
                expect('}');
                return map;
            }
            while (true) {
                skipWhitespace();
                String key = parseString();
                skipWhitespace();
                expect(':');
                Object value = parseValue();
                map.put(key, value);
                skipWhitespace();
                if (peek('}')) {
                    expect('}');
                    return map;
                }
                expect(',');
            }
        }

        private List<Object> parseArray() {
            List<Object> list = new ArrayList<Object>();
            expect('[');
            skipWhitespace();
            if (peek(']')) {
                expect(']');
                return list;
            }
            while (true) {
                list.add(parseValue());
                skipWhitespace();
                if (peek(']')) {
                    expect(']');
                    return list;
                }
                expect(',');
            }
        }

        private String parseString() {
            expect('"');
            StringBuilder builder = new StringBuilder();
            while (index < text.length()) {
                char ch = text.charAt(index++);
                if (ch == '"') {
                    return builder.toString();
                }
                if (ch == '\\') {
                    if (index >= text.length()) {
                        throw error("文字列の escape が不正です");
                    }
                    char escaped = text.charAt(index++);
                    if (escaped == '"' || escaped == '\\' || escaped == '/') {
                        builder.append(escaped);
                    } else if (escaped == 'b') {
                        builder.append('\b');
                    } else if (escaped == 'f') {
                        builder.append('\f');
                    } else if (escaped == 'n') {
                        builder.append('\n');
                    } else if (escaped == 'r') {
                        builder.append('\r');
                    } else if (escaped == 't') {
                        builder.append('\t');
                    } else if (escaped == 'u') {
                        builder.append(parseUnicodeEscape());
                    } else {
                        throw error("未対応の escape です");
                    }
                    continue;
                }
                builder.append(ch);
            }
            throw error("文字列の終端が見つかりません");
        }

        private char parseUnicodeEscape() {
            if (index + 4 > text.length()) {
                throw error("unicode escape が不正です");
            }
            String hex = text.substring(index, index + 4);
            index += 4;
            try {
                return (char) Integer.parseInt(hex, 16);
            } catch (NumberFormatException ex) {
                throw error("unicode escape が不正です");
            }
        }

        private Number parseNumber() {
            int start = index;
            if (peek('-')) {
                index++;
            }
            consumeDigits();
            boolean decimal = false;
            if (peek('.')) {
                decimal = true;
                index++;
                consumeDigits();
            }
            if (peek('e') || peek('E')) {
                decimal = true;
                index++;
                if (peek('+') || peek('-')) {
                    index++;
                }
                consumeDigits();
            }
            String token = text.substring(start, index);
            try {
                if (decimal) {
                    return Double.valueOf(token);
                }
                long value = Long.parseLong(token);
                if (value >= Integer.MIN_VALUE && value <= Integer.MAX_VALUE) {
                    return Integer.valueOf((int) value);
                }
                return Long.valueOf(value);
            } catch (NumberFormatException ex) {
                throw error("数値を解釈できません");
            }
        }

        private void consumeDigits() {
            if (index >= text.length() || !isDigit(text.charAt(index))) {
                throw error("数値を解釈できません");
            }
            while (index < text.length() && isDigit(text.charAt(index))) {
                index++;
            }
        }

        private void expectKeyword(String keyword) {
            if (!text.regionMatches(index, keyword, 0, keyword.length())) {
                throw error("JSON の keyword を解釈できません");
            }
            index += keyword.length();
        }

        private void expect(char expected) {
            if (index >= text.length() || text.charAt(index) != expected) {
                throw error("JSON の記号 '" + expected + "' を期待しました");
            }
            index++;
        }

        private boolean peek(char value) {
            return index < text.length() && text.charAt(index) == value;
        }

        private void skipWhitespace() {
            while (index < text.length()) {
                char ch = text.charAt(index);
                if (ch == ' ' || ch == '\n' || ch == '\r' || ch == '\t') {
                    index++;
                    continue;
                }
                return;
            }
        }

        private boolean isDigit(char ch) {
            return ch >= '0' && ch <= '9';
        }

        private IllegalArgumentException error(String message) {
            return new IllegalArgumentException(message + " at index " + index);
        }
    }
}
