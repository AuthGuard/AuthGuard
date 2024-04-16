package com.nexblocks.authguard.dal.model;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class EventsJpaTest {
    private EntityManager entityManager;
    private EventDO firstEvent;
    private EventDO secondEvent;
    private EventDO thirdEvent;

    @BeforeAll
    void setup() {
        final H2 h2 = new H2().withMappedClass(EventDO.class)
                .prepare();

        entityManager = h2.getEntityManager();

        // create the test attempts
        entityManager.getTransaction().begin();

        firstEvent = EventDO.builder()
                .id(1)
                .domain("main")
                .channel("roles")
                .createdAt(Instant.now().minus(Duration.ofHours(3)))
                .build();

        secondEvent = EventDO.builder()
                .id(2)
                .domain("main")
                .channel("roles")
                .createdAt(Instant.now().minus(Duration.ofHours(1)))
                .build();

        thirdEvent = EventDO.builder()
                .id(3)
                .domain("main")
                .channel("accounts")
                .createdAt(Instant.now().minus(Duration.ofHours(1)))
                .build();

        entityManager.persist(firstEvent);
        entityManager.persist(secondEvent);
        entityManager.persist(thirdEvent);

        entityManager.persist(EventDO.builder()
                .id(4)
                .domain("other")
                .channel("roles")
                .createdAt(Instant.now().minus(Duration.ofHours(1)))
                .build());

        entityManager.getTransaction().commit();
    }

    @Test
    void getByDomainNoCursor() {
        TypedQuery<EventDO> query = entityManager.createNamedQuery("events.getByDomain", EventDO.class)
                .setParameter("domain", "main")
                .setParameter("cursor", Instant.now().plus(Duration.ofDays(100 * 365)));

        List<EventDO> retrieved = query.getResultList();
        Assertions.assertThat(retrieved).containsExactly(thirdEvent, secondEvent, firstEvent);
    }

    @Test
    void getByDomainAndChannelNoCursor() {
        TypedQuery<EventDO> query = entityManager.createNamedQuery("events.getByDomainAndChannel", EventDO.class)
                .setParameter("domain", "main")
                .setParameter("channel", "roles")
                .setParameter("cursor", Instant.now().plus(Duration.ofDays(100 * 365)));

        List<EventDO> retrieved = query.getResultList();
        Assertions.assertThat(retrieved).containsExactly(secondEvent, firstEvent);
    }

    @Test
    void getByDomainWithCursor() {
        TypedQuery<EventDO> query = entityManager.createNamedQuery("events.getByDomain", EventDO.class)
                .setParameter("domain", "main")
                .setParameter("cursor", Instant.now().minus(Duration.ofHours(2)));

        List<EventDO> retrieved = query.getResultList();
        Assertions.assertThat(retrieved).containsExactly(firstEvent);
    }

    @Test
    void getByDomainAndChannelWithCursor() {
        TypedQuery<EventDO> query = entityManager.createNamedQuery("events.getByDomainAndChannel", EventDO.class)
                .setParameter("domain", "main")
                .setParameter("channel", "roles")
                .setParameter("cursor", Instant.now().minus(Duration.ofHours(2)));

        List<EventDO> retrieved = query.getResultList();
        Assertions.assertThat(retrieved).containsExactly(secondEvent);
    }
}
