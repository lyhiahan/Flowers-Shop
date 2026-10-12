-- Additive upgrade for installations explicitly baselined at version 1.
-- Preserve existing columns and business data. Review duplicate-key errors manually; never auto-delete.

IF OBJECT_ID(N'dbo.activity_logs', N'U') IS NULL
BEGIN
    create table activity_logs (id bigint identity not null, action varchar(50) not null, target_id bigint, target_type varchar(50), timestamp datetime2(7), user_id bigint not null, primary key (id));
END;
GO
IF COL_LENGTH('dbo.activity_logs', 'action') IS NULL
    ALTER TABLE dbo.activity_logs ADD action varchar(50) NULL;
IF COL_LENGTH('dbo.activity_logs', 'target_id') IS NULL
    ALTER TABLE dbo.activity_logs ADD target_id bigint NULL;
IF COL_LENGTH('dbo.activity_logs', 'target_type') IS NULL
    ALTER TABLE dbo.activity_logs ADD target_type varchar(50) NULL;
IF COL_LENGTH('dbo.activity_logs', 'timestamp') IS NULL
    ALTER TABLE dbo.activity_logs ADD timestamp datetime2(7) NULL;
IF COL_LENGTH('dbo.activity_logs', 'user_id') IS NULL
    ALTER TABLE dbo.activity_logs ADD user_id bigint NULL;
GO

IF OBJECT_ID(N'dbo.cart_items', N'U') IS NULL
BEGIN
    create table cart_items (id bigint identity not null, delivery_date date, greeting_message nvarchar(500), order_type varchar(10) not null check ((order_type in ('BUY','RENT'))), quantity int not null, rent_end_date date, rent_start_date date, cart_id bigint not null, product_id bigint not null, store_id bigint not null, primary key (id));
END;
GO
IF COL_LENGTH('dbo.cart_items', 'delivery_date') IS NULL
    ALTER TABLE dbo.cart_items ADD delivery_date date NULL;
IF COL_LENGTH('dbo.cart_items', 'greeting_message') IS NULL
    ALTER TABLE dbo.cart_items ADD greeting_message nvarchar(500) NULL;
IF COL_LENGTH('dbo.cart_items', 'order_type') IS NULL
    ALTER TABLE dbo.cart_items ADD order_type varchar(10) NULL;
IF COL_LENGTH('dbo.cart_items', 'quantity') IS NULL
    ALTER TABLE dbo.cart_items ADD quantity int NULL;
IF COL_LENGTH('dbo.cart_items', 'rent_end_date') IS NULL
    ALTER TABLE dbo.cart_items ADD rent_end_date date NULL;
IF COL_LENGTH('dbo.cart_items', 'rent_start_date') IS NULL
    ALTER TABLE dbo.cart_items ADD rent_start_date date NULL;
IF COL_LENGTH('dbo.cart_items', 'cart_id') IS NULL
    ALTER TABLE dbo.cart_items ADD cart_id bigint NULL;
IF COL_LENGTH('dbo.cart_items', 'product_id') IS NULL
    ALTER TABLE dbo.cart_items ADD product_id bigint NULL;
IF COL_LENGTH('dbo.cart_items', 'store_id') IS NULL
    ALTER TABLE dbo.cart_items ADD store_id bigint NULL;
GO

IF OBJECT_ID(N'dbo.carts', N'U') IS NULL
BEGIN
    create table carts (id bigint identity not null, user_id bigint not null, primary key (id));
END;
GO
IF COL_LENGTH('dbo.carts', 'user_id') IS NULL
    ALTER TABLE dbo.carts ADD user_id bigint NULL;
GO

IF OBJECT_ID(N'dbo.categories', N'U') IS NULL
BEGIN
    create table categories (id bigint identity not null, description NVARCHAR(MAX), is_deleted bit, name NVARCHAR(100) not null, parent_id bigint, primary key (id));
END;
GO
IF COL_LENGTH('dbo.categories', 'description') IS NULL
    ALTER TABLE dbo.categories ADD description NVARCHAR(MAX) NULL;
IF COL_LENGTH('dbo.categories', 'is_deleted') IS NULL
    ALTER TABLE dbo.categories ADD is_deleted bit NOT NULL CONSTRAINT DF_upgrade_categories_is_deleted DEFAULT 0;
IF COL_LENGTH('dbo.categories', 'name') IS NULL
    ALTER TABLE dbo.categories ADD name NVARCHAR(100) NULL;
IF COL_LENGTH('dbo.categories', 'parent_id') IS NULL
    ALTER TABLE dbo.categories ADD parent_id bigint NULL;
GO

IF OBJECT_ID(N'dbo.chat_conversations', N'U') IS NULL
BEGIN
    create table chat_conversations (id bigint identity not null, conversation_key varchar(100) not null, created_at datetime2(7) not null, kind varchar(16) not null, last_message nvarchar(2000), updated_at datetime2(7) not null, customer_id bigint not null, store_id bigint, primary key (id));
END;
GO
IF COL_LENGTH('dbo.chat_conversations', 'conversation_key') IS NULL
    ALTER TABLE dbo.chat_conversations ADD conversation_key varchar(100) NULL;
IF COL_LENGTH('dbo.chat_conversations', 'created_at') IS NULL
    ALTER TABLE dbo.chat_conversations ADD created_at datetime2(7) NULL;
IF COL_LENGTH('dbo.chat_conversations', 'kind') IS NULL
    ALTER TABLE dbo.chat_conversations ADD kind varchar(16) NULL;
IF COL_LENGTH('dbo.chat_conversations', 'last_message') IS NULL
    ALTER TABLE dbo.chat_conversations ADD last_message nvarchar(2000) NULL;
IF COL_LENGTH('dbo.chat_conversations', 'updated_at') IS NULL
    ALTER TABLE dbo.chat_conversations ADD updated_at datetime2(7) NULL;
IF COL_LENGTH('dbo.chat_conversations', 'customer_id') IS NULL
    ALTER TABLE dbo.chat_conversations ADD customer_id bigint NULL;
IF COL_LENGTH('dbo.chat_conversations', 'store_id') IS NULL
    ALTER TABLE dbo.chat_conversations ADD store_id bigint NULL;
GO

IF OBJECT_ID(N'dbo.chat_messages', N'U') IS NULL
BEGIN
    create table chat_messages (id bigint identity not null, client_message_id varchar(36) not null, content nvarchar(2000) not null, created_at datetime2(7) not null, image_public_id varchar(255), image_url varchar(500), conversation_id bigint not null, sender_id bigint not null, primary key (id));
END;
GO
IF COL_LENGTH('dbo.chat_messages', 'client_message_id') IS NULL
    ALTER TABLE dbo.chat_messages ADD client_message_id varchar(36) NULL;
IF COL_LENGTH('dbo.chat_messages', 'content') IS NULL
    ALTER TABLE dbo.chat_messages ADD content nvarchar(2000) NULL;
IF COL_LENGTH('dbo.chat_messages', 'created_at') IS NULL
    ALTER TABLE dbo.chat_messages ADD created_at datetime2(7) NULL;
IF COL_LENGTH('dbo.chat_messages', 'image_public_id') IS NULL
    ALTER TABLE dbo.chat_messages ADD image_public_id varchar(255) NULL;
IF COL_LENGTH('dbo.chat_messages', 'image_url') IS NULL
    ALTER TABLE dbo.chat_messages ADD image_url varchar(500) NULL;
IF COL_LENGTH('dbo.chat_messages', 'conversation_id') IS NULL
    ALTER TABLE dbo.chat_messages ADD conversation_id bigint NULL;
IF COL_LENGTH('dbo.chat_messages', 'sender_id') IS NULL
    ALTER TABLE dbo.chat_messages ADD sender_id bigint NULL;
GO

IF OBJECT_ID(N'dbo.commissions', N'U') IS NULL
BEGIN
    create table commissions (id bigint identity not null, cost numeric(38,2) not null, created_at datetime2(7), description nvarchar(3000), is_deleted bit, name nvarchar(32) not null, updated_at datetime2(7), primary key (id));
END;
GO
IF COL_LENGTH('dbo.commissions', 'cost') IS NULL
    ALTER TABLE dbo.commissions ADD cost numeric(38,2) NULL;
IF COL_LENGTH('dbo.commissions', 'created_at') IS NULL
    ALTER TABLE dbo.commissions ADD created_at datetime2(7) NULL;
IF COL_LENGTH('dbo.commissions', 'description') IS NULL
    ALTER TABLE dbo.commissions ADD description nvarchar(3000) NULL;
IF COL_LENGTH('dbo.commissions', 'is_deleted') IS NULL
    ALTER TABLE dbo.commissions ADD is_deleted bit NOT NULL CONSTRAINT DF_upgrade_commissions_is_deleted DEFAULT 0;
IF COL_LENGTH('dbo.commissions', 'name') IS NULL
    ALTER TABLE dbo.commissions ADD name nvarchar(32) NULL;
IF COL_LENGTH('dbo.commissions', 'updated_at') IS NULL
    ALTER TABLE dbo.commissions ADD updated_at datetime2(7) NULL;
GO

IF OBJECT_ID(N'dbo.deliveries', N'U') IS NULL
BEGIN
    create table deliveries (id bigint identity not null, coverage_area nvarchar(255), created_at datetime2(7), description nvarchar(1000), is_active bit not null, is_deleted bit, name nvarchar(100) not null, phone varchar(50), price numeric(38,2) not null, updated_at datetime2(7), vehicle_type nvarchar(100), primary key (id));
END;
GO
IF COL_LENGTH('dbo.deliveries', 'coverage_area') IS NULL
    ALTER TABLE dbo.deliveries ADD coverage_area nvarchar(255) NULL;
IF COL_LENGTH('dbo.deliveries', 'created_at') IS NULL
    ALTER TABLE dbo.deliveries ADD created_at datetime2(7) NULL;
IF COL_LENGTH('dbo.deliveries', 'description') IS NULL
    ALTER TABLE dbo.deliveries ADD description nvarchar(1000) NULL;
IF COL_LENGTH('dbo.deliveries', 'is_active') IS NULL
    ALTER TABLE dbo.deliveries ADD is_active bit NULL;
IF COL_LENGTH('dbo.deliveries', 'is_deleted') IS NULL
    ALTER TABLE dbo.deliveries ADD is_deleted bit NOT NULL CONSTRAINT DF_upgrade_deliveries_is_deleted DEFAULT 0;
IF COL_LENGTH('dbo.deliveries', 'name') IS NULL
    ALTER TABLE dbo.deliveries ADD name nvarchar(100) NULL;
IF COL_LENGTH('dbo.deliveries', 'phone') IS NULL
    ALTER TABLE dbo.deliveries ADD phone varchar(50) NULL;
IF COL_LENGTH('dbo.deliveries', 'price') IS NULL
    ALTER TABLE dbo.deliveries ADD price numeric(38,2) NULL;
IF COL_LENGTH('dbo.deliveries', 'updated_at') IS NULL
    ALTER TABLE dbo.deliveries ADD updated_at datetime2(7) NULL;
IF COL_LENGTH('dbo.deliveries', 'vehicle_type') IS NULL
    ALTER TABLE dbo.deliveries ADD vehicle_type nvarchar(100) NULL;
GO

IF OBJECT_ID(N'dbo.inventory_transfer_requests', N'U') IS NULL
BEGIN
    create table inventory_transfer_requests (id bigint identity not null, created_at datetime2(7), quantity int not null, status varchar(20) not null check ((status in ('PENDING','APPROVED','REJECTED','COMPLETED'))), approved_by bigint, from_store_id bigint not null, product_id bigint not null, requested_by bigint, to_store_id bigint not null, primary key (id));
END;
GO
IF COL_LENGTH('dbo.inventory_transfer_requests', 'created_at') IS NULL
    ALTER TABLE dbo.inventory_transfer_requests ADD created_at datetime2(7) NULL;
IF COL_LENGTH('dbo.inventory_transfer_requests', 'quantity') IS NULL
    ALTER TABLE dbo.inventory_transfer_requests ADD quantity int NULL;
IF COL_LENGTH('dbo.inventory_transfer_requests', 'status') IS NULL
    ALTER TABLE dbo.inventory_transfer_requests ADD status varchar(20) NULL;
IF COL_LENGTH('dbo.inventory_transfer_requests', 'approved_by') IS NULL
    ALTER TABLE dbo.inventory_transfer_requests ADD approved_by bigint NULL;
IF COL_LENGTH('dbo.inventory_transfer_requests', 'from_store_id') IS NULL
    ALTER TABLE dbo.inventory_transfer_requests ADD from_store_id bigint NULL;
IF COL_LENGTH('dbo.inventory_transfer_requests', 'product_id') IS NULL
    ALTER TABLE dbo.inventory_transfer_requests ADD product_id bigint NULL;
IF COL_LENGTH('dbo.inventory_transfer_requests', 'requested_by') IS NULL
    ALTER TABLE dbo.inventory_transfer_requests ADD requested_by bigint NULL;
