package com.nexblocks.authguard.rest.vertx;

import com.google.inject.Inject;
import com.nexblocks.authguard.api.access.VertxRolesAccessHandler;
import com.nexblocks.authguard.api.common.Domain;
import com.nexblocks.authguard.api.routes.VertxApiHandler;
import com.nexblocks.authguard.rest.mappers.RestMapper;
import com.nexblocks.authguard.service.TrackingSessionsService;
import com.nexblocks.authguard.service.exceptions.ServiceNotFoundException;
import com.nexblocks.authguard.service.exceptions.codes.ErrorCode;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

public class TrackingSessionsHandler implements VertxApiHandler {
    private final TrackingSessionsService trackingSessionsService;
    private final RestMapper restMapper;

    @Inject
    public TrackingSessionsHandler(final TrackingSessionsService trackingSessionsService,
                                   final RestMapper restMapper) {
        this.trackingSessionsService = trackingSessionsService;
        this.restMapper = restMapper;
    }

    @Override
    public void register(final Router router) {
        router.delete("/domains/:domain/tracking_sessions/:token")
                .handler(VertxRolesAccessHandler.onlyAdminClient())
                .handler(this::deleteByToken);
    }

    public void deleteByToken(final RoutingContext context) {
        String domain = Domain.fromContext(context);
        String token = context.pathParam("token");

        trackingSessionsService.terminateSession(token, domain)
                .map(opt -> opt.map(restMapper::toDTO)
                        .orElseThrow(() -> new ServiceNotFoundException(ErrorCode.INVALID_TOKEN,
                                "No session was found for the token")))
                .subscribe().withSubscriber(new VertxJsonSubscriber<>(context));
    }
}
