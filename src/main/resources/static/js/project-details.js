import { authenticatedFetch } from "./firebase-session.js";

const article = document.querySelector("#project-detail");
const errorElement = document.querySelector("#detail-error");
const commentsElement = document.querySelector("#project-comments");
const commentsErrorElement = document.querySelector("#comments-error");
const commentForm = document.querySelector("#comment-form");
const commentText = document.querySelector("#comment-text");
let currentUserId = "";
let currentProjectOwnerId = "";
const projectId = decodeURIComponent(window.location.pathname.split("/").pop());

function escapeHtml(value) {
    const element = document.createElement("div");
    element.textContent = value == null ? "" : String(value);
    return element.innerHTML;
}

function dateLabel(value) {
    const date = value ? new Date(value) : null;
    return date && !Number.isNaN(date.getTime()) ? date.toLocaleString() : "Date unavailable";
}

function renderComments(comments) {
    commentsElement.innerHTML = comments.length
        ? comments.map(comment => {
            const canDelete = currentUserId === comment.authorId || currentUserId === currentProjectOwnerId;
            return `
                <article class="comment-row">
                    <div class="comment-heading">
                        <div>
                            <strong>${escapeHtml(comment.authorName)}</strong>
                            <span class="muted">${escapeHtml(comment.authorRole)} · ${dateLabel(comment.postedAt)}</span>
                        </div>
                        ${canDelete ? `<button class="text-action comment-delete" type="button"
                            data-comment-id="${encodeURIComponent(comment.commentId)}">Delete</button>` : ""}
                    </div>
                    <p class="comment-text">${escapeHtml(comment.text).replace(/\n/g, "<br>")}</p>
                </article>`;
        }).join("")
        : '<p class="muted dashboard-empty">No comments yet. Start the discussion.</p>';
}

async function loadComments() {
    commentsErrorElement.textContent = "";
    commentsElement.innerHTML = '<p class="muted">Loading comments...</p>';
    try {
        const response = await authenticatedFetch(`/api/projects/${encodeURIComponent(projectId)}/comments`);
        if (!response.ok) throw new Error("Project comments could not be loaded.");
        renderComments(await response.json());
    } catch (error) {
        commentsElement.innerHTML = "";
        commentsErrorElement.textContent = error.message || "Project comments could not be loaded.";
    }
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
        if (!accountResponse.ok) throw new Error("Your account details could not be loaded.");
        const account = await accountResponse.json();
        const isOwner = account?.profile?.uid === project.ownerId;
        currentUserId = account?.profile?.uid || "";
        currentProjectOwnerId = project.ownerId || "";
        commentForm.hidden = !account?.profileComplete;
        document.querySelector("#comment-profile-message").hidden = !!account?.profileComplete;
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
        await loadComments();
        if (isOwner) {
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

        const collaboration = document.querySelector("#project-collaboration");
        const [savedResponse, applicationResponse] = await Promise.all([
            authenticatedFetch(`/api/projects/${encodeURIComponent(project.projectId)}/saved`),
            authenticatedFetch(`/api/projects/${encodeURIComponent(project.projectId)}/applications/mine`)
        ]);
        if (!savedResponse.ok || (applicationResponse.status !== 204 && !applicationResponse.ok)) {
            throw new Error("Project collaboration details could not be loaded.");
        }
        const saved = (await savedResponse.json()).saved;
        const application = applicationResponse.status === 204 ? null : await applicationResponse.json();
        const saveButton = document.createElement("button");
        saveButton.className = "button button-secondary";
        saveButton.type = "button";
        saveButton.textContent = saved ? "Remove saved project" : "Save project";
        saveButton.addEventListener("click", async () => {
            saveButton.disabled = true;
            try {
                const nextSaved = saveButton.textContent === "Save project";
                const saveResult = await authenticatedFetch(
                    `/api/projects/${encodeURIComponent(project.projectId)}/saved`,
                    { method: nextSaved ? "PUT" : "DELETE" }
                );
                if (!saveResult.ok) throw new Error("Saved project could not be updated.");
                saveButton.textContent = nextSaved ? "Remove saved project" : "Save project";
            } catch (error) {
                errorElement.textContent = error.message;
            } finally {
                saveButton.disabled = false;
            }
        });
        collaboration.append(saveButton);

        if (!isOwner) {
            const applyButton = document.createElement("button");
            applyButton.className = "button button-dark";
            applyButton.type = "button";
            if (application?.status === "PENDING" || application?.status === "ACCEPTED") {
                applyButton.textContent = application.status === "PENDING" ? "Request pending" : "Accepted";
                applyButton.disabled = true;
            } else {
                applyButton.textContent = application?.status === "REJECTED" ? "Apply again" : "Request to join";
                applyButton.disabled = project.status !== "PUBLISHED" || project.availablePositions < 1;
                applyButton.addEventListener("click", async () => {
                    applyButton.disabled = true;
                    try {
                        const applyResponse = await authenticatedFetch(
                            `/api/projects/${encodeURIComponent(project.projectId)}/applications`,
                            { method: "POST" }
                        );
                        if (!applyResponse.ok) {
                            const payload = await applyResponse.json().catch(() => null);
                            throw new Error(payload?.message || "Join request could not be submitted.");
                        }
                        applyButton.textContent = "Request pending";
                    } catch (error) {
                        errorElement.textContent = error.message;
                        applyButton.disabled = false;
                    }
                });
            }
            collaboration.append(applyButton);
        }
    } catch (error) {
        article.innerHTML = "";
        errorElement.textContent = error.message;
    }
}

commentForm.addEventListener("submit", async event => {
    event.preventDefault();
    commentsErrorElement.textContent = "";
    const submitButton = document.querySelector("#comment-submit");
    submitButton.disabled = true;
    try {
        const response = await authenticatedFetch(`/api/projects/${encodeURIComponent(projectId)}/comments`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ text: commentText.value })
        });
        if (!response.ok) {
            const payload = await response.json().catch(() => null);
            throw new Error(payload?.message || payload?.detail || "Comment could not be posted.");
        }
        commentText.value = "";
        await loadComments();
    } catch (error) {
        commentsErrorElement.textContent = error.message || "Comment could not be posted.";
    } finally {
        submitButton.disabled = false;
    }
});

commentsElement.addEventListener("click", async event => {
    const button = event.target.closest("button[data-comment-id]");
    if (!button || !window.confirm("Delete this comment?")) return;
    button.disabled = true;
    commentsErrorElement.textContent = "";
    try {
        const response = await authenticatedFetch(
            `/api/projects/${encodeURIComponent(projectId)}/comments/${button.dataset.commentId}`,
            { method: "DELETE" }
        );
        if (!response.ok) throw new Error("Comment could not be deleted.");
        await loadComments();
    } catch (error) {
        commentsErrorElement.textContent = error.message || "Comment could not be deleted.";
        button.disabled = false;
    }
});

loadProject();
