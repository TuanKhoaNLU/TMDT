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
public class CustomOrderService {
    private final JdbcTemplate jdbcTemplate;
    public CustomOrderService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
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