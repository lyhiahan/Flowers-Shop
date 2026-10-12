package com.finalproject.service;

import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;

/** Require one bare mailbox, never a display name, list, or injected mail header. */
public final class OtpEmailInput {
    private OtpEmailInput() {}
    public static String address(String value) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Vui lòng nhập email hợp lệ.");
        String address = value.trim();
        if (address.length() > 254 || value.contains("\r") || value.contains("\n"))
            throw new IllegalArgumentException("Email không hợp lệ.");
        try {
            var parsed = new InternetAddress(address, true);
            parsed.validate();
            if (parsed.isGroup() || parsed.getPersonal() != null || !address.equals(parsed.getAddress())) throw new AddressException();
        } catch (AddressException ex) { throw new IllegalArgumentException("Email không hợp lệ."); }
        return address;
    }
}
