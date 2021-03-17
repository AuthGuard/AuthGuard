package com.nexblocks.authguard.bindings;

import com.nexblocks.authguard.service.model.BindingBO;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PluginsRegistryTest {

    @Test
    void register() {
        PluginsRegistry.register(PluginsRegistry.class);

        assertThat(PluginsRegistry.getBindingsGroupedByPackage())
                .containsKey(this.getClass().getPackageName());
    }

    @Test
    void registryBindingsSetIsImmutable() {
        assertThatThrownBy(() -> PluginsRegistry.getBindings().add(BindingBO.builder().build()))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}