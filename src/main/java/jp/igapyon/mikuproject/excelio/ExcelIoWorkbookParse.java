/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuproject.excelio;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import jp.igapyon.mikuproject.projectxlsx.XlsxWorkbookLike;

public class ExcelIoWorkbookParse {
    private final ExcelIoStylesParse stylesParse = new ExcelIoStylesParse();
    private final ExcelIoWorksheetParse worksheetParse = new ExcelIoWorksheetParse();

    public XlsxWorkbookLike parseWorkbookEntries(Map<String, byte[]> entries) {
        XmlSupport xmlSupport = new XmlSupport();
        String workbookXml = xmlSupport.decodeRequiredEntry(entries, "xl/workbook.xml");
        String workbookRelsXml = xmlSupport.decodeRequiredEntry(entries, "xl/_rels/workbook.xml.rels");
        String stylesXml = entries.containsKey("xl/styles.xml") ? ExcelIoUtil.decodeUtf8(entries.get("xl/styles.xml")) : null;

        Document workbookDocument = xmlSupport.parseXmlDocument(workbookXml);
        Document relsDocument = xmlSupport.parseXmlDocument(workbookRelsXml);
        Map<String, String> relationshipMap = new LinkedHashMap<String, String>();
        NodeList relationships = relsDocument.getElementsByTagNameNS("http://schemas.openxmlformats.org/package/2006/relationships",
                "Relationship");
        for (int index = 0; index < relationships.getLength(); index++) {
            Element relationship = (Element) relationships.item(index);
            String id = relationship.getAttribute("Id");
            String target = relationship.getAttribute("Target");
            if (id != null && !id.isEmpty() && target != null && !target.isEmpty()) {
                relationshipMap.put(id, target.startsWith("xl/") ? target : "xl/" + target.replaceFirst("^\\./", ""));
            }
        }

        List<ExcelIoStylesBuild.StyleDescriptor> styleBook = stylesParse.parseStylesXml(stylesXml, xmlSupport);
        XlsxWorkbookLike workbook = new XlsxWorkbookLike();
        NodeList sheetNodes = workbookDocument.getElementsByTagNameNS("http://schemas.openxmlformats.org/spreadsheetml/2006/main", "sheet");
        for (int index = 0; index < sheetNodes.getLength(); index++) {
            Element sheet = (Element) sheetNodes.item(index);
            String name = sheet.getAttribute("name");
            String relationshipId = sheet.getAttribute("r:id");
            if (relationshipId == null || relationshipId.isEmpty()) {
                relationshipId = sheet.getAttributeNS("http://schemas.openxmlformats.org/officeDocument/2006/relationships", "id");
            }
            String target = relationshipMap.get(relationshipId);
            if (target == null) {
                throw new IllegalArgumentException("worksheet relationship target is missing: " + name);
            }
            workbook.sheets.add(worksheetParse.parseWorksheetXml(name, xmlSupport.decodeRequiredEntry(entries, target), styleBook, xmlSupport));
        }
        return workbook;
    }

    public static class XmlSupport {
        public Document parseXmlDocument(String xmlText) {
            try {
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                factory.setNamespaceAware(true);
                return factory.newDocumentBuilder().parse(new ByteArrayInputStream(ExcelIoUtil.encodeUtf8(xmlText)));
            } catch (Exception ex) {
                throw new IllegalArgumentException("xml parse に失敗しました", ex);
            }
        }

        public Element findDirectChild(Element element, String localName) {
            Node child = element.getFirstChild();
            while (child != null) {
                if (child.getNodeType() == Node.ELEMENT_NODE && localName.equals(child.getLocalName())) {
                    return (Element) child;
                }
                child = child.getNextSibling();
            }
            return null;
        }

        public List<Element> directChildren(Element element, String localName) {
            List<Element> result = new ArrayList<Element>();
            Node child = element.getFirstChild();
            while (child != null) {
                if (child.getNodeType() == Node.ELEMENT_NODE && localName.equals(child.getLocalName())) {
                    result.add((Element) child);
                }
                child = child.getNextSibling();
            }
            return result;
        }

        public String decodeRequiredEntry(Map<String, byte[]> entries, String name) {
            byte[] bytes = entries.get(name);
            if (bytes == null) {
                throw new IllegalArgumentException("required entry is missing: " + name);
            }
            return ExcelIoUtil.decodeUtf8(bytes);
        }

        public String emptyToNull(String value) {
            return value == null || value.isEmpty() ? null : value;
        }
    }
}
