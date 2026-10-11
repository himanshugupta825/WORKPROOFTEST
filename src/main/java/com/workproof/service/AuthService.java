package com.workproof.service;

import com.workproof.dao.RoleDao;
import com.workproof.dao.UserDao;
import com.workproof.exception.AuthenticationException;
import com.workproof.exception.ValidationException;
import com.workproof.model.User;
import com.workproof.model.enums.NotificationType;
import com.workproof.model.enums.RoleName;
import com.workproof.util.PasswordUtil;
import com.workproof.util.ValidationUtil;

/**
 * Authentication and account self-service:
 * registration, login, logout bookkeeping and password change.
 *
 * Security behaviour implemented here:
 *   - BCrypt hashing (never a plain password in the database or the log),
 *   - duplicate e-mail prevention,
 *   - generic error message on failed login (does not reveal whether the e-mail exists),
 *   - inactive accounts are refused,
 *   - the last login timestamp and an audit entry are written on success.
 */
public class AuthService {

    private final UserDao userDao = new UserDao();
    private final RoleDao roleDao = new RoleDao();
    private final ActivityService activityService = new ActivityService();
    private final NotificationService notificationService = new NotificationService();

    /* ============================= registration ============================= */

    /**
     * Registers a new user.
     *
     * @param roleName one of MANAGER, MEMBER, REVIEWER (admins are created by an admin)
     * @return the persisted user (without exposing the password hash to the caller's view)
     */
    public User register(String fullName, String email, String password, String confirmPassword,
                         String roleName, String department, String designation, String phone, String ip) {

        // ---- server-side validation of every field ----
        fullName = ValidationUtil.require(fullName, "Full name", 3, 120);
        email = ValidationUtil.requireEmail(email);
        password = ValidationUtil.requirePassword(password, "Password");
        ValidationUtil.requireMatch(password, confirmPassword, "The two passwords do not match.");
        department = ValidationUtil.optional(department, 100, "Department");
        designation = ValidationUtil.optional(designation, 100, "Designation");
        phone = ValidationUtil.optionalPhone(phone);

        RoleName role = RoleName.from(roleName);
        if (!Permissions.isSelfRegisterableRole(role)) {
            throw new ValidationException("Please choose one of the available roles: "
                    + "Project Manager, Team Member or Reviewer.");
        }

        // ---- duplicate e-mail check (the UNIQUE index is the final guard) ----
        if (userDao.existsByEmail(email)) {
            throw new ValidationException("An account with this email already exists. "
                    + "Please sign in or use a different email address.");
        }

        User user = new User();
        user.setFullName(fullName);
        user.setEmail(email);
        user.setPasswordHash(PasswordUtil.hash(password));
        user.setRoleId(roleDao.requireIdByName(role.name()));
        user.setRoleName(role.name());
        user.setStatus("ACTIVE");
        user.setDepartment(department);
        user.setDesignation(designation);
        user.setPhone(phone);

        int userId = userDao.insert(user);
        user.setId(userId);

        activityService.log(user, ActivityService.AUTH_REGISTER, "USER", userId,
                "Registered a new " + role.getLabel() + " account (" + email + ")", ip);
        notificationService.send(userId, "Welcome to WorkProof",
                "Your account is ready. Explore your dashboard to see the projects and tasks "
                        + "assigned to you.", NotificationType.SYSTEM, "/dashboard", "welcome:" + userId);

        return user;
    }

    /* ================================ login ================================ */

    /**
     * Verifies credentials and returns the user on success.
     *
     * @throws AuthenticationException with a message that is safe to display
     */
    public User login(String email, String password, String ip) {
        String cleanEmail = ValidationUtil.trimToNull(email);
        if (cleanEmail == null || password == null || password.isEmpty()) {
            throw new AuthenticationException("Please enter your email and password.");
        }

        User user = userDao.findByEmail(cleanEmail);
        boolean passwordOk = user != null && PasswordUtil.matches(password, user.getPasswordHash());

        if (user == null || !passwordOk) {
            // Generic message: never reveal whether the address exists.
            activityService.logForUserId(user == null ? null : user.getId(), ActivityService.AUTH_LOGIN_FAILED,
                    "USER", user == null ? null : user.getId(),
                    "Failed sign-in attempt for " + cleanEmail, ip);
            throw new AuthenticationException("Incorrect email or password.");
        }
        if (!user.isActive()) {
            throw new AuthenticationException("This account has been deactivated. "
                    + "Please contact your administrator.");
        }

        userDao.updateLastLogin(user.getId());
        activityService.log(user, ActivityService.AUTH_LOGIN, "USER", user.getId(),
                user.getFullName() + " signed in", ip);
        return user;
    }

    public void recordLogout(User user, String ip) {
        if (user != null) {
            activityService.log(user, ActivityService.AUTH_LOGOUT, "USER", user.getId(),
                    user.getFullName() + " signed out", ip);
        }
    }

    /* =========================== account settings =========================== */

    /** Changes the password of the logged-in user after verifying the current one. */
    public void changePassword(User user, String currentPassword, String newPassword, String confirmPassword, String ip) {
        User stored = userDao.findById(user.getId());
        if (stored == null) {
            throw new AuthenticationException("Your account no longer exists.");
        }
        if (!PasswordUtil.matches(currentPassword, stored.getPasswordHash())) {
            throw new ValidationException("Your current password is not correct.");
        }
        newPassword = ValidationUtil.requirePassword(newPassword, "New password");
        ValidationUtil.requireMatch(newPassword, confirmPassword, "The two new passwords do not match.");
        if (PasswordUtil.matches(newPassword, stored.getPasswordHash())) {
            throw new ValidationException("The new password must be different from the current password.");
        }

        userDao.updatePassword(user.getId(), PasswordUtil.hash(newPassword));
        activityService.log(user, ActivityService.PASSWORD_CHANGED, "USER", user.getId(),
                user.getFullName() + " changed the account password", ip);
    }

    public void updateProfile(User user, String fullName, String department, String designation,
                              String phone, String ip) {
        fullName = ValidationUtil.require(fullName, "Full name", 3, 120);
        department = ValidationUtil.optional(department, 100, "Department");
        designation = ValidationUtil.optional(designation, 100, "Designation");
        phone = ValidationUtil.optionalPhone(phone);

        userDao.updateProfile(user.getId(), fullName, department, designation, phone);

        // Keep the session copy in sync so the header shows the new name immediately.
        user.setFullName(fullName);
        user.setDepartment(department);
        user.setDesignation(designation);
        user.setPhone(phone);

        activityService.log(user, ActivityService.PROFILE_UPDATED, "USER", user.getId(),
                user.getFullName() + " updated their profile", ip);
    }

    public User findById(int userId) {
        return userDao.findById(userId);
    }
}
