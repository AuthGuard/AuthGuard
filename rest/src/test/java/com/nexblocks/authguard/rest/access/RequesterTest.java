package com.nexblocks.authguard.rest.access;

import com.nexblocks.authguard.api.dto.requests.AuthRequestDTO;
import org.jeasy.random.EasyRandom;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RequesterTest {

    private final EasyRandom random = new EasyRandom();

    @Test
    void authClientCanPerform() {
        final AuthRequestDTO request = random.nextObject(AuthRequestDTO.class)
                .withUserAgent(null)
                .withSourceIp(null);

        assertThat(Requester.authClientCanPerform(request)).isTrue();
    }

    @Test
    void authClientCanPerformWithUserAgent() {
        final AuthRequestDTO request = random.nextObject(AuthRequestDTO.class)
                .withUserAgent("agent");

        assertThat(Requester.authClientCanPerform(request)).isFalse();
    }

    @Test
    void authClientCanPerformWithSourceIp() {
        final AuthRequestDTO request = random.nextObject(AuthRequestDTO.class)
                .withSourceIp("ip");

        assertThat(Requester.authClientCanPerform(request)).isFalse();
    }
}