package com.tmdt.marketplace.controller;

import com.tmdt.marketplace.service.MarketplaceModuleService;
import com.tmdt.marketplace.service.MarketplaceModuleService.*;
import com.tmdt.marketplace.service.MarketplaceService;
import com.tmdt.marketplace.service.MarketplaceService.*;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class CustomOrderController {

    private final MarketplaceModuleService marketplaceModuleService;
    private final RequestGuard requestGuard;

    public CustomOrderController(MarketplaceModuleService marketplaceModuleService, RequestGuard requestGuard) {
        this.marketplaceModuleService = marketplaceModuleService;
        this.requestGuard = requestGuard;
    }

    @GetMapping("/v1/custom-orders")
    public List<CustomOrderSummary> customOrders(
            @RequestHeader(value = "X-Shop-Id", required = false) Long shopId,
            @RequestAttribute(value = "shopId", required = false) Long tokenShopId,
            @RequestAttribute(value = "userId", required = false) Long tokenUserId,
            @RequestAttribute(value = "role", required = false) String role) {
        Long userId = requestGuard.requireUserId(tokenUserId);
        Long effectiveShopId = "SELLER".equalsIgnoreCase(role) ? requestGuard.requireSellerShopId(shopId, tokenShopId, tokenUserId, role) : null;
        return marketplaceModuleService.listCustomOrders(effectiveShopId == null ? userId : null, effectiveShopId);
    }

    @PutMapping("/v1/custom-orders/{customOrderId}/status")
    public CustomOrderSummary updateCustomOrderStatus(
            @RequestHeader(value = "X-Shop-Id", required = false) Long shopId,
            @RequestAttribute(value = "shopId", required = false) Long tokenShopId,
            @RequestAttribute(value = "userId", required = false) Long tokenUserId,
            @RequestAttribute(value = "role", required = false) String role,
            @PathVariable Long customOrderId,
            @RequestBody StatusRequest request) {
        Long userId = requestGuard.requireUserId(tokenUserId);
        Long effectiveShopId = "SELLER".equalsIgnoreCase(role) ? requestGuard.requireSellerShopId(shopId, tokenShopId, tokenUserId, role) : null;
        return marketplaceModuleService.updateCustomOrderStatus(customOrderId, request, userId, role, effectiveShopId);
    }

    @GetMapping("/v1/custom-orders/revisions")
    public List<CustomOrderRevisionSummary> customOrderRevisions(
            @RequestHeader(value = "X-Shop-Id", required = false) Long shopId,
            @RequestAttribute(value = "shopId", required = false) Long tokenShopId,
            @RequestAttribute(value = "userId", required = false) Long tokenUserId,
            @RequestAttribute(value = "role", required = false) String role) {
        Long userId = requestGuard.requireUserId(tokenUserId);
        Long effectiveShopId = "SELLER".equalsIgnoreCase(role)
                ? requestGuard.requireSellerShopId(shopId, tokenShopId, tokenUserId, role)
                : null;
        return marketplaceModuleService.listCustomOrderRevisions(userId, role, effectiveShopId);
    }

    @PostMapping("/v1/custom-orders/{customOrderId}/revisions")
    public CustomOrderRevisionSummary requestRevision(
            @RequestAttribute(value = "userId", required = false) Long tokenUserId,
            @RequestAttribute(value = "role", required = false) String role,
            @PathVariable Long customOrderId,
            @RequestBody CustomOrderRevisionRequest request) {
        if ("SELLER".equalsIgnoreCase(role) || "ADMIN".equalsIgnoreCase(role)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.FORBIDDEN, "Chỉ khách hàng được yêu cầu chỉnh sửa.");
        }
        return marketplaceModuleService.createCustomOrderRevision(customOrderId,
                requestGuard.requireUserId(tokenUserId), request);
    }

    @PutMapping("/v1/custom-orders/revisions/{revisionId}")
    public CustomOrderRevisionSummary resolveRevision(
            @RequestHeader(value = "X-Shop-Id", required = false) Long shopId,
            @RequestAttribute(value = "shopId", required = false) Long tokenShopId,
            @RequestAttribute(value = "userId", required = false) Long tokenUserId,
            @RequestAttribute(value = "role", required = false) String role,
            @PathVariable Long revisionId,
            @RequestBody CustomOrderRevisionResolveRequest request) {
        Long sellerShopId = requestGuard.requireSellerShopId(shopId, tokenShopId, tokenUserId, role);
        return marketplaceModuleService.resolveCustomOrderRevision(revisionId,
                requestGuard.requireUserId(tokenUserId), sellerShopId, request);
    }
}
