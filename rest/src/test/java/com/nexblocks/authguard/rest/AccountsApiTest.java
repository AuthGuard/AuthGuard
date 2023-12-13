package com.nexblocks.authguard.rest;

import com.nexblocks.authguard.api.dto.entities.AccountDTO;
import com.nexblocks.authguard.api.dto.entities.AccountEmailDTO;
import com.nexblocks.authguard.api.dto.entities.UserIdentifier;
import com.nexblocks.authguard.api.dto.entities.UserIdentifierDTO;
import com.nexblocks.authguard.api.dto.requests.CreateAccountRequestDTO;
import com.nexblocks.authguard.rest.util.IdempotencyHeader;
import com.nexblocks.authguard.service.AccountsService;
import com.nexblocks.authguard.service.model.AccountBO;
import com.nexblocks.authguard.service.model.RequestContextBO;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

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
                .email(AccountEmailDTO.builder()
                        .email("email@server.com")
                        .build())
                .domain("main")
                .identifiers(Collections.singletonList(
                        UserIdentifierDTO.builder()
                                .identifier("username")
                                .type(UserIdentifier.Type.USERNAME)
                                .domain("main")
                                .build()
                ))
                .plainPassword("password")
                .build();
        final RequestContextBO requestContext = RequestContextBO.builder()
                .idempotentKey(UUID.randomUUID().toString())
                .build();

        final AccountBO accountBO = mapper().toBO(requestDTO);
        final AccountBO serviceResponse = accountBO.withId(UUID.randomUUID().getMostSignificantBits());

        Mockito.when(accountsService.create(Mockito.eq(accountBO), Mockito.any()))
                .thenReturn(serviceResponse);

        LOG.info("Request {}", requestDTO);

        final ValidatableResponse httpResponse = given().body(requestDTO)
                .contentType(ContentType.JSON)
                .header(IdempotencyHeader.HEADER_NAME, requestContext.getIdempotentKey())
                .post(url())
                .then()
                .statusCode(201)
                .contentType(ContentType.JSON);

        final AccountDTO response = httpResponse
                .extract()
                .response()
                .getBody()
                .as(AccountDTO.class);

        assertThat(response).isEqualToIgnoringGivenFields(requestDTO,
                "id", "deleted", "createdAt", "lastModified", "passwordUpdatedAt",
                "social", "identityProvider", "passwordVersion");
        assertThat(response.getId()).isEqualTo(serviceResponse.getId());
    }
}