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
IF OBJECT_ID('dbo.activity_logs', 'U') IS NOT NULL DROP TABLE dbo.activity_logs;
IF OBJECT_ID('dbo.notifications', 'U') IS NOT NULL DROP TABLE dbo.notifications;
IF OBJECT_ID('dbo.inventory_transfer_requests', 'U') IS NOT NULL DROP TABLE dbo.inventory_transfer_requests;
IF OBJECT_ID('dbo.payments', 'U') IS NOT NULL DROP TABLE dbo.payments;
IF OBJECT_ID('dbo.order_details', 'U') IS NOT NULL DROP TABLE dbo.order_details;
IF OBJECT_ID('dbo.orders', 'U') IS NOT NULL DROP TABLE dbo.orders;
IF OBJECT_ID('dbo.cart_items', 'U') IS NOT NULL DROP TABLE dbo.cart_items;
IF OBJECT_ID('dbo.carts', 'U') IS NOT NULL DROP TABLE dbo.carts;
IF OBJECT_ID('dbo.store_inventories', 'U') IS NOT NULL DROP TABLE dbo.store_inventories;
IF OBJECT_ID('dbo.product_images', 'U') IS NOT NULL DROP TABLE dbo.product_images;
IF OBJECT_ID('dbo.products', 'U') IS NOT NULL DROP TABLE dbo.products;
IF OBJECT_ID('dbo.promotions', 'U') IS NOT NULL DROP TABLE dbo.promotions;
IF OBJECT_ID('dbo.categories', 'U') IS NOT NULL DROP TABLE dbo.categories;
IF OBJECT_ID('dbo.otp_verifications', 'U') IS NOT NULL DROP TABLE dbo.otp_verifications;
IF OBJECT_ID('dbo.users', 'U') IS NOT NULL DROP TABLE dbo.users;
IF OBJECT_ID('dbo.stores', 'U') IS NOT NULL DROP TABLE dbo.stores;
GO

-- ============================================================================
-- 2. TẠO CÁC BẢNG DỮ LIỆU (CREATE TABLES)
-- ============================================================================

-- Bảng 1: Chuỗi chi nhánh cửa hàng hoa (Stores)
CREATE TABLE dbo.stores (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    name NVARCHAR(150) NOT NULL,
    address NVARCHAR(255) NOT NULL,
    phone VARCHAR(20) NULL,
    latitude FLOAT NULL,
    longitude FLOAT NULL,
    status BIT NULL DEFAULT 1
);
GO

-- Bảng 2: Người dùng (Users: Khách hàng, Florist, Quản lý, Admin)
CREATE TABLE dbo.users (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    full_name NVARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    phone VARCHAR(20) NULL,
    address NVARCHAR(255) NULL,
    avatar_url VARCHAR(255) NULL,
    role VARCHAR(20) NOT NULL, -- 'ADMIN', 'MANAGER', 'CUSTOMER'
    is_active BIT NULL DEFAULT 0,
    created_date DATE NULL,
    store_id BIGINT NULL,
    CONSTRAINT FK_users_stores FOREIGN KEY (store_id) REFERENCES dbo.stores(id) ON DELETE SET NULL
);
GO

-- Bảng 3: Mã xác thực OTP (OtpVerification)
CREATE TABLE dbo.otp_verifications (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    email VARCHAR(100) NOT NULL,
    otp_code VARCHAR(6) NOT NULL,
    otp_type VARCHAR(20) NOT NULL,
    expiry_time DATETIME2 NOT NULL,
    is_verified BIT NULL DEFAULT 0,
    created_at DATETIME2 NULL
);
GO

-- Bảng 4: Danh mục hoa tươi (Categories)
CREATE TABLE dbo.categories (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    name NVARCHAR(100) NOT NULL UNIQUE,
    description NVARCHAR(MAX) NULL
);
GO

