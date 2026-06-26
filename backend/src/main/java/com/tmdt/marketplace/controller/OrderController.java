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
public class OrderController {

    private final MarketplaceService marketplaceService;
    private final MarketplaceModuleService marketplaceModuleService;
    private final RequestGuard requestGuard;

    public OrderController(MarketplaceService marketplaceService, MarketplaceModuleService marketplaceModuleService, RequestGuard requestGuard) {
        this.marketplaceService = marketplaceService;
        this.marketplaceModuleService = marketplaceModuleService;
        this.requestGuard = requestGuard;
    }

    @GetMapping("/orders")
    public List<BuyerOrderSummary> legacyOrders(
            @RequestAttribute(value = "userId", required = false) Long tokenUserId) {
        return marketplaceService.getBuyerOrders(requestGuard.requireUserId(tokenUserId));
    }

    @GetMapping("/v1/checkout/summary")
    public CheckoutSummaryResponse checkoutSummary(
            @RequestAttribute(value = "userId", required = false) Long tokenUserId,
            @RequestParam(required = false) Integer districtId,
            @RequestParam(required = false) String wardCode) {
        return marketplaceService.getCheckoutSummary(requestGuard.requireUserId(tokenUserId), districtId, wardCode);
    }

    @PostMapping("/v1/orders/checkout")
    public CheckoutResponse checkout(
            @RequestAttribute(value = "userId", required = false) Long tokenUserId,
            @RequestBody CheckoutRequest request) {
        return marketplaceService.checkout(requestGuard.requireUserId(tokenUserId), request);
    }

    @GetMapping("/v1/orders")
    public List<BuyerOrderSummary> buyerOrders(
            @RequestAttribute(value = "userId", required = false) Long tokenUserId) {
        return marketplaceService.getBuyerOrders(requestGuard.requireUserId(tokenUserId));
    }

    @GetMapping("/v1/orders/{orderId}")
    public OrderDetailResponse buyerOrderDetail(
            @RequestAttribute(value = "userId", required = false) Long tokenUserId,
            @PathVariable Long orderId) {
        return marketplaceService.getBuyerOrderDetail(requestGuard.requireUserId(tokenUserId), orderId);
    }

    @PutMapping("/v1/orders/{orderId}/cancel")
    public OrderDetailResponse cancelBuyerOrder(
            @RequestAttribute(value = "userId", required = false) Long tokenUserId,
            @PathVariable Long orderId) {
        return marketplaceService.cancelBuyerOrder(requestGuard.requireUserId(tokenUserId), orderId);
    }

    @PutMapping("/v1/orders/{orderId}/shop-orders/{shopOrderId}/cancel")
    public OrderDetailResponse cancelBuyerShopOrder(
            @RequestAttribute(value = "userId", required = false) Long tokenUserId,
            @PathVariable Long orderId,
            @PathVariable Long shopOrderId) {
        return marketplaceService.cancelBuyerShopOrder(requestGuard.requireUserId(tokenUserId), orderId, shopOrderId);
    }

    @PostMapping("/v1/orders/{orderId}/shop-orders/{shopOrderId}/returns")
    public ReturnRequestSummary requestReturn(
            @RequestAttribute(value = "userId", required = false) Long tokenUserId,
            @PathVariable Long orderId,
            @PathVariable Long shopOrderId,
            @RequestBody ReturnRequest request) {
        return marketplaceModuleService.createReturnRequest(requestGuard.requireUserId(tokenUserId), orderId, shopOrderId, request);
    }

    @GetMapping("/v1/orders/returns")
    public List<ReturnRequestSummary> buyerReturns(
            @RequestAttribute(value = "userId", required = false) Long tokenUserId) {
        return marketplaceModuleService.listReturnRequests(requestGuard.requireUserId(tokenUserId), null);
    }
}
