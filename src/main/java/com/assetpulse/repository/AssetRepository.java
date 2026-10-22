package com.assetpulse.repository;

import com.assetpulse.model.Asset;
import com.assetpulse.model.AssetStatus;
import com.assetpulse.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AssetRepository extends JpaRepository<Asset, Long> {
    Optional<Asset> findByAssetTag(String assetTag);
    boolean existsByAssetTag(String assetTag);
    List<Asset> findByAssignedTo(User user);
    List<Asset> findByWarrantyExpiryLessThanEqualAndStatusNot(LocalDate date, AssetStatus status);
    List<Asset> findByLicenceExpiryLessThanEqualAndStatusNot(LocalDate date, AssetStatus status);
}
