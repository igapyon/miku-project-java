/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuproject.conformance;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import jp.igapyon.mikuproject.coreapi.CoreApiAiJsonUtil;

/**
 * Java 8 test-side registry for the four fixed v1 JSON Schema documents.
 *
 * <p>This is deliberately a closed evaluator, not an open-ended JSON Schema
 * implementation. Registration rejects vocabulary not used by the pinned
 * draft 2020-12 schemas, and rejects references outside the four registered
 * schema identifiers. That makes a future schema expansion an explicit
 * review item instead of silently weakening Java conformance tests.</p>
 */
public final class ConformanceSchemaRegistry {
    private static final String DRAFT_2020_12 = "https://json-schema.org/draft/2020-12/schema";
    private static final String ARTIFACT_SCHEMA_PATH = "docs/schemas/miku-project-artifacts-v1.schema.json";
    private static final String DIAGNOSTIC_SCHEMA_PATH = "docs/schemas/miku-project-cli-diagnostic-v1.schema.json";
    private static final String RESULT_SCHEMA_PATH = "docs/schemas/miku-project-cli-result-v1.schema.json";
    private static final String RUNTIME_MANIFEST_SCHEMA_PATH = "docs/schemas/miku-project-runtime-manifest-v1.schema.json";
    private static final String ARTIFACT_SCHEMA_ID = "urn:miku-project:schema:artifacts:v1";
    private static final String DIAGNOSTIC_SCHEMA_ID = "urn:miku-project:schema:cli-diagnostic:v1";
    private static final String RESULT_SCHEMA_ID = "urn:miku-project:schema:cli-result:v1";
    private static final String RUNTIME_MANIFEST_SCHEMA_ID = "urn:miku-project:schema:runtime-manifest:v1";
    private static final int MAX_VIOLATIONS = 128;

    private static final Set<String> SUPPORTED_SCHEMA_KEYWORDS = setOf("$schema", "$id", "$defs", "$ref", "title",
            "description", "type", "const", "enum", "pattern", "format", "minLength", "maxLength", "minimum",
            "maximum", "minItems", "maxItems", "uniqueItems", "prefixItems", "items", "contains", "minContains",
            "maxContains", "minProperties", "maxProperties", "required", "properties", "additionalProperties", "allOf",
            "anyOf", "oneOf", "not", "if", "then", "else");

    private final LinkedHashMap<String, SchemaDocument> documentsById;
    private final CoreApiAiJsonUtil jsonUtil;

    private ConformanceSchemaRegistry(LinkedHashMap<String, SchemaDocument> documentsById) {
        this.documentsById = documentsById;
        this.jsonUtil = new CoreApiAiJsonUtil();
    }

    /**
     * Loads the four schemas from an inventory-verified snapshot.
     */
    public static ConformanceSchemaRegistry load(Path snapshotRoot) throws IOException {
        ConformanceSuiteLoader.ConformanceSuite suite = ConformanceSuiteLoader.load(snapshotRoot);
        return loadFromVerifiedSnapshot(suite.snapshotRoot);
    }

    static ConformanceSchemaRegistry loadFromVerifiedSnapshotForTest(Path snapshotRoot) throws IOException {
        return loadFromVerifiedSnapshot(snapshotRoot);
    }

    private static ConformanceSchemaRegistry loadFromVerifiedSnapshot(Path snapshotRoot) throws IOException {
        Path root = requireSnapshotRoot(snapshotRoot);
        LinkedHashMap<String, SchemaDocument> documents = new LinkedHashMap<String, SchemaDocument>();
        register(documents, readSchema(root, ARTIFACT_SCHEMA_PATH), ARTIFACT_SCHEMA_ID, ARTIFACT_SCHEMA_PATH);
        register(documents, readSchema(root, DIAGNOSTIC_SCHEMA_PATH), DIAGNOSTIC_SCHEMA_ID, DIAGNOSTIC_SCHEMA_PATH);
        register(documents, readSchema(root, RESULT_SCHEMA_PATH), RESULT_SCHEMA_ID, RESULT_SCHEMA_PATH);
        register(documents, readSchema(root, RUNTIME_MANIFEST_SCHEMA_PATH), RUNTIME_MANIFEST_SCHEMA_ID,
                RUNTIME_MANIFEST_SCHEMA_PATH);
        ConformanceSchemaRegistry registry = new ConformanceSchemaRegistry(documents);
        for (SchemaDocument document : documents.values()) {
            registry.assertSupportedSchema(document, document.root, "#");
        }
        return registry;
    }

