/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuproject.coreapi;

import jp.igapyon.mikuproject.projectpatchjson.ProjectPatchJson;
import jp.igapyon.mikuproject.projectworkbookjson.ProjectWorkbookJson;

public class CoreApiWorkbook {
    public final WorkbookJsonApi workbookJson = new WorkbookJsonApi();
    public final CoreApiWorkbookXlsx xlsx = new CoreApiWorkbookXlsx();
    public final PatchJsonApi patchJson = new PatchJsonApi();

    public static class WorkbookJsonApi extends ProjectWorkbookJson {
    }

    public static class PatchJsonApi extends ProjectPatchJson {
    }
}
