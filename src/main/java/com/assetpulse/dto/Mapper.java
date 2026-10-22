package com.assetpulse.dto;

import com.assetpulse.dto.Dtos.*;
import com.assetpulse.model.*;

public final class Mapper {
    private Mapper() {}

    public static UserResponse toResponse(User u) {
        return new UserResponse(u.getId(), u.getName(), u.getEmail(), u.getRole(), u.getDepartment(),
                u.getManager() == null ? null : u.getManager().getId(),
                u.getManager() == null ? null : u.getManager().getName(),
                u.isActive());
    }

    public static AssetResponse toResponse(Asset a) {
        User holder = a.getAssignedTo();
        return new AssetResponse(a.getId(), a.getAssetTag(), a.getName(), a.getType(), a.getBrand(), a.getModel(),
                a.getSerialNumber(), a.getStatus(), a.getPurchaseDate(), a.getPurchaseCost(),
                a.getWarrantyExpiry(), a.getLicenceExpiry(), a.getLocation(), a.getNotes(),
                holder == null ? null : holder.getId(), holder == null ? null : holder.getName(),
                a.getLastActivityAt());
    }

    public static RequestResponse toResponse(AssetRequest r) {
        Asset a = r.getAsset();
        return new RequestResponse(r.getId(), r.getType(), r.getStatus(), r.getRequestedAssetType(),
                a == null ? null : a.getId(), a == null ? null : a.getAssetTag(), a == null ? null : a.getName(),
                r.getRequester().getId(), r.getRequester().getName(), r.getReason(),
                r.getManagerComment(), r.getAdminComment(), r.getCreatedAt(), r.getUpdatedAt());
    }

    public static AlertResponse toResponse(Alert a) {
        return new AlertResponse(a.getId(), a.getAsset().getId(), a.getAsset().getAssetTag(), a.getAsset().getName(),
                a.getType(), a.getMessage(), a.getExpiryDate(), a.isAcknowledged(), a.getCreatedAt());
    }

    public static TransactionResponse toResponse(AssetTransaction t) {
        return new TransactionResponse(t.getId(), t.getAsset().getAssetTag(), t.getAsset().getName(),
                t.getUser() == null ? null : t.getUser().getName(), t.getAction(), t.getNote(), t.getTimestamp());
    }
}
