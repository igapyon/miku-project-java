/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuproject.cli;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jp.igapyon.mikuproject.coreapi.CoreApiAiJsonParseResult;
import jp.igapyon.mikuproject.coreapi.CoreApiReportAdapters;
import jp.igapyon.mikuproject.coreapi.CoreApiAiJsonUtil;
import jp.igapyon.mikuproject.coreapi.CoreApiImport;
import jp.igapyon.mikuproject.coreapi.CoreApiWorkbookXlsx;
import jp.igapyon.mikuproject.model.ProjectModel;
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
    public static final String VERSION = "0.8.1";

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
            String input = requireOption(options, "in", err, "ai detect-kind requires --in <document.json>");
            if (input == null) {
                return 2;
            }
            CoreApiAiJsonParseResult parsed = coreApiImport.parseAiJsonText(readText(input));
            out.println("kind=" + safe(parsed.kind));
            return parsed.kind == null || parsed.kind.length() == 0 ? 1 : 0;
        }
        if ("export".equals(action)) {
            return runAiExportCommand(args, out, err);
        }
        if ("validate-patch".equals(action)) {
            CliOptions options = parseOptions(args, 2);
            String state = requireOption(options, "state", err, "ai validate-patch requires --state <workbook.json>");
            String input = requireOption(options, "in", err, "ai validate-patch requires --in <patch.editjson>");
            if (state == null || input == null) {
                return 2;
            }
            ProjectModel baseModel = importWorkbookJsonModel(state);
            Object json = parseJsonFile(input);
            jp.igapyon.mikuproject.projectpatchjson.ProjectPatchJsonCore.ValidationResult validation = patchJson.validatePatchDocument(json);
            jp.igapyon.mikuproject.projectpatchjson.ProjectPatchJsonCore.ImportResult applied = patchJson.importProjectPatchJson(json, baseModel);
            for (PatchWarning warning : validation.warnings) {
                out.println("warning\t" + safe(warning.message));
            }
            for (PatchWarning warning : applied.warnings) {
                out.println("warning\t" + safe(warning.message));
            }
            out.println("operations=" + validation.document.operations.size() + ", changes=" + applied.changes.size()
                    + ", warnings=" + (validation.warnings.size() + applied.warnings.size()));
            return 0;
        }
        return usageError(err, "unknown ai command: " + action, true);
    }

    private int runAiExportCommand(String[] args, PrintStream out, PrintStream err) throws IOException {
        if (args.length < 3) {
            return usageError(err, "ai export requires <project-overview|task-edit|phase-detail>");
        }
        String subject = args[2];
        CliOptions options = parseOptions(args, 3);
        String input = requireOption(options, "in", err, "ai export " + subject + " requires --in <workbook.json>");
        if (input == null) {
            return 2;
        }
        ProjectModel model = importWorkbookJsonModel(input);
        Object result;
        if ("project-overview".equals(subject)) {
            result = msProjectXml.exportProjectOverviewView(model);
        } else if ("task-edit".equals(subject)) {
            String taskUid = options.get("task-uid");
            if (taskUid == null || taskUid.length() == 0) {
                return usageError(err, "ai export task-edit requires --task-uid <taskUid>");
            }
            result = msProjectXml.exportTaskEditView(model, taskUid);
        } else if ("phase-detail".equals(subject)) {
            result = msProjectXml.exportPhaseDetailView(model, options.get("phase-uid"), options.get("mode"),
                    options.get("root-uid"), options.getInteger("max-depth"));
        } else {
            return usageError(err, "unknown ai export command: " + subject, true);
        }
        writeTextOutput(options.get("out"), jsonUtil.stringifyJson(result) + "\n", out);
        return 0;
    }

    private int runStateCommand(String[] args, PrintStream out, PrintStream err) throws IOException {
        if (args.length < 2) {
            return usageError(err, "state requires <from-draft|validate|import|merge|apply-patch|summarize|diff>");
        }
        String action = args[1];
        CliOptions options = parseOptions(args, 2);
        if ("from-draft".equals(action)) {
            String input = requireOption(options, "in", err, "state from-draft requires --in <draft.editjson>");
            if (input == null) {
                return 2;
            }
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
            String state = requireOption(options, "state", err, "state apply-patch requires --state <workbook.json>");
            String input = requireOption(options, "in", err, "state apply-patch requires --in <patch.editjson>");
            if (state == null || input == null) {
                return 2;
            }
            jp.igapyon.mikuproject.projectpatchjson.ProjectPatchJsonCore.ImportResult result =
                    patchJson.importProjectPatchJson(parseJsonFile(input), importWorkbookJsonModel(state));
            writeWorkbookJsonOutput(options.get("out"), result.model, out);
            return 0;
        }
        if ("summarize".equals(action)) {
            String input = requireOption(options, "in", err, "state summarize requires --in <workbook.json>");
            if (input == null) {
                return 2;
            }
            writeTextOutput(options.get("out"), jsonUtil.stringifyJson(buildStateSummary(importWorkbookJsonModel(input))) + "\n", out);
            return 0;
        }
        if ("diff".equals(action)) {
            String before = requireOption(options, "before", err, "state diff requires --before <workbook.before.json>");
            String after = requireOption(options, "after", err, "state diff requires --after <workbook.after.json>");
            if (before == null || after == null) {
                return 2;
            }
            ProjectModel beforeModel = importWorkbookJsonModel(before);
            jp.igapyon.mikuproject.projectworkbookjson.ProjectWorkbookJsonImport.ImportResult result =
                    workbookJson.importProjectWorkbookJson(parseJsonFile(after), beforeModel);
            writeTextOutput(options.get("out"), jsonUtil.stringifyJson(buildStateDiffSummary(result)) + "\n", out);
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
        String input = requireOption(options, "in", err, "export " + subject + " requires --in <workbook.json>");
        if (input == null) {
            return 2;
        }
        ProjectModel model = importWorkbookJsonModel(input);
        if ("workbook-json".equals(subject)) {
            writeWorkbookJsonOutput(options.get("out"), model, out);
            return 0;
        }
        if ("xml".equals(subject)) {
            writeTextOutput(options.get("out"), msProjectXml.exportToXml(model), out);
            return 0;
        }
        if ("xlsx".equals(subject)) {
            String output = requireOption(options, "out", err, "export xlsx requires --out <workbook.xlsx>");
            if (output == null) {
                return 2;
            }
            byte[] bytes = workbookXlsx.encodeWorkbook(workbookXlsx.exportWorkbook(model));
            writeBinaryOutput(output, bytes);
            out.println("wrote " + output + " (" + bytes.length + " bytes)");
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
        if ("xlsx".equals(subject)) {
            String input = requireOption(options, "in", err, "import xlsx requires --in <workbook.xlsx>");
            if (input == null) {
                return 2;
            }
            ProjectModel model = workbookXlsx.importAsProjectModel(workbookXlsx.decodeWorkbook(Files.readAllBytes(Paths.get(input))));
            writeWorkbookJsonOutput(options.get("out"), model, out);
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
        String input = requireOption(options, "in", err, "report " + subject + " requires --in <workbook.json>");
        if (input == null) {
            return 2;
        }
        ProjectModel model = importWorkbookJsonModel(input);
        WbsMarkdownOptions markdownOptions = parseWbsMarkdownOptions(options);
        WbsExportOptions xlsxOptions = parseWbsXlsxOptions(options);
        NativeSvgOptions svgOptions = parseSvgOptions(options);
        if ("all".equals(subject)) {
            String output = requireOption(options, "out", err, "report all requires --out <report-bundle.zip>");
            if (output == null) {
                return 2;
            }
            CoreApiReportAdapters.ReportBundle bundle = reportAdapters.report.all.export(model, markdownOptions, xlsxOptions, svgOptions);
            writeBinaryOutput(output, bundle.zipBytes);
            out.println("wrote " + output + " (" + bundle.entries.size() + " entries)");
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
            String output = requireOption(options, "out", err, "report wbs-xlsx requires --out <wbs.xlsx>");
            if (output == null) {
                return 2;
            }
            byte[] bytes = reportAdapters.report.wbsXlsx.exportBytes(model, xlsxOptions);
            writeBinaryOutput(output, bytes);
            out.println("wrote " + output + " (" + bytes.length + " bytes)");
            return 0;
        }
        if ("daily-svg".equals(subject)) {
            writeTextOutput(options.get("out"), reportAdapters.report.svg.exportDaily(model, svgOptions), out);
            return 0;
        }
        if ("weekly-svg".equals(subject)) {
            writeTextOutput(options.get("out"), reportAdapters.report.svg.exportWeekly(model, svgOptions), out);
            return 0;
        }
        if ("monthly-calendar-svg".equals(subject)) {
            String output = requireOption(options, "out", err, "report monthly-calendar-svg requires --out <monthly-calendar.zip>");
            if (output == null) {
                return 2;
            }
            MonthlyCalendarSvgArchive archive = reportAdapters.report.svg.exportMonthlyCalendar(model, svgOptions);
            writeBinaryOutput(output, archive.zipBytes);
            out.println("wrote " + output + " (" + archive.entries.size() + " entries)");
            return 0;
        }
        if ("wbs-markdown".equals(subject)) {
            writeTextOutput(options.get("out"), reportAdapters.report.wbsMarkdown.export(model, markdownOptions), out);
            return 0;
        }
        if ("mermaid".equals(subject)) {
            writeTextOutput(options.get("out"), reportAdapters.report.mermaid.exportGantt(model), out);
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

    private String exportWorkbookJsonText(ProjectModel model) {
        jp.igapyon.mikuproject.projectworkbookjson.WorkbookJsonDocument document = workbookJson.exportProjectWorkbookJson(model);
        Map<String, Object> json = new LinkedHashMap<String, Object>();
        json.put("format", document.format);
        json.put("version", document.version);
        json.put("sheets", document.sheets);
        return jsonUtil.stringifyJson(json);
    }

    private void writeXml(String path, ProjectModel model) throws IOException {
        Files.write(Paths.get(path), msProjectXml.exportToXml(model).getBytes(StandardCharsets.UTF_8));
    }

    private String readText(String path) throws IOException {
        if ("-".equals(path)) {
            throw new IllegalArgumentException("stdin input is not supported by this Java embedding API; use a file path");
        }
        byte[] bytes = Files.readAllBytes(Paths.get(path));
        return new String(bytes, StandardCharsets.UTF_8);
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
        out.println("wrote " + path);
    }

    private void writeBinaryOutput(String path, byte[] bytes) throws IOException {
        Path outputFile = Paths.get(path);
        Path parent = outputFile.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.write(outputFile, bytes);
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
        summary.put("project", overview.get("project"));
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
        out.println("mikuproject-java " + runtimeVersion());
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
        out.println("mikuproject-java CLI");
        out.println("usage:");
        out.println("  --version");
        out.println("  ai spec");
        out.println("  ai detect-kind --in document.json");
        out.println("  ai export project-overview --in workbook.json [--out overview.editjson]");
        out.println("  ai export task-edit --in workbook.json --task-uid taskUid [--out task.editjson]");
        out.println("  ai export phase-detail --in workbook.json [--phase-uid phaseUid] [--mode mode] [--root-uid rootUid] [--max-depth n] [--out phase.editjson]");
        out.println("  ai validate-patch --state workbook.json --in patch.editjson");
        out.println("  state from-draft --in draft.editjson [--out workbook.json]");
        out.println("  state validate --in workbook.json");
        out.println("  state import --in workbook.json [--out workbook.normalized.json]");
        out.println("  state merge --state workbook.json --in workbook.patch.json [--out workbook.next.json]");
        out.println("  state apply-patch --state workbook.json --in patch.editjson [--out workbook.next.json]");
        out.println("  state summarize --in workbook.json [--out summary.json]");
        out.println("  state diff --before workbook.before.json --after workbook.after.json [--out diff.json]");
        out.println("  validate xml --in project.xml");
        out.println("  validate xlsx --in workbook.xlsx");
        out.println("  export workbook-json --in workbook.json [--out workbook.normalized.json]");
        out.println("  export xml --in workbook.json --out project.xml");
        out.println("  export xlsx --in workbook.json --out workbook.xlsx");
        out.println("  import xlsx --in workbook.xlsx [--out workbook.json]");
        out.println("  merge xlsx --state workbook.json --in workbook.xlsx [--out workbook.next.json]");
        out.println("  report all --in workbook.json --out report-bundle.zip");
        out.println("  report dir --in workbook.json --out report.dir");
        out.println("  report wbs-xlsx --in workbook.json --out wbs.xlsx");
        out.println("  report daily-svg --in workbook.json [--out daily.svg]");
        out.println("  report weekly-svg --in workbook.json [--out weekly.svg]");
        out.println("  report monthly-calendar-svg --in workbook.json --out monthly-calendar.zip");
        out.println("  report wbs-markdown --in workbook.json [--out wbs.md]");
        out.println("  report mermaid --in workbook.json [--out mermaid.mmd]");
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
