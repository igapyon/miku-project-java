/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuproject.conformance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

public class ConformanceBindingValidatorTest {
    private static final Path SNAPSHOT = Paths.get("vendor", "miku-project-contract", "v1.0.3");

    @Test
    public void canonicalJsonProducesThePinnedV1ArtifactDigests() throws IOException, ConformanceCanonicalJson.CanonicalJsonException {
        ConformanceSchemaRegistry registry = ConformanceSchemaRegistry.load(SNAPSHOT);

        Object state = registry.parseJson(SNAPSHOT.resolve("testdata/conformance/v1/golden/semantic/dependency.state.json"));
        Object request = registry.parseJson(SNAPSHOT.resolve("docs/examples/artifacts-v1/change-request.example.json"));
        Map<String, Object> planResult = object(registry.parseJson(
                SNAPSHOT.resolve("docs/examples/cli-v1/plan-change-succeeded.result.json")));
        Map<String, Object> data = object(planResult.get("data"));

        assertEquals("a98f0c8b560382234572a61f360c9e96911bc75fc1d57b79968d6e60b5d751d0",
                ConformanceCanonicalJson.sha256SemanticStateHex(state));
        assertEquals("25bb990ec7530995c7fdcb201c57eed083c85fe6e99aa77bc1f13814ca693c60",
                ConformanceCanonicalJson.sha256Hex(request));
        assertEquals("46b35347fbf493430bf9dd285b788bab6950cff431eb96b01a1725aaf9718db3",
                ConformanceCanonicalJson.sha256Hex(data.get("semantic_diff")));
        assertEquals("33f6a857bb88afe72d96458dd9c152c1f856ddced7c885534b768978462a05f7",
                ConformanceCanonicalJson.sha256Hex(data.get("output_plan")));
    }

    @Test
    public void canonicalJsonSortsKeysByUnicodeCodePointAndRejectsAmbiguousValues()
            throws ConformanceCanonicalJson.CanonicalJsonException {
        LinkedHashMap<String, Object> value = new LinkedHashMap<String, Object>();
        value.put("\uD83D\uDE00", Integer.valueOf(3));
        value.put("\uE000", Integer.valueOf(2));
        value.put("a", Integer.valueOf(1));

        assertEquals("{\"a\":1,\"" + "\uE000" + "\":2,\"😀\":3}",
                ConformanceCanonicalJson.serialize(value));
        assertThrows(ConformanceCanonicalJson.CanonicalJsonException.class,
                () -> ConformanceCanonicalJson.serialize("\uD800"));
        assertThrows(ConformanceCanonicalJson.CanonicalJsonException.class,
                () -> ConformanceCanonicalJson.serialize(Double.valueOf(1.0)));
    }

    @Test
    public void semanticStateDigestCanonicalizesOnlyNonTaskCollections()
            throws ConformanceCanonicalJson.CanonicalJsonException {
        Map<String, Object> left = semanticStateWithCollections("first", "second");
        Map<String, Object> reorderedCollections = object(ConformanceCanonicalJson.deepCopy(left));
        java.util.Collections.reverse(array(reorderedCollections.get("dependencies")));
        java.util.Collections.reverse(array(reorderedCollections.get("resources")));
        java.util.Collections.reverse(array(reorderedCollections.get("assignments")));
        java.util.Collections.reverse(array(reorderedCollections.get("calendars")));
        assertEquals(ConformanceCanonicalJson.sha256SemanticStateHex(left),
                ConformanceCanonicalJson.sha256SemanticStateHex(reorderedCollections));

        Map<String, Object> reorderedTasks = object(ConformanceCanonicalJson.deepCopy(left));
        java.util.Collections.reverse(array(reorderedTasks.get("tasks")));
        assertFalse(ConformanceCanonicalJson.sha256SemanticStateHex(left)
                .equals(ConformanceCanonicalJson.sha256SemanticStateHex(reorderedTasks)));
    }

    @Test
    public void semanticStateCanonicalizationUsesContractDomainKeysAndPinnedNodeDigest()
            throws ConformanceCanonicalJson.CanonicalJsonException {
        Map<String, Object> state = semanticStateWithOrderingEdgeCases();
        Map<String, Object> canonical = object(ConformanceCanonicalJson.normalizeSemanticCollections(state));

        assertEquals(Arrays.asList("2", "1"), stringField(array(canonical.get("tasks")), "uid"));
        assertEquals(Arrays.asList("\uE000", "😀"),
                stringField(array(canonical.get("dependencies")), "predecessor_uid"));
        assertEquals(Arrays.asList("1", "2"), stringField(array(canonical.get("resources")), "uid"));
        assertEquals(Arrays.asList("1", "2"), stringField(array(canonical.get("assignments")), "uid"));
        assertEquals(Arrays.asList("1", "2"), stringField(array(canonical.get("calendars")), "uid"));
        // Generated by scripts/lib/v1/cli-v1-canonical-json.mjs in the pinned
        // Node reference implementation. Optional fields deliberately sort in
        // the opposite order from these domain keys.
        assertEquals("4becd0421edd89c3d2e145147316914fc7031b511fe55a8246540b43fe7770c1",
                ConformanceCanonicalJson.sha256SemanticStateHex(state));
    }

