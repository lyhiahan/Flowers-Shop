-- =====================================================================
-- KỊCH BẢN TẠO DỮ LIỆU ĐÁNH GIÁ MẪU CHÂN THỰC CHO ĐỒ ÁN (SQL SERVER)
-- Điểm sao phân bổ tự nhiên: 3, 4, 5 sao => Trung bình từ 4.2 đến 4.9 sao
-- Có nhận xét bằng tiếng Việt thực tế, độ dài > 50 ký tự
-- =====================================================================

SET NOCOUNT ON;
SET QUOTED_IDENTIFIER ON;
SET ANSI_NULLS ON;

-- Bảng tạm lưu trữ mẫu bình luận thực tế
IF OBJECT_ID('tempdb..#SampleComments') IS NOT NULL DROP TABLE #SampleComments;
CREATE TABLE #SampleComments (
    id INT IDENTITY(1,1),
    stars INT,
    content NVARCHAR(500)
);

INSERT INTO #SampleComments (stars, content) VALUES
(5, N'Hoa nhận được rất tươi và thơm, cắm dáng chuẩn y hệt hình mẫu trên web. Shop còn tặng kèm thiệp chúc mừng viết tay rất nắn nót. Sẽ ủng hộ shop dài dài!'),
(5, N'Mẫu hoa phối màu rất trang nhã, các bông hoa chính nở đều đẹp. Giao hàng đúng khung giờ đã hẹn trước, shipper nhiệt tình và cẩn thận.'),
(4, N'Tổng thể bó hoa rất xinh xắn và bắt mắt, người nhận rất thích. Màu hoa ngoài đời hơi đậm hơn trên hình chụp một chút nhưng vẫn rất ưng ý.'),
(5, N'Hoa đẹp, cánh hoa cứng cáp và tươi lâu được gần cả tuần. Đóng gói rất cẩn thận, bọc xốp giữ ẩm đầy đủ. Rất hài lòng về dịch vụ của tiệm.'),
(4, N'Giao hoa nhanh đúng dịp sinh nhật bạn mình. Thiệp và nơ phối cùng tông hài hòa. Giá cả hợp lý so với chất lượng hoa tươi tại shop.'),
(3, N'Chất lượng hoa tươi tắn, cắm đều tay. Thời gian giao hàng hơi sát giờ hẹn một chút nhưng nhân viên có chủ động gọi điện báo trước nên thông cảm được.'),
(5, N'Bó hoa to và đầy đặn hơn mình nghĩ, hoa cắm chắc tay. Lúc đặt có nhờ shop đổi màu giấy gói và shop hỗ trợ rất chu đáo. Cho shop 5 sao!'),
(4, N'Hoa tươi, nở đều và thơm dịu. Giữ được khoảng 4-5 ngày. Nhìn chung thiết kế đẹp, nhân viên tư vấn tận tình, rất đáng tiền.'),
(5, N'Shop cắm hoa đúng mẫu, hoa tươi rói lúc nhận. Shipper gọi điện trước 15 phút rất lịch sự. Sẽ tiếp tục đặt hoa ở đây cho các dịp kỷ niệm sau.'),
(4, N'Mẫu hoa thanh lịch, giấy gói sang trọng. Chỉ có một vài cánh hoa phụ hơi dập nhẹ do vận chuyển nhưng không ảnh hưởng nhiều đến dáng hoa chung.'),
(5, N'Hoa đẹp xuất sắc, mùi thơm tự nhiên rất dễ chịu. Người nhận khen nức nở và hỏi ngay địa chỉ tiệm. Cảm ơn shop rất nhiều!'),
(4, N'Đúng giờ, hoa tươi, tông màu ngọt ngào rất hợp làm quà tặng sinh nhật. Giá cả phải chăng, lần sau sẽ ghé ủng hộ tiếp.');

-- Bảng tạm lưu trữ danh sách khách hàng thật
IF OBJECT_ID('tempdb..#SampleUsers') IS NOT NULL DROP TABLE #SampleUsers;
CREATE TABLE #SampleUsers (
    seq INT IDENTITY(1,1),
    user_id BIGINT
);

INSERT INTO #SampleUsers (user_id)
SELECT id FROM users WHERE role = 'USER' AND is_active = 1
ORDER BY id;

