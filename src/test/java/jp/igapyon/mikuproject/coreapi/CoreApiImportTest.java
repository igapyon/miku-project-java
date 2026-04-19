/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuproject.coreapi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import jp.igapyon.mikuproject.model.ProjectModel;
import jp.igapyon.mikuproject.projectworkbookjson.WorkbookJsonDocument;
import jp.igapyon.mikuproject.projectxlsx.XlsxWorkbookLike;

public class CoreApiImportTest {
    @Test
    public void parsesFencedAiJsonTextAndDetectsKind() {
        CoreApiImport api = new CoreApiImport();

        CoreApiAiJsonParseResult parsed = api.parseAiJsonText(joinLines(
                "説明文",
                "```json",
                "{\"view_type\":\"project_draft_view\",\"project\":{\"name\":\"Test\"},\"tasks\":[]}",
                "```"));

        assertEquals("project_draft_view", parsed.kind);
        assertEquals("project_draft_view", map(parsed.document).get("view_type"));
    }

    @Test
    public void importsProjectDraftViewWithoutUiDependencies() {
        CoreApiImport api = new CoreApiImport();

        CoreApiImportResult result = api.importAiJsonDocument(mapOf(
                "view_type", "project_draft_view",
                "project", mapOf("name", "API draft import", "planned_start", "2026-04-01"),
                "tasks", Arrays.asList(mapOf(
                        "uid", "draft-1",
                        "name", "開始",
                        "parent_uid", null,
                        "position", Integer.valueOf(0),
                        "is_milestone", Boolean.TRUE,
                        "planned_start", "2026-04-01",
                        "planned_finish", "2026-04-01")),
                "resources", Arrays.asList(),
                "assignments", Arrays.asList()));

        assertEquals("project_draft_view", result.kind);
        assertEquals("replace", result.mode);
        assertEquals("API draft import", result.model.project.name);
        assertEquals(1, result.model.tasks.size());
    }

    @Test
    public void importsWorkbookJsonWithAndWithoutBaseModel() throws IOException {
        CoreApiImport api = new CoreApiImport();
        ProjectModel baseModel = api.msproject.msProject.importFromXml(readVendorTestdata("dependency.xml"));
        WorkbookJsonDocument document = api.workbook.workbookJson.exportProjectWorkbookJson(baseModel);
        findProjectNameRow(document).put("Value", "Core API workbook import");

        CoreApiImportResult replaceResult = api.importAiJsonDocument(toDocumentLike(document));
        CoreApiImportResult mergeResult = api.importAiJsonDocument(toDocumentLike(document), baseModel);

        assertEquals("workbook_json", replaceResult.kind);
        assertEquals("replace", replaceResult.mode);
        assertEquals("Core API workbook import", replaceResult.model.project.name);
        assertEquals("workbook_json", mergeResult.kind);
        assertEquals("merge", mergeResult.mode);
        assertEquals("Core API workbook import", mergeResult.model.project.name);
        assertTrue(mergeResult.changes.size() > 0);
    }

    @Test
    public void importsAiJsonTextAndExposesAiJsonSpec() throws IOException {
        CoreApiImport api = new CoreApiImport();
        ProjectModel baseModel = api.msproject.msProject.importFromXml(readVendorTestdata("dependency.xml"));
        WorkbookJsonDocument document = api.workbook.workbookJson.exportProjectWorkbookJson(baseModel);
        findProjectNameRow(document).put("Value", "Core API text import");

        CoreApiAiJsonParseResult parsedImport = api.importAiJsonText(joinLines(
                "説明",
                "```json",
                toJsonText(toDocumentLike(document)),
                "```"), baseModel);
        CoreApiAiJsonSpec spec = api.getAiJsonSpec();

        assertEquals("workbook_json", parsedImport.kind);
        assertEquals("workbook_json", parsedImport.result.kind);
        assertEquals("merge", parsedImport.result.mode);
        assertEquals("Core API text import", parsedImport.result.model.project.name);
        assertEquals("mikuproject-ai-json-spec", spec.id);
        assertTrue(spec.version != null && !spec.version.isEmpty());
        assertTrue(spec.text.contains("project_draft_view"));
        assertTrue(api.getAiJsonSpecText().contains("Patch JSON"));
    }

