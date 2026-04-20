/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuproject.excelio;

import java.util.ArrayList;
import java.util.List;

import jp.igapyon.mikuproject.projectxlsx.XlsxWorkbookLike;

public class ExcelIoWorkbookBuild {
    private final ExcelIoStylesBuild stylesBuild = new ExcelIoStylesBuild();
    private final ExcelIoWorksheetBuild worksheetBuild = new ExcelIoWorksheetBuild();

    public List<ExcelIoZip.ZipEntryData> createWorkbookEntries(XlsxWorkbookLike workbook, ExcelIoPackageXml packageXml) {
        ExcelIoStylesBuild.StyleBook styleBook = stylesBuild.createStyleBook(workbook);
        boolean includeStyles = styleBook.styles.size() > 1;
        List<ExcelIoZip.ZipEntryData> entries = new ArrayList<ExcelIoZip.ZipEntryData>();
        entries.add(new ExcelIoZip.ZipEntryData("[Content_Types].xml",
                ExcelIoUtil.encodeUtf8(packageXml.buildContentTypesXml(workbook.sheets.size(), includeStyles))));
        entries.add(new ExcelIoZip.ZipEntryData("_rels/.rels", ExcelIoUtil.encodeUtf8(packageXml.buildRootRelationshipsXml())));

        List<ExcelIoPackageXml.WorkbookRelationship> relationships = new ArrayList<ExcelIoPackageXml.WorkbookRelationship>();
        List<ExcelIoPackageXml.WorkbookSheet> sheets = new ArrayList<ExcelIoPackageXml.WorkbookSheet>();
        for (int index = 0; index < workbook.sheets.size(); index++) {
            relationships.add(new ExcelIoPackageXml.WorkbookRelationship("rId" + (index + 1), "worksheets/sheet" + (index + 1) + ".xml"));
            sheets.add(new ExcelIoPackageXml.WorkbookSheet("rId" + (index + 1), workbook.sheets.get(index).name));
        }
        entries.add(new ExcelIoZip.ZipEntryData("xl/_rels/workbook.xml.rels",
                ExcelIoUtil.encodeUtf8(packageXml.buildWorkbookRelationshipsXml(relationships, includeStyles))));
        entries.add(new ExcelIoZip.ZipEntryData("xl/workbook.xml", ExcelIoUtil.encodeUtf8(packageXml.buildWorkbookXml(sheets))));
        if (includeStyles) {
            entries.add(new ExcelIoZip.ZipEntryData("xl/styles.xml", ExcelIoUtil.encodeUtf8(stylesBuild.buildStylesXml(styleBook.styles))));
        }
        for (int index = 0; index < workbook.sheets.size(); index++) {
            entries.add(new ExcelIoZip.ZipEntryData("xl/worksheets/sheet" + (index + 1) + ".xml",
                    ExcelIoUtil.encodeUtf8(worksheetBuild.buildWorksheetXml(workbook.sheets.get(index), styleBook, stylesBuild))));
        }
        return entries;
    }
}
