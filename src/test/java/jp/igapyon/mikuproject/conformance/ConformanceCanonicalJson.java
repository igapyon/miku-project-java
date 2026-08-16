/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuproject.conformance;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Canonical JSON and SHA-256 helpers defined by the immutable v1 conformance
 * corpus. This is test-side infrastructure: product commands must not depend
 * on it until their CLI contract implementation is introduced.
 *
 * <p>The serializer deliberately does not reuse the historical JSON utility.
 * The latter preserves insertion order, whereas the v1 contract requires
 * recursive Unicode-code-point key ordering. It also rejects unpaired
 * surrogates and non-integer numbers instead of silently deriving a digest
 * from a runtime-specific representation.</p>
 */
final class ConformanceCanonicalJson {
    private ConformanceCanonicalJson() {
    }

    static String serialize(Object value) throws CanonicalJsonException {
        StringBuilder builder = new StringBuilder();
        appendValue(builder, value);
        return builder.toString();
    }

    static String sha256Hex(Object value) throws CanonicalJsonException {
        return sha256Hex(serialize(value).getBytes(StandardCharsets.UTF_8));
    }

    static String sha256RawBytes(byte[] bytes) throws CanonicalJsonException {
        if (bytes == null) {
            throw failure("raw bytes must not be null");
        }
        return sha256Hex(bytes);
    }

    static Map<String, Object> sha256Digest(Object value) throws CanonicalJsonException {
        LinkedHashMap<String, Object> digest = new LinkedHashMap<String, Object>();
        digest.put("algorithm", "sha-256");
        digest.put("value", sha256Hex(value));
        return digest;
    }

    /**
     * Computes the semantic-state digest after the v1 collection
     * canonicalization step. Task order is intentionally retained because it
     * represents root/sibling order; the other semantic collections are sets.
     */
    static String sha256SemanticStateHex(Object semanticState) throws CanonicalJsonException {
        return sha256Hex(serialize(normalizeSemanticCollections(semanticState, null)).getBytes(StandardCharsets.UTF_8));
    }

    static Map<String, Object> sha256SemanticStateDigest(Object semanticState) throws CanonicalJsonException {
        LinkedHashMap<String, Object> digest = new LinkedHashMap<String, Object>();
        digest.put("algorithm", "sha-256");
        digest.put("value", sha256SemanticStateHex(semanticState));
        return digest;
    }

    static boolean jsonEquals(Object left, Object right) throws CanonicalJsonException {
        return serialize(left).equals(serialize(right));
    }

