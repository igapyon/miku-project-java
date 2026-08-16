/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuproject.conformance;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jp.igapyon.mikuproject.coreapi.CoreApiAiJsonUtil;

/**
 * Test-side loader for the immutable v1 conformance corpus.
 *
 * <p>The public entrypoint verifies the snapshot inventory before parsing the
 * two case indexes. The package-private entrypoint exists solely so focused
 * tests can exercise malformed index documents in a copied snapshot.</p>
 */
public final class ConformanceSuiteLoader {
    private static final String CONFORMANCE_DIRECTORY = "testdata/conformance/v1";
    private static final String SUITE_INDEX_PATH = CONFORMANCE_DIRECTORY + "/suite-index.json";
    private static final String CONTRACT_CASES_PATH = CONFORMANCE_DIRECTORY + "/contract-cases.json";
    private static final String CONTRACT_DOCUMENT_PATH = "docs/miku-project-conformance-corpus-v1.md";
    private static final String SEMANTIC_CATALOG_PATH = "docs/miku-project-semantic-fixture-catalog-v1.md";
    private static final String ARTIFACT_SCHEMA_PATH = "docs/schemas/miku-project-artifacts-v1.schema.json";
    private static final String RESULT_SCHEMA_PATH = "docs/schemas/miku-project-cli-result-v1.schema.json";
    private static final String CANONICALIZATION_REFERENCE = "../../../docs/miku-project-conformance-corpus-v1.md#seed-fixture%E3%81%AE%E6%84%8F%E5%91%B3";
    private static final String CAPABILITY_PROFILE = "miku-project-cli-core/v1";
    private static final String FIXTURE_SUITE_VERSION = "1";

    private static final Set<String> WORKFLOW_COMMANDS = setOf("validate", "inspect", "plan-change",
            "apply-change", "verify-artifact", "apply-change+verify-artifact", "cli");
    private static final Set<String> WORKFLOW_STATUSES = setOf("succeeded", "rejected", "runtime-error", "usage-error");
    private static final Set<String> COMPARISON_MODES = setOf("schema", "exact-json", "semantic-state",
            "cross-artifact-binding", "artifact-topology", "runtime-integrity", "byte-same-runtime");
    private static final Set<String> NEXT_ACTIONS = setOf("complete", "revise-invocation-or-input",
            "request-human-approval", "verify-artifact", "replan-and-request-human-approval",
            "abort-and-investigate", "repair-environment");
    private static final Set<String> NEXT_ACTION_COMMANDS = setOf("plan-change", "verify-artifact");
    private static final Set<String> RETRYABILITIES = setOf("after-input-change", "after-environment-change",
            "after-replan-and-approval", "not-retryable");
    private static final Set<String> CONTRACT_VALIDATION_LAYERS = setOf("json-schema", "cross-artifact-binding");
    private static final Set<String> CONTRACT_INPUT_ROLES = setOf("result", "change_request", "approval",
            "plan_result", "projection", "semantic_state");
    private static final Set<String> JSON_POINTER_OPERATIONS = setOf("add", "remove", "replace");

    private final CoreApiAiJsonUtil jsonUtil = new CoreApiAiJsonUtil();

    private ConformanceSuiteLoader() {
    }

    /**
     * Loads the two indexes only after the snapshot passes the P5-A verifier.
     */
    public static ConformanceSuite load(Path snapshotRoot) throws IOException {
        ContractSnapshotVerifier.verify(snapshotRoot);
        return new ConformanceSuiteLoader().loadFromVerifiedSnapshot(snapshotRoot);
    }

    /**
     * Test-only parser entrypoint for a caller that has already established a
     * trusted snapshot boundary. Do not use this to bypass {@link #load(Path)}
     * in a future command test.
     */
    static ConformanceSuite loadFromVerifiedSnapshotForTest(Path snapshotRoot) throws IOException {
        return new ConformanceSuiteLoader().loadFromVerifiedSnapshot(snapshotRoot);
    }

    private ConformanceSuite loadFromVerifiedSnapshot(Path snapshotRoot) throws IOException {
        Path root = requireSnapshotRoot(snapshotRoot);
        Path indexDirectory = root.resolve(CONFORMANCE_DIRECTORY).normalize();
        Path suiteIndexFile = resolveFixedMember(root, SUITE_INDEX_PATH, "suite-index.json");
        Path contractCasesFile = resolveFixedMember(root, CONTRACT_CASES_PATH, "contract-cases.json");

        Map<String, Object> suiteIndexDocument = readJsonObject(suiteIndexFile, "suite-index.json");
        Map<String, Object> contractCasesDocument = readJsonObject(contractCasesFile, "contract-cases.json");
        SuiteIndex suiteIndex = parseSuiteIndex(root, indexDirectory, suiteIndexDocument);
        ContractCaseIndex contractCases = parseContractCases(root, indexDirectory, contractCasesDocument,
                suiteIndex.workflowCasesById.keySet());
        return new ConformanceSuite(root, suiteIndex, contractCases);
    }

