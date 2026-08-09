/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuproject.coreapi;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jp.igapyon.mikuproject.model.ProjectModel;

public class CoreApiAiJson {
    // The fixed vendored snapshot predates the public miku-project rename.
    // Keep its resource filename internal while returning the current public
    // specification text and identifier.
    private static final String VENDORED_AI_JSON_SPEC_RESOURCE = "mikuproject-ai-json-spec.md";
    private static final Pattern VERSION_PATTERN = Pattern.compile("^- Version:\\s*`([^`]+)`", Pattern.MULTILINE);

    private final CoreApiAiJsonUtil util = new CoreApiAiJsonUtil();
    private final CoreApiAiJsonImport imports = new CoreApiAiJsonImport();

    public String getAiJsonSpecText() {
        try (InputStream in = CoreApiAiJson.class.getResourceAsStream(VENDORED_AI_JSON_SPEC_RESOURCE)) {
            if (in == null) {
                throw new IllegalStateException("AI JSON spec text resource が見つかりません: " + VENDORED_AI_JSON_SPEC_RESOURCE);
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int length;
            while ((length = in.read(buffer)) != -1) {
                out.write(buffer, 0, length);
            }
            return canonicalizePublicProductName(new String(out.toByteArray(), StandardCharsets.UTF_8));
        } catch (IOException ex) {
            throw new IllegalStateException("AI JSON spec text resource を読み込めません: " + VENDORED_AI_JSON_SPEC_RESOURCE, ex);
        }
    }

    public CoreApiAiJsonSpec getAiJsonSpec() {
        CoreApiAiJsonSpec spec = new CoreApiAiJsonSpec();
        spec.id = "miku-project-ai-json-spec";
        spec.text = getAiJsonSpecText();
        spec.version = detectAiJsonSpecVersion(spec.text);
        return spec;
    }

    public String detectAiJsonDocumentKind(Object documentLike) {
        return imports.detectAiJsonDocumentKind(documentLike);
    }

    public CoreApiAiJsonParseResult parseAiJsonText(String sourceText) {
        CoreApiAiJsonParseResult result = new CoreApiAiJsonParseResult();
        result.sourceText = sourceText;
        result.jsonText = util.extractLastJsonBlock(sourceText);
        result.document = util.parseJsonText(result.jsonText);
        result.kind = detectAiJsonDocumentKind(result.document);
        return result;
    }

    public CoreApiImportResult importAiJsonDocument(Object documentLike) {
        return imports.importAiJsonDocument(documentLike);
    }

    public CoreApiImportResult importAiJsonDocument(Object documentLike, ProjectModel baseModel) {
        return imports.importAiJsonDocument(documentLike, baseModel);
    }

    public CoreApiAiJsonParseResult importAiJsonText(String sourceText) {
        return importAiJsonText(sourceText, null);
    }

    public CoreApiAiJsonParseResult importAiJsonText(String sourceText, ProjectModel baseModel) {
        CoreApiAiJsonParseResult parsed = parseAiJsonText(sourceText);
        parsed.result = importAiJsonDocument(parsed.document, baseModel);
        return parsed;
    }

    private String detectAiJsonSpecVersion(String text) {
        Matcher matcher = VERSION_PATTERN.matcher(text == null ? "" : text);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "unknown";
    }

    private String canonicalizePublicProductName(String text) {
        return text
                .replace("mikuproject AI JSON Prompt / Spec", "miku-project AI JSON Prompt / Spec")
                .replace("`mikuproject`", "`miku-project`")
                .replace("mikuproject ai ", "miku-project ai ");
    }
}
