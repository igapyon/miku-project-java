/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuproject.projectworkbookjson;

import jp.igapyon.mikuproject.model.ProjectModel;

public class ProjectWorkbookJson {
    private final ProjectWorkbookJsonExport export = new ProjectWorkbookJsonExport();
    private final ProjectWorkbookJsonImport imports = new ProjectWorkbookJsonImport();

    public WorkbookJsonDocument exportProjectWorkbookJson(ProjectModel model) {
        return export.exportProjectWorkbookJson(model);
    }

    public ProjectWorkbookJsonImport.ImportAsProjectModelResult importProjectWorkbookJsonAsProjectModel(Object documentLike) {
        return imports.importProjectWorkbookJsonAsProjectModel(documentLike);
    }

    public ProjectWorkbookJsonImport.ImportResult importProjectWorkbookJson(Object documentLike, ProjectModel baseModel) {
        return imports.importProjectWorkbookJson(documentLike, baseModel);
    }

    public ProjectWorkbookJsonValidate.ValidationResult validateWorkbookJsonDocument(Object documentLike) {
        return imports.validateWorkbookJsonDocument(documentLike);
    }
}
