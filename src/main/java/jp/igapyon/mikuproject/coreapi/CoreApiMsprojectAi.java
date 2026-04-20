/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuproject.coreapi;

import java.util.List;
import java.util.Map;

import jp.igapyon.mikuproject.model.ProjectModel;
import jp.igapyon.mikuproject.msprojectxml.MsProjectXml;

public class CoreApiMsprojectAi {
    private final MsProjectXml msProjectXml = new MsProjectXml();

    public final AiViewsApi aiViews = new AiViewsApi();
    public final MermaidApi mermaid = new MermaidApi();

    public class AiViewsApi {
        public Map<String, Object> buildProjectDraftRequest(String name, String plannedStart, String goal, Integer teamCount,
                List<String> mustHavePhases, List<String> mustHaveMilestones) {
            return msProjectXml.buildProjectDraftRequest(name, plannedStart, goal, teamCount, mustHavePhases, mustHaveMilestones);
        }

        public ProjectModel importProjectDraftView(Object draft) {
            return msProjectXml.importProjectDraftView(draft);
        }

        public Map<String, Object> exportProjectOverviewView(ProjectModel model) {
            return msProjectXml.exportProjectOverviewView(model);
        }

        public Map<String, Object> exportTaskEditView(ProjectModel model, String requestedTaskUid) {
            return msProjectXml.exportTaskEditView(model, requestedTaskUid);
        }

        public Map<String, Object> exportPhaseDetailView(ProjectModel model) {
            return msProjectXml.exportPhaseDetailView(model);
        }

        public Map<String, Object> exportPhaseDetailView(ProjectModel model, String requestedPhaseUid, String mode,
                String rootUid, Integer maxDepth) {
            return msProjectXml.exportPhaseDetailView(model, requestedPhaseUid, mode, rootUid, maxDepth);
        }
    }

    public class MermaidApi {
        public String exportGantt(ProjectModel model) {
            return msProjectXml.exportMermaidGantt(model);
        }
    }
}
