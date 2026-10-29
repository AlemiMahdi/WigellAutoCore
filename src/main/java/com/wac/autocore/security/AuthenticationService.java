package com.wac.autocore.security;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service 
public class AuthenticationService {
    
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
        Authentication result = authenticationManager.authenticate(request);

        //Spara den autentiserade användaren
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        
        context.setAuthentication(result);
        SecurityContextHolder.setContext(context);
        
    }

    public void logout(){
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
