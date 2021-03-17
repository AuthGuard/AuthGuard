package com.nexblocks.authguard.bindings;

import com.google.common.collect.ImmutableSet;
import com.nexblocks.authguard.service.model.BindingBO;

import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class PluginsRegistry {
    private static final Set<BindingBO> bindings = new HashSet<>();

    static void register(final BindingBO binding) {
        bindings.add(binding);
    }

    static void register(final Class<?> clazz) {
        final URL uri = clazz.getProtectionDomain().getCodeSource().getLocation();

        register(BindingBO.builder()
                .packageName(clazz.getPackageName())
                .name(clazz.getSimpleName())
                .location(uri.getFile())
                .build());
    }

    public static Set<BindingBO> getBindings() {
        return ImmutableSet.copyOf(bindings);
    }

    public static Map<String, List<BindingBO>> getBindingsGroupedByPackage() {
        return bindings.stream()
                .collect(Collectors.groupingBy(BindingBO::getPackageName));
    }
}
