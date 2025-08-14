    package com.vso.DaddyJohn.Repositry;

    import com.vso.DaddyJohn.Entity.Users;
    import org.springframework.beans.factory.annotation.Autowired;
    import org.springframework.security.core.userdetails.UserDetails;
    import org.springframework.security.core.userdetails.User;
    import org.springframework.security.core.userdetails.UserDetailsService;
    import org.springframework.security.core.userdetails.UsernameNotFoundException;
    import org.springframework.stereotype.Repository;
    import org.springframework.stereotype.Service;

    @Service
    public class UserDetailService implements UserDetailsService {

        private final UserRepo userRepo;

        public UserDetailService(UserRepo userRepo) {
            this.userRepo = userRepo;
        }

        @Override
        public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
            Users user  = userRepo.findByUsername(username);

            if(user != null){
                return User.builder()
                        .username(user.getUsername())
                        .password(user.getPasswordHash())
                        .roles(user.getRoles().toArray(new String[0]))
                        .build();
            }

            throw new UsernameNotFoundException("USERNAME NOT FOUND" + username);
        }
    }
