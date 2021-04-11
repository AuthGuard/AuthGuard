package com.nexblocks.authguard.dal.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import javax.persistence.*;
import java.time.OffsetDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
// JPA
@Entity
@Table(name = "account_tokens", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "token" })
})
@NamedQuery(
        name = "account_tokens.getByToken",
        query = "SELECT account_token FROM AccountTokenDO account_token " +
                "WHERE account_token.token = :token AND account_token.deleted = false"
)
public class AccountTokenDO extends AbstractDO {
    private String token;
    private String associatedAccountId;
    private OffsetDateTime expiresAt;

    @ElementCollection(fetch = FetchType.EAGER)
    private Map<String, String> additionalInformation;

    @Embedded
    private TokenRestrictionsDO tokenRestrictions;
}
