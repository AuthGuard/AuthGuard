package com.nexblocks.authguard.crypto.generators;

public abstract class GeneratorParameters {
    private final int size;

    protected GeneratorParameters(final int size) {
        this.size = size;
    }

    public int getSize() {
        return size;
    }
}