    @Test
    public void importsExternalFormatsThroughImportExternal() throws IOException {
        CoreApiImport api = new CoreApiImport();
        ProjectModel baseModel = api.msproject.msProject.importFromXml(readVendorTestdata("dependency.xml"));
        XlsxWorkbookLike workbook = api.workbook.xlsx.exportWorkbook(baseModel);
        workbook.sheets.get(0).rows.get(3).cells.get(1).value = "Core API external xlsx";
        byte[] xlsxBytes = api.workbook.xlsx.encodeWorkbook(workbook);
        WorkbookJsonDocument workbookJson = api.workbook.workbookJson.exportProjectWorkbookJson(baseModel);
        findProjectNameRow(workbookJson).put("Value", "Core API external workbook json");

        CoreApiImportResult xmlResult = api.importExternal(externalInput(
                externalTextSource("ms_project_xml", readVendorTestdata("dependency.xml")), "replace", null));
        CoreApiImportResult xlsxReplaceResult = api.importExternal(externalInput(
                externalBytesSource("xlsx", xlsxBytes), "replace", null));
        CoreApiImportResult xlsxMergeResult = api.importExternal(externalInput(
                externalBytesSource("xlsx", xlsxBytes), "merge", baseModel));
        CoreApiImportResult workbookJsonMergeResult = api.importExternal(externalInput(
                externalDocumentSource("workbook_json", toDocumentLike(workbookJson)), "merge", baseModel));
        CoreApiImportResult patchResult = api.importExternal(externalInput(
                externalDocumentSource("patch_json", mapOf("operations", Arrays.asList(
                        mapOf("op", "update_project", "fields", mapOf("name", "Core API external patch"))))),
                "patch", baseModel));

        assertEquals("ms_project_xml", xmlResult.kind);
        assertEquals("replace", xmlResult.mode);
        assertEquals("xlsx", xlsxReplaceResult.kind);
        assertEquals("Core API external xlsx", xlsxReplaceResult.model.project.name);
        assertEquals("xlsx", xlsxMergeResult.kind);
        assertEquals("merge", xlsxMergeResult.mode);
        assertTrue(xlsxMergeResult.changes.size() > 0);
        assertEquals("workbook_json", workbookJsonMergeResult.kind);
        assertEquals("merge", workbookJsonMergeResult.mode);
        assertEquals("Core API external workbook json", workbookJsonMergeResult.model.project.name);
        assertEquals("patch_json", patchResult.kind);
        assertEquals("Core API external patch", patchResult.model.project.name);
    }

    @Test
    public void appliesPatchJsonThroughUnifiedEntryPoint() throws IOException {
        CoreApiImport api = new CoreApiImport();
        ProjectModel baseModel = api.msproject.msProject.importFromXml(readVendorTestdata("dependency.xml"));

        CoreApiImportResult result = api.importAiJsonDocument(mapOf("operations", Arrays.asList(
                mapOf("op", "update_project", "fields", mapOf("name", "Core API patch import")))), baseModel);

        assertEquals("patch_json", result.kind);
        assertEquals("patch", result.mode);
        assertEquals("Core API patch import", result.model.project.name);
        assertTrue(result.changes.size() > 0);
    }

    @Test
    public void rejectsPatchJsonWhenBaseModelIsMissing() {
        CoreApiImport api = new CoreApiImport();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                new org.junit.jupiter.api.function.Executable() {
                    @Override
                    public void execute() {
                        api.importAiJsonDocument(mapOf("operations", Arrays.asList()));
                    }
                });

