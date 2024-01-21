package com.nexblocks.authguard.service;

import com.nexblocks.authguard.service.model.AuthResponseBO;

public interface VerificationService {
    void verifyEmail(String verificationToken, String domain);
    AuthResponseBO sendPhoneNumberVerification(long accountId, String domain);
    AuthResponseBO sendPhoneNumberVerificationByIdentifier(String identifier, String domain);
    void verifyPhoneNumber(long passwordId, String domain, String otp, String phoneNumber);
}
