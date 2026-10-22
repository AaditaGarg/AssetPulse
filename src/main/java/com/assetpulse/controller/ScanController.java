package com.assetpulse.controller;

import com.assetpulse.dto.Dtos.AssetResponse;
import com.assetpulse.dto.Dtos.ScanRequest;
import com.assetpulse.dto.Mapper;
import com.assetpulse.model.User;
import com.assetpulse.service.AssetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** Endpoints called after the frontend decodes a QR code. */
@RestController
@RequestMapping("/api/scan")
@RequiredArgsConstructor
public class ScanController {
    private final AssetService service;

    @GetMapping("/lookup")
    public AssetResponse lookup(@RequestParam String code) {
        return Mapper.toResponse(service.getByCode(code));
    }

    @PostMapping("/check-out")
    public AssetResponse checkOut(@Valid @RequestBody ScanRequest req, @AuthenticationPrincipal User me) {
        return Mapper.toResponse(service.checkOut(req.code(), me, req.userId()));
    }

    @PostMapping("/check-in")
    public AssetResponse checkIn(@Valid @RequestBody ScanRequest req, @AuthenticationPrincipal User me) {
        return Mapper.toResponse(service.checkIn(req.code(), me));
    }
}
