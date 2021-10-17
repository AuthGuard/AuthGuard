package com.nexblocks.authguard.service.model;

import org.immutables.value.Value;

import java.util.Map;
import java.util.Set;

@Value.Immutable
@BOStyle
public interface Account extends Entity {
    String getExternalId();

    String getFirstName();
    String getMiddleName();
    String getLastName();
    String getFullName();

    Set<PermissionBO> getPermissions();
    Set<String> getRoles();
    AccountEmailBO getEmail();
    AccountEmailBO getBackupEmail();
    PhoneNumberBO getPhoneNumber();

    Map<String, String> getMetadata();

    boolean isActive();
    boolean isDeleted();

    @Override
    @Value.Derived
    default String getEntityType() {
        return "Account";
    }
}
