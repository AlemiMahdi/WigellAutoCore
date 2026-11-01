package com.wac.autocore.security;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.security.core.AuthenticationException;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

@Service 
public class AuthenticationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthenticationService.class);
    private final AuthenticationManager authenticationManager;

    public AuthenticationService(AuthenticationManager authenticationManager){
        this.authenticationManager = authenticationManager;
    }

    public void login(String username, String passowrd){

        //Ta bort eventuell tidigare inloggning
        SecurityContextHolder.clearContext();

        //Skapa en inloggninsförsök
        Authentication request = UsernamePasswordAuthenticationToken.unauthenticated(
            username, passowrd);

        //Kontrollera användarnamn och lösenord
        Authentication result;

        try {
            result = authenticationManager.authenticate(request);
        } catch (AuthenticationException exception) {
            LOGGER.warn("Login failed for user {}", username);
            throw exception;
        }

        //Spara den autentiserade användaren
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        
        context.setAuthentication(result);
        SecurityContextHolder.setContext(context);
        LOGGER.info("User {} logged in", username);
        
    }

    public void logout(){

        LOGGER.info("User {} logged out", getCurrentUsername());
        SecurityContextHolder.clearContext();
    }

    public boolean isLoggedIn(){
        Authentication authentication =
            SecurityContextHolder.getContext().getAuthentication();
        return authentication != null
            && authentication.isAuthenticated();
    }

    public String getCurrentUsername(){
        if (!isLoggedIn()) {
            return null;
        }
        return SecurityContextHolder.getContext()
            .getAuthentication()
            .getName();
    }

    public boolean hasRole(Role role) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        
        return authentication.getAuthorities()
                .stream()
                .anyMatch( authority ->
                    authority.getAuthority().equals(
                        "ROLE_" + role.name()
                    )
                );
    }
}
