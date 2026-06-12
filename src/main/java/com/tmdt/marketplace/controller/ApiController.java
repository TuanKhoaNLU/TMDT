package com.tmdt.marketplace.controller;

import com.tmdt.marketplace.service.MarketplaceService;
import com.tmdt.marketplace.service.MarketplaceService.BuyerOrderSummary;
import com.tmdt.marketplace.service.MarketplaceService.CartItemRequest;
import com.tmdt.marketplace.service.MarketplaceService.CartResponse;
import com.tmdt.marketplace.service.MarketplaceService.CheckoutRequest;
import com.tmdt.marketplace.service.MarketplaceService.CheckoutResponse;
import com.tmdt.marketplace.service.MarketplaceService.CheckoutSummaryResponse;
import com.tmdt.marketplace.service.MarketplaceService.MasterDataOption;
import com.tmdt.marketplace.service.MarketplaceService.OrderDetailResponse;
import com.tmdt.marketplace.service.MarketplaceService.PaymentCreateRequest;
import com.tmdt.marketplace.service.MarketplaceService.PaymentCreateResponse;
import com.tmdt.marketplace.service.MarketplaceService.PaymentReturnResponse;
import com.tmdt.marketplace.service.MarketplaceService.ProductCard;
import com.tmdt.marketplace.service.MarketplaceService.SellerOrderResponse;
import com.tmdt.marketplace.service.MarketplaceService.SellerShipmentRequest;
import com.tmdt.marketplace.service.MarketplaceService.SellerStatusUpdateRequest;
import com.tmdt.marketplace.service.MarketplaceService.ShippingFeeRequest;
import com.tmdt.marketplace.service.MarketplaceService.ShippingFeeResponse;
import com.tmdt.marketplace.service.MarketplaceService.UpdateCartItemRequest;
import org.springframework.web.bind.annotation.*;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final MarketplaceService marketplaceService;

    public ApiController(MarketplaceService marketplaceService) {
        this.marketplaceService = marketplaceService;
    }

    private Long requireUserId(Long tokenUserId) {
        if (tokenUserId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Vui long dang nhap de tiep tuc");
        }
        return tokenUserId;
    }

    private void requireRole(String actualRole, String expectedRole) {
        if (actualRole == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Vui long dang nhap de tiep tuc");
        }
        if (!expectedRole.equalsIgnoreCase(actualRole)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tai khoan khong co quyen thuc hien thao tac nay");
        }
    }

    private Long requireSellerShopId(Long requestedShopId, Long tokenShopId, Long tokenUserId, String role) {
        Long userId = requireUserId(tokenUserId);
        requireRole(role, "SELLER");
        Long shopId = requestedShopId != null ? requestedShopId : tokenShopId;
        if (shopId == null || !marketplaceService.userOwnsShop(userId, shopId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tai khoan khong quan ly shop nay");
        }
        return shopId;
    }

    @GetMapping({"/products", "/v1/products"})
    public List<ProductCard> products() {
        return marketplaceService.getProducts();
    }

    @GetMapping("/orders")
    public List<BuyerOrderSummary> legacyOrders(
            @RequestAttribute(value = "userId", required = false) Long tokenUserId) {
        return marketplaceService.getBuyerOrders(requireUserId(tokenUserId));
    }

    @GetMapping("/v1/cart")
    public CartResponse cart(
            @RequestAttribute(value = "userId", required = false) Long tokenUserId) {
        if (tokenUserId == null) {
            return new CartResponse(0L, List.of(), BigDecimal.ZERO, 0, false);
        }
        return marketplaceService.getCart(tokenUserId);
    }

    @PostMapping("/v1/cart/items")
    public CartResponse addCartItem(
            @RequestAttribute(value = "userId", required = false) Long tokenUserId,
            @RequestBody CartItemRequest request) {
        return marketplaceService.addCartItem(requireUserId(tokenUserId), request);
    }

    @PutMapping("/v1/cart/items/{itemKey}")
    public CartResponse updateCartItem(
            @RequestAttribute(value = "userId", required = false) Long tokenUserId,
            @PathVariable String itemKey,
            @RequestBody UpdateCartItemRequest request) {
        return marketplaceService.updateCartItem(requireUserId(tokenUserId), itemKey, request);
    }

    @DeleteMapping("/v1/cart/items/{itemKey}")
    public CartResponse deleteCartItem(
            @RequestAttribute(value = "userId", required = false) Long tokenUserId,
            @PathVariable String itemKey) {
        return marketplaceService.deleteCartItem(requireUserId(tokenUserId), itemKey);
    }

    @DeleteMapping("/v1/cart")
    public CartResponse clearCart(
            @RequestAttribute(value = "userId", required = false) Long tokenUserId) {
        return marketplaceService.clearCart(requireUserId(tokenUserId));
    }

    @GetMapping("/v1/checkout/summary")
    public CheckoutSummaryResponse checkoutSummary(
            @RequestAttribute(value = "userId", required = false) Long tokenUserId,
            @RequestParam(required = false) Integer districtId,
            @RequestParam(required = false) String wardCode) {
        return marketplaceService.getCheckoutSummary(requireUserId(tokenUserId), districtId, wardCode);
    }

    @PostMapping("/v1/orders/checkout")
    public CheckoutResponse checkout(
            @RequestAttribute(value = "userId", required = false) Long tokenUserId,
            @RequestBody CheckoutRequest request) {
        return marketplaceService.checkout(requireUserId(tokenUserId), request);
    }

    @GetMapping("/v1/orders")
    public List<BuyerOrderSummary> buyerOrders(
            @RequestAttribute(value = "userId", required = false) Long tokenUserId) {
        return marketplaceService.getBuyerOrders(requireUserId(tokenUserId));
    }

    @GetMapping("/v1/orders/{orderId}")
    public OrderDetailResponse buyerOrderDetail(
            @RequestAttribute(value = "userId", required = false) Long tokenUserId,
            @PathVariable Long orderId) {
        return marketplaceService.getBuyerOrderDetail(requireUserId(tokenUserId), orderId);
    }

    @PutMapping("/v1/orders/{orderId}/cancel")
    public OrderDetailResponse cancelBuyerOrder(
            @RequestAttribute(value = "userId", required = false) Long tokenUserId,
            @PathVariable Long orderId) {
        return marketplaceService.cancelBuyerOrder(requireUserId(tokenUserId), orderId);
    }

    @PutMapping("/v1/orders/{orderId}/shop-orders/{shopOrderId}/cancel")
    public OrderDetailResponse cancelBuyerShopOrder(
            @RequestAttribute(value = "userId", required = false) Long tokenUserId,
            @PathVariable Long orderId,
            @PathVariable Long shopOrderId) {
        return marketplaceService.cancelBuyerShopOrder(requireUserId(tokenUserId), orderId, shopOrderId);
    }

    @PostMapping("/v1/payments/vnpay/create")
    public PaymentCreateResponse createVnpayPayment(
            @RequestAttribute(value = "userId", required = false) Long tokenUserId,
            @RequestBody PaymentCreateRequest request) {
        return marketplaceService.createVnpayPayment(requireUserId(tokenUserId), request);
    }

    @GetMapping("/v1/payments/vnpay-return")
    public PaymentReturnResponse vnpayReturn(@RequestParam Map<String, String> params) {
        return marketplaceService.verifyVnpayReturn(params);
    }

    @PostMapping("/v1/shipping/fee")
    public ShippingFeeResponse shippingFee(@RequestBody ShippingFeeRequest request) {
        return marketplaceService.calculateShippingFee(request);
    }

    @GetMapping("/v1/shipping/provinces")
    public List<MasterDataOption> provinces() {
        return marketplaceService.getProvinces();
    }

    @GetMapping("/v1/shipping/districts")
    public List<MasterDataOption> districts(@RequestParam(required = false) Integer provinceId) {
        return marketplaceService.getDistricts(provinceId);
    }

    @GetMapping("/v1/shipping/wards")
    public List<MasterDataOption> wards(@RequestParam(required = false) Integer districtId) {
        return marketplaceService.getWards(districtId);
    }

    @GetMapping("/v1/seller/orders")
    public List<SellerOrderResponse> sellerOrders(
            @RequestHeader(value = "X-Shop-Id", required = false) Long shopId,
            @RequestAttribute(value = "shopId", required = false) Long tokenShopId,
            @RequestAttribute(value = "userId", required = false) Long tokenUserId,
            @RequestAttribute(value = "role", required = false) String role) {
        return marketplaceService.getSellerOrders(requireSellerShopId(shopId, tokenShopId, tokenUserId, role));
    }

    @PutMapping("/v1/seller/orders/{shopOrderId}/status")
    public SellerOrderResponse updateSellerOrderStatus(
            @RequestHeader(value = "X-Shop-Id", required = false) Long shopId,
            @RequestAttribute(value = "shopId", required = false) Long tokenShopId,
            @RequestAttribute(value = "userId", required = false) Long tokenUserId,
            @RequestAttribute(value = "role", required = false) String role,
            @PathVariable Long shopOrderId,
            @RequestBody SellerStatusUpdateRequest request) {
        return marketplaceService.updateSellerOrderStatus(requireSellerShopId(shopId, tokenShopId, tokenUserId, role), shopOrderId, request);
    }

    @PostMapping("/v1/seller/orders/{shopOrderId}/shipments/ghn")
    public SellerOrderResponse createGhnShipment(
            @RequestHeader(value = "X-Shop-Id", required = false) Long shopId,
            @RequestAttribute(value = "shopId", required = false) Long tokenShopId,
            @RequestAttribute(value = "userId", required = false) Long tokenUserId,
            @RequestAttribute(value = "role", required = false) String role,
            @PathVariable Long shopOrderId,
            @RequestBody SellerShipmentRequest request) {
        return marketplaceService.createGhnShipment(requireSellerShopId(shopId, tokenShopId, tokenUserId, role), shopOrderId, request);
    }

    @GetMapping("/v1/admin/orders")
    public List<OrderDetailResponse> adminOrders(
            @RequestAttribute(value = "role", required = false) String role) {
        requireRole(role, "ADMIN");
        return marketplaceService.getAdminOrders();
    }

    @PutMapping("/v1/admin/orders/{orderId}/cancel")
    public OrderDetailResponse cancelAdminOrder(
            @RequestAttribute(value = "role", required = false) String role,
            @PathVariable Long orderId) {
        requireRole(role, "ADMIN");
        return marketplaceService.cancelAdminOrder(orderId);
    }

    @PutMapping("/v1/admin/orders/{orderId}/shop-orders/{shopOrderId}/cancel")
    public OrderDetailResponse cancelAdminShopOrder(
            @RequestAttribute(value = "role", required = false) String role,
            @PathVariable Long orderId,
            @PathVariable Long shopOrderId) {
        requireRole(role, "ADMIN");
        return marketplaceService.cancelAdminShopOrder(orderId, shopOrderId);
    }
}
