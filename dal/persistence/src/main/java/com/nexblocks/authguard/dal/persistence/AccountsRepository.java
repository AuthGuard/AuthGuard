package com.nexblocks.authguard.dal.persistence;

import com.nexblocks.authguard.dal.model.AccountDO;
import com.nexblocks.authguard.dal.model.PasswordDO;
import com.nexblocks.authguard.dal.model.PermissionDO;
import com.nexblocks.authguard.dal.model.UserIdentifierDO;
import com.nexblocks.authguard.dal.repository.Repository;
import io.smallrye.mutiny.Uni;

import java.util.List;
import java.util.Optional;

public interface AccountsRepository extends Repository<AccountDO> {
    Uni<Optional<AccountDO>> getByExternalId(String externalId);
    Uni<Optional<AccountDO>> getByEmail(String email, String domain);
    Uni<List<AccountDO>> getByRole(String role, String domain);
    Uni<Optional<AccountDO>> findByIdentifier(String identifier, String domain);

    Uni<AccountDO> addAccountPermissions(AccountDO account, List<PermissionDO> permissions);
    Uni<AccountDO> removeAccountPermissions(AccountDO account, List<PermissionDO> permissions);

    Uni<AccountDO> addUserIdentifier(AccountDO account, UserIdentifierDO identifier);
    Uni<AccountDO> removeUserIdentifier(AccountDO account, UserIdentifierDO identifier);
    Uni<AccountDO> replaceIdentifierInPlace(AccountDO accountDO, String oldIdentifier, UserIdentifierDO newIdentifier);

    Uni<AccountDO> updateUserPassword(AccountDO account, PasswordDO hashedPassword);
}
