package com.authguard.service.jwt;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.Verification;
import com.authguard.config.ConfigContext;
import com.authguard.config.JacksonConfigContext;
import com.authguard.dal.AccountTokensRepository;
import com.authguard.dal.model.AccountTokenDO;
import com.authguard.service.model.AccountBO;
import com.authguard.service.model.PermissionBO;
import com.authguard.service.model.TokensBO;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AccessTokenProviderTest {
    private static final String ALGORITHM = "HMAC256";
    private static final String KEY = "this secret is only for testing purposes";
    private static final String ISSUER = "test";

    private AccountTokensRepository accountTokensRepository;
    private JtiProvider jtiProvider;

    private final static EasyRandom RANDOM = new EasyRandom(new EasyRandomParameters().collectionSizeRange(1, 4));

    private ConfigContext jwtConfig() {
        final ObjectNode configNode = new ObjectNode(JsonNodeFactory.instance);
        
        configNode.put("algorithm", ALGORITHM)
                .put("key", KEY)
                .put("issuer", ISSUER);
        
        return new JacksonConfigContext(configNode);
    }

    private ConfigContext strategyConfig(final boolean useJti) {
        final ObjectNode configNode = new ObjectNode(JsonNodeFactory.instance);

        configNode.put("tokenLife", "5m")
                .put("refreshTokenLife", "20m")
                .put("useJti", useJti)
                .put("includePermissions", true);

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
                .isAfter(ZonedDateTime.now());

        verifyToken(tokens.getToken(), account.getId(), null, null, null);
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

        verifyToken(tokens.getToken(), account.getId(), jti, null, null);
    }

    private void verifyToken(final String token, final String subject, final String jti,
                             final List<PermissionBO> permissions, final List<String> scopes) {
        final Verification verifier = JWT.require(Algorithm.HMAC256(KEY))
                .withIssuer(ISSUER)
                .withSubject(subject);

        if (jti != null) {
            verifier.withJWTId(jti);
        }

        final DecodedJWT decodedJWT = verifier.build().verify(token);

        if (permissions != null) {
            assertThat(decodedJWT.getClaim("permissions").asArray(String.class)).hasSameSizeAs(permissions);
        }

        if (scopes != null) {
            assertThat(decodedJWT.getClaim("scopes").asArray(String.class)).containsExactlyInAnyOrder(scopes.toArray(new String[0]));
        }
    }
}
