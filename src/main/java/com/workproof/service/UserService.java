package com.workproof.service;

import com.workproof.dao.RoleDao;
import com.workproof.dao.UserDao;
import com.workproof.exception.NotFoundException;
import com.workproof.exception.ValidationException;
import com.workproof.model.PagedResult;
import com.workproof.model.Role;
import com.workproof.model.User;
import com.workproof.model.enums.NotificationType;
import com.workproof.model.enums.RoleName;
import com.workproof.model.enums.UserStatus;
import com.workproof.util.PasswordUtil;
import com.workproof.util.ValidationUtil;

import java.util.List;

/**
 * Administrator user management (Module 1 & the admin screens).
 *
 * Safety rules implemented:
 *   - an administrator cannot deactivate or downgrade their own account,
 *   - the last active administrator cannot be deactivated (otherwise nobody could
 *     administer the system any more),
 *   - duplicate e-mails are refused,
 *   - passwords are always stored as BCrypt hashes.
 */
public class UserService {

    private final UserDao userDao = new UserDao();
    private final RoleDao roleDao = new RoleDao();
    private final ActivityService activityService = new ActivityService();
    private final NotificationService notificationService = new NotificationService();

    public List<Role> allRoles() {
        return roleDao.findAll();
    }

    public PagedResult<User> search(String search, String roleName, String status, int page, int pageSize) {
        return userDao.search(search, roleName, status, page, pageSize);
    }

    public User findById(int userId) {
        return userDao.findById(userId);
    }

    public List<User> findAll() {
        return userDao.findAllBasic();
    }

    public List<User> findReviewerCandidates() {
        return userDao.findReviewerCandidates();
    }

    public List<User> findAssignableUsers() {
        return userDao.findAssignableUsers();
    }

    public int countAll() {
        return userDao.countAll();
    }

    public int countActive() {
        return userDao.countActive();
    }

    public int countByRole(RoleName roleName) {
        return userDao.countByRoleName(roleName.name());
    }

    /** Creates an account from the admin screen (any role, including ADMIN). */
    public int create(User actor, String fullName, String email, String password, String confirmPassword,
                      String roleName, String department, String designation, String phone, String ip) {
        Permissions.requireAdmin(actor);

        fullName = ValidationUtil.require(fullName, "Full name", 3, 120);
        email = ValidationUtil.requireEmail(email);
        password = ValidationUtil.requirePassword(password, "Password");
        ValidationUtil.requireMatch(password, confirmPassword, "The two passwords do not match.");
        RoleName role = RoleName.from(roleName);

        if (userDao.existsByEmail(email)) {
            throw new ValidationException("An account with this email already exists.");
        }

        User user = new User();
        user.setFullName(fullName);
        user.setEmail(email);
        user.setPasswordHash(PasswordUtil.hash(password));
        user.setRoleId(roleDao.requireIdByName(role.name()));
        user.setRoleName(role.name());
        user.setStatus(UserStatus.ACTIVE.name());
        user.setDepartment(ValidationUtil.optional(department, 100, "Department"));
        user.setDesignation(ValidationUtil.optional(designation, 100, "Designation"));
        user.setPhone(ValidationUtil.optionalPhone(phone));

        int newUserId = userDao.insert(user);

        activityService.log(actor, ActivityService.USER_CREATED, "USER", newUserId,
                "Created account " + email + " with role " + role.getLabel(), ip);
        notificationService.send(newUserId, "Welcome to WorkProof",
                "An administrator created your account. You can sign in and change your password "
                        + "from your profile page.", NotificationType.SYSTEM, "/dashboard", "welcome:" + newUserId);
        return newUserId;
    }

