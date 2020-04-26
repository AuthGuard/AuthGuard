package com.authguard.rest;

import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import com.authguard.rest.dto.AccountDTO;
import com.authguard.service.AccountsService;
import com.authguard.service.model.AccountBO;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AccountsRouteTest extends AbstractRouteTest {
    private static final String ENDPOINT = "accounts";

    AccountsRouteTest() {
        super(ENDPOINT);
    }

    private AccountsService accountsService;

    @BeforeAll
    void setup() {
        accountsService = mockService(AccountsService.class);
    }

    @BeforeEach
    void reset() {
        Mockito.reset(accountsService);
    }

    @Test
    @Disabled
    void create() {
        final AccountDTO accountDTO = randomObject(AccountDTO.class);
        final AccountBO accountBO = mapper().toBO(accountDTO);
        final AccountBO serviceResponse = accountBO.withId(UUID.randomUUID().toString());

        Mockito.when(accountsService.create(eq(accountBO))).thenReturn(serviceResponse);

        final ValidatableResponse httpResponse = given().body(accountDTO)
                .contentType(ContentType.JSON)
                .post(url())
                .then()
                .statusCode(201)
                .contentType(ContentType.JSON);

        final AccountDTO response = httpResponse
                .extract()
                .response()
                .getBody()
                .as(AccountDTO.class);

        assertThat(response).isEqualToIgnoringGivenFields(accountDTO, "id", "plainPassword");
        assertThat(response.getId()).isEqualTo(serviceResponse.getId());
    }
}