package com.authguard.service.exchange;

import com.authguard.dal.model.AccountTokenDO;
import com.authguard.service.AccountsService;
import com.authguard.service.exceptions.ServiceAuthorizationException;
import com.authguard.service.exceptions.codes.ErrorCode;
import com.authguard.service.jwt.AccessTokenProvider;
import com.authguard.service.mappers.ServiceMapper;
import com.authguard.service.model.TokenRestrictionsBO;
import com.authguard.service.model.TokensBO;
import com.authguard.service.oauth.AuthorizationCodeVerifier;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

@TokenExchange(from = "authorizationCode", to = "accessToken")
public class AuthorizationCodeToAccessToken implements Exchange {
    private static final Logger log = LoggerFactory.getLogger(AuthorizationCodeToAccessToken.class);

    private final AccountsService accountsService;
    private final AuthorizationCodeVerifier authorizationCodeVerifier;
    private final AccessTokenProvider accessTokenProvider;
    private final ServiceMapper serviceMapper;

    @Inject
    public AuthorizationCodeToAccessToken(final AccountsService accountsService,
                                          final AuthorizationCodeVerifier authorizationCodeVerifier,
                                          final AccessTokenProvider accessTokenProvider,
                                          final ServiceMapper serviceMapper) {
        this.accountsService = accountsService;
        this.authorizationCodeVerifier = authorizationCodeVerifier;
        this.accessTokenProvider = accessTokenProvider;
        this.serviceMapper = serviceMapper;
    }

    @Override
    public Optional<TokensBO> exchangeToken(final String authorizationCode) {
        return authorizationCodeVerifier.verifyAndGetAccountToken(authorizationCode)
                .flatMap(this::generateToken);
    }

    @Override
    public Optional<TokensBO> exchangeToken(final String authorizationCode, final TokenRestrictionsBO restrictions) {
        return authorizationCodeVerifier.verifyAccountToken(authorizationCode)
                .flatMap(accountsService::getById)
                .map(account -> accessTokenProvider.generateToken(account, restrictions));
    }

    private Optional<TokensBO> generateToken(final AccountTokenDO accountToken) {
        if (accountToken.getAdditionalInformation() == null) {
            return generateWithNoRestrictions(accountToken);
        } else {
            if (TokenRestrictionsBO.class.isAssignableFrom(accountToken.getAdditionalInformation().getClass())) {
                return accountsService.getById(accountToken.getAssociatedAccountId())
                        .map(account -> accessTokenProvider
                                .generateToken(account, serviceMapper.toBO(accountToken.getTokenRestrictions())));
            } else {
                throw new ServiceAuthorizationException(ErrorCode.INVALID_ADDITIONAL_INFORMATION_TYPE,
                        "Found additional information of wrong type " + accountToken.getAdditionalInformation().getClass());
            }
        }
    }

    private Optional<TokensBO> generateWithNoRestrictions(final AccountTokenDO accountToken) {
        return accountsService.getById(accountToken.getAssociatedAccountId())
                .map(accessTokenProvider::generateToken);
    }
}