    public Validation validateArtifact(Object instance) {
        return validate(ARTIFACT_SCHEMA_ID, instance);
    }

    public Validation validateDiagnostic(Object instance) {
        return validate(DIAGNOSTIC_SCHEMA_ID, instance);
    }

    public Validation validateResult(Object instance) {
        return validate(RESULT_SCHEMA_ID, instance);
    }

    public Validation validateRuntimeManifest(Object instance) {
        return validate(RUNTIME_MANIFEST_SCHEMA_ID, instance);
    }

    public Validation validate(String schemaId, Object instance) {
        SchemaDocument document = documentsById.get(schemaId);
        if (document == null) {
            return Validation.invalid(Collections.singletonList(new Violation("", "", "schema-id",
                    "No registered schema has ID " + schemaId)));
        }
        List<Violation> violations = new ArrayList<Violation>();
        boolean valid = evaluate(document, document.root, instance, "", "#", violations, new LinkedHashSet<String>());
        return new Validation(valid && violations.isEmpty(), violations);
    }

    public Object parseJson(Path path) throws IOException {
        try {
            return jsonUtil.parseJsonText(new String(Files.readAllBytes(path), StandardCharsets.UTF_8));
        } catch (RuntimeException error) {
            throw failure("conformance-schema.json-invalid", "Unable to parse JSON input: " + path, error);
        }
    }

    public Set<String> schemaIds() {
        return Collections.unmodifiableSet(new LinkedHashSet<String>(documentsById.keySet()));
    }

    private boolean evaluate(SchemaDocument currentDocument, Object rawSchema, Object instance, String instancePointer,
            String schemaPointer, List<Violation> violations, Set<String> referenceStack) {
        if (violations.size() >= MAX_VIOLATIONS) {
            return false;
        }
        if (rawSchema instanceof Boolean) {
            if (((Boolean) rawSchema).booleanValue()) {
                return true;
            }
            addViolation(violations, instancePointer, schemaPointer, "false-schema", "Boolean false schema rejects the value");
            return false;
        }
        if (!(rawSchema instanceof Map<?, ?>)) {
            addViolation(violations, instancePointer, schemaPointer, "schema-shape", "Schema node must be an object or boolean");
            return false;
        }
        Map<String, Object> schema = castObject(rawSchema);
        boolean valid = true;

        if (schema.containsKey("$ref")) {
            String reference = schema.get("$ref") instanceof String ? (String) schema.get("$ref") : null;
            if (reference == null) {
                addViolation(violations, instancePointer, schemaPointer, "$ref", "$ref must be a string");
                valid = false;
            } else {
                ResolvedReference resolved = resolveReference(currentDocument, reference);
                if (resolved == null) {
                    addViolation(violations, instancePointer, schemaPointer, "$ref", "Unresolved schema reference: " + reference);
                    valid = false;
                } else {
                    String referenceKey = resolved.document.id + resolved.pointer;
                    if (!referenceStack.add(referenceKey)) {
                        addViolation(violations, instancePointer, schemaPointer, "$ref", "Schema reference cycle: " + reference);
                        valid = false;
                    } else {
                        boolean referenceValid = evaluate(resolved.document, resolved.schema, instance, instancePointer,
                                resolved.pointer, violations, referenceStack);
                        referenceStack.remove(referenceKey);
                        valid = referenceValid && valid;
                    }
                }
            }
        }

        if (schema.containsKey("type") && !matchesType(schema.get("type"), instance)) {
            addViolation(violations, instancePointer, schemaPointer, "type", "Value does not match required type");
            valid = false;
        }
        if (schema.containsKey("const") && !jsonEquals(schema.get("const"), instance)) {
            addViolation(violations, instancePointer, schemaPointer, "const", "Value does not match const");
            valid = false;
        }
        if (schema.containsKey("enum") && !matchesEnum(schema.get("enum"), instance)) {
            addViolation(violations, instancePointer, schemaPointer, "enum", "Value is not one of the allowed values");
            valid = false;
        }
        if (instance instanceof String) {
            valid = evaluateString(schema, (String) instance, instancePointer, schemaPointer, violations) && valid;
        }
        if (instance instanceof Number) {
            valid = evaluateNumber(schema, (Number) instance, instancePointer, schemaPointer, violations) && valid;
        }
        if (instance instanceof List<?>) {
            valid = evaluateArray(currentDocument, schema, (List<?>) instance, instancePointer, schemaPointer, violations,
                    referenceStack) && valid;
        }
        if (instance instanceof Map<?, ?>) {
            valid = evaluateObject(currentDocument, schema, castObject(instance), instancePointer, schemaPointer, violations,
                    referenceStack) && valid;
        }
        valid = evaluateCombinators(currentDocument, schema, instance, instancePointer, schemaPointer, violations,
                referenceStack) && valid;
        return valid;
    }

