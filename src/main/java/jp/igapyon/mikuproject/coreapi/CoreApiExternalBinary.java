/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuproject.coreapi;

import jp.igapyon.mikuproject.model.ProjectModel;
import jp.igapyon.mikuproject.projectxlsx.ProjectXlsxImport;
import jp.igapyon.mikuproject.projectxlsx.XlsxWorkbookLike;

public class CoreApiExternalBinary {
    private final CoreApiMsproject msproject = new CoreApiMsproject();
    private final CoreApiWorkbook workbook = new CoreApiWorkbook();

    public CoreApiImportResult importMsProjectXml(String sourceText, String mode) {
        if (!"replace".equals(mode)) {
            throw new IllegalArgumentException("MS Project XML は replace import のみ対応です");
        }
        CoreApiImportResult result = new CoreApiImportResult();
        result.kind = "ms_project_xml";
        result.mode = mode;
        result.model = msproject.msProject.importFromXml(sourceText);
        return result;
    }

    public CoreApiImportResult importXlsx(byte[] sourceBytes, String mode, ProjectModel baseModel) {
        if ("patch".equals(mode)) {
            throw new IllegalArgumentException("XLSX は replace または merge import のみ対応です");
        }
        XlsxWorkbookLike workbookLike = workbook.xlsx.decodeWorkbook(sourceBytes);
        if ("replace".equals(mode)) {
            CoreApiImportResult result = new CoreApiImportResult();
            result.kind = "xlsx";
            result.mode = mode;
            result.model = workbook.xlsx.importAsProjectModel(workbookLike);
            return result;
        }
        if (baseModel == null) {
            throw new IllegalArgumentException("XLSX merge import には baseModel が必要です");
        }
        ProjectXlsxImport.ImportResult imported = workbook.xlsx.importIntoProjectModelDetailed(workbookLike, baseModel);
        CoreApiImportResult result = new CoreApiImportResult();
        result.kind = "xlsx";
        result.mode = mode;
        result.model = imported.model;
        result.changes.addAll(imported.changes);
        return result;
    }
}
