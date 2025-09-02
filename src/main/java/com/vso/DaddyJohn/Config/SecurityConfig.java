package com.vso.DaddyJohn.Config;

import com.vso.DaddyJohn.Filter.JwtAuthenticationFilter;
import com.vso.DaddyJohn.Repositry.UserDetailService;
import com.vso.DaddyJohn.Utils.JwtUtil;
import com.vso.DaddyJohn.Utils.JwtAuthEntryPoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.http.HttpMethod;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;


@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final UserDetailService userDetailService;
    private final JwtUtil jwtUtil;
    private final JwtAuthEntryPoint authEntryPoint;

    public SecurityConfig(UserDetailService userDetailService, JwtUtil jwtUtil, JwtAuthEntryPoint authEntryPoint) {
        this.userDetailService = userDetailService;
        this.jwtUtil = jwtUtil;
        this.authEntryPoint = authEntryPoint;
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(jwtUtil, userDetailService);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(org.springframework.security.config.annotation.web.builders.HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .exceptionHandling(e -> e.authenticationEntryPoint(authEntryPoint))
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // public endpoints
                        .requestMatchers(HttpMethod.GET, "/", "/hello", "/health").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/auth/signup").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // File serving endpoints - allow public access to uploaded files
                        .requestMatchers(HttpMethod.GET, "/files/**").permitAll()

                        // Mock endpoint for testing
                        .requestMatchers("/api/mock/**").permitAll()

                        // protected endpoints
                        .requestMatchers("/api/conversations/**").authenticated()
                        .requestMatchers("/User/**").authenticated()
                        .anyRequest().authenticated()
                );

        http.addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Allow all origins
        configuration.addAllowedOriginPattern("*");

        // Allow all headers
        configuration.addAllowedHeader("*");

        // Allow all methods
        configuration.addAllowedMethod("*");

        // Expose headers if needed
        configuration.addExposedHeader("Authorization");

        // If you don't want cookies across origins
        configuration.setAllowCredentials(false);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}