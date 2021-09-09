package com.nexblocks.authguard.jwt.oauth.service;

import com.nexblocks.authguard.dal.cache.AccountTokensRepository;
import com.nexblocks.authguard.dal.model.AccountTokenDO;
import com.nexblocks.authguard.jwt.oauth.route.ImmutableOpenIdConnectRequest;
import com.nexblocks.authguard.service.ApplicationsService;
import com.nexblocks.authguard.service.exceptions.ServiceException;
import com.nexblocks.authguard.service.exceptions.codes.ErrorCode;
import com.nexblocks.authguard.service.model.AppBO;
import com.nexblocks.authguard.service.util.ID;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class OpenIdConnectService {
    private final AccountTokensRepository accountTokensRepository;
    private final ApplicationsService applicationsService;

    public OpenIdConnectService(final AccountTokensRepository accountTokensRepository,
                                final ApplicationsService applicationsService) {
        this.accountTokensRepository = accountTokensRepository;
        this.applicationsService = applicationsService;
    }

    public AccountTokenDO createToken(final ImmutableOpenIdConnectRequest request) {
        final AppBO clientApp = applicationsService.getById(request.getClientId())
                .orElseThrow(() -> new ServiceException(ErrorCode.APP_DOES_NOT_EXIST, "Client " + request.getClientId() + " does not exist"));

        // TODO find

        final Map<String, String> data = new HashMap<>();

        data.put("clientId", request.getClientId());
        data.put("redirectUri", request.getRedirectUri());
        data.put("state", request.getState());
        data.put("scope", String.join(",", request.getScope()));

        if (request.getNonce() != null) {
            data.put("nonce", request.getNonce());
        }

        final AccountTokenDO token = AccountTokenDO.builder()
                .id(ID.generate())
                .expiresAt(OffsetDateTime.now().plusMinutes(10))
                .additionalInformation(data)
                .build();

        return accountTokensRepository.save(token).join();
    }
}
