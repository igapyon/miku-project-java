/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuproject.conformance;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Test-side evaluator for the cross-artifact rules exercised by the immutable
 * v1 contract corpus.
 *
 * <p>JSON Schema is a prerequisite, not a substitute. This evaluator first
 * validates every supplied input with {@link ConformanceSchemaRegistry}, then
 * verifies RB-001--RB-006, RB-011, and RB-012 whenever their required
 * artifact roles are present. RB-007--RB-010 require actual command
 * execution, filesystem observations, or diagnostic aggregation and remain
 * the responsibility of the later command runner.</p>
 */
public final class ConformanceBindingValidator {
    private static final Set<String> INPUT_ROLES = setOf("result", "change_request", "approval", "plan_result",
            "projection", "semantic_state");

    private final ConformanceSchemaRegistry registry;

    private ConformanceBindingValidator(ConformanceSchemaRegistry registry) {
        this.registry = registry;
    }

    /**
     * Loads the binding evaluator only through the verified immutable snapshot
     * boundary.
     */
    public static ConformanceBindingValidator load(Path snapshotRoot) throws IOException {
        return new ConformanceBindingValidator(ConformanceSchemaRegistry.load(snapshotRoot));
    }

    /**
     * Validates the supplied role-to-document set. A schema prerequisite
     * failure never falls through to a potentially misleading binding pass.
     */
    public BindingValidation validate(Map<String, Object> inputs) {
        LinkedHashSet<String> prerequisiteFailures = new LinkedHashSet<String>();
        if (inputs == null || inputs.isEmpty()) {
            prerequisiteFailures.add("input-set-empty");
            return new BindingValidation(Collections.<String>emptyList(), Collections.<String>emptyList(),
                    new ArrayList<String>(prerequisiteFailures));
        }
        for (Map.Entry<String, Object> entry : inputs.entrySet()) {
            String role = entry.getKey();
            if (!INPUT_ROLES.contains(role)) {
                prerequisiteFailures.add("input-role-unknown:" + role);
                continue;
            }
            if (!schemaValidForRole(role, entry.getValue())) {
                prerequisiteFailures.add("schema-invalid:" + role);
                continue;
            }
            if (!matchesRoleKind(role, entry.getValue())) {
                prerequisiteFailures.add("input-role-kind-mismatch:" + role);
            }
        }
        if (!prerequisiteFailures.isEmpty()) {
            return new BindingValidation(Collections.<String>emptyList(), Collections.<String>emptyList(),
                    new ArrayList<String>(prerequisiteFailures));
        }

        LinkedHashSet<String> evaluated = new LinkedHashSet<String>();
        LinkedHashSet<String> violated = new LinkedHashSet<String>();
        try {
            Map<String, Object> result = object(inputs.get("result"));
            if (result != null) {
                String command = text(result.get("command"));
                String status = text(result.get("status"));
                if ("plan-change".equals(command) && "succeeded".equals(status)) {
                    validatePlanChange(result, object(inputs.get("change_request")), object(inputs.get("approval")),
                            evaluated, violated);
                }
                if ("apply-change".equals(command) && "succeeded".equals(status)) {
                    validateApplyChange(result, object(inputs.get("plan_result")), evaluated, violated);
                }
                if ("verify-artifact".equals(command)) {
                    validateVerifyArtifact(result, evaluated, violated);
                }
            }
            Map<String, Object> projection = object(inputs.get("projection"));
            if (projection != null) {
                validateProjection(projection, object(inputs.get("semantic_state")), evaluated, violated);
            }
        } catch (ConformanceCanonicalJson.CanonicalJsonException error) {
            prerequisiteFailures.add("canonical-json-invalid");
        }
        return new BindingValidation(new ArrayList<String>(evaluated), new ArrayList<String>(violated),
                new ArrayList<String>(prerequisiteFailures));
    }

    private boolean schemaValidForRole(String role, Object value) {
        if ("result".equals(role) || "plan_result".equals(role)) {
            return registry.validateResult(value).valid;
        }
        return registry.validateArtifact(value).valid;
    }

