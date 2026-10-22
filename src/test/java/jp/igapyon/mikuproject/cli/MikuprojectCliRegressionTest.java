/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuproject.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.ResourceLock;

import jp.igapyon.mikuproject.coreapi.CoreApiAiJsonUtil;

@ResourceLock("java.lang.System.in")
public class MikuprojectCliRegressionTest {
    @TempDir
    Path directory;

    private final CoreApiAiJsonUtil json = new CoreApiAiJsonUtil();

    @Test
    public void propagatesUnknownWorkbookWarningsThroughTextAndJsonCommands() throws IOException {
        Path unknown = fixture("UNKNOWN.json");
        String[][] commands = {
                { "ai", "export", "task-edit", "--select", "first-task" },
                { "state", "summarize" },
                { "export", "xml" },
                { "report", "wbs-markdown" }
        };
        for (String format : Arrays.asList("text", "json")) {
            for (String[] command : commands) {
                String[] args = append(command, "--in", unknown.toString(), "--diagnostics", format);
                Run result = run(args);
                assertEquals(0, result.exitCode, Arrays.toString(command) + " " + format);
                assertFalse(result.stdout.length == 0, Arrays.toString(command) + " primary output");
                if ("json".equals(format)) {
                    Map<String, Object> diagnostics = object(result.stderrText());
                    assertEquals("warning", diagnostics.get("status"), Arrays.toString(command));
                    assertEquals(Integer.valueOf(2), diagnostics.get("warning_count"), Arrays.toString(command));
                    assertWarningMessages(diagnostics, false);
                } else {
                    assertEquals(2, count(result.stderrText(), "[warning]"), Arrays.toString(command));
                    assertTrue(result.stderrText().contains("Tasks[0].AgentNote"), Arrays.toString(command));
                    assertTrue(result.stderrText().contains("CustomData"), Arrays.toString(command));
                }
            }
        }
    }

    @Test
    public void combinesWorkbookAndPatchWarningsForValidationAndApplication() throws IOException {
        Path unknown = fixture("UNKNOWN.json");
        Path patchOk = fixture("PATCH_OK.json");
        Path patchUnknown = directory.resolve("PATCH_UNKNOWN.json");
        Files.write(patchUnknown, "{\"operations\":[{\"op\":\"unknown_op\"}]}".getBytes(StandardCharsets.UTF_8));
        Path[] patches = { patchOk, patchUnknown };
        int[] expectedWarnings = { 2, 3 };
        String[] expectedOperations = { "update_project", "unknown_op" };

        for (String format : Arrays.asList("text", "json")) {
            for (int patchIndex = 0; patchIndex < patches.length; patchIndex++) {
                for (String command : Arrays.asList("validate", "apply")) {
                    String[] args = "validate".equals(command)
                            ? new String[] { "ai", "validate-patch", "--state", unknown.toString(), "--in", patches[patchIndex].toString(),
                                    "--diagnostics", format }
                            : new String[] { "state", "apply-patch", "--state", unknown.toString(), "--in", patches[patchIndex].toString(),
                                    "--diagnostics", format };
                    Run result = run(args);
                    assertEquals(0, result.exitCode, command + " " + format + " " + expectedOperations[patchIndex]);
                    assertFalse(result.stdout.length == 0, command + " primary output");
                    if ("json".equals(format)) {
                        Map<String, Object> report = "validate".equals(command) ? object(result.stdoutText()) : object(result.stderrText());
                        assertEquals("warning", report.get("status"), command + " status");
                        assertEquals(Integer.valueOf(expectedWarnings[patchIndex]), report.get("warning_count"), command + " warning count");
                        assertWarningMessages(report, patchIndex == 1);
                    } else {
                        String diagnosticText = "validate".equals(command) ? result.stdoutText() : result.stderrText();
                        assertTrue(diagnosticText.contains("warnings=" + expectedWarnings[patchIndex]), command + " warning summary");
                        assertEquals(expectedWarnings[patchIndex], count(diagnosticText, "[warning]"), command + " warning lines");
                    }
                }
            }
        }
    }

