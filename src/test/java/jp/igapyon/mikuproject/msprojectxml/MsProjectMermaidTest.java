/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuproject.msprojectxml;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

import jp.igapyon.mikuproject.model.PredecessorModel;
import jp.igapyon.mikuproject.model.ProjectModel;

public class MsProjectMermaidTest {
    @Test
    public void exportsMermaidFromSampleProject() {
        MsProjectXml xml = new MsProjectXml();
        ProjectModel model = new MsProjectSamples().buildSampleProjectModel();

        String mermaid = xml.exportMermaidGantt(model);

        assertTrue(mermaid.contains("gantt"));
        assertTrue(mermaid.contains("title miku-project開発"));
        assertTrue(mermaid.contains("section 基盤整備"));
    }

    @Test
    public void keepsComplexMermaidDependenciesAsComments() throws IOException {
        MsProjectXml xml = new MsProjectXml();
        ProjectModel model = xml.importFromXml(readVendorTestdata("hierarchy.xml"));
        model.tasks.get(1).predecessors.clear();
        PredecessorModel first = new PredecessorModel();
        first.predecessorUid = "1";
        first.type = Integer.valueOf(1);
        PredecessorModel second = new PredecessorModel();
        second.predecessorUid = "3";
        second.type = Integer.valueOf(2);
        second.linkLag = "PT4H0M0S";
        model.tasks.get(1).predecessors.add(first);
        model.tasks.get(1).predecessors.add(second);

        String mermaid = xml.exportMermaidGantt(model);

        assertTrue(mermaid.contains("%% dependency:"));
        assertTrue(mermaid.contains("type=FF"));
        assertTrue(mermaid.contains("lag=4h"));
    }

    @Test
    public void sanitizesDateLeadingMermaidGanttLabels() throws IOException {
        MsProjectXml xml = new MsProjectXml();
        ProjectModel model = xml.importFromXml(readVendorTestdata("hierarchy.xml"));
        model.tasks.get(0).name = "2026-03-16 初期実装（42513dd：XML import/export）";

        String mermaid = xml.exportMermaidGantt(model);

        assertTrue(mermaid.contains("title Hierarchy Project"));
        assertTrue(mermaid.contains("section Section 2026-03-16 初期実装（42513dd XML import/export）"));
    }

    private String readVendorTestdata(String fileName) throws IOException {
        byte[] bytes = Files.readAllBytes(Paths.get("vendor", "miku-project", "testdata", fileName));
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
