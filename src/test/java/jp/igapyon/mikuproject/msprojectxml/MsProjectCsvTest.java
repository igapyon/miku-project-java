/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuproject.msprojectxml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

import jp.igapyon.mikuproject.model.ProjectModel;

public class MsProjectCsvTest {
    @Test
    public void roundTripsHierarchyThroughCsvParentId() throws IOException {
        MsProjectXml xml = new MsProjectXml();
        MsProjectCsv csv = new MsProjectCsv();
        ProjectModel model = xml.importFromXml(readVendorTestdata("hierarchy.xml"));

        String csvText = csv.exportCsvParentId(model);
        ProjectModel importedModel = csv.importCsvParentId(csvText);

        assertTrue(csvText.contains("ID,ParentID,WBS,Name"));
        assertTrue(csvText.contains("Child A"));
        assertTrue(csvText.contains("Child B"));
        assertEquals("CSV Imported Project", importedModel.project.name);
        assertEquals("CSV Imported Project", importedModel.project.title);
        assertEquals(3, importedModel.tasks.size());
        assertEquals("Summary", importedModel.tasks.get(0).name);
        assertEquals("Child A", importedModel.tasks.get(1).name);
        assertEquals("Child B", importedModel.tasks.get(2).name);
        assertEquals("1", importedModel.tasks.get(0).outlineNumber);
        assertEquals("1.1", importedModel.tasks.get(1).outlineNumber);
        assertEquals("1.2", importedModel.tasks.get(2).outlineNumber);
        assertEquals("Second child task", importedModel.tasks.get(2).notes);
    }

    @Test
    public void rejectsDuplicateIdInCsvImport() {
        MsProjectCsv csv = new MsProjectCsv();
        String csvText = "ID,ParentID,Name\n1,,Root\n1,,Duplicate\n";

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                new org.junit.jupiter.api.function.Executable() {
                    @Override
                    public void execute() {
                        csv.importCsvParentId(csvText);
                    }
                });

        assertTrue(exception.getMessage().contains("CSV の ID が重複しています"));
    }

    @Test
    public void rejectsMissingParentIdInCsvImport() {
        MsProjectCsv csv = new MsProjectCsv();
        String csvText = "ID,ParentID,Name\n1,,Root\n2,99,Child\n";

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                new org.junit.jupiter.api.function.Executable() {
                    @Override
                    public void execute() {
                        csv.importCsvParentId(csvText);
                    }
                });

        assertTrue(exception.getMessage().contains("CSV の ParentID が既存 ID を指していません"));
    }

    @Test
    public void rejectsCyclicParentIdInCsvImport() {
        MsProjectCsv csv = new MsProjectCsv();
        String csvText = "ID,ParentID,Name\n1,2,Root\n2,1,Child\n";

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                new org.junit.jupiter.api.function.Executable() {
                    @Override
                    public void execute() {
                        csv.importCsvParentId(csvText);
                    }
                });

        assertTrue(exception.getMessage().contains("CSV の ParentID が循環しています"));
    }

    private String readVendorTestdata(String fileName) throws IOException {
        byte[] bytes = Files.readAllBytes(Paths.get("vendor", "mikuproject", "testdata", fileName));
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
