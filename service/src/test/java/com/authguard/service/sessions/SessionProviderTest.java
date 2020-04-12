package com.authguard.service.sessions;

import com.authguard.config.ConfigContext;
import com.authguard.config.JacksonConfigContext;
import com.authguard.dal.SessionsRepository;
import com.authguard.dal.model.SessionDO;
import com.authguard.service.mappers.ServiceMapperImpl;
import com.authguard.service.model.AccountBO;
import com.authguard.service.model.TokensBO;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;

class SessionProviderTest {
    private ConfigContext sessionsConfig() {
        final ObjectNode configNode = new ObjectNode(JsonNodeFactory.instance);

        configNode.put("randomSize", 128)
                .put("lifeTime", "20m");

        return new JacksonConfigContext(configNode);
    }

    @Test
    void generateForAccount() {
        final SessionsRepository sessionsRepository = Mockito.mock(SessionsRepository.class);

        Mockito.when(sessionsRepository.save(any()))
                .thenAnswer(invocation -> CompletableFuture.completedFuture(invocation.getArgument(0, SessionDO.class)));

        final SessionProvider sessionProvider = new SessionProvider(sessionsRepository, sessionsConfig(), new ServiceMapperImpl());

        final AccountBO account = AccountBO.builder()
                .id("account-id")
                .build();

        final TokensBO generated = sessionProvider.generateToken(account);

        assertThat(generated.getType()).isEqualTo("session");
    }
}