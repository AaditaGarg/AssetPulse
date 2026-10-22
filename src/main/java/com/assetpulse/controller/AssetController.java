package com.assetpulse.controller;

import com.assetpulse.dto.Dtos.AssetPayload;
import com.assetpulse.dto.Dtos.AssetResponse;
import com.assetpulse.dto.Dtos.TransactionResponse;
import com.assetpulse.dto.Mapper;
import com.assetpulse.model.Asset;
import com.assetpulse.model.AssetStatus;
import com.assetpulse.model.AssetType;
import com.assetpulse.model.User;
import com.assetpulse.service.AssetService;
import com.assetpulse.service.QrCodeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/assets")
@RequiredArgsConstructor
public class AssetController {
    private final AssetService service;
    private final QrCodeService qrCodeService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public List<AssetResponse> list(@RequestParam(required = false) AssetStatus status,
                                    @RequestParam(required = false) AssetType type,
                                    @RequestParam(required = false) String q) {
        return service.search(status, type, q).stream().map(Mapper::toResponse).toList();
    }

    /** Assets any logged-in user can request. */
    @GetMapping("/available")
    public List<AssetResponse> available(@RequestParam(required = false) AssetType type) {
        return service.search(AssetStatus.AVAILABLE, type, null).stream().map(Mapper::toResponse).toList();
    }

    /** Assets currently allocated / checked out to the logged-in user. */
    @GetMapping("/mine")
    public List<AssetResponse> mine(@AuthenticationPrincipal User me) {
        return service.assignedTo(me).stream().map(Mapper::toResponse).toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public AssetResponse get(@PathVariable Long id) {
        return Mapper.toResponse(service.get(id));
    }

    @GetMapping("/{id}/history")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public List<TransactionResponse> history(@PathVariable Long id) {
        return service.history(id).stream().map(Mapper::toResponse).toList();
    }

    /** PNG QR code. Encodes "ASSETPULSE:<assetTag>". */
    @GetMapping(value = "/{id}/qr", produces = MediaType.IMAGE_PNG_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<byte[]> qr(@PathVariable Long id, @RequestParam(defaultValue = "300") int size) {
        Asset a = service.get(id);
        int s = Math.min(Math.max(size, 100), 1000);
        return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG)
                .body(qrCodeService.generatePng(AssetService.QR_PREFIX + a.getAssetTag(), s));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public AssetResponse create(@Valid @RequestBody AssetPayload payload) {
        return Mapper.toResponse(service.create(payload));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public AssetResponse update(@PathVariable Long id, @Valid @RequestBody AssetPayload payload) {
        return Mapper.toResponse(service.update(id, payload));
    }

    /** Soft delete - marks the asset RETIRED. */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public AssetResponse retire(@PathVariable Long id) {
        return Mapper.toResponse(service.retire(id));
    }
}
