package com.fooddelivery.ordermanagement.security;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

/**
 * Authentication token whose principal is the raw user id from the {@code X-User-Id} header.
 */
public class UserIdAuthenticationToken extends AbstractAuthenticationToken {

    private final String userId;

    public UserIdAuthenticationToken(String userId) {
        super(null);
        this.userId = userId;
        setAuthenticated(false);
    }

    public UserIdAuthenticationToken(String userId, Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.userId = userId;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return null;
    }

    @Override
    public Object getPrincipal() {
        return userId;
    }
}
