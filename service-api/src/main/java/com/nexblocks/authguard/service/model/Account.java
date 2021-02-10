package com.nexblocks.authguard.service.model;

import org.immutables.value.Value;

import java.util.Set;

@Value.Immutable
@BOStyle
public interface Account extends Entity {
    String getExternalId();
    Set<PermissionBO> getPermissions();
    Set<String> getRoles();
    AccountEmailBO getEmail();
    AccountEmailBO getBackupEmail();
    boolean isActive();
    boolean isDeleted();

    @Override
    @Value.Derived
    default String getEntityType() {
        return "Account";
    }
}
