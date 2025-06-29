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
    public ErrorDetails(final ErrorCode errorCode, final Serializable... arguments) {

        super();

        this.errorCode = errorCode;
        this.arguments = arguments;
    }

    ///..
    public ErrorDetails(final ErrorCode errorCode, final Throwable cause, final Serializable... arguments) {

        super(cause);

        this.errorCode = errorCode;
        this.arguments = arguments;
    }

    ///
}
