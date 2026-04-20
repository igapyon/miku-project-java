/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuproject.msprojectxml;

import jp.igapyon.mikuproject.model.AssignmentModel;
import jp.igapyon.mikuproject.model.ProjectInfo;
import jp.igapyon.mikuproject.model.ProjectModel;
import jp.igapyon.mikuproject.model.ResourceModel;
import jp.igapyon.mikuproject.model.TaskModel;

public class MsProjectSamples {
    public static final int DEFAULT_PROJECT_MINUTES_PER_DAY = 480;
    public static final int DEFAULT_PROJECT_MINUTES_PER_WEEK = 2400;
    public static final int DEFAULT_PROJECT_DAYS_PER_MONTH = 20;

    public ProjectModel buildSampleProjectModel() {
        ProjectModel model = new ProjectModel();
        model.project = buildProjectInfo();

        model.tasks.add(buildTask("draft-100", "1", "基盤整備", 1, "1", true, false, 100, "2026-03-16T09:00:00",
                "2026-03-17T18:00:00"));
        model.tasks.add(buildTask("draft-110", "2", "着手", 2, "1.1", false, true, 100, "2026-03-16T09:00:00",
                "2026-03-16T09:00:00"));
        model.tasks.add(buildTask("draft-120", "3",
                "初期実装（MS Project XML 調査・基軸フォーマット選定・内部モデルの概要確定）", 2, "1.2", false, false, 100,
                "2026-03-16T09:00:00", "2026-03-16T18:00:00"));
        model.tasks.add(buildTask("draft-130", "4", "round-trip拡張（MS Project XML → 内部JSON形式 → MS Project XML の往復対応）",
                2, "1.3", false, false, 100, "2026-03-17T09:00:00", "2026-03-17T18:00:00"));
        model.tasks.add(buildTask("draft-150", "5", "架空検討フェーズ【架空】", 1, "2", true, false, 25, "2026-03-19T09:00:00",
                "2026-03-25T18:00:00"));
        model.tasks.add(buildTask("draft-160", "6", "ユーザー操作フローの見直し【架空】", 2, "2.1", false, false, 50,
                "2026-03-19T09:00:00", "2026-03-23T18:00:00"));
        model.tasks.add(buildTask("draft-170", "7", "画面構成の再整理【架空】", 2, "2.2", false, false, null,
                "2026-03-24T09:00:00", "2026-03-25T18:00:00"));
        model.tasks.add(buildTask("draft-200", "8", "XLSX / UI 強化", 1, "3", true, false, null, "2026-03-27T09:00:00",
                "2026-03-28T18:00:00"));
        model.tasks.add(buildTask("draft-210", "9", "GitHub リポジトリ独立化", 2, "3.1", false, true, null,
                "2026-03-27T09:00:00", "2026-03-27T09:00:00"));
        model.tasks.add(buildTask("draft-220", "10", "MS Project XML と XLSX の相互変換・round-trip実装", 2, "3.2", false,
                false, null, "2026-03-27T09:00:00", "2026-03-27T18:00:00"));
        model.tasks.add(buildTask("draft-230", "11", "XLSXレイアウト再設計・再整理", 2, "3.3", false, false, null,
                "2026-03-28T09:00:00", "2026-03-28T18:00:00"));
        model.tasks.add(buildTask("draft-300", "12", "リリース", 1, "4", true, false, null, "2026-03-29T09:00:00",
                "2026-03-29T18:00:00"));
        model.tasks.add(buildTask("draft-310", "13", "v1.0 リリース", 2, "4.1", false, true, null, "2026-03-29T09:00:00",
                "2026-03-29T09:00:00"));

        model.resources.add(buildResource());
        model.assignments.add(buildAssignment("asg-1", "draft-120", "res-1", "2026-03-16T09:00:00", "2026-03-16T18:00:00",
                100));
        model.assignments.add(buildAssignment("asg-2", "draft-130", "res-1", "2026-03-17T09:00:00", "2026-03-17T18:00:00",
                100));

        return new MsProjectXml().normalizeProjectModel(model);
    }

    public String buildSampleXml() {
        return buildSampleXml(new MsProjectXml());
    }

    public String buildSampleXml(MsProjectXml msProjectXml) {
        return msProjectXml.exportToXml(buildSampleProjectModel());
    }

    private ProjectInfo buildProjectInfo() {
        ProjectInfo project = new ProjectInfo();
        project.name = "mikuproject開発";
        project.startDate = "2026-03-16T09:00:00";
        project.finishDate = "2026-04-01T18:00:00";
        project.scheduleFromStart = true;
        project.minutesPerDay = Integer.valueOf(DEFAULT_PROJECT_MINUTES_PER_DAY);
        project.minutesPerWeek = Integer.valueOf(DEFAULT_PROJECT_MINUTES_PER_WEEK);
        project.daysPerMonth = Integer.valueOf(DEFAULT_PROJECT_DAYS_PER_MONTH);
        project.defaultStartTime = "09:00:00";
        project.defaultFinishTime = "18:00:00";
        project.currentDate = "2026-03-23T09:00:00";
        project.statusDate = "2026-03-23T09:00:00";
        return project;
    }

    private TaskModel buildTask(String uid, String id, String name, int outlineLevel, String outlineNumber, boolean summary,
            boolean milestone, Integer percentComplete, String start, String finish) {
        TaskModel task = new TaskModel();
        task.uid = uid;
        task.id = id;
        task.name = name;
        task.outlineLevel = Integer.valueOf(outlineLevel);
        task.outlineNumber = outlineNumber;
        task.wbs = outlineNumber;
        task.summary = summary;
        task.milestone = milestone;
        task.percentComplete = percentComplete;
        task.start = start;
        task.finish = finish;
        task.calendarUID = "1";
        if (milestone) {
            task.duration = "PT0H0M0S";
        } else if (!summary) {
            task.duration = "PT8H0M0S";
        }
        return task;
    }

    private ResourceModel buildResource() {
        ResourceModel resource = new ResourceModel();
        resource.uid = "res-1";
        resource.id = "1";
        resource.name = "Mikuku";
        resource.initials = "M";
        resource.group = "Development";
        resource.maxUnits = Double.valueOf(1.0d);
        resource.calendarUID = "1";
        return resource;
    }

    private AssignmentModel buildAssignment(String uid, String taskUid, String resourceUid, String start, String finish,
            int percentWorkComplete) {
        AssignmentModel assignment = new AssignmentModel();
        assignment.uid = uid;
        assignment.taskUid = taskUid;
        assignment.resourceUid = resourceUid;
        assignment.start = start;
        assignment.finish = finish;
        assignment.startVariance = "PT0H0M0S";
        assignment.finishVariance = "PT0H0M0S";
        assignment.units = Double.valueOf(1.0d);
        assignment.work = "PT8H0M0S";
        assignment.overtimeWork = "PT0H0M0S";
        assignment.actualOvertimeWork = "PT0H0M0S";
        assignment.percentWorkComplete = Integer.valueOf(percentWorkComplete);
        return assignment;
    }
}
