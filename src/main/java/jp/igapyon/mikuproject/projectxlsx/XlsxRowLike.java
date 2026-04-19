/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuproject.projectxlsx;

import java.util.ArrayList;
import java.util.List;

public class XlsxRowLike {
    public Integer height;
    public List<XlsxCellLike> cells = new ArrayList<XlsxCellLike>();
}
