package org.santavm.tms.service;

import lombok.RequiredArgsConstructor;
import org.santavm.tms.model.User;
import org.santavm.tms.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository repository;
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Optional<User> userOptional = repository.findByEmail(email);
        return userOptional.orElseThrow(
                () -> new UsernameNotFoundException("user not found with email :" + email));
    }
}
