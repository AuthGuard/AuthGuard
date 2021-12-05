package com.nexblocks.authguard.rest;

import com.nexblocks.authguard.api.dto.entities.AccountDTO;
import com.nexblocks.authguard.api.dto.entities.AccountEmailDTO;
import com.nexblocks.authguard.api.dto.entities.UserIdentifier;
import com.nexblocks.authguard.api.dto.entities.UserIdentifierDTO;
import com.nexblocks.authguard.api.dto.requests.CreateAccountRequestDTO;
import com.nexblocks.authguard.api.dto.requests.CreateCompleteAccountRequestDTO;
import com.nexblocks.authguard.api.dto.requests.CreateCompleteAccountResponseDTO;
import com.nexblocks.authguard.api.dto.requests.CreateCredentialsRequestDTO;
import com.nexblocks.authguard.rest.util.IdempotencyHeader;
import com.nexblocks.authguard.service.AccountsService;
import com.nexblocks.authguard.service.CredentialsService;
import com.nexblocks.authguard.service.exceptions.IdempotencyException;
import com.nexblocks.authguard.service.model.AccountBO;
import com.nexblocks.authguard.service.model.CredentialsBO;
import com.nexblocks.authguard.service.model.IdempotentRecordBO;
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

import java.util.UUID;
import java.util.concurrent.CompletionException;

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
    private CredentialsService credentialsService;

    @BeforeAll
    void setup() {
        accountsService = mockService(AccountsService.class);
        credentialsService = mockService(CredentialsService.class);
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
                .build();
        final RequestContextBO requestContext = RequestContextBO.builder()
                .idempotentKey(UUID.randomUUID().toString())
                .build();

        final AccountBO accountBO = mapper().toBO(requestDTO);
        final AccountBO serviceResponse = accountBO.withId(UUID.randomUUID().toString());

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
                "id", "deleted", "createdAt", "lastModified", "social", "identityProvider");
        assertThat(response.getId()).isEqualTo(serviceResponse.getId());
    }

    @Test
    void createWithCredentials() {
        final CreateAccountRequestDTO accountRequest = CreateAccountRequestDTO.builder()
                .externalId("external")
                .email(AccountEmailDTO.builder()
                        .email("email@server.com")
                        .build())
                .build();

        final CreateCredentialsRequestDTO credentialsRequest = CreateCredentialsRequestDTO.builder()
                .plainPassword("password")
                .addIdentifiers(UserIdentifierDTO.builder()
                        .identifier("username")
                        .type(UserIdentifier.Type.USERNAME)
                        .build())
                .build();

        final CreateCompleteAccountRequestDTO completeRequest = CreateCompleteAccountRequestDTO.builder()
                .account(accountRequest)
                .credentials(credentialsRequest)
                .build();

        final RequestContextBO requestContext = RequestContextBO.builder()
                .idempotentKey(UUID.randomUUID().toString())
                .build();

        final AccountBO accountBO = mapper().toBO(accountRequest);
        final AccountBO accountResponse = accountBO.withId(UUID.randomUUID().toString());

        final CredentialsBO credentialsBO = mapper().toBO(credentialsRequest).withAccountId(accountResponse.getId());
        final CredentialsBO credentialsResponse = credentialsBO.withId(UUID.randomUUID().toString());

        Mockito.when(accountsService.create(Mockito.eq(accountBO), Mockito.any()))
                .thenReturn(accountResponse);
        Mockito.when(credentialsService.create(Mockito.eq(credentialsBO), Mockito.any()))
                .thenReturn(credentialsResponse);

        LOG.info("Request {}", accountRequest);

        final ValidatableResponse httpResponse = given().body(completeRequest)
                .contentType(ContentType.JSON)
                .header(IdempotencyHeader.HEADER_NAME, requestContext.getIdempotentKey())
                .post(url("complete"))
                .then()
                .statusCode(201)
                .contentType(ContentType.JSON);

        final CreateCompleteAccountResponseDTO response = httpResponse
                .extract()
                .response()
                .getBody()
                .as(CreateCompleteAccountResponseDTO.class);

        assertThat(response.getAccountId()).isEqualTo(accountResponse.getId());
        assertThat(response.getCredentialsId()).isEqualTo(credentialsResponse.getId());
    }

    @Test
    void createWithCredentialsAccountExists() {
        final CreateAccountRequestDTO accountRequest = CreateAccountRequestDTO.builder()
                .externalId("external")
                .email(AccountEmailDTO.builder()
                        .email("email@server.com")
                        .build())
                .build();

        final CreateCredentialsRequestDTO credentialsRequest = CreateCredentialsRequestDTO.builder()
                .plainPassword("password")
                .addIdentifiers(UserIdentifierDTO.builder()
                        .identifier("username")
                        .type(UserIdentifier.Type.USERNAME)
                        .build())
                .build();

        final CreateCompleteAccountRequestDTO completeRequest = CreateCompleteAccountRequestDTO.builder()
                .account(accountRequest)
                .credentials(credentialsRequest)
                .build();

        final RequestContextBO requestContext = RequestContextBO.builder()
                .idempotentKey(UUID.randomUUID().toString())
                .build();

        final AccountBO accountBO = mapper().toBO(accountRequest);
        final AccountBO accountResponse = accountBO.withId(UUID.randomUUID().toString());

        final CredentialsBO credentialsBO = mapper().toBO(credentialsRequest).withAccountId(accountResponse.getId());
        final CredentialsBO credentialsResponse = credentialsBO.withId(UUID.randomUUID().toString());

        Mockito.when(accountsService.create(Mockito.eq(accountBO), Mockito.any()))
                .thenThrow(new CompletionException(new IdempotencyException(IdempotentRecordBO.builder()
                        .entityId(accountResponse.getId())
                        .build())));
        Mockito.when(credentialsService.create(Mockito.eq(credentialsBO), Mockito.any()))
                .thenReturn(credentialsResponse);

        LOG.info("Request {}", accountRequest);

        final ValidatableResponse httpResponse = given().body(completeRequest)
                .contentType(ContentType.JSON)
                .header(IdempotencyHeader.HEADER_NAME, requestContext.getIdempotentKey())
                .post(url("complete"))
                .then()
                .statusCode(201)
                .contentType(ContentType.JSON);

        final CreateCompleteAccountResponseDTO response = httpResponse
                .extract()
                .response()
                .getBody()
                .as(CreateCompleteAccountResponseDTO.class);

        assertThat(response.getAccountId()).isEqualTo(accountResponse.getId());
        assertThat(response.getCredentialsId()).isEqualTo(credentialsResponse.getId());
    }

    @Test
    void createWithCredentialsAllExist() {
        final CreateAccountRequestDTO accountRequest = CreateAccountRequestDTO.builder()
                .externalId("external")
                .email(AccountEmailDTO.builder()
                        .email("email@server.com")
                        .build())
                .build();

        final CreateCredentialsRequestDTO credentialsRequest = CreateCredentialsRequestDTO.builder()
                .plainPassword("password")
                .addIdentifiers(UserIdentifierDTO.builder()
                        .identifier("username")
                        .type(UserIdentifier.Type.USERNAME)
                        .build())
                .build();

        final CreateCompleteAccountRequestDTO completeRequest = CreateCompleteAccountRequestDTO.builder()
                .account(accountRequest)
                .credentials(credentialsRequest)
                .build();

        final RequestContextBO requestContext = RequestContextBO.builder()
                .idempotentKey(UUID.randomUUID().toString())
                .build();

        final AccountBO accountBO = mapper().toBO(accountRequest);
        final AccountBO accountResponse = accountBO.withId(UUID.randomUUID().toString());

        final CredentialsBO credentialsBO = mapper().toBO(credentialsRequest).withAccountId(accountResponse.getId());
        final CredentialsBO credentialsResponse = credentialsBO.withId(UUID.randomUUID().toString());

        Mockito.when(accountsService.create(Mockito.eq(accountBO), Mockito.any()))
                .thenThrow(new CompletionException(new IdempotencyException(IdempotentRecordBO.builder()
                        .entityId(accountResponse.getId())
                        .build())));
        Mockito.when(credentialsService.create(Mockito.eq(credentialsBO), Mockito.any()))
                .thenThrow(new CompletionException(new IdempotencyException(IdempotentRecordBO.builder()
                        .entityId(credentialsResponse.getId())
                        .build())));

        LOG.info("Request {}", accountRequest);

        final ValidatableResponse httpResponse = given().body(completeRequest)
                .contentType(ContentType.JSON)
                .header(IdempotencyHeader.HEADER_NAME, requestContext.getIdempotentKey())
                .post(url("complete"))
                .then()
                .statusCode(201)
                .contentType(ContentType.JSON);

        final CreateCompleteAccountResponseDTO response = httpResponse
                .extract()
                .response()
                .getBody()
                .as(CreateCompleteAccountResponseDTO.class);

        assertThat(response.getAccountId()).isEqualTo(accountResponse.getId());
        assertThat(response.getCredentialsId()).isEqualTo(credentialsResponse.getId());
    }
}