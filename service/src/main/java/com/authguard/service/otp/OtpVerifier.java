package com.authguard.service.otp;

import com.authguard.dal.OtpRepository;
import com.authguard.service.AuthTokenVerfier;
import com.authguard.service.exceptions.ServiceAuthorizationException;
import com.authguard.service.exceptions.codes.ErrorCode;
import com.authguard.service.mappers.ServiceMapper;
import com.authguard.service.model.OneTimePasswordBO;
import com.google.inject.Inject;

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
    public Optional<String> verifyAccountToken(final String token) {
        final String[] parts = token.split(":");

        if (parts.length != 2) {
            throw new ServiceAuthorizationException(ErrorCode.INVALID_AUTHORIZATION_FORMAT, "Invalid OTP token format");
        }

        final String passwordId = parts[0];
        final String otp = parts[1];

        final OneTimePasswordBO generated = otpRepository.getById(passwordId)
                .thenApply(optional -> optional.map(serviceMapper::toBO))
                .join()
                .orElseThrow(() -> new ServiceAuthorizationException(ErrorCode.INVALID_TOKEN, "Invalid OTP ID"));

        if (generated.getExpiresAt().isBefore(ZonedDateTime.now())) {
            throw new ServiceAuthorizationException(ErrorCode.EXPIRED_TOKEN, "OTP " + passwordId + " has expired");
        }

        if (generated.getPassword().equals(otp)) {
            return Optional.of(generated.getAccountId());
        } else {
            return Optional.empty();
        }
    }
}
