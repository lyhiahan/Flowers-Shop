package com.finalproject.utils;

import java.text.Normalizer;
import java.util.*;

/** Shared labels/validation for editable flower attributes; no category changes. */
public final class FlowerAttributes {
    private FlowerAttributes() {}
    public static String group(String name) {
        if (name == null) return "";
        return switch (normalize(name).toLowerCase(Locale.ROOT)) {
            case "loại hoa" -> "Loại hoa";
            case "màu sắc", "tone màu hoa", "tone màu thiết kế" -> "Màu sắc";
            case "phong cách", "phong cách thiết kế", "phong cách cắm" -> "Phong cách";
            default -> name;
        };
    }
    public static boolean editable(String name) {
        return Set.of("Loại hoa", "Màu sắc", "Phong cách").contains(group(name));
    }
    public static String normalize(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFC).strip().replaceAll("\\s+", " ");
    }
    public static List<String> parse(String raw) {
        if (raw == null || raw.isBlank()) return List.of();
        if (raw.length() > 1500) throw new IllegalArgumentException("Nội dung thuộc tính quá dài.");
        Map<String, String> result = new LinkedHashMap<>();
        for (String part : raw.split("[,;\\r\\n]+")) {
            String value = normalize(part);
            if (value.isEmpty()) continue;
            if (value.length() > 32 || value.chars().anyMatch(Character::isISOControl))
                throw new IllegalArgumentException("Mỗi giá trị thuộc tính tối đa 32 ký tự và không chứa ký tự điều khiển.");
            result.putIfAbsent(value.toLowerCase(Locale.ROOT), value);
        }
        if (result.size() > 15) throw new IllegalArgumentException("Mỗi nhóm thuộc tính tối đa 15 giá trị.");
        return new ArrayList<>(result.values());
    }
}
