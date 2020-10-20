package com.authguard.ldap;

import com.authguard.service.model.AccountBO;
import com.authguard.service.model.AccountEmailBO;
import com.authguard.service.model.PermissionBO;
import com.google.common.collect.ImmutableList;
import com.unboundid.ldap.sdk.Attribute;
import com.unboundid.ldap.sdk.SearchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class LdapAccountMapper {
    private static final Logger LOG = LoggerFactory.getLogger(LdapAccountMapper.class);

    public Map<String, String[]> getAttributes(final SearchResult searchResult) {
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
    }

    public AccountBO.Builder mapAttributes(final Map<String, String[]> attributes, final Map<String, String> fieldMappings) {
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

                    case "emails":
                        builder.emails(
                                Stream.of(value).
                                        map(email -> AccountEmailBO.builder().email(email).build())
                                        .collect(Collectors.toList())
                        );
                        break;

                    default:
                        LOG.warn("Invalid field mapping to {}", mappedField);
                }
            }
        });

        return builder;
    }
}
