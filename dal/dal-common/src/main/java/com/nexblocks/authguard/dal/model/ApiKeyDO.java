package com.nexblocks.authguard.dal.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

@Data
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
// JPA
@Entity
@Table(name = "api_keys", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "key" })
})
@NamedQuery(
        name = "api_keys.getById",
        query = "SELECT api_key FROM ApiKeyDO api_key " +
                "WHERE api_key.id = :id AND api_key.deleted = false"
)
@NamedQuery(
        name = "api_keys.getByAppId",
        query = "SELECT api_key FROM ApiKeyDO api_key " +
                "WHERE api_key.appId = :appId AND api_key.deleted = false"
)
@NamedQuery(
        name = "api_keys.getByKey",
        query = "SELECT api_key FROM ApiKeyDO api_key " +
                "WHERE api_key.key = :key AND api_key.deleted = false"
)
public class ApiKeyDO extends AbstractDO {
    @Column(name = "\"key\"")
    private String key;
    private long appId;
    private String type;
    private String name;
    private boolean forClient;
    private Instant expiresAt;
}
