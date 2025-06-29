package io.github.clamentos.cachecruncher.persistence.entities;

///
import io.github.clamentos.cachecruncher.persistence.UserRole;

///.
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
    private final UserRole role;

    ///
    public boolean isExpired(final long now) {

        return expiresAt < now;
    }

    ///
}
