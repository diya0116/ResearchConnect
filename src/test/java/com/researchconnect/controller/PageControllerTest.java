package com.researchconnect.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.researchconnect.config.FirebaseProperties;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.junit.jupiter.api.Test;

class PageControllerTest {

    @Test
    void dashboardPageLoadsFirebaseConfiguration() {
        FirebaseProperties properties = new FirebaseProperties("web-key", "auth-domain", "project-id", "app-id", "");
        PageController controller = new PageController(properties);
        Model model = new ExtendedModelMap();

        assertEquals("dashboard", controller.dashboard(model));
        assertEquals("web-key", model.getAttribute("firebaseApiKey"));
        assertEquals("project-id", model.getAttribute("firebaseProjectId"));
    }

    @Test
    void projectsAliasLoadsFirebaseConfiguration() {
        FirebaseProperties properties = new FirebaseProperties("web-key", "auth-domain", "project-id", "app-id", "");
        PageController controller = new PageController(properties);
        Model model = new ExtendedModelMap();

        assertEquals("app", controller.projects(model));
        assertEquals("web-key", model.getAttribute("firebaseApiKey"));
        assertEquals("project-id", model.getAttribute("firebaseProjectId"));
    }
}