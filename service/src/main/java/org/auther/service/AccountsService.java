package org.auther.service;

import org.auther.service.model.AccountBO;
import org.auther.service.model.PermissionBO;
import org.auther.service.model.TokensBO;

import java.util.List;
import java.util.Optional;

public interface AccountsService {
    AccountBO create(AccountBO account);
    Optional<AccountBO> getById(String accountId);
    Optional<TokensBO> authenticate(String autherization);
    AccountBO grantPermissions(String accountId, List<PermissionBO> permissions);
    AccountBO revokePermissions(String accountId, List<PermissionBO> permissions);
}
