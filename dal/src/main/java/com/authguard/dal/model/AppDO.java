package com.authguard.dal.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import javax.persistence.*;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
// JPA
@Entity
@Table(name = "apps")
@NamedQuery(
        name = "apps.getByExternalId",
        query = "SELECT app FROM AppDO app WHERE app.externalId = :externalId"
)
@NamedQuery(
        name = "apps.getByAccountId",
        query = "SELECT app FROM AppDO app WHERE app.parentAccountId = :parentAccountId"
)
public class AppDO extends AbstractDO {
    private String externalId;
    private String name;
    private String parentAccountId;

    @ElementCollection(fetch = FetchType.EAGER)
    private Set<String> roles;

    @ManyToMany(fetch = FetchType.EAGER)
    private Set<PermissionDO> permissions;

    private boolean active;
}