-- Bảng 5: Sản phẩm hoa tươi & Phụ kiện sự kiện (Products)
CREATE TABLE dbo.products (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    name NVARCHAR(200) NOT NULL,
    description NVARCHAR(MAX) NULL,
    import_price NUMERIC(38,2) NOT NULL,
    price_buy NUMERIC(38,2) NOT NULL,
    price_rent_per_day NUMERIC(38,2) NOT NULL,
    deposit_fee NUMERIC(38,2) NOT NULL,
    category_id BIGINT NOT NULL,
    CONSTRAINT FK_products_categories FOREIGN KEY (category_id) REFERENCES dbo.categories(id) ON DELETE CASCADE
);
GO

-- Bảng 6: Hình ảnh hoa tươi (ProductImages)
CREATE TABLE dbo.product_images (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    image_url VARCHAR(255) NOT NULL,
    product_id BIGINT NOT NULL,
    CONSTRAINT FK_product_images_products FOREIGN KEY (product_id) REFERENCES dbo.products(id) ON DELETE CASCADE
);
GO

-- Bảng 7: Tồn kho hoa tươi theo chi nhánh (StoreInventories)
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

-- Bảng 8: Giỏ hàng (Carts)
CREATE TABLE dbo.carts (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    CONSTRAINT FK_carts_users FOREIGN KEY (user_id) REFERENCES dbo.users(id) ON DELETE CASCADE
);
GO

-- Bảng 9: Chi tiết giỏ hàng (CartItems)
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

-- Bảng 10: Đơn đặt hoa (Orders)
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
    CONSTRAINT FK_orders_users FOREIGN KEY (user_id) REFERENCES dbo.users(id),
    CONSTRAINT FK_orders_pickup_stores FOREIGN KEY (pickup_store_id) REFERENCES dbo.stores(id),
    CONSTRAINT FK_orders_return_stores FOREIGN KEY (return_store_id) REFERENCES dbo.stores(id)
);
GO

-- Bảng 11: Chi tiết đơn hàng hoa (OrderDetails)
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

-- Bảng 12: Thanh toán (Payments)
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

-- Bảng 13: Điều phối hoa giữa các chi nhánh (InventoryTransferRequests)
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

-- Bảng 14: Mã giảm giá & Ưu đãi hoa tươi (Promotions)
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
INSERT INTO dbo.stores (name, address, phone, latitude, longitude, status) VALUES 
(N'Bloom Flower - Chi Nhánh Bến Thành (Quận 1)', N'123 Lê Lợi, Phường Bến Thành, Quận 1, TP.HCM', '0281111222', 10.7725, 106.6980, 1),
(N'Bloom Flower - Chi Nhánh Phú Mỹ Hưng (Quận 7)', N'456 Nguyễn Thị Thập, Tân Hưng, Quận 7, TP.HCM', '0283333444', 10.7382, 106.7085, 1),
(N'Bloom Flower - Chi Nhánh Làng Đại Học (TP. Thủ Đức)', N'1 Võ Văn Ngân, Linh Chiểu, TP. Thủ Đức, TP.HCM', '0285555666', 10.8505, 106.7719, 1);
GO

-- 3.2. Tài khoản mẫu (Mật khẩu: 123456)
INSERT INTO dbo.users (username, password, full_name, email, phone, address, role, is_active, created_date, store_id) VALUES 
('admin', '$2a$10$MMgWoixo7KjB5HrbStq9g.NkNv9zWbVpdSlBmPjrncPse25LBqh82', N'Quản Trị Viên Chuỗi Hoa Tươi', 'admin@bloomflower.vn', '0901111222', N'TP. Hồ Chí Minh', 'ADMIN', 1, CAST(GETDATE() AS DATE), NULL),
('manager_q1', '$2a$10$MMgWoixo7KjB5HrbStq9g.NkNv9zWbVpdSlBmPjrncPse25LBqh82', N'Nguyễn Quản Lý Cửa Hàng Q1', 'manager.q1@bloomflower.vn', '0902222333', N'Quận 1, TP.HCM', 'MANAGER', 1, CAST(GETDATE() AS DATE), 1),
('khachhang', '$2a$10$MMgWoixo7KjB5HrbStq9g.NkNv9zWbVpdSlBmPjrncPse25LBqh82', N'Lê Khách Hàng Yêu Hoa', 'khachhang@gmail.com', '0909999888', N'TP. Hồ Chí Minh', 'CUSTOMER', 1, CAST(GETDATE() AS DATE), NULL);
GO