    private boolean matchesRoleKind(String role, Object value) {
        Map<String, Object> document = object(value);
        if (document == null) {
            return false;
        }
        String kind = text(document.get("kind"));
        if ("change_request".equals(role)) {
            return "miku_project_change_request".equals(kind);
        }
        if ("approval".equals(role)) {
            return "miku_project_change_approval".equals(kind);
        }
        if ("projection".equals(role)) {
            return "miku_project_projection".equals(kind);
        }
        if ("semantic_state".equals(role)) {
            return "miku_project_semantic_state".equals(kind);
        }
        return "miku_project_cli_result".equals(kind);
    }

    private void validatePlanChange(Map<String, Object> result, Map<String, Object> request,
            Map<String, Object> approval, Set<String> evaluated, Set<String> violated)
            throws ConformanceCanonicalJson.CanonicalJsonException {
        Map<String, Object> data = object(result.get("data"));
        Map<String, Object> semanticDiff = data == null ? null : object(data.get("semantic_diff"));
        Map<String, Object> outputPlan = data == null ? null : object(data.get("output_plan"));

        evaluate(evaluated, violated, "RB-001", request != null && semanticDiff != null && outputPlan != null
                && same(digestAt(semanticDiff, "base_state_digest"), digestAt(outputPlan, "base_state_digest"))
                && same(digestAt(semanticDiff, "base_state_digest"), digestAt(object(request.get("base")), "state_digest")));

        evaluate(evaluated, violated, "RB-002", request != null && semanticDiff != null && outputPlan != null
                && same(digestAt(semanticDiff, "change_request_digest"), digestAt(outputPlan, "change_request_digest"))
                && same(digestAt(semanticDiff, "change_request_digest"), ConformanceCanonicalJson.sha256Digest(request)));

        evaluate(evaluated, violated, "RB-003", semanticDiff != null && outputPlan != null
                && same(digestAt(outputPlan, "semantic_diff_digest"), ConformanceCanonicalJson.sha256Digest(semanticDiff))
                && same(digestAt(object(outputPlan.get("preflight")), "proposed_state_digest"),
                        digestAt(semanticDiff, "proposed_state_digest")));

        Map<String, Object> runtime = object(result.get("runtime"));
        Map<String, Object> planRuntime = outputPlan == null ? null : object(outputPlan.get("runtime"));
        evaluate(evaluated, violated, "RB-004", runtime != null && "verified".equals(text(runtime.get("binding_status")))
                && planRuntime != null && same(runtimeWithoutBindingStatus(runtime), planRuntime));

        Map<String, Object> io = object(result.get("io"));
        Map<String, Object> destination = io == null ? null : object(io.get("destination"));
        Map<String, Object> planOutput = outputPlan == null ? null : object(outputPlan.get("output"));
        Map<String, Object> planDestination = planOutput == null ? null : object(planOutput.get("destination"));
        evaluate(evaluated, violated, "RB-005", same(textAt(destination, "path"), textAt(planDestination, "path")));

        if (approval != null) {
            evaluate(evaluated, violated, "RB-006", request != null && semanticDiff != null && outputPlan != null
                    && same(digestAt(approval, "base_state_digest"), digestAt(object(request.get("base")), "state_digest"))
                    && same(digestAt(approval, "change_request_digest"), ConformanceCanonicalJson.sha256Digest(request))
                    && same(digestAt(approval, "semantic_diff_digest"), ConformanceCanonicalJson.sha256Digest(semanticDiff))
                    && same(digestAt(approval, "output_plan_digest"), ConformanceCanonicalJson.sha256Digest(outputPlan)));
        }
    }

