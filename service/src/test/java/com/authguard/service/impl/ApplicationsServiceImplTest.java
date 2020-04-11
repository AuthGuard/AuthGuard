package com.authguard.service.impl;

import com.authguard.dal.ApplicationsRepository;
import com.authguard.service.AccountsService;
import com.authguard.service.ApplicationsService;
import com.authguard.dal.model.AppDO;
import com.authguard.service.mappers.ServiceMapperImpl;
import com.authguard.service.model.AccountBO;
import com.authguard.service.model.AppBO;
import com.authguard.service.model.PermissionBO;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mockito;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ApplicationsServiceImplTest {
    private final EasyRandom random = new EasyRandom(new EasyRandomParameters()
            .collectionSizeRange(1, 4));

    private ApplicationsRepository applicationsRepository;
    private ApplicationsService applicationsService;
    private AccountsService accountsService;

    @BeforeAll
    void setup() {
        applicationsRepository = Mockito.mock(ApplicationsRepository.class);
        accountsService = Mockito.mock(AccountsService.class);

        applicationsService = new ApplicationsServiceImpl(applicationsRepository, accountsService,
                new ServiceMapperImpl());
    }

    @Test
    void create() {
        final AppBO app = random.nextObject(AppBO.class);

        Mockito.when(accountsService.getById(app.getParentAccountId()))
                .thenReturn(Optional.of(random.nextObject(AccountBO.class)));

        Mockito.when(applicationsRepository.save(any()))
                .thenAnswer(invocation -> CompletableFuture.completedFuture(invocation.getArgument(0, AppDO.class)));

        final AppBO created = applicationsService.create(app);

        assertThat(created).isEqualToIgnoringGivenFields(app, "id");
    }

    @Test
    void getById() {
        final AppDO app = random.nextObject(AppDO.class);

        app.setDeleted(false);

        Mockito.when(applicationsRepository.getById(any())).thenReturn(CompletableFuture.completedFuture(Optional.of(app)));

        final Optional<AppBO> retrieved = applicationsService.getById("");

        assertThat(retrieved).isPresent();
        assertThat(retrieved.get()).isEqualToIgnoringGivenFields(app, "permissions");
        assertThat(retrieved.get().getPermissions()).containsExactly(app.getPermissions().stream()
                .map(permissionDO -> PermissionBO.builder()
                        .group(permissionDO.getGroup())
                        .name(permissionDO.getName())
                        .build()
                ).toArray(PermissionBO[]::new));
    }
}