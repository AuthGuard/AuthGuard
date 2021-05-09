package com.nexblocks.authguard.jwt.exchange;

import com.google.inject.Inject;
import com.nexblocks.authguard.service.AccountsService;
import com.nexblocks.authguard.service.exceptions.ServiceAuthorizationException;
import com.nexblocks.authguard.service.exceptions.codes.ErrorCode;
import com.nexblocks.authguard.service.model.AccountBO;
import io.vavr.control.Either;

/**
 * An adapter class to wrap {@link AccountsService} to
 * provide a difference interface, more suitable for
 * its use here.
 */
public class AccountsServiceAdapter {
    private final AccountsService accountsService;

    @Inject
    public AccountsServiceAdapter(final AccountsService accountsService) {
        this.accountsService = accountsService;
    }

    public Either<Exception, AccountBO> getAccount(final String accountId) {
        return accountsService.getById(accountId)
                .<Either<Exception, AccountBO>>map(Either::right)
                .orElseGet(() -> Either.left(new ServiceAuthorizationException(ErrorCode.ACCOUNT_DOES_NOT_EXIST,
                        "Account " + accountId + " does not exist")));
    }
}
