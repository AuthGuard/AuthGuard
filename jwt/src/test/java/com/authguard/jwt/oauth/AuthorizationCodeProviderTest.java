package com.authguard.jwt.oauth;

import com.authguard.config.ConfigContext;
import com.authguard.config.JacksonConfigContext;
import com.authguard.dal.cache.AccountTokensRepository;
import com.authguard.dal.model.AccountTokenDO;
import com.authguard.service.mappers.ServiceMapperImpl;
import com.authguard.service.model.AccountBO;
import com.authguard.service.model.TokensBO;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.time.Duration;
import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class AuthorizationCodeProviderTest {

    private ConfigContext config() {
        final ObjectNode configNode = new ObjectNode(JsonNodeFactory.instance)
                .put("lifeTime", "5m")
                .put("randomSize", 128);

        return new JacksonConfigContext(configNode);
    }

    @Test
    void generateToken() {
        final AccountTokensRepository accountTokensRepository = Mockito.mock(AccountTokensRepository.class);
        final AuthorizationCodeProvider authorizationCodeProvider =
                new AuthorizationCodeProvider(accountTokensRepository, new ServiceMapperImpl(), config());

        final AccountBO account = AccountBO.builder()
                .id("account-id")
                .build();

        final TokensBO tokens = authorizationCodeProvider.generateToken(account);

        assertThat(tokens.getType()).isEqualTo("authorizationCode");
        assertThat(tokens.getToken()).isNotNull();
        assertThat(tokens.getRefreshToken()).isNull();

        final ArgumentCaptor<AccountTokenDO> argCaptor = ArgumentCaptor.forClass(AccountTokenDO.class);

        Mockito.verify(accountTokensRepository, Mockito.times(1))
                .save(argCaptor.capture());

        assertThat(argCaptor.getValue().getToken()).isEqualTo(tokens.getToken());
        assertThat(argCaptor.getValue().getAssociatedAccountId()).isEqualTo(account.getId());
        assertThat(argCaptor.getValue().getExpiresAt())
                .isAfter(ZonedDateTime.now())
                .isBefore(ZonedDateTime.now().plus(Duration.ofMinutes(6)));
    }
}