IF COL_LENGTH('dbo.inventory_transfer_requests', 'to_store_id') IS NULL
    ALTER TABLE dbo.inventory_transfer_requests ADD to_store_id bigint NULL;
GO

IF OBJECT_ID(N'dbo.jwt_refresh_tokens', N'U') IS NULL
BEGIN
    create table jwt_refresh_tokens (token_hash varchar(64) not null, consumed bit not null, expires_at datetimeoffset(7) not null, session_id varchar(36) not null, primary key (token_hash));
END;
GO
IF COL_LENGTH('dbo.jwt_refresh_tokens', 'token_hash') IS NULL
    ALTER TABLE dbo.jwt_refresh_tokens ADD token_hash varchar(64) NULL;
IF COL_LENGTH('dbo.jwt_refresh_tokens', 'consumed') IS NULL
    ALTER TABLE dbo.jwt_refresh_tokens ADD consumed bit NOT NULL CONSTRAINT DF_upgrade_jwt_refresh_tokens_consumed DEFAULT 0;
IF COL_LENGTH('dbo.jwt_refresh_tokens', 'expires_at') IS NULL
    ALTER TABLE dbo.jwt_refresh_tokens ADD expires_at datetimeoffset(7) NULL;
IF COL_LENGTH('dbo.jwt_refresh_tokens', 'session_id') IS NULL
    ALTER TABLE dbo.jwt_refresh_tokens ADD session_id varchar(36) NULL;
GO

IF OBJECT_ID(N'dbo.jwt_sessions', N'U') IS NULL
BEGIN
    create table jwt_sessions (id varchar(36) not null, credential_hash varchar(64) not null, expires_at datetimeoffset(7) not null, revoked bit not null, user_id bigint not null, primary key (id));
END;
GO
IF COL_LENGTH('dbo.jwt_sessions', 'credential_hash') IS NULL
    ALTER TABLE dbo.jwt_sessions ADD credential_hash varchar(64) NULL;
IF COL_LENGTH('dbo.jwt_sessions', 'expires_at') IS NULL
    ALTER TABLE dbo.jwt_sessions ADD expires_at datetimeoffset(7) NULL;
IF COL_LENGTH('dbo.jwt_sessions', 'revoked') IS NULL
    ALTER TABLE dbo.jwt_sessions ADD revoked bit NOT NULL CONSTRAINT DF_upgrade_jwt_sessions_revoked DEFAULT 0;
IF COL_LENGTH('dbo.jwt_sessions', 'user_id') IS NULL
    ALTER TABLE dbo.jwt_sessions ADD user_id bigint NULL;
GO

IF OBJECT_ID(N'dbo.momo_payment_attempts', N'U') IS NULL
BEGIN
    create table momo_payment_attempts (id bigint identity not null, amount numeric(38,2) not null, completed bit not null, created_at datetime2(7) not null, environment varchar(16) not null, extra_data varchar(1000) not null, gateway_order_id varchar(100) not null, order_info nvarchar(255) not null, request_id varchar(100) not null, transaction_id varchar(100), order_id bigint not null, primary key (id));
END;
GO
IF COL_LENGTH('dbo.momo_payment_attempts', 'amount') IS NULL
    ALTER TABLE dbo.momo_payment_attempts ADD amount numeric(38,2) NULL;
IF COL_LENGTH('dbo.momo_payment_attempts', 'completed') IS NULL
    ALTER TABLE dbo.momo_payment_attempts ADD completed bit NULL;
IF COL_LENGTH('dbo.momo_payment_attempts', 'created_at') IS NULL
    ALTER TABLE dbo.momo_payment_attempts ADD created_at datetime2(7) NULL;
IF COL_LENGTH('dbo.momo_payment_attempts', 'environment') IS NULL
    ALTER TABLE dbo.momo_payment_attempts ADD environment varchar(16) NULL;
IF COL_LENGTH('dbo.momo_payment_attempts', 'extra_data') IS NULL
    ALTER TABLE dbo.momo_payment_attempts ADD extra_data varchar(1000) NULL;
IF COL_LENGTH('dbo.momo_payment_attempts', 'gateway_order_id') IS NULL
    ALTER TABLE dbo.momo_payment_attempts ADD gateway_order_id varchar(100) NULL;
IF COL_LENGTH('dbo.momo_payment_attempts', 'order_info') IS NULL
    ALTER TABLE dbo.momo_payment_attempts ADD order_info nvarchar(255) NULL;
IF COL_LENGTH('dbo.momo_payment_attempts', 'request_id') IS NULL
    ALTER TABLE dbo.momo_payment_attempts ADD request_id varchar(100) NULL;
IF COL_LENGTH('dbo.momo_payment_attempts', 'transaction_id') IS NULL
    ALTER TABLE dbo.momo_payment_attempts ADD transaction_id varchar(100) NULL;
IF COL_LENGTH('dbo.momo_payment_attempts', 'order_id') IS NULL
    ALTER TABLE dbo.momo_payment_attempts ADD order_id bigint NULL;
GO

IF OBJECT_ID(N'dbo.notifications', N'U') IS NULL
BEGIN
    create table notifications (id bigint identity not null, content NVARCHAR(MAX) not null, created_at datetime2(7), is_read bit, related_order_id bigint, title nvarchar(200) not null, type varchar(50), user_id bigint not null, primary key (id));
END;
GO
IF COL_LENGTH('dbo.notifications', 'content') IS NULL
    ALTER TABLE dbo.notifications ADD content NVARCHAR(MAX) NULL;
IF COL_LENGTH('dbo.notifications', 'created_at') IS NULL
    ALTER TABLE dbo.notifications ADD created_at datetime2(7) NULL;
IF COL_LENGTH('dbo.notifications', 'is_read') IS NULL
    ALTER TABLE dbo.notifications ADD is_read bit NULL;
IF COL_LENGTH('dbo.notifications', 'related_order_id') IS NULL
    ALTER TABLE dbo.notifications ADD related_order_id bigint NULL;
IF COL_LENGTH('dbo.notifications', 'title') IS NULL
    ALTER TABLE dbo.notifications ADD title nvarchar(200) NULL;
IF COL_LENGTH('dbo.notifications', 'type') IS NULL
    ALTER TABLE dbo.notifications ADD type varchar(50) NULL;
IF COL_LENGTH('dbo.notifications', 'user_id') IS NULL
    ALTER TABLE dbo.notifications ADD user_id bigint NULL;
GO

IF OBJECT_ID(N'dbo.order_details', N'U') IS NULL
BEGIN
    create table order_details (id bigint identity not null, actual_return_date date, deposit_fee numeric(38,2), order_type varchar(10) check ((order_type in ('BUY','RENT'))), quantity int not null, rent_end_date date, rent_start_date date, unit_price numeric(38,2) not null, order_id bigint not null, product_id bigint not null, primary key (id));
END;
GO
IF COL_LENGTH('dbo.order_details', 'actual_return_date') IS NULL
    ALTER TABLE dbo.order_details ADD actual_return_date date NULL;
IF COL_LENGTH('dbo.order_details', 'deposit_fee') IS NULL
    ALTER TABLE dbo.order_details ADD deposit_fee numeric(38,2) NULL;
IF COL_LENGTH('dbo.order_details', 'order_type') IS NULL
    ALTER TABLE dbo.order_details ADD order_type varchar(10) NULL;
IF COL_LENGTH('dbo.order_details', 'quantity') IS NULL
    ALTER TABLE dbo.order_details ADD quantity int NULL;
IF COL_LENGTH('dbo.order_details', 'rent_end_date') IS NULL
    ALTER TABLE dbo.order_details ADD rent_end_date date NULL;
IF COL_LENGTH('dbo.order_details', 'rent_start_date') IS NULL
    ALTER TABLE dbo.order_details ADD rent_start_date date NULL;
IF COL_LENGTH('dbo.order_details', 'unit_price') IS NULL
    ALTER TABLE dbo.order_details ADD unit_price numeric(38,2) NULL;
IF COL_LENGTH('dbo.order_details', 'order_id') IS NULL
    ALTER TABLE dbo.order_details ADD order_id bigint NULL;
IF COL_LENGTH('dbo.order_details', 'product_id') IS NULL
    ALTER TABLE dbo.order_details ADD product_id bigint NULL;
GO

IF OBJECT_ID(N'dbo.orders', N'U') IS NULL
BEGIN
    create table orders (id bigint identity not null, cancellation_reason nvarchar(500), created_at datetime2(7), delivery_date date, delivery_time time, delivery_unit_id bigint, discount_amount numeric(38,2), fine_amount numeric(38,2), greeting_message nvarchar(500), order_type varchar(10) not null check ((order_type in ('BUY','RENT'))), payment_expired bit, payment_expires_at datetime2(7), payment_method varchar(20), payment_status varchar(20), recipient_address nvarchar(255), recipient_name nvarchar(100), recipient_phone varchar(20), shipping_fee numeric(18,2), shipping_method varchar(20), status varchar(20) not null check ((status in ('PENDING','CONFIRMED','PREPARING','PICKED_UP','DELIVERING','DELIVERED','COMPLETED','CANCELLED','REFUND_REQUESTED','REFUNDED','RENTING','RETURNED','OVERDUE'))), total_amount numeric(38,2), total_deposit numeric(38,2), version bigint not null, shipper_id bigint, pickup_store_id bigint not null, return_store_id bigint, user_id bigint not null, voucher_id bigint, primary key (id));
END;
GO
IF COL_LENGTH('dbo.orders', 'cancellation_reason') IS NULL
    ALTER TABLE dbo.orders ADD cancellation_reason nvarchar(500) NULL;
IF COL_LENGTH('dbo.orders', 'created_at') IS NULL
    ALTER TABLE dbo.orders ADD created_at datetime2(7) NULL;
IF COL_LENGTH('dbo.orders', 'delivery_date') IS NULL
    ALTER TABLE dbo.orders ADD delivery_date date NULL;
IF COL_LENGTH('dbo.orders', 'delivery_time') IS NULL
    ALTER TABLE dbo.orders ADD delivery_time time NULL;
IF COL_LENGTH('dbo.orders', 'delivery_unit_id') IS NULL
    ALTER TABLE dbo.orders ADD delivery_unit_id bigint NULL;
IF COL_LENGTH('dbo.orders', 'discount_amount') IS NULL
    ALTER TABLE dbo.orders ADD discount_amount numeric(38,2) NULL;
IF COL_LENGTH('dbo.orders', 'fine_amount') IS NULL
    ALTER TABLE dbo.orders ADD fine_amount numeric(38,2) NULL;
IF COL_LENGTH('dbo.orders', 'greeting_message') IS NULL
    ALTER TABLE dbo.orders ADD greeting_message nvarchar(500) NULL;
IF COL_LENGTH('dbo.orders', 'order_type') IS NULL
    ALTER TABLE dbo.orders ADD order_type varchar(10) NULL;
IF COL_LENGTH('dbo.orders', 'payment_expired') IS NULL
    ALTER TABLE dbo.orders ADD payment_expired bit NOT NULL CONSTRAINT DF_upgrade_orders_payment_expired DEFAULT 0;
IF COL_LENGTH('dbo.orders', 'payment_expires_at') IS NULL
    ALTER TABLE dbo.orders ADD payment_expires_at datetime2(7) NULL;
IF COL_LENGTH('dbo.orders', 'payment_method') IS NULL
    ALTER TABLE dbo.orders ADD payment_method varchar(20) NULL;
IF COL_LENGTH('dbo.orders', 'payment_status') IS NULL
    ALTER TABLE dbo.orders ADD payment_status varchar(20) NULL;
IF COL_LENGTH('dbo.orders', 'recipient_address') IS NULL
    ALTER TABLE dbo.orders ADD recipient_address nvarchar(255) NULL;
IF COL_LENGTH('dbo.orders', 'recipient_name') IS NULL
    ALTER TABLE dbo.orders ADD recipient_name nvarchar(100) NULL;
IF COL_LENGTH('dbo.orders', 'recipient_phone') IS NULL
    ALTER TABLE dbo.orders ADD recipient_phone varchar(20) NULL;
IF COL_LENGTH('dbo.orders', 'shipping_fee') IS NULL
    ALTER TABLE dbo.orders ADD shipping_fee numeric(18,2) NULL;
IF COL_LENGTH('dbo.orders', 'shipping_method') IS NULL
    ALTER TABLE dbo.orders ADD shipping_method varchar(20) NULL;
IF COL_LENGTH('dbo.orders', 'status') IS NULL
    ALTER TABLE dbo.orders ADD status varchar(20) NULL;
IF COL_LENGTH('dbo.orders', 'total_amount') IS NULL
    ALTER TABLE dbo.orders ADD total_amount numeric(38,2) NULL;
IF COL_LENGTH('dbo.orders', 'total_deposit') IS NULL
    ALTER TABLE dbo.orders ADD total_deposit numeric(38,2) NULL;
