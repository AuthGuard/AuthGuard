package com.nexblocks.authguard.dal.model;

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
                "LEFT JOIN FETCH account.roles " +
                "LEFT JOIN FETCH account.permissions " +
                "WHERE account.id = :id AND account.deleted = false"
)
@NamedQuery(
        name = "accounts.getByExternalId",
        query = "SELECT account FROM AccountDO account " +
                "LEFT JOIN FETCH account.roles " +
                "LEFT JOIN FETCH account.permissions " +
                "WHERE account.externalId = :externalId AND account.deleted = false"
)
@NamedQuery(
        name = "accounts.getByEmail",
        query = "SELECT account FROM AccountDO account " +
                "LEFT JOIN FETCH account.roles " +
                "LEFT JOIN FETCH account.permissions " +
                "WHERE (account.email.email = :email OR account.backupEmail.email = :email) AND account.deleted = false"
)
@NamedQuery(
        name = "accounts.getByRole",
        query = "SELECT account FROM AccountDO account " +
                "LEFT JOIN FETCH account.permissions " +
                "LEFT JOIN FETCH account.roles role " +
                "WHERE role = :role AND account.deleted = false "
)
public class AccountDO extends AbstractDO {
    private String externalId;

    private String firstName;
    private String middleName;
    private String lastName;
    private String fullName;

    @ElementCollection(fetch = FetchType.LAZY)
    @JoinTable(name = "account_roles")
    private Set<String> roles;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "account_permissions")
    private Set<PermissionDO> permissions;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "email", column = @Column(name = "email", unique = true)),
            @AttributeOverride(name = "verified", column = @Column(name = "email_verified")),
            @AttributeOverride(name = "active", column = @Column(name = "email_active"))
    })
    private EmailDO email;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "email", column = @Column(name = "backup_email", unique = true)),
            @AttributeOverride(name = "verified", column = @Column(name = "backup_email_verified")),
            @AttributeOverride(name = "active", column = @Column(name = "backup_email_active"))
    })
    private EmailDO backupEmail;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "number", column = @Column(name = "phone_number", unique = true)),
            @AttributeOverride(name = "verified", column = @Column(name = "phone_number_verified")),
            @AttributeOverride(name = "active", column = @Column(name = "phone_number_active"))
    })
    private PhoneNumberDO phoneNumber;

    private boolean active;
}
