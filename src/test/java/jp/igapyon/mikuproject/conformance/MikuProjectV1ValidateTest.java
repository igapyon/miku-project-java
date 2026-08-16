/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuproject.conformance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import jp.igapyon.mikuproject.coreapi.CoreApiAiJsonUtil;
import jp.igapyon.mikuproject.v1.MikuProjectV1Validate;

/** P5-C1 executable checks over the inventory-verified v1 snapshot. */
public class MikuProjectV1ValidateTest {
    private static final Path SNAPSHOT = Paths.get("vendor", "miku-project-contract", "v1.0.3");
    private static final String DIGEST_A = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
    private static final String DIGEST_B = "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb";
    private final CoreApiAiJsonUtil json = new CoreApiAiJsonUtil();

    @Test
    public void validatesTheFixedValidAndHierarchyFixturesWithSchemaValidResults() throws Exception {
        ConformanceSuiteLoader.ConformanceSuite suite = ConformanceSuiteLoader.load(SNAPSHOT);
        ConformanceSchemaRegistry schema = ConformanceSchemaRegistry.load(SNAPSHOT);
        assertValidCase(suite, schema, "CV-VALID-001");
        assertValidCase(suite, schema, "CV-HIERARCHY-VALID-001");
    }

    @Test
    public void reportsTheFixedInvalidAndUnsupportedFixturesWithoutPretendingTheyAreValid() throws Exception {
        ConformanceSuiteLoader.ConformanceSuite suite = ConformanceSuiteLoader.load(SNAPSHOT);
        ConformanceSchemaRegistry schema = ConformanceSchemaRegistry.load(SNAPSHOT);
        assertRejectedCase(suite, schema, "CV-INVALID-001");
        assertRejectedCase(suite, schema, "CV-UNSUPPORTED-001");
        assertRejectedCase(suite, schema, "CV-HIERARCHY-INVALID-PREORDER-001");
        assertRejectedCase(suite, schema, "CV-HIERARCHY-INVALID-SUMMARY-001");
    }

    @Test
    public void rejectsUsageBeforeReadingProjectInput() throws Exception {
        ConformanceSchemaRegistry schema = ConformanceSchemaRegistry.load(SNAPSHOT);
        Run run = invoke(new String[] { "validate", "--project" }, new NeverReadInput(), runtime());

        assertEquals(2, run.exitCode);
        assertSchema(schema, run.result);
        assertEquals("cli", run.result.get("command"));
        assertEquals("usage-error", run.result.get("status"));
        assertEquals("cli.missing-option", diagnosticCodes(run.result).get(0));
        assertFalse(((NeverReadInput) run.stdin).wasRead);
    }

    @Test
    public void executesTheFixedUsageCaseAsTheCorpusDefinesTheWholeCliArgv() throws Exception {
        ConformanceSuiteLoader.ConformanceSuite suite = ConformanceSuiteLoader.load(SNAPSHOT);
        ConformanceSchemaRegistry schema = ConformanceSchemaRegistry.load(SNAPSHOT);
        ConformanceSuiteLoader.WorkflowCase workflow = suite.workflowCasesById.get("CU-USAGE-001");
        assertNotNull(workflow);
        @SuppressWarnings("unchecked")
        List<String> arguments = (List<String>) workflow.parameters.get("arguments");
        String[] argv = arguments.toArray(new String[arguments.size()]);
        NeverReadInput stdin = new NeverReadInput();
        Run run = invoke(argv, stdin, runtime());

        assertEquals(workflow.expectedExitCode, run.exitCode);
        assertSchema(schema, run.result);
        assertEquals("cli", run.result.get("command"));
        assertEquals(workflow.expectedStatus, run.result.get("status"));
        assertEquals(workflow.expectedDiagnosticCodes, diagnosticCodes(run.result));
        assertTrue(diagnosticRuleIds(run.result).stream().allMatch(ruleId -> ruleId == null));
        Map<String, Object> location = diagnosticLocation(run.result, 0);
        assertEquals("option", location.get("scope"));
        assertEquals("--unknown-option", location.get("option"));
        assertFalse(stdin.wasRead);
    }

