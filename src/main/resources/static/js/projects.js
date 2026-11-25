import { authenticatedFetch } from "./firebase-session.js";

const feed = document.querySelector("#project-feed");
const errorElement = document.querySelector("#feed-error");
const search = document.querySelector("#search");
const department = document.querySelector("#department-filter");
const category = document.querySelector("#category-filter");
const projectType = document.querySelector("#type-filter");

async function loadProfile() {
    try {
        const response = await authenticatedFetch("/api/auth/me");
        if (!response.ok) return;
        const account = await response.json();
        if (account.profileComplete) {
            document.querySelector("#profile-link").textContent =
                `${account.profile.fullName || "Profile"} · ${account.profile.role}`;
        }
    } catch (_) {
        // The profile label remains a neutral fallback.
    }
}

loadProfile();

function escapeHtml(value) {
    const element = document.createElement("div");
    element.textContent = value == null ? "" : String(value);
    return element.innerHTML;
}

async function loadProjects() {
    errorElement.textContent = "";
    feed.innerHTML = '<p class="muted">Loading projects...</p>';
    const parameters = new URLSearchParams();
    if (search.value) parameters.set("search", search.value);
    if (department.value) parameters.set("department", department.value);
    if (category.value) parameters.set("category", category.value);
    if (projectType.value) parameters.set("projectType", projectType.value);

    try {
        const response = await authenticatedFetch(`/api/projects?${parameters}`);
        if (!response.ok) {
            const payload = await response.json().catch(() => null);
            throw new Error(payload?.message || `Projects could not be loaded (HTTP ${response.status}).`);
        }
        const projects = await response.json();
        if (projects.length === 0) {
            feed.innerHTML = `
                <section class="empty-state">
                    <p class="eyebrow">NO OPPORTUNITIES FOUND</p>
                    <h2>No projects match this view</h2>
                    <p class="muted">Try removing a filter or publish a new project.</p>
                    <a class="button button-dark button-inline" href="/create-project">Create project</a>
                </section>`;
            return;
        }
        feed.innerHTML = projects.map(project => `
            <article class="project-row">
                <div class="project-main">
                    <p class="project-meta">${escapeHtml(project.category)} / ${escapeHtml(project.department)}</p>
                    <h2>${escapeHtml(project.title)}</h2>
                    <p class="project-description">${escapeHtml(project.shortDescription)}</p>
                    <p class="project-owner">Published by ${escapeHtml(project.ownerName)} · ${escapeHtml(project.ownerRole)}</p>
                </div>
                <div class="project-side">
                    <p class="project-meta">REQUIRED SKILLS</p>
                    <p>${escapeHtml(project.requiredSkills.join(", "))}</p>
                    <p class="position-count">${project.availablePositions} position${project.availablePositions === 1 ? "" : "s"} available</p>
                    <a class="text-action" href="/projects/${encodeURIComponent(project.projectId)}">View Details</a>
                </div>
            </article>`).join("");
    } catch (error) {
        errorElement.textContent = error.message;
        feed.innerHTML = "";
    }
}

let debounce;
search.addEventListener("input", () => {
    clearTimeout(debounce);
    debounce = setTimeout(loadProjects, 250);
});
department.addEventListener("change", loadProjects);
category.addEventListener("change", loadProjects);
projectType.addEventListener("change", loadProjects);

loadProjects();
