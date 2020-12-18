package com.authguard.dal.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import javax.persistence.Entity;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import java.time.OffsetDateTime;

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
                "WHERE lock.accountId = :accountId"
)
public class AccountLockDO extends AbstractDO {
    private String accountId;
    private OffsetDateTime expiresAt;
}
