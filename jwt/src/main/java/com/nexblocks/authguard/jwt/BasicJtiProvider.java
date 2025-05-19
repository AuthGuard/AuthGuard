package com.nexblocks.authguard.jwt;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.nexblocks.authguard.dal.cache.AccountTokensRepository;
import com.nexblocks.authguard.dal.model.AccountTokenDO;
import com.nexblocks.authguard.service.util.ID;

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
        final AccountTokenDO persistedToken = accountTokensRepository.save(AccountTokenDO.builder()
                .id(ID.generate())
                .token(ID.generateSimplifiedUuid())
                .build())
                .subscribeAsCompletionStage()
                .join();

        return persistedToken.getToken();
    }

    @Override
    public boolean validate(final String jti) {
        return accountTokensRepository.getByToken(jti).join().isPresent();
    }
}
