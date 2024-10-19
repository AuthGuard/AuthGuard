package com.nexblocks.authguard.rest.routes;

import com.google.inject.Inject;
import com.nexblocks.authguard.api.annotations.DependsOnConfiguration;
import com.nexblocks.authguard.api.common.BodyHandler;
import com.nexblocks.authguard.api.common.Domain;
import com.nexblocks.authguard.api.common.RequestValidationException;
import com.nexblocks.authguard.api.dto.entities.CollectionResponseDTO;
import com.nexblocks.authguard.api.dto.entities.TotpKeyDTO;
import com.nexblocks.authguard.api.dto.requests.TotpKeyRequestDTO;
import com.nexblocks.authguard.api.dto.validation.violations.Violation;
import com.nexblocks.authguard.api.dto.validation.violations.ViolationType;
import com.nexblocks.authguard.api.routes.TotpApi;
import com.nexblocks.authguard.rest.mappers.RestMapper;
import com.nexblocks.authguard.service.TotpKeysService;
import io.javalin.validation.Validator;
import io.javalin.http.Context;

import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@DependsOnConfiguration("totpAuthenticators")
public class TotpRoute extends TotpApi {
    private final TotpKeysService totpKeysService;
    private final RestMapper restMapper;

    private final BodyHandler<TotpKeyRequestDTO> requestBodyHandler;

    @Inject
    public TotpRoute(final TotpKeysService totpKeysService, final RestMapper restMapper) {
        this.totpKeysService = totpKeysService;
        this.restMapper = restMapper;

        this.requestBodyHandler = new BodyHandler.Builder<>(TotpKeyRequestDTO.class)
                .build();
    }

    @Override
    public void generate(final Context context) {
        String domain = Domain.fromContext(context);
        TotpKeyRequestDTO request = requestBodyHandler.getValidated(context);

        CompletableFuture<TotpKeyDTO> persisted =
                totpKeysService.generate(request.getAccountId(), domain, request.getAuthenticator())
                        .thenApply(restMapper::toDTO);

        context.future(() -> persisted.thenAccept(r -> context.status(201).json(r)));
    }

    @Override
    public void getByAccountId(final Context context) {
        Validator<Long> accountId = context.pathParamAsClass("accountId", Long.class);

        if (!accountId.hasValue()) {
            throw new RequestValidationException(Collections.singletonList(new Violation("accountId", ViolationType.INVALID_VALUE)));
        }

        String domain = Domain.fromContext(context);

        context.future(() -> totpKeysService.getByAccountId(accountId.get(), domain)
                .thenApply(list -> CollectionResponseDTO.builder()
                        .items(list.stream().map(restMapper::toDTO).collect(Collectors.toList()))
                        .build())
                .thenAccept(context::json));
    }

    @Override
    public void deleteById(final Context context) {
        Validator<Long> id = context.pathParamAsClass("id", Long.class);

        if (!id.hasValue()) {
            throw new RequestValidationException(Collections.singletonList(new Violation("id", ViolationType.INVALID_VALUE)));
        }

        String domain = Domain.fromContext(context);

        context.future(() -> totpKeysService.delete(id.get(), domain)
                .thenApply(list -> CollectionResponseDTO.builder()
                        .items(list.stream().map(restMapper::toDTO).collect(Collectors.toList()))
                        .build())
                .thenAccept(context::json));
    }
}
