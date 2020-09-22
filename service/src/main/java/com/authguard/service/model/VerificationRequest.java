package com.authguard.service.model;

import org.immutables.value.Value;

import java.util.List;

@Value.Immutable
@BOStyle
public interface VerificationRequest {
    AccountBO getAccount();
    List<AccountEmailBO> getEmails(); // we might not need to verify all emails
}
