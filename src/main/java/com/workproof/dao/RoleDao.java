package com.workproof.dao;

import com.workproof.model.Role;

import java.sql.ResultSet;
import java.util.List;

/** Data access for the `roles` reference table. */
public class RoleDao extends BaseDao {

    private static final String SELECT_COLUMNS = "SELECT id, name, description FROM roles ";

    private final RowMapper<Role> mapper = (ResultSet rs) -> new Role(
            rs.getInt("id"),
            rs.getString("name"),
            rs.getString("description"));

    public List<Role> findAll() {
        return list(SELECT_COLUMNS + "ORDER BY id", mapper);
    }

    public Role findByName(String name) {
        return one(SELECT_COLUMNS + "WHERE name = ?", mapper, name);
    }

    public Role findById(int id) {
        return one(SELECT_COLUMNS + "WHERE id = ?", mapper, id);
    }

    /** @return the role id, or 0 when the role name does not exist (should not happen with seed data). */
    public int findIdByName(String name) {
        try {
            return count("SELECT id FROM roles WHERE name = ?", name);
        } catch (RuntimeException e) {
            return 0;
        }
    }

    /** Simple lookup helper used by services when they must not fail on a missing role. */
    public int requireIdByName(String name) {
        Role role = findByName(name);
        if (role == null) {
            throw new IllegalStateException("Required role '" + name + "' is missing. "
                    + "Did you run database/schema.sql and database/seed.sql?");
        }
        return role.getId();
    }
}
