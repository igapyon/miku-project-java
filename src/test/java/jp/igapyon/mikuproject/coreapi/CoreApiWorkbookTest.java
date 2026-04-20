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
import java.util.List;
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
    public void mergesExtendedWorkbookEditableFieldsThroughUnifiedEntryPoint() throws IOException {
        CoreApiWorkbook api = new CoreApiWorkbook();
        ProjectModel baseModel = new MsProjectXml().importFromXml(readVendorTestdata("dependency.xml"));
        WorkbookJsonDocument document = api.workbookJson.exportProjectWorkbookJson(baseModel);
        Map<String, Object> taskRow = findRowByUid(document.sheets.get("Tasks"), "2");
        Map<String, Object> resourceRow = findRowByUid(document.sheets.get("Resources"), "1");
        Map<String, Object> assignmentRow = findRowByUid(document.sheets.get("Assignments"), "1");
        XlsxWorkbookLike workbook = api.xlsx.exportWorkbook(baseModel);
        jp.igapyon.mikuproject.projectxlsx.XlsxRowLike taskXlsxRow = findSheetRowByUid(findSheet(workbook, "Tasks"), "2");
        jp.igapyon.mikuproject.projectxlsx.XlsxRowLike resourceXlsxRow = findSheetRowByUid(findSheet(workbook, "Resources"), "1");
        jp.igapyon.mikuproject.projectxlsx.XlsxRowLike assignmentXlsxRow = findSheetRowByUid(findSheet(workbook, "Assignments"), "1");

        taskRow.put("Type", Integer.valueOf(2));
        taskRow.put("Priority", Integer.valueOf(500));
        taskRow.put("ConstraintType", Integer.valueOf(4));
        taskRow.put("ConstraintDate", "2026-03-17 09:00:00");
        taskRow.put("Deadline", "2026-03-18 18:00:00");
        resourceRow.put("Type", Integer.valueOf(1));
        resourceRow.put("Initials", "MU");
        resourceRow.put("StandardRate", "1200/h");
        resourceRow.put("OvertimeRate", "1800/h");
        resourceRow.put("CostPerUse", Double.valueOf(500));
        resourceRow.put("Work", "PT8H0M0S");
        resourceRow.put("ActualWork", "PT4H0M0S");
        resourceRow.put("RemainingWork", "PT4H0M0S");
        resourceRow.put("Cost", Double.valueOf(1000));
        resourceRow.put("ActualCost", Double.valueOf(500));
        resourceRow.put("RemainingCost", Double.valueOf(500));
        resourceRow.put("PercentWorkComplete", Integer.valueOf(50));
        resourceRow.put("WorkGroup", Integer.valueOf(0));
        resourceRow.put("StandardRateFormat", Integer.valueOf(2));
        resourceRow.put("OvertimeRateFormat", Integer.valueOf(2));
        assignmentRow.put("Start", "2026-03-16 09:00:00");
        assignmentRow.put("Finish", "2026-03-16 18:00:00");
        assignmentRow.put("StartVariance", "PT0H30M0S");
        assignmentRow.put("FinishVariance", "PT1H0M0S");
        assignmentRow.put("Delay", "PT0H15M0S");
        assignmentRow.put("Milestone", "○");
        assignmentRow.put("WorkContour", Integer.valueOf(1));
        assignmentRow.put("Cost", Double.valueOf(1000));
        assignmentRow.put("ActualWork", "PT4H0M0S");
        assignmentRow.put("RemainingWork", "PT4H0M0S");
        assignmentRow.put("ActualCost", Double.valueOf(300));
        assignmentRow.put("RemainingCost", Double.valueOf(200));
        assignmentRow.put("OvertimeWork", "PT1H0M0S");
        assignmentRow.put("ActualOvertimeWork", "PT0H30M0S");
        taskXlsxRow.cells.get(14).value = "2";
        taskXlsxRow.cells.get(15).value = "500";
        taskXlsxRow.cells.get(17).value = "4";
        taskXlsxRow.cells.get(18).value = "2026-03-17 09:00:00";
        taskXlsxRow.cells.get(19).value = "2026-03-18 18:00:00";
        resourceXlsxRow.cells.get(3).value = "1";
        resourceXlsxRow.cells.get(4).value = "MU";
        resourceXlsxRow.cells.get(8).value = "1200/h";
        resourceXlsxRow.cells.get(9).value = "1800/h";
        resourceXlsxRow.cells.get(10).value = "500";
        resourceXlsxRow.cells.get(11).value = "PT8H0M0S";
        resourceXlsxRow.cells.get(12).value = "PT4H0M0S";
        resourceXlsxRow.cells.get(13).value = "PT4H0M0S";
        resourceXlsxRow.cells.get(14).value = "1000";
        resourceXlsxRow.cells.get(15).value = "500";
        resourceXlsxRow.cells.get(16).value = "500";
        resourceXlsxRow.cells.get(17).value = "50";
        resourceXlsxRow.cells.get(18).value = "0";
        resourceXlsxRow.cells.get(19).value = "2";
        resourceXlsxRow.cells.get(20).value = "2";
        assignmentXlsxRow.cells.get(5).value = "2026-03-16 09:00:00";
        assignmentXlsxRow.cells.get(6).value = "2026-03-16 18:00:00";
        assignmentXlsxRow.cells.get(7).value = "PT0H30M0S";
        assignmentXlsxRow.cells.get(8).value = "PT1H0M0S";
        assignmentXlsxRow.cells.get(9).value = "PT0H15M0S";
        assignmentXlsxRow.cells.get(10).value = "○";
        assignmentXlsxRow.cells.get(11).value = "1";
        assignmentXlsxRow.cells.get(14).value = "1000";
        assignmentXlsxRow.cells.get(15).value = "PT4H0M0S";
        assignmentXlsxRow.cells.get(16).value = "PT4H0M0S";
        assignmentXlsxRow.cells.get(17).value = "300";
        assignmentXlsxRow.cells.get(18).value = "200";
        assignmentXlsxRow.cells.get(19).value = "PT1H0M0S";
        assignmentXlsxRow.cells.get(20).value = "PT0H30M0S";

        ProjectModel workbookJsonModel = api.workbookJson.importProjectWorkbookJson(toDocumentLike(document), baseModel).model;
        ProjectModel xlsxModel = api.xlsx.importIntoProjectModel(workbook, baseModel);

        assertEquals(Integer.valueOf(2), workbookJsonModel.tasks.get(1).type);
        assertEquals(Integer.valueOf(500), workbookJsonModel.tasks.get(1).priority);
        assertEquals(Integer.valueOf(4), workbookJsonModel.tasks.get(1).constraintType);
        assertEquals("2026-03-17T09:00:00", workbookJsonModel.tasks.get(1).constraintDate);
        assertEquals("2026-03-18T18:00:00", workbookJsonModel.tasks.get(1).deadline);
        assertEquals(Integer.valueOf(1), workbookJsonModel.resources.get(0).type);
        assertEquals("MU", workbookJsonModel.resources.get(0).initials);
        assertEquals("1200/h", workbookJsonModel.resources.get(0).standardRate);
        assertEquals("1800/h", workbookJsonModel.resources.get(0).overtimeRate);
        assertEquals(Double.valueOf(500), workbookJsonModel.resources.get(0).costPerUse);
        assertEquals("PT8H0M0S", workbookJsonModel.resources.get(0).work);
        assertEquals("PT4H0M0S", workbookJsonModel.resources.get(0).actualWork);
        assertEquals("PT4H0M0S", workbookJsonModel.resources.get(0).remainingWork);
        assertEquals(Double.valueOf(1000), workbookJsonModel.resources.get(0).cost);
        assertEquals(Double.valueOf(500), workbookJsonModel.resources.get(0).actualCost);
        assertEquals(Double.valueOf(500), workbookJsonModel.resources.get(0).remainingCost);
        assertEquals(Integer.valueOf(50), workbookJsonModel.resources.get(0).percentWorkComplete);
        assertEquals(Integer.valueOf(0), workbookJsonModel.resources.get(0).workGroup);
        assertEquals(Integer.valueOf(2), workbookJsonModel.resources.get(0).standardRateFormat);
        assertEquals(Integer.valueOf(2), workbookJsonModel.resources.get(0).overtimeRateFormat);
        assertEquals("2026-03-16T09:00:00", workbookJsonModel.assignments.get(0).start);
        assertEquals("2026-03-16T18:00:00", workbookJsonModel.assignments.get(0).finish);
        assertEquals("PT0H30M0S", workbookJsonModel.assignments.get(0).startVariance);
        assertEquals("PT1H0M0S", workbookJsonModel.assignments.get(0).finishVariance);
        assertEquals("PT0H15M0S", workbookJsonModel.assignments.get(0).delay);
        assertEquals(Boolean.TRUE, workbookJsonModel.assignments.get(0).milestone);
        assertEquals(Integer.valueOf(1), workbookJsonModel.assignments.get(0).workContour);
        assertEquals(Double.valueOf(1000), workbookJsonModel.assignments.get(0).cost);
        assertEquals("PT4H0M0S", workbookJsonModel.assignments.get(0).actualWork);
        assertEquals("PT4H0M0S", workbookJsonModel.assignments.get(0).remainingWork);
        assertEquals(Double.valueOf(300), workbookJsonModel.assignments.get(0).actualCost);
        assertEquals(Double.valueOf(200), workbookJsonModel.assignments.get(0).remainingCost);
        assertEquals("PT1H0M0S", workbookJsonModel.assignments.get(0).overtimeWork);
        assertEquals("PT0H30M0S", workbookJsonModel.assignments.get(0).actualOvertimeWork);
        assertEquals(Integer.valueOf(2), xlsxModel.tasks.get(1).type);
        assertEquals(Integer.valueOf(500), xlsxModel.tasks.get(1).priority);
        assertEquals(Integer.valueOf(4), xlsxModel.tasks.get(1).constraintType);
        assertEquals("2026-03-17T09:00:00", xlsxModel.tasks.get(1).constraintDate);
        assertEquals("2026-03-18T18:00:00", xlsxModel.tasks.get(1).deadline);
        assertEquals(Integer.valueOf(1), xlsxModel.resources.get(0).type);
        assertEquals("MU", xlsxModel.resources.get(0).initials);
        assertEquals("1200/h", xlsxModel.resources.get(0).standardRate);
        assertEquals("1800/h", xlsxModel.resources.get(0).overtimeRate);
        assertEquals(Double.valueOf(500), xlsxModel.resources.get(0).costPerUse);
        assertEquals("PT8H0M0S", xlsxModel.resources.get(0).work);
        assertEquals("PT4H0M0S", xlsxModel.resources.get(0).actualWork);
        assertEquals("PT4H0M0S", xlsxModel.resources.get(0).remainingWork);
        assertEquals(Double.valueOf(1000), xlsxModel.resources.get(0).cost);
        assertEquals(Double.valueOf(500), xlsxModel.resources.get(0).actualCost);
        assertEquals(Double.valueOf(500), xlsxModel.resources.get(0).remainingCost);
        assertEquals(Integer.valueOf(50), xlsxModel.resources.get(0).percentWorkComplete);
        assertEquals(Integer.valueOf(0), xlsxModel.resources.get(0).workGroup);
        assertEquals(Integer.valueOf(2), xlsxModel.resources.get(0).standardRateFormat);
        assertEquals(Integer.valueOf(2), xlsxModel.resources.get(0).overtimeRateFormat);
        assertEquals("2026-03-16T09:00:00", xlsxModel.assignments.get(0).start);
        assertEquals("2026-03-16T18:00:00", xlsxModel.assignments.get(0).finish);
        assertEquals("PT0H30M0S", xlsxModel.assignments.get(0).startVariance);
        assertEquals("PT1H0M0S", xlsxModel.assignments.get(0).finishVariance);
        assertEquals("PT0H15M0S", xlsxModel.assignments.get(0).delay);
        assertEquals(Boolean.TRUE, xlsxModel.assignments.get(0).milestone);
        assertEquals(Integer.valueOf(1), xlsxModel.assignments.get(0).workContour);
        assertEquals(Double.valueOf(1000), xlsxModel.assignments.get(0).cost);
        assertEquals("PT4H0M0S", xlsxModel.assignments.get(0).actualWork);
        assertEquals("PT4H0M0S", xlsxModel.assignments.get(0).remainingWork);
        assertEquals(Double.valueOf(300), xlsxModel.assignments.get(0).actualCost);
        assertEquals(Double.valueOf(200), xlsxModel.assignments.get(0).remainingCost);
        assertEquals("PT1H0M0S", xlsxModel.assignments.get(0).overtimeWork);
        assertEquals("PT0H30M0S", xlsxModel.assignments.get(0).actualOvertimeWork);
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

    private Map<String, Object> findRowByUid(List<Map<String, Object>> rows, String uid) {
        for (Map<String, Object> row : rows) {
            if (uid.equals(row.get("UID"))) {
                return row;
            }
        }
        return null;
    }

    private jp.igapyon.mikuproject.projectxlsx.XlsxSheetLike findSheet(XlsxWorkbookLike workbook, String name) {
        for (jp.igapyon.mikuproject.projectxlsx.XlsxSheetLike sheet : workbook.sheets) {
            if (name.equals(sheet.name)) {
                return sheet;
            }
        }
        return null;
    }

    private jp.igapyon.mikuproject.projectxlsx.XlsxRowLike findSheetRowByUid(jp.igapyon.mikuproject.projectxlsx.XlsxSheetLike sheet, String uid) {
        for (int index = 3; index < sheet.rows.size(); index += 1) {
            jp.igapyon.mikuproject.projectxlsx.XlsxRowLike row = sheet.rows.get(index);
            if (uid.equals(String.valueOf(row.cells.get(0).value))) {
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
