package com.authguard.dal.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Data
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class ApiKeyDO extends AbstractDO {
    private String jti;
    private String key;
    private String appId;
}
