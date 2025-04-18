package io.github.clamentos.cachecruncher.web.controllers;

///
import io.github.clamentos.cachecruncher.error.ErrorCode;

///..
import io.github.clamentos.cachecruncher.error.exceptions.EntityAlreadyExistsException;
import io.github.clamentos.cachecruncher.error.exceptions.EntityNotFoundException;
import io.github.clamentos.cachecruncher.error.exceptions.SimulationException;
import io.github.clamentos.cachecruncher.error.exceptions.TooManySimulationsException;

///.
import java.net.URI;

///..
import java.util.ArrayList;
import java.util.List;

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
    private static final String LOG_PATTERN = "{}: {}";

    ///
    @ExceptionHandler(value = DataAccessException.class)
    protected ResponseEntity<ProblemDetail> handleDataAccessException(DataAccessException exc, WebRequest request) {

        log.error(LOG_PATTERN, exc.getClass().getSimpleName(), exc.getMessage());
        return this.constructErrorFromExceptionMessage(exc, request, HttpStatus.UNPROCESSABLE_ENTITY);
    }

    ///..
    @ExceptionHandler(value = EntityNotFoundException.class)
    protected ResponseEntity<ProblemDetail> handleEntityNotFoundException(EntityNotFoundException exc, WebRequest request) {

        return this.constructErrorFromExceptionMessage(exc, request, HttpStatus.NOT_FOUND);
    }

    ///..
    @ExceptionHandler(value = EntityAlreadyExistsException.class)
    protected ResponseEntity<ProblemDetail> handleEntityAlreadyExistsException(EntityAlreadyExistsException exc, WebRequest request) {

        return this.constructErrorFromExceptionMessage(exc, request, HttpStatus.CONFLICT);
    }

    ///..
    @ExceptionHandler(value = IllegalArgumentException.class)
    protected ResponseEntity<ProblemDetail> handleIllegalArgumentException(IllegalArgumentException exc, WebRequest request) {

        return this.constructErrorFromExceptionMessage(exc, request, HttpStatus.BAD_REQUEST);
    }

    ///..
    @ExceptionHandler(value = SimulationException.class)
    protected ResponseEntity<ProblemDetail> handleSimulationException(SimulationException exc, WebRequest request) {

        log.error(LOG_PATTERN, exc.getClass().getSimpleName(), exc.getMessage());
        return this.constructErrorFromExceptionMessage(exc, request, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    ///..
    @ExceptionHandler(value = TooManySimulationsException.class)
    protected ResponseEntity<ProblemDetail> handleTooManySimulationsException(TooManySimulationsException exc, WebRequest request) {

        return this.constructErrorFromExceptionMessage(exc, request, HttpStatus.SERVICE_UNAVAILABLE);
    }

    ///.
    private ResponseEntity<ProblemDetail> constructErrorFromExceptionMessage(Throwable exc, WebRequest request, HttpStatus status) {

        String message = exc.getMessage() != null ? exc.getMessage() : "";
        String[] splits = message.split("/");

        ErrorCode errorCode = ErrorCode.getDefault();
        String title = null;
        List<String> arguments = new ArrayList<>();

        if(splits.length >= 1) {

            errorCode = ErrorCode.valueOf(splits[0]);
        }

        if(splits.length >= 2) {

            title = splits[1];
        }

        if(splits.length >= 3) {

            for(int i = 2; i < splits.length; i++) {

                arguments.add(splits[i]);
            }
        }

        ProblemDetail payload = ProblemDetail.forStatus(status);

        payload.setType(URI.create(errorCode.toString()));      // "NOT_ENOUGH_PRIVILEGES"
        payload.setTitle(status.getReasonPhrase());             // "Forbidden"
        payload.setDetail(title);                               // "Not enough privileges to call this API"
        payload.setInstance(URI.create(((ServletWebRequest)request).getRequest().getRequestURI())); // "http://..."

        payload.setProperty("timestamp", System.currentTimeMillis());   // 123456789...
        payload.setProperty("detailArguments", arguments);              // [...]

        return ResponseEntity.status(status).body(payload);
    }

    ///
}
