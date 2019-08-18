package org.auther.api.routes;

import io.restassured.http.ContentType;
import org.auther.api.dto.AccountDTO;
import org.auther.service.model.AccountBO;
import org.jeasy.random.EasyRandom;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mockito;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UsersRouteTest extends TestServer {
    private static final EasyRandom RANDOM = new EasyRandom();
    private static final String ENDPOINT = "users";

    private String url() {
        return String.format("http://localhost:%d/%s", getPort(), ENDPOINT);
    }

    @Test
    void create() {
        final AccountDTO accountDTO = RANDOM.nextObject(AccountDTO.class);
        final AccountBO accountBO = new RestMapperImpl().toBO(accountDTO);

        Mockito.when(getAccountsService().create(eq(accountBO))).thenReturn(accountBO
                .withId(UUID.randomUUID().toString())
                .withPlainPassword(null)
                .withHashedPassword(null)
        );

        final AccountDTO response = given().body(accountDTO)
                .contentType(ContentType.JSON)
                .post(url())
                .getBody()
                .as(AccountDTO.class);

        assertThat(response).isEqualToIgnoringGivenFields(accountDTO, "id", "plainPassword");
    }
}