    private boolean evaluateString(Map<String, Object> schema, String value, String instancePointer, String schemaPointer,
            List<Violation> violations) {
        boolean valid = true;
        if (schema.containsKey("minLength") && codePointLength(value) < integerKeyword(schema.get("minLength"))) {
            addViolation(violations, instancePointer, schemaPointer, "minLength", "String is shorter than minLength");
            valid = false;
        }
        if (schema.containsKey("maxLength") && codePointLength(value) > integerKeyword(schema.get("maxLength"))) {
            addViolation(violations, instancePointer, schemaPointer, "maxLength", "String is longer than maxLength");
            valid = false;
        }
        if (schema.containsKey("pattern")) {
            String pattern = schema.get("pattern") instanceof String ? (String) schema.get("pattern") : null;
            try {
                if (pattern == null || !Pattern.compile(pattern).matcher(value).find()) {
                    addViolation(violations, instancePointer, schemaPointer, "pattern", "String does not match pattern");
                    valid = false;
                }
            } catch (PatternSyntaxException error) {
                addViolation(violations, instancePointer, schemaPointer, "pattern", "Schema has an invalid regular expression");
                valid = false;
            }
        }
        if (schema.containsKey("format")) {
            String format = schema.get("format") instanceof String ? (String) schema.get("format") : null;
            if ("uri".equals(format) && !isAbsoluteUri(value)) {
                addViolation(violations, instancePointer, schemaPointer, "format", "String is not an absolute URI");
                valid = false;
            }
        }
        return valid;
    }

    private boolean evaluateNumber(Map<String, Object> schema, Number value, String instancePointer, String schemaPointer,
            List<Violation> violations) {
        boolean valid = true;
        BigDecimal number = decimal(value);
        if (schema.containsKey("minimum") && number.compareTo(decimal(schema.get("minimum"))) < 0) {
            addViolation(violations, instancePointer, schemaPointer, "minimum", "Number is below minimum");
            valid = false;
        }
        if (schema.containsKey("maximum") && number.compareTo(decimal(schema.get("maximum"))) > 0) {
            addViolation(violations, instancePointer, schemaPointer, "maximum", "Number is above maximum");
            valid = false;
        }
        return valid;
    }

