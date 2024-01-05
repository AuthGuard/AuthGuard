package com.nexblocks.authguard.service.impl;

import com.google.inject.Inject;
import com.nexblocks.authguard.dal.model.RoleDO;
import com.nexblocks.authguard.dal.persistence.RolesRepository;
import com.nexblocks.authguard.emb.MessageBus;
import com.nexblocks.authguard.service.RolesService;
import com.nexblocks.authguard.service.exceptions.ServiceConflictException;
import com.nexblocks.authguard.service.exceptions.codes.ErrorCode;
import com.nexblocks.authguard.service.mappers.ServiceMapper;
import com.nexblocks.authguard.service.model.RoleBO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class RolesServiceImpl implements RolesService {
    private static final Logger LOG = LoggerFactory.getLogger(RolesServiceImpl.class);

    private static final String ROLES_CHANNEL = "roles";

    private final RolesRepository rolesRepository;
    private final ServiceMapper serviceMapper;
    private final PersistenceService<RoleBO, RoleDO, RolesRepository> persistenceService;

    @Inject
    public RolesServiceImpl(final RolesRepository rolesRepository,
                            final ServiceMapper serviceMapper,
                            final MessageBus messageBus) {
        this.rolesRepository = rolesRepository;
        this.serviceMapper = serviceMapper;

        this.persistenceService = new PersistenceService<>(rolesRepository, messageBus,
                serviceMapper::toDO, serviceMapper::toBO, ROLES_CHANNEL);
    }

    @Override
    public CompletableFuture<List<RoleBO>> getAll(final String domain) {
        return rolesRepository.getAll(domain)
                .thenApply(list -> list.stream()
                        .map(serviceMapper::toBO)
                        .collect(Collectors.toList()));
    }

    @Override
    public CompletableFuture<RoleBO> create(final RoleBO role) {
        LOG.debug("New role request. role={}, domain={}", role.getName(), role.getDomain());

        return getRoleByName(role.getName(), role.getDomain())
                .thenCompose(opt -> {
                    if (opt.isPresent()) {
                        LOG.info("Role already exists. role={}, domain={}", role.getName(), role.getDomain());

                        return CompletableFuture.failedFuture(new ServiceConflictException(ErrorCode.ROLE_ALREADY_EXISTS,
                                "Role " + role.getName() + " already exists"));
                    }

                    return persistenceService.create(role);
                });
    }

    @Override
    public CompletableFuture<Optional<RoleBO>> getById(final long id) {
        return persistenceService.getById(id);
    }

    @Override
    public CompletableFuture<Optional<RoleBO>> update(final RoleBO entity) {
        throw new UnsupportedOperationException("Roles cannot be updated");
    }

    @Override
    public CompletableFuture<Optional<RoleBO>> delete(final long id) {
        LOG.info("Request to delete role. roleId={}", id);

        return persistenceService.delete(id);
    }

    @Override
    public CompletableFuture<Optional<RoleBO>> getRoleByName(final String name, final String domain) {
        return rolesRepository.getByName(name, domain)
                .thenApply(optional -> optional.map(serviceMapper::toBO));
    }

    @Override
    public List<String> verifyRoles(final Collection<String> roles, final String domain) {
        return rolesRepository.getMultiple(roles, domain)
                .thenApply(found -> found.stream()
                        .map(RoleDO::getName)
                        .collect(Collectors.toList())
                ).join();
    }
}