    private SuiteIndex parseSuiteIndex(Path root, Path indexDirectory, Map<String, Object> document) throws IOException {
        requireExactKeys(document, "suite-index.json", setOf("kind", "schema_version", "fixture_suite_version",
                "capability_profile", "contract", "semantic_catalog_materialization", "cases"));
        requireEquals("miku_project_conformance_suite_index", document.get("kind"), "suite-index.json.kind");
        requireEquals("1", document.get("schema_version"), "suite-index.json.schema_version");
        requireEquals(FIXTURE_SUITE_VERSION, document.get("fixture_suite_version"), "suite-index.json.fixture_suite_version");
        requireEquals(CAPABILITY_PROFILE, document.get("capability_profile"), "suite-index.json.capability_profile");
        Path contractDocument = resolveExpectedReference(root, indexDirectory, document.get("contract"),
                "../../../" + CONTRACT_DOCUMENT_PATH, "suite-index.json.contract", exactPath(CONTRACT_DOCUMENT_PATH));
        SemanticCatalogMaterialization semanticCatalog = parseSemanticCatalog(root, indexDirectory,
                document.get("semantic_catalog_materialization"));

        List<Object> rawCases = requireList(document.get("cases"), "suite-index.json.cases");
        if (rawCases.isEmpty()) {
            throw failure("conformance-suite.empty-workflow-cases", "suite-index.json.cases must not be empty");
        }
        List<WorkflowCase> workflowCases = new ArrayList<WorkflowCase>();
        LinkedHashMap<String, WorkflowCase> workflowCasesById = new LinkedHashMap<String, WorkflowCase>();
        for (int index = 0; index < rawCases.size(); index++) {
            WorkflowCase workflowCase = parseWorkflowCase(root, indexDirectory,
                    requireObject(rawCases.get(index), "suite-index.json.cases[" + index + "]"),
                    new LinkedHashSet<String>(semanticCatalog.fixtureIds));
            if (workflowCasesById.put(workflowCase.id, workflowCase) != null) {
                throw failure("conformance-suite.duplicate-case-id",
                        "suite-index.json contains a duplicate case ID: " + workflowCase.id);
            }
            workflowCases.add(workflowCase);
        }
        validateWorkflowCaseReferences(workflowCasesById);
        return new SuiteIndex(contractDocument, semanticCatalog, workflowCases, workflowCasesById);
    }

    private SemanticCatalogMaterialization parseSemanticCatalog(Path root, Path indexDirectory, Object raw) throws IOException {
        Map<String, Object> catalog = requireObject(raw, "suite-index.json.semantic_catalog_materialization");
        requireExactKeys(catalog, "suite-index.json.semantic_catalog_materialization",
                setOf("source", "ids", "materialization_phase"));
        Path source = resolveExpectedReference(root, indexDirectory, catalog.get("source"),
                "../../../" + SEMANTIC_CATALOG_PATH, "suite-index.json.semantic_catalog_materialization.source",
                exactPath(SEMANTIC_CATALOG_PATH));
        List<String> ids = requireStringList(catalog.get("ids"), "suite-index.json.semantic_catalog_materialization.ids", true);
        if (ids.isEmpty()) {
            throw failure("conformance-suite.empty-semantic-catalog", "semantic catalog IDs must not be empty");
        }
        requireEquals("P4/P5", catalog.get("materialization_phase"),
                "suite-index.json.semantic_catalog_materialization.materialization_phase");
        return new SemanticCatalogMaterialization(source, ids);
    }

    private WorkflowCase parseWorkflowCase(Path root, Path indexDirectory, Map<String, Object> value,
            Set<String> semanticCatalogIds) throws IOException {
        Set<String> required = setOf("id", "semantic_fixture_ids", "command", "parameters", "input",
                "golden_semantic_state", "expected_status", "expected_exit_code", "expected_next_action",
                "expected_diagnostic_codes", "expected_rule_ids", "comparison_modes", "materialization_phase");
        Set<String> allowed = setOf("id", "semantic_fixture_ids", "command", "parameters", "input",
                "change_request_template", "golden_semantic_state", "expected_status", "expected_exit_code",
                "expected_next_action", "expected_diagnostic_codes", "expected_diagnostic_paths", "expected_rule_ids",
                "comparison_modes", "materialization_phase");
        requireKeys(value, "suite-index.json workflow case", required, allowed);
        String id = requireCaseId(value.get("id"), "suite-index.json workflow case.id");
        List<String> semanticFixtureIds = requireStringList(value.get("semantic_fixture_ids"),
                "suite-index.json workflow case " + id + ".semantic_fixture_ids", true);
        for (String semanticFixtureId : semanticFixtureIds) {
            if (!semanticCatalogIds.contains(semanticFixtureId)) {
                throw failure("conformance-suite.unknown-semantic-fixture",
                        "workflow case " + id + " refers to an unknown semantic fixture ID: " + semanticFixtureId);
            }
        }
        String command = requireEnum(value.get("command"), WORKFLOW_COMMANDS,
                "suite-index.json workflow case " + id + ".command");
        Map<String, Object> parameters = requireObject(value.get("parameters"),
                "suite-index.json workflow case " + id + ".parameters");
        validateWorkflowParameters(command, parameters, id);
        Path input = resolveNullableReference(root, indexDirectory, value.get("input"),
                "suite-index.json workflow case " + id + ".input", workflowProjectFixturePath());
        Path changeRequestTemplate = resolveNullableReference(root, indexDirectory, value.get("change_request_template"),
                "suite-index.json workflow case " + id + ".change_request_template", workflowChangeFixturePath());
        Path goldenSemanticState = resolveNullableReference(root, indexDirectory, value.get("golden_semantic_state"),
                "suite-index.json workflow case " + id + ".golden_semantic_state", workflowSemanticGoldenPath());
        String expectedStatus = requireEnum(value.get("expected_status"), WORKFLOW_STATUSES,
                "suite-index.json workflow case " + id + ".expected_status");
        int expectedExitCode = requireExitCode(value.get("expected_exit_code"),
                "suite-index.json workflow case " + id + ".expected_exit_code");
        NextAction nextAction = parseNextAction(value.get("expected_next_action"),
                "suite-index.json workflow case " + id + ".expected_next_action");
        List<String> diagnosticCodes = requireStringList(value.get("expected_diagnostic_codes"),
                "suite-index.json workflow case " + id + ".expected_diagnostic_codes", false);
        List<String> diagnosticPaths = value.containsKey("expected_diagnostic_paths")
                ? requireStringList(value.get("expected_diagnostic_paths"),
                        "suite-index.json workflow case " + id + ".expected_diagnostic_paths", false)
                : Collections.<String>emptyList();
        List<String> ruleIds = requireNullableStringList(value.get("expected_rule_ids"),
                "suite-index.json workflow case " + id + ".expected_rule_ids", false);
        List<String> comparisonModes = requireStringList(value.get("comparison_modes"),
                "suite-index.json workflow case " + id + ".comparison_modes", true);
        if (comparisonModes.isEmpty()) {
            throw failure("conformance-suite.empty-comparison-modes", "workflow case " + id + " has no comparison modes");
        }
        for (String comparisonMode : comparisonModes) {
            if (!COMPARISON_MODES.contains(comparisonMode)) {
                throw failure("conformance-suite.unknown-comparison-mode",
                        "workflow case " + id + " has an unknown comparison mode: " + comparisonMode);
            }
        }
        String phase = requireEnum(value.get("materialization_phase"), setOf("P3", "P4/P5"),
                "suite-index.json workflow case " + id + ".materialization_phase");
        return new WorkflowCase(id, semanticFixtureIds, command, parameters, input, changeRequestTemplate,
                goldenSemanticState, expectedStatus, expectedExitCode, nextAction, diagnosticCodes, diagnosticPaths,
                ruleIds, comparisonModes, phase);
    }

