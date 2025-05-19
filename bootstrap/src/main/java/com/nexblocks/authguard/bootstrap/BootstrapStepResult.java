package com.nexblocks.authguard.bootstrap;

public class BootstrapStepResult {
    private final boolean successful;
    private final Exception error;

    private BootstrapStepResult(final boolean successful, final Exception error) {
        this.successful = successful;
        this.error = error;
    }

    public static BootstrapStepResult success() {
        return new BootstrapStepResult(true, null);
    }

    public static BootstrapStepResult failure(Exception error) {
        return new BootstrapStepResult(false, error);
    }
}
