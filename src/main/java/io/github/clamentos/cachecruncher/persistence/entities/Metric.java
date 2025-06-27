package io.github.clamentos.cachecruncher.persistence.entities;

///
import lombok.AllArgsConstructor;
import lombok.Getter;

///
@AllArgsConstructor
@Getter

///
public final class Metric {

    ///
    private final long id;
    private final long createdAt;
    private final long timestamp;
    private final String endpoint;
    private final String data;
    private final short status;

    ///
}
