package org.auther.api.routes;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import org.auther.service.AccountsService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mockito;

import static io.javalin.apibuilder.ApiBuilder.path;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class TestServer {
    private int port;

    private AccountsService accountsService;

    @BeforeAll
    void setupServer() {
        final AccountsService accountsService = Mockito.mock(AccountsService.class);

        final Javalin app = Javalin.create().start();
        port = app.port();

        app.routes(() -> {
            path("/users", new UsersRoute(accountsService));
        });

        this.accountsService = accountsService;
    }

    AccountsService getAccountsService() {
        return accountsService;
    }

    int getPort() {
        return port;
    }
}
