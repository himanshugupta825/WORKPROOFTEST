package com.workproof.model;

import com.workproof.model.enums.RoleName;

/** Row of the `roles` table. */
public class Role {

    private int id;
    private String name;
    private String description;

    public Role() {
    }

    public Role(int id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    /** Typed accessor used by JSP EL: ${currentUser.roleName}. */
    public RoleName getRoleName() {
        return RoleName.from(name);
    }

    public String getLabel() {
        return getRoleName().getLabel();
    }

    @Override
    public String toString() {
        return "Role{" + name + "}";
    }
}
