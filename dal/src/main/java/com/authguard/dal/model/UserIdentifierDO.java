package com.authguard.dal.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserIdentifierDO {
    private Type type;
    private String identifier;
    private boolean active;

    public enum Type {
        USERNAME,
        EMAIL,
        PHONE_NUMBER
    }
}
