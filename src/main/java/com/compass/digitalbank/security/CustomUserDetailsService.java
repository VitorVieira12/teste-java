package com.compass.digitalbank.security;

import com.compass.digitalbank.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByUsername(username)
                .map(user -> new AuthenticatedAccount(
                        user.getAccount().getId(),
                        user.getUsername(),
                        user.getPassword()))
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }
}