    @Test
    public void rejectsAnUnverifiedRuntimeBeforeReadingProjectInput() throws Exception {
        ConformanceSchemaRegistry schema = ConformanceSchemaRegistry.load(SNAPSHOT);
        NeverReadInput stdin = new NeverReadInput();
        Run run = invoke(new String[] { "validate", "--project", "-" }, stdin,
                new MikuProjectV1Validate.VerifiedRuntime("1.0.0", "not-a-digest", DIGEST_B));

        assertEquals(3, run.exitCode);
        assertSchema(schema, run.result);
        assertEquals("validate", run.result.get("command"));
        assertEquals("runtime-error", run.result.get("status"));
        assertEquals("runtime.manifest-invalid", diagnosticCodes(run.result).get(0));
        assertFalse(stdin.wasRead);
        assertEquals("unverified", object(run.result.get("runtime")).get("binding_status"));
    }

    @Test
    public void emitsASchemaValidRejectedResultForMalformedXmlAfterTheInputIsRead() throws Exception {
        ConformanceSchemaRegistry schema = ConformanceSchemaRegistry.load(SNAPSHOT);
        Run run = invoke(new String[] { "validate", "--project", "-" },
                new ByteArrayInputStream("<Project".getBytes(StandardCharsets.UTF_8)), runtime());

        assertEquals(1, run.exitCode);
        assertSchema(schema, run.result);
        assertEquals("xml.invalid", diagnosticCodes(run.result).get(0));
        assertEquals("--project", object(list(object(run.result.get("io")).get("inputs")).get(0)).get("option"));
    }

    @Test
    public void preservesInvalidLexicalValuesForStableSemanticDiagnostics() throws Exception {
        ConformanceSuiteLoader.ConformanceSuite suite = ConformanceSuiteLoader.load(SNAPSHOT);
        ConformanceSchemaRegistry schema = ConformanceSchemaRegistry.load(SNAPSHOT);
        String canonical = fixtureText(suite, "CV-VALID-001");

        assertRejectedXml(schema,
                canonical.replace("<ScheduleFromStart>1</ScheduleFromStart>", "<ScheduleFromStart>invalid</ScheduleFromStart>"),
                Arrays.asList("semantic.invalid"), Arrays.asList("S-I024"));
        assertRejectedXml(schema,
                canonical.replace("<IsBaseCalendar>1</IsBaseCalendar>", "<IsBaseCalendar>invalid</IsBaseCalendar>"),
                Arrays.asList("semantic.invalid"), Arrays.asList("S-I024"));
        assertRejectedXml(schema, canonical.replaceFirst("<Milestone>0</Milestone>", "<Milestone>invalid</Milestone>"),
                Arrays.asList("semantic.invalid"), Arrays.asList("S-I024"));
        assertRejectedXml(schema, canonical.replaceFirst("<ID>1</ID>", "<ID>invalid</ID>"),
                Arrays.asList("semantic.invalid"), Arrays.asList("S-I003"));
        assertRejectedXml(schema,
                canonical.replace("<Name>Miku</Name>\n      <Type>1</Type>", "<Name>Miku</Name>\n      <Type>invalid</Type>"),
                Arrays.asList("semantic.unsupported"), Arrays.asList("S-I020"));
        assertRejectedXml(schema, canonical.replaceFirst("<Duration>PT16H0M0S</Duration>",
                "<Duration>PT999999999999999999999H0M0S</Duration>"), Arrays.asList("semantic.invalid"),
                Arrays.asList("S-I009"));
    }

    @Test
    public void rejectsCyclesAndUsesTheNodeAssignmentRuleIds() throws Exception {
        ConformanceSuiteLoader.ConformanceSuite suite = ConformanceSuiteLoader.load(SNAPSHOT);
        ConformanceSchemaRegistry schema = ConformanceSchemaRegistry.load(SNAPSHOT);
        String canonical = fixtureText(suite, "CV-VALID-001");
        String cycle = canonical.replace("<PercentComplete>100</PercentComplete>",
                "<PercentComplete>100</PercentComplete><PredecessorLink><PredecessorUID>2</PredecessorUID>"
                        + "<Type>1</Type><LinkLag>0</LinkLag><LagFormat>3</LagFormat></PredecessorLink>");

        assertRejectedXml(schema, cycle, Arrays.asList("semantic.invalid"), Arrays.asList("S-I016"));
        assertRejectedXml(schema, canonical.replace("<TaskUID>2</TaskUID>", "<TaskUID>999</TaskUID>"),
                Arrays.asList("semantic.invalid"), Arrays.asList("S-I017"));
    }

