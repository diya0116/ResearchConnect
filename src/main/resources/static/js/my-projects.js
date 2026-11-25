import { authenticatedFetch } from "./firebase-session.js";

const list = document.querySelector("#my-projects-list");
const errorElement = document.querySelector("#my-projects-error");

function escapeHtml(value) {
    const element = document.createElement("div");
    element.textContent = value == null ? "" : String(value);
    return element.innerHTML;
}

function dateLabel(value) {
    const date = value ? new Date(value) : null;
    return date && !Number.isNaN(date.getTime()) ? date.toLocaleDateString() : "Date unavailable";
}

async function loadOwnedProjects() {
    try {
        const response = await authenticatedFetch("/api/projects/mine");
        if (!response.ok) {
            const payload = await response.json().catch(() => null);
            throw new Error(payload?.message || `Projects could not be loaded (HTTP ${response.status}).`);
        }
        const projects = await response.json();
        if (!projects.length) {
            list.innerHTML = `
                <section class="empty-state">
                    <p class="eyebrow">NO PROJECTS YET</p>
                    <h2>Your published work will appear here</h2>
                    <p class="muted">Create a project to begin building a research team.</p>
                    <a class="button button-dark button-inline" href="/create-project">Create project</a>
                </section>`;
            return;
        }
        const memberCounts = await Promise.all(projects.map(async project => {
            const response = await authenticatedFetch(
                `/api/projects/${encodeURIComponent(project.projectId)}/member-count`);
            if (!response.ok) {
                const payload = await response.json().catch(() => null);
                throw new Error(payload?.message || `Team details could not be loaded (HTTP ${response.status}).`);
            }
            return (await response.json()).count;
        }));
        list.innerHTML = projects.map((project, index) => `
            <article class="project-row">
                <div class="project-main">
                    <p class="project-meta">${escapeHtml(project.category)} / ${escapeHtml(project.department)}</p>
                    <h2>${escapeHtml(project.title)}</h2>
                    <p class="project-description">${escapeHtml(project.shortDescription)}</p>
                    <p class="project-owner">Published ${dateLabel(project.createdAt)}</p>
                </div>
                <div class="project-side">
                    <p class="project-meta">STATUS</p>
                    <p>${escapeHtml(project.status)}</p>
                    <p>${project.availablePositions} position${project.availablePositions === 1 ? "" : "s"} available</p>
                    <p class="muted">Accepted members: ${memberCounts[index]}</p>
                    <a class="text-action" href="/projects/${encodeURIComponent(project.projectId)}">Manage project</a>
                </div>
            </article>`).join("");
    } catch (error) {
        errorElement.textContent = error.message;
        list.innerHTML = "";
    }
}

loadOwnedProjects();
