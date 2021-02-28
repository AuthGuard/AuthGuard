package com.nexblocks.authguard.sessions;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.nexblocks.authguard.config.ConfigContext;
import com.nexblocks.authguard.config.JacksonConfigContext;
import com.nexblocks.authguard.service.SessionsService;
import com.nexblocks.authguard.service.exceptions.ServiceAuthorizationException;
import com.nexblocks.authguard.service.model.*;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

    @Test
    void delete() {
        final SessionsService sessionsService = Mockito.mock(SessionsService.class);
        final SessionProvider sessionProvider = new SessionProvider(sessionsService, sessionsConfig());

        final String sessionToken = "session-token";
        final String accountId = "account";

        Mockito.when(sessionsService.deleteByToken(sessionToken))
                .thenReturn(Optional.of(SessionBO.builder()
                        .accountId(accountId)
                        .sessionToken(sessionToken)
                        .build()));

        final TokensBO expected = TokensBO.builder()
                .type("session")
                .entityType(EntityType.ACCOUNT)
                .entityId(accountId)
                .token(sessionToken)
                .build();

        final TokensBO actual = sessionProvider.delete(AuthRequestBO.builder()
                .token(sessionToken)
                .build());

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void deleteInvalidToken() {
        final SessionsService sessionsService = Mockito.mock(SessionsService.class);
        final SessionProvider sessionProvider = new SessionProvider(sessionsService, sessionsConfig());

        final String sessionToken = "session-token";

        Mockito.when(sessionsService.deleteByToken(sessionToken))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> sessionProvider.delete(AuthRequestBO.builder()
                        .token(sessionToken)
                        .build()))
                .isInstanceOf(ServiceAuthorizationException.class);
    }
}