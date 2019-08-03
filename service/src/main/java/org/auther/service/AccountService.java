package org.auther.service;

import org.auther.service.model.AccountBO;
import org.auther.service.model.TokensBO;

import java.util.Optional;

public interface AccountService {
    AccountBO create(AccountBO account);
    Optional<AccountBO> getById(String accountId);
    Optional<TokensBO> authenticate(String autherization);
}
