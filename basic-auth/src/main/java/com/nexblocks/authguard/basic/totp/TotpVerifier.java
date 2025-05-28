package com.nexblocks.authguard.basic.totp;

import com.atlassian.onetime.core.HMACDigest;
import com.atlassian.onetime.core.OTPLength;
import com.atlassian.onetime.core.TOTP;
import com.atlassian.onetime.core.TOTPGenerator;
import com.atlassian.onetime.model.TOTPSecret;
import com.atlassian.onetime.service.DefaultTOTPService;
import com.atlassian.onetime.service.TOTPConfiguration;
import com.atlassian.onetime.service.TOTPService;
import com.atlassian.onetime.service.TOTPVerificationResult;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.nexblocks.authguard.config.ConfigContext;
import com.nexblocks.authguard.dal.cache.AccountTokensRepository;
import com.nexblocks.authguard.dal.model.AccountTokenDO;
import com.nexblocks.authguard.service.TotpKeysService;
import com.nexblocks.authguard.service.auth.AuthVerifier;
import com.nexblocks.authguard.service.config.AuthenticatorConfig;
import com.nexblocks.authguard.service.config.TotpAuthenticatorsConfig;
import com.nexblocks.authguard.service.exceptions.ServiceException;
import com.nexblocks.authguard.service.exceptions.codes.ErrorCode;
import com.nexblocks.authguard.service.model.AuthRequest;
import com.nexblocks.authguard.service.util.AsyncUtils;
import io.smallrye.mutiny.Uni;
import io.vavr.control.Try;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import io.smallrye.mutiny.Uni;
import java.util.stream.Collectors;

public class TotpVerifier implements AuthVerifier {
    private static final Logger LOG = LoggerFactory.getLogger(TotpVerifier.class);

    private final AccountTokensRepository accountTokensRepository;
    private final TotpKeysService totpKeysService;

    private final TOTPService defaultTotpService;
    private final Map<String, TOTPService> customTotpServices;

    @Inject
    public TotpVerifier(final AccountTokensRepository accountTokensRepository,
                        final TotpKeysService totpKeysService,
                        final @Named("totpAuthenticators") ConfigContext configContext) {
        this.accountTokensRepository = accountTokensRepository;
        this.totpKeysService = totpKeysService;

        TotpAuthenticatorsConfig config = configContext.asConfigBean(TotpAuthenticatorsConfig.class);

        this.defaultTotpService = new DefaultTOTPService();
        this.customTotpServices = config.getCustomAuthenticators().stream()
                .collect(Collectors.toMap(
                        AuthenticatorConfig::getName,
                        authenticatorConfig -> new DefaultTOTPService(new TOTPGenerator(
                                Clock.systemUTC(),
                                0,
                                authenticatorConfig.getTimeStep(),
                                OTPLength.SIX,
                                HMACDigest.SHA1
                        ), new TOTPConfiguration())
                ));
    }

    @Override
    public Long verifyAccountToken(final String token) {
        throw new UnsupportedOperationException("Use verifyAndGetAccountTokenAsync instead");
    }

    @Override
    public Uni<AccountTokenDO> verifyAndGetAccountTokenAsync(final AuthRequest request) {
        String token = request.getToken();
        String[] parts = token.split(":");

        if (parts.length != 2) {
            return Uni.createFrom().failure(new ServiceException(ErrorCode.INVALID_TOKEN,
                    "Token must follow the format linker:totp"));
        }
        String accountToken = parts[0];
        String totp = parts[1];

        return accountTokensRepository.getByToken(accountToken)
                .flatMap(opt -> {
                    if (opt.isPresent()) {
                        return AsyncUtils.uniFromTry(checkIfExpired(opt.get()));
                    }

                    return Uni.createFrom().failure(new ServiceException(ErrorCode.INVALID_TOKEN,
                            "Invalid or expired token"));
                })
                .flatMap(retrievedToken -> verifyTotp(retrievedToken, totp));
    }

    private Try<AccountTokenDO> checkIfExpired(final AccountTokenDO accountToken) {
        if (accountToken.getExpiresAt().isBefore(Instant.now())) {
            return Try.failure(new ServiceException(ErrorCode.EXPIRED_TOKEN,
                    "TOTP linker token has expired"));
        }

        return Try.success(accountToken);
    }

    private Uni<AccountTokenDO> verifyTotp(final AccountTokenDO accountToken,
                                                         final String totp) {
        long accountId = accountToken.getAssociatedAccountId();
        String domain = accountToken.getDomain();

        return totpKeysService.getByAccountIdDecrypted(accountId, domain)
                .flatMap(opt -> {
                    if (opt.isEmpty()) {
                        return Uni.createFrom().failure(new ServiceException(ErrorCode.TOTO_NO_KEY,
                                "Account has no active TOTP keys"));
                    }

                    String authenticator = opt.get().getAuthenticator();
                    TOTPService totpService = defaultTotpService;

                    if (authenticator != null
                            && !authenticator.isBlank()
                            && customTotpServices.containsKey(authenticator)) {
                        totpService = customTotpServices.get(authenticator);
                    }

                    byte[] key = opt.get().getKey();

                    if (!verifyTotp(key, totp, totpService)) {
                        return Uni.createFrom().failure(new ServiceException(ErrorCode.TOTP_INVALID,
                                "TOTP is incorrect"));
                    }

                    return Uni.createFrom().item(accountToken);
                });
    }

    private boolean verifyTotp(final byte[] key, final String input, TOTPService service) {
        TOTP totp = new TOTP(input);
        TOTPVerificationResult result = service.verify(totp, new TOTPSecret(key));

        if (result.isSuccess()) {
            int index = ((TOTPVerificationResult.Success) result).getIndex();

            if (index == 1) {
                LOG.info("Verified TOTP with a skewed time window (T+1) - we were behind the client");
            } else if (index == -1) {
                LOG.info("Verified TOTP with a skewed time window (T-1) - we were ahead of the client");
            }

            return true;
        }

        return false;
    }
}
