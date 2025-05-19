package com.nexblocks.authguard.bootstrap;

import io.smallrye.mutiny.Uni;

@FunctionalInterface
public interface BootstrapStep {
    Uni<BootstrapStepResult> run();
}