    @Test
    public void rejectsSchemaValidTaskContextForSummaryAndNonLeafTasks() throws IOException,
            ConformanceCanonicalJson.CanonicalJsonException {
        ConformanceSchemaRegistry registry = ConformanceSchemaRegistry.load(SNAPSHOT);
        ConformanceBindingValidator validator = ConformanceBindingValidator.load(SNAPSHOT);
        Map<String, Object> hierarchy = object(ConformanceCanonicalJson.deepCopy(registry.parseJson(
                SNAPSHOT.resolve("testdata/conformance/v1/golden/semantic/hierarchy.state.json"))));

        assertProjectionLeafViolation(registry, validator, hierarchy);

        Map<String, Object> nonSummaryParent = object(ConformanceCanonicalJson.deepCopy(hierarchy));
        object(array(nonSummaryParent.get("tasks")).get(0)).put("summary", Boolean.FALSE);
        assertProjectionLeafViolation(registry, validator, nonSummaryParent);
    }

    @Test
    public void evaluatesEveryCheckedInCrossArtifactBindingCase() throws IOException {
        ConformanceSchemaRegistry registry = ConformanceSchemaRegistry.load(SNAPSHOT);
        ConformanceBindingValidator validator = ConformanceBindingValidator.load(SNAPSHOT);
        ConformanceSuiteLoader.ConformanceSuite suite = ConformanceSuiteLoader.load(SNAPSHOT);
        int executed = 0;
        for (ConformanceSuiteLoader.ContractCase contractCase : suite.contractCases) {
            if (!"cross-artifact-binding".equals(contractCase.validationLayer)) {
                continue;
            }
            Map<String, Object> inputs = ConformanceCaseMaterializer.materialize(contractCase, registry);
            ConformanceBindingValidator.BindingValidation validation = validator.validate(inputs);
            assertEquals(contractCase.expectedValid, validation.valid, contractCase.id + " prerequisites="
                    + validation.prerequisiteFailures + " violations=" + validation.violatedRuleIds);
            assertEquals(contractCase.expectedRuleIds, validation.violatedRuleIds, contractCase.id);
            if (contractCase.expectedValid) {
                assertEquals(contractCase.checkedRuleIds, validation.evaluatedRuleIds, contractCase.id);
            }
            executed++;
        }
        assertEquals(13, executed);
    }

