package com.nexblocks.authguard.rest;

import com.nexblocks.authguard.api.dto.entities.AuthResponseDTO;
import com.nexblocks.authguard.api.dto.requests.AuthRequestDTO;
import com.nexblocks.authguard.rest.mappers.RestMapper;
import com.nexblocks.authguard.rest.mappers.RestMapperImpl;
import com.nexblocks.authguard.service.AuthenticationService;
import com.nexblocks.authguard.service.model.AuthRequestBO;
import com.nexblocks.authguard.service.model.AuthResponseBO;
import com.nexblocks.authguard.service.model.RequestContextBO;
import io.restassured.http.ContentType;
import io.restassured.http.Header;
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
        final AuthRequestBO requestBO = restMapper.toBO(requestDTO)
                .withClientId("valid-test-app");
        final AuthResponseBO tokensBO = AuthResponseBO.builder()
                .token("token")
                .build();
        final AuthResponseDTO tokensDTO = mapper().toDTO(tokensBO);

        Mockito.when(authenticationService.authenticate(Mockito.eq(requestBO), Mockito.any()))
                .thenReturn(Optional.of(tokensBO));

        final ValidatableResponse httpResponse = given()
                .body(requestDTO)
                .post(url("authenticate"))
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON);

        final AuthResponseDTO responseBody = httpResponse.extract()
                .response()
                .body()
                .as(AuthResponseDTO.class);

        assertThat(responseBody).isEqualTo(tokensDTO);
    }

    @Test
    void authenticateAuthClient() {
        final AuthRequestDTO requestDTO = randomObject(AuthRequestDTO.class);
        final AuthRequestBO requestBO = restMapper.toBO(requestDTO)
                .withClientId("valid-test-app");
        final AuthResponseBO tokensBO = AuthResponseBO.builder()
                .token("token")
                .build();
        final AuthResponseDTO tokensDTO = mapper().toDTO(tokensBO);

        Mockito.when(authenticationService.authenticate(Mockito.eq(requestBO), Mockito.any()))
                .thenReturn(Optional.of(tokensBO));

        final ValidatableResponse httpResponse = given()
                .body(requestDTO)
                .post(url("authenticate"))
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON);

        final AuthResponseDTO responseBody = httpResponse.extract()
                .response()
                .body()
                .as(AuthResponseDTO.class);

        assertThat(responseBody).isEqualTo(tokensDTO);
    }

    @Test
    void authenticateAuthClientWithViolations() {
        final AuthRequestDTO requestDTO = AuthRequestDTO.builder()
                .identifier("identifier")
                .domain("test")
                .sourceIp("ip")
                .userAgent("browser")
                .build();

        given()
                .header(new Header("Authorization", "auth-client"))
                .body(requestDTO)
                .post(url("authenticate"))
                .then()
                .statusCode(403)
                .contentType(ContentType.JSON);
    }

    @Test
    void authenticateAuthClientWithWrongDomain() {
        final AuthRequestDTO requestDTO = AuthRequestDTO.builder()
                .identifier("identifier")
                .domain("else")
                .sourceIp("ip")
                .userAgent("browser")
                .build();

        given()
                .header(new Header("Authorization", "auth-client"))
                .body(requestDTO)
                .post(url("authenticate"))
                .then()
                .statusCode(403)
                .contentType(ContentType.JSON);
    }

    @Test
    void authenticateUnsuccessful() {
        final AuthRequestDTO requestDTO = randomObject(AuthRequestDTO.class);
        final AuthRequestBO requestBO = restMapper.toBO(requestDTO);
        final RequestContextBO requestContext = RequestContextBO.builder().build();

        Mockito.when(authenticationService.authenticate(requestBO, requestContext)).thenReturn(Optional.empty());

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