-- 3.3. Giỏ hàng cho khách hàng mẫu
INSERT INTO dbo.carts (user_id) VALUES (5);
GO

-- 3.4. Danh mục hoa tươi
INSERT INTO dbo.categories (name, description) VALUES 
(N'Hoa Bó Tươi Nghệ Thuật', N'Các mẫu bó hoa tươi tuyển chọn từ hoa hồng Ecuador, tulip Hà Lan, baby, hướng dương phong cách sang trọng'),
(N'Giỏ & Hộp Hoa Cao Cấp', N'Giỏ hoa và hộp hoa chúc mừng sinh nhật, kỷ niệm, ngày lễ với thiết kế tinh tế'),
(N'Kệ Hoa Khai Trương Hồng Phát', N'Kệ hoa 2 tầng, lẵng hoa chúc mừng khai trương tài lộc, sự kiện trang trọng'),
(N'Hoa Cưới & Cầm Tay Cô Dâu', N'Bộ sưu tập hoa cưới tinh khôi, hoa cầm tay cô dâu và hoa cài áo chú rể'),
(N'Lan Hồ Điệp Hoàng Gia', N'Chậu lan hồ điệp tuyển chọn thượng hạng với vẻ đẹp sang trọng, quý phái'),
(N'Thuê Cổng Hoa & Khung Sự Kiện', N'Dịch vụ cho thuê khung cổng hoa cưới tiệc, chân kệ hoa mỹ thuật cho đám cưới và sự kiện');
GO

