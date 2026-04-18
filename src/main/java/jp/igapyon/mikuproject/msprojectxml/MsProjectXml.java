package jp.igapyon.mikuproject.msprojectxml;

import java.util.List;

import jp.igapyon.mikuproject.model.ProjectModel;
import jp.igapyon.mikuproject.model.ValidationIssue;

public class MsProjectXml {
    private final MsProjectCodec codec = new MsProjectCodec();
    private final MsProjectCalendar calendar = new MsProjectCalendar();
    private final MsProjectValidate validate = new MsProjectValidate();

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
        return model;
    }

    public List<ValidationIssue> validateProjectModel(ProjectModel model) {
        return validate.validateProjectModel(normalizeProjectModel(model));
    }
}
