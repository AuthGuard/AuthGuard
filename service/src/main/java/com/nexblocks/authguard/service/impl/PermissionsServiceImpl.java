package com.nexblocks.authguard.service.impl;

import com.nexblocks.authguard.dal.model.PermissionDO;
import com.nexblocks.authguard.dal.persistence.PermissionsRepository;
import com.nexblocks.authguard.emb.MessageBus;
import com.nexblocks.authguard.service.PermissionsService;
import com.nexblocks.authguard.service.mappers.ServiceMapper;
import com.nexblocks.authguard.service.model.PermissionBO;
import com.google.inject.Inject;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class PermissionsServiceImpl implements PermissionsService {
    private static final String PERMISSIONS_CHANNEL = "permissions";

    private final PermissionsRepository permissionsRepository;
    private final ServiceMapper serviceMapper;
    private final PersistenceService<PermissionBO, PermissionDO, PermissionsRepository> persistenceService;

    @Inject
    public PermissionsServiceImpl(final PermissionsRepository permissionsRepository,
                                  final ServiceMapper serviceMapper,
                                  final MessageBus messageBus) {
        this.permissionsRepository = permissionsRepository;
        this.serviceMapper = serviceMapper;

        this.persistenceService = new PersistenceService<>(permissionsRepository, messageBus,
                serviceMapper::toDO, serviceMapper::toBO, PERMISSIONS_CHANNEL);
    }

    @Override
    public PermissionBO create(final PermissionBO permission) {
        return persistenceService.create(permission);
    }

    @Override
    public Optional<PermissionBO> getById(final String id) {
        return persistenceService.getById(id);
    }

    @Override
    public Optional<PermissionBO> update(final PermissionBO entity) {
        throw new UnsupportedOperationException("Permissions cannot be updated");
    }

    @Override
    public List<PermissionBO> validate(final List<PermissionBO> permissions) {
        return permissions.stream()
                .filter(permission -> permissionsRepository.search(permission.getGroup(), permission.getName()).join().isPresent())
                .collect(Collectors.toList());
    }

    @Override
    public List<PermissionBO> getAll() {
        return permissionsRepository.getAll().join()
                .stream()
                .map(serviceMapper::toBO)
                .collect(Collectors.toList());
    }

    @Override
    public Collection<PermissionBO> getAllForGroup(final String group) {
        return permissionsRepository.getAllForGroup(group)
                .join()
                .stream()
                .map(serviceMapper::toBO)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<PermissionBO> delete(final String id) {
        return persistenceService.delete(id);
    }
}
