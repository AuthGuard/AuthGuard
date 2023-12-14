package com.nexblocks.authguard.service.impl;

import com.nexblocks.authguard.dal.persistence.ApplicationsRepository;
import com.nexblocks.authguard.emb.MessageBus;
import com.nexblocks.authguard.service.AccountsService;
import com.nexblocks.authguard.service.ApplicationsService;
import com.nexblocks.authguard.dal.model.AppDO;
import com.nexblocks.authguard.service.IdempotencyService;
import com.nexblocks.authguard.service.mappers.ServiceMapper;
import com.nexblocks.authguard.service.mappers.ServiceMapperImpl;
import com.nexblocks.authguard.service.model.AccountBO;
import com.nexblocks.authguard.service.model.AppBO;
import com.nexblocks.authguard.service.model.PermissionBO;
import com.nexblocks.authguard.service.model.RequestContextBO;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

class ApplicationsServiceImplTest {
    private final EasyRandom random = new EasyRandom(new EasyRandomParameters()
            .collectionSizeRange(1, 4));

    private ApplicationsRepository applicationsRepository;
    private ApplicationsService applicationsService;
    private AccountsService accountsService;
    private IdempotencyService idempotencyService;
    private MessageBus messageBus;
    private ServiceMapper serviceMapper;

    @BeforeEach
    void setup() {
        applicationsRepository = Mockito.mock(ApplicationsRepository.class);
        accountsService = Mockito.mock(AccountsService.class);
        idempotencyService = Mockito.mock(IdempotencyService.class);
        messageBus = Mockito.mock(MessageBus.class);

        serviceMapper = new ServiceMapperImpl();

        applicationsService = new ApplicationsServiceImpl(applicationsRepository, accountsService,
                idempotencyService, serviceMapper, messageBus);
    }

    @Test
    void create() {
        final AppBO app = random.nextObject(AppBO.class);

        final String idempotentKey = "idempotent-key";
        final RequestContextBO requestContext = RequestContextBO.builder()
                .idempotentKey(idempotentKey)
                .build();

        Mockito.when(accountsService.getById(app.getParentAccountId()))
                .thenReturn(Optional.of(random.nextObject(AccountBO.class)));

        Mockito.when(applicationsRepository.save(any()))
                .thenAnswer(invocation -> CompletableFuture.completedFuture(invocation.getArgument(0, AppDO.class)));

        Mockito.when(idempotencyService.performOperation(Mockito.any(), Mockito.eq(idempotentKey), Mockito.eq(app.getEntityType())))
                .thenAnswer(invocation -> {
                    return CompletableFuture.completedFuture(invocation.getArgument(0, Supplier.class).get());
                });

        final AppBO created = applicationsService.create(app, requestContext);
        final List<PermissionBO> expectedPermissions = app.getPermissions().stream()
                .map(permission -> permission.withEntityType(null))
                .collect(Collectors.toList());

        assertThat(created).isEqualToIgnoringGivenFields(app.withPermissions(expectedPermissions),
                "id", "createdAt", "lastModified", "entityType");

        Mockito.verify(messageBus, Mockito.times(1))
                .publish(eq("apps"), any());
    }

    @Test
    void getById() {
        final AppBO app = random.nextObject(AppBO.class)
                .withDeleted(false);

        Mockito.when(applicationsRepository.getById(Mockito.anyLong()))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(serviceMapper.toDO(app))));

        final Optional<AppBO> retrieved = applicationsService.getById(1);
        final List<PermissionBO> expectedPermissions = app.getPermissions().stream()
                .map(permission -> permission.withEntityType(null))
                .collect(Collectors.toList());

        assertThat(retrieved).isPresent();
        assertThat(retrieved.get()).isEqualToIgnoringGivenFields(app.withPermissions(expectedPermissions),
                "permissions", "entityType");
    }

    @Test
    void delete() {
        final AppDO app = random.nextObject(AppDO.class);

        app.setDeleted(false);

        Mockito.when(applicationsRepository.delete(app.getId()))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(app)));

        applicationsService.delete(app.getId());

        Mockito.verify(applicationsRepository).delete(app.getId());
    }

    @Test
    void activate() {
        final AppDO app = random.nextObject(AppDO.class);

        app.setActive(false);

        Mockito.when(applicationsRepository.getById(app.getId()))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(app)));
        Mockito.when(applicationsRepository.update(any()))
                .thenAnswer(invocation -> CompletableFuture.completedFuture(Optional.of(invocation.getArgument(0, AppDO.class))));

        final AppBO updated = applicationsService.activate(app.getId()).orElse(null);

        assertThat(updated).isNotNull();
        assertThat(updated.isActive()).isTrue();
    }

    @Test
    void deactivate() {
        final AppDO app = random.nextObject(AppDO.class);

        app.setActive(true);

        Mockito.when(applicationsRepository.getById(app.getId()))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(app)));
        Mockito.when(applicationsRepository.update(any()))
                .thenAnswer(invocation -> CompletableFuture.completedFuture(Optional.of(invocation.getArgument(0, AppDO.class))));

        final AppBO updated = applicationsService.deactivate(app.getId()).orElse(null);

        assertThat(updated).isNotNull();
        assertThat(updated.isActive()).isFalse();
    }
}