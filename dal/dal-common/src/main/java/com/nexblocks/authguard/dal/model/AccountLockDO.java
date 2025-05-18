package com.nexblocks.authguard.dal.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import jakarta.persistence.Entity;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;
import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
// JPA
@Entity
@Table(name = "account_locks")
@NamedQuery(
        name = "account_locks.getByAccountId",
        query = "SELECT lock FROM AccountLockDO lock " +
                "WHERE lock.accountId = :accountId AND lock.deleted = false"
)
public class AccountLockDO extends AbstractDO {
    private long accountId;
    private Instant expiresAt;
}