    @Test
    public void reportsInvalidPatchKindAndUsageFailureOnTheirExpectedChannels() throws IOException {
        Path state = fixture("BASE.json");
        Path wrongKind = write("wrong-kind-patch.json", draftDocument("not a patch"));

        for (String format : Arrays.asList("text", "json")) {
            Run result = run(new String[] { "ai", "validate-patch", "--state", state.toString(), "--in", wrongKind.toString(),
                    "--diagnostics", format });

            assertEquals(1, result.exitCode, format);
            assertEquals("", result.stderrText(), format);
            if ("json".equals(format)) {
                Map<String, Object> report = object(result.stdoutText());
                assertEquals(Boolean.FALSE, report.get("ok"));
                assertEquals("error", report.get("status"));
                assertEquals(Integer.valueOf(1), report.get("exit_code"));
                assertEquals("invalid_patch_kind", report.get("error_code"));
                assertEquals("project_draft_view", ((Map<?, ?>) report.get("error_details")).get("actual_kind"));
                assertEquals(Integer.valueOf(1), report.get("error_count"));
            } else {
                assertTrue(result.stdoutText().contains("ok=false status=error"));
                assertTrue(result.stdoutText().contains("[error]"));
            }
        }

        Run usage = run(new String[] { "ai", "validate-patch", "--in", fixture("PATCH_OK.json").toString(), "--diagnostics", "json" });
        assertEquals(2, usage.exitCode);
        assertEquals("", usage.stdoutText());
        assertEquals("missing_state_option", object(usage.stderrText()).get("error_code"));
    }

    @Test
    public void rejectsEmptyStateAsUsageError() throws IOException {
        Run result = run(new String[] { "ai", "validate-patch", "--state", "", "--in", fixture("PATCH_OK.json").toString(),
                "--diagnostics", "json" });

        assertEquals(2, result.exitCode);
        assertEquals("", result.stdoutText());
        Map<String, Object> diagnostics = object(result.stderrText());
        assertEquals("missing_state_option", diagnostics.get("error_code"));
        assertEquals("--state", diagnostics.get("option"));
    }

    @Test
    public void reportsMalformedPatchAsValidationFailureAndKeepsBaseWarnings() throws IOException {
        Path state = fixture("UNKNOWN.json");
        Path malformed = write("malformed-patch.json", "{\"operations\":[1]}\n");

        for (String format : Arrays.asList("text", "json")) {
            Run result = run(new String[] { "ai", "validate-patch", "--state", state.toString(), "--in", malformed.toString(),
                    "--diagnostics", format });

            assertEquals(1, result.exitCode, format);
            assertEquals("", result.stderrText(), format);
            if ("json".equals(format)) {
                Map<String, Object> report = object(result.stdoutText());
                assertEquals(Boolean.FALSE, report.get("ok"));
                assertEquals("error", report.get("status"));
                assertEquals("patch_validation_failed", report.get("error_code"));
                assertEquals(Integer.valueOf(2), report.get("warning_count"));
                assertEquals(Integer.valueOf(1), report.get("error_count"));
                assertWarningMessages(report, false);
                assertTrue(list(report.get("errors")).get(0).toString().contains("operations[0]"));
            } else {
                assertTrue(result.stdoutText().contains("ok=false status=error warnings=2 errors=1 changes=0"));
                assertTrue(result.stdoutText().contains("[error] operations[0] がオブジェクトではありません"));
                assertEquals(2, count(result.stdoutText(), "[warning]"));
            }
        }
    }

    @Test
    public void reportsMalformedPatchJsonAndUnreadableInputAsValidationReports() throws IOException {
        Path state = fixture("BASE.json");
        Path malformed = write("malformed-json-patch.json", "{\n");
        Path missing = directory.resolve("missing-patch.json");

        Run invalidJson = run(new String[] { "ai", "validate-patch", "--state", state.toString(), "--in", malformed.toString(),
                "--diagnostics", "json" });
        assertEquals(1, invalidJson.exitCode);
        assertEquals("", invalidJson.stderrText());
        Map<String, Object> invalidJsonReport = object(invalidJson.stdoutText());
        assertEquals("invalid_json_input", invalidJsonReport.get("error_code"));
        assertEquals("ai validate-patch --in", ((Map<?, ?>) invalidJsonReport.get("error_details")).get("context"));
        assertEquals(Integer.valueOf(1), invalidJsonReport.get("error_count"));

        Run unreadable = run(new String[] { "ai", "validate-patch", "--state", state.toString(), "--in", missing.toString(),
                "--diagnostics", "json" });
        assertEquals(1, unreadable.exitCode);
        assertEquals("", unreadable.stderrText());
        Map<String, Object> unreadableReport = object(unreadable.stdoutText());
        assertEquals("patch_validation_failed", unreadableReport.get("error_code"));
        assertEquals(Integer.valueOf(1), unreadableReport.get("error_count"));
    }

