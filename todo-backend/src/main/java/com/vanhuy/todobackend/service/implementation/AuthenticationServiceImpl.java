package com.vanhuy.todobackend.service.implementation;

import com.vanhuy.todobackend.service.EmailService;
import com.vanhuy.todobackend.service.JwtService;
import com.vanhuy.todobackend.dtos.AuthenticationRequest;
import com.vanhuy.todobackend.dtos.AuthenticationResponse;
import com.vanhuy.todobackend.dtos.RegisterRequest;
import com.vanhuy.todobackend.entity.*;
import com.vanhuy.todobackend.exception.AuthenticationException;
import com.vanhuy.todobackend.repo.RoleRepo;
import com.vanhuy.todobackend.repo.UserRepo;
import com.vanhuy.todobackend.service.AuthenticationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthenticationServiceImpl implements AuthenticationService {
    private final UserRepo userRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final RoleRepo roleRepo;
    private final EmailService emailService;



    @Override
    public AuthenticationResponse register(RegisterRequest request) {
        // check if email exists
        if (userRepo.findByEmail(request.getEmail()).isPresent()) {
            throw new AuthenticationException("Email already exists");
        }
        // default role is user
        Set<String> requestRoles = request.getRoles();
        if (requestRoles == null || requestRoles.isEmpty()) {
            requestRoles = new HashSet<>();
            requestRoles.add("ROLE_USER");
        }

        Set<Role> roles = requestRoles.stream()
                .map(roleName -> roleRepo.findByName(roleName)
                        .orElseThrow(() -> new RuntimeException("Role not found")))
                .filter(Objects::nonNull)
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

        // send email
        emailService.sendWelcomeEmail(user.getEmail(), user.getFirstName() + " " + user.getLastName());

        return AuthenticationResponse.builder()
                .token(jwtService.generateToken(user))
                .build();
    }

    @Override
    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        try {
            User user = userRepo.findByEmail(request.getEmail())
                    .orElseThrow(() -> new AuthenticationException("Email does not exist"));

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
        } catch (BadCredentialsException e) {
            throw new AuthenticationException("Password is incorrect");
        }
    }
}
