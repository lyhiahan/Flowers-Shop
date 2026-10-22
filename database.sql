-- LEGACY RESET SCRIPT: drops existing tables. Not used by application startup.
-- New installations and upgrades use Flyway; see README.md before running SQL manually.
-- ============================================================================
-- HỆ THỐNG QUẢN LÝ BÁN HÀNG THEO MÔ HÌNH CHUỖI CỬA HÀNG HOA TƯƠI (FLOWER SHOP CHAIN)
-- HỆ QUẢN TRỊ CƠ SỞ DỮ LIỆU: MICROSOFT SQL SERVER
-- ============================================================================

IF NOT EXISTS (SELECT * FROM sys.databases WHERE name = 'RentalShop')
BEGIN
    CREATE DATABASE RentalShop;
END
GO

USE RentalShop;
GO

-- ============================================================================
-- 1. XÓA CÁC BẢNG NẾU ĐÃ TỒN TẠI (THEO THỨ TỰ RÀNG BUỘC KHÓA NGOẠI)
-- ============================================================================
DECLARE @sqlDropFk NVARCHAR(MAX) = N'';
SELECT @sqlDropFk += N'ALTER TABLE ' + QUOTENAME(s.name) + N'.' + QUOTENAME(t.name) + N' DROP CONSTRAINT ' + QUOTENAME(f.name) + N';' + CHAR(13)
FROM sys.foreign_keys f
INNER JOIN sys.tables t ON f.parent_object_id = t.object_id
INNER JOIN sys.schemas s ON t.schema_id = s.schema_id;
IF LEN(@sqlDropFk) > 0 EXEC sp_executesql @sqlDropFk;
GO

IF OBJECT_ID('dbo.transactions', 'U') IS NOT NULL DROP TABLE dbo.transactions;
IF OBJECT_ID('dbo.user_follow_store', 'U') IS NOT NULL DROP TABLE dbo.user_follow_store;
IF OBJECT_ID('dbo.recently_viewed', 'U') IS NOT NULL DROP TABLE dbo.recently_viewed;
IF OBJECT_ID('dbo.product_wishlists', 'U') IS NOT NULL DROP TABLE dbo.product_wishlists;
IF OBJECT_ID('dbo.reviews', 'U') IS NOT NULL DROP TABLE dbo.reviews;
IF OBJECT_ID('dbo.user_addresses', 'U') IS NOT NULL DROP TABLE dbo.user_addresses;
IF OBJECT_ID('dbo.activity_logs', 'U') IS NOT NULL DROP TABLE dbo.activity_logs;
IF OBJECT_ID('dbo.notifications', 'U') IS NOT NULL DROP TABLE dbo.notifications;
IF OBJECT_ID('dbo.inventory_transfer_requests', 'U') IS NOT NULL DROP TABLE dbo.inventory_transfer_requests;
IF OBJECT_ID('dbo.payments', 'U') IS NOT NULL DROP TABLE dbo.payments;
IF OBJECT_ID('dbo.order_details', 'U') IS NOT NULL DROP TABLE dbo.order_details;
IF OBJECT_ID('dbo.orders', 'U') IS NOT NULL DROP TABLE dbo.orders;
IF OBJECT_ID('dbo.vouchers', 'U') IS NOT NULL DROP TABLE dbo.vouchers;
IF OBJECT_ID('dbo.cart_items', 'U') IS NOT NULL DROP TABLE dbo.cart_items;
IF OBJECT_ID('dbo.carts', 'U') IS NOT NULL DROP TABLE dbo.carts;
IF OBJECT_ID('dbo.store_inventories', 'U') IS NOT NULL DROP TABLE dbo.store_inventories;
IF OBJECT_ID('dbo.product_images', 'U') IS NOT NULL DROP TABLE dbo.product_images;
IF OBJECT_ID('dbo.products', 'U') IS NOT NULL DROP TABLE dbo.products;
IF OBJECT_ID('dbo.style_values', 'U') IS NOT NULL DROP TABLE dbo.style_values;
IF OBJECT_ID('dbo.styles', 'U') IS NOT NULL DROP TABLE dbo.styles;
IF OBJECT_ID('dbo.promotions', 'U') IS NOT NULL DROP TABLE dbo.promotions;
IF OBJECT_ID('dbo.deliveries', 'U') IS NOT NULL DROP TABLE dbo.deliveries;
IF OBJECT_ID('dbo.categories', 'U') IS NOT NULL DROP TABLE dbo.categories;
IF OBJECT_ID('dbo.otp_verifications', 'U') IS NOT NULL DROP TABLE dbo.otp_verifications;
IF OBJECT_ID('dbo.users', 'U') IS NOT NULL DROP TABLE dbo.users;
IF OBJECT_ID('dbo.user_levels', 'U') IS NOT NULL DROP TABLE dbo.user_levels;
IF OBJECT_ID('dbo.stores', 'U') IS NOT NULL DROP TABLE dbo.stores;
IF OBJECT_ID('dbo.store_levels', 'U') IS NOT NULL DROP TABLE dbo.store_levels;
IF OBJECT_ID('dbo.commissions', 'U') IS NOT NULL DROP TABLE dbo.commissions;
GO

