package com.nexblocks.authguard.jwt.crypto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CryptoHashTest {

    @Test
    void hash() {
        String plain = "Hello, world!";
        String expected = "315f5bdb76d078c43b8ac0064e4a0164612b1fce77c869345bfc94c75894edd3";

        assertThat(CryptoHash.hash(plain)).isEqualTo(expected);
    }
}