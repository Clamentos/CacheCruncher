package io.github.clamentos.cachecruncher.web.controllers;

///
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import com.fasterxml.jackson.databind.exc.ValueInstantiationException;

///.
import io.github.clamentos.cachecruncher.error.ErrorCode;
import io.github.clamentos.cachecruncher.error.ErrorDetails;

///..
import io.github.clamentos.cachecruncher.error.exceptions.AuthenticationException;
import io.github.clamentos.cachecruncher.error.exceptions.AuthorizationException;
import io.github.clamentos.cachecruncher.error.exceptions.DatabaseException;
import io.github.clamentos.cachecruncher.error.exceptions.EntityAlreadyExistsException;
import io.github.clamentos.cachecruncher.error.exceptions.EntityNotFoundException;
import io.github.clamentos.cachecruncher.error.exceptions.UnprocessableRequestException;
import io.github.clamentos.cachecruncher.error.exceptions.ValidationException;

///.
import java.io.Serializable;

///..
import java.net.URI;

///..
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

///.
import lombok.extern.slf4j.Slf4j;

///.
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;

///..
import org.springframework.http.converter.HttpMessageNotReadableException;

///..
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

///..
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

///..
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

///
@ControllerAdvice
@Slf4j

///
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    ///
    @ExceptionHandler(value = AuthenticationException.class)
    protected ResponseEntity<ProblemDetail> handleAuthenticationException(final AuthenticationException exc, final WebRequest request) {

        return this.constructErrorFromExceptionMessage(exc, request, HttpStatus.UNAUTHORIZED);
    }

    ///..
    @ExceptionHandler(value = AuthorizationException.class)
    protected ResponseEntity<ProblemDetail> handleAuthorizationException(final AuthorizationException exc, final WebRequest request) {

        return this.constructErrorFromExceptionMessage(exc, request, HttpStatus.FORBIDDEN);
    }

    ///..
    @ExceptionHandler(value = DatabaseException.class)
    protected ResponseEntity<ProblemDetail> handleDatabaseException(final DatabaseException exc, final WebRequest request) {

        log.error("{}: {}", exc.getClass().getSimpleName(), exc.getMessage());
        return this.constructErrorFromExceptionMessage(exc, request, HttpStatus.UNPROCESSABLE_ENTITY);
    }

    ///..
    @ExceptionHandler(value = EntityAlreadyExistsException.class)
    protected ResponseEntity<ProblemDetail> handleEntityAlreadyExistsException(

        final EntityAlreadyExistsException exc,
        final WebRequest request
    ) {

        return this.constructErrorFromExceptionMessage(exc, request, HttpStatus.CONFLICT);
    }

    ///..
    @ExceptionHandler(value = EntityNotFoundException.class)
    protected ResponseEntity<ProblemDetail> handleEntityNotFoundException(final EntityNotFoundException exc, final WebRequest request) {

        return this.constructErrorFromExceptionMessage(exc, request, HttpStatus.NOT_FOUND);
    }

    ///..
    @ExceptionHandler(value = UnprocessableRequestException.class)
    protected ResponseEntity<ProblemDetail> handleUnprocessableRequestException(

        final UnprocessableRequestException exc,
        final WebRequest request
    ) {

        final ErrorDetails details = (ErrorDetails)exc.getCause();
        final ErrorCode errorCode = details.getErrorCode();

        if(errorCode == ErrorCode.TOO_MANY_REQUESTS) {

            final HttpHeaders headers = new HttpHeaders();
            headers.add("Retry-After", details.getArguments()[1].toString());

            return new ResponseEntity<>(

                this.constructPayload(exc, request, HttpStatus.TOO_MANY_REQUESTS),
                headers,
                HttpStatus.TOO_MANY_REQUESTS
            );
        }

        return this.constructErrorFromExceptionMessage(

            exc,
            request,
            errorCode == ErrorCode.API_NOT_FOUND ? HttpStatus.NOT_FOUND : HttpStatus.BAD_REQUEST
        );
    }

    ///..
    @ExceptionHandler(value = ValidationException.class)
    protected ResponseEntity<ProblemDetail> handleValidationException(final ValidationException exc, final WebRequest request) {

        return this.constructErrorFromExceptionMessage(exc, request, HttpStatus.BAD_REQUEST);
    }

    ///..
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(

        final HttpMessageNotReadableException exc,
        final HttpHeaders headers,
        final HttpStatusCode status,
        final WebRequest request
    ) {

        final Throwable cause = exc.getCause();

        if(cause != null) {

            if(cause instanceof final UnrecognizedPropertyException unrecognized) {

                return this.constructErrorFromExceptionMessageObj(

                    new ErrorDetails(ErrorCode.VALIDATOR_BAD_FORMAT, unrecognized.getPropertyName(), "is unknown"),
                    request,
                    HttpStatus.BAD_REQUEST
                );
            }

            if(cause instanceof final ValueInstantiationException value) {

                final Throwable exception = value.getCause();

                if(exception != null && (exception.getCause() instanceof final ErrorDetails details)) {

                    return this.constructErrorFromExceptionMessageObj(details, request, HttpStatus.BAD_REQUEST);
                }
            }
        }

		return super.handleHttpMessageNotReadable(exc, headers, status, request);
	}

    ///.
    private ResponseEntity<ProblemDetail> constructErrorFromExceptionMessage(

        final Throwable exc,
        final WebRequest request,
        final HttpStatus status
    ) {

        return ResponseEntity.status(status).body(this.constructPayload(exc, request, status));
    }

    ///..
    private ResponseEntity<Object> constructErrorFromExceptionMessageObj(

        final Throwable exc,
        final WebRequest request,
        final HttpStatus status
    ) {

        return ResponseEntity.status(status).body(this.constructPayload(exc, request, status));
    }

    ///..
    private ProblemDetail constructPayload(final Throwable exc, final WebRequest request, final HttpStatus status) {

        ErrorCode errorCode = ErrorCode.getDefault();
        final List<String> arguments = new ArrayList<>();

        if(exc instanceof ErrorDetails details) {

            errorCode = this.fillArguments(details, arguments);
        }

        else {

            final Throwable cause = exc.getCause();

            if(cause instanceof final ErrorDetails details) {

                errorCode = this.fillArguments(details, arguments);
            }
        }

        final ProblemDetail payload = ProblemDetail.forStatus(status);    // 403

        payload.setType(URI.create(errorCode.toString()));          // "NOT_ENOUGH_PRIVILEGES"
        payload.setTitle(status.getReasonPhrase());                 // "Forbidden"
        payload.setDetail(errorCode.getMessage());                  // "Not enough privileges to call this API"
        payload.setInstance(URI.create(((ServletWebRequest)request).getRequest().getRequestURI())); // "http://..."

        payload.setProperty("timestamp", System.currentTimeMillis());
        payload.setProperty("detailArguments", arguments);              // [...]

        if(errorCode == ErrorCode.UNCATEGORIZED) log.error("constructPayload fallback log", exc);
        return payload;
    }

    ///..
    private ErrorCode fillArguments(final ErrorDetails details, final List<String> arguments) {

        final Serializable[] detailArguments = details.getArguments();

        if(detailArguments != null) {

            for(final Object argument : detailArguments) {

                arguments.add(Objects.toString(argument));
            }
        }

        return details.getErrorCode();
    }

    ///
}
