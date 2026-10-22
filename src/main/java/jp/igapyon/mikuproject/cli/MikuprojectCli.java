/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuproject.cli;

import java.io.ByteArrayOutputStream;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jp.igapyon.mikuproject.coreapi.CoreApiAiJsonParseResult;
import jp.igapyon.mikuproject.coreapi.CoreApiReportAdapters;
import jp.igapyon.mikuproject.coreapi.CoreApiAiJsonUtil;
import jp.igapyon.mikuproject.coreapi.CoreApiImport;
import jp.igapyon.mikuproject.coreapi.CoreApiWorkbookXlsx;
import jp.igapyon.mikuproject.model.ProjectModel;
import jp.igapyon.mikuproject.model.TaskModel;
import jp.igapyon.mikuproject.model.ValidationIssue;
import jp.igapyon.mikuproject.msprojectxml.MsProjectXml;
import jp.igapyon.mikuproject.projectpatchjson.PatchWarning;
import jp.igapyon.mikuproject.projectpatchjson.ProjectPatchJson;
import jp.igapyon.mikuproject.projectworkbookjson.ProjectWorkbookJson;
import jp.igapyon.mikuproject.projectworkbookjson.WorkbookJsonWarning;
import jp.igapyon.mikuproject.wbsmarkdown.WbsMarkdown.WbsMarkdownOptions;
import jp.igapyon.mikuproject.wbssvg.WbsSvg.NativeSvgOptions;
import jp.igapyon.mikuproject.wbssvg.WbsSvg.MonthlyCalendarSvgArchive;
import jp.igapyon.mikuproject.wbsxlsx.WbsXlsx.WbsExportOptions;

public class MikuprojectCli {
    public static final String VERSION = "1.1.2";
    private static final String[] NODE_TASK_HEADERS = { "UID", "ID", "Name", "OutlineLevel", "OutlineNumber", "WBS", "Start",
            "Finish", "Duration", "PercentComplete", "PercentWorkComplete", "Milestone", "Summary", "Critical", "CalendarUID",
            "Predecessors", "Notes" };
    private static final String[] NODE_RESOURCE_HEADERS = { "UID", "ID", "Name", "Type", "Initials", "Group", "MaxUnits",
            "CalendarUID", "StandardRate", "OvertimeRate", "CostPerUse", "Work", "ActualWork", "RemainingWork" };
    private static final String[] NODE_ASSIGNMENT_HEADERS = { "UID", "TaskUID", "TaskName", "ResourceUID", "ResourceName", "Start",
            "Finish", "Units", "Work", "ActualWork", "RemainingWork", "PercentWorkComplete" };
    private static final String[] NODE_CALENDAR_HEADERS = { "UID", "Name", "IsBaseCalendar", "BaseCalendarUID", "WeekDays",
            "Exceptions", "WorkWeeks" };
    private static final String[] NODE_NON_WORKING_DAYS_HEADERS = { "CalendarUID", "Index", "CalendarName", "Name", "Date",
            "FromDate", "ToDate", "DayWorking" };

    private final MsProjectXml msProjectXml = new MsProjectXml();
    private final CoreApiReportAdapters reportAdapters = new CoreApiReportAdapters();
    private final CoreApiAiJsonUtil jsonUtil = new CoreApiAiJsonUtil();
    private final CoreApiImport coreApiImport = new CoreApiImport();
    private final CoreApiWorkbookXlsx workbookXlsx = new CoreApiWorkbookXlsx();
    private final ProjectWorkbookJson workbookJson = new ProjectWorkbookJson();
    private final ProjectPatchJson patchJson = new ProjectPatchJson();
    private List<WorkbookJsonWarning> currentWorkbookWarnings = new ArrayList<WorkbookJsonWarning>();
    private String currentCommandName = "unknown command";
    private String currentDiagnosticsFormat;

    public static void main(String[] args) {
        PrintStream stdout = utf8ConsoleStream(FileDescriptor.out);
        PrintStream stderr = utf8ConsoleStream(FileDescriptor.err);
        int exitCode = new MikuprojectCli().run(args, stdout, stderr);
        if (exitCode != 0) {
            System.exit(exitCode);
        }
    }

    private static PrintStream utf8ConsoleStream(FileDescriptor descriptor) {
        try {
            return new PrintStream(new FileOutputStream(descriptor), true, StandardCharsets.UTF_8.name());
        } catch (java.io.UnsupportedEncodingException ex) {
            throw new IllegalStateException("UTF-8 is required for CLI output", ex);
        }
    }

    public int run(String[] args, PrintStream out, PrintStream err) {
        if (args == null || args.length == 0 || isHelp(args[0])) {
            printUsage(out);
            return 0;
        }
        String command = args[0];
        if (isVersion(command)) {
            printVersion(out);
            return 0;
        }
        currentWorkbookWarnings = new ArrayList<WorkbookJsonWarning>();
        Invocation invocation = inspectInvocation(args);
        currentCommandName = invocation.commandName;
        currentDiagnosticsFormat = invocation.options.get("diagnostics");
        if (!isValidDiagnosticsFormat(currentDiagnosticsFormat)) {
            return usageError(err, "--diagnostics requires text or json");
        }
        int optionCheck = validateInvocationOptions(invocation, err);
        if (optionCheck >= 0) return optionCheck;
        try {
            if ("ai".equals(command)) {
                return runAiCommand(args, out, err);
            }
            if ("state".equals(command)) {
                return runStateCommand(args, out, err);
            }
            if ("validate".equals(command)) {
                return runValidateCommand(args, out, err);
            }
            if ("export".equals(command)) {
                return runExportCommand(args, out, err);
            }
            if ("import".equals(command)) {
                return runImportCommand(args, out, err);
            }
            if ("merge".equals(command)) {
                return runMergeCommand(args, out, err);
            }
            if ("report".equals(command)) {
                return runReportCommand(args, out, err);
            }
            return usageError(err, "unknown command: " + command, true);
        } catch (BinaryUsageFailure ex) {
            return binaryUsageError(err, ex.getMessage(), ex.code, ex.details, invocation.options);
        } catch (IOException ex) {
            return executionError(err, "I/O error: " + ex.getMessage(), "io_error");
        } catch (RuntimeException ex) {
            return executionError(err, "command failed: " + ex.getMessage(), "command_error");
        }
    }

    private Invocation inspectInvocation(String[] args) {
        int commandLength = 1;
        if (args.length > 1 && !args[1].startsWith("--")) commandLength = 2;
        if ("ai".equals(args[0])) {
            commandLength = args.length > 1 && !args[1].startsWith("--") ? 2 : 1;
            if (commandLength == 2 && "export".equals(args[1]) && args.length > 2 && !args[2].startsWith("--")) {
                commandLength = 3;
            }
        }
        if (commandLength > args.length) commandLength = args.length;
        List<String> parts = new ArrayList<String>();
        for (int index = 0; index < commandLength; index++) parts.add(args[index]);
        String name = join(parts, " ");
        return new Invocation(name, parseOptions(args, commandLength));
    }

    private int validateInvocationOptions(Invocation invocation, PrintStream err) {
        List<String> allowed = allowedOptions(invocation.commandName);
        if (allowed == null) {
            String message = "未対応のコマンドです: " + invocation.commandName;
            return structuredUsageError(err, message, "unsupported_command", null, allowed);
        }
        if (!invocation.options.missingValueOptions.isEmpty()) {
            String option = "--" + invocation.options.missingValueOptions.iterator().next();
            return structuredUsageError(err, "オプション " + option + " には値が必要です", "missing_option_value", option, null);
        }
        if (!invocation.options.positionals.isEmpty()) {
            String commandName = invocation.commandName + " " + join(invocation.options.positionals, " ");
            currentCommandName = commandName;
            return structuredUsageError(err, "未対応のコマンドです: " + commandName, "unsupported_command", null, null);
        }
        for (String option : invocation.options.values.keySet()) {
            if (!allowed.contains(option)) {
                String message = invocation.commandName + " では --" + option + " を使えません";
                return structuredUsageError(err, message, "unknown_option", "--" + option, allowed);
            }
        }
        if ("import xlsx".equals(invocation.commandName)) {
            CliOptions options = invocation.options;
            boolean hasInput = hasValue(options.get("in"));
            boolean hasBase64Input = hasValue(options.get("in-base64"));
            if (hasInput && hasBase64Input) {
                return binaryUsageError(err, "import xlsx では --in と --in-base64 を同時に指定できません",
                        "multiple_input_sources", diagnosticsDetail("options", java.util.Arrays.asList("--in", "--in-base64")), options);
            }
            if ("-".equals(options.get("in"))) {
                return binaryUsageError(err, "import xlsx の binary stdin には --in-base64 - を指定してください",
                        "binary_stdin_requires_base64", diagnosticsDetail("option", "--in-base64"), options);
            }
            if (!hasInput && !hasBase64Input) {
                return binaryUsageError(err, "import xlsx には --in <path> または --in-base64 - が必要です",
                        "missing_binary_input", diagnosticsDetail("required_options", java.util.Arrays.asList("--in", "--in-base64")), options);
            }
        }
        if (allowed.contains("out-base64")) {
            CliOptions options = invocation.options;
            boolean hasOutput = hasValue(options.get("out"));
            boolean hasBase64Output = hasValue(options.get("out-base64"));
            if (hasOutput && hasBase64Output) {
                return binaryUsageError(err, invocation.commandName + " では --out と --out-base64 を同時に指定できません",
                        "multiple_output_targets", diagnosticsDetail("options", java.util.Arrays.asList("--out", "--out-base64")), options);
            }
            if (hasBase64Output && !"-".equals(options.get("out-base64"))) {
                Map<String, Object> details = diagnosticsDetail("option", "--out-base64");
                details.put("expected", "-");
                return binaryUsageError(err, invocation.commandName + " の --out-base64 は - のみ対応です",
                        "invalid_base64_output_target", details, options);
            }
            if (!hasBase64Output && (!hasOutput || "-".equals(options.get("out")))) {
                Map<String, Object> details = diagnosticsDetail("option", "--out");
                details.put("command", invocation.commandName);
                details.put("expected", "file_or_base64_stdout");
                return binaryUsageError(err, invocation.commandName + " is a binary artifact and requires --out <path> or --out-base64 -",
                        "binary_stdout_not_supported", details, options);
            }
        }
        return -1;
    }

    private boolean hasValue(String value) {
        return value != null && value.length() > 0;
    }

    private int binaryUsageError(PrintStream err, String message, String code, Map<String, Object> details, CliOptions options) {
        if (!"json".equals(currentDiagnosticsFormat)) return usageError(err, message);
        Map<String, Object> diagnostics = errorDiagnostics(2, currentCommandName, message, code);
        Map<String, Object> item = new LinkedHashMap<String, Object>();
        item.put("code", code);
        item.put("message", message);
        if (details != null) item.put("details", details);
        List<Object> errors = new ArrayList<Object>();
        errors.add(item);
        diagnostics.put("errors", errors);
        diagnostics.put("io", buildBinaryIoDiagnostics(options));
        diagnostics.put("error_type", "usage_error");
        diagnostics.put("error_details", details);
        err.println(jsonUtil.stringifyPrettyJson(diagnostics));
        return 2;
    }

