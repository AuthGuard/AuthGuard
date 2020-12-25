package com.authguard.jwt;

import com.authguard.dal.cache.AccountTokensRepository;
import com.authguard.dal.model.AccountTokenDO;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.util.UUID;

/**
 * This class is only here until a proper implementation is available
 */
@Singleton
public class BasicJtiProvider implements JtiProvider {
    private final AccountTokensRepository accountTokensRepository;

    @Inject
    public BasicJtiProvider(final AccountTokensRepository accountTokensRepository) {
        this.accountTokensRepository = accountTokensRepository;
    }

    @Override
    public String next() {
        final String id = UUID.randomUUID().toString();

        accountTokensRepository.save(AccountTokenDO.builder()
                .id(UUID.randomUUID().toString())
                .token(id)
                .build()).join();

        return id;
    }

    @Override
    public boolean validate(final String jti) {
        return accountTokensRepository.getByToken(jti).join().isPresent();
    }
}