    private void validateWorkflowParameters(String command, Map<String, Object> parameters, String caseId) throws IOException {
        String label = "suite-index.json workflow case " + caseId + ".parameters";
        if ("validate".equals(command)) {
            requireKeys(parameters, label, Collections.<String>emptySet(), setOf("expected_project_input_read", "runtime_setup"));
            if (parameters.containsKey("expected_project_input_read")
                    && !(parameters.get("expected_project_input_read") instanceof Boolean)) {
                throw failure("conformance-suite.parameter-invalid", label + ".expected_project_input_read must be boolean");
            }
            if (parameters.containsKey("runtime_setup")) {
                validateRuntimeSetup(requireObject(parameters.get("runtime_setup"), label + ".runtime_setup"), label + ".runtime_setup");
            }
            return;
        }
        if ("inspect".equals(command)) {
            requireExactKeys(parameters, label, setOf("purpose", "task_uid"));
            requireEnum(parameters.get("purpose"), setOf("project_overview", "task_change_context"), label + ".purpose");
            requireNullableString(parameters.get("task_uid"), label + ".task_uid");
            return;
        }
        if ("plan-change".equals(command)) {
            requireExactKeys(parameters, label, setOf("destination", "request"));
            requireString(parameters.get("destination"), label + ".destination");
            requireString(parameters.get("request"), label + ".request");
            return;
        }
        if ("apply-change".equals(command)) {
            requireKeys(parameters, label, setOf("plan_case", "approval"), setOf("plan_case", "approval", "before_apply", "injection"));
            requireString(parameters.get("plan_case"), label + ".plan_case");
            requireString(parameters.get("approval"), label + ".approval");
            if (parameters.containsKey("before_apply")) {
                requireString(parameters.get("before_apply"), label + ".before_apply");
            }
            if (parameters.containsKey("injection")) {
                requireString(parameters.get("injection"), label + ".injection");
            }
            return;
        }
        if ("verify-artifact".equals(command)) {
            requireKeys(parameters, label, setOf("artifact_setup"), setOf("artifact_setup", "expect_plan_result"));
            requireString(parameters.get("artifact_setup"), label + ".artifact_setup");
            if (parameters.containsKey("expect_plan_result")) {
                requireString(parameters.get("expect_plan_result"), label + ".expect_plan_result");
            }
            return;
        }
        if ("apply-change+verify-artifact".equals(command)) {
            requireExactKeys(parameters, label, setOf("apply_case", "injection"));
            requireString(parameters.get("apply_case"), label + ".apply_case");
            requireString(parameters.get("injection"), label + ".injection");
            return;
        }
        if ("cli".equals(command)) {
            requireExactKeys(parameters, label, setOf("arguments"));
            requireStringList(parameters.get("arguments"), label + ".arguments", false);
            return;
        }
        throw failure("conformance-suite.command-invalid", label + " has an unsupported command: " + command);
    }

    private void validateRuntimeSetup(Map<String, Object> setup, String label) throws IOException {
        requireExactKeys(setup, label, setOf("manifest_base", "manifest_mutations", "filesystem_mutations"));
        requireEquals("generated-test-runtime", setup.get("manifest_base"), label + ".manifest_base");
        List<Object> manifestMutations = requireList(setup.get("manifest_mutations"), label + ".manifest_mutations");
        for (int index = 0; index < manifestMutations.size(); index++) {
            validatePointerMutation(requireObject(manifestMutations.get(index), label + ".manifest_mutations[" + index + "]"),
                    label + ".manifest_mutations[" + index + "]", null);
        }
        List<Object> filesystemMutations = requireList(setup.get("filesystem_mutations"), label + ".filesystem_mutations");
        for (int index = 0; index < filesystemMutations.size(); index++) {
            Map<String, Object> mutation = requireObject(filesystemMutations.get(index),
                    label + ".filesystem_mutations[" + index + "]");
            String mutationLabel = label + ".filesystem_mutations[" + index + "]";
            String operation = requireString(mutation.get("operation"), mutationLabel + ".operation");
            if ("append-content".equals(operation)) {
                requireExactKeys(mutation, mutationLabel, setOf("operation", "artifact_role", "content"));
                requireEnum(mutation.get("artifact_role"), setOf("executable", "sources"), mutationLabel + ".artifact_role");
                requireString(mutation.get("content"), mutationLabel + ".content");
            } else if ("remove".equals(operation)) {
                requireExactKeys(mutation, mutationLabel, setOf("operation", "artifact_role"));
                requireEnum(mutation.get("artifact_role"), setOf("executable", "sources"), mutationLabel + ".artifact_role");
            } else {
                throw failure("conformance-suite.mutation-invalid", mutationLabel + " has an unsupported filesystem mutation");
            }
        }
    }

