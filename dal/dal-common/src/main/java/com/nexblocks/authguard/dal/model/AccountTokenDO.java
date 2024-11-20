package com.nexblocks.authguard.dal.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import javax.persistence.*;
import java.time.Instant;
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
    private long associatedAccountId;
    private Instant expiresAt;
    private String sourceAuthType;
    private String deviceId;
    private String clientId;
    private String externalSessionId;
    private String trackingSession;
    private String userAgent;
    private String sourceIp;
    private String domain;

    @ElementCollection(fetch = FetchType.EAGER)
    @MapKeyColumn(name="key")
    @Column(name="value")
    private Map<String, String> additionalInformation;

    @Embedded
    private TokenRestrictionsDO tokenRestrictions;
}
