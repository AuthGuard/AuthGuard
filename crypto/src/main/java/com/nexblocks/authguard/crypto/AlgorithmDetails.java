package com.nexblocks.authguard.crypto;

import com.nexblocks.authguard.crypto.generators.Generator;
import com.nexblocks.authguard.crypto.generators.GeneratorParameters;

import java.util.Set;

public class AlgorithmDetails<T extends GeneratorParameters> {

    enum Type {
        SYMMETRIC,
        ASYMMETRIC
    }

    private final String name;
    private final Type type;
    private final Set<Integer> allowedSizes;
    private final Generator<T> generator;

    public AlgorithmDetails(final String name, final Type type, final Set<Integer> allowedSizes,
                            final Generator<T> generator) {
        this.name = name;
        this.type = type;
        this.allowedSizes = allowedSizes;
        this.generator = generator;
    }

    public String getName() {
        return name;
    }

    public Set<Integer> getAllowedSizes() {
        return allowedSizes;
    }

    public Type getType() {
        return type;
    }

    public Generator<T> getGenerator() {
        return generator;
    }
}