-- ============================================================================
-- 2. TẠO CÁC BẢNG DỮ LIỆU (CREATE TABLES)
-- ============================================================================

-- Bảng: Cấp bậc thành viên (User Levels)
CREATE TABLE dbo.user_levels (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    name NVARCHAR(32) NOT NULL UNIQUE,
    min_point INT NOT NULL UNIQUE,
    discount DECIMAL(5,2) NOT NULL DEFAULT 0,
    is_deleted BIT NOT NULL DEFAULT 0,
    created_at DATETIME DEFAULT GETDATE(),
    updated_at DATETIME DEFAULT GETDATE()
);
GO

-- Bảng: Cấp bậc cửa hàng (Store Levels)
CREATE TABLE dbo.store_levels (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    name NVARCHAR(32) NOT NULL UNIQUE,
    min_point INT NOT NULL UNIQUE,
    discount DECIMAL(5,2) NOT NULL DEFAULT 0,
    is_deleted BIT NOT NULL DEFAULT 0,
    created_at DATETIME DEFAULT GETDATE(),
    updated_at DATETIME DEFAULT GETDATE()
);
GO

-- Bảng: Chính sách chiết khấu / hoa hồng (Commissions)
CREATE TABLE dbo.commissions (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    name NVARCHAR(32) NOT NULL UNIQUE,
    cost DECIMAL(18,2) NOT NULL,
    description NVARCHAR(3000),
    is_deleted BIT NOT NULL DEFAULT 0,
    created_at DATETIME DEFAULT GETDATE(),
    updated_at DATETIME DEFAULT GETDATE()
);
GO

-- Bảng: Chuỗi chi nhánh cửa hàng hoa (Stores)
CREATE TABLE dbo.stores (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    name NVARCHAR(150) NOT NULL,
    address NVARCHAR(255) NOT NULL,
    phone VARCHAR(20) NULL,
    latitude FLOAT NULL,
    longitude FLOAT NULL,
    status BIT NULL DEFAULT 1,
    owner_id BIGINT NULL,
    bio NVARCHAR(1000) NULL,
    avatar NVARCHAR(500) NULL,
    cover NVARCHAR(500) NULL,
    featured_images NVARCHAR(MAX) NULL,
    commission_id BIGINT NULL,
    store_level_id BIGINT NULL,
    is_open BIT NOT NULL DEFAULT 1,
    point INT NOT NULL DEFAULT 0,
    rating FLOAT NOT NULL DEFAULT 5.0,
    e_wallet DECIMAL(18,2) NOT NULL DEFAULT 0,
    CONSTRAINT FK_stores_commissions FOREIGN KEY (commission_id) REFERENCES dbo.commissions(id),
    CONSTRAINT FK_stores_store_levels FOREIGN KEY (store_level_id) REFERENCES dbo.store_levels(id)
);
GO

-- Bảng: Người dùng (Users: Khách hàng, Quản lý, Admin, Nhân viên giao hàng)
CREATE TABLE dbo.users (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    full_name NVARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    phone VARCHAR(20) NULL,
    address NVARCHAR(255) NULL,
    avatar_url VARCHAR(255) NULL,
    cover NVARCHAR(500) NULL,
    role VARCHAR(20) NOT NULL, -- 'ADMIN', 'MANAGER', 'CUSTOMER', 'SHIPPER'
    is_active BIT NULL DEFAULT 0,
    created_date DATE NULL,
    store_id BIGINT NULL,
    point INT NOT NULL DEFAULT 0,
    e_wallet DECIMAL(18,2) NOT NULL DEFAULT 0,
    is_email_active BIT NOT NULL DEFAULT 1,
    otp_code VARCHAR(10) NULL,
    otp_expiry DATETIME NULL,
    user_level_id BIGINT NULL,
    CONSTRAINT FK_users_stores FOREIGN KEY (store_id) REFERENCES dbo.stores(id) ON DELETE SET NULL,
    CONSTRAINT FK_users_user_levels FOREIGN KEY (user_level_id) REFERENCES dbo.user_levels(id)
);
GO

-- Bảng: Mã xác thực OTP (OtpVerification)
CREATE TABLE dbo.otp_verifications (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    code VARCHAR(6) NOT NULL,
    type VARCHAR(30) NOT NULL,
    expired_at DATETIME2 NOT NULL,
    is_used BIT NULL DEFAULT 0,
    created_at DATETIME2 NULL,
    CONSTRAINT FK_otp_verifications_users FOREIGN KEY (user_id) REFERENCES dbo.users(id) ON DELETE CASCADE
);
GO

-- Bảng: Đơn vị vận chuyển (Deliveries)
CREATE TABLE dbo.deliveries (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    name NVARCHAR(100) NOT NULL UNIQUE,
    description NVARCHAR(1000),
    price DECIMAL(18,2) NOT NULL DEFAULT 0,
    is_active BIT NOT NULL DEFAULT 1,
    is_deleted BIT NOT NULL DEFAULT 0,
    created_at DATETIME DEFAULT GETDATE(),
    updated_at DATETIME DEFAULT GETDATE()
);
GO

