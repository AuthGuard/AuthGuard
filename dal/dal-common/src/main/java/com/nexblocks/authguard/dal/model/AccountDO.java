package com.nexblocks.authguard.dal.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.Map;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
// JPA
@Entity
@Table(name = "accounts", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "email", "domain" }, name = "EMAIL_DUP"),
        @UniqueConstraint(columnNames = { "backup_email", "domain" }, name = "BACKUP_EMAIL_DUP"),
        @UniqueConstraint(columnNames = { "phone_number", "domain" }, name = "PHONE_NUMBER_DUP")
})
@NamedQuery(
        name = "accounts.getById",
        query = "SELECT DISTINCT account FROM AccountDO account " +
                "LEFT JOIN FETCH account.roles " +
                "LEFT JOIN FETCH account.permissions " +
                "LEFT JOIN FETCH account.identifiers identifier " +
                "WHERE account.id = :id AND account.deleted = false"
)
@NamedQuery(
        name = "accounts.getByExternalId",
        query = "SELECT DISTINCT account FROM AccountDO account " +
                "LEFT JOIN FETCH account.roles " +
                "LEFT JOIN FETCH account.permissions " +
                "LEFT JOIN FETCH account.identifiers identifier " +
                "WHERE account.externalId = :externalId AND account.deleted = false"
)
@NamedQuery(
        name = "accounts.getByEmail",
        query = "SELECT DISTINCT account FROM AccountDO account " +
                "LEFT JOIN FETCH account.roles " +
                "LEFT JOIN FETCH account.permissions " +
                "LEFT JOIN FETCH account.identifiers identifier " +
                "WHERE (account.email.email = :email OR account.backupEmail.email = :email) AND account.domain = :domain AND account.deleted = false"
)
@NamedQuery(
        name = "accounts.getByRole",
        query = "SELECT DISTINCT account FROM AccountDO account " +
                "LEFT JOIN FETCH account.permissions " +
                "LEFT JOIN FETCH account.roles role " +
                "WHERE role = :role AND account.domain = :domain AND account.deleted = false "
)
@NamedQuery(
        name = "accounts.getByIdentifier",
        query = "SELECT account FROM AccountDO account " +
                "LEFT JOIN FETCH account.roles " +
                "LEFT JOIN FETCH account.permissions " +
                "LEFT JOIN FETCH account.identifiers identifier " +
                "WHERE identifier.identifier = :identifier AND identifier.domain = :domain AND account.deleted = false"
)
public class AccountDO extends AbstractDO {
    private String externalId;

    private String firstName;
    private String middleName;
    private String lastName;
    private String fullName;
    private String domain;

    private boolean social;
    private String identityProvider;

    @ElementCollection(fetch = FetchType.LAZY)
    @JoinTable(name = "account_roles")
    private Set<String> roles;

    @ManyToMany(fetch = FetchType.EAGER)
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

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "number", column = @Column(name = "phone_number")),
            @AttributeOverride(name = "verified", column = @Column(name = "phone_number_verified")),
            @AttributeOverride(name = "active", column = @Column(name = "phone_number_active"))
    })
    private PhoneNumberDO phoneNumber;

    // credentials
    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JoinColumn(name = "account_id")
    private Set<UserIdentifierDO> identifiers;

    @Embedded
    private PasswordDO hashedPassword;

    private Instant passwordUpdatedAt;
    private int passwordVersion;

    private boolean active;

    @ElementCollection(fetch = FetchType.EAGER)
    @MapKeyColumn(name="\"key\"")
    @Column(name="\"value\"")
    private Map<String, String> metadata;
}
