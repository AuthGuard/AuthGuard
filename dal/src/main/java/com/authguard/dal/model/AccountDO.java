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
@Table(name = "accounts")
@NamedQuery(
        name = "accounts.getById",
        query = "SELECT account FROM AccountDO account " +
                "JOIN FETCH account.roles " +
                "JOIN FETCH account.permissions " +
                "WHERE account.id = :id"
)
@NamedQuery(
        name = "accounts.getByExternalId",
        query = "SELECT account FROM AccountDO account " +
                "JOIN FETCH account.roles " +
                "JOIN FETCH account.permissions " +
                "WHERE account.externalId = :externalId"
)
@NamedQuery(
        name = "accounts.getByRole",
        query = "SELECT account FROM AccountDO account " +
                "JOIN FETCH account.permissions " +
                "JOIN FETCH account.roles role " +
                "WHERE role = :role"
)
public class AccountDO extends AbstractDO {
    private String externalId;

    @ElementCollection(fetch = FetchType.LAZY)
    @JoinTable(name = "account_roles")
    private Set<String> roles;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "account_permissions")
    private Set<PermissionDO> permissions;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "email", column = @Column(name = "email")),
            @AttributeOverride(name = "verified", column = @Column(name = "email_verified")),
            @AttributeOverride(name = "active", column = @Column(name = "email_active"))
    })
    private EmailDO email;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "email", column = @Column(name = "backup_email")),
            @AttributeOverride(name = "verified", column = @Column(name = "backup_email_verified")),
            @AttributeOverride(name = "active", column = @Column(name = "backup_email_active"))
    })
    private EmailDO backupEmail;

    private boolean active;
}
