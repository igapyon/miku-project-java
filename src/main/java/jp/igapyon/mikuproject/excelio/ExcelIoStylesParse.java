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

public class ExcelIoStylesParse {
    public List<ExcelIoStylesBuild.StyleDescriptor> parseStylesXml(String xmlText, ExcelIoWorkbookParse.XmlSupport xmlSupport) {
        List<ExcelIoStylesBuild.StyleDescriptor> styles = new ArrayList<ExcelIoStylesBuild.StyleDescriptor>();
        styles.add(new ExcelIoStylesBuild.StyleDescriptor());
        if (xmlText == null || xmlText.isEmpty()) {
            return styles;
        }
        Document document = xmlSupport.parseXmlDocument(xmlText);
        NodeList cellXfs = document.getElementsByTagNameNS("http://schemas.openxmlformats.org/spreadsheetml/2006/main", "xf");
        for (int index = 0; index < cellXfs.getLength(); index++) {
            Element xf = (Element) cellXfs.item(index);
            if (xf.getParentNode() == null || !"cellXfs".equals(xf.getParentNode().getLocalName())) {
                continue;
            }
            ExcelIoStylesBuild.StyleDescriptor descriptor = new ExcelIoStylesBuild.StyleDescriptor();
            Element alignment = xmlSupport.findDirectChild(xf, "alignment");
            if (alignment != null) {
                descriptor.horizontalAlign = xmlSupport.emptyToNull(alignment.getAttribute("horizontal"));
                descriptor.verticalAlign = xmlSupport.emptyToNull(alignment.getAttribute("vertical"));
                descriptor.wrapText = "1".equals(alignment.getAttribute("wrapText")) ? Boolean.TRUE : null;
            }
            styles.add(descriptor);
        }
        return styles;
    }
}