    private boolean evaluateArray(SchemaDocument currentDocument, Map<String, Object> schema, List<?> values,
            String instancePointer, String schemaPointer, List<Violation> violations, Set<String> referenceStack) {
        boolean valid = true;
        if (schema.containsKey("minItems") && values.size() < integerKeyword(schema.get("minItems"))) {
            addViolation(violations, instancePointer, schemaPointer, "minItems", "Array has too few items");
            valid = false;
        }
        if (schema.containsKey("maxItems") && values.size() > integerKeyword(schema.get("maxItems"))) {
            addViolation(violations, instancePointer, schemaPointer, "maxItems", "Array has too many items");
            valid = false;
        }
        if (Boolean.TRUE.equals(schema.get("uniqueItems")) && !hasUniqueItems(values)) {
            addViolation(violations, instancePointer, schemaPointer, "uniqueItems", "Array has duplicate items");
            valid = false;
        }
        int prefixLength = 0;
        if (schema.containsKey("prefixItems")) {
            List<?> prefixItems = schema.get("prefixItems") instanceof List<?> ? (List<?>) schema.get("prefixItems")
                    : Collections.emptyList();
            prefixLength = prefixItems.size();
            for (int index = 0; index < values.size() && index < prefixItems.size(); index++) {
                boolean itemValid = evaluate(currentDocument, prefixItems.get(index), values.get(index),
                        appendPointer(instancePointer, String.valueOf(index)), appendPointer(schemaPointer, "prefixItems/" + index),
                        violations, referenceStack);
                valid = itemValid && valid;
            }
        }
        if (schema.containsKey("items")) {
            Object itemSchema = schema.get("items");
            for (int index = prefixLength; index < values.size(); index++) {
                boolean itemValid = evaluate(currentDocument, itemSchema, values.get(index),
                        appendPointer(instancePointer, String.valueOf(index)), appendPointer(schemaPointer, "items"),
                        violations, referenceStack);
                valid = itemValid && valid;
            }
        }
        if (schema.containsKey("contains")) {
            int matching = 0;
            for (int index = 0; index < values.size(); index++) {
                if (matchesSilently(currentDocument, schema.get("contains"), values.get(index), referenceStack)) {
                    matching++;
                }
            }
            int minimum = schema.containsKey("minContains") ? integerKeyword(schema.get("minContains")) : 1;
            int maximum = schema.containsKey("maxContains") ? integerKeyword(schema.get("maxContains")) : Integer.MAX_VALUE;
            if (matching < minimum || matching > maximum) {
                addViolation(violations, instancePointer, schemaPointer, "contains", "Array contains an invalid number of matching items");
                valid = false;
            }
        }
        return valid;
    }

    private boolean evaluateObject(SchemaDocument currentDocument, Map<String, Object> schema, Map<String, Object> value,
            String instancePointer, String schemaPointer, List<Violation> violations, Set<String> referenceStack) {
        boolean valid = true;
        if (schema.containsKey("minProperties") && value.size() < integerKeyword(schema.get("minProperties"))) {
            addViolation(violations, instancePointer, schemaPointer, "minProperties", "Object has too few properties");
            valid = false;
        }
        if (schema.containsKey("maxProperties") && value.size() > integerKeyword(schema.get("maxProperties"))) {
            addViolation(violations, instancePointer, schemaPointer, "maxProperties", "Object has too many properties");
            valid = false;
        }
        Set<String> declaredProperties = Collections.emptySet();
        if (schema.containsKey("properties")) {
            Map<String, Object> properties = castObject(schema.get("properties"));
            declaredProperties = properties.keySet();
            for (Map.Entry<String, Object> entry : properties.entrySet()) {
                if (value.containsKey(entry.getKey())) {
                    boolean propertyValid = evaluate(currentDocument, entry.getValue(), value.get(entry.getKey()),
                            appendPointer(instancePointer, entry.getKey()), appendPointer(schemaPointer, "properties/" + entry.getKey()),
                            violations, referenceStack);
                    valid = propertyValid && valid;
                }
            }
        }
        if (schema.containsKey("required")) {
            List<?> required = schema.get("required") instanceof List<?> ? (List<?>) schema.get("required") : Collections.emptyList();
            for (Object property : required) {
                if (!(property instanceof String) || !value.containsKey((String) property)) {
                    addViolation(violations, instancePointer, schemaPointer, "required", "Object is missing a required property");
                    valid = false;
                }
            }
        }
        if (schema.containsKey("additionalProperties")) {
            Object additionalProperties = schema.get("additionalProperties");
            for (Map.Entry<String, Object> entry : value.entrySet()) {
                if (declaredProperties.contains(entry.getKey())) {
                    continue;
                }
                if (Boolean.FALSE.equals(additionalProperties)) {
                    addViolation(violations, appendPointer(instancePointer, entry.getKey()), schemaPointer, "additionalProperties",
                            "Object contains an undeclared property");
                    valid = false;
                } else if (additionalProperties instanceof Map<?, ?> || additionalProperties instanceof Boolean) {
                    boolean propertyValid = evaluate(currentDocument, additionalProperties, entry.getValue(),
                            appendPointer(instancePointer, entry.getKey()), appendPointer(schemaPointer, "additionalProperties"),
                            violations, referenceStack);
                    valid = propertyValid && valid;
                }
            }
        }
        return valid;
    }

