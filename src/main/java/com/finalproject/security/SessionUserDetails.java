package com.finalproject.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import org.springframework.security.core.GrantedAuthority;

/** The stamp survives Spring's erasure of the BCrypt password after login. */
public final class SessionUserDetails extends org.springframework.security.core.userdetails.User {
    private static final long serialVersionUID = 1L;
    private final byte[] credentialStamp;

    public SessionUserDetails(String username, String password, boolean enabled,
                              Collection<? extends GrantedAuthority> authorities) {
        super(username, password, enabled, true, true, true, authorities);
        credentialStamp = stamp(password);
    }

    public boolean matchesCredential(String currentPassword) {
        return MessageDigest.isEqual(credentialStamp, stamp(currentPassword));
    }

    private static byte[] stamp(String password) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(
                    (password == null ? "" : password).getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException ex) { throw new IllegalStateException(ex); }
    }
}