        assertTrue(exception.getMessage().contains("baseModel"));
    }

    @Test
    public void rejectsUnsupportedFormatAndModeCombinationsInImportExternal() throws IOException {
        CoreApiImport api = new CoreApiImport();
        ProjectModel baseModel = api.msproject.msProject.importFromXml(readVendorTestdata("dependency.xml"));
        XlsxWorkbookLike workbook = api.workbook.xlsx.exportWorkbook(baseModel);
        byte[] xlsxBytes = api.workbook.xlsx.encodeWorkbook(workbook);
        WorkbookJsonDocument workbookJson = api.workbook.workbookJson.exportProjectWorkbookJson(baseModel);

        assertTrue(assertThrows(IllegalArgumentException.class,
                new org.junit.jupiter.api.function.Executable() {
                    @Override
                    public void execute() {
                        api.importExternal(externalInput(externalTextSource("ms_project_xml", readVendorTestdataUnchecked()),
                                "merge", baseModel));
                    }
                }).getMessage().contains("format=ms_project_xml"));
        assertTrue(assertThrows(IllegalArgumentException.class,
                new org.junit.jupiter.api.function.Executable() {
                    @Override
                    public void execute() {
                        api.importExternal(externalInput(externalDocumentSource("patch_json",
                                mapOf("operations", Arrays.asList())), "replace", baseModel));
                    }
                }).getMessage().contains("mode=patch"));
        assertTrue(assertThrows(IllegalArgumentException.class,
                new org.junit.jupiter.api.function.Executable() {
                    @Override
                    public void execute() {
                        api.importExternal(externalInput(externalBytesSource("xlsx", xlsxBytes), "patch", baseModel));
                    }
                }).getMessage().contains("mode=replace / mode=merge"));
        assertTrue(assertThrows(IllegalArgumentException.class,
                new org.junit.jupiter.api.function.Executable() {
                    @Override
                    public void execute() {
                        api.importExternal(externalInput(externalDocumentSource("workbook_json", toDocumentLike(workbookJson)),
                                "patch", baseModel));
                    }
                }).getMessage().contains("format=workbook_json"));
    }

    @Test
    public void rejectsMergeImportsWhenBaseModelIsMissing() throws IOException {
        CoreApiImport api = new CoreApiImport();
        ProjectModel baseModel = api.msproject.msProject.importFromXml(readVendorTestdata("dependency.xml"));
        XlsxWorkbookLike workbook = api.workbook.xlsx.exportWorkbook(baseModel);
        byte[] xlsxBytes = api.workbook.xlsx.encodeWorkbook(workbook);
        WorkbookJsonDocument workbookJson = api.workbook.workbookJson.exportProjectWorkbookJson(baseModel);

        assertTrue(assertThrows(IllegalArgumentException.class,
                new org.junit.jupiter.api.function.Executable() {
                    @Override
                    public void execute() {
                        api.importExternal(externalInput(externalBytesSource("xlsx", xlsxBytes), "merge", null));
                    }
                }).getMessage().contains("format=xlsx mode=merge"));
        assertTrue(assertThrows(IllegalArgumentException.class,
                new org.junit.jupiter.api.function.Executable() {
                    @Override
                    public void execute() {
                        api.importExternal(externalInput(externalDocumentSource("workbook_json", toDocumentLike(workbookJson)),
                                "merge", null));
                    }
                }).getMessage().contains("format=workbook_json mode=merge"));
        assertTrue(assertThrows(IllegalArgumentException.class,
                new org.junit.jupiter.api.function.Executable() {
                    @Override
                    public void execute() {
                        api.importExternal(externalInput(externalDocumentSource("patch_json",
                                mapOf("operations", Arrays.asList())), "patch", null));
                    }
                }).getMessage().contains("format=patch_json mode=patch"));
    }

    private CoreApiExternalImport.ExternalImportInput externalInput(CoreApiExternalImport.ExternalImportSource source, String mode,
            ProjectModel baseModel) {
        CoreApiExternalImport.ExternalImportInput input = new CoreApiExternalImport.ExternalImportInput();
        input.source = source;
        input.mode = mode;
        input.baseModel = baseModel;
        return input;
    }

    private CoreApiExternalImport.ExternalImportSource externalTextSource(String format, String text) {
        CoreApiExternalImport.ExternalImportSource source = new CoreApiExternalImport.ExternalImportSource();
        source.format = format;
        source.text = text;
        return source;
    }

    private CoreApiExternalImport.ExternalImportSource externalBytesSource(String format, byte[] bytes) {
        CoreApiExternalImport.ExternalImportSource source = new CoreApiExternalImport.ExternalImportSource();
        source.format = format;
        source.bytes = bytes;
        return source;
    }

    private CoreApiExternalImport.ExternalImportSource externalDocumentSource(String format, Object document) {
        CoreApiExternalImport.ExternalImportSource source = new CoreApiExternalImport.ExternalImportSource();
        source.format = format;
        source.document = document;
        return source;
    }

    private Map<String, Object> findProjectNameRow(WorkbookJsonDocument document) {
        for (Map<String, Object> row : document.sheets.get("Project")) {
            if ("Name".equals(row.get("Field"))) {
                return row;
            }
        }
        return null;
    }

    private Map<String, Object> toDocumentLike(WorkbookJsonDocument document) {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        map.put("format", document.format);
        map.put("version", document.version);
        map.put("sheets", document.sheets);
        return map;
    }

    private String readVendorTestdata(String fileName) throws IOException {
        byte[] bytes = Files.readAllBytes(Paths.get("vendor", "mikuproject", "testdata", fileName));
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private String readVendorTestdataUnchecked() {
        try {
            return readVendorTestdata("dependency.xml");
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private String joinLines(String... lines) {
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < lines.length; index++) {
            if (index > 0) {
                builder.append('\n');
            }
            builder.append(lines[index]);
        }
        return builder.toString();
    }

    private String toJsonText(Map<String, Object> map) {
        StringBuilder builder = new StringBuilder();
        appendJsonValue(builder, map);
        return builder.toString();
    }

    @SuppressWarnings("unchecked")
    private void appendJsonValue(StringBuilder builder, Object value) {
        if (value == null) {
            builder.append("null");
            return;
        }
        if (value instanceof String) {
            builder.append('"').append(escapeJson((String) value)).append('"');
            return;
        }
        if (value instanceof Number || value instanceof Boolean) {
            builder.append(String.valueOf(value));
            return;
        }
        if (value instanceof Map<?, ?>) {
            builder.append('{');
            boolean first = true;
            for (Map.Entry<String, Object> entry : ((Map<String, Object>) value).entrySet()) {
                if (!first) {
                    builder.append(',');
                }
                first = false;
                builder.append('"').append(escapeJson(entry.getKey())).append('"').append(':');
                appendJsonValue(builder, entry.getValue());
            }
            builder.append('}');
            return;
        }
        if (value instanceof Iterable<?>) {
            builder.append('[');
            boolean first = true;
            for (Object item : (Iterable<?>) value) {
                if (!first) {
                    builder.append(',');
                }
                first = false;
                appendJsonValue(builder, item);
            }
            builder.append(']');
            return;
        }
        builder.append('"').append(escapeJson(String.valueOf(value))).append('"');
    }

    private String escapeJson(String text) {
        return text
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> map(Object value) {
        assertNotNull(value);
        return (Map<String, Object>) value;
    }

    private Map<String, Object> mapOf(Object... entries) {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        for (int i = 0; i < entries.length; i += 2) {
            map.put(String.valueOf(entries[i]), entries[i + 1]);
        }
        return map;
    }
}
