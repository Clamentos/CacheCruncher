package io.github.clamentos.cachecruncher.web.controllers;

///
import io.github.clamentos.cachecruncher.business.services.CacheTraceService;

///..
import io.github.clamentos.cachecruncher.error.exceptions.EntityNotFoundException;
import io.github.clamentos.cachecruncher.error.exceptions.SimulationException;

///..
import io.github.clamentos.cachecruncher.web.dtos.SimulationArgumentsDto;
import io.github.clamentos.cachecruncher.web.dtos.SimulationReportSummaryDto;

///.
import java.util.Map;

///.
import org.springframework.beans.factory.annotation.Autowired;

///..
import org.springframework.dao.DataAccessException;

///..
import org.springframework.http.ResponseEntity;

///..
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

///
@RestController
@RequestMapping(path = "cache-cruncher/simulation")

///
public class CacheSimulationController {

    ///
    private final CacheTraceService cacheTraceService;

    ///
    @Autowired
    public CacheSimulationController(CacheTraceService cacheTraceService) {

        this.cacheTraceService = cacheTraceService;
    }

    ///
    @GetMapping(consumes = "application/json", produces = "application/json")
    public ResponseEntity<Map<String, Map<String, SimulationReportSummaryDto>>> simulate(

        @RequestBody SimulationArgumentsDto simulationArgumentsDto

    ) throws DataAccessException, EntityNotFoundException, IllegalArgumentException, SimulationException {

        return ResponseEntity.ok(cacheTraceService.simulate(simulationArgumentsDto));
    }

    ///
}
