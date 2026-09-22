-- ============================================================================
-- HỆ THỐNG QUẢN LÝ BÁN VÀ CHO THUÊ ĐỒ DÃ NGOẠI & THIẾT BỊ (RENTAL & RETAIL SHOP)
-- HỆ QUẢN TRỊ CƠ SỞ DỮ LIỆU: MICROSOFT SQL SERVER
-- Tác giả: Nhóm phát triển dự án
-- Ngày tạo: 2026-09-22
-- ============================================================================

IF NOT EXISTS (SELECT * FROM sys.databases WHERE name = 'RentalShop')
BEGIN
    CREATE DATABASE RentalShop;
END
GO

USE RentalShop;
GO

-- ============================================================================
-- 1. XÓA CÁC RÀNG BUỘC KHÓA NGOẠI VÀ BẢNG NẾU ĐÃ TỒN TẠI (DROP TABLES)
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

-- Bảng 1: Chi nhánh cửa hàng (Stores)
CREATE TABLE dbo.stores (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    name NVARCHAR(150) NOT NULL,
    address NVARCHAR(255) NOT NULL,
    phone VARCHAR(20) NULL,
    latitude FLOAT NULL,
    longitude FLOAT NULL,
    status BIT DEFAULT 1 -- 1: Đang hoạt động, 0: Tạm ngưng
);
GO

-- Bảng 2: Người dùng & Tài khoản nhân sự (Users)
CREATE TABLE dbo.users (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    full_name NVARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    phone VARCHAR(20) NULL,
    address NVARCHAR(255) NULL,
    avatar_url VARCHAR(255) NULL,
    role VARCHAR(20) NOT NULL, -- ADMIN, MANAGER, STAFF, CUSTOMER
    is_active BIT DEFAULT 0, -- Kích hoạt sau khi xác thực OTP
    created_date DATE DEFAULT CAST(GETDATE() AS DATE),
    store_id BIGINT NULL,
    CONSTRAINT FK_users_stores FOREIGN KEY (store_id) REFERENCES dbo.stores(id)
);
GO

-- Bảng 3: Mã xác thực OTP kích hoạt tài khoản / đổi mật khẩu (OtpVerifications)
CREATE TABLE dbo.otp_verifications (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    code VARCHAR(6) NOT NULL,
    type VARCHAR(30) NOT NULL, -- ACTIVATE_ACCOUNT, FORGOT_PASSWORD
    expired_at DATETIME2(7) NOT NULL,
    created_at DATETIME2(7) DEFAULT SYSDATETIME(),
    is_used BIT DEFAULT 0,
    CONSTRAINT FK_otp_users FOREIGN KEY (user_id) REFERENCES dbo.users(id)
);
GO

-- Bảng 4: Danh mục sản phẩm (Categories)
CREATE TABLE dbo.categories (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    name NVARCHAR(100) NOT NULL,
    description NVARCHAR(255) NULL
);
GO

-- Bảng 5: Sản phẩm bán và cho thuê (Products)
CREATE TABLE dbo.products (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    category_id BIGINT NOT NULL,
    name NVARCHAR(200) NOT NULL,
    description NVARCHAR(MAX) NULL,
    import_price DECIMAL(18,2) NOT NULL,      -- Giá nhập gốc để kiểm tra chống bán lỗ
    price_buy DECIMAL(18,2) NOT NULL,         -- Giá bán đứt
    price_rent_per_day DECIMAL(18,2) NOT NULL,-- Đơn giá thuê theo ngày
    deposit_fee DECIMAL(18,2) NOT NULL,       -- Tiền đặt cọc cố định
    CONSTRAINT FK_products_categories FOREIGN KEY (category_id) REFERENCES dbo.categories(id)
);
GO

-- Bảng 6: Hình ảnh sản phẩm (ProductImages)
CREATE TABLE dbo.product_images (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    product_id BIGINT NOT NULL,
    image_url VARCHAR(255) NOT NULL,
    is_main BIT DEFAULT 0, -- 1: Ảnh chính hiển thị thumbnail
    CONSTRAINT FK_product_images_products FOREIGN KEY (product_id) REFERENCES dbo.products(id) ON DELETE CASCADE
);
GO

