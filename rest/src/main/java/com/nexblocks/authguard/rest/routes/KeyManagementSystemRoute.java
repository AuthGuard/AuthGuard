package com.nexblocks.authguard.rest.routes;

import com.google.inject.Inject;
import com.nexblocks.authguard.api.dto.entities.CryptoKeyDTO;
import com.nexblocks.authguard.api.dto.requests.CryptoKeyRequestDTO;
import com.nexblocks.authguard.api.dto.validation.violations.Violation;
import com.nexblocks.authguard.api.dto.validation.violations.ViolationType;
import com.nexblocks.authguard.api.routes.KeyManagementSystemApi;
import com.nexblocks.authguard.rest.exceptions.RequestValidationException;
import com.nexblocks.authguard.rest.mappers.RestMapper;
import com.nexblocks.authguard.rest.util.BodyHandler;
import com.nexblocks.authguard.rest.util.Cursors;
import com.nexblocks.authguard.rest.util.Domain;
import com.nexblocks.authguard.service.KeyManagementService;
import com.nexblocks.authguard.service.exceptions.ServiceNotFoundException;
import com.nexblocks.authguard.service.exceptions.codes.ErrorCode;
import com.nexblocks.authguard.service.model.EphemeralKeyBO;
import com.nexblocks.authguard.service.model.PersistedKeyBO;
import com.nexblocks.authguard.service.util.AsyncUtils;
import io.javalin.core.validation.Validator;
import io.javalin.http.Context;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class KeyManagementSystemRoute extends KeyManagementSystemApi {

    private final KeyManagementService keyManagementService;
    private final RestMapper restMapper;

    private final BodyHandler<CryptoKeyRequestDTO> requestBodyHandler;

    @Inject
    public KeyManagementSystemRoute(final KeyManagementService keyManagementService, final RestMapper restMapper) {
        this.keyManagementService = keyManagementService;
        this.restMapper = restMapper;

        this.requestBodyHandler = new BodyHandler.Builder<>(CryptoKeyRequestDTO.class)
                .build();
    }

    @Override
    public void generate(final Context context) {
        String domain = Domain.fromContext(context);
        CryptoKeyRequestDTO request = requestBodyHandler.getValidated(context);

        EphemeralKeyBO key = keyManagementService.generate(request.getAlgorithm().name(), request.getSize());

        if (!request.isPersist()) {
            context.status(200)
                    .json(restMapper.toDTO(key));
            return;
        }

        PersistedKeyBO toPersist = PersistedKeyBO.builder()
                .domain(domain)
                .algorithm(key.getAlgorithm())
                .name(request.getName())
                .size(key.getSize())
                .privateKey(key.getPrivateKey())
                .publicKey(key.getPublicKey())
                .passcodeProtected(request.isPasscodeProtected())
                .passcode(request.getPasscode())
                .build();

        CompletableFuture<CryptoKeyDTO> persisted = keyManagementService.create(toPersist)
                .thenApply(restMapper::toDTO);

        context.status(201).json(persisted);
    }

    @Override
    public void getByDomain(final Context context) {
        String domain = Domain.fromContext(context);
        Long cursor = context.queryParam("cursor", Long.class).getOrNull();
        Instant instantCursor = Cursors.parseInstantCursor(cursor);

        CompletableFuture<List<CryptoKeyDTO>> keys = keyManagementService.getByDomain(domain, instantCursor)
                .thenApply(list -> list.stream()
                        .map(restMapper::toDTO)
                        .collect(Collectors.toList()));

        context.json(keys);
    }

    @Override
    public void getById(final Context context) {
        Validator<Long> keyId = context.pathParam("id", Long.class);

        if (!keyId.isValid()) {
            throw new RequestValidationException(Collections.singletonList(new Violation("id", ViolationType.INVALID_VALUE)));
        }

        String decrypt = context.queryParam("decrypt");
        String passcode = context.queryParam("passcode");

        CompletableFuture<Optional<PersistedKeyBO>> retrieveFuture = Objects.equals(decrypt, "1") ?
                keyManagementService.getDecrypted(keyId.get(), Domain.fromContext(context), passcode) :
                keyManagementService.getById(keyId.get(), Domain.fromContext(context));

        CompletableFuture<CryptoKeyDTO> key = retrieveFuture
                .thenApply(opt -> opt.map(restMapper::toDTO)
                        .orElseThrow(() -> new ServiceNotFoundException(ErrorCode.CRYPTO_KEY_DOES_NOT_EXIST, "Key does not exist")));

        context.json(key);
    }

    @Override
    public void deleteById(final Context context) {
        Validator<Long> keyId = context.pathParam("id", Long.class);

        if (!keyId.isValid()) {
            throw new RequestValidationException(Collections.singletonList(new Violation("id", ViolationType.INVALID_VALUE)));
        }

        CompletableFuture<CryptoKeyDTO> key = keyManagementService.delete(keyId.get(), Domain.fromContext(context))
                .thenCompose(opt -> AsyncUtils.fromOptional(opt, ErrorCode.CRYPTO_KEY_DOES_NOT_EXIST, "Key does not exist"))
                .thenApply(restMapper::toDTO);

        context.json(key);
    }
}