-- Bảng: Danh mục hoa tươi (Categories) - Hỗ trợ phân cấp cha/con
CREATE TABLE dbo.categories (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    name NVARCHAR(100) NOT NULL UNIQUE,
    description NVARCHAR(MAX) NULL,
    parent_id BIGINT NULL,
    CONSTRAINT FK_categories_parent FOREIGN KEY (parent_id) REFERENCES dbo.categories(id)
);
GO

-- Bảng: Phong cách & Thiết kế hoa (Styles & Style Values)
CREATE TABLE dbo.styles (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    name NVARCHAR(32) NOT NULL UNIQUE,
    category_id BIGINT NULL,
    is_deleted BIT NOT NULL DEFAULT 0,
    created_at DATETIME DEFAULT GETDATE(),
    updated_at DATETIME DEFAULT GETDATE(),
    CONSTRAINT FK_styles_categories FOREIGN KEY (category_id) REFERENCES dbo.categories(id) ON DELETE SET NULL
);
GO

CREATE TABLE dbo.style_values (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    name NVARCHAR(32) NOT NULL,
    style_id BIGINT NOT NULL,
    is_deleted BIT NOT NULL DEFAULT 0,
    created_at DATETIME DEFAULT GETDATE(),
    updated_at DATETIME DEFAULT GETDATE(),
    CONSTRAINT FK_style_values_styles FOREIGN KEY (style_id) REFERENCES dbo.styles(id) ON DELETE CASCADE
);
GO

-- Bảng: Sản phẩm hoa tươi & Phụ kiện sự kiện (Products)
CREATE TABLE dbo.products (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    name NVARCHAR(200) NOT NULL,
    description NVARCHAR(MAX) NULL,
    import_price NUMERIC(38,2) NOT NULL,
    price_buy NUMERIC(38,2) NOT NULL,
    price_rent_per_day NUMERIC(38,2) NOT NULL,
    deposit_fee NUMERIC(38,2) NOT NULL,
    category_id BIGINT NOT NULL,
    sold INT NOT NULL DEFAULT 0,
    rating FLOAT NOT NULL DEFAULT 5.0,
    view_count INT NOT NULL DEFAULT 0,
    store_id BIGINT NULL,
    is_active BIT NOT NULL DEFAULT 1,
    is_selling BIT NOT NULL DEFAULT 1,
    promotional_price DECIMAL(18,2) NULL,
    image_url NVARCHAR(500) NULL,
    created_at DATETIME DEFAULT GETDATE(),
    CONSTRAINT FK_products_categories FOREIGN KEY (category_id) REFERENCES dbo.categories(id) ON DELETE CASCADE,
    CONSTRAINT FK_products_stores FOREIGN KEY (store_id) REFERENCES dbo.stores(id) ON DELETE SET NULL
);
GO

-- Bảng: Hình ảnh hoa tươi (ProductImages)
CREATE TABLE dbo.product_images (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    image_url VARCHAR(255) NOT NULL,
    product_id BIGINT NOT NULL,
    is_main BIT NOT NULL DEFAULT 0,
    CONSTRAINT FK_product_images_products FOREIGN KEY (product_id) REFERENCES dbo.products(id) ON DELETE CASCADE
);
GO

-- Bảng: Tồn kho hoa tươi theo chi nhánh (StoreInventories)
CREATE TABLE dbo.store_inventories (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    store_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INT NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT UQ_store_product UNIQUE (store_id, product_id),
    CONSTRAINT FK_inventories_stores FOREIGN KEY (store_id) REFERENCES dbo.stores(id) ON DELETE CASCADE,
    CONSTRAINT FK_inventories_products FOREIGN KEY (product_id) REFERENCES dbo.products(id) ON DELETE CASCADE
);
GO

-- Bảng: Giỏ hàng (Carts)
CREATE TABLE dbo.carts (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    CONSTRAINT FK_carts_users FOREIGN KEY (user_id) REFERENCES dbo.users(id) ON DELETE CASCADE
);
GO

-- Bảng: Chi tiết giỏ hàng (CartItems)
CREATE TABLE dbo.cart_items (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    cart_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    store_id BIGINT NOT NULL,
    order_type VARCHAR(10) NOT NULL, -- 'BUY' hoặc 'RENT'
    quantity INT NOT NULL DEFAULT 1,
    rent_start_date DATE NULL,
    rent_end_date DATE NULL,
    delivery_date DATE NULL,
    greeting_message NVARCHAR(500) NULL,
    CONSTRAINT FK_cart_items_carts FOREIGN KEY (cart_id) REFERENCES dbo.carts(id) ON DELETE CASCADE,
    CONSTRAINT FK_cart_items_products FOREIGN KEY (product_id) REFERENCES dbo.products(id),
    CONSTRAINT FK_cart_items_stores FOREIGN KEY (store_id) REFERENCES dbo.stores(id)
);
GO

