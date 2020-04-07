package com.authguard.service.impl;

import com.authguard.config.ConfigContext;
import com.authguard.service.config.ConfigParser;
import com.authguard.service.mappers.ServiceMapper;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.apache.commons.lang3.RandomStringUtils;
import com.authguard.dal.OtpRepository;
import com.authguard.emb.MessagePublisher;
import com.authguard.emb.model.EventType;
import com.authguard.emb.model.MessageMO;
import com.authguard.service.AccountsService;
import com.authguard.service.AuthProvider;
import com.authguard.service.OtpService;
import com.authguard.service.config.ImmutableOtpConfig;
import com.authguard.service.exceptions.ServiceAuthorizationException;
import com.authguard.service.model.AccountBO;
import com.authguard.service.model.OneTimePasswordBO;
import com.authguard.service.model.TokensBO;

import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.UUID;

public class OtpServiceImpl implements OtpService {
    private final OtpRepository otpRepository;
    private final MessagePublisher emb;
    private final AccountsService accountsService;
    private final ServiceMapper serviceMapper;
    private final AuthProvider authProvider;
    private final ImmutableOtpConfig otpConfig;

    @Inject
    public OtpServiceImpl(final OtpRepository otpRepository, final MessagePublisher emb,
                          final AccountsService accountsService,
                          @Named("authenticationTokenProvider") final AuthProvider authProvider,
                          final ServiceMapper serviceMapper,
                          @Named("otp") final ConfigContext configContext) {
        this.otpRepository = otpRepository;
        this.emb = emb;
        this.accountsService = accountsService;
        this.serviceMapper = serviceMapper;
        this.authProvider = authProvider;
        this.otpConfig = configContext.asConfigBean(ImmutableOtpConfig.class);
    }

    @Override
    public TokensBO generate(final AccountBO account) {
        final String passwordId = UUID.randomUUID().toString();
        final String password = generatePassword();

        final TokensBO token = createToken(passwordId);

        final OneTimePasswordBO oneTimePassword = OneTimePasswordBO.builder()
                .id(passwordId)
                .accountId(account.getId())
                .expiresAt(ZonedDateTime.now().plus(ConfigParser.parseDuration(otpConfig.getLifeTime())))
                .password(password)
                .build();

        otpRepository.save(serviceMapper.toDO(oneTimePassword));

        // a place holder until actual implementation is available
        emb.publish(MessageMO.builder()
                .eventType(EventType.OTP)
                .timestamp(OffsetDateTime.now())
                .messageBody(oneTimePassword)
                .build());

        return token;
    }

    @Override
    public TokensBO authenticate(final String passwordId, final String otp) {
        final OneTimePasswordBO generated = otpRepository.getById(passwordId)
                .thenApply(optional -> optional.map(serviceMapper::toBO))
                .join()
                .orElseThrow(() -> new ServiceAuthorizationException("Invalid OTP ID"));

        if (generated.getExpiresAt().isBefore(ZonedDateTime.now())) {
            throw new ServiceAuthorizationException("OTP " + passwordId + " has expired");
        }

        if (generated.getPassword().equals(otp)) {
            return accountsService.getById(generated.getAccountId())
                    .map(authProvider::generateToken)
                    .orElseThrow(() -> new ServiceAuthorizationException("Account " + generated.getAccountId()
                            + " doesn't exist"));
        } else {
            throw new ServiceAuthorizationException("OTPs don't match");
        }
    }

    private TokensBO createToken(final String passwordId) {
        return TokensBO.builder()
                .type("OTP")
                .token(passwordId)
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
