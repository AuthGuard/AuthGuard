package com.nexblocks.authguard.service.impl;

import com.google.inject.Inject;
import com.nexblocks.authguard.dal.model.AppDO;
import com.nexblocks.authguard.dal.model.PermissionDO;
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
    public Uni<AppBO> create(final AppBO app, final RequestContextBO requestContext) {
        return idempotencyService.performOperationAsync(() -> doCreate(app),
                requestContext.getIdempotentKey(), app.getEntityType());
    }

    private Uni<AppBO> doCreate(final AppBO app) {
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
            return combined
                    .flatMap(ignored -> accountsService.getById(app.getParentAccountId(), app.getDomain()))
                    .flatMap(opt -> {
                        if (opt.isEmpty()) {
                            throw new ServiceNotFoundException(ErrorCode.ACCOUNT_DOES_NOT_EXIST, "No account with ID " + app.getParentAccountId() + " exists");
                        }

                        return persistenceService.create(app);
                    });
        }

        return combined
                .flatMap(ignored -> persistenceService.create(app));
    }

    @Override
    public Uni<Optional<AppBO>> getById(final long id, String domain) {
        return persistenceService.getById(id)
                .map(opt -> opt.filter(app -> Objects.equals(app.getDomain(), domain)));
    }

    @Override
    public Uni<Optional<AppBO>> getByExternalId(final long externalId, String domain) {
        return applicationsRepository.getById(externalId)
                .map(optional -> optional
                        .filter(app -> Objects.equals(app.getDomain(), domain))
                        .map(serviceMapper::toBO));
    }

    @Override
    public Uni<Optional<AppBO>> update(final AppBO app, final String domain) {
        LOG.info("Application update request. accountId={}", app.getId());

        // FIXME accountId cannot be updated
        return getById(app.getId(), domain).flatMap(ignored -> persistenceService.update(app));
    }

    @Override
    public Uni<Optional<AppBO>> delete(final long id, final String domain) {
        LOG.info("Application delete request. accountId={}", id);

        return persistenceService.delete(id);
    }

    @Override
    public Uni<AppBO> activate(final long id, final String domain) {
        return getById(id, domain)
                .flatMap(AsyncUtils::uniFromAppOptional)
                .flatMap(app -> {
                    LOG.info("Activate application request. appId={}, domain={}", app.getId(), app.getDomain());

                    AppBO activated = app.withActive(true);
                    return update(activated, domain)
                            .map(persisted -> {
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
    public Uni<AppBO> deactivate(final long id, final String domain) {
        return getById(id, domain)
                .flatMap(AsyncUtils::uniFromAppOptional)
                .flatMap(app -> {
                    LOG.info("Activate application request. appId={}, domain={}", app.getId(), app.getDomain());

                    AppBO deactivated = app.withActive(false);
                    return update(deactivated, domain)
                            .map(persisted -> {
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
    public Uni<List<AppBO>> getByAccountId(final long accountId, final String domain,
                                                         final Long cursor) {
        return applicationsRepository.getAllForAccount(accountId, LongPage.of(cursor, 20))
                .map(list -> list.stream().map(serviceMapper::toBO).collect(Collectors.toList()));
    }

    @Override
    public Uni<AppBO> grantPermissions(long id, List<PermissionBO> permissions, String domain) {
        return getById(id, domain)
                .flatMap(AsyncUtils::uniFromAppOptional)
                .flatMap(app -> verifyPermissionsOrFail(permissions, app.getDomain())
                        .map(ignored -> app))
                .flatMap(app -> {
                    List<PermissionDO> permissionsToAdd = permissions.stream()
                            .map(serviceMapper::toDO)
                            .toList();

                    return applicationsRepository.addAppPermissions(serviceMapper.toDO(app), permissionsToAdd)
                            .map(updated -> {
                                LOG.info("Granted app permissions. accountId={}, domain={}, permissions={}",
                                        updated.getId(), updated.getDomain(), permissions);

                                return serviceMapper.toBO(updated);
                            });
                });
    }

    @Override
    public Uni<AppBO> revokePermissions(long id, List<PermissionBO> permissions, String domain) {
        return getById(id, domain)
                .flatMap(AsyncUtils::uniFromAppOptional)
                .flatMap(app -> {
                    Set<String> permissionsFullNames = permissions.stream()
                            .map(Permission::getFullName)
                            .collect(Collectors.toSet());

                    List<PermissionDO> permissionsToRemove = app.getPermissions().stream()
                            .filter(permission -> permissionsFullNames.contains(permission.getFullName()))
                            .map(serviceMapper::toDO)
                            .toList();

                    return applicationsRepository.removeAppPermissions(serviceMapper.toDO(app), permissionsToRemove)
                            .map(updated -> {
                                LOG.info("Revoked app permissions. accountId={}, domain={}, permissions={}",
                                        updated.getId(), updated.getDomain(), permissionsFullNames);

                                return serviceMapper.toBO(updated);
                            });
                });
    }

    @Override
    public Uni<Optional<AppBO>> grantRoles(long id, List<String> roles, String domain) {
        return getById(id, domain)
                .flatMap(AsyncUtils::uniFromAppOptional)
                .flatMap(app -> verifyRolesOrFail(roles, app.getDomain())
                        .map(ignored -> app))
                .flatMap(app -> {
                    LOG.info("Grant app roles request. accountId={}, domain={}, permissions={}",
                            app.getId(), app.getDomain(), roles);

                    List<String> combinedRoles = Stream.concat(app.getRoles().stream(), roles.stream())
                            .distinct()
                            .collect(Collectors.toList());

                    AppBO withNewRoles = app.withRoles(combinedRoles);

                    return applicationsRepository.update(serviceMapper.toDO(withNewRoles))
                            .map(updated -> updated.map(appDO -> {
                                LOG.info("Granted app roles request. accountId={}, domain={}, permissions={}",
                                        app.getId(), app.getDomain(), roles);

                                return serviceMapper.toBO(appDO);
                            }));
                });
    }

    @Override
    public Uni<Optional<AppBO>> revokeRoles(long id, List<String> roles, String domain) {
        return getById(id, domain)
                .flatMap(AsyncUtils::uniFromAppOptional)
                .flatMap(app -> {
                    LOG.info("Revoke app roles request. accountId={}, domain={}, permissions={}",
                            app.getId(), app.getDomain(), roles);

                    List<String> filteredRoles = app.getRoles().stream()
                            .filter(role -> !roles.contains(role))
                            .collect(Collectors.toList());

                    AppBO withNewRoles = app.withRoles(filteredRoles);

                    return applicationsRepository.update(serviceMapper.toDO(withNewRoles))
                            .map(updated -> updated.map(appDO -> {
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
