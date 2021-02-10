package com.nexblocks.authguard.service;

public interface VerificationService {
    void verifyEmail(String verificationToken);
}
