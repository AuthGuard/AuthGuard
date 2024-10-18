package com.nexblocks.authguard.basic.totp;

import com.nexblocks.authguard.config.ConfigContext;
import com.nexblocks.authguard.dal.cache.AccountTokensRepository;
import com.nexblocks.authguard.dal.model.AccountTokenDO;
import com.nexblocks.authguard.service.TotpKeysService;
import com.nexblocks.authguard.service.config.AuthenticatorConfig;
import com.nexblocks.authguard.service.config.TotpAuthenticatorsConfig;
import com.nexblocks.authguard.service.exceptions.ServiceException;
import com.nexblocks.authguard.service.exceptions.codes.ErrorCode;
import com.nexblocks.authguard.service.model.TotpKeyBO;
import de.taimos.totp.TOTP;
import org.apache.commons.codec.binary.Hex;
import org.bouncycastle.util.encoders.Base32;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TotpVerifierTest {
    private static final String KEY = "TMK73USREMOEMLNZO3HM6BGRP7G5LWLM";

    private AccountTokensRepository accountTokensRepository;
    private TotpKeysService totpKeysService;

    private TotpVerifier totpVerifier;

    @BeforeEach
    void setup() {
        accountTokensRepository = Mockito.mock(AccountTokensRepository.class);
        totpKeysService = Mockito.mock(TotpKeysService.class);

        ConfigContext configContext = Mockito.mock(ConfigContext.class);

        Mockito.when(configContext.asConfigBean(TotpAuthenticatorsConfig.class))
                .thenReturn(TotpAuthenticatorsConfig.builder()
                        .addCustomAuthenticators(AuthenticatorConfig.builder()
                                .name("custom")
                                .timeStep(60)
                                .build())
                        .build());

        totpVerifier = new TotpVerifier(accountTokensRepository, totpKeysService, configContext);
    }

    @Test
    void verify() {
        long accountId = 1;
        String domain = "test";
        AccountTokenDO accountToken = AccountTokenDO.builder()
                .id(2)
                .associatedAccountId(accountId)
                .domain("test")
                .token("token")
                .expiresAt(Instant.now().plusSeconds(1))
                .build();

        TotpKeyBO totpKey = TotpKeyBO.builder()
                .key(Base32.decode(KEY))
                .build();

        String totp = getTOTPCode();

        Mockito.when(totpKeysService.getByAccountIdDecrypted(accountId, domain))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(totpKey)));

        Mockito.when(accountTokensRepository.getByToken(accountToken.getToken()))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(accountToken)));

        AccountTokenDO actual = totpVerifier.verifyAndGetAccountTokenAsync(accountToken.getToken() + ":" + totp).join();

        assertThat(actual).isEqualTo(accountToken);
    }

    @Test
    void verifyExpiredToken() {
        long accountId = 1;
        AccountTokenDO accountToken = AccountTokenDO.builder()
                .id(2)
                .associatedAccountId(accountId)
                .domain("test")
                .token("token")
                .expiresAt(Instant.now().minusSeconds(1))
                .build();

        Mockito.when(accountTokensRepository.getByToken(accountToken.getToken()))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(accountToken)));

        String totp = getTOTPCode();

        CompletableFuture<AccountTokenDO> future =
                totpVerifier.verifyAndGetAccountTokenAsync(accountToken.getToken() + ":" + totp);

        assertThatThrownBy(future::join)
                .hasCauseInstanceOf(ServiceException.class)
                .cause()
                .extracting("errorCode")
                .isEqualTo(ErrorCode.EXPIRED_TOKEN.getCode());
    }

    @Test
    void verifyInvalidFormat() {
        CompletableFuture<AccountTokenDO> future =
                totpVerifier.verifyAndGetAccountTokenAsync("invalid");

        assertThatThrownBy(future::join)
                .hasCauseInstanceOf(ServiceException.class)
                .cause()
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_TOKEN.getCode());
    }

    @Test
    void verifyWrongTotp() {
        long accountId = 1;
        String domain = "test";
        AccountTokenDO accountToken = AccountTokenDO.builder()
                .id(2)
                .associatedAccountId(accountId)
                .domain("test")
                .token("token")
                .expiresAt(Instant.now().plusSeconds(1))
                .build();

        TotpKeyBO totpKey = TotpKeyBO.builder()
                .key(Base32.decode(KEY))
                .build();

        Mockito.when(totpKeysService.getByAccountIdDecrypted(accountId, domain))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(totpKey)));

        Mockito.when(accountTokensRepository.getByToken(accountToken.getToken()))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(accountToken)));

        CompletableFuture<AccountTokenDO> future =
                totpVerifier.verifyAndGetAccountTokenAsync(accountToken.getToken() + ":never");

        assertThatThrownBy(future::join)
                .hasCauseInstanceOf(ServiceException.class)
                .cause()
                .extracting("errorCode")
                .isEqualTo(ErrorCode.TOTP_INVALID.getCode());

    }

    private String getTOTPCode() {
        byte[] bytes = Base32.decode(KEY);
        String hexKey = Hex.encodeHexString(bytes);
        return TOTP.getOTP(hexKey);
    }
}