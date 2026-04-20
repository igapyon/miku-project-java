/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuproject.wbsxlsx;

import java.util.List;

import jp.igapyon.mikuproject.model.ProjectModel;
import jp.igapyon.mikuproject.msprojectxml.MsProjectXml;
import jp.igapyon.mikuproject.projectxlsx.XlsxWorkbookLike;
import jp.igapyon.mikuproject.wbsdateband.WbsDateband;

public class WbsXlsxPublic {
    private final MsProjectXml msProjectXml = new MsProjectXml();
    private final WbsDateband dateband = new WbsDateband();
    private final WbsXlsxLayout layout = new WbsXlsxLayout();
    private final WbsXlsxExport export = new WbsXlsxExport(msProjectXml, dateband, layout);

    public List<String> collectWbsHolidayDates(ProjectModel model) {
        return dateband.collectWbsHolidayDates(msProjectXml.normalizeProjectModel(model));
    }

    public XlsxWorkbookLike exportWbsWorkbook(ProjectModel model) {
        return exportWbsWorkbook(model, null);
    }

    public XlsxWorkbookLike exportWbsWorkbook(ProjectModel model, WbsXlsxExport.WbsExportOptions options) {
        return export.exportWbsWorkbook(model, options);
    }

    public WbsXlsxLayout getLayout() {
        return layout;
    }
}
