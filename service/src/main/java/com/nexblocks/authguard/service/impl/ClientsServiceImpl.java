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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
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
    public ClientBO create(final ClientBO client, final RequestContextBO requestContext) {
        return idempotencyService.performOperation(() -> doCreate(client), requestContext.getIdempotentKey(), client.getEntityType())
                .join();
    }

    private ClientBO doCreate(final ClientBO client) {
        if (client.getAccountId() != null && accountsService.getById(client.getAccountId()).isEmpty()) {
            throw new ServiceNotFoundException(ErrorCode.ACCOUNT_DOES_NOT_EXIST, "No account with ID " + client.getAccountId() + " exists");
        }

        return persistenceService.create(client);
    }

    @Override
    public Optional<ClientBO> getById(final long id) {
        return persistenceService.getById(id);
    }

    @Override
    public Optional<ClientBO> getByExternalId(final String externalId) {
        return clientsRepository.getByExternalId(externalId)
                .thenApply(optional -> optional.map(serviceMapper::toBO))
                .join();
    }

    @Override
    public Optional<ClientBO> update(final ClientBO client) {
        LOG.info("Client update request. accountId={}", client.getId());

        // FIXME accountId cannot be updated
        return persistenceService.update(client);
    }

    @Override
    public Optional<ClientBO> delete(final long id) {
        LOG.info("Client delete request. accountId={}", id);

        return persistenceService.delete(id);
    }

    @Override
    public Optional<ClientBO> activate(final long id) {
        return getById(id)
                .flatMap(client -> {
                    LOG.info("Activate client request. clientId={}, domain={}", client.getId(), client.getDomain());

                    ClientBO activated = client.withActive(true);
                    Optional<ClientBO> persisted = this.update(activated);

                    if (persisted.isPresent()) {
                        LOG.info("Client activated. clientId={}, domain={}", client.getId(), client.getDomain());
                    } else {
                        LOG.info("Failed to activate client. clientId={}, domain={}", client.getId(), client.getDomain());
                    }

                    return persisted;
                });
    }

    @Override
    public Optional<ClientBO> deactivate(final long id) {
        return getById(id)
                .flatMap(client -> {
                    LOG.info("Deactivate client request. clientId={}, domain={}", client.getId(), client.getDomain());

                    ClientBO deactivated = client.withActive(false);
                    Optional<ClientBO> persisted = this.update(deactivated);

                    if (persisted.isPresent()) {
                        LOG.info("Client deactivated. clientId={}, domain={}", client.getId(), client.getDomain());
                    } else {
                        LOG.info("Failed to deactivate Client. clientId={}, domain={}", client.getId(), client.getDomain());
                    }

                    return persisted;
                });
    }

    @Override
    public List<ClientBO> getByAccountId(final long accountId) {
        return clientsRepository.getAllForAccount(accountId)
                .thenApply(list -> list.stream().map(serviceMapper::toBO).collect(Collectors.toList()))
                .join();
    }
}
