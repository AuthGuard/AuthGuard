package com.authguard.dal.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import javax.persistence.Entity;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

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
        name = "api_keys.getByAppId",
        query = "SELECT api_key FROM ApiKeyDO api_key WHERE api_key.appId = :appId"
)
@NamedQuery(
        name = "api_keys.getByKey",
        query = "SELECT api_key FROM ApiKeyDO api_key WHERE api_key.key = :key"
)
public class ApiKeyDO extends AbstractDO {
    private String key;
    private String appId;
}
