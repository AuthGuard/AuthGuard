package com.nexblocks.authguard.dal.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Type;
import org.hibernate.type.SqlTypes;
import org.hibernate.usertype.UserType;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
// JPA
@Entity
@Table(name = "crypto_keys")
@NamedQuery(
        name = "crypto_keys.getById",
        query = "SELECT key FROM CryptoKeyDO key WHERE key.id = :id AND key.deleted = false"
)
@NamedQuery(
        name = "crypto_keys.getByDomain",
        query = "SELECT key FROM CryptoKeyDO key WHERE key.domain = :domain " +
                "AND key.accountId = :id " +
                "AND key.deleted = false " +
                "AND key.createdAt < :cursor " +
                "ORDER BY key.createdAt desc"
)
@NamedQuery(
        name = "crypto_keys.getByAccountId",
        query = "SELECT key FROM CryptoKeyDO key WHERE key.domain = :domain " +
                "AND key.accountId = :id " +
                "AND key.deleted = false " +
                "AND key.createdAt < :cursor " +
                "ORDER BY key.createdAt desc"
)
@NamedQuery(
        name = "crypto_keys.getByAppId",
        query = "SELECT crypto_key FROM CryptoKeyDO crypto_key WHERE crypto_key.domain = :domain " +
                "AND crypto_key.appId = :id " +
                "AND crypto_key.deleted = false " +
                "AND crypto_key.createdAt < :cursor " +
                "ORDER BY crypto_key.createdAt desc"
)
public class CryptoKeyDO extends AbstractDO {
    private String domain;
    private Long accountId;
    private Long appId;
    private String name;
    private String algorithm;
    private int size;
    private int version;

    private boolean passcodeProtected;
    private String passcodeCheckPlain;
    private String passcodeCheckEncrypted;

    @Lob
    @JdbcTypeCode(SqlTypes.VARBINARY)
    private byte[] nonce;
    @Lob
    @JdbcTypeCode(SqlTypes.VARBINARY)
    private byte[] privateKey;
    @Lob
    @JdbcTypeCode(SqlTypes.VARBINARY)
    private byte[] publicKey;

    @ElementCollection(fetch = FetchType.EAGER)
    @MapKeyColumn(name="\"key\"")
    @Column(name="\"value\"")
    private Map<String, String> parameters;
}
