package com.authguard.service.ldap;

import com.authguard.service.config.LdapConfig;
import com.authguard.service.exceptions.ServiceAuthorizationException;
import com.authguard.service.model.AccountBO;
import com.authguard.service.model.HashedPasswordBO;
import com.authguard.service.passwords.SecurePassword;
import com.unboundid.ldap.listener.InMemoryDirectoryServer;
import com.unboundid.ldap.listener.InMemoryDirectoryServerConfig;
import com.unboundid.ldap.sdk.LDAPException;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UnboundedLdapServiceTest {

    private InMemoryDirectoryServer ldapServer;
    private UnboundedLdapService ldapService;
    private SecurePassword securePassword;

    @BeforeAll
    void setup() throws LDAPException {
        // server configuration
        final InMemoryDirectoryServerConfig config = new InMemoryDirectoryServerConfig("dc=example,dc=com");
        config.addAdditionalBindCredentials("cn=Directory Manager", "password");

        // Create the directory server instance, populate it with data from the
        ldapServer = new InMemoryDirectoryServer(config);
        ldapServer.importFromLDIF(true, this.getClass().getClassLoader().getResource("ldap-config.ldif").getFile());
        ldapServer.startListening();

        // mocks
        securePassword = Mockito.mock(SecurePassword.class);

        // set up the service
        ldapService = new UnboundedLdapService(LdapConfig.builder()
                .adminBindFormat("cn=%s")
                .baseDN("ou=people,dc=example,dc=com")
                .searchAttribute("uid")
                .passwordAttribute("userPassword")
                .host(ldapServer.getConnection().getConnectedIPAddress())
                .port(ldapServer.getConnection().getConnectedPort())
                .isSecure(false)
                .admin("Directory Manager")
                .adminPassword("password")
                .build(), securePassword);
    }

    @AfterAll
    void cleanup() {
        ldapServer.close();
    }

    @BeforeEach
    void reset() {
        Mockito.reset(securePassword);
    }

    @Test
    void authenticate() {
        Mockito.when(securePassword.verify("bobspassword", HashedPasswordBO.builder().password("bobspassword").build()))
                .thenReturn(true);

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