package com.nexblocks.authguard.dal.model;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class LongToStringConverter implements AttributeConverter<String, Long> {
    @Override
    public Long convertToDatabaseColumn(final String fieldValue) {
        return Long.parseLong(fieldValue);
    }

    @Override
    public String convertToEntityAttribute(final Long columnValue) {
        return String.valueOf(columnValue);
    }
}
