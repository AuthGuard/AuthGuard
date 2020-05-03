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
public class AccountDO extends AbstractDO {
    private String externalId;
    private List<String> roles;
    private List<PermissionDO> permissions;
    private List<String> scopes;
    private List<EmailDO> emails;

    private boolean active;


}