    @Test
    public void rejectsBinaryInputCombinationsBeforeReadingAndReportsRequiredOptions() throws IOException {
        Run bothSources = run(new String[] { "import", "xlsx", "--in", directory.resolve("unused.xlsx").toString(),
                "--in-base64", "-", "--diagnostics", "json" });
        assertEquals(2, bothSources.exitCode);
        assertEquals("", bothSources.stdoutText());
        Map<String, Object> bothDiagnostics = object(bothSources.stderrText());
        assertEquals("multiple_input_sources", bothDiagnostics.get("error_code"));
        assertEquals(Arrays.asList("--in", "--in-base64"), ((Map<?, ?>) bothDiagnostics.get("error_details")).get("options"));
        assertEquals(2, list(((Map<?, ?>) bothDiagnostics.get("io")).get("inputs")).size());

        CountingFailingInputStream stdin = new CountingFailingInputStream();
        Run rawStdin = runWithStdin(new String[] { "import", "xlsx", "--in", "-", "--diagnostics", "json" }, stdin);
        assertEquals(2, rawStdin.exitCode);
        assertEquals("binary_stdin_requires_base64", object(rawStdin.stderrText()).get("error_code"));
        assertEquals(0, stdin.readCount);

        Run missing = run(new String[] { "import", "xlsx", "--diagnostics", "json" });
        assertEquals(2, missing.exitCode);
        Map<String, Object> missingDiagnostics = object(missing.stderrText());
        assertEquals("missing_binary_input", missingDiagnostics.get("error_code"));
        assertEquals(Arrays.asList("--in", "--in-base64"),
                ((Map<?, ?>) missingDiagnostics.get("error_details")).get("required_options"));
    }

    @Test
    public void rejectsEmptyInvalidAndUnpaddedBase64BeforeXlsxDecoding() throws IOException {
        String[] contents = { "", "not base64\n", "YWI\n" };
        for (int index = 0; index < contents.length; index++) {
            Path encoded = write("invalid-base64-" + index + ".txt", contents[index]);
            Run result = run(new String[] { "import", "xlsx", "--in-base64", encoded.toString(), "--diagnostics", "json" });

            assertEquals(2, result.exitCode, "case " + index);
            assertEquals("", result.stdoutText(), "case " + index);
            Map<String, Object> diagnostics = object(result.stderrText());
            assertEquals("invalid_base64_input", diagnostics.get("error_code"), "case " + index);
            assertEquals("usage_error", diagnostics.get("error_type"), "case " + index);
            assertEquals("import xlsx", ((Map<?, ?>) diagnostics.get("error_details")).get("context"), "case " + index);
            Map<?, ?> io = (Map<?, ?>) diagnostics.get("io");
            Map<?, ?> input = (Map<?, ?>) list(io.get("inputs")).get(0);
            assertEquals("--in-base64", input.get("option"), "case " + index);
            assertEquals("file_base64", input.get("mode"), "case " + index);
        }
    }

    @Test
    public void rejectsBinaryOutputTargetsWithNodeCodesAndRequiredOptionDetails() throws IOException {
        String missingWorkbook = directory.resolve("must-not-read.json").toString();
        Path output = directory.resolve("must-not-write.xlsx");

        Run bothTargets = run(new String[] { "export", "xlsx", "--in", missingWorkbook, "--out", output.toString(), "--out-base64", "-",
                "--diagnostics", "json" });
        assertEquals(2, bothTargets.exitCode);
        Map<String, Object> bothDiagnostics = object(bothTargets.stderrText());
        assertEquals("multiple_output_targets", bothDiagnostics.get("error_code"));
        assertEquals(Arrays.asList("--out", "--out-base64"),
                ((Map<?, ?>) bothDiagnostics.get("error_details")).get("options"));
        assertEquals("stdout_base64", ((Map<?, ?>) ((Map<?, ?>) bothDiagnostics.get("io")).get("output")).get("mode"));
        assertFalse(Files.exists(output));

        String base64Path = directory.resolve("base64.txt").toString();
        Run invalidBase64Target = run(new String[] { "export", "xlsx", "--in", missingWorkbook, "--out-base64", base64Path,
                "--diagnostics", "json" });
        assertEquals(2, invalidBase64Target.exitCode);
        Map<String, Object> invalidTargetDiagnostics = object(invalidBase64Target.stderrText());
        assertEquals("invalid_base64_output_target", invalidTargetDiagnostics.get("error_code"));
        assertEquals("-", ((Map<?, ?>) invalidTargetDiagnostics.get("error_details")).get("expected"));
        assertEquals("file_base64", ((Map<?, ?>) ((Map<?, ?>) invalidTargetDiagnostics.get("io")).get("output")).get("mode"));

        Run missingOutput = run(new String[] { "report", "all", "--in", missingWorkbook, "--diagnostics", "json" });
        assertEquals(2, missingOutput.exitCode);
        Map<String, Object> missingOutputDiagnostics = object(missingOutput.stderrText());
        assertEquals("binary_stdout_not_supported", missingOutputDiagnostics.get("error_code"));
        Map<?, ?> details = (Map<?, ?>) missingOutputDiagnostics.get("error_details");
        assertEquals("--out", details.get("option"));
        assertEquals("report all", details.get("command"));
        assertEquals("file_or_base64_stdout", details.get("expected"));
        assertEquals("stdout", ((Map<?, ?>) ((Map<?, ?>) missingOutputDiagnostics.get("io")).get("output")).get("mode"));
    }

