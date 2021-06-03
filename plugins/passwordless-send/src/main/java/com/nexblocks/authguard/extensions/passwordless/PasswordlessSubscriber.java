package com.nexblocks.authguard.extensions.passwordless;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;

@Channel("passwordless")
public class PasswordlessSubscriber implements MessageSubscriber {
    private static final Logger LOG = LoggerFactory.getLogger(PasswordlessSubscriber.class);

    private final EmailProvider emailProvider;

    @Inject
    public PasswordlessSubscriber(final EmailProvider emailProvider) {
        this.emailProvider = emailProvider;
    }

    @Override
    public void onMessage(final Message message) {
        if (message.getEventType() == EventType.PASSWORDLESS_GENERATED) {
            final PasswordlessMessageBody messageBody = (PasswordlessMessageBody) message.getMessageBody();
            final AccountBO account = messageBody.getAccount();
            final AccountTokenDO accountToken = messageBody.getAccountToken();

            sendEmail(account, accountToken);
        }
    }

    private void sendEmail(final AccountBO account, final AccountTokenDO accountToken) {
        if (account.getEmail() != null) {
            final ImmutableEmail email = ImmutableEmail.builder()
                    .template("passwordless")
                    .parameters(Collections.singletonMap("token", accountToken.getToken()))
                    .to(account.getEmail().getEmail())
                    .build();

            emailProvider.send(email);
        } else {
            LOG.error("A passwordless token was generated for an account without an email. Account: {}, token ID: {}",
                    account.getId(), accountToken.getId());
        }
    }
}
