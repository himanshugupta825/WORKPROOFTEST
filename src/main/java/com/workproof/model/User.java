package com.workproof.model;

import com.workproof.model.enums.RoleName;
import com.workproof.model.enums.UserStatus;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Row of the `users` table and JPA Entity.
 * The domain object is used across Spring Security, Spring Data JPA, and web layers.
 */
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    /** BCrypt hash - intentionally NOT exposed through a getter for JSP/view use. */
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "role_id")
    private int roleId = 1;

    @Column(name = "role_name", nullable = false)
    private String roleName = "TECHNICIAN";

    @Column(name = "status", nullable = false)
    private String status = "ACTIVE";

    @Column(name = "department")
    private String department;

    @Column(name = "designation")
    private String designation;

    @Column(name = "phone")
    private String phone;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    public User() {}

    public User(String fullName, String email, String passwordHash, RoleName role) {
        this.fullName = fullName;
        this.email = email;
        this.passwordHash = passwordHash;
        this.roleName = (role != null) ? role.name() : RoleName.TECHNICIAN.name();
        this.status = "ACTIVE";
        this.createdAt = LocalDateTime.now();
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
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

    public LocalDateTime getLastLoginAt() { return lastLoginAt; }
    public void setLastLoginAt(LocalDateTime lastLoginAt) { this.lastLoginAt = lastLoginAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    /* ------------------- typed helpers used by templates and Java code ------------------- */

    public RoleName getRole() {
        return RoleName.from(roleName);
    }

    public void setRole(RoleName role) {
        if (role != null) {
            this.roleName = role.name();
        }
    }

    public UserStatus getUserStatus() {
        return UserStatus.from(status);
    }

    public void setUserStatus(UserStatus userStatus) {
        if (userStatus != null) {
            this.status = userStatus.name();
        }
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

    public boolean isTechnician() {
        return getRole() == RoleName.TECHNICIAN || getRole() == RoleName.MEMBER;
    }

    public boolean isCustomer() {
        return getRole() == RoleName.CUSTOMER;
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
}
