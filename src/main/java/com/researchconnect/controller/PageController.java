package com.researchconnect.controller;

import com.researchconnect.config.FirebaseProperties;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    private final FirebaseProperties firebaseProperties;

    public PageController(FirebaseProperties firebaseProperties) {
        this.firebaseProperties = firebaseProperties;
    }

    @GetMapping({"/", "/login"})
    public String login(Model model) {
        model.addAttribute("firebaseApiKey", firebaseProperties.webApiKey());
        model.addAttribute("firebaseAuthDomain", firebaseProperties.webAuthDomain());
        model.addAttribute("firebaseProjectId", firebaseProperties.webProjectId());
        model.addAttribute("firebaseAppId", firebaseProperties.webAppId());
        return "login";
    }

    @GetMapping("/onboarding")
    public String onboarding(Model model) {
        model.addAttribute("firebaseApiKey", firebaseProperties.webApiKey());
        model.addAttribute("firebaseAuthDomain", firebaseProperties.webAuthDomain());
        model.addAttribute("firebaseProjectId", firebaseProperties.webProjectId());
        model.addAttribute("firebaseAppId", firebaseProperties.webAppId());
        return "onboarding";
    }

    @GetMapping("/app")
    public String app(Model model) {
        addFirebaseConfig(model);
        return "app";
    }

    @GetMapping("/create-project")
    public String createProject(Model model) {
        addFirebaseConfig(model);
        return "create-project";
    }

    @GetMapping("/projects/{projectId}/edit")
    public String editProject(Model model) {
        addFirebaseConfig(model);
        return "create-project";
    }

    @GetMapping("/my-projects")
    public String myProjects(Model model) {
        addFirebaseConfig(model);
        return "my-projects";
    }

    @GetMapping("/projects")
    public String projects() {
        return "app";
    }

    @GetMapping("/projects/{projectId}")
    public String projectDetails(Model model) {
        addFirebaseConfig(model);
        return "project-details";
    }

    private void addFirebaseConfig(Model model) {
        model.addAttribute("firebaseApiKey", firebaseProperties.webApiKey());
        model.addAttribute("firebaseAuthDomain", firebaseProperties.webAuthDomain());
        model.addAttribute("firebaseProjectId", firebaseProperties.webProjectId());
        model.addAttribute("firebaseAppId", firebaseProperties.webAppId());
    }
}
