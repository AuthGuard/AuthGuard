package com.nexblocks.authguard.basic.otp;

import com.nexblocks.authguard.dal.cache.OtpRepository;
import com.nexblocks.authguard.service.auth.AuthVerifier;
import com.nexblocks.authguard.service.exceptions.ServiceAuthorizationException;
import com.nexblocks.authguard.service.exceptions.codes.ErrorCode;
import com.nexblocks.authguard.service.mappers.ServiceMapper;
import com.nexblocks.authguard.service.model.EntityType;
import com.nexblocks.authguard.service.model.OneTimePasswordBO;
import com.google.inject.Inject;
import io.vavr.control.Either;

import java.time.OffsetDateTime;
import java.util.Optional;

public class OtpVerifier implements AuthVerifier {
    private final OtpRepository otpRepository;
    private final ServiceMapper serviceMapper;

    @Inject
    public OtpVerifier(final OtpRepository otpRepository, final ServiceMapper serviceMapper) {
        this.otpRepository = otpRepository;
        this.serviceMapper = serviceMapper;
    }

    @Override
    public Either<Exception, String> verifyAccountToken(final String token) {
        // TODO: no need to have a special format for the token, just receive the two parts in the request
        final String[] parts = token.split(":");

        if (parts.length != 2) {
            return Either.left(new ServiceAuthorizationException(ErrorCode.INVALID_AUTHORIZATION_FORMAT,
                    "Invalid OTP token format"));
        }

        final String passwordId = parts[0];
        final String otp = parts[1];

        final Optional<OneTimePasswordBO> generatedOpt = otpRepository.getById(passwordId)
                .thenApply(optional -> optional.map(serviceMapper::toBO))
                .join();

        if (generatedOpt.isPresent()) {
            final OneTimePasswordBO generated = generatedOpt.get();

            if (generated.getExpiresAt().isBefore(OffsetDateTime.now())) {
                return Either.left(new ServiceAuthorizationException(ErrorCode.EXPIRED_TOKEN, "OTP " + passwordId + " has expired",
                        EntityType.ACCOUNT, generated.getAccountId()));
            }

            if (generated.getPassword().equals(otp)) {
                return Either.right(generated.getAccountId());
            } else {
                return Either.left(new ServiceAuthorizationException(ErrorCode.PASSWORDS_DO_NOT_MATCH,
                        "OTP " + passwordId + " values did not match", EntityType.ACCOUNT, generated.getAccountId()));
            }
        } else {
            return Either.left(new ServiceAuthorizationException(ErrorCode.INVALID_TOKEN, "Invalid OTP ID"));
        }
    }
}
