package com.authguard.service.impl;

import com.authguard.config.ConfigContext;
import com.authguard.dal.AccountTokensRepository;
import com.authguard.dal.model.AccountTokenDO;
import com.authguard.external.email.EmailProvider;
import com.authguard.external.email.ImmutableEmail;
import com.authguard.service.VerificationMessageService;
import com.authguard.service.config.ConfigParser;
import com.authguard.service.config.VerificationConfig;
import com.authguard.service.model.AccountBO;
import com.authguard.service.model.AccountEmailBO;
import com.google.inject.Inject;
import com.google.inject.name.Named;

import java.time.ZonedDateTime;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class VerificationMessageServiceImpl implements VerificationMessageService {
    private final String EMAIL_TEMPLATE = "verify-email";

    private final EmailProvider emailProvider;
    private final AccountTokensRepository accountTokensRepository;
    private final VerificationConfig verificationConfig;

    @Inject
    public VerificationMessageServiceImpl(final EmailProvider emailProvider,
                                          final AccountTokensRepository accountTokensRepository,
                                          final @Named("verification") ConfigContext configContext) {
        this.emailProvider = emailProvider;
        this.accountTokensRepository = accountTokensRepository;
        this.verificationConfig = configContext.asConfigBean(VerificationConfig.class);
    }

    @Override
    public void sendVerificationEmail(final AccountBO account) {
        final List<String> unverifiedEmails = account.getEmails().stream()
                .filter(accountEmailBO -> !accountEmailBO.isVerified())
                .map(AccountEmailBO::getEmail)
                .collect(Collectors.toList());

        sendVerificationEmail(account, unverifiedEmails);
    }

    @Override
    public void sendVerificationEmail(final AccountBO account, final List<String> emails) {
        emails.forEach(email -> {
            final String token = generateVerificationString();

            final ZonedDateTime expiration = ZonedDateTime.now()
                    .plus(ConfigParser.parseDuration(verificationConfig.getEmailVerificationLife()));

            final AccountTokenDO accountToken = AccountTokenDO.builder()
                    .expiresAt(expiration)
                    .associatedAccountId(account.getId())
                    .token(token)
                    .additionalInformation(email)
                    .build();

            accountTokensRepository.save(accountToken);

            final String url = generateVerificationUrl(token);

            final ImmutableEmail email1 = ImmutableEmail.builder()
                    .template(EMAIL_TEMPLATE)
                    .to(email)
                    .putParameters("url", url)
                    .build();

            emailProvider.send(email1);
        });
    }

    private String generateVerificationString() {
        final byte[] verificationCode = UUID.randomUUID().toString().getBytes();
        return Base64.getEncoder().encodeToString(verificationCode);
    }

    private String generateVerificationUrl(final String token) {
        return verificationConfig.getVerifyEmailUrlTemplate().replace("${token}", token);
    }
}
