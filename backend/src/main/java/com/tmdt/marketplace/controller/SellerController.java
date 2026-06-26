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
public class SellerController {

    private final MarketplaceService marketplaceService;
    private final MarketplaceModuleService marketplaceModuleService;
    private final RequestGuard requestGuard;

    public SellerController(MarketplaceService marketplaceService, MarketplaceModuleService marketplaceModuleService, RequestGuard requestGuard) {
        this.marketplaceService = marketplaceService;
        this.marketplaceModuleService = marketplaceModuleService;
        this.requestGuard = requestGuard;
    }

    @PostMapping("/v1/seller/questions/{questionId}/answer")
    public QuestionSummary answerQuestion(
            @RequestHeader(value = "X-Shop-Id", required = false) Long shopId,
            @RequestAttribute(value = "shopId", required = false) Long tokenShopId,
            @RequestAttribute(value = "userId", required = false) Long tokenUserId,
            @RequestAttribute(value = "role", required = false) String role,
            @PathVariable Long questionId,
            @RequestBody QuestionAnswerRequest request) {
        Long sellerShopId = requestGuard.requireSellerShopId(shopId, tokenShopId, tokenUserId, role);
        return marketplaceService.answerQuestion(sellerShopId, questionId, request);
    }

    @GetMapping("/v1/seller/products")
    public List<ProductAdminSummary> sellerProducts(
            @RequestHeader(value = "X-Shop-Id", required = false) Long shopId,
            @RequestAttribute(value = "shopId", required = false) Long tokenShopId,
            @RequestAttribute(value = "userId", required = false) Long tokenUserId,
            @RequestAttribute(value = "role", required = false) String role) {
        return marketplaceModuleService.listSellerProducts(requestGuard.requireSellerShopId(shopId, tokenShopId, tokenUserId, role));
    }

    @PostMapping("/v1/seller/products")
    public ProductAdminSummary createSellerProduct(
            @RequestHeader(value = "X-Shop-Id", required = false) Long shopId,
            @RequestAttribute(value = "shopId", required = false) Long tokenShopId,
            @RequestAttribute(value = "userId", required = false) Long tokenUserId,
            @RequestAttribute(value = "role", required = false) String role,
            @RequestBody ProductWriteRequest request) {
        return marketplaceModuleService.createSellerProduct(requestGuard.requireSellerShopId(shopId, tokenShopId, tokenUserId, role), request);
    }

    @PutMapping("/v1/seller/products/{productId}")
    public ProductAdminSummary updateSellerProduct(
            @RequestHeader(value = "X-Shop-Id", required = false) Long shopId,
            @RequestAttribute(value = "shopId", required = false) Long tokenShopId,
            @RequestAttribute(value = "userId", required = false) Long tokenUserId,
            @RequestAttribute(value = "role", required = false) String role,
            @PathVariable Long productId,
            @RequestBody ProductWriteRequest request) {
        return marketplaceModuleService.updateSellerProduct(requestGuard.requireSellerShopId(shopId, tokenShopId, tokenUserId, role), productId, request);
    }

    @PostMapping("/v1/seller/vouchers")
    public VoucherSummary createSellerVoucher(
            @RequestHeader(value = "X-Shop-Id", required = false) Long shopId,
            @RequestAttribute(value = "shopId", required = false) Long tokenShopId,
            @RequestAttribute(value = "userId", required = false) Long tokenUserId,
            @RequestAttribute(value = "role", required = false) String role,
            @RequestBody VoucherRequest request) {
        return marketplaceModuleService.createVoucher(request, requestGuard.requireSellerShopId(shopId, tokenShopId, tokenUserId, role));
    }

    @GetMapping("/v1/shipping-profiles")
    public List<ShippingProfileSummary> shippingProfiles(
            @RequestHeader(value = "X-Shop-Id", required = false) Long shopId,
            @RequestAttribute(value = "shopId", required = false) Long tokenShopId,
            @RequestAttribute(value = "userId", required = false) Long tokenUserId,
            @RequestAttribute(value = "role", required = false) String role) {
        return marketplaceModuleService.listShippingProfiles(requestGuard.requireSellerShopId(shopId, tokenShopId, tokenUserId, role));
    }

    @GetMapping("/v1/seller/reviews")
    public List<ReviewModerationSummary> sellerReviews(
            @RequestHeader(value = "X-Shop-Id", required = false) Long shopId,
            @RequestAttribute(value = "shopId", required = false) Long tokenShopId,
            @RequestAttribute(value = "userId", required = false) Long tokenUserId,
            @RequestAttribute(value = "role", required = false) String role) {
        return marketplaceModuleService.listReviews(requestGuard.requireSellerShopId(shopId, tokenShopId, tokenUserId, role));
    }

    @PutMapping("/v1/seller/reviews/{reviewId}/reply")
    public ReviewModerationSummary sellerReplyReview(
            @RequestHeader(value = "X-Shop-Id", required = false) Long shopId,
            @RequestAttribute(value = "shopId", required = false) Long tokenShopId,
            @RequestAttribute(value = "userId", required = false) Long tokenUserId,
            @RequestAttribute(value = "role", required = false) String role,
            @PathVariable Long reviewId,
            @RequestBody ReviewReplyRequest request) {
        return marketplaceModuleService.replyReview(reviewId, request, requestGuard.requireSellerShopId(shopId, tokenShopId, tokenUserId, role));
    }

