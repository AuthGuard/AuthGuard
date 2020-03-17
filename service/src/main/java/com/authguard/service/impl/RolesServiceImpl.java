package com.authguard.service.impl;

import com.authguard.dal.model.RoleDO;
import com.authguard.service.exceptions.ServiceNotFoundException;
import com.authguard.service.impl.mappers.ServiceMapper;
import com.google.inject.Inject;
import com.authguard.dal.RolesRepository;
import com.authguard.service.RolesService;
import com.authguard.service.model.PermissionBO;
import com.authguard.service.model.RoleBO;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RolesServiceImpl implements RolesService {
    private final RolesRepository rolesRepository;
    private final ServiceMapper serviceMapper;

    @Inject
    public RolesServiceImpl(final RolesRepository rolesRepository, final ServiceMapper serviceMapper) {
        this.rolesRepository = rolesRepository;
        this.serviceMapper = serviceMapper;
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
        final RoleDO roleDO = serviceMapper.toDO(role);

        return rolesRepository.save(roleDO)
                .thenApply(serviceMapper::toBO)
                .join();
    }

    @Override
    public Optional<RoleBO> getRoleByName(final String name) {
        return rolesRepository.getByName(name)
                .thenApply(optional -> optional.map(serviceMapper::toBO))
                .join();
    }

    @Override
    public List<PermissionBO> getPermissionsByName(final String name) {
        return getRoleByName(name)
                .map(RoleBO::getPermissions)
                .orElseThrow(ServiceNotFoundException::new);
    }

    @Override
    public Optional<RoleBO> grantPermissions(final String name, final List<PermissionBO> permission) {
        return rolesRepository.getByName(name)
                .thenApply(optional -> optional.map(serviceMapper::toBO)
                        .map(role -> role.withPermissions(
                                Stream.concat(role.getPermissions().stream(), permission.stream())
                                        .distinct().collect(Collectors.toList()))
                        ).map(serviceMapper::toDO)
                )
                .thenApply(optional -> {
                    if (optional.isPresent()) {
                        return rolesRepository.update(optional.get())
                                .thenApply(opt -> opt.map(serviceMapper::toBO))
                                .join();
                    } else {
                        return Optional.<RoleBO>empty();
                    }
                }).join();
    }

    @Override
    public Optional<RoleBO> revokePermissions(final String name, final List<PermissionBO> permissions) {
        final RoleBO role = rolesRepository.getByName(name)
                .join()
                .map(serviceMapper::toBO)
                .orElseThrow(ServiceNotFoundException::new);

        final List<PermissionBO> updatedPermissions = role.getPermissions().stream()
                .filter(permission -> !permissions.contains(permission))
                .collect(Collectors.toList());

        return rolesRepository.update(serviceMapper.toDO(role.withPermissions(updatedPermissions)))
                .thenApply(optional -> optional.map(serviceMapper::toBO))
                .join();
    }
}
