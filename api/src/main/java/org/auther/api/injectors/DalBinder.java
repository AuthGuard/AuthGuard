package org.auther.api.injectors;

import com.google.inject.AbstractModule;
import org.auther.dal.*;
import org.auther.dal.mock.MockAccountsRepository;
import org.auther.dal.mock.MockAccountsTokensRepository;
import org.auther.dal.mock.MockCredentialsRepository;
import org.auther.dal.model.*;

import java.util.List;
import java.util.Optional;

public class DalBinder extends AbstractModule {

    @Override
    public void configure() {
        bind(CredentialsRepository.class).to(MockCredentialsRepository.class);
        bind(CredentialsAuditRepository.class).to(CredentialsAuditRepositoryImpl.class);
        bind(AccountsRepository.class).to(MockAccountsRepository.class);
        bind(PermissionsRepository.class).to(PermissionsRepositoryImpl.class);
        bind(RolesRepository.class).to(RolesRepositoryImpl.class);
        bind(AccountTokensRepository.class).to(MockAccountsTokensRepository.class);
    }


    // mocks until the actual implementations are ready
    static class CredentialsAuditRepositoryImpl implements CredentialsAuditRepository {

        @Override
        public CredentialsAuditDO save(final CredentialsAuditDO credentialsAudit) {
            return null;
        }

        @Override
        public Optional<CredentialsAuditDO> getById(final String id) {
            return Optional.empty();
        }

        @Override
        public List<CredentialsAuditDO> findByCredentialsId(final String credentialsId) {
            return null;
        }
    }

    static class PermissionsRepositoryImpl implements PermissionsRepository {
        @Override
        public PermissionGroupDO createPermissionGroup(final PermissionGroupDO permissionGroup) {
            return null;
        }

        @Override
        public Optional<PermissionGroupDO> deletePermissionGroup() {
            return Optional.empty();
        }

        @Override
        public Optional<PermissionGroupDO> getPermissionGroupByName(final String groupName) {
            return Optional.empty();
        }

        @Override
        public PermissionDO createPermission(final PermissionDO permission) {
            return null;
        }

        @Override
        public Optional<PermissionDO> deletePermission(final PermissionDO permission) {
            return Optional.empty();
        }

        @Override
        public List<PermissionDO> getAllPermissions() {
            return null;
        }

        @Override
        public Optional<List<PermissionDO>> getPermissions(final String permissionGroup) {
            return Optional.empty();
        }

        @Override
        public Optional<PermissionDO> getPermission(final String permissionGroup, final String permissionName) {
            return Optional.empty();
        }
    }

    static class RolesRepositoryImpl implements RolesRepository {

        @Override
        public List<RoleDO> getAll() {
            return null;
        }

        @Override
        public Optional<RoleDO> getByName(final String name) {
            return Optional.empty();
        }

        @Override
        public RoleDO save(final RoleDO role) {
            return null;
        }

        @Override
        public Optional<RoleDO> delete(final String name) {
            return Optional.empty();
        }

        @Override
        public Optional<RoleDO> update(final RoleDO role) {
            return Optional.empty();
        }
    }
}
