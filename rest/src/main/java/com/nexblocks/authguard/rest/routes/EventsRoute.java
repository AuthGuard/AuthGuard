package com.nexblocks.authguard.rest.routes;

import com.nexblocks.authguard.api.access.ActorRoles;
import com.google.inject.Inject;
import com.nexblocks.authguard.api.common.Cursors;
import com.nexblocks.authguard.api.common.Domain;
import com.nexblocks.authguard.api.dto.entities.CollectionResponse;
import com.nexblocks.authguard.api.dto.entities.CollectionResponseDTO;
import com.nexblocks.authguard.api.dto.entities.EventDTO;
import com.nexblocks.authguard.api.routes.EventsApi;
import com.nexblocks.authguard.rest.mappers.RestMapper;
import com.nexblocks.authguard.service.EventsService;
import com.nexblocks.authguard.service.model.EventBO;
import io.javalin.http.Context;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static io.javalin.apibuilder.ApiBuilder.get;

public class EventsRoute extends EventsApi {
    private final EventsService eventsService;
    private final RestMapper restMapper;

    @Inject
    public EventsRoute(final EventsService eventsService, final RestMapper restMapper) {
        this.eventsService = eventsService;
        this.restMapper = restMapper;
    }

    @Override
    public String getPath() {
        return "/domains/{domain}/events";
    }

    @Override
    public void addEndpoints() {
        get("/", this::getByDomain, ActorRoles.adminClient());
    }

    @Override
    public void getByDomain(final Context context) {
        String domain = Domain.fromContext(context);
        String channel = context.queryParam("channel");
        Long cursor = context.queryParamAsClass("cursor", Long.class).getOrDefault(null);
        Instant instantCursor = Cursors.parseInstantCursor(cursor);

        CompletableFuture<List<EventBO>> eventsFuture;

        if (channel != null) {
            eventsFuture = eventsService.getByDomainAndChannel(domain, channel, instantCursor);
        } else {
            eventsFuture = eventsService.getByDomain(domain, instantCursor);
        }

        CompletableFuture<CollectionResponse<EventDTO>> result = eventsFuture
                .thenApply(list -> list.stream().map(restMapper::toDTO).collect(Collectors.toList()))
                .thenApply(this::collectionResponse);

        context.future(() -> result.thenAccept(context::json));
    }

    private CollectionResponseDTO<EventDTO> collectionResponse(List<EventDTO> list) {
        return CollectionResponseDTO.<EventDTO>builder()
                .items(list)
                .build();
    }
}
