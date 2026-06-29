package com.tmdt.marketplace.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
public class MarketplaceService {

    private static final Set<String> PAYMENT_METHODS = Set.of("COD", "VNPAY");
    private static final Set<String> SELLER_STATUSES = Set.of(
            "NEW", "CONFIRMED", "PACKING", "SHIPPING", "COMPLETED", "CANCELLED", "DELIVERY_FAILED"
    );
    private static final Set<String> BUYER_CANCELLABLE_SHOP_STATUSES = Set.of(
            "PENDING_PAYMENT", "NEW", "CONFIRMED"
    );

    private final JdbcTemplate jdbcTemplate;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final Map<Long, CartState> memoryCarts = new ConcurrentHashMap<>();

    private final long defaultBuyerId;
    private final long defaultShopId;
    private final long cartTtlDays;
    private final BigDecimal commissionRate;
    private final boolean vnpayMockEnabled;
    private final String vnpayReturnUrl;
    private final String vnpaySecret;
    private final String vnpayTmnCode;
    private final String vnpayPayUrl;
    private final GHNService ghnService;

    public MarketplaceService(
            JdbcTemplate jdbcTemplate,
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            GHNService ghnService,
            @Value("${app.default-buyer-id:1}") long defaultBuyerId,
            @Value("${app.default-shop-id:1}") long defaultShopId,
            @Value("${marketplace.cart.ttl-days:7}") long cartTtlDays,
            @Value("${marketplace.commission-rate:0.10}") BigDecimal commissionRate,
            @Value("${vnpay.mock-enabled:false}") boolean vnpayMockEnabled,
            @Value("${vnpay.return-url:http://localhost:8080/payment-result.html}") String vnpayReturnUrl,
            @Value("${vnpay.hash-secret:dev-vnpay-secret-change-me}") String vnpaySecret,
            @Value("${vnpay.tmn-code:DEMO}") String vnpayTmnCode,
            @Value("${vnpay.pay-url:https://sandbox.vnpayment.vn/paymentv2/vpcpay.html}") String vnpayPayUrl
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.defaultBuyerId = defaultBuyerId;
        this.defaultShopId = defaultShopId;
        this.cartTtlDays = cartTtlDays;
        this.commissionRate = commissionRate;
        this.vnpayMockEnabled = vnpayMockEnabled;
        this.vnpayReturnUrl = vnpayReturnUrl;
        this.vnpaySecret = StringUtils.hasText(vnpaySecret) ? vnpaySecret : "dev-vnpay-secret-change-me";
        this.vnpayTmnCode = vnpayTmnCode;
        this.vnpayPayUrl = vnpayPayUrl;
        this.ghnService = ghnService;
    }

    private Integer parseInteger(String str) {
        if (str == null || str.isBlank()) return null;
        try { return Integer.parseInt(str); } catch (NumberFormatException e) { return null; }
    }

    public long buyerIdOrDefault(Long buyerId) {
        return buyerId == null || buyerId <= 0 ? defaultBuyerId : buyerId;
    }

    public long shopIdOrDefault(Long shopId) {
        return shopId == null || shopId <= 0 ? defaultShopId : shopId;
    }

