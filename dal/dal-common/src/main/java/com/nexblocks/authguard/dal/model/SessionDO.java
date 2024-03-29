package com.nexblocks.authguard.dal.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import javax.persistence.*;
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
public class SessionDO extends AbstractDO {
    private String sessionToken;
    private long accountId;
    private Instant expiresAt;

    @ElementCollection(fetch = FetchType.EAGER)
    private Map<String, String> data;
}