-- Bảng 7: Tồn kho tại từng chi nhánh (StoreInventories)
CREATE TABLE dbo.store_inventories (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    store_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INT NOT NULL DEFAULT 0,
    version BIGINT DEFAULT 0, -- Khóa lạc quan chống bán trùng (Optimistic Locking)
    CONSTRAINT UQ_store_product UNIQUE (store_id, product_id),
    CONSTRAINT FK_inventories_stores FOREIGN KEY (store_id) REFERENCES dbo.stores(id),
    CONSTRAINT FK_inventories_products FOREIGN KEY (product_id) REFERENCES dbo.products(id)
);
GO

-- Bảng 8: Giỏ hàng khách hàng (Carts)
CREATE TABLE dbo.carts (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    CONSTRAINT FK_carts_users FOREIGN KEY (user_id) REFERENCES dbo.users(id) ON DELETE CASCADE
);
GO

-- Bảng 9: Chi tiết mục giỏ hàng (CartItems)
CREATE TABLE dbo.cart_items (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    cart_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    store_id BIGINT NOT NULL,
    order_type VARCHAR(10) NOT NULL, -- BUY, RENT
    quantity INT NOT NULL DEFAULT 1,
    rent_start_date DATE NULL,
    rent_end_date DATE NULL,
    CONSTRAINT FK_cart_items_carts FOREIGN KEY (cart_id) REFERENCES dbo.carts(id) ON DELETE CASCADE,
    CONSTRAINT FK_cart_items_products FOREIGN KEY (product_id) REFERENCES dbo.products(id),
    CONSTRAINT FK_cart_items_stores FOREIGN KEY (store_id) REFERENCES dbo.stores(id)
);
GO

-- Bảng 10: Đơn hàng (Orders)
CREATE TABLE dbo.orders (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    pickup_store_id BIGINT NOT NULL, -- Chi nhánh xuất kho nhận hàng
    return_store_id BIGINT NULL,     -- Chi nhánh nhận trả đồ (hỗ trợ trả chéo kho)
    order_type VARCHAR(10) NOT NULL, -- BUY, RENT
    status VARCHAR(20) NOT NULL,     -- PENDING, CONFIRMED, RENTING, OVERDUE, RETURNED, COMPLETED, CANCELLED
    total_amount DECIMAL(18,2) NOT NULL DEFAULT 0,
    total_deposit DECIMAL(18,2) DEFAULT 0,
    fine_amount DECIMAL(18,2) DEFAULT 0, -- Tiền phạt trễ hạn / hư hại đồ
    created_at DATETIME2(7) DEFAULT SYSDATETIME(),
    version BIGINT DEFAULT 0,        -- Khóa lạc quan
    CONSTRAINT FK_orders_users FOREIGN KEY (user_id) REFERENCES dbo.users(id),
    CONSTRAINT FK_orders_pickup_stores FOREIGN KEY (pickup_store_id) REFERENCES dbo.stores(id),
    CONSTRAINT FK_orders_return_stores FOREIGN KEY (return_store_id) REFERENCES dbo.stores(id)
);
GO

-- Bảng 11: Chi tiết đơn hàng (OrderDetails)
CREATE TABLE dbo.order_details (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    unit_price DECIMAL(18,2) NOT NULL,
    deposit_fee DECIMAL(18,2) DEFAULT 0,
    order_type VARCHAR(10) NOT NULL, -- BUY, RENT
    rent_start_date DATE NULL,
    rent_end_date DATE NULL,
    actual_return_date DATE NULL,
    CONSTRAINT FK_order_details_orders FOREIGN KEY (order_id) REFERENCES dbo.orders(id) ON DELETE CASCADE,
    CONSTRAINT FK_order_details_products FOREIGN KEY (product_id) REFERENCES dbo.products(id)
);
GO

-- Bảng 12: Thanh toán & Tất toán cọc (Payments)
CREATE TABLE dbo.payments (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    order_id BIGINT NOT NULL,
    amount DECIMAL(18,2) NOT NULL,
    method VARCHAR(20) NOT NULL,       -- CASH, VNPAY, MOMO, COD
    status VARCHAR(20) NOT NULL,       -- PENDING, SUCCESS, FAILED, REFUNDED
    transaction_code VARCHAR(100) NULL,
    paid_at DATETIME2(7) NULL,
    refund_method NVARCHAR(50) NULL,   -- Phương thức hoàn cọc khi trả đồ
    refund_note NVARCHAR(255) NULL,    -- Ghi chú khấu trừ tiền phạt/hư hỏng
    refunded_at DATETIME2(7) NULL,
    CONSTRAINT FK_payments_orders FOREIGN KEY (order_id) REFERENCES dbo.orders(id)
);
GO

