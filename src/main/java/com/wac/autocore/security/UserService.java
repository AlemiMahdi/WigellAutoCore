package com.wac.autocore.security;

import java.util.Map;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

public class UserService implements UserDetailsService{

    private final Map<String, AppUser> users;

    public UserService(PasswordEncoder passwordEncoder) {

        users = Map.of(
                "admin",
                new AppUser(
                        "admin",
                        passwordEncoder.encode(getPassword("admin")),
                        Role.ADMIN
                ),

                "mechanic",
                new AppUser(
                        "mechanic",
                        passwordEncoder.encode(getPassword("mechanic")),
                        Role.WORKSHOP
                ),

                "viewer",
                new AppUser(
                        "viewer",
                        passwordEncoder.encode(getPassword("viewer")),
                        Role.READ_ONLY
                )
        );
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        AppUser appUser = users.get(username);
        if (appUser == null) {
            throw new UsernameNotFoundException("Felaktiga inloggningsuppgifter");
        }
        return User.builder()
            .username(appUser.getUsername())
            .password(appUser.getPasswordHash())
            .roles(appUser.getRole().name())
            .build();
    }

    private static String getPassword(String variableName) {
        
        String password = System.getenv(variableName);
        if (password == null || password.isBlank()) {
            throw new IllegalStateException("Miljövariabel saknas: " + variableName);
        }
        return password;
    }
    
}
