package com.authguard.ldap;

import com.authguard.ldap.config.LdapConfig;
import com.unboundid.ldap.sdk.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

public class LdapSearch {
    private static final Logger LOG = LoggerFactory.getLogger(LdapSearch.class);

    private final UnboundConnectionFactory connectionFactory;
    private final String[] attributesToRetrieve;
    private final LdapConfig config;

    public LdapSearch(final UnboundConnectionFactory connectionFactory, final LdapConfig config) {
        this.connectionFactory = connectionFactory;
        this.config = config;

        // cache the list of attributes instead of reading them with every operation
        final Set<String> attributesSet = new HashSet<>(config.getFieldMapping().keySet());

        attributesSet.add(config.getPasswordAttribute());
        attributesSet.add(SearchRequest.ALL_USER_ATTRIBUTES);

        this.attributesToRetrieve = attributesSet.toArray(String[]::new);
    }

    public SearchResult findUser(final String username) throws LDAPException {
        try (LDAPConnection connection = connectionFactory.create()) {
            final Filter filter = Filter.createEqualityFilter(config.getSearchAttribute(), username);

            final SearchRequest searchRequest = new SearchRequest(
                    config.getBaseDN(),
                    SearchScope.SUBORDINATE_SUBTREE,
                    filter,
                    attributesToRetrieve
            );

            LOG.debug("Searching for user DN, request: {}", searchRequest);

            return connection.search(searchRequest);
        }
    }
}