    static Object deepCopy(Object value) throws CanonicalJsonException {
        if (value == null || value instanceof String || value instanceof Boolean || value instanceof Number) {
            // Serialize first so invalid number/string values cannot cross this
            // test-side contract boundary unnoticed.
            serialize(value);
            return value;
        }
        if (value instanceof Map<?, ?>) {
            LinkedHashMap<String, Object> copy = new LinkedHashMap<String, Object>();
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
                if (!(entry.getKey() instanceof String)) {
                    throw failure("JSON object key must be a string");
                }
                copy.put((String) entry.getKey(), deepCopy(entry.getValue()));
            }
            return copy;
        }
        if (value instanceof List<?>) {
            List<Object> copy = new ArrayList<Object>();
            for (Object item : (List<?>) value) {
                copy.add(deepCopy(item));
            }
            return copy;
        }
        throw failure("unsupported JSON value type: " + value.getClass().getName());
    }

    private static void appendValue(StringBuilder builder, Object value) throws CanonicalJsonException {
        if (value == null) {
            builder.append("null");
            return;
        }
        if (value instanceof String) {
            appendString(builder, (String) value);
            return;
        }
        if (value instanceof Boolean) {
            builder.append(((Boolean) value).booleanValue() ? "true" : "false");
            return;
        }
        if (value instanceof Number) {
            builder.append(integerText((Number) value));
            return;
        }
        if (value instanceof Map<?, ?>) {
            appendObject(builder, (Map<?, ?>) value);
            return;
        }
        if (value instanceof List<?>) {
            appendArray(builder, (List<?>) value);
            return;
        }
        throw failure("unsupported JSON value type: " + value.getClass().getName());
    }

    /**
     * Returns a deep copy whose v1 semantic collections use the contract's
     * domain ordering. This is intentionally shared by digest and comparison
     * assertions: a whole-object JSON-text sort is not the v1 ordering rule.
     */
    static Object normalizeSemanticCollections(Object value) throws CanonicalJsonException {
        return normalizeSemanticCollections(value, null);
    }

    private static Object normalizeSemanticCollections(Object value, String parentKey) throws CanonicalJsonException {
        if (value instanceof Map<?, ?>) {
            LinkedHashMap<String, Object> normalized = new LinkedHashMap<String, Object>();
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
                if (!(entry.getKey() instanceof String)) {
                    throw failure("JSON object key must be a string");
                }
                String key = (String) entry.getKey();
                normalized.put(key, normalizeSemanticCollections(entry.getValue(), key));
            }
            return normalized;
        }
        if (value instanceof List<?>) {
            List<Object> normalized = new ArrayList<Object>();
            for (Object item : (List<?>) value) {
                normalized.add(normalizeSemanticCollections(item, null));
            }
            if (isUnorderedSemanticCollection(parentKey)) {
                List<SemanticCollectionValue> sortable = new ArrayList<SemanticCollectionValue>();
                for (Object item : normalized) {
                    sortable.add(new SemanticCollectionValue(item, semanticCollectionSortKey(parentKey, item)));
                }
                Collections.sort(sortable, new Comparator<SemanticCollectionValue>() {
                    @Override
                    public int compare(SemanticCollectionValue left, SemanticCollectionValue right) {
                        return compareSemanticCollectionKeys(left.sortKey, right.sortKey);
                    }
                });
                normalized.clear();
                for (SemanticCollectionValue item : sortable) {
                    normalized.add(item.value);
                }
            }
            return normalized;
        }
        return deepCopy(value);
    }

    private static boolean isUnorderedSemanticCollection(String key) {
        return "dependencies".equals(key) || "resources".equals(key) || "assignments".equals(key)
                || "calendars".equals(key);
    }

    private static List<String> semanticCollectionSortKey(String collection, Object value)
            throws CanonicalJsonException {
        if ("dependencies".equals(collection)) {
            return Arrays.asList(requiredMemberString(value, collection, "predecessor_uid"),
                    requiredMemberString(value, collection, "successor_uid"),
                    requiredMemberString(value, collection, "type"), requiredMemberString(value, collection, "lag"));
        }
        return Collections.singletonList(requiredMemberString(value, collection, "uid"));
    }

    private static String requiredMemberString(Object value, String collection, String key) throws CanonicalJsonException {
        if (!(value instanceof Map<?, ?>)) {
            throw failure(collection + " member must be an object");
        }
        Object field = ((Map<?, ?>) value).get(key);
        if (!(field instanceof String)) {
            throw failure(collection + " member must have string " + key);
        }
        String text = (String) field;
        validateUnicodeScalars(text);
        return text;
    }

    private static int compareSemanticCollectionKeys(List<String> left, List<String> right) {
        for (int index = 0; index < left.size(); index++) {
            int comparison = compareUnicodeScalars(left.get(index), right.get(index));
            if (comparison != 0) {
                return comparison;
            }
        }
        return 0;
    }

    private static int compareUnicodeScalars(String left, String right) {
        int leftIndex = 0;
        int rightIndex = 0;
        while (leftIndex < left.length() && rightIndex < right.length()) {
            int leftCodePoint = left.codePointAt(leftIndex);
            int rightCodePoint = right.codePointAt(rightIndex);
            if (leftCodePoint != rightCodePoint) {
                return leftCodePoint < rightCodePoint ? -1 : 1;
            }
            leftIndex += Character.charCount(leftCodePoint);
            rightIndex += Character.charCount(rightCodePoint);
        }
        if (leftIndex == left.length() && rightIndex == right.length()) {
            return 0;
        }
        return leftIndex == left.length() ? -1 : 1;
    }

    private static void appendObject(StringBuilder builder, Map<?, ?> object) throws CanonicalJsonException {
        List<String> keys = new ArrayList<String>();
        for (Object rawKey : object.keySet()) {
            if (!(rawKey instanceof String)) {
                throw failure("JSON object key must be a string");
            }
            String key = (String) rawKey;
            validateUnicodeScalars(key);
            keys.add(key);
        }
        Collections.sort(keys, CODE_POINT_ORDER);
        builder.append('{');
        for (int index = 0; index < keys.size(); index++) {
            if (index > 0) {
                builder.append(',');
            }
            String key = keys.get(index);
            appendString(builder, key);
            builder.append(':');
            appendValue(builder, object.get(key));
        }
        builder.append('}');
    }

    private static void appendArray(StringBuilder builder, List<?> values) throws CanonicalJsonException {
        builder.append('[');
        for (int index = 0; index < values.size(); index++) {
            if (index > 0) {
                builder.append(',');
            }
            appendValue(builder, values.get(index));
        }
        builder.append(']');
    }

    private static void appendString(StringBuilder builder, String value) throws CanonicalJsonException {
        validateUnicodeScalars(value);
        builder.append('"');
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (character == '"' || character == '\\') {
                builder.append('\\').append(character);
            } else if (character == '\b') {
                builder.append("\\b");
            } else if (character == '\t') {
                builder.append("\\t");
            } else if (character == '\n') {
                builder.append("\\n");
            } else if (character == '\f') {
                builder.append("\\f");
            } else if (character == '\r') {
                builder.append("\\r");
            } else if (character < 0x20) {
                String hex = Integer.toHexString(character);
                builder.append("\\u");
                for (int padding = hex.length(); padding < 4; padding++) {
                    builder.append('0');
                }
                builder.append(hex);
            } else {
                builder.append(character);
            }
        }
        builder.append('"');
    }

    private static void validateUnicodeScalars(String value) throws CanonicalJsonException {
        if (value == null) {
            throw failure("JSON string must not be null");
        }
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (Character.isHighSurrogate(character)) {
                if (index + 1 >= value.length() || !Character.isLowSurrogate(value.charAt(index + 1))) {
                    throw failure("JSON string contains an unpaired high surrogate");
                }
                index++;
            } else if (Character.isLowSurrogate(character)) {
                throw failure("JSON string contains an unpaired low surrogate");
            }
        }
    }

    private static String integerText(Number value) throws CanonicalJsonException {
        if (value instanceof Byte || value instanceof Short || value instanceof Integer || value instanceof Long
                || value instanceof BigInteger) {
            return String.valueOf(value);
        }
        throw failure("canonical v1 JSON accepts integer Number values only: " + value.getClass().getName());
    }

    private static String sha256Hex(byte[] bytes) throws CanonicalJsonException {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes);
            StringBuilder result = new StringBuilder(digest.length * 2);
            for (byte item : digest) {
                result.append(String.format("%02x", Integer.valueOf(item & 0xff)));
            }
            return result.toString();
        } catch (NoSuchAlgorithmException error) {
            throw failure("SHA-256 is unavailable", error);
        }
    }

    private static final Comparator<String> CODE_POINT_ORDER = new Comparator<String>() {
        @Override
        public int compare(String left, String right) {
            return compareUnicodeScalars(left, right);
        }
    };

    private static final class SemanticCollectionValue {
        final Object value;
        final List<String> sortKey;

        SemanticCollectionValue(Object value, List<String> sortKey) {
            this.value = value;
            this.sortKey = sortKey;
        }
    }

    private static CanonicalJsonException failure(String message) {
        return new CanonicalJsonException(message);
    }

    private static CanonicalJsonException failure(String message, Throwable cause) {
        return new CanonicalJsonException(message, cause);
    }

    static final class CanonicalJsonException extends Exception {
        private static final long serialVersionUID = 1L;

        CanonicalJsonException(String message) {
            super(message);
        }

        CanonicalJsonException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
