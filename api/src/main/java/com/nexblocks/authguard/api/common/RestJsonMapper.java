package com.nexblocks.authguard.api.common;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class RestJsonMapper {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static <T> T asClass(final String json, final Class<T> clazz) {
        try {
            return objectMapper.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            throw new RuntimeJsonException(e);
        }
    }
}
