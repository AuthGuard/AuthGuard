package org.auther.service.impl.jwt;

import org.auther.service.impl.jwt.BasicJTIProvider;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BasicJTIProviderTest {
    private final BasicJTIProvider provider = new BasicJTIProvider();

    @Test
    void generate() {
        assertThat(provider.next()).isNotNull();
    }

    @Test
    void canOnlyBeUsedOnce() {
        final String jti = provider.next();
        assertThat(provider.validate(jti)).isTrue();
        assertThat(provider.validate(jti)).isFalse();
    }

    @Test
    void notGenerated() {
        assertThat(provider.validate("malicious")).isFalse();
    }
}