    private boolean evaluateCombinators(SchemaDocument currentDocument, Map<String, Object> schema, Object instance,
            String instancePointer, String schemaPointer, List<Violation> violations, Set<String> referenceStack) {
        boolean valid = true;
        if (schema.containsKey("allOf")) {
            List<?> schemas = schema.get("allOf") instanceof List<?> ? (List<?>) schema.get("allOf") : Collections.emptyList();
            for (int index = 0; index < schemas.size(); index++) {
                boolean memberValid = evaluate(currentDocument, schemas.get(index), instance, instancePointer,
                        appendPointer(schemaPointer, "allOf/" + index), violations, referenceStack);
                valid = memberValid && valid;
            }
        }
        if (schema.containsKey("anyOf")) {
            List<?> schemas = schema.get("anyOf") instanceof List<?> ? (List<?>) schema.get("anyOf") : Collections.emptyList();
            boolean matches = false;
            for (Object candidate : schemas) {
                if (matchesSilently(currentDocument, candidate, instance, referenceStack)) {
                    matches = true;
                    break;
                }
            }
            if (!matches) {
                addViolation(violations, instancePointer, schemaPointer, "anyOf", "Value does not match any allowed branch");
                valid = false;
            }
        }
        if (schema.containsKey("oneOf")) {
            List<?> schemas = schema.get("oneOf") instanceof List<?> ? (List<?>) schema.get("oneOf") : Collections.emptyList();
            int matches = 0;
            for (Object candidate : schemas) {
                if (matchesSilently(currentDocument, candidate, instance, referenceStack)) {
                    matches++;
                }
            }
            if (matches != 1) {
                addViolation(violations, instancePointer, schemaPointer, "oneOf", "Value must match exactly one branch");
                valid = false;
            }
        }
        if (schema.containsKey("not") && matchesSilently(currentDocument, schema.get("not"), instance, referenceStack)) {
            addViolation(violations, instancePointer, schemaPointer, "not", "Value matches a prohibited schema");
            valid = false;
        }
        if (schema.containsKey("if")) {
            boolean ifMatches = matchesSilently(currentDocument, schema.get("if"), instance, referenceStack);
            String branch = ifMatches ? "then" : "else";
            if (schema.containsKey(branch)) {
                boolean branchValid = evaluate(currentDocument, schema.get(branch), instance, instancePointer,
                        appendPointer(schemaPointer, branch), violations, referenceStack);
                valid = branchValid && valid;
            }
        }
        return valid;
    }

    private boolean matchesSilently(SchemaDocument currentDocument, Object schema, Object instance,
            Set<String> referenceStack) {
        return evaluate(currentDocument, schema, instance, "", "#", new ArrayList<Violation>(),
                new LinkedHashSet<String>(referenceStack));
    }

    private ResolvedReference resolveReference(SchemaDocument currentDocument, String reference) {
        String documentId = currentDocument.id;
        String fragment = "";
        int hashIndex = reference.indexOf('#');
        if (hashIndex >= 0) {
            if (hashIndex > 0) {
                documentId = reference.substring(0, hashIndex);
            }
            fragment = reference.substring(hashIndex);
        } else if (documentsById.containsKey(reference)) {
            documentId = reference;
        } else {
            return null;
        }
        SchemaDocument document = documentsById.get(documentId);
        if (document == null) {
            return null;
        }
        if (fragment.length() == 0 || "#".equals(fragment)) {
            return new ResolvedReference(document, document.root, "#");
        }
        if (!fragment.startsWith("#/")) {
            return null;
        }
        Object node = document.root;
        String[] segments = fragment.substring(2).split("/", -1);
        for (String rawSegment : segments) {
            if (!(node instanceof Map<?, ?>)) {
                return null;
            }
            String segment = decodePointerSegment(rawSegment);
            Map<String, Object> map = castObject(node);
            if (!map.containsKey(segment)) {
                return null;
            }
            node = map.get(segment);
        }
        return new ResolvedReference(document, node, fragment);
    }

