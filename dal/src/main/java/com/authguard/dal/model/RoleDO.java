package com.authguard.dal.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import javax.persistence.*;

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
        name = "roles.getAll",
        query = "SELECT role FROM RoleDO role"
)
@NamedQuery(
        name = "roles.getByName",
        query = "SELECT role FROM RoleDO role WHERE role.name = :name"
)
@NamedQuery(
        name = "roles.getMultiple",
        query = "SELECT role FROM RoleDO role WHERE role.name in :names"
)
public class RoleDO extends AbstractDO {
    private String name;
}
