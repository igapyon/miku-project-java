/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuproject.projectxlsx;

import java.util.ArrayList;
import java.util.List;

public class XlsxSheetLike {
    public String name;
    public List<XlsxColumnLike> columns = new ArrayList<XlsxColumnLike>();
    public XlsxFreezePaneLike freezePane;
    public List<String> mergedRanges = new ArrayList<String>();
    public List<XlsxDataValidationLike> dataValidations = new ArrayList<XlsxDataValidationLike>();
    public List<XlsxRowLike> rows = new ArrayList<XlsxRowLike>();
}
