package com.nexblocks.authguard.dal.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import javax.persistence.*;
import java.time.OffsetDateTime;

@Data
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@MappedSuperclass
public abstract class AbstractDO {
    @Id
    @Convert(converter = LongToStringConverter.class)
    private String id;
    private boolean deleted;
    private OffsetDateTime createdAt;
    private OffsetDateTime lastModified;
}
