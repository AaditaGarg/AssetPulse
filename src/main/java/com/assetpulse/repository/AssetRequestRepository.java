package com.assetpulse.repository;

import com.assetpulse.model.*;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface AssetRequestRepository extends JpaRepository<AssetRequest, Long> {
    List<AssetRequest> findByRequesterOrderByCreatedAtDesc(User requester);
    List<AssetRequest> findByStatusOrderByCreatedAtAsc(RequestStatus status);
    List<AssetRequest> findByStatusAndRequester_ManagerOrderByCreatedAtAsc(RequestStatus status, User manager);
    List<AssetRequest> findAllByOrderByCreatedAtDesc();
    List<AssetRequest> findByTypeAndStatusIn(RequestType type, Collection<RequestStatus> statuses);
    boolean existsByAssetAndTypeAndStatusIn(Asset asset, RequestType type, Collection<RequestStatus> statuses);
    long countByStatusIn(Collection<RequestStatus> statuses);
}
