package com.assetpulse.controller;

import com.assetpulse.dto.Dtos.*;
import com.assetpulse.dto.Mapper;
import com.assetpulse.exception.ApiException;
import com.assetpulse.model.User;
import com.assetpulse.repository.UserRepository;
import com.assetpulse.security.JwtService;
import com.assetpulse.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final UserService userService;

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest req) {
        User u = users.findByEmail(req.email().toLowerCase())
                .filter(User::isActive)
                .filter(x -> encoder.matches(req.password(), x.getPassword()))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));
        return new AuthResponse(jwt.generate(u), Mapper.toResponse(u));
    }

    /** Public self-registration always creates an EMPLOYEE. Admins create other roles via /api/users. */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest req) {
        User u = userService.register(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(new AuthResponse(jwt.generate(u), Mapper.toResponse(u)));
    }

    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal User user) {
        return Mapper.toResponse(user);
    }
}
