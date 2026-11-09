package com.datashare.configuration.security;

import com.datashare.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailService implements UserDetailsService {
    private final UserRepository users;

    public CustomUserDetailService(UserRepository users) { this.users = users; }

    @Override
    public UserDetails loadUserByUsername(String email) {
        return users.findByEmail(email).map(DataShareUserDetails::new)
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur inconnu."));
    }
}
