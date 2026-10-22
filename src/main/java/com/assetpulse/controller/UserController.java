package com.assetpulse.controller;

import com.assetpulse.dto.Dtos.UserRequest;
import com.assetpulse.dto.Dtos.UserResponse;
import com.assetpulse.dto.Mapper;
import com.assetpulse.model.User;
import com.assetpulse.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService service;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public List<UserResponse> list() {
        return service.all().stream().map(Mapper::toResponse).toList();
    }

    @GetMapping("/my-team")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public List<UserResponse> myTeam(@AuthenticationPrincipal User me) {
        return service.teamOf(me).stream().map(Mapper::toResponse).toList();
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse create(@Valid @RequestBody UserRequest req) {
        return Mapper.toResponse(service.create(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse update(@PathVariable Long id, @Valid @RequestBody UserRequest req) {
        return Mapper.toResponse(service.update(id, req));
    }
}
