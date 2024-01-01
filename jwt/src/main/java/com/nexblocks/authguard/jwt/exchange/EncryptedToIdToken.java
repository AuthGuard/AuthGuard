package com.nexblocks.authguard.jwt.exchange;

import com.google.inject.Inject;
import com.nexblocks.authguard.jwt.IdTokenVerifier;
import com.nexblocks.authguard.jwt.crypto.TokenEncryptorAdapter;
import com.nexblocks.authguard.service.exchange.Exchange;
import com.nexblocks.authguard.service.exchange.TokenExchange;
import com.nexblocks.authguard.service.model.AuthRequestBO;
import com.nexblocks.authguard.service.model.AuthResponseBO;
import io.vavr.control.Either;

import java.util.concurrent.CompletableFuture;

@TokenExchange(from = "encryptedToken", to = "idToken")
public class EncryptedToIdToken implements Exchange {
    private static final String TOKEN_TYPE = "accessToken";

    private final IdTokenVerifier idTokenVerifier;
    private final TokenEncryptorAdapter tokenEncryptor;

    @Inject
    public EncryptedToIdToken(final IdTokenVerifier accessTokenVerifier, final TokenEncryptorAdapter tokenEncryptor) {
        this.idTokenVerifier = accessTokenVerifier;
        this.tokenEncryptor = tokenEncryptor;
    }

    @Override
    public CompletableFuture<AuthResponseBO> exchange(final AuthRequestBO request) {
        String encrypted = request.getToken();
        Either<Exception, String> decrypted = tokenEncryptor.decryptEncoded(encrypted)
                .map(idTokenVerifier::verify);

        if (decrypted.isLeft()) {
            return CompletableFuture.failedFuture(decrypted.getLeft());
        }

        return CompletableFuture.completedFuture(AuthResponseBO.builder()
                        .type(TOKEN_TYPE)
                        .token(decrypted.get())
                        .build());
    }
}