    private void validateApplyChange(Map<String, Object> result, Map<String, Object> planResult,
            Set<String> evaluated, Set<String> violated) throws ConformanceCanonicalJson.CanonicalJsonException {
        Map<String, Object> planData = planResult == null ? null : object(planResult.get("data"));
        Map<String, Object> outputPlan = planData == null ? null : object(planData.get("output_plan"));
        Map<String, Object> planOutput = outputPlan == null ? null : object(outputPlan.get("output"));
        Map<String, Object> planDestinationObject = planOutput == null ? null : object(planOutput.get("destination"));
        String planDestination = textAt(planDestinationObject, "path");
        Map<String, Object> io = object(result.get("io"));
        String resultDestination = textAt(object(io == null ? null : io.get("destination")), "path");
        Map<String, Object> effects = object(result.get("effects"));
        Map<String, Object> projectArtifact = effects == null ? null : object(effects.get("project_artifact"));
        Map<String, Object> data = object(result.get("data"));
        Map<String, Object> artifactSet = data == null ? null : object(data.get("artifact_set"));
        Map<String, Object> cleanup = effects == null ? null : object(effects.get("cleanup"));
        boolean cleanupPathMatches = cleanup != null && cleanup.get("path") == null
                || cleanup != null && same(text(cleanup.get("path")), planDestination);
        boolean validPlan = planResult != null && "plan-change".equals(text(planResult.get("command")))
                && "succeeded".equals(text(planResult.get("status")));
        evaluate(evaluated, violated, "RB-011", validPlan && planDestination != null
                && same(resultDestination, planDestination) && same(textAt(projectArtifact, "path"), planDestination)
                && same(textAt(artifactSet, "path"), planDestination)
                && "committed".equals(textAt(projectArtifact, "publication_state"))
                && same(textAt(projectArtifact, "publication_state"), textAt(artifactSet, "publication_state"))
                && cleanupPathMatches);
    }

    private void validateVerifyArtifact(Map<String, Object> result, Set<String> evaluated, Set<String> violated)
            throws ConformanceCanonicalJson.CanonicalJsonException {
        Map<String, Object> io = object(result.get("io"));
        Map<String, Object> artifactSetInput = inputByRole(io == null ? null : list(io.get("inputs")), "artifact_set");
        Map<String, Object> data = object(result.get("data"));
        Map<String, Object> verification = data == null ? null : object(data.get("verification"));
        Map<String, Object> effects = object(result.get("effects"));
        Map<String, Object> projectArtifact = effects == null ? null : object(effects.get("project_artifact"));
        Map<String, Object> cleanup = effects == null ? null : object(effects.get("cleanup"));
        String artifactPath = textAt(artifactSetInput, "path");
        String verificationPath = textAt(verification, "path");
        String effectPath = textAt(projectArtifact, "path");
        boolean cleanupPathMatches = cleanup != null && cleanup.get("path") == null
                || cleanup != null && same(text(cleanup.get("path")), artifactPath);
        evaluate(evaluated, violated, "RB-011", artifactPath != null && same(artifactPath, verificationPath)
                && same(artifactPath, effectPath)
                && same(textAt(verification, "publication_state"), textAt(projectArtifact, "publication_state"))
                && cleanupPathMatches);
    }

    private void validateProjection(Map<String, Object> projection, Map<String, Object> semanticState,
            Set<String> evaluated, Set<String> violated) throws ConformanceCanonicalJson.CanonicalJsonException {
        boolean valid = semanticState != null
                && same(digestAt(projection, "source_state_digest"),
                        ConformanceCanonicalJson.sha256SemanticStateDigest(semanticState));
        String purpose = text(projection.get("purpose"));
        if ("project_overview".equals(purpose)) {
            valid = valid && projectionOverviewMatches(projection, semanticState);
        } else if ("task_change_context".equals(purpose)) {
            valid = valid && taskChangeContextMatches(projection, semanticState);
        } else {
            valid = false;
        }
        evaluate(evaluated, violated, "RB-012", valid);
    }

    private boolean projectionOverviewMatches(Map<String, Object> projection, Map<String, Object> semanticState)
            throws ConformanceCanonicalJson.CanonicalJsonException {
        if (!same(projection.get("project"), semanticState.get("project"))) {
            return false;
        }
        List<Object> sourceTasks = list(semanticState.get("tasks"));
        List<Object> expectedTasks = new ArrayList<Object>();
        if (sourceTasks == null) {
            return false;
        }
        for (int index = 0; index < sourceTasks.size(); index++) {
            Map<String, Object> task = object(sourceTasks.get(index));
            if (task == null) {
                return false;
            }
            LinkedHashMap<String, Object> overviewTask = new LinkedHashMap<String, Object>();
            overviewTask.put("uid", task.get("uid"));
            overviewTask.put("name", task.get("name"));
            overviewTask.put("parent_uid", task.get("parent_uid"));
            overviewTask.put("order", Integer.valueOf(index));
            overviewTask.put("summary", task.get("summary"));
            overviewTask.put("percent_complete", task.get("percent_complete"));
            expectedTasks.add(overviewTask);
        }
        return same(projection.get("tasks"), expectedTasks)
                && sameUnordered(list(projection.get("dependencies")), list(semanticState.get("dependencies")));
    }

