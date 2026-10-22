package com.assetpulse.service;

import com.assetpulse.dto.Dtos.*;
import com.assetpulse.dto.Mapper;
import com.assetpulse.model.*;
import com.assetpulse.repository.AlertRepository;
import com.assetpulse.repository.AssetRepository;
import com.assetpulse.repository.AssetRequestRepository;
import com.assetpulse.repository.AssetTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyticsService {
    private static final List<RequestStatus> OPEN = List.of(RequestStatus.PENDING_MANAGER, RequestStatus.PENDING_ADMIN);

    private final AssetRepository assets;
    private final AssetRequestRepository requests;
    private final AssetTransactionRepository transactions;
    private final AlertRepository alerts;

    @Value("${app.analytics.idle-days:30}") private int defaultIdleDays;
    @Value("${app.alerts.days-before-expiry:30}") private int expiryWindow;

    public DashboardSummary dashboard() {
        List<Asset> active = assets.findAll().stream().filter(a -> a.getStatus() != AssetStatus.RETIRED).toList();
        LocalDate soon = LocalDate.now().plusDays(expiryWindow);

        Map<String, Long> byStatus = assets.findAll().stream()
                .collect(Collectors.groupingBy(a -> a.getStatus().name(), TreeMap::new, Collectors.counting()));
        Map<String, Long> byType = active.stream()
                .collect(Collectors.groupingBy(a -> a.getType().name(), TreeMap::new, Collectors.counting()));

        long inUse = active.stream()
                .filter(a -> a.getStatus() == AssetStatus.ALLOCATED || a.getStatus() == AssetStatus.CHECKED_OUT).count();
        double utilisation = active.isEmpty() ? 0 : Math.round(inUse * 1000.0 / active.size()) / 10.0;

        BigDecimal value = active.stream().map(Asset::getPurchaseCost).filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long warranties = active.stream().filter(a -> a.getWarrantyExpiry() != null && !a.getWarrantyExpiry().isAfter(soon)).count();
        long licences = active.stream().filter(a -> a.getLicenceExpiry() != null && !a.getLicenceExpiry().isAfter(soon)).count();

        List<TransactionResponse> recent = transactions.findTop15ByOrderByTimestampDesc().stream().map(Mapper::toResponse).toList();

        return new DashboardSummary(active.size(), byStatus, byType, alerts.countByAcknowledgedFalse(),
                requests.countByStatusIn(OPEN), warranties, licences, idleAssets(defaultIdleDays).size(),
                utilisation, value, recent);
    }

    /** Assets with no activity for >= days. AVAILABLE => unused stock, ALLOCATED => underused. */
    public List<IdleAssetInfo> idleAssets(Integer days) {
        int threshold = days == null ? defaultIdleDays : days;
        LocalDateTime now = LocalDateTime.now();
        return assets.findAll().stream()
                .filter(a -> a.getStatus() == AssetStatus.AVAILABLE || a.getStatus() == AssetStatus.ALLOCATED)
                .map(a -> toIdleInfo(a, now))
                .filter(i -> i.idleDays() >= threshold)
                .sorted(Comparator.comparingLong(IdleAssetInfo::idleDays).reversed())
                .toList();
    }

    /** For every open allocation request, suggest idle / underused assets of the same type. */
    public List<Recommendation> recommendations(Integer days) {
        List<IdleAssetInfo> idle = idleAssets(days);
        Set<Long> alreadySuggested = new HashSet<>();
        List<Recommendation> result = new ArrayList<>();

        for (AssetRequest r : requests.findByTypeAndStatusIn(RequestType.ALLOCATION, OPEN)) {
            User requester = r.getRequester();
            List<IdleAssetInfo> candidates = idle.stream()
                    .filter(i -> i.type() == r.getRequestedAssetType())
                    .filter(i -> !alreadySuggested.contains(i.assetId()))
                    .filter(i -> i.assignedToId() == null || !i.assignedToId().equals(requester.getId()))
                    .sorted(Comparator.comparing((IdleAssetInfo i) -> i.status() != AssetStatus.AVAILABLE)
                            .thenComparing(Comparator.comparingLong(IdleAssetInfo::idleDays).reversed()))
                    .limit(3).toList();

            String message;
            if (candidates.isEmpty()) {
                message = "No idle " + r.getRequestedAssetType() + " available. Procurement may be required.";
            } else {
                IdleAssetInfo best = candidates.get(0);
                alreadySuggested.add(best.assetId());
                message = best.status() == AssetStatus.AVAILABLE
                        ? "Assign idle stock %s (%s) - unused for %d days - instead of buying new.".formatted(best.name(), best.assetTag(), best.idleDays())
                        : "Reassign underused %s (%s) from %s - no activity for %d days.".formatted(best.name(), best.assetTag(), best.assignedToName(), best.idleDays());
            }
            result.add(new Recommendation(r.getId(), requester.getId(), requester.getName(),
                    r.getRequestedAssetType(), r.getReason(), candidates, message));
        }
        return result;
    }

    public List<TypeUtilisation> utilisationByType(Integer days) {
        Set<Long> idleIds = idleAssets(days).stream().map(IdleAssetInfo::assetId).collect(Collectors.toSet());
        Map<AssetType, List<Asset>> grouped = assets.findAll().stream()
                .filter(a -> a.getStatus() != AssetStatus.RETIRED)
                .collect(Collectors.groupingBy(Asset::getType));
        return grouped.entrySet().stream()
                .map(e -> new TypeUtilisation(e.getKey(), e.getValue().size(),
                        e.getValue().stream().filter(a -> a.getStatus() == AssetStatus.ALLOCATED || a.getStatus() == AssetStatus.CHECKED_OUT).count(),
                        e.getValue().stream().filter(a -> idleIds.contains(a.getId())).count()))
                .sorted(Comparator.comparing((TypeUtilisation t) -> t.type().name()))
                .toList();
    }

    private IdleAssetInfo toIdleInfo(Asset a, LocalDateTime now) {
        LocalDateTime last = a.getLastActivityAt() != null ? a.getLastActivityAt() : a.getCreatedAt();
        long idle = last == null ? 0 : Duration.between(last, now).toDays();
        User holder = a.getAssignedTo();
        return new IdleAssetInfo(a.getId(), a.getAssetTag(), a.getName(), a.getType(), a.getStatus(),
                holder == null ? null : holder.getId(), holder == null ? null : holder.getName(),
                last, idle, a.getStatus() == AssetStatus.AVAILABLE ? "UNUSED_IN_STOCK" : "UNDERUSED_ALLOCATED");
    }
}
