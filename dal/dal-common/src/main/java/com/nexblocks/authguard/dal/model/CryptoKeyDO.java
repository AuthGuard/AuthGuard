package com.nexblocks.authguard.dal.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import javax.persistence.*;
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
        query = "SELECT key FROM CryptoKeyDO key WHERE key.domain = :domain " +
                "AND key.appId = :id " +
                "AND key.deleted = false " +
                "AND key.createdAt < :cursor " +
                "ORDER BY key.createdAt desc"
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
    private byte[] nonce;
    @Lob
    private byte[] privateKey;
    @Lob
    private byte[] publicKey;

    @ElementCollection(fetch = FetchType.EAGER)
    @MapKeyColumn(name="key")
    @Column(name="value")
    private Map<String, String> parameters;
}
