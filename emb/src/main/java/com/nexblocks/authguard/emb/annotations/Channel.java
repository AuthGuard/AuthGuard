package com.nexblocks.authguard.emb.annotations;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Repeatable(Channels.class)
public @interface Channel {
    String value();
}
