package com.nexblocks.authguard.rest;

import com.nexblocks.authguard.api.dto.entities.AuthResponseDTO;
import com.nexblocks.authguard.api.dto.requests.AuthRequestDTO;
import com.nexblocks.authguard.rest.mappers.RestMapper;
import com.nexblocks.authguard.rest.mappers.RestMapperImpl;
import com.nexblocks.authguard.service.AuthenticationService;
import com.nexblocks.authguard.service.exceptions.ServiceAuthorizationException;
import com.nexblocks.authguard.service.exceptions.codes.ErrorCode;
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

import io.smallrye.mutiny.Uni;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AuthRouteTest extends AbstractRouteTest {
    private static String ENDPOINT = "domains/main/auth";

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
        AuthRequestDTO requestDTO = randomObject(AuthRequestDTO.class);
        AuthRequestBO requestBO = restMapper.toBO(requestDTO)
                .withClientId("201");
        AuthResponseBO tokensBO = AuthResponseBO.builder()
                .token("token")
                .build();
        AuthResponseDTO tokensDTO = mapper().toDTO(tokensBO);

        Mockito.when(authenticationService.authenticate(Mockito.eq(requestBO), Mockito.any()))
                .thenReturn(Uni.createFrom().item(tokensBO));

        ValidatableResponse httpResponse = given()
                .body(requestDTO)
                .post(url("authenticate"))
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON);

        AuthResponseDTO responseBody = httpResponse.extract()
                .response()
                .body()
                .as(AuthResponseDTO.class);

        assertThat(responseBody).isEqualTo(tokensDTO);
    }

    @Test
    void authenticateAuthClient() {
        AuthRequestDTO requestDTO = randomObject(AuthRequestDTO.class);
        AuthRequestBO requestBO = restMapper.toBO(requestDTO)
                .withClientId("201");
        AuthResponseBO tokensBO = AuthResponseBO.builder()
                .token("token")
                .build();
        AuthResponseDTO tokensDTO = mapper().toDTO(tokensBO);

        Mockito.when(authenticationService.authenticate(Mockito.eq(requestBO), Mockito.any()))
                .thenReturn(Uni.createFrom().item(tokensBO));

        ValidatableResponse httpResponse = given()
                .body(requestDTO)
                .post(url("authenticate"))
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON);

        AuthResponseDTO responseBody = httpResponse.extract()
                .response()
                .body()
                .as(AuthResponseDTO.class);

        assertThat(responseBody).isEqualTo(tokensDTO);
    }

    @Test
    void authenticateAuthClientWithViolations() {
        AuthRequestDTO requestDTO = AuthRequestDTO.builder()
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
        AuthRequestDTO requestDTO = AuthRequestDTO.builder()
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
        AuthRequestDTO requestDTO = randomObject(AuthRequestDTO.class);
        AuthRequestBO requestBO = restMapper.toBO(requestDTO)
                .withClientId("201");

        Mockito.when(authenticationService.authenticate(Mockito.eq(requestBO), Mockito.any()))
                .thenReturn(Uni.createFrom().failure(new ServiceAuthorizationException(ErrorCode.GENERIC_AUTH_FAILURE, "")));

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
