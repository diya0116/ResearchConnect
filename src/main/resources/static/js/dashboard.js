import { authenticatedFetch } from "./firebase-session.js";

const errorElement = document.querySelector("#dashboard-error");
const projectsElement = document.querySelector("#dashboard-projects");
const applicationsElement = document.querySelector("#dashboard-applications");
const incomingElement = document.querySelector("#dashboard-incoming");
const notificationsElement = document.querySelector("#dashboard-notifications");
const teamsElement = document.querySelector("#dashboard-teams");
const savedElement = document.querySelector("#dashboard-saved");

function escapeHtml(value) {
    const element = document.createElement("div");
    element.textContent = value == null ? "" : String(value);
    return element.innerHTML;
}

function dateLabel(value) {
    const date = value ? new Date(value) : null;
    return date && !Number.isNaN(date.getTime()) ? date.toLocaleDateString() : "Date unavailable";
}

function dateTimeLabel(value) {
    const date = value ? new Date(value) : null;
    return date && !Number.isNaN(date.getTime()) ? date.toLocaleString() : "Date unavailable";
}

function emptyState(message) {
    return `<p class="muted dashboard-empty">${escapeHtml(message)}</p>`;
}

function projectRow(item, actions) {
    const project = item.project;
    const projectId = encodeURIComponent(project.projectId);
    return `
        <article class="project-row">
            <div class="project-main">
                <p class="project-meta">${escapeHtml(project.category)} / ${escapeHtml(project.department)}</p>
                <h3>${escapeHtml(project.title)}</h3>
                <p class="project-description">${escapeHtml(project.shortDescription)}</p>
                <p class="muted">Team ${item.currentMembers} / ${project.teamSize} ·
                    ${project.availablePositions} position${project.availablePositions === 1 ? "" : "s"} available</p>
            </div>
            <div class="project-side">
                <p class="project-meta">STATUS</p>
                <p>${escapeHtml(project.status)}</p>
                <div class="dashboard-actions">${actions(project, projectId)}</div>
            </div>
        </article>`;
}

function renderProjectList(element, projects, emptyMessage, actions) {
    element.innerHTML = projects.length
        ? projects.map(item => projectRow(item, actions)).join("")
        : emptyState(emptyMessage);
}

function errorMessage(payload, fallback, status) {
    return payload?.message || payload?.detail || `${fallback} (HTTP ${status}).`;
}

async function loadDashboard() {
    errorElement.textContent = "";
    try {
        const response = await authenticatedFetch("/api/dashboard");
        if (!response.ok) {
            const payload = await response.json().catch(() => null);
            throw new Error(errorMessage(payload, "Dashboard could not be loaded", response.status));
        }
        const dashboard = await response.json();

        document.querySelector("#dashboard-profile-heading").textContent =
            dashboard.profile.name || "ResearchConnect member";
        document.querySelector("#profile-email").textContent = dashboard.profile.email || "—";
        document.querySelector("#profile-role").textContent = dashboard.profile.role || "—";
        document.querySelector("#profile-department").textContent = dashboard.profile.department || "—";
        document.querySelector("#profile-link").textContent =
            `${dashboard.profile.name || "Profile"} · ${dashboard.profile.role || "Member"}`;
        document.querySelector("#total-projects").textContent = dashboard.totalProjects;
        document.querySelector("#active-memberships").textContent = dashboard.activeMemberProjects;
        document.querySelector("#incoming-count").textContent = dashboard.pendingIncomingRequests;
        document.querySelector("#outgoing-count").textContent = dashboard.pendingOutgoingRequests;

        renderProjectList(
            projectsElement,
            dashboard.myProjects,
            "You have not published any projects yet.",
            (project, projectId) => `
                <a class="text-action" href="/projects/${projectId}">View</a>
                <a class="text-action" href="/projects/${projectId}/edit">Edit</a>
                <button class="text-action dashboard-action-button" type="button"
                    data-action="delete-project" data-project-id="${projectId}">Delete</button>`
        );

        applicationsElement.innerHTML = dashboard.myApplications.length
            ? dashboard.myApplications.map(application => `
                <article class="dashboard-row">
                    <div>
                        <p class="project-meta">${escapeHtml(application.status)} · Applied ${dateLabel(application.applicationDate)}</p>
                        <h3>${escapeHtml(application.projectTitle)}</h3>
                        <p class="muted">Project owner: ${escapeHtml(application.projectOwner)}</p>
                    </div>
                    <a class="text-action" href="/projects/${encodeURIComponent(application.projectId)}">View project</a>
                </article>`).join("")
            : emptyState("You have not submitted any join requests.");

        incomingElement.innerHTML = dashboard.incomingRequests.length
            ? dashboard.incomingRequests.map(request => `
                <article class="dashboard-row">
                    <div>
                        <p class="project-meta">${escapeHtml(request.status)} · Received ${dateLabel(request.applicationDate)}</p>
                        <h3>${escapeHtml(request.projectTitle)}</h3>
                        <p>${escapeHtml(request.applicantName)} · ${escapeHtml(request.applicantRole)}</p>
                        <p class="muted">${escapeHtml(request.applicantEmail)} · ${escapeHtml(request.applicantDepartment)}</p>
                    </div>
                    <div class="dashboard-actions">
                        <a class="text-action" href="/projects/${encodeURIComponent(request.projectId)}">View project</a>
                        ${request.status === "PENDING" ? `
                            <button class="button button-secondary" type="button" data-action="decision"
                                data-application-id="${encodeURIComponent(request.applicationId)}"
                                data-status="REJECTED">Reject</button>
                            <button class="button button-dark dashboard-button" type="button" data-action="decision"
                                data-application-id="${encodeURIComponent(request.applicationId)}"
                                data-status="ACCEPTED">Accept</button>` : ""}
                    </div>
                </article>`).join("")
            : emptyState("There are no incoming join requests.");

        teamsElement.innerHTML = dashboard.teamMemberships.length
            ? dashboard.teamMemberships.map(team => `
                <article class="dashboard-row team-row">
                    <div>
                        <p class="project-meta">${escapeHtml(team.project.status)} ·
                            ${escapeHtml(team.project.category)} / ${escapeHtml(team.project.department)}</p>
                        <h3>${escapeHtml(team.project.title)}</h3>
                        <p class="muted">Owner: ${escapeHtml(team.project.ownerName)} ·
                            Team ${team.currentMembers} / ${team.project.teamSize}</p>
                        <p class="team-members">${team.members.map(member =>
        `${escapeHtml(member.name)} (${escapeHtml(member.role)})`).join(" · ")}</p>
                    </div>
                    <a class="text-action" href="/projects/${encodeURIComponent(team.project.projectId)}">View project</a>
                </article>`).join("")
            : emptyState("You are not currently an accepted member of another project.");

        renderProjectList(
            savedElement,
            dashboard.savedProjects,
            "You have not saved any projects.",
            (project, projectId) => `
                <a class="text-action" href="/projects/${projectId}">View</a>
                <button class="text-action dashboard-action-button" type="button"
                    data-action="remove-saved" data-project-id="${projectId}">Remove</button>`
        );
        await loadNotifications();
    } catch (error) {
        errorElement.textContent = error.message || "Dashboard could not be loaded.";
        [projectsElement, applicationsElement, incomingElement, teamsElement, savedElement, notificationsElement]
            .forEach(element => { element.innerHTML = ""; });
    }
}