-- Bảng 13: Yêu cầu điều chuyển tồn kho giữa các chi nhánh (InventoryTransferRequests)
CREATE TABLE dbo.inventory_transfer_requests (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    from_store_id BIGINT NOT NULL,
    to_store_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    created_by_staff_id BIGINT NOT NULL,
    approved_by_manager_id BIGINT NULL,
    quantity INT NOT NULL,
    status VARCHAR(20) NOT NULL, -- PENDING, APPROVED, REJECTED, COMPLETED
    note NVARCHAR(255) NULL,
    created_at DATETIME2(7) DEFAULT SYSDATETIME(),
    updated_at DATETIME2(7) NULL,
    CONSTRAINT FK_transfer_from_store FOREIGN KEY (from_store_id) REFERENCES dbo.stores(id),
    CONSTRAINT FK_transfer_to_store FOREIGN KEY (to_store_id) REFERENCES dbo.stores(id),
    CONSTRAINT FK_transfer_product FOREIGN KEY (product_id) REFERENCES dbo.products(id),
    CONSTRAINT FK_transfer_staff FOREIGN KEY (created_by_staff_id) REFERENCES dbo.users(id),
    CONSTRAINT FK_transfer_manager FOREIGN KEY (approved_by_manager_id) REFERENCES dbo.users(id)
);
GO

-- Bảng 14: Chương trình khuyến mãi (Promotions)
CREATE TABLE dbo.promotions (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    description NVARCHAR(255) NULL,
    discount_percent INT NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    store_id BIGINT NULL, -- Áp dụng riêng từng chi nhánh hoặc toàn hệ thống nếu NULL
    CONSTRAINT FK_promotions_stores FOREIGN KEY (store_id) REFERENCES dbo.stores(id)
);
GO

-- Bảng 15: Thông báo hệ thống (Notifications)
CREATE TABLE dbo.notifications (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title NVARCHAR(100) NOT NULL,
    message NVARCHAR(255) NOT NULL,
    is_read BIT DEFAULT 0,
    created_at DATETIME2(7) DEFAULT SYSDATETIME(),
    CONSTRAINT FK_notifications_users FOREIGN KEY (user_id) REFERENCES dbo.users(id)
);
GO

-- Bảng 16: Nhật ký hoạt động nhân sự (ActivityLogs)
CREATE TABLE dbo.activity_logs (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    action VARCHAR(50) NOT NULL,     -- APPROVE_ORDER, DELIVER_ORDER, CONFIRM_RETURN...
    target_type VARCHAR(50) NOT NULL, -- Order, Product, TransferRequest
    target_id BIGINT NOT NULL,
    timestamp DATETIME2(7) DEFAULT SYSDATETIME(),
    CONSTRAINT FK_activity_logs_users FOREIGN KEY (user_id) REFERENCES dbo.users(id)
);
GO

-- ============================================================================
-- 3. CHÈN DỮ LIỆU KHỞI TẠO MẪU (INSERT SAMPLE DATA)
-- ============================================================================

-- 3.1. Các chi nhánh
INSERT INTO dbo.stores (name, address, phone, latitude, longitude, status) VALUES
(N'Chi nhánh Quận 1 (Cơ sở chính)', N'123 Lê Lợi, Phường Bến Thành, Quận 1, TP.HCM', '0281111222', 10.7725, 106.6980, 1),
(N'Chi nhánh Quận 7', N'456 Nguyễn Thị Thập, Tân Hưng, Quận 7, TP.HCM', '0283333444', 10.7380, 106.7130, 1),
(N'Chi nhánh TP. Thủ Đức', N'1 Võ Văn Ngân, Linh Chiểu, Thủ Đức, TP.HCM', '0285555666', 10.8505, 106.7719, 1);
GO

