package com.nexblocks.authguard.crypto.generators;

import com.nexblocks.authguard.crypto.GeneratorResult;

public interface Generator<T extends GeneratorParameters> {
    GeneratorResult generate(T parameters);
}
