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
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jp.igapyon.mikuproject.coreapi.CoreApiAiJson;
import jp.igapyon.mikuproject.coreapi.CoreApiAiJsonParseResult;
import jp.igapyon.mikuproject.coreapi.CoreApiReportAdapters;
import jp.igapyon.mikuproject.coreapi.CoreApiAiJsonUtil;
import jp.igapyon.mikuproject.coreapi.CoreApiExternalImport;
import jp.igapyon.mikuproject.coreapi.CoreApiImport;
import jp.igapyon.mikuproject.coreapi.CoreApiImportResult;
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
    private final MsProjectXml msProjectXml = new MsProjectXml();
    private final CoreApiReportAdapters reportAdapters = new CoreApiReportAdapters();
    private final CoreApiAiJsonUtil jsonUtil = new CoreApiAiJsonUtil();
    private final CoreApiImport coreApiImport = new CoreApiImport();
    private final CoreApiAiJson coreApiAiJson = new CoreApiAiJson();
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
        try {
            if ("validate-xml".equals(command)) {
                return validateXml(args, out, err);
            }
            if ("validate-xml-batch".equals(command)) {
                return validateXmlBatch(args, out, err);
            }
            if ("export-mermaid".equals(command)) {
                return exportMermaid(args, out, err);
            }
            if ("export-mermaid-batch".equals(command)) {
                return exportMermaidBatch(args, out, err);
            }
            if ("export-wbs-markdown".equals(command)) {
                return exportWbsMarkdown(args, out, err);
            }
            if ("export-wbs-markdown-batch".equals(command)) {
                return exportWbsMarkdownBatch(args, out, err);
            }
            if ("export-daily-svg".equals(command)) {
                return exportDailySvg(args, out, err);
            }
            if ("export-daily-svg-batch".equals(command)) {
                return exportDailySvgBatch(args, out, err);
            }
            if ("export-weekly-svg".equals(command)) {
                return exportWeeklySvg(args, out, err);
            }
            if ("export-weekly-svg-batch".equals(command)) {
                return exportWeeklySvgBatch(args, out, err);
            }
            if ("export-monthly-svg-zip".equals(command)) {
                return exportMonthlySvgZip(args, out, err);
            }
            if ("export-monthly-svg-zip-batch".equals(command)) {
                return exportMonthlySvgZipBatch(args, out, err);
            }
            if ("export-report-bundle".equals(command)) {
                return exportReportBundle(args, out, err);
            }
            if ("export-report-bundle-batch".equals(command)) {
                return exportReportBundleBatch(args, out, err);
            }
            if ("export-report-dir".equals(command)) {
                return exportReportDir(args, out, err);
            }
            if ("export-report-dir-batch".equals(command)) {
                return exportReportDirBatch(args, out, err);
            }
            if ("export-wbs-xlsx".equals(command)) {
                return exportWbsXlsx(args, out, err);
            }
            if ("export-wbs-xlsx-batch".equals(command)) {
                return exportWbsXlsxBatch(args, out, err);
            }
            if ("export-workbook-json".equals(command)) {
                return exportWorkbookJson(args, out, err);
            }
            if ("export-workbook-json-batch".equals(command)) {
                return exportWorkbookJsonBatch(args, out, err);
            }
            if ("export-project-overview-view".equals(command)) {
                return exportProjectOverviewView(args, out, err);
            }
            if ("export-project-overview-view-batch".equals(command)) {
                return exportProjectOverviewViewBatch(args, out, err);
            }
            if ("export-phase-detail-view".equals(command)) {
                return exportPhaseDetailView(args, out, err);
            }
            if ("export-phase-detail-view-batch".equals(command)) {
                return exportPhaseDetailViewBatch(args, out, err);
            }
            if ("export-task-edit-view".equals(command)) {
                return exportTaskEditView(args, out, err);
            }
            if ("export-task-edit-view-batch".equals(command)) {
                return exportTaskEditViewBatch(args, out, err);
            }
            if ("export-project-draft-request".equals(command)) {
                return exportProjectDraftRequest(args, out, err);
            }
            if ("validate-workbook-json".equals(command)) {
                return validateWorkbookJson(args, out, err);
            }
            if ("validate-workbook-json-batch".equals(command)) {
                return validateWorkbookJsonBatch(args, out, err);
            }
            if ("import-workbook-json".equals(command)) {
                return importWorkbookJson(args, out, err);
            }
            if ("import-workbook-json-batch".equals(command)) {
                return importWorkbookJsonBatch(args, out, err);
            }
            if ("merge-workbook-json".equals(command)) {
                return mergeWorkbookJson(args, out, err);
            }
            if ("merge-workbook-json-batch".equals(command)) {
                return mergeWorkbookJsonBatch(args, out, err);
            }
            if ("validate-patch-json".equals(command)) {
                return validatePatchJson(args, out, err);
            }
            if ("validate-patch-json-batch".equals(command)) {
                return validatePatchJsonBatch(args, out, err);
            }
            if ("apply-patch-json".equals(command)) {
                return applyPatchJson(args, out, err);
            }
            if ("apply-patch-json-batch".equals(command)) {
                return applyPatchJsonBatch(args, out, err);
            }
            if ("export-ai-json-spec".equals(command)) {
                return exportAiJsonSpec(args, out, err);
            }
            if ("detect-ai-json-kind".equals(command)) {
                return detectAiJsonKind(args, out, err);
            }
            if ("detect-ai-json-kind-batch".equals(command)) {
                return detectAiJsonKindBatch(args, out, err);
            }
            if ("import-ai-json".equals(command)) {
                return importAiJson(args, out, err);
            }
            if ("import-external".equals(command)) {
                return importExternal(args, out, err);
            }
            if ("export-xlsx".equals(command)) {
                return exportXlsx(args, out, err);
            }
            if ("export-xlsx-batch".equals(command)) {
                return exportXlsxBatch(args, out, err);
            }
            if ("validate-xlsx".equals(command)) {
                return validateXlsx(args, out, err);
            }
            if ("validate-xlsx-batch".equals(command)) {
                return validateXlsxBatch(args, out, err);
            }
            if ("import-xlsx".equals(command)) {
                return importXlsx(args, out, err);
            }
            if ("import-xlsx-batch".equals(command)) {
                return importXlsxBatch(args, out, err);
            }
            if ("merge-xlsx".equals(command)) {
                return mergeXlsx(args, out, err);
            }
            if ("merge-xlsx-batch".equals(command)) {
                return mergeXlsxBatch(args, out, err);
            }
            return usageError(err, "unknown command: " + command, true);
        } catch (IOException ex) {
            return commandError(err, "I/O error: " + ex.getMessage());
        } catch (RuntimeException ex) {
            return commandError(err, "command failed: " + ex.getMessage());
        }
    }

    private int validateXml(String[] args, PrintStream out, PrintStream err) throws IOException {
        if (args.length < 2) {
            return usageError(err, "validate-xml requires <input.xml>");
        }
        ProjectModel model = importXml(args[1]);
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

    private int validateXmlBatch(String[] args, PrintStream out, PrintStream err) throws IOException {
        if (args.length < 2) {
            return usageError(err, "validate-xml-batch requires <input.xml>...");
        }
        int exitCode = 0;
        for (int index = 1; index < args.length; index++) {
            ProjectModel model = importXml(args[index]);
            List<ValidationIssue> issues = msProjectXml.validateProjectModel(model);
            int errorCount = 0;
            for (ValidationIssue issue : issues) {
                if ("error".equals(issue.level)) {
                    errorCount++;
                }
            }
            out.println(args[index] + "\tissues=" + issues.size() + "\terrors=" + errorCount);
            if (errorCount > 0) {
                exitCode = 1;
            }
        }
        return exitCode;
    }

    private int exportMermaid(String[] args, PrintStream out, PrintStream err) throws IOException {
        if (args.length < 2) {
            return usageError(err, "export-mermaid requires <input.xml>");
        }
        out.print(msProjectXml.exportMermaidGantt(importXml(args[1])));
        return 0;
    }

    private int exportMermaidBatch(String[] args, PrintStream out, PrintStream err) throws IOException {
        if (args.length < 4) {
            return usageError(err, "export-mermaid-batch requires <outputRoot.dir> <input.xml> <name> [<input.xml> <name>]...");
        }
        if (((args.length - 2) % 2) != 0) {
            return usageError(err, "export-mermaid-batch requires <input.xml> <name> pairs");
        }
        Path outputRoot = Paths.get(args[1]);
        for (int index = 2; index < args.length; index += 2) {
            String input = args[index];
            String name = args[index + 1];
            String mermaidText = msProjectXml.exportMermaidGantt(importXml(input));
            Path outputFile = outputRoot.resolve(name + ".mmd");
            Path parent = outputFile.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.write(outputFile, mermaidText.getBytes(StandardCharsets.UTF_8));
            out.println("wrote " + outputFile);
        }
        return 0;
    }

    private int exportWbsMarkdown(String[] args, PrintStream out, PrintStream err) throws IOException {
        if (args.length < 2) {
            return usageError(err, "export-wbs-markdown requires <input.xml> [<beforeDays> [<afterDays> [<displayMode> [<progressMode> [<holidayDatesCsv>]]]]]");
        }
        out.print(msProjectXml.exportWbsMarkdown(importXml(args[1]), parseWbsMarkdownOptions(args, 2)));
        return 0;
    }

    private int exportWbsMarkdownBatch(String[] args, PrintStream out, PrintStream err) throws IOException {
        if (args.length < 4) {
            return usageError(err,
                    "export-wbs-markdown-batch requires <outputRoot.dir> <input.xml> <name> [<input.xml> <name>]... [-- <beforeDays> [<afterDays> [<displayMode> [<progressMode> [<holidayDatesCsv>]]]]]");
        }
        int delimiterIndex = findArg(args, "--");
        int pairEnd = delimiterIndex >= 0 ? delimiterIndex : args.length;
        if (((pairEnd - 2) % 2) != 0) {
            return usageError(err, "export-wbs-markdown-batch requires <input.xml> <name> pairs");
        }
        WbsMarkdownOptions options = parseWbsMarkdownOptions(args, delimiterIndex >= 0 ? delimiterIndex + 1 : args.length);
        Path outputRoot = Paths.get(args[1]);
        for (int index = 2; index < pairEnd; index += 2) {
            String input = args[index];
            String name = args[index + 1];
            String markdownText = msProjectXml.exportWbsMarkdown(importXml(input), options);
            Path outputFile = outputRoot.resolve(name + ".md");
            Path parent = outputFile.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.write(outputFile, markdownText.getBytes(StandardCharsets.UTF_8));
            out.println("wrote " + outputFile);
        }
        return 0;
    }

    private int exportDailySvg(String[] args, PrintStream out, PrintStream err) throws IOException {
        if (args.length < 2) {
            return usageError(err, "export-daily-svg requires <input.xml> [<labelMode>]");
        }
        out.print(msProjectXml.exportNativeSvg(importXml(args[1]), parseNativeSvgOptions(args, 2)));
        return 0;
    }

    private int exportDailySvgBatch(String[] args, PrintStream out, PrintStream err) throws IOException {
        if (args.length < 4) {
            return usageError(err, "export-daily-svg-batch requires <outputRoot.dir> <input.xml> <name> [<input.xml> <name>]... [-- <labelMode>]");
        }
        int delimiterIndex = findArg(args, "--");
        int pairEnd = delimiterIndex >= 0 ? delimiterIndex : args.length;
        if (((pairEnd - 2) % 2) != 0) {
            return usageError(err, "export-daily-svg-batch requires <input.xml> <name> pairs");
        }
        NativeSvgOptions options = parseNativeSvgOptions(args, delimiterIndex >= 0 ? delimiterIndex + 1 : args.length);
        Path outputRoot = Paths.get(args[1]);
        for (int index = 2; index < pairEnd; index += 2) {
            String input = args[index];
            String name = args[index + 1];
            String svgText = msProjectXml.exportNativeSvg(importXml(input), options);
            Path outputFile = outputRoot.resolve(name + ".svg");
            Path parent = outputFile.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.write(outputFile, svgText.getBytes(StandardCharsets.UTF_8));
            out.println("wrote " + outputFile);
        }
        return 0;
    }

    private int exportWeeklySvg(String[] args, PrintStream out, PrintStream err) throws IOException {
        if (args.length < 2) {
            return usageError(err, "export-weekly-svg requires <input.xml> [<labelMode>]");
        }
        out.print(msProjectXml.exportWeeklyNativeSvg(importXml(args[1]), parseNativeSvgOptions(args, 2)));
        return 0;
    }

    private int exportWeeklySvgBatch(String[] args, PrintStream out, PrintStream err) throws IOException {
        if (args.length < 4) {
            return usageError(err, "export-weekly-svg-batch requires <outputRoot.dir> <input.xml> <name> [<input.xml> <name>]... [-- <labelMode>]");
        }
        int delimiterIndex = findArg(args, "--");
        int pairEnd = delimiterIndex >= 0 ? delimiterIndex : args.length;
        if (((pairEnd - 2) % 2) != 0) {
            return usageError(err, "export-weekly-svg-batch requires <input.xml> <name> pairs");
        }
        NativeSvgOptions options = parseNativeSvgOptions(args, delimiterIndex >= 0 ? delimiterIndex + 1 : args.length);
        Path outputRoot = Paths.get(args[1]);
        for (int index = 2; index < pairEnd; index += 2) {
            String input = args[index];
            String name = args[index + 1];
            String svgText = msProjectXml.exportWeeklyNativeSvg(importXml(input), options);
            Path outputFile = outputRoot.resolve(name + ".svg");
            Path parent = outputFile.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.write(outputFile, svgText.getBytes(StandardCharsets.UTF_8));
            out.println("wrote " + outputFile);
        }
        return 0;
    }

    private int exportMonthlySvgZip(String[] args, PrintStream out, PrintStream err) throws IOException {
        if (args.length < 3) {
            return usageError(err, "export-monthly-svg-zip requires <input.xml> <output.zip> [<holidayDatesCsv> [<labelMode>]]");
        }
        MonthlyCalendarSvgArchive archive = msProjectXml.exportMonthlyWbsCalendarSvgArchive(importXml(args[1]), parseMonthlySvgOptions(args, 3));
        Files.write(Paths.get(args[2]), archive.zipBytes);
        out.println("wrote " + args[2] + " (" + archive.entries.size() + " entries)");
        return 0;
    }

    private int exportMonthlySvgZipBatch(String[] args, PrintStream out, PrintStream err) throws IOException {
        if (args.length < 4) {
            return usageError(err,
                    "export-monthly-svg-zip-batch requires <outputRoot.dir> <input.xml> <name> [<input.xml> <name>]... [-- <holidayDatesCsv> [<labelMode>]]");
        }
        int delimiterIndex = findArg(args, "--");
        int pairEnd = delimiterIndex >= 0 ? delimiterIndex : args.length;
        if (((pairEnd - 2) % 2) != 0) {
            return usageError(err, "export-monthly-svg-zip-batch requires <input.xml> <name> pairs");
        }
        NativeSvgOptions options = parseMonthlySvgOptions(args, delimiterIndex >= 0 ? delimiterIndex + 1 : args.length);
        Path outputRoot = Paths.get(args[1]);
        for (int index = 2; index < pairEnd; index += 2) {
            String input = args[index];
            String name = args[index + 1];
            MonthlyCalendarSvgArchive archive = msProjectXml.exportMonthlyWbsCalendarSvgArchive(importXml(input), options);
            Path outputFile = outputRoot.resolve(name + ".zip");
            Path parent = outputFile.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.write(outputFile, archive.zipBytes);
            out.println("wrote " + outputFile + " (" + archive.entries.size() + " entries)");
        }
        return 0;
    }

    private int exportReportBundle(String[] args, PrintStream out, PrintStream err) throws IOException {
        if (args.length < 3) {
            return usageError(err,
                    "export-report-bundle requires <input.xml> <output.zip> [<beforeDays> [<afterDays> [<displayMode> [<progressMode> [<holidayDatesCsv> [<labelMode>]]]]]]");
        }
        WbsMarkdownOptions markdownOptions = parseWbsMarkdownOptions(args, 3);
        WbsExportOptions xlsxOptions = parseWbsXlsxOptions(args, 3);
        NativeSvgOptions svgOptions = parseReportSvgOptions(args, 3);
        CoreApiReportAdapters.ReportBundle bundle = reportAdapters.report.all.export(importXml(args[1]), markdownOptions, xlsxOptions, svgOptions);
        Files.write(Paths.get(args[2]), bundle.zipBytes);
        out.println("wrote " + args[2] + " (" + bundle.entries.size() + " entries)");
        return 0;
    }

    private int exportReportBundleBatch(String[] args, PrintStream out, PrintStream err) throws IOException {
        if (args.length < 4) {
            return usageError(err,
                    "export-report-bundle-batch requires <outputRoot.dir> <input.xml> <name> [<input.xml> <name>]... [-- <beforeDays> [<afterDays> [<displayMode> [<progressMode> [<holidayDatesCsv> [<labelMode>]]]]]]");
        }
        int delimiterIndex = findArg(args, "--");
        int pairEnd = delimiterIndex >= 0 ? delimiterIndex : args.length;
        if (((pairEnd - 2) % 2) != 0) {
            return usageError(err, "export-report-bundle-batch requires <input.xml> <name> pairs");
        }
        WbsMarkdownOptions markdownOptions = parseWbsMarkdownOptions(args, delimiterIndex >= 0 ? delimiterIndex + 1 : args.length);
        WbsExportOptions xlsxOptions = parseWbsXlsxOptions(args, delimiterIndex >= 0 ? delimiterIndex + 1 : args.length);
        NativeSvgOptions svgOptions = parseReportSvgOptions(args, delimiterIndex >= 0 ? delimiterIndex + 1 : args.length);
        Path outputRoot = Paths.get(args[1]);
        for (int index = 2; index < pairEnd; index += 2) {
            String input = args[index];
            String name = args[index + 1];
            CoreApiReportAdapters.ReportBundle bundle = reportAdapters.report.all.export(importXml(input), markdownOptions, xlsxOptions, svgOptions);
            Path outputFile = outputRoot.resolve(name + ".zip");
            Path parent = outputFile.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.write(outputFile, bundle.zipBytes);
            out.println("wrote " + outputFile + " (" + bundle.entries.size() + " entries)");
        }
        return 0;
    }

    private int exportReportDir(String[] args, PrintStream out, PrintStream err) throws IOException {
        if (args.length < 3) {
            return usageError(err,
                    "export-report-dir requires <input.xml> <output.dir> [<beforeDays> [<afterDays> [<displayMode> [<progressMode> [<holidayDatesCsv> [<labelMode>]]]]]]");
        }
        WbsMarkdownOptions markdownOptions = parseWbsMarkdownOptions(args, 3);
        WbsExportOptions xlsxOptions = parseWbsXlsxOptions(args, 3);
        NativeSvgOptions svgOptions = parseReportSvgOptions(args, 3);
        CoreApiReportAdapters.ReportBundle bundle = reportAdapters.report.all.export(importXml(args[1]), markdownOptions, xlsxOptions, svgOptions);
        Path outputDir = Paths.get(args[2]);
        for (jp.igapyon.mikuproject.coreapi.CoreApiReport.ReportEntry entry : bundle.entries) {
            Path outputFile = outputDir.resolve(entry.name);
            Path parent = outputFile.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.write(outputFile, entry.data);
        }
        out.println("wrote " + args[2] + " (" + bundle.entries.size() + " entries)");
        return 0;
    }

    private int exportReportDirBatch(String[] args, PrintStream out, PrintStream err) throws IOException {
        if (args.length < 4) {
            return usageError(err,
                    "export-report-dir-batch requires <outputRoot.dir> <input.xml> <name> [<input.xml> <name>]... [-- <beforeDays> [<afterDays> [<displayMode> [<progressMode> [<holidayDatesCsv> [<labelMode>]]]]]]");
        }
        int delimiterIndex = findArg(args, "--");
        int pairEnd = delimiterIndex >= 0 ? delimiterIndex : args.length;
        if (((pairEnd - 2) % 2) != 0) {
            return usageError(err, "export-report-dir-batch requires <input.xml> <name> pairs");
        }
        WbsMarkdownOptions markdownOptions = parseWbsMarkdownOptions(args, delimiterIndex >= 0 ? delimiterIndex + 1 : args.length);
        WbsExportOptions xlsxOptions = parseWbsXlsxOptions(args, delimiterIndex >= 0 ? delimiterIndex + 1 : args.length);
        NativeSvgOptions svgOptions = parseReportSvgOptions(args, delimiterIndex >= 0 ? delimiterIndex + 1 : args.length);
        Path outputRoot = Paths.get(args[1]);
        for (int index = 2; index < pairEnd; index += 2) {
            String input = args[index];
            String name = args[index + 1];
            CoreApiReportAdapters.ReportBundle bundle = reportAdapters.report.all.export(importXml(input), markdownOptions, xlsxOptions, svgOptions);
            Path outputDir = outputRoot.resolve(name);
            writeReportEntries(outputDir, bundle);
            out.println("wrote " + outputDir + " (" + bundle.entries.size() + " entries)");
        }
        return 0;
    }

    private int exportWbsXlsx(String[] args, PrintStream out, PrintStream err) throws IOException {
        if (args.length < 3) {
            return usageError(err,
                    "export-wbs-xlsx requires <input.xml> <output.xlsxbin> [<beforeDays> [<afterDays> [<displayMode> [<progressMode> [<holidayDatesCsv>]]]]]");
        }
        byte[] bytes = reportAdapters.report.wbsXlsx.exportBytes(importXml(args[1]), parseWbsXlsxOptions(args, 3));
        Files.write(Paths.get(args[2]), bytes);
        out.println("wrote " + args[2] + " (" + bytes.length + " bytes)");
        return 0;
    }

    private int exportWbsXlsxBatch(String[] args, PrintStream out, PrintStream err) throws IOException {
        if (args.length < 4) {
            return usageError(err,
                    "export-wbs-xlsx-batch requires <outputRoot.dir> <input.xml> <name> [<input.xml> <name>]... [-- <beforeDays> [<afterDays> [<displayMode> [<progressMode> [<holidayDatesCsv>]]]]]");
        }
        int delimiterIndex = findArg(args, "--");
        int pairEnd = delimiterIndex >= 0 ? delimiterIndex : args.length;
        if (((pairEnd - 2) % 2) != 0) {
            return usageError(err, "export-wbs-xlsx-batch requires <input.xml> <name> pairs");
        }
        WbsExportOptions options = parseWbsXlsxOptions(args, delimiterIndex >= 0 ? delimiterIndex + 1 : args.length);
        Path outputRoot = Paths.get(args[1]);
        for (int index = 2; index < pairEnd; index += 2) {
            String input = args[index];
            String name = args[index + 1];
            byte[] bytes = reportAdapters.report.wbsXlsx.exportBytes(importXml(input), options);
            Path outputFile = outputRoot.resolve(name + ".xlsxbin");
            Path parent = outputFile.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.write(outputFile, bytes);
            out.println("wrote " + outputFile + " (" + bytes.length + " bytes)");
        }
        return 0;
    }

    private int exportWorkbookJson(String[] args, PrintStream out, PrintStream err) throws IOException {
        if (args.length < 2) {
            return usageError(err, "export-workbook-json requires <input.xml>");
        }
        out.print(exportWorkbookJsonText(importXml(args[1])));
        return 0;
    }

    private int exportWorkbookJsonBatch(String[] args, PrintStream out, PrintStream err) throws IOException {
        if (args.length < 4) {
            return usageError(err, "export-workbook-json-batch requires <outputRoot.dir> <input.xml> <name> [<input.xml> <name>]...");
        }
        if (((args.length - 2) % 2) != 0) {
            return usageError(err, "export-workbook-json-batch requires <input.xml> <name> pairs");
        }
        Path outputRoot = Paths.get(args[1]);
        for (int index = 2; index < args.length; index += 2) {
            String input = args[index];
            String name = args[index + 1];
            String jsonText = exportWorkbookJsonText(importXml(input));
            Path outputFile = outputRoot.resolve(name + ".json");
            Path parent = outputFile.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.write(outputFile, jsonText.getBytes(StandardCharsets.UTF_8));
            out.println("wrote " + outputFile);
        }
        return 0;
    }

    private int exportProjectOverviewView(String[] args, PrintStream out, PrintStream err) throws IOException {
        if (args.length < 2) {
            return usageError(err, "export-project-overview-view requires <input.xml>");
        }
        out.print(jsonUtil.stringifyJson(msProjectXml.exportProjectOverviewView(importXml(args[1]))));
        return 0;
    }

    private int exportProjectOverviewViewBatch(String[] args, PrintStream out, PrintStream err) throws IOException {
        if (args.length < 4) {
            return usageError(err, "export-project-overview-view-batch requires <outputRoot.dir> <input.xml> <name> [<input.xml> <name>]...");
        }
        if (((args.length - 2) % 2) != 0) {
            return usageError(err, "export-project-overview-view-batch requires <input.xml> <name> pairs");
        }
        Path outputRoot = Paths.get(args[1]);
        for (int index = 2; index < args.length; index += 2) {
            String input = args[index];
            String name = args[index + 1];
            String jsonText = jsonUtil.stringifyJson(msProjectXml.exportProjectOverviewView(importXml(input)));
            Path outputFile = outputRoot.resolve(name + ".json");
            Path parent = outputFile.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.write(outputFile, jsonText.getBytes(StandardCharsets.UTF_8));
            out.println("wrote " + outputFile);
        }
        return 0;
    }

    private int exportPhaseDetailView(String[] args, PrintStream out, PrintStream err) throws IOException {
        if (args.length < 2) {
            return usageError(err, "export-phase-detail-view requires <input.xml> [<phaseUid> [<mode> [<rootUid> [<maxDepth>]]]]");
        }
        ProjectModel model = importXml(args[1]);
        String requestedPhaseUid = args.length >= 3 ? args[2] : null;
        String mode = args.length >= 4 ? args[3] : null;
        String rootUid = args.length >= 5 ? args[4] : null;
        Integer maxDepth = args.length >= 6 ? Integer.valueOf(Integer.parseInt(args[5])) : null;
        out.print(jsonUtil.stringifyJson(msProjectXml.exportPhaseDetailView(model, requestedPhaseUid, mode, rootUid, maxDepth)));
        return 0;
    }

    private int exportPhaseDetailViewBatch(String[] args, PrintStream out, PrintStream err) throws IOException {
        if (args.length < 4) {
            return usageError(err,
                    "export-phase-detail-view-batch requires <outputRoot.dir> <input.xml> <name> [<input.xml> <name>]... [-- <phaseUid> [<mode> [<rootUid> [<maxDepth>]]]]");
        }
        int delimiterIndex = findArg(args, "--");
        int pairEnd = delimiterIndex >= 0 ? delimiterIndex : args.length;
        if (((pairEnd - 2) % 2) != 0) {
            return usageError(err, "export-phase-detail-view-batch requires <input.xml> <name> pairs");
        }
        String requestedPhaseUid = delimiterIndex >= 0 && delimiterIndex + 1 < args.length ? args[delimiterIndex + 1] : null;
        String mode = delimiterIndex >= 0 && delimiterIndex + 2 < args.length ? args[delimiterIndex + 2] : null;
        String rootUid = delimiterIndex >= 0 && delimiterIndex + 3 < args.length ? args[delimiterIndex + 3] : null;
        Integer maxDepth = delimiterIndex >= 0 && delimiterIndex + 4 < args.length ? Integer.valueOf(Integer.parseInt(args[delimiterIndex + 4])) : null;
        Path outputRoot = Paths.get(args[1]);
        for (int index = 2; index < pairEnd; index += 2) {
            String input = args[index];
            String name = args[index + 1];
            ProjectModel model = importXml(input);
            String jsonText = jsonUtil.stringifyJson(msProjectXml.exportPhaseDetailView(model, requestedPhaseUid, mode, rootUid, maxDepth));
            Path outputFile = outputRoot.resolve(name + ".json");
            Path parent = outputFile.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.write(outputFile, jsonText.getBytes(StandardCharsets.UTF_8));
            out.println("wrote " + outputFile);
        }
        return 0;
    }

    private int exportTaskEditView(String[] args, PrintStream out, PrintStream err) throws IOException {
        if (args.length < 3) {
            return usageError(err, "export-task-edit-view requires <input.xml> <taskUid>");
        }
        out.print(jsonUtil.stringifyJson(msProjectXml.exportTaskEditView(importXml(args[1]), args[2])));
        return 0;
    }

    private int exportTaskEditViewBatch(String[] args, PrintStream out, PrintStream err) throws IOException {
        if (args.length < 5) {
            return usageError(err,
                    "export-task-edit-view-batch requires <outputRoot.dir> <taskUid> <input.xml> <name> [<input.xml> <name>]...");
        }
        if (((args.length - 3) % 2) != 0) {
            return usageError(err, "export-task-edit-view-batch requires <input.xml> <name> pairs");
        }
        Path outputRoot = Paths.get(args[1]);
        String taskUid = args[2];
        for (int index = 3; index < args.length; index += 2) {
            String input = args[index];
            String name = args[index + 1];
            String jsonText = jsonUtil.stringifyJson(msProjectXml.exportTaskEditView(importXml(input), taskUid));
            Path outputFile = outputRoot.resolve(name + ".json");
            Path parent = outputFile.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.write(outputFile, jsonText.getBytes(StandardCharsets.UTF_8));
            out.println("wrote " + outputFile);
        }
        return 0;
    }

    private int exportProjectDraftRequest(String[] args, PrintStream out, PrintStream err) {
        if (args.length < 3) {
            return usageError(err,
                    "export-project-draft-request requires <name> <plannedStart> [<goal> [<teamCount> [<mustHavePhasesCsv> [<mustHaveMilestonesCsv>]]]]");
        }
        String name = args[1];
        String plannedStart = args[2];
        String goal = args.length >= 4 ? args[3] : null;
        Integer teamCount = args.length >= 5 && args[4] != null && args[4].length() > 0 ? Integer.valueOf(Integer.parseInt(args[4])) : null;
        List<String> mustHavePhases = args.length >= 6 ? splitCsv(args[5]) : null;
        List<String> mustHaveMilestones = args.length >= 7 ? splitCsv(args[6]) : null;
        out.print(jsonUtil.stringifyJson(msProjectXml.buildProjectDraftRequest(name, plannedStart, goal, teamCount, mustHavePhases, mustHaveMilestones)));
        return 0;
    }

    private int validateWorkbookJson(String[] args, PrintStream out, PrintStream err) throws IOException {
        if (args.length < 2) {
            return usageError(err, "validate-workbook-json requires <input.json>");
        }
        Object json = parseJsonFile(args[1]);
        jp.igapyon.mikuproject.projectworkbookjson.ProjectWorkbookJsonValidate.ValidationResult result =
                workbookJson.validateWorkbookJsonDocument(json);
        for (WorkbookJsonWarning warning : result.warnings) {
            out.println("warning\t" + safe(warning.message));
        }
        out.println("warnings=" + result.warnings.size());
        return 0;
    }

    private int validateWorkbookJsonBatch(String[] args, PrintStream out, PrintStream err) throws IOException {
        if (args.length < 2) {
            return usageError(err, "validate-workbook-json-batch requires <input.json>...");
        }
        for (int index = 1; index < args.length; index++) {
            Object json = parseJsonFile(args[index]);
            jp.igapyon.mikuproject.projectworkbookjson.ProjectWorkbookJsonValidate.ValidationResult result =
                    workbookJson.validateWorkbookJsonDocument(json);
            out.println(args[index] + "\twarnings=" + result.warnings.size());
        }
        return 0;
    }

    private int importWorkbookJson(String[] args, PrintStream out, PrintStream err) throws IOException {
        if (args.length < 3) {
            return usageError(err, "import-workbook-json requires <input.json> <output.xml>");
        }
        Object json = parseJsonFile(args[1]);
        jp.igapyon.mikuproject.projectworkbookjson.ProjectWorkbookJsonImport.ImportAsProjectModelResult result =
                workbookJson.importProjectWorkbookJsonAsProjectModel(json);
        writeXml(args[2], result.model);
        out.println("wrote " + args[2] + " (warnings=" + result.warnings.size() + ")");
        return 0;
    }

    private int importWorkbookJsonBatch(String[] args, PrintStream out, PrintStream err) throws IOException {
        if (args.length < 4) {
            return usageError(err, "import-workbook-json-batch requires <outputRoot.dir> <input.json> <name> [<input.json> <name>]...");
        }
        if (((args.length - 2) % 2) != 0) {
            return usageError(err, "import-workbook-json-batch requires <input.json> <name> pairs");
        }
        Path outputRoot = Paths.get(args[1]);
        for (int index = 2; index < args.length; index += 2) {
            Object json = parseJsonFile(args[index]);
            jp.igapyon.mikuproject.projectworkbookjson.ProjectWorkbookJsonImport.ImportAsProjectModelResult result =
                    workbookJson.importProjectWorkbookJsonAsProjectModel(json);
            Path outputFile = outputRoot.resolve(args[index + 1] + ".xml");
            Path parent = outputFile.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            writeXml(outputFile.toString(), result.model);
            out.println("wrote " + outputFile + " (warnings=" + result.warnings.size() + ")");
        }
        return 0;
    }

    private int mergeWorkbookJson(String[] args, PrintStream out, PrintStream err) throws IOException {
        if (args.length < 4) {
            return usageError(err, "merge-workbook-json requires <base.xml> <input.json> <output.xml>");
        }
        ProjectModel baseModel = importXml(args[1]);
        Object json = parseJsonFile(args[2]);
        jp.igapyon.mikuproject.projectworkbookjson.ProjectWorkbookJsonImport.ImportResult result =
                workbookJson.importProjectWorkbookJson(json, baseModel);
        writeXml(args[3], result.model);
        out.println("wrote " + args[3] + " (changes=" + result.changes.size() + ", warnings=" + result.warnings.size() + ")");
        return 0;
    }

    private int mergeWorkbookJsonBatch(String[] args, PrintStream out, PrintStream err) throws IOException {
        if (args.length < 5) {
            return usageError(err, "merge-workbook-json-batch requires <base.xml> <outputRoot.dir> <input.json> <name> [<input.json> <name>]...");
        }
        if (((args.length - 3) % 2) != 0) {
            return usageError(err, "merge-workbook-json-batch requires <input.json> <name> pairs");
        }
        ProjectModel baseModel = importXml(args[1]);
        Path outputRoot = Paths.get(args[2]);
        for (int index = 3; index < args.length; index += 2) {
            Object json = parseJsonFile(args[index]);
            jp.igapyon.mikuproject.projectworkbookjson.ProjectWorkbookJsonImport.ImportResult result =
                    workbookJson.importProjectWorkbookJson(json, baseModel);
            Path outputFile = outputRoot.resolve(args[index + 1] + ".xml");
            Path parent = outputFile.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            writeXml(outputFile.toString(), result.model);
            out.println("wrote " + outputFile + " (changes=" + result.changes.size() + ", warnings=" + result.warnings.size() + ")");
        }
        return 0;
    }

    private int validatePatchJson(String[] args, PrintStream out, PrintStream err) throws IOException {
        if (args.length < 2) {
            return usageError(err, "validate-patch-json requires <input.json>");
        }
        Object json = parseJsonFile(args[1]);
        jp.igapyon.mikuproject.projectpatchjson.ProjectPatchJsonCore.ValidationResult result = patchJson.validatePatchDocument(json);
        for (PatchWarning warning : result.warnings) {
            out.println("warning\t" + safe(warning.message));
        }
        out.println("operations=" + result.document.operations.size() + ", warnings=" + result.warnings.size());
        return 0;
    }

    private int validatePatchJsonBatch(String[] args, PrintStream out, PrintStream err) throws IOException {
        if (args.length < 2) {
            return usageError(err, "validate-patch-json-batch requires <input.json>...");
        }
        for (int index = 1; index < args.length; index++) {
            Object json = parseJsonFile(args[index]);
            jp.igapyon.mikuproject.projectpatchjson.ProjectPatchJsonCore.ValidationResult result = patchJson.validatePatchDocument(json);
            out.println(args[index] + "\toperations=" + result.document.operations.size() + "\twarnings=" + result.warnings.size());
        }
        return 0;
    }

    private int applyPatchJson(String[] args, PrintStream out, PrintStream err) throws IOException {
        if (args.length < 4) {
            return usageError(err, "apply-patch-json requires <base.xml> <patch.json> <output.xml>");
        }
        ProjectModel baseModel = importXml(args[1]);
        Object json = parseJsonFile(args[2]);
        jp.igapyon.mikuproject.projectpatchjson.ProjectPatchJsonCore.ImportResult result =
                patchJson.importProjectPatchJson(json, baseModel);
        writeXml(args[3], result.model);
        out.println("wrote " + args[3] + " (changes=" + result.changes.size() + ", warnings=" + result.warnings.size() + ")");
        return 0;
    }

    private int applyPatchJsonBatch(String[] args, PrintStream out, PrintStream err) throws IOException {
        if (args.length < 5) {
            return usageError(err, "apply-patch-json-batch requires <base.xml> <outputRoot.dir> <patch.json> <name> [<patch.json> <name>]...");
        }
        if (((args.length - 3) % 2) != 0) {
            return usageError(err, "apply-patch-json-batch requires <patch.json> <name> pairs");
        }
        ProjectModel baseModel = importXml(args[1]);
        Path outputRoot = Paths.get(args[2]);
        for (int index = 3; index < args.length; index += 2) {
            Object json = parseJsonFile(args[index]);
            jp.igapyon.mikuproject.projectpatchjson.ProjectPatchJsonCore.ImportResult result =
                    patchJson.importProjectPatchJson(json, baseModel);
            Path outputFile = outputRoot.resolve(args[index + 1] + ".xml");
            Path parent = outputFile.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            writeXml(outputFile.toString(), result.model);
            out.println("wrote " + outputFile + " (changes=" + result.changes.size() + ", warnings=" + result.warnings.size() + ")");
        }
        return 0;
    }

    private int exportAiJsonSpec(String[] args, PrintStream out, PrintStream err) {
        out.print(coreApiImport.getAiJsonSpecText());
        return 0;
    }

    private int detectAiJsonKind(String[] args, PrintStream out, PrintStream err) throws IOException {
        if (args.length < 2) {
            return usageError(err, "detect-ai-json-kind requires <input.txt>");
        }
        CoreApiAiJsonParseResult parsed = coreApiImport.parseAiJsonText(readText(args[1]));
        out.println("kind=" + safe(parsed.kind));
        return parsed.kind == null || parsed.kind.length() == 0 ? 1 : 0;
    }

    private int detectAiJsonKindBatch(String[] args, PrintStream out, PrintStream err) throws IOException {
        if (args.length < 2) {
            return usageError(err, "detect-ai-json-kind-batch requires <input.txt>...");
        }
        int exitCode = 0;
        for (int index = 1; index < args.length; index++) {
            CoreApiAiJsonParseResult parsed = coreApiImport.parseAiJsonText(readText(args[index]));
            out.println(args[index] + "\tkind=" + safe(parsed.kind));
            if (parsed.kind == null || parsed.kind.length() == 0) {
                exitCode = 1;
            }
        }
        return exitCode;
    }

    private int importAiJson(String[] args, PrintStream out, PrintStream err) throws IOException {
        if (args.length < 3) {
            return usageError(err, "import-ai-json requires <input.txt> <output.xml> [<base.xml>]");
        }
        String sourceText = readText(args[1]);
        CoreApiAiJsonParseResult parsed = args.length >= 4
                ? coreApiAiJson.importAiJsonText(sourceText, importXml(args[3]))
                : coreApiAiJson.importAiJsonText(sourceText);
        if (parsed.result == null || parsed.result.model == null) {
            return commandError(err, "AI JSON import result is empty");
        }
        writeXml(args[2], parsed.result.model);
        out.println("wrote " + args[2] + " (kind=" + safe(parsed.kind) + ", mode=" + safe(parsed.result.mode) + ")");
        return 0;
    }

    private int importExternal(String[] args, PrintStream out, PrintStream err) throws IOException {
        if (args.length < 5) {
            return usageError(err, "import-external requires <format> <mode> <input> <output.xml> [<base.xml>]");
        }
        CoreApiExternalImport.ExternalImportInput input = new CoreApiExternalImport.ExternalImportInput();
        input.source = new CoreApiExternalImport.ExternalImportSource();
        input.source.format = args[1];
        input.mode = args[2];
        fillExternalSource(input.source, args[3]);
        if (args.length >= 6) {
            input.baseModel = importXml(args[5]);
        }
        CoreApiImportResult result = coreApiImport.importExternal(input);
        writeXml(args[4], result.model);
        out.println("wrote " + args[4] + " (kind=" + safe(result.kind) + ", mode=" + safe(result.mode)
                + ", changes=" + result.changes.size() + ", warnings=" + result.warnings.size() + ")");
        return 0;
    }

    private int exportXlsx(String[] args, PrintStream out, PrintStream err) throws IOException {
        if (args.length < 3) {
            return usageError(err, "export-xlsx requires <input.xml> <output.xlsxbin>");
        }
        byte[] bytes = workbookXlsx.encodeWorkbook(workbookXlsx.exportWorkbook(importXml(args[1])));
        Files.write(Paths.get(args[2]), bytes);
        out.println("wrote " + args[2] + " (" + bytes.length + " bytes)");
        return 0;
    }

    private int exportXlsxBatch(String[] args, PrintStream out, PrintStream err) throws IOException {
        if (args.length < 4) {
            return usageError(err, "export-xlsx-batch requires <outputRoot.dir> <input.xml> <name> [<input.xml> <name>]...");
        }
        if (((args.length - 2) % 2) != 0) {
            return usageError(err, "export-xlsx-batch requires <input.xml> <name> pairs");
        }
        Path outputRoot = Paths.get(args[1]);
        for (int index = 2; index < args.length; index += 2) {
            String input = args[index];
            String name = args[index + 1];
            byte[] bytes = workbookXlsx.encodeWorkbook(workbookXlsx.exportWorkbook(importXml(input)));
            Path outputFile = outputRoot.resolve(name + ".xlsxbin");
            Path parent = outputFile.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.write(outputFile, bytes);
            out.println("wrote " + outputFile + " (" + bytes.length + " bytes)");
        }
        return 0;
    }

    private int validateXlsx(String[] args, PrintStream out, PrintStream err) throws IOException {
        if (args.length < 2) {
            return usageError(err, "validate-xlsx requires <input.xlsxbin>");
        }
        byte[] bytes = Files.readAllBytes(Paths.get(args[1]));
        out.println("sheets=" + workbookXlsx.decodeWorkbook(bytes).sheets.size());
        return 0;
    }

    private int validateXlsxBatch(String[] args, PrintStream out, PrintStream err) throws IOException {
        if (args.length < 2) {
            return usageError(err, "validate-xlsx-batch requires <input.xlsxbin>...");
        }
        for (int index = 1; index < args.length; index++) {
            byte[] bytes = Files.readAllBytes(Paths.get(args[index]));
            out.println(args[index] + "\tsheets=" + workbookXlsx.decodeWorkbook(bytes).sheets.size());
        }
        return 0;
    }

    private int importXlsx(String[] args, PrintStream out, PrintStream err) throws IOException {
        if (args.length < 3) {
            return usageError(err, "import-xlsx requires <input.xlsxbin> <output.xml>");
        }
        byte[] bytes = Files.readAllBytes(Paths.get(args[1]));
        ProjectModel model = workbookXlsx.importAsProjectModel(workbookXlsx.decodeWorkbook(bytes));
        writeXml(args[2], model);
        out.println("wrote " + args[2]);
        return 0;
    }

    private int importXlsxBatch(String[] args, PrintStream out, PrintStream err) throws IOException {
        if (args.length < 4) {
            return usageError(err, "import-xlsx-batch requires <outputRoot.dir> <input.xlsxbin> <name> [<input.xlsxbin> <name>]...");
        }
        if (((args.length - 2) % 2) != 0) {
            return usageError(err, "import-xlsx-batch requires <input.xlsxbin> <name> pairs");
        }
        Path outputRoot = Paths.get(args[1]);
        for (int index = 2; index < args.length; index += 2) {
            byte[] bytes = Files.readAllBytes(Paths.get(args[index]));
            ProjectModel model = workbookXlsx.importAsProjectModel(workbookXlsx.decodeWorkbook(bytes));
            Path outputFile = outputRoot.resolve(args[index + 1] + ".xml");
            Path parent = outputFile.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            writeXml(outputFile.toString(), model);
            out.println("wrote " + outputFile);
        }
        return 0;
    }

    private int mergeXlsx(String[] args, PrintStream out, PrintStream err) throws IOException {
        if (args.length < 4) {
            return usageError(err, "merge-xlsx requires <base.xml> <input.xlsxbin> <output.xml>");
        }
        ProjectModel baseModel = importXml(args[1]);
        byte[] bytes = Files.readAllBytes(Paths.get(args[2]));
        ProjectModel model = workbookXlsx.importIntoProjectModel(workbookXlsx.decodeWorkbook(bytes), baseModel);
        writeXml(args[3], model);
        out.println("wrote " + args[3]);
        return 0;
    }

    private int mergeXlsxBatch(String[] args, PrintStream out, PrintStream err) throws IOException {
        if (args.length < 5) {
            return usageError(err, "merge-xlsx-batch requires <base.xml> <outputRoot.dir> <input.xlsxbin> <name> [<input.xlsxbin> <name>]...");
        }
        if (((args.length - 3) % 2) != 0) {
            return usageError(err, "merge-xlsx-batch requires <input.xlsxbin> <name> pairs");
        }
        ProjectModel baseModel = importXml(args[1]);
        Path outputRoot = Paths.get(args[2]);
        for (int index = 3; index < args.length; index += 2) {
            byte[] bytes = Files.readAllBytes(Paths.get(args[index]));
            ProjectModel model = workbookXlsx.importIntoProjectModel(workbookXlsx.decodeWorkbook(bytes), baseModel);
            Path outputFile = outputRoot.resolve(args[index + 1] + ".xml");
            Path parent = outputFile.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            writeXml(outputFile.toString(), model);
            out.println("wrote " + outputFile);
        }
        return 0;
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
        byte[] bytes = Files.readAllBytes(Paths.get(path));
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private void fillExternalSource(CoreApiExternalImport.ExternalImportSource source, String path) throws IOException {
        if ("ms_project_xml".equals(source.format)) {
            source.text = readText(path);
            return;
        }
        if ("xlsx".equals(source.format)) {
            source.bytes = Files.readAllBytes(Paths.get(path));
            return;
        }
        if ("workbook_json".equals(source.format) || "patch_json".equals(source.format) || "project_draft_view".equals(source.format)) {
            source.document = parseJsonFile(path);
            return;
        }
        throw new IllegalArgumentException("unsupported external format: " + safe(source.format));
    }

    private boolean isHelp(String command) {
        return "--help".equals(command) || "-h".equals(command) || "help".equals(command);
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

    private WbsMarkdownOptions parseWbsMarkdownOptions(String[] args, int fromIndex) {
        WbsMarkdownOptions options = new WbsMarkdownOptions();
        options.displayDaysBeforeBaseDate = parseOptionalIntegerArg(args, fromIndex);
        options.displayDaysAfterBaseDate = parseOptionalIntegerArg(args, fromIndex + 1);
        options.useBusinessDaysForDisplayRange = parseOptionalCalendarModeArg(args, fromIndex + 2);
        options.useBusinessDaysForProgressBand = parseOptionalCalendarModeArg(args, fromIndex + 3);
        options.holidayDates.addAll(parseOptionalCsvArg(args, fromIndex + 4));
        return options;
    }

    private WbsExportOptions parseWbsXlsxOptions(String[] args, int fromIndex) {
        WbsExportOptions options = new WbsExportOptions();
        options.displayDaysBeforeBaseDate = parseOptionalIntegerArg(args, fromIndex);
        options.displayDaysAfterBaseDate = parseOptionalIntegerArg(args, fromIndex + 1);
        options.useBusinessDaysForDisplayRange = parseOptionalCalendarModeArg(args, fromIndex + 2);
        options.useBusinessDaysForProgressBand = parseOptionalCalendarModeArg(args, fromIndex + 3);
        options.holidayDates.addAll(parseOptionalCsvArg(args, fromIndex + 4));
        return options;
    }

    private Integer parseOptionalIntegerArg(String[] args, int index) {
        if (index >= args.length || args[index] == null || args[index].length() == 0) {
            return null;
        }
        return Integer.valueOf(Integer.parseInt(args[index]));
    }

    private Boolean parseOptionalCalendarModeArg(String[] args, int index) {
        if (index >= args.length || args[index] == null || args[index].length() == 0) {
            return null;
        }
        String value = args[index].toLowerCase();
        if ("business".equals(value)) {
            return Boolean.TRUE;
        }
        if ("calendar".equals(value)) {
            return Boolean.FALSE;
        }
        throw new IllegalArgumentException("unsupported calendar mode: " + args[index] + " (use business or calendar)");
    }

    private List<String> parseOptionalCsvArg(String[] args, int index) {
        if (index >= args.length || args[index] == null || args[index].length() == 0) {
            return new ArrayList<String>();
        }
        return splitCsv(args[index]);
    }

    private NativeSvgOptions parseNativeSvgOptions(String[] args, int fromIndex) {
        NativeSvgOptions options = new NativeSvgOptions();
        if (fromIndex < args.length && args[fromIndex] != null && args[fromIndex].length() > 0) {
            options.labelMode = args[fromIndex];
        }
        return options;
    }

    private NativeSvgOptions parseMonthlySvgOptions(String[] args, int fromIndex) {
        NativeSvgOptions options = new NativeSvgOptions();
        options.holidayDates.addAll(parseOptionalCsvArg(args, fromIndex));
        if (fromIndex + 1 < args.length && args[fromIndex + 1] != null && args[fromIndex + 1].length() > 0) {
            options.labelMode = args[fromIndex + 1];
        }
        return options;
    }

    private NativeSvgOptions parseReportSvgOptions(String[] args, int fromIndex) {
        NativeSvgOptions options = new NativeSvgOptions();
        options.holidayDates.addAll(parseOptionalCsvArg(args, fromIndex + 4));
        if (fromIndex + 5 < args.length && args[fromIndex + 5] != null && args[fromIndex + 5].length() > 0) {
            options.labelMode = args[fromIndex + 5];
        }
        return options;
    }

    private int findArg(String[] args, String value) {
        for (int index = 0; index < args.length; index++) {
            if (value.equals(args[index])) {
                return index;
            }
        }
        return -1;
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
        out.println("  validate-xml <input.xml>");
        out.println("  validate-xml-batch <input.xml>...");
        out.println("  export-mermaid <input.xml>");
        out.println("  export-mermaid-batch <outputRoot.dir> <input.xml> <name> [<input.xml> <name>]...");
        out.println("  export-wbs-markdown <input.xml> [<beforeDays> [<afterDays> [<displayMode> [<progressMode> [<holidayDatesCsv>]]]]]");
        out.println("  export-wbs-markdown-batch <outputRoot.dir> <input.xml> <name> [<input.xml> <name>]... [-- <beforeDays> [<afterDays> [<displayMode> [<progressMode> [<holidayDatesCsv>]]]]]");
        out.println("  export-daily-svg <input.xml> [<labelMode>]");
        out.println("  export-daily-svg-batch <outputRoot.dir> <input.xml> <name> [<input.xml> <name>]... [-- <labelMode>]");
        out.println("  export-weekly-svg <input.xml> [<labelMode>]");
        out.println("  export-weekly-svg-batch <outputRoot.dir> <input.xml> <name> [<input.xml> <name>]... [-- <labelMode>]");
        out.println("  export-monthly-svg-zip <input.xml> <output.zip> [<holidayDatesCsv> [<labelMode>]]");
        out.println("  export-monthly-svg-zip-batch <outputRoot.dir> <input.xml> <name> [<input.xml> <name>]... [-- <holidayDatesCsv> [<labelMode>]]");
        out.println("  export-report-bundle <input.xml> <output.zip> [<beforeDays> [<afterDays> [<displayMode> [<progressMode> [<holidayDatesCsv> [<labelMode>]]]]]]");
        out.println("  export-report-bundle-batch <outputRoot.dir> <input.xml> <name> [<input.xml> <name>]... [-- <beforeDays> [<afterDays> [<displayMode> [<progressMode> [<holidayDatesCsv> [<labelMode>]]]]]]");
        out.println("  export-report-dir <input.xml> <output.dir> [<beforeDays> [<afterDays> [<displayMode> [<progressMode> [<holidayDatesCsv> [<labelMode>]]]]]]");
        out.println("  export-report-dir-batch <outputRoot.dir> <input.xml> <name> [<input.xml> <name>]... [-- <beforeDays> [<afterDays> [<displayMode> [<progressMode> [<holidayDatesCsv> [<labelMode>]]]]]]");
        out.println("  export-wbs-xlsx <input.xml> <output.xlsxbin> [<beforeDays> [<afterDays> [<displayMode> [<progressMode> [<holidayDatesCsv>]]]]]");
        out.println("  export-wbs-xlsx-batch <outputRoot.dir> <input.xml> <name> [<input.xml> <name>]... [-- <beforeDays> [<afterDays> [<displayMode> [<progressMode> [<holidayDatesCsv>]]]]]");
        out.println("  export-workbook-json <input.xml>");
        out.println("  export-workbook-json-batch <outputRoot.dir> <input.xml> <name> [<input.xml> <name>]...");
        out.println("  export-project-overview-view <input.xml>");
        out.println("  export-project-overview-view-batch <outputRoot.dir> <input.xml> <name> [<input.xml> <name>]...");
        out.println("  export-phase-detail-view <input.xml> [<phaseUid> [<mode> [<rootUid> [<maxDepth>]]]]");
        out.println("  export-phase-detail-view-batch <outputRoot.dir> <input.xml> <name> [<input.xml> <name>]... [-- <phaseUid> [<mode> [<rootUid> [<maxDepth>]]]]");
        out.println("  export-task-edit-view <input.xml> <taskUid>");
        out.println("  export-task-edit-view-batch <outputRoot.dir> <taskUid> <input.xml> <name> [<input.xml> <name>]...");
        out.println("  export-project-draft-request <name> <plannedStart> [<goal> [<teamCount> [<mustHavePhasesCsv> [<mustHaveMilestonesCsv>]]]]");
        out.println("  validate-workbook-json <input.json>");
        out.println("  validate-workbook-json-batch <input.json>...");
        out.println("  import-workbook-json <input.json> <output.xml>");
        out.println("  import-workbook-json-batch <outputRoot.dir> <input.json> <name> [<input.json> <name>]...");
        out.println("  merge-workbook-json <base.xml> <input.json> <output.xml>");
        out.println("  merge-workbook-json-batch <base.xml> <outputRoot.dir> <input.json> <name> [<input.json> <name>]...");
        out.println("  validate-patch-json <input.json>");
        out.println("  validate-patch-json-batch <input.json>...");
        out.println("  apply-patch-json <base.xml> <patch.json> <output.xml>");
        out.println("  apply-patch-json-batch <base.xml> <outputRoot.dir> <patch.json> <name> [<patch.json> <name>]...");
        out.println("  export-ai-json-spec");
        out.println("  detect-ai-json-kind <input.txt>");
        out.println("  detect-ai-json-kind-batch <input.txt>...");
        out.println("  import-ai-json <input.txt> <output.xml> [<base.xml>]");
        out.println("  import-external <format> <mode> <input> <output.xml> [<base.xml>]");
        out.println("  export-xlsx <input.xml> <output.xlsxbin>");
        out.println("  export-xlsx-batch <outputRoot.dir> <input.xml> <name> [<input.xml> <name>]...");
        out.println("  validate-xlsx <input.xlsxbin>");
        out.println("  validate-xlsx-batch <input.xlsxbin>...");
        out.println("  import-xlsx <input.xlsxbin> <output.xml>");
        out.println("  import-xlsx-batch <outputRoot.dir> <input.xlsxbin> <name> [<input.xlsxbin> <name>]...");
        out.println("  merge-xlsx <base.xml> <input.xlsxbin> <output.xml>");
        out.println("  merge-xlsx-batch <base.xml> <outputRoot.dir> <input.xlsxbin> <name> [<input.xlsxbin> <name>]...");
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
