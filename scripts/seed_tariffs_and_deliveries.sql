-- Cập nhật thông tin 3 đơn vị vận chuyển chuẩn
UPDATE deliveries 
SET name = N'Giao hàng nội bộ', 
    phone = '028 7108 8810', 
    price = 0, 
    description = N'Đội ngũ giao hoa nội bộ tiệm, trang bị thùng giữ ẩm chuyên dụng và che chắn hoa cẩn thận.',
    vehicle_type = N'Xe máy chuyên dụng giữ ẩm',
    coverage_area = N'Bán kính các chi nhánh toàn hệ thống',
    is_active = 1,
    is_deleted = 0
WHERE id = 1;

UPDATE deliveries 
SET name = N'GrabExpress', 
    phone = '1900 633 677', 
    price = 0, 
    description = N'Đối tác GrabExpress chuyên giao hàng hỏa tốc xe máy có lồng bảo hộ cánh hoa.',
    vehicle_type = N'Xe máy lồng kính bảo hộ',
    coverage_area = N'Nội thành TP.HCM và các thành phố chi nhánh',
    is_active = 1,
    is_deleted = 0
WHERE id = 2;

UPDATE deliveries 
SET name = N'Ahamove', 
    phone = '1900 545 411', 
    price = 0, 
    description = N'Đối tác Ahamove giao siêu tốc, chuyên đơn hàng hoa tươi và bánh kem nội thành.',
    vehicle_type = N'Tài xế chuyên giao hoa tươi',
    coverage_area = N'Toàn khu vực nội thành các chi nhánh',
    is_active = 1,
    is_deleted = 0
WHERE id = 3;

-- Cập nhật tọa độ cho chi nhánh Cao Lãnh nếu chưa có
UPDATE stores
SET latitude = 10.4597, longitude = 105.6325
WHERE id = 7 AND (latitude IS NULL OR longitude IS NULL);

-- Xóa các biểu phí cũ để nạp biểu phí chuẩn đầy đủ cho cả 4 chi nhánh
DELETE FROM shipping_tariffs;

-- CHI NHÁNH 1: BẾN THÀNH (QUẬN 1)
INSERT INTO shipping_tariffs (store_id, destination_area, service_name, delivery_id, base_fee, bulky_surcharge, estimated_time, note, is_active, is_deleted, created_at, updated_at)
VALUES 
(1, N'Khu vực gần chi nhánh (< 5km)', N'Tiêu chuẩn', 1, 25000, 20000, N'4 - 6 giờ', N'Giao hoa tươi giữ ẩm tận nơi Quận 1, 3, 4', 1, 0, GETDATE(), GETDATE()),
(1, N'Khu vực xa hơn (5 - 15km)', N'Tiêu chuẩn', 1, 40000, 25000, N'4 - 6 giờ', N'Giao các quận nội thành lân cận Q5, Q10, Bình Thạnh, Phú Nhuận', 1, 0, GETDATE(), GETDATE()),
(1, N'Khu vực gần chi nhánh (< 5km)', N'Hỏa tốc', 2, 45000, 30000, N'Trong vòng 2 giờ', N'Ưu tiên cắm và giao ngay trong lồng kính xe máy bảo vệ hoa', 1, 0, GETDATE(), GETDATE()),
(1, N'Khu vực xa hơn (5 - 15km)', N'Hỏa tốc', 2, 65000, 35000, N'Trong vòng 2 giờ', N'Hỏa tốc liên quận nội thành trong 2 giờ', 1, 0, GETDATE(), GETDATE()),
(1, N'Khu vực gần chi nhánh (< 5km)', N'Siêu tốc', 3, 50000, 30000, N'60 - 90 phút', N'Tài xế Ahamove giao siêu tốc hoa tươi hẹn giờ', 1, 0, GETDATE(), GETDATE());

