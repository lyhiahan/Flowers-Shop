package com.finalproject.enums;

import lombok.Getter;

@Getter
public enum RoleRequestStatus {
    PENDING("Chờ duyệt", "warning"),
    APPROVED("Đã phê duyệt", "success"),
    REJECTED("Đã từ chối", "danger"),
    CANCELLED("Đã rút yêu cầu", "secondary");

    private final String displayName;
    private final String badgeClass;

    RoleRequestStatus(String displayName, String badgeClass) {
        this.displayName = displayName;
        this.badgeClass = badgeClass;
    }
}
