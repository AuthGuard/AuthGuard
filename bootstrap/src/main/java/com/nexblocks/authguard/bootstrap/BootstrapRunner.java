package com.nexblocks.authguard.bootstrap;

import com.nexblocks.authguard.injection.ClassSearch;
import com.google.inject.Injector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BootstrapRunner {
    private static Logger LOG = LoggerFactory.getLogger(BootstrapRunner.class);

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
                .forEach(step -> {
                    try {
                        step.run();
                    } catch (final Exception e) {
                        LOG.error("Bootstrap step {} threw an error", step.getClass().getCanonicalName(), e);
                    }
                });
    }
}
