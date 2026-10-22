package com.assetpulse.repository;

import com.assetpulse.model.Asset;
import com.assetpulse.model.AssetTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AssetTransactionRepository extends JpaRepository<AssetTransaction, Long> {
    List<AssetTransaction> findByAssetOrderByTimestampDesc(Asset asset);
    List<AssetTransaction> findTop15ByOrderByTimestampDesc();
}
