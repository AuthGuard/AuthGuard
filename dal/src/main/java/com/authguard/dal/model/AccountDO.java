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
        name = "accounts.getByExternalId",
        query = "SELECT account FROM AccountDO account WHERE account.externalId = :externalId"
)
@NamedQuery(
        name = "accounts.getByRole",
        query = "SELECT account FROM AccountDO account JOIN account.roles role WHERE role = :role"
)
public class AccountDO extends AbstractDO {
    private String externalId;

    @ElementCollection(fetch = FetchType.EAGER) // temporary
    @JoinTable(name = "account_roles")
    private Set<String> roles;

    @ManyToMany(fetch = FetchType.EAGER) // temporary
    @JoinTable(name = "account_permissions")
    private Set<PermissionDO> permissions;

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL) // temporary
    @JoinTable(name = "account_emails")
    private Set<EmailDO> emails;

    private boolean active;
}
