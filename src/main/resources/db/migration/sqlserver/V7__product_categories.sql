-- Keep the existing main category; extra categories are optional and unique.
CREATE TABLE dbo.product_additional_categories (
    product_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    CONSTRAINT PK_product_additional_categories PRIMARY KEY (product_id, category_id),
    CONSTRAINT FK_product_additional_product FOREIGN KEY (product_id) REFERENCES dbo.products(id),
    CONSTRAINT FK_product_additional_category FOREIGN KEY (category_id) REFERENCES dbo.categories(id)
);
CREATE INDEX IX_product_additional_category ON dbo.product_additional_categories(category_id, product_id);

-- Seed by purpose, not flower species or container. Do not revive deliberately hidden categories.
DECLARE @occasions TABLE (name NVARCHAR(100), description NVARCHAR(500));
INSERT INTO @occasions VALUES
    (N'Hoa Tình Yêu', N'Hoa dành cho tình yêu, kỷ niệm và Valentine.'),
    (N'Hoa Sinh Nhật', N'Hoa chúc mừng sinh nhật.'),
    (N'Hoa Cưới', N'Hoa dành cho lễ cưới và cô dâu.'),
    (N'Hoa Khai Trương', N'Hoa chúc mừng khai trương.'),
    (N'Hoa Tốt Nghiệp', N'Hoa chúc mừng tốt nghiệp.'),
    (N'Hoa Chúc Mừng', N'Hoa chúc mừng thành công và các dịp đặc biệt.'),
    (N'Hoa Chia Buồn', N'Hoa chia buồn và kính viếng.'),
    (N'Hoa Tân Gia', N'Hoa mừng nhà mới.'),
    (N'Hoa Trang Trí', N'Hoa và phụ kiện trang trí không gian, sự kiện.'),
    (N'Hoa Quà Tặng', N'Hoa làm quà tặng; chọn thêm dịp phù hợp khi chỉnh sửa sản phẩm.');
INSERT INTO dbo.categories(name, description, is_deleted)
SELECT o.name, o.description, 0 FROM @occasions o
WHERE NOT EXISTS (SELECT 1 FROM dbo.categories c WHERE c.name=o.name);

-- Only convert known legacy defaults, leaving custom categories untouched.
DECLARE @mapping TABLE (old_name NVARCHAR(100), new_name NVARCHAR(100));
INSERT INTO @mapping VALUES
    (N'Hoa Bó Tươi Nghệ Thuật', N'Hoa Quà Tặng'),
    (N'Giỏ & Hộp Hoa Cao Cấp', N'Hoa Quà Tặng'),
    (N'Kệ Hoa Khai Trương Hồng Phát', N'Hoa Khai Trương'),
    (N'Hoa Cưới & Cầm Tay Cô Dâu', N'Hoa Cưới'),
    (N'Lan Hồ Điệp Hoàng Gia', N'Hoa Quà Tặng'),
    (N'Phụ Kiện Hoa & Khung Sự Kiện', N'Hoa Trang Trí'),
    (N'Hoa Tình Yêu & Valentine', N'Hoa Tình Yêu'),
    (N'Lan Hồ Điệp Sang Trọng', N'Hoa Quà Tặng'),
    (N'Hoa Cưới Cầm Tay Cô Dâu', N'Hoa Cưới'),
    (N'Hoa Chúc Mừng & Tốt Nghiệp', N'Hoa Chúc Mừng'),
    (N'Hoa Chia Buồn & Kính Viếng', N'Hoa Chia Buồn'),
    (N'Bình Hoa Nghệ Thuật Để Bàn', N'Hoa Trang Trí'),
    (N'Hộp Hoa & Giỏ Hoa Quà Tặng', N'Hoa Quà Tặng'),
    (N'Bó Hoa Sinh Nhật', N'Hoa Sinh Nhật'),
    (N'Lẵng Hoa Sinh Nhật', N'Hoa Sinh Nhật'),
    (N'Bó Hoa Hồng Đỏ', N'Hoa Tình Yêu'),
    (N'Bó Hoa Baby Trắng', N'Hoa Tình Yêu'),
    (N'Kệ Hoa Khai Trương', N'Hoa Khai Trương'),
    (N'Lẵng Hoa Khai Trương Để Bàn', N'Hoa Khai Trương'),
    (N'Chậu Lan Hồ Điệp Trắng', N'Hoa Quà Tặng'),
    (N'Chậu Lan Hồ Điệp Vàng', N'Hoa Quà Tặng');

-- Preserve the previous assignment for traceability; no products/orders/stocks are deleted.
CREATE TABLE dbo.product_category_legacy (
    product_id BIGINT NOT NULL CONSTRAINT PK_product_category_legacy PRIMARY KEY,
    category_id BIGINT NOT NULL,
    CONSTRAINT FK_product_category_legacy_product FOREIGN KEY (product_id) REFERENCES dbo.products(id),
    CONSTRAINT FK_product_category_legacy_category FOREIGN KEY (category_id) REFERENCES dbo.categories(id)
);
INSERT INTO dbo.product_category_legacy(product_id, category_id)
SELECT p.id,p.category_id FROM dbo.products p JOIN dbo.categories c ON c.id=p.category_id
JOIN @mapping m ON m.old_name=c.name;

-- For general gift/display groups, explicit occasions in the product name take precedence.
UPDATE p SET category_id=target.id
FROM dbo.products p JOIN dbo.categories old ON old.id=p.category_id
JOIN @mapping m ON m.old_name=old.name
JOIN dbo.categories target ON target.name=CASE
    WHEN m.new_name IN (N'Hoa Quà Tặng',N'Hoa Trang Trí',N'Hoa Chúc Mừng') THEN
        CASE WHEN p.name LIKE N'%Sinh Nhật%' THEN N'Hoa Sinh Nhật'
             WHEN p.name LIKE N'%Khai Trương%' THEN N'Hoa Khai Trương'
             WHEN p.name LIKE N'%Tốt Nghiệp%' THEN N'Hoa Tốt Nghiệp'
             WHEN p.name LIKE N'%Tân Gia%' THEN N'Hoa Tân Gia'
             WHEN p.name LIKE N'%Hoa Cưới%' OR p.name LIKE N'%Cô Dâu%' THEN N'Hoa Cưới'
             WHEN p.name LIKE N'%Tình Yêu%' OR p.name LIKE N'%Valentine%' OR p.name LIKE N'%Tỏ Tình%' THEN N'Hoa Tình Yêu'
             ELSE m.new_name END
    ELSE m.new_name END;

-- Scoped attributes/custom children still point to a valid canonical category.
UPDATE s SET category_id=target.id FROM dbo.styles s JOIN dbo.categories old ON old.id=s.category_id
JOIN @mapping m ON m.old_name=old.name JOIN dbo.categories target ON target.name=m.new_name;
UPDATE child SET parent_id=target.id FROM dbo.categories child JOIN dbo.categories old ON old.id=child.parent_id
JOIN @mapping m ON m.old_name=old.name JOIN dbo.categories target ON target.name=m.new_name
WHERE NOT EXISTS (SELECT 1 FROM @mapping own WHERE own.old_name=child.name);
UPDATE c SET is_deleted=1 FROM dbo.categories c JOIN @mapping m ON m.old_name=c.name;
