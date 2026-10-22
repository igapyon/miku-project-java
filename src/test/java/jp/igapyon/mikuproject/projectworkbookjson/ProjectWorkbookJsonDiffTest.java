/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuproject.projectworkbookjson;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import jp.igapyon.mikuproject.coreapi.CoreApiAiJsonUtil;

public class ProjectWorkbookJsonDiffTest {
    private final ProjectWorkbookJsonDiff diff = new ProjectWorkbookJsonDiff();
    private final CoreApiAiJsonUtil json = new CoreApiAiJsonUtil();

    @Test
    public void comparesProjectNameByField() throws IOException {
        Map<String, Object> before = base();
        Map<String, Object> after = copy(before);
        row(after, "Project", 0).put("Value", "Updated project");

        assertOnlyChange(diff.diffDocuments(before, after), "Project", "project", "Name", "Project", "Updated project");
    }

    @Test
    public void comparesAColumnOutsideMergeImport() throws IOException {
        Map<String, Object> before = base();
        Map<String, Object> after = copy(before);
        row(after, "Resources", 0).put("StandardRate", "123");

        assertOnlyChange(diff.diffDocuments(before, after), "Resources", "1", "StandardRate", null, "123");
    }

    @Test
    public void treatsMissingAndNullAsEquivalent() throws IOException {
        Map<String, Object> before = base();
        Map<String, Object> after = copy(before);
        row(after, "Tasks", 0).put("Notes", null);

        assertEquals(0, diff.diffDocuments(before, after).changes.size());
    }

    @Test
    public void distinguishesEmptyStringFromNull() throws IOException {
        Map<String, Object> before = base();
        row(before, "Tasks", 0).put("Notes", null);
        Map<String, Object> after = copy(before);
        row(after, "Tasks", 0).put("Notes", "");

        assertOnlyChange(diff.diffDocuments(before, after), "Tasks", "1", "Notes", null, "");
    }

    @Test
    public void preservesZeroFromNull() throws IOException {
        Map<String, Object> before = base();
        Map<String, Object> after = copy(before);
        row(after, "Tasks", 0).put("PercentComplete", Integer.valueOf(0));

        assertOnlyChange(diff.diffDocuments(before, after), "Tasks", "1", "PercentComplete", null, Integer.valueOf(0));
    }

    @Test
    public void preservesFalseFromNull() throws IOException {
        Map<String, Object> before = base();
        Map<String, Object> after = copy(before);
        row(after, "Tasks", 0).put("Milestone", Boolean.FALSE);

        assertOnlyChange(diff.diffDocuments(before, after), "Tasks", "1", "Milestone", null, Boolean.FALSE);
    }

    @Test
    public void distinguishesNumericAndStringValues() throws IOException {
        Map<String, Object> before = base();
        Map<String, Object> after = copy(before);
        row(after, "Tasks", 0).put("OutlineLevel", "1");

        assertOnlyChange(diff.diffDocuments(before, after), "Tasks", "1", "OutlineLevel", Integer.valueOf(1), "1");
    }

    @Test
    public void detectsAddedAndRemovedRowsForEveryEntitySheet() throws IOException {
        String[] sheets = { "Tasks", "Resources", "Assignments", "Calendars", "NonWorkingDays" };
        String[] scopes = { "tasks", "resources", "assignments", "calendars", "calendars" };
        String[] uids = { "new-task", "new-resource", "new-assignment", "new-calendar", "1" };
        String[] rowKeys = { "new-task", "new-resource", "new-assignment", "new-calendar", "[\"1\",\"1\"]" };
        String[] removedUids = { "2", "2", "1", "1", "1" };
        String[] removedRowKeys = { "2", "2", "1", "1", "[\"1\",\"0\"]" };
        for (int index = 0; index < sheets.length; index++) {
            Map<String, Object> beforeAdd = base();
            Map<String, Object> afterAdd = copy(beforeAdd);
            List<Map<String, Object>> addedRows = rows(afterAdd, sheets[index]);
            int addPosition = addedRows.size();
            addedRows.add(newRow(sheets[index]));
            ProjectWorkbookJsonDiff.Result added = diff.diffDocuments(beforeAdd, afterAdd);
            assertEquals(1, added.changes.size(), sheets[index] + " addition");
            Map<String, Object> add = added.changes.get(0);
            assertEquals("row-added", add.get("change_type"), sheets[index] + " addition type");
            assertEquals(scopes[index], add.get("scope"), sheets[index] + " addition scope");
            assertEquals(uids[index], add.get("uid"), sheets[index] + " addition uid");
            assertEquals(rowKeys[index], add.get("row_key"), sheets[index] + " addition row key");
            assertEquals(Integer.valueOf(addPosition), add.get("after_position"), sheets[index] + " addition position");

            Map<String, Object> beforeRemove = base();
            Map<String, Object> afterRemove = copy(beforeRemove);
            int removePosition = rows(afterRemove, sheets[index]).size() - 1;
            rows(afterRemove, sheets[index]).remove(removePosition);
            ProjectWorkbookJsonDiff.Result removed = diff.diffDocuments(beforeRemove, afterRemove);
            assertEquals(1, removed.changes.size(), sheets[index] + " removal");
            Map<String, Object> remove = removed.changes.get(0);
            assertEquals("row-removed", remove.get("change_type"), sheets[index] + " removal type");
            assertEquals(scopes[index], remove.get("scope"), sheets[index] + " removal scope");
            assertEquals(removedUids[index], remove.get("uid"), sheets[index] + " removal uid");
            assertEquals(removedRowKeys[index], remove.get("row_key"), sheets[index] + " removal row key");
            assertEquals(Integer.valueOf(removePosition), remove.get("before_position"), sheets[index] + " removal position");
            assertEquals(null, remove.get("after_position"), sheets[index] + " removal has no after position");
        }
    }