-- Bảng: Mã giảm giá & Voucher (Vouchers)
CREATE TABLE dbo.vouchers (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name NVARCHAR(150) NOT NULL,
    discount_type VARCHAR(20) NOT NULL, -- PERCENT, FIXED_AMOUNT, FREE_SHIP
    discount_value DECIMAL(18,2) NOT NULL,
    min_order_value DECIMAL(18,2) DEFAULT 0,
    max_discount_amount DECIMAL(18,2) NULL,
    store_id BIGINT NULL,
    quantity INT NOT NULL DEFAULT 100,
    used_count INT NOT NULL DEFAULT 0,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    is_active BIT NOT NULL DEFAULT 1,
    created_at DATETIME DEFAULT GETDATE(),
    CONSTRAINT FK_vouchers_stores FOREIGN KEY (store_id) REFERENCES dbo.stores(id) ON DELETE SET NULL
);
GO

-- Bảng: Sổ địa chỉ người dùng (UserAddresses)
CREATE TABLE dbo.user_addresses (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    recipient_name NVARCHAR(100) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    address_detail NVARCHAR(255) NOT NULL,
    is_default BIT NOT NULL DEFAULT 0,
    created_at DATETIME DEFAULT GETDATE(),
    CONSTRAINT FK_user_addresses_users FOREIGN KEY (user_id) REFERENCES dbo.users(id) ON DELETE CASCADE
);
GO

-- Bảng: Đơn đặt hoa (Orders)
CREATE TABLE dbo.orders (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    pickup_store_id BIGINT NOT NULL,
    return_store_id BIGINT NULL,
    order_type VARCHAR(10) NOT NULL, -- 'BUY', 'RENT'
    status VARCHAR(20) NOT NULL,     -- 'PENDING', 'CONFIRMED', 'PREPARING', 'DELIVERING', 'COMPLETED', 'CANCELLED'
    total_amount NUMERIC(38,2) NULL,
    total_deposit NUMERIC(38,2) NULL,
    fine_amount NUMERIC(38,2) NULL,
    delivery_date DATE NULL,
    greeting_message NVARCHAR(500) NULL,
    recipient_name NVARCHAR(100) NULL,
    recipient_phone VARCHAR(20) NULL,
    recipient_address NVARCHAR(255) NULL,
    created_at DATETIME2 NULL,
    version BIGINT NOT NULL DEFAULT 0,
    shipper_id BIGINT NULL,
    voucher_id BIGINT NULL,
    discount_amount DECIMAL(18,2) NOT NULL DEFAULT 0,
    payment_method VARCHAR(20) NOT NULL DEFAULT 'COD',
    payment_status VARCHAR(20) NOT NULL DEFAULT 'UNPAID',
    CONSTRAINT FK_orders_users FOREIGN KEY (user_id) REFERENCES dbo.users(id),
    CONSTRAINT FK_orders_pickup_stores FOREIGN KEY (pickup_store_id) REFERENCES dbo.stores(id),
    CONSTRAINT FK_orders_return_stores FOREIGN KEY (return_store_id) REFERENCES dbo.stores(id),
    CONSTRAINT FK_orders_shippers FOREIGN KEY (shipper_id) REFERENCES dbo.users(id),
    CONSTRAINT FK_orders_vouchers FOREIGN KEY (voucher_id) REFERENCES dbo.vouchers(id)
);
GO

-- Bảng: Chi tiết đơn hàng hoa (OrderDetails)
CREATE TABLE dbo.order_details (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    unit_price NUMERIC(38,2) NOT NULL,
    order_type VARCHAR(10) NULL,
    deposit_fee NUMERIC(38,2) NULL,
    rent_start_date DATE NULL,
    rent_end_date DATE NULL,
    actual_return_date DATE NULL,
    CONSTRAINT FK_order_details_orders FOREIGN KEY (order_id) REFERENCES dbo.orders(id) ON DELETE CASCADE,
    CONSTRAINT FK_order_details_products FOREIGN KEY (product_id) REFERENCES dbo.products(id)
);
GO

-- Bảng: Thanh toán (Payments)
CREATE TABLE dbo.payments (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    order_id BIGINT NOT NULL,
    method VARCHAR(20) NOT NULL, -- 'COD', 'BANK_TRANSFER', 'VNPAY', 'MOMO'
    amount NUMERIC(38,2) NOT NULL,
    status VARCHAR(20) NOT NULL, -- 'PENDING', 'SUCCESS', 'FAILED'
    paid_at DATETIME2 NULL,
    CONSTRAINT FK_payments_orders FOREIGN KEY (order_id) REFERENCES dbo.orders(id) ON DELETE CASCADE
);
GO

-- Bảng: Đánh giá & bình luận sản phẩm (Reviews)
CREATE TABLE dbo.reviews (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    store_id BIGINT NULL,
    order_id BIGINT NULL,
    content NVARCHAR(1000) NOT NULL,
    rating INT NOT NULL CHECK (rating >= 1 AND rating <= 5),
    stars INT NOT NULL CHECK (stars >= 1 AND stars <= 5),
    image_url NVARCHAR(500) NULL,
    created_at DATETIME DEFAULT GETDATE(),
    updated_at DATETIME DEFAULT GETDATE(),
    CONSTRAINT FK_reviews_users FOREIGN KEY (user_id) REFERENCES dbo.users(id),
    CONSTRAINT FK_reviews_products FOREIGN KEY (product_id) REFERENCES dbo.products(id) ON DELETE CASCADE
);
GO

