package com.assetpulse.service;

import com.assetpulse.dto.Dtos.AssetPayload;
import com.assetpulse.exception.ApiException;
import com.assetpulse.model.*;
import com.assetpulse.repository.AssetRepository;
import com.assetpulse.repository.AssetTransactionRepository;
import com.assetpulse.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AssetService {
    public static final String QR_PREFIX = "ASSETPULSE:";

    private final AssetRepository assets;
    private final UserRepository users;
    private final AssetTransactionRepository transactions;

    // ---------- queries ----------
    public List<Asset> search(AssetStatus status, AssetType type, String q) {
        String needle = q == null ? "" : q.trim().toLowerCase();
        return assets.findAll(Sort.by(Sort.Direction.DESC, "createdAt")).stream()
                .filter(a -> status == null || a.getStatus() == status)
                .filter(a -> type == null || a.getType() == type)
                .filter(a -> needle.isEmpty() || contains(a.getName(), needle)
                        || contains(a.getAssetTag(), needle) || contains(a.getSerialNumber(), needle))
                .toList();
    }

    public Asset get(Long id) {
        return assets.findById(id).orElseThrow(() -> ApiException.notFound("Asset not found: " + id));
    }

    public Asset getByCode(String code) {
        String tag = code.trim();
        if (tag.startsWith(QR_PREFIX)) tag = tag.substring(QR_PREFIX.length());
        return assets.findByAssetTag(tag).orElseThrow(() -> ApiException.notFound("No asset for code: " + code));
    }

    public List<Asset> assignedTo(User user) { return assets.findByAssignedTo(user); }

    public List<AssetTransaction> history(Long assetId) {
        return transactions.findByAssetOrderByTimestampDesc(get(assetId));
    }

    // ---------- CRUD ----------
    @Transactional
    public Asset create(AssetPayload p) {
        Asset a = new Asset();
        apply(a, p);
        a.setAssetTag(generateTag());
        AssetStatus s = p.status() == null ? AssetStatus.AVAILABLE : p.status();
        if (s == AssetStatus.ALLOCATED || s == AssetStatus.CHECKED_OUT)
            throw ApiException.badRequest("New assets cannot start as ALLOCATED or CHECKED_OUT");
        a.setStatus(s);
        a.setLastActivityAt(LocalDateTime.now());
        return assets.save(a);
    }

    @Transactional
    public Asset update(Long id, AssetPayload p) {
        Asset a = get(id);
        apply(a, p);
        AssetStatus s = p.status();
        if (s != null && s != a.getStatus()) {
            if (a.getAssignedTo() != null) throw ApiException.conflict("Asset is assigned to a user; process a return first");
            if (s == AssetStatus.ALLOCATED || s == AssetStatus.CHECKED_OUT)
                throw ApiException.badRequest("Use allocation requests or QR check-out for this status");
            a.setStatus(s);
        }
        return assets.save(a);
    }

    /** Soft delete: keeps history, marks asset RETIRED. */
    @Transactional
    public Asset retire(Long id) {
        Asset a = get(id);
        if (a.getAssignedTo() != null) throw ApiException.conflict("Asset is assigned to a user; process a return first");
        a.setStatus(AssetStatus.RETIRED);
        return assets.save(a);
    }

    // ---------- QR check-out / check-in ----------
    @Transactional
    public Asset checkOut(String code, User actor, Long targetUserId) {
        Asset a = getByCode(code);
        User holder = actor;
        if (targetUserId != null) {
            if (actor.getRole() == Role.EMPLOYEE) throw ApiException.forbidden("Employees can only check out for themselves");
            holder = users.findById(targetUserId).orElseThrow(() -> ApiException.notFound("User not found"));
        }
        switch (a.getStatus()) {
            case ALLOCATED -> {
                if (a.getAssignedTo() != null && a.getAssignedTo().getId().equals(holder.getId())) {
                    log(a, holder, TransactionAction.USAGE_LOGGED, "Usage confirmed via QR scan");
                    touch(a);
                } else {
                    throw ApiException.conflict("Asset is allocated to " + a.getAssignedTo().getName());
                }
            }
            case AVAILABLE -> {
                a.setStatus(AssetStatus.CHECKED_OUT);
                a.setAssignedTo(holder);
                touch(a);
                log(a, holder, TransactionAction.CHECK_OUT, "Checked out via QR scan");
            }
            default -> throw ApiException.conflict("Asset is not available (status: " + a.getStatus() + ")");
        }
        return assets.save(a);
    }

    @Transactional
    public Asset checkIn(String code, User actor) {
        Asset a = getByCode(code);
        if (a.getStatus() != AssetStatus.CHECKED_OUT)
            throw ApiException.conflict("Asset is not checked out (status: " + a.getStatus() + ")");
        User holder = a.getAssignedTo();
        if (actor.getRole() == Role.EMPLOYEE && (holder == null || !holder.getId().equals(actor.getId())))
            throw ApiException.forbidden("Only the current holder can check this asset in");
        log(a, holder, TransactionAction.CHECK_IN, "Checked in via QR scan");
        a.setStatus(AssetStatus.AVAILABLE);
        a.setAssignedTo(null);
        touch(a);
        return assets.save(a);
    }

    // ---------- used by approval workflow ----------
    @Transactional
    public Asset allocate(Asset a, User user, String note) {
        if (a.getStatus() == AssetStatus.ALLOCATED && a.getAssignedTo() != null) {
            log(a, a.getAssignedTo(), TransactionAction.RETURNED,
                    "Reassigned to " + user.getName() + (note == null ? "" : " (" + note + ")"));
        } else if (a.getStatus() != AssetStatus.AVAILABLE) {
            throw ApiException.conflict("Asset cannot be allocated (status: " + a.getStatus() + ")");
        }
        a.setStatus(AssetStatus.ALLOCATED);
        a.setAssignedTo(user);
        touch(a);
        log(a, user, TransactionAction.ALLOCATED, note);
        return assets.save(a);
    }

    @Transactional
    public Asset release(Asset a, String note) {
        log(a, a.getAssignedTo(), TransactionAction.RETURNED, note);
        a.setStatus(AssetStatus.AVAILABLE);
        a.setAssignedTo(null);
        touch(a);
        return assets.save(a);
    }

    // ---------- helpers ----------
    private void touch(Asset a) { a.setLastActivityAt(LocalDateTime.now()); }

    private void log(Asset a, User u, TransactionAction action, String note) {
        AssetTransaction t = new AssetTransaction();
        t.setAsset(a);
        t.setUser(u);
        t.setAction(action);
        t.setNote(note);
        transactions.save(t);
    }

    private void apply(Asset a, AssetPayload p) {
        a.setName(p.name());
        a.setType(p.type());
        a.setBrand(p.brand());
        a.setModel(p.model());
        a.setSerialNumber(p.serialNumber());
        a.setPurchaseDate(p.purchaseDate());
        a.setPurchaseCost(p.purchaseCost());
        a.setWarrantyExpiry(p.warrantyExpiry());
        a.setLicenceExpiry(p.licenceExpiry());
        a.setLocation(p.location());
        a.setNotes(p.notes());
    }

    private String generateTag() {
        String tag;
        do {
            tag = "AST-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        } while (assets.existsByAssetTag(tag));
        return tag;
    }

    private static boolean contains(String value, String needle) {
        return value != null && value.toLowerCase().contains(needle);
    }
}