    @Test
    public void comparesProjectRowsByFieldInsteadOfPosition() throws IOException {
        Map<String, Object> before = base();
        Map<String, Object> after = copy(before);
        java.util.Collections.reverse(rows(after, "Project"));
        assertEquals(0, diff.diffDocuments(before, after).changes.size());

        after = copy(before);
        rows(after, "Project").remove(0);
        ProjectWorkbookJsonDiff.Result result = diff.diffDocuments(before, after);
        assertOnlyChange(result, "Project", "project", "Name", "Project", null);
        assertEquals("Name", result.changes.get(0).get("row_key"));
    }

    @Test
    public void detectsTaskReorderingButNotShiftsFromFrontInsertion() throws IOException {
        Map<String, Object> before = base();
        Map<String, Object> after = copy(before);
        java.util.Collections.reverse(rows(after, "Tasks"));
        ProjectWorkbookJsonDiff.Result reordered = diff.diffDocuments(before, after);
        assertEquals(2, reordered.changes.size());
        assertOrderChange(reordered.changes.get(0), "1", 0, 1);
        assertOrderChange(reordered.changes.get(1), "2", 1, 0);

        after = copy(before);
        rows(after, "Tasks").add(0, newRow("Tasks"));
        ProjectWorkbookJsonDiff.Result inserted = diff.diffDocuments(before, after);
        assertEquals(1, inserted.changes.size());
        assertEquals("row-added", inserted.changes.get(0).get("change_type"));
        assertEquals("new-task", inserted.changes.get(0).get("row_key"));
        assertEquals(Integer.valueOf(0), inserted.changes.get(0).get("after_position"));
    }

    @Test
    public void treatsNonTaskSheetsAsKeyedSets() throws IOException {
        Map<String, Object> before = base();
        for (String sheet : Arrays.asList("Resources", "Assignments", "Calendars", "NonWorkingDays")) {
            rows(before, sheet).add(newRow(sheet));
        }
        Map<String, Object> after = copy(before);
        for (String sheet : Arrays.asList("Resources", "Assignments", "Calendars", "NonWorkingDays")) {
            java.util.Collections.reverse(rows(after, sheet));
        }
        assertEquals(0, diff.diffDocuments(before, after).changes.size());
    }

    @Test
    public void reportsUnknownColumnOnEachSideAndDoesNotMutateInputs() throws IOException {
        Map<String, Object> before = base();
        Map<String, Object> after = copy(before);
        row(before, "Tasks", 0).put("AgentNote", "before-only information");
        row(after, "Tasks", 0).put("AgentNote", "before-only information");
        String beforeOriginal = json.stringifyJson(before);
        String afterOriginal = json.stringifyJson(after);

        ProjectWorkbookJsonDiff.Result result = diff.diffDocuments(before, after);

        assertTrue(result.changes.isEmpty());
        assertFalse(result.comparisonComplete);
        assertEquals(2, result.warnings.size());
        assertEquals("before: 未知の列は無視します: Tasks[0].AgentNote", result.warnings.get(0).message);
        assertEquals("after: 未知の列は無視します: Tasks[0].AgentNote", result.warnings.get(1).message);
        assertEquals(beforeOriginal, json.stringifyJson(before));
        assertEquals(afterOriginal, json.stringifyJson(after));
    }

    @Test
    public void reportsAnUnknownSheetWithoutDeclaringTheComparisonComplete() throws IOException {
        Map<String, Object> before = base();
        Map<String, Object> after = copy(before);
        sheets(after).put("CustomData", Arrays.<Object>asList(values("Field", "value")));

        ProjectWorkbookJsonDiff.Result result = diff.diffDocuments(before, after);

        assertTrue(result.changes.isEmpty());
        assertFalse(result.comparisonComplete);
        assertEquals(1, result.warnings.size());
        assertEquals("after: 未知の sheet は無視します: CustomData", result.warnings.get(0).message);
    }

    @Test
    public void reportsAnUnknownProjectFieldWithoutDeclaringTheComparisonComplete() throws IOException {
        Map<String, Object> before = base();
        Map<String, Object> after = copy(before);
        rows(after, "Project").add(values("Field", "CustomField", "Value", "value"));

        ProjectWorkbookJsonDiff.Result result = diff.diffDocuments(before, after);

        assertTrue(result.changes.isEmpty());
        assertFalse(result.comparisonComplete);
        assertEquals(1, result.warnings.size());
        assertEquals("after: 未知の Project field は比較しません: CustomField", result.warnings.get(0).message);
    }

