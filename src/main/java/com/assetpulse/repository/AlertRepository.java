package com.assetpulse.repository;

import com.assetpulse.model.Alert;
import com.assetpulse.model.AlertType;
import com.assetpulse.model.Asset;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface AlertRepository extends JpaRepository<Alert, Long> {
    List<Alert> findByAcknowledgedFalseOrderByExpiryDateAsc();
    List<Alert> findAllByOrderByCreatedAtDesc();
    boolean existsByAssetAndTypeAndExpiryDate(Asset asset, AlertType type, LocalDate expiryDate);
    long countByAcknowledgedFalse();
}
