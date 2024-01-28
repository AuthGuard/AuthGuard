package com.nexblocks.authguard.service.impl;

import com.google.inject.Inject;
import com.nexblocks.authguard.dal.model.ClientDO;
import com.nexblocks.authguard.dal.persistence.ClientsRepository;
import com.nexblocks.authguard.emb.MessageBus;
import com.nexblocks.authguard.service.AccountsService;
import com.nexblocks.authguard.service.ClientsService;
import com.nexblocks.authguard.service.IdempotencyService;
import com.nexblocks.authguard.service.exceptions.ServiceNotFoundException;
import com.nexblocks.authguard.service.exceptions.codes.ErrorCode;
import com.nexblocks.authguard.service.mappers.ServiceMapper;
import com.nexblocks.authguard.service.model.ClientBO;
import com.nexblocks.authguard.service.model.RequestContextBO;
import com.nexblocks.authguard.service.util.AsyncUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class ClientsServiceImpl implements ClientsService {

    private static final Logger LOG = LoggerFactory.getLogger(ClientsServiceImpl.class);

    private static final String CLIENTS_CHANNEL = "clients";

    private final ClientsRepository clientsRepository;
    private final AccountsService accountsService;
    private final IdempotencyService idempotencyService;
    private final ServiceMapper serviceMapper;
    private final PersistenceService<ClientBO, ClientDO, ClientsRepository> persistenceService;

    @Inject
    public ClientsServiceImpl(final ClientsRepository clientsRepository,
                                   final AccountsService accountsService,
                                   final IdempotencyService idempotencyService,
                                   final ServiceMapper serviceMapper,
                                   final MessageBus messageBus) {
        this.clientsRepository = clientsRepository;
        this.accountsService = accountsService;
        this.idempotencyService = idempotencyService;
        this.serviceMapper = serviceMapper;

        this.persistenceService = new PersistenceService<>(clientsRepository, messageBus,
                serviceMapper::toDO, serviceMapper::toBO, CLIENTS_CHANNEL);
    }

    @Override
    public CompletableFuture<ClientBO> create(final ClientBO client, final RequestContextBO requestContext) {
        return idempotencyService.performOperationAsync(() -> doCreate(client), requestContext.getIdempotentKey(),
                client.getEntityType());
    }

    private CompletableFuture<ClientBO> doCreate(final ClientBO client) {
        if (client.getAccountId() != null) {
            return accountsService.getById(client.getAccountId(), client.getDomain())
                    .thenCompose(opt -> {
                        if (opt.isEmpty()) {
                            throw new ServiceNotFoundException(ErrorCode.ACCOUNT_DOES_NOT_EXIST,
                                    "No account with ID " + client.getAccountId() + " exists");
                        }

                        return persistenceService.create(client);
                    });
        }

        return persistenceService.create(client);
    }

    @Override
    public CompletableFuture<Optional<ClientBO>> getById(final long id, final String domain) {
        return persistenceService.getById(id)
                .thenApply(opt -> opt.filter(client -> Objects.equals(client.getDomain(), domain)));
    }

    @Override
    public CompletableFuture<Optional<ClientBO>> getByIdUnchecked(final long id) {
        return persistenceService.getById(id);
    }

    @Override
    public CompletableFuture<Optional<ClientBO>> getByExternalId(final String externalId, final String domain) {
        return clientsRepository.getByExternalId(externalId)
                .thenApply(optional -> optional
                        .filter(client -> Objects.equals(client.getDomain(), domain))
                        .map(serviceMapper::toBO));
    }

    @Override
    public CompletableFuture<Optional<ClientBO>> update(final ClientBO client, final String domain) {
        LOG.info("Client update request. accountId={}", client.getId());

        // FIXME accountId cannot be updated
        return persistenceService.update(client);
    }

    @Override
    public CompletableFuture<Optional<ClientBO>> delete(final long id, String domain) {
        LOG.info("Client delete request. accountId={}", id);

        return persistenceService.delete(id);
    }

    @Override
    public CompletableFuture<ClientBO> activate(final long id, final String domain) {
        return getById(id, domain)
                .thenCompose(AsyncUtils::fromClientOptional)
                .thenCompose(client -> {
                    LOG.info("Activate client request. clientId={}, domain={}", client.getId(), client.getDomain());

                    ClientBO activated = client.withActive(true);

                    return update(activated, domain)
                            .thenApply(persisted -> {
                                if (persisted.isPresent()) {
                                    LOG.info("Client activated. clientId={}, domain={}", client.getId(), client.getDomain());
                                    return persisted.get();
                                }

                                LOG.info("Failed to activate client. clientId={}, domain={}", client.getId(), client.getDomain());
                                throw new ServiceNotFoundException(ErrorCode.APP_DOES_NOT_EXIST, "Client does not exist");
                            });
                });
    }

    @Override
    public CompletableFuture<ClientBO> deactivate(final long id, final String domain) {
        return getById(id, domain)
                .thenCompose(AsyncUtils::fromClientOptional)
                .thenCompose(client -> {
                    LOG.info("Deactivate client request. clientId={}, domain={}", client.getId(), client.getDomain());

                    ClientBO deactivated = client.withActive(false);

                    return update(deactivated, domain)
                            .thenApply(persisted -> {
                                if (persisted.isPresent()) {
                                    LOG.info("Client deactivated. clientId={}, domain={}", client.getId(), client.getDomain());
                                    return persisted.get();
                                }

                                LOG.info("Failed to deactivate Client. clientId={}, domain={}", client.getId(), client.getDomain());
                                throw new ServiceNotFoundException(ErrorCode.APP_DOES_NOT_EXIST, "Client does not exist");
                            });
                });
    }

    @Override
    public CompletableFuture<List<ClientBO>> getByAccountId(final long accountId, final String domain) {
        return clientsRepository.getAllForAccount(accountId)
                .thenApply(list -> list.stream().map(serviceMapper::toBO).collect(Collectors.toList()));
    }

    @Override
    public CompletableFuture<List<ClientBO>> getByDomain(final String domain) {
        return clientsRepository.getByDomain(domain)
                .thenApply(list -> list.stream().map(serviceMapper::toBO).collect(Collectors.toList()));
    }
}
