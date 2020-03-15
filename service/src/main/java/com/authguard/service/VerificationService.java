package com.authguard.service;

public interface VerificationService {
    void verifyEmail(String verificationToken);
}
