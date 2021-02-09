package com.nexblocks.authguard.ldap;

import com.nexblocks.authguard.ldap.config.LdapConfig;
import com.nexblocks.authguard.service.exceptions.ServiceAuthorizationException;
import com.nexblocks.authguard.service.model.AccountBO;
import com.unboundid.ldap.listener.InMemoryDirectoryServer;
import com.unboundid.ldap.listener.InMemoryDirectoryServerConfig;
import com.unboundid.ldap.sdk.LDAPException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UnboundLdapServiceTest {

    private InMemoryDirectoryServer ldapServer;
    private UnboundLdapService ldapService;

    @BeforeAll
    void setup() throws LDAPException {
        // server configuration
        final InMemoryDirectoryServerConfig config = new InMemoryDirectoryServerConfig("dc=example,dc=com");
        config.addAdditionalBindCredentials("cn=Directory Manager", "password");

        // Create the directory server instance, populate it with data from the
        ldapServer = new InMemoryDirectoryServer(config);
        ldapServer.importFromLDIF(true, this.getClass().getClassLoader().getResource("ldap-config.ldif").getFile());
        ldapServer.startListening();

        // set up the service
        ldapService = new UnboundLdapService(LdapConfig.builder()
                .adminBindFormat("cn=%s")
                .baseDN("ou=people,dc=example,dc=com")
                .searchAttribute("uid")
                .passwordAttribute("userPassword")
                .host(ldapServer.getConnection().getConnectedIPAddress())
                .port(ldapServer.getConnection().getConnectedPort())
                .isSecure(false)
                .admin("Directory Manager")
                .adminPassword("password")
                .bindType("simple")
                .build());
    }

    @AfterAll
    void cleanup() {
        ldapServer.close();
    }

    @Test
    void authenticate() {
        final AccountBO user = ldapService.authenticate("bob", "bobspassword");

        assertThat(user).isNotNull();
    }

    @Test
    void authenticateWrongPassword() {
        assertThatThrownBy(() -> ldapService.authenticate("bob", "wrong"))
                .isInstanceOf(ServiceAuthorizationException.class);
    }

    @Test
    void authenticateNotFound() {
        assertThatThrownBy(() -> ldapService.authenticate("invalid", "wrong"))
                .isInstanceOf(ServiceAuthorizationException.class);
    }
}