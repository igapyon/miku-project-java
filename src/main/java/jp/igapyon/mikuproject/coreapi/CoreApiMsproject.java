/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuproject.coreapi;

import java.util.List;

import jp.igapyon.mikuproject.model.ProjectModel;
import jp.igapyon.mikuproject.model.ValidationIssue;
import jp.igapyon.mikuproject.msprojectxml.MsProjectCsv;
import jp.igapyon.mikuproject.msprojectxml.MsProjectSamples;
import jp.igapyon.mikuproject.msprojectxml.MsProjectXml;

public class CoreApiMsproject {
    private final MsProjectXml msProjectXml = new MsProjectXml();
    private final MsProjectSamples samplesImpl = new MsProjectSamples();
    private final MsProjectCsv csv = new MsProjectCsv();
    private final CoreApiMsprojectAi msprojectAi = new CoreApiMsprojectAi();

    public final SamplesApi samples = new SamplesApi();
    public final ProjectModelApi projectModel = new ProjectModelApi();
    public final MsProjectApi msProject = new MsProjectApi();
    public final CoreApiMsprojectAi.AiViewsApi aiViews = msprojectAi.aiViews;
    public final CoreApiMsprojectAi.MermaidApi mermaid = msprojectAi.mermaid;

    public class SamplesApi {
        public String getSampleXml() {
            return samplesImpl.buildSampleXml(msProjectXml);
        }

        public ProjectModel getSampleProjectDraftView() {
            return samplesImpl.buildSampleProjectModel();
        }
    }

    public class ProjectModelApi {
        public ProjectModel normalize(ProjectModel model) {
            return msProjectXml.normalizeProjectModel(model);
        }

        public List<ValidationIssue> validate(ProjectModel model) {
            return msProjectXml.validateProjectModel(model);
        }
    }

    public class MsProjectApi {
        public ProjectModel importFromXml(String sourceText) {
            return msProjectXml.importFromXml(sourceText);
        }

        public String exportToXml(ProjectModel model) {
            return msProjectXml.exportToXml(model);
        }

        public String exportToCsvParentId(ProjectModel model) {
            return csv.exportCsvParentId(model);
        }

        public ProjectModel importFromCsvParentId(String sourceText) {
            return csv.importCsvParentId(sourceText);
        }
    }
}
