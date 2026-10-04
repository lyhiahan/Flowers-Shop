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
        List<String> styles = (flowerStyle != null && !flowerStyle.trim().isEmpty()) ? List.of(flowerStyle.trim()) : null;
        List<String> types = (flowerType != null && !flowerType.trim().isEmpty()) ? List.of(flowerType.trim()) : null;
        List<String> colors = (flowerColor != null && !flowerColor.trim().isEmpty()) ? List.of(flowerColor.trim()) : null;
        List<String> occasions = (flowerOccasion != null && !flowerOccasion.trim().isEmpty()) ? List.of(flowerOccasion.trim()) : null;
        return filter(categoryIds, keyword, minPrice, maxPrice, minRating, storeId, styles, types, colors, occasions);
    }

    public static Specification<Product> filter(
            List<Long> categoryIds,
            String keyword,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Double minRating,
            Long storeId,
            List<String> flowerStyles,
            List<String> flowerTypes,
            List<String> flowerColors,
            List<String> flowerOccasions
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isTrue(root.get("isActive")));
            predicates.add(cb.isTrue(root.get("isSelling")));
            predicates.add(cb.isFalse(root.get("isDeleted")));
            predicates.add(cb.isTrue(root.get("store").get("status")));
            predicates.add(cb.isTrue(root.get("store").get("isOpen")));

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

            // 1. Lọc theo Kiểu dáng (Bó hoa, Giỏ hoa, Hộp hoa, Bình hoa / Lọ hoa, Chậu hoa, Kệ hoa) - Trong nhóm dùng OR
            if (flowerStyles != null && !flowerStyles.isEmpty()) {
                List<Predicate> stylePredicates = new ArrayList<>();
                for (String s : flowerStyles) {
                    if (s != null && !s.trim().isEmpty()) {
                        stylePredicates.add(buildStylePredicate(cb, root, s));
                    }
                }
                if (!stylePredicates.isEmpty()) {
                    predicates.add(cb.or(stylePredicates.toArray(new Predicate[0])));
                }
            }

            // 2. Lọc theo Loại hoa (Hoa hồng, Tulip, Hướng dương, Cẩm tú cầu, Lan hồ điệp, Baby...) - Trong nhóm dùng OR
            if (flowerTypes != null && !flowerTypes.isEmpty()) {
                List<Predicate> typePredicates = new ArrayList<>();
                for (String t : flowerTypes) {
                    if (t != null && !t.trim().isEmpty()) {
                        typePredicates.add(buildTypePredicate(cb, root, t));
                    }
                }
                if (!typePredicates.isEmpty()) {
                    predicates.add(cb.or(typePredicates.toArray(new Predicate[0])));
                }
            }

            // 3. Lọc theo Dịp tặng (Sinh nhật, Khai trương, Tình yêu, Chúc mừng, Tốt nghiệp, Chia buồn) - Trong nhóm dùng OR
            if (flowerOccasions != null && !flowerOccasions.isEmpty()) {
                List<Predicate> occasionPredicates = new ArrayList<>();
                for (String o : flowerOccasions) {
                    if (o != null && !o.trim().isEmpty()) {
                        occasionPredicates.add(buildOccasionPredicate(cb, root, o));
                    }
                }
                if (!occasionPredicates.isEmpty()) {
                    predicates.add(cb.or(occasionPredicates.toArray(new Predicate[0])));
                }
            }

            // 4. Lọc theo Màu sắc (Đỏ, Hồng, Vàng, Xanh, Trắng, Tím, Cam) - Trong nhóm dùng OR
            if (flowerColors != null && !flowerColors.isEmpty()) {
                List<Predicate> colorPredicates = new ArrayList<>();
                for (String c : flowerColors) {
                    if (c != null && !c.trim().isEmpty()) {
                        colorPredicates.add(buildColorPredicate(cb, root, c));
                    }
                }
                if (!colorPredicates.isEmpty()) {
                    predicates.add(cb.or(colorPredicates.toArray(new Predicate[0])));
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static Predicate buildStylePredicate(jakarta.persistence.criteria.CriteriaBuilder cb, jakarta.persistence.criteria.Root<Product> root, String s) {
        String lower = s.trim().toLowerCase();
        if (lower.contains("bó") || lower.contains("bo")) {
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
            return cb.and(isBo, notGio, notHop, notKe, notBinh, notLo, notChau);
        } else if (lower.contains("giỏ") || lower.contains("gio")) {
            return cb.or(
                    cb.like(cb.lower(root.get("name")), "giỏ%"),
                    cb.like(cb.lower(root.get("name")), "%giỏ hoa%")
            );
        } else if (lower.contains("hộp") || lower.contains("hop")) {
            return cb.or(
                    cb.like(cb.lower(root.get("name")), "hộp%"),
                    cb.like(cb.lower(root.get("name")), "%hộp hoa%")
            );
        } else if (lower.contains("bình") || lower.contains("binh") || lower.contains("lọ") || lower.contains("lo")) {
            return cb.or(
                    cb.like(cb.lower(root.get("name")), "bình%"),
                    cb.like(cb.lower(root.get("name")), "lọ%"),
                    cb.like(cb.lower(root.get("name")), "%bình hoa%"),
                    cb.like(cb.lower(root.get("name")), "%lọ hoa%")
            );
        } else if (lower.contains("chậu") || lower.contains("chau")) {
            return cb.or(
                    cb.like(cb.lower(root.get("name")), "chậu%"),
                    cb.like(cb.lower(root.get("name")), "%chậu hoa%"),
                    cb.like(cb.lower(root.get("name")), "%chậu lan%")
            );
        } else if (lower.contains("kệ") || lower.contains("ke")) {
            return cb.or(
                    cb.like(cb.lower(root.get("name")), "kệ%"),
                    cb.like(cb.lower(root.get("name")), "%kệ hoa%"),
                    cb.like(cb.lower(root.get("name")), "%2 tầng%")
            );
        } else {
            return cb.like(cb.lower(root.get("name")), "%" + lower + "%");
        }
    }

    private static Predicate buildTypePredicate(jakarta.persistence.criteria.CriteriaBuilder cb, jakarta.persistence.criteria.Root<Product> root, String t) {
        String lower = t.trim().toLowerCase();
        if (lower.contains("hồng") || lower.contains("rose")) {
            return cb.or(
                    cb.like(cb.lower(root.get("name")), "%hồng%"),
                    cb.like(cb.lower(root.get("description")), "%hồng%")
            );
        } else if (lower.contains("tulip")) {
            return cb.or(
                    cb.like(cb.lower(root.get("name")), "%tulip%"),
                    cb.like(cb.lower(root.get("description")), "%tulip%")
            );
        } else if (lower.contains("hướng dương") || lower.contains("sunflower")) {
            return cb.or(
                    cb.like(cb.lower(root.get("name")), "%hướng dương%"),
                    cb.like(cb.lower(root.get("description")), "%hướng dương%")
            );
        } else if (lower.contains("cẩm tú cầu") || lower.contains("hydrangea")) {
            return cb.or(
                    cb.like(cb.lower(root.get("name")), "%cẩm tú cầu%"),
                    cb.like(cb.lower(root.get("description")), "%cẩm tú cầu%")
            );
        } else if (lower.contains("lan") || lower.contains("hồ điệp")) {
            return cb.or(
                    cb.like(cb.lower(root.get("name")), "%lan%"),
                    cb.like(cb.lower(root.get("description")), "%lan%")
            );
        } else if (lower.contains("calla") || lower.contains("rum")) {
            return cb.or(
                    cb.like(cb.lower(root.get("name")), "%calla%"),
                    cb.like(cb.lower(root.get("name")), "%rum%"),
                    cb.like(cb.lower(root.get("description")), "%calla%")
            );
        } else if (lower.contains("baby")) {
            return cb.or(
                    cb.like(cb.lower(root.get("name")), "%baby%"),
                    cb.like(cb.lower(root.get("description")), "%baby%")
            );
        } else if (lower.contains("sen")) {
            return cb.or(
                    cb.like(cb.lower(root.get("name")), "%sen%"),
                    cb.like(cb.lower(root.get("description")), "%sen%")
            );
        } else if (lower.contains("cúc")) {
            return cb.or(
                    cb.like(cb.lower(root.get("name")), "%cúc%"),
                    cb.like(cb.lower(root.get("description")), "%cúc%")
            );
        } else {
            return cb.or(
                    cb.like(cb.lower(root.get("name")), "%" + lower + "%"),
                    cb.like(cb.lower(root.get("description")), "%" + lower + "%")
            );
        }
    }

    private static Predicate buildOccasionPredicate(jakarta.persistence.criteria.CriteriaBuilder cb, jakarta.persistence.criteria.Root<Product> root, String o) {
        String lower = o.trim().toLowerCase();
        if (lower.contains("sinh nhật")) {
            return cb.or(
                    cb.like(cb.lower(root.get("category").get("name")), "%sinh nhật%"),
                    cb.like(cb.lower(root.get("name")), "%sinh nhật%"),
                    cb.like(cb.lower(root.get("description")), "%sinh nhật%")
            );
        } else if (lower.contains("khai trương")) {
            return cb.or(
                    cb.like(cb.lower(root.get("category").get("name")), "%khai trương%"),
                    cb.like(cb.lower(root.get("name")), "%khai trương%"),
                    cb.like(cb.lower(root.get("description")), "%khai trương%")
            );
        } else if (lower.contains("tình yêu") || lower.contains("valentine") || lower.contains("kỷ niệm")) {
            return cb.or(
                    cb.like(cb.lower(root.get("category").get("name")), "%tình yêu%"),
                    cb.like(cb.lower(root.get("name")), "%tình yêu%"),
                    cb.like(cb.lower(root.get("name")), "%cưới%"),
                    cb.like(cb.lower(root.get("description")), "%tình yêu%"),
                    cb.like(cb.lower(root.get("description")), "%kỷ niệm%")
            );
        } else if (lower.contains("chúc mừng") || lower.contains("tốt nghiệp")) {
            return cb.or(
                    cb.like(cb.lower(root.get("category").get("name")), "%chúc mừng%"),
                    cb.like(cb.lower(root.get("name")), "%chúc mừng%"),
                    cb.like(cb.lower(root.get("name")), "%tốt nghiệp%"),
                    cb.like(cb.lower(root.get("description")), "%chúc mừng%")
            );
        } else if (lower.contains("chia buồn") || lower.contains("tang")) {
            return cb.or(
                    cb.like(cb.lower(root.get("name")), "%chia buồn%"),
                    cb.like(cb.lower(root.get("description")), "%chia buồn%")
            );
        } else if (lower.contains("cưới")) {
            return cb.or(
                    cb.like(cb.lower(root.get("name")), "%cưới%"),
                    cb.like(cb.lower(root.get("description")), "%cưới%")
            );
        } else {
            return cb.or(
                    cb.like(cb.lower(root.get("category").get("name")), "%" + lower + "%"),
                    cb.like(cb.lower(root.get("name")), "%" + lower + "%"),
                    cb.like(cb.lower(root.get("description")), "%" + lower + "%")
            );
        }
    }

    private static Predicate buildColorPredicate(jakarta.persistence.criteria.CriteriaBuilder cb, jakarta.persistence.criteria.Root<Product> root, String c) {
        String lower = c.trim().toLowerCase();
        return cb.or(
                cb.like(cb.lower(root.get("name")), "%" + lower + "%"),
                cb.like(cb.lower(root.get("description")), "%" + lower + "%")
        );
    }
}