-- Bảng: Danh sách yêu thích sản phẩm (ProductWishlists)
CREATE TABLE dbo.product_wishlists (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    created_at DATETIME DEFAULT GETDATE(),
    CONSTRAINT uq_user_wishlist UNIQUE (user_id, product_id),
    CONSTRAINT FK_wishlist_users FOREIGN KEY (user_id) REFERENCES dbo.users(id) ON DELETE CASCADE,
    CONSTRAINT FK_wishlist_products FOREIGN KEY (product_id) REFERENCES dbo.products(id) ON DELETE CASCADE
);
GO

-- Bảng: Sản phẩm vừa xem gần đây (RecentlyViewed)
CREATE TABLE dbo.recently_viewed (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    viewed_at DATETIME DEFAULT GETDATE(),
    CONSTRAINT FK_recently_viewed_users FOREIGN KEY (user_id) REFERENCES dbo.users(id) ON DELETE CASCADE,
    CONSTRAINT FK_recently_viewed_products FOREIGN KEY (product_id) REFERENCES dbo.products(id) ON DELETE CASCADE
);
GO

-- Bảng: Theo dõi cửa hàng (UserFollowStore)
CREATE TABLE dbo.user_follow_store (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    store_id BIGINT NOT NULL,
    created_at DATETIME DEFAULT GETDATE(),
    CONSTRAINT uq_user_follow_store UNIQUE (user_id, store_id),
    CONSTRAINT FK_follow_users FOREIGN KEY (user_id) REFERENCES dbo.users(id) ON DELETE CASCADE,
    CONSTRAINT FK_follow_stores FOREIGN KEY (store_id) REFERENCES dbo.stores(id) ON DELETE CASCADE
);
GO

-- Bảng: Lịch sử biến động số dư / Giao dịch ví (Transactions)
CREATE TABLE dbo.transactions (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id BIGINT NULL,
    store_id BIGINT NULL,
    is_up BIT NOT NULL DEFAULT 1,
    amount DECIMAL(18,2) NOT NULL,
    type VARCHAR(30) DEFAULT 'ORDER_PAYMENT',
    description NVARCHAR(500),
    status VARCHAR(20) DEFAULT 'COMPLETED',
    created_at DATETIME DEFAULT GETDATE(),
    CONSTRAINT FK_transactions_users FOREIGN KEY (user_id) REFERENCES dbo.users(id),
    CONSTRAINT FK_transactions_stores FOREIGN KEY (store_id) REFERENCES dbo.stores(id)
);
GO

-- Bảng: Điều phối hoa giữa các chi nhánh (InventoryTransferRequests)
CREATE TABLE dbo.inventory_transfer_requests (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    from_store_id BIGINT NOT NULL,
    to_store_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at DATETIME2 NULL,
    CONSTRAINT FK_transfer_from_stores FOREIGN KEY (from_store_id) REFERENCES dbo.stores(id),
    CONSTRAINT FK_transfer_to_stores FOREIGN KEY (to_store_id) REFERENCES dbo.stores(id),
    CONSTRAINT FK_transfer_products FOREIGN KEY (product_id) REFERENCES dbo.products(id)
);
GO

-- Bảng: Mã khuyến mãi chung (Promotions)
CREATE TABLE dbo.promotions (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    code VARCHAR(30) NOT NULL UNIQUE,
    description NVARCHAR(255) NULL,
    discount_percentage INT NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    is_active BIT NULL DEFAULT 1
);
GO

-- ============================================================================
-- 3. NẠP DỮ LIỆU MẪU CHUỖI CỬA HÀNG HOA TƯƠI (SAMPLE DATA SEEDING)
-- ============================================================================

-- 3.1. Chuỗi chi nhánh cửa hàng hoa tươi tại TP.HCM
INSERT INTO dbo.stores (name, address, phone, latitude, longitude, status, is_open, rating, e_wallet) VALUES 
(N'Chi Nhánh Bến Thành (Quận 1)', N'123 Lê Lợi, Phường Bến Thành, Quận 1, TP.HCM', '0281111222', 10.7725, 106.6980, 1, 1, 4.9, 15500000),
(N'Chi Nhánh Phú Mỹ Hưng (Quận 7)', N'456 Nguyễn Thị Thập, Tân Hưng, Quận 7, TP.HCM', '0283333444', 10.7382, 106.7085, 1, 1, 4.8, 8200000),
(N'Chi Nhánh Làng Đại Học (TP. Thủ Đức)', N'1 Võ Văn Ngân, Linh Chiểu, TP. Thủ Đức, TP.HCM', '0285555666', 10.8505, 106.7719, 1, 1, 4.7, 4300000);
GO

