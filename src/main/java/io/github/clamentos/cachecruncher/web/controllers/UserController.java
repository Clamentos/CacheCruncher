package io.github.clamentos.cachecruncher.web.controllers;

///
import io.github.clamentos.cachecruncher.business.services.UserService;

///..
import io.github.clamentos.cachecruncher.error.exceptions.AuthenticationException;
import io.github.clamentos.cachecruncher.error.exceptions.AuthorizationException;
import io.github.clamentos.cachecruncher.error.exceptions.EntityAlreadyExistsException;
import io.github.clamentos.cachecruncher.error.exceptions.EntityNotFoundException;

///..
import io.github.clamentos.cachecruncher.persistence.entities.Session;
import io.github.clamentos.cachecruncher.persistence.entities.User;

///..
import io.github.clamentos.cachecruncher.utility.Pair;

///.
import java.util.List;

///.
import org.springframework.beans.factory.annotation.Autowired;

///..
import org.springframework.dao.DataAccessException;

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
    public UserController(UserService userService) {

        this.userService = userService;
    }

    ///
    @PostMapping(path = "/register", consumes = "application/json")
    public ResponseEntity<Void> register(@RequestParam String email, @RequestParam String password)
    throws DataAccessException, EntityAlreadyExistsException, IllegalArgumentException {

        userService.register(email, password);
        return ResponseEntity.ok().build();
    }

    ///..
    @PostMapping(path = "/login", produces = "application/json")
    public ResponseEntity<User> login(

        @RequestParam String username,
        @RequestParam String password,
        @RequestHeader(name = "User-Agent") String device

    ) throws AuthenticationException, DataAccessException {

        Pair<User, Session> loginResult =  userService.login(username, password, device);
        HttpHeaders headers = new HttpHeaders();

        headers.add(

            "Set-Cookie",
            "sessionIdCookie=" + loginResult.getB().getId() + "; " +
            "Max-Age=" + (loginResult.getB().getExpiresAt() / 1000) + "; " +
            "Path=/cache-cruncher; " +
            "HttpOnly"
        );

        return new ResponseEntity<>(loginResult.getA(), headers, HttpStatus.OK);
    }

    ///..
    @DeleteMapping(path = "/logout")
    public ResponseEntity<Void> logout(@RequestAttribute(name = "session") Session session)
    throws AuthenticationException, DataAccessException {

        userService.logout(session);
        return ResponseEntity.ok().build();
    }

    ///..
    @DeleteMapping(path = "/logout-all")
    public ResponseEntity<Void> logoutAll(@RequestAttribute(name = "session") Session session) {

        userService.logoutAll(session);
        return ResponseEntity.ok().build();
    }

    ///..
    @GetMapping
    public ResponseEntity<List<User>> getAll() throws DataAccessException {

        return ResponseEntity.ok(userService.getAll());
    }

    ///..
    @PatchMapping
    public ResponseEntity<Void> updatePrivilege(@RequestParam long id, @RequestParam boolean admin) throws DataAccessException {

        userService.updatePrivilege(id, admin);
        return ResponseEntity.ok().build();
    }

    ///..
    @DeleteMapping
    public ResponseEntity<Void> delete(@RequestParam long id, @RequestAttribute(name = "session") Session session)
    throws AuthorizationException, DataAccessException, EntityNotFoundException {

        userService.delete(id, session);
        return ResponseEntity.ok().build();
    }

    ///
}
