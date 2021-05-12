package com.nexblocks.authguard.jwt;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.Verification;
import com.nexblocks.authguard.dal.cache.AccountTokensRepository;
import com.nexblocks.authguard.dal.model.AccountTokenDO;
import com.nexblocks.authguard.service.config.EncryptionConfig;
import com.nexblocks.authguard.service.config.JwtConfig;
import com.nexblocks.authguard.service.config.StrategyConfig;
import com.nexblocks.authguard.service.exceptions.ServiceAuthorizationException;
import com.nexblocks.authguard.service.mappers.ServiceMapperImpl;
import com.nexblocks.authguard.service.model.*;
import io.vavr.control.Either;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AccessTokenProviderTest {
    private static final String ALGORITHM = "HMAC256";
    private static final String KEY = "src/test/resources/hmac256.pem";
    private static final String ISSUER = "test";

    private AccountTokensRepository accountTokensRepository;
    private JtiProvider jtiProvider;
    private TokenEncryptor tokenEncryptor;

    private final static EasyRandom RANDOM = new EasyRandom(new EasyRandomParameters().collectionSizeRange(1, 4));

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
                .build();
    }

    private StrategyConfig strategyConfigWithJti() {
        return StrategyConfig.builder()
                .tokenLife("5m")
                .refreshTokenLife("20m")
                .useJti(true)
                .includePermissions(true)
                .build();
    }

    private AccessTokenProvider newProviderInstance(final JwtConfig jwtConfig,
                                                    final StrategyConfig strategyConfig) {
        jtiProvider = Mockito.mock(JtiProvider.class);
        accountTokensRepository = Mockito.mock(AccountTokensRepository.class);
        tokenEncryptor = Mockito.mock(TokenEncryptor.class);

        Mockito.when(accountTokensRepository.save(Mockito.any())).thenAnswer(invocation -> {
            final AccountTokenDO arg = invocation.getArgument(0);
            return CompletableFuture.completedFuture(arg);
        });

        return new AccessTokenProvider(accountTokensRepository, jwtConfig, strategyConfig, jtiProvider,
                tokenEncryptor, new ServiceMapperImpl());
    }

    @Test
    void generate() {
        final AccessTokenProvider accessTokenProvider = newProviderInstance(jwtConfig(), strategyConfig());

        final AccountBO account = RANDOM.nextObject(AccountBO.class);
        final AuthResponseBO tokens = accessTokenProvider.generateToken(account);

        assertThat(tokens).isNotNull();
        assertThat(tokens.getToken()).isNotNull();
        assertThat(tokens.getRefreshToken()).isNotNull();
        assertThat(tokens.getToken()).isNotEqualTo(tokens.getRefreshToken());

        final ArgumentCaptor<AccountTokenDO> accountTokenCaptor = ArgumentCaptor.forClass(AccountTokenDO.class);

        Mockito.verify(accountTokensRepository).save(accountTokenCaptor.capture());

        assertThat(accountTokenCaptor.getValue().getAssociatedAccountId()).isEqualTo(account.getId());
        assertThat(accountTokenCaptor.getValue().getToken()).isEqualTo(tokens.getRefreshToken());
        assertThat(accountTokenCaptor.getValue().getExpiresAt()).isNotNull()
                .isAfter(OffsetDateTime.now());

        verifyToken(tokens.getToken().toString(), account.getId(), null, null);
    }

    @Test
    void generateEncrypted() {
        final AccessTokenProvider accessTokenProvider =
                newProviderInstance(jwtConfigWithEncryption(), strategyConfig());

        Mockito.when(tokenEncryptor.encryptAndEncode(Mockito.any()))
                .thenAnswer(invocation -> Either.right("encrypted"));

        final AccountBO account = RANDOM.nextObject(AccountBO.class);
        final AuthResponseBO tokens = accessTokenProvider.generateToken(account);

        assertThat(tokens).isNotNull();
        assertThat(tokens.getToken()).isEqualTo("encrypted");
        assertThat(tokens.getRefreshToken()).isNotNull();
        assertThat(tokens.getToken()).isNotEqualTo(tokens.getRefreshToken());

        final ArgumentCaptor<AccountTokenDO> accountTokenCaptor = ArgumentCaptor.forClass(AccountTokenDO.class);

        Mockito.verify(accountTokensRepository).save(accountTokenCaptor.capture());

        assertThat(accountTokenCaptor.getValue().getAssociatedAccountId()).isEqualTo(account.getId());
        assertThat(accountTokenCaptor.getValue().getToken()).isEqualTo(tokens.getRefreshToken());
        assertThat(accountTokenCaptor.getValue().getExpiresAt()).isNotNull()
                .isAfter(OffsetDateTime.now());

        // token cannot be verified directly since it's encrypted
    }

    @Test
    void generateWithRestrictions() {
        final AccessTokenProvider accessTokenProvider = newProviderInstance(jwtConfig(), strategyConfig());

        final AccountBO account = RANDOM.nextObject(AccountBO.class)
                .withPermissions(Arrays.asList(
                        PermissionBO.builder().group("super").name("permission-1").build(),
                        PermissionBO.builder().group("super").name("permission-2").build())
                );

        final TokenRestrictionsBO restrictions = TokenRestrictionsBO.builder()
                .addPermissions("super:permission-1")
                .build();

        final AuthResponseBO tokens = accessTokenProvider.generateToken(account, restrictions);

        assertThat(tokens).isNotNull();
        assertThat(tokens.getToken()).isNotNull();
        assertThat(tokens.getRefreshToken()).isNotNull();
        assertThat(tokens.getToken()).isNotEqualTo(tokens.getRefreshToken());

        final ArgumentCaptor<AccountTokenDO> accountTokenCaptor = ArgumentCaptor.forClass(AccountTokenDO.class);

        Mockito.verify(accountTokensRepository).save(accountTokenCaptor.capture());

        assertThat(accountTokenCaptor.getValue().getAssociatedAccountId()).isEqualTo(account.getId());
        assertThat(accountTokenCaptor.getValue().getToken()).isEqualTo(tokens.getRefreshToken());
        assertThat(accountTokenCaptor.getValue().getExpiresAt()).isNotNull()
                .isAfter(OffsetDateTime.now());
        assertThat(accountTokenCaptor.getValue().getTokenRestrictions())
                .isEqualToComparingFieldByField(restrictions);

        verifyToken(tokens.getToken().toString(), account.getId(), null, Collections.singletonList("permission-1"));
    }

    @Test
    void generateWithJti() {
        final AccessTokenProvider accessTokenProvider = newProviderInstance(jwtConfig(), strategyConfigWithJti());

        final String jti = UUID.randomUUID().toString();

        Mockito.when(jtiProvider.next()).thenReturn(jti);

        final AccountBO account = RANDOM.nextObject(AccountBO.class);
        final AuthResponseBO tokens = accessTokenProvider.generateToken(account);

        assertThat(tokens).isNotNull();
        assertThat(tokens.getToken()).isNotNull();
        assertThat(tokens.getRefreshToken()).isNotNull();
        assertThat(tokens.getToken()).isNotEqualTo(tokens.getRefreshToken());

        verifyToken(tokens.getToken().toString(), account.getId(), jti, null);
    }

    @Test
    void delete() {
        final AccessTokenProvider accessTokenProvider = newProviderInstance(jwtConfig(), strategyConfig());

        final String refreshToken = "refresh";
        final String accountId = "account";
        final AuthRequestBO deleteRequest = AuthRequestBO.builder().token(refreshToken).build();

        Mockito.when(accountTokensRepository.deleteToken(refreshToken))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(
                        AccountTokenDO.builder()
                                .associatedAccountId(accountId)
                                .token(refreshToken)
                                .build()
                )));

        final AuthResponseBO expected = AuthResponseBO.builder()
                .type("access_token")
                .entityType(EntityType.ACCOUNT)
                .entityId(accountId)
                .refreshToken(refreshToken)
                .build();

        final AuthResponseBO actual = accessTokenProvider.delete(deleteRequest);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void deleteInvalidToken() {
        final AccessTokenProvider accessTokenProvider = newProviderInstance(jwtConfig(), strategyConfig());

        final String refreshToken = "refresh";
        final AuthRequestBO deleteRequest = AuthRequestBO.builder().token(refreshToken).build();

        Mockito.when(accountTokensRepository.deleteToken(refreshToken))
                .thenReturn(CompletableFuture.completedFuture(Optional.empty()));

        assertThatThrownBy(() -> accessTokenProvider.delete(deleteRequest))
                .isInstanceOf(ServiceAuthorizationException.class);
    }

    private void verifyToken(final String token, final String subject, final String jti, final List<String> permissions) {
        final Verification verifier = JWT.require(JwtConfigParser.parseAlgorithm(ALGORITHM, null, KEY))
                .withIssuer(ISSUER)
                .withSubject(subject);

        if (jti != null) {
            verifier.withJWTId(jti);
        }

        final DecodedJWT decodedJWT = verifier.build().verify(token);

        if (permissions != null) {
            assertThat(decodedJWT.getClaim("permissions").asArray(String.class)).hasSameSizeAs(permissions);
        }
    }
}
