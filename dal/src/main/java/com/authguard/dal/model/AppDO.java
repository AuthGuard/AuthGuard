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
public class AppDO extends AbstractDO {
    private String name;
    private String parentAccountId;
    private List<String> roles;
    private List<PermissionDO> permissions;
    private List<String> scopes;

    private boolean active;
}
