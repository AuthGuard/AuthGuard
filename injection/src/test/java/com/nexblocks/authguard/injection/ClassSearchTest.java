package com.nexblocks.authguard.injection;

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
        final ClassSearch classSearch = new ClassSearch(new Reflections("com.nexblocks.authguard"));
        final Class<? extends Interface> implementation = classSearch.findImplementationClass(Interface.class);

        assertThat(implementation).isNotNull();
        assertThat(implementation).isEqualTo(MockAccountsRepository.class);
    }

    @Test
    void findAccountsRepositoryNoImplementation() {
        final Reflections reflections = new Reflections(new ConfigurationBuilder()
                .setUrls(ClasspathHelper.forClass(this.getClass()))
                .filterInputsBy(new FilterBuilder().exclude(".*" + MockAccountsRepository.class.getSimpleName() + ".*"))
        );

        final ClassSearch classSearch = new ClassSearch(reflections);

        assertThatThrownBy(() -> classSearch.findImplementationClass(Interface.class)).isInstanceOf(NoImplementationFoundException.class);
    }

    // Mock classes for tests
    interface Interface {}

    // --- AccountsRepository
    class MockAccountsRepository implements Interface { }

    interface NotImpl extends Interface {}

}