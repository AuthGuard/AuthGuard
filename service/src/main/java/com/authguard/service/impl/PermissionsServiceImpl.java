package com.authguard.service.impl;

import com.authguard.service.impl.mappers.ServiceMapper;
import com.google.inject.Inject;
import com.authguard.dal.PermissionsRepository;
import com.authguard.service.PermissionsService;
import com.authguard.service.model.PermissionBO;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class PermissionsServiceImpl implements PermissionsService {
    private final PermissionsRepository permissionsRepository;
    private final ServiceMapper serviceMapper;

    @Inject
    public PermissionsServiceImpl(final PermissionsRepository permissionsRepository, final ServiceMapper serviceMapper) {
        this.permissionsRepository = permissionsRepository;
        this.serviceMapper = serviceMapper;
    }

    @Override
    public PermissionBO create(final PermissionBO permission) {
        return Optional.of(permission)
                .map(serviceMapper::toDO)
                .map(permissionsRepository::save)
                .map(serviceMapper::toBO)
                .orElseThrow(IllegalStateException::new);
    }

    @Override
    public List<PermissionBO> validate(final List<PermissionBO> permissions) {
        return permissions.stream()
                .filter(permission -> permissionsRepository.search(permission.getGroup(), permission.getName()).isPresent())
                .collect(Collectors.toList());
    }

    @Override
    public List<PermissionBO> getAll() {
        return permissionsRepository.getAll().stream()
                .map(serviceMapper::toBO)
                .collect(Collectors.toList());
    }

    @Override
    public Collection<PermissionBO> getAllForGroup(final String group) {
        return permissionsRepository.getAllForGroup(group)
                .stream()
                .map(serviceMapper::toBO)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<PermissionBO> delete(final String id) {
        return permissionsRepository.delete(id)
                .map(serviceMapper::toBO);
    }
}
