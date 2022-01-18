package com.nexblocks.authguard.jwt.exchange;

import com.nexblocks.authguard.dal.model.AccountTokenDO;
import com.nexblocks.authguard.dal.model.TokenRestrictionsDO;
import com.nexblocks.authguard.jwt.OpenIdConnectTokenProvider;
import com.nexblocks.authguard.jwt.oauth.AuthorizationCodeVerifier;
import com.nexblocks.authguard.service.AccountsService;
import com.nexblocks.authguard.service.mappers.ServiceMapper;
import com.nexblocks.authguard.service.mappers.ServiceMapperImpl;
import com.nexblocks.authguard.service.model.AccountBO;
import com.nexblocks.authguard.service.model.AuthRequestBO;
import com.nexblocks.authguard.service.model.AuthResponseBO;
import com.nexblocks.authguard.service.model.TokenRestrictionsBO;
import io.vavr.control.Either;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class AuthorizationCodeToOidcTest {
    private AuthorizationCodeVerifier authorizationCodeVerifier;
    private AccountsService accountsService;
    private OpenIdConnectTokenProvider openIdConnectTokenProvider;
    private ServiceMapper serviceMapper;

    private AuthorizationCodeToOidc authorizationCodeToOidc;

    @BeforeEach
    void setup() {
        authorizationCodeVerifier = Mockito.mock(AuthorizationCodeVerifier.class);
        accountsService = Mockito.mock(AccountsService.class);
        openIdConnectTokenProvider = Mockito.mock(OpenIdConnectTokenProvider.class);
        serviceMapper = new ServiceMapperImpl();

        authorizationCodeToOidc = new AuthorizationCodeToOidc(
                new AccountsServiceAdapter(accountsService),
                authorizationCodeVerifier,
                openIdConnectTokenProvider,
                serviceMapper
        );
    }

    @Test
    void exchange() {
        final AuthRequestBO authRequest = AuthRequestBO.builder()
                .token("auth code")
                .build();

        final AccountTokenDO accountToken = AccountTokenDO.builder()
                .associatedAccountId("account")
                .build();

        final AccountBO account = AccountBO.builder()
                .id("account")
                .build();

        final AuthResponseBO authResponse = AuthResponseBO.builder()
                .token("OIDC")
                .build();

        Mockito.when(authorizationCodeVerifier.verifyAndGetAccountToken(authRequest.getToken()))
                .thenReturn(Either.right(accountToken));

        Mockito.when(accountsService.getById(accountToken.getAssociatedAccountId()))
                .thenReturn(Optional.of(account));

        Mockito.when(openIdConnectTokenProvider.generateToken(account, (TokenRestrictionsBO) null))
                .thenReturn(authResponse);

        final Either<Exception, AuthResponseBO> actual = authorizationCodeToOidc.exchange(authRequest);

        assertThat(actual.isRight());
        assertThat(actual.get()).isEqualTo(authResponse);
    }

    @Test
    void exchangeWithRestrictions() {
        final AuthRequestBO authRequest = AuthRequestBO.builder()
                .token("auth code")
                .build();

        final AccountTokenDO accountToken = AccountTokenDO.builder()
                .associatedAccountId("account")
                .tokenRestrictions(TokenRestrictionsDO.builder()
                        .scopes(Collections.emptySet())
                        .permissions(new HashSet<>(Arrays.asList("perm-1", "perm-2")))
                        .build())
                .build();

        final AccountBO account = AccountBO.builder()
                .id("account")
                .build();

        final AuthResponseBO authResponse = AuthResponseBO.builder()
                .token("OIDC")
                .build();

        Mockito.when(authorizationCodeVerifier.verifyAndGetAccountToken(authRequest.getToken()))
                .thenReturn(Either.right(accountToken));

        Mockito.when(accountsService.getById(accountToken.getAssociatedAccountId()))
                .thenReturn(Optional.of(account));

        Mockito.when(openIdConnectTokenProvider.generateToken(account, serviceMapper.toBO(accountToken.getTokenRestrictions())))
                .thenReturn(authResponse);

        final Either<Exception, AuthResponseBO> actual = authorizationCodeToOidc.exchange(authRequest);

        assertThat(actual.isRight());
        assertThat(actual.get()).isEqualTo(authResponse);
    }
}