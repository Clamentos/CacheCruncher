package io.github.clamentos.cachecruncher.web.controllers;

///
import io.github.clamentos.cachecruncher.business.services.UserService;

///..
import io.github.clamentos.cachecruncher.error.exceptions.AuthenticationException;
import io.github.clamentos.cachecruncher.error.exceptions.AuthorizationException;
import io.github.clamentos.cachecruncher.error.exceptions.DatabaseException;
import io.github.clamentos.cachecruncher.error.exceptions.EntityAlreadyExistsException;
import io.github.clamentos.cachecruncher.error.exceptions.EntityNotFoundException;
import io.github.clamentos.cachecruncher.error.exceptions.ValidationException;

///..
import io.github.clamentos.cachecruncher.persistence.entities.Session;
import io.github.clamentos.cachecruncher.persistence.entities.User;

///..
import io.github.clamentos.cachecruncher.utility.Pair;

///..
import io.github.clamentos.cachecruncher.web.dtos.AuthDto;

///.
import java.util.List;

///.
import org.springframework.beans.factory.annotation.Autowired;

///..
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

///..
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

///
@RestController
@RequestMapping(path = "cache-cruncher/user")

///
public class UserController {

    ///
    private final UserService userService;

    ///
    @Autowired
    public UserController(final UserService userService) {

        this.userService = userService;
    }

    ///
    @PostMapping(path = "/register", produces = "text/plain", consumes = "application/json")
    public ResponseEntity<String> register(@RequestBody final AuthDto auth)
    throws DatabaseException, EntityAlreadyExistsException, ValidationException {

        return ResponseEntity.ok(userService.register(auth));
    }

    ///..
    @PostMapping(path = "/resend", produces = "text/plain", consumes = "application/json")
    public ResponseEntity<String> resendVerificationEmail(@RequestBody final String email) {

        return ResponseEntity.ok(userService.resendVerificationEmail(email));
    }

    ///..
    @GetMapping(path = "/confirm-email")
    public ResponseEntity<Void> confirmEmail(@RequestParam final String token)
    throws AuthenticationException, DatabaseException, EntityNotFoundException, ValidationException {

        userService.confirmEmail(token);
        return ResponseEntity.ok().build();
    }

    ///..
    @PostMapping(path = "/login", produces = "application/json", consumes = "application/json")
    public ResponseEntity<Pair<User, Long>> login(

        @RequestBody final AuthDto authDto,
        @RequestHeader(name = "User-Agent") final String device

    ) throws AuthenticationException, AuthorizationException, DatabaseException, ValidationException {

        final Pair<User, Session> loginResult =  userService.login(authDto, device);

        return new ResponseEntity<>(

            new Pair<>(loginResult.getA(), loginResult.getB().getExpiresAt()),
            this.generateCookie(loginResult.getB().getId(), loginResult.getB().getExpiresAt()),
            HttpStatus.OK
        );
    }

    ///..
    @PostMapping(path = "/refresh", produces = "text/plain")
    public ResponseEntity<Long> refresh(

        @RequestAttribute(name = "session") final Session session,
        @RequestHeader(name = "User-Agent") final String device

    ) throws AuthenticationException, AuthorizationException, DatabaseException {

        final Session refreshedSession = userService.refresh(session, device);

        return new ResponseEntity<>(
            
            refreshedSession.getExpiresAt(),
            this.generateCookie(refreshedSession.getId(), refreshedSession.getExpiresAt()),
            HttpStatus.OK
        );
    }

    ///..
    @DeleteMapping(path = "/logout")
    public ResponseEntity<Void> logout(@RequestAttribute(name = "session") final Session session)
    throws AuthenticationException, DatabaseException {

        userService.logout(session);
        return ResponseEntity.ok().build();
    }

    ///..
    @DeleteMapping(path = "/logout-all")
    public ResponseEntity<Void> logoutAll(@RequestAttribute(name = "session") final Session session) {

        userService.logoutAll(session.getUserId());
        return ResponseEntity.ok().build();
    }

    ///..
    @GetMapping(produces = "application/json")
    public ResponseEntity<List<User>> getAll() throws DatabaseException {

        return ResponseEntity.ok(userService.getAll());
    }

    ///..
    @PatchMapping
    public ResponseEntity<Void> updatePrivilege(@RequestParam final long userId, @RequestParam final boolean admin)
    throws DatabaseException, EntityNotFoundException {

        userService.updatePrivilege(userId, admin);
        return ResponseEntity.ok().build();
    }

    ///..
    @DeleteMapping
    public ResponseEntity<Void> delete(@RequestParam final long userId, @RequestAttribute(name = "session") final Session session)
    throws AuthenticationException, AuthorizationException, DatabaseException, EntityNotFoundException {

        userService.delete(userId, session);
        return ResponseEntity.ok().build();
    }

    ///.
    private HttpHeaders generateCookie(String sessionId, long expiresAt) {

        final HttpHeaders headers = new HttpHeaders();

        headers.add(

            "Set-Cookie",
            "sessionIdCookie=" + sessionId + "; " +
            "Max-Age=" + (expiresAt / 1_000) + "; " +
            "Path=/cache-cruncher; " +
            "HttpOnly; " +
            "Secure"
        );

        return headers;
    }

    ///
}
