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
        DescriptorBook<FontDescriptor> fonts = dedupeFonts(styles);
        DescriptorBook<FillDescriptor> fills = dedupeFills(styles);
        DescriptorBook<BorderDescriptor> borders = dedupeBorders(styles);
        StringBuilder styleNodes = new StringBuilder();
        for (StyleDescriptor style : styles) {
            int numFmtId = mapNumberFormatId(style.numberFormat);
            int fontId = fonts.indexByKey.get(fontKey(style.bold, style.fontSize)).intValue();
            int fillId = fills.indexByKey.get(fillKey(style.fillColor == null ? "none" : "solid", style.fillColor)).intValue();
            int borderId = borders.indexByKey.get(borderKey(style.border)).intValue();
            StringBuilder alignment = new StringBuilder();
            if (style.horizontalAlign != null) {
                alignment.append(" horizontal=\"").append(escapeXml(style.horizontalAlign)).append("\"");
            }
            if (style.verticalAlign != null) {
                alignment.append(" vertical=\"").append(escapeXml(style.verticalAlign)).append("\"");
            }
            if (Boolean.TRUE.equals(style.wrapText)) {
                alignment.append(" wrapText=\"1\"");
            }
            styleNodes.append("<xf numFmtId=\"").append(numFmtId).append("\" fontId=\"").append(fontId).append("\" fillId=\"")
                    .append(fillId).append("\" borderId=\"").append(borderId).append("\" xfId=\"0\"");
            if (numFmtId != 0) {
                styleNodes.append(" applyNumberFormat=\"1\"");
            }
            if (alignment.length() > 0) {
                styleNodes.append(" applyAlignment=\"1\"");
            }
            if (fontId != 0) {
                styleNodes.append(" applyFont=\"1\"");
            }
            if (fillId != 0) {
                styleNodes.append(" applyFill=\"1\"");
            }
            if (borderId != 0) {
                styleNodes.append(" applyBorder=\"1\"");
            }
            if (alignment.length() > 0) {
                styleNodes.append("><alignment").append(alignment).append("/></xf>");
            } else {
                styleNodes.append("></xf>");
            }
        }
        StringBuilder builder = new StringBuilder();
        builder.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n");
        builder.append("<styleSheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">\n");
        builder.append("  <numFmts count=\"0\"/>\n");
        builder.append("  <fonts count=\"").append(fonts.items.size()).append("\">\n    ");
        for (FontDescriptor font : fonts.items) {
            builder.append(buildFontXml(font));
        }
        builder.append("\n  </fonts>\n");
        builder.append("  <fills count=\"").append(fills.items.size()).append("\">\n    ");
        for (FillDescriptor fill : fills.items) {
            builder.append(buildFillXml(fill));
        }
        builder.append("\n  </fills>\n");
        builder.append("  <borders count=\"").append(borders.items.size()).append("\">\n    ");
        for (BorderDescriptor border : borders.items) {
            builder.append(buildBorderXml(border));
        }
        builder.append("\n  </borders>\n");
        builder.append("  <cellStyleXfs count=\"1\">\n");
        builder.append("    <xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\"/>\n");
        builder.append("  </cellStyleXfs>\n");
        builder.append("  <cellXfs count=\"").append(styles.size()).append("\">\n    ").append(styleNodes).append("\n  </cellXfs>\n");
        builder.append("  <cellStyles count=\"1\">\n");
        builder.append("    <cellStyle name=\"Normal\" xfId=\"0\" builtinId=\"0\"/>\n");
        builder.append("  </cellStyles>\n");
        builder.append("</styleSheet>");
        return builder.toString();
    }

    private StyleDescriptor getStyleDescriptor(XlsxCellLike cell) {
        if (cell == null) {
            return null;
        }
        String numberFormat = cell.numberFormat == null && cell.value != null ? "text" : cell.numberFormat == null ? "general" : cell.numberFormat;
        if ("general".equals(numberFormat) && cell.horizontalAlign == null && cell.verticalAlign == null && cell.wrapText == null
                && cell.bold == null && cell.fontSize == null && cell.fillColor == null && cell.border == null) {
            return null;
        }
        StyleDescriptor descriptor = new StyleDescriptor();
        descriptor.numberFormat = numberFormat;
        descriptor.horizontalAlign = cell.horizontalAlign;
        descriptor.verticalAlign = cell.verticalAlign;
        descriptor.wrapText = Boolean.TRUE.equals(cell.wrapText) ? Boolean.TRUE : null;
        descriptor.bold = Boolean.TRUE.equals(cell.bold) ? Boolean.TRUE : null;
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

    private DescriptorBook<FontDescriptor> dedupeFonts(List<StyleDescriptor> styles) {
        DescriptorBook<FontDescriptor> book = new DescriptorBook<FontDescriptor>();
        addFont(book, null, null);
        for (StyleDescriptor style : styles) {
            addFont(book, style.bold, style.fontSize);
        }
        return book;
    }

    private void addFont(DescriptorBook<FontDescriptor> book, Boolean bold, Integer fontSize) {
        String key = fontKey(bold, fontSize);
        if (book.indexByKey.containsKey(key)) {
            return;
        }
        FontDescriptor font = new FontDescriptor();
        font.bold = bold;
        font.fontSize = fontSize;
        book.indexByKey.put(key, Integer.valueOf(book.items.size()));
        book.items.add(font);
    }

    private DescriptorBook<FillDescriptor> dedupeFills(List<StyleDescriptor> styles) {
        DescriptorBook<FillDescriptor> book = new DescriptorBook<FillDescriptor>();
        addFill(book, "none", null);
        addFill(book, "gray125", null);
        for (StyleDescriptor style : styles) {
            addFill(book, style.fillColor == null ? "none" : "solid", style.fillColor);
        }
        return book;
    }

    private void addFill(DescriptorBook<FillDescriptor> book, String patternType, String fillColor) {
        String key = fillKey(patternType, fillColor);
        if (book.indexByKey.containsKey(key)) {
            return;
        }
        FillDescriptor fill = new FillDescriptor();
        fill.patternType = patternType;
        fill.fillColor = fillColor;
        book.indexByKey.put(key, Integer.valueOf(book.items.size()));
        book.items.add(fill);
    }

    private DescriptorBook<BorderDescriptor> dedupeBorders(List<StyleDescriptor> styles) {
        DescriptorBook<BorderDescriptor> book = new DescriptorBook<BorderDescriptor>();
        addBorder(book, null);
        for (StyleDescriptor style : styles) {
            addBorder(book, style.border);
        }
        return book;
    }

    private void addBorder(DescriptorBook<BorderDescriptor> book, String borderStyle) {
        String key = borderKey(borderStyle);
        if (book.indexByKey.containsKey(key)) {
            return;
        }
        BorderDescriptor border = new BorderDescriptor();
        border.border = borderStyle;
        book.indexByKey.put(key, Integer.valueOf(book.items.size()));
        book.items.add(border);
    }

    private String fontKey(Boolean bold, Integer fontSize) {
        return (Boolean.TRUE.equals(bold) ? "bold" : "") + "::" + (fontSize == null ? "" : String.valueOf(fontSize));
    }

    private String fillKey(String patternType, String fillColor) {
        return safe(patternType == null ? "none" : patternType) + "::" + safe(fillColor);
    }

    private String borderKey(String border) {
        return safe(border);
    }

    private String buildFontXml(FontDescriptor font) {
        StringBuilder builder = new StringBuilder();
        if (Boolean.TRUE.equals(font.bold)) {
            builder.append("<b/>");
        }
        if (font.fontSize != null) {
            builder.append("<sz val=\"").append(font.fontSize).append("\"/>");
        }
        return builder.length() == 0 ? "<font/>" : "<font>" + builder.toString() + "</font>";
    }

    private String buildFillXml(FillDescriptor fill) {
        if ("gray125".equals(fill.patternType)) {
            return "<fill><patternFill patternType=\"gray125\"/></fill>";
        }
        if (fill.fillColor == null || !"solid".equals(fill.patternType)) {
            return "<fill><patternFill patternType=\"none\"/></fill>";
        }
        return "<fill><patternFill patternType=\"solid\"><fgColor rgb=\"" + escapeXml(fill.fillColor)
                + "\"/><bgColor indexed=\"64\"/></patternFill></fill>";
    }

    private String buildBorderXml(BorderDescriptor border) {
        if (border.border == null || border.border.isEmpty()) {
            return "<border/>";
        }
        String style = escapeXml(border.border);
        return "<border><left style=\"" + style + "\"/><right style=\"" + style + "\"/><top style=\"" + style
                + "\"/><bottom style=\"" + style + "\"/><diagonal/></border>";
    }

    private int mapNumberFormatId(String numberFormat) {
        if ("integer".equals(numberFormat)) {
            return 1;
        }
        if ("decimal".equals(numberFormat)) {
            return 2;
        }
        if ("text".equals(numberFormat)) {
            return 49;
        }
        if ("date".equals(numberFormat)) {
            return 14;
        }
        if ("datetime".equals(numberFormat)) {
            return 22;
        }
        if ("percent".equals(numberFormat)) {
            return 10;
        }
        return 0;
    }

    private String escapeXml(String value) {
        return safe(value).replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")
                .replace("'", "&apos;");
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

    private static class DescriptorBook<T> {
        public final List<T> items = new ArrayList<T>();
        public final Map<String, Integer> indexByKey = new LinkedHashMap<String, Integer>();
    }

    private static class FontDescriptor {
        public Boolean bold;
        public Integer fontSize;
    }

    private static class FillDescriptor {
        public String patternType;
        public String fillColor;
    }

    private static class BorderDescriptor {
        public String border;
    }
}
