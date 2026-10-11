package com.workproof.dao;

import com.workproof.model.PagedResult;
import com.workproof.model.User;

import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Data access for `users`.
 * All statements use PreparedStatements with positional parameters, so user input
 * can never be interpreted as SQL.
 */
public class UserDao extends BaseDao {

    /** Column list + join to roles, reused by every query so mapping stays identical. */
    private static final String BASE_SELECT =
            "SELECT u.id, u.full_name, u.email, u.password_hash, u.role_id, u.status, u.department, "
          + "       u.designation, u.phone, u.last_login_at, u.created_at, r.name AS role_name "
          + "FROM users u JOIN roles r ON r.id = u.role_id ";

    private final RowMapper<User> mapper = (ResultSet rs) -> {
        User u = new User();
        u.setId(rs.getInt("id"));
        u.setFullName(rs.getString("full_name"));
        u.setEmail(rs.getString("email"));
        u.setPasswordHash(rs.getString("password_hash"));
        u.setRoleId(rs.getInt("role_id"));
        u.setRoleName(rs.getString("role_name"));
        u.setStatus(rs.getString("status"));
        u.setDepartment(rs.getString("department"));
        u.setDesignation(rs.getString("designation"));
        u.setPhone(rs.getString("phone"));
        u.setLastLoginAt(toLocalDateTime(rs.getTimestamp("last_login_at")));
        u.setCreatedAt(toLocalDateTime(rs.getTimestamp("created_at")));
        return u;
    };

    /* ============================== reads ============================== */

    public User findById(int id) {
        return one(BASE_SELECT + "WHERE u.id = ?", mapper, id);
    }

    public User findByEmail(String email) {
        if (email == null) {
            return null;
        }
        return one(BASE_SELECT + "WHERE u.email = ?", mapper, email.trim().toLowerCase());
    }

    public boolean existsByEmail(String email) {
        if (email == null) {
            return false;
        }
        return count("SELECT COUNT(*) FROM users WHERE email = ?", email.trim().toLowerCase()) > 0;
    }

    public List<User> findActiveByRoleName(String roleName) {
        return list(BASE_SELECT + "WHERE u.status = 'ACTIVE' AND r.name = ? ORDER BY u.full_name", mapper, roleName);
    }

    /** Active users who can own work (managers + members + reviewers), for member pickers. */
    public List<User> findAssignableUsers() {
        return list(BASE_SELECT
                + "WHERE u.status = 'ACTIVE' AND r.name IN ('MANAGER', 'MEMBER', 'REVIEWER') "
                + "ORDER BY u.full_name", mapper);
    }

    /** Active users who may be chosen as a reviewer on an evidence submission. */
    public List<User> findReviewerCandidates() {
        return list(BASE_SELECT
                + "WHERE u.status = 'ACTIVE' AND r.name IN ('REVIEWER', 'MANAGER') "
                + "ORDER BY u.full_name", mapper);
    }

    /**
     * Paged, filtered user listing for the admin screen.
     *
     * @param search   matches full name or email (case-insensitive, optional)
     * @param roleName filters on roles.name (optional)
     * @param status   filters on users.status (optional)
     */
    public PagedResult<User> search(String search, String roleName, String status, int page, int pageSize) {
        StringBuilder where = new StringBuilder(" WHERE 1 = 1 ");
        List<Object> params = new ArrayList<>();

        if (search != null && !search.trim().isEmpty()) {
            where.append(" AND (u.full_name LIKE ? OR u.email LIKE ?) ");
            String like = "%" + search.trim() + "%";
            params.add(like);
            params.add(like);
        }
        if (roleName != null && !roleName.trim().isEmpty()) {
            where.append(" AND r.name = ? ");
            params.add(roleName.trim());
        }
        if (status != null && !status.trim().isEmpty()) {
            where.append(" AND u.status = ? ");
            params.add(status.trim());
        }

        int total = count("SELECT COUNT(*) FROM users u JOIN roles r ON r.id = u.role_id " + where,
                params.toArray());
        int offset = (Math.max(page, 1) - 1) * pageSize;

        List<Object> listParams = new ArrayList<>(params);
        listParams.add(pageSize);
        listParams.add(offset);

        List<User> users = list(BASE_SELECT + where + " ORDER BY u.created_at DESC LIMIT ? OFFSET ?",
                mapper, listParams.toArray());

        return new PagedResult<>(users, total, page, pageSize);
    }

    public int countAll() {
        return count("SELECT COUNT(*) FROM users");
    }

    public int countActive() {
        return count("SELECT COUNT(*) FROM users WHERE status = 'ACTIVE'");
    }

    public int countByRoleName(String roleName) {
        return count("SELECT COUNT(*) FROM users u JOIN roles r ON r.id = u.role_id WHERE r.name = ?", roleName);
    }

    /* ============================== writes ============================== */

    /** Inserts a new account and returns its generated id. */
    public int insert(User user) {
        return insert("INSERT INTO users (full_name, email, password_hash, role_id, status, department, "
                    + "designation, phone) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                user.getFullName(),
                user.getEmail().toLowerCase(),
                user.getPasswordHash(),
                user.getRoleId(),
                user.getStatus() == null ? "ACTIVE" : user.getStatus(),
                user.getDepartment(),
                user.getDesignation(),
                user.getPhone());
    }

    public void updateLastLogin(int userId) {
        update("UPDATE users SET last_login_at = ? WHERE id = ?", LocalDateTime.now(), userId);
    }

    /** Profile update - email and role are intentionally NOT editable here (admin only). */
    public int updateProfile(int userId, String fullName, String department, String designation, String phone) {
        return update("UPDATE users SET full_name = ?, department = ?, designation = ?, phone = ? WHERE id = ?",
                fullName, department, designation, phone, userId);
    }

    public int updatePassword(int userId, String passwordHash) {
        return update("UPDATE users SET password_hash = ? WHERE id = ?", passwordHash, userId);
    }

    public int updateStatus(int userId, String status) {
        return update("UPDATE users SET status = ? WHERE id = ?", status, userId);
    }

    public int updateRole(int userId, int roleId) {
        return update("UPDATE users SET role_id = ? WHERE id = ?", roleId, userId);
    }

    /**
     * Admin-side full update. Kept in one statement so the form can save
     * name, role and status together.
     */
    public int adminUpdate(int userId, String fullName, int roleId, String status,
                           String department, String designation, String phone) {
        return update("UPDATE users SET full_name = ?, role_id = ?, status = ?, department = ?, "
                    + "designation = ?, phone = ? WHERE id = ?",
                fullName, roleId, status, department, designation, phone, userId);
    }

    /** Used by the "count active admins" safety check before deactivating an admin. */
    public int countActiveAdminsExcluding(int userId) {
        return count("SELECT COUNT(*) FROM users u JOIN roles r ON r.id = u.role_id "
                   + "WHERE r.name = 'ADMIN' AND u.status = 'ACTIVE' AND u.id <> ?", userId);
    }

    public List<User> findAllBasic() {
        return list(BASE_SELECT + "ORDER BY u.full_name", mapper);
    }

    /** Small helper used when a report needs a name without loading the full list. */
    public String findFullNameById(int userId) {
        User user = findById(userId);
        return user == null ? "Unknown user" : user.getFullName();
    }
}
