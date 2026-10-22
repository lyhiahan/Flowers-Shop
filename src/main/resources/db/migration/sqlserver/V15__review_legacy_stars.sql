-- Giữ tương thích với bảng reviews cũ có stars NOT NULL, không xóa đánh giá đã lưu.
IF COL_LENGTH('dbo.reviews', 'stars') IS NULL
    ALTER TABLE dbo.reviews ADD stars int NULL;
GO
UPDATE dbo.reviews SET stars = COALESCE(rating, 5) WHERE stars IS NULL;
GO
