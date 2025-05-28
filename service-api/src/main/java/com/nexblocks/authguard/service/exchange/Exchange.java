package com.nexblocks.authguard.service.exchange;

import com.nexblocks.authguard.service.model.AuthRequestBO;
import com.nexblocks.authguard.service.model.AuthResponseBO;

import io.smallrye.mutiny.Uni;

public interface Exchange {
    Uni<AuthResponseBO> exchange(AuthRequestBO request);
}
