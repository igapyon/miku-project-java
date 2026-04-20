/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuproject.projectpatchjson;

import jp.igapyon.mikuproject.model.ProjectModel;

public class ProjectPatchJson {
    private final ProjectPatchJsonCore core = new ProjectPatchJsonCore();

    public ProjectPatchJsonCore.ImportResult importProjectPatchJson(Object documentLike, ProjectModel baseModel) {
        return core.importProjectPatchJson(documentLike, baseModel);
    }

    public ProjectPatchJsonCore.ValidationResult validatePatchDocument(Object documentLike) {
        return core.validatePatchDocument(documentLike);
    }
}
