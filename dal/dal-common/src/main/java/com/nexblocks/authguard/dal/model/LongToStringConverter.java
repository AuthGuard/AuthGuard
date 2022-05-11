package com.nexblocks.authguard.dal.model;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

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
