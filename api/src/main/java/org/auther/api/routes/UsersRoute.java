package org.auther.api.routes;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.apibuilder.EndpointGroup;
import io.javalin.http.Context;
import org.auther.api.dto.AccountDTO;
import org.auther.api.dto.AuthenticationRequestDTO;
import org.auther.api.dto.TokensDTO;
import org.auther.service.AccountService;

import java.io.IOException;
import java.util.Optional;

import static io.javalin.apibuilder.ApiBuilder.post;

public class UsersRoute implements EndpointGroup {
    private final ObjectMapper mapper;
    private final AccountService accountService;
    private final RestMapper restMapper;

    public UsersRoute(final ObjectMapper mapper, final AccountService accountService) {
        this.mapper = mapper;
        this.accountService = accountService;
        restMapper = RestMapper.INSTANCE;
    }

    public void addEndpoints() {
        post("/", this::create);
        post("/authenticate", this::authenticate);
    }

    private void create(final Context context) throws IOException {
        final AccountDTO account = mapper.readValue(context.body(), AccountDTO.class);

        final Optional<AccountDTO> createdAccount = Optional.of(restMapper.toBO(account))
                .map(accountService::create)
                .map(restMapper::toDTO);

        if (createdAccount.isPresent()) {
            context.json(createdAccount.get());
        } else {
            context.status(400).result("Failed to create account");
        }
    }

    private void authenticate(final Context context) {
        final AuthenticationRequestDTO authenticationRequest = context.bodyAsClass(AuthenticationRequestDTO.class);

        final Optional<TokensDTO> tokens = accountService.authenticate(authenticationRequest.getAuthorization())
                .map(restMapper::toDTO);

        if (tokens.isPresent()) {
            context.json(tokens.get());
        } else {
            context.status(400).result("Failed to authenticate user");
        }
    }
}
