package com.nexblocks.authguard.jwt;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.Verification;
import com.nexblocks.authguard.dal.cache.AccountTokensRepository;
import com.nexblocks.authguard.dal.model.AccountTokenDO;
import com.nexblocks.authguard.jwt.crypto.TokenEncryptorAdapter;
import com.nexblocks.authguard.service.TrackingSessionsService;
import com.nexblocks.authguard.service.config.EncryptionConfig;
import com.nexblocks.authguard.service.config.JwtConfig;
import com.nexblocks.authguard.service.config.StrategyConfig;
import com.nexblocks.authguard.service.exceptions.ServiceAuthorizationException;
import com.nexblocks.authguard.service.exceptions.ServiceException;
import com.nexblocks.authguard.service.mappers.ServiceMapperImpl;
import com.nexblocks.authguard.service.model.*;
import io.smallrye.mutiny.Uni;
import io.vavr.control.Either;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AccessTokenProviderTest {
    private static final String ALGORITHM = "HMAC256";
    private static final String KEY = "file:src/test/resources/hmac256.pem";
    private static final String ISSUER = "test";
    private static final String[] SKIPPED_FIELDS = new String[] { "id", "createdAt", "lastModifiedAt",
            "token", "expiresAt" };

    private TrackingSessionsService trackingSessionsService;
    private AccountTokensRepository accountTokensRepository;
    private JtiProvider jtiProvider;
    private TokenEncryptorAdapter tokenEncryptor;

    private static final EasyRandom RANDOM = new EasyRandom(new EasyRandomParameters()
            .excludeField(field -> field.getName().equals("initShim"))
            .collectionSizeRange(1, 4));

    private JwtConfig jwtConfig() {
        return JwtConfig.builder()
                .algorithm(ALGORITHM)
                .privateKey(KEY)
                .issuer(ISSUER)
                .build();
    }

    private JwtConfig jwtConfigWithEncryption() {
        return JwtConfig.builder()
                .algorithm(ALGORITHM)
                .privateKey(KEY)
                .issuer(ISSUER)
                .encryption(EncryptionConfig.builder()
                        .build())
                .build();
    }

    private StrategyConfig strategyConfig() {
        return StrategyConfig.builder()
                .tokenLife("5m")
                .refreshTokenLife("20m")
                .useJti(false)
                .includePermissions(true)
                .includeRoles(true)
                .includeVerification(true)
                .build();
    }

    private StrategyConfig strategyConfigWithJti() {
        return StrategyConfig.builder()
                .tokenLife("5m")
                .refreshTokenLife("20m")
                .useJti(true)
                .build();
    }

    private AccessTokenProvider newProviderInstance(JwtConfig jwtConfig,
                                                    StrategyConfig strategyConfig) {
        jtiProvider = Mockito.mock(JtiProvider.class);
        accountTokensRepository = Mockito.mock(AccountTokensRepository.class);
        trackingSessionsService = Mockito.mock(TrackingSessionsService.class);
        tokenEncryptor = Mockito.mock(TokenEncryptorAdapter.class);

        Mockito.when(trackingSessionsService.isSessionActive(Mockito.eq("tracking-session"), Mockito.any()))
                .thenReturn(CompletableFuture.completedFuture(true));

        Mockito.when(trackingSessionsService.isSessionActive(Mockito.eq("terminated"), Mockito.any()))
                .thenReturn(CompletableFuture.completedFuture(false));

        Mockito.when(accountTokensRepository.save(Mockito.any())).thenAnswer(invocation -> {
            AccountTokenDO arg = invocation.getArgument(0);
            return Uni.createFrom().item(arg);
        });

        return new AccessTokenProvider(trackingSessionsService, accountTokensRepository, jwtConfig,
                strategyConfig, jtiProvider, tokenEncryptor, new ServiceMapperImpl());
    }

    @Test
    void generate() {
        AccessTokenProvider accessTokenProvider = newProviderInstance(jwtConfig(), strategyConfig());

        AccountBO account = RANDOM.nextObject(AccountBO.class).withActive(true);
        TokenOptionsBO options = TokenOptionsBO.builder()
                .trackingSession("tracking-session")
                .build();

        AuthResponseBO tokens = accessTokenProvider.generateToken(account, options).join();

        assertThat(tokens).isNotNull();
        assertThat(tokens.getToken()).isNotNull();
        assertThat(tokens.getRefreshToken()).isNotNull();
        assertThat(tokens.getToken()).isNotEqualTo(tokens.getRefreshToken());

        ArgumentCaptor<AccountTokenDO> accountTokenCaptor = ArgumentCaptor.forClass(AccountTokenDO.class);

        Mockito.verify(accountTokensRepository).save(accountTokenCaptor.capture());

        assertThat(accountTokenCaptor.getValue().getAssociatedAccountId()).isEqualTo(account.getId());
        assertThat(accountTokenCaptor.getValue().getToken()).isEqualTo(tokens.getRefreshToken());
        assertThat(accountTokenCaptor.getValue().getExpiresAt()).isNotNull()
                .isAfter(Instant.now());

        verifyToken(tokens.getToken().toString(), account.getId(), null, null);
    }

    @Test
    void generateWithTerminatedSession() {
        AccessTokenProvider accessTokenProvider = newProviderInstance(jwtConfig(), strategyConfig());
        TokenOptionsBO options = TokenOptionsBO.builder()
                .trackingSession("terminated")
                .build();

        AccountBO account = RANDOM.nextObject(AccountBO.class).withActive(true);

        assertThatThrownBy(() -> accessTokenProvider.generateToken(account, options).join())
                .hasCauseInstanceOf(ServiceException.class);
    }

    @Test
    void generateForInactiveAccount() {
        AccessTokenProvider accessTokenProvider = newProviderInstance(jwtConfig(), strategyConfig());

        AccountBO account = RANDOM.nextObject(AccountBO.class).withActive(false);

        assertThatThrownBy(() -> accessTokenProvider.generateToken(account))
                .isInstanceOf(ServiceAuthorizationException.class);
    }

    @Test
    void generateEncrypted() {
        AccessTokenProvider accessTokenProvider =
                newProviderInstance(jwtConfigWithEncryption(), strategyConfig());

        Mockito.when(tokenEncryptor.encryptAndEncode(Mockito.any()))
                .thenAnswer(invocation -> Either.right("encrypted"));

        AccountBO account = RANDOM.nextObject(AccountBO.class).withActive(true);
        TokenOptionsBO options = TokenOptionsBO.builder()
                .trackingSession("tracking-session")
                .build();

        AuthResponseBO tokens = accessTokenProvider.generateToken(account, options).join();

        assertThat(tokens).isNotNull();
        assertThat(tokens.getToken()).isEqualTo("encrypted");
        assertThat(tokens.getRefreshToken()).isNotNull();
        assertThat(tokens.getToken()).isNotEqualTo(tokens.getRefreshToken());

        ArgumentCaptor<AccountTokenDO> accountTokenCaptor = ArgumentCaptor.forClass(AccountTokenDO.class);

        Mockito.verify(accountTokensRepository).save(accountTokenCaptor.capture());

        assertThat(accountTokenCaptor.getValue().getAssociatedAccountId()).isEqualTo(account.getId());
        assertThat(accountTokenCaptor.getValue().getToken()).isEqualTo(tokens.getRefreshToken());
        assertThat(accountTokenCaptor.getValue().getExpiresAt()).isNotNull()
                .isAfter(Instant.now());

        // token cannot be verified directly since it's encrypted
    }

    @Test
    void generateWithRestrictions() {
        AccessTokenProvider accessTokenProvider = newProviderInstance(jwtConfig(), strategyConfig());

        AccountBO account = RANDOM.nextObject(AccountBO.class)
                .withActive(true)
                .withPermissions(Arrays.asList(
                        PermissionBO.builder().group("super").name("permission-1").build(),
                        PermissionBO.builder().group("super").name("permission-2").build())
                );

        TokenRestrictionsBO restrictions = TokenRestrictionsBO.builder()
                .addPermissions("super:permission-1")
                .build();

        TokenOptionsBO options = TokenOptionsBO.builder()
                .trackingSession("tracking-session")
                .build();

        AuthResponseBO tokens = accessTokenProvider.generateToken(account, restrictions, options).join();

        assertThat(tokens).isNotNull();
        assertThat(tokens.getToken()).isNotNull();
        assertThat(tokens.getRefreshToken()).isNotNull();
        assertThat(tokens.getToken()).isNotEqualTo(tokens.getRefreshToken());

        ArgumentCaptor<AccountTokenDO> accountTokenCaptor = ArgumentCaptor.forClass(AccountTokenDO.class);

        Mockito.verify(accountTokensRepository).save(accountTokenCaptor.capture());

        assertThat(accountTokenCaptor.getValue().getAssociatedAccountId()).isEqualTo(account.getId());
        assertThat(accountTokenCaptor.getValue().getToken()).isEqualTo(tokens.getRefreshToken());
        assertThat(accountTokenCaptor.getValue().getExpiresAt()).isNotNull()
                .isAfter(Instant.now());
        assertThat(accountTokenCaptor.getValue().getTokenRestrictions())
                .usingRecursiveComparison()
                .isEqualTo(restrictions);

        verifyToken(tokens.getToken().toString(), account.getId(), null, Collections.singletonList("permission-1"));
    }

    @Test
    void generateWithOptions() {
        AccessTokenProvider accessTokenProvider = newProviderInstance(jwtConfig(), strategyConfig());

        AccountBO account = RANDOM.nextObject(AccountBO.class)
                .withActive(true)
                .withPermissions(Arrays.asList(
                        PermissionBO.builder().group("super").name("permission-1").build(),
                        PermissionBO.builder().group("super").name("permission-2").build())
                );

        TokenOptionsBO tokenOptions = TokenOptionsBO.builder()
                .clientId("client-1")
                .source("basic")
                .deviceId("device-1")
                .sourceIp("127.0.0.1")
                .externalSessionId("session-1")
                .userAgent("test")
                .trackingSession("tracking-session")
                .build();

        AuthResponseBO tokens = accessTokenProvider.generateToken(account, tokenOptions).join();

        assertThat(tokens).isNotNull();
        assertThat(tokens.getToken()).isNotNull();
        assertThat(tokens.getRefreshToken()).isNotNull();
        assertThat(tokens.getToken()).isNotEqualTo(tokens.getRefreshToken());

        ArgumentCaptor<AccountTokenDO> accountTokenCaptor = ArgumentCaptor.forClass(AccountTokenDO.class);
        AccountTokenDO expectedRefreshToken = AccountTokenDO.builder()
                .associatedAccountId(account.getId())
                .clientId("client-1")
                .sourceAuthType("basic")
                .deviceId("device-1")
                .sourceIp("127.0.0.1")
                .externalSessionId("session-1")
                .userAgent("test")
                .trackingSession("tracking-session")
                .build();

        Mockito.verify(accountTokensRepository).save(accountTokenCaptor.capture());

        assertThat(accountTokenCaptor.getValue())
                .usingRecursiveComparison()
                .ignoringFields(SKIPPED_FIELDS)
                .isEqualTo(expectedRefreshToken);

        assertThat(accountTokenCaptor.getValue().getToken()).isEqualTo(tokens.getRefreshToken());
        assertThat(accountTokenCaptor.getValue().getExpiresAt()).isNotNull()
                .isAfter(Instant.now());
    }

    @Test
    void generateWithJti() {
        AccessTokenProvider accessTokenProvider = newProviderInstance(jwtConfig(), strategyConfigWithJti());

        String jti = UUID.randomUUID().toString();

        Mockito.when(jtiProvider.next()).thenReturn(jti);

        AccountBO account = RANDOM.nextObject(AccountBO.class).withActive(true);
        TokenOptionsBO options = TokenOptionsBO.builder()
                .trackingSession("tracking-session")
                .build();
        AuthResponseBO tokens = accessTokenProvider.generateToken(account, options).join();

        assertThat(tokens).isNotNull();
        assertThat(tokens.getToken()).isNotNull();
        assertThat(tokens.getRefreshToken()).isNotNull();
        assertThat(tokens.getToken()).isNotEqualTo(tokens.getRefreshToken());

        verifyToken(tokens.getToken().toString(), account.getId(), jti, null);
    }

    @Test
    void delete() {
        AccessTokenProvider accessTokenProvider = newProviderInstance(jwtConfig(), strategyConfig());

        String refreshToken = "refresh";
        long accountId = 101;
        AuthRequestBO deleteRequest = AuthRequestBO.builder().token(refreshToken).build();

        Mockito.when(accountTokensRepository.deleteToken(refreshToken))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(
                        AccountTokenDO.builder()
                                .associatedAccountId(accountId)
                                .token(refreshToken)
                                .build()
                )));

        AuthResponseBO expected = AuthResponseBO.builder()
                .type("accessToken")
                .entityType(EntityType.ACCOUNT)
                .entityId(accountId)
                .refreshToken(refreshToken)
                .build();

        AuthResponseBO actual = accessTokenProvider.delete(deleteRequest).join();

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void deleteInvalidToken() {
        AccessTokenProvider accessTokenProvider = newProviderInstance(jwtConfig(), strategyConfig());

        String refreshToken = "refresh";
        AuthRequestBO deleteRequest = AuthRequestBO.builder().token(refreshToken).build();

        Mockito.when(accountTokensRepository.deleteToken(refreshToken))
                .thenReturn(CompletableFuture.completedFuture(Optional.empty()));

        assertThatThrownBy(() -> accessTokenProvider.delete(deleteRequest).join())
                .hasCauseInstanceOf(ServiceAuthorizationException.class);
    }

    private void verifyToken(String token, long subject, String jti, List<String> permissions) {
        Verification verifier = JWT.require(JwtConfigParser.parseAlgorithm(ALGORITHM, null, KEY))
                .withIssuer(ISSUER)
                .withSubject("" + subject);

        if (jti != null) {
            verifier.withJWTId(jti);
        }

        DecodedJWT decodedJWT = verifier.build().verify(token);

        if (permissions != null) {
            assertThat(decodedJWT.getClaim("permissions").asArray(String.class)).hasSameSizeAs(permissions);
        }
    }
}
