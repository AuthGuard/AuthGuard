package org.auther.service.impl.jwt;

import org.auther.service.JtiProvider;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * This class is only here until a proper implementation is available
 */
public class BasicJtiProvider implements JtiProvider {
    private final Set<String> generatedIds;
    private final Set<String> usedIds;

    public BasicJtiProvider() {
        usedIds = new ConcurrentSkipListSet<>();
        generatedIds = new ConcurrentSkipListSet<>();
    }

    @Override
    public String next() {
        final String id = UUID.randomUUID().toString();
        generatedIds.add(id);
        return id;
    }

    @Override
    public boolean validate(final String jti) {
        if (usedIds.contains(jti) || !generatedIds.contains(jti)) {
            return false;
        }

        usedIds.add(jti);
        return true;
    }
}
