package com.tmdt.marketplace.controller;

import com.tmdt.marketplace.service.AdvancedMarketplaceService;
import com.tmdt.marketplace.service.AdvancedMarketplaceService.AdminUserRequest;
import com.tmdt.marketplace.service.AdvancedMarketplaceService.AdminUserSummary;
import com.tmdt.marketplace.service.AdvancedMarketplaceService.CommissionPostRequest;
import com.tmdt.marketplace.service.AdvancedMarketplaceService.CommissionPostSummary;
import com.tmdt.marketplace.service.AdvancedMarketplaceService.CategoryAdminSummary;
import com.tmdt.marketplace.service.AdvancedMarketplaceService.CategoryRequest;
import com.tmdt.marketplace.service.AdvancedMarketplaceService.ConversationRequest;
import com.tmdt.marketplace.service.AdvancedMarketplaceService.ConversationSummary;
import com.tmdt.marketplace.service.AdvancedMarketplaceService.CustomQuoteRequest;
import com.tmdt.marketplace.service.AdvancedMarketplaceService.CustomOrderRequest;
import com.tmdt.marketplace.service.AdvancedMarketplaceService.CustomOrderSummary;
import com.tmdt.marketplace.service.AdvancedMarketplaceService.FlashSaleRequest;
import com.tmdt.marketplace.service.AdvancedMarketplaceService.FlashSaleSummary;
import com.tmdt.marketplace.service.AdvancedMarketplaceService.GiftWrapTier;
import com.tmdt.marketplace.service.AdvancedMarketplaceService.MarketplaceModulesResponse;
import com.tmdt.marketplace.service.AdvancedMarketplaceService.MediaFolderRequest;
import com.tmdt.marketplace.service.AdvancedMarketplaceService.MediaFolderSummary;
import com.tmdt.marketplace.service.AdvancedMarketplaceService.MediaImageRequest;
import com.tmdt.marketplace.service.AdvancedMarketplaceService.MediaImageSummary;
import com.tmdt.marketplace.service.AdvancedMarketplaceService.MessageRequest;
import com.tmdt.marketplace.service.AdvancedMarketplaceService.MessageSummary;
import com.tmdt.marketplace.service.AdvancedMarketplaceService.PaymentHistorySummary;
import com.tmdt.marketplace.service.AdvancedMarketplaceService.PaymentReliabilityResponse;
import com.tmdt.marketplace.service.AdvancedMarketplaceService.ProposalRequest;
import com.tmdt.marketplace.service.AdvancedMarketplaceService.ProposalSummary;
import com.tmdt.marketplace.service.AdvancedMarketplaceService.ProductAdminSummary;
import com.tmdt.marketplace.service.AdvancedMarketplaceService.ProductWriteRequest;
import com.tmdt.marketplace.service.AdvancedMarketplaceService.ReportRequest;
import com.tmdt.marketplace.service.AdvancedMarketplaceService.ReportSummary;
import com.tmdt.marketplace.service.AdvancedMarketplaceService.ReportUpdateRequest;
import com.tmdt.marketplace.service.AdvancedMarketplaceService.ReviewModerationSummary;
import com.tmdt.marketplace.service.AdvancedMarketplaceService.ReviewReplyRequest;
import com.tmdt.marketplace.service.AdvancedMarketplaceService.SellerAnalyticsResponse;
import com.tmdt.marketplace.service.AdvancedMarketplaceService.ShippingProfileSummary;
import com.tmdt.marketplace.service.AdvancedMarketplaceService.StatusRequest;
import com.tmdt.marketplace.service.AdvancedMarketplaceService.VoucherApplyRequest;
import com.tmdt.marketplace.service.AdvancedMarketplaceService.VoucherApplyResponse;
import com.tmdt.marketplace.service.AdvancedMarketplaceService.VoucherRequest;
import com.tmdt.marketplace.service.AdvancedMarketplaceService.VoucherSummary;
import com.tmdt.marketplace.service.MarketplaceService;
import com.tmdt.marketplace.service.MarketplaceService.BuyerOrderSummary;
import com.tmdt.marketplace.service.MarketplaceService.CartItemRequest;
import com.tmdt.marketplace.service.MarketplaceService.CartResponse;
import com.tmdt.marketplace.service.MarketplaceService.CheckoutRequest;
import com.tmdt.marketplace.service.MarketplaceService.CheckoutResponse;
import com.tmdt.marketplace.service.MarketplaceService.CheckoutSummaryResponse;
import com.tmdt.marketplace.service.MarketplaceService.AdminDashboardResponse;
import com.tmdt.marketplace.service.MarketplaceService.FollowToggleResponse;
import com.tmdt.marketplace.service.MarketplaceService.HomepageResponse;
import com.tmdt.marketplace.service.MarketplaceService.MasterDataOption;
import com.tmdt.marketplace.service.MarketplaceService.NotificationCenterResponse;
import com.tmdt.marketplace.service.MarketplaceService.OrderDetailResponse;
import com.tmdt.marketplace.service.MarketplaceService.PaymentCreateRequest;
import com.tmdt.marketplace.service.MarketplaceService.PaymentCreateResponse;
import com.tmdt.marketplace.service.MarketplaceService.PaymentReturnResponse;
import com.tmdt.marketplace.service.MarketplaceService.ProductCard;
import com.tmdt.marketplace.service.MarketplaceService.ProductDetailResponse;
import com.tmdt.marketplace.service.MarketplaceService.PublicShopResponse;
import com.tmdt.marketplace.service.MarketplaceService.QuestionAnswerRequest;
import com.tmdt.marketplace.service.MarketplaceService.QuestionRequest;
import com.tmdt.marketplace.service.MarketplaceService.QuestionSummary;
import com.tmdt.marketplace.service.MarketplaceService.ReviewRequest;
import com.tmdt.marketplace.service.MarketplaceService.ReviewSummary;
import com.tmdt.marketplace.service.MarketplaceService.SellerDashboardResponse;
import com.tmdt.marketplace.service.MarketplaceService.SellerOrderResponse;
import com.tmdt.marketplace.service.MarketplaceService.SellerShipmentRequest;
import com.tmdt.marketplace.service.MarketplaceService.SellerStatusUpdateRequest;
import com.tmdt.marketplace.service.MarketplaceService.ShippingFeeRequest;
import com.tmdt.marketplace.service.MarketplaceService.ShippingFeeResponse;
import com.tmdt.marketplace.service.MarketplaceService.UpdateCartItemRequest;
import com.tmdt.marketplace.service.MarketplaceService.UserProfileResponse;
import com.tmdt.marketplace.service.MarketplaceService.WishlistToggleResponse;
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
    private final AdvancedMarketplaceService advancedMarketplaceService;

    public ApiController(MarketplaceService marketplaceService, AdvancedMarketplaceService advancedMarketplaceService) {
        this.marketplaceService = marketplaceService;
        this.advancedMarketplaceService = advancedMarketplaceService;
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

    @GetMapping("/v1/homepage")
    public HomepageResponse homepage() {
        return marketplaceService.getHomepage();
    }

    @GetMapping("/v1/products/{productId}")
    public ProductDetailResponse productDetail(
            @RequestAttribute(value = "userId", required = false) Long tokenUserId,
            @PathVariable Long productId) {
        return marketplaceService.getProductDetail(tokenUserId, productId);
    }

    @GetMapping("/v1/shops/{shopId}")
    public PublicShopResponse publicShop(
            @RequestAttribute(value = "userId", required = false) Long tokenUserId,
            @PathVariable Long shopId) {
        return marketplaceService.getPublicShop(tokenUserId, shopId);
    }

    @PostMapping("/v1/shops/{shopId}/follow")
    public FollowToggleResponse toggleFollowShop(
            @RequestAttribute(value = "userId", required = false) Long tokenUserId,
            @PathVariable Long shopId) {
        return marketplaceService.toggleFollowShop(requireUserId(tokenUserId), shopId);
    }

    @GetMapping("/v1/users/me")
    public UserProfileResponse profile(
            @RequestAttribute(value = "userId", required = false) Long tokenUserId) {
        return marketplaceService.getProfile(requireUserId(tokenUserId));
    }

    @GetMapping("/v1/wishlist")
    public List<ProductCard> wishlist(
            @RequestAttribute(value = "userId", required = false) Long tokenUserId) {
        return marketplaceService.getWishlist(requireUserId(tokenUserId));
    }

    @PostMapping("/v1/wishlist/{productId}")
    public WishlistToggleResponse toggleWishlist(
            @RequestAttribute(value = "userId", required = false) Long tokenUserId,
            @PathVariable Long productId) {
        return marketplaceService.toggleWishlist(requireUserId(tokenUserId), productId);
    }

    @PostMapping("/v1/products/{productId}/questions")
    public QuestionSummary askQuestion(
            @RequestAttribute(value = "userId", required = false) Long tokenUserId,
            @PathVariable Long productId,
            @RequestBody QuestionRequest request) {
        return marketplaceService.askQuestion(requireUserId(tokenUserId), productId, request);
    }

    @PostMapping("/v1/seller/questions/{questionId}/answer")
    public QuestionSummary answerQuestion(
            @RequestHeader(value = "X-Shop-Id", required = false) Long shopId,
            @RequestAttribute(value = "shopId", required = false) Long tokenShopId,
            @RequestAttribute(value = "userId", required = false) Long tokenUserId,
            @RequestAttribute(value = "role", required = false) String role,
            @PathVariable Long questionId,
            @RequestBody QuestionAnswerRequest request) {
        Long sellerShopId = requireSellerShopId(shopId, tokenShopId, tokenUserId, role);
        return marketplaceService.answerQuestion(sellerShopId, questionId, request);
    }

    @PostMapping("/v1/products/{productId}/reviews")
    public ReviewSummary addReview(
            @RequestAttribute(value = "userId", required = false) Long tokenUserId,
            @PathVariable Long productId,
            @RequestBody ReviewRequest request) {
        return marketplaceService.addReview(requireUserId(tokenUserId), productId, request);
    }

    @GetMapping("/v1/reviews")
    public List<ReviewModerationSummary> reviews() {
        return advancedMarketplaceService.listReviews(null);
    }

    @GetMapping("/v1/admin/reviews")
    public List<ReviewModerationSummary> adminReviews(
            @RequestAttribute(value = "role", required = false) String role) {
        requireRole(role, "ADMIN");
        return advancedMarketplaceService.listReviews(null);
    }

    @PutMapping("/v1/admin/reviews/{reviewId}/reply")
    public ReviewModerationSummary adminReplyReview(
            @RequestAttribute(value = "role", required = false) String role,
            @PathVariable Long reviewId,
            @RequestBody ReviewReplyRequest request) {
        requireRole(role, "ADMIN");
        return advancedMarketplaceService.replyReview(reviewId, request, null);
    }

    @GetMapping("/v1/notifications")
    public NotificationCenterResponse notifications(
            @RequestAttribute(value = "userId", required = false) Long tokenUserId) {
        return marketplaceService.getNotificationCenter(requireUserId(tokenUserId));
    }

    @PutMapping("/v1/notifications/{notificationId}/read")
    public NotificationCenterResponse markNotificationRead(
            @RequestAttribute(value = "userId", required = false) Long tokenUserId,
            @PathVariable Long notificationId) {
        return marketplaceService.markNotificationRead(requireUserId(tokenUserId), notificationId);
    }

    @PutMapping("/v1/notifications/read-all")
    public NotificationCenterResponse markAllNotificationsRead(
            @RequestAttribute(value = "userId", required = false) Long tokenUserId) {
        return marketplaceService.markAllNotificationsRead(requireUserId(tokenUserId));
    }

    @GetMapping("/v1/marketplace/modules")
    public MarketplaceModulesResponse modules() {
        return advancedMarketplaceService.modules();
    }

    @GetMapping("/v1/categories")
    public List<CategoryAdminSummary> categories() {
        return advancedMarketplaceService.listCategories();
    }

    @GetMapping("/v1/admin/users")
    public List<AdminUserSummary> adminUsers(
            @RequestAttribute(value = "role", required = false) String role) {
        requireRole(role, "ADMIN");
        return advancedMarketplaceService.listAdminUsers();
    }

    @PostMapping("/v1/admin/users")
    public AdminUserSummary createAdminUser(
            @RequestAttribute(value = "role", required = false) String role,
            @RequestBody AdminUserRequest request) {
        requireRole(role, "ADMIN");
        return advancedMarketplaceService.createAdminUser(request);
    }

    @PutMapping("/v1/admin/users/{userId}")
    public AdminUserSummary updateAdminUser(
            @RequestAttribute(value = "role", required = false) String role,
            @PathVariable Long userId,
            @RequestBody AdminUserRequest request) {
        requireRole(role, "ADMIN");
        return advancedMarketplaceService.updateAdminUser(userId, request);
    }

    @PutMapping("/v1/admin/users/{userId}/verify-seller")
    public AdminUserSummary verifySeller(
            @RequestAttribute(value = "role", required = false) String role,
            @PathVariable Long userId,
            @RequestParam(defaultValue = "true") boolean verified) {
        requireRole(role, "ADMIN");
        return advancedMarketplaceService.verifySeller(userId, verified);
    }

    @PostMapping("/v1/admin/categories")
    public CategoryAdminSummary createCategory(
            @RequestAttribute(value = "role", required = false) String role,
            @RequestBody CategoryRequest request) {
        requireRole(role, "ADMIN");
        return advancedMarketplaceService.createCategory(request);
    }

    @PutMapping("/v1/admin/categories/{categoryId}")
    public CategoryAdminSummary updateCategory(
            @RequestAttribute(value = "role", required = false) String role,
            @PathVariable Long categoryId,
            @RequestBody CategoryRequest request) {
        requireRole(role, "ADMIN");
        return advancedMarketplaceService.updateCategory(categoryId, request);
    }

    @GetMapping("/v1/seller/products")
    public List<ProductAdminSummary> sellerProducts(
            @RequestHeader(value = "X-Shop-Id", required = false) Long shopId,
            @RequestAttribute(value = "shopId", required = false) Long tokenShopId,
            @RequestAttribute(value = "userId", required = false) Long tokenUserId,
            @RequestAttribute(value = "role", required = false) String role) {
        return advancedMarketplaceService.listSellerProducts(requireSellerShopId(shopId, tokenShopId, tokenUserId, role));
    }

    @PostMapping("/v1/seller/products")
    public ProductAdminSummary createSellerProduct(
            @RequestHeader(value = "X-Shop-Id", required = false) Long shopId,
            @RequestAttribute(value = "shopId", required = false) Long tokenShopId,
            @RequestAttribute(value = "userId", required = false) Long tokenUserId,
            @RequestAttribute(value = "role", required = false) String role,
            @RequestBody ProductWriteRequest request) {
        return advancedMarketplaceService.createSellerProduct(requireSellerShopId(shopId, tokenShopId, tokenUserId, role), request);
    }

    @PutMapping("/v1/seller/products/{productId}")
    public ProductAdminSummary updateSellerProduct(
            @RequestHeader(value = "X-Shop-Id", required = false) Long shopId,
            @RequestAttribute(value = "shopId", required = false) Long tokenShopId,
            @RequestAttribute(value = "userId", required = false) Long tokenUserId,
            @RequestAttribute(value = "role", required = false) String role,
            @PathVariable Long productId,
            @RequestBody ProductWriteRequest request) {
        return advancedMarketplaceService.updateSellerProduct(requireSellerShopId(shopId, tokenShopId, tokenUserId, role), productId, request);
    }

    @GetMapping("/v1/admin/products")
    public List<ProductAdminSummary> adminProducts(
            @RequestAttribute(value = "role", required = false) String role) {
        requireRole(role, "ADMIN");
        return advancedMarketplaceService.listAdminProducts();
    }

    @PutMapping("/v1/admin/products/{productId}/status")
    public ProductAdminSummary moderateProduct(
            @RequestAttribute(value = "role", required = false) String role,
            @PathVariable Long productId,
            @RequestBody StatusRequest request) {
        requireRole(role, "ADMIN");
        return advancedMarketplaceService.moderateProduct(productId, request);
    }

    @GetMapping("/v1/vouchers")
    public List<VoucherSummary> vouchers() {
        return advancedMarketplaceService.listVouchers();
    }

    @PostMapping("/v1/vouchers/apply")
    public VoucherApplyResponse applyVoucher(
            @RequestAttribute(value = "userId", required = false) Long tokenUserId,
            @RequestBody VoucherApplyRequest request) {
        return advancedMarketplaceService.applyVoucher(requireUserId(tokenUserId), request);
    }

    @PostMapping("/v1/admin/vouchers")
    public VoucherSummary createPlatformVoucher(
            @RequestAttribute(value = "role", required = false) String role,
            @RequestBody VoucherRequest request) {
        requireRole(role, "ADMIN");
        return advancedMarketplaceService.createVoucher(request, null);
    }

    @PostMapping("/v1/seller/vouchers")
    public VoucherSummary createSellerVoucher(
            @RequestHeader(value = "X-Shop-Id", required = false) Long shopId,
            @RequestAttribute(value = "shopId", required = false) Long tokenShopId,
            @RequestAttribute(value = "userId", required = false) Long tokenUserId,
            @RequestAttribute(value = "role", required = false) String role,
            @RequestBody VoucherRequest request) {
        return advancedMarketplaceService.createVoucher(request, requireSellerShopId(shopId, tokenShopId, tokenUserId, role));
    }

    @GetMapping("/v1/flash-sales")
    public List<FlashSaleSummary> flashSales() {
        return advancedMarketplaceService.listFlashSales();
    }

    @PostMapping("/v1/admin/flash-sales")
    public FlashSaleSummary createFlashSale(
            @RequestAttribute(value = "role", required = false) String role,
            @RequestBody FlashSaleRequest request) {
        requireRole(role, "ADMIN");
        return advancedMarketplaceService.createFlashSale(request);
    }

    @GetMapping("/v1/gift-wrap-tiers")
    public List<GiftWrapTier> giftWrapTiers() {
        return advancedMarketplaceService.listGiftWrapTiers();
    }

    @GetMapping("/v1/shipping-profiles")
    public List<ShippingProfileSummary> shippingProfiles(
            @RequestHeader(value = "X-Shop-Id", required = false) Long shopId,
            @RequestAttribute(value = "shopId", required = false) Long tokenShopId,
            @RequestAttribute(value = "userId", required = false) Long tokenUserId,
            @RequestAttribute(value = "role", required = false) String role) {
        return advancedMarketplaceService.listShippingProfiles(requireSellerShopId(shopId, tokenShopId, tokenUserId, role));
    }

    @GetMapping("/v1/chat/conversations")
    public List<ConversationSummary> conversations(
            @RequestHeader(value = "X-Shop-Id", required = false) Long shopId,
            @RequestAttribute(value = "shopId", required = false) Long tokenShopId,
            @RequestAttribute(value = "userId", required = false) Long tokenUserId,
            @RequestAttribute(value = "role", required = false) String role) {
        Long userId = requireUserId(tokenUserId);
        Long effectiveShopId = "SELLER".equalsIgnoreCase(role) ? requireSellerShopId(shopId, tokenShopId, tokenUserId, role) : null;
        return advancedMarketplaceService.listConversations(userId, effectiveShopId, role);
    }

    @PostMapping("/v1/chat/conversations")
    public ConversationSummary startConversation(
            @RequestAttribute(value = "userId", required = false) Long tokenUserId,
            @RequestBody ConversationRequest request) {
        return advancedMarketplaceService.startConversation(requireUserId(tokenUserId), request);
    }

    @GetMapping("/v1/chat/conversations/{conversationId}/messages")
    public List<MessageSummary> messages(
            @RequestAttribute(value = "userId", required = false) Long tokenUserId,
            @PathVariable Long conversationId) {
        requireUserId(tokenUserId);
        return advancedMarketplaceService.listMessages(conversationId);
    }

    @PostMapping("/v1/chat/conversations/{conversationId}/messages")
    public MessageSummary sendMessage(
            @RequestAttribute(value = "userId", required = false) Long tokenUserId,
            @RequestAttribute(value = "role", required = false) String role,
            @PathVariable Long conversationId,
            @RequestBody MessageRequest request) {
        return advancedMarketplaceService.sendMessage(requireUserId(tokenUserId), role, conversationId, request);
    }

    @PostMapping("/v1/chat/conversations/{conversationId}/custom-quotes")
    public MessageSummary sendCustomQuote(
            @RequestHeader(value = "X-Shop-Id", required = false) Long shopId,
            @RequestAttribute(value = "shopId", required = false) Long tokenShopId,
            @RequestAttribute(value = "userId", required = false) Long tokenUserId,
            @RequestAttribute(value = "role", required = false) String role,
            @PathVariable Long conversationId,
            @RequestBody CustomQuoteRequest request) {
        return advancedMarketplaceService.sendCustomQuote(
                requireUserId(tokenUserId),
                requireSellerShopId(shopId, tokenShopId, tokenUserId, role),
                conversationId,
                request);
    }

    @GetMapping("/v1/seller/reviews")
    public List<ReviewModerationSummary> sellerReviews(
            @RequestHeader(value = "X-Shop-Id", required = false) Long shopId,
            @RequestAttribute(value = "shopId", required = false) Long tokenShopId,
            @RequestAttribute(value = "userId", required = false) Long tokenUserId,
            @RequestAttribute(value = "role", required = false) String role) {
        return advancedMarketplaceService.listReviews(requireSellerShopId(shopId, tokenShopId, tokenUserId, role));
    }

    @PutMapping("/v1/seller/reviews/{reviewId}/reply")
    public ReviewModerationSummary sellerReplyReview(
            @RequestHeader(value = "X-Shop-Id", required = false) Long shopId,
            @RequestAttribute(value = "shopId", required = false) Long tokenShopId,
            @RequestAttribute(value = "userId", required = false) Long tokenUserId,
            @RequestAttribute(value = "role", required = false) String role,
            @PathVariable Long reviewId,
            @RequestBody ReviewReplyRequest request) {
        return advancedMarketplaceService.replyReview(reviewId, request, requireSellerShopId(shopId, tokenShopId, tokenUserId, role));
    }

    @GetMapping("/v1/custom-orders")
    public List<CustomOrderSummary> customOrders(
            @RequestHeader(value = "X-Shop-Id", required = false) Long shopId,
            @RequestAttribute(value = "shopId", required = false) Long tokenShopId,
            @RequestAttribute(value = "userId", required = false) Long tokenUserId,
            @RequestAttribute(value = "role", required = false) String role) {
        Long userId = requireUserId(tokenUserId);
        Long effectiveShopId = "SELLER".equalsIgnoreCase(role) ? requireSellerShopId(shopId, tokenShopId, tokenUserId, role) : null;
        return advancedMarketplaceService.listCustomOrders(effectiveShopId == null ? userId : null, effectiveShopId);
    }

    @PostMapping("/v1/seller/custom-orders")
    public CustomOrderSummary createCustomOrder(
            @RequestHeader(value = "X-Shop-Id", required = false) Long shopId,
            @RequestAttribute(value = "shopId", required = false) Long tokenShopId,
            @RequestAttribute(value = "userId", required = false) Long tokenUserId,
            @RequestAttribute(value = "role", required = false) String role,
            @RequestBody CustomOrderRequest request) {
        return advancedMarketplaceService.createCustomOrder(requireUserId(tokenUserId),
                requireSellerShopId(shopId, tokenShopId, tokenUserId, role), request);
    }

    @PutMapping("/v1/custom-orders/{customOrderId}/status")
    public CustomOrderSummary updateCustomOrderStatus(
            @RequestAttribute(value = "userId", required = false) Long tokenUserId,
            @PathVariable Long customOrderId,
            @RequestBody StatusRequest request) {
        requireUserId(tokenUserId);
        return advancedMarketplaceService.updateCustomOrderStatus(customOrderId, request);
    }

    @GetMapping("/v1/commissions")
    public List<CommissionPostSummary> commissions() {
        return advancedMarketplaceService.listCommissionPosts();
    }

    @PostMapping("/v1/commissions")
    public CommissionPostSummary createCommission(
            @RequestAttribute(value = "userId", required = false) Long tokenUserId,
            @RequestBody CommissionPostRequest request) {
        return advancedMarketplaceService.createCommissionPost(requireUserId(tokenUserId), request);
    }

    @GetMapping("/v1/commissions/{postId}/proposals")
    public List<ProposalSummary> proposals(@PathVariable Long postId) {
        return advancedMarketplaceService.listProposals(postId);
    }

    @PostMapping("/v1/commissions/{postId}/proposals")
    public ProposalSummary createProposal(
            @RequestHeader(value = "X-Shop-Id", required = false) Long shopId,
            @RequestAttribute(value = "shopId", required = false) Long tokenShopId,
            @RequestAttribute(value = "userId", required = false) Long tokenUserId,
            @RequestAttribute(value = "role", required = false) String role,
            @PathVariable Long postId,
            @RequestBody ProposalRequest request) {
        return advancedMarketplaceService.createProposal(requireUserId(tokenUserId),
                requireSellerShopId(shopId, tokenShopId, tokenUserId, role), postId, request);
    }

    @PutMapping("/v1/commissions/{postId}/proposals/{proposalId}/accept")
    public ProposalSummary acceptProposal(
            @RequestAttribute(value = "userId", required = false) Long tokenUserId,
            @PathVariable Long postId,
            @PathVariable Long proposalId) {
        return advancedMarketplaceService.acceptProposal(requireUserId(tokenUserId), postId, proposalId);
    }

    @GetMapping("/v1/media/folders")
    public List<MediaFolderSummary> mediaFolders(
            @RequestAttribute(value = "userId", required = false) Long tokenUserId) {
        return advancedMarketplaceService.listMediaFolders(requireUserId(tokenUserId));
    }

    @PostMapping("/v1/media/folders")
    public MediaFolderSummary createMediaFolder(
            @RequestAttribute(value = "userId", required = false) Long tokenUserId,
            @RequestBody MediaFolderRequest request) {
        return advancedMarketplaceService.createMediaFolder(requireUserId(tokenUserId), request);
    }

    @GetMapping("/v1/media/images")
    public List<MediaImageSummary> mediaImages(
            @RequestAttribute(value = "userId", required = false) Long tokenUserId) {
        return advancedMarketplaceService.listMediaImages(requireUserId(tokenUserId));
    }

    @PostMapping("/v1/media/images")
    public MediaImageSummary addMediaImage(
            @RequestAttribute(value = "userId", required = false) Long tokenUserId,
            @RequestBody MediaImageRequest request) {
        return advancedMarketplaceService.addMediaImage(requireUserId(tokenUserId), request);
    }

    @PostMapping("/v1/reports")
    public ReportSummary createReport(
            @RequestAttribute(value = "userId", required = false) Long tokenUserId,
            @RequestBody ReportRequest request) {
        return advancedMarketplaceService.createReport(requireUserId(tokenUserId), request);
    }

    @GetMapping("/v1/admin/reports")
    public List<ReportSummary> adminReports(
            @RequestAttribute(value = "role", required = false) String role) {
        requireRole(role, "ADMIN");
        return advancedMarketplaceService.listReports();
    }

    @PutMapping("/v1/admin/reports/{reportId}")
    public ReportSummary updateReport(
            @RequestAttribute(value = "role", required = false) String role,
            @PathVariable Long reportId,
            @RequestBody ReportUpdateRequest request) {
        requireRole(role, "ADMIN");
        return advancedMarketplaceService.updateReport(reportId, request);
    }

    @GetMapping("/v1/settings/platform")
    public Map<String, String> platformSettings() {
        return advancedMarketplaceService.platformSettings();
    }

    @PutMapping("/v1/settings/platform")
    public Map<String, String> updatePlatformSettings(
            @RequestAttribute(value = "role", required = false) String role,
            @RequestBody Map<String, String> request) {
        requireRole(role, "ADMIN");
        return advancedMarketplaceService.updatePlatformSettings(request);
    }

    @GetMapping("/v1/payments/history")
    public List<PaymentHistorySummary> paymentHistory(
            @RequestAttribute(value = "userId", required = false) Long tokenUserId) {
        return advancedMarketplaceService.paymentHistory(requireUserId(tokenUserId));
    }

    @GetMapping("/v1/analytics/seller/summary")
    public SellerAnalyticsResponse sellerAnalytics(
            @RequestHeader(value = "X-Shop-Id", required = false) Long shopId,
            @RequestAttribute(value = "shopId", required = false) Long tokenShopId,
            @RequestAttribute(value = "userId", required = false) Long tokenUserId,
            @RequestAttribute(value = "role", required = false) String role) {
        return advancedMarketplaceService.sellerAnalytics(requireSellerShopId(shopId, tokenShopId, tokenUserId, role));
    }

    @GetMapping("/v1/admin/payment-reliability")
    public PaymentReliabilityResponse paymentReliability(
            @RequestAttribute(value = "role", required = false) String role) {
        requireRole(role, "ADMIN");
        return advancedMarketplaceService.paymentReliability();
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

    @GetMapping("/v1/seller/dashboard")
    public SellerDashboardResponse sellerDashboard(
            @RequestHeader(value = "X-Shop-Id", required = false) Long shopId,
            @RequestAttribute(value = "shopId", required = false) Long tokenShopId,
            @RequestAttribute(value = "userId", required = false) Long tokenUserId,
            @RequestAttribute(value = "role", required = false) String role) {
        return marketplaceService.getSellerDashboard(requireSellerShopId(shopId, tokenShopId, tokenUserId, role));
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

    @GetMapping("/v1/admin/dashboard")
    public AdminDashboardResponse adminDashboard(
            @RequestAttribute(value = "role", required = false) String role) {
        requireRole(role, "ADMIN");
        return marketplaceService.getAdminDashboard();
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
