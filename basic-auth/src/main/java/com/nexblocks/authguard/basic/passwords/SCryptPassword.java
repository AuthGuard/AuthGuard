package com.nexblocks.authguard.basic.passwords;

import com.nexblocks.authguard.basic.config.PasswordsConfig;
import com.nexblocks.authguard.basic.config.SCryptConfig;
import com.nexblocks.authguard.config.ConfigContext;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import io.smallrye.mutiny.Uni;
import org.bouncycastle.crypto.generators.SCrypt;

public class SCryptPassword extends AbstractSecurePassword {
    private final SCryptConfig config;

    public SCryptPassword() {
        this(SCryptConfig.builder().build());
    }

    public SCryptPassword(final SCryptConfig config) {
        super(config.getSaltSize());

        this.config = config;
    }

    @Inject
    public SCryptPassword(final @Named("passwords") ConfigContext config) {
        this(config.asConfigBean(PasswordsConfig.class).getScrypt());
    }

    @Override
    protected Uni<byte[]> hashWithSalt(final String plain, final byte[] saltBytes) {
        return Uni.createFrom().item(() -> SCrypt.generate(plain.getBytes(), saltBytes, config.getCPUMemoryCostParameter(),
                config.getBlockSize(), config.getParallelization(), config.getKeySize()));
    }
}
