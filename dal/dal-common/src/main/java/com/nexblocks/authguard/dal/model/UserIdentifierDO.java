package com.nexblocks.authguard.dal.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import jakarta.persistence.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
// JPA
@Entity
@Table(name = "user_identifiers", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "identifier", "domain" }, name = "IDENTIFIER_DUP")
})
public class UserIdentifierDO {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // this is only here for relational DBs, other DBs can ignore it

    private Type type;
    private String identifier;
    private boolean active;
    private String domain;
    private Long accountId;

    public enum Type {
        USERNAME,
        EMAIL,
        PHONE_NUMBER
    }
}
