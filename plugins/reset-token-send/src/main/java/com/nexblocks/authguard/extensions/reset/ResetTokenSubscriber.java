package com.nexblocks.authguard.extensions.reset;

import com.google.inject.Inject;
import com.nexblocks.authguard.dal.model.AccountTokenDO;
import com.nexblocks.authguard.emb.MessageSubscriber;
import com.nexblocks.authguard.emb.annotations.Channel;
import com.nexblocks.authguard.emb.model.EventType;
import com.nexblocks.authguard.emb.model.Message;
import com.nexblocks.authguard.external.email.EmailProvider;
import com.nexblocks.authguard.external.email.ImmutableEmail;
import com.nexblocks.authguard.service.messaging.ResetTokenMessage;
import com.nexblocks.authguard.service.model.AccountBO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;

@Channel("credentials")
public class ResetTokenSubscriber implements MessageSubscriber {
    private static final Logger LOG = LoggerFactory.getLogger(ResetTokenSubscriber.class);

    private final EmailProvider emailProvider;

    @Inject
    public ResetTokenSubscriber(final EmailProvider emailProvider) {
        this.emailProvider = emailProvider;
    }

    @Override
    public void onMessage(final Message message) {
        if (message.getEventType() == EventType.RESET_TOKEN_GENERATED) {
            final ResetTokenMessage accountToken = (ResetTokenMessage) message.getMessageBody();

            sendEmail(accountToken.getAccount(), accountToken.getAccountToken());
        }
    }

    private void sendEmail(final AccountBO account, final AccountTokenDO accountToken) {
        if (account.getEmail() != null) {
            final ImmutableEmail email = ImmutableEmail.builder()
                    .template("passwordReset")
                    .parameters(Collections.singletonMap("token", accountToken.getToken()))
                    .to(account.getEmail().getEmail())
                    .build();

            emailProvider.send(email);
        } else {
            LOG.error("A password reset token was generated for an account without an email. Account: {}, token ID: {}",
                    account.getId(), accountToken.getId());
        }
    }
}
