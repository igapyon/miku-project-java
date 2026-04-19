/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuproject.excelio;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

import jp.igapyon.mikuproject.projectxlsx.XlsxCellLike;
import jp.igapyon.mikuproject.projectxlsx.XlsxColumnLike;
import jp.igapyon.mikuproject.projectxlsx.XlsxDataValidationLike;
import jp.igapyon.mikuproject.projectxlsx.XlsxRowLike;
import jp.igapyon.mikuproject.projectxlsx.XlsxSheetLike;
import jp.igapyon.mikuproject.projectxlsx.XlsxWorkbookLike;

public class ExcelIoNormalize {
    private static final Pattern INVALID_SHEET_NAME_PATTERN = Pattern.compile("[:\\\\/?*\\[\\]]");
    private static final Pattern MERGED_RANGE_PATTERN = Pattern.compile("^[A-Z]+\\d+:[A-Z]+\\d+$");
    private static final Pattern SQREF_CELL_PATTERN = Pattern.compile("^[A-Z]+\\d+$");
    private static final Pattern SQREF_RANGE_PATTERN = Pattern.compile("^[A-Z]+\\d+:[A-Z]+\\d+$");
    private static final Pattern COLOR_PATTERN = Pattern.compile("^#?[0-9A-Fa-f]{6}$");

    public XlsxWorkbookLike normalizeWorkbook(XlsxWorkbookLike workbook) {
        if (workbook == null || workbook.sheets == null || workbook.sheets.isEmpty()) {
            throw new IllegalArgumentException("Workbook must contain at least one sheet");
        }
        Set<String> seenNames = new LinkedHashSet<String>();
        for (XlsxSheetLike sheet : workbook.sheets) {
            if (sheet == null || sheet.name == null) {
                throw new IllegalArgumentException("Each sheet must have a valid sheet name");
            }
            validateSheetName(sheet.name);
            String canonicalName = sheet.name.toLowerCase(Locale.ROOT);
            if (!seenNames.add(canonicalName)) {
                throw new IllegalArgumentException("Duplicate sheet name is not allowed: " + sheet.name);
            }
            normalizeSheet(sheet);
        }
        return workbook;
    }

    public String normalizeMergedRange(String range) {
        if (range == null) {
            throw new IllegalArgumentException("Merged range must be a string");
        }
        String trimmed = range.trim().toUpperCase(Locale.ROOT);
        if (!MERGED_RANGE_PATTERN.matcher(trimmed).matches()) {
            throw new IllegalArgumentException("Invalid merged range: " + range);
        }
        return trimmed;
    }

