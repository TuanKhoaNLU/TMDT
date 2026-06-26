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
public class AdminController {

    private final MarketplaceService marketplaceService;
    private final MarketplaceModuleService marketplaceModuleService;
    private final RequestGuard requestGuard;

    public AdminController(MarketplaceService marketplaceService, MarketplaceModuleService marketplaceModuleService, RequestGuard requestGuard) {
        this.marketplaceService = marketplaceService;
        this.marketplaceModuleService = marketplaceModuleService;
        this.requestGuard = requestGuard;
    }

    @GetMapping("/v1/admin/reviews")
    public List<ReviewModerationSummary> adminReviews(
            @RequestAttribute(value = "role", required = false) String role) {
        requestGuard.requireRole(role, "ADMIN");
        return marketplaceModuleService.listReviews(null);
    }

    @PutMapping("/v1/admin/reviews/{reviewId}/reply")
    public ReviewModerationSummary adminReplyReview(
            @RequestAttribute(value = "role", required = false) String role,
            @PathVariable Long reviewId,
            @RequestBody ReviewReplyRequest request) {
        requestGuard.requireRole(role, "ADMIN");
        return marketplaceModuleService.replyReview(reviewId, request, null);
    }

    @GetMapping("/v1/admin/users")
    public List<AdminUserSummary> adminUsers(
            @RequestAttribute(value = "role", required = false) String role) {
        requestGuard.requireRole(role, "ADMIN");
        return marketplaceModuleService.listAdminUsers();
    }

    @PostMapping("/v1/admin/users")
    public AdminUserSummary createAdminUser(
            @RequestAttribute(value = "role", required = false) String role,
            @RequestBody AdminUserRequest request) {
        requestGuard.requireRole(role, "ADMIN");
        return marketplaceModuleService.createAdminUser(request);
    }

    @PutMapping("/v1/admin/users/{userId}")
    public AdminUserSummary updateAdminUser(
            @RequestAttribute(value = "role", required = false) String role,
            @PathVariable Long userId,
            @RequestBody AdminUserRequest request) {
        requestGuard.requireRole(role, "ADMIN");
        return marketplaceModuleService.updateAdminUser(userId, request);
    }

    @PutMapping("/v1/admin/users/{userId}/verify-seller")
    public AdminUserSummary verifySeller(
            @RequestAttribute(value = "role", required = false) String role,
            @PathVariable Long userId,
            @RequestParam(defaultValue = "true") boolean verified) {
        requestGuard.requireRole(role, "ADMIN");
        return marketplaceModuleService.verifySeller(userId, verified);
    }

    @PostMapping("/v1/admin/categories")
    public CategoryAdminSummary createCategory(
            @RequestAttribute(value = "role", required = false) String role,
            @RequestBody CategoryRequest request) {
        requestGuard.requireRole(role, "ADMIN");
        return marketplaceModuleService.createCategory(request);
    }

    @PutMapping("/v1/admin/categories/{categoryId}")
    public CategoryAdminSummary updateCategory(
            @RequestAttribute(value = "role", required = false) String role,
            @PathVariable Long categoryId,
            @RequestBody CategoryRequest request) {
        requestGuard.requireRole(role, "ADMIN");
        return marketplaceModuleService.updateCategory(categoryId, request);
    }

    @GetMapping("/v1/admin/products")
    public List<ProductAdminSummary> adminProducts(
            @RequestAttribute(value = "role", required = false) String role) {
        requestGuard.requireRole(role, "ADMIN");
        return marketplaceModuleService.listAdminProducts();
    }

    @PutMapping("/v1/admin/products/{productId}/status")
    public ProductAdminSummary moderateProduct(
            @RequestAttribute(value = "role", required = false) String role,
            @PathVariable Long productId,
            @RequestBody StatusRequest request) {
        requestGuard.requireRole(role, "ADMIN");
        return marketplaceModuleService.moderateProduct(productId, request);
    }

