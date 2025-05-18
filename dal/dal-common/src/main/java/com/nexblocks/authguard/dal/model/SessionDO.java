package com.nexblocks.authguard.dal.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
// JPA
@Entity
@Table(name = "sessions", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"sessionToken"})
})
@NamedQuery(
        name = "sessions.getByToken",
        query = "SELECT session FROM SessionDO session WHERE session.sessionToken = :token AND session.deleted = false"
)
@NamedQuery(
        name = "sessions.getByAccountId",
        query = "SELECT session FROM SessionDO session " +
                "WHERE session.domain = :domain AND session.accountId = :accountId " +
                "AND session.deleted = false"
)
public class SessionDO extends AbstractDO {
    private String domain;
    private String sessionToken;
    private long accountId;
    private Instant expiresAt;
    private boolean forTracking;
    private boolean active;

    @ElementCollection(fetch = FetchType.EAGER)
    private Map<String, String> data;
}
