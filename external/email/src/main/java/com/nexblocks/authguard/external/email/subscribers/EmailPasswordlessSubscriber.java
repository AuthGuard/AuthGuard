package com.nexblocks.authguard.external.email.subscribers;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.nexblocks.authguard.basic.passwordless.PasswordlessMessageBody;
import com.nexblocks.authguard.dal.model.AccountTokenDO;
import com.nexblocks.authguard.emb.MessageSubscriber;
import com.nexblocks.authguard.emb.annotations.Channel;
import com.nexblocks.authguard.emb.model.EventType;
import com.nexblocks.authguard.emb.model.Message;
import com.nexblocks.authguard.external.email.EmailProvider;
import com.nexblocks.authguard.external.email.ImmutableEmail;
import com.nexblocks.authguard.service.model.AccountBO;
import com.nexblocks.authguard.service.model.TokenOptionsBO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Channel("passwordless")
public class EmailPasswordlessSubscriber implements MessageSubscriber {
    private static final Logger LOG = LoggerFactory.getLogger(EmailPasswordlessSubscriber.class);

    private final EmailProvider emailProvider;

    @Inject
    public EmailPasswordlessSubscriber(final EmailProvider emailProvider) {
        this.emailProvider = emailProvider;
    }

    @Override
    public void onMessage(final Message message) {
        if (message.getEventType() == EventType.PASSWORDLESS_GENERATED) {
            final PasswordlessMessageBody body = (PasswordlessMessageBody) message.getMessageBody();

            sendEmail(body.getAccount(), body.getAccountToken(), body.getTokenOptions());
        }
    }

    private void sendEmail(final AccountBO account, final AccountTokenDO accountToken,
                           final TokenOptionsBO tokenOptions) {
        if (account.getEmail() != null) {
            final ImmutableMap.Builder<String, String> parameters
                    = EmailParametersHelper.getForAccount(account, tokenOptions);

            parameters.put("token", accountToken.getToken());

            final ImmutableEmail email = ImmutableEmail.builder()
                    .template("passwordless")
                    .parameters(parameters.build())
                    .to(account.getEmail().getEmail())
                    .build();

            emailProvider.send(email);
        } else {
            LOG.error("A passwordless token was generated for an account without an email. Account: {}, token ID: {}",
                    account.getId(), accountToken.getId());
        }
    }
}
