package io.github.clamentos.cachecruncher.persistence.entities;

///
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

///
@AllArgsConstructor
@Getter
@Setter

///
public final class CacheTrace {

    ///
    private long id;
    private long createdAt;
    private Long updatedAt;
    private String description;
    private String name;
    private String data;

    ///
}
