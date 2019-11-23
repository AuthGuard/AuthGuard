package org.auther.injection;

import com.google.inject.AbstractModule;
import org.auther.dal.AccountsRepository;
import org.auther.dal.PermissionsRepository;
import org.auther.dal.model.AccountDO;
import org.auther.dal.model.PermissionDO;
import org.junit.jupiter.api.Test;
import org.reflections.Reflections;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;

import java.util.Collection;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ClassSearchTest {

    @Test
    void findAccountsRepositoryImplementation() throws Exception {
        final ClassSearch classSearch = new ClassSearch(new Reflections(""));
        final Implementaion<AccountsRepository> implementation = classSearch.findAccountsRepositoryImplementation();

        assertThat(implementation).isNotNull();
        assertThat(implementation.getImplementationClass()).isEqualTo(AccountsRepositoryImpl.class);
        assertThat(implementation.getInjectorModule()).isEqualTo(AccountsRepositoryInjector.class);
    }

    @Test
    void findAccountsRepositoryNoImplementation() {
        final Reflections reflections = new Reflections(new ConfigurationBuilder()
                .setUrls(ClasspathHelper.forClass(this.getClass()))
                .filterInputsBy(new FilterBuilder().exclude(".*" + AccountsRepositoryImpl.class.getSimpleName() + ".*"))
        );

        final ClassSearch classSearch = new ClassSearch(reflections);

        assertThatThrownBy(classSearch::findAccountsRepositoryImplementation).isInstanceOf(NoImplementationFoundException.class);
    }

    @Test
    void findPermissionsRepositoryImplementation() throws Exception {
        final ClassSearch classSearch = new ClassSearch(new Reflections(""));
        final Implementaion<PermissionsRepository> implementation = classSearch.findPermissionsRepositoryImplementation();

        assertThat(implementation).isNotNull();
        assertThat(implementation.getImplementationClass()).isEqualTo(PermissionsRepositoryImpl.class);
        assertThat(implementation.getInjectorModule()).isEqualTo(PermissionsRepositoryInjector.class);
    }

    @Test
    void findPermissionsRepositoryNoImplementation() {
        final Reflections reflections = new Reflections(new ConfigurationBuilder()
                .setUrls(ClasspathHelper.forClass(this.getClass()))
                .filterInputsBy(new FilterBuilder().exclude(".*" + PermissionsRepositoryImpl.class.getSimpleName() + ".*"))
        );

        final ClassSearch classSearch = new ClassSearch(reflections);

        assertThatThrownBy(classSearch::findPermissionsRepositoryImplementation).isInstanceOf(NoImplementationFoundException.class);
    }

    // Mock classes for tests

    // --- AccountsRepository
    private static class AccountsRepositoryImpl implements AccountsRepository {
        @Override
        public AccountDO save(final AccountDO account) {
            return null;
        }

        @Override
        public Optional<AccountDO> getById(final String accountId) {
            return Optional.empty();
        }

        @Override
        public Optional<AccountDO> update(final AccountDO account) {
            return Optional.empty();
        }
    }

    interface AccountsRepositoryNotImpl extends AccountsRepository {}

    @InjectorModule(target = AccountsRepository.class)
    private static class AccountsRepositoryInjector extends AbstractModule {}

    // --- PermissionsRepository
    private static class PermissionsRepositoryImpl implements PermissionsRepository {
        @Override
        public PermissionDO save(final PermissionDO permission) {
            return null;
        }

        @Override
        public Optional<PermissionDO> getById(final String id) {
            return Optional.empty();
        }

        @Override
        public Optional<PermissionDO> search(final String group, final String name) {
            return Optional.empty();
        }

        @Override
        public Optional<PermissionDO> delete(final String id) {
            return Optional.empty();
        }

        @Override
        public Collection<PermissionDO> getAll() {
            return null;
        }

        @Override
        public Collection<PermissionDO> getAllForGroup(final String group) {
            return null;
        }
    }

    interface PermissionsRepositoryNotImpl extends PermissionsRepository {}

    @InjectorModule(target = PermissionsRepository.class)
    private static class PermissionsRepositoryInjector extends AbstractModule {}
}