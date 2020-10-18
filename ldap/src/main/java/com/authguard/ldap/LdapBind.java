package com.authguard.ldap;

import com.authguard.ldap.config.LdapConfig;
import com.authguard.service.exceptions.ConfigurationException;
import com.unboundid.ldap.sdk.*;

import java.util.function.BiFunction;

public class LdapBind {
    private final UnboundConnectionFactory connectionFactory;
    private final BiFunction<String, String, BindRequest> bindRequestProvider;

    public LdapBind(final UnboundConnectionFactory connectionFactory, final LdapConfig ldapConfig) {
        this.connectionFactory = connectionFactory;
        this.bindRequestProvider = createBindRequestProvider(ldapConfig.getBindType());
    }

    public void bind(final String dn, final String password) throws LDAPException {
        try (LDAPConnection connection = connectionFactory.create()) {
            final BindRequest bindRequest = bindRequestProvider.apply(dn, password);
            connection.bind(bindRequest);
        }
    }

    private BiFunction<String, String, BindRequest> createBindRequestProvider(final String type) {
        if (type == null) {
            throw new ConfigurationException("An LDAP bind type must be provided");
        }

        switch (type) {
            case "simple":
                return SimpleBindRequest::new;

            case "cram_md5":
                return (dn, password) -> new CRAMMD5BindRequest("dn:" + dn, password);

            case "digest_md5":
                return (dn, password) -> new DIGESTMD5BindRequest("dn:" + dn, password);

            case "plain":
                return (dn, password) -> new PLAINBindRequest("dn:" + dn, password);

            default:
                throw new ConfigurationException("Unsupported LDAP bind type " + type);
        }
    }
}