-- 3.2. Cấp bậc thành viên & Cửa hàng
INSERT INTO dbo.user_levels (name, min_point, discount) VALUES 
(N'Thành Viên Mới', 0, 0.0),
(N'Thành Viên Bạc', 100, 3.0),
(N'Thành Viên Vàng', 500, 5.0),
(N'Thành Viên Kim Cương', 1000, 10.0);
GO

INSERT INTO dbo.store_levels (name, min_point, discount) VALUES 
(N'Cửa Hàng Chuẩn', 0, 0.0),
(N'Cửa Hàng Uy Tín', 200, 2.0),
(N'Cửa Hàng Xuất Sắc', 1000, 5.0);
GO

-- 3.3. Đơn vị vận chuyển
INSERT INTO dbo.deliveries (name, description, price, is_active) VALUES 
(N'Giao Hỏa Tốc (2 Giờ)', N'Giao hàng nhanh bằng xe máy trong 2 giờ bảo quản hoa lạnh', 35000, 1),
(N'Giao Tiêu Chuẩn Trong Ngày', N'Giao hoa tươi trong ngày theo khung giờ hẹn trước', 20000, 1);
GO

-- 3.4. Tài khoản mẫu (Mật khẩu: 123456)
INSERT INTO dbo.users (username, password, full_name, email, phone, address, role, is_active, is_email_active, created_date, store_id) VALUES 
('admin', '$2a$10$MMgWoixo7KjB5HrbStq9g.NkNv9zWbVpdSlBmPjrncPse25LBqh82', N'Quản Trị Viên Toàn Hệ Thống', 'admin@rentalshop.vn', '0901111222', N'TP. Hồ Chí Minh', 'ADMIN', 1, 1, CAST(GETDATE() AS DATE), NULL),
('manager_q1', '$2a$10$MMgWoixo7KjB5HrbStq9g.NkNv9zWbVpdSlBmPjrncPse25LBqh82', N'Nguyễn Quản Lý Cửa Hàng Q1', 'manager.q1@rentalshop.vn', '0902222333', N'Quận 1, TP.HCM', 'MANAGER', 1, 1, CAST(GETDATE() AS DATE), 1),
('khachhang', '$2a$10$MMgWoixo7KjB5HrbStq9g.NkNv9zWbVpdSlBmPjrncPse25LBqh82', N'Lê Khách Hàng Yêu Hoa', 'khachhang@gmail.com', '0909999888', N'TP. Hồ Chí Minh', 'CUSTOMER', 1, 1, CAST(GETDATE() AS DATE), NULL),
('shipper_nhanh', '$2a$10$MMgWoixo7KjB5HrbStq9g.NkNv9zWbVpdSlBmPjrncPse25LBqh82', N'Trần Giao Hàng Nhanh', 'shipper@rentalshop.vn', '0907777888', N'TP. Hồ Chí Minh', 'SHIPPER', 1, 1, CAST(GETDATE() AS DATE), NULL);
GO

-- 3.5. Danh mục hoa tươi (phân cấp: 3 danh mục gốc + danh mục con)
-- Danh mục gốc (parent_id = NULL)
INSERT INTO dbo.categories (name, description, parent_id) VALUES
(N'Hoa Tươi Sự Kiện & Chúc Mừng',   N'Bộ sưu tập hoa chúc mừng, sự kiện, khai trương và sinh nhật', NULL),
(N'Hoa Tình Yêu & Hoa Chậu',          N'Hoa hồng lãng mạn, hoa cưới và lan hồ điệp cao cấp', NULL),
(N'Hoa Trang Trí & Không Gian Sống', N'Hoa tươi trang trí nội thất, văn phòng và không gian sống', NULL);
GO

-- Danh mục con của id 1 (Hoa Tươi Sự Kiện & Chúc Mừng)
DECLARE @id1 BIGINT = (SELECT id FROM categories WHERE name = N'Hoa Tươi Sự Kiện & Chúc Mừng');
DECLARE @id2 BIGINT = (SELECT id FROM categories WHERE name = N'Hoa Tình Yêu & Hoa Chậu');
DECLARE @id3 BIGINT = (SELECT id FROM categories WHERE name = N'Hoa Trang Trí & Không Gian Sống');
INSERT INTO dbo.categories (name, description, parent_id) VALUES
(N'Hoa Sinh Nhật',          N'Bó hoa và giỏ hoa tươi chúc mừng sinh nhật',            @id1),
(N'Hoa Khai Trương',        N'Lẵng hoa chúc mừng khai trương cửa hàng, công ty',      @id1),
(N'Hoa Tốt Nghiệp',        N'Hoa chúc mừng tốt nghiệp và thành đạt',                  @id1),
(N'Hoa Hồng Tình Yêu',     N'Hoa hồng đỏ, hồng phấn cho Valentine và kỷ niệm',       @id2),
(N'Lan Hồ Điệp Cao Cấp',   N'Lan hồ điệp sang trọng dành làm quà tặng cao cấp',      @id2),
(N'Hoa Để Bàn & Nội Thất', N'Hoa cắm bình trang trí bàn làm việc và nội thất',        @id3),
(N'Hoa Văn Phòng',         N'Hoa tươi trang trí văn phòng, hành lang và sảnh',        @id3);
GO

