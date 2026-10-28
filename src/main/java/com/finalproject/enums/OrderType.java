package com.finalproject.enums;

public enum OrderType {
    BUY,
    /** Historical database value only; new carts and orders accept BUY. */
    RENT;

    public String getDisplayName() {
        return this == BUY ? "Mua hoa tươi" : "Giao dịch lịch sử";
    }
}
