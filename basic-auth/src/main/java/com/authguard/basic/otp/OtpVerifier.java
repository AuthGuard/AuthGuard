package com.authguard.basic.otp;

import com.authguard.dal.OtpRepository;
import com.authguard.service.auth.AuthTokenVerfier;
import com.authguard.service.exceptions.ServiceAuthorizationException;
import com.authguard.service.exceptions.codes.ErrorCode;
import com.authguard.service.mappers.ServiceMapper;
import com.authguard.service.model.OneTimePasswordBO;
import com.google.inject.Inject;
import io.vavr.control.Either;

import java.time.ZonedDateTime;
import java.util.Optional;

public class OtpVerifier implements AuthTokenVerfier {
    private final OtpRepository otpRepository;
    private final ServiceMapper serviceMapper;

    @Inject
    public OtpVerifier(final OtpRepository otpRepository, final ServiceMapper serviceMapper) {
        this.otpRepository = otpRepository;
        this.serviceMapper = serviceMapper;
    }

    @Override
    public Either<Exception, String> verifyAccountToken(final String token) {
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

            if (generated.getExpiresAt().isBefore(ZonedDateTime.now())) {
                return Either.left(new ServiceAuthorizationException(ErrorCode.EXPIRED_TOKEN, "OTP " + passwordId + " has expired"));
            }

            if (generated.getPassword().equals(otp)) {
                return Either.right(generated.getAccountId());
            } else {
                return Either.left(new ServiceAuthorizationException(ErrorCode.PASSWORDS_DO_NOT_MATCH,
                        "OTP " + passwordId + " values did not match"));
            }
        } else {
            return Either.left(new ServiceAuthorizationException(ErrorCode.INVALID_TOKEN, "Invalid OTP ID"));
        }
    }
}
