package com.authguard.dal.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import javax.persistence.Entity;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
// JPA
@Entity
@Table(name = "roles", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"name"})
})
@NamedQuery(
        name = "roles.getById",
        query = "SELECT role FROM RoleDO role WHERE role.id = :id AND role.deleted = false"
)
@NamedQuery(
        name = "roles.getAll",
        query = "SELECT role FROM RoleDO role WHERE role.deleted = false"
)
@NamedQuery(
        name = "roles.getByName",
        query = "SELECT role FROM RoleDO role WHERE role.name = :name AND role.deleted = false"
)
@NamedQuery(
        name = "roles.getMultiple",
        query = "SELECT role FROM RoleDO role WHERE role.name in :names AND role.deleted = false"
)
public class RoleDO extends AbstractDO {
    private String name;
}
