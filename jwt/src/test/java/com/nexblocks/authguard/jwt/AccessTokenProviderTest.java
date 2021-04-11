package com.nexblocks.authguard.jwt;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.Verification;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.nexblocks.authguard.config.ConfigContext;
import com.nexblocks.authguard.config.JacksonConfigContext;
import com.nexblocks.authguard.dal.cache.AccountTokensRepository;
import com.nexblocks.authguard.dal.model.AccountTokenDO;
import com.nexblocks.authguard.service.exceptions.ServiceAuthorizationException;
import com.nexblocks.authguard.service.model.*;
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

    private final static EasyRandom RANDOM = new EasyRandom(new EasyRandomParameters().collectionSizeRange(1, 4));

    private ConfigContext jwtConfig() {
        final ObjectNode configNode = new ObjectNode(JsonNodeFactory.instance);
        
        configNode.put("algorithm", ALGORITHM)
                .put("privateKey", KEY)
                .put("issuer", ISSUER);
        
        return new JacksonConfigContext(configNode);
    }

    private ConfigContext strategyConfig(final boolean useJti) {
        final ObjectNode configNode = new ObjectNode(JsonNodeFactory.instance);

        configNode.put("tokenLife", "5m")
                .put("refreshTokenLife", "20m")
                .put("useJti", useJti)
                .put("includePermissions", true)
                .put("includeScopes", true);

        return new JacksonConfigContext(configNode);
    }

    private AccessTokenProvider newProviderInstance(final ConfigContext strategyConfig) {
        jtiProvider = Mockito.mock(JtiProvider.class);
        accountTokensRepository = Mockito.mock(AccountTokensRepository.class);

        Mockito.when(accountTokensRepository.save(Mockito.any())).thenAnswer(invocation -> {
            final AccountTokenDO arg = invocation.getArgument(0);
            return CompletableFuture.completedFuture(arg);
        });

        return new AccessTokenProvider(accountTokensRepository, jwtConfig(), strategyConfig, jtiProvider);
    }

    @Test
    void generate() {
        final ConfigContext strategyConfig = strategyConfig(false);

        final AccessTokenProvider accessTokenProvider = newProviderInstance(strategyConfig);

        final AccountBO account = RANDOM.nextObject(AccountBO.class);
        final TokensBO tokens = accessTokenProvider.generateToken(account);

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
    void generateWithRestrictions() {
        final ConfigContext strategyConfig = strategyConfig(false);

        final AccessTokenProvider accessTokenProvider = newProviderInstance(strategyConfig);

        final AccountBO account = RANDOM.nextObject(AccountBO.class)
                .withPermissions(Arrays.asList(
                        PermissionBO.builder().group("super").name("permission-1").build(),
                        PermissionBO.builder().group("super").name("permission-2").build())
                );

        final TokenRestrictionsBO restrictions = TokenRestrictionsBO.builder()
                .addPermissions("super.permission-1")
                .build();

        final TokensBO tokens = accessTokenProvider.generateToken(account, restrictions);

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

        verifyToken(tokens.getToken().toString(), account.getId(), null, Collections.singletonList("permission-1"));
    }

    @Test
    void generateWithJti() {
        final ConfigContext strategyConfig = strategyConfig(true);

        final AccessTokenProvider accessTokenProvider = newProviderInstance(strategyConfig);

        final String jti = UUID.randomUUID().toString();

        Mockito.when(jtiProvider.next()).thenReturn(jti);

        final AccountBO account = RANDOM.nextObject(AccountBO.class);
        final TokensBO tokens = accessTokenProvider.generateToken(account);

        assertThat(tokens).isNotNull();
        assertThat(tokens.getToken()).isNotNull();
        assertThat(tokens.getRefreshToken()).isNotNull();
        assertThat(tokens.getToken()).isNotEqualTo(tokens.getRefreshToken());

        verifyToken(tokens.getToken().toString(), account.getId(), jti, null);
    }

    @Test
    void delete() {
        final ConfigContext strategyConfig = strategyConfig(false);
        final AccessTokenProvider accessTokenProvider = newProviderInstance(strategyConfig);

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

        final TokensBO expected = TokensBO.builder()
                .type("access_token")
                .entityType(EntityType.ACCOUNT)
                .entityId(accountId)
                .refreshToken(refreshToken)
                .build();

        final TokensBO actual = accessTokenProvider.delete(deleteRequest);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void deleteInvalidToken() {
        final ConfigContext strategyConfig = strategyConfig(false);
        final AccessTokenProvider accessTokenProvider = newProviderInstance(strategyConfig);

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
