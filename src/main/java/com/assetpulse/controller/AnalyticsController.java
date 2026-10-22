package com.assetpulse.controller;

import com.assetpulse.dto.Dtos.*;
import com.assetpulse.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/analytics")
@PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
@RequiredArgsConstructor
public class AnalyticsController {
    private final AnalyticsService service;

    @GetMapping("/dashboard")
    public DashboardSummary dashboard() { return service.dashboard(); }

    @GetMapping("/idle-assets")
    public List<IdleAssetInfo> idle(@RequestParam(required = false) Integer days) { return service.idleAssets(days); }

    @GetMapping("/recommendations")
    public List<Recommendation> recommendations(@RequestParam(required = false) Integer days) {
        return service.recommendations(days);
    }

    @GetMapping("/utilisation-by-type")
    public List<TypeUtilisation> utilisation(@RequestParam(required = false) Integer days) {
        return service.utilisationByType(days);
    }
}
