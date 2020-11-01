package com.authguard.dal.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import javax.persistence.*;
import java.time.ZonedDateTime;
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
        query = "SELECT session FROM SessionDO session WHERE session.sessionToken = :token"
)
public class SessionDO extends AbstractDO {
    private String sessionToken;
    private String accountId;
    private ZonedDateTime expiresAt;

    @ElementCollection
    private Map<String, String> data;
}
