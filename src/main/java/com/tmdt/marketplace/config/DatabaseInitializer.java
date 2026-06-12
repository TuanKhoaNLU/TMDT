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
        addColumnIfMissing("Orders", "updated_at", "`updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP");

        addColumnIfMissing("OrderItems", "shop_order_id", "`shop_order_id` bigint");
        addColumnIfMissing("OrderItems", "product_name_snapshot", "`product_name_snapshot` varchar(255)");
        addColumnIfMissing("OrderItems", "shop_name_snapshot", "`shop_name_snapshot` varchar(255)");
        addColumnIfMissing("OrderItems", "custom_options_json", "`custom_options_json` text");
        addColumnIfMissing("OrderItems", "note", "`note` text");
        
        addColumnIfMissing("Accounts", "role", "`role` varchar(50) DEFAULT 'BUYER'");
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
