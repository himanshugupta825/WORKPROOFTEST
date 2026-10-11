package com.workproof.model;

import com.workproof.model.enums.ProjectRole;

import java.time.LocalDateTime;

/** Row of `project_members` joined with the member's user record. */
public class ProjectMember {

    private int id;
    private int projectId;
    private int userId;
    private String roleInProject = "MEMBER";
    private LocalDateTime joinedAt;

    /* joined user fields, so the JSP can render the member list without extra queries */
    private String fullName;
    private String email;
    private String globalRoleName;
    private String department;
    private String status;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getProjectId() { return projectId; }
    public void setProjectId(int projectId) { this.projectId = projectId; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getRoleInProject() { return roleInProject; }
    public void setRoleInProject(String roleInProject) { this.roleInProject = roleInProject; }

    public LocalDateTime getJoinedAt() { return joinedAt; }
    public void setJoinedAt(LocalDateTime joinedAt) { this.joinedAt = joinedAt; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getGlobalRoleName() { return globalRoleName; }
    public void setGlobalRoleName(String globalRoleName) { this.globalRoleName = globalRoleName; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public ProjectRole getProjectRole() {
        return ProjectRole.from(roleInProject);
    }

    public String getInitial() {
        return (fullName == null || fullName.isEmpty()) ? "?" : fullName.substring(0, 1).toUpperCase();
    }
}
