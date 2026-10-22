-- Replace only the known missing sample photo for the 20-rose demo bouquet.
-- Keep operator-uploaded photos and other products unchanged.
UPDATE dbo.products SET image_url = '/images/demo-rose-bouquet.jpg'
WHERE image_url = 'https://images.unsplash.com/photo-1548094878-84ced0f68b08?w=800'
  AND name IN (
      N'Bó Hoa Hồng Đỏ 20 Bông',
      N'Bó Hoa Hồng Nhập Khẩu 20 Bông ''Tỏ Tình Lãng Mạn'''
  );
