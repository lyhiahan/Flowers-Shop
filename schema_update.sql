USE RentalShop;
GO

-- 1. Cập nhật bảng users
IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('users') AND name = 'cover')
    ALTER TABLE users ADD cover NVARCHAR(500) NULL;
IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('users') AND name = 'point')
    ALTER TABLE users ADD point INT NOT NULL DEFAULT 0;
IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('users') AND name = 'e_wallet')
    ALTER TABLE users ADD e_wallet DECIMAL(18,2) NOT NULL DEFAULT 0;
IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('users') AND name = 'is_email_active')
    ALTER TABLE users ADD is_email_active BIT NOT NULL DEFAULT 1;
IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('users') AND name = 'otp_code')
    ALTER TABLE users ADD otp_code VARCHAR(10) NULL;
IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('users') AND name = 'otp_expiry')
    ALTER TABLE users ADD otp_expiry DATETIME NULL;
IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('users') AND name = 'user_level_id')
    ALTER TABLE users ADD user_level_id BIGINT NULL;
GO

-- 2. Cập nhật bảng stores
IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('stores') AND name = 'owner_id')
    ALTER TABLE stores ADD owner_id BIGINT NULL;
IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('stores') AND name = 'bio')
    ALTER TABLE stores ADD bio NVARCHAR(1000) NULL;
IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('stores') AND name = 'avatar')
    ALTER TABLE stores ADD avatar NVARCHAR(500) NULL;
IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('stores') AND name = 'cover')
    ALTER TABLE stores ADD cover NVARCHAR(500) NULL;
IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('stores') AND name = 'featured_images')
    ALTER TABLE stores ADD featured_images NVARCHAR(MAX) NULL;
IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('stores') AND name = 'commission_id')
    ALTER TABLE stores ADD commission_id BIGINT NULL;
IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('stores') AND name = 'store_level_id')
    ALTER TABLE stores ADD store_level_id BIGINT NULL;
IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('stores') AND name = 'is_open')
    ALTER TABLE stores ADD is_open BIT NOT NULL DEFAULT 1;
IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('stores') AND name = 'point')
    ALTER TABLE stores ADD point INT NOT NULL DEFAULT 0;
IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('stores') AND name = 'rating')
    ALTER TABLE stores ADD rating FLOAT NOT NULL DEFAULT 5.0;
IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('stores') AND name = 'e_wallet')
    ALTER TABLE stores ADD e_wallet DECIMAL(18,2) NOT NULL DEFAULT 0;
GO

-- 3. Cập nhật bảng products
IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('products') AND name = 'sold')
    ALTER TABLE products ADD sold INT NOT NULL DEFAULT 0;
IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('products') AND name = 'rating')
    ALTER TABLE products ADD rating FLOAT NOT NULL DEFAULT 5.0;
IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('products') AND name = 'view_count')
    ALTER TABLE products ADD view_count INT NOT NULL DEFAULT 0;
IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('products') AND name = 'store_id')
    ALTER TABLE products ADD store_id BIGINT NULL;
IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('products') AND name = 'is_active')
    ALTER TABLE products ADD is_active BIT NOT NULL DEFAULT 1;
IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('products') AND name = 'is_selling')
    ALTER TABLE products ADD is_selling BIT NOT NULL DEFAULT 1;
IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('products') AND name = 'promotional_price')
    ALTER TABLE products ADD promotional_price DECIMAL(18,2) NULL;
IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('products') AND name = 'image_url')
    ALTER TABLE products ADD image_url NVARCHAR(500) NULL;
GO

-- 4. Bảng user_levels
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'user_levels')
BEGIN
    CREATE TABLE user_levels (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        name NVARCHAR(32) NOT NULL UNIQUE,
        min_point INT NOT NULL UNIQUE,
        discount DECIMAL(5,2) NOT NULL DEFAULT 0,
        is_deleted BIT NOT NULL DEFAULT 0,
        created_at DATETIME DEFAULT GETDATE(),
        updated_at DATETIME DEFAULT GETDATE()
    );
END
GO

-- 5. Bảng store_levels
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'store_levels')
BEGIN
    CREATE TABLE store_levels (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        name NVARCHAR(32) NOT NULL UNIQUE,
        min_point INT NOT NULL UNIQUE,
        discount DECIMAL(5,2) NOT NULL DEFAULT 0,
        is_deleted BIT NOT NULL DEFAULT 0,
        created_at DATETIME DEFAULT GETDATE(),
        updated_at DATETIME DEFAULT GETDATE()
    );
END
GO

-- 6. Bảng commissions
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'commissions')
BEGIN
    CREATE TABLE commissions (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        name NVARCHAR(32) NOT NULL UNIQUE,
        cost DECIMAL(18,2) NOT NULL,
        description NVARCHAR(3000),
        is_deleted BIT NOT NULL DEFAULT 0,
        created_at DATETIME DEFAULT GETDATE(),
        updated_at DATETIME DEFAULT GETDATE()
    );
END
GO

-- 7. Bảng styles & style_values
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'styles')
BEGIN
    CREATE TABLE styles (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        name NVARCHAR(32) NOT NULL UNIQUE,
        category_id BIGINT NULL,
        is_deleted BIT NOT NULL DEFAULT 0,
        created_at DATETIME DEFAULT GETDATE(),
        updated_at DATETIME DEFAULT GETDATE()
    );
