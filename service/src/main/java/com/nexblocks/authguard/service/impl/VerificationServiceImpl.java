package com.nexblocks.authguard.service.impl;

import com.google.inject.Inject;
import com.nexblocks.authguard.basic.otp.OtpProvider;
import com.nexblocks.authguard.basic.otp.OtpVerifier;
import com.nexblocks.authguard.dal.cache.AccountTokensRepository;
import com.nexblocks.authguard.dal.model.AccountTokenDO;
import com.nexblocks.authguard.external.sms.ImmutableTextMessage;
import com.nexblocks.authguard.external.sms.SmsProvider;
import com.nexblocks.authguard.service.AccountsService;
import com.nexblocks.authguard.service.VerificationService;
import com.nexblocks.authguard.service.exceptions.ServiceException;
import com.nexblocks.authguard.service.exceptions.ServiceNotFoundException;
import com.nexblocks.authguard.service.exceptions.codes.ErrorCode;
import com.nexblocks.authguard.service.model.AccountBO;
import com.nexblocks.authguard.service.model.AuthResponseBO;
import com.nexblocks.authguard.service.model.PhoneNumberBO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public class VerificationServiceImpl implements VerificationService {
    private static final Logger LOG = LoggerFactory.getLogger(VerificationServiceImpl.class);

    private static final String TARGET_EMAIL_PROPERTY = "email";
    private static final String SMS_TEMPLATE = "verify-phone-number";

    private final AccountTokensRepository accountTokensRepository;
    private final AccountsService accountsService;
    private final SmsProvider smsProvider;
    private final OtpProvider otpProvider;
    private final OtpVerifier otpVerifier;

    @Inject
    public VerificationServiceImpl(final AccountTokensRepository accountTokensRepository,
                                   final AccountsService accountsService,
                                   final SmsProvider smsProvider,
                                   final OtpProvider otpProvider,
                                   final OtpVerifier otpVerifier) {
        this.accountTokensRepository = accountTokensRepository;
        this.accountsService = accountsService;
        this.smsProvider = smsProvider;
        this.otpProvider = otpProvider;
        this.otpVerifier = otpVerifier;
    }

    @Override
    public void verifyEmail(final String verificationToken) {
        final AccountTokenDO accountToken = accountTokensRepository.getByToken(verificationToken)
                .join()
                .orElseThrow(() -> new ServiceNotFoundException(ErrorCode.TOKEN_EXPIRED_OR_DOES_NOT_EXIST,
                        "AccountDO token " + verificationToken + " does not exist"));

        if (accountToken.getExpiresAt().isBefore(Instant.now())) {
            LOG.info("Email verification request with expired token. tokenId={}, expiresAt={}, accountId={}",
                    accountToken.getId(), accountToken.getExpiresAt(), accountToken.getAssociatedAccountId());

            throw new ServiceException(ErrorCode.EXPIRED_TOKEN, "Token " + verificationToken + " has expired");
        }

        LOG.info("Email verification request. tokenId={}, expiresAt={}, accountId={}",
                accountToken.getId(), accountToken.getExpiresAt(), accountToken.getAssociatedAccountId());

        final String verifiedEmail = Optional.ofNullable(accountToken.getAdditionalInformation())
                .map(additional -> additional.get(TARGET_EMAIL_PROPERTY))
                .orElseThrow(() -> new ServiceException(ErrorCode.INVALID_TOKEN, "Invalid account token: no valid additional information"));

        final AccountBO account = accountsService.getById(accountToken.getAssociatedAccountId()).join()
                .orElseThrow(() -> new ServiceNotFoundException(ErrorCode.ACCOUNT_DOES_NOT_EXIST,
                        "Account " + accountToken.getAssociatedAccountId() + " does not exist"));

        final AccountBO updated;

        if (verifiedEmail.equals(account.getEmail().getEmail())) {
            updated = account.withEmail(account.getEmail().withVerified(true));
        } else if (verifiedEmail.equals(account.getBackupEmail().getEmail())) {
            updated = account.withBackupEmail(account.getBackupEmail().withVerified(true));
        } else {
            throw new ServiceException(ErrorCode.INVALID_TOKEN, "Account " + account.getId() + " does not contain the " +
                    "email associated with the verification token");
        }

        try {
            accountsService.update(updated);
        } catch (final Exception e) {
            LOG.error("Failed to update account after email verification", e);
        }
    }

    @Override
    public AuthResponseBO sendPhoneNumberVerification(final long accountId) {
        final AccountBO account = accountsService.getById(accountId).join()
                .orElseThrow(() -> new ServiceNotFoundException(ErrorCode.ACCOUNT_DOES_NOT_EXIST,
                        "Account " + accountId + " does not exist"));

        return sendVerificationSms(account);
    }

    @Override
    public AuthResponseBO sendPhoneNumberVerificationByIdentifier(final String identifier, final String domain) {
        final AccountBO account = accountsService.getByIdentifier(identifier, domain).join()
                .orElseThrow(() -> new ServiceNotFoundException(ErrorCode.ACCOUNT_DOES_NOT_EXIST,
                        "No account with that identifier exists"));

        return sendVerificationSms(account);
    }

    @Override
    public void verifyPhoneNumber(final long passwordId, final String otp, final String phoneNumber) {
        String token = passwordId + ":" + otp;
        Long accountId = otpVerifier.verifyAccountTokenAsync(token).join();

        Optional<AccountBO> account = accountsService.getById(accountId).join();

        if (account.isEmpty()) {
            LOG.info("Phone number verification request for deleted account. passwordId={}, accountId={}", passwordId, accountId);

            throw new ServiceException(ErrorCode.ACCOUNT_DOES_NOT_EXIST, "The account associated with that token no longer exists");
        }

        if (account.get().getPhoneNumber() == null
                || !Objects.equals(account.get().getPhoneNumber().getNumber(), phoneNumber)) {
            LOG.warn("Phone number verification request with the wrong phone number. accountId={}, passwordId={}",
                    accountId, passwordId);

            throw new ServiceException(ErrorCode.GENERIC_AUTH_FAILURE, "The provided phone number does not match the one in the account");
        }

        LOG.info("Phone number verification request. accountId={}, domain={}, passwordId={}",
                passwordId, account.get().getDomain(), account.get().getId());

        AccountBO updated = account.get().withPhoneNumber(PhoneNumberBO.builder()
                .number(account.get().getPhoneNumber().getNumber())
                .verified(true)
                .build());

        try {
            accountsService.update(updated);
        } catch (final Exception e) {
            LOG.error("Failed to update account after phone number verification", e);
        }
    }

    private AuthResponseBO sendVerificationSms(final AccountBO account) {
        AuthResponseBO otp = otpProvider.generateToken(account).join();
        ImmutableTextMessage message = ImmutableTextMessage.builder()
                .template(SMS_TEMPLATE)
                .to(account.getPhoneNumber().getNumber())
                .putParameters("account", account)
                .putParameters("otp", otp.getToken())
                .build();

        smsProvider.send(message);

        return otp;
    }
}
