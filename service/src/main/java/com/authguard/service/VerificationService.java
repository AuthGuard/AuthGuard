package com.authguard.service;

import com.authguard.service.model.AccountBO;

import java.util.List;

public interface VerificationService {
    void sendVerificationEmail(AccountBO account);
    void sendVerificationEmail(AccountBO account, List<String> emails);
}
