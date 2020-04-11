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
public class OneTimePasswordDO extends AbstractDO {
    private String accountId;
    private String password;
    private ZonedDateTime expiresAt;
}
