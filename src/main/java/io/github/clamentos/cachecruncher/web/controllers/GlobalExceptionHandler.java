package io.github.clamentos.cachecruncher.web.controllers;

///
import io.github.clamentos.cachecruncher.error.ErrorCode;
import io.github.clamentos.cachecruncher.error.ErrorDetails;

///..
import io.github.clamentos.cachecruncher.error.exceptions.AuthenticationException;
import io.github.clamentos.cachecruncher.error.exceptions.AuthorizationException;
import io.github.clamentos.cachecruncher.error.exceptions.EntityAlreadyExistsException;
import io.github.clamentos.cachecruncher.error.exceptions.EntityNotFoundException;

///.
import java.net.URI;

///..
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

///.
import org.springframework.dao.DataAccessException;

///..
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;

///..
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

///..
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

///..
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

///.
import lombok.extern.slf4j.Slf4j;

///
@ControllerAdvice
@Slf4j

///
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    ///
    @ExceptionHandler(value = AuthenticationException.class)
    protected ResponseEntity<ProblemDetail> handleAuthenticationException(AuthenticationException exc, WebRequest request) {

        return this.constructErrorFromExceptionMessage(exc, request, HttpStatus.UNAUTHORIZED);
    }

    ///..
    @ExceptionHandler(value = AuthorizationException.class)
    protected ResponseEntity<ProblemDetail> handleAuthorizationException(AuthorizationException exc, WebRequest request) {

        return this.constructErrorFromExceptionMessage(exc, request, HttpStatus.FORBIDDEN);
    }

    ///..
    @ExceptionHandler(value = DataAccessException.class)
    protected ResponseEntity<ProblemDetail> handleDataAccessException(DataAccessException exc, WebRequest request) {

        log.error("{}: {}", exc.getClass().getSimpleName(), exc.getMessage());
        return this.constructErrorFromExceptionMessage(exc, request, HttpStatus.UNPROCESSABLE_ENTITY);
    }

    ///..
    @ExceptionHandler(value = EntityAlreadyExistsException.class)
    protected ResponseEntity<ProblemDetail> handleEntityAlreadyExistsException(EntityAlreadyExistsException exc, WebRequest request) {

        return this.constructErrorFromExceptionMessage(exc, request, HttpStatus.CONFLICT);
    }

    ///..
    @ExceptionHandler(value = EntityNotFoundException.class)
    protected ResponseEntity<ProblemDetail> handleEntityNotFoundException(EntityNotFoundException exc, WebRequest request) {

        return this.constructErrorFromExceptionMessage(exc, request, HttpStatus.NOT_FOUND);
    }

    ///..
    @ExceptionHandler(value = IllegalArgumentException.class)
    protected ResponseEntity<ProblemDetail> handleIllegalArgumentException(IllegalArgumentException exc, WebRequest request) {

        return this.constructErrorFromExceptionMessage(exc, request, HttpStatus.BAD_REQUEST);
    }

    ///.
    private ResponseEntity<ProblemDetail> constructErrorFromExceptionMessage(Throwable exc, WebRequest request, HttpStatus status) {

        String message = exc.getMessage() != null ? exc.getMessage() : "";
        ErrorCode errorCode = ErrorCode.getDefault();
        Throwable cause = exc.getCause();
        List<String> arguments = new ArrayList<>();

        if(cause instanceof ErrorDetails details) {

            errorCode = details.getErrorCode();
            
            if(details.getArguments() != null) {

                for(Object argument : details.getArguments()) {

                    arguments.add(Objects.toString(argument));
                }
            }
        }

        ProblemDetail payload = ProblemDetail.forStatus(status);    // 403

        payload.setType(URI.create(errorCode.toString()));          // "NOT_ENOUGH_PRIVILEGES"
        payload.setTitle(status.getReasonPhrase());                 // "Forbidden"
        payload.setDetail(message);                                 // "Not enough privileges to call this API"
        payload.setInstance(URI.create(((ServletWebRequest)request).getRequest().getRequestURI())); // "http://..."

        payload.setProperty("timestamp", System.currentTimeMillis());
        payload.setProperty("detailArguments", arguments);              // [...]

        return ResponseEntity.status(status).body(payload);
    }

    ///
}
