package com.nexblocks.authguard.dal.model;

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
@Table(name = "permissions", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "group", "name", "domain" }, name = "PERMISSION_DUP")
})
@NamedQuery(
        name = "permissions.getById",
        query = "SELECT permission FROM PermissionDO permission " +
                "WHERE permission.id = :id AND permission.deleted = false"
)
@NamedQuery(
        name = "permissions.getAll",
        query = "SELECT permission FROM PermissionDO permission " +
                "WHERE permission.deleted = false " +
                "AND permission.domain = :domain AND permission.id > :cursor " +
                "ORDER BY permission.id"
)
@NamedQuery(
        name = "permissions.getByGroupAndName",
        query = "SELECT permission FROM PermissionDO permission " +
                "WHERE permission.group = :group AND permission.name = :name AND permission.domain = :domain AND permission.deleted = false"
)
@NamedQuery(
        name = "permissions.getByGroup",
        query = "SELECT permission FROM PermissionDO permission " +
                "WHERE permission.group = :group AND permission.domain = :domain " +
                "AND permission.deleted = false AND permission.id > :cursor " +
                "ORDER BY permission.id"
)
public class PermissionDO extends AbstractDO {
    @Column(name = "\"group\"")
    private String group;
    private String name;
    private String domain;
    private boolean forAccounts;
    private boolean forApplications;
}
