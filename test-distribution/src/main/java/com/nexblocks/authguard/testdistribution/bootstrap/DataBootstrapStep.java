package com.nexblocks.authguard.testdistribution.bootstrap;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.inject.Inject;
import com.nexblocks.authguard.bootstrap.BootstrapStep;
import com.nexblocks.authguard.bootstrap.BootstrapStepResult;
import com.nexblocks.authguard.service.AccountsService;
import com.nexblocks.authguard.service.ApplicationsService;
import com.nexblocks.authguard.service.ClientsService;
import com.nexblocks.authguard.service.RolesService;
import com.nexblocks.authguard.service.PermissionsService;
import com.nexblocks.authguard.service.model.AccountBO;
import com.nexblocks.authguard.service.model.AppBO;
import com.nexblocks.authguard.service.model.ClientBO;
import com.nexblocks.authguard.service.model.RoleBO;
import com.nexblocks.authguard.service.model.PermissionBO;
import com.nexblocks.authguard.service.model.RequestContextBO;
import com.nexblocks.authguard.rest.mappers.RestMapper;
import com.nexblocks.authguard.api.dto.entities.RoleDTO;
import com.nexblocks.authguard.api.dto.entities.PermissionDTO;
import com.nexblocks.authguard.api.dto.requests.CreateAccountRequestDTO;
import com.nexblocks.authguard.api.dto.requests.CreateAppRequestDTO;
import com.nexblocks.authguard.api.dto.requests.CreateClientRequestDTO;
import io.smallrye.mutiny.Uni;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class DataBootstrapStep implements BootstrapStep {
    private static final Logger LOG = LoggerFactory.getLogger(DataBootstrapStep.class);
    private static final String BOOTSTRAP_DATA_FILE = "bootstrap_data.yaml";

    private final RolesService rolesService;
    private final PermissionsService permissionsService;
    private final AccountsService accountsService;
    private final ApplicationsService applicationsService;
    private final ClientsService clientsService;
    private final RestMapper restMapper;
    private final ObjectMapper yamlMapper;

    @Inject
    public DataBootstrapStep(final RolesService rolesService,
                             final PermissionsService permissionsService,
                             final AccountsService accountsService,
                             final ApplicationsService applicationsService,
                             final ClientsService clientsService,
                             final RestMapper restMapper) {
        this.rolesService = rolesService;
        this.permissionsService = permissionsService;
        this.accountsService = accountsService;
        this.applicationsService = applicationsService;
        this.clientsService = clientsService;
        this.restMapper = restMapper;
        this.yamlMapper = createYamlMapper();
    }

    @Override
    public Uni<BootstrapStepResult> run() {
        LOG.info("Running data bootstrap step");

        try {
            final BootstrapData bootstrapData = loadBootstrapData();
            
            if (bootstrapData == null) {
                LOG.warn("No bootstrap data file found at {}", BOOTSTRAP_DATA_FILE);
                return Uni.createFrom().item(BootstrapStepResult.success());
            }

            return createEntities(bootstrapData)
                    .onItem().transform(v -> {
                        LOG.info("Data bootstrap completed successfully");
                        return BootstrapStepResult.success();
                    })
                    .onFailure().recoverWithItem(error -> {
                        LOG.error("Data bootstrap failed", error);
                        return BootstrapStepResult.failure((Exception) error);
                    });
        } catch (Exception e) {
            LOG.error("Failed to load bootstrap data", e);
            return Uni.createFrom().item(BootstrapStepResult.failure(e));
        }
    }

    private BootstrapData loadBootstrapData() throws IOException {
        final ClassLoader classLoader = getClass().getClassLoader();
        final InputStream inputStream = classLoader.getResourceAsStream(BOOTSTRAP_DATA_FILE);

        if (inputStream == null) {
            return null;
        }

        return yamlMapper.readValue(inputStream, BootstrapData.class);
    }

    private Uni<Void> createEntities(final BootstrapData bootstrapData) {
        // Create roles and permissions first, then other entities
        return createRoles(bootstrapData)
                .chain(() -> createPermissions(bootstrapData))
                .chain(() -> createAccounts(bootstrapData))
                .chain(() -> createApplications(bootstrapData))
                .chain(() -> createClients(bootstrapData));
    }

    private Uni<Void> createRoles(final BootstrapData bootstrapData) {
        if (bootstrapData.getRoles() == null || bootstrapData.getRoles().isEmpty()) {
            return Uni.createFrom().voidItem();
        }

        final List<Uni<Void>> operations = new ArrayList<>();
        for (RoleDTO roleDTO : bootstrapData.getRoles()) {
            final RoleBO role = restMapper.toBO(roleDTO);
            final Uni<Void> createRole = rolesService
                    .create(role)
                    .onItem().transform(created -> {
                        LOG.info("Created role: {} ({})", created.getName(), created.getId());
                        return null;
                    });
            operations.add(createRole);
        }

        return Uni.combine().all().unis(operations).discardItems();
    }

    private Uni<Void> createPermissions(final BootstrapData bootstrapData) {
        if (bootstrapData.getPermissions() == null || bootstrapData.getPermissions().isEmpty()) {
            return Uni.createFrom().voidItem();
        }

        final List<Uni<Void>> operations = new ArrayList<>();
        for (PermissionDTO permissionDTO : bootstrapData.getPermissions()) {
            final PermissionBO permission = restMapper.toBO(permissionDTO);
            final Uni<Void> createPermission = permissionsService
                    .create(permission)
                    .onItem().transform(created -> {
                        LOG.info("Created permission: {}:{} ({})", created.getGroup(), created.getName(), created.getId());
                        return null;
                    });
            operations.add(createPermission);
        }

        return Uni.combine().all().unis(operations).discardItems();
    }

    private Uni<Void> createAccounts(final BootstrapData bootstrapData) {
        if (bootstrapData.getAccounts() == null || bootstrapData.getAccounts().isEmpty()) {
            return Uni.createFrom().voidItem();
        }

        final List<Uni<Void>> operations = new ArrayList<>();
        for (CreateAccountRequestDTO accountDTO : bootstrapData.getAccounts()) {
            final AccountBO account = restMapper.toBO(accountDTO);
            final Uni<Void> createAccount = accountsService
                    .create(account, createRequestContext(account))
                    .onItem().transform(created -> {
                        LOG.info("Created account: {} ({})", created.getEmail(), created.getId());
                        return null;
                    });
            operations.add(createAccount);
        }

        return Uni.combine().all().unis(operations).discardItems();
    }

    private Uni<Void> createApplications(final BootstrapData bootstrapData) {
        if (bootstrapData.getApplications() == null || bootstrapData.getApplications().isEmpty()) {
            return Uni.createFrom().voidItem();
        }

        final List<Uni<Void>> operations = new ArrayList<>();
        for (CreateAppRequestDTO appDTO : bootstrapData.getApplications()) {
            final AppBO app = restMapper.toBO(appDTO);
            final Uni<Void> createApp = applicationsService
                    .create(app, createRequestContext(app))
                    .onItem().transform(created -> {
                        LOG.info("Created application: {} ({})", created.getName(), created.getId());
                        return null;
                    });
            operations.add(createApp);
        }

        return Uni.combine().all().unis(operations).discardItems();
    }

    private Uni<Void> createClients(final BootstrapData bootstrapData) {
        if (bootstrapData.getClients() == null || bootstrapData.getClients().isEmpty()) {
            return Uni.createFrom().voidItem();
        }

        final List<Uni<Void>> operations = new ArrayList<>();
        for (CreateClientRequestDTO clientDTO : bootstrapData.getClients()) {
            final ClientBO client = restMapper.toBO(clientDTO);
            final Uni<Void> createClient = clientsService
                    .create(client, createRequestContext(client))
                    .onItem().transform(created -> {
                        LOG.info("Created client: {} ({})", created.getName(), created.getId());
                        return null;
                    });
            operations.add(createClient);
        }

        return Uni.combine().all().unis(operations).discardItems();
    }

    private RequestContextBO createRequestContext(final Object entity) {
        return RequestContextBO.builder()
                .idempotentKey("bootstrap-" + entity.hashCode())
                .build();
    }

    private ObjectMapper createYamlMapper() {
        final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return mapper;
    }
}