DECLARE @UserCount INT = (SELECT COUNT(*) FROM #SampleUsers);
DECLARE @CommentCount INT = (SELECT COUNT(*) FROM #SampleComments);

-- Duyệt qua từng sản phẩm có lượt bán (sold > 0)
DECLARE @ProdId BIGINT, @ProdName NVARCHAR(255), @ProdSold INT, @StoreId BIGINT, @ImgUrl NVARCHAR(500);

DECLARE prod_cursor CURSOR FOR
SELECT id, name, sold, store_id, image_url
FROM products
WHERE ISNULL(is_deleted, 0) = 0 AND is_active = 1 AND ISNULL(sold, 0) > 0
ORDER BY id;

OPEN prod_cursor;
FETCH NEXT FROM prod_cursor INTO @ProdId, @ProdName, @ProdSold, @StoreId, @ImgUrl;

DECLARE @InsertedCount INT = 0;

WHILE @@FETCH_STATUS = 0
BEGIN
    -- Số lượng review mẫu cần tạo dựa trên số lượng bán (từ 2 đến 4 review/sản phẩm)
    DECLARE @TargetReviews INT = CASE 
        WHEN @ProdSold >= 70 THEN 4
        WHEN @ProdSold >= 45 THEN 3
        ELSE 2
    END;

    -- Kiểm tra số review hiện tại của sản phẩm
    DECLARE @CurrentReviews INT = (SELECT COUNT(*) FROM reviews WHERE product_id = @ProdId);
    DECLARE @ReviewsToAdd INT = @TargetReviews - @CurrentReviews;

    IF @ReviewsToAdd > 0
    BEGIN
        DECLARE @i INT = 0;
        WHILE @i < @ReviewsToAdd
        BEGIN
            -- Chọn user quay vòng
            DECLARE @UserSeq INT = ((@ProdId + @i + @CurrentReviews) % @UserCount) + 1;
            DECLARE @SelectedUserId BIGINT = (SELECT user_id FROM #SampleUsers WHERE seq = @UserSeq);

            -- Chọn bình luận quay vòng
            DECLARE @CommentSeq INT = ((@ProdId * 3 + @i * 2 + @CurrentReviews) % @CommentCount) + 1;
            DECLARE @SelectedStars INT = (SELECT stars FROM #SampleComments WHERE id = @CommentSeq);
            DECLARE @SelectedContent NVARCHAR(500) = (SELECT content FROM #SampleComments WHERE id = @CommentSeq);

            -- Ngày đánh giá trong khoảng 3 đến 20 ngày trước
            DECLARE @DaysAgo INT = ((@ProdId * 7 + @i * 5) % 18) + 2;
            DECLARE @ReviewDate DATETIME2 = DATEADD(DAY, -@DaysAgo, DATEADD(HOUR, -((@ProdId + @i) % 12), GETDATE()));

            -- Kiểm tra xem user này đã review sản phẩm này chưa (để tránh lặp)
            IF NOT EXISTS (SELECT 1 FROM reviews WHERE product_id = @ProdId AND user_id = @SelectedUserId)
            BEGIN
                INSERT INTO reviews (user_id, product_id, store_id, order_id, rating, content, image_url, created_at, updated_at)
                VALUES (@SelectedUserId, @ProdId, @StoreId, NULL, @SelectedStars, @SelectedContent, @ImgUrl, @ReviewDate, @ReviewDate);

                SET @InsertedCount = @InsertedCount + 1;
            END

            SET @i = @i + 1;
        END
    END

    FETCH NEXT FROM prod_cursor INTO @ProdId, @ProdName, @ProdSold, @StoreId, @ImgUrl;
END;

CLOSE prod_cursor;
DEALLOCATE prod_cursor;

-- Cập nhật lại cột rating trong bảng products bằng trung bình thực tế từ bảng reviews
UPDATE p
SET p.rating = ROUND(sub.avg_r, 1)
FROM products p
CROSS APPLY (
    SELECT AVG(r.rating * 1.0) AS avg_r
    FROM reviews r
    WHERE r.product_id = p.id
) sub
WHERE sub.avg_r IS NOT NULL;

-- Cập nhật lại cột rating trong bảng stores theo trung bình đánh giá cửa hàng
UPDATE s
SET s.rating = ROUND(sub.avg_r, 1)
FROM stores s
CROSS APPLY (
    SELECT AVG(r.rating * 1.0) AS avg_r
    FROM reviews r
    WHERE r.store_id = s.id
) sub
WHERE sub.avg_r IS NOT NULL;

PRINT N'Hoàn tất! Đã thêm ' + CAST(@InsertedCount AS NVARCHAR(10)) + N' đánh giá mẫu chân thực.';
