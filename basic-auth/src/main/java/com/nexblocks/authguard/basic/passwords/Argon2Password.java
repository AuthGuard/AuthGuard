package com.nexblocks.authguard.basic.passwords;

import com.nexblocks.authguard.basic.config.ArgonConfig;
import io.smallrye.mutiny.Uni;
import org.bouncycastle.crypto.generators.Argon2BytesGenerator;
import org.bouncycastle.crypto.params.Argon2Parameters;

import java.nio.charset.StandardCharsets;

public class Argon2Password extends AbstractSecurePassword {
    private final ArgonConfig config;

    public Argon2Password() {
        this(ArgonConfig.builder().build());
    }

    public Argon2Password(ArgonConfig config) {
        super(config.getSaltSize());

        this.config = config;
    }

    @Override
    protected Uni<byte[]> hashWithSalt(final String plain, final byte[] saltBytes) {
        return Uni.createFrom().item(() -> {
            Argon2Parameters builder = createParameters(saltBytes);
            int hashLength = 32;

            Argon2BytesGenerator generate = new Argon2BytesGenerator();
            generate.init(builder);
            byte[] result = new byte[hashLength];
            generate.generateBytes(plain.getBytes(StandardCharsets.UTF_8), result, 0, result.length);

            return result;
        });
    }

    private Argon2Parameters createParameters(final byte[] saltBytes) {
        int iterations = config.getIterations();
        int memLimit = config.getMemoryLimit();
        int parallelism = config.getParallelism();

        return new Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
                .withVersion(Argon2Parameters.ARGON2_id)
                .withIterations(iterations)
                .withMemoryAsKB(memLimit)
                .withParallelism(parallelism)
                .withSalt(saltBytes)
                .build();
    }
}