IF COL_LENGTH('dbo.orders', 'version') IS NULL
    ALTER TABLE dbo.orders ADD version bigint NOT NULL CONSTRAINT DF_upgrade_orders_version DEFAULT 0;
IF COL_LENGTH('dbo.orders', 'shipper_id') IS NULL
    ALTER TABLE dbo.orders ADD shipper_id bigint NULL;
IF COL_LENGTH('dbo.orders', 'pickup_store_id') IS NULL
    ALTER TABLE dbo.orders ADD pickup_store_id bigint NULL;
IF COL_LENGTH('dbo.orders', 'return_store_id') IS NULL
    ALTER TABLE dbo.orders ADD return_store_id bigint NULL;
IF COL_LENGTH('dbo.orders', 'user_id') IS NULL
    ALTER TABLE dbo.orders ADD user_id bigint NULL;
IF COL_LENGTH('dbo.orders', 'voucher_id') IS NULL
    ALTER TABLE dbo.orders ADD voucher_id bigint NULL;
GO

IF OBJECT_ID(N'dbo.otp_verifications', N'U') IS NULL
BEGIN
    create table otp_verifications (id bigint identity not null, code varchar(6) not null, created_at datetime2(7), expired_at datetime2(7) not null, failed_attempts int not null, is_used bit, type varchar(30) not null check ((type in ('ACTIVATE_ACCOUNT','RESET_PASSWORD'))), user_id bigint not null, primary key (id));
END;
GO
IF COL_LENGTH('dbo.otp_verifications', 'code') IS NULL
    ALTER TABLE dbo.otp_verifications ADD code varchar(6) NULL;
IF COL_LENGTH('dbo.otp_verifications', 'created_at') IS NULL
    ALTER TABLE dbo.otp_verifications ADD created_at datetime2(7) NULL;
IF COL_LENGTH('dbo.otp_verifications', 'expired_at') IS NULL
    ALTER TABLE dbo.otp_verifications ADD expired_at datetime2(7) NULL;
IF COL_LENGTH('dbo.otp_verifications', 'failed_attempts') IS NULL
    ALTER TABLE dbo.otp_verifications ADD failed_attempts int NOT NULL CONSTRAINT DF_upgrade_otp_verifications_failed_attempts DEFAULT 0;
IF COL_LENGTH('dbo.otp_verifications', 'is_used') IS NULL
    ALTER TABLE dbo.otp_verifications ADD is_used bit NULL;
IF COL_LENGTH('dbo.otp_verifications', 'type') IS NULL
    ALTER TABLE dbo.otp_verifications ADD type varchar(30) NULL;
IF COL_LENGTH('dbo.otp_verifications', 'user_id') IS NULL
    ALTER TABLE dbo.otp_verifications ADD user_id bigint NULL;
GO

IF OBJECT_ID(N'dbo.payments', N'U') IS NULL
BEGIN
    create table payments (id bigint identity not null, amount numeric(38,2) not null, method varchar(20) not null check ((method in ('COD','VNPAY','MOMO','CASH','BANK_TRANSFER'))), momo_environment varchar(16), paid_at datetime2(7), refund_method varchar(255), refund_note varchar(255), refunded_at datetime2(7), status varchar(20) not null check ((status in ('PENDING','SUCCESS','FAILED','REFUNDED'))), transaction_code varchar(255), order_id bigint not null, primary key (id));
END;
GO
IF COL_LENGTH('dbo.payments', 'amount') IS NULL
    ALTER TABLE dbo.payments ADD amount numeric(38,2) NULL;
IF COL_LENGTH('dbo.payments', 'method') IS NULL
    ALTER TABLE dbo.payments ADD method varchar(20) NULL;
IF COL_LENGTH('dbo.payments', 'momo_environment') IS NULL
    ALTER TABLE dbo.payments ADD momo_environment varchar(16) NULL;
IF COL_LENGTH('dbo.payments', 'paid_at') IS NULL
    ALTER TABLE dbo.payments ADD paid_at datetime2(7) NULL;
IF COL_LENGTH('dbo.payments', 'refund_method') IS NULL
    ALTER TABLE dbo.payments ADD refund_method varchar(255) NULL;
IF COL_LENGTH('dbo.payments', 'refund_note') IS NULL
    ALTER TABLE dbo.payments ADD refund_note varchar(255) NULL;
IF COL_LENGTH('dbo.payments', 'refunded_at') IS NULL
    ALTER TABLE dbo.payments ADD refunded_at datetime2(7) NULL;
IF COL_LENGTH('dbo.payments', 'status') IS NULL
    ALTER TABLE dbo.payments ADD status varchar(20) NULL;
IF COL_LENGTH('dbo.payments', 'transaction_code') IS NULL
    ALTER TABLE dbo.payments ADD transaction_code varchar(255) NULL;
IF COL_LENGTH('dbo.payments', 'order_id') IS NULL
    ALTER TABLE dbo.payments ADD order_id bigint NULL;
GO

IF OBJECT_ID(N'dbo.product_images', N'U') IS NULL
BEGIN
    create table product_images (id bigint identity not null, cloudinary_public_id varchar(255), image_url varchar(255) not null, is_main bit, product_id bigint not null, primary key (id));
END;
GO
IF COL_LENGTH('dbo.product_images', 'cloudinary_public_id') IS NULL
    ALTER TABLE dbo.product_images ADD cloudinary_public_id varchar(255) NULL;
IF COL_LENGTH('dbo.product_images', 'image_url') IS NULL
    ALTER TABLE dbo.product_images ADD image_url varchar(255) NULL;
IF COL_LENGTH('dbo.product_images', 'is_main') IS NULL
    ALTER TABLE dbo.product_images ADD is_main bit NULL;
IF COL_LENGTH('dbo.product_images', 'product_id') IS NULL
    ALTER TABLE dbo.product_images ADD product_id bigint NULL;
GO

IF OBJECT_ID(N'dbo.product_style_values', N'U') IS NULL
BEGIN
    create table product_style_values (product_id bigint not null, style_value_id bigint not null, primary key (product_id, style_value_id));
END;
GO
IF COL_LENGTH('dbo.product_style_values', 'product_id') IS NULL
    ALTER TABLE dbo.product_style_values ADD product_id bigint NULL;
IF COL_LENGTH('dbo.product_style_values', 'style_value_id') IS NULL
    ALTER TABLE dbo.product_style_values ADD style_value_id bigint NULL;
GO

IF OBJECT_ID(N'dbo.product_wishlists', N'U') IS NULL
BEGIN
    create table product_wishlists (id bigint identity not null, created_at datetime2(7), product_id bigint not null, user_id bigint not null, primary key (id));
END;
GO
IF COL_LENGTH('dbo.product_wishlists', 'created_at') IS NULL
    ALTER TABLE dbo.product_wishlists ADD created_at datetime2(7) NULL;
IF COL_LENGTH('dbo.product_wishlists', 'product_id') IS NULL
    ALTER TABLE dbo.product_wishlists ADD product_id bigint NULL;
IF COL_LENGTH('dbo.product_wishlists', 'user_id') IS NULL
    ALTER TABLE dbo.product_wishlists ADD user_id bigint NULL;
GO

IF OBJECT_ID(N'dbo.products', N'U') IS NULL
BEGIN
    create table products (id bigint identity not null, created_at datetime2(7), deposit_fee numeric(38,2) not null, description NVARCHAR(MAX), image_url varchar(500), import_price numeric(38,2) not null, is_active bit not null, is_deleted bit, is_selling bit not null, name nvarchar(200) not null, price_buy numeric(38,2) not null, price_rent_per_day numeric(38,2) not null, promotional_price numeric(38,2), rating float(53), sold int, view_count int, category_id bigint not null, store_id bigint, primary key (id));
END;
GO
IF COL_LENGTH('dbo.products', 'created_at') IS NULL
    ALTER TABLE dbo.products ADD created_at datetime2(7) NULL;
IF COL_LENGTH('dbo.products', 'deposit_fee') IS NULL
    ALTER TABLE dbo.products ADD deposit_fee numeric(38,2) NULL;
IF COL_LENGTH('dbo.products', 'description') IS NULL
    ALTER TABLE dbo.products ADD description NVARCHAR(MAX) NULL;
IF COL_LENGTH('dbo.products', 'image_url') IS NULL
    ALTER TABLE dbo.products ADD image_url varchar(500) NULL;
IF COL_LENGTH('dbo.products', 'import_price') IS NULL
    ALTER TABLE dbo.products ADD import_price numeric(38,2) NULL;
IF COL_LENGTH('dbo.products', 'is_active') IS NULL
    ALTER TABLE dbo.products ADD is_active bit NULL;
IF COL_LENGTH('dbo.products', 'is_deleted') IS NULL
    ALTER TABLE dbo.products ADD is_deleted bit NOT NULL CONSTRAINT DF_upgrade_products_is_deleted DEFAULT 0;
IF COL_LENGTH('dbo.products', 'is_selling') IS NULL
    ALTER TABLE dbo.products ADD is_selling bit NULL;
IF COL_LENGTH('dbo.products', 'name') IS NULL
    ALTER TABLE dbo.products ADD name nvarchar(200) NULL;
IF COL_LENGTH('dbo.products', 'price_buy') IS NULL
    ALTER TABLE dbo.products ADD price_buy numeric(38,2) NULL;
IF COL_LENGTH('dbo.products', 'price_rent_per_day') IS NULL
    ALTER TABLE dbo.products ADD price_rent_per_day numeric(38,2) NULL;
IF COL_LENGTH('dbo.products', 'promotional_price') IS NULL
    ALTER TABLE dbo.products ADD promotional_price numeric(38,2) NULL;
IF COL_LENGTH('dbo.products', 'rating') IS NULL
    ALTER TABLE dbo.products ADD rating float(53) NULL;
IF COL_LENGTH('dbo.products', 'sold') IS NULL
    ALTER TABLE dbo.products ADD sold int NULL;
IF COL_LENGTH('dbo.products', 'view_count') IS NULL
    ALTER TABLE dbo.products ADD view_count int NULL;
IF COL_LENGTH('dbo.products', 'category_id') IS NULL
    ALTER TABLE dbo.products ADD category_id bigint NULL;
IF COL_LENGTH('dbo.products', 'store_id') IS NULL
    ALTER TABLE dbo.products ADD store_id bigint NULL;
GO

IF OBJECT_ID(N'dbo.promotions', N'U') IS NULL
BEGIN
    create table promotions (id bigint identity not null, apply_type varchar(10) check ((apply_type in ('BUY','RENT'))), discount_percentage int not null, end_date date, name NVARCHAR(150) not null, start_date date, store_id bigint, primary key (id));
END;
GO
IF COL_LENGTH('dbo.promotions', 'apply_type') IS NULL
    ALTER TABLE dbo.promotions ADD apply_type varchar(10) NULL;
IF COL_LENGTH('dbo.promotions', 'discount_percentage') IS NULL
    ALTER TABLE dbo.promotions ADD discount_percentage int NULL;
IF COL_LENGTH('dbo.promotions', 'end_date') IS NULL
    ALTER TABLE dbo.promotions ADD end_date date NULL;
IF COL_LENGTH('dbo.promotions', 'name') IS NULL
    ALTER TABLE dbo.promotions ADD name NVARCHAR(150) NULL;
IF COL_LENGTH('dbo.promotions', 'start_date') IS NULL
    ALTER TABLE dbo.promotions ADD start_date date NULL;
IF COL_LENGTH('dbo.promotions', 'store_id') IS NULL
    ALTER TABLE dbo.promotions ADD store_id bigint NULL;
GO

IF OBJECT_ID(N'dbo.recently_viewed', N'U') IS NULL
BEGIN
    create table recently_viewed (id bigint identity not null, viewed_at datetime2(7), product_id bigint not null, user_id bigint not null, primary key (id));
END;
GO
IF COL_LENGTH('dbo.recently_viewed', 'viewed_at') IS NULL
    ALTER TABLE dbo.recently_viewed ADD viewed_at datetime2(7) NULL;
IF COL_LENGTH('dbo.recently_viewed', 'product_id') IS NULL
    ALTER TABLE dbo.recently_viewed ADD product_id bigint NULL;
IF COL_LENGTH('dbo.recently_viewed', 'user_id') IS NULL
    ALTER TABLE dbo.recently_viewed ADD user_id bigint NULL;
GO

IF OBJECT_ID(N'dbo.reviews', N'U') IS NULL
BEGIN
    create table reviews (id bigint identity not null, content nvarchar(1000) not null, created_at datetime2(7), image_url varchar(500), rating int not null, updated_at datetime2(7), order_id bigint, product_id bigint not null, store_id bigint, user_id bigint not null, primary key (id));
END;
GO
IF COL_LENGTH('dbo.reviews', 'content') IS NULL
    ALTER TABLE dbo.reviews ADD content nvarchar(1000) NULL;
