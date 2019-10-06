package org.auther.api;

import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import org.auther.api.dto.CredentialsDTO;
import org.auther.service.CredentialsService;
import org.auther.service.model.CredentialsBO;
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
        final CredentialsDTO credentialsDTO = randomObject(CredentialsDTO.class);
        final CredentialsBO credentialsBO = mapper().toBO(credentialsDTO);
        final CredentialsBO serviceResponse = credentialsBO
                .withPlainPassword(null)
                .withId(UUID.randomUUID().toString());

        Mockito.when(credentialsService.create(credentialsBO)).thenReturn(serviceResponse);

        final ValidatableResponse httpResponse = given().body(credentialsDTO)
                .contentType(ContentType.JSON)
                .post(url())
                .then()
                .statusCode(201)
                .contentType(ContentType.JSON);

        final CredentialsDTO responseBody = httpResponse
                .extract()
                .response()
                .getBody()
                .as(CredentialsDTO.class);

        assertThat(responseBody).isEqualToIgnoringGivenFields(credentialsDTO, "id", "plainPassword");
        assertThat(responseBody.getPlainPassword()).isNull();
        assertThat(responseBody.getId()).isEqualTo(serviceResponse.getId());
    }
}
