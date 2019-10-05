package org.auther.api;

import org.jeasy.random.EasyRandom;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class AbstractRouteTest {
    private final EasyRandom easyRandom;
    private final TestServer server;
    private final RestMapper restMapper;

    private final String endpoint;

    AbstractRouteTest(final String endpoint) {
        this.server = new TestServer();
        this.easyRandom = new EasyRandom();
        this.restMapper = new RestMapperImpl();

        this.endpoint = endpoint;
    }

    String url() {
        return String.format("http://localhost:%d/%s", server.getPort(), endpoint);
    }

    String url(final String... parts) {
        return String.format("http://localhost:%d/%s/%s", server.getPort(), endpoint, String.join("/", parts));
    }

    @BeforeAll
    void startServer() {
        server.start();
    }

    <T> T randomObject(final Class<T> clazz) {
        return easyRandom.nextObject(clazz);
    }

    RestMapper mapper() {
        return this.restMapper;
    }

    <T> T mockService(final Class<T> clazz) {
        return server.getMock(clazz);
    }
}