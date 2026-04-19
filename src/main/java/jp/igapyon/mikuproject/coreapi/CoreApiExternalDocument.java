/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuproject.coreapi;

import jp.igapyon.mikuproject.model.ProjectModel;

public class CoreApiExternalDocument {
    private final CoreApiAiJson aiJson = new CoreApiAiJson();

    public CoreApiImportResult importDocument(String format, Object document, String mode, ProjectModel baseModel) {
        if ("workbook_json".equals(format)) {
            if ("patch".equals(mode)) {
                throw new IllegalArgumentException("workbook JSON は patch import に対応していません");
            }
            if ("merge".equals(mode) && baseModel == null) {
                throw new IllegalArgumentException("workbook JSON merge import には baseModel が必要です");
            }
            return "merge".equals(mode) ? aiJson.importAiJsonDocument(document, baseModel) : aiJson.importAiJsonDocument(document);
        }

        if ("project_draft_view".equals(format)) {
            if (!"replace".equals(mode)) {
                throw new IllegalArgumentException("project_draft_view は replace import のみ対応です");
            }
            return aiJson.importAiJsonDocument(document);
        }

        if ("patch_json".equals(format)) {
            if (!"patch".equals(mode)) {
                throw new IllegalArgumentException("patch JSON は patch import のみ対応です");
            }
            return aiJson.importAiJsonDocument(document, baseModel);
        }

        throw new IllegalArgumentException("未対応の import format です: " + format);
    }
}
