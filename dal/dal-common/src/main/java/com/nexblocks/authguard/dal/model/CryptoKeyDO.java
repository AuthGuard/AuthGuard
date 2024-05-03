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
public class CryptoKeyDO extends AbstractDO {
    private String domain;
    private String accountId;
    private String appId;
    private String algorithm;
    private int size;
    private int version;

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
