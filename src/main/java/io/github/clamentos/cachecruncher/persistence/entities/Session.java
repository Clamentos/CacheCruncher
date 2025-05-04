package io.github.clamentos.cachecruncher.persistence.entities;

///
import lombok.AllArgsConstructor;
import lombok.Getter;

///
@AllArgsConstructor
@Getter

///
public final class Session {

    ///
    private final long userId;
    private final long expiresAt;
    private final String email;
    private final String device;
    private final String id;
    private final boolean isAdmin;

    ///
}
