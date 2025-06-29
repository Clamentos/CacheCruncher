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
import io.github.clamentos.cachecruncher.error.exceptions.DeserializationException;
import io.github.clamentos.cachecruncher.error.exceptions.EntityAlreadyExistsException;
import io.github.clamentos.cachecruncher.error.exceptions.EntityNotFoundException;
import io.github.clamentos.cachecruncher.error.exceptions.SerializationException;
import io.github.clamentos.cachecruncher.error.exceptions.UnprocessableRequestException;
import io.github.clamentos.cachecruncher.error.exceptions.ValidationException;
import io.github.clamentos.cachecruncher.error.exceptions.WrongPasswordException;

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
    @ExceptionHandler(value = {

        AuthenticationException.class,
        AuthorizationException.class,
        DatabaseException.class,
        DeserializationException.class,
        EntityAlreadyExistsException.class,
        EntityNotFoundException.class,
        IllegalArgumentException.class,
        SerializationException.class,
        UnprocessableRequestException.class,
        ValidationException.class,
        WrongPasswordException.class
    })
    protected ResponseEntity<Object> handle(final Exception exc, final WebRequest request) {

        return this.constructResponse(exc, request);
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

                final Exception placeholder = new Exception(new ErrorDetails(

                    ErrorCode.VALIDATOR_BAD_FORMAT,
                    unrecognized.getPropertyName(),
                    "is unknown"
                ));

                return this.constructResponse(placeholder, request);
            }

            if(cause instanceof final ValueInstantiationException value) {

                final Throwable exception = value.getCause();
                if(exception != null) return this.constructResponse(exception, request);
            }
        }

		return super.handleHttpMessageNotReadable(exc, headers, status, request);
	}

    ///.
    private ResponseEntity<Object> constructResponse(final Throwable exc, final WebRequest request) {

        ErrorCode errorCode = ErrorCode.getDefault();
        final List<String> arguments = new ArrayList<>();

        if(exc.getCause() instanceof final ErrorDetails cause) {

            errorCode = cause.getErrorCode();
            final Serializable[] args = cause.getArguments();

            if(args != null) {

                for(final Serializable argument : args) {

                    arguments.add(Objects.toString(argument));
                }
            }
        }

        final HttpStatus responseStatus = errorCode.getResponseStatus();
        final ProblemDetail payload = ProblemDetail.forStatus(responseStatus);

        payload.setType(URI.create(errorCode.toString()));
        payload.setTitle(responseStatus.getReasonPhrase());
        payload.setDetail(errorCode.getMessage());
        payload.setInstance(URI.create(((ServletWebRequest)request).getRequest().getRequestURI()));

        payload.setProperty("timestamp", System.currentTimeMillis());
        payload.setProperty("detailArguments", arguments);

        if(errorCode == ErrorCode.UNCATEGORIZED) log.error("constructPayload fallback log", exc);
        return ResponseEntity.status(responseStatus).body(payload);
    }

    ///
}
