/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuproject.msprojectxml;

import java.util.List;

import jp.igapyon.mikuproject.model.ProjectModel;
import jp.igapyon.mikuproject.model.ValidationIssue;
import jp.igapyon.mikuproject.wbsmarkdown.WbsMarkdown;
import jp.igapyon.mikuproject.wbsmarkdown.WbsMarkdown.WbsMarkdownOptions;
import jp.igapyon.mikuproject.wbssvg.WbsSvg;
import jp.igapyon.mikuproject.wbssvg.WbsSvg.MonthlyCalendarSvgArchive;
import jp.igapyon.mikuproject.wbssvg.WbsSvg.NativeSvgOptions;

public class MsProjectXml {
    private final MsProjectCodec codec = new MsProjectCodec();
    private final MsProjectCalendar calendar = new MsProjectCalendar();
    private final MsProjectValidate validate = new MsProjectValidate();
    private final MsProjectMermaid mermaid = new MsProjectMermaid();
    private final MsProjectAiViews aiViews = new MsProjectAiViews();
    private final WbsMarkdown wbsMarkdown = new WbsMarkdown();
    private final WbsSvg wbsSvg = new WbsSvg();

    public ProjectModel importFromXml(String xmlText) {
        ProjectModel model = codec.importMsProjectXml(xmlText);
        model = calendar.ensureDefaultProjectCalendar(model);
        model = normalizeProjectModel(model);
        return model;
    }

    public String exportToXml(ProjectModel model) {
        ProjectModel normalizedModel = calendar.ensureDefaultProjectCalendar(normalizeProjectModel(model));
        return codec.exportMsProjectXml(normalizedModel);
    }

    public ProjectModel normalizeProjectModel(ProjectModel model) {
        if (model == null) {
            return new ProjectModel();
        }
        if (model.project == null) {
            model.project = new jp.igapyon.mikuproject.model.ProjectInfo();
        }
        if (model.project.outlineCodes == null) {
            model.project.outlineCodes = new java.util.ArrayList<jp.igapyon.mikuproject.model.OutlineCodeModel>();
        }
        if (model.project.wbsMasks == null) {
            model.project.wbsMasks = new java.util.ArrayList<jp.igapyon.mikuproject.model.WbsMaskModel>();
        }
        if (model.project.extendedAttributes == null) {
            model.project.extendedAttributes =
                    new java.util.ArrayList<jp.igapyon.mikuproject.model.ProjectExtendedAttributeModel>();
        }
        if (model.tasks == null) {
            model.tasks = new java.util.ArrayList<jp.igapyon.mikuproject.model.TaskModel>();
        }
        if (model.resources == null) {
            model.resources = new java.util.ArrayList<jp.igapyon.mikuproject.model.ResourceModel>();
        }
        if (model.assignments == null) {
            model.assignments = new java.util.ArrayList<jp.igapyon.mikuproject.model.AssignmentModel>();
        }
        if (model.calendars == null) {
            model.calendars = new java.util.ArrayList<jp.igapyon.mikuproject.model.CalendarModel>();
        }
        for (jp.igapyon.mikuproject.model.TaskModel task : model.tasks) {
            if (task == null) {
                continue;
            }
            if (task.extendedAttributes == null) {
                task.extendedAttributes =
                        new java.util.ArrayList<jp.igapyon.mikuproject.model.TaskExtendedAttributeModel>();
            }
            if (task.baselines == null) {
                task.baselines = new java.util.ArrayList<jp.igapyon.mikuproject.model.TaskBaselineModel>();
            }
            if (task.timephasedData == null) {
                task.timephasedData = new java.util.ArrayList<jp.igapyon.mikuproject.model.TaskTimephasedDataModel>();
            }
            if (task.predecessors == null) {
                task.predecessors = new java.util.ArrayList<jp.igapyon.mikuproject.model.PredecessorModel>();
            }
        }
        for (jp.igapyon.mikuproject.model.ResourceModel resource : model.resources) {
            if (resource == null) {
                continue;
            }
            if (resource.extendedAttributes == null) {
                resource.extendedAttributes =
                        new java.util.ArrayList<jp.igapyon.mikuproject.model.ResourceExtendedAttributeModel>();
            }
            if (resource.baselines == null) {
                resource.baselines = new java.util.ArrayList<jp.igapyon.mikuproject.model.ResourceBaselineModel>();
            }
            if (resource.timephasedData == null) {
                resource.timephasedData =
                        new java.util.ArrayList<jp.igapyon.mikuproject.model.ResourceTimephasedDataModel>();
            }
        }
        for (jp.igapyon.mikuproject.model.AssignmentModel assignment : model.assignments) {
            if (assignment == null) {
                continue;
            }
            if (assignment.extendedAttributes == null) {
                assignment.extendedAttributes =
                        new java.util.ArrayList<jp.igapyon.mikuproject.model.AssignmentExtendedAttributeModel>();
            }
            if (assignment.baselines == null) {
                assignment.baselines =
                        new java.util.ArrayList<jp.igapyon.mikuproject.model.AssignmentBaselineModel>();
            }
            if (assignment.timephasedData == null) {
                assignment.timephasedData =
                        new java.util.ArrayList<jp.igapyon.mikuproject.model.AssignmentTimephasedDataModel>();
            }
        }
        for (jp.igapyon.mikuproject.model.CalendarModel calendarModel : model.calendars) {
            if (calendarModel == null) {
                continue;
            }
            if (calendarModel.weekDays == null) {
                calendarModel.weekDays = new java.util.ArrayList<jp.igapyon.mikuproject.model.WeekDayModel>();
            }
            if (calendarModel.exceptions == null) {
                calendarModel.exceptions =
                        new java.util.ArrayList<jp.igapyon.mikuproject.model.CalendarExceptionModel>();
            }
            if (calendarModel.workWeeks == null) {
                calendarModel.workWeeks = new java.util.ArrayList<jp.igapyon.mikuproject.model.WorkWeekModel>();
            }
        }
        return model;
    }