IF COL_LENGTH('dbo.reviews', 'created_at') IS NULL
    ALTER TABLE dbo.reviews ADD created_at datetime2(7) NULL;
IF COL_LENGTH('dbo.reviews', 'image_url') IS NULL
    ALTER TABLE dbo.reviews ADD image_url varchar(500) NULL;
IF COL_LENGTH('dbo.reviews', 'rating') IS NULL
    ALTER TABLE dbo.reviews ADD rating int NULL;
IF COL_LENGTH('dbo.reviews', 'updated_at') IS NULL
    ALTER TABLE dbo.reviews ADD updated_at datetime2(7) NULL;
IF COL_LENGTH('dbo.reviews', 'order_id') IS NULL
    ALTER TABLE dbo.reviews ADD order_id bigint NULL;
IF COL_LENGTH('dbo.reviews', 'product_id') IS NULL
    ALTER TABLE dbo.reviews ADD product_id bigint NULL;
IF COL_LENGTH('dbo.reviews', 'store_id') IS NULL
    ALTER TABLE dbo.reviews ADD store_id bigint NULL;
IF COL_LENGTH('dbo.reviews', 'user_id') IS NULL
    ALTER TABLE dbo.reviews ADD user_id bigint NULL;
GO

IF OBJECT_ID(N'dbo.role_audit_logs', N'U') IS NULL
BEGIN
    create table role_audit_logs (id bigint identity not null, action varchar(20) not null, created_at datetime2(7) not null, reason varchar(500), role varchar(30) not null check ((role in ('MANAGER','ADMIN','USER'))), performed_by_id bigint not null, store_id bigint, user_id bigint not null, primary key (id));
END;
GO
IF COL_LENGTH('dbo.role_audit_logs', 'action') IS NULL
    ALTER TABLE dbo.role_audit_logs ADD action varchar(20) NULL;
IF COL_LENGTH('dbo.role_audit_logs', 'created_at') IS NULL
    ALTER TABLE dbo.role_audit_logs ADD created_at datetime2(7) NULL;
IF COL_LENGTH('dbo.role_audit_logs', 'reason') IS NULL
    ALTER TABLE dbo.role_audit_logs ADD reason varchar(500) NULL;
IF COL_LENGTH('dbo.role_audit_logs', 'role') IS NULL
    ALTER TABLE dbo.role_audit_logs ADD role varchar(30) NULL;
IF COL_LENGTH('dbo.role_audit_logs', 'performed_by_id') IS NULL
    ALTER TABLE dbo.role_audit_logs ADD performed_by_id bigint NULL;
IF COL_LENGTH('dbo.role_audit_logs', 'store_id') IS NULL
    ALTER TABLE dbo.role_audit_logs ADD store_id bigint NULL;
IF COL_LENGTH('dbo.role_audit_logs', 'user_id') IS NULL
    ALTER TABLE dbo.role_audit_logs ADD user_id bigint NULL;
GO

IF OBJECT_ID(N'dbo.role_requests', N'U') IS NULL
BEGIN
    create table role_requests (id bigint identity not null, contact_info nvarchar(255), created_at datetime2(7) not null, experience_note nvarchar(1000), phone varchar(20), rejection_reason nvarchar(500), reviewed_at datetime2(7), shop_address nvarchar(500), shop_name nvarchar(255), status varchar(30) not null check ((status in ('PENDING','APPROVED','REJECTED','CANCELLED'))), target_role varchar(30) not null, vehicle_type nvarchar(100), work_area nvarchar(255), assigned_store_id bigint, reviewed_by_id bigint, target_store_id bigint, user_id bigint not null, primary key (id));
END;
GO
IF COL_LENGTH('dbo.role_requests', 'contact_info') IS NULL
    ALTER TABLE dbo.role_requests ADD contact_info nvarchar(255) NULL;
IF COL_LENGTH('dbo.role_requests', 'created_at') IS NULL
    ALTER TABLE dbo.role_requests ADD created_at datetime2(7) NULL;
IF COL_LENGTH('dbo.role_requests', 'experience_note') IS NULL
    ALTER TABLE dbo.role_requests ADD experience_note nvarchar(1000) NULL;
IF COL_LENGTH('dbo.role_requests', 'phone') IS NULL
    ALTER TABLE dbo.role_requests ADD phone varchar(20) NULL;
IF COL_LENGTH('dbo.role_requests', 'rejection_reason') IS NULL
    ALTER TABLE dbo.role_requests ADD rejection_reason nvarchar(500) NULL;
IF COL_LENGTH('dbo.role_requests', 'reviewed_at') IS NULL
    ALTER TABLE dbo.role_requests ADD reviewed_at datetime2(7) NULL;
IF COL_LENGTH('dbo.role_requests', 'shop_address') IS NULL
    ALTER TABLE dbo.role_requests ADD shop_address nvarchar(500) NULL;
IF COL_LENGTH('dbo.role_requests', 'shop_name') IS NULL
    ALTER TABLE dbo.role_requests ADD shop_name nvarchar(255) NULL;
IF COL_LENGTH('dbo.role_requests', 'status') IS NULL
    ALTER TABLE dbo.role_requests ADD status varchar(30) NULL;
IF COL_LENGTH('dbo.role_requests', 'target_role') IS NULL
    ALTER TABLE dbo.role_requests ADD target_role varchar(30) NULL;
IF COL_LENGTH('dbo.role_requests', 'vehicle_type') IS NULL
    ALTER TABLE dbo.role_requests ADD vehicle_type nvarchar(100) NULL;
IF COL_LENGTH('dbo.role_requests', 'work_area') IS NULL
    ALTER TABLE dbo.role_requests ADD work_area nvarchar(255) NULL;
IF COL_LENGTH('dbo.role_requests', 'assigned_store_id') IS NULL
    ALTER TABLE dbo.role_requests ADD assigned_store_id bigint NULL;
IF COL_LENGTH('dbo.role_requests', 'reviewed_by_id') IS NULL
    ALTER TABLE dbo.role_requests ADD reviewed_by_id bigint NULL;
IF COL_LENGTH('dbo.role_requests', 'target_store_id') IS NULL
    ALTER TABLE dbo.role_requests ADD target_store_id bigint NULL;
IF COL_LENGTH('dbo.role_requests', 'user_id') IS NULL
    ALTER TABLE dbo.role_requests ADD user_id bigint NULL;
GO

IF OBJECT_ID(N'dbo.shipping_tariffs', N'U') IS NULL
BEGIN
    create table shipping_tariffs (id bigint identity not null, base_fee numeric(18,2) not null, bulky_surcharge numeric(18,2) not null, created_at datetime2(7), destination_area nvarchar(150) not null, estimated_time nvarchar(100), is_active bit not null, is_deleted bit not null, note nvarchar(500), service_name nvarchar(100) not null, updated_at datetime2(7), delivery_id bigint, store_id bigint, primary key (id));
END;
GO
IF COL_LENGTH('dbo.shipping_tariffs', 'base_fee') IS NULL
    ALTER TABLE dbo.shipping_tariffs ADD base_fee numeric(18,2) NULL;
IF COL_LENGTH('dbo.shipping_tariffs', 'bulky_surcharge') IS NULL
    ALTER TABLE dbo.shipping_tariffs ADD bulky_surcharge numeric(18,2) NULL;
IF COL_LENGTH('dbo.shipping_tariffs', 'created_at') IS NULL
    ALTER TABLE dbo.shipping_tariffs ADD created_at datetime2(7) NULL;
IF COL_LENGTH('dbo.shipping_tariffs', 'destination_area') IS NULL
    ALTER TABLE dbo.shipping_tariffs ADD destination_area nvarchar(150) NULL;
IF COL_LENGTH('dbo.shipping_tariffs', 'estimated_time') IS NULL
    ALTER TABLE dbo.shipping_tariffs ADD estimated_time nvarchar(100) NULL;
IF COL_LENGTH('dbo.shipping_tariffs', 'is_active') IS NULL
    ALTER TABLE dbo.shipping_tariffs ADD is_active bit NULL;
IF COL_LENGTH('dbo.shipping_tariffs', 'is_deleted') IS NULL
    ALTER TABLE dbo.shipping_tariffs ADD is_deleted bit NOT NULL CONSTRAINT DF_upgrade_shipping_tariffs_is_deleted DEFAULT 0;
IF COL_LENGTH('dbo.shipping_tariffs', 'note') IS NULL
    ALTER TABLE dbo.shipping_tariffs ADD note nvarchar(500) NULL;
IF COL_LENGTH('dbo.shipping_tariffs', 'service_name') IS NULL
    ALTER TABLE dbo.shipping_tariffs ADD service_name nvarchar(100) NULL;
IF COL_LENGTH('dbo.shipping_tariffs', 'updated_at') IS NULL
    ALTER TABLE dbo.shipping_tariffs ADD updated_at datetime2(7) NULL;
IF COL_LENGTH('dbo.shipping_tariffs', 'delivery_id') IS NULL
    ALTER TABLE dbo.shipping_tariffs ADD delivery_id bigint NULL;
IF COL_LENGTH('dbo.shipping_tariffs', 'store_id') IS NULL
    ALTER TABLE dbo.shipping_tariffs ADD store_id bigint NULL;
GO

IF OBJECT_ID(N'dbo.store_inventories', N'U') IS NULL
BEGIN
    create table store_inventories (id bigint identity not null, quantity int not null, version bigint not null, product_id bigint not null, store_id bigint not null, primary key (id));
END;
GO
IF COL_LENGTH('dbo.store_inventories', 'quantity') IS NULL
    ALTER TABLE dbo.store_inventories ADD quantity int NULL;
IF COL_LENGTH('dbo.store_inventories', 'version') IS NULL
    ALTER TABLE dbo.store_inventories ADD version bigint NOT NULL CONSTRAINT DF_upgrade_store_inventories_version DEFAULT 0;
IF COL_LENGTH('dbo.store_inventories', 'product_id') IS NULL
    ALTER TABLE dbo.store_inventories ADD product_id bigint NULL;
IF COL_LENGTH('dbo.store_inventories', 'store_id') IS NULL
    ALTER TABLE dbo.store_inventories ADD store_id bigint NULL;
GO

IF OBJECT_ID(N'dbo.store_levels', N'U') IS NULL
BEGIN
    create table store_levels (id bigint identity not null, created_at datetime2(7), discount numeric(38,2) not null, is_deleted bit, min_point int not null, name nvarchar(32) not null, updated_at datetime2(7), primary key (id));
END;
GO
IF COL_LENGTH('dbo.store_levels', 'created_at') IS NULL
    ALTER TABLE dbo.store_levels ADD created_at datetime2(7) NULL;
IF COL_LENGTH('dbo.store_levels', 'discount') IS NULL
    ALTER TABLE dbo.store_levels ADD discount numeric(38,2) NULL;
IF COL_LENGTH('dbo.store_levels', 'is_deleted') IS NULL
    ALTER TABLE dbo.store_levels ADD is_deleted bit NOT NULL CONSTRAINT DF_upgrade_store_levels_is_deleted DEFAULT 0;
IF COL_LENGTH('dbo.store_levels', 'min_point') IS NULL
    ALTER TABLE dbo.store_levels ADD min_point int NULL;
IF COL_LENGTH('dbo.store_levels', 'name') IS NULL
    ALTER TABLE dbo.store_levels ADD name nvarchar(32) NULL;
IF COL_LENGTH('dbo.store_levels', 'updated_at') IS NULL
    ALTER TABLE dbo.store_levels ADD updated_at datetime2(7) NULL;
GO

IF OBJECT_ID(N'dbo.store_memberships', N'U') IS NULL
BEGIN
    create table store_memberships (id bigint identity not null, access_role varchar(16) not null, active bit not null, joined_at datetime2(7), store_id bigint not null, user_id bigint not null, primary key (id));
END;
GO
IF COL_LENGTH('dbo.store_memberships', 'access_role') IS NULL
    ALTER TABLE dbo.store_memberships ADD access_role varchar(16) NULL;
IF COL_LENGTH('dbo.store_memberships', 'active') IS NULL
    ALTER TABLE dbo.store_memberships ADD active bit NULL;
IF COL_LENGTH('dbo.store_memberships', 'joined_at') IS NULL
    ALTER TABLE dbo.store_memberships ADD joined_at datetime2(7) NULL;
IF COL_LENGTH('dbo.store_memberships', 'store_id') IS NULL
    ALTER TABLE dbo.store_memberships ADD store_id bigint NULL;
IF COL_LENGTH('dbo.store_memberships', 'user_id') IS NULL
    ALTER TABLE dbo.store_memberships ADD user_id bigint NULL;
GO

