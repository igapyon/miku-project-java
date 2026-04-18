package jp.igapyon.mikuproject.model;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class ProjectModelTest {
    @Test
    public void modelListsAreInitialized() {
        ProjectModel model = new ProjectModel();

        assertNotNull(model.project);
        assertNotNull(model.tasks);
        assertNotNull(model.resources);
        assertNotNull(model.assignments);
        assertNotNull(model.calendars);
        assertTrue(model.tasks.isEmpty());
        assertTrue(model.resources.isEmpty());
        assertTrue(model.assignments.isEmpty());
        assertTrue(model.calendars.isEmpty());
    }
}
