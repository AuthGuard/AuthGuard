package com.nexblocks.authguard.dal.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import jakarta.persistence.Entity;
import jakarta.persistence.Lob;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
// JPA
@Entity
@Table(name = "totp_keys")
@NamedQuery(
        name = "totp_keys.getById",
        query = "SELECT key FROM TotpKeyDO key WHERE key.id = :id AND key.deleted = false"
)
@NamedQuery(
        name = "totp_keys.getByAccountId",
        query = "SELECT key FROM TotpKeyDO key WHERE key.domain = :domain " +
                "AND key.accountId = :accountId " +
                "AND key.deleted = false " +
                "ORDER BY key.createdAt desc"
)
public class TotpKeyDO extends AbstractDO {
    private String domain;
    private Long accountId;
    private String authenticator;

    @Lob
    @JdbcTypeCode(SqlTypes.VARBINARY)
    private byte[] nonce;
    @Lob
    @JdbcTypeCode(SqlTypes.VARBINARY)
    private byte[] encryptedKey;
}
