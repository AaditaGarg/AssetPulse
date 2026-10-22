package com.assetpulse.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "asset_transactions")
@Getter @Setter @NoArgsConstructor
public class AssetTransaction {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "asset_id")
    private Asset asset;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionAction action;

    private String note;
    private LocalDateTime timestamp;

    @PrePersist
    void onCreate() { if (timestamp == null) timestamp = LocalDateTime.now(); }
}
