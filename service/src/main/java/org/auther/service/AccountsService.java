package org.auther.service;

import org.auther.service.model.AccountBO;
import org.auther.service.model.PermissionBO;

import java.util.List;
import java.util.Optional;

public interface AccountsService {
    AccountBO create(AccountBO account);
    Optional<AccountBO> getById(String accountId);
    AccountBO grantPermissions(String accountId, List<PermissionBO> permissions);
    AccountBO revokePermissions(String accountId, List<PermissionBO> permissions);
    AccountBO grantRoles(String accountId, List<String> roles);
    AccountBO revokeRoles(String accountId, List<String> roles);
}
