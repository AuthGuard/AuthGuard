package org.auther.api.injectors;

import com.google.inject.AbstractModule;
import org.auther.dal.*;
import org.auther.dal.model.*;

import java.util.List;
import java.util.Optional;

public class DalBinder extends AbstractModule {

    @Override
    public void configure() {
        bind(CredentialsRepository.class).to(CredentialsRepositoryImpl.class);
        bind(CredentialsAuditRepository.class).to(CredentialsAuditRepositoryImpl.class);
        bind(AccountsRepository.class).to(AccountsRepositoryImpl.class);
        bind(PermissionsRepository.class).to(PermissionsRepositoryImpl.class);
        bind(RolesRepository.class).to(RolesRepositoryImpl.class);
    }


    // mocks until the actual implementations are ready
    static class CredentialsRepositoryImpl implements CredentialsRepository {

        @Override
        public CredentialsDO save(final CredentialsDO credentials) {
            return null;
        }

        @Override
        public Optional<CredentialsDO> getById(final String id) {
            return Optional.empty();
        }

        @Override
        public Optional<CredentialsDO> findByUsername(final String username) {
            return Optional.empty();
        }

        @Override
        public Optional<CredentialsDO> update(final CredentialsDO credentials) {
            return Optional.empty();
        }

        @Override
        public Optional<CredentialsDO> delete(final String id) {
            return Optional.empty();
        }
    }

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

    static class AccountsRepositoryImpl implements AccountsRepository {
        @Override
        public AccountDO save(final AccountDO account) {
            return null;
        }

        @Override
        public Optional<AccountDO> getById(final String accountId) {
            return Optional.empty();
        }

        @Override
        public Optional<AccountDO> findByUsername(final String username) {
            return Optional.empty();
        }

        @Override
        public Optional<AccountDO> update(final AccountDO account) {
            return Optional.empty();
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