    @Test
    public void rejectsMissingAndDuplicateMeaningfulRowIdentities() throws IOException {
        Map<String, Object> before = base();
        Map<String, Object> missing = copy(before);
        row(missing, "Tasks", 0).remove("UID");
        assertThrows(IllegalArgumentException.class, () -> diff.diffDocuments(before, missing));

        Map<String, Object> duplicate = copy(before);
        rows(duplicate, "Tasks").add(new LinkedHashMap<String, Object>(row(duplicate, "Tasks", 0)));
        assertThrows(IllegalArgumentException.class, () -> diff.diffDocuments(before, duplicate));
    }

    @Test
    public void rejectsNonScalarKnownValues() throws IOException {
        Map<String, Object> before = base();
        Map<String, Object> after = copy(before);
        row(after, "Tasks", 0).put("Name", values("nested", "value"));
        assertThrows(IllegalArgumentException.class, () -> diff.diffDocuments(before, after));
    }

    @Test
    public void treatsOnlyCompletelyBlankPlaceholderRowsAsEmpty() {
        Map<String, Object> empty = values("format", "mikuproject_workbook_json", "version", Integer.valueOf(1), "sheets", values());
        Map<String, Object> placeholders = copy(empty);
        Map<String, Object> placeholderSheets = sheets(placeholders);
        placeholderSheets.put("Project", Arrays.<Object>asList(values("Field", null, "Value", null)));
        placeholderSheets.put("Tasks", Arrays.<Object>asList(values("UID", null, "Name", "")));
        placeholderSheets.put("Resources", Arrays.<Object>asList(values("UID", null, "Name", null)));
        placeholderSheets.put("Assignments", Arrays.<Object>asList(values("UID", null)));
        placeholderSheets.put("Calendars", Arrays.<Object>asList(values("UID", null)));
        placeholderSheets.put("NonWorkingDays", Arrays.<Object>asList(values("CalendarUID", null, "Index", null)));

        ProjectWorkbookJsonDiff.Result result = diff.diffDocuments(empty, placeholders);
        assertTrue(result.changes.isEmpty());
        assertTrue(result.warnings.isEmpty());
        assertTrue(result.comparisonComplete);

        rows(placeholders, "Resources").get(0).put("Name", "A real resource");
        assertThrows(IllegalArgumentException.class, () -> diff.diffDocuments(empty, placeholders));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> base() throws IOException {
        InputStream stream = getClass().getResourceAsStream("/node-java-137/BASE.json");
        if (stream == null) throw new IOException("missing BASE.json fixture");
        try {
            byte[] bytes = readAll(stream);
            return (Map<String, Object>) json.parseJsonText(new String(bytes, StandardCharsets.UTF_8));
        } finally {
            stream.close();
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> copy(Map<String, Object> source) {
        return (Map<String, Object>) json.parseJsonText(json.stringifyJson(source));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> row(Map<String, Object> document, String sheet, int index) {
        return rows(document, sheet).get(index);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> rows(Map<String, Object> document, String sheet) {
        return (List<Map<String, Object>>) sheets(document).get(sheet);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> sheets(Map<String, Object> document) {
        return (Map<String, Object>) document.get("sheets");
    }

    private Map<String, Object> newRow(String sheet) {
        if ("Tasks".equals(sheet)) return values("UID", "new-task", "Name", "New task");
        if ("Resources".equals(sheet)) return values("UID", "new-resource", "Name", "New resource");
        if ("Assignments".equals(sheet)) return values("UID", "new-assignment", "TaskUID", "new-task");
        if ("Calendars".equals(sheet)) return values("UID", "new-calendar", "Name", "New calendar");
        return values("CalendarUID", "1", "Index", Integer.valueOf(1), "Name", "New holiday");
    }

    private Map<String, Object> values(Object... values) {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        for (int index = 0; index < values.length; index += 2) {
            result.put(String.valueOf(values[index]), values[index + 1]);
        }
        return result;
    }

    private void assertOrderChange(Map<String, Object> change, String uid, int before, int after) {
        assertEquals("Tasks", change.get("sheet"));
        assertEquals(uid, change.get("uid"));
        assertEquals("$position", change.get("field"));
        assertEquals("order-changed", change.get("change_type"));
        assertEquals(Integer.valueOf(before), change.get("before"));
        assertEquals(Integer.valueOf(after), change.get("after"));
    }

    private void assertOnlyChange(ProjectWorkbookJsonDiff.Result result, String sheet, String uid, String field,
            Object before, Object after) {
        assertEquals(1, result.changes.size());
        Map<String, Object> change = result.changes.get(0);
        assertEquals(sheet, change.get("sheet"));
        assertEquals(uid, change.get("uid"));
        assertEquals(field, change.get("field"));
        assertEquals(before, change.get("before"));
        assertEquals(after, change.get("after"));
        assertEquals("field-updated", change.get("change_type"));
    }

    private byte[] readAll(InputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int count;
        while ((count = input.read(buffer)) >= 0) {
            output.write(buffer, 0, count);
        }
        return output.toByteArray();
    }
}
