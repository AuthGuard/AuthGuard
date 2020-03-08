package com.authguard.service.impl;

import com.authguard.service.exceptions.ServiceNotFoundException;
import com.authguard.service.impl.jwt.ApiTokenProvider;
import com.authguard.service.impl.mappers.ServiceMapper;
import com.google.inject.Inject;
import com.authguard.dal.ApiKeysRepository;
import com.authguard.service.ApiKeysService;
import com.authguard.service.ApplicationsService;
import com.authguard.service.model.AppBO;
import com.authguard.service.model.TokensBO;

import java.util.Optional;
import java.util.UUID;

public class ApiKeysServiceImpl implements ApiKeysService {
    private final ApplicationsService applicationsService;
    private final ApiTokenProvider tokenProvider;
    private final ApiKeysRepository keysRepository;
    private final ServiceMapper serviceMapper;

    @Inject
    public ApiKeysServiceImpl(final ApplicationsService applicationsService, final ApiTokenProvider tokenProvider,
                              final ApiKeysRepository keysRepository, final ServiceMapper serviceMapper) {
        this.applicationsService = applicationsService;
        this.tokenProvider = tokenProvider;
        this.keysRepository = keysRepository;
        this.serviceMapper = serviceMapper;
    }

    @Override
    public String generateApiKey(final String appId) {
        final AppBO app = applicationsService.getById(appId)
                .orElseThrow(() -> new ServiceNotFoundException("No app with ID " + appId + " found"));

        return generateApiKey(app);
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