package com.nexblocks.authguard.external.sms.subscribers;

import com.google.common.collect.ImmutableMap;
import com.nexblocks.authguard.service.model.AccountBO;
import com.nexblocks.authguard.service.model.TokenOptionsBO;

class SmsParametersHelper {
    static ImmutableMap.Builder<String, String> getForAccount(final AccountBO account,
                                                              final TokenOptionsBO tokenOptions) {
        final ImmutableMap.Builder<String, String> parameters = ImmutableMap.builder();

        if (account.getFirstName() != null) {
            parameters.put("firstName", account.getFirstName());
        }

        if (account.getLastName() != null) {
            parameters.put("lastName", account.getLastName());
        }

        if (tokenOptions.getUserAgent() != null) {
            parameters.put("userAgent", tokenOptions.getUserAgent());
        }

        if (tokenOptions.getSourceIp() != null) {
            parameters.put("sourceIp", tokenOptions.getSourceIp());;
        }

        return parameters;
    }
}
