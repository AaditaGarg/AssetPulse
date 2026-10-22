package com.assetpulse.dto;

import com.assetpulse.model.*;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public final class Dtos {
    private Dtos() {}

    // ---- auth / users ----
    public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {}
    public record RegisterRequest(@NotBlank String name, @NotBlank @Email String email,
                                  @NotBlank @Size(min = 8) String password, String department) {}
    public record UserRequest(@NotBlank String name, @NotBlank @Email String email,
                              @Size(min = 8) String password, @NotNull Role role,
                              String department, Long managerId, Boolean active) {}
    public record UserResponse(Long id, String name, String email, Role role, String department,
                               Long managerId, String managerName, boolean active) {}
    public record AuthResponse(String token, UserResponse user) {}

    // ---- assets ----
    public record AssetPayload(@NotBlank String name, @NotNull AssetType type, String brand, String model,
                               String serialNumber, AssetStatus status, LocalDate purchaseDate,
                               BigDecimal purchaseCost, LocalDate warrantyExpiry, LocalDate licenceExpiry,
                               String location, String notes) {}
    public record AssetResponse(Long id, String assetTag, String name, AssetType type, String brand, String model,
                                String serialNumber, AssetStatus status, LocalDate purchaseDate,
                                BigDecimal purchaseCost, LocalDate warrantyExpiry, LocalDate licenceExpiry,
                                String location, String notes, Long assignedToId, String assignedToName,
                                LocalDateTime lastActivityAt) {}
    /** code = asset tag or QR content ("ASSETPULSE:AST-XXXX"). userId only for admin/manager checking out on behalf of someone. */
    public record ScanRequest(@NotBlank String code, Long userId) {}
    public record TransactionResponse(Long id, String assetTag, String assetName, String userName,
                                      TransactionAction action, String note, LocalDateTime timestamp) {}

    // ---- requests / approvals ----
    public record AllocationPayload(@NotNull AssetType assetType, Long assetId, @NotBlank String reason) {}
    public record ReturnPayload(@NotNull Long assetId, String reason) {}
    /** assetId: admin picks the asset to allocate (required when the request has no specific asset). */
    public record DecisionPayload(@NotNull Boolean approve, String comment, Long assetId) {}
    public record RequestResponse(Long id, RequestType type, RequestStatus status, AssetType requestedAssetType,
                                  Long assetId, String assetTag, String assetName, Long requesterId,
                                  String requesterName, String reason, String managerComment, String adminComment,
                                  LocalDateTime createdAt, LocalDateTime updatedAt) {}

    // ---- alerts ----
    public record AlertResponse(Long id, Long assetId, String assetTag, String assetName, AlertType type,
                                String message, LocalDate expiryDate, boolean acknowledged, LocalDateTime createdAt) {}

    // ---- analytics ----
    public record IdleAssetInfo(Long assetId, String assetTag, String name, AssetType type, AssetStatus status,
                                Long assignedToId, String assignedToName, LocalDateTime lastActivityAt,
                                long idleDays, String category) {}
    public record Recommendation(Long requestId, Long requesterId, String requesterName, AssetType requestedType,
                                 String reason, List<IdleAssetInfo> suggestions, String message) {}
    public record TypeUtilisation(AssetType type, long total, long inUse, long idle) {}
    public record DashboardSummary(long totalAssets, Map<String, Long> byStatus, Map<String, Long> byType,
                                   long activeAlerts, long pendingRequests, long warrantiesExpiringSoon,
                                   long licencesExpiringSoon, long idleAssets, double utilisationRate,
                                   BigDecimal totalAssetValue, List<TransactionResponse> recentActivity) {}
}
