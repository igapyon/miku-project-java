/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuproject.conformance;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Materializes one immutable contract case into independent mutable JSON
 * values. Mutation mechanics live here rather than in individual tests so
 * schema and binding layers exercise exactly the same RFC 6901 input shape.
 */
final class ConformanceCaseMaterializer {
    private ConformanceCaseMaterializer() {
    }

    static Map<String, Object> materialize(ConformanceSuiteLoader.ContractCase contractCase,
            ConformanceSchemaRegistry registry) throws IOException {
        LinkedHashMap<String, Object> documents = new LinkedHashMap<String, Object>();
        for (ConformanceSuiteLoader.ContractInput input : contractCase.inputs) {
            try {
                documents.put(input.role, ConformanceCanonicalJson.deepCopy(registry.parseJson(input.path)));
            } catch (ConformanceCanonicalJson.CanonicalJsonException error) {
                throw failure(contractCase.id + " input " + input.role + " is not canonicalizable", error);
            }
        }
        for (ConformanceSuiteLoader.Mutation mutation : contractCase.mutations) {
            Object document = documents.get(mutation.inputRole);
            if (document == null) {
                throw failure(contractCase.id + " has no materialized role " + mutation.inputRole);
            }
            documents.put(mutation.inputRole, applyMutation(document, mutation, contractCase.id));
        }
        return documents;
    }

    private static Object applyMutation(Object document, ConformanceSuiteLoader.Mutation mutation, String caseId)
            throws IOException {
        try {
            if (mutation.pointer.length() == 0) {
                if (!"replace".equals(mutation.operation)) {
                    throw failure(caseId + " may replace but not " + mutation.operation + " the document root");
                }
                return copyMutationValue(mutation);
            }
            String[] segments = mutation.pointer.substring(1).split("/", -1);
            Object parent = document;
            for (int index = 0; index < segments.length - 1; index++) {
                parent = child(parent, decodePointerSegment(segments[index]), caseId, mutation.pointer);
            }
            String last = decodePointerSegment(segments[segments.length - 1]);
            if (parent instanceof Map<?, ?>) {
                Map<String, Object> object = object(parent, caseId, mutation.pointer);
                if ("remove".equals(mutation.operation)) {
                    if (!object.containsKey(last)) {
                        throw failure(caseId + " cannot remove an absent object member at " + mutation.pointer);
                    }
                    object.remove(last);
                } else if ("replace".equals(mutation.operation)) {
                    if (!object.containsKey(last)) {
                        throw failure(caseId + " cannot replace an absent object member at " + mutation.pointer);
                    }
                    object.put(last, copyMutationValue(mutation));
                } else if ("add".equals(mutation.operation)) {
                    object.put(last, copyMutationValue(mutation));
                } else {
                    throw failure(caseId + " has an unsupported mutation operation: " + mutation.operation);
                }
                return document;
            }
            List<Object> array = array(parent, caseId, mutation.pointer);
            if ("add".equals(mutation.operation) && "-".equals(last)) {
                array.add(copyMutationValue(mutation));
                return document;
            }
            int index = arrayIndex(last, array.size(), "add".equals(mutation.operation), caseId, mutation.pointer);
            if ("add".equals(mutation.operation)) {
                array.add(index, copyMutationValue(mutation));
            } else if ("replace".equals(mutation.operation)) {
                array.set(index, copyMutationValue(mutation));
            } else if ("remove".equals(mutation.operation)) {
                array.remove(index);
            } else {
                throw failure(caseId + " has an unsupported mutation operation: " + mutation.operation);
            }
            return document;
        } catch (ConformanceCanonicalJson.CanonicalJsonException error) {
            throw failure(caseId + " mutation value is not canonicalizable", error);
        }
    }

    private static Object copyMutationValue(ConformanceSuiteLoader.Mutation mutation)
            throws ConformanceCanonicalJson.CanonicalJsonException, IOException {
        if (!mutation.hasValue) {
            throw failure("mutation " + mutation.pointer + " requires a value");
        }
        return ConformanceCanonicalJson.deepCopy(mutation.value);
    }

    private static Object child(Object parent, String segment, String caseId, String pointer) throws IOException {
        if (parent instanceof Map<?, ?>) {
            Map<String, Object> object = object(parent, caseId, pointer);
            if (!object.containsKey(segment)) {
                throw failure(caseId + " references an absent object member at " + pointer);
            }
            return object.get(segment);
        }
        List<Object> array = array(parent, caseId, pointer);
        return array.get(arrayIndex(segment, array.size(), false, caseId, pointer));
    }

    private static int arrayIndex(String value, int size, boolean allowEnd, String caseId, String pointer)
            throws IOException {
        if (value.length() == 0 || (value.length() > 1 && value.charAt(0) == '0')) {
            throw failure(caseId + " has an invalid array index at " + pointer);
        }
        try {
            int index = Integer.parseInt(value);
            int upperBound = allowEnd ? size : size - 1;
            if (index < 0 || index > upperBound) {
                throw failure(caseId + " has an out-of-range array index at " + pointer);
            }
            return index;
        } catch (NumberFormatException error) {
            throw failure(caseId + " has an invalid array index at " + pointer, error);
        }
    }

    private static String decodePointerSegment(String segment) {
        return segment.replace("~1", "/").replace("~0", "~");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> object(Object value, String caseId, String pointer) throws IOException {
        if (!(value instanceof Map<?, ?>)) {
            throw failure(caseId + " expected an object parent at " + pointer);
        }
        return (Map<String, Object>) value;
    }

    @SuppressWarnings("unchecked")
    private static List<Object> array(Object value, String caseId, String pointer) throws IOException {
        if (!(value instanceof List<?>)) {
            throw failure(caseId + " expected an array parent at " + pointer);
        }
        return (List<Object>) value;
    }

    private static IOException failure(String message) {
        return new IOException("conformance-case.materialization: " + message);
    }

    private static IOException failure(String message, Throwable cause) {
        return new IOException("conformance-case.materialization: " + message, cause);
    }
}
