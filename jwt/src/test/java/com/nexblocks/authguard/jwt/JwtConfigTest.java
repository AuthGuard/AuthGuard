package com.nexblocks.authguard.jwt;

import com.nexblocks.authguard.config.ConfigContext;
import org.jeasy.random.EasyRandom;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mockito;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class JwtConfigTest {
    private static final String ALGORITHM_CONFIG_PROPERTY = "algorithm";
    private static final String KEY_CONFIG_PROPERTY = "key";
    private static final String ISSUER_CONFIG_PROPERTY = "issuer";
    private static final String JTI_CONFIG_PROPERTY = "strategy.useJti";
    private static final String TOKEN_LIFE_PROPERTY = "tokenLife";
    private static final String REFRESH_TOKEN_LIFE_PROPERTY = "refreshTokenLife";

    private static EasyRandom easyRandom = new EasyRandom();

    private ConfigContext configContext;

    @BeforeAll
    void initConfig() {
        configContext = Mockito.mock(ConfigContext.class);
    }

    @AfterEach
    void reset() {
        Mockito.reset(configContext);
    }

//    @Test
//    void algorithm() {
//        final JwtConfigInterface jwtConfig = new JwtConfigInterface(configContext);
//
//        final String algorithm = "HMAC256";
//        final String key = "This is a test key";
//
//        Mockito.when(configContext.getAsString(ALGORITHM_CONFIG_PROPERTY)).thenReturn(algorithm);
//        Mockito.when(configContext.getAsString(KEY_CONFIG_PROPERTY)).thenReturn(key);
//
//        final Algorithm value = jwtConfig.algorithm();
//
//        assertThat(value).isNotNull();
//        assertThat(value.getName()).isEqualTo("HS256");
//    }
//
//    @Test
//    void verifier() {
//        final JwtConfigInterface jwtConfig = new JwtConfigInterface(configContext);
//
//        final String algorithm = "HMAC256";
//        final String key = "This is a test key";
//
//        Mockito.when(configContext.getAsString(ALGORITHM_CONFIG_PROPERTY)).thenReturn(algorithm);
//        Mockito.when(configContext.getAsString(KEY_CONFIG_PROPERTY)).thenReturn(key);
//
//        final JWTVerifier verifier = jwtConfig.verifier();
//
//        assertThat(verifier).isNotNull();
//    }
//
//    @Test
//    void issuer() {
//        final JwtConfigInterface jwtConfig = new JwtConfigInterface(configContext);
//
//        final String issuer = easyRandom.nextObject(String.class);
//
//        Mockito.when(configContext.getAsString(ISSUER_CONFIG_PROPERTY)).thenReturn(issuer);
//
//        assertThat(jwtConfig.issuer()).isEqualTo(issuer);
//    }
//
//    @Test
//    void useJti() {
//        final JwtConfigInterface jwtConfig = new JwtConfigInterface(configContext);
//
//        final boolean useJti = easyRandom.nextBoolean();
//
//        Mockito.when(configContext.getAsBoolean(JTI_CONFIG_PROPERTY)).thenReturn(useJti);
//
//        assertThat(jwtConfig.useJti()).isEqualTo(useJti);
//    }
//
//    @Test
//    void tokenLife() {
//        final JwtConfigInterface jwtConfig = new JwtConfigInterface(configContext);
//
//        final int amount = easyRandom.nextInt();
//        final String tokenLife = amount + "m";
//
//        Mockito.when(configContext.getAsString(TOKEN_LIFE_PROPERTY)).thenReturn(tokenLife);
//
//        final Duration tokenLifeDuration = jwtConfig.tokenLife();
//
//        assertThat(tokenLifeDuration.toMinutes()).isEqualTo(amount);
//    }
//
//    @Test
//    void refreshTokenLife() {
//        final JwtConfigInterface jwtConfig = new JwtConfigInterface(configContext);
//
//        final int amount = easyRandom.nextInt();
//        final String tokenLife = amount + "m";
//
//        Mockito.when(configContext.getAsString(REFRESH_TOKEN_LIFE_PROPERTY)).thenReturn(tokenLife);
//
//        final Duration tokenLifeDuration = jwtConfig.refreshTokenLife();
//
//        assertThat(tokenLifeDuration.toMinutes()).isEqualTo(amount);
//    }
}