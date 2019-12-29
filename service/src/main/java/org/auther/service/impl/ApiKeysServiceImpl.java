package org.auther.service.impl;

import com.google.inject.Inject;
import org.auther.dal.ApiKeysRepository;
import org.auther.service.ApiKeysService;
import org.auther.service.impl.jwt.ApiTokenProvider;
import org.auther.service.impl.mappers.ServiceMapper;
import org.auther.service.model.AppBO;
import org.auther.service.model.TokensBO;

import java.util.Optional;
import java.util.UUID;

public class ApiKeysServiceImpl implements ApiKeysService {
    private final ApiTokenProvider tokenProvider;
    private final ApiKeysRepository keysRepository;
    private final ServiceMapper serviceMapper;

    @Inject
    public ApiKeysServiceImpl(final ApiTokenProvider tokenProvider, final ApiKeysRepository keysRepository,
                              final ServiceMapper serviceMapper) {
        this.tokenProvider = tokenProvider;
        this.keysRepository = keysRepository;
        this.serviceMapper = serviceMapper;
    }

    @Override
    public String generateApiKey(final AppBO app) {
        final TokensBO token = tokenProvider.generateToken(app);

        keysRepository.save(serviceMapper.toDO(token, app)
                .withId(UUID.randomUUID().toString()));

        return token.getToken();
    }

    @Override
    public Optional<AppBO> validateApiKey() {
        return Optional.empty();
    }
}
