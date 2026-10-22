package com.assetpulse.service;

import com.assetpulse.dto.Dtos.RegisterRequest;
import com.assetpulse.dto.Dtos.UserRequest;
import com.assetpulse.exception.ApiException;
import com.assetpulse.model.Role;
import com.assetpulse.model.User;
import com.assetpulse.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository users;
    private final PasswordEncoder encoder;

    public List<User> all() { return users.findAll(); }

    public List<User> teamOf(User manager) { return users.findByManager(manager); }

    public User get(Long id) {
        return users.findById(id).orElseThrow(() -> ApiException.notFound("User not found: " + id));
    }

    @Transactional
    public User register(RegisterRequest r) {
        if (users.existsByEmail(r.email().toLowerCase())) throw ApiException.conflict("Email already registered");
        User u = new User();
        u.setName(r.name());
        u.setEmail(r.email().toLowerCase());
        u.setPassword(encoder.encode(r.password()));
        u.setRole(Role.EMPLOYEE);
        u.setDepartment(r.department());
        return users.save(u);
    }

    @Transactional
    public User create(UserRequest r) {
        if (r.password() == null || r.password().isBlank()) throw ApiException.badRequest("password is required");
        if (users.existsByEmail(r.email().toLowerCase())) throw ApiException.conflict("Email already registered");
        User u = new User();
        u.setPassword(encoder.encode(r.password()));
        return users.save(apply(u, r));
    }

    @Transactional
    public User update(Long id, UserRequest r) {
        User u = get(id);
        users.findByEmail(r.email().toLowerCase())
                .filter(other -> !other.getId().equals(id))
                .ifPresent(o -> { throw ApiException.conflict("Email already in use"); });
        if (r.password() != null && !r.password().isBlank()) u.setPassword(encoder.encode(r.password()));
        return users.save(apply(u, r));
    }

    private User apply(User u, UserRequest r) {
        u.setName(r.name());
        u.setEmail(r.email().toLowerCase());
        u.setRole(r.role());
        u.setDepartment(r.department());
        if (r.active() != null) u.setActive(r.active());
        if (r.managerId() != null) {
            User m = get(r.managerId());
            if (m.getRole() == Role.EMPLOYEE) throw ApiException.badRequest("Manager must have MANAGER or ADMIN role");
            u.setManager(m);
        } else {
            u.setManager(null);
        }
        return u;
    }
}
