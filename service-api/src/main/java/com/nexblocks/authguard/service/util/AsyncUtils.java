package com.nexblocks.authguard.service.util;

import io.vavr.control.Try;

import java.util.concurrent.CompletableFuture;

public final class AsyncUtils {
    public static <T> CompletableFuture<T> fromTry(Try<T> opt) {
        return opt.map(CompletableFuture::completedFuture)
                .getOrElseGet(CompletableFuture::failedFuture);
    }
}
