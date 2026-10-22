package com.assetpulse.controller;

import com.assetpulse.dto.Dtos.*;
import com.assetpulse.dto.Mapper;
import com.assetpulse.model.RequestStatus;
import com.assetpulse.model.User;
import com.assetpulse.service.RequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/requests")
@RequiredArgsConstructor
public class RequestController {
    private final RequestService service;

    @PostMapping("/allocation")
    @ResponseStatus(HttpStatus.CREATED)
    public RequestResponse requestAllocation(@Valid @RequestBody AllocationPayload p, @AuthenticationPrincipal User me) {
        return Mapper.toResponse(service.createAllocation(me, p));
    }

    @PostMapping("/return")
    @ResponseStatus(HttpStatus.CREATED)
    public RequestResponse requestReturn(@Valid @RequestBody ReturnPayload p, @AuthenticationPrincipal User me) {
        return Mapper.toResponse(service.createReturn(me, p));
    }

    @GetMapping("/mine")
    public List<RequestResponse> mine(@AuthenticationPrincipal User me) {
        return service.mine(me).stream().map(Mapper::toResponse).toList();
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<RequestResponse> all(@RequestParam(required = false) RequestStatus status) {
        return service.all(status).stream().map(Mapper::toResponse).toList();
    }

    @GetMapping("/pending/manager")
    @PreAuthorize("hasRole('MANAGER')")
    public List<RequestResponse> pendingForManager(@AuthenticationPrincipal User me) {
        return service.pendingForManager(me).stream().map(Mapper::toResponse).toList();
    }

    @GetMapping("/pending/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public List<RequestResponse> pendingForAdmin() {
        return service.pendingForAdmin().stream().map(Mapper::toResponse).toList();
    }

    @PostMapping("/{id}/manager-decision")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public RequestResponse managerDecision(@PathVariable Long id, @Valid @RequestBody DecisionPayload p,
                                           @AuthenticationPrincipal User me) {
        return Mapper.toResponse(service.managerDecision(id, p, me));
    }

    @PostMapping("/{id}/admin-decision")
    @PreAuthorize("hasRole('ADMIN')")
    public RequestResponse adminDecision(@PathVariable Long id, @Valid @RequestBody DecisionPayload p,
                                         @AuthenticationPrincipal User me) {
        return Mapper.toResponse(service.adminDecision(id, p, me));
    }

    @PostMapping("/{id}/cancel")
    public RequestResponse cancel(@PathVariable Long id, @AuthenticationPrincipal User me) {
        return Mapper.toResponse(service.cancel(id, me));
    }
}
