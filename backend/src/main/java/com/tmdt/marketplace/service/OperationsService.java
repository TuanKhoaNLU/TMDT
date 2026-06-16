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
public class OperationsService {
    private final JdbcTemplate jdbcTemplate;
    public OperationsService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
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

}