    /** Full update from the admin form (name, role, status and contact details). */
    public void update(User actor, int userId, String fullName, String roleName, String status,
                       String department, String designation, String phone, String ip) {
        Permissions.requireAdmin(actor);

        User target = userDao.findById(userId);
        if (target == null) {
            throw new NotFoundException("User #" + userId + " was not found.");
        }

        fullName = ValidationUtil.require(fullName, "Full name", 3, 120);
        RoleName role = RoleName.from(roleName);
        UserStatus userStatus = UserStatus.from(status);

        if (target.getId() == actor.getId()) {
            if (!target.isAdmin()) {
                throw new ValidationException("You cannot remove the administrator role from your own account.");
            }
            if (userStatus == UserStatus.INACTIVE) {
                throw new ValidationException("You cannot deactivate your own account.");
            }
        }
        if (target.isAdmin() && role != RoleName.ADMIN && userDao.countActiveAdminsExcluding(userId) == 0) {
            throw new ValidationException("The last active administrator cannot be changed to another role.");
        }
        if (target.isAdmin() && userStatus == UserStatus.INACTIVE
                && userDao.countActiveAdminsExcluding(userId) == 0) {
            throw new ValidationException("The last active administrator cannot be deactivated.");
        }

        int roleId = roleDao.requireIdByName(role.name());
        userDao.adminUpdate(userId, fullName, roleId, userStatus.name(),
                ValidationUtil.optional(department, 100, "Department"),
                ValidationUtil.optional(designation, 100, "Designation"),
                ValidationUtil.optionalPhone(phone));

        if (!role.name().equalsIgnoreCase(target.getRoleName())) {
            activityService.log(actor, ActivityService.USER_ROLE_CHANGED, "USER", userId,
                    "Changed role of " + target.getEmail() + " from " + target.getRoleName() + " to " + role.name(), ip);
            notificationService.send(userId, "Your role changed",
                    "An administrator set your role to " + role.getLabel() + ".", NotificationType.SYSTEM,
                    "/dashboard", null);
        }
        activityService.log(actor, ActivityService.USER_UPDATED, "USER", userId,
                "Updated account " + target.getEmail(), ip);
    }

    /** Activates or deactivates an account. Deactivated users cannot sign in. */
    public void changeStatus(User actor, int userId, String newStatus, String ip) {
        Permissions.requireAdmin(actor);

        User target = userDao.findById(userId);
        if (target == null) {
            throw new NotFoundException("User #" + userId + " was not found.");
        }
        if (target.getId() == actor.getId()) {
            throw new ValidationException("You cannot change the status of your own account.");
        }
        UserStatus status = UserStatus.from(newStatus);
        if (target.isAdmin() && status == UserStatus.INACTIVE && userDao.countActiveAdminsExcluding(userId) == 0) {
            throw new ValidationException("The last active administrator cannot be deactivated.");
        }

        userDao.updateStatus(userId, status.name());
        activityService.log(actor, ActivityService.USER_STATUS_CHANGED, "USER", userId,
                (status == UserStatus.ACTIVE ? "Activated " : "Deactivated ") + target.getEmail(), ip);
        if (status == UserStatus.ACTIVE) {
            notificationService.send(userId, "Account reactivated",
                    "Your account is active again. Welcome back!", NotificationType.SYSTEM, "/dashboard", null);
        }
    }

    /** Admin password reset - the new password is never written to the log. */
    public void resetPassword(User actor, int userId, String newPassword, String confirmPassword, String ip) {
        Permissions.requireAdmin(actor);

        User target = userDao.findById(userId);
        if (target == null) {
            throw new NotFoundException("User #" + userId + " was not found.");
        }
        newPassword = ValidationUtil.requirePassword(newPassword, "New password");
        ValidationUtil.requireMatch(newPassword, confirmPassword, "The two passwords do not match.");

        userDao.updatePassword(userId, PasswordUtil.hash(newPassword));
        activityService.log(actor, ActivityService.USER_PASSWORD_RESET, "USER", userId,
                "Reset the password of " + target.getEmail(), ip);
        notificationService.send(userId, "Your password was reset",
                "An administrator reset the password of your account. Please sign in and set a new "
                        + "password from your profile page.", NotificationType.SYSTEM, "/profile", null);
    }
}
