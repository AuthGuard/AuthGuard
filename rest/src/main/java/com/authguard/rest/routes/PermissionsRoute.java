package com.authguard.rest.routes;

import com.authguard.api.dto.entities.Error;
import com.authguard.api.dto.entities.PermissionDTO;
import com.authguard.api.routes.PermissionsApi;
import com.authguard.rest.mappers.RestJsonMapper;
import com.authguard.rest.mappers.RestMapper;
import com.authguard.service.PermissionsService;
import com.authguard.service.exceptions.codes.ErrorCode;
import com.authguard.service.model.PermissionBO;
import com.google.inject.Inject;
import io.javalin.http.Context;

import java.util.List;
import java.util.stream.Collectors;

public class PermissionsRoute extends PermissionsApi {
    private final PermissionsService permissionsService;
    private final RestMapper restMapper;

    @Inject
    public PermissionsRoute(final PermissionsService permissionsService, final RestMapper restMapper) {
        this.permissionsService = permissionsService;
        this.restMapper = restMapper;
    }

    public void create(final Context context) {
        final PermissionDTO permission = RestJsonMapper.asClass(context.body(), PermissionDTO.class);
        final PermissionBO created = permissionsService.create(restMapper.toBO(permission));

        context.status(201)
                .json(restMapper.toDTO(created));
    }

    @Override
    public void getById(final Context context) {
        final String id = context.pathParam("id");

        permissionsService.getById(id)
                .map(restMapper::toDTO)
                .ifPresentOrElse(
                        context::json,
                        // or else
                        () -> context.status(404)
                                .json(new Error(ErrorCode.PERMISSION_DOES_NOT_EXIST.getCode(),
                                        "No role with ID " + id + " exists"))
                );
    }

    @Override
    public void deleteById(final Context context) {
        final String id = context.pathParam("id");

        permissionsService.delete(id)
                .map(restMapper::toDTO)
                .ifPresentOrElse(
                        context::json,
                        // or else
                        () -> context.status(404)
                                .json(new Error(ErrorCode.PERMISSION_DOES_NOT_EXIST.getCode(),
                                        "No role with ID " + id + " exists"))
                );
    }

    @Override
    public void getByGroup(final Context context) {
        final String group = context.pathParam("group");

        final List<PermissionDTO> permissions = permissionsService.getAllForGroup(group)
                .stream()
                .map(restMapper::toDTO)
                .collect(Collectors.toList());

        context.json(permissions);
    }

    public void getAll(final Context context) {
        final List<PermissionDTO> permissions = permissionsService.getAll().stream()
                .map(restMapper::toDTO)
                .collect(Collectors.toList());

        context.json(permissions);
    }
}
