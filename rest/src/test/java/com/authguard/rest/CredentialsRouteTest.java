package com.authguard.rest;

import com.authguard.api.dto.entities.CredentialsDTO;
import com.authguard.api.dto.requests.CreateCredentialsRequestDTO;
import com.authguard.service.CredentialsService;
import com.authguard.service.model.CredentialsBO;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mockito;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CredentialsRouteTest extends AbstractRouteTest {
    private static final String ENDPOINT = "credentials";

    CredentialsRouteTest() {
        super(ENDPOINT);
    }

    private CredentialsService credentialsService;

    @BeforeAll
    void setup() {
        credentialsService = mockService(CredentialsService.class);
    }

    @BeforeEach
    void reset() {
        Mockito.reset(credentialsService);
    }

    @Test
    void create() {
        final CreateCredentialsRequestDTO credentialsRequest = randomObject(CreateCredentialsRequestDTO.class);
        final CredentialsBO credentialsBO = mapper().toBO(credentialsRequest);
        final CredentialsBO serviceResponse = credentialsBO
                .withPlainPassword(null)
                .withId(UUID.randomUUID().toString());

        Mockito.when(credentialsService.create(Mockito.eq(credentialsBO), Mockito.any())).thenReturn(serviceResponse);

        final ValidatableResponse httpResponse = given().body(credentialsRequest)
                .contentType(ContentType.JSON)
                .header("X-IdempotentKey", "key")
                .post(url())
                .then()
                .statusCode(201)
                .contentType(ContentType.JSON);

        final CredentialsDTO responseBody = httpResponse
                .extract()
                .response()
                .getBody()
                .as(CredentialsDTO.class);

        assertThat(responseBody).isEqualToIgnoringGivenFields(credentialsRequest, "id", "plainPassword");
        assertThat(responseBody.getPlainPassword()).isNull();
        assertThat(responseBody.getId()).isEqualTo(serviceResponse.getId());
    }
}
