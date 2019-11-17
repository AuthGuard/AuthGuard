package org.auther.dal.mock;

import com.google.inject.Singleton;
import org.auther.dal.CredentialsRepository;
import org.auther.dal.model.CredentialsDO;

import java.util.Optional;

@Singleton
public class MockCredentialsRepository extends AbstractRepository<CredentialsDO> implements CredentialsRepository {
    @Override
    public Optional<CredentialsDO> findByUsername(final String username) {
        return getRepo()
                .values()
                .stream()
                .filter(credentials -> credentials.getUsername().equals(username))
                .findFirst();
    }

    @Override
    public Optional<CredentialsDO> delete(final String id) {
        return Optional.empty();
    }
}
