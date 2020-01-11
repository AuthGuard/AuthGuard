package org.auther.service.impl;

import com.auther.config.ConfigContext;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.apache.commons.lang3.RandomStringUtils;
import org.auther.dal.OtpRepository;
import org.auther.emb.MessagePublisher;
import org.auther.emb.model.EventType;
import org.auther.emb.model.MessageMO;
import org.auther.service.AccountsService;
import org.auther.service.JwtProvider;
import org.auther.service.OtpService;
import org.auther.service.config.ImmutableOtpConfig;
import org.auther.service.exceptions.ServiceAuthorizationException;
import org.auther.service.impl.jwt.JwtConfigParser;
import org.auther.service.impl.mappers.ServiceMapper;
import org.auther.service.model.AccountBO;
import org.auther.service.model.OneTimePasswordBO;
import org.auther.service.model.TokensBO;

import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.UUID;

public class OtpServiceImpl implements OtpService {
    private final OtpRepository otpRepository;
    private final MessagePublisher emb;
    private final AccountsService accountsService;
    private final ServiceMapper serviceMapper;
    private final JwtProvider jwtProvider;
    private final ImmutableOtpConfig otpConfig;

    @Inject
    public OtpServiceImpl(final OtpRepository otpRepository, final MessagePublisher emb,
                          final AccountsService accountsService,
                          @Named("authenticationTokenProvider") final JwtProvider jwtProvider,
                          final ServiceMapper serviceMapper,
                          @Named("otp") final ConfigContext configContext) {
        this.otpRepository = otpRepository;
        this.emb = emb;
        this.accountsService = accountsService;
        this.serviceMapper = serviceMapper;
        this.jwtProvider = jwtProvider;
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
                .expiresAt(ZonedDateTime.now().plus(JwtConfigParser.parseDuration(otpConfig.getLifeTime())))
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
                .map(serviceMapper::toBO)
                .orElseThrow(() -> new ServiceAuthorizationException("Invalid OTP ID"));

        if (generated.getExpiresAt().isBefore(ZonedDateTime.now())) {
            throw new ServiceAuthorizationException("OTP " + passwordId + " has expired");
        }

        if (generated.getPassword().equals(otp)) {
            return accountsService.getById(generated.getAccountId())
                    .map(jwtProvider::generateToken)
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
