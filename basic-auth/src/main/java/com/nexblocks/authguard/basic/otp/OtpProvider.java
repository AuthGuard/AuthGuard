package com.nexblocks.authguard.basic.otp;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.nexblocks.authguard.basic.config.OtpConfig;
import com.nexblocks.authguard.basic.config.OtpConfigInterface;
import com.nexblocks.authguard.config.ConfigContext;
import com.nexblocks.authguard.dal.cache.OtpRepository;
import com.nexblocks.authguard.emb.MessageBus;
import com.nexblocks.authguard.emb.Messages;
import com.nexblocks.authguard.service.auth.AuthProvider;
import com.nexblocks.authguard.service.auth.ProvidesToken;
import com.nexblocks.authguard.service.config.ConfigParser;
import com.nexblocks.authguard.service.exceptions.ServiceAuthorizationException;
import com.nexblocks.authguard.service.exceptions.codes.ErrorCode;
import com.nexblocks.authguard.service.mappers.ServiceMapper;
import com.nexblocks.authguard.service.model.*;
import com.nexblocks.authguard.service.util.ID;
import org.apache.commons.lang3.RandomStringUtils;

import java.time.Duration;
import java.time.Instant;
import io.smallrye.mutiny.Uni;

@ProvidesToken("otp")
public class OtpProvider implements AuthProvider {
    private static final String TOKEN_TYPE = "otp";
    private static final String OTP_CHANNEL = "otp";

    private final OtpRepository otpRepository;
    private final ServiceMapper serviceMapper;
    private final MessageBus messageBus;
    private final OtpConfig otpConfig;

    private final Duration tokenTtl;

    @Inject
    public OtpProvider(final OtpRepository otpRepository, final ServiceMapper serviceMapper,
                       final MessageBus messageBus, final @Named("otp") ConfigContext otpConfig) {
        this.otpRepository = otpRepository;
        this.serviceMapper = serviceMapper;
        this.messageBus = messageBus;
        this.otpConfig = otpConfig.asConfigBean(OtpConfig.class);
        this.tokenTtl = ConfigParser.parseDuration(this.otpConfig.getLifeTime());
    }

    @Override
    public Uni<AuthResponseBO> generateToken(final AccountBO account, final TokenRestrictionsBO restrictions,
                                                           final TokenOptionsBO options) {
        if (!account.isActive()) {
            throw new ServiceAuthorizationException(ErrorCode.ACCOUNT_INACTIVE, "Account was deactivated");
        }

        String password = generatePassword();

        OneTimePasswordBO oneTimePassword = OneTimePasswordBO.builder()
                .id(ID.generate())
                .accountId(account.getId())
                .expiresAt(Instant.now().plus(tokenTtl))
                .password(password)
                .clientId(options.getClientId())
                .deviceId(options.getDeviceId())
                .externalSessionId(options.getExternalSessionId())
                .sourceIp(options.getSourceIp())
                .userAgent(options.getUserAgent())
                .build();

        return otpRepository.save(serviceMapper.toDO(oneTimePassword))
                .map(persistedOtp -> {
                    OtpMessageBody messageBody = new OtpMessageBody(oneTimePassword, account,
                            options,
                            otpConfig.getMethod() == OtpConfigInterface.Method.EMAIL,
                            otpConfig.getMethod() == OtpConfigInterface.Method.SMS);

                    AuthResponseBO token = createToken(persistedOtp.getId(), account.getId(),
                            options.getTrackingSession());

                    messageBus.publish(OTP_CHANNEL, Messages.otpGenerated(messageBody, account.getDomain()));

                    return token;
                });
    }

    @Override
    public Uni<AuthResponseBO> generateToken(final AccountBO account) {
        throw new UnsupportedOperationException("Use the method which accepts TokenOptionsBO");
    }

    @Override
    public AuthResponseBO generateToken(final AppBO app) {
        throw new UnsupportedOperationException("OTPs cannot be generated for applications");
    }

    private AuthResponseBO createToken(final long passwordId, final long accountId,
                                       final String trackingSession) {
        return AuthResponseBO.builder()
                .type(TOKEN_TYPE)
                .token(passwordId)
                .entityType(EntityType.ACCOUNT)
                .entityId(accountId)
                .trackingSession(trackingSession)
                .build();
    }

    private String generatePassword() {
        switch (otpConfig.getMode()) {
            case ALPHANUMERIC: return RandomStringUtils.randomAlphanumeric(otpConfig.getLength());
            case ALPHABETIC: return RandomStringUtils.randomAlphabetic(otpConfig.getLength());
            case NUMERIC: return RandomStringUtils.randomNumeric(otpConfig.getLength());
            default: throw new IllegalStateException("Unrecognized OTP mode " + otpConfig.getMode());
        }
    }
}
