/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuproject.excelio;

import java.util.List;

public class ExcelIoPackageXml {
    public String buildContentTypesXml(int sheetCount, boolean includeStyles) {
        StringBuilder worksheetOverrides = new StringBuilder();
        for (int index = 0; index < sheetCount; index++) {
            worksheetOverrides.append("<Override PartName=\"/xl/worksheets/sheet").append(index + 1)
                    .append(".xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>");
        }
        String stylesOverride = includeStyles
                ? "<Override PartName=\"/xl/styles.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml\"/>"
                : "";

        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
                + "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">"
                + "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>"
                + "<Default Extension=\"xml\" ContentType=\"application/xml\"/>"
                + "<Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>"
                + worksheetOverrides.toString() + stylesOverride + "</Types>";
    }

    public String buildRootRelationshipsXml() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
                + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
                + "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/>"
                + "</Relationships>";
    }

    public String buildWorkbookRelationshipsXml(List<WorkbookRelationship> relationships, boolean includeStyles) {
        StringBuilder builder = new StringBuilder();
        builder.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n");
        builder.append("<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">");
        for (WorkbookRelationship relationship : relationships) {
            builder.append("<Relationship Id=\"").append(escapeXml(relationship.relationshipId))
                    .append("\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"")
                    .append(escapeXml(relationship.target)).append("\"/>");
        }
        if (includeStyles) {
            builder.append("<Relationship Id=\"rId").append(relationships.size() + 1)
                    .append("\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles\" Target=\"styles.xml\"/>");
        }
        builder.append("</Relationships>");
        return builder.toString();
    }

    public String buildWorkbookXml(List<WorkbookSheet> relationships) {
        StringBuilder builder = new StringBuilder();
        builder.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n");
        builder.append("<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">");
        builder.append("<sheets>");
        for (int index = 0; index < relationships.size(); index++) {
            WorkbookSheet relationship = relationships.get(index);
            builder.append("<sheet name=\"").append(escapeXml(relationship.name))
                    .append("\" sheetId=\"").append(index + 1)
                    .append("\" r:id=\"").append(escapeXml(relationship.relationshipId)).append("\"/>");
        }
        builder.append("</sheets></workbook>");
        return builder.toString();
    }

    private String escapeXml(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    public static class WorkbookRelationship {
        public final String relationshipId;
        public final String target;

        public WorkbookRelationship(String relationshipId, String target) {
            this.relationshipId = relationshipId;
            this.target = target;
        }
    }

    public static class WorkbookSheet {
        public final String relationshipId;
        public final String name;

        public WorkbookSheet(String relationshipId, String name) {
            this.relationshipId = relationshipId;
            this.name = name;
        }
    }
}
