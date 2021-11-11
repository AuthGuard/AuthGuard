package com.nexblocks.authguard.ldap.config;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.nexblocks.authguard.service.config.AccountConfig;
import com.nexblocks.authguard.service.config.ConfigStyle;
import org.immutables.value.Value;

import java.util.Map;

@Value.Immutable
@ConfigStyle
@JsonDeserialize(as = LdapConfig.class)
public interface LdapConfigInterface {
    String getBaseDN();
    String getAdmin();
    String getAdminPassword();
    String getHost();
    Integer getPort();
    Boolean isSecure();
    String getAdminBindFormat();
    String getSearchAttribute();
    String getPasswordAttribute();
    String getBindType();

    Map<String, String> getFieldMapping();
}