-- 3.2. Tài khoản mẫu (Mật khẩu mặc định: 123456 qua chuẩn BCrypt)
-- Hash BCrypt: $2a$10$wT0vN69g2uG0fL4k2t6EreIcx7jE4t/8D4O1bM91gWk37t9HcmEfa
INSERT INTO dbo.users (username, password, full_name, email, phone, address, role, is_active, created_date, store_id) VALUES
('admin', '$2a$10$wT0vN69g2uG0fL4k2t6EreIcx7jE4t/8D4O1bM91gWk37t9HcmEfa', N'Quản Trị Viên Toàn Hệ Thống', 'admin@rentalshop.vn', '0901111222', N'TP. Hồ Chí Minh', 'ADMIN', 1, CAST(GETDATE() AS DATE), NULL),
('manager_q1', '$2a$10$wT0vN69g2uG0fL4k2t6EreIcx7jE4t/8D4O1bM91gWk37t9HcmEfa', N'Nguyễn Quản Lý Q1', 'manager.q1@rentalshop.vn', '0902222333', N'Quận 1, TP.HCM', 'MANAGER', 1, CAST(GETDATE() AS DATE), 1),
('staff_q1', '$2a$10$wT0vN69g2uG0fL4k2t6EreIcx7jE4t/8D4O1bM91gWk37t9HcmEfa', N'Trần Nhân Viên Q1', 'staff.q1@rentalshop.vn', '0903333444', N'Quận 1, TP.HCM', 'STAFF', 1, CAST(GETDATE() AS DATE), 1),
('staff_q7', '$2a$10$wT0vN69g2uG0fL4k2t6EreIcx7jE4t/8D4O1bM91gWk37t9HcmEfa', N'Lê Nhân Viên Q7', 'staff.q7@rentalshop.vn', '0904444555', N'Quận 7, TP.HCM', 'STAFF', 1, CAST(GETDATE() AS DATE), 2),
('khachhang', '$2a$10$wT0vN69g2uG0fL4k2t6EreIcx7jE4t/8D4O1bM91gWk37t9HcmEfa', N'Lê Khách Hàng', 'khachhang@gmail.com', '0909999888', N'TP. Hồ Chí Minh', 'CUSTOMER', 1, CAST(GETDATE() AS DATE), NULL);
GO

-- 3.3. Giỏ hàng cho khách hàng
INSERT INTO dbo.carts (user_id) VALUES (5);
GO

-- 3.4. Danh mục sản phẩm
INSERT INTO dbo.categories (name, description) VALUES
(N'Đồ Dã Ngoại & Cắm Trại', N'Lều, túi ngủ, đèn pin dã ngoại cao cấp'),
(N'Dụng Cụ Nấu Ăn Dã Ngoại', N'Bếp gas mini, bộ nồi dã ngoại chuyên dụng'),
(N'Thiết Bị Âm Thanh & Ánh Sáng', N'Loa kéo du lịch, đèn dây trang trí camping');
GO

-- 3.5. Sản phẩm mẫu
INSERT INTO dbo.products (category_id, name, description, import_price, price_buy, price_rent_per_day, deposit_fee) VALUES
(1, N'Lều Cắm Trại Chống Nước 4 Người Vintage', N'Lều vải oxford hai lớp chống thấm tuyệt đối, dễ dàng tháo lắp trong 5 phút.', 1000000.00, 1500000.00, 80000.00, 500000.00),
(1, N'Túi Ngủ Đôi Giữ Nhiệt Cao Cấp Naturehike', N'Lót nỉ siêu ấm áp, chống lạnh tới -5 độ C, gập gọn tiện dụng.', 400000.00, 650000.00, 35000.00, 200000.00),
(2, N'Bộ Bếp & Nồi Nấu Dã Ngoại Nhôm Anodize', N'Gọn nhẹ tiện lợi, thích hợp cho nhóm 3-5 người leo núi dã ngoại.', 300000.00, 500000.00, 30000.00, 150000.00);
GO

-- 3.6. Tồn kho chi nhánh
INSERT INTO dbo.store_inventories (store_id, product_id, quantity, version) VALUES
(1, 1, 5, 0), -- Chi nhánh Q1 có 5 lều
(2, 1, 2, 0), -- Chi nhánh Q7 có 2 lều
(3, 1, 0, 0), -- Chi nhánh Thủ Đức tạm hết
(1, 2, 10, 0),
(2, 2, 8, 0),
(3, 2, 4, 0),
(1, 3, 6, 0),
(2, 3, 3, 0),
(3, 3, 5, 0);
GO
