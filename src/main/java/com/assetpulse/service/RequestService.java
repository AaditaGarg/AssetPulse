package com.assetpulse.service;

import com.assetpulse.dto.Dtos.AllocationPayload;
import com.assetpulse.dto.Dtos.DecisionPayload;
import com.assetpulse.dto.Dtos.ReturnPayload;
import com.assetpulse.exception.ApiException;
import com.assetpulse.model.*;
import com.assetpulse.repository.AssetRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Approval workflow:
 *  ALLOCATION: employee -> PENDING_MANAGER -> (manager approves) -> PENDING_ADMIN -> (admin approves + picks asset) -> APPROVED
 *              (managers / users without a manager skip the manager step)
 *  RETURN:     owner -> PENDING_ADMIN -> (admin approves) -> APPROVED, asset becomes AVAILABLE
 */
@Service
@RequiredArgsConstructor
public class RequestService {
    private static final List<RequestStatus> OPEN = List.of(RequestStatus.PENDING_MANAGER, RequestStatus.PENDING_ADMIN);

    private final AssetRequestRepository requests;
    private final AssetService assetService;

    public AssetRequest get(Long id) {
        return requests.findById(id).orElseThrow(() -> ApiException.notFound("Request not found: " + id));
    }

    public List<AssetRequest> mine(User u) { return requests.findByRequesterOrderByCreatedAtDesc(u); }

    public List<AssetRequest> all(RequestStatus status) {
        return status == null ? requests.findAllByOrderByCreatedAtDesc() : requests.findByStatusOrderByCreatedAtAsc(status);
    }

    public List<AssetRequest> pendingForManager(User manager) {
        return requests.findByStatusAndRequester_ManagerOrderByCreatedAtAsc(RequestStatus.PENDING_MANAGER, manager);
    }

    public List<AssetRequest> pendingForAdmin() {
        return requests.findByStatusOrderByCreatedAtAsc(RequestStatus.PENDING_ADMIN);
    }

    @Transactional
    public AssetRequest createAllocation(User requester, AllocationPayload p) {
        AssetType type = p.assetType();
        Asset asset = null;
        if (p.assetId() != null) {
            asset = assetService.get(p.assetId());
            if (asset.getStatus() != AssetStatus.AVAILABLE)
                throw ApiException.conflict("Requested asset is not available");
            type = asset.getType();
        }
        AssetRequest r = new AssetRequest();
        r.setType(RequestType.ALLOCATION);
        r.setRequestedAssetType(type);
        r.setAsset(asset);
        r.setRequester(requester);
        r.setReason(p.reason());
        boolean needsManager = requester.getRole() == Role.EMPLOYEE && requester.getManager() != null;
        r.setStatus(needsManager ? RequestStatus.PENDING_MANAGER : RequestStatus.PENDING_ADMIN);
        return requests.save(r);
    }

    @Transactional
    public AssetRequest createReturn(User requester, ReturnPayload p) {
        Asset asset = assetService.get(p.assetId());
        if (asset.getStatus() != AssetStatus.ALLOCATED || asset.getAssignedTo() == null
                || !asset.getAssignedTo().getId().equals(requester.getId()))
            throw ApiException.badRequest("You can only return an asset that is allocated to you");
        if (requests.existsByAssetAndTypeAndStatusIn(asset, RequestType.RETURN, OPEN))
            throw ApiException.conflict("A return request for this asset is already pending");
        AssetRequest r = new AssetRequest();
        r.setType(RequestType.RETURN);
        r.setRequestedAssetType(asset.getType());
        r.setAsset(asset);
        r.setRequester(requester);
        r.setReason(p.reason());
        r.setStatus(RequestStatus.PENDING_ADMIN);
        return requests.save(r);
    }

    @Transactional
    public AssetRequest managerDecision(Long id, DecisionPayload p, User actor) {
        AssetRequest r = get(id);
        if (r.getStatus() != RequestStatus.PENDING_MANAGER)
            throw ApiException.conflict("Request is not awaiting manager approval");
        if (actor.getRole() == Role.MANAGER) {
            User mgr = r.getRequester().getManager();
            if (mgr == null || !mgr.getId().equals(actor.getId()))
                throw ApiException.forbidden("This request does not belong to your team");
        }
        r.setManagerReviewer(actor);
        r.setManagerComment(p.comment());
        r.setStatus(p.approve() ? RequestStatus.PENDING_ADMIN : RequestStatus.REJECTED);
        return requests.save(r);
    }

    @Transactional
    public AssetRequest adminDecision(Long id, DecisionPayload p, User admin) {
        AssetRequest r = get(id);
        if (r.getStatus() != RequestStatus.PENDING_ADMIN)
            throw ApiException.conflict("Request is not awaiting admin approval");
        r.setAdminReviewer(admin);
        r.setAdminComment(p.comment());

        if (!p.approve()) {
            r.setStatus(RequestStatus.REJECTED);
            return requests.save(r);
        }

        if (r.getType() == RequestType.RETURN) {
            assetService.release(r.getAsset(), "Return approved: " + nullToEmpty(r.getReason()));
        } else {
            Asset asset = r.getAsset();
            if (p.assetId() != null) asset = assetService.get(p.assetId());
            if (asset == null) throw ApiException.badRequest("assetId is required to approve this allocation");
            if (r.getRequestedAssetType() != null && asset.getType() != r.getRequestedAssetType())
                throw ApiException.badRequest("Asset type does not match the requested type " + r.getRequestedAssetType());
            if (asset.getAssignedTo() != null && asset.getAssignedTo().getId().equals(r.getRequester().getId()))
                throw ApiException.badRequest("Asset is already assigned to this user");
            assetService.allocate(asset, r.getRequester(), "Request #" + r.getId());
            r.setAsset(asset);
        }
        r.setStatus(RequestStatus.APPROVED);
        return requests.save(r);
    }

    @Transactional
    public AssetRequest cancel(Long id, User actor) {
        AssetRequest r = get(id);
        if (!r.getRequester().getId().equals(actor.getId())) throw ApiException.forbidden("Not your request");
        if (!OPEN.contains(r.getStatus())) throw ApiException.conflict("Only pending requests can be cancelled");
        r.setStatus(RequestStatus.CANCELLED);
        return requests.save(r);
    }

    private static String nullToEmpty(String s) { return s == null ? "" : s; }
}
