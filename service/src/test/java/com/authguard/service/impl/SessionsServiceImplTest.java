package com.authguard.service.impl;

import com.authguard.dal.SessionsRepository;
import com.authguard.dal.model.SessionDO;
import com.authguard.emb.MessageBus;
import com.authguard.service.SessionsService;
import com.authguard.service.config.SessionsConfig;
import com.authguard.service.mappers.ServiceMapper;
import com.authguard.service.mappers.ServiceMapperImpl;
import com.authguard.service.model.SessionBO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;

class SessionsServiceImplTest {
    private SessionsRepository repository;
    private MessageBus emb;
    private ServiceMapper serviceMapper;
    private SessionsService service;

    @BeforeEach
    void setup() {
        repository = Mockito.mock(SessionsRepository.class);
        emb = Mockito.mock(MessageBus.class);
        serviceMapper = new ServiceMapperImpl();

        service = new SessionsServiceImpl(repository, emb, serviceMapper, SessionsConfig.builder()
                .randomSize(20)
                .build());
    }

    @Test
    void create() {
        final SessionBO sessionBO = SessionBO.builder()
                .accountId("account")
                .data(Collections.singletonMap("key", "value"))
                .build();

        Mockito.when(repository.save(Mockito.any()))
                .thenAnswer(invocation -> CompletableFuture.completedFuture(invocation.getArgument(0, SessionDO.class)));

        final SessionBO created = service.create(sessionBO);

        assertThat(created).isEqualToIgnoringGivenFields(sessionBO, "id", "sessionToken");
        assertThat(created.getId()).isNotNull();

        Mockito.verify(repository).save(Mockito.any());
        Mockito.verify(emb).publish(Mockito.eq("sessions"), Mockito.any());
    }

    @Test
    void getById() {
        final SessionDO sessionDO = SessionDO.builder()
                .id("session-id")
                .accountId("account")
                .data(Collections.singletonMap("key", "value"))
                .build();

        Mockito.when(repository.getById(sessionDO.getId()))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(sessionDO)));

        final SessionBO expected = serviceMapper.toBO(sessionDO);
        final Optional<SessionBO> actual = service.getById(sessionDO.getId());

        assertThat(actual).contains(expected);
    }

    @Test
    void getByToken() {
        final SessionDO sessionDO = SessionDO.builder()
                .id("session-id")
                .accountId("account")
                .sessionToken("token")
                .data(Collections.singletonMap("key", "value"))
                .build();

        Mockito.when(repository.getByToken(sessionDO.getSessionToken()))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(sessionDO)));

        final SessionBO expected = serviceMapper.toBO(sessionDO);
        final Optional<SessionBO> actual = service.getByToken(sessionDO.getSessionToken());

        assertThat(actual).contains(expected);
    }
}