    private boolean taskChangeContextMatches(Map<String, Object> projection, Map<String, Object> semanticState)
            throws ConformanceCanonicalJson.CanonicalJsonException {
        if (!same(projection.get("project"), semanticState.get("project"))) {
            return false;
        }
        Map<String, Object> scope = object(projection.get("scope"));
        String targetUid = textAt(scope, "target_task_uid");
        List<Object> sourceTasks = list(semanticState.get("tasks"));
        Map<String, Object> target = taskByUid(sourceTasks, targetUid);
        if (target == null || !Boolean.FALSE.equals(target.get("summary")) || hasDirectChild(sourceTasks, targetUid)
                || !same(projection.get("target_task"), target)) {
            return false;
        }
        List<Object> expectedAncestors = ancestorChain(sourceTasks, target);
        if (expectedAncestors == null || !same(projection.get("ancestors"), expectedAncestors)) {
            return false;
        }
        List<Object> sourceDependencies = list(semanticState.get("dependencies"));
        List<Object> expectedDependencies = new ArrayList<Object>();
        if (sourceDependencies == null) {
            return false;
        }
        for (Object rawDependency : sourceDependencies) {
            Map<String, Object> dependency = object(rawDependency);
            if (dependency != null && (targetUid.equals(text(dependency.get("predecessor_uid")))
                    || targetUid.equals(text(dependency.get("successor_uid"))))) {
                expectedDependencies.add(dependency);
            }
        }
        List<Object> sourceAssignments = list(semanticState.get("assignments"));
        List<Object> expectedAssignments = new ArrayList<Object>();
        LinkedHashSet<String> resourceUids = new LinkedHashSet<String>();
        if (sourceAssignments == null) {
            return false;
        }
        for (Object rawAssignment : sourceAssignments) {
            Map<String, Object> assignment = object(rawAssignment);
            if (assignment != null && targetUid.equals(text(assignment.get("task_uid")))) {
                expectedAssignments.add(assignment);
                String resourceUid = text(assignment.get("resource_uid"));
                if (resourceUid != null) {
                    resourceUids.add(resourceUid);
                }
            }
        }
        List<Object> sourceResources = list(semanticState.get("resources"));
        List<Object> expectedResources = new ArrayList<Object>();
        if (sourceResources == null) {
            return false;
        }
        for (Object rawResource : sourceResources) {
            Map<String, Object> resource = object(rawResource);
            if (resource != null && resourceUids.contains(text(resource.get("uid")))) {
                expectedResources.add(resource);
            }
        }
        return sameUnordered(list(projection.get("dependencies")), expectedDependencies)
                && sameUnordered(list(projection.get("assignments")), expectedAssignments)
                && sameUnordered(list(projection.get("resources")), expectedResources);
    }

    private static List<Object> ancestorChain(List<Object> tasks, Map<String, Object> target) {
        if (tasks == null) {
            return null;
        }
        LinkedHashMap<String, Map<String, Object>> tasksByUid = new LinkedHashMap<String, Map<String, Object>>();
        for (Object rawTask : tasks) {
            Map<String, Object> task = object(rawTask);
            String uid = task == null ? null : text(task.get("uid"));
            if (uid == null || tasksByUid.put(uid, task) != null) {
                return null;
            }
        }
        List<Object> ancestors = new ArrayList<Object>();
        LinkedHashSet<String> visited = new LinkedHashSet<String>();
        Map<String, Object> current = target;
        while (current != null) {
            String parentUid = text(current.get("parent_uid"));
            if (parentUid == null) {
                return ancestors;
            }
            if (!visited.add(parentUid)) {
                return null;
            }
            Map<String, Object> parent = tasksByUid.get(parentUid);
            if (parent == null) {
                return null;
            }
            ancestors.add(0, parent);
            current = parent;
        }
        return null;
    }

