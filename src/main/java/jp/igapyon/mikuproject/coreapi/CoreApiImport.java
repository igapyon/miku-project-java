/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuproject.coreapi;

import jp.igapyon.mikuproject.model.ProjectModel;

public class CoreApiImport {
    public final CoreApiMsproject msproject = new CoreApiMsproject();
    public final CoreApiWorkbook workbook = new CoreApiWorkbook();
    public final CoreApiAiJson aiJson = new CoreApiAiJson();
    public final CoreApiExternalImport externalImport = new CoreApiExternalImport();

    public String getAiJsonSpecText() {
        return aiJson.getAiJsonSpecText();
    }

    public CoreApiAiJsonSpec getAiJsonSpec() {
        return aiJson.getAiJsonSpec();
    }

    public String detectAiJsonDocumentKind(Object documentLike) {
        return aiJson.detectAiJsonDocumentKind(documentLike);
    }

    public CoreApiAiJsonParseResult parseAiJsonText(String sourceText) {
        return aiJson.parseAiJsonText(sourceText);
    }

    public CoreApiImportResult importAiJsonDocument(Object documentLike) {
        return aiJson.importAiJsonDocument(documentLike);
    }

    public CoreApiImportResult importAiJsonDocument(Object documentLike, ProjectModel baseModel) {
        return aiJson.importAiJsonDocument(documentLike, baseModel);
    }

    public CoreApiAiJsonParseResult importAiJsonText(String sourceText) {
        return aiJson.importAiJsonText(sourceText);
    }

    public CoreApiAiJsonParseResult importAiJsonText(String sourceText, ProjectModel baseModel) {
        return aiJson.importAiJsonText(sourceText, baseModel);
    }

    public CoreApiImportResult importExternal(CoreApiExternalImport.ExternalImportInput input) {
        return externalImport.importExternal(input);
    }
}