    @Test
    public void refusesToEvaluateBindingsBeforeTheSchemaPrerequisite() throws IOException,
            ConformanceCanonicalJson.CanonicalJsonException {
        ConformanceSchemaRegistry registry = ConformanceSchemaRegistry.load(SNAPSHOT);
        ConformanceBindingValidator validator = ConformanceBindingValidator.load(SNAPSHOT);
        Map<String, Object> result = object(ConformanceCanonicalJson.deepCopy(registry.parseJson(
                SNAPSHOT.resolve("docs/examples/cli-v1/plan-change-succeeded.result.json"))));
        object(result.get("data")).remove("output_plan");

        Map<String, Object> inputs = new LinkedHashMap<String, Object>();
        inputs.put("result", result);
        ConformanceBindingValidator.BindingValidation validation = validator.validate(inputs);

        assertFalse(validation.valid);
        assertEquals(Arrays.asList("schema-invalid:result"), validation.prerequisiteFailures);
        assertTrue(validation.evaluatedRuleIds.isEmpty());
        assertTrue(validation.violatedRuleIds.isEmpty());
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> object(Object value) {
        return (Map<String, Object>) value;
    }

    @SuppressWarnings("unchecked")
    private static List<Object> array(Object value) {
        return (List<Object>) value;
    }

    private static Map<String, Object> semanticStateWithCollections(String first, String second) {
        LinkedHashMap<String, Object> state = new LinkedHashMap<String, Object>();
        state.put("kind", "miku_project_semantic_state");
        state.put("tasks", Arrays.<Object>asList(task("1", first), task("2", second)));
        state.put("dependencies", Arrays.<Object>asList(dependency(first, "1"), dependency(second, "2")));
        state.put("resources", Arrays.<Object>asList(item("uid", first), item("uid", second)));
        state.put("assignments", Arrays.<Object>asList(assignment(first, "1", "1"), assignment(second, "2", "2")));
        state.put("calendars", Arrays.<Object>asList(item("uid", first), item("uid", second)));
        return state;
    }

    private static Map<String, Object> semanticStateWithOrderingEdgeCases() {
        LinkedHashMap<String, Object> state = new LinkedHashMap<String, Object>();
        state.put("kind", "miku_project_semantic_state");
        state.put("project", item("name", "Ordering"));
        state.put("tasks", Arrays.<Object>asList(task("2", "first"), task("1", "second")));
        state.put("dependencies", Arrays.<Object>asList(
                dependency("😀", "x", "FS", "PT0H0M0S", "Alpha"),
                dependency("\uE000", "z", "FS", "PT0H0M0S", "Zulu")));
        state.put("resources", Arrays.<Object>asList(namedUid("2", "Alpha"), namedUid("1", "Zulu")));
        state.put("assignments", Arrays.<Object>asList(assignment("2", "\uE000", "Alpha"),
                assignment("1", "😀", "Zulu")));
        state.put("calendars", Arrays.<Object>asList(namedUid("2", "Alpha"), namedUid("1", "Zulu")));
        return state;
    }

    private static void assertProjectionLeafViolation(ConformanceSchemaRegistry registry,
            ConformanceBindingValidator validator, Map<String, Object> semanticState)
            throws ConformanceCanonicalJson.CanonicalJsonException {
        Map<String, Object> projection = taskChangeContextForFirstTask(semanticState);
        assertTrue(registry.validateArtifact(semanticState).valid);
        assertTrue(registry.validateArtifact(projection).valid);
        Map<String, Object> inputs = new LinkedHashMap<String, Object>();
        inputs.put("projection", projection);
        inputs.put("semantic_state", semanticState);

        ConformanceBindingValidator.BindingValidation validation = validator.validate(inputs);
        assertFalse(validation.valid);
        assertEquals(Arrays.asList("RB-012"), validation.evaluatedRuleIds);
        assertEquals(Arrays.asList("RB-012"), validation.violatedRuleIds);
    }

    private static Map<String, Object> taskChangeContextForFirstTask(Map<String, Object> semanticState)
            throws ConformanceCanonicalJson.CanonicalJsonException {
        Map<String, Object> projection = object(ConformanceCanonicalJson.deepCopy(semanticState));
        projection.clear();
        projection.put("kind", "miku_project_projection");
        projection.put("schema_version", "1");
        projection.put("semantic_contract_version", "1");
        projection.put("purpose", "task_change_context");
        projection.put("source_state_digest", ConformanceCanonicalJson.sha256SemanticStateDigest(semanticState));
        LinkedHashMap<String, Object> scope = new LinkedHashMap<String, Object>();
        scope.put("target_task_uid", "1");
        scope.put("included_domains", Arrays.<Object>asList("project", "target_task", "ancestors", "dependencies",
                "assignments", "resources"));
        scope.put("omitted_domains", Arrays.<Object>asList("other_task_details", "raw_external_artifact", "unsupported_data"));
        projection.put("scope", scope);
        projection.put("project", ConformanceCanonicalJson.deepCopy(semanticState.get("project")));
        projection.put("target_task", ConformanceCanonicalJson.deepCopy(array(semanticState.get("tasks")).get(0)));
        projection.put("ancestors", Arrays.<Object>asList());
        projection.put("dependencies", ConformanceCanonicalJson.deepCopy(semanticState.get("dependencies")));
        projection.put("resources", Arrays.<Object>asList());
        projection.put("assignments", Arrays.<Object>asList());
        projection.put("capability", item("unsupported_data", Arrays.<Object>asList()));
        LinkedHashMap<String, Object> request = new LinkedHashMap<String, Object>();
        request.put("kind", "set_task_percent_complete");
        request.put("required_preconditions", Arrays.<Object>asList("source_state_digest", "expected_percent_complete"));
        projection.put("supported_change_requests", Arrays.<Object>asList(request));
        return projection;
    }

    private static Map<String, Object> task(String uid, String name) {
        LinkedHashMap<String, Object> task = new LinkedHashMap<String, Object>();
        task.put("uid", uid);
        task.put("name", name);
        return task;
    }

    private static Map<String, Object> item(String key, String value) {
        LinkedHashMap<String, Object> item = new LinkedHashMap<String, Object>();
        item.put(key, value);
        return item;
    }

    private static Map<String, Object> item(String key, List<Object> value) {
        LinkedHashMap<String, Object> item = new LinkedHashMap<String, Object>();
        item.put(key, value);
        return item;
    }

    private static Map<String, Object> dependency(String predecessorUid, String successorUid) {
        return dependency(predecessorUid, successorUid, "FS", "PT0H0M0S", null);
    }

    private static Map<String, Object> dependency(String predecessorUid, String successorUid, String type, String lag,
            String note) {
        LinkedHashMap<String, Object> dependency = new LinkedHashMap<String, Object>();
        dependency.put("predecessor_uid", predecessorUid);
        dependency.put("successor_uid", successorUid);
        dependency.put("type", type);
        dependency.put("lag", lag);
        if (note != null) {
            dependency.put("note", note);
        }
        return dependency;
    }

    private static Map<String, Object> assignment(String uid, String taskUid, String resourceUid) {
        LinkedHashMap<String, Object> assignment = new LinkedHashMap<String, Object>();
        assignment.put("uid", uid);
        assignment.put("task_uid", taskUid);
        assignment.put("resource_uid", resourceUid);
        return assignment;
    }

    private static Map<String, Object> namedUid(String uid, String name) {
        LinkedHashMap<String, Object> item = new LinkedHashMap<String, Object>();
        item.put("uid", uid);
        item.put("name", name);
        return item;
    }

    private static List<String> stringField(List<Object> values, String key) {
        java.util.ArrayList<String> result = new java.util.ArrayList<String>();
        for (Object value : values) {
            result.add((String) object(value).get(key));
        }
        return result;
    }
}
