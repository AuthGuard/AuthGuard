package org.auther.injection;

import com.google.inject.AbstractModule;
import org.auther.dal.AccountsRepository;
import org.auther.dal.PermissionsRepository;
import org.auther.dal.mock.MockAccountsRepository;
import org.auther.dal.mock.MockPermissionsRepository;
import org.auther.emb.MessagePublisher;
import org.auther.emb.model.MessageMO;
import org.junit.jupiter.api.Test;
import org.reflections.Reflections;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ClassSearchTest {

    @Test
    void findAccountsRepositoryImplementation() throws Exception {
        final ClassSearch classSearch = new ClassSearch(new Reflections("org.auther"));
        final Implementaion<AccountsRepository> implementation = classSearch.findAccountsRepositoryImplementation();

        assertThat(implementation).isNotNull();
        assertThat(implementation.getImplementationClass()).isEqualTo(MockAccountsRepository.class);
        assertThat(implementation.getInjectorModule()).isEqualTo(AccountsRepositoryInjector.class);
    }

    @Test
    void findAccountsRepositoryNoImplementation() {
        final Reflections reflections = new Reflections(new ConfigurationBuilder()
                .setUrls(ClasspathHelper.forClass(this.getClass()))
                .filterInputsBy(new FilterBuilder().exclude(".*" + MockAccountsRepository.class.getSimpleName() + ".*"))
        );

        final ClassSearch classSearch = new ClassSearch(reflections);

        assertThatThrownBy(classSearch::findAccountsRepositoryImplementation).isInstanceOf(NoImplementationFoundException.class);
    }

    @Test
    void findPermissionsRepositoryImplementation() throws Exception {
        final ClassSearch classSearch = new ClassSearch(new Reflections("org.auther"));
        final Implementaion<PermissionsRepository> implementation = classSearch.findPermissionsRepositoryImplementation();

        assertThat(implementation).isNotNull();
        assertThat(implementation.getImplementationClass()).isEqualTo(MockPermissionsRepository.class);
        assertThat(implementation.getInjectorModule()).isEqualTo(PermissionsRepositoryInjector.class);
    }

    @Test
    void findPermissionsRepositoryNoImplementation() {
        final Reflections reflections = new Reflections(new ConfigurationBuilder()
                .setUrls(ClasspathHelper.forClass(this.getClass()))
                .filterInputsBy(new FilterBuilder().exclude(".*" + MockPermissionsRepository.class.getSimpleName() + ".*"))
        );

        final ClassSearch classSearch = new ClassSearch(reflections);

        assertThatThrownBy(classSearch::findPermissionsRepositoryImplementation).isInstanceOf(NoImplementationFoundException.class);
    }

    // Mock classes for tests

    // --- AccountsRepository
    interface AccountsRepositoryNotImpl extends AccountsRepository {}

    @InjectorModule(target = AccountsRepository.class)
    private static class AccountsRepositoryInjector extends AbstractModule {}

    // --- PermissionsRepository

    interface PermissionsRepositoryNotImpl extends PermissionsRepository {}

    @InjectorModule(target = PermissionsRepository.class)
    private static class PermissionsRepositoryInjector extends AbstractModule {}

    // --- EMB
    private static class MessagePublisherImpl implements MessagePublisher {
        @Override
        public void publish(final MessageMO message) {
            
        }
    }

    interface MessagePublisherNotImpl extends MessagePublisher {}
}