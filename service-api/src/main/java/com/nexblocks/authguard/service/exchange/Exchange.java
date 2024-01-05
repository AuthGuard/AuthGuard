package com.nexblocks.authguard.service.exchange;

import com.nexblocks.authguard.service.model.AuthRequestBO;
import com.nexblocks.authguard.service.model.AuthResponseBO;

import java.util.concurrent.CompletableFuture;

public interface Exchange {
    CompletableFuture<AuthResponseBO> exchange(AuthRequestBO request);
}
