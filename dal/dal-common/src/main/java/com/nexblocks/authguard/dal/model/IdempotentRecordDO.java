package com.nexblocks.authguard.dal.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import jakarta.persistence.Entity;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
// JPA
@Entity
@Table(name = "idempotent_records")
@NamedQuery(
        name = "idempotent_records.getByKey",
        query = "SELECT record FROM IdempotentRecordDO record WHERE record.idempotentKey = :key"
)
@NamedQuery(
        name = "idempotent_records.getByKeyAndEntity",
        query = "SELECT record FROM IdempotentRecordDO record WHERE record.idempotentKey = :key and record.entityType = :entityType"
)
public class IdempotentRecordDO extends AbstractDO {
    private String idempotentKey;
    private long entityId;
    private String entityType;
}
