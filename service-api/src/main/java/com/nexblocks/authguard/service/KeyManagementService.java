package com.nexblocks.authguard.service;

import com.nexblocks.authguard.service.model.EphemeralKeyBO;
import com.nexblocks.authguard.service.model.PersistedKeyBO;

public interface KeyManagementService extends CrudService<PersistedKeyBO> {
    EphemeralKeyBO generate(String algorithm, int size);
}
