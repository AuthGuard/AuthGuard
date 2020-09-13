package com.authguard.rest;

import com.authguard.api.dto.entities.AccountDTO;
import com.authguard.api.dto.entities.AccountEmailDTO;
import com.authguard.api.dto.requests.CreateAccountRequestDTO;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import com.authguard.service.AccountsService;
import com.authguard.service.model.AccountBO;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AccountsApiTest extends AbstractRouteTest {
    private static final Logger LOG = LoggerFactory.getLogger(AccountsApiTest.class);

    private static final String ENDPOINT = "accounts";

    AccountsApiTest() {
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
    void create() {
        final CreateAccountRequestDTO requestDTO = CreateAccountRequestDTO.builder()
                .externalId("external")
                .addEmails(AccountEmailDTO.builder()
                        .email("email@server.com")
                        .build())
                .build();

        final AccountBO accountBO = mapper().toBO(requestDTO);
        final AccountBO serviceResponse = accountBO.withId(UUID.randomUUID().toString());

        Mockito.when(accountsService.create(eq(accountBO))).thenReturn(serviceResponse);

        LOG.info("Request {}", requestDTO);

        final ValidatableResponse httpResponse = given().body(requestDTO)
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

        assertThat(response).isEqualToIgnoringGivenFields(requestDTO, "id", "deleted");
        assertThat(response.getId()).isEqualTo(serviceResponse.getId());
    }
}