    public void validateSheetName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Sheet name must not be empty");
        }
        if (name.length() > 31) {
            throw new IllegalArgumentException("Sheet name is too long: " + name);
        }
        if (INVALID_SHEET_NAME_PATTERN.matcher(name).find()) {
            throw new IllegalArgumentException("Sheet name contains invalid characters: " + name);
        }
        if (name.startsWith("'") || name.endsWith("'")) {
            throw new IllegalArgumentException("Sheet name must not start or end with apostrophe: " + name);
        }
    }

    public String normalizeColor(String color) {
        if (color == null || !COLOR_PATTERN.matcher(color).matches()) {
            throw new IllegalArgumentException("Unsupported color format: " + color);
        }
        String hex = color.startsWith("#") ? color.substring(1) : color;
        return "FF" + hex.toUpperCase(Locale.ROOT);
    }

    public String denormalizeColor(String color) {
        if (color == null) {
            return null;
        }
        String normalized = color.toUpperCase(Locale.ROOT);
        if (normalized.matches("^[0-9A-F]{8}$")) {
            return "#" + normalized.substring(2);
        }
        if (normalized.matches("^[0-9A-F]{6}$")) {
            return "#" + normalized;
        }
        return null;
    }

    private void normalizeSheet(XlsxSheetLike sheet) {
        if (sheet.columns != null) {
            for (XlsxColumnLike column : sheet.columns) {
                if (column == null) {
                    continue;
                }
                if (column.width != null && (!Double.isFinite(column.width.doubleValue()) || column.width.doubleValue() <= 0d)) {
                    throw new IllegalArgumentException("Column width must be a finite positive number");
                }
                if (column.hidden != null && !Boolean.TRUE.equals(column.hidden) && !Boolean.FALSE.equals(column.hidden)) {
                    throw new IllegalArgumentException("Column hidden must be boolean");
                }
            }
        }
        if (sheet.mergedRanges != null) {
            for (int index = 0; index < sheet.mergedRanges.size(); index++) {
                sheet.mergedRanges.set(index, normalizeMergedRange(sheet.mergedRanges.get(index)));
            }
        }
        if (sheet.dataValidations != null) {
            for (XlsxDataValidationLike dataValidation : sheet.dataValidations) {
                normalizeDataValidation(dataValidation);
            }
        }
        if (sheet.rows != null) {
            for (XlsxRowLike row : sheet.rows) {
                if (row == null) {
                    continue;
                }
                if (row.height != null && row.height.intValue() <= 0) {
                    throw new IllegalArgumentException("Row height must be a finite positive number");
                }
                if (row.cells != null) {
                    for (XlsxCellLike cell : row.cells) {
                        normalizeCell(cell);
                    }
                }
            }
        }
    }

    private void normalizeDataValidation(XlsxDataValidationLike dataValidation) {
        if (dataValidation == null) {
            throw new IllegalArgumentException("Data validation must be defined");
        }
        if (!"list".equals(dataValidation.type)) {
            throw new IllegalArgumentException("Unsupported data validation type: " + dataValidation.type);
        }
        if (dataValidation.formula1 == null || dataValidation.formula1.isEmpty()) {
            throw new IllegalArgumentException("Data validation formula1 must be a non-empty string");
        }
        if (dataValidation.sqref == null) {
            throw new IllegalArgumentException("Data validation sqref must be a string");
        }
        StringBuilder normalized = new StringBuilder();
        for (String part : dataValidation.sqref.trim().toUpperCase(Locale.ROOT).split("\\s+")) {
            if (part.isEmpty()) {
                continue;
            }
            if (!SQREF_CELL_PATTERN.matcher(part).matches() && !SQREF_RANGE_PATTERN.matcher(part).matches()) {
                throw new IllegalArgumentException("Invalid data validation sqref: " + part);
            }
            if (normalized.length() > 0) {
                normalized.append(' ');
            }
            normalized.append(part);
        }
        if (normalized.length() == 0) {
            throw new IllegalArgumentException("Data validation sqref must not be empty");
        }
        dataValidation.sqref = normalized.toString();
    }

    private void normalizeCell(XlsxCellLike cell) {
        if (cell == null) {
            return;
        }
        if (cell.value != null && !(cell.value instanceof String) && !(cell.value instanceof Number) && !(cell.value instanceof Boolean)) {
            throw new IllegalArgumentException("Cell value must be string, number, or boolean");
        }
        if (cell.fontSize != null && cell.fontSize.intValue() <= 0) {
            throw new IllegalArgumentException("Cell fontSize must be a finite positive number");
        }
        if (cell.fillColor != null) {
            cell.fillColor = normalizeColor(cell.fillColor);
        }
        if (cell.numberFormat != null
                && !"general".equals(cell.numberFormat)
                && !"integer".equals(cell.numberFormat)
                && !"decimal".equals(cell.numberFormat)
                && !"date".equals(cell.numberFormat)
                && !"datetime".equals(cell.numberFormat)
                && !"percent".equals(cell.numberFormat)
                && !"text".equals(cell.numberFormat)) {
            throw new IllegalArgumentException("Unsupported cell number format: " + cell.numberFormat);
        }
        if (cell.horizontalAlign != null
                && !"left".equals(cell.horizontalAlign)
                && !"center".equals(cell.horizontalAlign)
                && !"right".equals(cell.horizontalAlign)) {
            throw new IllegalArgumentException("Unsupported cell horizontal align: " + cell.horizontalAlign);
        }
        if (cell.verticalAlign != null
                && !"top".equals(cell.verticalAlign)
                && !"center".equals(cell.verticalAlign)
                && !"bottom".equals(cell.verticalAlign)) {
            throw new IllegalArgumentException("Unsupported cell vertical align: " + cell.verticalAlign);
        }
        if (cell.border != null && !"thin".equals(cell.border)) {
            throw new IllegalArgumentException("Unsupported cell border: " + cell.border);
        }
    }
}