    @Test
    public void writesJapaneseCliDataAndDiagnosticsAsUtf8WithLf() throws IOException, InterruptedException {
        Path state = japaneseState();
        String[] stdoutArgs = { "state", "summarize", "--in", state.toString(), "--diagnostics", "json" };
        Run stdout = run(stdoutArgs);
        assertEquals(0, stdout.exitCode);
        assertUtf8Lf(stdout.stdout);
        assertUtf8Lf(stdout.stderr);
        assertTrue(stdout.stdoutText().contains("日本語プロジェクト"));
        assertEquals("日本語プロジェクト", ((Map<?, ?>) object(stdout.stdoutText()).get("project")).get("name"));
        assertTrue(stdout.stderrText().contains("日本語プロジェクト"));

        Path file = directory.resolve("japanese-summary.json");
        Run fileOutput = run(new String[] { "state", "summarize", "--in", state.toString(), "--out", file.toString() });
        assertEquals(0, fileOutput.exitCode);
        assertEquals("", fileOutput.stdoutText());
        assertUtf8Lf(Files.readAllBytes(file));
        assertTrue(new String(Files.readAllBytes(file), StandardCharsets.UTF_8).contains("日本語プロジェクト"));

        Run alternateDefault = runMainWithEncoding(stdoutArgs, "ISO-8859-1");
        assertEquals(0, alternateDefault.exitCode);
        assertUtf8Lf(alternateDefault.stdout);
        assertUtf8Lf(alternateDefault.stderr);
        assertTrue(alternateDefault.stdoutText().contains("日本語プロジェクト"));
        assertTrue(alternateDefault.stderrText().contains("日本語プロジェクト"));
    }

    @Test
    public void keepsWarningFreePrimaryOutputAndSuccessDiagnostics() throws IOException {
        Path base = fixture("BASE.json");
        Run result = run(new String[] { "state", "summarize", "--in", base.toString(), "--diagnostics", "json" });

        assertEquals(0, result.exitCode);
        assertFalse(result.stdout.length == 0);
        Map<String, Object> diagnostics = object(result.stderrText());
        assertEquals("success", diagnostics.get("status"));
        assertEquals(Integer.valueOf(0), diagnostics.get("warning_count"));
        assertTrue(list(diagnostics.get("warnings")).isEmpty());
    }

    @Test
    public void reportsUndetectedKindAsProcessingErrorWithoutPrimaryOutput() throws IOException {
        Path indeterminate = write("indeterminate.json", "{}\n");
        for (String format : Arrays.asList("text", "json")) {
            Path output = directory.resolve("undetected-kind-" + format + ".txt");
            Run result = run(new String[] { "ai", "detect-kind", "--in", indeterminate.toString(), "--out", output.toString(),
                    "--diagnostics", format });

            assertEquals(1, result.exitCode, format);
            assertEquals("", result.stdoutText(), format);
            assertFalse(Files.exists(output), format);
            if ("json".equals(format)) {
                Map<String, Object> diagnostics = object(result.stderrText());
                assertEquals(Boolean.FALSE, diagnostics.get("ok"));
                assertEquals("error", diagnostics.get("status"));
                assertEquals(Integer.valueOf(1), diagnostics.get("exit_code"));
                assertEquals("document_kind_not_detected", diagnostics.get("error_code"));
                assertEquals(Integer.valueOf(1), diagnostics.get("error_count"));
            } else {
                assertTrue(result.stderrText().contains("入力 JSON の kind を判定できませんでした"));
            }
        }
    }

    @Test
    public void keepsDetectedKindTextAndJsonOutputs() throws IOException {
        Path patch = fixture("PATCH_OK.json");
        for (String format : Arrays.asList("text", "json")) {
            Run result = run(new String[] { "ai", "detect-kind", "--in", patch.toString(), "--diagnostics", format });

            assertEquals(0, result.exitCode, format);
            assertEquals("patch_json\n", result.stdoutText(), format);
            if ("json".equals(format)) {
                Map<String, Object> diagnostics = object(result.stderrText());
                assertEquals(Boolean.TRUE, diagnostics.get("ok"));
                assertEquals("success", diagnostics.get("status"));
                assertEquals("patch_json", diagnostics.get("detected_kind"));
            } else {
                assertEquals("", result.stderrText(), format);
            }
        }
    }

    @Test
    public void emitsOnlyErrorDiagnosticsWhenTextAndBinaryOutputsTargetDirectories() throws IOException {
        Path state = NodeJavaLegacyCases.writeHierarchyState(directory);
        byte[] originalState = Files.readAllBytes(state);
        Path textTarget = directory.resolve("text-output-directory");
        Path binaryTarget = directory.resolve("binary-output-directory");
        Files.createDirectory(textTarget);
        Files.createDirectory(binaryTarget);

        Run text = run(new String[] { "report", "wbs-markdown", "--in", state.toString(), "--out", textTarget.toString(),
                "--diagnostics", "json" });
        assertSaveFailure(text, state, originalState);

        Run binary = run(new String[] { "report", "wbs-xlsx", "--in", state.toString(), "--out", binaryTarget.toString(),
                "--diagnostics", "json" });
        assertSaveFailure(binary, state, originalState);
    }

