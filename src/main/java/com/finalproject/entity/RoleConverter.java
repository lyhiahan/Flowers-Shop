package com.finalproject.entity;

import com.finalproject.enums.Role;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** Keeps retired role values readable without restoring their permissions. */
@Converter
public class RoleConverter implements AttributeConverter<Role, String> {
    @Override
    public String convertToDatabaseColumn(Role role) {
        return role == null ? null : role.name();
    }

    @Override
    public Role convertToEntityAttribute(String value) {
        if (value == null) return null;
        return "SHIPPER".equals(value) ? Role.CUSTOMER : Role.valueOf(value);
    }
}
