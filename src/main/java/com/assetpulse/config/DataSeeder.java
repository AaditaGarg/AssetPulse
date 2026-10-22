package com.assetpulse.config;

import com.assetpulse.model.*;
import com.assetpulse.repository.AssetRepository;
import com.assetpulse.repository.AssetRequestRepository;
import com.assetpulse.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Creates the first admin (and optional demo data) when the database is empty. */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements ApplicationRunner {
    private final UserRepository users;
    private final AssetRepository assets;
    private final AssetRequestRepository requests;
    private final PasswordEncoder encoder;

    @Value("${app.seed.admin-email}") private String adminEmail;
    @Value("${app.seed.admin-password}") private String adminPassword;
    @Value("${app.seed.demo-data:true}") private boolean demo;

    @Override
    public void run(ApplicationArguments args) {
        if (users.count() > 0) return;

        User admin = user("Admin User", adminEmail, adminPassword, Role.ADMIN, "IT", null);
        log.info("Seeded admin account: {}", adminEmail);
        if (!demo) return;

        User manager = user("Maya Manager", "manager@assetpulse.com", "Manager@123", Role.MANAGER, "Engineering", null);
        User alice = user("Alice Employee", "alice@assetpulse.com", "Employee@123", Role.EMPLOYEE, "Engineering", manager);
        User bob = user("Bob Employee", "bob@assetpulse.com", "Employee@123", Role.EMPLOYEE, "Engineering", manager);

        asset("Dell Latitude 5440", AssetType.LAPTOP, "Dell", AssetStatus.AVAILABLE, null, 75, "1450", LocalDate.now().plusDays(12), null);
        asset("MacBook Pro 14", AssetType.LAPTOP, "Apple", AssetStatus.ALLOCATED, alice, 50, "2400", LocalDate.now().plusDays(400), null);
        asset("HP EliteBook 840", AssetType.LAPTOP, "HP", AssetStatus.AVAILABLE, null, 5, "1300", LocalDate.now().plusDays(300), null);
        asset("Dell U2723 Monitor", AssetType.MONITOR, "Dell", AssetStatus.AVAILABLE, null, 90, "420", LocalDate.now().plusDays(200), null);
        asset("Microsoft 365 E3 Licence", AssetType.SOFTWARE_LICENCE, "Microsoft", AssetStatus.ALLOCATED, bob, 2, "320", null, LocalDate.now().plusDays(20));
        asset("Logitech MX Keys", AssetType.PERIPHERAL, "Logitech", AssetStatus.AVAILABLE, null, 10, "110", null, null);

        AssetRequest r = new AssetRequest();
        r.setType(RequestType.ALLOCATION);
        r.setRequestedAssetType(AssetType.LAPTOP);
        r.setRequester(bob);
        r.setReason("Need a laptop for the new project");
        r.setStatus(RequestStatus.PENDING_MANAGER);
        requests.save(r);
        log.info("Seeded demo users, assets and one pending request");
    }

    private User user(String name, String email, String pw, Role role, String dept, User manager) {
        User u = new User();
        u.setName(name);
        u.setEmail(email.toLowerCase());
        u.setPassword(encoder.encode(pw));
        u.setRole(role);
        u.setDepartment(dept);
        u.setManager(manager);
        return users.save(u);
    }

    private void asset(String name, AssetType type, String brand, AssetStatus status, User holder,
                       int idleDays, String cost, LocalDate warranty, LocalDate licence) {
        Asset a = new Asset();
        a.setAssetTag("AST-" + java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        a.setName(name);
        a.setType(type);
        a.setBrand(brand);
        a.setStatus(status);
        a.setAssignedTo(holder);
        a.setPurchaseCost(new BigDecimal(cost));
        a.setPurchaseDate(LocalDate.now().minusMonths(10));
        a.setWarrantyExpiry(warranty);
        a.setLicenceExpiry(licence);
        a.setLocation("HQ - Bhopal");
        a.setLastActivityAt(LocalDateTime.now().minusDays(idleDays));
        assets.save(a);
    }
}