-- 3.6. Sản phẩm hoa tươi (category_id trỏ về danh mục con)
-- category 4=Hoa Sinh Nhật, 5=Hoa Khai Trương, 7=Hoa Hồng Tình Yêu, 8=Lan Hồ Điệp, 9=Hoa Để Bàn, 10=Hoa Văn Phòng
DECLARE @catSinhNhat   BIGINT = (SELECT id FROM categories WHERE name = N'Hoa Sinh Nhật');
DECLARE @catKhaiTruong BIGINT = (SELECT id FROM categories WHERE name = N'Hoa Khai Trương');
DECLARE @catHoaHong    BIGINT = (SELECT id FROM categories WHERE name = N'Hoa Hồng Tình Yêu');
DECLARE @catLan        BIGINT = (SELECT id FROM categories WHERE name = N'Lan Hồ Điệp Cao Cấp');
DECLARE @catDeBan      BIGINT = (SELECT id FROM categories WHERE name = N'Hoa Để Bàn & Nội Thất');
DECLARE @catVanPhong   BIGINT = (SELECT id FROM categories WHERE name = N'Hoa Văn Phòng');
INSERT INTO dbo.products (name, description, import_price, price_buy, price_rent_per_day, deposit_fee, category_id, image_url, store_id, sold, rating, view_count, is_active, is_selling) VALUES
(N'Bó Hoa Hồng Đỏ',   N'Gồm 19 bông hồng đỏ Ecuador nhập khẩu phối hoa baby trắng Hà Lan, bọc giấy gói cao cấp phong cách Hàn Quốc.',  350000.00, 650000.00,  0.00, 0.00, @catSinhNhat,   'https://images.unsplash.com/photo-1548094878-84ced0f68b08?w=800', 1, 45, 4.9, 320, 1, 1),
(N'Bó Hoa Tulip Hồng',   N'15 bông tulip Hà Lan màu hồng pastel và trắng ngọc, biểu trưng cho tình yêu chân thành và trang nhã.',            480000.00, 850000.00,  0.00, 0.00, @catSinhNhat,   'https://images.unsplash.com/photo-1520763185298-1b434c919102?w=800', 1, 38, 4.8, 280, 1, 1),
(N'Bó Hoa Hướng Dương Tana',    N'5 bông hướng dương đà lạt loại 1 phối hoa cúc tana và lá bạc nhập khẩu, mang ý nghĩa may mắn và thành công.',   220000.00, 420000.00,  0.00, 0.00, @catSinhNhat,   'https://images.unsplash.com/photo-1597848212624-a19eb35e2651?w=800', 2, 29, 4.7, 195, 1, 1),
(N'Giỏ Hoa Cẩm Tú Cầu',        N'Giỏ mây vintage kết hợp hoa cẩm tú cầu xanh lam, hoa hồng kem và hoa cát tường dịu dàng.',                         420000.00, 750000.00,  0.00, 0.00, @catSinhNhat,   'https://images.unsplash.com/photo-1508610048659-a06b669e3321?w=800', 2, 24, 4.9, 160, 1, 1),
(N'Hộp Hoa Hồng Juliet', N'Hộp hoa mica trong suốt cao cấp phối hoa hồng phấn Juliet và baby khô cao cấp lưu giữ vẻ đẹp lâu dài.',         520000.00, 920000.00,  0.00, 0.00, @catHoaHong,    'https://images.unsplash.com/photo-1561181286-d3fee7d55364?w=800', 1, 19, 4.8, 140, 1, 1),
(N'Kệ Hoa Khai Trương 2 Tầng',       N'Kệ hoa 2 tầng cao 1.8m phối hoa hồng môn đỏ, hướng dương và hoa đồng tiền rực rỡ mang tài lộc dồi dào.',         950000.00, 1650000.00, 0.00, 0.00, @catKhaiTruong, 'https://images.unsplash.com/photo-1533616688419-b7a585564566?w=800', 1, 15, 5.0, 210, 1, 1),
(N'Kệ Hoa Khai Trương Tone Cam',     N'Thiết kế hiện đại tone màu cam vàng hoàng kim, chúc mừng đối tác kinh doanh phát đạt thịnh vượng.',               800000.00, 1350000.00, 0.00, 0.00, @catKhaiTruong, 'https://images.unsplash.com/photo-1527061011665-3652c757a4d4?w=800', 3, 12, 4.6, 115, 1, 1),
(N'Bó Hoa Cưới Calla Lily', N'Hoa rum Calla Lily trắng nhập khẩu kết hợp nơ lụa voan cao cấp, tôn vinh vẻ đẹp thuần khiết của cô dâu.',         650000.00, 1150000.00, 0.00, 0.00, @catHoaHong,    'https://images.unsplash.com/photo-1545232979-8bf68ee9b1af?w=800', 1, 16, 5.0, 185, 1, 1),
(N'Chậu Lan Hồ Điệp Tím',       N'Chậu sứ Bát Tràng cao cấp cắm 5 cành lan hồ điệp tím viền trắng cánh dày, hoa bền tươi trên 1 tháng.',           1200000.00, 2100000.00, 0.00, 0.00, @catLan,        'https://images.unsplash.com/photo-1566838318109-a8bffb91d082?w=800', 2,  8, 4.9,  90, 1, 1),
(N'Lọ Hoa Để Bàn Hoa Hồng Nhung Đỏ',        N'Bình thủy tinh cắm hoa hồng nhung đỏ và hoa limonium tím nhỏ xinh, trang trí bàn làm việc sang trọng.',            180000.00, 320000.00,  0.00, 0.00, @catDeBan,      'https://images.unsplash.com/photo-1582794543462-0d7922e50cf5?w=800', 3,  7, 4.8,  88, 1, 1),
(N'Chậu Hoa Văn Phòng Bình An Xanh Mướt',    N'Chậu hoa tươi hỗn hợp xanh mướt trang trí văn phòng, hành lang, mang lại không khí trong lành và thanh thản.',  280000.00, 490000.00,  0.00, 0.00, @catVanPhong,   'https://images.unsplash.com/photo-1532712938310-34cb3982ef74?w=800', 3,  5, 4.7,  75, 1, 1);
GO

