package com.authguard.dal.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.ZonedDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class AccountTokenDO extends AbstractDO {
    private String token;
    private String associatedAccountId;
    private ZonedDateTime expiresAt;
    private Object additionalInformation;
}
