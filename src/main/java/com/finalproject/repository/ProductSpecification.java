package com.finalproject.repository;

import com.finalproject.entity.Product;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class ProductSpecification {

    public static Specification<Product> filter(
            List<Long> categoryIds,
            String keyword,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Double minRating,
            Long storeId,
            String flowerStyle,
            String flowerType,
            String flowerColor,
            String flowerOccasion
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isTrue(root.get("isActive")));
            predicates.add(cb.isTrue(root.get("isSelling")));

            if (categoryIds != null && !categoryIds.isEmpty()) {
                predicates.add(root.get("category").get("id").in(categoryIds));
            }

            if (keyword != null && !keyword.trim().isEmpty()) {
                String kw = "%" + keyword.trim().toLowerCase() + "%";
                Predicate nameLike = cb.like(cb.lower(root.get("name")), kw);
                Predicate descLike = cb.like(cb.lower(root.get("description")), kw);
                predicates.add(cb.or(nameLike, descLike));

                if (query != null && query.getResultType() != Long.class && query.getResultType() != long.class) {
                    query.orderBy(
                            cb.asc(cb.selectCase().when(cb.like(cb.lower(root.get("name")), kw), 0).otherwise(1)),
                            cb.desc(root.get("id"))
                    );
                }
            }

            if (minPrice != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("priceBuy"), minPrice));
            }
            if (maxPrice != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("priceBuy"), maxPrice));
            }
            if (minRating != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("rating"), minRating));
            }
            if (storeId != null) {
                predicates.add(cb.equal(root.get("store").get("id"), storeId));
            }

            // 1. Lọc theo Kiểu dáng (Bó hoa, Giỏ hoa, Hộp hoa, Bình hoa / Lọ hoa, Chậu hoa, Kệ hoa)
            if (flowerStyle != null && !flowerStyle.trim().isEmpty()) {
                String s = flowerStyle.trim().toLowerCase();
                if (s.contains("bó") || s.contains("bo")) {
                    Predicate isBo = cb.or(
                            cb.like(cb.lower(root.get("name")), "bó %"),
                            cb.like(cb.lower(root.get("name")), "%bó hoa%"),
                            cb.like(cb.lower(root.get("description")), "%bó hoa%")
                    );
                    Predicate notGio = cb.notLike(cb.lower(root.get("name")), "giỏ%");
                    Predicate notHop = cb.notLike(cb.lower(root.get("name")), "hộp%");
                    Predicate notKe = cb.notLike(cb.lower(root.get("name")), "kệ%");
                    Predicate notBinh = cb.notLike(cb.lower(root.get("name")), "bình%");
                    Predicate notLo = cb.notLike(cb.lower(root.get("name")), "lọ%");
                    Predicate notChau = cb.notLike(cb.lower(root.get("name")), "chậu%");
                    predicates.add(cb.and(isBo, notGio, notHop, notKe, notBinh, notLo, notChau));
                } else if (s.contains("giỏ") || s.contains("gio")) {
                    predicates.add(cb.or(
                            cb.like(cb.lower(root.get("name")), "giỏ%"),
                            cb.like(cb.lower(root.get("name")), "%giỏ hoa%")
                    ));
                } else if (s.contains("hộp") || s.contains("hop")) {
                    predicates.add(cb.or(
                            cb.like(cb.lower(root.get("name")), "hộp%"),
                            cb.like(cb.lower(root.get("name")), "%hộp hoa%")
                    ));
                } else if (s.contains("bình") || s.contains("binh") || s.contains("lọ") || s.contains("lo")) {
                    predicates.add(cb.or(
                            cb.like(cb.lower(root.get("name")), "bình%"),
                            cb.like(cb.lower(root.get("name")), "lọ%"),
                            cb.like(cb.lower(root.get("name")), "%bình hoa%"),
                            cb.like(cb.lower(root.get("name")), "%lọ hoa%")
                    ));
                } else if (s.contains("chậu") || s.contains("chau")) {
                    predicates.add(cb.or(
                            cb.like(cb.lower(root.get("name")), "chậu%"),
                            cb.like(cb.lower(root.get("name")), "%chậu hoa%"),
                            cb.like(cb.lower(root.get("name")), "%chậu lan%")
                    ));
                } else if (s.contains("kệ") || s.contains("ke")) {
                    predicates.add(cb.or(
                            cb.like(cb.lower(root.get("name")), "kệ%"),
                            cb.like(cb.lower(root.get("name")), "%kệ hoa%"),
                            cb.like(cb.lower(root.get("name")), "%2 tầng%")
                    ));
                } else {
                    predicates.add(cb.like(cb.lower(root.get("name")), "%" + s + "%"));
                }
            }

            // 2. Lọc theo Loại hoa (Hoa hồng, Tulip, Hướng dương, Cẩm tú cầu, Lan hồ điệp, Baby...)
            if (flowerType != null && !flowerType.trim().isEmpty()) {
                String t = flowerType.trim().toLowerCase();
                if (t.contains("hồng") || t.contains("rose")) {
                    predicates.add(cb.or(
                            cb.like(cb.lower(root.get("name")), "%hồng%"),
                            cb.like(cb.lower(root.get("description")), "%hồng%")
                    ));
                } else if (t.contains("tulip")) {
                    predicates.add(cb.or(
                            cb.like(cb.lower(root.get("name")), "%tulip%"),
                            cb.like(cb.lower(root.get("description")), "%tulip%")
                    ));
                } else if (t.contains("hướng dương") || t.contains("sunflower")) {
                    predicates.add(cb.or(
                            cb.like(cb.lower(root.get("name")), "%hướng dương%"),
                            cb.like(cb.lower(root.get("description")), "%hướng dương%")
                    ));
                } else if (t.contains("cẩm tú cầu") || t.contains("hydrangea")) {
                    predicates.add(cb.or(
                            cb.like(cb.lower(root.get("name")), "%cẩm tú cầu%"),
                            cb.like(cb.lower(root.get("description")), "%cẩm tú cầu%")
                    ));
                } else if (t.contains("lan") || t.contains("hồ điệp")) {
                    predicates.add(cb.or(
                            cb.like(cb.lower(root.get("name")), "%lan%"),
                            cb.like(cb.lower(root.get("description")), "%lan%")
                    ));
                } else if (t.contains("baby")) {
                    predicates.add(cb.or(
                            cb.like(cb.lower(root.get("name")), "%baby%"),
                            cb.like(cb.lower(root.get("description")), "%baby%")
                    ));
                } else if (t.contains("sen")) {
                    predicates.add(cb.or(
                            cb.like(cb.lower(root.get("name")), "%sen%"),
                            cb.like(cb.lower(root.get("description")), "%sen%")
                    ));
                } else if (t.contains("cúc")) {
                    predicates.add(cb.or(
                            cb.like(cb.lower(root.get("name")), "%cúc%"),
                            cb.like(cb.lower(root.get("description")), "%cúc%")
                    ));
                } else {
                    predicates.add(cb.or(
                            cb.like(cb.lower(root.get("name")), "%" + t + "%"),
                            cb.like(cb.lower(root.get("description")), "%" + t + "%")
                    ));
                }
            }

            // 3. Lọc theo Dịp tặng (Sinh nhật, Khai trương, Tình yêu, Chúc mừng, Tốt nghiệp, Chia buồn)
            if (flowerOccasion != null && !flowerOccasion.trim().isEmpty()) {
                String o = flowerOccasion.trim().toLowerCase();
                if (o.contains("sinh nhật")) {
                    predicates.add(cb.or(
                            cb.like(cb.lower(root.get("category").get("name")), "%sinh nhật%"),
                            cb.like(cb.lower(root.get("name")), "%sinh nhật%"),
                            cb.like(cb.lower(root.get("description")), "%sinh nhật%")
                    ));
                } else if (o.contains("khai trương")) {
                    predicates.add(cb.or(
                            cb.like(cb.lower(root.get("category").get("name")), "%khai trương%"),
                            cb.like(cb.lower(root.get("name")), "%khai trương%"),
                            cb.like(cb.lower(root.get("description")), "%khai trương%")
                    ));
                } else if (o.contains("tình yêu") || o.contains("valentine") || o.contains("kỷ niệm")) {
                    predicates.add(cb.or(
                            cb.like(cb.lower(root.get("category").get("name")), "%tình yêu%"),
                            cb.like(cb.lower(root.get("name")), "%tình yêu%"),
                            cb.like(cb.lower(root.get("name")), "%cưới%"),
                            cb.like(cb.lower(root.get("description")), "%tình yêu%"),
                            cb.like(cb.lower(root.get("description")), "%kỷ niệm%")
                    ));
                } else if (o.contains("chúc mừng") || o.contains("tốt nghiệp")) {
                    predicates.add(cb.or(
                            cb.like(cb.lower(root.get("category").get("name")), "%chúc mừng%"),
                            cb.like(cb.lower(root.get("name")), "%chúc mừng%"),
                            cb.like(cb.lower(root.get("name")), "%tốt nghiệp%"),
                            cb.like(cb.lower(root.get("description")), "%chúc mừng%")
                    ));
                } else if (o.contains("chia buồn") || o.contains("tang")) {
                    predicates.add(cb.or(
                            cb.like(cb.lower(root.get("name")), "%chia buồn%"),
                            cb.like(cb.lower(root.get("description")), "%chia buồn%")
                    ));
                } else {
                    predicates.add(cb.or(
                            cb.like(cb.lower(root.get("category").get("name")), "%" + o + "%"),
                            cb.like(cb.lower(root.get("name")), "%" + o + "%"),
                            cb.like(cb.lower(root.get("description")), "%" + o + "%")
                    ));
                }
            }

            // 4. Lọc theo Màu sắc (Đỏ, Hồng, Vàng, Xanh, Trắng, Tím, Cam)
            if (flowerColor != null && !flowerColor.trim().isEmpty()) {
                String c = flowerColor.trim().toLowerCase();
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), "%" + c + "%"),
                        cb.like(cb.lower(root.get("description")), "%" + c + "%")
                ));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
