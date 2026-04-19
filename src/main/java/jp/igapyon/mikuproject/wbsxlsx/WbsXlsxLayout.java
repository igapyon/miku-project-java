/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuproject.wbsxlsx;

public class WbsXlsxLayout {
    public String columnName(int columnIndex) {
        if (columnIndex < 0) {
            throw new IllegalArgumentException("columnIndex must be >= 0");
        }
        int current = columnIndex;
        StringBuilder builder = new StringBuilder();
        do {
            int remainder = current % 26;
            builder.insert(0, (char) ('A' + remainder));
            current = current / 26 - 1;
        } while (current >= 0);
        return builder.toString();
    }

    public int columnIndex(String columnReference) {
        if (columnReference == null || columnReference.isEmpty()) {
            throw new IllegalArgumentException("columnReference must not be empty");
        }
        int result = 0;
        for (int index = 0; index < columnReference.length(); index++) {
            char ch = Character.toUpperCase(columnReference.charAt(index));
            if (ch < 'A' || ch > 'Z') {
                throw new IllegalArgumentException("invalid column reference: " + columnReference);
            }
            result = result * 26 + (ch - 'A' + 1);
        }
        return result - 1;
    }

    public String reference(int rowNumber, int columnIndex) {
        return columnName(columnIndex) + rowNumber;
    }

    public ParsedCellReference parseCellReference(String reference) {
        if (reference == null || reference.isEmpty()) {
            throw new IllegalArgumentException("reference must not be empty");
        }
        int splitIndex = 0;
        while (splitIndex < reference.length() && Character.isLetter(reference.charAt(splitIndex))) {
            splitIndex++;
        }
        if (splitIndex == 0 || splitIndex >= reference.length()) {
            throw new IllegalArgumentException("invalid cell reference: " + reference);
        }
        String columnName = reference.substring(0, splitIndex).toUpperCase();
        int rowNumber = Integer.parseInt(reference.substring(splitIndex));
        ParsedCellReference result = new ParsedCellReference();
        result.reference = columnName + rowNumber;
        result.rowNumber = rowNumber;
        result.rowIndex = rowNumber - 1;
        result.columnName = columnName;
        result.columnIndex = columnIndex(columnName);
        return result;
    }

    public String range(String startReference, String endReference) {
        return startReference + ":" + endReference;
    }

    public String describeCell(String reference) {
        ParsedCellReference parsed = parseCellReference(reference);
        return parsed.reference + " (row " + parsed.rowNumber + ", rowIndex " + parsed.rowIndex + ", column "
                + parsed.columnName + ", columnIndex " + parsed.columnIndex + ")";
    }

    public String logCell(String reference, String label, Logger logger) {
        String message = (label == null || label.isEmpty() ? "" : label + ": ") + describeCell(reference);
        if (logger != null) {
            logger.log(message);
        }
        return message;
    }

    public interface Logger {
        void log(String message);
    }

    public static class ParsedCellReference {
        public String reference;
        public int rowNumber;
        public int rowIndex;
        public String columnName;
        public int columnIndex;
    }
}