    private Map<String, Object> buildBinaryIoDiagnostics(CliOptions options) {
        List<Object> inputs = new ArrayList<Object>();
        boolean hasExplicitInput = false;
        Map<String, Object> output = new LinkedHashMap<String, Object>();
        output.put("mode", "stdout");
        for (Map.Entry<String, String> entry : options.values.entrySet()) {
            String name = entry.getKey();
            String value = entry.getValue();
            if ("in".equals(name) || "in-base64".equals(name)) {
                hasExplicitInput = true;
                Map<String, Object> input = new LinkedHashMap<String, Object>();
                boolean base64 = "in-base64".equals(name);
                input.put("option", base64 ? "--in-base64" : "--in");
                input.put("mode", base64 ? ("-".equals(value) ? "stdin_base64" : "file_base64")
                        : ("-".equals(value) ? "stdin" : "file"));
                if (hasValue(value) && !"-".equals(value)) input.put("path", value);
                inputs.add(input);
            } else if ("out".equals(name)) {
                output.clear();
                output.put("mode", "-".equals(value) ? "stdout" : "file");
                if (hasValue(value) && !"-".equals(value)) output.put("path", value);
            } else if ("out-base64".equals(name)) {
                output.clear();
                output.put("mode", "-".equals(value) ? "stdout_base64" : "file_base64");
                if (hasValue(value) && !"-".equals(value)) output.put("path", value);
            }
        }
        if (!hasExplicitInput && !"import xlsx".equals(currentCommandName)) {
            Map<String, Object> implicit = new LinkedHashMap<String, Object>();
            implicit.put("option", "--in");
            implicit.put("mode", "stdin_implicit");
            inputs.add(implicit);
        }
        Map<String, Object> io = new LinkedHashMap<String, Object>();
        io.put("inputs", inputs);
        io.put("output", output);
        return io;
    }

    private List<String> allowedOptions(String command) {
        if ("ai spec".equals(command)) return options("out");
        if ("ai detect-kind".equals(command)) return options("in", "out", "diagnostics");
        if ("ai export project-overview".equals(command) || "ai export bundle".equals(command)) return options("in", "out", "diagnostics");
        if ("ai export task-edit".equals(command)) return options("in", "out", "diagnostics", "task-uid", "select");
        if ("ai export phase-detail".equals(command)) return options("in", "out", "diagnostics", "phase-uid", "select", "mode", "root-task-uid", "max-depth");
        if ("ai validate-patch".equals(command)) return options("state", "in", "out", "diagnostics");
        if ("state from-draft".equals(command)) return options("in", "out");
        if ("state validate".equals(command)) return options("in");
        if ("state import".equals(command)) return options("in", "out");
        if ("state merge".equals(command)) return options("state", "in", "out");
        if ("state apply-patch".equals(command)) return options("state", "in", "out", "diagnostics");
        if ("state summarize".equals(command)) return options("in", "out", "diagnostics");
        if ("state diff".equals(command)) return options("before", "after", "out", "diagnostics");
        if ("validate xml".equals(command) || "validate xlsx".equals(command)) return options("in");
        if ("export workbook-json".equals(command) || "export xml".equals(command)) return options("in", "out", "diagnostics");
        if ("export xlsx".equals(command)) return options("in", "out", "out-base64", "diagnostics");
        if ("import xlsx".equals(command)) return options("in", "in-base64", "out", "diagnostics");
        if ("merge xlsx".equals(command)) return options("state", "in", "out");
        if ("report all".equals(command) || "report wbs-xlsx".equals(command)
                || "report monthly-calendar-svg".equals(command)) return options("in", "out", "out-base64", "diagnostics");
        if ("report daily-svg".equals(command) || "report weekly-svg".equals(command)
                || "report wbs-markdown".equals(command) || "report mermaid".equals(command)) return options("in", "out", "diagnostics");
        if ("report dir".equals(command)) return options("in", "out");
        return null;
    }

    private List<String> options(String... names) {
        List<String> result = new ArrayList<String>();
        for (String name : names) result.add(name);
        return result;
    }

    private int structuredUsageError(PrintStream err, String message, String code, String option, List<String> allowed) {
        if (!"json".equals(currentDiagnosticsFormat)) return usageError(err, message);
        Map<String, Object> diagnostics = errorDiagnostics(2, currentCommandName, message, code);
        if (option != null) diagnostics.put("option", option);
        if (allowed != null) {
            List<String> allowedNames = new ArrayList<String>();
            for (String name : allowed) allowedNames.add("--" + name);
            diagnostics.put("allowed_options", allowedNames);
        }
        err.println(jsonUtil.stringifyPrettyJson(diagnostics));
        return 2;
    }

    private int executionError(PrintStream err, String message, String code) {
        if ("json".equals(currentDiagnosticsFormat)) {
            err.println(jsonUtil.stringifyPrettyJson(errorDiagnostics(1, currentCommandName, message, code)));
        } else {
            commandError(err, message);
        }
        return 1;
    }

    private Map<String, Object> errorDiagnostics(int exitCode, String command, String message, String code) {
        Map<String, Object> error = new LinkedHashMap<String, Object>();
        error.put("code", code);
        error.put("message", message);
        List<Object> errors = new ArrayList<Object>();
        errors.add(error);
        Map<String, Object> diagnostics = new LinkedHashMap<String, Object>();
        diagnostics.put("ok", Boolean.FALSE);
        diagnostics.put("diagnostics_version", Integer.valueOf(1));
        diagnostics.put("command", command);
        diagnostics.put("context", command);
        diagnostics.put("status", "error");
        diagnostics.put("exit_code", Integer.valueOf(exitCode));
        diagnostics.put("warning_count", Integer.valueOf(0));
        diagnostics.put("error_count", Integer.valueOf(1));
        diagnostics.put("warnings", new ArrayList<Object>());
        diagnostics.put("errors", errors);
        diagnostics.put("error_code", code);
        diagnostics.put("message", message);
        return diagnostics;
    }

    private String join(List<String> values, String separator) {
        StringBuilder result = new StringBuilder();
        for (String value : values) {
            if (result.length() > 0) result.append(separator);
            result.append(value);
        }
        return result.toString();
    }

    private static class Invocation {
        final String commandName;
        final CliOptions options;

        Invocation(String commandName, CliOptions options) {
            this.commandName = commandName;
            this.options = options;
        }
    }

    private int runAiCommand(String[] args, PrintStream out, PrintStream err) throws IOException {
        if (args.length < 2) {
            return usageError(err, "ai requires <spec|detect-kind|export|validate-patch>");
        }
        String action = args[1];
        if ("spec".equals(action)) {
            CliOptions options = parseOptions(args, 2);
            String text = coreApiImport.getAiJsonSpecText();
            writeTextOutput(options.get("out"), text, out);
            return 0;
        }
        if ("detect-kind".equals(action)) {
            CliOptions options = parseOptions(args, 2);
            String diagnosticsFormat = options.get("diagnostics");
            if (!isValidDiagnosticsFormat(diagnosticsFormat)) {
                return usageError(err, "--diagnostics requires text or json");
            }
            String input = inputOrStdin(options);
            CoreApiAiJsonParseResult parsed = coreApiImport.parseAiJsonText(readText(input));
            if (parsed.kind == null || parsed.kind.length() == 0) {
                return executionError(err, "入力 JSON の kind を判定できませんでした", "document_kind_not_detected");
            }
            writeTextOutput(options.get("out"), safe(parsed.kind) + "\n", out);
            if ("json".equals(diagnosticsFormat)) {
                Map<String, Object> diagnostics = buildCommandDiagnostics("detect-kind", options);
                diagnostics.put("detected_kind", parsed.kind);
                err.println(jsonUtil.stringifyPrettyJson(diagnostics));
            }
            return 0;
        }
        if ("export".equals(action)) {
            return runAiExportCommand(args, out, err);
        }
        if ("validate-patch".equals(action)) {
            CliOptions options = parseOptions(args, 2);
            String diagnosticsFormat = options.get("diagnostics");
            if (!isValidDiagnosticsFormat(diagnosticsFormat)) {
                return usageError(err, "--diagnostics requires text or json");
            }
            String state = options.get("state");
            if (state == null || state.length() == 0) {
                structuredUsageError(err, "ai validate-patch には --state workbook.json が必要です", "missing_state_option", "--state", null);
                return 2;
            }
            String input = inputOrStdin(options);
            if ("-".equals(state) && "-".equals(input)) {
                return rejectMultipleStdinSources(err, "--state", "--in");
            }
            Object stateDocument;
            try {
                stateDocument = parseJsonFile(state);
            } catch (RuntimeException ex) {
                Map<String, Object> details = new LinkedHashMap<String, Object>();
                details.put("context", "ai validate-patch --state");
                return writePatchValidationFailure(diagnosticsFormat, options,
                        "ai validate-patch --state の JSON を解析できませんでした", "invalid_json_input", details, out);
            } catch (IOException ex) {
                return writePatchValidationFailure(diagnosticsFormat, options, ex.getMessage(), "patch_validation_failed", null, out);
            }
            Object json;
            try {
                json = parseAiJsonFile(input);
            } catch (RuntimeException ex) {
                Map<String, Object> details = new LinkedHashMap<String, Object>();
                details.put("context", "ai validate-patch --in");
                return writePatchValidationFailure(diagnosticsFormat, options,
                        "ai validate-patch --in の JSON を解析できませんでした", "invalid_json_input", details, out);
            } catch (IOException ex) {
                return writePatchValidationFailure(diagnosticsFormat, options, ex.getMessage(), "patch_validation_failed", null, out);
            }
            String stateKind = coreApiImport.detectAiJsonDocumentKind(stateDocument);
            if (!"workbook_json".equals(stateKind)) {
                Map<String, Object> details = new LinkedHashMap<String, Object>();
                details.put("context", "ai validate-patch");
                details.put("expected_kind", "workbook_json");
                details.put("actual_kind", stateKind);
                return writePatchValidationFailure(diagnosticsFormat, options,
                        "ai validate-patch は mikuproject_workbook_json を入力してください", "invalid_workbook_kind", details, out);
            }
            String patchKind = coreApiImport.detectAiJsonDocumentKind(json);
            if (!"patch_json".equals(patchKind)) {
                Map<String, Object> details = new LinkedHashMap<String, Object>();
                details.put("context", "ai validate-patch");
                details.put("expected_kind", "patch_json");
                details.put("actual_kind", patchKind);
                return writePatchValidationFailure(diagnosticsFormat, options,
                        "ai validate-patch は patch_json を入力してください", "invalid_patch_kind", details, out);
            }
            ProjectModel baseModel;
            jp.igapyon.mikuproject.projectpatchjson.ProjectPatchJsonCore.ImportResult applied;
            try {
                jp.igapyon.mikuproject.projectworkbookjson.ProjectWorkbookJsonImport.ImportAsProjectModelResult baseImport =
                        workbookJson.importProjectWorkbookJsonAsProjectModel(stateDocument);
                currentWorkbookWarnings = baseImport.warnings == null ? new ArrayList<WorkbookJsonWarning>() : baseImport.warnings;
                appendUpstreamWorkbookColumnWarnings(stateDocument);
                baseModel = baseImport.model;
                applied = patchJson.importProjectPatchJson(json, baseModel);
            } catch (RuntimeException ex) {
                return writePatchValidationFailure(diagnosticsFormat, options, ex.getMessage(), "patch_validation_failed", null, out);
            }
            writePatchValidationOutput(diagnosticsFormat, options, applied, out);
            return 0;
        }
        return usageError(err, "unknown ai command: " + action, true);
    }

