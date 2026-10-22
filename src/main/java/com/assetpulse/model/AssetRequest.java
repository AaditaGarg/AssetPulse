package com.assetpulse.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "asset_requests")
@Getter @Setter @NoArgsConstructor
public class AssetRequest {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RequestType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RequestStatus status;

    @Enumerated(EnumType.STRING)
    private AssetType requestedAssetType;

    /** Specific asset (optional for allocation, mandatory for return). */
    @ManyToOne
    @JoinColumn(name = "asset_id")
    private Asset asset;

    @ManyToOne(optional = false)
    @JoinColumn(name = "requester_id")
    private User requester;

    @Column(length = 1000)
    private String reason;

    @Column(length = 1000)
    private String managerComment;

    @Column(length = 1000)
    private String adminComment;

    @ManyToOne @JoinColumn(name = "manager_reviewer_id")
    private User managerReviewer;

    @ManyToOne @JoinColumn(name = "admin_reviewer_id")
    private User adminReviewer;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() { createdAt = LocalDateTime.now(); updatedAt = createdAt; }

    @PreUpdate
    void onUpdate() { updatedAt = LocalDateTime.now(); }
}