    @Test
    public void matchesNodeSemanticBoundariesForMissingReferencesAndSafeIntegers() throws Exception {
        ConformanceSuiteLoader.ConformanceSuite suite = ConformanceSuiteLoader.load(SNAPSHOT);
        ConformanceSchemaRegistry schema = ConformanceSchemaRegistry.load(SNAPSHOT);
        String canonical = fixtureText(suite, "CV-VALID-001");

        assertRejectedXml(schema, canonical.replaceFirst("\\n      <PercentComplete>100</PercentComplete>", ""),
                Arrays.asList("semantic.invalid"), Arrays.asList("S-I008"),
                Arrays.asList("tasks[uid=1].percent_complete"));
        assertRejectedXml(schema, canonical.replaceFirst("<PercentComplete>100</PercentComplete>",
                "<PercentComplete>2147483648</PercentComplete>"), Arrays.asList("semantic.invalid"),
                Arrays.asList("S-I012"), Arrays.asList("tasks[uid=1].percent_complete"));
        assertRejectedXml(schema, canonical.replaceFirst("<PercentComplete>100</PercentComplete>",
                "<PercentComplete>9007199254740992</PercentComplete>"), Arrays.asList("semantic.invalid"),
                Arrays.asList("S-I013"), Arrays.asList("tasks[uid=1].percent_complete"));
        assertRejectedXml(schema, canonical.replace("<PredecessorUID>1</PredecessorUID>", ""),
                Arrays.asList("semantic.invalid"), Arrays.asList("S-I008"), Arrays.asList("dependencies[0]"));

        Run safeLargeId = invoke(new String[] { "validate", "--project", "-" }, new ByteArrayInputStream(canonical
                .replaceFirst("<ID>1</ID>", "<ID>2147483648</ID>").getBytes(StandardCharsets.UTF_8)), runtime());
        assertEquals(0, safeLargeId.exitCode);
        assertSchema(schema, safeLargeId.result);
        assertEquals("succeeded", safeLargeId.result.get("status"));
    }

    @Test
    public void selectsStableUidDiagnosticPathsForResourcesAssignmentsAndCalendars() throws Exception {
        ConformanceSuiteLoader.ConformanceSuite suite = ConformanceSuiteLoader.load(SNAPSHOT);
        ConformanceSchemaRegistry schema = ConformanceSchemaRegistry.load(SNAPSHOT);
        String canonical = fixtureText(suite, "CV-VALID-001");

        assertRejectedXml(schema, canonical.replace("<Name>Miku</Name>", "<Name>Miku</Name><ActualCost>1</ActualCost>"),
                Arrays.asList("semantic.unsupported"), Arrays.asList("S-I020"), Arrays.asList("resources[uid=1].actual_cost"));
        assertRejectedXml(schema, canonical.replace("<Work>PT16H0M0S</Work>", "<Work>PT16H0M0S</Work><ActualWork>PT0H0M0S</ActualWork>"),
                Arrays.asList("semantic.unsupported"), Arrays.asList("S-I020"), Arrays.asList("assignments[uid=1].actual_work"));
        assertRejectedXml(schema, canonical.replace("<IsBaseCalendar>1</IsBaseCalendar>",
                "<IsBaseCalendar>1</IsBaseCalendar><BaseCalendarUID>9</BaseCalendarUID>"),
                Arrays.asList("semantic.unsupported"), Arrays.asList("S-I020"), Arrays.asList("calendars[uid=1].base_calendar_uid"));
        assertRejectedXml(schema, canonical.replace("<Name>Miku</Name>\n      <Type>1</Type>",
                "<Name>Miku</Name>\n      <CalendarUID>9</CalendarUID>\n      <Type>1</Type>"),
                Arrays.asList("semantic.invalid"), Arrays.asList("S-I018"), Arrays.asList("resources[uid=1].calendar_uid"));
    }

