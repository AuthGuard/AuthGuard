package com.nexblocks.authguard.emb;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.nexblocks.authguard.config.ConfigContext;
import com.nexblocks.authguard.config.JacksonConfigContext;
import com.nexblocks.authguard.emb.model.Message;
import com.nexblocks.authguard.emb.vertx.VertxPublisherFactory;
import io.vertx.core.Vertx;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MessageBusTest {
    private MessageBus messageBus;

    private final List<Message<?>> receivedFromAccounts = new ArrayList<>();
    private final List<Message<?>> receivedFromAuth = new ArrayList<>();
    private final List<Message<?>> receivedFromAll = new ArrayList<>();

    @BeforeAll
    void setup()  {
        ObjectNode configNode = new ObjectNode(JsonNodeFactory.instance);

        configNode.set("channels", new ArrayNode(JsonNodeFactory.instance)
                .add("accounts")
                .add("auth")
        );

        ConfigContext configContext = new JacksonConfigContext(configNode);

        Vertx vertx = Vertx.vertx();
        messageBus = new MessageBus(new VertxPublisherFactory(vertx.eventBus()), configContext);
    }

    @Test
    void pubSub() throws InterruptedException {
        messageBus.subscribe("accounts", receivedFromAccounts::add);
        messageBus.subscribe("auth", receivedFromAuth::add);
        messageBus.subscribe("*", receivedFromAll::add);

        messageBus.publish("accounts", Message.builder()
                .messageBody("Accounts Message")
                .bodyType(String.class)
                .build()
        );

        messageBus.publish("auth", Message.builder()
                .messageBody("Auth Message")
                .bodyType(String.class)
                .build()
        );

        Thread.sleep(1000);

        assertThat(receivedFromAccounts).containsExactly(Message.builder()
                .channel("accounts")
                .messageBody("Accounts Message")
                .bodyType(String.class)
                .build());

        assertThat(receivedFromAuth).containsExactly(Message.builder()
                .channel("auth")
                .messageBody("Auth Message")
                .bodyType(String.class)
                .build());

        assertThat(receivedFromAll).containsExactly(
                Message.builder()
                        .channel("accounts")
                        .messageBody("Accounts Message")
                        .bodyType(String.class)
                        .build(),
                Message.builder()
                        .channel("auth")
                        .messageBody("Auth Message")
                        .bodyType(String.class)
                        .build());
    }
}