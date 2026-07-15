package com.fooddelivery.ordermanagement.security;

import com.fooddelivery.ordermanagement.user.Role;
import com.fooddelivery.ordermanagement.user.User;
import com.fooddelivery.ordermanagement.user.UserRepository;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class UserAuthProvider implements AuthenticationProvider {

    private final UserRepository userRepository;

    public UserAuthProvider(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String userId = String.valueOf(authentication.getPrincipal());
        if (userId == null || userId.isBlank() || "null".equals(userId)) {
            throw new BadCredentialsException("Missing user id");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadCredentialsException("Unknown user: " + userId));

        List<SimpleGrantedAuthority> authorities = user.getRoles().stream()
                .map(Role::getName)
                .map(SimpleGrantedAuthority::new)
                .toList();

        return new UserIdAuthenticationToken(user.getId(), authorities);
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UserIdAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