    @Test
    public void rejectsMissingValuesExtraPositionalsAndUnknownOptionsBeforeIo() throws IOException {
        Run missingValue = run(new String[] { "ai", "spec", "--out", "--diagnostics", "json" });
        assertEquals(2, missingValue.exitCode);
        assertEquals("", missingValue.stdoutText());
        Map<String, Object> missingDiagnostics = object(missingValue.stderrText());
        assertEquals("missing_option_value", missingDiagnostics.get("error_code"));
        assertEquals("--out", missingDiagnostics.get("option"));

        Path repeatedOptionOutput = directory.resolve("repeated-option-spec.md");
        Run repeatedMissingValue = run(new String[] { "ai", "spec", "--out", "--out", repeatedOptionOutput.toString(), "--diagnostics", "json" });
        assertEquals(2, repeatedMissingValue.exitCode);
        assertEquals("", repeatedMissingValue.stdoutText());
        assertEquals("missing_option_value", object(repeatedMissingValue.stderrText()).get("error_code"));
        assertFalse(Files.exists(repeatedOptionOutput));

        Run extraArgument = run(new String[] { "ai", "spec", "extra", "--diagnostics", "json" });
        assertEquals(2, extraArgument.exitCode);
        assertEquals("", extraArgument.stdoutText());
        Map<String, Object> extraDiagnostics = object(extraArgument.stderrText());
        assertEquals("unsupported_command", extraDiagnostics.get("error_code"));
        assertEquals("ai spec extra", extraDiagnostics.get("command"));

        Path protectedInputAndOutput = directory.resolve("protected.json");
        byte[] original = "unchanged input bytes".getBytes(StandardCharsets.UTF_8);
        Files.write(protectedInputAndOutput, original);
        Run unknownOption = run(new String[] { "state", "summarize", "--in", protectedInputAndOutput.toString(), "--out",
                protectedInputAndOutput.toString(), "--mystery", "value", "--diagnostics", "json" });
        assertEquals(2, unknownOption.exitCode);
        assertEquals("", unknownOption.stdoutText());
        Map<String, Object> unknownDiagnostics = object(unknownOption.stderrText());
        assertEquals("unknown_option", unknownDiagnostics.get("error_code"));
        assertEquals("--mystery", unknownDiagnostics.get("option"));
        assertTrue(Arrays.equals(original, Files.readAllBytes(protectedInputAndOutput)));
    }

    @Test
    public void keepsTheLastValueForRepeatedOptions() throws IOException {
        Path firstOutput = directory.resolve("first-spec.md");
        Path lastOutput = directory.resolve("last-spec.md");
        Run spec = run(new String[] { "ai", "spec", "--out", firstOutput.toString(), "--out", lastOutput.toString() });

        assertEquals(0, spec.exitCode);
        assertEquals("", spec.stdoutText());
        assertFalse(Files.exists(firstOutput));
        assertTrue(Files.isRegularFile(lastOutput));
        assertTrue(Files.size(lastOutput) > 0);

        Path base = fixture("BASE.json");
        Run diagnostics = run(new String[] { "state", "summarize", "--in", base.toString(), "--diagnostics", "text", "--diagnostics", "json" });
        assertEquals(0, diagnostics.exitCode);
        assertEquals("success", object(diagnostics.stderrText()).get("status"));
    }

    @Test
    public void rejectsMultipleStdinSourcesBeforeReadingOrWriting() throws IOException {
        Path diffOutput = directory.resolve("diff.json");
        assertMultipleStdinRejected(new String[] { "state", "diff", "--before", "-", "--after", "-", "--out",
                diffOutput.toString(), "--diagnostics", "json" }, diffOutput);

        Path validationOutput = directory.resolve("validation.json");
        assertMultipleStdinRejected(new String[] { "ai", "validate-patch", "--state", "-", "--in", "-", "--out",
                validationOutput.toString(), "--diagnostics", "json" }, validationOutput);

        Path applicationOutput = directory.resolve("applied.json");
        assertMultipleStdinRejected(new String[] { "state", "apply-patch", "--state", "-", "--out",
                applicationOutput.toString(), "--diagnostics", "json" }, applicationOutput);
    }

    @Test
    public void acceptsOneImplicitStdinSource() throws IOException {
        Run result = runWithStdin(new String[] { "state", "summarize", "--diagnostics", "json" },
                new ByteArrayInputStream(NodeJavaLegacyCases.fixture("BASE.json")));

        assertEquals(0, result.exitCode);
        assertFalse(result.stdout.length == 0);
        Map<String, Object> diagnostics = object(result.stderrText());
        assertEquals("success", diagnostics.get("status"));
        assertEquals(Integer.valueOf(0), diagnostics.get("warning_count"));
    }

