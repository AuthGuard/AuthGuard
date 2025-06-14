package com.nexblocks.authguard.rest.vertx;

import com.google.inject.Inject;
import com.nexblocks.authguard.api.access.VertxRolesAccessHandler;
import com.nexblocks.authguard.api.common.Cursors;
import com.nexblocks.authguard.api.dto.entities.CollectionResponseDTO;
import com.nexblocks.authguard.api.dto.entities.EventDTO;
import com.nexblocks.authguard.api.routes.VertxApiHandler;
import com.nexblocks.authguard.rest.mappers.RestMapper;
import com.nexblocks.authguard.service.EventsService;
import io.vertx.core.json.Json;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

public class EventsHandler implements VertxApiHandler {
    private final EventsService eventsService;
    private final RestMapper restMapper;

    @Inject
    public EventsHandler(final EventsService eventsService, final RestMapper restMapper) {
        this.eventsService = eventsService;
        this.restMapper = restMapper;
    }

    public void register(final Router router) {
        router.get("/domains/:domain/events")
                .handler(VertxRolesAccessHandler.onlyAdminClient())
                .handler(this::getByDomain);
    }

    private void getByDomain(final RoutingContext context) {
        String domain = context.pathParam("domain");
        String channel = context.queryParam("channel").stream().findFirst().orElse(null);
        Long cursor = Cursors.getLongCursor(context);
        Instant instantCursor = Cursors.parseInstantCursor(cursor);

        var eventsFuture = channel != null
                ? eventsService.getByDomainAndChannel(domain, channel, instantCursor)
                : eventsService.getByDomain(domain, instantCursor);

        eventsFuture
                .map(events -> events.stream().map(restMapper::toDTO).collect(Collectors.toList()))
                .map(this::collectionResponse)
                .subscribe().withSubscriber(new VertxJsonSubscriber<>(context));
    }

    private CollectionResponseDTO<EventDTO> collectionResponse(List<EventDTO> list) {
        return CollectionResponseDTO.<EventDTO>builder()
                .items(list)
                .build();
    }
}
