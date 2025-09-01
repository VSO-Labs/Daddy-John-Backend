package com.vso.DaddyJohn.Controller;

import com.vso.DaddyJohn.Entity.Users;
import com.vso.DaddyJohn.Repositry.UserDetailService;
import com.vso.DaddyJohn.Service.UserService;
import com.vso.DaddyJohn.Utils.JwtUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*", maxAge = 3600)
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UserService userService;
    private final UserDetailService userDetailService;

    public AuthController(AuthenticationManager authenticationManager,
                          JwtUtil jwtUtil,
                          UserService userService,
                          UserDetailService userDetailService) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.userService = userService;
        this.userDetailService = userDetailService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest req) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.username(), req.password())
        );

        var userDetails = (org.springframework.security.core.userdetails.User) authentication.getPrincipal();

        List<String> roles = userDetails.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .collect(Collectors.toList());

        String token = jwtUtil.generateToken(userDetails.getUsername(), roles);

        return ResponseEntity.ok(new AuthResponse(token, "Bearer"));
    }

    @PostMapping("/signup")
    public Users signup(@RequestBody Users u) {
        return userService.addUser(u);
    }

    public record AuthRequest(String username, String password) {}
    public record AuthResponse(String token, String tokenType) {}
}
