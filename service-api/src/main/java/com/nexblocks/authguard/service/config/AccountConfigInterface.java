package com.nexblocks.authguard.service.config;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

import java.util.Map;
import java.util.Set;

@Value.Immutable
@ConfigStyle
@JsonDeserialize(as = AccountConfig.class)
public interface AccountConfigInterface {
    String getAuthguardAdminRole();
    boolean requireEmail();
    boolean requirePhoneNumber();
    boolean verifyEmail();
    boolean verifyPhoneNumber();
    String getDefaultDomain();
    Map<String, Set<String>> getDefaultRolesByDomain();
}