IF OBJECT_ID(N'dbo.stores', N'U') IS NULL
BEGIN
    create table stores (id bigint identity not null, address nvarchar(255) not null, avatar varchar(500), avatar_public_id varchar(255), bio nvarchar(1000), cover varchar(500), cover_public_id varchar(255), created_at datetime2(7), creation_request_key varchar(80), e_wallet numeric(18,2), featured_image_public_ids NVARCHAR(MAX), featured_images NVARCHAR(MAX), is_open bit, latitude float(53), longitude float(53), name nvarchar(150) not null, phone varchar(20), point int, rating float(53), status bit, version bigint not null, commission_id bigint, owner_id bigint, store_level_id bigint, primary key (id));
END;
GO
IF COL_LENGTH('dbo.stores', 'address') IS NULL
    ALTER TABLE dbo.stores ADD address nvarchar(255) NULL;
IF COL_LENGTH('dbo.stores', 'avatar') IS NULL
    ALTER TABLE dbo.stores ADD avatar varchar(500) NULL;
IF COL_LENGTH('dbo.stores', 'avatar_public_id') IS NULL
    ALTER TABLE dbo.stores ADD avatar_public_id varchar(255) NULL;
IF COL_LENGTH('dbo.stores', 'bio') IS NULL
    ALTER TABLE dbo.stores ADD bio nvarchar(1000) NULL;
IF COL_LENGTH('dbo.stores', 'cover') IS NULL
    ALTER TABLE dbo.stores ADD cover varchar(500) NULL;
IF COL_LENGTH('dbo.stores', 'cover_public_id') IS NULL
    ALTER TABLE dbo.stores ADD cover_public_id varchar(255) NULL;
IF COL_LENGTH('dbo.stores', 'created_at') IS NULL
    ALTER TABLE dbo.stores ADD created_at datetime2(7) NULL;
IF COL_LENGTH('dbo.stores', 'creation_request_key') IS NULL
    ALTER TABLE dbo.stores ADD creation_request_key varchar(80) NULL;
IF COL_LENGTH('dbo.stores', 'e_wallet') IS NULL
    ALTER TABLE dbo.stores ADD e_wallet numeric(18,2) NULL;
IF COL_LENGTH('dbo.stores', 'featured_image_public_ids') IS NULL
    ALTER TABLE dbo.stores ADD featured_image_public_ids NVARCHAR(MAX) NULL;
IF COL_LENGTH('dbo.stores', 'featured_images') IS NULL
    ALTER TABLE dbo.stores ADD featured_images NVARCHAR(MAX) NULL;
IF COL_LENGTH('dbo.stores', 'is_open') IS NULL
    ALTER TABLE dbo.stores ADD is_open bit NULL;
IF COL_LENGTH('dbo.stores', 'latitude') IS NULL
    ALTER TABLE dbo.stores ADD latitude float(53) NULL;
IF COL_LENGTH('dbo.stores', 'longitude') IS NULL
    ALTER TABLE dbo.stores ADD longitude float(53) NULL;
IF COL_LENGTH('dbo.stores', 'name') IS NULL
    ALTER TABLE dbo.stores ADD name nvarchar(150) NULL;
IF COL_LENGTH('dbo.stores', 'phone') IS NULL
    ALTER TABLE dbo.stores ADD phone varchar(20) NULL;
IF COL_LENGTH('dbo.stores', 'point') IS NULL
    ALTER TABLE dbo.stores ADD point int NULL;
IF COL_LENGTH('dbo.stores', 'rating') IS NULL
    ALTER TABLE dbo.stores ADD rating float(53) NULL;
IF COL_LENGTH('dbo.stores', 'status') IS NULL
    ALTER TABLE dbo.stores ADD status bit NULL;
IF COL_LENGTH('dbo.stores', 'version') IS NULL
    ALTER TABLE dbo.stores ADD version bigint NOT NULL CONSTRAINT DF_upgrade_stores_version DEFAULT 0;
IF COL_LENGTH('dbo.stores', 'commission_id') IS NULL
    ALTER TABLE dbo.stores ADD commission_id bigint NULL;
IF COL_LENGTH('dbo.stores', 'owner_id') IS NULL
    ALTER TABLE dbo.stores ADD owner_id bigint NULL;
IF COL_LENGTH('dbo.stores', 'store_level_id') IS NULL
    ALTER TABLE dbo.stores ADD store_level_id bigint NULL;
GO

IF OBJECT_ID(N'dbo.style_values', N'U') IS NULL
BEGIN
    create table style_values (id bigint identity not null, created_at datetime2(7), is_deleted bit, name nvarchar(32) not null, updated_at datetime2(7), style_id bigint not null, primary key (id));
END;
GO
IF COL_LENGTH('dbo.style_values', 'created_at') IS NULL
    ALTER TABLE dbo.style_values ADD created_at datetime2(7) NULL;
IF COL_LENGTH('dbo.style_values', 'is_deleted') IS NULL
    ALTER TABLE dbo.style_values ADD is_deleted bit NOT NULL CONSTRAINT DF_upgrade_style_values_is_deleted DEFAULT 0;
IF COL_LENGTH('dbo.style_values', 'name') IS NULL
    ALTER TABLE dbo.style_values ADD name nvarchar(32) NULL;
IF COL_LENGTH('dbo.style_values', 'updated_at') IS NULL
    ALTER TABLE dbo.style_values ADD updated_at datetime2(7) NULL;
IF COL_LENGTH('dbo.style_values', 'style_id') IS NULL
    ALTER TABLE dbo.style_values ADD style_id bigint NULL;
GO

IF OBJECT_ID(N'dbo.styles', N'U') IS NULL
BEGIN
    create table styles (id bigint identity not null, created_at datetime2(7), is_deleted bit not null, name nvarchar(100) not null, updated_at datetime2(7), category_id bigint, primary key (id));
END;
GO
IF COL_LENGTH('dbo.styles', 'created_at') IS NULL
    ALTER TABLE dbo.styles ADD created_at datetime2(7) NULL;
IF COL_LENGTH('dbo.styles', 'is_deleted') IS NULL
    ALTER TABLE dbo.styles ADD is_deleted bit NOT NULL CONSTRAINT DF_upgrade_styles_is_deleted DEFAULT 0;
IF COL_LENGTH('dbo.styles', 'name') IS NULL
    ALTER TABLE dbo.styles ADD name nvarchar(100) NULL;
IF COL_LENGTH('dbo.styles', 'updated_at') IS NULL
    ALTER TABLE dbo.styles ADD updated_at datetime2(7) NULL;
IF COL_LENGTH('dbo.styles', 'category_id') IS NULL
    ALTER TABLE dbo.styles ADD category_id bigint NULL;
GO

IF OBJECT_ID(N'dbo.transactions', N'U') IS NULL
BEGIN
    create table transactions (id bigint identity not null, amount numeric(38,2) not null, created_at datetime2(7), description nvarchar(500), earned_points int, is_up bit not null, request_key varchar(80), status varchar(20), type varchar(30), order_id bigint, store_id bigint, user_id bigint, primary key (id));
END;
GO
IF COL_LENGTH('dbo.transactions', 'amount') IS NULL
    ALTER TABLE dbo.transactions ADD amount numeric(38,2) NULL;
IF COL_LENGTH('dbo.transactions', 'created_at') IS NULL
    ALTER TABLE dbo.transactions ADD created_at datetime2(7) NULL;
IF COL_LENGTH('dbo.transactions', 'description') IS NULL
    ALTER TABLE dbo.transactions ADD description nvarchar(500) NULL;
IF COL_LENGTH('dbo.transactions', 'earned_points') IS NULL
    ALTER TABLE dbo.transactions ADD earned_points int NULL;
IF COL_LENGTH('dbo.transactions', 'is_up') IS NULL
    ALTER TABLE dbo.transactions ADD is_up bit NULL;
IF COL_LENGTH('dbo.transactions', 'request_key') IS NULL
    ALTER TABLE dbo.transactions ADD request_key varchar(80) NULL;
IF COL_LENGTH('dbo.transactions', 'status') IS NULL
    ALTER TABLE dbo.transactions ADD status varchar(20) NULL;
IF COL_LENGTH('dbo.transactions', 'type') IS NULL
    ALTER TABLE dbo.transactions ADD type varchar(30) NULL;
IF COL_LENGTH('dbo.transactions', 'order_id') IS NULL
    ALTER TABLE dbo.transactions ADD order_id bigint NULL;
IF COL_LENGTH('dbo.transactions', 'store_id') IS NULL
    ALTER TABLE dbo.transactions ADD store_id bigint NULL;
IF COL_LENGTH('dbo.transactions', 'user_id') IS NULL
    ALTER TABLE dbo.transactions ADD user_id bigint NULL;
GO

IF OBJECT_ID(N'dbo.user_addresses', N'U') IS NULL
BEGIN
    create table user_addresses (id bigint identity not null, address_detail nvarchar(255) not null, created_at datetime2(7), is_default bit not null, phone varchar(20) not null, recipient_name nvarchar(100) not null, user_id bigint not null, primary key (id));
END;
GO
IF COL_LENGTH('dbo.user_addresses', 'address_detail') IS NULL
    ALTER TABLE dbo.user_addresses ADD address_detail nvarchar(255) NULL;
IF COL_LENGTH('dbo.user_addresses', 'created_at') IS NULL
    ALTER TABLE dbo.user_addresses ADD created_at datetime2(7) NULL;
IF COL_LENGTH('dbo.user_addresses', 'is_default') IS NULL
    ALTER TABLE dbo.user_addresses ADD is_default bit NULL;
IF COL_LENGTH('dbo.user_addresses', 'phone') IS NULL
    ALTER TABLE dbo.user_addresses ADD phone varchar(20) NULL;
IF COL_LENGTH('dbo.user_addresses', 'recipient_name') IS NULL
    ALTER TABLE dbo.user_addresses ADD recipient_name nvarchar(100) NULL;
IF COL_LENGTH('dbo.user_addresses', 'user_id') IS NULL
    ALTER TABLE dbo.user_addresses ADD user_id bigint NULL;
GO

IF OBJECT_ID(N'dbo.user_follow_store', N'U') IS NULL
BEGIN
    create table user_follow_store (id bigint identity not null, created_at datetime2(7), store_id bigint not null, user_id bigint not null, primary key (id));
END;
GO
IF COL_LENGTH('dbo.user_follow_store', 'created_at') IS NULL
    ALTER TABLE dbo.user_follow_store ADD created_at datetime2(7) NULL;
IF COL_LENGTH('dbo.user_follow_store', 'store_id') IS NULL
    ALTER TABLE dbo.user_follow_store ADD store_id bigint NULL;
IF COL_LENGTH('dbo.user_follow_store', 'user_id') IS NULL
    ALTER TABLE dbo.user_follow_store ADD user_id bigint NULL;
GO

IF OBJECT_ID(N'dbo.user_levels', N'U') IS NULL
BEGIN
    create table user_levels (id bigint identity not null, created_at datetime2(7), discount numeric(38,2) not null, is_deleted bit, min_point int not null, name nvarchar(32) not null, updated_at datetime2(7), primary key (id));
END;
GO
IF COL_LENGTH('dbo.user_levels', 'created_at') IS NULL
    ALTER TABLE dbo.user_levels ADD created_at datetime2(7) NULL;
IF COL_LENGTH('dbo.user_levels', 'discount') IS NULL
    ALTER TABLE dbo.user_levels ADD discount numeric(38,2) NULL;
IF COL_LENGTH('dbo.user_levels', 'is_deleted') IS NULL
    ALTER TABLE dbo.user_levels ADD is_deleted bit NOT NULL CONSTRAINT DF_upgrade_user_levels_is_deleted DEFAULT 0;
IF COL_LENGTH('dbo.user_levels', 'min_point') IS NULL
    ALTER TABLE dbo.user_levels ADD min_point int NULL;
IF COL_LENGTH('dbo.user_levels', 'name') IS NULL
    ALTER TABLE dbo.user_levels ADD name nvarchar(32) NULL;
IF COL_LENGTH('dbo.user_levels', 'updated_at') IS NULL
    ALTER TABLE dbo.user_levels ADD updated_at datetime2(7) NULL;
GO

IF OBJECT_ID(N'dbo.user_roles', N'U') IS NULL
BEGIN
    create table user_roles (user_id bigint not null, role varchar(30) not null check ((role in ('MANAGER','ADMIN','USER'))), primary key (user_id, role));
END;
GO
IF COL_LENGTH('dbo.user_roles', 'user_id') IS NULL
    ALTER TABLE dbo.user_roles ADD user_id bigint NULL;
IF COL_LENGTH('dbo.user_roles', 'role') IS NULL
    ALTER TABLE dbo.user_roles ADD role varchar(30) NULL;
GO

IF OBJECT_ID(N'dbo.users', N'U') IS NULL
BEGIN
    create table users (id bigint identity not null, account_disabled_by_admin bit, address nvarchar(255), avatar_public_id varchar(255), avatar_url varchar(255), cover varchar(500), cover_public_id varchar(255), created_at datetime2(7), created_date date, creation_time_known bit, e_wallet numeric(18,2), email varchar(100) not null, full_name nvarchar(100) not null, is_active bit, is_email_active bit not null, otp_code varchar(10), otp_expiry datetime2(7), password varchar(255) not null, phone varchar(20), point int, retired_delivery_account bit, role varchar(20) not null check ((role in ('MANAGER','ADMIN','USER'))), username varchar(50) not null, store_id bigint, user_level_id bigint, primary key (id));
