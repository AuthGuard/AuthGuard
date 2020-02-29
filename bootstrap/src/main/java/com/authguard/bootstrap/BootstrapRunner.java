package com.authguard.bootstrap;

import com.authguard.injection.ClassSearch;
import com.google.inject.Injector;

public class BootstrapRunner {
    private final ClassSearch classSearch;
    private final Injector injector;

    public BootstrapRunner(final ClassSearch classSearch, final Injector injector) {
        this.classSearch = classSearch;
        this.injector = injector;
    }

    public void runAll() {
        classSearch.findAllImplementationClass(BootstrapStep.class)
                .stream()
                .map(injector::getInstance)
                .forEach(BootstrapStep::run);
    }
}
