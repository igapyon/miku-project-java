/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuproject.coreapi;

import jp.igapyon.mikuproject.model.ProjectModel;
import jp.igapyon.mikuproject.projectpatchjson.ProjectPatchJsonCore;
import jp.igapyon.mikuproject.projectworkbookjson.ProjectWorkbookJsonImport;

public class CoreApiAiJsonImport {
    private final CoreApiMsproject msproject = new CoreApiMsproject();
    private final CoreApiWorkbook workbook = new CoreApiWorkbook();
    private final CoreApiAiJsonUtil util = new CoreApiAiJsonUtil();

    public String detectAiJsonDocumentKind(Object documentLike) {
        return util.detectJsonDocumentKind(documentLike);
    }

    public CoreApiImportResult importAiJsonDocument(Object documentLike) {
        return importAiJsonDocument(documentLike, null);
    }

    public CoreApiImportResult importAiJsonDocument(Object documentLike, ProjectModel baseModel) {
        String kind = detectAiJsonDocumentKind(documentLike);
        if ("project_draft_view".equals(kind)) {
            CoreApiImportResult result = new CoreApiImportResult();
            result.kind = kind;
            result.mode = "replace";
            result.model = msproject.aiViews.importProjectDraftView(documentLike);
            return result;
        }

        if ("workbook_json".equals(kind)) {
            if (baseModel != null) {
                ProjectWorkbookJsonImport.ImportResult imported = workbook.workbookJson.importProjectWorkbookJson(documentLike,
                        baseModel);
                CoreApiImportResult result = new CoreApiImportResult();
                result.kind = kind;
                result.mode = "merge";
                result.model = imported.model;
                result.changes.addAll(imported.changes);
                result.warnings.addAll(imported.warnings);
                return result;
            }
            ProjectWorkbookJsonImport.ImportAsProjectModelResult imported = workbook.workbookJson
                    .importProjectWorkbookJsonAsProjectModel(documentLike);
            CoreApiImportResult result = new CoreApiImportResult();
            result.kind = kind;
            result.mode = "replace";
            result.model = imported.model;
            result.warnings.addAll(imported.warnings);
            return result;
        }

        if ("patch_json".equals(kind)) {
            if (baseModel == null) {
                throw new IllegalArgumentException("Patch JSON の適用には baseModel が必要です");
            }
            ProjectPatchJsonCore.ImportResult imported = workbook.patchJson.importProjectPatchJson(documentLike, baseModel);
            CoreApiImportResult result = new CoreApiImportResult();
            result.kind = kind;
            result.mode = "patch";
            result.model = imported.model;
            result.changes.addAll(imported.changes);
            result.warnings.addAll(imported.warnings);
            return result;
        }

        throw new IllegalArgumentException("AI JSON の format / view_type を判別できません");
    }
}