    private static Map<String, Object> taskByUid(List<Object> tasks, String uid) {
        if (tasks == null || uid == null) {
            return null;
        }
        Map<String, Object> result = null;
        for (Object rawTask : tasks) {
            Map<String, Object> task = object(rawTask);
            if (task != null && uid.equals(text(task.get("uid")))) {
                if (result != null) {
                    return null;
                }
                result = task;
            }
        }
        return result;
    }

    private static boolean hasDirectChild(List<Object> tasks, String parentUid) {
        if (tasks == null || parentUid == null) {
            return false;
        }
        for (Object rawTask : tasks) {
            Map<String, Object> task = object(rawTask);
            if (task != null && parentUid.equals(text(task.get("parent_uid")))) {
                return true;
            }
        }
        return false;
    }

    private static Map<String, Object> inputByRole(List<Object> inputs, String role) {
        if (inputs == null) {
            return null;
        }
        Map<String, Object> result = null;
        for (Object rawInput : inputs) {
            Map<String, Object> input = object(rawInput);
            if (input != null && role.equals(text(input.get("role")))) {
                if (result != null) {
                    return null;
                }
                result = input;
            }
        }
        return result;
    }

    private static Map<String, Object> runtimeWithoutBindingStatus(Map<String, Object> runtime) {
        if (runtime == null) {
            return null;
        }
        LinkedHashMap<String, Object> copy = new LinkedHashMap<String, Object>(runtime);
        copy.remove("binding_status");
        return copy;
    }

    private static boolean sameUnordered(List<Object> left, List<Object> right)
            throws ConformanceCanonicalJson.CanonicalJsonException {
        if (left == null || right == null || left.size() != right.size()) {
            return false;
        }
        List<String> leftCanonical = new ArrayList<String>();
        List<String> rightCanonical = new ArrayList<String>();
        for (Object item : left) {
            leftCanonical.add(ConformanceCanonicalJson.serialize(item));
        }
        for (Object item : right) {
            rightCanonical.add(ConformanceCanonicalJson.serialize(item));
        }
        Collections.sort(leftCanonical);
        Collections.sort(rightCanonical);
        return leftCanonical.equals(rightCanonical);
    }

    private static void evaluate(Set<String> evaluated, Set<String> violated, String ruleId, boolean valid) {
        evaluated.add(ruleId);
        if (!valid) {
            violated.add(ruleId);
        }
    }

    private static boolean same(Object left, Object right) throws ConformanceCanonicalJson.CanonicalJsonException {
        return left != null && right != null && ConformanceCanonicalJson.jsonEquals(left, right);
    }

    private static Object digestAt(Map<String, Object> object, String key) {
        return object == null ? null : object.get(key);
    }

    private static String textAt(Map<String, Object> object, String key) {
        return object == null ? null : text(object.get(key));
    }

    private static String text(Object value) {
        return value instanceof String ? (String) value : null;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> object(Object value) {
        return value instanceof Map<?, ?> ? (Map<String, Object>) value : null;
    }

    @SuppressWarnings("unchecked")
    private static List<Object> list(Object value) {
        return value instanceof List<?> ? (List<Object>) value : null;
    }

    private static Set<String> setOf(String... values) {
        return new LinkedHashSet<String>(Arrays.asList(values));
    }

    public static final class BindingValidation {
        public final boolean valid;
        public final List<String> evaluatedRuleIds;
        public final List<String> violatedRuleIds;
        public final List<String> prerequisiteFailures;

        BindingValidation(List<String> evaluatedRuleIds, List<String> violatedRuleIds,
                List<String> prerequisiteFailures) {
            this.valid = violatedRuleIds.isEmpty() && prerequisiteFailures.isEmpty();
            this.evaluatedRuleIds = Collections.unmodifiableList(new ArrayList<String>(evaluatedRuleIds));
            this.violatedRuleIds = Collections.unmodifiableList(new ArrayList<String>(violatedRuleIds));
            this.prerequisiteFailures = Collections.unmodifiableList(new ArrayList<String>(prerequisiteFailures));
        }
    }
}
