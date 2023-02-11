package com.nexblocks.authguard.service;

import com.nexblocks.authguard.service.model.AuthResponseBO;

public interface VerificationService {
    void verifyEmail(String verificationToken);
    AuthResponseBO sendPhoneNumberVerification(String accountId);
    AuthResponseBO sendPhoneNumberVerificationByIdentifier(String identifier, String domain);
    void verifyPhoneNumber(String passwordId, String otp, String phoneNumber);
}