    private NextAction parseNextAction(Object raw, String label) throws IOException {
        Map<String, Object> action = requireObject(raw, label);
        requireExactKeys(action, label, setOf("action", "command", "source_retryability"));
        String actionName = requireEnum(action.get("action"), NEXT_ACTIONS, label + ".action");
        String command = requireNullableString(action.get("command"), label + ".command");
        if (command != null && !NEXT_ACTION_COMMANDS.contains(command)) {
            throw failure("conformance-suite.next-action-invalid", label + ".command is unsupported: " + command);
        }
        String retryability = requireNullableString(action.get("source_retryability"), label + ".source_retryability");
        if (retryability != null && !RETRYABILITIES.contains(retryability)) {
            throw failure("conformance-suite.next-action-invalid", label + ".source_retryability is unsupported: " + retryability);
        }
        return new NextAction(actionName, command, retryability);
    }

    private void validateWorkflowCaseReferences(Map<String, WorkflowCase> workflowCasesById) throws IOException {
        for (WorkflowCase workflowCase : workflowCasesById.values()) {
            Object planCase = workflowCase.parameters.get("plan_case");
            if (planCase != null) {
                requireWorkflowCaseReference(workflowCasesById, planCase, workflowCase.id, "plan_case");
            }
            Object applyCase = workflowCase.parameters.get("apply_case");
            if (applyCase != null) {
                requireWorkflowCaseReference(workflowCasesById, applyCase, workflowCase.id, "apply_case");
            }
            Object artifactSetup = workflowCase.parameters.get("artifact_setup");
            if (artifactSetup instanceof String && ((String) artifactSetup).startsWith("output-of:")) {
                requireWorkflowCaseReference(workflowCasesById, ((String) artifactSetup).substring("output-of:".length()),
                        workflowCase.id, "artifact_setup");
            }
        }
    }

    private void requireWorkflowCaseReference(Map<String, WorkflowCase> workflowCasesById, Object value,
            String caseId, String field) throws IOException {
        String reference = requireString(value, "workflow case " + caseId + ".parameters." + field);
        if (!workflowCasesById.containsKey(reference)) {
            throw failure("conformance-suite.unknown-case-reference",
                    "workflow case " + caseId + " refers to an unknown workflow case: " + reference);
        }
    }

    private ContractCaseIndex parseContractCases(Path root, Path indexDirectory, Map<String, Object> document,
            Set<String> workflowCaseIds) throws IOException {
        requireExactKeys(document, "contract-cases.json", setOf("kind", "schema_version", "artifact_schema",
                "result_schema", "canonicalization", "cases"));
        requireEquals("miku_project_contract_validation_cases", document.get("kind"), "contract-cases.json.kind");
        requireEquals("1", document.get("schema_version"), "contract-cases.json.schema_version");
        Path artifactSchema = resolveExpectedReference(root, indexDirectory, document.get("artifact_schema"),
                "../../../" + ARTIFACT_SCHEMA_PATH, "contract-cases.json.artifact_schema", exactPath(ARTIFACT_SCHEMA_PATH));
        Path resultSchema = resolveExpectedReference(root, indexDirectory, document.get("result_schema"),
                "../../../" + RESULT_SCHEMA_PATH, "contract-cases.json.result_schema", exactPath(RESULT_SCHEMA_PATH));
        requireEquals(CANONICALIZATION_REFERENCE, document.get("canonicalization"), "contract-cases.json.canonicalization");
        resolveExpectedReference(root, indexDirectory,
                String.valueOf(document.get("canonicalization")).substring(0, CANONICALIZATION_REFERENCE.indexOf('#')),
                "../../../" + CONTRACT_DOCUMENT_PATH, "contract-cases.json.canonicalization", exactPath(CONTRACT_DOCUMENT_PATH));

        List<Object> rawCases = requireList(document.get("cases"), "contract-cases.json.cases");
        if (rawCases.isEmpty()) {
            throw failure("conformance-suite.empty-contract-cases", "contract-cases.json.cases must not be empty");
        }
        List<ContractCase> contractCases = new ArrayList<ContractCase>();
        LinkedHashMap<String, ContractCase> contractCasesById = new LinkedHashMap<String, ContractCase>();
        LinkedHashSet<String> allCaseIds = new LinkedHashSet<String>(workflowCaseIds);
        for (int index = 0; index < rawCases.size(); index++) {
            ContractCase contractCase = parseContractCase(root, indexDirectory,
                    requireObject(rawCases.get(index), "contract-cases.json.cases[" + index + "]"));
            if (!allCaseIds.add(contractCase.id)) {
                throw failure("conformance-suite.duplicate-case-id",
                        "contract-cases.json contains a duplicate case ID: " + contractCase.id);
            }
            contractCasesById.put(contractCase.id, contractCase);
            contractCases.add(contractCase);
        }
        return new ContractCaseIndex(artifactSchema, resultSchema, contractCases, contractCasesById);
    }

