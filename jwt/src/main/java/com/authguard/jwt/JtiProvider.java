package com.authguard.jwt;

/**
 * JTI interface.
 */
public interface JtiProvider {
    /**
     * @return A unique JTI.
     */
    String next();

    /**
     * Check a JTI against a blacklist
     * @param jti The JTI
     * @return True if the JTI is valid, false
     *         otherwise.
     */
    boolean validate(String jti);
}
