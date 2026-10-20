package com.workproof.service;

import com.workproof.model.User;
import com.workproof.model.enums.UserStatus;
import com.workproof.repository.UserRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email != null ? email.trim().toLowerCase() : "")
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        boolean enabled = user.getUserStatus() == UserStatus.ACTIVE;

        List<GrantedAuthority> authorities = new ArrayList<>();
        String role = user.getRoleName();
        if (role != null) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()));
            // Grant role aliases for compatibility
            if ("MEMBER".equalsIgnoreCase(role)) {
                authorities.add(new SimpleGrantedAuthority("ROLE_TECHNICIAN"));
            } else if ("TECHNICIAN".equalsIgnoreCase(role)) {
                authorities.add(new SimpleGrantedAuthority("ROLE_MEMBER"));
            } else if ("REVIEWER".equalsIgnoreCase(role)) {
                authorities.add(new SimpleGrantedAuthority("ROLE_MANAGER"));
            } else if ("ADMIN".equalsIgnoreCase(role)) {
                authorities.add(new SimpleGrantedAuthority("ROLE_MANAGER"));
            }
        }

        return new CustomUserDetails(user, authorities, enabled);
    }
}

