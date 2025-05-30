package com.nexblocks.authguard.dal.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import jakarta.persistence.Entity;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
// JPA
@Entity
@Table(name = "roles", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "name", "domain" }, name = "ROLE_DUP")
})
@NamedQuery(
        name = "roles.getById",
        query = "SELECT role FROM RoleDO role WHERE role.id = :id AND role.deleted = false"
)
@NamedQuery(
        name = "roles.getAll",
        query = "SELECT role FROM RoleDO role WHERE role.deleted = false " +
                "AND role.domain = :domain AND role.id > :cursor " +
                "ORDER BY role.id "
)
@NamedQuery(
        name = "roles.getByName",
        query = "SELECT role FROM RoleDO role WHERE role.name = :name AND role.domain = :domain AND role.deleted = false"
)
@NamedQuery(
        name = "roles.getMultiple",
        query = "SELECT role FROM RoleDO role WHERE role.name in :names AND role.domain = :domain AND role.deleted = false"
)
public class RoleDO extends AbstractDO {
    private String name;
    private String domain;
    private boolean forAccounts;
    private boolean forApplications;
}
