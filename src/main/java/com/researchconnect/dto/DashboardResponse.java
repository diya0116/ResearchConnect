package com.researchconnect.dto;

import com.researchconnect.entity.Project;
import com.researchconnect.enums.ApplicationStatus;
import java.time.Instant;
import java.util.List;

public record DashboardResponse(
        ProfileSummary profile,
        int totalProjects,
        int activeMemberProjects,
        int pendingIncomingRequests,
        int pendingOutgoingRequests,
        List<ProjectItem> myProjects,
        List<ApplicationItem> myApplications,
        List<IncomingRequestItem> incomingRequests,
        List<TeamProjectItem> teamMemberships,
        List<ProjectItem> savedProjects
) {
    public record ProfileSummary(String name, String email, String role, String department) {
    }

    public record ProjectItem(Project project, int currentMembers) {
    }

    public record ApplicationItem(
            String applicationId,
            String projectId,
            String projectTitle,
            String projectOwner,
            Instant applicationDate,
            ApplicationStatus status
    ) {
    }

    public record IncomingRequestItem(
            String applicationId,
            String projectId,
            String projectTitle,
            String applicantId,
            String applicantName,
            String applicantEmail,
            String applicantRole,
            String applicantDepartment,
            Instant applicationDate,
            ApplicationStatus status
    ) {
    }

    public record TeamProjectItem(Project project, int currentMembers, List<TeamMember> members) {
    }

    public record TeamMember(
            String name,
            String email,
            String role,
            String department,
            Instant joinedAt
    ) {
    }
}