    private void assertSupportedSchema(SchemaDocument currentDocument, Object rawSchema, String schemaPointer)
            throws IOException {
        if (rawSchema instanceof Boolean) {
            return;
        }
        if (!(rawSchema instanceof Map<?, ?>)) {
            throw failure("conformance-schema.schema-invalid", "Schema node must be an object or boolean: " + schemaPointer);
        }
        Map<String, Object> schema = castObject(rawSchema);
        for (String keyword : schema.keySet()) {
            if (!SUPPORTED_SCHEMA_KEYWORDS.contains(keyword)) {
                throw failure("conformance-schema.unsupported-keyword",
                        "Unsupported JSON Schema keyword in " + currentDocument.id + ": " + keyword);
            }
        }
        if (schema.containsKey("$ref")) {
            if (!(schema.get("$ref") instanceof String) || resolveReference(currentDocument, (String) schema.get("$ref")) == null) {
                throw failure("conformance-schema.reference-invalid", "Unresolved schema reference at " + schemaPointer);
            }
        }
        assertSchemaMap(schema.get("$defs"), currentDocument, appendPointer(schemaPointer, "$defs"));
        assertSchemaMap(schema.get("properties"), currentDocument, appendPointer(schemaPointer, "properties"));
        assertSchemaValue(schema.get("additionalProperties"), currentDocument, appendPointer(schemaPointer, "additionalProperties"));
        assertSchemaValue(schema.get("items"), currentDocument, appendPointer(schemaPointer, "items"));
        assertSchemaList(schema.get("prefixItems"), currentDocument, appendPointer(schemaPointer, "prefixItems"));
        assertSchemaList(schema.get("allOf"), currentDocument, appendPointer(schemaPointer, "allOf"));
        assertSchemaList(schema.get("anyOf"), currentDocument, appendPointer(schemaPointer, "anyOf"));
        assertSchemaList(schema.get("oneOf"), currentDocument, appendPointer(schemaPointer, "oneOf"));
        assertSchemaValue(schema.get("not"), currentDocument, appendPointer(schemaPointer, "not"));
        assertSchemaValue(schema.get("if"), currentDocument, appendPointer(schemaPointer, "if"));
        assertSchemaValue(schema.get("then"), currentDocument, appendPointer(schemaPointer, "then"));
        assertSchemaValue(schema.get("else"), currentDocument, appendPointer(schemaPointer, "else"));
        assertSchemaValue(schema.get("contains"), currentDocument, appendPointer(schemaPointer, "contains"));
    }

    private void assertSchemaMap(Object raw, SchemaDocument document, String pointer) throws IOException {
        if (raw == null) {
            return;
        }
        if (!(raw instanceof Map<?, ?>)) {
            throw failure("conformance-schema.schema-invalid", "Schema map must be an object: " + pointer);
        }
        for (Map.Entry<String, Object> entry : castObject(raw).entrySet()) {
            assertSupportedSchema(document, entry.getValue(), appendPointer(pointer, entry.getKey()));
        }
    }

    private void assertSchemaList(Object raw, SchemaDocument document, String pointer) throws IOException {
        if (raw == null) {
            return;
        }
        if (!(raw instanceof List<?>)) {
            throw failure("conformance-schema.schema-invalid", "Schema list must be an array: " + pointer);
        }
        List<?> list = (List<?>) raw;
        for (int index = 0; index < list.size(); index++) {
            assertSupportedSchema(document, list.get(index), appendPointer(pointer, String.valueOf(index)));
        }
    }

    private void assertSchemaValue(Object raw, SchemaDocument document, String pointer) throws IOException {
        if (raw == null) {
            return;
        }
        assertSupportedSchema(document, raw, pointer);
    }

    private static void register(Map<String, SchemaDocument> documents, SchemaDocument document, String expectedId,
            String expectedPath) throws IOException {
        Object schema = document.root.get("$schema");
        Object id = document.root.get("$id");
        if (!DRAFT_2020_12.equals(schema) || !expectedId.equals(id)) {
            throw failure("conformance-schema.identity-mismatch", "Schema identity does not match " + expectedPath);
        }
        SchemaDocument registered = new SchemaDocument(expectedId, document.path, document.root);
        if (documents.put(expectedId, registered) != null) {
            throw failure("conformance-schema.duplicate-id", "Duplicate schema ID: " + expectedId);
        }
    }

