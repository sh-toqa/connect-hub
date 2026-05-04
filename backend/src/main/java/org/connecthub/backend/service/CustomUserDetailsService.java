package org.connecthub.backend.service;

import org.connecthub.backend.model.User;
import org.connecthub.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Loads a User by their email (stored as JWT subject).
 * The UserDetails principal name is set to the UUID string
 * so controllers can still extract UUID from @AuthenticationPrincipal.
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));

        return new org.springframework.security.core.userdetails.User(
                user.getUserId().toString(),
                user.getHashedPassword(),
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }
}