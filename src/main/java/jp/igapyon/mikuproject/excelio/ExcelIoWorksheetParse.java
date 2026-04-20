/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuproject.excelio;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import jp.igapyon.mikuproject.projectxlsx.XlsxCellLike;
import jp.igapyon.mikuproject.projectxlsx.XlsxColumnLike;
import jp.igapyon.mikuproject.projectxlsx.XlsxDataValidationLike;
import jp.igapyon.mikuproject.projectxlsx.XlsxFreezePaneLike;
import jp.igapyon.mikuproject.projectxlsx.XlsxRowLike;
import jp.igapyon.mikuproject.projectxlsx.XlsxSheetLike;

public class ExcelIoWorksheetParse {
    public XlsxSheetLike parseWorksheetXml(String name, String xmlText, List<ExcelIoStylesBuild.StyleDescriptor> styleBook,
            ExcelIoWorkbookParse.XmlSupport xmlSupport) {
        Document document = xmlSupport.parseXmlDocument(xmlText);
        XlsxSheetLike sheet = new XlsxSheetLike();
        sheet.name = name;
        sheet.freezePane = parseFreezePane(document);

        NodeList colNodes = document.getElementsByTagNameNS("http://schemas.openxmlformats.org/spreadsheetml/2006/main", "col");
        for (int index = 0; index < colNodes.getLength(); index++) {
            Element col = (Element) colNodes.item(index);
            XlsxColumnLike column = new XlsxColumnLike();
            String width = col.getAttribute("width");
            column.width = width == null || width.isEmpty() ? null : Double.valueOf(Double.parseDouble(width));
            column.hidden = "1".equals(col.getAttribute("hidden")) ? Boolean.TRUE : null;
            sheet.columns.add(column);
        }

        NodeList rowNodes = document.getElementsByTagNameNS("http://schemas.openxmlformats.org/spreadsheetml/2006/main", "row");
        for (int rowIndex = 0; rowIndex < rowNodes.getLength(); rowIndex++) {
            Element rowElement = (Element) rowNodes.item(rowIndex);
            XlsxRowLike row = new XlsxRowLike();
            String height = rowElement.getAttribute("ht");
            row.height = height == null || height.isEmpty() ? null : Integer.valueOf((int) Double.parseDouble(height));
            List<Element> cellElements = xmlSupport.directChildren(rowElement, "c");
            int currentColumn = 0;
            for (Element cellElement : cellElements) {
                String ref = cellElement.getAttribute("r");
                int targetColumn = decodeColumnIndex(ref);
                while (currentColumn < targetColumn) {
                    row.cells.add(new XlsxCellLike());
                    currentColumn++;
                }
                row.cells.add(parseCell(cellElement, styleBook, xmlSupport));
                currentColumn++;
            }
            while (currentColumn < sheet.columns.size()) {
                row.cells.add(new XlsxCellLike());
                currentColumn++;
            }
            sheet.rows.add(row);
        }

        NodeList mergeNodes = document.getElementsByTagNameNS("http://schemas.openxmlformats.org/spreadsheetml/2006/main", "mergeCell");
        for (int index = 0; index < mergeNodes.getLength(); index++) {
            Element merge = (Element) mergeNodes.item(index);
            sheet.mergedRanges.add(merge.getAttribute("ref"));
        }
        NodeList validationNodes = document.getElementsByTagNameNS("http://schemas.openxmlformats.org/spreadsheetml/2006/main",
                "dataValidation");
        for (int index = 0; index < validationNodes.getLength(); index++) {
            Element validationElement = (Element) validationNodes.item(index);
            XlsxDataValidationLike validation = new XlsxDataValidationLike();
            validation.type = validationElement.getAttribute("type");
            validation.sqref = validationElement.getAttribute("sqref");
            validation.allowBlank = "1".equals(validationElement.getAttribute("allowBlank")) ? Boolean.TRUE : null;
            Element formula1 = xmlSupport.findDirectChild(validationElement, "formula1");
            validation.formula1 = formula1 == null ? "" : formula1.getTextContent();
            sheet.dataValidations.add(validation);
        }
        padRowsToSheetWidth(sheet);
        return sheet;
    }

