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
            // A paused product remains in the catalog with a disabled purchase action.
            // Legacy rows predate soft deletion; NULL means not deleted, as in Product.getIsDeleted().
            predicates.add(cb.or(cb.isFalse(root.get("isDeleted")), cb.isNull(root.get("isDeleted"))));
            var ownerStore = root.join("store", jakarta.persistence.criteria.JoinType.LEFT);
            predicates.add(cb.or(cb.isNull(ownerStore.get("id")), cb.and(cb.isTrue(ownerStore.get("status")), cb.isTrue(ownerStore.get("isOpen")))));

            if (categoryIds != null && !categoryIds.isEmpty()) {
                var extraQuery = query.subquery(Long.class);
                var extraProduct = extraQuery.from(Product.class);
                var extra = extraProduct.join("additionalCategories");
                extraQuery.select(extraProduct.get("id")).where(cb.equal(extraProduct.get("id"), root.get("id")),
                        extra.get("id").in(categoryIds), cb.or(cb.isFalse(extra.get("isDeleted")), cb.isNull(extra.get("isDeleted"))));
                predicates.add(cb.or(root.get("category").get("id").in(categoryIds), cb.exists(extraQuery)));
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

            var sellingPrice = cb.<BigDecimal>selectCase().when(cb.and(cb.isNotNull(root.get("promotionalPrice")),
                    cb.greaterThanOrEqualTo(root.get("promotionalPrice"), BigDecimal.ONE),
                    cb.equal(cb.function("floor", BigDecimal.class, root.get("promotionalPrice")), root.get("promotionalPrice")),
                    cb.lessThanOrEqualTo(root.get("promotionalPrice"), root.get("priceBuy"))), root.<BigDecimal>get("promotionalPrice"))
                    .otherwise(root.<BigDecimal>get("priceBuy"));
            if (minPrice != null) predicates.add(cb.greaterThanOrEqualTo(sellingPrice, minPrice));
            if (maxPrice != null) predicates.add(cb.lessThanOrEqualTo(sellingPrice, maxPrice));
            if (minRating != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("rating"), minRating));
            }
            if (storeId != null) {
                var inventory = query.subquery(Long.class);
                var stock = inventory.from(com.finalproject.entity.StoreInventory.class);
                inventory.select(stock.get("id")).where(cb.equal(stock.get("product").get("id"), root.get("id")), cb.equal(stock.get("store").get("id"), storeId),
                        cb.isFalse(stock.get("isDeleted")));
                predicates.add(cb.or(cb.equal(ownerStore.get("id"), storeId), cb.and(cb.isNull(ownerStore.get("id")), cb.exists(inventory))));
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
                    predicates.add(attributeFilter(root, query, cb, List.of("kiểu dáng", "kiểu dáng cắm", "kiểu dáng hoa", "phong cách cắm"), flowerStyles, cb.or(stylePredicates.toArray(new Predicate[0]))));
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
                    predicates.add(attributeFilter(root, query, cb, List.of("loại hoa"), flowerTypes, cb.or(typePredicates.toArray(new Predicate[0]))));
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
                    var extraQuery = query.subquery(Long.class);
                    var extraProduct = extraQuery.from(Product.class);
                    var extraCategory = extraProduct.join("additionalCategories");
                    var names = new ArrayList<Predicate>();
                    var mainNames = new ArrayList<Predicate>();
                    for (String occasion : flowerOccasions) {
                        if (occasion == null || occasion.isBlank()) continue;
                        String term = occasion.trim().toLowerCase(java.util.Locale.ROOT);
                        if (term.contains("valentine") || term.contains("kỷ niệm")) term = "tình yêu";
                        if (term.contains("cưới")) term = "cưới";
                        names.add(cb.like(cb.lower(extraCategory.get("name")), "%" + term + "%"));
                        mainNames.add(cb.like(cb.lower(root.get("category").get("name")), "%" + term + "%"));
                    }
                    extraQuery.select(extraProduct.get("id")).where(cb.equal(extraProduct.get("id"), root.get("id")),
                            cb.or(names.toArray(new Predicate[0])), cb.or(cb.isFalse(extraCategory.get("isDeleted")), cb.isNull(extraCategory.get("isDeleted"))));
                    predicates.add(cb.or(cb.or(mainNames.toArray(new Predicate[0])), cb.exists(extraQuery),
                            attributeFilter(root, query, cb, List.of("dịp tặng"), flowerOccasions, cb.or(occasionPredicates.toArray(new Predicate[0])))));
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
                    predicates.add(attributeFilter(root, query, cb, List.of("màu sắc", "tone màu hoa", "tone màu thiết kế"), flowerColors, cb.or(colorPredicates.toArray(new Predicate[0]))));
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static Predicate attributeFilter(jakarta.persistence.criteria.Root<Product> root,
            jakarta.persistence.criteria.CriteriaQuery<?> query, jakarta.persistence.criteria.CriteriaBuilder cb,
            List<String> groups, List<String> selected, Predicate legacy) {
        var any = query.subquery(Long.class);
        var source = any.from(Product.class);
        var assigned = source.join("styleValues");
        any.select(source.get("id")).where(cb.equal(source.get("id"), root.get("id")),
                cb.lower(assigned.get("style").get("name")).in(groups),
                cb.or(cb.isFalse(assigned.get("isDeleted")), cb.isNull(assigned.get("isDeleted"))),
                cb.or(cb.isFalse(assigned.get("style").get("isDeleted")), cb.isNull(assigned.get("style").get("isDeleted"))));
        var matching = query.subquery(Long.class);
        var product = matching.from(Product.class);
        var value = product.join("styleValues");
        matching.select(product.get("id")).where(cb.equal(product.get("id"), root.get("id")),
                cb.lower(value.get("style").get("name")).in(groups),
                cb.lower(value.get("name")).in(selected.stream().map(v -> v.trim().toLowerCase(java.util.Locale.ROOT)).toList()),
                cb.or(cb.isFalse(value.get("isDeleted")), cb.isNull(value.get("isDeleted"))),
                cb.or(cb.isFalse(value.get("style").get("isDeleted")), cb.isNull(value.get("style").get("isDeleted"))));
        // Explicit attributes take precedence. Legacy products remain searchable until classified.
        return cb.or(cb.exists(matching), cb.and(cb.not(cb.exists(any)), legacy));
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
                    cb.like(cb.lower(root.get("name")), "%hoa hồng%"),
                    cb.like(cb.lower(root.get("name")), "%bó hồng%"),
                    cb.like(cb.lower(root.get("name")), "%giỏ hồng%"),
                    cb.like(cb.lower(root.get("name")), "%hộp hồng%"),
                    cb.like(cb.lower(root.get("name")), "%bình hồng%"),
                    cb.like(cb.lower(root.get("name")), "%lọ hồng%"),
                    cb.like(cb.lower(root.get("name")), "%lẵng hồng%"),
                    cb.like(cb.lower(root.get("name")), "%cành hồng%"),
                    cb.like(cb.lower(root.get("name")), "%bông hồng%"),
                    cb.like(cb.lower(root.get("name")), "%hồng đỏ%"),
                    cb.like(cb.lower(root.get("name")), "%hồng vàng%"),
                    cb.like(cb.lower(root.get("name")), "%hồng trắng%"),
                    cb.like(cb.lower(root.get("name")), "%hồng kem%"),
                    cb.like(cb.lower(root.get("name")), "%hồng phấn%"),
                    cb.like(cb.lower(root.get("name")), "%hồng cam%"),
                    cb.like(cb.lower(root.get("name")), "%hồng tím%"),
                    cb.like(cb.lower(root.get("name")), "%hồng nhung%"),
                    cb.like(cb.lower(root.get("name")), "%hồng ecuador%"),
                    cb.like(cb.lower(root.get("name")), "%hồng ohara%"),
                    cb.like(cb.lower(root.get("name")), "%rose%"),
                    cb.like(cb.lower(root.get("description")), "%hoa hồng%"),
                    cb.like(cb.lower(root.get("description")), "%bông hồng%"),
                    cb.like(cb.lower(root.get("description")), "%cành hồng%"),
                    cb.like(cb.lower(root.get("description")), "%rose%")
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
        if (lower.equals("hồng") || lower.contains("màu hồng") || lower.contains("pink")) {
            return cb.or(
                    cb.like(cb.lower(root.get("name")), "%màu hồng%"),
                    cb.like(cb.lower(root.get("name")), "%tone hồng%"),
                    cb.like(cb.lower(root.get("name")), "%sắc hồng%"),
                    cb.like(cb.lower(root.get("name")), "%hồng phấn%"),
                    cb.like(cb.lower(root.get("name")), "%hồng pastel%"),
                    cb.like(cb.lower(root.get("name")), "%hồng nhạt%"),
                    cb.like(cb.lower(root.get("name")), "%hồng sen%"),
                    cb.like(cb.lower(root.get("name")), "%hồng kem%"),
                    cb.like(cb.lower(root.get("name")), "%hồng baby%"),
                    cb.like(cb.lower(root.get("name")), "%pink%"),
                    cb.like(cb.lower(root.get("description")), "%màu hồng%"),
                    cb.like(cb.lower(root.get("description")), "%tone hồng%"),
                    cb.like(cb.lower(root.get("description")), "%hồng phấn%"),
                    cb.like(cb.lower(root.get("description")), "%hồng pastel%"),
                    cb.like(cb.lower(root.get("description")), "%pink%")
            );
        } else if (lower.contains("đỏ") || lower.contains("red")) {
            return cb.or(
                    cb.like(cb.lower(root.get("name")), "%đỏ%"),
                    cb.like(cb.lower(root.get("name")), "%red%"),
                    cb.like(cb.lower(root.get("description")), "%đỏ%"),
                    cb.like(cb.lower(root.get("description")), "%red%")
            );
        } else if (lower.contains("vàng") || lower.contains("yellow")) {
            return cb.or(
                    cb.like(cb.lower(root.get("name")), "%vàng%"),
                    cb.like(cb.lower(root.get("name")), "%yellow%"),
                    cb.like(cb.lower(root.get("description")), "%vàng%"),
                    cb.like(cb.lower(root.get("description")), "%yellow%")
            );
        } else if (lower.contains("trắng") || lower.contains("white")) {
            return cb.or(
                    cb.like(cb.lower(root.get("name")), "%trắng%"),
                    cb.like(cb.lower(root.get("name")), "%white%"),
                    cb.like(cb.lower(root.get("description")), "%trắng%"),
                    cb.like(cb.lower(root.get("description")), "%white%")
            );
        } else if (lower.contains("tím") || lower.contains("purple") || lower.contains("violet")) {
            return cb.or(
                    cb.like(cb.lower(root.get("name")), "%tím%"),
                    cb.like(cb.lower(root.get("name")), "%purple%"),
                    cb.like(cb.lower(root.get("name")), "%violet%"),
                    cb.like(cb.lower(root.get("description")), "%tím%"),
                    cb.like(cb.lower(root.get("description")), "%purple%")
            );
        } else if (lower.contains("xanh") || lower.contains("blue") || lower.contains("green")) {
            return cb.or(
                    cb.like(cb.lower(root.get("name")), "%xanh%"),
                    cb.like(cb.lower(root.get("name")), "%blue%"),
                    cb.like(cb.lower(root.get("name")), "%green%"),
                    cb.like(cb.lower(root.get("description")), "%xanh%"),
                    cb.like(cb.lower(root.get("description")), "%blue%")
            );
        } else if (lower.contains("cam") || lower.contains("orange")) {
            return cb.or(
                    cb.like(cb.lower(root.get("name")), "%màu cam%"),
                    cb.like(cb.lower(root.get("name")), "%cam %"),
                    cb.like(cb.lower(root.get("name")), "% cam%"),
                    cb.like(cb.lower(root.get("name")), "%orange%"),
                    cb.like(cb.lower(root.get("description")), "%màu cam%"),
                    cb.like(cb.lower(root.get("description")), "%orange%")
            );
        }
        return cb.or(
                cb.like(cb.lower(root.get("name")), "%" + lower + "%"),
                cb.like(cb.lower(root.get("description")), "%" + lower + "%")
        );
    }
}
