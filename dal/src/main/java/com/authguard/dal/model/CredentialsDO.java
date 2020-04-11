package com.authguard.dal.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class CredentialsDO extends AbstractDO {
    private String accountId;
    private List<UserIdentifierDO> identifiers;
    private PasswordDO hashedPassword;
}
