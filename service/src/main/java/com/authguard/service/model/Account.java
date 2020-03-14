package com.authguard.service.model;

import org.immutables.value.Value;

import java.util.List;

@Value.Immutable
@BOStyle
public interface Account {
    String getId();
    List<PermissionBO> getPermissions();
    List<String> getScopes();
    List<String> getRoles();
    List<AccountEmailBO> getAccountEmails();
    boolean isActive();
    boolean isDeleted();
}