    @PostMapping("/v1/seller/custom-orders")
    public CustomOrderSummary createCustomOrder(
            @RequestHeader(value = "X-Shop-Id", required = false) Long shopId,
            @RequestAttribute(value = "shopId", required = false) Long tokenShopId,
            @RequestAttribute(value = "userId", required = false) Long tokenUserId,
            @RequestAttribute(value = "role", required = false) String role,
            @RequestBody CustomOrderRequest request) {
        return marketplaceModuleService.createCustomOrder(requestGuard.requireUserId(tokenUserId),
                requestGuard.requireSellerShopId(shopId, tokenShopId, tokenUserId, role), request);
    }

    @GetMapping("/v1/analytics/seller/summary")
    public SellerAnalyticsResponse sellerAnalytics(
            @RequestHeader(value = "X-Shop-Id", required = false) Long shopId,
            @RequestAttribute(value = "shopId", required = false) Long tokenShopId,
            @RequestAttribute(value = "userId", required = false) Long tokenUserId,
            @RequestAttribute(value = "role", required = false) String role) {
        return marketplaceModuleService.sellerAnalytics(requestGuard.requireSellerShopId(shopId, tokenShopId, tokenUserId, role));
    }

    @GetMapping("/v1/seller/orders")
    public List<SellerOrderResponse> sellerOrders(
            @RequestHeader(value = "X-Shop-Id", required = false) Long shopId,
            @RequestAttribute(value = "shopId", required = false) Long tokenShopId,
            @RequestAttribute(value = "userId", required = false) Long tokenUserId,
            @RequestAttribute(value = "role", required = false) String role) {
        return marketplaceService.getSellerOrders(requestGuard.requireSellerShopId(shopId, tokenShopId, tokenUserId, role));
    }

    @GetMapping("/v1/seller/dashboard")
    public SellerDashboardResponse sellerDashboard(
            @RequestHeader(value = "X-Shop-Id", required = false) Long shopId,
            @RequestAttribute(value = "shopId", required = false) Long tokenShopId,
            @RequestAttribute(value = "userId", required = false) Long tokenUserId,
            @RequestAttribute(value = "role", required = false) String role) {
        return marketplaceService.getSellerDashboard(requestGuard.requireSellerShopId(shopId, tokenShopId, tokenUserId, role));
    }

    @PutMapping("/v1/seller/orders/{shopOrderId}/status")
    public SellerOrderResponse updateSellerOrderStatus(
            @RequestHeader(value = "X-Shop-Id", required = false) Long shopId,
            @RequestAttribute(value = "shopId", required = false) Long tokenShopId,
            @RequestAttribute(value = "userId", required = false) Long tokenUserId,
            @RequestAttribute(value = "role", required = false) String role,
            @PathVariable Long shopOrderId,
            @RequestBody SellerStatusUpdateRequest request) {
        return marketplaceService.updateSellerOrderStatus(requestGuard.requireSellerShopId(shopId, tokenShopId, tokenUserId, role), shopOrderId, request);
    }

    @PostMapping("/v1/seller/orders/{shopOrderId}/shipments/ghn")
    public SellerOrderResponse createGhnShipment(
            @RequestHeader(value = "X-Shop-Id", required = false) Long shopId,
            @RequestAttribute(value = "shopId", required = false) Long tokenShopId,
            @RequestAttribute(value = "userId", required = false) Long tokenUserId,
            @RequestAttribute(value = "role", required = false) String role,
            @PathVariable Long shopOrderId,
            @RequestBody SellerShipmentRequest request) {
        return marketplaceService.createGhnShipment(requestGuard.requireSellerShopId(shopId, tokenShopId, tokenUserId, role), shopOrderId, request);
    }

    @GetMapping("/v1/seller/returns")
    public List<ReturnRequestSummary> sellerReturns(
            @RequestHeader(value = "X-Shop-Id", required = false) Long shopId,
            @RequestAttribute(value = "shopId", required = false) Long tokenShopId,
            @RequestAttribute(value = "userId", required = false) Long tokenUserId,
            @RequestAttribute(value = "role", required = false) String role) {
        return marketplaceModuleService.listReturnRequests(null, requestGuard.requireSellerShopId(shopId, tokenShopId, tokenUserId, role));
    }

    @PutMapping("/v1/seller/returns/{returnId}")
    public ReturnRequestSummary updateSellerReturn(
            @RequestHeader(value = "X-Shop-Id", required = false) Long shopId,
            @RequestAttribute(value = "shopId", required = false) Long tokenShopId,
            @RequestAttribute(value = "userId", required = false) Long tokenUserId,
            @RequestAttribute(value = "role", required = false) String role,
            @PathVariable Long returnId,
            @RequestBody ReturnUpdateRequest request) {
        Long sellerShopId = requestGuard.requireSellerShopId(shopId, tokenShopId, tokenUserId, role);
        return marketplaceModuleService.updateReturnRequest(returnId, request, sellerShopId);
    }
}
