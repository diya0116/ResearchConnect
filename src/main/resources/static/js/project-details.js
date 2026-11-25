import { authenticatedFetch } from "./firebase-session.js";

const article = document.querySelector("#project-detail");
const errorElement = document.querySelector("#detail-error");
const projectId = decodeURIComponent(window.location.pathname.split("/").pop());

function escapeHtml(value) {
    const element = document.createElement("div");
    element.textContent = value == null ? "" : String(value);
    return element.innerHTML;
}

async function loadProject() {
    try {
        const response = await authenticatedFetch(`/api/projects/${encodeURIComponent(projectId)}`);
        if (response.status === 404) {
            throw new Error("This project could not be found.");
        }
        if (!response.ok) throw new Error("Project details could not be loaded.");
        const project = await response.json();
        const accountResponse = await authenticatedFetch("/api/auth/me");
        const account = accountResponse.ok ? await accountResponse.json() : null;
        article.innerHTML = `
            <header class="detail-header">
                <p class="project-meta">${escapeHtml(project.category)} / ${escapeHtml(project.department)}</p>
                <h1>${escapeHtml(project.title)}</h1>
                <p class="detail-owner">Published by ${escapeHtml(project.ownerName)} · ${escapeHtml(project.ownerRole)}</p>
            </header>
            <aside class="detail-facts">
                <div><span>Team size</span><strong>${project.teamSize}</strong></div>
                <div><span>Available positions</span><strong>${project.availablePositions}</strong></div>
                <div><span>Duration</span><strong>${escapeHtml(project.duration)}</strong></div>
                <div><span>Weekly commitment</span><strong>${project.weeklyCommitment} hours</strong></div>
                <div><span>Expected start</span><strong>${escapeHtml(project.expectedStartDate)}</strong></div>
            </aside>
            <div class="detail-content">
                <section>
                    <p class="eyebrow">DESCRIPTION</p>
                    <p class="detail-copy">${escapeHtml(project.description).replace(/\n/g, "<br>")}</p>
                </section>
                <section>
                    <p class="eyebrow">REQUIRED SKILLS</p>
                    <p>${escapeHtml(project.requiredSkills.join(", "))}</p>
                </section>
                <section>
                    <p class="eyebrow">OBJECTIVES</p>
                    <p class="detail-copy">${escapeHtml(project.objectives || "No objectives provided.")}</p>
                </section>
                <section>
                    <p class="eyebrow">REQUIREMENTS / ELIGIBILITY</p>
                    <p class="detail-copy">${escapeHtml(project.requirements || "No additional requirements provided.")}</p>
                </section>
            </div>`;
        if (account?.profile?.uid === project.ownerId) {
            const management = document.querySelector("#project-management");
            management.hidden = false;
            management.innerHTML = `
                <a class="button button-secondary" href="/projects/${encodeURIComponent(project.projectId)}/edit">Edit project</a>
                <button class="button button-dark" id="delete-project" type="button">Delete project</button>`;
            document.querySelector("#delete-project").addEventListener("click", async () => {
                if (!window.confirm("Delete this project permanently?")) return;
                try {
                    const deleteResponse = await authenticatedFetch(`/api/projects/${encodeURIComponent(project.projectId)}`, {
                        method: "DELETE"
                    });
                    if (!deleteResponse.ok) {
                        const payload = await deleteResponse.json().catch(() => null);
                        throw new Error(payload?.message || "Project could not be deleted.");
                    }
                    window.location.href = "/my-projects";
                } catch (error) {
                    errorElement.textContent = error.message;
                }
            });
        }
    } catch (error) {
        errorElement.textContent = error.message;
    }
}

loadProject();
