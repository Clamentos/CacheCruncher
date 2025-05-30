package io.github.clamentos.cachecruncher.web.controllers;

///
import io.github.clamentos.cachecruncher.business.services.CacheTraceService;

///..
import io.github.clamentos.cachecruncher.error.exceptions.EntityNotFoundException;

///..
import io.github.clamentos.cachecruncher.web.dtos.trace.CacheTraceDto;

///.
import java.util.List;

///.
import org.springframework.beans.factory.annotation.Autowired;

///..
import org.springframework.dao.DataAccessException;

///..
import org.springframework.http.ResponseEntity;

///..
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

///
@RestController
@RequestMapping(path = "cache-cruncher/cache-trace")

///
public class CacheTraceController {

    ///
    private final CacheTraceService cacheTraceService;

    ///
    @Autowired
    public CacheTraceController(final CacheTraceService cacheTraceService) {

        this.cacheTraceService = cacheTraceService;
    }

    ///
    @PostMapping(consumes = "application/json")
    public ResponseEntity<Void> create(@RequestBody final CacheTraceDto cacheTraceDto)
    throws DataAccessException, IllegalArgumentException {

        cacheTraceService.create(cacheTraceDto);
        return ResponseEntity.ok().build();
    }

    ///..
    @GetMapping(produces = "application/json")
    public ResponseEntity<CacheTraceDto> getById(@RequestParam final long traceId) throws DataAccessException, EntityNotFoundException {

        return ResponseEntity.ok(cacheTraceService.getById(traceId));
    }

    ///..
    @GetMapping(path = "/search", produces = "application/json")
    public ResponseEntity<List<CacheTraceDto>> getMinimalByNameLikeAndDates(

        @RequestParam final String name,
        @RequestParam final long createdAtStart,
        @RequestParam final long createdAtEnd,
        @RequestParam(required = false) final Long updatedAtStart,
        @RequestParam(required = false) final Long updatedAtEnd

    ) throws DataAccessException {

        return ResponseEntity.ok(cacheTraceService.getByFilter(

            name,
            createdAtStart,
            createdAtEnd,
            updatedAtStart,
            updatedAtEnd
        ));
    }

    ///..
    @PatchMapping(consumes = "application/json")
    public ResponseEntity<Void> update(@RequestBody final CacheTraceDto cacheTraceDto)
    throws DataAccessException, EntityNotFoundException, IllegalArgumentException {

        cacheTraceService.update(cacheTraceDto);
        return ResponseEntity.ok().build();
    }

    ///..
    @DeleteMapping
    public ResponseEntity<Void> deleteById(@RequestParam final long traceId) throws DataAccessException, EntityNotFoundException {

        cacheTraceService.delete(traceId);
        return ResponseEntity.ok().build();
    }

    ///
}