END;
GO
IF COL_LENGTH('dbo.users', 'account_disabled_by_admin') IS NULL
    ALTER TABLE dbo.users ADD account_disabled_by_admin bit NOT NULL CONSTRAINT DF_upgrade_users_account_disabled_by_admin DEFAULT 0;
IF COL_LENGTH('dbo.users', 'address') IS NULL
    ALTER TABLE dbo.users ADD address nvarchar(255) NULL;
IF COL_LENGTH('dbo.users', 'avatar_public_id') IS NULL
    ALTER TABLE dbo.users ADD avatar_public_id varchar(255) NULL;
IF COL_LENGTH('dbo.users', 'avatar_url') IS NULL
    ALTER TABLE dbo.users ADD avatar_url varchar(255) NULL;
IF COL_LENGTH('dbo.users', 'cover') IS NULL
    ALTER TABLE dbo.users ADD cover varchar(500) NULL;
IF COL_LENGTH('dbo.users', 'cover_public_id') IS NULL
    ALTER TABLE dbo.users ADD cover_public_id varchar(255) NULL;
IF COL_LENGTH('dbo.users', 'created_at') IS NULL
    ALTER TABLE dbo.users ADD created_at datetime2(7) NULL;
IF COL_LENGTH('dbo.users', 'created_date') IS NULL
    ALTER TABLE dbo.users ADD created_date date NULL;
IF COL_LENGTH('dbo.users', 'creation_time_known') IS NULL
    ALTER TABLE dbo.users ADD creation_time_known bit NOT NULL CONSTRAINT DF_upgrade_users_creation_time_known DEFAULT 0;
IF COL_LENGTH('dbo.users', 'e_wallet') IS NULL
    ALTER TABLE dbo.users ADD e_wallet numeric(18,2) NULL;
IF COL_LENGTH('dbo.users', 'email') IS NULL
    ALTER TABLE dbo.users ADD email varchar(100) NULL;
IF COL_LENGTH('dbo.users', 'full_name') IS NULL
    ALTER TABLE dbo.users ADD full_name nvarchar(100) NULL;
IF COL_LENGTH('dbo.users', 'is_active') IS NULL
    ALTER TABLE dbo.users ADD is_active bit NULL;
IF COL_LENGTH('dbo.users', 'is_email_active') IS NULL
    ALTER TABLE dbo.users ADD is_email_active bit NULL;
IF COL_LENGTH('dbo.users', 'otp_code') IS NULL
    ALTER TABLE dbo.users ADD otp_code varchar(10) NULL;
IF COL_LENGTH('dbo.users', 'otp_expiry') IS NULL
    ALTER TABLE dbo.users ADD otp_expiry datetime2(7) NULL;
IF COL_LENGTH('dbo.users', 'password') IS NULL
    ALTER TABLE dbo.users ADD password varchar(255) NULL;
IF COL_LENGTH('dbo.users', 'phone') IS NULL
    ALTER TABLE dbo.users ADD phone varchar(20) NULL;
IF COL_LENGTH('dbo.users', 'point') IS NULL
    ALTER TABLE dbo.users ADD point int NULL;
IF COL_LENGTH('dbo.users', 'retired_delivery_account') IS NULL
    ALTER TABLE dbo.users ADD retired_delivery_account bit NOT NULL CONSTRAINT DF_upgrade_users_retired_delivery_account DEFAULT 0;
IF COL_LENGTH('dbo.users', 'role') IS NULL
    ALTER TABLE dbo.users ADD role varchar(20) NULL;
IF COL_LENGTH('dbo.users', 'username') IS NULL
    ALTER TABLE dbo.users ADD username varchar(50) NULL;
IF COL_LENGTH('dbo.users', 'store_id') IS NULL
    ALTER TABLE dbo.users ADD store_id bigint NULL;
IF COL_LENGTH('dbo.users', 'user_level_id') IS NULL
    ALTER TABLE dbo.users ADD user_level_id bigint NULL;
GO

IF OBJECT_ID(N'dbo.vouchers', N'U') IS NULL
BEGIN
    create table vouchers (id bigint identity not null, code varchar(50) not null, created_at datetime2(7), discount_type varchar(20) not null, discount_value numeric(38,2) not null, end_date date not null, is_active bit not null, max_discount_amount numeric(38,2), min_order_value numeric(38,2), name nvarchar(150) not null, quantity int not null, start_date date not null, used_count int not null, store_id bigint, primary key (id));
END;
GO
IF COL_LENGTH('dbo.vouchers', 'code') IS NULL
    ALTER TABLE dbo.vouchers ADD code varchar(50) NULL;
IF COL_LENGTH('dbo.vouchers', 'created_at') IS NULL
    ALTER TABLE dbo.vouchers ADD created_at datetime2(7) NULL;
IF COL_LENGTH('dbo.vouchers', 'discount_type') IS NULL
    ALTER TABLE dbo.vouchers ADD discount_type varchar(20) NULL;
IF COL_LENGTH('dbo.vouchers', 'discount_value') IS NULL
    ALTER TABLE dbo.vouchers ADD discount_value numeric(38,2) NULL;
IF COL_LENGTH('dbo.vouchers', 'end_date') IS NULL
    ALTER TABLE dbo.vouchers ADD end_date date NULL;
IF COL_LENGTH('dbo.vouchers', 'is_active') IS NULL
    ALTER TABLE dbo.vouchers ADD is_active bit NULL;
IF COL_LENGTH('dbo.vouchers', 'max_discount_amount') IS NULL
    ALTER TABLE dbo.vouchers ADD max_discount_amount numeric(38,2) NULL;
IF COL_LENGTH('dbo.vouchers', 'min_order_value') IS NULL
    ALTER TABLE dbo.vouchers ADD min_order_value numeric(38,2) NULL;
IF COL_LENGTH('dbo.vouchers', 'name') IS NULL
    ALTER TABLE dbo.vouchers ADD name nvarchar(150) NULL;
IF COL_LENGTH('dbo.vouchers', 'quantity') IS NULL
    ALTER TABLE dbo.vouchers ADD quantity int NULL;
IF COL_LENGTH('dbo.vouchers', 'start_date') IS NULL
    ALTER TABLE dbo.vouchers ADD start_date date NULL;
IF COL_LENGTH('dbo.vouchers', 'used_count') IS NULL
    ALTER TABLE dbo.vouchers ADD used_count int NULL;
IF COL_LENGTH('dbo.vouchers', 'store_id') IS NULL
    ALTER TABLE dbo.vouchers ADD store_id bigint NULL;
GO

IF NOT EXISTS (SELECT 1 FROM sys.objects WHERE name = 'UK64t7ox312pqal3p7fg9o503c2' AND parent_object_id = OBJECT_ID('dbo.carts'))
    alter table carts add constraint UK64t7ox312pqal3p7fg9o503c2 unique (user_id);
GO

IF NOT EXISTS (SELECT 1 FROM sys.objects WHERE name = 'UKt8o6pivur7nn124jehx7cygw5' AND parent_object_id = OBJECT_ID('dbo.categories'))
    alter table categories add constraint UKt8o6pivur7nn124jehx7cygw5 unique (name);
GO

IF NOT EXISTS (SELECT 1 FROM sys.objects WHERE name = 'UK7ds4r2pfp64juwa44fwmh54qh' AND parent_object_id = OBJECT_ID('dbo.chat_conversations'))
    alter table chat_conversations add constraint UK7ds4r2pfp64juwa44fwmh54qh unique (conversation_key);
GO

IF NOT EXISTS (SELECT 1 FROM sys.objects WHERE name = 'UKglyxi3j3gc62pa8vhgmyuu326' AND parent_object_id = OBJECT_ID('dbo.chat_messages'))
    alter table chat_messages add constraint UKglyxi3j3gc62pa8vhgmyuu326 unique (conversation_id, sender_id, client_message_id);
GO

IF NOT EXISTS (SELECT 1 FROM sys.objects WHERE name = 'UKhp34c4kdi2glh06lxfybb00bb' AND parent_object_id = OBJECT_ID('dbo.commissions'))
    alter table commissions add constraint UKhp34c4kdi2glh06lxfybb00bb unique (name);
GO

IF NOT EXISTS (SELECT 1 FROM sys.objects WHERE name = 'UK23btti2t7198im5s1enq84xwh' AND parent_object_id = OBJECT_ID('dbo.deliveries'))
    alter table deliveries add constraint UK23btti2t7198im5s1enq84xwh unique (name);
GO

IF NOT EXISTS (SELECT 1 FROM sys.objects WHERE name = 'UK4yyb20v8dlkofq876yc6aq2d0' AND parent_object_id = OBJECT_ID('dbo.momo_payment_attempts'))
    alter table momo_payment_attempts add constraint UK4yyb20v8dlkofq876yc6aq2d0 unique (gateway_order_id);
GO

IF NOT EXISTS (SELECT 1 FROM sys.objects WHERE name = 'UK1odxoox1qvmqntsr5rasww1x6' AND parent_object_id = OBJECT_ID('dbo.momo_payment_attempts'))
    alter table momo_payment_attempts add constraint UK1odxoox1qvmqntsr5rasww1x6 unique (request_id);
GO

IF NOT EXISTS (SELECT 1 FROM sys.objects WHERE name = 'uq_user_wishlist' AND parent_object_id = OBJECT_ID('dbo.product_wishlists'))
    alter table product_wishlists add constraint uq_user_wishlist unique (user_id, product_id);
GO

IF NOT EXISTS (SELECT 1 FROM sys.objects WHERE name = 'UKnli2i738mjqjmxhoaha6hrp1k' AND parent_object_id = OBJECT_ID('dbo.store_inventories'))
    alter table store_inventories add constraint UKnli2i738mjqjmxhoaha6hrp1k unique (store_id, product_id);
GO

IF NOT EXISTS (SELECT 1 FROM sys.objects WHERE name = 'UK59x77kdmmxjuh4glireym50bk' AND parent_object_id = OBJECT_ID('dbo.store_levels'))
    alter table store_levels add constraint UK59x77kdmmxjuh4glireym50bk unique (min_point);
GO

IF NOT EXISTS (SELECT 1 FROM sys.objects WHERE name = 'UKru6j646n9grvbu9fpesqaw7cj' AND parent_object_id = OBJECT_ID('dbo.store_levels'))
    alter table store_levels add constraint UKru6j646n9grvbu9fpesqaw7cj unique (name);
GO

IF NOT EXISTS (SELECT 1 FROM sys.objects WHERE name = 'UKj4hfafeymtlvresja6jsiquid' AND parent_object_id = OBJECT_ID('dbo.store_memberships'))
    alter table store_memberships add constraint UKj4hfafeymtlvresja6jsiquid unique (store_id, user_id);
GO

IF NOT EXISTS (SELECT 1 FROM sys.objects WHERE name = 'UKt66pj9594i558j902po967bfq' AND parent_object_id = OBJECT_ID('dbo.styles'))
    alter table styles add constraint UKt66pj9594i558j902po967bfq unique (name);
GO

IF NOT EXISTS (SELECT 1 FROM sys.objects WHERE name = 'uq_user_follow_store' AND parent_object_id = OBJECT_ID('dbo.user_follow_store'))
    alter table user_follow_store add constraint uq_user_follow_store unique (user_id, store_id);
GO

IF NOT EXISTS (SELECT 1 FROM sys.objects WHERE name = 'UK77sn8fbnmyyqh80vlibfhhbru' AND parent_object_id = OBJECT_ID('dbo.user_levels'))
    alter table user_levels add constraint UK77sn8fbnmyyqh80vlibfhhbru unique (min_point);
GO

IF NOT EXISTS (SELECT 1 FROM sys.objects WHERE name = 'UKd70naawuai6m3nc3h489ng926' AND parent_object_id = OBJECT_ID('dbo.user_levels'))
    alter table user_levels add constraint UKd70naawuai6m3nc3h489ng926 unique (name);
GO

IF NOT EXISTS (SELECT 1 FROM sys.objects WHERE name = 'UK6dotkott2kjsp8vw4d0m25fb7' AND parent_object_id = OBJECT_ID('dbo.users'))
    alter table users add constraint UK6dotkott2kjsp8vw4d0m25fb7 unique (email);
GO

IF NOT EXISTS (SELECT 1 FROM sys.objects WHERE name = 'UKr43af9ap4edm43mmtq01oddj6' AND parent_object_id = OBJECT_ID('dbo.users'))
    alter table users add constraint UKr43af9ap4edm43mmtq01oddj6 unique (username);
GO

