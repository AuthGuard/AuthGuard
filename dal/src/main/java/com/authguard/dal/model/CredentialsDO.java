package com.authguard.dal.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class CredentialsDO extends AbstractDO {
    private String accountId;
    private String username;
    private PasswordDO hashedPassword;
}