    @Test
    public void rejectsMalformedMilestonesWithoutAnInternalError() throws Exception {
        ConformanceSuiteLoader.ConformanceSuite suite = ConformanceSuiteLoader.load(SNAPSHOT);
        ConformanceSchemaRegistry schema = ConformanceSchemaRegistry.load(SNAPSHOT);
        String canonical = fixtureText(suite, "CV-VALID-001");
        String malformed = canonical.replaceFirst("\n      <Start>2026-03-16T09:00:00</Start>", "")
                .replaceFirst("<Milestone>0</Milestone>", "<Milestone>1</Milestone>")
                .replaceFirst("<Duration>PT16H0M0S</Duration>", "<Duration>PT0H0M0S</Duration>");
        Run run = invoke(new String[] { "validate", "--project", "-" },
                new ByteArrayInputStream(malformed.getBytes(StandardCharsets.UTF_8)), runtime());

        assertEquals(1, run.exitCode);
        assertSchema(schema, run.result);
        assertEquals("rejected", run.result.get("status"));
        assertEquals(Arrays.asList("semantic.invalid", "semantic.invalid"), diagnosticCodes(run.result));
        assertEquals(Arrays.asList("S-I010", "S-I008"), diagnosticRuleIds(run.result));
    }

    @Test
    public void reservesAResultFileExclusivelyAndDoesNotOverwriteIt() throws Exception {
        ConformanceSchemaRegistry schema = ConformanceSchemaRegistry.load(SNAPSHOT);
        ConformanceSuiteLoader.ConformanceSuite suite = ConformanceSuiteLoader.load(SNAPSHOT);
        Path result = Files.createTempDirectory("miku-project-v1-result-").resolve("result.json");
        try {
            Run first = invoke(new String[] { "validate", "--project", suite.workflowCasesById.get("CV-VALID-001").input.toString(),
                    "--result", result.toString() }, new ByteArrayInputStream(new byte[0]), runtime());
            assertEquals(0, first.exitCode);
            assertEquals(0, first.stdout.size());
            Map<String, Object> persisted = object(json.parseJsonText(new String(Files.readAllBytes(result), StandardCharsets.UTF_8)));
            assertSchema(schema, persisted);
            Run second = invoke(new String[] { "validate", "--project", suite.workflowCasesById.get("CV-VALID-001").input.toString(),
                    "--result", result.toString() }, new ByteArrayInputStream(new byte[0]), runtime());
            assertEquals(1, second.exitCode);
            assertSchema(schema, second.result);
            assertEquals("io.result-path-exists", diagnosticCodes(second.result).get(0));
        } finally {
            Files.deleteIfExists(result);
            Files.deleteIfExists(result.getParent());
        }
    }

    private void assertValidCase(ConformanceSuiteLoader.ConformanceSuite suite, ConformanceSchemaRegistry schema, String caseId)
            throws Exception {
        ConformanceSuiteLoader.WorkflowCase workflow = suite.workflowCasesById.get(caseId);
        assertNotNull(workflow);
        Run run = invoke(new String[] { "validate", "--project", workflow.input.toString() }, new ByteArrayInputStream(new byte[0]), runtime());

        assertEquals(workflow.expectedExitCode, run.exitCode);
        assertSchema(schema, run.result);
        assertEquals(workflow.expectedStatus, run.result.get("status"));
        assertEquals(Boolean.TRUE, object(object(run.result.get("data")).get("validation")).get("valid"));
        Object golden = schema.parseJson(workflow.goldenSemanticState);
        assertEquals(ConformanceCanonicalJson.sha256SemanticStateDigest(golden),
                object(object(run.result.get("data")).get("validation")).get("state_digest"));
        Run repeated = invoke(new String[] { "validate", "--project", workflow.input.toString() },
                new ByteArrayInputStream(new byte[0]), runtime());
        assertArrayEquals(run.stdout.toByteArray(), repeated.stdout.toByteArray(),
                "byte-same-runtime output must be deterministic for a fixed binding and input path");
    }

    private void assertRejectedCase(ConformanceSuiteLoader.ConformanceSuite suite, ConformanceSchemaRegistry schema, String caseId)
            throws Exception {
        ConformanceSuiteLoader.WorkflowCase workflow = suite.workflowCasesById.get(caseId);
        assertNotNull(workflow);
        Run run = invoke(new String[] { "validate", "--project", workflow.input.toString() }, new ByteArrayInputStream(new byte[0]), runtime());

        assertEquals(workflow.expectedExitCode, run.exitCode);
        assertSchema(schema, run.result);
        assertEquals(workflow.expectedStatus, run.result.get("status"));
        assertEquals(Boolean.FALSE, object(object(run.result.get("data")).get("validation")).get("valid"));
        assertEquals(workflow.expectedDiagnosticCodes, diagnosticCodes(run.result));
        assertEquals(workflow.expectedRuleIds, diagnosticRuleIds(run.result));
    }