IF NOT EXISTS (SELECT 1 FROM sys.objects WHERE name = 'UK30ftp2biebbvpik8e49wlmady' AND parent_object_id = OBJECT_ID('dbo.vouchers'))
    alter table vouchers add constraint UK30ftp2biebbvpik8e49wlmady unique (code);
GO

IF NOT EXISTS (SELECT 1 FROM sys.objects WHERE name = 'FK5bm1lt4f4eevt8lv2517soakd' AND parent_object_id = OBJECT_ID('dbo.activity_logs'))
    alter table activity_logs add constraint FK5bm1lt4f4eevt8lv2517soakd foreign key (user_id) references users;
GO

IF NOT EXISTS (SELECT 1 FROM sys.objects WHERE name = 'FKpcttvuq4mxppo8sxggjtn5i2c' AND parent_object_id = OBJECT_ID('dbo.cart_items'))
    alter table cart_items add constraint FKpcttvuq4mxppo8sxggjtn5i2c foreign key (cart_id) references carts;
GO

IF NOT EXISTS (SELECT 1 FROM sys.objects WHERE name = 'FK1re40cjegsfvw58xrkdp6bac6' AND parent_object_id = OBJECT_ID('dbo.cart_items'))
    alter table cart_items add constraint FK1re40cjegsfvw58xrkdp6bac6 foreign key (product_id) references products;
GO

IF NOT EXISTS (SELECT 1 FROM sys.objects WHERE name = 'FK5hdx4t879kwd8w87xkhs9c6dq' AND parent_object_id = OBJECT_ID('dbo.cart_items'))
    alter table cart_items add constraint FK5hdx4t879kwd8w87xkhs9c6dq foreign key (store_id) references stores;
GO

IF NOT EXISTS (SELECT 1 FROM sys.objects WHERE name = 'FKb5o626f86h46m4s7ms6ginnop' AND parent_object_id = OBJECT_ID('dbo.carts'))
    alter table carts add constraint FKb5o626f86h46m4s7ms6ginnop foreign key (user_id) references users;
GO

IF NOT EXISTS (SELECT 1 FROM sys.objects WHERE name = 'FKsaok720gsu4u2wrgbk10b5n8d' AND parent_object_id = OBJECT_ID('dbo.categories'))
    alter table categories add constraint FKsaok720gsu4u2wrgbk10b5n8d foreign key (parent_id) references categories;
GO

IF NOT EXISTS (SELECT 1 FROM sys.objects WHERE name = 'FKld52p1xmde0cjvgv223ari29b' AND parent_object_id = OBJECT_ID('dbo.chat_conversations'))
    alter table chat_conversations add constraint FKld52p1xmde0cjvgv223ari29b foreign key (customer_id) references users;
GO

IF NOT EXISTS (SELECT 1 FROM sys.objects WHERE name = 'FKrkyi6s1ft5jbdislsnxmwh2pk' AND parent_object_id = OBJECT_ID('dbo.chat_conversations'))
    alter table chat_conversations add constraint FKrkyi6s1ft5jbdislsnxmwh2pk foreign key (store_id) references stores;
GO

IF NOT EXISTS (SELECT 1 FROM sys.objects WHERE name = 'FKqgkanrr90j46564w4ww63jcna' AND parent_object_id = OBJECT_ID('dbo.chat_messages'))
    alter table chat_messages add constraint FKqgkanrr90j46564w4ww63jcna foreign key (conversation_id) references chat_conversations;
GO

IF NOT EXISTS (SELECT 1 FROM sys.objects WHERE name = 'FKgiqeap8ays4lf684x7m0r2729' AND parent_object_id = OBJECT_ID('dbo.chat_messages'))
    alter table chat_messages add constraint FKgiqeap8ays4lf684x7m0r2729 foreign key (sender_id) references users;
GO

IF NOT EXISTS (SELECT 1 FROM sys.objects WHERE name = 'FKej5vbouyy3i9p4y8qw5sq98ah' AND parent_object_id = OBJECT_ID('dbo.inventory_transfer_requests'))
    alter table inventory_transfer_requests add constraint FKej5vbouyy3i9p4y8qw5sq98ah foreign key (approved_by) references users;
GO

IF NOT EXISTS (SELECT 1 FROM sys.objects WHERE name = 'FK5a8ds67i17h3qo66s30rgqwjc' AND parent_object_id = OBJECT_ID('dbo.inventory_transfer_requests'))
    alter table inventory_transfer_requests add constraint FK5a8ds67i17h3qo66s30rgqwjc foreign key (from_store_id) references stores;
GO

IF NOT EXISTS (SELECT 1 FROM sys.objects WHERE name = 'FKg6yujmfiba7s7q72kjxrj57gk' AND parent_object_id = OBJECT_ID('dbo.inventory_transfer_requests'))
    alter table inventory_transfer_requests add constraint FKg6yujmfiba7s7q72kjxrj57gk foreign key (product_id) references products;
GO

IF NOT EXISTS (SELECT 1 FROM sys.objects WHERE name = 'FKra51d45u5bogis2am06tsnei6' AND parent_object_id = OBJECT_ID('dbo.inventory_transfer_requests'))
    alter table inventory_transfer_requests add constraint FKra51d45u5bogis2am06tsnei6 foreign key (requested_by) references users;
GO

IF NOT EXISTS (SELECT 1 FROM sys.objects WHERE name = 'FKo0ro90y62109bbnrw3035d1ok' AND parent_object_id = OBJECT_ID('dbo.inventory_transfer_requests'))
    alter table inventory_transfer_requests add constraint FKo0ro90y62109bbnrw3035d1ok foreign key (to_store_id) references stores;
GO

IF NOT EXISTS (SELECT 1 FROM sys.objects WHERE name = 'FKroxe9ihrjj2vo8g48ary6yvcm' AND parent_object_id = OBJECT_ID('dbo.jwt_refresh_tokens'))
    alter table jwt_refresh_tokens add constraint FKroxe9ihrjj2vo8g48ary6yvcm foreign key (session_id) references jwt_sessions;
GO

IF NOT EXISTS (SELECT 1 FROM sys.objects WHERE name = 'FK9dc87454jgu3s53vlk9b61myu' AND parent_object_id = OBJECT_ID('dbo.jwt_sessions'))
    alter table jwt_sessions add constraint FK9dc87454jgu3s53vlk9b61myu foreign key (user_id) references users;
GO

IF NOT EXISTS (SELECT 1 FROM sys.objects WHERE name = 'FKghde1tpss2u5vmycow9vc49lc' AND parent_object_id = OBJECT_ID('dbo.momo_payment_attempts'))
    alter table momo_payment_attempts add constraint FKghde1tpss2u5vmycow9vc49lc foreign key (order_id) references orders;
GO

IF NOT EXISTS (SELECT 1 FROM sys.objects WHERE name = 'FK9y21adhxn0ayjhfocscqox7bh' AND parent_object_id = OBJECT_ID('dbo.notifications'))
    alter table notifications add constraint FK9y21adhxn0ayjhfocscqox7bh foreign key (user_id) references users;
GO

IF NOT EXISTS (SELECT 1 FROM sys.objects WHERE name = 'FKjyu2qbqt8gnvno9oe9j2s2ldk' AND parent_object_id = OBJECT_ID('dbo.order_details'))
    alter table order_details add constraint FKjyu2qbqt8gnvno9oe9j2s2ldk foreign key (order_id) references orders;
GO

IF NOT EXISTS (SELECT 1 FROM sys.objects WHERE name = 'FK4q98utpd73imf4yhttm3w0eax' AND parent_object_id = OBJECT_ID('dbo.order_details'))
    alter table order_details add constraint FK4q98utpd73imf4yhttm3w0eax foreign key (product_id) references products;
GO

IF NOT EXISTS (SELECT 1 FROM sys.objects WHERE name = 'FKmjnf91amspoh2m4qg8ct5y8uj' AND parent_object_id = OBJECT_ID('dbo.orders'))
    alter table orders add constraint FKmjnf91amspoh2m4qg8ct5y8uj foreign key (shipper_id) references users;
GO

IF NOT EXISTS (SELECT 1 FROM sys.objects WHERE name = 'FK1mkbbvgn9c5h1kflgdsmb4wlh' AND parent_object_id = OBJECT_ID('dbo.orders'))
    alter table orders add constraint FK1mkbbvgn9c5h1kflgdsmb4wlh foreign key (pickup_store_id) references stores;
GO

IF NOT EXISTS (SELECT 1 FROM sys.objects WHERE name = 'FKolvb9ta2243gfb66xa497q54l' AND parent_object_id = OBJECT_ID('dbo.orders'))
    alter table orders add constraint FKolvb9ta2243gfb66xa497q54l foreign key (return_store_id) references stores;
GO

IF NOT EXISTS (SELECT 1 FROM sys.objects WHERE name = 'FK32ql8ubntj5uh44ph9659tiih' AND parent_object_id = OBJECT_ID('dbo.orders'))
    alter table orders add constraint FK32ql8ubntj5uh44ph9659tiih foreign key (user_id) references users;
GO

IF NOT EXISTS (SELECT 1 FROM sys.objects WHERE name = 'FKdimvsocblb17f45ikjr6xn1wj' AND parent_object_id = OBJECT_ID('dbo.orders'))
    alter table orders add constraint FKdimvsocblb17f45ikjr6xn1wj foreign key (voucher_id) references vouchers;
GO

IF NOT EXISTS (SELECT 1 FROM sys.objects WHERE name = 'FKssn2wgmijxelm0u26ti271lcf' AND parent_object_id = OBJECT_ID('dbo.otp_verifications'))
    alter table otp_verifications add constraint FKssn2wgmijxelm0u26ti271lcf foreign key (user_id) references users;
GO

IF NOT EXISTS (SELECT 1 FROM sys.objects WHERE name = 'FK81gagumt0r8y3rmudcgpbk42l' AND parent_object_id = OBJECT_ID('dbo.payments'))
    alter table payments add constraint FK81gagumt0r8y3rmudcgpbk42l foreign key (order_id) references orders;
GO

IF NOT EXISTS (SELECT 1 FROM sys.objects WHERE name = 'FKqnq71xsohugpqwf3c9gxmsuy' AND parent_object_id = OBJECT_ID('dbo.product_images'))
    alter table product_images add constraint FKqnq71xsohugpqwf3c9gxmsuy foreign key (product_id) references products;
GO

IF NOT EXISTS (SELECT 1 FROM sys.objects WHERE name = 'FK85vd9dytavlsl2x07ja801se' AND parent_object_id = OBJECT_ID('dbo.product_style_values'))
    alter table product_style_values add constraint FK85vd9dytavlsl2x07ja801se foreign key (style_value_id) references style_values;
GO

IF NOT EXISTS (SELECT 1 FROM sys.objects WHERE name = 'FKc5711wvcu4cr0qhh49pbi456f' AND parent_object_id = OBJECT_ID('dbo.product_style_values'))
    alter table product_style_values add constraint FKc5711wvcu4cr0qhh49pbi456f foreign key (product_id) references products;
GO

IF NOT EXISTS (SELECT 1 FROM sys.objects WHERE name = 'FKijv049itnxhknmq3vxaimd7ox' AND parent_object_id = OBJECT_ID('dbo.product_wishlists'))
    alter table product_wishlists add constraint FKijv049itnxhknmq3vxaimd7ox foreign key (product_id) references products;
GO

IF NOT EXISTS (SELECT 1 FROM sys.objects WHERE name = 'FK2exgx7bqbuyddij4kbkyxb6dx' AND parent_object_id = OBJECT_ID('dbo.product_wishlists'))
    alter table product_wishlists add constraint FK2exgx7bqbuyddij4kbkyxb6dx foreign key (user_id) references users;
GO

IF NOT EXISTS (SELECT 1 FROM sys.objects WHERE name = 'FKog2rp4qthbtt2lfyhfo32lsw9' AND parent_object_id = OBJECT_ID('dbo.products'))
    alter table products add constraint FKog2rp4qthbtt2lfyhfo32lsw9 foreign key (category_id) references categories;
GO

IF NOT EXISTS (SELECT 1 FROM sys.objects WHERE name = 'FKgcyffheofvmy2x5l78xam63mc' AND parent_object_id = OBJECT_ID('dbo.products'))
    alter table products add constraint FKgcyffheofvmy2x5l78xam63mc foreign key (store_id) references stores;
GO

IF NOT EXISTS (SELECT 1 FROM sys.objects WHERE name = 'FKom2fn577fpuiddjbssbq16wyu' AND parent_object_id = OBJECT_ID('dbo.promotions'))
    alter table promotions add constraint FKom2fn577fpuiddjbssbq16wyu foreign key (store_id) references stores;
GO

IF NOT EXISTS (SELECT 1 FROM sys.objects WHERE name = 'FK9rd0vyea43aug5ggyu3i2r68q' AND parent_object_id = OBJECT_ID('dbo.recently_viewed'))
    alter table recently_viewed add constraint FK9rd0vyea43aug5ggyu3i2r68q foreign key (product_id) references products;
