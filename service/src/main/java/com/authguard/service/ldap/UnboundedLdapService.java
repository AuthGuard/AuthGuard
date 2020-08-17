package com.authguard.service.ldap;

import com.authguard.config.ConfigContext;
import com.authguard.service.config.LdapConfig;
import com.authguard.service.exceptions.ServiceAuthorizationException;
import com.authguard.service.exceptions.codes.ErrorCode;
import com.authguard.service.model.AccountBO;
import com.authguard.service.model.AccountEmailBO;
import com.authguard.service.model.HashedPasswordBO;
import com.authguard.service.model.PermissionBO;
import com.authguard.service.passwords.SecurePassword;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.unboundid.ldap.sdk.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class UnboundedLdapService implements LdapService {
    private static final Logger log = LoggerFactory.getLogger(UnboundedLdapService.class);

    private final LdapConfig config;
    private final LDAPConnection connection;
    private final SecurePassword securePassword;

    private final String[] attributesToRetrieve;

    @Inject
    public UnboundedLdapService(final @Named("ldap") ConfigContext config, final SecurePassword securePassword) {
        this(config.asConfigBean(LdapConfig.class), securePassword);
    }

    UnboundedLdapService(final LdapConfig config, final SecurePassword securePassword) {
        this.config = config;
        this.securePassword = securePassword;

        final String admin = this.config.getAdmin() == null ?
                null :
                String.format(this.config.getAdminBindFormat(), this.config.getAdmin());

        try {
            this.connection = new UnboundedConnectionFactory(this.config.getHost(), this.config.getPort())
                    .useSecureConnection(this.config.isSecure())
                    .withAdmin(admin, this.config.getAdminPassword())
                    .create();
        } catch (final LDAPException e) {
            throw new RuntimeException("Failed to start LDAP service", e);
        }

        // cache the list of attributes instead of reading them with every operation
        final Set<String> attributesSet = new HashSet<>(config.getFieldMapping().keySet());

        attributesSet.add(config.getPasswordAttribute());
        attributesSet.add(SearchRequest.ALL_USER_ATTRIBUTES);

        this.attributesToRetrieve = attributesSet.toArray(String[]::new);
    }

    @Override
    public AccountBO authenticate(final String username, final String password) {
        final Map<String, String[]> attributes = findUserAttributes(username);

        if (attributes.isEmpty()) {
            throw new ServiceAuthorizationException(ErrorCode.IDENTIFIER_DOES_NOT_EXIST, "Username not found");
        }

        final String[] storedPassword = attributes.get(config.getPasswordAttribute());

        if (storedPassword.length > 1) {
            throw new ServiceAuthorizationException(ErrorCode.LDAP_MULTIPLE_PASSWORD_ENTRIES,
                    "Found multiple values for " + config.getPasswordAttribute());
        }

        final HashedPasswordBO hashedPassword = HashedPasswordBO.builder()
                .password(storedPassword[0])
                .build(); // the salt should be stored as part of the password

        if (!securePassword.verify(password, hashedPassword)) {
            throw new ServiceAuthorizationException(ErrorCode.PASSWORDS_DO_NOT_MATCH, "Failed to authenticate user");
        }

        return mapAttributes(attributes, config.getFieldMapping()).build();
    }

    private Map<String, String[]> findUserAttributes(final String username) {
        try {
            final Filter filter = Filter.createEqualityFilter(config.getSearchAttribute(), username);

            final SearchRequest searchRequest = new SearchRequest(
                    config.getBaseDN(),
                    SearchScope.SUBORDINATE_SUBTREE,
                    filter,
                    attributesToRetrieve
            );

            final SearchResult searchResult = connection.search(searchRequest);

            if (searchResult.getSearchEntries().isEmpty()) {
                return Collections.emptyMap();
            } else {
                return searchResult.getSearchEntries().get(0)
                        .getAttributes()
                        .stream()
                        .collect(Collectors.toMap(
                                Attribute::getName,
                                Attribute::getValues
                        ));
            }
        } catch (LDAPException e) {
            throw new ServiceAuthorizationException(ErrorCode.LDAP_ERROR, "Failed to authenticate user");
        }
    }

    private AccountBO.Builder mapAttributes(final Map<String, String[]> attributes, final Map<String, String> fieldMappings) {
        final AccountBO.Builder builder = AccountBO.builder();

        attributes.forEach((key, value) -> {
            final String mappedField = fieldMappings.get(key);

            if (mappedField != null) {
                switch (mappedField) {
                    case "id":
                        builder.id(value[0]);
                        break;

                    case "roles":
                        builder.roles(ImmutableList.copyOf(value));
                        break;

                    case "permissions":
                        builder.permissions(
                                Stream.of(value)
                                        .map(permission -> PermissionBO.builder().name(permission).build())
                                        .collect(Collectors.toList())
                        );
                        break;

                    case "scopes":
                        builder.scopes(ImmutableList.copyOf(value));
                        break;

                    case "emails":
                        builder.emails(
                                Stream.of(value).
                                        map(email -> AccountEmailBO.builder().email(email).build())
                                        .collect(Collectors.toList())
                        );
                        break;

                    default:
                        log.warn("Invalid field mapping to {}", mappedField);
                }
            }
        });

        return builder;
    }
}