-- 3.5. Danh mục sản phẩm hoa tươi & Phụ kiện sự kiện
INSERT INTO dbo.products (name, description, import_price, price_buy, price_rent_per_day, deposit_fee, category_id, image_url) VALUES 
(N'Bó Hoa Hồng Đỏ Ecuador Tình Nồng Cháy', N'Gồm 19 bông hồng đỏ Ecuador nhập khẩu phối hoa baby trắng Hà Lan, bọc giấy gói cao cấp phong cách Hàn Quốc.', 350000.00, 650000.00, 0.00, 0.00, 1, 'https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=800'),
(N'Bó Hoa Tulip Hà Lan Pastel Thanh Khiết', N'15 bông tulip Hà Lan màu hồng pastel và trắng ngọc, biểu trưng cho tình yêu chân thành và trang nhã.', 480000.00, 850000.00, 0.00, 0.00, 1, 'https://images.unsplash.com/photo-1526047932273-341f2a7631f9?w=800'),
(N'Bó Hoa Hướng Dương Ánh Dương Rạng Rỡ', N'5 bông hướng dương đà lạt loại 1 phối hoa cúc tana và lá bạc nhập khẩu, mang ý nghĩa may mắn và thành công.', 220000.00, 420000.00, 0.00, 0.00, 1, 'https://images.unsplash.com/photo-1597848212624-a19eb35e2651?w=800'),
(N'Giỏ Hoa Cẩm Tú Cầu Xanh Mộng Mơ', N'Giỏ mây vintage kết hợp hoa cẩm tú cầu xanh lam, hoa hồng kem và hoa cát tường dịu dàng.', 420000.00, 750000.00, 0.00, 0.00, 2, 'https://images.unsplash.com/photo-1561181286-d3fee7d55364?w=800'),
(N'Hộp Hoa Tròn Sang Trọng Sweet Love', N'Hộp hoa mica trong suốt cao cấp phối hoa hồng phấn Juliet và baby khô cao cấp lưu giữ vẻ đẹp lâu dài.', 520000.00, 920000.00, 0.00, 0.00, 2, 'https://images.unsplash.com/photo-1525310072745-f49212b5ac6d?w=800'),
(N'Kệ Hoa Khai Trương Đại Cát 2 Tầng', N'Kệ hoa 2 tầng cao 1.8m phối hoa hồng môn đỏ, hướng dương và hoa đồng tiền rực rỡ mang tài lộc dồi dào.', 950000.00, 1650000.00, 0.00, 0.00, 3, 'https://images.unsplash.com/photo-1533616688419-b7a585564566?w=800'),
(N'Kệ Hoa Khai Trương Hồng Phát Rực Rỡ', N'Thiết kế hiện đại tone màu cam vàng hoàng kim, chúc mừng đối tác kinh doanh phát đạt thịnh vượng.', 800000.00, 1350000.00, 0.00, 0.00, 3, 'https://images.unsplash.com/photo-1527061011665-3652c757a4d4?w=800'),
(N'Bó Hoa Cưới Cầm Tay Calla Lily Tinh Khôi', N'Hoa rum Calla Lily trắng nhập khẩu kết hợp nơ lụa voan cao cấp, tôn vinh vẻ đẹp thuần khiết của cô dâu.', 650000.00, 1150000.00, 0.00, 0.00, 4, 'https://images.unsplash.com/photo-1545232979-8bf68ee9b1af?w=800'),
(N'Chậu Lan Hồ Điệp Hoàng Gia 5 Cành Đột Biến', N'Chậu sứ Bát Tràng cao cấp cắm 5 cành lan hồ điệp tím viền trắng cánh dày, hoa bền tươi trên 1 tháng.', 1200000.00, 2100000.00, 0.00, 0.00, 5, 'https://images.unsplash.com/photo-1566838318109-a8bffb91d082?w=800'),
(N'Khung Cổng Hoa Cưới Vòm Tròn Bohemian Luxury', N'Khung cổng hoa kim loại sơn tĩnh điện hoa văn mỹ thuật kèm hoa lụa và hoa tươi cao cấp decor trọn gói.', 5500000.00, 8500000.00, 650000.00, 2000000.00, 6, 'https://images.unsplash.com/photo-1532712938310-34cb3982ef74?w=800'),
(N'Chân Kệ Hoa Sắt Mỹ Thuật & Bình Pha Lê Cổ Điển', N'Set 4 chân đôn cắm hoa tiệc cưới kèm bình pha lê sang trọng trang trí lối đi hoặc bàn tiệc sự kiện.', 1500000.00, 2400000.00, 150000.00, 500000.00, 6, 'https://images.unsplash.com/photo-1582794543462-0d7922e50cf5?w=800');
GO

-- 3.6. Tồn kho hoa tươi sẵn sàng tại 3 chi nhánh
INSERT INTO dbo.store_inventories (store_id, product_id, quantity, version) VALUES 
-- Chi nhánh Quận 1 (Bến Thành)
(1, 1, 20, 0), (1, 2, 15, 0), (1, 3, 25, 0), (1, 4, 12, 0), (1, 5, 10, 0), (1, 6, 8, 0), (1, 7, 10, 0), (1, 8, 6, 0), (1, 9, 8, 0), (1, 10, 3, 0), (1, 11, 6, 0),
-- Chi nhánh Quận 7 (Phú Mỹ Hưng)
(2, 1, 18, 0), (2, 2, 12, 0), (2, 3, 20, 0), (2, 4, 15, 0), (2, 5, 8, 0), (2, 6, 6, 0), (2, 7, 8, 0), (2, 8, 5, 0), (2, 9, 10, 0), (2, 10, 2, 0), (2, 11, 4, 0),
-- Chi nhánh TP. Thủ Đức (Làng Đại Học)
(3, 1, 15, 0), (3, 2, 10, 0), (3, 3, 30, 0), (3, 4, 10, 0), (3, 5, 6, 0), (3, 6, 5, 0), (3, 7, 6, 0), (3, 8, 4, 0), (3, 9, 5, 0), (3, 10, 2, 0), (3, 11, 4, 0);
GO
