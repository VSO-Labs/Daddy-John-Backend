package com.vso.DaddyJohn.Repositry;

import com.vso.DaddyJohn.Entity.Users;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.stream.Collectors;

@Service
public class UserDetailService implements UserDetailsService {

    private final UserRepo userRepo;

    public UserDetailService(UserRepo userRepo) {
        this.userRepo = userRepo;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Users user  = userRepo.findByUsername(username);

        if (user != null) {
            var roles = user.getRoles(); // List<String>
            var authorities = roles.stream()
                    .map(r -> r.startsWith("ROLE_") ? r : "ROLE_" + r)
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());

            return User.withUsername(user.getUsername())
                    .password(user.getPasswordHash())
                    .authorities(authorities)
                    .build();
        }

        throw new UsernameNotFoundException("USERNAME NOT FOUND: " + username);
    }
}