    @PostMapping("/v1/admin/vouchers")
    public VoucherSummary createPlatformVoucher(
            @RequestAttribute(value = "role", required = false) String role,
            @RequestBody VoucherRequest request) {
        requestGuard.requireRole(role, "ADMIN");
        return marketplaceModuleService.createVoucher(request, null);
    }

    @PostMapping("/v1/admin/flash-sales")
    public FlashSaleSummary createFlashSale(
            @RequestAttribute(value = "role", required = false) String role,
            @RequestBody FlashSaleRequest request) {
        requestGuard.requireRole(role, "ADMIN");
        return marketplaceModuleService.createFlashSale(request);
    }

    @GetMapping("/v1/admin/reports")
    public List<ReportSummary> adminReports(
            @RequestAttribute(value = "role", required = false) String role) {
        requestGuard.requireRole(role, "ADMIN");
        return marketplaceModuleService.listReports();
    }

    @PutMapping("/v1/admin/reports/{reportId}")
    public ReportSummary updateReport(
            @RequestAttribute(value = "role", required = false) String role,
            @PathVariable Long reportId,
            @RequestBody ReportUpdateRequest request) {
        requestGuard.requireRole(role, "ADMIN");
        return marketplaceModuleService.updateReport(reportId, request);
    }

    @PutMapping("/v1/settings/platform")
    public Map<String, String> updatePlatformSettings(
            @RequestAttribute(value = "role", required = false) String role,
            @RequestBody Map<String, String> request) {
        requestGuard.requireRole(role, "ADMIN");
        return marketplaceModuleService.updatePlatformSettings(request);
    }

    @GetMapping("/v1/admin/payment-reliability")
    public PaymentReliabilityResponse paymentReliability(
            @RequestAttribute(value = "role", required = false) String role) {
        requestGuard.requireRole(role, "ADMIN");
        return marketplaceModuleService.paymentReliability();
    }

    @GetMapping("/v1/admin/orders")
    public List<OrderDetailResponse> adminOrders(
            @RequestAttribute(value = "role", required = false) String role) {
        requestGuard.requireRole(role, "ADMIN");
        return marketplaceService.getAdminOrders();
    }

    @GetMapping("/v1/admin/dashboard")
    public AdminDashboardResponse adminDashboard(
            @RequestAttribute(value = "role", required = false) String role) {
        requestGuard.requireRole(role, "ADMIN");
        return marketplaceService.getAdminDashboard();
    }

    @PutMapping("/v1/admin/orders/{orderId}/cancel")
    public OrderDetailResponse cancelAdminOrder(
            @RequestAttribute(value = "role", required = false) String role,
            @PathVariable Long orderId) {
        requestGuard.requireRole(role, "ADMIN");
        return marketplaceService.cancelAdminOrder(orderId);
    }

    @PutMapping("/v1/admin/orders/{orderId}/shop-orders/{shopOrderId}/cancel")
    public OrderDetailResponse cancelAdminShopOrder(
            @RequestAttribute(value = "role", required = false) String role,
            @PathVariable Long orderId,
            @PathVariable Long shopOrderId) {
        requestGuard.requireRole(role, "ADMIN");
        return marketplaceService.cancelAdminShopOrder(orderId, shopOrderId);
    }

    @GetMapping("/v1/admin/returns")
    public List<ReturnRequestSummary> adminReturns(
            @RequestAttribute(value = "role", required = false) String role) {
        requestGuard.requireRole(role, "ADMIN");
        return marketplaceModuleService.listReturnRequests(null, null);
    }

    @PutMapping("/v1/admin/returns/{returnId}")
    public ReturnRequestSummary updateAdminReturn(
            @RequestAttribute(value = "role", required = false) String role,
            @PathVariable Long returnId,
            @RequestBody ReturnUpdateRequest request) {
        requestGuard.requireRole(role, "ADMIN");
        return marketplaceModuleService.updateReturnRequest(returnId, request, null);
    }
}
