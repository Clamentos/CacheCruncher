package io.github.clamentos.cachecruncher.error;

///
import java.io.Serializable;

///.
import lombok.Getter;

///
@Getter

///
public final class ErrorDetails extends Throwable {

    ///
    private final ErrorCode errorCode;
    private final Serializable[] arguments;

    ///
    public ErrorDetails(ErrorCode errorCode, Serializable... arguments) {

        super();

        this.errorCode = errorCode;
        this.arguments = arguments;
    }

    ///
}
