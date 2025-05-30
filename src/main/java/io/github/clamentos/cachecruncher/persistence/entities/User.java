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
public final class User {

    ///
    private final long id;
    private final Long lockedUntil;
    private final long createdAt;
    private final Long validatedAt;
    private final String email;
    private String password;
    private final short failedAccesses;
    private final boolean isAdmin;

    ///
}
