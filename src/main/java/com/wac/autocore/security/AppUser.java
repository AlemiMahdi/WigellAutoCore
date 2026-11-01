package com.wac.autocore.security;

import java.util.Objects;

public class AppUser {
    private final String username;
    private final String passwordHash;
    private final Role role;

    public AppUser(String username, String passwordHash, Role role){
        this.username = Objects.requireNonNull(username);
        this.passwordHash = Objects.requireNonNull(passwordHash);
        this.role = Objects.requireNonNull(role);
    }

    public String getUsername(){
        return username;
    }

    public String getPasswordHash(){
        return passwordHash;
    }

    public Role getRole(){
        return role;
    }
}
