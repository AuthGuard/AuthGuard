package com.nexblocks.authguard.service.impl;

import com.nexblocks.authguard.dal.cache.SessionsRepository;
import com.nexblocks.authguard.dal.model.SessionDO;
import com.nexblocks.authguard.emb.MessageBus;
import com.nexblocks.authguard.service.SessionsService;
import com.nexblocks.authguard.service.config.SessionsConfig;
import com.nexblocks.authguard.service.mappers.ServiceMapper;
import com.nexblocks.authguard.service.mappers.ServiceMapperImpl;
import com.nexblocks.authguard.service.model.SessionBO;
import io.smallrye.mutiny.Uni;
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
        SessionBO sessionBO = SessionBO.builder()
                .accountId(101)
                .data(Collections.singletonMap("key", "value"))
                .build();

        Mockito.when(repository.save(Mockito.any()))
                .thenAnswer(invocation -> Uni.createFrom().item(invocation.getArgument(0, SessionDO.class)));

        SessionBO created = service.create(sessionBO).join();

        assertThat(created).usingRecursiveComparison()
                .ignoringFields("id", "sessionToken")
                .isEqualTo(sessionBO);
        assertThat(created.getId()).isNotNull();

        Mockito.verify(repository).save(Mockito.any());
        Mockito.verify(emb).publish(Mockito.eq("sessions"), Mockito.any());
    }

    @Test
    void getById() {
        SessionDO sessionDO = SessionDO.builder()
                .id(1)
                .accountId(101)
                .data(Collections.singletonMap("key", "value"))
                .build();

        Mockito.when(repository.getById(sessionDO.getId()))
                .thenReturn(Uni.createFrom().item(Optional.of(sessionDO)));

        SessionBO expected = serviceMapper.toBO(sessionDO);
        Optional<SessionBO> actual = service.getById(sessionDO.getId()).join();

        assertThat(actual).contains(expected);
    }

    @Test
    void getByToken() {
        SessionDO sessionDO = SessionDO.builder()
                .id(1)
                .accountId(101)
                .sessionToken("token")
                .data(Collections.singletonMap("key", "value"))
                .build();

        Mockito.when(repository.getByToken(sessionDO.getSessionToken()))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(sessionDO)));

        SessionBO expected = serviceMapper.toBO(sessionDO);
        Optional<SessionBO> actual = service.getByToken(sessionDO.getSessionToken()).join();

        assertThat(actual).contains(expected);
    }

    @Test
    void deleteByToken() {
        SessionDO sessionDO = SessionDO.builder()
                .id(1)
                .accountId(101)
                .sessionToken("token")
                .data(Collections.singletonMap("key", "value"))
                .build();

        Mockito.when(repository.deleteByToken(sessionDO.getSessionToken()))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(sessionDO)));

        SessionBO expected = serviceMapper.toBO(sessionDO);
        Optional<SessionBO> actual = service.deleteByToken(sessionDO.getSessionToken()).join();

        assertThat(actual).contains(expected);
    }
}