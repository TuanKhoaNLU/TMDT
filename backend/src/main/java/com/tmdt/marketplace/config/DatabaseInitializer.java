package com.tmdt.marketplace.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DatabaseInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DatabaseInitializer.class);

    private final JdbcTemplate jdbcTemplate;

    public DatabaseInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            ensureDatabaseCharset();
            createBaseTables();
            addMarketplaceColumns();
            createMarketplaceTables();
            ensureTableCharsets();
            seedDemoData();
            log.info("Marketplace MySQL schema is ready.");
        } catch (DataAccessException ex) {
            log.warn("MySQL is not ready; the app started but database APIs need MySQL. {}", ex.getMessage());
        }
    }

    private void ensureDatabaseCharset() {
        String databaseName = jdbcTemplate.queryForObject("SELECT DATABASE()", String.class);
        if (databaseName != null && !databaseName.isBlank()) {
            jdbcTemplate.execute("ALTER DATABASE `" + databaseName + "` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
        }
    }

    private void ensureTableCharsets() {
        var tables = jdbcTemplate.queryForList("""
                SELECT table_name
                FROM information_schema.tables
                WHERE table_schema = DATABASE()
                  AND table_type = 'BASE TABLE'
                """, String.class);
        for (String table : tables) {
            try {
                jdbcTemplate.execute("ALTER TABLE `" + table.replace("`", "``") + "` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
            } catch (DataAccessException ex) {
                log.warn("Could not convert table {} to utf8mb4: {}", table, ex.getMessage());
            }
        }
    }

    private void createBaseTables() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS `Users` (
                  `id` bigint PRIMARY KEY,
                  `full_name` varchar(255),
                  `phone` varchar(255),
                  `address` text,
                  `created_at` timestamp DEFAULT CURRENT_TIMESTAMP
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS `Accounts` (
                  `id` bigint PRIMARY KEY,
                  `user_id` bigint,
                  `username` varchar(255) UNIQUE,
                  `password_hash` varchar(255),
                  `email` varchar(255) UNIQUE,
                  `email_crc` bigint,
                  `status` int
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS `Shops` (
                  `id` bigint PRIMARY KEY,
                  `owner_id` bigint,
                  `shop_name` varchar(255),
                  `logo_url` varchar(255),
                  `description` text,
                  `rating` float,
                  `created_at` timestamp DEFAULT CURRENT_TIMESTAMP
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS `Categories` (
                  `id` int PRIMARY KEY,
                  `parent_id` int,
                  `name` varchar(255),
                  `slug` varchar(255)
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS `Products` (
                  `id` bigint PRIMARY KEY,
                  `shop_id` bigint,
                  `cat_id` int,
                  `name` varchar(255),
                  `price` decimal(18,2),
                  `description` text,
                  `is_custom` boolean,
                  `avg_rating` float,
                  `status` enum('active','hidden','out_of_stock','low_stock')
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS `ProductImages` (
                  `id` bigint PRIMARY KEY,
                  `product_id` bigint,
                  `url` varchar(500),
                  `is_main` boolean
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS `Storage` (
                  `id` bigint PRIMARY KEY,
                  `product_id` bigint,
                  `warehouse_id` int,
                  `quantity` int,
                  `reserved_quantity` int,
                  `low_stock_alert` int,
                  `last_updated` timestamp DEFAULT CURRENT_TIMESTAMP
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS `Orders` (
                  `id` bigint PRIMARY KEY,
                  `buyer_id` bigint,
                  `total_price` decimal(18,2),
                  `status` varchar(255),
                  `created_at` timestamp DEFAULT CURRENT_TIMESTAMP,
                  `shipping_address` text,
                  `receiver_name` varchar(255),
                  `phone_number` varchar(255)
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS `OrderItems` (
                  `id` bigint PRIMARY KEY,
                  `order_id` bigint,
                  `product_id` bigint,
                  `shop_id` bigint,
                  `quantity` int,
                  `price_at_purchase` decimal(18,2),
                  `merchant_status` varchar(255)
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS `Payments` (
                  `id` bigint PRIMARY KEY,
                  `order_id` bigint,
                  `method` varchar(255),
                  `transaction_id` varchar(255),
                  `amount` decimal(18,2),
                  `status` enum('pending','successed','failed','refunded')
                )
                """);
    }

    private void addMarketplaceColumns() {
        addColumnIfMissing("Orders", "subtotal_price", "`subtotal_price` decimal(18,2) DEFAULT 0");
        addColumnIfMissing("Orders", "shipping_fee", "`shipping_fee` decimal(18,2) DEFAULT 0");
        addColumnIfMissing("Orders", "payment_method", "`payment_method` varchar(30)");
        addColumnIfMissing("Orders", "payment_status", "`payment_status` varchar(30)");
        addColumnIfMissing("Orders", "receiver_phone", "`receiver_phone` varchar(50)");
        addColumnIfMissing("Orders", "receiver_province", "`receiver_province` varchar(120)");
        addColumnIfMissing("Orders", "receiver_district", "`receiver_district` varchar(120)");
        addColumnIfMissing("Orders", "receiver_ward", "`receiver_ward` varchar(120)");
        addColumnIfMissing("Orders", "receiver_address", "`receiver_address` text");
        addColumnIfMissing("Orders", "voucher_code", "`voucher_code` varchar(80)");
        addColumnIfMissing("Orders", "discount_amount", "`discount_amount` decimal(18,2) DEFAULT 0");
        addColumnIfMissing("Orders", "gift_wrap_tier_id", "`gift_wrap_tier_id` bigint");
        addColumnIfMissing("Orders", "gift_wrap_snapshot", "`gift_wrap_snapshot` text");
        addColumnIfMissing("Orders", "gift_message", "`gift_message` text");
        addColumnIfMissing("Orders", "reward_points_used", "`reward_points_used` int DEFAULT 0");
        addColumnIfMissing("Orders", "idempotency_key", "`idempotency_key` varchar(120)");
        addColumnIfMissing("Orders", "updated_at", "`updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP");

        addColumnIfMissing("OrderItems", "shop_order_id", "`shop_order_id` bigint");
        addColumnIfMissing("OrderItems", "product_name_snapshot", "`product_name_snapshot` varchar(255)");
        addColumnIfMissing("OrderItems", "shop_name_snapshot", "`shop_name_snapshot` varchar(255)");
        addColumnIfMissing("OrderItems", "custom_options_json", "`custom_options_json` text");
        addColumnIfMissing("OrderItems", "note", "`note` text");
        
        addColumnIfMissing("Accounts", "role", "`role` varchar(50) DEFAULT 'BUYER'");
        addColumnIfMissing("Users", "reward_points", "`reward_points` int DEFAULT 0");
        addColumnIfMissing("Users", "status", "`status` varchar(30) DEFAULT 'ACTIVE'");
        addColumnIfMissing("Shops", "hero_url", "`hero_url` varchar(500)");
        addColumnIfMissing("Shops", "about", "`about` text");
        addColumnIfMissing("Shops", "materials", "`materials` varchar(500)");
        addColumnIfMissing("Shops", "years_experience", "`years_experience` int DEFAULT 1");
        addColumnIfMissing("Shops", "verified_artisan", "`verified_artisan` boolean DEFAULT false");
        addColumnIfMissing("Shops", "status", "`status` varchar(30) DEFAULT 'ACTIVE'");
        addColumnIfMissing("Categories", "status", "`status` varchar(30) DEFAULT 'ACTIVE'");
        addColumnIfMissing("Categories", "image_url", "`image_url` varchar(500)");
        addColumnIfMissing("Products", "sku", "`sku` varchar(120)");
        addColumnIfMissing("Products", "tags", "`tags` varchar(500)");
        addColumnIfMissing("Products", "approval_status", "`approval_status` varchar(30) DEFAULT 'APPROVED'");
        addColumnIfMissing("Products", "main_image_url", "`main_image_url` varchar(500)");
        addColumnIfMissing("Products", "options_json", "`options_json` text");
        addColumnIfMissing("Products", "requires_personalization", "`requires_personalization` boolean DEFAULT false");
        addColumnIfMissing("Products", "processing_days", "`processing_days` int DEFAULT 3");
        addColumnIfMissing("Products", "shipping_profile_id", "`shipping_profile_id` bigint");
        addColumnIfMissing("Products", "view_count", "`view_count` int DEFAULT 0");
    }

    private void createMarketplaceTables() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS `shop_orders` (
                  `id` bigint PRIMARY KEY,
                  `order_id` bigint NOT NULL,
                  `shop_id` bigint NOT NULL,
                  `shop_name_snapshot` varchar(255),
                  `item_subtotal` decimal(18,2) DEFAULT 0,
                  `shipping_fee` decimal(18,2) DEFAULT 0,
                  `commission_amount` decimal(18,2) DEFAULT 0,
                  `payout_amount` decimal(18,2) DEFAULT 0,
                  `cod_amount` decimal(18,2) DEFAULT 0,
                  `status` varchar(40),
                  `created_at` timestamp DEFAULT CURRENT_TIMESTAMP,
                  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                  INDEX `idx_shop_orders_order` (`order_id`),
                  INDEX `idx_shop_orders_shop` (`shop_id`)
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS `payment_transactions` (
                  `id` bigint PRIMARY KEY,
                  `order_id` bigint NOT NULL,
                  `provider` varchar(40),
                  `method` varchar(30),
                  `transaction_ref` varchar(120),
                  `provider_transaction_id` varchar(120),
                  `amount` decimal(18,2) DEFAULT 0,
                  `status` varchar(40),
                  `raw_payload` text,
                  `created_at` timestamp DEFAULT CURRENT_TIMESTAMP,
                  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                  INDEX `idx_payment_transactions_order` (`order_id`),
                  UNIQUE KEY `uk_payment_transaction_ref` (`transaction_ref`)
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS `shipments` (
                  `id` bigint PRIMARY KEY,
                  `shop_order_id` bigint NOT NULL,
                  `provider` varchar(40),
                  `tracking_code` varchar(120),
                  `service_name` varchar(120),
                  `fee` decimal(18,2) DEFAULT 0,
                  `cod_amount` decimal(18,2) DEFAULT 0,
                  `status` varchar(40),
                  `raw_payload` text,
                  `created_at` timestamp DEFAULT CURRENT_TIMESTAMP,
                  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                  INDEX `idx_shipments_shop_order` (`shop_order_id`)
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS `return_requests` (
                  `id` bigint PRIMARY KEY,
                  `order_id` bigint NOT NULL,
                  `shop_order_id` bigint,
                  `buyer_id` bigint NOT NULL,
                  `shop_id` bigint,
                  `reason` text,
                  `status` varchar(40),
                  `refund_amount` decimal(18,2) DEFAULT 0,
                  `admin_note` text,
                  `created_at` timestamp DEFAULT CURRENT_TIMESTAMP,
                  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                  INDEX `idx_return_requests_order` (`order_id`),
                  INDEX `idx_return_requests_shop` (`shop_id`)
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS `shop_shipping_profiles` (
                  `id` bigint PRIMARY KEY,
                  `shop_id` bigint NOT NULL,
                  `pickup_name` varchar(255),
                  `phone` varchar(50),
                  `province` varchar(120),
                  `district` varchar(120),
                  `ward` varchar(120),
                  `address` text,
                  `ghn_shop_id` varchar(80),
                  `ghn_district_id` varchar(80),
                  `ghn_ward_code` varchar(80),
                  UNIQUE KEY `uk_shop_shipping_profiles_shop` (`shop_id`)
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS `user_addresses` (
                  `id` bigint PRIMARY KEY,
                  `user_id` bigint NOT NULL,
                  `label` varchar(80),
                  `receiver_name` varchar(255),
                  `phone` varchar(50),
                  `province` varchar(120),
                  `district` varchar(120),
                  `ward` varchar(120),
                  `address` text,
                  `is_default` boolean DEFAULT false,
                  `created_at` timestamp DEFAULT CURRENT_TIMESTAMP,
                  INDEX `idx_user_addresses_user` (`user_id`)
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS `wishlists` (
                  `user_id` bigint NOT NULL,
                  `product_id` bigint NOT NULL,
                  `created_at` timestamp DEFAULT CURRENT_TIMESTAMP,
                  PRIMARY KEY (`user_id`, `product_id`)
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS `shop_follows` (
                  `user_id` bigint NOT NULL,
                  `shop_id` bigint NOT NULL,
                  `created_at` timestamp DEFAULT CURRENT_TIMESTAMP,
                  PRIMARY KEY (`user_id`, `shop_id`)
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS `product_reviews` (
                  `id` bigint PRIMARY KEY,
                  `product_id` bigint NOT NULL,
                  `shop_id` bigint NOT NULL,
                  `user_id` bigint NOT NULL,
                  `rating` int NOT NULL,
                  `comment` text,
                  `seller_reply` text,
                  `created_at` timestamp DEFAULT CURRENT_TIMESTAMP,
                  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                  INDEX `idx_product_reviews_product` (`product_id`),
                  UNIQUE KEY `uk_product_reviews_user_product` (`user_id`, `product_id`)
                )
                """);
        addUniqueIndexIfMissing("product_reviews", "uk_product_reviews_user_product", "`user_id`, `product_id`");
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS `product_questions` (
                  `id` bigint PRIMARY KEY,
                  `product_id` bigint NOT NULL,
                  `user_id` bigint,
                  `question` text NOT NULL,
                  `answer` text,
                  `status` varchar(30) DEFAULT 'PUBLISHED',
                  `created_at` timestamp DEFAULT CURRENT_TIMESTAMP,
                  `answered_at` timestamp NULL,
                  INDEX `idx_product_questions_product` (`product_id`)
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS `notifications` (
                  `id` bigint PRIMARY KEY,
                  `user_id` bigint NOT NULL,
                  `type` varchar(80),
                  `title` varchar(255),
                  `message` text,
                  `read_at` timestamp NULL,
                  `dedupe_key` varchar(160),
                  `created_at` timestamp DEFAULT CURRENT_TIMESTAMP,
                  UNIQUE KEY `uk_notifications_dedupe` (`user_id`, `dedupe_key`),
                  INDEX `idx_notifications_user` (`user_id`)
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS `homepage_banners` (
                  `id` bigint PRIMARY KEY,
                  `title` varchar(255),
                  `subtitle` text,
                  `image_url` varchar(500),
                  `link_url` varchar(255),
                  `sort_order` int DEFAULT 0,
                  `active` boolean DEFAULT true
                )
                """);
        createMarketplaceModuleTables();
    }

    private void createMarketplaceModuleTables() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS `vouchers` (
                  `id` bigint PRIMARY KEY,
                  `code` varchar(80) UNIQUE,
                  `scope` varchar(30),
                  `shop_id` bigint,
                  `category_id` int,
                  `title` varchar(255),
                  `discount_percent` decimal(5,2) DEFAULT 0,
                  `max_discount_amount` decimal(18,2) DEFAULT 0,
                  `min_order_amount` decimal(18,2) DEFAULT 0,
                  `usage_limit` int DEFAULT 0,
                  `used_count` int DEFAULT 0,
                  `per_user_limit` int DEFAULT 1,
                  `active` boolean DEFAULT true,
                  `end_at` timestamp NULL,
                  `created_at` timestamp DEFAULT CURRENT_TIMESTAMP
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS `flash_sales` (
                  `id` bigint PRIMARY KEY,
                  `name` varchar(255),
                  `description` text,
                  `banner_url` varchar(500),
                  `state` varchar(30),
                  `discount_percent` decimal(5,2) DEFAULT 0,
                  `max_units` int DEFAULT 0,
                  `sold_units` int DEFAULT 0,
                  `reserved_units` int DEFAULT 0,
                  `per_user_limit` int DEFAULT 1,
                  `start_at` timestamp NULL,
                  `end_at` timestamp NULL
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS `flash_sale_products` (
                  `flash_sale_id` bigint NOT NULL,
                  `product_id` bigint NOT NULL,
                  PRIMARY KEY (`flash_sale_id`, `product_id`)
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS `gift_wrap_tiers` (
                  `id` bigint PRIMARY KEY,
                  `name` varchar(255),
                  `description` text,
                  `price` decimal(18,2) DEFAULT 0,
                  `has_card` boolean DEFAULT false,
                  `sort_order` int DEFAULT 0,
                  `active` boolean DEFAULT true
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS `chat_conversations` (
                  `id` bigint PRIMARY KEY,
                  `customer_id` bigint NOT NULL,
                  `shop_id` bigint NOT NULL,
                  `product_id` bigint,
                  `last_message` text,
                  `customer_unread` int DEFAULT 0,
                  `seller_unread` int DEFAULT 0,
                  `created_at` timestamp DEFAULT CURRENT_TIMESTAMP,
                  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                  UNIQUE KEY `uk_chat_customer_shop` (`customer_id`, `shop_id`)
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS `chat_messages` (
                  `id` bigint PRIMARY KEY,
                  `conversation_id` bigint NOT NULL,
                  `sender_id` bigint NOT NULL,
                  `sender_role` varchar(30),
                  `message_type` varchar(40),
                  `body` text,
                  `image_url` varchar(500),
                  `custom_order_id` bigint,
                  `created_at` timestamp DEFAULT CURRENT_TIMESTAMP,
                  INDEX `idx_chat_messages_conversation` (`conversation_id`)
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS `custom_orders` (
                  `id` bigint PRIMARY KEY,
                  `customer_id` bigint NOT NULL,
                  `shop_id` bigint NOT NULL,
                  `conversation_id` bigint,
                  `title` varchar(255),
                  `description` text,
                  `price` decimal(18,2) DEFAULT 0,
                  `status` varchar(40),
                  `payment_status` varchar(40),
                  `due_date` date NULL,
                  `created_at` timestamp DEFAULT CURRENT_TIMESTAMP,
                  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS `custom_order_events` (
                  `id` bigint PRIMARY KEY,
                  `custom_order_id` bigint NOT NULL,
                  `title` varchar(255),
                  `note` text,
                  `image_url` varchar(500),
                  `status` varchar(40),
                  `created_at` timestamp DEFAULT CURRENT_TIMESTAMP
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS `custom_order_revisions` (
                  `id` bigint PRIMARY KEY,
                  `custom_order_id` bigint NOT NULL,
                  `customer_id` bigint NOT NULL,
                  `reason` text NOT NULL,
                  `status` varchar(30) DEFAULT 'REQUESTED',
                  `seller_response` text,
                  `created_at` timestamp DEFAULT CURRENT_TIMESTAMP,
                  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                  INDEX `idx_custom_revision_order` (`custom_order_id`),
                  INDEX `idx_custom_revision_customer` (`customer_id`)
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS `commission_posts` (
                  `id` bigint PRIMARY KEY,
                  `customer_id` bigint NOT NULL,
                  `title` varchar(255),
                  `description` text,
                  `budget_min` decimal(18,2) DEFAULT 0,
                  `budget_max` decimal(18,2) DEFAULT 0,
                  `desired_timeline` varchar(120),
                  `reference_images` text,
                  `status` varchar(30),
                  `created_at` timestamp DEFAULT CURRENT_TIMESTAMP
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS `commission_proposals` (
                  `id` bigint PRIMARY KEY,
                  `post_id` bigint NOT NULL,
                  `seller_id` bigint NOT NULL,
                  `shop_id` bigint NOT NULL,
                  `message` text,
                  `proposed_price` decimal(18,2) DEFAULT 0,
                  `lead_time_days` int DEFAULT 0,
                  `sketch_image_url` varchar(500),
                  `status` varchar(30),
                  `created_at` timestamp DEFAULT CURRENT_TIMESTAMP,
                  UNIQUE KEY `uk_commission_seller_post` (`post_id`, `shop_id`)
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS `media_folders` (
                  `id` bigint PRIMARY KEY,
                  `owner_id` bigint NOT NULL,
                  `name` varchar(255),
                  `created_at` timestamp DEFAULT CURRENT_TIMESTAMP
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS `media_images` (
                  `id` bigint PRIMARY KEY,
                  `folder_id` bigint NOT NULL,
                  `owner_id` bigint NOT NULL,
                  `url` varchar(500),
                  `alt_text` varchar(255),
                  `created_at` timestamp DEFAULT CURRENT_TIMESTAMP
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS `reports` (
                  `id` bigint PRIMARY KEY,
                  `reporter_id` bigint NOT NULL,
                  `type` varchar(40),
                  `target_id` bigint,
                  `reason` text,
                  `status` varchar(30),
                  `admin_note` text,
                  `created_at` timestamp DEFAULT CURRENT_TIMESTAMP,
                  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS `platform_settings` (
                  `setting_key` varchar(120) PRIMARY KEY,
                  `setting_value` text,
                  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS `auth_otps` (
                  `id` bigint PRIMARY KEY,
                  `account_id` bigint,
                  `email` varchar(255),
                  `purpose` varchar(40),
                  `otp_code` varchar(12),
                  `consumed` boolean DEFAULT false,
                  `expires_at` timestamp NULL,
                  `created_at` timestamp DEFAULT CURRENT_TIMESTAMP,
                  INDEX `idx_auth_otps_email` (`email`)
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS `refresh_tokens` (
                  `id` bigint PRIMARY KEY,
                  `account_id` bigint NOT NULL,
                  `token` varchar(160) UNIQUE,
                  `revoked` boolean DEFAULT false,
                  `expires_at` timestamp NULL,
                  `created_at` timestamp DEFAULT CURRENT_TIMESTAMP
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS `inventory_logs` (
                  `id` bigint PRIMARY KEY,
                  `product_id` bigint NOT NULL,
                  `change_qty` int NOT NULL,
                  `reason` varchar(120),
                  `note` text,
                  `created_at` timestamp DEFAULT CURRENT_TIMESTAMP,
                  INDEX `idx_inventory_logs_product` (`product_id`)
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS `marketplace_ledger` (
                  `id` bigint PRIMARY KEY,
                  `order_id` bigint,
                  `shop_id` bigint,
                  `entry_type` varchar(50),
                  `amount` decimal(18,2) DEFAULT 0,
                  `note` text,
                  `created_at` timestamp DEFAULT CURRENT_TIMESTAMP
                )
                """);
    }

    private void seedDemoData() {
        jdbcTemplate.update("""
                INSERT INTO `Users` (`id`, `full_name`, `phone`, `address`, `created_at`)
                VALUES (1, 'Nguyen Buyer', '0900000001', 'Quan 1, TP HCM', CURRENT_TIMESTAMP),
                       (2, 'Tran Seller', '0900000002', 'Thu Duc, TP HCM', CURRENT_TIMESTAMP),
                       (3, 'Admin Marketplace', '0900000003', 'TP HCM', CURRENT_TIMESTAMP),
                       (4, 'Mai Paperworks', '0900000004', 'Quan 3, TP HCM', CURRENT_TIMESTAMP),
                       (5, 'An Nhien Ceramics', '0900000005', 'Hoi An, Quang Nam', CURRENT_TIMESTAMP),
                       (6, 'Bamboo & Linen', '0900000006', 'Ba Dinh, Ha Noi', CURRENT_TIMESTAMP),
                       (7, 'Saigon Leathercraft', '0900000007', 'Binh Thanh, TP HCM', CURRENT_TIMESTAMP),
                       (8, 'Da Lat Candle House', '0900000008', 'Da Lat, Lam Dong', CURRENT_TIMESTAMP),
                       (9, 'Hue Lacquer Atelier', '0900000009', 'Hue, Thua Thien Hue', CURRENT_TIMESTAMP),
                       (10, 'Mekong Woven Studio', '0900000010', 'Ninh Kieu, Can Tho', CURRENT_TIMESTAMP)
                ON DUPLICATE KEY UPDATE `full_name` = VALUES(`full_name`), `phone` = VALUES(`phone`), `address` = VALUES(`address`)
                """);
        String buyerHash = org.mindrot.jbcrypt.BCrypt.hashpw("buyer", org.mindrot.jbcrypt.BCrypt.gensalt());
        String sellerHash = org.mindrot.jbcrypt.BCrypt.hashpw("seller", org.mindrot.jbcrypt.BCrypt.gensalt());
        String adminHash = org.mindrot.jbcrypt.BCrypt.hashpw("admin", org.mindrot.jbcrypt.BCrypt.gensalt());

        jdbcTemplate.update("""
                INSERT INTO `Accounts` (`id`, `user_id`, `username`, `password_hash`, `email`, `email_crc`, `status`, `role`)
                VALUES (1, 1, 'buyer', ?, 'buyer@example.com', 0, 1, 'BUYER'),
                       (2, 2, 'seller', ?, 'seller@example.com', 0, 1, 'SELLER'),
                       (3, 3, 'admin', ?, 'admin@example.com', 0, 1, 'ADMIN'),
                       (4, 4, 'seller2', ?, 'seller2@example.com', 0, 1, 'SELLER'),
                       (5, 5, 'seller3', ?, 'seller3@example.com', 0, 1, 'SELLER'),
                       (6, 6, 'seller4', ?, 'seller4@example.com', 0, 1, 'SELLER'),
                       (7, 7, 'seller5', ?, 'seller5@example.com', 0, 1, 'SELLER'),
                       (8, 8, 'seller6', ?, 'seller6@example.com', 0, 1, 'SELLER'),
                       (9, 9, 'seller7', ?, 'seller7@example.com', 0, 1, 'SELLER'),
                       (10, 10, 'seller8', ?, 'seller8@example.com', 0, 1, 'SELLER')
                ON DUPLICATE KEY UPDATE `user_id` = VALUES(`user_id`), `status` = VALUES(`status`), `role` = VALUES(`role`)
                """, buyerHash, sellerHash, adminHash, sellerHash, sellerHash, sellerHash, sellerHash, sellerHash, sellerHash, sellerHash);
        jdbcTemplate.update("""
                INSERT INTO `Shops` (`id`, `owner_id`, `shop_name`, `logo_url`, `description`, `rating`, `created_at`)
                VALUES (1, 2, 'Luna Press', '', 'Thiệp letterpress và quà handmade', 4.8, CURRENT_TIMESTAMP),
                       (2, 2, 'Golden Fold', '', 'Đồ giấy thủ công đặt riêng', 4.7, CURRENT_TIMESTAMP),
                       (3, 2, 'Indigo Studio', '', 'Quà tặng và decor thiết kế riêng', 4.9, CURRENT_TIMESTAMP)
                ON DUPLICATE KEY UPDATE `shop_name` = VALUES(`shop_name`), `description` = VALUES(`description`), `rating` = VALUES(`rating`)
                """);
        jdbcTemplate.update("""
                UPDATE `Shops`
                SET `hero_url` = CASE `id`
                    WHEN 1 THEN 'https://picsum.photos/seed/luna-hero/1200/420'
                    WHEN 2 THEN 'https://picsum.photos/seed/golden-hero/1200/420'
                    ELSE 'https://picsum.photos/seed/indigo-hero/1200/420'
                  END,
                  `about` = CASE `id`
                    WHEN 1 THEN 'Studio ép hoa khô trên giấy cotton, phù hợp thiệp cưới và quà tặng nhỏ.'
                    WHEN 2 THEN 'Workshop giấy thủ công chuyên hộp quà cá nhân hóa và phác thảo mẫu riêng.'
                    ELSE 'Nhóm nghệ nhân vẽ tay decor, quà kỷ niệm và phụ kiện trang trí.'
                  END,
                  `materials` = CASE `id`
                    WHEN 1 THEN 'Giấy cotton, hoa khô, mực letterpress'
                    WHEN 2 THEN 'Giay my thuat, ru bang, son acrylic'
                    ELSE 'Gỗ nhẹ, vải canvas, màu nước'
                  END,
                  `years_experience` = CASE `id` WHEN 1 THEN 5 WHEN 2 THEN 4 ELSE 6 END,
                  `verified_artisan` = true
                WHERE `id` IN (1, 2, 3)
                """);
        jdbcTemplate.update("""
                INSERT INTO `Categories` (`id`, `parent_id`, `name`, `slug`)
                VALUES (1, NULL, 'Thiệp handmade', 'thiep-handmade'),
                       (2, NULL, 'Quà tặng custom', 'qua-tang-custom'),
                       (3, NULL, 'Decor thủ công', 'decor-thu-cong'),
                       (4, NULL, 'Gốm sứ thủ công', 'gom-su-thu-cong'),
                       (5, NULL, 'Túi ví da handmade', 'tui-vi-da-handmade'),
                       (6, NULL, 'Nến thơm và xà phòng', 'nen-thom-xa-phong'),
                       (7, NULL, 'Trang sức handmade', 'trang-suc-handmade'),
                       (8, NULL, 'Đồ đan lát và vải', 'do-dan-lat-va-vai')
                ON DUPLICATE KEY UPDATE `name` = VALUES(`name`), `slug` = VALUES(`slug`)
                """);
        jdbcTemplate.update("""
                INSERT INTO `Products` (`id`, `shop_id`, `cat_id`, `name`, `price`, `description`, `is_custom`, `avg_rating`, `status`)
                VALUES (1, 1, 1, 'Petals & Parchment', 240000, 'Thiệp ép hoa khô và giấy cotton', false, 4.8, 'active'),
                       (2, 2, 2, 'Golden Solstice', 360000, 'Hop qua sinh nhat dat mau rieng', true, 4.7, 'active'),
                       (3, 3, 2, 'Indigo Dreams', 480000, 'Qua ky niem ve tay tong xanh cham', true, 4.9, 'active'),
                       (4, 1, 3, 'Botanical Keepsake', 520000, 'Khung decor thực vật bảo quản', true, 4.6, 'active')
                ON DUPLICATE KEY UPDATE `shop_id` = VALUES(`shop_id`), `cat_id` = VALUES(`cat_id`), `name` = VALUES(`name`),
                    `price` = VALUES(`price`), `description` = VALUES(`description`), `is_custom` = VALUES(`is_custom`),
                    `avg_rating` = VALUES(`avg_rating`), `status` = VALUES(`status`)
                """);
        jdbcTemplate.update("""
                INSERT INTO `ProductImages` (`id`, `product_id`, `url`, `is_main`)
                VALUES (1, 1, 'https://picsum.photos/seed/petals-parchment/640/480', true),
                       (2, 2, 'https://picsum.photos/seed/golden-solstice/640/480', true),
                       (3, 3, 'https://picsum.photos/seed/indigo-dreams/640/480', true),
                       (4, 4, 'https://picsum.photos/seed/botanical-keepsake/640/480', true)
                ON DUPLICATE KEY UPDATE `url` = VALUES(`url`), `is_main` = VALUES(`is_main`)
                """);
        jdbcTemplate.update("""
                INSERT INTO `Storage` (`id`, `product_id`, `warehouse_id`, `quantity`, `reserved_quantity`, `low_stock_alert`, `last_updated`)
                VALUES (1, 1, 1, 24, 0, 5, CURRENT_TIMESTAMP),
                       (2, 2, 1, 12, 0, 4, CURRENT_TIMESTAMP),
                       (3, 3, 1, 10, 0, 4, CURRENT_TIMESTAMP),
                       (4, 4, 1, 8, 0, 3, CURRENT_TIMESTAMP)
                ON DUPLICATE KEY UPDATE `quantity` = VALUES(`quantity`), `low_stock_alert` = VALUES(`low_stock_alert`)
                """);
        seedExpandedHandmadeCatalog();
        jdbcTemplate.update("""
                INSERT INTO `shop_shipping_profiles` (`id`, `shop_id`, `pickup_name`, `phone`, `province`, `district`, `ward`, `address`, `ghn_shop_id`, `ghn_district_id`, `ghn_ward_code`)
                VALUES (1, 1, 'Luna Press Studio', '0900000011', 'TP HCM', 'Quan 1', 'Ben Nghe', '12 Nguyen Hue', 'GHN-LUNA', '1442', '20101'),
                       (2, 2, 'Golden Fold Workshop', '0900000012', 'TP HCM', 'Quan 3', 'Vo Thi Sau', '45 Pasteur', 'GHN-GOLDEN', '1444', '20308'),
                       (3, 3, 'Indigo Studio', '0900000013', 'TP HCM', 'Thu Duc', 'Linh Trung', '9 Xa Lo Ha Noi', 'GHN-INDIGO', '3695', '90737')
                ON DUPLICATE KEY UPDATE `pickup_name` = VALUES(`pickup_name`), `phone` = VALUES(`phone`), `address` = VALUES(`address`)
                """);
        jdbcTemplate.update("""
                INSERT INTO `homepage_banners` (`id`, `title`, `subtitle`, `image_url`, `link_url`, `sort_order`, `active`)
                VALUES (1, 'Quà handmade cá nhân hóa', 'Đặt thiệp, hộp quà và decor từ các shop nghệ nhân trong marketplace.', 'https://picsum.photos/seed/handmade-market-hero/1400/520', '/?category=Qua%20tang%20custom', 1, true)
                ON DUPLICATE KEY UPDATE `title` = VALUES(`title`), `subtitle` = VALUES(`subtitle`), `image_url` = VALUES(`image_url`), `link_url` = VALUES(`link_url`)
                """);
        jdbcTemplate.update("""
                INSERT INTO `product_reviews` (`id`, `product_id`, `shop_id`, `user_id`, `rating`, `comment`, `seller_reply`)
                VALUES (1, 1, 1, 1, 5, 'Thiệp rất đẹp, đóng gói cẩn thận.', 'Cảm ơn bạn đã ủng hộ Luna Press.'),
                       (2, 2, 2, 1, 5, 'Hop qua dung mau minh chon.', NULL),
                       (3, 3, 3, 1, 4, 'Mau ve tay rat co hon.', NULL)
                ON DUPLICATE KEY UPDATE `rating` = VALUES(`rating`), `comment` = VALUES(`comment`)
                """);
        jdbcTemplate.update("""
                INSERT INTO `product_questions` (`id`, `product_id`, `user_id`, `question`, `answer`, `answered_at`)
                VALUES (1, 1, 1, 'Có thể in tên người nhận lên thiệp không?', 'Shop có hỗ trợ in tên và lời chúc ngắn.', CURRENT_TIMESTAMP),
                       (2, 2, 1, 'Thoi gian lam hop custom mat bao lau?', 'Thuong tu 2 den 4 ngay tuy chi tiet.', CURRENT_TIMESTAMP)
                ON DUPLICATE KEY UPDATE `question` = VALUES(`question`), `answer` = VALUES(`answer`)
                """);
        jdbcTemplate.update("""
                INSERT INTO `notifications` (`id`, `user_id`, `type`, `title`, `message`, `dedupe_key`)
                VALUES (1, 1, 'SYSTEM', 'Chào mừng đến TMDT Market', 'Theo dõi shop và lưu wishlist để cá nhân hóa trải nghiệm mua handmade.', 'welcome-buyer')
                ON DUPLICATE KEY UPDATE `title` = VALUES(`title`), `message` = VALUES(`message`)
                """);
        seedMarketplaceModuleDemoData();
    }

    private void seedExpandedHandmadeCatalog() {
        Object[][] shops = {
                {1, 2, "Luna Press", "Thiệp letterpress, ép hoa khô và giấy cotton làm theo yêu cầu", 4.8, "Giấy cotton, hoa khô, mực letterpress", 5, "TP HCM"},
                {2, 2, "Golden Fold", "Hop qua giay my thuat, ru bang va thiep viet tay", 4.7, "Giay my thuat, ru bang, son acrylic", 4, "TP HCM"},
                {3, 2, "Indigo Studio", "Quà tặng vẽ tay và decor tone xanh chàm", 4.9, "Gỗ nhẹ, vải canvas, màu nước", 6, "TP HCM"},
                {4, 4, "Mai Paperworks", "So tay, album anh va thiep cat lop phong cach toi gian", 4.6, "Giay kraft, giay do, chi cotton", 3, "TP HCM"},
                {5, 5, "An Nhien Ceramics", "Gốm sứ nhỏ vẽ men thủ công cho bàn ăn và góc làm việc", 4.9, "Đất sét, men tro, men ngọc", 7, "Hoi An"},
                {6, 6, "Bamboo & Linen", "Đồ vải lanh, khăn trải bàn và giỏ tre đan thủ công", 4.7, "Vải linen, tre, cói, cotton", 5, "Ha Noi"},
                {7, 7, "Saigon Leathercraft", "Ví da, móc khóa và bao sổ tay cắt may thủ công", 4.8, "Da bò, chỉ sáp, phụ kiện đồng", 8, "TP HCM"},
                {8, 8, "Da Lat Candle House", "Nến thơm, sáp thơm và xà phòng thiên nhiên mê hóa mùi hương", 4.6, "Sáp đậu nành, tinh dầu, bơ shea", 4, "Da Lat"},
                {9, 9, "Hue Lacquer Atelier", "Khay son mai va trang suc son mai sac Hue", 4.9, "Son ta, vo trung, go sung", 9, "Hue"},
                {10, 10, "Mekong Woven Studio", "Tui coi, khay luc binh va phu kien dan lat mien Tay", 4.7, "Luc binh, coi, may, cotton", 6, "Can Tho"},
                {11, 2, "Clay & Bloom", "Hoa đất sét, charm đất nung và phụ kiện bàn làm việc", 4.5, "Đất polymer, màu acrylic, resin", 2, "Da Nang"},
                {12, 4, "Silver Fern Jewelry", "Nhan bac, vong da va trang suc dat rieng", 4.8, "Bac 925, da mat trang, ngoc trai", 5, "Ha Noi"}
        };
        for (Object[] shop : shops) {
            jdbcTemplate.update("""
                    INSERT INTO `Shops` (`id`, `owner_id`, `shop_name`, `logo_url`, `description`, `rating`, `created_at`,
                      `hero_url`, `about`, `materials`, `years_experience`, `verified_artisan`)
                    VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, ?, ?, ?, ?, true)
                    ON DUPLICATE KEY UPDATE `owner_id` = VALUES(`owner_id`), `shop_name` = VALUES(`shop_name`),
                      `description` = VALUES(`description`), `rating` = VALUES(`rating`), `hero_url` = VALUES(`hero_url`),
                      `about` = VALUES(`about`), `materials` = VALUES(`materials`),
                      `years_experience` = VALUES(`years_experience`), `verified_artisan` = VALUES(`verified_artisan`)
                    """,
                    shop[0], shop[1], shop[2],
                    "https://source.unsplash.com/240x240/?handmade,shop," + shop[0],
                    shop[3], shop[4],
                    "https://source.unsplash.com/1400x520/?handmade,workshop," + shop[0],
                    "Xưởng " + shop[7] + " tập trung vào sản phẩm làm tay số lượng nhỏ, có thể tùy biến theo người nhận.",
                    shop[5], shop[6]);
        }

        Object[][] shippingProfiles = {
                {4, 4, "Mai Paperworks Studio", "0900000014", "TP HCM", "Quan 3", "Vo Thi Sau", "18 Nguyen Dinh Chieu", "GHN-MAI", "1444", "20308"},
                {5, 5, "An Nhien Ceramics", "0900000015", "Quang Nam", "Hoi An", "Cam Chau", "72 Tran Nhan Tong", "GHN-ANNHIEN", "1527", "500101"},
                {6, 6, "Bamboo & Linen", "0900000016", "Ha Noi", "Ba Dinh", "Doi Can", "25 Doi Can", "GHN-BAMBOO", "1482", "10101"},
                {7, 7, "Saigon Leathercraft", "0900000017", "TP HCM", "Binh Thanh", "Phuong 25", "88 Xo Viet Nghe Tinh", "GHN-LEATHER", "1451", "20701"},
                {8, 8, "Da Lat Candle House", "0900000018", "Lam Dong", "Da Lat", "Phuong 1", "5 Hoa Binh", "GHN-CANDLE", "1560", "670101"},
                {9, 9, "Hue Lacquer Atelier", "0900000019", "Thua Thien Hue", "Hue", "Phu Hoi", "31 Le Loi", "GHN-LACQUER", "1600", "530101"},
                {10, 10, "Mekong Woven Studio", "0900000020", "Can Tho", "Ninh Kieu", "An Cu", "14 Mau Than", "GHN-WOVEN", "1644", "900101"},
                {11, 11, "Clay & Bloom", "0900000021", "Da Nang", "Hai Chau", "Thach Thang", "40 Bach Dang", "GHN-CLAY", "1526", "550101"},
                {12, 12, "Silver Fern Jewelry", "0900000022", "Ha Noi", "Hoan Kiem", "Hang Bac", "21 Hang Bac", "GHN-SILVER", "1482", "100101"}
        };
        for (Object[] profile : shippingProfiles) {
            jdbcTemplate.update("""
                    INSERT INTO `shop_shipping_profiles` (`id`, `shop_id`, `pickup_name`, `phone`, `province`, `district`, `ward`, `address`,
                      `ghn_shop_id`, `ghn_district_id`, `ghn_ward_code`)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    ON DUPLICATE KEY UPDATE `pickup_name` = VALUES(`pickup_name`), `phone` = VALUES(`phone`),
                      `province` = VALUES(`province`), `district` = VALUES(`district`), `ward` = VALUES(`ward`),
                      `address` = VALUES(`address`), `ghn_shop_id` = VALUES(`ghn_shop_id`),
                      `ghn_district_id` = VALUES(`ghn_district_id`), `ghn_ward_code` = VALUES(`ghn_ward_code`)
                    """, profile);
        }

        Object[][] products = {
                {1, 1, 1, "Petals & Parchment", 240000, "Thiệp ép hoa khô trên giấy cotton, hộp lời chúc sinh nhật và cảm ơn.", false, 4.8, "thiep,hoa-kho,letterpress", 24, 5, "pressed-flower-card"},
                {2, 2, 2, "Golden Solstice", 360000, "Hop qua sinh nhat tone vang kem thiep viet tay va ru bang lua.", true, 4.7, "hop-qua,custom,ru-bang", 12, 4, "gift-box"},
                {3, 3, 2, "Indigo Dreams", 480000, "Set qua ky niem ve tay tone xanh cham kem tranh mini canvas.", true, 4.9, "qua-tang,ve-tay,canvas", 10, 4, "hand-painted-gift"},
                {4, 1, 3, "Botanical Keepsake", 520000, "Khung decor thực vật bảo quản, co the gan ten va ngay ky niem.", true, 4.6, "decor,hoa-kho,khung", 8, 3, "botanical-frame"},
                {5, 4, 1, "Kraft Memory Card Set", 180000, "Bo 12 thiep kraft mini kem phong bi va tem niem phong.", false, 4.5, "thiep,kraft,mini-card", 40, 8, "kraft-card"},
                {6, 4, 3, "Layered Paper Garden", 420000, "Tranh giay cat lop 3D chu de vuon nho cho ban lam viec.", true, 4.7, "paper-art,decor,3d", 9, 3, "paper-art"},
                {7, 4, 2, "Personal Photo Album", 590000, "Album anh dan tay bia vai linen, nhan ten bang dong dap noi.", true, 4.8, "album,photo,custom", 15, 4, "handmade-album"},
                {8, 4, 1, "Wedding Vow Card", 260000, "Cap thiep loi the uoc giay cotton, in ten co dau chu re.", true, 4.9, "wedding,card,letterpress", 28, 6, "wedding-card"},
                {9, 5, 4, "Moon Glaze Espresso Cup", 320000, "Ly espresso men tro cham xanh xam, moi chiec co van men rieng.", false, 4.9, "gom,ly,espresso", 32, 6, "ceramic-cup"},
                {10, 5, 4, "Hoi An Dessert Plate", 390000, "Đĩa bánh men vàng đất sét nung nhiệt cao, phù hợp bàn trà.", false, 4.8, "gom,dia,banh", 22, 5, "ceramic-plate"},
                {11, 5, 3, "Tiny Incense Holder", 150000, "De tram gom nho tao hinh nui, di kem hop giay kraft.", false, 4.6, "gom,tram,decor", 45, 8, "incense-holder"},
                {12, 5, 2, "Couple Ceramic Set", 720000, "Set 2 ly gốm khắc chữ cái và hộp quà gói sẵn.", true, 4.9, "gom,qua-cuoi,custom", 14, 4, "ceramic-gift"},
                {13, 6, 8, "Linen Table Runner", 410000, "Khan trai ban vai linen det tho, vien tay mau be tu nhien.", false, 4.7, "linen,table,home", 20, 4, "linen-runner"},
                {14, 6, 8, "Bamboo Picnic Basket", 680000, "Giỏ tre đan có nắp vải, dùng quà picnic hoặc decor bếp.", false, 4.8, "tre,gio,picnic", 11, 3, "bamboo-basket"},
                {15, 6, 8, "Cotton Napkin Duo", 220000, "Cap khan an cotton nhuom mau thuc vat, may vien tay.", false, 4.5, "cotton,khan-an,nhuom-tu-nhien", 36, 8, "cotton-napkin"},
                {16, 6, 2, "Rustic Breakfast Gift", 560000, "Set qua gom khan linen, muong go va thiep loi chuc.", true, 4.7, "qua-tang,linen,rustic", 16, 4, "breakfast-gift"},
                {17, 7, 5, "Classic Card Wallet", 450000, "Vi dung the da bo sap, khau tay chi sap mau nau tram.", false, 4.8, "da,vi,card-wallet", 18, 4, "leather-wallet"},
                {18, 7, 5, "Passport Sleeve Initials", 620000, "Bao passport da that khac chu cai, co ngan ve may bay.", true, 4.9, "da,passport,custom", 14, 3, "passport-sleeve"},
                {19, 7, 5, "Leather Key Fob", 160000, "Moc khoa da cat tay, co the dap ten ngan.", true, 4.6, "da,moc-khoa,qua-tang", 55, 10, "leather-keychain"},
                {20, 7, 3, "Desk Cable Roll", 290000, "Day cuon cap da bo giup sap xep ban lam viec gon gang.", false, 4.5, "da,desk,organizer", 27, 6, "leather-desk"},
                {21, 8, 6, "Pine Morning Candle", 240000, "Nen sap dau nanh mui thong va cam bergamot, ly thuy tinh nau.", false, 4.7, "nen-thom,sap-dau-nanh,pine", 46, 10, "soy-candle"},
                {22, 8, 6, "Lavender Sleep Soap", 150000, "Xa phong lavender bo shea, cat khoi nho cho da nhay cam.", false, 4.6, "xa-phong,lavender,shea", 60, 12, "handmade-soap"},
                {23, 8, 6, "Citrus Wax Sachet", 190000, "Sap thom treo tu do voi vo cam kho va hoa nho.", false, 4.5, "sap-thom,citrus,tu-do", 38, 8, "wax-sachet"},
                {24, 8, 2, "Scented Care Box", 520000, "Hop qua cham soc gom nen, xa phong va thiep viet tay.", true, 4.8, "qua-tang,nen-thom,self-care", 18, 4, "self-care-gift"},
                {25, 9, 3, "Hue Lacquer Tray", 780000, "Khay son mai go sung hoa tiet song Huong, phu bong nhieu lop.", false, 4.9, "son-mai,khay,decor", 9, 2, "lacquer-tray"},
                {26, 9, 7, "Eggshell Pendant", 420000, "Mat day chuyen son mai kham vo trung, moi mat co van rieng.", false, 4.8, "son-mai,trang-suc,mat-day", 21, 5, "lacquer-pendant"},
                {27, 9, 3, "Mini Lacquer Coaster", 260000, "Bo 4 lot ly son mai mau tram, dung tiec tra hoac decor.", false, 4.7, "son-mai,lot-ly,home", 30, 6, "lacquer-coaster"},
                {28, 9, 2, "Royal Hue Gift Set", 950000, "Set khay son mai nho va thiep calligraphy dat rieng.", true, 4.9, "qua-tang,son-mai,premium", 7, 2, "premium-lacquer-gift"},
                {29, 10, 8, "Water Hyacinth Tote", 390000, "Tui luc binh dan tay co quai vai cotton, nhe va thoang.", false, 4.7, "luc-binh,tui,dan-lat", 26, 5, "woven-tote"},
                {30, 10, 8, "Round Woven Tray", 310000, "Khay cói tròn dùng bàn trà, chụp ảnh sản phẩm hoặc decor.", false, 4.6, "coi,khay,decor", 34, 7, "woven-tray"},
                {31, 10, 3, "Mekong Wall Basket", 460000, "Bo 3 dia dan treo tuong mau tu nhien cho phong khach.", false, 4.8, "dan-lat,decor,wall", 13, 3, "wall-basket"},
                {32, 10, 2, "Market Morning Hamper", 640000, "Giỏ quà miền Tây gồm khay cói, khăn cotton và thiệp.", true, 4.7, "qua-tang,hamper,dan-lat", 12, 3, "woven-hamper"},
                {33, 11, 7, "Polymer Daisy Earrings", 210000, "Bong tai dat polymer hinh cuc hoa mi, nhe va chong tham.", false, 4.5, "dat-polymer,bong-tai,hoa", 42, 8, "polymer-earrings"},
                {34, 11, 3, "Clay Desk Buddy", 180000, "Tuong dat nho de ban lam viec, co the chon mau ao.", true, 4.6, "dat-set,decor,desk", 25, 5, "clay-figure"},
                {35, 11, 2, "Pet Miniature Keepsake", 540000, "Tuong thu cung dat polymer theo anh tham khao.", true, 4.9, "custom,pet,miniature", 8, 2, "pet-miniature"},
                {36, 11, 1, "Clay Floral Greeting", 230000, "Thiệp gắn hoa đất mini nổi, hộp quà sinh nhật độc lạ.", true, 4.7, "thiep,hoa-dat,custom", 19, 4, "clay-card"},
                {37, 12, 7, "Silver Fern Ring", 690000, "Nhẫn bạc 925 tạo hình lá dương xỉ, đánh bóng thủ công.", false, 4.8, "bac,nhan,trang-suc", 16, 4, "silver-ring"},
                {38, 12, 7, "Moonstone Bracelet", 520000, "Vong da mat trang phoi charm bac, hop qua kem tui nhung.", true, 4.7, "vong-da,moonstone,bac", 20, 5, "moonstone-bracelet"},
                {39, 12, 7, "Pearl Drop Earrings", 480000, "Bong tai ngoc trai nuoi nuoc ngot, moc bac 925.", false, 4.8, "ngoc-trai,bong-tai,bac", 18, 4, "pearl-earrings"},
                {40, 12, 2, "Initial Jewelry Box", 890000, "Hop trang suc ca nhan hoa gom nhan bac va thiep loi chuc.", true, 4.9, "qua-tang,trang-suc,custom", 9, 2, "jewelry-gift"}
        };
        for (Object[] product : products) {
            String imageUrl = "https://source.unsplash.com/900x700/?handmade," + product[11];
            jdbcTemplate.update("""
                    INSERT INTO `Products` (`id`, `shop_id`, `cat_id`, `name`, `price`, `description`, `is_custom`, `avg_rating`, `status`,
                      `sku`, `tags`, `approval_status`, `main_image_url`, `options_json`, `requires_personalization`, `processing_days`)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'active', ?, ?, 'APPROVED', ?, ?, ?, ?)
                    ON DUPLICATE KEY UPDATE `shop_id` = VALUES(`shop_id`), `cat_id` = VALUES(`cat_id`),
                      `name` = VALUES(`name`), `price` = VALUES(`price`), `description` = VALUES(`description`),
                      `is_custom` = VALUES(`is_custom`), `avg_rating` = VALUES(`avg_rating`), `status` = VALUES(`status`),
                      `sku` = VALUES(`sku`), `tags` = VALUES(`tags`), `approval_status` = VALUES(`approval_status`),
                      `main_image_url` = VALUES(`main_image_url`), `options_json` = VALUES(`options_json`),
                      `requires_personalization` = VALUES(`requires_personalization`), `processing_days` = VALUES(`processing_days`)
                    """,
                    product[0], product[1], product[2], product[3], product[4], product[5], product[6], product[7],
                    "HM-" + String.format("%04d", product[0]), product[8], imageUrl,
                    "{\"colors\":[\"natural\",\"warm\",\"custom\"],\"giftReady\":true}", product[6], 3 + (((Number) product[0]).intValue() % 5));
            jdbcTemplate.update("""
                    INSERT INTO `ProductImages` (`id`, `product_id`, `url`, `is_main`)
                    VALUES (?, ?, ?, true)
                    ON DUPLICATE KEY UPDATE `url` = VALUES(`url`), `is_main` = VALUES(`is_main`)
                    """, product[0], product[0], imageUrl);
            jdbcTemplate.update("""
                    INSERT INTO `Storage` (`id`, `product_id`, `warehouse_id`, `quantity`, `reserved_quantity`, `low_stock_alert`, `last_updated`)
                    VALUES (?, ?, 1, ?, 0, ?, CURRENT_TIMESTAMP)
                    ON DUPLICATE KEY UPDATE `quantity` = VALUES(`quantity`), `low_stock_alert` = VALUES(`low_stock_alert`),
                      `last_updated` = CURRENT_TIMESTAMP
                    """, product[0], product[0], product[9], product[10]);
        }

        Object[][] reviews = {
                {4, 5, 4, 1, 5, "Giay day dep, mau kraft rat am."},
                {5, 9, 5, 1, 5, "Ly gom cam chac tay, men ngoai doi dep hon anh."},
                {6, 14, 6, 1, 5, "Giỏ tre đóng gói kỹ, form rất xinh."},
                {7, 18, 7, 1, 5, "Khac chu cai sac net, duong may deu."},
                {8, 21, 8, 1, 4, "Mui thong de chiu, dot lau."},
                {9, 25, 9, 1, 5, "Khay son mai sang va rat hop lam qua tang."},
                {10, 29, 10, 1, 4, "Tui nhe, quai vai chac."},
                {11, 35, 11, 1, 5, "Tuong thu cung lam giong anh gui."},
                {12, 37, 12, 1, 5, "Nhan bac dep, size vua."}
        };
        for (Object[] review : reviews) {
            jdbcTemplate.update("""
                    INSERT INTO `product_reviews` (`id`, `product_id`, `shop_id`, `user_id`, `rating`, `comment`, `seller_reply`)
                    VALUES (?, ?, ?, ?, ?, ?, NULL)
                    ON DUPLICATE KEY UPDATE `rating` = VALUES(`rating`), `comment` = VALUES(`comment`)
                    """, review);
        }
    }

    private void seedMarketplaceModuleDemoData() {
        jdbcTemplate.update("""
                INSERT INTO `vouchers` (`id`, `code`, `scope`, `shop_id`, `category_id`, `title`, `discount_percent`,
                  `max_discount_amount`, `min_order_amount`, `usage_limit`, `used_count`, `per_user_limit`, `active`, `end_at`)
                VALUES (1, 'HANDMADE10', 'PLATFORM', NULL, NULL, 'Giảm 10% đơn handmade', 10, 50000, 200000, 100, 0, 1, true, DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 30 DAY)),
                       (2, 'LUNA15', 'SELLER', 1, 1, 'Luna Press voucher', 15, 70000, 150000, 50, 0, 1, true, DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 20 DAY))
                ON DUPLICATE KEY UPDATE `title` = VALUES(`title`), `active` = VALUES(`active`)
                """);
        jdbcTemplate.update("""
                INSERT INTO `flash_sales` (`id`, `name`, `description`, `banner_url`, `state`, `discount_percent`,
                  `max_units`, `sold_units`, `reserved_units`, `per_user_limit`, `start_at`, `end_at`)
                VALUES (1, 'Flash handmade cuối tuần', 'Giảm giá demo cho sản phẩm có sẵn', 'https://picsum.photos/seed/flash-handmade/1200/360', 'ACTIVE', 12, 40, 3, 0, 2, CURRENT_TIMESTAMP, DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 7 DAY))
                ON DUPLICATE KEY UPDATE `name` = VALUES(`name`), `state` = VALUES(`state`)
                """);
        jdbcTemplate.update("""
                INSERT IGNORE INTO `flash_sale_products` (`flash_sale_id`, `product_id`)
                VALUES (1, 1), (1, 4)
                """);
        jdbcTemplate.update("""
                INSERT INTO `gift_wrap_tiers` (`id`, `name`, `description`, `price`, `has_card`, `sort_order`, `active`)
                VALUES (1, 'Goi giay kraft', 'Goi qua co ban kem day coi', 25000, false, 1, true),
                       (2, 'Goi qua premium', 'Hop cung, ru bang va thiep viet tay', 65000, true, 2, true)
                ON DUPLICATE KEY UPDATE `price` = VALUES(`price`), `active` = VALUES(`active`)
                """);
        jdbcTemplate.update("""
                INSERT INTO `chat_conversations` (`id`, `customer_id`, `shop_id`, `product_id`, `last_message`, `customer_unread`, `seller_unread`)
                VALUES (1, 1, 1, 1, 'Shop co the in ten len thiep.', 0, 1)
                ON DUPLICATE KEY UPDATE `last_message` = VALUES(`last_message`)
                """);
        jdbcTemplate.update("""
                INSERT INTO `chat_messages` (`id`, `conversation_id`, `sender_id`, `sender_role`, `message_type`, `body`)
                VALUES (1, 1, 1, 'CUSTOMER', 'TEXT', 'Shop co the in ten len thiep khong?'),
                       (2, 1, 2, 'SELLER', 'TEXT', 'Duoc ban, shop co the gui mau truoc khi in.')
                ON DUPLICATE KEY UPDATE `body` = VALUES(`body`)
                """);
        jdbcTemplate.update("""
                INSERT INTO `custom_orders` (`id`, `customer_id`, `shop_id`, `conversation_id`, `title`, `description`, `price`, `status`, `payment_status`)
                VALUES (1, 1, 1, 1, 'Thiệp cưới ép hoa riêng', 'Bộ 20 thiệp có tên cô dâu chú rể', 1200000, 'PENDING_REVIEW', 'UNPAID')
                ON DUPLICATE KEY UPDATE `status` = VALUES(`status`), `price` = VALUES(`price`)
                """);
        jdbcTemplate.update("""
                INSERT INTO `commission_posts` (`id`, `customer_id`, `title`, `description`, `budget_min`, `budget_max`, `desired_timeline`, `reference_images`, `status`)
                VALUES (1, 1, 'Can hop qua sinh nhat tone xanh', 'Muon hop qua handmade co tranh mini va thiep viet tay', 400000, 800000, '2 tuan', '', 'OPEN')
                ON DUPLICATE KEY UPDATE `status` = VALUES(`status`)
                """);
        jdbcTemplate.update("""
                INSERT INTO `commission_proposals` (`id`, `post_id`, `seller_id`, `shop_id`, `message`, `proposed_price`, `lead_time_days`, `status`)
                VALUES (1, 1, 2, 2, 'Golden Fold co the lam hop qua dung tone xanh va gui sketch truoc.', 620000, 7, 'PENDING')
                ON DUPLICATE KEY UPDATE `status` = VALUES(`status`)
                """);
        jdbcTemplate.update("""
                INSERT INTO `media_folders` (`id`, `owner_id`, `name`)
                VALUES (1, 2, 'Sản phẩm Luna Press')
                ON DUPLICATE KEY UPDATE `name` = VALUES(`name`)
                """);
        jdbcTemplate.update("""
                INSERT INTO `media_images` (`id`, `folder_id`, `owner_id`, `url`, `alt_text`)
                VALUES (1, 1, 2, 'https://picsum.photos/seed/luna-media-1/640/480', 'Mau thiep ep hoa')
                ON DUPLICATE KEY UPDATE `url` = VALUES(`url`)
                """);
        jdbcTemplate.update("""
                INSERT INTO `reports` (`id`, `reporter_id`, `type`, `target_id`, `reason`, `status`, `admin_note`)
                VALUES (1, 1, 'PRODUCT', 1, 'Demo report moderation', 'PENDING', NULL)
                ON DUPLICATE KEY UPDATE `status` = VALUES(`status`)
                """);
        jdbcTemplate.update("""
                INSERT INTO `platform_settings` (`setting_key`, `setting_value`)
                VALUES ('platform_name', 'TMDT Market'),
                       ('platform_description', 'Marketplace handmade nhieu seller'),
                       ('commission_bps', '1000'),
                       ('default_shipping_fee', '25000')
                ON DUPLICATE KEY UPDATE `setting_value` = `setting_value`
                """);
        jdbcTemplate.update("""
                INSERT INTO `marketplace_ledger` (`id`, `order_id`, `shop_id`, `entry_type`, `amount`, `note`)
                VALUES (1, NULL, 1, 'PLATFORM_FEE', 0, 'Demo ledger entry cho payment reliability')
                ON DUPLICATE KEY UPDATE `note` = VALUES(`note`)
                """);
    }

    private void addColumnIfMissing(String tableName, String columnName, String definition) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND table_name = ?
                  AND column_name = ?
                """, Integer.class, tableName, columnName);
        if (count != null && count == 0) {
            jdbcTemplate.execute("ALTER TABLE `" + tableName + "` ADD COLUMN " + definition);
        }
    }

    private void addUniqueIndexIfMissing(String tableName, String indexName, String columns) {
        try {
            Integer count = jdbcTemplate.queryForObject("""
                    SELECT COUNT(*)
                    FROM information_schema.statistics
                    WHERE table_schema = DATABASE() AND table_name = ? AND index_name = ?
                    """, Integer.class, tableName, indexName);
            if (count == null || count == 0) {
                jdbcTemplate.execute("ALTER TABLE `" + tableName + "` ADD UNIQUE KEY `" + indexName + "` (" + columns + ")");
            }
        } catch (DataAccessException ex) {
            log.warn("Could not add unique index {} on {}: {}", indexName, tableName, ex.getMessage());
        }
    }
}