-- CHI NHÁNH 2: PHÚ MỸ HƯNG (QUẬN 7)
INSERT INTO shipping_tariffs (store_id, destination_area, service_name, delivery_id, base_fee, bulky_surcharge, estimated_time, note, is_active, is_deleted, created_at, updated_at)
VALUES 
(2, N'Khu vực gần chi nhánh (< 5km)', N'Tiêu chuẩn', 1, 25000, 20000, N'4 - 6 giờ', N'Giao hoa tận nơi khu đô thị Phú Mỹ Hưng, Tân Phong, Tân Phú', 1, 0, GETDATE(), GETDATE()),
(2, N'Khu vực xa hơn (5 - 15km)', N'Tiêu chuẩn', 1, 40000, 25000, N'4 - 6 giờ', N'Giao các quận lân cận Q4, Q8, Nhà Bè, Bình Chánh', 1, 0, GETDATE(), GETDATE()),
(2, N'Khu vực gần chi nhánh (< 5km)', N'Hỏa tốc', 2, 45000, 30000, N'Trong vòng 2 giờ', N'Giao nhanh 2h khu đô thị Quận 7 bảo quản hoa thùng lạnh', 1, 0, GETDATE(), GETDATE()),
(2, N'Khu vực xa hơn (5 - 15km)', N'Hỏa tốc', 2, 65000, 35000, N'Trong vòng 2 giờ', N'Giao hỏa tốc từ Quận 7 sang các quận lân cận', 1, 0, GETDATE(), GETDATE()),
(2, N'Khu vực gần chi nhánh (< 5km)', N'Siêu tốc', 3, 50000, 30000, N'60 - 90 phút', N'Giao siêu tốc Ahamove đơn hoa tặng tiệc cưới, sự kiện', 1, 0, GETDATE(), GETDATE());

-- CHI NHÁNH 3: LÀNG ĐẠI HỌC (TP. THỦ ĐỨC)
INSERT INTO shipping_tariffs (store_id, destination_area, service_name, delivery_id, base_fee, bulky_surcharge, estimated_time, note, is_active, is_deleted, created_at, updated_at)
VALUES 
(3, N'Khu vực gần chi nhánh (< 5km)', N'Tiêu chuẩn', 1, 25000, 20000, N'4 - 6 giờ', N'Giao khu vực Làng Đại Học, Linh Chiểu, Linh Trung, ĐHQG', 1, 0, GETDATE(), GETDATE()),
(3, N'Khu vực xa hơn (5 - 15km)', N'Tiêu chuẩn', 1, 40000, 25000, N'4 - 6 giờ', N'Giao toàn khu vực TP. Thủ Đức (Q2, Q9 cũ) và Dĩ An', 1, 0, GETDATE(), GETDATE()),
(3, N'Khu vực gần chi nhánh (< 5km)', N'Hỏa tốc', 2, 45000, 30000, N'Trong vòng 2 giờ', N'Hỏa tốc 2 giờ đơn hoa chúc mừng tốt nghiệp, sinh nhật', 1, 0, GETDATE(), GETDATE()),
(3, N'Khu vực xa hơn (5 - 15km)', N'Hỏa tốc', 2, 65000, 35000, N'Trong vòng 2 giờ', N'Giao hỏa tốc Thủ Đức sang Bình Thạnh, Gò Vấp', 1, 0, GETDATE(), GETDATE()),
(3, N'Khu vực gần chi nhánh (< 5km)', N'Siêu tốc', 3, 50000, 30000, N'60 - 90 phút', N'Giao siêu tốc hoa tươi trong 60-90 phút', 1, 0, GETDATE(), GETDATE());

-- CHI NHÁNH 7: CAO LÃNH (ĐỒNG THÁP)
INSERT INTO shipping_tariffs (store_id, destination_area, service_name, delivery_id, base_fee, bulky_surcharge, estimated_time, note, is_active, is_deleted, created_at, updated_at)
VALUES 
(7, N'Khu vực gần chi nhánh (< 5km)', N'Tiêu chuẩn', 1, 20000, 15000, N'4 - 6 giờ', N'Giao hoa nội ô TP. Cao Lãnh (P1, P2, P3, P4, Mỹ Trà, An Bình)', 1, 0, GETDATE(), GETDATE()),
(7, N'Khu vực xa hơn (5 - 15km)', N'Tiêu chuẩn', 1, 35000, 20000, N'4 - 6 giờ', N'Giao huyện Cao Lãnh, Lấp Vò, vùng ven Đồng Tháp ≤ 15km', 1, 0, GETDATE(), GETDATE()),
(7, N'Khu vực gần chi nhánh (< 5km)', N'Hỏa tốc', 2, 40000, 25000, N'Trong vòng 2 giờ', N'Giao hoa tươi hỏa tốc nội ô TP. Cao Lãnh', 1, 0, GETDATE(), GETDATE()),
(7, N'Khu vực xa hơn (5 - 15km)', N'Hỏa tốc', 2, 55000, 30000, N'Trong vòng 2 giờ', N'Hỏa tốc vùng ven TP. Cao Lãnh và các huyện lân cận', 1, 0, GETDATE(), GETDATE()),
(7, N'Khu vực gần chi nhánh (< 5km)', N'Siêu tốc', 3, 45000, 25000, N'60 - 90 phút', N'Tài xế chuyên hoa tươi giao nhanh tận nhà', 1, 0, GETDATE(), GETDATE());
