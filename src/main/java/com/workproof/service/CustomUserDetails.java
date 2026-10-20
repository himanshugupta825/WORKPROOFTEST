package com.workproof.service;

import com.workproof.model.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

public class CustomUserDetails implements UserDetails {

    private final User user;
    private final Collection<? extends GrantedAuthority> authorities;
    private final boolean enabled;

    public CustomUserDetails(User user, Collection<? extends GrantedAuthority> authorities, boolean enabled) {
        this.user = user;
        this.authorities = authorities;
        this.enabled = enabled;
    }

    public User getUser() {
        return user;
    }

    public Integer getId() {
        return user.getId();
    }

    public String getFullName() {
        return user.getFullName();
    }

    public String getRoleName() {
        return user.getRoleName();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return user.getPasswordHash();
    }

    @Override
    public String getUsername() {
        return user.getEmail();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}