    private int runAiExportCommand(String[] args, PrintStream out, PrintStream err) throws IOException {
        if (args.length < 3) {
            return usageError(err, "ai export requires <project-overview|bundle|task-edit|phase-detail>");
        }
        String subject = args[2];
        CliOptions options = parseOptions(args, 3);
        String diagnosticsFormat = options.get("diagnostics");
        if (!isValidDiagnosticsFormat(diagnosticsFormat)) {
            return usageError(err, "--diagnostics requires text or json");
        }
        String input = inputOrStdin(options);
        ProjectModel model = importWorkbookJsonModel(input);
        Object result;
        if ("project-overview".equals(subject)) {
            result = msProjectXml.exportProjectOverviewView(model);
        } else if ("bundle".equals(subject)) {
            result = buildAiProjectionBundle(model);
        } else if ("task-edit".equals(subject)) {
            String taskUid = resolveTaskEditUid(model, options, err);
            if (taskUid == null) {
                return 2;
            }
            result = msProjectXml.exportTaskEditView(model, taskUid);
        } else if ("phase-detail".equals(subject)) {
            String phaseUid = resolvePhaseDetailUid(model, options, err);
            if (phaseUid == null) {
                return 2;
            }
            String mode = parsePhaseDetailMode(options.get("mode"), err);
            if (mode == null) {
                return 2;
            }
            Integer maxDepth = parseOptionalNonNegativeInteger(options.get("max-depth"), err);
            if (options.has("max-depth") && maxDepth == null) {
                return 2;
            }
            result = msProjectXml.exportPhaseDetailView(model, phaseUid, mode,
                    options.get("root-task-uid"), maxDepth);
        } else {
            return usageError(err, "unknown ai export command: " + subject, true);
        }
        writeTextOutput(options.get("out"), jsonUtil.stringifyPrettyJson(result) + "\n", out);
        writeAiExportDiagnostics(diagnosticsFormat, subject, options, result, err);
        return 0;
    }

    private int runStateCommand(String[] args, PrintStream out, PrintStream err) throws IOException {
        if (args.length < 2) {
            return usageError(err, "state requires <from-draft|validate|import|merge|apply-patch|summarize|diff>");
        }
        String action = args[1];
        CliOptions options = parseOptions(args, 2);
        if ("from-draft".equals(action)) {
            String input = inputOrStdin(options);
            ProjectModel model = msProjectXml.importProjectDraftView(parseAiJsonFile(input));
            writeWorkbookJsonOutput(options.get("out"), model, out);
            return 0;
        }
        if ("validate".equals(action)) {
            String input = requireOption(options, "in", err, "state validate requires --in <workbook.json>");
            if (input == null) {
                return 2;
            }
            return validateWorkbookJsonDocument(parseJsonFile(input), out);
        }
        if ("import".equals(action)) {
            String input = requireOption(options, "in", err, "state import requires --in <workbook.json>");
            if (input == null) {
                return 2;
            }
            writeWorkbookJsonOutput(options.get("out"), importWorkbookJsonModel(input), out);
            return 0;
        }
        if ("merge".equals(action)) {
            String state = requireOption(options, "state", err, "state merge requires --state <workbook.json>");
            String input = requireOption(options, "in", err, "state merge requires --in <workbook.patch.json>");
            if (state == null || input == null) {
                return 2;
            }
            jp.igapyon.mikuproject.projectworkbookjson.ProjectWorkbookJsonImport.ImportResult result =
                    workbookJson.importProjectWorkbookJson(parseJsonFile(input), importWorkbookJsonModel(state));
            writeWorkbookJsonOutput(options.get("out"), result.model, out);
            return 0;
        }
        if ("apply-patch".equals(action)) {
            String diagnosticsFormat = options.get("diagnostics");
            if (!isValidDiagnosticsFormat(diagnosticsFormat)) {
                return usageError(err, "--diagnostics requires text or json");
            }
            String state = requireOption(options, "state", err, "state apply-patch requires --state <workbook.json>");
            String input = inputOrStdin(options);
            if (state == null) {
                return 2;
            }
            if ("-".equals(state) && "-".equals(input)) {
                return rejectMultipleStdinSources(err, "--state", "--in");
            }
            jp.igapyon.mikuproject.projectpatchjson.ProjectPatchJsonCore.ImportResult result =
                    patchJson.importProjectPatchJson(parseAiJsonFile(input), importWorkbookJsonModel(state));
            writeWorkbookJsonOutput(options.get("out"), result.model, out);
            writePatchDiagnostics(diagnosticsFormat, "apply-patch", options, result, err);
            return 0;
        }
        if ("summarize".equals(action)) {
            String diagnosticsFormat = options.get("diagnostics");
            if (!isValidDiagnosticsFormat(diagnosticsFormat)) {
                return usageError(err, "--diagnostics requires text or json");
            }
            String input = inputOrStdin(options);
            Map<String, Object> summary = buildStateSummary(importWorkbookJsonModel(input));
            writeTextOutput(options.get("out"), jsonUtil.stringifyPrettyJson(summary) + "\n", out);
            writeStateSummaryDiagnostics(diagnosticsFormat, options, summary, err);
            return 0;
        }
        if ("diff".equals(action)) {
            String diagnosticsFormat = options.get("diagnostics");
            if (!isValidDiagnosticsFormat(diagnosticsFormat)) {
                return usageError(err, "--diagnostics requires text or json");
            }
            String before = requireOption(options, "before", err, "state diff requires --before <workbook.before.json>");
            String after = requireOption(options, "after", err, "state diff requires --after <workbook.after.json>");
            if (before == null || after == null) {
                return 2;
            }
            if ("-".equals(before) && "-".equals(after)) {
                return rejectMultipleStdinSources(err, "--before", "--after");
            }
            jp.igapyon.mikuproject.projectworkbookjson.ProjectWorkbookJsonDiff.Result result =
                    workbookJson.diffDocuments(parseJsonFile(before), parseJsonFile(after));
            Map<String, Object> summary = buildStateDiffSummary(result);
            writeTextOutput(options.get("out"), jsonUtil.stringifyPrettyJson(summary) + "\n", out);
            writeStateDiffDiagnostics(diagnosticsFormat, options, summary, err);
            return 0;
        }
        return usageError(err, "unknown state command: " + action, true);
    }

    private int runValidateCommand(String[] args, PrintStream out, PrintStream err) throws IOException {
        if (args.length < 2) {
            return usageError(err, "validate requires <xml|xlsx>");
        }
        String subject = args[1];
        CliOptions options = parseOptions(args, 2);
        String input = requireOption(options, "in", err, "validate " + subject + " requires --in <path>");
        if (input == null) {
            return 2;
        }
        if ("xml".equals(subject)) {
            return validateProjectModel(importXml(input), out);
        }
        if ("xlsx".equals(subject)) {
            byte[] bytes = Files.readAllBytes(Paths.get(input));
            out.println("sheets=" + workbookXlsx.decodeWorkbook(bytes).sheets.size());
            return 0;
        }
        return usageError(err, "unknown validate command: " + subject, true);
    }

    private int runExportCommand(String[] args, PrintStream out, PrintStream err) throws IOException {
        if (args.length < 2) {
            return usageError(err, "export requires <workbook-json|xml|xlsx>");
        }
        String subject = args[1];
        CliOptions options = parseOptions(args, 2);
        String diagnosticsFormat = options.get("diagnostics");
        if (!isValidDiagnosticsFormat(diagnosticsFormat)) {
            return usageError(err, "--diagnostics requires text or json");
        }
        String input = inputOrStdin(options);
        ProjectModel model = importWorkbookJsonModel(input);
        if ("workbook-json".equals(subject)) {
            writeWorkbookJsonOutput(options.get("out"), model, out);
            writeSimpleDiagnostics(diagnosticsFormat, "export workbook-json", options, "workbook_json",
                    diagnosticsDetail("sheet_count", Integer.valueOf(6)), err);
            return 0;
        }
        if ("xml".equals(subject)) {
            String xmlText = msProjectXml.exportToXml(model) + "\n";
            writeTextOutput(options.get("out"), xmlText, out);
            writeSimpleDiagnostics(diagnosticsFormat, "export xml", options, "ms_project_xml",
                    diagnosticsDetail("output_length", Integer.valueOf(xmlText.length())), err);
            return 0;
        }
        if ("xlsx".equals(subject)) {
            if (!ensureBinaryOutputTarget(options, "export xlsx", err)) {
                return 2;
            }
            byte[] bytes = workbookXlsx.encodeWorkbook(workbookXlsx.exportWorkbook(model));
            writeBinaryOutput(options, bytes, out);
            writeSimpleDiagnostics(diagnosticsFormat, "export xlsx", options, "xlsx",
                    diagnosticsDetail("byte_length", Integer.valueOf(bytes.length)), err);
            return 0;
        }
        return usageError(err, "unknown export command: " + subject, true);
    }

    private int runImportCommand(String[] args, PrintStream out, PrintStream err) throws IOException {
        if (args.length < 2) {
            return usageError(err, "import requires <xlsx>");
        }
        String subject = args[1];
        CliOptions options = parseOptions(args, 2);
        String diagnosticsFormat = options.get("diagnostics");
        if (!isValidDiagnosticsFormat(diagnosticsFormat)) {
            return usageError(err, "--diagnostics requires text or json");
        }
        if ("xlsx".equals(subject)) {
            if (!ensureBinaryInputSource(options, "import xlsx", err)) {
                return 2;
            }
            byte[] inputBytes = readBinaryInput(options, "import xlsx");
            ProjectModel model = workbookXlsx.importAsProjectModel(workbookXlsx.decodeWorkbook(inputBytes));
            writeWorkbookJsonOutput(options.get("out"), model, out);
            Map<String, Object> details = diagnosticsDetail("input_kind", "xlsx");
            details.put("output_kind", "workbook_json");
            details.put("byte_length", Integer.valueOf(inputBytes.length));
            details.put("sheet_count", Integer.valueOf(6));
            writeSimpleDiagnostics(diagnosticsFormat, "import xlsx", options, null, details, err);
            return 0;
        }
        return usageError(err, "unknown import command: " + subject, true);
    }

    private int runMergeCommand(String[] args, PrintStream out, PrintStream err) throws IOException {
        if (args.length < 2) {
            return usageError(err, "merge requires <xlsx>");
        }
        String subject = args[1];
        CliOptions options = parseOptions(args, 2);
        if ("xlsx".equals(subject)) {
            String state = requireOption(options, "state", err, "merge xlsx requires --state <workbook.json>");
            String input = requireOption(options, "in", err, "merge xlsx requires --in <workbook.xlsx>");
            if (state == null || input == null) {
                return 2;
            }
            ProjectModel model = workbookXlsx.importIntoProjectModel(workbookXlsx.decodeWorkbook(Files.readAllBytes(Paths.get(input))),
                    importWorkbookJsonModel(state));
            writeWorkbookJsonOutput(options.get("out"), model, out);
            return 0;
        }
        return usageError(err, "unknown merge command: " + subject, true);
    }