    @Test
    public void acceptsMissingZeroAndPositiveMaxDepthValues() throws IOException {
        Path state = NodeJavaLegacyCases.writeHierarchyState(directory);
        String[] values = { null, "0", "2", "100", "000", "00100", repeat('0', 200) + "100" };
        for (String value : values) {
            Path output = directory.resolve("phase-detail-" + (value == null ? "default" : value) + ".json");
            Run result = run(phaseArgs(state, output, value));
            assertEquals(0, result.exitCode, "max-depth=" + value);
            Map<String, Object> phase = object(new String(Files.readAllBytes(output), StandardCharsets.UTF_8));
            Map<?, ?> scope = (Map<?, ?>) phase.get("scope");
            assertEquals(value == null ? null : Integer.valueOf(value), scope.get("max_depth"), "max-depth=" + value);
        }
    }

    @Test
    public void rejectsInvalidMaxDepthLexemesAsUsageErrors() throws IOException {
        Path state = NodeJavaLegacyCases.writeHierarchyState(directory);
        for (String value : Arrays.asList("-1", "1.5", "1e2", "+1", " 1 ", "abc")) {
            Path output = directory.resolve("phase-detail-invalid-" + value + ".json");
            Run result = run(phaseArgs(state, output, value));
            assertEquals(2, result.exitCode, value);
            assertEquals("", result.stdoutText(), value);
            Map<String, Object> diagnostics = object(result.stderrText());
            assertEquals("invalid_integer_option", diagnostics.get("error_code"), value);
            assertEquals("--max-depth", diagnostics.get("option"), value);
            assertFalse(Files.exists(output), value);
        }
    }

    @Test
    public void rejectsOutOfRangeAndVeryLongMaxDepthValuesAsUsageErrors() throws IOException {
        Path state = NodeJavaLegacyCases.writeHierarchyState(directory);
        byte[] originalState = Files.readAllBytes(state);
        for (String value : Arrays.asList("101", "2147483647", "2147483648", "9007199254740991",
                "9007199254740992", repeat('9', 200))) {
            Path output = directory.resolve("phase-detail-out-of-range.json");
            Files.write(output, "sentinel\n".getBytes(StandardCharsets.UTF_8));
            byte[] originalOutput = Files.readAllBytes(output);
            Run java = run(phaseArgs(state, output, value));

            assertEquals(2, java.exitCode, value);
            assertEquals("", java.stdoutText(), value);
            Map<String, Object> diagnostics = object(java.stderrText());
            assertEquals("invalid_integer_option", diagnostics.get("error_code"), value);
            assertEquals("--max-depth", diagnostics.get("option"), value);
            assertTrue(Arrays.equals(originalOutput, Files.readAllBytes(output)), value);
        }
        assertTrue(Arrays.equals(originalState, Files.readAllBytes(state)));
    }

    @Test
    public void parsesPlainFencedAndLastFencedDraftDocuments() throws IOException {
        Path plain = write("draft-plain.json", draftDocument("Plain draft"));
        Path fenced = write("draft-fenced.md", "Draft notes before\n```json\n" + draftDocument("Fenced draft")
                + "\n```\nDraft notes after\n");
        Path twoFences = write("draft-two-fences.md", "```json\n" + draftDocument("Discarded draft") + "\n```\n"
                + "Between the examples\n```json\n" + draftDocument("Last fenced draft") + "\n```\n");
        Path[] inputs = { plain, fenced, twoFences };
        String[] expectedNames = { "Plain draft", "Fenced draft", "Last fenced draft" };

        for (int index = 0; index < inputs.length; index++) {
            Path output = directory.resolve("draft-output-" + index + ".json");
            Run result = run(new String[] { "state", "from-draft", "--in", inputs[index].toString(), "--out", output.toString() });
            assertEquals(0, result.exitCode, expectedNames[index]);
            assertEquals("", result.stdoutText());
            assertEquals(expectedNames[index], projectName(output));
        }
    }

