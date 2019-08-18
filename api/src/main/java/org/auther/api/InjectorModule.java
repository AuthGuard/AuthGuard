package org.auther.api;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.google.inject.AbstractModule;
import org.auther.api.routes.RestMapper;
import org.auther.api.routes.RestMapperImpl;
import org.auther.dal.AccountsRepository;
import org.auther.dal.PermissionsRepository;
import org.auther.dal.model.AccountDO;
import org.auther.dal.model.PermissionDO;
import org.auther.dal.model.PermissionGroupDO;
import org.auther.service.*;
import org.auther.service.impl.*;
import org.auther.service.impl.passwords.SCryptPassword;

import java.security.SecureRandom;
import java.util.List;
import java.util.Optional;

public class InjectorModule extends AbstractModule {
    // TODO read the configuration

    @Override
    public void configure() {
        // JWT stuff
        final byte[] randomSecret = new byte[256];
        new SecureRandom().nextBytes(randomSecret);

        final Algorithm jwtAlgorithm = Algorithm.HMAC256(randomSecret);

        // --- Auth0 configuration
        bind(Algorithm.class).toInstance(jwtAlgorithm);
        bind(JWTVerifier.class).toInstance(JWT.require(jwtAlgorithm).build());

        // --- Provider instances
        bind(JTIProvider.class).to(BasicJTIProvider.class);
        bind(JWTProvider.class).to(BasicJWTHandler.class);

        // DAL
        bind(AccountsRepository.class).to(AccountsRepositoryImpl.class);
        bind(PermissionsRepository.class).to(PermissionsRepositoryImpl.class);

        // Services
        bind(ServiceMapper.class).to(ServiceMapperImpl.class);
        bind(AccountsService.class).to(AccountsServiceImpl.class);
        bind(PermissionsService.class).to(PermissionsServiceImpl.class);
        bind(SecurePassword.class).to(SCryptPassword.class);

        // Mappers
        bind(RestMapper.class).to(RestMapperImpl.class);
    }

    // mocks until the actual implementations are ready
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
    }
}
