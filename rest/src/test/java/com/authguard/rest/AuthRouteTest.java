package com.authguard.rest;

import com.authguard.api.dto.entities.TokensDTO;
import com.authguard.api.dto.requests.AuthRequestDTO;
import com.authguard.rest.mappers.RestMapper;
import com.authguard.rest.mappers.RestMapperImpl;
import com.authguard.service.AuthenticationService;
import com.authguard.service.model.AuthRequestBO;
import com.authguard.service.model.TokensBO;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
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

    private RestMapper restMapper;
    private AuthenticationService authenticationService;

    @BeforeAll
    void setup() {
        restMapper = new RestMapperImpl();
        authenticationService = mockService(AuthenticationService.class);
    }

    @AfterEach
    void reset() {
        Mockito.reset(authenticationService);
    }

    @Test
    void authenticate() {
        final AuthRequestDTO requestDTO = randomObject(AuthRequestDTO.class);
        final AuthRequestBO requestBO = restMapper.toBO(requestDTO);
        final TokensBO tokensBO = TokensBO.builder()
                .token("token")
                .build();
        final TokensDTO tokensDTO = mapper().toDTO(tokensBO);

        Mockito.when(authenticationService.authenticate(requestBO)).thenReturn(Optional.of(tokensBO));

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
    void authenticateUnsuccessful() {
        final AuthRequestDTO requestDTO = randomObject(AuthRequestDTO.class);
        final AuthRequestBO requestBO = restMapper.toBO(requestDTO);

        Mockito.when(authenticationService.authenticate(requestBO)).thenReturn(Optional.empty());

        given()
                .body(requestDTO)
                .post(url("authenticate"))
                .then()
                .statusCode(400);
    }

    @Test
    void getExchangeAttemptsNoEntityId() {
        given().get(url("exchange", "attempts"))
                .then()
                .statusCode(400);
    }

    @Test
    void getExchangeAttemptsWithExchangeWithoutTimestamp() {
        given().get(url("exchange", "attempts?entityId=entity&fromExchange=basic"))
                .then()
                .statusCode(400);
    }
}
