import { authenticatedFetch } from "./firebase-session.js";

const form = document.querySelector("#project-form");
const errorElement = document.querySelector("#project-error");
const editing = window.location.pathname.endsWith("/edit");
const projectId = editing ? window.location.pathname.split("/")[2] : null;

async function loadProjectForEdit() {
    if (!editing) return;
    try {
        const response = await authenticatedFetch(`/api/projects/${encodeURIComponent(projectId)}`);
        if (!response.ok) throw new Error("Project details could not be loaded.");
        const project = await response.json();
        for (const [name, value] of Object.entries(project)) {
            const field = form.elements[name];
            if (field && name !== "requiredSkills") field.value = value ?? "";
        }
        form.elements.requiredSkills.value = project.requiredSkills.join(", ");
        document.querySelector("#form-title").textContent = "Edit project";
        document.querySelector("#submit-project").textContent = "Save changes";
    } catch (error) {
        errorElement.textContent = error.message;
    }
}

form.addEventListener("submit", async (event) => {
    event.preventDefault();
    errorElement.textContent = "";

    if (!form.reportValidity()) {
        errorElement.textContent = "Please complete all required fields.";
        return;
    }

    const data = new FormData(form);
    const teamSize = Number(data.get("teamSize"));
    const availablePositions = Number(data.get("availablePositions"));
    if (availablePositions > teamSize) {
        errorElement.textContent = "Available positions cannot be greater than team size.";
        return;
    }

    const skills = String(data.get("requiredSkills"))
        .split(",")
        .map(skill => skill.trim())
        .filter(Boolean);
    if (skills.length === 0) {
        errorElement.textContent = "Enter at least one required skill.";
        return;
    }

    const submitButton = form.querySelector('button[type="submit"]');
    submitButton.disabled = true;
    try {
        const response = await authenticatedFetch(
            editing ? `/api/projects/${encodeURIComponent(projectId)}` : "/api/projects",
            {
            method: editing ? "PUT" : "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({
                title: data.get("title"),
                shortDescription: data.get("shortDescription"),
                description: data.get("description"),
                category: data.get("category"),
                department: data.get("department"),
                requiredSkills: skills,
                teamSize,
                availablePositions,
                duration: data.get("duration"),
                weeklyCommitment: Number(data.get("weeklyCommitment")),
                expectedStartDate: data.get("expectedStartDate"),
                objectives: data.get("objectives"),
                requirements: data.get("requirements")
            })
            }
        );
        if (!response.ok) {
            const payload = await response.json().catch(() => null);
            throw new Error(payload?.detail || payload?.message || "Project could not be created.");
        }
        const project = await response.json();
        window.location.href = `/projects/${encodeURIComponent(project.projectId)}`;
    } catch (error) {
        errorElement.textContent = error.message;
        submitButton.disabled = false;
    }
});

loadProjectForEdit();