    private int runReportCommand(String[] args, PrintStream out, PrintStream err) throws IOException {
        if (args.length < 2) {
            return usageError(err, "report requires <all|dir|wbs-xlsx|daily-svg|weekly-svg|monthly-calendar-svg|wbs-markdown|mermaid>");
        }
        String subject = args[1];
        CliOptions options = parseOptions(args, 2);
        String diagnosticsFormat = options.get("diagnostics");
        if (!isValidDiagnosticsFormat(diagnosticsFormat)) {
            return usageError(err, "--diagnostics requires text or json");
        }
        String input = "dir".equals(subject) ? requireOption(options, "in", err, "report " + subject + " requires --in <workbook.json>")
                : inputOrStdin(options);
        if (input == null) {
            return 2;
        }
        ProjectModel model = importWorkbookJsonModel(input);
        WbsMarkdownOptions markdownOptions = parseWbsMarkdownOptions(options);
        WbsExportOptions xlsxOptions = parseWbsXlsxOptions(options);
        NativeSvgOptions svgOptions = parseSvgOptions(options);
        if ("all".equals(subject)) {
            if (!ensureBinaryOutputTarget(options, "report all", err)) {
                return 2;
            }
            CoreApiReportAdapters.ReportBundle bundle = reportAdapters.report.all.export(model, markdownOptions, xlsxOptions, svgOptions);
            writeBinaryOutput(options, bundle.zipBytes, out, " (" + bundle.entries.size() + " entries)");
            writeSimpleDiagnostics(diagnosticsFormat, "report all", options, "report_bundle_zip",
                    diagnosticsDetail("byte_length", Integer.valueOf(bundle.zipBytes.length)), err);
            return 0;
        }
        if ("dir".equals(subject)) {
            String output = requireOption(options, "out", err, "report dir requires --out <report.dir>");
            if (output == null) {
                return 2;
            }
            CoreApiReportAdapters.ReportBundle bundle = reportAdapters.report.all.export(model, markdownOptions, xlsxOptions, svgOptions);
            writeReportEntries(Paths.get(output), bundle);
            out.println("wrote " + output + " (" + bundle.entries.size() + " entries)");
            return 0;
        }
        if ("wbs-xlsx".equals(subject)) {
            if (!ensureBinaryOutputTarget(options, "report wbs-xlsx", err)) {
                return 2;
            }
            byte[] bytes = reportAdapters.report.wbsXlsx.exportBytes(model, xlsxOptions);
            writeBinaryOutput(options, bytes, out);
            writeSimpleDiagnostics(diagnosticsFormat, "report wbs-xlsx", options, "wbs_xlsx",
                    diagnosticsDetail("byte_length", Integer.valueOf(bytes.length)), err);
            return 0;
        }
        if ("daily-svg".equals(subject)) {
            String svgText = reportAdapters.report.svg.exportDaily(model, svgOptions) + "\n";
            writeTextOutput(options.get("out"), svgText, out);
            writeSimpleDiagnostics(diagnosticsFormat, "report daily-svg", options, "daily_svg",
                    diagnosticsDetail("output_length", Integer.valueOf(svgText.length())), err);
            return 0;
        }
        if ("weekly-svg".equals(subject)) {
            String svgText = reportAdapters.report.svg.exportWeekly(model, svgOptions) + "\n";
            writeTextOutput(options.get("out"), svgText, out);
            writeSimpleDiagnostics(diagnosticsFormat, "report weekly-svg", options, "weekly_svg",
                    diagnosticsDetail("output_length", Integer.valueOf(svgText.length())), err);
            return 0;
        }
        if ("monthly-calendar-svg".equals(subject)) {
            if (!ensureBinaryOutputTarget(options, "report monthly-calendar-svg", err)) {
                return 2;
            }
            MonthlyCalendarSvgArchive archive = reportAdapters.report.svg.exportMonthlyCalendar(model, svgOptions);
            writeBinaryOutput(options, archive.zipBytes, out, " (" + archive.entries.size() + " entries)");
            writeSimpleDiagnostics(diagnosticsFormat, "report monthly-calendar-svg", options, "monthly_calendar_svg_zip",
                    diagnosticsDetail("byte_length", Integer.valueOf(archive.zipBytes.length)), err);
            return 0;
        }
        if ("wbs-markdown".equals(subject)) {
            String markdownText = reportAdapters.report.wbsMarkdown.export(model, markdownOptions) + "\n";
            writeTextOutput(options.get("out"), markdownText, out);
            writeSimpleDiagnostics(diagnosticsFormat, "report wbs-markdown", options, "wbs_markdown",
                    diagnosticsDetail("output_length", Integer.valueOf(markdownText.length())), err);
            return 0;
        }
        if ("mermaid".equals(subject)) {
            String mermaidText = reportAdapters.report.mermaid.exportGantt(model) + "\n";
            writeTextOutput(options.get("out"), mermaidText, out);
            writeSimpleDiagnostics(diagnosticsFormat, "report mermaid", options, "mermaid",
                    diagnosticsDetail("output_length", Integer.valueOf(mermaidText.length())), err);
            return 0;
        }
        return usageError(err, "unknown report command: " + subject, true);
    }

    private ProjectModel importXml(String path) throws IOException {
        byte[] bytes = Files.readAllBytes(Paths.get(path));
        return msProjectXml.importFromXml(new String(bytes, StandardCharsets.UTF_8));
    }

    private Object parseJsonFile(String path) throws IOException {
        return jsonUtil.parseJsonText(readText(path));
    }

    private Object parseAiJsonFile(String path) throws IOException {
        return coreApiImport.parseAiJsonText(readText(path)).document;
    }

    private String inputOrStdin(CliOptions options) {
        String input = options.get("in");
        return input == null || input.length() == 0 ? "-" : input;
    }

    private int rejectMultipleStdinSources(PrintStream err, String... optionNames) {
        StringBuilder sources = new StringBuilder();
        for (String optionName : optionNames) {
            if (sources.length() > 0) sources.append(", ");
            sources.append(optionName);
        }
        String message = "標準入力を使える入力オプションは 1 つだけです: " + sources;
        return structuredUsageError(err, message, "multiple_stdin_sources", null, null);
    }

    private String exportWorkbookJsonText(ProjectModel model) {
        jp.igapyon.mikuproject.projectworkbookjson.WorkbookJsonDocument document = workbookJson.exportProjectWorkbookJson(model);
        Map<String, Object> json = new LinkedHashMap<String, Object>();
        json.put("format", document.format);
        json.put("version", document.version);
        json.put("sheets", nodeCompatibleWorkbookSheets(document));
        return jsonUtil.stringifyPrettyJson(json);
    }

    private Map<String, Object> nodeCompatibleWorkbookSheets(
            jp.igapyon.mikuproject.projectworkbookjson.WorkbookJsonDocument document) {
        Map<String, Object> sheets = new LinkedHashMap<String, Object>();
        sheets.put("Project", document.sheets.get("Project"));
        sheets.put("Tasks", nodeCompatibleWorkbookRows(document.sheets.get("Tasks"), NODE_TASK_HEADERS, "Tasks"));
        sheets.put("Resources", nodeCompatibleWorkbookRows(document.sheets.get("Resources"), NODE_RESOURCE_HEADERS, "Resources"));
        sheets.put("Assignments", nodeCompatibleWorkbookRows(document.sheets.get("Assignments"), NODE_ASSIGNMENT_HEADERS, "Assignments"));
        sheets.put("Calendars", nodeCompatibleWorkbookRows(document.sheets.get("Calendars"), NODE_CALENDAR_HEADERS, "Calendars"));
        sheets.put("NonWorkingDays", nodeCompatibleWorkbookRows(document.sheets.get("NonWorkingDays"), NODE_NON_WORKING_DAYS_HEADERS,
                "NonWorkingDays"));
        return sheets;
    }

    private List<Object> nodeCompatibleWorkbookRows(List<Map<String, Object>> rows, String[] headers, String sheetName) {
        List<Object> result = new ArrayList<Object>();
        if (rows == null) {
            return result;
        }
        for (Map<String, Object> source : rows) {
            Map<String, Object> row = new LinkedHashMap<String, Object>();
            for (String header : headers) {
                Object value = source.get(header);
                if ("Tasks".equals(sheetName) && "Predecessors".equals(header) && value == null) {
                    value = "";
                }
                if (("Tasks".equals(sheetName) && ("WBS".equals(header) || "CalendarUID".equals(header) || "Notes".equals(header)))
                        && isBlankText(value)) {
                    value = null;
                }
                if ("NonWorkingDays".equals(sheetName) && "Index".equals(header) && value != null) {
                    value = String.valueOf(Integer.parseInt(String.valueOf(value)) + 1);
                }
                row.put(header, value);
            }
            result.add(row);
        }
        return result;
    }

    private boolean isBlankText(Object value) {
        return value == null || String.valueOf(value).trim().length() == 0;
    }

    private void writeXml(String path, ProjectModel model) throws IOException {
        Files.write(Paths.get(path), msProjectXml.exportToXml(model).getBytes(StandardCharsets.UTF_8));
    }

    private String readText(String path) throws IOException {
        if ("-".equals(path)) {
            return new String(readStdinBytes(), StandardCharsets.UTF_8);
        }
        byte[] bytes = Files.readAllBytes(Paths.get(path));
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private byte[] readStdinBytes() throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[8192];
        int read;
        while ((read = System.in.read(chunk)) != -1) {
            buffer.write(chunk, 0, read);
        }
        return buffer.toByteArray();
    }

    private byte[] readBinaryInput(CliOptions options, String commandLabel) throws IOException {
        if (hasValue(options.get("in-base64"))) {
            String source = options.get("in-base64");
            byte[] encoded = "-".equals(source) ? readStdinBytes() : Files.readAllBytes(Paths.get(source));
            if ("-".equals(source) && encoded.length == 0) {
                throw new BinaryUsageFailure("入力が必要です。--in を指定するか標準入力を渡してください", "missing_input",
                        diagnosticsDetail("option", "--in"));
            }
            String normalized = new String(encoded, StandardCharsets.UTF_8).replaceAll("\\s+", "");
            if (normalized.length() == 0) {
                throw new BinaryUsageFailure(commandLabel + " の Base64 入力が空です", "invalid_base64_input",
                        diagnosticsDetail("context", commandLabel));
            }
            if (!normalized.matches("[A-Za-z0-9+/]*={0,2}") || normalized.length() % 4 != 0) {
                throw new BinaryUsageFailure(commandLabel + " の Base64 入力を解析できませんでした", "invalid_base64_input",
                        diagnosticsDetail("context", commandLabel));
            }
            try {
                return Base64.getDecoder().decode(normalized);
            } catch (IllegalArgumentException ex) {
                throw new BinaryUsageFailure(commandLabel + " の Base64 入力を解析できませんでした", "invalid_base64_input",
                        diagnosticsDetail("context", commandLabel));
            }
        }
        return Files.readAllBytes(Paths.get(options.get("in")));
    }

