package com.nexblocks.authguard.service.impl;

import com.google.inject.Inject;
import com.nexblocks.authguard.dal.model.AppDO;
import com.nexblocks.authguard.dal.persistence.ApplicationsRepository;
import com.nexblocks.authguard.dal.persistence.LongPage;
import com.nexblocks.authguard.emb.MessageBus;
import com.nexblocks.authguard.service.*;
import com.nexblocks.authguard.service.exceptions.ServiceException;
import com.nexblocks.authguard.service.exceptions.ServiceNotFoundException;
import com.nexblocks.authguard.service.exceptions.codes.ErrorCode;
import com.nexblocks.authguard.service.mappers.ServiceMapper;
import com.nexblocks.authguard.service.model.*;
import com.nexblocks.authguard.service.util.AsyncUtils;
import io.smallrye.mutiny.Uni;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ApplicationsServiceImpl implements ApplicationsService {
    private static final Logger LOG = LoggerFactory.getLogger(ApplicationsServiceImpl.class);

    private static final String APPS_CHANNEL = "apps";

    private final ApplicationsRepository applicationsRepository;
    private final AccountsService accountsService;
    private final IdempotencyService idempotencyService;
    private final PermissionsService permissionsService;
    private final RolesService rolesService;
    private final ServiceMapper serviceMapper;
    private final PersistenceService<AppBO, AppDO, ApplicationsRepository> persistenceService;

    @Inject
    public ApplicationsServiceImpl(final ApplicationsRepository applicationsRepository,
                                   final AccountsService accountsService,
                                   final IdempotencyService idempotencyService,
                                   final PermissionsService permissionsService,
                                   final RolesService rolesService,
                                   final ServiceMapper serviceMapper,
                                   final MessageBus messageBus) {
        this.applicationsRepository = applicationsRepository;
        this.accountsService = accountsService;
        this.idempotencyService = idempotencyService;
        this.permissionsService = permissionsService;
        this.rolesService = rolesService;
        this.serviceMapper = serviceMapper;

        this.persistenceService = new PersistenceService<>(applicationsRepository, messageBus,
                serviceMapper::toDO, serviceMapper::toBO, APPS_CHANNEL);
    }

    @Override
    public CompletableFuture<AppBO> create(final AppBO app, final RequestContextBO requestContext) {
        return idempotencyService.performOperationAsync(() -> doCreate(app),
                requestContext.getIdempotentKey(), app.getEntityType());
    }

    private CompletableFuture<AppBO> doCreate(final AppBO app) {
        Uni<Void> roleValidationUni = verifyRolesOrFail(app.getRoles(), app.getDomain());
        Uni<Void> permissionsValidationUni = verifyPermissionsOrFail(app.getPermissions(), app.getDomain());

        Uni<Object> combined = Uni.combine().all().unis(roleValidationUni, permissionsValidationUni)
                .with(ignored -> null);

        /*
         * It's undecided whether an app should be under an
         * account or not. So for now, we only check that the
         * account exists if it's set.
         */
        if (app.getParentAccountId() != null) {
            return combined.subscribeAsCompletionStage()
                    .thenCompose(ignored -> accountsService.getById(app.getParentAccountId(), app.getDomain()))
                    .thenCompose(opt -> {
                        if (opt.isEmpty()) {
                            throw new ServiceNotFoundException(ErrorCode.ACCOUNT_DOES_NOT_EXIST, "No account with ID " + app.getParentAccountId() + " exists");
                        }

                        return persistenceService.create(app);
                    });
        }

        return combined.subscribeAsCompletionStage()
                .thenCompose(ignored -> persistenceService.create(app));
    }

    @Override
    public CompletableFuture<Optional<AppBO>> getById(final long id, String domain) {
        return persistenceService.getById(id)
                .thenApply(opt -> opt.filter(app -> Objects.equals(app.getDomain(), domain)));
    }

    @Override
    public CompletableFuture<Optional<AppBO>> getByExternalId(final long externalId, String domain) {
        return applicationsRepository.getById(externalId).subscribe().asCompletionStage()
                .thenApply(optional -> optional
                        .filter(app -> Objects.equals(app.getDomain(), domain))
                        .map(serviceMapper::toBO));
    }

    @Override
    public CompletableFuture<Optional<AppBO>> update(final AppBO app, final String domain) {
        LOG.info("Application update request. accountId={}", app.getId());

        // FIXME accountId cannot be updated
        return getById(app.getId(), domain).thenCompose(ignored -> persistenceService.update(app));
    }

    @Override
    public CompletableFuture<Optional<AppBO>> delete(final long id, final String domain) {
        LOG.info("Application delete request. accountId={}", id);

        return persistenceService.delete(id);
    }

    @Override
    public CompletableFuture<AppBO> activate(final long id, final String domain) {
        return getById(id, domain)
                .thenCompose(AsyncUtils::fromAppOptional)
                .thenCompose(app -> {
                    LOG.info("Activate application request. appId={}, domain={}", app.getId(), app.getDomain());

                    AppBO activated = app.withActive(true);
                    return update(activated, domain)
                            .thenApply(persisted -> {
                                if (persisted.isPresent()) {
                                    LOG.info("Application activated. appId={}, domain={}", app.getId(), app.getDomain());
                                    return persisted.get();
                                }

                                LOG.info("Failed to activate application. appId={}, domain={}", app.getId(), app.getDomain());
                                throw new ServiceNotFoundException(ErrorCode.APP_DOES_NOT_EXIST, "Application does not exist");
                            });
                });
    }

    @Override
    public CompletableFuture<AppBO> deactivate(final long id, final String domain) {
        return getById(id, domain)
                .thenCompose(AsyncUtils::fromAppOptional)
                .thenCompose(app -> {
                    LOG.info("Activate application request. appId={}, domain={}", app.getId(), app.getDomain());

                    AppBO deactivated = app.withActive(false);
                    return update(deactivated, domain)
                            .thenApply(persisted -> {
                                if (persisted.isPresent()) {
                                    LOG.info("Application deactivated. appId={}, domain={}", app.getId(), app.getDomain());
                                    return persisted.get();
                                }

                                LOG.info("Failed to deactivate application. appId={}, domain={}", app.getId(), app.getDomain());
                                throw new ServiceNotFoundException(ErrorCode.APP_DOES_NOT_EXIST, "Application does not exist");
                            });
                });
    }

    @Override
    public CompletableFuture<List<AppBO>> getByAccountId(final long accountId, final String domain,
                                                         final Long cursor) {
        return applicationsRepository.getAllForAccount(accountId, LongPage.of(cursor, 20))
                .thenApply(list -> list.stream().map(serviceMapper::toBO).collect(Collectors.toList()));
    }

    @Override
    public CompletableFuture<Optional<AppBO>> grantPermissions(long id, List<PermissionBO> permissions, String domain) {
        return getById(id, domain)
                .thenCompose(AsyncUtils::fromAppOptional)
                .thenCompose(app -> verifyPermissionsOrFail(permissions, domain)
                        .subscribeAsCompletionStage()
                        .thenApply(ignored -> app))
                .thenCompose(app -> {
                    List<PermissionBO> combinedPermissions = Stream.concat(app.getPermissions().stream(), permissions.stream())
                            .distinct()
                            .collect(Collectors.toList());

                    AppBO withNewPermissions = app.withPermissions(combinedPermissions);

                    return applicationsRepository.update(serviceMapper.toDO(withNewPermissions)).subscribe().asCompletionStage()
                            .thenApply(updated -> updated.map(appDO -> {
                                LOG.info("Granted app permissions. accountId={}, domain={}, permissions={}",
                                        app.getId(), app.getDomain(), permissions);

                                return serviceMapper.toBO(appDO);
                            }));
                });
    }

    @Override
    public CompletableFuture<Optional<AppBO>> revokePermissions(long id, List<PermissionBO> permissions, String domain) {
        return getById(id, domain)
                .thenCompose(AsyncUtils::fromAppOptional)
                .thenCompose(app -> {
                    Set<String> permissionsFullNames = permissions.stream()
                            .map(Permission::getFullName)
                            .collect(Collectors.toSet());

                    LOG.info("Revoke app permissions request. accountId={}, domain={}, permissions={}",
                            app.getId(), app.getDomain(), permissionsFullNames);

                    List<PermissionBO> filteredPermissions = app.getPermissions().stream()
                            .filter(permission -> !permissionsFullNames.contains(permission.getFullName()))
                            .collect(Collectors.toList());

                    AppBO withNewPermissions = app.withPermissions(filteredPermissions);

                    return applicationsRepository.update(serviceMapper.toDO(withNewPermissions)).subscribe().asCompletionStage()
                            .thenApply(updated -> updated.map(appDO -> {
                                LOG.info("Revoked app permissions. accountId={}, domain={}, permissions={}",
                                        app.getId(), app.getDomain(), permissionsFullNames);

                                return serviceMapper.toBO(appDO);
                            }));
                });
    }

    @Override
    public CompletableFuture<Optional<AppBO>> grantRoles(long id, List<String> roles, String domain) {
        return getById(id, domain)
                .thenCompose(AsyncUtils::fromAppOptional)
                .thenCompose(app -> verifyRolesOrFail(roles, app.getDomain())
                        .subscribeAsCompletionStage()
                        .thenApply(ignored -> app))
                .thenCompose(app -> {
                    LOG.info("Grant app roles request. accountId={}, domain={}, permissions={}",
                            app.getId(), app.getDomain(), roles);

                    List<String> combinedRoles = Stream.concat(app.getRoles().stream(), roles.stream())
                            .distinct()
                            .collect(Collectors.toList());

                    AppBO withNewRoles = app.withRoles(combinedRoles);

                    return applicationsRepository.update(serviceMapper.toDO(withNewRoles)).subscribe().asCompletionStage()
                            .thenApply(updated -> updated.map(appDO -> {
                                LOG.info("Granted app roles request. accountId={}, domain={}, permissions={}",
                                        app.getId(), app.getDomain(), roles);

                                return serviceMapper.toBO(appDO);
                            }));
                });
    }

    @Override
    public CompletableFuture<Optional<AppBO>> revokeRoles(long id, List<String> roles, String domain) {
        return getById(id, domain)
                .thenCompose(AsyncUtils::fromAppOptional)
                .thenCompose(app -> {
                    LOG.info("Revoke app roles request. accountId={}, domain={}, permissions={}",
                            app.getId(), app.getDomain(), roles);

                    List<String> filteredRoles = app.getRoles().stream()
                            .filter(role -> !roles.contains(role))
                            .collect(Collectors.toList());

                    AppBO withNewRoles = app.withRoles(filteredRoles);

                    return applicationsRepository.update(serviceMapper.toDO(withNewRoles)).subscribe().asCompletionStage()
                            .thenApply(updated -> updated.map(appDO -> {
                                LOG.info("Revoked app roles. accountId={}, domain={}, permissions={}",
                                        app.getId(), app.getDomain(), roles);

                                return serviceMapper.toBO(appDO);
                            }));
                });
    }


    private Uni<Void> verifyRolesOrFail(final Collection<String> roles, final String domain) {
        return rolesService.verifyRoles(roles, domain, EntityType.APPLICATION)
                .flatMap(verifiedRoles-> {
                    if (verifiedRoles.size() != roles.size()) {
                        List<String> difference = roles.stream()
                                .filter(role -> !verifiedRoles.contains(role))
                                .toList();

                        return Uni.createFrom().failure(new ServiceException(ErrorCode.ROLE_DOES_NOT_EXIST,
                                "The following roles are not valid " + difference));
                    }

                    return Uni.createFrom().voidItem();
                });
    }

    private Uni<Void> verifyPermissionsOrFail(final Collection<PermissionBO> permissions, final String domain) {
        return permissionsService.validate(permissions, domain, EntityType.APPLICATION)
                .flatMap(verifiedPermissions -> {
                    if (verifiedPermissions.size() != permissions.size()) {
                        Set<String> verifiedPermissionNames = verifiedPermissions.stream()
                                .map(Permission::getFullName)
                                .collect(Collectors.toSet());
                        List<String> difference = permissions.stream()
                                .map(Permission::getFullName)
                                .filter(permission -> !verifiedPermissionNames.contains(permission))
                                .toList();

                        return Uni.createFrom().failure(new ServiceException(ErrorCode.PERMISSION_DOES_NOT_EXIST,
                                "The following permissions are not valid " + difference));
                    }

                    return Uni.createFrom().voidItem();
                });
    }
}
