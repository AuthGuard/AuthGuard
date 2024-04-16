package com.nexblocks.authguard.service.model;

import org.immutables.value.Value;

import java.time.Instant;
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

    boolean isSocial();
    String getIdentityProvider();

    Set<PermissionBO> getPermissions();
    Set<String> getRoles();
    AccountEmailBO getEmail();
    AccountEmailBO getBackupEmail();
    PhoneNumberBO getPhoneNumber();

    // credentials
    Set<UserIdentifierBO> getIdentifiers();
    String getPlainPassword();
    HashedPasswordBO getHashedPassword();
    Instant getPasswordUpdatedAt();
    Integer getPasswordVersion();

    Map<String, String> getMetadata();

    @Value.Default
    default boolean isActive() {
        return true;
    }

    boolean isDeleted();

    @Override
    @Value.Derived
    default String getEntityType() {
        return "Account";
    }
}
