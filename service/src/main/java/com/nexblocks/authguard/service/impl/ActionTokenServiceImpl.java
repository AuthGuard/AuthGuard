package com.nexblocks.authguard.service.impl;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.nexblocks.authguard.basic.BasicAuthProvider;
import com.nexblocks.authguard.basic.otp.OtpProvider;
import com.nexblocks.authguard.basic.otp.OtpVerifier;
import com.nexblocks.authguard.dal.cache.AccountTokensRepository;
import com.nexblocks.authguard.dal.model.AccountTokenDO;
import com.nexblocks.authguard.service.AccountsService;
import com.nexblocks.authguard.service.ActionTokenService;
import com.nexblocks.authguard.service.exceptions.ServiceException;
import com.nexblocks.authguard.service.exceptions.codes.ErrorCode;
import com.nexblocks.authguard.service.model.AccountBO;
import com.nexblocks.authguard.service.model.ActionTokenBO;
import com.nexblocks.authguard.service.model.AuthRequestBO;
import com.nexblocks.authguard.service.model.AuthResponseBO;
import com.nexblocks.authguard.service.random.CryptographicRandom;
import com.nexblocks.authguard.service.util.ID;
import io.vavr.control.Try;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

public class ActionTokenServiceImpl implements ActionTokenService {
    private static final Logger LOG = LoggerFactory.getLogger(ActionTokenServiceImpl.class);

    private static final int ACTION_TOKEN_SIZE = 128;
    private static final Duration TOKEN_LIFETIME = Duration.ofMinutes(5);

    private final AccountsService accountsService;
    private final BasicAuthProvider basicAuthProvider;
    private final OtpProvider otpProvider;
    private final OtpVerifier otpVerifier;
    private final AccountTokensRepository accountTokensRepository;
    private final CryptographicRandom cryptographicRandom;

    @Inject
    public ActionTokenServiceImpl(final AccountsService accountsService, final BasicAuthProvider basicAuthProvider,
                                  final OtpProvider otpProvider,
                                  final OtpVerifier otpVerifier,
                                  final AccountTokensRepository accountTokensRepository) {
        this.accountsService = accountsService;
        this.basicAuthProvider = basicAuthProvider;
        this.otpProvider = otpProvider;
        this.otpVerifier = otpVerifier;
        this.accountTokensRepository = accountTokensRepository;

        this.cryptographicRandom = new CryptographicRandom();
    }

    @Override
    public Try<AuthResponseBO> generateOtp(final long accountId) {
        final AccountBO account = accountsService.getById(accountId).join().orElse(null);

        if (account == null) {
            return Try.failure(new ServiceException(ErrorCode.ACCOUNT_DOES_NOT_EXIST, "Account does not exist"));
        }

        LOG.info("Generate OTP for action token request. accountId={}, domain={}", account.getId(), account.getDomain());

        return Try.success(otpProvider.generateToken(account));
    }

    @Override
    public Try<ActionTokenBO> generateFromBasicAuth(final AuthRequestBO authRequest, final String action) {
        final AccountBO account = basicAuthProvider.getAccount(authRequest);
        final AccountTokenDO token = generateToken(account, action);

        LOG.info("Action token from credentials request. accountId={}, domain={}, tokenId={}, expiresAt={}",
                account.getId(), account.getDomain(), token.getId(), token.getExpiresAt());

        return Try.success(ActionTokenBO.builder()
                .accountId(account.getId())
                .token(token.getToken())
                .validFor(TOKEN_LIFETIME.toSeconds())
                .build());
    }

    @Override
    public Try<ActionTokenBO> generateFromOtp(final long passwordId, final String otp, final String action) {
        String otpToken = passwordId + ":" + otp;
        Optional<AccountBO> otpResult = otpVerifier.verifyAccountTokenAsync(otpToken)
                .thenCompose(accountsService::getById)
                .join();

        if (otpResult.isEmpty()) {
            LOG.error("Verified OTP request but the account doesn't exist. passwordId={}", passwordId);
            return Try.failure(new ServiceException(ErrorCode.ACCOUNT_DOES_NOT_EXIST,
                    "The account associated with that OTP no longer exists"));
        }

        AccountBO account = otpResult.get();

        AccountTokenDO token = generateToken(account, action);
        LOG.info("Generated action token from OTP request. passwordId={}, tokenId={}, expiresAt={}",
                passwordId, token.getId(), token.getExpiresAt());

        return Try.success(ActionTokenBO.builder()
                .accountId(account.getId())
                .token(token.getToken())
                .validFor(TOKEN_LIFETIME.toSeconds())
                .build());
    }

    @Override
    public Try<ActionTokenBO> verifyToken(final String token, final String action) {
        Optional<AccountTokenDO> persisted = accountTokensRepository.getByToken(token).join();

        if (persisted.isEmpty()) {
            return Try.failure(new ServiceException(ErrorCode.TOKEN_EXPIRED_OR_DOES_NOT_EXIST, "Token was not found"));
        }

        Instant now = Instant.now();

        if (persisted.get().getExpiresAt().isBefore(now)) {
            return Try.failure(new ServiceException(ErrorCode.EXPIRED_TOKEN, "Token has expired"));
        }

        String allowedAction = persisted.get().getAdditionalInformation().get("action");

        if (allowedAction == null || !allowedAction.equals(action)) {
            return Try.failure(new ServiceException(ErrorCode.INVALID_TOKEN, "Token was created for a different action"));
        }

        LOG.info("Action token verified. tokenId={}, action={}", persisted.get().getId(), action);

        return Try.success(ActionTokenBO.builder()
                .accountId(persisted.get().getAssociatedAccountId())
                .token(token)
                .action(action)
                .build());
    }

    private AccountTokenDO generateToken(final AccountBO account, final String action) {
        Instant now = Instant.now();

        AccountTokenDO accountToken = AccountTokenDO
                .builder()
                .id(ID.generate())
                .createdAt(now)
                .token(cryptographicRandom.base64Url(ACTION_TOKEN_SIZE))
                .associatedAccountId(account.getId())
                .additionalInformation(ImmutableMap.of("action", action))
                .expiresAt(now.plus(TOKEN_LIFETIME))
                .build();

        return accountTokensRepository.save(accountToken).join();
    }
}
