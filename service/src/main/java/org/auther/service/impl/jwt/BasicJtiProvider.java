package org.auther.service.impl.jwt;

import com.google.inject.Singleton;
import org.auther.service.JtiProvider;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * This class is only here until a proper implementation is available
 */
@Singleton
public class BasicJtiProvider implements JtiProvider {
    private final Set<String> generatedIds;

    public BasicJtiProvider() {
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
        return generatedIds.contains(jti);
    }
}
