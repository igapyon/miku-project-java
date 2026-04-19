/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuproject.coreapi;

import jp.igapyon.mikuproject.excelio.XlsxWorkbookCodec;
import jp.igapyon.mikuproject.model.ProjectModel;
import jp.igapyon.mikuproject.projectxlsx.ProjectXlsx;
import jp.igapyon.mikuproject.projectxlsx.ProjectXlsxImport;
import jp.igapyon.mikuproject.projectxlsx.XlsxWorkbookLike;

public class CoreApiWorkbookXlsx {
    private final ProjectXlsx projectXlsx = new ProjectXlsx();
    private final XlsxWorkbookCodec codec = new XlsxWorkbookCodec();

    public XlsxWorkbookLike exportWorkbook(ProjectModel model) {
        return projectXlsx.exportProjectWorkbook(model);
    }

    public ProjectModel importAsProjectModel(XlsxWorkbookLike workbook) {
        return projectXlsx.importProjectWorkbookAsProjectModel(workbook);
    }

    public ProjectModel importIntoProjectModel(XlsxWorkbookLike workbook, ProjectModel baseModel) {
        return projectXlsx.importProjectWorkbook(workbook, baseModel);
    }

    public ProjectXlsxImport.ImportResult importIntoProjectModelDetailed(XlsxWorkbookLike workbook, ProjectModel baseModel) {
        return projectXlsx.importProjectWorkbookDetailed(workbook, baseModel);
    }

    public byte[] encodeWorkbook(XlsxWorkbookLike workbook) {
        return codec.exportWorkbook(workbook);
    }

    public XlsxWorkbookLike decodeWorkbook(byte[] bytes) {
        return codec.importWorkbook(bytes);
    }
}