    @Test
    public void parsesPlainFencedAndLastFencedPatchDocumentsOnlyOnAiPatchPaths() throws IOException {
        Path state = NodeJavaLegacyCases.writeHierarchyState(directory);
        Path plain = fixture("PATCH_OK.json");
        String fencedPatch = "Patch explanation\n```json\n" + patchDocument("Fenced patch") + "\n```\nUse this patch\n";
        Path fenced = write("patch-fenced.md", fencedPatch);
        Path twoFences = write("patch-two-fences.md", "```json\n" + patchDocument("Discarded patch") + "\n```\n"
                + "The later block is authoritative\n```json\n" + patchDocument("Last fenced patch") + "\n```\n");
        Path[] inputs = { plain, fenced, twoFences };
        String[] expectedNames = { "Updated project", "Fenced patch", "Last fenced patch" };

        for (int index = 0; index < inputs.length; index++) {
            Path output = directory.resolve("patch-output-" + index + ".json");
            Run application = run(new String[] { "state", "apply-patch", "--state", state.toString(), "--in", inputs[index].toString(),
                    "--out", output.toString(), "--diagnostics", "json" });
            assertEquals(0, application.exitCode, "apply " + expectedNames[index]);
            assertEquals(expectedNames[index], projectName(output));

            Run validation = run(new String[] { "ai", "validate-patch", "--state", state.toString(), "--in", inputs[index].toString(),
                    "--diagnostics", "json" });
            assertEquals(0, validation.exitCode, "validate " + expectedNames[index]);
            Map<String, Object> report = object(validation.stdoutText());
            Map<?, ?> changes = (Map<?, ?>) report.get("changes_summary");
            assertEquals(Integer.valueOf(1), changes.get("total_changes"), "validate " + expectedNames[index]);
        }
    }

    @Test
    public void keepsWorkbookJsonImportStrictAboutFencedInput() throws IOException {
        Path base = fixture("BASE.json");
        String source = new String(Files.readAllBytes(base), StandardCharsets.UTF_8);
        Path fencedWorkbook = write("fenced-workbook.md", "```json\n" + source + "\n```\n");
        Path output = directory.resolve("must-not-create-workbook.json");

        Run result = run(new String[] { "state", "import", "--in", fencedWorkbook.toString(), "--out", output.toString() });

        assertEquals(1, result.exitCode);
        assertEquals("", result.stdoutText());
        assertFalse(Files.exists(output));
        assertTrue(result.stderrText().contains("command failed"));
    }

    private Path fixture(String name) throws IOException {
        Path result = directory.resolve(name);
        Files.write(result, NodeJavaLegacyCases.fixture(name));
        return result;
    }

    private Path write(String name, String content) throws IOException {
        Path result = directory.resolve(name);
        Files.write(result, content.getBytes(StandardCharsets.UTF_8));
        return result;
    }

    private Path japaneseState() throws IOException {
        String source = new String(NodeJavaLegacyCases.fixture("BASE.json"), StandardCharsets.UTF_8);
        String japanese = source.replace("\"Value\":\"Project\"", "\"Value\":\"日本語プロジェクト\"");
        Path result = directory.resolve("japanese-state.json");
        Files.write(result, japanese.getBytes(StandardCharsets.UTF_8));
        return result;
    }

