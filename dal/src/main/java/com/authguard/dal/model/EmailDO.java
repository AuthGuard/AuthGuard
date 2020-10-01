package com.authguard.dal.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import javax.persistence.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
// JPA
@Entity
@Table(name = "emails")
public class EmailDO {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // this is only here for relational DBs, other DBs can ignore it

    private String email;
    private boolean verified;

    @Column(name = "\"primary\"")
    private boolean primary;
    private boolean backup;
    private boolean active;
}