END
GO

IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'style_values')
BEGIN
    CREATE TABLE style_values (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        name NVARCHAR(32) NOT NULL,
        style_id BIGINT NOT NULL,
        is_deleted BIT NOT NULL DEFAULT 0,
        created_at DATETIME DEFAULT GETDATE(),
        updated_at DATETIME DEFAULT GETDATE(),
        FOREIGN KEY (style_id) REFERENCES styles(id) ON DELETE CASCADE
    );
END
GO

-- 8. Bảng deliveries (Đơn vị vận chuyển)
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'deliveries')
BEGIN
    CREATE TABLE deliveries (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        name NVARCHAR(100) NOT NULL UNIQUE,
        description NVARCHAR(1000),
        price DECIMAL(18,2) NOT NULL DEFAULT 0,
        is_active BIT NOT NULL DEFAULT 1,
        is_deleted BIT NOT NULL DEFAULT 0,
        created_at DATETIME DEFAULT GETDATE(),
        updated_at DATETIME DEFAULT GETDATE()
    );
END
GO

-- 9. Bảng user_addresses (Sổ địa chỉ người dùng)
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'user_addresses')
BEGIN
    CREATE TABLE user_addresses (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        user_id BIGINT NOT NULL,
        recipient_name NVARCHAR(100) NOT NULL,
        phone VARCHAR(20) NOT NULL,
        address_detail NVARCHAR(255) NOT NULL,
        is_default BIT NOT NULL DEFAULT 0,
        created_at DATETIME DEFAULT GETDATE(),
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
    );
END
GO

-- 10. Bảng vouchers (Mã giảm giá)
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'vouchers')
BEGIN
    CREATE TABLE vouchers (
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
        created_at DATETIME DEFAULT GETDATE()
    );
END
GO

-- 11. Cập nhật orders (shipper, voucher, discount, payment_method)
IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('orders') AND name = 'shipper_id')
    ALTER TABLE orders ADD shipper_id BIGINT NULL;
IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('orders') AND name = 'voucher_id')
    ALTER TABLE orders ADD voucher_id BIGINT NULL;
IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('orders') AND name = 'discount_amount')
    ALTER TABLE orders ADD discount_amount DECIMAL(18,2) NOT NULL DEFAULT 0;
IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('orders') AND name = 'payment_method')
    ALTER TABLE orders ADD payment_method VARCHAR(20) NOT NULL DEFAULT 'COD';
IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('orders') AND name = 'payment_status')
    ALTER TABLE orders ADD payment_status VARCHAR(20) NOT NULL DEFAULT 'UNPAID';
GO

-- 12. Bảng reviews (Đánh giá & bình luận)
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'reviews')
BEGIN
    CREATE TABLE reviews (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        user_id BIGINT NOT NULL,
        product_id BIGINT NOT NULL,
        store_id BIGINT NULL,
        order_id BIGINT NULL,
        content NVARCHAR(1000) NOT NULL,
        stars INT NOT NULL CHECK (stars >= 1 AND stars <= 5),
        image_url NVARCHAR(500) NULL,
        created_at DATETIME DEFAULT GETDATE(),
        updated_at DATETIME DEFAULT GETDATE(),
        FOREIGN KEY (user_id) REFERENCES users(id),
        FOREIGN KEY (product_id) REFERENCES products(id)
    );
END
GO

-- 13. Bảng product_wishlists
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'product_wishlists')
BEGIN
    CREATE TABLE product_wishlists (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        user_id BIGINT NOT NULL,
        product_id BIGINT NOT NULL,
        created_at DATETIME DEFAULT GETDATE(),
        CONSTRAINT uq_user_wishlist UNIQUE (user_id, product_id),
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
        FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
    );
END
GO

-- 14. Bảng recently_viewed
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'recently_viewed')
BEGIN
    CREATE TABLE recently_viewed (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        user_id BIGINT NOT NULL,
        product_id BIGINT NOT NULL,
        viewed_at DATETIME DEFAULT GETDATE(),
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
        FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
    );
END
GO

-- 15. Bảng user_follow_store
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'user_follow_store')
BEGIN
    CREATE TABLE user_follow_store (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        user_id BIGINT NOT NULL,
        store_id BIGINT NOT NULL,
        created_at DATETIME DEFAULT GETDATE(),
        CONSTRAINT uq_user_follow_store UNIQUE (user_id, store_id),
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
        FOREIGN KEY (store_id) REFERENCES stores(id) ON DELETE CASCADE
    );
END
GO

-- 16. Bảng transactions
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'transactions')
BEGIN
    CREATE TABLE transactions (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        user_id BIGINT NULL,
        store_id BIGINT NULL,
        is_up BIT NOT NULL DEFAULT 1,
        amount DECIMAL(18,2) NOT NULL,
        type VARCHAR(30) DEFAULT 'ORDER_PAYMENT',
        description NVARCHAR(500),
        status VARCHAR(20) DEFAULT 'COMPLETED',
        created_at DATETIME DEFAULT GETDATE(),
        FOREIGN KEY (user_id) REFERENCES users(id),
        FOREIGN KEY (store_id) REFERENCES stores(id)
    );
END
GO
