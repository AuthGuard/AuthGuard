package org.auther.dal.mock;

import com.google.inject.Singleton;
import org.auther.dal.AccountTokensRepository;
import org.auther.dal.model.AccountTokenDO;

import java.util.Optional;

@Singleton
public class MockAccountsTokensRepository extends AbstractRepository<AccountTokenDO> implements AccountTokensRepository {

    @Override
    public Optional<AccountTokenDO> getByToken(final String token) {
        return getRepo().values().stream()
                .filter(accountToken -> accountToken.getToken().equals(token))
                .findFirst();
    }
}
