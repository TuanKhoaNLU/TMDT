package com.tmdt.marketplace.service;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class MarketplaceModuleService {

    private final JdbcTemplate jdbcTemplate;

    public MarketplaceModuleService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public MarketplaceModulesResponse modules() {
        return new MarketplaceModulesResponse(
                listVouchers(),
                listFlashSales(),
                listGiftWrapTiers(),
                listCommissionPosts(),
                listCustomOrders(null, null),
                List.of(),
                Map.of(),
                new PaymentReliabilityResponse(0, 0, 0, 0)
        );
    }

    public List<CategoryAdminSummary> listCategories() {
        return jdbcTemplate.query("""
                SELECT c.`id`, c.`parent_id`, c.`name`, c.`slug`, COALESCE(c.`image_url`, '') AS image_url,
                       COALESCE(c.`status`, 'ACTIVE') AS status, COUNT(p.`id`) AS product_count
                FROM `Categories` c
                LEFT JOIN `Products` p ON p.`cat_id` = c.`id`
                GROUP BY c.`id`, c.`parent_id`, c.`name`, c.`slug`, c.`image_url`, c.`status`
                ORDER BY c.`id`
                """, this::mapCategory);
    }

    public CategoryAdminSummary createCategory(CategoryRequest request) {
        requireText(request.name(), "Thieu ten danh muc.");
        long id = nextId("Categories");
        jdbcTemplate.update("""
                INSERT INTO `Categories` (`id`, `parent_id`, `name`, `slug`, `image_url`, `status`)
                VALUES (?, ?, ?, ?, ?, ?)
                """, id, request.parentId(), request.name(), slugOrDefault(request.slug(), request.name()),
                request.imageUrl(), request.status() == null ? "ACTIVE" : request.status());
        return listCategories().stream().filter(category -> category.id().equals(id)).findFirst().orElseThrow();
    }

    public CategoryAdminSummary updateCategory(Long categoryId, CategoryRequest request) {
        jdbcTemplate.update("""
                UPDATE `Categories`
                SET `parent_id` = ?, `name` = ?, `slug` = ?, `image_url` = ?, `status` = ?
                WHERE `id` = ?
                """, request.parentId(), request.name(), slugOrDefault(request.slug(), request.name()),
                request.imageUrl(), request.status() == null ? "ACTIVE" : request.status(), categoryId);
        return listCategories().stream().filter(category -> category.id().equals(categoryId)).findFirst().orElseThrow();
    }

    public List<ProductAdminSummary> listSellerProducts(Long shopId) {
        return jdbcTemplate.query("""
                SELECT p.`id`, p.`shop_id`, p.`cat_id`, p.`name`, COALESCE(p.`sku`, '') AS sku,
                       COALESCE(c.`name`, 'Handmade') AS category_name, COALESCE(p.`price`, 0) AS price,
                       COALESCE(p.`description`, '') AS description, COALESCE(p.`tags`, '') AS tags,
                       COALESCE(p.`approval_status`, 'APPROVED') AS approval_status,
                       COALESCE(p.`status`, 'hidden') AS sale_status,
                       COALESCE(p.`main_image_url`, pi.`url`, '') AS image_url,
                       COALESCE(p.`options_json`, '') AS options_json,
                       COALESCE(p.`requires_personalization`, false) AS requires_personalization,
                       COALESCE(p.`processing_days`, 3) AS processing_days,
                       COALESCE(st.`quantity`, 0) AS stock,
                       COALESCE(st.`low_stock_alert`, 0) AS low_stock_alert
                FROM `Products` p
                LEFT JOIN `Categories` c ON c.`id` = p.`cat_id`
                LEFT JOIN `ProductImages` pi ON pi.`product_id` = p.`id` AND pi.`is_main` = true
                LEFT JOIN `Storage` st ON st.`product_id` = p.`id`
                WHERE p.`shop_id` = ?
                ORDER BY p.`id` DESC
                """, this::mapProductAdmin, shopId);
    }

    public List<ProductAdminSummary> listAdminProducts() {
        return jdbcTemplate.query("""
                SELECT p.`id`, p.`shop_id`, p.`cat_id`, p.`name`, COALESCE(p.`sku`, '') AS sku,
                       COALESCE(c.`name`, 'Handmade') AS category_name, COALESCE(p.`price`, 0) AS price,
                       COALESCE(p.`description`, '') AS description, COALESCE(p.`tags`, '') AS tags,
                       COALESCE(p.`approval_status`, 'APPROVED') AS approval_status,
                       COALESCE(p.`status`, 'hidden') AS sale_status,
                       COALESCE(p.`main_image_url`, pi.`url`, '') AS image_url,
                       COALESCE(p.`options_json`, '') AS options_json,
                       COALESCE(p.`requires_personalization`, false) AS requires_personalization,
                       COALESCE(p.`processing_days`, 3) AS processing_days,
                       COALESCE(st.`quantity`, 0) AS stock,
                       COALESCE(st.`low_stock_alert`, 0) AS low_stock_alert
                FROM `Products` p
                LEFT JOIN `Categories` c ON c.`id` = p.`cat_id`
                LEFT JOIN `ProductImages` pi ON pi.`product_id` = p.`id` AND pi.`is_main` = true
                LEFT JOIN `Storage` st ON st.`product_id` = p.`id`
                ORDER BY p.`id` DESC
                """, this::mapProductAdmin);
    }

    @Transactional
    public ProductAdminSummary createSellerProduct(Long shopId, ProductWriteRequest request) {
        requireText(request.name(), "Thieu ten san pham.");
        long productId = nextId("Products");
        jdbcTemplate.update("""
                INSERT INTO `Products` (`id`, `shop_id`, `cat_id`, `name`, `price`, `description`, `is_custom`, `avg_rating`, `status`,
                  `sku`, `tags`, `approval_status`, `main_image_url`, `options_json`, `requires_personalization`, `processing_days`)
                VALUES (?, ?, ?, ?, ?, ?, ?, 0, 'hidden', ?, ?, 'PENDING', ?, ?, ?, ?)
                """, productId, shopId, request.categoryId(), request.name(), value(request.price()), request.description(),
                Boolean.TRUE.equals(request.customizable()), request.sku(), request.tags(), request.imageUrl(),
                request.optionsJson(), Boolean.TRUE.equals(request.requiresPersonalization()),
                request.processingDays() == null ? 3 : request.processingDays());
        long storageId = nextId("Storage");
        int stock = request.stock() == null ? 0 : request.stock();
        jdbcTemplate.update("""
                INSERT INTO `Storage` (`id`, `product_id`, `warehouse_id`, `quantity`, `reserved_quantity`, `low_stock_alert`, `last_updated`)
                VALUES (?, ?, 1, ?, 0, ?, CURRENT_TIMESTAMP)
                """, storageId, productId, stock, request.lowStockAlert() == null ? 5 : request.lowStockAlert());
        logInventory(productId, stock, "SELLER_CREATE", "Tao listing moi dang cho duyet");
        return listSellerProducts(shopId).stream().filter(product -> product.id().equals(productId)).findFirst().orElseThrow();
    }

    @Transactional
    public ProductAdminSummary updateSellerProduct(Long shopId, Long productId, ProductWriteRequest request) {
        jdbcTemplate.update("""
                UPDATE `Products`
                SET `cat_id` = ?, `name` = ?, `price` = ?, `description` = ?, `is_custom` = ?, `status` = 'hidden',
                    `sku` = ?, `tags` = ?, `approval_status` = 'PENDING', `main_image_url` = ?,
                    `options_json` = ?, `requires_personalization` = ?, `processing_days` = ?
                WHERE `id` = ? AND `shop_id` = ?
                """, request.categoryId(), request.name(), value(request.price()), request.description(),
                Boolean.TRUE.equals(request.customizable()), request.sku(), request.tags(), request.imageUrl(),
                request.optionsJson(), Boolean.TRUE.equals(request.requiresPersonalization()),
                request.processingDays() == null ? 3 : request.processingDays(), productId, shopId);
        if (request.stock() != null) {
            Integer oldStock = jdbcTemplate.queryForObject("SELECT COALESCE(quantity, 0) FROM Storage WHERE product_id = ? LIMIT 1", Integer.class, productId);
            jdbcTemplate.update("UPDATE `Storage` SET `quantity` = ?, `low_stock_alert` = ?, `last_updated` = CURRENT_TIMESTAMP WHERE `product_id` = ?",
                    request.stock(), request.lowStockAlert() == null ? 5 : request.lowStockAlert(), productId);
            logInventory(productId, request.stock() - (oldStock == null ? 0 : oldStock), "SELLER_UPDATE", "Seller cap nhat ton kho");
        }
        return listSellerProducts(shopId).stream().filter(product -> product.id().equals(productId)).findFirst().orElseThrow();
    }

    public ProductAdminSummary moderateProduct(Long productId, StatusRequest request) {
        String approval = request.status() == null ? "APPROVED" : request.status().toUpperCase();
        String saleStatus = "APPROVED".equals(approval) ? "active" : "hidden";
        jdbcTemplate.update("UPDATE `Products` SET `approval_status` = ?, `status` = ? WHERE `id` = ?", approval, saleStatus, productId);
        return listAdminProducts().stream().filter(product -> product.id().equals(productId)).findFirst().orElseThrow();
    }

    public List<AdminUserSummary> listAdminUsers() {
        return jdbcTemplate.query("""
                SELECT u.`id`, u.`full_name`, COALESCE(u.`phone`, '') AS phone, COALESCE(u.`status`, 'ACTIVE') AS status,
                       COALESCE(a.`email`, '') AS email, COALESCE(a.`username`, '') AS username,
                       COALESCE(a.`role`, 'BUYER') AS role,
                       (SELECT COUNT(*) FROM `Orders` o WHERE o.`buyer_id` = u.`id`) AS orders_count,
                       (SELECT COALESCE(SUM(o.`total_price`), 0) FROM `Orders` o WHERE o.`buyer_id` = u.`id`) AS total_spent,
                       (SELECT COALESCE(SUM(so.`item_subtotal`), 0)
                        FROM `Shops` s JOIN `shop_orders` so ON so.`shop_id` = s.`id`
                        WHERE s.`owner_id` = u.`id`) AS sales
                FROM `Users` u
                LEFT JOIN `Accounts` a ON a.`user_id` = u.`id`
                ORDER BY u.`id`
                """, this::mapAdminUser);
    }

    public AdminUserSummary createAdminUser(AdminUserRequest request) {
        requireText(request.username(), "Thieu username.");
        requireText(request.fullName(), "Thieu ho ten.");
        long userId = nextId("Users");
        long accountId = nextId("Accounts");
        jdbcTemplate.update("""
                INSERT INTO `Users` (`id`, `full_name`, `phone`, `status`)
                VALUES (?, ?, ?, ?)
                """, userId, request.fullName(), request.phone(), request.status() == null ? "ACTIVE" : request.status());
        jdbcTemplate.update("""
                INSERT INTO `Accounts` (`id`, `user_id`, `username`, `password_hash`, `email`, `role`, `status`)
                VALUES (?, ?, ?, '{noop}123456', ?, ?, 1)
                """, accountId, userId, request.username(), request.email(), request.role() == null ? "BUYER" : request.role());
        return listAdminUsers().stream().filter(user -> user.id().equals(userId)).findFirst().orElseThrow();
    }

    public AdminUserSummary updateAdminUser(Long userId, AdminUserRequest request) {
        jdbcTemplate.update("""
                UPDATE `Users`
                SET `full_name` = COALESCE(?, `full_name`), `phone` = COALESCE(?, `phone`), `status` = COALESCE(?, `status`)
                WHERE `id` = ?
                """, request.fullName(), request.phone(), request.status(), userId);
        jdbcTemplate.update("""
                UPDATE `Accounts`
                SET `role` = COALESCE(?, `role`), `email` = COALESCE(?, `email`)
                WHERE `user_id` = ?
                """, request.role(), request.email(), userId);
        return listAdminUsers().stream().filter(user -> user.id().equals(userId)).findFirst().orElseThrow();
    }

    public AdminUserSummary verifySeller(Long userId, boolean verified) {
        jdbcTemplate.update("UPDATE `Shops` SET `verified_artisan` = ? WHERE `owner_id` = ?", verified, userId);
        return listAdminUsers().stream().filter(user -> user.id().equals(userId)).findFirst().orElseThrow();
    }

    public List<ReviewModerationSummary> listReviews(Long shopId) {
        if (shopId != null) {
            return jdbcTemplate.query("""
                    SELECT r.`id`, r.`product_id`, p.`name` AS product_name, r.`shop_id`, s.`shop_name`,
                           r.`user_id`, COALESCE(u.`full_name`, 'Khach hang') AS customer_name,
                           r.`rating`, r.`comment`, r.`seller_reply`, r.`created_at`
                    FROM `product_reviews` r
                    JOIN `Products` p ON p.`id` = r.`product_id`
                    JOIN `Shops` s ON s.`id` = r.`shop_id`
                    LEFT JOIN `Users` u ON u.`id` = r.`user_id`
                    WHERE r.`shop_id` = ?
                    ORDER BY r.`created_at` DESC
                    """, this::mapReviewModeration, shopId);
        }
        return jdbcTemplate.query("""
                SELECT r.`id`, r.`product_id`, p.`name` AS product_name, r.`shop_id`, s.`shop_name`,
                       r.`user_id`, COALESCE(u.`full_name`, 'Khach hang') AS customer_name,
                       r.`rating`, r.`comment`, r.`seller_reply`, r.`created_at`
                FROM `product_reviews` r
                JOIN `Products` p ON p.`id` = r.`product_id`
                JOIN `Shops` s ON s.`id` = r.`shop_id`
                LEFT JOIN `Users` u ON u.`id` = r.`user_id`
                ORDER BY r.`created_at` DESC
                """, this::mapReviewModeration);
    }

    public ReviewModerationSummary replyReview(Long reviewId, ReviewReplyRequest request, Long shopId) {
        if (shopId != null) {
            jdbcTemplate.update("UPDATE `product_reviews` SET `seller_reply` = ?, `updated_at` = CURRENT_TIMESTAMP WHERE `id` = ? AND `shop_id` = ?",
                    request.reply(), reviewId, shopId);
        } else {
            jdbcTemplate.update("UPDATE `product_reviews` SET `seller_reply` = ?, `updated_at` = CURRENT_TIMESTAMP WHERE `id` = ?",
                    request.reply(), reviewId);
        }
        return listReviews(shopId).stream().filter(review -> review.id().equals(reviewId)).findFirst().orElseThrow();
    }

    public List<VoucherSummary> listVouchers() {
        return jdbcTemplate.query("""
                SELECT `id`, `code`, `scope`, `shop_id`, `category_id`, `title`, `discount_percent`,
                       `max_discount_amount`, `min_order_amount`, `usage_limit`, `used_count`,
                       `per_user_limit`, `active`, `end_at`
                FROM `vouchers`
                WHERE `active` = true
                ORDER BY `id`
                """, this::mapVoucher);
    }

    public VoucherSummary createVoucher(VoucherRequest request, Long shopId) {
        requireText(request.code(), "Thieu voucher code.");
        long id = nextId("vouchers");
        jdbcTemplate.update("""
                INSERT INTO `vouchers` (`id`, `code`, `scope`, `shop_id`, `category_id`, `title`, `discount_percent`,
                  `max_discount_amount`, `min_order_amount`, `usage_limit`, `used_count`, `per_user_limit`, `active`, `end_at`)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0, ?, true, DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 30 DAY))
                """, id, request.code().trim().toUpperCase(), request.scope() == null ? "PLATFORM" : request.scope(),
                shopId, request.categoryId(), request.title(), value(request.discountPercent()),
                value(request.maxDiscountAmount()), value(request.minOrderAmount()),
                request.usageLimit() == null ? 100 : request.usageLimit(),
                request.perUserLimit() == null ? 1 : request.perUserLimit());
        return findVoucher(id);
    }

    public VoucherApplyResponse applyVoucher(Long userId, VoucherApplyRequest request) {
        VoucherSummary voucher = jdbcTemplate.queryForObject("""
                SELECT `id`, `code`, `scope`, `shop_id`, `category_id`, `title`, `discount_percent`,
                       `max_discount_amount`, `min_order_amount`, `usage_limit`, `used_count`,
                       `per_user_limit`, `active`, `end_at`
                FROM `vouchers`
                WHERE `code` = ? AND `active` = true
                """, this::mapVoucher, request.code() == null ? "" : request.code().trim().toUpperCase());
        BigDecimal subtotal = value(request.subtotal());
        if (subtotal.compareTo(voucher.minOrderAmount()) < 0) {
            throw badRequest("Don hang chua dat gia tri toi thieu cho voucher.");
        }
        BigDecimal discount = subtotal.multiply(voucher.discountPercent()).divide(BigDecimal.valueOf(100));
        if (voucher.maxDiscountAmount().compareTo(BigDecimal.ZERO) > 0) {
            discount = discount.min(voucher.maxDiscountAmount());
        }
        return new VoucherApplyResponse(voucher.code(), money(discount), money(subtotal.subtract(discount)), "APPLIED");
    }

    public List<FlashSaleSummary> listFlashSales() {
        return jdbcTemplate.query("""
                SELECT `id`, `name`, `description`, `banner_url`, `state`, `discount_percent`, `max_units`,
                       `sold_units`, `reserved_units`, `per_user_limit`, `start_at`, `end_at`
                FROM `flash_sales`
                ORDER BY `id`
                """, this::mapFlashSale);
    }

    public FlashSaleSummary createFlashSale(FlashSaleRequest request) {
        long id = nextId("flash_sales");
        jdbcTemplate.update("""
                INSERT INTO `flash_sales` (`id`, `name`, `description`, `banner_url`, `state`, `discount_percent`,
                  `max_units`, `sold_units`, `reserved_units`, `per_user_limit`, `start_at`, `end_at`)
                VALUES (?, ?, ?, ?, 'ACTIVE', ?, ?, 0, 0, ?, CURRENT_TIMESTAMP, DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 7 DAY))
                """, id, request.name(), request.description(), request.bannerUrl(), value(request.discountPercent()),
                request.maxUnits() == null ? 100 : request.maxUnits(), request.perUserLimit() == null ? 1 : request.perUserLimit());
        return jdbcTemplate.queryForObject("""
                SELECT `id`, `name`, `description`, `banner_url`, `state`, `discount_percent`, `max_units`,
                       `sold_units`, `reserved_units`, `per_user_limit`, `start_at`, `end_at`
                FROM `flash_sales` WHERE `id` = ?
                """, this::mapFlashSale, id);
    }

    public List<GiftWrapTier> listGiftWrapTiers() {
        return jdbcTemplate.query("""
                SELECT `id`, `name`, `description`, `price`, `has_card`, `sort_order`, `active`
                FROM `gift_wrap_tiers`
                WHERE `active` = true
                ORDER BY `sort_order`, `id`
                """, this::mapGiftWrap);
    }

    public List<ShippingProfileSummary> listShippingProfiles(Long shopId) {
        return jdbcTemplate.query("""
                SELECT `id`, `shop_id`, `pickup_name`, `phone`, `province`, `district`, `ward`, `address`,
                       `ghn_shop_id`, `ghn_district_id`, `ghn_ward_code`
                FROM `shop_shipping_profiles`
                WHERE `shop_id` = ?
                ORDER BY `id`
                """, this::mapShippingProfile, shopId);
    }

    public List<ConversationSummary> listConversations(Long userId, Long shopId, String role) {
        if ("SELLER".equalsIgnoreCase(role)) {
            return jdbcTemplate.query("""
                    SELECT c.`id`, c.`customer_id`, c.`shop_id`, s.`shop_name`, c.`product_id`, c.`last_message`,
                           c.`customer_unread`, c.`seller_unread`, c.`updated_at`
                    FROM `chat_conversations` c
                    JOIN `Shops` s ON s.`id` = c.`shop_id`
                    WHERE c.`shop_id` = ?
                    ORDER BY c.`updated_at` DESC
                    """, this::mapConversation, shopId);
        }
        return jdbcTemplate.query("""
                SELECT c.`id`, c.`customer_id`, c.`shop_id`, s.`shop_name`, c.`product_id`, c.`last_message`,
                       c.`customer_unread`, c.`seller_unread`, c.`updated_at`
                FROM `chat_conversations` c
                JOIN `Shops` s ON s.`id` = c.`shop_id`
                WHERE c.`customer_id` = ?
                ORDER BY c.`updated_at` DESC
                """, this::mapConversation, userId);
    }

    @Transactional
    public ConversationSummary startConversation(Long userId, ConversationRequest request) {
        if (request.shopId() == null) {
            throw badRequest("Thieu shop.");
        }
        Long existing = jdbcTemplate.query("""
                SELECT `id` FROM `chat_conversations` WHERE `customer_id` = ? AND `shop_id` = ?
                """, rs -> rs.next() ? rs.getLong("id") : null, userId, request.shopId());
        if (existing == null) {
            existing = nextId("chat_conversations");
            jdbcTemplate.update("""
                    INSERT INTO `chat_conversations` (`id`, `customer_id`, `shop_id`, `product_id`, `last_message`)
                    VALUES (?, ?, ?, ?, '')
                    """, existing, userId, request.shopId(), request.productId());
        }
        return findConversation(existing);
    }

    public List<MessageSummary> listMessages(Long conversationId, Long userId, String role, Long shopId) {
        ensureConversationAccess(conversationId, userId, role, shopId);
        return jdbcTemplate.query("""
                SELECT `id`, `conversation_id`, `sender_id`, `sender_role`, `message_type`, `body`, `image_url`,
                       `custom_order_id`, `created_at`
                FROM `chat_messages`
                WHERE `conversation_id` = ?
                ORDER BY `id`
                """, this::mapMessage, conversationId);
    }

    public MessageSummary sendMessage(Long userId, String role, Long conversationId, Long shopId, MessageRequest request) {
        ensureConversationAccess(conversationId, userId, role, shopId);
        requireText(request.body(), "Tin nhan khong duoc rong.");
        long id = nextId("chat_messages");
        String senderRole = role == null ? "CUSTOMER" : role;
        jdbcTemplate.update("""
                INSERT INTO `chat_messages` (`id`, `conversation_id`, `sender_id`, `sender_role`, `message_type`, `body`, `image_url`, `custom_order_id`)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """, id, conversationId, userId, senderRole, request.messageType() == null ? "TEXT" : request.messageType(),
                request.body(), request.imageUrl(), request.customOrderId());
        jdbcTemplate.update("UPDATE `chat_conversations` SET `last_message` = ?, `updated_at` = CURRENT_TIMESTAMP WHERE `id` = ?",
                request.body(), conversationId);
        return listMessages(conversationId, userId, role, shopId).stream().filter(message -> message.id().equals(id)).findFirst().orElseThrow();
    }

    @Transactional
    public MessageSummary sendCustomQuote(Long sellerId, Long shopId, Long conversationId, CustomQuoteRequest request) {
        ensureConversationAccess(conversationId, sellerId, "SELLER", shopId);
        requireText(request.title(), "Thieu tieu de bao gia.");
        Long customerId = jdbcTemplate.queryForObject("SELECT `customer_id` FROM `chat_conversations` WHERE `id` = ? AND `shop_id` = ?",
                Long.class, conversationId, shopId);
        long customOrderId = nextId("custom_orders");
        jdbcTemplate.update("""
                INSERT INTO `custom_orders` (`id`, `customer_id`, `shop_id`, `conversation_id`, `title`, `description`, `price`, `status`, `payment_status`)
                VALUES (?, ?, ?, ?, ?, ?, ?, 'PENDING_REVIEW', 'UNPAID')
                """, customOrderId, customerId, shopId, conversationId, request.title(), request.description(), value(request.price()));
        long messageId = nextId("chat_messages");
        String body = "Bao gia custom: " + request.title() + " - " + money(value(request.price())) + " VND";
        jdbcTemplate.update("""
                INSERT INTO `chat_messages` (`id`, `conversation_id`, `sender_id`, `sender_role`, `message_type`, `body`, `custom_order_id`)
                VALUES (?, ?, ?, 'SELLER', 'CUSTOM_ORDER_OFFER', ?, ?)
                """, messageId, conversationId, sellerId, body, customOrderId);
        jdbcTemplate.update("UPDATE `chat_conversations` SET `last_message` = ?, `customer_unread` = COALESCE(`customer_unread`, 0) + 1, `updated_at` = CURRENT_TIMESTAMP WHERE `id` = ?",
                body, conversationId);
        return listMessages(conversationId, sellerId, "SELLER", shopId).stream().filter(message -> message.id().equals(messageId)).findFirst().orElseThrow();
    }

    public List<CustomOrderSummary> listCustomOrders(Long userId, Long shopId) {
        if (shopId != null) {
            return jdbcTemplate.query("""
                    SELECT `id`, `customer_id`, `shop_id`, `conversation_id`, `title`, `description`, `price`,
                           `status`, `payment_status`, `due_date`, `created_at`, `updated_at`
                    FROM `custom_orders`
                    WHERE `shop_id` = ?
                    ORDER BY `updated_at` DESC
                    """, this::mapCustomOrder, shopId);
        }
        if (userId != null) {
            return jdbcTemplate.query("""
                    SELECT `id`, `customer_id`, `shop_id`, `conversation_id`, `title`, `description`, `price`,
                           `status`, `payment_status`, `due_date`, `created_at`, `updated_at`
                    FROM `custom_orders`
                    WHERE `customer_id` = ?
                    ORDER BY `updated_at` DESC
                    """, this::mapCustomOrder, userId);
        }
        return jdbcTemplate.query("""
                SELECT `id`, `customer_id`, `shop_id`, `conversation_id`, `title`, `description`, `price`,
                       `status`, `payment_status`, `due_date`, `created_at`, `updated_at`
                FROM `custom_orders`
                ORDER BY `updated_at` DESC
                """, this::mapCustomOrder);
    }

    public CustomOrderSummary createCustomOrder(Long sellerId, Long shopId, CustomOrderRequest request) {
        if (!shopOwnedByUser(sellerId, shopId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tai khoan khong quan ly shop nay");
        }
        Map<String, Object> conversation = jdbcTemplate.queryForMap("""
                SELECT `customer_id`, `shop_id`
                FROM `chat_conversations`
                WHERE `id` = ? AND `shop_id` = ?
                """, request.conversationId(), shopId);
        Long conversationCustomerId = ((Number) conversation.get("customer_id")).longValue();
        if (request.customerId() == null || !request.customerId().equals(conversationCustomerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tai khoan khong quan ly cuoc hoi thoai nay");
        }
        long id = nextId("custom_orders");
        jdbcTemplate.update("""
                INSERT INTO `custom_orders` (`id`, `customer_id`, `shop_id`, `conversation_id`, `title`, `description`, `price`, `status`, `payment_status`)
                VALUES (?, ?, ?, ?, ?, ?, ?, 'PENDING_REVIEW', 'UNPAID')
                """, id, request.customerId(), shopId, request.conversationId(), request.title(), request.description(), value(request.price()));
        return listCustomOrders(null, shopId).stream().filter(order -> order.id().equals(id)).findFirst().orElseThrow();
    }

    public CustomOrderSummary updateCustomOrderStatus(Long customOrderId, StatusRequest request, Long userId, String role, Long shopId) {
        requireText(request.status(), "Thieu status.");
        Map<String, Object> order = jdbcTemplate.queryForMap("""
                SELECT `customer_id`, `shop_id`
                FROM `custom_orders`
                WHERE `id` = ?
                """, customOrderId);
        Long customerId = ((Number) order.get("customer_id")).longValue();
        Long orderShopId = ((Number) order.get("shop_id")).longValue();
        if ("ADMIN".equalsIgnoreCase(role)) {
            // admin is allowed
        } else if ("SELLER".equalsIgnoreCase(role)) {
            if (shopId == null || !shopId.equals(orderShopId) || !shopOwnedByUser(userId, shopId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tai khoan khong quan ly don nay");
            }
        } else if (!customerId.equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tai khoan khong quan ly don nay");
        }
        jdbcTemplate.update("UPDATE `custom_orders` SET `status` = ?, `updated_at` = CURRENT_TIMESTAMP WHERE `id` = ?",
                request.status(), customOrderId);
        return jdbcTemplate.queryForObject("""
                SELECT `id`, `customer_id`, `shop_id`, `conversation_id`, `title`, `description`, `price`,
                       `status`, `payment_status`, `due_date`, `created_at`, `updated_at`
                FROM `custom_orders`
                WHERE `id` = ?
                """, this::mapCustomOrder, customOrderId);
    }

    public List<CommissionPostSummary> listCommissionPosts() {
        return jdbcTemplate.query("""
                SELECT `id`, `customer_id`, `title`, `description`, `budget_min`, `budget_max`, `desired_timeline`,
                       `reference_images`, `status`, `created_at`
                FROM `commission_posts`
                ORDER BY `created_at` DESC
                """, this::mapCommissionPost);
    }

    public CommissionPostSummary createCommissionPost(Long userId, CommissionPostRequest request) {
        long id = nextId("commission_posts");
        jdbcTemplate.update("""
                INSERT INTO `commission_posts` (`id`, `customer_id`, `title`, `description`, `budget_min`, `budget_max`,
                  `desired_timeline`, `reference_images`, `status`)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'OPEN')
                """, id, userId, request.title(), request.description(), value(request.budgetMin()), value(request.budgetMax()),
                request.desiredTimeline(), request.referenceImages());
        return listCommissionPosts().stream().filter(post -> post.id().equals(id)).findFirst().orElseThrow();
    }

    public ProposalSummary createProposal(Long sellerId, Long shopId, Long postId, ProposalRequest request) {
        long id = nextId("commission_proposals");
        jdbcTemplate.update("""
                INSERT INTO `commission_proposals` (`id`, `post_id`, `seller_id`, `shop_id`, `message`, `proposed_price`, `lead_time_days`, `sketch_image_url`, `status`)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'PENDING')
                """, id, postId, sellerId, shopId, request.message(), value(request.proposedPrice()),
                request.leadTimeDays() == null ? 7 : request.leadTimeDays(), request.sketchImageUrl());
        return listProposals(postId).stream().filter(proposal -> proposal.id().equals(id)).findFirst().orElseThrow();
    }

    @Transactional
    public ProposalSummary acceptProposal(Long userId, Long postId, Long proposalId) {
        jdbcTemplate.update("UPDATE `commission_proposals` SET `status` = 'REJECTED' WHERE `post_id` = ?", postId);
        jdbcTemplate.update("UPDATE `commission_proposals` SET `status` = 'ACCEPTED' WHERE `id` = ? AND `post_id` = ?", proposalId, postId);
        jdbcTemplate.update("UPDATE `commission_posts` SET `status` = 'ASSIGNED' WHERE `id` = ? AND `customer_id` = ?", postId, userId);
        return listProposals(postId).stream().filter(proposal -> proposal.id().equals(proposalId)).findFirst().orElseThrow();
    }

    public List<ProposalSummary> listProposals(Long postId) {
        return jdbcTemplate.query("""
                SELECT p.`id`, p.`post_id`, p.`seller_id`, p.`shop_id`, s.`shop_name`, p.`message`,
                       p.`proposed_price`, p.`lead_time_days`, p.`sketch_image_url`, p.`status`, p.`created_at`
                FROM `commission_proposals` p
                JOIN `Shops` s ON s.`id` = p.`shop_id`
                WHERE p.`post_id` = ?
                ORDER BY p.`created_at` DESC
                """, this::mapProposal, postId);
    }

    public List<MediaFolderSummary> listMediaFolders(Long ownerId) {
        return jdbcTemplate.query("""
                SELECT f.`id`, f.`owner_id`, f.`name`, f.`created_at`,
                       (SELECT COUNT(*) FROM `media_images` i WHERE i.`folder_id` = f.`id`) AS image_count
                FROM `media_folders` f
                WHERE f.`owner_id` = ?
                ORDER BY f.`id`
                """, this::mapMediaFolder, ownerId);
    }

    public MediaFolderSummary createMediaFolder(Long ownerId, MediaFolderRequest request) {
        long id = nextId("media_folders");
        jdbcTemplate.update("INSERT INTO `media_folders` (`id`, `owner_id`, `name`) VALUES (?, ?, ?)",
                id, ownerId, request.name());
        return listMediaFolders(ownerId).stream().filter(folder -> folder.id().equals(id)).findFirst().orElseThrow();
    }

    public MediaImageSummary addMediaImage(Long ownerId, MediaImageRequest request) {
        long id = nextId("media_images");
        jdbcTemplate.update("""
                INSERT INTO `media_images` (`id`, `folder_id`, `owner_id`, `url`, `alt_text`)
                VALUES (?, ?, ?, ?, ?)
                """, id, request.folderId(), ownerId, request.url(), request.altText());
        return jdbcTemplate.queryForObject("""
                SELECT `id`, `folder_id`, `owner_id`, `url`, `alt_text`, `created_at`
                FROM `media_images` WHERE `id` = ?
                """, this::mapMediaImage, id);
    }

    public List<MediaImageSummary> listMediaImages(Long ownerId) {
        return jdbcTemplate.query("""
                SELECT `id`, `folder_id`, `owner_id`, `url`, `alt_text`, `created_at`
                FROM `media_images`
                WHERE `owner_id` = ?
                ORDER BY `id` DESC
                """, this::mapMediaImage, ownerId);
    }

    public List<ReportSummary> listReports() {
        return jdbcTemplate.query("""
                SELECT `id`, `reporter_id`, `type`, `target_id`, `reason`, `status`, `admin_note`, `created_at`, `updated_at`
                FROM `reports`
                ORDER BY `created_at` DESC
                """, this::mapReport);
    }

    public ReportSummary createReport(Long userId, ReportRequest request) {
        long id = nextId("reports");
        jdbcTemplate.update("""
                INSERT INTO `reports` (`id`, `reporter_id`, `type`, `target_id`, `reason`, `status`)
                VALUES (?, ?, ?, ?, ?, 'PENDING')
                """, id, userId, request.type(), request.targetId(), request.reason());
        return listReports().stream().filter(report -> report.id().equals(id)).findFirst().orElseThrow();
    }

    public ReportSummary updateReport(Long reportId, ReportUpdateRequest request) {
        jdbcTemplate.update("UPDATE `reports` SET `status` = ?, `admin_note` = ?, `updated_at` = CURRENT_TIMESTAMP WHERE `id` = ?",
                request.status(), request.adminNote(), reportId);
        return listReports().stream().filter(report -> report.id().equals(reportId)).findFirst().orElseThrow();
    }

    public Map<String, String> platformSettings() {
        return jdbcTemplate.query("SELECT `setting_key`, `setting_value` FROM `platform_settings`",
                rs -> {
                    java.util.LinkedHashMap<String, String> values = new java.util.LinkedHashMap<>();
                    while (rs.next()) values.put(rs.getString("setting_key"), rs.getString("setting_value"));
                    return values;
                });
    }

    public Map<String, String> updatePlatformSettings(Map<String, String> values) {
        values.forEach((key, value) -> jdbcTemplate.update("""
                INSERT INTO `platform_settings` (`setting_key`, `setting_value`) VALUES (?, ?)
                ON DUPLICATE KEY UPDATE `setting_value` = VALUES(`setting_value`)
                """, key, value));
        return platformSettings();
    }

    public List<PaymentHistorySummary> paymentHistory(Long userId) {
        return jdbcTemplate.query("""
                SELECT p.`id`, p.`order_id`, p.`method`, p.`transaction_id`, p.`amount`, p.`status`
                FROM `Payments` p
                JOIN `Orders` o ON o.`id` = p.`order_id`
                WHERE o.`buyer_id` = ?
                ORDER BY p.`id` DESC
                """, this::mapPaymentHistory, userId);
    }

    public SellerAnalyticsResponse sellerAnalytics(Long shopId) {
        return new SellerAnalyticsResponse(
                money(sum("SELECT COALESCE(SUM(`item_subtotal`), 0) FROM `shop_orders` WHERE `shop_id` = ?", shopId)),
                count("SELECT COUNT(*) FROM `shop_orders` WHERE `shop_id` = ?", shopId),
                count("SELECT COUNT(*) FROM `product_reviews` WHERE `shop_id` = ?", shopId),
                count("SELECT COUNT(*) FROM `Products` p JOIN `Storage` st ON st.`product_id` = p.`id` WHERE p.`shop_id` = ? AND st.`quantity` <= st.`low_stock_alert`", shopId),
                jdbcTemplate.query("""
                        SELECT COALESCE(c.`name`, 'Handmade') AS label, COALESCE(SUM(oi.`price_at_purchase` * oi.`quantity`), 0) AS value
                        FROM `OrderItems` oi
                        JOIN `Products` p ON p.`id` = oi.`product_id`
                        LEFT JOIN `Categories` c ON c.`id` = p.`cat_id`
                        WHERE oi.`shop_id` = ?
                        GROUP BY c.`name`
                        ORDER BY value DESC
                        """, (rs, rowNum) -> new AnalyticsPoint(rs.getString("label"), money(rs.getBigDecimal("value"))), shopId),
                listReviews(shopId).stream().limit(5).toList()
        );
    }

    public PaymentReliabilityResponse paymentReliability() {
        return new PaymentReliabilityResponse(
                count("SELECT COUNT(*) FROM `Orders` WHERE `payment_status` = 'PAID' AND `id` NOT IN (SELECT `order_id` FROM `marketplace_ledger` WHERE `entry_type` = 'PAYMENT_CAPTURE' AND `order_id` IS NOT NULL)"),
                count("SELECT COUNT(*) FROM `payment_transactions` WHERE `status` = 'PENDING'"),
                count("SELECT COUNT(*) FROM `Payments` WHERE `status` = 'failed'"),
                count("SELECT COUNT(*) FROM `marketplace_ledger`")
        );
    }

    private VoucherSummary findVoucher(long id) {
        return jdbcTemplate.queryForObject("""
                SELECT `id`, `code`, `scope`, `shop_id`, `category_id`, `title`, `discount_percent`,
                       `max_discount_amount`, `min_order_amount`, `usage_limit`, `used_count`,
                       `per_user_limit`, `active`, `end_at`
                FROM `vouchers` WHERE `id` = ?
                """, this::mapVoucher, id);
    }

    private ConversationSummary findConversation(long id) {
        return jdbcTemplate.queryForObject("""
                SELECT c.`id`, c.`customer_id`, c.`shop_id`, s.`shop_name`, c.`product_id`, c.`last_message`,
                       c.`customer_unread`, c.`seller_unread`, c.`updated_at`
                FROM `chat_conversations` c
                JOIN `Shops` s ON s.`id` = c.`shop_id`
                WHERE c.`id` = ?
                """, this::mapConversation, id);
    }

    private void ensureConversationAccess(Long conversationId, Long userId, String role, Long shopId) {
        Map<String, Object> row = jdbcTemplate.queryForMap("""
                SELECT `customer_id`, `shop_id`
                FROM `chat_conversations`
                WHERE `id` = ?
                """, conversationId);
        Long customerId = ((Number) row.get("customer_id")).longValue();
        Long conversationShopId = ((Number) row.get("shop_id")).longValue();

        if ("ADMIN".equalsIgnoreCase(role)) {
            return;
        }
        if ("SELLER".equalsIgnoreCase(role)) {
            if (shopId == null || !shopId.equals(conversationShopId) || !shopOwnedByUser(userId, shopId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tai khoan khong quan ly cuoc hoi thoai nay");
            }
            return;
        }
        if (!customerId.equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tai khoan khong quan ly cuoc hoi thoai nay");
        }
    }

    private boolean shopOwnedByUser(Long userId, Long shopId) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM `Shops`
                WHERE `id` = ? AND `owner_id` = ?
                """, Integer.class, shopId, userId);
        return count != null && count > 0;
    }

    private long nextId(String tableName) {
        Long id = jdbcTemplate.queryForObject("SELECT COALESCE(MAX(`id`), 0) + 1 FROM `" + tableName + "`", Long.class);
        return id == null ? 1L : id;
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

    private BigDecimal value(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private BigDecimal money(BigDecimal value) {
        return value(value).setScale(2, java.math.RoundingMode.HALF_UP);
    }

    private LocalDateTime time(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private void requireText(String value, String message) {
        if (value == null || value.isBlank()) throw badRequest(message);
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    private VoucherSummary mapVoucher(ResultSet rs, int rowNum) throws SQLException {
        return new VoucherSummary(rs.getLong("id"), rs.getString("code"), rs.getString("scope"), nullableLong(rs, "shop_id"),
                nullableInt(rs, "category_id"), rs.getString("title"), money(rs.getBigDecimal("discount_percent")),
                money(rs.getBigDecimal("max_discount_amount")), money(rs.getBigDecimal("min_order_amount")),
                rs.getInt("usage_limit"), rs.getInt("used_count"), rs.getInt("per_user_limit"), rs.getBoolean("active"), time(rs.getTimestamp("end_at")));
    }

    private CategoryAdminSummary mapCategory(ResultSet rs, int rowNum) throws SQLException {
        return new CategoryAdminSummary(rs.getLong("id"), nullableLong(rs, "parent_id"), rs.getString("name"),
                rs.getString("slug"), rs.getString("image_url"), rs.getString("status"), rs.getInt("product_count"));
    }

    private ProductAdminSummary mapProductAdmin(ResultSet rs, int rowNum) throws SQLException {
        return new ProductAdminSummary(rs.getLong("id"), rs.getLong("shop_id"), nullableInt(rs, "cat_id"),
                rs.getString("category_name"), rs.getString("name"), rs.getString("sku"), money(rs.getBigDecimal("price")),
                rs.getString("description"), rs.getString("tags"), rs.getString("approval_status"), rs.getString("sale_status"),
                rs.getString("image_url"), rs.getString("options_json"), rs.getBoolean("requires_personalization"),
                rs.getInt("processing_days"), rs.getInt("stock"), rs.getInt("low_stock_alert"));
    }

    private AdminUserSummary mapAdminUser(ResultSet rs, int rowNum) throws SQLException {
        return new AdminUserSummary(
                rs.getLong("id"),
                rs.getString("username"),
                rs.getString("full_name"),
                rs.getString("email"),
                rs.getString("phone"),
                rs.getString("role"),
                rs.getString("status"),
                rs.getInt("orders_count"),
                money(rs.getBigDecimal("total_spent")),
                money(rs.getBigDecimal("sales"))
        );
    }

    private ReviewModerationSummary mapReviewModeration(ResultSet rs, int rowNum) throws SQLException {
        return new ReviewModerationSummary(
                rs.getLong("id"),
                rs.getLong("product_id"),
                rs.getString("product_name"),
                rs.getLong("shop_id"),
                rs.getString("shop_name"),
                rs.getLong("user_id"),
                rs.getString("customer_name"),
                rs.getInt("rating"),
                rs.getString("comment"),
                rs.getString("seller_reply"),
                time(rs.getTimestamp("created_at"))
        );
    }

    private void logInventory(Long productId, int changeQty, String reason, String note) {
        jdbcTemplate.update("""
                INSERT INTO `inventory_logs` (`id`, `product_id`, `change_qty`, `reason`, `note`)
                VALUES (?, ?, ?, ?, ?)
                """, nextId("inventory_logs"), productId, changeQty, reason, note);
    }

    private String slugOrDefault(String slug, String name) {
        String raw = slug == null || slug.isBlank() ? name : slug;
        return raw == null ? "" : raw.trim().toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
    }

    private FlashSaleSummary mapFlashSale(ResultSet rs, int rowNum) throws SQLException {
        return new FlashSaleSummary(rs.getLong("id"), rs.getString("name"), rs.getString("description"), rs.getString("banner_url"),
                rs.getString("state"), money(rs.getBigDecimal("discount_percent")), rs.getInt("max_units"),
                rs.getInt("sold_units"), rs.getInt("reserved_units"), rs.getInt("per_user_limit"),
                time(rs.getTimestamp("start_at")), time(rs.getTimestamp("end_at")));
    }

    private GiftWrapTier mapGiftWrap(ResultSet rs, int rowNum) throws SQLException {
        return new GiftWrapTier(rs.getLong("id"), rs.getString("name"), rs.getString("description"),
                money(rs.getBigDecimal("price")), rs.getBoolean("has_card"), rs.getInt("sort_order"), rs.getBoolean("active"));
    }

    private ShippingProfileSummary mapShippingProfile(ResultSet rs, int rowNum) throws SQLException {
        return new ShippingProfileSummary(rs.getLong("id"), rs.getLong("shop_id"), rs.getString("pickup_name"), rs.getString("phone"),
                rs.getString("province"), rs.getString("district"), rs.getString("ward"), rs.getString("address"),
                rs.getString("ghn_shop_id"), rs.getString("ghn_district_id"), rs.getString("ghn_ward_code"));
    }

    private ConversationSummary mapConversation(ResultSet rs, int rowNum) throws SQLException {
        return new ConversationSummary(rs.getLong("id"), rs.getLong("customer_id"), rs.getLong("shop_id"), rs.getString("shop_name"),
                nullableLong(rs, "product_id"), rs.getString("last_message"), rs.getInt("customer_unread"),
                rs.getInt("seller_unread"), time(rs.getTimestamp("updated_at")));
    }

    private MessageSummary mapMessage(ResultSet rs, int rowNum) throws SQLException {
        return new MessageSummary(rs.getLong("id"), rs.getLong("conversation_id"), rs.getLong("sender_id"), rs.getString("sender_role"),
                rs.getString("message_type"), rs.getString("body"), rs.getString("image_url"), nullableLong(rs, "custom_order_id"),
                time(rs.getTimestamp("created_at")));
    }

    private CustomOrderSummary mapCustomOrder(ResultSet rs, int rowNum) throws SQLException {
        return new CustomOrderSummary(rs.getLong("id"), rs.getLong("customer_id"), rs.getLong("shop_id"), nullableLong(rs, "conversation_id"),
                rs.getString("title"), rs.getString("description"), money(rs.getBigDecimal("price")), rs.getString("status"),
                rs.getString("payment_status"), rs.getString("due_date"), time(rs.getTimestamp("created_at")), time(rs.getTimestamp("updated_at")));
    }

    private CommissionPostSummary mapCommissionPost(ResultSet rs, int rowNum) throws SQLException {
        return new CommissionPostSummary(rs.getLong("id"), rs.getLong("customer_id"), rs.getString("title"), rs.getString("description"),
                money(rs.getBigDecimal("budget_min")), money(rs.getBigDecimal("budget_max")), rs.getString("desired_timeline"),
                rs.getString("reference_images"), rs.getString("status"), time(rs.getTimestamp("created_at")));
    }

    private ProposalSummary mapProposal(ResultSet rs, int rowNum) throws SQLException {
        return new ProposalSummary(rs.getLong("id"), rs.getLong("post_id"), rs.getLong("seller_id"), rs.getLong("shop_id"),
                rs.getString("shop_name"), rs.getString("message"), money(rs.getBigDecimal("proposed_price")),
                rs.getInt("lead_time_days"), rs.getString("sketch_image_url"), rs.getString("status"), time(rs.getTimestamp("created_at")));
    }

    private MediaFolderSummary mapMediaFolder(ResultSet rs, int rowNum) throws SQLException {
        return new MediaFolderSummary(rs.getLong("id"), rs.getLong("owner_id"), rs.getString("name"), rs.getInt("image_count"), time(rs.getTimestamp("created_at")));
    }

    private MediaImageSummary mapMediaImage(ResultSet rs, int rowNum) throws SQLException {
        return new MediaImageSummary(rs.getLong("id"), rs.getLong("folder_id"), rs.getLong("owner_id"), rs.getString("url"),
                rs.getString("alt_text"), time(rs.getTimestamp("created_at")));
    }

    private ReportSummary mapReport(ResultSet rs, int rowNum) throws SQLException {
        return new ReportSummary(rs.getLong("id"), rs.getLong("reporter_id"), rs.getString("type"), nullableLong(rs, "target_id"),
                rs.getString("reason"), rs.getString("status"), rs.getString("admin_note"), time(rs.getTimestamp("created_at")),
                time(rs.getTimestamp("updated_at")));
    }

    private PaymentHistorySummary mapPaymentHistory(ResultSet rs, int rowNum) throws SQLException {
        return new PaymentHistorySummary(rs.getLong("id"), rs.getLong("order_id"), rs.getString("method"),
                rs.getString("transaction_id"), money(rs.getBigDecimal("amount")), rs.getString("status"));
    }

    private Long nullableLong(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }

    private Integer nullableInt(ResultSet rs, String column) throws SQLException {
        int value = rs.getInt(column);
        return rs.wasNull() ? null : value;
    }

    public record MarketplaceModulesResponse(
            List<VoucherSummary> vouchers,
            List<FlashSaleSummary> flashSales,
            List<GiftWrapTier> giftWrapTiers,
            List<CommissionPostSummary> commissions,
            List<CustomOrderSummary> customOrders,
            List<ReportSummary> reports,
            Map<String, String> settings,
            PaymentReliabilityResponse reliability
    ) {}
    public record CategoryAdminSummary(Long id, Long parentId, String name, String slug, String imageUrl, String status, int productCount) {}
    public record CategoryRequest(Long parentId, String name, String slug, String imageUrl, String status) {}
    public record ProductAdminSummary(Long id, Long shopId, Integer categoryId, String categoryName, String name, String sku,
                                      BigDecimal price, String description, String tags, String approvalStatus,
                                      String saleStatus, String imageUrl, String optionsJson, boolean requiresPersonalization,
                                      int processingDays, int stock, int lowStockAlert) {}
    public record ProductWriteRequest(Integer categoryId, String name, String sku, BigDecimal price, String description,
                                      String tags, Boolean customizable, String imageUrl, String optionsJson,
                                      Boolean requiresPersonalization, Integer processingDays, Integer stock,
                                      Integer lowStockAlert) {}
    public record AdminUserSummary(Long id, String username, String fullName, String email, String phone, String role,
                                   String status, int ordersCount, BigDecimal totalSpent, BigDecimal sales) {}
    public record AdminUserRequest(String username, String fullName, String email, String phone, String role, String status) {}
    public record ReviewModerationSummary(Long id, Long productId, String productName, Long shopId, String shopName,
                                          Long userId, String customerName, int rating, String comment,
                                          String sellerReply, LocalDateTime createdAt) {}
    public record ReviewReplyRequest(String reply) {}
    public record VoucherSummary(Long id, String code, String scope, Long shopId, Integer categoryId, String title,
                                 BigDecimal discountPercent, BigDecimal maxDiscountAmount, BigDecimal minOrderAmount,
                                 int usageLimit, int usedCount, int perUserLimit, boolean active, LocalDateTime endAt) {}
    public record VoucherRequest(String code, String scope, Integer categoryId, String title, BigDecimal discountPercent,
                                 BigDecimal maxDiscountAmount, BigDecimal minOrderAmount, Integer usageLimit, Integer perUserLimit) {}
    public record VoucherApplyRequest(String code, BigDecimal subtotal) {}
    public record VoucherApplyResponse(String code, BigDecimal discountAmount, BigDecimal totalAfterDiscount, String status) {}
    public record FlashSaleSummary(Long id, String name, String description, String bannerUrl, String state,
                                   BigDecimal discountPercent, int maxUnits, int soldUnits, int reservedUnits,
                                   int perUserLimit, LocalDateTime startAt, LocalDateTime endAt) {}
    public record FlashSaleRequest(String name, String description, String bannerUrl, BigDecimal discountPercent,
                                   Integer maxUnits, Integer perUserLimit) {}
    public record GiftWrapTier(Long id, String name, String description, BigDecimal price, boolean hasCard, int sortOrder, boolean active) {}
    public record ShippingProfileSummary(Long id, Long shopId, String pickupName, String phone, String province,
                                         String district, String ward, String address, String ghnShopId,
                                         String ghnDistrictId, String ghnWardCode) {}
    public record ConversationSummary(Long id, Long customerId, Long shopId, String shopName, Long productId,
                                      String lastMessage, int customerUnread, int sellerUnread, LocalDateTime updatedAt) {}
    public record ConversationRequest(Long shopId, Long productId) {}
    public record MessageSummary(Long id, Long conversationId, Long senderId, String senderRole, String messageType,
                                 String body, String imageUrl, Long customOrderId, LocalDateTime createdAt) {}
    public record MessageRequest(String messageType, String body, String imageUrl, Long customOrderId) {}
    public record CustomQuoteRequest(String title, String description, BigDecimal price) {}
    public record CustomOrderSummary(Long id, Long customerId, Long shopId, Long conversationId, String title,
                                     String description, BigDecimal price, String status, String paymentStatus,
                                     String dueDate, LocalDateTime createdAt, LocalDateTime updatedAt) {}
    public record CustomOrderRequest(Long customerId, Long conversationId, String title, String description, BigDecimal price) {}
    public record StatusRequest(String status) {}
    public record CommissionPostSummary(Long id, Long customerId, String title, String description, BigDecimal budgetMin,
                                        BigDecimal budgetMax, String desiredTimeline, String referenceImages,
                                        String status, LocalDateTime createdAt) {}
    public record CommissionPostRequest(String title, String description, BigDecimal budgetMin, BigDecimal budgetMax,
                                        String desiredTimeline, String referenceImages) {}
    public record ProposalSummary(Long id, Long postId, Long sellerId, Long shopId, String shopName, String message,
                                  BigDecimal proposedPrice, int leadTimeDays, String sketchImageUrl,
                                  String status, LocalDateTime createdAt) {}
    public record ProposalRequest(String message, BigDecimal proposedPrice, Integer leadTimeDays, String sketchImageUrl) {}
    public record MediaFolderSummary(Long id, Long ownerId, String name, int imageCount, LocalDateTime createdAt) {}
    public record MediaFolderRequest(String name) {}
    public record MediaImageSummary(Long id, Long folderId, Long ownerId, String url, String altText, LocalDateTime createdAt) {}
    public record MediaImageRequest(Long folderId, String url, String altText) {}
    public record ReportSummary(Long id, Long reporterId, String type, Long targetId, String reason, String status,
                                String adminNote, LocalDateTime createdAt, LocalDateTime updatedAt) {}
    public record ReportRequest(String type, Long targetId, String reason) {}
    public record ReportUpdateRequest(String status, String adminNote) {}
    public record PaymentHistorySummary(Long id, Long orderId, String method, String transactionId, BigDecimal amount, String status) {}
    public record AnalyticsPoint(String label, BigDecimal value) {}
    public record SellerAnalyticsResponse(BigDecimal revenue, int orderCount, int reviewCount, int lowStockCount,
                                          List<AnalyticsPoint> revenueByCategory,
                                          List<ReviewModerationSummary> latestReviews) {}
    public record PaymentReliabilityResponse(int paidMissingLedger, int pendingTransactions, int failedPayments, int ledgerEntries) {}
}
