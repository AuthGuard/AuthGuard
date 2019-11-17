package org.auther.dal.mock;

import com.google.inject.Singleton;
import org.auther.dal.AccountsRepository;
import org.auther.dal.model.AccountDO;

@Singleton
public class MockAccountsRepository extends AbstractRepository<AccountDO> implements AccountsRepository {
}