    private static SchemaDocument readSchema(Path root, String relativePath) throws IOException {
        Path file = root.resolve(relativePath).normalize();
        if (!file.startsWith(root)) {
            throw failure("conformance-schema.path-escape", "Schema path escapes the snapshot root: " + relativePath);
        }
        requireRegularNonSymlinkFile(root, file, relativePath);
        Object parsed;
        try {
            parsed = new CoreApiAiJsonUtil().parseJsonText(new String(Files.readAllBytes(file), StandardCharsets.UTF_8));
        } catch (RuntimeException error) {
            throw failure("conformance-schema.json-invalid", "Schema is not valid JSON: " + relativePath, error);
        }
        if (!(parsed instanceof Map<?, ?>)) {
            throw failure("conformance-schema.schema-invalid", "Schema root must be an object: " + relativePath);
        }
        return new SchemaDocument(null, file, castObject(parsed));
    }

    private static Path requireSnapshotRoot(Path snapshotRoot) throws IOException {
        if (snapshotRoot == null) {
            throw failure("conformance-schema.snapshot-invalid", "Snapshot root must not be null");
        }
        Path root = snapshotRoot.toAbsolutePath().normalize();
        if (Files.isSymbolicLink(root) || !Files.isDirectory(root, LinkOption.NOFOLLOW_LINKS)) {
            throw failure("conformance-schema.snapshot-invalid", "Snapshot root must be a non-symlink directory");
        }
        return root;
    }

