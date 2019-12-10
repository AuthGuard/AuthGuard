package org.auther.dal.mock;

import com.google.inject.Singleton;
import org.auther.dal.AccountsRepository;
import org.auther.dal.model.AccountDO;

import java.util.List;
import java.util.stream.Collectors;

@Singleton
public class MockAccountsRepository extends AbstractRepository<AccountDO> implements AccountsRepository {
    @Override
    public List<AccountDO> getAdmins() {
        return getRepo().values()
                .stream()
                .filter(account -> account.getRoles().contains("admin"))
                .collect(Collectors.toList());
    }
}