async function loadNotifications() {
    try {
        const response = await authenticatedFetch("/api/notifications");
        if (!response.ok) {
            const payload = await response.json().catch(() => null);
            throw new Error(errorMessage(payload, "Notifications could not be loaded", response.status));
        }
        const notifications = await response.json();
        notificationsElement.innerHTML = notifications.length
            ? notifications.map(notification => `
                <article class="dashboard-row notification-row${notification.read ? "" : " notification-unread"}">
                    <div>
                        <p class="project-meta">${notification.read ? "READ" : "UNREAD"} · ${dateTimeLabel(notification.createdAt)}</p>
                        <p>${escapeHtml(notification.message)}</p>
                    </div>
                    <div class="dashboard-actions">
                        <a class="text-action" href="/projects/${encodeURIComponent(notification.projectId)}">View project</a>
                        ${notification.read ? "" : `<button class="text-action dashboard-action-button" type="button"
                            data-action="mark-notification-read"
                            data-notification-id="${encodeURIComponent(notification.notificationId)}">Mark as read</button>`}
                    </div>
                </article>`).join("")
            : emptyState("You do not have any notifications.");
    } catch (error) {
        notificationsElement.innerHTML = `<p class="form-error">${escapeHtml(error.message)}</p>`;
    }
}

document.querySelector(".dashboard-page").addEventListener("click", async event => {
    const button = event.target.closest("button[data-action]");
    if (!button) return;
    const action = button.dataset.action;
    const projectId = button.dataset.projectId;
    const applicationId = button.dataset.applicationId;
    if (action === "delete-project" && !window.confirm("Delete this project permanently?")) return;

    button.disabled = true;
    errorElement.textContent = "";
    try {
        let response;
        if (action === "decision") {
            response = await authenticatedFetch(
                `/api/applications/${applicationId}/decision`,
                {
                    method: "PATCH",
                    headers: { "Content-Type": "application/json" },
                    body: JSON.stringify({ status: button.dataset.status })
                }
            );
        } else if (action === "remove-saved") {
            response = await authenticatedFetch(`/api/projects/${projectId}/saved`, { method: "DELETE" });
        } else if (action === "mark-notification-read") {
            response = await authenticatedFetch(
                `/api/notifications/${button.dataset.notificationId}/read`,
                { method: "PATCH" }
            );
        } else {
            response = await authenticatedFetch(`/api/projects/${projectId}`, { method: "DELETE" });
        }
        if (!response.ok) {
            const payload = await response.json().catch(() => null);
            throw new Error(errorMessage(payload, "The requested change could not be saved", response.status));
        }
        await loadDashboard();
    } catch (error) {
        errorElement.textContent = error.message;
        button.disabled = false;
    }
});

loadDashboard();
