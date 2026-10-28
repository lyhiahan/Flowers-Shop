-- ============================================================================
-- ĐỒ ÁN LẬP TRÌNH WEB: HỆ THỐNG WEBSITE BÁN HOA THEO MÔ HÌNH CHUỖI CỬA HÀNG
-- DỰ ÁN: HOA TƯƠI SÀI GÒN (BLOOM FLOWER CHAIN)
-- CƠ SỞ DỮ LIỆU: MICROSOFT SQL SERVER (FULL SCHEMA & DỮ LIỆU MẪU ĐẦY ĐỦ)
-- Ngày xuất bản: 2026-10-28 17:03:42
-- ============================================================================

IF NOT EXISTS (SELECT * FROM sys.databases WHERE name = 'FlowersShop')
BEGIN
    CREATE DATABASE FlowersShop;
END
GO

USE FlowersShop;
GO

-- 1. XÓA RÀNG BUỘC KHÓA NGOẠI VÀ CÁC BẢNG CŨ (NẾU CÓ)
DECLARE @sqlDropFk NVARCHAR(MAX) = N'';
SELECT @sqlDropFk += N'ALTER TABLE ' + QUOTENAME(s.name) + N'.' + QUOTENAME(t.name) + N' DROP CONSTRAINT ' + QUOTENAME(f.name) + N';' + CHAR(13)
FROM sys.foreign_keys f
INNER JOIN sys.tables t ON f.parent_object_id = t.object_id
INNER JOIN sys.schemas s ON t.schema_id = s.schema_id;
IF LEN(@sqlDropFk) > 0 EXEC sp_executesql @sqlDropFk;
GO

IF OBJECT_ID('dbo.[activity_logs]', 'U') IS NOT NULL DROP TABLE dbo.[activity_logs];
IF OBJECT_ID('dbo.[cart_items]', 'U') IS NOT NULL DROP TABLE dbo.[cart_items];
IF OBJECT_ID('dbo.[carts]', 'U') IS NOT NULL DROP TABLE dbo.[carts];
IF OBJECT_ID('dbo.[categories]', 'U') IS NOT NULL DROP TABLE dbo.[categories];
IF OBJECT_ID('dbo.[chat_access_audits]', 'U') IS NOT NULL DROP TABLE dbo.[chat_access_audits];
IF OBJECT_ID('dbo.[chat_conversations]', 'U') IS NOT NULL DROP TABLE dbo.[chat_conversations];
IF OBJECT_ID('dbo.[chat_messages]', 'U') IS NOT NULL DROP TABLE dbo.[chat_messages];
IF OBJECT_ID('dbo.[chat_product_contexts]', 'U') IS NOT NULL DROP TABLE dbo.[chat_product_contexts];
IF OBJECT_ID('dbo.[commissions]', 'U') IS NOT NULL DROP TABLE dbo.[commissions];
IF OBJECT_ID('dbo.[customer_wallet_entries]', 'U') IS NOT NULL DROP TABLE dbo.[customer_wallet_entries];
IF OBJECT_ID('dbo.[customer_wallets]', 'U') IS NOT NULL DROP TABLE dbo.[customer_wallets];
IF OBJECT_ID('dbo.[deliveries]', 'U') IS NOT NULL DROP TABLE dbo.[deliveries];
IF OBJECT_ID('dbo.[inventory_transfer_requests]', 'U') IS NOT NULL DROP TABLE dbo.[inventory_transfer_requests];
IF OBJECT_ID('dbo.[jwt_refresh_tokens]', 'U') IS NOT NULL DROP TABLE dbo.[jwt_refresh_tokens];
IF OBJECT_ID('dbo.[jwt_sessions]', 'U') IS NOT NULL DROP TABLE dbo.[jwt_sessions];
IF OBJECT_ID('dbo.[momo_payment_attempts]', 'U') IS NOT NULL DROP TABLE dbo.[momo_payment_attempts];
IF OBJECT_ID('dbo.[notifications]', 'U') IS NOT NULL DROP TABLE dbo.[notifications];
IF OBJECT_ID('dbo.[order_details]', 'U') IS NOT NULL DROP TABLE dbo.[order_details];
IF OBJECT_ID('dbo.[orders]', 'U') IS NOT NULL DROP TABLE dbo.[orders];
IF OBJECT_ID('dbo.[otp_verifications]', 'U') IS NOT NULL DROP TABLE dbo.[otp_verifications];
IF OBJECT_ID('dbo.[payments]', 'U') IS NOT NULL DROP TABLE dbo.[payments];
IF OBJECT_ID('dbo.[product_additional_categories]', 'U') IS NOT NULL DROP TABLE dbo.[product_additional_categories];
IF OBJECT_ID('dbo.[product_images]', 'U') IS NOT NULL DROP TABLE dbo.[product_images];
IF OBJECT_ID('dbo.[product_style_values]', 'U') IS NOT NULL DROP TABLE dbo.[product_style_values];
IF OBJECT_ID('dbo.[product_wishlists]', 'U') IS NOT NULL DROP TABLE dbo.[product_wishlists];
IF OBJECT_ID('dbo.[products]', 'U') IS NOT NULL DROP TABLE dbo.[products];
IF OBJECT_ID('dbo.[promotions]', 'U') IS NOT NULL DROP TABLE dbo.[promotions];
IF OBJECT_ID('dbo.[recently_viewed]', 'U') IS NOT NULL DROP TABLE dbo.[recently_viewed];
IF OBJECT_ID('dbo.[reviews]', 'U') IS NOT NULL DROP TABLE dbo.[reviews];
IF OBJECT_ID('dbo.[role_audit_logs]', 'U') IS NOT NULL DROP TABLE dbo.[role_audit_logs];
IF OBJECT_ID('dbo.[role_requests]', 'U') IS NOT NULL DROP TABLE dbo.[role_requests];
IF OBJECT_ID('dbo.[shipping_tariffs]', 'U') IS NOT NULL DROP TABLE dbo.[shipping_tariffs];
IF OBJECT_ID('dbo.[store_inventories]', 'U') IS NOT NULL DROP TABLE dbo.[store_inventories];
IF OBJECT_ID('dbo.[store_levels]', 'U') IS NOT NULL DROP TABLE dbo.[store_levels];
IF OBJECT_ID('dbo.[store_memberships]', 'U') IS NOT NULL DROP TABLE dbo.[store_memberships];
IF OBJECT_ID('dbo.[stores]', 'U') IS NOT NULL DROP TABLE dbo.[stores];
IF OBJECT_ID('dbo.[style_values]', 'U') IS NOT NULL DROP TABLE dbo.[style_values];
IF OBJECT_ID('dbo.[styles]', 'U') IS NOT NULL DROP TABLE dbo.[styles];
IF OBJECT_ID('dbo.[transactions]', 'U') IS NOT NULL DROP TABLE dbo.[transactions];
IF OBJECT_ID('dbo.[user_addresses]', 'U') IS NOT NULL DROP TABLE dbo.[user_addresses];
IF OBJECT_ID('dbo.[user_follow_store]', 'U') IS NOT NULL DROP TABLE dbo.[user_follow_store];
IF OBJECT_ID('dbo.[user_levels]', 'U') IS NOT NULL DROP TABLE dbo.[user_levels];
IF OBJECT_ID('dbo.[user_roles]', 'U') IS NOT NULL DROP TABLE dbo.[user_roles];
IF OBJECT_ID('dbo.[users]', 'U') IS NOT NULL DROP TABLE dbo.[users];
IF OBJECT_ID('dbo.[vouchers]', 'U') IS NOT NULL DROP TABLE dbo.[vouchers];
GO

-- 2. TẠO CẤU TRÚC TOÀN BỘ CÁC BẢNG (TABLE DEFINITIONS)
CREATE TABLE dbo.[activity_logs] (
    [id] BIGINT IDENTITY(1,1) NOT NULL,
    [target_id] BIGINT NULL,
    [timestamp] DATETIME2 NULL,
    [user_id] BIGINT NOT NULL,
    [action] NVARCHAR(50) NOT NULL,
    [target_type] NVARCHAR(50) NULL,
    CONSTRAINT [PK_activity_logs] PRIMARY KEY ([id])
);
GO

CREATE TABLE dbo.[cart_items] (
    [delivery_date] DATE NULL,
    [quantity] INT NOT NULL,
    [rent_end_date] DATE NULL,
    [rent_start_date] DATE NULL,
    [cart_id] BIGINT NOT NULL,
    [id] BIGINT IDENTITY(1,1) NOT NULL,
    [product_id] BIGINT NOT NULL,
    [store_id] BIGINT NOT NULL,
    [order_type] NVARCHAR(10) NOT NULL,
    [greeting_message] NVARCHAR(500) NULL,
    CONSTRAINT [PK_cart_items] PRIMARY KEY ([id])
);
GO

CREATE TABLE dbo.[carts] (
    [id] BIGINT IDENTITY(1,1) NOT NULL,
    [user_id] BIGINT NOT NULL,
    CONSTRAINT [PK_carts] PRIMARY KEY ([id])
);
GO

CREATE TABLE dbo.[categories] (
    [is_deleted] BIT NULL,
    [id] BIGINT IDENTITY(1,1) NOT NULL,
    [parent_id] BIGINT NULL,
    [name] NVARCHAR(100) NOT NULL,
    [description] NVARCHAR(MAX) NULL,
    CONSTRAINT [PK_categories] PRIMARY KEY ([id])
);
GO

CREATE TABLE dbo.[chat_access_audits] (
    [accessed_at] DATETIME2 NOT NULL,
    [admin_id] BIGINT NOT NULL,
    [before_message_id] BIGINT NULL,
    [conversation_id] BIGINT NOT NULL,
    [id] BIGINT IDENTITY(1,1) NOT NULL,
    CONSTRAINT [PK_chat_access_audits] PRIMARY KEY ([id])
);
GO

CREATE TABLE dbo.[chat_conversations] (
    [created_at] DATETIME2 NOT NULL,
    [customer_id] BIGINT NOT NULL,
    [id] BIGINT IDENTITY(1,1) NOT NULL,
    [store_id] BIGINT NULL,
    [updated_at] DATETIME2 NOT NULL,
    [kind] NVARCHAR(16) NOT NULL,
    [conversation_key] NVARCHAR(100) NOT NULL,
    [last_message] NVARCHAR(2000) NULL,
    CONSTRAINT [PK_chat_conversations] PRIMARY KEY ([id])
);
GO

CREATE TABLE dbo.[chat_messages] (
    [conversation_id] BIGINT NOT NULL,
    [created_at] DATETIME2 NOT NULL,
    [id] BIGINT IDENTITY(1,1) NOT NULL,
    [sender_id] BIGINT NOT NULL,
    [client_message_id] NVARCHAR(36) NOT NULL,
    [image_url] NVARCHAR(500) NULL,
    [content] NVARCHAR(2000) NOT NULL,
    [image_public_id] NVARCHAR(255) NULL,
    CONSTRAINT [PK_chat_messages] PRIMARY KEY ([id])
);
GO

CREATE TABLE dbo.[chat_product_contexts] (
    [product_price] DECIMAL(38,2) NOT NULL,
    [id] BIGINT IDENTITY(1,1) NOT NULL,
    [message_id] BIGINT NOT NULL,
    [product_id] BIGINT NOT NULL,
    [product_name] NVARCHAR(200) NOT NULL,
    [product_image_url] NVARCHAR(500) NULL,
    CONSTRAINT [PK_chat_product_contexts] PRIMARY KEY ([id])
);
GO

CREATE TABLE dbo.[commissions] (
    [cost] DECIMAL(38,2) NOT NULL,
    [is_deleted] BIT NULL,
    [created_at] DATETIME2 NULL,
    [id] BIGINT IDENTITY(1,1) NOT NULL,
    [updated_at] DATETIME2 NULL,
    [name] NVARCHAR(32) NOT NULL,
    [description] NVARCHAR(3000) NULL,
    CONSTRAINT [PK_commissions] PRIMARY KEY ([id])
);
GO

CREATE TABLE dbo.[customer_wallet_entries] (
    [amount] DECIMAL(18,2) NOT NULL,
    [balance_after] DECIMAL(18,2) NOT NULL,
    [created_at] DATETIME2 NOT NULL,
    [id] BIGINT IDENTITY(1,1) NOT NULL,
    [order_id] BIGINT NOT NULL,
    [user_id] BIGINT NOT NULL,
    [type] NVARCHAR(12) NOT NULL,
    CONSTRAINT [PK_customer_wallet_entries] PRIMARY KEY ([id])
);
GO

CREATE TABLE dbo.[customer_wallets] (
    [balance] DECIMAL(18,2) NOT NULL,
    [user_id] BIGINT NOT NULL,
    [version] BIGINT NOT NULL,
    CONSTRAINT [PK_customer_wallets] PRIMARY KEY ([user_id])
);
GO

CREATE TABLE dbo.[deliveries] (
    [is_active] BIT NOT NULL,
    [is_deleted] BIT NULL,
    [price] DECIMAL(38,2) NOT NULL,
    [created_at] DATETIME2 NULL,
    [id] BIGINT IDENTITY(1,1) NOT NULL,
    [updated_at] DATETIME2 NULL,
    [phone] NVARCHAR(50) NULL,
    [name] NVARCHAR(100) NOT NULL,
    [vehicle_type] NVARCHAR(100) NULL,
    [description] NVARCHAR(1000) NULL,
    [coverage_area] NVARCHAR(255) NULL,
    CONSTRAINT [PK_deliveries] PRIMARY KEY ([id])
);
GO

CREATE TABLE dbo.[inventory_transfer_requests] (
    [quantity] INT NOT NULL,
    [approved_by] BIGINT NULL,
    [created_at] DATETIME2 NULL,
    [from_store_id] BIGINT NOT NULL,
    [id] BIGINT IDENTITY(1,1) NOT NULL,
    [product_id] BIGINT NOT NULL,
    [requested_by] BIGINT NULL,
    [to_store_id] BIGINT NOT NULL,
    [status] NVARCHAR(20) NOT NULL,
    CONSTRAINT [PK_inventory_transfer_requests] PRIMARY KEY ([id])
);
GO

CREATE TABLE dbo.[jwt_refresh_tokens] (
    [consumed] BIT NOT NULL,
    [expires_at] DATETIME2 NOT NULL,
    [session_id] NVARCHAR(36) NOT NULL,
    [token_hash] NVARCHAR(64) NOT NULL,
    CONSTRAINT [PK_jwt_refresh_tokens] PRIMARY KEY ([token_hash])
);
GO

CREATE TABLE dbo.[jwt_sessions] (
    [revoked] BIT NOT NULL,
    [expires_at] DATETIME2 NOT NULL,
    [user_id] BIGINT NOT NULL,
    [id] NVARCHAR(36) NOT NULL,
    [credential_hash] NVARCHAR(64) NOT NULL,
    CONSTRAINT [PK_jwt_sessions] PRIMARY KEY ([id])
);
GO

CREATE TABLE dbo.[momo_payment_attempts] (
    [amount] DECIMAL(38,2) NOT NULL,
    [completed] BIT NOT NULL,
    [created_at] DATETIME2 NOT NULL,
    [id] BIGINT IDENTITY(1,1) NOT NULL,
    [order_id] BIGINT NOT NULL,
    [environment] NVARCHAR(16) NOT NULL,
    [gateway_order_id] NVARCHAR(100) NOT NULL,
    [request_id] NVARCHAR(100) NOT NULL,
    [transaction_id] NVARCHAR(100) NULL,
    [extra_data] NVARCHAR(1000) NOT NULL,
    [order_info] NVARCHAR(255) NOT NULL,
    CONSTRAINT [PK_momo_payment_attempts] PRIMARY KEY ([id])
);
GO

CREATE TABLE dbo.[notifications] (
    [is_read] BIT NULL,
    [created_at] DATETIME2 NULL,
    [id] BIGINT IDENTITY(1,1) NOT NULL,
    [related_order_id] BIGINT NULL,
    [user_id] BIGINT NOT NULL,
    [type] NVARCHAR(50) NULL,
    [title] NVARCHAR(200) NOT NULL,
    [content] NVARCHAR(MAX) NOT NULL,
    CONSTRAINT [PK_notifications] PRIMARY KEY ([id])
);
GO

CREATE TABLE dbo.[order_details] (
    [actual_return_date] DATE NULL,
    [deposit_fee] DECIMAL(38,2) NULL,
    [quantity] INT NOT NULL,
    [rent_end_date] DATE NULL,
    [rent_start_date] DATE NULL,
    [unit_price] DECIMAL(38,2) NOT NULL,
    [id] BIGINT IDENTITY(1,1) NOT NULL,
    [order_id] BIGINT NOT NULL,
    [product_id] BIGINT NOT NULL,
    [order_type] NVARCHAR(10) NULL,
    CONSTRAINT [PK_order_details] PRIMARY KEY ([id])
);
GO

CREATE TABLE dbo.[orders] (
    [commission_rate_snapshot] DECIMAL(9,4) NULL,
    [delivery_date] DATE NULL,
    [delivery_time] TIME(0) NULL,
    [discount_amount] DECIMAL(38,2) NULL,
    [fine_amount] DECIMAL(38,2) NULL,
    [funds_amount] DECIMAL(18,2) NULL,
    [payment_expired] BIT NULL,
    [shipping_fee] DECIMAL(18,2) NULL,
    [total_amount] DECIMAL(38,2) NULL,
    [total_deposit] DECIMAL(38,2) NULL,
    [created_at] DATETIME2 NULL,
    [delivery_unit_id] BIGINT NULL,
    [id] BIGINT IDENTITY(1,1) NOT NULL,
    [payment_expires_at] DATETIME2 NULL,
    [pickup_store_id] BIGINT NOT NULL,
    [return_store_id] BIGINT NULL,
    [shipper_id] BIGINT NULL,
    [user_id] BIGINT NOT NULL,
    [version] BIGINT NOT NULL,
    [voucher_id] BIGINT NULL,
    [order_type] NVARCHAR(10) NOT NULL,
    [funds_status] NVARCHAR(20) NOT NULL,
    [payment_method] NVARCHAR(20) NULL,
    [payment_status] NVARCHAR(20) NULL,
    [recipient_phone] NVARCHAR(20) NULL,
    [shipping_method] NVARCHAR(20) NULL,
    [status] NVARCHAR(20) NOT NULL,
    [recipient_name] NVARCHAR(100) NULL,
    [cancellation_reason] NVARCHAR(500) NULL,
    [greeting_message] NVARCHAR(500) NULL,
    [recipient_address] NVARCHAR(255) NULL,
    CONSTRAINT [PK_orders] PRIMARY KEY ([id])
);
GO

CREATE TABLE dbo.[otp_verifications] (
    [failed_attempts] INT NOT NULL,
    [is_used] BIT NULL,
    [code] NVARCHAR(6) NOT NULL,
    [created_at] DATETIME2 NULL,
    [expired_at] DATETIME2 NOT NULL,
    [id] BIGINT IDENTITY(1,1) NOT NULL,
    [user_id] BIGINT NOT NULL,
    [type] NVARCHAR(30) NOT NULL,
    CONSTRAINT [PK_otp_verifications] PRIMARY KEY ([id])
);
GO

CREATE TABLE dbo.[payments] (
    [amount] DECIMAL(38,2) NOT NULL,
    [id] BIGINT IDENTITY(1,1) NOT NULL,
    [order_id] BIGINT NOT NULL,
    [paid_at] DATETIME2 NULL,
    [refunded_at] DATETIME2 NULL,
    [momo_environment] NVARCHAR(16) NULL,
    [method] NVARCHAR(20) NOT NULL,
    [status] NVARCHAR(20) NOT NULL,
    [refund_method] NVARCHAR(255) NULL,
    [refund_note] NVARCHAR(255) NULL,
    [transaction_code] NVARCHAR(255) NULL,
    CONSTRAINT [PK_payments] PRIMARY KEY ([id])
);
GO

CREATE TABLE dbo.[product_additional_categories] (
    [category_id] BIGINT NOT NULL,
    [product_id] BIGINT NOT NULL,
    CONSTRAINT [PK_product_additional_categories] PRIMARY KEY ([category_id], [product_id])
);
GO

CREATE TABLE dbo.[product_images] (
    [is_main] BIT NULL,
    [id] BIGINT IDENTITY(1,1) NOT NULL,
    [product_id] BIGINT NOT NULL,
    [cloudinary_public_id] NVARCHAR(255) NULL,
    [image_url] NVARCHAR(255) NOT NULL,
    CONSTRAINT [PK_product_images] PRIMARY KEY ([id])
);
GO

CREATE TABLE dbo.[product_style_values] (
    [product_id] BIGINT NOT NULL,
    [style_value_id] BIGINT NOT NULL,
    CONSTRAINT [PK_product_style_values] PRIMARY KEY ([product_id], [style_value_id])
);
GO

CREATE TABLE dbo.[product_wishlists] (
    [created_at] DATETIME2 NULL,
    [id] BIGINT IDENTITY(1,1) NOT NULL,
    [product_id] BIGINT NOT NULL,
    [user_id] BIGINT NOT NULL,
    CONSTRAINT [PK_product_wishlists] PRIMARY KEY ([id])
);
GO

CREATE TABLE dbo.[products] (
    [deposit_fee] DECIMAL(38,2) NOT NULL,
    [import_price] DECIMAL(38,2) NOT NULL,
    [is_active] BIT NOT NULL,
    [is_deleted] BIT NULL,
    [is_selling] BIT NOT NULL,
    [price_buy] DECIMAL(38,2) NOT NULL,
    [price_rent_per_day] DECIMAL(38,2) NOT NULL,
    [promotional_price] DECIMAL(38,2) NULL,
    [rating] FLOAT NULL,
    [sold] INT NULL,
    [view_count] INT NULL,
    [category_id] BIGINT NOT NULL,
    [created_at] DATETIME2 NULL,
    [id] BIGINT IDENTITY(1,1) NOT NULL,
    [store_id] BIGINT NULL,
    [name] NVARCHAR(200) NOT NULL,
    [image_url] NVARCHAR(500) NULL,
    [description] NVARCHAR(MAX) NULL,
    CONSTRAINT [PK_products] PRIMARY KEY ([id])
);
GO

CREATE TABLE dbo.[promotions] (
    [discount_percentage] INT NOT NULL,
    [end_date] DATE NULL,
    [start_date] DATE NULL,
    [id] BIGINT IDENTITY(1,1) NOT NULL,
    [store_id] BIGINT NULL,
    [apply_type] NVARCHAR(10) NULL,
    [name] NVARCHAR(150) NOT NULL,
    CONSTRAINT [PK_promotions] PRIMARY KEY ([id])
);
GO

CREATE TABLE dbo.[recently_viewed] (
    [id] BIGINT IDENTITY(1,1) NOT NULL,
    [product_id] BIGINT NOT NULL,
    [user_id] BIGINT NOT NULL,
    [viewed_at] DATETIME2 NULL,
    CONSTRAINT [PK_recently_viewed] PRIMARY KEY ([id])
);
GO

CREATE TABLE dbo.[reviews] (
    [rating] INT NOT NULL,
    [created_at] DATETIME2 NULL,
    [id] BIGINT IDENTITY(1,1) NOT NULL,
    [order_id] BIGINT NULL,
    [product_id] BIGINT NOT NULL,
    [store_id] BIGINT NULL,
    [updated_at] DATETIME2 NULL,
    [user_id] BIGINT NOT NULL,
    [image_url] NVARCHAR(500) NULL,
    [content] NVARCHAR(1000) NOT NULL,
    [image_public_id] NVARCHAR(255) NULL,
    CONSTRAINT [PK_reviews] PRIMARY KEY ([id])
);
GO

CREATE TABLE dbo.[role_audit_logs] (
    [created_at] DATETIME2 NOT NULL,
    [id] BIGINT IDENTITY(1,1) NOT NULL,
    [performed_by_id] BIGINT NOT NULL,
    [store_id] BIGINT NULL,
    [user_id] BIGINT NOT NULL,
    [action] NVARCHAR(20) NOT NULL,
    [role] NVARCHAR(30) NOT NULL,
    [reason] NVARCHAR(500) NULL,
    CONSTRAINT [PK_role_audit_logs] PRIMARY KEY ([id])
);
GO

CREATE TABLE dbo.[role_requests] (
    [assigned_store_id] BIGINT NULL,
    [created_at] DATETIME2 NOT NULL,
    [id] BIGINT IDENTITY(1,1) NOT NULL,
    [reviewed_at] DATETIME2 NULL,
    [reviewed_by_id] BIGINT NULL,
    [target_store_id] BIGINT NULL,
    [user_id] BIGINT NOT NULL,
    [phone] NVARCHAR(20) NULL,
    [status] NVARCHAR(30) NOT NULL,
    [target_role] NVARCHAR(30) NOT NULL,
    [vehicle_type] NVARCHAR(100) NULL,
    [rejection_reason] NVARCHAR(500) NULL,
    [shop_address] NVARCHAR(500) NULL,
    [experience_note] NVARCHAR(1000) NULL,
    [contact_info] NVARCHAR(255) NULL,
    [shop_name] NVARCHAR(255) NULL,
    [work_area] NVARCHAR(255) NULL,
    CONSTRAINT [PK_role_requests] PRIMARY KEY ([id])
);
GO

CREATE TABLE dbo.[shipping_tariffs] (
    [base_fee] DECIMAL(18,2) NOT NULL,
    [bulky_surcharge] DECIMAL(18,2) NOT NULL,
    [is_active] BIT NOT NULL,
    [is_deleted] BIT NOT NULL,
    [created_at] DATETIME2 NULL,
    [delivery_id] BIGINT NULL,
    [id] BIGINT IDENTITY(1,1) NOT NULL,
    [store_id] BIGINT NULL,
    [updated_at] DATETIME2 NULL,
    [estimated_time] NVARCHAR(100) NULL,
    [service_name] NVARCHAR(100) NOT NULL,
    [destination_area] NVARCHAR(150) NOT NULL,
    [note] NVARCHAR(500) NULL,
    CONSTRAINT [PK_shipping_tariffs] PRIMARY KEY ([id])
);
GO

CREATE TABLE dbo.[store_inventories] (
    [is_deleted] BIT NOT NULL,
    [is_selling] BIT NOT NULL,
    [quantity] INT NOT NULL,
    [id] BIGINT IDENTITY(1,1) NOT NULL,
    [product_id] BIGINT NOT NULL,
    [store_id] BIGINT NOT NULL,
    [version] BIGINT NOT NULL,
    CONSTRAINT [PK_store_inventories] PRIMARY KEY ([id])
);
GO

CREATE TABLE dbo.[store_levels] (
    [discount] DECIMAL(38,2) NOT NULL,
    [is_deleted] BIT NULL,
    [min_point] INT NOT NULL,
    [created_at] DATETIME2 NULL,
    [id] BIGINT IDENTITY(1,1) NOT NULL,
    [updated_at] DATETIME2 NULL,
    [name] NVARCHAR(32) NOT NULL,
    CONSTRAINT [PK_store_levels] PRIMARY KEY ([id])
);
GO

CREATE TABLE dbo.[store_memberships] (
    [active] BIT NOT NULL,
    [id] BIGINT IDENTITY(1,1) NOT NULL,
    [joined_at] DATETIME2 NULL,
    [store_id] BIGINT NOT NULL,
    [user_id] BIGINT NOT NULL,
    [access_role] NVARCHAR(16) NOT NULL,
    CONSTRAINT [PK_store_memberships] PRIMARY KEY ([id])
);
GO

CREATE TABLE dbo.[stores] (
    [e_wallet] DECIMAL(18,2) NULL,
    [is_open] BIT NULL,
    [latitude] FLOAT NULL,
    [longitude] FLOAT NULL,
    [pending_balance] DECIMAL(18,2) NOT NULL,
    [point] INT NULL,
    [rating] FLOAT NULL,
    [status] BIT NULL,
    [commission_id] BIGINT NULL,
    [created_at] DATETIME2 NULL,
    [id] BIGINT IDENTITY(1,1) NOT NULL,
    [owner_id] BIGINT NULL,
    [store_level_id] BIGINT NULL,
    [version] BIGINT NOT NULL,
    [phone] NVARCHAR(20) NULL,
    [creation_request_key] NVARCHAR(80) NULL,
    [name] NVARCHAR(150) NOT NULL,
    [avatar] NVARCHAR(500) NULL,
    [cover] NVARCHAR(500) NULL,
    [bio] NVARCHAR(1000) NULL,
    [address] NVARCHAR(255) NOT NULL,
    [avatar_public_id] NVARCHAR(255) NULL,
    [cover_public_id] NVARCHAR(255) NULL,
    [featured_image_public_ids] NVARCHAR(MAX) NULL,
    [featured_images] NVARCHAR(MAX) NULL,
    CONSTRAINT [PK_stores] PRIMARY KEY ([id])
);
GO

CREATE TABLE dbo.[style_values] (
    [is_deleted] BIT NULL,
    [created_at] DATETIME2 NULL,
    [id] BIGINT IDENTITY(1,1) NOT NULL,
    [style_id] BIGINT NOT NULL,
    [updated_at] DATETIME2 NULL,
    [name] NVARCHAR(32) NOT NULL,
    CONSTRAINT [PK_style_values] PRIMARY KEY ([id])
);
GO

CREATE TABLE dbo.[styles] (
    [is_deleted] BIT NOT NULL,
    [category_id] BIGINT NULL,
    [created_at] DATETIME2 NULL,
    [id] BIGINT IDENTITY(1,1) NOT NULL,
    [updated_at] DATETIME2 NULL,
    [name] NVARCHAR(100) NOT NULL,
    CONSTRAINT [PK_styles] PRIMARY KEY ([id])
);
GO

CREATE TABLE dbo.[transactions] (
    [amount] DECIMAL(38,2) NOT NULL,
    [earned_points] INT NULL,
    [is_up] BIT NOT NULL,
    [created_at] DATETIME2 NULL,
    [id] BIGINT IDENTITY(1,1) NOT NULL,
    [order_id] BIGINT NULL,
    [store_id] BIGINT NULL,
    [user_id] BIGINT NULL,
    [status] NVARCHAR(20) NULL,
    [wallet_bucket] NVARCHAR(20) NOT NULL,
    [type] NVARCHAR(30) NULL,
    [request_key] NVARCHAR(80) NULL,
    [description] NVARCHAR(500) NULL,
    CONSTRAINT [PK_transactions] PRIMARY KEY ([id])
);
GO

CREATE TABLE dbo.[user_addresses] (
    [is_default] BIT NOT NULL,
    [created_at] DATETIME2 NULL,
    [id] BIGINT IDENTITY(1,1) NOT NULL,
    [user_id] BIGINT NOT NULL,
    [phone] NVARCHAR(20) NOT NULL,
    [recipient_name] NVARCHAR(100) NOT NULL,
    [address_detail] NVARCHAR(255) NOT NULL,
    CONSTRAINT [PK_user_addresses] PRIMARY KEY ([id])
);
GO

CREATE TABLE dbo.[user_follow_store] (
    [created_at] DATETIME2 NULL,
    [id] BIGINT IDENTITY(1,1) NOT NULL,
    [store_id] BIGINT NOT NULL,
    [user_id] BIGINT NOT NULL,
    CONSTRAINT [PK_user_follow_store] PRIMARY KEY ([id])
);
GO

CREATE TABLE dbo.[user_levels] (
    [discount] DECIMAL(38,2) NOT NULL,
    [is_deleted] BIT NULL,
    [min_point] INT NOT NULL,
    [created_at] DATETIME2 NULL,
    [id] BIGINT IDENTITY(1,1) NOT NULL,
    [updated_at] DATETIME2 NULL,
    [name] NVARCHAR(32) NOT NULL,
    CONSTRAINT [PK_user_levels] PRIMARY KEY ([id])
);
GO

CREATE TABLE dbo.[user_roles] (
    [user_id] BIGINT NOT NULL,
    [role] NVARCHAR(30) NOT NULL,
    CONSTRAINT [PK_user_roles] PRIMARY KEY ([user_id], [role])
);
GO

CREATE TABLE dbo.[users] (
    [account_disabled_by_admin] BIT NULL,
    [created_date] DATE NULL,
    [creation_time_known] BIT NULL,
    [e_wallet] DECIMAL(18,2) NULL,
    [is_active] BIT NULL,
    [is_email_active] BIT NOT NULL,
    [point] INT NULL,
    [retired_delivery_account] BIT NULL,
    [created_at] DATETIME2 NULL,
    [id] BIGINT IDENTITY(1,1) NOT NULL,
    [otp_expiry] DATETIME2 NULL,
    [store_id] BIGINT NULL,
    [user_level_id] BIGINT NULL,
    [otp_code] NVARCHAR(10) NULL,
    [phone] NVARCHAR(20) NULL,
    [role] NVARCHAR(20) NOT NULL,
    [username] NVARCHAR(50) NOT NULL,
    [email] NVARCHAR(100) NOT NULL,
    [full_name] NVARCHAR(100) NOT NULL,
    [cover] NVARCHAR(500) NULL,
    [address] NVARCHAR(255) NULL,
    [avatar_public_id] NVARCHAR(255) NULL,
    [avatar_url] NVARCHAR(255) NULL,
    [cover_public_id] NVARCHAR(255) NULL,
    [password] NVARCHAR(255) NOT NULL,
    CONSTRAINT [PK_users] PRIMARY KEY ([id])
);
GO

CREATE TABLE dbo.[vouchers] (
    [discount_value] DECIMAL(38,2) NOT NULL,
    [end_date] DATE NOT NULL,
    [is_active] BIT NOT NULL,
    [max_discount_amount] DECIMAL(38,2) NULL,
    [min_order_value] DECIMAL(38,2) NULL,
    [quantity] INT NOT NULL,
    [start_date] DATE NOT NULL,
    [used_count] INT NOT NULL,
    [created_at] DATETIME2 NULL,
    [id] BIGINT IDENTITY(1,1) NOT NULL,
    [store_id] BIGINT NULL,
    [discount_type] NVARCHAR(20) NOT NULL,
    [code] NVARCHAR(50) NOT NULL,
    [name] NVARCHAR(150) NOT NULL,
    CONSTRAINT [PK_vouchers] PRIMARY KEY ([id])
);
GO

GO

-- 3. NẠP DỮ LIỆU MẪU (SEED DATA)
EXEC sp_MSforeachtable 'ALTER TABLE ? NOCHECK CONSTRAINT ALL';
GO

-- Dữ liệu bảng: cart_items
SET IDENTITY_INSERT dbo.[cart_items] ON;
INSERT INTO dbo.[cart_items] ([delivery_date], [quantity], [rent_end_date], [rent_start_date], [cart_id], [id], [product_id], [store_id], [order_type], [greeting_message]) VALUES ('2026-10-04', 2, NULL, NULL, 2, 4, 1, 1, N'BUY', NULL);
INSERT INTO dbo.[cart_items] ([delivery_date], [quantity], [rent_end_date], [rent_start_date], [cart_id], [id], [product_id], [store_id], [order_type], [greeting_message]) VALUES ('2026-10-07', 1, NULL, NULL, 19, 18, 37, 3, N'BUY', NULL);
INSERT INTO dbo.[cart_items] ([delivery_date], [quantity], [rent_end_date], [rent_start_date], [cart_id], [id], [product_id], [store_id], [order_type], [greeting_message]) VALUES ('2026-10-13', 1, NULL, NULL, 21, 26, 22, 2, N'BUY', NULL);
INSERT INTO dbo.[cart_items] ([delivery_date], [quantity], [rent_end_date], [rent_start_date], [cart_id], [id], [product_id], [store_id], [order_type], [greeting_message]) VALUES ('2026-10-22', 2, NULL, NULL, 22, 34, 21, 2, N'BUY', NULL);
INSERT INTO dbo.[cart_items] ([delivery_date], [quantity], [rent_end_date], [rent_start_date], [cart_id], [id], [product_id], [store_id], [order_type], [greeting_message]) VALUES ('2026-10-23', 2, NULL, NULL, 22, 36, 38, 3, N'BUY', NULL);
INSERT INTO dbo.[cart_items] ([delivery_date], [quantity], [rent_end_date], [rent_start_date], [cart_id], [id], [product_id], [store_id], [order_type], [greeting_message]) VALUES ('2026-10-23', 1, NULL, NULL, 1, 39, 15, 1, N'BUY', NULL);
SET IDENTITY_INSERT dbo.[cart_items] OFF;
GO

-- Dữ liệu bảng: carts
SET IDENTITY_INSERT dbo.[carts] ON;
INSERT INTO dbo.[carts] ([id], [user_id]) VALUES (1, 5);
INSERT INTO dbo.[carts] ([id], [user_id]) VALUES (2, 1);
INSERT INTO dbo.[carts] ([id], [user_id]) VALUES (3, 2);
INSERT INTO dbo.[carts] ([id], [user_id]) VALUES (18, 21);
INSERT INTO dbo.[carts] ([id], [user_id]) VALUES (19, 35);
INSERT INTO dbo.[carts] ([id], [user_id]) VALUES (20, 36);
INSERT INTO dbo.[carts] ([id], [user_id]) VALUES (21, 6);
INSERT INTO dbo.[carts] ([id], [user_id]) VALUES (22, 37);
INSERT INTO dbo.[carts] ([id], [user_id]) VALUES (23, 40);
INSERT INTO dbo.[carts] ([id], [user_id]) VALUES (24, 42);
SET IDENTITY_INSERT dbo.[carts] OFF;
GO

-- Dữ liệu bảng: categories
SET IDENTITY_INSERT dbo.[categories] ON;
INSERT INTO dbo.[categories] ([is_deleted], [id], [parent_id], [name], [description]) VALUES (1, 1, NULL, N'Hoa Bó Tươi Nghệ Thuật', N'Các mẫu bó hoa tươi thiết kế nghệ thuật cao cấp, hoa hồng, tulip, hướng dương...');
INSERT INTO dbo.[categories] ([is_deleted], [id], [parent_id], [name], [description]) VALUES (1, 2, NULL, N'Giỏ & Hộp Hoa Cao Cấp', N'Giỏ hoa khai trương, hộp hoa tình yêu sang trọng thích hợp làm quà tặng.');
INSERT INTO dbo.[categories] ([is_deleted], [id], [parent_id], [name], [description]) VALUES (1, 6, NULL, N'Phụ Kiện Hoa & Khung Sự Kiện', N'Dịch vụ cho thuê cổng hoa, khung vòm, chân kệ sắt mỹ thuật phục vụ sự kiện cưới hỏi.');
INSERT INTO dbo.[categories] ([is_deleted], [id], [parent_id], [name], [description]) VALUES (0, 7, NULL, N'Hoa Sinh Nhật', N'Các mẫu bó hoa, lẵng hoa chúc mừng sinh nhật ngọt ngào, ý nghĩa');
INSERT INTO dbo.[categories] ([is_deleted], [id], [parent_id], [name], [description]) VALUES (0, 8, NULL, N'Hoa Khai Trương', N'Kệ hoa, lẵng hoa khai trương sang trọng phát tài phát lộc');
INSERT INTO dbo.[categories] ([is_deleted], [id], [parent_id], [name], [description]) VALUES (1, 9, NULL, N'Hoa Tình Yêu & Valentine', N'Hoa hồng đỏ Ecuador, hoa baby trắng lãng mạn tỏ tình');
INSERT INTO dbo.[categories] ([is_deleted], [id], [parent_id], [name], [description]) VALUES (1, 10, NULL, N'Lan Hồ Điệp Sang Trọng', N'Chậu hoa lan hồ điệp cao cấp trưng Tết, biếu sếp, mừng tân gia');
INSERT INTO dbo.[categories] ([is_deleted], [id], [parent_id], [name], [description]) VALUES (1, 11, NULL, N'Hoa Cưới Cầm Tay Cô Dâu', N'Bó hoa cưới tinh tế tôn vinh vẻ đẹp cô dâu trong ngày trọng đại');
INSERT INTO dbo.[categories] ([is_deleted], [id], [parent_id], [name], [description]) VALUES (1, 12, NULL, N'Hoa Chúc Mừng & Tốt Nghiệp', N'Thiết kế tươi sáng, gửi gắm lời chúc may mắn thành công');
INSERT INTO dbo.[categories] ([is_deleted], [id], [parent_id], [name], [description]) VALUES (1, 13, NULL, N'Hoa Chia Buồn & Kính Viếng', N'Kệ hoa trang trọng, thành kính phân ưu cùng gia quyến');
INSERT INTO dbo.[categories] ([is_deleted], [id], [parent_id], [name], [description]) VALUES (1, 14, NULL, N'Bình Hoa Nghệ Thuật Để Bàn', N'Bình gốm, bình thủy tinh cắm hoa tươi trang trí phòng khách và văn phòng');
INSERT INTO dbo.[categories] ([is_deleted], [id], [parent_id], [name], [description]) VALUES (1, 15, NULL, N'Hộp Hoa & Giỏ Hoa Quà Tặng', N'Hộp hoa mica, hộp tròn cao cấp và giỏ hoa tươi thiết kế độc đáo');
INSERT INTO dbo.[categories] ([is_deleted], [id], [parent_id], [name], [description]) VALUES (1, 16, 7, N'Bó Hoa Sinh Nhật', N'Các mẫu bó hoa tươi rực rỡ chúc mừng sinh nhật');
INSERT INTO dbo.[categories] ([is_deleted], [id], [parent_id], [name], [description]) VALUES (1, 20, 8, N'Kệ Hoa Khai Trương', N'Kệ hoa chúc mừng khai trương hồng phát sang trọng');
INSERT INTO dbo.[categories] ([is_deleted], [id], [parent_id], [name], [description]) VALUES (1, 22, 10, N'Chậu Lan Hồ Điệp Trắng', N'Chậu lan hồ điệp trắng đại diện cho sự thanh cao, sang trọng');
INSERT INTO dbo.[categories] ([is_deleted], [id], [parent_id], [name], [description]) VALUES (0, 24, NULL, N'Hoa Tình Yêu', N'Hoa dành cho tình yêu, kỷ niệm và Valentine.');
INSERT INTO dbo.[categories] ([is_deleted], [id], [parent_id], [name], [description]) VALUES (0, 25, NULL, N'Hoa Cưới', N'Hoa dành cho lễ cưới và cô dâu.');
INSERT INTO dbo.[categories] ([is_deleted], [id], [parent_id], [name], [description]) VALUES (0, 26, NULL, N'Hoa Tốt Nghiệp', N'Hoa chúc mừng tốt nghiệp.');
INSERT INTO dbo.[categories] ([is_deleted], [id], [parent_id], [name], [description]) VALUES (0, 27, NULL, N'Hoa Chúc Mừng', N'Hoa chúc mừng thành công và các dịp đặc biệt.');
INSERT INTO dbo.[categories] ([is_deleted], [id], [parent_id], [name], [description]) VALUES (0, 28, NULL, N'Hoa Chia Buồn', N'Hoa chia buồn và kính viếng.');
INSERT INTO dbo.[categories] ([is_deleted], [id], [parent_id], [name], [description]) VALUES (0, 29, NULL, N'Hoa Tân Gia', N'Hoa mừng nhà mới.');
INSERT INTO dbo.[categories] ([is_deleted], [id], [parent_id], [name], [description]) VALUES (0, 30, NULL, N'Hoa Trang Trí', N'Hoa và phụ kiện trang trí không gian, sự kiện.');
INSERT INTO dbo.[categories] ([is_deleted], [id], [parent_id], [name], [description]) VALUES (0, 31, NULL, N'Hoa Quà Tặng', N'Hoa làm quà tặng; chọn thêm dịp phù hợp khi chỉnh sửa sản phẩm.');
SET IDENTITY_INSERT dbo.[categories] OFF;
GO

-- Dữ liệu bảng: chat_access_audits
SET IDENTITY_INSERT dbo.[chat_access_audits] ON;
INSERT INTO dbo.[chat_access_audits] ([accessed_at], [admin_id], [before_message_id], [conversation_id], [id]) VALUES ('2026-10-13 17:45:40.415165', 1, NULL, 1, 1);
INSERT INTO dbo.[chat_access_audits] ([accessed_at], [admin_id], [before_message_id], [conversation_id], [id]) VALUES ('2026-10-13 17:45:42.655206', 1, NULL, 1, 2);
INSERT INTO dbo.[chat_access_audits] ([accessed_at], [admin_id], [before_message_id], [conversation_id], [id]) VALUES ('2026-10-13 17:51:07.804703', 1, NULL, 1, 3);
INSERT INTO dbo.[chat_access_audits] ([accessed_at], [admin_id], [before_message_id], [conversation_id], [id]) VALUES ('2026-10-13 17:55:00.766837', 1, NULL, 1, 4);
INSERT INTO dbo.[chat_access_audits] ([accessed_at], [admin_id], [before_message_id], [conversation_id], [id]) VALUES ('2026-10-13 18:13:06.692906', 1, NULL, 1, 5);
INSERT INTO dbo.[chat_access_audits] ([accessed_at], [admin_id], [before_message_id], [conversation_id], [id]) VALUES ('2026-10-24 16:19:20.179179', 1, NULL, 7, 6);
SET IDENTITY_INSERT dbo.[chat_access_audits] OFF;
GO

-- Dữ liệu bảng: chat_conversations
SET IDENTITY_INSERT dbo.[chat_conversations] ON;
INSERT INTO dbo.[chat_conversations] ([created_at], [customer_id], [id], [store_id], [updated_at], [kind], [conversation_key], [last_message]) VALUES ('2026-10-13 15:47:40.746646', 36, 1, 1, '2026-10-13 16:25:14.476457', N'STORE', N'STORE:36:1', N'hi');
INSERT INTO dbo.[chat_conversations] ([created_at], [customer_id], [id], [store_id], [updated_at], [kind], [conversation_key], [last_message]) VALUES ('2026-10-13 15:48:10.106436', 2, 2, NULL, '2026-10-13 15:48:10.106436', N'ADMIN', N'ADMIN:2', NULL);
INSERT INTO dbo.[chat_conversations] ([created_at], [customer_id], [id], [store_id], [updated_at], [kind], [conversation_key], [last_message]) VALUES ('2026-10-13 17:11:13.290086', 36, 3, NULL, '2026-10-13 17:45:22.641341', N'ADMIN', N'ADMIN:36', N'hi');
INSERT INTO dbo.[chat_conversations] ([created_at], [customer_id], [id], [store_id], [updated_at], [kind], [conversation_key], [last_message]) VALUES ('2026-10-14 20:06:01.542351', 5, 4, 1, '2026-10-24 17:00:59.561558', N'STORE', N'STORE:5:1', N'dạ anh tham khảo giá tại web nha, với mỗi tài khoản sẽ được thêm mã giảm giá');
INSERT INTO dbo.[chat_conversations] ([created_at], [customer_id], [id], [store_id], [updated_at], [kind], [conversation_key], [last_message]) VALUES ('2026-10-22 16:53:34.699624', 37, 5, 2, '2026-10-24 15:42:25.001957', N'STORE', N'STORE:37:2', N'hi');
INSERT INTO dbo.[chat_conversations] ([created_at], [customer_id], [id], [store_id], [updated_at], [kind], [conversation_key], [last_message]) VALUES ('2026-10-24 16:16:21.824233', 5, 6, NULL, '2026-10-24 16:16:21.824233', N'ADMIN', N'ADMIN:5', NULL);
INSERT INTO dbo.[chat_conversations] ([created_at], [customer_id], [id], [store_id], [updated_at], [kind], [conversation_key], [last_message]) VALUES ('2026-10-24 16:18:32.334232', 5, 7, 2, '2026-10-24 16:20:05.777073', N'STORE', N'STORE:5:2', N'dạ mầu này bên em còn ạ');
INSERT INTO dbo.[chat_conversations] ([created_at], [customer_id], [id], [store_id], [updated_at], [kind], [conversation_key], [last_message]) VALUES ('2026-10-28 14:35:19.129689', 2, 8, 1, '2026-10-28 14:35:19.129689', N'BRANCH_SUPPORT', N'BRANCH_SUPPORT:1', NULL);
SET IDENTITY_INSERT dbo.[chat_conversations] OFF;
GO

-- Dữ liệu bảng: chat_messages
SET IDENTITY_INSERT dbo.[chat_messages] ON;
INSERT INTO dbo.[chat_messages] ([conversation_id], [created_at], [id], [sender_id], [client_message_id], [image_url], [content], [image_public_id]) VALUES (1, '2026-10-13 15:47:46.826499', 1, 36, N'34a79694-d5f0-4496-89c1-6c78317c6204', NULL, N'Dạ chào shop ạ', NULL);
INSERT INTO dbo.[chat_messages] ([conversation_id], [created_at], [id], [sender_id], [client_message_id], [image_url], [content], [image_public_id]) VALUES (1, '2026-10-13 16:24:57.947982', 2, 2, N'b9276b48-55f1-4dea-87a7-f3545b4569d9', NULL, N'shop hỗ trợ gì được bạn ạ', NULL);
INSERT INTO dbo.[chat_messages] ([conversation_id], [created_at], [id], [sender_id], [client_message_id], [image_url], [content], [image_public_id]) VALUES (1, '2026-10-13 16:25:14.476457', 3, 36, N'b9d5d0be-e6d4-4c19-b6ba-00c3cb25b8d4', NULL, N'hi', NULL);
INSERT INTO dbo.[chat_messages] ([conversation_id], [created_at], [id], [sender_id], [client_message_id], [image_url], [content], [image_public_id]) VALUES (3, '2026-10-13 17:45:22.641341', 4, 36, N'7a75d8f0-e4e7-4216-adef-2e15b07a4c9e', NULL, N'hi', NULL);
INSERT INTO dbo.[chat_messages] ([conversation_id], [created_at], [id], [sender_id], [client_message_id], [image_url], [content], [image_public_id]) VALUES (4, '2026-10-14 20:07:15.880711', 5, 5, N'943eaa8d-6dd5-41d3-bc74-792e4485e0b0', NULL, N'Tôi muốn hỏi về mẫu hoa này.', NULL);
INSERT INTO dbo.[chat_messages] ([conversation_id], [created_at], [id], [sender_id], [client_message_id], [image_url], [content], [image_public_id]) VALUES (4, '2026-10-14 20:08:41.354104', 6, 2, N'fe7eaf81-615e-44b7-bae9-3f649d71ef55', N'https://res.cloudinary.com/rbw3u9td/image/upload/v1791983324/flower_shop/chat/4/jv5c4qun9ras6hofspzp.jpg', N'Dạ mẫu này shop còn ạ, ngoài ra còn mẫu mix nhiều màu', N'flower_shop/chat/4/jv5c4qun9ras6hofspzp');
INSERT INTO dbo.[chat_messages] ([conversation_id], [created_at], [id], [sender_id], [client_message_id], [image_url], [content], [image_public_id]) VALUES (5, '2026-10-24 15:42:25.001957', 7, 30, N'd175e9d9-0ea2-4159-96d9-fd7079203c2c', NULL, N'hi', NULL);
INSERT INTO dbo.[chat_messages] ([conversation_id], [created_at], [id], [sender_id], [client_message_id], [image_url], [content], [image_public_id]) VALUES (7, '2026-10-24 16:18:42.884318', 8, 5, N'27dd170d-d12f-4c43-85dc-8f8eb61f766c', NULL, N'Tôi muốn hỏi về mẫu hoa này.', NULL);
INSERT INTO dbo.[chat_messages] ([conversation_id], [created_at], [id], [sender_id], [client_message_id], [image_url], [content], [image_public_id]) VALUES (7, '2026-10-24 16:20:05.777073', 9, 6, N'5c60fcf0-0e9a-4fe4-95ab-2755be7b3e39', NULL, N'dạ mầu này bên em còn ạ', NULL);
INSERT INTO dbo.[chat_messages] ([conversation_id], [created_at], [id], [sender_id], [client_message_id], [image_url], [content], [image_public_id]) VALUES (4, '2026-10-24 16:59:51.518525', 10, 5, N'389325ae-2f38-4ed3-b69e-8a085a1b5fa0', NULL, N'giá sao vậy', NULL);
INSERT INTO dbo.[chat_messages] ([conversation_id], [created_at], [id], [sender_id], [client_message_id], [image_url], [content], [image_public_id]) VALUES (4, '2026-10-24 17:00:59.561558', 11, 2, N'0c78c828-e627-4962-8368-ed2843e7b897', NULL, N'dạ anh tham khảo giá tại web nha, với mỗi tài khoản sẽ được thêm mã giảm giá', NULL);
SET IDENTITY_INSERT dbo.[chat_messages] OFF;
GO

-- Dữ liệu bảng: chat_product_contexts
SET IDENTITY_INSERT dbo.[chat_product_contexts] ON;
INSERT INTO dbo.[chat_product_contexts] ([product_price], [id], [message_id], [product_id], [product_name], [product_image_url]) VALUES (120000.00, 1, 5, 39, N'Hoa Cẩm Tú Cầu', N'https://res.cloudinary.com/rbw3u9td/image/upload/v1791983039/rental_shop_products/u6ndxlyceuogjpcmke91.webp');
INSERT INTO dbo.[chat_product_contexts] ([product_price], [id], [message_id], [product_id], [product_name], [product_image_url]) VALUES (650000.00, 2, 8, 21, N'Rung Động Đầu Tiên', N'https://res.cloudinary.com/rbw3u9td/image/upload/v1792385304/rental_shop_products/jqeoylnxs9amupcy3fpl.jpg');
SET IDENTITY_INSERT dbo.[chat_product_contexts] OFF;
GO

-- Dữ liệu bảng: commissions
SET IDENTITY_INSERT dbo.[commissions] ON;
INSERT INTO dbo.[commissions] ([cost], [is_deleted], [created_at], [id], [updated_at], [name], [description]) VALUES (10.00, 0, '2026-10-01 09:09:08.237', 1, '2026-10-01 09:09:08.237', N'Gói cơ bản (Standard)', N'Tỷ lệ chiết khấu sàn 10% áp dụng cho chi nhánh mới gia nhập');
INSERT INTO dbo.[commissions] ([cost], [is_deleted], [created_at], [id], [updated_at], [name], [description]) VALUES (8.00, 0, '2026-10-01 09:09:08.247', 2, '2026-10-01 09:09:08.247', N'Gói đối tác thân thiết', N'Tỷ lệ chiết khấu ưu đãi 8% cho cửa hàng có trên 100 đơn/tháng');
INSERT INTO dbo.[commissions] ([cost], [is_deleted], [created_at], [id], [updated_at], [name], [description]) VALUES (5.00, 0, '2026-10-01 09:09:08.247', 3, '2026-10-01 09:09:08.247', N'Gói đối tác chiến lược', N'Tỷ lệ chiết khấu đặc biệt 5% cho chuỗi phân phối hoa tươi lớn');
SET IDENTITY_INSERT dbo.[commissions] OFF;
GO

-- Dữ liệu bảng: customer_wallet_entries
SET IDENTITY_INSERT dbo.[customer_wallet_entries] ON;
INSERT INTO dbo.[customer_wallet_entries] ([amount], [balance_after], [created_at], [id], [order_id], [user_id], [type]) VALUES (675000.00, 675000.00, '2026-10-24 17:22:37.590262', 1, 36, 5, N'REFUND');
INSERT INTO dbo.[customer_wallet_entries] ([amount], [balance_after], [created_at], [id], [order_id], [user_id], [type]) VALUES (675000.00, 0.00, '2026-10-24 18:51:44.309451', 2, 37, 5, N'PAYMENT');
INSERT INTO dbo.[customer_wallet_entries] ([amount], [balance_after], [created_at], [id], [order_id], [user_id], [type]) VALUES (675000.00, 675000.00, '2026-10-24 18:52:25.142641', 3, 37, 5, N'REFUND');
INSERT INTO dbo.[customer_wallet_entries] ([amount], [balance_after], [created_at], [id], [order_id], [user_id], [type]) VALUES (915000.00, 1590000.00, '2026-10-25 23:51:28.30304', 4, 40, 5, N'REFUND');
INSERT INTO dbo.[customer_wallet_entries] ([amount], [balance_after], [created_at], [id], [order_id], [user_id], [type]) VALUES (715000.00, 2305000.00, '2026-10-26 00:07:04.933975', 5, 41, 5, N'REFUND');
INSERT INTO dbo.[customer_wallet_entries] ([amount], [balance_after], [created_at], [id], [order_id], [user_id], [type]) VALUES (915000.00, 1390000.00, '2026-10-26 21:34:35.669229', 6, 43, 5, N'PAYMENT');
SET IDENTITY_INSERT dbo.[customer_wallet_entries] OFF;
GO

-- Dữ liệu bảng: customer_wallets
INSERT INTO dbo.[customer_wallets] ([balance], [user_id], [version]) VALUES (1390000.00, 5, 6);
GO

-- Dữ liệu bảng: deliveries
SET IDENTITY_INSERT dbo.[deliveries] ON;
INSERT INTO dbo.[deliveries] ([is_active], [is_deleted], [price], [created_at], [id], [updated_at], [phone], [name], [vehicle_type], [description], [coverage_area]) VALUES (1, 0, 0.00, '2026-09-22 20:36:14.63', 1, '2026-09-22 20:36:14.63', N'028 7108 8810', N'Giao hàng nội bộ', N'Xe máy chuyên dụng giữ ẩm', N'Đội ngũ giao hoa nội bộ tiệm, trang bị thùng giữ ẩm chuyên dụng và che chắn hoa cẩn thận.', N'Bán kính các chi nhánh toàn hệ thống');
INSERT INTO dbo.[deliveries] ([is_active], [is_deleted], [price], [created_at], [id], [updated_at], [phone], [name], [vehicle_type], [description], [coverage_area]) VALUES (1, 0, 0.00, '2026-09-22 20:36:14.727', 2, '2026-09-22 20:36:14.727', N'1900 633 677', N'GrabExpress', N'Xe máy lồng kính bảo hộ', N'Đối tác GrabExpress chuyên giao hàng hỏa tốc xe máy có lồng bảo hộ cánh hoa.', N'Nội thành TP.HCM và các thành phố chi nhánh');
INSERT INTO dbo.[deliveries] ([is_active], [is_deleted], [price], [created_at], [id], [updated_at], [phone], [name], [vehicle_type], [description], [coverage_area]) VALUES (1, 0, 0.00, '2026-09-22 20:36:14.74', 3, '2026-09-22 20:36:14.74', N'1900 545 411', N'Ahamove', N'Tài xế chuyên giao hoa tươi', N'Đối tác Ahamove giao siêu tốc, chuyên đơn hàng hoa tươi và bánh kem nội thành.', N'Toàn khu vực nội thành các chi nhánh');
SET IDENTITY_INSERT dbo.[deliveries] OFF;
GO

-- Dữ liệu bảng: jwt_refresh_tokens
INSERT INTO dbo.[jwt_refresh_tokens] ([consumed], [expires_at], [session_id], [token_hash]) VALUES (0, '2026-11-04 15:30:41.382284', N'4020cd7a-304c-4824-8f6f-bb01b01e3128', N'a727dbed3f66f8dbb805a03a3d74596b7d2496941e5510a24755b43f271e2bb7');
GO

-- Dữ liệu bảng: jwt_sessions
SET IDENTITY_INSERT dbo.[jwt_sessions] ON;
INSERT INTO dbo.[jwt_sessions] ([revoked], [expires_at], [user_id], [id], [credential_hash]) VALUES (0, '2026-11-04 15:30:41.382284', 42, N'4020cd7a-304c-4824-8f6f-bb01b01e3128', N'b4514a964de68b94a0cff7d79e822bf7a749fd56f5edec8d01677075e77d15b6');
SET IDENTITY_INSERT dbo.[jwt_sessions] OFF;
GO

-- Dữ liệu bảng: notifications
SET IDENTITY_INSERT dbo.[notifications] ON;
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (1, '2026-10-07 01:05:40.136648', 1, 18, 5, N'ORDER_CREATED', N'🌸 Đặt hoa thành công!', N'Đơn hoa #18 trị giá 1,775,000 đ đã được tạo thành công. Tiệm hoa đang tiến hành chuẩn bị.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (1, '2026-10-07 01:05:40.16315', 2, 18, 2, N'ORDER_NEW', N'🔔 Đơn hoa mới #18', N'Có đơn hoa mới từ khách Lê Khách Hàng Yêu Hoa (0909999888). Tổng: 1,775,000 đ.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (0, '2026-10-07 10:25:36.931872', 5, 19, 35, N'ORDER_CREATED', N'🌸 Đặt hoa thành công!', N'Đơn hoa #19 trị giá 890,000 đ đã được tạo thành công. Tiệm hoa đang tiến hành chuẩn bị.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (1, '2026-10-07 10:25:36.940383', 6, 19, 2, N'ORDER_NEW', N'🔔 Đơn hoa mới #19', N'Có đơn hoa mới từ khách Đinh Quốc Anh (0912457891). Tổng: 890,000 đ.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (1, '2026-10-07 16:48:41.776239', 8, 20, 2, N'ORDER_NEW', N'🔔 Đơn hoa mới #20', N'Có đơn hoa mới từ khách Lê Minh Hoa (0933516292). Tổng: 39,000,000 đ.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (1, '2026-10-07 16:49:26.722207', 10, 21, 6, N'ORDER_NEW', N'🔔 Đơn hoa mới #21', N'Có đơn hoa mới từ khách Lê Minh Hoa (0933516292). Tổng: 39,000,000 đ.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (0, '2026-10-07 16:49:26.72835', 11, 21, 29, N'ORDER_NEW', N'🔔 Đơn hoa mới #21', N'Có đơn hoa mới từ khách Lê Minh Hoa (0933516292). Tổng: 39,000,000 đ.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (0, '2026-10-07 16:49:26.730408', 12, 21, 30, N'ORDER_NEW', N'🔔 Đơn hoa mới #21', N'Có đơn hoa mới từ khách Lê Minh Hoa (0933516292). Tổng: 39,000,000 đ.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (0, '2026-10-07 16:49:26.730408', 13, 21, 31, N'ORDER_NEW', N'🔔 Đơn hoa mới #21', N'Có đơn hoa mới từ khách Lê Minh Hoa (0933516292). Tổng: 39,000,000 đ.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (0, '2026-10-07 16:49:26.731809', 14, 21, 32, N'ORDER_NEW', N'🔔 Đơn hoa mới #21', N'Có đơn hoa mới từ khách Lê Minh Hoa (0933516292). Tổng: 39,000,000 đ.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (1, '2026-10-13 07:51:00.361222', 16, 22, 2, N'ORDER_NEW', N'🔔 Đơn hoa mới #22', N'Có đơn hoa mới từ khách Lê Minh Hoa (0933516292). Tổng: 34,500,000 đ.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (1, '2026-10-13 08:53:24.638816', 20, 23, 2, N'ORDER_NEW', N'🔔 Đơn hoa mới #23', N'Có đơn hoa mới từ khách Lê Minh Hoa (0933516292). Tổng: 34,500,000 đ.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (1, '2026-10-13 09:12:22.652574', 22, 24, 6, N'ORDER_NEW', N'🔔 Đơn hoa mới #24', N'Có đơn hoa mới từ khách Lê Khách Hàng Yêu Hoa (0909999888). Tổng: 34,425,000 đ.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (0, '2026-10-13 09:12:22.722137', 23, 24, 29, N'ORDER_NEW', N'🔔 Đơn hoa mới #24', N'Có đơn hoa mới từ khách Lê Khách Hàng Yêu Hoa (0909999888). Tổng: 34,425,000 đ.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (0, '2026-10-13 09:12:22.731308', 24, 24, 30, N'ORDER_NEW', N'🔔 Đơn hoa mới #24', N'Có đơn hoa mới từ khách Lê Khách Hàng Yêu Hoa (0909999888). Tổng: 34,425,000 đ.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (0, '2026-10-13 09:12:22.737333', 25, 24, 31, N'ORDER_NEW', N'🔔 Đơn hoa mới #24', N'Có đơn hoa mới từ khách Lê Khách Hàng Yêu Hoa (0909999888). Tổng: 34,425,000 đ.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (0, '2026-10-13 09:12:22.742624', 26, 24, 32, N'ORDER_NEW', N'🔔 Đơn hoa mới #24', N'Có đơn hoa mới từ khách Lê Khách Hàng Yêu Hoa (0909999888). Tổng: 34,425,000 đ.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (1, '2026-10-13 15:01:26.402903', 28, 25, 6, N'ORDER_NEW', N'🔔 Đơn hoa mới #25', N'Có đơn hoa mới từ khách Lê Khách Hàng Yêu Hoa (0909999888). Tổng: 34,475,000 đ.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (0, '2026-10-13 15:01:26.459983', 29, 25, 29, N'ORDER_NEW', N'🔔 Đơn hoa mới #25', N'Có đơn hoa mới từ khách Lê Khách Hàng Yêu Hoa (0909999888). Tổng: 34,475,000 đ.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (0, '2026-10-13 15:01:26.478017', 30, 25, 30, N'ORDER_NEW', N'🔔 Đơn hoa mới #25', N'Có đơn hoa mới từ khách Lê Khách Hàng Yêu Hoa (0909999888). Tổng: 34,475,000 đ.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (0, '2026-10-13 15:01:26.478017', 31, 25, 31, N'ORDER_NEW', N'🔔 Đơn hoa mới #25', N'Có đơn hoa mới từ khách Lê Khách Hàng Yêu Hoa (0909999888). Tổng: 34,475,000 đ.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (0, '2026-10-13 15:01:26.490828', 32, 25, 32, N'ORDER_NEW', N'🔔 Đơn hoa mới #25', N'Có đơn hoa mới từ khách Lê Khách Hàng Yêu Hoa (0909999888). Tổng: 34,475,000 đ.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (1, '2026-10-13 15:16:24.510389', 37, 25, 6, N'ORDER_COMPLETED', N'✨ Đơn hoa #25 đã hoàn tất', N'Đơn hoa #25 đã giao thành công tới người nhận.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (0, '2026-10-13 15:16:24.535613', 38, 25, 29, N'ORDER_COMPLETED', N'✨ Đơn hoa #25 đã hoàn tất', N'Đơn hoa #25 đã giao thành công tới người nhận.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (0, '2026-10-13 15:16:24.536614', 39, 25, 30, N'ORDER_COMPLETED', N'✨ Đơn hoa #25 đã hoàn tất', N'Đơn hoa #25 đã giao thành công tới người nhận.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (0, '2026-10-13 15:16:24.538602', 40, 25, 31, N'ORDER_COMPLETED', N'✨ Đơn hoa #25 đã hoàn tất', N'Đơn hoa #25 đã giao thành công tới người nhận.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (0, '2026-10-13 15:16:24.539604', 41, 25, 32, N'ORDER_COMPLETED', N'✨ Đơn hoa #25 đã hoàn tất', N'Đơn hoa #25 đã giao thành công tới người nhận.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (1, '2026-10-13 15:16:30.245474', 43, 25, 6, N'ORDER_COMPLETED', N'✨ Đơn hoa #25 đã hoàn tất', N'Đơn hoa #25 đã giao thành công tới người nhận.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (0, '2026-10-13 15:16:30.253877', 44, 25, 29, N'ORDER_COMPLETED', N'✨ Đơn hoa #25 đã hoàn tất', N'Đơn hoa #25 đã giao thành công tới người nhận.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (0, '2026-10-13 15:16:30.258608', 45, 25, 30, N'ORDER_COMPLETED', N'✨ Đơn hoa #25 đã hoàn tất', N'Đơn hoa #25 đã giao thành công tới người nhận.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (0, '2026-10-13 15:16:30.260141', 46, 25, 31, N'ORDER_COMPLETED', N'✨ Đơn hoa #25 đã hoàn tất', N'Đơn hoa #25 đã giao thành công tới người nhận.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (0, '2026-10-13 15:16:30.261756', 47, 25, 32, N'ORDER_COMPLETED', N'✨ Đơn hoa #25 đã hoàn tất', N'Đơn hoa #25 đã giao thành công tới người nhận.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (1, '2026-10-13 15:17:28.071619', 49, 26, 6, N'ORDER_NEW', N'🔔 Đơn hoa mới #26', N'Có đơn hoa mới từ khách Lê Khách Hàng Yêu Hoa (0909999888). Tổng: 3,275,000 đ.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (0, '2026-10-13 15:17:28.083994', 50, 26, 29, N'ORDER_NEW', N'🔔 Đơn hoa mới #26', N'Có đơn hoa mới từ khách Lê Khách Hàng Yêu Hoa (0909999888). Tổng: 3,275,000 đ.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (0, '2026-10-13 15:17:28.089113', 51, 26, 30, N'ORDER_NEW', N'🔔 Đơn hoa mới #26', N'Có đơn hoa mới từ khách Lê Khách Hàng Yêu Hoa (0909999888). Tổng: 3,275,000 đ.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (0, '2026-10-13 15:17:28.094677', 52, 26, 31, N'ORDER_NEW', N'🔔 Đơn hoa mới #26', N'Có đơn hoa mới từ khách Lê Khách Hàng Yêu Hoa (0909999888). Tổng: 3,275,000 đ.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (0, '2026-10-13 15:17:28.094677', 53, 26, 32, N'ORDER_NEW', N'🔔 Đơn hoa mới #26', N'Có đơn hoa mới từ khách Lê Khách Hàng Yêu Hoa (0909999888). Tổng: 3,275,000 đ.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (1, '2026-10-13 15:17:51.287256', 54, 26, 5, N'ORDER_CONFIRMED', N'✅ Đơn hoa #26 đã được xác nhận', N'Chi nhánh Chi Nhánh Phú Mỹ Hưng (Quận 7) đã tiếp nhận đơn của bạn.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (1, '2026-10-13 15:20:44.189291', 57, 26, 6, N'ORDER_CANCELLED', N'⚠️ Đơn hoa #26 đã bị hủy', N'Đơn hoa #26 đã bị hủy bởi người dùng hoặc hệ thống.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (0, '2026-10-13 15:20:44.198388', 58, 26, 29, N'ORDER_CANCELLED', N'⚠️ Đơn hoa #26 đã bị hủy', N'Đơn hoa #26 đã bị hủy bởi người dùng hoặc hệ thống.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (0, '2026-10-13 15:20:44.198388', 59, 26, 30, N'ORDER_CANCELLED', N'⚠️ Đơn hoa #26 đã bị hủy', N'Đơn hoa #26 đã bị hủy bởi người dùng hoặc hệ thống.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (0, '2026-10-13 15:20:44.198388', 60, 26, 31, N'ORDER_CANCELLED', N'⚠️ Đơn hoa #26 đã bị hủy', N'Đơn hoa #26 đã bị hủy bởi người dùng hoặc hệ thống.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (0, '2026-10-13 15:20:44.198388', 61, 26, 32, N'ORDER_CANCELLED', N'⚠️ Đơn hoa #26 đã bị hủy', N'Đơn hoa #26 đã bị hủy bởi người dùng hoặc hệ thống.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (1, '2026-10-13 15:36:37.155381', 64, 23, 2, N'ORDER_CANCELLED', N'⚠️ Đơn hoa #23 đã bị hủy', N'Đơn hàng của bạn đã bị hủy bởi admin (quản trị viên) với lý do: không còn hàng. Đơn chưa thanh toán đã được hủy.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (1, '2026-10-19 11:13:41.310619', 66, 27, 2, N'ORDER_NEW', N'🔔 Đơn hoa mới #27', N'Có đơn hoa mới từ khách Lê Khách Hàng Yêu Hoa (0909999888). Tổng: 744,100 đ.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (1, '2026-10-21 21:44:22.667377', 67, 28, 5, N'ORDER_CREATED', N'🌸 Đặt hoa thành công!', N'Đơn hoa #28 trị giá 975,000 đ đã được tạo thành công. Tiệm hoa đang tiến hành chuẩn bị.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (1, '2026-10-21 21:44:22.717507', 68, 28, 6, N'ORDER_NEW', N'🔔 Đơn hoa mới #28', N'Có đơn hoa mới từ khách Lê Khách Hàng Yêu Hoa (0909999888). Tổng: 975,000 đ.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (0, '2026-10-21 21:44:22.770593', 69, 28, 29, N'ORDER_NEW', N'🔔 Đơn hoa mới #28', N'Có đơn hoa mới từ khách Lê Khách Hàng Yêu Hoa (0909999888). Tổng: 975,000 đ.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (0, '2026-10-21 21:44:22.774541', 70, 28, 30, N'ORDER_NEW', N'🔔 Đơn hoa mới #28', N'Có đơn hoa mới từ khách Lê Khách Hàng Yêu Hoa (0909999888). Tổng: 975,000 đ.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (0, '2026-10-21 21:44:22.778564', 71, 28, 31, N'ORDER_NEW', N'🔔 Đơn hoa mới #28', N'Có đơn hoa mới từ khách Lê Khách Hàng Yêu Hoa (0909999888). Tổng: 975,000 đ.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (0, '2026-10-21 21:44:22.782559', 72, 28, 32, N'ORDER_NEW', N'🔔 Đơn hoa mới #28', N'Có đơn hoa mới từ khách Lê Khách Hàng Yêu Hoa (0909999888). Tổng: 975,000 đ.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (1, '2026-10-21 21:48:48.572318', 73, 29, 37, N'ORDER_CREATED', N'🌸 Đặt hoa thành công!', N'Đơn hoa #29 trị giá 650,000 đ đã được tạo thành công. Tiệm hoa đang tiến hành chuẩn bị.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (1, '2026-10-21 21:48:48.597233', 74, 29, 6, N'ORDER_NEW', N'🔔 Đơn hoa mới #29', N'Có đơn hoa mới từ khách Lý Gia Bảo (0947475853). Tổng: 650,000 đ.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (0, '2026-10-21 21:48:48.633592', 75, 29, 29, N'ORDER_NEW', N'🔔 Đơn hoa mới #29', N'Có đơn hoa mới từ khách Lý Gia Bảo (0947475853). Tổng: 650,000 đ.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (0, '2026-10-21 21:48:48.638122', 76, 29, 30, N'ORDER_NEW', N'🔔 Đơn hoa mới #29', N'Có đơn hoa mới từ khách Lý Gia Bảo (0947475853). Tổng: 650,000 đ.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (0, '2026-10-21 21:48:48.641658', 77, 29, 31, N'ORDER_NEW', N'🔔 Đơn hoa mới #29', N'Có đơn hoa mới từ khách Lý Gia Bảo (0947475853). Tổng: 650,000 đ.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (0, '2026-10-21 21:48:48.645661', 78, 29, 32, N'ORDER_NEW', N'🔔 Đơn hoa mới #29', N'Có đơn hoa mới từ khách Lý Gia Bảo (0947475853). Tổng: 650,000 đ.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (0, '2026-10-22 15:36:39.84406', 79, 29, 37, N'ORDER_CONFIRMED', N'✅ Đơn hoa #29 đã được xác nhận', N'Chi nhánh Chi Nhánh Phú Mỹ Hưng (Quận 7) đã tiếp nhận đơn của bạn.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (0, '2026-10-22 15:38:56.575843', 80, 29, 37, N'ORDER_PREPARING', N'🌺 Đang cắm hoa đơn #29', N'Nghệ nhân tiệm hoa đã bắt đầu cắm hoa/chuẩn bị cho bạn. Từ thời điểm này, bạn không thể tự hủy đơn.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (0, '2026-10-22 15:52:09.863581', 81, 30, 37, N'ORDER_CREATED', N'🌸 Đặt hoa thành công!', N'Đơn hoa #30 trị giá 390,000 đ đã được tạo thành công. Tiệm hoa đang tiến hành chuẩn bị.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (1, '2026-10-22 15:57:26.14145', 82, 29, 37, N'ORDER_DELIVERED', N'🎉 Giao hoa thành công đơn #29', N'Đơn hoa đã được giao đến nơi! Cảm ơn bạn đã lựa chọn tiệm hoa. Hãy để lại đánh giá cảm nhận nhé.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (1, '2026-10-22 15:57:26.15973', 83, 29, 6, N'ORDER_COMPLETED', N'✨ Đơn hoa #29 đã hoàn tất', N'Đơn hoa #29 đã giao thành công tới người nhận.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (0, '2026-10-22 15:57:26.164728', 84, 29, 29, N'ORDER_COMPLETED', N'✨ Đơn hoa #29 đã hoàn tất', N'Đơn hoa #29 đã giao thành công tới người nhận.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (0, '2026-10-22 15:57:26.165729', 85, 29, 30, N'ORDER_COMPLETED', N'✨ Đơn hoa #29 đã hoàn tất', N'Đơn hoa #29 đã giao thành công tới người nhận.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (0, '2026-10-22 15:57:26.166728', 86, 29, 31, N'ORDER_COMPLETED', N'✨ Đơn hoa #29 đã hoàn tất', N'Đơn hoa #29 đã giao thành công tới người nhận.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (0, '2026-10-22 15:57:26.167727', 87, 29, 32, N'ORDER_COMPLETED', N'✨ Đơn hoa #29 đã hoàn tất', N'Đơn hoa #29 đã giao thành công tới người nhận.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (1, '2026-10-22 16:53:29.494736', 88, 31, 37, N'ORDER_CREATED', N'🌸 Đặt hoa thành công!', N'Đơn hoa #31 trị giá 950,000 đ đã được tạo thành công. Tiệm hoa đang tiến hành chuẩn bị.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (1, '2026-10-22 16:53:29.508397', 89, 31, 6, N'ORDER_NEW', N'🔔 Đơn hoa mới #31', N'Có đơn hoa mới từ khách Lý Gia Bảo (0947475853). Tổng: 950,000 đ.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (0, '2026-10-22 16:53:29.524532', 90, 31, 29, N'ORDER_NEW', N'🔔 Đơn hoa mới #31', N'Có đơn hoa mới từ khách Lý Gia Bảo (0947475853). Tổng: 950,000 đ.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (0, '2026-10-22 16:53:29.525538', 91, 31, 30, N'ORDER_NEW', N'🔔 Đơn hoa mới #31', N'Có đơn hoa mới từ khách Lý Gia Bảo (0947475853). Tổng: 950,000 đ.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (0, '2026-10-22 16:53:29.526538', 92, 31, 31, N'ORDER_NEW', N'🔔 Đơn hoa mới #31', N'Có đơn hoa mới từ khách Lý Gia Bảo (0947475853). Tổng: 950,000 đ.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (0, '2026-10-22 16:53:29.527534', 93, 31, 32, N'ORDER_NEW', N'🔔 Đơn hoa mới #31', N'Có đơn hoa mới từ khách Lý Gia Bảo (0947475853). Tổng: 950,000 đ.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (0, '2026-10-23 22:38:58.001564', 96, 33, 6, N'ORDER_NEW', N'🔔 Đơn hoa mới #33', N'Có đơn hoa mới từ khách Lê Minh Hoa (0933516292). Tổng: 915.000 đ.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (0, '2026-10-23 22:38:58.588894', 97, 33, 29, N'ORDER_NEW', N'🔔 Đơn hoa mới #33', N'Có đơn hoa mới từ khách Lê Minh Hoa (0933516292). Tổng: 915.000 đ.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (0, '2026-10-23 22:38:58.667494', 98, 33, 30, N'ORDER_NEW', N'🔔 Đơn hoa mới #33', N'Có đơn hoa mới từ khách Lê Minh Hoa (0933516292). Tổng: 915.000 đ.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (0, '2026-10-23 22:38:58.748116', 99, 33, 31, N'ORDER_NEW', N'🔔 Đơn hoa mới #33', N'Có đơn hoa mới từ khách Lê Minh Hoa (0933516292). Tổng: 915.000 đ.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (0, '2026-10-23 22:38:58.846222', 100, 33, 32, N'ORDER_NEW', N'🔔 Đơn hoa mới #33', N'Có đơn hoa mới từ khách Lê Minh Hoa (0933516292). Tổng: 915.000 đ.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (0, '2026-10-23 22:49:02.49771', 102, 33, 6, N'ORDER_CANCELLED', N'⚠️ Đơn hoa #33 đã bị hủy', N'Đơn đã hết thời hạn thanh toán. Tồn kho đã được trả lại chi nhánh.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (0, '2026-10-23 22:49:02.578883', 103, 33, 29, N'ORDER_CANCELLED', N'⚠️ Đơn hoa #33 đã bị hủy', N'Đơn đã hết thời hạn thanh toán. Tồn kho đã được trả lại chi nhánh.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (0, '2026-10-23 22:49:02.59318', 104, 33, 30, N'ORDER_CANCELLED', N'⚠️ Đơn hoa #33 đã bị hủy', N'Đơn đã hết thời hạn thanh toán. Tồn kho đã được trả lại chi nhánh.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (0, '2026-10-23 22:49:02.596192', 105, 33, 31, N'ORDER_CANCELLED', N'⚠️ Đơn hoa #33 đã bị hủy', N'Đơn đã hết thời hạn thanh toán. Tồn kho đã được trả lại chi nhánh.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (0, '2026-10-23 22:49:02.601453', 106, 33, 32, N'ORDER_CANCELLED', N'⚠️ Đơn hoa #33 đã bị hủy', N'Đơn đã hết thời hạn thanh toán. Tồn kho đã được trả lại chi nhánh.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (0, '2026-10-23 23:20:32.736127', 108, 34, 6, N'ORDER_NEW', N'🔔 Đơn hoa mới #34', N'Có đơn hoa mới từ khách Lê Minh Hoa (0933516292). Tổng: 675.000 đ.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (0, '2026-10-23 23:20:33.795689', 109, 34, 29, N'ORDER_NEW', N'🔔 Đơn hoa mới #34', N'Có đơn hoa mới từ khách Lê Minh Hoa (0933516292). Tổng: 675.000 đ.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (0, '2026-10-23 23:20:33.911984', 110, 34, 30, N'ORDER_NEW', N'🔔 Đơn hoa mới #34', N'Có đơn hoa mới từ khách Lê Minh Hoa (0933516292). Tổng: 675.000 đ.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (0, '2026-10-23 23:20:34.030966', 111, 34, 31, N'ORDER_NEW', N'🔔 Đơn hoa mới #34', N'Có đơn hoa mới từ khách Lê Minh Hoa (0933516292). Tổng: 675.000 đ.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (0, '2026-10-23 23:20:34.139009', 112, 34, 32, N'ORDER_NEW', N'🔔 Đơn hoa mới #34', N'Có đơn hoa mới từ khách Lê Minh Hoa (0933516292). Tổng: 675.000 đ.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (0, '2026-10-24 14:36:46.223882', 115, 35, 6, N'ORDER_NEW', N'🔔 Đơn hoa mới #35', N'Có đơn hoa mới từ khách Lê Minh Hoa (0933516292). Tổng: 675.000 đ.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (0, '2026-10-24 14:36:47.242099', 116, 35, 29, N'ORDER_NEW', N'🔔 Đơn hoa mới #35', N'Có đơn hoa mới từ khách Lê Minh Hoa (0933516292). Tổng: 675.000 đ.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (0, '2026-10-24 14:36:47.445421', 117, 35, 30, N'ORDER_NEW', N'🔔 Đơn hoa mới #35', N'Có đơn hoa mới từ khách Lê Minh Hoa (0933516292). Tổng: 675.000 đ.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (0, '2026-10-24 14:36:47.637888', 118, 35, 31, N'ORDER_NEW', N'🔔 Đơn hoa mới #35', N'Có đơn hoa mới từ khách Lê Minh Hoa (0933516292). Tổng: 675.000 đ.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (0, '2026-10-24 14:36:47.778101', 119, 35, 32, N'ORDER_NEW', N'🔔 Đơn hoa mới #35', N'Có đơn hoa mới từ khách Lê Minh Hoa (0933516292). Tổng: 675.000 đ.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (1, '2026-10-24 14:37:24.887732', 120, 35, 5, N'REFUND_COMPLETED', N'💰 Hoàn tiền thành công đơn #35', N'Hoàn tiền thành công. Giao dịch demo, không phát sinh chuyển tiền MoMo thật.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (0, '2026-10-24 15:09:59.272524', 121, 6, 34, N'ORDER_DELIVERING', N'🚀 Đơn hoa #6 đang được giao', N'Đơn hoa của bạn đang trên đường vận chuyển. Vui lòng để ý điện thoại nhé!');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (0, '2026-10-24 15:42:00.101409', 122, 11, 34, N'ORDER_DELIVERED', N'🎉 Giao hoa thành công đơn #11', N'Đơn hoa đã được giao đến nơi! Cảm ơn bạn đã lựa chọn tiệm hoa. Hãy để lại đánh giá cảm nhận nhé.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (0, '2026-10-24 15:42:00.299609', 123, 11, 6, N'ORDER_COMPLETED', N'✨ Đơn hoa #11 đã hoàn tất', N'Đơn hoa #11 đã giao thành công tới người nhận.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (0, '2026-10-24 15:42:00.336097', 124, 11, 29, N'ORDER_COMPLETED', N'✨ Đơn hoa #11 đã hoàn tất', N'Đơn hoa #11 đã giao thành công tới người nhận.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (0, '2026-10-24 15:42:00.341097', 125, 11, 30, N'ORDER_COMPLETED', N'✨ Đơn hoa #11 đã hoàn tất', N'Đơn hoa #11 đã giao thành công tới người nhận.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (0, '2026-10-24 15:42:00.3481', 126, 11, 31, N'ORDER_COMPLETED', N'✨ Đơn hoa #11 đã hoàn tất', N'Đơn hoa #11 đã giao thành công tới người nhận.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (0, '2026-10-24 15:42:00.354109', 127, 11, 32, N'ORDER_COMPLETED', N'✨ Đơn hoa #11 đã hoàn tất', N'Đơn hoa #11 đã giao thành công tới người nhận.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (0, '2026-10-24 17:22:07.249418', 129, 36, 6, N'ORDER_NEW', N'🔔 Đơn hoa mới #36', N'Có đơn hoa mới từ khách Lê Minh Hoa (0933516292). Tổng: 675.000 đ.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (0, '2026-10-24 17:22:07.957151', 130, 36, 29, N'ORDER_NEW', N'🔔 Đơn hoa mới #36', N'Có đơn hoa mới từ khách Lê Minh Hoa (0933516292). Tổng: 675.000 đ.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (0, '2026-10-24 17:22:08.046465', 131, 36, 30, N'ORDER_NEW', N'🔔 Đơn hoa mới #36', N'Có đơn hoa mới từ khách Lê Minh Hoa (0933516292). Tổng: 675.000 đ.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (0, '2026-10-24 17:22:08.138145', 132, 36, 31, N'ORDER_NEW', N'🔔 Đơn hoa mới #36', N'Có đơn hoa mới từ khách Lê Minh Hoa (0933516292). Tổng: 675.000 đ.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (0, '2026-10-24 17:22:08.235083', 133, 36, 32, N'ORDER_NEW', N'🔔 Đơn hoa mới #36', N'Có đơn hoa mới từ khách Lê Minh Hoa (0933516292). Tổng: 675.000 đ.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (0, '2026-10-24 18:51:45.430536', 136, 37, 2, N'ORDER_NEW', N'🔔 Đơn hoa mới #37', N'Có đơn hoa mới từ khách Lê Minh Hoa (0933516292). Tổng: 675.000 đ.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (0, '2026-10-24 18:51:45.900886', 137, 37, 39, N'ORDER_NEW', N'🔔 Đơn hoa mới #37', N'Có đơn hoa mới từ khách Lê Minh Hoa (0933516292). Tổng: 675.000 đ.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (0, '2026-10-25 18:19:59.881602', 140, 38, 6, N'ORDER_NEW', N'🔔 Đơn hoa mới #38', N'Có đơn hoa mới từ khách Lê Minh Hoa (0933516292). Tổng: 675.000 đ.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (0, '2026-10-25 18:20:01.331863', 141, 38, 29, N'ORDER_NEW', N'🔔 Đơn hoa mới #38', N'Có đơn hoa mới từ khách Lê Minh Hoa (0933516292). Tổng: 675.000 đ.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (0, '2026-10-25 18:20:01.539535', 142, 38, 30, N'ORDER_NEW', N'🔔 Đơn hoa mới #38', N'Có đơn hoa mới từ khách Lê Minh Hoa (0933516292). Tổng: 675.000 đ.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (0, '2026-10-25 18:20:01.742563', 143, 38, 31, N'ORDER_NEW', N'🔔 Đơn hoa mới #38', N'Có đơn hoa mới từ khách Lê Minh Hoa (0933516292). Tổng: 675.000 đ.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (0, '2026-10-25 18:20:01.947932', 144, 38, 32, N'ORDER_NEW', N'🔔 Đơn hoa mới #38', N'Có đơn hoa mới từ khách Lê Minh Hoa (0933516292). Tổng: 675.000 đ.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (0, '2026-10-25 18:30:40.176101', 146, 38, 6, N'ORDER_CANCELLED', N'⚠️ Đơn hoa #38 đã bị hủy', N'Đơn đã hết thời hạn thanh toán. Tồn kho đã được trả lại chi nhánh.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (0, '2026-10-25 18:30:41.639091', 147, 38, 29, N'ORDER_CANCELLED', N'⚠️ Đơn hoa #38 đã bị hủy', N'Đơn đã hết thời hạn thanh toán. Tồn kho đã được trả lại chi nhánh.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (0, '2026-10-25 18:30:41.853106', 148, 38, 30, N'ORDER_CANCELLED', N'⚠️ Đơn hoa #38 đã bị hủy', N'Đơn đã hết thời hạn thanh toán. Tồn kho đã được trả lại chi nhánh.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (0, '2026-10-25 18:30:42.052863', 149, 38, 31, N'ORDER_CANCELLED', N'⚠️ Đơn hoa #38 đã bị hủy', N'Đơn đã hết thời hạn thanh toán. Tồn kho đã được trả lại chi nhánh.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (0, '2026-10-25 18:30:42.259518', 150, 38, 32, N'ORDER_CANCELLED', N'⚠️ Đơn hoa #38 đã bị hủy', N'Đơn đã hết thời hạn thanh toán. Tồn kho đã được trả lại chi nhánh.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (0, '2026-10-25 23:36:02.488372', 152, 39, 6, N'ORDER_NEW', N'🔔 Đơn hoa mới #39', N'Có đơn hoa mới từ khách Lê Minh Hoa (0933516292). Tổng: 915.000 đ.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (0, '2026-10-25 23:36:03.999562', 153, 39, 29, N'ORDER_NEW', N'🔔 Đơn hoa mới #39', N'Có đơn hoa mới từ khách Lê Minh Hoa (0933516292). Tổng: 915.000 đ.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (0, '2026-10-25 23:36:04.214212', 154, 39, 30, N'ORDER_NEW', N'🔔 Đơn hoa mới #39', N'Có đơn hoa mới từ khách Lê Minh Hoa (0933516292). Tổng: 915.000 đ.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (0, '2026-10-25 23:36:04.425997', 155, 39, 31, N'ORDER_NEW', N'🔔 Đơn hoa mới #39', N'Có đơn hoa mới từ khách Lê Minh Hoa (0933516292). Tổng: 915.000 đ.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (0, '2026-10-25 23:36:04.640057', 156, 39, 32, N'ORDER_NEW', N'🔔 Đơn hoa mới #39', N'Có đơn hoa mới từ khách Lê Minh Hoa (0933516292). Tổng: 915.000 đ.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (0, '2026-10-25 23:50:48.386676', 158, 40, 6, N'ORDER_NEW', N'🔔 Đơn hoa mới #40', N'Có đơn hoa mới từ khách Lê Minh Hoa (0933516292). Tổng: 915.000 đ.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (0, '2026-10-25 23:50:50.115649', 159, 40, 29, N'ORDER_NEW', N'🔔 Đơn hoa mới #40', N'Có đơn hoa mới từ khách Lê Minh Hoa (0933516292). Tổng: 915.000 đ.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (0, '2026-10-25 23:50:50.397382', 160, 40, 30, N'ORDER_NEW', N'🔔 Đơn hoa mới #40', N'Có đơn hoa mới từ khách Lê Minh Hoa (0933516292). Tổng: 915.000 đ.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (0, '2026-10-25 23:50:50.652728', 161, 40, 31, N'ORDER_NEW', N'🔔 Đơn hoa mới #40', N'Có đơn hoa mới từ khách Lê Minh Hoa (0933516292). Tổng: 915.000 đ.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (0, '2026-10-25 23:50:50.901885', 162, 40, 32, N'ORDER_NEW', N'🔔 Đơn hoa mới #40', N'Có đơn hoa mới từ khách Lê Minh Hoa (0933516292). Tổng: 915.000 đ.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (0, '2026-10-26 00:05:51.963997', 165, 41, 6, N'ORDER_NEW', N'🔔 Đơn hoa mới #41', N'Có đơn hoa mới từ khách Lê Minh Hoa (0933516292). Tổng: 715.000 đ.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (0, '2026-10-26 00:05:53.41323', 166, 41, 29, N'ORDER_NEW', N'🔔 Đơn hoa mới #41', N'Có đơn hoa mới từ khách Lê Minh Hoa (0933516292). Tổng: 715.000 đ.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (0, '2026-10-26 00:05:53.620283', 167, 41, 30, N'ORDER_NEW', N'🔔 Đơn hoa mới #41', N'Có đơn hoa mới từ khách Lê Minh Hoa (0933516292). Tổng: 715.000 đ.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (0, '2026-10-26 00:05:53.83039', 168, 41, 31, N'ORDER_NEW', N'🔔 Đơn hoa mới #41', N'Có đơn hoa mới từ khách Lê Minh Hoa (0933516292). Tổng: 715.000 đ.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (0, '2026-10-26 00:05:54.034346', 169, 41, 32, N'ORDER_NEW', N'🔔 Đơn hoa mới #41', N'Có đơn hoa mới từ khách Lê Minh Hoa (0933516292). Tổng: 715.000 đ.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (1, '2026-10-26 21:14:51.520666', 172, 42, 6, N'ORDER_NEW', N'🔔 Đơn hoa mới #42', N'Có đơn hoa mới từ khách Lê Minh Hoa (0933516292). Tổng: 545.000 đ.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (0, '2026-10-26 21:14:54.378725', 173, 42, 29, N'ORDER_NEW', N'🔔 Đơn hoa mới #42', N'Có đơn hoa mới từ khách Lê Minh Hoa (0933516292). Tổng: 545.000 đ.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (0, '2026-10-26 21:14:54.695458', 174, 42, 30, N'ORDER_NEW', N'🔔 Đơn hoa mới #42', N'Có đơn hoa mới từ khách Lê Minh Hoa (0933516292). Tổng: 545.000 đ.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (0, '2026-10-26 21:14:54.99371', 175, 42, 31, N'ORDER_NEW', N'🔔 Đơn hoa mới #42', N'Có đơn hoa mới từ khách Lê Minh Hoa (0933516292). Tổng: 545.000 đ.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (0, '2026-10-26 21:14:55.401532', 176, 42, 32, N'ORDER_NEW', N'🔔 Đơn hoa mới #42', N'Có đơn hoa mới từ khách Lê Minh Hoa (0933516292). Tổng: 545.000 đ.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (0, '2026-10-26 21:25:37.46333', 181, 42, 6, N'ORDER_COMPLETED', N'✨ Đơn hoa #42 đã hoàn tất', N'Đơn hoa #42 đã giao thành công tới người nhận.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (0, '2026-10-26 21:25:39.395683', 182, 42, 29, N'ORDER_COMPLETED', N'✨ Đơn hoa #42 đã hoàn tất', N'Đơn hoa #42 đã giao thành công tới người nhận.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (0, '2026-10-26 21:25:39.703773', 183, 42, 30, N'ORDER_COMPLETED', N'✨ Đơn hoa #42 đã hoàn tất', N'Đơn hoa #42 đã giao thành công tới người nhận.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (0, '2026-10-26 21:25:40.006743', 184, 42, 31, N'ORDER_COMPLETED', N'✨ Đơn hoa #42 đã hoàn tất', N'Đơn hoa #42 đã giao thành công tới người nhận.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (0, '2026-10-26 21:25:40.261387', 185, 42, 32, N'ORDER_COMPLETED', N'✨ Đơn hoa #42 đã hoàn tất', N'Đơn hoa #42 đã giao thành công tới người nhận.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (0, '2026-10-26 21:34:38.319793', 187, 43, 6, N'ORDER_NEW', N'🔔 Đơn hoa mới #43', N'Có đơn hoa mới từ khách Lê Minh Hoa (0933516292). Tổng: 915.000 đ.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (0, '2026-10-26 21:34:40.116032', 188, 43, 29, N'ORDER_NEW', N'🔔 Đơn hoa mới #43', N'Có đơn hoa mới từ khách Lê Minh Hoa (0933516292). Tổng: 915.000 đ.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (0, '2026-10-26 21:34:40.37101', 189, 43, 30, N'ORDER_NEW', N'🔔 Đơn hoa mới #43', N'Có đơn hoa mới từ khách Lê Minh Hoa (0933516292). Tổng: 915.000 đ.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (0, '2026-10-26 21:34:40.623817', 190, 43, 31, N'ORDER_NEW', N'🔔 Đơn hoa mới #43', N'Có đơn hoa mới từ khách Lê Minh Hoa (0933516292). Tổng: 915.000 đ.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (0, '2026-10-26 21:34:40.881177', 191, 43, 32, N'ORDER_NEW', N'🔔 Đơn hoa mới #43', N'Có đơn hoa mới từ khách Lê Minh Hoa (0933516292). Tổng: 915.000 đ.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (0, '2026-10-28 14:17:25.240683', 192, 44, 40, N'ORDER_CREATED', N'🌸 Đặt hoa thành công!', N'Đơn hoa #44 trị giá 610,000 đ đã được tạo thành công. Tiệm hoa đang tiến hành chuẩn bị.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (0, '2026-10-28 14:17:25.251678', 193, 44, 2, N'ORDER_NEW', N'🔔 Đơn hoa mới #44', N'Có đơn hoa mới từ khách Local Customer (0909999888). Tổng: 610,000 đ.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (0, '2026-10-28 14:17:25.260955', 194, 44, 39, N'ORDER_NEW', N'🔔 Đơn hoa mới #44', N'Có đơn hoa mới từ khách Local Customer (0909999888). Tổng: 610,000 đ.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (0, '2026-10-28 14:17:25.261948', 195, 44, 41, N'ORDER_NEW', N'🔔 Đơn hoa mới #44', N'Có đơn hoa mới từ khách Local Customer (0909999888). Tổng: 610,000 đ.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (0, '2026-10-28 14:17:25.263364', 196, 44, 42, N'ORDER_NEW', N'🔔 Đơn hoa mới #44', N'Có đơn hoa mới từ khách Local Customer (0909999888). Tổng: 610,000 đ.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (0, '2026-10-28 14:27:45.229926', 197, 44, 40, N'ORDER_CANCELLED', N'❌ Đơn hoa #44 đã bị hủy', N'Đơn đã hết thời hạn thanh toán. Tồn kho đã được trả lại chi nhánh.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (0, '2026-10-28 14:27:45.255661', 198, 44, 2, N'ORDER_CANCELLED', N'⚠️ Đơn hoa #44 đã bị hủy', N'Đơn đã hết thời hạn thanh toán. Tồn kho đã được trả lại chi nhánh.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (0, '2026-10-28 14:27:45.2657', 199, 44, 39, N'ORDER_CANCELLED', N'⚠️ Đơn hoa #44 đã bị hủy', N'Đơn đã hết thời hạn thanh toán. Tồn kho đã được trả lại chi nhánh.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (0, '2026-10-28 14:27:45.267699', 200, 44, 41, N'ORDER_CANCELLED', N'⚠️ Đơn hoa #44 đã bị hủy', N'Đơn đã hết thời hạn thanh toán. Tồn kho đã được trả lại chi nhánh.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (0, '2026-10-28 14:27:45.269699', 201, 44, 42, N'ORDER_CANCELLED', N'⚠️ Đơn hoa #44 đã bị hủy', N'Đơn đã hết thời hạn thanh toán. Tồn kho đã được trả lại chi nhánh.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (0, '2026-10-28 14:30:30.219555', 203, 45, 6, N'ORDER_NEW', N'🔔 Đơn hoa mới #45', N'Có đơn hoa mới từ khách Lê Minh Hoa (0933516292). Tổng: 646,000 đ.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (0, '2026-10-28 14:30:30.233368', 204, 45, 29, N'ORDER_NEW', N'🔔 Đơn hoa mới #45', N'Có đơn hoa mới từ khách Lê Minh Hoa (0933516292). Tổng: 646,000 đ.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (0, '2026-10-28 14:30:30.233368', 205, 45, 30, N'ORDER_NEW', N'🔔 Đơn hoa mới #45', N'Có đơn hoa mới từ khách Lê Minh Hoa (0933516292). Tổng: 646,000 đ.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (0, '2026-10-28 14:30:30.233368', 206, 45, 31, N'ORDER_NEW', N'🔔 Đơn hoa mới #45', N'Có đơn hoa mới từ khách Lê Minh Hoa (0933516292). Tổng: 646,000 đ.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (0, '2026-10-28 14:30:30.240402', 207, 45, 32, N'ORDER_NEW', N'🔔 Đơn hoa mới #45', N'Có đơn hoa mới từ khách Lê Minh Hoa (0933516292). Tổng: 646,000 đ.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (0, '2026-10-28 14:40:45.455708', 209, 45, 6, N'ORDER_CANCELLED', N'⚠️ Đơn hoa #45 đã bị hủy', N'Đơn đã hết thời hạn thanh toán. Tồn kho đã được trả lại chi nhánh.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (0, '2026-10-28 14:40:45.455708', 210, 45, 29, N'ORDER_CANCELLED', N'⚠️ Đơn hoa #45 đã bị hủy', N'Đơn đã hết thời hạn thanh toán. Tồn kho đã được trả lại chi nhánh.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (0, '2026-10-28 14:40:45.455708', 211, 45, 30, N'ORDER_CANCELLED', N'⚠️ Đơn hoa #45 đã bị hủy', N'Đơn đã hết thời hạn thanh toán. Tồn kho đã được trả lại chi nhánh.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (0, '2026-10-28 14:40:45.455708', 212, 45, 31, N'ORDER_CANCELLED', N'⚠️ Đơn hoa #45 đã bị hủy', N'Đơn đã hết thời hạn thanh toán. Tồn kho đã được trả lại chi nhánh.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (0, '2026-10-28 14:40:45.455708', 213, 45, 32, N'ORDER_CANCELLED', N'⚠️ Đơn hoa #45 đã bị hủy', N'Đơn đã hết thời hạn thanh toán. Tồn kho đã được trả lại chi nhánh.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (0, '2026-10-28 14:53:45.834732', 214, 46, 2, N'ORDER_CREATED', N'🌸 Đặt hoa thành công!', N'Đơn hoa #46 trị giá 826,000 đ đã được tạo thành công. Tiệm hoa đang tiến hành chuẩn bị.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (0, '2026-10-28 14:53:45.852961', 215, 46, 6, N'ORDER_NEW', N'🔔 Đơn hoa mới #46', N'Có đơn hoa mới từ khách Nguyễn Quang Huy (0902222333). Tổng: 826,000 đ.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (0, '2026-10-28 14:53:45.880448', 216, 46, 29, N'ORDER_NEW', N'🔔 Đơn hoa mới #46', N'Có đơn hoa mới từ khách Nguyễn Quang Huy (0902222333). Tổng: 826,000 đ.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (0, '2026-10-28 14:53:45.883365', 217, 46, 30, N'ORDER_NEW', N'🔔 Đơn hoa mới #46', N'Có đơn hoa mới từ khách Nguyễn Quang Huy (0902222333). Tổng: 826,000 đ.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (0, '2026-10-28 14:53:45.883365', 218, 46, 31, N'ORDER_NEW', N'🔔 Đơn hoa mới #46', N'Có đơn hoa mới từ khách Nguyễn Quang Huy (0902222333). Tổng: 826,000 đ.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (0, '2026-10-28 14:53:45.883365', 219, 46, 32, N'ORDER_NEW', N'🔔 Đơn hoa mới #46', N'Có đơn hoa mới từ khách Nguyễn Quang Huy (0902222333). Tổng: 826,000 đ.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (0, '2026-10-28 15:04:09.728551', 220, 46, 2, N'ORDER_CANCELLED', N'❌ Đơn hoa #46 đã bị hủy', N'Đơn đã hết thời hạn thanh toán. Tồn kho đã được trả lại chi nhánh.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (0, '2026-10-28 15:04:09.730569', 221, 46, 6, N'ORDER_CANCELLED', N'⚠️ Đơn hoa #46 đã bị hủy', N'Đơn đã hết thời hạn thanh toán. Tồn kho đã được trả lại chi nhánh.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (0, '2026-10-28 15:04:09.734669', 222, 46, 29, N'ORDER_CANCELLED', N'⚠️ Đơn hoa #46 đã bị hủy', N'Đơn đã hết thời hạn thanh toán. Tồn kho đã được trả lại chi nhánh.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (0, '2026-10-28 15:04:09.734669', 223, 46, 30, N'ORDER_CANCELLED', N'⚠️ Đơn hoa #46 đã bị hủy', N'Đơn đã hết thời hạn thanh toán. Tồn kho đã được trả lại chi nhánh.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (0, '2026-10-28 15:04:09.734669', 224, 46, 31, N'ORDER_CANCELLED', N'⚠️ Đơn hoa #46 đã bị hủy', N'Đơn đã hết thời hạn thanh toán. Tồn kho đã được trả lại chi nhánh.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (0, '2026-10-28 15:04:09.748538', 225, 46, 32, N'ORDER_CANCELLED', N'⚠️ Đơn hoa #46 đã bị hủy', N'Đơn đã hết thời hạn thanh toán. Tồn kho đã được trả lại chi nhánh.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (1, '2026-10-28 14:40:45.455708', 208, 45, 5, N'ORDER_CANCELLED', N'❌ Đơn hoa #45 đã bị hủy', N'Đơn đã hết thời hạn thanh toán. Tồn kho đã được trả lại chi nhánh.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (1, '2026-10-28 14:30:30.212478', 202, 45, 5, N'ORDER_CREATED', N'🌸 Đặt hoa thành công!', N'Đơn hoa #45 trị giá 646,000 đ đã được tạo thành công. Tiệm hoa đang tiến hành chuẩn bị.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (1, '2026-10-26 21:34:37.977786', 186, 43, 5, N'ORDER_CREATED', N'🌸 Đặt hoa thành công!', N'Đơn hoa #43 trị giá 915.000 đ đã được tạo thành công. Tiệm hoa đang tiến hành chuẩn bị.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (1, '2026-10-26 21:25:37.158516', 180, 42, 5, N'ORDER_DELIVERED', N'🎉 Giao hoa thành công đơn #42', N'Đơn hoa đã được giao đến nơi! Cảm ơn bạn đã lựa chọn tiệm hoa. Hãy để lại đánh giá cảm nhận nhé.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (1, '2026-10-26 21:22:36.151945', 179, 42, 5, N'ORDER_DELIVERING', N'🚀 Đơn hoa #42 đang được giao', N'Đơn hoa của bạn đang trên đường vận chuyển. Vui lòng để ý điện thoại nhé!');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (1, '2026-10-26 21:21:08.85571', 178, 42, 5, N'ORDER_PREPARING', N'🌺 Đang cắm hoa đơn #42', N'Nghệ nhân tiệm hoa đã bắt đầu cắm hoa/chuẩn bị cho bạn. Từ thời điểm này, bạn không thể tự hủy đơn.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (1, '2026-10-26 21:20:23.078292', 177, 42, 5, N'ORDER_CONFIRMED', N'✅ Đơn hoa #42 đã được xác nhận', N'Chi nhánh Chi Nhánh Phú Mỹ Hưng (Quận 7) đã tiếp nhận đơn của bạn.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (1, '2026-10-26 21:14:51.181716', 171, 42, 5, N'ORDER_CREATED', N'🌸 Đặt hoa thành công!', N'Đơn hoa #42 trị giá 545.000 đ đã được tạo thành công. Tiệm hoa đang tiến hành chuẩn bị.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (1, '2026-10-26 00:07:06.777053', 170, 41, 5, N'REFUND_COMPLETED', N'💰 Hoàn tiền thành công đơn #41', N'Hoàn tiền thành công vào ví của bạn. Không phát sinh chuyển tiền MoMo hoặc ngân hàng thật.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (1, '2026-10-26 00:05:51.756898', 164, 41, 5, N'ORDER_CREATED', N'🌸 Đặt hoa thành công!', N'Đơn hoa #41 trị giá 715.000 đ đã được tạo thành công. Tiệm hoa đang tiến hành chuẩn bị.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (1, '2026-10-25 23:51:30.600344', 163, 40, 5, N'REFUND_COMPLETED', N'💰 Hoàn tiền thành công đơn #40', N'Hoàn tiền thành công vào ví của bạn. Không phát sinh chuyển tiền MoMo hoặc ngân hàng thật.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (1, '2026-10-25 23:50:48.11839', 157, 40, 5, N'ORDER_CREATED', N'🌸 Đặt hoa thành công!', N'Đơn hoa #40 trị giá 915.000 đ đã được tạo thành công. Tiệm hoa đang tiến hành chuẩn bị.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (1, '2026-10-25 23:36:02.270743', 151, 39, 5, N'ORDER_CREATED', N'🌸 Đặt hoa thành công!', N'Đơn hoa #39 trị giá 915.000 đ đã được tạo thành công. Tiệm hoa đang tiến hành chuẩn bị.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (1, '2026-10-25 18:30:39.712033', 145, 38, 5, N'ORDER_CANCELLED', N'❌ Đơn hoa #38 đã bị hủy', N'Đơn đã hết thời hạn thanh toán. Tồn kho đã được trả lại chi nhánh.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (1, '2026-10-25 18:19:59.659305', 139, 38, 5, N'ORDER_CREATED', N'🌸 Đặt hoa thành công!', N'Đơn hoa #38 trị giá 675.000 đ đã được tạo thành công. Tiệm hoa đang tiến hành chuẩn bị.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (1, '2026-10-24 18:52:26.424719', 138, 37, 5, N'REFUND_COMPLETED', N'💰 Hoàn tiền thành công đơn #37', N'Hoàn tiền thành công vào ví của bạn. Không phát sinh chuyển tiền MoMo hoặc ngân hàng thật.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (1, '2026-10-24 18:51:45.298747', 135, 37, 5, N'ORDER_CREATED', N'🌸 Đặt hoa thành công!', N'Đơn hoa #37 trị giá 675.000 đ đã được tạo thành công. Tiệm hoa đang tiến hành chuẩn bị.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (1, '2026-10-24 17:22:38.307553', 134, 36, 5, N'REFUND_COMPLETED', N'💰 Hoàn tiền thành công đơn #36', N'Hoàn tiền thành công vào ví demo của bạn. Không phát sinh chuyển tiền MoMo hoặc ngân hàng thật.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (1, '2026-10-24 17:22:07.141088', 128, 36, 5, N'ORDER_CREATED', N'🌸 Đặt hoa thành công!', N'Đơn hoa #36 trị giá 675.000 đ đã được tạo thành công. Tiệm hoa đang tiến hành chuẩn bị.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (1, '2026-10-24 14:36:46.057061', 114, 35, 5, N'ORDER_CREATED', N'🌸 Đặt hoa thành công!', N'Đơn hoa #35 trị giá 675.000 đ đã được tạo thành công. Tiệm hoa đang tiến hành chuẩn bị.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (1, '2026-10-23 23:21:24.151041', 113, 34, 5, N'REFUND_COMPLETED', N'💰 Hoàn tiền thành công đơn #34', N'Hoàn tiền thành công. Giao dịch demo, không phát sinh chuyển tiền MoMo thật.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (1, '2026-10-23 23:20:32.605301', 107, 34, 5, N'ORDER_CREATED', N'🌸 Đặt hoa thành công!', N'Đơn hoa #34 trị giá 675.000 đ đã được tạo thành công. Tiệm hoa đang tiến hành chuẩn bị.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (1, '2026-10-23 22:49:01.954313', 101, 33, 5, N'ORDER_CANCELLED', N'❌ Đơn hoa #33 đã bị hủy', N'Đơn đã hết thời hạn thanh toán. Tồn kho đã được trả lại chi nhánh.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (1, '2026-10-23 22:38:57.900975', 95, 33, 5, N'ORDER_CREATED', N'🌸 Đặt hoa thành công!', N'Đơn hoa #33 trị giá 915.000 đ đã được tạo thành công. Tiệm hoa đang tiến hành chuẩn bị.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (1, '2026-10-23 02:06:48.105639', 94, 32, 5, N'ORDER_CREATED', N'🌸 Đặt hoa thành công!', N'Đơn hoa #32 trị giá 740,000 đ đã được tạo thành công. Tiệm hoa đang tiến hành chuẩn bị.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (1, '2026-10-19 11:13:41.296757', 65, 27, 5, N'ORDER_CREATED', N'🌸 Đặt hoa thành công!', N'Đơn hoa #27 trị giá 744,100 đ đã được tạo thành công. Tiệm hoa đang tiến hành chuẩn bị.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (1, '2026-10-13 15:36:37.109689', 63, 23, 5, N'ORDER_CANCELLED', N'❌ Đơn hoa #23 đã bị hủy', N'Đơn hàng của bạn đã bị hủy bởi admin (quản trị viên) với lý do: không còn hàng. Đơn chưa thanh toán đã được hủy.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (1, '2026-10-13 15:21:22.369659', 62, 23, 5, N'ORDER_CONFIRMED', N'✅ Đơn hoa #23 đã được xác nhận', N'Chi nhánh Chi Nhánh Bến Thành (Quận 1) đã tiếp nhận đơn của bạn.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (1, '2026-10-13 15:20:44.172487', 56, 26, 5, N'ORDER_CANCELLED', N'❌ Đơn hoa #26 đã bị hủy', N'Đơn hàng #26 đã được hủy thành công.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (1, '2026-10-13 15:19:08.546387', 55, 26, 5, N'ORDER_PREPARING', N'🌺 Đang cắm hoa đơn #26', N'Nghệ nhân tiệm hoa đã bắt đầu cắm hoa/chuẩn bị cho bạn. Từ thời điểm này, bạn không thể tự hủy đơn.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (1, '2026-10-13 15:17:28.060963', 48, 26, 5, N'ORDER_CREATED', N'🌸 Đặt hoa thành công!', N'Đơn hoa #26 trị giá 3,275,000 đ đã được tạo thành công. Tiệm hoa đang tiến hành chuẩn bị.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (1, '2026-10-13 15:16:30.242346', 42, 25, 5, N'ORDER_DELIVERED', N'🎉 Giao hoa thành công đơn #25', N'Đơn hoa đã được giao đến nơi! Cảm ơn bạn đã lựa chọn tiệm hoa. Hãy để lại đánh giá cảm nhận nhé.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (1, '2026-10-13 15:16:24.476263', 36, 25, 5, N'ORDER_DELIVERED', N'🎉 Giao hoa thành công đơn #25', N'Đơn hoa đã được giao đến nơi! Cảm ơn bạn đã lựa chọn tiệm hoa. Hãy để lại đánh giá cảm nhận nhé.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (1, '2026-10-13 15:16:19.327715', 35, 25, 5, N'ORDER_DELIVERING', N'🚀 Đơn hoa #25 đang được giao', N'Đơn hoa của bạn đang trên đường vận chuyển. Vui lòng để ý điện thoại nhé!');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (1, '2026-10-13 15:16:02.249587', 34, 25, 5, N'ORDER_PREPARING', N'🌺 Đang cắm hoa đơn #25', N'Nghệ nhân tiệm hoa đã bắt đầu cắm hoa/chuẩn bị cho bạn. Từ thời điểm này, bạn không thể tự hủy đơn.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (1, '2026-10-13 15:02:23.693489', 33, 25, 5, N'ORDER_CONFIRMED', N'✅ Đơn hoa #25 đã được xác nhận', N'Chi nhánh Chi Nhánh Phú Mỹ Hưng (Quận 7) đã tiếp nhận đơn của bạn.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (1, '2026-10-13 15:01:26.378223', 27, 25, 5, N'ORDER_CREATED', N'🌸 Đặt hoa thành công!', N'Đơn hoa #25 trị giá 34,475,000 đ đã được tạo thành công. Tiệm hoa đang tiến hành chuẩn bị.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (1, '2026-10-13 09:12:22.511207', 21, 24, 5, N'ORDER_CREATED', N'🌸 Đặt hoa thành công!', N'Đơn hoa #24 trị giá 34,425,000 đ đã được tạo thành công. Tiệm hoa đang tiến hành chuẩn bị.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (1, '2026-10-13 08:53:24.624577', 19, 23, 5, N'ORDER_CREATED', N'🌸 Đặt hoa thành công!', N'Đơn hoa #23 trị giá 34,500,000 đ đã được tạo thành công. Tiệm hoa đang tiến hành chuẩn bị.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (1, '2026-10-13 07:56:12.99071', 18, 22, 5, N'ORDER_PREPARING', N'🌺 Đang cắm hoa đơn #22', N'Nghệ nhân tiệm hoa đang tuyển chọn hoa tươi mới và tiến hành cắm hoa cho bạn.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (1, '2026-10-13 07:55:01.979525', 17, 22, 5, N'ORDER_CONFIRMED', N'✅ Đơn hoa #22 đã được xác nhận', N'Chi nhánh Chi Nhánh Bến Thành (Quận 1) đã tiếp nhận đơn của bạn.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (1, '2026-10-13 07:51:00.352716', 15, 22, 5, N'ORDER_CREATED', N'🌸 Đặt hoa thành công!', N'Đơn hoa #22 trị giá 34,500,000 đ đã được tạo thành công. Tiệm hoa đang tiến hành chuẩn bị.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (1, '2026-10-07 16:49:26.7192', 9, 21, 5, N'ORDER_CREATED', N'🌸 Đặt hoa thành công!', N'Đơn hoa #21 trị giá 39,000,000 đ đã được tạo thành công. Tiệm hoa đang tiến hành chuẩn bị.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (1, '2026-10-07 16:48:41.762857', 7, 20, 5, N'ORDER_CREATED', N'🌸 Đặt hoa thành công!', N'Đơn hoa #20 trị giá 39,000,000 đ đã được tạo thành công. Tiệm hoa đang tiến hành chuẩn bị.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (1, '2026-10-07 01:07:53.351472', 4, 18, 5, N'PAYMENT_SUCCESS', N'💳 Đã nhận tiền chuyển khoản!', N'Chi nhánh đã xác nhận nhận đủ tiền chuyển khoản cho đơn hoa #18.');
INSERT INTO dbo.[notifications] ([is_read], [created_at], [id], [related_order_id], [user_id], [type], [title], [content]) VALUES (1, '2026-10-07 01:07:15.639689', 3, 18, 5, N'ORDER_CONFIRMED', N'✅ Đơn hoa #18 đã được xác nhận', N'Chi nhánh Chi Nhánh Bến Thành (Quận 1) đã tiếp nhận đơn của bạn.');
SET IDENTITY_INSERT dbo.[notifications] OFF;
GO

-- Dữ liệu bảng: order_details
SET IDENTITY_INSERT dbo.[order_details] ON;
INSERT INTO dbo.[order_details] ([actual_return_date], [deposit_fee], [quantity], [rent_end_date], [rent_start_date], [unit_price], [id], [order_id], [product_id], [order_type]) VALUES (NULL, 0.00, 1, NULL, NULL, 650000.00, 1, 1, 1, N'BUY');
INSERT INTO dbo.[order_details] ([actual_return_date], [deposit_fee], [quantity], [rent_end_date], [rent_start_date], [unit_price], [id], [order_id], [product_id], [order_type]) VALUES (NULL, NULL, 1, NULL, NULL, 650000.00, 2, 2, 21, N'BUY');
INSERT INTO dbo.[order_details] ([actual_return_date], [deposit_fee], [quantity], [rent_end_date], [rent_start_date], [unit_price], [id], [order_id], [product_id], [order_type]) VALUES (NULL, NULL, 1, NULL, NULL, 950000.00, 3, 3, 23, N'BUY');
INSERT INTO dbo.[order_details] ([actual_return_date], [deposit_fee], [quantity], [rent_end_date], [rent_start_date], [unit_price], [id], [order_id], [product_id], [order_type]) VALUES (NULL, NULL, 1, NULL, NULL, 720000.00, 4, 4, 24, N'BUY');
INSERT INTO dbo.[order_details] ([actual_return_date], [deposit_fee], [quantity], [rent_end_date], [rent_start_date], [unit_price], [id], [order_id], [product_id], [order_type]) VALUES (NULL, NULL, 1, NULL, NULL, 890000.00, 5, 5, 22, N'BUY');
INSERT INTO dbo.[order_details] ([actual_return_date], [deposit_fee], [quantity], [rent_end_date], [rent_start_date], [unit_price], [id], [order_id], [product_id], [order_type]) VALUES (NULL, NULL, 1, NULL, NULL, 1950000.00, 6, 6, 25, N'BUY');
INSERT INTO dbo.[order_details] ([actual_return_date], [deposit_fee], [quantity], [rent_end_date], [rent_start_date], [unit_price], [id], [order_id], [product_id], [order_type]) VALUES (NULL, NULL, 1, NULL, NULL, 690000.00, 7, 7, 27, N'BUY');
INSERT INTO dbo.[order_details] ([actual_return_date], [deposit_fee], [quantity], [rent_end_date], [rent_start_date], [unit_price], [id], [order_id], [product_id], [order_type]) VALUES (NULL, NULL, 1, NULL, NULL, 2300000.00, 8, 8, 26, N'BUY');
INSERT INTO dbo.[order_details] ([actual_return_date], [deposit_fee], [quantity], [rent_end_date], [rent_start_date], [unit_price], [id], [order_id], [product_id], [order_type]) VALUES (NULL, NULL, 1, NULL, NULL, 650000.00, 9, 9, 21, N'BUY');
INSERT INTO dbo.[order_details] ([actual_return_date], [deposit_fee], [quantity], [rent_end_date], [rent_start_date], [unit_price], [id], [order_id], [product_id], [order_type]) VALUES (NULL, NULL, 1, NULL, NULL, 950000.00, 10, 9, 23, N'BUY');
INSERT INTO dbo.[order_details] ([actual_return_date], [deposit_fee], [quantity], [rent_end_date], [rent_start_date], [unit_price], [id], [order_id], [product_id], [order_type]) VALUES (NULL, NULL, 1, NULL, NULL, 720000.00, 11, 10, 24, N'BUY');
INSERT INTO dbo.[order_details] ([actual_return_date], [deposit_fee], [quantity], [rent_end_date], [rent_start_date], [unit_price], [id], [order_id], [product_id], [order_type]) VALUES (NULL, NULL, 1, NULL, NULL, 890000.00, 12, 11, 22, N'BUY');
INSERT INTO dbo.[order_details] ([actual_return_date], [deposit_fee], [quantity], [rent_end_date], [rent_start_date], [unit_price], [id], [order_id], [product_id], [order_type]) VALUES (NULL, NULL, 1, NULL, NULL, 1950000.00, 13, 12, 25, N'BUY');
INSERT INTO dbo.[order_details] ([actual_return_date], [deposit_fee], [quantity], [rent_end_date], [rent_start_date], [unit_price], [id], [order_id], [product_id], [order_type]) VALUES (NULL, NULL, 1, NULL, NULL, 2300000.00, 14, 13, 26, N'BUY');
INSERT INTO dbo.[order_details] ([actual_return_date], [deposit_fee], [quantity], [rent_end_date], [rent_start_date], [unit_price], [id], [order_id], [product_id], [order_type]) VALUES (NULL, NULL, 1, NULL, NULL, 650000.00, 15, 14, 21, N'BUY');
INSERT INTO dbo.[order_details] ([actual_return_date], [deposit_fee], [quantity], [rent_end_date], [rent_start_date], [unit_price], [id], [order_id], [product_id], [order_type]) VALUES (NULL, NULL, 1, NULL, NULL, 690000.00, 16, 15, 27, N'BUY');
INSERT INTO dbo.[order_details] ([actual_return_date], [deposit_fee], [quantity], [rent_end_date], [rent_start_date], [unit_price], [id], [order_id], [product_id], [order_type]) VALUES (NULL, NULL, 1, NULL, NULL, 890000.00, 17, 16, 22, N'BUY');
INSERT INTO dbo.[order_details] ([actual_return_date], [deposit_fee], [quantity], [rent_end_date], [rent_start_date], [unit_price], [id], [order_id], [product_id], [order_type]) VALUES (NULL, NULL, 1, NULL, NULL, 1950000.00, 18, 17, 25, N'BUY');
INSERT INTO dbo.[order_details] ([actual_return_date], [deposit_fee], [quantity], [rent_end_date], [rent_start_date], [unit_price], [id], [order_id], [product_id], [order_type]) VALUES (NULL, 0.00, 1, NULL, NULL, 1200000.00, 19, 18, 12, N'BUY');
INSERT INTO dbo.[order_details] ([actual_return_date], [deposit_fee], [quantity], [rent_end_date], [rent_start_date], [unit_price], [id], [order_id], [product_id], [order_type]) VALUES (NULL, 0.00, 1, NULL, NULL, 650000.00, 20, 18, 21, N'BUY');
INSERT INTO dbo.[order_details] ([actual_return_date], [deposit_fee], [quantity], [rent_end_date], [rent_start_date], [unit_price], [id], [order_id], [product_id], [order_type]) VALUES (NULL, 0.00, 1, NULL, NULL, 890000.00, 21, 19, 22, N'BUY');
INSERT INTO dbo.[order_details] ([actual_return_date], [deposit_fee], [quantity], [rent_end_date], [rent_start_date], [unit_price], [id], [order_id], [product_id], [order_type]) VALUES (NULL, 0.00, 20, NULL, NULL, 1950000.00, 22, 20, 25, N'BUY');
INSERT INTO dbo.[order_details] ([actual_return_date], [deposit_fee], [quantity], [rent_end_date], [rent_start_date], [unit_price], [id], [order_id], [product_id], [order_type]) VALUES (NULL, 0.00, 20, NULL, NULL, 1950000.00, 23, 21, 25, N'BUY');
INSERT INTO dbo.[order_details] ([actual_return_date], [deposit_fee], [quantity], [rent_end_date], [rent_start_date], [unit_price], [id], [order_id], [product_id], [order_type]) VALUES (NULL, 0.00, 15, NULL, NULL, 2300000.00, 24, 22, 26, N'BUY');
INSERT INTO dbo.[order_details] ([actual_return_date], [deposit_fee], [quantity], [rent_end_date], [rent_start_date], [unit_price], [id], [order_id], [product_id], [order_type]) VALUES (NULL, 0.00, 15, NULL, NULL, 2300000.00, 25, 23, 26, N'BUY');
INSERT INTO dbo.[order_details] ([actual_return_date], [deposit_fee], [quantity], [rent_end_date], [rent_start_date], [unit_price], [id], [order_id], [product_id], [order_type]) VALUES (NULL, 0.00, 15, NULL, NULL, 2300000.00, 26, 24, 26, N'BUY');
INSERT INTO dbo.[order_details] ([actual_return_date], [deposit_fee], [quantity], [rent_end_date], [rent_start_date], [unit_price], [id], [order_id], [product_id], [order_type]) VALUES (NULL, 0.00, 15, NULL, NULL, 2300000.00, 27, 25, 26, N'BUY');
INSERT INTO dbo.[order_details] ([actual_return_date], [deposit_fee], [quantity], [rent_end_date], [rent_start_date], [unit_price], [id], [order_id], [product_id], [order_type]) VALUES (NULL, 0.00, 5, NULL, NULL, 650000.00, 28, 26, 21, N'BUY');
INSERT INTO dbo.[order_details] ([actual_return_date], [deposit_fee], [quantity], [rent_end_date], [rent_start_date], [unit_price], [id], [order_id], [product_id], [order_type]) VALUES (NULL, 0.00, 1, NULL, NULL, 799000.00, 29, 27, 40, N'BUY');
INSERT INTO dbo.[order_details] ([actual_return_date], [deposit_fee], [quantity], [rent_end_date], [rent_start_date], [unit_price], [id], [order_id], [product_id], [order_type]) VALUES (NULL, 0.00, 1, NULL, NULL, 950000.00, 30, 28, 23, N'BUY');
INSERT INTO dbo.[order_details] ([actual_return_date], [deposit_fee], [quantity], [rent_end_date], [rent_start_date], [unit_price], [id], [order_id], [product_id], [order_type]) VALUES (NULL, 0.00, 1, NULL, NULL, 650000.00, 31, 29, 21, N'BUY');
INSERT INTO dbo.[order_details] ([actual_return_date], [deposit_fee], [quantity], [rent_end_date], [rent_start_date], [unit_price], [id], [order_id], [product_id], [order_type]) VALUES (NULL, 0.00, 1, NULL, NULL, 390000.00, 32, 30, 31, N'BUY');
INSERT INTO dbo.[order_details] ([actual_return_date], [deposit_fee], [quantity], [rent_end_date], [rent_start_date], [unit_price], [id], [order_id], [product_id], [order_type]) VALUES (NULL, 0.00, 1, NULL, NULL, 950000.00, 33, 31, 23, N'BUY');
INSERT INTO dbo.[order_details] ([actual_return_date], [deposit_fee], [quantity], [rent_end_date], [rent_start_date], [unit_price], [id], [order_id], [product_id], [order_type]) VALUES (NULL, 0.00, 2, NULL, NULL, 350000.00, 34, 32, 32, N'BUY');
INSERT INTO dbo.[order_details] ([actual_return_date], [deposit_fee], [quantity], [rent_end_date], [rent_start_date], [unit_price], [id], [order_id], [product_id], [order_type]) VALUES (NULL, 0.00, 1, NULL, NULL, 890000.00, 35, 33, 22, N'BUY');
INSERT INTO dbo.[order_details] ([actual_return_date], [deposit_fee], [quantity], [rent_end_date], [rent_start_date], [unit_price], [id], [order_id], [product_id], [order_type]) VALUES (NULL, 0.00, 1, NULL, NULL, 650000.00, 36, 34, 21, N'BUY');
INSERT INTO dbo.[order_details] ([actual_return_date], [deposit_fee], [quantity], [rent_end_date], [rent_start_date], [unit_price], [id], [order_id], [product_id], [order_type]) VALUES (NULL, 0.00, 1, NULL, NULL, 650000.00, 37, 35, 21, N'BUY');
INSERT INTO dbo.[order_details] ([actual_return_date], [deposit_fee], [quantity], [rent_end_date], [rent_start_date], [unit_price], [id], [order_id], [product_id], [order_type]) VALUES (NULL, 0.00, 1, NULL, NULL, 650000.00, 38, 36, 21, N'BUY');
INSERT INTO dbo.[order_details] ([actual_return_date], [deposit_fee], [quantity], [rent_end_date], [rent_start_date], [unit_price], [id], [order_id], [product_id], [order_type]) VALUES (NULL, 0.00, 1, NULL, NULL, 650000.00, 39, 37, 1, N'BUY');
INSERT INTO dbo.[order_details] ([actual_return_date], [deposit_fee], [quantity], [rent_end_date], [rent_start_date], [unit_price], [id], [order_id], [product_id], [order_type]) VALUES (NULL, 0.00, 1, NULL, NULL, 650000.00, 40, 38, 21, N'BUY');
INSERT INTO dbo.[order_details] ([actual_return_date], [deposit_fee], [quantity], [rent_end_date], [rent_start_date], [unit_price], [id], [order_id], [product_id], [order_type]) VALUES (NULL, 0.00, 1, NULL, NULL, 890000.00, 41, 39, 22, N'BUY');
INSERT INTO dbo.[order_details] ([actual_return_date], [deposit_fee], [quantity], [rent_end_date], [rent_start_date], [unit_price], [id], [order_id], [product_id], [order_type]) VALUES (NULL, 0.00, 1, NULL, NULL, 890000.00, 42, 40, 22, N'BUY');
INSERT INTO dbo.[order_details] ([actual_return_date], [deposit_fee], [quantity], [rent_end_date], [rent_start_date], [unit_price], [id], [order_id], [product_id], [order_type]) VALUES (NULL, 0.00, 1, NULL, NULL, 690000.00, 43, 41, 27, N'BUY');
INSERT INTO dbo.[order_details] ([actual_return_date], [deposit_fee], [quantity], [rent_end_date], [rent_start_date], [unit_price], [id], [order_id], [product_id], [order_type]) VALUES (NULL, 0.00, 1, NULL, NULL, 520000.00, 44, 42, 28, N'BUY');
INSERT INTO dbo.[order_details] ([actual_return_date], [deposit_fee], [quantity], [rent_end_date], [rent_start_date], [unit_price], [id], [order_id], [product_id], [order_type]) VALUES (NULL, 0.00, 1, NULL, NULL, 890000.00, 45, 43, 22, N'BUY');
INSERT INTO dbo.[order_details] ([actual_return_date], [deposit_fee], [quantity], [rent_end_date], [rent_start_date], [unit_price], [id], [order_id], [product_id], [order_type]) VALUES (NULL, 0.00, 1, NULL, NULL, 650000.00, 46, 44, 1, N'BUY');
INSERT INTO dbo.[order_details] ([actual_return_date], [deposit_fee], [quantity], [rent_end_date], [rent_start_date], [unit_price], [id], [order_id], [product_id], [order_type]) VALUES (NULL, 0.00, 1, NULL, NULL, 690000.00, 47, 45, 27, N'BUY');
INSERT INTO dbo.[order_details] ([actual_return_date], [deposit_fee], [quantity], [rent_end_date], [rent_start_date], [unit_price], [id], [order_id], [product_id], [order_type]) VALUES (NULL, 0.00, 1, NULL, NULL, 890000.00, 48, 46, 22, N'BUY');
SET IDENTITY_INSERT dbo.[order_details] OFF;
GO

-- Dữ liệu bảng: orders
SET IDENTITY_INSERT dbo.[orders] ON;
INSERT INTO dbo.[orders] ([commission_rate_snapshot], [delivery_date], [delivery_time], [discount_amount], [fine_amount], [funds_amount], [payment_expired], [shipping_fee], [total_amount], [total_deposit], [created_at], [delivery_unit_id], [id], [payment_expires_at], [pickup_store_id], [return_store_id], [shipper_id], [user_id], [version], [voucher_id], [order_type], [funds_status], [payment_method], [payment_status], [recipient_phone], [shipping_method], [status], [recipient_name], [cancellation_reason], [greeting_message], [recipient_address]) VALUES (NULL, '2026-09-25', NULL, 0.00, NULL, NULL, NULL, NULL, 650000.00, 0.00, '2026-09-22 14:46:55.015813', NULL, 1, NULL, 1, NULL, NULL, 5, 3, NULL, N'BUY', N'NONE', N'COD', N'UNPAID', N'0988776655', N'DELIVERY', N'DELIVERING', N'Nguyen Thi Hoa', NULL, N'Mung sinh nhat nguoi thuong, mai mai tuoi dep nhu doa hoa!', N'72 Le Thanh Ton, Quan 1, TP.HCM');
INSERT INTO dbo.[orders] ([commission_rate_snapshot], [delivery_date], [delivery_time], [discount_amount], [fine_amount], [funds_amount], [payment_expired], [shipping_fee], [total_amount], [total_deposit], [created_at], [delivery_unit_id], [id], [payment_expires_at], [pickup_store_id], [return_store_id], [shipper_id], [user_id], [version], [voucher_id], [order_type], [funds_status], [payment_method], [payment_status], [recipient_phone], [shipping_method], [status], [recipient_name], [cancellation_reason], [greeting_message], [recipient_address]) VALUES (NULL, '2026-10-07', NULL, 0.00, NULL, NULL, NULL, 25000.00, 675000.00, NULL, '2026-10-07 00:44:11.232454', 1, 2, NULL, 2, NULL, NULL, 33, 0, NULL, N'BUY', N'NONE', N'COD', N'UNPAID', N'0903112233', N'DELIVERY', N'PENDING', N'Chị Ngọc Mai', NULL, N'Chúc chị yêu sinh nhật vui vẻ, luôn trẻ đẹp và thành công trong công việc!', N'Chung cư Scenic Valley 1, Phú Mỹ Hưng, Quận 7, TP.HCM');
INSERT INTO dbo.[orders] ([commission_rate_snapshot], [delivery_date], [delivery_time], [discount_amount], [fine_amount], [funds_amount], [payment_expired], [shipping_fee], [total_amount], [total_deposit], [created_at], [delivery_unit_id], [id], [payment_expires_at], [pickup_store_id], [return_store_id], [shipper_id], [user_id], [version], [voucher_id], [order_type], [funds_status], [payment_method], [payment_status], [recipient_phone], [shipping_method], [status], [recipient_name], [cancellation_reason], [greeting_message], [recipient_address]) VALUES (NULL, '2026-10-07', NULL, 0.00, NULL, NULL, NULL, 25000.00, 975000.00, NULL, '2026-10-07 00:20:11.232454', 1, 3, NULL, 2, NULL, NULL, 5, 0, NULL, N'BUY', N'NONE', N'VNPAY', N'PAID', N'0912445566', N'DELIVERY', N'PENDING', N'Cô giáo Thu Hà', NULL, N'Kính chúc cô giáo luôn dồi dào sức khỏe, hạnh phúc và tràn đầy nhiệt huyết!', N'Trường Quốc Tế Nam Sài Gòn (SSIS), Nguyễn Đức Cảnh, Tân Phong, Quận 7');
INSERT INTO dbo.[orders] ([commission_rate_snapshot], [delivery_date], [delivery_time], [discount_amount], [fine_amount], [funds_amount], [payment_expired], [shipping_fee], [total_amount], [total_deposit], [created_at], [delivery_unit_id], [id], [payment_expires_at], [pickup_store_id], [return_store_id], [shipper_id], [user_id], [version], [voucher_id], [order_type], [funds_status], [payment_method], [payment_status], [recipient_phone], [shipping_method], [status], [recipient_name], [cancellation_reason], [greeting_message], [recipient_address]) VALUES (NULL, '2026-10-06', NULL, 0.00, NULL, NULL, NULL, 45000.00, 765000.00, NULL, '2026-10-06 22:52:11.232454', 1, 4, NULL, 2, NULL, NULL, 34, 0, NULL, N'BUY', N'NONE', N'VNPAY', N'PAID', N'0909887766', N'DELIVERY', N'CONFIRMED', N'Anh Tuấn Giám Đốc', NULL, N'Công ty Nam Sài Gòn chúc mừng sinh nhật Sếp Tuấn, chúc Sếp vạn sự hanh thông!', N'Tòa nhà Crescent Plaza, 105 Tôn Dật Tiên, Tân Phú, Quận 7');
INSERT INTO dbo.[orders] ([commission_rate_snapshot], [delivery_date], [delivery_time], [discount_amount], [fine_amount], [funds_amount], [payment_expired], [shipping_fee], [total_amount], [total_deposit], [created_at], [delivery_unit_id], [id], [payment_expires_at], [pickup_store_id], [return_store_id], [shipper_id], [user_id], [version], [voucher_id], [order_type], [funds_status], [payment_method], [payment_status], [recipient_phone], [shipping_method], [status], [recipient_name], [cancellation_reason], [greeting_message], [recipient_address]) VALUES (NULL, '2026-10-06', NULL, 0.00, NULL, NULL, NULL, 45000.00, 935000.00, NULL, '2026-10-06 21:37:11.232454', 1, 5, NULL, 2, NULL, NULL, 33, 0, NULL, N'BUY', N'NONE', N'MOMO', N'PAID', N'0934556677', N'DELIVERY', N'CONFIRMED', N'Cô dâu Phương Uyên', NULL, N'Trăm năm hạnh phúc, mãi mãi ngọt ngào bên nhau nhé Uyên!', N'Trung tâm hội nghị tiệc cưới Crystal Palace, Nguyễn Lương Bằng, Quận 7');
INSERT INTO dbo.[orders] ([commission_rate_snapshot], [delivery_date], [delivery_time], [discount_amount], [fine_amount], [funds_amount], [payment_expired], [shipping_fee], [total_amount], [total_deposit], [created_at], [delivery_unit_id], [id], [payment_expires_at], [pickup_store_id], [return_store_id], [shipper_id], [user_id], [version], [voucher_id], [order_type], [funds_status], [payment_method], [payment_status], [recipient_phone], [shipping_method], [status], [recipient_name], [cancellation_reason], [greeting_message], [recipient_address]) VALUES (NULL, '2026-10-06', NULL, 0.00, NULL, NULL, NULL, 50000.00, 2000000.00, NULL, '2026-10-06 20:47:11.232454', 1, 6, NULL, 2, NULL, NULL, 34, 1, NULL, N'BUY', N'NONE', N'VNPAY', N'PAID', N'02854112233', N'DELIVERY', N'DELIVERING', N'Showroom Audi Phú Mỹ Hưng', NULL, N'Chúc mừng khai trương đại hồng phát, tấn tài tấn lộc!', N'Tân Trào, Tân Phú, Quận 7, TP.HCM');
INSERT INTO dbo.[orders] ([commission_rate_snapshot], [delivery_date], [delivery_time], [discount_amount], [fine_amount], [funds_amount], [payment_expired], [shipping_fee], [total_amount], [total_deposit], [created_at], [delivery_unit_id], [id], [payment_expires_at], [pickup_store_id], [return_store_id], [shipper_id], [user_id], [version], [voucher_id], [order_type], [funds_status], [payment_method], [payment_status], [recipient_phone], [shipping_method], [status], [recipient_name], [cancellation_reason], [greeting_message], [recipient_address]) VALUES (NULL, '2026-10-06', NULL, 0.00, NULL, NULL, NULL, 25000.00, 715000.00, NULL, '2026-10-06 19:32:11.232454', 1, 7, NULL, 2, NULL, NULL, 5, 0, NULL, N'BUY', N'NONE', N'VNPAY', N'PAID', N'0902334455', N'DELIVERY', N'DELIVERING', N'Mẹ Hoàng Oanh', NULL, N'Con chúc Mẹ yêu luôn mạnh khỏe, bình an và hạnh phúc bên con cháu!', N'128 Đường số 79, Phường Tân Quy, Quận 7, TP.HCM');
INSERT INTO dbo.[orders] ([commission_rate_snapshot], [delivery_date], [delivery_time], [discount_amount], [fine_amount], [funds_amount], [payment_expired], [shipping_fee], [total_amount], [total_deposit], [created_at], [delivery_unit_id], [id], [payment_expires_at], [pickup_store_id], [return_store_id], [shipper_id], [user_id], [version], [voucher_id], [order_type], [funds_status], [payment_method], [payment_status], [recipient_phone], [shipping_method], [status], [recipient_name], [cancellation_reason], [greeting_message], [recipient_address]) VALUES (NULL, '2026-10-06', NULL, 0.00, NULL, NULL, NULL, 50000.00, 2350000.00, NULL, '2026-10-06 17:47:11.232454', 1, 8, NULL, 2, NULL, NULL, 34, 0, NULL, N'BUY', N'NONE', N'VNPAY', N'PAID', N'02854138899', N'DELIVERY', N'DELIVERED', N'Ngân Hàng Shinhan Bank - Chi Nhánh PMH', NULL, N'Kính chúc đối tác Shinhan Bank luôn thịnh vượng và ngày càng phát triển vượt bậc!', N'Nguyễn Khắc Viện, Tân Phú, Quận 7, TP.HCM');
INSERT INTO dbo.[orders] ([commission_rate_snapshot], [delivery_date], [delivery_time], [discount_amount], [fine_amount], [funds_amount], [payment_expired], [shipping_fee], [total_amount], [total_deposit], [created_at], [delivery_unit_id], [id], [payment_expires_at], [pickup_store_id], [return_store_id], [shipper_id], [user_id], [version], [voucher_id], [order_type], [funds_status], [payment_method], [payment_status], [recipient_phone], [shipping_method], [status], [recipient_name], [cancellation_reason], [greeting_message], [recipient_address]) VALUES (NULL, '2026-10-06', NULL, 0.00, NULL, NULL, NULL, 25000.00, 1625000.00, NULL, '2026-10-06 15:32:11.232454', 1, 9, NULL, 2, NULL, NULL, 33, 0, NULL, N'BUY', N'NONE', N'MOMO', N'PAID', N'0909112233', N'DELIVERY', N'DELIVERED', N'Em gái Trâm Anh', NULL, N'Chúc mừng em gái hoàn thành xuất sắc đồ án tốt nghiệp cử nhân!', N'Riverside Residence, Nguyễn Lương Bằng, Phú Mỹ Hưng, Quận 7');
INSERT INTO dbo.[orders] ([commission_rate_snapshot], [delivery_date], [delivery_time], [discount_amount], [fine_amount], [funds_amount], [payment_expired], [shipping_fee], [total_amount], [total_deposit], [created_at], [delivery_unit_id], [id], [payment_expires_at], [pickup_store_id], [return_store_id], [shipper_id], [user_id], [version], [voucher_id], [order_type], [funds_status], [payment_method], [payment_status], [recipient_phone], [shipping_method], [status], [recipient_name], [cancellation_reason], [greeting_message], [recipient_address]) VALUES (NULL, '2026-10-06', NULL, 0.00, NULL, NULL, NULL, 25000.00, 745000.00, NULL, '2026-10-06 15:30:11.232454', 1, 10, NULL, 2, NULL, NULL, 5, 0, NULL, N'BUY', N'NONE', N'VNPAY', N'PAID', N'0933445566', N'DELIVERY', N'DELIVERED', N'Bạn Thảo My', NULL, N'Chúc mừng tân gia nhà mới ngập tràn niềm vui và may mắn!', N'Chung cư M-One Nam Sài Gòn, Bế Văn Cấm, Tân Kiểng, Quận 7');
INSERT INTO dbo.[orders] ([commission_rate_snapshot], [delivery_date], [delivery_time], [discount_amount], [fine_amount], [funds_amount], [payment_expired], [shipping_fee], [total_amount], [total_deposit], [created_at], [delivery_unit_id], [id], [payment_expires_at], [pickup_store_id], [return_store_id], [shipper_id], [user_id], [version], [voucher_id], [order_type], [funds_status], [payment_method], [payment_status], [recipient_phone], [shipping_method], [status], [recipient_name], [cancellation_reason], [greeting_message], [recipient_address]) VALUES (NULL, '2026-10-06', NULL, 0.00, NULL, NULL, NULL, 25000.00, 915000.00, NULL, '2026-10-06 10:15:11.232454', 1, 11, NULL, 2, NULL, NULL, 34, 1, NULL, N'BUY', N'NONE', N'VNPAY', N'PAID', N'0908223344', N'DELIVERY', N'COMPLETED', N'Bác Sĩ Trưởng Khoa', NULL, N'Gia đình bệnh nhân xin chân thành cảm ơn y bác sĩ đã tận tình cứu chữa!', N'Bệnh viện FV (Pháp Việt), 6 Nguyễn Lương Bằng, Tân Phú, Quận 7');
INSERT INTO dbo.[orders] ([commission_rate_snapshot], [delivery_date], [delivery_time], [discount_amount], [fine_amount], [funds_amount], [payment_expired], [shipping_fee], [total_amount], [total_deposit], [created_at], [delivery_unit_id], [id], [payment_expires_at], [pickup_store_id], [return_store_id], [shipper_id], [user_id], [version], [voucher_id], [order_type], [funds_status], [payment_method], [payment_status], [recipient_phone], [shipping_method], [status], [recipient_name], [cancellation_reason], [greeting_message], [recipient_address]) VALUES (NULL, '2026-10-05', NULL, 0.00, NULL, NULL, NULL, 50000.00, 2000000.00, NULL, '2026-10-05 14:20:11.232454', 1, 12, NULL, 2, NULL, NULL, 34, 0, NULL, N'BUY', N'NONE', N'VNPAY', N'PAID', N'02837755666', N'DELIVERY', N'COMPLETED', N'Công Ty Bất Động Sản Hưng Phát', NULL, N'Chúc mừng khai trương trụ sở mới, làm ăn phát đạt, tiền vào như nước!', N'Huỳnh Tấn Phát, Phú Mỹ, Quận 7, TP.HCM');
INSERT INTO dbo.[orders] ([commission_rate_snapshot], [delivery_date], [delivery_time], [discount_amount], [fine_amount], [funds_amount], [payment_expired], [shipping_fee], [total_amount], [total_deposit], [created_at], [delivery_unit_id], [id], [payment_expires_at], [pickup_store_id], [return_store_id], [shipper_id], [user_id], [version], [voucher_id], [order_type], [funds_status], [payment_method], [payment_status], [recipient_phone], [shipping_method], [status], [recipient_name], [cancellation_reason], [greeting_message], [recipient_address]) VALUES (NULL, '2026-10-04', NULL, 0.00, NULL, NULL, NULL, 25000.00, 2325000.00, NULL, '2026-10-04 11:45:11.232454', 1, 13, NULL, 2, NULL, NULL, 33, 0, NULL, N'BUY', N'NONE', N'MOMO', N'PAID', N'0903556677', N'DELIVERY', N'COMPLETED', N'Bà Nội', NULL, N'Kính chúc Bà thượng thọ an khang, sống lâu trăm tuổi cùng con cháu!', N'Biệt thự Mỹ Gia, Phú Mỹ Hưng, Quận 7');
INSERT INTO dbo.[orders] ([commission_rate_snapshot], [delivery_date], [delivery_time], [discount_amount], [fine_amount], [funds_amount], [payment_expired], [shipping_fee], [total_amount], [total_deposit], [created_at], [delivery_unit_id], [id], [payment_expires_at], [pickup_store_id], [return_store_id], [shipper_id], [user_id], [version], [voucher_id], [order_type], [funds_status], [payment_method], [payment_status], [recipient_phone], [shipping_method], [status], [recipient_name], [cancellation_reason], [greeting_message], [recipient_address]) VALUES (NULL, '2026-10-03', NULL, 0.00, NULL, NULL, NULL, 25000.00, 675000.00, NULL, '2026-10-03 16:10:11.232454', 1, 14, NULL, 2, NULL, NULL, 5, 0, NULL, N'BUY', N'NONE', N'VNPAY', N'PAID', N'0908889900', N'DELIVERY', N'COMPLETED', N'Bạn gái Diễm Hương', NULL, N'Kỷ niệm 2 năm bên nhau, cảm ơn em đã luôn là hậu phương ngọt ngào của anh!', N'Sky Garden 3, Phú Mỹ Hưng, Quận 7, TP.HCM');
INSERT INTO dbo.[orders] ([commission_rate_snapshot], [delivery_date], [delivery_time], [discount_amount], [fine_amount], [funds_amount], [payment_expired], [shipping_fee], [total_amount], [total_deposit], [created_at], [delivery_unit_id], [id], [payment_expires_at], [pickup_store_id], [return_store_id], [shipper_id], [user_id], [version], [voucher_id], [order_type], [funds_status], [payment_method], [payment_status], [recipient_phone], [shipping_method], [status], [recipient_name], [cancellation_reason], [greeting_message], [recipient_address]) VALUES (NULL, '2026-10-02', NULL, 0.00, NULL, NULL, NULL, 25000.00, 715000.00, NULL, '2026-10-02 09:30:11.232454', 1, 15, NULL, 2, NULL, NULL, 33, 0, NULL, N'BUY', N'NONE', N'VNPAY', N'PAID', N'0918223344', N'DELIVERY', N'COMPLETED', N'Chị Thảo Nguyên', NULL, N'Chúc chị sinh nhật vui vẻ, gặt hái thêm nhiều thành công mới trong cuộc sống!', N'Star Hill, Tân Phú, Quận 7, TP.HCM');
INSERT INTO dbo.[orders] ([commission_rate_snapshot], [delivery_date], [delivery_time], [discount_amount], [fine_amount], [funds_amount], [payment_expired], [shipping_fee], [total_amount], [total_deposit], [created_at], [delivery_unit_id], [id], [payment_expires_at], [pickup_store_id], [return_store_id], [shipper_id], [user_id], [version], [voucher_id], [order_type], [funds_status], [payment_method], [payment_status], [recipient_phone], [shipping_method], [status], [recipient_name], [cancellation_reason], [greeting_message], [recipient_address]) VALUES (NULL, '2026-10-01', NULL, 0.00, NULL, NULL, NULL, 25000.00, 915000.00, NULL, '2026-10-01 13:15:11.232454', 1, 16, NULL, 2, NULL, NULL, 34, 0, NULL, N'BUY', N'NONE', N'VNPAY', N'PAID', N'02854118899', N'DELIVERY', N'COMPLETED', N'Khách Sạn MerPerle Crystal Palace', NULL, N'Chúc mừng sự kiện Gala Dinner đối tác thành công mỹ mãn!', N'Nguyễn Lương Bằng, Quận 7, TP.HCM');
INSERT INTO dbo.[orders] ([commission_rate_snapshot], [delivery_date], [delivery_time], [discount_amount], [fine_amount], [funds_amount], [payment_expired], [shipping_fee], [total_amount], [total_deposit], [created_at], [delivery_unit_id], [id], [payment_expires_at], [pickup_store_id], [return_store_id], [shipper_id], [user_id], [version], [voucher_id], [order_type], [funds_status], [payment_method], [payment_status], [recipient_phone], [shipping_method], [status], [recipient_name], [cancellation_reason], [greeting_message], [recipient_address]) VALUES (NULL, '2026-10-04', NULL, 0.00, NULL, NULL, NULL, 25000.00, 1975000.00, NULL, '2026-10-04 08:10:11.232454', 1, 17, NULL, 2, NULL, NULL, 5, 0, NULL, N'BUY', N'NONE', N'COD', N'UNPAID', N'0909123456', N'DELIVERY', N'CANCELLED', N'Anh Hoàng', N'Khách hàng thông báo hủy do hoãn sự kiện khai trương sang tuần sau.', N'Chúc mừng khai trương hồng phát!', N'Tân Mỹ, Tân Phú, Quận 7, TP.HCM');
INSERT INTO dbo.[orders] ([commission_rate_snapshot], [delivery_date], [delivery_time], [discount_amount], [fine_amount], [funds_amount], [payment_expired], [shipping_fee], [total_amount], [total_deposit], [created_at], [delivery_unit_id], [id], [payment_expires_at], [pickup_store_id], [return_store_id], [shipper_id], [user_id], [version], [voucher_id], [order_type], [funds_status], [payment_method], [payment_status], [recipient_phone], [shipping_method], [status], [recipient_name], [cancellation_reason], [greeting_message], [recipient_address]) VALUES (NULL, '2026-10-07', '15:00:00', 100000.00, NULL, NULL, NULL, 25000.00, 1775000.00, 0.00, '2026-10-07 01:05:39.926729', 1, 18, NULL, 1, NULL, NULL, 5, 3, 1, N'BUY', N'NONE', N'BANK_TRANSFER', N'PAID', N'0909999888', N'DELIVERY', N'CONFIRMED', N'Lê Khách Hàng Yêu Hoa', NULL, NULL, N'88 Nam Kỳ Khởi Nghĩa');
INSERT INTO dbo.[orders] ([commission_rate_snapshot], [delivery_date], [delivery_time], [discount_amount], [fine_amount], [funds_amount], [payment_expired], [shipping_fee], [total_amount], [total_deposit], [created_at], [delivery_unit_id], [id], [payment_expires_at], [pickup_store_id], [return_store_id], [shipper_id], [user_id], [version], [voucher_id], [order_type], [funds_status], [payment_method], [payment_status], [recipient_phone], [shipping_method], [status], [recipient_name], [cancellation_reason], [greeting_message], [recipient_address]) VALUES (NULL, '2026-10-07', '13:00:00', 0.00, NULL, NULL, NULL, 0.00, 890000.00, 0.00, '2026-10-07 10:25:36.378394', NULL, 19, NULL, 1, NULL, NULL, 35, 1, NULL, N'BUY', N'NONE', N'COD', N'UNPAID', N'0912457891', N'PICKUP', N'PENDING', N'Đinh Quốc Anh', NULL, NULL, N'Nhận tại chi nhánh: Chi Nhánh Bến Thành (Quận 1) - 123 Lê Lợi, Phường Bến Thành, Quận 1, TP. Hồ Chí Minh');
INSERT INTO dbo.[orders] ([commission_rate_snapshot], [delivery_date], [delivery_time], [discount_amount], [fine_amount], [funds_amount], [payment_expired], [shipping_fee], [total_amount], [total_deposit], [created_at], [delivery_unit_id], [id], [payment_expires_at], [pickup_store_id], [return_store_id], [shipper_id], [user_id], [version], [voucher_id], [order_type], [funds_status], [payment_method], [payment_status], [recipient_phone], [shipping_method], [status], [recipient_name], [cancellation_reason], [greeting_message], [recipient_address]) VALUES (NULL, '2026-10-07', '17:00:00', 0.00, NULL, NULL, 0, 0.00, 39000000.00, 0.00, '2026-10-07 16:48:41.708248', NULL, 20, NULL, 1, NULL, NULL, 5, 1, NULL, N'BUY', N'NONE', N'COD', N'UNPAID', N'0933516292', N'PICKUP', N'PENDING', N'Lê Minh Hoa', NULL, NULL, N'Nhận tại chi nhánh: Chi Nhánh Bến Thành (Quận 1) - 123 Lê Lợi, Phường Bến Thành, Quận 1, TP. Hồ Chí Minh');
INSERT INTO dbo.[orders] ([commission_rate_snapshot], [delivery_date], [delivery_time], [discount_amount], [fine_amount], [funds_amount], [payment_expired], [shipping_fee], [total_amount], [total_deposit], [created_at], [delivery_unit_id], [id], [payment_expires_at], [pickup_store_id], [return_store_id], [shipper_id], [user_id], [version], [voucher_id], [order_type], [funds_status], [payment_method], [payment_status], [recipient_phone], [shipping_method], [status], [recipient_name], [cancellation_reason], [greeting_message], [recipient_address]) VALUES (NULL, '2026-10-07', '17:00:00', 0.00, NULL, NULL, 0, 0.00, 39000000.00, 0.00, '2026-10-07 16:49:26.700522', NULL, 21, NULL, 2, NULL, NULL, 5, 2, NULL, N'BUY', N'NONE', N'COD', N'UNPAID', N'0933516292', N'PICKUP', N'CANCELLED', N'Lê Minh Hoa', NULL, NULL, N'Nhận tại chi nhánh: Chi Nhánh Phú Mỹ Hưng (Quận 7) - 456 Nguyễn Thị Thập, Tân Hưng, Quận 7, TP.HCM');
INSERT INTO dbo.[orders] ([commission_rate_snapshot], [delivery_date], [delivery_time], [discount_amount], [fine_amount], [funds_amount], [payment_expired], [shipping_fee], [total_amount], [total_deposit], [created_at], [delivery_unit_id], [id], [payment_expires_at], [pickup_store_id], [return_store_id], [shipper_id], [user_id], [version], [voucher_id], [order_type], [funds_status], [payment_method], [payment_status], [recipient_phone], [shipping_method], [status], [recipient_name], [cancellation_reason], [greeting_message], [recipient_address]) VALUES (NULL, '2026-10-13', '11:00:00', 0.00, NULL, NULL, 0, 0.00, 34500000.00, 0.00, '2026-10-13 07:51:00.307009', NULL, 22, NULL, 1, NULL, NULL, 5, 4, NULL, N'BUY', N'NONE', N'COD', N'UNPAID', N'0933516292', N'PICKUP', N'CANCELLED', N'Lê Minh Hoa', N'Khách hàng đã hủy đơn trước khi giao hàng.', NULL, N'Nhận tại chi nhánh: Chi Nhánh Bến Thành (Quận 1) - 123 Lê Lợi, Phường Bến Thành, Quận 1, TP. Hồ Chí Minh');
INSERT INTO dbo.[orders] ([commission_rate_snapshot], [delivery_date], [delivery_time], [discount_amount], [fine_amount], [funds_amount], [payment_expired], [shipping_fee], [total_amount], [total_deposit], [created_at], [delivery_unit_id], [id], [payment_expires_at], [pickup_store_id], [return_store_id], [shipper_id], [user_id], [version], [voucher_id], [order_type], [funds_status], [payment_method], [payment_status], [recipient_phone], [shipping_method], [status], [recipient_name], [cancellation_reason], [greeting_message], [recipient_address]) VALUES (NULL, '2026-10-13', '17:00:00', 0.00, NULL, NULL, 0, 0.00, 34500000.00, 0.00, '2026-10-13 08:53:24.554199', NULL, 23, NULL, 1, NULL, NULL, 5, 3, NULL, N'BUY', N'NONE', N'COD', N'UNPAID', N'0933516292', N'PICKUP', N'CANCELLED', N'Lê Minh Hoa', N'Đơn hàng của bạn đã bị hủy bởi admin (quản trị viên) với lý do: không còn hàng.', NULL, N'Nhận tại chi nhánh: Chi Nhánh Bến Thành (Quận 1) - 123 Lê Lợi, Phường Bến Thành, Quận 1, TP. Hồ Chí Minh');
INSERT INTO dbo.[orders] ([commission_rate_snapshot], [delivery_date], [delivery_time], [discount_amount], [fine_amount], [funds_amount], [payment_expired], [shipping_fee], [total_amount], [total_deposit], [created_at], [delivery_unit_id], [id], [payment_expires_at], [pickup_store_id], [return_store_id], [shipper_id], [user_id], [version], [voucher_id], [order_type], [funds_status], [payment_method], [payment_status], [recipient_phone], [shipping_method], [status], [recipient_name], [cancellation_reason], [greeting_message], [recipient_address]) VALUES (NULL, '2026-10-13', '17:00:00', 100000.00, NULL, NULL, 0, 25000.00, 34425000.00, 0.00, '2026-10-13 09:12:21.720364', 1, 24, NULL, 2, NULL, NULL, 5, 2, 1, N'BUY', N'NONE', N'COD', N'UNPAID', N'0909999888', N'DELIVERY', N'CANCELLED', N'Lê Khách Hàng Yêu Hoa', N'Khách hàng đã hủy đơn trước khi giao hàng.', NULL, N'88 Nam Kỳ Khởi Nghĩa, Phường Bến Nghé, Quận 1, TP. Hồ Chí Minh');
INSERT INTO dbo.[orders] ([commission_rate_snapshot], [delivery_date], [delivery_time], [discount_amount], [fine_amount], [funds_amount], [payment_expired], [shipping_fee], [total_amount], [total_deposit], [created_at], [delivery_unit_id], [id], [payment_expires_at], [pickup_store_id], [return_store_id], [shipper_id], [user_id], [version], [voucher_id], [order_type], [funds_status], [payment_method], [payment_status], [recipient_phone], [shipping_method], [status], [recipient_name], [cancellation_reason], [greeting_message], [recipient_address]) VALUES (NULL, '2026-10-13', '17:00:00', 50000.00, NULL, NULL, 0, 25000.00, 34475000.00, 0.00, '2026-10-13 15:01:26.218657', 1, 25, NULL, 2, NULL, NULL, 5, 6, 3, N'BUY', N'NONE', N'COD', N'PAID', N'0909999888', N'DELIVERY', N'COMPLETED', N'Lê Khách Hàng Yêu Hoa', NULL, NULL, N'88 Nam Kỳ Khởi Nghĩa, Phường Bến Nghé, Quận 1, TP. Hồ Chí Minh');
INSERT INTO dbo.[orders] ([commission_rate_snapshot], [delivery_date], [delivery_time], [discount_amount], [fine_amount], [funds_amount], [payment_expired], [shipping_fee], [total_amount], [total_deposit], [created_at], [delivery_unit_id], [id], [payment_expires_at], [pickup_store_id], [return_store_id], [shipper_id], [user_id], [version], [voucher_id], [order_type], [funds_status], [payment_method], [payment_status], [recipient_phone], [shipping_method], [status], [recipient_name], [cancellation_reason], [greeting_message], [recipient_address]) VALUES (NULL, '2026-10-13', '17:00:00', 0.00, NULL, NULL, 0, 25000.00, 3275000.00, 0.00, '2026-10-13 15:17:27.793693', 1, 26, NULL, 2, NULL, NULL, 5, 4, NULL, N'BUY', N'NONE', N'COD', N'UNPAID', N'0909999888', N'DELIVERY', N'CANCELLED', N'Lê Khách Hàng Yêu Hoa', N'Chi nhánh/quản trị viên đã hủy đơn hàng.', NULL, N'88 Nam Kỳ Khởi Nghĩa, Phường Bến Nghé, Quận 1, TP. Hồ Chí Minh');
INSERT INTO dbo.[orders] ([commission_rate_snapshot], [delivery_date], [delivery_time], [discount_amount], [fine_amount], [funds_amount], [payment_expired], [shipping_fee], [total_amount], [total_deposit], [created_at], [delivery_unit_id], [id], [payment_expires_at], [pickup_store_id], [return_store_id], [shipper_id], [user_id], [version], [voucher_id], [order_type], [funds_status], [payment_method], [payment_status], [recipient_phone], [shipping_method], [status], [recipient_name], [cancellation_reason], [greeting_message], [recipient_address]) VALUES (NULL, '2026-10-19', '15:00:00', 79900.00, NULL, NULL, 0, 25000.00, 744100.00, 0.00, '2026-10-19 11:13:41.226909', 1, 27, '2026-10-19 11:23:41.260248', 1, NULL, NULL, 5, 2, 1, N'BUY', N'NONE', N'MOMO_DEMO', N'UNPAID', N'0909999888', N'DELIVERY', N'CANCELLED', N'Lê Khách Hàng Yêu Hoa', N'Khách hàng đã hủy đơn trước khi bắt đầu cắm hoa/chuẩn bị.', NULL, N'88 Nam Kỳ Khởi Nghĩa, Phường Bến Nghé, Quận 1, TP. Hồ Chí Minh');
INSERT INTO dbo.[orders] ([commission_rate_snapshot], [delivery_date], [delivery_time], [discount_amount], [fine_amount], [funds_amount], [payment_expired], [shipping_fee], [total_amount], [total_deposit], [created_at], [delivery_unit_id], [id], [payment_expires_at], [pickup_store_id], [return_store_id], [shipper_id], [user_id], [version], [voucher_id], [order_type], [funds_status], [payment_method], [payment_status], [recipient_phone], [shipping_method], [status], [recipient_name], [cancellation_reason], [greeting_message], [recipient_address]) VALUES (NULL, '2026-10-22', '17:00:00', 0.00, NULL, NULL, 0, 25000.00, 975000.00, 0.00, '2026-10-21 21:44:22.405235', 1, 28, NULL, 2, NULL, NULL, 5, 1, NULL, N'BUY', N'NONE', N'COD', N'UNPAID', N'0909999888', N'DELIVERY', N'PENDING', N'Lê Khách Hàng Yêu Hoa', NULL, NULL, N'88 Nam Kỳ Khởi Nghĩa, Phường Bến Nghé, Quận 1, TP. Hồ Chí Minh');
INSERT INTO dbo.[orders] ([commission_rate_snapshot], [delivery_date], [delivery_time], [discount_amount], [fine_amount], [funds_amount], [payment_expired], [shipping_fee], [total_amount], [total_deposit], [created_at], [delivery_unit_id], [id], [payment_expires_at], [pickup_store_id], [return_store_id], [shipper_id], [user_id], [version], [voucher_id], [order_type], [funds_status], [payment_method], [payment_status], [recipient_phone], [shipping_method], [status], [recipient_name], [cancellation_reason], [greeting_message], [recipient_address]) VALUES (NULL, '2026-10-22', '07:00:00', 0.00, NULL, NULL, 0, 0.00, 650000.00, 0.00, '2026-10-21 21:48:48.459686', NULL, 29, NULL, 2, NULL, NULL, 37, 4, NULL, N'BUY', N'NONE', N'COD', N'PAID', N'0947475853', N'PICKUP', N'COMPLETED', N'Lý Gia Bảo', NULL, N'Chúc em 20/10 hạnh phúc', N'Nhận tại chi nhánh: Chi Nhánh Phú Mỹ Hưng (Quận 7) - 456 Nguyễn Thị Thập, Tân Hưng, Quận 7, TP.HCM');
INSERT INTO dbo.[orders] ([commission_rate_snapshot], [delivery_date], [delivery_time], [discount_amount], [fine_amount], [funds_amount], [payment_expired], [shipping_fee], [total_amount], [total_deposit], [created_at], [delivery_unit_id], [id], [payment_expires_at], [pickup_store_id], [return_store_id], [shipper_id], [user_id], [version], [voucher_id], [order_type], [funds_status], [payment_method], [payment_status], [recipient_phone], [shipping_method], [status], [recipient_name], [cancellation_reason], [greeting_message], [recipient_address]) VALUES (NULL, '2026-10-22', '17:00:00', 0.00, NULL, NULL, 0, 0.00, 390000.00, 0.00, '2026-10-22 15:52:09.263197', NULL, 30, NULL, 3, NULL, NULL, 37, 2, NULL, N'BUY', N'NONE', N'COD', N'UNPAID', N'0947475853', N'PICKUP', N'CANCELLED', N'Lý Gia Bảo', N'Khách hàng đã hủy đơn trước khi bắt đầu cắm hoa/chuẩn bị.', NULL, N'Nhận tại chi nhánh: Chi Nhánh Làng Đại Học (TP. Thủ Đức) - 789 Võ Văn Ngân, Linh Chiểu, TP. Thủ Đức, TP. Hồ Chí Minh');
INSERT INTO dbo.[orders] ([commission_rate_snapshot], [delivery_date], [delivery_time], [discount_amount], [fine_amount], [funds_amount], [payment_expired], [shipping_fee], [total_amount], [total_deposit], [created_at], [delivery_unit_id], [id], [payment_expires_at], [pickup_store_id], [return_store_id], [shipper_id], [user_id], [version], [voucher_id], [order_type], [funds_status], [payment_method], [payment_status], [recipient_phone], [shipping_method], [status], [recipient_name], [cancellation_reason], [greeting_message], [recipient_address]) VALUES (NULL, '2026-10-22', '17:00:00', 0.00, NULL, NULL, 0, 0.00, 950000.00, 0.00, '2026-10-22 16:53:29.421136', NULL, 31, NULL, 2, NULL, NULL, 37, 2, NULL, N'BUY', N'NONE', N'COD', N'UNPAID', N'0947475853', N'PICKUP', N'CANCELLED', N'Lý Gia Bảo', N'Khách hàng đã hủy đơn với lý do: Thay đổi nhu cầu, không còn nhu cầu mua.', NULL, N'Nhận tại chi nhánh: Chi Nhánh Phú Mỹ Hưng (Quận 7) - 456 Nguyễn Thị Thập, Tân Hưng, Quận 7, TP.HCM');
INSERT INTO dbo.[orders] ([commission_rate_snapshot], [delivery_date], [delivery_time], [discount_amount], [fine_amount], [funds_amount], [payment_expired], [shipping_fee], [total_amount], [total_deposit], [created_at], [delivery_unit_id], [id], [payment_expires_at], [pickup_store_id], [return_store_id], [shipper_id], [user_id], [version], [voucher_id], [order_type], [funds_status], [payment_method], [payment_status], [recipient_phone], [shipping_method], [status], [recipient_name], [cancellation_reason], [greeting_message], [recipient_address]) VALUES (NULL, '2026-10-23', '17:00:00', 0.00, NULL, NULL, 0, 40000.00, 740000.00, 0.00, '2026-10-23 02:06:28.141063', 1, 32, NULL, 3, NULL, NULL, 5, 1, NULL, N'BUY', N'NONE', N'COD', N'UNPAID', N'0933516292', N'DELIVERY', N'PENDING', N'Lê Minh Hoa', NULL, NULL, N'88 Nam Kỳ Khởi Nghĩa, Bến Nghé, Quận 1, TP. Hồ Chí Minh');
INSERT INTO dbo.[orders] ([commission_rate_snapshot], [delivery_date], [delivery_time], [discount_amount], [fine_amount], [funds_amount], [payment_expired], [shipping_fee], [total_amount], [total_deposit], [created_at], [delivery_unit_id], [id], [payment_expires_at], [pickup_store_id], [return_store_id], [shipper_id], [user_id], [version], [voucher_id], [order_type], [funds_status], [payment_method], [payment_status], [recipient_phone], [shipping_method], [status], [recipient_name], [cancellation_reason], [greeting_message], [recipient_address]) VALUES (NULL, '2026-10-24', '07:00:00', 0.00, NULL, NULL, 1, 25000.00, 915000.00, 0.00, '2026-10-23 22:38:56.377565', 1, 33, '2026-10-23 22:48:57.287089', 2, NULL, NULL, 5, 2, NULL, N'BUY', N'NONE', N'MOMO_DEMO', N'UNPAID', N'0933516292', N'DELIVERY', N'CANCELLED', N'Lê Minh Hoa', N'Đơn đã hết thời hạn thanh toán. Tồn kho đã được trả lại chi nhánh.', NULL, N'88 Nam Kỳ Khởi Nghĩa, Bến Nghé, Quận 1, TP. Hồ Chí Minh');
INSERT INTO dbo.[orders] ([commission_rate_snapshot], [delivery_date], [delivery_time], [discount_amount], [fine_amount], [funds_amount], [payment_expired], [shipping_fee], [total_amount], [total_deposit], [created_at], [delivery_unit_id], [id], [payment_expires_at], [pickup_store_id], [return_store_id], [shipper_id], [user_id], [version], [voucher_id], [order_type], [funds_status], [payment_method], [payment_status], [recipient_phone], [shipping_method], [status], [recipient_name], [cancellation_reason], [greeting_message], [recipient_address]) VALUES (8.0000, '2026-10-24', '07:00:00', 0.00, NULL, 675000.00, 0, 25000.00, 675000.00, 0.00, '2026-10-23 23:20:30.922861', 1, 34, '2026-10-23 23:30:32.144981', 2, NULL, NULL, 5, 3, NULL, N'BUY', N'REFUNDED', N'MOMO_DEMO', N'REFUNDED', N'0933516292', N'DELIVERY', N'REFUNDED', N'Lê Minh Hoa', N'Khách hàng đã hủy đơn với lý do: Thay đổi nhu cầu, không còn nhu cầu mua. Đã hủy đơn và hoàn tiền thành công.', NULL, N'88 Nam Kỳ Khởi Nghĩa, Bến Nghé, Quận 1, TP. Hồ Chí Minh');
INSERT INTO dbo.[orders] ([commission_rate_snapshot], [delivery_date], [delivery_time], [discount_amount], [fine_amount], [funds_amount], [payment_expired], [shipping_fee], [total_amount], [total_deposit], [created_at], [delivery_unit_id], [id], [payment_expires_at], [pickup_store_id], [return_store_id], [shipper_id], [user_id], [version], [voucher_id], [order_type], [funds_status], [payment_method], [payment_status], [recipient_phone], [shipping_method], [status], [recipient_name], [cancellation_reason], [greeting_message], [recipient_address]) VALUES (8.0000, '2026-10-24', '17:00:00', 0.00, NULL, 675000.00, 0, 25000.00, 675000.00, 0.00, '2026-10-24 14:36:43.796173', 1, 35, '2026-10-24 14:46:45.391601', 2, NULL, NULL, 5, 3, NULL, N'BUY', N'REFUNDED', N'MOMO_DEMO', N'REFUNDED', N'0933516292', N'DELIVERY', N'REFUNDED', N'Lê Minh Hoa', N'Khách hàng đã hủy đơn với lý do: Muốn thay đổi địa chỉ nhận hàng. Đã hủy đơn và hoàn tiền thành công.', NULL, N'88 Nam Kỳ Khởi Nghĩa, Bến Nghé, Quận 1, TP. Hồ Chí Minh');
INSERT INTO dbo.[orders] ([commission_rate_snapshot], [delivery_date], [delivery_time], [discount_amount], [fine_amount], [funds_amount], [payment_expired], [shipping_fee], [total_amount], [total_deposit], [created_at], [delivery_unit_id], [id], [payment_expires_at], [pickup_store_id], [return_store_id], [shipper_id], [user_id], [version], [voucher_id], [order_type], [funds_status], [payment_method], [payment_status], [recipient_phone], [shipping_method], [status], [recipient_name], [cancellation_reason], [greeting_message], [recipient_address]) VALUES (8.0000, '2026-10-31', '17:00:00', 0.00, NULL, 675000.00, 0, 25000.00, 675000.00, 0.00, '2026-10-24 17:22:05.733138', 1, 36, '2026-10-24 17:32:06.81913', 2, NULL, NULL, 5, 4, NULL, N'BUY', N'REFUNDED', N'MOMO_DEMO', N'REFUNDED', N'0933516292', N'DELIVERY', N'REFUNDED', N'Lê Minh Hoa', N'Khách hàng đã hủy đơn với lý do: Thay đổi nhu cầu, không còn nhu cầu mua. Đã hủy đơn và hoàn tiền thành công. Tiền được cộng vào ví demo của bạn.', NULL, N'88 Nam Kỳ Khởi Nghĩa, Bến Nghé, Quận 1, TP. Hồ Chí Minh');
INSERT INTO dbo.[orders] ([commission_rate_snapshot], [delivery_date], [delivery_time], [discount_amount], [fine_amount], [funds_amount], [payment_expired], [shipping_fee], [total_amount], [total_deposit], [created_at], [delivery_unit_id], [id], [payment_expires_at], [pickup_store_id], [return_store_id], [shipper_id], [user_id], [version], [voucher_id], [order_type], [funds_status], [payment_method], [payment_status], [recipient_phone], [shipping_method], [status], [recipient_name], [cancellation_reason], [greeting_message], [recipient_address]) VALUES (10.0000, '2026-11-07', '17:00:00', 0.00, NULL, 675000.00, 0, 25000.00, 675000.00, 0.00, '2026-10-24 18:51:41.806133', 1, 37, NULL, 1, NULL, NULL, 5, 4, NULL, N'BUY', N'REFUNDED', N'WALLET_DEMO', N'REFUNDED', N'0933516292', N'DELIVERY', N'REFUNDED', N'Lê Minh Hoa', N'Khách hàng đã hủy đơn với lý do: Đặt nhầm sản phẩm. Đã hủy đơn và hoàn tiền thành công. Tiền được cộng vào ví của bạn.', NULL, N'88 Nam Kỳ Khởi Nghĩa, Bến Nghé, Quận 1, TP. Hồ Chí Minh');
INSERT INTO dbo.[orders] ([commission_rate_snapshot], [delivery_date], [delivery_time], [discount_amount], [fine_amount], [funds_amount], [payment_expired], [shipping_fee], [total_amount], [total_deposit], [created_at], [delivery_unit_id], [id], [payment_expires_at], [pickup_store_id], [return_store_id], [shipper_id], [user_id], [version], [voucher_id], [order_type], [funds_status], [payment_method], [payment_status], [recipient_phone], [shipping_method], [status], [recipient_name], [cancellation_reason], [greeting_message], [recipient_address]) VALUES (NULL, '2026-10-26', '09:00:00', 0.00, NULL, NULL, 1, 25000.00, 675000.00, 0.00, '2026-10-25 18:19:56.375694', 1, 38, '2026-10-25 18:29:58.974629', 2, NULL, NULL, 5, 2, NULL, N'BUY', N'NONE', N'MOMO_DEMO', N'UNPAID', N'0933516292', N'DELIVERY', N'CANCELLED', N'Lê Minh Hoa', N'Đơn đã hết thời hạn thanh toán. Tồn kho đã được trả lại chi nhánh.', NULL, N'88 Nam Kỳ Khởi Nghĩa, Bến Nghé, Quận 1, TP. Hồ Chí Minh');
INSERT INTO dbo.[orders] ([commission_rate_snapshot], [delivery_date], [delivery_time], [discount_amount], [fine_amount], [funds_amount], [payment_expired], [shipping_fee], [total_amount], [total_deposit], [created_at], [delivery_unit_id], [id], [payment_expires_at], [pickup_store_id], [return_store_id], [shipper_id], [user_id], [version], [voucher_id], [order_type], [funds_status], [payment_method], [payment_status], [recipient_phone], [shipping_method], [status], [recipient_name], [cancellation_reason], [greeting_message], [recipient_address]) VALUES (NULL, '2026-10-27', '07:00:00', 0.00, NULL, NULL, 0, 25000.00, 915000.00, 0.00, '2026-10-25 23:35:59.473412', 1, 39, '2026-10-25 23:46:01.615308', 2, NULL, NULL, 5, 2, NULL, N'BUY', N'NONE', N'MOMO_DEMO', N'UNPAID', N'0933516292', N'DELIVERY', N'CANCELLED', N'Lê Minh Hoa', N'Khách hàng đã hủy đơn với lý do: Muốn đổi mã giảm giá.', NULL, N'88 Nam Kỳ Khởi Nghĩa, Bến Nghé, Quận 1, TP. Hồ Chí Minh');
INSERT INTO dbo.[orders] ([commission_rate_snapshot], [delivery_date], [delivery_time], [discount_amount], [fine_amount], [funds_amount], [payment_expired], [shipping_fee], [total_amount], [total_deposit], [created_at], [delivery_unit_id], [id], [payment_expires_at], [pickup_store_id], [return_store_id], [shipper_id], [user_id], [version], [voucher_id], [order_type], [funds_status], [payment_method], [payment_status], [recipient_phone], [shipping_method], [status], [recipient_name], [cancellation_reason], [greeting_message], [recipient_address]) VALUES (8.0000, '2026-10-27', '07:00:00', 0.00, NULL, 915000.00, 0, 25000.00, 915000.00, 0.00, '2026-10-25 23:50:44.859639', 1, 40, '2026-10-26 00:00:47.354899', 2, NULL, NULL, 5, 4, NULL, N'BUY', N'REFUNDED', N'MOMO_DEMO', N'REFUNDED', N'0933516292', N'DELIVERY', N'REFUNDED', N'Lê Minh Hoa', N'Khách hàng đã hủy đơn với lý do: Muốn thay đổi địa chỉ nhận hàng. Đã hủy đơn và hoàn tiền thành công. Tiền được cộng vào ví của bạn.', NULL, N'88 Nam Kỳ Khởi Nghĩa, Bến Nghé, Quận 1, TP. Hồ Chí Minh');
INSERT INTO dbo.[orders] ([commission_rate_snapshot], [delivery_date], [delivery_time], [discount_amount], [fine_amount], [funds_amount], [payment_expired], [shipping_fee], [total_amount], [total_deposit], [created_at], [delivery_unit_id], [id], [payment_expires_at], [pickup_store_id], [return_store_id], [shipper_id], [user_id], [version], [voucher_id], [order_type], [funds_status], [payment_method], [payment_status], [recipient_phone], [shipping_method], [status], [recipient_name], [cancellation_reason], [greeting_message], [recipient_address]) VALUES (8.0000, '2026-10-27', '09:00:00', 0.00, NULL, 715000.00, 0, 25000.00, 715000.00, 0.00, '2026-10-26 00:05:48.960688', 1, 41, '2026-10-26 00:15:51.111288', 2, NULL, NULL, 5, 4, NULL, N'BUY', N'REFUNDED', N'MOMO_DEMO', N'REFUNDED', N'0933516292', N'DELIVERY', N'REFUNDED', N'Lê Minh Hoa', N'Khách hàng đã hủy đơn với lý do: Muốn thay đổi địa chỉ nhận hàng. Đã hủy đơn và hoàn tiền thành công. Tiền được cộng vào ví của bạn.', NULL, N'88 Nam Kỳ Khởi Nghĩa, Bến Nghé, Quận 1, TP. Hồ Chí Minh');
INSERT INTO dbo.[orders] ([commission_rate_snapshot], [delivery_date], [delivery_time], [discount_amount], [fine_amount], [funds_amount], [payment_expired], [shipping_fee], [total_amount], [total_deposit], [created_at], [delivery_unit_id], [id], [payment_expires_at], [pickup_store_id], [return_store_id], [shipper_id], [user_id], [version], [voucher_id], [order_type], [funds_status], [payment_method], [payment_status], [recipient_phone], [shipping_method], [status], [recipient_name], [cancellation_reason], [greeting_message], [recipient_address]) VALUES (8.0000, '2026-10-27', '17:00:00', 0.00, NULL, 545000.00, 0, 25000.00, 545000.00, 0.00, '2026-10-26 21:14:46.290061', 1, 42, '2026-10-26 21:24:50.221788', 2, NULL, NULL, 5, 6, NULL, N'BUY', N'SETTLED', N'MOMO_DEMO', N'PAID', N'0933516292', N'DELIVERY', N'DELIVERED', N'Lê Minh Hoa', NULL, NULL, N'88 Nam Kỳ Khởi Nghĩa, Bến Nghé, Quận 1, TP. Hồ Chí Minh');
INSERT INTO dbo.[orders] ([commission_rate_snapshot], [delivery_date], [delivery_time], [discount_amount], [fine_amount], [funds_amount], [payment_expired], [shipping_fee], [total_amount], [total_deposit], [created_at], [delivery_unit_id], [id], [payment_expires_at], [pickup_store_id], [return_store_id], [shipper_id], [user_id], [version], [voucher_id], [order_type], [funds_status], [payment_method], [payment_status], [recipient_phone], [shipping_method], [status], [recipient_name], [cancellation_reason], [greeting_message], [recipient_address]) VALUES (8.0000, '2026-10-31', '13:00:00', 0.00, NULL, 915000.00, 0, 25000.00, 915000.00, 0.00, '2026-10-26 21:34:29.600775', 1, 43, NULL, 2, NULL, NULL, 5, 2, NULL, N'BUY', N'HELD', N'WALLET_DEMO', N'PAID', N'0933516292', N'DELIVERY', N'PENDING', N'Lê Minh Hoa', NULL, NULL, N'88 Nam Kỳ Khởi Nghĩa, Bến Nghé, Quận 1, TP. Hồ Chí Minh');
INSERT INTO dbo.[orders] ([commission_rate_snapshot], [delivery_date], [delivery_time], [discount_amount], [fine_amount], [funds_amount], [payment_expired], [shipping_fee], [total_amount], [total_deposit], [created_at], [delivery_unit_id], [id], [payment_expires_at], [pickup_store_id], [return_store_id], [shipper_id], [user_id], [version], [voucher_id], [order_type], [funds_status], [payment_method], [payment_status], [recipient_phone], [shipping_method], [status], [recipient_name], [cancellation_reason], [greeting_message], [recipient_address]) VALUES (NULL, '2026-10-29', '09:00:00', 65000.00, NULL, NULL, 1, 25000.00, 610000.00, 0.00, '2026-10-28 14:17:25.166613', 1, 44, '2026-10-28 14:27:25.200107', 1, NULL, NULL, 40, 2, 1, N'BUY', N'NONE', N'MOMO_DEMO', N'UNPAID', N'0909999888', N'DELIVERY', N'CANCELLED', N'Local Customer', N'Đơn đã hết thời hạn thanh toán. Tồn kho đã được trả lại chi nhánh.', NULL, N'88 Nam Ká»³ Khá»Ÿi NghÄ©a, Quáº­n 1, TP.HCM');
INSERT INTO dbo.[orders] ([commission_rate_snapshot], [delivery_date], [delivery_time], [discount_amount], [fine_amount], [funds_amount], [payment_expired], [shipping_fee], [total_amount], [total_deposit], [created_at], [delivery_unit_id], [id], [payment_expires_at], [pickup_store_id], [return_store_id], [shipper_id], [user_id], [version], [voucher_id], [order_type], [funds_status], [payment_method], [payment_status], [recipient_phone], [shipping_method], [status], [recipient_name], [cancellation_reason], [greeting_message], [recipient_address]) VALUES (NULL, '2026-10-28', '15:00:00', 69000.00, NULL, NULL, 1, 25000.00, 646000.00, 0.00, '2026-10-28 14:30:30.143769', 1, 45, '2026-10-28 14:40:30.191815', 2, NULL, NULL, 5, 2, 1, N'BUY', N'NONE', N'MOMO_DEMO', N'UNPAID', N'0933516292', N'DELIVERY', N'CANCELLED', N'Lê Minh Hoa', N'Đơn đã hết thời hạn thanh toán. Tồn kho đã được trả lại chi nhánh.', NULL, N'88 Nam Kỳ Khởi Nghĩa, Bến Nghé, Quận 1, TP. Hồ Chí Minh');
INSERT INTO dbo.[orders] ([commission_rate_snapshot], [delivery_date], [delivery_time], [discount_amount], [fine_amount], [funds_amount], [payment_expired], [shipping_fee], [total_amount], [total_deposit], [created_at], [delivery_unit_id], [id], [payment_expires_at], [pickup_store_id], [return_store_id], [shipper_id], [user_id], [version], [voucher_id], [order_type], [funds_status], [payment_method], [payment_status], [recipient_phone], [shipping_method], [status], [recipient_name], [cancellation_reason], [greeting_message], [recipient_address]) VALUES (NULL, '2026-10-28', '15:00:00', 89000.00, NULL, NULL, 1, 25000.00, 826000.00, 0.00, '2026-10-28 14:53:45.613188', 1, 46, '2026-10-28 15:03:45.674047', 2, NULL, NULL, 2, 2, 1, N'BUY', N'NONE', N'MOMO_DEMO', N'UNPAID', N'0902222333', N'DELIVERY', N'CANCELLED', N'Nguyễn Quang Huy', N'Đơn đã hết thời hạn thanh toán. Tồn kho đã được trả lại chi nhánh.', NULL, N'123 Lê Lợi, Bến Thành, Quận 1, TP. Hồ Chí Minh');
SET IDENTITY_INSERT dbo.[orders] OFF;
GO

-- Dữ liệu bảng: payments
SET IDENTITY_INSERT dbo.[payments] ON;
INSERT INTO dbo.[payments] ([amount], [id], [order_id], [paid_at], [refunded_at], [momo_environment], [method], [status], [refund_method], [refund_note], [transaction_code]) VALUES (650000.00, 1, 1, NULL, NULL, NULL, N'COD', N'PENDING', NULL, NULL, NULL);
INSERT INTO dbo.[payments] ([amount], [id], [order_id], [paid_at], [refunded_at], [momo_environment], [method], [status], [refund_method], [refund_note], [transaction_code]) VALUES (1775000.00, 2, 18, '2026-10-07 01:07:53.345177', NULL, NULL, N'BANK_TRANSFER', N'SUCCESS', NULL, NULL, NULL);
INSERT INTO dbo.[payments] ([amount], [id], [order_id], [paid_at], [refunded_at], [momo_environment], [method], [status], [refund_method], [refund_note], [transaction_code]) VALUES (890000.00, 3, 19, NULL, NULL, NULL, N'COD', N'PENDING', NULL, NULL, NULL);
INSERT INTO dbo.[payments] ([amount], [id], [order_id], [paid_at], [refunded_at], [momo_environment], [method], [status], [refund_method], [refund_note], [transaction_code]) VALUES (39000000.00, 4, 20, NULL, NULL, NULL, N'COD', N'PENDING', NULL, NULL, NULL);
INSERT INTO dbo.[payments] ([amount], [id], [order_id], [paid_at], [refunded_at], [momo_environment], [method], [status], [refund_method], [refund_note], [transaction_code]) VALUES (39000000.00, 5, 21, NULL, NULL, NULL, N'COD', N'FAILED', NULL, NULL, NULL);
INSERT INTO dbo.[payments] ([amount], [id], [order_id], [paid_at], [refunded_at], [momo_environment], [method], [status], [refund_method], [refund_note], [transaction_code]) VALUES (34500000.00, 6, 22, NULL, NULL, NULL, N'COD', N'FAILED', NULL, NULL, NULL);
INSERT INTO dbo.[payments] ([amount], [id], [order_id], [paid_at], [refunded_at], [momo_environment], [method], [status], [refund_method], [refund_note], [transaction_code]) VALUES (34500000.00, 7, 23, NULL, NULL, NULL, N'COD', N'FAILED', NULL, NULL, NULL);
INSERT INTO dbo.[payments] ([amount], [id], [order_id], [paid_at], [refunded_at], [momo_environment], [method], [status], [refund_method], [refund_note], [transaction_code]) VALUES (34425000.00, 8, 24, NULL, NULL, NULL, N'COD', N'FAILED', NULL, NULL, NULL);
INSERT INTO dbo.[payments] ([amount], [id], [order_id], [paid_at], [refunded_at], [momo_environment], [method], [status], [refund_method], [refund_note], [transaction_code]) VALUES (34475000.00, 9, 25, '2026-10-13 15:16:24.386719', NULL, NULL, N'COD', N'SUCCESS', NULL, NULL, NULL);
INSERT INTO dbo.[payments] ([amount], [id], [order_id], [paid_at], [refunded_at], [momo_environment], [method], [status], [refund_method], [refund_note], [transaction_code]) VALUES (3275000.00, 10, 26, NULL, NULL, NULL, N'COD', N'FAILED', NULL, NULL, NULL);
INSERT INTO dbo.[payments] ([amount], [id], [order_id], [paid_at], [refunded_at], [momo_environment], [method], [status], [refund_method], [refund_note], [transaction_code]) VALUES (744100.00, 11, 27, NULL, NULL, NULL, N'MOMO_DEMO', N'FAILED', NULL, NULL, NULL);
INSERT INTO dbo.[payments] ([amount], [id], [order_id], [paid_at], [refunded_at], [momo_environment], [method], [status], [refund_method], [refund_note], [transaction_code]) VALUES (975000.00, 12, 28, NULL, NULL, NULL, N'COD', N'PENDING', NULL, NULL, NULL);
INSERT INTO dbo.[payments] ([amount], [id], [order_id], [paid_at], [refunded_at], [momo_environment], [method], [status], [refund_method], [refund_note], [transaction_code]) VALUES (650000.00, 13, 29, '2026-10-22 15:57:26.073291', NULL, NULL, N'COD', N'SUCCESS', NULL, NULL, NULL);
INSERT INTO dbo.[payments] ([amount], [id], [order_id], [paid_at], [refunded_at], [momo_environment], [method], [status], [refund_method], [refund_note], [transaction_code]) VALUES (390000.00, 14, 30, NULL, NULL, NULL, N'COD', N'FAILED', NULL, NULL, NULL);
INSERT INTO dbo.[payments] ([amount], [id], [order_id], [paid_at], [refunded_at], [momo_environment], [method], [status], [refund_method], [refund_note], [transaction_code]) VALUES (950000.00, 15, 31, NULL, NULL, NULL, N'COD', N'FAILED', NULL, NULL, NULL);
INSERT INTO dbo.[payments] ([amount], [id], [order_id], [paid_at], [refunded_at], [momo_environment], [method], [status], [refund_method], [refund_note], [transaction_code]) VALUES (740000.00, 16, 32, NULL, NULL, NULL, N'COD', N'PENDING', NULL, NULL, NULL);
INSERT INTO dbo.[payments] ([amount], [id], [order_id], [paid_at], [refunded_at], [momo_environment], [method], [status], [refund_method], [refund_note], [transaction_code]) VALUES (915000.00, 17, 33, NULL, NULL, NULL, N'MOMO_DEMO', N'FAILED', NULL, NULL, NULL);
INSERT INTO dbo.[payments] ([amount], [id], [order_id], [paid_at], [refunded_at], [momo_environment], [method], [status], [refund_method], [refund_note], [transaction_code]) VALUES (675000.00, 18, 34, '2026-10-23 23:20:45.531022', '2026-10-23 23:21:24.147809', N'demo', N'MOMO_DEMO', N'REFUNDED', N'DEMO_AUTO', N'T? d?ng hoàn ti?n demo khi khách h?y don MoMo', N'DEMO-MOMO-34');
INSERT INTO dbo.[payments] ([amount], [id], [order_id], [paid_at], [refunded_at], [momo_environment], [method], [status], [refund_method], [refund_note], [transaction_code]) VALUES (675000.00, 19, 35, '2026-10-24 14:37:01.180574', '2026-10-24 14:37:24.883254', N'demo', N'MOMO_DEMO', N'REFUNDED', N'DEMO_AUTO', N'T? d?ng hoàn ti?n demo khi khách h?y don MoMo', N'DEMO-MOMO-35');
INSERT INTO dbo.[payments] ([amount], [id], [order_id], [paid_at], [refunded_at], [momo_environment], [method], [status], [refund_method], [refund_note], [transaction_code]) VALUES (675000.00, 20, 36, '2026-10-24 17:22:17.827097', '2026-10-24 17:22:38.30652', N'demo', N'MOMO_DEMO', N'REFUNDED', N'DEMO_AUTO', N'T? d?ng hoàn ti?n vào ví demo khi khách h?y don. Ðã c?ng vào ví demo c?a khách hàng, không ph?i ti?n th?t.', N'DEMO-MOMO-36');
INSERT INTO dbo.[payments] ([amount], [id], [order_id], [paid_at], [refunded_at], [momo_environment], [method], [status], [refund_method], [refund_note], [transaction_code]) VALUES (675000.00, 21, 37, '2026-10-24 18:51:44.73364', '2026-10-24 18:52:26.424719', N'demo-wallet', N'WALLET_DEMO', N'REFUNDED', N'DEMO_AUTO', N'T? d?ng hoàn ti?n vào ví demo khi khách h?y don. Ðã c?ng vào ví demo c?a khách hàng, không ph?i ti?n th?t.', N'WALLET-DEMO-37');
INSERT INTO dbo.[payments] ([amount], [id], [order_id], [paid_at], [refunded_at], [momo_environment], [method], [status], [refund_method], [refund_note], [transaction_code]) VALUES (675000.00, 22, 38, NULL, NULL, NULL, N'MOMO_DEMO', N'FAILED', NULL, NULL, NULL);
INSERT INTO dbo.[payments] ([amount], [id], [order_id], [paid_at], [refunded_at], [momo_environment], [method], [status], [refund_method], [refund_note], [transaction_code]) VALUES (915000.00, 23, 39, NULL, NULL, NULL, N'MOMO_DEMO', N'FAILED', NULL, NULL, NULL);
INSERT INTO dbo.[payments] ([amount], [id], [order_id], [paid_at], [refunded_at], [momo_environment], [method], [status], [refund_method], [refund_note], [transaction_code]) VALUES (915000.00, 24, 40, '2026-10-25 23:51:04.280399', '2026-10-25 23:51:30.598392', N'demo', N'MOMO_DEMO', N'REFUNDED', N'DEMO_AUTO', N'Tự động hoàn tiền vào ví demo khi khách hủy đơn. Đã cộng vào ví demo của khách hàng, không phải tiền thật.', N'DEMO-MOMO-40');
INSERT INTO dbo.[payments] ([amount], [id], [order_id], [paid_at], [refunded_at], [momo_environment], [method], [status], [refund_method], [refund_note], [transaction_code]) VALUES (715000.00, 25, 41, '2026-10-26 00:06:41.468473', '2026-10-26 00:07:06.777053', N'demo', N'MOMO_DEMO', N'REFUNDED', N'DEMO_AUTO', N'Tự động hoàn tiền vào ví demo khi khách hủy đơn. Đã cộng vào ví demo của khách hàng, không phải tiền thật.', N'DEMO-MOMO-41');
INSERT INTO dbo.[payments] ([amount], [id], [order_id], [paid_at], [refunded_at], [momo_environment], [method], [status], [refund_method], [refund_note], [transaction_code]) VALUES (545000.00, 26, 42, '2026-10-26 21:15:22.759397', NULL, N'demo', N'MOMO_DEMO', N'SUCCESS', NULL, NULL, N'DEMO-MOMO-42');
INSERT INTO dbo.[payments] ([amount], [id], [order_id], [paid_at], [refunded_at], [momo_environment], [method], [status], [refund_method], [refund_note], [transaction_code]) VALUES (915000.00, 27, 43, '2026-10-26 21:34:36.787055', NULL, N'demo-wallet', N'WALLET_DEMO', N'SUCCESS', NULL, NULL, N'WALLET-DEMO-43');
INSERT INTO dbo.[payments] ([amount], [id], [order_id], [paid_at], [refunded_at], [momo_environment], [method], [status], [refund_method], [refund_note], [transaction_code]) VALUES (610000.00, 28, 44, NULL, NULL, NULL, N'MOMO_DEMO', N'FAILED', NULL, NULL, NULL);
INSERT INTO dbo.[payments] ([amount], [id], [order_id], [paid_at], [refunded_at], [momo_environment], [method], [status], [refund_method], [refund_note], [transaction_code]) VALUES (646000.00, 29, 45, NULL, NULL, NULL, N'MOMO_DEMO', N'FAILED', NULL, NULL, NULL);
INSERT INTO dbo.[payments] ([amount], [id], [order_id], [paid_at], [refunded_at], [momo_environment], [method], [status], [refund_method], [refund_note], [transaction_code]) VALUES (826000.00, 30, 46, NULL, NULL, NULL, N'MOMO_DEMO', N'FAILED', NULL, NULL, NULL);
SET IDENTITY_INSERT dbo.[payments] OFF;
GO

-- Dữ liệu bảng: product_additional_categories
INSERT INTO dbo.[product_additional_categories] ([category_id], [product_id]) VALUES (7, 21);
INSERT INTO dbo.[product_additional_categories] ([category_id], [product_id]) VALUES (7, 40);
INSERT INTO dbo.[product_additional_categories] ([category_id], [product_id]) VALUES (7, 41);
INSERT INTO dbo.[product_additional_categories] ([category_id], [product_id]) VALUES (8, 40);
INSERT INTO dbo.[product_additional_categories] ([category_id], [product_id]) VALUES (8, 41);
INSERT INTO dbo.[product_additional_categories] ([category_id], [product_id]) VALUES (25, 21);
INSERT INTO dbo.[product_additional_categories] ([category_id], [product_id]) VALUES (25, 40);
INSERT INTO dbo.[product_additional_categories] ([category_id], [product_id]) VALUES (25, 41);
INSERT INTO dbo.[product_additional_categories] ([category_id], [product_id]) VALUES (26, 21);
INSERT INTO dbo.[product_additional_categories] ([category_id], [product_id]) VALUES (27, 21);
INSERT INTO dbo.[product_additional_categories] ([category_id], [product_id]) VALUES (27, 40);
INSERT INTO dbo.[product_additional_categories] ([category_id], [product_id]) VALUES (31, 21);
GO

-- Dữ liệu bảng: product_images
SET IDENTITY_INSERT dbo.[product_images] ON;
INSERT INTO dbo.[product_images] ([is_main], [id], [product_id], [cloudinary_public_id], [image_url]) VALUES (1, 45, 1, NULL, N'https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=800');
INSERT INTO dbo.[product_images] ([is_main], [id], [product_id], [cloudinary_public_id], [image_url]) VALUES (0, 46, 1, NULL, N'https://images.unsplash.com/photo-1559563458-527698bf5295?w=800');
INSERT INTO dbo.[product_images] ([is_main], [id], [product_id], [cloudinary_public_id], [image_url]) VALUES (0, 47, 1, NULL, N'https://images.unsplash.com/photo-1563245372-f21724e3856d?w=800');
INSERT INTO dbo.[product_images] ([is_main], [id], [product_id], [cloudinary_public_id], [image_url]) VALUES (0, 48, 1, NULL, N'https://images.unsplash.com/photo-1582794543139-8ac9cb0f7b11?w=800');
INSERT INTO dbo.[product_images] ([is_main], [id], [product_id], [cloudinary_public_id], [image_url]) VALUES (1, 49, 2, NULL, N'https://images.unsplash.com/photo-1526047932273-341f2a7631f9?w=800');
INSERT INTO dbo.[product_images] ([is_main], [id], [product_id], [cloudinary_public_id], [image_url]) VALUES (0, 50, 2, NULL, N'https://images.unsplash.com/photo-1589244159943-460088ed5c92?w=800');
INSERT INTO dbo.[product_images] ([is_main], [id], [product_id], [cloudinary_public_id], [image_url]) VALUES (0, 51, 2, NULL, N'https://images.unsplash.com/photo-1490750967868-88aa4486c946?w=800');
INSERT INTO dbo.[product_images] ([is_main], [id], [product_id], [cloudinary_public_id], [image_url]) VALUES (0, 52, 2, NULL, N'https://images.unsplash.com/photo-1520763185298-1b434c919102?w=800');
INSERT INTO dbo.[product_images] ([is_main], [id], [product_id], [cloudinary_public_id], [image_url]) VALUES (1, 53, 3, NULL, N'https://images.unsplash.com/photo-1597848212624-a19eb35e2651?w=800');
INSERT INTO dbo.[product_images] ([is_main], [id], [product_id], [cloudinary_public_id], [image_url]) VALUES (0, 54, 3, NULL, N'https://images.unsplash.com/photo-1508610048659-a06b669e3321?w=800');
INSERT INTO dbo.[product_images] ([is_main], [id], [product_id], [cloudinary_public_id], [image_url]) VALUES (0, 55, 3, NULL, N'https://images.unsplash.com/photo-1601985705806-5b9a71f6004f?w=800');
INSERT INTO dbo.[product_images] ([is_main], [id], [product_id], [cloudinary_public_id], [image_url]) VALUES (0, 56, 3, NULL, N'https://images.unsplash.com/photo-1470240731273-7821a6eeb6bd?w=800');
INSERT INTO dbo.[product_images] ([is_main], [id], [product_id], [cloudinary_public_id], [image_url]) VALUES (1, 57, 4, NULL, N'https://images.unsplash.com/photo-1561181286-d3fee7d55364?w=800');
INSERT INTO dbo.[product_images] ([is_main], [id], [product_id], [cloudinary_public_id], [image_url]) VALUES (0, 58, 4, NULL, N'https://images.unsplash.com/photo-1508615039623-a25605d2b022?w=800');
INSERT INTO dbo.[product_images] ([is_main], [id], [product_id], [cloudinary_public_id], [image_url]) VALUES (0, 59, 4, NULL, N'https://images.unsplash.com/photo-1582794543462-0d7922e50cf5?w=800');
INSERT INTO dbo.[product_images] ([is_main], [id], [product_id], [cloudinary_public_id], [image_url]) VALUES (0, 60, 4, NULL, N'https://images.unsplash.com/photo-1487070183336-b863922373d4?w=800');
INSERT INTO dbo.[product_images] ([is_main], [id], [product_id], [cloudinary_public_id], [image_url]) VALUES (1, 61, 5, NULL, N'https://images.unsplash.com/photo-1525310072745-f49212b5ac6d?w=800');
INSERT INTO dbo.[product_images] ([is_main], [id], [product_id], [cloudinary_public_id], [image_url]) VALUES (0, 62, 5, NULL, N'https://images.unsplash.com/photo-1567696911980-2eed69a46042?w=800');
INSERT INTO dbo.[product_images] ([is_main], [id], [product_id], [cloudinary_public_id], [image_url]) VALUES (0, 63, 5, NULL, N'https://images.unsplash.com/photo-1534447677768-be436bb09401?w=800');
INSERT INTO dbo.[product_images] ([is_main], [id], [product_id], [cloudinary_public_id], [image_url]) VALUES (0, 64, 5, NULL, N'https://images.unsplash.com/photo-1507290439931-a861b5a38200?w=800');
INSERT INTO dbo.[product_images] ([is_main], [id], [product_id], [cloudinary_public_id], [image_url]) VALUES (1, 65, 6, NULL, N'https://images.unsplash.com/photo-1533616688419-b7a585564566?w=800');
INSERT INTO dbo.[product_images] ([is_main], [id], [product_id], [cloudinary_public_id], [image_url]) VALUES (0, 66, 6, NULL, N'https://images.unsplash.com/photo-1513519245088-0e12902e5a38?w=800');
INSERT INTO dbo.[product_images] ([is_main], [id], [product_id], [cloudinary_public_id], [image_url]) VALUES (0, 67, 6, NULL, N'https://images.unsplash.com/photo-1469371670807-013ccf25f16a?w=800');
INSERT INTO dbo.[product_images] ([is_main], [id], [product_id], [cloudinary_public_id], [image_url]) VALUES (0, 68, 6, NULL, N'https://images.unsplash.com/photo-1527061011665-3652c757a4d4?w=800');
INSERT INTO dbo.[product_images] ([is_main], [id], [product_id], [cloudinary_public_id], [image_url]) VALUES (1, 69, 7, NULL, N'https://images.unsplash.com/photo-1527061011665-3652c757a4d4?w=800');
INSERT INTO dbo.[product_images] ([is_main], [id], [product_id], [cloudinary_public_id], [image_url]) VALUES (0, 70, 7, NULL, N'https://images.unsplash.com/photo-1513519245088-0e12902e5a38?w=800');
INSERT INTO dbo.[product_images] ([is_main], [id], [product_id], [cloudinary_public_id], [image_url]) VALUES (0, 71, 7, NULL, N'https://images.unsplash.com/photo-1469371670807-013ccf25f16a?w=800');
INSERT INTO dbo.[product_images] ([is_main], [id], [product_id], [cloudinary_public_id], [image_url]) VALUES (0, 72, 7, NULL, N'https://images.unsplash.com/photo-1533616688419-b7a585564566?w=800');
INSERT INTO dbo.[product_images] ([is_main], [id], [product_id], [cloudinary_public_id], [image_url]) VALUES (1, 73, 8, NULL, N'https://images.unsplash.com/photo-1545232979-8bf68ee9b1af?w=800');
INSERT INTO dbo.[product_images] ([is_main], [id], [product_id], [cloudinary_public_id], [image_url]) VALUES (0, 74, 8, NULL, N'https://images.unsplash.com/photo-1522673607200-164d1b6ce486?w=800');
INSERT INTO dbo.[product_images] ([is_main], [id], [product_id], [cloudinary_public_id], [image_url]) VALUES (0, 75, 8, NULL, N'https://images.unsplash.com/photo-1519741497674-611481863552?w=800');
INSERT INTO dbo.[product_images] ([is_main], [id], [product_id], [cloudinary_public_id], [image_url]) VALUES (0, 76, 8, NULL, N'https://images.unsplash.com/photo-1537633552985-df8429e8048b?w=800');
INSERT INTO dbo.[product_images] ([is_main], [id], [product_id], [cloudinary_public_id], [image_url]) VALUES (1, 77, 9, NULL, N'https://images.unsplash.com/photo-1566838318109-a8bffb91d082?w=800');
INSERT INTO dbo.[product_images] ([is_main], [id], [product_id], [cloudinary_public_id], [image_url]) VALUES (0, 78, 9, NULL, N'https://images.unsplash.com/photo-1509316975850-ff9c5deb0cd9?w=800');
INSERT INTO dbo.[product_images] ([is_main], [id], [product_id], [cloudinary_public_id], [image_url]) VALUES (0, 79, 9, NULL, N'https://images.unsplash.com/photo-1512428813834-c702c7702b78?w=800');
INSERT INTO dbo.[product_images] ([is_main], [id], [product_id], [cloudinary_public_id], [image_url]) VALUES (0, 80, 9, NULL, N'https://images.unsplash.com/photo-1530595467537-0b5996c41f2d?w=800');
INSERT INTO dbo.[product_images] ([is_main], [id], [product_id], [cloudinary_public_id], [image_url]) VALUES (1, 81, 10, NULL, N'https://images.unsplash.com/photo-1532712938310-34cb3982ef74?w=800');
INSERT INTO dbo.[product_images] ([is_main], [id], [product_id], [cloudinary_public_id], [image_url]) VALUES (0, 82, 10, NULL, N'https://images.unsplash.com/photo-1465495976277-4387d4b0b4c6?w=800');
INSERT INTO dbo.[product_images] ([is_main], [id], [product_id], [cloudinary_public_id], [image_url]) VALUES (0, 83, 10, NULL, N'https://images.unsplash.com/photo-1520854221256-17451cc331bf?w=800');
INSERT INTO dbo.[product_images] ([is_main], [id], [product_id], [cloudinary_public_id], [image_url]) VALUES (0, 84, 10, NULL, N'https://images.unsplash.com/photo-1519741497674-611481863552?w=800');
INSERT INTO dbo.[product_images] ([is_main], [id], [product_id], [cloudinary_public_id], [image_url]) VALUES (1, 85, 11, NULL, N'https://images.unsplash.com/photo-1582794543462-0d7922e50cf5?w=800');
INSERT INTO dbo.[product_images] ([is_main], [id], [product_id], [cloudinary_public_id], [image_url]) VALUES (0, 86, 11, NULL, N'https://images.unsplash.com/photo-1579783900882-c0d3dad7b119?w=800');
INSERT INTO dbo.[product_images] ([is_main], [id], [product_id], [cloudinary_public_id], [image_url]) VALUES (0, 87, 11, NULL, N'https://images.unsplash.com/photo-1507290439931-a861b5a38200?w=800');
INSERT INTO dbo.[product_images] ([is_main], [id], [product_id], [cloudinary_public_id], [image_url]) VALUES (0, 88, 11, NULL, N'https://images.unsplash.com/photo-1470240731273-7821a6eeb6bd?w=800');
INSERT INTO dbo.[product_images] ([is_main], [id], [product_id], [cloudinary_public_id], [image_url]) VALUES (1, 89, 39, N'rental_shop_products/u6ndxlyceuogjpcmke91', N'https://res.cloudinary.com/rbw3u9td/image/upload/v1791983039/rental_shop_products/u6ndxlyceuogjpcmke91.webp');
INSERT INTO dbo.[product_images] ([is_main], [id], [product_id], [cloudinary_public_id], [image_url]) VALUES (1, 90, 40, N'rental_shop_products/xdy9szinrabhmbgvihzl', N'https://res.cloudinary.com/rbw3u9td/image/upload/v1792072370/rental_shop_products/xdy9szinrabhmbgvihzl.jpg');
INSERT INTO dbo.[product_images] ([is_main], [id], [product_id], [cloudinary_public_id], [image_url]) VALUES (1, 91, 1, N'rental_shop_products/ddqvni5yjcqkqfkvqkot', N'https://res.cloudinary.com/rbw3u9td/image/upload/v1792383962/rental_shop_products/ddqvni5yjcqkqfkvqkot.jpg');
INSERT INTO dbo.[product_images] ([is_main], [id], [product_id], [cloudinary_public_id], [image_url]) VALUES (1, 92, 2, N'rental_shop_products/ebs31w0xymkoz93usibx', N'https://res.cloudinary.com/rbw3u9td/image/upload/v1792384058/rental_shop_products/ebs31w0xymkoz93usibx.jpg');
INSERT INTO dbo.[product_images] ([is_main], [id], [product_id], [cloudinary_public_id], [image_url]) VALUES (0, 93, 2, N'rental_shop_products/agqsxpbr7kxhx3ezmwhm', N'https://res.cloudinary.com/rbw3u9td/image/upload/v1792384060/rental_shop_products/agqsxpbr7kxhx3ezmwhm.jpg');
INSERT INTO dbo.[product_images] ([is_main], [id], [product_id], [cloudinary_public_id], [image_url]) VALUES (1, 94, 2, N'rental_shop_products/jqvhp5thdhv3ildb9lqu', N'https://res.cloudinary.com/rbw3u9td/image/upload/v1792384087/rental_shop_products/jqvhp5thdhv3ildb9lqu.jpg');
INSERT INTO dbo.[product_images] ([is_main], [id], [product_id], [cloudinary_public_id], [image_url]) VALUES (1, 95, 2, N'rental_shop_products/nz3h08rearw8vzv8iunk', N'https://res.cloudinary.com/rbw3u9td/image/upload/v1792384105/rental_shop_products/nz3h08rearw8vzv8iunk.jpg');
INSERT INTO dbo.[product_images] ([is_main], [id], [product_id], [cloudinary_public_id], [image_url]) VALUES (1, 96, 2, N'rental_shop_products/uceivnonflmz43gblfts', N'https://res.cloudinary.com/rbw3u9td/image/upload/v1792384117/rental_shop_products/uceivnonflmz43gblfts.jpg');
INSERT INTO dbo.[product_images] ([is_main], [id], [product_id], [cloudinary_public_id], [image_url]) VALUES (1, 97, 3, N'rental_shop_products/x90f3ggwdoxpczvsxkqc', N'https://res.cloudinary.com/rbw3u9td/image/upload/v1792384151/rental_shop_products/x90f3ggwdoxpczvsxkqc.jpg');
INSERT INTO dbo.[product_images] ([is_main], [id], [product_id], [cloudinary_public_id], [image_url]) VALUES (1, 98, 4, N'rental_shop_products/xg9cabvsafvmqsxvt2wj', N'https://res.cloudinary.com/rbw3u9td/image/upload/v1792384192/rental_shop_products/xg9cabvsafvmqsxvt2wj.jpg');
INSERT INTO dbo.[product_images] ([is_main], [id], [product_id], [cloudinary_public_id], [image_url]) VALUES (1, 99, 5, N'rental_shop_products/e5hqbkxlvqjoviezvsth', N'https://res.cloudinary.com/rbw3u9td/image/upload/v1792384276/rental_shop_products/e5hqbkxlvqjoviezvsth.jpg');
INSERT INTO dbo.[product_images] ([is_main], [id], [product_id], [cloudinary_public_id], [image_url]) VALUES (1, 100, 6, N'rental_shop_products/ivxwxa6crszqvsnfgdit', N'https://res.cloudinary.com/rbw3u9td/image/upload/v1792384363/rental_shop_products/ivxwxa6crszqvsnfgdit.jpg');
INSERT INTO dbo.[product_images] ([is_main], [id], [product_id], [cloudinary_public_id], [image_url]) VALUES (1, 101, 7, N'rental_shop_products/vyw45oqb0kfdchhbb7to', N'https://res.cloudinary.com/rbw3u9td/image/upload/v1792384482/rental_shop_products/vyw45oqb0kfdchhbb7to.jpg');
INSERT INTO dbo.[product_images] ([is_main], [id], [product_id], [cloudinary_public_id], [image_url]) VALUES (1, 102, 9, N'rental_shop_products/cpbimzsgscvrppee5or8', N'https://res.cloudinary.com/rbw3u9td/image/upload/v1792384593/rental_shop_products/cpbimzsgscvrppee5or8.jpg');
INSERT INTO dbo.[product_images] ([is_main], [id], [product_id], [cloudinary_public_id], [image_url]) VALUES (1, 103, 10, N'rental_shop_products/qint0xlpa2pu3elhc1ss', N'https://res.cloudinary.com/rbw3u9td/image/upload/v1792384648/rental_shop_products/qint0xlpa2pu3elhc1ss.jpg');
INSERT INTO dbo.[product_images] ([is_main], [id], [product_id], [cloudinary_public_id], [image_url]) VALUES (1, 104, 10, N'rental_shop_products/gfsiejjejspesjnioyfx', N'https://res.cloudinary.com/rbw3u9td/image/upload/v1792384661/rental_shop_products/gfsiejjejspesjnioyfx.jpg');
INSERT INTO dbo.[product_images] ([is_main], [id], [product_id], [cloudinary_public_id], [image_url]) VALUES (1, 105, 11, N'rental_shop_products/cpf9cbo5od1f9asulv6d', N'https://res.cloudinary.com/rbw3u9td/image/upload/v1792384710/rental_shop_products/cpf9cbo5od1f9asulv6d.jpg');
INSERT INTO dbo.[product_images] ([is_main], [id], [product_id], [cloudinary_public_id], [image_url]) VALUES (1, 106, 12, N'rental_shop_products/ijutawdn72vifedaga3j', N'https://res.cloudinary.com/rbw3u9td/image/upload/v1792384757/rental_shop_products/ijutawdn72vifedaga3j.jpg');
INSERT INTO dbo.[product_images] ([is_main], [id], [product_id], [cloudinary_public_id], [image_url]) VALUES (1, 107, 14, N'rental_shop_products/wonhckaunotusyyokmfk', N'https://res.cloudinary.com/rbw3u9td/image/upload/v1792384830/rental_shop_products/wonhckaunotusyyokmfk.jpg');
INSERT INTO dbo.[product_images] ([is_main], [id], [product_id], [cloudinary_public_id], [image_url]) VALUES (1, 108, 15, N'rental_shop_products/lr1efmkngjsdg7cyuasj', N'https://res.cloudinary.com/rbw3u9td/image/upload/v1792384868/rental_shop_products/lr1efmkngjsdg7cyuasj.jpg');
INSERT INTO dbo.[product_images] ([is_main], [id], [product_id], [cloudinary_public_id], [image_url]) VALUES (1, 109, 16, N'rental_shop_products/ophwai7qpouohknx0kkw', N'https://res.cloudinary.com/rbw3u9td/image/upload/v1792384920/rental_shop_products/ophwai7qpouohknx0kkw.jpg');
INSERT INTO dbo.[product_images] ([is_main], [id], [product_id], [cloudinary_public_id], [image_url]) VALUES (1, 110, 21, N'rental_shop_products/jqeoylnxs9amupcy3fpl', N'https://res.cloudinary.com/rbw3u9td/image/upload/v1792385304/rental_shop_products/jqeoylnxs9amupcy3fpl.jpg');
INSERT INTO dbo.[product_images] ([is_main], [id], [product_id], [cloudinary_public_id], [image_url]) VALUES (1, 111, 22, N'rental_shop_products/tu8yxjg3ol9iz0wlx7hl', N'https://res.cloudinary.com/rbw3u9td/image/upload/v1792385395/rental_shop_products/tu8yxjg3ol9iz0wlx7hl.jpg');
INSERT INTO dbo.[product_images] ([is_main], [id], [product_id], [cloudinary_public_id], [image_url]) VALUES (1, 112, 26, N'rental_shop_products/zlzdz7vlcirapjnkgtl4', N'https://res.cloudinary.com/rbw3u9td/image/upload/v1792385438/rental_shop_products/zlzdz7vlcirapjnkgtl4.jpg');
INSERT INTO dbo.[product_images] ([is_main], [id], [product_id], [cloudinary_public_id], [image_url]) VALUES (1, 113, 27, N'rental_shop_products/hdobmj2kvr3o1w6xddc0', N'https://res.cloudinary.com/rbw3u9td/image/upload/v1792385630/rental_shop_products/hdobmj2kvr3o1w6xddc0.jpg');
INSERT INTO dbo.[product_images] ([is_main], [id], [product_id], [cloudinary_public_id], [image_url]) VALUES (1, 114, 31, N'rental_shop_products/rd1hjq4szbwpu0fcyltc', N'https://res.cloudinary.com/rbw3u9td/image/upload/v1792385722/rental_shop_products/rd1hjq4szbwpu0fcyltc.jpg');
INSERT INTO dbo.[product_images] ([is_main], [id], [product_id], [cloudinary_public_id], [image_url]) VALUES (1, 115, 25, N'rental_shop_products/ll8zsohdh7hpendmuxl2', N'https://res.cloudinary.com/rbw3u9td/image/upload/v1792385805/rental_shop_products/ll8zsohdh7hpendmuxl2.jpg');
INSERT INTO dbo.[product_images] ([is_main], [id], [product_id], [cloudinary_public_id], [image_url]) VALUES (1, 116, 23, N'rental_shop_products/zrxs6m5fqegqnxrxvynh', N'https://res.cloudinary.com/rbw3u9td/image/upload/v1792386111/rental_shop_products/zrxs6m5fqegqnxrxvynh.jpg');
INSERT INTO dbo.[product_images] ([is_main], [id], [product_id], [cloudinary_public_id], [image_url]) VALUES (1, 117, 24, N'rental_shop_products/m0r7xzscuymz1zzbpgx6', N'https://res.cloudinary.com/rbw3u9td/image/upload/v1792386421/rental_shop_products/m0r7xzscuymz1zzbpgx6.jpg');
INSERT INTO dbo.[product_images] ([is_main], [id], [product_id], [cloudinary_public_id], [image_url]) VALUES (1, 118, 41, N'rental_shop_products/zb6eibhltm9ghojolzij', N'https://res.cloudinary.com/rbw3u9td/image/upload/v1792690534/rental_shop_products/zb6eibhltm9ghojolzij.jpg');
SET IDENTITY_INSERT dbo.[product_images] OFF;
GO

-- Dữ liệu bảng: product_style_values
INSERT INTO dbo.[product_style_values] ([product_id], [style_value_id]) VALUES (4, 8);
INSERT INTO dbo.[product_style_values] ([product_id], [style_value_id]) VALUES (5, 9);
INSERT INTO dbo.[product_style_values] ([product_id], [style_value_id]) VALUES (6, 13);
INSERT INTO dbo.[product_style_values] ([product_id], [style_value_id]) VALUES (6, 23);
INSERT INTO dbo.[product_style_values] ([product_id], [style_value_id]) VALUES (21, 2);
INSERT INTO dbo.[product_style_values] ([product_id], [style_value_id]) VALUES (21, 8);
INSERT INTO dbo.[product_style_values] ([product_id], [style_value_id]) VALUES (21, 15);
INSERT INTO dbo.[product_style_values] ([product_id], [style_value_id]) VALUES (21, 21);
INSERT INTO dbo.[product_style_values] ([product_id], [style_value_id]) VALUES (21, 22);
INSERT INTO dbo.[product_style_values] ([product_id], [style_value_id]) VALUES (21, 24);
INSERT INTO dbo.[product_style_values] ([product_id], [style_value_id]) VALUES (21, 25);
INSERT INTO dbo.[product_style_values] ([product_id], [style_value_id]) VALUES (21, 30);
INSERT INTO dbo.[product_style_values] ([product_id], [style_value_id]) VALUES (21, 33);
INSERT INTO dbo.[product_style_values] ([product_id], [style_value_id]) VALUES (21, 69);
INSERT INTO dbo.[product_style_values] ([product_id], [style_value_id]) VALUES (22, 3);
INSERT INTO dbo.[product_style_values] ([product_id], [style_value_id]) VALUES (22, 8);
INSERT INTO dbo.[product_style_values] ([product_id], [style_value_id]) VALUES (22, 15);
INSERT INTO dbo.[product_style_values] ([product_id], [style_value_id]) VALUES (22, 26);
INSERT INTO dbo.[product_style_values] ([product_id], [style_value_id]) VALUES (22, 30);
INSERT INTO dbo.[product_style_values] ([product_id], [style_value_id]) VALUES (22, 32);
INSERT INTO dbo.[product_style_values] ([product_id], [style_value_id]) VALUES (23, 2);
INSERT INTO dbo.[product_style_values] ([product_id], [style_value_id]) VALUES (23, 7);
INSERT INTO dbo.[product_style_values] ([product_id], [style_value_id]) VALUES (23, 8);
INSERT INTO dbo.[product_style_values] ([product_id], [style_value_id]) VALUES (23, 24);
INSERT INTO dbo.[product_style_values] ([product_id], [style_value_id]) VALUES (23, 34);
INSERT INTO dbo.[product_style_values] ([product_id], [style_value_id]) VALUES (23, 69);
INSERT INTO dbo.[product_style_values] ([product_id], [style_value_id]) VALUES (23, 70);
INSERT INTO dbo.[product_style_values] ([product_id], [style_value_id]) VALUES (24, 2);
INSERT INTO dbo.[product_style_values] ([product_id], [style_value_id]) VALUES (24, 9);
INSERT INTO dbo.[product_style_values] ([product_id], [style_value_id]) VALUES (24, 15);
INSERT INTO dbo.[product_style_values] ([product_id], [style_value_id]) VALUES (24, 21);
INSERT INTO dbo.[product_style_values] ([product_id], [style_value_id]) VALUES (24, 28);
INSERT INTO dbo.[product_style_values] ([product_id], [style_value_id]) VALUES (24, 71);
INSERT INTO dbo.[product_style_values] ([product_id], [style_value_id]) VALUES (27, 4);
INSERT INTO dbo.[product_style_values] ([product_id], [style_value_id]) VALUES (27, 8);
INSERT INTO dbo.[product_style_values] ([product_id], [style_value_id]) VALUES (27, 15);
INSERT INTO dbo.[product_style_values] ([product_id], [style_value_id]) VALUES (27, 21);
INSERT INTO dbo.[product_style_values] ([product_id], [style_value_id]) VALUES (27, 22);
INSERT INTO dbo.[product_style_values] ([product_id], [style_value_id]) VALUES (27, 24);
INSERT INTO dbo.[product_style_values] ([product_id], [style_value_id]) VALUES (27, 25);
INSERT INTO dbo.[product_style_values] ([product_id], [style_value_id]) VALUES (27, 26);
INSERT INTO dbo.[product_style_values] ([product_id], [style_value_id]) VALUES (27, 29);
INSERT INTO dbo.[product_style_values] ([product_id], [style_value_id]) VALUES (27, 33);
INSERT INTO dbo.[product_style_values] ([product_id], [style_value_id]) VALUES (39, 8);
INSERT INTO dbo.[product_style_values] ([product_id], [style_value_id]) VALUES (39, 22);
INSERT INTO dbo.[product_style_values] ([product_id], [style_value_id]) VALUES (39, 24);
INSERT INTO dbo.[product_style_values] ([product_id], [style_value_id]) VALUES (40, 3);
INSERT INTO dbo.[product_style_values] ([product_id], [style_value_id]) VALUES (40, 7);
INSERT INTO dbo.[product_style_values] ([product_id], [style_value_id]) VALUES (40, 8);
INSERT INTO dbo.[product_style_values] ([product_id], [style_value_id]) VALUES (40, 15);
INSERT INTO dbo.[product_style_values] ([product_id], [style_value_id]) VALUES (40, 21);
INSERT INTO dbo.[product_style_values] ([product_id], [style_value_id]) VALUES (40, 22);
INSERT INTO dbo.[product_style_values] ([product_id], [style_value_id]) VALUES (40, 23);
INSERT INTO dbo.[product_style_values] ([product_id], [style_value_id]) VALUES (40, 24);
INSERT INTO dbo.[product_style_values] ([product_id], [style_value_id]) VALUES (40, 25);
INSERT INTO dbo.[product_style_values] ([product_id], [style_value_id]) VALUES (40, 26);
INSERT INTO dbo.[product_style_values] ([product_id], [style_value_id]) VALUES (40, 29);
INSERT INTO dbo.[product_style_values] ([product_id], [style_value_id]) VALUES (40, 33);
INSERT INTO dbo.[product_style_values] ([product_id], [style_value_id]) VALUES (41, 8);
INSERT INTO dbo.[product_style_values] ([product_id], [style_value_id]) VALUES (41, 15);
INSERT INTO dbo.[product_style_values] ([product_id], [style_value_id]) VALUES (41, 21);
INSERT INTO dbo.[product_style_values] ([product_id], [style_value_id]) VALUES (41, 24);
INSERT INTO dbo.[product_style_values] ([product_id], [style_value_id]) VALUES (41, 33);
INSERT INTO dbo.[product_style_values] ([product_id], [style_value_id]) VALUES (41, 73);
INSERT INTO dbo.[product_style_values] ([product_id], [style_value_id]) VALUES (41, 77);
GO

-- Dữ liệu bảng: product_wishlists
SET IDENTITY_INSERT dbo.[product_wishlists] ON;
INSERT INTO dbo.[product_wishlists] ([created_at], [id], [product_id], [user_id]) VALUES ('2026-10-21 21:17:44.653', 4, 21, 37);
SET IDENTITY_INSERT dbo.[product_wishlists] OFF;
GO

-- Dữ liệu bảng: products
SET IDENTITY_INSERT dbo.[products] ON;
INSERT INTO dbo.[products] ([deposit_fee], [import_price], [is_active], [is_deleted], [is_selling], [price_buy], [price_rent_per_day], [promotional_price], [rating], [sold], [view_count], [category_id], [created_at], [id], [store_id], [name], [image_url], [description]) VALUES (0.00, 480000.00, 1, NULL, 1, 850000.00, 0.00, NULL, 4.8, 85, 2, 7, NULL, 2, NULL, N'Bó Hoa Tulip Hồng', N'https://res.cloudinary.com/rbw3u9td/image/upload/v1792384117/rental_shop_products/uceivnonflmz43gblfts.jpg', N'15 bông tulip Hà Lan màu hồng pastel và trắng ngọc, biểu trưng cho tình yêu chân thành và trang nhã.');
INSERT INTO dbo.[products] ([deposit_fee], [import_price], [is_active], [is_deleted], [is_selling], [price_buy], [price_rent_per_day], [promotional_price], [rating], [sold], [view_count], [category_id], [created_at], [id], [store_id], [name], [image_url], [description]) VALUES (0.00, 220000.00, 1, NULL, 1, 420000.00, 0.00, NULL, 4.9, 95, 1, 7, NULL, 3, NULL, N'Bó Hoa Hướng Dương Tana', N'https://res.cloudinary.com/rbw3u9td/image/upload/v1792384151/rental_shop_products/x90f3ggwdoxpczvsxkqc.jpg', N'5 bông hướng dương Đà Lạt loại 1 phối hoa cúc tana và lá bạc nhập khẩu, mang ý nghĩa may mắn và thành công.');
INSERT INTO dbo.[products] ([deposit_fee], [import_price], [is_active], [is_deleted], [is_selling], [price_buy], [price_rent_per_day], [promotional_price], [rating], [sold], [view_count], [category_id], [created_at], [id], [store_id], [name], [image_url], [description]) VALUES (0.00, 420000.00, 1, NULL, 1, 750000.00, 0.00, NULL, 4.7, 45, 0, 31, NULL, 4, NULL, N'Bó Hoa Cẩm Tú Cầu', N'https://res.cloudinary.com/rbw3u9td/image/upload/v1792384192/rental_shop_products/xg9cabvsafvmqsxvt2wj.jpg', N'Giỏ mây vintage kết hợp hoa cẩm tú cầu xanh lam, hoa hồng kem và hoa cát tường dịu dàng.');
INSERT INTO dbo.[products] ([deposit_fee], [import_price], [is_active], [is_deleted], [is_selling], [price_buy], [price_rent_per_day], [promotional_price], [rating], [sold], [view_count], [category_id], [created_at], [id], [store_id], [name], [image_url], [description]) VALUES (0.00, 520000.00, 1, NULL, 1, 920000.00, 0.00, NULL, 5.0, 110, 7, 24, NULL, 5, NULL, N'Giỏ Hoa Hồng Juliet', N'https://res.cloudinary.com/rbw3u9td/image/upload/v1792384276/rental_shop_products/e5hqbkxlvqjoviezvsth.jpg', N'Hộp hoa mica trong suốt cao cấp phối hoa hồng phấn Juliet và baby khô cao cấp lưu giữ vẻ đẹp lâu dài.');
INSERT INTO dbo.[products] ([deposit_fee], [import_price], [is_active], [is_deleted], [is_selling], [price_buy], [price_rent_per_day], [promotional_price], [rating], [sold], [view_count], [category_id], [created_at], [id], [store_id], [name], [image_url], [description]) VALUES (0.00, 950000.00, 1, NULL, 1, 1650000.00, 0.00, NULL, 4.8, 64, 1, 8, NULL, 6, NULL, N'Kệ Hoa Khai Trương 2 Tầng', N'https://res.cloudinary.com/rbw3u9td/image/upload/v1792384363/rental_shop_products/ivxwxa6crszqvsnfgdit.jpg', N'Kệ hoa 2 tầng cao 1.8m phối hoa hồng môn đỏ, hướng dương và hoa đồng tiền rực rỡ mang tài lộc dồi dào.');
INSERT INTO dbo.[products] ([deposit_fee], [import_price], [is_active], [is_deleted], [is_selling], [price_buy], [price_rent_per_day], [promotional_price], [rating], [sold], [view_count], [category_id], [created_at], [id], [store_id], [name], [image_url], [description]) VALUES (0.00, 800000.00, 1, NULL, 1, 1350000.00, 0.00, NULL, 4.9, 52, 1, 8, NULL, 7, NULL, N'Kệ Hoa Khai Trương Tone Cam', N'https://res.cloudinary.com/rbw3u9td/image/upload/v1792384482/rental_shop_products/vyw45oqb0kfdchhbb7to.jpg', N'Thiết kế hiện đại tone màu cam vàng hoàng kim, chúc mừng đối tác kinh doanh phát đạt thịnh vượng.');
INSERT INTO dbo.[products] ([deposit_fee], [import_price], [is_active], [is_deleted], [is_selling], [price_buy], [price_rent_per_day], [promotional_price], [rating], [sold], [view_count], [category_id], [created_at], [id], [store_id], [name], [image_url], [description]) VALUES (0.00, 650000.00, 1, NULL, 1, 1150000.00, 0.00, NULL, 4.8, 38, 1, 7, NULL, 8, NULL, N'Bó Hoa Cưới Calla Lily', N'https://images.unsplash.com/photo-1533616688419-b7a585564566?w=800&q=80', N'Hoa rum Calla Lily trắng nhập khẩu kết hợp nơ lụa voan cao cấp, tôn vinh vẻ đẹp thuần khiết của cô dâu.');
INSERT INTO dbo.[products] ([deposit_fee], [import_price], [is_active], [is_deleted], [is_selling], [price_buy], [price_rent_per_day], [promotional_price], [rating], [sold], [view_count], [category_id], [created_at], [id], [store_id], [name], [image_url], [description]) VALUES (0.00, 1200000.00, 1, NULL, 1, 2100000.00, 0.00, NULL, 4.9, 25, 1, 31, NULL, 9, NULL, N'Chậu Lan Hồ Điệp Tím', N'https://res.cloudinary.com/rbw3u9td/image/upload/v1792384593/rental_shop_products/cpbimzsgscvrppee5or8.jpg', N'Chậu sứ Bát Tràng cao cấp cắm 5 cành lan hồ điệp tím viền trắng cánh dày, hoa bền tươi trên 1 tháng.');
INSERT INTO dbo.[products] ([deposit_fee], [import_price], [is_active], [is_deleted], [is_selling], [price_buy], [price_rent_per_day], [promotional_price], [rating], [sold], [view_count], [category_id], [created_at], [id], [store_id], [name], [image_url], [description]) VALUES (500000.00, 1500000.00, 1, NULL, 1, 2400000.00, 150000.00, NULL, 5.0, 0, 4, 8, NULL, 11, NULL, N'Chân Kệ Bình Hoa Pha Lê', N'https://res.cloudinary.com/rbw3u9td/image/upload/v1792384710/rental_shop_products/cpf9cbo5od1f9asulv6d.jpg', N'Bộ 2 chân kệ sắt nghệ thuật sơn tĩnh điện cao cấp kèm bình cắm pha lê phục vụ tiệc cưới hội nghị.');
INSERT INTO dbo.[products] ([deposit_fee], [import_price], [is_active], [is_deleted], [is_selling], [price_buy], [price_rent_per_day], [promotional_price], [rating], [sold], [view_count], [category_id], [created_at], [id], [store_id], [name], [image_url], [description]) VALUES (500000.00, 700000.00, 1, NULL, 1, 1200000.00, 150000.00, NULL, 4.9, 85, 2, 24, '2026-10-01 09:25:42.694488', 12, 1, N'Bó Hoa Hồng Đỏ 99 Bông', N'https://res.cloudinary.com/rbw3u9td/image/upload/v1792384757/rental_shop_products/ijutawdn72vifedaga3j.jpg', N'Bó hoa hồng đỏ nhập khẩu Ecuador size bông cực lớn, cánh dày nhung mịn, kết hợp lá bạc Dollar sang trọng biểu tượng tình yêu vĩnh cửu bất diệt.');
INSERT INTO dbo.[products] ([deposit_fee], [import_price], [is_active], [is_deleted], [is_selling], [price_buy], [price_rent_per_day], [promotional_price], [rating], [sold], [view_count], [category_id], [created_at], [id], [store_id], [name], [image_url], [description]) VALUES (200000.00, 250000.00, 1, NULL, 1, 480000.00, 60000.00, NULL, 4.9, 95, 1, 7, '2026-10-01 09:25:43.022418', 13, 1, N'Bó Hoa Hướng Dương Vintage', N'https://images.unsplash.com/photo-1597848212624-a19eb35e2651?w=800&q=80', N'Bó hoa hướng dương 5 bông size lớn gói giấy báo vintage phối kèm hoa cúc tana, hoa phi yến trắng tượng trưng cho niềm tin và hoài bão vươn xa.');
INSERT INTO dbo.[products] ([deposit_fee], [import_price], [is_active], [is_deleted], [is_selling], [price_buy], [price_rent_per_day], [promotional_price], [rating], [sold], [view_count], [category_id], [created_at], [id], [store_id], [name], [image_url], [description]) VALUES (800000.00, 1000000.00, 1, NULL, 1, 1850000.00, 200000.00, NULL, 4.8, 64, 0, 8, '2026-10-01 09:25:43.178948', 14, 1, N'Kệ Hoa Khai Trương Đồng Tiền', N'https://res.cloudinary.com/rbw3u9td/image/upload/v1792384830/rental_shop_products/wonhckaunotusyyokmfk.jpg', N'Kệ hoa thiết kế 2 tầng kết hợp hoa đồng tiền, hoa hướng dương rực rỡ và lan hồ điệp vàng mang lại nhiều may mắn cho ngày khởi đầu kinh doanh.');
INSERT INTO dbo.[products] ([deposit_fee], [import_price], [is_active], [is_deleted], [is_selling], [price_buy], [price_rent_per_day], [promotional_price], [rating], [sold], [view_count], [category_id], [created_at], [id], [store_id], [name], [image_url], [description]) VALUES (400000.00, 550000.00, 1, NULL, 0, 950000.00, 120000.00, NULL, 4.9, 58, 1, 8, '2026-10-01 09:25:43.232812', 15, 1, N'Giỏ Hoa Tone Cam', N'https://res.cloudinary.com/rbw3u9td/image/upload/v1792384868/rental_shop_products/lr1efmkngjsdg7cyuasj.jpg', N'Giỏ hoa thiết kế phối hoa đồng tiền, hướng dương và hoa hồng cam lãng mạn mang đến tài lộc khởi sắc cho chi nhánh mới.');
INSERT INTO dbo.[products] ([deposit_fee], [import_price], [is_active], [is_deleted], [is_selling], [price_buy], [price_rent_per_day], [promotional_price], [rating], [sold], [view_count], [category_id], [created_at], [id], [store_id], [name], [image_url], [description]) VALUES (350000.00, 480000.00, 1, NULL, 1, 850000.00, 100000.00, NULL, 5.0, 72, 0, 31, '2026-10-01 09:25:43.258384', 16, 1, N'Hộp Hoa Hồng Đỏ & Baby', N'https://res.cloudinary.com/rbw3u9td/image/upload/v1792384920/rental_shop_products/ophwai7qpouohknx0kkw.jpg', N'Hộp hoa nhung tròn cao cấp cắm kín hoa hồng đỏ nhung cùng hoa baby trắng biểu tượng tình yêu sâu đậm và nồng cháy.');
INSERT INTO dbo.[products] ([deposit_fee], [import_price], [is_active], [is_deleted], [is_selling], [price_buy], [price_rent_per_day], [promotional_price], [rating], [sold], [view_count], [category_id], [created_at], [id], [store_id], [name], [image_url], [description]) VALUES (1200000.00, 1400000.00, 1, 1, 0, 2500000.00, 280000.00, NULL, 4.9, 34, 0, 31, '2026-10-01 09:25:43.514088', 17, 1, N'Chậu Lan Hồ Điệp Trắng', N'https://images.unsplash.com/photo-1533616688419-b7a585564566?w=800&q=80', N'Chậu lan hồ điệp trắng nhập khẩu cành dài hoa to kết hợp chậu sứ vân đá cẩm thạch sang trọng thích hợp biếu đối tác doanh nghiệp.');
INSERT INTO dbo.[products] ([deposit_fee], [import_price], [is_active], [is_deleted], [is_selling], [price_buy], [price_rent_per_day], [promotional_price], [rating], [sold], [view_count], [category_id], [created_at], [id], [store_id], [name], [image_url], [description]) VALUES (300000.00, 420000.00, 1, NULL, 1, 780000.00, 90000.00, NULL, 4.8, 46, 0, 30, '2026-10-01 09:25:43.556012', 18, 1, N'Bình Hoa Hồng Ohara', N'https://images.unsplash.com/photo-1582794543139-8ac9cb0f7b11?w=800&q=80', N'Bình gốm sứ mạ viền vàng cắm hoa hồng ngoại Ohara thơm ngát phối cúc mẫu đơn Hà Lan trưng phòng khách đẳng cấp.');
INSERT INTO dbo.[products] ([deposit_fee], [import_price], [is_active], [is_deleted], [is_selling], [price_buy], [price_rent_per_day], [promotional_price], [rating], [sold], [view_count], [category_id], [created_at], [id], [store_id], [name], [image_url], [description]) VALUES (400000.00, 500000.00, 1, NULL, 1, 890000.00, 120000.00, NULL, 4.9, 51, 0, 25, '2026-10-01 09:25:43.585045', 19, 1, N'Bó Hoa Cưới Tulip Trắng', N'https://images.unsplash.com/photo-1533616688419-b7a585564566?w=800&q=80', N'Bó hoa cưới tulip trắng phối mao lương và hoa hồng trắng quấn dải ruy băng lụa satin tôn vinh nét đẹp tinh khôi ngày cưới.');
INSERT INTO dbo.[products] ([deposit_fee], [import_price], [is_active], [is_deleted], [is_selling], [price_buy], [price_rent_per_day], [promotional_price], [rating], [sold], [view_count], [category_id], [created_at], [id], [store_id], [name], [image_url], [description]) VALUES (500000.00, 650000.00, 1, NULL, 1, 1150000.00, 150000.00, NULL, 4.8, 29, 0, 28, '2026-10-01 09:25:43.599046', 20, 1, N'Kệ Hoa Tang Lễ Trắng Tím', N'https://images.unsplash.com/photo-1533616688419-b7a585564566?w=800&q=80', N'Kệ hoa tang lễ thiết kế trang nghiêm phối hoa cúc trắng, hoa lan tím và hoa ly trắng gửi gắm niềm tiếc thương sâu sắc.');
INSERT INTO dbo.[products] ([deposit_fee], [import_price], [is_active], [is_deleted], [is_selling], [price_buy], [price_rent_per_day], [promotional_price], [rating], [sold], [view_count], [category_id], [created_at], [id], [store_id], [name], [image_url], [description]) VALUES (350000.00, 420000.00, 1, NULL, 0, 790000.00, 100000.00, NULL, 4.8, 55, 6, 8, '2026-10-01 09:25:43.88912', 38, 3, N'Kệ Hoa Khai Trương Ngũ Sắc', N'https://images.unsplash.com/photo-1526047932273-341f2a7631f9?w=800&q=80', N'Kệ hoa chân gỗ cắm hoa đồng tiền ngũ sắc và hoa hướng dương chúc mừng lễ thành lập câu lạc bộ, đội nhóm trường đại học.');
INSERT INTO dbo.[products] ([deposit_fee], [import_price], [is_active], [is_deleted], [is_selling], [price_buy], [price_rent_per_day], [promotional_price], [rating], [sold], [view_count], [category_id], [created_at], [id], [store_id], [name], [image_url], [description]) VALUES (2000000.00, 5500000.00, 1, NULL, 1, 8500000.00, 650000.00, NULL, 5.0, 0, 2, 25, NULL, 10, NULL, N'Khung Cổng Hoa Cưới Vòm', N'https://res.cloudinary.com/rbw3u9td/image/upload/v1792384661/rental_shop_products/gfsiejjejspesjnioyfx.jpg', N'Khung cổng hoa cưới thiết kế vòm tròn phong cách Bohemian nghệ thuật, hoa lụa cao cấp kết hợp hoa tươi.');
INSERT INTO dbo.[products] ([deposit_fee], [import_price], [is_active], [is_deleted], [is_selling], [price_buy], [price_rent_per_day], [promotional_price], [rating], [sold], [view_count], [category_id], [created_at], [id], [store_id], [name], [image_url], [description]) VALUES (400000.00, 550000.00, 0, 1, 0, 950000.00, 110000.00, NULL, 4.9, 68, 7, 24, '2026-10-01 09:25:43.656019', 23, NULL, N'Vườn Xuân Ngọt Ngào', N'https://res.cloudinary.com/rbw3u9td/image/upload/v1792386111/rental_shop_products/zrxs6m5fqegqnxrxvynh.jpg', N'Vườn Xuân Ngọt Ngào là bó hoa tươi tắn mang dáng một khu vườn nhỏ, phối nhiều loại hoa với gam hồng, trắng, kem điểm thêm sắc xanh dương và tím nhạt. Cúc đồng tiền hồng, cẩm chướng nhiều nếp, hồng bông, cúc daisy trắng và những bông hoa xanh pastel nhỏ xen kẽ nhau, được tô điểm bằng lá phụ xanh mảnh ở viền và gói bằng giấy kính trong suốt buộc ruy băng voan mềm. Bó hoa vui mắt, hợp để tặng sinh nhật, chúc mừng, tặng bạn bè, đồng nghiệp hoặc nhân dịp 20/10, 8/3.');
INSERT INTO dbo.[products] ([deposit_fee], [import_price], [is_active], [is_deleted], [is_selling], [price_buy], [price_rent_per_day], [promotional_price], [rating], [sold], [view_count], [category_id], [created_at], [id], [store_id], [name], [image_url], [description]) VALUES (300000.00, 250000.00, 1, NULL, 1, 350000.00, 90000.00, NULL, 4.9, 88, 0, 7, '2026-10-01 09:25:43.679539', 24, NULL, N'Sinh Nhật Ngọt Ngào', N'https://res.cloudinary.com/rbw3u9td/image/upload/v1792386421/rental_shop_products/m0r7xzscuymz1zzbpgx6.jpg', N'Hộp quà sinh nhật gồm bánh kem size mini phủ kem hồng phấn, viền kem trắng chảy nhẹ, trang trí hoa kem trắng xinh xắn, kèm dòng chữ chúc mừng viết theo yêu cầu. Bên cạnh là cụm hoa hồng pastel, hồng kem và cúc họa mi nhỏ, cắm gọn trong cùng một hộp hồng. Món quà trọn vẹn, đẹp mắt, sẵn sàng để tặng mà khách không phải chuẩn bị gì thêm.');
INSERT INTO dbo.[products] ([deposit_fee], [import_price], [is_active], [is_deleted], [is_selling], [price_buy], [price_rent_per_day], [promotional_price], [rating], [sold], [view_count], [category_id], [created_at], [id], [store_id], [name], [image_url], [description]) VALUES (850000.00, 1100000.00, 0, 1, 0, 1950000.00, 220000.00, NULL, 4.8, 53, 5, 8, '2026-10-01 09:25:43.699058', 25, 2, N'Kệ Hoa Khai Trương Tone Đỏ', N'https://res.cloudinary.com/rbw3u9td/image/upload/v1792385805/rental_shop_products/ll8zsohdh7hpendmuxl2.jpg', N'Kệ hoa chúc mừng khai trương tone đỏ dành tặng khai trương quán cafe, quán ăn, tạo nên không gian trang trọng và quý phái.');
INSERT INTO dbo.[products] ([deposit_fee], [import_price], [is_active], [is_deleted], [is_selling], [price_buy], [price_rent_per_day], [promotional_price], [rating], [sold], [view_count], [category_id], [created_at], [id], [store_id], [name], [image_url], [description]) VALUES (220000.00, 280000.00, 1, NULL, 1, 520000.00, 70000.00, NULL, 4.9, 91, 3, 24, '2026-10-01 09:25:43.762772', 28, 2, N'Bó Hoa Baby Trắng', N'https://images.unsplash.com/photo-1533616688419-b7a585564566?w=800&q=80', N'Bó hoa baby trắng khổng lồ bồng bềnh như mây, gói giấy Kraft tối giản tạo vẻ đẹp tinh tế thuần khiết.');
INSERT INTO dbo.[products] ([deposit_fee], [import_price], [is_active], [is_deleted], [is_selling], [price_buy], [price_rent_per_day], [promotional_price], [rating], [sold], [view_count], [category_id], [created_at], [id], [store_id], [name], [image_url], [description]) VALUES (250000.00, 340000.00, 1, NULL, 1, 620000.00, 80000.00, NULL, 4.9, 77, 0, 27, '2026-10-01 09:25:43.774895', 29, 2, N'Bó Hoa Hướng Dương Chuông Xanh', N'https://images.unsplash.com/photo-1597848212624-a19eb35e2651?w=800&q=80', N'Bó hoa hướng dương rực rỡ phối hoa chuông xanh và cúc tana chúc mừng tân gia, mừng thăng chức thành công rực rỡ.');
INSERT INTO dbo.[products] ([deposit_fee], [import_price], [is_active], [is_deleted], [is_selling], [price_buy], [price_rent_per_day], [promotional_price], [rating], [sold], [view_count], [category_id], [created_at], [id], [store_id], [name], [image_url], [description]) VALUES (1000000.00, 1200000.00, 1, NULL, 1, 2200000.00, 250000.00, NULL, 4.7, 38, 0, 31, '2026-10-01 09:25:43.798172', 30, 3, N'Chậu Lan Hồ Điệp Tím 5 Cành', N'https://images.unsplash.com/photo-1525310072745-f49212b5ac6d?w=800&q=80', N'Chậu men sứ cao cấp trồng 5 cành hoa lan hồ điệp tím rực rỡ, hoa nở đều đẹp, độ bền kéo dài từ 4 đến 8 tuần thích hợp trang trí sảnh phòng khách.');
INSERT INTO dbo.[products] ([deposit_fee], [import_price], [is_active], [is_deleted], [is_selling], [price_buy], [price_rent_per_day], [promotional_price], [rating], [sold], [view_count], [category_id], [created_at], [id], [store_id], [name], [image_url], [description]) VALUES (150000.00, 180000.00, 1, NULL, 1, 350000.00, 50000.00, NULL, 4.9, 110, 6, 7, '2026-10-01 09:25:43.820308', 32, 3, N'Bó Hoa Cúc Tana', N'https://images.unsplash.com/photo-1561181286-d3fee7d55364?w=800&q=80', N'Bó hoa cúc tana trắng cánh mỏng nhỏ xinh phối lá cỏ đồng nội nhẹ nhàng, món quà sinh nhật mộc mạc đáng yêu.');
INSERT INTO dbo.[products] ([deposit_fee], [import_price], [is_active], [is_deleted], [is_selling], [price_buy], [price_rent_per_day], [promotional_price], [rating], [sold], [view_count], [category_id], [created_at], [id], [store_id], [name], [image_url], [description]) VALUES (220000.00, 290000.00, 1, NULL, 1, 550000.00, 70000.00, NULL, 4.8, 82, 2, 31, '2026-10-01 09:25:43.831005', 33, 3, N'Giỏ Hoa Cẩm Chướng Pastel', N'https://images.unsplash.com/photo-1561181286-d3fee7d55364?w=800&q=80', N'Giỏ gỗ cắm hoa đồng tiền hồng pastel, cẩm chướng và hoa sao tím tri ân thầy cô giáo và chúc mừng bạn bè.');
INSERT INTO dbo.[products] ([deposit_fee], [import_price], [is_active], [is_deleted], [is_selling], [price_buy], [price_rent_per_day], [promotional_price], [rating], [sold], [view_count], [category_id], [created_at], [id], [store_id], [name], [image_url], [description]) VALUES (200000.00, 260000.00, 1, NULL, 1, 490000.00, 60000.00, NULL, 4.9, 67, 1, 31, '2026-10-01 09:25:43.844856', 34, 3, N'Hộp Hoa Tone Hồng', N'https://images.unsplash.com/photo-1561181286-d3fee7d55364?w=800&q=80', N'Hộp mica nắp trong suốt sang xịn, hoa tươi cắm bên trong thắt nơ lụa tinh xảo, thích hợp để bàn học, bàn làm việc.');
INSERT INTO dbo.[products] ([deposit_fee], [import_price], [is_active], [is_deleted], [is_selling], [price_buy], [price_rent_per_day], [promotional_price], [rating], [sold], [view_count], [category_id], [created_at], [id], [store_id], [name], [image_url], [description]) VALUES (380000.00, 450000.00, 1, NULL, 0, 850000.00, 110000.00, NULL, 4.8, 49, 4, 27, '2026-10-01 09:25:43.853966', 35, 3, N'Kệ Hoa Hướng Dương 1 Tầng', N'https://images.unsplash.com/photo-1526047932273-341f2a7631f9?w=800&q=80', N'Kệ hoa chân sắt gọn nhẹ cắm hoa hướng dương, đồng tiền đỏ và lan mokara tươi sáng cho sự kiện câu lạc bộ, khoa viện.');
INSERT INTO dbo.[products] ([deposit_fee], [import_price], [is_active], [is_deleted], [is_selling], [price_buy], [price_rent_per_day], [promotional_price], [rating], [sold], [view_count], [category_id], [created_at], [id], [store_id], [name], [image_url], [description]) VALUES (250000.00, 300000.00, 1, NULL, 1, 580000.00, 75000.00, NULL, 4.9, 76, 6, 24, '2026-10-01 09:25:43.873091', 37, 3, N'Bó Hoa Hồng Đỏ 20 Bông', N'/images/demo-rose-bouquet.jpg', N'Bó hoa hồng đỏ gói giấy đen nhám viền vàng phong cách lãng mạn giúp các bạn sinh viên tỏ tình thành công.');
INSERT INTO dbo.[products] ([deposit_fee], [import_price], [is_active], [is_deleted], [is_selling], [price_buy], [price_rent_per_day], [promotional_price], [rating], [sold], [view_count], [category_id], [created_at], [id], [store_id], [name], [image_url], [description]) VALUES (120000.00, 140000.00, 1, NULL, 1, 280000.00, 40000.00, NULL, 4.9, 98, 5, 30, '2026-10-01 09:25:43.863546', 36, 3, N'Bình Hoa Cúc Họa Mi', N'https://images.unsplash.com/photo-1582794543139-8ac9cb0f7b11?w=800&q=80', N'Bình thủy tinh nhỏ xinh cắm hoa cúc họa mi trắng thanh khiết, mang lại năng lượng tích cực cho góc học tập.');
INSERT INTO dbo.[products] ([deposit_fee], [import_price], [is_active], [is_deleted], [is_selling], [price_buy], [price_rent_per_day], [promotional_price], [rating], [sold], [view_count], [category_id], [created_at], [id], [store_id], [name], [image_url], [description]) VALUES (180000.00, 200000.00, 1, NULL, 1, 390000.00, 50000.00, NULL, 5.0, 135, 17, 26, '2026-10-01 09:25:43.810306', 31, 3, N'Bó Hoa Hướng Dương Tốt Nghiệp', N'https://res.cloudinary.com/rbw3u9td/image/upload/v1792385722/rental_shop_products/rd1hjq4szbwpu0fcyltc.jpg', N'Bó hoa hướng dương rực rỡ tặng lễ tốt nghiệp cử nhân.');
INSERT INTO dbo.[products] ([deposit_fee], [import_price], [is_active], [is_deleted], [is_selling], [price_buy], [price_rent_per_day], [promotional_price], [rating], [sold], [view_count], [category_id], [created_at], [id], [store_id], [name], [image_url], [description]) VALUES (1100000.00, 1300000.00, 1, NULL, 1, 2300000.00, 260000.00, NULL, 4.8, 39, 21, 31, '2026-10-01 09:25:43.721455', 26, 2, N'Chậu Lan Hồ Điệp Vàng', N'https://res.cloudinary.com/rbw3u9td/image/upload/v1792385438/rental_shop_products/zlzdz7vlcirapjnkgtl4.jpg', N'Chậu lan hồ điệp vàng hoàng yến cánh dày bóng mượt, tượng trưng cho tiền tài sung túc và phát triển bền vững.');
INSERT INTO dbo.[products] ([deposit_fee], [import_price], [is_active], [is_deleted], [is_selling], [price_buy], [price_rent_per_day], [promotional_price], [rating], [sold], [view_count], [category_id], [created_at], [id], [store_id], [name], [image_url], [description]) VALUES (280000.00, 380000.00, 1, NULL, 1, 690000.00, 80000.00, NULL, 4.7, 44, 5, 31, '2026-10-01 09:25:43.748787', 27, 2, N'Bó Hoa Cẩm Tú Cầu', N'https://res.cloudinary.com/rbw3u9td/image/upload/v1792385630/rental_shop_products/hdobmj2kvr3o1w6xddc0.jpg', N'Bình hoa cẩm tú cầu xanh bồng bềnh kết hợp hoa baby trắng tinh khiết tạo điểm nhấn trang nhã cho không gian sống.');
INSERT INTO dbo.[products] ([deposit_fee], [import_price], [is_active], [is_deleted], [is_selling], [price_buy], [price_rent_per_day], [promotional_price], [rating], [sold], [view_count], [category_id], [created_at], [id], [store_id], [name], [image_url], [description]) VALUES (36000.00, 72000.00, 1, 0, 1, 120000.00, 12000.00, 120000.00, 5.0, 0, 3, 31, '2026-10-14 20:04:00.511247', 39, 1, N'Bó Hoa Cẩm Tú Cầu Xanh', N'https://res.cloudinary.com/rbw3u9td/image/upload/v1791983039/rental_shop_products/u6ndxlyceuogjpcmke91.webp', N'Sản phẩm bao gồm :
Hoa cẩm tú cầu
Hoa tana
Lá phụ kèm');
INSERT INTO dbo.[products] ([deposit_fee], [import_price], [is_active], [is_deleted], [is_selling], [price_buy], [price_rent_per_day], [promotional_price], [rating], [sold], [view_count], [category_id], [created_at], [id], [store_id], [name], [image_url], [description]) VALUES (0.00, 450000.00, 1, 0, 1, 550000.00, 0.00, NULL, 5.0, 0, 0, 24, '2026-10-23 00:35:33.213264', 41, NULL, N'Ly Hồng Dịu Dàng', N'https://res.cloudinary.com/rbw3u9td/image/upload/v1792690534/rental_shop_products/zb6eibhltm9ghojolzij.jpg', N'Lily Hồng Dịu Dàng là bó hoa thanh lịch với những bông lily hồng phấn nở rộ xen lily kem và nụ xanh mềm mại, điểm thêm vài cụm hoa xanh pastel nhỏ xinh và lá phụ tươi mát. Bó hoa được gói bằng giấy trắng sữa bản lớn, buộc ruy băng hồng nhạt, tạo cảm giác nhẹ nhàng, sang trọng.');
INSERT INTO dbo.[products] ([deposit_fee], [import_price], [is_active], [is_deleted], [is_selling], [price_buy], [price_rent_per_day], [promotional_price], [rating], [sold], [view_count], [category_id], [created_at], [id], [store_id], [name], [image_url], [description]) VALUES (255000.00, 510000.00, 1, 0, 1, 850000.00, 85000.00, 799000.00, 5.0, 0, 2, 31, '2026-10-15 20:52:51.845844', 40, 1, N'Bó Hoa tone pastel', N'https://res.cloudinary.com/rbw3u9td/image/upload/v1792072370/rental_shop_products/xdy9szinrabhmbgvihzl.jpg', N'Hoa phụ: hồng ohara, thuỷ tiên, cát tường, thuý châu và một số lại hoa lá phụ khác.

Kích thước và tông màu sản phẩm có thể thay đổi theo yêu cầu của quý khách.
Bạn sẽ nhận được ảnh sản phẩm hoàn thiện trước khi chúng tôi tiến hành giao hoa.

Lưu ý: Sản phẩm thực tế có thể khác hình mẫu đôi chút do đặc tính thủ công và mùa hoa. Nếu có nhiều thay đổi, shop sẽ báo cho quý khách trước khi thực hiện.');
INSERT INTO dbo.[products] ([deposit_fee], [import_price], [is_active], [is_deleted], [is_selling], [price_buy], [price_rent_per_day], [promotional_price], [rating], [sold], [view_count], [category_id], [created_at], [id], [store_id], [name], [image_url], [description]) VALUES (400000.00, 500000.00, 1, NULL, 1, 890000.00, 120000.00, NULL, 4.8, 42, 23, 25, '2026-10-01 09:25:43.63055', 22, 2, N'Bó Hoa Cưới Tulip Tone Hồng', N'https://res.cloudinary.com/rbw3u9td/image/upload/v1792385395/rental_shop_products/tu8yxjg3ol9iz0wlx7hl.jpg', N'Bó hoa cưới thiết kế tay cầm quấn ruy băng lụa cao cấp, phối hoa tulip Hà Lan trắng, mao lương trắng và hoa hồng Ohara thơm ngát quyến rũ.');
INSERT INTO dbo.[products] ([deposit_fee], [import_price], [is_active], [is_deleted], [is_selling], [price_buy], [price_rent_per_day], [promotional_price], [rating], [sold], [view_count], [category_id], [created_at], [id], [store_id], [name], [image_url], [description]) VALUES (0.00, 350000.00, 1, NULL, 1, 650000.00, 0.00, NULL, 4.9, 125, 14, 7, NULL, 1, NULL, N'Bó Hoa Hồng Đỏ', N'https://res.cloudinary.com/rbw3u9td/image/upload/v1792383962/rental_shop_products/ddqvni5yjcqkqfkvqkot.jpg', N'Gồm 19 bông hồng đỏ Ecuador nhập khẩu phối hoa baby trắng Hà Lan, bọc giấy gói cao cấp phong cách Hàn Quốc.');
INSERT INTO dbo.[products] ([deposit_fee], [import_price], [is_active], [is_deleted], [is_selling], [price_buy], [price_rent_per_day], [promotional_price], [rating], [sold], [view_count], [category_id], [created_at], [id], [store_id], [name], [image_url], [description]) VALUES (300000.00, 350000.00, 1, NULL, 1, 650000.00, 80000.00, NULL, 5.0, 120, 59, 24, '2026-10-01 09:25:43.617551', 21, 2, N'Rung Động Đầu Tiên', N'https://res.cloudinary.com/rbw3u9td/image/upload/v1792385304/rental_shop_products/jqeoylnxs9amupcy3fpl.jpg', N'   Rung Động Đầu Tiên là bó hoa tròn đầy đặn với sắc hồng đậm của hoa hồng bông xen cùng cẩm chướng hồng phấn xếp nếp mềm mại, điểm xuyết vài bông hoa trắng nhỏ tâm xanh mint như những đốm sáng. Bó hoa được gói bằng giấy đen sang trọng, lớp lót hồng phấn bên trong làm nổi bật từng sắc hoa. Phù hợp để tỏ tình, kỷ niệm, sinh nhật hoặc tặng người thương nhân dịp 20/10, 8/3.

   Công thức phối gồm khoảng 8-10 cành hồng bông hồng đậm làm hoa chính, 10-12 bông cẩm chướng hồng phấn để làm mềm bó, và 3 bông hoa trắng nhỏ đặt rải ở ba vị trí hình tam giác làm điểm nhấn. Tỷ lệ màu khoảng 55% hồng đậm, 40% hồng phấn, 5% trắng. Cắm theo kiểu xoắn ốc, giữ mặt bó phẳng và tròn, buộc cố định rồi lót giấy hồng nhạt trước, bọc giấy đen bên ngoài cao hơn mặt hoa 5-7 cm để ôm lấy bó.');
SET IDENTITY_INSERT dbo.[products] OFF;
GO

-- Dữ liệu bảng: recently_viewed
SET IDENTITY_INSERT dbo.[recently_viewed] ON;
INSERT INTO dbo.[recently_viewed] ([id], [product_id], [user_id], [viewed_at]) VALUES (1, 5, 5, '2026-09-22 22:48:28.473');
INSERT INTO dbo.[recently_viewed] ([id], [product_id], [user_id], [viewed_at]) VALUES (4, 31, 5, '2026-10-04 20:11:02.373');
INSERT INTO dbo.[recently_viewed] ([id], [product_id], [user_id], [viewed_at]) VALUES (5, 37, 5, '2026-10-04 20:20:15.14');
INSERT INTO dbo.[recently_viewed] ([id], [product_id], [user_id], [viewed_at]) VALUES (6, 1, 5, '2026-10-24 15:25:52.807');
INSERT INTO dbo.[recently_viewed] ([id], [product_id], [user_id], [viewed_at]) VALUES (8, 21, 1, '2026-10-04 19:17:05.11');
INSERT INTO dbo.[recently_viewed] ([id], [product_id], [user_id], [viewed_at]) VALUES (9, 13, 5, '2026-10-04 19:35:01.677');
INSERT INTO dbo.[recently_viewed] ([id], [product_id], [user_id], [viewed_at]) VALUES (10, 32, 5, '2026-10-23 02:06:02.59');
INSERT INTO dbo.[recently_viewed] ([id], [product_id], [user_id], [viewed_at]) VALUES (11, 36, 5, '2026-10-04 20:19:35.723');
INSERT INTO dbo.[recently_viewed] ([id], [product_id], [user_id], [viewed_at]) VALUES (12, 38, 5, '2026-10-04 20:20:21.66');
INSERT INTO dbo.[recently_viewed] ([id], [product_id], [user_id], [viewed_at]) VALUES (13, 28, 5, '2026-10-26 21:13:47.816532');
INSERT INTO dbo.[recently_viewed] ([id], [product_id], [user_id], [viewed_at]) VALUES (14, 12, 5, '2026-10-04 22:14:18.503');
INSERT INTO dbo.[recently_viewed] ([id], [product_id], [user_id], [viewed_at]) VALUES (15, 22, 35, '2026-10-07 10:24:18.92');
INSERT INTO dbo.[recently_viewed] ([id], [product_id], [user_id], [viewed_at]) VALUES (16, 25, 5, '2026-10-07 16:50:19.453');
INSERT INTO dbo.[recently_viewed] ([id], [product_id], [user_id], [viewed_at]) VALUES (17, 26, 5, '2026-10-23 02:02:18.727');
INSERT INTO dbo.[recently_viewed] ([id], [product_id], [user_id], [viewed_at]) VALUES (18, 21, 36, '2026-10-13 17:10:39.473');
INSERT INTO dbo.[recently_viewed] ([id], [product_id], [user_id], [viewed_at]) VALUES (19, 26, 36, '2026-10-13 17:11:23.093');
INSERT INTO dbo.[recently_viewed] ([id], [product_id], [user_id], [viewed_at]) VALUES (20, 23, 36, '2026-10-13 17:15:43.57');
INSERT INTO dbo.[recently_viewed] ([id], [product_id], [user_id], [viewed_at]) VALUES (22, 39, 5, '2026-10-14 20:12:17.987');
INSERT INTO dbo.[recently_viewed] ([id], [product_id], [user_id], [viewed_at]) VALUES (23, 31, 37, '2026-10-21 21:49:24.063');
INSERT INTO dbo.[recently_viewed] ([id], [product_id], [user_id], [viewed_at]) VALUES (24, 11, 37, '2026-10-21 20:38:57.933');
INSERT INTO dbo.[recently_viewed] ([id], [product_id], [user_id], [viewed_at]) VALUES (25, 26, 37, '2026-10-21 20:49:19.563');
INSERT INTO dbo.[recently_viewed] ([id], [product_id], [user_id], [viewed_at]) VALUES (26, 21, 37, '2026-10-23 00:48:31.507');
INSERT INTO dbo.[recently_viewed] ([id], [product_id], [user_id], [viewed_at]) VALUES (27, 15, 37, '2026-10-21 21:05:59.127');
INSERT INTO dbo.[recently_viewed] ([id], [product_id], [user_id], [viewed_at]) VALUES (28, 23, 5, '2026-10-21 21:42:18.113');
INSERT INTO dbo.[recently_viewed] ([id], [product_id], [user_id], [viewed_at]) VALUES (30, 23, 37, '2026-10-22 16:52:48.617');
INSERT INTO dbo.[recently_viewed] ([id], [product_id], [user_id], [viewed_at]) VALUES (31, 38, 37, '2026-10-23 00:38:35.28');
INSERT INTO dbo.[recently_viewed] ([id], [product_id], [user_id], [viewed_at]) VALUES (32, 35, 5, '2026-10-23 01:01:19.9');
INSERT INTO dbo.[recently_viewed] ([id], [product_id], [user_id], [viewed_at]) VALUES (33, 34, 5, '2026-10-23 01:28:58.177');
INSERT INTO dbo.[recently_viewed] ([id], [product_id], [user_id], [viewed_at]) VALUES (34, 33, 5, '2026-10-23 01:29:10.31');
INSERT INTO dbo.[recently_viewed] ([id], [product_id], [user_id], [viewed_at]) VALUES (35, 40, 5, '2026-10-23 01:29:13.703');
INSERT INTO dbo.[recently_viewed] ([id], [product_id], [user_id], [viewed_at]) VALUES (36, 27, 5, '2026-10-26 21:09:46.531772');
INSERT INTO dbo.[recently_viewed] ([id], [product_id], [user_id], [viewed_at]) VALUES (37, 37, 1, '2026-10-28 13:18:09.097168');
INSERT INTO dbo.[recently_viewed] ([id], [product_id], [user_id], [viewed_at]) VALUES (39, 26, 40, '2026-10-28 14:44:29.334244');
INSERT INTO dbo.[recently_viewed] ([id], [product_id], [user_id], [viewed_at]) VALUES (40, 26, 41, '2026-10-28 14:44:29.534934');
INSERT INTO dbo.[recently_viewed] ([id], [product_id], [user_id], [viewed_at]) VALUES (41, 26, 42, '2026-10-28 14:44:29.759851');
INSERT INTO dbo.[recently_viewed] ([id], [product_id], [user_id], [viewed_at]) VALUES (42, 26, 43, '2026-10-28 14:44:29.919165');
INSERT INTO dbo.[recently_viewed] ([id], [product_id], [user_id], [viewed_at]) VALUES (43, 27, 30, '2026-10-28 14:45:49.128667');
INSERT INTO dbo.[recently_viewed] ([id], [product_id], [user_id], [viewed_at]) VALUES (44, 22, 2, '2026-10-28 14:53:03.676096');
INSERT INTO dbo.[recently_viewed] ([id], [product_id], [user_id], [viewed_at]) VALUES (45, 36, 2, '2026-10-28 15:03:21.202715');
INSERT INTO dbo.[recently_viewed] ([id], [product_id], [user_id], [viewed_at]) VALUES (46, 21, 40, '2026-10-28 15:30:41.027243');
INSERT INTO dbo.[recently_viewed] ([id], [product_id], [user_id], [viewed_at]) VALUES (47, 21, 41, '2026-10-28 15:30:41.301793');
INSERT INTO dbo.[recently_viewed] ([id], [product_id], [user_id], [viewed_at]) VALUES (48, 21, 42, '2026-10-28 15:30:41.509174');
INSERT INTO dbo.[recently_viewed] ([id], [product_id], [user_id], [viewed_at]) VALUES (49, 21, 43, '2026-10-28 15:30:41.705642');
INSERT INTO dbo.[recently_viewed] ([id], [product_id], [user_id], [viewed_at]) VALUES (21, 22, 6, '2026-10-28 15:47:54.692874');
INSERT INTO dbo.[recently_viewed] ([id], [product_id], [user_id], [viewed_at]) VALUES (50, 1, 42, '2026-10-28 16:00:22.398519');
INSERT INTO dbo.[recently_viewed] ([id], [product_id], [user_id], [viewed_at]) VALUES (51, 1, 40, '2026-10-28 16:00:22.564894');
INSERT INTO dbo.[recently_viewed] ([id], [product_id], [user_id], [viewed_at]) VALUES (52, 40, 2, '2026-10-28 16:01:11.035873');
INSERT INTO dbo.[recently_viewed] ([id], [product_id], [user_id], [viewed_at]) VALUES (53, 26, 2, '2026-10-28 16:02:09.672987');
INSERT INTO dbo.[recently_viewed] ([id], [product_id], [user_id], [viewed_at]) VALUES (54, 27, 6, '2026-10-28 16:03:28.474659');
INSERT INTO dbo.[recently_viewed] ([id], [product_id], [user_id], [viewed_at]) VALUES (55, 10, 5, '2026-10-28 16:12:40.383269');
INSERT INTO dbo.[recently_viewed] ([id], [product_id], [user_id], [viewed_at]) VALUES (29, 22, 5, '2026-10-28 16:12:47.523271');
INSERT INTO dbo.[recently_viewed] ([id], [product_id], [user_id], [viewed_at]) VALUES (7, 21, 5, '2026-10-28 16:12:55.026749');
INSERT INTO dbo.[recently_viewed] ([id], [product_id], [user_id], [viewed_at]) VALUES (38, 22, 30, '2026-10-28 16:36:37.724744');
INSERT INTO dbo.[recently_viewed] ([id], [product_id], [user_id], [viewed_at]) VALUES (56, 21, 30, '2026-10-28 16:36:49.316191');
INSERT INTO dbo.[recently_viewed] ([id], [product_id], [user_id], [viewed_at]) VALUES (57, 36, 30, '2026-10-28 16:36:54.882365');
SET IDENTITY_INSERT dbo.[recently_viewed] OFF;
GO

-- Dữ liệu bảng: reviews
SET IDENTITY_INSERT dbo.[reviews] ON;
INSERT INTO dbo.[reviews] ([rating], [created_at], [id], [order_id], [product_id], [store_id], [updated_at], [user_id], [image_url], [content], [image_public_id]) VALUES (5, '2026-10-23 00:06:59.463', 8, 29, 21, 2, '2026-10-23 00:06:59.463', 37, N'https://res.cloudinary.com/rbw3u9td/image/upload/v1792688818/reviews/gfut5keypju2ywxfnjok.jpg', N'Hoa tươi, cánh còn căng và màu rất đẹp giống như hình. Đóng gói cẩn thận, thiệp xinh, shipper giao nhanh và thân thiện. Mình rất hài lòng, sẽ ủng hộ tiệm lần sau!', NULL);
SET IDENTITY_INSERT dbo.[reviews] OFF;
GO

-- Dữ liệu bảng: role_requests
SET IDENTITY_INSERT dbo.[role_requests] ON;
INSERT INTO dbo.[role_requests] ([assigned_store_id], [created_at], [id], [reviewed_at], [reviewed_by_id], [target_store_id], [user_id], [phone], [status], [target_role], [vehicle_type], [rejection_reason], [shop_address], [experience_note], [contact_info], [shop_name], [work_area]) VALUES (NULL, '2026-10-04 20:32:26.122334', 1, NULL, NULL, NULL, 5, N'0909999888', N'PENDING', N'MANAGER', NULL, NULL, N'112, Võ Hưng Yên,Đạo Thạnh, Đà Lạt', N',Hoa hồng, Cẩm Chướng', N'0973124579', N'Hoa Tươi - Chi Nhánh Đà Lạt', NULL);
INSERT INTO dbo.[role_requests] ([assigned_store_id], [created_at], [id], [reviewed_at], [reviewed_by_id], [target_store_id], [user_id], [phone], [status], [target_role], [vehicle_type], [rejection_reason], [shop_address], [experience_note], [contact_info], [shop_name], [work_area]) VALUES (NULL, '2026-10-07 10:26:44.047552', 2, NULL, NULL, 7, 35, N'0912457891', N'PENDING', N'MANAGER', NULL, NULL, NULL, N'', NULL, NULL, NULL);
SET IDENTITY_INSERT dbo.[role_requests] OFF;
GO

-- Dữ liệu bảng: shipping_tariffs
SET IDENTITY_INSERT dbo.[shipping_tariffs] ON;
INSERT INTO dbo.[shipping_tariffs] ([base_fee], [bulky_surcharge], [is_active], [is_deleted], [created_at], [delivery_id], [id], [store_id], [updated_at], [estimated_time], [service_name], [destination_area], [note]) VALUES (25000.00, 20000.00, 1, 0, '2026-10-15 00:20:08.656666', 1, 4, 1, '2026-10-15 00:20:08.656666', N'4 - 6 giờ', N'Tiêu chuẩn', N'Khu vực gần chi nhánh (< 5km)', N'Giao hoa tươi giữ ẩm tận nơi Quận 1, 3, 4');
INSERT INTO dbo.[shipping_tariffs] ([base_fee], [bulky_surcharge], [is_active], [is_deleted], [created_at], [delivery_id], [id], [store_id], [updated_at], [estimated_time], [service_name], [destination_area], [note]) VALUES (40000.00, 25000.00, 1, 0, '2026-10-15 00:20:08.656666', 1, 5, 1, '2026-10-15 00:20:08.656666', N'4 - 6 giờ', N'Tiêu chuẩn', N'Khu vực xa hơn (5 - 15km)', N'Giao các quận nội thành lân cận Q5, Q10, Bình Thạnh, Phú Nhuận');
INSERT INTO dbo.[shipping_tariffs] ([base_fee], [bulky_surcharge], [is_active], [is_deleted], [created_at], [delivery_id], [id], [store_id], [updated_at], [estimated_time], [service_name], [destination_area], [note]) VALUES (45000.00, 30000.00, 1, 0, '2026-10-15 00:20:08.656666', 2, 6, 1, '2026-10-15 00:20:08.656666', N'Trong vòng 2 giờ', N'Hỏa tốc', N'Khu vực gần chi nhánh (< 5km)', N'Ưu tiên cắm và giao ngay trong lồng kính xe máy bảo vệ hoa');
INSERT INTO dbo.[shipping_tariffs] ([base_fee], [bulky_surcharge], [is_active], [is_deleted], [created_at], [delivery_id], [id], [store_id], [updated_at], [estimated_time], [service_name], [destination_area], [note]) VALUES (65000.00, 35000.00, 1, 0, '2026-10-15 00:20:08.656666', 2, 7, 1, '2026-10-15 00:20:08.656666', N'Trong vòng 2 giờ', N'Hỏa tốc', N'Khu vực xa hơn (5 - 15km)', N'Hỏa tốc liên quận nội thành trong 2 giờ');
INSERT INTO dbo.[shipping_tariffs] ([base_fee], [bulky_surcharge], [is_active], [is_deleted], [created_at], [delivery_id], [id], [store_id], [updated_at], [estimated_time], [service_name], [destination_area], [note]) VALUES (50000.00, 30000.00, 1, 0, '2026-10-15 00:20:08.656666', 3, 8, 1, '2026-10-15 00:20:08.656666', N'60 - 90 phút', N'Siêu tốc', N'Khu vực gần chi nhánh (< 5km)', N'Tài xế Ahamove giao siêu tốc hoa tươi hẹn giờ');
INSERT INTO dbo.[shipping_tariffs] ([base_fee], [bulky_surcharge], [is_active], [is_deleted], [created_at], [delivery_id], [id], [store_id], [updated_at], [estimated_time], [service_name], [destination_area], [note]) VALUES (25000.00, 20000.00, 1, 0, '2026-10-15 00:20:08.66', 1, 9, 2, '2026-10-15 00:20:08.66', N'4 - 6 giờ', N'Tiêu chuẩn', N'Khu vực gần chi nhánh (< 5km)', N'Giao hoa tận nơi khu đô thị Phú Mỹ Hưng, Tân Phong, Tân Phú');
INSERT INTO dbo.[shipping_tariffs] ([base_fee], [bulky_surcharge], [is_active], [is_deleted], [created_at], [delivery_id], [id], [store_id], [updated_at], [estimated_time], [service_name], [destination_area], [note]) VALUES (40000.00, 25000.00, 1, 0, '2026-10-15 00:20:08.66', 1, 10, 2, '2026-10-15 00:20:08.66', N'4 - 6 giờ', N'Tiêu chuẩn', N'Khu vực xa hơn (5 - 15km)', N'Giao các quận lân cận Q4, Q8, Nhà Bè, Bình Chánh');
INSERT INTO dbo.[shipping_tariffs] ([base_fee], [bulky_surcharge], [is_active], [is_deleted], [created_at], [delivery_id], [id], [store_id], [updated_at], [estimated_time], [service_name], [destination_area], [note]) VALUES (45000.00, 30000.00, 1, 0, '2026-10-15 00:20:08.66', 2, 11, 2, '2026-10-15 00:20:08.66', N'Trong vòng 2 giờ', N'Hỏa tốc', N'Khu vực gần chi nhánh (< 5km)', N'Giao nhanh 2h khu đô thị Quận 7 bảo quản hoa thùng lạnh');
INSERT INTO dbo.[shipping_tariffs] ([base_fee], [bulky_surcharge], [is_active], [is_deleted], [created_at], [delivery_id], [id], [store_id], [updated_at], [estimated_time], [service_name], [destination_area], [note]) VALUES (65000.00, 35000.00, 1, 0, '2026-10-15 00:20:08.66', 2, 12, 2, '2026-10-15 00:20:08.66', N'Trong vòng 2 giờ', N'Hỏa tốc', N'Khu vực xa hơn (5 - 15km)', N'Giao hỏa tốc từ Quận 7 sang các quận lân cận');
INSERT INTO dbo.[shipping_tariffs] ([base_fee], [bulky_surcharge], [is_active], [is_deleted], [created_at], [delivery_id], [id], [store_id], [updated_at], [estimated_time], [service_name], [destination_area], [note]) VALUES (50000.00, 30000.00, 1, 0, '2026-10-15 00:20:08.66', 3, 13, 2, '2026-10-15 00:20:08.66', N'60 - 90 phút', N'Siêu tốc', N'Khu vực gần chi nhánh (< 5km)', N'Giao siêu tốc Ahamove đơn hoa tặng tiệc cưới, sự kiện');
INSERT INTO dbo.[shipping_tariffs] ([base_fee], [bulky_surcharge], [is_active], [is_deleted], [created_at], [delivery_id], [id], [store_id], [updated_at], [estimated_time], [service_name], [destination_area], [note]) VALUES (25000.00, 20000.00, 1, 0, '2026-10-15 00:20:08.66', 1, 14, 3, '2026-10-15 00:20:08.66', N'4 - 6 giờ', N'Tiêu chuẩn', N'Khu vực gần chi nhánh (< 5km)', N'Giao khu vực Làng Đại Học, Linh Chiểu, Linh Trung, ĐHQG');
INSERT INTO dbo.[shipping_tariffs] ([base_fee], [bulky_surcharge], [is_active], [is_deleted], [created_at], [delivery_id], [id], [store_id], [updated_at], [estimated_time], [service_name], [destination_area], [note]) VALUES (40000.00, 25000.00, 1, 0, '2026-10-15 00:20:08.66', 1, 15, 3, '2026-10-15 00:20:08.66', N'4 - 6 giờ', N'Tiêu chuẩn', N'Khu vực xa hơn (5 - 15km)', N'Giao toàn khu vực TP. Thủ Đức (Q2, Q9 cũ) và Dĩ An');
INSERT INTO dbo.[shipping_tariffs] ([base_fee], [bulky_surcharge], [is_active], [is_deleted], [created_at], [delivery_id], [id], [store_id], [updated_at], [estimated_time], [service_name], [destination_area], [note]) VALUES (45000.00, 30000.00, 1, 0, '2026-10-15 00:20:08.66', 2, 16, 3, '2026-10-15 00:20:08.66', N'Trong vòng 2 giờ', N'Hỏa tốc', N'Khu vực gần chi nhánh (< 5km)', N'Hỏa tốc 2 giờ đơn hoa chúc mừng tốt nghiệp, sinh nhật');
INSERT INTO dbo.[shipping_tariffs] ([base_fee], [bulky_surcharge], [is_active], [is_deleted], [created_at], [delivery_id], [id], [store_id], [updated_at], [estimated_time], [service_name], [destination_area], [note]) VALUES (65000.00, 35000.00, 1, 0, '2026-10-15 00:20:08.66', 2, 17, 3, '2026-10-15 00:20:08.66', N'Trong vòng 2 giờ', N'Hỏa tốc', N'Khu vực xa hơn (5 - 15km)', N'Giao hỏa tốc Thủ Đức sang Bình Thạnh, Gò Vấp');
INSERT INTO dbo.[shipping_tariffs] ([base_fee], [bulky_surcharge], [is_active], [is_deleted], [created_at], [delivery_id], [id], [store_id], [updated_at], [estimated_time], [service_name], [destination_area], [note]) VALUES (50000.00, 30000.00, 1, 0, '2026-10-15 00:20:08.66', 3, 18, 3, '2026-10-15 00:20:08.66', N'60 - 90 phút', N'Siêu tốc', N'Khu vực gần chi nhánh (< 5km)', N'Giao siêu tốc hoa tươi trong 60-90 phút');
INSERT INTO dbo.[shipping_tariffs] ([base_fee], [bulky_surcharge], [is_active], [is_deleted], [created_at], [delivery_id], [id], [store_id], [updated_at], [estimated_time], [service_name], [destination_area], [note]) VALUES (20000.00, 15000.00, 1, 0, '2026-10-15 00:20:08.663333', 1, 19, 7, '2026-10-15 00:20:08.663333', N'4 - 6 giờ', N'Tiêu chuẩn', N'Khu vực gần chi nhánh (< 5km)', N'Giao hoa nội ô TP. Cao Lãnh (P1, P2, P3, P4, Mỹ Trà, An Bình)');
INSERT INTO dbo.[shipping_tariffs] ([base_fee], [bulky_surcharge], [is_active], [is_deleted], [created_at], [delivery_id], [id], [store_id], [updated_at], [estimated_time], [service_name], [destination_area], [note]) VALUES (35000.00, 20000.00, 1, 0, '2026-10-15 00:20:08.663333', 1, 20, 7, '2026-10-15 00:20:08.663333', N'4 - 6 giờ', N'Tiêu chuẩn', N'Khu vực xa hơn (5 - 15km)', N'Giao huyện Cao Lãnh, Lấp Vò, vùng ven Đồng Tháp ≤ 15km');
INSERT INTO dbo.[shipping_tariffs] ([base_fee], [bulky_surcharge], [is_active], [is_deleted], [created_at], [delivery_id], [id], [store_id], [updated_at], [estimated_time], [service_name], [destination_area], [note]) VALUES (40000.00, 25000.00, 1, 0, '2026-10-15 00:20:08.663333', 2, 21, 7, '2026-10-15 00:20:08.663333', N'Trong vòng 2 giờ', N'Hỏa tốc', N'Khu vực gần chi nhánh (< 5km)', N'Giao hoa tươi hỏa tốc nội ô TP. Cao Lãnh');
INSERT INTO dbo.[shipping_tariffs] ([base_fee], [bulky_surcharge], [is_active], [is_deleted], [created_at], [delivery_id], [id], [store_id], [updated_at], [estimated_time], [service_name], [destination_area], [note]) VALUES (55000.00, 30000.00, 1, 0, '2026-10-15 00:20:08.663333', 2, 22, 7, '2026-10-15 00:20:08.663333', N'Trong vòng 2 giờ', N'Hỏa tốc', N'Khu vực xa hơn (5 - 15km)', N'Hỏa tốc vùng ven TP. Cao Lãnh và các huyện lân cận');
INSERT INTO dbo.[shipping_tariffs] ([base_fee], [bulky_surcharge], [is_active], [is_deleted], [created_at], [delivery_id], [id], [store_id], [updated_at], [estimated_time], [service_name], [destination_area], [note]) VALUES (45000.00, 25000.00, 1, 0, '2026-10-15 00:20:08.663333', 3, 23, 7, '2026-10-15 00:20:08.663333', N'60 - 90 phút', N'Siêu tốc', N'Khu vực gần chi nhánh (< 5km)', N'Tài xế chuyên hoa tươi giao nhanh tận nhà');
SET IDENTITY_INSERT dbo.[shipping_tariffs] OFF;
GO

-- Dữ liệu bảng: store_inventories
SET IDENTITY_INSERT dbo.[store_inventories] ON;
INSERT INTO dbo.[store_inventories] ([is_deleted], [is_selling], [quantity], [id], [product_id], [store_id], [version]) VALUES (0, 1, 15, 2, 2, 1, 0);
INSERT INTO dbo.[store_inventories] ([is_deleted], [is_selling], [quantity], [id], [product_id], [store_id], [version]) VALUES (0, 1, 25, 3, 3, 1, 0);
INSERT INTO dbo.[store_inventories] ([is_deleted], [is_selling], [quantity], [id], [product_id], [store_id], [version]) VALUES (0, 1, 12, 4, 4, 1, 0);
INSERT INTO dbo.[store_inventories] ([is_deleted], [is_selling], [quantity], [id], [product_id], [store_id], [version]) VALUES (0, 1, 10, 5, 5, 1, 0);
INSERT INTO dbo.[store_inventories] ([is_deleted], [is_selling], [quantity], [id], [product_id], [store_id], [version]) VALUES (0, 1, 8, 6, 6, 1, 0);
INSERT INTO dbo.[store_inventories] ([is_deleted], [is_selling], [quantity], [id], [product_id], [store_id], [version]) VALUES (0, 1, 10, 7, 7, 1, 0);
INSERT INTO dbo.[store_inventories] ([is_deleted], [is_selling], [quantity], [id], [product_id], [store_id], [version]) VALUES (0, 1, 6, 8, 8, 1, 0);
INSERT INTO dbo.[store_inventories] ([is_deleted], [is_selling], [quantity], [id], [product_id], [store_id], [version]) VALUES (0, 1, 8, 9, 9, 1, 0);
INSERT INTO dbo.[store_inventories] ([is_deleted], [is_selling], [quantity], [id], [product_id], [store_id], [version]) VALUES (0, 1, 3, 10, 10, 1, 0);
INSERT INTO dbo.[store_inventories] ([is_deleted], [is_selling], [quantity], [id], [product_id], [store_id], [version]) VALUES (0, 1, 6, 11, 11, 1, 0);
INSERT INTO dbo.[store_inventories] ([is_deleted], [is_selling], [quantity], [id], [product_id], [store_id], [version]) VALUES (0, 1, 18, 12, 1, 2, 0);
INSERT INTO dbo.[store_inventories] ([is_deleted], [is_selling], [quantity], [id], [product_id], [store_id], [version]) VALUES (0, 1, 12, 13, 2, 2, 0);
INSERT INTO dbo.[store_inventories] ([is_deleted], [is_selling], [quantity], [id], [product_id], [store_id], [version]) VALUES (0, 1, 20, 14, 3, 2, 0);
INSERT INTO dbo.[store_inventories] ([is_deleted], [is_selling], [quantity], [id], [product_id], [store_id], [version]) VALUES (0, 1, 15, 15, 4, 2, 0);
INSERT INTO dbo.[store_inventories] ([is_deleted], [is_selling], [quantity], [id], [product_id], [store_id], [version]) VALUES (0, 1, 8, 16, 5, 2, 0);
INSERT INTO dbo.[store_inventories] ([is_deleted], [is_selling], [quantity], [id], [product_id], [store_id], [version]) VALUES (0, 1, 6, 17, 6, 2, 0);
INSERT INTO dbo.[store_inventories] ([is_deleted], [is_selling], [quantity], [id], [product_id], [store_id], [version]) VALUES (0, 1, 8, 18, 7, 2, 0);
INSERT INTO dbo.[store_inventories] ([is_deleted], [is_selling], [quantity], [id], [product_id], [store_id], [version]) VALUES (0, 1, 5, 19, 8, 2, 0);
INSERT INTO dbo.[store_inventories] ([is_deleted], [is_selling], [quantity], [id], [product_id], [store_id], [version]) VALUES (0, 1, 10, 20, 9, 2, 0);
INSERT INTO dbo.[store_inventories] ([is_deleted], [is_selling], [quantity], [id], [product_id], [store_id], [version]) VALUES (0, 1, 2, 21, 10, 2, 0);
INSERT INTO dbo.[store_inventories] ([is_deleted], [is_selling], [quantity], [id], [product_id], [store_id], [version]) VALUES (0, 1, 4, 22, 11, 2, 0);
INSERT INTO dbo.[store_inventories] ([is_deleted], [is_selling], [quantity], [id], [product_id], [store_id], [version]) VALUES (0, 1, 15, 23, 1, 3, 0);
INSERT INTO dbo.[store_inventories] ([is_deleted], [is_selling], [quantity], [id], [product_id], [store_id], [version]) VALUES (0, 1, 10, 24, 2, 3, 0);
INSERT INTO dbo.[store_inventories] ([is_deleted], [is_selling], [quantity], [id], [product_id], [store_id], [version]) VALUES (0, 1, 30, 25, 3, 3, 0);
INSERT INTO dbo.[store_inventories] ([is_deleted], [is_selling], [quantity], [id], [product_id], [store_id], [version]) VALUES (0, 1, 10, 26, 4, 3, 0);
INSERT INTO dbo.[store_inventories] ([is_deleted], [is_selling], [quantity], [id], [product_id], [store_id], [version]) VALUES (0, 1, 6, 27, 5, 3, 0);
INSERT INTO dbo.[store_inventories] ([is_deleted], [is_selling], [quantity], [id], [product_id], [store_id], [version]) VALUES (0, 1, 5, 28, 6, 3, 0);
INSERT INTO dbo.[store_inventories] ([is_deleted], [is_selling], [quantity], [id], [product_id], [store_id], [version]) VALUES (0, 1, 6, 29, 7, 3, 0);
INSERT INTO dbo.[store_inventories] ([is_deleted], [is_selling], [quantity], [id], [product_id], [store_id], [version]) VALUES (0, 1, 4, 30, 8, 3, 0);
INSERT INTO dbo.[store_inventories] ([is_deleted], [is_selling], [quantity], [id], [product_id], [store_id], [version]) VALUES (0, 1, 5, 31, 9, 3, 0);
INSERT INTO dbo.[store_inventories] ([is_deleted], [is_selling], [quantity], [id], [product_id], [store_id], [version]) VALUES (0, 1, 2, 32, 10, 3, 0);
INSERT INTO dbo.[store_inventories] ([is_deleted], [is_selling], [quantity], [id], [product_id], [store_id], [version]) VALUES (0, 1, 4, 33, 11, 3, 0);
INSERT INTO dbo.[store_inventories] ([is_deleted], [is_selling], [quantity], [id], [product_id], [store_id], [version]) VALUES (0, 1, 24, 34, 12, 1, 1);
INSERT INTO dbo.[store_inventories] ([is_deleted], [is_selling], [quantity], [id], [product_id], [store_id], [version]) VALUES (0, 1, 25, 35, 13, 1, 0);
INSERT INTO dbo.[store_inventories] ([is_deleted], [is_selling], [quantity], [id], [product_id], [store_id], [version]) VALUES (0, 1, 25, 36, 14, 1, 0);
INSERT INTO dbo.[store_inventories] ([is_deleted], [is_selling], [quantity], [id], [product_id], [store_id], [version]) VALUES (0, 0, 25, 37, 15, 1, 1);
INSERT INTO dbo.[store_inventories] ([is_deleted], [is_selling], [quantity], [id], [product_id], [store_id], [version]) VALUES (0, 1, 25, 38, 16, 1, 0);
INSERT INTO dbo.[store_inventories] ([is_deleted], [is_selling], [quantity], [id], [product_id], [store_id], [version]) VALUES (1, 0, 25, 39, 17, 1, 1);
INSERT INTO dbo.[store_inventories] ([is_deleted], [is_selling], [quantity], [id], [product_id], [store_id], [version]) VALUES (0, 1, 25, 40, 18, 1, 0);
INSERT INTO dbo.[store_inventories] ([is_deleted], [is_selling], [quantity], [id], [product_id], [store_id], [version]) VALUES (0, 1, 25, 41, 19, 1, 0);
INSERT INTO dbo.[store_inventories] ([is_deleted], [is_selling], [quantity], [id], [product_id], [store_id], [version]) VALUES (0, 1, 25, 42, 20, 1, 0);
INSERT INTO dbo.[store_inventories] ([is_deleted], [is_selling], [quantity], [id], [product_id], [store_id], [version]) VALUES (0, 1, 44, 43, 21, 2, 12);
INSERT INTO dbo.[store_inventories] ([is_deleted], [is_selling], [quantity], [id], [product_id], [store_id], [version]) VALUES (0, 1, 29, 45, 23, 2, 4);
INSERT INTO dbo.[store_inventories] ([is_deleted], [is_selling], [quantity], [id], [product_id], [store_id], [version]) VALUES (0, 1, 40, 46, 24, 2, 1);
INSERT INTO dbo.[store_inventories] ([is_deleted], [is_selling], [quantity], [id], [product_id], [store_id], [version]) VALUES (0, 1, 20, 47, 25, 2, 3);
INSERT INTO dbo.[store_inventories] ([is_deleted], [is_selling], [quantity], [id], [product_id], [store_id], [version]) VALUES (0, 1, 24, 50, 28, 2, 1);
INSERT INTO dbo.[store_inventories] ([is_deleted], [is_selling], [quantity], [id], [product_id], [store_id], [version]) VALUES (0, 1, 25, 51, 29, 2, 0);
INSERT INTO dbo.[store_inventories] ([is_deleted], [is_selling], [quantity], [id], [product_id], [store_id], [version]) VALUES (0, 1, 25, 52, 30, 3, 0);
INSERT INTO dbo.[store_inventories] ([is_deleted], [is_selling], [quantity], [id], [product_id], [store_id], [version]) VALUES (0, 1, 25, 53, 31, 3, 2);
INSERT INTO dbo.[store_inventories] ([is_deleted], [is_selling], [quantity], [id], [product_id], [store_id], [version]) VALUES (0, 1, 23, 54, 32, 3, 1);
INSERT INTO dbo.[store_inventories] ([is_deleted], [is_selling], [quantity], [id], [product_id], [store_id], [version]) VALUES (0, 1, 25, 55, 33, 3, 0);
INSERT INTO dbo.[store_inventories] ([is_deleted], [is_selling], [quantity], [id], [product_id], [store_id], [version]) VALUES (0, 1, 25, 56, 34, 3, 0);
INSERT INTO dbo.[store_inventories] ([is_deleted], [is_selling], [quantity], [id], [product_id], [store_id], [version]) VALUES (0, 1, 25, 57, 35, 3, 0);
INSERT INTO dbo.[store_inventories] ([is_deleted], [is_selling], [quantity], [id], [product_id], [store_id], [version]) VALUES (0, 1, 25, 58, 36, 3, 0);
INSERT INTO dbo.[store_inventories] ([is_deleted], [is_selling], [quantity], [id], [product_id], [store_id], [version]) VALUES (0, 1, 25, 59, 37, 3, 0);
INSERT INTO dbo.[store_inventories] ([is_deleted], [is_selling], [quantity], [id], [product_id], [store_id], [version]) VALUES (0, 1, 25, 60, 38, 3, 0);
INSERT INTO dbo.[store_inventories] ([is_deleted], [is_selling], [quantity], [id], [product_id], [store_id], [version]) VALUES (0, 1, 25, 61, 1, 7, 0);
INSERT INTO dbo.[store_inventories] ([is_deleted], [is_selling], [quantity], [id], [product_id], [store_id], [version]) VALUES (0, 1, 25, 62, 2, 7, 0);
INSERT INTO dbo.[store_inventories] ([is_deleted], [is_selling], [quantity], [id], [product_id], [store_id], [version]) VALUES (0, 1, 25, 63, 3, 7, 0);
INSERT INTO dbo.[store_inventories] ([is_deleted], [is_selling], [quantity], [id], [product_id], [store_id], [version]) VALUES (0, 1, 25, 64, 4, 7, 0);
INSERT INTO dbo.[store_inventories] ([is_deleted], [is_selling], [quantity], [id], [product_id], [store_id], [version]) VALUES (0, 1, 25, 65, 5, 7, 0);
INSERT INTO dbo.[store_inventories] ([is_deleted], [is_selling], [quantity], [id], [product_id], [store_id], [version]) VALUES (0, 1, 25, 66, 6, 7, 0);
INSERT INTO dbo.[store_inventories] ([is_deleted], [is_selling], [quantity], [id], [product_id], [store_id], [version]) VALUES (0, 1, 25, 67, 7, 7, 0);
INSERT INTO dbo.[store_inventories] ([is_deleted], [is_selling], [quantity], [id], [product_id], [store_id], [version]) VALUES (0, 1, 25, 68, 8, 7, 0);
INSERT INTO dbo.[store_inventories] ([is_deleted], [is_selling], [quantity], [id], [product_id], [store_id], [version]) VALUES (0, 1, 25, 69, 9, 7, 0);
INSERT INTO dbo.[store_inventories] ([is_deleted], [is_selling], [quantity], [id], [product_id], [store_id], [version]) VALUES (0, 1, 25, 70, 10, 7, 0);
INSERT INTO dbo.[store_inventories] ([is_deleted], [is_selling], [quantity], [id], [product_id], [store_id], [version]) VALUES (0, 1, 25, 71, 11, 7, 0);
INSERT INTO dbo.[store_inventories] ([is_deleted], [is_selling], [quantity], [id], [product_id], [store_id], [version]) VALUES (0, 1, 20, 153, 39, 1, 0);
INSERT INTO dbo.[store_inventories] ([is_deleted], [is_selling], [quantity], [id], [product_id], [store_id], [version]) VALUES (0, 1, 20, 154, 40, 1, 2);
INSERT INTO dbo.[store_inventories] ([is_deleted], [is_selling], [quantity], [id], [product_id], [store_id], [version]) VALUES (0, 1, 20, 155, 41, 1, 0);
INSERT INTO dbo.[store_inventories] ([is_deleted], [is_selling], [quantity], [id], [product_id], [store_id], [version]) VALUES (0, 1, 20, 156, 41, 2, 0);
INSERT INTO dbo.[store_inventories] ([is_deleted], [is_selling], [quantity], [id], [product_id], [store_id], [version]) VALUES (0, 1, 20, 157, 41, 3, 0);
INSERT INTO dbo.[store_inventories] ([is_deleted], [is_selling], [quantity], [id], [product_id], [store_id], [version]) VALUES (0, 1, 20, 158, 41, 7, 0);
INSERT INTO dbo.[store_inventories] ([is_deleted], [is_selling], [quantity], [id], [product_id], [store_id], [version]) VALUES (0, 1, 19, 1, 1, 1, 5);
INSERT INTO dbo.[store_inventories] ([is_deleted], [is_selling], [quantity], [id], [product_id], [store_id], [version]) VALUES (0, 1, 35, 49, 27, 2, 5);
INSERT INTO dbo.[store_inventories] ([is_deleted], [is_selling], [quantity], [id], [product_id], [store_id], [version]) VALUES (0, 1, 24, 44, 22, 2, 9);
INSERT INTO dbo.[store_inventories] ([is_deleted], [is_selling], [quantity], [id], [product_id], [store_id], [version]) VALUES (0, 1, 5, 48, 26, 2, 5);
SET IDENTITY_INSERT dbo.[store_inventories] OFF;
GO

-- Dữ liệu bảng: store_levels
SET IDENTITY_INSERT dbo.[store_levels] ON;
INSERT INTO dbo.[store_levels] ([discount], [is_deleted], [min_point], [created_at], [id], [updated_at], [name]) VALUES (0.00, 0, 0, '2026-10-01 09:09:08.133', 1, '2026-10-01 09:09:08.133', N'Tiêu chuẩn');
INSERT INTO dbo.[store_levels] ([discount], [is_deleted], [min_point], [created_at], [id], [updated_at], [name]) VALUES (5.00, 0, 10000000, '2026-10-01 09:09:08.203', 2, '2026-10-02 14:54:49.753', N'Cửa hàng uy tín');
INSERT INTO dbo.[store_levels] ([discount], [is_deleted], [min_point], [created_at], [id], [updated_at], [name]) VALUES (10.00, 0, 30000000, '2026-10-01 09:09:08.22', 3, '2026-10-02 14:54:56.037', N'Cửa hàng xuất sắc');
SET IDENTITY_INSERT dbo.[store_levels] OFF;
GO

-- Dữ liệu bảng: store_memberships
SET IDENTITY_INSERT dbo.[store_memberships] ON;
INSERT INTO dbo.[store_memberships] ([active], [id], [joined_at], [store_id], [user_id], [access_role]) VALUES (1, 6, '2026-08-07 01:02:09.949881', 1, 6, N'PARTNER');
INSERT INTO dbo.[store_memberships] ([active], [id], [joined_at], [store_id], [user_id], [access_role]) VALUES (1, 7, '2026-07-07 01:02:09.958902', 2, 29, N'STAFF');
INSERT INTO dbo.[store_memberships] ([active], [id], [joined_at], [store_id], [user_id], [access_role]) VALUES (1, 8, '2026-05-07 01:02:10.152806', 2, 30, N'STAFF');
INSERT INTO dbo.[store_memberships] ([active], [id], [joined_at], [store_id], [user_id], [access_role]) VALUES (1, 9, '2026-07-07 01:02:10.325856', 2, 31, N'STAFF');
INSERT INTO dbo.[store_memberships] ([active], [id], [joined_at], [store_id], [user_id], [access_role]) VALUES (1, 10, '2026-08-07 01:02:10.568159', 2, 32, N'STAFF');
INSERT INTO dbo.[store_memberships] ([active], [id], [joined_at], [store_id], [user_id], [access_role]) VALUES (0, 11, '2026-10-24 15:45:58.326052', 1, 38, N'STAFF');
INSERT INTO dbo.[store_memberships] ([active], [id], [joined_at], [store_id], [user_id], [access_role]) VALUES (1, 12, '2026-10-24 15:53:53.505694', 1, 39, N'STAFF');
INSERT INTO dbo.[store_memberships] ([active], [id], [joined_at], [store_id], [user_id], [access_role]) VALUES (1, 13, '2026-10-28 13:28:58.148572', 1, 41, N'STAFF');
INSERT INTO dbo.[store_memberships] ([active], [id], [joined_at], [store_id], [user_id], [access_role]) VALUES (1, 14, '2026-10-28 13:28:58.148572', 1, 42, N'PARTNER');
SET IDENTITY_INSERT dbo.[store_memberships] OFF;
GO

-- Dữ liệu bảng: stores
SET IDENTITY_INSERT dbo.[stores] ON;
INSERT INTO dbo.[stores] ([e_wallet], [is_open], [latitude], [longitude], [pending_balance], [point], [rating], [status], [commission_id], [created_at], [id], [owner_id], [store_level_id], [version], [phone], [creation_request_key], [name], [avatar], [cover], [bio], [address], [avatar_public_id], [cover_public_id], [featured_image_public_ids], [featured_images]) VALUES (0.00, 1, 10.7725, 106.698, 0.00, 0, 5.0, 1, 1, NULL, 1, 2, 1, 9, N'02773124758', NULL, N'Chi Nhánh Bến Thành (Quận 1)', N'https://res.cloudinary.com/rbw3u9td/image/upload/v1792691575/rental_shop_stores/avatars/w4cp1vzdxodewizr2afc.jpg', N'https://res.cloudinary.com/rbw3u9td/image/upload/v1792691617/rental_shop_stores/covers/mums5g7p7hid8sxkm7gm.jpg', N'Chi nhánh trung tâm chuyên cung cấp hoa tươi cao cấp và dịch vụ điện hoa hỏa tốc.', N'123 Lê Lợi, Phường Bến Thành, Quận 1, TP. Hồ Chí Minh', N'rental_shop_stores/avatars/w4cp1vzdxodewizr2afc', N'rental_shop_stores/covers/mums5g7p7hid8sxkm7gm', NULL, NULL);
INSERT INTO dbo.[stores] ([e_wallet], [is_open], [latitude], [longitude], [pending_balance], [point], [rating], [status], [commission_id], [created_at], [id], [owner_id], [store_level_id], [version], [phone], [creation_request_key], [name], [avatar], [cover], [bio], [address], [avatar_public_id], [cover_public_id], [featured_image_public_ids], [featured_images]) VALUES (51466400.00, 1, 10.7382, 106.7085, 915000.00, 4816, 5.0, 1, 2, NULL, 2, 6, 2, 22, N'0945870811', NULL, N'Chi Nhánh Phú Mỹ Hưng (Quận 7)', N'https://images.unsplash.com/photo-1563245372-f21724e3856d?w=400&q=80', N'https://images.unsplash.com/photo-1526047932273-341f2a7631f9?w=1200&q=80', N'Chi nhánh hoa tươi cao cấp Phú Mỹ Hưng - Quận 7 trực thuộc chuỗi Bloom Flower. Chúng tôi chuyên cung cấp hoa nhập khẩu Hà Lan, Ecuador, hoa tươi Đà Lạt cắt cành trong ngày, lẵng hoa sinh nhật, kệ hoa khai trương sang trọng và dịch vụ điện hoa hỏa tốc trong 2 giờ.', N'456 Nguyễn Thị Thập, phường Tân Hưng, TP.HCM', NULL, NULL, NULL, N'');
INSERT INTO dbo.[stores] ([e_wallet], [is_open], [latitude], [longitude], [pending_balance], [point], [rating], [status], [commission_id], [created_at], [id], [owner_id], [store_level_id], [version], [phone], [creation_request_key], [name], [avatar], [cover], [bio], [address], [avatar_public_id], [cover_public_id], [featured_image_public_ids], [featured_images]) VALUES (0.00, 1, 10.8505, 106.7719, 0.00, 0, 5.0, 1, 1, NULL, 3, NULL, 1, 1, N'0285555666', NULL, N'Chi Nhánh Làng Đại Học (TP. Thủ Đức)', NULL, NULL, N'Chuyên hoa tốt nghiệp, sinh nhật, sự kiện sinh viên và hoa tươi theo yêu cầu.', N'789 Võ Văn Ngân, Linh Chiểu, TP. Thủ Đức, TP. Hồ Chí Minh', NULL, NULL, NULL, NULL);
INSERT INTO dbo.[stores] ([e_wallet], [is_open], [latitude], [longitude], [pending_balance], [point], [rating], [status], [commission_id], [created_at], [id], [owner_id], [store_level_id], [version], [phone], [creation_request_key], [name], [avatar], [cover], [bio], [address], [avatar_public_id], [cover_public_id], [featured_image_public_ids], [featured_images]) VALUES (0.00, 1, 10.4597, 105.6325, 0.00, 0, 5.0, 1, 1, '2026-10-01 19:40:30.720586', 7, 6, 1, 2, N'08730123123', NULL, N'Tiệm Hoa Sài Gòn - Chi Nhánh Cao Lãnh', NULL, NULL, N'Hoa tươi mỗi ngày, gói hoa theo nhu cầu của bạn', N'112, An Bình, Cao Lãnh, Đồng Tháp', NULL, NULL, NULL, NULL);
SET IDENTITY_INSERT dbo.[stores] OFF;
GO

-- Dữ liệu bảng: style_values
SET IDENTITY_INSERT dbo.[style_values] ON;
INSERT INTO dbo.[style_values] ([is_deleted], [created_at], [id], [style_id], [updated_at], [name]) VALUES (0, '2026-10-02 14:43:58.873', 1, 6, '2026-10-02 14:43:58.873', N'Hướng dương');
INSERT INTO dbo.[style_values] ([is_deleted], [created_at], [id], [style_id], [updated_at], [name]) VALUES (0, '2026-10-02 14:43:58.883', 2, 6, '2026-10-02 14:43:58.883', N'Hoa hồng');
INSERT INTO dbo.[style_values] ([is_deleted], [created_at], [id], [style_id], [updated_at], [name]) VALUES (0, '2026-10-02 14:43:58.883', 3, 6, '2026-10-02 14:43:58.883', N'Tulip');
INSERT INTO dbo.[style_values] ([is_deleted], [created_at], [id], [style_id], [updated_at], [name]) VALUES (0, '2026-10-02 14:43:58.887', 4, 6, '2026-10-02 14:43:58.887', N'Cẩm tú cầu');
INSERT INTO dbo.[style_values] ([is_deleted], [created_at], [id], [style_id], [updated_at], [name]) VALUES (0, '2026-10-02 14:43:58.89', 5, 6, '2026-10-02 14:43:58.89', N'Lan hồ điệp');
INSERT INTO dbo.[style_values] ([is_deleted], [created_at], [id], [style_id], [updated_at], [name]) VALUES (0, '2026-10-02 14:43:58.89', 6, 6, '2026-10-02 14:43:58.89', N'Calla Lily');
INSERT INTO dbo.[style_values] ([is_deleted], [created_at], [id], [style_id], [updated_at], [name]) VALUES (0, '2026-10-02 14:43:58.89', 7, 6, '2026-10-02 14:43:58.89', N'Hoa Baby');
INSERT INTO dbo.[style_values] ([is_deleted], [created_at], [id], [style_id], [updated_at], [name]) VALUES (0, '2026-10-02 14:43:58.907', 8, 3, '2026-10-02 14:43:58.907', N'Bó hoa');
INSERT INTO dbo.[style_values] ([is_deleted], [created_at], [id], [style_id], [updated_at], [name]) VALUES (0, '2026-10-02 14:43:58.907', 9, 3, '2026-10-02 14:43:58.907', N'Giỏ hoa');
INSERT INTO dbo.[style_values] ([is_deleted], [created_at], [id], [style_id], [updated_at], [name]) VALUES (0, '2026-10-02 14:43:58.907', 10, 3, '2026-10-02 14:43:58.907', N'Bình hoa');
INSERT INTO dbo.[style_values] ([is_deleted], [created_at], [id], [style_id], [updated_at], [name]) VALUES (0, '2026-10-02 14:43:58.907', 11, 3, '2026-10-02 14:43:58.907', N'Hộp hoa');
INSERT INTO dbo.[style_values] ([is_deleted], [created_at], [id], [style_id], [updated_at], [name]) VALUES (0, '2026-10-02 14:43:58.907', 12, 3, '2026-10-02 14:43:58.907', N'Chậu hoa');
INSERT INTO dbo.[style_values] ([is_deleted], [created_at], [id], [style_id], [updated_at], [name]) VALUES (0, '2026-10-02 14:43:58.907', 13, 3, '2026-10-02 14:43:58.907', N'Kệ hoa');
INSERT INTO dbo.[style_values] ([is_deleted], [created_at], [id], [style_id], [updated_at], [name]) VALUES (0, '2026-10-02 14:43:58.923', 14, 2, '2026-10-02 14:43:58.923', N'Vàng');
INSERT INTO dbo.[style_values] ([is_deleted], [created_at], [id], [style_id], [updated_at], [name]) VALUES (0, '2026-10-02 14:43:58.923', 15, 2, '2026-10-02 14:43:58.923', N'Hồng');
INSERT INTO dbo.[style_values] ([is_deleted], [created_at], [id], [style_id], [updated_at], [name]) VALUES (0, '2026-10-02 14:43:58.923', 16, 2, '2026-10-02 14:43:58.923', N'Trắng');
INSERT INTO dbo.[style_values] ([is_deleted], [created_at], [id], [style_id], [updated_at], [name]) VALUES (0, '2026-10-02 14:43:58.923', 17, 2, '2026-10-02 14:43:58.923', N'Đỏ');
INSERT INTO dbo.[style_values] ([is_deleted], [created_at], [id], [style_id], [updated_at], [name]) VALUES (0, '2026-10-02 14:43:58.923', 18, 2, '2026-10-02 14:43:58.923', N'Tím');
INSERT INTO dbo.[style_values] ([is_deleted], [created_at], [id], [style_id], [updated_at], [name]) VALUES (0, '2026-10-02 14:43:58.94', 19, 2, '2026-10-22 16:31:44.86', N'Xanh dương');
INSERT INTO dbo.[style_values] ([is_deleted], [created_at], [id], [style_id], [updated_at], [name]) VALUES (0, '2026-10-02 14:43:58.94', 20, 2, '2026-10-02 14:43:58.94', N'Cam');
INSERT INTO dbo.[style_values] ([is_deleted], [created_at], [id], [style_id], [updated_at], [name]) VALUES (0, '2026-10-02 14:43:58.947', 21, 7, '2026-10-02 14:43:58.947', N'Sinh nhật');
INSERT INTO dbo.[style_values] ([is_deleted], [created_at], [id], [style_id], [updated_at], [name]) VALUES (0, '2026-10-02 14:43:58.957', 22, 7, '2026-10-02 14:43:58.957', N'Tốt nghiệp');
INSERT INTO dbo.[style_values] ([is_deleted], [created_at], [id], [style_id], [updated_at], [name]) VALUES (0, '2026-10-02 14:43:58.957', 23, 7, '2026-10-02 14:43:58.957', N'Khai trương');
INSERT INTO dbo.[style_values] ([is_deleted], [created_at], [id], [style_id], [updated_at], [name]) VALUES (0, '2026-10-02 14:43:58.957', 24, 7, '2026-10-02 14:43:58.957', N'Tình yêu');
INSERT INTO dbo.[style_values] ([is_deleted], [created_at], [id], [style_id], [updated_at], [name]) VALUES (0, '2026-10-02 14:43:58.957', 25, 7, '2026-10-02 14:43:58.957', N'Chúc mừng');
INSERT INTO dbo.[style_values] ([is_deleted], [created_at], [id], [style_id], [updated_at], [name]) VALUES (0, '2026-10-02 14:43:58.957', 26, 7, '2026-10-02 14:43:58.957', N'Đám cưới');
INSERT INTO dbo.[style_values] ([is_deleted], [created_at], [id], [style_id], [updated_at], [name]) VALUES (0, '2026-10-02 14:43:58.97', 27, 7, '2026-10-02 14:43:58.97', N'Chia buồn');
INSERT INTO dbo.[style_values] ([is_deleted], [created_at], [id], [style_id], [updated_at], [name]) VALUES (0, '2026-10-02 14:43:58.99', 28, 4, '2026-10-02 14:43:58.99', N'Hàn Quốc hiện đại');
INSERT INTO dbo.[style_values] ([is_deleted], [created_at], [id], [style_id], [updated_at], [name]) VALUES (0, '2026-10-02 14:43:58.99', 29, 4, '2026-10-02 14:43:58.99', N'Vintage cổ điển');
INSERT INTO dbo.[style_values] ([is_deleted], [created_at], [id], [style_id], [updated_at], [name]) VALUES (0, '2026-10-02 14:43:58.99', 30, 4, '2026-10-02 14:43:58.99', N'Tối giản Minimalist');
INSERT INTO dbo.[style_values] ([is_deleted], [created_at], [id], [style_id], [updated_at], [name]) VALUES (0, '2026-10-02 14:43:59.013', 31, 4, '2026-10-22 16:29:56.063', N'Sang trọng');
INSERT INTO dbo.[style_values] ([is_deleted], [created_at], [id], [style_id], [updated_at], [name]) VALUES (0, '2026-10-02 14:43:59.05', 32, 5, '2026-10-02 14:43:59.05', N'Size S (Nhỏ)');
INSERT INTO dbo.[style_values] ([is_deleted], [created_at], [id], [style_id], [updated_at], [name]) VALUES (0, '2026-10-02 14:43:59.06', 33, 5, '2026-10-02 14:43:59.06', N'Size M (Vừa)');
INSERT INTO dbo.[style_values] ([is_deleted], [created_at], [id], [style_id], [updated_at], [name]) VALUES (0, '2026-10-02 14:43:59.073', 34, 5, '2026-10-02 14:43:59.073', N'Size L (Lớn)');
INSERT INTO dbo.[style_values] ([is_deleted], [created_at], [id], [style_id], [updated_at], [name]) VALUES (0, '2026-10-19 11:48:23.91', 69, 6, '2026-10-19 11:48:23.91', N'Cẩm Chướng');
INSERT INTO dbo.[style_values] ([is_deleted], [created_at], [id], [style_id], [updated_at], [name]) VALUES (0, '2026-10-19 12:01:51.227', 70, 6, '2026-10-19 12:01:51.227', N'Cúc đồng tiền');
INSERT INTO dbo.[style_values] ([is_deleted], [created_at], [id], [style_id], [updated_at], [name]) VALUES (0, '2026-10-19 12:07:00.997', 71, 6, '2026-10-19 12:07:00.997', N'Cúc Họa Mi');
INSERT INTO dbo.[style_values] ([is_deleted], [created_at], [id], [style_id], [updated_at], [name]) VALUES (0, '2026-10-22 16:29:11.3', 72, 4, '2026-10-22 16:29:11.3', N'Lãng mạn');
INSERT INTO dbo.[style_values] ([is_deleted], [created_at], [id], [style_id], [updated_at], [name]) VALUES (0, '2026-10-22 16:29:26.21', 73, 4, '2026-10-22 16:29:26.21', N'Nhẹ nhàng');
INSERT INTO dbo.[style_values] ([is_deleted], [created_at], [id], [style_id], [updated_at], [name]) VALUES (0, '2026-10-22 16:29:38.767', 74, 4, '2026-10-22 16:29:38.767', N'Hoa khô');
INSERT INTO dbo.[style_values] ([is_deleted], [created_at], [id], [style_id], [updated_at], [name]) VALUES (0, '2026-10-22 16:30:17.163', 75, 4, '2026-10-22 16:30:17.163', N'Nhẹ nhàng, ngọt ngào');
INSERT INTO dbo.[style_values] ([is_deleted], [created_at], [id], [style_id], [updated_at], [name]) VALUES (0, '2026-10-22 16:31:37.1', 76, 2, '2026-10-22 16:31:37.1', N'Đen');
INSERT INTO dbo.[style_values] ([is_deleted], [created_at], [id], [style_id], [updated_at], [name]) VALUES (0, '2026-10-23 00:35:33.147', 77, 6, '2026-10-23 00:35:33.147', N'Ly');
SET IDENTITY_INSERT dbo.[style_values] OFF;
GO

-- Dữ liệu bảng: styles
SET IDENTITY_INSERT dbo.[styles] ON;
INSERT INTO dbo.[styles] ([is_deleted], [category_id], [created_at], [id], [updated_at], [name]) VALUES (0, NULL, '2026-10-01 09:15:32.333', 2, '2026-10-02 14:43:58.777', N'Màu sắc');
INSERT INTO dbo.[styles] ([is_deleted], [category_id], [created_at], [id], [updated_at], [name]) VALUES (0, NULL, '2026-10-01 09:15:32.333', 3, '2026-10-02 14:43:58.823', N'Kiểu dáng');
INSERT INTO dbo.[styles] ([is_deleted], [category_id], [created_at], [id], [updated_at], [name]) VALUES (0, NULL, '2026-10-01 09:15:32.333', 4, '2026-10-01 09:15:32.333', N'Phong cách thiết kế');
INSERT INTO dbo.[styles] ([is_deleted], [category_id], [created_at], [id], [updated_at], [name]) VALUES (0, NULL, '2026-10-01 09:15:32.333', 5, '2026-10-02 14:43:58.823', N'Kích cỡ');
INSERT INTO dbo.[styles] ([is_deleted], [category_id], [created_at], [id], [updated_at], [name]) VALUES (0, NULL, '2026-10-02 14:43:58.84', 6, '2026-10-02 14:43:58.84', N'Loại hoa');
INSERT INTO dbo.[styles] ([is_deleted], [category_id], [created_at], [id], [updated_at], [name]) VALUES (0, NULL, '2026-10-02 14:43:58.947', 7, '2026-10-02 14:43:58.947', N'Dịp tặng');
SET IDENTITY_INSERT dbo.[styles] OFF;
GO

-- Dữ liệu bảng: transactions
SET IDENTITY_INSERT dbo.[transactions] ON;
INSERT INTO dbo.[transactions] ([amount], [earned_points], [is_up], [created_at], [id], [order_id], [store_id], [user_id], [status], [wallet_bucket], [type], [request_key], [description]) VALUES (2350000.00, NULL, 1, '2026-10-06 17:52:11.233', 1, 8, 2, 34, N'COMPLETED', N'AVAILABLE', N'ORDER_PAYMENT', NULL, N'Nhận thanh toán đơn hàng hoa tươi #8 (Ngân Hàng Shinhan Bank - Chi Nhánh PMH)');
INSERT INTO dbo.[transactions] ([amount], [earned_points], [is_up], [created_at], [id], [order_id], [store_id], [user_id], [status], [wallet_bucket], [type], [request_key], [description]) VALUES (188000.00, NULL, 0, '2026-10-06 17:53:11.233', 2, 8, 2, 6, N'COMPLETED', N'AVAILABLE', N'COMMISSION_FEE', NULL, N'Phí hoa hồng sàn 8% áp dụng cho đơn hàng #8');
INSERT INTO dbo.[transactions] ([amount], [earned_points], [is_up], [created_at], [id], [order_id], [store_id], [user_id], [status], [wallet_bucket], [type], [request_key], [description]) VALUES (1625000.00, NULL, 1, '2026-10-06 15:37:11.233', 3, 9, 2, 33, N'COMPLETED', N'AVAILABLE', N'ORDER_PAYMENT', NULL, N'Nhận thanh toán đơn hàng hoa tươi #9 (Em gái Trâm Anh)');
INSERT INTO dbo.[transactions] ([amount], [earned_points], [is_up], [created_at], [id], [order_id], [store_id], [user_id], [status], [wallet_bucket], [type], [request_key], [description]) VALUES (130000.00, NULL, 0, '2026-10-06 15:38:11.233', 4, 9, 2, 6, N'COMPLETED', N'AVAILABLE', N'COMMISSION_FEE', NULL, N'Phí hoa hồng sàn 8% áp dụng cho đơn hàng #9');
INSERT INTO dbo.[transactions] ([amount], [earned_points], [is_up], [created_at], [id], [order_id], [store_id], [user_id], [status], [wallet_bucket], [type], [request_key], [description]) VALUES (745000.00, NULL, 1, '2026-10-06 15:35:11.233', 5, 10, 2, 5, N'COMPLETED', N'AVAILABLE', N'ORDER_PAYMENT', NULL, N'Nhận thanh toán đơn hàng hoa tươi #10 (Bạn Thảo My)');
INSERT INTO dbo.[transactions] ([amount], [earned_points], [is_up], [created_at], [id], [order_id], [store_id], [user_id], [status], [wallet_bucket], [type], [request_key], [description]) VALUES (59600.00, NULL, 0, '2026-10-06 15:36:11.233', 6, 10, 2, 6, N'COMPLETED', N'AVAILABLE', N'COMMISSION_FEE', NULL, N'Phí hoa hồng sàn 8% áp dụng cho đơn hàng #10');
INSERT INTO dbo.[transactions] ([amount], [earned_points], [is_up], [created_at], [id], [order_id], [store_id], [user_id], [status], [wallet_bucket], [type], [request_key], [description]) VALUES (915000.00, NULL, 1, '2026-10-06 10:20:11.233', 7, 11, 2, 34, N'COMPLETED', N'AVAILABLE', N'ORDER_PAYMENT', NULL, N'Nhận thanh toán đơn hàng hoa tươi #11 (Bác Sĩ Trưởng Khoa)');
INSERT INTO dbo.[transactions] ([amount], [earned_points], [is_up], [created_at], [id], [order_id], [store_id], [user_id], [status], [wallet_bucket], [type], [request_key], [description]) VALUES (73200.00, NULL, 0, '2026-10-06 10:21:11.233', 8, 11, 2, 6, N'COMPLETED', N'AVAILABLE', N'COMMISSION_FEE', NULL, N'Phí hoa hồng sàn 8% áp dụng cho đơn hàng #11');
INSERT INTO dbo.[transactions] ([amount], [earned_points], [is_up], [created_at], [id], [order_id], [store_id], [user_id], [status], [wallet_bucket], [type], [request_key], [description]) VALUES (2000000.00, NULL, 1, '2026-10-05 14:25:11.233', 9, 12, 2, 34, N'COMPLETED', N'AVAILABLE', N'ORDER_PAYMENT', NULL, N'Nhận thanh toán đơn hàng hoa tươi #12 (Công Ty Bất Động Sản Hưng Phát)');
INSERT INTO dbo.[transactions] ([amount], [earned_points], [is_up], [created_at], [id], [order_id], [store_id], [user_id], [status], [wallet_bucket], [type], [request_key], [description]) VALUES (160000.00, NULL, 0, '2026-10-05 14:26:11.233', 10, 12, 2, 6, N'COMPLETED', N'AVAILABLE', N'COMMISSION_FEE', NULL, N'Phí hoa hồng sàn 8% áp dụng cho đơn hàng #12');
INSERT INTO dbo.[transactions] ([amount], [earned_points], [is_up], [created_at], [id], [order_id], [store_id], [user_id], [status], [wallet_bucket], [type], [request_key], [description]) VALUES (2325000.00, NULL, 1, '2026-10-04 11:50:11.233', 11, 13, 2, 33, N'COMPLETED', N'AVAILABLE', N'ORDER_PAYMENT', NULL, N'Nhận thanh toán đơn hàng hoa tươi #13 (Bà Nội)');
INSERT INTO dbo.[transactions] ([amount], [earned_points], [is_up], [created_at], [id], [order_id], [store_id], [user_id], [status], [wallet_bucket], [type], [request_key], [description]) VALUES (186000.00, NULL, 0, '2026-10-04 11:51:11.233', 12, 13, 2, 6, N'COMPLETED', N'AVAILABLE', N'COMMISSION_FEE', NULL, N'Phí hoa hồng sàn 8% áp dụng cho đơn hàng #13');
INSERT INTO dbo.[transactions] ([amount], [earned_points], [is_up], [created_at], [id], [order_id], [store_id], [user_id], [status], [wallet_bucket], [type], [request_key], [description]) VALUES (675000.00, NULL, 1, '2026-10-03 16:15:11.233', 13, 14, 2, 5, N'COMPLETED', N'AVAILABLE', N'ORDER_PAYMENT', NULL, N'Nhận thanh toán đơn hàng hoa tươi #14 (Bạn gái Diễm Hương)');
INSERT INTO dbo.[transactions] ([amount], [earned_points], [is_up], [created_at], [id], [order_id], [store_id], [user_id], [status], [wallet_bucket], [type], [request_key], [description]) VALUES (54000.00, NULL, 0, '2026-10-03 16:16:11.233', 14, 14, 2, 6, N'COMPLETED', N'AVAILABLE', N'COMMISSION_FEE', NULL, N'Phí hoa hồng sàn 8% áp dụng cho đơn hàng #14');
INSERT INTO dbo.[transactions] ([amount], [earned_points], [is_up], [created_at], [id], [order_id], [store_id], [user_id], [status], [wallet_bucket], [type], [request_key], [description]) VALUES (715000.00, NULL, 1, '2026-10-02 09:35:11.233', 15, 15, 2, 33, N'COMPLETED', N'AVAILABLE', N'ORDER_PAYMENT', NULL, N'Nhận thanh toán đơn hàng hoa tươi #15 (Chị Thảo Nguyên)');
INSERT INTO dbo.[transactions] ([amount], [earned_points], [is_up], [created_at], [id], [order_id], [store_id], [user_id], [status], [wallet_bucket], [type], [request_key], [description]) VALUES (57200.00, NULL, 0, '2026-10-02 09:36:11.233', 16, 15, 2, 6, N'COMPLETED', N'AVAILABLE', N'COMMISSION_FEE', NULL, N'Phí hoa hồng sàn 8% áp dụng cho đơn hàng #15');
INSERT INTO dbo.[transactions] ([amount], [earned_points], [is_up], [created_at], [id], [order_id], [store_id], [user_id], [status], [wallet_bucket], [type], [request_key], [description]) VALUES (915000.00, NULL, 1, '2026-10-01 13:20:11.233', 17, 16, 2, 34, N'COMPLETED', N'AVAILABLE', N'ORDER_PAYMENT', NULL, N'Nhận thanh toán đơn hàng hoa tươi #16 (Khách Sạn MerPerle Crystal Palace)');
INSERT INTO dbo.[transactions] ([amount], [earned_points], [is_up], [created_at], [id], [order_id], [store_id], [user_id], [status], [wallet_bucket], [type], [request_key], [description]) VALUES (73200.00, NULL, 0, '2026-10-01 13:21:11.233', 18, 16, 2, 6, N'COMPLETED', N'AVAILABLE', N'COMMISSION_FEE', NULL, N'Phí hoa hồng sàn 8% áp dụng cho đơn hàng #16');
INSERT INTO dbo.[transactions] ([amount], [earned_points], [is_up], [created_at], [id], [order_id], [store_id], [user_id], [status], [wallet_bucket], [type], [request_key], [description]) VALUES (5000000.00, NULL, 0, '2026-10-05 17:00:11.623', 19, NULL, 2, 6, N'COMPLETED', N'AVAILABLE', N'WITHDRAWAL', NULL, N'Rút tiền doanh thu tuần về tài khoản Vietcombank - STK 0123456789 - Trần Đối Tác');
INSERT INTO dbo.[transactions] ([amount], [earned_points], [is_up], [created_at], [id], [order_id], [store_id], [user_id], [status], [wallet_bucket], [type], [request_key], [description]) VALUES (10000000.00, NULL, 0, '2026-10-02 18:30:11.627', 20, NULL, 2, 6, N'COMPLETED', N'AVAILABLE', N'WITHDRAWAL', NULL, N'Rút tiền lợi nhuận bán hoa về tài khoản Techcombank - STK 1903456789 - Trần Đối Tác');
INSERT INTO dbo.[transactions] ([amount], [earned_points], [is_up], [created_at], [id], [order_id], [store_id], [user_id], [status], [wallet_bucket], [type], [request_key], [description]) VALUES (31717000.00, 3447, 1, '2026-10-13 15:16:24.447', 21, 25, 2, NULL, N'COMPLETED', N'AVAILABLE', N'ORDER_PAYMENT', NULL, N'Doanh thu đơn hàng #25 (+ 3,447 điểm thi đua [Hạng Cửa hàng uy tín])');
INSERT INTO dbo.[transactions] ([amount], [earned_points], [is_up], [created_at], [id], [order_id], [store_id], [user_id], [status], [wallet_bucket], [type], [request_key], [description]) VALUES (598000.00, 65, 1, '2026-10-22 15:57:26.123', 22, 29, 2, NULL, N'COMPLETED', N'AVAILABLE', N'ORDER_PAYMENT', NULL, N'Doanh thu đơn hàng #29 (+ 65 điểm thi đua [Hạng Cửa hàng uy tín])');
INSERT INTO dbo.[transactions] ([amount], [earned_points], [is_up], [created_at], [id], [order_id], [store_id], [user_id], [status], [wallet_bucket], [type], [request_key], [description]) VALUES (675000.00, NULL, 1, '2026-10-23 23:20:45.897', 23, 34, 2, NULL, N'COMPLETED', N'PENDING', N'PAYMENT_HOLD', N'demo-funds:PAYMENT_HOLD:34', N'Nhận tiền chờ giao hàng đơn #34');
INSERT INTO dbo.[transactions] ([amount], [earned_points], [is_up], [created_at], [id], [order_id], [store_id], [user_id], [status], [wallet_bucket], [type], [request_key], [description]) VALUES (675000.00, NULL, 0, '2026-10-23 23:21:23.83', 24, 34, 2, NULL, N'COMPLETED', N'PENDING', N'REFUND', N'demo-funds:REFUND:34', N'Hoàn tiền chờ cho khách đơn #34');
INSERT INTO dbo.[transactions] ([amount], [earned_points], [is_up], [created_at], [id], [order_id], [store_id], [user_id], [status], [wallet_bucket], [type], [request_key], [description]) VALUES (675000.00, NULL, 1, '2026-10-24 14:37:01.54', 25, 35, 2, NULL, N'COMPLETED', N'PENDING', N'PAYMENT_HOLD', N'demo-funds:PAYMENT_HOLD:35', N'Nhận tiền chờ giao hàng đơn #35');
INSERT INTO dbo.[transactions] ([amount], [earned_points], [is_up], [created_at], [id], [order_id], [store_id], [user_id], [status], [wallet_bucket], [type], [request_key], [description]) VALUES (675000.00, NULL, 0, '2026-10-24 14:37:24.613', 26, 35, 2, NULL, N'COMPLETED', N'PENDING', N'REFUND', N'demo-funds:REFUND:35', N'Hoàn tiền chờ cho khách đơn #35');
INSERT INTO dbo.[transactions] ([amount], [earned_points], [is_up], [created_at], [id], [order_id], [store_id], [user_id], [status], [wallet_bucket], [type], [request_key], [description]) VALUES (675000.00, NULL, 1, '2026-10-24 17:22:18.137', 27, 36, 2, NULL, N'COMPLETED', N'PENDING', N'PAYMENT_HOLD', N'demo-funds:PAYMENT_HOLD:36', N'Nhận tiền chờ giao hàng đơn #36');
INSERT INTO dbo.[transactions] ([amount], [earned_points], [is_up], [created_at], [id], [order_id], [store_id], [user_id], [status], [wallet_bucket], [type], [request_key], [description]) VALUES (675000.00, NULL, 0, '2026-10-24 17:22:38.113', 28, 36, 2, NULL, N'COMPLETED', N'PENDING', N'REFUND', N'demo-funds:REFUND:36', N'Hoàn tiền chờ cho khách đơn #36');
INSERT INTO dbo.[transactions] ([amount], [earned_points], [is_up], [created_at], [id], [order_id], [store_id], [user_id], [status], [wallet_bucket], [type], [request_key], [description]) VALUES (675000.00, NULL, 1, '2026-10-24 18:51:45.153', 29, 37, 1, NULL, N'COMPLETED', N'PENDING', N'PAYMENT_HOLD', N'demo-funds:PAYMENT_HOLD:37', N'Nhận tiền chờ giao hàng đơn #37');
INSERT INTO dbo.[transactions] ([amount], [earned_points], [is_up], [created_at], [id], [order_id], [store_id], [user_id], [status], [wallet_bucket], [type], [request_key], [description]) VALUES (675000.00, NULL, 0, '2026-10-24 18:52:26.057', 30, 37, 1, NULL, N'COMPLETED', N'PENDING', N'REFUND', N'demo-funds:REFUND:37', N'Hoàn tiền chờ cho khách đơn #37');
INSERT INTO dbo.[transactions] ([amount], [earned_points], [is_up], [created_at], [id], [order_id], [store_id], [user_id], [status], [wallet_bucket], [type], [request_key], [description]) VALUES (915000.00, NULL, 1, '2026-10-25 23:51:05.047342', 31, 40, 2, NULL, N'COMPLETED', N'PENDING', N'PAYMENT_HOLD', N'demo-funds:PAYMENT_HOLD:40', N'Nhận tiền chờ giao hàng đơn #40');
INSERT INTO dbo.[transactions] ([amount], [earned_points], [is_up], [created_at], [id], [order_id], [store_id], [user_id], [status], [wallet_bucket], [type], [request_key], [description]) VALUES (915000.00, NULL, 0, '2026-10-25 23:51:29.977818', 32, 40, 2, NULL, N'COMPLETED', N'PENDING', N'REFUND', N'demo-funds:REFUND:40', N'Hoàn tiền chờ cho khách đơn #40');
INSERT INTO dbo.[transactions] ([amount], [earned_points], [is_up], [created_at], [id], [order_id], [store_id], [user_id], [status], [wallet_bucket], [type], [request_key], [description]) VALUES (715000.00, NULL, 1, '2026-10-26 00:06:42.134293', 33, 41, 2, NULL, N'COMPLETED', N'PENDING', N'PAYMENT_HOLD', N'demo-funds:PAYMENT_HOLD:41', N'Nhận tiền chờ giao hàng đơn #41');
INSERT INTO dbo.[transactions] ([amount], [earned_points], [is_up], [created_at], [id], [order_id], [store_id], [user_id], [status], [wallet_bucket], [type], [request_key], [description]) VALUES (715000.00, NULL, 0, '2026-10-26 00:07:06.371144', 34, 41, 2, NULL, N'COMPLETED', N'PENDING', N'REFUND', N'demo-funds:REFUND:41', N'Hoàn tiền chờ cho khách đơn #41');
INSERT INTO dbo.[transactions] ([amount], [earned_points], [is_up], [created_at], [id], [order_id], [store_id], [user_id], [status], [wallet_bucket], [type], [request_key], [description]) VALUES (545000.00, NULL, 1, '2026-10-26 21:15:23.770869', 35, 42, 2, NULL, N'COMPLETED', N'PENDING', N'PAYMENT_HOLD', N'demo-funds:PAYMENT_HOLD:42', N'Nhận tiền chờ giao hàng đơn #42');
INSERT INTO dbo.[transactions] ([amount], [earned_points], [is_up], [created_at], [id], [order_id], [store_id], [user_id], [status], [wallet_bucket], [type], [request_key], [description]) VALUES (545000.00, NULL, 0, '2026-10-26 21:25:35.707362', 36, 42, 2, NULL, N'COMPLETED', N'PENDING', N'HOLD_RELEASE', N'demo-funds:HOLD_RELEASE:42', N'Chuyển tiền chờ sang thu nhập đơn #42');
INSERT INTO dbo.[transactions] ([amount], [earned_points], [is_up], [created_at], [id], [order_id], [store_id], [user_id], [status], [wallet_bucket], [type], [request_key], [description]) VALUES (43600.00, NULL, 1, '2026-10-26 21:25:36.166617', 37, 42, 2, NULL, N'COMPLETED', N'PLATFORM', N'COMMISSION_FEE', N'demo-funds:COMMISSION_FEE:42', N'Hoa hồng đơn #42');
INSERT INTO dbo.[transactions] ([amount], [earned_points], [is_up], [created_at], [id], [order_id], [store_id], [user_id], [status], [wallet_bucket], [type], [request_key], [description]) VALUES (501400.00, 54, 1, '2026-10-26 21:25:36.835181', 38, 42, 2, NULL, N'COMPLETED', N'AVAILABLE', N'ORDER_PAYMENT', NULL, N'Doanh thu đơn hàng #42 (+ 54 điểm thi đua [Hạng Cửa hàng uy tín])');
INSERT INTO dbo.[transactions] ([amount], [earned_points], [is_up], [created_at], [id], [order_id], [store_id], [user_id], [status], [wallet_bucket], [type], [request_key], [description]) VALUES (915000.00, NULL, 1, '2026-10-26 21:34:37.733841', 39, 43, 2, NULL, N'COMPLETED', N'PENDING', N'PAYMENT_HOLD', N'demo-funds:PAYMENT_HOLD:43', N'Nhận tiền chờ giao hàng đơn #43');
SET IDENTITY_INSERT dbo.[transactions] OFF;
GO

-- Dữ liệu bảng: user_addresses
SET IDENTITY_INSERT dbo.[user_addresses] ON;
INSERT INTO dbo.[user_addresses] ([is_default], [created_at], [id], [user_id], [phone], [recipient_name], [address_detail]) VALUES (1, '2026-09-22 20:36:14.567', 1, 5, N'0909999888', N'Lê Khách Hàng Yêu Hoa', N'88 Nam Kỳ Khởi Nghĩa, Phường Bến Nghé, Quận 1, TP. Hồ Chí Minh');
INSERT INTO dbo.[user_addresses] ([is_default], [created_at], [id], [user_id], [phone], [recipient_name], [address_detail]) VALUES (0, '2026-09-22 20:36:14.587', 2, 5, N'0909999888', N'Lê Khách Hàng Yêu Hoa', N'88 Nam Kỳ Khởi Nghĩa, Phường Bến Nghé, Quận 1, TP. Hồ Chí Minh');
INSERT INTO dbo.[user_addresses] ([is_default], [created_at], [id], [user_id], [phone], [recipient_name], [address_detail]) VALUES (1, '2026-10-04 17:09:38.003', 3, 22, N'0901234567', N'Trần Thị Hoa', N'123 Nguyễn Huệ, Phường Bến Nghé, Quận 1, TP.HCM');
INSERT INTO dbo.[user_addresses] ([is_default], [created_at], [id], [user_id], [phone], [recipient_name], [address_detail]) VALUES (0, '2026-10-07 16:14:37.883', 4, 5, N'0912547893', N'Lý Gia Hân', N'112, Nguyễn Huệ, Cao Lãnh, Đồng Tháp');
INSERT INTO dbo.[user_addresses] ([is_default], [created_at], [id], [user_id], [phone], [recipient_name], [address_detail]) VALUES (1, '2026-10-21 20:47:23.53', 6, 37, N'0947475853', N'Lý Gia Bảo', N'309/45 Võ Văn Ngân, Linh Chiểu, Thủ Đức');
SET IDENTITY_INSERT dbo.[user_addresses] OFF;
GO

-- Dữ liệu bảng: user_follow_store
SET IDENTITY_INSERT dbo.[user_follow_store] ON;
INSERT INTO dbo.[user_follow_store] ([created_at], [id], [store_id], [user_id]) VALUES ('2026-10-26 21:11:14.980589', 1, 2, 5);
SET IDENTITY_INSERT dbo.[user_follow_store] OFF;
GO

-- Dữ liệu bảng: user_levels
SET IDENTITY_INSERT dbo.[user_levels] ON;
INSERT INTO dbo.[user_levels] ([discount], [is_deleted], [min_point], [created_at], [id], [updated_at], [name]) VALUES (0.00, 0, 0, '2026-09-22 20:36:14.84', 1, '2026-09-22 20:36:14.84', N'Thành Viên Đồng');
INSERT INTO dbo.[user_levels] ([discount], [is_deleted], [min_point], [created_at], [id], [updated_at], [name]) VALUES (3.00, 0, 200, '2026-09-22 20:36:14.86', 2, '2026-09-22 20:36:14.86', N'Thành Viên Bạc');
INSERT INTO dbo.[user_levels] ([discount], [is_deleted], [min_point], [created_at], [id], [updated_at], [name]) VALUES (5.00, 0, 500, '2026-09-22 20:36:14.873', 3, '2026-09-22 20:36:14.873', N'Thành Viên Vàng');
INSERT INTO dbo.[user_levels] ([discount], [is_deleted], [min_point], [created_at], [id], [updated_at], [name]) VALUES (10.00, 0, 1000, '2026-09-22 20:36:14.88', 4, '2026-09-22 20:36:14.88', N'VIP Kim Cương');
SET IDENTITY_INSERT dbo.[user_levels] OFF;
GO

-- Dữ liệu bảng: user_roles
INSERT INTO dbo.[user_roles] ([user_id], [role]) VALUES (1, N'ADMIN');
INSERT INTO dbo.[user_roles] ([user_id], [role]) VALUES (2, N'MANAGER');
INSERT INTO dbo.[user_roles] ([user_id], [role]) VALUES (2, N'USER');
INSERT INTO dbo.[user_roles] ([user_id], [role]) VALUES (5, N'USER');
INSERT INTO dbo.[user_roles] ([user_id], [role]) VALUES (6, N'MANAGER');
INSERT INTO dbo.[user_roles] ([user_id], [role]) VALUES (6, N'USER');
INSERT INTO dbo.[user_roles] ([user_id], [role]) VALUES (21, N'USER');
INSERT INTO dbo.[user_roles] ([user_id], [role]) VALUES (29, N'STAFF');
INSERT INTO dbo.[user_roles] ([user_id], [role]) VALUES (30, N'STAFF');
INSERT INTO dbo.[user_roles] ([user_id], [role]) VALUES (31, N'STAFF');
INSERT INTO dbo.[user_roles] ([user_id], [role]) VALUES (32, N'STAFF');
INSERT INTO dbo.[user_roles] ([user_id], [role]) VALUES (35, N'USER');
INSERT INTO dbo.[user_roles] ([user_id], [role]) VALUES (36, N'USER');
INSERT INTO dbo.[user_roles] ([user_id], [role]) VALUES (37, N'USER');
INSERT INTO dbo.[user_roles] ([user_id], [role]) VALUES (40, N'USER');
INSERT INTO dbo.[user_roles] ([user_id], [role]) VALUES (41, N'STAFF');
INSERT INTO dbo.[user_roles] ([user_id], [role]) VALUES (42, N'MANAGER');
INSERT INTO dbo.[user_roles] ([user_id], [role]) VALUES (42, N'USER');
INSERT INTO dbo.[user_roles] ([user_id], [role]) VALUES (43, N'ADMIN');
GO

-- Dữ liệu bảng: users
SET IDENTITY_INSERT dbo.[users] ON;
INSERT INTO dbo.[users] ([account_disabled_by_admin], [created_date], [creation_time_known], [e_wallet], [is_active], [is_email_active], [point], [retired_delivery_account], [created_at], [id], [otp_expiry], [store_id], [user_level_id], [otp_code], [phone], [role], [username], [email], [full_name], [cover], [address], [avatar_public_id], [avatar_url], [cover_public_id], [password]) VALUES (NULL, '2026-09-22', 0, 0.00, 1, 1, 0, NULL, '2026-09-22 00:00:00.0', 1, NULL, NULL, 1, NULL, N'0901111222', N'ADMIN', N'nguyenthanhminh', N'admin@bloomflower.vn', N'Nguyễn Thanh Minh', NULL, N'Tòa nhà Bitexco, Số 2 Hải Triều, Quận 1, TP. Hồ Chí Minh', NULL, NULL, NULL, N'$2a$10$MMgWoixo7KjB5HrbStq9g.NkNv9zWbVpdSlBmPjrncPse25LBqh82');
INSERT INTO dbo.[users] ([account_disabled_by_admin], [created_date], [creation_time_known], [e_wallet], [is_active], [is_email_active], [point], [retired_delivery_account], [created_at], [id], [otp_expiry], [store_id], [user_level_id], [otp_code], [phone], [role], [username], [email], [full_name], [cover], [address], [avatar_public_id], [avatar_url], [cover_public_id], [password]) VALUES (NULL, '2026-09-22', 0, 0.00, 1, 1, 0, NULL, '2026-09-22 00:00:00.0', 2, NULL, 1, 1, NULL, N'0902222333', N'MANAGER', N'nguyenquanghuy', N'manager.q1@bloomflower.vn', N'Nguyễn Quang Huy', NULL, N'123 Lê Lợi, Bến Thành, Quận 1, TP. Hồ Chí Minh', NULL, NULL, NULL, N'$2a$10$MMgWoixo7KjB5HrbStq9g.NkNv9zWbVpdSlBmPjrncPse25LBqh82');
INSERT INTO dbo.[users] ([account_disabled_by_admin], [created_date], [creation_time_known], [e_wallet], [is_active], [is_email_active], [point], [retired_delivery_account], [created_at], [id], [otp_expiry], [store_id], [user_level_id], [otp_code], [phone], [role], [username], [email], [full_name], [cover], [address], [avatar_public_id], [avatar_url], [cover_public_id], [password]) VALUES (NULL, '2026-09-22', 0, 0.00, 1, 1, 0, NULL, '2026-09-22 00:00:00.0', 5, NULL, NULL, 1, NULL, N'0933516292', N'USER', N'leminhhoa', N'khachhang@gmail.com', N'Lê Minh Hoa', NULL, N'88 Nam Kỳ Khởi Nghĩa, Bến Nghé, Quận 1, TP. Hồ Chí Minh', N'rental_shop_avatars/vn6lnqlye1xp8a7hcnjp', N'https://res.cloudinary.com/hquyvrip/image/upload/v1791116496/rental_shop_avatars/vn6lnqlye1xp8a7hcnjp.webp', NULL, N'$2a$10$hjJJ7jjXqJfdloZjn9914uSVZ6zooQ4Wq79mDGmS5UV42DIX97PxS');
INSERT INTO dbo.[users] ([account_disabled_by_admin], [created_date], [creation_time_known], [e_wallet], [is_active], [is_email_active], [point], [retired_delivery_account], [created_at], [id], [otp_expiry], [store_id], [user_level_id], [otp_code], [phone], [role], [username], [email], [full_name], [cover], [address], [avatar_public_id], [avatar_url], [cover_public_id], [password]) VALUES (NULL, '2026-09-22', 0, 0.00, 1, 1, 0, NULL, '2026-09-22 00:00:00.0', 6, NULL, 2, 1, NULL, N'0988776655', N'MANAGER', N'tranduclong', N'vendor@rentalshop.vn', N'Trần Đức Long', NULL, N'Vườn hoa Vạn Thành, TP. Đà Lạt, Lâm Đồng', NULL, NULL, NULL, N'$2a$10$Qrf8KxSKM0n/08y98lAkde9Z8oENlgR3kH0c9jp8o3Rr6VuSIRKL2');
INSERT INTO dbo.[users] ([account_disabled_by_admin], [created_date], [creation_time_known], [e_wallet], [is_active], [is_email_active], [point], [retired_delivery_account], [created_at], [id], [otp_expiry], [store_id], [user_level_id], [otp_code], [phone], [role], [username], [email], [full_name], [cover], [address], [avatar_public_id], [avatar_url], [cover_public_id], [password]) VALUES (NULL, '2026-10-01', 0, 0.00, 1, 1, 0, NULL, '2026-10-01 00:00:00.0', 21, NULL, NULL, 1, NULL, N'0933516123', N'USER', N'lygiahan', N'lygiahan17082006@gmail.com', N'Lý Gia Hân', NULL, N'An Bình, Huyện Cao Lãnh, Đồng Tháp', NULL, NULL, NULL, N'$2a$10$InXt8OBukPF2OkbasIL/rOtF/Je5cnXtC1bw7z/iyxDbuZ/OcZ6ha');
INSERT INTO dbo.[users] ([account_disabled_by_admin], [created_date], [creation_time_known], [e_wallet], [is_active], [is_email_active], [point], [retired_delivery_account], [created_at], [id], [otp_expiry], [store_id], [user_level_id], [otp_code], [phone], [role], [username], [email], [full_name], [cover], [address], [avatar_public_id], [avatar_url], [cover_public_id], [password]) VALUES (NULL, '2026-10-04', 0, 3000000.00, 1, 1, 0, 0, '2026-10-04 00:00:00.0', 22, NULL, NULL, 1, NULL, N'0901234567', N'USER', N'tranthihoa', N'khachhang2@gmail.com', N'Trần Thị Hoa', NULL, N'123 Nguyễn Huệ, Phường Bến Nghé, Quận 1, TP.HCM', NULL, NULL, NULL, N'$2a$10$kJwNeQyfV.YzFDNRzkLd..0HUpRTu29j4K5EGG5Kc7wXjFOrFoiva');
INSERT INTO dbo.[users] ([account_disabled_by_admin], [created_date], [creation_time_known], [e_wallet], [is_active], [is_email_active], [point], [retired_delivery_account], [created_at], [id], [otp_expiry], [store_id], [user_level_id], [otp_code], [phone], [role], [username], [email], [full_name], [cover], [address], [avatar_public_id], [avatar_url], [cover_public_id], [password]) VALUES (0, '2026-06-07', 0, 0.00, 1, 1, 0, 0, '2026-06-07 00:00:00.0', 29, NULL, 2, 1, NULL, N'0988776699', N'STAFF', N'tranminhtuan', N'vendor.demo@rentalshop.vn', N'Trần Minh Tuấn', NULL, NULL, NULL, NULL, NULL, N'$2a$10$Sym5N6j5L4/dItmLeO9jeOrWGu.XrPTwGSeO0AWdfQijEVG4/1FKu');
INSERT INTO dbo.[users] ([account_disabled_by_admin], [created_date], [creation_time_known], [e_wallet], [is_active], [is_email_active], [point], [retired_delivery_account], [created_at], [id], [otp_expiry], [store_id], [user_level_id], [otp_code], [phone], [role], [username], [email], [full_name], [cover], [address], [avatar_public_id], [avatar_url], [cover_public_id], [password]) VALUES (0, '2026-05-07', 0, 0.00, 1, 1, 0, 0, '2026-05-07 00:00:00.0', 30, NULL, 2, 1, NULL, N'0938112233', N'STAFF', N'nguyenmailan', N'mailan.florist@rentalshop.vn', N'Nguyễn Thị Mai Lan', NULL, NULL, NULL, NULL, NULL, N'$2a$10$3l13rw1yMRLExkZZlGxE/uz8lsUNLK1zSNx58mc93d5z38.10TS6m');
INSERT INTO dbo.[users] ([account_disabled_by_admin], [created_date], [creation_time_known], [e_wallet], [is_active], [is_email_active], [point], [retired_delivery_account], [created_at], [id], [otp_expiry], [store_id], [user_level_id], [otp_code], [phone], [role], [username], [email], [full_name], [cover], [address], [avatar_public_id], [avatar_url], [cover_public_id], [password]) VALUES (0, '2026-07-07', 0, 0.00, 1, 1, 0, 0, '2026-07-07 00:00:00.0', 31, NULL, 2, 1, NULL, N'0938223344', N'STAFF', N'tranbaophuc', N'baophuc.orders@rentalshop.vn', N'Trần Bảo Phúc', NULL, NULL, NULL, NULL, NULL, N'$2a$10$LSC9i6aK6ZEDosd0KwY8Ger16f2lIYPaLkw6y8GqUEiZEbr85FOxu');
INSERT INTO dbo.[users] ([account_disabled_by_admin], [created_date], [creation_time_known], [e_wallet], [is_active], [is_email_active], [point], [retired_delivery_account], [created_at], [id], [otp_expiry], [store_id], [user_level_id], [otp_code], [phone], [role], [username], [email], [full_name], [cover], [address], [avatar_public_id], [avatar_url], [cover_public_id], [password]) VALUES (0, '2026-08-07', 0, 0.00, 1, 1, 0, 0, '2026-08-07 00:00:00.0', 32, NULL, 2, 1, NULL, N'0938334455', N'STAFF', N'lehoangkhoi', N'hoangkhoi.stock@rentalshop.vn', N'Lê Hoàng Khôi', NULL, NULL, NULL, NULL, NULL, N'$2a$10$zwD5SzO2uNgsjCliZLZgOOV8siWxxmy4/x3Rce6iDmgOnbMDC2YaW');
INSERT INTO dbo.[users] ([account_disabled_by_admin], [created_date], [creation_time_known], [e_wallet], [is_active], [is_email_active], [point], [retired_delivery_account], [created_at], [id], [otp_expiry], [store_id], [user_level_id], [otp_code], [phone], [role], [username], [email], [full_name], [cover], [address], [avatar_public_id], [avatar_url], [cover_public_id], [password]) VALUES (0, '2026-07-07', 0, 0.00, 1, 1, 0, 0, '2026-07-07 00:00:00.0', 33, NULL, NULL, 1, NULL, N'0908776655', N'USER', N'nguyenbichtram', N'bichtram.flower@gmail.com', N'Nguyễn Ngọc Bích Trâm', NULL, N'Khu biệt thự Chateau, Phú Mỹ Hưng, Quận 7, TP.HCM', NULL, NULL, NULL, N'$2a$10$Mj8qw8HlllWRZcF5ZmbrY.HfxHhLUZqE9M0fT/dtm51ef/XYqNQVS');
INSERT INTO dbo.[users] ([account_disabled_by_admin], [created_date], [creation_time_known], [e_wallet], [is_active], [is_email_active], [point], [retired_delivery_account], [created_at], [id], [otp_expiry], [store_id], [user_level_id], [otp_code], [phone], [role], [username], [email], [full_name], [cover], [address], [avatar_public_id], [avatar_url], [cover_public_id], [password]) VALUES (0, '2026-06-07', 0, 0.00, 1, 1, 0, 0, '2026-06-07 00:00:00.0', 34, NULL, NULL, 1, NULL, N'02873009988', N'USER', N'namsaigon', N'contact@namsaigontech.vn', N'Công Ty Cổ Phần Công Nghệ Nam Sài Gòn', NULL, N'Tòa nhà MapleTree, 1060 Nguyễn Văn Linh, Tân Phong, Quận 7, TP.HCM', NULL, NULL, NULL, N'$2a$10$kz4gKF4gd3991Tmbg5DzreeV32INOL6M4e1uLqUlavUbl1XhdnZ3W');
INSERT INTO dbo.[users] ([account_disabled_by_admin], [created_date], [creation_time_known], [e_wallet], [is_active], [is_email_active], [point], [retired_delivery_account], [created_at], [id], [otp_expiry], [store_id], [user_level_id], [otp_code], [phone], [role], [username], [email], [full_name], [cover], [address], [avatar_public_id], [avatar_url], [cover_public_id], [password]) VALUES (0, '2026-10-07', 1, 0.00, 1, 1, 0, 0, '2026-10-07 10:23:25.307746', 35, NULL, NULL, NULL, NULL, N'0912457891', N'USER', N'dinhquocanh', N'quocanhug0124@gmail.com', N'Đinh Quốc Anh', NULL, N'Chợ Vàm, Huyện Phú Tân, An Giang', NULL, NULL, NULL, N'$2a$10$sQiQHOl2GWXJCpAjhpMOMOT1v1yuEmhdFNvL2YA2018CVmJqFJSHi');
INSERT INTO dbo.[users] ([account_disabled_by_admin], [created_date], [creation_time_known], [e_wallet], [is_active], [is_email_active], [point], [retired_delivery_account], [created_at], [id], [otp_expiry], [store_id], [user_level_id], [otp_code], [phone], [role], [username], [email], [full_name], [cover], [address], [avatar_public_id], [avatar_url], [cover_public_id], [password]) VALUES (0, '2026-10-13', 1, 0.00, 1, 1, 0, 0, '2026-10-13 08:00:36.06945', 36, NULL, NULL, NULL, NULL, N'0933141289', N'USER', N'lygiahan03', N'dotai22092006@gmail.com', N'Ly GiaHan', NULL, N'phước long, Huyện Bù Gia Mập, Bình Phước', NULL, NULL, NULL, N'$2a$10$wFIdVt5ItO3y/t716pk/ze/1NGF49CtNiktqoftpvpIX9e1t5Uu0W');
INSERT INTO dbo.[users] ([account_disabled_by_admin], [created_date], [creation_time_known], [e_wallet], [is_active], [is_email_active], [point], [retired_delivery_account], [created_at], [id], [otp_expiry], [store_id], [user_level_id], [otp_code], [phone], [role], [username], [email], [full_name], [cover], [address], [avatar_public_id], [avatar_url], [cover_public_id], [password]) VALUES (0, '2026-10-21', 1, 0.00, 1, 1, 0, 0, '2026-10-21 20:23:34.643912', 37, NULL, NULL, NULL, NULL, N'0947475853', N'USER', N'lygiabao02', N'lygiabao140424@gmail.com', N'Lý Gia Bảo', NULL, N'An Bình, TP. Cao Lãnh, Đồng Tháp', N'rental_shop_avatars/clhrpmrivykv9kkpxkok', N'https://res.cloudinary.com/rbw3u9td/image/upload/v1792590363/rental_shop_avatars/clhrpmrivykv9kkpxkok.webp', NULL, N'$2a$10$C5iUa/MLqJV040nDTVUmge14YaTcmsL8zHONYpHYEJ1C7C1mGi9XS');
INSERT INTO dbo.[users] ([account_disabled_by_admin], [created_date], [creation_time_known], [e_wallet], [is_active], [is_email_active], [point], [retired_delivery_account], [created_at], [id], [otp_expiry], [store_id], [user_level_id], [otp_code], [phone], [role], [username], [email], [full_name], [cover], [address], [avatar_public_id], [avatar_url], [cover_public_id], [password]) VALUES (0, '2026-10-24', 1, 0.00, 1, 1, 0, 0, '2026-10-24 15:45:58.084887', 38, NULL, NULL, NULL, NULL, N'09474758', N'STAFF', N'giabao', N'lygiabao14@gmail.com', N'Lý Gia Bảo', NULL, NULL, NULL, NULL, NULL, N'$2a$10$8j6Fxq/28C8OOZyM.93VTeB3Bgx.gE/D79L.V6X3scNvnXwZKo2ui');
INSERT INTO dbo.[users] ([account_disabled_by_admin], [created_date], [creation_time_known], [e_wallet], [is_active], [is_email_active], [point], [retired_delivery_account], [created_at], [id], [otp_expiry], [store_id], [user_level_id], [otp_code], [phone], [role], [username], [email], [full_name], [cover], [address], [avatar_public_id], [avatar_url], [cover_public_id], [password]) VALUES (0, '2026-10-24', 1, 0.00, 1, 1, 0, 0, '2026-10-24 15:53:53.165533', 39, NULL, 1, NULL, NULL, N'0933516293', N'STAFF', N'tranthimai', N'tranthimai@gmail.com', N'Trần Thị Mai', NULL, NULL, NULL, NULL, NULL, N'$2a$10$CXE6FFFW6idX2dw3gGLRFuIWVS0UWei5OjmIMQpcgbs1Kzwr9bkxa');
INSERT INTO dbo.[users] ([account_disabled_by_admin], [created_date], [creation_time_known], [e_wallet], [is_active], [is_email_active], [point], [retired_delivery_account], [created_at], [id], [otp_expiry], [store_id], [user_level_id], [otp_code], [phone], [role], [username], [email], [full_name], [cover], [address], [avatar_public_id], [avatar_url], [cover_public_id], [password]) VALUES (0, '2026-10-28', 1, 0.00, 1, 1, 0, 0, '2026-10-28 13:28:58.148572', 40, NULL, NULL, NULL, NULL, N'0901234567', N'USER', N'local_customer', N'local_customer@local.test', N'Local test USER', NULL, N'123 Test, Quan 1', NULL, NULL, NULL, N'$2a$10$z7khrXSywU2ZsWrFRmMCMuLRkHF03/WpWqDcK/6iA/0s9UXPNLse.');
INSERT INTO dbo.[users] ([account_disabled_by_admin], [created_date], [creation_time_known], [e_wallet], [is_active], [is_email_active], [point], [retired_delivery_account], [created_at], [id], [otp_expiry], [store_id], [user_level_id], [otp_code], [phone], [role], [username], [email], [full_name], [cover], [address], [avatar_public_id], [avatar_url], [cover_public_id], [password]) VALUES (0, '2026-10-28', 1, 0.00, 1, 1, 0, 0, '2026-10-28 13:28:58.148572', 41, NULL, 1, NULL, NULL, N'0901234567', N'STAFF', N'local_staff', N'local_staff@local.test', N'Local test STAFF', NULL, N'123 Test, Quan 1', NULL, NULL, NULL, N'$2a$10$LKiS.Wn5wc/mvFmhJIWnteoESx1XDmtDRE.2fK9A.Ko2I.mgc76rG');
INSERT INTO dbo.[users] ([account_disabled_by_admin], [created_date], [creation_time_known], [e_wallet], [is_active], [is_email_active], [point], [retired_delivery_account], [created_at], [id], [otp_expiry], [store_id], [user_level_id], [otp_code], [phone], [role], [username], [email], [full_name], [cover], [address], [avatar_public_id], [avatar_url], [cover_public_id], [password]) VALUES (0, '2026-10-28', 1, 0.00, 1, 1, 0, 0, '2026-10-28 13:28:58.148572', 42, NULL, 1, NULL, NULL, N'0901234567', N'MANAGER', N'local_manager', N'local_manager@local.test', N'Local test MANAGER', NULL, N'123 Test, Quan 1', NULL, NULL, NULL, N'$2a$10$dBhdGThnWbgk5lrMNKGL.uRM8mrqKfRj.UKJ5yaijWovR7eXcvCu2');
INSERT INTO dbo.[users] ([account_disabled_by_admin], [created_date], [creation_time_known], [e_wallet], [is_active], [is_email_active], [point], [retired_delivery_account], [created_at], [id], [otp_expiry], [store_id], [user_level_id], [otp_code], [phone], [role], [username], [email], [full_name], [cover], [address], [avatar_public_id], [avatar_url], [cover_public_id], [password]) VALUES (0, '2026-10-28', 1, 0.00, 1, 1, 0, 0, '2026-10-28 13:28:58.148572', 43, NULL, NULL, NULL, NULL, N'0901234567', N'ADMIN', N'local_admin', N'local_admin@local.test', N'Local test ADMIN', NULL, N'123 Test, Quan 1', NULL, NULL, NULL, N'$2a$10$Z7l9U2HYTQUFm20qVJ4DDORm2vbAzxb1JYzjY5HC9f8uE2TFRUBI6');
SET IDENTITY_INSERT dbo.[users] OFF;
GO

-- Dữ liệu bảng: vouchers
SET IDENTITY_INSERT dbo.[vouchers] ON;
INSERT INTO dbo.[vouchers] ([discount_value], [end_date], [is_active], [max_discount_amount], [min_order_value], [quantity], [start_date], [used_count], [created_at], [id], [store_id], [discount_type], [code], [name]) VALUES (50000.00, '2026-11-22', 1, NULL, 300000.00, 50, '2026-09-17', 12, '2026-09-22 20:36:14.803', 2, NULL, N'FREE_SHIP', N'FREESHIP', N'Miễn Phí Vận Chuyển Toàn Thành Phố');
INSERT INTO dbo.[vouchers] ([discount_value], [end_date], [is_active], [max_discount_amount], [min_order_value], [quantity], [start_date], [used_count], [created_at], [id], [store_id], [discount_type], [code], [name]) VALUES (50000.00, '2027-03-22', 1, NULL, 500000.00, 200, '2026-09-20', 29, '2026-09-22 20:36:14.817', 3, NULL, N'FIXED_AMOUNT', N'TET2026', N'Giảm 50K Chào Đón Năm Mới');
INSERT INTO dbo.[vouchers] ([discount_value], [end_date], [is_active], [max_discount_amount], [min_order_value], [quantity], [start_date], [used_count], [created_at], [id], [store_id], [discount_type], [code], [name]) VALUES (15.00, '2027-01-07', 1, 150000.00, 300000.00, 150, '2026-09-22', 24, '2026-09-22 01:02:10.6', 8, 2, N'PERCENT', N'Q7PHUMYHUNG', N'Ưu Đãi 15% Hoa Tươi Khai Trương Phú Mỹ Hưng');
INSERT INTO dbo.[vouchers] ([discount_value], [end_date], [is_active], [max_discount_amount], [min_order_value], [quantity], [start_date], [used_count], [created_at], [id], [store_id], [discount_type], [code], [name]) VALUES (35000.00, '2026-12-07', 1, NULL, 250000.00, 100, '2026-09-27', 42, '2026-09-27 01:02:10.683', 9, 2, N'FREE_SHIP', N'Q7FREESHIP', N'Freeship Đơn Hoa Tươi Nội Thành Quận 7');
INSERT INTO dbo.[vouchers] ([discount_value], [end_date], [is_active], [max_discount_amount], [min_order_value], [quantity], [start_date], [used_count], [created_at], [id], [store_id], [discount_type], [code], [name]) VALUES (50000.00, '2026-12-07', 1, NULL, 500000.00, 80, '2026-10-02', 15, '2026-10-02 01:02:10.69', 10, 2, N'FIXED_AMOUNT', N'Q7TRIAN50K', N'Tri Ân Khách Hàng Thân Thiết Q7 - Giảm 50K');
INSERT INTO dbo.[vouchers] ([discount_value], [end_date], [is_active], [max_discount_amount], [min_order_value], [quantity], [start_date], [used_count], [created_at], [id], [store_id], [discount_type], [code], [name]) VALUES (100000.00, '2027-01-07', 1, NULL, 1000000.00, 50, '2026-10-05', 8, '2026-10-05 01:02:10.693', 11, 2, N'FIXED_AMOUNT', N'Q7VIP100K', N'Đặc Quyền Khách Hàng VIP Quận 7 - Giảm 100K');
INSERT INTO dbo.[vouchers] ([discount_value], [end_date], [is_active], [max_discount_amount], [min_order_value], [quantity], [start_date], [used_count], [created_at], [id], [store_id], [discount_type], [code], [name]) VALUES (10.00, '2026-12-22', 1, 100000.00, 200000.00, 100, '2026-09-12', 11, '2026-09-22 20:36:14.763', 1, NULL, N'PERCENT', N'FLOWER10', N'Giảm 10% Cho Mọi Đơn Hoa Tươi');
SET IDENTITY_INSERT dbo.[vouchers] OFF;
GO

EXEC sp_MSforeachtable 'ALTER TABLE ? WITH CHECK CHECK CONSTRAINT ALL';
GO

-- ============================================================================
-- KẾT THÚC IMPORT DỮ LIỆU SQL SERVER THÀNH CÔNG!
-- ============================================================================
