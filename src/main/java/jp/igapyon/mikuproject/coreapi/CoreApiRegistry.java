/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuproject.coreapi;

import jp.igapyon.mikuproject.model.ProjectModel;

public class CoreApiRegistry {
    private final CoreApiImport coreApiImport = new CoreApiImport();
    private final CoreApiReportPublic coreApiReportPublic = new CoreApiReportPublic();

    public final CoreApiMsproject.SamplesApi samples = coreApiImport.msproject.samples;
    public final CoreApiMsproject.ProjectModelApi projectModel = coreApiImport.msproject.projectModel;
    public final CoreApiMsproject.MsProjectApi msProject = coreApiImport.msproject.msProject;
    public final CoreApiMsprojectAi.AiViewsApi aiViews = coreApiImport.msproject.aiViews;
    public final CoreApiWorkbook.WorkbookJsonApi workbookJson = coreApiImport.workbook.workbookJson;
    public final CoreApiWorkbookXlsx xlsx = coreApiImport.workbook.xlsx;
    public final CoreApiWorkbook.PatchJsonApi patchJson = coreApiImport.workbook.patchJson;
    public final CoreApiReportAdapters.ReportApi report = coreApiReportPublic.report;

    public String getAiJsonSpecText() {
        return coreApiImport.getAiJsonSpecText();
    }

    public CoreApiAiJsonSpec getAiJsonSpec() {
        return coreApiImport.getAiJsonSpec();
    }

    public String detectAiJsonDocumentKind(Object documentLike) {
        return coreApiImport.detectAiJsonDocumentKind(documentLike);
    }

    public CoreApiAiJsonParseResult parseAiJsonText(String sourceText) {
        return coreApiImport.parseAiJsonText(sourceText);
    }

    public CoreApiImportResult importAiJsonDocument(Object documentLike) {
        return coreApiImport.importAiJsonDocument(documentLike);
    }

    public CoreApiImportResult importAiJsonDocument(Object documentLike, ProjectModel baseModel) {
        return coreApiImport.importAiJsonDocument(documentLike, baseModel);
    }

    public CoreApiAiJsonParseResult importAiJsonText(String sourceText) {
        return coreApiImport.importAiJsonText(sourceText);
    }

    public CoreApiAiJsonParseResult importAiJsonText(String sourceText, ProjectModel baseModel) {
        return coreApiImport.importAiJsonText(sourceText, baseModel);
    }

    public CoreApiImportResult importExternal(CoreApiExternalImport.ExternalImportInput input) {
        return coreApiImport.importExternal(input);
    }

}
