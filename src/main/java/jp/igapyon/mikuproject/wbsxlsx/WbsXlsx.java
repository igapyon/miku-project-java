/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuproject.wbsxlsx;

import java.util.List;

import jp.igapyon.mikuproject.model.ProjectModel;
import jp.igapyon.mikuproject.projectxlsx.XlsxWorkbookLike;

public class WbsXlsx {
    private final WbsXlsxPublic publicApi = new WbsXlsxPublic();

    public List<String> collectWbsHolidayDates(ProjectModel model) {
        return publicApi.collectWbsHolidayDates(model);
    }

    public XlsxWorkbookLike exportWbsWorkbook(ProjectModel model) {
        return publicApi.exportWbsWorkbook(model);
    }

    public XlsxWorkbookLike exportWbsWorkbook(ProjectModel model, WbsExportOptions options) {
        return publicApi.exportWbsWorkbook(model, options);
    }

    public WbsXlsxLayout getLayout() {
        return publicApi.getLayout();
    }

    public static class WbsExportOptions extends WbsXlsxExport.WbsExportOptions {
    }
}
