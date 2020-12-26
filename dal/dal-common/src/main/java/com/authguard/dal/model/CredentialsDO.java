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
@Table(name = "credentials")
@NamedQuery(
        name = "credentials.getByAccountId",
        query = "SELECT credentials FROM CredentialsDO credentials WHERE credentials.accountId = :accountId"
)
@NamedQuery(
        name = "credentials.getByIdentifier",
        query = "SELECT credentials FROM CredentialsDO credentials JOIN credentials.identifiers identifier WHERE identifier.identifier = :identifier"
)
public class CredentialsDO extends AbstractDO {
    private String accountId;

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private Set<UserIdentifierDO> identifiers;

    @Embedded
    private PasswordDO hashedPassword;
}