    private void padRowsToSheetWidth(XlsxSheetLike sheet) {
        int maxCells = sheet.columns.size();
        for (XlsxRowLike row : sheet.rows) {
            maxCells = Math.max(maxCells, row.cells.size());
        }
        for (XlsxRowLike row : sheet.rows) {
            while (row.cells.size() < maxCells) {
                row.cells.add(new XlsxCellLike());
            }
        }
    }

    private XlsxCellLike parseCell(Element cellElement, List<ExcelIoStylesBuild.StyleDescriptor> styleBook,
            ExcelIoWorkbookParse.XmlSupport xmlSupport) {
        XlsxCellLike cell = new XlsxCellLike();
        String type = cellElement.getAttribute("t");
        String styleIndexText = cellElement.getAttribute("s");
        if (styleIndexText != null && !styleIndexText.isEmpty()) {
            int styleIndex = Integer.parseInt(styleIndexText);
            if (styleIndex >= 0 && styleIndex < styleBook.size()) {
                ExcelIoStylesBuild.StyleDescriptor descriptor = styleBook.get(styleIndex);
                cell.horizontalAlign = descriptor.horizontalAlign;
                cell.verticalAlign = descriptor.verticalAlign;
                cell.wrapText = descriptor.wrapText;
            }
        }
        Element valueElement = xmlSupport.findDirectChild(cellElement, "v");
        Element formulaElement = xmlSupport.findDirectChild(cellElement, "f");
        Element inlineString = xmlSupport.findDirectChild(cellElement, "is");
        if (formulaElement != null) {
            cell.formula = formulaElement.getTextContent();
        }
        if ("b".equals(type) && valueElement != null) {
            cell.value = Boolean.valueOf("1".equals(valueElement.getTextContent()));
        } else if ("inlineStr".equals(type) && inlineString != null) {
            Element text = xmlSupport.findDirectChild(inlineString, "t");
            cell.value = text == null ? "" : text.getTextContent();
        } else if ("str".equals(type) && valueElement != null) {
            cell.value = valueElement.getTextContent();
        } else if (valueElement != null) {
            String raw = valueElement.getTextContent();
            try {
                if (raw.contains(".")) {
                    double parsed = Double.parseDouble(raw);
                    cell.value = parsed == Math.rint(parsed) ? Integer.valueOf((int) parsed) : Double.valueOf(parsed);
                } else {
                    cell.value = Integer.valueOf(Integer.parseInt(raw));
                }
            } catch (NumberFormatException ex) {
                cell.value = raw;
            }
        }
        return cell;
    }

    private XlsxFreezePaneLike parseFreezePane(Document document) {
        NodeList paneNodes = document.getElementsByTagNameNS("http://schemas.openxmlformats.org/spreadsheetml/2006/main", "pane");
        if (paneNodes.getLength() == 0) {
            return null;
        }
        Element pane = (Element) paneNodes.item(0);
        if (!"frozen".equals(pane.getAttribute("state"))) {
            return null;
        }
        XlsxFreezePaneLike freezePane = new XlsxFreezePaneLike();
        String rowSplit = pane.getAttribute("ySplit");
        String colSplit = pane.getAttribute("xSplit");
        freezePane.rowSplit = rowSplit == null || rowSplit.isEmpty() ? null : Integer.valueOf((int) Double.parseDouble(rowSplit));
        freezePane.colSplit = colSplit == null || colSplit.isEmpty() ? null : Integer.valueOf((int) Double.parseDouble(colSplit));
        if (freezePane.rowSplit == null && freezePane.colSplit == null) {
            return null;
        }
        return freezePane;
    }

    private int decodeColumnIndex(String reference) {
        int result = 0;
        for (int index = 0; index < reference.length(); index++) {
            char ch = reference.charAt(index);
            if (!Character.isLetter(ch)) {
                break;
            }
            result = result * 26 + (Character.toUpperCase(ch) - 'A' + 1);
        }
        return Math.max(0, result - 1);
    }
}