    private Run runMainWithEncoding(String[] args, String encoding) throws IOException, InterruptedException {
        List<String> command = new ArrayList<String>();
        String javaExecutable = Paths.get(System.getProperty("java.home"), "bin", "java").toString();
        command.add(javaExecutable);
        command.add("-Dfile.encoding=" + encoding);
        command.add("-cp");
        command.add(System.getProperty("surefire.test.class.path", System.getProperty("java.class.path")));
        command.add(MikuprojectCli.class.getName());
        command.addAll(Arrays.asList(args));
        Process process = new ProcessBuilder(command).directory(Paths.get(".").toFile()).start();
        process.getOutputStream().close();
        byte[] stdout = readAll(process.getInputStream());
        byte[] stderr = readAll(process.getErrorStream());
        if (!process.waitFor(10, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            throw new IOException("Java CLI subprocess timed out: " + command);
        }
        return new Run(process.exitValue(), stdout, stderr);
    }

    private byte[] readAll(InputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int read;
        while ((read = input.read(buffer)) >= 0) {
            output.write(buffer, 0, read);
        }
        return output.toByteArray();
    }

    private void assertUtf8Lf(byte[] bytes) {
        assertTrue(Arrays.equals(bytes, new String(bytes, StandardCharsets.UTF_8).getBytes(StandardCharsets.UTF_8)));
        assertTrue(bytes.length > 0 && bytes[bytes.length - 1] == (byte) '\n');
        assertFalse(new String(bytes, StandardCharsets.UTF_8).contains("\r"));
    }

    private String draftDocument(String projectName) {
        return "{\"view_type\":\"project_draft_view\",\"project\":{\"name\":\"" + projectName
                + "\"},\"tasks\":[],\"resources\":[],\"assignments\":[]}";
    }

    private String patchDocument(String projectName) {
        return "{\"operations\":[{\"op\":\"update_project\",\"fields\":{\"name\":\"" + projectName + "\"}}]}";
    }

    private String projectName(Path workbook) throws IOException {
        Map<String, Object> document = object(new String(Files.readAllBytes(workbook), StandardCharsets.UTF_8));
        Map<?, ?> sheets = (Map<?, ?>) document.get("sheets");
        for (Object row : list(sheets.get("Project"))) {
            Map<?, ?> value = (Map<?, ?>) row;
            if ("Name".equals(value.get("Field"))) return String.valueOf(value.get("Value"));
        }
        return null;
    }

    private Run run(String[] args) throws IOException {
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        int exitCode;
        try (PrintStream out = new PrintStream(stdout, true, StandardCharsets.UTF_8.name());
                PrintStream err = new PrintStream(stderr, true, StandardCharsets.UTF_8.name())) {
            exitCode = new MikuprojectCli().run(args, out, err);
        }
        return new Run(exitCode, stdout.toByteArray(), stderr.toByteArray());
    }

    private String[] append(String[] values, String... suffix) {
        String[] result = Arrays.copyOf(values, values.length + suffix.length);
        System.arraycopy(suffix, 0, result, values.length, suffix.length);
        return result;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> object(String text) {
        return (Map<String, Object>) json.parseJsonText(text);
    }

    @SuppressWarnings("unchecked")
    private List<Object> list(Object value) {
        return (List<Object>) value;
    }

    private void assertWarningMessages(Map<String, Object> diagnostics, boolean expectPatchWarning) {
        List<Object> warnings = list(diagnostics.get("warnings"));
        assertEquals(expectPatchWarning ? 3 : 2, warnings.size());
        assertTrue(warningMessages(warnings).contains("未知の列は無視します: Tasks[0].AgentNote"));
        assertTrue(warningMessages(warnings).contains("未知の sheet は無視します: CustomData"));
        if (expectPatchWarning) {
            assertTrue(warningMessages(warnings).contains("未対応の op は無視します: operations[0].op = unknown_op"));
        }
    }

    private void assertSaveFailure(Run result, Path input, byte[] originalInput) throws IOException {
        assertEquals(1, result.exitCode);
        assertEquals("", result.stdoutText());
        Map<String, Object> diagnostics = object(result.stderrText());
        assertEquals(Boolean.FALSE, diagnostics.get("ok"));
        assertEquals("error", diagnostics.get("status"));
        assertEquals(Integer.valueOf(1), diagnostics.get("exit_code"));
        assertEquals(Integer.valueOf(1), diagnostics.get("error_count"));
        assertEquals(1, list(diagnostics.get("errors")).size());
        assertEquals("error", diagnostics.get("status"));
        assertTrue(Arrays.equals(originalInput, Files.readAllBytes(input)));
    }

    private void assertMultipleStdinRejected(String[] args, Path output) throws IOException {
        CountingFailingInputStream stdin = new CountingFailingInputStream();
        Run result = runWithStdin(args, stdin);
        assertEquals(2, result.exitCode);
        assertEquals("", result.stdoutText());
        assertEquals("multiple_stdin_sources", object(result.stderrText()).get("error_code"));
        assertEquals(0, stdin.readCount);
        assertFalse(Files.exists(output));
    }

    private String[] phaseArgs(Path state, Path output, String maxDepth) {
        String[] args = { "ai", "export", "phase-detail", "--in", state.toString(), "--phase-uid", "1", "--mode", "scoped",
                "--root-task-uid", "2" };
        if (maxDepth != null) {
            args = append(args, "--max-depth", maxDepth);
        }
        return append(args, "--out", output.toString(), "--diagnostics", "json");
    }

    private String repeat(char value, int count) {
        StringBuilder result = new StringBuilder();
        for (int index = 0; index < count; index++) result.append(value);
        return result.toString();
    }

    private Run runWithStdin(String[] args, InputStream stdin) throws IOException {
        InputStream original = System.in;
        try {
            System.setIn(stdin);
            return run(args);
        } finally {
            System.setIn(original);
        }
    }

    private List<String> warningMessages(List<Object> warnings) {
        java.util.ArrayList<String> result = new java.util.ArrayList<String>();
        for (Object warning : warnings) {
            result.add(String.valueOf(((Map<?, ?>) warning).get("message")));
        }
        return result;
    }

    private int count(String text, String token) {
        int result = 0;
        int offset = 0;
        while ((offset = text.indexOf(token, offset)) >= 0) {
            result++;
            offset += token.length();
        }
        return result;
    }

    private static final class Run {
        final int exitCode;
        final byte[] stdout;
        final byte[] stderr;

        Run(int exitCode, byte[] stdout, byte[] stderr) {
            this.exitCode = exitCode;
            this.stdout = stdout;
            this.stderr = stderr;
        }

        String stdoutText() {
            return new String(stdout, StandardCharsets.UTF_8);
        }

        String stderrText() {
            return new String(stderr, StandardCharsets.UTF_8);
        }
    }

    private static final class CountingFailingInputStream extends InputStream {
        int readCount;

        @Override
        public int read() throws IOException {
            readCount++;
            throw new IOException("stdin should not be read for a rejected invocation");
        }
    }
}
