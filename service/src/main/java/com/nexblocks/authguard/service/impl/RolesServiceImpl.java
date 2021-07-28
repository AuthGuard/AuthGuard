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

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class RolesServiceImpl implements RolesService {
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
    public List<RoleBO> getAll() {
        return rolesRepository.getAll()
                .join()
                .stream()
                .map(serviceMapper::toBO)
                .collect(Collectors.toList());
    }

    @Override
    public RoleBO create(final RoleBO role) {
        if (getRoleByName(role.getName()).isPresent()) {
            throw new ServiceConflictException(ErrorCode.ROLE_ALREADY_EXISTS,
                    "Role " + role.getName() + " already exists");
        }

        return persistenceService.create(role);
    }

    @Override
    public Optional<RoleBO> getById(final String id) {
        return persistenceService.getById(id);
    }

    @Override
    public Optional<RoleBO> update(final RoleBO entity) {
        throw new UnsupportedOperationException("Roles cannot be updated");
    }

    @Override
    public Optional<RoleBO> delete(final String id) {
        return persistenceService.delete(id);
    }

    @Override
    public Optional<RoleBO> getRoleByName(final String name) {
        return rolesRepository.getByName(name)
                .thenApply(optional -> optional.map(serviceMapper::toBO))
                .join();
    }

    @Override
    public List<String> verifyRoles(final List<String> roles) {
        return rolesRepository.getMultiple(roles)
                .thenApply(found -> found.stream()
                        .map(RoleDO::getName)
                        .collect(Collectors.toList())
                ).join();
    }
}
