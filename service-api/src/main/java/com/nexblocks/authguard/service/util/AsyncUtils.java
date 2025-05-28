package com.nexblocks.authguard.service.util;

import com.nexblocks.authguard.service.exceptions.ServiceNotFoundException;
import com.nexblocks.authguard.service.exceptions.codes.ErrorCode;
import com.nexblocks.authguard.service.model.AccountBO;
import com.nexblocks.authguard.service.model.AppBO;
import com.nexblocks.authguard.service.model.ClientBO;
import io.smallrye.mutiny.Uni;
import io.vavr.control.Try;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public final class AsyncUtils {
    public static <T> CompletableFuture<T> fromTry(final Try<T> opt) {
        return opt.map(CompletableFuture::completedFuture)
                .getOrElseGet(CompletableFuture::failedFuture);
    }

    public static <T> Uni<T> uniFromTry(final Try<T> opt) {
        return opt.map(item -> Uni.createFrom().item(item))
                .getOrElseGet(ex -> Uni.createFrom().failure(ex));
    }

    public static <T> CompletableFuture<T> fromOptional(final Optional<T> opt, final ErrorCode missingCode, final String missingMessage) {
        return opt.map(CompletableFuture::completedFuture)
                .orElseGet(() -> CompletableFuture.failedFuture(new ServiceNotFoundException(missingCode, missingMessage)));
    }

    public static <T> Uni<T> uniFromOptional(final Optional<T> opt, final ErrorCode missingCode, final String missingMessage) {
        return opt.map(item -> Uni.createFrom().item(item))
                .orElseGet(() -> Uni.createFrom().failure(new ServiceNotFoundException(missingCode, missingMessage)));
    }

    public static CompletableFuture<AccountBO> fromAccountOptional(final Optional<AccountBO> opt) {
        return fromOptional(opt, ErrorCode.ACCOUNT_DOES_NOT_EXIST, "Account does not exist");
    }

    public static Uni<AccountBO> uniFromAccountOptional(final Optional<AccountBO> opt) {
        return uniFromOptional(opt, ErrorCode.ACCOUNT_DOES_NOT_EXIST, "Account does not exist");
    }

    public static CompletableFuture<AppBO> fromAppOptional(final Optional<AppBO> opt) {
        return fromOptional(opt, ErrorCode.APP_DOES_NOT_EXIST, "Application does not exist");
    }

    public static Uni<AppBO> uniFromAppOptional(final Optional<AppBO> opt) {
        return uniFromOptional(opt, ErrorCode.APP_DOES_NOT_EXIST, "Application does not exist");
    }

    public static CompletableFuture<ClientBO> fromClientOptional(final Optional<ClientBO> opt) {
        return fromOptional(opt, ErrorCode.APP_DOES_NOT_EXIST, "Client does not exist");
    }

    public static Uni<ClientBO> uniFromClientOptional(final Optional<ClientBO> opt) {
        return uniFromOptional(opt, ErrorCode.APP_DOES_NOT_EXIST, "Client does not exist");
    }
}