    private ContractCase parseContractCase(Path root, Path indexDirectory, Map<String, Object> value) throws IOException {
        Set<String> required = setOf("id", "validation_layer", "inputs", "mutations", "expected_valid", "expected_rule_ids");
        Set<String> allowed = setOf("id", "validation_layer", "inputs", "mutations", "expected_valid",
                "expected_rule_ids", "checked_rule_ids");
        requireKeys(value, "contract-cases.json case", required, allowed);
        String id = requireCaseId(value.get("id"), "contract-cases.json case.id");
        String validationLayer = requireEnum(value.get("validation_layer"), CONTRACT_VALIDATION_LAYERS,
                "contract-cases.json case " + id + ".validation_layer");
        List<ContractInput> inputs = parseContractInputs(root, indexDirectory, value.get("inputs"), id);
        List<Mutation> mutations = parseContractMutations(value.get("mutations"), id, inputs);
        if (!(value.get("expected_valid") instanceof Boolean)) {
            throw failure("conformance-suite.case-invalid", "contract case " + id + ".expected_valid must be boolean");
        }
        List<String> expectedRuleIds = requireStringList(value.get("expected_rule_ids"),
                "contract-cases.json case " + id + ".expected_rule_ids", false);
        List<String> checkedRuleIds = value.containsKey("checked_rule_ids")
                ? requireStringList(value.get("checked_rule_ids"),
                        "contract-cases.json case " + id + ".checked_rule_ids", true)
                : Collections.<String>emptyList();
        if (!"cross-artifact-binding".equals(validationLayer) && !checkedRuleIds.isEmpty()) {
            throw failure("conformance-suite.case-invalid",
                    "only cross-artifact-binding cases may declare checked_rule_ids: " + id);
        }
        return new ContractCase(id, validationLayer, inputs, mutations,
                ((Boolean) value.get("expected_valid")).booleanValue(), expectedRuleIds, checkedRuleIds);
    }

    private List<ContractInput> parseContractInputs(Path root, Path indexDirectory, Object raw, String caseId)
            throws IOException {
        List<Object> values = requireList(raw, "contract-cases.json case " + caseId + ".inputs");
        if (values.isEmpty()) {
            throw failure("conformance-suite.empty-contract-inputs", "contract case " + caseId + " has no inputs");
        }
        List<ContractInput> inputs = new ArrayList<ContractInput>();
        LinkedHashSet<String> roles = new LinkedHashSet<String>();
        for (int index = 0; index < values.size(); index++) {
            String label = "contract-cases.json case " + caseId + ".inputs[" + index + "]";
            Map<String, Object> input = requireObject(values.get(index), label);
            requireExactKeys(input, label, setOf("role", "path"));
            String role = requireEnum(input.get("role"), CONTRACT_INPUT_ROLES, label + ".role");
            if (!roles.add(role)) {
                throw failure("conformance-suite.duplicate-input-role",
                        "contract case " + caseId + " repeats input role: " + role);
            }
            Path path = resolveReference(root, indexDirectory, requireString(input.get("path"), label + ".path"), label + ".path",
                    contractInputPath());
            inputs.add(new ContractInput(role, path));
        }
        return inputs;
    }

    private List<Mutation> parseContractMutations(Object raw, String caseId, List<ContractInput> inputs) throws IOException {
        List<Object> values = requireList(raw, "contract-cases.json case " + caseId + ".mutations");
        LinkedHashSet<String> inputRoles = new LinkedHashSet<String>();
        for (ContractInput input : inputs) {
            inputRoles.add(input.role);
        }
        List<Mutation> mutations = new ArrayList<Mutation>();
        for (int index = 0; index < values.size(); index++) {
            String label = "contract-cases.json case " + caseId + ".mutations[" + index + "]";
            Map<String, Object> mutation = requireObject(values.get(index), label);
            String inputRole = requireString(mutation.get("input_role"), label + ".input_role");
            if (!inputRoles.contains(inputRole)) {
                throw failure("conformance-suite.unknown-input-role",
                        "contract case " + caseId + " mutates an absent input role: " + inputRole);
            }
            String operation = requireEnum(mutation.get("operation"), JSON_POINTER_OPERATIONS, label + ".operation");
            validatePointerMutation(mutation, label, inputRoles);
            String pointer = requireJsonPointer(mutation.get("pointer"), label + ".pointer");
            boolean hasValue = mutation.containsKey("value");
            mutations.add(new Mutation(inputRole, operation, pointer, mutation.get("value"), hasValue));
        }
        return mutations;
    }

    private void validatePointerMutation(Map<String, Object> mutation, String label, Set<String> allowedInputRoles)
            throws IOException {
        String operation = requireEnum(mutation.get("operation"), JSON_POINTER_OPERATIONS, label + ".operation");
        if (allowedInputRoles == null) {
            if ("remove".equals(operation)) {
                requireExactKeys(mutation, label, setOf("operation", "pointer"));
            } else {
                requireExactKeys(mutation, label, setOf("operation", "pointer", "value"));
            }
        } else {
            if ("remove".equals(operation)) {
                requireExactKeys(mutation, label, setOf("input_role", "operation", "pointer"));
            } else {
                requireExactKeys(mutation, label, setOf("input_role", "operation", "pointer", "value"));
            }
            String inputRole = requireString(mutation.get("input_role"), label + ".input_role");
            if (!allowedInputRoles.contains(inputRole)) {
                throw failure("conformance-suite.unknown-input-role", label + " refers to unknown input role: " + inputRole);
            }
        }
        requireJsonPointer(mutation.get("pointer"), label + ".pointer");
    }

    private Path resolveNullableReference(Path root, Path indexDirectory, Object value, String label,
            ReferencePolicy policy) throws IOException {
        if (value == null) {
            return null;
        }
        return resolveReference(root, indexDirectory, requireString(value, label), label, policy);
    }

    private Path resolveExpectedReference(Path root, Path indexDirectory, Object value, String expectedReference,
            String label, ReferencePolicy policy) throws IOException {
        requireEquals(expectedReference, value, label);
        return resolveReference(root, indexDirectory, expectedReference, label, policy);
    }