    private void assertRejectedXml(ConformanceSchemaRegistry schema, String xml, List<String> expectedCodes,
            List<String> expectedRuleIds) {
        assertRejectedXml(schema, xml, expectedCodes, expectedRuleIds, null);
    }

    private void assertRejectedXml(ConformanceSchemaRegistry schema, String xml, List<String> expectedCodes,
            List<String> expectedRuleIds, List<String> expectedPaths) {
        Run run = invoke(new String[] { "validate", "--project", "-" },
                new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)), runtime());
        assertEquals(1, run.exitCode);
        assertSchema(schema, run.result);
        assertEquals("rejected", run.result.get("status"));
        assertEquals(expectedCodes, diagnosticCodes(run.result));
        assertEquals(expectedRuleIds, diagnosticRuleIds(run.result));
        if (expectedPaths != null) {
            assertEquals(expectedPaths, diagnosticPaths(run.result));
        }
    }

    private static String fixtureText(ConformanceSuiteLoader.ConformanceSuite suite, String caseId) throws IOException {
        return new String(Files.readAllBytes(suite.workflowCasesById.get(caseId).input), StandardCharsets.UTF_8);
    }

    private Run invoke(String[] argv, InputStream stdin, MikuProjectV1Validate.VerifiedRuntime runtime) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        int exitCode = MikuProjectV1Validate.run(argv, stdin, output, new ByteArrayOutputStream(), runtime);
        Map<String, Object> result = output.size() == 0 ? null
                : object(json.parseJsonText(new String(output.toByteArray(), StandardCharsets.UTF_8)));
        return new Run(exitCode, result, stdin, output);
    }

    private static void assertSchema(ConformanceSchemaRegistry schema, Map<String, Object> result) {
        ConformanceSchemaRegistry.Validation validation = schema.validateResult(result);
        assertTrue(validation.valid, () -> "result schema violations: " + describe(validation.violations));
    }

    private static String describe(List<ConformanceSchemaRegistry.Violation> violations) {
        List<String> values = new ArrayList<String>();
        for (ConformanceSchemaRegistry.Violation violation : violations) {
            values.add(violation.instancePointer + " " + violation.keyword + " " + violation.message);
        }
        return values.toString();
    }

    private static List<String> diagnosticCodes(Map<String, Object> result) {
        List<String> values = new ArrayList<String>();
        for (Object diagnostic : list(result.get("diagnostics"))) {
            values.add((String) object(diagnostic).get("code"));
        }
        return values;
    }

    private static List<String> diagnosticRuleIds(Map<String, Object> result) {
        List<String> values = new ArrayList<String>();
        for (Object diagnostic : list(result.get("diagnostics"))) {
            values.add((String) object(object(diagnostic).get("location")).get("rule_id"));
        }
        return values;
    }

    private static List<String> diagnosticPaths(Map<String, Object> result) {
        List<String> values = new ArrayList<String>();
        for (Object diagnostic : list(result.get("diagnostics"))) {
            values.add((String) diagnosticLocation(object(diagnostic)).get("path"));
        }
        return values;
    }

    private static Map<String, Object> diagnosticLocation(Map<String, Object> result, int index) {
        return diagnosticLocation(object(list(result.get("diagnostics")).get(index)));
    }

    private static Map<String, Object> diagnosticLocation(Map<String, Object> diagnostic) {
        return object(diagnostic.get("location"));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> object(Object value) {
        return (Map<String, Object>) value;
    }

    @SuppressWarnings("unchecked")
    private static List<Object> list(Object value) {
        return (List<Object>) value;
    }

    private static MikuProjectV1Validate.VerifiedRuntime runtime() {
        return new MikuProjectV1Validate.VerifiedRuntime("1.0.0", DIGEST_A, DIGEST_B);
    }

    private static final class Run {
        final int exitCode;
        final Map<String, Object> result;
        final InputStream stdin;
        final ByteArrayOutputStream stdout;

        Run(int exitCode, Map<String, Object> result, InputStream stdin, ByteArrayOutputStream stdout) {
            this.exitCode = exitCode;
            this.result = result;
            this.stdin = stdin;
            this.stdout = stdout;
        }
    }

    private static final class NeverReadInput extends InputStream {
        boolean wasRead;

        @Override
        public int read() throws IOException {
            wasRead = true;
            throw new IOException("input must not be read");
        }
    }
}
