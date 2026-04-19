/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuproject.coreapi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import jp.igapyon.mikuproject.model.ProjectModel;
import jp.igapyon.mikuproject.projectworkbookjson.WorkbookJsonDocument;
import jp.igapyon.mikuproject.projectxlsx.XlsxWorkbookLike;
import jp.igapyon.mikuproject.msprojectxml.MsProjectXml;

public class CoreApiWorkbookTest {
    @Test
    public void importsWorkbookJsonWithAndWithoutBaseModel() throws IOException {
        CoreApiWorkbook api = new CoreApiWorkbook();
        ProjectModel baseModel = new MsProjectXml().importFromXml(readVendorTestdata("dependency.xml"));
        WorkbookJsonDocument document = api.workbookJson.exportProjectWorkbookJson(baseModel);
        findProjectNameRow(document).put("Value", "Core API workbook import");

        ProjectModel replaceModel = api.workbookJson.importProjectWorkbookJsonAsProjectModel(toDocumentLike(document)).model;
        ProjectModel mergeModel = api.workbookJson.importProjectWorkbookJson(toDocumentLike(document), baseModel).model;

        assertEquals("Core API workbook import", replaceModel.project.name);
        assertEquals("Core API workbook import", mergeModel.project.name);
    }

    @Test
    public void validatesWorkbookJsonAndAppliesPatchJson() throws IOException {
        CoreApiWorkbook api = new CoreApiWorkbook();
        ProjectModel baseModel = new MsProjectXml().importFromXml(readVendorTestdata("dependency.xml"));
        WorkbookJsonDocument document = api.workbookJson.exportProjectWorkbookJson(baseModel);
        findProjectNameRow(document).put("Value", "Core API text import");
        document.sheets.put("UnknownSheet", new java.util.ArrayList<java.util.Map<String, Object>>());

        assertEquals("mikuproject_workbook_json", api.workbookJson.validateWorkbookJsonDocument(toDocumentLike(document)).document.format);
        assertTrue(api.workbookJson.validateWorkbookJsonDocument(toDocumentLike(document)).warnings.get(0).message
                .contains("未知の sheet は無視します"));

        jp.igapyon.mikuproject.projectpatchjson.ProjectPatchJsonCore.ImportResult patchResult = api.patchJson
                .importProjectPatchJson(mapOf("operations",
                        Arrays.asList(mapOf("op", "update_project", "fields", mapOf("name", "Core API patch import")))),
                        baseModel);
        assertEquals("Core API patch import", patchResult.model.project.name);
        assertTrue(patchResult.changes.size() > 0);
    }

    @Test
    public void exposesProjectXlsxThroughUnifiedEntryPoint() throws IOException {
        CoreApiWorkbook api = new CoreApiWorkbook();
        ProjectModel baseModel = new MsProjectXml().importFromXml(readVendorTestdata("dependency.xml"));
        XlsxWorkbookLike workbook = api.xlsx.exportWorkbook(baseModel);
        workbook.sheets.get(0).rows.get(3).cells.get(1).value = "Core API xlsx import";

        ProjectModel replaceModel = api.xlsx.importAsProjectModel(workbook);
        ProjectModel mergeModel = api.xlsx.importIntoProjectModel(workbook, baseModel);

        assertEquals("Core API xlsx import", replaceModel.project.name);
        assertEquals("Core API xlsx import", mergeModel.project.name);
        assertNotNull(api.xlsx.importIntoProjectModelDetailed(workbook, baseModel));
    }

    @Test
    public void encodesAndDecodesWorkbookThroughUnifiedEntryPoint() throws IOException {
        CoreApiWorkbook api = new CoreApiWorkbook();
        ProjectModel baseModel = new MsProjectXml().importFromXml(readVendorTestdata("dependency.xml"));
        XlsxWorkbookLike workbook = api.xlsx.exportWorkbook(baseModel);
        byte[] bytes = api.xlsx.encodeWorkbook(workbook);
        XlsxWorkbookLike decoded = api.xlsx.decodeWorkbook(bytes);

        assertTrue(bytes.length > 0);
        assertEquals(workbook.sheets.get(0).name, decoded.sheets.get(0).name);
        assertEquals(workbook.sheets.get(0).rows.get(0).cells.get(0).value, decoded.sheets.get(0).rows.get(0).cells.get(0).value);
    }

    @Test
    public void roundTripsHierarchyFixtureThroughWorkbookWrappers() throws IOException {
        CoreApiWorkbook api = new CoreApiWorkbook();
        ProjectModel baseModel = new MsProjectXml().importFromXml(readVendorTestdata("hierarchy.xml"));

        WorkbookJsonDocument document = api.workbookJson.exportProjectWorkbookJson(baseModel);
        ProjectModel workbookJsonModel = api.workbookJson.importProjectWorkbookJsonAsProjectModel(toDocumentLike(document)).model;
        XlsxWorkbookLike workbook = api.xlsx.exportWorkbook(baseModel);
        ProjectModel xlsxModel = api.xlsx.importAsProjectModel(workbook);

        assertEquals("Hierarchy Project", workbookJsonModel.project.name);
        assertEquals(3, workbookJsonModel.tasks.size());
        assertEquals("1.2", workbookJsonModel.tasks.get(2).outlineNumber);
        assertEquals("Hierarchy Project", xlsxModel.project.name);
        assertEquals(3, xlsxModel.tasks.size());
        assertEquals("1.2", xlsxModel.tasks.get(2).outlineNumber);
    }

    private Map<String, Object> findProjectNameRow(WorkbookJsonDocument document) {
        for (Map<String, Object> row : document.sheets.get("Project")) {
            if ("Name".equals(row.get("Field"))) {
                return row;
            }
        }
        return null;
    }

    private Map<String, Object> toDocumentLike(WorkbookJsonDocument document) {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        map.put("format", document.format);
        map.put("version", document.version);
        map.put("sheets", document.sheets);
        return map;
    }

    private String readVendorTestdata(String fileName) throws IOException {
        byte[] bytes = Files.readAllBytes(Paths.get("vendor", "mikuproject", "testdata", fileName));
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private Map<String, Object> mapOf(Object... entries) {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        for (int i = 0; i < entries.length; i += 2) {
            map.put(String.valueOf(entries[i]), entries[i + 1]);
        }
        return map;
    }
}
