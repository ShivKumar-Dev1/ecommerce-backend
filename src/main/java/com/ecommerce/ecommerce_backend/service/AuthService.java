package com.ecommerce.ecommerce_backend.service;

import com.ecommerce.ecommerce_backend.dto.request.LoginRequest;
import com.ecommerce.ecommerce_backend.dto.request.RegisterRequest;
import com.ecommerce.ecommerce_backend.dto.response.AuthResponse;
import com.ecommerce.ecommerce_backend.entity.User;
import com.ecommerce.ecommerce_backend.exception.ApiException;
import com.ecommerce.ecommerce_backend.repository.UserRepository;
import com.ecommerce.ecommerce_backend.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

    // ── REGISTER ──────────────────────────────────────────
    public AuthResponse register(RegisterRequest request) {

        // Check if email already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ApiException(
                    "Email already registered", HttpStatus.BAD_REQUEST);
        }

        // Create new user
        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(User.Role.USER)
                .build();

        // Save to database
        User savedUser = userRepository.save(user);

        // Generate JWT token — pass role with ROLE_ prefix
        String role = "ROLE_" + savedUser.getRole().name();
        String token = jwtTokenProvider.generateTokenFromEmail(savedUser.getEmail(), role);

        // Return response
        return AuthResponse.builder()
                .token(token)
                .type("Bearer")
                .id(savedUser.getId())
                .name(savedUser.getName())
                .email(savedUser.getEmail())
                .role(savedUser.getRole().name())
                .build();
    }

    // ── LOGIN ─────────────────────────────────────────────
    public AuthResponse login(LoginRequest request) {

        // Authenticate user — throws exception if wrong credentials
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        // Generate JWT token from authentication object
        String token = jwtTokenProvider.generateToken(authentication);

        // Get user details from database
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() ->
                        new ApiException("User not found", HttpStatus.NOT_FOUND));

        // Return response
        return AuthResponse.builder()
                .token(token)
                .type("Bearer")
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .build();
    }
}