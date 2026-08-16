/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuproject.conformance;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Small strict JSON parser for runtime-manifest intake. Unlike the historical
 * project JSON helper it rejects duplicate object keys and malformed UTF-8,
 * which are part of the v1 runtime trust boundary.
 */
final class ConformanceStrictJson {
    private ConformanceStrictJson() {
    }

    static Map<String, Object> parseUtf8Object(byte[] bytes) throws StrictJsonException {
        Object value = new Parser(decodeUtf8(bytes)).parse();
        if (!(value instanceof Map<?, ?>)) {
            throw failure("root-not-object", "JSON root must be an object");
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> object = (Map<String, Object>) value;
        return object;
    }

    private static String decodeUtf8(byte[] bytes) throws StrictJsonException {
        try {
            CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder();
            decoder.onMalformedInput(CodingErrorAction.REPORT);
            decoder.onUnmappableCharacter(CodingErrorAction.REPORT);
            CharBuffer decoded = decoder.decode(ByteBuffer.wrap(bytes));
            return decoded.toString();
        } catch (CharacterCodingException error) {
            throw failure("invalid-utf8", "JSON is not valid UTF-8", error);
        }
    }

    private static StrictJsonException failure(String message) {
        return new StrictJsonException("syntax", message);
    }

    private static StrictJsonException failure(String message, Throwable cause) {
        return new StrictJsonException("syntax", message, cause);
    }

    private static StrictJsonException failure(String code, String message) {
        return new StrictJsonException(code, message);
    }

    private static StrictJsonException failure(String code, String message, Throwable cause) {
        return new StrictJsonException(code, message, cause);
    }

    private static final class Parser {
        private final String text;
        private int index;

        Parser(String text) {
            this.text = text;
        }

        Object parse() throws StrictJsonException {
            skipWhitespace();
            Object value = parseValue();
            skipWhitespace();
            if (index != text.length()) {
                throw error("unexpected trailing content");
            }
            return value;
        }

        private Object parseValue() throws StrictJsonException {
            skipWhitespace();
            if (index >= text.length()) {
                throw error("expected a JSON value");
            }
            char character = text.charAt(index);
            if (character == '{') {
                return parseObject();
            }
            if (character == '[') {
                return parseArray();
            }
            if (character == '"') {
                return parseString();
            }
            if (character == 't') {
                expectKeyword("true");
                return Boolean.TRUE;
            }
            if (character == 'f') {
                expectKeyword("false");
                return Boolean.FALSE;
            }
            if (character == 'n') {
                expectKeyword("null");
                return null;
            }
            if (character == '-' || isDigit(character)) {
                return parseNumber();
            }
            throw error("invalid JSON value");
        }

        private Map<String, Object> parseObject() throws StrictJsonException {
            LinkedHashMap<String, Object> object = new LinkedHashMap<String, Object>();
            expect('{');
            skipWhitespace();
            if (peek('}')) {
                expect('}');
                return object;
            }
            while (true) {
                skipWhitespace();
                if (!peek('"')) {
                    throw error("object key must be a string");
                }
                String key = parseString();
                if (object.containsKey(key)) {
                    throw failure("duplicate-key", "duplicate object key: " + key);
                }
                skipWhitespace();
                expect(':');
                object.put(key, parseValue());
                skipWhitespace();
                if (peek('}')) {
                    expect('}');
                    return object;
                }
                expect(',');
            }
        }

        private List<Object> parseArray() throws StrictJsonException {
            List<Object> values = new ArrayList<Object>();
            expect('[');
            skipWhitespace();
            if (peek(']')) {
                expect(']');
                return values;
            }
            while (true) {
                values.add(parseValue());
                skipWhitespace();
                if (peek(']')) {
                    expect(']');
                    return values;
                }
                expect(',');
            }
        }

        private String parseString() throws StrictJsonException {
            expect('"');
            StringBuilder value = new StringBuilder();
            while (index < text.length()) {
                char character = text.charAt(index++);
                if (character == '"') {
                    validateUnicodeScalars(value.toString());
                    return value.toString();
                }
                if (character < 0x20) {
                    throw error("unescaped control character in string");
                }
                if (character != '\\') {
                    value.append(character);
                    continue;
                }
                if (index >= text.length()) {
                    throw error("unterminated escape");
                }
                char escaped = text.charAt(index++);
                if (escaped == '"' || escaped == '\\' || escaped == '/') {
                    value.append(escaped);
                } else if (escaped == 'b') {
                    value.append('\b');
                } else if (escaped == 'f') {
                    value.append('\f');
                } else if (escaped == 'n') {
                    value.append('\n');
                } else if (escaped == 'r') {
                    value.append('\r');
                } else if (escaped == 't') {
                    value.append('\t');
                } else if (escaped == 'u') {
                    value.append(parseUnicodeEscape());
                } else {
                    throw error("invalid escape");
                }
            }
            throw error("unterminated string");
        }

        private char parseUnicodeEscape() throws StrictJsonException {
            if (index + 4 > text.length()) {
                throw error("truncated unicode escape");
            }
            String hex = text.substring(index, index + 4);
            index += 4;
            try {
                return (char) Integer.parseInt(hex, 16);
            } catch (NumberFormatException error) {
                throw error("invalid unicode escape");
            }
        }

        private Number parseNumber() throws StrictJsonException {
            int start = index;
            if (peek('-')) {
                index++;
            }
            if (index >= text.length()) {
                throw error("invalid number");
            }
            if (peek('0')) {
                index++;
                if (index < text.length() && isDigit(text.charAt(index))) {
                    throw error("leading zero in number");
                }
            } else {
                consumeDigits();
            }
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
            } catch (NumberFormatException error) {
                throw error("invalid number");
            }
        }

        private void consumeDigits() throws StrictJsonException {
            if (index >= text.length() || !isDigit(text.charAt(index))) {
                throw error("expected digit");
            }
            while (index < text.length() && isDigit(text.charAt(index))) {
                index++;
            }
        }

        private void expectKeyword(String keyword) throws StrictJsonException {
            if (!text.regionMatches(index, keyword, 0, keyword.length())) {
                throw error("invalid JSON keyword");
            }
            index += keyword.length();
        }

        private void expect(char expected) throws StrictJsonException {
            if (index >= text.length() || text.charAt(index) != expected) {
                throw error("expected '" + expected + "'");
            }
            index++;
        }

        private boolean peek(char character) {
            return index < text.length() && text.charAt(index) == character;
        }

        private void skipWhitespace() {
            while (index < text.length()) {
                char character = text.charAt(index);
                if (character == ' ' || character == '\n' || character == '\r' || character == '\t') {
                    index++;
                } else {
                    return;
                }
            }
        }

        private static boolean isDigit(char character) {
            return character >= '0' && character <= '9';
        }

        private static void validateUnicodeScalars(String value) throws StrictJsonException {
            for (int offset = 0; offset < value.length(); offset++) {
                char character = value.charAt(offset);
                if (Character.isHighSurrogate(character)) {
                    if (offset + 1 >= value.length() || !Character.isLowSurrogate(value.charAt(offset + 1))) {
                        throw failure("unpaired high surrogate");
                    }
                    offset++;
                } else if (Character.isLowSurrogate(character)) {
                    throw failure("unpaired low surrogate");
                }
            }
        }

        private StrictJsonException error(String message) {
            return failure(message + " at index " + index);
        }
    }

    static final class StrictJsonException extends Exception {
        private static final long serialVersionUID = 1L;
        final String code;

        StrictJsonException(String code, String message) {
            super(message);
            this.code = code;
        }

        StrictJsonException(String code, String message, Throwable cause) {
            super(message, cause);
            this.code = code;
        }
    }
}
