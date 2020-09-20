package com.authguard.dal.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class IdempotentRecordDO extends AbstractDO {
    private String idempotentKey;
    private String entityId;
    private String entityType;
}
