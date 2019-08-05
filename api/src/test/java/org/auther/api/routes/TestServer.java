package org.auther.api.routes;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import org.auther.service.AccountService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mockito;

import static io.javalin.apibuilder.ApiBuilder.path;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class TestServer {
    private int port;

    private AccountService accountService;

    @BeforeAll
    void setupServer() {
        final ObjectMapper mapper = new ObjectMapper();
        final AccountService accountService = Mockito.mock(AccountService.class);

        final Javalin app = Javalin.create().start();
        port = app.port();

        app.routes(() -> {
            path("/users", new UsersRoute(mapper, accountService));
        });

        this.accountService = accountService;
    }

    AccountService getAccountService() {
        return accountService;
    }

    int getPort() {
        return port;
    }
}
