package com.authguard.rest;

import com.authguard.api.dto.AuthRequestDTO;
import com.authguard.api.dto.TokensDTO;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import com.authguard.service.AuthenticationService;
import com.authguard.service.model.TokensBO;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;

import java.util.Optional;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AuthRouteTest extends AbstractRouteTest {
    private static final String ENDPOINT = "auth";

    AuthRouteTest() {
        super(ENDPOINT);
    }

    private AuthenticationService authenticationService;

    @BeforeAll
    void setup() {
        authenticationService = mockService(AuthenticationService.class);
    }

    @AfterEach
    void reset() {
        Mockito.reset(authenticationService);
    }

    @Test
    @Disabled
    void authenticate() {
        final AuthRequestDTO requestDTO = randomObject(AuthRequestDTO.class);
        final TokensBO tokensBO = randomObject(TokensBO.class);
        final TokensDTO tokensDTO = mapper().toDTO(tokensBO);

        Mockito.when(authenticationService.authenticate(requestDTO.getAuthorization())).thenReturn(Optional.of(tokensBO));

        final ValidatableResponse httpResponse = given()
                .body(requestDTO)
                .post(url("authenticate"))
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON);

        final TokensDTO responseBody = httpResponse.extract()
                .response()
                .body()
                .as(TokensDTO.class);

        assertThat(responseBody).isEqualTo(tokensDTO);
    }

    @Test
    @Disabled
    void authenticateUnsuccessful() {
        final AuthRequestDTO requestDTO = randomObject(AuthRequestDTO.class);

        Mockito.when(authenticationService.authenticate(requestDTO.getAuthorization())).thenReturn(Optional.empty());

        given()
                .body(requestDTO)
                .post(url("authenticate"))
                .then()
                .statusCode(400);
    }
}
