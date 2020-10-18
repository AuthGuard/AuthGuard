package com.authguard.ldap;

import com.authguard.config.ConfigContext;
import com.authguard.ldap.config.LdapConfig;
import com.authguard.service.exceptions.ServiceAuthorizationException;
import com.authguard.service.exceptions.codes.ErrorCode;
import com.authguard.service.model.AccountBO;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.unboundid.ldap.sdk.LDAPBindException;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.SearchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class UnboundLdapService implements LdapService {
    private static final Logger LOG = LoggerFactory.getLogger(UnboundLdapService.class);

    private final LdapConfig config;
    private final LdapSearch ldapSearch;
    private final LdapBind ldapBind;
    private final LdapAccountMapper ldapAccountMapper;

    @Inject
    public UnboundLdapService(final @Named("ldap") ConfigContext config) {
        this(config.asConfigBean(LdapConfig.class));
    }

    UnboundLdapService(final LdapConfig config) {
        this.config = config;

        final String admin = this.config.getAdmin() == null ?
                null :
                String.format(this.config.getAdminBindFormat(), this.config.getAdmin());

        final UnboundConnectionFactory connectionFactory = new UnboundConnectionFactory(this.config.getHost(), this.config.getPort())
                .useSecureConnection(this.config.isSecure())
                .withAdmin(admin, this.config.getAdminPassword());

        this.ldapSearch = new LdapSearch(connectionFactory, config);
        this.ldapBind = new LdapBind(connectionFactory, config);
        this.ldapAccountMapper = new LdapAccountMapper();
    }

    @Override
    public AccountBO authenticate(final String username, final String password) {
        // search
        final SearchResult searchResult;
        final Map<String, String[]> attributes;

        try {
            searchResult = ldapSearch.findUser(username);
            attributes = ldapAccountMapper.getAttributes(searchResult);
        } catch (final LDAPException e) {
            LOG.error("Failed to establish a connection the LDAP server or perform a search", e);

            throw new ServiceAuthorizationException(ErrorCode.LDAP_ERROR, "Could not perform an LDAP search");
        }

        if (attributes.isEmpty()) {
            throw new ServiceAuthorizationException(ErrorCode.IDENTIFIER_DOES_NOT_EXIST, "Username not found");
        }

        // bind
        try {
            ldapBind.bind(searchResult.getSearchEntries().get(0).getDN(), password);
        } catch (final LDAPBindException e) {
            throw new ServiceAuthorizationException(ErrorCode.GENERIC_AUTH_FAILURE, "Failed to authenticate user");
        } catch (final LDAPException e) {
            LOG.error("Failed to establish a connection the LDAP server or perform a bind", e);

            throw new ServiceAuthorizationException(ErrorCode.LDAP_ERROR, "Could not perform an LDAP search");
        }

        return ldapAccountMapper.mapAttributes(attributes, config.getFieldMapping()).build();
    }
}
