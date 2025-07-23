package com.nexblocks.authguard.emb.vertx;

import com.nexblocks.authguard.emb.model.Message;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.MessageCodec;

public class EmbMessageCodec implements MessageCodec<Message, Message> {
    @Override
    public void encodeToWire(final Buffer buffer, final Message message) {

    }

    @Override
    public Message decodeFromWire(final int i, final Buffer buffer) {
        return null;
    }

    @Override
    public Message transform(final Message message) {
        return message;
    }

    @Override
    public String name() {
        return this.getClass().getCanonicalName();
    }

    @Override
    public byte systemCodecID() {
        return -1;
    }
}
