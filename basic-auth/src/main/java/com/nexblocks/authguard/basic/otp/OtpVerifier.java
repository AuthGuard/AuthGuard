package com.nexblocks.authguard.basic.otp;

import com.google.inject.Inject;
import com.nexblocks.authguard.dal.cache.OtpRepository;
import com.nexblocks.authguard.service.auth.AuthVerifier;
import com.nexblocks.authguard.service.exceptions.ServiceAuthorizationException;
import com.nexblocks.authguard.service.exceptions.codes.ErrorCode;
import com.nexblocks.authguard.service.mappers.ServiceMapper;
import com.nexblocks.authguard.service.model.AuthRequest;
import com.nexblocks.authguard.service.model.EntityType;
import com.nexblocks.authguard.service.model.OneTimePasswordBO;
import com.nexblocks.authguard.service.util.AsyncUtils;
import io.vavr.control.Try;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class OtpVerifier implements AuthVerifier {
    private final OtpRepository otpRepository;
    private final ServiceMapper serviceMapper;

    @Inject
    public OtpVerifier(final OtpRepository otpRepository, final ServiceMapper serviceMapper) {
        this.otpRepository = otpRepository;
        this.serviceMapper = serviceMapper;
    }

    @Override
    public Long verifyAccountToken(String token) {
        throw new UnsupportedOperationException("Use the async function");
    }

    @Override
    public CompletableFuture<Long> verifyAccountTokenAsync(final AuthRequest request) {
        // TODO: no need to have a special format for the token, just receive the two parts in the request
        String[] parts = request.getToken().split(":");

        if (parts.length != 2) {
            return CompletableFuture.failedFuture(new ServiceAuthorizationException(ErrorCode.INVALID_AUTHORIZATION_FORMAT,
                    "Invalid OTP token format"));
        }

        long passwordId;

        try {
            passwordId = Long.parseLong(parts[0]);
        } catch (Exception e) {
            return CompletableFuture.failedFuture(new ServiceAuthorizationException(ErrorCode.INVALID_AUTHORIZATION_FORMAT,
                    "Invalid OTP ID"));
        }
        String otp = parts[1];

        return otpRepository.getById(passwordId)
                .subscribeAsCompletionStage()
                .thenCompose(opt -> {
                    Optional<OneTimePasswordBO> generatedOpt = opt.map(serviceMapper::toBO);

                    if (generatedOpt.isEmpty()) {
                        throw new ServiceAuthorizationException(ErrorCode.INVALID_TOKEN, "Invalid OTP ID");
                    }

                    return AsyncUtils.fromTry(verifyOtp(generatedOpt.get(), otp));
                });
    }

    private Try<Long> verifyOtp(final OneTimePasswordBO generated, final String otp) {
        if (generated.getExpiresAt().isBefore(Instant.now())) {
            return Try.failure(new ServiceAuthorizationException(ErrorCode.EXPIRED_TOKEN, "OTP has expired",
                    EntityType.ACCOUNT, generated.getAccountId()));
        }

        if (generated.getPassword().equals(otp)) {
            return Try.success(generated.getAccountId());
        }

        return Try.failure(new ServiceAuthorizationException(ErrorCode.PASSWORDS_DO_NOT_MATCH,
                "OTP values did not match", EntityType.ACCOUNT, generated.getAccountId()));
    }
}