    public List<ValidationIssue> validateProjectModel(ProjectModel model) {
        return validate.validateProjectModel(normalizeProjectModel(model));
    }

    public String exportMermaidGantt(ProjectModel model) {
        return mermaid.exportMermaidGantt(normalizeProjectModel(model));
    }

    public String exportWbsMarkdown(ProjectModel model) {
        return wbsMarkdown.exportWbsMarkdown(normalizeProjectModel(model));
    }

    public String exportWbsMarkdown(ProjectModel model, WbsMarkdownOptions options) {
        return wbsMarkdown.exportWbsMarkdown(normalizeProjectModel(model), options);
    }

    public String exportNativeSvg(ProjectModel model) {
        return wbsSvg.exportNativeSvg(normalizeProjectModel(model));
    }

    public String exportNativeSvg(ProjectModel model, NativeSvgOptions options) {
        return wbsSvg.exportNativeSvg(normalizeProjectModel(model), options);
    }

    public String exportWeeklyNativeSvg(ProjectModel model) {
        return wbsSvg.exportWeeklyNativeSvg(normalizeProjectModel(model));
    }

    public String exportWeeklyNativeSvg(ProjectModel model, NativeSvgOptions options) {
        return wbsSvg.exportWeeklyNativeSvg(normalizeProjectModel(model), options);
    }

    public MonthlyCalendarSvgArchive exportMonthlyWbsCalendarSvgArchive(ProjectModel model) {
        return wbsSvg.exportMonthlyWbsCalendarSvgArchive(normalizeProjectModel(model));
    }

    public java.util.Map<String, Object> buildProjectDraftRequest(String name, String plannedStart, String goal, Integer teamCount,
            java.util.List<String> mustHavePhases, java.util.List<String> mustHaveMilestones) {
        return aiViews.buildProjectDraftRequest(name, plannedStart, goal, teamCount, mustHavePhases, mustHaveMilestones);
    }

    public ProjectModel importProjectDraftView(Object draft) {
        return aiViews.importProjectDraftView(draft);
    }

    public java.util.Map<String, Object> exportProjectOverviewView(ProjectModel model) {
        return aiViews.exportProjectOverviewView(normalizeProjectModel(model));
    }

    public java.util.Map<String, Object> exportPhaseDetailView(ProjectModel model) {
        return aiViews.exportPhaseDetailView(normalizeProjectModel(model));
    }

    public java.util.Map<String, Object> exportPhaseDetailView(ProjectModel model, String requestedPhaseUid, String mode,
            String rootUid, Integer maxDepth) {
        return aiViews.exportPhaseDetailView(normalizeProjectModel(model), requestedPhaseUid, mode, rootUid, maxDepth);
    }

    public java.util.Map<String, Object> exportTaskEditView(ProjectModel model, String requestedTaskUid) {
        return aiViews.exportTaskEditView(normalizeProjectModel(model), requestedTaskUid);
    }
}
