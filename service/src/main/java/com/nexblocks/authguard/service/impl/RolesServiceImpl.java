package com.nexblocks.authguard.service.impl;

import com.google.inject.Inject;
import com.nexblocks.authguard.dal.model.RoleDO;
import com.nexblocks.authguard.dal.persistence.LongPage;
import com.nexblocks.authguard.dal.persistence.Page;
import com.nexblocks.authguard.dal.persistence.RolesRepository;
import com.nexblocks.authguard.emb.MessageBus;
import com.nexblocks.authguard.service.RolesService;
import com.nexblocks.authguard.service.exceptions.ServiceConflictException;
import com.nexblocks.authguard.service.exceptions.ServiceNotFoundException;
import com.nexblocks.authguard.service.exceptions.codes.ErrorCode;
import com.nexblocks.authguard.service.mappers.ServiceMapper;
import com.nexblocks.authguard.service.model.EntityType;
import com.nexblocks.authguard.service.model.RoleBO;
import io.smallrye.mutiny.Uni;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import io.smallrye.mutiny.Uni;
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
    public Uni<List<RoleBO>> getAll(final String domain, final Long cursor) {
        return rolesRepository.getAll(domain, LongPage.of(cursor, 20))
                .map(list -> list.stream()
                        .map(serviceMapper::toBO)
                        .collect(Collectors.toList()));
    }

    @Override
    public Uni<RoleBO> create(final RoleBO role) {
        LOG.debug("New role request. role={}, domain={}", role.getName(), role.getDomain());

        return getRoleByName(role.getName(), role.getDomain())
                .flatMap(opt -> {
                    if (opt.isPresent()) {
                        LOG.info("Role already exists. role={}, domain={}", role.getName(), role.getDomain());

                        return Uni.createFrom().failure(new ServiceConflictException(ErrorCode.ROLE_ALREADY_EXISTS,
                                "Role " + role.getName() + " already exists"));
                    }

                    return persistenceService.create(role);
                });
    }

    @Override
    public Uni<Optional<RoleBO>> getById(final long id, final String domain) {
        return persistenceService.getById(id)
                .map(opt -> opt.filter(account -> Objects.equals(account.getDomain(), domain)));
    }

    @Override
    public Uni<Optional<RoleBO>> update(final RoleBO role, final String domain) {
        return getById(role.getId(), domain)
                .flatMap(opt -> {
                    if (opt.isEmpty()) {
                        return Uni.createFrom().item(opt);
                    }

                    RoleBO newRole = RoleBO.builder()
                            .from(opt.get())
                            .forAccounts(role.isForAccounts())
                            .forApplications(role.isForApplications())
                            .build();

                    return persistenceService.update(newRole);
                });
    }

    @Override
    public Uni<Optional<RoleBO>> delete(final long id, String domain) {
        LOG.info("Request to delete role. roleId={}", id);

        return persistenceService.delete(id);
    }

    @Override
    public Uni<Optional<RoleBO>> getRoleByName(final String name, final String domain) {
        return rolesRepository.getByName(name, domain)
                .map(optional -> optional.map(serviceMapper::toBO));
    }

    @Override
    public Uni<List<String>> verifyRoles(final Collection<String> roles, final String domain, EntityType entityType) {
        return rolesRepository.getMultiple(roles, domain)
                .map(found -> found.stream()
                        .filter(role -> {
                            switch (entityType) {
                                case ACCOUNT: return role.isForAccounts();
                                case APPLICATION: return role.isForApplications();
                                default: return false;
                            }
                        })
                        .map(RoleDO::getName)
                        .collect(Collectors.toList())
                );
    }
}
