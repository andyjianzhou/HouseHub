package com.housing.backend.security;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collections;

public class JwtAuthenticationToken extends AbstractAuthenticationToken {

    private final UserDetails userDetails;

    public JwtAuthenticationToken(UserDetails userDetails) {
        super(Collections.emptyList());
        this.userDetails = userDetails;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return null;
    }

    @Override
    public Object getPrincipal() {
        return userDetails;
    }
}