-- 3.7. Tồn kho hoa tươi sẵn sàng tại 3 chi nhánh
INSERT INTO dbo.store_inventories (store_id, product_id, quantity, version) VALUES 
-- Chi nhánh Quận 1 (Bến Thành)
(1, 1, 20, 0), (1, 2, 15, 0), (1, 3, 25, 0), (1, 4, 12, 0), (1, 5, 10, 0), (1, 6, 8, 0), (1, 7, 10, 0), (1, 8, 6, 0), (1, 9, 8, 0), (1, 10, 3, 0), (1, 11, 6, 0),
-- Chi nhánh Quận 7 (Phú Mỹ Hưng)
(2, 1, 18, 0), (2, 2, 12, 0), (2, 3, 20, 0), (2, 4, 15, 0), (2, 5, 8, 0), (2, 6, 6, 0), (2, 7, 8, 0), (2, 8, 5, 0), (2, 9, 10, 0), (2, 10, 2, 0), (2, 11, 4, 0),
-- Chi nhánh TP. Thủ Đức (Làng Đại Học)
(3, 1, 15, 0), (3, 2, 10, 0), (3, 3, 30, 0), (3, 4, 10, 0), (3, 5, 6, 0), (3, 6, 5, 0), (3, 7, 6, 0), (3, 8, 4, 0), (3, 9, 5, 0), (3, 10, 2, 0), (3, 11, 4, 0);
GO

-- 3.8. Giỏ hàng mẫu cho tài khoản khachhang
INSERT INTO dbo.carts (user_id) VALUES (3);
GO

-- 3.9. Mã giảm giá / Voucher mẫu
INSERT INTO dbo.vouchers (code, name, discount_type, discount_value, min_order_value, max_discount_amount, quantity, used_count, start_date, end_date, is_active) VALUES 
('HOATUOI20K', N'Giảm 20K cho đơn từ 200K', 'FIXED_AMOUNT', 20000, 200000, 20000, 100, 5, CAST(GETDATE() AS DATE), DATEADD(month, 3, GETDATE()), 1),
('TRIAN10', N'Giảm 10% tri ân khách hàng', 'PERCENT', 10, 300000, 50000, 50, 2, CAST(GETDATE() AS DATE), DATEADD(month, 3, GETDATE()), 1),
('FREESHIP', N'Miễn phí vận chuyển toàn quốc', 'FREE_SHIP', 35000, 150000, 35000, 200, 12, CAST(GETDATE() AS DATE), DATEADD(month, 3, GETDATE()), 1);
GO

-- 3.10. Đơn hàng mẫu đã giao thành công và đánh giá sản phẩm mẫu cho tài khoản khachhang
INSERT INTO dbo.orders (user_id, pickup_store_id, order_type, status, total_amount, recipient_name, recipient_phone, recipient_address, created_at, payment_method, payment_status)
VALUES (3, 1, 'BUY', 'DELIVERED', 650000.00, N'Gia Hân Khách Hàng', '0912345678', N'123 Lê Lợi, P. Bến Nghé, Quận 1, TP.HCM', SYSDATETIME(), 'COD', 'PAID');

DECLARE @sampleOrderId BIGINT = SCOPE_IDENTITY();

INSERT INTO dbo.order_details (order_id, product_id, quantity, unit_price, order_type)
VALUES (@sampleOrderId, 1, 1, 650000.00, 'BUY');

INSERT INTO dbo.reviews (user_id, product_id, store_id, order_id, content, rating, stars, created_at)
VALUES (3, 1, 1, @sampleOrderId, N'Hoa hồng đỏ Ecuador nhận được rất tươi, cánh hoa nở to đều và thơm dịu. Đóng gói cẩn thận có thiệp viết tay rất chỉn chu. Shipper giao hoa đúng giờ hẹn, phục vụ rất nhiệt tình!', 5, 5, GETDATE());
GO

