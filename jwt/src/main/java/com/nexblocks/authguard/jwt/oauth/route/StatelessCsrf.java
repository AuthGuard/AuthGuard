package com.nexblocks.authguard.jwt.oauth.route;

import com.atlassian.onetime.core.HMACDigest;
import com.atlassian.onetime.core.OTPLength;
import com.atlassian.onetime.core.TOTP;
import com.atlassian.onetime.core.TOTPGenerator;
import com.atlassian.onetime.model.TOTPSecret;
import com.atlassian.onetime.service.DefaultTOTPService;
import com.atlassian.onetime.service.TOTPConfiguration;
import com.atlassian.onetime.service.TOTPService;
import com.atlassian.onetime.service.TOTPVerificationResult;
import com.nexblocks.authguard.crypto.KeyLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;
import java.time.Clock;
import java.util.Base64;
import java.util.List;

/**
 * This class generates and validates CSRF tokens without
 * needing to store them. It also enforces time constraints
 * by using a time-based one-time password as part of the
 * token generation. The algorithm works as follows:
 * To generate:
 * 1. generate a TOTP of length N
 * 2. XOR the bytes of the TOTP with an identifier for the
 *    request, where the identifier has length K >= N
 * 3. encode the bytes as base-64
 * -
 * To verify:
 * 1. decode the token from base-64
 * 2. XOR the bytes of the token with the identifier which
 *    results in the TOTP
 * 3. validate the TOTP as normal
 */
public class StatelessCsrf {
    private static final Logger LOG = LoggerFactory.getLogger(StatelessCsrf.class);
    private static final int TOKEN_DURATION_SECONDS = 5 * 60;

    private final byte[] key;
    private final TOTPService totpService;
    private final TOTPConfiguration totpConfiguration;
    private final TOTPGenerator totpGenerator;
    private final TOTPSecret totpSecret;

    public StatelessCsrf(final String key) {
        this.key = KeyLoader.readTexFileOrValue(key);
        this.totpGenerator = new TOTPGenerator(
                Clock.systemUTC(),
                0,
                TOKEN_DURATION_SECONDS,
                OTPLength.TEN,
                HMACDigest.SHA1
        );
        this.totpConfiguration = new TOTPConfiguration(1, 1);
        this.totpService = new DefaultTOTPService(this.totpGenerator, this.totpConfiguration);
        this.totpSecret = new TOTPSecret(this.key);
    }

    public String generate(final String identifier) {
        List<TOTP> codes = this.totpGenerator.generate(this.totpSecret,
                totpConfiguration.getAllowedPastSteps(),
                totpConfiguration.getAllowedFutureSteps());

        byte[] csrfBytes = codes.get(0).getValue().getBytes(Charset.defaultCharset());
        byte[] identifierBytes = identifier.getBytes(Charset.defaultCharset());

        for (int i = 0; i < Math.min(csrfBytes.length, identifierBytes.length); i++) {
            csrfBytes[i] = (byte) (csrfBytes[i] ^ identifier.charAt(i));
        }

        return Base64.getUrlEncoder().encodeToString(csrfBytes);
    }

    public boolean isValid(final String csrfToken, final String identifier) {
        byte[] csrfBytes;

        try {
            csrfBytes = Base64.getUrlDecoder().decode(csrfToken);
        } catch (Exception e) {
            return false;
        }

        byte[] identifierBytes = identifier.getBytes(Charset.defaultCharset());
        byte[] totpBytes = new byte[csrfBytes.length];

        for (int i = 0; i < csrfBytes.length; i++) {
            totpBytes[i] = (byte) (csrfBytes[i] ^ identifierBytes[i]);
        }

        String totpString = new String(totpBytes);

        TOTP totp = new TOTP(totpString);
        TOTPVerificationResult result = this.totpService.verify(totp, this.totpSecret);

        if (result.isSuccess()) {
            int index = ((TOTPVerificationResult.Success) result).getIndex();

            if (index == 1) {
                LOG.info("Verified TOTP with a skewed time window (T+1) - we were behind the client");
            } else if (index == -1) {
                LOG.info("Verified TOTP with a skewed time window (T-1) - we were ahead of the client");
            }

            return true;
        }

        return false;
    }
}
