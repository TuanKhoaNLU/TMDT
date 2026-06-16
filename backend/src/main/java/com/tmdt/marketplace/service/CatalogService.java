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

import com.tmdt.marketplace.service.MarketplaceModuleService.*;

@Service
public class CatalogService {
    private final JdbcTemplate jdbcTemplate;
    public CatalogService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
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

    public List<ShippingProfileSummary> listShippingProfiles(Long shopId) {
        return jdbcTemplate.query("""
                SELECT `id`, `shop_id`, `pickup_name`, `phone`, `province`, `district`, `ward`, `address`,
                       `ghn_shop_id`, `ghn_district_id`, `ghn_ward_code`
                FROM `shop_shipping_profiles`
                WHERE `shop_id` = ?
                ORDER BY `id`
                """, this::mapShippingProfile, shopId);
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

}