package com.vso.DaddyJohn.Service;

import com.vso.DaddyJohn.Entity.Users;
import com.vso.DaddyJohn.Repositry.UserRepo;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {
    private final UserRepo userRepo;
    private final PasswordEncoder passwordEncoder;


    public UserService(UserRepo userRepo, PasswordEncoder passwordEncoder) {
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
    }

    public Users addUser(Users users){
        if (users.getPasswordHash() != null) {
            users.setPasswordHash(passwordEncoder.encode(users.getPasswordHash()));
        }
        if (users.getRoles() == null) {
            users.setRoles(List.of("USER"));
        }
        return userRepo.save(users);
    }

    public void deleteUser(Users users){
        userRepo.delete(users);
    }

    public List<Users> getAllUser(){
        return userRepo.findAll();
    }
}
