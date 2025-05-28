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

import java.time.Duration;
import java.util.Optional;
import io.smallrye.mutiny.Uni;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;

class SessionProviderTest {
    private ConfigContext sessionsConfig() {
        ObjectNode configNode = new ObjectNode(JsonNodeFactory.instance);

        configNode.put("randomSize", 128)
                .put("lifeTime", "20m");

        return new JacksonConfigContext(configNode);
    }

    @Test
    void generateForAccount() {
        SessionsService sessionsService = Mockito.mock(SessionsService.class);

        Mockito.when(sessionsService.create(any()))
                .thenAnswer(invocation -> Uni.createFrom().item(invocation.getArgument(0, SessionBO.class).withSessionToken("token")));

        SessionProvider sessionProvider = new SessionProvider(sessionsService, sessionsConfig());

        AccountBO account = AccountBO.builder()
                .id(101)
                .build();

        AuthResponseBO expected = AuthResponseBO.builder()
                .type("session_token")
                .entityType(EntityType.ACCOUNT)
                .entityId(account.getId())
                .validFor(Duration.ofMinutes(20).getSeconds())
                .build();

        AuthResponseBO actual = sessionProvider.generateToken(account).subscribeAsCompletionStage().join();

        assertThat(actual).isEqualToIgnoringGivenFields(expected, "token");
        assertThat(actual.getToken()).isNotNull().isInstanceOf(String.class);
    }

    @Test
    void generateForInactiveAccount() {
        SessionsService sessionsService = Mockito.mock(SessionsService.class);

        Mockito.when(sessionsService.create(any()))
                .thenAnswer(invocation -> invocation.getArgument(0, SessionBO.class).withSessionToken("token"));

        SessionProvider sessionProvider = new SessionProvider(sessionsService, sessionsConfig());

        AccountBO account = AccountBO.builder()
                .id(101)
                .active(false)
                .build();

        assertThatThrownBy(() -> sessionProvider.generateToken(account)).isInstanceOf(ServiceAuthorizationException.class);
    }

    @Test
    void delete() {
        SessionsService sessionsService = Mockito.mock(SessionsService.class);
        SessionProvider sessionProvider = new SessionProvider(sessionsService, sessionsConfig());

        String sessionToken = "session-token";
        long accountId = 101;

        Mockito.when(sessionsService.deleteByToken(sessionToken))
                .thenReturn(Uni.createFrom().item(Optional.of(SessionBO.builder()
                        .accountId(accountId)
                        .sessionToken(sessionToken)
                        .build())));

        AuthResponseBO expected = AuthResponseBO.builder()
                .type("session_token")
                .entityType(EntityType.ACCOUNT)
                .entityId(accountId)
                .token(sessionToken)
                .build();

        AuthResponseBO actual = sessionProvider.delete(AuthRequestBO.builder()
                .token(sessionToken)
                .build()).subscribeAsCompletionStage().join();

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void deleteInvalidToken() {
        SessionsService sessionsService = Mockito.mock(SessionsService.class);
        SessionProvider sessionProvider = new SessionProvider(sessionsService, sessionsConfig());

        String sessionToken = "session-token";

        Mockito.when(sessionsService.deleteByToken(sessionToken))
                .thenReturn(Uni.createFrom().item(Optional.empty()));

        assertThatThrownBy(() -> sessionProvider.delete(AuthRequestBO.builder()
                        .token(sessionToken)
                        .build()).subscribeAsCompletionStage().join())
                .hasCauseInstanceOf(ServiceAuthorizationException.class);
    }
}