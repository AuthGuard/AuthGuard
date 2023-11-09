package com.nexblocks.authguard.jwt.exchange;

import com.nexblocks.authguard.dal.model.AccountTokenDO;
import com.nexblocks.authguard.dal.model.TokenRestrictionsDO;
import com.nexblocks.authguard.jwt.OpenIdConnectTokenProvider;
import com.nexblocks.authguard.jwt.oauth.AuthorizationCodeVerifier;
import com.nexblocks.authguard.service.AccountsService;
import com.nexblocks.authguard.service.mappers.ServiceMapper;
import com.nexblocks.authguard.service.mappers.ServiceMapperImpl;
import com.nexblocks.authguard.service.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

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
        AuthRequestBO authRequest = AuthRequestBO.builder()
                .token("auth code")
                .build();

        AccountTokenDO accountToken = AccountTokenDO.builder()
                .associatedAccountId(101)
                .sourceAuthType("authorizationCode")
                .clientId("client")
                .deviceId("device")
                .userAgent("test")
                .sourceIp("127.0.0.1")
                .externalSessionId("session")
                .build();

        AccountBO account = AccountBO.builder()
                .id(101)
                .build();

        AuthResponseBO authResponse = AuthResponseBO.builder()
                .token("OIDC")
                .build();

        TokenOptionsBO options = TokenOptionsBO.builder()
                .source("authorizationCode")
                .clientId(accountToken.getClientId())
                .deviceId(accountToken.getDeviceId())
                .userAgent(accountToken.getUserAgent())
                .externalSessionId(accountToken.getExternalSessionId())
                .sourceIp(accountToken.getSourceIp())
                .build();

        Mockito.when(authorizationCodeVerifier.verifyAndGetAccountTokenAsync(authRequest.getToken()))
                .thenReturn(CompletableFuture.completedFuture(accountToken));

        Mockito.when(accountsService.getByIdUnchecked(accountToken.getAssociatedAccountId()))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(account)));

        Mockito.when(openIdConnectTokenProvider.generateToken(account, null, options))
                .thenReturn(CompletableFuture.completedFuture(authResponse));

        AuthResponseBO actual = authorizationCodeToOidc.exchange(authRequest).join();

        assertThat(actual).isEqualTo(authResponse);
    }

    @Test
    void exchangeWithRestrictions() {
        AuthRequestBO authRequest = AuthRequestBO.builder()
                .token("auth code")
                .build();

        AccountTokenDO accountToken = AccountTokenDO.builder()
                .associatedAccountId(101)
                .sourceAuthType("authorizationCode")
                .tokenRestrictions(TokenRestrictionsDO.builder()
                        .scopes(Collections.emptySet())
                        .permissions(new HashSet<>(Arrays.asList("perm-1", "perm-2")))
                        .build())
                .clientId("client")
                .deviceId("device")
                .userAgent("test")
                .sourceIp("127.0.0.1")
                .externalSessionId("session")
                .build();

        AccountBO account = AccountBO.builder()
                .id(101)
                .build();

        AuthResponseBO authResponse = AuthResponseBO.builder()
                .token("OIDC")
                .build();

        final TokenRestrictionsBO restrictions = serviceMapper.toBO(accountToken.getTokenRestrictions());

        TokenOptionsBO options = TokenOptionsBO.builder()
                .source("authorizationCode")
                .clientId(accountToken.getClientId())
                .deviceId(accountToken.getDeviceId())
                .userAgent(accountToken.getUserAgent())
                .externalSessionId(accountToken.getExternalSessionId())
                .sourceIp(accountToken.getSourceIp())
                .build();

        Mockito.when(authorizationCodeVerifier.verifyAndGetAccountTokenAsync(authRequest.getToken()))
                .thenReturn(CompletableFuture.completedFuture(accountToken));

        Mockito.when(accountsService.getByIdUnchecked(accountToken.getAssociatedAccountId()))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(account)));

        Mockito.when(openIdConnectTokenProvider.generateToken(account, serviceMapper.toBO(accountToken.getTokenRestrictions()), options))
                .thenReturn(CompletableFuture.completedFuture(authResponse));

        AuthResponseBO actual = authorizationCodeToOidc.exchange(authRequest).join();

        assertThat(actual).isEqualTo(authResponse);
    }
}