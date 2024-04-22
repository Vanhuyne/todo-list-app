package com.vanhuy.todobackend.service;

import com.vanhuy.todobackend.config.JwtService;
import com.vanhuy.todobackend.entity.*;
import com.vanhuy.todobackend.repo.RoleRepo;
import com.vanhuy.todobackend.repo.UserRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthenticationService {
    private final UserRepo userRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final RoleRepo roleRepo;

    public AuthenticationResponse register(RegisterRequest request) {
        // default role is user
        Set<String> requestRoles = request.getRoles();
        if (requestRoles == null || requestRoles.isEmpty()) {
            requestRoles = new HashSet<>();
            requestRoles.add("ROLE_USER");
        }

        Set<Role> roles = requestRoles.stream()
                .map(roleName -> roleRepo.findByName(roleName)
                        .orElseThrow(() -> new RuntimeException("Role not found")))
                .filter(role -> role != null)
                .collect(Collectors.toSet());

        var user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .enabled(true)
                .password(passwordEncoder.encode(request.getPassword()))
                .roles(roles)
                .build();
        userRepo.save(user);
        return AuthenticationResponse.builder()
                .token(jwtService.generateToken(user))
                .build();
    }

    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        Optional<User> userOptional = userRepo.findByEmail(request.getEmail());
        if (userOptional.isEmpty()) {
            throw new RuntimeException("User not found");
        }
        User user = userOptional.get();
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword(),
                        user.getAuthorities()
                )
        );

        // authenticate user
        var token = jwtService.generateToken(user);
        return AuthenticationResponse.builder()
                .token(token)
                .build();
    }
}