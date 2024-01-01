package com.nexblocks.authguard.rest.routes;

import com.nexblocks.authguard.api.dto.entities.Error;
import com.nexblocks.authguard.api.dto.entities.PermissionDTO;
import com.nexblocks.authguard.api.dto.requests.CreatePermissionRequestDTO;
import com.nexblocks.authguard.api.dto.validation.violations.Violation;
import com.nexblocks.authguard.api.dto.validation.violations.ViolationType;
import com.nexblocks.authguard.api.routes.PermissionsApi;
import com.nexblocks.authguard.rest.exceptions.RequestValidationException;
import com.nexblocks.authguard.rest.mappers.RestMapper;
import com.nexblocks.authguard.rest.util.BodyHandler;
import com.nexblocks.authguard.service.PermissionsService;
import com.nexblocks.authguard.service.exceptions.codes.ErrorCode;
import com.nexblocks.authguard.service.model.PermissionBO;
import com.google.inject.Inject;
import io.javalin.core.validation.Validator;
import io.javalin.http.Context;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class PermissionsRoute extends PermissionsApi {
    private final PermissionsService permissionsService;
    private final RestMapper restMapper;

    private final BodyHandler<CreatePermissionRequestDTO> createPermissionRequestBodyHandler;

    @Inject
    public PermissionsRoute(final PermissionsService permissionsService, final RestMapper restMapper) {
        this.permissionsService = permissionsService;
        this.restMapper = restMapper;

        this.createPermissionRequestBodyHandler = new BodyHandler.Builder<>(CreatePermissionRequestDTO.class)
                .build();
    }

    public void create(final Context context) {
        final CreatePermissionRequestDTO permission = createPermissionRequestBodyHandler.getValidated(context);
        final PermissionBO created = permissionsService.create(restMapper.toBO(permission)).join();

        context.status(201)
                .json(restMapper.toDTO(created));
    }

    @Override
    public void getById(final Context context) {
        final Validator<Long> id = context.pathParam("id", Long.class);

        if (!id.isValid()) {
            throw new RequestValidationException(Collections.singletonList(new Violation("id", ViolationType.INVALID_VALUE)));
        }

        permissionsService.getById(id.get()).join()
                .map(restMapper::toDTO)
                .ifPresentOrElse(
                        context::json,
                        // or else
                        () -> context.status(404)
                                .json(new Error(ErrorCode.PERMISSION_DOES_NOT_EXIST.getCode(),
                                        "No role with ID " + id.get() + " exists"))
                );
    }

    @Override
    public void deleteById(final Context context) {
        final Validator<Long> id = context.pathParam("id", Long.class);

        if (!id.isValid()) {
            throw new RequestValidationException(Collections.singletonList(new Violation("id", ViolationType.INVALID_VALUE)));
        }

        permissionsService.delete(id.get()).join()
                .map(restMapper::toDTO)
                .ifPresentOrElse(
                        context::json,
                        // or else
                        () -> context.status(404)
                                .json(new Error(ErrorCode.PERMISSION_DOES_NOT_EXIST.getCode(),
                                        "No role with ID " + id.get() + " exists"))
                );
    }

    @Override
    public void getByGroup(final Context context) {
        final String group = context.pathParam("group");
        final String domain = context.pathParam("domain");

        final List<PermissionDTO> permissions = permissionsService.getAllForGroup(group, domain)
                .stream()
                .map(restMapper::toDTO)
                .collect(Collectors.toList());

        context.json(permissions);
    }

    public void getAll(final Context context) {
        final String domain = context.pathParam("domain");

        final List<PermissionDTO> permissions = permissionsService.getAll(domain).stream()
                .map(restMapper::toDTO)
                .collect(Collectors.toList());

        context.json(permissions);
    }
}
