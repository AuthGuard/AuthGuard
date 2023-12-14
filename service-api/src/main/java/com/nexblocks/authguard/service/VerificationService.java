package com.nexblocks.authguard.service;

import com.nexblocks.authguard.service.model.AuthResponseBO;

public interface VerificationService {
    void verifyEmail(String verificationToken);
    AuthResponseBO sendPhoneNumberVerification(long accountId);
    AuthResponseBO sendPhoneNumberVerificationByIdentifier(String identifier, String domain);
    void verifyPhoneNumber(long passwordId, String otp, String phoneNumber);
}
