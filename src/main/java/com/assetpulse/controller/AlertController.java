package com.assetpulse.controller;

import com.assetpulse.dto.Dtos.AlertResponse;
import com.assetpulse.dto.Mapper;
import com.assetpulse.service.AlertService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
public class AlertController {
    private final AlertService service;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public List<AlertResponse> list(@RequestParam(defaultValue = "false") boolean includeAcknowledged) {
        return service.list(includeAcknowledged).stream().map(Mapper::toResponse).toList();
    }

    /** Manually trigger the expiry scan (it also runs daily on a schedule). */
    @PostMapping("/scan")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Integer> scan() {
        return Map.of("newAlerts", service.scan());
    }

    @PostMapping("/{id}/acknowledge")
    @PreAuthorize("hasRole('ADMIN')")
    public AlertResponse acknowledge(@PathVariable Long id) {
        return Mapper.toResponse(service.acknowledge(id));
    }
}
