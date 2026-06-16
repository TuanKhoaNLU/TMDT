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
            createBaseTables();
            addMarketplaceColumns();
            createMarketplaceTables();
            seedDemoData();
            log.info("Marketplace MySQL schema is ready.");
        } catch (DataAccessException ex) {
            log.warn("MySQL is not ready; the app started but database APIs need MySQL. {}", ex.getMessage());
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
                  INDEX `idx_product_reviews_product` (`product_id`)
                )
                """);
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
                       (3, 'Admin Marketplace', '0900000003', 'TP HCM', CURRENT_TIMESTAMP)
                ON DUPLICATE KEY UPDATE `full_name` = VALUES(`full_name`), `phone` = VALUES(`phone`), `address` = VALUES(`address`)
                """);
        String buyerHash = org.mindrot.jbcrypt.BCrypt.hashpw("buyer", org.mindrot.jbcrypt.BCrypt.gensalt());
        String sellerHash = org.mindrot.jbcrypt.BCrypt.hashpw("seller", org.mindrot.jbcrypt.BCrypt.gensalt());
        String adminHash = org.mindrot.jbcrypt.BCrypt.hashpw("admin", org.mindrot.jbcrypt.BCrypt.gensalt());

        jdbcTemplate.update("""
                INSERT INTO `Accounts` (`id`, `user_id`, `username`, `password_hash`, `email`, `email_crc`, `status`, `role`)
                VALUES (1, 1, 'buyer', ?, 'buyer@example.com', 0, 1, 'BUYER'),
                       (2, 2, 'seller', ?, 'seller@example.com', 0, 1, 'SELLER'),
                       (3, 3, 'admin', ?, 'admin@example.com', 0, 1, 'ADMIN')
                ON DUPLICATE KEY UPDATE `user_id` = VALUES(`user_id`), `status` = VALUES(`status`), `role` = VALUES(`role`)
                """, buyerHash, sellerHash, adminHash);
        jdbcTemplate.update("""
                INSERT INTO `Shops` (`id`, `owner_id`, `shop_name`, `logo_url`, `description`, `rating`, `created_at`)
                VALUES (1, 2, 'Luna Press', '', 'Thiep letterpress va qua handmade', 4.8, CURRENT_TIMESTAMP),
                       (2, 2, 'Golden Fold', '', 'Do giay thu cong dat rieng', 4.7, CURRENT_TIMESTAMP),
                       (3, 2, 'Indigo Studio', '', 'Qua tang va decor thiet ke rieng', 4.9, CURRENT_TIMESTAMP)
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
                    WHEN 1 THEN 'Studio ep hoa kho tren giay cotton, phu hop thiep cuoi va qua tang nho.'
                    WHEN 2 THEN 'Workshop giay thu cong chuyen hop qua ca nhan hoa va phac thao mau rieng.'
                    ELSE 'Nhom nghe nhan ve tay decor, qua ky niem va phu kien trang tri.'
                  END,
                  `materials` = CASE `id`
                    WHEN 1 THEN 'Giay cotton, hoa kho, muc letterpress'
                    WHEN 2 THEN 'Giay my thuat, ru bang, son acrylic'
                    ELSE 'Go nhe, vai canvas, mau nuoc'
                  END,
                  `years_experience` = CASE `id` WHEN 1 THEN 5 WHEN 2 THEN 4 ELSE 6 END,
                  `verified_artisan` = true
                WHERE `id` IN (1, 2, 3)
                """);
        jdbcTemplate.update("""
                INSERT INTO `Categories` (`id`, `parent_id`, `name`, `slug`)
                VALUES (1, NULL, 'Thiep handmade', 'thiep-handmade'),
                       (2, NULL, 'Qua tang custom', 'qua-tang-custom'),
                       (3, NULL, 'Decor thu cong', 'decor-thu-cong')
                ON DUPLICATE KEY UPDATE `name` = VALUES(`name`), `slug` = VALUES(`slug`)
                """);
        jdbcTemplate.update("""
                INSERT INTO `Products` (`id`, `shop_id`, `cat_id`, `name`, `price`, `description`, `is_custom`, `avg_rating`, `status`)
                VALUES (1, 1, 1, 'Petals & Parchment', 240000, 'Thiep ep hoa kho va giay cotton', false, 4.8, 'active'),
                       (2, 2, 2, 'Golden Solstice', 360000, 'Hop qua sinh nhat dat mau rieng', true, 4.7, 'active'),
                       (3, 3, 2, 'Indigo Dreams', 480000, 'Qua ky niem ve tay tong xanh cham', true, 4.9, 'active'),
                       (4, 1, 3, 'Botanical Keepsake', 520000, 'Khung decor thuc vat bao quan', true, 4.6, 'active')
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
        jdbcTemplate.update("""
                INSERT INTO `shop_shipping_profiles` (`id`, `shop_id`, `pickup_name`, `phone`, `province`, `district`, `ward`, `address`, `ghn_shop_id`, `ghn_district_id`, `ghn_ward_code`)
                VALUES (1, 1, 'Luna Press Studio', '0900000011', 'TP HCM', 'Quan 1', 'Ben Nghe', '12 Nguyen Hue', 'GHN-LUNA', '1442', '20101'),
                       (2, 2, 'Golden Fold Workshop', '0900000012', 'TP HCM', 'Quan 3', 'Vo Thi Sau', '45 Pasteur', 'GHN-GOLDEN', '1444', '20308'),
                       (3, 3, 'Indigo Studio', '0900000013', 'TP HCM', 'Thu Duc', 'Linh Trung', '9 Xa Lo Ha Noi', 'GHN-INDIGO', '3695', '90737')
                ON DUPLICATE KEY UPDATE `pickup_name` = VALUES(`pickup_name`), `phone` = VALUES(`phone`), `address` = VALUES(`address`)
                """);
        jdbcTemplate.update("""
                INSERT INTO `homepage_banners` (`id`, `title`, `subtitle`, `image_url`, `link_url`, `sort_order`, `active`)
                VALUES (1, 'Qua handmade ca nhan hoa', 'Dat thiep, hop qua va decor tu cac shop nghe nhan trong marketplace.', 'https://picsum.photos/seed/handmade-market-hero/1400/520', '/?category=Qua%20tang%20custom', 1, true)
                ON DUPLICATE KEY UPDATE `title` = VALUES(`title`), `subtitle` = VALUES(`subtitle`), `image_url` = VALUES(`image_url`), `link_url` = VALUES(`link_url`)
                """);
        jdbcTemplate.update("""
                INSERT INTO `product_reviews` (`id`, `product_id`, `shop_id`, `user_id`, `rating`, `comment`, `seller_reply`)
                VALUES (1, 1, 1, 1, 5, 'Thiep rat dep, dong goi can than.', 'Cam on ban da ung ho Luna Press.'),
                       (2, 2, 2, 1, 5, 'Hop qua dung mau minh chon.', NULL),
                       (3, 3, 3, 1, 4, 'Mau ve tay rat co hon.', NULL)
                ON DUPLICATE KEY UPDATE `rating` = VALUES(`rating`), `comment` = VALUES(`comment`)
                """);
        jdbcTemplate.update("""
                INSERT INTO `product_questions` (`id`, `product_id`, `user_id`, `question`, `answer`, `answered_at`)
                VALUES (1, 1, 1, 'Co the in ten nguoi nhan len thiep khong?', 'Shop co ho tro in ten va loi chuc ngan.', CURRENT_TIMESTAMP),
                       (2, 2, 1, 'Thoi gian lam hop custom mat bao lau?', 'Thuong tu 2 den 4 ngay tuy chi tiet.', CURRENT_TIMESTAMP)
                ON DUPLICATE KEY UPDATE `question` = VALUES(`question`), `answer` = VALUES(`answer`)
                """);
        jdbcTemplate.update("""
                INSERT INTO `notifications` (`id`, `user_id`, `type`, `title`, `message`, `dedupe_key`)
                VALUES (1, 1, 'SYSTEM', 'Chao mung den TMDT Market', 'Theo doi shop va luu wishlist de ca nhan hoa trai nghiem mua handmade.', 'welcome-buyer')
                ON DUPLICATE KEY UPDATE `title` = VALUES(`title`), `message` = VALUES(`message`)
                """);
        seedMarketplaceModuleDemoData();
    }

    private void seedMarketplaceModuleDemoData() {
        jdbcTemplate.update("""
                INSERT INTO `vouchers` (`id`, `code`, `scope`, `shop_id`, `category_id`, `title`, `discount_percent`,
                  `max_discount_amount`, `min_order_amount`, `usage_limit`, `used_count`, `per_user_limit`, `active`, `end_at`)
                VALUES (1, 'HANDMADE10', 'PLATFORM', NULL, NULL, 'Giam 10% don handmade', 10, 50000, 200000, 100, 0, 1, true, DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 30 DAY)),
                       (2, 'LUNA15', 'SELLER', 1, 1, 'Luna Press voucher', 15, 70000, 150000, 50, 0, 1, true, DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 20 DAY))
                ON DUPLICATE KEY UPDATE `title` = VALUES(`title`), `active` = VALUES(`active`)
                """);
        jdbcTemplate.update("""
                INSERT INTO `flash_sales` (`id`, `name`, `description`, `banner_url`, `state`, `discount_percent`,
                  `max_units`, `sold_units`, `reserved_units`, `per_user_limit`, `start_at`, `end_at`)
                VALUES (1, 'Flash handmade cuoi tuan', 'Giam gia demo cho san pham co san', 'https://picsum.photos/seed/flash-handmade/1200/360', 'ACTIVE', 12, 40, 3, 0, 2, CURRENT_TIMESTAMP, DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 7 DAY))
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
                VALUES (1, 1, 1, 1, 'Thiep cuoi ep hoa rieng', 'Bo 20 thiep co ten co dau chu re', 1200000, 'PENDING_REVIEW', 'UNPAID')
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
                VALUES (1, 2, 'San pham Luna Press')
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
                ON DUPLICATE KEY UPDATE `setting_value` = VALUES(`setting_value`)
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
}