GO

IF NOT EXISTS (SELECT 1 FROM sys.objects WHERE name = 'FK6e3sbjf7qnbbmaff019w7p8b1' AND parent_object_id = OBJECT_ID('dbo.recently_viewed'))
    alter table recently_viewed add constraint FK6e3sbjf7qnbbmaff019w7p8b1 foreign key (user_id) references users;
GO

IF NOT EXISTS (SELECT 1 FROM sys.objects WHERE name = 'FKqwgq1lxgahsxdspnwqfac6sv6' AND parent_object_id = OBJECT_ID('dbo.reviews'))
    alter table reviews add constraint FKqwgq1lxgahsxdspnwqfac6sv6 foreign key (order_id) references orders;
GO

IF NOT EXISTS (SELECT 1 FROM sys.objects WHERE name = 'FKpl51cejpw4gy5swfar8br9ngi' AND parent_object_id = OBJECT_ID('dbo.reviews'))
    alter table reviews add constraint FKpl51cejpw4gy5swfar8br9ngi foreign key (product_id) references products;
GO

IF NOT EXISTS (SELECT 1 FROM sys.objects WHERE name = 'FKg5v2uypi6rxq6647cqef2d7pt' AND parent_object_id = OBJECT_ID('dbo.reviews'))
    alter table reviews add constraint FKg5v2uypi6rxq6647cqef2d7pt foreign key (store_id) references stores;
GO

IF NOT EXISTS (SELECT 1 FROM sys.objects WHERE name = 'FKcgy7qjc1r99dp117y9en6lxye' AND parent_object_id = OBJECT_ID('dbo.reviews'))
    alter table reviews add constraint FKcgy7qjc1r99dp117y9en6lxye foreign key (user_id) references users;
GO

IF NOT EXISTS (SELECT 1 FROM sys.objects WHERE name = 'FKa9lbsh9xvp97f9y6gmv782upg' AND parent_object_id = OBJECT_ID('dbo.role_audit_logs'))
    alter table role_audit_logs add constraint FKa9lbsh9xvp97f9y6gmv782upg foreign key (performed_by_id) references users;
GO

IF NOT EXISTS (SELECT 1 FROM sys.objects WHERE name = 'FK2l39fvxtf3li7l5esjs1nbtht' AND parent_object_id = OBJECT_ID('dbo.role_audit_logs'))
    alter table role_audit_logs add constraint FK2l39fvxtf3li7l5esjs1nbtht foreign key (store_id) references stores;
GO

IF NOT EXISTS (SELECT 1 FROM sys.objects WHERE name = 'FKol18bx0p3yj0rl4v1dsdac4bt' AND parent_object_id = OBJECT_ID('dbo.role_audit_logs'))
    alter table role_audit_logs add constraint FKol18bx0p3yj0rl4v1dsdac4bt foreign key (user_id) references users;
GO

IF NOT EXISTS (SELECT 1 FROM sys.objects WHERE name = 'FKkhp04lucjilblmacg0g71p3qd' AND parent_object_id = OBJECT_ID('dbo.role_requests'))
    alter table role_requests add constraint FKkhp04lucjilblmacg0g71p3qd foreign key (assigned_store_id) references stores;
GO

IF NOT EXISTS (SELECT 1 FROM sys.objects WHERE name = 'FKsttq3p46qr0y1d4ec8hbtem7f' AND parent_object_id = OBJECT_ID('dbo.role_requests'))
    alter table role_requests add constraint FKsttq3p46qr0y1d4ec8hbtem7f foreign key (reviewed_by_id) references users;
GO

IF NOT EXISTS (SELECT 1 FROM sys.objects WHERE name = 'FK3bie0jak710g4lj2ajqtgrmmv' AND parent_object_id = OBJECT_ID('dbo.role_requests'))
    alter table role_requests add constraint FK3bie0jak710g4lj2ajqtgrmmv foreign key (target_store_id) references stores;
GO

IF NOT EXISTS (SELECT 1 FROM sys.objects WHERE name = 'FKljihuio5sbmdk3jjmtm3lh72j' AND parent_object_id = OBJECT_ID('dbo.role_requests'))
    alter table role_requests add constraint FKljihuio5sbmdk3jjmtm3lh72j foreign key (user_id) references users;
GO

IF NOT EXISTS (SELECT 1 FROM sys.objects WHERE name = 'FKh7j4yugm081bsgj2p42dsp339' AND parent_object_id = OBJECT_ID('dbo.shipping_tariffs'))
    alter table shipping_tariffs add constraint FKh7j4yugm081bsgj2p42dsp339 foreign key (delivery_id) references deliveries;
GO

IF NOT EXISTS (SELECT 1 FROM sys.objects WHERE name = 'FK9lo2w8nxtwsntv2adkdg6l0eh' AND parent_object_id = OBJECT_ID('dbo.shipping_tariffs'))
    alter table shipping_tariffs add constraint FK9lo2w8nxtwsntv2adkdg6l0eh foreign key (store_id) references stores;
GO

IF NOT EXISTS (SELECT 1 FROM sys.objects WHERE name = 'FKdrxi4d8299cemiy6ql0997vvw' AND parent_object_id = OBJECT_ID('dbo.store_inventories'))
    alter table store_inventories add constraint FKdrxi4d8299cemiy6ql0997vvw foreign key (product_id) references products;
GO

IF NOT EXISTS (SELECT 1 FROM sys.objects WHERE name = 'FKd4cgylj455v3070atnfjeevub' AND parent_object_id = OBJECT_ID('dbo.store_inventories'))
    alter table store_inventories add constraint FKd4cgylj455v3070atnfjeevub foreign key (store_id) references stores;
GO

IF NOT EXISTS (SELECT 1 FROM sys.objects WHERE name = 'FKh0866jgo4jxs6iopjwjcnvk3r' AND parent_object_id = OBJECT_ID('dbo.store_memberships'))
    alter table store_memberships add constraint FKh0866jgo4jxs6iopjwjcnvk3r foreign key (store_id) references stores;
GO

IF NOT EXISTS (SELECT 1 FROM sys.objects WHERE name = 'FKpteo6d92ivg71wm8lk6yipij7' AND parent_object_id = OBJECT_ID('dbo.store_memberships'))
    alter table store_memberships add constraint FKpteo6d92ivg71wm8lk6yipij7 foreign key (user_id) references users;
GO

IF NOT EXISTS (SELECT 1 FROM sys.objects WHERE name = 'FKfqq1f3ykutni846uh0cdgc3i6' AND parent_object_id = OBJECT_ID('dbo.stores'))
    alter table stores add constraint FKfqq1f3ykutni846uh0cdgc3i6 foreign key (commission_id) references commissions;
GO

IF NOT EXISTS (SELECT 1 FROM sys.objects WHERE name = 'FK62smc31fbgclsu56aa4y2hrxg' AND parent_object_id = OBJECT_ID('dbo.stores'))
    alter table stores add constraint FK62smc31fbgclsu56aa4y2hrxg foreign key (owner_id) references users;
GO

IF NOT EXISTS (SELECT 1 FROM sys.objects WHERE name = 'FKn7meq44uynr3k3qlxtt11qar2' AND parent_object_id = OBJECT_ID('dbo.stores'))
    alter table stores add constraint FKn7meq44uynr3k3qlxtt11qar2 foreign key (store_level_id) references store_levels;
GO

IF NOT EXISTS (SELECT 1 FROM sys.objects WHERE name = 'FKsld464t4acexwkgsr3ck7r5qq' AND parent_object_id = OBJECT_ID('dbo.style_values'))
    alter table style_values add constraint FKsld464t4acexwkgsr3ck7r5qq foreign key (style_id) references styles;
GO

IF NOT EXISTS (SELECT 1 FROM sys.objects WHERE name = 'FK2t27wgex7e0uguashq7kuhwoj' AND parent_object_id = OBJECT_ID('dbo.styles'))
    alter table styles add constraint FK2t27wgex7e0uguashq7kuhwoj foreign key (category_id) references categories;
GO

IF NOT EXISTS (SELECT 1 FROM sys.objects WHERE name = 'FKfyxndk58yiq2vpn0yd4m09kbt' AND parent_object_id = OBJECT_ID('dbo.transactions'))
    alter table transactions add constraint FKfyxndk58yiq2vpn0yd4m09kbt foreign key (order_id) references orders;
GO

IF NOT EXISTS (SELECT 1 FROM sys.objects WHERE name = 'FK76d23hljajshvpfgctye587jp' AND parent_object_id = OBJECT_ID('dbo.transactions'))
    alter table transactions add constraint FK76d23hljajshvpfgctye587jp foreign key (store_id) references stores;
GO

IF NOT EXISTS (SELECT 1 FROM sys.objects WHERE name = 'FKqwv7rmvc8va8rep7piikrojds' AND parent_object_id = OBJECT_ID('dbo.transactions'))
    alter table transactions add constraint FKqwv7rmvc8va8rep7piikrojds foreign key (user_id) references users;
GO

IF NOT EXISTS (SELECT 1 FROM sys.objects WHERE name = 'FKn2fisxyyu3l9wlch3ve2nocgp' AND parent_object_id = OBJECT_ID('dbo.user_addresses'))
    alter table user_addresses add constraint FKn2fisxyyu3l9wlch3ve2nocgp foreign key (user_id) references users;
GO

IF NOT EXISTS (SELECT 1 FROM sys.objects WHERE name = 'FKc0rr7ubt8yihgxt93tp33sgv8' AND parent_object_id = OBJECT_ID('dbo.user_follow_store'))
    alter table user_follow_store add constraint FKc0rr7ubt8yihgxt93tp33sgv8 foreign key (store_id) references stores;
GO

IF NOT EXISTS (SELECT 1 FROM sys.objects WHERE name = 'FKq4kua3133i76x4a3thd2795w8' AND parent_object_id = OBJECT_ID('dbo.user_follow_store'))
    alter table user_follow_store add constraint FKq4kua3133i76x4a3thd2795w8 foreign key (user_id) references users;
GO

IF NOT EXISTS (SELECT 1 FROM sys.objects WHERE name = 'FKhfh9dx7w3ubf1co1vdev94g3f' AND parent_object_id = OBJECT_ID('dbo.user_roles'))
    alter table user_roles add constraint FKhfh9dx7w3ubf1co1vdev94g3f foreign key (user_id) references users;
GO

IF NOT EXISTS (SELECT 1 FROM sys.objects WHERE name = 'FK7wra86jadsraitoewujbjj1pd' AND parent_object_id = OBJECT_ID('dbo.users'))
    alter table users add constraint FK7wra86jadsraitoewujbjj1pd foreign key (store_id) references stores;
GO

IF NOT EXISTS (SELECT 1 FROM sys.objects WHERE name = 'FKgus9g9avvhyh8vjmb3wt4fea0' AND parent_object_id = OBJECT_ID('dbo.users'))
    alter table users add constraint FKgus9g9avvhyh8vjmb3wt4fea0 foreign key (user_level_id) references user_levels;
GO

IF NOT EXISTS (SELECT 1 FROM sys.objects WHERE name = 'FKtiv6d285b5inctt0etfx751yu' AND parent_object_id = OBJECT_ID('dbo.vouchers'))
    alter table vouchers add constraint FKtiv6d285b5inctt0etfx751yu foreign key (store_id) references stores;
GO

-- Make ownership-scoped memberships explicit without enabling disabled accounts.
INSERT INTO dbo.store_memberships(store_id,user_id,access_role,active,joined_at)
SELECT u.store_id,u.id,'PARTNER',1,SYSDATETIME()
FROM dbo.users u JOIN dbo.stores s ON s.id=u.store_id
WHERE u.role IN ('STAFF','VENDOR','MANAGER') AND (s.owner_id IS NULL OR s.owner_id<>u.id)
AND NOT EXISTS(SELECT 1 FROM dbo.store_memberships m WHERE m.user_id=u.id AND m.store_id=u.store_id);
UPDATE dbo.users SET created_at=CAST(created_date AS datetime2), creation_time_known=0 WHERE created_at IS NULL AND created_date IS NOT NULL;
IF NOT EXISTS(SELECT 1 FROM sys.indexes WHERE name='ux_store_creation_request' AND object_id=OBJECT_ID('dbo.stores'))
    CREATE UNIQUE INDEX ux_store_creation_request ON dbo.stores(creation_request_key) WHERE creation_request_key IS NOT NULL;
IF NOT EXISTS(SELECT 1 FROM sys.indexes WHERE name='ux_transaction_request' AND object_id=OBJECT_ID('dbo.transactions'))
    CREATE UNIQUE INDEX ux_transaction_request ON dbo.transactions(request_key) WHERE request_key IS NOT NULL;
CREATE INDEX IX_jwt_session_user ON dbo.jwt_sessions(user_id);
CREATE INDEX IX_jwt_refresh_session ON dbo.jwt_refresh_tokens(session_id);
