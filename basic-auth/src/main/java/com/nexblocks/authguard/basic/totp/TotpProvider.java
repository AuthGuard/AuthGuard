package com.nexblocks.authguard.basic.totp;

import com.google.inject.Inject;
import com.nexblocks.authguard.dal.cache.AccountTokensRepository;
import com.nexblocks.authguard.dal.model.AccountTokenDO;
import com.nexblocks.authguard.emb.MessageBus;
import com.nexblocks.authguard.emb.Messages;
import com.nexblocks.authguard.service.auth.AuthProvider;
import com.nexblocks.authguard.service.auth.ProvidesToken;
import com.nexblocks.authguard.service.exceptions.ServiceAuthorizationException;
import com.nexblocks.authguard.service.exceptions.codes.ErrorCode;
import com.nexblocks.authguard.service.mappers.ServiceMapper;
import com.nexblocks.authguard.service.model.*;
import com.nexblocks.authguard.service.random.CryptographicRandom;
import com.nexblocks.authguard.service.util.ID;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;

/**
 * Not really a TOTP provider per se, but just generates
 * an intermediate token between a previous auth step
 * and TOTP. The linker token is then checked by the
 * verifier to ensure that the TOTP is not the first
 * step in an auth flow, and also enforce token restrictions
 * and do the rest of security checks (same user agent..etc).
 */
@ProvidesToken("totp")
public class TotpProvider implements AuthProvider {
    private static final String TOKEN_TYPE = "totp";
    private static final String TOTP_CHANNEL = "totp";
    private static final int TOKEN_SIZE = 12;

    private final AccountTokensRepository accountTokensRepository;
    private final ServiceMapper serviceMapper;
    private final MessageBus messageBus;
    private final CryptographicRandom cryptographicRandom;
    private final Duration tokenTtl;

    @Inject
    public TotpProvider(final AccountTokensRepository accountTokensRepository,
                        final ServiceMapper serviceMapper,
                        final MessageBus messageBus) {
        this.accountTokensRepository = accountTokensRepository;
        this.serviceMapper = serviceMapper;
        this.messageBus = messageBus;

        this.cryptographicRandom = new CryptographicRandom();
        this.tokenTtl = Duration.ofMinutes(2);
    }


    @Override
    public CompletableFuture<AuthResponseBO> generateToken(final AccountBO account,
                                                           final TokenRestrictionsBO restrictions,
                                                           final TokenOptionsBO options) {
        if (!account.isActive()) {
            throw new ServiceAuthorizationException(ErrorCode.ACCOUNT_INACTIVE, "Account was deactivated");
        }

        String token = cryptographicRandom.base64(TOKEN_SIZE);

        AccountTokenDO accountToken = AccountTokenDO.builder()
                .id(ID.generate())
                .token(token)
                .domain(account.getDomain())
                .associatedAccountId(account.getId())
                .expiresAt(Instant.now().plus(tokenTtl))
                .clientId(options.getClientId())
                .deviceId(options.getDeviceId())
                .externalSessionId(options.getExternalSessionId())
                .trackingSession(options.getTrackingSession())
                .sourceIp(options.getSourceIp())
                .userAgent(options.getUserAgent())
                .tokenRestrictions(serviceMapper.toDO(restrictions))
                .build();

        return accountTokensRepository.save(accountToken)
                .map(persisted -> {
                    TotpLinkerMessageBody messageBody = new TotpLinkerMessageBody(token, account,
                            options);

                    messageBus.publish(TOTP_CHANNEL, Messages.otpGenerated(messageBody, account.getDomain()));

                    return AuthResponseBO.builder()
                            .type(TOKEN_TYPE)
                            .token(token)
                            .entityType(EntityType.ACCOUNT)
                            .entityId(account.getId())
                            .trackingSession(options.getTrackingSession())
                            .build();
                })
                .subscribeAsCompletionStage();
    }

    @Override
    public AuthResponseBO generateToken(final AppBO app) {
        throw new UnsupportedOperationException("TOTP cannot be used by applications");
    }
}