    private Path resolveReference(Path root, Path indexDirectory, String reference, String label, ReferencePolicy policy)
            throws IOException {
        if (reference.length() == 0 || reference.indexOf('\\') >= 0 || reference.indexOf('\u0000') >= 0) {
            throw failure("conformance-suite.reference-invalid", label + " is not a safe relative reference");
        }
        Path rawPath;
        try {
            rawPath = Paths.get(reference);
        } catch (RuntimeException error) {
            throw failure("conformance-suite.reference-invalid", label + " is not a filesystem path", error);
        }
        if (rawPath.isAbsolute()) {
            throw failure("conformance-suite.reference-escape", label + " must not be absolute");
        }
        Path resolved = indexDirectory.resolve(rawPath).normalize();
        if (!resolved.startsWith(root)) {
            throw failure("conformance-suite.reference-escape", label + " escapes the snapshot root");
        }
        String snapshotRelative = toSlash(root.relativize(resolved));
        String canonicalReference = toSlash(indexDirectory.relativize(resolved));
        String canonicalSnapshotRootReference = "../../../" + snapshotRelative;
        if (!reference.equals(canonicalReference) && !reference.equals(canonicalSnapshotRootReference)) {
            throw failure("conformance-suite.reference-invalid", label + " is not a normalized relative reference");
        }
        if (!policy.accepts(snapshotRelative)) {
            throw failure("conformance-suite.reference-not-allowed",
                    label + " refers outside its allowlisted snapshot members: " + snapshotRelative);
        }
        requireRegularNonSymlinkFile(root, resolved, label);
        return resolved;
    }

    private Path resolveFixedMember(Path root, String relativePath, String label) throws IOException {
        Path resolved = root.resolve(relativePath).normalize();
        if (!resolved.startsWith(root)) {
            throw failure("conformance-suite.reference-escape", label + " escapes the snapshot root");
        }
        requireRegularNonSymlinkFile(root, resolved, label);
        return resolved;
    }

