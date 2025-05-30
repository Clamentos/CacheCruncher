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
import io.github.clamentos.cachecruncher.error.exceptions.EntityAlreadyExistsException;
import io.github.clamentos.cachecruncher.error.exceptions.EntityNotFoundException;

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
import org.springframework.dao.DataAccessException;

///..
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;

///..
import org.springframework.http.converter.HttpMessageNotReadableException;

///..
import org.springframework.mail.MailException;

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
    @ExceptionHandler(value = DataAccessException.class)
    protected ResponseEntity<ProblemDetail> handleDataAccessException(final DataAccessException exc, final WebRequest request) {

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
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(

        final HttpMessageNotReadableException exc,
        final HttpHeaders headers,
        final HttpStatusCode status,
        final WebRequest request
    ) {

        final Throwable cause = exc.getCause();

        if(cause != null) {

            if(cause instanceof UnrecognizedPropertyException unrecognized) {

                return this.constructErrorFromExceptionMessageObj(

                    new ErrorDetails(ErrorCode.VALIDATOR_BAD_FORMAT, unrecognized.getPropertyName(), " is unknown"),
                    request,
                    HttpStatus.BAD_REQUEST
                );
            }

            if(cause instanceof ValueInstantiationException value) {

                final Throwable exception = value.getCause();

                if(exception != null && (exception.getCause() instanceof ErrorDetails details)) {

                    return this.constructErrorFromExceptionMessageObj(details, request, HttpStatus.BAD_REQUEST);
                }
            }
        }

		return super.handleHttpMessageNotReadable(exc, headers, status, request);
	}

    ///..
    @ExceptionHandler(value = IllegalArgumentException.class)
    protected ResponseEntity<ProblemDetail> handleIllegalArgumentException(

        final IllegalArgumentException exc,
        final WebRequest request
    ) {

        return this.constructErrorFromExceptionMessage(exc, request, HttpStatus.BAD_REQUEST);
    }

    ///..
    @ExceptionHandler(value = MailException.class)
    protected ResponseEntity<ProblemDetail> handleMailException(final MailException exc, final WebRequest request) {

        return this.constructErrorFromExceptionMessage(exc, request, HttpStatus.UNPROCESSABLE_ENTITY);
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

            Throwable cause = exc.getCause();

            if(cause instanceof ErrorDetails details) {

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

        return payload;
    }

    ///..
    private ErrorCode fillArguments(final ErrorDetails details, final List<String> arguments) {

        final Serializable[] detailArguments = details.getArguments();

        if(detailArguments != null) {

            for(Object argument : detailArguments) {

                arguments.add(Objects.toString(argument));
            }
        }

        return details.getErrorCode();
    }

    ///
}
