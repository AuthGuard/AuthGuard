package com.nexblocks.authguard.rest.routes;

import com.google.inject.Inject;
import com.nexblocks.authguard.api.common.Domain;
import com.nexblocks.authguard.api.dto.entities.SessionDTO;
import com.nexblocks.authguard.api.routes.TrackingSessionsApi;
import com.nexblocks.authguard.rest.mappers.RestMapper;
import com.nexblocks.authguard.service.TrackingSessionsService;
import com.nexblocks.authguard.service.exceptions.ServiceNotFoundException;
import com.nexblocks.authguard.service.exceptions.codes.ErrorCode;
import io.javalin.http.Context;

import java.util.concurrent.CompletableFuture;

public class TrackingSessionsRoute extends TrackingSessionsApi {
    private final TrackingSessionsService trackingSessionsService;
    private final RestMapper restMapper;

    @Inject
    public TrackingSessionsRoute(final TrackingSessionsService trackingSessionsService, final RestMapper restMapper) {
        this.trackingSessionsService = trackingSessionsService;
        this.restMapper = restMapper;
    }

    @Override
    public void deleteById(final Context context) {
        String domain = Domain.fromContext(context);
        String token = context.pathParam("token");

        CompletableFuture<SessionDTO> terminatedSession = trackingSessionsService.terminateSession(token, domain)
                .thenApply(opt -> opt.map(restMapper::toDTO)
                        .orElseThrow(() -> new ServiceNotFoundException(ErrorCode.INVALID_TOKEN,
                                "No session was found for the token")));

        context.future(() -> terminatedSession.thenAccept(context::json));
    }
}