    private void requireRegularNonSymlinkFile(Path root, Path file, String label) throws IOException {
        Path relative = root.relativize(file);
        Path current = root;
        for (Path segment : relative) {
            current = current.resolve(segment);
            if (Files.isSymbolicLink(current)) {
                throw failure("conformance-suite.symlink", label + " must not traverse a symbolic link");
            }
        }
        if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) {
            throw failure("conformance-suite.reference-unavailable", label + " must refer to a regular file");
        }
    }

    private Map<String, Object> readJsonObject(Path file, String label) throws IOException {
        Object parsed;
        try {
            parsed = jsonUtil.parseJsonText(new String(Files.readAllBytes(file), StandardCharsets.UTF_8));
        } catch (RuntimeException error) {
            throw failure("conformance-suite.json-invalid", label + " is not valid JSON", error);
        }
        return requireObject(parsed, label);
    }

    private Path requireSnapshotRoot(Path snapshotRoot) throws IOException {
        if (snapshotRoot == null) {
            throw failure("conformance-suite.snapshot-invalid", "snapshot root must not be null");
        }
        Path root = snapshotRoot.toAbsolutePath().normalize();
        if (Files.isSymbolicLink(root) || !Files.isDirectory(root, LinkOption.NOFOLLOW_LINKS)) {
            throw failure("conformance-suite.snapshot-invalid", "snapshot root must be a non-symlink directory");
        }
        return root;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> requireObject(Object value, String label) throws IOException {
        if (!(value instanceof Map<?, ?>)) {
            throw failure("conformance-suite.shape-invalid", label + " must be an object");
        }
        Map<?, ?> rawMap = (Map<?, ?>) value;
        LinkedHashMap<String, Object> result = new LinkedHashMap<String, Object>();
        for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
            if (!(entry.getKey() instanceof String)) {
                throw failure("conformance-suite.shape-invalid", label + " has a non-string field name");
            }
            result.put((String) entry.getKey(), entry.getValue());
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private static List<Object> requireList(Object value, String label) throws IOException {
        if (!(value instanceof List<?>)) {
            throw failure("conformance-suite.shape-invalid", label + " must be an array");
        }
        return new ArrayList<Object>((List<Object>) value);
    }

    private static void requireExactKeys(Map<String, Object> value, String label, Set<String> expectedKeys)
            throws IOException {
        requireKeys(value, label, expectedKeys, expectedKeys);
    }

    private static void requireKeys(Map<String, Object> value, String label, Set<String> requiredKeys,
            Set<String> allowedKeys) throws IOException {
        for (String requiredKey : requiredKeys) {
            if (!value.containsKey(requiredKey)) {
                throw failure("conformance-suite.required-field-missing", label + " is missing required field: " + requiredKey);
            }
        }
        for (String actualKey : value.keySet()) {
            if (!allowedKeys.contains(actualKey)) {
                throw failure("conformance-suite.unknown-field", label + " has an unknown field: " + actualKey);
            }
        }
    }

    private static String requireCaseId(Object value, String label) throws IOException {
        String id = requireString(value, label);
        if (!id.matches("[A-Z][A-Z0-9-]*")) {
            throw failure("conformance-suite.case-id-invalid", label + " must be an uppercase case ID");
        }
        return id;
    }

    private static String requireString(Object value, String label) throws IOException {
        if (!(value instanceof String) || ((String) value).length() == 0) {
            throw failure("conformance-suite.shape-invalid", label + " must be a non-empty string");
        }
        return (String) value;
    }

    private static String requireNullableString(Object value, String label) throws IOException {
        if (value == null) {
            return null;
        }
        return requireString(value, label);
    }

    private static String requireEnum(Object value, Set<String> allowed, String label) throws IOException {
        String stringValue = requireString(value, label);
        if (!allowed.contains(stringValue)) {
            throw failure("conformance-suite.value-invalid", label + " has an unsupported value: " + stringValue);
        }
        return stringValue;
    }

    private static int requireExitCode(Object value, String label) throws IOException {
        if (!(value instanceof Integer) && !(value instanceof Long)) {
            throw failure("conformance-suite.shape-invalid", label + " must be an integer");
        }
        long numeric = ((Number) value).longValue();
        if (numeric < 0 || numeric > 3) {
            throw failure("conformance-suite.value-invalid", label + " must be in the v1 exit-code range");
        }
        return (int) numeric;
    }

    private static List<String> requireStringList(Object value, String label, boolean unique) throws IOException {
        List<Object> rawValues = requireList(value, label);
        List<String> result = new ArrayList<String>();
        LinkedHashSet<String> seen = new LinkedHashSet<String>();
        for (int index = 0; index < rawValues.size(); index++) {
            String item = requireString(rawValues.get(index), label + "[" + index + "]");
            if (unique && !seen.add(item)) {
                throw failure("conformance-suite.duplicate-value", label + " contains a duplicate value: " + item);
            }
            result.add(item);
        }
        return Collections.unmodifiableList(result);
    }

    private static List<String> requireNullableStringList(Object value, String label, boolean unique) throws IOException {
        List<Object> rawValues = requireList(value, label);
        List<String> result = new ArrayList<String>();
        LinkedHashSet<String> seen = new LinkedHashSet<String>();
        boolean sawNull = false;
        for (int index = 0; index < rawValues.size(); index++) {
            Object rawItem = rawValues.get(index);
            if (rawItem == null) {
                if (unique && sawNull) {
                    throw failure("conformance-suite.duplicate-value", label + " contains a duplicate null value");
                }
                sawNull = true;
                result.add(null);
                continue;
            }
            String item = requireString(rawItem, label + "[" + index + "]");
            if (unique && !seen.add(item)) {
                throw failure("conformance-suite.duplicate-value", label + " contains a duplicate value: " + item);
            }
            result.add(item);
        }
        return Collections.unmodifiableList(result);
    }

    private static String requireJsonPointer(Object value, String label) throws IOException {
        String pointer = requireString(value, label);
        if (!pointer.startsWith("/")) {
            throw failure("conformance-suite.pointer-invalid", label + " must be an RFC 6901 pointer");
        }
        for (int index = 0; index < pointer.length(); index++) {
            if (pointer.charAt(index) == '~'
                    && (index + 1 >= pointer.length() || (pointer.charAt(index + 1) != '0' && pointer.charAt(index + 1) != '1'))) {
                throw failure("conformance-suite.pointer-invalid", label + " contains an invalid RFC 6901 escape");
            }
        }
        return pointer;
    }

    private static void requireEquals(Object expected, Object actual, String label) throws IOException {
        if (!expected.equals(actual)) {
            throw failure("conformance-suite.identity-mismatch", label + " does not match the fixed v1 corpus");
        }
    }

    private static ReferencePolicy exactPath(final String expected) {
        return new ReferencePolicy() {
            @Override
            public boolean accepts(String relativePath) {
                return expected.equals(relativePath);
            }
        };
    }

    private static ReferencePolicy workflowProjectFixturePath() {
        return underPath(CONFORMANCE_DIRECTORY + "/fixtures/project/");
    }

    private static ReferencePolicy workflowChangeFixturePath() {
        return underPath(CONFORMANCE_DIRECTORY + "/fixtures/change/");
    }

    private static ReferencePolicy workflowSemanticGoldenPath() {
        return underPath(CONFORMANCE_DIRECTORY + "/golden/semantic/");
    }

    private static ReferencePolicy contractInputPath() {
        return new ReferencePolicy() {
            @Override
            public boolean accepts(String relativePath) {
                return relativePath.startsWith("docs/examples/artifacts-v1/")
                        || relativePath.startsWith("docs/examples/cli-v1/")
                        || relativePath.startsWith(CONFORMANCE_DIRECTORY + "/golden/semantic/");
            }
        };
    }

    private static ReferencePolicy underPath(final String prefix) {
        return new ReferencePolicy() {
            @Override
            public boolean accepts(String relativePath) {
                return relativePath.startsWith(prefix);
            }
        };
    }

    private static String toSlash(Path path) {
        return path.toString().replace(path.getFileSystem().getSeparator(), "/");
    }

    private static Set<String> setOf(String... values) {
        return Collections.unmodifiableSet(new LinkedHashSet<String>(Arrays.asList(values)));
    }

    private static Map<String, Object> immutableMap(Map<String, Object> value) {
        return Collections.unmodifiableMap(new LinkedHashMap<String, Object>(value));
    }

    private static ConformanceSuiteLoadException failure(String code, String message) {
        return new ConformanceSuiteLoadException(code, message);
    }

    private static ConformanceSuiteLoadException failure(String code, String message, Throwable cause) {
        return new ConformanceSuiteLoadException(code, message, cause);
    }

    private interface ReferencePolicy {
        boolean accepts(String relativePath);
    }

    public static final class ConformanceSuite {
        public final Path snapshotRoot;
        public final Path contractDocument;
        public final Path semanticCatalogDocument;
        public final List<String> semanticFixtureIds;
        public final List<WorkflowCase> workflowCases;
        public final Map<String, WorkflowCase> workflowCasesById;
        public final Path artifactSchema;
        public final Path resultSchema;
        public final List<ContractCase> contractCases;
        public final Map<String, ContractCase> contractCasesById;

        private ConformanceSuite(Path snapshotRoot, SuiteIndex suiteIndex, ContractCaseIndex contractCases) {
            this.snapshotRoot = snapshotRoot;
            this.contractDocument = suiteIndex.contractDocument;
            this.semanticCatalogDocument = suiteIndex.semanticCatalog.source;
            this.semanticFixtureIds = suiteIndex.semanticCatalog.fixtureIds;
            this.workflowCases = Collections.unmodifiableList(new ArrayList<WorkflowCase>(suiteIndex.workflowCases));
            this.workflowCasesById = Collections.unmodifiableMap(new LinkedHashMap<String, WorkflowCase>(suiteIndex.workflowCasesById));
            this.artifactSchema = contractCases.artifactSchema;
            this.resultSchema = contractCases.resultSchema;
            this.contractCases = Collections.unmodifiableList(new ArrayList<ContractCase>(contractCases.contractCases));
            this.contractCasesById = Collections.unmodifiableMap(new LinkedHashMap<String, ContractCase>(contractCases.contractCasesById));
        }
    }

    public static final class WorkflowCase {
        public final String id;
        public final List<String> semanticFixtureIds;
        public final String command;
        public final Map<String, Object> parameters;
        public final Path input;
        public final Path changeRequestTemplate;
        public final Path goldenSemanticState;
        public final String expectedStatus;
        public final int expectedExitCode;
        public final NextAction expectedNextAction;
        public final List<String> expectedDiagnosticCodes;
        public final List<String> expectedDiagnosticPaths;
        public final List<String> expectedRuleIds;
        public final List<String> comparisonModes;
        public final String materializationPhase;

        private WorkflowCase(String id, List<String> semanticFixtureIds, String command, Map<String, Object> parameters,
                Path input, Path changeRequestTemplate, Path goldenSemanticState, String expectedStatus, int expectedExitCode,
                NextAction expectedNextAction, List<String> expectedDiagnosticCodes, List<String> expectedDiagnosticPaths,
                List<String> expectedRuleIds, List<String> comparisonModes, String materializationPhase) {
            this.id = id;
            this.semanticFixtureIds = semanticFixtureIds;
            this.command = command;
            this.parameters = immutableMap(parameters);
            this.input = input;
            this.changeRequestTemplate = changeRequestTemplate;
            this.goldenSemanticState = goldenSemanticState;
            this.expectedStatus = expectedStatus;
            this.expectedExitCode = expectedExitCode;
            this.expectedNextAction = expectedNextAction;
            this.expectedDiagnosticCodes = expectedDiagnosticCodes;
            this.expectedDiagnosticPaths = expectedDiagnosticPaths;
            this.expectedRuleIds = expectedRuleIds;
            this.comparisonModes = comparisonModes;
            this.materializationPhase = materializationPhase;
        }
    }

    public static final class NextAction {
        public final String action;
        public final String command;
        public final String sourceRetryability;

        private NextAction(String action, String command, String sourceRetryability) {
            this.action = action;
            this.command = command;
            this.sourceRetryability = sourceRetryability;
        }
    }

    public static final class ContractCase {
        public final String id;
        public final String validationLayer;
        public final List<ContractInput> inputs;
        public final List<Mutation> mutations;
        public final boolean expectedValid;
        public final List<String> expectedRuleIds;
        public final List<String> checkedRuleIds;

        private ContractCase(String id, String validationLayer, List<ContractInput> inputs, List<Mutation> mutations,
                boolean expectedValid, List<String> expectedRuleIds, List<String> checkedRuleIds) {
            this.id = id;
            this.validationLayer = validationLayer;
            this.inputs = Collections.unmodifiableList(new ArrayList<ContractInput>(inputs));
            this.mutations = Collections.unmodifiableList(new ArrayList<Mutation>(mutations));
            this.expectedValid = expectedValid;
            this.expectedRuleIds = expectedRuleIds;
            this.checkedRuleIds = checkedRuleIds;
        }
    }

    public static final class ContractInput {
        public final String role;
        public final Path path;

        private ContractInput(String role, Path path) {
            this.role = role;
            this.path = path;
        }
    }

    public static final class Mutation {
        public final String inputRole;
        public final String operation;
        public final String pointer;
        public final Object value;
        public final boolean hasValue;

        private Mutation(String inputRole, String operation, String pointer, Object value, boolean hasValue) {
            this.inputRole = inputRole;
            this.operation = operation;
            this.pointer = pointer;
            this.value = value;
            this.hasValue = hasValue;
        }
    }

    private static final class SuiteIndex {
        private final Path contractDocument;
        private final SemanticCatalogMaterialization semanticCatalog;
        private final List<WorkflowCase> workflowCases;
        private final Map<String, WorkflowCase> workflowCasesById;

        private SuiteIndex(Path contractDocument, SemanticCatalogMaterialization semanticCatalog,
                List<WorkflowCase> workflowCases, Map<String, WorkflowCase> workflowCasesById) {
            this.contractDocument = contractDocument;
            this.semanticCatalog = semanticCatalog;
            this.workflowCases = workflowCases;
            this.workflowCasesById = workflowCasesById;
        }
    }

    private static final class SemanticCatalogMaterialization {
        private final Path source;
        private final List<String> fixtureIds;

        private SemanticCatalogMaterialization(Path source, List<String> fixtureIds) {
            this.source = source;
            this.fixtureIds = fixtureIds;
        }
    }

    private static final class ContractCaseIndex {
        private final Path artifactSchema;
        private final Path resultSchema;
        private final List<ContractCase> contractCases;
        private final Map<String, ContractCase> contractCasesById;

        private ContractCaseIndex(Path artifactSchema, Path resultSchema, List<ContractCase> contractCases,
                Map<String, ContractCase> contractCasesById) {
            this.artifactSchema = artifactSchema;
            this.resultSchema = resultSchema;
            this.contractCases = contractCases;
            this.contractCasesById = contractCasesById;
        }
    }

    public static final class ConformanceSuiteLoadException extends IOException {
        private static final long serialVersionUID = 1L;
        public final String code;

        private ConformanceSuiteLoadException(String code, String message) {
            super(message);
            this.code = code;
        }

        private ConformanceSuiteLoadException(String code, String message, Throwable cause) {
            super(message, cause);
            this.code = code;
        }
    }
}
