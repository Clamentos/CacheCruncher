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
    private long updatedAt;
    private String description;
    private String name;
    private String data;

    ///
    public CacheTrace(long timestamp, String description, String name, String data) {

        this.id = 0;

        createdAt = timestamp;
        updatedAt = timestamp;

        this.description = description;
        this.name = name;
        this.data = data;
    }

    ///
}
