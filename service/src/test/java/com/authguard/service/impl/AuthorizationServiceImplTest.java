package com.authguard.service.impl;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.authguard.dal.AccountTokensRepository;
import com.authguard.service.AccountsService;
import com.authguard.service.AuthorizationService;
import com.authguard.service.JwtProvider;
import com.authguard.dal.model.AccountTokenDO;
import com.authguard.service.config.ImmutableStrategyConfig;
import com.authguard.service.exceptions.ServiceAuthorizationException;
import com.authguard.service.model.AccountBO;
import com.authguard.service.model.TokensBO;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthorizationServiceImplTest {
    private final EasyRandom random = new EasyRandom(new EasyRandomParameters()
            .collectionSizeRange(1, 4));

    private AuthorizationService authorizationService;
    private AccountsService accountsService;
    private JwtProvider idTokenProvider;
    private JwtProvider accessTokenProvider;
    private AccountTokensRepository accountTokensRepository;

    @BeforeEach
    void setup() {
        accountsService = Mockito.mock(AccountsService.class);
        idTokenProvider = Mockito.mock(JwtProvider.class);
        accessTokenProvider = Mockito.mock(JwtProvider.class);
        accountTokensRepository = Mockito.mock(AccountTokensRepository.class);

        final ImmutableStrategyConfig accessTokenStrategy = ImmutableStrategyConfig.builder()
                        .refreshTokenLife("5m")
                        .build();

        authorizationService = new AuthorizationServiceImpl(accountsService, idTokenProvider,
                accessTokenProvider, accountTokensRepository, accessTokenStrategy);
    }

    @Test
    void authorize() {
        // data
        final String accountId = "accountId";
        final String mockIdToken = "mock-jwt";
        final String authorizationHeader = "Bearer " + mockIdToken;

        final DecodedJWT mockJwt = Mockito.mock(DecodedJWT.class);

        final AccountBO account = random.nextObject(AccountBO.class);
        final TokensBO tokens = random.nextObject(TokensBO.class);

        // prepare mocks
        Mockito.when(mockJwt.getSubject()).thenReturn(accountId);
        Mockito.when(idTokenProvider.validateToken(mockIdToken))
                .thenReturn(Optional.of(mockJwt));
        Mockito.when(accountsService.getById(accountId)).thenReturn(Optional.of(account));
        Mockito.when(accessTokenProvider.generateToken(account)).thenReturn(tokens);

        // call
        final Optional<TokensBO> generatedTokens = authorizationService
                .authorize(authorizationHeader);

        // assert
        assertThat(generatedTokens).isPresent();
        assertThat(generatedTokens).contains(tokens);

        final ArgumentCaptor<AccountTokenDO> accountTokenArg = ArgumentCaptor
                .forClass(AccountTokenDO.class);

        Mockito.verify(accountTokensRepository).save(accountTokenArg.capture());

        final AccountTokenDO accountToken = accountTokenArg.getValue();

        assertThat(accountToken.getToken()).isEqualTo(tokens.getRefreshToken());
        assertThat(accountToken.getAssociatedAccountId()).isEqualTo(accountId);

        // since we can't know the exact expiration date those seem like reasonable limits
        final ZonedDateTime upperLimit = ZonedDateTime.now()
                .plus(Duration.ofMinutes(5));
        final ZonedDateTime lowerLimit = ZonedDateTime.now()
                .minus(Duration.ofSeconds(5))
                .plus(Duration.ofMinutes(5));

        assertThat(accountToken.expiresAt())
                .isBefore(upperLimit)
                .isAfter(lowerLimit);

    }

    @Test
    void refresh() {
        // data
        final String refreshToken = "refresh";
        final AccountTokenDO existingAccountToken = random.nextObject(AccountTokenDO.class)
                .withExpiresAt(ZonedDateTime.now().plus(Duration.ofMinutes(10)));

        final AccountBO account = random.nextObject(AccountBO.class)
                .withId(existingAccountToken.getAssociatedAccountId());
        final TokensBO tokens = random.nextObject(TokensBO.class);

        // prepare mocks
        Mockito.when(accountTokensRepository.getByToken(refreshToken))
                .thenReturn(Optional.of(existingAccountToken));

        Mockito.when(accountsService.getById(existingAccountToken.getAssociatedAccountId()))
                .thenReturn(Optional.of(account));
        Mockito.when(accessTokenProvider.generateToken(account)).thenReturn(tokens);

        // call
        final Optional<TokensBO> generatedTokens = authorizationService
                .refresh(refreshToken);

        // assert
        assertThat(generatedTokens).isPresent();
        assertThat(generatedTokens).contains(tokens);

        final ArgumentCaptor<AccountTokenDO> accountTokenArg = ArgumentCaptor
                .forClass(AccountTokenDO.class);

        Mockito.verify(accountTokensRepository).save(accountTokenArg.capture());

        final AccountTokenDO newAccountToken = accountTokenArg.getValue();

        assertThat(newAccountToken.getToken()).isEqualTo(tokens.getRefreshToken());
        assertThat(newAccountToken.getAssociatedAccountId()).isEqualTo(account.getId());

        // since we can't know the exact expiration date those seem like reasonable limits
        final ZonedDateTime upperLimit = ZonedDateTime.now()
                .plus(Duration.ofMinutes(5));
        final ZonedDateTime lowerLimit = ZonedDateTime.now()
                .minus(Duration.ofSeconds(5))
                .plus(Duration.ofMinutes(5));

        assertThat(newAccountToken.expiresAt())
                .isBefore(upperLimit)
                .isAfter(lowerLimit);
    }

    @Test
    void refreshExpired() {
        // data
        final String refreshToken = "refresh";
        final AccountTokenDO existingAccountToken = random.nextObject(AccountTokenDO.class)
                .withExpiresAt(ZonedDateTime.now().minus(Duration.ofMinutes(10)));

        // prepare mocks
        Mockito.when(accountTokensRepository.getByToken(refreshToken))
                .thenReturn(Optional.of(existingAccountToken));

        // call
        assertThatThrownBy(() -> authorizationService.refresh(refreshToken))
                .isInstanceOf(ServiceAuthorizationException.class);
    }
}