    public boolean userOwnsShop(Long userId, Long shopId) {
        if (userId == null || shopId == null) {
            return false;
        }
        try {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM `Shops` WHERE `id` = ? AND `owner_id` = ?",
                    Integer.class,
                    shopId,
                    userId);
            return count != null && count > 0;
        } catch (DataAccessException ex) {
            throw serviceUnavailable();
        }
    }

    public List<ProductCard> getProducts() {
        try {
            return jdbcTemplate.query(productSelectSql() + " ORDER BY p.id", this::mapProductCard);
        } catch (DataAccessException ex) {
            return fallbackProducts();
        }
    }

    public HomepageResponse getHomepage() {
        try {
            List<HomepageBanner> banners = jdbcTemplate.query("""
                    SELECT `id`, `title`, `subtitle`, `image_url`, `link_url`
                    FROM `homepage_banners`
                    WHERE `active` = true
                    ORDER BY `sort_order`, `id`
                    """, (rs, rowNum) -> new HomepageBanner(
                    rs.getLong("id"),
                    rs.getString("title"),
                    rs.getString("subtitle"),
                    rs.getString("image_url"),
                    rs.getString("link_url")
            ));
            List<CategorySummary> categories = jdbcTemplate.query("""
                    SELECT c.`id`, c.`name`, c.`slug`, COALESCE(c.`status`, 'ACTIVE') AS status,
                           COUNT(p.`id`) AS product_count
                    FROM `Categories` c
                    LEFT JOIN `Products` p ON p.`cat_id` = c.`id` AND p.`status` = 'active'
                    GROUP BY c.`id`, c.`name`, c.`slug`, c.`status`
                    ORDER BY c.`id`
                    """, this::mapCategorySummary);
            List<ProductCard> featured = jdbcTemplate.query(productSelectSql()
                    + " WHERE p.`status` = 'active' ORDER BY p.`avg_rating` DESC, p.`id` LIMIT 8", this::mapProductCard);
            List<ProductCard> bestSellers = jdbcTemplate.query(productSelectSql()
                    + " WHERE p.`status` = 'active' ORDER BY COALESCE(inv.`stock`, 0) ASC, p.`id` LIMIT 8", this::mapProductCard);
            List<PromotionCard> promotions = List.of(
                    new PromotionCard("FLASH_DEMO", "Flash handmade", "Giam gia mau cho cac san pham co san", "Dang mo"),
                    new PromotionCard("VOUCHER_DEMO", "Voucher shop", "Nhap ma tai checkout khi shop kich hoat voucher", "Sap co")
            );
            return new HomepageResponse(banners, categories, featured, bestSellers, promotions);
        } catch (DataAccessException ex) {
            List<ProductCard> products = fallbackProducts();
            return new HomepageResponse(List.of(), List.of(), products, products, List.of());
        }
    }

    public ProductDetailResponse getProductDetail(Long buyerIdHeader, Long productId) {
        ProductCard product = findProductCard(productId).orElseThrow(() -> notFound("Khong tim thay san pham."));
        try {
            List<ReviewSummary> reviews = jdbcTemplate.query("""
                    SELECT r.`id`, r.`rating`, r.`comment`, r.`seller_reply`, u.`full_name`, r.`created_at`
                    FROM `product_reviews` r
                    LEFT JOIN `Users` u ON u.`id` = r.`user_id`
                    WHERE r.`product_id` = ?
                    ORDER BY r.`created_at` DESC
                    """, this::mapReviewSummary, productId);
            List<QuestionSummary> questions = jdbcTemplate.query("""
                    SELECT q.`id`, q.`question`, q.`answer`, COALESCE(u.`full_name`, 'Khach hang') AS asker_name,
                           q.`status`, q.`created_at`, q.`answered_at`
                    FROM `product_questions` q
                    LEFT JOIN `Users` u ON u.`id` = q.`user_id`
                    WHERE q.`product_id` = ? AND q.`status` = 'PUBLISHED'
                    ORDER BY q.`created_at` DESC
                    """, this::mapQuestionSummary, productId);
            boolean wished = buyerIdHeader != null && exists("SELECT COUNT(*) FROM `wishlists` WHERE `user_id` = ? AND `product_id` = ?", buyerIdHeader, productId);
            boolean followedShop = buyerIdHeader != null && exists("SELECT COUNT(*) FROM `shop_follows` WHERE `user_id` = ? AND `shop_id` = ?", buyerIdHeader, product.shopId());
            List<ProductCard> related = jdbcTemplate.query(productSelectSql()
                    + " WHERE p.`id` <> ? AND c.`name` = ? AND p.`status` = 'active' ORDER BY p.`id` LIMIT 4",
                    this::mapProductCard, productId, product.category());
            return new ProductDetailResponse(product, reviews, questions, wished, followedShop, related);
        } catch (DataAccessException ex) {
            return fallbackProductDetail(product);
        }
    }

    public PublicShopResponse getPublicShop(Long buyerIdHeader, Long shopId) {
        try {
            ShopProfile shop = jdbcTemplate.queryForObject("""
                    SELECT s.`id`, s.`shop_name`, COALESCE(s.`logo_url`, '') AS logo_url,
                           COALESCE(s.`hero_url`, '') AS hero_url, COALESCE(s.`description`, '') AS description,
                           COALESCE(s.`about`, '') AS about, COALESCE(s.`materials`, '') AS materials,
                           COALESCE(s.`years_experience`, 1) AS years_experience,
                           COALESCE(s.`verified_artisan`, false) AS verified_artisan,
                           COALESCE(s.`rating`, 0) AS rating,
                           (SELECT COUNT(*) FROM `shop_follows` f WHERE f.`shop_id` = s.`id`) AS follower_count
                    FROM `Shops` s
                    WHERE s.`id` = ?
                    """, this::mapShopProfile, shopId);
            List<ProductCard> products = jdbcTemplate.query(productSelectSql()
                    + " WHERE p.`shop_id` = ? AND p.`status` = 'active' ORDER BY p.`id`", this::mapProductCard, shopId);
            boolean followed = buyerIdHeader != null && exists("SELECT COUNT(*) FROM `shop_follows` WHERE `user_id` = ? AND `shop_id` = ?", buyerIdHeader, shopId);
            return new PublicShopResponse(shop, products, followed);
        } catch (EmptyResultDataAccessException ex) {
            throw notFound("Khong tim thay shop.");
        } catch (DataAccessException ex) {
            return fallbackPublicShop(shopId).orElseThrow(() -> serviceUnavailable());
        }
    }

    public UserProfileResponse getProfile(Long userIdHeader) {
        long userId = buyerIdOrDefault(userIdHeader);
        try {
            UserProfile profile = jdbcTemplate.queryForObject("""
                    SELECT u.`id`, u.`full_name`, COALESCE(a.`email`, '') AS email, COALESCE(u.`phone`, '') AS phone,
                           COALESCE(u.`address`, '') AS address, COALESCE(u.`reward_points`, 0) AS reward_points,
                           COALESCE(u.`status`, 'ACTIVE') AS status, COALESCE(a.`role`, 'BUYER') AS role
                    FROM `Users` u
                    LEFT JOIN `Accounts` a ON a.`user_id` = u.`id`
                    WHERE u.`id` = ?
                    """, this::mapUserProfile, userId);
            List<AddressSummary> addresses = jdbcTemplate.query("""
                    SELECT `id`, `label`, `receiver_name`, `phone`, `province`, `district`, `ward`, `address`, `is_default`
                    FROM `user_addresses`
                    WHERE `user_id` = ?
                    ORDER BY `is_default` DESC, `id`
                    LIMIT 5
                    """, this::mapAddressSummary, userId);
            List<ProductCard> wishlist = getWishlist(userId);
            List<ShopProfile> followed = jdbcTemplate.query("""
                    SELECT s.`id`, s.`shop_name`, COALESCE(s.`logo_url`, '') AS logo_url,
                           COALESCE(s.`hero_url`, '') AS hero_url, COALESCE(s.`description`, '') AS description,
                           COALESCE(s.`about`, '') AS about, COALESCE(s.`materials`, '') AS materials,
                           COALESCE(s.`years_experience`, 1) AS years_experience,
                           COALESCE(s.`verified_artisan`, false) AS verified_artisan,
                           COALESCE(s.`rating`, 0) AS rating,
                           (SELECT COUNT(*) FROM `shop_follows` f2 WHERE f2.`shop_id` = s.`id`) AS follower_count
                    FROM `shop_follows` f
                    JOIN `Shops` s ON s.`id` = f.`shop_id`
                    WHERE f.`user_id` = ?
                    ORDER BY f.`created_at` DESC
                    """, this::mapShopProfile, userId);
            return new UserProfileResponse(profile, addresses, wishlist, followed);
        } catch (EmptyResultDataAccessException ex) {
            throw notFound("Khong tim thay profile.");
        } catch (DataAccessException ex) {
            throw serviceUnavailable();
        }
    }

    public List<ProductCard> getWishlist(Long userIdHeader) {
        long userId = buyerIdOrDefault(userIdHeader);
        try {
            return jdbcTemplate.query(productSelectSql()
                    + " JOIN `wishlists` w ON w.`product_id` = p.`id` WHERE w.`user_id` = ? ORDER BY w.`created_at` DESC",
                    this::mapProductCard, userId);
        } catch (DataAccessException ex) {
            throw serviceUnavailable();
        }
    }

    public WishlistToggleResponse toggleWishlist(Long userIdHeader, Long productId) {
        long userId = buyerIdOrDefault(userIdHeader);
        findProduct(productId).orElseThrow(() -> notFound("Khong tim thay san pham."));
        try {
            if (exists("SELECT COUNT(*) FROM `wishlists` WHERE `user_id` = ? AND `product_id` = ?", userId, productId)) {
                jdbcTemplate.update("DELETE FROM `wishlists` WHERE `user_id` = ? AND `product_id` = ?", userId, productId);
                return new WishlistToggleResponse(productId, false);
            }
            jdbcTemplate.update("INSERT INTO `wishlists` (`user_id`, `product_id`) VALUES (?, ?)", userId, productId);
            notifyUser(userId, "WISHLIST", "Da luu san pham", "San pham da duoc them vao wishlist.", "wishlist-" + productId);
            return new WishlistToggleResponse(productId, true);
        } catch (DataAccessException ex) {
            throw serviceUnavailable();
        }
    }

    public FollowToggleResponse toggleFollowShop(Long userIdHeader, Long shopId) {
        long userId = buyerIdOrDefault(userIdHeader);
        if (userOwnsShop(userId, shopId)) {
            throw badRequest("Khong the follow shop cua chinh minh.");
        }
        if (!exists("SELECT COUNT(*) FROM `Shops` WHERE `id` = ?", shopId)) {
            throw notFound("Khong tim thay shop.");
        }
        try {
            if (exists("SELECT COUNT(*) FROM `shop_follows` WHERE `user_id` = ? AND `shop_id` = ?", userId, shopId)) {
                jdbcTemplate.update("DELETE FROM `shop_follows` WHERE `user_id` = ? AND `shop_id` = ?", userId, shopId);
                return new FollowToggleResponse(shopId, false, count("SELECT COUNT(*) FROM `shop_follows` WHERE `shop_id` = ?", shopId));
            }
            jdbcTemplate.update("INSERT INTO `shop_follows` (`user_id`, `shop_id`) VALUES (?, ?)", userId, shopId);
            notifyUser(userId, "SHOP_FOLLOWED", "Dang theo doi shop", "Ban se thay cap nhat tu shop nay trong ho so.", "follow-shop-" + shopId);
            return new FollowToggleResponse(shopId, true, count("SELECT COUNT(*) FROM `shop_follows` WHERE `shop_id` = ?", shopId));
        } catch (DataAccessException ex) {
            throw serviceUnavailable();
        }
    }

    public QuestionSummary askQuestion(Long userIdHeader, Long productId, QuestionRequest request) {
        long userId = buyerIdOrDefault(userIdHeader);
        ProductRow product = findProduct(productId).orElseThrow(() -> notFound("Khong tim thay san pham."));
        requireText(request.question(), "Vui long nhap cau hoi.");
        try {
            long id = nextId("product_questions");
            jdbcTemplate.update("""
                    INSERT INTO `product_questions` (`id`, `product_id`, `user_id`, `question`, `status`, `created_at`)
                    VALUES (?, ?, ?, ?, 'PUBLISHED', CURRENT_TIMESTAMP)
                    """, id, productId, userId, normalize(request.question()));
            notifyShopOwner(product.shopId(), "QUESTION_CREATED", "Co cau hoi moi", "Khach hang vua hoi ve san pham " + product.name() + ".", "question-" + id);
            return getQuestion(id);
        } catch (DataAccessException ex) {
            throw serviceUnavailable();
        }
    }

    public QuestionSummary answerQuestion(Long shopIdHeader, Long questionId, QuestionAnswerRequest request) {
        requireText(request.answer(), "Vui long nhap cau tra loi.");
        try {
            Long ownerShopId = jdbcTemplate.queryForObject("""
                    SELECT p.`shop_id`
                    FROM `product_questions` q
                    JOIN `Products` p ON p.`id` = q.`product_id`
                    WHERE q.`id` = ?
                    """, Long.class, questionId);
            if (!Objects.equals(ownerShopId, shopIdHeader)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cau hoi khong thuoc shop nay.");
            }
            jdbcTemplate.update("""
                    UPDATE `product_questions`
                    SET `answer` = ?, `answered_at` = CURRENT_TIMESTAMP
                    WHERE `id` = ?
                    """, normalize(request.answer()), questionId);
            return getQuestion(questionId);
        } catch (EmptyResultDataAccessException ex) {
            throw notFound("Khong tim thay cau hoi.");
        } catch (DataAccessException ex) {
            throw serviceUnavailable();
        }
    }

    public ReviewEligibilityResponse getReviewEligibility(Long userIdHeader, Long productId) {
        long userId = buyerIdOrDefault(userIdHeader);
        boolean purchased = exists("""
                SELECT COUNT(*)
                FROM `Orders` o
                JOIN `OrderItems` oi ON oi.`order_id` = o.`id`
                WHERE o.`buyer_id` = ? AND oi.`product_id` = ?
                  AND (UPPER(COALESCE(oi.`merchant_status`, '')) = 'COMPLETED'
                       OR UPPER(COALESCE(o.`status`, '')) = 'COMPLETED')
                """, userId, productId);
        boolean reviewed = exists("SELECT COUNT(*) FROM `product_reviews` WHERE `user_id` = ? AND `product_id` = ?",
                userId, productId);
        String reason = !purchased
                ? "Bạn chỉ có thể đánh giá sau khi đơn hàng đã hoàn tất."
                : reviewed ? "Bạn đã đánh giá sản phẩm này." : "Bạn có thể đánh giá sản phẩm.";
        return new ReviewEligibilityResponse(purchased && !reviewed, purchased, reviewed, reason);
    }

    @Transactional
    public ReviewSummary addReview(Long userIdHeader, Long productId, ReviewRequest request) {
        long userId = buyerIdOrDefault(userIdHeader);
        ProductRow product = findProduct(productId).orElseThrow(() -> notFound("Khong tim thay san pham."));
        ReviewEligibilityResponse eligibility = getReviewEligibility(userId, productId);
        if (!eligibility.eligible()) {
            throw badRequest(eligibility.reason());
        }
        int rating = request.rating() == null ? 5 : request.rating();
        if (rating < 1 || rating > 5) {
            throw badRequest("Rating phai tu 1 den 5.");
        }
        try {
            long id = nextId("product_reviews");
            jdbcTemplate.update("""
                    INSERT INTO `product_reviews` (`id`, `product_id`, `shop_id`, `user_id`, `rating`, `comment`, `created_at`, `updated_at`)
                    VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                    """, id, productId, product.shopId(), userId, rating, normalize(request.comment()));
            jdbcTemplate.update("""
                    UPDATE `Products`
                    SET `avg_rating` = (SELECT AVG(`rating`) FROM `product_reviews` WHERE `product_id` = ?)
                    WHERE `id` = ?
                    """, productId, productId);
            notifyShopOwner(product.shopId(), "REVIEW_CREATED", "Co review moi", "San pham " + product.name() + " vua nhan danh gia.", "review-" + id);
            return getReview(id);
        } catch (DataAccessException ex) {
            throw serviceUnavailable();
        }
    }

    public List<NotificationSummary> getNotifications(Long userIdHeader) {
        long userId = buyerIdOrDefault(userIdHeader);
        try {
            return jdbcTemplate.query("""
                    SELECT `id`, `type`, `title`, `message`, `read_at`, `created_at`
                    FROM `notifications`
                    WHERE `user_id` = ?
                    ORDER BY `created_at` DESC
                    LIMIT 50
                    """, this::mapNotificationSummary, userId);
        } catch (DataAccessException ex) {
            throw serviceUnavailable();
        }
    }

    public NotificationCenterResponse getNotificationCenter(Long userIdHeader) {
        long userId = buyerIdOrDefault(userIdHeader);
        return new NotificationCenterResponse(getNotifications(userId), count("SELECT COUNT(*) FROM `notifications` WHERE `user_id` = ? AND `read_at` IS NULL", userId));
    }

    public NotificationCenterResponse markNotificationRead(Long userIdHeader, Long notificationId) {
        long userId = buyerIdOrDefault(userIdHeader);
        jdbcTemplate.update("UPDATE `notifications` SET `read_at` = CURRENT_TIMESTAMP WHERE `id` = ? AND `user_id` = ?", notificationId, userId);
        return getNotificationCenter(userId);
    }

    public NotificationCenterResponse markAllNotificationsRead(Long userIdHeader) {
        long userId = buyerIdOrDefault(userIdHeader);
        jdbcTemplate.update("UPDATE `notifications` SET `read_at` = CURRENT_TIMESTAMP WHERE `user_id` = ? AND `read_at` IS NULL", userId);
        return getNotificationCenter(userId);
    }

    public AdminDashboardResponse getAdminDashboard() {
        return new AdminDashboardResponse(
                count("SELECT COUNT(*) FROM `Users`"),
                count("SELECT COUNT(*) FROM `Shops`"),
                count("SELECT COUNT(*) FROM `Products`"),
                count("SELECT COUNT(*) FROM `Orders`"),
                money(sum("SELECT COALESCE(SUM(`total_price`), 0) FROM `Orders`")),
                count("SELECT COUNT(*) FROM `product_reviews`"),
                count("SELECT COUNT(*) FROM `product_questions` WHERE `answer` IS NULL")
        );
    }

    public SellerDashboardResponse getSellerDashboard(Long shopIdHeader) {
        long shopId = shopIdOrDefault(shopIdHeader);
        return new SellerDashboardResponse(
                shopId,
                count("SELECT COUNT(*) FROM `Products` WHERE `shop_id` = ?", shopId),
                count("SELECT COUNT(*) FROM `Products` p JOIN `Storage` st ON st.`product_id` = p.`id` WHERE p.`shop_id` = ? AND COALESCE(st.`quantity`, 0) <= COALESCE(st.`low_stock_alert`, 0)", shopId),
                count("SELECT COUNT(*) FROM `shop_orders` WHERE `shop_id` = ?", shopId),
                money(sum("SELECT COALESCE(SUM(`item_subtotal`), 0) FROM `shop_orders` WHERE `shop_id` = ?", shopId)),
                count("SELECT COUNT(*) FROM `product_reviews` WHERE `shop_id` = ?", shopId),
                count("SELECT COUNT(*) FROM `product_questions` q JOIN `Products` p ON p.`id` = q.`product_id` WHERE p.`shop_id` = ? AND q.`answer` IS NULL", shopId)
        );
    }

    public CartResponse getCart(Long buyerIdHeader) {
        long buyerId = buyerIdOrDefault(buyerIdHeader);
        CartState state = loadCartState(buyerId);
        Map<Long, ShopCartBuilder> builders = new LinkedHashMap<>();
        int totalQuantity = 0;
        boolean canCheckout = true;

        for (StoredCartItem item : state.items()) {
            totalQuantity += item.quantity();
            Optional<ProductRow> product = findProduct(item.productId());
            ProductRow row = product.orElseGet(() -> new ProductRow(
                    item.productId(), item.shopId(), "Shop khong xac dinh", "San pham khong con ban",
                    "Handmade", "INACTIVE", BigDecimal.ZERO, false, "", "hidden", "REJECTED", false, 0
            ));
            boolean lineAvailable = product.isPresent()
                    && "active".equalsIgnoreCase(row.status())
                    && "APPROVED".equalsIgnoreCase(row.approvalStatus())
                    && "ACTIVE".equalsIgnoreCase(row.categoryStatus())
                    && (!row.requiresPersonalization() || StringUtils.hasText(item.note()))
                    && row.stock() >= item.quantity()
                    && item.quantity() > 0;
            if (!lineAvailable) {
                canCheckout = false;
            }
            BigDecimal lineTotal = money(row.price().multiply(BigDecimal.valueOf(item.quantity())));
            CartLine line = new CartLine(
                    item.itemKey(),
                    row.id(),
                    row.shopId(),
                    row.name(),
                    row.shopName(),
                    row.image(),
                    item.quantity(),
                    money(row.price()),
                    lineTotal,
                    normalize(item.customOptionsJson()),
                    normalize(item.note()),
                    row.stock(),
                    row.status(),
                    lineAvailable,
                    lineAvailable ? "" : unavailableMessage(row, item.quantity())
            );
            ShopCartBuilder builder = builders.computeIfAbsent(row.shopId(), ignored -> new ShopCartBuilder(row.shopId(), row.shopName()));
            builder.lines.add(line);
            builder.subtotal = money(builder.subtotal.add(lineTotal));
            builder.canCheckout = builder.canCheckout && lineAvailable;
        }

        List<ShopCartGroup> groups = builders.values().stream()
                .map(builder -> new ShopCartGroup(
                        builder.shopId,
                        builder.shopName,
                        builder.lines,
                        money(builder.subtotal),
                        builder.canCheckout
                ))
                .toList();
        BigDecimal subtotal = groups.stream()
                .map(ShopCartGroup::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new CartResponse(buyerId, groups, money(subtotal), totalQuantity, canCheckout && !groups.isEmpty());
    }

    public CartResponse addCartItem(Long buyerIdHeader, CartItemRequest request) {
        long buyerId = buyerIdOrDefault(buyerIdHeader);
        int quantity = positiveQuantity(request.quantity());
        ProductRow product = findProduct(request.productId())
                .orElseThrow(() -> badRequest("San pham khong ton tai."));
        if (!"active".equalsIgnoreCase(product.status())) {
            throw badRequest("San pham dang khong mo ban.");
        }
        if (!"APPROVED".equalsIgnoreCase(product.approvalStatus()) || !"ACTIVE".equalsIgnoreCase(product.categoryStatus())) {
            throw badRequest("San pham hoac danh muc chua duoc phe duyet.");
        }
        if (product.requiresPersonalization() && !StringUtils.hasText(request.note())) {
            throw badRequest("San pham nay bat buoc nhap noi dung ca nhan hoa.");
        }
        CartState state = loadCartState(buyerId);
        List<StoredCartItem> items = new ArrayList<>(state.items());
        String itemKey = buildItemKey(product.id(), request.customOptionsJson(), request.note());
        boolean merged = false;
        for (int i = 0; i < items.size(); i++) {
            StoredCartItem existing = items.get(i);
            if (existing.itemKey().equals(itemKey)) {
                int nextQuantity = existing.quantity() + quantity;
                ensureStock(product, nextQuantity);
                items.set(i, new StoredCartItem(itemKey, product.id(), product.shopId(), nextQuantity,
                        normalize(request.customOptionsJson()), normalize(request.note())));
                merged = true;
                break;
            }
        }
        if (!merged) {
            ensureStock(product, quantity);
            items.add(new StoredCartItem(itemKey, product.id(), product.shopId(), quantity,
                    normalize(request.customOptionsJson()), normalize(request.note())));
        }
        saveCartState(buyerId, new CartState(items));
        return getCart(buyerId);
    }

    public CartResponse updateCartItem(Long buyerIdHeader, String itemKey, UpdateCartItemRequest request) {
        long buyerId = buyerIdOrDefault(buyerIdHeader);
        CartState state = loadCartState(buyerId);
        List<StoredCartItem> items = new ArrayList<>(state.items());
        boolean found = false;
        for (int i = 0; i < items.size(); i++) {
            StoredCartItem existing = items.get(i);
            if (!existing.itemKey().equals(itemKey)) {
                continue;
            }
            found = true;
            int quantity = request.quantity() == null ? existing.quantity() : request.quantity();
            if (quantity <= 0) {
                items.remove(i);
            } else {
                ProductRow product = findProduct(existing.productId())
                        .orElseThrow(() -> badRequest("San pham khong ton tai."));
                ensureStock(product, quantity);
                items.set(i, new StoredCartItem(existing.itemKey(), product.id(), product.shopId(), quantity,
                        request.customOptionsJson() == null ? existing.customOptionsJson() : normalize(request.customOptionsJson()),
                        request.note() == null ? existing.note() : normalize(request.note())));
            }
            break;
        }
        if (!found) {
            throw notFound("Khong tim thay san pham trong gio hang.");
        }
        saveCartState(buyerId, new CartState(items));
        return getCart(buyerId);
    }

    public CartResponse deleteCartItem(Long buyerIdHeader, String itemKey) {
        long buyerId = buyerIdOrDefault(buyerIdHeader);
        CartState state = loadCartState(buyerId);
        List<StoredCartItem> items = state.items().stream()
                .filter(item -> !item.itemKey().equals(itemKey))
                .toList();
        saveCartState(buyerId, new CartState(items));
        return getCart(buyerId);
    }

    public CartResponse clearCart(Long buyerIdHeader) {
        long buyerId = buyerIdOrDefault(buyerIdHeader);
        saveCartState(buyerId, new CartState(List.of()));
        return getCart(buyerId);
    }

    public CheckoutSummaryResponse getCheckoutSummary(Long buyerIdHeader, Integer districtId, String wardCode) {
        CartResponse cart = getCart(buyerIdHeader);
        List<ShopCheckoutSummary> shopSummaries = cart.shops().stream()
                .map(group -> {
                    int itemCount = group.items().stream().mapToInt(CartLine::quantity).sum();
                    BigDecimal shippingFee = computeShippingFee(group.shopId(), itemCount, group.subtotal(), districtId, wardCode);
                    return new ShopCheckoutSummary(
                            group.shopId(),
                            group.shopName(),
                            group.subtotal(),
                            shippingFee,
                            money(group.subtotal().add(shippingFee)),
                            itemCount,
                            "GHN Standard"
                    );
                })
                .toList();
        BigDecimal shippingTotal = shopSummaries.stream()
                .map(ShopCheckoutSummary::shippingFee)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal grandTotal = money(cart.subtotal().add(shippingTotal));
        String message = shopSummaries.isEmpty()
                ? "Gio hang dang trong."
                : "Don co the duoc giao thanh nhieu kien tu nhieu shop.";
        return new CheckoutSummaryResponse(
                shopSummaries,
                cart.subtotal(),
                money(shippingTotal),
                grandTotal,
                cart.canCheckout(),
                message
        );
    }

    @Transactional
    public CheckoutResponse checkout(Long buyerIdHeader, CheckoutRequest request) {
        long buyerId = buyerIdOrDefault(buyerIdHeader);
        String paymentMethod = normalizePaymentMethod(request.paymentMethod());
        requireText(request.receiverName(), "Vui long nhap ten nguoi nhan.");
        requireText(request.phone(), "Vui long nhap so dien thoai.");
        requireText(request.address(), "Vui long nhap dia chi.");

        CartResponse cart = getCart(buyerId);
        if (StringUtils.hasText(request.idempotencyKey())) {
            Optional<CheckoutResponse> existing = findCheckoutByIdempotency(buyerId, request.idempotencyKey());
            if (existing.isPresent()) {
                return existing.get();
            }
        }
        Integer districtId = parseInteger(request.district());
        CheckoutSummaryResponse summary = getCheckoutSummary(buyerId, districtId, request.ward());
        if (!summary.canCheckout()) {
            throw badRequest("Gio hang chua san sang checkout. Vui long kiem tra ton kho hoac trang thai san pham.");
        }
        BigDecimal discountAmount = calculateVoucherDiscount(request.voucherCode(), summary.subtotal());
        GiftWrapSnapshot giftWrap = findGiftWrap(request.giftWrapTierId());
        int rewardPointsUsed = Math.max(0, request.rewardPointsUsed() == null ? 0 : request.rewardPointsUsed());
        Integer availableRewardPoints = jdbcTemplate.queryForObject(
                "SELECT COALESCE(`reward_points`, 0) FROM `Users` WHERE `id` = ?", Integer.class, buyerId);
        if (rewardPointsUsed > (availableRewardPoints == null ? 0 : availableRewardPoints)) {
            throw badRequest("Số điểm thưởng sử dụng vượt quá số điểm hiện có.");
        }
        BigDecimal rewardDiscount = money(BigDecimal.valueOf(rewardPointsUsed));
        BigDecimal adjustedTotal = money(summary.grandTotal().add(giftWrap.price()).subtract(discountAmount).subtract(rewardDiscount));
        if (adjustedTotal.compareTo(BigDecimal.ZERO) < 0) {
            adjustedTotal = BigDecimal.ZERO;
        }

        long orderId = nextId("Orders");
        String paymentStatus = "VNPAY".equals(paymentMethod) ? "PENDING" : "COD_PENDING";
        String orderStatus = "VNPAY".equals(paymentMethod) ? "PENDING_PAYMENT" : "PROCESSING";
        String shippingAddress = buildShippingAddress(request);
        jdbcTemplate.update("""
                INSERT INTO `Orders` (`id`, `buyer_id`, `total_price`, `status`, `created_at`, `shipping_address`,
                  `receiver_name`, `phone_number`, `subtotal_price`, `shipping_fee`, `payment_method`, `payment_status`,
                  `receiver_phone`, `receiver_province`, `receiver_district`, `receiver_ward`, `receiver_address`,
                  `voucher_code`, `discount_amount`, `gift_wrap_tier_id`, `gift_wrap_snapshot`, `gift_message`,
                  `reward_points_used`, `idempotency_key`, `updated_at`)
                VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
                """,
                orderId,
                buyerId,
                adjustedTotal,
                orderStatus,
                shippingAddress,
                request.receiverName(),
                request.phone(),
                summary.subtotal(),
                summary.shippingTotal(),
                paymentMethod,
                paymentStatus,
                request.phone(),
                displayLocationName(request.provinceName(), request.province()),
                displayLocationName(request.districtName(), request.district()),
                displayLocationName(request.wardName(), request.ward()),
                normalize(request.address()),
                normalize(request.voucherCode()),
                discountAmount.add(rewardDiscount),
                request.giftWrapTierId(),
                giftWrap.snapshot(),
                normalize(request.giftMessage()),
                rewardPointsUsed,
                normalize(request.idempotencyKey()));
        if (rewardPointsUsed > 0) {
            jdbcTemplate.update("UPDATE `Users` SET `reward_points` = GREATEST(COALESCE(`reward_points`, 0) - ?, 0) WHERE `id` = ?",
                    rewardPointsUsed, buyerId);
        }

        Map<Long, ShopCheckoutSummary> feeByShop = new LinkedHashMap<>();
        for (ShopCheckoutSummary shopSummary : summary.shopSummaries()) {
            feeByShop.put(shopSummary.shopId(), shopSummary);
        }

        BigDecimal effectiveCommissionRate = resolveCommissionRate();
        for (ShopCartGroup group : cart.shops()) {
            ShopCheckoutSummary shopSummary = feeByShop.get(group.shopId());
            long shopOrderId = nextId("shop_orders");
            BigDecimal commission = money(group.subtotal().multiply(effectiveCommissionRate));
            BigDecimal payout = money(group.subtotal().subtract(commission));
            BigDecimal codAmount = "COD".equals(paymentMethod) ? shopSummary.total() : BigDecimal.ZERO;
            String shopStatus = "VNPAY".equals(paymentMethod) ? "PENDING_PAYMENT" : "NEW";
            jdbcTemplate.update("""
                    INSERT INTO `shop_orders` (`id`, `order_id`, `shop_id`, `shop_name_snapshot`, `item_subtotal`, `shipping_fee`,
                      `commission_amount`, `payout_amount`, `cod_amount`, `status`, `created_at`, `updated_at`)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                    """,
                    shopOrderId,
                    orderId,
                    group.shopId(),
                    group.shopName(),
                    group.subtotal(),
                    shopSummary.shippingFee(),
                    commission,
                    payout,
                    money(codAmount),
                    shopStatus);

            for (CartLine line : group.items()) {
                long itemId = nextId("OrderItems");
                jdbcTemplate.update("""
                        INSERT INTO `OrderItems` (`id`, `order_id`, `product_id`, `shop_id`, `quantity`, `price_at_purchase`,
                          `merchant_status`, `shop_order_id`, `product_name_snapshot`, `shop_name_snapshot`, `custom_options_json`, `note`)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                        itemId,
                        orderId,
                        line.productId(),
                        line.shopId(),
                        line.quantity(),
                        line.unitPrice(),
                        shopStatus,
                        shopOrderId,
                        line.productName(),
                        line.shopName(),
                        line.customOptionsJson(),
                        line.note());
                jdbcTemplate.update("""
                        UPDATE `Storage`
                        SET `reserved_quantity` = COALESCE(`reserved_quantity`, 0) + ?,
                            `last_updated` = CURRENT_TIMESTAMP
                        WHERE `product_id` = ?
                        LIMIT 1
                        """, line.quantity(), line.productId());
                jdbcTemplate.update("""
                        INSERT INTO `inventory_logs` (`id`, `product_id`, `change_qty`, `reason`, `note`)
                        VALUES (?, ?, ?, 'CHECKOUT_RESERVE', ?)
                        """, nextId("inventory_logs"), line.productId(), -line.quantity(), "Reserve cho order #" + orderId);
            }
        }

        String transactionRef = "ORDER-" + orderId + "-" + System.currentTimeMillis();
        long transactionId = nextId("payment_transactions");
        jdbcTemplate.update("""
                INSERT INTO `payment_transactions` (`id`, `order_id`, `provider`, `method`, `transaction_ref`,
                  `amount`, `status`, `raw_payload`, `created_at`, `updated_at`)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """,
                transactionId,
                orderId,
                paymentMethod,
                paymentMethod,
                transactionRef,
                adjustedTotal,
                paymentStatus,
                "{}");
        jdbcTemplate.update("""
                INSERT INTO `Payments` (`id`, `order_id`, `method`, `transaction_id`, `amount`, `status`)
                VALUES (?, ?, ?, ?, ?, 'pending')
                """,
                nextId("Payments"),
                orderId,
                paymentMethod,
                transactionRef,
                adjustedTotal);

        clearCart(buyerId);
        String nextAction = "VNPAY".equals(paymentMethod) ? "PAYMENT_REQUIRED" : "VIEW_ORDER";
        return new CheckoutResponse(orderId, orderStatus, paymentStatus, paymentMethod,
                adjustedTotal, transactionRef, nextAction, "/order-detail.html?id=" + orderId);
    }

    public PaymentCreateResponse createVnpayPayment(Long buyerIdHeader, PaymentCreateRequest request) {
        if (request.orderId() == null) {
            throw badRequest("Thieu ma don hang.");
        }
        long buyerId = buyerIdOrDefault(buyerIdHeader);
        PaymentOrder order = findPaymentOrder(request.orderId())
                .orElseThrow(() -> notFound("Khong tim thay don hang."));
        if (!order.buyerId().equals(buyerId)) {
            throw notFound("Khong tim thay don hang.");
        }
        if (!"VNPAY".equalsIgnoreCase(order.paymentMethod())) {
            throw badRequest("Don hang nay khong dung phuong thuc VNPay.");
        }
        if ("PAID".equalsIgnoreCase(order.paymentStatus())) {
            return new PaymentCreateResponse(order.id(), "/payment-result.html?orderId=" + order.id() + "&alreadyPaid=true",
                    order.total(), "ALREADY_PAID");
        }
        if (!"PENDING_PAYMENT".equalsIgnoreCase(order.status()) || !"PENDING".equalsIgnoreCase(order.paymentStatus())) {
            throw badRequest("Don hang khong con o trang thai cho thanh toan.");
        }
        if (!vnpayMockEnabled) {
            requireVnpaySandboxConfig();
        }

        String transactionRef = findTransactionRef(order.id())
                .orElseGet(() -> createPaymentTransaction(order.id(), order.total(), "VNPAY", "PENDING"));
        Map<String, String> params = new TreeMap<>();
        params.put("orderId", String.valueOf(order.id()));
        params.put("vnp_Amount", order.total().multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP).toPlainString());
        params.put("vnp_OrderInfo", "Thanh toan don handmade #" + order.id());
        params.put("vnp_PayDate", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));
        params.put("vnp_ResponseCode", "00");
        params.put("vnp_TmnCode", vnpayTmnCode);
        params.put("vnp_TransactionNo", "MOCK" + order.id());
        params.put("vnp_TxnRef", transactionRef);

        String paymentUrl;
        if (vnpayMockEnabled) {
            params.put("mock", "true");
            params.put("vnp_SecureHash", signParams(params));
            paymentUrl = appendQuery(vnpayReturnUrl, params);
        } else {
            params.remove("orderId");
            params.remove("mock");
            params.remove("vnp_ResponseCode");
            params.remove("vnp_TransactionNo");
            params.put("vnp_Command", "pay");
            params.put("vnp_CreateDate", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));
            params.put("vnp_CurrCode", "VND");
            params.put("vnp_IpAddr", "127.0.0.1");
            params.put("vnp_Locale", "vn");
            params.put("vnp_OrderType", "other");
            params.put("vnp_ReturnUrl", vnpayReturnUrl);
            params.put("vnp_Version", "2.1.0");
            params.put("vnp_SecureHash", signParams(params));
            paymentUrl = appendQuery(vnpayPayUrl, params);
        }
        return new PaymentCreateResponse(order.id(), paymentUrl, order.total(), "CREATED");
    }

    @Transactional
    public PaymentReturnResponse verifyVnpayReturn(Map<String, String> params) {
        Long orderId = resolveOrderId(params);
        if (orderId == null) {
            return new PaymentReturnResponse(false, null, "UNKNOWN", "UNKNOWN", BigDecimal.ZERO,
                    "Khong xac dinh duoc ma don hang.");
        }
        Optional<PaymentOrder> orderOptional = findPaymentOrder(orderId);
        if (orderOptional.isEmpty()) {
            return new PaymentReturnResponse(false, orderId, "UNKNOWN", "UNKNOWN", BigDecimal.ZERO,
                    "Khong tim thay don hang.");
        }
        PaymentOrder order = orderOptional.get();
        if ("PAID".equalsIgnoreCase(order.paymentStatus())) {
            return new PaymentReturnResponse(true, order.id(), "PAID", order.status(), order.total(),
                    "Don hang da duoc thanh toan truoc do.");
        }
        if (!verifySecureHash(params)) {
            return new PaymentReturnResponse(false, order.id(), order.paymentStatus(), order.status(), order.total(),
                    "Chu ky VNPay khong hop le.");
        }
        if (!vnpayAmountMatches(order.total(), params.get("vnp_Amount"))) {
            return new PaymentReturnResponse(false, order.id(), order.paymentStatus(), order.status(), order.total(),
                    "So tien VNPay tra ve khong khop voi tong don hien tai.");
        }
        if (!"PENDING_PAYMENT".equalsIgnoreCase(order.status()) || !"PENDING".equalsIgnoreCase(order.paymentStatus())) {
            return new PaymentReturnResponse(false, order.id(), order.paymentStatus(), order.status(), order.total(),
                    "Don hang khong con o trang thai cho thanh toan.");
        }

        boolean paid = "00".equals(params.getOrDefault("vnp_ResponseCode", "99"));
        String rawPayload = toJson(params);
        if (paid) {
            jdbcTemplate.update("""
                    UPDATE `Orders`
                    SET `payment_status` = 'PAID', `status` = 'PROCESSING', `updated_at` = CURRENT_TIMESTAMP
                    WHERE `id` = ?
                    """, order.id());
            jdbcTemplate.update("""
                    UPDATE `shop_orders`
                    SET `status` = 'NEW', `updated_at` = CURRENT_TIMESTAMP
                    WHERE `order_id` = ? AND `status` = 'PENDING_PAYMENT'
                    """, order.id());
            jdbcTemplate.update("""
                    UPDATE `payment_transactions`
                    SET `status` = 'SUCCESS', `provider_transaction_id` = ?, `raw_payload` = ?, `updated_at` = CURRENT_TIMESTAMP
                    WHERE `order_id` = ? AND `method` = 'VNPAY'
                    """, params.get("vnp_TransactionNo"), rawPayload, order.id());
            jdbcTemplate.update("""
                    UPDATE `Payments`
                    SET `status` = 'successed'
                    WHERE `order_id` = ? AND `method` = 'VNPAY'
                    """, order.id());
            return new PaymentReturnResponse(true, order.id(), "PAID", "PROCESSING", order.total(),
                    "Thanh toan VNPay thanh cong.");
        }

        cancelOrderInternal(order.id(), "FAILED");
        jdbcTemplate.update("""
                UPDATE `payment_transactions`
                SET `status` = 'FAILED', `raw_payload` = ?, `updated_at` = CURRENT_TIMESTAMP
                WHERE `order_id` = ? AND `method` = 'VNPAY'
                """, rawPayload, order.id());
        jdbcTemplate.update("""
                UPDATE `Payments`
                SET `status` = 'failed'
                WHERE `order_id` = ? AND `method` = 'VNPAY'
                """, order.id());
        return new PaymentReturnResponse(false, order.id(), "FAILED", "CANCELLED", order.total(),
                "Thanh toan VNPay that bai.");
    }

    public ShippingFeeResponse calculateShippingFee(ShippingFeeRequest request) {
        long shopId = request.shopId() == null ? defaultShopId : request.shopId();
        int quantity = request.quantity() == null || request.quantity() <= 0 ? 1 : request.quantity();
        BigDecimal fee = computeShippingFee(shopId, quantity, request.subtotal() == null ? BigDecimal.ZERO : request.subtotal(), parseInteger(request.district()), request.ward());
        return new ShippingFeeResponse(shopId, "GHN", "GHN Standard", fee, "GHN_FEE");
    }

    public List<MasterDataOption> getProvinces() {
        return ghnService.getProvinces();
    }

    public List<MasterDataOption> getDistricts(Integer provinceId) {
        return ghnService.getDistricts(provinceId);
    }

    public List<MasterDataOption> getWards(Integer districtId) {
        return ghnService.getWards(districtId);
    }

    public List<BuyerOrderSummary> getBuyerOrders(Long buyerIdHeader) {
        long buyerId = buyerIdOrDefault(buyerIdHeader);
        try {
            return jdbcTemplate.query("""
                    SELECT o.`id`, o.`status`, o.`payment_status`, o.`payment_method`, o.`total_price`, o.`receiver_name`,
                           COALESCE(o.`receiver_phone`, o.`phone_number`) AS receiver_phone,
                           o.`created_at`,
                           COUNT(DISTINCT so.`id`) AS shop_count,
                           COALESCE(SUM(oi.`quantity`), 0) AS item_count
                    FROM `Orders` o
                    LEFT JOIN `shop_orders` so ON so.`order_id` = o.`id`
                    LEFT JOIN `OrderItems` oi ON oi.`order_id` = o.`id`
                    WHERE o.`buyer_id` = ?
                    GROUP BY o.`id`, o.`status`, o.`payment_status`, o.`payment_method`, o.`total_price`,
                             o.`receiver_name`, receiver_phone, o.`created_at`
                    ORDER BY o.`created_at` DESC, o.`id` DESC
                    """, this::mapBuyerOrderSummary, buyerId);
        } catch (DataAccessException ex) {
            throw serviceUnavailable();
        }
    }

    public OrderDetailResponse getBuyerOrderDetail(Long buyerIdHeader, Long orderId) {
        return getOrderDetailInternal(orderId, buyerIdOrDefault(buyerIdHeader));
    }

    @Transactional
    public OrderDetailResponse cancelBuyerOrder(Long buyerIdHeader, Long orderId) {
        long buyerId = buyerIdOrDefault(buyerIdHeader);
        OrderDetailResponse order = getOrderDetailInternal(orderId, buyerId);
        ensureOrderCancellable(order);
        cancelOrderInternal(orderId, "CANCELLED");
        return getOrderDetailInternal(orderId, buyerId);
    }

    @Transactional
    public OrderDetailResponse cancelBuyerShopOrder(Long buyerIdHeader, Long orderId, Long shopOrderId) {
        long buyerId = buyerIdOrDefault(buyerIdHeader);
        OrderDetailResponse order = getOrderDetailInternal(orderId, buyerId);
        ShopOrderDetail shopOrder = findShopOrderInOrder(order, shopOrderId);
        ensureShopOrderCancellable(order, shopOrder);
        cancelShopOrderInternal(order.id(), shopOrder);
        return getOrderDetailInternal(orderId, buyerId);
    }

    @Transactional
    public OrderDetailResponse cancelAdminOrder(Long orderId) {
        OrderDetailResponse order = getOrderDetailInternal(orderId, null);
        ensureOrderCancellable(order);
        cancelOrderInternal(orderId, "CANCELLED");
        return getOrderDetailInternal(orderId, null);
    }

    @Transactional
    public OrderDetailResponse cancelAdminShopOrder(Long orderId, Long shopOrderId) {
        OrderDetailResponse order = getOrderDetailInternal(orderId, null);
        ShopOrderDetail shopOrder = findShopOrderInOrder(order, shopOrderId);
        ensureShopOrderCancellable(order, shopOrder);
        cancelShopOrderInternal(order.id(), shopOrder);
        return getOrderDetailInternal(orderId, null);
    }

    public List<SellerOrderResponse> getSellerOrders(Long shopIdHeader) {
        long shopId = shopIdOrDefault(shopIdHeader);
        try {
            return jdbcTemplate.query("""
                    SELECT so.`id` AS shop_order_id, so.`order_id`, so.`shop_id`, so.`shop_name_snapshot`, so.`status`,
                           so.`item_subtotal`, so.`shipping_fee`, so.`cod_amount`, so.`created_at`,
                           o.`status` AS order_status, o.`payment_status`, o.`payment_method`, o.`receiver_name`,
                           COALESCE(o.`receiver_phone`, o.`phone_number`) AS receiver_phone, o.`shipping_address`
                    FROM `shop_orders` so
                    JOIN `Orders` o ON o.`id` = so.`order_id`
                    WHERE so.`shop_id` = ?
                    ORDER BY so.`created_at` DESC, so.`id` DESC
                    """, (rs, rowNum) -> mapSellerOrderResponse(rs), shopId);
        } catch (DataAccessException ex) {
            throw serviceUnavailable();
        }
    }

    @Transactional
    public SellerOrderResponse updateSellerOrderStatus(Long shopIdHeader, Long shopOrderId, SellerStatusUpdateRequest request) {
        long shopId = shopIdOrDefault(shopIdHeader);
        String nextStatus = normalize(request.status()).toUpperCase(Locale.ROOT);
        if (!SELLER_STATUSES.contains(nextStatus)) {
            throw badRequest("Trang thai shop order khong hop le.");
        }
        SellerOrderGuard guard = findSellerOrderGuard(shopId, shopOrderId)
                .orElseThrow(() -> notFound("Khong tim thay shop order."));
        if (nextStatus.equalsIgnoreCase(guard.currentStatus())) {
            return getSellerOrder(shopId, shopOrderId);
        }
        guardSellerActionAllowed(guard, nextStatus);
        if ("CANCELLED".equals(nextStatus) || "DELIVERY_FAILED".equals(nextStatus)) {
            releaseReservationsForShopOrder(shopOrderId);
        }
        jdbcTemplate.update("""
                UPDATE `shop_orders`
                SET `status` = ?, `updated_at` = CURRENT_TIMESTAMP
                WHERE `id` = ? AND `shop_id` = ?
                """, nextStatus, shopOrderId, shopId);
        jdbcTemplate.update("""
                UPDATE `OrderItems`
                SET `merchant_status` = ?
                WHERE `shop_order_id` = ?
                """, nextStatus, shopOrderId);
        if ("COMPLETED".equals(nextStatus)) {
            finalizeReservationsForShopOrder(shopOrderId);
        }
        refreshOrderAggregateStatus(guard.orderId());
        return getSellerOrder(shopId, shopOrderId);
    }

    @Transactional
    public SellerOrderResponse createGhnShipment(Long shopIdHeader, Long shopOrderId, SellerShipmentRequest request) {
        long shopId = shopIdOrDefault(shopIdHeader);
        SellerOrderGuard guard = findSellerOrderGuard(shopId, shopOrderId)
                .orElseThrow(() -> notFound("Khong tim thay shop order."));
        if ("SHIPPING".equalsIgnoreCase(guard.currentStatus())) {
            return getSellerOrder(shopId, shopOrderId);
        }
        guardSellerActionAllowed(guard, "SHIPPING");

        Optional<Long> existingShipment = findExistingShipment(shopOrderId);
        if (existingShipment.isEmpty()) {
            String trackingCode = "GHN" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + shopOrderId;
            jdbcTemplate.update("""
                    INSERT INTO `shipments` (`id`, `shop_order_id`, `provider`, `tracking_code`, `service_name`, `fee`,
                      `cod_amount`, `status`, `raw_payload`, `created_at`, `updated_at`)
                    VALUES (?, ?, 'GHN', ?, ?, ?, ?, 'CREATED', ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                    """,
                    nextId("shipments"),
                    shopOrderId,
                    trackingCode,
                    StringUtils.hasText(request.serviceName()) ? request.serviceName() : "GHN Standard",
                    guard.shippingFee(),
                    guard.codAmount(),
                    toJson(Map.of("note", normalize(request.note()), "mock", true)));
        }
        jdbcTemplate.update("""
                UPDATE `shop_orders`
                SET `status` = 'SHIPPING', `updated_at` = CURRENT_TIMESTAMP
                WHERE `id` = ? AND `shop_id` = ?
                """, shopOrderId, shopId);
        jdbcTemplate.update("""
                UPDATE `OrderItems`
                SET `merchant_status` = 'SHIPPING'
                WHERE `shop_order_id` = ?
                """, shopOrderId);
        refreshOrderAggregateStatus(guard.orderId());
        return getSellerOrder(shopId, shopOrderId);
    }

    @Transactional
    public ShipmentDetail updateShipmentStatus(Long shipmentId, ShipmentStatusRequest request) {
        String status = normalize(request.status());
        if (!StringUtils.hasText(status)) {
            throw badRequest("Vui lòng nhập trạng thái vận chuyển.");
        }
        status = status.toUpperCase();
        Map<String, Object> row = jdbcTemplate.queryForMap("""
                SELECT sh.`shop_order_id`, so.`shop_id`, so.`order_id`
                FROM `shipments` sh
                JOIN `shop_orders` so ON so.`id` = sh.`shop_order_id`
                WHERE sh.`id` = ?
                """, shipmentId);
        Long shopOrderId = ((Number) row.get("shop_order_id")).longValue();
        Long shopId = ((Number) row.get("shop_id")).longValue();
        Long orderId = ((Number) row.get("order_id")).longValue();
        jdbcTemplate.update("""
                UPDATE `shipments`
                SET `status` = ?, `tracking_code` = COALESCE(?, `tracking_code`), `updated_at` = CURRENT_TIMESTAMP
                WHERE `id` = ?
                """, status, normalize(request.trackingCode()), shipmentId);

        String shopStatus = switch (status) {
            case "DELIVERED" -> "COMPLETED";
            case "FAILED", "RETURNING", "RETURNED" -> "DELIVERY_FAILED";
            case "IN_TRANSIT", "PICKED", "SHIPPING" -> "SHIPPING";
            default -> null;
        };
        if (shopStatus != null) {
            jdbcTemplate.update("""
                    UPDATE `shop_orders`
                    SET `status` = ?, `updated_at` = CURRENT_TIMESTAMP
                    WHERE `id` = ?
                    """, shopStatus, shopOrderId);
            jdbcTemplate.update("UPDATE `OrderItems` SET `merchant_status` = ? WHERE `shop_order_id` = ?",
                    shopStatus, shopOrderId);
            if ("COMPLETED".equals(shopStatus)) {
                finalizeReservationsForShopOrder(shopOrderId);
            }
            refreshOrderAggregateStatus(orderId);
        }
        List<ShipmentDetail> shipments = getShipmentsForShopOrder(shopOrderId);
        return shipments.stream().filter(item -> item.id().equals(shipmentId)).findFirst()
                .orElseThrow(() -> notFound("Không tìm thấy vận đơn."));
    }

    public List<OrderDetailResponse> getAdminOrders() {
        try {
            List<Long> ids = jdbcTemplate.queryForList("""
                    SELECT `id`
                    FROM `Orders`
                    ORDER BY `created_at` DESC, `id` DESC
                    LIMIT 50
                    """, Long.class);
            return ids.stream().map(id -> getOrderDetailInternal(id, null)).toList();
        } catch (DataAccessException ex) {
            throw serviceUnavailable();
        }
    }

    private void ensureOrderCancellable(OrderDetailResponse order) {
        if (order == null) {
            throw notFound("Khong tim thay don hang.");
        }
        if ("PAID".equalsIgnoreCase(order.paymentStatus())) {
            throw badRequest("Don da thanh toan can quy trinh hoan tien rieng.");
        }
        if ("CANCELLED".equalsIgnoreCase(order.status()) || "COMPLETED".equalsIgnoreCase(order.status())) {
            throw badRequest("Don hang khong the huy o trang thai hien tai.");
        }
        boolean hasNonCancellableShopOrder = order.shopOrders().stream()
                .map(ShopOrderDetail::status)
                .map(status -> normalize(status).toUpperCase(Locale.ROOT))
                .anyMatch(status -> !BUYER_CANCELLABLE_SHOP_STATUSES.contains(status));
        if (hasNonCancellableShopOrder) {
            throw badRequest("Don hang da vao buoc xu ly/van chuyen nen khong the huy tu dong.");
        }
    }

    private ShopOrderDetail findShopOrderInOrder(OrderDetailResponse order, Long shopOrderId) {
        if (order == null) {
            throw notFound("Khong tim thay don hang.");
        }
        if (shopOrderId == null) {
            throw badRequest("Thieu ma goi hang shop.");
        }
        return order.shopOrders().stream()
                .filter(shopOrder -> shopOrder.id().equals(shopOrderId))
                .findFirst()
                .orElseThrow(() -> notFound("Khong tim thay goi hang shop trong don nay."));
    }

    private void ensureShopOrderCancellable(OrderDetailResponse order, ShopOrderDetail shopOrder) {
        if ("PAID".equalsIgnoreCase(order.paymentStatus())) {
            throw badRequest("Don da thanh toan can quy trinh hoan tien rieng.");
        }
        if ("CANCELLED".equalsIgnoreCase(order.status()) || "COMPLETED".equalsIgnoreCase(order.status())) {
            throw badRequest("Don hang khong the huy o trang thai hien tai.");
        }
        String status = normalize(shopOrder.status()).toUpperCase(Locale.ROOT);
        if (!BUYER_CANCELLABLE_SHOP_STATUSES.contains(status)) {
            throw badRequest("Goi hang nay da vao buoc xu ly/van chuyen nen khong the huy tu dong.");
        }
    }

    private void cancelOrderInternal(Long orderId, String paymentStatus) {
        releaseReservationsForOrder(orderId);
        jdbcTemplate.update("""
                UPDATE `shop_orders`
                SET `status` = 'CANCELLED', `updated_at` = CURRENT_TIMESTAMP
                WHERE `order_id` = ? AND `status` NOT IN ('COMPLETED', 'CANCELLED')
                """, orderId);
        jdbcTemplate.update("""
                UPDATE `OrderItems`
                SET `merchant_status` = 'CANCELLED'
                WHERE `order_id` = ? AND COALESCE(`merchant_status`, '') NOT IN ('COMPLETED', 'CANCELLED')
                """, orderId);
        jdbcTemplate.update("""
                UPDATE `Orders`
                SET `status` = 'CANCELLED', `payment_status` = ?, `updated_at` = CURRENT_TIMESTAMP
                WHERE `id` = ?
                """, paymentStatus, orderId);
        jdbcTemplate.update("""
                UPDATE `payment_transactions`
                SET `status` = CASE WHEN `status` = 'SUCCESS' THEN `status` ELSE 'CANCELLED' END,
                    `updated_at` = CURRENT_TIMESTAMP
                WHERE `order_id` = ?
                """, orderId);
        jdbcTemplate.update("""
                UPDATE `Payments`
                SET `status` = CASE WHEN `status` = 'successed' THEN `status` ELSE 'failed' END
                WHERE `order_id` = ?
                """, orderId);
    }

    private void cancelShopOrderInternal(Long orderId, ShopOrderDetail shopOrder) {
        releaseReservationsForShopOrder(shopOrder.id());
        jdbcTemplate.update("""
                UPDATE `shop_orders`
                SET `status` = 'CANCELLED', `updated_at` = CURRENT_TIMESTAMP
                WHERE `id` = ? AND `order_id` = ? AND `status` NOT IN ('COMPLETED', 'CANCELLED')
                """, shopOrder.id(), orderId);
        jdbcTemplate.update("""
                UPDATE `OrderItems`
                SET `merchant_status` = 'CANCELLED'
                WHERE `shop_order_id` = ? AND COALESCE(`merchant_status`, '') NOT IN ('COMPLETED', 'CANCELLED')
                """, shopOrder.id());
        BigDecimal packageTotal = money(shopOrder.itemSubtotal().add(shopOrder.shippingFee()));
        jdbcTemplate.update("""
                UPDATE `Orders`
                SET `subtotal_price` = GREATEST(COALESCE(`subtotal_price`, 0) - ?, 0),
                    `shipping_fee` = GREATEST(COALESCE(`shipping_fee`, 0) - ?, 0),
                    `total_price` = GREATEST(COALESCE(`total_price`, 0) - ?, 0),
                    `updated_at` = CURRENT_TIMESTAMP
                WHERE `id` = ?
                """, shopOrder.itemSubtotal(), shopOrder.shippingFee(), packageTotal, orderId);
        refreshOrderAggregateStatus(orderId);
        syncPaymentAfterPartialCancellation(orderId);
    }

    private void syncPaymentAfterPartialCancellation(Long orderId) {
        BigDecimal total = currentOrderTotal(orderId);
        boolean allCancelled = allShopOrdersCancelled(orderId);
        if (allCancelled) {
            jdbcTemplate.update("""
                    UPDATE `Orders`
                    SET `payment_status` = 'CANCELLED', `updated_at` = CURRENT_TIMESTAMP
                    WHERE `id` = ?
                    """, orderId);
            jdbcTemplate.update("""
                    UPDATE `payment_transactions`
                    SET `status` = CASE WHEN `status` = 'SUCCESS' THEN `status` ELSE 'CANCELLED' END,
                        `updated_at` = CURRENT_TIMESTAMP
                    WHERE `order_id` = ?
                    """, orderId);
            jdbcTemplate.update("""
                    UPDATE `Payments`
                    SET `status` = CASE WHEN `status` = 'successed' THEN `status` ELSE 'failed' END
                    WHERE `order_id` = ?
                    """, orderId);
            return;
        }
        jdbcTemplate.update("""
                UPDATE `Payments`
                SET `amount` = ?
                WHERE `order_id` = ? AND `status` <> 'successed'
                """, total, orderId);
        jdbcTemplate.update("""
                UPDATE `payment_transactions`
                SET `status` = 'CANCELLED', `updated_at` = CURRENT_TIMESTAMP
                WHERE `order_id` = ? AND `method` = 'VNPAY' AND `status` = 'PENDING'
                """, orderId);
    }

    private BigDecimal currentOrderTotal(Long orderId) {
        BigDecimal total = jdbcTemplate.queryForObject(
                "SELECT `total_price` FROM `Orders` WHERE `id` = ?",
                BigDecimal.class,
                orderId);
        return money(total);
    }

    private boolean allShopOrdersCancelled(Long orderId) {
        Integer activeCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM `shop_orders`
                WHERE `order_id` = ? AND `status` <> 'CANCELLED'
                """, Integer.class, orderId);
        return activeCount == null || activeCount == 0;
    }

    private void releaseReservationsForOrder(Long orderId) {
        applyInventoryAdjustments("""
                SELECT `product_id`, SUM(`quantity`) AS `quantity`
                FROM `OrderItems`
                WHERE `order_id` = ? AND COALESCE(`merchant_status`, '') NOT IN ('COMPLETED', 'CANCELLED', 'DELIVERY_FAILED')
                GROUP BY `product_id`
                """, false, orderId);
    }

    private void releaseReservationsForShopOrder(Long shopOrderId) {
        applyInventoryAdjustments("""
                SELECT `product_id`, SUM(`quantity`) AS `quantity`
                FROM `OrderItems`
                WHERE `shop_order_id` = ? AND COALESCE(`merchant_status`, '') NOT IN ('COMPLETED', 'CANCELLED', 'DELIVERY_FAILED')
                GROUP BY `product_id`
                """, false, shopOrderId);
    }

    private void finalizeReservationsForShopOrder(Long shopOrderId) {
        applyInventoryAdjustments("""
                SELECT `product_id`, SUM(`quantity`) AS `quantity`
                FROM `OrderItems`
                WHERE `shop_order_id` = ?
                GROUP BY `product_id`
                """, true, shopOrderId);
    }

    private void applyInventoryAdjustments(String sql, boolean finalizeSale, Object... args) {
        List<InventoryAdjustment> adjustments = jdbcTemplate.query(sql, (rs, rowNum) ->
                new InventoryAdjustment(rs.getLong("product_id"), rs.getInt("quantity")), args);
        for (InventoryAdjustment adjustment : adjustments) {
            if (adjustment.quantity() <= 0) {
                continue;
            }
            if (finalizeSale) {
                jdbcTemplate.update("""
                        UPDATE `Storage`
                        SET `quantity` = GREATEST(COALESCE(`quantity`, 0) - ?, 0),
                            `reserved_quantity` = GREATEST(COALESCE(`reserved_quantity`, 0) - ?, 0),
                            `last_updated` = CURRENT_TIMESTAMP
                        WHERE `product_id` = ?
                        LIMIT 1
                        """, adjustment.quantity(), adjustment.quantity(), adjustment.productId());
            } else {
                jdbcTemplate.update("""
                        UPDATE `Storage`
                        SET `reserved_quantity` = GREATEST(COALESCE(`reserved_quantity`, 0) - ?, 0),
                            `last_updated` = CURRENT_TIMESTAMP
                        WHERE `product_id` = ?
                        LIMIT 1
                        """, adjustment.quantity(), adjustment.productId());
            }
        }
    }

    private OrderDetailResponse getOrderDetailInternal(Long orderId, Long buyerIdOrNull) {
        if (orderId == null) {
            throw badRequest("Thieu ma don hang.");
        }
        try {
            String where = buyerIdOrNull == null ? "WHERE `id` = ?" : "WHERE `id` = ? AND `buyer_id` = ?";
            Object[] args = buyerIdOrNull == null ? new Object[]{orderId} : new Object[]{orderId, buyerIdOrNull};
            OrderHeader header = jdbcTemplate.queryForObject("""
                    SELECT `id`, `buyer_id`, `status`, `payment_status`, `payment_method`, `receiver_name`,
                           COALESCE(`receiver_phone`, `phone_number`) AS receiver_phone, `shipping_address`,
                           `subtotal_price`, `shipping_fee`, `total_price`, `created_at`
                    FROM `Orders`
                    """ + where, (rs, rowNum) -> new OrderHeader(
                    rs.getLong("id"),
                    rs.getLong("buyer_id"),
                    rs.getString("status"),
                    rs.getString("payment_status"),
                    rs.getString("payment_method"),
                    rs.getString("receiver_name"),
                    rs.getString("receiver_phone"),
                    rs.getString("shipping_address"),
                    money(rs.getBigDecimal("subtotal_price")),
                    money(rs.getBigDecimal("shipping_fee")),
                    money(rs.getBigDecimal("total_price")),
                    toLocalDateTime(rs.getTimestamp("created_at"))
            ), args);
            if (header == null) {
                throw notFound("Khong tim thay don hang.");
            }
            List<ShopOrderDetail> shopOrders = getShopOrderDetails(orderId);
            List<PaymentTransactionDetail> payments = getPaymentDetails(orderId);
            return new OrderDetailResponse(
                    header.id(),
                    header.buyerId(),
                    header.status(),
                    header.paymentStatus(),
                    header.paymentMethod(),
                    header.receiverName(),
                    header.receiverPhone(),
                    header.shippingAddress(),
                    header.subtotal(),
                    header.shippingFee(),
                    header.total(),
                    header.createdAt(),
                    shopOrders,
                    payments
            );
        } catch (EmptyResultDataAccessException ex) {
            throw notFound("Khong tim thay don hang.");
        } catch (DataAccessException ex) {
            throw serviceUnavailable();
        }
    }

    private SellerOrderResponse getSellerOrder(long shopId, long shopOrderId) {
        return getSellerOrders(shopId).stream()
                .filter(order -> order.shopOrderId().equals(shopOrderId))
                .findFirst()
                .orElseThrow(() -> notFound("Khong tim thay shop order."));
    }

    private SellerOrderResponse mapSellerOrderResponse(ResultSet rs) throws SQLException {
        long shopOrderId = rs.getLong("shop_order_id");
        return new SellerOrderResponse(
                shopOrderId,
                rs.getLong("order_id"),
                rs.getLong("shop_id"),
                rs.getString("shop_name_snapshot"),
                rs.getString("status"),
                rs.getString("order_status"),
                rs.getString("payment_status"),
                rs.getString("payment_method"),
                rs.getString("receiver_name"),
                rs.getString("receiver_phone"),
                rs.getString("shipping_address"),
                money(rs.getBigDecimal("item_subtotal")),
                money(rs.getBigDecimal("shipping_fee")),
                money(rs.getBigDecimal("cod_amount")),
                toLocalDateTime(rs.getTimestamp("created_at")),
                getOrderItemsForShopOrder(shopOrderId),
                getShipmentsForShopOrder(shopOrderId)
        );
    }

    private List<ShopOrderDetail> getShopOrderDetails(Long orderId) {
        return jdbcTemplate.query("""
                SELECT `id`, `shop_id`, `shop_name_snapshot`, `item_subtotal`, `shipping_fee`, `commission_amount`,
                       `payout_amount`, `cod_amount`, `status`
                FROM `shop_orders`
                WHERE `order_id` = ?
                ORDER BY `id`
                """, (rs, rowNum) -> {
            long shopOrderId = rs.getLong("id");
            return new ShopOrderDetail(
                    shopOrderId,
                    rs.getLong("shop_id"),
                    rs.getString("shop_name_snapshot"),
                    money(rs.getBigDecimal("item_subtotal")),
                    money(rs.getBigDecimal("shipping_fee")),
                    money(rs.getBigDecimal("commission_amount")),
                    money(rs.getBigDecimal("payout_amount")),
                    money(rs.getBigDecimal("cod_amount")),
                    rs.getString("status"),
                    getOrderItemsForShopOrder(shopOrderId),
                    getShipmentsForShopOrder(shopOrderId)
            );
        }, orderId);
    }

    private List<OrderItemDetail> getOrderItemsForShopOrder(Long shopOrderId) {
        return jdbcTemplate.query("""
                SELECT `id`, `product_id`, `product_name_snapshot`, `quantity`, `price_at_purchase`,
                       `custom_options_json`, `note`
                FROM `OrderItems`
                WHERE `shop_order_id` = ?
                ORDER BY `id`
                """, (rs, rowNum) -> {
            int quantity = rs.getInt("quantity");
            BigDecimal unitPrice = money(rs.getBigDecimal("price_at_purchase"));
            return new OrderItemDetail(
                    rs.getLong("id"),
                    rs.getLong("product_id"),
                    rs.getString("product_name_snapshot"),
                    quantity,
                    unitPrice,
                    money(unitPrice.multiply(BigDecimal.valueOf(quantity))),
                    rs.getString("custom_options_json"),
                    rs.getString("note")
            );
        }, shopOrderId);
    }

    private List<ShipmentDetail> getShipmentsForShopOrder(Long shopOrderId) {
        return jdbcTemplate.query("""
                SELECT `id`, `provider`, `tracking_code`, `service_name`, `fee`, `cod_amount`, `status`, `created_at`
                FROM `shipments`
                WHERE `shop_order_id` = ?
                ORDER BY `id`
                """, (rs, rowNum) -> new ShipmentDetail(
                rs.getLong("id"),
                rs.getString("provider"),
                rs.getString("tracking_code"),
                rs.getString("service_name"),
                money(rs.getBigDecimal("fee")),
                money(rs.getBigDecimal("cod_amount")),
                rs.getString("status"),
                toLocalDateTime(rs.getTimestamp("created_at"))
        ), shopOrderId);
    }

    private List<PaymentTransactionDetail> getPaymentDetails(Long orderId) {
        return jdbcTemplate.query("""
                SELECT `id`, `provider`, `method`, `transaction_ref`, `provider_transaction_id`, `amount`, `status`, `created_at`
                FROM `payment_transactions`
                WHERE `order_id` = ?
                ORDER BY `id`
                """, (rs, rowNum) -> new PaymentTransactionDetail(
                rs.getLong("id"),
                rs.getString("provider"),
                rs.getString("method"),
                rs.getString("transaction_ref"),
                rs.getString("provider_transaction_id"),
                money(rs.getBigDecimal("amount")),
                rs.getString("status"),
                toLocalDateTime(rs.getTimestamp("created_at"))
        ), orderId);
    }

    private Optional<SellerOrderGuard> findSellerOrderGuard(long shopId, long shopOrderId) {
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject("""
                    SELECT so.`id`, so.`order_id`, so.`shop_id`, so.`status`, so.`shipping_fee`, so.`cod_amount`,
                           o.`payment_method`, o.`payment_status`
                    FROM `shop_orders` so
                    JOIN `Orders` o ON o.`id` = so.`order_id`
                    WHERE so.`id` = ? AND so.`shop_id` = ?
                    """, (rs, rowNum) -> new SellerOrderGuard(
                    rs.getLong("id"),
                    rs.getLong("order_id"),
                    rs.getLong("shop_id"),
                    rs.getString("status"),
                    money(rs.getBigDecimal("shipping_fee")),
                    money(rs.getBigDecimal("cod_amount")),
                    rs.getString("payment_method"),
                    rs.getString("payment_status")
            ), shopOrderId, shopId));
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    private void guardSellerActionAllowed(SellerOrderGuard guard, String nextStatus) {
        if ("CANCELLED".equals(nextStatus) && "PAID".equalsIgnoreCase(guard.paymentStatus())) {
            throw badRequest("Don da thanh toan can quy trinh hoan tien rieng.");
        }
        if ("VNPAY".equalsIgnoreCase(guard.paymentMethod())
                && !"PAID".equalsIgnoreCase(guard.paymentStatus())
                && !"CANCELLED".equals(nextStatus)) {
            throw badRequest("Don VNPay chi duoc xu ly sau khi payment_status=PAID.");
        }
        if (!sellerTransitionAllowed(guard.currentStatus(), nextStatus)) {
            throw badRequest("Trang thai shop order khong the chuyen tu "
                    + guard.currentStatus() + " sang " + nextStatus + ".");
        }
    }

    private boolean sellerTransitionAllowed(String currentStatus, String nextStatus) {
        String current = normalize(currentStatus).toUpperCase(Locale.ROOT);
        return switch (current) {
            case "PENDING_PAYMENT" -> "CANCELLED".equals(nextStatus);
            case "NEW" -> Set.of("CONFIRMED", "CANCELLED").contains(nextStatus);
            case "CONFIRMED" -> Set.of("PACKING", "CANCELLED").contains(nextStatus);
            case "PACKING" -> Set.of("SHIPPING", "CANCELLED").contains(nextStatus);
            case "SHIPPING" -> Set.of("COMPLETED", "DELIVERY_FAILED").contains(nextStatus);
            default -> false;
        };
    }

    private Optional<Long> findExistingShipment(Long shopOrderId) {
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject("""
                    SELECT `id`
                    FROM `shipments`
                    WHERE `shop_order_id` = ? AND `provider` = 'GHN'
                    ORDER BY `id` DESC
                    LIMIT 1
                    """, Long.class, shopOrderId));
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    private void refreshOrderAggregateStatus(Long orderId) {
        List<String> statuses = jdbcTemplate.queryForList("""
                SELECT `status`
                FROM `shop_orders`
                WHERE `order_id` = ?
                """, String.class, orderId);
        if (statuses.isEmpty()) {
            return;
        }
        String aggregate;
        if (statuses.stream().allMatch("CANCELLED"::equalsIgnoreCase)) {
            aggregate = "CANCELLED";
        } else if (statuses.stream().allMatch("COMPLETED"::equalsIgnoreCase)) {
            aggregate = "COMPLETED";
        } else if (statuses.stream().anyMatch("SHIPPING"::equalsIgnoreCase)) {
            aggregate = "SHIPPING";
        } else if (statuses.stream().anyMatch("DELIVERY_FAILED"::equalsIgnoreCase)) {
            aggregate = "DELIVERY_FAILED";
        } else if (statuses.stream().anyMatch("PENDING_PAYMENT"::equalsIgnoreCase)) {
            aggregate = "PENDING_PAYMENT";
        } else {
            aggregate = "PROCESSING";
        }
        jdbcTemplate.update("""
                UPDATE `Orders`
                SET `status` = ?, `updated_at` = CURRENT_TIMESTAMP
                WHERE `id` = ?
                """, aggregate, orderId);
    }

    private Optional<PaymentOrder> findPaymentOrder(Long orderId) {
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject("""
                    SELECT `id`, `buyer_id`, `total_price`, `payment_method`, `payment_status`, `status`
                    FROM `Orders`
                    WHERE `id` = ?
                    """, (rs, rowNum) -> new PaymentOrder(
                    rs.getLong("id"),
                    rs.getLong("buyer_id"),
                    money(rs.getBigDecimal("total_price")),
                    rs.getString("payment_method"),
                    rs.getString("payment_status"),
                    rs.getString("status")
            ), orderId));
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        } catch (DataAccessException ex) {
            throw serviceUnavailable();
        }
    }

    private Optional<String> findTransactionRef(Long orderId) {
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject("""
                    SELECT `transaction_ref`
                    FROM `payment_transactions`
                    WHERE `order_id` = ? AND `method` = 'VNPAY' AND `status` = 'PENDING'
                    ORDER BY `id` DESC
                    LIMIT 1
                    """, String.class, orderId));
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    private String createPaymentTransaction(Long orderId, BigDecimal amount, String method, String status) {
        String transactionRef = "ORDER-" + orderId + "-" + System.currentTimeMillis();
        jdbcTemplate.update("""
                INSERT INTO `payment_transactions` (`id`, `order_id`, `provider`, `method`, `transaction_ref`,
                  `amount`, `status`, `raw_payload`, `created_at`, `updated_at`)
                VALUES (?, ?, ?, ?, ?, ?, ?, '{}', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """, nextId("payment_transactions"), orderId, method, method, transactionRef, amount, status);
        return transactionRef;
    }

    private void requireVnpaySandboxConfig() {
        if (!StringUtils.hasText(vnpayTmnCode) || !StringUtils.hasText(vnpaySecret)
                || !StringUtils.hasText(vnpayPayUrl) || !StringUtils.hasText(vnpayReturnUrl)) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Chua cau hinh VNPAY_TMN_CODE/VNPAY_HASH_SECRET/VNPAY_PAY_URL/VNPAY_RETURN_URL nen khong the mo VNPay sandbox.");
        }
    }

    private boolean vnpayAmountMatches(BigDecimal expectedAmount, String returnedAmount) {
        if (!StringUtils.hasText(returnedAmount)) {
            return false;
        }
        try {
            BigDecimal expected = money(expectedAmount).multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP);
            BigDecimal actual = new BigDecimal(returnedAmount).setScale(0, RoundingMode.HALF_UP);
            return expected.compareTo(actual) == 0;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    private ProductCard mapProductCard(ResultSet rs, int rowNum) throws SQLException {
        String shopName = rs.getString("shop_name");
        return new ProductCard(
                rs.getLong("id"),
                rs.getLong("shop_id"),
                shopName,
                rs.getString("name"),
                rs.getString("category"),
                money(rs.getBigDecimal("price")),
                rs.getBoolean("is_custom"),
                rs.getString("image"),
                shopName,
                rs.getString("status"),
                rs.getInt("stock")
        );
    }

    private Optional<ProductCard> findProductCard(Long productId) {
        if (productId == null) {
            return Optional.empty();
        }
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(productSelectSql() + " WHERE p.`id` = ?",
                    this::mapProductCard, productId));
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        } catch (DataAccessException ex) {
            return fallbackProducts().stream()
                    .filter(product -> product.id().equals(productId))
                    .findFirst();
        }
    }

    private CategorySummary mapCategorySummary(ResultSet rs, int rowNum) throws SQLException {
        return new CategorySummary(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getString("slug"),
                rs.getString("status"),
                rs.getInt("product_count")
        );
    }

    private ReviewSummary mapReviewSummary(ResultSet rs, int rowNum) throws SQLException {
        return new ReviewSummary(
                rs.getLong("id"),
                rs.getInt("rating"),
                rs.getString("comment"),
                rs.getString("seller_reply"),
                rs.getString("full_name"),
                toLocalDateTime(rs.getTimestamp("created_at"))
        );
    }

    private QuestionSummary mapQuestionSummary(ResultSet rs, int rowNum) throws SQLException {
        return new QuestionSummary(
                rs.getLong("id"),
                rs.getString("question"),
                rs.getString("answer"),
                rs.getString("asker_name"),
                rs.getString("status"),
                toLocalDateTime(rs.getTimestamp("created_at")),
                toLocalDateTime(rs.getTimestamp("answered_at"))
        );
    }

    private ShopProfile mapShopProfile(ResultSet rs, int rowNum) throws SQLException {
        return new ShopProfile(
                rs.getLong("id"),
                rs.getString("shop_name"),
                rs.getString("logo_url"),
                rs.getString("hero_url"),
                rs.getString("description"),
                rs.getString("about"),
                rs.getString("materials"),
                rs.getInt("years_experience"),
                rs.getBoolean("verified_artisan"),
                rs.getDouble("rating"),
                rs.getInt("follower_count")
        );
    }

    private UserProfile mapUserProfile(ResultSet rs, int rowNum) throws SQLException {
        return new UserProfile(
                rs.getLong("id"),
                rs.getString("full_name"),
                rs.getString("email"),
                rs.getString("phone"),
                rs.getString("address"),
                rs.getInt("reward_points"),
                rs.getString("status"),
                rs.getString("role")
        );
    }

    private AddressSummary mapAddressSummary(ResultSet rs, int rowNum) throws SQLException {
        return new AddressSummary(
                rs.getLong("id"),
                rs.getString("label"),
                rs.getString("receiver_name"),
                rs.getString("phone"),
                rs.getString("province"),
                rs.getString("district"),
                rs.getString("ward"),
                rs.getString("address"),
                rs.getBoolean("is_default")
        );
    }

    private NotificationSummary mapNotificationSummary(ResultSet rs, int rowNum) throws SQLException {
        return new NotificationSummary(
                rs.getLong("id"),
                rs.getString("type"),
                rs.getString("title"),
                rs.getString("message"),
                rs.getTimestamp("read_at") != null,
                toLocalDateTime(rs.getTimestamp("created_at"))
        );
    }

    private BuyerOrderSummary mapBuyerOrderSummary(ResultSet rs, int rowNum) throws SQLException {
        return new BuyerOrderSummary(
                rs.getLong("id"),
                rs.getString("status"),
                rs.getString("payment_status"),
                rs.getString("payment_method"),
                money(rs.getBigDecimal("total_price")),
                rs.getString("receiver_name"),
                rs.getString("receiver_phone"),
                rs.getInt("shop_count"),
                rs.getInt("item_count"),
                toLocalDateTime(rs.getTimestamp("created_at"))
        );
    }

    private Optional<ProductRow> findProduct(Long productId) {
        if (productId == null) {
            return Optional.empty();
        }
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(productSelectSql() + " WHERE p.`id` = ?",
                    (rs, rowNum) -> new ProductRow(
                            rs.getLong("id"),
                            rs.getLong("shop_id"),
                            rs.getString("shop_name"),
                            rs.getString("name"),
                            rs.getString("category"),
                            rs.getString("category_status"),
                            money(rs.getBigDecimal("price")),
                            rs.getBoolean("is_custom"),
                            rs.getString("image"),
                            rs.getString("status"),
                            rs.getString("approval_status"),
                            rs.getBoolean("requires_personalization"),
                            rs.getInt("stock")
                    ), productId));
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        } catch (DataAccessException ex) {
            throw serviceUnavailable();
        }
    }

    private String productSelectSql() {
        return """
                SELECT p.`id`, p.`shop_id`, COALESCE(s.`shop_name`, 'Unknown shop') AS shop_name,
                       p.`name`, COALESCE(c.`name`, 'Handmade') AS category, COALESCE(c.`status`, 'ACTIVE') AS category_status,
                       COALESCE(p.`price`, 0) AS price,
                       COALESCE(p.`is_custom`, false) AS is_custom,
                       COALESCE(NULLIF(p.`main_image_url`, ''), pi.`url`, '') AS image,
                       COALESCE(p.`status`, 'hidden') AS status,
                       COALESCE(p.`approval_status`, 'APPROVED') AS approval_status,
                       COALESCE(p.`requires_personalization`, false) AS requires_personalization,
                       COALESCE(inv.`stock`, 0) AS stock
                FROM `Products` p
                LEFT JOIN `Shops` s ON s.`id` = p.`shop_id`
                LEFT JOIN `Categories` c ON c.`id` = p.`cat_id`
                LEFT JOIN `ProductImages` pi ON pi.`product_id` = p.`id` AND pi.`is_main` = true
                LEFT JOIN (
                    SELECT `product_id`, SUM(GREATEST(COALESCE(`quantity`, 0) - COALESCE(`reserved_quantity`, 0), 0)) AS stock
                    FROM `Storage`
                    GROUP BY `product_id`
                ) inv ON inv.`product_id` = p.`id`
                """;
    }

    private ReviewSummary getReview(Long reviewId) {
        return jdbcTemplate.queryForObject("""
                SELECT r.`id`, r.`rating`, r.`comment`, r.`seller_reply`, u.`full_name`, r.`created_at`
                FROM `product_reviews` r
                LEFT JOIN `Users` u ON u.`id` = r.`user_id`
                WHERE r.`id` = ?
                """, this::mapReviewSummary, reviewId);
    }

    private Optional<CheckoutResponse> findCheckoutByIdempotency(long buyerId, String idempotencyKey) {
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject("""
                    SELECT o.`id`, o.`status`, o.`payment_status`, o.`payment_method`, o.`total_price`,
                           COALESCE(pt.`transaction_ref`, '') AS transaction_ref
                    FROM `Orders` o
                    LEFT JOIN `payment_transactions` pt ON pt.`order_id` = o.`id`
                    WHERE o.`buyer_id` = ? AND o.`idempotency_key` = ?
                    ORDER BY o.`id` DESC
                    LIMIT 1
                    """, (rs, rowNum) -> new CheckoutResponse(
                    rs.getLong("id"),
                    rs.getString("status"),
                    rs.getString("payment_status"),
                    rs.getString("payment_method"),
                    money(rs.getBigDecimal("total_price")),
                    rs.getString("transaction_ref"),
                    "VNPAY".equalsIgnoreCase(rs.getString("payment_method")) ? "PAYMENT_REQUIRED" : "VIEW_ORDER",
                    "/order-detail.html?id=" + rs.getLong("id")
            ), buyerId, idempotencyKey));
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    private BigDecimal calculateVoucherDiscount(String voucherCode, BigDecimal subtotal) {
        if (!StringUtils.hasText(voucherCode)) {
            return BigDecimal.ZERO;
        }
        try {
            Map<String, Object> voucher = jdbcTemplate.queryForMap("""
                    SELECT `discount_percent`, `max_discount_amount`, `min_order_amount`, `usage_limit`, `used_count`
                    FROM `vouchers`
                    WHERE `code` = ? AND `active` = true AND (`end_at` IS NULL OR `end_at` > CURRENT_TIMESTAMP)
                    """, voucherCode.trim().toUpperCase(Locale.ROOT));
            BigDecimal minOrder = money((BigDecimal) voucher.get("min_order_amount"));
            if (subtotal.compareTo(minOrder) < 0) {
                throw badRequest("Voucher chua dat gia tri don toi thieu.");
            }
            int usageLimit = ((Number) voucher.get("usage_limit")).intValue();
            int usedCount = ((Number) voucher.get("used_count")).intValue();
            if (usageLimit > 0 && usedCount >= usageLimit) {
                throw badRequest("Voucher da het luot su dung.");
            }
            BigDecimal discount = subtotal.multiply((BigDecimal) voucher.get("discount_percent")).divide(BigDecimal.valueOf(100));
            BigDecimal maxDiscount = money((BigDecimal) voucher.get("max_discount_amount"));
            if (maxDiscount.compareTo(BigDecimal.ZERO) > 0) {
                discount = discount.min(maxDiscount);
            }
            jdbcTemplate.update("UPDATE `vouchers` SET `used_count` = COALESCE(`used_count`, 0) + 1 WHERE `code` = ?",
                    voucherCode.trim().toUpperCase(Locale.ROOT));
            return money(discount);
        } catch (EmptyResultDataAccessException ex) {
            throw badRequest("Voucher khong hop le.");
        }
    }

    private GiftWrapSnapshot findGiftWrap(Long tierId) {
        if (tierId == null) {
            return new GiftWrapSnapshot(BigDecimal.ZERO, "");
        }
        try {
            return jdbcTemplate.queryForObject("""
                    SELECT `name`, `description`, `price`, `has_card`
                    FROM `gift_wrap_tiers`
                    WHERE `id` = ? AND `active` = true
                    """, (rs, rowNum) -> new GiftWrapSnapshot(
                    money(rs.getBigDecimal("price")),
                    rs.getString("name") + " - " + rs.getString("description") + " - card:" + rs.getBoolean("has_card")
            ), tierId);
        } catch (EmptyResultDataAccessException ex) {
            throw badRequest("Goi qua khong hop le.");
        }
    }

    private QuestionSummary getQuestion(Long questionId) {
        return jdbcTemplate.queryForObject("""
                SELECT q.`id`, q.`question`, q.`answer`, COALESCE(u.`full_name`, 'Khach hang') AS asker_name,
                       q.`status`, q.`created_at`, q.`answered_at`
                FROM `product_questions` q
                LEFT JOIN `Users` u ON u.`id` = q.`user_id`
                WHERE q.`id` = ?
                """, this::mapQuestionSummary, questionId);
    }

    private boolean exists(String sql, Object... args) {
        return count(sql, args) > 0;
    }

    private int count(String sql, Object... args) {
        try {
            Integer value = jdbcTemplate.queryForObject(sql, Integer.class, args);
            return value == null ? 0 : value;
        } catch (DataAccessException ex) {
            return 0;
        }
    }

    private BigDecimal sum(String sql, Object... args) {
        try {
            BigDecimal value = jdbcTemplate.queryForObject(sql, BigDecimal.class, args);
            return value == null ? BigDecimal.ZERO : value;
        } catch (DataAccessException ex) {
            return BigDecimal.ZERO;
        }
    }

    private BigDecimal resolveCommissionRate() {
        try {
            String rawBps = jdbcTemplate.queryForObject("""
                    SELECT `setting_value`
                    FROM `platform_settings`
                    WHERE `setting_key` = 'commission_bps'
                    """, String.class);
            if (StringUtils.hasText(rawBps)) {
                int bps = Integer.parseInt(rawBps.trim());
                if (bps >= 0 && bps <= 5000) {
                    return BigDecimal.valueOf(bps).divide(BigDecimal.valueOf(10_000), 6, RoundingMode.HALF_UP);
                }
            }
        } catch (DataAccessException | NumberFormatException ignored) {
            // Fall back to application configuration when the setting is unavailable.
        }
        return commissionRate;
    }

    private void notifyShopOwner(Long shopId, String type, String title, String message, String dedupeKey) {
        try {
            Long ownerId = jdbcTemplate.queryForObject("SELECT `owner_id` FROM `Shops` WHERE `id` = ?", Long.class, shopId);
            notifyUser(ownerId, type, title, message, dedupeKey);
        } catch (DataAccessException ignored) {
        }
    }

    private void notifyUser(Long userId, String type, String title, String message, String dedupeKey) {
        if (userId == null) {
            return;
        }
        try {
            jdbcTemplate.update("""
                    INSERT INTO `notifications` (`id`, `user_id`, `type`, `title`, `message`, `dedupe_key`, `created_at`)
                    VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
                    ON DUPLICATE KEY UPDATE `message` = VALUES(`message`)
                    """, nextId("notifications"), userId, type, title, message, dedupeKey);
        } catch (DataAccessException ignored) {
        }
    }

    private CartState loadCartState(long buyerId) {
        String redisKey = cartKey(buyerId);
        try {
            String raw = redisTemplate.opsForValue().get(redisKey);
            if (StringUtils.hasText(raw)) {
                CartState state = objectMapper.readValue(raw, CartState.class);
                memoryCarts.put(buyerId, state);
                return state;
            }
        } catch (RuntimeException | JsonProcessingException ignored) {
            // Redis is optional for local dev; in-memory keeps the frontend usable.
        }
        return memoryCarts.getOrDefault(buyerId, new CartState(List.of()));
    }

    private void saveCartState(long buyerId, CartState state) {
        memoryCarts.put(buyerId, state);
        try {
            redisTemplate.opsForValue().set(cartKey(buyerId), objectMapper.writeValueAsString(state), cartTtlDays, TimeUnit.DAYS);
        } catch (RuntimeException | JsonProcessingException ignored) {
            // In-memory fallback is already updated.
        }
    }

    private String cartKey(long buyerId) {
        return "cart:" + buyerId;
    }

    private int positiveQuantity(Integer quantity) {
        int value = quantity == null ? 1 : quantity;
        if (value <= 0) {
            throw badRequest("So luong phai lon hon 0.");
        }
        return value;
    }

    private void ensureStock(ProductRow product, int requestedQuantity) {
        if (requestedQuantity > product.stock()) {
            throw badRequest("Ton kho khong du cho san pham " + product.name() + ".");
        }
    }

    private String unavailableMessage(ProductRow row, int quantity) {
        if (!"active".equalsIgnoreCase(row.status())) {
            return "San pham dang khong mo ban.";
        }
        if (row.stock() < quantity) {
            return "Chi con " + row.stock() + " san pham.";
        }
        return "San pham khong san sang.";
    }

    private BigDecimal computeShippingFee(Long shopId, int itemCount, BigDecimal subtotal, Integer districtId, String wardCode) {
        if (itemCount <= 0) return BigDecimal.ZERO;
        return money(ghnService.calculateFee(districtId, wardCode, itemCount));
    }

    private String normalizePaymentMethod(String value) {
        String paymentMethod = normalize(value).toUpperCase(Locale.ROOT);
        if (!PAYMENT_METHODS.contains(paymentMethod)) {
            throw badRequest("Phuong thuc thanh toan khong hop le.");
        }
        return paymentMethod;
    }

    private String buildShippingAddress(CheckoutRequest request) {
        List<String> parts = new ArrayList<>();
        if (StringUtils.hasText(request.address())) {
            parts.add(request.address().trim());
        }
        String wardName = displayLocationName(request.wardName(), request.ward());
        String districtName = displayLocationName(request.districtName(), request.district());
        String provinceName = displayLocationName(request.provinceName(), request.province());
        if (StringUtils.hasText(wardName)) {
            parts.add(wardName);
        }
        if (StringUtils.hasText(districtName)) {
            parts.add(districtName);
        }
        if (StringUtils.hasText(provinceName)) {
            parts.add(provinceName);
        }
        return String.join(", ", parts);
    }

    private String displayLocationName(String label, String code) {
        return StringUtils.hasText(label) ? label.trim() : normalize(code);
    }

    private long nextId(String tableName) {
        Long value = jdbcTemplate.queryForObject("SELECT COALESCE(MAX(`id`), 0) + 1 FROM `" + tableName + "`", Long.class);
        return value == null ? 1L : value;
    }

    private String buildItemKey(Long productId, String customOptionsJson, String note) {
        String value = productId + "|" + normalize(customOptionsJson) + "|" + normalize(note);
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (int i = 0; i < 6; i++) {
                hex.append(String.format("%02x", bytes[i]));
            }
            return productId + "-" + hex;
        } catch (Exception ex) {
            return productId + "-" + Math.abs(Objects.hash(value));
        }
    }

    private String signParams(Map<String, String> params) {
        return hmacSha512(signingData(params), vnpaySecret);
    }

    private boolean verifySecureHash(Map<String, String> params) {
        String received = params.get("vnp_SecureHash");
        if (!StringUtils.hasText(received)) {
            return false;
        }
        String expected = signParams(params);
        return received.equalsIgnoreCase(expected);
    }

    private String signingData(Map<String, String> params) {
        TreeMap<String, String> sorted = new TreeMap<>(params);
        sorted.remove("vnp_SecureHash");
        sorted.remove("vnp_SecureHashType");
        return sorted.entrySet().stream()
                .filter(entry -> StringUtils.hasText(entry.getValue()))
                .map(entry -> urlEncode(entry.getKey()) + "=" + urlEncode(entry.getValue()))
                .reduce((left, right) -> left + "&" + right)
                .orElse("");
    }

    private String appendQuery(String baseUrl, Map<String, String> params) {
        String separator = baseUrl.contains("?") ? "&" : "?";
        return baseUrl + separator + signingData(params) + "&vnp_SecureHash=" + urlEncode(params.get("vnp_SecureHash"));
    }

    private String hmacSha512(String data, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA512");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
            byte[] bytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder();
            for (byte b : bytes) {
                result.append(String.format("%02x", b));
            }
            return result.toString();
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Khong tao duoc chu ky thanh toan.");
        }
    }

    private Long resolveOrderId(Map<String, String> params) {
        String direct = params.get("orderId");
        if (StringUtils.hasText(direct)) {
            try {
                return Long.parseLong(direct);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        String txnRef = params.get("vnp_TxnRef");
        if (!StringUtils.hasText(txnRef) || !txnRef.startsWith("ORDER-")) {
            return null;
        }
        String[] parts = txnRef.split("-");
        if (parts.length < 2) {
            return null;
        }
        try {
            return Long.parseLong(parts[1]);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            return "{}";
        }
    }

    private String requireText(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw badRequest(message);
        }
        return value.trim();
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(0, RoundingMode.HALF_UP);
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    private ResponseStatusException notFound(String message) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, message);
    }

    private ResponseStatusException serviceUnavailable() {
        return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "MySQL chua san sang. Hay kiem tra MYSQL_URL, MYSQL_USER, MYSQL_PASSWORD va database e_commerce.");
    }

    private ProductDetailResponse fallbackProductDetail(ProductCard product) {
        List<ProductCard> related = fallbackProducts().stream()
                .filter(item -> !item.id().equals(product.id()))
                .filter(item -> Objects.equals(item.category(), product.category()) || Objects.equals(item.shopId(), product.shopId()))
                .limit(4)
                .toList();
        return new ProductDetailResponse(product, List.of(), List.of(), false, false, related);
    }

    private Optional<PublicShopResponse> fallbackPublicShop(Long shopId) {
        List<ProductCard> products = fallbackProducts().stream()
                .filter(product -> Objects.equals(product.shopId(), shopId))
                .toList();
        if (products.isEmpty()) {
            return Optional.empty();
        }
        String shopName = products.get(0).shopName();
        ShopProfile shop = new ShopProfile(shopId, shopName, "", "", "Shop demo khi MySQL chua san sang.",
                "", "", 1, false, 0, 0);
        return Optional.of(new PublicShopResponse(shop, products, false));
    }

    private List<ProductCard> fallbackProducts() {
        return List.of(
                new ProductCard(1L, 1L, "Luna Press", "Petals & Parchment", "Thiep handmade",
                        BigDecimal.valueOf(240000), false, "https://picsum.photos/seed/petals-parchment/640/480",
                        "Luna Press", "active", 24),
                new ProductCard(2L, 2L, "Golden Fold", "Golden Solstice", "Qua tang custom",
                        BigDecimal.valueOf(360000), true, "https://picsum.photos/seed/golden-solstice/640/480",
                        "Golden Fold", "active", 12),
                new ProductCard(3L, 3L, "Indigo Studio", "Indigo Dreams", "Qua tang custom",
                        BigDecimal.valueOf(480000), true, "https://picsum.photos/seed/indigo-dreams/640/480",
                        "Indigo Studio", "active", 10),
                new ProductCard(4L, 1L, "Luna Press", "Botanical Keepsake", "Decor thu cong",
                        BigDecimal.valueOf(520000), true, "https://picsum.photos/seed/botanical-keepsake/640/480",
                        "Luna Press", "active", 8)
        );
    }

    private static final class ShopCartBuilder {
        private final Long shopId;
        private final String shopName;
        private final List<CartLine> lines = new ArrayList<>();
        private BigDecimal subtotal = BigDecimal.ZERO;
        private boolean canCheckout = true;

        private ShopCartBuilder(Long shopId, String shopName) {
            this.shopId = shopId;
            this.shopName = shopName;
        }
    }

    private record ProductRow(
            Long id,
            Long shopId,
            String shopName,
            String name,
            String category,
            String categoryStatus,
            BigDecimal price,
            boolean customizable,
            String image,
            String status,
            String approvalStatus,
            boolean requiresPersonalization,
            int stock
    ) {
    }

    private record GiftWrapSnapshot(BigDecimal price, String snapshot) {
    }

    private record CartState(List<StoredCartItem> items) {
        CartState {
            items = items == null ? List.of() : List.copyOf(items);
        }
    }

    private record StoredCartItem(
            String itemKey,
            Long productId,
            Long shopId,
            int quantity,
            String customOptionsJson,
            String note
    ) {
    }

    private record PaymentOrder(Long id, Long buyerId, BigDecimal total, String paymentMethod, String paymentStatus, String status) {
    }

    private record OrderHeader(
            Long id,
            Long buyerId,
            String status,
            String paymentStatus,
            String paymentMethod,
            String receiverName,
            String receiverPhone,
            String shippingAddress,
            BigDecimal subtotal,
            BigDecimal shippingFee,
            BigDecimal total,
            LocalDateTime createdAt
    ) {
    }

    private record SellerOrderGuard(
            Long shopOrderId,
            Long orderId,
            Long shopId,
            String currentStatus,
            BigDecimal shippingFee,
            BigDecimal codAmount,
            String paymentMethod,
            String paymentStatus
    ) {
    }

    private record InventoryAdjustment(Long productId, int quantity) {
    }

    public record ProductCard(
            Long id,
            Long shopId,
            String shopName,
            String name,
            String category,
            BigDecimal price,
            boolean customizable,
            String image,
            String artisan,
            String status,
            int stock
    ) {
    }

    public record HomepageResponse(
            List<HomepageBanner> banners,
            List<CategorySummary> categories,
            List<ProductCard> featuredProducts,
            List<ProductCard> bestSellers,
            List<PromotionCard> promotions
    ) {
    }

    public record HomepageBanner(Long id, String title, String subtitle, String imageUrl, String linkUrl) {
    }

    public record CategorySummary(Long id, String name, String slug, String status, int productCount) {
    }

    public record PromotionCard(String code, String title, String description, String status) {
    }

    public record ProductDetailResponse(
            ProductCard product,
            List<ReviewSummary> reviews,
            List<QuestionSummary> questions,
            boolean wished,
            boolean followedShop,
            List<ProductCard> relatedProducts
    ) {
    }

    public record ReviewSummary(
            Long id,
            int rating,
            String comment,
            String sellerReply,
            String customerName,
            LocalDateTime createdAt
    ) {
    }

    public record QuestionSummary(
            Long id,
            String question,
            String answer,
            String askerName,
            String status,
            LocalDateTime createdAt,
            LocalDateTime answeredAt
    ) {
    }

    public record QuestionRequest(String question) {
    }

    public record QuestionAnswerRequest(String answer) {
    }

    public record ReviewRequest(Integer rating, String comment) {
    }

    public record ReviewEligibilityResponse(boolean eligible, boolean purchased, boolean reviewed, String reason) {
    }

    public record ShopProfile(
            Long id,
            String shopName,
            String logoUrl,
            String heroUrl,
            String description,
            String about,
            String materials,
            int yearsExperience,
            boolean verifiedArtisan,
            double rating,
            int followerCount
    ) {
    }

    public record PublicShopResponse(ShopProfile shop, List<ProductCard> products, boolean followed) {
    }

    public record UserProfile(
            Long id,
            String fullName,
            String email,
            String phone,
            String address,
            int rewardPoints,
            String status,
            String role
    ) {
    }

    public record AddressSummary(
            Long id,
            String label,
            String receiverName,
            String phone,
            String province,
            String district,
            String ward,
            String address,
            boolean defaultAddress
    ) {
    }

    public record UserProfileResponse(
            UserProfile profile,
            List<AddressSummary> addresses,
            List<ProductCard> wishlist,
            List<ShopProfile> followedShops
    ) {
    }

    public record WishlistToggleResponse(Long productId, boolean wished) {
    }

    public record FollowToggleResponse(Long shopId, boolean followed, int followerCount) {
    }

    public record NotificationSummary(
            Long id,
            String type,
            String title,
            String message,
            boolean read,
            LocalDateTime createdAt
    ) {
    }

    public record NotificationCenterResponse(List<NotificationSummary> notifications, int unreadCount) {
    }

    public record AdminDashboardResponse(
            int users,
            int shops,
            int products,
            int orders,
            BigDecimal revenue,
            int reviews,
            int pendingQuestions
    ) {
    }

    public record SellerDashboardResponse(
            Long shopId,
            int products,
            int lowStockProducts,
            int orders,
            BigDecimal revenue,
            int reviews,
            int pendingQuestions
    ) {
    }

    public record CartItemRequest(Long productId, Long shopId, Integer quantity, String customOptionsJson, String note) {
    }

    public record UpdateCartItemRequest(Integer quantity, String customOptionsJson, String note) {
    }

    public record CartResponse(Long buyerId, List<ShopCartGroup> shops, BigDecimal subtotal, int totalQuantity, boolean canCheckout) {
    }

    public record ShopCartGroup(Long shopId, String shopName, List<CartLine> items, BigDecimal subtotal, boolean canCheckout) {
    }

    public record CartLine(
            String itemKey,
            Long productId,
            Long shopId,
            String productName,
            String shopName,
            String image,
            int quantity,
            BigDecimal unitPrice,
            BigDecimal lineTotal,
            String customOptionsJson,
            String note,
            int stock,
            String status,
            boolean available,
            String message
    ) {
    }

    public record CheckoutSummaryResponse(
            List<ShopCheckoutSummary> shopSummaries,
            BigDecimal subtotal,
            BigDecimal shippingTotal,
            BigDecimal grandTotal,
            boolean canCheckout,
            String message
    ) {
    }

    public record ShopCheckoutSummary(
            Long shopId,
            String shopName,
            BigDecimal itemSubtotal,
            BigDecimal shippingFee,
            BigDecimal total,
            int itemCount,
            String shipmentNote
    ) {
    }

    public record CheckoutRequest(
            String receiverName,
            String phone,
            String province,
            String district,
            String ward,
            String provinceName,
            String districtName,
            String wardName,
            String address,
            String paymentMethod,
            String voucherCode,
            Long giftWrapTierId,
            String giftMessage,
            Integer rewardPointsUsed,
            String idempotencyKey
    ) {
    }

    public record CheckoutResponse(
            Long orderId,
            String status,
            String paymentStatus,
            String paymentMethod,
            BigDecimal total,
            String transactionRef,
            String nextAction,
            String orderUrl
    ) {
    }

    public record PaymentCreateRequest(Long orderId) {
    }

    public record PaymentCreateResponse(Long orderId, String paymentUrl, BigDecimal amount, String status) {
    }

    public record PaymentReturnResponse(
            boolean success,
            Long orderId,
            String paymentStatus,
            String orderStatus,
            BigDecimal amount,
            String message
    ) {
    }

    public record ShippingFeeRequest(Long shopId, Integer quantity, BigDecimal subtotal, String province, String district, String ward) {
    }

    public record ShippingFeeResponse(Long shopId, String provider, String serviceName, BigDecimal fee, String code) {
    }

    public record MasterDataOption(String code, String name) {
    }

    public record BuyerOrderSummary(
            Long id,
            String status,
            String paymentStatus,
            String paymentMethod,
            BigDecimal total,
            String receiverName,
            String receiverPhone,
            int shopCount,
            int itemCount,
            LocalDateTime createdAt
    ) {
    }

    public record OrderDetailResponse(
            Long id,
            Long buyerId,
            String status,
            String paymentStatus,
            String paymentMethod,
            String receiverName,
            String receiverPhone,
            String shippingAddress,
            BigDecimal subtotal,
            BigDecimal shippingFee,
            BigDecimal total,
            LocalDateTime createdAt,
            List<ShopOrderDetail> shopOrders,
            List<PaymentTransactionDetail> payments
    ) {
    }

    public record ShopOrderDetail(
            Long id,
            Long shopId,
            String shopName,
            BigDecimal itemSubtotal,
            BigDecimal shippingFee,
            BigDecimal commissionAmount,
            BigDecimal payoutAmount,
            BigDecimal codAmount,
            String status,
            List<OrderItemDetail> items,
            List<ShipmentDetail> shipments
    ) {
    }

    public record OrderItemDetail(
            Long id,
            Long productId,
            String productName,
            int quantity,
            BigDecimal unitPrice,
            BigDecimal lineTotal,
            String customOptionsJson,
            String note
    ) {
    }

    public record PaymentTransactionDetail(
            Long id,
            String provider,
            String method,
            String transactionRef,
            String providerTransactionId,
            BigDecimal amount,
            String status,
            LocalDateTime createdAt
    ) {
    }

    public record ShipmentDetail(
            Long id,
            String provider,
            String trackingCode,
            String serviceName,
            BigDecimal fee,
            BigDecimal codAmount,
            String status,
            LocalDateTime createdAt
    ) {
    }

    public record SellerOrderResponse(
            Long shopOrderId,
            Long orderId,
            Long shopId,
            String shopName,
            String status,
            String orderStatus,
            String paymentStatus,
            String paymentMethod,
            String receiverName,
            String receiverPhone,
            String shippingAddress,
            BigDecimal itemSubtotal,
            BigDecimal shippingFee,
            BigDecimal codAmount,
            LocalDateTime createdAt,
            List<OrderItemDetail> items,
            List<ShipmentDetail> shipments
    ) {
    }

    public record SellerStatusUpdateRequest(String status) {
    }

    public record SellerShipmentRequest(String serviceName, String note) {
    }

    public record ShipmentStatusRequest(String status, String trackingCode) {
    }
}
