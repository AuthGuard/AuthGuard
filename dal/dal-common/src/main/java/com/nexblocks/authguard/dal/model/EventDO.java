package com.nexblocks.authguard.dal.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import javax.persistence.Entity;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
// JPA
@Entity
@Table(name = "events")
@NamedQuery(
        name = "events.getByDomain",
        query = "SELECT event FROM EventDO event " +
                "WHERE event.domain = :domain " +
                "AND event.createdAt < :cursor " +
                "ORDER BY event.createdAt desc"
)
@NamedQuery(
        name = "events.getByDomainAndChannel",
        query = "SELECT event FROM EventDO event " +
                "WHERE event.domain = :domain " +
                "AND event.channel = :channel " +
                "AND event.createdAt < :cursor " +
                "ORDER BY event.createdAt desc"
)
public class EventDO extends AbstractDO {
    private String domain;
    private String eventType;
    private String channel;
    private String eventEntityType;
    private Long entityId;
    private String entitySnapshot;
    private String actorType;
    private String actor;
}
