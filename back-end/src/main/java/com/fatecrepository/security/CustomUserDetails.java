package com.fatecrepository.security;

import com.fatecrepository.model.Gestor;
import com.fatecrepository.model.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.UUID;

public class CustomUserDetails implements UserDetails {

    private final UUID id;
    private final String email;
    private final String password;
    private final Collection<? extends GrantedAuthority> authorities;

    public CustomUserDetails(UUID id, String email, String password, Collection<? extends GrantedAuthority> authorities) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.authorities = authorities;
    }

    public static CustomUserDetails fromUser(User user) {
        String role = "ROLE_" + user.getRole().name();
        return new CustomUserDetails(
            user.getId(),
            user.getEmail(),
            user.getSenha(),
            Collections.singletonList(new SimpleGrantedAuthority(role))
        );
    }

    public static CustomUserDetails fromGestor(Gestor gestor) {
        return new CustomUserDetails(
            gestor.getId(),
            gestor.getEmail(),
            gestor.getSenha(),
            Collections.singletonList(new SimpleGrantedAuthority("ROLE_GESTOR"))
        );
    }

    public UUID getId() {
        return id;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }
}
