/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuproject.cli;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
    public static final String VERSION = "0.8.4";
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

    public static void main(String[] args) {
        int exitCode = new MikuprojectCli().run(args, System.out, System.err);
        if (exitCode != 0) {
            System.exit(exitCode);
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
        } catch (IOException ex) {
            return commandError(err, "I/O error: " + ex.getMessage());
        } catch (RuntimeException ex) {
            return commandError(err, "command failed: " + ex.getMessage());
        }
    }

    private int runAiCommand(String[] args, PrintStream out, PrintStream err) throws IOException {
        if (args.length < 2) {
            return usageError(err, "ai requires <spec|detect-kind|export|validate-patch>");
        }
        String action = args[1];
        if ("spec".equals(action)) {
            out.print(coreApiImport.getAiJsonSpecText());
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
            out.println(safe(parsed.kind));
            if ("json".equals(diagnosticsFormat)) {
                Map<String, Object> diagnostics = buildCommandDiagnostics("detect-kind", options);
                diagnostics.put("detected_kind", parsed.kind);
                err.println(jsonUtil.stringifyPrettyJson(diagnostics));
            }
            return parsed.kind == null || parsed.kind.length() == 0 ? 1 : 0;
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
            String state = requireOption(options, "state", err, "ai validate-patch requires --state <workbook.json>");
            String input = inputOrStdin(options);
            if (state == null) {
                return 2;
            }
            ProjectModel baseModel = importWorkbookJsonModel(state);
            Object json = parseJsonFile(input);
            jp.igapyon.mikuproject.projectpatchjson.ProjectPatchJsonCore.ImportResult applied = patchJson.importProjectPatchJson(json, baseModel);
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
            result = msProjectXml.exportPhaseDetailView(model, phaseUid, mode,
                    options.get("root-task-uid"), options.getInteger("max-depth"));
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
            ProjectModel model = msProjectXml.importProjectDraftView(parseJsonFile(input));
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
            jp.igapyon.mikuproject.projectpatchjson.ProjectPatchJsonCore.ImportResult result =
                    patchJson.importProjectPatchJson(parseJsonFile(input), importWorkbookJsonModel(state));
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
            ProjectModel beforeModel = importWorkbookJsonModel(before);
            jp.igapyon.mikuproject.projectworkbookjson.ProjectWorkbookJsonImport.ImportResult result =
                    workbookJson.importProjectWorkbookJson(parseJsonFile(after), beforeModel);
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

    private String inputOrStdin(CliOptions options) {
        String input = options.get("in");
        return input == null || input.length() == 0 ? "-" : input;
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
        if (options.has("in-base64")) {
            String source = options.get("in-base64");
            byte[] encoded = "-".equals(source) ? readStdinBytes() : Files.readAllBytes(Paths.get(source));
            try {
                String normalized = new String(encoded, StandardCharsets.UTF_8).replaceAll("\\s+", "");
                return Base64.getDecoder().decode(normalized);
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException(commandLabel + " invalid Base64 input");
            }
        }
        return Files.readAllBytes(Paths.get(options.get("in")));
    }

    private ProjectModel importWorkbookJsonModel(String path) throws IOException {
        jp.igapyon.mikuproject.projectworkbookjson.ProjectWorkbookJsonImport.ImportAsProjectModelResult result =
                workbookJson.importProjectWorkbookJsonAsProjectModel(parseJsonFile(path));
        return result.model;
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
        return value == null || value.length() == 0 || "text".equals(value) || "json".equals(value);
    }

    private void writeAiExportDiagnostics(String diagnosticsFormat, String subject, CliOptions options, Object exported,
            PrintStream err) {
        if (!"json".equals(diagnosticsFormat)) {
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
        err.println(jsonUtil.stringifyPrettyJson(diagnostics));
    }

    private void writeSimpleDiagnostics(String diagnosticsFormat, String command, CliOptions options, String outputKind,
            PrintStream err) {
        writeSimpleDiagnostics(diagnosticsFormat, command, options, outputKind, new LinkedHashMap<String, Object>(), err);
    }

    private void writeSimpleDiagnostics(String diagnosticsFormat, String command, CliOptions options, String outputKind,
            Map<String, Object> details, PrintStream err) {
        if (!"json".equals(diagnosticsFormat)) {
            return;
        }
        Map<String, Object> diagnostics = buildCommandDiagnostics(command, options);
        if (outputKind != null) {
            diagnostics.put("output_kind", outputKind);
        }
        diagnostics.putAll(details);
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
        err.println(jsonUtil.stringifyPrettyJson(diagnostics));
    }

    private void writeStateDiffDiagnostics(String diagnosticsFormat, CliOptions options, Map<String, Object> summary,
            PrintStream err) {
        if (!"json".equals(diagnosticsFormat)) {
            return;
        }
        Map<String, Object> diagnostics = buildCommandDiagnostics("state diff", options);
        diagnostics.put("io", buildIoDiagnostics(options, "before", "after"));
        Object warnings = summary.get("warnings");
        int warningCount = listSize(warnings);
        diagnostics.put("status", warningCount > 0 ? "warning" : "noop");
        diagnostics.put("warning_count", Integer.valueOf(warningCount));
        diagnostics.put("warnings", warnings instanceof List<?> ? warnings : new ArrayList<Object>());
        diagnostics.put("changes_summary", summary.get("changes_summary"));
        err.println(jsonUtil.stringifyPrettyJson(diagnostics));
    }

    private void writePatchValidationOutput(String diagnosticsFormat, CliOptions options,
            jp.igapyon.mikuproject.projectpatchjson.ProjectPatchJsonCore.ImportResult result, PrintStream out) {
        Map<String, Object> report = buildPatchDiagnostics("ai validate-patch", options, result, "state", "in");
        report.put("diagnostics_format", diagnosticsFormat == null || diagnosticsFormat.length() == 0 ? "text" : diagnosticsFormat);
        if ("json".equals(diagnosticsFormat)) {
            out.println(jsonUtil.stringifyPrettyJson(report));
            return;
        }
        Map<?, ?> changesSummary = (Map<?, ?>) report.get("changes_summary");
        out.println("[miku-project-cli] validate-patch ok=true status=" + report.get("status") + " warnings="
                + report.get("warning_count") + " errors=0 changes=" + changesSummary.get("total_changes"));
        for (PatchWarning warning : result.warnings) {
            out.println("[warning] " + formatPatchWarning(warning));
        }
        Map<?, ?> byScope = (Map<?, ?>) changesSummary.get("by_scope");
        out.println("[changes] project=" + byScope.get("project") + " tasks=" + byScope.get("tasks") + " resources="
                + byScope.get("resources") + " assignments=" + byScope.get("assignments") + " calendars="
                + byScope.get("calendars"));
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
        List<Object> warnings = patchWarningMaps(result.warnings);
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
            boolean base64Input = "in".equals(inputName) && options.has("in-base64");
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
        boolean base64Output = options.has("out-base64");
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
        if (options.has("out-base64")) {
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
        if (options.has("in") && options.has("in-base64")) {
            usageError(err, commandLabel + " cannot use --in and --in-base64 together");
            return false;
        }
        if ("-".equals(options.get("in"))) {
            usageError(err, commandLabel + " binary stdin requires --in-base64 -");
            return false;
        }
        if (options.has("in-base64")) {
            if (options.get("in-base64").length() == 0) {
                usageError(err, commandLabel + " requires --in <path> or --in-base64 -");
                return false;
            }
            return true;
        }
        if (options.has("in") && options.get("in").length() > 0) {
            return true;
        }
        usageError(err, commandLabel + " requires --in <path> or --in-base64 -");
        return false;
    }

    private boolean ensureBinaryOutputTarget(CliOptions options, String commandLabel, PrintStream err) {
        if (options.has("out") && options.has("out-base64")) {
            usageError(err, commandLabel + " cannot use --out and --out-base64 together");
            return false;
        }
        if (options.has("out-base64")) {
            if (!"-".equals(options.get("out-base64"))) {
                usageError(err, commandLabel + " --out-base64 supports only -");
                return false;
            }
            return true;
        }
        if (options.has("out") && options.get("out").length() > 0 && !"-".equals(options.get("out"))) {
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

    private Map<String, Object> buildStateDiffSummary(jp.igapyon.mikuproject.projectworkbookjson.ProjectWorkbookJsonImport.ImportResult result) {
        Map<String, Object> summary = new LinkedHashMap<String, Object>();
        summary.put("kind", "state_diff_summary");
        summary.put("warnings", result.warnings);
        summary.put("changes_summary", summarizeWorkbookChanges(result.changes));
        summary.put("changed_items", buildWorkbookChangedItems(result.changes));
        return summary;
    }

    private Map<String, Object> summarizeWorkbookChanges(List<jp.igapyon.mikuproject.projectworkbookjson.ImportChange> changes) {
        Map<String, Integer> byScope = initializedScopeCounts();
        Map<String, List<String>> affected = initializedScopeLists();
        for (jp.igapyon.mikuproject.projectworkbookjson.ImportChange change : changes) {
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

    private Map<String, Object> buildWorkbookChangedItems(List<jp.igapyon.mikuproject.projectworkbookjson.ImportChange> changes) {
        Map<String, Object> grouped = new LinkedHashMap<String, Object>();
        grouped.put("project", new ArrayList<Object>());
        grouped.put("tasks", new ArrayList<Object>());
        grouped.put("resources", new ArrayList<Object>());
        grouped.put("assignments", new ArrayList<Object>());
        grouped.put("calendars", new ArrayList<Object>());
        for (jp.igapyon.mikuproject.projectworkbookjson.ImportChange change : changes) {
            if (!grouped.containsKey(change.scope)) {
                continue;
            }
            Map<String, Object> item = new LinkedHashMap<String, Object>();
            item.put("uid", change.uid);
            item.put("label", change.label);
            item.put("field", change.field);
            item.put("before", change.before);
            item.put("after", change.after);
            @SuppressWarnings("unchecked")
            List<Object> list = (List<Object>) grouped.get(change.scope);
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

    private static class CliOptions {
        final Map<String, String> values = new LinkedHashMap<String, String>();
        final List<String> positionals = new ArrayList<String>();

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
        out.println(runtimeVersion());
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
        out.println("  ai spec");
        out.println("  ai export project-overview [--in workbook.json|-] [--diagnostics text|json] [--out overview.editjson|-]");
        out.println("  ai export task-edit [--in workbook.json|-] [--task-uid taskUid] [--select auto|first-task|uid] [--diagnostics text|json] [--out task.editjson|-]");
        out.println("  ai export phase-detail [--in workbook.json|-] [--phase-uid phaseUid] [--select auto|first-phase|uid] [--mode scoped|full] [--root-task-uid rootTaskUid] [--max-depth n] [--diagnostics text|json] [--out phase.editjson|-]");
        out.println("  ai export bundle [--in workbook.json|-] [--diagnostics text|json] [--out bundle.editjson|-]");
        out.println("  ai detect-kind [--in document.json|-] [--diagnostics text|json]");
        out.println("  ai validate-patch --state workbook.json [--in patch.json] [--diagnostics text|json]");
        out.println("  state from-draft [--in draft.editjson|-] [--out workbook.json|-]");
        out.println("  state apply-patch --state workbook.json|- [--in patch.json|-] [--diagnostics text|json] [--out workbook.next.json|-]");
        out.println("  state summarize [--in workbook.json|-] [--diagnostics text|json]");
        out.println("  state diff --before workbook.before.json --after workbook.after.json [--diagnostics text|json]");
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
