package com.workproof.model;

import com.workproof.model.enums.RoleName;
import com.workproof.model.enums.UserStatus;

/**
 * Row of the `users` table (never exposes the password hash to JSP pages).
 * The domain object is what the servlets and JSP pages work with.
 */
public class User {

    private int id;
    private String fullName;
    private String email;
    /** BCrypt hash - intentionally NOT exposed through a getter for JSP use. */
    private String passwordHash;
    private int roleId;
    private String roleName;
    private String status = "ACTIVE";
    private String department;
    private String designation;
    private String phone;
    private java.time.LocalDateTime lastLoginAt;
    private java.time.LocalDateTime createdAt;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public int getRoleId() { return roleId; }
    public void setRoleId(int roleId) { this.roleId = roleId; }

    public String getRoleName() { return roleName; }
    public void setRoleName(String roleName) { this.roleName = roleName; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getDesignation() { return designation; }
    public void setDesignation(String designation) { this.designation = designation; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public java.time.LocalDateTime getLastLoginAt() { return lastLoginAt; }
    public void setLastLoginAt(java.time.LocalDateTime lastLoginAt) { this.lastLoginAt = lastLoginAt; }

    public java.time.LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(java.time.LocalDateTime createdAt) { this.createdAt = createdAt; }

    /* ------------------- typed helpers used by JSP EL and Java code ------------------- */

    public RoleName getRole() {
        return RoleName.from(roleName);
    }

    public UserStatus getUserStatus() {
        return UserStatus.from(status);
    }

    public boolean isAdmin() {
        return getRole() == RoleName.ADMIN;
    }

    public boolean isManager() {
        return getRole() == RoleName.MANAGER;
    }

    public boolean isMember() {
        return getRole() == RoleName.MEMBER;
    }

    public boolean isReviewer() {
        return getRole() == RoleName.REVIEWER;
    }

    public boolean isActive() {
        return getUserStatus() == UserStatus.ACTIVE;
    }

    /** Managers and members can both own tasks inside projects. */
    public boolean canOwnTasks() {
        return isManager() || isMember();
    }

    /** First letter of the name, used for the avatar circle in the UI. */
    public String getInitial() {
        return (fullName == null || fullName.isEmpty()) ? "?" : fullName.substring(0, 1).toUpperCase();
    }

    @Override
    public String toString() {
        return "User{id=" + id + ", email=" + email + ", role=" + roleName + "}";
    }

    /* Deliberately no equals/hashCode override: identity is the database id and
       entity instances are not used as collection keys. */
}
