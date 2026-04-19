/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuproject.projectxlsx;

import jp.igapyon.mikuproject.model.ProjectModel;

public class ProjectXlsx {
    private final ProjectXlsxExport export = new ProjectXlsxExport();
    private final ProjectXlsxImport imports = new ProjectXlsxImport();

    public XlsxWorkbookLike exportProjectWorkbook(ProjectModel model) {
        return export.exportProjectWorkbook(model);
    }

    public ProjectModel importProjectWorkbook(XlsxWorkbookLike workbook, ProjectModel baseModel) {
        return imports.importProjectWorkbook(workbook, baseModel);
    }

    public ProjectModel importProjectWorkbookAsProjectModel(XlsxWorkbookLike workbook) {
        return imports.importProjectWorkbookAsProjectModel(workbook);
    }

    public ProjectXlsxImport.ImportResult importProjectWorkbookDetailed(XlsxWorkbookLike workbook, ProjectModel baseModel) {
        return imports.importProjectWorkbookDetailed(workbook, baseModel);
    }
}
