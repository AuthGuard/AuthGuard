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
import com.nexblocks.authguard.service.model.*;
import com.nexblocks.authguard.service.random.CryptographicRandom;
import com.nexblocks.authguard.service.util.AsyncUtils;
import com.nexblocks.authguard.service.util.ID;
import io.smallrye.mutiny.Uni;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import io.smallrye.mutiny.Uni;

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
    public Uni<AuthResponseBO> generateOtp(final long accountId, final String domain) {
        return accountsService.getById(accountId, domain)
                .flatMap(AsyncUtils::uniFromAccountOptional)
                .flatMap(account -> {
                    LOG.info("Generate OTP for action token request. accountId={}, domain={}", account.getId(), account.getDomain());

                    return otpProvider.generateToken(account);
                });
    }

    @Override
    public Uni<ActionTokenBO> generateFromBasicAuth(final AuthRequestBO authRequest, final String action) {
        return basicAuthProvider.getAccount(authRequest)
                .map(account -> {
                    AccountTokenDO token = generateToken(account, action);

                    LOG.info("Action token from credentials request. accountId={}, domain={}, tokenId={}, expiresAt={}",
                            account.getId(), account.getDomain(), token.getId(), token.getExpiresAt());

                    return ActionTokenBO.builder()
                            .accountId(account.getId())
                            .token(token.getToken())
                            .validFor(TOKEN_LIFETIME.toSeconds())
                            .build();
                });
    }

    @Override
    public Uni<ActionTokenBO> generateFromOtp(final long passwordId, String domain, final String otp, final String action) {
        String otpToken = passwordId + ":" + otp;
        AuthRequest request = AuthRequestBO.builder()
                .token(otpToken)
                .build();

        return otpVerifier.verifyAccountTokenAsync(request)
                .flatMap(id -> accountsService.getById(id, domain))
                .flatMap(result -> {
                    if (result.isEmpty()) {
                        LOG.warn("Verified OTP request but the account doesn't exist. passwordId={}", passwordId);
                        return Uni.createFrom().failure(new ServiceException(ErrorCode.ACCOUNT_DOES_NOT_EXIST,
                                "The account associated with that OTP no longer exists"));
                    }

                    return Uni.createFrom().item(result.get());
                })
                .map(account -> {
                    AccountTokenDO token = generateToken(account, action);
                    LOG.info("Generated action token from OTP request. passwordId={}, tokenId={}, expiresAt={}",
                            passwordId, token.getId(), token.getExpiresAt());

                    return ActionTokenBO.builder()
                            .accountId(account.getId())
                            .token(token.getToken())
                            .validFor(TOKEN_LIFETIME.toSeconds())
                            .build();
                });
    }

    @Override
    public Uni<ActionTokenBO> verifyToken(final String token, final String action) {
        return accountTokensRepository.getByToken(token)
                .flatMap(persisted -> {
                    if (persisted.isEmpty()) {
                        return Uni.createFrom().failure(
                                new ServiceException(ErrorCode.TOKEN_EXPIRED_OR_DOES_NOT_EXIST, "Token does not exist"));
                    }

                    Instant now = Instant.now();

                    if (persisted.get().getExpiresAt().isBefore(now)) {
                        return Uni.createFrom().failure(new ServiceException(ErrorCode.EXPIRED_TOKEN, "Token has expired"));
                    }

                    String allowedAction = persisted.get().getAdditionalInformation().get("action");

                    if (allowedAction == null || !allowedAction.equals(action)) {
                        return Uni.createFrom().failure(new ServiceException(ErrorCode.INVALID_TOKEN, "Token was created for a different action"));
                    }

                    LOG.info("Action token verified. tokenId={}, action={}", persisted.get().getId(), action);

                    return Uni.createFrom().item(ActionTokenBO.builder()
                            .accountId(persisted.get().getAssociatedAccountId())
                            .token(token)
                            .action(action)
                            .build());
                });
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

        return accountTokensRepository.save(accountToken)
                .subscribeAsCompletionStage().join();
    }
}
