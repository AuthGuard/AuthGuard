package com.nexblocks.authguard.jwt.exchange;

import com.google.inject.Inject;
import com.nexblocks.authguard.service.AccountsService;
import com.nexblocks.authguard.service.exceptions.ServiceAuthorizationException;
import com.nexblocks.authguard.service.exceptions.codes.ErrorCode;
import com.nexblocks.authguard.service.model.AccountBO;

import io.smallrye.mutiny.Uni;

/**
 * An adapter class to wrap {@link AccountsService} to
 * provide a different interface, more suitable for
 * its use here.
 */
public class AccountsServiceAdapter {
    private final AccountsService accountsService;

    @Inject
    public AccountsServiceAdapter(final AccountsService accountsService) {
        this.accountsService = accountsService;
    }

    public Uni<AccountBO> getAccount(final long accountId) {
        return accountsService.getByIdUnchecked(accountId)
                .flatMap(opt -> opt.map(item -> Uni.createFrom().item(item))
                        .orElseGet(() -> Uni.createFrom().failure(new ServiceAuthorizationException(ErrorCode.ACCOUNT_DOES_NOT_EXIST,
                                "Account does not exist"))));
    }
}
