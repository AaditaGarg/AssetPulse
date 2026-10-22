package com.assetpulse.service;

import com.assetpulse.exception.ApiException;
import com.assetpulse.model.*;
import com.assetpulse.repository.AlertRepository;
import com.assetpulse.repository.AssetRepository;
import com.assetpulse.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlertService {
    private final AlertRepository alerts;
    private final AssetRepository assets;
    private final UserRepository users;
    private final ObjectProvider<JavaMailSender> mailSender;

    @Value("${app.alerts.days-before-expiry:30}") private int daysBefore;
    @Value("${app.mail.enabled:false}") private boolean mailEnabled;
    @Value("${app.mail.from:no-reply@assetpulse.local}") private String mailFrom;

    public List<Alert> list(boolean includeAcknowledged) {
        return includeAcknowledged ? alerts.findAllByOrderByCreatedAtDesc() : alerts.findByAcknowledgedFalseOrderByExpiryDateAsc();
    }

    @Transactional
    public Alert acknowledge(Long id) {
        Alert a = alerts.findById(id).orElseThrow(() -> ApiException.notFound("Alert not found: " + id));
        a.setAcknowledged(true);
        return alerts.save(a);
    }

    /** Runs daily (cron configurable in application.properties). */
    @Scheduled(cron = "${app.alerts.cron:0 0 8 * * *}")
    public void scheduledScan() {
        int n = scan();
        log.info("Expiry scan finished: {} new alert(s)", n);
    }

    /** Creates alerts for warranties / licences that are expired or expire within the configured window. */
    @Transactional
    public int scan() {
        LocalDate limit = LocalDate.now().plusDays(daysBefore);
        List<Alert> created = new ArrayList<>();

        for (Asset a : assets.findByWarrantyExpiryLessThanEqualAndStatusNot(limit, AssetStatus.RETIRED)) {
            if (!alerts.existsByAssetAndTypeAndExpiryDate(a, AlertType.WARRANTY_EXPIRY, a.getWarrantyExpiry()))
                created.add(alerts.save(build(a, AlertType.WARRANTY_EXPIRY, a.getWarrantyExpiry())));
        }
        for (Asset a : assets.findByLicenceExpiryLessThanEqualAndStatusNot(limit, AssetStatus.RETIRED)) {
            if (!alerts.existsByAssetAndTypeAndExpiryDate(a, AlertType.LICENCE_EXPIRY, a.getLicenceExpiry()))
                created.add(alerts.save(build(a, AlertType.LICENCE_EXPIRY, a.getLicenceExpiry())));
        }
        notifyAdmins(created);
        return created.size();
    }

    private Alert build(Asset asset, AlertType type, LocalDate expiry) {
        long days = ChronoUnit.DAYS.between(LocalDate.now(), expiry);
        String label = type == AlertType.WARRANTY_EXPIRY ? "Warranty" : "Licence";
        String msg = days < 0
                ? "%s for %s (%s) expired on %s".formatted(label, asset.getName(), asset.getAssetTag(), expiry)
                : "%s for %s (%s) expires on %s (%d days left)".formatted(label, asset.getName(), asset.getAssetTag(), expiry, days);
        Alert alert = new Alert();
        alert.setAsset(asset);
        alert.setType(type);
        alert.setExpiryDate(expiry);
        alert.setMessage(msg);
        return alert;
    }

    private void notifyAdmins(List<Alert> created) {
        if (!mailEnabled || created.isEmpty()) return;
        JavaMailSender sender = mailSender.getIfAvailable();
        if (sender == null) { log.warn("Mail enabled but no SMTP host configured"); return; }
        String[] to = users.findByRole(Role.ADMIN).stream().filter(User::isActive)
                .map(User::getEmail).toArray(String[]::new);
        if (to.length == 0) return;
        try {
            SimpleMailMessage m = new SimpleMailMessage();
            m.setFrom(mailFrom);
            m.setTo(to);
            m.setSubject("AssetPulse: " + created.size() + " new expiry alert(s)");
            m.setText(created.stream().map(Alert::getMessage).collect(Collectors.joining("\n")));
            sender.send(m);
        } catch (Exception e) {
            log.warn("Could not send alert email: {}", e.getMessage());
        }
    }
}
