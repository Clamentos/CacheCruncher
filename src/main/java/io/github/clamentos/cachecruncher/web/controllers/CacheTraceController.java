package io.github.clamentos.cachecruncher.web.controllers;

///
import io.github.clamentos.cachecruncher.business.services.CacheTraceService;

///..
import io.github.clamentos.cachecruncher.error.exceptions.EntityAlreadyExistsException;
import io.github.clamentos.cachecruncher.error.exceptions.EntityNotFoundException;

///..
import io.github.clamentos.cachecruncher.web.dtos.CacheSimulationReportDto;
import io.github.clamentos.cachecruncher.web.dtos.CacheTraceDto;
import io.github.clamentos.cachecruncher.web.dtos.SimulationArgumentsDto;

///.
import java.util.List;
import java.util.Map;

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
    public CacheTraceController(CacheTraceService cacheTraceService) {

        this.cacheTraceService = cacheTraceService;
    }

    ///
    @PostMapping(consumes = "application/json")
    public ResponseEntity<Void> create(@RequestBody CacheTraceDto cacheTraceDto)
    throws DataAccessException, EntityAlreadyExistsException, IllegalArgumentException {

        cacheTraceService.create(cacheTraceDto);
        return ResponseEntity.ok().build();
    }

    ///..
    @GetMapping(produces = "application/json")
    public ResponseEntity<CacheTraceDto> getById(@RequestParam long id) throws DataAccessException, EntityNotFoundException {

        return ResponseEntity.ok(cacheTraceService.getById(id));
    }

    ///..
    @GetMapping(path = "/search", produces = "application/json")
    public ResponseEntity<List<CacheTraceDto>> getMinimalByNameLikeAndDates(

        @RequestParam String name,
        @RequestParam long createdAtStart,
        @RequestParam long createdAtEnd,
        @RequestParam long updatedAtStart,
        @RequestParam long updatedAtEnd

    ) throws DataAccessException {

        return ResponseEntity.ok(cacheTraceService.getMinimalByNameLikeAndDates(

            name,
            createdAtStart,
            createdAtEnd,
            updatedAtStart,
            updatedAtEnd
        ));
    }

    ///..
    @PatchMapping(consumes = "application/json")
    public ResponseEntity<Void> update(@RequestBody CacheTraceDto cacheTraceDto)
    throws DataAccessException, EntityAlreadyExistsException, EntityNotFoundException, IllegalArgumentException {

        cacheTraceService.update(cacheTraceDto);
        return ResponseEntity.ok().build();
    }

    ///..
    @DeleteMapping
    public ResponseEntity<Void> deleteById(@RequestParam long id) throws DataAccessException {

        cacheTraceService.deleteById(id);
        return ResponseEntity.ok().build();
    }

    ///..
    @PostMapping(path = "/simulate", consumes = "application/json", produces = "application/json")
    public ResponseEntity<Map<Long, Map<String, CacheSimulationReportDto>>> simulate(@RequestBody SimulationArgumentsDto simulationArgumentsDto) {

        return ResponseEntity.ok(cacheTraceService.simulate(simulationArgumentsDto));
    }

    ///
}
