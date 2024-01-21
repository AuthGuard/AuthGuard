package com.nexblocks.authguard.service.impl;

import com.google.inject.Inject;
import com.nexblocks.authguard.dal.model.PermissionDO;
import com.nexblocks.authguard.dal.persistence.PermissionsRepository;
import com.nexblocks.authguard.emb.MessageBus;
import com.nexblocks.authguard.service.PermissionsService;
import com.nexblocks.authguard.service.exceptions.ServiceConflictException;
import com.nexblocks.authguard.service.exceptions.codes.ErrorCode;
import com.nexblocks.authguard.service.mappers.ServiceMapper;
import com.nexblocks.authguard.service.model.PermissionBO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class PermissionsServiceImpl implements PermissionsService {
    private static final Logger LOG = LoggerFactory.getLogger(PermissionsServiceImpl.class);

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
    public CompletableFuture<PermissionBO> create(final PermissionBO permission) {
        LOG.debug("New permission request. permission={}, domain={}", permission.getFullName(), permission.getDomain());

        if (permissionsRepository.search(permission.getGroup(), permission.getName(), permission.getDomain()).join().isPresent()) {
            throw new ServiceConflictException(ErrorCode.PERMISSION_ALREADY_EXIST,
                    "Permission " + permission.getFullName() + " already exists");
        }

        return persistenceService.create(permission);
    }

    @Override
    public CompletableFuture<Optional<PermissionBO>> getById(final long id, String domain) {
        return persistenceService.getById(id);
    }

    @Override
    public CompletableFuture<Optional<PermissionBO>> update(final PermissionBO entity, String domain) {
        throw new UnsupportedOperationException("Permissions cannot be updated");
    }

    @Override
    public List<PermissionBO> validate(final List<PermissionBO> permissions, final String domain) {
        return permissions.stream()
                .map(permission -> permissionsRepository.search(permission.getGroup(), permission.getName(), domain)
                        .join()
                        .map(serviceMapper::toBO))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    @Override
    public CompletableFuture<List<PermissionBO>> getAll(final String domain) {
        return permissionsRepository.getAll(domain)
                .thenApply(list -> list.stream()
                        .map(serviceMapper::toBO)
                        .collect(Collectors.toList()));
    }

    @Override
    public CompletableFuture<List<PermissionBO>> getAllForGroup(final String group, final String domain) {
        return permissionsRepository.getAllForGroup(group, domain)
                .thenApply(list -> list.stream()
                        .map(serviceMapper::toBO)
                        .collect(Collectors.toList()));
    }

    @Override
    public CompletableFuture<Optional<PermissionBO>> delete(final long id, String domain) {
        LOG.info("Request to delete permission. permissionId={}", id);

        return persistenceService.delete(id);
    }
}