    private static void requireRegularNonSymlinkFile(Path root, Path file, String label) throws IOException {
        Path current = root;
        for (Path segment : root.relativize(file)) {
            current = current.resolve(segment);
            if (Files.isSymbolicLink(current)) {
                throw failure("conformance-schema.symlink", "Schema path traverses a symlink: " + label);
            }
        }
        if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) {
            throw failure("conformance-schema.file-invalid", "Schema must be a regular file: " + label);
        }
    }

    private static boolean matchesType(Object rawType, Object value) {
        if (rawType instanceof List<?>) {
            for (Object type : (List<?>) rawType) {
                if (matchesType(type, value)) {
                    return true;
                }
            }
            return false;
        }
        if (!(rawType instanceof String)) {
            return false;
        }
        String type = (String) rawType;
        if ("object".equals(type)) {
            return value instanceof Map<?, ?>;
        }
        if ("array".equals(type)) {
            return value instanceof List<?>;
        }
        if ("string".equals(type)) {
            return value instanceof String;
        }
        if ("number".equals(type)) {
            return value instanceof Number;
        }
        if ("integer".equals(type)) {
            return value instanceof Number && isInteger((Number) value);
        }
        if ("boolean".equals(type)) {
            return value instanceof Boolean;
        }
        if ("null".equals(type)) {
            return value == null;
        }
        return false;
    }

    private static boolean matchesEnum(Object rawEnum, Object value) {
        if (!(rawEnum instanceof List<?>)) {
            return false;
        }
        for (Object candidate : (List<?>) rawEnum) {
            if (jsonEquals(candidate, value)) {
                return true;
            }
        }
        return false;
    }

    private static boolean jsonEquals(Object left, Object right) {
        if (left instanceof Number && right instanceof Number) {
            return decimal(left).compareTo(decimal(right)) == 0;
        }
        if (left instanceof Map<?, ?> && right instanceof Map<?, ?>) {
            Map<String, Object> leftMap = castObject(left);
            Map<String, Object> rightMap = castObject(right);
            if (!leftMap.keySet().equals(rightMap.keySet())) {
                return false;
            }
            for (String key : leftMap.keySet()) {
                if (!jsonEquals(leftMap.get(key), rightMap.get(key))) {
                    return false;
                }
            }
            return true;
        }
        if (left instanceof List<?> && right instanceof List<?>) {
            List<?> leftList = (List<?>) left;
            List<?> rightList = (List<?>) right;
            if (leftList.size() != rightList.size()) {
                return false;
            }
            for (int index = 0; index < leftList.size(); index++) {
                if (!jsonEquals(leftList.get(index), rightList.get(index))) {
                    return false;
                }
            }
            return true;
        }
        return left == null ? right == null : left.equals(right);
    }

    private static boolean hasUniqueItems(List<?> values) {
        for (int left = 0; left < values.size(); left++) {
            for (int right = left + 1; right < values.size(); right++) {
                if (jsonEquals(values.get(left), values.get(right))) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean isInteger(Number value) {
        try {
            return decimal(value).stripTrailingZeros().scale() <= 0;
        } catch (NumberFormatException error) {
            return false;
        }
    }

    private static BigDecimal decimal(Object value) {
        if (!(value instanceof Number)) {
            throw new IllegalArgumentException("Expected JSON number");
        }
        return new BigDecimal(String.valueOf(value));
    }

    private static int integerKeyword(Object value) {
        if (!(value instanceof Number)) {
            return Integer.MAX_VALUE;
        }
        long result = ((Number) value).longValue();
        return result > Integer.MAX_VALUE ? Integer.MAX_VALUE : (result < Integer.MIN_VALUE ? Integer.MIN_VALUE : (int) result);
    }

    private static int codePointLength(String value) {
        return value.codePointCount(0, value.length());
    }

    private static boolean isAbsoluteUri(String value) {
        try {
            return new URI(value).isAbsolute();
        } catch (URISyntaxException error) {
            return false;
        }
    }

    private static void addViolation(List<Violation> violations, String instancePointer, String schemaPointer,
            String keyword, String message) {
        if (violations.size() < MAX_VIOLATIONS) {
            violations.add(new Violation(instancePointer, schemaPointer, keyword, message));
        }
    }

    private static String appendPointer(String base, String segment) {
        String encoded = segment.replace("~", "~0").replace("/", "~1");
        return base.length() == 0 ? "/" + encoded : base + "/" + encoded;
    }

    private static String decodePointerSegment(String segment) {
        StringBuilder result = new StringBuilder();
        for (int index = 0; index < segment.length(); index++) {
            char character = segment.charAt(index);
            if (character == '~' && index + 1 < segment.length()) {
                char escaped = segment.charAt(index + 1);
                if (escaped == '0') {
                    result.append('~');
                    index++;
                    continue;
                }
                if (escaped == '1') {
                    result.append('/');
                    index++;
                    continue;
                }
            }
            result.append(character);
        }
        return result.toString();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> castObject(Object value) {
        return (Map<String, Object>) value;
    }

    private static Set<String> setOf(String... values) {
        return Collections.unmodifiableSet(new LinkedHashSet<String>(Arrays.asList(values)));
    }

    private static ConformanceSchemaException failure(String code, String message) {
        return new ConformanceSchemaException(code, message);
    }

    private static ConformanceSchemaException failure(String code, String message, Throwable cause) {
        return new ConformanceSchemaException(code, message, cause);
    }

    public static final class Validation {
        public final boolean valid;
        public final List<Violation> violations;

        private Validation(boolean valid, List<Violation> violations) {
            this.valid = valid;
            this.violations = Collections.unmodifiableList(new ArrayList<Violation>(violations));
        }

        private static Validation invalid(List<Violation> violations) {
            return new Validation(false, violations);
        }
    }

    public static final class Violation {
        public final String instancePointer;
        public final String schemaPointer;
        public final String keyword;
        public final String message;

        private Violation(String instancePointer, String schemaPointer, String keyword, String message) {
            this.instancePointer = instancePointer;
            this.schemaPointer = schemaPointer;
            this.keyword = keyword;
            this.message = message;
        }
    }

    public static final class ConformanceSchemaException extends IOException {
        private static final long serialVersionUID = 1L;
        public final String code;

        private ConformanceSchemaException(String code, String message) {
            super(message);
            this.code = code;
        }

        private ConformanceSchemaException(String code, String message, Throwable cause) {
            super(message, cause);
            this.code = code;
        }
    }

    private static final class SchemaDocument {
        private final String id;
        private final Path path;
        private final Map<String, Object> root;

        private SchemaDocument(String id, Path path, Map<String, Object> root) {
            this.id = id;
            this.path = path;
            this.root = root;
        }
    }

    private static final class ResolvedReference {
        private final SchemaDocument document;
        private final Object schema;
        private final String pointer;

        private ResolvedReference(SchemaDocument document, Object schema, String pointer) {
            this.document = document;
            this.schema = schema;
            this.pointer = pointer;
        }
    }
}
