package com.authguard.sessions;

import com.authguard.config.ConfigContext;
import com.authguard.config.JacksonConfigContext;
import com.authguard.service.SessionsService;
import com.authguard.service.model.AccountBO;
import com.authguard.service.model.SessionBO;
import com.authguard.service.model.TokensBO;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

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
        final SessionsService sessionsService = Mockito.mock(SessionsService.class);

        Mockito.when(sessionsService.create(any()))
                .thenAnswer(invocation -> invocation.getArgument(0, SessionBO.class).withSessionToken("token"));

        final SessionProvider sessionProvider = new SessionProvider(sessionsService, sessionsConfig());

        final AccountBO account = AccountBO.builder()
                .id("account-id")
                .build();

        final TokensBO generated = sessionProvider.generateToken(account);

        assertThat(generated.getType()).isEqualTo("session");
        assertThat(generated.getToken()).isNotNull().isInstanceOf(String.class);
    }
}