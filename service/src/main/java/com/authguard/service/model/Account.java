package com.authguard.service.model;

import org.immutables.value.Value;

import java.util.List;

@Value.Immutable
@BOStyle
public interface Account extends Entity {
    String getExternalId();
    List<PermissionBO> getPermissions();
    List<String> getScopes();
    List<String> getRoles();
    List<AccountEmailBO> getEmails();
    boolean isActive();
    boolean isDeleted();

    @Override
    @Value.Derived
    default String getEntityType() {
        return "Account";
    }
}
