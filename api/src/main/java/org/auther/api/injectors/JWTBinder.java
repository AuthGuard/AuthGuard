package org.auther.api.injectors;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import org.auther.service.JTIProvider;
import org.auther.service.JwtProvider;
import org.auther.service.impl.jwt.BasicJTIProvider;
import org.auther.service.impl.jwt.JwtProviderImpl;

import java.security.SecureRandom;

public class JWTBinder extends MappersBinder {

    @Override
    public void configure() {
        final byte[] randomSecret = new byte[256];
        new SecureRandom().nextBytes(randomSecret);

        final Algorithm jwtAlgorithm = Algorithm.HMAC256(randomSecret);

        bind(Algorithm.class).toInstance(jwtAlgorithm);
        bind(JWTVerifier.class).toInstance(JWT.require(jwtAlgorithm).build());

        bind(JTIProvider.class).to(BasicJTIProvider.class);
        bind(JwtProvider.class).to(JwtProviderImpl.class);
    }

}