    private ProjectModel importWorkbookJsonModel(String path) throws IOException {
        Object document = parseJsonFile(path);
        jp.igapyon.mikuproject.projectworkbookjson.ProjectWorkbookJsonImport.ImportAsProjectModelResult result =
                workbookJson.importProjectWorkbookJsonAsProjectModel(document);
        currentWorkbookWarnings = result.warnings == null ? new ArrayList<WorkbookJsonWarning>() : result.warnings;
        appendUpstreamWorkbookColumnWarnings(document);
        return result.model;
    }

    private void appendUpstreamWorkbookColumnWarnings(Object documentLike) {
        if (!(documentLike instanceof Map<?, ?>)) return;
        Object sheetsValue = ((Map<?, ?>) documentLike).get("sheets");
        if (!(sheetsValue instanceof Map<?, ?>)) return;
        for (Map.Entry<?, ?> sheetEntry : ((Map<?, ?>) sheetsValue).entrySet()) {
            String sheet = String.valueOf(sheetEntry.getKey());
            if (!(sheetEntry.getValue() instanceof List<?>)) continue;
            List<?> rows = (List<?>) sheetEntry.getValue();
            for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
                Object rowValue = rows.get(rowIndex);
                if (!(rowValue instanceof Map<?, ?>)) continue;
                for (Object fieldValue : ((Map<?, ?>) rowValue).keySet()) {
                    String field = String.valueOf(fieldValue);
                    if (isUpstreamWorkbookColumn(sheet, field)) continue;
                    String message = "未知の列は無視します: " + sheet + "[" + rowIndex + "]." + field;
                    if (!hasWorkbookWarning(currentWorkbookWarnings, message)) {
                        WorkbookJsonWarning warning = new WorkbookJsonWarning();
                        warning.message = message;
                        currentWorkbookWarnings.add(warning);
                    }
                }
            }
        }
    }

    private boolean isUpstreamWorkbookColumn(String sheet, String field) {
        if ("Project".equals(sheet)) return "Field".equals(field) || "Value".equals(field);
        if ("Tasks".equals(sheet)) return contains(NODE_TASK_HEADERS, field);
        if ("Resources".equals(sheet)) return contains(NODE_RESOURCE_HEADERS, field);
        if ("Assignments".equals(sheet)) return contains(NODE_ASSIGNMENT_HEADERS, field);
        if ("Calendars".equals(sheet)) return contains(NODE_CALENDAR_HEADERS, field);
        if ("NonWorkingDays".equals(sheet)) return contains(NODE_NON_WORKING_DAYS_HEADERS, field);
        return true;
    }

    private boolean contains(String[] values, String value) {
        for (String candidate : values) if (candidate.equals(value)) return true;
        return false;
    }

    private boolean hasWorkbookWarning(List<WorkbookJsonWarning> warnings, String message) {
        for (WorkbookJsonWarning warning : warnings) if (message.equals(warning.message)) return true;
        return false;
    }

    private void writeWorkbookJsonOutput(String path, ProjectModel model, PrintStream out) throws IOException {
        writeTextOutput(path, exportWorkbookJsonText(model) + "\n", out);
    }

    private void writeTextOutput(String path, String text, PrintStream out) throws IOException {
        if (path == null || path.length() == 0 || "-".equals(path)) {
            out.print(text);
            return;
        }
        Path outputFile = Paths.get(path);
        Path parent = outputFile.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.write(outputFile, text.getBytes(StandardCharsets.UTF_8));
    }

    private boolean isValidDiagnosticsFormat(String value) {
        return value == null || "text".equals(value) || "json".equals(value);
    }

    private void writeAiExportDiagnostics(String diagnosticsFormat, String subject, CliOptions options, Object exported,
            PrintStream err) {
        if (!"json".equals(diagnosticsFormat)) {
            writeWorkbookWarningsText(currentWorkbookWarnings, err);
            return;
        }
        Map<String, Object> diagnostics = buildCommandDiagnostics("ai export " + subject, options);
        if ("bundle".equals(subject) && exported instanceof Map<?, ?>) {
            Map<?, ?> bundle = (Map<?, ?>) exported;
            diagnostics.put("output_kind", "ai_projection_bundle");
            diagnostics.put("phase_count", Integer.valueOf(listSize(bundle.get("phase_detail_views_full"))));
            diagnostics.put("task_count", Integer.valueOf(listSize(bundle.get("task_edit_views_full"))));
        } else if ("project-overview".equals(subject) && exported instanceof Map<?, ?>) {
            Map<?, ?> overview = (Map<?, ?>) exported;
            diagnostics.put("output_kind", "project_overview_view");
            diagnostics.put("phase_count", Integer.valueOf(listSize(overview.get("phases"))));
            Object summary = overview.get("summary");
            if (summary instanceof Map<?, ?>) {
                diagnostics.put("milestone_count", ((Map<?, ?>) summary).get("milestone_count"));
            }
        } else if ("task-edit".equals(subject) && exported instanceof Map<?, ?>) {
            Map<?, ?> taskEdit = (Map<?, ?>) exported;
            diagnostics.put("output_kind", "task_edit_view");
            Object targetTask = taskEdit.get("target_task");
            if (targetTask instanceof Map<?, ?>) {
                diagnostics.put("target_task_uid", ((Map<?, ?>) targetTask).get("uid"));
            }
            Object phase = taskEdit.get("phase");
            diagnostics.put("phase_uid", phase instanceof Map<?, ?> ? ((Map<?, ?>) phase).get("uid") : null);
        } else if ("phase-detail".equals(subject) && exported instanceof Map<?, ?>) {
            Map<?, ?> phaseDetail = (Map<?, ?>) exported;
            diagnostics.put("output_kind", "phase_detail_view");
            Object phase = phaseDetail.get("phase");
            diagnostics.put("phase_uid", phase instanceof Map<?, ?> ? ((Map<?, ?>) phase).get("uid") : null);
            Object scope = phaseDetail.get("scope");
            if (scope instanceof Map<?, ?>) {
                diagnostics.put("mode", ((Map<?, ?>) scope).get("mode"));
                diagnostics.put("root_task_uid", ((Map<?, ?>) scope).get("root_uid"));
                diagnostics.put("max_depth", ((Map<?, ?>) scope).get("max_depth"));
            }
            diagnostics.put("task_count", Integer.valueOf(listSize(phaseDetail.get("tasks"))));
        }
        applyWorkbookWarnings(diagnostics);
        err.println(jsonUtil.stringifyPrettyJson(diagnostics));
    }

    private void writeSimpleDiagnostics(String diagnosticsFormat, String command, CliOptions options, String outputKind,
            PrintStream err) {
        writeSimpleDiagnostics(diagnosticsFormat, command, options, outputKind, new LinkedHashMap<String, Object>(), err);
    }

    private void writeSimpleDiagnostics(String diagnosticsFormat, String command, CliOptions options, String outputKind,
            Map<String, Object> details, PrintStream err) {
        if (!"json".equals(diagnosticsFormat)) {
            writeWorkbookWarningsText(currentWorkbookWarnings, err);
            return;
        }
        Map<String, Object> diagnostics = buildCommandDiagnostics(command, options);
        if (outputKind != null) {
            diagnostics.put("output_kind", outputKind);
        }
        diagnostics.putAll(details);
        applyWorkbookWarnings(diagnostics);
        err.println(jsonUtil.stringifyPrettyJson(diagnostics));
    }

    private Map<String, Object> diagnosticsDetail(String key, Object value) {
        Map<String, Object> details = new LinkedHashMap<String, Object>();
        details.put(key, value);
        return details;
    }

    private void writeStateSummaryDiagnostics(String diagnosticsFormat, CliOptions options, Map<String, Object> summary,
            PrintStream err) {
        if (!"json".equals(diagnosticsFormat)) {
            writeWorkbookWarningsText(currentWorkbookWarnings, err);
            return;
        }
        Map<String, Object> diagnostics = buildCommandDiagnostics("state summarize", options);
        Object project = summary.get("project");
        if (project instanceof Map<?, ?>) {
            diagnostics.put("project_name", ((Map<?, ?>) project).get("name"));
        }
        diagnostics.put("phase_count", summary.get("phase_count"));
        Object detail = summary.get("summary");
        if (detail instanceof Map<?, ?>) {
            diagnostics.put("task_count", ((Map<?, ?>) detail).get("task_count"));
            diagnostics.put("milestone_count", ((Map<?, ?>) detail).get("milestone_count"));
        }
        applyWorkbookWarnings(diagnostics);
        err.println(jsonUtil.stringifyPrettyJson(diagnostics));
    }

    private void writeStateDiffDiagnostics(String diagnosticsFormat, CliOptions options, Map<String, Object> summary,
            PrintStream err) {
        Object warnings = summary.get("warnings");
        if (!"json".equals(diagnosticsFormat)) {
            writeWorkbookWarningsText(warnings, err);
            return;
        }
        Map<String, Object> diagnostics = buildCommandDiagnostics("state diff", options);
        diagnostics.put("io", buildIoDiagnostics(options, "before", "after"));
        int warningCount = listSize(warnings);
        Object changesSummary = summary.get("changes_summary");
        int changeCount = changesSummary instanceof Map<?, ?> && ((Map<?, ?>) changesSummary).get("total_changes") instanceof Number
                ? ((Number) ((Map<?, ?>) changesSummary).get("total_changes")).intValue() : 0;
        diagnostics.put("status", warningCount > 0 ? "warning" : changeCount == 0 ? "noop" : "success");
        diagnostics.put("warning_count", Integer.valueOf(warningCount));
        diagnostics.put("warnings", warnings instanceof List<?> ? warnings : new ArrayList<Object>());
        diagnostics.put("comparison_complete", summary.get("comparison_complete"));
        diagnostics.put("changes_summary", changesSummary);
        err.println(jsonUtil.stringifyPrettyJson(diagnostics));
    }

    private void writePatchValidationOutput(String diagnosticsFormat, CliOptions options,
            jp.igapyon.mikuproject.projectpatchjson.ProjectPatchJsonCore.ImportResult result, PrintStream out) throws IOException {
        Map<String, Object> report = buildPatchDiagnostics("ai validate-patch", options, result, "state", "in");
        report.put("diagnostics_format", diagnosticsFormat == null || diagnosticsFormat.length() == 0 ? "text" : diagnosticsFormat);
        if ("json".equals(diagnosticsFormat)) {
            writeTextOutput(options.get("out"), jsonUtil.stringifyPrettyJson(report) + "\n", out);
            return;
        }
        Map<?, ?> changesSummary = (Map<?, ?>) report.get("changes_summary");
        StringBuilder output = new StringBuilder();
        output.append("[miku-project-cli] validate-patch ok=true status=").append(report.get("status")).append(" warnings=")
                .append(report.get("warning_count")).append(" errors=0 changes=").append(changesSummary.get("total_changes")).append('\n');
        for (PatchWarning warning : result.warnings) {
            output.append("[warning] ").append(formatPatchWarning(warning)).append('\n');
        }
        for (Object warning : workbookWarningMaps(currentWorkbookWarnings)) {
            output.append("[warning] ").append(((Map<?, ?>) warning).get("message")).append('\n');
        }
        Map<?, ?> byScope = (Map<?, ?>) changesSummary.get("by_scope");
        output.append("[changes] project=").append(byScope.get("project")).append(" tasks=").append(byScope.get("tasks"))
                .append(" resources=").append(byScope.get("resources")).append(" assignments=").append(byScope.get("assignments"))
                .append(" calendars=").append(byScope.get("calendars")).append('\n');
        writeTextOutput(options.get("out"), output.toString(), out);
    }

    private int writePatchValidationFailure(String diagnosticsFormat, CliOptions options, String message, String code,
            Map<String, Object> details, PrintStream out) throws IOException {
        Map<String, Object> report = buildCommandDiagnostics("ai validate-patch", options);
        List<Object> warnings = workbookWarningMaps(currentWorkbookWarnings);
        Map<String, Object> error = new LinkedHashMap<String, Object>();
        error.put("code", code);
        error.put("message", message);
        if (details != null) error.put("details", details);
        List<Object> errors = new ArrayList<Object>();
        errors.add(error);
        Map<String, Object> emptyChanges = summarizePatchChanges(new ArrayList<jp.igapyon.mikuproject.projectpatchjson.ImportChange>());

        report.put("ok", Boolean.FALSE);
        report.put("status", "error");
        report.put("exit_code", Integer.valueOf(1));
        report.put("warning_count", Integer.valueOf(warnings.size()));
        report.put("error_count", Integer.valueOf(1));
        report.put("io", buildIoDiagnostics(options, "state", "in"));
        report.put("error_type", "processing_error");
        report.put("error_code", code);
        if (details != null) report.put("error_details", details);
        report.put("warnings", warnings);
        report.put("errors", errors);
        report.put("changes_summary", emptyChanges);
        report.put("diagnostics_format", diagnosticsFormat == null || diagnosticsFormat.length() == 0 ? "text" : diagnosticsFormat);

        if ("json".equals(diagnosticsFormat)) {
            writeTextOutput(options.get("out"), jsonUtil.stringifyPrettyJson(report) + "\n", out);
            return 1;
        }
        Map<?, ?> byScope = (Map<?, ?>) emptyChanges.get("by_scope");
        StringBuilder output = new StringBuilder();
        output.append("[miku-project-cli] validate-patch ok=false status=error warnings=").append(warnings.size())
                .append(" errors=1 changes=0\n");
        output.append("[error] ").append(message).append('\n');
        for (Object warning : warnings) {
            Object warningMessage = warning instanceof Map<?, ?> ? ((Map<?, ?>) warning).get("message") : warning;
            output.append("[warning] ").append(safe(warningMessage == null ? null : String.valueOf(warningMessage))).append('\n');
        }
        output.append("[changes] project=").append(byScope.get("project")).append(" tasks=").append(byScope.get("tasks"))
                .append(" resources=").append(byScope.get("resources")).append(" assignments=").append(byScope.get("assignments"))
                .append(" calendars=").append(byScope.get("calendars")).append('\n');
        writeTextOutput(options.get("out"), output.toString(), out);
        return 1;
    }

    private void writePatchDiagnostics(String diagnosticsFormat, String context, CliOptions options,
            jp.igapyon.mikuproject.projectpatchjson.ProjectPatchJsonCore.ImportResult result, PrintStream err) {
        Map<String, Object> diagnostics = buildPatchDiagnostics(context, options, result, "state", "in");
        if ("json".equals(diagnosticsFormat)) {
            err.println(jsonUtil.stringifyPrettyJson(diagnostics));
            return;
        }
        outPatchDiagnosticsText(context, diagnostics, result.warnings, err);
    }

    private Map<String, Object> buildPatchDiagnostics(String context, CliOptions options,
            jp.igapyon.mikuproject.projectpatchjson.ProjectPatchJsonCore.ImportResult result, String... inputNames) {
        Map<String, Object> changesSummary = summarizePatchChanges(result.changes);
        List<Object> warnings = workbookWarningMaps(currentWorkbookWarnings);
        warnings.addAll(patchWarningMaps(result.warnings));
        String status = warnings.isEmpty() ? (Integer.valueOf(0).equals(changesSummary.get("total_changes")) ? "noop" : "success")
                : "warning";
        Map<String, Object> diagnostics = buildCommandDiagnostics(context, options);
        diagnostics.put("status", status);
        diagnostics.put("warning_count", Integer.valueOf(warnings.size()));
        diagnostics.put("warnings", warnings);
        diagnostics.put("io", buildIoDiagnostics(options, inputNames));
        diagnostics.put("changes_summary", changesSummary);
        return diagnostics;
    }

    private void outPatchDiagnosticsText(String context, Map<String, Object> diagnostics, List<PatchWarning> warnings,
            PrintStream err) {
        Map<?, ?> changesSummary = (Map<?, ?>) diagnostics.get("changes_summary");
        err.println("[miku-project-cli] " + context + " patch_json status=" + diagnostics.get("status") + " changes="
                + changesSummary.get("total_changes") + " warnings=" + diagnostics.get("warning_count"));
        for (PatchWarning warning : warnings) {
            err.println("[warning] " + formatPatchWarning(warning));
        }
        writeWorkbookWarningsText(currentWorkbookWarnings, err);
    }

    private List<Object> workbookWarningMaps(List<WorkbookJsonWarning> warnings) {
        List<Object> result = new ArrayList<Object>();
        if (warnings == null) return result;
        for (WorkbookJsonWarning warning : warnings) {
            Map<String, Object> item = new LinkedHashMap<String, Object>();
            item.put("message", warning.message);
            result.add(item);
        }
        return result;
    }

    private void applyWorkbookWarnings(Map<String, Object> diagnostics) {
        List<Object> warnings = workbookWarningMaps(currentWorkbookWarnings);
        if (warnings.isEmpty()) return;
        diagnostics.put("status", "warning");
        diagnostics.put("warning_count", Integer.valueOf(warnings.size()));
        diagnostics.put("warnings", warnings);
    }

    private void writeWorkbookWarningsText(Object warnings, PrintStream out) {
        if (!(warnings instanceof List<?>)) return;
        for (Object warning : (List<?>) warnings) {
            String message = warning instanceof WorkbookJsonWarning ? ((WorkbookJsonWarning) warning).message
                    : warning instanceof Map<?, ?> ? String.valueOf(((Map<?, ?>) warning).get("message")) : String.valueOf(warning);
            out.println("[warning] " + message);
        }
    }

    private String formatPatchWarning(PatchWarning warning) {
        StringBuilder text = new StringBuilder(safe(warning.message));
        appendPatchWarningDetail(text, "scope", warning.scope);
        appendPatchWarningDetail(text, "uid", warning.uid);
        appendPatchWarningDetail(text, "label", warning.label);
        return text.toString();
    }

    private void appendPatchWarningDetail(StringBuilder text, String label, String value) {
        if (value != null && value.length() > 0) {
            text.append(" ").append(label).append("=").append(value);
        }
    }

    private List<Object> patchWarningMaps(List<PatchWarning> warnings) {
        List<Object> result = new ArrayList<Object>();
        for (PatchWarning warning : warnings) {
            Map<String, Object> item = new LinkedHashMap<String, Object>();
            item.put("message", warning.message);
            if (warning.scope != null) {
                item.put("scope", warning.scope);
            }
            if (warning.uid != null) {
                item.put("uid", warning.uid);
            }
            if (warning.label != null) {
                item.put("label", warning.label);
            }
            result.add(item);
        }
        return result;
    }

    private Map<String, Object> summarizePatchChanges(List<jp.igapyon.mikuproject.projectpatchjson.ImportChange> changes) {
        Map<String, Integer> byScope = initializedScopeCounts();
        Map<String, List<String>> affected = initializedScopeLists();
        for (jp.igapyon.mikuproject.projectpatchjson.ImportChange change : changes) {
            if (!byScope.containsKey(change.scope)) {
                continue;
            }
            byScope.put(change.scope, Integer.valueOf(byScope.get(change.scope).intValue() + 1));
            List<String> items = affected.get(change.scope);
            if (!items.contains(change.uid)) {
                items.add(change.uid);
            }
        }
        Map<String, Object> affectedCounts = new LinkedHashMap<String, Object>();
        for (Map.Entry<String, List<String>> entry : affected.entrySet()) {
            affectedCounts.put(entry.getKey(), Integer.valueOf(entry.getValue().size()));
        }
        Map<String, Object> summary = new LinkedHashMap<String, Object>();
        summary.put("total_changes", Integer.valueOf(changes.size()));
        summary.put("by_scope", byScope);
        summary.put("affected_items", affectedCounts);
        return summary;
    }

    private Map<String, Object> buildCommandDiagnostics(String command, CliOptions options) {
        Map<String, Object> diagnostics = new LinkedHashMap<String, Object>();
        diagnostics.put("ok", Boolean.TRUE);
        diagnostics.put("diagnostics_version", Integer.valueOf(1));
        diagnostics.put("command", command);
        diagnostics.put("context", command);
        diagnostics.put("status", "success");
        diagnostics.put("exit_code", Integer.valueOf(0));
        diagnostics.put("warning_count", Integer.valueOf(0));
        diagnostics.put("error_count", Integer.valueOf(0));
        diagnostics.put("warnings", new ArrayList<Object>());
        diagnostics.put("errors", new ArrayList<Object>());
        diagnostics.put("io", buildIoDiagnostics(options));
        return diagnostics;
    }

    private Map<String, Object> buildIoDiagnostics(CliOptions options) {
        return buildIoDiagnostics(options, "in");
    }

    private Map<String, Object> buildIoDiagnostics(CliOptions options, String... inputNames) {
        Map<String, Object> io = new LinkedHashMap<String, Object>();
        List<Object> inputs = new ArrayList<Object>();
        for (String inputName : inputNames) {
            Map<String, Object> input = new LinkedHashMap<String, Object>();
            boolean base64Input = "in".equals(inputName) && hasValue(options.get("in-base64"));
            String value = base64Input ? options.get("in-base64") : options.get(inputName);
            input.put("option", base64Input ? "--in-base64" : "--" + inputName);
            if (base64Input) {
                input.put("mode", "-".equals(value) ? "stdin_base64" : "file_base64");
            } else {
                input.put("mode", value == null || value.length() == 0 ? "stdin_implicit" : "-".equals(value) ? "stdin" : "file");
            }
            if (value != null && value.length() > 0 && !"-".equals(value)) {
                input.put("path", value);
            }
            inputs.add(input);
        }
        io.put("inputs", inputs);
        Map<String, Object> output = new LinkedHashMap<String, Object>();
        boolean base64Output = hasValue(options.get("out-base64"));
        if (base64Output) {
            String value = options.get("out-base64");
            output.put("mode", "-".equals(value) ? "stdout_base64" : "file_base64");
            if (value != null && !"-".equals(value)) {
                output.put("path", value);
            }
        } else {
            String value = options.get("out");
            output.put("mode", value == null || value.length() == 0 || "-".equals(value) ? "stdout" : "file");
            if (value != null && value.length() > 0 && !"-".equals(value)) {
                output.put("path", value);
            }
        }
        io.put("output", output);
        return io;
    }

    private int listSize(Object value) {
        return value instanceof List<?> ? ((List<?>) value).size() : 0;
    }

    private String parsePhaseDetailMode(String value, PrintStream err) {
        if (value == null || value.length() == 0) {
            return "scoped";
        }
        if ("scoped".equals(value) || "full".equals(value)) {
            return value;
        }
        usageError(err, "--mode requires scoped or full");
        return null;
    }

    private Integer parseOptionalNonNegativeInteger(String value, PrintStream err) {
        if (value == null) {
            return null;
        }
        if (!value.matches("[0-9]+")) {
            structuredUsageError(err, "--max-depth には 0 以上 100 以下の整数を指定してください", "invalid_integer_option", "--max-depth", null);
            return null;
        }
        String canonical = value.replaceFirst("^0+(?!$)", "");
        if (canonical.length() > 3 || (canonical.length() == 3 && canonical.compareTo("100") > 0)) {
            structuredUsageError(err, "--max-depth には 0 以上 100 以下の整数を指定してください", "invalid_integer_option", "--max-depth", null);
            return null;
        }
        return Integer.valueOf(canonical);
    }

    private String parseSelectMode(String value, PrintStream err) {
        if (value == null || value.length() == 0) {
            return "auto";
        }
        if ("auto".equals(value) || "first-task".equals(value) || "first-phase".equals(value) || "uid".equals(value)) {
            return value;
        }
        usageError(err, "--select requires auto, first-task, first-phase, or uid");
        return null;
    }

    private String resolveTaskEditUid(ProjectModel model, CliOptions options, PrintStream err) {
        String select = parseSelectMode(options.get("select"), err);
        if (select == null) {
            return null;
        }
        String taskUid = options.get("task-uid");
        if (taskUid != null && taskUid.length() > 0) {
            return taskUid;
        }
        if ("auto".equals(select) || "first-task".equals(select)) {
            return findFirstTaskUid(model);
        }
        if ("uid".equals(select)) {
            usageError(err, "ai export task-edit --select uid requires --task-uid <taskUid>");
            return null;
        }
        usageError(err, "ai export task-edit does not support --select " + select);
        return null;
    }

    private String resolvePhaseDetailUid(ProjectModel model, CliOptions options, PrintStream err) {
        String select = parseSelectMode(options.get("select"), err);
        if (select == null) {
            return null;
        }
        String phaseUid = options.get("phase-uid");
        if (phaseUid != null && phaseUid.length() > 0) {
            return phaseUid;
        }
        if ("auto".equals(select) || "first-phase".equals(select)) {
            return findFirstPhaseUid(model);
        }
        if ("uid".equals(select)) {
            usageError(err, "ai export phase-detail --select uid requires --phase-uid <phaseUid>");
            return null;
        }
        usageError(err, "ai export phase-detail does not support --select " + select);
        return null;
    }

    private String findFirstTaskUid(ProjectModel model) {
        if (model == null || model.tasks == null) {
            return null;
        }
        String firstAnyUid = null;
        for (TaskModel task : model.tasks) {
            if (task == null || task.uid == null || task.uid.trim().length() == 0 || "0".equals(task.uid.trim())) {
                continue;
            }
            if (firstAnyUid == null) {
                firstAnyUid = task.uid;
            }
            if (!task.summary) {
                return task.uid;
            }
        }
        return firstAnyUid;
    }

    private String findFirstPhaseUid(ProjectModel model) {
        if (model == null || model.tasks == null) {
            return null;
        }
        for (TaskModel task : model.tasks) {
            if (task == null || task.uid == null || task.uid.trim().length() == 0 || "0".equals(task.uid.trim())) {
                continue;
            }
            if (task.summary && Integer.valueOf(1).equals(task.outlineLevel)) {
                return task.uid;
            }
        }
        return null;
    }

    private void writeBinaryOutput(CliOptions options, byte[] bytes, PrintStream out) throws IOException {
        writeBinaryOutput(options, bytes, out, " (" + bytes.length + " bytes)");
    }

    private void writeBinaryOutput(CliOptions options, byte[] bytes, PrintStream out, String suffix) throws IOException {
        if (hasValue(options.get("out-base64"))) {
            out.println(Base64.getEncoder().encodeToString(bytes));
            return;
        }
        String path = options.get("out");
        if (path == null || path.length() == 0 || "-".equals(path)) {
            out.write(bytes);
            out.flush();
            return;
        }
        Path outputFile = Paths.get(path);
        Path parent = outputFile.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.write(outputFile, bytes);
    }

    private boolean ensureBinaryInputSource(CliOptions options, String commandLabel, PrintStream err) {
        if (hasValue(options.get("in")) && hasValue(options.get("in-base64"))) {
            usageError(err, commandLabel + " cannot use --in and --in-base64 together");
            return false;
        }
        if ("-".equals(options.get("in"))) {
            usageError(err, commandLabel + " binary stdin requires --in-base64 -");
            return false;
        }
        if (hasValue(options.get("in-base64"))) {
            return true;
        }
        if (hasValue(options.get("in"))) {
            return true;
        }
        usageError(err, commandLabel + " requires --in <path> or --in-base64 -");
        return false;
    }

    private boolean ensureBinaryOutputTarget(CliOptions options, String commandLabel, PrintStream err) {
        if (hasValue(options.get("out")) && hasValue(options.get("out-base64"))) {
            usageError(err, commandLabel + " cannot use --out and --out-base64 together");
            return false;
        }
        if (hasValue(options.get("out-base64"))) {
            if (!"-".equals(options.get("out-base64"))) {
                usageError(err, commandLabel + " --out-base64 supports only -");
                return false;
            }
            return true;
        }
        if (hasValue(options.get("out")) && !"-".equals(options.get("out"))) {
            return true;
        }
        usageError(err, commandLabel + " is a binary artifact and requires --out <path> or --out-base64 -");
        return false;
    }

    private int validateProjectModel(ProjectModel model, PrintStream out) {
        List<ValidationIssue> issues = msProjectXml.validateProjectModel(model);
        int errorCount = 0;
        for (ValidationIssue issue : issues) {
            if ("error".equals(issue.level)) {
                errorCount++;
            }
            out.println(issue.level + "\t" + safe(issue.scope) + "\t" + safe(issue.message));
        }
        out.println("issues=" + issues.size() + ", errors=" + errorCount);
        return errorCount > 0 ? 1 : 0;
    }

    private int validateWorkbookJsonDocument(Object json, PrintStream out) {
        jp.igapyon.mikuproject.projectworkbookjson.ProjectWorkbookJsonValidate.ValidationResult result =
                workbookJson.validateWorkbookJsonDocument(json);
        for (WorkbookJsonWarning warning : result.warnings) {
            out.println("warning\t" + safe(warning.message));
        }
        out.println("warnings=" + result.warnings.size());
        return 0;
    }

    private CliOptions parseOptions(String[] args, int fromIndex) {
        CliOptions options = new CliOptions();
        int index = fromIndex;
        while (index < args.length) {
            String key = args[index];
            if (key == null || !key.startsWith("--")) {
                options.positionals.add(key);
                index++;
                continue;
            }
            String name = key.substring(2);
            String value = "";
            if (index + 1 < args.length && args[index + 1] != null && !args[index + 1].startsWith("--")) {
                value = args[index + 1];
                index += 2;
            } else {
                options.missingValueOptions.add(name);
                index++;
            }
            options.values.put(name, value);
        }
        return options;
    }

    private String requireOption(CliOptions options, String name, PrintStream err, String message) {
        String value = options.get(name);
        if (value == null || value.length() == 0) {
            usageError(err, message);
            return null;
        }
        return value;
    }

    private WbsMarkdownOptions parseWbsMarkdownOptions(CliOptions args) {
        WbsMarkdownOptions options = new WbsMarkdownOptions();
        options.displayDaysBeforeBaseDate = args.getInteger("before");
        options.displayDaysAfterBaseDate = args.getInteger("after");
        options.useBusinessDaysForDisplayRange = parseCalendarMode(args.get("display-mode"));
        options.useBusinessDaysForProgressBand = parseCalendarMode(args.get("progress-mode"));
        options.holidayDates.addAll(splitCsv(args.get("holiday-dates")));
        return options;
    }

    private WbsExportOptions parseWbsXlsxOptions(CliOptions args) {
        WbsExportOptions options = new WbsExportOptions();
        options.displayDaysBeforeBaseDate = args.getInteger("before");
        options.displayDaysAfterBaseDate = args.getInteger("after");
        options.useBusinessDaysForDisplayRange = parseCalendarMode(args.get("display-mode"));
        options.useBusinessDaysForProgressBand = parseCalendarMode(args.get("progress-mode"));
        options.holidayDates.addAll(splitCsv(args.get("holiday-dates")));
        return options;
    }

    private NativeSvgOptions parseSvgOptions(CliOptions args) {
        NativeSvgOptions options = new NativeSvgOptions();
        options.holidayDates.addAll(splitCsv(args.get("holiday-dates")));
        String labelMode = args.get("label-mode");
        if (labelMode != null && labelMode.length() > 0) {
            options.labelMode = labelMode;
        }
        return options;
    }

    private Boolean parseCalendarMode(String value) {
        if (value == null || value.length() == 0) {
            return null;
        }
        String normalized = value.toLowerCase();
        if ("business".equals(normalized)) {
            return Boolean.TRUE;
        }
        if ("calendar".equals(normalized)) {
            return Boolean.FALSE;
        }
        throw new IllegalArgumentException("unsupported calendar mode: " + value + " (use business or calendar)");
    }

    private Map<String, Object> buildStateSummary(ProjectModel model) {
        Map<String, Object> overview = msProjectXml.exportProjectOverviewView(model);
        Map<String, Object> summary = new LinkedHashMap<String, Object>();
        summary.put("kind", "state_summary");
        summary.put("project", withoutNullValues(overview.get("project")));
        summary.put("summary", overview.get("summary"));
        List<?> phases = overview.get("phases") instanceof List<?> ? (List<?>) overview.get("phases") : new ArrayList<Object>();
        List<?> dependencies = overview.get("top_level_dependencies") instanceof List<?> ? (List<?>) overview.get("top_level_dependencies") : new ArrayList<Object>();
        List<?> milestones = overview.get("milestones") instanceof List<?> ? (List<?>) overview.get("milestones") : new ArrayList<Object>();
        summary.put("phase_count", Integer.valueOf(phases.size()));
        summary.put("top_level_dependency_count", Integer.valueOf(dependencies.size()));
        List<Object> phaseSummaries = new ArrayList<Object>();
        for (Object phase : phases) {
            if (phase instanceof Map<?, ?>) {
                Map<?, ?> source = (Map<?, ?>) phase;
                Map<String, Object> item = new LinkedHashMap<String, Object>();
                item.put("uid", source.get("uid"));
                item.put("name", source.get("name"));
                item.put("task_count", source.get("task_count"));
                item.put("milestone_count", source.get("milestone_count"));
                item.put("planned_start", source.get("planned_start"));
                item.put("planned_finish", source.get("planned_finish"));
                phaseSummaries.add(item);
            }
        }
        summary.put("phases", phaseSummaries);
        List<Object> majorMilestones = new ArrayList<Object>();
        for (int index = 0; index < milestones.size() && index < 10; index++) {
            majorMilestones.add(milestones.get(index));
        }
        summary.put("major_milestones", majorMilestones);
        return summary;
    }

    private Object withoutNullValues(Object value) {
        if (!(value instanceof Map<?, ?>)) {
            return value;
        }
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
            if (entry.getValue() != null) {
                result.put(String.valueOf(entry.getKey()), entry.getValue());
            }
        }
        return result;
    }

    private Map<String, Object> buildAiProjectionBundle(ProjectModel model) {
        Map<String, Object> projectOverview = msProjectXml.exportProjectOverviewView(model);
        List<Map<String, Object>> phaseDetailViewsFull = new ArrayList<Map<String, Object>>();
        Object phasesObject = projectOverview.get("phases");
        if (phasesObject instanceof List<?>) {
            for (Object phaseObject : (List<?>) phasesObject) {
                if (!(phaseObject instanceof Map<?, ?>)) {
                    continue;
                }
                Object phaseUid = ((Map<?, ?>) phaseObject).get("uid");
                if (phaseUid == null || String.valueOf(phaseUid).length() == 0) {
                    continue;
                }
                phaseDetailViewsFull.add(msProjectXml.exportPhaseDetailView(model, String.valueOf(phaseUid), "full", null, null));
            }
        }

        List<Map<String, Object>> taskEditViewsFull = new ArrayList<Map<String, Object>>();
        if (model.tasks != null) {
            for (TaskModel task : model.tasks) {
                if (task == null || "0".equals(task.uid) || task.summary) {
                    continue;
                }
                taskEditViewsFull.add(msProjectXml.exportTaskEditView(model, task.uid));
            }
        }

        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("view_type", "ai_projection_bundle");
        result.put("project_overview_view", projectOverview);
        result.put("phase_detail_views_full", phaseDetailViewsFull);
        result.put("task_edit_views_full", taskEditViewsFull);
        return result;
    }

    private Map<String, Object> buildStateDiffSummary(jp.igapyon.mikuproject.projectworkbookjson.ProjectWorkbookJsonDiff.Result result) {
        Map<String, Object> summary = new LinkedHashMap<String, Object>();
        summary.put("kind", "state_diff_summary");
        summary.put("comparison_complete", Boolean.valueOf(result.comparisonComplete));
        summary.put("warnings", result.warnings);
        summary.put("changes_summary", summarizeWorkbookChanges(result.changes));
        summary.put("changed_items", buildWorkbookChangedItems(result.changes));
        return summary;
    }

    private Map<String, Object> summarizeWorkbookChanges(List<Map<String, Object>> changes) {
        Map<String, Integer> byScope = initializedScopeCounts();
        Map<String, List<String>> affected = initializedScopeLists();
        for (Map<String, Object> change : changes) {
            String scope = String.valueOf(change.get("scope"));
            if (!byScope.containsKey(scope)) {
                continue;
            }
            byScope.put(scope, Integer.valueOf(byScope.get(scope).intValue() + 1));
            List<String> items = affected.get(scope);
            String uid = String.valueOf(change.get("uid"));
            if (!items.contains(uid)) {
                items.add(uid);
            }
        }
        Map<String, Object> affectedCounts = new LinkedHashMap<String, Object>();
        for (Map.Entry<String, List<String>> entry : affected.entrySet()) {
            affectedCounts.put(entry.getKey(), Integer.valueOf(entry.getValue().size()));
        }
        Map<String, Object> summary = new LinkedHashMap<String, Object>();
        summary.put("total_changes", Integer.valueOf(changes.size()));
        summary.put("by_scope", byScope);
        summary.put("affected_items", affectedCounts);
        return summary;
    }

    private Map<String, Object> buildWorkbookChangedItems(List<Map<String, Object>> changes) {
        Map<String, Object> grouped = new LinkedHashMap<String, Object>();
        grouped.put("project", new ArrayList<Object>());
        grouped.put("tasks", new ArrayList<Object>());
        grouped.put("resources", new ArrayList<Object>());
        grouped.put("assignments", new ArrayList<Object>());
        grouped.put("calendars", new ArrayList<Object>());
        for (Map<String, Object> change : changes) {
            String scope = String.valueOf(change.get("scope"));
            if (!grouped.containsKey(scope)) {
                continue;
            }
            Map<String, Object> item = new LinkedHashMap<String, Object>();
            item.put("uid", change.get("uid"));
            item.put("label", change.get("label"));
            item.put("field", change.get("field"));
            item.put("before", change.get("before"));
            item.put("after", change.get("after"));
            item.put("change_type", change.get("change_type"));
            item.put("sheet", change.get("sheet"));
            item.put("row_key", change.get("row_key"));
            if ("row-added".equals(change.get("change_type")) || "row-removed".equals(change.get("change_type"))) {
                item.put("before_position", change.get("before_position"));
                item.put("after_position", change.get("after_position"));
            }
            @SuppressWarnings("unchecked")
            List<Object> list = (List<Object>) grouped.get(scope);
            list.add(item);
        }
        return grouped;
    }

    private Map<String, Integer> initializedScopeCounts() {
        Map<String, Integer> result = new LinkedHashMap<String, Integer>();
        result.put("project", Integer.valueOf(0));
        result.put("tasks", Integer.valueOf(0));
        result.put("resources", Integer.valueOf(0));
        result.put("assignments", Integer.valueOf(0));
        result.put("calendars", Integer.valueOf(0));
        return result;
    }

    private Map<String, List<String>> initializedScopeLists() {
        Map<String, List<String>> result = new LinkedHashMap<String, List<String>>();
        result.put("project", new ArrayList<String>());
        result.put("tasks", new ArrayList<String>());
        result.put("resources", new ArrayList<String>());
        result.put("assignments", new ArrayList<String>());
        result.put("calendars", new ArrayList<String>());
        return result;
    }

    private static class BinaryUsageFailure extends RuntimeException {
        private static final long serialVersionUID = 1L;
        final String code;
        final Map<String, Object> details;

        BinaryUsageFailure(String message, String code, Map<String, Object> details) {
            super(message);
            this.code = code;
            this.details = details;
        }
    }

    private static class CliOptions {
        final Map<String, String> values = new LinkedHashMap<String, String>();
        final List<String> positionals = new ArrayList<String>();
        final Set<String> missingValueOptions = new LinkedHashSet<String>();

        String get(String name) {
            return values.get(name);
        }

        boolean has(String name) {
            return values.containsKey(name);
        }

        Integer getInteger(String name) {
            String value = get(name);
            if (value == null || value.length() == 0) {
                return null;
            }
            return Integer.valueOf(Integer.parseInt(value));
        }
    }

    private boolean isHelp(String command) {
        return "--help".equals(command) || "-h".equals(command) || "help".equals(command);
    }

    private boolean isVersion(String command) {
        return "--version".equals(command) || "version".equals(command);
    }

    private void printVersion(PrintStream out) {
        out.println("miku-project " + runtimeVersion());
    }

    private String runtimeVersion() {
        String implementationVersion = MikuprojectCli.class.getPackage().getImplementationVersion();
        return implementationVersion == null || implementationVersion.length() == 0 ? VERSION : implementationVersion;
    }

    private int usageError(PrintStream err, String message) {
        return usageError(err, message, false);
    }

    private int usageError(PrintStream err, String message, boolean printFullUsage) {
        err.println("usage error: " + message);
        if (printFullUsage) {
            printUsage(err);
        }
        return 2;
    }

    private int commandError(PrintStream err, String message) {
        err.println(message);
        return 1;
    }

    private void writeReportEntries(Path outputDir, CoreApiReportAdapters.ReportBundle bundle) throws IOException {
        for (jp.igapyon.mikuproject.coreapi.CoreApiReport.ReportEntry entry : bundle.entries) {
            Path outputFile = outputDir.resolve(entry.name);
            Path parent = outputFile.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.write(outputFile, entry.data);
        }
    }

    private void printUsage(PrintStream out) {
        out.println("miku-project CLI");
        out.println("Usage:");
        out.println("Shared Node-compatible commands:");
        out.println("  --version");
        out.println("  ai spec [--out spec.md|-]");
        out.println("  ai export project-overview [--in workbook.json|-] [--diagnostics text|json] [--out overview.editjson|-]");
        out.println("  ai export task-edit [--in workbook.json|-] [--task-uid taskUid] [--select auto|first-task|uid] [--diagnostics text|json] [--out task.editjson|-]");
        out.println("  ai export phase-detail [--in workbook.json|-] [--phase-uid phaseUid] [--select auto|first-phase|uid] [--mode scoped|full] [--root-task-uid rootTaskUid] [--max-depth 0..100] [--diagnostics text|json] [--out phase.editjson|-]");
        out.println("  ai export bundle [--in workbook.json|-] [--diagnostics text|json] [--out bundle.editjson|-]");
        out.println("  ai detect-kind [--in document.json|-] [--diagnostics text|json] [--out kind.txt|-]");
        out.println("  ai validate-patch --state workbook.json [--in patch.json] [--diagnostics text|json] [--out validation.txt|-]");
        out.println("  state from-draft [--in draft.editjson|-] [--out workbook.json|-]");
        out.println("  state apply-patch --state workbook.json|- [--in patch.json|-] [--diagnostics text|json] [--out workbook.next.json|-]");
        out.println("  state summarize [--in workbook.json|-] [--diagnostics text|json]");
        out.println("  state diff --before workbook.before.json --after workbook.after.json [--diagnostics text|json] [--out diff.json|-]");
        out.println("  export workbook-json [--in workbook.json|-] [--diagnostics text|json] [--out workbook.json|-]");
        out.println("  export xml [--in workbook.json|-] [--diagnostics text|json] [--out project.xml|-]");
        out.println("  export xlsx [--in workbook.json|-] [--diagnostics text|json] (--out project.xlsx|--out-base64 -)");
        out.println("  import xlsx (--in workbook.xlsx|--in-base64 -) [--diagnostics text|json] [--out workbook.json|-]");
        out.println("  report wbs-xlsx [--in workbook.json|-] [--diagnostics text|json] (--out report.xlsx|--out-base64 -)");
        out.println("  report daily-svg [--in workbook.json|-] [--diagnostics text|json] [--out report.svg|-]");
        out.println("  report weekly-svg [--in workbook.json|-] [--diagnostics text|json] [--out report.svg|-]");
        out.println("  report monthly-calendar-svg [--in workbook.json|-] [--diagnostics text|json] (--out report.zip|--out-base64 -)");
        out.println("  report all [--in workbook.json|-] [--diagnostics text|json] (--out report-bundle.zip|--out-base64 -)");
        out.println("  report wbs-markdown [--in workbook.json|-] [--diagnostics text|json] [--out report.md|-]");
        out.println("  report mermaid [--in workbook.json|-] [--diagnostics text|json] [--out report.mmd|-]");
        out.println("Java extensions:");
        out.println("  state validate --in workbook.json");
        out.println("  state import --in workbook.json [--out workbook.normalized.json]");
        out.println("  state merge --state workbook.json --in workbook.patch.json [--out workbook.next.json]");
        out.println("  validate xml --in project.xml");
        out.println("  validate xlsx --in workbook.xlsx");
        out.println("  merge xlsx --state workbook.json --in workbook.xlsx [--out workbook.next.json]");
        out.println("  report dir --in workbook.json --out report.dir");
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private List<String> splitCsv(String value) {
        List<String> result = new ArrayList<String>();
        if (value == null || value.trim().length() == 0) {
            return result;
        }
        String[] items = value.split(",");
        for (String item : items) {
            String trimmed = item == null ? "" : item.trim();
            if (trimmed.length() > 0) {
                result.add(trimmed);
            }
        }
        return result;
    }
}
