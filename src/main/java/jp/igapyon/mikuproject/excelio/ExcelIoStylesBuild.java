/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuproject.excelio;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jp.igapyon.mikuproject.projectxlsx.XlsxCellLike;
import jp.igapyon.mikuproject.projectxlsx.XlsxRowLike;
import jp.igapyon.mikuproject.projectxlsx.XlsxSheetLike;
import jp.igapyon.mikuproject.projectxlsx.XlsxWorkbookLike;

public class ExcelIoStylesBuild {
    public StyleBook createStyleBook(XlsxWorkbookLike workbook) {
        StyleBook book = new StyleBook();
        book.styles.add(new StyleDescriptor());
        book.styleIndexByKey.put(styleKey(book.styles.get(0)), Integer.valueOf(0));

        for (XlsxSheetLike sheet : workbook.sheets) {
            for (XlsxRowLike row : sheet.rows) {
                for (XlsxCellLike cell : row.cells) {
                    StyleDescriptor descriptor = getStyleDescriptor(cell);
                    if (descriptor == null) {
                        continue;
                    }
                    String key = styleKey(descriptor);
                    if (!book.styleIndexByKey.containsKey(key)) {
                        book.styleIndexByKey.put(key, Integer.valueOf(book.styles.size()));
                        book.styles.add(descriptor);
                    }
                }
            }
        }

        return book;
    }

    public int resolveStyleIndex(XlsxCellLike cell, StyleBook styleBook) {
        StyleDescriptor descriptor = getStyleDescriptor(cell);
        if (descriptor == null) {
            return 0;
        }
        Integer styleIndex = styleBook.styleIndexByKey.get(styleKey(descriptor));
        return styleIndex == null ? 0 : styleIndex.intValue();
    }

    public String buildStylesXml(List<StyleDescriptor> styles) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<styleSheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">"
                + "<fonts count=\"2\"><font><sz val=\"11\"/><name val=\"Calibri\"/></font><font><b/><sz val=\"14\"/><name val=\"Calibri\"/></font></fonts>"
                + "<fills count=\"2\"><fill><patternFill patternType=\"none\"/></fill><fill><patternFill patternType=\"solid\"><fgColor rgb=\"FFD9EAF7\"/></patternFill></fill></fills>"
                + "<borders count=\"2\"><border/><border><left style=\"thin\"/><right style=\"thin\"/><top style=\"thin\"/><bottom style=\"thin\"/></border></borders>"
                + "<cellStyleXfs count=\"1\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\"/></cellStyleXfs>"
                + "<cellXfs count=\"" + styles.size() + "\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\"/>"
                + (styles.size() > 1
                        ? "<xf numFmtId=\"0\" fontId=\"1\" fillId=\"1\" borderId=\"1\" xfId=\"0\" applyFont=\"1\" applyFill=\"1\" applyBorder=\"1\"/>"
                        : "")
                + "</cellXfs></styleSheet>";
    }

    private StyleDescriptor getStyleDescriptor(XlsxCellLike cell) {
        if (cell == null) {
            return null;
        }
        if (cell.numberFormat == null && cell.horizontalAlign == null && cell.verticalAlign == null && cell.wrapText == null
                && cell.bold == null && cell.fontSize == null && cell.fillColor == null && cell.border == null) {
            return null;
        }
        StyleDescriptor descriptor = new StyleDescriptor();
        descriptor.numberFormat = cell.numberFormat == null ? "general" : cell.numberFormat;
        descriptor.horizontalAlign = cell.horizontalAlign;
        descriptor.verticalAlign = cell.verticalAlign;
        descriptor.wrapText = cell.wrapText;
        descriptor.bold = cell.bold;
        descriptor.fontSize = cell.fontSize;
        descriptor.fillColor = cell.fillColor;
        descriptor.border = cell.border;
        return descriptor;
    }

    private String styleKey(StyleDescriptor descriptor) {
        return safe(descriptor.numberFormat) + "::" + safe(descriptor.horizontalAlign) + "::" + safe(descriptor.verticalAlign) + "::"
                + safe(descriptor.wrapText == null ? null : String.valueOf(descriptor.wrapText)) + "::"
                + safe(descriptor.bold == null ? null : String.valueOf(descriptor.bold)) + "::"
                + safe(descriptor.fontSize == null ? null : String.valueOf(descriptor.fontSize)) + "::" + safe(descriptor.fillColor) + "::"
                + safe(descriptor.border);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    public static class StyleBook {
        public final List<StyleDescriptor> styles = new ArrayList<StyleDescriptor>();
        public final Map<String, Integer> styleIndexByKey = new LinkedHashMap<String, Integer>();
    }

    public static class StyleDescriptor {
        public String numberFormat = "general";
        public String horizontalAlign;
        public String verticalAlign;
        public Boolean wrapText;
        public Boolean bold;
        public Integer fontSize;
        public String fillColor;
        public String border;
    }
}
